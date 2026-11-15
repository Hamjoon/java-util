package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.Arrays;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class LRUCache_get_Test {

    private LRUCache<Integer, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // The concrete cache instance is created inside each test (or in the
        // helper method) because the strategy varies per test case.
        cache = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // The threaded strategy creates a shared executor that must be shut down.
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    private void initCache(int capacity, LRUCache.StrategyType type) {
        // Helper for the original tests – retained for compatibility.
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void getReturnsStoredValue(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void getReturnsNullWhenKeyAbsent(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(3, strategy);
        cache.put(10, "ten");
        assertNull(cache.get(99), "Missing key should return null");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void getHandlesNullKeyAndValue(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(3, strategy);
        cache.put(null, null);
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null), "Value associated with null key is null");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void getHandlesNullKeyWithNonNullValue(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(3, strategy);
        cache.put(null, "nullKey");
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue("nullKey"));
        assertEquals("nullKey", cache.get(null));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void getAfterEvictionRespectsLRUOrder(LRUCache.StrategyType strategy) {
        // capacity 2 -> after inserting three distinct keys one will be evicted
        LRUCache<Integer, String> cache = new LRUCache<>(2, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        // access key 1 to make it recently used
        assertEquals("A", cache.get(1));
        // inserting a new entry forces eviction of the least‑recently‑used (key 2)
        cache.put(3, "C");
        // For the threaded strategy eviction happens asynchronously, so we wait a little.
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (!cache.containsKey(2)) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        assertNull(cache.get(2), "Key 2 should have been evicted");
        assertEquals("A", cache.get(1), "Key 1 should still be present");
        assertEquals("C", cache.get(3), "Key 3 should be present");
    }

    /**
     * Basic retrieval – present and absent keys.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void basicRetrieval(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(2, strategy);
        cache.put("alpha", 42);
        // present key
        assertEquals(42, cache.get("alpha"));
        // absent key
        assertNull(cache.get("beta"));
    }

    /**
     * Null key handling when the cache is empty.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void nullKeyWhenCacheEmpty(LRUCache.StrategyType strategy) {
        LRUCache<Object, Object> cache = new LRUCache<>(1, strategy);
        assertNull(cache.get(null), "Getting a null key from an empty cache should return null");
    }

    /**
     * Supplying a key of the wrong generic type must not throw ClassCastException.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void typeMismatchKeyReturnsNull(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(2, strategy);
        cache.put(1, "one");
        // The key type does not match – should simply return null.
        assertNull(cache.get("unexpectedString"));
    }

    /**
     * After a removal, the key should no longer be found.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void postRemovalRetrieval(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(2, strategy);
        cache.put("gamma", 99);
        cache.remove("gamma");
        assertNull(cache.get("gamma"));
    }

    /**
     * After clear(), all entries disappear.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void postClearRetrieval(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(2, strategy);
        cache.put("delta", 55);
        cache.clear();
        assertNull(cache.get("delta"));
    }

    /**
     * Operations after shutdown() must throw IllegalStateException.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void afterShutdownRetrievalThrows(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(2, strategy);
        cache.put("epsilon", 5);
        cache.shutdown();
        assertThrows(IllegalStateException.class, () -> cache.get("epsilon"));
    }

    /**
     * LRU eviction scenario described in the JSON test case.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void lruEvictionBehavior(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(2, strategy);
        cache.put("a", 1);
        cache.put("b", 2);
        // Access "a" to make it most‑recently‑used
        assertEquals(1, cache.get("a"));
        // Adding "c" forces eviction of "b"
        cache.put("c", 3);
        // Wait for async eviction when using the threaded strategy
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (!cache.containsKey("b"))
                break;
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        assertNull(cache.get("b"), "Key 'b' should have been evicted");
        assertEquals(1, cache.get("a"));
        assertEquals(3, cache.get("c"));
    }

    /**
     * Access via get() updates the LRU order, causing a later eviction.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getUpdatesLruOrder(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(2, strategy);
        cache.put("a", 1);
        cache.put("b", 2);
        // Access "a" – now "b" is LRU
        assertEquals(1, cache.get("a"));
        // Insert "d" – should evict "b"
        cache.put("d", 4);
        // Wait for possible async cleanup
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (!cache.containsKey("b"))
                break;
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        assertNull(cache.get("b"), "Key 'b' should have been evicted after get updates LRU order");
        assertEquals(1, cache.get("a"));
        assertEquals(4, cache.get("d"));
    }

    /**
     * putAll() correctly stores entries that can be retrieved via get().
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void putAllRetrieval(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        Map<String, Integer> map = new HashMap<>();
        map.put("x", 10);
        map.put("y", 20);
        cache.putAll(map);
        assertEquals(10, cache.get("x"));
        assertEquals(20, cache.get("y"));
    }

    /**
     * Instantiating a cache with a null underlying strategy should cause NPE on get().
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void nullStrategyCausesNullPointerException(LRUCache.StrategyType strategy) throws Exception {
        // Create a normal cache first, then null‑out the private delegate map.
        LRUCache<String, Integer> cache = new LRUCache<>(2, strategy);
        // Use reflection to set the private field `strategy` to null.
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.get("anyKey"));
    }
}
