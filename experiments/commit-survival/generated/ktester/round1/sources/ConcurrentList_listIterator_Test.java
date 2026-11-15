package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_listIterator_Test {

    /**
     * The list under test – a fresh instance for each test method.
     */
    private ConcurrentList<Integer> list;

    /**
     * Helper class that implements {@link List} but its {@code iterator()}
     * throws a supplied exception when invoked.
     */
    private static class ExceptionThrowingList<E> implements List<E> {

        private final RuntimeException toThrow;

        ExceptionThrowingList(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public Iterator<E> iterator() {
            throw toThrow;
        }

        // All other methods are simple delegations to an internal ArrayList – they are never used in the test.
        private final List<E> delegate = new ArrayList<>();

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        @Override
        public boolean add(E e) {
            return delegate.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return delegate.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return delegate.addAll(c);
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            return delegate.addAll(index, c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return delegate.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return delegate.retainAll(c);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public E get(int index) {
            return delegate.get(index);
        }

        @Override
        public E set(int index, E element) {
            return delegate.set(index, element);
        }

        @Override
        public void add(int index, E element) {
            delegate.add(index, element);
        }

        @Override
        public E remove(int index) {
            return delegate.remove(index);
        }

        @Override
        public int indexOf(Object o) {
            return delegate.indexOf(o);
        }

        @Override
        public int lastIndexOf(Object o) {
            return delegate.lastIndexOf(o);
        }

        @Override
        public ListIterator<E> listIterator() {
            return delegate.listIterator();
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            return delegate.listIterator(index);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return delegate.subList(fromIndex, toIndex);
        }

        @Override
        public boolean equals(Object o) {
            return delegate.equals(o);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a new, empty ConcurrentList before each test.
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the list to help the GC; not strictly required.
        list.clear();
    }

    /**
     * Utility method to replace the private {@code list} field of the test instance via reflection.
     */
    private void injectBackingList(List<Integer> replacement) throws Exception {
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(list, replacement);
    }

    @Test
    void listIteratorOnEmptyListShouldHaveNoElements() {
        // GIVEN – an empty ConcurrentList (created in @BeforeEach)
        // WHEN
        ListIterator<Integer> it = list.listIterator();
        // THEN
        assertFalse(it.hasNext(), "Iterator on an empty list must report no next element");
        assertFalse(it.hasPrevious(), "Iterator on an empty list must report no previous element");
        assertThrows(IndexOutOfBoundsException.class, it::next, "Calling next() on empty iterator should throw");
        assertThrows(IndexOutOfBoundsException.class, it::previous, "Calling previous() on empty iterator should throw");
    }

    @Test
    void listIteratorProvidesSnapshotOfCurrentState() {
        // GIVEN – a list with three elements
        list.add(1);
        list.add(2);
        list.add(3);
        // Obtain a snapshot iterator
        ListIterator<Integer> snapshotIt = list.listIterator();
        // Modify the underlying list after the iterator has been created
        list.add(4);
        list.remove(Integer.valueOf(2));
        // WHEN – we walk through the snapshot iterator
        List<Integer> observed = new ArrayList<>();
        while (snapshotIt.hasNext()) {
            observed.add(snapshotIt.next());
        }
        // THEN – the iterator reflects the state *at the time of creation* (1,2,3)
        assertEquals(List.of(1, 2, 3), observed, "Iterator must return a snapshot of the list as it was when listIterator() was called");
        // The current list now contains 1,3,4
        assertEquals(List.of(1, 3, 4), list, "The underlying list should reflect the modifications performed after iterator creation");
    }

    @Test
    void listIteratorTraversalMatchesListOrder() {
        // GIVEN – a list with known ordering
        List<Integer> source = List.of(10, 20, 30, 40);
        source.forEach(list::add);
        // WHEN – obtain iterator and traverse forward then backward
        ListIterator<Integer> it = list.listIterator();
        // forward traversal
        List<Integer> forward = new ArrayList<>();
        while (it.hasNext()) {
            forward.add(it.next());
        }
        // backward traversal (now at the end)
        List<Integer> backward = new ArrayList<>();
        while (it.hasPrevious()) {
            backward.add(it.previous());
        }
        // THEN – forward order must match original list
        assertEquals(source, forward, "Forward traversal should preserve list order");
        // backward order must be the reverse of forward traversal
        List<Integer> reversed = new ArrayList<>(forward);
        java.util.Collections.reverse(reversed);
        assertEquals(reversed, backward, "Backward traversal should be the reverse of forward traversal");
    }

    @Test
    void listIteratorModificationsDoNotAffectOriginalList() {
        // GIVEN – populate list
        list.addAll(List.of(5, 6, 7));
        // Obtain iterator
        ListIterator<Integer> it = list.listIterator();
        // Modify the snapshot via iterator (add, set, remove)
        // points to 5
        it.next();
        // change 5 -> 50 in snapshot
        it.set(50);
        // insert 55 after 50 in snapshot
        it.add(55);
        // points to original 6
        it.next();
        // remove 6 from snapshot
        it.remove();
        // Capture snapshot content
        List<Integer> snapshot = new ArrayList<>();
        // new iterator over a *new* snapshot (reflects current list)
        it = list.listIterator();
        while (it.hasNext()) {
            snapshot.add(it.next());
        }
        // THEN – original list must remain unchanged (5,6,7)
        assertEquals(List.of(5, 6, 7), list, "Operations on the iterator must not mutate the underlying ConcurrentList");
        // The snapshot we observed after the manipulations (via the same iterator) should reflect the changes
        // because the iterator works on a copy taken at the moment listIterator() was called.
        // The expected snapshot is: [50, 55, 6, 7] (5 became 50, 55 added, 6 still present because we removed it later)
        // However, note that after we called it.remove() it removed the element that was just returned (6).
        // So the snapshot should be [50, 55, 7].
        assertEquals(List.of(50, 55, 7), snapshot, "Changes made through the ListIterator affect only the snapshot, not the original list");
    }

    @Test
    void twoConsecutiveCallsReturnDistinctIterators() {
        // GIVEN
        list.addAll(List.of(1, 2, 3));
        // WHEN
        ListIterator<Integer> it1 = list.listIterator();
        ListIterator<Integer> it2 = list.listIterator();
        // THEN
        assertNotSame(it1, it2, "Each call to listIterator() must return a distinct ListIterator instance");
    }

    @Test
    void iteratorRemoveDoesNotAffectOriginalListSize() {
        // GIVEN
        list.addAll(List.of(10, 20, 30));
        // Obtain iterator and remove first element via iterator
        ListIterator<Integer> it = list.listIterator();
        // points to 10
        it.next();
        it.remove();
        // THEN
        assertEquals(3, list.size(), "Removing via the snapshot iterator must not change the original list size");
        assertEquals(List.of(10, 20, 30), list, "Original list content must stay unchanged");
    }

    @Test
    void iteratorReportsCorrectNextAndPreviousIndex() {
        // GIVEN
        list.addAll(List.of(7, 8, 9));
        ListIterator<Integer> it = list.listIterator();
        // Initial indices
        assertEquals(0, it.nextIndex(), "At start nextIndex() should be 0");
        assertEquals(-1, it.previousIndex(), "At start previousIndex() should be -1");
        // After one next()
        // returns 7
        it.next();
        assertEquals(1, it.nextIndex(), "After one next() nextIndex() should be 1");
        assertEquals(0, it.previousIndex(), "After one next() previousIndex() should be 0");
    }

    @Test
    void iteratorPreviousBeforeNextThrowsNoSuchElementException() {
        // GIVEN
        list.addAll(List.of(1, 2));
        ListIterator<Integer> it = list.listIterator();
        // THEN
        assertThrows(IndexOutOfBoundsException.class, it::previous, "Calling previous() before any next() must throw IndexOutOfBoundsException");
    }

    @Test
    void iteratorHasPreviousBehavior() {
        // GIVEN
        list.addAll(List.of(100, 200));
        ListIterator<Integer> it = list.listIterator();
        // Initially hasPrevious() is false
        assertFalse(it.hasPrevious(), "hasPrevious() must be false at the start");
        // After moving forward once
        it.next();
        assertTrue(it.hasPrevious(), "hasPrevious() must be true after a successful next()");
    }

    @Test
    void iteratorSetAffectsOnlySnapshot() {
        // GIVEN
        list.addAll(List.of(1, 2, 3));
        ListIterator<Integer> it = list.listIterator();
        // points to 1
        it.next();
        // modify snapshot
        it.set(99);
        // Capture snapshot via a fresh iterator
        List<Integer> snapshot = new ArrayList<>();
        ListIterator<Integer> fresh = list.listIterator();
        while (fresh.hasNext()) {
            snapshot.add(fresh.next());
        }
        // THEN
        assertEquals(List.of(1, 2, 3), list, "Original list must stay unchanged after iterator.set()");
        assertEquals(List.of(99, 2, 3), snapshot, "Snapshot must reflect the set() change");
    }

    @Test
    void iteratorIndicesAtEndOfList() {
        // GIVEN
        list.addAll(List.of(5, 6, 7));
        ListIterator<Integer> it = list.listIterator();
        // Move to the end
        while (it.hasNext()) {
            it.next();
        }
        // THEN
        assertEquals(3, it.nextIndex(), "At end nextIndex() must equal size");
        assertEquals(2, it.previousIndex(), "At end previousIndex() must be size‑1");
    }

    @Test
    void iteratorRemoveBeforeNextThrowsIllegalStateException() {
        // GIVEN
        list.addAll(List.of(42));
        ListIterator<Integer> it = list.listIterator();
        // THEN
        assertThrows(IllegalStateException.class, it::remove, "Calling remove() before next() or previous() must throw IllegalStateException");
    }

    @Test
    void twoIteratorsOperateIndependently() {
        // GIVEN
        list.addAll(List.of(1, 2, 3));
        ListIterator<Integer> it1 = list.listIterator();
        ListIterator<Integer> it2 = list.listIterator();
        // Advance first iterator once
        it1.next();
        // THEN
        assertEquals(1, it1.nextIndex(), "it1 should be at position 1 after one next()");
        assertEquals(0, it2.nextIndex(), "it2 must remain at start (position 0)");
    }

    @Test
    void iteratorHandlesNullElements() {
        // GIVEN
        List<Integer> source = new ArrayList<>(Arrays.asList(1, null, 3));
        source.forEach(list::add);
        ListIterator<Integer> it = list.listIterator();
        // WHEN
        List<Integer> observed = new ArrayList<>();
        while (it.hasNext()) {
            observed.add(it.next());
        }
        // THEN
        assertEquals(source, observed, "Iterator must return null entries unchanged");
    }

    @Test
    void largeListSnapshotTraversalDoesNotOverflow() {
        // GIVEN a relatively large list (10,000 elements)
        int size = 10_000;
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        // WHEN
        ListIterator<Integer> it = list.listIterator();
        // THEN – walk through the whole iterator without error
        int count = 0;
        while (it.hasNext()) {
            assertEquals(count, it.next(), "Element order must be preserved for large list");
            count++;
        }
        assertEquals(size, count, "Iterator must traverse exactly all elements");
    }

    @Test
    void iteratorCreationIsThreadSafeAgainstConcurrentModification() throws Exception {
        // GIVEN a list with a few elements
        list.addAll(List.of(1, 2, 3));
        // Latch to synchronize the concurrent add
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        AtomicBoolean exceptionOccurred = new AtomicBoolean(false);
        // Thread that adds an element while the snapshot is being created
        Thread modifier = new Thread(() -> {
            try {
                // wait until iterator creation starts
                startLatch.await();
                // concurrent modification
                list.add(99);
                doneLatch.countDown();
            } catch (Exception e) {
                exceptionOccurred.set(true);
            }
        });
        modifier.start();
        // WHEN – create iterator (the readOperation uses a lock, but we still test that no CME propagates)
        ListIterator<Integer> it = null;
        try {
            // allow modifier to run
            startLatch.countDown();
            // snapshot creation
            it = list.listIterator();
            // ensure modifier finished
            doneLatch.await();
        } catch (Exception e) {
            fail("listIterator() should not throw even if another thread modifies the list concurrently", e);
        }
        // THEN – iterator reflects the state before the concurrent add (i.e., 1,2,3)
        List<Integer> snapshot = new ArrayList<>();
        while (it.hasNext()) {
            snapshot.add(it.next());
        }
        assertEquals(List.of(1, 2, 3), snapshot, "Snapshot must represent the list state at the moment listIterator() was invoked");
        assertFalse(exceptionOccurred.get(), "No exception should have been recorded in the modifier thread");
    }

    @Test
    void listIteratorThrowsNullPointerExceptionWhenBackingListIsNull() throws Exception {
        // GIVEN – use reflection to set the private backing list to null
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(list, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> list.listIterator(), "If the internal backing list is null, listIterator() must throw NullPointerException");
    }

    @Test
    void listIteratorPropagatesConcurrentModificationExceptionFromBackingList() throws Exception {
        // GIVEN a backing list whose iterator throws CME
        ExceptionThrowingList<Integer> badList = new ExceptionThrowingList<>(new ConcurrentModificationException("boom"));
        injectBackingList(badList);
        // WHEN / THEN
        assertThrows(ConcurrentModificationException.class, () -> list.listIterator(), "If the backing list's iterator throws CME, listIterator() must propagate it");
    }

    @Test
    void listIteratorPropagatesIllegalStateExceptionFromBackingList() throws Exception {
        // GIVEN a backing list whose iterator throws IllegalStateException
        ExceptionThrowingList<Integer> badList = new ExceptionThrowingList<>(new IllegalStateException("illegal"));
        injectBackingList(badList);
        // WHEN / THEN
        assertThrows(IllegalStateException.class, () -> list.listIterator(), "If the backing list's iterator throws IllegalStateException, listIterator() must propagate it");
    }

    // ----------------------------------------------------------------------
    @Disabled("OutOfMemoryError test – enabling this will intentionally crash the JVM")
    @Test
    void listIteratorPropagatesOutOfMemoryErrorWhenCopyingHugeList() throws Exception {
        // This test would create a list with a size that cannot be allocated.
        // In practice we cannot allocate Integer.MAX_VALUE / 2 elements,
        // so we document the expectation and keep the test disabled.
        // Expected: OutOfMemoryError propagates from new ArrayList<>(list).
    }
}
