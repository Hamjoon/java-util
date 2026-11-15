package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_first_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private NavigableSet<String> set;

    /**
     * A comparator that orders strings by length, treating {@code null} as the smallest element.
     */
    private static final Comparator<String> LENGTH_COMPARATOR = Comparator.comparingInt(s -> s == null ? 0 : s.length());

    /**
     * Comparator that orders strings in reverse (largest first).
     */
    private static final Comparator<String> REVERSE_COMPARATOR = Comparator.reverseOrder();

    @BeforeEach
    void setupBeforeEach() {
        // By default a natural‑ordering set is used; individual tests may replace it.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared so that a stray reference does not affect other tests.
        set.clear();
    }

    /**
     * Data for the “normal element” group – a single non‑null element.
     */
    static Stream<Arguments> normalElementProvider() {
        return Stream.of(Arguments.of("apple"));
    }

    /**
     * Data for the “multiple elements natural order” group.
     */
    static Stream<Arguments> naturalOrderProvider() {
        return Stream.of(Arguments.of(new String[] { "apple", "banana", "cherry" }, "apple"));
    }

    /**
     * Data for the “custom comparator reversed order” group.
     */
    static Stream<Arguments> reversedComparatorProvider() {
        return Stream.of(Arguments.of(new String[] { "apple", "banana", "cherry" }, "cherry"));
    }

    @Test
    void first_onEmptySet_throwsNoSuchElementException() {
        // GIVEN an empty set (created in @BeforeEach)
        // WHEN/THEN calling first() should throw NoSuchElementException
        assertThrows(NoSuchElementException.class, () -> set.first(), "first() on an empty NavigableSet must throw NoSuchElementException");
    }

    @Test
    void first_withNaturalOrdering_returnsSmallestElement() {
        // GIVEN a set populated with natural ordering elements, including null
        assertTrue(set.add("banana"));
        assertTrue(set.add("apple"));
        assertTrue(set.add(null));
        assertTrue(set.add("cherry"));
        // WHEN we call first()
        String first = set.first();
        // THEN the sentinel representing null should be unmasked and returned as null,
        //      because null is considered smaller than any non‑null element.
        assertNull(first, "With natural ordering, null should be the first element when present");
    }

    @Test
    void first_withoutNull_returnsFirstNonNullElement() {
        // GIVEN a set without null
        assertTrue(set.add("banana"));
        assertTrue(set.add("apple"));
        assertTrue(set.add("cherry"));
        // WHEN we call first()
        String first = set.first();
        // THEN the smallest element according to natural ordering ("apple") is returned.
        assertEquals("apple", first, "first() should return the lowest element according to natural order");
    }

    @Test
    void first_withCustomComparator_returnsAccordingToComparator() {
        // GIVEN a set constructed with a length‑based comparator
        set = new ConcurrentNavigableSetNullSafe<>(LENGTH_COMPARATOR);
        // length 4
        assertTrue(set.add("kiwi"));
        // length 5
        assertTrue(set.add("apple"));
        // length 6
        assertTrue(set.add("banana"));
        // length 0 (treated as smallest)
        assertTrue(set.add(null));
        // WHEN we call first()
        String first = set.first();
        // THEN null (the sentinel) should be returned because the comparator treats it as the smallest.
        assertNull(first, "Custom comparator should still return null as the first element when present");
    }

    @Test
    void first_withoutNull_customComparator_returnsShortestString() {
        // GIVEN a set with the same length comparator but without null
        set = new ConcurrentNavigableSetNullSafe<>(LENGTH_COMPARATOR);
        // length 4
        assertTrue(set.add("kiwi"));
        // length 5
        assertTrue(set.add("apple"));
        // length 6
        assertTrue(set.add("banana"));
        // WHEN we call first()
        String first = set.first();
        // THEN the shortest string ("kiwi") should be returned.
        assertEquals("kiwi", first, "first() should return the element with the smallest length");
    }

    @ParameterizedTest(name = "[{index}] first() returns ''{0}'' when only that element is present")
    @MethodSource("normalElementProvider")
    void first_normalElement_returnsIt(String element) {
        // GIVEN a set containing a single non‑null element
        assertTrue(set.add(element));
        // WHEN
        String first = set.first();
        // THEN the same element is returned
        assertEquals(element, first);
    }

    @Test
    void first_nullOnlyElement_returnsNull() {
        // GIVEN a set that contains only a null element
        assertTrue(set.add(null));
        // WHEN
        String first = set.first();
        // THEN the returned value is null (sentinel is correctly unmasked)
        assertNull(first, "When the set contains only null, first() must return null");
    }

    @ParameterizedTest(name = "[{index}] first() on natural order set returns ''{1}''")
    @MethodSource("naturalOrderProvider")
    void first_multipleElementsNaturalOrder(String[] elements, String expected) {
        // GIVEN a set populated with the supplied elements (natural ordering)
        for (String e : elements) {
            assertTrue(set.add(e));
        }
        // WHEN
        String first = set.first();
        // THEN the smallest element according to natural ordering is returned
        assertEquals(expected, first);
    }

    @ParameterizedTest(name = "[{index}] first() with reverse comparator returns ''{1}''")
    @MethodSource("reversedComparatorProvider")
    void first_customComparatorReversedOrder(String[] elements, String expected) {
        // GIVEN a set constructed with a comparator that orders strings in reverse
        set = new ConcurrentNavigableSetNullSafe<>(REVERSE_COMPARATOR);
        for (String e : elements) {
            assertTrue(set.add(e));
        }
        // WHEN
        String first = set.first();
        // THEN the element that is first under the custom order is returned
        assertEquals(expected, first);
    }

    @Test
    void first_incomparableElements_throwsClassCastException() {
        // GIVEN a raw set (no generic type) so we can add heterogeneous elements
        @SuppressWarnings("unchecked")
        NavigableSet rawSet = new ConcurrentNavigableSetNullSafe();
        // Add an Integer and a String – they are not mutually comparable without a comparator
        assertTrue(rawSet.add(42));
        assertTrue(rawSet.add("apple"));
        // WHEN/THEN calling first() should fail with ClassCastException
        assertThrows(ClassCastException.class, rawSet::first, "first() on a set with incomparable element types must throw ClassCastException");
    }

    @Test
    void first_emptySet_throwsNoSuchElementException() {
        // GIVEN an empty set (created in @BeforeEach)
        // WHEN/THEN calling first() should throw NoSuchElementException
        assertThrows(NoSuchElementException.class, () -> set.first(), "first() on an empty NavigableSet must throw NoSuchElementException");
    }
}
