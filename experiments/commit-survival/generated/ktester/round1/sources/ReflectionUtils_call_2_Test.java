package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;

class ReflectionUtils_call_2_Test {

    /**
     * Simple bean used for the majority of test scenarios.
     */
    static class SampleBean {

        /**
         * Public method used for the "Successful invocation" test.
         */
        public String greet(String name) {
            return "Hello, " + name + "!";
        }

        /**
         * Private method used for the "Private method invocation" test.
         */
        private String privateGreet() {
            return "private";
        }

        /**
         * Method that throws an unchecked exception, used for the "Method throws exception" test.
         */
        public void explode() {
            throw new IllegalStateException("Boom!");
        }

        /**
         * Void method used for the "Void method returns null" test.
         */
        public void doSomething() {
            // intentionally empty
        }

        /**
         * No‑arg method returning a primitive int, used for the "No‑arg method" test.
         */
        public int answer() {
            return 42;
        }

        /**
         * Method with primitive int parameters, used for the "Primitive argument handling" test.
         */
        public int add(int a, int b) {
            return a + b;
        }

        /**
         * Static method invoked via an instance, used for the "Static method invocation" test.
         */
        public static String staticEcho(String s) {
            return s;
        }
    }

    /**
     * Bean with a public method that returns a value – kept from the original scaffold.
     */
    static class SimpleBean {

        String echo(String s) {
            return "echo:" + s;
        }
    }

    /**
     * Bean with a method that deliberately throws a checked exception – kept from the original scaffold.
     */
    static class ExceptionBean {

        void explode() throws IllegalArgumentException {
            throw new IllegalArgumentException("boom");
        }
    }

    @Test
    void callInvokesMethodAndReturnsResult() {
        // GIVEN
        SimpleBean bean = new SimpleBean();
        String methodName = "echo";
        Object[] args = new Object[] { "hello" };
        // WHEN
        Object result = ReflectionUtils.call(bean, methodName, args);
        // THEN
        assertEquals("echo:hello", result);
    }

    @Test
    void callWrapsInvocationTargetException() {
        // GIVEN
        ExceptionBean bean = new ExceptionBean();
        String methodName = "explode";
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(bean, methodName));
        // The RuntimeException should wrap the original IllegalArgumentException.
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertEquals("boom", ex.getCause().getMessage());
        // The message should contain the reflective call information.
        assertTrue(ex.getMessage().contains("Exception thrown inside reflectively called method"));
    }

    @Test
    void successfulInvocationReturnsExpectedValue() {
        // GIVEN
        SampleBean bean = new SampleBean();
        String methodName = "greet";
        Object[] args = new Object[] { "World" };
        // WHEN
        Object result = ReflectionUtils.call(bean, methodName, args);
        // THEN
        assertEquals("Hello, World!", result);
    }

    @Test
    void privateMethodInvocationIsWrappedInRuntimeException() {
        // GIVEN
        SampleBean bean = new SampleBean();
        String methodName = "privateGreet";
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(bean, methodName));
        // The wrapper should indicate an IllegalAccessException.
        assertTrue(ex.getMessage().contains("IllegalAccessException occurred attempting to reflectively call method: privateGreet"));
        assertTrue(ex.getCause() instanceof IllegalAccessException);
    }

    @Test
    void methodThatThrowsExceptionIsWrappedPreservingCause() {
        // GIVEN
        SampleBean bean = new SampleBean();
        String methodName = "explode";
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(bean, methodName));
        // The cause should be the original IllegalStateException with message "Boom!".
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertEquals("Boom!", ex.getCause().getMessage());
        // The outer RuntimeException message must contain the reflective‑call hint.
        assertTrue(ex.getMessage().contains("Exception thrown inside reflectively called method"));
    }

    @Test
    void methodNotFoundResultsInIllegalArgumentException() {
        // GIVEN
        SampleBean bean = new SampleBean();
        String methodName = "nonExistingMethod";
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(bean, methodName));
        // Verify that the exception message mentions the missing method name and arg count.
        assertTrue(ex.getMessage().contains("nonExistingMethod"));
    }

    @Test
    void nullBeanCausesNullPointerException() {
        // GIVEN
        Object bean = null;
        String methodName = "toString";
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ReflectionUtils.call(bean, methodName));
    }

    @Test
    void voidMethodReturnsNull() {
        // GIVEN
        SampleBean bean = new SampleBean();
        String methodName = "doSomething";
        // WHEN
        Object result = ReflectionUtils.call(bean, methodName);
        // THEN
        assertNull(result, "Void methods should return null from Method.invoke");
    }

    @Test
    void noArgMethodReturnsPrimitiveValue() {
        // GIVEN
        SampleBean bean = new SampleBean();
        String methodName = "answer";
        // WHEN
        Object result = ReflectionUtils.call(bean, methodName);
        // THEN
        assertEquals(42, result);
    }

    @Test
    void primitiveArgumentHandlingWorksWithAutoboxing() {
        // GIVEN
        SampleBean bean = new SampleBean();
        String methodName = "add";
        // autoboxed to Integer
        Object[] args = new Object[] { 5, 7 };
        // WHEN
        Object result = ReflectionUtils.call(bean, methodName, args);
        // THEN
        assertEquals(12, result);
    }

    @Test
    void staticMethodInvocationViaInstanceReturnsExpectedValue() {
        // GIVEN
        // instance is irrelevant for static method
        SampleBean bean = new SampleBean();
        String methodName = "staticEcho";
        Object[] args = new Object[] { "echo" };
        // WHEN
        Object result = ReflectionUtils.call(bean, methodName, args);
        // THEN
        assertEquals("echo", result);
    }

    @Test
    void nullMethodNameCausesNullPointerException() {
        // GIVEN
        SampleBean bean = new SampleBean();
        String methodName = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ReflectionUtils.call(bean, methodName));
    }
}
