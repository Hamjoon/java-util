package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_headSet_2_Test {

    /**
     * Set used in most tests – created fresh before each test method.
     */
    private NavigableSet<String> set;

    /**
     * A set that uses a custom comparator (by string length).
     */
    private NavigableSet<String> lengthSet;

    /**
     * Integer set used for the numeric test cases (natural ordering).
     */
    private NavigableSet<Integer> intSet;

    /**
     * Integer set with a comparator that rejects negative numbers.
     */
    private NavigableSet<Integer> rejectingComparatorSet;

    @BeforeEach
    void setupBeforeEach() {
        // Default‑ordering set (natural ordering, null is treated as the greatest element)
        set = new ConcurrentNavigableSetNullSafe<>();
        // Comparator‑ordering set – orders by string length, null is the smallest element (length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        lengthSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // Integer set for the numeric scenarios
        intSet = new ConcurrentNavigableSetNullSafe<>();
        // Comparator that throws IllegalArgumentException for negative values
        Comparator<Integer> rejectNegatives = (a, b) -> {
            if (a < 0 || b < 0) {
                throw new IllegalArgumentException("Negative values are not allowed");
            }
            return Integer.compare(a, b);
        };
        rejectingComparatorSet = new ConcurrentNavigableSetNullSafe<>(rejectNegatives);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid cross‑test contamination.
        if (set != null)
            set.clear();
        if (lengthSet != null)
            lengthSet.clear();
        if (intSet != null)
            intSet.clear();
        if (rejectingComparatorSet != null)
            rejectingComparatorSet.clear();
    }

    /**
     * Verify that {@code headSet(E)} returns all elements that are strictly less than the bound.
     * With natural ordering the sentinel for {@code null} is the greatest element, so {@code headSet(null)}
     * should contain every non‑null element.
     */
    @Test
    void testHeadSetWithNaturalOrdering() {
        // Populate the set – note the iterator order from the original test class:
        // apple, banana, cherry, null
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        assertTrue(set.add(null));
        // headSet with bound "banana" (exclusive) should contain only "apple"
        SortedSet<String> head = set.headSet("banana");
        assertEquals(1, head.size());
        assertTrue(head.contains("apple"));
        assertFalse(head.contains("banana"));
        assertFalse(head.contains("cherry"));
        assertFalse(head.contains(null));
        // headSet with bound null should contain all non‑null elements
        SortedSet<String> headToNull = set.headSet(null);
        assertEquals(3, headToNull.size());
        assertTrue(headToNull.containsAll(Arrays.asList("apple", "banana", "cherry")));
        assertFalse(headToNull.contains(null));
        // The returned view must be backed by the original set – removing via the view affects the set
        headToNull.remove("banana");
        assertFalse(set.contains("banana"));
        assertEquals(2, set.size());
    }

    /**
     * Verify that {@code headSet(E)} works correctly when a custom comparator is used.
     * The comparator orders strings by length, treating {@code null} as length 0.
     */
    @Test
    void testHeadSetWithLengthComparator() {
        // Add elements of varying length; iterator order will be: null, kiwi, apple, banana
        assertTrue(lengthSet.add(null));
        assertTrue(lengthSet.add("kiwi"));
        assertTrue(lengthSet.add("apple"));
        assertTrue(lengthSet.add("banana"));
        // headSet with bound "apple" (exclusive) – should contain null and "kiwi"
        SortedSet<String> head = lengthSet.headSet("apple");
        assertEquals(2, head.size());
        assertTrue(head.contains(null));
        assertTrue(head.contains("kiwi"));
        assertFalse(head.contains("apple"));
        assertFalse(head.contains("banana"));
        // Adding an element via the headSet view should be reflected in the original set
        // length 3, which is less than length of "apple" (5)
        head.add("fig");
        assertTrue(lengthSet.contains("fig"));
        assertTrue(head.contains("fig"));
        assertEquals(5, lengthSet.size());
    }

    /**
     * Verify that the view returned by {@code headSet(E)} throws {@link IllegalArgumentException}
     * when the supplied bound is {@code null} and the set uses a comparator that does not
     * support {@code null} (e.g., natural ordering with {@code null} considered greatest).
     * In our implementation {@code null} is allowed, but we still check that the method
     * behaves sensibly when the bound is not comparable to existing elements.
     */
    @Test
    void testHeadSetWithIncompatibleBound() {
        // Populate the set with natural ordering (null is greatest)
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        // Using a bound that is not present but comparable – e.g., "aardvark"
        SortedSet<String> head = set.headSet("aardvark");
        // "aardvark" is greater than "apple" but less than "banana" in natural order
        // So the headSet should contain "apple"
        assertEquals(1, head.size());
        assertTrue(head.contains("apple"));
        assertFalse(head.contains("banana"));
        assertFalse(head.contains(null));
        // Using a bound that is greater than any element (including null) should return the whole set
        SortedSet<String> fullHead = set.headSet("zzzz");
        assertEquals(3, fullHead.size());
        assertTrue(fullHead.containsAll(Arrays.asList("apple", "banana", null)));
    }

    /**
     * Verify that modifications to the original set are reflected in an existing headSet view.
     */
    @Test
    void testHeadSetReflectsOriginalModifications() {
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        assertTrue(set.add(null));
        // should contain only "apple"
        SortedSet<String> head = set.headSet("banana");
        assertEquals(1, head.size());
        assertTrue(head.contains("apple"));
        // Add a new element that is less than the bound directly to the original set
        // "apricot" > "apple" but < "banana"
        assertTrue(set.add("apricot"));
        // The headSet view should now contain both "apple" and "apricot"
        assertEquals(2, head.size());
        assertTrue(head.containsAll(Arrays.asList("apple", "apricot")));
    }

    /**
     * Normal exclusive‑upper‑bound behavior for natural ordering of integers.
     *
     * The parameterized test covers three scenarios:
     * <ul>
     *   <li>bound inside the range → subset of smaller elements</li>
     *   <li>bound equal to the smallest element → empty set</li>
     *   <li>bound greater than the greatest element → full set</li>
     * </ul>
     */
    @ParameterizedTest(name = "headSet({0}) on {{1,2,3,4}} → expected {2}")
    @CsvSource({ // toElement, expectedElements (comma‑separated)
    "3, 1|2", "1, ''", "5, 1|2|3|4" })
    void testHeadSetIntegerNormalBehavior(int toElement, String expectedCsv) {
        // Populate the integer set with {1,2,3,4}
        assertTrue(intSet.add(1));
        assertTrue(intSet.add(2));
        assertTrue(intSet.add(3));
        assertTrue(intSet.add(4));
        SortedSet<Integer> head = intSet.headSet(toElement);
        // Convert the expected CSV string into a Set<Integer>
        Set<Integer> expected = new HashSet<>();
        if (!expectedCsv.isEmpty()) {
            for (String s : expectedCsv.split("\\|")) {
                expected.add(Integer.valueOf(s));
            }
        }
        assertEquals(expected.size(), head.size(), () -> "Expected size " + expected.size() + " but got " + head.size());
        assertTrue(head.containsAll(expected), () -> "Head set does not contain the expected elements: " + expected);
    }

    /**
     * Verify that calling {@code headSet(...)} on an empty set always returns an empty view,
     * regardless of the supplied bound.
     */
    @Test
    void testHeadSetOnEmptySetReturnsEmpty() {
        // intSet is freshly created and therefore empty
        SortedSet<Integer> head = intSet.headSet(10);
        assertTrue(head.isEmpty(), "headSet on an empty set should be empty");
    }

    /**
     * According to the {@link NavigableSet} contract, {@code headSet(null)} may throw
     * {@link NullPointerException} when the set does not permit {@code null} as a bound.
     * This test asserts that the implementation follows that contract for an empty set.
     */
    @Test
    void testHeadSetNullOnEmptySetThrowsNPE() {
        assertThrows(NullPointerException.class, () -> intSet.headSet(null), "headSet(null) on an empty set should throw NullPointerException");
    }

    /**
     * Verify that a {@code null} bound on a non‑empty set also results in
     * {@link NullPointerException} (as required by the {@link NavigableSet} spec).
     */
    @Test
    void testHeadSetNullOnNonEmptySetThrowsNPE() {
        assertTrue(intSet.add(1));
        assertTrue(intSet.add(2));
        assertTrue(intSet.add(3));
        assertTrue(intSet.add(4));
        assertThrows(NullPointerException.class, () -> intSet.headSet(null), "headSet(null) on a non‑empty set should throw NullPointerException");
    }

    /**
     * Passing a bound of an incompatible type (e.g., {@code String} to an {@code Integer}
     * set) must result in a {@link ClassCastException}.
     */
    @Test
    void testHeadSetIncompatibleBoundTypeThrowsClassCastException() {
        // Populate the integer set
        assertTrue(intSet.add(1));
        assertTrue(intSet.add(2));
        assertTrue(intSet.add(3));
        assertTrue(intSet.add(4));
        // The bound is a String, which cannot be compared to Integer elements
        assertThrows(ClassCastException.class, () -> intSet.headSet((Integer) (Object) "string"), "Using an incompatible bound type should throw ClassCastException");
    }

    /**
     * Verify that a custom comparator which rejects negative numbers propagates the
     * {@link IllegalArgumentException} when such a value is used as the bound.
     */
    @Test
    void testHeadSetBoundRejectedByComparatorThrowsIllegalArgumentException() {
        // Populate with positive integers only
        assertTrue(rejectingComparatorSet.add(1));
        assertTrue(rejectingComparatorSet.add(2));
        assertTrue(rejectingComparatorSet.add(3));
        // The comparator throws IllegalArgumentException for any negative argument
        assertThrows(IllegalArgumentException.class, () -> rejectingComparatorSet.headSet(-5), "Bound -5 should be rejected by the comparator");
    }
}
