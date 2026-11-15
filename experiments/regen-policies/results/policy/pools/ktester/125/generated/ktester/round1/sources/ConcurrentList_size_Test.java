package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_size_Test {

    /**
     * The list under test.
     */
    private List<Integer> list;

    /**
     * Runs before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor (stand‑alone mode) for a fresh list each test
        list = new ConcurrentList<>();
    }

    /**
     * Runs after each test method.
     */
    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; ConcurrentList does not hold external resources
        list = null;
    }

    /**
     * Verify that a newly created list reports size 0.
     */
    @Test
    void size_EmptyList_ReturnsZero() {
        assertTrue(list.isEmpty(), "New list should be empty");
        assertEquals(0, list.size(), "Size of an empty list must be 0");
    }

    /**
     * Verify size after a sequence of adds.
     */
    @Test
    void size_AfterAdds_ReturnsCorrectCount() {
        list.add(10);
        assertEquals(1, list.size(), "Size should be 1 after one add");
        list.add(20);
        list.add(30);
        assertEquals(3, list.size(), "Size should reflect three added elements");
    }

    /**
     * Verify size after removals.
     */
    @Test
    void size_AfterRemovals_ReturnsUpdatedCount() {
        list.addAll(List.of(1, 2, 3, 4, 5));
        assertEquals(5, list.size(), "Initial size should be 5");
        list.remove(Integer.valueOf(3));
        assertEquals(4, list.size(), "Size should decrement after removal");
        // remove element at index 0
        list.remove(0);
        assertEquals(3, list.size(), "Size should decrement after index removal");
    }

    /**
     * Verify size when the list wraps an existing collection.
     */
    @Test
    void size_WrapperMode_ReturnsUnderlyingListSize() {
        List<Integer> backing = new ArrayList<>(List.of(7, 8, 9));
        List<Integer> wrapped = new ConcurrentList<>(backing);
        assertEquals(3, wrapped.size(), "Wrapped list should report size of backing list");
        wrapped.add(10);
        assertEquals(4, wrapped.size(), "Size should grow after adding to wrapped list");
    }

    /**
     * Verify that size works correctly under concurrent modifications.
     */
    @Test
    void size_ConcurrentAdds_ReturnsExpectedTotal() throws InterruptedException {
        int threads = 8;
        int addsPerThread = 250;
        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            final int base = i * addsPerThread;
            workers[i] = new Thread(() -> {
                for (int j = 0; j < addsPerThread; j++) {
                    list.add(base + j);
                }
            });
            workers[i].start();
        }
        for (Thread t : workers) {
            t.join();
        }
        int expected = threads * addsPerThread;
        assertEquals(expected, list.size(), "Size should equal total number of concurrent adds");
    }

    /**
     * Verify that size reports zero after clear().
     */
    @Test
    void size_AfterClear_ReturnsZero() {
        list.addAll(List.of(1, 2, 3));
        assertEquals(3, list.size(), "Pre‑condition check");
        list.clear();
        assertEquals(0, list.size(), "Size must be zero after clear()");
        assertTrue(list.isEmpty(), "List should be empty after clear()");
    }

    /**
     * Basic functionality: newly created wrapper around an empty ArrayList.
     */
    @Test
    void size_WrapperEmptyArrayList_ReturnsZero() {
        List<Integer> emptyBacking = new ArrayList<>();
        List<Integer> wrapped = new ConcurrentList<>(emptyBacking);
        assertEquals(0, wrapped.size(), "A newly created ConcurrentList that wraps an empty ArrayList should report size() == 0.");
    }

    /**
     * Basic functionality: after adding a single element.
     */
    @Test
    void size_AfterSingleAdd_ReturnsOne() {
        list.add(42);
        assertEquals(1, list.size(), "After adding a single element via add(E) the list size should be 1.");
    }

    /**
     * Basic functionality: after adding five distinct elements.
     */
    @Test
    void size_AfterFiveAdds_ReturnsFive() {
        list.addAll(List.of(1, 2, 3, 4, 5));
        assertEquals(5, list.size(), "After adding five distinct elements the list size should be 5.");
    }

    /**
     * Basic functionality: after clear().
     */
    @Test
    void size_AfterClearOnPopulatedList_ReturnsZero() {
        list.addAll(List.of(9, 8, 7));
        list.clear();
        assertEquals(0, list.size(), "After calling clear() on a list that previously contained elements, size() should return 0.");
    }

    /**
     * Basic functionality: remove by index after three adds.
     */
    @Test
    void size_RemoveByIndex_ReturnsCorrectCount() {
        list.addAll(List.of(11, 22, 33));
        // remove the element at index 1
        list.remove(1);
        assertEquals(2, list.size(), "After adding three elements, removing one by index, and then calling size(), the result should be 2.");
    }

    /**
     * Basic functionality: wrapper around Collections.emptyList() (immutable).
     */
    @Test
    void size_WrapperImmutableEmptyList_ReturnsZero() {
        List<Integer> immutableEmpty = Collections.emptyList();
        List<Integer> wrapped = new ConcurrentList<>(immutableEmpty);
        assertEquals(0, wrapped.size(), "When the underlying list is a Collections.emptyList() (immutable empty list), size() should still return 0.");
    }

    /**
     * Basic functionality: wrapper around a synchronized list with two elements.
     */
    @Test
    void size_WrapperSynchronizedListWithTwoElements_ReturnsTwo() {
        List<Integer> syncList = Collections.synchronizedList(new ArrayList<>(List.of(5, 6)));
        List<Integer> wrapped = new ConcurrentList<>(syncList);
        assertEquals(2, wrapped.size(), "When the underlying list is a synchronizedList wrapping an ArrayList with two elements, size() should correctly return 2.");
    }

    /**
     * Exception scenario: constructing with a null underlying list should throw NPE.
     */
    @Test
    void size_NullUnderlyingList_ThrowsNullPointerException() {
        // The constructor that accepts a backing list is assumed to exist.
        ConcurrentList<Integer> cl = new ConcurrentList<>(null);
        assertThrows(NullPointerException.class, cl::size, "If the ConcurrentList is constructed with a null underlying list, calling size() should throw NullPointerException.");
    }

    /**
     * Exception scenario: underlying list throws ConcurrentModificationException on size().
     */
    @Test
    void size_UnderlyingListThrowsConcurrentModificationException() {
        @SuppressWarnings("unchecked")
        List<Integer> mocked = mock(List.class);
        when(mocked.size()).thenThrow(new ConcurrentModificationException());
        ConcurrentList<Integer> cl = new ConcurrentList<>(mocked);
        assertThrows(ConcurrentModificationException.class, cl::size, "If another thread modifies the underlying list during the execution of size(), the read lock may not protect against structural changes causing size() to throw ConcurrentModificationException.");
    }

    /**
     * Exception scenario: internal lock is null, causing IllegalStateException.
     */
    @Test
    void size_InternalLockNull_ThrowsIllegalStateException() throws Exception {
        // Create a normal instance first.
        ConcurrentList<Integer> cl = new ConcurrentList<>();
        // Use reflection to corrupt the internal lock (field name guessed as 'lock').
        // If the actual field name differs, adjust accordingly.
        Field lockField = null;
        for (Field f : ConcurrentList.class.getDeclaredFields()) {
            if (java.util.concurrent.locks.ReadWriteLock.class.isAssignableFrom(f.getType())) {
                lockField = f;
                break;
            }
        }
        assertNotNull(lockField, "Unable to locate internal ReadWriteLock field via reflection.");
        lockField.setAccessible(true);
        // force an illegal state
        lockField.set(cl, null);
        assertThrows(IllegalStateException.class, cl::size, "If the internal ReadWriteLock is in an invalid state (e.g., lock not initialized), invoking size() should result in IllegalStateException.");
    }
}
