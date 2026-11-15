package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CollectionUtilities_getEmptyCollection_Test {

    @Test
    void shouldReturnEmptyNavigableSetWhenInputIsNavigableSet() {
        // given: a non‑empty NavigableSet (TreeSet implements NavigableSet)
        NavigableSet<String> input = new TreeSet<>();
        // ensure the input is not already empty
        input.add("one");
        // when
        Collection<String> result = CollectionUtilities.getEmptyCollection(input);
        // then
        assertTrue(result instanceof NavigableSet, "Result should be a NavigableSet");
        assertTrue(result.isEmpty(), "Resulting collection must be empty");
        // emptyNavigableSet is immutable – verify by expecting UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> ((NavigableSet<String>) result).add("test"));
        // also verify that it is the same instance as Collections.emptyNavigableSet()
        assertSame(Collections.emptyNavigableSet(), result);
    }

    @Test
    void shouldReturnEmptySortedSetWhenInputIsSortedSetButNotNavigableSet() {
        // given: a SortedSet (TreeSet also implements NavigableSet, so we use a custom implementation)
        SortedSet<Integer> input = new TreeSet<>() {

            @Override
            public boolean equals(Object o) {
                return super.equals(o);
            }
        };
        input.add(42);
        // when
        Collection<Integer> result = CollectionUtilities.getEmptyCollection(input);
        // then
        assertTrue(result instanceof SortedSet, "Result should be a SortedSet");
        assertTrue(result.isEmpty(), "Resulting collection must be empty");
        assertThrows(UnsupportedOperationException.class, () -> ((SortedSet<Integer>) result).add(99));
        assertSame(Collections.emptySortedSet(), result);
    }

    @Test
    void shouldReturnEmptySetWhenInputIsSetButNotSortedOrNavigable() {
        // given
        Set<Double> input = new HashSet<>();
        input.add(3.14);
        // when
        Collection<Double> result = CollectionUtilities.getEmptyCollection(input);
        // then
        assertTrue(result instanceof Set, "Result should be a Set");
        assertTrue(result.isEmpty(), "Resulting collection must be empty");
        assertThrows(UnsupportedOperationException.class, () -> ((Set<Double>) result).add(2.71));
        assertSame(Collections.emptySet(), result);
    }

    @Test
    void shouldReturnEmptyListWhenInputIsList() {
        // given
        List<String> input = new ArrayList<>();
        input.add("alpha");
        // when
        Collection<String> result = CollectionUtilities.getEmptyCollection(input);
        // then
        assertTrue(result instanceof List, "Result should be a List");
        assertTrue(result.isEmpty(), "Resulting collection must be empty");
        assertThrows(UnsupportedOperationException.class, () -> ((List<String>) result).add("beta"));
        assertSame(Collections.emptyList(), result);
    }

    @Test
    void shouldDefaultToEmptyListForOtherCollectionTypes() {
        // given: a LinkedList (implements List, but we treat it as “other” to hit the fallback)
        Collection<Character> input = new LinkedList<>();
        input.add('x');
        // when
        Collection<Character> result = CollectionUtilities.getEmptyCollection(input);
        // then
        // The implementation falls back to emptyList()
        assertTrue(result instanceof List, "Result should be a List (fallback)");
        assertTrue(result.isEmpty(), "Resulting collection must be empty");
        assertThrows(UnsupportedOperationException.class, () -> ((List<Character>) result).add('y'));
        assertSame(Collections.emptyList(), result);
    }

    @Test
    void shouldThrowNPEWhenCollectionIsNull() {
        // given
        Collection<Object> input = null;
        // when / then
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CollectionUtilities.getEmptyCollection(input));
        assertEquals("Collection must not be null", ex.getMessage());
    }
}
