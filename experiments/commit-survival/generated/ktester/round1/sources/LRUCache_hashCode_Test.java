package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentModificationException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_hashCode_Test {

    private LRUCache<Integer, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // The cache will be created inside each test (or test helper) to keep tests independent.
        cache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            cache.clear();
            // ensure any background threads are stopped for this instance
            cache.shutdown();
            cache = null;
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the thread pool used by the threaded strategy (if any).
        ThreadedLRUCacheStrategy.shutdown();
    }

    /* --------------------------------------------------------------------- */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /* --------------------------------------------------------------------- */
    private void injectStrategy(Map<Integer, String> newStrategy) throws Exception {
        // any strategy, we will replace it
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, newStrategy);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeMatchesUnderlyingMap(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        // Build a reference map with the same entries and order.
        Map<Integer, String> reference = new LinkedHashMap<>();
        reference.put(1, "A");
        reference.put(2, "B");
        reference.put(3, "C");
        assertEquals(reference.hashCode(), cache.hashCode(), "LRUCache.hashCode() should delegate to the underlying strategy's map hashCode");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeIsConsistentWhenCacheUnchanged(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(10, "X");
        cache.put(20, "Y");
        int first = cache.hashCode();
        int second = cache.hashCode();
        assertEquals(first, second, "hashCode should be stable while the cache content does not change");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeDiffersWhenContentDiffers(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache1 = new LRUCache<>(3, strategy);
        LRUCache<Integer, String> cache2 = new LRUCache<>(3, strategy);
        cache1.put(1, "A");
        cache1.put(2, "B");
        cache2.put(1, "A");
        cache2.put(2, "B");
        assertEquals(cache1.hashCode(), cache2.hashCode(), "Two caches with identical entries should have identical hashCodes");
        // Change one entry in cache2
        cache2.put(2, "Z");
        assertNotEquals(cache1.hashCode(), cache2.hashCode(), "Caches with different entries should produce different hashCodes");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeHandlesNullKeyAndValue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(null, null);
        cache.put(1, null);
        cache.put(null, "value");
        // Build a reference map with the same null handling.
        Map<Integer, String> reference = new LinkedHashMap<>();
        // last put overwrites previous null key entry
        reference.put(null, "value");
        reference.put(1, null);
        assertEquals(reference.hashCode(), cache.hashCode(), "hashCode should correctly account for null keys and values");
        assertNotEquals(0, cache.hashCode(), "hashCode should not be zero for a non‑empty cache");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeReflectsEvictionAfterPut(LRUCache.StrategyType strategy) {
        // Use a small capacity to force eviction.
        cache = new LRUCache<>(2, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        int hashBefore = cache.hashCode();
        // Adding a third entry should evict the least‑recently‑used (key 1).
        cache.put(3, "C");
        int hashAfter = cache.hashCode();
        assertNotEquals(hashBefore, hashAfter, "hashCode should change after eviction modifies the cache content");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeDelegatesToEmptyStrategy(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        // underlying map is empty
        assertEquals(0, cache.hashCode(), "hashCode of an empty LRUCache should match Map.hashCode() which is 0 for empty maps");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeDelegatesToSingleEntry(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "one");
        // Build reference map with same entry
        Map<Integer, String> reference = new LinkedHashMap<>();
        reference.put(1, "one");
        assertEquals(reference.hashCode(), cache.hashCode(), "hashCode should match the underlying map's hashCode for a single entry");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeDelegatesToMultipleEntries(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        cache.put(4, "D");
        cache.put(5, "E");
        Map<Integer, String> reference = new LinkedHashMap<>();
        reference.put(1, "A");
        reference.put(2, "B");
        reference.put(3, "C");
        reference.put(4, "D");
        reference.put(5, "E");
        assertEquals(reference.hashCode(), cache.hashCode(), "hashCode should be identical to the underlying map's hashCode for multiple entries");
    }

    @Test
    void hashCodeDelegatesToSynchronizedMapWrapper() throws Exception {
        Map<Integer, String> syncMap = Collections.synchronizedMap(new LinkedHashMap<>());
        syncMap.put(10, "X");
        syncMap.put(20, "Y");
        injectStrategy(syncMap);
        assertEquals(syncMap.hashCode(), cache.hashCode(), "LRUCache should delegate hashCode to a synchronized map wrapper");
    }

    @Test
    void hashCodeDelegatesToUnmodifiableMapWrapper() throws Exception {
        Map<Integer, String> unmod = Collections.unmodifiableMap(new LinkedHashMap<>(Map.of(1, "A", 2, "B")));
        injectStrategy(unmod);
        assertEquals(unmod.hashCode(), cache.hashCode(), "LRUCache should delegate hashCode to an unmodifiable map wrapper");
    }

    @Test
    void hashCodePropagatesConstantHashFromCustomMap() throws Exception {
        // Custom map that always returns 42 from hashCode()
        Map<Integer, String> constantMap = new AbstractMap<>() {

            @Override
            public Set<Entry<Integer, String>> entrySet() {
                // not used for hashCode test
                return Collections.emptySet();
            }

            @Override
            public int hashCode() {
                return 42;
            }
        };
        injectStrategy(constantMap);
        assertEquals(42, cache.hashCode(), "LRUCache.hashCode() must propagate the constant hash code from the custom map");
    }

    @Test
    void hashCodeReflectsClearedMap() throws Exception {
        Map<Integer, String> underlying = new LinkedHashMap<>();
        underlying.put(1, "A");
        underlying.put(2, "B");
        injectStrategy(underlying);
        // verify non‑zero before clear
        assertNotEquals(0, cache.hashCode(), "Pre‑condition: hashCode should be non‑zero when map has entries");
        // clear directly
        underlying.clear();
        assertEquals(0, cache.hashCode(), "After clearing the underlying map, hashCode should reflect the empty state (0)");
    }

    @Test
    void hashCodePropagatesUnsupportedOperationException() throws Exception {
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.hashCode()).thenThrow(new UnsupportedOperationException("hash not supported"));
        injectStrategy(mockMap);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> cache.hashCode(), "hashCode should propagate UnsupportedOperationException from the strategy");
        assertEquals("hash not supported", ex.getMessage());
    }

    @Test
    void hashCodePropagatesConcurrentModificationException() throws Exception {
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.hashCode()).thenThrow(new ConcurrentModificationException("concurrent mod"));
        injectStrategy(mockMap);
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, () -> cache.hashCode(), "hashCode should propagate ConcurrentModificationException from the strategy");
        assertEquals("concurrent mod", ex.getMessage());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCodeAfterShutdownThrowsNullPointerException(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        // internal strategy set to null
        cache.shutdown();
        assertThrows(NullPointerException.class, () -> cache.hashCode(), "Calling hashCode after shutdown should throw NullPointerException because the strategy is cleared");
    }
}
