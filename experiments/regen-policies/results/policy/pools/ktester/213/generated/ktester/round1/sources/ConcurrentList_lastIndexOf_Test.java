package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_lastIndexOf_Test {

    private List<Integer> list;

    /**
     * Simple immutable point class used for equality tests.
     */
    private static final class Point {

        private final int x;

        private final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Point))
                return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return "Point(" + x + "," + y + ")";
        }
    }

    /**
     * List implementation that delegates to an internal {@link ArrayList}
     * but throws {@link UnsupportedOperationException} from {@code lastIndexOf}.
     */
    private static class ThrowingList<E> implements List<E> {

        private final List<E> delegate;

        ThrowingList(Collection<E> c) {
            this.delegate = new ArrayList<>(c);
        }

        @Override
        public int lastIndexOf(Object o) {
            throw new UnsupportedOperationException("lastIndexOf is unsupported in this test list");
        }

        // -----------------------------------------------------------------
        // All other List methods simply delegate to the internal list.
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
        public int indexOf(Object o) {
            return delegate.indexOf(o);
        }

        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return delegate.subList(fromIndex, toIndex);
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
        // Use the no‑arg constructor (stand‑alone mode) for a fresh list before each test
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required
        list = null;
    }

    /**
     * Provides arguments for the "branch coverage" group.
     */
    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(// empty list, searching for null → -1
        Arguments.of(new ArrayList<>(), null, -1), // ["x","a","b","a"] → after removing last element, last "a" is at index 2
        Arguments.of(new ArrayList<>(Arrays.asList("x", "a", "b", "a")), "a", 2, true), // object not present → -1
        Arguments.of(new ArrayList<>(Arrays.asList("x", "a", "b", "a")), "missing", -1), // mixed types [1, 42, "42"] → lastIndexOf(42) == 1
        Arguments.of(new ArrayList<>(Arrays.asList(1, 42, "42")), 42, 1), // multiple "duplicate" entries → highest index 3
        Arguments.of(new ArrayList<>(Arrays.asList("duplicate", "dup", "duplicate", "duplicate")), "duplicate", 3));
    }

    // Overloaded provider for the case that requires a removal before the assertion
    private static Stream<Arguments> branchCoverageProviderWithRemoval() {
        // Only the case that needs removal is expressed here
        List<Object> base = new ArrayList<>(Arrays.asList("x", "a", "b", "a"));
        return Stream.of(Arguments.of(base, "a", 2, true));
    }

    /**
     * Provides arguments for the "additional cases" group.
     */
    private static Stream<Arguments> additionalCasesProvider() {
        return Stream.of(// empty list, any non‑null → -1
        Arguments.of(new ArrayList<>(), "any", -1), // [null, "a", null] → last null = 2
        Arguments.of(new ArrayList<>(Arrays.asList(null, "a", null)), null, 2), // same list, search "a" → 1
        Arguments.of(new ArrayList<>(Arrays.asList(null, "a", null)), "a", 1), // single‑element ["only"] → 0
        Arguments.of(new ArrayList<>(Collections.singletonList("only")), "only", 0), // single‑element ["only"], absent → -1
        Arguments.of(new ArrayList<>(Collections.singletonList("only")), "absent", -1), // two‑element ["first","second"] → 1
        Arguments.of(new ArrayList<>(Arrays.asList("first", "second")), "second", 1), // all identical ["dup","dup","dup"] → 2
        Arguments.of(new ArrayList<>(Arrays.asList("dup", "dup", "dup")), "dup", 2), // custom Point objects
        Arguments.of(new ArrayList<>(Arrays.asList(new Point(1, 2), new Point(3, 4), new Point(1, 2))), new Point(1, 2), 2), // distinct String instance (new String("test"))
        Arguments.of(new ArrayList<>(Collections.singletonList(new String("test"))), "test", 0), // integers [1,2,3] searching 42 → -1
        Arguments.of(new ArrayList<>(Arrays.asList(1, 2, 3)), 42, -1));
    }

    @Test
    void lastIndexOf_basicBehaviour() {
        list.addAll(Arrays.asList(1, 2, 3, 2, 4));
        // The last occurrence of 2 should be at index 3
        assertEquals(3, list.lastIndexOf(2));
        // The last occurrence of an element not present should be -1
        assertEquals(-1, list.lastIndexOf(5));
    }

    @Test
    void lastIndexOf_withNullElements() {
        List<String> stringList = new ConcurrentList<>();
        stringList.addAll(Arrays.asList("a", null, "b", null, "c"));
        // The last null should be at index 3
        assertEquals(3, stringList.lastIndexOf(null));
        // Normal element
        assertEquals(2, stringList.lastIndexOf("b"));
    }

    @Test
    void lastIndexOf_onEmptyList() {
        assertEquals(-1, list.lastIndexOf(1), "Empty list should always return -1");
    }

    @Test
    void lastIndexOf_whenWrappedListIsUsed() {
        // Verify that wrapper mode works the same way
        java.util.ArrayList<Integer> backing = new java.util.ArrayList<>(Arrays.asList(7, 8, 7, 9));
        List<Integer> wrapped = new ConcurrentList<>(backing);
        assertEquals(2, wrapped.lastIndexOf(7));
        // Remove an element and check again
        // removes first occurrence
        wrapped.remove(Integer.valueOf(7));
        assertEquals(1, wrapped.lastIndexOf(7));
    }

    @ParameterizedTest(name = "[branch] list={0}, search={1}, expected={2}")
    @MethodSource("branchCoverageProvider")
    void lastIndexOf_branchCoverage(List<Object> initial, Object search, int expected) throws Exception {
        // Overload that does not need removal
        runLastIndexOfScenario(initial, search, expected, false);
    }

    @ParameterizedTest(name = "[branch‑remove] list={0}, search={1}, expected={2}")
    @MethodSource("branchCoverageProviderWithRemoval")
    void lastIndexOf_branchCoverage_withRemoval(List<Object> initial, Object search, int expected, boolean removeLast) throws Exception {
        runLastIndexOfScenario(initial, search, expected, removeLast);
    }

    /**
     * Helper that creates a {@link ConcurrentList} from the supplied initial data,
     * optionally removes the last element, and then asserts {@code lastIndexOf}.
     */
    @SuppressWarnings("unchecked")
    private void runLastIndexOfScenario(List<Object> initial, Object search, int expected, boolean removeLast) {
        ConcurrentList<Object> cl = new ConcurrentList<>();
        cl.addAll(initial);
        if (removeLast && !initial.isEmpty()) {
            cl.remove(initial.size() - 1);
        }
        assertEquals(expected, cl.lastIndexOf(search));
    }

    @ParameterizedTest(name = "[additional] list={0}, search={1}, expected={2}")
    @MethodSource("additionalCasesProvider")
    void lastIndexOf_additionalCases(List<Object> initial, Object search, int expected) {
        ConcurrentList<Object> cl = new ConcurrentList<>();
        cl.addAll(initial);
        assertEquals(expected, cl.lastIndexOf(search));
    }

    @Test
    void lastIndexOf_nullBackingList_throws_NullPointerException() throws Exception {
        // Create a normal list and then break its internal backing list via reflection
        ConcurrentList<Integer> cl = new ConcurrentList<>();
        cl.add(1);
        // Use reflection to set the private field 'list' to null
        Field field = ConcurrentList.class.getDeclaredField("list");
        field.setAccessible(true);
        field.set(cl, null);
        // The call should propagate a NullPointerException from the lambda
        assertThrows(NullPointerException.class, () -> cl.lastIndexOf(1));
    }

    @Test
    void lastIndexOf_backingListThrows_UnsupportedOperationException() {
        // Backing list that throws from lastIndexOf
        List<Integer> throwingList = new ThrowingList<>(Arrays.asList(1, 2, 3));
        ConcurrentList<Integer> cl = new ConcurrentList<>(throwingList);
        assertThrows(UnsupportedOperationException.class, () -> cl.lastIndexOf(2));
    }
}
