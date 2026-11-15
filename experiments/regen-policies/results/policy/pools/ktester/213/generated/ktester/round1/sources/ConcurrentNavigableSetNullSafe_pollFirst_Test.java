package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Comparator;
import java.util.NavigableSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_pollFirst_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural ordering) constructor for most tests.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared between tests in case a test mutates it.
        if (set != null) {
            set.clear();
        }
    }

    @Test
    void pollFirstReturnsFirstElementInNaturalOrder() {
        // Given
        set.add("banana");
        // null is considered the smallest element
        set.add(null);
        set.add("apple");
        set.add("cherry");
        // When
        String first = set.pollFirst();
        // Then
        assertNull(first, "pollFirst should return null when null is present as the smallest element");
        assertEquals(3, set.size(), "Set size should decrease by one after pollFirst");
        assertFalse(set.contains(null), "Set should no longer contain the null element");
        // The next smallest element should now be \"apple\"
        assertEquals("apple", set.first());
    }

    @Test
    void pollFirstRespectsCustomComparator() {
        // Given a comparator that orders strings by length, treating null as length 0
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        set.add("kiwi");
        // length 0 (smallest)
        set.add(null);
        // length 5
        set.add("apple");
        // length 6
        set.add("banana");
        // When
        String first = set.pollFirst();
        // Then
        assertNull(first, "With length comparator, null (length 0) should be first");
        assertEquals(3, set.size());
        assertFalse(set.contains(null));
        // The next element according to length should be \"kiwi\"
        assertEquals("kiwi", set.first());
    }

    @Test
    void pollFirstOnEmptySetReturnsNull() {
        // Given an empty set (no elements added)
        // When
        String first = set.pollFirst();
        // Then
        assertNull(first, "pollFirst on an empty set should return null");
        assertTrue(set.isEmpty(), "Set should remain empty after pollFirst");
    }

    @Test
    void pollFirstRemovesElementsInCorrectSequence() {
        // Given a set with several elements (including null)
        set.add("c");
        set.add("a");
        set.add(null);
        set.add("b");
        // When & Then – poll repeatedly and verify order
        assertNull(set.pollFirst(), "First poll should return null");
        assertEquals("a", set.pollFirst(), "Second poll should return 'a'");
        assertEquals("b", set.pollFirst(), "Third poll should return 'b'");
        assertEquals("c", set.pollFirst(), "Fourth poll should return 'c'");
        // Finally the set should be empty
        assertTrue(set.isEmpty(), "Set should be empty after polling all elements");
    }

    /**
     * When the underlying internalSet is empty, {@code pollFirst()} must return {@code null}.
     */
    @Test
    void pollFirstEmptyInternalSetReturnsNull() {
        // No elements added – set is already empty.
        assertNull(set.pollFirst(), "pollFirst should return null for an empty internal set");
    }

    /**
     * When the internal set contains only a masked‑null sentinel, {@code pollFirst()}
     * should unmask it and return {@code null}.
     */
    @Test
    void pollFirstWithOnlyMaskedNullReturnsNull() {
        // Adding {@code null} forces the class to store the sentinel internally.
        set.add(null);
        assertNull(set.pollFirst(), "pollFirst should return null when the only element is a masked null");
        assertTrue(set.isEmpty(), "Set should be empty after polling the masked null");
    }

    /**
     * When the internal set contains a regular (non‑null) element as its first entry,
     * {@code pollFirst()} should return that element unchanged.
     */
    @Test
    void pollFirstWithSingleRegularElement() {
        set.add("element");
        assertEquals("element", set.pollFirst(), "pollFirst should return the sole regular element");
        assertTrue(set.isEmpty(), "Set should be empty after polling the element");
    }

    /**
     * Verify that {@code pollFirst()} removes the returned element and updates the size.
     */
    @Test
    void pollFirstRemovalEffect() {
        set.add("alpha");
        set.add("beta");
        assertEquals("alpha", set.pollFirst(), "pollFirst should return the natural first element 'alpha'");
        assertEquals(1, set.size(), "Size should be 1 after removing the first element");
        assertFalse(set.contains("alpha"), "Set should no longer contain the removed element");
        assertTrue(set.contains("beta"), "Remaining element should still be present");
    }

    /**
     * Mixed null and non‑null elements: null (sentinel) must be considered first.
     */
    @Test
    void pollFirstMixedNullAndNonNull() {
        set.add("z");
        // sentinel for real null
        set.add(null);
        assertNull(set.pollFirst(), "pollFirst should return null when both null and other elements exist");
        assertEquals(1, set.size(), "Only the non‑null element should remain");
        assertEquals("z", set.first(), "The remaining element should be 'z'");
    }

    /**
     * Natural ordering with {@code Integer} elements.
     */
    @Test
    void pollFirstWithIntegersNaturalOrdering() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.add(5);
        intSet.add(3);
        intSet.add(9);
        assertEquals(Integer.valueOf(3), intSet.pollFirst(), "pollFirst should return the smallest integer (3)");
        assertEquals(2, intSet.size(), "Size should decrease after polling");
    }

    /**
     * Custom reverse comparator: the logical first element is the greatest according to natural order.
     */
    @Test
    void pollFirstWithReverseComparator() {
        Comparator<String> reverse = Comparator.reverseOrder();
        set = new ConcurrentNavigableSetNullSafe<>(reverse);
        set.add("a");
        set.add("b");
        set.add("c");
        assertEquals("c", set.pollFirst(), "With reverse comparator, the first element should be 'c'");
        assertEquals(2, set.size(), "Size should be reduced after polling");
        // The next first element should now be 'b'
        assertEquals("b", set.first());
    }

    /**
     * Parameterized test covering simple single‑element scenarios for both {@code String}
     * and {@code Integer} types.
     *
     * @param type   the type identifier ("String" or "Integer")
     * @param value  the element value to insert
     */
    @ParameterizedTest(name = "pollFirst with single {0} element ''{1}'' returns the element")
    @CsvSource({ "'String', 'singleString'", "'Integer', 42" })
    void pollFirstSingleElementParameterized(String type, String value) {
        if ("String".equals(type)) {
            set.add(value);
            assertEquals(value, set.pollFirst(), "pollFirst should return the inserted string");
        } else if ("Integer".equals(type)) {
            @SuppressWarnings("unchecked")
            NavigableSet<Integer> intSet = (NavigableSet<Integer>) new ConcurrentNavigableSetNullSafe<>();
            int intValue = Integer.parseInt(value);
            intSet.add(intValue);
            assertEquals(Integer.valueOf(intValue), intSet.pollFirst(), "pollFirst should return the inserted integer");
        }
    }
}
