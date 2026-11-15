package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_removeAll_Test {

    /**
     * Instance under test – uses the default no‑arg constructor **
     */
    private ConcurrentList<Integer> list;

    /**
     * Instance under test – wraps a mocked List to verify delegation **
     */
    @Mock
    private List<Integer> mockedDelegate;

    private ConcurrentList<Integer> listWithMock;

    // ---------- Collection iterator throws ConcurrentModificationException ----------
    private static class CmeThrowingCollection<E> implements Collection<E> {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    throw new ConcurrentModificationException();
                }

                @Override
                public E next() {
                    throw new ConcurrentModificationException();
                }
            };
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
        public boolean add(E e) {
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
        public boolean addAll(Collection<? extends E> c) {
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
    }

    // ---------- Collection element equals throws RuntimeException ----------
    private static class FaultyEquals {

        @Override
        public boolean equals(Object obj) {
            throw new RuntimeException("Faulty equals");
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh list for each test case.
        list = new ConcurrentList<>();
        // Initialise the list that wraps a mocked delegate.
        // Mockito creates the mock before each test (via @Mock annotation).
        listWithMock = new ConcurrentList<>(mockedDelegate);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps GC in long test suites.
        list = null;
        listWithMock = null;
    }

    // ---------- Parameterized tests for the “basic functionality” group ----------
    static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// underlying ["a","b","c"], remove ["a","b"] -> true, result ["c"]
        Arguments.of(Arrays.asList("a", "b", "c"), Arrays.asList("a", "b"), true, Arrays.asList("c")), // underlying ["a","b","c"], remove ["x","y"] -> false, unchanged
        Arguments.of(Arrays.asList("a", "b", "c"), Arrays.asList("x", "y"), false, Arrays.asList("a", "b", "c")), // underlying ["a","b","c"], remove [] -> false, unchanged
        Arguments.of(Arrays.asList("a", "b", "c"), Collections.emptyList(), false, Arrays.asList("a", "b", "c")), // underlying [null,"a"], remove [null] -> true, result ["a"]
        Arguments.of(Arrays.asList(null, "a"), Collections.singletonList(null), true, Collections.singletonList("a")), // underlying ["a","a","b"], remove ["a","a"] -> true, result ["b"]
        Arguments.of(Arrays.asList("a", "a", "b"), Arrays.asList("a", "a"), true, Collections.singletonList("b")));
    }

    /* ---------------------------------------------------------------------- */
    @Test
    void removeAll_RemovesExistingElements_ReturnsTrue() {
        // given
        list.addAll(Arrays.asList("1", "2", "3", "4", "5"));
        // when
        boolean changed = list.removeAll(Arrays.asList("2", "4"));
        // then
        assertTrue(changed, "removeAll should return true when the list is modified");
        assertEquals(Arrays.asList("1", "3", "5"), list, "Elements 2 and 4 must be removed");
    }

    @Test
    void removeAll_NoMatchingElements_ReturnsFalse() {
        // given
        list.addAll(Arrays.asList("10", "20", "30"));
        // when
        boolean changed = list.removeAll(Arrays.asList("1", "2", "3"));
        // then
        assertFalse(changed, "removeAll should return false when nothing is removed");
        assertEquals(Arrays.asList("10", "20", "30"), list, "Original list must stay unchanged");
    }

    @Test
    void removeAll_EmptyCollection_ReturnsFalseAndLeavesListIntact() {
        // given
        list.addAll(Arrays.asList("7", "8", "9"));
        // when
        boolean changed = list.removeAll(Collections.emptyList());
        // then
        assertFalse(changed, "removeAll on an empty collection should return false");
        assertEquals(Arrays.asList("7", "8", "9"), list, "List must remain unchanged");
    }

    @Test
    void removeAll_NullCollection_ThrowsNPE() {
        // given
        list.addAll(Arrays.asList("1", "2", "3"));
        // when / then
        assertThrows(NullPointerException.class, () -> list.removeAll(null), "removeAll should propagate NullPointerException when argument is null");
    }

    @Test
    void removeAll_DelegatesToUnderlyingList_WhenWrapped() {
        // given
        Collection<String> toRemove = Arrays.asList("5", "6");
        when(mockedDelegate.removeAll(toRemove)).thenReturn(true);
        // when
        boolean result = listWithMock.removeAll(toRemove);
        // then
        assertTrue(result, "Result should reflect the delegate's return value");
        verify(mockedDelegate, times(1)).removeAll(toRemove);
    }

    @Test
    void removeAll_ConstructorRejectsNullDelegate() {
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new ConcurrentList<Integer>(null), "Constructor must reject a null backing list");
        assertEquals("List cannot be null", ex.getMessage());
    }

    @ParameterizedTest(name = "[{index}] removeAll basic – expectedChanged={2}")
    @MethodSource("basicFunctionalityProvider")
    void removeAll_BasicFunctionality(List<String> initial, Collection<String> toRemove, boolean expectedChanged, List<String> expectedResult) {
        // given
        list.addAll(initial);
        // when
        boolean changed = list.removeAll(toRemove);
        // then
        assertEquals(expectedChanged, changed, "removeAll should return " + expectedChanged);
        assertEquals(expectedResult, list, "List content after removeAll does not match expectation");
    }

    // ---------- Empty underlying list ----------
    @Test
    void removeAll_EmptyUnderlyingList_NonEmptyCollection_ReturnsFalse() {
        // list is empty by default
        boolean changed = list.removeAll(Arrays.asList("a", "b"));
        assertFalse(changed, "Removing from an empty list must return false");
        assertTrue(list.isEmpty(), "List should still be empty");
    }

    @Test
    void removeAll_EmptyUnderlyingList_EmptyCollection_ReturnsFalse() {
        boolean changed = list.removeAll(Collections.emptyList());
        assertFalse(changed, "Removing empty collection from empty list must return false");
        assertTrue(list.isEmpty(), "List should still be empty");
    }

    // ---------- Collection with null mixed with other elements ----------
    @Test
    void removeAll_CollectionWithNullMixed_RemovesOnlyNull() {
        list.addAll(Arrays.asList(null, "a", "b"));
        boolean changed = list.removeAll(Arrays.asList(null, "x"));
        assertTrue(changed, "Method should return true because null was removed");
        assertEquals(Arrays.asList("a", "b"), list, "Only the null element should be removed");
    }

    // ---------- Collection larger than list with some matches ----------
    @Test
    void removeAll_CollectionLargerThanList_WithMatches_EmptiesList() {
        list.addAll(Arrays.asList("a", "b"));
        boolean changed = list.removeAll(Arrays.asList("a", "b", "c", "d", "e"));
        assertTrue(changed, "Should return true because elements were removed");
        assertTrue(list.isEmpty(), "List should be empty after removing all matching elements");
    }

    // ---------- Collection is the same instance as the underlying list ----------
    @Test
    void removeAll_CollectionSameInstance_ClearsList() {
        list.addAll(Arrays.asList("a", "b"));
        // 'self' means we pass the list itself as the collection argument
        @SuppressWarnings("unchecked")
        Collection<String> self = (Collection<String>) list;
        boolean changed = list.removeAll(self);
        assertTrue(changed, "removeAll(self) should return true when list is cleared");
        assertTrue(list.isEmpty(), "List must be empty after removing itself");
    }

    // ---------- Collection with non‑matching types ----------
    @Test
    void removeAll_CollectionWithDifferentTypes_NoMatch() {
        list.addAll(Arrays.asList("a", "b"));
        // Using raw Collection to hold heterogeneous types
        Collection<?> mixed = Arrays.asList(123, true);
        boolean changed = list.removeAll(mixed);
        assertFalse(changed, "No element matches, should return false");
        assertEquals(Arrays.asList("a", "b"), list, "List must stay unchanged");
    }

    // ---------- Underlying list unmodifiable ----------
    @Test
    void removeAll_UnmodifiableUnderlyingList_PropagatesException() {
        List<String> backing = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("a", "b")));
        ConcurrentList<String> unmodifiable = new ConcurrentList<>(backing);
        assertThrows(UnsupportedOperationException.class, () -> unmodifiable.removeAll(Arrays.asList("a", "b")), "removeAll should propagate UnsupportedOperationException from delegate");
    }

    @Test
    void removeAll_CollectionIteratorThrowsCME_Propagates() {
        CmeThrowingCollection<String> cmeCollection = new CmeThrowingCollection<>();
        assertThrows(ConcurrentModificationException.class, () -> list.removeAll(cmeCollection), "removeAll must propagate ConcurrentModificationException from the argument collection");
    }

    @Test
    void removeAll_ElementEqualsThrowsRuntimeException_Propagates() {
        list.addAll(Arrays.asList("a", "b"));
        Collection<Object> faulty = Collections.singletonList(new FaultyEquals());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> list.removeAll(faulty), "removeAll should propagate RuntimeException thrown by equals");
        assertEquals("Faulty equals", ex.getMessage());
    }
}
