package com.cedarsoftware.util;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setMethodCache_Test {

    /**
     * Simple class used for reflective look‑ups in the tests.
     */
    static class TestClass {

        @Override
        public String toString() {
            return "TestClass instance";
        }
    }

    /**
     * Custom map subclass that records whether {@code put} is invoked.
     */
    static class MyMap extends HashMap<Object, Method> {

        boolean putCalled = false;

        @Override
        public Method put(Object key, Method value) {
            putCalled = true;
            return super.put(key, value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the cache is in a known state before each test.
        ReflectionUtils.setMethodCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the cache to an empty map so that one test does not affect another.
        ReflectionUtils.setMethodCache(new HashMap<>());
    }

    /**
     * Verify that calling {@code setMethodCache} with an empty map does not break subsequent
     * reflective look‑ups.
     */
    @Test
    void testSetMethodCacheWithEmptyMap() throws Exception {
        Map<Object, Method> emptyCache = new HashMap<>();
        ReflectionUtils.setMethodCache(emptyCache);
        Method m = ReflectionUtils.getMethod(TestClass.class, "toString");
        assertNotNull(m, "Method should be found even after cache replacement");
        assertEquals("toString", m.getName());
    }

    /**
     * Verify that a custom cache containing a pre‑populated entry does not interfere with the
     * normal operation of {@code getMethod}.  The method retrieved by {@code getMethod}
     * should be the same instance that we placed into the cache (the cache is a {@code Map}
     * of {@code Method} objects, so identity equality can be checked).
     */
    @Test
    void testSetMethodCacheWithPrepopulatedMap() throws Exception {
        Method expectedMethod = TestClass.class.getMethod("toString");
        Map<Object, Method> customCache = new HashMap<>();
        customCache.put("dummyKey", expectedMethod);
        ReflectionUtils.setMethodCache(customCache);
        Method actualMethod = ReflectionUtils.getMethod(TestClass.class, "toString");
        assertNotNull(actualMethod, "Method lookup should still succeed");
        assertEquals(expectedMethod, actualMethod);
    }

    /**
     * Verify that replacing the cache with a map that contains a *different* Method does not
     * corrupt subsequent look‑ups for unrelated methods.
     */
    @Test
    void testSetMethodCacheDoesNotAffectOtherMethods() throws Exception {
        Method otherMethod = Object.class.getMethod("hashCode");
        Map<Object, Method> customCache = new HashMap<>();
        customCache.put("otherKey", otherMethod);
        ReflectionUtils.setMethodCache(customCache);
        Method toStringMethod = ReflectionUtils.getMethod(TestClass.class, "toString");
        assertNotNull(toStringMethod, "Lookup should succeed even when cache holds unrelated entries");
        assertEquals("toString", toStringMethod.getName());
        assertEquals(TestClass.class, toStringMethod.getDeclaringClass());
    }

    /**
     * Helper to read the private static {@code METHOD_CACHE} field via reflection.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Method> getInternalCache() throws Exception {
        Field f = ReflectionUtils.class.getDeclaredField("METHOD_CACHE");
        f.setAccessible(true);
        return (Map<Object, Method>) f.get(null);
    }

    /**
     * Case: Pass a {@code null} reference to {@code setMethodCache} and ensure the static
     * cache reference becomes {@code null}.
     */
    @Test
    void testSetMethodCacheWithNull() throws Exception {
        ReflectionUtils.setMethodCache(null);
        Map<Object, Method> cache = getInternalCache();
        assertNull(cache, "METHOD_CACHE should be null after passing null");
    }

    /**
     * Case: Provide an empty mutable map and verify that the internal cache references the
     * exact same instance.
     */
    @Test
    void testSetMethodCacheWithExactEmptyMapInstance() throws Exception {
        Map<Object, Method> emptyMap = new HashMap<>();
        ReflectionUtils.setMethodCache(emptyMap);
        Map<Object, Method> internal = getInternalCache();
        assertSame(emptyMap, internal, "Internal cache should reference the supplied empty map");
    }

    /**
     * Case: Provide a map that already contains a valid {@code Method} entry and verify the
     * entry is preserved.
     */
    @Test
    void testSetMethodCacheWithPrepopulatedEntry() throws Exception {
        Method lengthMethod = String.class.getMethod("length");
        Object key = new Object();
        Map<Object, Method> prepopulated = new HashMap<>();
        prepopulated.put(key, lengthMethod);
        ReflectionUtils.setMethodCache(prepopulated);
        Map<Object, Method> internal = getInternalCache();
        assertSame(prepopulated, internal, "Cache reference should be the supplied map");
        assertEquals(lengthMethod, internal.get(key), "Entry should be preserved unchanged");
    }

    /**
     * Case: Pass an instance of the original {@code LRUCache} type (the default cache) and
     * verify that the reference is kept.
     */
    @Test
    void testSetMethodCacheWithLRUCacheInstance() throws Exception {
        try {
            Class<?> lruClass = Class.forName("com.cedarsoftware.util.LRUCache");
            Constructor<?> ctor = lruClass.getDeclaredConstructor(int.class);
            ctor.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Object, Method> lruCache = (Map<Object, Method>) ctor.newInstance(2);
            Method m1 = String.class.getMethod("length");
            Method m2 = String.class.getMethod("isEmpty");
            lruCache.put("k1", m1);
            lruCache.put("k2", m2);
            ReflectionUtils.setMethodCache(lruCache);
            Map<Object, Method> internal = getInternalCache();
            assertSame(lruCache, internal, "Internal cache should be the supplied LRUCache instance");
            assertEquals(m1, internal.get("k1"));
            assertEquals(m2, internal.get("k2"));
        } catch (ClassNotFoundException e) {
            // Skip test if LRUCache is not present.
            assumeTrue(false, "LRUCache class not found – test skipped");
        }
    }

    /**
     * Case: Verify that the cache stores the reference, not a copy, by mutating the map after
     * the call.
     */
    @Test
    void testSetMethodCacheReferenceSemantics() throws Exception {
        Map<Object, Method> mutable = new HashMap<>();
        ReflectionUtils.setMethodCache(mutable);
        Method lengthMethod = String.class.getMethod("length");
        mutable.put("newKey", lengthMethod);
        Map<Object, Method> internal = getInternalCache();
        assertSame(mutable, internal, "Cache should still be the same mutable map instance");
        assertEquals(lengthMethod, internal.get("newKey"));
    }

    /**
     * Case: Provide an immutable map and verify that attempts to modify the cache throw
     * {@link UnsupportedOperationException}.
     */
    @Test
    void testSetMethodCacheWithUnmodifiableMap() throws Exception {
        Map<Object, Method> base = new HashMap<>();
        Map<Object, Method> unmodifiable = Collections.unmodifiableMap(base);
        ReflectionUtils.setMethodCache(unmodifiable);
        Map<Object, Method> internal = getInternalCache();
        assertSame(unmodifiable, internal, "Cache reference should be the unmodifiable map");
        assertThrows(UnsupportedOperationException.class, () -> internal.put("k", String.class.getMethod("length")));
    }

    /**
     * Case: Map containing a {@code null} key.
     */
    @Test
    void testSetMethodCacheWithNullKey() throws Exception {
        Method lengthMethod = String.class.getMethod("length");
        Map<Object, Method> map = new HashMap<>();
        map.put(null, lengthMethod);
        ReflectionUtils.setMethodCache(map);
        Map<Object, Method> internal = getInternalCache();
        assertSame(map, internal);
        assertEquals(lengthMethod, internal.get(null));
    }

    /**
     * Case: Map containing a {@code null} value.
     */
    @Test
    void testSetMethodCacheWithNullValue() throws Exception {
        Object key = new Object();
        Map<Object, Method> map = new HashMap<>();
        map.put(key, null);
        ReflectionUtils.setMethodCache(map);
        Map<Object, Method> internal = getInternalCache();
        assertSame(map, internal);
        assertTrue(internal.containsKey(key));
        assertNull(internal.get(key));
    }

    /**
     * Case: Use a {@code ConcurrentHashMap} with three distinct entries.
     */
    @Test
    void testSetMethodCacheWithConcurrentHashMap() throws Exception {
        ConcurrentHashMap<Object, Method> concurrent = new ConcurrentHashMap<>();
        concurrent.put("len", String.class.getMethod("length"));
        concurrent.put("empty", String.class.getMethod("isEmpty"));
        concurrent.put("charAt", String.class.getMethod("charAt", int.class));
        ReflectionUtils.setMethodCache(concurrent);
        Map<Object, Method> internal = getInternalCache();
        assertSame(concurrent, internal, "Internal cache should be the supplied ConcurrentHashMap");
        assertEquals(3, internal.size());
        assertEquals(String.class.getMethod("length"), internal.get("len"));
    }

    /**
     * Case: Provide a custom map subclass and verify that {@code setMethodCache} does not
     * invoke mutating operations during assignment.
     */
    @Test
    void testSetMethodCacheWithCustomMapSubclass() throws Exception {
        MyMap myMap = new MyMap();
        Method lengthMethod = String.class.getMethod("length");
        myMap.put("k", lengthMethod);
        ReflectionUtils.setMethodCache(myMap);
        Map<Object, Method> internal = getInternalCache();
        assertSame(myMap, internal, "Cache should be the custom map instance");
        assertFalse(myMap.putCalled, "setMethodCache must not call put during assignment");
        assertEquals(lengthMethod, internal.get("k"));
    }

    /**
     * Case: Call {@code setMethodCache} twice with different map instances.
     */
    @Test
    void testSetMethodCacheMultipleAssignments() throws Exception {
        Map<Object, Method> firstMap = new HashMap<>();
        firstMap.put("first", String.class.getMethod("length"));
        ReflectionUtils.setMethodCache(firstMap);
        assertSame(firstMap, getInternalCache());
        Map<Object, Method> secondMap = new LinkedHashMap<>();
        secondMap.put("second", String.class.getMethod("isEmpty"));
        ReflectionUtils.setMethodCache(secondMap);
        assertSame(secondMap, getInternalCache());
        assertFalse(getInternalCache().containsKey("first"));
        assertEquals(String.class.getMethod("isEmpty"), getInternalCache().get("second"));
    }

    /**
     * Case: Provide a dynamic proxy that implements {@code Map<Object, Method>}.
     */
    @Test
    void testSetMethodCacheWithProxyMap() throws Exception {
        Map<Object, Method> delegate = new HashMap<>();
        delegate.put("proxyKey", String.class.getMethod("length"));
        @SuppressWarnings("unchecked")
        Map<Object, Method> proxy = (Map<Object, Method>) Proxy.newProxyInstance(ReflectionUtils_setMethodCache_Test.class.getClassLoader(), new Class[] { Map.class }, (Object proxyObj, Method method, Object[] args) -> method.invoke(delegate, args));
        ReflectionUtils.setMethodCache(proxy);
        Map<Object, Method> internal = getInternalCache();
        assertSame(proxy, internal, "Internal cache should be the proxy instance");
        assertEquals(String.class.getMethod("length"), internal.get("proxyKey"));
    }
}
