package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_setMethodCache_Test {

    /**
     * Holds a reference to a method that will be used in the assertions.
     */
    private static Method toStringMethod;

    /**
     * Simple class used for reflective method look‑ups.
     */
    static class TestClass {

        @Override
        public String toString() {
            return "TestClass instance";
        }
    }

    /**
     * Map implementation that counts calls to mutating methods.
     */
    static class CountingMap<K, V> extends HashMap<K, V> {

        int putCount = 0;

        int removeCount = 0;

        @Override
        public V put(K key, V value) {
            putCount++;
            return super.put(key, value);
        }

        @Override
        public V remove(Object key) {
            removeCount++;
            return super.remove(key);
        }
    }

    /**
     * Map that throws on any get() call – used to ensure setMethodCache does not read.
     */
    static class ThrowingMap<K, V> implements Map<K, V> {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public V get(Object key) {
            throw new RuntimeException("get() should not be called");
        }

        @Override
        public V put(K key, V value) {
            return null;
        }

        @Override
        public V remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<V> values() {
            return Collections.emptyList();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }
    }

    @BeforeAll
    static void setupBeforeAll() throws NoSuchMethodException {
        // Resolve a known method once; it will be used in multiple test cases.
        toStringMethod = TestClass.class.getMethod("toString");
        assertNotNull(toStringMethod, "Pre‑condition: toString method must be discoverable");
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the default cache is restored before each test.
        // The default implementation of ReflectionUtils uses an internal LRUCache,
        // but we can safely reset it to an empty map – this does not rely on private
        // fields because the public setter is used.
        ReflectionUtils.setMethodCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up by resetting the cache to its original state (empty map).
        ReflectionUtils.setMethodCache(new HashMap<>());
    }

    /**
     * Verify that calling {@code setMethodCache} with an empty map does not
     * interfere with subsequent reflective look‑ups.
     */
    @Test
    void setMethodCache_WithEmptyMap_DoesNotBreakGetMethod() {
        // GIVEN – an empty cache (already set in @BeforeEach)
        Map<Object, Method> emptyCache = Collections.emptyMap();
        ReflectionUtils.setMethodCache(emptyCache);
        // WHEN – we ask ReflectionUtils for a method that is known to exist
        Method result = ReflectionUtils.getMethod(TestClass.class, "toString");
        // THEN – the method is correctly returned and matches the pre‑resolved one
        assertNotNull(result, "Method should be found even after cache replacement");
        assertEquals(toStringMethod, result, "Returned method must be the same as the reflective lookup");
    }

    /**
     * Verify that a non‑empty map can be supplied without causing errors.
     * The map does not need to contain any specific keys – the implementation
     * simply stores the reference.
     */
    @Test
    void setMethodCache_WithNonEmptyMap_DoesNotThrow() {
        // GIVEN – a map containing an unrelated entry
        Map<Object, Method> customCache = new HashMap<>();
        customCache.put("dummyKey", toStringMethod);
        // WHEN – the cache is replaced
        assertDoesNotThrow(() -> ReflectionUtils.setMethodCache(customCache), "Replacing the cache with a custom map should not throw");
        // AND – a normal reflective lookup is performed
        Method result = ReflectionUtils.getMethod(TestClass.class, "toString");
        // THEN – the lookup still succeeds
        assertNotNull(result, "Method lookup must still succeed after custom cache injection");
        assertEquals(toStringMethod, result, "Returned method must still be correct");
    }

    /**
     * Verify that passing {@code null} as the cache leads to a predictable
     * failure when the cache is later used. The contract of the method does not
     * forbid {@code null}, but subsequent reflective calls are expected to
     * throw a {@link NullPointerException}.
     */
    @Test
    void setMethodCache_WithNull_LeadsToNPEOnSubsequentLookup() {
        // GIVEN – a null cache
        ReflectionUtils.setMethodCache(null);
        // WHEN / THEN – any operation that relies on the cache should now fail
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getMethod(TestClass.class, "toString"), "Using the cache after setting it to null should result in NPE");
    }

    /**
     * Helper that returns the current value of the private static METHOD_CACHE field.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Method> getCurrentMethodCache() throws Exception {
        Field field = ReflectionUtils.class.getDeclaredField("METHOD_CACHE");
        field.setAccessible(true);
        return (Map<Object, Method>) field.get(null);
    }

    @Test
    void setMethodCache_WithEmptyHashMap_ReferenceIsPreserved() throws Exception {
        Map<Object, Method> empty = new HashMap<>();
        ReflectionUtils.setMethodCache(empty);
        Map<Object, Method> cache = getCurrentMethodCache();
        assertSame(empty, cache, "The internal cache should reference the exact map supplied");
    }

    @Test
    void setMethodCache_WithLRUCache_InstanceIsPreserved() throws Exception {
        // LRUCache is part of the library; we assume a public constructor (size).
        Map<Object, Method> lru = new LRUCache<>(10);
        ReflectionUtils.setMethodCache(lru);
        Map<Object, Method> cache = getCurrentMethodCache();
        assertSame(lru, cache, "LRUCache instance should be stored unchanged");
    }

    @Test
    void setMethodCache_WithUnmodifiableMap_StoredAndImmutable() throws Exception {
        Map<Object, Method> modifiable = new HashMap<>();
        modifiable.put("key", toStringMethod);
        Map<Object, Method> unmodifiable = Collections.unmodifiableMap(modifiable);
        ReflectionUtils.setMethodCache(unmodifiable);
        Map<Object, Method> cache = getCurrentMethodCache();
        assertSame(unmodifiable, cache, "Unmodifiable map should be stored as‑is");
        // Attempting to modify through the reference must throw.
        assertThrows(UnsupportedOperationException.class, () -> cache.put("another", toStringMethod));
    }

    @Test
    void setMethodCache_WithPrepopulatedMap_PreservesEntries() throws Exception {
        Map<Object, Method> prepopulated = new HashMap<>();
        Method m = ReflectionUtils.class.getDeclaredMethod("setMethodCache", Map.class);
        prepopulated.put("key", m);
        ReflectionUtils.setMethodCache(prepopulated);
        Map<Object, Method> cache = getCurrentMethodCache();
        assertSame(prepopulated, cache);
        assertEquals(m, cache.get("key"));
    }

    @Test
    void setMethodCache_WithMapContainingNullKey_PreservesNullKey() throws Exception {
        Map<Object, Method> map = new HashMap<>();
        Method m = ReflectionUtils.class.getDeclaredMethod("setMethodCache", Map.class);
        map.put(null, m);
        ReflectionUtils.setMethodCache(map);
        Map<Object, Method> cache = getCurrentMethodCache();
        assertTrue(cache.containsKey(null), "Cache must contain the null key");
        assertEquals(m, cache.get(null));
    }

    @Test
    void setMethodCache_WithMapContainingNullValue_PreservesNullValue() throws Exception {
        Map<Object, Method> map = new HashMap<>();
        map.put("nullValue", null);
        ReflectionUtils.setMethodCache(map);
        Map<Object, Method> cache = getCurrentMethodCache();
        assertTrue(cache.containsKey("nullValue"));
        assertNull(cache.get("nullValue"));
    }

    @Test
    void setMethodCache_WithConcurrentHashMap_InstanceIsPreserved() throws Exception {
        Map<Object, Method> concurrent = new ConcurrentHashMap<>();
        ReflectionUtils.setMethodCache(concurrent);
        Map<Object, Method> cache = getCurrentMethodCache();
        assertSame(concurrent, cache);
    }

    @Test
    void setMethodCache_WithSynchronizedMapWrapper_InstanceIsPreserved() throws Exception {
        Map<Object, Method> sync = Collections.synchronizedMap(new HashMap<>());
        ReflectionUtils.setMethodCache(sync);
        Map<Object, Method> cache = getCurrentMethodCache();
        assertSame(sync, cache);
    }

    @Test
    void setMethodCache_WithCountingMap_NoInteractionOccurs() throws Exception {
        CountingMap<Object, Method> counting = new CountingMap<>();
        ReflectionUtils.setMethodCache(counting);
        Map<Object, Method> cache = getCurrentMethodCache();
        assertSame(counting, cache);
        assertEquals(0, counting.putCount, "setMethodCache must not call put()");
        assertEquals(0, counting.removeCount, "setMethodCache must not call remove()");
    }

    @Test
    void setMethodCache_WithWeakHashMap_InstanceIsPreserved() throws Exception {
        Map<Object, Method> weak = new WeakHashMap<>();
        ReflectionUtils.setMethodCache(weak);
        Map<Object, Method> cache = getCurrentMethodCache();
        assertSame(weak, cache);
    }

    @Test
    void setMethodCache_WithTreeMapAndComparator_InstanceIsPreserved() throws Exception {
        Comparator<Object> comp = Comparator.comparing(Object::toString);
        Map<Object, Method> tree = new TreeMap<>(comp);
        ReflectionUtils.setMethodCache(tree);
        Map<Object, Method> cache = getCurrentMethodCache();
        assertSame(tree, cache);
    }

    @Test
    void setMethodCache_WithGenericMapVariance_InstanceIsPreserved() throws Exception {
        Map<String, Method> generic = new HashMap<>();
        generic.put("s", toStringMethod);
        // The method signature uses raw Map<Object, Method>, type erasure allows this.
        ReflectionUtils.setMethodCache((Map) generic);
        Map<Object, Method> cache = getCurrentMethodCache();
        assertSame(generic, cache);
        assertEquals(toStringMethod, cache.get("s"));
    }

    @Test
    void setMethodCache_WithThrowingMap_NoExceptionDuringAssignment() throws Exception {
        ThrowingMap<Object, Method> throwing = new ThrowingMap<>();
        // No call to get() should happen inside setMethodCache.
        assertDoesNotThrow(() -> ReflectionUtils.setMethodCache(throwing));
        Map<Object, Method> cache = getCurrentMethodCache();
        assertSame(throwing, cache);
    }
}
