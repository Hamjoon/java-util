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
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_size_Test {

    private LRUCache<Integer, String> lruCache;

    @BeforeEach
    void setupBeforeEach() {
        // The concrete cache instance is created inside each parameterized test
        // because the strategy type varies per test case.
        lruCache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared between tests to avoid cross‑test contamination.
        if (lruCache != null) {
            lruCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the thread pool used by the threaded strategy, if any.
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testSizeReflectsNumberOfEntries(LRUCache.StrategyType strategy) {
        // given: three distinct entries
        lruCache = new LRUCache<>(3, strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        // then
        assertEquals(3, lruCache.size(), "Size should reflect the number of inserted entries");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testSizeZeroWhenEmpty(LRUCache.StrategyType strategy) {
        // given: a freshly created cache
        lruCache = new LRUCache<>(3, strategy);
        // then
        assertEquals(0, lruCache.size(), "Newly created cache must have size 0");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testSizeAfterClear(LRUCache.StrategyType strategy) {
        // given: a populated cache
        lruCache = new LRUCache<>(3, strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        assertEquals(3, lruCache.size(), "Pre‑condition check");
        // when
        lruCache.clear();
        // then
        assertEquals(0, lruCache.size(), "Size must be 0 after clear()");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testSizeAfterEviction(LRUCache.StrategyType strategy) {
        // given a cache with capacity 3
        lruCache = new LRUCache<>(3, strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        // access key 1 to make it most‑recently used
        lruCache.get(1);
        // put a fourth entry, triggering eviction of the least‑recently used (key 2)
        lruCache.put(4, "D");
        // wait a short moment for the threaded strategy to perform cleanup
        if (strategy == LRUCache.StrategyType.THREADED) {
            try {
                // give the background thread time to evict
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        // then
        assertEquals(3, lruCache.size(), "Cache size should stay equal to its capacity after eviction");
        assertTrue(lruCache.containsKey(1));
        assertTrue(lruCache.containsKey(3));
        assertTrue(lruCache.containsKey(4));
        assertNull(lruCache.get(2), "Key 2 should have been evicted");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testSizeAfterRemoval(LRUCache.StrategyType strategy) {
        // given: three entries, then two removals
        lruCache = new LRUCache<>(3, strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        lruCache.remove(2);
        lruCache.remove(3);
        // then
        assertEquals(1, lruCache.size(), "Size must reflect the remaining single entry after removals");
        assertTrue(lruCache.containsKey(1));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testSizeAfterShutdown(LRUCache.StrategyType strategy) {
        // given: a populated cache
        lruCache = new LRUCache<>(3, strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        // when
        lruCache.shutdown();
        // then: according to the specification, size() should now throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> lruCache.size(), "After shutdown(), size() must throw IllegalStateException");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testSizeAfterDuplicateKeyInsertion(LRUCache.StrategyType strategy) {
        // given: two distinct keys
        lruCache = new LRUCache<>(3, strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        // when: inserting a duplicate key (key 1) with a new value
        lruCache.put(1, "A-modified");
        // then: size must stay unchanged (2)
        assertEquals(2, lruCache.size(), "Duplicate key insertion must not increase the size");
        assertEquals("A-modified", lruCache.get(1), "Value for the duplicated key should be updated");
    }

    @Test
    void testSizeWhenInternalStrategyIsNull() throws Exception {
        // given: a normal cache
        lruCache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        // use reflection to nullify the private 'strategy' field
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(lruCache, null);
        // then: invoking size() must throw NullPointerException
        assertThrows(NullPointerException.class, () -> lruCache.size(), "Calling size() with a null internal strategy should throw NPE");
    }

    @Test
    void testSizePropagatesUnsupportedOperationException(@Mock Map<Integer, String> mockMap) throws Exception {
        // given: a mocked strategy that throws on size()
        when(mockMap.size()).thenThrow(new UnsupportedOperationException("size not supported"));
        lruCache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        // inject the mock via reflection
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(lruCache, mockMap);
        // then: size() should propagate the UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> lruCache.size(), "size() must propagate UnsupportedOperationException from the underlying map");
    }

    @Test
    void testSizeAfterShutdownThrowsIllegalStateException() {
        // given: a cache that has been shut down
        lruCache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        lruCache.put(1, "A");
        lruCache.shutdown();
        // then: subsequent size() call must throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> lruCache.size(), "size() after shutdown() should throw IllegalStateException");
    }
}
