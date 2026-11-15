package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_removeAll_Test {

    @Test
    void removeAll_RemovesExistingElements_ReturnsTrue() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3, 4));
        // when
        boolean modified = set.removeAll(Arrays.asList(2, 4));
        // then
        assertTrue(modified, "removeAll should return true when at least one element is removed");
        assertEquals(2, set.size(), "Set should contain only the non‑removed elements");
        assertTrue(set.containsAll(Arrays.asList(1, 3)), "Remaining elements should be 1 and 3");
        assertFalse(set.contains(2), "Element 2 should have been removed");
        assertFalse(set.contains(4), "Element 4 should have been removed");
    }

    @Test
    void removeAll_NoMatchingElements_ReturnsFalse() {
        // given
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        // when
        boolean modified = set.removeAll(Arrays.asList("x", "y"));
        // then
        assertFalse(modified, "removeAll should return false when nothing is removed");
        assertEquals(3, set.size(), "Set size must stay unchanged");
        assertTrue(set.containsAll(Arrays.asList("a", "b", "c")), "All original elements must remain");
    }

    @Test
    void removeAll_EmptyCollection_ReturnsFalseAndLeavesSetUnchanged() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(10, 20));
        // when
        boolean modified = set.removeAll(Collections.emptyList());
        // then
        assertFalse(modified, "removeAll on an empty collection should return false");
        assertEquals(2, set.size(), "Set size must stay unchanged");
        assertTrue(set.containsAll(Arrays.asList(10, 20)), "Original elements must still be present");
    }

    @Test
    void removeAll_CollectionContainsNull_HandlesNullCorrectly() {
        // given
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("x", null, "y"));
        // when
        boolean modified = set.removeAll(Arrays.asList(null, "z"));
        // then
        assertTrue(modified, "removeAll should return true when null is removed");
        assertEquals(2, set.size(), "Set should contain the two non‑null elements");
        assertTrue(set.containsAll(Arrays.asList("x", "y")), "Remaining elements should be 'x' and 'y'");
        assertFalse(set.contains(null), "Null should have been removed");
    }

    @Test
    void removeAll_IsIdempotent() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(5, 6, 7));
        // when – first removal
        boolean firstCall = set.removeAll(Arrays.asList(5, 6));
        // and – second removal of the same collection
        boolean secondCall = set.removeAll(Arrays.asList(5, 6));
        // then
        assertTrue(firstCall, "First call should report modification");
        assertFalse(secondCall, "Second call should report no modification because elements are already gone");
        assertEquals(1, set.size(), "Only element 7 should remain");
        assertTrue(set.contains(7), "Set must still contain element 7");
    }

    @Test
    void removeAll_SingleMatchingElement() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        // when
        boolean modified = set.removeAll(Arrays.asList(2));
        // then
        assertTrue(modified, "Should return true when a single matching element is removed");
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList(1, 3)));
        assertFalse(set.contains(2));
    }

    @Test
    void removeAll_MultipleElementsMixedPresence() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        // when
        boolean modified = set.removeAll(Arrays.asList(2, 4, 5, 3));
        // then
        assertTrue(modified, "Should return true when at least one element is removed");
        assertEquals(1, set.size());
        assertTrue(set.contains(1));
        assertFalse(set.contains(2));
        assertFalse(set.contains(3));
    }

    @Test
    void removeAll_AllElementsPresent() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        // when
        boolean modified = set.removeAll(Arrays.asList(1, 2, 3));
        // then
        assertTrue(modified, "Should return true when all elements are removed");
        assertTrue(set.isEmpty(), "Set should be empty after removing all elements");
    }

    @Test
    void removeAll_DuplicateElementsInCollection() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        // when
        boolean modified = set.removeAll(Arrays.asList(2, 2, 2));
        // then
        assertTrue(modified, "Should return true because at least one removal succeeded");
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList(1, 3)));
        assertFalse(set.contains(2));
    }

    @Test
    void removeAll_NullCollectionArgument_ThrowsNPE() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        // when / then
        assertThrows(NullPointerException.class, () -> set.removeAll(null), "Passing null as the collection argument must throw NullPointerException");
    }

    @Test
    void removeAll_SetEmptyNonEmptyCollection() {
        // given
        // empty set
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        // when
        boolean modified = set.removeAll(Arrays.asList(1, 2, 3));
        // then
        assertFalse(modified, "Should return false because nothing can be removed from an empty set");
        assertTrue(set.isEmpty(), "Set should remain empty");
    }

    @Test
    void removeAll_CollectionContainsNull_SetDoesNotContainNull() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        // when
        boolean modified = set.removeAll(Arrays.asList((Object) null));
        // then
        assertFalse(modified, "Should return false because null is not present in the set");
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    void removeAll_CollectionContainsNullAndExistingElement() {
        // given
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, null, 2));
        // when
        boolean modified = set.removeAll(Arrays.asList((Object) null, 2));
        // then
        assertTrue(modified, "Should return true because at least one element (null or 2) is removed");
        assertEquals(1, set.size());
        assertTrue(set.contains(1));
        assertFalse(set.contains(null));
        assertFalse(set.contains(2));
    }

    @Test
    void removeAll_SelfRemoval() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        // when: pass the set itself as the collection argument
        boolean modified = set.removeAll(set);
        // then
        assertTrue(modified, "Self‑removal should return true when the set was non‑empty");
        assertTrue(set.isEmpty(), "Set must be empty after removing all its own elements");
    }

    @Test
    void removeAll_HashSetAllElementsPresent() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        Set<Integer> other = new HashSet<>(Arrays.asList(1, 2, 3));
        // when
        boolean modified = set.removeAll(other);
        // then
        assertTrue(modified, "Should return true when all elements are removed via a HashSet");
        assertTrue(set.isEmpty(), "Set should be empty after removal");
    }

    @Test
    void removeAll_IncompatibleTypeElements() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        // when
        boolean modified = set.removeAll(Arrays.asList("a", "b"));
        // then
        assertFalse(modified, "Should return false because no element matches the set's type");
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    void removeAll_UnmodifiableCollectionArgument() {
        // given
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        List<Integer> unmodifiable = Collections.unmodifiableList(Arrays.asList(2, 3));
        // when
        boolean modified = set.removeAll(unmodifiable);
        // then
        assertTrue(modified, "Should return true because elements are removed even though the collection is unmodifiable");
        assertEquals(1, set.size());
        assertTrue(set.contains(1));
        assertFalse(set.contains(2));
        assertFalse(set.contains(3));
    }
}
