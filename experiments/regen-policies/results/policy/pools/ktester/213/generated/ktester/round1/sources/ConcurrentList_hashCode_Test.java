package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_hashCode_Test {

    /**
     * Real list used for integration‑style verification.
     */
    private ConcurrentList<Integer> realConcurrentList;

    /**
     * Mocked backing list used to verify delegation.
     */
    @Mock
    private List<Integer> mockBackingList;

    /**
     * Instance under test that wraps the {@code mockBackingList}.
     */
    private ConcurrentList<Integer> mockConcurrentList;

    class CustomObj {

        private final String name;

        private final int hash;

        CustomObj(String name, int hash) {
            this.name = name;
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "CustomObj{name='" + name + '\'' + ", hash=" + hash + '}';
        }
    }

    /**
     * Custom element whose {@code hashCode()} deliberately throws an {@link IllegalStateException}.
     */
    static class ExceptionHashObject {

        @Override
        public int hashCode() {
            throw new IllegalStateException("Deliberate exception from element");
        }
    }

    /**
     * A List implementation whose {@code hashCode()} throws {@link ConcurrentModificationException}.
     * All other methods are stubbed minimally for compilation.
     */
    static class ThrowingList<E> implements List<E> {

        @Override
        public int hashCode() {
            throw new ConcurrentModificationException("Hash code not allowed");
        }

        // ---- Minimal stubs -------------------------------------------------
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
        public Iterator<E> iterator() {
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
        public boolean add(E e) {
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
        public boolean addAll(Collection<? extends E> c) {
            return false;
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
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
        }

        @Override
        public E get(int index) {
            return null;
        }

        @Override
        public E set(int index, E element) {
            return null;
        }

        @Override
        public void add(int index, E element) {
        }

        @Override
        public E remove(int index) {
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
        public ListIterator<E> listIterator() {
            return Collections.emptyListIterator();
        }

        @Override
        public ListIterator<E> listIterator(int index) {
            return Collections.emptyListIterator();
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return Collections.emptyList();
        }
        // --------------------------------------------------------------------
    }

    class CustomObj {

        private final String name;

        private final int hash;

        CustomObj(String name, int hash) {
            this.name = name;
            this.hash = hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return "CustomObj{name='" + name + '\'' + ", hash=" + hash + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Real list – start empty for each test.
        realConcurrentList = new ConcurrentList<>();
        // Mock‑backed list – reset interactions before each test.
        reset(mockBackingList);
        mockConcurrentList = new ConcurrentList<>(mockBackingList);
    }

    /**
     * Provides a stream of arguments where each entry contains:
     * <ul>
     *   <li>a list of initial elements to insert into a fresh {@link ConcurrentList}</li>
     *   <li>the expected hash code computed by a regular {@link ArrayList}</li>
     * </ul>
     * Elements may be integers, strings, booleans, {@code null},
     * custom objects with overridden {@code hashCode()}, or a mixture thereof.
     */
    static Stream<Arguments> hashCodeScenarios() {
        // Helper for custom objects with a predefined hash value
        class CustomObj {

            private final String name;

            private final int hash;

            CustomObj(String name, int hash) {
                this.name = name;
                this.hash = hash;
            }

            @Override
            public int hashCode() {
                return hash;
            }

            @Override
            public String toString() {
                return "CustomObj{name='" + name + '\'' + ", hash=" + hash + '}';
            }
        }
        return Stream.of(// empty list
        Arguments.of(List.of(), new ArrayList<>().hashCode()), // [1,2,3]
        Arguments.of(List.of(1, 2, 3), Arrays.asList(1, 2, 3).hashCode()), // [null, "test"]
        Arguments.of(Arrays.asList(null, "test"), Arrays.asList(null, "test").hashCode()), // two custom objects with same hash (42)
        Arguments.of(List.of(new CustomObj("A", 42), new CustomObj("B", 42)), List.of(new CustomObj("A", 42), new CustomObj("B", 42)).hashCode()), // [true, false, true]
        Arguments.of(List.of(true, false, true), Arrays.asList(true, false, true).hashCode()), // duplicate strings
        Arguments.of(List.of("duplicate", "duplicate"), Arrays.asList("duplicate", "duplicate").hashCode()), // singleton list [42]
        Arguments.of(List.of(42), Collections.singletonList(42).hashCode()), // negative integers
        Arguments.of(List.of(-1, -2, -3), Arrays.asList(-1, -2, -3).hashCode()), // mixed numeric Double and null
        Arguments.of(Arrays.asList(1.5, -2.5, null), Arrays.asList(1.5, -2.5, null).hashCode()), // longer boolean pattern
        Arguments.of(List.of(true, false, true, false, true), Arrays.asList(true, false, true, false, true).hashCode()), // only nulls
        Arguments.of(Arrays.asList(null, null, null), Arrays.asList(null, null, null).hashCode()), // three distinct custom objects with different hashes
        Arguments.of(List.of(new CustomObj("C1", 11), new CustomObj("C2", 22), new CustomObj("C3", 33)), List.of(new CustomObj("C1", 11), new CustomObj("C2", 22), new CustomObj("C3", 33)).hashCode()), // heterogeneous list
        Arguments.of(Arrays.asList(1, "two", true, null, 5.5), Arrays.asList(1, "two", true, null, 5.5).hashCode()), // large list of 50 elements (0‑9 repeated 5 times)
        Arguments.of(createLargeList(), new ArrayList<>(createLargeList()).hashCode()));
    }

    /**
     * Helper to create the 50‑element list used in the large‑list test case.
     */
    private static List<Integer> createLargeList() {
        List<Integer> list = new ArrayList<>(50);
        int[] pattern = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        for (int i = 0; i < 5; i++) {
            for (int v : pattern) {
                list.add(v);
            }
        }
        return list;
    }

    @Test
    void hashCodeDelegatesToUnderlyingList() {
        // Arrange
        when(mockBackingList.hashCode()).thenReturn(42);
        // Act
        int result = mockConcurrentList.hashCode();
        // Assert
        assertEquals(42, result, "hashCode() should return the value supplied by the backing list");
        verify(mockBackingList, times(1)).hashCode();
    }

    @Test
    void hashCodeMatchesUnderlyingListForEmptyList() {
        // The real ConcurrentList starts empty.
        // hash code of an empty list (should be 1)
        int expected = new ArrayList<Integer>().hashCode();
        assertEquals(expected, realConcurrentList.hashCode(), "hashCode of an empty ConcurrentList must equal hashCode of an empty backing list");
    }

    @Test
    void hashCodeReflectsListContentsAfterModifications() {
        // Populate the list with known elements.
        List<Integer> data = List.of(5, 10, 15);
        for (Integer i : data) {
            realConcurrentList.add(i);
        }
        // The expected hash code is the one computed by a regular ArrayList with the same elements.
        List<Integer> reference = new ArrayList<>(data);
        int expected = reference.hashCode();
        assertEquals(expected, realConcurrentList.hashCode(), "hashCode should reflect the current contents of the list");
        // Change the list and verify the hash code updates accordingly.
        // list now [5,15]
        realConcurrentList.remove(Integer.valueOf(10));
        reference.remove(Integer.valueOf(10));
        assertEquals(reference.hashCode(), realConcurrentList.hashCode(), "hashCode must change after the list is mutated");
    }

    @Test
    void hashCodeWorksWhenBackingListContainsNulls() {
        // Add null elements – underlying ArrayList permits nulls.
        realConcurrentList.add(null);
        realConcurrentList.add(1);
        realConcurrentList.add(null);
        List<Integer> reference = new ArrayList<>();
        reference.add(null);
        reference.add(1);
        reference.add(null);
        assertEquals(reference.hashCode(), realConcurrentList.hashCode(), "hashCode should correctly handle null elements");
    }

    @ParameterizedTest(name = "{index}: hashCode with elements {0}")
    @MethodSource("hashCodeScenarios")
    void hashCodeMatchesReferenceList(List<Object> initialElements, int expectedHash) {
        // Arrange: create a fresh ConcurrentList and populate it.
        ConcurrentList<Object> cl = new ConcurrentList<>();
        for (Object o : initialElements) {
            //noinspection unchecked
            cl.add(o);
        }
        // Act
        int actual = cl.hashCode();
        // Assert
        assertEquals(expectedHash, actual, () -> "hashCode should match reference list for elements: " + initialElements);
    }

    @Test
    void hashCodeThrowsNullPointerExceptionWhenBackingListIsNull() throws Exception {
        // Create a normal instance and then nullify its private 'list' field via reflection.
        ConcurrentList<Object> cl = new ConcurrentList<>();
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(cl, null);
        // Expect NPE when hashCode is invoked.
        assertThrows(NullPointerException.class, cl::hashCode, "Calling hashCode() with a null internal list should throw NullPointerException");
    }

    @Test
    void hashCodePropagatesExceptionFromElementHashCode() {
        ConcurrentList<Object> cl = new ConcurrentList<>();
        cl.add(new ExceptionHashObject());
        // The exception should bubble up unchanged.
        IllegalStateException ex = assertThrows(IllegalStateException.class, cl::hashCode, "hashCode() must propagate exceptions thrown by element.hashCode()");
        assertEquals("Deliberate exception from element", ex.getMessage());
    }

    @Test
    void hashCodePropagatesExceptionFromUnderlyingList() {
        // Inject the throwing list via the wrapper‑mode constructor.
        ConcurrentList<Object> cl = new ConcurrentList<>(new ThrowingList<>());
        // The hashCode call must propagate the ConcurrentModificationException.
        assertThrows(ConcurrentModificationException.class, cl::hashCode, "hashCode() should propagate exceptions thrown by the backing list");
    }
}
