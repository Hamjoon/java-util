package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_equals_Test {

    private LRUCache<Integer, String> cache;

    private static final int CAPACITY = 5;

    @BeforeEach
    void setupBeforeEach() {
        // Use the locking strategy for deterministic behaviour in unit tests
        cache = new LRUCache<>(CAPACITY, LRUCache.StrategyType.LOCKING);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid cross‑test contamination
        cache.clear();
    }

    /* --------------------------------------------------------------
     *  Basic reflexive / null / non‑Map behaviour (parameterized)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> basicEqualityProvider() {
        return Stream.of(Arguments.of(cache -> cache, true, "same instance (reflexive)"), Arguments.of(cache -> null, false, "null argument"), Arguments.of(cache -> "not a map", false, "non‑Map argument"));
    }

    @Test
    void equalsSameInstanceReturnsTrue() {
        assertTrue(cache.equals(cache), "Cache should be equal to itself");
    }

    @Test
    void equalsNullReturnsFalse() {
        assertFalse(cache.equals(null), "Cache should not be equal to null");
    }

    @Test
    void equalsNonMapReturnsFalse() {
        assertFalse(cache.equals("not a map"), "Cache should not be equal to an object that is not a Map");
    }

    @Test
    void equalsEmptyCacheAndEmptyMap() {
        Map<Integer, String> emptyMap = new HashMap<>();
        assertTrue(cache.equals(emptyMap), "Empty cache should be equal to an empty map");
        assertTrue(emptyMap.equals(cache), "Empty map should be equal to empty cache (Map contract)");
    }

    @Test
    void equalsCacheAndMapWithSameEntries() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        Map<Integer, String> other = new HashMap<>();
        other.put(1, "one");
        other.put(2, "two");
        other.put(3, "three");
        assertTrue(cache.equals(other), "Cache should be equal to a map with the same entries");
        assertTrue(other.equals(cache), "Map should be equal to cache with the same entries (Map contract)");
    }

    @Test
    void equalsCacheAndMapWithDifferentValues() {
        cache.put(1, "ONE");
        cache.put(2, "TWO");
        Map<Integer, String> other = new HashMap<>();
        // different case
        other.put(1, "one");
        other.put(2, "TWO");
        assertFalse(cache.equals(other), "Cache should not be equal when values differ");
    }

    @Test
    void equalsCacheAndMapWithDifferentKeys() {
        cache.put(1, "A");
        cache.put(2, "B");
        Map<Integer, String> other = new HashMap<>();
        other.put(1, "A");
        // different key
        other.put(3, "C");
        assertFalse(cache.equals(other), "Cache should not be equal when key sets differ");
    }

    @Test
    void equalsCacheContainingNullKeyAndValue() {
        cache.put(null, null);
        cache.put(1, "value");
        Map<Integer, String> other = new HashMap<>();
        other.put(null, null);
        other.put(1, "value");
        assertTrue(cache.equals(other), "Cache with null key/value should be equal to map with same entries");
    }

    @Test
    void equalsCacheAgainstMapWithExtraEntry() {
        cache.put(1, "A");
        cache.put(2, "B");
        Map<Integer, String> other = new HashMap<>();
        other.put(1, "A");
        other.put(2, "B");
        // extra entry
        other.put(3, "C");
        assertFalse(cache.equals(other), "Cache should not be equal to a map that has extra entries");
    }

    @ParameterizedTest(name = "equals({2}) → {1}")
    @MethodSource("basicEqualityProvider")
    void equalsBasicBehavior(java.util.function.Function<LRUCache<Integer, String>, Object> mapper, boolean expected, String description) {
        Object other = mapper.apply(cache);
        assertEquals(expected, cache.equals(other), description);
    }

    @Test
    void equalsCacheWithLinkedHashMapSubclass() {
        cache.put("k", "5");
        cache.put("l", "6");
        // Anonymous subclass of LinkedHashMap with same entries
        Map<String, String> linkedMap = new LinkedHashMap<>() {

            {
                put("k", "5");
                put("l", "6");
            }
        };
        assertTrue(cache.equals(linkedMap), "Cache should be equal to a LinkedHashMap subclass with same entries");
    }

    /* --------------------------------------------------------------
     *  Equality with another LRUCache instance
     * -------------------------------------------------------------- */
    @Test
    void equalsTwoEmptyLRUCaches() {
        LRUCache<Integer, String> other = new LRUCache<>(CAPACITY, LRUCache.StrategyType.LOCKING);
        assertTrue(cache.equals(other), "Two empty LRUCache instances should be equal");
        assertTrue(other.equals(cache), "Map contract: equality should be symmetric");
    }

    @Test
    void equalsTwoLRUCachesWithSameEntries() {
        cache.put(10, "x");
        cache.put(20, "y");
        LRUCache<Integer, String> other = new LRUCache<>(CAPACITY, LRUCache.StrategyType.LOCKING);
        other.put(10, "x");
        other.put(20, "y");
        assertTrue(cache.equals(other), "Caches with identical entries should be equal");
        assertTrue(other.equals(cache), "Symmetric equality");
    }

    @Test
    void equalsLRUCacheWithExtraEntry() {
        cache.put(1, "a");
        cache.put(2, "b");
        LRUCache<Integer, String> other = new LRUCache<>(CAPACITY, LRUCache.StrategyType.LOCKING);
        other.put(1, "a");
        other.put(2, "b");
        // extra entry
        other.put(3, "c");
        assertFalse(cache.equals(other), "Cache should not be equal to another cache with extra entry");
    }

    @Test
    void equalsLRUCacheWithDifferentValue() {
        cache.put(1, "a");
        cache.put(2, "b");
        LRUCache<Integer, String> other = new LRUCache<>(CAPACITY, LRUCache.StrategyType.LOCKING);
        other.put(1, "different");
        other.put(2, "b");
        assertFalse(cache.equals(other), "Caches with same keys but different values must not be equal");
    }

    /* --------------------------------------------------------------
     *  Exception propagation from custom strategy implementations
     * -------------------------------------------------------------- */
    @Test
    void equalsThrowsNullPointerExceptionWhenStrategyDisallowsNullKey() throws Exception {
        // Mock a strategy map that throws NPE when equals is invoked with a map containing a null key
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.equals(any())).thenAnswer(invocation -> {
            Map<?, ?> other = (Map<?, ?>) invocation.getArgument(0);
            if (other.containsKey(null)) {
                throw new NullPointerException("null keys not allowed");
            }
            // delegate to default (won't be reached)
            return mockStrategy.equals(other);
        });
        // Replace the private 'strategy' field with our mock
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        Map<Integer, String> originalStrategy = (Map<Integer, String>) strategyField.get(cache);
        strategyField.set(cache, mockStrategy);
        try {
            Map<Integer, String> mapWithNullKey = new HashMap<>();
            mapWithNullKey.put(null, "value");
            assertThrows(NullPointerException.class, () -> cache.equals(mapWithNullKey), "Expected NullPointerException when strategy does not support null keys");
        } finally {
            // Restore original strategy to avoid side‑effects on other tests
            strategyField.set(cache, originalStrategy);
        }
    }

    @Test
    void equalsThrowsNullPointerExceptionWhenStrategyDisallowsNullValue() throws Exception {
        // Mock a strategy map that throws NPE when equals is invoked with a map containing a null value
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.equals(any())).thenAnswer(invocation -> {
            Map<?, ?> other = (Map<?, ?>) invocation.getArgument(0);
            if (other.containsValue(null)) {
                throw new NullPointerException("null values not allowed");
            }
            return mockStrategy.equals(other);
        });
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        Map<Integer, String> originalStrategy = (Map<Integer, String>) strategyField.get(cache);
        strategyField.set(cache, mockStrategy);
        try {
            Map<Integer, String> mapWithNullValue = new HashMap<>();
            mapWithNullValue.put(1, null);
            assertThrows(NullPointerException.class, () -> cache.equals(mapWithNullValue), "Expected NullPointerException when strategy does not support null values");
        } finally {
            strategyField.set(cache, originalStrategy);
        }
    }

    @Test
    void equalsPropagatesIllegalStateExceptionFromCustomStrategy() throws Exception {
        // Mock a strategy that always throws IllegalStateException from its equals()
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.equals(any())).thenThrow(new IllegalStateException("boom"));
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        Map<Integer, String> originalStrategy = (Map<Integer, String>) strategyField.get(cache);
        strategyField.set(cache, mockStrategy);
        try {
            Map<Integer, String> emptyMap = Collections.emptyMap();
            assertThrows(IllegalStateException.class, () -> cache.equals(emptyMap), "Exception from custom strategy should propagate");
        } finally {
            strategyField.set(cache, originalStrategy);
        }
    }
}
