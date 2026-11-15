package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIf;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

class TTLCache_size_Test {

    private TTLCache<Integer, String> ttlCache;

    @BeforeEach
    void setupBeforeEach() {
        // Default cache used for most tests: 10‑second TTL, no size limit.
        ttlCache = new TTLCache<>(10_000L, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that each test starts fresh.
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background purge thread used by TTLCache.
        TTLCache.shutdown();
    }

    @Test
    void sizeReflectsNumberOfEntries() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        assertEquals(3, ttlCache.size(), "size() should return the number of stored entries");
    }

    @Test
    void sizeDecreasesAfterRemoval() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        ttlCache.remove(2);
        assertEquals(2, ttlCache.size(), "size() should decrease after an entry is removed");
        assertFalse(ttlCache.containsKey(2), "Removed key should no longer be present");
    }

    @Test
    void sizeBecomesZeroAfterClear() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.clear();
        assertEquals(0, ttlCache.size(), "size() should be zero after clear()");
        assertTrue(ttlCache.isEmpty(), "Cache should report empty after clear()");
    }

    @EnabledIf("com.cedarsoftware.util.TestUtil#isReleaseMode")
    @Test
    void sizeReflectsExpiration() throws InterruptedException {
        // Very short TTL (200 ms) and a quick cleanup interval (100 ms)
        ttlCache = new TTLCache<>(200L, -1, 100L);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        // Verify entries are present initially
        assertEquals(3, ttlCache.size());
        // Wait long enough for the entries to expire
        Thread.sleep(350L);
        // After expiration the size must be zero
        assertEquals(0, ttlCache.size(), "size() should be zero after all entries have expired");
    }

    @Test
    void sizeRespectsLRUEvictionLimit() {
        // Cache with a max size of 3 (LRU enabled)
        ttlCache = new TTLCache<>(10_000L, 3);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        // Should evict the least‑recently used entry (key 1)
        ttlCache.put(4, "D");
        assertEquals(3, ttlCache.size(), "size() should never exceed the configured maxSize");
        assertNull(ttlCache.get(1), "Key 1 should have been evicted by LRU policy");
        assertNotNull(ttlCache.get(2));
        assertNotNull(ttlCache.get(3));
        assertNotNull(ttlCache.get(4));
    }

    @Test
    void sizeIsZeroForNewCache() {
        assertEquals(0, ttlCache.size(), "A newly created cache should have size zero");
        assertTrue(ttlCache.isEmpty(), "Cache should report empty when newly created");
    }

    @Test
    void sizeAfterSingleInsert() {
        ttlCache.put(1, "A");
        assertEquals(1, ttlCache.size(), "After inserting one entry size() should be 1");
    }

    @Test
    void sizeAfterFiveDistinctInserts() {
        for (int i = 1; i <= 5; i++) {
            ttlCache.put(i, "V" + i);
        }
        assertEquals(5, ttlCache.size(), "After inserting five distinct entries size() must be 5");
    }

    @Test
    void sizeReflectsRemoval() {
        for (int i = 1; i <= 5; i++) {
            ttlCache.put(i, "V" + i);
        }
        ttlCache.remove(3);
        assertEquals(4, ttlCache.size(), "After removing one entry size() should reflect the removal (4)");
        assertFalse(ttlCache.containsKey(3), "Removed key must no longer be present");
    }

    @Test
    void sizeUnchangedWhenRemovingNonExistentKeyFromEmptyCache() {
        // no entry present
        ttlCache.remove(99);
        assertEquals(0, ttlCache.size(), "Removing a non‑existent key from an empty cache must leave size at 0");
    }

    @Test
    void sizeAfterPutAllWithNewEntries() {
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "A");
        source.put(2, "B");
        source.put(3, "C");
        ttlCache.putAll(source);
        assertEquals(3, ttlCache.size(), "putAll with three distinct entries should result in size 3");
    }

    @Test
    void sizeAfterPutAllWithOverlappingKeys() {
        // initial cache contains keys 1 and 2
        ttlCache.put(1, "old1");
        ttlCache.put(2, "old2");
        // map to putAll contains key 2 (new value) and a brand‑new key 3
        Map<Integer, String> source = new HashMap<>();
        source.put(2, "new2");
        source.put(3, "new3");
        ttlCache.putAll(source);
        assertEquals(3, ttlCache.size(), "After putAll with overlapping key, size should be the count of unique keys (3)");
        assertEquals("new2", ttlCache.get(2), "Existing key should be replaced with new value");
        assertEquals("new3", ttlCache.get(3), "New key should be added");
    }

    @EnabledIf("com.cedarsoftware.util.TestUtil#isReleaseMode")
    @Test
    void sizeAfterEntryExpiration() throws InterruptedException {
        // very short TTL (100 ms) and a purge interval (50 ms)
        ttlCache = new TTLCache<>(100L, -1, 50L);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        // sanity check: entries are present immediately
        assertEquals(2, ttlCache.size(), "Initially size should be 2");
        // wait long enough for expiration
        Thread.sleep(200L);
        // after expiration the cache should be empty
        assertEquals(0, ttlCache.size(), "size() must be zero after entries have expired");
    }

    @Test
    void sizeAfterConcurrentPuts() throws InterruptedException, ExecutionException {
        final int threadCount = 4;
        final int keysPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // ensure all threads start together
                    startLatch.await();
                    for (int i = 1; i <= keysPerThread; i++) {
                        // unique key across threads
                        int key = threadId * keysPerThread + i;
                        ttlCache.put(key, "V" + key);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // trigger the concurrent work
        startLatch.countDown();
        // wait for all threads to finish
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All threads should finish within timeout");
        executor.shutdownNow();
        assertEquals(threadCount * keysPerThread, ttlCache.size(), "After concurrent insertion of unique keys size() should equal total unique keys");
    }

    @Test
    void sizeAfterReaddingSameKey() {
        ttlCache.put(1, "A");
        assertEquals(1, ttlCache.size(), "Size should be 1 after first insertion");
        // replace value, key already exists
        ttlCache.put(1, "B");
        assertEquals(1, ttlCache.size(), "Re‑adding the same key must not increase size");
        assertEquals("B", ttlCache.get(1), "Value should be updated to the new one");
    }

    @Test
    void sizeAfterRemovingNonExistentKeyFromNonEmptyCache() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        assertEquals(2, ttlCache.size(), "Pre‑condition: cache should contain 2 entries");
        // key 99 does not exist
        ttlCache.remove(99);
        assertEquals(2, ttlCache.size(), "Size must remain unchanged after removing a non‑existent key");
    }
}
