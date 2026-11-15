package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiFunction;

@DisplayName("AbstractConcurrentNullSafeMap#values() tests")
class AbstractConcurrentNullSafeMap_values_Test {

    private TestMap map;

    /**
     * Simple concrete implementation for testing purposes.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a fresh ConcurrentHashMap for each test to guarantee isolation.
        ConcurrentMap<Object, Object> internal = new ConcurrentHashMap<>();
        map = new TestMap(internal);
        // Populate with a mixture of normal and null entries.
        map.put("a", "alpha");
        map.put("b", "beta");
        // null key, non‑null value
        map.put(null, "nullKey");
        // non‑null key, null value
        map.put("c", null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared after each test to avoid cross‑test contamination.
        map.clear();
    }

    /**
     * Helper to create a map backed by a deterministic ConcurrentMap.
     */
    private static TestMap<String, Integer> createIntegerMap() {
        // ConcurrentSkipListMap provides deterministic ordering based on keys.
        ConcurrentMap<Object, Object> internal = new ConcurrentSkipListMap<>();
        TestMap<String, Integer> intMap = new TestMap<>(internal);
        intMap.put("a", 1);
        intMap.put("b", null);
        intMap.put("c", 2);
        return intMap;
    }

    @Test
    @DisplayName("values() iterator returns unmasked values, including null")
    void valuesIteratorShouldUnmaskNullValues() {
        Collection<String> values = map.values();
        assertNotNull(values, "values() must not return null");
        // Collect values via iterator.
        Iterator<String> it = values.iterator();
        // Use a simple loop to gather all values.
        java.util.Set<String> result = new java.util.HashSet<>();
        while (it.hasNext()) {
            result.add(it.next());
        }
        // Expected set contains the original values, with null preserved.
        java.util.Set<String> expected = java.util.Set.of("alpha", "beta", "nullKey", null);
        assertEquals(expected, result, "Iterator should return the unmasked values");
    }

    @Test
    @DisplayName("values() size reflects underlying map size")
    void valuesSizeShouldMatchInternalSize() {
        Collection<String> values = map.values();
        assertEquals(4, values.size(), "Size should equal number of entries in the map");
    }

    @Test
    @DisplayName("values() contains works for normal and null values")
    void valuesContainsShouldHandleNullAndNonNull() {
        Collection<String> values = map.values();
        assertTrue(values.contains("alpha"), "Should contain existing non‑null value");
        assertTrue(values.contains(null), "Should contain null value");
        assertFalse(values.contains("nonexistent"), "Should not contain value that was never added");
    }

    @Test
    @DisplayName("values() clear empties the map")
    void valuesClearShouldEmptyMap() {
        Collection<String> values = map.values();
        assertFalse(values.isEmpty(), "Precondition: map should not be empty");
        values.clear();
        assertTrue(map.isEmpty(), "Map should be empty after clear via values collection");
        assertEquals(0, values.size(), "Values collection size should be zero after clear");
    }

    @Test
    @DisplayName("values() size reflects all entries including null values")
    void valuesBasicSize() {
        TestMap<String, Integer> intMap = createIntegerMap();
        Collection<Integer> values = intMap.values();
        assertEquals(3, values.size(), "size == 3");
    }

    @Test
    @DisplayName("values() iterator yields values in the same order as internal map")
    void valuesIteratorOrder() {
        TestMap<String, Integer> intMap = createIntegerMap();
        // Capture order from the underlying map directly.
        List<Integer> expected = new ArrayList<>();
        for (Object o : intMap.internalMap.values()) {
            expected.add(intMap.unmaskNullValue(o));
        }
        // Collect order from the values() iterator.
        List<Integer> actual = new ArrayList<>();
        for (Integer v : intMap.values()) {
            actual.add(v);
        }
        assertEquals(expected, actual, "iterator yields [1, null, 2] in the same order as the internal map's values iterator");
    }

    @Test
    @DisplayName("values() contains returns true for existing non‑null value")
    void valuesContainsExistingNonNull() {
        TestMap<String, Integer> intMap = createIntegerMap();
        assertTrue(intMap.values().contains(1), "contains(1) == true");
    }

    @Test
    @DisplayName("values() contains returns true for existing null value")
    void valuesContainsExistingNull() {
        TestMap<String, Integer> intMap = createIntegerMap();
        assertTrue(intMap.values().contains(null), "contains(null) == true");
    }

    @Test
    @DisplayName("values() contains returns false for absent value")
    void valuesContainsAbsent() {
        TestMap<String, Integer> intMap = createIntegerMap();
        assertFalse(intMap.values().contains(99), "contains(99) == false");
    }

    @Test
    @DisplayName("values() contains returns false for type‑mismatched value")
    void valuesContainsTypeMismatch() {
        TestMap<String, Integer> intMap = createIntegerMap();
        assertFalse(intMap.values().contains("string"), "contains(\"string\") == false");
    }

    @Test
    @DisplayName("values() clear empties collection and underlying map")
    void valuesClearBehavior() {
        TestMap<String, Integer> intMap = createIntegerMap();
        Collection<Integer> values = intMap.values();
        values.clear();
        assertEquals(0, values.size(), "size == 0 after clear");
        assertTrue(intMap.isEmpty(), "underlying map must be empty");
        assertFalse(values.contains(1), "contains(1) == false after clear");
        assertFalse(values.iterator().hasNext(), "iterator.hasNext() == false after clear");
    }

    @Test
    @DisplayName("iterator.remove() after next() removes the corresponding entry")
    void iteratorRemoveOnce() {
        TestMap<String, Integer> intMap = createIntegerMap();
        Iterator<Integer> it = intMap.values().iterator();
        assertTrue(it.hasNext());
        // should be 1
        Integer first = it.next();
        assertEquals(1, first);
        // remove entry with value 1 (key "a")
        it.remove();
        assertEquals(2, intMap.values().size(), "size == 2 after removal");
        assertFalse(intMap.containsKey("a"), "containsKey(\"a\") == false after remove");
    }

    @Test
    @DisplayName("iterator.remove() before next() throws IllegalStateException")
    void iteratorRemoveBeforeNext() {
        TestMap<String, Integer> intMap = createIntegerMap();
        Iterator<Integer> it = intMap.values().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException");
    }

    @Test
    @DisplayName("consecutive iterator.remove() calls throw IllegalStateException")
    void iteratorRemoveConsecutiveCalls() {
        TestMap<String, Integer> intMap = createIntegerMap();
        Iterator<Integer> it = intMap.values().iterator();
        assertTrue(it.hasNext());
        // first element
        it.next();
        // first removal OK
        it.remove();
        assertThrows(IllegalStateException.class, it::remove, "Second remove() without intervening next() must throw IllegalStateException");
    }

    @Test
    @DisplayName("values() size updates after adding a new entry")
    void valuesSizeAfterPut() {
        TestMap<String, Integer> intMap = createIntegerMap();
        intMap.put("d", 3);
        assertEquals(4, intMap.values().size(), "size == 4 after adding entry \"d\"->3");
    }

    @Test
    @DisplayName("values() size updates after removing an entry via map.remove")
    void valuesSizeAfterMapRemove() {
        TestMap<String, Integer> intMap = createIntegerMap();
        // removes entry with value 2
        intMap.remove("c");
        assertEquals(2, intMap.values().size(), "size == 2 after removing entry \"c\"");
    }

    @Test
    @DisplayName("values() contains(null) returns false when no null values are present")
    void valuesContainsNullWhenAbsent() {
        TestMap<String, Integer> intMap = createIntegerMap();
        // remove the entry that has null value
        intMap.remove("b");
        assertFalse(intMap.values().contains(null), "contains(null) == false when no null values");
    }

    @Test
    @DisplayName("values() contains(null) returns true after inserting a null value")
    void valuesContainsNullAfterInsert() {
        TestMap<String, Integer> intMap = createIntegerMap();
        intMap.put("e", null);
        assertTrue(intMap.values().contains(null), "contains(null) == true after inserting null value");
    }

    @Test
    @DisplayName("iterator.next() on empty collection throws NoSuchElementException")
    void iteratorNextOnEmptyThrows() {
        // Empty map
        TestMap<String, Integer> emptyMap = new TestMap<>(new ConcurrentSkipListMap<>());
        Iterator<Integer> it = emptyMap.values().iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next, "Calling next() without hasNext() must throw NoSuchElementException");
    }

    @Test
    @DisplayName("iterator.remove() propagates UnsupportedOperationException from underlying iterator")
    void iteratorRemoveUnsupportedOperation() {
        // Mock a ConcurrentMap whose values iterator does not support remove()
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> internalMock = mock(ConcurrentMap.class);
        @SuppressWarnings("unchecked")
        Collection<Object> valuesCollMock = mock(Collection.class);
        @SuppressWarnings("unchecked")
        Iterator<Object> iteratorMock = mock(Iterator.class);
        when(internalMock.values()).thenReturn(valuesCollMock);
        when(valuesCollMock.iterator()).thenReturn(iteratorMock);
        when(iteratorMock.hasNext()).thenReturn(true);
        // any object
        when(iteratorMock.next()).thenReturn("sentinel");
        doThrow(new UnsupportedOperationException()).when(iteratorMock).remove();
        TestMap<String, String> mapWithUnsupportedRemove = new TestMap<>(internalMock);
        // Populate via put to ensure internal map has something (masking is irrelevant here)
        mapWithUnsupportedRemove.put("x", "y");
        Iterator<String> it = mapWithUnsupportedRemove.values().iterator();
        assertTrue(it.hasNext());
        // advance to element
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove, "remove() should propagate UnsupportedOperationException from underlying iterator");
    }
}
