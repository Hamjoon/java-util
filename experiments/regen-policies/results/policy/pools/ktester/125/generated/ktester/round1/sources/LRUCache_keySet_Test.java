package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.cache.LockingLRUCacheStrategy;
import java.lang.reflect.Field;
import java.util.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LRUCache_keySet_Test {

    @AfterAll
    static void teardownAfterAll() {
        // Ensure the thread pool used by ThreadedLRUCacheStrategy is cleaned up.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /* --------------------------------------------------------------------- *
     *  Parameter source – both supported strategies are exercised.            *
     * --------------------------------------------------------------------- */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /* --------------------------------------------------------------------- *
     *  1. keySet contains exactly the keys that have been put into the cache *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetReflectsCurrentKeys(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        Set<Integer> keys = cache.keySet();
        assertEquals(3, keys.size(), "keySet size should match number of entries");
        assertTrue(keys.containsAll(Arrays.asList(1, 2, 3)));
    }

    /* --------------------------------------------------------------------- *
     *  2. keySet is a live view – after a new entry is added, the set sees it *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetUpdatesWhenCacheIsModified(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("x", 10);
        cache.put("y", 20);
        Set<String> keys = cache.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("x"));
        assertTrue(keys.contains("y"));
        // Add a new entry after the set was obtained
        cache.put("z", 30);
        assertEquals(3, keys.size(), "keySet should reflect the newly added key");
        assertTrue(keys.contains("z"));
    }

    /* --------------------------------------------------------------------- *
     *  3. Removing an entry from the cache removes it from the keySet view    *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetReflectsRemoval(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(10, "ten");
        cache.put(20, "twenty");
        Set<Integer> keys = cache.keySet();
        assertTrue(keys.contains(10));
        assertTrue(keys.contains(20));
        cache.remove(10);
        assertFalse(keys.contains(10), "Removed key must disappear from keySet");
        assertTrue(keys.contains(20));
        assertEquals(1, keys.size());
    }

    /* --------------------------------------------------------------------- *
     *  4. The iterator returned by keySet supports remove()                  *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetIteratorRemoveWorks(LRUCache.StrategyType strategy) {
        LRUCache<String, String> cache = new LRUCache<>(5, strategy);
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");
        Set<String> keys = cache.keySet();
        Iterator<String> it = keys.iterator();
        // Remove the first element via iterator
        assertTrue(it.hasNext());
        String first = it.next();
        it.remove();
        // The underlying cache must no longer contain that key
        assertFalse(cache.containsKey(first));
        assertEquals(2, cache.size());
        assertEquals(2, keys.size());
        assertFalse(keys.contains(first));
    }

    /* --------------------------------------------------------------------- *
     *  5. Empty cache returns an empty Set                                   *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetOnEmptyCacheIsEmpty(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        Set<String> keys = cache.keySet();
        assertTrue(keys.isEmpty(), "keySet of a newly created cache must be empty");
        assertEquals(0, keys.size());
    }

    /* --------------------------------------------------------------------- *
     *  6. After clear() and a new put, keySet reflects only the new key      *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetAfterClearAndAdd(LRUCache.StrategyType strategy) {
        LRUCache<String, String> cache = new LRUCache<>(5, strategy);
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        cache.clear();
        // after clear the set must be empty
        assertTrue(cache.keySet().isEmpty());
        cache.put("k1", "v1");
        Set<String> keys = cache.keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.contains("k1"));
    }

    /* --------------------------------------------------------------------- *
     *  7. Removing via the Set's iterator also removes the mapping from cache *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetIteratorRemoveAffectsCache(LRUCache.StrategyType strategy) {
        LRUCache<String, String> cache = new LRUCache<>(5, strategy);
        cache.put("removable", "value");
        cache.put("stay", "value2");
        Set<String> keys = cache.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            if ("removable".equals(it.next())) {
                it.remove();
                break;
            }
        }
        assertNull(cache.get("removable"));
        assertEquals(1, cache.size());
        assertFalse(keys.contains("removable"));
        assertTrue(keys.contains("stay"));
    }

    /* --------------------------------------------------------------------- *
     *  8. The Set view does not support add operations                         *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetAddIsUnsupported(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        Set<String> keys = cache.keySet();
        assertThrows(UnsupportedOperationException.class, () -> keys.add("newKey"));
    }

    /* --------------------------------------------------------------------- *
     *  9. The Set view does not support bulk add (addAll)                     *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetAddAllIsUnsupported(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        Set<String> keys = cache.keySet();
        assertThrows(UnsupportedOperationException.class, () -> keys.addAll(Arrays.asList("x", "y")));
    }

    /* --------------------------------------------------------------------- *
     * 10. The Set view does not support clear()                               *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetClearIsUnsupported(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("a", 1);
        Set<String> keys = cache.keySet();
        assertThrows(UnsupportedOperationException.class, keys::clear);
    }

    /* --------------------------------------------------------------------- *
     * 11. removeAll(null) and retainAll(null) must throw NPE                  *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetRemoveAllNullThrowsNPE(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        Set<Integer> keys = cache.keySet();
        assertThrows(NullPointerException.class, () -> keys.removeAll(null));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void keySetRetainAllNullThrowsNPE(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        Set<Integer> keys = cache.keySet();
        assertThrows(NullPointerException.class, () -> keys.retainAll(null));
    }

    /* --------------------------------------------------------------------- *
     * 12. toArray(null) must throw NullPointerException                        *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetToArrayNullThrowsNPE(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("a", 1);
        Set<String> keys = cache.keySet();
        assertThrows(NullPointerException.class, () -> keys.toArray(null));
    }

    /* --------------------------------------------------------------------- *
     * 13. Successive calls return the same Set instance (reference equality) *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetReturnsSameInstanceOnRepeatedCalls(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("one", 1);
        Set<String> first = cache.keySet();
        Set<String> second = cache.keySet();
        assertSame(first, second, "keySet() should return the same Set instance");
    }

    /* --------------------------------------------------------------------- *
     * 14. Verify that the underlying strategy class matches the requested one *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void underlyingStrategyMatchesRequested(LRUCache.StrategyType strategy) throws Exception {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        // Access the private 'strategy' field via reflection
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        Object underlying = f.get(cache);
        if (strategy == LRUCache.StrategyType.LOCKING) {
            assertTrue(underlying instanceof LockingLRUCacheStrategy, "Expected LockingLRUCacheStrategy for LOCKING strategy");
        } else {
            assertTrue(underlying instanceof ThreadedLRUCacheStrategy, "Expected ThreadedLRUCacheStrategy for THREADED strategy");
        }
    }

    /* --------------------------------------------------------------------- *
     * 15. iterator.remove() without a preceding next() must throw IllegalStateException *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void iteratorRemoveWithoutNextThrows(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("a", 1);
        cache.put("b", 2);
        Iterator<String> it = cache.keySet().iterator();
        assertThrows(IllegalStateException.class, it::remove);
    }

    /* --------------------------------------------------------------------- *
     * 16. iterator.remove() called twice without an intervening next() must throw IllegalStateException *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void iteratorRemoveTwiceThrows(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("a", 1);
        cache.put("b", 2);
        Iterator<String> it = cache.keySet().iterator();
        assertTrue(it.hasNext());
        // first element
        it.next();
        // first removal succeeds
        it.remove();
        assertThrows(IllegalStateException.class, it::remove, "Second remove() without next() must fail");
    }

    /* --------------------------------------------------------------------- *
     * 17. After shutdown, any operation (including keySet) should fail with IllegalStateException *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetAfterShutdownThrows(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("a", 1);
        cache.put("b", 2);
        // put the cache into a non‑operational state
        cache.shutdown();
        assertThrows(IllegalStateException.class, cache::keySet, "keySet() after shutdown must throw IllegalStateException");
    }

    /* --------------------------------------------------------------------- *
     * 18. keySet().contains() basic true/false checks                         *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetContainsWorks(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("alpha", 1);
        cache.put("beta", 2);
        Set<String> keys = cache.keySet();
        assertTrue(keys.contains("alpha"));
        assertFalse(keys.contains("gamma"));
    }
}
