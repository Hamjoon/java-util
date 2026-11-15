package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LRUCache_hashCode_Test {

    private static class ConstantHashMap<K, V> implements Map<K, V> {

        @Override
        public int hashCode() {
            return 42;
        }

        // Minimal viable implementation – other methods delegate to an internal map
        private final Map<K, V> delegate = new HashMap<>();

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return delegate.get(key);
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            delegate.putAll(m);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<K> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<V> values() {
            return delegate.values();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return delegate.entrySet();
        }
    }

    private static class ThrowingHashMap<K, V> implements Map<K, V> {

        @Override
        public int hashCode() {
            throw new IllegalStateException("forced");
        }

        // Minimal delegate for other methods
        private final Map<K, V> delegate = new HashMap<>();

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return delegate.get(key);
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            delegate.putAll(m);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<K> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<V> values() {
            return delegate.values();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return delegate.entrySet();
        }
    }

    private static class RuntimeThrowingHashMap<K, V> implements Map<K, V> {

        @Override
        public int hashCode() {
            throw new RuntimeException("runtime failure");
        }

        private final Map<K, V> delegate = new HashMap<>();

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return delegate.get(key);
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            delegate.putAll(m);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<K> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<V> values() {
            return delegate.values();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return delegate.entrySet();
        }
    }

    /**
     * Provide the two supported strategy types for the parameterized tests.
     */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Replaces the internal strategy map of the given cache via reflection.
     */
    private static <K, V> void setStrategy(LRUCache<K, V> cache, Map<K, V> newStrategy) {
        try {
            Field f = LRUCache.class.getDeclaredField("strategy");
            f.setAccessible(true);
            f.set(cache, newStrategy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verifies that {@code LRUCache.hashCode()} simply forwards to the underlying strategy's
     * {@code hashCode()} implementation. Two caches with identical contents must produce the same
     * hash code, irrespective of the chosen strategy.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeIsConsistentForEqualCaches(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cacheA = new LRUCache<>(5, strategy);
        LRUCache<Integer, String> cacheB = new LRUCache<>(5, strategy);
        cacheA.put(1, "one");
        cacheA.put(2, "two");
        cacheA.put(null, "nullKey");
        // null value
        cacheA.put(3, null);
        cacheB.put(1, "one");
        cacheB.put(2, "two");
        cacheB.put(null, "nullKey");
        cacheB.put(3, null);
        assertEquals(cacheA, cacheB, "Caches with identical entries should be equal");
        assertEquals(cacheA.hashCode(), cacheB.hashCode(), "Hash codes must be equal for equal caches");
    }

    /**
     * When the cache contents differ, the hash codes must also differ (high probability).
     * This test confirms that the hash code reflects the underlying map state.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeDiffersForDifferentCaches(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache1 = new LRUCache<>(5, strategy);
        LRUCache<Integer, String> cache2 = new LRUCache<>(5, strategy);
        cache1.put(1, "A");
        cache1.put(2, "B");
        cache2.put(1, "A");
        // different value for key 2
        cache2.put(2, "C");
        // The caches are not equal, therefore hash codes should not be equal.
        assertNotEquals(cache1, cache2, "Caches with different entries should not be equal");
        assertNotEquals(cache1.hashCode(), cache2.hashCode(), "Hash codes should differ for caches with different contents");
    }

    /**
     * Edge case: an empty cache must return the same hash code as the underlying empty strategy map.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeForEmptyCache(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> emptyCache = new LRUCache<>(10, strategy);
        // The hash code of an empty map is defined by the JDK implementation; we just ensure
        // that calling hashCode() does not throw and returns a consistent value.
        int hc1 = emptyCache.hashCode();
        int hc2 = emptyCache.hashCode();
        assertEquals(hc1, hc2, "Hash code of an empty cache should be stable across calls");
    }

    /**
     * Returns the internal strategy map of the given cache via reflection.
     */
    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> getStrategy(LRUCache<K, V> cache) {
        try {
            Field f = LRUCache.class.getDeclaredField("strategy");
            f.setAccessible(true);
            return (Map<K, V>) f.get(cache);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeDelegatesToEmptyStrategy(LRUCache.StrategyType type) {
        LRUCache<Integer, String> cache = new LRUCache<>(10, type);
        Map<Integer, String> strategy = getStrategy(cache);
        assertTrue(strategy.isEmpty(), "Strategy map should be empty");
        assertEquals(strategy.hashCode(), cache.hashCode(), "hashCode() must delegate to the empty strategy map");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeDelegatesToSingleEntry(LRUCache.StrategyType type) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, type);
        cache.put("a", 1);
        Map<String, Integer> strategy = getStrategy(cache);
        assertEquals(strategy.hashCode(), cache.hashCode(), "hashCode() must equal the strategy map's hash for a single entry");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeDelegatesToMultipleEntriesOrderIndependent(LRUCache.StrategyType type) {
        LRUCache<String, Integer> cache = new LRUCache<>(10, type);
        // Insert in one order
        cache.put("x", 10);
        cache.put("y", 20);
        cache.put("z", 30);
        Map<String, Integer> strategy = getStrategy(cache);
        int expected = strategy.hashCode();
        // Re‑create a cache with same entries in a different order
        LRUCache<String, Integer> cache2 = new LRUCache<>(10, type);
        cache2.put("z", 30);
        cache2.put("x", 10);
        cache2.put("y", 20);
        assertEquals(expected, cache2.hashCode(), "Map hashCode must be order‑independent; cache should delegate correctly");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeIncludesNullKeyAndNullValue(LRUCache.StrategyType type) {
        LRUCache<String, String> cache = new LRUCache<>(5, type);
        cache.put(null, null);
        cache.put("key", null);
        cache.put(null, "value");
        Map<String, String> strategy = getStrategy(cache);
        assertEquals(strategy.hashCode(), cache.hashCode(), "hashCode() must reflect null keys/values present in the strategy");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeReflectsModifications(LRUCache.StrategyType type) {
        LRUCache<Integer, Integer> cache = new LRUCache<>(5, type);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        int afterAdds = cache.hashCode();
        cache.remove(2);
        int afterRemove = cache.hashCode();
        assertNotEquals(afterAdds, afterRemove, "hashCode must change after a removal");
        assertEquals(getStrategy(cache).hashCode(), cache.hashCode(), "hashCode must always delegate to the current strategy state");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeConsistencyAfterRepeatedCalls(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(10, strategy);
        cache.put(1, "one");
        int first = cache.hashCode();
        int second = cache.hashCode();
        assertEquals(first, second, "hashCode must be deterministic when cache is unchanged");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeAfterClear(LRUCache.StrategyType type) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, type);
        cache.put(1, "a");
        cache.put(2, "b");
        cache.put(3, "c");
        cache.clear();
        Map<Integer, String> strategy = getStrategy(cache);
        assertTrue(strategy.isEmpty(), "Strategy should be empty after clear");
        assertEquals(strategy.hashCode(), cache.hashCode(), "hashCode after clear must equal empty strategy hash");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeAfterPutAll(LRUCache.StrategyType type) {
        LRUCache<String, Integer> cache = new LRUCache<>(10, type);
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);
        cache.putAll(source);
        assertEquals(source.hashCode(), cache.hashCode(), "hashCode must match the source map's hash after putAll");
    }

    @Test
    void hashCodeDelegatesToCustomConstantHashMap() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        // Inject the custom map
        setStrategy(cache, new ConstantHashMap<>());
        assertEquals(42, cache.hashCode(), "LRUCache.hashCode() must return the constant hash supplied by the custom map");
    }

    @Test
    void hashCodeMatchesLockingStrategy() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        cache.put(1, "one");
        cache.put(2, "two");
        Map<Integer, String> strategy = getStrategy(cache);
        assertEquals(strategy.hashCode(), cache.hashCode(), "Locking strategy hash must be delegated correctly");
    }

    @Test
    void hashCodeMatchesThreadedStrategy() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.THREADED);
        cache.put(1, "one");
        cache.put(2, "two");
        Map<Integer, String> strategy = getStrategy(cache);
        assertEquals(strategy.hashCode(), cache.hashCode(), "Threaded strategy hash must be delegated correctly");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeUnchangedWhenRemovingAbsentKey(LRUCache.StrategyType type) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, type);
        cache.put("a", 1);
        cache.put("b", 2);
        int before = cache.hashCode();
        cache.remove("non‑existent");
        assertEquals(before, cache.hashCode(), "hashCode must stay the same when removing a key that is not present");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeScalesToManyEntries(LRUCache.StrategyType type) {
        LRUCache<Integer, Integer> cache = new LRUCache<>(20000, type);
        for (int i = 0; i < 10_000; i++) {
            cache.put(i, i * 2);
        }
        Map<Integer, Integer> strategy = getStrategy(cache);
        assertEquals(strategy.hashCode(), cache.hashCode(), "hashCode must reflect all 10,000 inserted entries");
    }

    @Test
    void hashCodeThrowsWhenStrategyIsNull() throws Exception {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        // Force the private final field to null
        setStrategy(cache, null);
        assertThrows(NullPointerException.class, cache::hashCode, "Calling hashCode() with a null strategy should throw NPE");
    }

    @Test
    void hashCodePropagatesIllegalStateExceptionFromStrategy() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        setStrategy(cache, new ThrowingHashMap<>());
        IllegalStateException ex = assertThrows(IllegalStateException.class, cache::hashCode, "Exception from strategy.hashCode() must propagate unchanged");
        assertEquals("forced", ex.getMessage());
    }

    @Test
    void hashCodePropagatesRuntimeExceptionFromStrategy() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.THREADED);
        setStrategy(cache, new RuntimeThrowingHashMap<>());
        RuntimeException ex = assertThrows(RuntimeException.class, cache::hashCode, "RuntimeException from strategy.hashCode() must bubble up");
        assertEquals("runtime failure", ex.getMessage());
    }
}
