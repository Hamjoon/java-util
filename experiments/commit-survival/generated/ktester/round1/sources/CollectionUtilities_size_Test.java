package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_size_Test {

    static Stream<Arguments> nullAndNonNullCollectionsProvider() {
        return Stream.of(// null collection
        Arguments.of(null, 0), // empty ArrayList
        Arguments.of(new ArrayList<>(), 0), // list with three integers
        Arguments.of(Arrays.asList(1, 2, 3), 3), // HashSet with two distinct strings
        Arguments.of(new HashSet<>(Arrays.asList("a", "b")), 2), // LinkedHashSet containing a single null element
        Arguments.of(new LinkedHashSet<Object>() {

            {
                add(null);
            }
        }, 1));
    }

    static Stream<Arguments> variousImplementationsProvider() {
        return Stream.of(// LinkedList with four integers
        Arguments.of(new LinkedList<>(Arrays.asList(1, 2, 3, 4)), 4), // Immutable empty set
        Arguments.of(Collections.emptySet(), 0), // Singleton list
        Arguments.of(Collections.singletonList("only"), 1), // Vector with three strings
        Arguments.of(new Vector<>(Arrays.asList("a", "b", "c")), 3), // CopyOnWriteArrayList with five integers
        Arguments.of(new CopyOnWriteArrayList<>(Arrays.asList(1, 2, 3, 4, 5)), 5), // ArrayList with two null elements
        Arguments.of(new ArrayList<>(Arrays.asList(null, null)), 2), // Unmodifiable list view of two elements
        Arguments.of(Collections.unmodifiableList(Arrays.asList(1, 2)), 2), // SubList view (elements 2,3,4) of a five‑element list
        Arguments.of(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)).subList(1, 4), 3));
    }

    static Stream<Arguments> customAndEdgeCasesProvider() {
        // Custom AbstractCollection that pretends to have size 7
        Collection<Integer> customAbstract = new AbstractCollection<Integer>() {

            private final int fakeSize = 7;

            @Override
            public Iterator<Integer> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return fakeSize;
            }
        };
        // Large ArrayList with 1000 copies of "x"
        List<String> largeList = new ArrayList<>();
        largeList.addAll(Collections.nCopies(1000, "x"));
        return Stream.of(Arguments.of(customAbstract, 7), Arguments.of(Collections.emptyList(), 0), Arguments.of(largeList, 1000));
    }

    @Test
    void givenNullCollection_whenSize_thenReturnsZero() {
        // given
        Collection<?> collection = null;
        // when
        int result = CollectionUtilities.size(collection);
        // then
        assertEquals(0, result, "size(null) should return 0");
    }

    @Test
    void givenEmptyArrayList_whenSize_thenReturnsZero() {
        // given
        List<String> list = new ArrayList<>();
        // when
        int result = CollectionUtilities.size(list);
        // then
        assertEquals(0, result);
    }

    @Test
    void givenNonEmptyArrayList_whenSize_thenReturnsCorrectCount() {
        // given
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3));
        // when
        int result = CollectionUtilities.size(list);
        // then
        assertEquals(3, result);
    }

    @Test
    void givenEmptyHashSet_whenSize_thenReturnsZero() {
        // given
        Set<String> set = new HashSet<>();
        // when
        int result = CollectionUtilities.size(set);
        // then
        assertEquals(0, result);
    }

    @Test
    void givenNonEmptyLinkedHashSet_whenSize_thenReturnsCorrectCount() {
        // given
        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        // when
        int result = CollectionUtilities.size(set);
        // then
        assertEquals(4, result);
    }

    @Test
    void givenCollectionWithNullElements_whenSize_thenCountsElements() {
        // given
        List<String> list = new ArrayList<>(Arrays.asList(null, null));
        // when
        int result = CollectionUtilities.size(list);
        // then
        assertEquals(2, result, "size should count elements even if they are null");
    }

    /**
     * Group: size(Collection) - Null and Non-Null Collections
     */
    @ParameterizedTest(name = "{index} => col={0}, expected={1}")
    @MethodSource("nullAndNonNullCollectionsProvider")
    void size_NullAndNonNullCollections(Collection<?> col, int expected) {
        assertEquals(expected, CollectionUtilities.size(col));
    }

    /**
     * Group: size(Collection) - Various Collection Implementations
     */
    @ParameterizedTest(name = "{index} => col={0}, expected={1}")
    @MethodSource("variousImplementationsProvider")
    void size_VariousImplementations(Collection<?> col, int expected) {
        assertEquals(expected, CollectionUtilities.size(col));
    }

    /**
     * Group: size(Collection) - Custom and Edge Cases
     */
    @ParameterizedTest(name = "{index} => col={0}, expected={1}")
    @MethodSource("customAndEdgeCasesProvider")
    void size_CustomAndEdgeCases(Collection<?> col, int expected) {
        assertEquals(expected, CollectionUtilities.size(col));
    }
}
