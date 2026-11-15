package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.stream.IntStream;

class ThreadedLRUCacheStrategy_size_Test {

    /**
     * The cache instance used in each test.
     */
    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Common configuration for the cache – a small capacity is enough for size tests.
     */
    private static final int CAPACITY = 5;

    private static final int CLEANUP_DELAY_MILLIS = 50;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for every test to guarantee isolation.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that lingering scheduled tasks do not interfere
        // with subsequent tests.
        if (cache != null) {
            cache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances.
        ThreadedLRUCacheStrategy.shutdown();
    }

    @Test
    void size_isZeroWhenCacheIsNew() {
        assertEquals(0, cache.size(), "A newly created cache should have size 0");
    }

    @Test
    void size_reflectsNumberOfPutEntries() {
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        assertEquals(3, cache.size(), "Size should match the number of distinct keys inserted");
    }

    @Test
    void size_decreasesAfterRemove() {
        cache.put("a", 10);
        cache.put("b", 20);
        assertEquals(2, cache.size(), "Pre‑condition: size should be 2 after two puts");
        cache.remove("a");
        assertEquals(1, cache.size(), "Size should decrement after removing a key");
    }

    @Test
    void size_isZeroAfterClear() {
        cache.put("x", 100);
        cache.put("y", 200);
        assertEquals(2, cache.size(), "Pre‑condition: size should be 2 before clear");
        cache.clear();
        assertEquals(0, cache.size(), "Size should be 0 after clear");
    }

    @Test
    void size_countsNullKeyAndNullValue() {
        // The implementation supports null keys and values.
        cache.put(null, null);
        cache.put("normal", 42);
        assertEquals(2, cache.size(), "Size should include entries with null key/value");
    }

    @Test
    void size_doesNotCountDuplicateKeys() {
        cache.put("dup", 1);
        // replaces the previous entry
        cache.put("dup", 2);
        assertEquals(1, cache.size(), "Putting a duplicate key should not increase size");
    }

    @ParameterizedTest(name = "[{index}] Insert {0} distinct entries → expected size {0}")
    @CsvSource({ "1", "5" })
    void size_matchesNumberOfDistinctPuts(int entryCount) {
        // Insert `entryCount` distinct entries.
        IntStream.rangeClosed(1, entryCount).forEach(i -> cache.put("key" + i, i));
        assertEquals(entryCount, cache.size(), "Size should equal the number of distinct entries inserted");
    }

    @ParameterizedTest(name = "[{index}] Start with {0} entries, remove {1} → expected size {2}")
    @CsvSource({ "5, 2, 3" })
    void size_afterRemovals(int initial, int toRemove, int expected) {
        // Populate the cache.
        IntStream.rangeClosed(1, initial).forEach(i -> cache.put("k" + i, i));
        // Remove the first `toRemove` keys.
        IntStream.rangeClosed(1, toRemove).forEach(i -> cache.remove("k" + i));
        assertEquals(expected, cache.size(), "Size after removals should match expected value");
    }

    @Test
    void size_countsEntriesAddedConcurrently() throws Exception {
        // Use a larger capacity so that no eviction interferes with the test.
        ThreadedLRUCacheStrategy<String, Integer> concurrentCache = new ThreadedLRUCacheStrategy<>(200, CLEANUP_DELAY_MILLIS);
        final int THREAD_COUNT = 10;
        final int ENTRIES_PER_THREAD = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        // Each thread will insert ENTRIES_PER_THREAD unique keys.
        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // ensure all threads start together
                    startLatch.await();
                    for (int i = 0; i < ENTRIES_PER_THREAD; i++) {
                        String key = "t" + threadId + "_k" + i;
                        concurrentCache.put(key, threadId * ENTRIES_PER_THREAD + i);
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Release all workers and wait for completion.
        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All worker threads should finish within the timeout");
        // Verify total size.
        assertEquals(THREAD_COUNT * ENTRIES_PER_THREAD, concurrentCache.size(), "Size should reflect all concurrently inserted entries");
        // Clean up.
        executor.shutdownNow();
        concurrentCache.clear();
    }

    @Test
    void size_throwsNullPointerExceptionWhenInternalCacheIsNull() throws Exception {
        // Use reflection to set the private `cache` field to null.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        // The size() method dereferences the map without a null‑check, so NPE is expected.
        assertThrows(NullPointerException.class, cache::size, "Calling size() when internal cache is null should throw NullPointerException");
    }
}
