package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import java.util.Collections;
import java.util.List;
import static org.mockito.ArgumentMatchers.anyInt;

@ExtendWith(MockitoExtension.class)
class SealableList_listIterator_2_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        list.add(10);
        list.add(20);
        list.add(30);
    }

    @Test
    void testListIteratorStartsAtGivenIndex() {
        // start at index 1 (element 20)
        ListIterator<Integer> it = list.listIterator(1);
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(20), it.next());
        assertTrue(it.hasPrevious());
        assertEquals(Integer.valueOf(20), it.previous());
        // move forward again
        assertEquals(Integer.valueOf(20), it.next());
        assertEquals(Integer.valueOf(30), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testListIteratorAtBounds() {
        // index 0 – iterator at the beginning
        ListIterator<Integer> itStart = list.listIterator(0);
        assertTrue(itStart.hasNext());
        assertFalse(itStart.hasPrevious());
        // index size – iterator positioned after the last element
        ListIterator<Integer> itEnd = list.listIterator(list.size());
        assertFalse(itEnd.hasNext());
        assertTrue(itEnd.hasPrevious());
        assertEquals(Integer.valueOf(30), itEnd.previous());
        // negative index should throw
        assertThrows(IndexOutOfBoundsException.class, () -> list.listIterator(-1));
        // index greater than size should throw
        assertThrows(IndexOutOfBoundsException.class, () -> list.listIterator(list.size() + 1));
    }

    @Test
    void testListIteratorSetWhenSealed() {
        ListIterator<Integer> it = list.listIterator(0);
        // points to 10
        it.next();
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> it.set(99));
    }

    @Test
    void testListIteratorAddWhenSealed() {
        // between 10 and 20
        ListIterator<Integer> it = list.listIterator(1);
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> it.add(15));
    }

    @Test
    void testListIteratorRemoveWhenSealed() {
        ListIterator<Integer> it = list.listIterator(0);
        // points to 10
        it.next();
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void testListIteratorModificationWhenUnsealed() {
        // between 10 and 20
        ListIterator<Integer> it = list.listIterator(1);
        // add a new element 15
        it.add(15);
        assertEquals(4, list.size());
        assertEquals(Integer.valueOf(15), list.get(1));
        // move to the newly added element and replace it
        // now points to 15
        it.previous();
        it.set(16);
        assertEquals(Integer.valueOf(16), list.get(1));
        // remove it
        it.remove();
        assertEquals(3, list.size());
        assertEquals(Integer.valueOf(20), list.get(1));
    }

    @Test
    void testListIteratorNextThrowsNoSuchElementException() {
        // positioned after last
        ListIterator<Integer> it = list.listIterator(list.size());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void testListIteratorPreviousThrowsNoSuchElementException() {
        // positioned before first
        ListIterator<Integer> it = list.listIterator(0);
        assertFalse(it.hasPrevious());
        assertThrows(NoSuchElementException.class, it::previous);
    }

    /**
     * Parameterized test for valid indices inside the bounds of a non‑empty list.
     * Uses a {@code SealableList<String>} to demonstrate that the iterator works
     * with any generic type.
     */
    @ParameterizedTest(name = "listIterator({0}) on ['a','b','c'] returns element ''{1}''")
    @CsvSource({ "0, a", "2, c" })
    void testValidIndicesWithinBounds_StringList(int index, String expectedNext) {
        // Arrange: a list containing ["a","b","c"]
        List<String> backing = new ArrayList<>();
        Collections.addAll(backing, "a", "b", "c");
        SealableList<String> stringList = new SealableList<>(backing, sealedSupplier);
        // Act
        ListIterator<String> it = stringList.listIterator(index);
        // Assert: next() should return the element at the supplied index
        assertTrue(it.hasNext(), "Iterator should have a next element");
        assertEquals(expectedNext, it.next(), "next() should return the element at the requested index");
        // previousIndex() is defined by ListIterator contract: after next() it equals index
        assertEquals(index, it.previousIndex(), "previousIndex() after next() must equal the supplied index");
    }

    /**
     * Verify behaviour when the iterator is created at {@code index == size}.
     * The iterator should be positioned after the last element, have no next
     * element, and {@code previousIndex()} should be {@code size‑1}.
     */
    @Test
    void testIndexEqualToSize_StringList() {
        // Arrange
        List<String> backing = new ArrayList<>();
        Collections.addAll(backing, "a", "b", "c");
        SealableList<String> stringList = new SealableList<>(backing, sealedSupplier);
        int size = stringList.size();
        // Act
        ListIterator<String> it = stringList.listIterator(size);
        // Assert
        assertFalse(it.hasNext(), "Iterator at end should not have a next element");
        assertTrue(it.hasPrevious(), "Iterator at end should have a previous element");
        assertEquals(size - 1, it.previousIndex(), "previousIndex() should be size‑1 when positioned after the last element");
        assertEquals("c", it.previous(), "previous() should return the last element");
    }

    /**
     * Iterator on an empty underlying list.  The iterator should report no
     * next element and a {@code previousIndex()} of {@code -1}.
     */
    @Test
    void testEmptyListIterator() {
        // Arrange: an empty SealableList
        SealableList<String> emptyList = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // Act
        ListIterator<String> it = emptyList.listIterator(0);
        // Assert
        assertFalse(it.hasNext(), "Empty iterator must not have a next element");
        assertFalse(it.hasPrevious(), "Empty iterator must not have a previous element");
        assertEquals(-1, it.previousIndex(), "previousIndex() on an empty iterator should be -1");
    }

    /**
     * When the wrapped list's {@code listIterator(int)} throws an
     * {@link UnsupportedOperationException} (e.g., an unmodifiable list), the
     * {@code SealableList} must propagate that exception unchanged.
     */
    @Test
    void testUnderlyingListIteratorThrowsException() {
        // Arrange: mock a List that throws UnsupportedOperationException on listIterator()
        @SuppressWarnings("unchecked")
        List<String> mockList = Mockito.mock(List.class);
        Mockito.when(mockList.listIterator(anyInt())).thenThrow(new UnsupportedOperationException("Underlying list is unmodifiable"));
        SealableList<String> sealable = new SealableList<>(mockList, sealedSupplier);
        // Act & Assert
        assertThrows(UnsupportedOperationException.class, () -> sealable.listIterator(0), "SealableList should propagate UnsupportedOperationException from the underlying list");
    }

    /**
     * Defensive test: constructing a {@code SealableList} with a {@code null}
     * internal list and then invoking {@code listIterator(0)} should result in
     * a {@link NullPointerException}.  This mirrors the contract of the
     * underlying {@code List} implementation.
     */
    @Test
    void testNullUnderlyingListThrowsNPE() {
        // Arrange: deliberately pass a null backing list
        SealableList<String> nullBacking = new SealableList<>(null, sealedSupplier);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> nullBacking.listIterator(0), "Calling listIterator on a SealableList with a null backing list should throw NPE");
    }
}
