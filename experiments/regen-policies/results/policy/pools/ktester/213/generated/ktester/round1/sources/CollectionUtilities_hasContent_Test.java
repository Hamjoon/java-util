package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_hasContent_Test {

    /**
     * A thin wrapper around an existing {@link Collection} that delegates
     * all operations except {@code isEmpty()}, which throws the supplied
     * {@link RuntimeException}. This allows us to test exception propagation
     * without having to implement every method of the {@code Collection} interface.
     */
    private static class ExceptionThrowingCollection<E> extends AbstractCollection<E> {

        private final Collection<E> delegate;

        private final RuntimeException toThrow;

        ExceptionThrowingCollection(Collection<E> delegate, RuntimeException toThrow) {
            this.delegate = Objects.requireNonNull(delegate);
            this.toThrow = Objects.requireNonNull(toThrow);
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            // The method under test calls this; we deliberately throw.
            throw toThrow;
        }

        // All other mutating methods delegate to the wrapped collection.
        @Override
        public boolean add(E e) {
            return delegate.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return delegate.remove(o);
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }

    /**
     * Supplies a stream of arguments where each argument consists of:
     *   - a descriptive name (used by JUnit for reporting)
     *   - the collection instance to test
     *   - the expected boolean result
     */
    private static Stream<Arguments> additionalCollectionsProvider() {
        return Stream.of(// ArrayList with a single integer
        Arguments.of("ArrayList single integer", new ArrayList<>(Collections.singletonList(1)), true), // LinkedHashSet created from a list with duplicate values (still size 1)
        Arguments.of("LinkedHashSet with duplicates", new LinkedHashSet<>(Arrays.asList(1, 1, 1)), true), // TreeSet (SortedSet) with three integers
        Arguments.of("TreeSet (SortedSet) integers", new TreeSet<>(Arrays.asList(3, 2, 1)), true), // ArrayDeque (Deque) with three strings
        Arguments.of("ArrayDeque with strings", new ArrayDeque<>(Arrays.asList("x", "y", "z")), true), // Unmodifiable List via Collections.unmodifiableList
        Arguments.of("Unmodifiable List", Collections.unmodifiableList(Arrays.asList("a", "b", "c")), true), // Synchronized List via Collections.synchronizedList
        Arguments.of("Synchronized List", Collections.synchronizedList(new ArrayList<>(Arrays.asList("alpha", "beta"))), true), // Collection with multiple null elements
        Arguments.of("List with multiple nulls", Arrays.asList(null, null, null), true), // SubList view of an ArrayList
        Arguments.of("SubList view", new ArrayList<>(Arrays.asList("sub", "list")).subList(0, 2), true), // Fixed‑size list created by Arrays.asList
        Arguments.of("Fixed‑size list (Arrays.asList)", Arrays.asList("fixed", "size", "list"), true), // Empty immutable collection returned by Collections.emptySet()
        Arguments.of("Empty immutable Set", Collections.emptySet(), false));
    }

    /**
     * Supplies collections whose {@code isEmpty()} method throws a specific
     * runtime exception. The test verifies that {@code hasContent} propagates
     * the exception unchanged.
     */
    private static Stream<Arguments> exceptionCollectionsProvider() {
        return Stream.of(Arguments.of("UnsupportedOperationException", new ExceptionThrowingCollection<>(new ArrayList<>(), new UnsupportedOperationException("unsupported")), UnsupportedOperationException.class), Arguments.of("IllegalStateException", new ExceptionThrowingCollection<>(new LinkedHashSet<>(), new IllegalStateException("illegal state")), IllegalStateException.class), Arguments.of("RuntimeException", new ExceptionThrowingCollection<>(new HashSet<>(), new RuntimeException("generic runtime")), RuntimeException.class));
    }

    @Test
    void hasContent_NullCollection_ReturnsFalse() {
        // GIVEN a null collection
        Collection<?> col = null;
        // WHEN checking for content
        boolean result = CollectionUtilities.hasContent(col);
        // THEN the result should be false
        assertFalse(result, "hasContent should return false for a null collection");
    }

    @Test
    void hasContent_EmptyCollection_ReturnsFalse() {
        // GIVEN an empty collection (list)
        Collection<String> col = Collections.emptyList();
        // WHEN checking for content
        boolean result = CollectionUtilities.hasContent(col);
        // THEN the result should be false
        assertFalse(result, "hasContent should return false for an empty collection");
    }

    @Test
    void hasContent_NonEmptyList_ReturnsTrue() {
        // GIVEN a non‑empty List
        List<String> col = Arrays.asList("element");
        // WHEN checking for content
        boolean result = CollectionUtilities.hasContent(col);
        // THEN the result should be true
        assertTrue(result, "hasContent should return true for a list with at least one element");
    }

    @Test
    void hasContent_NonEmptySet_ReturnsTrue() {
        // GIVEN a non‑empty Set
        Set<Integer> col = new HashSet<>(Arrays.asList(1, 2, 3));
        // WHEN checking for content
        boolean result = CollectionUtilities.hasContent(col);
        // THEN the result should be true
        assertTrue(result, "hasContent should return true for a set with at least one element");
    }

    @Test
    void hasContent_CollectionWithNullElement_ReturnsTrue() {
        // GIVEN a collection that contains a null element but is not empty
        List<Object> col = Collections.singletonList(null);
        // WHEN checking for content
        boolean result = CollectionUtilities.hasContent(col);
        // THEN the result should be true (null element does not make the collection empty)
        assertTrue(result, "hasContent should return true when collection contains a null element");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalCollectionsProvider")
    void hasContent_AdditionalCollections(String description, Collection<?> col, boolean expected) {
        // WHEN checking for content
        boolean result = CollectionUtilities.hasContent(col);
        // THEN the result matches the expectation
        assertEquals(expected, result, () -> "hasContent should return " + expected + " for: " + description);
    }

    @ParameterizedTest(name = "[{index}] Exception test: {0}")
    @MethodSource("exceptionCollectionsProvider")
    void hasContent_ExceptionPropagation(String description, Collection<?> col, Class<? extends RuntimeException> expectedException) {
        // WHEN invoking hasContent we expect the same exception to be thrown
        assertThrows(expectedException, () -> CollectionUtilities.hasContent(col), () -> "hasContent should propagate " + expectedException.getSimpleName() + " from collection.isEmpty()");
    }
}
