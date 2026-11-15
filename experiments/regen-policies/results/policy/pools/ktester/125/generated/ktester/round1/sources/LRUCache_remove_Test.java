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
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class LRUCache_remove_Test {

    private LRUCache<Integer, String> cache;

    @AfterAll
    static void teardownAfterAll() {
        // The threaded strategy uses a shared executor that must be shut down.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /* --------------------------------------------------------------------- */
    private void setUp(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
    }

    /* --------------------------------------------------------------------- */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /* --------------------------------------------------------------------- */
    private static void injectMockStrategy(LRUCache<?, ?> target, Map<?, ?> mock) throws Exception {
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(target, mock);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveExistingKey(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        assertEquals(2, cache.size());
        String removed = cache.remove(1);
        assertEquals("one", removed, "remove should return the previous value");
        assertFalse(cache.containsKey(1), "key 1 should no longer be present");
        assertEquals(1, cache.size(), "size should decrease after removal");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveMissingKey(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(10, "ten");
        cache.put(20, "twenty");
        int originalSize = cache.size();
        // 30 was never added
        String removed = cache.remove(30);
        assertNull(removed, "remove of absent key should return null");
        assertEquals(originalSize, cache.size(), "size must stay the same");
        assertTrue(cache.containsKey(10));
        assertTrue(cache.containsKey(20));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveNullKey(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(null, "nullValue");
        cache.put(5, "five");
        assertTrue(cache.containsKey(null));
        String removed = cache.remove(null);
        assertEquals("nullValue", removed, "remove should return the value for null key");
        assertFalse(cache.containsKey(null), "null key should be gone after removal");
        assertTrue(cache.containsKey(5), "other entries must remain untouched");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveAbsentNullKey(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "one");
        assertFalse(cache.containsKey(null));
        String removed = cache.remove(null);
        assertNull(removed, "remove of absent null key should return null");
        assertEquals(1, cache.size(), "size must be unchanged");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveAfterClear(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(100, "hundred");
        cache.put(200, "two hundred");
        // make the cache empty
        cache.clear();
        assertEquals(0, cache.size());
        String removed = cache.remove(100);
        assertNull(removed, "remove on cleared cache should return null");
        assertEquals(0, cache.size(), "size stays zero after removal attempt");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveKeyWithNullValue(LRUCache.StrategyType strategy) {
        setUp(strategy);
        // store explicit null value
        cache.put(42, null);
        assertTrue(cache.containsKey(42));
        String removed = cache.remove(42);
        assertNull(removed, "remove should return the stored null value");
        assertFalse(cache.containsKey(42), "key must be removed even if value was null");
        assertEquals(0, cache.size(), "size should reflect the removal");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveOnNeverPopulatedCache(LRUCache.StrategyType strategy) {
        setUp(strategy);
        assertEquals(0, cache.size());
        String removed = cache.remove("missingKey");
        assertNull(removed, "remove on empty cache should return null");
        assertEquals(0, cache.size(), "size remains zero");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveAfterEviction(LRUCache.StrategyType strategy) {
        // Use a tiny capacity to force eviction deterministically.
        cache = new LRUCache<>(2, strategy);
        // most recent
        cache.put(1, "one");
        // LRU order: 1,2
        cache.put(2, "two");
        // should evict key 1 (LRU)
        cache.put(3, "three");
        // Ensure eviction has taken place (for LOCKING it is immediate,
        // for THREADED we wait a short moment because cleanup runs async).
        if (strategy == LRUCache.StrategyType.THREADED) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
        assertFalse(cache.containsKey(1), "key 1 must have been evicted");
        assertTrue(cache.containsKey(2));
        assertTrue(cache.containsKey(3));
        String removed = cache.remove(1);
        assertNull(removed, "removing an evicted key should return null");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testSizeDecrementAfterRemove(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(10, "ten");
        cache.put(20, "twenty");
        int sizeBefore = cache.size();
        cache.remove(10);
        assertEquals(sizeBefore - 1, cache.size(), "size should be reduced by exactly one after removal");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveAfterShutdown(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(5, "five");
        // mark cache as non‑operational
        cache.shutdown();
        assertThrows(IllegalStateException.class, () -> cache.remove(5), "remove after shutdown must throw IllegalStateException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRemoveWrongKeyType() {
        // Cache expects String keys.
        LRUCache<String, String> stringCache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        stringCache.put("key", "value");
        // Attempt to remove using an Integer – the underlying map will try to cast.
        assertThrows(ClassCastException.class, () -> stringCache.remove(123), "Removing with an incompatible key type should throw ClassCastException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRemoveUnsupportedOperation() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.remove(any())).thenThrow(new UnsupportedOperationException("remove not supported"));
        injectMockStrategy(cache, mockMap);
        assertThrows(UnsupportedOperationException.class, () -> cache.remove(1), "When underlying strategy does not support remove, the exception should propagate");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRemoveNullKeyThrowsNPE() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.remove(isNull())).thenThrow(new NullPointerException("null keys not allowed"));
        injectMockStrategy(cache, mockMap);
        assertThrows(NullPointerException.class, () -> cache.remove(null), "If the underlying strategy rejects null keys, remove should propagate NPE");
    }
}
