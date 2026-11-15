package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getNonOverloadedMethod_Test {

    /**
     * The cache used by {@link #getNonOverloadedMethod(Class, String)}.  An
     * {@link AtomicReference} is employed so that the cache can be swapped in tests
     * (see {@code clearMethodCache()} in the test class).
     */
    private static final AtomicReference<Map<MethodCacheKey, Method>> METHOD_CACHE =
            new AtomicReference<>(new HashMap<>());

    /**
     * Simple class with a single, non‑overloaded method.
     */
    static class Simple {

        @Override
        public String toString() {
            return "Simple";
        }
    }

    /**
     * Class that defines two overloaded methods named “overloaded”.
     */
    static class Overloaded {

        public void overloaded() {
        }

        public void overloaded(int i) {
        }
    }

    /**
     * Class without the requested method.
     */
    static class NoMethod {
    }

    /**
     * Simple class with a single, non‑overloaded instance method.
     */
    static class SimpleClass {

        public void foo() {
        }
    }

    /**
     * Class with a static public method.
     */
    static class StaticMethodClass {

        public static void staticMethod() {
        }
    }

    /**
     * Subclass that inherits {@code foo()} from {@link SimpleClass}.
     */
    static class InheritedClass extends SimpleClass {
    }

    /**
     * Simple interface used for implementation tests.
     */
    interface InterfaceA {

        void ifaceMethod();
    }

    /**
     * Class that implements {@link InterfaceA}.
     */
    static class ImplClass implements InterfaceA {

        @Override
        public void ifaceMethod() {
        }
    }

    /**
     * Class that defines two overloaded methods named {@code bar}.
     */
    static class OverloadedClass {

        public void bar() {
        }

        public void bar(int i) {
        }
    }

    /**
     * Class that defines a private method – it should be invisible to {@code getMethods()}.
     */
    static class PrivateMethodClass {

        @SuppressWarnings("unused")
        private void privateMethod() {
        }
    }

    /**
     * Class used for the “no method” scenario.
     */
    static class NoMethodClass {
    }

    /**
     * Sample class used in the JSON test description – identical to {@link SimpleClass}.
     */
    static class SampleClass {

        public void foo() {
        }
    }

    /**
     * Simple key used for the cache – it combines the target class and the method name.
     */
    private static final class MethodCacheKey {

        private final Class<?> clazz;
        private final String methodName;
        private final int hash;

        MethodCacheKey(Class<?> clazz, String methodName) {
            this.clazz = clazz;
            this.methodName = methodName;
            this.hash = clazz.hashCode() * 31 + methodName.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof MethodCacheKey))
                return false;
            MethodCacheKey that = (MethodCacheKey) o;
            return clazz.equals(that.clazz) && methodName.equals(that.methodName);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Ensure a clean cache before each test to avoid cross‑test interference.
        clearMethodCache();
    }

    /* ------------------------------------------------------------
     *  Parameterized validation tests (null / blank / empty)
     * ------------------------------------------------------------ */
    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(
                Arguments.of(null, "anyMethod", "null class"),
                Arguments.of(SampleClass.class, null, "null method name"),
                Arguments.of(SampleClass.class, "   ", "blank method name"),
                Arguments.of(SampleClass.class, "", "empty method name")
        );
    }

    /**
     * Retrieves a public, zero‑argument method with the supplied name from the given
     * class.  The method must not be overloaded – i.e. there must be exactly one
     * matching method.  The result is cached for fast subsequent look‑ups.
     *
     * @param clazz      the class to search
     * @param methodName the name of the method (must be non‑null / non‑blank)
     * @return the {@link Method} instance representing the requested method
     * @throws IllegalArgumentException if the class or method name is invalid,
     *                                  if the method cannot be found, or if the
     *                                  method is overloaded.
     */
    public static Method getNonOverloadedMethod(Class<?> clazz, String methodName) {
        // 1. Validate arguments.
        if (clazz == null) {
            throw new IllegalArgumentException("null class");
        }
        if (methodName == null || methodName.trim().isEmpty()) {
            throw new IllegalArgumentException("null or blank method name");
        }

        // 2. Check cache.
        MethodCacheKey key = new MethodCacheKey(clazz, methodName);
        Method cached = METHOD_CACHE.get().get(key);
        if (cached != null) {
            return cached;
        }

        // 3. Search public zero‑arg methods.
        Method found = null;
        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(methodName)) {
                continue;
            }
            if (m.getParameterCount() != 0) {
                continue;
            }
            if (found != null) {
                throw new IllegalArgumentException(
                        "Method '" + methodName + "' is overloaded methods on class " + clazz.getName());
            }
            found = m;
        }

        // 4. Handle outcomes.
        if (found == null) {
            throw new IllegalArgumentException(
                    "Method '" + methodName + "' is not found on class " + clazz.getName());
        }

        if (!Modifier.isPublic(found.getModifiers())) {
            found.setAccessible(true);
        }

        METHOD_CACHE.get().put(key, found);
        return found;
    }

    /**
     * Allows the test‑suite to replace the internal cache with a fresh, empty
     * {@link java.util.HashMap}.  This method is deliberately package‑private
     * (default visibility) – the test class lives in the same package.
     */
    static void setMethodCache(Map<MethodCacheKey, Method> newCache) {
        METHOD_CACHE.set(newCache);
    }

    /**
     * Convenience method used by the tests to clear the cache via reflection.
     * It simply delegates to {@link #setMethodCache(Map)}.
     */
    public static void clearCacheForTests() {
        setMethodCache(new HashMap<>());
    }

    @Test
    void shouldReturnMethodWhenExactlyOneMatchExists() {
        Method m = ReflectionUtils.getNonOverloadedMethod(Simple.class, "toString");
        assertNotNull(m, "Method should be found");
        assertEquals("toString", m.getName());
        // The method should be the one declared on Simple (or inherited from Object)
        assertTrue(m.getDeclaringClass() == Simple.class || m.getDeclaringClass() == Object.class);
    }

    @Test
    void shouldCacheResultAndReturnSameInstanceOnSubsequentCalls() {
        Method first = ReflectionUtils.getNonOverloadedMethod(Simple.class, "toString");
        Method second = ReflectionUtils.getNonOverloadedMethod(Simple.class, "toString");
        assertSame(first, second, "Subsequent calls should return the cached Method instance");
    }

    @Test
    void shouldThrowWhenMethodIsOverloaded() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getNonOverloadedMethod(Overloaded.class, "overloaded"));
        assertTrue(ex.getMessage().contains("overloaded methods"),
                "Exception message should mention overloaded methods");
    }

    @Test
    void shouldThrowWhenClassIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getNonOverloadedMethod(null, "any"));
        assertTrue(ex.getMessage().contains("null class"),
                "Exception message should indicate a null class argument");
    }

    @Test
    void shouldThrowWhenMethodNameIsNullOrBlank() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getNonOverloadedMethod(Simple.class, null));
        assertTrue(ex1.getMessage().contains("null or blank method name"));
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getNonOverloadedMethod(Simple.class, "   "));
        assertTrue(ex2.getMessage().contains("null or blank method name"));
    }

    @Test
    void shouldThrowWhenMethodDoesNotExist() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getNonOverloadedMethod(NoMethodClass.class, "missing"));
        assertTrue(ex.getMessage().contains("is not found on class"),
                "Message must indicate that the method could not be located");
    }

    /* ------------------------------------------------------------
     *  Helper – clear the static METHOD_CACHE via reflection
     * ------------------------------------------------------------ */
    @SuppressWarnings("unchecked")
    private static void clearMethodCache() throws Exception {
        java.lang.reflect.Field cacheField = ReflectionUtils.class.getDeclaredField("METHOD_CACHE");
        cacheField.setAccessible(true);
        AtomicReference<Map<?, Method>> ref = (AtomicReference<Map<?, Method>>) cacheField.get(null);
        // Replace with a fresh, empty HashMap.
        ref.set(new HashMap<>());
    }

    @ParameterizedTest(name = "[{index}] clazz={0}, methodName={1}")
    @MethodSource("invalidInputProvider")
    void shouldValidateNullOrBlankInputs(Class<?> clazz, String methodName, String expectedMessageFragment) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getNonOverloadedMethod(clazz, methodName));
        assertTrue(ex.getMessage().contains(expectedMessageFragment),
                () -> "Expected message to contain '" + expectedMessageFragment + "' but was: " + ex.getMessage());
    }

    @Test
    void shouldRetrievePublicInstanceMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(SimpleClass.class, "foo");
        assertNotNull(m);
        assertEquals("foo", m.getName());
        assertEquals(SimpleClass.class, m.getDeclaringClass());
    }

    @Test
    void shouldRetrievePublicStaticMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(StaticMethodClass.class, "staticMethod");
        assertNotNull(m);
        assertEquals("staticMethod", m.getName());
        assertTrue(Modifier.isStatic(m.getModifiers()));
        assertEquals(StaticMethodClass.class, m.getDeclaringClass());
    }

    @Test
    void shouldRetrieveInheritedMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(InheritedClass.class, "foo");
        assertNotNull(m);
        assertEquals("foo", m.getName());
        // The method is declared in SimpleClass but visible via inheritance.
        assertEquals(SimpleClass.class, m.getDeclaringClass());
    }

    @Test
    void shouldRetrieveInterfaceImplementedMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(ImplClass.class, "ifaceMethod");
        assertNotNull(m);
        assertEquals("ifaceMethod", m.getName());
        // Declaring class is the interface (as returned by Class.getMethods()).
        assertEquals(InterfaceA.class, m.getDeclaringClass());
    }

    @Test
    void shouldRetrieveObjectMethod() {
        Method m = ReflectionUtils.getNonOverloadedMethod(Object.class, "toString");
        assertNotNull(m);
        assertEquals("toString", m.getName());
        assertEquals(Object.class, m.getDeclaringClass());
    }

    @Test
    void shouldDetectUserDefinedOverloadedMethod() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getNonOverloadedMethod(OverloadedClass.class, "bar"));
        assertTrue(ex.getMessage().contains("overloaded methods"),
                "Exception should mention overloaded methods");
    }

    @Test
    void shouldDetectBuiltInOverloadedMethod_wait() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getNonOverloadedMethod(Object.class, "wait"));
        assertTrue(ex.getMessage().contains("overloaded methods"),
                "Object.wait has several overloads and should trigger the overload check");
    }

    @Test
    void shouldTreatPrivateMethodAsMissing() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getNonOverloadedMethod(PrivateMethodClass.class, "privateMethod"));
        assertTrue(ex.getMessage().contains("is not found on class"),
                "Private methods are not returned by Class.getMethods() and should be reported as missing");
    }

    @Test
    void shouldCacheSuccessfulLookup() {
        Method first = ReflectionUtils.getNonOverloadedMethod(SimpleClass.class, "foo");
        Method second = ReflectionUtils.getNonOverloadedMethod(SimpleClass.class, "foo");
        assertSame(first, second,
                "Subsequent successful look‑ups must return the identical Method instance");
    }

    @Test
    void shouldNotCacheFailedLookupDueToOverload() {
        // First call throws – cache must not store the failure.
        assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getNonOverloadedMethod(OverloadedClass.class, "bar"));
        // Second call should throw again (i.e., not return a cached result).
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.getNonOverloadedMethod(OverloadedClass.class, "bar"));
        assertTrue(ex.getMessage().contains("overloaded methods"));
    }

    /* ------------------------------------------------------------
     *  Optional SecurityManager test (disabled by default)
     * ------------------------------------------------------------ */
    @Disabled("SecurityManager is deprecated and may not be enabled in the test environment")
    @Test
    void shouldPropagateSecurityExceptionWhenReflectionIsDenied() throws Exception {
        // Install a SecurityManager that forbids reflective access to methods.
        SecurityManager original = System.getSecurityManager();
        try {
            System.setSecurityManager(new SecurityManager() {

                // Overriding without @Override to avoid compilation issues on newer JDKs.
                public void checkMemberAccess(Class<?> clazz, int which) {
                    throw new SecurityException("Reflective access denied");
                }

                public void checkPermission(java.security.Permission perm) {
                    // allow everything else
                }
            });
            SecurityException se = assertThrows(SecurityException.class,
                    () -> ReflectionUtils.getNonOverloadedMethod(SimpleClass.class, "foo"));
            assertTrue(se.getMessage().contains("Reflective access denied"));
        } finally {
            System.setSecurityManager(original);
        }
    }
}