package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_addAll_Test {

    private ConcurrentSet<Integer> intSet;

    private ConcurrentSet<String> stringSet;

    @BeforeEach
    void setupBeforeEach() {
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required.
        intSet = null;
        stringSet = null;
    }

    static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// case: three distinct elements
        Arguments.of(Arrays.asList("apple", "banana", "cherry"), true, new HashSet<>(Arrays.asList("apple", "banana", "cherry"))), // case: duplicate element inside collection
        Arguments.of(Arrays.asList("apple", "banana", "apple"), true, new HashSet<>(Arrays.asList("apple", "banana"))));
    }

    @Test
    void addAllReturnsTrueWhenNewElementsAreAdded() {
        // given
        List<Integer> toAdd = Arrays.asList(1, 2, 3);
        // when
        boolean modified = intSet.addAll(toAdd);
        // then
        assertTrue(modified, "addAll should return true when at least one new element is added");
        assertEquals(3, intSet.size(), "Set size should reflect the number of distinct elements added");
        assertTrue(intSet.containsAll(toAdd), "Set should contain all elements that were added");
    }

    @Test
    void addAllReturnsFalseWhenCollectionIsEmpty() {
        // given
        List<Integer> empty = Collections.emptyList();
        // when
        boolean modified = intSet.addAll(empty);
        // then
        assertFalse(modified, "addAll should return false when the collection is empty");
        assertEquals(0, intSet.size(), "Set should remain empty");
    }

    @Test
    void addAllReturnsFalseWhenAllElementsAlreadyPresent() {
        // given
        intSet.addAll(Arrays.asList(1, 2, 3));
        assertEquals(3, intSet.size(), "Pre‑condition: set should contain three elements");
        // when
        boolean modified = intSet.addAll(Arrays.asList(1, 2, 3));
        // then
        assertFalse(modified, "addAll should return false when no new elements are introduced");
        assertEquals(3, intSet.size(), "Set size should remain unchanged");
    }

    @Test
    void addAllHandlesNullElementsCorrectly() {
        // given
        List<String> withNull = Arrays.asList("a", null, "b");
        // when
        boolean modified = stringSet.addAll(withNull);
        // then
        assertTrue(modified, "addAll should return true when null is added as a new element");
        assertEquals(3, stringSet.size(), "Set should contain three distinct elements including null");
        assertTrue(stringSet.contains(null), "Set must contain the null element");
        assertTrue(stringSet.containsAll(Arrays.asList("a", "b")), "Set must contain the non‑null elements");
    }

    @Test
    void addAllIgnoresDuplicateElementsWithinTheSuppliedCollection() {
        // given
        List<Integer> withDuplicates = Arrays.asList(1, 2, 2, 3, 1);
        // when
        boolean modified = intSet.addAll(withDuplicates);
        // then
        assertTrue(modified, "addAll should return true when at least one new element is added");
        assertEquals(3, intSet.size(), "Set should contain only distinct elements");
        assertTrue(intSet.containsAll(Arrays.asList(1, 2, 3)), "Set should contain the distinct elements");
    }

    @Test
    void addAllWorksWithConcurrentSetConstructorTakingCollection() {
        // given
        List<Integer> initial = Arrays.asList(5, 6);
        ConcurrentSet<Integer> constructed = new ConcurrentSet<>(initial);
        assertEquals(2, constructed.size(), "Constructor should have added the initial elements");
        // when
        boolean modified = constructed.addAll(Arrays.asList(6, 7, 8));
        // then
        assertTrue(modified, "addAll should return true because new elements (7,8) are added");
        assertEquals(4, constructed.size(), "Set size should reflect newly added distinct elements");
        assertTrue(constructed.containsAll(Arrays.asList(5, 6, 7, 8)));
    }

    /* ----------------------------------------------------------------------
     *  Group: addAll - basic functionality
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: collection={0}")
    @MethodSource("basicFunctionalityProvider")
    void addAllBasicFunctionality(List<String> collection, boolean expectedModified, Set<String> expectedContent) {
        // given
        ConcurrentSet<String> set = new ConcurrentSet<>();
        // when
        boolean modified = set.addAll(collection);
        // then
        assertEquals(expectedModified, modified, "addAll should return " + expectedModified + " for collection " + collection);
        assertEquals(expectedContent.size(), set.size(), "Set size should match number of distinct elements after addAll");
        assertTrue(set.containsAll(expectedContent), "Set should contain all expected elements after addAll");
    }

    /* ----------------------------------------------------------------------
     *  Group: addAll - empty collection
     * ---------------------------------------------------------------------- */
    @Test
    void addAllEmptyCollectionReturnsFalse() {
        // given
        List<Integer> empty = Collections.emptyList();
        // when
        boolean modified = intSet.addAll(empty);
        // then
        assertFalse(modified, "addAll should return false when the collection is empty");
        assertEquals(0, intSet.size(), "Set should remain empty after adding an empty collection");
    }

    /* ----------------------------------------------------------------------
     *  Group: addAll - all elements already present
     * ---------------------------------------------------------------------- */
    @Test
    void addAllAllElementsAlreadyPresentReturnsFalse() {
        // given
        intSet.addAll(Arrays.asList(1, 2, 3));
        assertEquals(3, intSet.size(), "Pre‑condition: set should contain three elements");
        // when
        boolean modified = intSet.addAll(Arrays.asList(1, 2, 3));
        // then
        assertFalse(modified, "addAll should return false when no new elements are introduced");
        assertEquals(3, intSet.size(), "Set size should remain unchanged");
    }

    /* ----------------------------------------------------------------------
     *  Group: addAll - mixed new and existing elements
     * ---------------------------------------------------------------------- */
    @Test
    void addAllMixedNewAndExistingElements() {
        // given
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("alpha", "beta"));
        assertEquals(2, set.size());
        // when
        boolean modified = set.addAll(Arrays.asList("delta", "beta", "epsilon"));
        // then
        assertTrue(modified, "addAll should return true because new elements are added");
        assertEquals(4, set.size(), "Set size should reflect newly added distinct elements");
        assertTrue(set.containsAll(Arrays.asList("alpha", "beta", "delta", "epsilon")), "Set must contain all original and newly added elements");
    }

    /* ----------------------------------------------------------------------
     *  Group: addAll - null collection argument
     * ---------------------------------------------------------------------- */
    @Test
    void addAllNullCollectionThrowsNullPointerException() {
        // given
        ConcurrentSet<String> set = new ConcurrentSet<>();
        // when / then
        assertThrows(NullPointerException.class, () -> set.addAll(null), "addAll should throw NullPointerException when the collection argument is null");
    }

    /* ----------------------------------------------------------------------
     *  Group: addAll - collection containing null element
     * ---------------------------------------------------------------------- */
    @Test
    void addAllCollectionContainingNullElementThrowsNullPointerException() {
        // given
        List<String> withNull = Arrays.asList(null, "zeta");
        ConcurrentSet<String> set = new ConcurrentSet<>();
        // when / then
        assertThrows(NullPointerException.class, () -> set.addAll(withNull), "addAll should propagate NullPointerException when the collection contains a null element");
    }

    /* ----------------------------------------------------------------------
     *  Group: addAll - adding the set to itself
     * ---------------------------------------------------------------------- */
    @Test
    void addAllAddingSetToItselfDoesNotModify() {
        // given
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("one", "two"));
        int originalSize = set.size();
        // when
        // self‑addition
        boolean modified = set.addAll(set);
        // then
        assertFalse(modified, "addAll(self) should return false because no new elements are added");
        assertEquals(originalSize, set.size(), "Set size must remain unchanged after adding the set to itself");
        assertTrue(set.containsAll(Arrays.asList("one", "two")), "Original elements must still be present");
    }

    /* ----------------------------------------------------------------------
     *  Group: addAll - incompatible element type
     * ---------------------------------------------------------------------- */
    @Test
    void addAllIncompatibleElementTypeThrowsClassCastException() {
        // given
        ConcurrentSet<String> set = new ConcurrentSet<>();
        // 42 cannot be cast to String
        List<Object> mixed = Arrays.asList("alpha", 42);
        // when / then
        assertThrows(ClassCastException.class, () -> {
            // The raw type forces the compiler to accept the mismatched list,
            // but at runtime the enhanced‑for loop will attempt to cast 42 to String.
            @SuppressWarnings("unchecked")
            Collection<String> casted = (Collection<String>) (Collection<?>) mixed;
            set.addAll(casted);
        }, "addAll should throw ClassCastException when collection contains an element of incompatible type");
    }
}
