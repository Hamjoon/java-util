package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_getEmptyCollection_Test {

    /**
     * Supplies a stream of arguments for {@link #getEmptyCollection_ReturnsCorrectEmptyInstance}.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>the input collection (different concrete implementations)</li>
     *   <li>the expected empty collection instance from {@link Collections}</li>
     *   <li>a human‑readable description for test naming/debugging</li>
     * </ul>
     */
    private static Stream<Arguments> collectionProvider() {
        return Stream.of(// NavigableSet cases
        Arguments.of(new TreeSet<String>(), Collections.emptyNavigableSet(), "NavigableSet (TreeSet)"), Arguments.of(Collections.unmodifiableNavigableSet(new TreeSet<String>()), Collections.emptyNavigableSet(), "Unmodifiable NavigableSet"), // SortedSet (non‑Navigable) case
        Arguments.of(Collections.unmodifiableSortedSet(new TreeSet<Integer>()), Collections.emptySortedSet(), "SortedSet (unmodifiable)"), // Set cases
        Arguments.of(new HashSet<Long>(), Collections.emptySet(), "HashSet"), Arguments.of(Collections.unmodifiableSet(new HashSet<Long>()), Collections.emptySet(), "Unmodifiable Set"), // List cases
        Arguments.of(new ArrayList<String>(), Collections.emptyList(), "ArrayList"), Arguments.of(new LinkedList<String>(), Collections.emptyList(), "LinkedList"), // Fallback for other Collection types
        Arguments.of(new ArrayDeque<Double>(), Collections.emptyList(), "ArrayDeque (Queue) – fallback to emptyList"), Arguments.of(new PriorityQueue<String>(), Collections.emptyList(), "PriorityQueue – fallback to emptyList"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getEmptyCollection_NullCollection_ThrowsNPE() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CollectionUtilities.getEmptyCollection(null));
        assertEquals("Collection must not be null", ex.getMessage(), "Exception message must match the contract");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getEmptyCollection_NavigableSet_ReturnsEmptyNavigableSet() {
        NavigableSet<String> input = new TreeSet<>();
        Collection<String> result = CollectionUtilities.getEmptyCollection(input);
        assertSame(Collections.emptyNavigableSet(), result, "Should return the shared emptyNavigableSet instance");
        assertTrue(result instanceof NavigableSet);
        assertTrue(result.isEmpty());
        // Immutability check
        assertThrows(UnsupportedOperationException.class, () -> result.add("test"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getEmptyCollection_SortedSet_ReturnsEmptySortedSet() {
        // An unmodifiableSortedSet does NOT implement NavigableSet
        SortedSet<Integer> input = Collections.unmodifiableSortedSet(new TreeSet<>());
        Collection<Integer> result = CollectionUtilities.getEmptyCollection(input);
        assertSame(Collections.emptySortedSet(), result, "Should return the shared emptySortedSet instance");
        assertTrue(result instanceof SortedSet);
        assertFalse(result instanceof NavigableSet);
        assertTrue(result.isEmpty());
        // Immutability check
        assertThrows(UnsupportedOperationException.class, () -> result.add(1));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getEmptyCollection_Set_ReturnsEmptySet() {
        Set<Long> input = new HashSet<>();
        Collection<Long> result = CollectionUtilities.getEmptyCollection(input);
        assertSame(Collections.emptySet(), result, "Should return the shared emptySet instance");
        assertTrue(result instanceof Set);
        assertTrue(result.isEmpty());
        // Immutability check
        assertThrows(UnsupportedOperationException.class, () -> result.add(42L));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getEmptyCollection_List_ReturnsEmptyList() {
        List<String> input = new ArrayList<>();
        Collection<String> result = CollectionUtilities.getEmptyCollection(input);
        assertSame(Collections.emptyList(), result, "Should return the shared emptyList instance");
        assertTrue(result instanceof List);
        assertTrue(result.isEmpty());
        // Immutability check
        assertThrows(UnsupportedOperationException.class, () -> result.add("x"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getEmptyCollection_OtherCollection_ReturnsEmptyList() {
        Queue<Double> input = new ArrayDeque<>();
        Collection<Double> result = CollectionUtilities.getEmptyCollection(input);
        // The implementation defaults to emptyList()
        assertSame(Collections.emptyList(), result, "Non‑List/Set types should fall back to emptyList");
        assertTrue(result instanceof List);
        assertTrue(result.isEmpty());
        // Immutability check
        assertThrows(UnsupportedOperationException.class, () -> result.add(3.14));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => input={0}")
    @MethodSource("collectionProvider")
    void getEmptyCollection_ReturnsCorrectEmptyInstance(Collection<?> input, Collection<?> expectedEmpty, String description) {
        // Act
        Collection<?> result = CollectionUtilities.getEmptyCollection(input);
        // Assert that the exact shared empty collection instance is returned.
        assertSame(expectedEmpty, result, () -> "Expected shared empty instance for: " + description);
        // Verify the result implements the expected interface(s).
        assertTrue(expectedEmpty.getClass().isInstance(result), () -> "Result should be instance of " + expectedEmpty.getClass().getSimpleName());
        // The collection must be empty.
        assertTrue(result.isEmpty(), "Returned collection must be empty");
        // Verify immutability – any add operation should fail.
        assertThrows(UnsupportedOperationException.class, () -> result.add(new Object()), "Returned collection must be immutable");
    }
}
