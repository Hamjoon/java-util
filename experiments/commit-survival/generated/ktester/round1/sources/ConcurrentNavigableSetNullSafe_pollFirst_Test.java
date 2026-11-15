package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_pollFirst_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural ordering) constructor for most tests.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no test leaks state.
        set.clear();
        set = null;
    }

    @Test
    void pollFirst_returnsAndRemovesFirstElement_NaturalOrder() {
        // Arrange – natural ordering, null is placed after non‑null values.
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        assertTrue(set.add("cherry"));
        assertEquals(4, set.size());
        // Act
        String first = set.pollFirst();
        // Assert
        assertEquals("apple", first, "pollFirst should return the lowest element");
        assertEquals(3, set.size(), "size should decrease after pollFirst");
        assertFalse(set.contains("apple"), "removed element must no longer be present");
        // Remaining order should be banana, cherry, null
        assertEquals("banana", set.iterator().next());
    }

    @Test
    void pollFirst_returnsNullWhenSetIsEmpty() {
        // Arrange – the set is already empty from @BeforeEach
        // Act
        String first = set.pollFirst();
        // Assert
        assertNull(first, "pollFirst on an empty set must return null");
        assertTrue(set.isEmpty(), "set remains empty");
    }

    @Test
    void pollFirst_returnsAndRemovesNullWhenNullIsFirst_WithNullsFirstComparator() {
        // Arrange – use a comparator that orders null before any other element.
        Comparator<String> nullsFirst = Comparator.nullsFirst(Comparator.naturalOrder());
        set = new ConcurrentNavigableSetNullSafe<>(nullsFirst);
        assertTrue(set.add("apple"));
        assertTrue(set.add(null));
        assertTrue(set.add("banana"));
        assertEquals(3, set.size());
        // Act
        String first = set.pollFirst();
        // Assert
        assertNull(first, "With nullsFirst comparator, pollFirst should return null");
        assertEquals(2, set.size(), "size should decrease after removing null");
        assertFalse(set.contains(null), "null must have been removed");
        // Verify the new first element is the smallest non‑null value.
        assertEquals("apple", set.first());
    }

    @Test
    void pollFirst_multipleCallsConsumeElementsInOrder() {
        // Arrange
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        assertTrue(set.add(null));
        assertEquals(4, set.size());
        // Act & Assert – first call
        assertEquals("apple", set.pollFirst());
        assertEquals(3, set.size());
        // second call
        assertEquals("banana", set.pollFirst());
        assertEquals(2, set.size());
        // third call
        assertEquals("cherry", set.pollFirst());
        assertEquals(1, set.size());
        // fourth call – now null is the only element left
        assertNull(set.pollFirst());
        assertTrue(set.isEmpty());
    }

    /**
     * Empty set must return null (already covered, but kept for completeness).
     */
    @Test
    void pollFirst_emptySet_returnsNull() {
        assertTrue(set.isEmpty());
        assertNull(set.pollFirst(), "Empty set should return null");
    }

    /**
     * A set that only contains a null element must return null.
     */
    @Test
    void pollFirst_onlyNullElement_returnsNull() {
        assertTrue(set.add(null));
        assertEquals(1, set.size());
        String result = set.pollFirst();
        assertNull(result, "pollFirst should return null when the only element is null");
        assertTrue(set.isEmpty(), "Set should be empty after removing the null element");
    }

    /**
     * A set that only contains a regular element must return that element.
     */
    @Test
    void pollFirst_onlyRegularElement_returnsElement() {
        assertTrue(set.add("alpha"));
        assertEquals(1, set.size());
        String result = set.pollFirst();
        assertEquals("alpha", result, "pollFirst should return the only element present");
        assertTrue(set.isEmpty(), "Set should be empty after polling the element");
    }

    /**
     * Removal semantics – first element removed, second call returns next element.
     */
    @Test
    void pollFirst_removalSemantics_twoElements() {
        assertTrue(set.add("alpha"));
        assertTrue(set.add("beta"));
        assertEquals(2, set.size());
        // first call – smallest element according to natural order
        assertEquals("alpha", set.pollFirst());
        assertEquals(1, set.size());
        assertFalse(set.contains("alpha"));
        // second call – remaining element
        assertEquals("beta", set.pollFirst());
        assertTrue(set.isEmpty());
    }

    /**
     * Null is considered the lowest element when a null‑first comparator is used.
     */
    @Test
    void pollFirst_nullAndNonNull_nullFirstComparator() {
        Comparator<String> nullsFirst = Comparator.nullsFirst(Comparator.naturalOrder());
        set = new ConcurrentNavigableSetNullSafe<>(nullsFirst);
        assertTrue(set.add("gamma"));
        assertTrue(set.add(null));
        assertEquals(2, set.size());
        // pollFirst should return null because the comparator puts null first
        assertNull(set.pollFirst());
        assertEquals(1, set.size());
        assertFalse(set.contains(null));
        // next pollFirst returns the remaining non‑null element
        assertEquals("gamma", set.pollFirst());
        assertTrue(set.isEmpty());
    }

    /**
     * Custom comparator that orders strings in descending (reverse) order.
     */
    @Test
    void pollFirst_descendingComparator() {
        Comparator<String> descending = Comparator.reverseOrder();
        set = new ConcurrentNavigableSetNullSafe<>(descending);
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        assertTrue(set.add("c"));
        assertEquals(3, set.size());
        // According to reverse order, "c" is the smallest element.
        assertEquals("c", set.pollFirst());
        assertEquals(2, set.size());
        // Next smallest is "b".
        assertEquals("b", set.pollFirst());
        assertEquals(1, set.size());
        // Finally "a".
        assertEquals("a", set.pollFirst());
        assertTrue(set.isEmpty());
    }

    /**
     * Comparator that prefers even numbers over odd numbers.
     */
    @Test
    void pollFirst_evenNumbersFirstComparator() {
        // Create a set of Integer with a custom comparator.
        Comparator<Integer> evenFirst = (a, b) -> {
            boolean aEven = (a % 2) == 0;
            boolean bEven = (b % 2) == 0;
            if (aEven && !bEven)
                return -1;
            if (!aEven && bEven)
                return 1;
            return Integer.compare(a, b);
        };
        @SuppressWarnings("unchecked")
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>(evenFirst);
        assertTrue(intSet.add(3));
        assertTrue(intSet.add(2));
        assertTrue(intSet.add(5));
        assertTrue(intSet.add(4));
        assertEquals(4, intSet.size());
        // First even number (smallest) is 2.
        assertEquals(Integer.valueOf(2), intSet.pollFirst());
        assertEquals(3, intSet.size());
        // Next even number is 4.
        assertEquals(Integer.valueOf(4), intSet.pollFirst());
        assertEquals(2, intSet.size());
        // Now only odds remain; smallest odd is 3.
        assertEquals(Integer.valueOf(3), intSet.pollFirst());
        assertEquals(1, intSet.size());
        // Finally 5.
        assertEquals(Integer.valueOf(5), intSet.pollFirst());
        assertTrue(intSet.isEmpty());
    }

    /**
     * Simulate a broken internal state where internalSet is null – should throw NPE.
     */
    @Test
    void pollFirst_internalSetNull_throwsNullPointerException() throws Exception {
        // Use reflection to set the private field `internalSet` to null.
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        internalField.set(set, null);
        // The call must fail with NPE because we try to invoke pollFirst on a null reference.
        assertThrows(NullPointerException.class, () -> set.pollFirst(), "pollFirst should throw NullPointerException when internalSet is null");
    }

    /**
     * Populate internalSet with a mismatched type (Integer in a String set) – should throw CCE.
     */
    @Test
    void pollFirst_typeMismatch_throwsClassCastException() throws Exception {
        // Build a raw ConcurrentSkipListSet containing an Integer.
        ConcurrentSkipListSet<Object> rawSet = new ConcurrentSkipListSet<>();
        // Integer element
        rawSet.add(42);
        // Inject the raw set into a ConcurrentNavigableSetNullSafe<String> instance.
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        internalField.set(set, rawSet);
        // pollFirst will retrieve the Integer and attempt to cast it to String,
        // resulting in a ClassCastException.
        assertThrows(ClassCastException.class, () -> set.pollFirst(), "pollFirst should throw ClassCastException when element type does not match generic type");
    }
}
