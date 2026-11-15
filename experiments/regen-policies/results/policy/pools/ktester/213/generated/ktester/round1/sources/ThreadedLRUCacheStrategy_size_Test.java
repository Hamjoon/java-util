package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_size_Test {

    /**
     * Cache instance used in each test.
     */
    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Capacity used for the cache in the tests.
     */
    private static final int CAPACITY = 3;

    /**
     * Delay (in ms) before the background purge task runs.
     */
    private static final int CLEANUP_DELAY_MILLIS = 10;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for every test case.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that background tasks do not interfere
        // with subsequent tests.
        cache.clear();
    }

    @Test
    void size_isZeroOnNewCache() {
        assertEquals(0, cache.size(), "A newly created cache should report size 0.");
    }

    @Test
    void size_reflectsNumberOfPutEntries() {
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        assertEquals(3, cache.size(), "Cache size should match the number of entries inserted.");
    }

    @Test
    void size_decreasesAfterRemove() {
        cache.put("a", 10);
        cache.put("b", 20);
        assertEquals(2, cache.size(), "Pre‑condition: size should be 2 after two puts.");
        cache.remove("a");
        assertEquals(1, cache.size(), "Size should decrement after removing an existing key.");
        // Removing a non‑existent key must not change the size.
        cache.remove("nonexistent");
        assertEquals(1, cache.size(), "Size should stay unchanged when removing a missing key.");
    }

    @Test
    void size_respectsCapacityAfterBackgroundPurge() throws InterruptedException {
        // Insert more entries than the configured capacity.
        cache.put("k1", 1);
        cache.put("k2", 2);
        cache.put("k3", 3);
        cache.put("k4", 4);
        cache.put("k5", 5);
        // At this point the cache may temporarily hold more than CAPACITY entries.
        // Wait for the scheduled purge task to run (cleanupDelayMillis = 10 ms).
        TimeUnit.MILLISECONDS.sleep(CLEANUP_DELAY_MILLIS * 5);
        // After the purge the size must be <= capacity.
        int actualSize = cache.size();
        assertTrue(actualSize <= CAPACITY, "After the background purge the cache size (" + actualSize + ") should not exceed the configured capacity (" + CAPACITY + ").");
    }

    @Test
    void size_isZeroAfterClear() {
        cache.put("x", 100);
        cache.put("y", 200);
        assertEquals(2, cache.size(), "Pre‑condition: size should be 2 before clear.");
        cache.clear();
        assertEquals(0, cache.size(), "Calling clear() must reset the cache size to 0.");
    }

    @Test
    void size_countsUniqueKeysWhenDuplicatesAreInserted() {
        // Insert three distinct entries.
        cache.put("k1", 1);
        cache.put("k2", 2);
        cache.put("k3", 3);
        // Replace the value for an existing key.
        cache.put("k2", 22);
        // Add two new keys.
        cache.put("k4", 4);
        cache.put("k5", 5);
        // Unique keys = 5 (k1‑k5). The duplicate put must not increase the count.
        assertEquals(5, cache.size(), "Duplicate key insertion should replace the value without changing size.");
    }

    @Test
    void size_unchangedWhenPutAllWithEmptyMap() {
        cache.put("single", 1);
        assertEquals(1, cache.size(), "Pre‑condition: size should be 1 before putAll.");
        cache.putAll(Collections.emptyMap());
        assertEquals(1, cache.size(), "Putting an empty map must not modify the cache size.");
    }

    @Test
    void size_countsCorrectlyWhenPutAllHasOverlappingKeys() {
        // Initial three unique entries.
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        // Map with two overlapping keys (different values) and one new key.
        Map<String, Integer> other = new HashMap<>();
        // overlap
        other.put("b", 20);
        // overlap
        other.put("c", 30);
        // new
        other.put("d", 40);
        cache.putAll(other);
        // Expected unique keys: a,b,c,d => 4
        assertEquals(4, cache.size(), "putAll should replace overlapping keys without increasing count and add new keys.");
    }

    @Test
    void size_unchangedWhenPuttingNullKey() {
        assertThrows(NullPointerException.class, () -> cache.put(null, 123), "Putting a null key must throw NullPointerException.");
        assertEquals(0, cache.size(), "Cache size must remain 0 after a failed null‑key insertion.");
    }

    @Test
    void size_reflectsAllEntriesAfterConcurrentPuts() throws Exception {
        final int threadCount = 10;
        final int keysPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // ensure all threads start together
                    startLatch.await();
                    for (int i = 0; i < keysPerThread; i++) {
                        String key = "t" + threadId + "_k" + i;
                        cache.put(key, i);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Release all threads simultaneously.
        startLatch.countDown();
        // Wait for all threads to finish.
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All worker threads should finish within the timeout.");
        executor.shutdownNow();
        int expected = threadCount * keysPerThread;
        assertEquals(expected, cache.size(), "After concurrent puts the size must equal the total number of unique keys.");
    }
}
