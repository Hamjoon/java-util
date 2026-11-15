package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

// Mockito not used but kept from template
@ExtendWith(MockitoExtension.class)
class CollectionUtilities_getUnmodifiableCollection_Test {

    /* --------------------------------------------------------------------- *
     *  Empty collections – parameterized to avoid duplication
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> emptyCollectionProvider() {
        return Stream.of(Arguments.of(new ArrayList<>(), List.class, "List"), Arguments.of(new HashSet<>(), Set.class, "Set"), Arguments.of(new ArrayDeque<>(), Collection.class, "Collection"));
    }

    @Test
    void getUnmodifiableCollection_NullArgument_ThrowsNPE() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CollectionUtilities.getUnmodifiableCollection(null));
        assertEquals("Collection must not be null", ex.getMessage(), "Exception message must match the one from Objects.requireNonNull");
    }

    @Test
    void getUnmodifiableCollection_NavigableSet_ReturnsUnmodifiableNavigableSet() {
        NavigableSet<String> original = new TreeSet<>(Arrays.asList("a", "b", "c"));
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(original);
        assertTrue(result instanceof NavigableSet, "Result should be a NavigableSet");
        NavigableSet<String> unmodifiable = (NavigableSet<String>) result;
        // The wrapper must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add("d"));
        // Original content must be preserved
        assertEquals(Set.of("a", "b", "c"), unmodifiable);
    }

    @Test
    void getUnmodifiableCollection_SortedSet_ReturnsUnmodifiableSortedSet() {
        SortedSet<Integer> original = new TreeSet<>(Arrays.asList(1, 2, 3));
        Collection<Integer> result = CollectionUtilities.getUnmodifiableCollection(original);
        // In Java the emptySortedSet also implements NavigableSet, but a regular TreeSet
        // is both SortedSet and NavigableSet; the method will return an unmodifiable
        // NavigableSet.  We only require that it is at least a SortedSet.
        assertTrue(result instanceof SortedSet, "Result should be a SortedSet");
        SortedSet<Integer> unmodifiable = (SortedSet<Integer>) result;
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add(4));
        assertEquals(Set.of(1, 2, 3), unmodifiable);
    }

    @Test
    void getUnmodifiableCollection_Set_ReturnsUnmodifiableSet() {
        Set<Double> original = new HashSet<>(Arrays.asList(1.1, 2.2));
        Collection<Double> result = CollectionUtilities.getUnmodifiableCollection(original);
        assertTrue(result instanceof Set, "Result should be a Set");
        Set<Double> unmodifiable = (Set<Double>) result;
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add(3.3));
        assertEquals(Set.of(1.1, 2.2), unmodifiable);
    }

    @Test
    void getUnmodifiableCollection_List_ReturnsUnmodifiableList() {
        List<String> original = new ArrayList<>(Arrays.asList("x", "y"));
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(original);
        assertTrue(result instanceof List, "Result should be a List");
        List<String> unmodifiable = (List<String>) result;
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add("z"));
        assertEquals(List.of("x", "y"), unmodifiable);
    }

    @Test
    void getUnmodifiableCollection_OtherCollection_ReturnsUnmodifiableCollection() {
        // ArrayDeque implements Collection but not List/Set/NavigableSet/SortedSet
        Collection<Integer> original = new ArrayDeque<>(Arrays.asList(5, 6, 7));
        Collection<Integer> result = CollectionUtilities.getUnmodifiableCollection(original);
        // The returned collection should be unmodifiable but still a Collection
        assertTrue(result instanceof Collection, "Result should be a Collection");
        assertFalse(result instanceof List, "Result should not be a List");
        assertFalse(result instanceof Set, "Result should not be a Set");
        assertThrows(UnsupportedOperationException.class, () -> result.add(8));
        assertEquals(Set.of(5, 6, 7), new HashSet<>(result));
    }

    @Test
    void getUnmodifiableCollection_EmptySortedSet_ReturnsUnmodifiableSortedSet() {
        // Collections.emptySortedSet() returns a SortedSet (which also implements NavigableSet)
        SortedSet<String> original = Collections.emptySortedSet();
        Collection<String> result = CollectionUtilities.getUnmodifiableCollection(original);
        // At minimum it must be a SortedSet; the implementation will actually be an
        // unmodifiable NavigableSet, which is fine for the contract.
        assertTrue(result instanceof SortedSet, "Result should be a SortedSet");
        SortedSet<String> unmodifiable = (SortedSet<String>) result;
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.add("any"));
        assertTrue(unmodifiable.isEmpty(), "The unmodifiable SortedSet should be empty");
    }

    @ParameterizedTest(name = "Empty {2} should be unmodifiable")
    @MethodSource("emptyCollectionProvider")
    void getUnmodifiableCollection_EmptyCollections_ReturnUnmodifiable(Collection<?> original, Class<?> expectedInterface, String description) {
        Collection<?> result = CollectionUtilities.getUnmodifiableCollection(original);
        // Verify the result implements the expected interface (List, Set or generic Collection)
        assertTrue(expectedInterface.isInstance(result), () -> "Result should be an instance of " + expectedInterface.getSimpleName());
        // Any mutating operation must throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> result.add(new Object()), "Adding to an unmodifiable " + description + " must fail");
        // The collection must be empty
        assertTrue(result.isEmpty(), "The unmodifiable " + description + " should be empty");
    }
}
