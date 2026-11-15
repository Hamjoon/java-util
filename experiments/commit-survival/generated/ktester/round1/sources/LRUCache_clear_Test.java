package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class LRUCache_clear_Test {

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
        // Shut down the thread pool used by the threaded strategy (if any).
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    private static void injectStrategy(LRUCache<?, ?> target, Map<?, ?> strategy) throws Exception {
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(target, strategy);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clear_RemovesAllEntries_ForBothStrategies(LRUCache.StrategyType strategy) {
        // given: a cache with a few entries
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        assertEquals(3, cache.size(), "Pre‑condition: cache should contain three entries");
        assertTrue(cache.containsKey(1));
        assertTrue(cache.containsKey(2));
        assertTrue(cache.containsKey(3));
        // when: clear() is invoked
        cache.clear();
        // then: the cache reports being empty and contains no keys/values
        assertEquals(0, cache.size(), "Cache size must be zero after clear()");
        assertTrue(cache.isEmpty(), "Cache should be empty after clear()");
        assertNull(cache.get(1));
        assertNull(cache.get(2));
        assertNull(cache.get(3));
        assertFalse(cache.containsKey(1));
        assertFalse(cache.containsKey(2));
        assertFalse(cache.containsKey(3));
        // and when: a new entry is added after clear
        cache.put(4, "four");
        // then: the new entry is stored correctly
        assertEquals(1, cache.size());
        assertEquals("four", cache.get(4));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clear_OnEmptyCache_DoesNotThrow_ForBothStrategies(LRUCache.StrategyType strategy) {
        // given: an empty cache
        cache = new LRUCache<>(3, strategy);
        assertTrue(cache.isEmpty());
        // when/then: calling clear on an empty cache should not throw and remain empty
        assertDoesNotThrow(() -> cache.clear());
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
    }

    @Test
    void clear_DelegatesToStrategyExactlyOnce() throws Exception {
        // Create a real cache and replace its internal strategy with a mock.
        cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        injectStrategy(cache, mockStrategy);
        // when
        cache.clear();
        // then: the mock's clear method must be called exactly once.
        verify(mockStrategy, times(1)).clear();
    }

    @Test
    void clear_PropagatesRuntimeException_FromStrategy() throws Exception {
        cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        doThrow(new RuntimeException("boom")).when(mockStrategy).clear();
        injectStrategy(cache, mockStrategy);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.clear());
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void clear_PropagatesIllegalStateException_FromStrategy() throws Exception {
        cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        doThrow(new IllegalStateException("illegal state")).when(mockStrategy).clear();
        injectStrategy(cache, mockStrategy);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cache.clear());
        assertEquals("illegal state", ex.getMessage());
    }

    @Test
    void clear_PropagatesUnsupportedOperationException_FromStrategy() throws Exception {
        cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        // Use an unmodifiable map as the strategy – its clear() throws UOE.
        Map<Integer, String> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        injectStrategy(cache, unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> cache.clear());
    }

    @Test
    void clear_PropagatesError_FromStrategy() throws Exception {
        cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        doThrow(new AssertionError("assertion error")).when(mockStrategy).clear();
        injectStrategy(cache, mockStrategy);
        AssertionError err = assertThrows(AssertionError.class, () -> cache.clear());
        assertEquals("assertion error", err.getMessage());
    }

    @Test
    void clear_ThrowsNullPointerException_WhenStrategyIsNull() throws Exception {
        cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        // Force the private field to null.
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.clear());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clear_AfterSeriesOfPutAndRemove_LeavesCacheEmpty(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.remove(1);
        cache.remove(2);
        assertTrue(cache.isEmpty());
        // clear on already‑empty cache – should stay empty and not throw.
        assertDoesNotThrow(() -> cache.clear());
        assertTrue(cache.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clear_WhenCacheAtFullCapacity_EmptiesCache(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(2, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        assertEquals(2, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clear_RemovesEntryWithNullKey(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(null, "nullKey");
        assertEquals(1, cache.size());
        assertTrue(cache.containsKey(null));
        cache.clear();
        assertEquals(0, cache.size());
        assertFalse(cache.containsKey(null));
        assertNull(cache.get(null));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clear_RemovesEntriesWithNullValues(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, null);
        cache.put(2, null);
        assertEquals(2, cache.size());
        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get(1));
        assertNull(cache.get(2));
    }

    @Test
    void clear_ConcurrentInvocations_DoNotCorruptState() throws Exception {
        cache = new LRUCache<>(100, LRUCache.StrategyType.THREADED);
        // Populate cache with some data.
        for (int i = 0; i < 50; i++) {
            cache.put(i, "v" + i);
        }
        assertEquals(50, cache.size());
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger exceptions = new AtomicInteger(0);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // synchronize start
                    startLatch.await();
                    cache.clear();
                } catch (Throwable t) {
                    exceptions.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Release all threads at once.
        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdownNow();
        // No thread should have thrown an exception.
        assertEquals(0, exceptions.get(), "No thread should see an exception during clear()");
        // After all clears, the cache must be empty.
        assertEquals(0, cache.size());
        assertTrue(cache.isEmpty());
    }
}
