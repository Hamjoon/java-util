package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

// Mockito not strictly required but kept from template
@ExtendWith(MockitoExtension.class)
class ConcurrentList_equals_Test {

    private List<Integer> concurrentList;

    private List<Integer> otherList;

    private List<Integer> plainArrayList;

    /**
     * A minimal List implementation whose {@code equals} method throws a supplied exception.
     */
    private static class ThrowingEqualsList<E> implements List<E> {

        private final RuntimeException toThrow;

        ThrowingEqualsList(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public boolean equals(Object o) {
            throw toThrow;
        }

        // The remaining List methods are not used in these tests; they simply delegate to an empty list.
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
        // Fresh lists for every test to avoid cross‑test interference
        concurrentList = new ConcurrentList<>(new ArrayList<>(Arrays.asList(1, 2, 3)));
        otherList = new ConcurrentList<>(new ArrayList<>(Arrays.asList(1, 2, 3)));
        plainArrayList = new ArrayList<>(Arrays.asList(1, 2, 3));
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps GC but not strictly required
        concurrentList = null;
        otherList = null;
        plainArrayList = null;
    }

    /**
     * Supplies a stream of arguments for the equality tests.
     * Each argument consists of:
     *   - a description (used only for readability in the test name)
     *   - the {@code ConcurrentList} under test (always a list of Strings)
     *   - the {@code other} object to compare with
     *   - the expected boolean result
     */
    static Stream<Arguments> equalityScenarios() {
        return Stream.of(// Reflexive property
        Arguments.of("same instance", new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), "self", true), // Two different ConcurrentList objects with identical contents
        Arguments.of("different ConcurrentList, same elements", new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), true), // Different element values
        Arguments.of("different element values", new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "x", "c"))), false), // Plain List with same elements
        Arguments.of("plain List same elements", new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), Arrays.asList("a", "b", "c"), true), // Same elements but different order
        Arguments.of("same elements different order", new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), Arrays.asList("c", "b", "a"), false), // Null comparison
        Arguments.of("null comparison", new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), null, false), // Unrelated type
        Arguments.of("unrelated type", new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), "some string", false), // Different concrete List implementation internally
        Arguments.of("different internal List implementation", new ConcurrentList<>(new LinkedList<>(Arrays.asList("a", "b", "c"))), new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), true), // Different sizes
        Arguments.of("different sizes", new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b"))), false), // Empty vs non‑empty
        Arguments.of("empty vs non‑empty", new ConcurrentList<>(new ArrayList<>()), new ArrayList<>(Arrays.asList("a")), false), // Two empty ConcurrentLists
        Arguments.of("two empty ConcurrentLists", new ConcurrentList<>(new ArrayList<>()), new ConcurrentList<>(new ArrayList<>()), true), // Empty plain List
        Arguments.of("empty plain List", new ConcurrentList<>(new ArrayList<>()), new ArrayList<>(), true), // Lists containing null elements
        Arguments.of("null element present", new ConcurrentList<>(Arrays.asList(null, "a")), Arrays.asList(null, "a"), true), // Null element missing in other list
        Arguments.of("null element missing", new ConcurrentList<>(Arrays.asList(null, "a")), Arrays.asList("a"), false), // Unmodifiable view
        Arguments.of("unmodifiable List view", new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), Collections.unmodifiableList(Arrays.asList("a", "b", "c")), true), // Synchronized wrapper
        Arguments.of("synchronized List view", new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), Collections.synchronizedList(Arrays.asList("a", "b", "c")), true), // Set (different collection type)
        Arguments.of("different collection type (Set)", new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), new HashSet<>(Arrays.asList("a", "b", "c")), false), // SubList view
        Arguments.of("subList view", new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), Arrays.asList("x", "a", "b", "c", "y").subList(1, 4), true), // Repeated equals call – consistency
        Arguments.of("repeated equals call", new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), true));
    }

    @Test
    void equalsSameInstanceReturnsTrue() {
        assertTrue(concurrentList.equals(concurrentList), "A list should be equal to itself");
    }

    @Test
    void equalsAnotherConcurrentListWithSameElementsReturnsTrue() {
        assertTrue(concurrentList.equals(otherList), "Two ConcurrentList instances containing the same elements should be equal");
    }

    @Test
    void equalsPlainListWithSameElementsReturnsTrue() {
        assertTrue(concurrentList.equals(plainArrayList), "ConcurrentList should be equal to a regular List when they contain the same elements");
    }

    @Test
    void equalsDifferentElementsReturnsFalse() {
        List<Integer> different = new ConcurrentList<>(new ArrayList<>(Arrays.asList(4, 5, 6)));
        assertFalse(concurrentList.equals(different), "Lists with different elements must not be equal");
    }

    @Test
    void equalsNullReturnsFalse() {
        assertFalse(concurrentList.equals(null), "A list should not be equal to null");
    }

    @Test
    void equalsDifferentTypeReturnsFalse() {
        String notAList = "not a list";
        assertFalse(concurrentList.equals(notAList), "A list should not be equal to an object of a different type");
    }

    @Test
    void equalsReflectsChangesInWrappedList() {
        // underlying list is the same instance used in the constructor
        List<Integer> backing = new ArrayList<>(Arrays.asList(1, 2, 3));
        ConcurrentList<Integer> wrapper = new ConcurrentList<>(backing);
        List<Integer> other = new ArrayList<>(Arrays.asList(1, 2, 3));
        // initially equal
        assertTrue(wrapper.equals(other));
        // mutate the backing list
        backing.add(4);
        other.add(4);
        // equality should now reflect the mutation
        assertTrue(wrapper.equals(other));
        // remove an element only from the wrapper's view
        wrapper.remove(Integer.valueOf(2));
        other.remove(Integer.valueOf(2));
        assertTrue(wrapper.equals(other));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("equalityScenarios")
    void equalsVariousScenarios(String description, ConcurrentList<String> listUnderTest, Object other, boolean expected) {
        // Special handling for the "same instance" case where the other object is the list itself
        Object actualOther = "self".equals(other) ? listUnderTest : other;
        assertEquals(expected, listUnderTest.equals(actualOther), () -> "Failed scenario: " + description);
    }

    @Test
    void equalsPropagatesIllegalStateExceptionFromWrappedList() {
        List<Integer> badList = new ThrowingEqualsList<>(new IllegalStateException("boom"));
        ConcurrentList<Integer> wrapper = new ConcurrentList<>(badList);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> wrapper.equals(Collections.emptyList()), "IllegalStateException from the wrapped list must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void equalsPropagatesNullPointerExceptionFromWrappedList() {
        List<Integer> badList = new ThrowingEqualsList<>(new NullPointerException("null boom"));
        ConcurrentList<Integer> wrapper = new ConcurrentList<>(badList);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> wrapper.equals(Collections.emptyList()), "NullPointerException from the wrapped list must propagate");
        assertEquals("null boom", ex.getMessage());
    }

    @Test
    void equalsPropagatesRuntimeExceptionFromWrappedList() {
        List<Integer> badList = new ThrowingEqualsList<>(new RuntimeException("runtime boom"));
        ConcurrentList<Integer> wrapper = new ConcurrentList<>(badList);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> wrapper.equals(Collections.emptyList()), "RuntimeException from the wrapped list must propagate");
        assertEquals("runtime boom", ex.getMessage());
    }

    /**
     * This test verifies that an exception thrown by the *argument*’s {@code equals}
     * method is not swallowed. We achieve this by passing a custom List whose
     * {@code equals} throws, while the internal list is a normal {@code ArrayList}.
     * The {@code ArrayList.equals} implementation calls {@code other.equals(this)}
     * only when the other object is *not* a List, so to force the call we provide
     * a wrapper that pretends not to be a List (i.e., a plain Object). The test
     * therefore checks that the exception bubbles up unchanged.
     */
    @Test
    void equalsPropagatesExceptionFromArgumentEquals() {
        // Argument that throws when its equals method is invoked.
        List<String> throwingArgument = new ThrowingEqualsList<>(new RuntimeException("argument boom"));
        // Internal list is a normal ArrayList – its equals will attempt to compare elements
        // and will end up calling throwingArgument.equals(this) only via the fallback path.
        ConcurrentList<String> wrapper = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a")));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> wrapper.equals(throwingArgument), "Exception from the argument's equals must propagate");
        assertEquals("argument boom", ex.getMessage());
    }
}
