package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.stream.Stream;

// kept for consistency with the template
@ExtendWith(MockitoExtension.class)
class TTLCache_isEmpty_Test {

    private TTLCache<Integer, String> ttlCache;

    /**
     * Creates a fresh cache instance before each test. The TTL is set to a
     * relatively large value (10 seconds) so that expiration does not interfere
     * with the basic “empty vs. non‑empty” assertions. The maxSize is -1
     * (no LRU eviction) which matches the usage pattern in the original test
     * suite.
     */
    @BeforeEach
    void setupBeforeEach() {
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
     * Shuts down the shared background purge thread used by all TTLCache
     * instances. This mirrors the behaviour of the original {@code TTLCacheTest}
     * class.
     */
    @AfterAll
    static void teardownAfterAll() {
        TTLCache.shutdown();
    }

    /**
     * Supplies the data for the normal‑behaviour tests derived from the JSON
     * description. Each {@link TestScenario} encodes the actions to perform on
     * the cache and the expected result of {@code isEmpty()}.
     */
    private static Stream<TestScenario> normalScenarios() {
        return Stream.of(// 1. newly created cache – already covered by testIsEmptyOnNewCache,
        //    but kept here for completeness of the parameterised suite.
        new TestScenario("new", null, null, false), // 2. after a single put – expect false
        new TestScenario("put", Map.of(1, "value"), null, false), // 3. after putAll with multiple entries – expect false
        new TestScenario("putAll", null, Map.of(10, "v10", 20, "v20"), false), // 4. put then remove – expect true
        new TestScenario("putThenRemove", Map.of(99, "temp"), null, true));
    }

    @Test
    @DisplayName("isEmpty() returns true for a newly created cache")
    void testIsEmptyOnNewCache() {
        // GIVEN a freshly created cache (setup in @BeforeEach)
        // WHEN checking emptiness
        boolean empty = ttlCache.isEmpty();
        // THEN it should be empty
        assertTrue(empty, "A newly created cache must be empty");
    }

    @Test
    @DisplayName("isEmpty() returns false after a put operation")
    void testIsEmptyAfterPut() {
        // GIVEN a cache with one entry
        ttlCache.put(1, "value");
        // WHEN checking emptiness
        boolean empty = ttlCache.isEmpty();
        // THEN it should not be empty
        assertFalse(empty, "Cache should not be empty after a put");
    }

    @Test
    @DisplayName("isEmpty() returns true after clearing a non‑empty cache")
    void testIsEmptyAfterClear() {
        // GIVEN a cache with entries
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        assertFalse(ttlCache.isEmpty(), "Precondition: cache must not be empty");
        // WHEN the cache is cleared
        ttlCache.clear();
        // THEN it should be empty again
        assertTrue(ttlCache.isEmpty(), "Cache should be empty after clear()");
    }

    @Test
    @DisplayName("isEmpty() reflects expiration of entries")
    void testIsEmptyAfterExpiration() throws InterruptedException {
        // Use a short TTL so that entries expire quickly
        // 200 ms TTL, 50 ms cleanup interval
        ttlCache = new TTLCache<>(200L, -1, 50L);
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        // Ensure the cache is not empty right after insertion
        assertFalse(ttlCache.isEmpty(), "Cache should contain entries immediately after put");
        // Wait longer than the TTL so the background task purges the entries
        Thread.sleep(350L);
        // The cache should now be empty
        assertTrue(ttlCache.isEmpty(), "Cache should be empty after entries have expired");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalScenarios")
    @DisplayName("Parameterized isEmpty() behaviour tests")
    void testIsEmptyParameterized(TestScenario scenario) {
        // GIVEN – the cache is freshly created (handled by @BeforeEach)
        switch(scenario.action) {
            case "new" ->
                {
                    // nothing to do
                }
            case "put" ->
                {
                    // single put
                    scenario.singleEntry.forEach(ttlCache::put);
                }
            case "putAll" ->
                {
                    // bulk insertion
                    ttlCache.putAll(scenario.bulkEntries);
                }
            case "putThenRemove" ->
                {
                    // put then remove the same key
                    scenario.singleEntry.forEach(ttlCache::put);
                    scenario.singleEntry.keySet().forEach(ttlCache::remove);
                }
            default ->
                fail("Unsupported scenario action: " + scenario.action);
        }
        // WHEN
        boolean empty = ttlCache.isEmpty();
        // THEN
        assertEquals(scenario.expectedEmpty, empty, "Scenario '" + scenario.action + "' produced unexpected emptiness state.");
    }

    @Test
    @DisplayName("isEmpty() throws NullPointerException when internal cacheMap is null")
    void testIsEmptyWhenCacheMapIsNull() throws Exception {
        // GIVEN a normal cache instance
        assertNotNull(ttlCache, "Cache instance must be non‑null");
        // Use reflection to nullify the private final field `cacheMap`
        Field cacheMapField = TTLCache.class.getDeclaredField("cacheMap");
        cacheMapField.setAccessible(true);
        cacheMapField.set(ttlCache, null);
        // WHEN invoking isEmpty()
        // THEN a NullPointerException is expected because the method dereferences the null field
        assertThrows(NullPointerException.class, ttlCache::isEmpty, "Calling isEmpty() with a null internal map should throw NPE");
    }
}
