package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_size_Test {

    /**
     * Cache used for the majority of the tests.
     */
    private TTLCache<Integer, String> ttlCache;

    /**
     * Cache used for expiration‑specific tests where we need a short cleanup interval.
     */
    private TTLCache<Integer, String> ttlCacheWithFastCleanup;

    /* -------------------------------------------------------------
     * Helper class to pass the cache instance from lambda to test.
     * ------------------------------------------------------------- */
    private static class TestContext {

        static TTLCache<Integer, String> currentCache;
    }

    @BeforeEach
    void setupBeforeEach() {
        // A “normal” cache with a long TTL so that it does not expire during the test.
        ttlCache = new TTLCache<>(10_000L, -1);
        // A cache that expires quickly; cleanup interval is also short.
        ttlCacheWithFastCleanup = new TTLCache<>(200L, -1, 50L);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the caches to avoid side‑effects between tests.
        if (ttlCache != null) {
            ttlCache.clear();
        }
        if (ttlCacheWithFastCleanup != null) {
            ttlCacheWithFastCleanup.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background thread used by TTLCache.
        TTLCache.shutdown();
    }

    /**
     * Data holder for basic‑behavior scenarios.
     */
    private static Stream<Arguments> basicBehaviorProvider() {
        return Stream.of(// newly created cache → size 0
        Arguments.of(// no actions
        (Runnable) () -> {
        }, 0, "A newly created TTLCache should report size 0 because no entries have been added."), // three puts → size 3
        Arguments.of((Runnable) () -> {
            TTLCache<Integer, String> c = new TTLCache<>(10_000L, -1);
            c.put(1, "A");
            c.put(2, "B");
            c.put(3, "C");
            TestContext.currentCache = c;
        }, 3, "After inserting three distinct key‑value pairs with put(...), size() must return 3."), // remove one of three → size 2
        Arguments.of((Runnable) () -> {
            TTLCache<Integer, String> c = new TTLCache<>(10_000L, -1);
            c.put(1, "A");
            c.put(2, "B");
            c.put(3, "C");
            c.remove(2);
            TestContext.currentCache = c;
        }, 2, "After removing one of the previously added keys, size() should decrease to reflect the remaining entries."), // clear after two puts → size 0
        Arguments.of((Runnable) () -> {
            TTLCache<Integer, String> c = new TTLCache<>(10_000L, -1);
            c.put(1, "A");
            c.put(2, "B");
            c.clear();
            TestContext.currentCache = c;
        }, 0, "Calling clear() removes all entries; size() must return 0 again."), // putAll with five entries → size 5
        Arguments.of((Runnable) () -> {
            TTLCache<Integer, String> c = new TTLCache<>(10_000L, -1);
            Map<Integer, String> m = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                m.put(i, "V" + i);
            }
            c.putAll(m);
            TestContext.currentCache = c;
        }, 5, "Using putAll() to add a map containing five entries should make size() report 5."));
    }

    /**
     * Data holder for duplicate‑key handling scenarios.
     */
    private static Stream<Arguments> duplicateKeyProvider() {
        return Stream.of(// same key put twice → size 1
        Arguments.of((Runnable) () -> {
            TTLCache<Integer, String> c = new TTLCache<>(10_000L, -1);
            c.put(1, "A");
            // replacement
            c.put(1, "B");
            TestContext.currentCache = c;
        }, 1, "Insert a key-value pair, then insert another value with the same key. The second put replaces the existing entry, so size() should remain 1."), // two distinct keys, replace one → size 2
        Arguments.of((Runnable) () -> {
            TTLCache<Integer, String> c = new TTLCache<>(10_000L, -1);
            c.put(1, "A");
            c.put(2, "B");
            // replace key 1
            c.put(1, "C");
            TestContext.currentCache = c;
        }, 2, "Insert two distinct keys, then replace the value of one of them with a new put using the same key. Size should stay at 2."));
    }

    /**
     * Data holder for removal of non‑existing keys.
     */
    private static Stream<Arguments> removeNonExistingProvider() {
        return Stream.of(// three entries, remove unknown key → size stays 3
        Arguments.of((Runnable) () -> {
            TTLCache<Integer, String> c = new TTLCache<>(10_000L, -1);
            c.put(1, "A");
            c.put(2, "B");
            c.put(3, "C");
            // non‑existing
            c.remove(99);
            TestContext.currentCache = c;
        }, 3, "After adding three distinct entries, attempt to remove a key that was never added. The size must stay 3 because nothing was removed."), // empty cache, remove any key → size stays 0
        Arguments.of((Runnable) () -> {
            TTLCache<Integer, String> c = new TTLCache<>(10_000L, -1);
            // nothing to remove
            c.remove(1);
            TestContext.currentCache = c;
        }, 0, "When the cache is empty, calling remove() with any key (e.g., null or a random object) must leave size() at 0."));
    }

    /**
     * Data holder for putAll with overlapping keys.
     */
    private static Stream<Arguments> putAllOverlapProvider() {
        return Stream.of(// start with 4 entries, putAll with 2 new + 2 existing → size 6
        Arguments.of((Runnable) () -> {
            TTLCache<Integer, String> c = new TTLCache<>(10_000L, -1);
            for (int i = 1; i <= 4; i++) {
                c.put(i, "V" + i);
            }
            Map<Integer, String> m = new HashMap<>();
            // new
            m.put(5, "V5");
            // new
            m.put(6, "V6");
            // existing, replacement
            m.put(2, "V2new");
            // existing, replacement
            m.put(3, "V3new");
            c.putAll(m);
            TestContext.currentCache = c;
        }, 6, "Start with a cache containing four distinct entries. Call putAll() with a map that has two new keys and two keys that already exist (different values). After the operation size() should be 6 because the two new keys increase the count while the existing keys are only replaced."), // putAll only with existing keys → size unchanged (4)
        Arguments.of((Runnable) () -> {
            TTLCache<Integer, String> c = new TTLCache<>(10_000L, -1);
            for (int i = 1; i <= 4; i++) {
                c.put(i, "V" + i);
            }
            Map<Integer, String> m = new HashMap<>();
            m.put(1, "V1new");
            m.put(2, "V2new");
            m.put(3, "V3new");
            m.put(4, "V4new");
            c.putAll(m);
            TestContext.currentCache = c;
        }, 4, "If putAll() is invoked with a map that contains only keys already present in the cache (no new keys), size() must remain unchanged."));
    }

    @Test
    void sizeIsZeroWhenCacheIsNew() {
        assertEquals(0, ttlCache.size(), "A newly created cache should report size 0.");
    }

    @Test
    void sizeReflectsNumberOfPutEntries() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        assertEquals(3, ttlCache.size(), "Size should equal the number of entries that have been put.");
    }

    @Test
    void sizeDecreasesWhenEntriesAreRemoved() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        assertEquals(3, ttlCache.size());
        ttlCache.remove(2);
        assertEquals(2, ttlCache.size(), "Size should decrement after a removal.");
        ttlCache.remove(1);
        ttlCache.remove(3);
        assertEquals(0, ttlCache.size(), "Size should be zero after all entries are removed.");
    }

    @Test
    void sizeBecomesZeroAfterTTLExpiration() throws InterruptedException {
        // Insert three entries; they will expire after ~200 ms.
        ttlCacheWithFastCleanup.put(1, "A");
        ttlCacheWithFastCleanup.put(2, "B");
        ttlCacheWithFastCleanup.put(3, "C");
        assertEquals(3, ttlCacheWithFastCleanup.size(), "Size should be 3 before expiration.");
        // Wait longer than the TTL so that the background purge task runs.
        Thread.sleep(350L);
        // The purge task runs asynchronously; give it a tiny extra window.
        Thread.sleep(50L);
        assertEquals(0, ttlCacheWithFastCleanup.size(), "Size should be 0 after entries have expired.");
    }

    @Test
    void sizeReflectsLRUEvictionWhenMaxSizeIsReached() {
        // Cache with maxSize = 2, TTL long enough that nothing expires.
        TTLCache<Integer, String> lruCache = new TTLCache<>(10_000L, 2);
        // cache: [1]
        lruCache.put(1, "A");
        // cache: [1,2]
        lruCache.put(2, "B");
        assertEquals(2, lruCache.size());
        // Access key 1 to make it most‑recently used.
        // cache order: [2,1]
        lruCache.get(1);
        // Adding a third entry should evict the least‑recently used entry (key 2).
        lruCache.put(3, "C");
        assertEquals(2, lruCache.size(), "Size should stay at the configured maxSize after LRU eviction.");
        assertNull(lruCache.get(2), "Key 2 should have been evicted by LRU policy.");
        assertEquals("A", lruCache.get(1), "Key 1 should still be present.");
        assertEquals("C", lruCache.get(3), "Key 3 should be present.");
    }

    @Test
    void sizeRemainsConsistentAfterClear() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        assertEquals(2, ttlCache.size());
        ttlCache.clear();
        assertEquals(0, ttlCache.size(), "Size should be zero after clear() is called.");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicBehaviorProvider")
    void sizeBasicBehavior(Runnable setup, int expectedSize, String description) {
        // Reset the shared reference before each scenario.
        TestContext.currentCache = null;
        setup.run();
        TTLCache<Integer, String> cache = TestContext.currentCache != null ? TestContext.currentCache : ttlCache;
        assertEquals(expectedSize, cache.size(), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("duplicateKeyProvider")
    void sizeDuplicateKeyHandling(Runnable setup, int expectedSize, String description) {
        TestContext.currentCache = null;
        setup.run();
        TTLCache<Integer, String> cache = TestContext.currentCache;
        assertEquals(expectedSize, cache.size(), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("removeNonExistingProvider")
    void sizeRemoveNonExistingKey(Runnable setup, int expectedSize, String description) {
        TestContext.currentCache = null;
        setup.run();
        TTLCache<Integer, String> cache = TestContext.currentCache;
        assertEquals(expectedSize, cache.size(), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("putAllOverlapProvider")
    void sizePutAllWithOverlappingKeys(Runnable setup, int expectedSize, String description) {
        TestContext.currentCache = null;
        setup.run();
        TTLCache<Integer, String> cache = TestContext.currentCache;
        assertEquals(expectedSize, cache.size(), description);
    }

    /**
     * Tests for null‑key handling – expecting NullPointerException.
     */
    @Test
    void sizePutNullKeyThrowsNPE() {
        TTLCache<Integer, String> cache = new TTLCache<>(10_000L, -1);
        int before = cache.size();
        assertThrows(NullPointerException.class, () -> cache.put(null, "value"), "Putting a null key should throw NullPointerException.");
        assertEquals(before, cache.size(), "Cache size must remain unchanged after a failed null‑key put.");
    }

    @Test
    void sizePutAllWithNullKeyThrowsNPE() {
        TTLCache<Integer, String> cache = new TTLCache<>(10_000L, -1);
        cache.put(1, "A");
        int before = cache.size();
        Map<Integer, String> mapWithNull = new HashMap<>();
        mapWithNull.put(null, "nullKey");
        mapWithNull.put(2, "B");
        assertThrows(NullPointerException.class, () -> cache.putAll(mapWithNull), "putAll containing a null key should throw NullPointerException.");
        assertEquals(before, cache.size(), "Cache size must remain unchanged after a failed putAll with null key.");
    }

    /**
     * Tests for empty putAll scenarios.
     */
    @Test
    void sizeEmptyPutAllOnNewCache() {
        TTLCache<Integer, String> cache = new TTLCache<>(10_000L, -1);
        // empty map
        cache.putAll(Map.of());
        assertEquals(0, cache.size(), "Calling putAll() with an empty map on a newly created cache should leave size() at 0.");
    }

    @Test
    void sizeEmptyPutAllOnPopulatedCache() {
        TTLCache<Integer, String> cache = new TTLCache<>(10_000L, -1);
        for (int i = 1; i <= 5; i++) {
            cache.put(i, "V" + i);
        }
        int before = cache.size();
        // empty map
        cache.putAll(Map.of());
        assertEquals(before, cache.size(), "After adding five entries, invoking putAll() with an empty map must keep size() at 5.");
    }
}
