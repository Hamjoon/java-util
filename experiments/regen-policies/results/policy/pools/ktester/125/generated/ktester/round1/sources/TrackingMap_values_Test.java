package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

class TrackingMap_values_Test {

    private TrackingMap<String, Integer> trackingMap;

    private Map<String, Integer> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        backingMap = new HashMap<>();
        // Populate the backing map with deterministic data
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        trackingMap = null;
        backingMap = null;
    }

    @Test
    void valuesReflectCurrentBackingMap() {
        Collection<Integer> values = trackingMap.values();
        // The collection should contain exactly the values we inserted
        assertEquals(3, values.size(), "values() should contain three elements");
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
    }

    @Test
    void valuesCollectionIsLiveView() {
        Collection<Integer> values = trackingMap.values();
        // Remove an element via the collection's iterator
        Iterator<Integer> it = values.iterator();
        assertTrue(it.hasNext(), "Iterator should have at least one element");
        // remove the first element
        it.next();
        it.remove();
        // The underlying map must reflect the removal
        assertEquals(2, trackingMap.size(), "Map size should shrink after removal via values()");
        assertEquals(2, values.size(), "Values collection size should shrink accordingly");
    }

    @Test
    void valuesAfterExpungeUnusedWhenNoAccesses() {
        // No calls to get() or containsKey() – all keys are considered unused
        trackingMap.expungeUnused();
        Collection<Integer> values = trackingMap.values();
        assertTrue(values.isEmpty(), "values() should be empty after expungeUnused with no accesses");
        assertTrue(trackingMap.isEmpty(), "Map itself should be empty after expungeUnused");
    }

    @Test
    void valuesAfterExpungeUnusedWhenSomeKeysAccessed() {
        // Access a subset of keys
        trackingMap.get("one");
        trackingMap.containsKey("two");
        // "three" remains unused
        trackingMap.expungeUnused();
        Collection<Integer> values = trackingMap.values();
        assertEquals(2, values.size(), "Only accessed entries should remain");
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertFalse(values.contains(3));
        assertEquals(2, trackingMap.size());
    }

    @Test
    void valuesOnEmptyMap() {
        // Clear the map first
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty(), "Pre‑condition: map should be empty");
        Collection<Integer> values = trackingMap.values();
        assertNotNull(values, "values() must never return null");
        assertTrue(values.isEmpty(), "values() of an empty map should be empty");
    }

    @Test
    void valuesEmptyMapReturnsEmptyCollection() {
        // Arrange: start from a fresh empty TrackingMap
        TrackingMap<String, String> emptyMap = new TrackingMap<>(new HashMap<>());
        // Act
        Collection<String> values = emptyMap.values();
        // Assert
        assertNotNull(values, "values() must never return null");
        assertTrue(values.isEmpty(), "When the internal map is empty, values() should return an empty collection.");
    }

    @Test
    void valuesNonEmptyMapContainsExactValues() {
        TrackingMap<String, String> map = new TrackingMap<>(new HashMap<>());
        map.put("k1", "apple");
        map.put("k2", "banana");
        Collection<String> values = map.values();
        assertEquals(2, values.size(), "Expected exactly two values");
        assertTrue(values.containsAll(Arrays.asList("apple", "banana")), "After putting two entries the values() collection should contain exactly those two values.");
    }

    @Test
    void valuesReflectMapRemovalAfterObtainingCollection() {
        TrackingMap<String, String> map = new TrackingMap<>(new HashMap<>());
        map.put("k1", "apple");
        map.put("k2", "banana");
        Collection<String> values = map.values();
        // Remove entry "k1" directly from the map
        map.remove("k1");
        // The previously obtained collection must reflect the change
        assertEquals(1, values.size(), "Collection size should shrink after map removal");
        assertTrue(values.contains("banana"));
        assertFalse(values.contains("apple"));
    }

    @Test
    void valuesRemovalViaIteratorRemovesCorrespondingEntry() {
        TrackingMap<String, String> map = new TrackingMap<>(new HashMap<>());
        map.put("k1", "apple");
        map.put("k2", "banana");
        Collection<String> values = map.values();
        Iterator<String> it = values.iterator();
        // Advance to first element and remove it
        assertTrue(it.hasNext());
        // "apple"
        it.next();
        it.remove();
        // Verify map state
        assertEquals(1, map.size(), "Map should contain only one entry after iterator removal");
        assertTrue(map.containsKey("k2"));
        assertFalse(map.containsKey("k1"));
        assertEquals(Collections.singleton("banana"), new HashSet<>(map.values()));
    }

    @Test
    void valuesUnsupportedAddOperationThrows() {
        Collection<Integer> values = trackingMap.values();
        assertThrows(UnsupportedOperationException.class, () -> values.add(99), "Attempt to add a new element to the collection returned by values() must throw UnsupportedOperationException.");
    }

    @Test
    void valuesContainsNullValue() {
        TrackingMap<String, String> map = new TrackingMap<>(new HashMap<>());
        map.put("k3", null);
        Collection<String> values = map.values();
        assertEquals(1, values.size(), "Collection should contain a single element");
        assertTrue(values.contains(null), "The values() collection should contain the null element.");
    }

    @Test
    void valuesLiveViewAfterClear() {
        TrackingMap<String, String> map = new TrackingMap<>(new HashMap<>());
        map.put("k1", "apple");
        map.put("k2", "banana");
        Collection<String> values = map.values();
        // Clear the map after obtaining the collection
        map.clear();
        assertTrue(values.isEmpty(), "After clear() the collection view must become empty.");
        assertTrue(map.isEmpty(), "Map itself should be empty after clear().");
    }

    @Test
    void valuesDuplicateValuesHandling() {
        TrackingMap<String, String> map = new TrackingMap<>(new HashMap<>());
        map.put("k4", "dup");
        map.put("k5", "dup");
        Collection<String> values = map.values();
        // The collection should contain "dup" twice (order not guaranteed)
        assertEquals(2, values.size(), "Two entries with duplicate values should be present.");
        List<String> valueList = new ArrayList<>(values);
        assertEquals("dup", valueList.get(0));
        assertEquals("dup", valueList.get(1));
    }

    @Test
    void iteratorNextOnEmptyMapThrowsNoSuchElementException() {
        TrackingMap<String, String> emptyMap = new TrackingMap<>(new HashMap<>());
        Iterator<String> it = emptyMap.values().iterator();
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an iterator of an empty values() collection must throw NoSuchElementException.");
    }

    @Test
    void iteratorRemoveWithoutPriorNextThrowsIllegalStateException() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        Iterator<Integer> it = map.values().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() must throw IllegalStateException.");
    }

    @Test
    void iteratorDoubleRemoveThrowsIllegalStateException() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        map.put("b", 2);
        Iterator<Integer> it = map.values().iterator();
        assertTrue(it.hasNext());
        // first element
        it.next();
        // first removal – ok
        it.remove();
        // Second removal without an intervening next()
        assertThrows(IllegalStateException.class, it::remove, "Second consecutive remove() must throw IllegalStateException.");
    }

    @Test
    void iteratorDetectsConcurrentModification() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("x", 10);
        map.put("y", 20);
        Iterator<Integer> it = map.values().iterator();
        // Structural modification after iterator creation
        map.put("z", 30);
        // The next call on the iterator should notice the change
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must detect concurrent modification and throw ConcurrentModificationException.");
    }

    @Test
    void valuesUnsupportedAddAllOperationThrows() {
        Collection<Integer> values = trackingMap.values();
        List<Integer> toAdd = Arrays.asList(4, 5, 6);
        assertThrows(UnsupportedOperationException.class, () -> values.addAll(toAdd), "Calling addAll on the collection returned by values() must throw UnsupportedOperationException.");
    }
}
