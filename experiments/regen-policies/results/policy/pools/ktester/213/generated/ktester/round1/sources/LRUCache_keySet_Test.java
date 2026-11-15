package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_keySet_Test {

    /* --------------------------------------------------------------------- *
     *  Parameter source – the two supported strategy types.                  *
     * --------------------------------------------------------------------- */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /* --------------------------------------------------------------------- *
     *  Test that keySet() returns a Set view containing exactly the keys   *
     *  that have been put into the cache.                                   *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetContainsAllInsertedKeys(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(10, "A");
        cache.put(20, "B");
        cache.put(30, "C");
        Set<Integer> keys = cache.keySet();
        assertEquals(3, keys.size(), "keySet size should match number of entries");
        assertTrue(keys.containsAll(Arrays.asList(10, 20, 30)), "keySet should contain all inserted keys");
    }

    /* --------------------------------------------------------------------- *
     *  Verify that the Set view is live – removing a key from the Set      *
     *  removes the corresponding entry from the cache.                     *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetRemovalIsReflectedInCache(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        Set<String> keys = cache.keySet();
        assertTrue(keys.remove("two"), "remove should return true for existing key");
        // The underlying map must no longer contain the removed entry.
        assertFalse(cache.containsKey("two"));
        assertEquals(2, cache.size());
        // The Set view must also be updated.
        assertFalse(keys.contains("two"));
        assertEquals(2, keys.size());
    }

    /* --------------------------------------------------------------------- *
     *  Ensure that the Set view reflects subsequent puts – i.e. it stays   *
     *  in sync with the cache after new entries are added.                  *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetReflectsSubsequentPuts(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(1, "A");
        Set<Integer> keys = cache.keySet();
        // At this point the set should contain only 1.
        assertEquals(1, keys.size());
        assertTrue(keys.contains(1));
        // Add more entries after obtaining the keySet.
        cache.put(2, "B");
        cache.put(3, "C");
        // The previously obtained Set view must see the new keys.
        assertEquals(3, keys.size());
        assertTrue(keys.containsAll(Arrays.asList(1, 2, 3)));
    }

    /* --------------------------------------------------------------------- *
     *  Edge case – an empty cache returns an empty Set.                     *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetOnEmptyCacheIsEmpty(LRUCache.StrategyType strategy) {
        LRUCache<Object, Object> cache = new LRUCache<>(3, strategy);
        Set<Object> keys = cache.keySet();
        assertNotNull(keys, "keySet should never be null");
        assertTrue(keys.isEmpty(), "keySet of an empty cache must be empty");
    }

    /* --------------------------------------------------------------------- *
     *  Edge case – internal strategy map is deliberately set to null.      *
     *  The method should throw a NullPointerException because it tries to *
     *  delegate to a null reference.                                        *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetThrowsWhenStrategyIsNull(LRUCache.StrategyType strategy) throws Exception {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        // Use reflection to replace the final 'strategy' field with null.
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        // Remove final modifier (required on some JVMs)
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(strategyField, strategyField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        strategyField.set(cache, null);
        assertThrows(NullPointerException.class, cache::keySet, "Calling keySet() when internal strategy is null should throw NPE");
    }

    /* --------------------------------------------------------------------- *
     *  Edge case – underlying map throws UnsupportedOperationException   *
     *  from its keySet() method. The exception must propagate unchanged.   *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetPropagatesUnsupportedOperationException(LRUCache.StrategyType strategy) throws Exception {
        // Create a mock Map that throws on keySet()
        @SuppressWarnings("unchecked")
        Map<String, Integer> throwingMap = mock(Map.class);
        when(throwingMap.keySet()).thenThrow(new UnsupportedOperationException("keySet not supported"));
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        // Inject the mock map via reflection
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        // Remove final modifier (required on some JVMs)
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(strategyField, strategyField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        strategyField.set(cache, throwingMap);
        assertThrows(UnsupportedOperationException.class, cache::keySet, "UnsupportedOperationException from underlying map should propagate");
    }

    /* --------------------------------------------------------------------- *
     *  Additional sanity test – live view after removal via iterator.      *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void keySetIteratorRemoveReflectsInCache(LRUCache.StrategyType strategy) {
        LRUCache<String, Integer> cache = new LRUCache<>(5, strategy);
        cache.put("x", 10);
        cache.put("y", 20);
        Set<String> keys = cache.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String k = it.next();
            if ("y".equals(k)) {
                // remove via iterator
                it.remove();
            }
        }
        assertFalse(keys.contains("y"));
        assertFalse(cache.containsKey("y"));
        assertEquals(1, keys.size());
        assertEquals(1, cache.size());
    }
}
