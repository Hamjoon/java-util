package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Method;
import java.util.stream.*;

@ExtendWith(MockitoExtension.class)
class MapUtilities_cloneMapOfMaps_Test {

    /**
     * Helper to create a mutable copy of a map (used for assertions).
     */
    private static <K, V> Map<K, V> mutableCopy(Map<K, V> src) {
        return new LinkedHashMap<>(src);
    }

    static Stream<Arguments> immutableTrueNormalMapProvider() {
        Map<String, Map<Integer, String>> original = Map.of("A", Map.of(1, "one", 2, "two"), "B", Map.of(3, "three"));
        return Stream.of(Arguments.of("Immutable true - normal map", original));
    }

    static Stream<Arguments> immutableFalseMutableCopyProvider() {
        Map<String, Map<String, Integer>> original = Map.of("X", Map.of("k1", 10, "k2", 20), "Y", Map.of("k3", 30));
        return Stream.of(Arguments.of("Immutable false - mutable copy", original));
    }

    static Stream<Arguments> emptyOriginalMapProvider() {
        return Stream.of(Arguments.of("Empty original map – immutable true", true), Arguments.of("Empty original map – immutable false", false));
    }

    static Stream<Arguments> nullOriginalMapProvider() {
        return Stream.of(Arguments.of("Null original map – immutable true", true), Arguments.of("Null original map – immutable false", false));
    }

    static Stream<Arguments> nullInnerMapProvider() {
        return Stream.of(Arguments.of("Original map contains null inner map – immutable true", true), Arguments.of("Original map contains null inner map – immutable false", false));
    }

    /**
     * Verify that a mutable clone creates new inner map instances.
     * Changes to the original map or its inner maps must not affect the cloned map,
     * and vice‑versa.
     */
    @Test
    void testCloneMapOfMapsMutable() {
        // Build original map of maps
        Map<String, Map<Integer, String>> original = new LinkedHashMap<>();
        Map<Integer, String> inner1 = new LinkedHashMap<>();
        inner1.put(1, "one");
        inner1.put(2, "two");
        original.put("first", inner1);
        Map<Integer, String> inner2 = new LinkedHashMap<>();
        inner2.put(3, "three");
        original.put("second", inner2);
        // Clone (mutable)
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);
        // Verify structure equality
        assertEquals(original, clone, "Cloned map should be equal to the original");
        // Verify that inner map objects are distinct (shallow copy of values only)
        assertNotSame(original.get("first"), clone.get("first"));
        assertNotSame(original.get("second"), clone.get("second"));
        // Mutate original inner map – should not affect clone
        original.get("first").put(99, "ninety‑nine");
        assertFalse(clone.get("first").containsKey(99), "Clone must not reflect changes made to original inner map");
        // Mutate clone inner map – should not affect original
        clone.get("second").put(88, "eighty‑eight");
        assertFalse(original.get("second").containsKey(88), "Original must not reflect changes made to cloned inner map");
    }

    /**
     * Verify that an immutable clone returns unmodifiable maps.
     * Both the outer map and each inner map must throw {@link UnsupportedOperationException}
     * when a mutating operation is attempted.
     */
    @Test
    void testCloneMapOfMapsImmutable() {
        // Build original map of maps
        Map<String, Map<Integer, String>> original = new LinkedHashMap<>();
        Map<Integer, String> inner = new LinkedHashMap<>();
        inner.put(1, "one");
        original.put("key", inner);
        // Clone (immutable)
        Map<String, Map<Integer, String>> immutableClone = MapUtilities.cloneMapOfMaps(original, true);
        // The outer map must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> immutableClone.put("newKey", new HashMap<>()));
        // The inner map must also be unmodifiable
        Map<Integer, String> clonedInner = immutableClone.get("key");
        assertThrows(UnsupportedOperationException.class, () -> clonedInner.put(2, "two"));
        // Ensure the content is the same as the original at the time of cloning
        assertEquals("one", clonedInner.get(1));
        assertEquals(1, clonedInner.size());
        // Original map remains mutable – verify we can still change it
        original.get("key").put(2, "two");
        assertEquals("two", original.get("key").get(2));
    }

    /**
     * Edge case: cloning an empty map should return an empty map,
     * and the immutability flag should still be honoured.
     */
    @Test
    void testCloneEmptyMap() {
        Map<String, Map<Integer, String>> empty = Collections.emptyMap();
        Map<String, Map<Integer, String>> mutableClone = MapUtilities.cloneMapOfMaps(empty, false);
        assertTrue(mutableClone.isEmpty());
        Map<String, Map<Integer, String>> immutableClone = MapUtilities.cloneMapOfMaps(empty, true);
        assertTrue(immutableClone.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> immutableClone.put("x", new HashMap<>()));
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("immutableTrueNormalMapProvider")
    void testImmutableTrueNormalMap(String description, Map<String, Map<Integer, String>> original) {
        Map<String, Map<Integer, String>> result = MapUtilities.cloneMapOfMaps(original, true);
        // Outer map must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> result.put("new", new HashMap<>()));
        // Verify each inner map is also unmodifiable and equal in content
        for (Map.Entry<String, Map<Integer, String>> e : original.entrySet()) {
            Map<Integer, String> inner = result.get(e.getKey());
            assertNotNull(inner, "Inner map should exist for key " + e.getKey());
            assertThrows(UnsupportedOperationException.class, () -> inner.put(999, "x"));
            assertEquals(e.getValue(), inner, "Inner map content must match original");
        }
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("immutableFalseMutableCopyProvider")
    void testImmutableFalseMutableCopy(String description, Map<String, Map<String, Integer>> original) {
        Map<String, Map<String, Integer>> result = MapUtilities.cloneMapOfMaps(original, false);
        // Outer map must be mutable and equal but not same reference
        assertEquals(original, result);
        assertNotSame(original, result);
        // Each inner map must be a new mutable LinkedHashMap instance
        for (String key : original.keySet()) {
            Map<String, Integer> srcInner = original.get(key);
            Map<String, Integer> clonedInner = result.get(key);
            assertNotSame(srcInner, clonedInner, "Inner map for key '" + key + "' must be a new instance");
            assertEquals(srcInner, clonedInner);
            // Mutate cloned inner map and verify source is untouched
            clonedInner.put("newKey", 999);
            assertFalse(srcInner.containsKey("newKey"));
        }
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("emptyOriginalMapProvider")
    void testEmptyOriginalMap(String description, boolean immutable) {
        Map<String, Map<Integer, String>> empty = Collections.emptyMap();
        Map<String, Map<Integer, String>> result = MapUtilities.cloneMapOfMaps(empty, immutable);
        assertTrue(result.isEmpty(), "Result should be empty");
        if (immutable) {
            assertThrows(UnsupportedOperationException.class, () -> result.put("x", new HashMap<>()));
        } else {
            // mutable – should allow put
            result.put("x", new HashMap<>());
            assertTrue(result.containsKey("x"));
        }
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("nullOriginalMapProvider")
    void testNullOriginalMap(String description, boolean immutable) {
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfMaps(null, immutable), "Method should throw NPE when original map is null");
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("nullInnerMapProvider")
    void testNullInnerMap(String description, boolean immutable) {
        Map<String, Map<Integer, String>> original = new LinkedHashMap<>() {

            {
                put("key", null);
            }
        };
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfMaps(original, immutable), "Method should propagate NPE when an inner map is null");
    }

    @Test
    void testImmutableFalseInnerMapOriginallyUnmodifiable() {
        Map<String, Map<Integer, String>> original = Map.of("A", Collections.unmodifiableMap(Map.of(1, "one", 2, "two")));
        Map<String, Map<Integer, String>> result = MapUtilities.cloneMapOfMaps(original, false);
        // Outer map must be mutable
        assertDoesNotThrow(() -> result.put("B", new HashMap<>()));
        // Inner map must be a new mutable copy
        Map<Integer, String> clonedInner = result.get("A");
        assertNotSame(original.get("A"), clonedInner);
        assertEquals(original.get("A"), clonedInner);
        assertDoesNotThrow(() -> clonedInner.put(3, "three"));
        // Original inner map remains unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> original.get("A").put(3, "three"));
    }

    @Test
    void testImmutableTrueInnerMapOriginallyUnmodifiable() {
        Map<String, Map<Integer, String>> original = Map.of("A", Collections.unmodifiableMap(Map.of(1, "one", 2, "two")));
        Map<String, Map<Integer, String>> result = MapUtilities.cloneMapOfMaps(original, true);
        // Outer map must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> result.put("B", new HashMap<>()));
        // Inner map must be unmodifiable (wrapper around the original unmodifiable map)
        Map<Integer, String> inner = result.get("A");
        assertThrows(UnsupportedOperationException.class, () -> inner.put(3, "three"));
        // Content must match
        assertEquals(original.get("A"), inner);
    }

    @Test
    void testOriginalMapWithNullKey() {
        Map<String, Map<Integer, String>> original = new LinkedHashMap<>() {

            {
                put(null, Map.of(1, "one"));
                put("notNull", Map.of(2, "two"));
            }
        };
        Map<String, Map<Integer, String>> result = MapUtilities.cloneMapOfMaps(original, false);
        assertTrue(result.containsKey(null), "Result must contain the null key");
        assertEquals(Map.of(1, "one"), result.get(null));
        // Ensure the inner map is a new mutable instance
        assertNotSame(original.get(null), result.get(null));
        // Mutate cloned inner map – original must stay unchanged
        result.get(null).put(99, "x");
        assertFalse(original.get(null).containsKey(99));
    }

    @Test
    void testInnerMapContainsNullKeyAndValue() {
        Map<String, Map<Integer, String>> original = Map.of("A", new LinkedHashMap<>() {

            {
                put(null, null);
                put(2, null);
            }
        });
        Map<String, Map<Integer, String>> result = MapUtilities.cloneMapOfMaps(original, false);
        Map<Integer, String> clonedInner = result.get("A");
        assertEquals(2, clonedInner.size());
        assertTrue(clonedInner.containsKey(null));
        assertNull(clonedInner.get(null));
        assertTrue(clonedInner.containsKey(2));
        assertNull(clonedInner.get(2));
        // Verify new instance
        assertNotSame(original.get("A"), clonedInner);
    }

    @Test
    void testSortedMapOrderPreservation() {
        // Build a TreeMap (SortedMap) for outer and inner maps
        TreeMap<String, Map<Integer, String>> original = new TreeMap<>() {

            {
                put("B", new TreeMap<Integer, String>() {

                    {
                        put(2, "two");
                        put(1, "one");
                    }
                });
                put("A", new TreeMap<Integer, String>() {

                    {
                        put(3, "three");
                    }
                });
            }
        };
        Map<String, Map<Integer, String>> result = MapUtilities.cloneMapOfMaps(original, false);
        // Verify outer map is a LinkedHashMap preserving natural order (A then B)
        List<String> outerKeys = new ArrayList<>(result.keySet());
        assertEquals(List.of("A", "B"), outerKeys, "Outer map iteration order must follow natural order of original TreeMap");
        // Verify each inner map is a LinkedHashMap preserving the iteration order of the source TreeMap
        Map<Integer, String> innerA = result.get("A");
        assertEquals(List.of(3), new ArrayList<>(innerA.keySet()));
        Map<Integer, String> innerB = result.get("B");
        assertEquals(List.of(1, 2), new ArrayList<>(innerB.keySet()));
        // Ensure the concrete class is LinkedHashMap, not TreeMap
        assertTrue(result instanceof LinkedHashMap, "Outer map should be a LinkedHashMap");
        assertTrue(innerA instanceof LinkedHashMap, "Inner map should be a LinkedHashMap");
        assertTrue(innerB instanceof LinkedHashMap, "Inner map should be a LinkedHashMap");
    }

    @Test
    void testImmutableTrueEmptyInnerMap() {
        Map<String, Map<Integer, String>> original = Map.of("A", Collections.emptyMap());
        Map<String, Map<Integer, String>> result = MapUtilities.cloneMapOfMaps(original, true);
        // Outer map unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> result.put("B", new HashMap<>()));
        // Inner map unmodifiable and empty
        Map<Integer, String> inner = result.get("A");
        assertTrue(inner.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> inner.put(1, "one"));
    }

    @Test
    void testImmutableFalseLargeMapStress() {
        // Build a map with 1,000 outer entries, each containing a 10‑entry inner map
        Map<String, Map<Integer, String>> original = IntStream.range(0, 1000).boxed().collect(Collectors.toMap(i -> "Key" + i, i -> IntStream.range(0, 10).boxed().collect(Collectors.toMap(j -> j, j -> "V" + j))));
        Map<String, Map<Integer, String>> result = MapUtilities.cloneMapOfMaps(original, false);
        // Verify outer size
        assertEquals(1000, result.size());
        // Spot‑check a few entries for correctness and independence
        for (String key : List.of("Key0", "Key500", "Key999")) {
            Map<Integer, String> srcInner = original.get(key);
            Map<Integer, String> clonedInner = result.get(key);
            assertNotSame(srcInner, clonedInner);
            assertEquals(srcInner, clonedInner);
            // Mutate cloned inner map and ensure source is unchanged
            clonedInner.put(999, "new");
            assertFalse(srcInner.containsKey(999));
        }
    }
}
