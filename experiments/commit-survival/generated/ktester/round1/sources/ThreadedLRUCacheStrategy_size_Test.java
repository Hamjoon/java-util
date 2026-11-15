package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ThreadedLRUCacheStrategy_size_Test {

    /**
     * Cache instance used in each test.
     */
    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Creates a fresh cache before each test. The capacity is deliberately small
     * (5) and the cleanup delay is short (50 ms) to keep the test fast while still
     * exercising the real implementation (no mocking of internal structures).
     */
    @BeforeEach
    void setupBeforeEach() {
        cache = new ThreadedLRUCacheStrategy<>(5, 50);
    }

    /**
     * Clears the cache after each test to avoid cross‑test contamination.
     * The {@code clear()} method is part of the public API, so it is safe to use.
     */
    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Shuts down the shared scheduler used by all {@code ThreadedLRUCacheStrategy}
     * instances. This prevents thread‑leaks that would otherwise keep the JVM alive
     * after the test suite finishes.
     */
    @AfterAll
    static void teardownAfterAll() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Provides the basic scenarios described in the first JSON group.
     * Each argument consists of:
     *   1. a description (used as the test name),
     *   2. an operation that mutates the cache,
     *   3. the expected size after the operation.
     */
    private static Stream<Arguments> basicSizeScenarios() {
        return Stream.of(// 0 – newly created cache
        Arguments.of("new cache", (Consumer<ThreadedLRUCacheStrategy<String, Integer>>) c -> {
        }, 0), // 1 – after inserting a single entry
        Arguments.of("single insert", (Consumer<ThreadedLRUCacheStrategy<String, Integer>>) c -> c.put("one", 1), 1), // 3 – after inserting three distinct entries
        Arguments.of("three inserts", (Consumer<ThreadedLRUCacheStrategy<String, Integer>>) c -> {
            c.put("a", 1);
            c.put("b", 2);
            c.put("c", 3);
        }, 3), // 2 – after inserting three entries and removing one
        Arguments.of("remove one entry", (Consumer<ThreadedLRUCacheStrategy<String, Integer>>) c -> {
            c.put("a", 1);
            c.put("b", 2);
            c.put("c", 3);
            c.remove("b");
        }, 2), // 0 – after inserting entries then clearing the cache
        Arguments.of("clear after inserts", (Consumer<ThreadedLRUCacheStrategy<String, Integer>>) c -> {
            c.put("x", 10);
            c.put("y", 20);
            c.clear();
        }, 0));
    }

    /**
     * The cache should be empty immediately after construction.
     */
    @Test
    void size_isZeroWhenCacheIsNew() {
        assertEquals(0, cache.size(), "A newly created cache must have size 0");
    }

    /**
     * Adding entries should increase the reported size accordingly.
     */
    @Test
    void size_reflectsNumberOfPutEntries() {
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        assertEquals(3, cache.size(), "Size should match the number of distinct keys put into the cache");
    }

    /**
     * Adding a duplicate key must not increase the size.
     */
    @Test
    void size_doesNotIncreaseWhenPuttingExistingKey() {
        cache.put("key", 100);
        assertEquals(1, cache.size());
        // Put the same key with a different value – size must stay 1
        cache.put("key", 200);
        assertEquals(1, cache.size(), "Putting an existing key should replace the value without changing size");
    }

    /**
     * Removing a key should decrease the size.
     */
    @Test
    void size_decreasesAfterRemove() {
        cache.put("a", 10);
        cache.put("b", 20);
        cache.put("c", 30);
        assertEquals(3, cache.size());
        cache.remove("b");
        assertEquals(2, cache.size(), "Size should decrement after a successful removal");
    }

    /**
     * When the cache exceeds its capacity, the internal purge task runs, but
     *  {@code size()} must still report the current number of entries (which may be
     *  temporarily larger than the capacity until the purge executes).
     */
    @Test
    void size_canTemporarilyExceedCapacity_beforePurge() throws InterruptedException {
        // Capacity is 5; insert 7 entries to trigger a purge.
        for (int i = 0; i < 7; i++) {
            cache.put("k" + i, i);
        }
        // The size may be 7 immediately after puts.
        assertTrue(cache.size() >= 5, "Size should be at least the capacity before the background purge runs");
        // Give the scheduled purge a moment to run.
        Thread.sleep(100);
        assertTrue(cache.size() <= 5, "After the purge task, size must not exceed the configured capacity");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("basicSizeScenarios")
    @Order(1)
    void size_basicScenarios(String description, Consumer<ThreadedLRUCacheStrategy<String, Integer>> operation, int expected) {
        // Apply the scenario‑specific operations on the freshly created cache.
        operation.accept(cache);
        assertEquals(expected, cache.size(), () -> "Scenario '" + description + "' expected size " + expected);
    }

    /**
     * Duplicate key insertion must not increase the size.
     */
    @Test
    @Order(2)
    void size_duplicateKeyDoesNotIncrease() {
        cache.put("k1", 1);
        // same key, different value
        cache.put("k1", 2);
        assertEquals(1, cache.size(), "Putting an existing key should replace the value without changing size");
    }

    /**
     * putAll() with overlapping keys should overwrite and keep distinct count.
     */
    @Test
    @Order(3)
    void size_putAllWithOverlap() {
        Map<String, Integer> first = new HashMap<>();
        first.put("a", 1);
        first.put("b", 2);
        cache.putAll(first);
        Map<String, Integer> second = new HashMap<>();
        // overlap
        second.put("b", 3);
        second.put("c", 4);
        cache.putAll(second);
        assertEquals(3, cache.size(), "After overlapping putAll calls, size should reflect distinct keys only");
    }

    /**
     * Removing a non‑existent key must leave the size unchanged.
     */
    @Test
    @Order(4)
    void size_removeMissingKeyHasNoEffect() {
        cache.put("x", 100);
        cache.put("y", 200);
        // no such key
        cache.remove("missing");
        assertEquals(2, cache.size(), "Removing a key that does not exist should not affect the size");
    }

    /**
     * Inserting a large number of entries verifies size under high load.
     */
    @Test
    @Order(5)
    void size_largeNumberOfEntries() {
        // Re‑create a cache with a capacity large enough to hold 1000 entries without immediate eviction.
        cache = new ThreadedLRUCacheStrategy<>(2000, 1000);
        for (int i = 0; i < 1000; i++) {
            cache.put("k" + i, i);
        }
        assertEquals(1000, cache.size(), "Cache should report 1000 after inserting 1000 unique entries");
    }

    /**
     * Repeated calls to size() without intervening modifications must be idempotent.
     */
    @Test
    @Order(6)
    void size_idempotentCalls() {
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        int first = cache.size();
        int second = cache.size();
        int third = cache.size();
        assertAll(() -> assertEquals(3, first, "First size call should be 3"), () -> assertEquals(3, second, "Second size call should still be 3"), () -> assertEquals(3, third, "Third size call should still be 3"));
    }

    /**
     * After shutting down the shared executor, size() should still return the correct value.
     */
    @Test
    // ensure this runs after the other tests
    @Order(99)
    void size_afterShutdownDoesNotClearEntries() {
        // The cache is newly created and empty.
        assertEquals(0, cache.size(), "New cache should have size 0 before shutdown");
        // Shut down the internal scheduler.
        ThreadedLRUCacheStrategy.shutdown();
        // Size must still report 0 (shutdown does not clear the map).
        assertEquals(0, cache.size(), "Size should remain 0 after shutdown for a newly created cache");
    }
}
