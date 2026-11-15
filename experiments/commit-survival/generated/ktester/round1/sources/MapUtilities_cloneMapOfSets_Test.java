package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_cloneMapOfSets_Test {

    /**
     * Helper to build a map with a null key – used in a test case.
     */
    private static Map<String, Set<Integer>> mapWithNullKey() {
        Map<String, Set<Integer>> map = new HashMap<>();
        map.put(null, new HashSet<>(Arrays.asList(1)));
        return map;
    }

    /**
     * Helper to build a map with a null value (set).
     */
    private static Map<String, Set<Integer>> mapWithNullValue() {
        Map<String, Set<Integer>> map = new HashMap<>();
        map.put("a", null);
        return map;
    }

    /**
     * Parameter source for immutable = false basic functionality.
     */
    private static Stream<Arguments> immutableFalseBasicProvider() {
        return Stream.of(// empty map
        Arguments.of(new HashMap<String, Set<Integer>>(), false, 0, true), // map with one entry
        Arguments.of(Collections.singletonMap("a", new HashSet<>(Arrays.asList(1, 2))), false, 1, false));
    }

    /**
     * Parameter source for immutable = true immutability tests.
     */
    private static Stream<Arguments> immutableTrueProvider() {
        return Stream.of(// empty map
        Arguments.of(new HashMap<String, Set<Integer>>(), true, 0), // map with two entries
        Arguments.of(new HashMap<String, Set<Integer>>() {

            {
                put("a", new HashSet<>(Arrays.asList(1, 2)));
                put("b", new HashSet<>(Arrays.asList(3)));
            }
        }, true, 2));
    }

    @Test
    void cloneMapOfSets_mutableCopy_isDeepAndModifiable() {
        // GIVEN – a map with two keys, each pointing to a distinct mutable set
        Map<String, Set<Integer>> original = new HashMap<>();
        Set<Integer> setA = new HashSet<>(Arrays.asList(1, 2, 3));
        Set<Integer> setB = new HashSet<>(Collections.singletonList(99));
        original.put("A", setA);
        original.put("B", setB);
        // WHEN – we clone the map with immutable = false
        Map<String, Set<Integer>> result = MapUtilities.cloneMapOfSets(original, false);
        // THEN – the result map is a new instance
        assertNotSame(original, result, "Result map should be a different instance");
        // and contains the same keys
        assertEquals(original.keySet(), result.keySet(), "Result map should contain the same keys");
        // each value set is a new mutable instance
        for (String key : original.keySet()) {
            Set<Integer> originalSet = original.get(key);
            Set<Integer> resultSet = result.get(key);
            assertNotSame(originalSet, resultSet, "Set for key '" + key + "' should be a new instance");
            assertEquals(originalSet, resultSet, "Set contents should be equal for key '" + key + "'");
            // mutability check – adding an element to the result set must not affect the original
            resultSet.add(42);
            assertTrue(resultSet.contains(42), "Result set should contain the newly added element");
            assertFalse(originalSet.contains(42), "Original set must remain unchanged");
        }
        // modifying the result map must not affect the original map
        result.put("C", new HashSet<>(Collections.singleton(7)));
        assertFalse(original.containsKey("C"), "Original map must not contain newly added key");
    }

    @Test
    void cloneMapOfSets_immutableCopy_isUnmodifiable() {
        // GIVEN – a map with a single entry
        Map<String, Set<String>> original = new HashMap<>();
        original.put("key", new HashSet<>(Arrays.asList("x", "y")));
        // WHEN – we clone the map with immutable = true
        Map<String, Set<String>> result = MapUtilities.cloneMapOfSets(original, true);
        // THEN – the map itself is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> result.put("newKey", Set.of("z")), "Result map should be unmodifiable");
        // each set inside the map is also unmodifiable
        Set<String> resultSet = result.get("key");
        assertThrows(UnsupportedOperationException.class, () -> resultSet.add("newValue"), "Inner set should be unmodifiable");
        // the contents must match the original
        assertEquals(original.get("key"), resultSet, "Contents of the inner set must be equal to the original");
    }

    @Test
    void cloneMapOfSets_emptyMap_returnsEmptyMap() {
        // GIVEN – an empty map
        Map<Integer, Set<Double>> original = Collections.emptyMap();
        // WHEN – mutable copy
        Map<Integer, Set<Double>> mutableResult = MapUtilities.cloneMapOfSets(original, false);
        // THEN – still empty and mutable
        assertTrue(mutableResult.isEmpty(), "Mutable result should be empty");
        // map is mutable (no exception on put)
        mutableResult.put(1, new HashSet<>(Collections.singleton(3.14)));
        assertTrue(mutableResult.containsKey(1));
        // WHEN – immutable copy
        Map<Integer, Set<Double>> immutableResult = MapUtilities.cloneMapOfSets(original, true);
        // THEN – still empty and unmodifiable
        assertTrue(immutableResult.isEmpty(), "Immutable result should be empty");
        assertThrows(UnsupportedOperationException.class, () -> immutableResult.put(2, Set.of(2.71)), "Immutable result map should be unmodifiable");
    }

    @Test
    void cloneMapOfSets_originalIsNotAffectedByResultModifications() {
        // GIVEN – a map with a mutable set
        Map<String, Set<String>> original = new HashMap<>();
        Set<String> inner = new HashSet<>(List.of("a", "b"));
        original.put("letters", inner);
        // WHEN – clone mutable copy
        Map<String, Set<String>> result = MapUtilities.cloneMapOfSets(original, false);
        // THEN – modify result's inner set and map
        result.get("letters").remove("a");
        result.put("newKey", new HashSet<>(List.of("c")));
        // original must stay unchanged
        assertTrue(original.containsKey("letters"));
        assertEquals(Set.of("a", "b"), original.get("letters"), "Original inner set must remain unchanged after result modifications");
        assertFalse(original.containsKey("newKey"), "Original map must not contain keys added to the result map");
    }

    @ParameterizedTest(name = "[{index}] immutable=false, original size={2}, expectMutable={3}")
    @MethodSource("immutableFalseBasicProvider")
    void cloneMapOfSets_immutableFalse_basicFunctionality(Map<String, Set<Integer>> original, boolean immutable, int expectedSize, boolean expectMutable) {
        Map<String, Set<Integer>> result = MapUtilities.cloneMapOfSets(original, immutable);
        assertEquals(expectedSize, result.size(), "Result map size should match expectation");
        // map must be mutable
        assertDoesNotThrow(() -> result.put("newKey", new HashSet<>(Arrays.asList(99))), "Result map should be mutable");
        // verify deep copy of sets
        for (String key : original.keySet()) {
            Set<Integer> origSet = original.get(key);
            Set<Integer> resSet = result.get(key);
            if (origSet != null) {
                assertNotSame(origSet, resSet, "Set instance must be a new copy");
                assertEquals(origSet, resSet, "Set contents must be equal");
                // mutating result set must not affect original
                resSet.add(999);
                assertFalse(origSet.contains(999), "Original set must stay unchanged");
            } else {
                // original set was null – we expect NPE earlier, so this path shouldn't be hit
                fail("Original set was null, should have thrown before this point");
            }
        }
    }

    @ParameterizedTest(name = "[{index}] immutable=true, original size={2}")
    @MethodSource("immutableTrueProvider")
    void cloneMapOfSets_immutableTrue_immutability(Map<String, Set<Integer>> original, boolean immutable, int expectedSize) {
        Map<String, Set<Integer>> result = MapUtilities.cloneMapOfSets(original, immutable);
        assertEquals(expectedSize, result.size(), "Result map size should match expectation");
        // map must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> result.put("newKey", Set.of(5)), "Result map should be unmodifiable");
        // each inner set must be unmodifiable and a deep copy
        for (String key : original.keySet()) {
            Set<Integer> origSet = original.get(key);
            Set<Integer> resSet = result.get(key);
            assertNotSame(origSet, resSet, "Inner set should be a new instance");
            assertEquals(origSet, resSet, "Inner set contents must be equal");
            assertThrows(UnsupportedOperationException.class, () -> resSet.add(123), "Inner set should be unmodifiable");
        }
    }

    @Test
    void cloneMapOfSets_nullKey_isHandledWhenMutable() {
        Map<String, Set<Integer>> original = mapWithNullKey();
        Map<String, Set<Integer>> result = MapUtilities.cloneMapOfSets(original, false);
        assertTrue(result.containsKey(null), "Result map must contain the null key");
        Set<Integer> resultSet = result.get(null);
        assertNotNull(resultSet, "Set for null key must not be null");
        assertEquals(Set.of(1), resultSet, "Set contents must match original");
        // mutability check
        assertDoesNotThrow(() -> resultSet.add(2));
        assertTrue(resultSet.contains(2));
    }

    @Test
    void cloneMapOfSets_nullValue_throwsNPE() {
        Map<String, Set<Integer>> original = mapWithNullValue();
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfSets(original, false), "Cloning a map with a null Set value should throw NullPointerException");
    }

    @Test
    void cloneMapOfSets_emptySet_insideMap_mutableCopy() {
        Map<String, Set<Integer>> original = Collections.singletonMap("empty", new HashSet<>());
        Map<String, Set<Integer>> result = MapUtilities.cloneMapOfSets(original, false);
        assertTrue(result.containsKey("empty"));
        Set<Integer> resultSet = result.get("empty");
        assertNotSame(original.get("empty"), resultSet, "Empty set must be a new instance");
        assertTrue(resultSet.isEmpty(), "Result set should be empty");
        // mutability
        assertDoesNotThrow(() -> resultSet.add(42));
        assertTrue(resultSet.contains(42));
    }

    @Test
    void cloneMapOfSets_multipleEntries_mutableClone() {
        Map<String, Set<Integer>> original = new HashMap<>() {

            {
                put("x", new HashSet<>(Arrays.asList(10, 20)));
                put("y", new HashSet<>(Arrays.asList(30)));
                // empty set
                put("z", new HashSet<>());
            }
        };
        Map<String, Set<Integer>> result = MapUtilities.cloneMapOfSets(original, false);
        assertEquals(3, result.size(), "Result map should have three entries");
        for (String key : original.keySet()) {
            Set<Integer> origSet = original.get(key);
            Set<Integer> resSet = result.get(key);
            assertNotSame(origSet, resSet, "Set for key '" + key + "' must be a new instance");
            assertEquals(origSet, resSet, "Set contents for key '" + key + "' must match");
        }
        // map mutability
        assertDoesNotThrow(() -> result.put("new", Set.of(99)));
    }

    @Test
    void cloneMapOfSets_originalMapModificationDoesNotAffectClone() {
        Map<String, Set<Integer>> original = new HashMap<>() {

            {
                put("a", new HashSet<>(Arrays.asList(1)));
            }
        };
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);
        // modify original after cloning
        original.put("b", new HashSet<>(Arrays.asList(2)));
        assertFalse(clone.containsKey("b"), "Clone should not reflect new entries added to original");
        assertEquals(1, clone.size(), "Clone size should remain unchanged");
    }

    @Test
    void cloneMapOfSets_originalSetModificationDoesNotAffectClone() {
        Map<String, Set<Integer>> original = new HashMap<>() {

            {
                put("a", new HashSet<>(Arrays.asList(1, 2)));
            }
        };
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);
        // modify original set
        original.get("a").add(3);
        Set<Integer> clonedSet = clone.get("a");
        assertEquals(Set.of(1, 2), clonedSet, "Cloned set must remain unchanged after original set mutation");
    }

    @Test
    void cloneMapOfSets_nullOriginalMap_throwsNPE_mutable() {
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfSets(null, false), "Passing null original map should throw NullPointerException");
    }

    @Test
    void cloneMapOfSets_nullOriginalMap_throwsNPE_immutable() {
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfSets(null, true), "Passing null original map should throw NullPointerException even when immutable");
    }
}
