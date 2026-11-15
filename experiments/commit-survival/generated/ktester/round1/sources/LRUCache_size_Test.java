package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class LRUCache_size_Test {

    private LRUCache<Integer, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // The concrete cache instance is created in each test (via setUpCache).
        // Keeping the field null here avoids accidental reuse between tests.
        cache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            // Gracefully stop any background cleanup thread.
            cache.shutdown();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Ensure the static thread‑pool used by the threaded strategy is shut down.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /* --------------------------------------------------------------------- */
    static Stream<LRUCache.StrategyType> strategies() {
        return Arrays.stream(new LRUCache.StrategyType[] { LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED });
    }

    /* --------------------------------------------------------------------- */
    private void setUpCache(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void sizeReflectsNumberOfEntries(LRUCache.StrategyType strategy) {
        // given
        setUpCache(strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        // then
        assertEquals(2, cache.size(), "Size should reflect the number of inserted entries");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void sizeIsZeroWhenCacheIsEmpty(LRUCache.StrategyType strategy) {
        // given
        setUpCache(strategy);
        // then
        assertEquals(0, cache.size(), "Newly created cache must have size 0");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void sizeUpdatesAfterRemoval(LRUCache.StrategyType strategy) {
        // given
        setUpCache(strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        assertEquals(3, cache.size());
        // when
        cache.remove(2);
        // then
        assertEquals(2, cache.size(), "Size should decrement after a removal");
        assertNull(cache.get(2), "Removed key must no longer be present");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void sizeResetsAfterClear(LRUCache.StrategyType strategy) {
        // given
        setUpCache(strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        assertEquals(3, cache.size());
        // when
        cache.clear();
        // then
        assertEquals(0, cache.size(), "Size must be 0 after clear()");
        assertTrue(cache.isEmpty(), "Cache should report empty after clear()");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void sizeCountsNullValues(LRUCache.StrategyType strategy) {
        // given
        setUpCache(strategy);
        cache.put(1, null);
        cache.put(2, "two");
        // then
        assertEquals(2, cache.size(), "Entries with null values are still counted");
    }

    /**
     * Verify that a single insertion results in size 1.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeAfterSingleEntry(LRUCache.StrategyType strategy) {
        setUpCache(strategy);
        cache.put(42, "answer");
        assertEquals(1, cache.size(), "Cache should contain exactly one entry after a single put()");
    }

    /**
     * Insert more items than the capacity and ensure the size never exceeds the capacity.
     * For the threaded strategy we give the cleanup thread a short moment to run.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeAfterExceedingCapacity(LRUCache.StrategyType strategy) throws InterruptedException {
        setUpCache(strategy);
        // capacity is 3; insert 5 distinct entries
        for (int i = 1; i <= 5; i++) {
            cache.put(i, "v" + i);
        }
        // For the threaded strategy the eviction happens asynchronously.
        if (strategy == LRUCache.StrategyType.THREADED) {
            // give the background thread time to trim the cache
            Thread.sleep(200);
        }
        assertEquals(3, cache.size(), "Cache size must be capped at its capacity (3) after inserting 5 items");
    }

    /**
     * putAll with more entries than the capacity should also respect the LRU limit.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeAfterPutAllExceedingCapacity(LRUCache.StrategyType strategy) throws InterruptedException {
        setUpCache(strategy);
        Map<Integer, String> many = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            many.put(i, "v" + i);
        }
        cache.putAll(many);
        if (strategy == LRUCache.StrategyType.THREADED) {
            Thread.sleep(200);
        }
        assertEquals(3, cache.size(), "After putAll with 5 entries, size should be limited to the cache capacity (3)");
    }

    /**
     * Simulate a broken cache where the internal strategy map is {@code null}.
     * The {@code size()} call must throw a {@link NullPointerException}.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeWhenStrategyIsNullThrowsNPE(LRUCache.StrategyType strategy) throws Exception {
        setUpCache(strategy);
        // Use reflection to nullify the private 'strategy' field.
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, null);
        assertThrows(NullPointerException.class, cache::size, "Calling size() when internal strategy is null should throw NPE");
    }

    /**
     * Replace the internal strategy with a mock that throws a RuntimeException
     * from its {@code size()} method. The exception must propagate unchanged.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void sizeWhenStrategyThrowsExceptionPropagates(LRUCache.StrategyType strategy) throws Exception {
        setUpCache(strategy);
        // Create a mock Map that throws on size()
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = Mockito.mock(Map.class);
        Mockito.when(mockMap.size()).thenThrow(new RuntimeException("boom"));
        // Inject the mock into the cache via reflection
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, mockMap);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::size, "size() should propagate the RuntimeException thrown by the strategy");
        assertEquals("boom", ex.getMessage());
    }
}
