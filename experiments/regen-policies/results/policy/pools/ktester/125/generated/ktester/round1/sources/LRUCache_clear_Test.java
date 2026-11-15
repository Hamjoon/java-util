package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.cache.LockingLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.mockito.Mockito.*;

// Mockito not used here but kept per template
@ExtendWith(MockitoExtension.class)
class LRUCache_clear_Test {

    private LRUCache<Integer, String> lruCache;

    @BeforeEach
    void setupBeforeEach() {
        // The actual cache instance is created per‑test in the parameterized method.
        lruCache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared after each test to avoid cross‑test contamination.
        if (lruCache != null) {
            lruCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the background thread pool used by the threaded strategy.
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Replaces the private {@code strategy} field of the given {@link LRUCache}
     * instance with the supplied mock.  This allows us to verify delegation
     * without relying on the concrete strategy implementations.
     *
     * @param cache   the cache whose strategy should be replaced
     * @param mock    the mock {@link Map} to inject
     * @throws Exception if reflection fails
     */
    private static void injectStrategy(LRUCache<?, ?> cache, Map<?, ?> mock) throws Exception {
        Field field = LRUCache.class.getDeclaredField("strategy");
        field.setAccessible(true);
        field.set(cache, mock);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clearRemovesAllEntries(LRUCache.StrategyType strategy) {
        // GIVEN: a cache with a few entries
        lruCache = new LRUCache<>(5, strategy);
        lruCache.put(1, "one");
        lruCache.put(2, "two");
        lruCache.put(3, "three");
        assertEquals(3, lruCache.size(), "Pre‑condition: cache should contain 3 entries");
        assertTrue(lruCache.containsKey(1));
        assertTrue(lruCache.containsKey(2));
        assertTrue(lruCache.containsKey(3));
        // WHEN: clear() is invoked
        lruCache.clear();
        // THEN: the cache is empty and behaves as a fresh instance
        assertEquals(0, lruCache.size(), "Cache should be empty after clear()");
        assertFalse(lruCache.containsKey(1));
        assertFalse(lruCache.containsKey(2));
        assertFalse(lruCache.containsKey(3));
        assertNull(lruCache.get(1));
        assertNull(lruCache.get(2));
        assertNull(lruCache.get(3));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clearOnEmptyCacheDoesNotThrow(LRUCache.StrategyType strategy) {
        // GIVEN: an empty cache
        lruCache = new LRUCache<>(5, strategy);
        assertEquals(0, lruCache.size());
        // WHEN / THEN: calling clear() should not throw and the cache stays empty
        assertDoesNotThrow(() -> lruCache.clear());
        assertEquals(0, lruCache.size());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clearAllowsSubsequentPutAndGet(LRUCache.StrategyType strategy) {
        // GIVEN: a cache filled and then cleared
        lruCache = new LRUCache<>(3, strategy);
        lruCache.put(10, "ten");
        lruCache.put(20, "twenty");
        assertEquals(2, lruCache.size());
        lruCache.clear();
        assertEquals(0, lruCache.size());
        // WHEN: new entries are added after clear
        lruCache.put(30, "thirty");
        lruCache.put(40, "forty");
        // THEN: they are retrievable as expected
        assertEquals(2, lruCache.size());
        assertEquals("thirty", lruCache.get(30));
        assertEquals("forty", lruCache.get(40));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clearDelegatesToStrategyAndInvokesExactlyOnce(LRUCache.StrategyType strategy) throws Exception {
        // Create a real cache, then replace its private 'strategy' field with a mock.
        lruCache = new LRUCache<>(5, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        injectStrategy(lruCache, mockStrategy);
        // Populate via the public API (delegates to the mock).
        lruCache.put(1, "one");
        lruCache.put(2, "two");
        // WHEN
        lruCache.clear();
        // THEN – verify that the mock's clear() was called exactly once.
        verify(mockStrategy, times(1)).clear();
        // The public view should also be empty.
        assertEquals(0, lruCache.size());
        assertTrue(lruCache.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clearOnEmptyCacheStillDelegatesToStrategy(LRUCache.StrategyType strategy) throws Exception {
        lruCache = new LRUCache<>(5, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        injectStrategy(lruCache, mockStrategy);
        // WHEN
        lruCache.clear();
        // THEN
        verify(mockStrategy, times(1)).clear();
        assertEquals(0, lruCache.size());
        assertTrue(lruCache.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clearAfterShutdownWorksNormally(LRUCache.StrategyType strategy) {
        lruCache = new LRUCache<>(5, strategy);
        lruCache.put(1, "one");
        lruCache.put(2, "two");
        assertEquals(2, lruCache.size());
        // Shut down any background resources.
        lruCache.shutdown();
        // clear should still succeed.
        assertDoesNotThrow(() -> lruCache.clear());
        assertEquals(0, lruCache.size());
        assertTrue(lruCache.isEmpty());
        // Subsequent puts must work.
        lruCache.put(3, "three");
        assertEquals(1, lruCache.size());
        assertEquals("three", lruCache.get(3));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clearAfterEvictionStillDelegatesOnce(LRUCache.StrategyType strategy) throws Exception {
        // Small capacity to force eviction.
        lruCache = new LRUCache<>(2, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        injectStrategy(lruCache, mockStrategy);
        // Insert three entries – the first should be evicted by the strategy.
        lruCache.put(1, "one");
        lruCache.put(2, "two");
        // triggers eviction internally
        lruCache.put(3, "three");
        // WHEN
        lruCache.clear();
        // THEN
        verify(mockStrategy, times(1)).clear();
        assertEquals(0, lruCache.size());
        assertTrue(lruCache.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void clearWithConcurrentAccessDoesNotThrow(LRUCache.StrategyType strategy) throws Exception {
        lruCache = new LRUCache<>(100, strategy);
        ExecutorService exec = Executors.newFixedThreadPool(4);
        AtomicBoolean clearCalled = new AtomicBoolean(false);
        // Task that continuously puts/gets.
        Runnable writerReader = () -> {
            for (int i = 0; i < 1000 && !clearCalled.get(); i++) {
                lruCache.put(i, "v" + i);
                lruCache.get(i);
            }
        };
        // Task that invokes clear once after a short delay.
        Runnable clearer = () -> {
            try {
                Thread.sleep(50);
                lruCache.clear();
                clearCalled.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        // Submit tasks.
        List<Future<?>> futures = new ArrayList<>();
        futures.add(exec.submit(writerReader));
        futures.add(exec.submit(writerReader));
        futures.add(exec.submit(writerReader));
        futures.add(exec.submit(clearer));
        // Wait for all to finish.
        for (Future<?> f : futures) {
            // propagate any exception
            f.get();
        }
        exec.shutdownNow();
        // After clear, cache must be empty and no ConcurrentModificationException should have bubbled up.
        assertEquals(0, lruCache.size());
        assertTrue(lruCache.isEmpty());
    }

    @Test
    void clearPropagatesRuntimeExceptionFromStrategy() throws Exception {
        lruCache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        doThrow(new RuntimeException("boom")).when(mockStrategy).clear();
        injectStrategy(lruCache, mockStrategy);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> lruCache.clear());
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void clearPropagatesUnsupportedOperationExceptionFromStrategy() throws Exception {
        lruCache = new LRUCache<>(5, LRUCache.StrategyType.THREADED);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        doThrow(new UnsupportedOperationException("not supported")).when(mockStrategy).clear();
        injectStrategy(lruCache, mockStrategy);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> lruCache.clear());
        assertEquals("not supported", ex.getMessage());
    }

    @Test
    void clearPropagatesIllegalStateExceptionAfterShutdown() throws Exception {
        lruCache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        // After shutdown, we want clear() to throw IllegalStateException.
        doAnswer(invocation -> {
            throw new IllegalStateException("already shut down");
        }).when(mockStrategy).clear();
        injectStrategy(lruCache, mockStrategy);
        // Simulate shutdown – the real shutdown does not affect the mock, but we call it for completeness.
        lruCache.shutdown();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> lruCache.clear());
        assertEquals("already shut down", ex.getMessage());
    }

    @Test
    void clearWithNullUnderlyingStrategyThrowsNPE() throws Exception {
        // Construct an LRUCache via reflection that has a null 'strategy' field.
        lruCache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        // Force the private field to null.
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(lruCache, null);
        assertThrows(NullPointerException.class, () -> lruCache.clear());
    }
}
