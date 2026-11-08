package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_call_Test {

    /**
     * Simple class with a variety of methods used for reflective calls.
     */
    static class SampleClass {

        public int add(int a, int b) {
            return a + b;
        }

        public String noArgs() {
            return "noArgsCalled";
        }

        public static String staticMethod() {
            return "staticCalled";
        }

        public void voidMethod() {
            // intentionally empty – returns void
        }

        public boolean isPositive(int value) {
            return value > 0;
        }

        public String concatAll(String... parts) {
            StringBuilder sb = new StringBuilder();
            for (String p : parts) sb.append(p);
            return sb.toString();
        }

        public String echo(Object o) {
            return (String) o;
        }

        public void throwsException() {
            throw new IllegalStateException("boom");
        }

        public void throwsNullPointerException() {
            throw new NullPointerException("null boom");
        }

        private String privateMethod() {
            return "private";
        }
    }

    /**
     * Superclass containing a method that will be invoked via a subclass instance.
     */
    static class SuperClass {

        public String superMethod() {
            return "superCalled";
        }
    }

    /**
     * Subclass used to test inherited method invocation.
     */
    static class SubSampleClass extends SuperClass {
        // No additional members – inherits superMethod()
    }

    /**
     * Simple class used in the original test suite (kept for compatibility).
     */
    static class TestTarget {

        public String echo(String msg) {
            return "echo:" + msg;
        }

        private String hidden() {
            return "hidden";
        }

        public void explode() throws Exception {
            throw new Exception("boom");
        }
    }

    private static Stream<Arguments> successfulInvocationProvider() throws NoSuchMethodException {
        SampleClass sample = new SampleClass();
        return Stream.of(Arguments.of("add(int,int) returns sum", SampleClass.class.getMethod("add", int.class, int.class), sample, new Object[] { 3, 4 }, 7), Arguments.of("noArgs() returns fixed string", SampleClass.class.getMethod("noArgs"), sample, new Object[] {}, "noArgsCalled"));
    }

    @Test
    void callInvokesPublicMethodSuccessfully() throws NoSuchMethodException {
        TestTarget target = new TestTarget();
        Method method = TestTarget.class.getMethod("echo", String.class);
        Object result = ReflectionUtils.call(target, method, "hello");
        assertEquals("echo:hello", result);
    }

    @Test
    void callThrowsIllegalArgumentExceptionWhenMethodIsNull() {
        TestTarget target = new TestTarget();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(target, null));
        assertTrue(ex.getMessage().contains("null Method passed"));
        assertTrue(ex.getMessage().contains(target.getClass().getName()));
    }

    @Test
    void callThrowsIllegalArgumentExceptionWhenInstanceIsNull() throws NoSuchMethodException {
        Method method = TestTarget.class.getMethod("echo", String.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(null, method));
        assertTrue(ex.getMessage().contains("Cannot call [echo]"));
    }

    @Test
    void callWrapsIllegalAccessExceptionInRuntimeException() throws NoSuchMethodException {
        TestTarget target = new TestTarget();
        // Obtain a private method without making it accessible
        Method privateMethod = TestTarget.class.getDeclaredMethod("hidden");
        RuntimeException re = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(target, privateMethod));
        assertTrue(re.getMessage().contains("IllegalAccessException"));
        assertTrue(re.getCause() instanceof IllegalAccessException);
    }

    @Test
    void callWrapsInvocationTargetExceptionCause() throws NoSuchMethodException {
        TestTarget target = new TestTarget();
        Method method = TestTarget.class.getMethod("explode");
        RuntimeException re = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(target, method));
        assertTrue(re.getMessage().contains("Exception thrown inside reflectively called method"));
        // The cause should be the original Exception thrown by explode()
        Throwable cause = re.getCause();
        assertNotNull(cause);
        assertEquals("boom", cause.getMessage());
        assertTrue(cause instanceof Exception);
    }

    // -----------------------------------------------------------------
    @Test
    void nullMethodThrowsIllegalArgumentException() {
        SampleClass instance = new SampleClass();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(instance, null));
        assertTrue(ex.getMessage().contains("null Method passed"));
        assertTrue(ex.getMessage().contains(instance.getClass().getName()));
    }

    // -----------------------------------------------------------------
    @Test
    void nullInstanceThrowsIllegalArgumentException() throws NoSuchMethodException {
        Method method = SampleClass.class.getMethod("add", int.class, int.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(null, method));
        assertTrue(ex.getMessage().contains("Cannot call [add]"));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("successfulInvocationProvider")
    void successfulInvocation(String description, Method method, Object instance, Object[] args, Object expected) throws Exception {
        Object result = ReflectionUtils.call(instance, method, args);
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------
    @Test
    void illegalAccessExceptionIsWrapped() throws NoSuchMethodException {
        SampleClass instance = new SampleClass();
        Method privateMethod = SampleClass.class.getDeclaredMethod("privateMethod");
        RuntimeException re = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, privateMethod));
        assertTrue(re.getMessage().contains("IllegalAccessException"));
        assertTrue(re.getCause() instanceof IllegalAccessException);
    }

    // -----------------------------------------------------------------
    @Test
    void invocationTargetExceptionIsWrappedWithOriginalCause() throws NoSuchMethodException {
        SampleClass instance = new SampleClass();
        Method method = SampleClass.class.getMethod("throwsException");
        RuntimeException re = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, method));
        assertTrue(re.getMessage().contains("Exception thrown inside reflectively called method"));
        assertNotNull(re.getCause());
        assertTrue(re.getCause() instanceof IllegalStateException);
        assertEquals("boom", re.getCause().getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    void staticMethodCanBeInvokedViaInstanceArgument() throws NoSuchMethodException {
        // instance is ignored for static method
        SampleClass instance = new SampleClass();
        Method staticMethod = SampleClass.class.getMethod("staticMethod");
        Object result = ReflectionUtils.call(instance, staticMethod);
        assertEquals("staticCalled", result);
    }

    // -----------------------------------------------------------------
    @Test
    void voidMethodReturnsNull() throws NoSuchMethodException {
        SampleClass instance = new SampleClass();
        Method voidMethod = SampleClass.class.getMethod("voidMethod");
        Object result = ReflectionUtils.call(instance, voidMethod);
        assertNull(result);
    }

    // -----------------------------------------------------------------
    @Test
    void primitiveBooleanReturnIsBoxed() throws NoSuchMethodException {
        SampleClass instance = new SampleClass();
        Method method = SampleClass.class.getMethod("isPositive", int.class);
        Object result = ReflectionUtils.call(instance, method, 5);
        assertTrue(result instanceof Boolean);
        assertEquals(Boolean.TRUE, result);
    }

    // -----------------------------------------------------------------
    @Test
    void varargsMethodHandlesSeparateArguments() throws NoSuchMethodException {
        SampleClass instance = new SampleClass();
        Method concatAll = SampleClass.class.getMethod("concatAll", String[].class);
        Object result = ReflectionUtils.call(instance, concatAll, "a", "b", "c");
        assertEquals("abc", result);
    }

    // -----------------------------------------------------------------
    @Test
    void argumentMismatchPropagatesIllegalArgumentException() throws NoSuchMethodException {
        SampleClass instance = new SampleClass();
        Method addMethod = SampleClass.class.getMethod("add", int.class, int.class);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, // only one argument
        () -> ReflectionUtils.call(instance, addMethod, 1));
        // The exact JVM message may vary, but it should mention argument type mismatch
        assertTrue(ex.getMessage().toLowerCase().contains("argument"));
    }

    // -----------------------------------------------------------------
    @Test
    void nullArgsArrayIsTreatedAsNoArguments() throws NoSuchMethodException {
        SampleClass instance = new SampleClass();
        Method noArgs = SampleClass.class.getMethod("noArgs");
        Object result = ReflectionUtils.call(instance, noArgs, (Object[]) null);
        assertEquals("noArgsCalled", result);
    }

    // -----------------------------------------------------------------
    @Test
    void methodAcceptingObjectReturnsNullWhenArgumentIsNull() throws NoSuchMethodException {
        SampleClass instance = new SampleClass();
        Method echo = SampleClass.class.getMethod("echo", Object.class);
        Object result = ReflectionUtils.call(instance, echo, (Object) null);
        assertNull(result);
    }

    // -----------------------------------------------------------------
    @Test
    void inheritedMethodCanBeInvokedViaSubclassInstance() throws NoSuchMethodException {
        SubSampleClass instance = new SubSampleClass();
        Method superMethod = SubSampleClass.class.getMethod("superMethod");
        Object result = ReflectionUtils.call(instance, superMethod);
        assertEquals("superCalled", result);
    }

    // -----------------------------------------------------------------
    @Test
    void invocationTargetExceptionWithNullPointerExceptionIsWrapped() throws NoSuchMethodException {
        SampleClass instance = new SampleClass();
        Method method = SampleClass.class.getMethod("throwsNullPointerException");
        RuntimeException re = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, method));
        assertTrue(re.getMessage().contains("Exception thrown inside reflectively called method"));
        assertNotNull(re.getCause());
        assertTrue(re.getCause() instanceof NullPointerException);
        assertEquals("null boom", re.getCause().getMessage());
    }
}
