package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_isEmpty_Test {

    /**
     * Minimal custom collection that throws a supplied {@link RuntimeException}
     * from its {@code isEmpty()} implementation. Used to verify that
     * {@link CollectionUtilities#isEmpty(Collection)} does not swallow exceptions.
     */
    private static class ThrowingCollection<E> extends AbstractCollection<E> {

        private final RuntimeException toThrow;

        ThrowingCollection(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public Iterator<E> iterator() {
            // Empty iterator – not used in the tests.
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            // Size is irrelevant because isEmpty() is overridden to throw.
            return 0;
        }

        @Override
        public boolean isEmpty() {
            throw toThrow;
        }
    }

    /**
     * Supplies the collections used by {@link #testIsEmptyVarious}.
     */
    static Stream<Arguments> isEmptyProvider() {
        return Stream.of(// null
        Arguments.of(null, true, "null collection should be treated as empty"), // empty mutable collections
        Arguments.of(new ArrayList<>(), true, "empty ArrayList"), Arguments.of(Collections.emptyList(), true, "Collections.emptyList()"), Arguments.of(Collections.emptySet(), true, "Collections.emptySet()"), Arguments.of(new HashSet<>(), true, "new empty HashSet"), Arguments.of(new LinkedHashSet<>(), true, "new empty LinkedHashSet"), // non‑empty mutable collections
        Arguments.of(Arrays.asList(1, 2, 3), false, "ArrayList with three integers"), Arguments.of(new HashSet<>(Arrays.asList(1, 2)), false, "HashSet with two integers"), Arguments.of(new LinkedHashSet<>(Collections.singleton("x")), false, "LinkedHashSet with one element"), // immutable wrappers around empty collections
        Arguments.of(Collections.unmodifiableList(new ArrayList<>()), true, "unmodifiable view of an empty list"), Arguments.of(Collections.unmodifiableList(Collections.emptyList()), true, "unmodifiable view of Collections.emptyList()"), Arguments.of(Collections.unmodifiableList(Collections.emptyList()), true, "unmodifiable view of an empty list created via List.of()"), Arguments.of(Collections.unmodifiableList(List.of()), true, "unmodifiable view of List.of() (empty)"), Arguments.of(Collections.unmodifiableList(List.of("a")), false, "unmodifiable view of List.of(\"a\") (non‑empty)"), Arguments.of(Collections.unmodifiableSet(Set.of()), true, "unmodifiable view of Set.of() (empty)"), Arguments.of(Collections.unmodifiableSet(Set.of(1, 2)), false, "unmodifiable view of Set.of(1,2) (non‑empty)"), // synchronized wrappers
        Arguments.of(Collections.synchronizedCollection(new ArrayList<>()), true, "synchronized wrapper around an empty list"), Arguments.of(Collections.synchronizedCollection(Arrays.asList(5, 6)), false, "synchronized wrapper around a non‑empty list"), // Java 9+ factory methods
        Arguments.of(List.of(), true, "List.of() – empty immutable list"), Arguments.of(List.of(1), false, "List.of(1) – non‑empty immutable list"), Arguments.of(Set.of(), true, "Set.of() – empty immutable set"), Arguments.of(Set.of(1, 2), false, "Set.of(1,2) – non‑empty immutable set"), // collection containing only null elements (still non‑empty)
        Arguments.of(Arrays.asList(null, null), false, "list containing only null elements should be non‑empty"));
    }

    /**
     * Supplies collections that deliberately throw when {@code isEmpty()} is called.
     */
    static Stream<Arguments> exceptionProvider() {
        return Stream.of(Arguments.of(new ThrowingCollection<>(new UnsupportedOperationException("isEmpty not supported")), UnsupportedOperationException.class, "ThrowingCollection should propagate UnsupportedOperationException"), Arguments.of(new ThrowingCollection<>(new IllegalStateException("illegal state")), IllegalStateException.class, "ThrowingCollection should propagate IllegalStateException"));
    }

    @Test
    @DisplayName("isEmpty should return true for a null collection")
    void testIsEmpty_NullCollection() {
        // given
        List<String> nullList = null;
        // when
        boolean result = CollectionUtilities.isEmpty(nullList);
        // then
        assertTrue(result, "isEmpty should treat null as empty");
    }

    @Test
    @DisplayName("isEmpty should return true for an empty collection")
    void testIsEmpty_EmptyCollection() {
        // given
        List<Integer> emptyList = Collections.emptyList();
        // when
        boolean result = CollectionUtilities.isEmpty(emptyList);
        // then
        assertTrue(result, "isEmpty should return true for an empty collection");
    }

    @Test
    @DisplayName("isEmpty should return false for a collection with elements")
    void testIsEmpty_NonEmptyCollection() {
        // given
        List<String> list = new ArrayList<>();
        list.add("element");
        // when
        boolean result = CollectionUtilities.isEmpty(list);
        // then
        assertFalse(result, "isEmpty should return false when the collection contains elements");
    }

    @Test
    @DisplayName("isEmpty should work with mutable collections that become empty")
    void testIsEmpty_CollectionBecomesEmpty() {
        // given
        List<String> list = new ArrayList<>();
        list.add("temp");
        // now the collection is empty
        list.clear();
        // when
        boolean result = CollectionUtilities.isEmpty(list);
        // then
        assertTrue(result, "isEmpty should reflect the current empty state of the collection");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("isEmptyProvider")
    @DisplayName("isEmpty – various collections")
    void testIsEmptyVarious(Collection<?> col, boolean expected, String description) {
        // when
        boolean result = CollectionUtilities.isEmpty(col);
        // then
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionProvider")
    @DisplayName("isEmpty – exception propagation")
    void testIsEmptyExceptions(Collection<?> col, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CollectionUtilities.isEmpty(col), description);
    }
}
