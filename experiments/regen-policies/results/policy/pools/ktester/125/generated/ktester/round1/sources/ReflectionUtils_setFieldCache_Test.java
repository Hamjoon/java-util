package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setFieldCache_Test {

    /**
     * Simple class used by the tests – it has two declared fields.
     */
    static class TestClass {

        public int field1;

        private String field2;
    }

    /**
     * Custom map that records calls to {@code put}.
     */
    static class CustomMap extends HashMap<Object, Field> {

        private final List<Object> putCalls = new ArrayList<>();

        @Override
        public Field put(Object key, Field value) {
            putCalls.add(key);
            return super.put(key, value);
        }

        List<Object> getPutCalls() {
            return putCalls;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the default cache is restored before each test so that tests are independent.
        // The default cache is created inside ReflectionUtils, therefore we simply reset it
        // by calling the method with a fresh empty map.
        ReflectionUtils.setFieldCache(new HashMap<>());
    }

    /**
     * Verifies that calling {@code setFieldCache} with an empty map does not affect
     * the ability of {@link ReflectionUtils#getAllDeclaredFields(Class)} to return
     * the correct fields for a class.
     */
    @Test
    void testSetFieldCacheWithEmptyMap() {
        Map<Object, Field> emptyMap = new HashMap<>();
        // Act – replace the internal cache with an empty map.
        ReflectionUtils.setFieldCache(emptyMap);
        // Assert – the public method still works and returns the expected fields.
        assertEquals(2, ReflectionUtils.getAllDeclaredFields(TestClass.class).size(), "Expected two fields (field1 and field2) to be discovered after cache replacement");
    }

    /**
     * Verifies that the cache can be replaced multiple times without causing
     * side‑effects. Each replacement uses a distinct map instance.
     */
    @Test
    void testSetFieldCacheMultipleReplacements() {
        // First replacement with an empty map.
        Map<Object, Field> firstMap = new HashMap<>();
        ReflectionUtils.setFieldCache(firstMap);
        assertEquals(2, ReflectionUtils.getAllDeclaredFields(TestClass.class).size(), "First replacement should not break field discovery");
        // Second replacement with a map that already contains one entry.
        Map<Object, Field> secondMap = new HashMap<>();
        // Populate the map with a legitimate field entry – the key type is Object,
        // so we can use the Field instance itself as the key for the purpose of the test.
        Field field1 = ReflectionUtils.getField(TestClass.class, "field1");
        assertNotNull(field1, "field1 should be obtainable via getField");
        secondMap.put(field1, field1);
        ReflectionUtils.setFieldCache(secondMap);
        // The public API must still return both fields; the internal cache content is
        // irrelevant to the contract of getAllDeclaredFields.
        assertEquals(2, ReflectionUtils.getAllDeclaredFields(TestClass.class).size(), "Second replacement should also preserve normal behaviour");
    }

    /**
     * Ensures that passing {@code null} to {@code setFieldCache} does not throw
     * an exception at call time (the method accepts a null reference). Subsequent
     * usage of the cache will result in a {@code NullPointerException}, which
     * is the expected behaviour of the library when the cache is deliberately
     * cleared.
     */
    @Test
    void testSetFieldCacheWithNull() {
        // Act – the method accepts null; we verify that no exception is thrown here.
        assertDoesNotThrow(() -> ReflectionUtils.setFieldCache(null));
        // After setting a null cache, any operation that tries to use the cache will
        // throw NPE. We verify this by calling a method that relies on the cache.
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getAllDeclaredFields(TestClass.class), "Using the utilities after a null cache should result in NPE");
    }

    /**
     * Helper to obtain the private static FIELD_NAME_CACHE via reflection.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Field> getInternalCache() throws Exception {
        Field cacheField = ReflectionUtils.class.getDeclaredField("FIELD_NAME_CACHE");
        cacheField.setAccessible(true);
        return (Map<Object, Field>) cacheField.get(null);
    }

    @Test
    void testSetFieldCacheWithNullMap() throws Exception {
        // Act – replace the cache with null.
        ReflectionUtils.setFieldCache(null);
        // Verify that the internal static field is now null.
        Map<Object, Field> cache = getInternalCache();
        assertNull(cache, "FIELD_NAME_CACHE should be null after passing null");
    }

    @Test
    void testSetFieldCacheWithMapContainingFieldValues() throws Exception {
        // Prepare a map whose values are real Field objects.
        Map<Object, Field> map = new HashMap<>();
        Field f1 = ReflectionUtils.getField(TestClass.class, "field1");
        Field f2 = ReflectionUtils.getField(TestClass.class, "field2");
        map.put("key1", f1);
        map.put("key2", f2);
        ReflectionUtils.setFieldCache(map);
        Map<Object, Field> cache = getInternalCache();
        assertSame(map, cache, "Internal cache must reference the supplied map");
        assertEquals(2, cache.size(), "Cache should contain the two supplied entries");
        assertEquals(f1, cache.get("key1"));
        assertEquals(f2, cache.get("key2"));
    }

    @Test
    void testSetFieldCacheWithNonFieldValues() throws Exception {
        // Values are strings – the method does not validate generic types.
        Map<Object, Field> map = new HashMap<>();
        // unsafe cast to satisfy compile‑time
        map.put("someKey", (Field) (Object) "notAField");
        ReflectionUtils.setFieldCache(map);
        Map<Object, Field> cache = getInternalCache();
        assertSame(map, cache, "Cache reference must be unchanged");
        assertEquals(1, cache.size());
        assertEquals("notAField", cache.get("someKey"));
    }

    @Test
    void testSetFieldCacheWithLRUCacheInstance() throws Exception {
        // LRUCache is the default implementation used by ReflectionUtils.
        LRUCache<Object, Field> lru = new LRUCache<>(5);
        Field f = ReflectionUtils.getField(TestClass.class, "field1");
        lru.put("k", f);
        ReflectionUtils.setFieldCache(lru);
        Map<Object, Field> cache = getInternalCache();
        assertSame(lru, cache, "FIELD_NAME_CACHE should reference the supplied LRUCache instance");
        assertEquals(1, cache.size());
        assertEquals(f, cache.get("k"));
    }

    @Test
    void testSetFieldCacheWithUnmodifiableMap() throws Exception {
        Map<Object, Field> mutable = new HashMap<>();
        mutable.put("k1", (Field) (Object) "v1");
        mutable.put("k2", (Field) (Object) "v2");
        Map<Object, Field> unmodifiable = Collections.unmodifiableMap(mutable);
        ReflectionUtils.setFieldCache(unmodifiable);
        Map<Object, Field> cache = getInternalCache();
        assertSame(unmodifiable, cache, "Cache must be the exact unmodifiable map instance");
        // Verify that attempts to modify the cache throw the expected exception.
        assertThrows(UnsupportedOperationException.class, () -> cache.put("k3", (Field) (Object) "v3"));
    }

    @Test
    void testSetFieldCacheWithNullKeyAndValue() throws Exception {
        Map<Object, Field> map = new HashMap<>();
        map.put(null, null);
        ReflectionUtils.setFieldCache(map);
        Map<Object, Field> cache = getInternalCache();
        assertSame(map, cache);
        assertTrue(cache.containsKey(null), "Cache should contain a null key");
        assertNull(cache.get(null), "Cache should map null key to null value");
    }

    @Test
    void testSetFieldCacheWithConcurrentHashMap() throws Exception {
        ConcurrentHashMap<Object, Field> concurrent = new ConcurrentHashMap<>();
        concurrent.put("c1", (Field) (Object) "fieldC");
        concurrent.put("c2", (Field) (Object) "fieldD");
        ReflectionUtils.setFieldCache(concurrent);
        Map<Object, Field> cache = getInternalCache();
        assertSame(concurrent, cache);
        assertEquals(2, cache.size());
        assertEquals("fieldC", cache.get("c1"));
        assertEquals("fieldD", cache.get("c2"));
    }

    @Test
    void testSetFieldCacheWithCustomMapImplementation() throws Exception {
        CustomMap custom = new CustomMap();
        custom.put("a", (Field) (Object) "A");
        custom.put("b", (Field) (Object) "B");
        ReflectionUtils.setFieldCache(custom);
        Map<Object, Field> cache = getInternalCache();
        assertSame(custom, cache);
        assertEquals(2, cache.size());
        // Verify that the overridden behavior is still present.
        assertEquals(List.of("a", "b"), custom.getPutCalls());
    }

    @Test
    void testSetFieldCacheWithDynamicProxyMap() throws Exception {
        // Real backing map.
        Map<Object, Field> backing = new HashMap<>();
        backing.put("p1", (Field) (Object) "fieldP");
        // Proxy that delegates all Map methods to the backing map.
        InvocationHandler handler = (proxy, method, args) -> method.invoke(backing, args);
        @SuppressWarnings("unchecked")
        Map<Object, Field> proxy = (Map<Object, Field>) Proxy.newProxyInstance(Map.class.getClassLoader(), new Class<?>[] { Map.class }, handler);
        ReflectionUtils.setFieldCache(proxy);
        Map<Object, Field> cache = getInternalCache();
        assertSame(proxy, cache);
        // Operations go through the proxy to the backing map.
        assertEquals("fieldP", cache.get("p1"));
        cache.put("p2", (Field) (Object) "fieldP2");
        assertEquals("fieldP2", backing.get("p2"));
    }

    @Test
    void testSetFieldCacheWithSynchronizedMap() throws Exception {
        Map<Object, Field> syncMap = Collections.synchronizedMap(new HashMap<>());
        syncMap.put("s1", (Field) (Object) "S1");
        ReflectionUtils.setFieldCache(syncMap);
        Map<Object, Field> cache = getInternalCache();
        assertSame(syncMap, cache);
        assertEquals("S1", cache.get("s1"));
    }

    @Test
    void testSetFieldCacheWithEmptyImmutableMap() throws Exception {
        Map<Object, Field> emptyImmutable = Collections.emptyMap();
        ReflectionUtils.setFieldCache(emptyImmutable);
        Map<Object, Field> cache = getInternalCache();
        assertSame(emptyImmutable, cache);
        assertTrue(cache.isEmpty());
        // Verify immutability.
        assertThrows(UnsupportedOperationException.class, () -> cache.put("x", (Field) (Object) "X"));
    }

    @Test
    void testSetFieldCacheWithNonMapThrowsClassCastException() {
        // Force a raw cast to Map at runtime; this will throw ClassCastException.
        @SuppressWarnings("unchecked")
        Map<Object, Field> bogus = (Map<Object, Field>) (Object) new ArrayList<>();
        assertThrows(ClassCastException.class, () -> ReflectionUtils.setFieldCache(bogus), "Passing a non‑Map object should result in a ClassCastException");
    }
}
