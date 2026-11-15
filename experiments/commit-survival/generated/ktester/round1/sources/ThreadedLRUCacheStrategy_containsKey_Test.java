package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_containsKey_Test {

    /**
     * Cache instance used in each test.
     */
    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Create a fresh cache before each test. The capacity and cleanup delay are chosen
     * to be small so the tests run quickly and deterministically.
     */
    @BeforeEach
    void setupBeforeEach() {
        // capacity = 5, cleanupDelayMillis = 50 (both satisfy constructor pre‑conditions)
        cache = new ThreadedLRUCacheStrategy<>(5, 50);
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances.
     * This prevents stray threads from keeping the JVM alive after the test suite finishes.
     */
    @AfterAll
    static void teardownAfterAll() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> simpleKeyProvider() {
        return Stream.of(// already added above
        org.junit.jupiter.params.provider.Arguments.of("alpha", true), org.junit.jupiter.params.provider.Arguments.of("beta", false), org.junit.jupiter.params.provider.Arguments.of("zeta", false), // added above
        org.junit.jupiter.params.provider.Arguments.of("epsilon", true));
    }

    @Test
    void containsKey_returnsTrueForExistingKey() {
        // given
        cache.put("one", 1);
        cache.put("two", 2);
        // when
        boolean result = cache.containsKey("one");
        // then
        assertTrue(result, "containsKey should return true for a key that was put into the cache");
    }

    @Test
    void containsKey_returnsFalseForMissingKey() {
        // given
        cache.put("alpha", 10);
        cache.put("beta", 20);
        // when
        boolean result = cache.containsKey("gamma");
        // then
        assertFalse(result, "containsKey should return false for a key that has never been added");
    }

    @Test
    void containsKey_handlesNullKey() {
        // given
        cache.put(null, 999);
        // when
        boolean result = cache.containsKey(null);
        // then
        assertTrue(result, "containsKey should correctly handle a null key when the cache permits nulls");
    }

    @Test
    void containsKey_returnsFalseAfterRemoval() {
        // given
        cache.put("temp", 42);
        assertTrue(cache.containsKey("temp"), "pre‑condition: key must be present before removal");
        // when
        cache.remove("temp");
        boolean result = cache.containsKey("temp");
        // then
        assertFalse(result, "containsKey should return false after the key has been removed");
    }

    /**
     * The cache contains the key "alpha" after a successful put; containsKey should return true.
     */
    @Test
    void containsKey_true_existingKey_alpha() {
        cache.put("alpha", 1);
        assertTrue(cache.containsKey("alpha"), "containsKey should return true for a key that was put into the cache");
    }

    /**
     * The key "beta" has never been added to the cache; containsKey should return false.
     */
    @Test
    void containsKey_false_missingKey_beta() {
        cache.put("someOther", 99);
        assertFalse(cache.containsKey("beta"), "containsKey should return false for a key that has never been added");
    }

    /**
     * ConcurrentMap implementations used by ThreadedLRUCacheStrategy do not accept null keys;
     * calling containsKey(null) must throw NullPointerException.
     */
    @Test
    void containsKey_nullKey_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> cache.containsKey(null), "containsKey(null) should throw NullPointerException");
    }

    /**
     * After calling clear() on the cache, any previously stored key (e.g., "gamma") is removed;
     * containsKey should return false.
     */
    @Test
    void containsKey_false_afterClear_gamma() {
        cache.put("gamma", 10);
        cache.clear();
        assertFalse(cache.containsKey("gamma"), "containsKey should return false after the cache has been cleared");
    }

    /**
     * After putting "delta" into the cache and then removing it via remove("delta"),
     * containsKey should return false.
     */
    @Test
    void containsKey_false_afterRemove_delta() {
        cache.put("delta", 20);
        cache.remove("delta");
        assertFalse(cache.containsKey("delta"), "containsKey should return false after the key has been removed");
    }

    /**
     * Insert key "epsilon" with a non‑null value, then verify containsKey returns true before any eviction occurs.
     */
    @Test
    void containsKey_true_beforeEviction_epsilon() {
        cache.put("epsilon", 30);
        assertTrue(cache.containsKey("epsilon"), "containsKey should return true for a freshly inserted key");
    }

    /**
     * Check containsKey for a key that was never added and never removed; should return false.
     */
    @Test
    void containsKey_false_neverAdded_zeta() {
        assertFalse(cache.containsKey("zeta"), "containsKey should return false for a key that has never been added");
    }

    /**
     * Add "theta", then call put with the same key but a different value;
     * containsKey should still be true (key presence is independent of value).
     */
    @Test
    void containsKey_true_sameKeyDifferentValue_theta() {
        cache.put("theta", 1);
        // replace value
        cache.put("theta", 2);
        assertTrue(cache.containsKey("theta"), "containsKey should remain true after updating the value for an existing key");
    }

    /**
     * Add "iota", then let the cache exceed its maximum size to trigger LRU eviction;
     * after eviction, containsKey should return false for "iota".
     */
    @Test
    void containsKey_false_afterEviction_iota() {
        // Insert iota first, then add enough additional entries to exceed capacity (5).
        // will become LRU
        cache.put("iota", 1);
        cache.put("a", 2);
        cache.put("b", 3);
        cache.put("c", 4);
        cache.put("d", 5);
        // 6th entry triggers eviction of the least‑recently used ("iota")
        cache.put("e", 6);
        // Give the background cleanup task a moment to run (it is scheduled with a short delay)
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        assertFalse(cache.containsKey("iota"), "containsKey should return false for a key that was evicted by LRU policy");
    }

    /**
     * Insert "kappa", then invoke clear() followed by a put of a different key;
     * containsKey for "kappa" must be false.
     */
    @Test
    void containsKey_false_afterClearAndOtherPut_kappa() {
        cache.put("kappa", 100);
        cache.clear();
        cache.put("other", 200);
        assertFalse(cache.containsKey("kappa"), "containsKey should be false for a key that existed before clear() and not re‑added");
    }

    /**
     * Insert "lambda", remove it with remove("lambda"), then verify containsKey returns false.
     */
    @Test
    void containsKey_false_afterRemove_lambda() {
        cache.put("lambda", 123);
        cache.remove("lambda");
        assertFalse(cache.containsKey("lambda"), "containsKey should return false after the key has been removed");
    }

    /**
     * Insert "mu" with a null value (the cache permits null values);
     * containsKey should still be true because the key exists.
     */
    @Test
    void containsKey_true_nullValue_mu() {
        cache.put("mu", null);
        assertTrue(cache.containsKey("mu"), "containsKey should return true for a key whose associated value is null");
    }

    /**
     * After putting "nu" and then shutting down the cache's background executor,
     * the map remains usable; containsKey should still correctly report false for a non‑existent key.
     */
    @Test
    void containsKey_false_afterShutdown_nu() {
        cache.put("someKey", 1);
        // shutdown shared scheduler
        ThreadedLRUCacheStrategy.shutdown();
        assertFalse(cache.containsKey("nu"), "containsKey should correctly report false for a missing key after shutdown");
    }

    /* --------------------------------------------------------------
     *                     PARAMETERIZED TEST (optional)
     * --------------------------------------------------------------
     *
     * The above individual tests are explicit for readability.
     * The following parameterized test demonstrates an alternative approach
     * for the simple true/false scenarios that do not require special setup.
     */
    @ParameterizedTest(name = "[{index}] key=''{0}'' expected={1}")
    @MethodSource("simpleKeyProvider")
    void containsKey_simpleScenarios(String key, boolean expected) {
        // Arrange – add a subset of keys that are needed for the true‑expected cases.
        if (expected) {
            cache.put(key, 1);
        }
        // Act
        boolean result = cache.containsKey(key);
        // Assert
        assertEquals(expected, result, () -> "containsKey should return " + expected + " for key='" + key + "'");
    }
}
