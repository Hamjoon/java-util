package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class SealableList_listIterator_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    private SealableList<String> stringList;

    @BeforeEach
    void setupBeforeEach() {
        // start each test un‑sealed
        sealedState = false;
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        list.add(10);
        list.add(20);
        list.add(30);
        // start each test un‑sealed
        sealedState = false;
        // For tests that need an empty list we will replace the backing list later.
        stringList = new SealableList<>(new ArrayList<>(), sealedSupplier);
    }

    /* --------------------------------------------------------------------- *
     *  Basic forward iteration – verifies order and NoSuchElementException *
     * --------------------------------------------------------------------- */
    @Test
    void testListIteratorBasicForward() {
        ListIterator<Integer> it = list.listIterator();
        assertTrue(it.hasNext());
        assertEquals(10, it.next());
        assertTrue(it.hasNext());
        assertEquals(20, it.next());
        assertTrue(it.hasNext());
        assertEquals(30, it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    /* --------------------------------------------------------------------- *
     *  Backward iteration after moving forward – verifies previous() logic *
     * --------------------------------------------------------------------- */
    @Test
    void testListIteratorBasicBackward() {
        ListIterator<Integer> it = list.listIterator();
        // move to the end
        while (it.hasNext()) {
            it.next();
        }
        assertTrue(it.hasPrevious());
        assertEquals(30, it.previous());
        assertEquals(20, it.previous());
        assertEquals(10, it.previous());
        assertFalse(it.hasPrevious());
        assertThrows(NoSuchElementException.class, it::previous);
    }

    /* --------------------------------------------------------------- *
     *  set() when the list is mutable – should replace the element *
     * --------------------------------------------------------------- */
    @Test
    void testListIteratorSetWhenUnsealed() {
        ListIterator<Integer> it = list.listIterator();
        // cursor after 10
        assertEquals(10, it.next());
        // replace 10 with 100
        it.set(100);
        assertEquals(100, list.get(0));
        assertEquals(20, list.get(1));
        assertEquals(30, list.get(2));
    }

    /* --------------------------------------------------------------- *
     *  add() when the list is mutable – inserts element at cursor *
     * --------------------------------------------------------------- */
    @Test
    void testListIteratorAddWhenUnsealed() {
        ListIterator<Integer> it = list.listIterator();
        // points after 10
        it.next();
        // insert 15 between 10 and 20
        it.add(15);
        assertEquals(4, list.size());
        assertEquals(Integer.valueOf(10), list.get(0));
        assertEquals(Integer.valueOf(15), list.get(1));
        assertEquals(Integer.valueOf(20), list.get(2));
        assertEquals(Integer.valueOf(30), list.get(3));
    }

    /* --------------------------------------------------------------- *
     *  remove() when the list is mutable – removes last returned *
     * --------------------------------------------------------------- */
    @Test
    void testListIteratorRemoveWhenUnsealed() {
        ListIterator<Integer> it = list.listIterator();
        assertEquals(10, it.next());
        // remove 10
        it.remove();
        assertEquals(2, list.size());
        assertEquals(Integer.valueOf(20), list.get(0));
        assertEquals(Integer.valueOf(30), list.get(1));
    }

    /* --------------------------------------------------------------- *
     *  set() when sealed – should throw UnsupportedOperationException *
     * --------------------------------------------------------------- */
    @Test
    void testListIteratorSetWhenSealed() {
        ListIterator<Integer> it = list.listIterator();
        // move to first element
        it.next();
        // seal the list
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> it.set(999));
    }

    /* --------------------------------------------------------------- *
     *  add() when sealed – should throw UnsupportedOperationException *
     * --------------------------------------------------------------- */
    @Test
    void testListIteratorAddWhenSealed() {
        ListIterator<Integer> it = list.listIterator();
        // advance cursor
        it.next();
        // seal the list
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> it.add(999));
    }

    /* --------------------------------------------------------------- *
     *  remove() when sealed – should throw UnsupportedOperationException *
     * --------------------------------------------------------------- */
    @Test
    void testListIteratorRemoveWhenSealed() {
        ListIterator<Integer> it = list.listIterator();
        // return first element
        it.next();
        // seal the list
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    /* --------------------------------------------------------------- *
     *  listIterator(int) – start at a specific index and verify order *
     * --------------------------------------------------------------- */
    @Test
    void testListIteratorWithIndex() {
        // should start at element 20
        ListIterator<Integer> it = list.listIterator(1);
        assertEquals(20, it.next());
        assertEquals(30, it.next());
        assertFalse(it.hasNext());
    }

    /* --------------------------------------------------------------- *
     *  Verify that nested sub‑list iterators also respect sealing *
     * --------------------------------------------------------------- */
    @Test
    void testNestedSubListIteratorHonorsSeal() {
        // full view
        List<Integer> sub = list.subList(0, list.size());
        ListIterator<Integer> it = sub.listIterator();
        // 10
        it.next();
        // insert via sub‑list iterator
        it.add(15);
        assertEquals(4, list.size());
        assertEquals(Integer.valueOf(15), list.get(1));
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> it.set(99));
        assertThrows(UnsupportedOperationException.class, () -> it.add(100));
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    /* --------------------------------------------------------------------- *
     *  Empty list iterator – basic contract verification                     *
     * --------------------------------------------------------------------- */
    @Test
    void testEmptyListIterator() {
        // The list is empty by default
        ListIterator<String> it = stringList.listIterator();
        assertNotNull(it, "listIterator() must never return null");
        assertFalse(it.hasNext(), "hasNext() must be false for an empty list");
        assertFalse(it.hasPrevious(), "hasPrevious() must be false for an empty list");
        assertThrows(NoSuchElementException.class, it::next, "next() on empty iterator should throw");
        assertThrows(NoSuchElementException.class, it::previous, "previous() on empty iterator should throw");
    }

    /* --------------------------------------------------------------------- *
     *  Verify nextIndex() and previousIndex() during forward iteration      *
     * --------------------------------------------------------------------- */
    @Test
    void testListIteratorNextPreviousIndexForward() {
        stringList.addAll(Arrays.asList("a", "b", "c"));
        ListIterator<String> it = stringList.listIterator();
        assertEquals(0, it.nextIndex(), "nextIndex() before any next() should be 0");
        assertEquals(-1, it.previousIndex(), "previousIndex() before any next() should be -1");
        assertEquals("a", it.next());
        assertEquals(1, it.nextIndex(), "nextIndex() after one next() should be 1");
        assertEquals(0, it.previousIndex(), "previousIndex() after one next() should be 0");
        assertEquals("b", it.next());
        assertEquals(2, it.nextIndex(), "nextIndex() after two next() should be 2");
        assertEquals(1, it.previousIndex(), "previousIndex() after two next() should be 1");
    }

    /* --------------------------------------------------------------------- *
     *  Verify nextIndex() and previousIndex() after moving to the end      *
     * --------------------------------------------------------------------- */
    @Test
    void testListIteratorNextPreviousIndexBackward() {
        stringList.addAll(Arrays.asList("a", "b", "c"));
        ListIterator<String> it = stringList.listIterator();
        // Move to the end
        while (it.hasNext()) {
            it.next();
        }
        assertEquals(3, it.nextIndex(), "nextIndex() at end should equal size");
        assertEquals(2, it.previousIndex(), "previousIndex() at end should be size‑1");
        assertEquals("c", it.previous());
        assertEquals(2, it.nextIndex(), "nextIndex() after one previous() should decrement");
        assertEquals(1, it.previousIndex(), "previousIndex() after one previous() should decrement");
    }

    /* --------------------------------------------------------------------- *
     *  listIterator(int) on an empty list (index 0)                         *
     * --------------------------------------------------------------------- */
    @Test
    void testListIteratorIntOnEmptyList() {
        ListIterator<String> it = stringList.listIterator(0);
        assertNotNull(it);
        assertFalse(it.hasNext());
        assertFalse(it.hasPrevious());
    }

    /* --------------------------------------------------------------------- *
     *  listIterator(int) start index equal to size (forward/backward)      *
     * --------------------------------------------------------------------- */
    @Test
    void testListIteratorIntStartAtSize() {
        stringList.addAll(Arrays.asList("a", "b", "c"));
        // after last element
        ListIterator<String> it = stringList.listIterator(3);
        assertFalse(it.hasNext(), "hasNext() must be false when positioned after last element");
        assertTrue(it.hasPrevious(), "hasPrevious() must be true when positioned after last element");
        assertEquals("c", it.previous(), "previous() should return the last element");
        assertEquals(2, it.nextIndex(), "nextIndex() after moving back once should be 2");
        assertEquals(1, it.previousIndex(), "previousIndex() after moving back once should be 1");
    }

    /* --------------------------------------------------------------------- *
     *  listIterator(int) middle index navigation (index = 1)              *
     * --------------------------------------------------------------------- */
    @Test
    void testListIteratorIntMiddleNavigation() {
        stringList.addAll(Arrays.asList("a", "b", "c"));
        // cursor between a and b
        ListIterator<String> it = stringList.listIterator(1);
        // forward
        assertEquals("b", it.next(), "next() should return element at index 1");
        assertEquals(2, it.nextIndex(), "nextIndex() after next() should be 2");
        assertEquals(0, it.previousIndex(), "previousIndex() after next() should be 0");
        // backward
        assertEquals("b", it.previous(), "previous() should return the same element");
        assertEquals(1, it.nextIndex(), "nextIndex() after previous() should be 1");
        assertEquals(-1, it.previousIndex(), "previousIndex() after previous() should be -1");
    }

    /* --------------------------------------------------------------------- *
     *  listIterator(int) negative index throws IndexOutOfBoundsException   *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @CsvSource({ "-1", "-5" })
    void testListIteratorIntNegativeIndexThrows(int index) {
        stringList.addAll(Arrays.asList("a", "b", "c"));
        assertThrows(IndexOutOfBoundsException.class, () -> stringList.listIterator(index), "listIterator(" + index + ") must throw IndexOutOfBoundsException");
    }

    /* --------------------------------------------------------------------- *
     *  listIterator(int) index greater than size throws IndexOutOfBoundsException *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @CsvSource({ "4", "10" })
    void testListIteratorIntGreaterThanSizeThrows(int index) {
        stringList.addAll(Arrays.asList("a", "b", "c"));
        assertThrows(IndexOutOfBoundsException.class, () -> stringList.listIterator(index), "listIterator(" + index + ") must throw IndexOutOfBoundsException");
    }

    /* --------------------------------------------------------------------- *
     *  Unsealed iterator remove before next – IllegalStateException        *
     * --------------------------------------------------------------------- */
    @Test
    void testIteratorRemoveBeforeNextThrowsIllegalState() {
        stringList.addAll(Arrays.asList("a", "b", "c"));
        ListIterator<String> it = stringList.listIterator();
        assertThrows(IllegalStateException.class, it::remove, "remove() before any next()/previous() must throw IllegalStateException");
    }

    /* --------------------------------------------------------------------- *
     *  Iterator respects seal after creation (retroactive sealing)       *
     * --------------------------------------------------------------------- */
    @Test
    void testIteratorRespectsSealAfterCreation() {
        stringList.addAll(Arrays.asList("a", "b", "c"));
        ListIterator<String> it = stringList.listIterator();
        // Perform a legal mutation before sealing
        assertEquals("a", it.next());
        // list becomes ["b","c"]
        it.remove();
        assertEquals(2, stringList.size());
        // Seal the list *after* the iterator has been obtained
        sealedState = true;
        // Subsequent mutating operations must now fail
        assertThrows(UnsupportedOperationException.class, () -> it.set("z"), "set() after sealing must throw UnsupportedOperationException");
        assertThrows(UnsupportedOperationException.class, () -> it.add("x"), "add() after sealing must throw UnsupportedOperationException");
        // remove() now illegal because the last element was already removed
        assertThrows(UnsupportedOperationException.class, it::remove, "remove() after sealing must throw UnsupportedOperationException");
    }

    /* --------------------------------------------------------------------- *
     *  Unsealed iterator remove operation – verifies removal semantics      *
     * --------------------------------------------------------------------- */
    @Test
    void testUnsealedIteratorRemoveOperation() {
        stringList.addAll(Arrays.asList("a", "b", "c"));
        ListIterator<String> it = stringList.listIterator();
        assertEquals("a", it.next());
        // remove "a"
        it.remove();
        assertEquals(2, stringList.size());
        assertEquals("b", stringList.get(0));
        assertEquals("c", stringList.get(1));
        // iterator should still be functional
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());
    }

    /* --------------------------------------------------------------------- *
     *  Unsealed iterator set operation – verifies element replacement       *
     * --------------------------------------------------------------------- */
    @Test
    void testUnsealedIteratorSetOperation() {
        stringList.addAll(Arrays.asList("a", "b", "c"));
        ListIterator<String> it = stringList.listIterator();
        assertEquals("a", it.next());
        // replace "a" with "z"
        it.set("z");
        assertEquals("z", stringList.get(0));
        assertEquals("b", stringList.get(1));
        assertEquals("c", stringList.get(2));
    }

    /* --------------------------------------------------------------------- *
     *  Unsealed iterator add operation – verifies insertion at cursor       *
     * --------------------------------------------------------------------- */
    @Test
    void testUnsealedIteratorAddOperation() {
        stringList.addAll(Arrays.asList("a", "b", "c"));
        ListIterator<String> it = stringList.listIterator();
        // cursor after "a"
        assertEquals("a", it.next());
        // insert "x" between "a" and "b"
        it.add("x");
        // List should now be ["a","x","b","c"]
        assertEquals(4, stringList.size());
        assertEquals("a", stringList.get(0));
        assertEquals("x", stringList.get(1));
        assertEquals("b", stringList.get(2));
        assertEquals("c", stringList.get(3));
        // iterator's next() should now return "b"
        assertEquals("b", it.next());
    }
}
