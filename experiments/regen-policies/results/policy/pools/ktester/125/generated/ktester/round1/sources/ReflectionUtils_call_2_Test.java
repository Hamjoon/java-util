package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_call_2_Test {

    /**
     * Simple class with a variety of methods for reflective invocation.
     */
    static class Sample {

        private final String value = "sample";

        // -------------------- regular public methods --------------------
        public String greet() {
            return "hello";
        }

        public String echo(String msg) {
            return value + ":" + msg;
        }

        public int add(int a, int b) {
            return a + b;
        }

        // static methods
        public static String staticConcat(String a, String b) {
            return a + b;
        }

        public static long staticAddLong(long a, long b) {
            return a + b;
        }

        // method that throws a checked exception
        public void throwsChecked() throws IOException {
            throw new IOException("boom");
        }

        // private method – used to trigger IllegalAccessException handling
        private int privateMultiply(int a, int b) {
            return a * b;
        }

        // overloaded methods (different parameter types, same count)
        public String overloaded(String s) {
            return "overloadedString";
        }

        public int overloaded(int i) {
            return i * 2;
        }

        // zero‑argument method
        public String noArgsMethod() {
            return "noArgs";
        }

        // var‑args method
        public int sumVarArgs(int... numbers) {
            return Arrays.stream(numbers).sum();
        }

        // primitive boolean return
        public boolean isPositive(int n) {
            return n > 0;
        }

        // protected method – will be accessed via a subclass instance
        protected String protectedGreet() {
            return "Hello from Sample";
        }

        // method with array parameter
        public String concatArray(String[] parts) {
            return String.join("", parts);
        }

        // method returning a generic collection
        public List<String> createList(String a, String b) {
            return new ArrayList<>(Arrays.asList(a, b));
        }
    }

    /**
     * Subclass used to test protected method inheritance.
     */
    static class SubSample extends Sample {
        // inherits protectedGreet()
    }

    /**
     * Simple interface with a single method.
     */
    interface InterfaceSample {

        String interfaceMethod();
    }

    /**
     * Implementation of InterfaceSample used for interface‑method tests.
     */
    static class ImplSample implements InterfaceSample {

        @Override
        public String interfaceMethod() {
            return "interface impl";
        }
    }

    // Example of a method source for future parameterized tests.
    static Stream<Arguments> dummyProvider() {
        return Stream.empty();
    }

    @Test
    void callMethodWithoutArguments() throws NoSuchMethodException {
        Sample instance = new Sample();
        // Use the overload that takes a Method instance.
        Method method = Sample.class.getMethod("greet");
        Object result = ReflectionUtils.call(instance, method);
        assertEquals("hello", result);
    }

    @Test
    void callMethodWithArgumentsUsingName() {
        Sample instance = new Sample();
        // Call the method by name with a single argument.
        Object result = ReflectionUtils.call(instance, "echo", "world");
        assertEquals("sample:world", result);
    }

    @Test
    void callMethodWithMultiplePrimitiveArguments() {
        Sample instance = new Sample();
        // Call the method that adds two ints.
        Object result = ReflectionUtils.call(instance, "add", 3, 7);
        assertTrue(result instanceof Integer);
        assertEquals(10, result);
    }

    @Test
    void callMethodHandlesInvocationTargetException() {
        Sample instance = new Sample();
        // Create a method that will deliberately throw an exception.
        // toString never throws, so we wrap it.
        Method method = Sample.class.getMethod("toString");
        // We simulate a failure by passing a null instance to a non‑static method.
        Exception thrown = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(null, method));
        assertTrue(thrown.getMessage().contains("IllegalAccessException"));
    }

    // ----- Successful invocation of public instance method (add) -----
    @Test
    void successfulPublicInstanceMethodInvocation() {
        Sample instance = new Sample();
        Object result = ReflectionUtils.call(instance, "add", 5, 7);
        assertEquals(12, result);
    }

    // ----- Successful invocation of static method with null instance (staticConcat) -----
    @Test
    void staticMethodInvocationWithNullInstance() {
        Object result = ReflectionUtils.call(null, "staticConcat", "Hello", "World");
        assertEquals("HelloWorld", result);
    }

    // ----- IllegalAccessException handling (private method) -----
    @Test
    void privateMethodTriggersIllegalAccessException() {
        Sample instance = new Sample();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, "privateMultiply", 3, 4));
        assertTrue(ex.getMessage().contains("IllegalAccessException occurred attempting to reflectively call method: privateMultiply"));
    }

    // ----- InvocationTargetException handling (method throws checked exception) -----
    @Test
    void methodThrowsCheckedExceptionIsWrapped() {
        Sample instance = new Sample();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, "throwsChecked"));
        assertTrue(ex.getMessage().contains("Exception thrown inside reflectively called method: throwsChecked"));
        assertTrue(ex.getCause() instanceof IOException);
    }

    // ----- Method not found (propagation) -----
    @Test
    void methodNotFoundThrowsIllegalArgumentException() {
        Sample instance = new Sample();
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(instance, "nonExistent"));
    }

    // ----- Invocation of overloaded method with matching argument count (String) -----
    @Test
    void overloadedMethodStringVariant() {
        Sample instance = new Sample();
        Object result = ReflectionUtils.call(instance, "overloaded", "text");
        assertEquals("overloadedString", result);
    }

    // ----- Invocation of overloaded method with matching argument count (int) -----
    @Test
    void overloadedMethodIntVariant() {
        Sample instance = new Sample();
        Object result = ReflectionUtils.call(instance, "overloaded", 42);
        assertEquals(84, result);
    }

    // ----- Invocation with null arguments array (treated as empty) -----
    @Test
    void nullVarArgsArrayTreatedAsEmpty() {
        Sample instance = new Sample();
        Object result = ReflectionUtils.call(instance, "noArgsMethod", (Object[]) null);
        assertEquals("noArgs", result);
    }

    // ----- Static method invocation with primitive widening (int → long) -----
    @Test
    void staticMethodWithPrimitiveWidening() {
        Object result = ReflectionUtils.call(null, "staticAddLong", 5, 10);
        assertTrue(result instanceof Long);
        assertEquals(15L, result);
    }

    // ----- Invocation of method with var‑args parameter -----
    @Test
    void varArgsMethodInvocation() {
        Sample instance = new Sample();
        Object result = ReflectionUtils.call(instance, "sumVarArgs", 1, 2, 3, 4);
        assertEquals(10, result);
    }

    // ----- Method with primitive boolean return type (true) -----
    @Test
    void booleanReturnTrue() {
        Sample instance = new Sample();
        Object result = ReflectionUtils.call(instance, "isPositive", 5);
        assertEquals(true, result);
    }

    // ----- Method with primitive boolean return type (false) -----
    @Test
    void booleanReturnFalse() {
        Sample instance = new Sample();
        Object result = ReflectionUtils.call(instance, "isPositive", -3);
        assertEquals(false, result);
    }

    // ----- Invocation on subclass inheriting protected method -----
    @Test
    void protectedMethodInheritedBySubclass() {
        SubSample instance = new SubSample();
        Object result = ReflectionUtils.call(instance, "protectedGreet");
        assertEquals("Hello from Sample", result);
    }

    // ----- Method with array parameter -----
    @Test
    void methodWithArrayParameter() {
        Sample instance = new Sample();
        String[] parts = new String[] { "a", "b", "c" };
        Object result = ReflectionUtils.call(instance, "concatArray", (Object) parts);
        assertEquals("abc", result);
    }

    // ----- Method with generic return type (List) -----
    @Test
    void methodReturningGenericList() {
        Sample instance = new Sample();
        Object result = ReflectionUtils.call(instance, "createList", "x", "y");
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(Arrays.asList("x", "y"), list);
    }

    // ----- Invocation where method is declared in an interface -----
    @Test
    void interfaceMethodInvocation() {
        ImplSample instance = new ImplSample();
        Object result = ReflectionUtils.call(instance, "interfaceMethod");
        assertEquals("interface impl", result);
    }

    // ----- Invocation with null instance on non‑static method -----
    @Test
    void nullInstanceOnNonStaticMethodThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(null, "add", 1, 2));
    }

    // ----- Null methodName handling -----
    @Test
    void nullMethodNameThrowsIllegalArgumentException() {
        Sample instance = new Sample();
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(instance, null, "any"));
    }

    // ----- Null args array handling (args is null) -----
    @Test
    void nullArgsArrayTriggersNullPointerException() {
        Sample instance = new Sample();
        // Directly invoke the overload that receives a var‑args array;
        // passing a null reference means the method will try to read args.length.
        assertThrows(NullPointerException.class, () -> ReflectionUtils.call(instance, "noArgsMethod", (Object[]) null));
    }

    // ----- Argument type mismatch IllegalArgumentException -----
    @Test
    void argumentTypeMismatchThrowsIllegalArgumentException() {
        Sample instance = new Sample();
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(instance, "add", "one", "two"));
    }

    // ----- Var‑args method with incompatible argument type -----
    @Test
    void varArgsIncompatibleArgumentTypeThrowsIllegalArgumentException() {
        Sample instance = new Sample();
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(instance, "sumVarArgs", "a", "b", "c"));
    }
}
