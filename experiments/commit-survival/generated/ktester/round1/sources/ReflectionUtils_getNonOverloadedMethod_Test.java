package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.Permission;
import java.security.ReflectPermission;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

class ReflectionUtils_getNonOverloadedMethod_Test {

    /**
     * Helper classes used only by the tests **
     */
    static class Sample {

        // a unique, non‑overloaded method with no arguments
        public void uniqueMethod() {
        }
    }

    static class AnotherSample {

        // a different unique method to avoid map key collisions between tests
        public void anotherUniqueMethod() {
        }
    }

    static class OverloadedClass {

        public void process() {
        }

        public void process(String s) {
        }

        public void execute(String s) {
        }
    }

    static class PrivateMethodClass {

        private void hidden() {
        }
    }

    static class SubSample extends Sample {
        // inherits foo()
    }

    static class StaticMethodClass {

        public static void initialize() {
        }
    }

    /**
     * Utility to clear the internal cache before each test **
     */
    @BeforeEach
    void clearCache() throws Exception {
        // ReflectionUtils.METHOD_MAP3 is private static final; we access it via reflection.
        var field = ReflectionUtils.class.getDeclaredField("METHOD_MAP3");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, Method> map = (ConcurrentMap<String, Method>) field.get(null);
        map.clear();
    }

    /* --------------------------------------------------------------
     *  Helper methods for cache manipulation and private static method
     *  invocation via reflection.
     * -------------------------------------------------------------- */
    private static void putInCache(Class<?> clazz, String methodName, Method method) throws Exception {
        var field = ReflectionUtils.class.getDeclaredField("METHOD_MAP3");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, Method> map = (ConcurrentMap<String, Method>) field.get(null);
        String key = buildCacheKey(clazz, methodName);
        map.put(key, method);
    }

    private static String buildCacheKey(Class<?> clazz, String methodName) throws Exception {
        // Replicate the key building logic from ReflectionUtils.getNonOverloadedMethod
        var getClassLoaderName = ReflectionUtils.class.getDeclaredMethod("getClassLoaderName", Class.class);
        getClassLoaderName.setAccessible(true);
        String loaderName = (String) getClassLoaderName.invoke(null, clazz);
        return loaderName + '.' + clazz.getName() + '.' + methodName;
    }

    /**
     * Bridge to invoke the private static {@code getMethodNoArgs} method via reflection.
     * Used only inside Mockito stubbing to avoid compilation errors.
     */
    private static Method invokePrivateGetMethodNoArgs(Class<?> c, String methodName) {
        try {
            var method = ReflectionUtils.class.getDeclaredMethod("getMethodNoArgs", Class.class, String.class);
            method.setAccessible(true);
            return (Method) method.invoke(null, c, methodName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void getNonOverloadedMethod_NullClass_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(null, "anyMethod"));
        assertTrue(ex.getMessage().contains("Attempted to call getMethod() [anyMethod]"));
    }

    @Test
    void getNonOverloadedMethod_NullMethodName_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Sample.class, null));
        assertTrue(ex.getMessage().contains("Attempted to call getMethod() with a null method name"));
        assertTrue(ex.getMessage().contains(Sample.class.getName()));
    }

    @Test
    void getNonOverloadedMethod_MethodNotFound_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Sample.class, "nonExistentMethod"));
        assertTrue(ex.getMessage().contains("Method: nonExistentMethod() is not found on class"));
        assertTrue(ex.getMessage().contains(Sample.class.getName()));
    }

    @Test
    void getNonOverloadedMethod_ReturnsCorrectMethod() throws NoSuchMethodException {
        Method method = ReflectionUtils.getNonOverloadedMethod(Sample.class, "uniqueMethod");
        assertNotNull(method);
        // Verify that the returned Method object matches the one obtained via reflection directly
        Method expected = Sample.class.getMethod("uniqueMethod");
        assertEquals(expected, method);
    }

    @Test
    void getNonOverloadedMethod_CachesMethodInstance() {
        // First call – method will be looked up and cached
        Method firstCall = ReflectionUtils.getNonOverloadedMethod(AnotherSample.class, "anotherUniqueMethod");
        // Second call – should retrieve the same Method instance from the cache
        Method secondCall = ReflectionUtils.getNonOverloadedMethod(AnotherSample.class, "anotherUniqueMethod");
        assertSame(firstCall, secondCall, "Method instances should be identical due to caching");
    }

    /* --------------------------------------------------------------
     *  Cache specific scenarios using Mockito to spy on the private static
     *  getMethodNoArgs method.
     * -------------------------------------------------------------- */
    @Test
    void getNonOverloadedMethod_CacheMissAndInsert_FirstInsertion() throws Exception {
        // Ensure cache is empty (handled by @BeforeEach)
        Method method = ReflectionUtils.getNonOverloadedMethod(Sample.class, "foo");
        assertNotNull(method);
        assertEquals("foo", method.getName());
        // Verify that the method is now cached
        Method cached = ReflectionUtils.getNonOverloadedMethod(Sample.class, "foo");
        assertSame(method, cached);
    }

    @Test
    void getNonOverloadedMethod_CacheHit_MethodAlreadyCached() throws Exception {
        // Pre‑populate the cache with the expected Method
        Method preCached = Sample.class.getMethod("foo");
        putInCache(Sample.class, "foo", preCached);
        // Mock static getMethodNoArgs to fail if it is called (should not be)
        try (MockedStatic<ReflectionUtils> mocked = Mockito.mockStatic(ReflectionUtils.class, CALLS_REAL_METHODS)) {
            mocked.when(() -> invokePrivateGetMethodNoArgs(any(), anyString())).thenThrow(new AssertionError("getMethodNoArgs should not be invoked on cache hit"));
            Method result = ReflectionUtils.getNonOverloadedMethod(Sample.class, "foo");
            assertSame(preCached, result);
        }
    }

    @Test
    void getNonOverloadedMethod_CacheMiss_WithExistingEntry_PutIfAbsentReturnsOther() throws Exception {
        // Cache already contains the method (simulating a race where putIfAbsent returns non‑null)
        Method cached = Sample.class.getMethod("foo");
        putInCache(Sample.class, "foo", cached);
        // Even though getMethodNoArgs will return a new Method instance, the cache entry should win.
        try (MockedStatic<ReflectionUtils> mocked = Mockito.mockStatic(ReflectionUtils.class, CALLS_REAL_METHODS)) {
            // Let the real getMethodNoArgs execute (it will return the same Method object)
            mocked.when(() -> invokePrivateGetMethodNoArgs(Sample.class, "foo")).thenCallRealMethod();
            Method result = ReflectionUtils.getNonOverloadedMethod(Sample.class, "foo");
            assertSame(cached, result, "Returned method should be the one already cached");
        }
    }

    /* --------------------------------------------------------------
     *  Overloaded method handling
     * -------------------------------------------------------------- */
    @Test
    void getNonOverloadedMethod_OverloadedMethod_WithNoArgVariant() {
        Method method = ReflectionUtils.getNonOverloadedMethod(OverloadedClass.class, "process");
        assertNotNull(method);
        assertEquals(0, method.getParameterCount());
        assertEquals("process", method.getName());
    }

    @Test
    void getNonOverloadedMethod_OverloadedMethod_WithoutNoArgVariant() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(OverloadedClass.class, "execute"));
        assertTrue(ex.getMessage().contains("Method: execute() is not found on class"));
    }

    /* --------------------------------------------------------------
     *  Private method access – the returned Method should be accessible
     * -------------------------------------------------------------- */
    @Test
    void getNonOverloadedMethod_PrivateMethod_AccessibleFlagTrue() {
        Method method = ReflectionUtils.getNonOverloadedMethod(PrivateMethodClass.class, "hidden");
        assertNotNull(method);
        assertTrue(Modifier.isPrivate(method.getModifiers()));
        // In Java 8 the accessible flag can be queried via isAccessible()
        assertTrue(method.isAccessible(), "Private method should be made accessible");
    }

    /* --------------------------------------------------------------
     *  Inherited method retrieval
     * -------------------------------------------------------------- */
    @Test
    void getNonOverloadedMethod_InheritedMethod() throws NoSuchMethodException {
        Method method = ReflectionUtils.getNonOverloadedMethod(SubSample.class, "foo");
        assertNotNull(method);
        Method expected = Sample.class.getMethod("foo");
        assertEquals(expected, method);
    }

    /* --------------------------------------------------------------
     *  Static no‑arg method retrieval
     * -------------------------------------------------------------- */
    @Test
    void getNonOverloadedMethod_StaticNoArgMethod() throws NoSuchMethodException {
        Method method = ReflectionUtils.getNonOverloadedMethod(StaticMethodClass.class, "initialize");
        assertNotNull(method);
        assertTrue(Modifier.isStatic(method.getModifiers()));
        Method expected = StaticMethodClass.class.getMethod("initialize");
        assertEquals(expected, method);
    }

    /* --------------------------------------------------------------
     *  Empty and whitespace method names
     * -------------------------------------------------------------- */
    @Test
    void getNonOverloadedMethod_EmptyMethodName_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Sample.class, ""));
        assertTrue(ex.getMessage().contains("Method: () is not found on class"));
    }

    @Test
    void getNonOverloadedMethod_WhitespaceMethodName_ThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getNonOverloadedMethod(Sample.class, "   "));
        assertTrue(ex.getMessage().contains("Method:    () is not found on class"));
    }

    /* --------------------------------------------------------------
     *  Cache isolation between different classes with same method name
     * -------------------------------------------------------------- */
    @Test
    void getNonOverloadedMethod_CacheIsolationBetweenDifferentClasses() throws NoSuchMethodException {
        Method sampleMethod = ReflectionUtils.getNonOverloadedMethod(Sample.class, "foo");
        Method anotherMethod = ReflectionUtils.getNonOverloadedMethod(AnotherSample.class, "foo");
        assertNotSame(sampleMethod, anotherMethod, "Different classes must have distinct cache entries");
        assertEquals(Sample.class.getMethod("foo"), sampleMethod);
        // AnotherSample does not declare foo(); we add it for the test
        Method expected = AnotherSample.class.getMethod("foo");
        assertEquals(expected, anotherMethod);
    }

    /* --------------------------------------------------------------
     *  Cache reference equality verification (cache hit)
     * -------------------------------------------------------------- */
    @Test
    void getNonOverloadedMethod_CacheReferenceEquality() {
        Method first = ReflectionUtils.getNonOverloadedMethod(Sample.class, "foo");
        Method second = ReflectionUtils.getNonOverloadedMethod(Sample.class, "foo");
        assertSame(first, second, "Second call should return the same Method instance (cache hit)");
    }

    /* --------------------------------------------------------------
     *  SecurityManager denial for private reflective access
     * -------------------------------------------------------------- */
    @Test
    void getNonOverloadedMethod_SecurityManagerDenial_ThrowsSecurityException() {
        // Install a SecurityManager that forbids suppressAccessChecks
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new SecurityManager() {

            @Override
            public void checkPermission(Permission perm) {
                // Allow everything except ReflectPermission "suppressAccessChecks"
                if (perm instanceof ReflectPermission && "suppressAccessChecks".equals(perm.getName())) {
                    throw new SecurityException("Reflective access denied");
                }
            }
        });
        try {
            // Attempt to retrieve a private method – should propagate SecurityException
            assertThrows(SecurityException.class, () -> ReflectionUtils.getNonOverloadedMethod(PrivateMethodClass.class, "hidden"));
        } finally {
            // Restore original SecurityManager to avoid side effects on other tests
            System.setSecurityManager(original);
        }
    }
}
