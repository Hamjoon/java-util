package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_headSet_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh set for each test – natural ordering
        set = new SealableNavigableSet<>(sealedSupplier);
        // Populate with a known ordering; tests that need a different collection will replace it.
        set.addAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7));
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        set = null;
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the "Normal behavior" group
     * -------------------------------------------------------------- */
    private static Stream<Arguments> normalBehaviorProvider() {
        return Stream.of(// toElement = 5 → exclusive view should contain 1‑4
        Arguments.of(5, List.of(1, 2, 3, 4)), // toElement larger than any element → view contains all
        Arguments.of(10, List.of(1, 2, 3, 4, 5, 6, 7)), // toElement equal to smallest element → empty view
        Arguments.of(1, List.of()));
    }

    @Test
    void testHeadSetExclusiveDefault() {
        // headSet(E) delegates to headSet(E, false) – the element itself must be excluded
        SortedSet<Integer> head = set.headSet(30);
        assertEquals(2, head.size(), "Expected two elements (<30)");
        assertTrue(head.contains(10));
        assertTrue(head.contains(20));
        assertFalse(head.contains(30));
        assertFalse(head.contains(40));
    }

    @Test
    void testHeadSetInclusiveTrue() {
        SortedSet<Integer> head = set.headSet(30, true);
        assertEquals(3, head.size(), "Expected three elements (≤30)");
        assertTrue(head.contains(10));
        assertTrue(head.contains(20));
        assertTrue(head.contains(30));
        assertFalse(head.contains(40));
    }

    @Test
    void testHeadSetModificationWhenSealed() {
        // seal the underlying set
        sealedState = true;
        SortedSet<Integer> head = set.headSet(30);
        assertThrows(UnsupportedOperationException.class, () -> head.add(5));
        assertThrows(UnsupportedOperationException.class, head::clear);
    }

    @Test
    void testHeadSetModificationWhenUnsealed() {
        // ensure the set is mutable
        sealedState = false;
        SortedSet<Integer> head = set.headSet(30);
        assertDoesNotThrow(() -> head.add(5));
        assertTrue(head.contains(5));
        assertTrue(set.contains(5), "Underlying set must reflect the addition");
        // Adding an element that is equal to the bound should fail for exclusive view
        assertThrows(IllegalArgumentException.class, () -> head.add(30));
    }

    @Test
    void testHeadSetIllegalArgumentOnAdd() {
        // Exclusive view – element equal to bound is illegal
        SortedSet<Integer> exclusive = set.headSet(30);
        assertThrows(IllegalArgumentException.class, () -> exclusive.add(30));
        // Inclusive view – element greater than bound is illegal
        SortedSet<Integer> inclusive = set.headSet(30, true);
        assertThrows(IllegalArgumentException.class, () -> inclusive.add(31));
    }

    @Test
    void testHeadSetReflectsUnderlyingChanges() {
        SortedSet<Integer> head = set.headSet(30);
        assertEquals(2, head.size());
        // Add a new element that belongs to the head set directly via the main set
        set.add(25);
        assertTrue(head.contains(25));
        assertEquals(3, head.size());
        // Remove an element via the main set and verify the view updates
        set.remove(10);
        assertFalse(head.contains(10));
        assertEquals(2, head.size());
    }

    @ParameterizedTest(name = "headSet({0}) exclusive returns {1}")
    @MethodSource("normalBehaviorProvider")
    void testHeadSetExclusiveNormal(int toElement, List<Integer> expected) {
        SortedSet<Integer> head = set.headSet(toElement);
        assertEquals(expected.size(), head.size(), "Unexpected size for headSet(" + toElement + ")");
        assertIterableEquals(expected, head, "Unexpected elements for headSet(" + toElement + ")");
    }

    /* --------------------------------------------------------------
     *  Empty set behavior
     * -------------------------------------------------------------- */
    @Test
    void testHeadSetOnEmptySetReturnsEmptyView() {
        // Replace the populated set with an empty one
        set.clear();
        SortedSet<Integer> head = set.headSet(5);
        assertTrue(head.isEmpty(), "headSet on an empty set must be empty");
    }

    @Test
    void testHeadSetNullOnEmptySetThrowsNPE() {
        set.clear();
        assertThrows(NullPointerException.class, () -> set.headSet(null));
    }

    /* --------------------------------------------------------------
     *  Boundary and error conditions
     * -------------------------------------------------------------- */
    @Test
    void testHeadSetBelowLowestElementReturnsEmpty() {
        // Set currently contains 1‑7
        SortedSet<Integer> head = set.headSet(0);
        assertTrue(head.isEmpty(), "headSet(0) should be empty when 0 < smallest element");
    }

    @Test
    void testHeadSetWithWrongTypeThrowsClassCastException() {
        // The set is of Integer; passing a String should fail
        assertThrows(ClassCastException.class, () -> set.headSet((Integer) (Object) "a"));
    }

    @Test
    void testHeadSetModificationWhenSealedThrowsUOE() {
        // seal the set
        sealedState = true;
        SortedSet<Integer> head = set.headSet(5);
        assertThrows(UnsupportedOperationException.class, () -> head.add(0));
        assertThrows(UnsupportedOperationException.class, head::clear);
    }

    /* --------------------------------------------------------------
     *  View mutability and backing semantics
     * -------------------------------------------------------------- */
    @Test
    void testHeadSetReflectsAdditionToUnderlyingSet() {
        SortedSet<Integer> head = set.headSet(5);
        assertIterableEquals(List.of(1, 2, 3, 4), head);
        // Add an element that belongs to the head view via the original set
        set.add(0);
        assertTrue(head.contains(0), "View must reflect addition to underlying set");
        assertEquals(5, head.size());
    }

    @Test
    void testHeadSetRemovalViaViewAffectsUnderlyingSet() {
        SortedSet<Integer> head = set.headSet(5);
        assertTrue(head.remove(2), "Removal via view should succeed");
        assertFalse(set.contains(2), "Underlying set must no longer contain removed element");
        assertEquals(3, head.size());
    }

    /* --------------------------------------------------------------
     *  Custom comparator behavior (reverse order)
     * -------------------------------------------------------------- */
    @Test
    void testHeadSetWithReverseComparatorExclusive() throws Exception {
        // Build a set with a descending comparator via reflection (constructor not part of the public API)
        Comparator<Integer> reverse = Comparator.reverseOrder();
        Constructor<SealableNavigableSet> ctor = SealableNavigableSet.class.getDeclaredConstructor(Comparator.class, Supplier.class);
        ctor.setAccessible(true);
        @SuppressWarnings("unchecked")
        SealableNavigableSet<Integer> reverseSet = (SealableNavigableSet<Integer>) ctor.newInstance(reverse, sealedSupplier);
        // natural order insertion; comparator will order descending
        reverseSet.addAll(Arrays.asList(5, 4, 3, 2, 1));
        // headSet(3) exclusive in descending order → elements greater than 3 (i.e., 5,4)
        SortedSet<Integer> head = reverseSet.headSet(3);
        assertIterableEquals(List.of(5, 4), head);
    }

    @Test
    void testHeadSetWithReverseComparatorExclusiveEmpty() throws Exception {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        Constructor<SealableNavigableSet> ctor = SealableNavigableSet.class.getDeclaredConstructor(Comparator.class, Supplier.class);
        ctor.setAccessible(true);
        @SuppressWarnings("unchecked")
        SealableNavigableSet<Integer> reverseSet = (SealableNavigableSet<Integer>) ctor.newInstance(reverse, sealedSupplier);
        reverseSet.addAll(Arrays.asList(5, 4, 3, 2, 1));
        // headSet(5) exclusive → no element is 'greater' than 5 in descending view
        SortedSet<Integer> head = reverseSet.headSet(5);
        assertTrue(head.isEmpty(), "Reverse comparator exclusive headSet(5) should be empty");
    }

    /* --------------------------------------------------------------
     *  Null handling on non‑empty set
     * -------------------------------------------------------------- */
    @Test
    void testHeadSetNullOnNonEmptySetThrowsNPE() {
        assertThrows(NullPointerException.class, () -> set.headSet(null));
    }

    /* --------------------------------------------------------------
     *  IllegalArgumentException on subSet view bounds
     * -------------------------------------------------------------- */
    @Test
    void testHeadSetOnSubSetWithUpperBoundViolationThrowsIAE() {
        // Create a sub‑set view from 2 (inclusive) to 5 (inclusive)
        NavigableSet<Integer> sub = set.subSet(2, true, 5, true);
        // toElement 6 is outside the sub‑set's upper bound
        assertThrows(IllegalArgumentException.class, () -> sub.headSet(6));
    }

    @Test
    void testHeadSetOnSubSetWithLowerBoundViolationThrowsIAE() {
        NavigableSet<Integer> sub = set.subSet(2, true, 5, true);
        // toElement 1 is below the sub‑set's lower bound
        assertThrows(IllegalArgumentException.class, () -> sub.headSet(1));
    }

    /* --------------------------------------------------------------
     *  Additional sanity checks for inclusive overload (already covered
     *  by earlier tests but kept for completeness)
     * -------------------------------------------------------------- */
    @Test
    void testHeadSetInclusiveTrueMatchesExpectation() {
        SortedSet<Integer> head = set.headSet(5, true);
        assertIterableEquals(List.of(1, 2, 3, 4, 5), head);
    }

    @Test
    void testInclusiveModificationWhenSealedThrowsUOE() {
        sealedState = true;
        SortedSet<Integer> head = set.headSet(5, true);
        assertThrows(UnsupportedOperationException.class, () -> head.add(0));
    }

    @Test
    void testInclusiveModificationWhenUnsealedSucceeds() {
        sealedState = false;
        SortedSet<Integer> head = set.headSet(5, true);
        assertDoesNotThrow(() -> head.add(0));
        assertTrue(head.contains(0));
        assertTrue(set.contains(0));
    }
}
