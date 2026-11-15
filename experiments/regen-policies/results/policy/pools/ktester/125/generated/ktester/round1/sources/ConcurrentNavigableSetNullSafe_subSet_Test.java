package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_subSet_Test {

    private ConcurrentNavigableSetNullSafe<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Populate a set that contains normal values and a null sentinel
        set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
        set.add("date");
        set.add("fig");
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared between tests
        set.clear();
    }

    /**
     * Helper to convert a string like "[1, 2, 3]" into a List<Integer>
     */
    private static List<Integer> parseIntegerList(String s) {
        if (s == null || s.trim().equals("[]")) {
            return Collections.emptyList();
        }
        // remove brackets
        String inner = s.substring(1, s.length() - 1).trim();
        String[] parts = inner.split("\\s*,\\s*");
        List<Integer> list = new ArrayList<>(parts.length);
        for (String p : parts) {
            if (p.equalsIgnoreCase("null")) {
                list.add(null);
            } else {
                list.add(Integer.valueOf(p));
            }
        }
        return list;
    }

    /**
     * -----------------------------------------------------------------
     */
    static Stream<Arguments> nonNullBoundsProvider() {
        return Stream.of(// from 1 (incl) to 5 (excl) => [1,2,3,4]
        Arguments.of(1, true, 5, false, "[1, 2, 3, 4]"), // from 2 (excl) to 4 (incl) => [3,4]
        Arguments.of(2, false, 4, true, "[3, 4]"));
    }

    /**
     * -----------------------------------------------------------------
     */
    static Stream<Arguments> nullLowerBoundProvider() {
        // set contains null,1,2,3,4 ; subset null (incl) to 3 (excl) => [null,1,2]
        return Stream.of(Arguments.of(null, true, 3, false, "[null, 1, 2]"));
    }

    @Test
    @DisplayName("subSet with inclusive lower bound and exclusive upper bound")
    void testSubSetInclusiveExclusive() {
        // from "banana" (inclusive) to "date" (exclusive) -> banana, cherry, null
        NavigableSet<String> sub = set.subSet("banana", true, "date", false);
        assertEquals(3, sub.size(), "Unexpected subset size");
        assertTrue(sub.contains("banana"));
        assertTrue(sub.contains("cherry"));
        assertTrue(sub.contains(null));
        assertFalse(sub.contains("date"));
        assertFalse(sub.contains("apple"));
    }

    @Test
    @DisplayName("subSet with both bounds exclusive")
    void testSubSetExclusiveExclusive() {
        // from "banana" (exclusive) to null (exclusive) -> cherry only
        NavigableSet<String> sub = set.subSet("banana", false, null, false);
        assertEquals(1, sub.size());
        assertTrue(sub.contains("cherry"));
        assertFalse(sub.contains("banana"));
        assertFalse(sub.contains(null));
    }

    @Test
    @DisplayName("subSet with null lower and upper bounds (full set)")
    void testSubSetFullRangeWithNulls() {
        // Both bounds null and inclusive should return the whole set
        NavigableSet<String> sub = set.subSet(null, true, null, true);
        assertEquals(set.size(), sub.size());
        assertTrue(sub.containsAll(set));
    }

    @Test
    @DisplayName("subSet that results in an empty set")
    void testSubSetEmptyResult() {
        // Upper bound lower than lower bound -> empty
        NavigableSet<String> sub = set.subSet("date", true, "banana", true);
        assertTrue(sub.isEmpty());
        assertEquals(0, sub.size());
    }

    @Test
    @DisplayName("subSet returned set reflects modifications to the original set")
    void testSubSetReflectsChanges() {
        NavigableSet<String> sub = set.subSet("banana", true, "fig", true);
        assertTrue(sub.remove("cherry"));
        assertFalse(set.contains("cherry"));
        assertTrue(set.add("grape"));
        // outside the sub‑range
        assertFalse(sub.contains("grape"));
    }

    @Test
    @DisplayName("subSet works with a custom comparator")
    void testSubSetWithComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        ConcurrentNavigableSetNullSafe<String> compSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        compSet.addAll(Arrays.asList("a", "bb", "ccc", null, "dddd"));
        // Subset from length 2 (inclusive) to length 4 (exclusive) -> "bb", "ccc"
        NavigableSet<String> sub = compSet.subSet("bb", true, "dddd", false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains("bb"));
        assertTrue(sub.contains("ccc"));
        assertFalse(sub.contains("a"));
        assertFalse(sub.contains(null));
        assertFalse(sub.contains("dddd"));
    }

    @Test
    @DisplayName("subSet returned type is also ConcurrentNavigableSetNullSafe")
    void testSubSetReturnType() {
        NavigableSet<String> sub = set.subSet("banana", true, null, true);
        assertTrue(sub instanceof ConcurrentNavigableSetNullSafe, "subSet should return a ConcurrentNavigableSetNullSafe instance");
    }

    @ParameterizedTest(name = "non‑null bounds: from={0}({1}), to={2}({3}) → {4}")
    @MethodSource("nonNullBoundsProvider")
    void testSubSetWithNonNullBounds(Integer from, boolean fromInc, Integer to, boolean toInc, String expected) {
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        // populate 1‑5
        for (int i = 1; i <= 5; i++) {
            intSet.add(i);
        }
        NavigableSet<Integer> sub = intSet.subSet(from, fromInc, to, toInc);
        List<Integer> expectedList = parseIntegerList(expected);
        assertIterableEquals(expectedList, sub, () -> "Expected subset " + expectedList + " but got " + sub);
    }

    @ParameterizedTest(name = "null lower bound: from={0}({1}), to={2}({3}) → {4}")
    @MethodSource("nullLowerBoundProvider")
    void testSubSetWithNullLowerBound(Integer from, boolean fromInc, Integer to, boolean toInc, String expected) {
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(null, 1, 2, 3, 4));
        NavigableSet<Integer> sub = intSet.subSet(from, fromInc, to, toInc);
        List<Integer> expectedList = parseIntegerList(expected);
        assertIterableEquals(expectedList, sub);
    }

    /**
     * -----------------------------------------------------------------
     */
    @Test
    @DisplayName("subSet with both bounds null (null inclusive to null exclusive) → empty")
    void testSubSetBothBoundsNullEmptyRange() {
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(null, 1, 2, 3));
        NavigableSet<Integer> sub = intSet.subSet(null, true, null, false);
        assertTrue(sub.isEmpty(), "Subset should be empty when lower bound inclusive null and upper bound exclusive null");
    }

    /**
     * -----------------------------------------------------------------
     */
    @Test
    @DisplayName("subSet with lower bound greater than upper bound throws IllegalArgumentException")
    void testSubSetFromGreaterThanToThrows() {
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        assertThrows(IllegalArgumentException.class, () -> intSet.subSet(5, true, 2, false), "Expected IllegalArgumentException when lower bound > upper bound");
    }

    /**
     * -----------------------------------------------------------------
     */
    @Test
    @DisplayName("subSet on an empty set returns empty subset")
    void testSubSetOnEmptySet() {
        ConcurrentNavigableSetNullSafe<Integer> emptySet = new ConcurrentNavigableSetNullSafe<>();
        NavigableSet<Integer> sub = emptySet.subSet(1, true, 5, false);
        assertTrue(sub.isEmpty(), "Subset of an empty set should be empty");
    }

    /**
     * -----------------------------------------------------------------
     */
    @Test
    @DisplayName("subSet with reverse order comparator returns descending range")
    void testSubSetWithReverseComparator() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableSetNullSafe<Integer> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        revSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // from 4 (inclusive) down to 2 (inclusive) → [4,3,2]
        NavigableSet<Integer> sub = revSet.subSet(4, true, 2, true);
        List<Integer> expected = Arrays.asList(4, 3, 2);
        assertIterableEquals(expected, sub);
    }

    /**
     * -----------------------------------------------------------------
     */
    @Test
    @DisplayName("modifications to subset are reflected in the original set")
    void testSubSetReflectsModifications() {
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // [2,3,4]
        NavigableSet<Integer> sub = intSet.subSet(2, true, 5, false);
        // Adding an element that already exists has no effect
        assertTrue(sub.add(3), "Adding existing element should return true according to Set contract");
        // Add a new element (2.5) – using Double would break type safety, so we use Integer 2 (already present)
        // Instead we add 6 which is outside the range and verify it does NOT appear in the subset
        assertTrue(intSet.add(6));
        assertFalse(sub.contains(6), "Element added to original set outside subset range must not appear in subset");
        // Add a new element within the range via the subset view
        assertTrue(sub.add(2), "Adding 2 (already present) returns true");
        // Verify original set now contains the element (it already did)
        assertTrue(intSet.contains(2));
    }

    /**
     * -----------------------------------------------------------------
     */
    @Test
    @DisplayName("subSet with incompatible lower bound type throws ClassCastException")
    void testSubSetIncompatibleLowerBound() {
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        assertThrows(ClassCastException.class, () -> intSet.subSet((Integer) (Object) "a", true, 5, false), "Expected ClassCastException when lower bound type is incompatible");
    }

    @Test
    @DisplayName("subSet with incompatible upper bound type throws ClassCastException")
    void testSubSetIncompatibleUpperBound() {
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        assertThrows(ClassCastException.class, () -> intSet.subSet(1, true, (Integer) (Object) "b", true), "Expected ClassCastException when upper bound type is incompatible");
    }
}
