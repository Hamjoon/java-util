package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_dupe_Test {

    /**
     * Shared data used by several tests
     */
    private Map<Class<?>, Set<String>> original;

    @BeforeEach
    void setupBeforeEach() {
        // Build a deterministic original map for each test
        original = new LinkedHashMap<>();
        Set<String> setA = new LinkedHashSet<>(Arrays.asList("alpha", "beta"));
        Set<String> setB = new LinkedHashSet<>(Collections.singletonList("gamma"));
        original.put(String.class, setA);
        original.put(Integer.class, setB);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC and avoid cross‑test contamination
        original = null;
    }

    /**
     * Provider for {@link #testDupeNullOtherMapThrows(boolean)}.
     */
    private static Stream<Arguments> nullOtherMapProvider() {
        return Stream.of(Arguments.of(false), Arguments.of(true));
    }

    /**
     * Provider for {@link #testDupeMutableCopyScenarios(String, Map)}.
     */
    private static Stream<Arguments> mutableCopyProvider() {
        // Normal map with two entries
        Map<Class<?>, Set<String>> normal = new LinkedHashMap<>();
        normal.put(String.class, new LinkedHashSet<>(Arrays.asList("a", "b")));
        normal.put(Integer.class, new LinkedHashSet<>(Collections.singletonList("1")));
        // Empty map
        Map<Class<?>, Set<String>> empty = new LinkedHashMap<>();
        return Stream.of(Arguments.of("Non‑empty map (two entries)", normal), Arguments.of("Empty map", empty));
    }

    /**
     * Provider for {@link #testDupeImmutableCopyScenarios(String, Map)}.
     */
    private static Stream<Arguments> immutableCopyProvider() {
        // Normal map with two entries
        Map<Class<?>, Set<String>> normal = new LinkedHashMap<>();
        normal.put(String.class, new LinkedHashSet<>(Arrays.asList("a", "b")));
        normal.put(Integer.class, new LinkedHashSet<>(Collections.singletonList("1")));
        // Empty map
        Map<Class<?>, Set<String>> empty = new LinkedHashMap<>();
        // Map with an empty set value
        Map<Class<?>, Set<String>> emptySet = new LinkedHashMap<>();
        emptySet.put(String.class, Collections.emptySet());
        // Map with a null key (value is non‑null)
        Map<Class<?>, Set<String>> nullKey = new LinkedHashMap<>();
        nullKey.put(null, new LinkedHashSet<>(Collections.singletonList("x")));
        return Stream.of(Arguments.of("Non‑empty map (two entries)", normal), Arguments.of("Empty map", empty), Arguments.of("Map with empty value set", emptySet), Arguments.of("Map with null key", nullKey));
    }

    @Test
    void testDupeCreatesMutableCopy() {
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, false);
        // The map instance must be different
        assertNotSame(original, copy);
        // Keys must be equal and in the same insertion order
        assertEquals(new ArrayList<>(original.keySet()), new ArrayList<>(copy.keySet()));
        // Each set must be equal but not the same instance
        for (Class<?> key : original.keySet()) {
            Set<String> originalSet = original.get(key);
            Set<String> copySet = copy.get(key);
            assertNotSame(originalSet, copySet, "Set for key " + key + " should be a new instance");
            assertEquals(originalSet, copySet);
        }
        // Mutating the original after dupe must NOT affect the copy
        original.get(String.class).add("delta");
        assertFalse(copy.get(String.class).contains("delta"));
        // Mutating the copy must NOT affect the original
        copy.get(Integer.class).add("epsilon");
        assertFalse(original.get(Integer.class).contains("epsilon"));
    }

    @Test
    void testDupeCreatesUnmodifiableCopy() {
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, true);
        // The map instance must be different
        assertNotSame(original, copy);
        // Keys must be equal and preserve insertion order
        assertEquals(new ArrayList<>(original.keySet()), new ArrayList<>(copy.keySet()));
        // Verify each set is unmodifiable
        for (Class<?> key : copy.keySet()) {
            Set<String> copySet = copy.get(key);
            assertThrows(UnsupportedOperationException.class, () -> copySet.add("newValue"));
        }
        // Verify the map itself is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> copy.put(Double.class, new HashSet<>()));
        // Mutating the original after dupe must NOT affect the unmodifiable copy
        original.get(String.class).add("zeta");
        assertFalse(copy.get(String.class).contains("zeta"));
    }

    @Test
    void testDupeWithEmptyMap() {
        Map<Class<?>, Set<String>> empty = new LinkedHashMap<>();
        Map<Class<?>, Set<String>> mutableCopy = MapUtilities.dupe(empty, false);
        assertTrue(mutableCopy.isEmpty());
        Map<Class<?>, Set<String>> unmodifiableCopy = MapUtilities.dupe(empty, true);
        assertTrue(unmodifiableCopy.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableCopy.put(String.class, new HashSet<>()));
    }

    @Test
    void testDupeDoesNotExposeOriginalCollections() {
        // Grab a reference to one of the original sets before dupe
        Set<String> originalSetRef = original.get(String.class);
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, false);
        Set<String> copySet = copy.get(String.class);
        // The copy's set must not be the same object as the original's set
        assertNotSame(originalSetRef, copySet);
        // Changing the copy's set must not affect the original reference
        copySet.remove("alpha");
        assertTrue(originalSetRef.contains("alpha"));
    }

    /**
     * Verifies that an immutable copy of a map containing an empty set
     * behaves correctly – the set is still present, but attempts to modify
     * it (or the map) throw {@link UnsupportedOperationException}.
     */
    @Test
    void testDupeImmutableCopyWithEmptyValueSet() {
        Map<Class<?>, Set<String>> source = new LinkedHashMap<>();
        source.put(String.class, Collections.emptySet());
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, true);
        // Map must contain the key
        assertTrue(copy.containsKey(String.class));
        // The associated set must be unmodifiable
        Set<String> copySet = copy.get(String.class);
        assertThrows(UnsupportedOperationException.class, () -> copySet.add("any"));
        // The map itself must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> copy.put(Integer.class, new HashSet<>()));
    }

    /**
     * Validates that a {@code null} key is allowed and correctly copied.
     * Both the map and the associated set must be immutable when the
     * {@code unmodifiable} flag is {@code true}.
     */
    @Test
    void testDupeImmutableCopyWithNullKey() {
        Map<Class<?>, Set<String>> source = new LinkedHashMap<>();
        source.put(null, new LinkedHashSet<>(Collections.singletonList("x")));
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, true);
        // Null key must be present
        assertTrue(copy.containsKey(null));
        Set<String> copySet = copy.get(null);
        assertEquals(Collections.singleton("x"), copySet);
        // Set immutability
        assertThrows(UnsupportedOperationException.class, () -> copySet.add("y"));
        // Map immutability
        assertThrows(UnsupportedOperationException.class, () -> copy.put(String.class, new HashSet<>()));
    }

    /**
     * The method constructs a {@link LinkedHashSet} from each map value.
     * Supplying a {@code null} value therefore triggers a {@link NullPointerException}.
     */
    @Test
    void testDupeImmutableCopyWithNullValueThrows() {
        Map<Class<?>, Set<String>> source = new LinkedHashMap<>();
        // deliberately null
        source.put(String.class, null);
        assertThrows(NullPointerException.class, () -> MapUtilities.dupe(source, true));
    }

    /**
     * Passing a {@code null} reference for the source map must result in a
     * {@link NullPointerException} regardless of the {@code unmodifiable} flag.
     */
    @ParameterizedTest(name = "{index}: unmodifiable={0}")
    @MethodSource("nullOtherMapProvider")
    void testDupeNullOtherMapThrows(boolean unmodifiable) {
        assertThrows(NullPointerException.class, () -> MapUtilities.dupe(null, unmodifiable));
    }

    /**
     * Parameterized test covering the standard mutable‑copy scenario with a
     * non‑empty map and with an empty map. This groups together the two
     * cases described in the JSON under “dupe - unmodifiable flag false”.
     */
    @ParameterizedTest(name = "{index}: description={0}")
    @MethodSource("mutableCopyProvider")
    void testDupeMutableCopyScenarios(String description, Map<Class<?>, Set<String>> source) {
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, false);
        // Map instance must be new
        assertNotSame(source, copy);
        // Keys and order must be preserved
        assertEquals(new ArrayList<>(source.keySet()), new ArrayList<>(copy.keySet()));
        // Each set must be a new mutable instance with equal contents
        for (Class<?> key : source.keySet()) {
            Set<String> originalSet = source.get(key);
            Set<String> copySet = copy.get(key);
            assertNotSame(originalSet, copySet, "Set for key " + key + " should be a new instance");
            assertEquals(originalSet, copySet);
            // Mutability check
            copySet.add("temp");
            assertTrue(copySet.contains("temp"));
        }
    }

    /**
     * Parameterized test covering immutable‑copy scenarios described in the JSON
     * under “dupe - unmodifiable flag true”. It verifies correct behaviour for
     * a normal map, an empty map, a map with an empty set, and a map with a
     * {@code null} key. The case with a {@code null} value is handled separately
     * because it expects an exception.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("immutableCopyProvider")
    void testDupeImmutableCopyScenarios(String description, Map<Class<?>, Set<String>> source) {
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, true);
        // Map must be a new instance and unmodifiable
        assertNotSame(source, copy);
        assertThrows(UnsupportedOperationException.class, () -> copy.put(Double.class, new HashSet<>()));
        // Keys and order must be preserved
        assertEquals(new ArrayList<>(source.keySet()), new ArrayList<>(copy.keySet()));
        // Verify each set is unmodifiable and contains the same elements
        for (Class<?> key : source.keySet()) {
            Set<String> originalSet = source.get(key);
            Set<String> copySet = copy.get(key);
            assertEquals(originalSet, copySet);
            assertThrows(UnsupportedOperationException.class, () -> copySet.add("new"));
        }
    }
}
