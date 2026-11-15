package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_higher_Test {

    /**
     * Set that uses natural ordering (the default constructor).
     */
    private NavigableSet<String> naturalSet;

    /**
     * Set that uses a length‑based comparator.
     */
    private NavigableSet<String> lengthSet;

    @BeforeEach
    void setupBeforeEach() {
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        lengthSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid cross‑test contamination.
        if (naturalSet != null) {
            naturalSet.clear();
        }
        if (lengthSet != null) {
            lengthSet.clear();
        }
    }

    // ---------- basic ordering with non‑null elements ----------
    private static Stream<Arguments> basicOrderingProvider() {
        return Stream.of(Arguments.of(Arrays.asList(1, 2, 3, 4), 2, 3, "Set contains [1,2,3,4]; higher(2) should return the next greater element 3."), Arguments.of(Arrays.asList(1, 2, 3, 4), 4, null, "Set contains [1,2,3,4]; higher(4) has no element greater than 4, so it should return null."));
    }

    // ---------- null element handling ----------
    private static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(// Set contains [null, 1, 2]; higher(null) → 1
        Arguments.of(Arrays.asList(null, 1, 2), null, 1, "Set contains [null, 1, 2]; higher(null) should return the smallest non‑null element 1."), // Set contains only [null]; higher(null) → null
        Arguments.of(Collections.singletonList(null), null, null, "Set contains only [null]; higher(null) has no element greater than the masked null, so it should return null."), // Empty set; higher(null) → null
        Arguments.of(Collections.emptyList(), null, null, "Empty set; higher(null) should return null."));
    }

    // ---------- element not present in set ----------
    private static Stream<Arguments> missingElementProvider() {
        return Stream.of(Arguments.of(Arrays.asList(1, 3, 5), 2, 3, "Set contains [1,3,5]; higher(2) should return the next greater element 3."), Arguments.of(Arrays.asList(1, 3, 5), 4, 5, "Set contains [1,3,5]; higher(4) should return the next greater element 5."), Arguments.of(Arrays.asList(1, 3, 5), 6, null, "Set contains [1,3,5]; higher(6) has no element greater than 6, so it should return null."));
    }

    @Test
    void higher_NaturalOrder_BasicCases() {
        // Populate the set – note that null is allowed.
        naturalSet.add("apple");
        naturalSet.add("banana");
        naturalSet.add("cherry");
        naturalSet.add(null);
        // "banana" → next higher element is "cherry"
        assertEquals("cherry", naturalSet.higher("banana"));
        // "cherry" is the greatest element, so higher should be null
        assertNull(naturalSet.higher("cherry"));
        // For null, there is no element greater than the sentinel used for null
        assertNull(naturalSet.higher(null));
    }

    @Test
    void higher_NaturalOrder_EmptySet() {
        // Empty set – any call to higher must return null
        assertNull(naturalSet.higher("anything"));
        assertNull(naturalSet.higher(null));
    }

    @Test
    void higher_NaturalOrder_NullElementBehavior() {
        // Only null present
        naturalSet.add(null);
        // lower(null) would be null (no element less than sentinel)
        // higher(null) must also be null because there is no element greater than sentinel
        assertNull(naturalSet.higher(null));
        // Adding a non‑null element makes it lower than the sentinel
        naturalSet.add("apple");
        // Now higher(null) is still null (sentinel is considered the greatest)
        assertNull(naturalSet.higher(null));
        // higher("apple") should be null because "apple" is the greatest non‑null element
        assertNull(naturalSet.higher("apple"));
    }

    @Test
    void higher_LengthComparator_BasicCases() {
        // Length order: null (length 0), "kiwi" (4), "apple" (5), "banana" (6)
        lengthSet.add(null);
        lengthSet.add("kiwi");
        lengthSet.add("apple");
        lengthSet.add("banana");
        // higher(null) → the smallest non‑null element, i.e. "kiwi"
        assertEquals("kiwi", lengthSet.higher(null));
        // higher("kiwi") → "apple"
        assertEquals("apple", lengthSet.higher("kiwi"));
        // higher("banana") → null because "banana" is the longest element
        assertNull(lengthSet.higher("banana"));
    }

    @Test
    void higher_LengthComparator_WithOnlyNull() {
        lengthSet.add(null);
        // With only the sentinel present, higher(null) must be null
        assertNull(lengthSet.higher(null));
    }

    @Test
    void higher_LengthComparator_MissingElement() {
        // Set does not contain the argument; higher should behave as if the argument
        // were inserted in order and then return the next element.
        lengthSet.add("apple");
        lengthSet.add("banana");
        // Query for an element that would sort between them ("kiwi" length 4)
        assertEquals("banana", lengthSet.higher("kiwi"));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("basicOrderingProvider")
    void higher_BasicOrdering(List<Integer> elements, Integer query, Integer expected, String description) {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(elements);
        assertEquals(expected, set.higher(query), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("nullHandlingProvider")
    void higher_NullElementHandling(List<Integer> elements, Integer query, Integer expected, String description) {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        // The set implementation accepts null, so we can add directly.
        set.addAll(elements);
        assertEquals(expected, set.higher(query), description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("missingElementProvider")
    void higher_ElementNotPresent(List<Integer> elements, Integer query, Integer expected, String description) {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(elements);
        assertEquals(expected, set.higher(query), description);
    }

    // ---------- element smaller than minimum ----------
    @Test
    void higher_ElementSmallerThanMinimum() {
        // Set contains [10,20]; higher(5) should return 10
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(10, 20));
        assertEquals(10, set.higher(5), "Set contains [10,20]; higher(5) should return the smallest element 10.");
    }

    // ---------- custom comparator (descending order) ----------
    @Test
    void higher_CustomComparator_DescendingOrder() {
        // Comparator that orders integers in descending order
        Comparator<Integer> descending = Comparator.reverseOrder();
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>(descending);
        // natural order would be [1,3,5]; descending makes it [5,3,1]
        set.addAll(Arrays.asList(5, 3, 1));
        // In descending order, the "higher" element is the one that would appear later
        // i.e., a smaller numeric value.
        assertEquals(1, set.higher(3), "Set uses a descending comparator and contains [5,3,1]; higher(3) should return 1.");
        assertNull(set.higher(5), "With the same descending set, higher(5) has no element greater than 5 in descending order, so it should return null.");
    }

    // ---------- single element set ----------
    @Test
    void higher_SingleElementSet() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.add(42);
        // higher(42) → null because there is no greater element
        assertNull(set.higher(42), "Set contains only [42]; higher(42) should return null because there is no greater element.");
        // higher(0) → 42 because 0 is less than the sole element
        assertEquals(42, set.higher(0), "Set contains only [42]; higher(0) should return 42, the sole element.");
    }
}
