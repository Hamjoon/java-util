package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_containsAll_Test {

    private ConcurrentList<Integer> list;

    /**
     * A minimal collection whose {@code iterator()} method always throws a {@link RuntimeException}.
     */
    private static class ThrowingCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            throw new RuntimeException("Iterator failure");
        }

        @Override
        public int size() {
            // size is irrelevant; iterator() never succeeds
            return 1;
        }
    }

    /**
     * A minimal collection whose {@code iterator()} method always throws an {@link IllegalStateException}.
     */
    private static class IllegalStateCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            throw new IllegalStateException("Illegal state during iteration");
        }

        @Override
        public int size() {
            return 1;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh list for each test to guarantee isolation
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the list to aid GC and avoid accidental cross‑test contamination
        list.clear();
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// Empty collection – must be true even if the list is non‑empty
        Arguments.of(Collections.emptyList(), true), // All elements present – true
        Arguments.of(Arrays.asList("a", "b"), true), // Missing element – false
        Arguments.of(Arrays.asList("a", "c"), false));
    }

    @Test
    void containsAll_ReturnsTrueWhenAllElementsPresent() {
        list.addAll(Arrays.asList(1, 2, 3, 4, 5));
        Collection<Integer> toCheck = Arrays.asList(1, 3, 5);
        assertTrue(list.containsAll(toCheck), "containsAll should return true when every element of the collection is present");
    }

    @Test
    void containsAll_ReturnsFalseWhenMissingElements() {
        list.addAll(Arrays.asList(10, 20, 30));
        // 40 is not in the list
        Collection<Integer> toCheck = Arrays.asList(10, 40);
        assertFalse(list.containsAll(toCheck), "containsAll should return false when the collection contains elements not present in the list");
    }

    @Test
    void containsAll_EmptyCollectionIsAlwaysTrue() {
        list.addAll(Arrays.asList(7, 8, 9));
        Collection<Integer> empty = Collections.emptyList();
        assertTrue(list.containsAll(empty), "containsAll should return true for an empty collection regardless of list contents");
    }

    @Test
    void containsAll_NullCollectionThrowsNPE() {
        list.add(1);
        assertThrows(NullPointerException.class, () -> list.containsAll(null), "containsAll should throw NullPointerException when the argument collection is null");
    }

    @Test
    void containsAll_DifferentObjectTypesAreHandledCorrectly() {
        list.addAll(Arrays.asList(1, 2, 3));
        // Collection with a different generic type but containing matching values
        Collection<String> stringCollection = Arrays.asList("1", "2");
        // Since the list holds Integers, containsAll should return false
        assertFalse(list.containsAll(stringCollection), "containsAll should return false when the collection's element types are not compatible");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for the basic functionality group
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => collection={0}, expected={1}")
    @MethodSource("basicFunctionalityProvider")
    void containsAll_BasicFunctionality(Collection<?> collection, boolean expected) {
        // List always contains "a" and "b"
        list.addAll(Arrays.asList("a", "b"));
        assertEquals(expected, list.containsAll(collection), () -> "containsAll should return " + expected + " for collection " + collection);
    }

    /* -------------------------------------------------------------
     *  Null argument test
     * ------------------------------------------------------------- */
    @Test
    void containsAll_NullArgument_ThrowsNPE() {
        list.add("anything");
        assertThrows(NullPointerException.class, () -> list.containsAll(null), "containsAll should throw NullPointerException when the argument collection is null");
    }

    /* -------------------------------------------------------------
     *  Null element handling
     * ------------------------------------------------------------- */
    @Test
    void containsAll_CollectionWithNull_ElementMissing_ReturnsFalse() {
        list.addAll(Arrays.asList("x", "y"));
        Collection<Object> withNull = Collections.singletonList(null);
        assertFalse(list.containsAll(withNull), "containsAll should return false when collection contains null but list does not");
    }

    @Test
    void containsAll_CollectionWithNull_ElementPresent_ReturnsTrue() {
        list.addAll(Arrays.asList("x", null, "y"));
        Collection<Object> withNull = Collections.singletonList(null);
        assertTrue(list.containsAll(withNull), "containsAll should return true when both list and collection contain null");
    }

    /* -------------------------------------------------------------
     *  Empty list vs non‑empty collection
     * ------------------------------------------------------------- */
    @Test
    void containsAll_EmptyList_NonEmptyCollection_ReturnsFalse() {
        // list is intentionally left empty
        Collection<String> coll = Collections.singletonList("x");
        assertFalse(list.containsAll(coll), "An empty list cannot contain any non‑empty collection");
    }

    /* -------------------------------------------------------------
     *  Duplicate elements in the argument collection
     * ------------------------------------------------------------- */
    @Test
    void containsAll_CollectionWithDuplicates_IgnoredBySetLogic_ReturnsTrue() {
        list.addAll(Arrays.asList("a"));
        Collection<String> dupes = Arrays.asList("a", "a");
        assertTrue(list.containsAll(dupes), "Duplicates in the collection should not affect the result");
    }

    /* -------------------------------------------------------------
     *  Different generic types
     * ------------------------------------------------------------- */
    @Test
    void containsAll_DifferentElementTypes_ReturnsFalse() {
        list.addAll(Arrays.asList("1", "2"));
        Collection<Integer> ints = Arrays.asList(1, 2);
        assertFalse(list.containsAll(ints), "Type mismatch should cause containsAll to return false");
    }

    /* -------------------------------------------------------------
     *  Collection is a Set implementation
     * ------------------------------------------------------------- */
    @Test
    void containsAll_SetImplementation_ReturnsTrueWhenElementPresent() {
        list.addAll(Arrays.asList("a", "b"));
        Set<String> set = new HashSet<>(Collections.singletonList("b"));
        assertTrue(list.containsAll(set), "containsAll must work with any Collection implementation, including Set");
    }

    /* -------------------------------------------------------------
     *  Propagation of iterator exceptions
     * ------------------------------------------------------------- */
    @Test
    void containsAll_CollectionIteratorThrowsRuntimeException_Propagates() {
        list.addAll(Arrays.asList("x"));
        Collection<Object> throwing = new ThrowingCollection<>();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> list.containsAll(throwing), "RuntimeException from iterator should be propagated");
        assertEquals("Iterator failure", ex.getMessage());
    }

    @Test
    void containsAll_CollectionIteratorThrowsIllegalStateException_Propagates() {
        list.addAll(Arrays.asList("x"));
        Collection<Object> illegal = new IllegalStateCollection<>();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> list.containsAll(illegal), "IllegalStateException from iterator should be propagated");
        assertEquals("Illegal state during iteration", ex.getMessage());
    }
}
