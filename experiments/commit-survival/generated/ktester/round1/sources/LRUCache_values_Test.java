package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class LRUCache_values_Test {

    private LRUCache<Integer, String> lruCache;

    @AfterEach
    void teardownAfterEach() {
        if (lruCache != null) {
            lruCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Ensure any background threads created by the threaded strategy are stopped.
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    private void setUp(LRUCache.StrategyType strategyType) {
        // capacity is supplied by each test case
        lruCache = new LRUCache<>(capacity, strategyType);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesReflectCacheContents(LRUCache.StrategyType strategy) {
        setUp(strategy);
        // Populate the cache
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        // Obtain the values view and verify its content
        Collection<String> values = lruCache.values();
        assertEquals(2, values.size(), "Values collection should contain two elements");
        assertTrue(values.contains("A"));
        assertTrue(values.contains("B"));
        // Add another entry – the view must reflect the change
        lruCache.put(3, "C");
        Collection<String> updatedValues = lruCache.values();
        assertEquals(3, updatedValues.size(), "Values collection should reflect the newly added entry");
        assertTrue(updatedValues.contains("C"));
        // Remove an entry – the view must also reflect the removal
        lruCache.remove(2);
        Collection<String> afterRemoval = lruCache.values();
        assertEquals(2, afterRemoval.size(), "Values collection should shrink after removal");
        assertFalse(afterRemoval.contains("B"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesCollectionIsLiveAndRemovable(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "X");
        lruCache.put(2, "Y");
        lruCache.put(3, "Z");
        Collection<String> values = lruCache.values();
        assertEquals(3, values.size());
        // Remove an element via the iterator of the values view
        Iterator<String> iterator = values.iterator();
        while (iterator.hasNext()) {
            String val = iterator.next();
            if ("Y".equals(val)) {
                // should remove the underlying map entry
                iterator.remove();
                break;
            }
        }
        // Verify that the cache no longer contains the key associated with the removed value
        assertFalse(lruCache.containsKey(2), "Key 2 should have been removed via values().remove()");
        assertEquals(2, lruCache.size());
        assertFalse(lruCache.values().contains("Y"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesWithNulls(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, null);
        lruCache.put(null, "nullKey");
        lruCache.put(2, null);
        Collection<String> values = lruCache.values();
        // The collection must contain both null values and the non‑null value
        assertEquals(3, values.size());
        assertTrue(values.contains(null));
        assertTrue(values.contains("nullKey"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesOnEmptyCache(LRUCache.StrategyType strategy) {
        setUp(strategy);
        Collection<String> values = lruCache.values();
        assertNotNull(values, "values() must never return null");
        assertEquals(0, values.size(), "Empty cache should produce an empty values collection");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesAfterMultiplePuts(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        Collection<String> values = lruCache.values();
        assertEquals(3, values.size(), "Cache with three entries should return three values");
        assertTrue(values.containsAll(Arrays.asList("A", "B", "C")), "Values collection must contain all inserted values (order not guaranteed)");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesAfterUpdatingExistingKey(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "X");
        lruCache.put(2, "Y");
        // Update key 1
        lruCache.put(1, "Z");
        Collection<String> values = lruCache.values();
        assertEquals(2, values.size(), "Updating an existing key must not increase collection size");
        assertTrue(values.containsAll(Arrays.asList("Z", "Y")), "Values collection must reflect the new value for the updated key");
        assertFalse(values.contains("X"), "Old value should be replaced");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesAfterRemovalOfKey(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.remove(2);
        Collection<String> values = lruCache.values();
        assertEquals(1, values.size(), "After removing a key, collection size must shrink");
        assertTrue(values.contains("A"));
        assertFalse(values.contains("B"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesLiveViewRemovalViaIterator(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        Collection<String> values = lruCache.values();
        Iterator<String> it = values.iterator();
        while (it.hasNext()) {
            if ("A".equals(it.next())) {
                // should delete the underlying entry (key 1)
                it.remove();
                break;
            }
        }
        assertEquals(1, lruCache.size(), "Cache size must reflect removal through iterator");
        assertFalse(lruCache.containsKey(1), "Key 1 should be removed via values().iterator().remove()");
        assertFalse(lruCache.values().contains("A"));
        assertTrue(lruCache.values().contains("B"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesAfterClear(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        lruCache.clear();
        Collection<String> values = lruCache.values();
        assertEquals(0, values.size(), "values() must be empty after clear()");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesAfterCapacityOverflowEviction(LRUCache.StrategyType strategy) {
        // capacity 2 to trigger eviction
        setUp(strategy, 2);
        // should become LRU
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        // triggers eviction of key 1
        lruCache.put(3, "C");
        // For the threaded strategy eviction may be asynchronous; give it a tiny window.
        if (strategy == LRUCache.StrategyType.THREADED) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
        Collection<String> values = lruCache.values();
        assertEquals(2, values.size(), "Cache must respect its max size after eviction");
        assertTrue(values.containsAll(Arrays.asList("B", "C")), "Evicted value must be absent");
        assertFalse(values.contains("A"), "Least‑recently used entry should have been evicted");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesHandlesNullValues(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, null);
        lruCache.put(2, "NotNull");
        Collection<String> values = lruCache.values();
        assertEquals(2, values.size(), "Collection size must count null values");
        assertTrue(values.contains(null), "Null value must be present in the collection");
        assertTrue(values.contains("NotNull"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesAfterPutAll(LRUCache.StrategyType strategy) {
        setUp(strategy);
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "X");
        source.put(2, "Y");
        source.put(3, "Z");
        lruCache.putAll(source);
        Collection<String> values = lruCache.values();
        assertEquals(3, values.size(), "putAll should populate the cache with all entries");
        assertTrue(values.containsAll(Arrays.asList("X", "Y", "Z")));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesAfterShutdownThrowsIllegalStateException(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "A");
        // Shut down the cache – the concrete implementation is expected to
        // mark the cache as unusable. The exact exception type is defined by the API.
        lruCache.shutdown();
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> lruCache.values(), "Calling values() after shutdown() must throw IllegalStateException");
        assertNotNull(thrown.getMessage());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testValuesWhenUnderlyingStrategyIsNullThrowsNPE(LRUCache.StrategyType strategy) throws Exception {
        // Create a normal cache first
        setUp(strategy);
        // Use reflection to nullify the private 'strategy' field
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(lruCache, null);
        // Expect a NullPointerException when delegating to the null strategy
        NullPointerException npe = assertThrows(NullPointerException.class, () -> lruCache.values(), "values() should throw NPE when internal strategy is null");
        assertNotNull(npe.getMessage());
    }
}
