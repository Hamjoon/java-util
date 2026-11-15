package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_clear_Test {

    /**
     * Instance under test – created fresh for each test method.
     */
    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Minimal valid constructor arguments (capacity ≥ 1, delay ≥ 10 ms).
     */
    private static final int CAPACITY = 5;

    private static final int CLEANUP_DELAY_MILLIS = 10;

    /**
     * Simple Awaitility wrapper to avoid pulling in the full library.
     * In a real project you would use org.awaitility.Awaitility,
     * but for this isolated test we implement a minimal busy‑wait.
     */
    class Awaitility {

        static ConditionAwaiter await() {
            return new ConditionAwaiter();
        }

        static class ConditionAwaiter {

            private long timeoutMs = 1000;

            ConditionAwaiter atMost(long duration, TimeUnit unit) {
                this.timeoutMs = unit.toMillis(duration);
                return this;
            }

            void until(Callable<Boolean> condition) throws InterruptedException {
                long deadline = System.currentTimeMillis() + timeoutMs;
                while (System.currentTimeMillis() < deadline) {
                    try {
                        if (condition.call()) {
                            return;
                        }
                    } catch (Exception ignored) {
                    }
                    Thread.sleep(10);
                }
                fail("Condition not met within timeout of " + timeoutMs + " ms");
            }
        }
    }

    static class ConditionAwaiter {

        private long timeoutMs = 1000;

        ConditionAwaiter atMost(long duration, TimeUnit unit) {
            this.timeoutMs = unit.toMillis(duration);
            return this;
        }

        void until(Callable<Boolean> condition) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                try {
                    if (condition.call()) {
                        return;
                    }
                } catch (Exception ignored) {
                }
                Thread.sleep(10);
            }
            fail("Condition not met within timeout of " + timeoutMs + " ms");
        }
    }

    static class ConditionAwaiter {

        private long timeoutMs = 1000;

        ConditionAwaiter atMost(long duration, TimeUnit unit) {
            this.timeoutMs = unit.toMillis(duration);
            return this;
        }

        void until(Callable<Boolean> condition) throws InterruptedException {
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (System.currentTimeMillis() < deadline) {
                try {
                    if (condition.call()) {
                        return;
                    }
                } catch (Exception ignored) {
                }
                Thread.sleep(10);
            }
            fail("Condition not met within timeout of " + timeoutMs + " ms");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a new cache for every test to guarantee isolation.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that any background purge task does not
        // retain references that could affect subsequent tests.
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
    void clear_RemovesAllEntries_WhenCacheIsPopulated() {
        // given: a cache populated with several entries
        Map<String, Integer> data = new HashMap<>();
        data.put("one", 1);
        data.put("two", 2);
        data.put("three", 3);
        data.forEach(cache::put);
        assertEquals(3, cache.size(), "Pre‑condition: cache should contain 3 entries");
        // when: clear() is invoked
        cache.clear();
        // then: the cache reports as empty
        assertTrue(cache.isEmpty(), "Cache should be empty after clear()");
        assertEquals(0, cache.size(), "Size should be 0 after clear()");
    }

    @Test
    void clear_OnEmptyCache_IsIdempotent() {
        // given: an empty cache
        assertTrue(cache.isEmpty(), "Cache should start empty");
        // when: clear() is called
        cache.clear();
        // then: the cache remains empty and no exception is thrown
        assertTrue(cache.isEmpty(), "Cache should remain empty after clear()");
        assertEquals(0, cache.size(), "Size should still be 0 after clear()");
    }

    @Test
    void clear_AfterPartialRemoval_EmptiesCache() {
        // given: a cache with three entries
        cache.put("a", 10);
        cache.put("b", 20);
        cache.put("c", 30);
        assertEquals(3, cache.size());
        // when: one entry is removed
        cache.remove("b");
        assertEquals(2, cache.size(), "Size after one removal should be 2");
        // and: clear() is invoked
        cache.clear();
        // then: cache is completely empty
        assertTrue(cache.isEmpty(), "Cache should be empty after clear()");
        assertEquals(0, cache.size());
        // previously stored keys return null
        assertNull(cache.get("a"));
        assertNull(cache.get("c"));
    }

    @Test
    void clear_AfterPutAll_EmptiesCache() {
        // given: a source map with several entries
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 100);
        source.put("y", 200);
        source.put("z", 300);
        // when: putAll is used to populate the cache
        cache.putAll(source);
        assertEquals(source.size(), cache.size());
        // then: each key is retrievable
        source.forEach((k, v) -> assertEquals(v, cache.get(k)));
        // when: clear() is called
        cache.clear();
        // then: cache reports empty and all previous keys return null
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        source.keySet().forEach(k -> assertNull(cache.get(k)));
    }

    @Test
    void clear_UnderConcurrentModifications_IsThreadSafe() throws InterruptedException {
        // given: a background thread continuously inserting entries
        AtomicBoolean running = new AtomicBoolean(true);
        Thread writer = new Thread(() -> {
            int i = 0;
            while (running.get()) {
                cache.put("key-" + i, i);
                i++;
                // small pause to avoid saturating CPU
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            }
        });
        writer.start();
        // wait until at least a few entries appear
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> cache.size() > 5);
        // when: clear() is invoked while the writer is still running
        cache.clear();
        // then: after clear() returns the cache must be empty
        assertTrue(cache.isEmpty(), "Cache should be empty immediately after clear()");
        assertEquals(0, cache.size());
        // stop the writer and wait for termination
        running.set(false);
        writer.join();
        // after stopping, the cache should still be empty
        assertTrue(cache.isEmpty());
        // and a new put works normally
        cache.put("final", 999);
        assertEquals(1, cache.size());
        assertEquals(999, cache.get("final"));
    }

    @Test
    void clear_AfterEvictionDueToCapacity_EmptiesCacheAndAllowsReuse() throws InterruptedException {
        // given: a cache with capacity 5
        ThreadedLRUCacheStrategy<Integer, String> smallCache = new ThreadedLRUCacheStrategy<>(5, CLEANUP_DELAY_MILLIS);
        // insert 10 entries to trigger eviction
        for (int i = 0; i < 10; i++) {
            smallCache.put(i, "val" + i);
        }
        // allow scheduled cleanup to run (it runs after each put)
        Thread.sleep(50);
        // then: size should not exceed the configured capacity
        assertTrue(smallCache.size() <= 5, "Size after eviction should be <= capacity (actual: " + smallCache.size() + ")");
        // when: clear() is called
        smallCache.clear();
        // then: cache is empty
        assertTrue(smallCache.isEmpty());
        assertEquals(0, smallCache.size());
        // and: cache can be reused
        smallCache.put(42, "answer");
        assertEquals(1, smallCache.size());
        assertEquals("answer", smallCache.get(42));
        // cleanup
        smallCache.clear();
    }

    @Test
    void clear_WhileIteratingEntrySet_DoesNotThrowConcurrentModificationException() {
        // given: a populated cache
        cache.put("k1", 1);
        cache.put("k2", 2);
        cache.put("k3", 3);
        assertEquals(3, cache.size());
        // when: we obtain an iterator and call clear() during iteration
        Iterator<Map.Entry<String, Integer>> it = cache.entrySet().iterator();
        assertTrue(it.hasNext());
        // advance one step
        it.next();
        // invoke clear while iterator is still alive
        cache.clear();
        // then: continue iteration – no CME should be thrown
        while (it.hasNext()) {
            // just consume remaining entries
            it.next();
        }
        // finally, cache must be empty
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }
}
