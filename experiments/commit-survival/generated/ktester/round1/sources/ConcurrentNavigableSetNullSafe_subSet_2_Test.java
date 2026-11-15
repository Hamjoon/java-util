package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_subSet_2_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private NavigableSet<String> set;

    /**
     * The set under test – created fresh for each test method.
     */
    private NavigableSet<Integer> intSet;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural ordering) constructor.
        set = new ConcurrentNavigableSetNullSafe<>();
        // Populate with a deterministic ordering that includes a null element.
        // Natural ordering puts null (masked internally) after all non‑null values.
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared so that each test starts from a clean state.
        set.clear();
    }

    private static Stream<Arguments> normalRangeProvider() {
        return Stream.of(// Typical range where from < to
        Arguments.of(2, 4, Arrays.asList(2, 3)), // Range covering most of the set
        Arguments.of(1, 5, Arrays.asList(1, 2, 3, 4)), // Empty range where from == to
        Arguments.of(3, 3, Collections.emptyList()), // Single‑element exclusive upper bound
        Arguments.of(2, 3, Collections.singletonList(2)), // Lower bound not present – starts at next higher element
        Arguments.of(2, 5, Arrays.asList(3, 4)), // Upper bound not present – stops before the next higher element
        // same as previous but demonstrates inclusive lower
        Arguments.of(2, 5, Arrays.asList(2, 3, 4)));
    }

    private static Stream<Arguments> invalidRangeProvider() {
        return Stream.of(Arguments.of(5, 3), Arguments.of(10, 2));
    }

    private static Stream<Arguments> typeMismatchProvider() {
        return Stream.of(// String bounds vs Integer set
        Arguments.of("a", "z"), // Mixed types: String lower, Integer upper
        Arguments.of("1", 2), // Date objects vs Integer set
        Arguments.of(new Date(), new Date(1000L)));
    }

    /**
     * The two‑argument {@code subSet} method should be equivalent to
     * {@code subSet(fromElement, true, toElement, false)}.
     */
    @Test
    void testSubSetDefaultInclusiveExclusive() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("apple", "banana", "cherry", null));
        SortedSet<String> sub = set.subSet("banana", "cherry");
        assertEquals(1, sub.size(), "Expected only one element in the subset");
        assertTrue(sub.contains("banana"));
        assertFalse(sub.contains("cherry"));
        assertFalse(sub.contains(null));
    }

    /**
     * Verify that a {@code null} upper bound works correctly with the default
     * exclusive semantics (the upper bound itself is not included).
     */
    @Test
    void testSubSetWithNullUpperBound() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("apple", "banana", "cherry", null));
        SortedSet<String> sub = set.subSet("banana", null);
        assertEquals(2, sub.size(), "Subset should contain banana and cherry");
        assertTrue(sub.contains("banana"));
        assertTrue(sub.contains("cherry"));
        assertFalse(sub.contains(null));
    }

    /**
     * Verify that a {@code null} lower bound works correctly with the default
     * inclusive semantics (the lower bound is always included).
     */
    @Test
    void testSubSetWithNullLowerBound() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("apple", "banana", "cherry", null));
        SortedSet<String> sub = set.subSet(null, "banana");
        assertEquals(2, sub.size(), "Subset should contain null and apple");
        assertTrue(sub.contains(null));
        assertTrue(sub.contains("apple"));
        assertFalse(sub.contains("banana"));
    }

    /**
     * When the lower bound is greater than the upper bound the subset should be empty.
     */
    @Test
    void testSubSetEmptyWhenFromGreaterThanTo() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("apple", "banana", "cherry", null));
        SortedSet<String> sub = set.subSet("cherry", "banana");
        assertTrue(sub.isEmpty(), "Subset should be empty when fromElement > toElement");
    }

    /**
     * The same semantics should hold when a custom comparator is used.
     */
    @Test
    void testSubSetWithComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> compSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // Add elements in arbitrary order; internal ordering is by length.
        // length 4
        compSet.add("kiwi");
        // length 5
        compSet.add("apple");
        // length 6
        compSet.add("banana");
        // length 0
        compSet.add(null);
        // From "kiwi" (inclusive) to "banana" (exclusive) → should contain "kiwi" and "apple".
        SortedSet<String> sub = compSet.subSet("kiwi", "banana");
        assertEquals(2, sub.size(), "Subset should contain kiwi and apple");
        assertTrue(sub.contains("kiwi"));
        assertTrue(sub.contains("apple"));
        assertFalse(sub.contains("banana"));
        assertFalse(sub.contains(null));
    }

    /**
     * Normal behaviour – inclusive lower bound, exclusive upper bound.
     */
    @ParameterizedTest(name = "[{index}] subSet({0}, {1}) → {2}")
    @MethodSource("normalRangeProvider")
    void testSubSetNormalBehavior(Integer from, Integer to, List<Integer> expected) {
        SortedSet<Integer> sub = intSet.subSet(from, to);
        assertEquals(expected.size(), sub.size(), () -> "Expected subset size " + expected.size() + " but got " + sub.size());
        assertIterableEquals(expected, sub, () -> "Subset elements differ. Expected " + expected + " but got " + sub);
    }

    /**
     * Null handling – the implementation permits {@code null} bounds.
     * The subset should behave as if {@code null} is the smallest (or largest)
     * element depending on the bound side.
     */
    @Test
    void testSubSetWithNullBoundsOnIntegerSet() {
        // Insert a null element explicitly.
        intSet.add(null);
        // From null (inclusive) to 3 (exclusive) → should contain null, 1, 2
        SortedSet<Integer> sub = intSet.subSet(null, 3);
        assertEquals(3, sub.size(), "Subset should contain null, 1 and 2");
        assertTrue(sub.contains(null));
        assertTrue(sub.contains(1));
        assertTrue(sub.contains(2));
        assertFalse(sub.contains(3));
    }

    /**
     * Invalid range where lower bound is greater than upper bound – should throw
     * {@link IllegalArgumentException} (consistent with {@link java.util.TreeSet}).
     */
    @ParameterizedTest(name = "[{index}] subSet({0}, {1}) throws IllegalArgumentException")
    @MethodSource("invalidRangeProvider")
    void testSubSetInvalidRange(Integer from, Integer to) {
        assertThrows(IllegalArgumentException.class, () -> intSet.subSet(from, to), () -> "Expected IllegalArgumentException for range (" + from + ", " + to + ")");
    }

    /**
     * Empty underlying set – any subSet call must return an empty set.
     */
    @Test
    void testSubSetOnEmptySet() {
        NavigableSet<Integer> emptySet = new ConcurrentNavigableSetNullSafe<>();
        SortedSet<Integer> sub = emptySet.subSet(1, 2);
        assertTrue(sub.isEmpty(), "Subset of an empty set should be empty");
    }

    /**
     * Type‑mismatch scenarios – the underlying set holds {@code Integer}s,
     * providing a {@code String} or other incompatible type should result in
     * {@link ClassCastException}.
     */
    @ParameterizedTest(name = "[{index}] subSet({0}, {1}) throws ClassCastException")
    @MethodSource("typeMismatchProvider")
    void testSubSetTypeMismatch(Object from, Object to) {
        assertThrows(ClassCastException.class, () -> intSet.subSet((Integer) from, (Integer) to), () -> "Expected ClassCastException for bounds (" + from + ", " + to + ")");
    }
}
