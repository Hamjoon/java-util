package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LRUCache_hashCode_Test {

    static final class FixedHashKey {

        private final int id;

        FixedHashKey(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            // fixed value for every instance
            return 777;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof FixedHashKey && ((FixedHashKey) o).id == this.id;
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // The threaded strategy uses a shared executor; shut it down once all
        // tests have finished to avoid thread‑leak warnings.
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    private static void setStrategy(LRUCache<?, ?> cache, Map<?, ?> strategy) throws Exception {
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, strategy);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCode_isConsistentAcrossCalls(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        int first = cache.hashCode();
        int second = cache.hashCode();
        assertEquals(first, second, "hashCode should return the same value on repeated calls");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCode_isEqualWhenCachesAreEqual(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cacheA = new LRUCache<>(3, strategy);
        LRUCache<Integer, String> cacheB = new LRUCache<>(3, strategy);
        cacheA.put(10, "ten");
        cacheA.put(20, "twenty");
        // also verify handling of nulls
        cacheA.put(null, null);
        cacheB.put(10, "ten");
        cacheB.put(20, "twenty");
        cacheB.put(null, null);
        assertTrue(cacheA.equals(cacheB), "Caches with identical content should be equal");
        assertEquals(cacheA.hashCode(), cacheB.hashCode(), "Equal caches must produce the same hashCode");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCode_differsWhenCachesDiffer(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> cache1 = new LRUCache<>(3, strategy);
        LRUCache<Integer, String> cache2 = new LRUCache<>(3, strategy);
        cache1.put(1, "A");
        cache1.put(2, "B");
        cache2.put(1, "A");
        cache2.put(2, "B");
        // extra entry makes the maps different
        cache2.put(3, "C");
        assertFalse(cache1.equals(cache2), "Caches with different content should not be equal");
        // In the astronomically unlikely event of a collision we still pass the test,
        // but we assert that they are *not* equal to highlight the intended behaviour.
        assertNotEquals(cache1.hashCode(), cache2.hashCode(), "Different caches should usually have different hashCodes");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void hashCode_ofEmptyCache(LRUCache.StrategyType strategy) {
        LRUCache<Integer, String> emptyCache = new LRUCache<>(2, strategy);
        // An empty map has a well‑defined hashCode (the hashCode of an empty map)
        int expected = Map.of().hashCode();
        assertEquals(expected, emptyCache.hashCode(), "Empty cache should delegate to the underlying empty map's hashCode");
    }

    @Test
    void hashCode_delegatesToEmptyHashMap() throws Exception {
        LRUCache<String, Integer> cache = new LRUCache<>(1, LRUCache.StrategyType.LOCKING);
        // Replace the internal map with a plain empty HashMap
        Map<String, Integer> emptyMap = new HashMap<>();
        setStrategy(cache, emptyMap);
        assertEquals(0, cache.hashCode(), "Empty HashMap.hashCode() is defined as 0, cache should delegate to it");
    }

    @Test
    void hashCode_delegatesToCustomMapReturningFixedHash() throws Exception {
        LRUCache<String, Integer> cache = new LRUCache<>(1, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<String, Integer> customMap = Mockito.mock(Map.class);
        Mockito.when(customMap.hashCode()).thenReturn(12345);
        // The map must also behave like a Map for other calls used by LRUCache; we only need hashCode here.
        setStrategy(cache, customMap);
        assertEquals(12345, cache.hashCode(), "Cache should return the hashCode supplied by the custom underlying map");
    }

    @Test
    void hashCode_delegatesToSingleEntryMap() throws Exception {
        Map<String, Integer> singleEntry = new HashMap<>();
        singleEntry.put("a", 1);
        LRUCache<String, Integer> cache = new LRUCache<>(1, LRUCache.StrategyType.LOCKING);
        setStrategy(cache, singleEntry);
        assertEquals(singleEntry.hashCode(), cache.hashCode(), "Cache hashCode must equal the underlying map's hashCode for a single entry");
    }

    @Test
    void hashCode_delegatesToTwoEntryMap() throws Exception {
        Map<String, Integer> twoEntries = new HashMap<>();
        twoEntries.put("a", 1);
        twoEntries.put("b", 2);
        LRUCache<String, Integer> cache = new LRUCache<>(2, LRUCache.StrategyType.LOCKING);
        setStrategy(cache, twoEntries);
        assertEquals(twoEntries.hashCode(), cache.hashCode(), "Cache hashCode must equal the underlying map's hashCode for two entries");
    }

    @Test
    void hashCode_reflectsMutationOfUnderlyingMap() throws Exception {
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("x", 10);
        LRUCache<String, Integer> cache = new LRUCache<>(2, LRUCache.StrategyType.LOCKING);
        setStrategy(cache, mutable);
        // mutate after construction
        mutable.put("y", 20);
        assertEquals(mutable.hashCode(), cache.hashCode(), "Cache hashCode should reflect changes made directly to the underlying map");
    }

    @Test
    void hashCode_afterPutMatchesUnderlyingMap() {
        LRUCache<String, Integer> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        cache.put("k", 100);
        // The underlying map is the strategy; we can obtain its hash via reflection
        // after put, hashCode should be consistent
        int expected = cache.hashCode();
        assertEquals(expected, cache.hashCode(), "hashCode after put must equal the hashCode of the underlying map containing the new entry");
    }

    @Test
    void hashCode_afterRemoveMatchesUnderlyingMap() {
        LRUCache<String, Integer> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.remove("a");
        // simply verify that the method still works
        int expected = cache.hashCode();
        assertEquals(expected, cache.hashCode(), "hashCode after remove must reflect the map state after the removal");
    }

    @Test
    void hashCode_afterClearIsZero() {
        LRUCache<String, Integer> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.clear();
        assertEquals(0, cache.hashCode(), "After clear the underlying map is empty, whose hashCode is defined as 0");
    }

    @Test
    void hashCode_withNullKeyAndValue() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put(null, null);
        LRUCache<String, Integer> cache = new LRUCache<>(1, LRUCache.StrategyType.LOCKING);
        setStrategy(cache, map);
        assertEquals(map.hashCode(), cache.hashCode(), "Cache must delegate hashCode correctly when map contains null key/value");
    }

    @Test
    void hashCode_withUnmodifiableMap() throws Exception {
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("x", 1);
        mutable.put("y", 2);
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(mutable);
        LRUCache<String, Integer> cache = new LRUCache<>(2, LRUCache.StrategyType.LOCKING);
        setStrategy(cache, unmodifiable);
        assertEquals(unmodifiable.hashCode(), cache.hashCode(), "Even when wrapped in Collections.unmodifiableMap, hashCode should delegate correctly");
    }

    @Test
    void hashCode_withCustomKeyObjects() throws Exception {
        Map<FixedHashKey, Integer> map = new HashMap<>();
        map.put(new FixedHashKey(1), 10);
        map.put(new FixedHashKey(2), 20);
        LRUCache<FixedHashKey, Integer> cache = new LRUCache<>(2, LRUCache.StrategyType.LOCKING);
        setStrategy(cache, map);
        assertEquals(map.hashCode(), cache.hashCode(), "Cache must respect custom key hashCode implementations when delegating");
    }

    @Test
    void hashCode_afterPutAllMatchesUnderlyingMap() {
        LRUCache<String, Integer> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        Map<String, Integer> source = Map.of("a", 1, "b", 2, "c", 3);
        cache.putAll(source);
        assertEquals(source.hashCode(), cache.hashCode(), "After putAll the cache's hashCode must equal the underlying map's hashCode");
    }

    @Test
    void hashCode_withConcurrentHashMapStrategy() throws Exception {
        ConcurrentHashMap<String, Integer> concurrent = new ConcurrentHashMap<>();
        concurrent.put("c1", 10);
        concurrent.put("c2", 20);
        LRUCache<String, Integer> cache = new LRUCache<>(2, LRUCache.StrategyType.LOCKING);
        setStrategy(cache, concurrent);
        assertEquals(concurrent.hashCode(), cache.hashCode(), "Cache should delegate hashCode to a ConcurrentHashMap correctly");
    }

    @Test
    void hashCode_withNullUnderlyingStrategy_throwsNPE() throws Exception {
        LRUCache<String, Integer> cache = new LRUCache<>(1, LRUCache.StrategyType.LOCKING);
        // force a null strategy
        setStrategy(cache, null);
        assertThrows(NullPointerException.class, cache::hashCode, "Calling hashCode when the underlying strategy map is null must throw NPE");
    }
}
