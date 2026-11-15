package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LRUCache_equals_Test {

    private LRUCache<Integer, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // The actual cache instance is created in the test methods so that we
        // can vary the strategy type (LOCKING or THREADED) per test.
        cache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache to avoid side‑effects between tests.
        if (cache != null) {
            cache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Ensure the thread pool used by the threaded strategy is cleaned up.
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void equalsSameInstanceReturnsTrue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        assertTrue(cache.equals(cache), "Cache should be equal to itself");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void equalsNullReturnsFalse(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        assertFalse(cache.equals(null), "Cache should not be equal to null");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void equalsNonMapReturnsFalse(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        assertFalse(cache.equals("not a map"), "Cache should not be equal to a non‑Map object");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void equalsMapWithSameContentReturnsTrue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        // Build a plain HashMap with identical entries
        Map<Integer, String> other = new HashMap<>();
        other.put(1, "A");
        other.put(2, "B");
        other.put(3, "C");
        assertTrue(cache.equals(other), "Cache should be equal to a Map with the same entries");
        // Symmetry: the other map's equals should also consider the cache equal
        assertTrue(other.equals(cache), "HashMap should consider the cache equal when entries match");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void equalsMapWithDifferentContentReturnsFalse(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        Map<Integer, String> other = new HashMap<>();
        other.put(1, "A");
        // different value
        other.put(2, "different");
        assertFalse(cache.equals(other), "Cache should not be equal when a value differs");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void equalsAnotherLRUCacheWithSameContentReturnsTrue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "X");
        // test null handling
        cache.put(2, null);
        cache.put(null, "Z");
        LRUCache<Integer, String> otherCache = new LRUCache<>(3, strategy);
        otherCache.put(1, "X");
        otherCache.put(2, null);
        otherCache.put(null, "Z");
        assertTrue(cache.equals(otherCache), "Two LRUCache instances with identical content should be equal");
        // also test the reverse direction
        assertTrue(otherCache.equals(cache), "Equality should be symmetric");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void equalsAnotherLRUCacheWithDifferentContentReturnsFalse(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        LRUCache<Integer, String> otherCache = new LRUCache<>(3, strategy);
        // different value for same key
        otherCache.put(1, "B");
        assertFalse(cache.equals(otherCache), "Caches with differing entries must not be equal");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void equalsEmptyCachesAreEqual(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        // different capacity is irrelevant
        LRUCache<Integer, String> other = new LRUCache<>(5, strategy);
        assertTrue(cache.equals(other), "Two empty caches should be equal regardless of capacity");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void equalsEmptyHashMapReturnsTrue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        Map<Integer, String> emptyMap = new HashMap<>();
        assertTrue(cache.equals(emptyMap), "Empty cache should be equal to an empty HashMap");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void equalsHashMapWithNullValueReturnsTrue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        // cache contains a null value
        cache.put(1, null);
        Map<Integer, String> other = new HashMap<>();
        other.put(1, null);
        assertTrue(cache.equals(other), "Cache with a null value should be equal to a map containing the same null entry");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void equalsTreeMapSameEntriesReturnsTrue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(2, "B");
        cache.put(1, "A");
        cache.put(3, "C");
        // TreeMap sorts keys; order of insertion differs from cache's internal order
        Map<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(1, "A");
        treeMap.put(2, "B");
        treeMap.put(3, "C");
        assertTrue(cache.equals(treeMap), "Cache should be equal to a TreeMap with the same entries despite different concrete types");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void equalsHashMapDifferentInsertionOrderReturnsTrue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        // Insert in order 1,2,3
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        // Insert same entries but in reverse order
        Map<Integer, String> other = new LinkedHashMap<>();
        other.put(3, "C");
        other.put(2, "B");
        other.put(1, "A");
        assertTrue(cache.equals(other), "Map equality is order‑independent; should return true even if insertion order differs");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void equalsHashMapWithExtraEntryReturnsFalse(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        Map<Integer, String> largerMap = new HashMap<>();
        largerMap.put(1, "A");
        largerMap.put(2, "B");
        // extra entry
        largerMap.put(3, "C");
        assertFalse(cache.equals(largerMap), "Cache should not be equal to a map that contains extra entries");
    }
}
