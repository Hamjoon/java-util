package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_remove_Test {

    private LRUCache<Integer, String> cache;

    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            cache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the thread pool used by the threaded strategy (if any).
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    private void setUp(LRUCache.StrategyType strategy) {
        // Capacity 3 is enough for all remove‑related tests.
        cache = new LRUCache<>(3, strategy);
    }

    /**
     * Helper that injects a Mockito mock {@code Map} into the private
     * {@code strategy} field of the cache instance.
     */
    private static <K, V> void injectMockStrategy(LRUCache<K, V> cache, Map<K, V> mockMap) {
        try {
            Field f = LRUCache.class.getDeclaredField("strategy");
            f.setAccessible(true);
            f.set(cache, mockMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock strategy", e);
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveExistingKey(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // Remove a key that is present.
        String removed = cache.remove(2);
        assertEquals("two", removed, "remove should return the previous value");
        assertFalse(cache.containsKey(2), "Key 2 should no longer be present");
        assertEquals(2, cache.size(), "Size should be decremented");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveNonExistingKey(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        // Attempt to remove a key that was never added.
        String removed = cache.remove(99);
        assertNull(removed, "remove should return null for absent key");
        assertEquals(2, cache.size(), "Size must remain unchanged");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveNullKey(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(null, "nullValue");
        cache.put(1, "one");
        // Remove the null key.
        String removed = cache.remove(null);
        assertEquals("nullValue", removed, "remove should handle null keys");
        assertFalse(cache.containsKey(null), "Null key should be gone after removal");
        assertEquals(1, cache.size(), "Size should reflect the removal of the null entry");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveKeyWithNullValue(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, null);
        cache.put(2, "two");
        // Remove the entry whose value is null.
        String removed = cache.remove(1);
        assertNull(removed, "remove should return null when the stored value is null");
        assertFalse(cache.containsKey(1), "Key 1 should be removed");
        assertEquals(1, cache.size(), "Size should be updated after removal");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testRemoveThenPutSameKey(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "first");
        cache.put(2, "second");
        // Remove key 1 and then put a new value for the same key.
        String removed = cache.remove(1);
        assertEquals("first", removed);
        assertFalse(cache.containsKey(1));
        cache.put(1, "new");
        assertTrue(cache.containsKey(1));
        assertEquals("new", cache.get(1));
        assertEquals(2, cache.size(), "Cache size should be back to 2 after re‑inserting");
    }

    @Test
    void testRemoveNullKeyThrowsNullPointerException() {
        // Use a normal cache instance, then replace its strategy with a mock.
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.remove(null)).thenThrow(new NullPointerException("null key not allowed"));
        injectMockStrategy(cache, mockMap);
        assertThrows(NullPointerException.class, () -> cache.remove(null), "Removing a null key should propagate NullPointerException from the strategy");
    }

    @Test
    void testRemoveIncompatibleKeyThrowsClassCastException() {
        // Cache expects String keys, we will pass an Integer.
        LRUCache<String, String> stringCache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<String, String> mockMap = mock(Map.class);
        when(mockMap.remove(any())).thenAnswer(invocation -> {
            Object key = invocation.getArgument(0);
            // Simulate the underlying map throwing ClassCastException for wrong type.
            if (!(key instanceof String)) {
                throw new ClassCastException("Key type mismatch");
            }
            return null;
        });
        injectMockStrategy(stringCache, mockMap);
        assertThrows(ClassCastException.class, () -> stringCache.remove(123), "Removing with an incompatible key type should result in ClassCastException");
    }

    @Test
    void testRemoveOnUnmodifiableStrategyThrowsUnsupportedOperationException() {
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.remove(any())).thenThrow(new UnsupportedOperationException("unmodifiable"));
        injectMockStrategy(cache, mockMap);
        assertThrows(UnsupportedOperationException.class, () -> cache.remove(1), "Removing from an unmodifiable underlying map should propagate UnsupportedOperationException");
    }

    @Test
    void testRemoveAfterShutdownThrowsIllegalStateException() {
        setUp(LRUCache.StrategyType.LOCKING);
        cache.put(1, "one");
        cache.put(2, "two");
        // Shut the cache down – after this the cache is expected to reject operations.
        cache.shutdown();
        assertThrows(IllegalStateException.class, () -> cache.remove(1), "Calling remove after shutdown should throw IllegalStateException");
    }
}
