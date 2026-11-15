package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import org.mockito.Mock;
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class LRUCache_isEmpty_Test {

    /**
     * The cache instance used by the tests.
     */
    private LRUCache<Integer, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // The cache is (re)created inside each test method so that the
        // strategy type can be varied per‑parameterized execution.
        cache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure any background threads started by the threaded strategy are
        // terminated promptly. The cache implementation does not expose a
        // shutdown hook, but clearing it removes references and allows GC.
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Provides the two strategy types supported by {@code LRUCache}.
     */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Verifies that a newly created cache reports {@code true} for {@code isEmpty()},
     * becomes {@code false} after a put operation, and returns to {@code true}
     * after {@code clear()}.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyTransitions(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        // freshly constructed cache – should be empty
        assertTrue(cache.isEmpty(), "Cache should be empty immediately after construction");
        // after inserting an entry – should not be empty
        cache.put(1, "A");
        assertFalse(cache.isEmpty(), "Cache should not be empty after a put operation");
        // after clearing – should be empty again
        cache.clear();
        assertTrue(cache.isEmpty(), "Cache should be empty after clear()");
    }

    /**
     * Checks that {@code isEmpty()} behaves correctly when the cache contains
     * a {@code null} key and/or {@code null} value.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyWithNullKeyAndValue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        // cache is empty initially
        assertTrue(cache.isEmpty());
        // put a null key with a non‑null value
        cache.put(null, "value");
        assertFalse(cache.isEmpty(), "Cache should not be empty after inserting a null key");
        // clear and verify emptiness again
        cache.clear();
        assertTrue(cache.isEmpty());
        // put a null key with a null value
        cache.put(null, null);
        assertFalse(cache.isEmpty(), "Cache should not be empty after inserting null key/value");
        // final cleanup
        cache.clear();
        assertTrue(cache.isEmpty());
    }

    /**
     * After a put followed by a remove, the cache must become empty again.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyAfterRemove(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(42, "answer");
        assertFalse(cache.isEmpty(), "Cache should not be empty after put");
        cache.remove(42);
        assertTrue(cache.isEmpty(), "Cache should be empty after removing the only entry");
    }

    /**
     * Injects a pre‑populated underlying map via reflection and verifies that
     * {@code isEmpty()} reports {@code false}.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyWithPrepopulatedStrategy(LRUCache.StrategyType strategy) throws Exception {
        cache = new LRUCache<>(3, strategy);
        // Prepare a map with a single entry.
        Map<Integer, String> prepopulated = new HashMap<>();
        prepopulated.put(7, "seven");
        // Replace the private 'strategy' field with our map.
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(cache, prepopulated);
        assertFalse(cache.isEmpty(), "Cache should report not empty when underlying strategy map has entries");
    }

    /**
     * Simulates a {@code null} internal strategy map and expects a
     * {@link NullPointerException} to be propagated.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyThrowsWhenStrategyIsNull(LRUCache.StrategyType strategy) throws Exception {
        cache = new LRUCache<>(3, strategy);
        // Force the private 'strategy' field to null.
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(cache, null);
        assertThrows(NullPointerException.class, cache::isEmpty, "Calling isEmpty() with a null internal strategy should throw NullPointerException");
    }

    /**
     * Replaces the internal strategy with a mock that throws a
     * {@link RuntimeException} from {@code isEmpty()} and verifies that the
     * exception is propagated unchanged.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyPropagatesRuntimeException(LRUCache.StrategyType strategy) throws Exception {
        cache = new LRUCache<>(3, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = Mockito.mock(Map.class);
        Mockito.when(mockStrategy.isEmpty()).thenThrow(new RuntimeException("forced failure"));
        // Inject the mock into the cache.
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(cache, mockStrategy);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::isEmpty, "RuntimeException from the underlying strategy should be propagated");
        assertEquals("forced failure", ex.getMessage(), "Exception message should match the one thrown by the mock");
    }
}
