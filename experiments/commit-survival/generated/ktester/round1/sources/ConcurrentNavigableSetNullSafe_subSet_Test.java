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
class ConcurrentNavigableSetNullSafe_subSet_Test {

    /**
     * The set used in most tests – populated in {@link #setupBeforeEach()}
     */
    private NavigableSet<String> set;

    /**
     * A set that uses a length‑based comparator – created only when needed
     */
    private NavigableSet<String> lengthSet;

    /**
     * A simple alphabetic set used for the JSON‑driven parameterised tests
     */
    private NavigableSet<String> alphaSet;

    @BeforeEach
    void setupBeforeEach() {
        // Natural ordering set with a mixture of values and a null element.
        set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
        set.add("date");
        // Comparator‑based set (ordered by string length, null is treated as length 0).
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        lengthSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        lengthSet.add(null);
        // length 4
        lengthSet.add("kiwi");
        // length 5
        lengthSet.add("apple");
        // length 6
        lengthSet.add("banana");
        // Alphabetic set for the exhaustive JSON test cases.
        alphaSet = new ConcurrentNavigableSetNullSafe<>();
        alphaSet.add("a");
        alphaSet.add("b");
        alphaSet.add("c");
        alphaSet.add("d");
        alphaSet.add("e");
        alphaSet.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the collections to avoid cross‑test contamination.
        if (set != null) {
            set.clear();
        }
        if (lengthSet != null) {
            lengthSet.clear();
        }
        if (alphaSet != null) {
            alphaSet.clear();
        }
    }

    /**
     * Supplies arguments for {@link #subSet_JsonDrivenScenarios}.
     *
     * The data mirrors the JSON document supplied with the task.
     */
    private static Stream<Arguments> provideSubSetScenarios() {
        return Stream.of(// group: subSet - non‑null bounds, various inclusivity
        Arguments.of("b", true, "d", false, Arrays.asList("b", "c")), Arguments.of(null, true, "c", true, Arrays.asList(null, "a", "b", "c")), Arguments.of("c", false, null, false, Arrays.asList("d", "e")), // group: subSet - null bounds edge cases
        Arguments.of(null, false, "c", true, Arrays.asList("a", "b", "c")), Arguments.of("d", true, null, true, Arrays.asList("d", "e", null)), Arguments.of(null, true, null, false, Arrays.asList(null, "a", "b", "c", "d", "e")), Arguments.of(null, false, null, true, Arrays.asList("a", "b", "c", "d", "e", null)), Arguments.of("e", false, null, false, Collections.emptyList()));
    }

    @Test
    void subSet_InclusiveBothBounds_IncludesAllElementsBetween() {
        // from "banana" (inclusive) to "date" (exclusive) → banana, cherry, null
        NavigableSet<String> sub = set.subSet("banana", true, "date", false);
        assertEquals(3, sub.size());
        assertTrue(sub.contains("banana"));
        assertTrue(sub.contains("cherry"));
        assertTrue(sub.contains(null));
        assertFalse(sub.contains("apple"));
        assertFalse(sub.contains("date"));
    }

    @Test
    void subSet_ExclusiveFrom_IncludesElementsAfterFrom() {
        // from "banana" (exclusive) to "date" (inclusive) → cherry, null, date
        NavigableSet<String> sub = set.subSet("banana", false, "date", true);
        assertEquals(3, sub.size());
        assertTrue(sub.contains("cherry"));
        assertTrue(sub.contains(null));
        assertTrue(sub.contains("date"));
        assertFalse(sub.contains("banana"));
    }

    @Test
    void subSet_WithNullBounds_InclusiveBoth() {
        // from null (inclusive) to null (inclusive) → only the null element
        NavigableSet<String> onlyNull = set.subSet(null, true, null, true);
        assertEquals(1, onlyNull.size());
        assertTrue(onlyNull.contains(null));
        // from "banana" (inclusive) to null (inclusive) → banana, cherry, null
        NavigableSet<String> toNull = set.subSet("banana", true, null, true);
        assertEquals(3, toNull.size());
        assertTrue(toNull.containsAll(Arrays.asList("banana", "cherry", null)));
    }

    @Test
    void subSet_EmptyResult_WhenUpperBoundIsBeforeLowerBound() {
        // Upper bound is before lower bound → empty set
        NavigableSet<String> empty = set.subSet("date", true, "apple", true);
        assertTrue(empty.isEmpty());
    }

    @Test
    void subSet_FullRange_ReturnsViewThatReflectsParentModifications() {
        // Full range view (null inclusive to null exclusive yields the whole set)
        NavigableSet<String> full = set.subSet(null, true, null, false);
        assertEquals(set.size(), full.size());
        // Mutate the parent set and verify the view sees the change
        set.add("elderberry");
        assertTrue(full.contains("elderberry"));
        assertEquals(set.size(), full.size());
        // Mutate the view and verify the parent set sees the change
        full.remove("apple");
        assertFalse(set.contains("apple"));
        assertEquals(set.size(), full.size());
    }

    @Test
    void subSet_WithComparator_InclusiveBothBounds_ByLength() {
        // Using length comparator: null (len 0) → kiwi (len 4) inclusive
        NavigableSet<String> sub = lengthSet.subSet(null, true, "kiwi", true);
        assertEquals(2, sub.size());
        assertTrue(sub.contains(null));
        assertTrue(sub.contains("kiwi"));
        assertFalse(sub.contains("apple"));
    }

    @Test
    void subSet_WithComparator_ExclusiveUpperBound() {
        // From null (inclusive) to "apple" (exclusive) → null, kiwi
        NavigableSet<String> sub = lengthSet.subSet(null, true, "apple", false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains(null));
        assertTrue(sub.contains("kiwi"));
        assertFalse(sub.contains("apple"));
    }

    @Test
    void subSet_WithComparator_EmptyWhenNoElementsMatch() {
        // From "banana" (exclusive) to null (inclusive) → empty because
        // comparator orders by length and all lengths are >= 0.
        NavigableSet<String> sub = lengthSet.subSet("banana", false, null, true);
        assertTrue(sub.isEmpty());
    }

    /**
     * Executes the exhaustive subSet scenarios described in the JSON test‑case document.
     *
     * @param fromElement   lower bound (may be {@code null})
     * @param fromInclusive whether the lower bound is inclusive
     * @param toElement     upper bound (may be {@code null})
     * @param toInclusive   whether the upper bound is inclusive
     * @param expected      list of expected elements in iteration order
     */
    @ParameterizedTest(name = "{index} ⇒ from={0} ({1}), to={2} ({3}) → {4}")
    @MethodSource("provideSubSetScenarios")
    void subSet_JsonDrivenScenarios(String fromElement, boolean fromInclusive, String toElement, boolean toInclusive, List<String> expected) {
        NavigableSet<String> sub = alphaSet.subSet(fromElement, fromInclusive, toElement, toInclusive);
        // Verify size first – a helpful diagnostic if the content check fails.
        assertEquals(expected.size(), sub.size(), () -> "Expected size " + expected.size() + " but got " + sub.size());
        // Verify the exact iteration order (NavigableSet guarantees sorted order).
        assertIterableEquals(expected, sub, () -> "Subset contents differ. Expected: " + expected + " Actual: " + new ArrayList<>(sub));
    }
}
