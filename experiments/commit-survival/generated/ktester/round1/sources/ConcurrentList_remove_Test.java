package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static com.cedarsoftware.util.DeepEquals.deepEquals;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_remove_Test {

    /**
     * List instance used by the tests that need a fresh list for each test.
     */
    private ConcurrentList<Integer> list;

    /**
     * A backing list used to test the constructor that wraps an existing list.
     */
    private List<Integer> backingList;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for every test to guarantee isolation.
        list = new ConcurrentList<>();
        backingList = new ArrayList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps the GC in long test suites.
        list = null;
        backingList = null;
    }

    @Test
    void removeAtValidIndex_shouldReturnElementAndShift() {
        // given
        list.addAll(List.of(0, 1, 2, 3, 4));
        // when
        // removes element '2'
        int removed = list.remove(2);
        // then
        assertEquals(2, removed, "Removed element should be the one at the specified index");
        deepEquals(List.of(0, 1, 3, 4), list);
    }

    @Test
    void removeAtFirstAndLastIndex_shouldBehaveCorrectly() {
        // given
        list.addAll(List.of(10, 20, 30, 40));
        // when / then – first element
        assertEquals(10, list.remove(0), "First element should be removed");
        deepEquals(List.of(20, 30, 40), list);
        // when / then – last element (now at index 2)
        assertEquals(40, list.remove(2), "Last element should be removed");
        deepEquals(List.of(20, 30), list);
    }

    @Test
    void removeWithNegativeIndex_shouldThrowIndexOutOfBoundsException() {
        // given
        list.addAll(List.of(1, 2, 3));
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1));
    }

    @Test
    void removeWithIndexEqualToSize_shouldThrowIndexOutOfBoundsException() {
        // given
        list.addAll(List.of(5, 6, 7));
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(list.size()));
    }

    @Test
    void removeFromEmptyList_shouldThrowIndexOutOfBoundsException() {
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(0));
    }

    @Test
    void removeUsingWrappedBackingList_shouldReflectChangesInBackingList() {
        // given – create a ConcurrentList that wraps an existing list
        backingList.addAll(List.of(100, 200, 300));
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backingList);
        // when
        // removes 200
        int removed = wrapped.remove(1);
        // then
        assertEquals(200, removed);
        deepEquals(List.of(100, 300), wrapped);
        // The backing list must see the same change because it is the same instance.
        assertSame(backingList, wrapped);
        deepEquals(List.of(100, 300), backingList);
    }

    @Test
    void constructorWithNullList_shouldThrowIllegalArgumentException() {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> new ConcurrentList<>(null));
    }

    /**
     * Normal operation – remove from beginning, middle and end.
     * Parameterized to cover the three cases in a compact way.
     */
    @ParameterizedTest(name = "remove({0}) on list {1} should return {2} and result {3}")
    @CsvSource({ // index, initial list (as semicolon‑separated), expected removed element, expected list after removal
    "0, first;second;third, first, second;third", "1, first;second;third, second, first;third", "2, first;second;third, third, first;second" })
    void remove_normalOperation_parameterized(int index, String initial, String expectedRemoved, String expectedAfter) {
        // given
        List<String> initialList = new ArrayList<>(Arrays.asList(initial.split(";")));
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(initialList));
        // when
        String removed = cl.remove(index);
        // then
        assertEquals(expectedRemoved, removed, "Returned element must match expectation");
        List<String> expectedList = new ArrayList<>(Arrays.asList(expectedAfter.split(";")));
        deepEquals(expectedList, cl);
    }

    /**
     * Boundary conditions – removing from an empty list, negative index,
     * and index equal to size must all throw IndexOutOfBoundsException.
     */
    @Test
    void remove_boundaryConditions() {
        // empty list
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(0));
        // negative index
        list.addAll(List.of(1, 2, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(-1));
        // index == size
        assertThrows(IndexOutOfBoundsException.class, () -> list.remove(list.size()));
    }

    /**
     * Single‑element list – after removal the list should be empty.
     */
    @Test
    void remove_singleElementList() {
        // given
        list.add(42);
        // when
        int removed = list.remove(0);
        // then
        assertEquals(42, removed);
        assertTrue(list.isEmpty(), "List must be empty after removing its sole element");
        assertEquals(0, list.size());
    }

    /**
     * Handling of {@code null} elements – ensure they are removed correctly
     * and the method returns {@code null}.
     */
    @Test
    void remove_handlingNullElements_startingWithNull() {
        // given
        list.addAll(Arrays.asList(null, 1, 2));
        // when
        Integer removed = list.remove(0);
        // then
        assertNull(removed, "Removing a null element should return null");
        deepEquals(List.of(1, 2), list);
    }

    @Test
    void remove_handlingNullElements_endingWithNull() {
        // given
        list.addAll(Arrays.asList(10, 20, null));
        // when
        Integer removed = list.remove(2);
        // then
        assertNull(removed, "Removing a null element should return null");
        deepEquals(List.of(10, 20), list);
    }

    /**
     * Concurrency safety – while another thread iterates over the list,
     * a removal must not cause {@link ConcurrentModificationException}.
     */
    @Test
    void remove_concurrencySafety_noConcurrentModificationException() throws Exception {
        // given
        list.addAll(List.of(0, 1, 2, 3, 4));
        CountDownLatch iteratorReady = new CountDownLatch(1);
        CountDownLatch removalDone = new CountDownLatch(1);
        AtomicReference<Exception> iteratorException = new AtomicReference<>();
        // Thread that creates an iterator and walks through it
        Thread iterThread = new Thread(() -> {
            try {
                // read‑only snapshot
                Iterator<Integer> it = list.iterator();
                // signal that iterator is ready
                iteratorReady.countDown();
                while (it.hasNext()) {
                    // just consume; should never throw CME
                    it.next();
                }
            } catch (Exception e) {
                iteratorException.set(e);
            } finally {
                // ensure test finishes even if iterator ends early
                removalDone.countDown();
            }
        });
        iterThread.start();
        // Wait until iterator is obtained
        assertTrue(iteratorReady.await(1, TimeUnit.SECONDS), "Iterator thread did not start in time");
        // when – remove while iterator is active
        int removed = list.remove(0);
        assertEquals(0, removed);
        // then – iterator must not have thrown an exception
        assertTrue(removalDone.await(1, TimeUnit.SECONDS), "Iterator thread did not finish");
        assertNull(iteratorException.get(), "Iterator should not throw ConcurrentModificationException");
    }

    /**
     * Iterator consistency after removal – an iterator created *before* a removal
     * should continue without {@code ConcurrentModificationException} and reflect the
     * updated order (the iterator is a snapshot, so it sees the state at creation time).
     */
    @Test
    void remove_iteratorConsistencyAfterRemoval() throws Exception {
        // given
        list.addAll(List.of(10, 20, 30, 40));
        // snapshot at this moment
        Iterator<Integer> it = list.iterator();
        // consume first element
        assertTrue(it.hasNext());
        assertEquals(10, it.next());
        // remove element at index 1 (which is '20' in the original list)
        int removed = list.remove(1);
        assertEquals(20, removed);
        // iterator should continue without CME and see the *original* ordering (snapshot)
        assertTrue(it.hasNext());
        // because snapshot had 30 after 10
        assertEquals(30, it.next());
        assertTrue(it.hasNext());
        assertEquals(40, it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Unsupported operation – wrapping an unmodifiable list should cause
     * {@link UnsupportedOperationException} to be propagated from the underlying list.
     */
    @Test
    void remove_unsupportedOperation_unmodifiableBackingList() {
        // given
        List<Integer> unmodifiable = Collections.unmodifiableList(new ArrayList<>(List.of(1, 2, 3)));
        ConcurrentList<Integer> cl = new ConcurrentList<>(unmodifiable);
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> cl.remove(0));
    }
}
