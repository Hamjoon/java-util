package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class TrackingMap_values_Test {

    private TrackingMap<String, Integer> trackingMap;

    private Map<String, Integer> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple LinkedHashMap as the backing map for deterministic ordering
        backingMap = new LinkedHashMap<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC and ensure isolation between tests
        trackingMap = null;
        backingMap = null;
    }

    /* -------------------------------------------------------------
     * Helper for parameterized basic behavior (optional demonstration)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> basicBehaviorProvider() {
        return Stream.of(Arguments.of(new HashMap<String, String>(), Collections.emptyList()), Arguments.of(mapOf("k1", "one"), Collections.singletonList("one")), Arguments.of(mapOf("k1", "one", "k2", "two", "k3", "three"), Arrays.asList("one", "two", "three")));
    }

    // Simple utility to create a map inline (Java 9+ Map.of cannot be used with varargs for >10 entries)
    private static Map<String, String> mapOf(Object... entries) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put((String) entries[i], (String) entries[i + 1]);
        }
        return map;
    }

    @Test
    void valuesReturnsAllCurrentValues() {
        Collection<Integer> values = trackingMap.values();
        // The collection should contain exactly the values we put in the backing map
        assertEquals(3, values.size());
        assertTrue(values.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    void valuesReflectsChangesInBackingMap() {
        // Modify the map after obtaining the collection
        Collection<Integer> values = trackingMap.values();
        // Add a new entry via the TrackingMap API
        trackingMap.put("four", 4);
        assertTrue(values.contains(4), "Values collection should reflect newly added entry");
        // Remove an entry
        trackingMap.remove("two");
        assertFalse(values.contains(2), "Values collection should no longer contain a removed entry");
    }

    @Test
    void valuesIsLiveViewNotSnapshot() {
        Collection<Integer> values = trackingMap.values();
        // Clear the map via the TrackingMap API
        trackingMap.clear();
        // The collection should now be empty because it is a live view of the internal map
        assertTrue(values.isEmpty(), "Values collection should be empty after clearing the map");
    }

    @Test
    void valuesDoesNotAllowModificationWhenBackedByUnmodifiableMap() {
        // Create a TrackingMap wrapping an unmodifiable map
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(new HashMap<>(backingMap));
        TrackingMap<String, Integer> unmodifiableTracking = new TrackingMap<>(unmodifiable);
        Collection<Integer> values = unmodifiableTracking.values();
        // Attempting to modify the collection should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> values.remove(1));
        assertThrows(UnsupportedOperationException.class, () -> values.add(5));
    }

    @Test
    void valuesReflectsNullValues() {
        trackingMap.put("nullKey", null);
        Collection<Integer> values = trackingMap.values();
        assertEquals(4, values.size());
        assertTrue(values.contains(null), "Values collection should contain null entries");
    }

    /**
     * 1. Basic behavior – empty, single and multiple entries **
     */
    @Test
    void valuesEmptyMapReturnsEmptyCollection() {
        TrackingMap<String, String> empty = new TrackingMap<>(new HashMap<>());
        Collection<String> values = empty.values();
        assertTrue(values.isEmpty(), "Values of an empty map should be empty");
    }

    @Test
    void valuesSingleEntryContainsExactlyThatValue() {
        Map<String, String> map = new HashMap<>();
        map.put("k1", "one");
        TrackingMap<String, String> tm = new TrackingMap<>(map);
        Collection<String> values = tm.values();
        assertEquals(1, values.size());
        assertTrue(values.contains("one"));
    }

    @Test
    void valuesMultipleEntriesContainAllValues() {
        Map<String, String> map = new HashMap<>();
        map.put("k1", "one");
        map.put("k2", "two");
        map.put("k3", "three");
        TrackingMap<String, String> tm = new TrackingMap<>(map);
        Collection<String> values = tm.values();
        assertEquals(3, values.size());
        assertTrue(values.containsAll(Arrays.asList("one", "two", "three")));
    }

    /**
     * 2. View reflects direct removal via the collection **
     */
    @Test
    void valuesRemoveThroughCollectionUpdatesUnderlyingMap() {
        Map<String, String> map = new HashMap<>();
        map.put("k1", "one");
        TrackingMap<String, String> tm = new TrackingMap<>(map);
        Collection<String> values = tm.values();
        // Removing the value should remove the corresponding entry from the map
        assertTrue(values.remove("one"));
        assertTrue(values.isEmpty(), "Collection should be empty after removal");
        assertEquals(0, tm.size(), "Underlying map should be empty after removal via values()");
    }

    /**
     * 3. Iterator removal **
     */
    @Test
    void valuesIteratorRemoveEliminatesEntryFromMap() {
        Collection<Integer> values = trackingMap.values();
        Iterator<Integer> it = values.iterator();
        // Advance iterator to the element "2" (value of key "two")
        while (it.hasNext()) {
            Integer val = it.next();
            if (val.equals(2)) {
                // remove via iterator
                it.remove();
                break;
            }
        }
        // After removal, map size should be 2 and collection should not contain 2
        assertEquals(2, trackingMap.size());
        assertFalse(trackingMap.values().contains(2));
    }

    /**
     * 4. Unsupported add operation **
     */
    @Test
    void valuesAddThrowsUnsupportedOperationException() {
        Collection<Integer> values = trackingMap.values();
        assertThrows(UnsupportedOperationException.class, () -> values.add(99));
    }

    /**
     * 5. Same instance on repeated calls **
     */
    @Test
    void valuesRepeatedCallsReturnSameInstance() {
        Collection<Integer> first = trackingMap.values();
        Collection<Integer> second = trackingMap.values();
        assertSame(first, second, "Consecutive calls to values() must return the same collection instance");
    }

    /**
     * 6. After clear **
     */
    @Test
    void valuesAfterClearIsEmpty() {
        trackingMap.clear();
        Collection<Integer> values = trackingMap.values();
        assertTrue(values.isEmpty(), "Values collection must be empty after map.clear()");
    }

    /**
     * 7. After putAll **
     */
    @Test
    void valuesAfterPutAllContainsAllInsertedValues() {
        Map<String, String> toPut = new HashMap<>();
        toPut.put("a", "alpha");
        toPut.put("b", "beta");
        toPut.put("c", "gamma");
        TrackingMap<String, String> tm = new TrackingMap<>(new HashMap<>());
        tm.putAll(toPut);
        Collection<String> values = tm.values();
        assertEquals(3, values.size());
        assertTrue(values.containsAll(Arrays.asList("alpha", "beta", "gamma")));
    }

    /**
     * 8. After wrapping a different map via setWrappedMap **
     */
    @Test
    void valuesAfterSetWrappedMapReflectsNewMap() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        Map<String, Integer> newMap = new HashMap<>();
        newMap.put("x", 1);
        newMap.put("y", 2);
        tm.setWrappedMap(newMap);
        Collection<Integer> values = tm.values();
        assertEquals(2, values.size());
        assertTrue(values.containsAll(Arrays.asList(1, 2)));
    }

    /**
     * 9. Null internal map should cause NPE **
     */
    @Test
    void valuesWhenInternalMapIsNullThrowsNullPointerException() throws Exception {
        // Use reflection to force the internalMap field to null (setWrappedMap(null) may already do this)
        trackingMap.setWrappedMap(null);
        assertThrows(NullPointerException.class, () -> trackingMap.values());
    }

    @ParameterizedTest
    @MethodSource("basicBehaviorProvider")
    void valuesParameterizedBasicBehavior(Map<String, String> map, List<String> expectedValues) {
        TrackingMap<String, String> tm = new TrackingMap<>(map);
        Collection<String> values = tm.values();
        assertEquals(expectedValues.size(), values.size());
        assertTrue(values.containsAll(expectedValues));
    }
}
