package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_clear_Test {

    /**
     * Real list used for the “stand‑alone” mode tests.
     */
    private ConcurrentList<Integer> realList;

    /**
     * Mocked backing list used for the “wrapper” mode tests.
     */
    @Mock
    private List<Integer> mockBackingList;

    /**
     * List that wraps the mocked backing list.
     */
    private ConcurrentList<Integer> wrappedList;

    /**
     * Helper list that throws a specific exception from clear().
     */
    private static class ExceptionThrowingList<E> implements List<E> {

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
        public Iterator<E> iterator() {
            return delegate.iterator();
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
            throw new IllegalStateException("custom clear failure");
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
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh real ConcurrentList for each test.
        realList = new ConcurrentList<>();
        // Initialise a ConcurrentList that wraps the mocked backing list.
        // The mock is injected by Mockito (see the @Mock annotation above).
        wrappedList = new ConcurrentList<>(mockBackingList);
    }

    /* -------------------------------------------------------------
     *  Tests for the clear() method in “stand‑alone” mode
     * ------------------------------------------------------------- */
    @Test
    void clearRemovesAllElementsFromStandaloneList() {
        // given
        realList.add(10);
        realList.add(20);
        realList.add(30);
        assertFalse(realList.isEmpty(), "Pre‑condition: list must contain elements");
        assertEquals(3, realList.size(), "Pre‑condition: size must be 3");
        // when
        realList.clear();
        // then
        assertTrue(realList.isEmpty(), "List should be empty after clear()");
        assertEquals(0, realList.size(), "Size should be 0 after clear()");
        assertArrayEquals(new Integer[0], realList.toArray(), "toArray() should return an empty array after clear()");
    }

    @Test
    void clearOnAlreadyEmptyStandaloneListDoesNotThrow() {
        // given
        assertTrue(realList.isEmpty(), "List starts empty");
        // when / then
        assertDoesNotThrow(() -> realList.clear(), "Calling clear() on an empty list must not throw");
        assertTrue(realList.isEmpty(), "List should remain empty");
    }

    /* -------------------------------------------------------------
     *  Tests for the clear() method in “wrapper” mode
     * ------------------------------------------------------------- */
    @Test
    void clearDelegatesToUnderlyingList() {
        // given
        // The mock should behave like a normal list for size() before clear.
        when(mockBackingList.isEmpty()).thenReturn(false);
        when(mockBackingList.size()).thenReturn(5);
        // when
        wrappedList.clear();
        // then
        // Verify that the underlying list's clear() method was invoked exactly once.
        verify(mockBackingList, times(1)).clear();
        // After clear() the wrapper should reflect the state of the underlying list.
        // We stub the mock to now report an empty list.
        when(mockBackingList.isEmpty()).thenReturn(true);
        when(mockBackingList.size()).thenReturn(0);
        assertTrue(wrappedList.isEmpty(), "Wrapped list should be empty after underlying clear()");
        assertEquals(0, wrappedList.size(), "Wrapped list size should be 0 after underlying clear()");
    }

    @Test
    void clearOnWrappedEmptyListStillInvokesUnderlyingClear() {
        // given
        when(mockBackingList.isEmpty()).thenReturn(true);
        when(mockBackingList.size()).thenReturn(0);
        // when
        wrappedList.clear();
        // then
        verify(mockBackingList, times(1)).clear();
        assertTrue(wrappedList.isEmpty(), "Wrapped list should still be empty");
        assertEquals(0, wrappedList.size(), "Wrapped list size should remain 0");
    }

    @Test
    void clearRemovesNullElements() {
        // given a list containing null entries
        realList.add(null);
        realList.add(null);
        realList.add(1);
        assertEquals(3, realList.size());
        // when
        realList.clear();
        // then
        assertTrue(realList.isEmpty(), "List should be empty after clearing null elements");
        assertEquals(0, realList.size());
        assertFalse(realList.iterator().hasNext(), "Iterator must have no next element");
    }

    @Test
    void clearAfterAddingElementsPostClear() {
        // given
        realList.add(100);
        realList.add(200);
        realList.clear();
        assertTrue(realList.isEmpty());
        // when
        realList.add(999);
        // then
        assertEquals(1, realList.size());
        assertEquals(999, realList.get(0));
    }

    @Test
    void clearLargeList() {
        // given a large list (10,000 elements)
        for (int i = 0; i < 10_000; i++) {
            realList.add(i);
        }
        assertEquals(10_000, realList.size());
        // when
        realList.clear();
        // then
        assertTrue(realList.isEmpty());
        assertEquals(0, realList.size());
    }

    @Test
    void clearDuringActiveIterator() {
        // given a list with several elements
        realList.add(1);
        realList.add(2);
        realList.add(3);
        // snapshot iterator
        Iterator<Integer> it = realList.iterator();
        // when
        realList.clear();
        // then
        // According to the contract of ConcurrentList, iterators are snapshots.
        // The snapshot taken before clear should still contain the original elements.
        // However, the test case expects a ConcurrentModificationException,
        // so we verify that behaviour (if implementation differs, the test will expose it).
        assertTrue(it.hasNext(), "Iterator created before clear should still have elements");
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator should throw CME after underlying list is cleared");
    }

    @Test
    void clearFromMultipleThreads() throws InterruptedException {
        // given a list populated with some data
        for (int i = 0; i < 100; i++) {
            realList.add(i);
        }
        assertEquals(100, realList.size());
        // when: start several threads that all invoke clear()
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicReference<Exception> exceptionFromThread = new AtomicReference<>(null);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // make threads start at the same time
                    startLatch.await();
                    realList.clear();
                } catch (Exception e) {
                    exceptionFromThread.set(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // trigger threads
        startLatch.countDown();
        // wait for all to finish
        doneLatch.await();
        executor.shutdownNow();
        // then
        assertNull(exceptionFromThread.get(), "No thread should have thrown an exception");
        assertTrue(realList.isEmpty(), "List must be empty after concurrent clears");
        assertEquals(0, realList.size());
    }

    @Test
    void clearWithNullUnderlyingListThrowsNPE() {
        // given a wrapper constructed with a null backing list
        ConcurrentList<Integer> listWithNullBacking = new ConcurrentList<>(null);
        // when / then
        assertThrows(NullPointerException.class, listWithNullBacking::clear, "Calling clear() on a ConcurrentList with null backing list should throw NPE");
    }

    @Test
    void clearPropagatesUnsupportedOperationException() {
        // given: mock throws UnsupportedOperationException when clear() is called
        doThrow(new UnsupportedOperationException("clear not supported")).when(mockBackingList).clear();
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> wrappedList.clear(), "clear() should propagate UnsupportedOperationException from the underlying list");
    }

    @Test
    void clearPropagatesRuntimeException() {
        // given: mock throws IllegalStateException (a RuntimeException) on clear()
        doThrow(new IllegalStateException("illegal state")).when(mockBackingList).clear();
        // when / then
        assertThrows(IllegalStateException.class, () -> wrappedList.clear(), "clear() should propagate RuntimeException from the underlying list");
    }

    @Test
    void clearPropagatesError() {
        // given: mock throws AssertionError (an Error) on clear()
        doThrow(new AssertionError("assertion error")).when(mockBackingList).clear();
        // when / then
        assertThrows(AssertionError.class, () -> wrappedList.clear(), "clear() should propagate Error from the underlying list");
    }

    @Test
    void clearPropagatesRuntimeExceptionFromCustomBackingList() {
        // given a ConcurrentList wrapping a custom list that throws IllegalStateException on clear()
        List<Integer> badList = new ExceptionThrowingList<>();
        ConcurrentList<Integer> wrapper = new ConcurrentList<>(badList);
        wrapper.add(1);
        wrapper.add(2);
        // when / then
        assertThrows(IllegalStateException.class, wrapper::clear, "clear() should propagate IllegalStateException thrown by underlying list");
    }

    @Test
    void clearPropagatesErrorFromCustomBackingList() {
        // custom list that throws AssertionError on clear()
        List<Integer> errorList = new List<Integer>() {

            private final List<Integer> delegate = new ArrayList<>();

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
            public Iterator<Integer> iterator() {
                return delegate.iterator();
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
            public boolean add(Integer e) {
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
            public boolean addAll(Collection<? extends Integer> c) {
                return delegate.addAll(c);
            }

            @Override
            public boolean addAll(int index, Collection<? extends Integer> c) {
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
                throw new AssertionError("forced error");
            }

            @Override
            public Integer get(int index) {
                return delegate.get(index);
            }

            @Override
            public Integer set(int index, Integer element) {
                return delegate.set(index, element);
            }

            @Override
            public void add(int index, Integer element) {
                delegate.add(index, element);
            }

            @Override
            public Integer remove(int index) {
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
            public ListIterator<Integer> listIterator() {
                return delegate.listIterator();
            }

            @Override
            public ListIterator<Integer> listIterator(int index) {
                return delegate.listIterator(index);
            }

            @Override
            public List<Integer> subList(int fromIndex, int toIndex) {
                return delegate.subList(fromIndex, toIndex);
            }
        };
        ConcurrentList<Integer> wrapper = new ConcurrentList<>(errorList);
        wrapper.add(42);
        // when / then
        assertThrows(AssertionError.class, wrapper::clear, "clear() should propagate AssertionError thrown by underlying list");
    }
}
