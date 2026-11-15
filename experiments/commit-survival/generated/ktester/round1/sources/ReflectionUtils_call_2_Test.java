package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.*;
import java.util.stream.*;

class ReflectionUtils_call_2_Test {

    /**
     * Simple public method – happy‑path scenario.
     */
    public static class GreetingBean {

        public String greet(String name) {
            return "Hello " + name;
        }
    }

    /**
     * Method that throws an exception – should trigger
     *        InvocationTargetException which is wrapped by ReflectionUtils.
     */
    public static class ExplodingBean {

        public void explode() {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * Bean used for the majority of test scenarios.
     */
    public static class SimpleBean {

        // ----- normal public methods -------------------------------------------------
        public String greet(String name) {
            return "Hello, " + name + "!";
        }

        public int add(int a, int b) {
            return a + b;
        }

        // ----- overloaded methods ----------------------------------------------------
        public String overloaded(String s) {
            return "Overloaded with String";
        }

        public int overloaded(int i) {
            return i * 2;
        }

        // ----- var‑args ----------------------------------------------------------------
        public int varArgsMethod(int... numbers) {
            int sum = 0;
            for (int n : numbers) sum += n;
            return sum;
        }

        // ----- nullable argument -------------------------------------------------------
        public String nullArgMethod(Object o) {
            return (o == null) ? "null received" : o.toString();
        }

        // ----- primitive parameter ----------------------------------------------------
        public String primitiveParamMethod(int i) {
            return "int:" + i;
        }

        // ----- exception‑throwing methods ---------------------------------------------
        public void throwsChecked() throws Exception {
            throw new Exception("checked exception");
        }

        public void throwsRuntime() {
            throw new RuntimeException("runtime exception");
        }

        public void throwsError() {
            throw new AssertionError("assertion error");
        }

        // ----- protected method (different package simulation) -----------------------
        protected String protectedMethod() {
            return "protected";
        }

        // ----- private method ---------------------------------------------------------
        @SuppressWarnings("unused")
        private String privateMethod() {
            return "private";
        }
    }

    /**
     * Bean with a private method – used by the original tests.
     */
    public static class PrivateBean {

        @SuppressWarnings("unused")
        private String secret() {
            return "secret";
        }
    }

    /**
     * Successful public‑method invocations.
     */
    static Stream<Arguments> successfulInvocations() {
        return Stream.of(// greet
        Arguments.of(new SimpleBean(), "greet", new Object[] { "World" }, "Hello, World!"), // add
        Arguments.of(new SimpleBean(), "add", new Object[] { 5, 7 }, 12), // overloaded with String
        Arguments.of(new SimpleBean(), "overloaded", new Object[] { "string" }, "Overloaded with String"), // overloaded with int
        Arguments.of(new SimpleBean(), "overloaded", new Object[] { 10 }, 20), // var‑args
        Arguments.of(new SimpleBean(), "varArgsMethod", new Object[] { 1, 2, 3 }, 6), // null argument method
        Arguments.of(new SimpleBean(), "nullArgMethod", new Object[] { null }, "null received"));
    }

    /**
     * Edge‑case and error‑handling scenarios.
     */
    static Stream<Arguments> errorScenarios() {
        return Stream.of(// null bean
        Arguments.of(null, "anyMethod", new Object[] {}, RuntimeException.class, "getMethod"), // null method name
        Arguments.of(new SimpleBean(), null, new Object[] {}, RuntimeException.class, "methodName"), // non‑existent method
        Arguments.of(new SimpleBean(), "nonExistent", new Object[] {}, RuntimeException.class, NoSuchMethodException.class.getSimpleName()), // private method – IllegalAccessException
        Arguments.of(new SimpleBean(), "privateMethod", new Object[] {}, RuntimeException.class, IllegalAccessException.class.getSimpleName()), // checked exception wrapped
        Arguments.of(new SimpleBean(), "throwsChecked", new Object[] {}, RuntimeException.class, Exception.class.getSimpleName()), // protected method – IllegalAccessException (different package simulation)
        Arguments.of(new SimpleBean(), "protectedMethod", new Object[] {}, RuntimeException.class, IllegalAccessException.class.getSimpleName()), // runtime exception from target method
        Arguments.of(new SimpleBean(), "throwsRuntime", new Object[] {}, RuntimeException.class, RuntimeException.class.getSimpleName()), // error from target method
        Arguments.of(new SimpleBean(), "throwsError", new Object[] {}, RuntimeException.class, AssertionError.class.getSimpleName()));
    }

    /**
     * Null var‑args array – should cause a NPE before any reflection occurs.
     */
    static Stream<Arguments> nullArgsArray() {
        return Stream.of(Arguments.of(new SimpleBean(), "greet", null));
    }

    /**
     * Supplying null for a primitive parameter – IllegalArgumentException from Method.invoke.
     */
    static Stream<Arguments> nullForPrimitiveParam() {
        return Stream.of(Arguments.of(new SimpleBean(), "primitiveParamMethod", new Object[] { null }));
    }

    /**
     * Argument type mismatch – IllegalArgumentException from Method.invoke.
     */
    static Stream<Arguments> argumentTypeMismatch() {
        return Stream.of(Arguments.of(new SimpleBean(), "add", new Object[] { "notAnInt", 2 }));
    }

    @Test
    void call_InvokesPublicMethodSuccessfully() {
        // GIVEN
        GreetingBean bean = new GreetingBean();
        // WHEN
        Object result = ReflectionUtils.call(bean, "greet", "World");
        // THEN
        assertEquals("Hello World", result);
    }

    @Test
    void call_IllegalAccessExceptionIsWrappedInRuntimeException() {
        // GIVEN
        PrivateBean bean = new PrivateBean();
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(bean, "secret"));
        assertTrue(ex.getCause() instanceof IllegalAccessException, "Cause should be IllegalAccessException");
        assertTrue(ex.getMessage().contains("IllegalAccessException occurred"), "Exception message should mention IllegalAccessException");
    }

    @Test
    void call_InvocationTargetExceptionIsWrappedInRuntimeException() {
        // GIVEN
        ExplodingBean bean = new ExplodingBean();
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(bean, "explode"));
        assertTrue(ex.getCause() instanceof IllegalStateException, "Cause should be the exception thrown by the target method");
        assertEquals("boom", ex.getCause().getMessage(), "Underlying exception message should be preserved");
        assertTrue(ex.getMessage().contains("Exception thrown inside reflectively called method"), "Exception message should mention InvocationTargetException handling");
    }

    @ParameterizedTest(name = "[{index}] {2} -> {3}")
    @MethodSource("successfulInvocations")
    void call_SuccessfulInvocations(Object bean, String methodName, Object[] args, Object expected) {
        // WHEN
        Object result = ReflectionUtils.call(bean, methodName, args);
        // THEN
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {1} on bean={0}")
    @MethodSource("errorScenarios")
    void call_ErrorScenarios(Object bean, String methodName, Object[] args, Class<? extends Throwable> expectedException, String expectedMessageFragment) {
        // WHEN / THEN
        Throwable thrown = assertThrows(expectedException, () -> ReflectionUtils.call(bean, methodName, args));
        // The wrapper is always a RuntimeException; we verify the cause/message.
        assertTrue(thrown instanceof RuntimeException, "Wrapper should be RuntimeException");
        // The cause may be the original reflection exception or the target exception.
        Throwable cause = thrown.getCause();
        assertNotNull(cause, "Cause should not be null");
        // Verify that the cause type or message contains the expected fragment.
        if (expectedMessageFragment != null) {
            // For cases where we expect a specific exception type (e.g., IllegalAccessException)
            // we also accept matching by class name.
            boolean matches = cause.getClass().getSimpleName().contains(expectedMessageFragment) || (cause.getMessage() != null && cause.getMessage().contains(expectedMessageFragment));
            assertTrue(matches, () -> "Cause should contain '" + expectedMessageFragment + "'. Actual: " + cause);
        }
    }

    @Test
    void call_NullArgsArray_ThrowsNullPointerException() {
        SimpleBean bean = new SimpleBean();
        // Passing a null var‑args array leads to a NPE when args.length is accessed.
        assertThrows(NullPointerException.class, () -> ReflectionUtils.call(bean, "greet", (Object[]) null));
    }

    @Test
    void call_NullForPrimitiveParameter_IllegalArgumentExceptionWrapped() {
        SimpleBean bean = new SimpleBean();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(bean, "primitiveParamMethod", (Object) null));
        assertTrue(ex.getCause() instanceof IllegalArgumentException, "Cause should be IllegalArgumentException for null primitive argument");
    }

    @Test
    void call_ArgumentTypeMismatch_IllegalArgumentExceptionWrapped() {
        SimpleBean bean = new SimpleBean();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(bean, "add", "notAnInt", 2));
        assertTrue(ex.getCause() instanceof IllegalArgumentException, "Cause should be IllegalArgumentException for type mismatch");
    }
}
