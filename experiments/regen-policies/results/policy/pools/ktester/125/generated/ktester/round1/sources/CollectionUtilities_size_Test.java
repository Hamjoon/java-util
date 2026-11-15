package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_size_Test {

    /**
     * A collection that always reports size 42 regardless of its contents.
     */
    private static class AlwaysSizeCollection<E> extends AbstractCollection<E> {

        private final Collection<E> delegate;

        AlwaysSizeCollection(Collection<E> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            return 42;
        }
    }

    /**
     * A collection whose size() method throws {@link UnsupportedOperationException}.
     */
    private static class SizeThrowsCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("size not supported");
        }
    }

    /**
     * A collection whose size() method throws {@link NullPointerException}.
     */
    private static class NullPointerSizeCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            throw new NullPointerException("size NPE");
        }
    }

    /**
     * A collection whose size() method throws {@link IllegalStateException}.
     */
    private static class IllegalStateSizeCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            throw new IllegalStateException("illegal state in size()");
        }
    }

    /**
     * A collection whose size() method throws {@link ConcurrentModificationException}.
     */
    private static class ConcurrentModificationSizeCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            throw new ConcurrentModificationException("concurrent modification in size()");
        }
    }

    /**
     * A collection whose size() method throws a generic {@link RuntimeException}.
     */
    private static class RuntimeExceptionSizeCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            throw new RuntimeException("generic runtime exception from size()");
        }
    }

    private static Stream<Arguments> provideTypicalCollections() {
        return Stream.of(// null collection
        Arguments.of(null, 0), // empty ArrayList
        Arguments.of(new ArrayList<>(), 0), // non‑empty List
        Arguments.of(Arrays.asList(1, 2, 3), 3), // non‑empty Set
        Arguments.of(new HashSet<>(Set.of("a", "b")), 2), // unmodifiable List with one element
        Arguments.of(Collections.unmodifiableList(Arrays.asList("x")), 1), // synchronized Set with four elements
        Arguments.of(Collections.synchronizedSet(new LinkedHashSet<>(List.of(5, 6, 7, 8))), 4));
    }

    private static Stream<Arguments> provideAdditionalCollections() {
        return Stream.of(// empty LinkedList
        Arguments.of(new LinkedList<>(), 0), // TreeSet (SortedSet) with three distinct elements
        Arguments.of(new TreeSet<>(Set.of(3, 1, 4)), 3), // List containing only nulls – each null counts as an element
        Arguments.of(Arrays.asList(null, null, null), 3), // empty unmodifiable collection
        Arguments.of(Collections.unmodifiableCollection(new ArrayList<>()), 0), // empty synchronized collection
        Arguments.of(Collections.synchronizedCollection(new LinkedHashSet<>()), 0), // subList view (size should be of the view, not the backing list)
        Arguments.of(Arrays.asList(10, 20, 30, 40).subList(1, 3), 2), // custom collection that always returns 42 from size()
        Arguments.of(new AlwaysSizeCollection<>(Arrays.asList("a", "b")), 42));
    }

    private static Stream<Arguments> provideExceptionCollections() {
        return Stream.of(// size() throws UnsupportedOperationException
        Arguments.of(new SizeThrowsCollection<>(), UnsupportedOperationException.class), // size() throws NullPointerException
        Arguments.of(new NullPointerSizeCollection<>(), NullPointerException.class), // size() throws IllegalStateException
        Arguments.of(new IllegalStateSizeCollection<>(), IllegalStateException.class), // size() throws ConcurrentModificationException
        Arguments.of(new ConcurrentModificationSizeCollection<>(), ConcurrentModificationException.class), // size() throws generic RuntimeException
        Arguments.of(new RuntimeExceptionSizeCollection<>(), RuntimeException.class));
    }

    @Test
    @DisplayName("size returns 0 for a null collection")
    void size_NullCollection_ReturnsZero() {
        // given
        Collection<?> nullCollection = null;
        // when
        int result = CollectionUtilities.size(nullCollection);
        // then
        assertEquals(0, result, "size(null) should return 0");
    }

    @Test
    @DisplayName("size returns 0 for an empty collection")
    void size_EmptyCollection_ReturnsZero() {
        // given
        Collection<String> emptyList = new ArrayList<>();
        // when
        int result = CollectionUtilities.size(emptyList);
        // then
        assertEquals(0, result, "size(empty collection) should return 0");
    }

    @Test
    @DisplayName("size returns correct count for a non‑empty List")
    void size_NonEmptyList_ReturnsCorrectCount() {
        // given
        List<Integer> numbers = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        // when
        int result = CollectionUtilities.size(numbers);
        // then
        assertEquals(5, result, "size(list with 5 elements) should return 5");
    }

    @Test
    @DisplayName("size returns correct count for a non‑empty Set")
    void size_NonEmptySet_ReturnsCorrectCount() {
        // given
        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        // when
        int result = CollectionUtilities.size(set);
        // then
        assertEquals(3, result, "size(set with 3 elements) should return 3");
    }

    @Test
    @DisplayName("size works with a collection that reports a custom size")
    void size_CollectionWithCustomSize_ReturnsCustomSize() {
        // given
        Collection<Integer> customCollection = new AbstractCollection<>() {

            @Override
            public Iterator<Integer> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 42;
            }
        };
        // when
        int result = CollectionUtilities.size(customCollection);
        // then
        assertEquals(42, result, "size(custom collection) should return the overridden size value");
    }

    /* -------------------------------------------------------------
     *  Group 1 – null and typical collections
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => col={0}, expected={1}")
    @MethodSource("provideTypicalCollections")
    @DisplayName("size returns expected value for null and typical collections")
    void size_TypicalCollections_ReturnsExpected(Collection<?> col, int expected) {
        // when
        int result = CollectionUtilities.size(col);
        // then
        assertEquals(expected, result, () -> "size(" + (col == null ? "null" : col.getClass().getSimpleName()) + ") should be " + expected);
    }

    /* -------------------------------------------------------------
     *  Group 2 – additional collection types and edge cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => col={0}, expected={1}")
    @MethodSource("provideAdditionalCollections")
    @DisplayName("size returns expected value for additional collection types and edge cases")
    void size_AdditionalCollections_ReturnsExpected(Collection<?> col, int expected) {
        // when
        int result = CollectionUtilities.size(col);
        // then
        assertEquals(expected, result, () -> "size(" + col.getClass().getSimpleName() + ") should be " + expected);
    }

    /* -------------------------------------------------------------
     *  Group 3 – exception propagation scenarios
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => col={0}, expectedException={1}")
    @MethodSource("provideExceptionCollections")
    @DisplayName("size propagates exceptions thrown by the underlying collection")
    void size_ExceptionPropagation_PropagatesException(Collection<?> col, Class<? extends Throwable> expectedException) {
        // when / then
        Throwable thrown = assertThrows(expectedException, () -> CollectionUtilities.size(col));
        assertNotNull(thrown, "Exception should be propagated");
    }
}
