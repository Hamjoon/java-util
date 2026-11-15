package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.NoSuchElementException;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_listIterator_Test {

    private ConcurrentList<Integer> list;

    /**
     * Subclass that overrides {@code readOperation} to throw {@link IllegalStateException},
     * simulating a failure to acquire the read lock.
     */
    private static class ReadLockFailureConcurrentList<E> extends ConcurrentList<E> {

        @Override
        protected <T> T readOperation(java.util.function.Supplier<T> operation) {
            throw new IllegalStateException("Simulated lock acquisition failure");
        }
    }

    /**
     * Subclass that overrides {@code readOperation} to throw a generic {@link RuntimeException}.
     * This mimics a supplier throwing an unexpected exception.
     */
    private static class SupplierExceptionConcurrentList<E> extends ConcurrentList<E> {

        @Override
        protected <T> T readOperation(java.util.function.Supplier<T> operation) {
            throw new RuntimeException("Simulated supplier exception");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh list for each test
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly necessary
        list = null;
    }

    /**
     * Verify that the {@code listIterator()} method returns an iterator that iterates over a
     * snapshot of the list at the time of the call. Subsequent modifications to the underlying
     * {@link ConcurrentList} must not be reflected in the iterator.
     */
    @Test
    void testListIteratorIsSnapshot() {
        // given
        list.addAll(List.of(1, 2, 3, 4, 5));
        // when
        ListIterator<Integer> iterator = list.listIterator();
        // modify the original list after obtaining the iterator
        list.add(6);
        list.remove(Integer.valueOf(2));
        // then
        List<Integer> iterated = new ArrayList<>();
        while (iterator.hasNext()) {
            iterated.add(iterator.next());
        }
        // The iterator should contain the original elements only (1‑5)
        assertEquals(List.of(1, 2, 3, 4, 5), iterated, "Iterator must reflect the list state at the moment of creation, not later modifications");
        // The underlying list must reflect the modifications
        assertEquals(List.of(1, 3, 4, 5, 6), list, "Underlying list should reflect the add and remove operations performed after iterator creation");
    }

    /**
     * Verify normal forward and backward traversal of the iterator returned by {@code listIterator()}.
     */
    @Test
    void testListIteratorTraversal() {
        // given
        list.addAll(List.of(10, 20, 30));
        // when
        ListIterator<Integer> iterator = list.listIterator();
        // then – forward traversal
        assertTrue(iterator.hasNext());
        assertEquals(10, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(20, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(30, iterator.next());
        assertFalse(iterator.hasNext());
        // then – backward traversal
        assertTrue(iterator.hasPrevious());
        assertEquals(30, iterator.previous());
        assertTrue(iterator.hasPrevious());
        assertEquals(20, iterator.previous());
        assertTrue(iterator.hasPrevious());
        assertEquals(10, iterator.previous());
        assertFalse(iterator.hasPrevious());
    }

    /**
     * Verify that mutating operations on the snapshot iterator (e.g., {@code remove()})
     * do not affect the original {@link ConcurrentList}.
     */
    @Test
    void testIteratorRemoveDoesNotAffectOriginalList() {
        // given
        list.addAll(List.of(7, 8, 9));
        // when
        ListIterator<Integer> iterator = list.listIterator();
        assertEquals(7, iterator.next());
        // removes from the snapshot copy
        iterator.remove();
        // then
        // Original list should remain unchanged
        assertEquals(List.of(7, 8, 9), list, "Removing via the iterator must not modify the underlying ConcurrentList");
        // Snapshot iterator reflects the removal
        assertTrue(iterator.hasNext());
        assertEquals(8, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(9, iterator.next());
        assertFalse(iterator.hasNext());
    }

    /**
     * Verify that calling {@code listIterator()} on an empty list returns an iterator
     * with no elements.
     */
    @Test
    void testListIteratorOnEmptyList() {
        // given – an empty list (already created in setup)
        // when
        ListIterator<Integer> iterator = list.listIterator();
        // then
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasPrevious());
        assertThrows(IndexOutOfBoundsException.class, iterator::next, "Calling next() on an empty iterator should throw IndexOutOfBoundsException");
    }

    /**
     * Verify that a list containing {@code null} elements is iterated correctly.
     * The iterator must return {@code null} as a legitimate element without throwing.
     */
    @Test
    void testListIteratorHandlesNullElements() {
        // given
        list.addAll(List.of(1, null, 3));
        // when
        ListIterator<Integer> it = list.listIterator();
        // then – forward traversal should see the null value
        assertTrue(it.hasNext());
        assertEquals(1, it.next());
        assertTrue(it.hasNext());
        assertNull(it.next(), "Iterator should return null for a stored null element");
        assertTrue(it.hasNext());
        assertEquals(3, it.next());
        assertFalse(it.hasNext());
        // backward traversal should also see the null element
        assertTrue(it.hasPrevious());
        assertEquals(3, it.previous());
        assertTrue(it.hasPrevious());
        assertNull(it.previous(), "Iterator should return null when moving backwards over a null element");
        assertTrue(it.hasPrevious());
        assertEquals(1, it.previous());
        assertFalse(it.hasPrevious());
    }

    /**
     * Verify iterator state transitions for a list that contains exactly one element.
     */
    @Test
    void testListIteratorSingleElementBehavior() {
        // given
        list.add(42);
        // when
        ListIterator<Integer> it = list.listIterator();
        // then – initial state
        assertTrue(it.hasNext());
        assertFalse(it.hasPrevious());
        // forward
        assertEquals(42, it.next());
        assertFalse(it.hasNext());
        assertTrue(it.hasPrevious());
        // backward
        assertEquals(42, it.previous());
        assertTrue(it.hasNext());
        assertFalse(it.hasPrevious());
        // calling next again should succeed, previous again should succeed
        assertEquals(42, it.next());
        assertEquals(42, it.previous());
    }

    /**
     * After the iterator reaches the end of the collection, a further {@code next()}
     * call must throw {@link NoSuchElementException}.
     */
    @Test
    void testListIteratorNextThrowsNoSuchElementExceptionAtEnd() {
        // given
        list.addAll(List.of(10, 20));
        // when
        ListIterator<Integer> it = list.listIterator();
        // consume all elements
        assertEquals(10, it.next());
        assertEquals(20, it.next());
        // then – iterator is exhausted
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next, "Calling next() past the last element should throw NoSuchElementException");
    }

    /**
     * Immediately after creation, calling {@code previous()} must throw
     * {@link NoSuchElementException} because the iterator is positioned before the first element.
     */
    @Test
    void testListIteratorPreviousThrowsNoSuchElementExceptionAtStart() {
        // given
        list.addAll(List.of(5, 6, 7));
        // when
        ListIterator<Integer> it = list.listIterator();
        // then
        assertFalse(it.hasPrevious());
        assertThrows(NoSuchElementException.class, it::previous, "Calling previous() before any forward traversal should throw NoSuchElementException");
    }

    /**
     * Simulate a corrupted internal state where the backing {@code list} field is {@code null}.
     * The {@code listIterator()} method should propagate the resulting {@link NullPointerException}.
     */
    @Test
    void testListIteratorWithNullUnderlyingListReference() throws Exception {
        // given – a normal list with some data
        list.addAll(List.of(1, 2, 3));
        // use reflection to set the private 'list' field to null
        Field field = ConcurrentList.class.getDeclaredField("list");
        field.setAccessible(true);
        field.set(list, null);
        // when / then
        assertThrows(NullPointerException.class, list::listIterator, "If the internal list reference is null, listIterator() should throw NullPointerException");
    }

    /**
     * Verify that a failure inside {@code readOperation} (e.g., lock acquisition problem)
     * propagates as an {@link IllegalStateException} from {@code listIterator()}.
     */
    @Test
    void testListIteratorReadLockAcquisitionFailure() {
        // given
        ReadLockFailureConcurrentList<Integer> faultyList = new ReadLockFailureConcurrentList<>();
        // the list is not really used because readOperation always fails
        faultyList.add(1);
        // when / then
        assertThrows(IllegalStateException.class, faultyList::listIterator, "readOperation failure should propagate as IllegalStateException");
    }

    /**
     * Verify that a runtime exception thrown from the supplier inside {@code readOperation}
     * is propagated unchanged by {@code listIterator()}.
     */
    @Test
    void testListIteratorSupplierThrowsRuntimeException() {
        // given
        SupplierExceptionConcurrentList<Integer> faultyList = new SupplierExceptionConcurrentList<>();
        faultyList.add(1);
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, faultyList::listIterator, "Exception from supplier should propagate as RuntimeException");
        assertEquals("Simulated supplier exception", ex.getMessage());
    }

    /**
     * Verify that the iterator returned by {@code listIterator()} is a snapshot.
     * This test mirrors the original snapshot test but uses the same assertions
     * to ensure consistency across the suite.
     */
    @Test
    void testListIteratorIsSnapshot_Additional() {
        // given
        list.addAll(List.of(1, 2, 3, 4, 5));
        // when
        ListIterator<Integer> iterator = list.listIterator();
        // modify the original list after obtaining the iterator
        list.add(6);
        list.remove(Integer.valueOf(2));
        // then – iterator sees original snapshot
        List<Integer> iterated = new ArrayList<>();
        while (iterator.hasNext()) {
            iterated.add(iterator.next());
        }
        assertEquals(List.of(1, 2, 3, 4, 5), iterated, "Iterator must reflect the list state at the moment of creation, not later modifications");
        // underlying list reflects modifications
        assertEquals(List.of(1, 3, 4, 5, 6), list, "Underlying list should reflect the add and remove operations performed after iterator creation");
    }
}
