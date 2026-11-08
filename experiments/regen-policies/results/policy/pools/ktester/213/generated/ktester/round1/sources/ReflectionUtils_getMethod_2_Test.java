package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getMethod_2_Test {

    /* -----------------------------------------------------------
     *  Helper classes used only by this test suite
     * ----------------------------------------------------------- */
    static class Sample {

        public String greet() {
            return "hello";
        }

        public void combine(int a, String b) {
            /* no‑op */
        }
    }

    /**
     * Simple class with a variety of methods used for the “successful retrieval” tests.
     */
    static class SampleClass {

        public void noArgs() {
            /* no‑op */
        }

        public void oneArg(Object a) {
            /* no‑op */
        }

        private void privateMethod(Object a, Object b) {
            /* no‑op */
        }

        public void overloaded(Object a) {
            /* no‑op */
        }

        public void overloaded(Object a, Object b) {
            /* no‑op */
        }
    }

    /**
     * Base class containing a method that will be inherited by {@link SubClass}.
     */
    static class BaseClass {

        public void baseOnly(Object a, Object b) {
            /* no‑op */
        }

        public void foo(Object a) {
            /* base implementation */
        }
    }

    /**
     * Subclass that overrides {@code foo} and inherits {@code baseOnly}.
     */
    static class SubClass extends BaseClass {

        @Override
        public void foo(Object a) {
            /* overridden implementation */
        }
    }

    private static class ValidationCase {

        final Object instance;

        final String methodName;

        final int argCount;

        final String expectedMessageFragment;

        ValidationCase(Object instance, String methodName, int argCount, String expectedMessageFragment) {
            this.instance = instance;
            this.methodName = methodName;
            this.argCount = argCount;
            this.expectedMessageFragment = expectedMessageFragment;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a fresh method cache for each test to avoid cross‑test interference
        ReflectionUtils.setMethodCache(new ConcurrentHashMap<>());
    }

    private static Stream<ValidationCase> validationCases() {
        return Stream.of(new ValidationCase(null, "anyMethod", 0, "Object instance cannot be null"), new ValidationCase(new SampleClass(), null, 0, "Method name cannot be null"), new ValidationCase(new SampleClass(), "noArgs", -1, "Argument count cannot be negative"));
    }

    @Test
    void shouldFindMethodWithExactArgumentCount() {
        Sample sample = new Sample();
        Method method = ReflectionUtils.getMethod(sample, "combine", 2);
        assertNotNull(method, "Method should be found");
        assertEquals("combine", method.getName());
        assertEquals(2, method.getParameterCount());
    }

    @Test
    void shouldReturnMethodFromCacheOnSecondInvocation() {
        Sample sample = new Sample();
        Method firstCall = ReflectionUtils.getMethod(sample, "greet", 0);
        Method secondCall = ReflectionUtils.getMethod(sample, "greet", 0);
        // The cached instance should be the same object reference
        assertSame(firstCall, secondCall, "Subsequent calls should return the cached Method instance");
    }

    @Test
    void shouldThrowWhenMethodDoesNotExist() {
        Sample sample = new Sample();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(sample, "nonExistingMethod", 1));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void shouldThrowWhenInstanceIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(null, "anyMethod", 0));
        assertTrue(ex.getMessage().contains("Object instance cannot be null"));
    }

    @Test
    void shouldThrowWhenMethodNameIsNull() {
        Sample sample = new Sample();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(sample, null, 0));
        assertTrue(ex.getMessage().contains("Method name cannot be null"));
    }

    @Test
    void shouldThrowWhenArgumentCountIsNegative() {
        Sample sample = new Sample();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(sample, "greet", -1));
        assertTrue(ex.getMessage().contains("Argument count cannot be negative"));
    }

    @Test
    void cacheShouldContainSelectedMethodAfterLookup() {
        Sample sample = new Sample();
        // Perform lookup – this will populate the internal cache
        Method method = ReflectionUtils.getMethod(sample, "greet", 0);
        assertNotNull(method);
        // Verify that the cache indeed holds the entry (using reflection only on the public API)
        // The ReflectionUtils class provides a setter for the cache, but no getter.
        // Therefore we replace the cache with a spy map to inspect its contents.
        Map<Object, Method> spyCache = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(spyCache);
        // The next call should store the method into our spyCache
        Method secondLookup = ReflectionUtils.getMethod(sample, "greet", 0);
        assertSame(method, secondLookup);
        assertTrue(spyCache.containsValue(method), "Method should be cached after lookup");
    }

    @ParameterizedTest
    @MethodSource("validationCases")
    void shouldValidateArguments(ValidationCase vc) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(vc.instance, vc.methodName, vc.argCount));
        assertTrue(ex.getMessage().contains(vc.expectedMessageFragment), () -> "Expected message to contain [" + vc.expectedMessageFragment + "] but was [" + ex.getMessage() + "]");
    }

    @Test
    void shouldRetrievePublicNoArgMethod() {
        SampleClass obj = new SampleClass();
        Method m = ReflectionUtils.getMethod(obj, "noArgs", 0);
        assertNotNull(m);
        assertEquals("noArgs", m.getName());
        assertEquals(0, m.getParameterCount());
        assertTrue(m.canAccess(obj), "Method should be accessible");
    }

    @Test
    void shouldRetrievePublicOneArgMethod() {
        SampleClass obj = new SampleClass();
        Method m = ReflectionUtils.getMethod(obj, "oneArg", 1);
        assertNotNull(m);
        assertEquals("oneArg", m.getName());
        assertEquals(1, m.getParameterCount());
        assertTrue(m.canAccess(obj));
    }

    @Test
    void shouldRetrieveOverriddenMethodFromSubclass() {
        SubClass obj = new SubClass();
        Method m = ReflectionUtils.getMethod(obj, "foo", 1);
        assertNotNull(m);
        assertEquals("foo", m.getName());
        assertEquals(1, m.getParameterCount());
        // The declaring class must be SubClass, not BaseClass
        assertEquals(SubClass.class, m.getDeclaringClass(), "The returned method should be the overriding version from SubClass");
    }

    @Test
    void shouldRetrieveInheritedMethodFromSuperclass() {
        SubClass obj = new SubClass();
        Method m = ReflectionUtils.getMethod(obj, "baseOnly", 2);
        assertNotNull(m);
        assertEquals("baseOnly", m.getName());
        assertEquals(2, m.getParameterCount());
        // Declaring class should be BaseClass
        assertEquals(BaseClass.class, m.getDeclaringClass(), "Method should be declared in the superclass");
    }

    @Test
    void shouldMakePrivateMethodAccessible() {
        SampleClass obj = new SampleClass();
        Method m = ReflectionUtils.getMethod(obj, "privateMethod", 2);
        assertNotNull(m);
        assertEquals("privateMethod", m.getName());
        assertEquals(2, m.getParameterCount());
        // The method is private; after lookup ReflectionUtils should have called
        // ClassUtilities.trySetAccessible, making it accessible for reflective calls.
        assertTrue(m.canAccess(obj), "Private method should have been made accessible by ReflectionUtils");
    }

    @Test
    void shouldSelectCorrectOverloadedMethodBasedOnArgumentCount() {
        SampleClass obj = new SampleClass();
        // There are two overloaded() methods – one with 1 param and one with 2.
        Method m1 = ReflectionUtils.getMethod(obj, "overloaded", 1);
        assertNotNull(m1);
        assertEquals(1, m1.getParameterCount());
        Method m2 = ReflectionUtils.getMethod(obj, "overloaded", 2);
        assertNotNull(m2);
        assertEquals(2, m2.getParameterCount());
        // Ensure they are different Method objects
        assertNotEquals(m1, m2);
    }

    @Test
    void shouldThrowWhenMethodNameNotPresent() {
        SampleClass obj = new SampleClass();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(obj, "nonExistingMethod", 0));
        assertTrue(ex.getMessage().contains("nonExistingMethod"), "Exception message should contain the missing method name");
    }

    @Test
    void shouldThrowWhenArgumentCountDoesNotMatchExistingMethod() {
        SampleClass obj = new SampleClass();
        // Method 'noArgs' exists but we ask for 1 argument
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getMethod(obj, "noArgs", 1));
        assertTrue(ex.getMessage().contains("noArgs"), "Exception message should mention the method name");
        assertTrue(ex.getMessage().contains("1 parameters"), "Exception message should mention the requested argument count");
    }
}
