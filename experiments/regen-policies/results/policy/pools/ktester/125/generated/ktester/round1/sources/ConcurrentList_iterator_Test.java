package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_iterator_Test {

    /**
     * The list under test.
     */
    private ConcurrentList<Integer> list;

    /**
     * The list under test.
     */
    private ConcurrentList<Integer> intList;

    /**
     * Helper list for String‑based tests.
     */
    private ConcurrentList<String> stringList;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for each test to guarantee isolation.
        intList = new ConcurrentList<>();
        stringList = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern symmetrical.
        list = null;
    }

    /**
     * Verify that {@code iterator()} returns a snapshot of the list at the moment
     * the iterator is created.  Subsequent modifications to the list must not be
     * reflected in the iterator.
     */
    @Test
    void iteratorProvidesSnapshot() {
        // given
        intList.add(1);
        intList.add(2);
        intList.add(3);
        // when
        // snapshot of [1,2,3]
        Iterator<Integer> it = intList.iterator();
        // consume first two elements
        assertTrue(it.hasNext());
        assertEquals(1, it.next());
        assertTrue(it.hasNext());
        assertEquals(2, it.next());
        // modify the underlying list after the iterator was obtained
        intList.add(4);
        intList.remove(Integer.valueOf(1));
        // then
        // iterator should still see the original third element (3) and then finish;
        // it must not see the newly added 4 nor the removal of 1.
        assertTrue(it.hasNext());
        assertEquals(3, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Verify that an iterator on an empty {@code ConcurrentList} behaves like a normal
     * empty iterator.
     */
    @Test
    void iteratorOnEmptyList() {
        // given – an empty list (already created in @BeforeEach)
        // when
        Iterator<Integer> it = list.iterator();
        // then
        assertFalse(it.hasNext());
        assertThrows(java.util.NoSuchElementException.class, it::next);
    }

    /**
     * The iterator returned is a snapshot; mutating the iterator must not affect the
     * original list.  The underlying {@code ArrayList} iterator supports {@code remove()},
     * so we can call it and then verify the original list is unchanged.
     */
    @Test
    void iteratorRemoveDoesNotAffectOriginalList() {
        // given
        intList.add(10);
        intList.add(20);
        intList.add(30);
        // when
        // snapshot of [10,20,30]
        Iterator<Integer> it = intList.iterator();
        // advance to the element we will remove via the iterator
        assertEquals(10, it.next());
        assertEquals(20, it.next());
        // remove the element 20 from the snapshot iterator
        // removes 20 from the snapshot only
        it.remove();
        // then
        // the original list must still contain all three elements
        assertEquals(3, intList.size());
        assertTrue(intList.contains(20), "Original list must still contain the element removed from the iterator");
        // the iterator should now continue with the next element (30)
        assertTrue(it.hasNext());
        assertEquals(30, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Verify that the iterator returned is read‑only with respect to structural changes
     * of the underlying list: adding elements after the iterator is created does not
     * appear during iteration.
     */
    @Test
    void iteratorIgnoresSubsequentAdds() {
        // given
        intList.addAll(List.of(5, 6, 7));
        // when
        // snapshot of [5,6,7]
        Iterator<Integer> it = intList.iterator();
        // modify the list after iterator creation
        intList.add(8);
        intList.add(9);
        // then
        // iterator should only iterate over the original three elements
        List<Integer> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertEquals(List.of(5, 6, 7), iterated, "Iterator must reflect the state of the list at creation time only");
        // Ensure the underlying list now contains the newly added elements
        assertEquals(5, intList.size());
        assertTrue(intList.containsAll(List.of(5, 6, 7, 8, 9)));
    }

    /**
     * Basic functionality: iterator over a populated list must return elements
     * in insertion order.
     */
    @Test
    void iteratorBasicFunctionalityPopulatedList() {
        // given
        stringList.add("a");
        stringList.add("b");
        stringList.add("c");
        // when
        Iterator<String> it = stringList.iterator();
        // then
        List<String> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }
        assertEquals(List.of("a", "b", "c"), result, "Iterator should return elements in insertion order");
    }

    /**
     * Snapshot semantics: elements added after iterator creation must not appear
     * in the iterator.
     */
    @Test
    void iteratorSnapshotIgnoresLaterAdditions() {
        // given
        stringList.add("first");
        // when
        // snapshot contains only "first"
        Iterator<String> it = stringList.iterator();
        stringList.add("second");
        stringList.add("third");
        // then
        assertTrue(it.hasNext());
        assertEquals("first", it.next());
        assertFalse(it.hasNext(), "Iterator must not see elements added after its creation");
    }

    /**
     * Snapshot semantics: elements removed after iterator creation must still be
     * returned by the iterator.
     */
    @Test
    void iteratorSnapshotIgnoresRemovals() {
        // given
        stringList.addAll(List.of("x", "y", "z"));
        // when
        // snapshot of [x,y,z]
        Iterator<String> it = stringList.iterator();
        // remove from underlying list
        stringList.remove("y");
        // then
        List<String> snapshot = new ArrayList<>();
        while (it.hasNext()) {
            snapshot.add(it.next());
        }
        assertEquals(List.of("x", "y", "z"), snapshot, "Iterator must retain removed element in its snapshot");
    }

    /**
     * Verify that calling {@code remove()} before any {@code next()} call throws
     * {@link IllegalStateException}, as required by the {@link Iterator} contract.
     */
    @Test
    void iteratorRemoveBeforeNextThrowsIllegalStateException() {
        // given
        stringList.addAll(List.of("one", "two"));
        Iterator<String> it = stringList.iterator();
        // when / then
        assertThrows(IllegalStateException.class, it::remove, "remove() before next() must throw IllegalStateException");
    }

    /**
     * Verify that calling {@code remove()} twice after a single {@code next()}
     * call throws {@link IllegalStateException}.
     */
    @Test
    void iteratorRemoveTwiceThrowsIllegalStateException() {
        // given
        stringList.addAll(List.of("alpha", "beta"));
        Iterator<String> it = stringList.iterator();
        // advance once
        assertEquals("alpha", it.next());
        // first remove succeeds (affects snapshot only)
        it.remove();
        // second remove should fail
        assertThrows(IllegalStateException.class, it::remove, "Second consecutive remove() must throw IllegalStateException");
    }

    /**
     * Null handling: iterator must correctly report and return {@code null}
     * elements.
     */
    @Test
    void iteratorHandlesNullElements() {
        // given
        stringList.add(null);
        // when
        Iterator<String> it = stringList.iterator();
        // then
        assertTrue(it.hasNext(), "Iterator should report hasNext() true for a single null element");
        assertNull(it.next(), "Iterator should return null for the stored null element");
        assertFalse(it.hasNext(), "Iterator should be exhausted after returning the null element");
    }

    /**
     * Verify that clearing the underlying list after obtaining an iterator does
     * not affect the iterator's snapshot (no {@link ConcurrentModificationException}
     * should be thrown).
     */
    @Test
    void iteratorSnapshotUnaffectedByClear() {
        // given
        stringList.addAll(List.of("keep", "also-keep"));
        // snapshot of two elements
        Iterator<String> it = stringList.iterator();
        // when: clear the underlying list
        stringList.clear();
        // then: iterator should still return the original elements
        List<String> snapshot = new ArrayList<>();
        while (it.hasNext()) {
            snapshot.add(it.next());
        }
        assertEquals(List.of("keep", "also-keep"), snapshot, "Iterator must still return elements from the snapshot after underlying list is cleared");
        // Also verify the underlying list is indeed empty
        assertTrue(stringList.isEmpty(), "Underlying list should be empty after clear()");
    }

    /**
     * Verify that calling {@code next()} on an empty iterator throws
     * {@link NoSuchElementException}.
     */
    @Test
    void iteratorNextOnEmptyThrowsNoSuchElementException() {
        // given an empty list
        Iterator<Integer> it = intList.iterator();
        // then
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
    }
}
