package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_listIterator_Test {

    private ConcurrentList<Integer> list;

    /**
     * Minimal List implementation whose {@link #iterator()} throws
     * {@link ConcurrentModificationException} immediately.
     */
    private static class ThrowingList<E> extends AbstractList<E> {

        private final List<E> delegate = new ArrayList<>();

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    throw new ConcurrentModificationException();
                }

                @Override
                public E next() {
                    throw new ConcurrentModificationException();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public E get(int index) {
            return delegate.get(index);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean add(E e) {
            return delegate.add(e);
        }
    }

    /**
     * Minimal List implementation whose {@link #iterator()} throws
     * {@link IllegalStateException} to simulate an unexpected runtime error.
     */
    private static class IllegalStateList<E> extends AbstractList<E> {

        private final List<E> delegate = new ArrayList<>();

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    throw new IllegalStateException("iterator failure");
                }

                @Override
                public E next() {
                    throw new IllegalStateException("iterator failure");
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public E get(int index) {
            return delegate.get(index);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean add(E e) {
            return delegate.add(e);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for every test to guarantee isolation.
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the list – not strictly required but makes intent clear.
        list.clear();
    }

    @Test
    void testListIteratorOnEmptyList() {
        ListIterator<Integer> it = list.listIterator();
        assertFalse(it.hasNext(), "Iterator on an empty list should have no next element");
        assertFalse(it.hasPrevious(), "Iterator on an empty list should have no previous element");
        assertThrows(java.util.NoSuchElementException.class, it::next, "Calling next() on empty iterator should throw");
        assertThrows(java.util.NoSuchElementException.class, it::previous, "Calling previous() on empty iterator should throw");
    }

    @Test
    void testListIteratorTraversalOrder() {
        // Populate the list.
        list.add(10);
        list.add(20);
        list.add(30);
        ListIterator<Integer> it = list.listIterator();
        // Forward traversal.
        assertTrue(it.hasNext());
        assertEquals(10, it.next());
        assertTrue(it.hasNext());
        assertEquals(20, it.next());
        assertTrue(it.hasNext());
        assertEquals(30, it.next());
        assertFalse(it.hasNext());
        // Backward traversal.
        assertTrue(it.hasPrevious());
        assertEquals(30, it.previous());
        assertTrue(it.hasPrevious());
        assertEquals(20, it.previous());
        assertTrue(it.hasPrevious());
        assertEquals(10, it.previous());
        assertFalse(it.hasPrevious());
        // Index checks (optional but useful).
        assertEquals(0, it.nextIndex());
        assertEquals(-1, it.previousIndex());
    }

    @Test
    void testListIteratorSnapshotIsImmutableToSubsequentModifications() {
        // Initial content.
        list.add(1);
        list.add(2);
        list.add(3);
        // Capture a snapshot iterator.
        ListIterator<Integer> snapshot = list.listIterator();
        // Modify the underlying list after the iterator has been created.
        list.add(4);
        list.remove(Integer.valueOf(2));
        // The snapshot must still reflect the original state (1,2,3).
        assertTrue(snapshot.hasNext());
        assertEquals(1, snapshot.next());
        assertTrue(snapshot.hasNext());
        assertEquals(2, snapshot.next());
        assertTrue(snapshot.hasNext());
        assertEquals(3, snapshot.next());
        assertFalse(snapshot.hasNext());
        // Verify the underlying list reflects the modifications.
        assertEquals(3, list.size());
        assertTrue(list.contains(1));
        assertFalse(list.contains(2));
        assertTrue(list.contains(3));
        assertTrue(list.contains(4));
    }

    @Test
    void testListIteratorDoesNotAllowRemovalFromUnderlyingList() {
        // Populate the list.
        list.add(5);
        list.add(6);
        ListIterator<Integer> it = list.listIterator();
        // The iterator is a snapshot; calling remove() should affect only the snapshot,
        // not the underlying ConcurrentList. According to the API contract the iterator
        // is read‑only, therefore we expect UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator returned by ConcurrentList should be read‑only");
    }

    @Test
    void testListIteratorPreviousAtStartThrowsException() {
        list.add(100);
        ListIterator<Integer> it = list.listIterator();
        // No call to next() yet – previous() must fail.
        assertThrows(NoSuchElementException.class, it::previous, "Calling previous() before any next() should throw NoSuchElementException");
    }

    @Test
    void testListIteratorNextAfterEndThrowsException() {
        list.add(1);
        list.add(2);
        ListIterator<Integer> it = list.listIterator();
        // 1
        it.next();
        // 2
        it.next();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next, "Calling next() after the last element should throw NoSuchElementException");
    }

    @Test
    void testListIteratorIndexReporting() {
        list.add(42);
        list.add(84);
        ListIterator<Integer> it = list.listIterator();
        // Initially
        assertEquals(0, it.nextIndex());
        assertEquals(-1, it.previousIndex());
        // After first next()
        assertEquals(42, it.next());
        assertEquals(1, it.nextIndex());
        assertEquals(0, it.previousIndex());
        // After second next()
        assertEquals(84, it.next());
        assertEquals(2, it.nextIndex());
        assertEquals(1, it.previousIndex());
        // Move back once
        assertEquals(84, it.previous());
        assertEquals(1, it.nextIndex());
        assertEquals(0, it.previousIndex());
    }

    @Test
    void testIteratorsAreIndependentSnapshots() {
        list.add(1);
        list.add(2);
        // sees [1,2]
        ListIterator<Integer> first = list.listIterator();
        // Modify underlying list
        list.add(3);
        // should see [1,2,3]
        ListIterator<Integer> second = list.listIterator();
        // Exhaust first iterator – should not see 3
        assertEquals(1, first.next());
        assertEquals(2, first.next());
        assertFalse(first.hasNext());
        // Second iterator must see the new element
        assertEquals(1, second.next());
        assertEquals(2, second.next());
        assertEquals(3, second.next());
        assertFalse(second.hasNext());
    }

    @Test
    void testIteratorSetDoesNotAffectUnderlyingList() {
        list.add(10);
        list.add(20);
        ListIterator<Integer> it = list.listIterator();
        // Move to first element and replace it via set()
        assertEquals(10, it.next());
        // mutates snapshot only
        it.set(99);
        // Snapshot reflects change
        // go back to see the replaced value
        assertEquals(99, it.previous());
        // Underlying list remains unchanged
        assertEquals(10, list.get(0));
        assertEquals(20, list.get(1));
    }

    @Test
    void testListIteratorAfterClearIsEmptyEvenIfOtherThreadAdds() throws Exception {
        // Fill list and then clear it.
        list.add(1);
        list.add(2);
        list.clear();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        AtomicBoolean iteratorSeenElements = new AtomicBoolean(false);
        // Thread that will add elements after the latch is released.
        Thread adder = new Thread(() -> {
            try {
                // wait until main thread has taken the snapshot
                startLatch.await();
                list.add(99);
                list.add(100);
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        adder.start();
        // Take snapshot while list is still empty.
        ListIterator<Integer> it = list.listIterator();
        // Release the other thread to add elements.
        startLatch.countDown();
        // Wait for the adder thread to finish.
        doneLatch.await();
        // The iterator must still be empty.
        assertFalse(it.hasNext());
        assertFalse(it.hasPrevious());
        assertThrows(NoSuchElementException.class, it::next);
        assertThrows(NoSuchElementException.class, it::previous);
    }

    @Test
    void testListIteratorThrowsNullPointerExceptionWhenInternalListIsNull() throws Exception {
        // Use reflection to set the private 'list' field to null.
        Field f = ConcurrentList.class.getDeclaredField("list");
        f.setAccessible(true);
        f.set(list, null);
        // The supplier inside listIterator() will attempt new ArrayList<>(null) -> NPE.
        assertThrows(NullPointerException.class, list::listIterator, "Calling listIterator() with internal list == null should propagate NullPointerException");
    }

    @Test
    void testListIteratorPropagatesConcurrentModificationExceptionFromSupplier() throws Exception {
        // Replace internal list with a custom list that throws CME during iteration.
        List<Integer> throwingList = new ThrowingList<>();
        throwingList.add(1);
        throwingList.add(2);
        Field f = ConcurrentList.class.getDeclaredField("list");
        f.setAccessible(true);
        f.set(list, throwingList);
        // Expect CME to bubble up.
        assertThrows(ConcurrentModificationException.class, list::listIterator, "listIterator() should propagate ConcurrentModificationException thrown by the supplier");
    }

    @Test
    void testListIteratorPropagatesUnexpectedRuntimeExceptionFromSupplier() throws Exception {
        // Replace internal list with a custom list whose iterator throws IllegalStateException.
        List<Integer> illegalStateList = new IllegalStateList<>();
        illegalStateList.add(1);
        illegalStateList.add(2);
        Field f = ConcurrentList.class.getDeclaredField("list");
        f.setAccessible(true);
        f.set(list, illegalStateList);
        // Expect the IllegalStateException to be propagated unchanged.
        IllegalStateException ex = assertThrows(IllegalStateException.class, list::listIterator, "listIterator() should propagate unexpected runtime exceptions from the supplier");
        assertEquals("iterator failure", ex.getMessage());
    }
}
