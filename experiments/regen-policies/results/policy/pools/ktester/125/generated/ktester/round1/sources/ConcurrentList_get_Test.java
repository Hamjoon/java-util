package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_get_Test {

    /* --------------------------------------------------------------------- *
     *  Supplier throws RuntimeException – custom backing list that throws
     * --------------------------------------------------------------------- */
    private static class ThrowingList<E> implements List<E> {

        @Override
        public E get(int index) {
            throw new RuntimeException("forced");
        }

        // The remaining List methods are not used in this test; they can throw
        // UnsupportedOperationException to keep the implementation minimal.
        @Override
        public int size() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean contains(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<E> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] toArray() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean add(E e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public E set(int index, E element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(int index, E element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public E remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int lastIndexOf(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListIterator<E> listIterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            throw new UnsupportedOperationException();
        }
    }

    /* --------------------------------------------------------------------- *
     *  Normal operation – parameterized test for index 0 and 2
     * --------------------------------------------------------------------- */
    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(Arguments.of(0, "firstElement"), Arguments.of(2, "thirdElement"));
    }

    /* --------------------------------------------------------------------- *
     *  Helper method for creating a ConcurrentList with predefined elements
     * --------------------------------------------------------------------- */
    private static <E> ConcurrentList<E> createList(E... elements) {
        ConcurrentList<E> list = new ConcurrentList<>();
        Collections.addAll(list, elements);
        return list;
    }

    @Test
    void get_ReturnsElementAtValidIndex_StandaloneMode() {
        // given
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.add(10);
        list.add(20);
        list.add(30);
        // when
        int value = list.get(1);
        // then
        assertEquals(20, value, "get(1) should return the element at index 1");
    }

    @Test
    void get_ReturnsElementAtValidIndex_WrapperMode() {
        // given
        List<Integer> backing = new ArrayList<>();
        backing.add(100);
        backing.add(200);
        backing.add(300);
        ConcurrentList<Integer> list = new ConcurrentList<>(backing);
        // when
        int value = list.get(2);
        // then
        assertEquals(300, value, "Wrapped list should return the correct element");
    }

    @Test
    void get_ThrowsIndexOutOfBoundsException_WhenIndexIsInvalid() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.add("a");
        list.add("b");
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1), "Negative index should throw IndexOutOfBoundsException");
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2), "Index equal to size should throw IndexOutOfBoundsException");
    }

    @Test
    void get_DelegatesToUnderlyingList_WhenWrapped(@Mock List<Integer> mockList) {
        // given
        when(mockList.get(0)).thenReturn(42);
        ConcurrentList<Integer> list = new ConcurrentList<>(mockList);
        // when
        int result = list.get(0);
        // then
        assertEquals(42, result, "Result should match the mocked underlying list value");
        verify(mockList, times(1)).get(0);
    }

    @Test
    void get_ReturnsCorrectValueAfterConcurrentAdds() throws InterruptedException, ExecutionException {
        // given
        ConcurrentList<Integer> list = new ConcurrentList<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        int adds = 200;
        // when: add elements concurrently
        Callable<Void> task = () -> {
            for (int i = 0; i < adds; i++) {
                list.add(i);
            }
            return null;
        };
        Future<Void> f1 = executor.submit(task);
        Future<Void> f2 = executor.submit(task);
        f1.get();
        f2.get();
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.MINUTES), "Executor should terminate within the timeout");
        // then: size should reflect all successful adds (duplicates are allowed)
        assertEquals(adds * 2, list.size(), "List size should equal the total number of added elements");
        // Spot‑check a few indices to ensure get works under concurrency
        assertNotNull(list.get(0), "First element should be present");
        assertNotNull(list.get(adds - 1), "Element at adds-1 should be present");
        assertNotNull(list.get(adds), "Element at adds should be present");
    }

    @ParameterizedTest(name = "[{index}] get({0}) should return {1}")
    @MethodSource("normalOperationProvider")
    void get_NormalOperation_ReturnsExpectedElement(int index, String expectedLabel) {
        // given – a list with three distinct elements
        ConcurrentList<String> list = new ConcurrentList<>();
        list.add("firstElement");
        list.add("secondElement");
        list.add("thirdElement");
        // when
        String result = list.get(index);
        // then
        assertEquals(expectedLabel, result, () -> String.format("get(%d) should return %s", index, expectedLabel));
    }

    /* --------------------------------------------------------------------- *
     *  Empty list – expect IndexOutOfBoundsException
     * --------------------------------------------------------------------- */
    @Test
    void get_EmptyList_ThrowsIndexOutOfBoundsException() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(0), "Calling get(0) on an empty list must throw IndexOutOfBoundsException");
    }

    /* --------------------------------------------------------------------- *
     *  Negative index – expect IndexOutOfBoundsException
     * --------------------------------------------------------------------- */
    @Test
    void get_NegativeIndex_ThrowsIndexOutOfBoundsException() {
        // given
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.add(1);
        list.add(2);
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1), "Negative index should throw IndexOutOfBoundsException");
    }

    /* --------------------------------------------------------------------- *
     *  Index equal to size – expect IndexOutOfBoundsException
     * --------------------------------------------------------------------- */
    @Test
    void get_IndexEqualToSize_ThrowsIndexOutOfBoundsException() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.add("a");
        list.add("b");
        // size == 3
        list.add("c");
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3), "Index equal to size should throw IndexOutOfBoundsException");
    }

    /* --------------------------------------------------------------------- *
     *  After removal – verify element shift
     * --------------------------------------------------------------------- */
    @Test
    void get_AfterRemoval_ReturnsElementShiftedLeft() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.add("firstElement");
        list.add("secondElement");
        list.add("thirdElement");
        // remove the element at index 1 ("secondElement")
        list.remove(1);
        // when
        String result = list.get(1);
        // then
        assertEquals("thirdElement", result, "After removing index 1, get(1) should return the original third element");
    }

    /* --------------------------------------------------------------------- *
     *  After addition to an initially empty list
     * --------------------------------------------------------------------- */
    @Test
    void get_AfterSingleAddition_ReturnsAddedElement() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.add("addedElement");
        // when
        String result = list.get(0);
        // then
        assertEquals("addedElement", result, "After adding a single element, get(0) should return that element");
    }

    /* --------------------------------------------------------------------- *
     *  Null element handling – list may contain nulls
     * --------------------------------------------------------------------- */
    @Test
    void get_NullElement_ReturnsNull() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.add("first");
        // index 1
        list.add(null);
        list.add("third");
        // when
        String result = list.get(1);
        // then
        assertNull(result, "When a null value is stored at index 1, get(1) must return null");
    }

    /* --------------------------------------------------------------------- *
     *  Null underlying list – simulate a faulty constructor via reflection
     * --------------------------------------------------------------------- */
    @Test
    void get_NullUnderlyingList_ThrowsNullPointerException() throws Exception {
        // given – construct a normal list then corrupt its internal field
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.add(1);
        // use reflection to set the private 'list' field to null
        Field field = ConcurrentList.class.getDeclaredField("list");
        field.setAccessible(true);
        field.set(list, null);
        // when / then
        assertThrows(NullPointerException.class, () -> list.get(0), "If the internal list reference is null, get(0) should throw NullPointerException");
    }

    @Test
    void get_SupplierThrowsRuntimeException_PropagatesException() {
        // given – a ConcurrentList that wraps a List whose get() throws RuntimeException
        List<Integer> throwing = new ThrowingList<>();
        ConcurrentList<Integer> list = new ConcurrentList<>(throwing);
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> list.get(0), "Exception thrown by the underlying list should propagate unchanged");
        assertEquals("forced", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  Concurrent read/write safety – reader repeatedly calls get while writer adds
     * --------------------------------------------------------------------- */
    @Test
    void get_ConcurrentReadWriteSafety_NoException() throws Exception {
        // given
        ConcurrentList<Integer> list = new ConcurrentList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch stopLatch = new CountDownLatch(1);
        // writer – adds elements continuously
        Runnable writer = () -> {
            try {
                // wait for the reader to be ready
                startLatch.await();
                for (int i = 0; i < 1000; i++) {
                    list.add(i);
                }
            } catch (InterruptedException ignored) {
            } finally {
                stopLatch.countDown();
            }
        };
        // reader – repeatedly calls get(0) (or catches IndexOutOfBounds if temporarily empty)
        Callable<Void> reader = () -> {
            // signal writer to start
            startLatch.countDown();
            while (stopLatch.getCount() > 0) {
                try {
                    list.get(0);
                } catch (IndexOutOfBoundsException | NullPointerException ignored) {
                    // acceptable when the list is momentarily empty or being cleared
                }
            }
            return null;
        };
        // when
        executor.submit(writer);
        Future<Void> readerFuture = executor.submit(reader);
        // wait for reader to finish
        readerFuture.get();
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS), "Executor should shut down cleanly");
        // then – no exception should have escaped the reader task
        // (assertions are implicit; if an exception propagated the test would fail)
    }

    /* --------------------------------------------------------------------- *
     *  Concurrent modification during read lock release – writer clears list
     * --------------------------------------------------------------------- */
    @Test
    void get_ConcurrentModificationDuringReadLock_NoException() throws Exception {
        // given
        ConcurrentList<Integer> list = new ConcurrentList<>();
        // ensure there is at least one element initially
        list.add(42);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);
        CountDownLatch done = new CountDownLatch(1);
        // writer – obtains write lock by calling add (which internally uses writeOperation)
        Runnable writer = () -> {
            try {
                // synchronize start with reader
                barrier.await();
                // clear the list while the reader may be holding a read lock
                list.clear();
            } catch (Exception e) {
                // ignore for the purpose of this test
            } finally {
                done.countDown();
            }
        };
        // reader – repeatedly calls get(0) under the read lock
        Callable<Void> reader = () -> {
            try {
                // synchronize start with writer
                barrier.await();
                // Perform a few reads; any IndexOutOfBoundsException is acceptable
                for (int i = 0; i < 10; i++) {
                    try {
                        list.get(0);
                    } catch (IndexOutOfBoundsException ignored) {
                    }
                }
            } finally {
                // nothing
            }
            return null;
        };
        // when
        Future<Void> readerFuture = executor.submit(reader);
        executor.submit(writer);
        // wait for reader
        readerFuture.get();
        // ensure writer finished
        done.await();
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS), "Executor should terminate cleanly");
        // then – the test passes if no unexpected exception (e.g., ConcurrentModificationException) escaped
    }
}
