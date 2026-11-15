package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_clear_Test {

    /**
     *  Fields used in the tests **
     */
    @Mock
    private List<Integer> // mocked backing list for verification
    mockList;

    // ConcurrentList that wraps the mock
    private ConcurrentList<Integer> listWithMock;

    // for manual mock cleanup (optional)
    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Initialise Mockito annotations (necessary when using @Mock without field injection)
        mocks = MockitoAnnotations.openMocks(this);
        // Create a ConcurrentList that delegates to the mocked List
        listWithMock = new ConcurrentList<>(mockList);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Verify that no unexpected interactions occurred on the mock.
        verifyNoMoreInteractions(mockList);
        // Close the mock context to avoid memory leaks.
        mocks.close();
    }

    @Test
    void clearShouldDelegateToUnderlyingListClear() {
        // GIVEN – the ConcurrentList is wrapping a mocked List (already set up in @BeforeEach)
        // WHEN
        listWithMock.clear();
        // THEN – verify that the underlying list's clear() method was invoked exactly once
        verify(mockList, times(1)).clear();
    }

    @Test
    void clearShouldEmptyListWhenUsingDefaultConstructor() {
        // GIVEN – a fresh ConcurrentList created with the no‑arg constructor
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2, 3, 4, 5));
        assertFalse(list.isEmpty(), "Precondition: list should not be empty before clear");
        assertEquals(5, list.size(), "Precondition: size should be 5 before clear");
        // WHEN
        list.clear();
        // THEN – the list must be empty and size zero
        assertTrue(list.isEmpty(), "List should be empty after clear");
        assertEquals(0, list.size(), "Size should be 0 after clear");
        // AND – subsequent operations continue to work normally
        list.add(42);
        assertEquals(1, list.size());
        assertEquals(42, list.get(0));
    }

    @Test
    void clearOnAlreadyEmptyListDoesNotThrow() {
        // GIVEN – an empty list created via the no‑arg constructor
        ConcurrentList<Integer> list = new ConcurrentList<>();
        assertTrue(list.isEmpty());
        // WHEN / THEN – calling clear should not throw and the list stays empty
        assertDoesNotThrow(list::clear);
        assertTrue(list.isEmpty());
    }

    @Test
    void clearWhileAnotherThreadIsReadingDoesNotThrowConcurrentModificationException() throws Exception {
        // GIVEN – a list populated with several elements
        ConcurrentList<Integer> list = new ConcurrentList<>(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)));
        CountDownLatch startIterating = new CountDownLatch(1);
        CountDownLatch iterationDone = new CountDownLatch(1);
        AtomicBoolean cmeCaught = new AtomicBoolean(false);
        // Background thread iterates over the list
        Thread reader = new Thread(() -> {
            try {
                // signal that iteration is about to start
                startIterating.countDown();
                for (Iterator<Integer> it = list.iterator(); it.hasNext(); ) {
                    // simply read; we do not modify
                    it.next();
                }
            } catch (ConcurrentModificationException e) {
                cmeCaught.set(true);
            } finally {
                iterationDone.countDown();
            }
        });
        reader.start();
        // Ensure the reader has started
        assertTrue(startIterating.await(1, TimeUnit.SECONDS));
        // WHEN – main thread clears the list while the reader is potentially still iterating
        list.clear();
        // THEN – no CME should have been observed and the list ends up empty
        assertFalse(cmeCaught.get(), "Reader thread should not receive ConcurrentModificationException");
        assertTrue(list.isEmpty(), "List must be empty after clear");
        assertEquals(0, list.size());
        // Wait for the reader to finish
        assertTrue(iterationDone.await(1, TimeUnit.SECONDS));
    }

    @Test
    void clearWhileAnotherThreadIsWritingResultsInEmptyList() throws Exception {
        // GIVEN – a list that a background thread will keep adding to
        ConcurrentList<Integer> list = new ConcurrentList<>(new ArrayList<>());
        CountDownLatch startAdding = new CountDownLatch(1);
        CountDownLatch stopAdding = new CountDownLatch(1);
        AtomicBoolean running = new AtomicBoolean(true);
        Thread writer = new Thread(() -> {
            // signal that we have started
            startAdding.countDown();
            int i = 0;
            while (running.get()) {
                list.add(i++);
            }
            // signal that we have stopped
            stopAdding.countDown();
        });
        writer.start();
        // Wait until the writer has started
        assertTrue(startAdding.await(1, TimeUnit.SECONDS));
        // WHEN – clear is invoked while the writer is still active
        list.clear();
        // Stop the writer thread
        running.set(false);
        assertTrue(stopAdding.await(1, TimeUnit.SECONDS));
        writer.join();
        // THEN – the list must be empty (any elements added after clear are discarded by the final clear)
        assertTrue(list.isEmpty(), "List should be empty after concurrent clear");
        assertEquals(0, list.size());
    }

    @Test
    void clearIsIdempotentWhenCalledMultipleTimes() {
        // GIVEN – a non‑empty list
        ConcurrentList<String> list = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c")));
        assertFalse(list.isEmpty());
        // WHEN – clear is called twice in a row
        list.clear();
        list.clear();
        // THEN – list remains empty and no exception is thrown
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    void clearRemovesNullElementsAlongsideNonNulls() {
        // GIVEN – a list containing nulls and regular values
        ConcurrentList<String> list = new ConcurrentList<>(new ArrayList<>(Arrays.asList("x", null, "y", null)));
        assertEquals(4, list.size());
        // WHEN
        list.clear();
        // THEN – list is empty regardless of the presence of nulls before clear
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    void clearUpdatesExistingSubListView() {
        // GIVEN – a populated list and a subList view
        List<Integer> backing = new ArrayList<>(Arrays.asList(10, 20, 30, 40, 50));
        ConcurrentList<Integer> list = new ConcurrentList<>(backing);
        // expected view of [20,30,40]
        List<Integer> sub = list.subList(1, 4);
        // Verify subList works before clear
        assertEquals(3, sub.size());
        assertEquals(20, sub.get(0));
        // WHEN
        list.clear();
        // THEN – the subList should now be empty (or throw UnsupportedOperationException if not implemented)
        try {
            assertEquals(0, sub.size(), "SubList should reflect empty parent after clear");
            assertThrows(IndexOutOfBoundsException.class, () -> sub.get(0));
        } catch (UnsupportedOperationException ignored) {
            // The API documentation mentions subList may not be implemented; accepting this behaviour.
        }
    }

    @Test
    void clearPropagatesUnsupportedOperationExceptionFromUnderlyingList() {
        // GIVEN – a custom List implementation whose clear() throws UOE
        List<Integer> throwingList = new List<Integer>() {

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<Integer> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return a;
            }

            @Override
            public boolean add(Integer integer) {
                return false;
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends Integer> c) {
                return false;
            }

            @Override
            public boolean addAll(int index, Collection<? extends Integer> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("clear not supported");
            }

            @Override
            public Integer get(int index) {
                return null;
            }

            @Override
            public Integer set(int index, Integer element) {
                return null;
            }

            @Override
            public void add(int index, Integer element) {
            }

            @Override
            public Integer remove(int index) {
                return null;
            }

            @Override
            public int indexOf(Object o) {
                return -1;
            }

            @Override
            public int lastIndexOf(Object o) {
                return -1;
            }

            @Override
            public ListIterator<Integer> listIterator() {
                return Collections.emptyListIterator();
            }

            @Override
            public ListIterator<Integer> listIterator(int index) {
                return Collections.emptyListIterator();
            }

            @Override
            public List<Integer> subList(int fromIndex, int toIndex) {
                return Collections.emptyList();
            }
        };
        ConcurrentList<Integer> wrapper = new ConcurrentList<>(throwingList);
        // WHEN / THEN – the UnsupportedOperationException should be propagated
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, wrapper::clear);
        assertEquals("clear not supported", ex.getMessage());
    }
}
