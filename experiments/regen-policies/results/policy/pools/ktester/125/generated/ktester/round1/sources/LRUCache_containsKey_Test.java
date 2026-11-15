package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LRUCache_containsKey_Test {

    /**
     * The cache instance used by the non‑parameterised tests.
     */
    private LRUCache<Integer, String> cache;

    /**
     * Initialise a fresh cache before each test (default to LOCKING strategy).
     */
    @BeforeEach
    void setupBeforeEach() {
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            cache.clear();
            cache = null;
        }
    }

    /**
     * Shut down the threaded strategy's internal executor service after all tests have run.
     */
    @AfterAll
    static void teardownAfterAll() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Provides the two supported strategy types for the parameterised tests.
     */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    private static Stream<Arguments> basicFunctionalityCases() {
        return Stream.of(// key present
        Arguments.of("existingKey", true, "Key is present in the underlying strategy map; containsKey should return true."), // key missing
        Arguments.of("missingKey", false, "Key is not present in the underlying strategy map; containsKey should return false."), // null key – according to the JSON spec the strategy does not allow null keys
        Arguments.of(null, false, "Null key is not stored in the cache (strategy does not allow null keys); containsKey should return false."));
    }

    private static Stream<Arguments> nonStringKeyCases() {
        return Stream.of(Arguments.of(123, true, "An Integer key is stored in the cache; containsKey should return true."), Arguments.of(456, false, "An Integer key that has never been added is queried; containsKey should return false."));
    }

    @Test
    void containsKey_returnsTrueWhenKeyIsPresent() {
        cache.put(1, "one");
        cache.put(2, "two");
        assertTrue(cache.containsKey(1), "Cache should contain key 1");
        assertTrue(cache.containsKey(2), "Cache should contain key 2");
    }

    @Test
    void containsKey_returnsFalseWhenKeyIsAbsent() {
        cache.put(1, "one");
        cache.put(2, "two");
        assertFalse(cache.containsKey(3), "Cache should not contain key 3");
    }

    @Test
    void containsKey_handlesNullKey() {
        cache.put(null, "nullValue");
        assertTrue(cache.containsKey(null), "Cache should recognise a null key");
        assertEquals("nullValue", cache.get(null));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_reflectsEvictionForBothStrategies(LRUCache.StrategyType strategy) {
        // Use a small capacity to trigger eviction quickly.
        LRUCache<Integer, String> evictCache = new LRUCache<>(3, strategy);
        evictCache.put(1, "A");
        evictCache.put(2, "B");
        evictCache.put(3, "C");
        // Access key 1 to make it most‑recently used.
        evictCache.get(1);
        // Adding a fourth entry should evict the least‑recently used key (2).
        evictCache.put(4, "D");
        // For the threaded strategy eviction happens asynchronously, so we wait a short time.
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (!evictCache.containsKey(2)) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        assertFalse(evictCache.containsKey(2), "Key 2 should have been evicted");
        assertTrue(evictCache.containsKey(1), "Key 1 should still be present");
        assertTrue(evictCache.containsKey(4), "Key 4 should be present");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: containsKey({0}) => {2}")
    @MethodSource("basicFunctionalityCases")
    void containsKey_basicFunctionality(Object key, boolean expected, String description) {
        // Arrange – put a known key unless the test case expects the key to be absent.
        if (expected && !"null".equals(String.valueOf(key))) {
            // Use a generic value; the actual type of the key does not matter.
            cache.put((Integer) (key instanceof Integer ? key : 1), "value");
        }
        // Act
        boolean result = cache.containsKey(key);
        // Assert
        assertEquals(expected, result, description);
    }

    // -----------------------------------------------------------------
    @Test
    void containsKey_afterClear() {
        cache.put(42, "answer");
        cache.clear();
        assertFalse(cache.containsKey(42), "After clear(), the previously stored key must not be present.");
    }

    // -----------------------------------------------------------------
    @Test
    void containsKey_afterRemoval() {
        cache.put(99, "ninety‑nine");
        cache.remove(99);
        assertFalse(cache.containsKey(99), "After remove(key), the key must no longer be present.");
    }

    // -----------------------------------------------------------------
    @Test
    void containsKey_afterPutNewKey() {
        cache.put(7, "seven");
        assertTrue(cache.containsKey(7), "A newly added key must be reported as present.");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_evictionDueToCapacity(LRUCache.StrategyType strategy) {
        // capacity of 2 forces eviction when a third entry is added
        LRUCache<Integer, String> smallCache = new LRUCache<>(2, strategy);
        smallCache.put(1, "first");
        smallCache.put(2, "second");
        // Access key 1 to make it most‑recently used
        smallCache.get(1);
        // Adding the third entry should evict key 2 (least‑recently used)
        smallCache.put(3, "third");
        // For the threaded strategy eviction may be asynchronous – wait briefly
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline && smallCache.containsKey(2)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        assertFalse(smallCache.containsKey(2), "Key 2 should have been evicted according to LRU policy.");
        assertTrue(smallCache.containsKey(1), "Key 1 should still be present after eviction.");
        assertTrue(smallCache.containsKey(3), "Key 3 should be present after insertion.");
        smallCache.clear();
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: containsKey({0}) => {2}")
    @MethodSource("nonStringKeyCases")
    void containsKey_nonStringKeyTypes(Object key, boolean expected, String description) {
        // Insert a known integer key if we expect it to be present
        if (expected) {
            cache.put((Integer) key, "intValue");
        }
        boolean result = cache.containsKey(key);
        assertEquals(expected, result, description);
    }

    // -----------------------------------------------------------------
    @Test
    void containsKey_keyPresentWithNullValue() {
        cache.put(555, null);
        assertTrue(cache.containsKey(555), "Key should be reported as present even if its associated value is null.");
    }

    // -----------------------------------------------------------------
    @Test
    void containsKey_afterShutdownReturnsFalse() {
        cache.put(1, "one");
        // puts the cache into an inactive state
        cache.shutdown();
        // According to the specification, after shutdown the internal map is cleared,
        // therefore containsKey should simply return false (no exception).
        assertFalse(cache.containsKey(1), "After shutdown, containsKey should return false for any key.");
    }

    // -----------------------------------------------------------------
    @Test
    void containsKey_afterShutdownThrowsIllegalStateException() {
        cache.put(2, "two");
        cache.shutdown();
        // Some implementations may throw IllegalStateException after shutdown.
        // The test asserts that the exception is propagated.
        assertThrows(IllegalStateException.class, () -> cache.containsKey(2), "After shutdown, containsKey should propagate IllegalStateException.");
    }
}
