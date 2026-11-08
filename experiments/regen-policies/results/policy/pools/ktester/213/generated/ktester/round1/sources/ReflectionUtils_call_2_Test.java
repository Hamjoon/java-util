package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_call_2_Test {

    /**
     * Simple class used for reflection calls in the tests.
     */
    static class TestClass {

        @Override
        public String toString() {
            return "TestClassInstance";
        }

        @Override
        public boolean equals(Object obj) {
            return obj == this;
        }

        public int staticValue() {
            return 42;
        }

        public void explode() {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * Simple class with a variety of methods used for reflection calls.
     */
    static class Sample {

        // ---------- normal public methods ----------
        public String greet(String name) {
            return "Hello " + name;
        }

        public String ping() {
            return "pong";
        }

        public String concat(String a, String b) {
            return a + b;
        }

        public String concat(String a, String b, String c) {
            return a + "-" + b + "-" + c;
        }

        public int getNumber() {
            return 42;
        }

        public void explode() {
            throw new IllegalStateException("boom");
        }

        public int add(int x, int y) {
            return x + y;
        }

        public int singleInt(int i) {
            return i;
        }

        // ---------- private method to trigger IllegalAccessException ----------
        private String secret() {
            return "top‑secret";
        }

        @Override
        public String toString() {
            return "SampleInstance";
        }
    }

    @Test
    void callMethodByName_NoArgs_ReturnsResult() throws NoSuchMethodException {
        TestClass instance = new TestClass();
        // invoke toString() via the utility method
        Object result = ReflectionUtils.call(instance, "toString");
        assertNotNull(result);
        assertTrue(result instanceof String);
        assertEquals(instance.toString(), result);
    }

    @Test
    void callMethodByName_WithArgs_ReturnsResult() throws NoSuchMethodException {
        TestClass instance = new TestClass();
        // invoke equals(Object) via the utility method
        Object result = ReflectionUtils.call(instance, "equals", instance);
        assertNotNull(result);
        assertTrue(result instanceof Boolean);
        assertTrue((Boolean) result);
    }

    @Test
    void callMethodThatThrows_ExceptionIsWrappedInRuntimeException() {
        TestClass instance = new TestClass();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, "explode"));
        // The cause should be the original IllegalStateException
        assertNotNull(thrown.getCause());
        assertTrue(thrown.getCause() instanceof IllegalStateException);
        assertEquals("boom", thrown.getCause().getMessage());
    }

    @Test
    void callStaticMethodViaInstance_ReturnsResult() throws NoSuchMethodException {
        TestClass instance = new TestClass();
        // staticValue() is an instance method (but we also test that a static‑like method works)
        // For a truly static method we could declare it static, but invoking via an instance is allowed.
        Object result = ReflectionUtils.call(instance, "staticValue");
        assertNotNull(result);
        assertTrue(result instanceof Integer);
        assertEquals(42, result);
    }

    @Test
    void callWithMethodObject_OverloadWorks() throws NoSuchMethodException {
        TestClass instance = new TestClass();
        Method method = TestClass.class.getMethod("toString");
        Object result = ReflectionUtils.call(instance, method);
        assertEquals(instance.toString(), result);
    }

    @Test
    void successfulPublicMethodInvocation() {
        Sample instance = new Sample();
        // greet(String) → "Hello World"
        Object result = ReflectionUtils.call(instance, "greet", "World");
        assertEquals("Hello World", result);
    }

    @Test
    void illegalAccessExceptionBranch() {
        Sample instance = new Sample();
        // The method "secret" is private; ReflectionUtils should attempt to invoke it,
        // receive IllegalAccessException and wrap it in a RuntimeException.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, "secret"));
        assertTrue(ex.getMessage().contains("IllegalAccessException occurred attempting to reflectively call method: secret()"));
        assertTrue(ex.getCause() instanceof IllegalAccessException);
    }

    @Test
    void invocationTargetExceptionBranch() {
        Sample instance = new Sample();
        // explode() throws IllegalStateException → should be wrapped.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, "explode"));
        assertTrue(ex.getMessage().contains("Exception thrown inside reflectively called method: explode()"));
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertEquals("boom", ex.getCause().getMessage());
    }

    @Test
    void successfulZeroArgumentPublicMethodInvocation() {
        Sample instance = new Sample();
        // ping() → "pong"
        Object result = ReflectionUtils.call(instance, "ping");
        assertEquals("pong", result);
    }

    @ParameterizedTest(name = "concat with {0} arguments returns \"{2}\"")
    @CsvSource({ // two‑argument overload
    "'Hello','World','HelloWorld'", // three‑argument overload
    "'A','B','C','A-B-C'" })
    void overloadedMethodSelectionBasedOnArgumentCount(String a, String b, String expected) {
        Sample instance = new Sample();
        // Determine whether we are testing the 2‑arg or 3‑arg overload
        Object result;
        if (expected.contains("-")) {
            // three‑argument case
            result = ReflectionUtils.call(instance, "concat", a, b, "C");
        } else {
            // two‑argument case
            result = ReflectionUtils.call(instance, "concat", a, b);
        }
        assertEquals(expected, result);
    }

    @Test
    void primitiveReturnTypeHandling() {
        Sample instance = new Sample();
        // getNumber() returns primitive int 42 → boxed Integer
        Object result = ReflectionUtils.call(instance, "getNumber");
        assertTrue(result instanceof Integer);
        assertEquals(42, result);
    }

    @Test
    void nullArgsArrayLeadsToNullPointerException() {
        Sample instance = new Sample();
        // Passing null for the var‑args parameter triggers NPE when args.length is accessed.
        assertThrows(NullPointerException.class, () -> ReflectionUtils.call(instance, "greet", (Object[]) null));
    }

    @Test
    void methodNotFoundWrongName() {
        Sample instance = new Sample();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, "nonExistentMethod"));
        // According to the API contract, a missing method results in IllegalArgumentException,
        // which is wrapped as RuntimeException by the caller.
        assertTrue(ex.getMessage().contains("No such method"));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void nullInstanceObject() {
        // The utility should throw NPE when trying to resolve the class of a null instance.
        assertThrows(NullPointerException.class, () -> ReflectionUtils.call(null, "toString"));
    }

    @Test
    void illegalArgumentExceptionBranch_WrongParameterTypes() {
        Sample instance = new Sample();
        // add(int,int) expects primitives; providing Strings causes IllegalArgumentException.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(instance, "add", "one", "two"));
        // No additional wrapping – the exception propagates directly.
        assertNotNull(ex.getMessage());
    }

    @Test
    void illegalArgumentExceptionBranch_NullForPrimitive() {
        Sample instance = new Sample();
        // singleInt(int) cannot accept null → IllegalArgumentException from Method.invoke.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(instance, "singleInt", (Object) null));
        assertNotNull(ex.getMessage());
    }
}
