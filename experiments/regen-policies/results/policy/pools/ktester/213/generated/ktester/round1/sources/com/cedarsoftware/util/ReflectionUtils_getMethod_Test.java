package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.*;
import java.util.stream.Stream;

// import org.junit.*;
// import static org.junit.Assert.*;
// @ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethod_Test {

    /**
     * Simple class with a custom {@code equals(Object)} implementation.
     */
    static class Simple {

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    /**
     * Base class defining {@code hashCode()} (inherited from {@link Object}).
     */
    static class Base {
        /* inherits Object.hashCode() */
    }

    /**
     * Subclass of {@link Base} that does NOT override {@code hashCode()}.
     */
    static class Sub extends Base {
        /* inherits hashCode from Object via Base */
    }

    /**
     * Base class for inheritance tests.
     */
    static class SuperClass {

        protected void protectedSuperMethod() {
        }
    }

    /**
     * Subclass that does not override the protected method.
     */
    static class SubClass extends SuperClass {
    }

    /**
     * Class containing a public no‑arg method.
     */
    static class PublicMethodClass {

        public void publicMethod() {
        }
    }

    /**
     * Class containing a private method with a single {@code String} argument.
     */
    static class PrivateMethodClass {

        @SuppressWarnings("unused")
        private void privateMethod(String s) {
        }
    }

    /**
     * Class with overloaded methods.
     */
    static class OverloadedMethodClass {

        public void overloaded() {
        }

        public void overloaded(String s) {
        }

        public void overloaded(Integer i) {
        }
    }

    /**
     * Class with a primitive‑type parameter method.
     */
    static class PrimitiveParamClass {

        public void primitiveMethod(int i) {
        }
    }

    /**
     * Class with an array‑type parameter method.
     */
    static class ArrayParamClass {

        public void arrayMethod(String[] arr) {
        }
    }

    /**
     * Class with a static method.
     */
    static class StaticMethodClass {

        public static void staticMethod() {
        }
    }

    /**
     * Private superclass containing a private method.
     */
    private static class PrivateSuperClass {

        private void privateSuperMethod() {
        }
    }

    /**
     * Subclass of the private superclass.
     */
    static class SubClassOfPrivateSuper extends PrivateSuperClass {
    }

    /**
     * Sample class used for a few generic tests.
     */
    static class SampleClass {

        public void publicMethod() {
        }
    }

    public static class DirectMethodTest {

        private final Class<?> target;

        private final String name;

        private final Class<?>[] types;

        private final Method expected;

        public DirectMethodTest(Class<?> target, String name, Class<?>[] types, Method expected) {
            this.target = target;
            this.name = name;
            this.types = types;
            this.expected = expected;
        }
    }

    public static class InheritedMethodTest {

        private final Class<?> target;

        private final String name;

        private final Class<?>[] types;

        private final Method expected;

        public InheritedMethodTest(Class<?> target, String name, Class<?>[] types, Method expected) {
            this.target = target;
            this.name = name;
            this.types = types;
            this.expected = expected;
        }
    }

    public static class OverloadSelectionTest {

        private final Class<?> target;

        private final String name;

        private final Class<?>[] types;

        private final Method expected;

        public OverloadSelectionTest(Class<?> target, String name, Class<?>[] types, Method expected) {
            this.target = target;
            this.name = name;
            this.types = types;
            this.expected = expected;
        }
    }

    public static class CacheEquivalenceTest {

        private final Class<?> target;

        private final String name;

        private final Class<?>[] types;

        public CacheEquivalenceTest(Class<?> target, String name, Class<?>[] types) {
            this.target = target;
            this.name = name;
            this.types = types;
        }
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Reset the method cache after each test that manipulates it.
        ReflectionUtils.setMethodCache(new LinkedHashMap<>());
    }

    // -----------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private static Map<Object, Method> getCurrentMethodCache() throws Exception {
        java.lang.reflect.Field field = ReflectionUtils.class.getDeclaredField("METHOD_CACHE");
        field.setAccessible(true);
        AtomicReference<Map<? super Object, Method>> ref = (AtomicReference<Map<? super Object, Method>>) field.get(null);
        return (Map<Object, Method>) ref.get();
    }
}
