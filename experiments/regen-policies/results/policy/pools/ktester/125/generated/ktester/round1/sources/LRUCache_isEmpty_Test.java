package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_isEmpty_Test {

    private LRUCache<Integer, String> lruCache;

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared after each test to avoid cross‑test contamination.
        if (lruCache != null) {
            lruCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the background thread used by the threaded strategy.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /* --------------------------------------------------------------------- */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyWhenCacheIsNew(LRUCache.StrategyType strategy) {
        // given
        lruCache = new LRUCache<>(3, strategy);
        // then
        assertTrue(lruCache.isEmpty(), "A newly created cache should be empty");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyAfterPut(LRUCache.StrategyType strategy) {
        // given
        lruCache = new LRUCache<>(3, strategy);
        lruCache.put(1, "A");
        // then
        assertFalse(lruCache.isEmpty(), "Cache should report non‑empty after a put");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyAfterClear(LRUCache.StrategyType strategy) {
        // given
        lruCache = new LRUCache<>(3, strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        assertFalse(lruCache.isEmpty(), "Pre‑condition: cache must be non‑empty");
        // when
        lruCache.clear();
        // then
        assertTrue(lruCache.isEmpty(), "Cache should be empty after clear()");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyWithNullKeyAndValue(LRUCache.StrategyType strategy) {
        // given
        lruCache = new LRUCache<>(3, strategy);
        lruCache.put(null, null);
        // then
        assertFalse(lruCache.isEmpty(), "Cache containing a null entry is not empty");
        // when
        lruCache.remove(null);
        // then
        assertTrue(lruCache.isEmpty(), "Cache should be empty after removing the null entry");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyAfterEviction(LRUCache.StrategyType strategy) {
        // given
        lruCache = new LRUCache<>(2, strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        assertFalse(lruCache.isEmpty(), "Pre‑condition: cache must be non‑empty");
        // when – add a third entry to trigger eviction
        lruCache.put(3, "C");
        // then – wait briefly for the threaded strategy to evict if needed
        if (strategy == LRUCache.StrategyType.THREADED) {
            try {
                // give background cleanup a chance
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
        // The cache size should be 2 (capacity), therefore still non‑empty
        assertFalse(lruCache.isEmpty(), "Cache should remain non‑empty after eviction");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyAfterRemovingAllEntriesIndividually(LRUCache.StrategyType strategy) {
        // given – populate the cache with three distinct entries
        lruCache = new LRUCache<>(5, strategy);
        lruCache.put(10, "X");
        lruCache.put(20, "Y");
        lruCache.put(30, "Z");
        assertFalse(lruCache.isEmpty(), "Pre‑condition: cache must be non‑empty after puts");
        // when – remove each key one by one
        lruCache.remove(10);
        assertFalse(lruCache.isEmpty(), "Cache should still be non‑empty after removing one entry");
        lruCache.remove(20);
        assertFalse(lruCache.isEmpty(), "Cache should still be non‑empty after removing second entry");
        lruCache.remove(30);
        // then – cache must be empty now
        assertTrue(lruCache.isEmpty(), "Cache should be empty after all entries are removed individually");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyPropagatesStrategyResult(LRUCache.StrategyType strategy) throws Exception {
        // given – create a normal cache and then replace its private strategy field
        lruCache = new LRUCache<>(3, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockedStrategy = mock(Map.class);
        // Simulate a non‑empty underlying map
        when(mockedStrategy.isEmpty()).thenReturn(false);
        // Also make other map operations behave reasonably for the test
        when(mockedStrategy.put(anyInt(), anyString())).thenReturn(null);
        when(mockedStrategy.remove(any())).thenReturn(null);
        when(mockedStrategy.clear()).thenAnswer(invocation -> null);
        when(mockedStrategy.size()).thenReturn(1);
        // Inject the mock via reflection
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(lruCache, mockedStrategy);
        // then – isEmpty should return whatever the mock reports
        assertFalse(lruCache.isEmpty(), "LRUCache.isEmpty() must propagate the result of the underlying strategy map");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyThrowsWhenStrategyIsNull(LRUCache.StrategyType strategy) throws Exception {
        // given – create a cache instance and then nullify its strategy field
        lruCache = new LRUCache<>(3, strategy);
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(lruCache, null);
        // when / then – calling isEmpty() should throw NPE
        assertThrows(NullPointerException.class, () -> lruCache.isEmpty(), "Calling isEmpty() with a null internal strategy should throw NullPointerException");
    }
}
