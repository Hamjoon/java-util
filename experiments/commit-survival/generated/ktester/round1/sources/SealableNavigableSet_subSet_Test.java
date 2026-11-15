package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_subSet_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set for each test with a known ordered collection.
        // The default population (10,20,30,40,50) is suitable for many tests,
        // but individual tests may replace it with a custom set.
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(30);
        set.add(40);
        set.add(50);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        set = null;
        sealedState = false;
    }

    private static Stream<Arguments> normalRangeProvider() {
        return Stream.of(Arguments.of(2, 5, List.of(2, 3, 4)));
    }

    /**
     * Verify that the simple subSet(E from, E to) delegates to the four‑argument version
     * and respects the inclusive/exclusive contract (from inclusive, to exclusive).
     */
    @Test
    void testSubSetInclusiveExclusive() {
        // should contain 20,30
        SortedSet<Integer> sub = set.subSet(20, 40);
        assertEquals(2, sub.size());
        assertTrue(sub.contains(20));
        assertTrue(sub.contains(30));
        assertFalse(sub.contains(40));
        assertFalse(sub.contains(10));
    }

    /**
     * Adding elements within the bounds of the subset should affect both the subset
     * and the underlying set when the set is unsealed.
     */
    @Test
    void testSubSetAddWhenUnsealed() {
        // bounds 15 (inclusive) to 45 (exclusive)
        SortedSet<Integer> sub = set.subSet(15, 45);
        // 20 and 30
        assertEquals(2, sub.size());
        // add a new element within bounds
        assertTrue(sub.add(25));
        assertEquals(3, sub.size());
        assertTrue(set.contains(25));
        assertEquals(6, set.size());
        // add the lower bound element (allowed because fromInclusive = true)
        assertTrue(sub.add(15));
        assertTrue(set.contains(15));
        assertEquals(7, set.size());
        // attempt to add element equal to upper bound -> should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> sub.add(45));
    }

    /**
     * When the set is sealed, any mutating operation on the subset must throw
     * UnsupportedOperationException.
     */
    @Test
    void testSubSetModificationWhenSealed() {
        // 10,20,30
        SortedSet<Integer> sub = set.subSet(10, 40);
        // seal the underlying set
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> sub.add(25));
        assertThrows(UnsupportedOperationException.class, sub::clear);
        assertThrows(UnsupportedOperationException.class, () -> sub.remove(20));
    }

    /**
     * Verify that the four‑argument subSet method respects the inclusive flags.
     */
    @Test
    void testSubSetInclusiveFlags() {
        // fromInclusive = false, toInclusive = true
        // should contain 30 and 40
        SortedSet<Integer> sub = set.subSet(20, false, 40, true);
        assertEquals(2, sub.size());
        assertFalse(sub.contains(20));
        assertTrue(sub.contains(30));
        assertTrue(sub.contains(40));
        // fromInclusive = true, toInclusive = false (default behavior of two‑arg version)
        SortedSet<Integer> sub2 = set.subSet(20, true, 40, false);
        assertEquals(2, sub2.size());
        assertTrue(sub2.contains(20));
        assertTrue(sub2.contains(30));
        assertFalse(sub2.contains(40));
    }

    /**
     * Ensure that the subset view remains consistent after mutations performed directly
     * on the backing set (while unsealed).
     */
    @Test
    void testSubsetReflectsExternalModifications() {
        // initially 20,30,40
        SortedSet<Integer> sub = set.subSet(15, 45);
        assertEquals(3, sub.size());
        // Add element directly to the backing set within the range
        assertTrue(set.add(35));
        assertTrue(sub.contains(35));
        assertEquals(4, sub.size());
        // Remove element directly from the backing set
        assertTrue(set.remove(20));
        assertFalse(sub.contains(20));
        assertEquals(3, sub.size());
    }

    /**
     * Verify that attempting to create a subset with illegal bounds throws
     * IllegalArgumentException as per SortedSet contract.
     */
    @Test
    void testSubSetIllegalBounds() {
        // fromElement > toElement
        assertThrows(IllegalArgumentException.class, () -> set.subSet(40, true, 20, true));
        // null bounds (the underlying set permits null, but subset should reject null)
        assertThrows(NullPointerException.class, () -> set.subSet(null, true, 30, true));
    }

    /**
     * Normal range subset: subSet(2,5) on a set containing {1,2,3,4,5,6}
     * should return a view with [2,3,4].
     */
    @ParameterizedTest(name = "{index} - from={0}, to={1}")
    @MethodSource("normalRangeProvider")
    void testNormalRangeSubset(Integer from, Integer to, List<Integer> expected) {
        // Build a fresh set {1..6}
        SealableNavigableSet<Integer> localSet = new SealableNavigableSet<>(sealedSupplier);
        for (int i = 1; i <= 6; i++) {
            localSet.add(i);
        }
        SortedSet<Integer> sub = localSet.subSet(from, to);
        assertEquals(expected.size(), sub.size());
        assertIterableEquals(expected, sub);
    }

    /**
     * Empty subset when bounds are equal: subSet(3,3) must be empty.
     */
    @Test
    void testEmptySubsetWhenBoundsEqual() {
        SealableNavigableSet<Integer> localSet = new SealableNavigableSet<>(() -> false);
        for (int i = 1; i <= 6; i++) {
            localSet.add(i);
        }
        SortedSet<Integer> sub = localSet.subSet(3, 3);
        assertTrue(sub.isEmpty(), "Subset should be empty when from == to");
    }

    /**
     * IllegalArgumentException when fromElement > toElement.
     */
    @Test
    void testIllegalArgumentWhenFromGreaterThanTo() {
        SealableNavigableSet<Integer> localSet = new SealableNavigableSet<>(() -> false);
        for (int i = 1; i <= 6; i++) {
            localSet.add(i);
        }
        assertThrows(IllegalArgumentException.class, () -> localSet.subSet(7, 4));
    }

    /**
     * NullPointerException for null fromElement.
     */
    @Test
    void testNullPointerExceptionForNullFromElement() {
        SealableNavigableSet<Integer> localSet = new SealableNavigableSet<>(() -> false);
        for (int i = 1; i <= 6; i++) {
            localSet.add(i);
        }
        assertThrows(NullPointerException.class, () -> localSet.subSet(null, 5));
    }

    /**
     * NullPointerException for null toElement.
     */
    @Test
    void testNullPointerExceptionForNullToElement() {
        SealableNavigableSet<Integer> localSet = new SealableNavigableSet<>(() -> false);
        for (int i = 1; i <= 6; i++) {
            localSet.add(i);
        }
        assertThrows(NullPointerException.class, () -> localSet.subSet(2, null));
    }

    /**
     * Modification of subset reflects in original set: removing 3 from the subset
     * must also remove it from the backing set.
     */
    @Test
    void testModificationReflectsInOriginalSet() {
        SealableNavigableSet<Integer> localSet = new SealableNavigableSet<>(() -> false);
        for (int i = 1; i <= 6; i++) {
            localSet.add(i);
        }
        // view [2,3,4]
        SortedSet<Integer> sub = localSet.subSet(2, 5);
        assertTrue(sub.remove(3), "Removal should succeed");
        assertFalse(sub.contains(3));
        assertFalse(localSet.contains(3), "Original set must also no longer contain 3");
        assertEquals(List.of(1, 2, 4, 5, 6), new ArrayList<>(localSet));
    }

    /**
     * Adding element equal to the exclusive upper bound should be rejected.
     * The contract dictates an IllegalArgumentException.
     */
    @Test
    void testAddingUpperBoundElementThrows() {
        SealableNavigableSet<Integer> localSet = new SealableNavigableSet<>(() -> false);
        for (int i = 1; i <= 6; i++) {
            localSet.add(i);
        }
        // upper bound 5 is exclusive
        SortedSet<Integer> sub = localSet.subSet(2, 5);
        // Adding 5 should fail
        assertThrows(IllegalArgumentException.class, () -> sub.add(5));
        // Ensure original set unchanged
        assertEquals(List.of(1, 2, 3, 4, 5, 6), new ArrayList<>(localSet));
    }

    /**
     * ClassCastException when bounds are of incompatible type (String vs Integer).
     */
    @Test
    void testClassCastExceptionForIncompatibleBounds() {
        // The set is typed to Integer, so providing String bounds must fail.
        assertThrows(ClassCastException.class, () -> set.subSet((Integer) (Object) "a", (Integer) (Object) "z"));
    }
}
