package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class ThreadedLRUCacheStrategy_put_Test {

    // small but > 10 as required
    private static final int CLEANUP_DELAY_MILLIS = 50;

    private ThreadedLRUCacheStrategy<String, String> cache;

    private int capacity;

    /**
     * Simple holder for put‑method input / expected output.
     */
    private static final class PutScenario {

        final String key;

        final String value;

        final String expectedReturn;

        final String description;

        PutScenario(String key, String value, String expectedReturn, String description) {
            this.key = key;
            this.value = value;
            this.expectedReturn = expectedReturn;
            this.description = description;
        }
    }

    /**
     * Holder for exception‑expectation scenarios.
     */
    private static final class ExceptionScenario {

        final Object key;

        final Object value;

        final Class<? extends Throwable> expectedException;

        final String description;

        ExceptionScenario(Object key, Object value, Class<? extends Throwable> expectedException, String description) {
            this.key = key;
            this.value = value;
            this.expectedException = expectedException;
            this.description = description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // each test gets a fresh cache instance
        // default capacity; individual tests can override
        capacity = 2;
        cache = new ThreadedLRUCacheStrategy<>(capacity, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that background purge tasks (if any) do not interfere
        cache.clear();
    }

    /**
     * Data provider for simple put scenarios that only check the returned value.
     */
    private static Stream<PutScenario> simplePutScenarios() {
        return Stream.of(// Insert first entry when cache is empty; oldNode is null → return null
        new PutScenario("A", "first", null, "Insert first entry when cache is empty"), // Insert entry with an existing key; oldNode is not null → return previous value
        new PutScenario("A", "second", "first", "Insert entry with an existing key"), // Insert second distinct key while cache size is within capacity → return null
        new PutScenario("B", "valueB", null, "Insert second distinct key within capacity"), // Insert third distinct key causing size > capacity → return null (cleanup scheduled)
        new PutScenario("C", "valueC", null, "Insert third distinct key exceeding capacity"));
    }

    /**
     * Data provider for exceptional cases (null key/value, wrong type).
     */
    private static Stream<ExceptionScenario> exceptionScenarios() {
        return Stream.of(new ExceptionScenario(null, "someValue", NullPointerException.class, "Passing a null key should throw NullPointerException"), new ExceptionScenario("someKey", null, NullPointerException.class, "Passing a null value should throw NullPointerException"), new ExceptionScenario(123, "someValue", ClassCastException.class, "Wrong key type should cause ClassCastException"), new ExceptionScenario("someKey", 456, ClassCastException.class, "Wrong value type should cause ClassCastException"));
    }

    @Test
    void putNewKeyReturnsNullAndIncreasesSize() {
        // given
        String key = "A";
        String value = "Alpha";
        // when
        String previous = cache.put(key, value);
        // then
        assertNull(previous, "put should return null when the key is new");
        assertEquals(1, cache.size(), "Cache size should be 1 after inserting a new entry");
        assertEquals(value, cache.get(key), "Stored value should be retrievable");
    }

    @Test
    void putExistingKeyReturnsOldValueAndUpdatesTimestamp() {
        // given
        String key = "B";
        String firstValue = "Beta";
        String secondValue = "Beta2";
        // first insertion
        cache.put(key, firstValue);
        // when
        // update same key
        String returned = cache.put(key, secondValue);
        // then
        assertEquals(firstValue, returned, "put should return the previous value for an existing key");
        assertEquals(1, cache.size(), "Cache size must stay the same when updating an existing key");
        assertEquals(secondValue, cache.get(key), "The value associated with the key should be the new one");
    }

    @Test
    void putExceedsCapacityTriggersImmediateCleanup() {
        // given a cache with capacity 2
        cache = new ThreadedLRUCacheStrategy<>(capacity, CLEANUP_DELAY_MILLIS);
        // oldest
        cache.put("k1", "v1");
        // newer
        cache.put("k2", "v2");
        // when inserting a third entry, which should cause cleanup
        cache.put("k3", "v3");
        // then the cache size must not exceed the configured capacity
        assertTrue(cache.size() <= capacity, "Cache size must be less than or equal to the configured capacity after cleanup");
        // the least‑recently‑used entry ("k1") should have been evicted
        assertNull(cache.get("k1"), "The LRU entry should have been evicted after exceeding capacity");
        assertNotNull(cache.get("k2"), "The second entry should still be present");
        assertNotNull(cache.get("k3"), "The newly added entry should be present");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("simplePutScenarios")
    void putSimpleScenarios(PutScenario scenario) {
        // Arrange: ensure the cache is at the expected state before the operation
        // (For the third scenario we need to pre‑populate the cache to reach capacity.)
        if ("Insert third distinct key exceeding capacity".equals(scenario.description)) {
            cache.put("A", "first");
            cache.put("B", "valueB");
        } else if ("Insert second distinct key within capacity".equals(scenario.description)) {
            cache.put("A", "first");
        } else if ("Insert entry with an existing key".equals(scenario.description)) {
            cache.put("A", "first");
        }
        // Act
        String result = cache.put(scenario.key, scenario.value);
        // Assert
        assertEquals(scenario.expectedReturn, result, "Unexpected return value for scenario: " + scenario.description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionScenarios")
    void putExceptionScenarios(ExceptionScenario scenario) {
        // The cache is typed <String,String>, so any non‑String key/value should trigger a ClassCastException.
        assertThrows(scenario.expectedException, () -> {
            // The raw put method is used to avoid compile‑time generic checks.
            @SuppressWarnings("unchecked")
            ThreadedLRUCacheStrategy rawCache = cache;
            rawCache.put(scenario.key, scenario.value);
        }, scenario.description);
    }

    @Test
    void putWithZeroCapacityImmediatelyCleansUp() {
        // capacity = 0 means any insertion should be evicted right away
        ThreadedLRUCacheStrategy<String, String> zeroCapCache = new ThreadedLRUCacheStrategy<>(0, CLEANUP_DELAY_MILLIS);
        String returned = zeroCapCache.put("K1", "V1");
        assertNull(returned, "When capacity is zero, put should return null");
        assertEquals(0, zeroCapCache.size(), "Cache size must be zero after insertion when capacity is zero");
        assertNull(zeroCapCache.get("K1"), "No entry should remain in the cache");
    }

    @Test
    void putReplaceWhenOverCapacityDoesNotTriggerAnotherCleanup() {
        // Fill the cache to exceed capacity and cause an eviction
        // 1
        cache.put("A", "oldValue");
        // 2
        cache.put("B", "valueB");
        // triggers cleanup, evicts "A"
        cache.put("C", "valueC");
        // At this point "A" may have been evicted; ensure it is present for the replacement test
        // re‑insert A so it exists
        cache.put("A", "oldValue");
        // Now replace existing key "A"
        String returned = cache.put("A", "newValue");
        assertEquals("oldValue", returned, "Replacing an existing key should return its previous value");
        assertEquals("newValue", cache.get("A"), "Value should be updated after replacement");
        assertTrue(cache.size() <= capacity, "Cache size must stay within capacity after replacement (no extra cleanup)");
    }

    @Test
    void putMultipleReplacementsReturnImmediatePreviousValue() {
        // First insertion
        assertNull(cache.put("M", "first"), "First insertion should return null");
        // Second insertion (replace)
        assertEquals("first", cache.put("M", "second"), "Second insertion should return the immediate prior value");
        // Third insertion (replace again)
        assertEquals("second", cache.put("M", "third"), "Third insertion should return the immediate prior value");
    }
}
