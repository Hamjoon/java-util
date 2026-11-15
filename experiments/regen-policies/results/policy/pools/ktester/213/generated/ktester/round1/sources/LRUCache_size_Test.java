package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.mockito.Mockito.*;

// Mockito not strictly required but kept per template
@ExtendWith(MockitoExtension.class)
class LRUCache_size_Test {

    private LRUCache<Integer, String> lruCache;

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache reference is cleared between tests
        lruCache = null;
    }

    /**
     * Provides the two strategy types supported by LRUCache.
     */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Helper to create a cache for a given strategy.
     */
    void setUp(LRUCache.StrategyType strategy) {
        // Use the public constructor that accepts capacity and strategy type.
        lruCache = new LRUCache<>(capacity, strategy);
    }

    /**
     * Verifies that size() reflects the number of entries currently stored.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testSize(LRUCache.StrategyType strategy) {
        setUp(strategy, 3);
        // initially empty
        assertEquals(0, lruCache.size(), "Size of a newly created cache should be 0");
        // add two entries
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        assertEquals(2, lruCache.size(), "Size should match number of inserted entries");
        // add a third entry (capacity reached)
        lruCache.put(3, "C");
        assertEquals(3, lruCache.size(), "Size should be capacity when fully populated");
        // add a fourth entry, causing eviction of the least‑recently‑used entry
        lruCache.put(4, "D");
        // After eviction the size must still be equal to capacity (3)
        assertEquals(3, lruCache.size(), "Size should stay at capacity after eviction");
        // remove an entry explicitly
        lruCache.remove(2);
        assertEquals(2, lruCache.size(), "Size should decrement after a removal");
    }

    /**
     * Additional sanity check: size after clearing the cache.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testSizeAfterClear(LRUCache.StrategyType strategy) {
        setUp(strategy, 3);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        assertEquals(3, lruCache.size());
        lruCache.clear();
        assertEquals(0, lruCache.size(), "Size should be 0 after clear()");
        assertTrue(lruCache.isEmpty(), "Cache should report empty after clear()");
    }

    /**
     * Size on a freshly created cache (already covered, but kept for clarity).
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeWhenNewlyCreated(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        assertEquals(0, lruCache.size(), "When the LRUCache is newly created and contains no entries, size() should return 0.");
    }

    /**
     * Size after inserting a single entry.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeAfterSinglePut(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        lruCache.put(10, "X");
        assertEquals(1, lruCache.size(), "After inserting a single key‑value pair using put(), size() should return 1.");
    }

    /**
     * Size after inserting three entries (capacity >= 3).
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeAfterThreePuts(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        assertEquals(3, lruCache.size(), "After inserting three distinct entries (less than or equal to the cache capacity), size() should return 3.");
    }

    /**
     * Size after removal of one entry.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeAfterRemoval(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        assertEquals(3, lruCache.size());
        lruCache.remove(2);
        assertEquals(2, lruCache.size(), "After removing one of the previously added entries with remove(), size() should reflect the decreased count (3 → 2).");
    }

    /**
     * Size after clear on a non‑empty cache.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeAfterClearNonEmpty(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.clear();
        assertEquals(0, lruCache.size(), "After calling clear() on a non‑empty cache, size() should return 0.");
    }

    /**
     * Size when the underlying strategy map is mocked to return a fixed value.
     */
    @Test
    void sizeWhenStrategyMockReturnsFive() throws Exception {
        // Use any strategy for construction; we'll replace the internal map later.
        setUp(LRUCache.StrategyType.LOCKING, 10);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.size()).thenReturn(5);
        // Inject the mock via reflection
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(lruCache, mockStrategy);
        assertEquals(5, lruCache.size(), "When the underlying strategy map is a mocked Map whose size() method returns 5, LRUCache.size() must forward that value unchanged.");
    }

    /**
     * Eviction test: capacity 3, four puts → size stays at capacity.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeAfterEvictionWhenCapacityExceeded(LRUCache.StrategyType strategy) {
        setUp(strategy, 3);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        // should evict key 1
        lruCache.put(4, "D");
        assertEquals(3, lruCache.size(), "Create an LRUCache with capacity 3, put four distinct entries. The fourth insertion should evict the least‑recently‑used entry, so size() must report the configured capacity (3) rather than 4.");
    }

    /**
     * putAll exceeding capacity: capacity 2, putAll with 3 entries → size = 2.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeAfterPutAllExceedingCapacity(LRUCache.StrategyType strategy) {
        setUp(strategy, 2);
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "A");
        source.put(2, "B");
        source.put(3, "C");
        lruCache.putAll(source);
        // The cache should retain only the two most‑recently‑added entries (2 and 3)
        assertEquals(2, lruCache.size(), "Create an LRUCache with capacity 2, then call putAll() with a map containing three distinct entries. After the operation the cache can hold only the two most‑recently‑added entries, therefore size() should return 2.");
    }

    /**
     * Replacing an existing key does not change the size.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeAfterReplacingExistingKey(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        lruCache.put(1, "A");
        assertEquals(1, lruCache.size());
        // replace value
        lruCache.put(1, "B");
        assertEquals(1, lruCache.size(), "Insert a key‑value pair, then insert another pair with the same key but a different value. The cache should replace the old value without increasing the entry count, so size() stays 1.");
    }

    /**
     * Removing a non‑existent key does not affect size.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeAfterRemovingNonExistentKey(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        assertEquals(2, lruCache.size());
        // key not present
        lruCache.remove(99);
        assertEquals(2, lruCache.size(), "Add two distinct entries, then call remove() with a key that was never added. The removal should have no effect and size() must still report 2.");
    }

    /**
     * Clear on an already empty cache leaves size at 0.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeAfterClearOnEmptyCache(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        assertEquals(0, lruCache.size());
        lruCache.clear();
        assertEquals(0, lruCache.size(), "Invoke clear() on a newly created (empty) cache and verify that size() remains 0.");
    }

    /**
     * Zero‑capacity cache never stores entries, size always 0.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeWithZeroCapacityCache(LRUCache.StrategyType strategy) {
        setUp(strategy, 0);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        assertEquals(0, lruCache.size(), "Construct an LRUCache with capacity 0. Any put() attempts must be ignored, so size() should always return 0 regardless of how many put() calls are made.");
    }

    /**
     * Threaded strategy should correctly report size after sequential puts.
     */
    @Test
    void sizeWithThreadedStrategyConsistency() {
        setUp(LRUCache.StrategyType.THREADED, 5);
        for (int i = 1; i <= 5; i++) {
            lruCache.put(i, "V" + i);
        }
        assertEquals(5, lruCache.size(), "Create an LRUCache using the ThreadedLRUCacheStrategy with capacity 5, sequentially put five distinct entries, then query size(). The thread‑safe strategy must still report the correct count (5).");
    }

    /**
     * Force the private 'strategy' field to null and expect NullPointerException.
     */
    @Test
    void sizeWhenStrategyIsNullThrowsNPE() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING, 5);
        // Null out the strategy via reflection
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(lruCache, null);
        assertThrows(NullPointerException.class, () -> lruCache.size(), "Force the private 'strategy' field to null and invoke size(); the method should throw NullPointerException.");
    }

    /**
     * Strategy's size() throws UnsupportedOperationException – should propagate.
     */
    @Test
    void sizeWhenStrategyThrowsUnsupportedOperationException() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING, 5);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.size()).thenThrow(new UnsupportedOperationException("not supported"));
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(lruCache, mockStrategy);
        assertThrows(UnsupportedOperationException.class, () -> lruCache.size(), "Inject a mock Map as the strategy whose size() method is stubbed to throw UnsupportedOperationException. Calling LRUCache.size() must propagate this exception.");
    }

    /**
     * Strategy's size() throws IllegalStateException – should propagate.
     */
    @Test
    void sizeWhenStrategyThrowsIllegalStateException() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING, 5);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.size()).thenThrow(new IllegalStateException("corrupted state"));
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(lruCache, mockStrategy);
        assertThrows(IllegalStateException.class, () -> lruCache.size(), "Provide a custom Map implementation as the strategy where size() deliberately throws IllegalStateException. LRUCache.size() should not catch it and the exception should surface.");
    }
}
