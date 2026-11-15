package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_subSet_2_Test {

    /**
     * The set under test – a mutable view that can be sealed via the supplied {@code Supplier}.
     */
    private SealableNavigableSet<Integer> set;

    /**
     * Mutable flag used by the {@code sealedSupplier} to indicate the sealed state.
     */
    private volatile boolean sealedState = false;

    /**
     * Supplier that simply returns the current {@code sealedState}.
     */
    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test and populate it with a known ordering.
        set = new SealableNavigableSet<>(sealedSupplier);
        // The original test suite adds a null element – the implementation permits it.
        set.add(null);
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        set.add(6);
        set.add(7);
        set.add(8);
        set.add(9);
        set.add(10);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the GC and ensure no test leaks state.
        set = null;
        sealedState = false;
    }

    /**
     * Helper that converts a string like "[1, 2, 3]" into a List<Integer>.
     */
    private static List<Integer> parseExpected(String s) {
        if (s == null || s.trim().equals("[]")) {
            return Collections.emptyList();
        }
        // remove brackets
        String inner = s.substring(1, s.length() - 1).trim();
        if (inner.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = inner.split("\\s*,\\s*");
        List<Integer> list = new ArrayList<>(parts.length);
        for (String p : parts) {
            list.add(Integer.valueOf(p));
        }
        return list;
    }

    private static Stream<Arguments> normalSubsetProvider() {
        return Stream.of(Arguments.of(2, true, 5, false, "[2, 3, 4]"), Arguments.of(2, false, 5, true, "[3, 4, 5]"), Arguments.of(1, true, 10, true, "[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]"));
    }

    private static Stream<Arguments> emptyExclusiveProvider() {
        return Stream.of(Arguments.of(3, false, 3, false, "[]"));
    }

    private static Stream<Arguments> singletonInclusiveProvider() {
        return Stream.of(Arguments.of(3, true, 3, true, "[3]"));
    }

    private static Stream<Arguments> invalidRangeProvider() {
        return Stream.of(Arguments.of(5, true, 2, true));
    }

    private static Stream<Arguments> lowerBoundMissingProvider() {
        return Stream.of(Arguments.of(0, false, 3, true, "[1, 2, 3]"));
    }

    private static Stream<Arguments> upperBoundMissingProvider() {
        return Stream.of(Arguments.of(8, true, 12, false, "[8, 9, 10]"));
    }

    private static Stream<Arguments> nullFromProvider() {
        return Stream.of(Arguments.of(null, true, 5, true));
    }

    private static Stream<Arguments> nullToProvider() {
        return Stream.of(Arguments.of(2, true, null, false));
    }

    private static Stream<Arguments> classCastProvider() {
        return Stream.of(// both bounds are Strings while the set holds Integers
        Arguments.of("a", true, "z", false), // mixed types: Integer and String
        Arguments.of(2, true, "z", true));
    }

    @Test
    void subSetIsSealableAndReflectsChangesInParent() {
        // Create a subset that includes 10..30 inclusive on both ends.
        NavigableSet<Integer> sub = set.subSet(10, true, 30, true);
        // Adding an element that lies inside the range must affect both sets.
        assertTrue(sub.add(25), "Adding 25 to the subset should succeed");
        assertTrue(set.contains(25), "Parent set must contain the element added via the subset");
        // Adding an element outside the defined range must be rejected.
        assertThrows(IllegalArgumentException.class, () -> sub.add(35), "Adding an element outside the subset range should throw IAE");
        // Removing via the subset also removes from the parent.
        assertTrue(sub.remove(20), "Removing 20 via subset should succeed");
        assertFalse(set.contains(20), "Parent set must no longer contain 20");
    }

    @Test
    void subSetRespectsInclusiveAndExclusiveBounds() {
        // Inclusive lower, exclusive upper.
        NavigableSet<Integer> subIncExc = set.subSet(10, true, 30, false);
        assertTrue(subIncExc.contains(10), "Lower bound inclusive – 10 must be present");
        assertFalse(subIncExc.contains(30), "Upper bound exclusive – 30 must NOT be present");
        // Exclusive lower, inclusive upper.
        NavigableSet<Integer> subExcInc = set.subSet(10, false, 30, true);
        assertFalse(subExcInc.contains(10), "Lower bound exclusive – 10 must NOT be present");
        assertTrue(subExcInc.contains(30), "Upper bound inclusive – 30 must be present");
    }

    @Test
    void subSetModificationWhenSealedThrowsUnsupportedOperationException() {
        // Obtain a normal (unsealed) subset first.
        NavigableSet<Integer> sub = set.subSet(10, true, 30, true);
        // Seal the backing set – the supplier now returns true.
        sealedState = true;
        // Any mutating operation on the subset must now fail.
        assertThrows(UnsupportedOperationException.class, () -> sub.add(22), "Adding to a sealed subset must throw UOE");
        assertThrows(UnsupportedOperationException.class, sub::clear, "Clearing a sealed subset must throw UOE");
        assertThrows(UnsupportedOperationException.class, () -> sub.remove(20), "Removing from a sealed subset must throw UOE");
    }

    @Test
    void subSetIteratorsRespectSealedState() {
        NavigableSet<Integer> sub = set.subSet(10, true, 30, true);
        var it = sub.iterator();
        // The iterator should be able to read elements regardless of seal state.
        sealedState = true;
        assertDoesNotThrow(it::next, "Iterator.next() must work when sealed");
        // But the iterator's remove operation must be prohibited.
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove() must throw UOE when the set is sealed");
    }

    @Test
    void subSetWithNullElementWorksWhenAllowed() {
        // The underlying set contains a null element; verify that a subset that includes it behaves correctly.
        NavigableSet<Integer> sub = set.subSet(null, true, 10, true);
        assertTrue(sub.contains(null), "Subset should contain the null element");
        assertTrue(sub.contains(10), "Subset should contain 10");
        // Adding a new element inside the range works.
        assertTrue(sub.add(5));
        assertTrue(set.contains(5));
    }

    @Test
    void subSetReturnedTypeIsSealableNavigableSet() {
        // The contract states that subSet returns a SealableNavigableSet wrapper.
        NavigableSet<Integer> sub = set.subSet(10, true, 30, true);
        assertTrue(sub instanceof SealableNavigableSet, "subSet should return an instance of SealableNavigableSet");
    }

    @ParameterizedTest(name = "Normal subset: from={0} ({1}), to={2} ({3}) → {4}")
    @MethodSource("normalSubsetProvider")
    void testNormalSubsetCreation(Integer from, boolean fromInc, Integer to, boolean toInc, String expectedStr) {
        NavigableSet<Integer> sub = set.subSet(from, fromInc, to, toInc);
        List<Integer> expected = parseExpected(expectedStr);
        assertIterableEquals(expected, sub, "Subset contents do not match expected");
    }

    @ParameterizedTest(name = "Empty exclusive subset: from={0} ({1}), to={2} ({3})")
    @MethodSource("emptyExclusiveProvider")
    void testEmptySubsetWhenBoundsEqualAndExclusive(Integer from, boolean fromInc, Integer to, boolean toInc, String expectedStr) {
        NavigableSet<Integer> sub = set.subSet(from, fromInc, to, toInc);
        List<Integer> expected = parseExpected(expectedStr);
        assertIterableEquals(expected, sub, "Expected an empty subset");
    }

    @ParameterizedTest(name = "Singleton inclusive subset: from={0} ({1}), to={2} ({3})")
    @MethodSource("singletonInclusiveProvider")
    void testSingletonSubsetWhenBoundsEqualAndInclusive(Integer from, boolean fromInc, Integer to, boolean toInc, String expectedStr) {
        NavigableSet<Integer> sub = set.subSet(from, fromInc, to, toInc);
        List<Integer> expected = parseExpected(expectedStr);
        assertIterableEquals(expected, sub, "Singleton subset should contain exactly the bound element");
    }

    @ParameterizedTest(name = "Invalid range: from={0} ({1}) > to={2} ({3})")
    @MethodSource("invalidRangeProvider")
    void testInvalidRangeThrowsException(Integer from, boolean fromInc, Integer to, boolean toInc) {
        assertThrows(IllegalArgumentException.class, () -> set.subSet(from, fromInc, to, toInc), "Expected IllegalArgumentException for fromElement > toElement");
    }

    @ParameterizedTest(name = "Lower bound missing: from={0} ({1}), to={2} ({3})")
    @MethodSource("lowerBoundMissingProvider")
    void testSubsetWithLowerBoundNotPresent(Integer from, boolean fromInc, Integer to, boolean toInc, String expectedStr) {
        NavigableSet<Integer> sub = set.subSet(from, fromInc, to, toInc);
        List<Integer> expected = parseExpected(expectedStr);
        assertIterableEquals(expected, sub, "Subset should start from the next greater element");
    }

    @ParameterizedTest(name = "Upper bound missing: from={0} ({1}), to={2} ({3})")
    @MethodSource("upperBoundMissingProvider")
    void testSubsetWithUpperBoundNotPresent(Integer from, boolean fromInc, Integer to, boolean toInc, String expectedStr) {
        NavigableSet<Integer> sub = set.subSet(from, fromInc, to, toInc);
        List<Integer> expected = parseExpected(expectedStr);
        assertIterableEquals(expected, sub, "Subset should stop at the last element of the backing set");
    }

    @ParameterizedTest(name = "Null fromElement throws NPE")
    @MethodSource("nullFromProvider")
    void testNullFromElementThrowsNPE(Integer from, boolean fromInc, Integer to, boolean toInc) {
        assertThrows(NullPointerException.class, () -> set.subSet(from, fromInc, to, toInc), "Passing null as fromElement should throw NullPointerException");
    }

    @ParameterizedTest(name = "Null toElement throws NPE")
    @MethodSource("nullToProvider")
    void testNullToElementThrowsNPE(Integer from, boolean fromInc, Integer to, boolean toInc) {
        assertThrows(NullPointerException.class, () -> set.subSet(from, fromInc, to, toInc), "Passing null as toElement should throw NullPointerException");
    }

    @ParameterizedTest(name = "Incompatible element type throws ClassCastException")
    @MethodSource("classCastProvider")
    void testIncompatibleElementTypeThrowsClassCastException(Object from, boolean fromInc, Object to, boolean toInc) {
        // The set is typed to Integer, so any non‑Integer bound must cause a ClassCastException.
        assertThrows(ClassCastException.class, () -> set.subSet((Integer) from, fromInc, (Integer) to, toInc), "Using incompatible element types should throw ClassCastException");
    }
}
