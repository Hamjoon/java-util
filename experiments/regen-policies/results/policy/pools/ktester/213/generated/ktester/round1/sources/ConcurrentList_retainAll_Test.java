package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_retainAll_Test {

    private ConcurrentList<Integer> standaloneList;

    private ConcurrentList<Integer> wrapperList;

    @Mock
    private List<Integer> mockBackingList;

    /**
     * Simple {@link ArrayList} subclass that rejects {@code null} elements
     * in {@link #retainAll(Collection)} by throwing {@link NullPointerException}.
     */
    private static class NoNullList<E> extends ArrayList<E> {

        NoNullList(Collection<? extends E> c) {
            super(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            if (c.contains(null)) {
                throw new NullPointerException("null not allowed in retainAll collection");
            }
            return super.retainAll(c);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stand‑alone list (uses its own internal ArrayList)
        standaloneList = new ConcurrentList<>();
        // Wrapper list (delegates to a mocked backing list)
        wrapperList = new ConcurrentList<>(mockBackingList);
    }

    @Test
    void retainAll_RemovesElementsNotInCollection_ReturnsTrue() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3, 4, 5));
        Collection<Integer> toKeep = Arrays.asList(2, 4);
        // when
        boolean changed = standaloneList.retainAll(toKeep);
        // then
        assertTrue(changed, "retainAll should report that the list was modified");
        assertEquals(Arrays.asList(2, 4), new ArrayList<>(standaloneList), "Only the elements present in the collection should remain");
    }

    @Test
    void retainAll_NoChange_ReturnsFalse() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> superset = Arrays.asList(1, 2, 3, 4, 5);
        // when
        boolean changed = standaloneList.retainAll(superset);
        // then
        assertFalse(changed, "retainAll should report false when the list is not modified");
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(standaloneList), "The original list should stay unchanged");
    }

    @Test
    void retainAll_NullCollection_ThrowsNullPointerException() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3));
        // when / then
        assertThrows(NullPointerException.class, () -> standaloneList.retainAll(null), "retainAll should throw NPE when the argument collection is null");
    }

    @Test
    void retainAll_DelegatesToBackingList_WhenWrapped() {
        // given
        Collection<Integer> collection = Arrays.asList(10, 20);
        when(mockBackingList.retainAll(collection)).thenReturn(true);
        // when
        boolean result = wrapperList.retainAll(collection);
        // then
        assertTrue(result, "Result should be whatever the backing list returned");
        verify(mockBackingList, times(1)).retainAll(collection);
    }

    @Test
    void retainAll_DelegatesResultWhenWrapped_ReturnsFalse() {
        // given
        Collection<Integer> collection = Collections.singletonList(99);
        when(mockBackingList.retainAll(collection)).thenReturn(false);
        // when
        boolean result = wrapperList.retainAll(collection);
        // then
        assertFalse(result, "Result should reflect the backing list's return value");
        verify(mockBackingList, times(1)).retainAll(collection);
    }

    @Test
    void retainAll_RetainSubset_ReturnsTrueAndKeepsSubset() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> subset = Arrays.asList(1, 2);
        // when
        boolean changed = standaloneList.retainAll(subset);
        // then
        assertTrue(changed);
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(standaloneList));
    }

    @Test
    void retainAll_CollectionContainsAllElements_ReturnsFalse() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> all = Arrays.asList(1, 2, 3);
        // when
        boolean changed = standaloneList.retainAll(all);
        // then
        assertFalse(changed);
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(standaloneList));
    }

    @Test
    void retainAll_EmptyCollectionClearsList_ReturnsTrue() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> empty = Collections.emptyList();
        // when
        boolean changed = standaloneList.retainAll(empty);
        // then
        assertTrue(changed);
        assertTrue(standaloneList.isEmpty());
    }

    @Test
    void retainAll_NoMatchingElementsClearsList_ReturnsTrue() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> none = Arrays.asList(4, 5);
        // when
        boolean changed = standaloneList.retainAll(none);
        // then
        assertTrue(changed);
        assertTrue(standaloneList.isEmpty());
    }

    @Test
    void retainAll_EmptyListWithAnyCollection_ReturnsFalse() {
        // given: list is already empty
        Collection<Integer> any = Arrays.asList(1, 2);
        // when
        boolean changed = standaloneList.retainAll(any);
        // then
        assertFalse(changed);
        assertTrue(standaloneList.isEmpty());
    }

    @Test
    void retainAll_EmptyListWithEmptyCollection_ReturnsFalse() {
        // given: list empty, collection empty
        Collection<Integer> empty = Collections.emptyList();
        // when
        boolean changed = standaloneList.retainAll(empty);
        // then
        assertFalse(changed);
        assertTrue(standaloneList.isEmpty());
    }

    @Test
    void retainAll_DuplicateElementsPreservedWhenSubset() {
        // given
        standaloneList.addAll(Arrays.asList(1, 1, 2, 2, 3, 3));
        Collection<Integer> subset = Arrays.asList(1, 2);
        // when
        boolean changed = standaloneList.retainAll(subset);
        // then
        assertTrue(changed);
        assertEquals(Arrays.asList(1, 1, 2, 2), new ArrayList<>(standaloneList));
    }

    @Test
    void retainAll_ListContainsNulls_CollectionIncludesNull_KeepsNulls() {
        // given
        standaloneList.addAll(Arrays.asList(null, null, 1, 2));
        Collection<Integer> coll = Arrays.asList(null, 2);
        // when
        boolean changed = standaloneList.retainAll(coll);
        // then
        assertTrue(changed);
        assertEquals(Arrays.asList(null, null, 2), new ArrayList<>(standaloneList));
    }

    @Test
    void retainAll_ListContainsNulls_CollectionExcludesNull_RemovesNulls() {
        // given
        standaloneList.addAll(Arrays.asList(null, null, 1, 2));
        Collection<Integer> coll = Arrays.asList(1, 2);
        // when
        boolean changed = standaloneList.retainAll(coll);
        // then
        assertTrue(changed);
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(standaloneList));
    }

    @Test
    void retainAll_SelfRetain_NoModification_ReturnsFalse() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3));
        // Using the list itself as the collection
        Collection<Integer> self = (Collection<Integer>) (Object) standaloneList;
        // when
        boolean changed = standaloneList.retainAll(self);
        // then
        assertFalse(changed);
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(standaloneList));
    }

    @Test
    void retainAll_SetCollection_KeepsOnlyMatchingElements() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3, 4));
        Set<Integer> set = new HashSet<>(Arrays.asList(2, 3));
        // when
        boolean changed = standaloneList.retainAll(set);
        // then
        assertTrue(changed);
        assertEquals(Arrays.asList(2, 3), new ArrayList<>(standaloneList));
    }

    @Test
    void retainAll_MixedMatchingAndNonMatching_ReturnsTrueAndKeepsMatches() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> coll = Arrays.asList(2, 4);
        // when
        boolean changed = standaloneList.retainAll(coll);
        // then
        assertTrue(changed);
        assertEquals(Collections.singletonList(2), new ArrayList<>(standaloneList));
    }

    @Test
    void retainAll_SubListView_AsCollection_KeepsElementsFromSubList() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // [2,3]
        List<Integer> sub = standaloneList.subList(1, 3);
        // when
        boolean changed = standaloneList.retainAll(sub);
        // then
        assertTrue(changed);
        assertEquals(Arrays.asList(2, 3), new ArrayList<>(standaloneList));
    }

    @Test
    void retainAll_CollectionWithDuplicatesContainingAllElements_NoChange() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> dup = Arrays.asList(1, 1, 2, 2, 3, 3);
        // when
        boolean changed = standaloneList.retainAll(dup);
        // then
        assertFalse(changed);
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(standaloneList));
    }

    @Test
    void retainAll_UnmodifiableCollection_KeepsSubset() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3, 4));
        Set<Integer> mutable = new HashSet<>(Arrays.asList(1, 3));
        Set<Integer> unmodifiable = Collections.unmodifiableSet(mutable);
        // when
        boolean changed = standaloneList.retainAll(unmodifiable);
        // then
        assertTrue(changed);
        assertEquals(Arrays.asList(1, 3), new ArrayList<>(standaloneList));
    }

    @Test
    void retainAll_IncompatibleElementTypes_ThrowsClassCastException() {
        // given
        standaloneList.addAll(Arrays.asList(1, 2, 3));
        Collection<String> strings = Arrays.asList("a", "b");
        // when / then
        assertThrows(ClassCastException.class, () -> standaloneList.retainAll(strings), "retainAll should propagate ClassCastException from the underlying list");
    }

    @Test
    void retainAll_UnmodifiableBackingList_ThrowsUnsupportedOperationException() {
        // given: wrap an unmodifiable list
        List<Integer> unmodifiable = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(1, 2, 3)));
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(unmodifiable);
        Collection<Integer> coll = Arrays.asList(1, 2);
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> wrapped.retainAll(coll), "Attempting to modify an unmodifiable backing list must throw UOE");
    }

    @Test
    void retainAll_BackingListDisallowsNull_ThrowsNullPointerException() {
        // given: a custom backing list that throws NPE when null is present in the argument collection
        List<Integer> noNullList = new NoNullList<>(Arrays.asList(1, 2, 3));
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(noNullList);
        Collection<Integer> collWithNull = Collections.singletonList(null);
        // when / then
        assertThrows(NullPointerException.class, () -> wrapped.retainAll(collWithNull), "Backing list that rejects nulls should cause NPE to propagate");
    }
}
