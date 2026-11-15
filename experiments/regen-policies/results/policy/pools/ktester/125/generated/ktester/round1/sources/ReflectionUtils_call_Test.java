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

    /**
     * A simple target class used by the original tests.
     */
    static class SimpleTarget {

        String toString() {
            return "simple";
        }

        boolean equals(Object o) {
            return this == o;
        }

        // Private method – will trigger IllegalAccessException when invoked
        // without setAccessible(true).
        private String privateMethod() {
            return "secret";
        }

        // Method that deliberately throws an exception.
        String explodingMethod() {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * A richer dummy class that provides the methods required by the JSON‑driven
     * test cases (return values, var‑args, static method, checked/unchecked
     * exceptions, etc.).
     */
    static class Dummy {

        private boolean flag;

        public void noArgs() {
            // no‑op
        }

        private String privateMethod() {
            return "private";
        }

        public void throwsChecked() throws Exception {
            throw new Exception("checked");
        }

        public String returnsString(String s) {
            return "echo:" + s;
        }

        public int add(int a, int b) {
            return a + b;
        }

        public void setFlag(boolean flag) {
            this.flag = flag;
        }

        public String concat(String... parts) {
            StringBuilder sb = new StringBuilder();
            for (String p : parts) {
                sb.append(p);
            }
            return sb.toString();
        }

        public String echo(String s) {
            return "echo:" + s;
        }

        public void throwsRuntime() {
            throw new RuntimeException("runtime boom");
        }

        public static String staticMethod() {
            return "static";
        }
    }

    private static Stream<Arguments> provideReturnTypeAndArgumentCases() throws NoSuchMethodException {
        Dummy dummy = new Dummy();
        return Stream.of(// Primitive int return (autoboxed)
        Arguments.of("Primitive int return (autoboxed)", dummy, Dummy.class.getMethod("add", int.class, int.class), new Object[] { 2, 3 }, 5), // Void method should return null
        Arguments.of("Void method returns null", dummy, Dummy.class.getMethod("setFlag", boolean.class), new Object[] { true }, null), // Var‑args method
        Arguments.of("Var‑args concatenation", dummy, Dummy.class.getMethod("concat", String[].class), new Object[] { new String[] { "a", "b", "c" } }, "abc"), // Method receiving a null argument
        Arguments.of("Method receives null argument", dummy, Dummy.class.getMethod("echo", String.class), new Object[] { null }, "echo:null"));
    }

    @Test
    void callInvokesMethodSuccessfully() throws NoSuchMethodException {
        SimpleTarget target = new SimpleTarget();
        Method method = SimpleTarget.class.getMethod("toString");
        Object result = ReflectionUtils.call(target, method);
        assertEquals("simple", result);
    }

    @Test
    void callInvokesMethodWithArgumentsSuccessfully() throws NoSuchMethodException {
        SimpleTarget target = new SimpleTarget();
        Method method = SimpleTarget.class.getMethod("equals", Object.class);
        Object result = ReflectionUtils.call(target, method, target);
        assertTrue((Boolean) result);
    }

    @Test
    void callRejectsNullMethod() {
        SimpleTarget target = new SimpleTarget();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(target, null));
        assertTrue(ex.getMessage().contains("null Method passed"));
    }

    @Test
    void callRejectsNullInstance() throws NoSuchMethodException {
        Method method = SimpleTarget.class.getMethod("toString");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(null, method));
        assertTrue(ex.getMessage().contains("Cannot call [toString] on a null object"));
    }

    @Test
    void callWrapsIllegalAccessException() throws NoSuchMethodException {
        SimpleTarget target = new SimpleTarget();
        // Obtain the private method without making it accessible.
        Method privateMethod = SimpleTarget.class.getDeclaredMethod("privateMethod");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(target, privateMethod));
        assertTrue(ex.getCause() instanceof IllegalAccessException);
        assertTrue(ex.getMessage().contains("IllegalAccessException occurred"));
    }

    @Test
    void callWrapsInvocationTargetException() throws NoSuchMethodException {
        SimpleTarget target = new SimpleTarget();
        Method exploding = SimpleTarget.class.getMethod("explodingMethod");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(target, exploding));
        // The cause should be the original IllegalStateException.
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertEquals("boom", ex.getCause().getMessage());
        assertTrue(ex.getMessage().contains("Exception thrown inside reflectively called method"));
    }

    // ----------------------------------------------------------------------
    @Test
    void callBothInstanceAndMethodNull_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(null, null));
        assertTrue(ex.getMessage().contains("null Method passed"));
    }

    @Test
    void callNullMethodWithNonNullInstance_throwsIllegalArgumentException() {
        Dummy dummy = new Dummy();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(dummy, null));
        assertTrue(ex.getMessage().contains("null Method passed"));
    }

    @Test
    void callNullInstanceWithValidMethod_throwsIllegalArgumentException() throws NoSuchMethodException {
        Method method = Dummy.class.getMethod("noArgs");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(null, method));
        assertTrue(ex.getMessage().contains("Cannot call [noArgs] on a null object"));
    }

    @Test
    void callMethodThrowsCheckedException_wrappedInRuntimeException() throws NoSuchMethodException {
        Dummy dummy = new Dummy();
        Method method = Dummy.class.getMethod("throwsChecked");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(dummy, method));
        assertTrue(ex.getCause() instanceof Exception);
        assertEquals("checked", ex.getCause().getMessage());
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideReturnTypeAndArgumentCases")
    void returnTypesAndArguments(String description, Dummy instance, Method method, Object[] args, Object expected) throws Exception {
        Object result = ReflectionUtils.call(instance, method, args);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    @Test
    void callUncheckedExceptionWrappedInRuntimeException() throws NoSuchMethodException {
        Dummy dummy = new Dummy();
        Method method = Dummy.class.getMethod("throwsRuntime");
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(dummy, method));
        assertTrue(ex.getCause() instanceof RuntimeException);
        assertEquals("runtime boom", ex.getCause().getMessage());
    }

    // ----------------------------------------------------------------------
    @Test
    void callStaticMethodViaInstanceReturnsResult() throws NoSuchMethodException {
        // instance is ignored for static method
        Dummy dummy = new Dummy();
        Method staticMethod = Dummy.class.getMethod("staticMethod");
        Object result = ReflectionUtils.call(dummy, staticMethod);
        assertEquals("static", result);
    }

    // ----------------------------------------------------------------------
    @Test
    void callWithInsufficientArguments_throwsIllegalArgumentException() throws NoSuchMethodException {
        Dummy dummy = new Dummy();
        Method method = Dummy.class.getMethod("add", int.class, int.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(dummy, method, 1));
        assertTrue(ex.getMessage().contains("argument type mismatch"));
    }

    @Test
    void callWithWrongArgumentType_throwsIllegalArgumentException() throws NoSuchMethodException {
        Dummy dummy = new Dummy();
        Method method = Dummy.class.getMethod("add", int.class, int.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(dummy, method, "notAnInt", 2));
        assertTrue(ex.getMessage().contains("argument type mismatch"));
    }

    @Test
    void callWithNullForPrimitiveParameter_throwsIllegalArgumentException() throws NoSuchMethodException {
        Dummy dummy = new Dummy();
        Method method = Dummy.class.getMethod("setFlag", boolean.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(dummy, method, (Object) null));
        assertTrue(ex.getMessage().contains("argument type mismatch"));
    }

    // ----------------------------------------------------------------------
    @Test
    void callMethodOnWrongInstanceType_throwsIllegalArgumentException() throws NoSuchMethodException {
        Object wrongInstance = new Object();
        Method method = Dummy.class.getMethod("noArgs");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(wrongInstance, method));
        assertTrue(ex.getMessage().contains("object is not an instance of declaring class"));
    }

    // ----------------------------------------------------------------------
    @Test
    void callHappyPathReturnsString() throws NoSuchMethodException {
        Dummy dummy = new Dummy();
        Method method = Dummy.class.getMethod("returnsString", String.class);
        Object result = ReflectionUtils.call(dummy, method, "test");
        assertEquals("echo:test", result);
    }
}
