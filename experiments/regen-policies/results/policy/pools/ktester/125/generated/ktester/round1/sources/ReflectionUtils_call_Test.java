package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_call_Test {

    /* --------------------------------------------------------------------- */
    static class Sample {

        public String greet() {
            return "hello";
        }

        public String greet(String name) {
            return "Hello " + name;
        }

        private String secret() {
            return "shh";
        }

        public void explode() {
            throw new IllegalStateException("boom");
        }

        public void noArgs() {
            // void method – nothing to do
        }

        public static void staticMethod() {
            // static method – nothing to do
        }

        public int add(int a, int b) {
            return a + b;
        }

        public void throwChecked() throws IOException {
            throw new IOException("checked");
        }
    }

    /**
     * Unrelated class used for the “instance type mismatch” scenario.
     */
    static class Other {

        public String greet() {
            return "other";
        }
    }

    private static Stream<Arguments> nullMethodProvider() throws NoSuchMethodException {
        Sample sample = new Sample();
        return Stream.of(Arguments.of(sample, null));
    }

    private static Stream<Arguments> nullInstanceProvider() throws NoSuchMethodException {
        Method greetWithArg = Sample.class.getMethod("greet", String.class);
        return Stream.of(Arguments.of(greetWithArg));
    }

    private static Stream<Arguments> successfulInvocationProvider() throws NoSuchMethodException {
        Sample sample = new Sample();
        Method greetWithArg = Sample.class.getMethod("greet", String.class);
        Method noArgs = Sample.class.getMethod("noArgs");
        return Stream.of(// greet(String) → "Hello Alice"
        Arguments.of(sample, greetWithArg, new Object[] { "Alice" }, "Hello Alice"), // void method → null
        Arguments.of(sample, noArgs, new Object[] {}, null));
    }

    private static Stream<Arguments> methodThrowsExceptionProvider() throws NoSuchMethodException {
        Sample sample = new Sample();
        Method explode = Sample.class.getMethod("explode");
        return Stream.of(Arguments.of(sample, explode));
    }

    private static Stream<Arguments> illegalAccessProvider() throws NoSuchMethodException {
        Sample sample = new Sample();
        Method secret = Sample.class.getDeclaredMethod("secret");
        return Stream.of(Arguments.of(sample, secret));
    }

    private static Stream<Arguments> argumentMismatchProvider() throws NoSuchMethodException {
        Sample sample = new Sample();
        Method greetWithArg = Sample.class.getMethod("greet", String.class);
        Method add = Sample.class.getMethod("add", int.class, int.class);
        return Stream.of(// Too few arguments for greet(String)
        Arguments.of(sample, greetWithArg, new Object[] {}), // Too few arguments for add(int,int)
        Arguments.of(sample, add, new Object[] { 1 }));
    }

    private static Stream<Arguments> staticMethodNullInstanceProvider() throws NoSuchMethodException {
        Method staticM = Sample.class.getMethod("staticMethod");
        return Stream.of(Arguments.of(staticM));
    }

    private static Stream<Arguments> instanceTypeMismatchProvider() throws NoSuchMethodException {
        Other other = new Other();
        Method greetWithArg = Sample.class.getMethod("greet", String.class);
        return Stream.of(Arguments.of(other, greetWithArg, new Object[] { "Bob" }));
    }

    private static Stream<Arguments> argumentTypeMismatchProvider() throws NoSuchMethodException {
        Sample sample = new Sample();
        Method add = Sample.class.getMethod("add", int.class, int.class);
        return Stream.of(Arguments.of(sample, add, new Object[] { "one", "two" }));
    }

    private static Stream<Arguments> checkedExceptionProvider() throws NoSuchMethodException {
        Sample sample = new Sample();
        Method throwChecked = Sample.class.getMethod("throwChecked");
        return Stream.of(Arguments.of(sample, throwChecked));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testCallSuccess() throws NoSuchMethodException {
        Sample instance = new Sample();
        Method method = Sample.class.getMethod("greet");
        Object result = ReflectionUtils.call(instance, method);
        assertEquals("hello", result);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testCallNullMethod() {
        Sample instance = new Sample();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(instance, null));
        assertTrue(ex.getMessage().contains("null Method passed"));
        assertTrue(ex.getMessage().contains(Sample.class.getName()));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testCallNullInstance() throws NoSuchMethodException {
        Method method = Sample.class.getMethod("greet");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(null, method));
        assertTrue(ex.getMessage().contains("Cannot call [greet]"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testCallIllegalAccess() throws NoSuchMethodException {
        Sample instance = new Sample();
        Method privateMethod = Sample.class.getDeclaredMethod("secret");
        // Do NOT set accessible – we want the IllegalAccessException path.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, privateMethod));
        assertTrue(ex.getMessage().contains("IllegalAccessException"));
        assertTrue(ex.getCause() instanceof IllegalAccessException);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testCallInvocationTargetException() throws NoSuchMethodException {
        Sample instance = new Sample();
        Method exploding = Sample.class.getMethod("explode");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, exploding));
        assertTrue(ex.getMessage().contains("Exception thrown inside reflectively called method"));
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertEquals("boom", ex.getCause().getMessage());
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Null Method")
    @MethodSource("nullMethodProvider")
    void testNullMethodParameterized(Sample instance, Method method) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(instance, method));
        assertTrue(ex.getMessage().contains("null Method passed"));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Null Instance")
    @MethodSource("nullInstanceProvider")
    void testNullInstanceParameterized(Method method) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(null, method, "World"));
        assertTrue(ex.getMessage().contains("Cannot call [greet]"));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Successful Invocation")
    @MethodSource("successfulInvocationProvider")
    void testSuccessfulInvocation(Object instance, Method method, Object[] args, Object expected) {
        Object result = ReflectionUtils.call(instance, method, args);
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Method Throws Exception")
    @MethodSource("methodThrowsExceptionProvider")
    void testMethodThrowsException(Sample instance, Method method) {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, method));
        assertTrue(ex.getMessage().contains("Exception thrown inside reflectively called method"));
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertEquals("boom", ex.getCause().getMessage());
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] IllegalAccessException Handling")
    @MethodSource("illegalAccessProvider")
    void testIllegalAccessException(Sample instance, Method method) {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, method));
        assertTrue(ex.getMessage().contains("IllegalAccessException"));
        assertTrue(ex.getCause() instanceof IllegalAccessException);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Argument Mismatch")
    @MethodSource("argumentMismatchProvider")
    void testArgumentMismatch(Sample instance, Method method, Object[] args) {
        // Method.invoke throws IllegalArgumentException which propagates
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(instance, method, args));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Static Method with Null Instance")
    @MethodSource("staticMethodNullInstanceProvider")
    void testStaticMethodNullInstance(Method staticMethod) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(null, staticMethod));
        assertTrue(ex.getMessage().contains("Cannot call [staticMethod]"));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Instance Type Mismatch")
    @MethodSource("instanceTypeMismatchProvider")
    void testInstanceTypeMismatch(Other otherInstance, Method method, Object[] args) {
        // Method.invoke will detect that 'otherInstance' is not assignable to Sample
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(otherInstance, method, args));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Argument Type Mismatch")
    @MethodSource("argumentTypeMismatchProvider")
    void testArgumentTypeMismatch(Sample instance, Method method, Object[] args) {
        // Method.invoke throws IllegalArgumentException for type mismatch
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(instance, method, args));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Checked Exception Propagation")
    @MethodSource("checkedExceptionProvider")
    void testCheckedExceptionPropagation(Sample instance, Method method) {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, method));
        assertTrue(ex.getMessage().contains("Exception thrown inside reflectively called method"));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("checked", ex.getCause().getMessage());
    }
}
