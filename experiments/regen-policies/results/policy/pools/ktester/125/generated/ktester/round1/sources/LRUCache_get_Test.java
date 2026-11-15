package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_get_Test {

    private LRUCache<Integer, String> cache;

    /**
     * Simple custom key class with proper equals/hashCode implementation.
     */
    private static final class CustomKey {

        private final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /**
     * Create a fresh cache before each test case.
     */
    @BeforeEach
    void setupBeforeEach() {
        // The concrete cache instance is created inside each test method
        // because the strategy type is supplied by the parameterized test.
        cache = null;
    }

    /**
     * No per‑test clean‑up required – the cache instance will be eligible for GC.
     */
    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            // ensure any background thread (threaded strategy) stops
            cache.shutdown();
        }
        cache = null;
    }

    /**
     * Shut down the shared thread pool used by the threaded strategy.
     */
    @AfterAll
    static void teardownAfterAll() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * All strategies that the cache supports.
     */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Uses reflection to replace the private {@code strategy} field of the cache with a supplied map.
     *
     * @param cache   the LRUCache instance whose delegate is to be replaced
     * @param newMap  the mock or custom map to inject
     */
    private static void injectStrategy(LRUCache<?, ?> cache, Map<?, ?> newMap) {
        try {
            Field f = LRUCache.class.getDeclaredField("strategy");
            f.setAccessible(true);
            f.set(cache, newMap);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to inject mock strategy", e);
        }
    }

    /**
     * Simple happy‑path: put a value and retrieve it with get().
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getReturnsPreviouslyPutValue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        assertEquals("one", cache.get(1), "get(1) should return the value that was put");
        assertEquals("two", cache.get(2), "get(2) should return the value that was put");
    }

    /**
     * When a key is absent, get() must return null.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getReturnsNullForMissingKey(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        assertNull(cache.get(99), "get() for a non‑existent key must return null");
    }

    /**
     * The cache supports null keys – get() should retrieve the associated value.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getWorksWithNullKey(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(null, "nullValue");
        assertEquals("nullValue", cache.get(null), "get(null) should return the value stored for a null key");
    }

    /**
     * Verify that get() does not interfere with the eviction policy.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getDoesNotPreventEvictionWhenCapacityExceeded(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        // Access key 1 to make it recently used
        assertEquals("A", cache.get(1));
        // Adding a fourth entry should evict the least‑recently used (key 2)
        cache.put(4, "D");
        // Because the threaded strategy evicts asynchronously, we wait a short while.
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (!cache.containsKey(2)) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
        assertNull(cache.get(2), "Key 2 should have been evicted");
        assertEquals("A", cache.get(1), "Key 1 should still be present after eviction");
        assertEquals("D", cache.get(4), "Key 4 should be present after insertion");
    }

    /**
     * Delegation verification – the underlying map’s get() must be called exactly once.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getDelegatesToUnderlyingStrategyExactlyOnce(LRUCache.StrategyType strategy) {
        // Create a mock Map and inject it via reflection
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.get(42)).thenReturn("delegated");
        cache = new LRUCache<>(5, strategy);
        injectStrategy(cache, mockMap);
        String result = cache.get(42);
        assertEquals("delegated", result, "Result must be the value returned by the mock strategy");
        // Verify delegation semantics
        verify(mockMap, times(1)).get(42);
        verifyNoMoreInteractions(mockMap);
    }

    /**
     * When the underlying strategy contains the key, get() returns the associated value.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getReturnsValueWhenKeyExistsInStrategy(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(100, "existingValue");
        assertEquals("existingValue", cache.get(100), "Should return the value stored for the existing key");
    }

    /**
     * When the underlying strategy does not contain the key, get() returns null.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getReturnsNullWhenKeyMissingInStrategy(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "value");
        assertNull(cache.get(999), "Missing key must result in null");
    }

    /**
     * When a null key is supplied and the key is not present, get() returns null.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getReturnsNullForAbsentNullKey(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "value");
        assertNull(cache.get(null), "Absent null key must return null");
    }

    /**
     * If the underlying strategy throws a RuntimeException, it propagates unchanged.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getPropagatesRuntimeExceptionFromStrategy(LRUCache.StrategyType strategy) {
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.get(777)).thenThrow(new RuntimeException("boom"));
        cache = new LRUCache<>(5, strategy);
        injectStrategy(cache, mockMap);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.get(777));
        assertEquals("boom", ex.getMessage());
    }

    /**
     * The underlying strategy contains the key but maps it to null – get() returns null.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getReturnsNullWhenStrategyMapsKeyToNull(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        // explicit null value
        cache.put(200, null);
        assertNull(cache.get(200), "Key present with null value must return null");
    }

    /**
     * Equality semantics – a different key instance that is equal must retrieve the value.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getUsesKeyEqualityNotIdentity(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        String keyA = new String("equalKey");
        // different object, same content
        String keyB = new String("equalKey");
        cache.put(keyA, "equalValue");
        assertEquals("equalValue", cache.get(keyB), "Equality (equals()) should be used for key lookup");
    }

    /**
     * Custom key object with overridden equals()/hashCode() works as expected.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getWorksWithCustomKeyObject(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        CustomKey key1 = new CustomKey(1);
        // equal to key1
        CustomKey key2 = new CustomKey(1);
        cache.put(key1, "customValue");
        assertEquals("customValue", cache.get(key2), "Custom key equality must be honoured");
    }

    /**
     * Supplying a key of the wrong type causes a ClassCastException to propagate.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getPropagatesClassCastExceptionWhenKeyTypeMismatch(LRUCache.StrategyType strategy) {
        // Map that only accepts String keys – we simulate this with a mock.
        @SuppressWarnings("unchecked")
        Map<String, String> mockMap = mock(Map.class);
        when(mockMap.get(any())).thenAnswer(invocation -> {
            Object key = invocation.getArgument(0);
            if (!(key instanceof String)) {
                throw new ClassCastException("Expected String key");
            }
            return null;
        });
        // LRUCache is typed with Integer keys, but we inject a Map<String, String>
        cache = new LRUCache<>(5, strategy);
        injectStrategy(cache, mockMap);
        assertThrows(ClassCastException.class, () -> cache.get(123));
    }

    /**
     * Null underlying strategy leads to NullPointerException on get().
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getThrowsNPEWhenUnderlyingStrategyIsNull(LRUCache.StrategyType strategy) throws Exception {
        cache = new LRUCache<>(5, strategy);
        // Force the private 'strategy' field to null via reflection
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.get("anyKey"));
    }

    /**
     * Underlying map throws NullPointerException for a specific key – it must propagate.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getPropagatesNPEFromUnderlyingMap(LRUCache.StrategyType strategy) {
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.get(eq(999))).thenThrow(new NullPointerException("triggered NPE"));
        // other keys normal
        when(mockMap.get(anyInt())).thenReturn(null);
        cache = new LRUCache<>(5, strategy);
        injectStrategy(cache, mockMap);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> cache.get(999));
        assertEquals("triggered NPE", npe.getMessage());
    }

    /**
     * After shutdown, the underlying strategy throws IllegalStateException – get() must propagate it.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void getAfterShutdownPropagatesIllegalStateException(LRUCache.StrategyType strategy) {
        // A mock strategy that throws after shutdown flag is set
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        AtomicBoolean shutdown = new AtomicBoolean(false);
        when(mockMap.get(anyInt())).thenAnswer(invocation -> {
            if (shutdown.get()) {
                throw new IllegalStateException("Cache is shut down");
            }
            return "valueBeforeShutdown";
        });
        cache = new LRUCache<>(5, strategy);
        injectStrategy(cache, mockMap);
        // Verify normal operation before shutdown
        assertEquals("valueBeforeShutdown", cache.get(1));
        // Simulate shutdown
        shutdown.set(true);
        cache.shutdown();
        IllegalStateException ise = assertThrows(IllegalStateException.class, () -> cache.get(1));
        assertEquals("Cache is shut down", ise.getMessage());
    }

    /**
     * Concurrency safety for ThreadedLRUCacheStrategy – many threads calling get() concurrently.
     */
    @Test
    void getConcurrentAccessWithThreadedStrategy() throws Exception {
        cache = new LRUCache<>(100, LRUCache.StrategyType.THREADED);
        // Populate cache with a known entry
        cache.put(42, "concurrentValue");
        int threadCount = 20;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> cache.get(42));
        }
        List<Future<String>> results = exec.invokeAll(tasks);
        exec.shutdown();
        assertTrue(exec.awaitTermination(2, TimeUnit.SECONDS), "Executor did not terminate in time");
        for (Future<String> f : results) {
            assertEquals("concurrentValue", f.get(), "All concurrent get() calls must return the stored value");
        }
    }
}
