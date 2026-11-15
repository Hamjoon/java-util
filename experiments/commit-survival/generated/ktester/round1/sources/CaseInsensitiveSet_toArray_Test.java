package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collection;
import java.util.Set;
import java.util.Arrays;

class CaseInsensitiveSet_toArray_Test {

    private CaseInsensitiveSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test.
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required.
        set = null;
    }

    /**
     * Helper that checks whether the supplied array contains all the expected values.
     */
    private static boolean containsAll(Object[] array, String... expected) {
        for (String exp : expected) {
            boolean found = false;
            for (Object o : array) {
                if (exp.equals(o)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Test
    void toArray_EmptySet_ReturnsEmptyArray() {
        // Given an empty CaseInsensitiveSet
        assertTrue(set.isEmpty(), "Set should be empty before invoking toArray");
        // When
        Object[] result = set.toArray();
        // Then
        assertNotNull(result, "Resulting array must not be null");
        assertEquals(0, result.length, "Array length should be 0 for an empty set");
    }

    @Test
    void toArray_PopulatedSet_ReturnsAllElements() {
        // Given a set with several distinct elements
        set.add("Apple");
        set.add("Banana");
        set.add("Cherry");
        // When
        Object[] result = set.toArray();
        // Then
        assertNotNull(result);
        assertEquals(3, result.length);
        // Order is defined by the underlying map's keySet iteration order.
        // Since CaseInsensitiveSet uses a sorted map by default, the order will be natural.
        // However, we only assert that the expected elements are present, regardless of order.
        assertTrue(containsAll(result, "Apple", "Banana", "Cherry"));
    }

    @Test
    void toArray_CaseInsensitiveBehaviour_SingleElementForDifferentCases() {
        // Given a set where we add strings that differ only by case
        set.add("Test");
        // should be considered duplicate
        set.add("test");
        // When
        Object[] result = set.toArray();
        // Then
        assertEquals(1, result.length, "Only one element should be stored because of case‑insensitivity");
        assertEquals("Test", result[0], "Original case of the first inserted element should be retained");
    }

    @Test
    void toArray_ModifyingReturnedArrayDoesNotAffectSet() {
        // Given a populated set
        set.add("One");
        set.add("Two");
        // When
        Object[] array = set.toArray();
        array[0] = "Modified";
        // Then
        // The set must remain unchanged
        assertTrue(set.contains("One"));
        assertTrue(set.contains("Two"));
        assertFalse(set.contains("Modified"));
        // Also verify that a fresh call to toArray reflects the original content
        Object[] fresh = set.toArray();
        assertTrue(containsAll(fresh, "One", "Two"));
    }

    @Test
    void toArray_SingleElement_ReturnsArrayWithThatElement() {
        // Group: Single Element
        set.add("Hello");
        Object[] result = set.toArray();
        assertNotNull(result);
        assertEquals(1, result.length, "Array length must be 1 after adding a single element");
        assertEquals("Hello", result[0], "The sole element must be the one that was added");
    }

    @Test
    void toArray_MultipleElements_SortedCaseInsensitiveOrder() {
        // Group: Multiple Elements (sorted order)
        set.add("apple");
        set.add("Banana");
        set.add("Cherry");
        Object[] result = set.toArray();
        // Expected order is case‑insensitive alphabetical: apple, Banana, Cherry
        String[] expected = { "apple", "Banana", "Cherry" };
        assertArrayEquals(expected, result, "Elements must be returned in case‑insensitive alphabetical order");
    }

    @Test
    void toArray_ArrayIndependence_ModificationDoesNotAffectSet() {
        // Group: Array Independence – modification does not affect set
        set.add("Alpha");
        set.add("Beta");
        Object[] array = set.toArray();
        array[0] = "Changed";
        // The original set must stay unchanged
        assertEquals(2, set.size(), "Set size must remain 2 after external array modification");
        assertTrue(set.contains("Alpha"));
        assertTrue(set.contains("Beta"));
        assertFalse(set.contains("Changed"));
    }

    @Test
    void toArray_ArraySnapshot_LaterModificationsDoNotChangePreviousArray() {
        // Group: Array Snapshot – later modifications do not affect previously returned array
        set.add("One");
        // should contain only "One"
        Object[] snapshot = set.toArray();
        // Add another element after snapshot was taken
        set.add("Two");
        // Snapshot must still contain only the original element
        assertEquals(1, snapshot.length, "Snapshot array length must stay unchanged");
        assertEquals("One", snapshot[0]);
        // The set now contains both elements
        assertEquals(2, set.size());
        assertTrue(set.contains("One"));
        assertTrue(set.contains("Two"));
    }

    @Test
    void toArray_NullElementHandling_AddNullThrowsExceptionAndArrayRemainsEmpty() {
        // Group: Null Element Handling
        NullPointerException npe = assertThrows(NullPointerException.class, () -> set.add(null), "Adding null should throw NullPointerException");
        assertNotNull(npe, "Exception instance must not be null");
        // After the failed add, the set should still be empty
        Object[] result = set.toArray();
        assertNotNull(result);
        assertEquals(0, result.length, "toArray() must return an empty array after a failed null add");
    }
}
