package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_isEmpty_Test {

    /**
     * A {@link Map} implementation whose {@code isEmpty()} deliberately throws
     * {@link UnsupportedOperationException}.
     */
    private static class FaultyMapUnsupported extends AbstractMap<Object, Object> {

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException("isEmpty not supported");
        }
    }

    /**
     * A {@link Map} implementation whose {@code isEmpty()} deliberately throws
     * {@link IllegalStateException}.
     */
    private static class FaultyMapIllegalState extends AbstractMap<Object, Object> {

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public boolean isEmpty() {
            throw new IllegalStateException("illegal state");
        }
    }

    /**
     * A {@link Map} implementation whose {@code isEmpty()} deliberately throws
     * {@link NullPointerException}.
     */
    private static class FaultyMapNullPointer extends AbstractMap<Object, Object> {

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public boolean isEmpty() {
            throw new NullPointerException("null pointer inside isEmpty");
        }
    }

    /**
     * Supplies a combination of map instances and the expected {@code isEmpty} result.
     *
     * <p>All maps are created fresh for each argument to avoid cross‑test contamination.
     */
    private static Stream<Arguments> mapProvider() {
        return Stream.of(// Empty maps
        Arguments.of(new HashMap<>(), true, "Empty HashMap"), Arguments.of(Collections.singletonMap("key", "value"), false, "Singleton map (HashMap backing)"), Arguments.of(new EnumMap<>(java.time.DayOfWeek.class), true, "Empty EnumMap"), Arguments.of(Collections.unmodifiableMap(new HashMap<>()), true, "Unmodifiable empty HashMap"), Arguments.of(Collections.unmodifiableMap(Collections.singletonMap(1, 2)), false, "Unmodifiable singleton map"), Arguments.of(new TreeMap<>(), true, "Empty TreeMap"), Arguments.of(Collections.unmodifiableSortedMap(new TreeMap<>(Collections.singletonMap("a", "b"))), false, "Unmodifiable non‑empty SortedMap"), Arguments.of(new LinkedHashMap<>(), true, "Empty LinkedHashMap"), Arguments.of(Collections.unmodifiableMap(new LinkedHashMap<>(Collections.singletonMap("k", "v"))), false, "Unmodifiable non‑empty LinkedHashMap"), Arguments.of(new Hashtable<>(), true, "Empty legacy Hashtable"), Arguments.of(new ConcurrentHashMap<>(), true, "Empty ConcurrentHashMap"), Arguments.of(Collections.emptyMap(), true, "Collections.emptyMap()"), Arguments.of(Collections.singletonMap(null, null), false, "Singleton map with null key/value"), Arguments.of(Map.of("a", 1, "b", 2), false, "Map.of() with multiple entries"));
    }

    @Test
    void shouldReturnTrueWhenMapIsNull() {
        assertTrue(MapUtilities.isEmpty(null), "isEmpty should return true for a null map");
    }

    @Test
    void shouldReturnTrueForEmptyMapsOfVariousImplementations() {
        // HashMap
        Map<String, Integer> hashMap = new HashMap<>();
        assertTrue(MapUtilities.isEmpty(hashMap), "isEmpty should return true for an empty HashMap");
        // TreeMap
        Map<String, Integer> treeMap = new TreeMap<>();
        assertTrue(MapUtilities.isEmpty(treeMap), "isEmpty should return true for an empty TreeMap");
        // LinkedHashMap
        Map<String, Integer> linkedHashMap = new LinkedHashMap<>();
        assertTrue(MapUtilities.isEmpty(linkedHashMap), "isEmpty should return true for an empty LinkedHashMap");
        // ConcurrentHashMap (if available in the JDK version)
        Map<String, Integer> concurrentMap = new java.util.concurrent.ConcurrentHashMap<>();
        assertTrue(MapUtilities.isEmpty(concurrentMap), "isEmpty should return true for an empty ConcurrentHashMap");
    }

    @Test
    void shouldReturnFalseWhenMapContainsElements() {
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertFalse(MapUtilities.isEmpty(map), "isEmpty should return false for a map that contains entries");
        // Add another entry of a different type to ensure generic handling
        map.put("anotherKey", "anotherValue");
        assertFalse(MapUtilities.isEmpty(map), "isEmpty should still return false after adding more entries");
    }

    @Test
    void shouldNotModifyTheSuppliedMap() {
        Map<String, Integer> original = new HashMap<>();
        original.put("one", 1);
        original.put("two", 2);
        // Capture the size and contents before calling isEmpty
        int sizeBefore = original.size();
        Set<Map.Entry<String, Integer>> entriesBefore = new HashSet<>(original.entrySet());
        // Invoke the method under test
        boolean result = MapUtilities.isEmpty(original);
        // Verify result is false and the map is unchanged
        assertFalse(result, "Result must be false for a non‑empty map");
        assertEquals(sizeBefore, original.size(), "Map size must remain unchanged");
        assertEquals(entriesBefore, new HashSet<>(original.entrySet()), "Map contents must remain unchanged");
    }

    @Test
    void shouldHandleMapsWithNullKeysOrValuesCorrectly() {
        Map<String, String> map = new HashMap<>();
        map.put(null, "nullKey");
        map.put("nullValue", null);
        assertFalse(MapUtilities.isEmpty(map), "A map containing null keys or values is still considered non‑empty");
    }

    @ParameterizedTest(name = "[{index}] {2} → expected empty={1}")
    @MethodSource("mapProvider")
    void shouldReturnExpectedResultForVariousMaps(Map<?, ?> map, boolean expected, String description) {
        // Act
        boolean actual = MapUtilities.isEmpty(map);
        // Assert
        assertEquals(expected, actual, () -> "MapUtilities.isEmpty failed for: " + description);
    }

    @Test
    void shouldPropagateUnsupportedOperationException() {
        Map<Object, Object> faulty = new FaultyMapUnsupported();
        assertThrows(UnsupportedOperationException.class, () -> MapUtilities.isEmpty(faulty), "UnsupportedOperationException must propagate unchanged");
    }

    @Test
    void shouldPropagateIllegalStateException() {
        Map<Object, Object> faulty = new FaultyMapIllegalState();
        assertThrows(IllegalStateException.class, () -> MapUtilities.isEmpty(faulty), "IllegalStateException must propagate unchanged");
    }

    @Test
    void shouldPropagateNullPointerException() {
        Map<Object, Object> faulty = new FaultyMapNullPointer();
        assertThrows(NullPointerException.class, () -> MapUtilities.isEmpty(faulty), "NullPointerException must propagate unchanged");
    }
}
