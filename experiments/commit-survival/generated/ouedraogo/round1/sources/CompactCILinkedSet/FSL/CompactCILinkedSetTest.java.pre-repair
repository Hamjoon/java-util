package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

public class CompactCILinkedSetTest {

    /**
     * Helper to obtain the compact size threshold from the protected method.
     */
    private int getCompactThreshold(CompactCILinkedSet<?> set) {
        // The method is protected, but we are in the same package, so we can call it directly.
        return set.compactSize();
    }

    @Test
    public void testEmptySetBehavior() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        assertTrue("New set should be empty", set.isEmpty());
        assertEquals("Size of new set should be 0", 0, set.size());
        assertFalse("Empty set should not contain any element", set.contains("anything"));
    }

    @Test
    public void testAddWithinCompactSizeKeepsInsertionOrder() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        int threshold = getCompactThreshold(set);

        // Add exactly 'threshold' distinct elements
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < threshold; i++) {
            String element = "elem" + i;
            expected.add(element);
            assertTrue("Add should succeed for new element", set.add(element));
        }

        // Verify size and order
        assertEquals("Size should equal threshold after adds", threshold, set.size());

        List<String> actual = new ArrayList<>();
        for (String s : set) {
            actual.add(s);
        }
        assertEquals("Iteration order must match insertion order within compact size", expected, actual);
    }

    @Test
    public void testTransitionBeyondCompactSizeMaintainsOrder() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        int threshold = getCompactThreshold(set);

        // Fill up to threshold
        for (int i = 0; i < threshold; i++) {
            set.add("pre" + i);
        }

        // Add one more element to trigger transition to the linked (CaseInsensitive) set
        String extra = "extra";
        assertTrue("Add should succeed for element causing transition", set.add(extra));

        assertEquals("Size should be threshold + 1 after transition", threshold + 1, set.size());

        // Verify iteration order includes the extra element at the end
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < threshold; i++) {
            expected.add("pre" + i);
        }
        expected.add(extra);

        List<String> actual = new ArrayList<>();
        for (String s : set) {
            actual.add(s);
        }
        assertEquals("Iteration order must be preserved after transition", expected, actual);
    }

    @Test
    public void testCaseInsensitiveAddAndContains() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();

        assertTrue("First add of 'Foo' should succeed", set.add("Foo"));
        assertFalse("Second add with different case should be ignored", set.add("foo"));
        assertEquals("Size must be 1 because set is case‑insensitive", 1, set.size());

        assertTrue("Contains should be true for different case", set.contains("FOO"));
        assertTrue("Contains should be true for original case", set.contains("Foo"));
    }

    @Test
    public void testCaseInsensitiveRemove() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        set.add("Bar");
        assertTrue("Set should contain 'Bar'", set.contains("Bar"));
        assertTrue("Remove using different case should succeed", set.remove("bAr"));
        assertFalse("Set should be empty after removal", set.contains("Bar"));
        assertEquals("Size must be 0 after removal", 0, set.size());
    }

    @Test
    public void testIterationOrderWithCaseInsensitivityAndTransition() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        int threshold = getCompactThreshold(set);

        // Fill up to threshold using mixed case values
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < threshold; i++) {
            String element = "Item" + i;
            expected.add(element);
            set.add(element);
            // add a duplicate with different case – should be ignored
            set.add(element.toUpperCase());
        }

        // Add one more distinct element to force transition
        String extra = "finalItem";
        expected.add(extra);
        set.add(extra);

        // Verify iteration order equals the order we inserted distinct values
        List<String> actual = new ArrayList<>();
        for (String s : set) {
            actual.add(s);
        }
        assertEquals("Iteration order must reflect insertion order, ignoring case duplicates", expected, actual);
    }

    @Test
    public void testConstructorFromCollection() {
        List<String> source = Arrays.asList("Alpha", "beta", "ALPHA", "Beta", "gamma");
        // Expected distinct values respecting case‑insensitivity, preserving first occurrence order
        List<String> expectedOrder = Arrays.asList("Alpha", "beta", "gamma");

        CompactCILinkedSet<String> set = new CompactCILinkedSet<>(source);

        assertEquals("Size must reflect case‑insensitive distinct elements", expectedOrder.size(), set.size());

        // Verify order via iterator
        List<String> actualOrder = new ArrayList<>();
        for (String s : set) {
            actualOrder.add(s);
        }
        assertEquals("Iterator order must follow first occurrence order from collection", expectedOrder, actualOrder);
    }

    @Test
    public void testClearEmptiesSet() {
        CompactCILinkedSet<Integer> set = new CompactCILinkedSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        assertFalse("Set should not be empty before clear", set.isEmpty());

        set.clear();

        assertTrue("Set should be empty after clear", set.isEmpty());
        assertEquals("Size must be 0 after clear", 0, set.size());
        assertFalse("Contains should be false after clear", set.contains(1));
    }

    @Test
    public void testRemoveNonExistingElement() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        set.add("exists");
        assertFalse("Removing a non‑existing element should return false", set.remove("doesNotExist"));
        assertEquals("Size must stay unchanged after failed removal", 1, set.size());
    }

    @Test
    public void testAddNullElementIfSupported() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        try {
            boolean added = set.add(null);
            // Some implementations allow null; we just verify the contract behaves consistently.
            if (added) {
                assertTrue("Set should contain null after successful add", set.contains(null));
                assertEquals("Size should be 1 after adding null", 1, set.size());
                // Adding null again must not increase size
                assertFalse("Second add of null should return false", set.add(null));
                assertEquals("Size must stay 1 after duplicate null add", 1, set.size());
            } else {
                // If null is not allowed, the set should simply not contain it.
                assertFalse("Set should not contain null if add returned false", set.contains(null));
            }
        } catch (NullPointerException npe) {
            // Acceptable if the implementation throws NPE for nulls.
        }
    }
}
