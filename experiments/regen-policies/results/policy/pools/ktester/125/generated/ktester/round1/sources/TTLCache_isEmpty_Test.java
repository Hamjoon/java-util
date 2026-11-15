package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TTLCache_isEmpty_Test {

    /**
     * Cache instance used in each test.
     */
    private TTLCache<Integer, String> ttlCache;

    /**
     * Helper holder for a parameterized scenario.
     */
    private static class IsEmptyScenario {

        final String description;

        final java.util.function.Consumer<TTLCache<Integer, String>> setup;

        final boolean expected;

        IsEmptyScenario(String description, java.util.function.Consumer<TTLCache<Integer, String>> setup, boolean expected) {
            this.description = description;
            this.setup = setup;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Creates a fresh cache before each test case.
     */
    @BeforeEach
    void setupBeforeEach() {
        // TTL of 10 seconds, no size limit, default cleanup interval.
        ttlCache = new TTLCache<>(10_000L, -1);
    }

    /**
     * Clears the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (ttlCache != null) {
            ttlCache.clear();
        }
    }

    /**
     * Shuts down the shared background thread after all tests have run.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    /* -------------------------------------------------------------
       Parameterized test covering the basic behavior group from JSON.
       ------------------------------------------------------------- */
    static Stream<IsEmptyScenario> basicBehaviorProvider() {
        return Stream.of(new IsEmptyScenario("new cache", cache -> {
        }, true), new IsEmptyScenario("single put", cache -> cache.put(1, "X"), false), new IsEmptyScenario("clear after put", cache -> {
            cache.put(1, "X");
            cache.clear();
        }, true), new IsEmptyScenario("remove only entry", cache -> {
            cache.put(1, "X");
            cache.remove(1);
        }, true), new IsEmptyScenario("multiple puts", cache -> {
            cache.put(1, "A");
            cache.put(2, "B");
            cache.put(3, "C");
        }, false));
    }

    @Test
    void givenNewCache_whenIsEmpty_thenTrue() {
        // GIVEN a newly created cache (already set up in @BeforeEach)
        // WHEN
        boolean empty = ttlCache.isEmpty();
        // THEN
        assertTrue(empty, "A newly created cache must be empty");
    }

    @Test
    void givenCacheWithOneEntry_whenIsEmpty_thenFalse() {
        // GIVEN a cache containing a single entry
        ttlCache.put(1, "A");
        // WHEN
        boolean empty = ttlCache.isEmpty();
        // THEN
        assertFalse(empty, "Cache with an entry should not be reported as empty");
    }

    @Test
    void givenCacheThatExpiresAllEntries_whenIsEmpty_thenTrue() throws InterruptedException {
        // GIVEN a cache with a very short TTL and a fast cleanup interval
        ttlCache = new TTLCache<>(200L, -1, 50L);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        // Ensure the entries are present initially
        assertEquals(2, ttlCache.size());
        // WHEN – wait longer than the TTL so the background purge removes the entries
        // > TTL (200 ms)
        Thread.sleep(300L);
        // THEN
        assertTrue(ttlCache.isEmpty(), "Cache should be empty after all entries have expired");
    }

    @Test
    void givenCacheWithOneEntry_whenClear_thenIsEmptyTrue() {
        // GIVEN a non‑empty cache
        ttlCache.put(1, "A");
        assertFalse(ttlCache.isEmpty(), "Pre‑condition: cache must be non‑empty before clear");
        // WHEN
        ttlCache.clear();
        // THEN
        assertTrue(ttlCache.isEmpty(), "After clear() the cache must be empty");
    }

    @Test
    void givenCacheWithOneEntry_whenRemove_thenIsEmptyTrue() {
        // GIVEN a cache with a single entry
        ttlCache.put(42, "answer");
        assertFalse(ttlCache.isEmpty(), "Pre‑condition: cache must be non‑empty before remove");
        // WHEN
        ttlCache.remove(42);
        // THEN
        assertTrue(ttlCache.isEmpty(), "Removing the only entry should leave the cache empty");
    }

    @Test
    void givenCacheWithMultipleEntries_whenIsEmpty_thenFalse() {
        // GIVEN three distinct entries
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        // WHEN
        boolean empty = ttlCache.isEmpty();
        // THEN
        assertFalse(empty, "Cache with multiple entries must not be reported as empty");
    }

    @Test
    void givenAlreadyEmptyCache_whenClear_thenIsEmptyTrue() {
        // GIVEN a freshly created (empty) cache
        assertTrue(ttlCache.isEmpty(), "Pre‑condition: cache must start empty");
        // WHEN
        ttlCache.clear();
        // THEN
        assertTrue(ttlCache.isEmpty(), "Calling clear() on an already empty cache must keep it empty");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("basicBehaviorProvider")
    void ttlCacheIsEmpty_basicBehavior(IsEmptyScenario scenario) {
        // GIVEN – scenario sets up the cache as required
        scenario.setup.accept(ttlCache);
        // WHEN
        boolean empty = ttlCache.isEmpty();
        // THEN
        assertEquals(scenario.expected, empty, () -> "Scenario '" + scenario.description + "' expected isEmpty() to be " + scenario.expected);
    }
}
