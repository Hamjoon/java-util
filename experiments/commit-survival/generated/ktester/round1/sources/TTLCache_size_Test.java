package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;

class TTLCache_size_Test {

    private TTLCache<Integer, String> ttlCache;

    /**
     * Creates a fresh cache instance before each test.
     * <p>
     * The cache is configured with a TTL of 500 ms, no maximum size, and a cleanup interval of 100 ms.
     * This configuration gives the tests deterministic timing while keeping the background purge thread alive.
     * </p>
     */
    @BeforeEach
    void setupBeforeEach() {
        ttlCache = new TTLCache<>(500L, -1, 100L);
    }

    /**
     * Clears the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            ttlCache.clear();
            ttlCache = null;
        }
    }

    /**
     * Shuts down the shared background executor used by all {@code TTLCache} instances.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    @Test
    void sizeIsZeroWhenCacheIsEmpty() {
        assertEquals(0, ttlCache.size(), "A newly created cache must report size 0");
        assertTrue(ttlCache.isEmpty(), "Cache should also report being empty");
    }

    @Test
    void sizeReflectsNumberOfPutEntries() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        assertEquals(3, ttlCache.size(), "Size should equal the number of inserted entries");
        assertFalse(ttlCache.isEmpty(), "Cache should not be empty after puts");
    }

    @Test
    void sizeBecomesZeroAfterAllEntriesExpire() throws InterruptedException {
        ttlCache.put(10, "X");
        ttlCache.put(20, "Y");
        // Verify they are present before expiration
        assertEquals(2, ttlCache.size(), "Pre‑condition: two entries present");
        // Wait longer than the TTL (500 ms) plus a small cushion for the purge task
        TimeUnit.MILLISECONDS.sleep(800);
        // After expiration the cache should report size 0
        assertEquals(0, ttlCache.size(), "All entries should have expired");
        assertTrue(ttlCache.isEmpty(), "Cache should be empty after expiration");
    }

    @Test
    void sizeDecreasesWhenEntriesAreRemoved() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        assertEquals(3, ttlCache.size());
        ttlCache.remove(2);
        assertEquals(2, ttlCache.size(), "Size should decrease after a removal");
        ttlCache.remove(1);
        ttlCache.remove(3);
        assertEquals(0, ttlCache.size(), "Size should be zero after removing all entries");
        assertTrue(ttlCache.isEmpty());
    }

    @Test
    void sizeAfterClearIsZero() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        assertEquals(2, ttlCache.size(), "Pre‑condition: two entries present");
        ttlCache.clear();
        assertEquals(0, ttlCache.size(), "Clear should reset size to zero");
        assertTrue(ttlCache.isEmpty(), "Cache should be empty after clear");
    }

    @Test
    void sizeDecreasesWhenAnEntryIsRemoved() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        assertEquals(3, ttlCache.size(), "Pre‑condition: three entries present");
        ttlCache.remove(2);
        assertEquals(2, ttlCache.size(), "Size should decrease after a removal");
    }

    @Test
    void sizeAfterPutAllWithFiveEntries() {
        // Build a map containing five distinct entries.
        Map<Integer, String> source = new HashMap<>();
        source.put(10, "Ten");
        source.put(20, "Twenty");
        source.put(30, "Thirty");
        source.put(40, "Forty");
        source.put(50, "Fifty");
        ttlCache.putAll(source);
        assertEquals(5, ttlCache.size(), "putAll with five entries must result in size 5");
        assertFalse(ttlCache.isEmpty(), "Cache should not be empty after putAll");
    }

    @Test
    void sizeRemainsZeroWhenPutAllWithEmptyMap() {
        Map<Integer, String> emptyMap = new HashMap<>();
        ttlCache.putAll(emptyMap);
        assertEquals(0, ttlCache.size(), "putAll with an empty map must leave size at 0");
        assertTrue(ttlCache.isEmpty(), "Cache should still be empty");
    }
}
