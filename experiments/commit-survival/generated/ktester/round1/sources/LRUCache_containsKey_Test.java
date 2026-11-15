package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_containsKey_Test {

    private LRUCache<Integer, String> cache;

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared after each test to avoid cross‑test contamination.
        if (cache != null) {
            cache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the background thread used by the threaded strategy.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Strategies to be used for the parameterised tests.
     */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Initialise the cache with a small capacity – sufficient for the tests.
     */
    private void setUp(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
    }

    /**
     * Replace the private {@code strategy} field of the cache with the supplied map.
     * Used for tests that need to inject a mock or a specially‑behaving map.
     */
    private void injectStrategy(LRUCache<?, ?> target, Map<?, ?> map) throws Exception {
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(target, map);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_returnsTrueWhenKeyPresent(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        assertTrue(cache.containsKey(1), "Cache should contain key 1");
        assertTrue(cache.containsKey(2), "Cache should contain key 2");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_returnsFalseWhenKeyAbsent(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "one");
        assertFalse(cache.containsKey(2), "Cache should not contain key 2");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_handlesNullKey(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(null, "nullValue");
        assertTrue(cache.containsKey(null), "Cache should report presence of null key");
        assertEquals("nullValue", cache.get(null));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_reflectsEvictionPolicy(LRUCache.StrategyType strategy) {
        setUp(strategy);
        // Fill the cache to capacity.
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        // Access key 1 to make it recently used.
        assertEquals("A", cache.get(1));
        // Adding a fourth entry should trigger eviction of the least‑recently‑used key (2).
        cache.put(4, "D");
        // For the threaded strategy eviction may be asynchronous, so we wait briefly.
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (!cache.containsKey(2) && cache.containsKey(1) && cache.containsKey(4)) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        assertFalse(cache.containsKey(2), "Key 2 should have been evicted");
        assertTrue(cache.containsKey(1), "Key 1 should still be present");
        assertTrue(cache.containsKey(4), "Key 4 should be present after insertion");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_handlesNullKeyWhenPresent(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(null, "nullValue");
        assertTrue(cache.containsKey(null), "Cache should report presence of null key");
        assertEquals("nullValue", cache.get(null));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_returnsFalseForNullKeyWhenNotPresent(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "one");
        assertFalse(cache.containsKey(null), "Null key has not been added – should return false");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_returnsFalseOnEmptyCache(LRUCache.StrategyType strategy) {
        setUp(strategy);
        assertFalse(cache.containsKey("anyKey"), "Empty cache must return false for any key");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_afterRemovalReturnsFalse(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(10, "temp");
        assertTrue(cache.containsKey(10), "Key must be present before removal");
        cache.remove(10);
        assertFalse(cache.containsKey(10), "Key should be absent after removal");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_afterClearReturnsFalse(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(42, "answer");
        assertTrue(cache.containsKey(42), "Key must be present before clear");
        cache.clear();
        assertFalse(cache.containsKey(42), "Key should be absent after clear");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_afterShutdownBehavesAsEmpty(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "apple");
        // release resources; the cache should behave like an empty map
        cache.shutdown();
        assertFalse(cache.containsKey(1), "After shutdown the cache should behave as empty");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_throwsNullPointerExceptionWhenStrategyIsNull(LRUCache.StrategyType strategy) throws Exception {
        setUp(strategy);
        // Force the internal strategy map to null.
        injectStrategy(cache, null);
        assertThrows(NullPointerException.class, () -> cache.containsKey("anyKey"), "Calling containsKey with a null internal strategy must throw NPE");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_propagatesNullPointerExceptionFromStrategy(@Mock Map<Object, Object> mockMap, LRUCache.StrategyType strategy) throws Exception {
        setUp(strategy);
        // Simulate a strategy that rejects null keys (e.g., TreeMap)
        when(mockMap.containsKey(null)).thenThrow(NullPointerException.class);
        // For any non‑null key, return false to keep the test focused.
        when(mockMap.containsKey(any())).thenReturn(false);
        injectStrategy(cache, mockMap);
        assertThrows(NullPointerException.class, () -> cache.containsKey(null), "NullPointerException from underlying map must be propagated");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_propagatesClassCastExceptionFromStrategy(@Mock Map<Object, Object> mockMap, LRUCache.StrategyType strategy) throws Exception {
        setUp(strategy);
        // Simulate a map that expects String keys only.
        when(mockMap.containsKey(any())).thenAnswer(invocation -> {
            Object key = invocation.getArgument(0);
            if (!(key instanceof String)) {
                throw new ClassCastException("Wrong key type");
            }
            return false;
        });
        injectStrategy(cache, mockMap);
        assertThrows(ClassCastException.class, () -> cache.containsKey(new Object()), "ClassCastException from underlying map must be propagated");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_propagatesUnsupportedOperationExceptionFromStrategy(@Mock Map<Object, Object> mockMap, LRUCache.StrategyType strategy) throws Exception {
        setUp(strategy);
        // Strategy deliberately throws UOE for any containsKey call.
        when(mockMap.containsKey(any())).thenThrow(UnsupportedOperationException.class);
        injectStrategy(cache, mockMap);
        assertThrows(UnsupportedOperationException.class, () -> cache.containsKey("test"), "UnsupportedOperationException from underlying map must be propagated");
    }
}
