package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableSet_removeAll_Test {

    private SealableSet<Integer> set;

    private volatile boolean sealed = false;

    private Supplier<Boolean> sealedSupplier = () -> sealed;

    /* --------------------------------------------------------------------- */
    private SealableSet<Integer> intSet;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh SealableSet for each test and populate it with known values
        set = new SealableSet<>(sealedSupplier);
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly necessary but keeps pattern consistent
        set = null;
        sealed = false;
    }

    /**
     * Parameter source for normal‑operation tests using a String set **
     */
    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(Arguments.of(Arrays.asList("a", "b"), true, // expected remaining elements after removal
        Set.of("c", "d")), Arguments.of(Collections.emptyList(), false, // unchanged
        Set.of("a", "b", "c", "d")), Arguments.of(Arrays.asList("x", "y"), false, // unchanged because none match
        Set.of("a", "b", "c", "d")));
    }

    @Test
    void removeAllWhenUnsealedRemovesExistingElements() {
        // given
        Collection<Integer> toRemove = Arrays.asList(1, 3, null);
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should report a change when elements are removed");
        assertEquals(1, set.size(), "Only element 2 should remain");
        assertTrue(set.contains(2), "Set should still contain element 2");
        assertFalse(set.contains(1), "Element 1 should have been removed");
        assertFalse(set.contains(3), "Element 3 should have been removed");
        assertFalse(set.contains(null), "Null element should have been removed");
    }

    @Test
    void removeAllWhenUnsealedWithNoMatchingElementsDoesNotChangeSet() {
        // given
        Collection<Integer> toRemove = Arrays.asList(4, 5, 6);
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertFalse(changed, "removeAll should report no change when nothing matches");
        assertEquals(4, set.size(), "Set size should remain unchanged");
        assertTrue(set.containsAll(Arrays.asList(1, 2, 3, null)), "Original elements must still be present");
    }

    @Test
    void removeAllWhenUnsealedWithMixedMatchingAndNonMatchingElements() {
        // given
        Collection<Integer> toRemove = Arrays.asList(2, 4, null);
        // when
        boolean changed = set.removeAll(toRemove);
        // then
        assertTrue(changed, "removeAll should report a change when at least one element is removed");
        assertEquals(2, set.size(), "Two elements should remain");
        assertTrue(set.contains(1));
        assertTrue(set.contains(3));
        assertFalse(set.contains(2));
        assertFalse(set.contains(null));
    }

    @Test
    void removeAllWhenSealedThrowsUnsupportedOperationException() {
        // given
        sealed = true;
        Collection<Integer> toRemove = Arrays.asList(1, 2);
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> set.removeAll(toRemove));
    }

    @Test
    void removeAllWithEmptyCollectionDoesNothing() {
        // given
        Collection<Integer> empty = List.of();
        // when
        boolean changed = set.removeAll(empty);
        // then
        assertFalse(changed, "removeAll on empty collection should report no change");
        assertEquals(4, set.size(), "Set size should stay the same");
    }

    @Test
    void removeAllWithNullCollectionThrowsNullPointerException() {
        // given
        Collection<Integer> nullCol = null;
        // when / then
        assertThrows(NullPointerException.class, () -> set.removeAll(nullCol));
    }

    @Test
    void removeAllWhenSealedThrowsIllegalStateException() {
        sealed = true;
        Collection<Integer> toRemove = Arrays.asList(1, 2);
        assertThrows(IllegalStateException.class, () -> intSet.removeAll(toRemove));
    }

    @ParameterizedTest(name = "removeAll normal operation with collection {0}")
    @MethodSource("normalOperationProvider")
    void removeAllNormalOperation(Collection<String> col, boolean expectedResult, Set<String> expectedRemaining) {
        // Arrange – a fresh String‑typed SealableSet with known contents
        // always unsealed
        Supplier<Boolean> supplier = () -> false;
        SealableSet<String> set = new SealableSet<>(supplier);
        set.addAll(Arrays.asList("a", "b", "c", "d"));
        // Act
        boolean changed = set.removeAll(col);
        // Assert
        assertEquals(expectedResult, changed, "removeAll should return " + expectedResult + " for collection " + col);
        assertEquals(expectedRemaining.size(), set.size(), "Set size after removal does not match expectation");
        assertTrue(set.containsAll(expectedRemaining), "Set should contain exactly the expected remaining elements");
    }

    @Test
    void removeAllOnSealedStringSetThrowsIllegalStateException() {
        // sealed
        Supplier<Boolean> supplier = () -> true;
        SealableSet<String> set = new SealableSet<>(supplier);
        set.addAll(Arrays.asList("a", "b"));
        assertThrows(IllegalStateException.class, () -> set.removeAll(Arrays.asList("a", "b")));
    }

    @Test
    void removeAllOnEmptySetReturnsFalse() {
        // unsealed
        Supplier<Boolean> supplier = () -> false;
        SealableSet<String> emptySet = new SealableSet<>(supplier);
        // No elements added – set is empty
        boolean changed = emptySet.removeAll(Arrays.asList("x", "y"));
        assertFalse(changed, "removeAll on an empty set must return false");
        assertTrue(emptySet.isEmpty(), "Set should remain empty");
    }

    @Test
    void removeAllMixedPresenceReturnsTrueAndRemovesOnlyExisting() {
        Supplier<Boolean> supplier = () -> false;
        SealableSet<String> set = new SealableSet<>(supplier);
        set.addAll(Arrays.asList("a", "b", "c"));
        // collection contains one present element ("a") and one absent ("z")
        boolean changed = set.removeAll(Arrays.asList("a", "z"));
        assertTrue(changed, "removeAll should report true when at least one element is removed");
        assertEquals(Set.of("b", "c"), new HashSet<>(set), "Only the present element should have been removed");
    }

    @Test
    void removeAllWithNullElementWhenSetContainsNullReturnsTrue() {
        Supplier<Boolean> supplier = () -> false;
        SealableSet<String> set = new SealableSet<>(supplier);
        set.addAll(Arrays.asList("x", null, "y"));
        boolean changed = set.removeAll(Collections.singletonList(null));
        assertTrue(changed, "Removing null that exists should return true");
        assertFalse(set.contains(null), "Null element should have been removed");
        assertEquals(Set.of("x", "y"), new HashSet<>(set));
    }

    @Test
    void removeAllWithNullElementWhenSetDoesNotContainNullReturnsFalse() {
        Supplier<Boolean> supplier = () -> false;
        SealableSet<String> set = new SealableSet<>(supplier);
        set.addAll(Arrays.asList("x", "y"));
        boolean changed = set.removeAll(Collections.singletonList(null));
        assertFalse(changed, "Removing null when not present should return false");
        assertEquals(Set.of("x", "y"), new HashSet<>(set));
    }

    @Test
    void removeAllSelfRemovalEmptiesSetAndReturnsTrue() {
        Supplier<Boolean> supplier = () -> false;
        SealableSet<String> set = new SealableSet<>(supplier);
        set.addAll(Arrays.asList("p", "q", "r"));
        // Create a copy of the current set
        Set<String> copy = new HashSet<>(set);
        boolean changed = set.removeAll(copy);
        assertTrue(changed, "Self‑removal should return true");
        assertTrue(set.isEmpty(), "Set should be empty after removing all its elements");
    }

    @Test
    void removeAllWithIncompatibleElementTypeThrowsClassCastException() {
        Supplier<Boolean> supplier = () -> false;
        SealableSet<String> set = new SealableSet<>(supplier);
        set.addAll(Arrays.asList("alpha", "beta"));
        // The collection contains Integers, which cannot be cast to String
        Collection<Integer> wrongType = Arrays.asList(1, 2);
        assertThrows(ClassCastException.class, () -> set.removeAll(wrongType));
    }
}
