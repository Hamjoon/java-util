package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

class CaseInsensitiveSet_removeAll_Test {

    private CaseInsensitiveSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test case.
        set = new CaseInsensitiveSet<>();
        set.add("Apple");
        set.add("Banana");
        set.add("Cherry");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to help GC; not strictly required but keeps the pattern consistent.
        set.clear();
    }

    private static Stream<Arguments> provideElementsCauseSizeChange() {
        return Stream.of(Arguments.of(List.of("Apple", "Banana"), true));
    }

    private static Stream<Arguments> provideNoElementsRemoved() {
        return Stream.of(Arguments.of(List.of("Durian", "Elderberry"), false));
    }

    @Test
    void removeAll_RemovesExistingElements_ReturnsTrueAndUpdatesSize() {
        // given
        List<String> toRemove = new ArrayList<>();
        // different case – should be removed because of case‑insensitivity
        toRemove.add("banana");
        // not present in the set
        toRemove.add("Durian");
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should report that the set was modified");
        assertEquals(2, set.size(), "Set size should be reduced by one");
        assertFalse(set.contains("Banana"), "Element 'Banana' should have been removed");
        assertTrue(set.contains("Apple"), "Other elements must remain untouched");
        assertTrue(set.contains("Cherry"));
    }

    @Test
    void removeAll_NoMatchingElements_ReturnsFalseAndLeavesSetUnchanged() {
        // given
        Collection<String> toRemove = List.of("Durian", "Elderberry");
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertFalse(changed, "removeAll should report no modification when nothing matches");
        assertEquals(3, set.size(), "Set size must stay the same");
        assertTrue(set.containsAll(List.of("Apple", "Banana", "Cherry")), "Original elements must still be present");
    }

    @Test
    void removeAll_EmptyCollection_ReturnsFalseAndLeavesSetUnchanged() {
        // given
        Collection<String> empty = List.of();
        // when
        boolean changed = set.removeAll(empty);
        // then
        assertFalse(changed, "removeAll on an empty collection must not modify the set");
        assertEquals(3, set.size(), "Set size must remain unchanged");
    }

    @Test
    void removeAll_NullElementInCollection_IgnoresNullAndRemovesOthers() {
        // given
        List<String> toRemove = new ArrayList<>();
        // null should be ignored by the implementation
        toRemove.add(null);
        // matches "Apple" case‑insensitively
        toRemove.add("APPLE");
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should report modification when a matching element is removed");
        assertEquals(2, set.size(), "Set size should be reduced by one");
        assertFalse(set.contains("Apple"), "Element 'Apple' should have been removed");
    }

    /**
     * Group: removeAll - elements cause size change
     * Removing two present elements should return true and leave only one element.
     */
    @ParameterizedTest(name = "[{index}] removeAll({0}) => changed={1}")
    @MethodSource("provideElementsCauseSizeChange")
    void removeAll_ElementsCauseSizeChange(Collection<String> toRemove, boolean expectedChanged) {
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertEquals(expectedChanged, changed, "removeAll should report modification");
        assertEquals(1, set.size(), "Only one element should remain after removing two");
        assertTrue(set.contains("Cherry"), "The remaining element must be 'Cherry'");
    }

    /**
     * Group: removeAll - no elements removed
     * Passing a collection with no matching elements should return false.
     */
    @ParameterizedTest(name = "[{index}] removeAll({0}) => changed={1}")
    @MethodSource("provideNoElementsRemoved")
    void removeAll_NoElementsRemoved(Collection<String> toRemove, boolean expectedChanged) {
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertEquals(expectedChanged, changed, "removeAll should report no modification");
        assertEquals(3, set.size(), "Set size must stay unchanged");
        assertTrue(set.containsAll(List.of("Apple", "Banana", "Cherry")), "All original elements must still be present");
    }

    /**
     * Group: removeAll - null collection
     * Passing {@code null} as the collection argument must throw {@link NullPointerException}.
     */
    @Test
    void removeAll_NullCollection_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> set.removeAll(null), "removeAll(null) should throw NullPointerException");
    }

    /**
     * Group: removeAll - collection contains null element
     * The underlying {@link java.util.concurrent.ConcurrentSkipListMap} does not permit {@code null}
     * keys; attempting to remove a {@code null} element should therefore throw {@link NullPointerException}.
     */
    @Test
    void removeAll_CollectionWithNullElement_ThrowsNullPointerException() {
        Collection<String> withNull = List.of((String) null);
        assertThrows(NullPointerException.class, () -> set.removeAll(withNull), "removeAll(collectionContainingNull) should throw NullPointerException");
    }

    /**
     * Group: removeAll - incompatible element type
     * Removing an element of a type incompatible with the set's generic type should result in
     * {@link ClassCastException} because the underlying map attempts to compare the key with existing
     * String keys.
     */
    @Test
    void removeAll_IncompatibleElementType_ThrowsClassCastException() {
        // The set holds Strings; we attempt to remove an Integer.
        Collection<Integer> incompatible = List.of(123);
        // Suppress unchecked warning – the method signature accepts Collection<?>
        @SuppressWarnings("unchecked")
        Collection<?> raw = (Collection<?>) incompatible;
        assertThrows(ClassCastException.class, () -> set.removeAll(raw), "removeAll(collectionWithInteger) should throw ClassCastException");
    }
}
