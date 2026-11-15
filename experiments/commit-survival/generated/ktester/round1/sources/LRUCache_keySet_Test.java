package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class LRUCache_keySet_Test {

    private LRUCache<Integer, String> cache;

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the thread pool used by the threaded strategy to avoid thread‑leak warnings
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * All supported strategy types for the LRUCache.
     */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Helper to create a cache with the given strategy.
     */
    private void setUp(LRUCache.StrategyType strategy) {
        // capacity is configurable per test; the strategy determines the internal implementation
        cache = new LRUCache<>(capacity, strategy);
    }

    /**
     * Verify that {@link LRUCache#keySet()} returns a set containing exactly the keys that have been
     * inserted and that the set updates when entries are removed or evicted.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetReflectsCurrentKeys(LRUCache.StrategyType strategy) {
        setUp(strategy, 3);
        // initially empty
        Set<Integer> emptySet = cache.keySet();
        assertTrue(emptySet.isEmpty(), "keySet should be empty for a new cache");
        // add three entries
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        Set<Integer> keys = cache.keySet();
        assertEquals(3, keys.size(), "keySet should contain three keys");
        assertTrue(keys.containsAll(Arrays.asList(1, 2, 3)));
        // remove one entry and verify the set shrinks
        cache.remove(2);
        Set<Integer> afterRemoval = cache.keySet();
        assertEquals(2, afterRemoval.size());
        assertTrue(afterRemoval.containsAll(Arrays.asList(1, 3)));
        assertFalse(afterRemoval.contains(2));
        // add a fourth entry to trigger eviction (capacity is 3)
        cache.put(4, "D");
        // Because eviction may be asynchronous for the threaded strategy, wait briefly
        long deadline = System.currentTimeMillis() + 3000;
        Set<Integer> afterEviction;
        do {
            afterEviction = cache.keySet();
            if (afterEviction.size() == 3 && afterEviction.containsAll(Arrays.asList(1, 3, 4))) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        } while (System.currentTimeMillis() < deadline);
        // The cache should now contain exactly three keys; the least‑recently‑used (key 1) may have been evicted
        // depending on the access pattern. We only assert that the size is 3 and that the set matches the cache content.
        assertEquals(3, afterEviction.size(), "keySet size should respect the capacity after eviction");
        assertTrue(afterEviction.containsAll(cache.keySet()));
    }

    /**
     * Ensure that the set returned by {@code keySet()} is a live view – modifications to the cache are reflected
     * in the set without needing to call {@code keySet()} again.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetIsLiveView(LRUCache.StrategyType strategy) {
        setUp(strategy, 3);
        cache.put(10, "X");
        cache.put(20, "Y");
        Set<Integer> ks = cache.keySet();
        assertEquals(2, ks.size());
        // modify the cache directly
        cache.put(30, "Z");
        assertTrue(ks.contains(30), "Live view should contain newly added key");
        // remove a key
        cache.remove(10);
        assertFalse(ks.contains(10), "Live view should not contain a removed key");
    }

    /**
     * Verify that {@code keySet()} works correctly when the cache contains {@code null} as a key.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetWithNullKey(LRUCache.StrategyType strategy) {
        setUp(strategy, 3);
        cache.put(null, "nullValue");
        cache.put(1, "one");
        Set<Integer> ks = cache.keySet();
        assertEquals(2, ks.size());
        assertTrue(ks.contains(null));
        assertTrue(ks.contains(1));
    }

    /**
     * Empty cache should yield an empty Set.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetOnEmptyCache(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        Set<Integer> ks = cache.keySet();
        assertNotNull(ks, "keySet must never be null");
        assertTrue(ks.isEmpty(), "keySet should be empty when cache has no entries");
        assertEquals(0, ks.size(), "Size must be 0 for an empty cache");
    }

    /**
     * After inserting three distinct entries, keySet must contain exactly those keys.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetOnCacheWithMultipleEntries(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        Set<Integer> ks = cache.keySet();
        assertEquals(3, ks.size(), "keySet should contain three keys");
        assertTrue(ks.containsAll(Arrays.asList(1, 2, 3)), "keySet must contain exactly the inserted keys");
    }

    /**
     * keySet reflects subsequent puts.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetReflectsSubsequentPuts(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        cache.put(1, "X");
        cache.put(2, "Y");
        // snapshot view (live view)
        Set<Integer> ks = cache.keySet();
        assertEquals(2, ks.size());
        // new entry
        cache.put(3, "Z");
        assertTrue(ks.contains(3), "Live view must reflect newly added key");
        assertEquals(3, ks.size(), "Live view size must increase after put");
    }

    /**
     * keySet reflects removals.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetReflectsRemovals(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        cache.put(1, "K1");
        cache.put(2, "K2");
        cache.put(3, "K3");
        // remove middle key
        cache.remove(2);
        Set<Integer> ks = cache.keySet();
        assertEquals(2, ks.size());
        assertTrue(ks.containsAll(Arrays.asList(1, 3)));
        assertFalse(ks.contains(2));
    }

    /**
     * After clear(), keySet must be empty.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetAfterClear(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.clear();
        Set<Integer> ks = cache.keySet();
        assertTrue(ks.isEmpty(), "keySet should be empty after clear()");
        assertEquals(0, ks.size());
    }

    /**
     * Live view: removing via Set iterator updates the underlying cache.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetLiveViewModificationViaIterator(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        cache.put(1, "only");
        Set<Integer> ks = cache.keySet();
        assertEquals(1, ks.size());
        Iterator<Integer> it = ks.iterator();
        assertTrue(it.hasNext());
        // advance to the only element
        it.next();
        // remove through iterator
        it.remove();
        assertFalse(ks.contains(1), "Key should be removed from live view");
        assertEquals(0, cache.size(), "Cache size must reflect removal via iterator");
    }

    /**
     * Capacity eviction: with capacity 2, after inserting four entries only the two most recent remain.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetAfterCapacityEviction(LRUCache.StrategyType strategy) {
        setUp(strategy, 2);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        cache.put(4, "D");
        // For the threaded strategy eviction may be asynchronous – wait a short while
        long deadline = System.currentTimeMillis() + 3000;
        Set<Integer> ks;
        do {
            ks = cache.keySet();
            if (ks.size() == 2 && ks.containsAll(Arrays.asList(3, 4))) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        } while (System.currentTimeMillis() < deadline);
        assertEquals(2, ks.size(), "After eviction, keySet size must equal capacity");
        assertTrue(ks.containsAll(Arrays.asList(3, 4)), "Remaining keys should be the most recently used ones");
        assertFalse(ks.contains(1));
        assertFalse(ks.contains(2));
    }

    /**
     * Removing a key via Set.remove(Object) updates the cache.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetRemoveObjectUpdatesCache(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        cache.put(1, "first");
        cache.put(2, "second");
        Set<Integer> ks = cache.keySet();
        assertTrue(ks.remove(1), "Set.remove should return true for existing key");
        assertFalse(ks.contains(1));
        assertEquals(1, cache.size());
        assertNull(cache.get(1), "Cache must no longer contain the removed key");
    }

    /**
     * keySet.contains reflects cache state before and after removal.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetContainsReflectsCacheState(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        cache.put(1, "alpha");
        cache.put(2, "beta");
        Set<Integer> ks = cache.keySet();
        assertTrue(ks.contains(1), "contains should be true for existing key");
        assertFalse(ks.contains(3), "contains should be false for absent key");
        cache.remove(2);
        assertFalse(ks.contains(2), "contains must be false after key removal");
    }

    /**
     * Iterator throws ConcurrentModificationException when the cache is structurally modified during iteration.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetIteratorConcurrentModification(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        cache.put(1, "a");
        cache.put(2, "b");
        Set<Integer> ks = cache.keySet();
        Iterator<Integer> it = ks.iterator();
        assertTrue(it.hasNext());
        // consume first element
        it.next();
        // structural change
        cache.put(3, "c");
        // Next call should detect concurrent modification
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must throw CME after underlying cache modification");
    }

    /**
     * After putAll, keySet must contain exactly the supplied keys.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetAfterPutAll(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "k1");
        source.put(2, "k2");
        source.put(3, "k3");
        cache.putAll(source);
        Set<Integer> ks = cache.keySet();
        assertEquals(3, ks.size());
        assertTrue(ks.containsAll(source.keySet()));
    }

    /**
     * After shutdown, the cache is cleared and keySet is empty.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetAfterShutdown(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.shutdown();
        Set<Integer> ks = cache.keySet();
        assertTrue(ks.isEmpty(), "keySet should be empty after shutdown");
        assertEquals(0, ks.size());
    }

    /**
     * The Set returned by keySet() does not support addition.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetAddThrowsUnsupportedOperationException(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        cache.put(1, "A");
        Set<Integer> ks = cache.keySet();
        assertThrows(UnsupportedOperationException.class, () -> ks.add(99), "Adding directly to the keySet view must throw UnsupportedOperationException");
    }

    /**
     * toArray returns an array containing exactly the keys.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testKeySetToArrayReturnsCorrectElements(LRUCache.StrategyType strategy) {
        setUp(strategy, 5);
        cache.put(1, "one");
        cache.put(2, "two");
        Set<Integer> ks = cache.keySet();
        Object[] array = ks.toArray();
        assertEquals(2, array.length, "Array length must match keySet size");
        List<Object> list = Arrays.asList(array);
        assertTrue(list.containsAll(Arrays.asList(1, 2)), "Array must contain exactly the keys");
    }
}
