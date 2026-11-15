package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_pollFirst_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test and populate it with a known ordering
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps the garbage collector in long test suites
        set = null;
        sealedState = false;
    }

    /* --------------------------------------------------------------
     * Parameterized test for the simple non‑empty sequence (re‑using JSON data)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> nonEmptySequenceProvider() {
        return Stream.of(Arguments.of(Arrays.asList(1, 2, 3), new int[] { 1, 2 }, "non‑empty underlying set"));
    }

    @Test
    void pollFirst_Unsealed_ReturnsFirstElementAndRemovesIt() {
        // given: set is unsealed (sealedState == false) and contains 10,20,30
        assertFalse(sealedState, "Set should start unsealed");
        // when
        Integer first = set.pollFirst();
        // then
        assertEquals(10, first, "pollFirst should return the smallest element");
        assertEquals(2, set.size(), "Element should be removed from the set");
        assertFalse(set.contains(10), "Removed element must no longer be present");
    }

    @Test
    void pollFirst_Unsealed_EmptySetReturnsNull() {
        // given: an empty set
        set.clear();
        assertTrue(set.isEmpty(), "Set should be empty before pollFirst");
        // when
        Integer result = set.pollFirst();
        // then
        assertNull(result, "pollFirst on an empty set should return null");
    }

    @Test
    void pollFirst_Sealed_ThrowsUnsupportedOperationException() {
        // given: the set is sealed
        sealedState = true;
        assertTrue(sealedState, "Set should be sealed for this test");
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> set.pollFirst(), "pollFirst must throw when the set is sealed");
    }

    /**
     * Replace the private {@code navSet} field of the {@link SealableNavigableSet}
     * with the supplied {@code NavigableSet}.  Used for tests that need a
     * custom underlying implementation (e.g. a mock or a TreeSet with a
     * custom comparator).
     */
    @SuppressWarnings("unchecked")
    private void setUnderlyingNavigableSet(NavigableSet<Integer> newNavSet) {
        try {
            Field f = SealableNavigableSet.class.getDeclaredField("navSet");
            f.setAccessible(true);
            f.set(set, newNavSet);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * pollFirst - sealed state **
     */
    @Test
    void pollFirst_Sealed_ThrowsIllegalStateException() {
        // seal the set
        sealedState = true;
        assertTrue(sealedState, "Set should be sealed for this test");
        // The implementation is documented to throw IllegalStateException (or a subclass)
        assertThrows(IllegalStateException.class, () -> set.pollFirst(), "pollFirst must throw IllegalStateException when the set is sealed");
    }

    /**
     * pollFirst - non‑empty underlying set **
     */
    @Test
    void pollFirst_NonEmpty_UnderlyingSetReturnsFirstThenSecond() {
        // first call – should return 10
        Integer first = set.pollFirst();
        assertEquals(10, first);
        assertEquals(2, set.size());
        assertFalse(set.contains(10));
        // second call – should return 20
        Integer second = set.pollFirst();
        assertEquals(20, second);
        assertEquals(1, set.size());
        assertFalse(set.contains(20));
    }

    /**
     * pollFirst - empty underlying set **
     */
    @Test
    void pollFirst_EmptyUnderlyingSet_ReturnsNull() {
        set.clear();
        assertTrue(set.isEmpty(), "Set should be empty before pollFirst");
        assertNull(set.pollFirst(), "pollFirst on an empty set should return null");
    }

    /**
     * pollFirst - single element set **
     */
    @Test
    void pollFirst_SingleElement_ReturnsElementThenNull() {
        set.clear();
        set.add(42);
        // first poll – returns the only element
        Integer first = set.pollFirst();
        assertEquals(42, first);
        assertTrue(set.isEmpty(), "Set should be empty after removing the sole element");
        // second poll – returns null
        assertNull(set.pollFirst(), "Second pollFirst on now‑empty set should return null");
    }

    /**
     * pollFirst - after full depletion **
     */
    @Test
    void pollFirst_FullDepletionSequence() {
        set.clear();
        set.add(5);
        set.add(10);
        assertEquals(5, set.pollFirst(), "First pollFirst should return 5");
        assertEquals(1, set.size());
        assertEquals(10, set.pollFirst(), "Second pollFirst should return 10");
        assertTrue(set.isEmpty());
        assertNull(set.pollFirst(), "Third pollFirst on empty set should return null");
    }

    /**
     * pollFirst - underlying set throws UnsupportedOperationException **
     */
    @Test
    void pollFirst_UnderlyingSetThrowsUnsupportedOperationException(@Mock NavigableSet<Integer> mockNav) {
        // Configure mock to throw when pollFirst() is invoked
        when(mockNav.pollFirst()).thenThrow(new UnsupportedOperationException("mock"));
        setUnderlyingNavigableSet(mockNav);
        // Seal check must pass (set is unsealed)
        assertFalse(sealedState);
        // The exception from the underlying set must propagate
        assertThrows(UnsupportedOperationException.class, () -> set.pollFirst(), "UnsupportedOperationException from underlying set must be propagated");
    }

    /**
     * pollFirst - sealed after partial removal **
     */
    @Test
    void pollFirst_SealedAfterPartialRemoval() {
        // first poll succeeds
        assertEquals(10, set.pollFirst(), "First pollFirst should return 10");
        // now seal the set
        sealedState = true;
        assertThrows(IllegalStateException.class, () -> set.pollFirst(), "After sealing, subsequent pollFirst must throw IllegalStateException");
    }

    /**
     * pollFirst - custom comparator ordering **
     */
    @Test
    void pollFirst_CustomComparator_DescendingOrder() {
        // TreeSet with a comparator that orders integers descending
        NavigableSet<Integer> descendingSet = new TreeSet<>(Comparator.reverseOrder());
        descendingSet.addAll(Arrays.asList(1, 2, 3));
        setUnderlyingNavigableSet(descendingSet);
        // First poll should return 3 (the "lowest" according to the comparator)
        assertEquals(3, set.pollFirst(), "With descending comparator, pollFirst should return 3");
        // Second poll should return 2
        assertEquals(2, set.pollFirst(), "Second pollFirst should return 2");
    }

    /**
     * pollFirst - ClassCastException with incomparable elements **
     */
    @Test
    void pollFirst_IncomparableElements_ThrowsClassCastException() {
        // TreeSet with natural ordering but mixed types
        // raw to allow mixed types
        NavigableSet rawSet = new TreeSet();
        rawSet.add(1);
        // incomparable with Integer
        rawSet.add("string");
        setUnderlyingNavigableSet(rawSet);
        // The first call to pollFirst triggers a comparison and should throw
        assertThrows(ClassCastException.class, () -> set.pollFirst(), "pollFirst must propagate ClassCastException from underlying set");
    }

    /**
     * pollFirst - NullPointerException with null element in natural ordering **
     */
    @Test
    void pollFirst_NullElementInNaturalOrder_ThrowsNullPointerException() {
        // TreeSet with natural ordering; add null and a non‑null element
        NavigableSet<Integer> naturalSet = new TreeSet<>();
        naturalSet.add(null);
        naturalSet.add(1);
        setUnderlyingNavigableSet(naturalSet);
        // TreeSet will throw NPE when trying to compare null with 1
        assertThrows(NullPointerException.class, () -> set.pollFirst(), "pollFirst must propagate NullPointerException caused by null element");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("nonEmptySequenceProvider")
    void pollFirst_Parameterized_NonEmptySequence(List<Integer> initial, int[] expected, String description) {
        // Re‑initialize the set with the supplied initial data
        set.clear();
        initial.forEach(set::add);
        for (int exp : expected) {
            Integer result = set.pollFirst();
            assertEquals(exp, result);
        }
    }
}
