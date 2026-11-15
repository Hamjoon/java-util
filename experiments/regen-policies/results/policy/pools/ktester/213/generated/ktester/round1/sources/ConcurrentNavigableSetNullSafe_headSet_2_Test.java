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
class ConcurrentNavigableSetNullSafe_headSet_2_Test {

    private NavigableSet<String> naturalSet;

    private NavigableSet<String> lengthComparatorSet;

    @BeforeEach
    void setupBeforeEach() {
        // Set up a set with natural ordering and a few elements including null
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        naturalSet.add("apple");
        naturalSet.add("banana");
        naturalSet.add("cherry");
        naturalSet.add(null);
        // Set up a set that uses a length‑based comparator
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        lengthComparatorSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        lengthComparatorSet.add(null);
        // length 4
        lengthComparatorSet.add("kiwi");
        // length 5
        lengthComparatorSet.add("apple");
        // length 6
        lengthComparatorSet.add("banana");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid cross‑test contamination
        naturalSet.clear();
        lengthComparatorSet.clear();
    }

    /**
     * Helper to create a {@link ConcurrentNavigableSetNullSafe} populated with the given integers.
     */
    private static NavigableSet<Integer> createIntegerSet(Integer... values) {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        Collections.addAll(set, values);
        return set;
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> basicRangeProvider() {
        return Stream.of(Arguments.of(new Integer[] { 1, 2, 3, 4, 5, 6, 7 }, 5, List.of(1, 2, 3, 4)), Arguments.of(new Integer[] { 1, 2, 3, 4, 5, 6, 7 }, 10, List.of(1, 2, 3, 4, 5, 6, 7)), Arguments.of(new Integer[] { 1, 2, 3, 4, 5, 6, 7 }, 1, List.of()));
    }

    @Test
    void testHeadSetExclusiveWithNonNullBound_NaturalOrder() {
        // exclusive
        SortedSet<String> head = naturalSet.headSet("banana");
        // Expected: only "apple" (null is greater than any non‑null in natural order)
        assertEquals(1, head.size());
        assertTrue(head.contains("apple"));
        assertFalse(head.contains("banana"));
        assertFalse(head.contains(null));
        // Verify ordering
        Iterator<String> it = head.iterator();
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());
        // Mutating the view should affect the original set
        assertTrue(head.add("avocado"));
        assertTrue(naturalSet.contains("avocado"));
        assertEquals(5, naturalSet.size());
    }

    @Test
    void testHeadSetExclusiveWithNullBound_NaturalOrder() {
        // null is the greatest element in natural ordering, so everything else is < null
        // exclusive
        SortedSet<String> head = naturalSet.headSet(null);
        assertEquals(3, head.size());
        assertTrue(head.containsAll(Arrays.asList("apple", "banana", "cherry")));
        assertFalse(head.contains(null));
        // The view should be sorted in natural order
        Iterator<String> it = head.iterator();
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testHeadSetExclusiveWithAbsentBound_NaturalOrder() {
        // "blueberry" is not present but would sort between "banana" and "cherry"
        // exclusive
        SortedSet<String> head = naturalSet.headSet("blueberry");
        assertEquals(2, head.size());
        assertTrue(head.containsAll(Arrays.asList("apple", "banana")));
        assertFalse(head.contains("cherry"));
        assertFalse(head.contains(null));
    }

    @Test
    void testHeadSetExclusiveWithNullBound_LengthComparator() {
        // With the length comparator, null (length 0) is the smallest element.
        // Therefore headSet(null) should be empty (exclusive bound).
        // exclusive
        SortedSet<String> head = lengthComparatorSet.headSet(null);
        assertTrue(head.isEmpty());
        // Adding to the empty view should not affect the original set
        assertThrows(UnsupportedOperationException.class, () -> head.add("new"));
    }

    @Test
    void testHeadSetExclusiveWithNonNullBound_LengthComparator() {
        // Using the length comparator, headSet("apple") (length 5) should contain
        // null (length 0) and "kiwi" (length 4) – both are less than length 5.
        // exclusive
        SortedSet<String> head = lengthComparatorSet.headSet("apple");
        assertEquals(2, head.size());
        assertTrue(head.contains(null));
        assertTrue(head.contains("kiwi"));
        assertFalse(head.contains("apple"));
        assertFalse(head.contains("banana"));
        // Verify ordering according to the comparator
        Iterator<String> it = head.iterator();
        // length 0
        assertEquals(null, it.next());
        // length 4
        assertEquals("kiwi", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testHeadSetViewIsBackedByOriginalSet() {
        // exclusive
        SortedSet<String> head = naturalSet.headSet("cherry");
        assertTrue(head.remove("banana"));
        assertFalse(naturalSet.contains("banana"));
        assertEquals(2, naturalSet.size());
        // Adding an element that belongs to the view should reflect in the original set
        assertTrue(head.add("avocado"));
        assertTrue(naturalSet.contains("avocado"));
    }

    @ParameterizedTest(name = "headSet({2}) on {0} => {3}")
    @MethodSource("basicRangeProvider")
    void testHeadSetBasicFunctionality(Integer[] initial, Integer toElement, List<Integer> expected) {
        NavigableSet<Integer> set = createIntegerSet(initial);
        // exclusive
        SortedSet<Integer> head = set.headSet(toElement);
        assertEquals(expected.size(), head.size(), "Unexpected size for headSet(" + toElement + ")");
        assertEquals(expected, new ArrayList<>(head), "Head set contents do not match expected");
    }

    // -----------------------------------------------------------------
    @Test
    void testHeadSetNullBound_WithNullsFirstComparator() {
        Comparator<Integer> nullsFirst = Comparator.nullsFirst(Integer::compareTo);
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>(nullsFirst);
        set.add(null);
        set.add(5);
        set.add(10);
        // exclusive
        SortedSet<Integer> head = set.headSet(null);
        assertTrue(head.isEmpty(), "headSet(null) with nullsFirst comparator must be empty");
    }

    // -----------------------------------------------------------------
    @Test
    void testHeadSetOnEmptySet() {
        NavigableSet<Integer> empty = new ConcurrentNavigableSetNullSafe<>();
        SortedSet<Integer> head = empty.headSet(5);
        assertTrue(head.isEmpty(), "headSet on an empty set must be empty");
    }

    // -----------------------------------------------------------------
    @Test
    void testHeadSetInvalidTypeThrowsClassCastException() {
        NavigableSet<Integer> intSet = createIntegerSet(1, 2, 3);
        assertThrows(ClassCastException.class, () -> intSet.headSet((Integer) (Object) "string"));
    }

    // -----------------------------------------------------------------
    @Test
    void testHeadSetBoundEqualsGreatestElement() {
        NavigableSet<Integer> set = createIntegerSet(1, 2, 3, 4, 5, 6, 7);
        // exclusive
        SortedSet<Integer> head = set.headSet(7);
        assertEquals(List.of(1, 2, 3, 4, 5, 6), new ArrayList<>(head));
    }

    // -----------------------------------------------------------------
    @Test
    void testHeadSetSingleElementEqualBound() {
        NavigableSet<Integer> set = createIntegerSet(42);
        // exclusive
        SortedSet<Integer> head = set.headSet(42);
        assertTrue(head.isEmpty(), "headSet(42) on a set containing only 42 must be empty");
    }

    @Test
    void testHeadSetSingleElementGreaterBound() {
        NavigableSet<Integer> set = createIntegerSet(42);
        // exclusive
        SortedSet<Integer> head = set.headSet(100);
        assertEquals(List.of(42), new ArrayList<>(head));
    }

    // -----------------------------------------------------------------
    @Test
    void testHeadSetWithNullElementAndNonNullBound() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.add(null);
        set.add(1);
        set.add(2);
        // exclusive, null < 1 < 2
        SortedSet<Integer> head = set.headSet(2);
        assertEquals(List.of(null, 1), new ArrayList<>(head));
    }

    // -----------------------------------------------------------------
    @Test
    void testAddEqualToBoundThrowsIllegalArgumentException() {
        NavigableSet<Integer> set = createIntegerSet(1, 2, 3, 4, 5);
        // exclusive
        SortedSet<Integer> head = set.headSet(4);
        assertThrows(IllegalArgumentException.class, () -> head.add(4), "Adding element equal to exclusive bound must throw IllegalArgumentException");
    }

    // -----------------------------------------------------------------
    @Test
    void testRemovalFromOriginalReflectedInView() {
        NavigableSet<Integer> set = createIntegerSet(1, 2, 3, 4, 5);
        // exclusive -> contains 1..4
        SortedSet<Integer> head = set.headSet(5);
        assertTrue(set.remove(4), "Remove 4 from original set");
        assertFalse(head.contains(4), "View must no longer contain removed element");
        assertEquals(List.of(1, 2, 3), new ArrayList<>(head));
    }

    // -----------------------------------------------------------------
    @Test
    void testClearViewRemovesOnlyRangeElements() {
        NavigableSet<Integer> set = createIntegerSet(1, 2, 3, 4, 5, 6, 7);
        // exclusive -> 1..4
        SortedSet<Integer> head = set.headSet(5);
        head.clear();
        // After clear, only elements >=5 should remain
        assertEquals(List.of(5, 6, 7), new ArrayList<>(set));
        // The view itself should now be empty
        assertTrue(head.isEmpty());
    }
}
