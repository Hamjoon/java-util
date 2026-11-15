package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentModificationException;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;

class LRUCache_putAll_Test {

    private LRUCache<Integer, String> cache;

    /**
     * A custom {@link Map} whose {@code entrySet().iterator()} throws
     * {@link ConcurrentModificationException}.  This verifies that LRUCache
     * does not swallow such runtime exceptions from the supplied map.
     */
    private static class CustomMapThrowingOnIteration<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> delegate = new HashMap<>();

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<Entry<K, V>>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    throw new ConcurrentModificationException("Iteration not allowed");
                }

                @Override
                public int size() {
                    return delegate.size();
                }
            };
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required.
        cache = null;
    }

    /**
     * Provide the two strategy types used by LRUCache.
     */
    static Stream<LRUCache.StrategyType> strategies() {
        return Stream.of(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Helper to create a cache with a small capacity (3) for all tests.
     */
    private void initCache(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
    }

    /**
     * Verify that putAll correctly copies all entries into the cache.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllAddsAllEntries(LRUCache.StrategyType strategy) {
        initCache(strategy);
        Map<Integer, String> source = new LinkedHashMap<>();
        source.put(1, "One");
        source.put(2, "Two");
        source.put(3, "Three");
        cache.putAll(source);
        assertEquals(3, cache.size(), "Cache should contain three entries after putAll");
        assertEquals("One", cache.get(1));
        assertEquals("Two", cache.get(2));
        assertEquals("Three", cache.get(3));
    }

    /**
     * Ensure that putAll respects the LRU eviction policy when the source map exceeds capacity.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllTriggersEvictionWhenOverCapacity(LRUCache.StrategyType strategy) {
        initCache(strategy);
        // Fill cache to its capacity first.
        cache.put(10, "Ten");
        cache.put(20, "Twenty");
        cache.put(30, "Thirty");
        // New map contains more entries than capacity.
        Map<Integer, String> newEntries = new LinkedHashMap<>();
        newEntries.put(40, "Forty");
        newEntries.put(50, "Fifty");
        newEntries.put(60, "Sixty");
        cache.putAll(newEntries);
        // After putAll, cache size must still be <= capacity (3).
        assertTrue(cache.size() <= 3, "Cache size must not exceed its capacity after putAll");
        // The most recent entries (40,50,60) should be present; older ones should be evicted.
        assertNull(cache.get(10), "Key 10 should have been evicted");
        assertNull(cache.get(20), "Key 20 should have been evicted");
        assertNull(cache.get(30), "Key 30 should have been evicted");
        assertEquals("Forty", cache.get(40));
        assertEquals("Fifty", cache.get(50));
        assertEquals("Sixty", cache.get(60));
    }

    /**
     * Verify that putAll correctly handles null keys and null values.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllWithNullKeyAndValue(LRUCache.StrategyType strategy) {
        initCache(strategy);
        Map<Integer, String> source = new HashMap<>();
        source.put(null, null);
        source.put(1, null);
        source.put(null, "NotNull");
        cache.putAll(source);
        // The map should contain the last value associated with the null key.
        assertTrue(cache.containsKey(null), "Cache should contain a null key");
        assertEquals("NotNull", cache.get(null), "Null key should map to the last put value");
        // Value null for a non‑null key is allowed.
        assertTrue(cache.containsKey(1));
        assertNull(cache.get(1));
    }

    /**
     * Ensure that putAll on an empty source map leaves the cache unchanged.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllWithEmptyMapDoesNotChangeCache(LRUCache.StrategyType strategy) {
        initCache(strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        Map<Integer, String> empty = Collections.emptyMap();
        cache.putAll(empty);
        assertEquals(2, cache.size());
        assertEquals("A", cache.get(1));
        assertEquals("B", cache.get(2));
    }

    /**
     * putAll(null) must throw NullPointerException as dictated by the Map contract.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllNullMapThrowsNPE(LRUCache.StrategyType strategy) {
        initCache(strategy);
        assertThrows(NullPointerException.class, () -> cache.putAll(null), "Calling putAll with a null map should throw NullPointerException");
    }

    /**
     * Simulate an underlying strategy that rejects null keys by throwing
     * {@link IllegalArgumentException}.  The cache should propagate this exception.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllNullKeyIllegalArgument(LRUCache.StrategyType strategy) throws Exception {
        initCache(strategy);
        // Create a mock for the private 'strategy' field.
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = Mockito.mock(Map.class);
        // Configure the mock to throw IllegalArgumentException when putAll is invoked.
        doThrow(new IllegalArgumentException("null keys not allowed")).when(mockStrategy).putAll(anyMap());
        // Replace the private final field via reflection.
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(cache, mockStrategy);
        Map<Integer, String> source = new HashMap<>();
        // null key triggers the exception in the mock.
        source.put(null, "value");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cache.putAll(source), "The underlying strategy should propagate IllegalArgumentException for null keys");
        assertEquals("null keys not allowed", ex.getMessage());
    }

    /**
     * Verify that a map throwing {@link ConcurrentModificationException} during iteration propagates the exception.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllCustomMapThrowsCME(LRUCache.StrategyType strategy) {
        initCache(strategy);
        CustomMapThrowingOnIteration<Integer, String> badMap = new CustomMapThrowingOnIteration<>();
        badMap.put(1, "one");
        badMap.put(2, "two");
        assertThrows(ConcurrentModificationException.class, () -> cache.putAll(badMap), "putAll should propagate ConcurrentModificationException from the supplied map");
    }
}
