package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_size_Test {

    /**
     * Simple custom collection that pretends to have a fixed size.
     */
    private static class CustomCollection<E> extends AbstractCollection<E> {

        private final int fixedSize;

        CustomCollection(int size) {
            this.fixedSize = size;
        }

        @Override
        public Iterator<E> iterator() {
            // Not needed for size() tests – return empty iterator.
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return fixedSize;
        }
    }

    /**
     * Base class for collections that throw a specific unchecked exception from size().
     */
    private abstract static class ExceptionThrowingBase<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            throw createException();
        }

        protected abstract RuntimeException createException();
    }

    private static class ExceptionThrowingCollection<E> extends ExceptionThrowingBase<E> {

        @Override
        protected RuntimeException createException() {
            return new UnsupportedOperationException("size() not supported");
        }
    }

    private static class IllegalStateExceptionCollection<E> extends ExceptionThrowingBase<E> {

        @Override
        protected RuntimeException createException() {
            return new IllegalStateException("illegal state while computing size");
        }
    }

    private static class ConcurrentModificationExceptionCollection<E> extends ExceptionThrowingBase<E> {

        @Override
        protected RuntimeException createException() {
            return new ConcurrentModificationException("concurrent modification detected");
        }
    }

    private static class RuntimeExceptionCollection<E> extends ExceptionThrowingBase<E> {

        @Override
        protected RuntimeException createException() {
            return new RuntimeException("generic runtime exception");
        }
    }

    private static class NullPointerExceptionCollection<E> extends ExceptionThrowingBase<E> {

        @Override
        protected RuntimeException createException() {
            return new NullPointerException("null pointer inside size()");
        }
    }

    static Stream<Arguments> nullAndNonNullBranchProvider() {
        return Stream.of(// null reference
        Arguments.of("null reference → 0", null, 0), // empty ArrayList
        Arguments.of("empty ArrayList → 0", new ArrayList<>(), 0), // List with three integers
        Arguments.of("List [1,2,3] → 3", Arrays.asList(1, 2, 3), 3), // HashSet with two distinct strings
        Arguments.of("HashSet {\"a\",\"b\"} → 2", new HashSet<>(Arrays.asList("a", "b")), 2), // Custom collection that returns a preset size (5)
        Arguments.of("CustomCollection preset size 5 → 5", new CustomCollection<>(5), 5));
    }

    static Stream<Arguments> variousImplementationsProvider() {
        return Stream.of(Arguments.of("LinkedHashSet with three elements → 3", new LinkedHashSet<>(Arrays.asList("x", "y", "z")), 3), Arguments.of("TreeSet with duplicate element → 3", new TreeSet<>(Arrays.asList(1, 2, 2, 3)), 3), Arguments.of("empty immutable Set → 0", Collections.emptySet(), 0), Arguments.of("unmodifiable List with two elements → 2", Collections.unmodifiableList(Arrays.asList("a", "b")), 2), Arguments.of("synchronized List with four elements → 4", Collections.synchronizedList(new ArrayList<>(Arrays.asList(10, 20, 30, 40))), 4));
    }

    static Stream<Arguments> exceptionThrowingCollectionsProvider() {
        return Stream.of(Arguments.of(new ExceptionThrowingCollection<>(), UnsupportedOperationException.class), Arguments.of(new IllegalStateExceptionCollection<>(), IllegalStateException.class), Arguments.of(new ConcurrentModificationExceptionCollection<>(), ConcurrentModificationException.class), Arguments.of(new RuntimeExceptionCollection<>(), RuntimeException.class), Arguments.of(new NullPointerExceptionCollection<>(), NullPointerException.class));
    }

    @Test
    @DisplayName("size(null) returns 0")
    void size_nullCollection_returnsZero() {
        // given
        List<String> nullList = null;
        // when
        int result = CollectionUtilities.size(nullList);
        // then
        assertEquals(0, result, "size(null) should return 0");
    }

    @Test
    @DisplayName("size(empty collection) returns 0")
    void size_emptyCollection_returnsZero() {
        // given
        List<Integer> emptyList = Collections.emptyList();
        // when
        int result = CollectionUtilities.size(emptyList);
        // then
        assertEquals(0, result, "size of an empty collection should be 0");
    }

    @Test
    @DisplayName("size(non‑empty collection) returns actual size")
    void size_nonEmptyCollection_returnsCorrectSize() {
        // given
        Set<String> set = new HashSet<>();
        set.add("alpha");
        set.add("beta");
        // size = 3
        set.add("gamma");
        // when
        int result = CollectionUtilities.size(set);
        // then
        assertEquals(3, result, "size should match the number of elements in the collection");
    }

    @Test
    @DisplayName("size(collection with null elements) counts nulls")
    void size_collectionWithNullElements_countsThem() {
        // given
        List<String> list = new ArrayList<>();
        list.add(null);
        list.add("value");
        // size = 2, null elements are still counted by Collection.size()
        // when
        int result = CollectionUtilities.size(list);
        // then
        assertEquals(2, result, "size should count null elements present in the collection");
    }

    /**
     * 1. Null and non‑null branches **
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullAndNonNullBranchProvider")
    void size_nullAndNonNullBranches(String description, Collection<?> col, Object expected) {
        if (expected instanceof Integer) {
            int result = CollectionUtilities.size(col);
            assertEquals(expected, result, description);
        } else if (expected instanceof Class) {
            // expected is an exception class
            assertThrows((Class<? extends Throwable>) expected, () -> CollectionUtilities.size(col), description);
        }
    }

    /**
     * 2. Various concrete implementations **
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("variousImplementationsProvider")
    void size_variousImplementations(String description, Collection<?> col, int expected) {
        int result = CollectionUtilities.size(col);
        assertEquals(expected, result, description);
    }

    /**
     * 3. Collection containing null elements **
     */
    @Test
    @DisplayName("List with two null elements → size 2")
    void size_collectionContainingNullElements() {
        List<Object> list = Arrays.asList(null, null);
        int result = CollectionUtilities.size(list);
        assertEquals(2, result, "A list holding two null references should have size 2");
    }

    /**
     * 4. Sub‑list view **
     */
    @Test
    @DisplayName("subList view of [1,2,3,4,5] (1,4) → size 3")
    void size_subListView() {
        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5);
        // elements 2,3,4
        List<Integer> sub = original.subList(1, 4);
        int result = CollectionUtilities.size(sub);
        assertEquals(3, result, "subList view should report its own size");
    }

    @ParameterizedTest(name = "[{index}] size() throws {1}")
    @MethodSource("exceptionThrowingCollectionsProvider")
    void size_propagatesExceptions(Collection<?> col, Class<? extends Throwable> exceptionClass) {
        assertThrows(exceptionClass, () -> CollectionUtilities.size(col), "size() should propagate " + exceptionClass.getSimpleName());
    }
}
