package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_tailSet_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    private SealableNavigableSet<Integer> intSet;

    // for comparator tests
    private SealableNavigableSet<String> stringSet;

    @BeforeEach
    void setupBeforeEach() {
        // fresh integer set for the majority of tests
        intSet = new SealableNavigableSet<>(sealedSupplier);
        // populate with a predictable ordering (null is allowed by the implementation)
        intSet.add(null);
        intSet.add(1);
        intSet.add(2);
        intSet.add(3);
        intSet.add(4);
        intSet.add(5);
        intSet.add(6);
        intSet.add(10);
        intSet.add(20);
        intSet.add(30);
        sealedState = false;
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the sealed flag for the next test
        sealedState = false;
    }

    /* --------------------------------------------------------------
     *  Parameterised tests for the "valid range" and "edge case"
     *  groups extracted from the JSON description.
     * -------------------------------------------------------------- */
    private static Stream<Arguments> validAndEdgeCases() {
        return Stream.of(// Valid range – fromElement present in set (exclusive)
        Arguments.of(3, List.of(4, 5, 6, 10, 20, 30)), // Valid range – fromElement not present in set (4.5)
        Arguments.of(4.5, List.of(5, 6, 10, 20, 30)), // Edge case – fromElement less than the smallest element
        Arguments.of(-10, List.of(1, 2, 3, 4, 5, 6, 10, 20, 30)), // Edge case – fromElement greater than the largest element
        Arguments.of(100, List.of()));
    }

    @Test
    void tailSet_exclusiveByDefault() {
        // The simple tailSet(E) method is defined as tailSet(fromElement, false)
        SortedSet<Integer> tail = set.tailSet(20);
        // Elements strictly greater than 20 should be present
        assertEquals(1, tail.size(), "Tail set should contain only one element");
        assertTrue(tail.contains(30), "Tail set should contain 30");
        assertFalse(tail.contains(20), "Tail set should not contain the fromElement (20)");
        // Null is less than any non‑null Integer, therefore not present
        assertFalse(tail.contains(null));
    }

    @Test
    void tailSet_modificationAllowedWhenUnsealed() {
        SortedSet<Integer> tail = set.tailSet(10);
        // The set is currently unsealed, so modifications must succeed
        assertDoesNotThrow(() -> tail.add(25));
        assertTrue(tail.contains(25));
        assertTrue(set.contains(25), "Underlying set should reflect the addition");
    }

    @Test
    void tailSet_modificationForbiddenWhenSealed() {
        SortedSet<Integer> tail = intSet.tailSet(10);
        // seal the set
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> tail.add(25));
        assertThrows(UnsupportedOperationException.class, tail::clear);
    }

    @Test
    void tailSet_reflectsChangesInUnderlyingSet() {
        SortedSet<Integer> tail = set.tailSet(15);
        // Initially only 20 and 30 are in the view
        assertEquals(2, tail.size());
        assertTrue(tail.contains(20));
        assertTrue(tail.contains(30));
        // Add a new element to the underlying set that belongs to the tail view
        assertDoesNotThrow(() -> set.add(25));
        assertTrue(tail.contains(25), "Tail view should see newly added element 25");
        assertEquals(3, tail.size());
        // Remove an element via the underlying set and verify the view updates
        assertTrue(set.remove(20));
        assertFalse(tail.contains(20));
        assertEquals(2, tail.size());
    }

    @ParameterizedTest(name = "tailSet({0}) should contain {1}")
    @MethodSource("validAndEdgeCases")
    void tailSet_exclusiveByDefault_VariousScenarios(Number fromElement, List<Integer> expected) {
        // The method under test is tailSet(E) which is exclusive by default.
        SortedSet<Integer> tail = intSet.tailSet(fromElement.intValue());
        assertEquals(expected.size(), tail.size(), "Unexpected size for tailSet(" + fromElement + ")");
        assertIterableEquals(expected, tail, "Unexpected elements for tailSet(" + fromElement + ")");
    }

    /* --------------------------------------------------------------
     *  Empty original set
     * -------------------------------------------------------------- */
    @Test
    void tailSet_onEmptySet_returnsEmptyView() {
        SealableNavigableSet<Integer> empty = new SealableNavigableSet<>(sealedSupplier);
        SortedSet<Integer> tail = empty.tailSet(1);
        assertTrue(tail.isEmpty(), "Tail view of an empty set must be empty");
        assertEquals(0, tail.size());
    }

    /* --------------------------------------------------------------
     *  Null argument handling – the implementation does not allow null
     * -------------------------------------------------------------- */
    @Test
    void tailSet_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> intSet.tailSet(null));
    }

    /* --------------------------------------------------------------
     *  Incompatible type – element not comparable to set elements
     * -------------------------------------------------------------- */
    @Test
    void tailSet_incompatibleType_throwsClassCastException() {
        // The set holds Integers; passing a String should fail during comparison.
        assertThrows(ClassCastException.class, () -> intSet.tailSet("string"));
    }

    /* --------------------------------------------------------------
     *  Sealed set – modification attempts on returned view
     * -------------------------------------------------------------- */
    @Test
    void tailSet_sealed_modificationAttemptsThrow() {
        SortedSet<Integer> tail = intSet.tailSet(2);
        // seal the backing set
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> tail.add(25));
        assertThrows(UnsupportedOperationException.class, tail::clear);
    }

    /* --------------------------------------------------------------
     *  Sealed set – read‑only operations on tailSet view
     * -------------------------------------------------------------- */
    @Test
    void tailSet_sealed_readOnlyOperationsSucceed() {
        sealedState = true;
        SortedSet<Integer> tail = intSet.tailSet(2);
        // contains, iterator, size, isEmpty must work without throwing
        assertTrue(tail.contains(3));
        assertFalse(tail.isEmpty());
        List<Integer> iterated = new ArrayList<>();
        tail.iterator().forEachRemaining(iterated::add);
        assertIterableEquals(List.of(3, 4, 5, 6, 10, 20, 30), iterated);
    }

    /* --------------------------------------------------------------
     *  View reflects subsequent modifications to original set
     * -------------------------------------------------------------- */
    @Test
    void tailSet_viewReflectsAdditionsAndRemovals() {
        SortedSet<Integer> tail = intSet.tailSet(3);
        // initial view should contain 4,5,6,10,20,30
        assertIterableEquals(List.of(4, 5, 6, 10, 20, 30), tail);
        // add a new element that belongs to the view
        assertDoesNotThrow(() -> intSet.add(25));
        assertTrue(tail.contains(25));
        assertEquals(7, tail.size());
        // remove an element via the backing set
        assertTrue(intSet.remove(20));
        assertFalse(tail.contains(20));
        assertEquals(6, tail.size());
    }

    /* --------------------------------------------------------------
     *  Single‑element set scenarios
     * -------------------------------------------------------------- */
    @Test
    void tailSet_singleElementSet_cases() {
        SealableNavigableSet<Integer> single = new SealableNavigableSet<>(sealedSupplier);
        single.add(10);
        // fromElement == 10 -> empty view (exclusive)
        SortedSet<Integer> tail1 = single.tailSet(10);
        assertTrue(tail1.isEmpty());
        // fromElement < element -> view contains the sole element
        SortedSet<Integer> tail2 = single.tailSet(5);
        assertEquals(1, tail2.size());
        assertTrue(tail2.contains(10));
        // fromElement > element -> empty view
        SortedSet<Integer> tail3 = single.tailSet(15);
        assertTrue(tail3.isEmpty());
    }

    /* --------------------------------------------------------------
     *  Custom comparator – reverse order
     * -------------------------------------------------------------- */
    @Test
    void tailSet_reverseComparator_behaviour() {
        // Build a set with a descending comparator
        SealableNavigableSet<Integer> rev = new SealableNavigableSet<>(Comparator.reverseOrder(), sealedSupplier);
        rev.add(10);
        rev.add(20);
        rev.add(30);
        // With reverse order, tailSet(30) returns elements that are *less* than 30
        SortedSet<Integer> tail = rev.tailSet(30);
        // Expected order is according to the comparator (descending)
        assertIterableEquals(List.of(20, 10), tail);
        // fromElement smaller than any element (5) -> whole set
        SortedSet<Integer> whole = rev.tailSet(5);
        assertIterableEquals(List.of(30, 20, 10), whole);
    }

    /* --------------------------------------------------------------
     *  Custom comparator – case‑insensitive strings
     * -------------------------------------------------------------- */
    @Test
    void tailSet_caseInsensitiveStringComparator() {
        // Build a set with CASE_INSENSITIVE_ORDER comparator
        SealableNavigableSet<String> strSet = new SealableNavigableSet<>(String.CASE_INSENSITIVE_ORDER, sealedSupplier);
        strSet.add("a");
        strSet.add("b");
        strSet.add("c");
        strSet.add("d");
        // fromElement "b" (any case) -> view should contain "c","d"
        SortedSet<String> tailB = strSet.tailSet("b");
        assertIterableEquals(List.of("c", "d"), tailB);
        // fromElement "B" (different case) -> same result
        SortedSet<String> tailUpperB = strSet.tailSet("B");
        assertIterableEquals(List.of("c", "d"), tailUpperB);
    }

    /* --------------------------------------------------------------
     *  Iterator order of tailSet view
     * -------------------------------------------------------------- */
    @Test
    void tailSet_iteratorOrder_isNatural() {
        SortedSet<Integer> tail = intSet.tailSet(3);
        List<Integer> iterated = new ArrayList<>();
        tail.iterator().forEachRemaining(iterated::add);
        assertIterableEquals(List.of(4, 5, 6, 10, 20, 30), iterated, "Iterator must follow natural ordering of the underlying set");
    }

    /* --------------------------------------------------------------
     *  Size and emptiness of tailSet view
     * -------------------------------------------------------------- */
    @Test
    void tailSet_sizeAndIsEmpty_behaviour() {
        // fromElement = 4 -> view should contain 5,6,10,20,30 (size 5)
        SortedSet<Integer> tail = intSet.tailSet(4);
        assertEquals(5, tail.size());
        assertFalse(tail.isEmpty());
        // fromElement = 100 -> empty view
        SortedSet<Integer> emptyTail = intSet.tailSet(100);
        assertEquals(0, emptyTail.size());
        assertTrue(emptyTail.isEmpty());
    }

    /* --------------------------------------------------------------
     *  toArray behavior of tailSet view
     * -------------------------------------------------------------- */
    @Test
    void tailSet_toArray_returnsCorrectArray() {
        SortedSet<Integer> tail = intSet.tailSet(3);
        Object[] array = tail.toArray();
        assertArrayEquals(new Object[] { 4, 5, 6, 10, 20, 30 }, array);
        SortedSet<Integer> emptyTail = intSet.tailSet(100);
        assertArrayEquals(new Object[] {}, emptyTail.toArray());
    }

    /* --------------------------------------------------------------
     *  subSet operation on tailSet view
     * -------------------------------------------------------------- */
    @Test
    void tailSet_subSetOnView_respectsInclusiveFlags() {
        // Base set: [1,2,3,4,5,6]
        SealableNavigableSet<Integer> base = new SealableNavigableSet<>(sealedSupplier);
        base.addAll(Arrays.asList(1, 2, 3, 4, 5, 6));
        // view = [3,4,5,6]
        SortedSet<Integer> tail = base.tailSet(2);
        // should contain only 4
        SortedSet<Integer> sub = tail.subSet(4, true, 5, false);
        assertIterableEquals(List.of(4), sub);
    }

    /* --------------------------------------------------------------
     *  Existing tests from the original framework (kept unchanged)
     * -------------------------------------------------------------- */
    @Test
    void tailSet_exclusiveByDefault_original() {
        SortedSet<Integer> tail = intSet.tailSet(20);
        assertEquals(1, tail.size(), "Tail set should contain only one element");
        assertTrue(tail.contains(30), "Tail set should contain 30");
        assertFalse(tail.contains(20), "Tail set should not contain the fromElement (20)");
        assertFalse(tail.contains(null));
    }
}
