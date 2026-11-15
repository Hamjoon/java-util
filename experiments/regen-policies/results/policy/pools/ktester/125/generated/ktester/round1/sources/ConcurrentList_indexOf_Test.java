package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_indexOf_Test {

    private ConcurrentList<Integer> realList;

    @Mock
    private List<Integer> // mocked backing list for wrapper mode
    mockList;

    private ConcurrentList<Integer> mockWrappedList;

    /**
     * Minimal List implementation that throws a supplied runtime exception
     * from {@link #indexOf(Object)} while delegating all other methods to
     * an internal {@link ArrayList}. This is used to verify exception
     * propagation from the wrapped list.
     */
    private static class ExceptionThrowingList<E> implements List<E> {

        private final RuntimeException toThrow;

        private final List<E> delegate = new ArrayList<>();

        ExceptionThrowingList(Supplier<? extends RuntimeException> exceptionSupplier) {
            this.toThrow = exceptionSupplier.get();
        }

        @Override
        public int indexOf(Object o) {
            throw toThrow;
        }

        // -----------------------------------------------------------------
        // Boiler‑plate delegation for the remaining List methods.
        // Only the methods required by ConcurrentList during the test are
        // implemented; the rest delegate to the internal ArrayList.
        // -----------------------------------------------------------------
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
        // Initialise a fresh real ConcurrentList for each test
        realList = new ConcurrentList<>();
        // Initialise the wrapper instance that delegates to a mocked List
        // MockitoExtension already creates the mockList instance
        mockWrappedList = new ConcurrentList<>(mockList);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test tidy
        realList = null;
        mockWrappedList = null;
        // Mockito will reset the mock automatically between tests
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of(Arrays.asList("apple", "banana", "cherry"), "banana", 1), Arguments.of(Arrays.asList("apple", "banana", "cherry"), "durian", -1), Arguments.of(Arrays.asList(null, "apple", null), null, 0), Arguments.of(Collections.emptyList(), "any", -1));
    }

    private static Stream<Arguments> duplicateElementsProvider() {
        return Stream.of(Arguments.of(Arrays.asList("a", "a", "b", "a"), "a", 0), Arguments.of(Arrays.asList("x", "y", "x", "z"), "x", 0));
    }

    @Test
    void testIndexOfExistingElement() {
        realList.add(10);
        realList.add(20);
        realList.add(30);
        assertEquals(1, realList.indexOf(20), "indexOf should return the correct index for an existing element");
    }

    @Test
    void testIndexOfNonExistingElement() {
        realList.add(1);
        realList.add(2);
        realList.add(3);
        assertEquals(-1, realList.indexOf(99), "indexOf should return -1 when the element is not present");
    }

    @Test
    void testIndexOfNullElementWhenSupported() {
        realList.add(null);
        realList.add(5);
        assertEquals(0, realList.indexOf(null), "indexOf should correctly locate a null element if the backing list permits it");
    }

    @Test
    void testIndexOfDelegatesToWrappedList() {
        // Arrange: define behaviour of the mocked backing list
        when(mockList.indexOf("alpha")).thenReturn(42);
        // Act: call indexOf on the ConcurrentList wrapper
        int result = mockWrappedList.indexOf("alpha");
        // Assert: result matches the stubbed value and the mock was invoked exactly once
        assertEquals(42, result, "ConcurrentList should return the value supplied by the wrapped list");
        verify(mockList, times(1)).indexOf("alpha");
    }

    @Test
    void testIndexOfDelegatesWhenElementNotFound() {
        when(mockList.indexOf(100)).thenReturn(-1);
        int result = mockWrappedList.indexOf(100);
        assertEquals(-1, result, "ConcurrentList should propagate -1 when the wrapped list does not contain the element");
        verify(mockList, times(1)).indexOf(100);
    }

    @Test
    void testIndexOfWithNullInWrappedList() {
        when(mockList.indexOf(null)).thenReturn(0);
        int result = mockWrappedList.indexOf(null);
        assertEquals(0, result, "ConcurrentList should correctly delegate a null search to the wrapped list");
        verify(mockList, times(1)).indexOf(null);
    }

    /**
     * Parameterised tests for the basic functionality group described in the JSON.
     */
    @ParameterizedTest(name = "{index} => list={0}, search={1}, expected={2}")
    @MethodSource("basicFunctionalityProvider")
    void testIndexOfBasicFunctionality(List<String> listElements, Object searchObject, int expected) {
        ConcurrentList<String> list = new ConcurrentList<>();
        // Populate the list (null values are allowed)
        for (String e : listElements) {
            list.add(e);
        }
        assertEquals(expected, list.indexOf(searchObject), "Unexpected index returned for searchObject=" + searchObject);
    }

    /**
     * Parameterised tests for duplicate‑element scenarios.
     */
    @ParameterizedTest(name = "{index} => list={0}, search={1}, expected={2}")
    @MethodSource("duplicateElementsProvider")
    void testIndexOfDuplicateElements(List<String> listElements, Object searchObject, int expected) {
        ConcurrentList<String> list = new ConcurrentList<>();
        listElements.forEach(list::add);
        assertEquals(expected, list.indexOf(searchObject), "indexOf should return the first occurrence index");
    }

    @Test
    void testIndexOfWithNullUnderlyingList() {
        // Passing null to the wrapper constructor results in an internal list reference of null.
        ConcurrentList<String> listWithNullBacking = new ConcurrentList<>(null);
        assertThrows(NullPointerException.class, () -> listWithNullBacking.indexOf("any"), "Calling indexOf when the internal list is null should throw NullPointerException");
    }

    @Test
    void testIndexOfUnderlyingListThrowsUnsupportedOperationException() {
        List<String> throwingList = new ExceptionThrowingList<>(UnsupportedOperationException::new);
        ConcurrentList<String> wrapper = new ConcurrentList<>(throwingList);
        assertThrows(UnsupportedOperationException.class, () -> wrapper.indexOf("test"), "UnsupportedOperationException from the backing list must propagate");
    }

    @Test
    void testIndexOfUnderlyingListThrowsConcurrentModificationException() {
        List<String> throwingList = new ExceptionThrowingList<>(ConcurrentModificationException::new);
        ConcurrentList<String> wrapper = new ConcurrentList<>(throwingList);
        assertThrows(ConcurrentModificationException.class, () -> wrapper.indexOf("test"), "ConcurrentModificationException from the backing list must propagate");
    }
}
