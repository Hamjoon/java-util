package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

class CollectionUtilities_isEmpty_Test {

    // Reusable collections for the tests
    private List<String> list;

    private Set<Integer> set;

    /**
     * Simple {@link Collection} implementation that throws a supplied exception
     * from its {@code isEmpty()} method. All other methods delegate to an empty
     * {@link ArrayList} to keep the implementation minimal.
     */
    private static class ThrowingCollection<E> implements Collection<E> {

        private final RuntimeException toThrow;

        ThrowingCollection(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public boolean isEmpty() {
            throw toThrow;
        }

        // The remaining methods are not relevant for the test; they delegate to an empty list.
        private final List<E> delegate = new ArrayList<>();

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
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create fresh, empty collections before each test
        list = new ArrayList<>();
        set = new HashSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test tidy
        list = null;
        set = null;
    }

    /**
     * Supplies a stream of arguments for the {@code isEmpty} null/empty/non‑empty checks.
     * Each argument consists of:
     * <ul>
     *   <li>the collection instance to test</li>
     *   <li>the expected boolean result</li>
     *   <li>a textual description used in the test name</li>
     * </ul>
     */
    private static Stream<Arguments> provideCollectionsForIsEmpty() {
        return Stream.of(// Null and empty checks
        Arguments.of(null, true, "null collection"), Arguments.of(new ArrayList<>(), true, "empty ArrayList"), Arguments.of(new LinkedHashSet<>(), true, "empty LinkedHashSet"), Arguments.of(new HashSet<>(), true, "empty HashSet"), Arguments.of(new TreeSet<>(), true, "empty TreeSet"), Arguments.of(new Vector<>(), true, "empty Vector"), Arguments.of(Collections.emptyList(), true, "Collections.emptyList()"), Arguments.of(Collections.emptySet(), true, "Collections.emptySet()"), Arguments.of(Collections.unmodifiableList(new ArrayList<>()), true, "unmodifiable empty List"), Arguments.of(Collections.unmodifiableSet(new HashSet<>()), true, "unmodifiable empty Set"), // Non‑empty collections
        Arguments.of(Arrays.asList(1, 2, 3), false, "non‑empty List created via Arrays.asList"), Arguments.of(new HashSet<>(Arrays.asList("a")), false, "non‑empty HashSet with one element"), Arguments.of(Arrays.asList((Object) null), false, "List containing a single null element"), Arguments.of(new LinkedList<>(Arrays.asList("x", "y")), false, "non‑empty LinkedList"), Arguments.of(new TreeSet<>(Arrays.asList(5, 10, 15)), false, "non‑empty TreeSet"), Arguments.of(Collections.singletonList("only"), false, "singleton List"), Arguments.of(Collections.singleton("only"), false, "singleton Set"));
    }

    private static Stream<Arguments> provideThrowingCollections() {
        return Stream.of(Arguments.of(new ThrowingCollection<>(new IllegalStateException("isEmpty failed")), IllegalStateException.class, "IllegalStateException from isEmpty"), Arguments.of(new ThrowingCollection<>(new NullPointerException("null inside")), NullPointerException.class, "NullPointerException from isEmpty"), Arguments.of(new ThrowingCollection<>(new UnsupportedOperationException("unsupported")), UnsupportedOperationException.class, "UnsupportedOperationException from isEmpty"));
    }

    @Test
    @DisplayName("isEmpty(null) should return true")
    void testIsEmpty_NullCollection() {
        // given
        List<String> nullList = null;
        // when
        boolean result = CollectionUtilities.isEmpty(nullList);
        // then
        assertTrue(result, "Expected isEmpty(null) to return true");
    }

    @Test
    @DisplayName("isEmpty(empty List) should return true")
    void testIsEmpty_EmptyList() {
        // given: list is already empty from @BeforeEach
        // when
        boolean result = CollectionUtilities.isEmpty(list);
        // then
        assertTrue(result, "Expected isEmpty(empty List) to return true");
    }

    @Test
    @DisplayName("isEmpty(non‑empty List) should return false")
    void testIsEmpty_NonEmptyList() {
        // given
        list.add("element");
        // when
        boolean result = CollectionUtilities.isEmpty(list);
        // then
        assertFalse(result, "Expected isEmpty(non‑empty List) to return false");
    }

    @Test
    @DisplayName("isEmpty(empty Set) should return true")
    void testIsEmpty_EmptySet() {
        // given: set is already empty from @BeforeEach
        // when
        boolean result = CollectionUtilities.isEmpty(set);
        // then
        assertTrue(result, "Expected isEmpty(empty Set) to return true");
    }

    @Test
    @DisplayName("isEmpty(non‑empty Set) should return false")
    void testIsEmpty_NonEmptySet() {
        // given
        set.add(42);
        // when
        boolean result = CollectionUtilities.isEmpty(set);
        // then
        assertFalse(result, "Expected isEmpty(non‑empty Set) to return false");
    }

    @ParameterizedTest(name = "isEmpty({2}) should return {1}")
    @MethodSource("provideCollectionsForIsEmpty")
    void testIsEmpty_VariousCollections(Collection<?> col, boolean expected, String description) {
        // when
        boolean result = CollectionUtilities.isEmpty(col);
        // then
        assertEquals(expected, result, () -> "isEmpty on " + description + " expected " + expected + " but got " + result);
    }

    @ParameterizedTest(name = "isEmpty should propagate {2}")
    @MethodSource("provideThrowingCollections")
    void testIsEmpty_ExceptionPropagation(Collection<?> col, Class<? extends RuntimeException> expectedException, String description) {
        // when / then
        RuntimeException ex = assertThrows(expectedException, () -> CollectionUtilities.isEmpty(col), () -> "Expected isEmpty to propagate " + description);
        // optional: verify the original message is preserved
        assertNotNull(ex.getMessage(), "Exception message should not be null");
    }
}
