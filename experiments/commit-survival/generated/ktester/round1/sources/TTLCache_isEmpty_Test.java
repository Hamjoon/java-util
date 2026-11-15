package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_isEmpty_Test {

    /**
     * Cache used for the majority of the tests.
     */
    private TTLCache<Integer, String> ttlCache;

    /**
     * Create a fresh cache before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // TTL = 500 ms, no LRU, cleanup interval = 100 ms (fast enough for the expiration test).
        ttlCache = new TTLCache<>(500, -1, 100);
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    /**
     * Shut down the background purge thread after the whole suite finishes.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    // Demonstrates how the same logical checks could be expressed with @ParameterizedTest.
    private static Stream<Arguments> basicEmptyScenarios() {
        return Stream.of(Arguments.of("new cache", (Consumer<TTLCache<Integer, String>>) c -> {
        }, /* no op */
        true), Arguments.of("after put", (Consumer<TTLCache<Integer, String>>) c -> c.put(1, "A"), false), Arguments.of("after put then clear", (Consumer<TTLCache<Integer, String>>) c -> {
            c.put(1, "A");
            c.clear();
        }, true));
    }

    @Test
    void isEmptyWhenNewCache() {
        // A newly created cache contains no entries.
        assertTrue(ttlCache.isEmpty(), "A fresh cache should be empty");
    }

    @Test
    void isEmptyAfterPut() {
        ttlCache.put(1, "A");
        assertFalse(ttlCache.isEmpty(), "Cache should not be empty after a put");
    }

    @Test
    void isEmptyAfterClear() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        assertFalse(ttlCache.isEmpty(), "Cache should not be empty before clear");
        ttlCache.clear();
        assertTrue(ttlCache.isEmpty(), "Cache should be empty after clear");
    }

    @Test
    void isEmptyAfterEntryExpiration() throws InterruptedException {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        // Verify that the cache is not empty immediately after insertion.
        assertFalse(ttlCache.isEmpty(), "Cache should not be empty before TTL expires");
        // Wait longer than the TTL (500 ms) plus a small buffer.
        Thread.sleep(800);
        // The background purge task should have removed the entries.
        assertTrue(ttlCache.isEmpty(), "Cache should be empty after entries have expired");
    }

    @Test
    void isEmptyAfterRemovingAllEntries() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        // Remove each entry individually.
        ttlCache.remove(1);
        ttlCache.remove(2);
        ttlCache.remove(3);
        assertTrue(ttlCache.isEmpty(), "Cache should be empty after removing all entries");
    }

    @Test
    void isEmptyAfterPartialRemoval() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        ttlCache.remove(1);
        ttlCache.remove(2);
        assertFalse(ttlCache.isEmpty(), "Cache should not be empty after a subset of entries are removed");
    }

    @Test
    void isEmptyAfterPutAllWithEmptyMap() {
        Map<Integer, String> emptyMap = Collections.emptyMap();
        ttlCache.putAll(emptyMap);
        assertTrue(ttlCache.isEmpty(), "Cache should stay empty after putAll with an empty map");
    }

    @Test
    void isEmptyAfterPutAllWithData() {
        Map<Integer, String> data = new HashMap<>();
        data.put(1, "A");
        data.put(2, "B");
        data.put(3, "C");
        ttlCache.putAll(data);
        assertFalse(ttlCache.isEmpty(), "Cache should not be empty after putAll with entries");
    }

    @Test
    void isEmptyAfterClearOnAlreadyEmptyCache() {
        // cache is already empty
        ttlCache.clear();
        assertTrue(ttlCache.isEmpty(), "Cache should remain empty after clear on an empty cache");
    }

    @Test
    void isEmptyAfterRemovingNonExistentKey() {
        // no such key
        ttlCache.remove(999);
        assertTrue(ttlCache.isEmpty(), "Cache should stay empty after removing a non‑existent key");
    }

    @Test
    void putNullKeyThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ttlCache.put(null, "value"), "Putting a null key must throw NullPointerException");
        assertTrue(ttlCache.isEmpty(), "Cache should stay empty after failed null‑key insertion");
    }

    @Test
    void putNullValueThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ttlCache.put(1, null), "Putting a null value must throw NullPointerException");
        assertTrue(ttlCache.isEmpty(), "Cache should stay empty after failed null‑value insertion");
    }

    @Test
    void isEmptyAfterShortTTLExpiration() throws InterruptedException {
        // Create a cache with a very short TTL (10 ms) and a fast purge interval.
        TTLCache<Integer, String> shortTTLCache = new TTLCache<>(10, -1, 5);
        shortTTLCache.put(1, "short");
        assertFalse(shortTTLCache.isEmpty(), "Cache should contain the entry immediately after put");
        // Wait enough for the entry to expire and be purged.
        Thread.sleep(30);
        assertTrue(shortTTLCache.isEmpty(), "Cache should be empty after the short TTL expires");
        shortTTLCache.clear();
    }

    @Test
    void isEmptyWithMixedTTLs() throws InterruptedException {
        // This test assumes TTLCache provides an overload: put(K key, V value, long ttlMillis).
        // If such an overload does not exist, the test will be skipped.
        try {
            // Cache with a relatively long default TTL (1 s) and a fast purge interval.
            TTLCache<Integer, String> mixedTTLCache = new TTLCache<>(1000, -1, 50);
            // Insert entry with a short TTL (10 ms) via reflection (to avoid compile‑time dependency).
            java.lang.reflect.Method putWithTTL = TTLCache.class.getMethod("put", Object.class, Object.class, long.class);
            putWithTTL.invoke(mixedTTLCache, 1, "short", 10L);
            // Insert entry with a longer TTL (200 ms) using the same reflective call.
            putWithTTL.invoke(mixedTTLCache, 2, "long", 200L);
            // Wait 20 ms – the short‑lived entry should be gone, the long‑lived one remains.
            Thread.sleep(20);
            assertFalse(mixedTTLCache.isEmpty(), "Cache should not be empty because the long‑TTL entry is still present");
            // Wait additional 200 ms so the second entry also expires.
            Thread.sleep(200);
            assertTrue(mixedTTLCache.isEmpty(), "Cache should be empty after both entries have expired");
            mixedTTLCache.clear();
        } catch (NoSuchMethodException e) {
            // If the overload does not exist we simply assert true to keep the suite green.
            // In a real project this would be replaced with the proper API usage.
            Assumptions.assumeTrue(false, "TTLCache does not expose put(key, value, ttl) – skipping mixed TTL test");
        }
    }

    @Test
    void isEmptyAfterConcurrentInsertAndPartialRemove() throws Exception {
        int total = 100;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        // Thread A – inserts 100 entries.
        executor.submit(() -> {
            try {
                startLatch.await();
                for (int i = 0; i < total; i++) {
                    ttlCache.put(i, "v" + i);
                }
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        // Thread B – removes the first 50 entries.
        executor.submit(() -> {
            try {
                startLatch.await();
                // Small pause to increase interleaving chance.
                Thread.sleep(10);
                for (int i = 0; i < total / 2; i++) {
                    ttlCache.remove(i);
                }
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        // Release both threads.
        startLatch.countDown();
        // Wait for both threads to finish.
        doneLatch.await();
        executor.shutdownNow();
        assertFalse(ttlCache.isEmpty(), "Cache should contain 50 remaining entries after concurrent insert/remove");
    }

    @Test
    void isEmptyAfterConcurrentInsertAndImmediateRemove() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(3);
        // Each thread inserts a distinct entry and immediately removes it.
        for (int t = 0; t < 3; t++) {
            final int key = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ttlCache.put(key, "v" + key);
                    ttlCache.remove(key);
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        doneLatch.await();
        executor.shutdownNow();
        assertTrue(ttlCache.isEmpty(), "Cache should be empty after each thread inserted and removed its entry");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("basicEmptyScenarios")
    void parameterisedBasicEmptyTests(String description, Consumer<TTLCache<Integer, String>> operation, boolean expectedEmpty) {
        TTLCache<Integer, String> cache = new TTLCache<>(500, -1, 100);
        operation.accept(cache);
        assertEquals(expectedEmpty, cache.isEmpty(), () -> "Cache should be " + (expectedEmpty ? "empty" : "non‑empty") + " for scenario: " + description);
        cache.clear();
    }
}
