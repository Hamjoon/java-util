package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class LRUCache_clear_Test {

    private LRUCache<Integer, String> lruCache;

    @BeforeEach
    void setupBeforeEach() {
        // The cache is created per‑test in the parameterized test method
        lruCache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        lruCache = null;
    }

    /* --------------------------------------------------------------------- */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testClearEmptiesCache(LRUCache.StrategyType strategy) {
        // given: a cache with a few entries
        lruCache = new LRUCache<>(3, strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        assertEquals(3, lruCache.size(), "pre‑condition: cache should contain 3 entries");
        assertFalse(lruCache.isEmpty(), "pre‑condition: cache should not be empty");
        assertEquals("A", lruCache.get(1));
        assertEquals("B", lruCache.get(2));
        assertEquals("C", lruCache.get(3));
        // when: clear() is invoked
        lruCache.clear();
        // then: cache behaves as empty
        assertEquals(0, lruCache.size(), "cache size must be 0 after clear");
        assertTrue(lruCache.isEmpty(), "cache must report empty after clear");
        assertNull(lruCache.get(1), "no entry should be returned for key 1 after clear");
        assertNull(lruCache.get(2), "no entry should be returned for key 2 after clear");
        assertNull(lruCache.get(3), "no entry should be returned for key 3 after clear");
        assertFalse(lruCache.containsKey(1), "containsKey must be false after clear");
        assertFalse(lruCache.containsValue("A"), "containsValue must be false after clear");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testClearOnEmptyCacheIsNoOp(LRUCache.StrategyType strategy) {
        // given: an empty cache
        lruCache = new LRUCache<>(5, strategy);
        assertTrue(lruCache.isEmpty(), "cache should start empty");
        // when: clear() is called
        lruCache.clear();
        // then: still empty and no exception thrown
        assertTrue(lruCache.isEmpty(), "cache must remain empty after clear");
        assertEquals(0, lruCache.size(), "size must stay 0 after clear");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testClearRemovesNullKeyAndValue(LRUCache.StrategyType strategy) {
        // given: cache containing a null key and a null value
        lruCache = new LRUCache<>(2, strategy);
        lruCache.put(null, null);
        lruCache.put(1, "X");
        assertEquals(2, lruCache.size());
        assertTrue(lruCache.containsKey(null));
        assertTrue(lruCache.containsValue(null));
        // when: clear() is invoked
        lruCache.clear();
        // then: everything is gone
        assertEquals(0, lruCache.size());
        assertFalse(lruCache.containsKey(null));
        assertFalse(lruCache.containsValue(null));
        assertNull(lruCache.get(null));
        assertNull(lruCache.get(1));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testClearEmptiesKeySetValuesEntrySet(LRUCache.StrategyType strategy) {
        // given: a populated cache
        lruCache = new LRUCache<>(3, strategy);
        lruCache.put(10, "X");
        lruCache.put(20, "Y");
        lruCache.put(30, "Z");
        assertFalse(lruCache.keySet().isEmpty());
        assertFalse(lruCache.values().isEmpty());
        assertFalse(lruCache.entrySet().isEmpty());
        // when
        lruCache.clear();
        // then
        assertTrue(lruCache.keySet().isEmpty(), "keySet must be empty after clear");
        assertTrue(lruCache.values().isEmpty(), "values collection must be empty after clear");
        assertTrue(lruCache.entrySet().isEmpty(), "entrySet must be empty after clear");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testClearAfterPutAllRemovesAllEntries(LRUCache.StrategyType strategy) {
        // given
        lruCache = new LRUCache<>(5, strategy);
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "A");
        source.put(2, "B");
        source.put(3, "C");
        lruCache.putAll(source);
        assertEquals(3, lruCache.size());
        // when
        lruCache.clear();
        // then
        assertEquals(0, lruCache.size());
        assertTrue(lruCache.isEmpty());
        source.keySet().forEach(k -> assertNull(lruCache.get(k)));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testClearRetainsCacheCapacity(LRUCache.StrategyType strategy) {
        // given
        int capacity = 7;
        lruCache = new LRUCache<>(capacity, strategy);
        int before = lruCache.getCapacity();
        // when
        lruCache.clear();
        // then
        int after = lruCache.getCapacity();
        assertEquals(before, after, "capacity must stay unchanged after clear");
        assertEquals(capacity, after, "capacity must equal the constructor argument");
    }

    /* --------------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static <K, V> void injectStrategy(LRUCache<K, V> cache, Map<K, V> mockStrategy) {
        try {
            Field f = LRUCache.class.getDeclaredField("strategy");
            f.setAccessible(true);
            f.set(cache, mockStrategy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testClearInvokesUnderlyingStrategyExactlyOnce() {
        // given
        LRUCache<Integer, String> cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        injectStrategy(cache, mockStrategy);
        // when
        cache.clear();
        // then
        verify(mockStrategy, times(1)).clear();
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testClearPropagatesRuntimeException() {
        // given
        LRUCache<Integer, String> cache = new LRUCache<>(10, LRUCache.StrategyType.THREADED);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        doThrow(new RuntimeException("clear failed")).when(mockStrategy).clear();
        injectStrategy(cache, mockStrategy);
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, cache::clear);
        assertEquals("clear failed", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testClearPropagatesUnsupportedOperationException() {
        // given
        LRUCache<Integer, String> cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        doThrow(new UnsupportedOperationException()).when(mockStrategy).clear();
        injectStrategy(cache, mockStrategy);
        // when / then
        assertThrows(UnsupportedOperationException.class, cache::clear);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testClearPropagatesIllegalStateException() {
        // given
        LRUCache<Integer, String> cache = new LRUCache<>(10, LRUCache.StrategyType.THREADED);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        doThrow(new IllegalStateException("illegal state")).when(mockStrategy).clear();
        injectStrategy(cache, mockStrategy);
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, cache::clear);
        assertEquals("illegal state", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testClearPropagatesConcurrentModificationException() {
        // given
        LRUCache<Integer, String> cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        doThrow(new ConcurrentModificationException("concurrent")).when(mockStrategy).clear();
        injectStrategy(cache, mockStrategy);
        // when / then
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, cache::clear);
        assertEquals("concurrent", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testClearWithNullUnderlyingStrategyThrowsNPE() throws Exception {
        // given: construct a cache and then null‑out the strategy field
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        // force null strategy
        f.set(cache, null);
        // when / then
        assertThrows(NullPointerException.class, cache::clear);
    }
}
