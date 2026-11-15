package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DayOfWeek;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_cloneMapOfMaps_Test {

    // Example of a method source for future expansion:
    static Stream<Arguments> emptyMapProvider() {
        return Stream.of(Arguments.of(new LinkedHashMap<>(), false), Arguments.of(new LinkedHashMap<>(), true));
    }

    @Test
    void testCloneMapOfMapsMutable() {
        // Arrange – create a map of maps with distinct inner map instances
        Map<String, Map<Integer, String>> original = new LinkedHashMap<>();
        Map<Integer, String> inner1 = new LinkedHashMap<>();
        inner1.put(1, "one");
        inner1.put(2, "two");
        Map<Integer, String> inner2 = new LinkedHashMap<>();
        inner2.put(3, "three");
        original.put("first", inner1);
        original.put("second", inner2);
        // Act – clone mutable (immutable = false)
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);
        // Assert – the outer map is a new instance
        assertNotSame(original, clone);
        assertEquals(original.size(), clone.size());
        // Each inner map should also be a new instance with equal content
        for (String key : original.keySet()) {
            Map<Integer, String> originalInner = original.get(key);
            Map<Integer, String> clonedInner = clone.get(key);
            assertNotSame(originalInner, clonedInner, "Inner map for key '" + key + "' should be a new instance");
            assertEquals(originalInner, clonedInner);
        }
        // Mutating the clone must not affect the original
        clone.get("first").put(99, "ninety‑nine");
        assertFalse(original.get("first").containsKey(99));
        // Mutating the original must not affect the clone
        original.get("second").put(100, "hundred");
        assertFalse(clone.get("second").containsKey(100));
    }

    @Test
    void testCloneMapOfMapsImmutable() {
        // Arrange – map of maps where inner maps are mutable before cloning
        Map<String, Map<Integer, String>> original = new LinkedHashMap<>();
        Map<Integer, String> inner = new LinkedHashMap<>();
        inner.put(10, "ten");
        original.put("key", inner);
        // Act – clone with immutable = true
        Map<String, Map<Integer, String>> immutableClone = MapUtilities.cloneMapOfMaps(original, true);
        // Assert – outer map is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> immutableClone.put("newKey", new HashMap<>()));
        // Inner map should be unmodifiable as well
        Map<Integer, String> clonedInner = immutableClone.get("key");
        assertThrows(UnsupportedOperationException.class, () -> clonedInner.put(20, "twenty"));
        // Because the inner map is a wrapped view of the original, changes to the original are reflected
        inner.put(30, "thirty");
        assertEquals("thirty", clonedInner.get(30));
    }

    @Test
    void testCloneMapOfMapsEmpty() {
        // Arrange – empty map
        Map<String, Map<Integer, String>> empty = new LinkedHashMap<>();
        // Act – mutable clone
        Map<String, Map<Integer, String>> mutableResult = MapUtilities.cloneMapOfMaps(empty, false);
        assertTrue(mutableResult.isEmpty());
        assertDoesNotThrow(() -> mutableResult.put("any", new HashMap<>()));
        // Act – immutable clone
        Map<String, Map<Integer, String>> immutableResult = MapUtilities.cloneMapOfMaps(empty, true);
        assertTrue(immutableResult.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> immutableResult.put("any", new HashMap<>()));
    }

    @Test
    void testCloneMapOfMapsNullOriginal() {
        // Null original should cause NPE when the method attempts to iterate over entrySet
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfMaps(null, false));
    }

    // -----------------------------------------------------------------
    @Test
    void testCloneEmptyMapMutable() {
        Map<String, Map<Integer, String>> original = new LinkedHashMap<>();
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);
        assertTrue(clone.isEmpty(), "Clone of empty map should be empty");
        assertDoesNotThrow(() -> clone.put("any", new HashMap<>()), "Mutable clone must allow modifications");
    }

    // -----------------------------------------------------------------
    @Test
    void testCloneNonEmptyMapMutableDeepCopy() {
        Map<String, Map<Integer, String>> original = Map.of("A", Map.of(1, "one", 2, "two"));
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);
        assertEquals(1, clone.size());
        assertTrue(clone.containsKey("A"));
        Map<Integer, String> innerOriginal = original.get("A");
        Map<Integer, String> innerClone = clone.get("A");
        // Verify deep copy (different instances)
        assertNotSame(innerOriginal, innerClone);
        assertEquals(innerOriginal, innerClone);
        // Modifying clone must not affect original
        innerClone.put(3, "three");
        assertFalse(innerOriginal.containsKey(3));
    }

    // -----------------------------------------------------------------
    @Test
    void testCloneNonEmptyMapImmutable() {
        Map<String, Map<Integer, String>> original = Map.of("A", Map.of(1, "one", 2, "two"));
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, true);
        // Outer map unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> clone.put("B", new HashMap<>()));
        // Inner map unmodifiable
        Map<Integer, String> inner = clone.get("A");
        assertThrows(UnsupportedOperationException.class, () -> inner.put(3, "three"));
    }

    // -----------------------------------------------------------------
    @Test
    void testClonePreservesInsertionOrder() {
        LinkedHashMap<String, Map<Integer, String>> ordered = new LinkedHashMap<>();
        ordered.put("first", Map.of(1, "one"));
        ordered.put("second", Map.of(2, "two"));
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(ordered, false);
        assertIterableEquals(List.of("first", "second"), new ArrayList<>(clone.keySet()), "Insertion order must be preserved");
    }

    // -----------------------------------------------------------------
    @Test
    void testCloneMapWithNullInnerMapThrowsNPE() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        original.put("A", null);
        // immutable = false
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfMaps(original, false), "Cloning with a null inner map should throw NPE (mutable case)");
        // immutable = true
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfMaps(original, true), "Cloning with a null inner map should throw NPE (immutable case)");
    }

    // -----------------------------------------------------------------
    @Test
    void testCloneTreeMapProducesLinkedHashMapWithSortedOrder() {
        TreeMap<String, Map<Integer, String>> treeMap = new TreeMap<>();
        treeMap.put("b", Map.of(2, "two"));
        treeMap.put("a", Map.of(1, "one"));
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(treeMap, false);
        // Result should be a LinkedHashMap preserving the iteration order of the source (sorted)
        assertIterableEquals(List.of("a", "b"), new ArrayList<>(clone.keySet()), "Clone of TreeMap must preserve natural key order");
    }

    // -----------------------------------------------------------------
    @Test
    void testCloneEnumMapImmutablePreservesEnumOrder() {
        EnumMap<DayOfWeek, Map<Integer, String>> enumMap = new EnumMap<>(DayOfWeek.class);
        enumMap.put(DayOfWeek.MONDAY, Map.of(1, "one"));
        enumMap.put(DayOfWeek.FRIDAY, Map.of(5, "five"));
        Map<DayOfWeek, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(enumMap, true);
        // Outer map should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> clone.put(DayOfWeek.SUNDAY, new HashMap<>()));
        // Verify iteration order matches enum declaration order (MONDAY then FRIDAY)
        assertIterableEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY), new ArrayList<>(clone.keySet()), "EnumMap order must be preserved");
        // Inner maps must also be unmodifiable
        Map<Integer, String> inner = clone.get(DayOfWeek.MONDAY);
        assertThrows(UnsupportedOperationException.class, () -> inner.put(2, "two"));
    }

    // -----------------------------------------------------------------
    @Test
    void testCloneFromUnmodifiableOuterMapMutableResult() {
        Map<String, Map<Integer, String>> source = Collections.unmodifiableMap(Map.of("X", Map.of(10, "ten")));
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(source, false);
        // Result must be mutable regardless of source mutability
        assertDoesNotThrow(() -> clone.put("Y", new HashMap<>()), "Resulting outer map should be mutable");
        Map<Integer, String> inner = clone.get("X");
        assertDoesNotThrow(() -> inner.put(20, "twenty"), "Resulting inner map should be mutable");
    }

    // -----------------------------------------------------------------
    @Test
    void testCloneWhenInnerMapsAlreadyUnmodifiable() {
        Map<String, Map<Integer, String>> source = Map.of("Y", Collections.unmodifiableMap(Map.of(20, "twenty")));
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(source, true);
        // Outer map must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> clone.put("Z", new HashMap<>()));
        // Inner map must remain unmodifiable (wrapping an already unmodifiable map is still unmodifiable)
        Map<Integer, String> inner = clone.get("Y");
        assertThrows(UnsupportedOperationException.class, () -> inner.put(30, "thirty"));
    }

    // -----------------------------------------------------------------
    @Test
    void testCloneMultipleEntriesDeepCopyAndOrder() {
        LinkedHashMap<String, Map<Integer, String>> original = new LinkedHashMap<>() {

            {
                put("first", new LinkedHashMap<>(Map.of(1, "one")));
                put("second", new LinkedHashMap<>(Map.of(2, "two")));
            }
        };
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);
        // Verify order is preserved
        assertIterableEquals(List.of("first", "second"), new ArrayList<>(clone.keySet()));
        // Verify each inner map is a distinct new instance
        for (String key : original.keySet()) {
            Map<Integer, String> originalInner = original.get(key);
            Map<Integer, String> clonedInner = clone.get(key);
            assertNotSame(originalInner, clonedInner, "Inner map for key '" + key + "' must be a new instance");
            assertEquals(originalInner, clonedInner);
        }
        // Mutate clone and ensure original stays unchanged
        clone.get("first").put(99, "ninety‑nine");
        assertFalse(original.get("first").containsKey(99));
    }
}
