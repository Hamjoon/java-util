package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_shutdown_Test {

    private LRUCache<Integer, String> cache;

    private MockedStatic<ThreadedLRUCacheStrategy> mockedThreadedStrategy;

    @BeforeEach
    void setupBeforeEach() {
        // Create a static mock for ThreadedLRUCacheStrategy so we can verify the shutdown call
        mockedThreadedStrategy = mockStatic(ThreadedLRUCacheStrategy.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Close the static mock to avoid leaking it into other tests
        if (mockedThreadedStrategy != null) {
            mockedThreadedStrategy.close();
        }
    }

    @Test
    void shutdownInvokesThreadedStrategyWhenThreaded() {
        // given: a cache created with the THREADED strategy
        cache = new LRUCache<>(3, LRUCache.StrategyType.THREADED);
        // when: shutdown is called
        cache.shutdown();
        // then: the static shutdown method of ThreadedLRUCacheStrategy should be invoked exactly once
        mockedThreadedStrategy.verify(() -> ThreadedLRUCacheStrategy.shutdown(), times(1));
        mockedThreadedStrategy.verifyNoMoreInteractions();
    }

    @Test
    void shutdownDoesNotInvokeThreadedStrategyWhenLocking() {
        // given: a cache created with the LOCKING strategy
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        // when: shutdown is called
        cache.shutdown();
        // then: the static shutdown method of ThreadedLRUCacheStrategy should NOT be invoked
        mockedThreadedStrategy.verifyNoInteractions();
    }

    /**
     * Verify that each call to {@code shutdown()} triggers a static shutdown
     * on {@code ThreadedLRUCacheStrategy}.  The test is parameterized to
     * exercise 1, 2 and 5 consecutive calls.
     */
    @ParameterizedTest
    @ValueSource(ints = { 1, 2, 5 })
    void shutdownInvokesThreadedStrategyOncePerCall(int callCount) {
        // given: a cache using the threaded strategy
        cache = new LRUCache<>(3, LRUCache.StrategyType.THREADED);
        // when: shutdown is invoked the specified number of times
        for (int i = 0; i < callCount; i++) {
            cache.shutdown();
        }
        // then: the static shutdown method should have been called exactly 'callCount' times
        mockedThreadedStrategy.verify(() -> ThreadedLRUCacheStrategy.shutdown(), times(callCount));
        mockedThreadedStrategy.verifyNoMoreInteractions();
    }

    /**
     * Ensure that the {@code instanceof} check also succeeds when the
     * underlying strategy is a subclass of {@code ThreadedLRUCacheStrategy}.
     * We replace the private {@code strategy} field via reflection with a
     * Mockito mock that extends {@code ThreadedLRUCacheStrategy}.
     */
    @Test
    void shutdownInvokesThreadedStrategyWhenStrategyIsSubclass() throws Exception {
        // given: a cache initially created with the locking strategy
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        // create a mock that is a subclass of ThreadedLRUCacheStrategy
        ThreadedLRUCacheStrategy subclassMock = mock(ThreadedLRUCacheStrategy.class);
        // inject the mock into the private 'strategy' field
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(cache, subclassMock);
        // when: shutdown is called
        cache.shutdown();
        // then: the static shutdown method should be invoked once
        mockedThreadedStrategy.verify(() -> ThreadedLRUCacheStrategy.shutdown(), times(1));
        mockedThreadedStrategy.verifyNoMoreInteractions();
    }

    /**
     * Verify that an exception thrown by {@code ThreadedLRUCacheStrategy.shutdown()}
     * propagates out of {@code LRUCache.shutdown()} unchanged.
     */
    @Test
    void shutdownPropagatesExceptionFromThreadedStrategy() {
        // given: a cache using the threaded strategy
        cache = new LRUCache<>(3, LRUCache.StrategyType.THREADED);
        // configure the static mock to throw a RuntimeException
        RuntimeException expected = new RuntimeException("shutdown failed");
        mockedThreadedStrategy.when(ThreadedLRUCacheStrategy::shutdown).thenThrow(expected);
        // when/then: calling shutdown should rethrow the same exception
        RuntimeException actual = assertThrows(RuntimeException.class, () -> cache.shutdown());
        assertSame(expected, actual, "The thrown exception should be the one from ThreadedLRUCacheStrategy.shutdown()");
        // verify that the static method was indeed called once before the exception propagated
        mockedThreadedStrategy.verify(() -> ThreadedLRUCacheStrategy.shutdown(), times(1));
    }
}
