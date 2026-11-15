package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_isEmpty_Test {

    /**
     * Mocked backing list used to verify delegation of {@code isEmpty()}
     */
    @Mock
    private List<Integer> mockBackingList;

    /**
     * Instance of {@link ConcurrentList} that wraps the mocked list
     */
    private ConcurrentList<Integer> listWithMock;

    /**
     * Auto‑closeable resource for Mockito annotations
     */
    private AutoCloseable mocks;

    class ToggleList<E> implements List<E> {

        private final List<E> delegate = new ArrayList<>();

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        // Minimal required List methods for the test
        @Override
        public boolean add(E e) {
            return delegate.add(e);
        }

        @Override
        public int size() {
            return delegate.size();
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
    }

    class ToggleList<E> implements List<E> {

        private final List<E> delegate = new ArrayList<>();

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        // Minimal required List methods for the test
        @Override
        public boolean add(E e) {
            return delegate.add(e);
        }

        @Override
        public int size() {
            return delegate.size();
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
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise @Mock fields
        mocks = MockitoAnnotations.openMocks(this);
        // Create the ConcurrentList that delegates to the mocked backing list
        listWithMock = new ConcurrentList<>(mockBackingList);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release mock resources
        mocks.close();
    }

    @Test
    void isEmpty_delegatesToBackingList_whenEmpty() {
        // GIVEN: the backing list reports empty
        when(mockBackingList.isEmpty()).thenReturn(true);
        // WHEN: isEmpty() is called on the ConcurrentList
        boolean result = listWithMock.isEmpty();
        // THEN: result reflects the backing list and the backing list method is invoked exactly once
        assertTrue(result, "ConcurrentList should report empty when backing list is empty");
        verify(mockBackingList, times(1)).isEmpty();
    }

    @Test
    void isEmpty_delegatesToBackingList_whenNotEmpty() {
        // GIVEN: the backing list reports not empty
        when(mockBackingList.isEmpty()).thenReturn(false);
        // WHEN: isEmpty() is called on the ConcurrentList
        boolean result = listWithMock.isEmpty();
        // THEN: result reflects the backing list and the backing list method is invoked exactly once
        assertFalse(result, "ConcurrentList should report not empty when backing list is not empty");
        verify(mockBackingList, times(1)).isEmpty();
    }

    @Test
    void isEmpty_worksWithDefaultConstructor() {
        // GIVEN: a freshly constructed ConcurrentList using the no‑arg constructor
        ConcurrentList<Integer> list = new ConcurrentList<>();
        // THEN: it should be empty initially
        assertTrue(list.isEmpty(), "Newly created list should be empty");
        // WHEN: an element is added
        list.add(42);
        // THEN: it should no longer be empty
        assertFalse(list.isEmpty(), "List should not be empty after adding an element");
    }

    @Test
    void isEmpty_newlyCreatedListWrappingEmptyArrayList() {
        // A newly created ConcurrentList wrapping an empty ArrayList should be empty.
        ConcurrentList<Integer> list = new ConcurrentList<>(new ArrayList<>());
        assertTrue(list.isEmpty(), "List wrapping an empty ArrayList must report empty");
    }

    @Test
    void isEmpty_afterAddingOneElement() {
        // After adding one element, isEmpty() must return false.
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.add(1);
        assertFalse(list.isEmpty(), "List should not be empty after adding an element");
    }

    @Test
    void isEmpty_propagatesRuntimeExceptionFromBackingList() {
        // Configure mock to throw a RuntimeException when isEmpty() is called.
        when(mockBackingList.isEmpty()).thenThrow(new RuntimeException("underlying list failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> listWithMock.isEmpty(), "RuntimeException from backing list must propagate");
        assertEquals("underlying list failure", ex.getMessage());
    }

    @Test
    void isEmpty_afterClearReturnsTrue() {
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.add(10);
        list.add(20);
        list.clear();
        assertTrue(list.isEmpty(), "List should be empty after clear()");
    }

    @Test
    void isEmpty_afterRemoveSingleElementBecomesEmpty() {
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.add(99);
        assertFalse(list.isEmpty(), "List should not be empty after adding an element");
        list.remove(0);
        assertTrue(list.isEmpty(), "List should be empty after removing the sole element");
    }

    @Test
    void isEmpty_containsOnlyNullElementIsNotEmpty() {
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.add(null);
        assertFalse(list.isEmpty(), "List containing a null element must not be considered empty");
    }

    @Test
    void isEmpty_largeListIsNotEmpty() {
        ConcurrentList<Integer> list = new ConcurrentList<>();
        for (int i = 0; i < 10_000; i++) {
            list.add(i);
        }
        assertFalse(list.isEmpty(), "Large list (10,000 elements) must report not empty");
    }

    @Test
    void isEmpty_concurrentReadWhileWriteNeverThrows() throws Exception {
        // One thread continuously adds elements, another repeatedly calls isEmpty().
        ConcurrentList<Integer> list = new ConcurrentList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean stop = new AtomicBoolean(false);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());
        // Writer task
        Runnable writer = () -> {
            try {
                startLatch.await();
                int i = 0;
                while (!stop.get()) {
                    list.add(i++);
                }
            } catch (Throwable t) {
                errors.add(t);
            }
        };
        // Reader task
        Runnable reader = () -> {
            try {
                startLatch.await();
                // Keep checking until we see a non‑empty state
                while (!stop.get()) {
                    // should never throw
                    boolean empty = list.isEmpty();
                    if (!empty) {
                        // success condition
                        stop.set(true);
                    }
                }
            } catch (Throwable t) {
                errors.add(t);
            }
        };
        executor.submit(writer);
        executor.submit(reader);
        // Start both threads
        startLatch.countDown();
        // Wait up to 5 seconds for the condition to be met
        executor.shutdown();
        boolean terminated = executor.awaitTermination(5, TimeUnit.SECONDS);
        if (!terminated) {
            stop.set(true);
            executor.shutdownNow();
        }
        assertTrue(errors.isEmpty(), "No exception should be thrown during concurrent reads/writes: " + errors);
        // At this point the reader should have observed a non‑empty list
        assertFalse(list.isEmpty(), "After concurrent writes, list must not be empty");
    }

    @Test
    void isEmpty_underlyingListChangesStateAfterAdd() {
        // Custom list whose isEmpty() first returns true, then false after an element is added.
        class ToggleList<E> implements List<E> {

            private final List<E> delegate = new ArrayList<>();

            @Override
            public boolean isEmpty() {
                return delegate.isEmpty();
            }

            // Minimal required List methods for the test
            @Override
            public boolean add(E e) {
                return delegate.add(e);
            }

            @Override
            public int size() {
                return delegate.size();
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
        }
        ToggleList<Integer> toggle = new ToggleList<>();
        ConcurrentList<Integer> clist = new ConcurrentList<>(toggle);
        assertTrue(clist.isEmpty(), "Initially empty");
        clist.add(123);
        assertFalse(clist.isEmpty(), "After adding an element, should be non‑empty");
    }

    @Test
    void isEmpty_nullUnderlyingListThrowsNPE() throws Exception {
        // Construct a normal list then replace its internal backing list with null via reflection.
        ConcurrentList<Integer> list = new ConcurrentList<>(new ArrayList<>());
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        // force null internal list
        listField.set(list, null);
        NullPointerException npe = assertThrows(NullPointerException.class, list::isEmpty, "Calling isEmpty() with a null internal list must throw NPE");
        // Message is not defined; just ensure the exception type.
    }

    @Test
    void isEmpty_underlyingListThrowsIllegalStateException() {
        List<Integer> badList = mock(List.class);
        when(badList.isEmpty()).thenThrow(new IllegalStateException("illegal state in list"));
        ConcurrentList<Integer> clist = new ConcurrentList<>(badList);
        IllegalStateException ex = assertThrows(IllegalStateException.class, clist::isEmpty, "IllegalStateException must propagate unchanged");
        assertEquals("illegal state in list", ex.getMessage());
    }

    @Test
    void isEmpty_underlyingListThrowsAssertionError() {
        List<Integer> badList = mock(List.class);
        when(badList.isEmpty()).thenThrow(new AssertionError("assertion failure in list"));
        ConcurrentList<Integer> clist = new ConcurrentList<>(badList);
        AssertionError err = assertThrows(AssertionError.class, clist::isEmpty, "AssertionError must propagate unchanged");
        assertEquals("assertion failure in list", err.getMessage());
    }

    @Test
    void readOperation_supplierNullThrowsIllegalArgumentException() throws Exception {
        // Use reflection to invoke the private readOperation method with a null supplier.
        ConcurrentList<Integer> list = new ConcurrentList<>();
        Method readOp = ConcurrentList.class.getDeclaredMethod("readOperation", Supplier.class);
        readOp.setAccessible(true);
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> readOp.invoke(list, (Object) null), "Calling readOperation with null supplier must throw IllegalArgumentException");
        // The implementation may provide its own message; we only verify the type.
    }
}
