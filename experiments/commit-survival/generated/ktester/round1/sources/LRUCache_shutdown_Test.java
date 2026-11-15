package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_shutdown_Test {

    private LRUCache<Integer, String> cache;

    class MyThreadedStrategy extends ThreadedLRUCacheStrategy {
        // no extra behaviour needed for the test
    }

    class MyThreadedStrategy extends ThreadedLRUCacheStrategy {
        // no extra behaviour needed for the test
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache reference does not leak between tests
        cache = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // The real ThreadedLRUCacheStrategy.shutdown is a no‑op if no threads were started,
        // but we call it defensively in case other tests left background threads running.
        ThreadedLRUCacheStrategy.shutdown();
    }

    @Test
    void shutdownInvokesThreadedStrategyWhenStrategyIsThreaded() {
        // given – a cache created with the threaded strategy
        cache = new LRUCache<>(3, LRUCache.StrategyType.THREADED);
        // and – a mocked static view of ThreadedLRUCacheStrategy
        try (MockedStatic<ThreadedLRUCacheStrategy> mocked = mockStatic(ThreadedLRUCacheStrategy.class)) {
            // when
            cache.shutdown();
            // then – the static shutdown method must have been called exactly once
            mocked.verify(ThreadedLRUCacheStrategy::shutdown, times(1));
        }
    }

    @Test
    void shutdownDoesNotInvokeThreadedStrategyWhenStrategyIsLocking() {
        // given – a cache created with the locking strategy
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        // and – a mocked static view of ThreadedLRUCacheStrategy
        try (MockedStatic<ThreadedLRUCacheStrategy> mocked = mockStatic(ThreadedLRUCacheStrategy.class)) {
            // when
            cache.shutdown();
            // then – the static shutdown method must never be called
            mocked.verifyNoInteractions();
        }
    }

    @Test
    void shutdownDoesNotThrowWhenStrategyIsLockingAndNoThreadedStrategyPresent() {
        // given – a locking‑strategy cache
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        // when / then – calling shutdown should complete without exception
        assertDoesNotThrow(() -> cache.shutdown());
    }

    @Test
    void shutdownIsIdempotentForThreadedStrategy() {
        // given – a threaded‑strategy cache
        cache = new LRUCache<>(3, LRUCache.StrategyType.THREADED);
        try (MockedStatic<ThreadedLRUCacheStrategy> mocked = mockStatic(ThreadedLRUCacheStrategy.class)) {
            // when – shutdown called twice
            cache.shutdown();
            cache.shutdown();
            // then – the static shutdown method should be invoked twice (once per call)
            mocked.verify(ThreadedLRUCacheStrategy::shutdown, times(2));
        }
    }

    @Test
    void shutdownInvokesThreadedStrategyWhenStrategyIsSubclass() throws Exception {
        // given – a cache whose internal strategy will be replaced by a subclass of ThreadedLRUCacheStrategy
        // any strategy works; we'll overwrite it
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        // create a simple subclass
        class MyThreadedStrategy extends ThreadedLRUCacheStrategy {
            // no extra behaviour needed for the test
        }
        // replace the private 'strategy' field via reflection
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(cache, new MyThreadedStrategy());
        // mock static shutdown
        try (MockedStatic<ThreadedLRUCacheStrategy> mocked = mockStatic(ThreadedLRUCacheStrategy.class)) {
            // when
            cache.shutdown();
            // then – static shutdown should be called exactly once despite the concrete type being a subclass
            mocked.verify(ThreadedLRUCacheStrategy::shutdown, times(1));
        }
    }

    @Test
    void shutdownDoesNotModifyCacheContents() {
        // given – a locking‑strategy cache populated with data
        cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        int sizeBefore = cache.size();
        String v2Before = cache.get(2);
        // when – shutdown is called
        cache.shutdown();
        // then – cache state must be unchanged
        assertEquals(sizeBefore, cache.size(), "Cache size should remain the same after shutdown");
        assertEquals(v2Before, cache.get(2), "Existing entry should still be retrievable after shutdown");
        assertTrue(cache.containsKey(1) && cache.containsKey(3), "All original keys must still be present");
    }

    @Test
    void shutdownHandlesNullStrategyGracefully() throws Exception {
        // given – an LRUCache instance with its private 'strategy' field set to null via reflection
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        // force null strategy
        strategyField.set(cache, null);
        // when / then – shutdown must not throw NPE and must simply return
        assertDoesNotThrow(() -> cache.shutdown(), "shutdown() should tolerate a null strategy field");
    }
}
