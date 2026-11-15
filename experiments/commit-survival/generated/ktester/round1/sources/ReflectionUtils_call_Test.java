package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_call_Test {

    private static TestBean sharedBean;

    class ArgBean {

        String concat(String a, String b) {
            return a + b;
        }
    }

    /**
     * Bean that contains the majority of methods exercised by the tests.
     */
    static class TestClass {

        public String publicNoArg() {
            return "ok";
        }

        public String concat(String a, String b) {
            return a + b;
        }

        public static int staticAdd(int a, int b) {
            return a + b;
        }

        public void throwsChecked() throws Exception {
            throw new Exception("checked");
        }

        public void throwsRuntime() {
            throw new IllegalStateException("runtime");
        }

        private String privateMethod() {
            return "secret";
        }

        public Object echo(Object o) {
            return o;
        }

        public int increment(int i) {
            return i + 1;
        }

        public String publicMethod() {
            return "publicMethod";
        }
    }

    /**
     * Bean used for the “bean type mismatch” scenario.
     */
    static class AnotherClass {

        public String anotherMethod() {
            return "another";
        }
    }

    /* --------------------------------------------------------------------- */
    private static final class Expected {

        // expected return value (may be null)
        final Object value;

        // expected exception type (null if success)
        final Class<? extends Throwable> ex;

        // part of the expected message (may be null)
        final String msgFragment;

        private Expected(Object value) {
            this.value = value;
            this.ex = null;
            this.msgFragment = null;
        }

        private Expected(Class<? extends Throwable> ex, String msgFragment) {
            this.value = null;
            this.ex = ex;
            this.msgFragment = msgFragment;
        }

        static Expected value(Object v) {
            return new Expected(v);
        }

        static Expected exception(Class<? extends Throwable> ex, String fragment) {
            return new Expected(ex, fragment);
        }
    }

    /**
     * Simple bean used for successful invocation and exception scenarios.
     */
    static class TestBean {

        private int value = 42;

        public int getValue() {
            return value;
        }

        private String privateMethod() {
            return "secret";
        }

        public void methodThatThrows() {
            throw new IllegalStateException("boom");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // shared immutable bean for tests that do not modify state
        sharedBean = new TestBean();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static reference
        sharedBean = null;
    }

    static Stream<Arguments> nullChecksProvider() throws NoSuchMethodException {
        TestClass bean = new TestClass();
        Method publicMethod = TestClass.class.getMethod("publicMethod");
        return Stream.of(// null Method
        Arguments.of("null Method argument", bean, null, new Object[] {}, Expected.exception(IllegalArgumentException.class, "null Method passed")), // null bean
        Arguments.of("null bean argument", null, publicMethod, new Object[] {}, Expected.exception(IllegalArgumentException.class, "Cannot call [publicMethod]")));
    }

    static Stream<Arguments> successfulCallsProvider() throws NoSuchMethodException {
        TestClass bean = new TestClass();
        Method publicNoArg = TestClass.class.getMethod("publicNoArg");
        Method concat = TestClass.class.getMethod("concat", String.class, String.class);
        // static, but we obtain via class
        Method staticAdd = TestClass.class.getMethod("staticAdd", int.class, int.class);
        return Stream.of(Arguments.of("public no‑arg method returns \"ok\"", bean, publicNoArg, new Object[] {}, Expected.value("ok")), Arguments.of("concat with two strings returns concatenated value", bean, concat, new Object[] { "Hello", "World" }, Expected.value("HelloWorld")), Arguments.of("static method invoked via instance returns arithmetic result", bean, staticAdd, new Object[] { 3, 7 }, Expected.value(10)));
    }

    static Stream<Arguments> exceptionPropagationProvider() throws NoSuchMethodException {
        TestClass bean = new TestClass();
        Method throwsChecked = TestClass.class.getMethod("throwsChecked");
        Method throwsRuntime = TestClass.class.getMethod("throwsRuntime");
        return Stream.of(Arguments.of("method throws checked Exception – should be wrapped", bean, throwsChecked, new Object[] {}, Expected.exception(RuntimeException.class, "Exception thrown inside reflectively called method: throwsChecked")), Arguments.of("method throws RuntimeException – should be wrapped", bean, throwsRuntime, new Object[] {}, Expected.exception(RuntimeException.class, "Exception thrown inside reflectively called method: throwsRuntime")));
    }

    static Stream<Arguments> illegalAccessProvider() throws NoSuchMethodException {
        TestClass bean = new TestClass();
        // not setAccessible
        Method privateMethod = TestClass.class.getDeclaredMethod("privateMethod");
        return Stream.of(Arguments.of("invoke private method without making it accessible", bean, privateMethod, new Object[] {}, Expected.exception(RuntimeException.class, "IllegalAccessException occurred attempting to reflectively call method: privateMethod")));
    }

    static Stream<Arguments> nullArgumentProvider() throws NoSuchMethodException {
        TestClass bean = new TestClass();
        Method echo = TestClass.class.getMethod("echo", Object.class);
        return Stream.of(Arguments.of("echo(null) should return null", bean, echo, new Object[] { null }, Expected.value(null)));
    }

    static Stream<Arguments> primitiveArgumentProvider() throws NoSuchMethodException {
        TestClass bean = new TestClass();
        Method increment = TestClass.class.getMethod("increment", int.class);
        return Stream.of(Arguments.of("increment(41) should return 42", bean, increment, new Object[] { 41 }, Expected.value(42)));
    }

    static Stream<Arguments> argumentCountMismatchProvider() throws NoSuchMethodException {
        TestClass bean = new TestClass();
        Method concat = TestClass.class.getMethod("concat", String.class, String.class);
        Method publicNoArg = TestClass.class.getMethod("publicNoArg");
        return Stream.of(Arguments.of("too few arguments for concat", bean, concat, new Object[] { "onlyOne" }, Expected.exception(IllegalArgumentException.class, "wrong number of arguments")), Arguments.of("too many arguments for publicNoArg", bean, publicNoArg, new Object[] { 1 }, Expected.exception(IllegalArgumentException.class, "wrong number of arguments")));
    }

    static Stream<Arguments> argumentTypeMismatchProvider() throws NoSuchMethodException {
        TestClass bean = new TestClass();
        Method concat = TestClass.class.getMethod("concat", String.class, String.class);
        Method increment = TestClass.class.getMethod("increment", int.class);
        return Stream.of(Arguments.of("integer supplied where String expected", bean, concat, new Object[] { 123, "World" }, Expected.exception(IllegalArgumentException.class, "argument type mismatch")), Arguments.of("null supplied for primitive int", bean, increment, new Object[] { null }, Expected.exception(IllegalArgumentException.class, "argument type mismatch")));
    }

    static Stream<Arguments> beanTypeMismatchProvider() throws NoSuchMethodException {
        TestClass bean = new TestClass();
        Method anotherMethod = AnotherClass.class.getMethod("anotherMethod");
        return Stream.of(Arguments.of("invoke method from AnotherClass on TestClass instance", bean, anotherMethod, new Object[] {}, Expected.exception(IllegalArgumentException.class, "object is not an instance of")));
    }

    /* --------------------------------------------------------------------- */
    private void executeAndAssert(Object bean, Method method, Object[] args, Expected expected) {
        if (expected.ex != null) {
            Throwable thrown = assertThrows(expected.ex, () -> ReflectionUtils.call(bean, method, args));
            assertTrue(thrown.getMessage().contains(expected.msgFragment), () -> "Expected message to contain [" + expected.msgFragment + "] but was [" + thrown.getMessage() + "]");
        } else {
            try {
                Object result = ReflectionUtils.call(bean, method, args);
                assertEquals(expected.value, result, () -> "Expected return value [" + expected.value + "] but got [" + result + "]");
            } catch (Throwable t) {
                fail("Did not expect an exception, but got: " + t, t);
            }
        }
    }

    @Test
    void call_WithValidMethod_ReturnsResult() throws Exception {
        // given
        Method method = TestBean.class.getMethod("getValue");
        // when
        Object result = ReflectionUtils.call(sharedBean, method);
        // then
        assertEquals(42, result);
    }

    @Test
    void call_WithNullMethod_ThrowsIllegalArgumentException() {
        // given
        Method nullMethod = null;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(sharedBean, nullMethod));
        assertTrue(ex.getMessage().contains("null Method passed"));
        assertTrue(ex.getMessage().contains(TestBean.class.getName()));
    }

    @Test
    void call_WithNullBean_ThrowsIllegalArgumentException() throws Exception {
        // given
        Method method = TestBean.class.getMethod("getValue");
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(null, method));
        assertTrue(ex.getMessage().contains("Cannot call [getValue]"));
    }

    @Test
    void call_WithInaccessibleMethod_ThrowsRuntimeExceptionWrappingIllegalAccessException() throws Exception {
        // given
        Method privateMethod = TestBean.class.getDeclaredMethod("privateMethod");
        // do NOT make it accessible – this will cause IllegalAccessException when invoked
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(sharedBean, privateMethod));
        assertTrue(ex.getMessage().contains("IllegalAccessException occurred"));
        assertTrue(ex.getCause() instanceof IllegalAccessException);
    }

    @Test
    void call_MethodThrowsException_ThrowsRuntimeExceptionWrappingTargetException() throws Exception {
        // given
        Method throwingMethod = TestBean.class.getMethod("methodThatThrows");
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(sharedBean, throwingMethod));
        assertTrue(ex.getMessage().contains("Exception thrown inside reflectively called method"));
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertEquals("boom", ex.getCause().getMessage());
    }

    @Test
    void call_WithArguments_PassesThemCorrectly() throws Exception {
        // given a bean with a method that accepts arguments
        class ArgBean {

            String concat(String a, String b) {
                return a + b;
            }
        }
        ArgBean bean = new ArgBean();
        Method concat = ArgBean.class.getMethod("concat", String.class, String.class);
        // when
        Object result = ReflectionUtils.call(bean, concat, "Hello", "World");
        // then
        assertEquals("HelloWorld", result);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullChecksProvider")
    void call_NullChecks(String description, Object bean, Method method, Object[] args, Expected expected) {
        executeAndAssert(bean, method, args, expected);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulCallsProvider")
    void call_SuccessfulCalls(String description, Object bean, Method method, Object[] args, Expected expected) {
        executeAndAssert(bean, method, args, expected);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionPropagationProvider")
    void call_ExceptionPropagation(String description, Object bean, Method method, Object[] args, Expected expected) {
        executeAndAssert(bean, method, args, expected);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("illegalAccessProvider")
    void call_IllegalAccess(String description, Object bean, Method method, Object[] args, Expected expected) {
        executeAndAssert(bean, method, args, expected);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullArgumentProvider")
    void call_NullArgumentHandling(String description, Object bean, Method method, Object[] args, Expected expected) {
        executeAndAssert(bean, method, args, expected);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("primitiveArgumentProvider")
    void call_PrimitiveArgumentTypes(String description, Object bean, Method method, Object[] args, Expected expected) {
        executeAndAssert(bean, method, args, expected);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("argumentCountMismatchProvider")
    void call_ArgumentCountMismatch(String description, Object bean, Method method, Object[] args, Expected expected) {
        executeAndAssert(bean, method, args, expected);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("argumentTypeMismatchProvider")
    void call_ArgumentTypeMismatch(String description, Object bean, Method method, Object[] args, Expected expected) {
        executeAndAssert(bean, method, args, expected);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("beanTypeMismatchProvider")
    void call_BeanTypeMismatch(String description, Object bean, Method method, Object[] args, Expected expected) {
        executeAndAssert(bean, method, args, expected);
    }
}
