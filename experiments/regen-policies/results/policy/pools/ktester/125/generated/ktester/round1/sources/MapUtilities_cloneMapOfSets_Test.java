package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

// kept to match the provided template
@ExtendWith(MockitoExtension.class)
class MapUtilities_cloneMapOfSets_Test {

    private static Stream<Arguments> mutableMapProvider() {
        return Stream.of(Arguments.of("Empty map – mutable false", Collections.emptyMap(), Collections.emptyMap()), Arguments.of("Single entry – deep copy", Map.of("key1", new HashSet<>(List.of(1, 2, 3))), Map.of("key1", new HashSet<>(List.of(1, 2, 3)))), Arguments.of("Multiple entries – deep copy", new HashMap<String, Set<Integer>>() {

            {
                put("a", new HashSet<>(List.of(1)));
                put("b", new HashSet<>(List.of(2, 3)));
            }
        }, new HashMap<String, Set<Integer>>() {

            {
                put("a", new HashSet<>(List.of(1)));
                put("b", new HashSet<>(List.of(2, 3)));
            }
        }));
    }

    private static Stream<Arguments> immutableMapProvider() {
        return Stream.of(Arguments.of("Empty map – immutable true", Collections.emptyMap(), Collections.emptyMap()), Arguments.of("Single entry – unmodifiable view", Map.of("k", new HashSet<>(List.of(10, 20))), Map.of("k", new HashSet<>(List.of(10, 20)))), Arguments.of("Multiple entries – unmodifiable view", new HashMap<String, Set<String>>() {

            {
                put("x", new HashSet<>(List.of("alpha")));
                put("y", new HashSet<>(List.of("beta", "gamma")));
            }
        }, new HashMap<String, Set<String>>() {

            {
                put("x", new HashSet<>(List.of("alpha")));
                put("y", new HashSet<>(List.of("beta", "gamma")));
            }
        }));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void cloneMapOfSetsMutable_copiesContentAndIsModifiable() {
        // GIVEN – a map whose values are mutable sets
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("a", new HashSet<>(Arrays.asList(1, 2, 3)));
        original.put("b", new HashSet<>(Collections.singletonList(42)));
        // WHEN – we clone it with immutable = false
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);
        // THEN – the clone is a different map instance
        assertNotSame(original, clone);
        // and contains the same keys
        assertEquals(original.keySet(), clone.keySet());
        // each value set is a different instance but with equal content
        for (String key : original.keySet()) {
            Set<Integer> originalSet = original.get(key);
            Set<Integer> clonedSet = clone.get(key);
            assertNotSame(originalSet, clonedSet);
            assertEquals(originalSet, clonedSet);
        }
        // The cloned sets are mutable – we can add/remove elements
        clone.get("a").add(99);
        assertTrue(clone.get("a").contains(99));
        // Modifying the original after cloning does NOT affect the clone
        original.get("a").remove(1);
        assertTrue(clone.get("a").contains(1));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void cloneMapOfSetsImmutable_returnsUnmodifiableMapAndSets() {
        // GIVEN
        Map<String, Set<String>> original = new LinkedHashMap<>();
        original.put("x", new LinkedHashSet<>(Arrays.asList("alpha", "beta")));
        original.put("y", new LinkedHashSet<>(Collections.singletonList("gamma")));
        // WHEN
        Map<String, Set<String>> clone = MapUtilities.cloneMapOfSets(original, true);
        // THEN – the map itself is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> clone.put("z", Collections.emptySet()));
        // Each set inside the map is also unmodifiable
        for (Set<String> set : clone.values()) {
            assertThrows(UnsupportedOperationException.class, () -> set.add("delta"));
        }
        // The content must match the original
        assertEquals(original.keySet(), clone.keySet());
        for (String key : original.keySet()) {
            assertEquals(original.get(key), clone.get(key));
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void cloneMapOfSetsEmptyMap_returnsEmptyMap() {
        // GIVEN
        Map<Integer, Set<Double>> empty = Collections.emptyMap();
        // WHEN
        Map<Integer, Set<Double>> resultMutable = MapUtilities.cloneMapOfSets(empty, false);
        Map<Integer, Set<Double>> resultImmutable = MapUtilities.cloneMapOfSets(empty, true);
        // THEN
        assertTrue(resultMutable.isEmpty());
        assertTrue(resultImmutable.isEmpty());
        // immutable result must still be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> resultImmutable.put(1, new HashSet<>()));
    }

    @Test
    void cloneMapOfSetsNullValue_throwsNullPointerException() {
        // GIVEN – a map containing a null set value
        Map<String, Set<String>> original = new HashMap<>();
        original.put("nullSet", null);
        // WHEN / THEN – NPE is expected because the implementation dereferences the value
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfSets(original, false));
    }

    /**
     * Immutable = false – various simple maps **
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mutableMapProvider")
    void cloneMapOfSetsMutable_VariousScenarios(String description, Map<String, Set<?>> original, Map<String, Set<?>> expectedContent) {
        Map<String, Set<?>> clone = MapUtilities.cloneMapOfSets(original, false);
        // map instance must be different
        assertNotSame(original, clone);
        // key set must be identical
        assertEquals(original.keySet(), clone.keySet());
        // each set must be a new mutable copy with equal content
        for (String key : original.keySet()) {
            Set<?> origSet = original.get(key);
            Set<?> clonedSet = clone.get(key);
            assertNotSame(origSet, clonedSet, "Set instance for key '" + key + "' should be a new object");
            assertEquals(origSet, clonedSet, "Set content for key '" + key + "' should be equal");
            // mutable check
            assertDoesNotThrow(() -> clonedSet.addAll(Collections.singleton(null)), "Cloned set should be mutable");
        }
        // verify expected content matches (helps readability)
        assertEquals(expectedContent, clone);
    }

    /**
     * Immutable = true – view semantics **
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("immutableMapProvider")
    void cloneMapOfSetsImmutable_VariousScenarios(String description, Map<String, Set<?>> original, Map<String, Set<?>> expectedContent) {
        Map<String, Set<?>> clone = MapUtilities.cloneMapOfSets(original, true);
        // map must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> clone.put("newKey", Set.of()));
        // each set must be unmodifiable view of the original set
        for (String key : original.keySet()) {
            Set<?> originalSet = original.get(key);
            Set<?> clonedSet = clone.get(key);
            assertSame(originalSet, ((Collections.UnmodifiableSet<?>) clonedSet).collection(), "When immutable=true the set should be a view of the original");
            assertThrows(UnsupportedOperationException.class, () -> clonedSet.add(null));
        }
        // content must match
        assertEquals(expectedContent, clone);
    }

    /**
     * Null handling – null map and null key **
     */
    @Test
    void cloneMapOfSets_NullMap_throwsNPE() {
        assertAll(() -> assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfSets(null, false)), () -> assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfSets(null, true)));
    }

    @Test
    void cloneMapOfSets_NullKey_isHandledCorrectly_mutable() {
        // GIVEN a map with a null key and a non‑null set
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put(null, new HashSet<>(List.of(1, 2)));
        // WHEN
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);
        // THEN – key remains null and set is deep‑copied
        assertTrue(clone.containsKey(null));
        assertNotSame(original.get(null), clone.get(null));
        assertEquals(Set.of(1, 2), clone.get(null));
        // mutable check
        clone.get(null).add(99);
        assertTrue(clone.get(null).contains(99));
    }

    @Test
    void cloneMapOfSets_NullKey_isHandledCorrectly_immutable() {
        // GIVEN a map with a null key and a non‑null set
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put(null, new HashSet<>(List.of(1, 2)));
        // WHEN
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, true);
        // THEN – map and set are unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> clone.put("new", Set.of()));
        assertThrows(UnsupportedOperationException.class, () -> clone.get(null).add(3));
        // the set inside the clone is a view of the original set
        assertSame(original.get(null), ((Collections.UnmodifiableSet<?>) clone.get(null)).collection());
    }

    /**
     * Empty set values **
     */
    @Test
    void cloneMapOfSets_EmptySetValue_mutable() {
        Map<String, Set<String>> original = Map.of("empty", new HashSet<>());
        Map<String, Set<String>> clone = MapUtilities.cloneMapOfSets(original, false);
        assertEquals(1, clone.size());
        Set<String> clonedSet = clone.get("empty");
        assertNotSame(original.get("empty"), clonedSet);
        assertTrue(clonedSet.isEmpty());
        // mutable – we can add an element
        clonedSet.add("new");
        assertTrue(clonedSet.contains("new"));
    }

    @Test
    void cloneMapOfSets_EmptySetValue_immutable() {
        Map<String, Set<String>> original = Map.of("empty", new HashSet<>());
        Map<String, Set<String>> clone = MapUtilities.cloneMapOfSets(original, true);
        assertEquals(1, clone.size());
        Set<String> clonedSet = clone.get("empty");
        assertTrue(clonedSet.isEmpty());
        // unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> clonedSet.add("new"));
    }

    /**
     * Map independence – structural changes after cloning **
     */
    @Test
    void cloneMapOfSets_MutableMapIndependence() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("a", new HashSet<>(List.of(1)));
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);
        // modify original map after cloning
        original.put("b", new HashSet<>(List.of(2)));
        // clone must remain unchanged
        assertFalse(clone.containsKey("b"));
        assertEquals(Set.of(1), clone.get("a"));
    }

    @Test
    void cloneMapOfSets_ImmutableMapIndependenceAndLiveSetView() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("a", new HashSet<>(List.of(1)));
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, true);
        // structural change on original should NOT appear in clone
        original.put("b", new HashSet<>(List.of(2)));
        assertFalse(clone.containsKey("b"));
        // however, the set for key "a" is a live view – changes to original set are visible
        original.get("a").add(99);
        assertTrue(clone.get("a").contains(99));
    }

    /**
     * Null set value – already covered but kept for completeness **
     */
    @Test
    void cloneMapOfSets_NullSetValue_throwsNPE_mutable() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("k", null);
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfSets(original, false));
    }

    @Test
    void cloneMapOfSets_NullSetValue_throwsNPE_immutable() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("k", null);
        assertThrows(NullPointerException.class, () -> MapUtilities.cloneMapOfSets(original, true));
    }
}
