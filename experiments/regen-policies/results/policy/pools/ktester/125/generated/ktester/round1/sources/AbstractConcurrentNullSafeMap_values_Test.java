package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;

class AbstractConcurrentNullSafeMap_values_Test {

    private TestMap map;

    /**
     * Concrete map used for most tests – supports null keys/values.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Concrete map with Integer values – used to provoke a ClassCastException.
     */
    private static class IntValueMap extends AbstractConcurrentNullSafeMap<String, Integer> {

        IntValueMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestMap();
        // Populate the map with a mix of normal, null‑key and null‑value entries
        map.put("a", "alpha");
        // null key mapped to a non‑null value
        map.put(null, "nullKey");
        // non‑null key mapped to a null value
        map.put("b", null);
        map = new TestMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each test starts with a fresh map
        map.clear();
        map.clear();
    }

    @Test
    void valuesCollectionReflectsUnderlyingMap() {
        Collection<String> values = map.values();
        // The collection should contain exactly the three values we inserted
        assertEquals(3, values.size(), "Size of values collection should match map size");
        // Order is not guaranteed – verify presence of each expected element
        assertTrue(values.contains("alpha"));
        assertTrue(values.contains("nullKey"));
        assertTrue(values.contains(null));
    }

    @Test
    void iteratorUnmasksNullValues() {
        Collection<String> values = map.values();
        Iterator<String> it = values.iterator();
        // Gather values from iterator
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // Verify that the iterator produced the same elements as the collection
        assertEquals(3, iterated.size());
        assertTrue(iterated.contains("alpha"));
        assertTrue(iterated.contains("nullKey"));
        assertTrue(iterated.contains(null));
    }

    @Test
    void containsUsesMaskingLogic() {
        Collection<String> values = map.values();
        // Directly test the contains implementation
        assertTrue(values.contains("alpha"));
        assertTrue(values.contains("nullKey"));
        assertTrue(values.contains(null));
        // A value not present should return false
        assertFalse(values.contains("nonexistent"));
    }

    @Test
    void clearEmptiesBothCollectionAndUnderlyingMap() {
        Collection<String> values = map.values();
        // Ensure map is populated before clear
        assertFalse(values.isEmpty());
        assertFalse(map.isEmpty());
        // Clear via the values collection
        values.clear();
        // Both the collection view and the map itself must now be empty
        assertTrue(values.isEmpty(), "Values collection should be empty after clear()");
        assertTrue(map.isEmpty(), "Underlying map should be empty after clear() on values collection");
    }

    @Test
    void removeViaIteratorAffectsUnderlyingMap() {
        Collection<String> values = map.values();
        Iterator<String> it = values.iterator();
        // Remove the first element we encounter
        assertTrue(it.hasNext());
        String removed = it.next();
        it.remove();
        // The underlying map must no longer contain that value
        assertFalse(values.contains(removed));
        assertEquals(2, values.size());
        assertEquals(2, map.size());
    }

    @Test
    void sizeReturnsZeroForEmptyMap() {
        // No entries have been added.
        assertEquals(0, map.values().size(), "A freshly created map must report size 0 on its values collection");
    }

    @Test
    void iteratorNextThrowsNoSuchElementExceptionWhenEmpty() {
        Iterator<String> it = map.values().iterator();
        assertFalse(it.hasNext(), "Iterator should report no elements");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    @Test
    void containsNullWhenOnlyNonNullValuesPresent() {
        map.put("x", "value");
        assertFalse(map.values().contains(null), "contains(null) must be false when the map holds only non‑null values");
    }

    @Test
    void containsNullWhenMultipleNullValuesPresent() {
        map.put("k1", null);
        map.put("k2", null);
        Collection<String> values = map.values();
        assertTrue(values.contains(null), "contains(null) must be true when at least one null value exists");
        assertEquals(2, values.size(), "Size must reflect both null‑value entries");
    }

    @Test
    void containsWithDifferentObjectTypeReturnsFalse() {
        map.put("s", "string");
        assertFalse(map.values().contains(123), "contains(Object) must return false when the argument type is unrelated to the value type");
    }

    @Test
    void iteratorRemoveBeforeNextThrowsIllegalStateException() {
        Iterator<String> it = map.values().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException per Iterator contract");
    }

    @Test
    void clearAfterConcurrentModificationEmptiesMap() {
        // Populate map and obtain iterator
        map.put("a", "alpha");
        map.put("b", "beta");
        Iterator<String> it = map.values().iterator();
        // Advance iterator once
        assertTrue(it.hasNext());
        it.next();
        // Concurrently add a new entry
        map.put("c", "gamma");
        // Now clear via the collection view
        map.values().clear();
        // Both the map and the collection must be empty
        assertTrue(map.isEmpty(), "Map must be empty after clear()");
        assertEquals(0, map.values().size(), "Values collection must report size 0 after clear()");
        assertFalse(it.hasNext(), "Iterator must report no further elements after clear()");
    }

    @Test
    void iteratorRemovePropagatesUnsupportedOperationException() {
        // ConcurrentHashMap's values iterator does not support remove
        map.put("x", "y");
        Iterator<String> it = map.values().iterator();
        assertTrue(it.hasNext());
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove, "Underlying iterator's UnsupportedOperationException must propagate");
    }

    @Test
    void containsThrowsClassCastExceptionWhenArgumentCannotBeCastedToValueType() {
        IntValueMap intMap = new IntValueMap();
        intMap.put("one", 1);
        intMap.put("two", 2);
        // The values collection expects Integer, passing a String forces a cast failure.
        Collection<Integer> values = intMap.values();
        assertThrows(ClassCastException.class, () -> values.contains("not-an-integer"), "contains() must throw ClassCastException when the argument cannot be cast to V");
    }

    @Test
    void valuesThrowsNullPointerExceptionWhenInternalMapIsNull() throws Exception {
        // Subclass that initially receives a valid map, then we null‑out the field via reflection.
        TestMap brokenMap = new TestMap();
        // Use reflection to set the private final field 'internalMap' to null.
        Field field = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(brokenMap, null);
        // Invoking values() should now attempt internalMap.values() and throw NPE.
        assertThrows(NullPointerException.class, brokenMap::values, "Calling values() when internalMap is null must throw NullPointerException");
    }

    @ParameterizedTest(name = "contains(null) with {0} null‑value entries")
    @ValueSource(ints = { 1, 2, 3 })
    void containsNullWithMultipleNullEntries(int nullCount) {
        for (int i = 0; i < nullCount; i++) {
            map.put("k" + i, null);
        }
        Collection<String> values = map.values();
        assertTrue(values.contains(null), "contains(null) must be true when the map contains at least one null value");
        assertEquals(nullCount, values.size(), "Size of values collection must equal the number of inserted entries");
    }
}
