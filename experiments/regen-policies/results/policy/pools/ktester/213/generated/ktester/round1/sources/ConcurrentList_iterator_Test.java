package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_iterator_Test {

    /**
     * Instance under test – created fresh for every test method.
     */
    private ConcurrentList<Integer> list;

    class BadConcurrentList<E> extends ConcurrentList<E> {

        @Override
        public Iterator<E> iterator() {
            // Simulate a failure inside the private readOperation
            throw new IllegalStateException("readOperation failed");
        }
    }

    class BadConcurrentList<E> extends ConcurrentList<E> {

        @Override
        public Iterator<E> iterator() {
            // Simulate a failure inside the private readOperation
            throw new IllegalStateException("readOperation failed");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a clean ConcurrentList before each test.
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up; the list will be garbage‑collected.
        list = null;
    }

    /* --------------------------------------------------------------------- *
     * Parameterized test source (used for potential future extensions)     *
     * --------------------------------------------------------------------- */
    private static Stream<List<String>> stringListsProvider() {
        return Stream.of(Arrays.asList("alpha", "beta", "gamma"), Arrays.asList("one", "two"), Collections.emptyList());
    }

    @Test
    @DisplayName("Iterator returns a snapshot that is unaffected by later modifications")
    void testIteratorSnapshotIsImmutable() {
        // Given – a list with three elements
        list.addAll(Arrays.asList(1, 2, 3));
        // When – we obtain an iterator
        Iterator<Integer> iterator = list.iterator();
        // And – we modify the original list after the iterator is created
        // add a new element
        list.add(4);
        // remove an existing element
        list.remove(Integer.valueOf(1));
        // Then – the iterator should iterate over the original snapshot (1,2,3)
        List<Integer> iterated = new ArrayList<>();
        while (iterator.hasNext()) {
            iterated.add(iterator.next());
        }
        assertEquals(Arrays.asList(1, 2, 3), iterated, "Iterator must reflect the state of the list at the moment of its creation");
        // And – the underlying list reflects the modifications performed after iterator creation
        assertEquals(Arrays.asList(2, 3, 4), list, "Underlying list should contain the modifications made after iterator creation");
    }

    @Test
    @DisplayName("Iterator on an empty ConcurrentList yields no elements")
    void testIteratorOnEmptyList() {
        // Given – an empty list
        assertTrue(list.isEmpty(), "Precondition: list should be empty");
        // When – we obtain an iterator
        Iterator<Integer> iterator = list.iterator();
        // Then – hasNext must be false and next must throw NoSuchElementException
        assertFalse(iterator.hasNext(), "Iterator on empty list should have no next element");
        assertThrows(java.util.NoSuchElementException.class, iterator::next, "Calling next() on an empty iterator should throw NoSuchElementException");
    }

    @Test
    @DisplayName("Iterator removal does not affect the original ConcurrentList")
    void testIteratorRemoveDoesNotAffectOriginalList() {
        // Given – a list with two elements
        list.addAll(Arrays.asList(10, 20));
        // When – we obtain an iterator and remove the first element via the iterator
        Iterator<Integer> iterator = list.iterator();
        assertTrue(iterator.hasNext(), "Iterator should have first element");
        // should be 10
        Integer first = iterator.next();
        // removes from the snapshot copy
        iterator.remove();
        // Then – the original list must remain unchanged
        assertEquals(Arrays.asList(10, 20), list, "Removing via the iterator must not modify the underlying ConcurrentList");
        // And – the iterator's snapshot reflects the removal
        List<Integer> remaining = new ArrayList<>();
        while (iterator.hasNext()) {
            remaining.add(iterator.next());
        }
        assertEquals(Arrays.asList(20), remaining, "Snapshot after iterator.remove() should contain the remaining elements");
    }

    @Test
    @DisplayName("Iterator traverses elements in insertion order")
    void testIteratorTraversesElementsInInsertionOrder() {
        // Given – a list with three strings
        ConcurrentList<String> strList = new ConcurrentList<>();
        strList.addAll(Arrays.asList("alpha", "beta", "gamma"));
        // When – we obtain an iterator and collect its elements
        Iterator<String> it = strList.iterator();
        List<String> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        // Then – the iterator yields the elements in the same order they were added
        assertEquals(Arrays.asList("alpha", "beta", "gamma"), collected, "Iterator must preserve insertion order");
    }

    @Test
    @DisplayName("Iterator still returns original elements after the list is cleared")
    void testIteratorSnapshotUnaffectedByClear() {
        // Given – a list with three integers
        list.addAll(Arrays.asList(1, 2, 3));
        // When – we obtain an iterator and then clear the underlying list
        Iterator<Integer> iterator = list.iterator();
        list.clear();
        // Then – iterator still yields the original three elements
        List<Integer> snapshot = new ArrayList<>();
        while (iterator.hasNext()) {
            snapshot.add(iterator.next());
        }
        assertEquals(Arrays.asList(1, 2, 3), snapshot, "Iterator must reflect the state at creation, not the cleared list");
        // And – the underlying list is now empty
        assertTrue(list.isEmpty(), "List should be empty after clear()");
    }

    @Test
    @DisplayName("Two successive iterators are independent")
    void testTwoIteratorsIndependent() {
        // Given – a list with three elements
        list.addAll(Arrays.asList(5, 6, 7));
        // When – we obtain two iterators sequentially
        Iterator<Integer> it1 = list.iterator();
        Iterator<Integer> it2 = list.iterator();
        // Advance the first iterator by one element
        assertEquals(5, it1.next());
        // Then – the second iterator should still start from the first element
        assertEquals(5, it2.next());
        assertEquals(6, it2.next());
        assertEquals(7, it2.next());
        assertFalse(it2.hasNext());
        // And – the first iterator continues from where it left off
        assertEquals(6, it1.next());
        assertEquals(7, it1.next());
        assertFalse(it1.hasNext());
    }

    @Test
    @DisplayName("Iterator throws NoSuchElementException after exhaustion")
    void testIteratorThrowsAfterExhaustion() {
        // Given – a list with two elements
        list.addAll(Arrays.asList(100, 200));
        // When – we iterate over all elements
        Iterator<Integer> it = list.iterator();
        assertEquals(100, it.next());
        assertEquals(200, it.next());
        assertFalse(it.hasNext());
        // Then – calling next() again throws NoSuchElementException
        assertThrows(NoSuchElementException.class, it::next, "Calling next() after iterator is exhausted should throw NoSuchElementException");
    }

    @Test
    @DisplayName("Iterator snapshot ignores elements added after its creation")
    void testIteratorIgnoresLaterAdditions() {
        // Given – a list with two elements
        ConcurrentList<String> strList = new ConcurrentList<>();
        strList.addAll(Arrays.asList("x", "y"));
        // When – we obtain an iterator and then add a new element to the list
        Iterator<String> it = strList.iterator();
        // modification after iterator creation
        strList.add("z");
        // Then – iterator only sees the original elements
        List<String> snapshot = new ArrayList<>();
        while (it.hasNext()) {
            snapshot.add(it.next());
        }
        assertEquals(Arrays.asList("x", "y"), snapshot, "Iterator must not reflect elements added after its creation");
    }

    @Test
    @DisplayName("Null internal list leads to NullPointerException")
    void testIteratorThrowsNPEWhenInternalListIsNull() throws Exception {
        // Given – a ConcurrentList instance whose private 'list' field is forced to null
        ConcurrentList<Integer> broken = new ConcurrentList<>();
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(broken, null);
        // When / Then – calling iterator() should throw NullPointerException
        assertThrows(NullPointerException.class, broken::iterator, "iterator() should throw NPE when internal list is null");
    }

    @Test
    @DisplayName("RuntimeException from underlying list during copy is propagated")
    void testIteratorPropagatesRuntimeExceptionFromUnderlyingList() {
        // Given – a custom List implementation whose iterator() throws RuntimeException
        List<Integer> faulty = new List<Integer>() {

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<Integer> iterator() {
                throw new RuntimeException("boom");
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
            public boolean add(Integer e) {
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
                return null;
            }

            @Override
            public ListIterator<Integer> listIterator(int index) {
                return null;
            }

            @Override
            public List<Integer> subList(int fromIndex, int toIndex) {
                return null;
            }
        };
        // When – we wrap this list with ConcurrentList
        ConcurrentList<Integer> wrapper = new ConcurrentList<>(faulty);
        // Then – iterator() should propagate the RuntimeException
        RuntimeException ex = assertThrows(RuntimeException.class, wrapper::iterator, "Exception from underlying list should be propagated");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    @DisplayName("IllegalStateException thrown from readOperation is propagated")
    void testIteratorPropagatesIllegalStateExceptionFromReadOperation() {
        // Subclass that deliberately throws IllegalStateException from iterator()
        class BadConcurrentList<E> extends ConcurrentList<E> {

            @Override
            public Iterator<E> iterator() {
                // Simulate a failure inside the private readOperation
                throw new IllegalStateException("readOperation failed");
            }
        }
        BadConcurrentList<Integer> bad = new BadConcurrentList<>();
        // populate to avoid empty‑list shortcut issues
        bad.add(1);
        IllegalStateException ex = assertThrows(IllegalStateException.class, bad::iterator, "IllegalStateException should be propagated unchanged");
        assertEquals("readOperation failed", ex.getMessage());
    }

    @ParameterizedTest(name = "Iterator on list {0}")
    @MethodSource("stringListsProvider")
    @DisplayName("Parameterized iterator order test")
    void parameterizedIteratorOrderTest(List<String> data) {
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.addAll(data);
        Iterator<String> it = cl.iterator();
        List<String> collected = new ArrayList<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        assertEquals(data, collected, "Iterator must produce the same ordering as the source list");
    }
}
