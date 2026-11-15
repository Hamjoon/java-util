package com.cedarsoftware.util;

import java.util.Collection;
import java.util.Arrays;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.cache.LockingLRUCacheStrategy;
import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_getCapacity_Test {

    private LRUCache<Integer, String> cache;

    private static final int CAPACITY = 5;

    /**
     * -----------------------------------------------------------------
     *  Subclass of ThreadedLRUCacheStrategy – ensure instanceof works.
     *  -----------------------------------------------------------------
     */
    static class MyThreadedStrategy extends ThreadedLRUCacheStrategy<Integer, String> {

        private final int capacity;

        MyThreadedStrategy(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public int getCapacity() {
            return capacity;
        }
    }

    /**
     * -----------------------------------------------------------------
     *  Subclass of LockingLRUCacheStrategy – ensure else‑branch handles it.
     *  -----------------------------------------------------------------
     */
    static class MyLockingStrategy extends LockingLRUCacheStrategy<Integer, String> {

        private final int capacity;

        MyLockingStrategy(int capacity) {
            this.capacity = capacity;
        }

        @Override
        public int getCapacity() {
            return capacity;
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache to avoid holding references between tests.
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * -----------------------------------------------------------------
     *  Parameter source – all supported strategy types
     *  -----------------------------------------------------------------
     */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * -----------------------------------------------------------------
     *  Helper to replace the private final 'strategy' field via reflection.
     *  -----------------------------------------------------------------
     */
    private static void injectStrategy(LRUCache<?, ?> target, Object strategy) {
        try {
            Field f = LRUCache.class.getDeclaredField("strategy");
            f.setAccessible(true);
            f.set(target, strategy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * -----------------------------------------------------------------
     *  Parameterized tests for various return values of Threaded strategy.
     *  -----------------------------------------------------------------
     */
    static Collection<Integer> threadedCapacities() {
        return Arrays.asList(0, Integer.MAX_VALUE);
    }

    /**
     * -----------------------------------------------------------------
     *  Parameterized tests for various return values of Locking strategy.
     *  -----------------------------------------------------------------
     */
    static Collection<Integer> lockingCapacities() {
        return Arrays.asList(0, -3);
    }

    /**
     * -----------------------------------------------------------------
     *  Test that getCapacity() returns the capacity supplied to the
     *  constructor for each strategy type.
     *  -----------------------------------------------------------------
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getCapacityMatchesConstructorArgument(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(CAPACITY, strategy);
        assertEquals(CAPACITY, cache.getCapacity(), "getCapacity() should return the value passed to the constructor for strategy " + strategy);
    }

    /**
     * -----------------------------------------------------------------
     *  Test the default (locking) constructor that does not require an
     *  explicit strategy enum.
     *  -----------------------------------------------------------------
     */
    @Test
    void getCapacityWithLockingOnlyConstructor() {
        // implicitly uses Locking strategy
        cache = new LRUCache<>(CAPACITY);
        assertEquals(CAPACITY, cache.getCapacity(), "getCapacity() should return the value passed to the locking‑only constructor");
    }

    /**
     * -----------------------------------------------------------------
     *  Test the threaded constructor that accepts a cleanup delay.
     *  This constructor always creates a ThreadedLRUCacheStrategy, so we
     *  verify that getCapacity() still reports the correct capacity.
     *  -----------------------------------------------------------------
     */
    @Test
    void getCapacityWithThreadedCleanupDelayConstructor() {
        // milliseconds – arbitrary non‑zero value
        int cleanupDelay = 20;
        cache = new LRUCache<>(CAPACITY, cleanupDelay);
        assertEquals(CAPACITY, cache.getCapacity(), "getCapacity() should return the capacity supplied to the threaded constructor with cleanup delay");
    }

    /**
     * -----------------------------------------------------------------
     *  Defensive test – ensure that an unsupported strategy enum throws
     *  the expected exception and that getCapacity() is never called in
     *  that case.
     *  -----------------------------------------------------------------
     */
    @Test
    void constructorRejectsUnsupportedStrategy() {
        // Create a dummy enum value that is not part of StrategyType.
        // Since StrategyType is an enum, we cannot instantiate a new value,
        // but we can simulate the failure path by passing null.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new LRUCache<>(CAPACITY, null));
        assertTrue(ex.getMessage().contains("Unsupported strategy type"));
    }

    /**
     * -----------------------------------------------------------------
     *  Branch: ThreadedLRUCacheStrategy – normal return value (10)
     *  -----------------------------------------------------------------
     */
    @Test
    void getCapacity_ThreadedStrategyReturns10() {
        // any ctor works
        cache = new LRUCache<>(CAPACITY, LRUCache.StrategyType.LOCKING);
        ThreadedLRUCacheStrategy<Integer, String> mocked = mock(ThreadedLRUCacheStrategy.class);
        when(mocked.getCapacity()).thenReturn(10);
        injectStrategy(cache, mocked);
        assertEquals(10, cache.getCapacity(), "When the internal strategy is ThreadedLRUCacheStrategy returning 10, getCapacity() should return 10");
    }

    /**
     * -----------------------------------------------------------------
     *  Branch: LockingLRUCacheStrategy – normal return value (5)
     *  -----------------------------------------------------------------
     */
    @Test
    void getCapacity_LockingStrategyReturns5() {
        cache = new LRUCache<>(CAPACITY, LRUCache.StrategyType.THREADED);
        LockingLRUCacheStrategy<Integer, String> mocked = mock(LockingLRUCacheStrategy.class);
        when(mocked.getCapacity()).thenReturn(5);
        injectStrategy(cache, mocked);
        assertEquals(5, cache.getCapacity(), "When the internal strategy is LockingLRUCacheStrategy returning 5, getCapacity() should return 5");
    }

    /**
     * -----------------------------------------------------------------
     *  Edge case: internal strategy is null → NullPointerException
     *  -----------------------------------------------------------------
     */
    @Test
    void getCapacity_nullStrategy_throwsNPE() {
        cache = new LRUCache<>(CAPACITY);
        injectStrategy(cache, null);
        assertThrows(NullPointerException.class, cache::getCapacity, "If the internal strategy field is null, getCapacity() should throw NullPointerException");
    }

    /**
     * -----------------------------------------------------------------
     *  Edge case: unexpected strategy type → ClassCastException
     *  -----------------------------------------------------------------
     */
    @Test
    void getCapacity_unexpectedStrategy_throwsClassCastException() {
        cache = new LRUCache<>(CAPACITY);
        // not a supported strategy
        injectStrategy(cache, new Object());
        assertThrows(ClassCastException.class, cache::getCapacity, "If the internal strategy is of an unexpected type, getCapacity() should throw ClassCastException");
    }

    @ParameterizedTest
    @MethodSource("threadedCapacities")
    void getCapacity_ThreadedStrategyVariousValues(int expected) {
        cache = new LRUCache<>(CAPACITY);
        ThreadedLRUCacheStrategy<Integer, String> mocked = mock(ThreadedLRUCacheStrategy.class);
        when(mocked.getCapacity()).thenReturn(expected);
        injectStrategy(cache, mocked);
        assertEquals(expected, cache.getCapacity(), "Threaded strategy returning " + expected + " should be propagated");
    }

    @ParameterizedTest
    @MethodSource("lockingCapacities")
    void getCapacity_LockingStrategyVariousValues(int expected) {
        cache = new LRUCache<>(CAPACITY);
        LockingLRUCacheStrategy<Integer, String> mocked = mock(LockingLRUCacheStrategy.class);
        when(mocked.getCapacity()).thenReturn(expected);
        injectStrategy(cache, mocked);
        assertEquals(expected, cache.getCapacity(), "Locking strategy returning " + expected + " should be propagated");
    }

    @Test
    void getCapacity_subclassOfThreadedStrategy() {
        cache = new LRUCache<>(CAPACITY);
        MyThreadedStrategy custom = new MyThreadedStrategy(42);
        injectStrategy(cache, custom);
        assertEquals(42, cache.getCapacity(), "Subclass of ThreadedLRUCacheStrategy returning 42 should be correctly handled");
    }

    @Test
    void getCapacity_subclassOfLockingStrategy() {
        cache = new LRUCache<>(CAPACITY);
        MyLockingStrategy custom = new MyLockingStrategy(7);
        injectStrategy(cache, custom);
        assertEquals(7, cache.getCapacity(), "Subclass of LockingLRUCacheStrategy returning 7 should be correctly handled");
    }

    /**
     * -----------------------------------------------------------------
     *  Propagation of RuntimeException from Threaded strategy.
     *  -----------------------------------------------------------------
     */
    @Test
    void getCapacity_ThreadedStrategyThrowsRuntimeException() {
        cache = new LRUCache<>(CAPACITY);
        ThreadedLRUCacheStrategy<Integer, String> mocked = mock(ThreadedLRUCacheStrategy.class);
        when(mocked.getCapacity()).thenThrow(new RuntimeException("boom"));
        injectStrategy(cache, mocked);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::getCapacity, "RuntimeException from ThreadedLRUCacheStrategy should propagate");
        assertEquals("boom", ex.getMessage());
    }

    /**
     * -----------------------------------------------------------------
     *  Propagation of RuntimeException from Locking strategy.
     *  -----------------------------------------------------------------
     */
    @Test
    void getCapacity_LockingStrategyThrowsRuntimeException() {
        cache = new LRUCache<>(CAPACITY);
        LockingLRUCacheStrategy<Integer, String> mocked = mock(LockingLRUCacheStrategy.class);
        when(mocked.getCapacity()).thenThrow(new RuntimeException("oops"));
        injectStrategy(cache, mocked);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::getCapacity, "RuntimeException from LockingLRUCacheStrategy should propagate");
        assertEquals("oops", ex.getMessage());
    }
}
