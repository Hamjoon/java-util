package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_removeAll_Test {

    private ConcurrentList<Integer> concurrentList;

    // used for wrapper‑mode tests
    private List<Integer> backingList;

    /**
     * Collection whose {@code iterator()} method always throws a {@link RuntimeException}.
     */
    private static class FaultyCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            throw new RuntimeException("Iterator failure");
        }

        @Override
        public int size() {
            return 0;
        }
    }

    /**
     * An element whose {@code equals(Object)} method throws a {@link RuntimeException}.
     */
    private static class Troublemaker {

        @Override
        public boolean equals(Object obj) {
            throw new RuntimeException("equals() failure");
        }

        @Override
        public int hashCode() {
            return 42;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh list for each test (stand‑alone mode)
        concurrentList = new ConcurrentList<>();
        // Backing list for wrapper‑mode tests – a spy so we can verify interaction
        backingList = spy(new ArrayList<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse
        concurrentList = null;
        backingList = null;
    }

    @Test
    void removeAll_RemovesExistingElements_ReturnsTrue() {
        // given
        concurrentList.addAll(Arrays.asList(1, 2, 3, 4, 5));
        Collection<Integer> toRemove = Arrays.asList(2, 4);
        // when
        boolean changed = concurrentList.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should return true when the list is modified");
        assertEquals(Arrays.asList(1, 3, 5), concurrentList, "Elements 2 and 4 must be removed");
    }

    @Test
    void removeAll_NoMatchingElements_ReturnsFalse() {
        // given
        concurrentList.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> toRemove = Arrays.asList(4, 5);
        // when
        boolean changed = concurrentList.removeAll(toRemove);
        // then
        assertFalse(changed, "removeAll should return false when nothing is removed");
        assertEquals(Arrays.asList(1, 2, 3), concurrentList, "List content must stay unchanged");
    }

    @Test
    void removeAll_EmptyCollection_ReturnsFalse() {
        // given
        concurrentList.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> empty = Collections.emptyList();
        // when
        boolean changed = concurrentList.removeAll(empty);
        // then
        assertFalse(changed, "removeAll on an empty collection should return false");
        assertEquals(Arrays.asList(1, 2, 3), concurrentList);
    }

    @Test
    void removeAll_NullCollection_ThrowsNullPointerException() {
        // given
        concurrentList.addAll(Arrays.asList(1, 2, 3));
        // when / then
        assertThrows(NullPointerException.class, () -> concurrentList.removeAll(null), "removeAll should throw NPE when the argument collection is null");
    }

    @Test
    void removeAll_WrapperMode_DelegatesToBackingList() {
        // given – wrap the spied backing list
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backingList);
        wrapped.addAll(Arrays.asList(10, 20, 30, 40));
        Collection<Integer> toRemove = Arrays.asList(20, 40);
        // when
        boolean changed = wrapped.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should report that the list changed");
        // Verify that the underlying list's removeAll method was invoked exactly once
        verify(backingList, times(1)).removeAll(toRemove);
        // The spy reflects the real state, so we can also assert the final contents
        assertEquals(Arrays.asList(10, 30), wrapped, "Underlying list must reflect the removals");
    }

    @Test
    void removeAll_EmptyListAndEmptyCollection_ReturnsFalse() {
        // given: both list and collection are empty
        Collection<Integer> empty = Collections.emptyList();
        // when
        boolean changed = concurrentList.removeAll(empty);
        // then
        assertFalse(changed, "removeAll on empty list with empty collection should return false");
        assertTrue(concurrentList.isEmpty(), "List must remain empty");
    }

    @Test
    void removeAll_AllElementsPresent_EmptiesList() {
        // given
        concurrentList.addAll(Arrays.asList("apple", "banana"));
        Collection<String> toRemove = Arrays.asList("apple", "banana");
        // when
        boolean changed = ((ConcurrentList<String>) (Object) concurrentList).removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should return true when all elements are removed");
        assertTrue(((ConcurrentList<String>) (Object) concurrentList).isEmpty(), "List must be empty after removing all elements");
    }

    @Test
    void removeAll_PartialOverlap_RemovesOnlyMatching() {
        // given
        concurrentList.addAll(Arrays.asList("apple", "banana"));
        Collection<String> toRemove = Arrays.asList("banana", "cherry");
        // when
        boolean changed = ((ConcurrentList<String>) (Object) concurrentList).removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should return true when at least one element is removed");
        assertEquals(Arrays.asList("apple"), ((ConcurrentList<String>) (Object) concurrentList), "Only the matching element should be removed");
    }

    @Test
    void removeAll_NoMatchingElements_ListUnchanged() {
        // given
        concurrentList.addAll(Arrays.asList("apple", "banana"));
        Collection<String> toRemove = Arrays.asList("cherry", "date");
        // when
        boolean changed = ((ConcurrentList<String>) (Object) concurrentList).removeAll(toRemove);
        // then
        assertFalse(changed, "removeAll should return false when nothing matches");
        assertEquals(Arrays.asList("apple", "banana"), ((ConcurrentList<String>) (Object) concurrentList), "List must stay unchanged");
    }

    @Test
    void removeAll_SupersetCollection_EmptiesList() {
        // given
        concurrentList.addAll(Arrays.asList("apple", "banana"));
        Collection<String> toRemove = Arrays.asList("apple", "banana", "cherry", "date");
        // when
        boolean changed = ((ConcurrentList<String>) (Object) concurrentList).removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should return true when list elements are removed");
        assertTrue(((ConcurrentList<String>) (Object) concurrentList).isEmpty(), "List must be empty after superset removal");
    }

    @Test
    void removeAll_NullElements_CollectionContainsNull_RemovesNull() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.add(null);
        list.add("apple");
        Collection<String> toRemove = Collections.singletonList(null);
        // when
        boolean changed = list.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should return true when null element is removed");
        assertEquals(Collections.singletonList("apple"), list, "Only the null element should be removed");
    }

    @Test
    void removeAll_NullElements_ListOnlyNull_CollectionHasNullAndOther() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.add(null);
        Collection<String> toRemove = Arrays.asList(null, "apple");
        // when
        boolean changed = list.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should return true when null is removed");
        assertTrue(list.isEmpty(), "List should become empty after removing the sole null");
    }

    @Test
    void removeAll_DuplicateElementsInList_AllOccurrencesRemoved() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList("apple", "apple", "banana"));
        Collection<String> toRemove = Collections.singletonList("apple");
        // when
        boolean changed = list.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should return true when duplicates are removed");
        assertEquals(Collections.singletonList("banana"), list, "All 'apple' occurrences must be removed");
    }

    @Test
    void removeAll_DuplicateElementsInCollection_RemovesOnceButAllOccurrences() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList("apple", "banana"));
        // duplicates in collection
        Collection<String> toRemove = Arrays.asList("apple", "apple");
        // when
        boolean changed = list.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should return true even if collection has duplicates");
        assertEquals(Collections.singletonList("banana"), list, "All 'apple' occurrences must be removed despite duplicate entries in collection");
    }

    @Test
    void removeAll_SelfCollection_ClearsList() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList("apple", "banana"));
        // when
        // self‑reference
        boolean changed = list.removeAll(list);
        // then
        assertTrue(changed, "removeAll(self) should return true when list is cleared");
        assertTrue(list.isEmpty(), "List must be empty after removing itself");
    }

    @Test
    void removeAll_EmptyListWithNonEmptyCollection_ReturnsFalse() {
        // given: list is empty, collection has elements
        Collection<Integer> toRemove = Collections.singletonList(1);
        // when
        boolean changed = concurrentList.removeAll(toRemove);
        // then
        assertFalse(changed, "removeAll should return false when list is empty");
        assertTrue(concurrentList.isEmpty(), "List must remain empty");
    }

    @Test
    void removeAll_CollectionWithDifferentElementType_NoRemoval() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList("1", "2"));
        // different type
        Collection<Integer> toRemove = Arrays.asList(1, 2);
        // when
        boolean changed = list.removeAll(toRemove);
        // then
        assertFalse(changed, "removeAll should return false when types do not match");
        assertEquals(Arrays.asList("1", "2"), list, "List must stay unchanged");
    }

    @Test
    void removeAll_CollectionWithoutNull_ListContainsNull_OnlyNonNullRemoved() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(null, "apple"));
        Collection<String> toRemove = Collections.singletonList("apple");
        // when
        boolean changed = list.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should return true when a non‑null element is removed");
        assertEquals(Collections.singletonList(null), list, "Null element must remain in the list");
    }

    @Test
    void removeAll_UnmodifiableCollectionArgument_Succeeds() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList("apple", "banana"));
        Collection<String> toRemove = Collections.unmodifiableList(Collections.singletonList("banana"));
        // when
        boolean changed = list.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should work with an unmodifiable collection argument");
        assertEquals(Collections.singletonList("apple"), list, "Only 'banana' should be removed");
    }

    @Test
    void removeAll_UnderlyingListUnmodifiable_PropagatesException() {
        // given: wrap an unmodifiable list
        List<String> unmodifiable = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("apple")));
        ConcurrentList<String> wrapped = new ConcurrentList<>(unmodifiable);
        Collection<String> toRemove = Collections.singletonList("apple");
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> wrapped.removeAll(toRemove), "removeAll should propagate UnsupportedOperationException from the backing list");
    }

    @Test
    void removeAll_CollectionIteratorThrowsException_PropagatesRuntimeException() {
        // given a collection whose iterator() throws
        Collection<String> faulty = new FaultyCollection<>();
        ConcurrentList<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList("a", "b"));
        // when / then
        assertThrows(RuntimeException.class, () -> list.removeAll(faulty), "removeAll must propagate exception thrown by collection iterator");
    }

    @Test
    void removeAll_ElementEqualsThrowsException_PropagatesRuntimeException() {
        // given a list containing a troublesome element
        Troublemaker troublesome = new Troublemaker();
        ConcurrentList<Object> list = new ConcurrentList<>();
        list.add(troublesome);
        list.add("good");
        Collection<Object> toRemove = Collections.singletonList("any");
        // when / then
        assertThrows(RuntimeException.class, () -> list.removeAll(toRemove), "removeAll must propagate exception thrown from element.equals()");
    }
}
