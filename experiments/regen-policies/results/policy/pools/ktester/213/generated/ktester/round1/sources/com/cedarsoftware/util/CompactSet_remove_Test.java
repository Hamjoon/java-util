package com.cedarsoftware.util;

import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactSet_remove_Test {

    /**
     * A normal (case‑sensitive) CompactSet used for most tests
     */
    private CompactSet<String> caseSensitiveSet;

    /**
     * A case‑insensitive CompactSet built via the builder
     */
    private CompactSet<String> caseInsensitiveSet;

    @BeforeEach
    void setupBeforeEach() {
        caseSensitiveSet = new CompactSet<>();
        // Builder configures a case‑insensitive set (the API uses caseSensitive flag)
        caseInsensitiveSet = CompactSet.<String>builder().caseSensitive(// false => case‑insensitive
        false).build();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the sets – not strictly required but keeps each test isolated
        caseSensitiveSet.clear();
        caseInsensitiveSet.clear();
    }

    @Test
    void removeExistingElement_returnsTrue_andElementIsGone() {
        caseSensitiveSet.add("foo");
        caseSensitiveSet.add("bar");
        assertEquals(2, caseSensitiveSet.size());
        assertTrue(caseSensitiveSet.remove("foo"), "remove should return true for existing element");
        assertFalse(caseSensitiveSet.contains("foo"));
        assertEquals(1, caseSensitiveSet.size());
        // Removing the same element again must now return false
        assertFalse(caseSensitiveSet.remove("foo"));
    }

    @Test
    void removeNonExistingElement_returnsFalse_andSetUnchanged() {
        caseSensitiveSet.add("alpha");
        caseSensitiveSet.add("beta");
        int originalSize = caseSensitiveSet.size();
        assertFalse(caseSensitiveSet.remove("gamma"), "remove should return false for missing element");
        assertEquals(originalSize, caseSensitiveSet.size(), "size must stay unchanged");
        assertTrue(caseSensitiveSet.contains("alpha"));
        assertTrue(caseSensitiveSet.contains("beta"));
    }

    @Test
    void removeNullElement_returnsFalse_andDoesNotThrow() {
        caseSensitiveSet.add("something");
        assertFalse(caseSensitiveSet.remove(null), "remove(null) should return false");
        assertEquals(1, caseSensitiveSet.size());
    }

    @Test
    void caseInsensitiveSet_removeIgnoresCase() {
        caseInsensitiveSet.add("Apple");
        assertEquals(1, caseInsensitiveSet.size());
        assertTrue(caseInsensitiveSet.contains("apple"));
        assertTrue(caseInsensitiveSet.contains("APPLE"));
        // Removal should succeed regardless of case
        assertTrue(caseInsensitiveSet.remove("APPLE"));
        assertTrue(caseInsensitiveSet.isEmpty());
    }

    @Test
    void removeAllElements_oneByOne_untilEmpty() {
        caseSensitiveSet.add("one");
        caseSensitiveSet.add("two");
        caseSensitiveSet.add("three");
        assertEquals(3, caseSensitiveSet.size());
        assertTrue(caseSensitiveSet.remove("two"));
        assertEquals(2, caseSensitiveSet.size());
        assertTrue(caseSensitiveSet.remove("one"));
        assertEquals(1, caseSensitiveSet.size());
        assertTrue(caseSensitiveSet.remove("three"));
        assertTrue(caseSensitiveSet.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRemoveExistingElement() {
        caseSensitiveSet.add("apple");
        caseSensitiveSet.add("banana");
        assertEquals(2, caseSensitiveSet.size());
        assertTrue(caseSensitiveSet.remove("apple"), "remove should return true for an element that exists");
        assertFalse(caseSensitiveSet.contains("apple"), "The element must no longer be present after removal");
        assertEquals(1, caseSensitiveSet.size(), "Set size must be decremented after successful removal");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRemoveNonExistingElement() {
        caseSensitiveSet.add("alpha");
        caseSensitiveSet.add("beta");
        int originalSize = caseSensitiveSet.size();
        assertFalse(caseSensitiveSet.remove("gamma"), "remove should return false when the element is not present");
        assertEquals(originalSize, caseSensitiveSet.size(), "Set size must stay unchanged after attempting to remove a missing element");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRemoveNullWhenPresent() {
        // CompactSet permits a null element, so we can add it directly.
        caseSensitiveSet.add(null);
        assertTrue(caseSensitiveSet.contains(null), "Set should contain null after adding it");
        assertTrue(caseSensitiveSet.remove(null), "remove(null) should return true when null is present");
        assertFalse(caseSensitiveSet.contains(null), "Null must be removed from the set");
        assertEquals(0, caseSensitiveSet.size(), "Set should be empty after removing the only element");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRemoveNullWhenAbsent() {
        caseSensitiveSet.add("something");
        assertFalse(caseSensitiveSet.remove(null), "remove(null) should return false when null is not present");
        assertEquals(1, caseSensitiveSet.size(), "Set size must stay unchanged when removing a non‑existent null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRemoveFromEmptySet() {
        assertTrue(caseSensitiveSet.isEmpty(), "Precondition: set must be empty");
        assertFalse(caseSensitiveSet.remove("any"), "remove on an empty set should always return false");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testCaseInsensitiveRemoveExisting() {
        caseInsensitiveSet.add("Apple");
        assertTrue(caseInsensitiveSet.contains("apple"), "Case‑insensitive set should report containment regardless of case");
        assertTrue(caseInsensitiveSet.contains("APPLE"));
        // Removal should succeed irrespective of case
        assertTrue(caseInsensitiveSet.remove("APPLE"), "remove should succeed for a matching element ignoring case");
        assertTrue(caseInsensitiveSet.isEmpty(), "Set must be empty after removing the only element");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRemoveIncompatibleKeyCaseSensitive() {
        caseSensitiveSet.add("one");
        // The set expects String keys; passing an Integer should cause a ClassCastException
        assertThrows(ClassCastException.class, () -> caseSensitiveSet.remove(123));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRemoveIncompatibleKeyCaseInsensitive() {
        caseInsensitiveSet.add("two");
        // Even though the set is case‑insensitive, it still stores keys as Strings.
        assertThrows(ClassCastException.class, () -> caseInsensitiveSet.remove(456));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testRemoveNonComparableInSortedSet() {
        // Build a set with sorted ordering – it uses a TreeMap internally.
        CompactSet<String> sortedSet = CompactSet.<String>builder().sortedOrder().build();
        sortedSet.add("alpha");
        sortedSet.add("beta");
        // Attempt to remove a key that is not Comparable (plain Object)
        Object nonComparableKey = new Object();
        assertThrows(ClassCastException.class, () -> sortedSet.remove(nonComparableKey));
    }
}
