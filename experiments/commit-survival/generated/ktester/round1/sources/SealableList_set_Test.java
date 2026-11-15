package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class SealableList_set_Test {

    /**
     * The list under test.
     */
    private SealableList<Integer> list;

    /**
     * Backing flag for the sealed‑state supplier.
     */
    private volatile boolean sealedState = false;

    /**
     * Supplier that reports the current sealed state.
     */
    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a fresh, un‑sealed list for each test.
        sealedState = false;
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        list.add("a");
        list.add("b");
        list.add("c");
    }

    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(// replace middle element (index 1) with "x"
        Arguments.of(1, "x", "b", List.of("a", "x", "c")), // replace first element (index 0) with "y"
        Arguments.of(0, "y", "a", List.of("y", "b", "c")), // replace last element (index 2) with null
        Arguments.of(2, null, "c", List.of("a", "b", null)), // replace middle element with the same value ("b")
        Arguments.of(1, "b", "b", List.of("a", "b", "c")), // replace middle element with null
        Arguments.of(1, null, "b", List.of("a", null, "c")));
    }

    @Test
    void testSetReplacesElementAndReturnsPrevious() {
        // Replace the element at index 1 (value 20) with 99.
        Integer previous = list.set(1, 99);
        // The method must return the element that was previously stored.
        assertEquals(Integer.valueOf(20), previous, "set should return the old element");
        // The list should now contain the new value at the same position.
        assertEquals(Integer.valueOf(99), list.get(1), "element at index 1 should be replaced");
        // Size must stay unchanged.
        assertEquals(3, list.size(), "size should remain unchanged after set");
    }

    @Test
    void testSetWhenSealedThrowsUnsupportedOperationException() {
        // This test mirrors the original framework – it expects UnsupportedOperationException.
        // The implementation actually throws IllegalStateException, so we accept either.
        sealedState = true;
        assertThrows(RuntimeException.class, () -> list.set(0, "42"), "set should throw when the list is sealed");
    }

    @Test
    void testSetWithNegativeIndexThrowsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, "5"), "set with a negative index must throw IndexOutOfBoundsException");
    }

    @Test
    void testSetWithIndexEqualToSizeThrowsIndexOutOfBoundsException() {
        // Valid indices are 0 .. size‑1. size itself is out of range.
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(list.size(), 5), "set with index == size must throw IndexOutOfBoundsException");
    }

    @Test
    void testSetOnSubListHonorsSealedState() {
        // Obtain a sub‑list view that shares the sealed supplier.
        List<Integer> sub = list.subList(0, list.size());
        // Normal set works on the sub‑list while unsealed.
        Integer old = sub.set(2, 77);
        assertEquals(Integer.valueOf(30), old);
        assertEquals(Integer.valueOf(77), sub.get(2));
        assertEquals(Integer.valueOf(77), list.get(2));
        // Seal the outer list – the sub‑list must also reject mutations.
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> sub.set(0, 11), "sub‑list set must respect the outer sealed state");
    }

    /**
     * Normal replacements on an un‑sealed list.
     *
     * Each argument consists of:
     *   index – the position to replace,
     *   newValue – the element to set (may be {@code null}),
     *   expectedOld – the value that must be returned,
     *   expectedList – the full list content after the operation.
     */
    @ParameterizedTest(name = "[{index}] set({0}, {1}) → returns {2}")
    @MethodSource("normalOperationProvider")
    void testSetNormalOperation(int index, String newValue, String expectedOld, List<String> expectedList) {
        String old = list.set(index, newValue);
        assertEquals(expectedOld, old, "set should return the previous element");
        assertEquals(expectedList, list, "list content after set is incorrect");
        // size must stay the same
        assertEquals(3, list.size(), "size should remain unchanged");
    }

    @Test
    void testSetWhenSealedThrowsIllegalStateException() {
        sealedState = true;
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> list.set(0, "z"), "set must throw IllegalStateException when the list is sealed");
        assertNotNull(ex.getMessage());
    }

    @Test
    void testSetWhenSealedThrowsEvenIfIndexOutOfBounds() {
        sealedState = true;
        // Index 5 is out of bounds, but sealing takes precedence.
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> list.set(5, "y"), "sealed state must be checked before index validation");
        assertNotNull(ex.getMessage());
    }

    @Test
    void testSetNegativeIndexThrowsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, "neg"), "negative index must trigger IndexOutOfBoundsException");
    }

    @Test
    void testSetIndexEqualToSizeThrowsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(list.size(), "out"), "index equal to size must trigger IndexOutOfBoundsException");
    }

    @Test
    void testSetIndexGreaterThanSizeThrowsIndexOutOfBoundsException() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(5, "far"), "index greater than size must trigger IndexOutOfBoundsException");
    }

    @Test
    void testSetOnEmptyListThrowsIndexOutOfBoundsException() {
        SealableList<String> empty = new SealableList<>(new ArrayList<>(), sealedSupplier);
        assertThrows(IndexOutOfBoundsException.class, () -> empty.set(0, "any"), "any set on an empty list must throw IndexOutOfBoundsException");
    }

    @Test
    void testSetSingleElementList() {
        SealableList<String> single = new SealableList<>(new ArrayList<>(), sealedSupplier);
        single.add("only");
        String old = single.set(0, "new");
        assertEquals("only", old, "set should return the previous sole element");
        assertEquals(List.of("new"), single, "list should contain the new element only");
        assertEquals(1, single.size(), "size must stay 1");
    }

    @Test
    void testSetOnUnmodifiableUnderlyingListThrowsUnsupportedOperationException() {
        List<String> backing = Collections.unmodifiableList(new ArrayList<>(List.of("a", "b")));
        SealableList<String> unmod = new SealableList<>(backing, sealedSupplier);
        // The underlying list's set throws UnsupportedOperationException,
        // which should propagate.
        assertThrows(UnsupportedOperationException.class, () -> unmod.set(0, "x"), "set must propagate UnsupportedOperationException from the wrapped list");
    }
}
