package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_subSet_Test {

    /**
     * The set under test – a fresh instance is created before each test method.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural ordering) constructor for most tests.
        set = new ConcurrentNavigableSetNullSafe<>();
        // Populate with a deterministic ordering that also includes a null element.
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid any cross‑test contamination.
        set.clear();
    }

    // -----------------------------------------------------------------
    private static void assertSubsetEquals(NavigableSet<Integer> actual, List<Integer> expected) {
        List<Integer> actualList = new ArrayList<>(actual);
        assertEquals(expected, actualList, () -> "Expected subset " + expected + " but got " + actualList);
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> nonNullInclusiveProvider() {
        return Stream.of(Arguments.of(1, true, 5, true, List.of(1, 2, 3, 4, 5)), Arguments.of(1, false, 5, false, List.of(2, 3, 4)), Arguments.of(1, true, 5, false, List.of(1, 2, 3, 4)), Arguments.of(1, false, 5, true, List.of(2, 3, 4, 5)));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> nullBoundsProvider() {
        return Stream.of(// null lower bound inclusive → treat null as smallest element
        Arguments.of(null, true, 5, true, List.of(null, 1, 2, 3, 4, 5)), // upper bound null exclusive → empty because null is smallest
        Arguments.of(1, true, null, false, Collections.emptyList()), // both bounds null, lower exclusive → empty
        Arguments.of(null, false, null, true, Collections.emptyList()));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> equalBoundsProvider() {
        return Stream.of(Arguments.of(3, true, 3, true, List.of(3)), Arguments.of(3, false, 3, false, Collections.emptyList()), Arguments.of(3, true, 3, false, Collections.emptyList()), Arguments.of(3, false, 3, true, Collections.emptyList()));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> reversedRangeProvider() {
        return Stream.of(Arguments.of(5, true, 1, true), Arguments.of(5, false, 1, false));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> singleElementProvider() {
        return Stream.of(Arguments.of(10, true, 10, true, List.of(10)), Arguments.of(10, false, 10, true, Collections.emptyList()), Arguments.of(10, true, 10, false, Collections.emptyList()), Arguments.of(10, false, 10, false, Collections.emptyList()));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> descendingComparatorProvider() {
        return Stream.of(// Set orders integers descending. Subset (4, exclusive) .. (2, inclusive) → [3, 2]
        Arguments.of(Comparator.<Integer>reverseOrder(), 4, false, 2, true, List.of(3, 2)), // Null treated as smallest element. Subset null (inclusive) .. 3 (exclusive)
        // → elements greater than 3 in descending order: 5, 4
        Arguments.of(Comparator.<Integer>reverseOrder(), null, true, 3, false, List.of(5, 4)));
    }

    @Test
    void subSet_InclusiveBothBounds_IncludesEndpoints() {
        // From "banana" (inclusive) to null (inclusive) should contain banana, cherry, null.
        NavigableSet<String> sub = set.subSet("banana", true, null, true);
        assertEquals(3, sub.size(), "Subset size should be 3 (banana, cherry, null)");
        assertTrue(sub.contains("banana"));
        assertTrue(sub.contains("cherry"));
        assertTrue(sub.contains(null));
        // Verify ordering – natural order places null last.
        Iterator<String> it = sub.iterator();
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void subSet_ExclusiveUpperBound_ExcludesUpperEndpoint() {
        // From "banana" (inclusive) to "cherry" (exclusive) should contain only banana.
        NavigableSet<String> sub = set.subSet("banana", true, "cherry", false);
        assertEquals(1, sub.size());
        assertTrue(sub.contains("banana"));
        assertFalse(sub.contains("cherry"));
    }

    @Test
    void subSet_ExclusiveLowerBound_ExcludesLowerEndpoint() {
        // From "banana" (exclusive) to null (inclusive) should contain cherry and null.
        NavigableSet<String> sub = set.subSet("banana", false, null, true);
        assertEquals(2, sub.size());
        assertFalse(sub.contains("banana"));
        assertTrue(sub.contains("cherry"));
        assertTrue(sub.contains(null));
    }

    @Test
    void subSet_BothBoundsExclusive_EmptyWhenAdjacent() {
        // From "banana" (exclusive) to "cherry" (exclusive) – no elements lie strictly between.
        NavigableSet<String> sub = set.subSet("banana", false, "cherry", false);
        assertTrue(sub.isEmpty(), "Subset should be empty because there are no elements strictly between banana and cherry");
    }

    @Test
    void subSet_NullLowerAndUpperBounds_OnlyNullIsReturned() {
        // Both bounds are null and inclusive – the subset should contain only the null element.
        NavigableSet<String> sub = set.subSet(null, true, null, true);
        assertEquals(1, sub.size());
        assertTrue(sub.contains(null));
    }

    @Test
    void subSet_NullLowerBound_ExclusiveUpper() {
        // From null (exclusive) to "banana" (inclusive) should contain "apple" only.
        NavigableSet<String> sub = set.subSet(null, false, "banana", true);
        assertEquals(1, sub.size());
        assertTrue(sub.contains("apple"));
        assertFalse(sub.contains(null));
        assertFalse(sub.contains("banana"));
    }

    @Test
    void subSet_WithCustomComparator_RespectsOrdering() {
        // Comparator that orders strings by length, null is treated as length 0.
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> lengthSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        lengthSet.addAll(Arrays.asList("a", "bb", "ccc", null, "dddd"));
        // Subset from "bb" (inclusive) to "dddd" (exclusive) – should contain "bb", "ccc", null.
        NavigableSet<String> sub = lengthSet.subSet("bb", true, "dddd", false);
        assertEquals(3, sub.size());
        assertTrue(sub.contains("bb"));
        assertTrue(sub.contains("ccc"));
        assertTrue(sub.contains(null));
        // Verify ordering by length: null (0), "bb" (2), "ccc" (3)
        Iterator<String> it = sub.iterator();
        assertNull(it.next());
        assertEquals("bb", it.next());
        assertEquals("ccc", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void subSet_InvalidRange_ThrowsIllegalArgumentException() {
        // "cherry" > "banana" in natural order, so requesting a subset where the lower bound is greater
        // than the upper bound should throw IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> set.subSet("cherry", true, "banana", true));
    }

    @Test
    void subSet_ResultIsIndependentFromOriginalSet_ModificationsDoNotAffectEachOther() {
        // Create a subset and then modify the original set – the subset view should reflect those changes
        // because it is backed by the same internal set (standard NavigableSet contract).
        NavigableSet<String> sub = set.subSet("banana", true, null, true);
        assertTrue(sub.contains(null));
        // Remove null from the original set.
        set.remove(null);
        assertFalse(sub.contains(null), "Subset should reflect removal of null from the backing set");
        // Add a new element that falls inside the subset range.
        set.add("blueberry");
        assertTrue(sub.contains("blueberry"));
    }

    @ParameterizedTest(name = "[{index}] from={0}({1}) to={2}({3}) → {4}")
    @MethodSource("nonNullInclusiveProvider")
    void subSet_NonNullInclusiveFlags(Integer from, boolean fromInc, Integer to, boolean toInc, List<Integer> expected) {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        // Populate with a deterministic range that covers the test values.
        IntStream.rangeClosed(1, 5).forEach(intSet::add);
        NavigableSet<Integer> sub = intSet.subSet(from, fromInc, to, toInc);
        assertSubsetEquals(sub, expected);
    }

    @ParameterizedTest(name = "[{index}] from={0}({1}) to={2}({3}) → {4}")
    @MethodSource("nullBoundsProvider")
    void subSet_NullBounds(Integer from, boolean fromInc, Integer to, boolean toInc, List<Integer> expected) {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        // Populate with 1‑5 and a null sentinel.
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        intSet.add(null);
        NavigableSet<Integer> sub = intSet.subSet(from, fromInc, to, toInc);
        assertSubsetEquals(sub, expected);
    }

    @ParameterizedTest(name = "[{index}] from={0}({1}) to={2}({3}) → {4}")
    @MethodSource("equalBoundsProvider")
    void subSet_EqualBounds(Integer from, boolean fromInc, Integer to, boolean toInc, List<Integer> expected) {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        NavigableSet<Integer> sub = intSet.subSet(from, fromInc, to, toInc);
        assertSubsetEquals(sub, expected);
    }

    @ParameterizedTest(name = "[{index}] from={0}({1}) to={2}({3}) → IllegalArgumentException")
    @MethodSource("reversedRangeProvider")
    void subSet_ReversedRangeThrows(Integer from, boolean fromInc, Integer to, boolean toInc) {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        assertThrows(IllegalArgumentException.class, () -> intSet.subSet(from, fromInc, to, toInc));
    }

    // -----------------------------------------------------------------
    @Test
    void subSet_OnEmptySet_ReturnsEmpty() {
        NavigableSet<Integer> empty = new ConcurrentNavigableSetNullSafe<>();
        NavigableSet<Integer> sub = empty.subSet(1, true, 5, true);
        assertTrue(sub.isEmpty(), "Subset of an empty set must be empty");
    }

    @ParameterizedTest(name = "[{index}] from={0}({1}) to={2}({3}) → {4}")
    @MethodSource("singleElementProvider")
    void subSet_SingleElementSet(Integer from, boolean fromInc, Integer to, boolean toInc, List<Integer> expected) {
        NavigableSet<Integer> single = new ConcurrentNavigableSetNullSafe<>();
        single.add(10);
        NavigableSet<Integer> sub = single.subSet(from, fromInc, to, toInc);
        assertSubsetEquals(sub, expected);
    }

    @ParameterizedTest(name = "[{index}] comparator=descending, from={0}({1}) to={2}({3}) → {4}")
    @MethodSource("descendingComparatorProvider")
    void subSet_DescendingComparator(Comparator<Integer> comparator, Integer from, boolean fromInc, Integer to, boolean toInc, List<Integer> expected) {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>(comparator);
        // Populate with 1‑5 and a null sentinel.
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        intSet.add(null);
        NavigableSet<Integer> sub = intSet.subSet(from, fromInc, to, toInc);
        assertSubsetEquals(sub, expected);
    }

    // -----------------------------------------------------------------
    @Test
    void subSet_IncompatibleLowerBound_ThrowsClassCastException() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3));
        // Lower bound is a String – should cause ClassCastException when comparator tries to compare.
        assertThrows(ClassCastException.class, () -> intSet.subSet("string", true, 5, true));
    }

    @Test
    void subSet_IncompatibleUpperBound_ThrowsClassCastException() {
        NavigableSet<String> strSet = new ConcurrentNavigableSetNullSafe<>();
        strSet.addAll(Arrays.asList("a", "b", "c"));
        // Upper bound is an Integer – should cause ClassCastException.
        assertThrows(ClassCastException.class, () -> strSet.subSet("a", true, 5, true));
    }
}
