package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_headSet_2_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        sealedSupplier = () -> sealedState;
        // The constructor used in the original test suite accepts only a Supplier.
        // It internally creates a mutable NavigableSet (TreeSet) for us.
        set = new SealableNavigableSet<>(sealedSupplier);
        // Populate with a known ordered collection
        set.addAll(List.of(1, 2, 3, 4, 5));
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references
        set = null;
    }

    private static Stream<Arguments> exclusiveInclusiveProvider() {
        return Stream.of(Arguments.of(3, false, List.of(1, 2)), Arguments.of(3, true, List.of(1, 2, 3)));
    }

    @Test
    void headSetExclusiveWhenUnsealed_AllowsModification() {
        // should contain 10,20
        SortedSet<Integer> head = set.headSet(30, false);
        assertEquals(2, head.size());
        assertTrue(head.contains(10));
        assertTrue(head.contains(20));
        assertFalse(head.contains(30));
        // Unsealed – modifications should succeed
        assertDoesNotThrow(() -> head.add(5));
        assertTrue(head.contains(5));
        assertEquals(3, head.size());
        // Adding an element equal to the bound should fail (outside the exclusive range)
        assertThrows(IllegalArgumentException.class, () -> head.add(30));
    }

    @Test
    void headSetInclusiveWhenUnsealed_AllowsModification() {
        // should contain 10,20,30
        SortedSet<Integer> head = set.headSet(30, true);
        assertEquals(3, head.size());
        assertTrue(head.contains(30));
        // Unsealed – modifications should succeed
        assertDoesNotThrow(() -> head.add(25));
        assertTrue(head.contains(25));
        assertEquals(4, head.size());
        // Adding element greater than bound should fail
        assertThrows(IllegalArgumentException.class, () -> head.add(35));
    }

    @Test
    void headSetModificationThrowsWhenSealed() {
        SortedSet<Integer> head = set.headSet(30, false);
        // seal the underlying set
        sealedState = true;
        // Reading is still allowed
        assertEquals(2, head.size());
        assertTrue(head.contains(10));
        // Any mutating operation must throw
        assertThrows(UnsupportedOperationException.class, () -> head.add(5));
        assertThrows(UnsupportedOperationException.class, head::clear);
        assertThrows(UnsupportedOperationException.class, () -> head.remove(10));
        assertThrows(UnsupportedOperationException.class, () -> head.addAll(java.util.List.of(1, 2)));
    }

    @Test
    void headSetReflectsChangesInParentSet() {
        // 10,20,30
        SortedSet<Integer> head = set.headSet(40, false);
        assertEquals(3, head.size());
        // Add element to parent set that falls within the headSet range
        assertDoesNotThrow(() -> set.add(25));
        assertTrue(head.contains(25));
        assertEquals(4, head.size());
        // Remove element via parent set
        assertTrue(set.remove(20));
        assertFalse(head.contains(20));
        assertEquals(3, head.size());
    }

    @Test
    void headSetDoesNotAllowNullWhenUnderlyingSetDisallowsIt() {
        // The underlying ConcurrentNavigableSetNullSafe does not permit nulls
        assertThrows(NullPointerException.class, () -> set.headSet(null, false));
    }

    /**
     * Retrieves the private {@code sealedSupplier} field from a {@link SealableNavigableSet}
     * instance via reflection.
     */
    @SuppressWarnings("unchecked")
    private static Supplier<Boolean> getSealedSupplier(SealableNavigableSet<?> s) {
        try {
            Field f = SealableNavigableSet.class.getDeclaredField("sealedSupplier");
            f.setAccessible(true);
            return (Supplier<Boolean>) f.get(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the private {@code navSet} field from a {@link SealableNavigableSet}
     * instance via reflection.
     */
    @SuppressWarnings("unchecked")
    private static NavigableSet<?> getNavSet(SealableNavigableSet<?> s) {
        try {
            Field f = SealableNavigableSet.class.getDeclaredField("navSet");
            f.setAccessible(true);
            return (NavigableSet<?>) f.get(s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void headSetReturnsSealableWrapperAndPreservesSupplier() {
        SortedSet<Integer> head = set.headSet(5, true);
        assertInstanceOf(SealableNavigableSet.class, head, "headSet should return a SealableNavigableSet wrapper");
        // Verify that the sealedSupplier reference is the same instance
        Supplier<Boolean> returnedSupplier = getSealedSupplier((SealableNavigableSet<Integer>) head);
        assertSame(sealedSupplier, returnedSupplier, "Returned set must reuse the original sealedSupplier instance");
    }

    @Test
    void headSetDelegatesToUnderlyingNavigableSet() {
        SortedSet<Integer> head = set.headSet(5, false);
        // Underlying navSet should be the headSet view of the original TreeSet
        NavigableSet<Integer> originalNav = (NavigableSet<Integer>) getNavSet(set);
        NavigableSet<Integer> expected = originalNav.headSet(5, false);
        assertEquals(expected, getNavSet((SealableNavigableSet<Integer>) head), "Underlying navSet of the wrapper must be the headSet of the original set");
    }

    @Test
    void headSetThrowsWhenUnderlyingSetRejectsNull() {
        assertThrows(NullPointerException.class, () -> set.headSet(null, true));
    }

    @Test
    void headSetPropagatesIllegalArgumentFromUnderlyingSet() {
        // Use a set with a comparator that only accepts positive numbers.
        Comparator<Integer> positiveOnly = (a, b) -> Integer.compare(a, b);
        NavigableSet<Integer> base = new TreeSet<>(positiveOnly);
        base.addAll(List.of(1, 2, 3));
        SealableNavigableSet<Integer> custom = new SealableNavigableSet<>(base, sealedSupplier);
        // The underlying TreeSet will throw IllegalArgumentException if we try to add a negative number.
        SortedSet<Integer> head = custom.headSet(2, true);
        assertThrows(IllegalArgumentException.class, () -> head.add(-1));
    }

    @Test
    void headSetMutatingOperationsRespectSealState() {
        SortedSet<Integer> head = set.headSet(5, true);
        // Unsealed – mutation works
        assertDoesNotThrow(() -> head.add(0));
        assertTrue(head.contains(0));
        // Seal the original set
        sealedState = true;
        assertThrows(IllegalStateException.class, () -> head.add(6), "When sealed, any mutating operation must throw IllegalStateException");
        assertThrows(IllegalStateException.class, head::clear);
        assertThrows(IllegalStateException.class, () -> head.remove(1));
    }

    @Test
    void headSetOnEmptyOriginalSetReturnsEmptySet() {
        SealableNavigableSet<Integer> empty = new SealableNavigableSet<>(sealedSupplier);
        SortedSet<Integer> head = empty.headSet(10, true);
        assertTrue(head.isEmpty(), "headSet of an empty set must be empty");
        assertFalse(head.iterator().hasNext());
    }

    @Test
    void headSetWithBoundLowerThanSmallestElementReturnsEmpty() {
        SortedSet<Integer> head = set.headSet(0, true);
        assertTrue(head.isEmpty(), "Bound lower than the smallest element should yield an empty headSet");
    }

    @Test
    void headSetWithBoundGreaterThanGreatestElementReturnsWholeSet() {
        SortedSet<Integer> head = set.headSet(10, false);
        assertEquals(5, head.size(), "Bound greater than greatest element should return all elements");
        assertTrue(head.containsAll(List.of(1, 2, 3, 4, 5)));
    }

    @ParameterizedTest(name = "exclusive bound {0} -> expected {2}")
    @MethodSource("exclusiveInclusiveProvider")
    void headSetInclusiveExclusiveBound(int bound, boolean inclusive, List<Integer> expected) {
        SortedSet<Integer> head = set.headSet(bound, inclusive);
        assertEquals(expected.size(), head.size());
        assertIterableEquals(expected, head);
    }

    @Test
    void headSetWithReverseOrderComparatorInclusive() {
        NavigableSet<Integer> reverse = new TreeSet<>(Comparator.reverseOrder());
        reverse.addAll(List.of(1, 2, 3, 4, 5));
        SealableNavigableSet<Integer> revSet = new SealableNavigableSet<>(reverse, sealedSupplier);
        // in reverse order "head" means >=3
        SortedSet<Integer> head = revSet.headSet(3, true);
        // Expected descending order: 5,4,3
        List<Integer> expected = List.of(5, 4, 3);
        assertIterableEquals(expected, head);
    }

    @Test
    void headSetWithReverseOrderComparatorExclusive() {
        NavigableSet<Integer> reverse = new TreeSet<>(Comparator.reverseOrder());
        reverse.addAll(List.of(1, 2, 3, 4, 5));
        SealableNavigableSet<Integer> revSet = new SealableNavigableSet<>(reverse, sealedSupplier);
        // >3 in reverse order
        SortedSet<Integer> head = revSet.headSet(3, false);
        List<Integer> expected = List.of(5, 4);
        assertIterableEquals(expected, head);
    }

    @Test
    void headSetAddReflectsInParentSetWhenUnsealed() {
        SortedSet<Integer> head = set.headSet(5, true);
        assertDoesNotThrow(() -> head.add(0));
        assertTrue(set.contains(0), "Adding via headSet must affect the original set");
        assertEquals(6, set.size());
    }

    @Test
    void headSetAddOutsideBoundThrows() {
        SortedSet<Integer> head = set.headSet(5, true);
        // 6 is greater than the inclusive bound 5
        assertThrows(IllegalArgumentException.class, () -> head.add(6));
    }

    @Test
    void headSetDynamicSealChangeIsHonored() {
        SortedSet<Integer> head = set.headSet(5, true);
        // Initially unsealed – mutation works
        assertDoesNotThrow(() -> head.add(0));
        // Change seal state after the view was created
        sealedState = true;
        assertThrows(IllegalStateException.class, () -> head.add(1));
    }

    @Test
    void headSetCreatedWhenAlreadySealedIsImmutable() {
        sealedState = true;
        SortedSet<Integer> head = set.headSet(5, true);
        assertThrows(IllegalStateException.class, () -> head.add(0));
    }

    @Test
    void headSetWithIncompatibleElementTypePropagatesClassCastException() {
        // The underlying set is a NavigableSet<Integer>
        // Passing a String should cause a ClassCastException from the delegate.
        assertThrows(ClassCastException.class, () -> set.headSet((Integer) (Object) "z", true));
    }

    @Test
    void headSetWhenUnderlyingSetIsUnmodifiablePropagatesUnsupportedOperation() {
        NavigableSet<Integer> unmodifiable = Collections.unmodifiableNavigableSet(new TreeSet<>(List.of(1, 2, 3)));
        SealableNavigableSet<Integer> wrapper = new SealableNavigableSet<>(unmodifiable, sealedSupplier);
        assertThrows(UnsupportedOperationException.class, () -> wrapper.headSet(2, true));
    }

    @Test
    void headSetCreationFailsWhenSupplierThrows() {
        Supplier<Boolean> explodingSupplier = () -> {
            throw new RuntimeException("Supplier exploded");
        };
        SealableNavigableSet<Integer> faulty = new SealableNavigableSet<>(explodingSupplier);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> faulty.headSet(5, true));
        assertEquals("Supplier exploded", ex.getMessage());
    }
}
