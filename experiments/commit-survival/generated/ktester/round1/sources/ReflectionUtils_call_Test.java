package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class ReflectionUtils_call_Test {

    private TestBean bean;

    /**
     * Simple bean with a public method, a private method and overloaded signatures.
     */
    static class TestBean {

        public String greet(String name) {
            return "Hello " + name;
        }

        private String secret() {
            return "secret";
        }

        public void explode() throws Exception {
            throw new Exception("boom");
        }

        public String methodWithInt(int i) {
            return "int:" + i;
        }

        public String methodWithString(String s) {
            return "str:" + s;
        }

        public String publicMethod() {
            return "public";
        }
    }

    /**
     * Bean whose method throws an unchecked exception to test InvocationTargetException handling.
     */
    static class ExceptionThrowingBean {

        public void explode() {
            throw new IllegalStateException("illegal state");
        }
    }

    /**
     * Simple holder for parameterized test data.
     */
    private static class ArgumentsHolder {

        final String methodName;

        final Object[] args;

        final String expected;

        ArgumentsHolder(String methodName, Object[] args, String expected) {
            this.methodName = methodName;
            this.args = args;
            this.expected = expected;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        bean = new TestBean();
    }

    private static Stream<ArgumentsHolder> validCallsProvider() throws Exception {
        return Stream.of(new ArgumentsHolder("greet", new Object[] { "World" }, "Hello World"), new ArgumentsHolder("methodWithInt", new Object[] { 42 }, "int:42"), new ArgumentsHolder("methodWithString", new Object[] { "foo" }, "str:foo"));
    }

    @Test
    void call_WithNullMethod_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(bean, null));
        assertTrue(ex.getMessage().contains("null Method passed to ReflectionUtils.call()"));
        assertTrue(ex.getMessage().contains(bean.getClass().getName()));
    }

    @Test
    void call_WithNullBean_ThrowsIllegalArgumentException() throws NoSuchMethodException {
        Method method = TestBean.class.getMethod("greet", String.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(null, method, "World"));
        assertTrue(ex.getMessage().contains("Cannot call [greet()] on a null object."));
    }

    @Test
    void call_WithValidPublicMethod_ReturnsResult() throws Exception {
        Method method = TestBean.class.getMethod("greet", String.class);
        Object result = ReflectionUtils.call(bean, method, "World");
        assertEquals("Hello World", result);
    }

    @Test
    void call_WithInaccessibleMethod_ThrowsRuntimeExceptionWrappingIllegalAccess() throws Exception {
        // private, not set accessible
        Method privateMethod = TestBean.class.getDeclaredMethod("secret");
        RuntimeException re = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(bean, privateMethod));
        assertTrue(re.getMessage().contains("IllegalAccessException occurred attempting to reflectively call method: secret()"));
        assertTrue(re.getCause() instanceof IllegalAccessException);
    }

    @Test
    void call_MethodThrowsException_ThrowsRuntimeExceptionWrappingTargetException() throws Exception {
        Method explodeMethod = TestBean.class.getMethod("explode");
        RuntimeException re = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(bean, explodeMethod));
        assertTrue(re.getMessage().contains("Exception thrown inside reflectively called method: explode()"));
        assertNotNull(re.getCause());
        assertEquals("boom", re.getCause().getMessage());
    }

    @Test
    void call_WithNullMethodAndNonNullBean_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(bean, null));
        // Message must contain the generic prefix and the bean's class name.
        assertTrue(ex.getMessage().contains("null Method passed to ReflectionUtils.call()"));
        assertTrue(ex.getMessage().contains(bean.getClass().getName()));
    }

    @Test
    void call_WithNullMethodAndNullBean_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(null, null));
        assertTrue(ex.getMessage().contains("null Method passed to ReflectionUtils.call()"));
        assertTrue(ex.getMessage().contains("null bean"));
    }

    @Test
    void call_WithInaccessiblePrivateMethod_ThrowsRuntimeExceptionWrappingIllegalAccess() throws Exception {
        // Obtain the private method without making it accessible.
        Method privateMethod = TestBean.class.getDeclaredMethod("secret");
        RuntimeException re = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(bean, privateMethod));
        assertTrue(re.getMessage().contains("IllegalAccessException occurred attempting to reflectively call method: secret()"));
        assertTrue(re.getCause() instanceof IllegalAccessException);
    }

    @Test
    void call_MethodThrowsUncheckedException_ThrowsRuntimeExceptionWrappingTargetException() throws Exception {
        Method explodeMethod = ExceptionThrowingBean.class.getMethod("explode");
        ExceptionThrowingBean exceptionBean = new ExceptionThrowingBean();
        RuntimeException re = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(exceptionBean, explodeMethod));
        assertTrue(re.getMessage().contains("Exception thrown inside reflectively called method: explode()"));
        assertNotNull(re.getCause());
        assertTrue(re.getCause() instanceof IllegalStateException);
        assertEquals("illegal state", re.getCause().getMessage());
    }

    @Test
    void call_WithArgumentTypeMismatch_PropagatesIllegalArgumentException() throws Exception {
        Method method = TestBean.class.getMethod("methodWithInt", int.class);
        // Passing a String where an int is expected triggers IllegalArgumentException from Method.invoke.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(bean, method, "notAnInt"));
        assertTrue(ex.getMessage().contains("argument type mismatch"));
    }

    @Test
    void call_WithNullArgsArrayAndMethodExpectingParams_PropagatesNullPointerException() throws Exception {
        Method method = TestBean.class.getMethod("methodWithString", String.class);
        // Passing null as the var‑args array leads to a NPE inside Method.invoke.
        NullPointerException npe = assertThrows(NullPointerException.class, () -> ReflectionUtils.call(bean, method, (Object[]) null));
        // The exact message is JVM‑dependent; we only assert the type.
        assertNotNull(npe);
    }

    @ParameterizedTest
    @MethodSource("validCallsProvider")
    void call_WithValidPublicMethods_ReturnsExpectedResult(ArgumentsHolder holder) throws Exception {
        Method method = TestBean.class.getMethod(holder.methodName, holder.args[0] instanceof Integer ? int.class : String.class);
        Object result = ReflectionUtils.call(bean, method, holder.args);
        assertEquals(holder.expected, result);
    }
}
