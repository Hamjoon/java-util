package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class LRUCache_isEmpty_Test {

    private LRUCache<Integer, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // Use a small capacity; the strategy type will be supplied by the
        // parameterized test. For the non‑parameterized tests we default to
        // the locking strategy.
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that one test does not affect another.
        if (cache != null) {
            cache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // The threaded strategy creates a background executor that must be
        // shut down to avoid thread‑leak warnings.
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Stream<LRUCache.StrategyType> strategies() {
        return Stream.of(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    @Test
    void testIsEmptyWhenNewCache() {
        assertTrue(cache.isEmpty(), "A newly created cache should be empty");
    }

    @Test
    void testIsEmptyAfterPut() {
        cache.put(1, "one");
        assertFalse(cache.isEmpty(), "Cache should not be empty after a put");
    }

    @Test
    void testIsEmptyAfterClear() {
        cache.put(1, "one");
        cache.put(2, "two");
        assertFalse(cache.isEmpty(), "Cache should not be empty before clear");
        cache.clear();
        assertTrue(cache.isEmpty(), "Cache should be empty after clear");
    }

    @Test
    void testIsEmptyAfterRemovingLastEntry() {
        cache.put(42, "answer");
        assertFalse(cache.isEmpty(), "Cache should contain the entry just added");
        cache.remove(42);
        assertTrue(cache.isEmpty(), "Cache should be empty after removing the only entry");
    }

    @Test
    void testIsEmptyWithNullKeyAndNullValue() {
        cache.put(null, null);
        assertFalse(cache.isEmpty(), "Cache should not be empty when a null key/value pair is stored");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testIsEmptyAcrossStrategies(LRUCache.StrategyType strategy) {
        // Re‑create the cache with the supplied strategy
        cache = new LRUCache<>(3, strategy);
        // 1. Empty cache
        assertTrue(cache.isEmpty(), "Cache should start empty for strategy " + strategy);
        // 2. After a put
        cache.put(1, "value");
        assertFalse(cache.isEmpty(), "Cache should report non‑empty after a put for strategy " + strategy);
        // 3. After clear
        cache.clear();
        assertTrue(cache.isEmpty(), "Cache should be empty after clear for strategy " + strategy);
    }

    /**
     * Verifies that {@code isEmpty()} throws a {@link NullPointerException}
     * when the internal {@code strategy} map reference is {@code null}.
     *
     * The JSON test case specifies this edge condition.  Because {@code strategy}
     * is a private final field, we use reflection to replace its value.
     */
    @Test
    void testIsEmptyWhenStrategyNull() throws Exception {
        // Use reflection to set the private field 'strategy' to null.
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(cache, null);
        // The contract is that a NullPointerException propagates.
        assertThrows(NullPointerException.class, () -> cache.isEmpty(), "Calling isEmpty() with a null internal strategy should throw NullPointerException");
    }
}
