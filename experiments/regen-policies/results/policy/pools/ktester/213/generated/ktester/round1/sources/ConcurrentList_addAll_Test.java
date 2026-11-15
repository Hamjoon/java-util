package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_addAll_Test {

    // mocked List used as a wrapper
    private List<Integer> mockBackingList;

    // ConcurrentList that wraps the mock
    private ConcurrentList<Integer> wrapperList;

    // a real ConcurrentList (stand‑alone mode)
    private ConcurrentList<Integer> realList;

    /**
     * A collection whose iterator's {@code next()} method always throws a {@link RuntimeException}.
     */
    private static class ExceptionThrowingCollection<E> implements Collection<E> {

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
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
                    return true;
                }

                @Override
                public E next() {
                    throw new RuntimeException("Iterator next() failure");
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
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
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for each test so interactions are isolated
        mockBackingList = mock(List.class);
        wrapperList = new ConcurrentList<>(mockBackingList);
        // Real list for functional verification (no mocking)
        realList = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test tidy
        mockBackingList = null;
        wrapperList = null;
        realList = null;
    }

    @Test
    void addAll_delegatesToBackingList_andReturnsResult() {
        // GIVEN
        Collection<Integer> toAdd = Arrays.asList(1, 2, 3);
        // Simulate the backing list returning true
        when(mockBackingList.addAll(toAdd)).thenReturn(true);
        // WHEN
        boolean result = wrapperList.addAll(toAdd);
        // THEN
        assertTrue(result, "addAll should return the value supplied by the backing list");
        verify(mockBackingList, times(1)).addAll(toAdd);
    }

    @Test
    void addAll_delegatesAndReturnsFalseWhenBackingListReturnsFalse() {
        // GIVEN
        Collection<Integer> toAdd = Collections.singletonList(42);
        when(mockBackingList.addAll(toAdd)).thenReturn(false);
        // WHEN
        boolean result = wrapperList.addAll(toAdd);
        // THEN
        assertFalse(result, "addAll should propagate false result from the backing list");
        verify(mockBackingList, times(1)).addAll(toAdd);
    }

    @Test
    void addAll_nullCollection_throwsNullPointerException() {
        // GIVEN
        Collection<Integer> nullCollection = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> wrapperList.addAll(nullCollection), "addAll should throw NPE when the supplied collection is null");
        // No interaction with the backing list should have occurred
        verifyNoInteractions(mockBackingList);
    }

    @Test
    void addAll_standaloneMode_addsElementsAndUpdatesSize() {
        // GIVEN
        List<Integer> toAdd = Arrays.asList(10, 20, 30);
        // WHEN
        boolean changed = realList.addAll(toAdd);
        // THEN
        assertTrue(changed, "addAll should return true when the list is modified");
        assertEquals(3, realList.size(), "Size should reflect number of added elements");
        assertEquals(toAdd, realList, "List contents should match the added collection");
    }

    @Test
    void addAll_standaloneMode_withEmptyCollection_returnsFalseAndLeavesListUnchanged() {
        // GIVEN
        List<Integer> empty = Collections.emptyList();
        // WHEN
        boolean changed = realList.addAll(empty);
        // THEN
        assertFalse(changed, "addAll should return false when adding an empty collection");
        assertTrue(realList.isEmpty(), "List should remain empty after adding an empty collection");
    }

    @Test
    void addAll_emptyCollection_returnsFalseAndLeavesListUnchanged() {
        // GIVEN
        List<Integer> empty = Collections.emptyList();
        // WHEN
        boolean changed = realList.addAll(empty);
        // THEN
        assertFalse(changed);
        assertTrue(realList.isEmpty());
    }

    @Test
    void addAll_nonEmptyCollection_returnsTrueAndPreservesOrder() {
        // GIVEN
        List<Integer> toAdd = Arrays.asList(1, 2, 3);
        // WHEN
        boolean changed = realList.addAll(toAdd);
        // THEN
        assertTrue(changed);
        assertEquals(toAdd, realList);
    }

    @Test
    void addAll_collectionWithNullElements_isAllowedWhenBackingListSupportsNull() {
        // GIVEN
        List<Integer> toAdd = Arrays.asList(null, null);
        // WHEN
        boolean changed = realList.addAll(toAdd);
        // THEN
        assertTrue(changed);
        assertEquals(2, realList.size());
        assertEquals(toAdd, realList);
    }

    @Test
    void addAll_addingToNonEmptyList_appendsElements() {
        // GIVEN
        realList.addAll(Arrays.asList(1, 2, 3));
        List<Integer> toAdd = Arrays.asList(4, 5);
        // WHEN
        boolean changed = realList.addAll(toAdd);
        // THEN
        assertTrue(changed);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), realList);
    }

    @Test
    void addAll_collectionWithDuplicateElements_preservesDuplicates() {
        // GIVEN
        List<Integer> toAdd = Arrays.asList(1, 1, 2);
        // WHEN
        boolean changed = realList.addAll(toAdd);
        // THEN
        assertTrue(changed);
        assertEquals(toAdd, realList);
    }

    @Test
    void addAll_mixedNullAndNonNullElements_preservesPositions() {
        // GIVEN
        List<Integer> toAdd = Arrays.asList(null, 7, null);
        // WHEN
        boolean changed = realList.addAll(toAdd);
        // THEN
        assertTrue(changed);
        assertEquals(toAdd, realList);
    }

    @Test
    void addAll_setImplementation_addsAllElementsInIterationOrder() {
        // GIVEN
        // LinkedHashSet preserves insertion order
        Set<Integer> set = new LinkedHashSet<>(Arrays.asList(9, 8, 7));
        // WHEN
        boolean changed = realList.addAll(set);
        // THEN
        assertTrue(changed);
        assertEquals(3, realList.size());
        assertEquals(Arrays.asList(9, 8, 7), realList);
    }

    @Test
    void addAll_selfAddition_duplicatesCurrentContents() {
        // GIVEN
        realList.addAll(Arrays.asList(1, 2, 3));
        // WHEN
        boolean changed = realList.addAll(realList);
        // THEN
        assertTrue(changed);
        assertEquals(6, realList.size());
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), realList);
    }

    @Test
    void addAll_underlyingUnmodifiableList_propagatesUnsupportedOperationException() {
        // GIVEN: an unmodifiable backing list
        List<Integer> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(unmodifiable);
        Collection<Integer> toAdd = Arrays.asList(1, 2, 3);
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, () -> wrapped.addAll(toAdd), "addAll should propagate UnsupportedOperationException from an unmodifiable backing list");
    }

    @Test
    void addAll_collectionIteratorThrowsRuntimeException_propagatesException() {
        // GIVEN
        Collection<Integer> badCollection = new ExceptionThrowingCollection<>();
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> realList.addAll(badCollection), "addAll should propagate RuntimeException thrown by the supplied collection");
        assertEquals("Iterator next() failure", ex.getMessage());
    }

    @Test
    void addAll_backingListRejectsNullElements_propagatesNullPointerException() {
        // GIVEN: mock that throws NPE when collection contains a null
        Collection<Integer> withNull = Arrays.asList(null);
        when(mockBackingList.addAll(argThat(col -> col.contains(null)))).thenThrow(new NullPointerException("null not allowed"));
        // WHEN / THEN
        NullPointerException ex = assertThrows(NullPointerException.class, () -> wrapperList.addAll(withNull), "addAll should propagate NPE from backing list when null elements are rejected");
        assertEquals("null not allowed", ex.getMessage());
        verify(mockBackingList, times(1)).addAll(withNull);
    }
}
