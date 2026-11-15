package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class SealableList_iterator_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the sealed flag starts as false for every test
        sealedState = false;
        // Initialise the SealableList with a mutable backing list
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // Populate with a deterministic data set
        list.add(10);
        list.add(20);
        list.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental cross‑test contamination
        list = null;
    }

    @Test
    void iteratorIteratesAllElementsInOrder() {
        Iterator<Integer> it = list.iterator();
        assertTrue(it.hasNext());
        assertEquals(10, it.next());
        assertTrue(it.hasNext());
        assertEquals(20, it.next());
        assertTrue(it.hasNext());
        assertEquals(30, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void iteratorRemoveWorksWhenUnsealed() {
        Iterator<Integer> it = list.iterator();
        // Advance to the second element
        assertEquals(10, it.next());
        assertEquals(20, it.next());
        // Remove the element that was just returned (20)
        it.remove();
        // The list should now contain only 10 and 30
        assertEquals(2, list.size());
        assertEquals(List.of(10, 30), list);
    }

    @Test
    void iteratorRemoveThrowsWhenSealed() {
        Iterator<Integer> it = list.iterator();
        // Move to first element while list is mutable
        assertEquals(10, it.next());
        // Seal the list *after* the iterator has been obtained
        sealedState = true;
        // The iterator must now refuse removal
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void iteratorHasNextHonorsSealStateAfterCreation() {
        Iterator<Integer> it = list.iterator();
        // Seal the list before any iteration
        sealedState = true;
        // hasNext should still work because it does not modify the collection
        assertTrue(it.hasNext());
        assertEquals(10, it.next());
        // Subsequent calls continue to work
        assertTrue(it.hasNext());
        assertEquals(20, it.next());
        // Removal is still prohibited
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void iteratorNextThrowsNoSuchElementExceptionAtEndEvenWhenSealed() {
        Iterator<Integer> it = list.iterator();
        // Exhaust the iterator while mutable
        assertEquals(10, it.next());
        assertEquals(20, it.next());
        assertEquals(30, it.next());
        // Seal the list now
        sealedState = true;
        // The iterator should still throw NoSuchElementException when next() is called past the end
        assertThrows(java.util.NoSuchElementException.class, it::next);
    }

    @Test
    void iteratorReflectsChangesMadeBeforeSeal() {
        // Remove an element before sealing
        list.remove(Integer.valueOf(20));
        // Seal the list
        sealedState = true;
        Iterator<Integer> it = list.iterator();
        // The iterator should see the current state (10,30) and not the removed element
        assertTrue(it.hasNext());
        assertEquals(10, it.next());
        assertTrue(it.hasNext());
        assertEquals(30, it.next());
        assertFalse(it.hasNext());
        // Attempting to remove via iterator must still fail
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    /**
     * Basic iteration – empty list **
     */
    @Test
    @DisplayName("Iterator on empty SealableList reports no elements")
    void iteratorEmptyListHasNoElements() {
        // Arrange: clear the pre‑populated list
        list.clear();
        // Act
        Iterator<Integer> it = list.iterator();
        // Assert
        assertFalse(it.hasNext(), "hasNext() should be false for an empty list");
        assertThrows(NoSuchElementException.class, it::next, "next() must throw NoSuchElementException when the list is empty");
    }

    /**
     * Basic iteration – single element **
     */
    @Test
    @DisplayName("Iterator on single‑element SealableList returns that element then ends")
    void iteratorSingleElement() {
        list.clear();
        // single element
        list.add(42);
        Iterator<Integer> it = list.iterator();
        assertTrue(it.hasNext());
        assertEquals(42, it.next());
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    /**
     * Basic iteration – multiple elements **
     */
    @Test
    @DisplayName("Iterator on multi‑element SealableList preserves order")
    void iteratorMultipleElementsPreservesOrder() {
        list.clear();
        List<Integer> expected = List.of(1, 2, 3, 4);
        expected.forEach(list::add);
        Iterator<Integer> it = list.iterator();
        for (Integer value : expected) {
            assertTrue(it.hasNext(), "hasNext() should be true before reading " + value);
            assertEquals(value, it.next(), "Iterator should return elements in insertion order");
        }
        assertFalse(it.hasNext());
    }

    /**
     * NoSuchElementException on empty list **
     */
    @Test
    @DisplayName("Calling next() on iterator of empty list throws NoSuchElementException")
    void iteratorNextThrowsOnEmpty() {
        list.clear();
        Iterator<Integer> it = list.iterator();
        assertThrows(NoSuchElementException.class, it::next);
    }

    /**
     * IllegalStateException when remove() called before next() **
     */
    @Test
    @DisplayName("Calling remove() before any next() throws IllegalStateException")
    void iteratorRemoveBeforeNextThrows() {
        Iterator<Integer> it = list.iterator();
        assertThrows(IllegalStateException.class, it::remove, "remove() must not be allowed before next() according to Iterator contract");
    }

    /**
     * Remove works on mutable list with different data **
     */
    @Test
    @DisplayName("Iterator.remove() works on mutable list and updates underlying list")
    void iteratorRemoveWorksOnMutableList() {
        // Use a fresh list with distinct values
        list.clear();
        list.addAll(List.of(100, 200));
        Iterator<Integer> it = list.iterator();
        assertEquals(100, it.next());
        // should remove 100
        it.remove();
        assertEquals(1, list.size());
        assertEquals(List.of(200), list);
    }

    /**
     * Remove unsupported when sealed (alternative scenario) **
     */
    @Test
    @DisplayName("Iterator.remove() throws UnsupportedOperationException after sealing before iteration")
    void iteratorRemoveUnsupportedWhenSealedBeforeIteration() {
        // seal before iterator creation
        sealedState = true;
        Iterator<Integer> it = list.iterator();
        // next() is allowed
        assertEquals(10, it.next());
        // remove() must now be prohibited
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    /**
     * Fail‑fast on concurrent modification **
     */
    @Test
    @DisplayName("Iterator detects structural modification after creation and throws ConcurrentModificationException")
    void iteratorFailFastOnConcurrentModification() {
        Iterator<Integer> it = list.iterator();
        // Perform a structural change on the underlying list
        list.add(40);
        // The next call on the iterator should notice the change
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must be fail‑fast and detect concurrent modification");
    }

    /**
     * Iterator reflects current order after removal via iterator **
     */
    @Test
    @DisplayName("Iterator continues correctly after removing the current element")
    void iteratorReflectsOrderAfterRemoval() {
        list.clear();
        list.addAll(List.of(1, 2, 3));
        Iterator<Integer> it = list.iterator();
        assertEquals(1, it.next());
        // remove element 1
        it.remove();
        // The iterator should now yield 2 then 3 without gaps
        assertTrue(it.hasNext());
        assertEquals(2, it.next());
        assertTrue(it.hasNext());
        assertEquals(3, it.next());
        assertFalse(it.hasNext());
        // Underlying list should contain only 2 and 3
        assertEquals(List.of(2, 3), list);
    }

    /**
     * Multiple independent iterators **
     */
    @Test
    @DisplayName("Two iterators from the same list operate independently")
    void multipleIndependentIterators() {
        list.clear();
        list.addAll(List.of(7, 8, 9));
        Iterator<Integer> it1 = list.iterator();
        Iterator<Integer> it2 = list.iterator();
        // Advance first iterator once
        assertEquals(7, it1.next());
        // Second iterator should still start at the first element
        assertEquals(7, it2.next());
        assertEquals(8, it2.next());
        // Continue with first iterator – it should now be at 8
        assertEquals(8, it1.next());
        assertEquals(9, it1.next());
        assertFalse(it1.hasNext());
        // Second iterator should still have one element left (9)
        assertEquals(9, it2.next());
        assertFalse(it2.hasNext());
    }

    /**
     * NullPointerException when internal list is null **
     */
    @Test
    @DisplayName("Calling iterator() on a SealableList with a null backing list throws NullPointerException")
    void iteratorThrowsWhenInternalListIsNull() throws Exception {
        // Use reflection to set the private 'list' field to null
        Field listField = SealableList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(list, null);
        // The call should now throw NPE
        assertThrows(NullPointerException.class, list::iterator, "iterator() must throw NullPointerException when the internal list reference is null");
    }
}
