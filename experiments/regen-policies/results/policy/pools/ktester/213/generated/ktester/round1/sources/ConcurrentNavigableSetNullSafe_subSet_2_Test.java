package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_subSet_2_Test {

    /**
     * The set used in each test – created fresh for test isolation.
     */
    private NavigableSet<String> set;

    /**
     * A comparator that orders strings by length, treating {@code null} as the smallest element.
     */
    private static final Comparator<String> LENGTH_COMPARATOR = Comparator.comparingInt(s -> s == null ? 0 : s.length());

    /**
     * Set used for the integer‑based test groups.
     */
    private NavigableSet<Integer> intSet;

    /**
     * Set used for the raw‑type (type‑mismatch) tests.
     */
    @SuppressWarnings("rawtypes")
    private ConcurrentNavigableSetNullSafe rawSet;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural) ordering for most tests.
        set = new ConcurrentNavigableSetNullSafe<>();
        // Populate with a deterministic ordering that also includes a null element.
        // length 5
        set.add("apple");
        // length 6
        set.add("banana");
        // length 6
        set.add("cherry");
        // treated as the smallest element
        set.add(null);
        // Integer set populated with 1‑6 (natural ordering).
        intSet = new ConcurrentNavigableSetNullSafe<>();
        for (int i = 1; i <= 6; i++) {
            intSet.add(i);
        }
        // Raw set for ClassCastException scenarios – populated with the same integers.
        rawSet = new ConcurrentNavigableSetNullSafe<>();
        for (int i = 1; i <= 6; i++) {
            rawSet.add(i);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared so that a failing test does not affect the next one.
        set.clear();
        intSet.clear();
        rawSet.clear();
    }

    private static Stream<Arguments> normalRangeProvider() {
        return Stream.of(Arguments.of(1, 5, Arrays.asList(1, 2, 3, 4)), Arguments.of(2, 2, Collections.emptyList()));
    }

    @Test
    void subSet_defaultInclusiveExclusive() {
        // The default subSet(E from, E to) is inclusive of 'from' and exclusive of 'to'.
        // Subset from "banana" (inclusive) to "cherry" (exclusive) should contain only "banana".
        NavigableSet<String> sub = set.subSet("banana", "cherry");
        assertEquals(1, sub.size(), "Expected exactly one element in the subset");
        assertTrue(sub.contains("banana"));
        assertFalse(sub.contains("cherry"));
        assertFalse(sub.contains(null));
    }

    @Test
    void subSet_inclusiveBothBounds() {
        // Include both bounds: from "banana" (inclusive) to null (inclusive).
        NavigableSet<String> sub = set.subSet("banana", true, null, true);
        // Expected order: "banana", "cherry", null (null is considered greatest in natural order)
        assertEquals(3, sub.size());
        assertTrue(sub.contains("banana"));
        assertTrue(sub.contains("cherry"));
        assertTrue(sub.contains(null));
        // Verify iteration order matches the underlying set order.
        assertEquals("banana", sub.first());
        assertEquals(null, sub.last());
    }

    @Test
    void subSet_exclusiveBothBounds() {
        // Exclude both bounds: from "apple" (exclusive) to "cherry" (exclusive).
        NavigableSet<String> sub = set.subSet("apple", false, "cherry", false);
        // Should contain only "banana"
        assertEquals(1, sub.size());
        assertTrue(sub.contains("banana"));
        assertFalse(sub.contains("apple"));
        assertFalse(sub.contains("cherry"));
    }

    @Test
    void subSet_withNullBounds() {
        // Subset from null (inclusive) to "banana" (exclusive) should contain only null.
        NavigableSet<String> sub = set.subSet(null, true, "banana", false);
        assertEquals(1, sub.size());
        assertTrue(sub.contains(null));
        assertFalse(sub.contains("apple"));
        assertFalse(sub.contains("banana"));
    }

    @Test
    void subSet_whenFromGreaterThanTo_returnsEmpty() {
        // From element greater than to element should result in an empty set.
        NavigableSet<String> sub = set.subSet("cherry", true, "banana", true);
        assertTrue(sub.isEmpty(), "Subset with reversed bounds must be empty");
    }

    @Test
    void subSet_usingCustomComparator() {
        // Re‑initialise the set with a length‑based comparator.
        set = new ConcurrentNavigableSetNullSafe<>(LENGTH_COMPARATOR);
        // length 1
        set.add("a");
        // length 2
        set.add("bb");
        // length 3
        set.add("ccc");
        // length 0 (smallest)
        set.add(null);
        // Subset from length 1 (inclusive) to length 3 (inclusive)
        NavigableSet<String> sub = set.subSet("a", true, "ccc", true);
        assertEquals(3, sub.size());
        assertTrue(sub.contains("a"));
        assertTrue(sub.contains("bb"));
        assertTrue(sub.contains("ccc"));
        assertFalse(sub.contains(null));
    }

    @Test
    void subSet_modificationsReflectInOriginalSet() {
        // Obtain a mutable subset and modify it; changes must be visible in the original set.
        NavigableSet<String> sub = set.subSet("banana", true, null, true);
        assertTrue(sub.remove("banana"));
        assertFalse(set.contains("banana"));
        assertTrue(sub.add("banana"));
        assertTrue(set.contains("banana"));
    }

    @Test
    void subSet_nullFromAndToBothInclusive() {
        // Subset where both bounds are null and inclusive should contain only the null element.
        NavigableSet<String> sub = set.subSet(null, true, null, true);
        assertEquals(1, sub.size());
        assertTrue(sub.contains(null));
    }

    /**
     * Normal range – typical inclusive/exclusive behaviour.
     *
     * The JSON specification defines two cases:
     *   1. subSet(1,5) → [1,2,3,4]
     *   2. subSet(2,2) → [] (empty because upper bound is exclusive)
     */
    @ParameterizedTest(name = "{index}: subSet({0},{1}) → {2}")
    @MethodSource("normalRangeProvider")
    void subSet_normalRange(Integer from, Integer to, List<Integer> expected) {
        SortedSet<Integer> subset = intSet.subSet(from, to);
        assertEquals(expected, new ArrayList<>(subset), () -> "Expected subset to contain " + expected);
    }

    @Test
    void subSet_nullLowerBound_throwsNPE() {
        // According to the JSON spec, a null lower bound should raise NPE.
        assertThrows(NullPointerException.class, () -> intSet.subSet(null, 5), "Calling subSet(null, 5) must throw NullPointerException");
    }

    @Test
    void subSet_nullUpperBound_throwsNPE() {
        assertThrows(NullPointerException.class, () -> intSet.subSet(1, null), "Calling subSet(1, null) must throw NullPointerException");
    }

    @Test
    void subSet_fromGreaterThanTo_throwsIllegalArgumentException() {
        // fromElement > toElement should violate the SortedSet contract.
        assertThrows(IllegalArgumentException.class, () -> intSet.subSet(5, 1), "subSet(5,1) must throw IllegalArgumentException");
    }

    @Test
    void subSet_equalBoundsExclusiveUpper_returnsEmpty() {
        SortedSet<Integer> subset = intSet.subSet(1, 1);
        assertTrue(subset.isEmpty(), "When both bounds are equal, the subset must be empty");
    }

    @Test
    void subSet_upperBoundBeyondGreatest_returnsAllFromLowerInclusive() {
        // Upper bound 7 is beyond the greatest element (6); expect 1‑6.
        SortedSet<Integer> subset = intSet.subSet(1, 7);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), new ArrayList<>(subset));
    }

    @Test
    void subSet_lowerBoundBelowSmallest_startsFromFirstElement() {
        // Lower bound -10 is below the smallest element (1); expect 1 and 2.
        SortedSet<Integer> subset = intSet.subSet(-10, 2);
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(subset));
    }

    @Test
    void subSet_equalBoundsExclusiveUpper_duplicateTest() {
        SortedSet<Integer> subset = intSet.subSet(3, 3);
        assertTrue(subset.isEmpty(), "Subset with equal bounds must be empty");
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void subSet_fromElementWrongType_throwsClassCastException() {
        // rawSet is declared without generic type information, allowing us to pass a String.
        assertThrows(ClassCastException.class, () -> rawSet.subSet("string", 5), "Using a String as lower bound on an Integer set must throw ClassCastException");
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void subSet_toElementWrongType_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> rawSet.subSet(1, "string"), "Using a String as upper bound on an Integer set must throw ClassCastException");
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void subSet_nonComparableObjects_throwsClassCastException() {
        // Both bounds are raw Objects that do not implement Comparable.
        Object o1 = new Object();
        Object o2 = new Object();
        assertThrows(ClassCastException.class, () -> rawSet.subSet(o1, o2), "Using non‑Comparable objects must throw ClassCastException");
    }
}
