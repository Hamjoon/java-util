package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.InvocationTargetException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_call_2_Test {

    class PrivateMethodHolder {

        @SuppressWarnings("unused")
        private String hidden() {
            return "secret";
        }
    }

    /**
     * Simple class used in the original test suite (unchanged).
     */
    static class Simple {

        @Override
        public String toString() {
            return "simple";
        }

        public boolean equals(Object o) {
            return this == o;
        }

        public String concat(String a, String b) {
            return a + b;
        }

        public void explode() {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * Sample class used for most of the new test cases.
     */
    static class Sample {

        public String greet(String name) {
            return "Hello " + name;
        }

        public static String staticGreet(String name) {
            return "Static Hello " + name;
        }

        public void instanceMethod() {
            // no‑op
        }

        public int add(int a, int b) {
            return a + b;
        }
    }

    /**
     * Class containing a private method – used to trigger IllegalAccessException.
     */
    static class PrivateMethod {

        private String secret() {
            return "secret";
        }
    }

    /**
     * Class whose method throws an exception – used to trigger InvocationTargetException.
     */
    static class ThrowsException {

        public void explode() {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * Class with a void‑returning method.
     */
    static class VoidMethod {

        public void doSomething() {
            // intentionally empty
        }
    }

    /**
     * Class with primitive return types and primitive parameters.
     */
    static class PrimitiveReturn {

        public int add(int a, int b) {
            return a + b;
        }

        public int multiply(int a, int b) {
            return a * b;
        }
    }

    /**
     * Class that simply returns the supplied argument (may be null).
     */
    static class NullArg {

        public Object echo(Object o) {
            return o;
        }
    }

    /**
     * Class that overloads the method name “greet”.
     */
    static class Overload {

        public String greet() {
            return "no args";
        }

        public String greet(String name) {
            return "Hello " + name;
        }
    }

    /**
     * Verify that a method can be invoked by name with no arguments.
     */
    @Test
    void callToStringByName() {
        Simple instance = new Simple();
        Object result = ReflectionUtils.call(instance, "toString");
        assertEquals("simple", result);
    }

    /**
     * Verify that a method can be invoked by name with arguments.
     */
    @Test
    void callConcatWithArgs() {
        Simple instance = new Simple();
        Object result = ReflectionUtils.call(instance, "concat", "hello", "world");
        assertEquals("helloworld", result);
    }

    /**
     * Verify that a method that returns a boolean works correctly.
     */
    @Test
    void callEqualsWithArg() {
        Simple instance = new Simple();
        Object result = ReflectionUtils.call(instance, "equals", instance);
        assertTrue((Boolean) result);
    }

    /**
     * Verify that an exception thrown by the target method is wrapped in a RuntimeException
     *  and the original exception is available as the cause.
     */
    @Test
    void callMethodThatThrowsException() {
        Simple instance = new Simple();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, "explode"));
        // The cause should be the original IllegalStateException from explode()
        assertTrue(thrown.getCause() instanceof IllegalStateException);
        assertEquals("boom", thrown.getCause().getMessage());
    }

    /**
     * Verify that an IllegalAccessException is wrapped correctly.
     *  This is achieved by attempting to call a private method via the overload that
     *  resolves the Method object first (which is accessible) and then invoking it
     *  through ReflectionUtils.call(Object, Method, Object...). The private method
     *  will cause IllegalAccessException when invoked.
     */
    @Test
    void callPrivateMethodViaMethodObject() throws NoSuchMethodException {
        class PrivateMethodHolder {

            @SuppressWarnings("unused")
            private String hidden() {
                return "secret";
            }
        }
        PrivateMethodHolder holder = new PrivateMethodHolder();
        // Obtain the private method via reflection; it is not accessible by default.
        java.lang.reflect.Method hiddenMethod = PrivateMethodHolder.class.getDeclaredMethod("hidden");
        // Ensure the method is not made accessible – we want the IllegalAccessException path.
        assertFalse(hiddenMethod.canAccess(holder));
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(holder, hiddenMethod));
        assertTrue(thrown.getMessage().contains("IllegalAccessException"));
        // The cause should be the original IllegalAccessException
        assertTrue(thrown.getCause() instanceof IllegalAccessException);
    }

    /**
     * Normal path – invoke a public method that returns a value.
     */
    @Test
    void callPublicMethodReturnsValue() {
        Sample instance = new Sample();
        Object result = ReflectionUtils.call(instance, "greet", "World");
        assertEquals("Hello World", result);
    }

    /**
     * Trigger IllegalAccessException by invoking a private method without making it accessible.
     */
    @Test
    void callPrivateMethodByNameTriggersIllegalAccessException() {
        PrivateMethod instance = new PrivateMethod();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, "secret"));
        assertTrue(thrown.getMessage().contains("IllegalAccessException"));
        assertTrue(thrown.getCause() instanceof IllegalAccessException);
    }

    /**
     * Cause an InvocationTargetException by invoking a method that throws an exception.
     */
    @Test
    void callMethodThatThrowsCheckedException() {
        ThrowsException instance = new ThrowsException();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, "explode"));
        assertTrue(thrown.getMessage().contains("Exception thrown inside reflectively called method: explode"));
        assertTrue(thrown.getCause() instanceof IllegalStateException);
        assertEquals("boom", thrown.getCause().getMessage());
    }

    /**
     * Invoke a static method with a null instance.
     */
    @Test
    void callStaticMethodWithNullInstance() {
        // The utility should resolve the method on the Sample class even when instance is null.
        Object result = ReflectionUtils.call(null, "staticGreet", "World");
        assertEquals("Static Hello World", result);
    }

    /**
     * Call a method that returns void – the reflective invoke returns null.
     */
    @Test
    void callVoidMethodReturnsNull() {
        VoidMethod instance = new VoidMethod();
        Object result = ReflectionUtils.call(instance, "doSomething");
        assertNull(result);
    }

    /**
     * Invoke a method that returns a primitive int – result should be autoboxed.
     */
    @Test
    void callPrimitiveReturnMethod() {
        PrimitiveReturn instance = new PrimitiveReturn();
        Object result = ReflectionUtils.call(instance, "add", 2, 3);
        assertEquals(5, result);
    }

    /**
     * Invoke a method with primitive parameters using boxed arguments – autounboxing should occur.
     */
    @Test
    void callPrimitiveMethodWithBoxedArgs() {
        PrimitiveReturn instance = new PrimitiveReturn();
        Object result = ReflectionUtils.call(instance, "multiply", Integer.valueOf(2), Integer.valueOf(3));
        assertEquals(6, result);
    }

    /**
     * Pass a null argument to a method that simply returns the received value.
     */
    @Test
    void callMethodWithNullArgument() {
        NullArg instance = new NullArg();
        Object result = ReflectionUtils.call(instance, "echo", (Object) null);
        assertNull(result);
    }

    /**
     * Verify overload resolution – no‑argument version.
     */
    @Test
    void callOverloadedMethodNoArgs() {
        Overload instance = new Overload();
        Object result = ReflectionUtils.call(instance, "greet");
        assertEquals("no args", result);
    }

    /**
     * Verify overload resolution – single‑argument version.
     */
    @Test
    void callOverloadedMethodWithArg() {
        Overload instance = new Overload();
        Object result = ReflectionUtils.call(instance, "greet", "Bob");
        assertEquals("Hello Bob", result);
    }

    /**
     * Attempt to invoke a method that does not exist – expect RuntimeException wrapping NoSuchMethodException.
     */
    @Test
    void callNonexistentMethod() {
        Sample instance = new Sample();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, "nonexistent"));
        assertTrue(thrown.getMessage().contains("NoSuchMethodException"));
    }

    /**
     * Provide an argument count that does not match any overload – expect RuntimeException wrapping NoSuchMethodException.
     */
    @Test
    void callMethodWithMismatchedArgumentCount() {
        Sample instance = new Sample();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, "greet"));
        assertTrue(thrown.getMessage().contains("NoSuchMethodException"));
    }

    /**
     * Invoke a non‑static instance method with a null target object – expect NullPointerException.
     */
    @Test
    void callInstanceMethodWithNullTarget() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.call(null, "instanceMethod"));
    }

    /**
     * Pass arguments of incompatible types – expect IllegalArgumentException from Method.invoke.
     */
    @Test
    void callMethodWithIncompatibleArgumentTypes() {
        Sample instance = new Sample();
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.call(instance, "add", "two", 3));
    }

    /**
     * Provide a null method name – expect NullPointerException (or IllegalArgumentException) propagated unchanged.
     */
    @Test
    void callWithNullMethodName() {
        Sample instance = new Sample();
        // The implementation throws IllegalArgumentException for a null method name; both are acceptable.
        assertThrows(RuntimeException.class, () -> ReflectionUtils.call(instance, null));
    }
}
