package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_values_Test {

    private TrackingMap<String, Integer> trackingMap;

    private Map<String, Integer> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a simple backing map and wrap it with TrackingMap
        backingMap = new HashMap<>();
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

    /* --------------------------------------------------------------
     *  Helper for parameterised tests (optional illustration)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> emptyAndPopulatedProvider() {
        Map<Integer, String> empty = Collections.emptyMap();
        Map<Integer, String> populated = new HashMap<>();
        populated.put(1, "Alice");
        populated.put(2, "Bob");
        return Stream.of(Arguments.of(empty, 0, Collections.emptyList()), Arguments.of(populated, 2, Arrays.asList("Alice", "Bob")));
    }

    @Test
    void values_ShouldReturnBackingMapValuesCollection() {
        // given – the trackingMap is already populated in setupBeforeEach
        // when
        Collection<Integer> values = trackingMap.values();
        // then
        assertNotNull(values, "The values collection should not be null");
        assertEquals(3, values.size(), "The values collection should contain exactly three elements");
        // The collection returned by Map.values() is a view, so changes to the backing map should be reflected
        backingMap.put("four", 4);
        assertEquals(4, values.size(), "The values view should reflect changes in the backing map");
        assertTrue(values.containsAll(Arrays.asList(1, 2, 3, 4)), "The values view should contain all values from the backing map");
    }

    @Test
    void values_EmptyMap_ReturnsEmptyCollection() {
        // given
        Map<String, Integer> empty = new HashMap<>();
        TrackingMap<String, Integer> emptyMap = new TrackingMap<>(empty);
        // when
        Collection<Integer> values = emptyMap.values();
        // then
        assertNotNull(values);
        assertTrue(values.isEmpty(), "Values collection should be empty for an empty map");
        assertEquals(0, values.size());
    }

    @Test
    void values_PopulatedMap_ReturnsAllValues() {
        // given
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "Alice");
        map.put(2, "Bob");
        TrackingMap<Integer, String> tm = new TrackingMap<>(map);
        // when
        Collection<String> values = tm.values();
        // then
        assertEquals(2, values.size());
        assertTrue(values.containsAll(Arrays.asList("Alice", "Bob")));
    }

    @Test
    void values_AddAfterFirstCall_CollectionReflectsNewEntry() {
        // given
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "Alice");
        map.put(2, "Bob");
        TrackingMap<Integer, String> tm = new TrackingMap<>(map);
        // first view
        Collection<String> values = tm.values();
        // when
        tm.put(3, "Charlie");
        // then
        assertEquals(3, values.size(), "Values view should grow after a new entry is added");
        assertTrue(values.contains("Charlie"));
    }

    @Test
    void values_RemoveKey_CollectionUpdates() {
        // given
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "Alice");
        map.put(2, "Bob");
        TrackingMap<Integer, String> tm = new TrackingMap<>(map);
        Collection<String> values = tm.values();
        // when
        tm.remove(1);
        // then
        assertEquals(1, values.size());
        assertFalse(values.contains("Alice"));
        assertTrue(values.contains("Bob"));
    }

    @Test
    void values_RemoveViaIterator_UpdatesUnderlyingMap() {
        // given
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "Alice");
        map.put(2, "Bob");
        TrackingMap<Integer, String> tm = new TrackingMap<>(map);
        Collection<String> values = tm.values();
        // when – remove "Bob" via iterator
        Iterator<String> it = values.iterator();
        while (it.hasNext()) {
            if ("Bob".equals(it.next())) {
                it.remove();
                break;
            }
        }
        // then
        assertEquals(1, values.size());
        assertFalse(values.contains("Bob"));
        assertEquals(1, tm.size(), "Map size must reflect the removal");
        assertFalse(tm.containsKey(2), "Key 2 should have been removed");
    }

    @Test
    void values_MultipleCalls_ReturnSameInstance() {
        // given – trackingMap already created in @BeforeEach
        // when
        Collection<Integer> first = trackingMap.values();
        Collection<Integer> second = trackingMap.values();
        // then
        assertSame(first, second, "Multiple calls to values() should return the identical view instance");
    }

    @Test
    void values_ModifyWrappedMap_ValuesViewSeesChange() {
        // given
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        TrackingMap<String, Integer> tm = new TrackingMap<>(map);
        Collection<Integer> values = tm.values();
        // when – modify via getWrappedMap()
        tm.getWrappedMap().put("c", 3);
        // then
        assertEquals(3, values.size());
        assertTrue(values.contains(3));
    }

    @Test
    void values_NullValue_PresentInCollection() {
        // given
        Map<Integer, String> map = new HashMap<>();
        map.put(1, null);
        map.put(2, "Bob");
        TrackingMap<Integer, String> tm = new TrackingMap<>(map);
        Collection<String> values = tm.values();
        // then
        assertEquals(2, values.size());
        assertTrue(values.contains(null), "Collection must contain the null value");
    }

    @Test
    void values_RemoveObject_ReturnsTrueAndUpdatesMap() {
        // given
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "X");
        map.put(2, "Y");
        TrackingMap<Integer, String> tm = new TrackingMap<>(map);
        Collection<String> values = tm.values();
        // when
        boolean removed = values.remove("X");
        // then
        assertTrue(removed, "remove(\"X\") should return true");
        assertEquals(1, tm.size());
        assertFalse(tm.containsKey(1));
        assertTrue(values.contains("Y"));
    }

    @Test
    void values_RemoveAll_RemovesMatchingEntries() {
        // given
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "A");
        map.put(2, "B");
        map.put(3, "C");
        TrackingMap<Integer, String> tm = new TrackingMap<>(map);
        Collection<String> values = tm.values();
        // when
        boolean changed = values.removeAll(Arrays.asList("A", "C"));
        // then
        assertTrue(changed, "removeAll should report that the collection changed");
        assertEquals(1, tm.size());
        assertTrue(tm.containsKey(2));
        assertEquals("B", tm.get(2));
        assertTrue(values.contains("B"));
        assertFalse(values.contains("A"));
        assertFalse(values.contains("C"));
    }

    @Test
    void values_RetainAll_KeepsOnlySpecifiedValues() {
        // given
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "M");
        map.put(2, "N");
        map.put(3, "O");
        TrackingMap<Integer, String> tm = new TrackingMap<>(map);
        Collection<String> values = tm.values();
        // when
        boolean changed = values.retainAll(Collections.singleton("N"));
        // then
        assertTrue(changed, "retainAll should report that the collection changed");
        assertEquals(1, tm.size());
        assertTrue(tm.containsKey(2));
        assertEquals("N", tm.get(2));
        assertEquals(Collections.singleton("N"), new HashSet<>(values));
    }

    @Test
    void values_AddUnsupported_ThrowsUOE() {
        // given
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "A");
        TrackingMap<Integer, String> tm = new TrackingMap<>(map);
        Collection<String> values = tm.values();
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> values.add("new"), "Adding directly to the values view must throw UnsupportedOperationException");
    }

    @Test
    void values_AfterClear_MapAndViewAreEmpty() {
        // given
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "A");
        map.put(2, "B");
        TrackingMap<Integer, String> tm = new TrackingMap<>(map);
        Collection<String> values = tm.values();
        // when
        tm.clear();
        // then
        assertTrue(values.isEmpty(), "Values view should be empty after map.clear()");
        assertEquals(0, tm.size());
    }

    @Test
    void values_ReflectsWrappedMapClear() {
        // given
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "A");
        map.put(2, "B");
        TrackingMap<Integer, String> tm = new TrackingMap<>(map);
        Collection<String> values = tm.values();
        // when
        tm.getWrappedMap().clear();
        // then
        assertTrue(values.isEmpty(), "Values view must reflect clear() on the wrapped map");
        assertEquals(0, tm.size());
    }

    @Test
    void values_InternalMapIsNull_ThrowsNullPointerException() throws Exception {
        // given – a normal instance
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        // use reflection to nullify the private internalMap field
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(tm, null);
        // when / then
        assertThrows(NullPointerException.class, tm::values, "Calling values() when internalMap is null should throw NullPointerException");
    }

    @ParameterizedTest
    @MethodSource("emptyAndPopulatedProvider")
    void values_ParameterizedTest(Map<Integer, String> initial, int expectedSize, List<String> expectedValues) {
        TrackingMap<Integer, String> tm = new TrackingMap<>(new HashMap<>(initial));
        Collection<String> values = tm.values();
        assertEquals(expectedSize, values.size());
        assertTrue(values.containsAll(expectedValues));
    }
}
