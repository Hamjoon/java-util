package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_dupe_Test {

    /**
     * Sample map used in the tests.
     */
    private Map<Class<?>, Set<String>> originalMap;

    /**
     * Populate a fresh map before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        originalMap = new LinkedHashMap<>();
        // key 1
        Set<String> setA = new LinkedHashSet<>(Arrays.asList("alpha", "beta"));
        originalMap.put(String.class, setA);
        // key 2
        Set<String> setB = new LinkedHashSet<>(Collections.singletonList("gamma"));
        originalMap.put(Integer.class, setB);
    }

    /**
     * Clear the reference after each test.
     */
    @AfterEach
    void teardownAfterEach() {
        originalMap = null;
    }

    // ---------- dupe – unmodifiable = true (standard map) ----------
    private static Stream<Arguments> unmodifiableTrueStandardProvider() {
        return Stream.of(Arguments.of(Map.of(String.class, Set.of("a", "b"), Integer.class, Set.of(1, 2, 3)), true, "standard"), Arguments.of(Collections.emptyMap(), true, "empty"));
    }

    // ---------- dupe – unmodifiable = false (standard map) ----------
    private static Stream<Arguments> unmodifiableFalseStandardProvider() {
        return Stream.of(Arguments.of(Map.of(String.class, Set.of("x", "y"), Double.class, Set.of(0.1, 0.2)), false, "standard"), Arguments.of(Collections.emptyMap(), false, "empty"));
    }

    /**
     * Verify that dupe returns a mutable copy when `unmodifiable` is false.
     */
    @Test
    void dupe_returnsMutableCopy_whenUnmodifiableFalse() {
        // GIVEN
        boolean unmodifiable = false;
        // WHEN
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(originalMap, unmodifiable);
        // THEN
        assertNotNull(copy, "Returned map should not be null");
        assertEquals(originalMap.size(), copy.size(), "Map size should be equal to original");
        assertTrue(copy.containsKey(String.class));
        assertTrue(copy.containsKey(Integer.class));
        // The sets should be equal but not the same instance
        Set<String> originalSetA = originalMap.get(String.class);
        Set<String> copySetA = copy.get(String.class);
        assertEquals(originalSetA, copySetA, "Set contents should be equal");
        assertNotSame(originalSetA, copySetA, "Set instances should be different");
        // Modifying the copy must not affect the original
        copySetA.add("delta");
        assertFalse(originalSetA.contains("delta"), "Original set must remain unchanged");
        // The returned map must be mutable
        copy.put(Double.class, new LinkedHashSet<>(Collections.singletonList("epsilon")));
        assertTrue(copy.containsKey(Double.class), "Map should be mutable and contain the newly added key");
    }

    /**
     * Verify that dupe returns an unmodifiable map and unmodifiable sets when `unmodifiable` is true.
     */
    @Test
    void dupe_returnsUnmodifiable_whenUnmodifiableTrue() {
        // GIVEN
        boolean unmodifiable = true;
        // WHEN
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(originalMap, unmodifiable);
        // THEN
        assertNotNull(copy, "Returned map should not be null");
        assertEquals(originalMap.size(), copy.size(), "Map size should be equal to original");
        // The map itself must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> copy.put(Float.class, new LinkedHashSet<>()), "Map should be unmodifiable");
        // Each set inside the map must also be unmodifiable
        Set<String> copySetA = copy.get(String.class);
        assertThrows(UnsupportedOperationException.class, () -> copySetA.add("delta"), "Set should be unmodifiable");
        // Ensure the contents are correct
        assertEquals(originalMap.get(String.class), copySetA, "Set contents should match the original");
    }

    /**
     * Verify that dupe correctly handles an empty map.
     */
    @Test
    void dupe_handlesEmptyMap() {
        // GIVEN
        Map<Class<?>, Set<String>> empty = Collections.emptyMap();
        // WHEN
        Map<Class<?>, Set<String>> mutableCopy = MapUtilities.dupe(empty, false);
        Map<Class<?>, Set<String>> immutableCopy = MapUtilities.dupe(empty, true);
        // THEN
        assertTrue(mutableCopy.isEmpty(), "Mutable copy of empty map should be empty");
        assertTrue(immutableCopy.isEmpty(), "Immutable copy of empty map should be empty");
        // Immutable copy must throw on modification
        assertThrows(UnsupportedOperationException.class, () -> immutableCopy.put(String.class, new LinkedHashSet<>()), "Immutable map should not allow put operation");
    }

    @ParameterizedTest(name = "dupe_unmodifiableTrue_{2}")
    @MethodSource("unmodifiableTrueStandardProvider")
    void dupe_unmodifiableTrue_StandardMap(Map<Class<?>, Set<?>> source, boolean unmodifiable, String label) {
        // WHEN
        Map<Class<?>, Set<?>> result = MapUtilities.dupe(source, unmodifiable);
        // THEN
        assertNotNull(result);
        assertEquals(source.size(), result.size());
        // map must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> result.put(Double.class, Set.of("x")));
        // each set must be unmodifiable and a different instance
        source.forEach((k, v) -> {
            Set<?> resultSet = result.get(k);
            assertNotNull(resultSet);
            assertEquals(v, resultSet);
            assertNotSame(v, resultSet, "Set instance must be a copy");
            assertThrows(UnsupportedOperationException.class, () -> resultSet.add("new"));
        });
    }

    @ParameterizedTest(name = "dupe_unmodifiableFalse_{2}")
    @MethodSource("unmodifiableFalseStandardProvider")
    void dupe_unmodifiableFalse_StandardMap(Map<Class<?>, Set<?>> source, boolean unmodifiable, String label) {
        // WHEN
        Map<Class<?>, Set<?>> result = MapUtilities.dupe(source, unmodifiable);
        // THEN
        assertNotNull(result);
        assertEquals(source.size(), result.size());
        // map must be mutable
        assertDoesNotThrow(() -> result.put(Float.class, Set.of("z")));
        // each set must be mutable and a different instance
        source.forEach((k, v) -> {
            Set<?> resultSet = result.get(k);
            assertNotNull(resultSet);
            assertEquals(v, resultSet);
            assertNotSame(v, resultSet, "Set instance must be a copy");
            assertDoesNotThrow(() -> resultSet.add("new"));
        });
    }

    // ---------- dupe – independence of copies ----------
    @Test
    void dupe_independenceOfCopies_mutable() {
        // GIVEN
        Map<Class<?>, Set<String>> source = Map.of(String.class, new LinkedHashSet<>(Arrays.asList("alpha", "beta")));
        // WHEN
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, false);
        // THEN
        Set<String> originalSet = source.get(String.class);
        Set<String> copySet = copy.get(String.class);
        assertNotSame(originalSet, copySet, "Returned Set must be a different instance");
        copySet.add("gamma");
        assertFalse(originalSet.contains("gamma"), "Original Set must remain unchanged");
    }

    @Test
    void dupe_independenceOfCopies_unmodifiable() {
        // GIVEN
        Map<Class<?>, Set<String>> source = Map.of(String.class, new LinkedHashSet<>(Arrays.asList("one")));
        // WHEN
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, true);
        // THEN
        Set<String> originalSet = source.get(String.class);
        Set<String> copySet = copy.get(String.class);
        assertNotSame(originalSet, copySet, "Returned Set must be a different instance");
        assertThrows(UnsupportedOperationException.class, () -> copySet.add("two"));
        assertFalse(originalSet.contains("two"));
    }

    // ---------- dupe – order preservation (LinkedHashMap semantics) ----------
    @Test
    void dupe_preservesInsertionOrder() {
        // GIVEN: LinkedHashMap with a known insertion order
        LinkedHashMap<Class<?>, Set<String>> source = new LinkedHashMap<>();
        source.put(String.class, Set.of("a"));
        source.put(Integer.class, Set.of("b"));
        source.put(Double.class, Set.of("c"));
        // WHEN
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, false);
        // THEN: iteration order must match the source order
        List<Class<?>> expectedOrder = new ArrayList<>(source.keySet());
        List<Class<?>> actualOrder = new ArrayList<>(copy.keySet());
        assertEquals(expectedOrder, actualOrder, "Key iteration order should be preserved");
    }

    // ---------- dupe – null source map ----------
    @Test
    void dupe_nullSourceMap_throwsNPE_unmodifiableFalse() {
        assertThrows(NullPointerException.class, () -> MapUtilities.dupe(null, false));
    }

    @Test
    void dupe_nullSourceMap_throwsNPE_unmodifiableTrue() {
        assertThrows(NullPointerException.class, () -> MapUtilities.dupe(null, true));
    }

    // ---------- dupe – null Set value in source map ----------
    @Test
    void dupe_nullSetValue_throwsNPE() {
        Map<Class<?>, Set<String>> source = new HashMap<>();
        source.put(String.class, null);
        assertThrows(NullPointerException.class, () -> MapUtilities.dupe(source, false));
    }

    // ---------- dupe – Set containing null element ----------
    @Test
    void dupe_setContainingNull_mutable() {
        // GIVEN
        Set<String> originalSet = new LinkedHashSet<>(Arrays.asList((String) null, "x"));
        Map<Class<?>, Set<String>> source = new HashMap<>();
        source.put(String.class, originalSet);
        // WHEN
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, false);
        // THEN
        Set<String> copySet = copy.get(String.class);
        assertEquals(originalSet, copySet, "Contents (including null) must be equal");
        assertNotSame(originalSet, copySet, "Set instance must be a new copy");
        // Mutability check
        assertDoesNotThrow(() -> copySet.add("y"));
        assertTrue(copySet.contains("y"));
    }

    @Test
    void dupe_setContainingNull_unmodifiable() {
        // GIVEN
        Set<String> originalSet = new LinkedHashSet<>(Arrays.asList((String) null, "y"));
        Map<Class<?>, Set<String>> source = new HashMap<>();
        source.put(String.class, originalSet);
        // WHEN
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, true);
        // THEN
        Set<String> copySet = copy.get(String.class);
        assertEquals(originalSet, copySet, "Contents (including null) must be equal");
        assertNotSame(originalSet, copySet, "Set instance must be a new copy");
        assertThrows(UnsupportedOperationException.class, () -> copySet.add("z"));
    }

    // ---------- dupe – source map already unmodifiable, mutable copy requested ----------
    @Test
    void dupe_sourceUnmodifiable_mutableCopy() {
        // GIVEN: source map is already unmodifiable
        Map<Class<?>, Set<String>> source = Collections.unmodifiableMap(Map.of(String.class, Set.of("a"), Integer.class, Set.of("1", "2")));
        // WHEN
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, false);
        // THEN
        assertNotSame(source, copy, "Returned map must be a new instance");
        assertDoesNotThrow(() -> copy.put(Float.class, Set.of("x")));
        copy.forEach((k, v) -> assertDoesNotThrow(() -> v.add("new")));
    }

    // ---------- dupe – source map already unmodifiable, unmodifiable copy requested ----------
    @Test
    void dupe_sourceUnmodifiable_unmodifiableCopy() {
        // GIVEN
        Map<Class<?>, Set<String>> source = Collections.unmodifiableMap(Map.of(String.class, Set.of("z")));
        // WHEN
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, true);
        // THEN
        assertNotSame(source, copy, "Returned map must be a distinct instance");
        assertThrows(UnsupportedOperationException.class, () -> copy.put(Double.class, Set.of("d")));
        Set<String> copySet = copy.get(String.class);
        assertThrows(UnsupportedOperationException.class, () -> copySet.add("new"));
    }

    // ---------- dupe – source map with empty Set values ----------
    @Test
    void dupe_sourceWithEmptySets_mutable() {
        // GIVEN
        Map<Class<?>, Set<String>> source = new HashMap<>();
        source.put(String.class, Collections.emptySet());
        source.put(Integer.class, Collections.emptySet());
        // WHEN
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, false);
        // THEN
        assertEquals(2, copy.size());
        copy.forEach((k, v) -> {
            assertTrue(v.isEmpty(), "Copied set should be initially empty");
            assertDoesNotThrow(() -> v.add("elem"));
        });
    }

    @Test
    void dupe_sourceWithEmptySets_unmodifiable() {
        // GIVEN
        Map<Class<?>, Set<String>> source = new HashMap<>();
        source.put(String.class, Collections.emptySet());
        // WHEN
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, true);
        // THEN
        Set<String> copySet = copy.get(String.class);
        assertTrue(copySet.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> copySet.add("elem"));
    }

    // ---------- dupe – null key in source map ----------
    @Test
    void dupe_nullKey_throwsNPE_unmodifiableFalse() {
        Map<Class<?>, Set<String>> source = new HashMap<>();
        source.put(null, Set.of("a"));
        assertThrows(NullPointerException.class, () -> MapUtilities.dupe(source, false));
    }

    @Test
    void dupe_nullKey_throwsNPE_unmodifiableTrue() {
        Map<Class<?>, Set<String>> source = new HashMap<>();
        source.put(null, Set.of("b"));
        assertThrows(NullPointerException.class, () -> MapUtilities.dupe(source, true));
    }
}
