package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_removeAll_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private CaseInsensitiveSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set for every test to guarantee isolation.
        set = new CaseInsensitiveSet<>();
        set.addAll(initialElements());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC – not strictly required.
        set = null;
    }

    /**
     * Helper to populate the set with a known mixture of elements.
     */
    private static Set<Object> initialElements() {
        Set<Object> s = new LinkedHashSet<>();
        s.add("One");
        s.add("Two");
        s.add("Three");
        s.add(42);
        s.add("Apple");
        return s;
    }

    private static Stream<Arguments> basicBehaviorProvider() {
        return Stream.of(// empty collection – no change
        arguments(Collections.emptyList(), false, Arrays.asList("a", "b", "c"), "Empty collection should not modify the set"), // remove two existing elements
        arguments(Arrays.asList("b", "c"), true, Collections.singletonList("a"), "Removing existing elements 'b' and 'c' should leave only 'a'"), // collection with no matching elements
        arguments(Collections.singletonList("x"), false, Arrays.asList("a", "b", "c"), "Non‑matching element should not modify the set"), // mixed existing and non‑existing
        arguments(Arrays.asList("b", "x"), true, Arrays.asList("a", "c"), "One matching element ('b') should cause modification"));
    }

    private static Stream<Arguments> caseInsensitiveProvider() {
        return Stream.of(arguments(Arrays.asList("A", "b"), true, Collections.singletonList("c"), "Upper‑case 'A' and lower‑case 'b' should both be removed"), arguments(Arrays.asList("C", "d"), true, Arrays.asList("a", "b"), "Upper‑case 'C' matches 'c'; 'd' is ignored"));
    }

    @Test
    void removeAll_RemovesExistingElements_ReturnsTrue() {
        // GIVEN
        Collection<Object> toRemove = Arrays.asList("one", "TWO", "Four", 42);
        // WHEN
        boolean modified = set.removeAll(toRemove);
        // THEN
        assertTrue(modified, "removeAll should report modification when elements are removed");
        // Elements that existed (case‑insensitively) must be gone
        assertFalse(set.contains("One"));
        assertFalse(set.contains("Two"));
        assertFalse(set.contains(42));
        // Elements not present originally must still be absent
        assertFalse(set.contains("Four"));
        // Remaining elements should still be present
        assertTrue(set.contains("Three"));
        assertTrue(set.contains("Apple"));
        // Size should reflect the removals
        assertEquals(2, set.size());
    }

    @Test
    void removeAll_NoMatchingElements_ReturnsFalse() {
        // GIVEN
        Collection<Object> toRemove = Arrays.asList("four", "FIVE", 99);
        // WHEN
        boolean modified = set.removeAll(toRemove);
        // THEN
        assertFalse(modified, "removeAll should return false when nothing is removed");
        // Original contents must be untouched
        assertTrue(set.contains("One"));
        assertTrue(set.contains("Two"));
        assertTrue(set.contains("Three"));
        assertTrue(set.contains(42));
        assertTrue(set.contains("Apple"));
        assertEquals(5, set.size());
    }

    @Test
    void removeAll_EmptyCollection_ReturnsFalseAndLeavesSetUnchanged() {
        // GIVEN
        Collection<Object> empty = Collections.emptyList();
        // WHEN
        boolean modified = set.removeAll(empty);
        // THEN
        assertFalse(modified, "removeAll on an empty collection should return false");
        assertEquals(5, set.size(), "Set size must remain unchanged");
    }

    @Test
    void removeAll_CollectionWithDuplicates_HandlesGracefully() {
        // GIVEN
        // Duplicate entries in the collection should not cause errors or extra removals
        Collection<Object> toRemove = Arrays.asList("one", "ONE", "One", 42, 42);
        // WHEN
        boolean modified = set.removeAll(toRemove);
        // THEN
        assertTrue(modified, "removeAll should report modification when elements are removed");
        assertFalse(set.contains("One"));
        assertFalse(set.contains(42));
        // Remaining elements unchanged
        assertTrue(set.contains("Two"));
        assertTrue(set.contains("Three"));
        assertTrue(set.contains("Apple"));
        assertEquals(3, set.size());
    }

    @Test
    void removeAll_MixedTypeCollection_RemovesCorrectElements() {
        // GIVEN
        // Add a non‑String element that is not comparable to strings
        set.add(3.14);
        Collection<Object> toRemove = Arrays.asList("apple", 3.14);
        // WHEN
        boolean modified = set.removeAll(toRemove);
        // THEN
        assertTrue(modified, "removeAll should return true when mixed‑type elements are removed");
        assertFalse(set.contains("Apple"));
        assertFalse(set.contains(3.14));
        // Verify other elements are still present
        assertTrue(set.contains("One"));
        assertTrue(set.contains("Two"));
        assertTrue(set.contains("Three"));
        assertTrue(set.contains(42));
        assertEquals(5, set.size());
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("basicBehaviorProvider")
    void removeAll_BasicBehavior(Collection<?> toRemove, boolean expectedModified, List<String> expectedRemaining, String description) {
        // Initialise a fresh set with the canonical ["a","b","c"] contents.
        CaseInsensitiveSet<String> localSet = new CaseInsensitiveSet<>();
        localSet.addAll(Arrays.asList("a", "b", "c"));
        // Exercise the method.
        boolean modified = localSet.removeAll(toRemove);
        // Verify the contract.
        assertEquals(expectedModified, modified, description);
        // The set should contain exactly the expectedRemaining elements (order not important).
        assertEquals(new HashSet<>(expectedRemaining), new HashSet<>(localSet), "Remaining elements mismatch: " + description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("caseInsensitiveProvider")
    void removeAll_CaseInsensitivity(Collection<?> toRemove, boolean expectedModified, List<String> expectedRemaining, String description) {
        CaseInsensitiveSet<String> localSet = new CaseInsensitiveSet<>();
        localSet.addAll(Arrays.asList("a", "b", "c"));
        boolean modified = localSet.removeAll(toRemove);
        assertEquals(expectedModified, modified, description);
        assertEquals(new HashSet<>(expectedRemaining), new HashSet<>(localSet), "Remaining elements after case‑insensitive removal do not match");
    }

    @Test
    void removeAll_NullCollection_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> set.removeAll(null), "Passing a null collection must throw NullPointerException");
    }

    @Test
    void removeAll_CollectionContainsNull_ThrowsNullPointerException() {
        // The underlying map (ConcurrentSkipListMap) does not accept null keys.
        Collection<Object> withNull = Arrays.asList(null, "One");
        assertThrows(NullPointerException.class, () -> set.removeAll(withNull), "Collection containing null should cause NullPointerException");
    }

    @Test
    void removeAll_DuplicateElementsInCollection_RemovesOnceAndReturnsTrue() {
        CaseInsensitiveSet<String> localSet = new CaseInsensitiveSet<>();
        localSet.addAll(Arrays.asList("a", "b", "c"));
        Collection<String> duplicates = Arrays.asList("b", "b", "x");
        boolean modified = localSet.removeAll(duplicates);
        assertTrue(modified, "Method should return true when a duplicate element causes a removal");
        assertFalse(localSet.contains("b"));
        assertEquals(new HashSet<>(Arrays.asList("a", "c")), new HashSet<>(localSet), "Set should contain only 'a' and 'c' after removal");
    }

    @Test
    void removeAll_RemovingAllElements_EmptiesSetAndReturnsTrue() {
        CaseInsensitiveSet<String> localSet = new CaseInsensitiveSet<>();
        localSet.addAll(Arrays.asList("a", "b", "c"));
        boolean modified = localSet.removeAll(Arrays.asList("a", "b", "c"));
        assertTrue(modified, "Removing all elements must return true");
        assertTrue(localSet.isEmpty(), "Set should be empty after removing all elements");
    }

    @Test
    void removeAll_DifferentTypeCollection_NoMatchReturnsFalse() {
        CaseInsensitiveSet<String> localSet = new CaseInsensitiveSet<>();
        localSet.addAll(Arrays.asList("a", "b", "c"));
        Collection<Integer> ints = Arrays.asList(1, 2);
        boolean modified = localSet.removeAll(ints);
        assertFalse(modified, "Removing with a collection of unrelated types should return false");
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), new HashSet<>(localSet), "Set must remain unchanged");
    }

    @Test
    void removeAll_IncompatibleElementTypes_ThrowsClassCastException() {
        CaseInsensitiveSet<String> localSet = new CaseInsensitiveSet<>();
        localSet.addAll(Arrays.asList("a", "b", "c"));
        // Integer cannot be cast to String inside the method.
        Collection<Integer> ints = Arrays.asList(1, 2);
        assertThrows(ClassCastException.class, () -> localSet.removeAll(ints), "Passing a collection of Integer to a Set<String> must throw ClassCastException");
    }

    @Test
    void removeAll_MixedIncompatibleTypes_ThrowsClassCastException() {
        CaseInsensitiveSet<String> localSet = new CaseInsensitiveSet<>();
        localSet.addAll(Arrays.asList("a", "b", "c"));
        // Mixed collection: Boolean then String – the Boolean triggers the exception first.
        Collection<Object> mixed = Arrays.asList(true, "a");
        assertThrows(ClassCastException.class, () -> localSet.removeAll(mixed), "Mixed collection with a non‑String element must cause ClassCastException");
    }

    @Test
    void removeAll_EmptySetEmptyCollection_ReturnsFalse() {
        CaseInsensitiveSet<String> emptySet = new CaseInsensitiveSet<>();
        boolean modified = emptySet.removeAll(Collections.emptyList());
        assertFalse(modified, "Calling removeAll on an empty set with an empty collection must return false");
    }

    @Test
    void removeAll_NonEmptySetEmptyCollection_ReturnsFalse() {
        CaseInsensitiveSet<String> localSet = new CaseInsensitiveSet<>();
        localSet.addAll(Arrays.asList("a", "b"));
        boolean modified = localSet.removeAll(Collections.emptyList());
        assertFalse(modified, "Empty collection should not modify a non‑empty set");
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), new HashSet<>(localSet), "Set contents must remain unchanged");
    }
}
