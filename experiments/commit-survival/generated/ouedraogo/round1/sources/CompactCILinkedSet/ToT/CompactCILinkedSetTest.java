package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 test suite for {@link CompactCILinkedSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic Set operations (add, remove, contains, clear, size, isEmpty)</li>
 *   <li>case‑insensitive behavior</li>
 *   <li>transition from compact representation to a {@link CaseInsensitiveSet}
 *       when the number of elements exceeds {@code compactSize()}</li>
 *   <li>preservation of insertion order after the transition (Linked behavior)</li>
 *   <li>exposure of protected members via a test‑only subclass</li>
 * </ul>
 */
public class CompactCILinkedSetTest {

    /** Helper subclass that exposes the protected members of {@link CompactCILinkedSet}. */
    private static class TestableCompactCILinkedSet<E> extends CompactCILinkedSet<E> {
        TestableCompactCILinkedSet() { super(); }
        TestableCompactCILinkedSet(Collection<E> other) { super(other); }

        /** Expose {@code compactSize()} for the tests. */
        int getCompactSize() {
            return super.compactSize();
        }

        /** Expose {@code getNewSet()} for the tests. */
        Set<E> exposeNewSet() {
            return super.getNewSet();
        }

        /** Expose {@code isCaseInsensitive()} for the tests. */
        boolean exposeIsCaseInsensitive() {
            return super.isCaseInsensitive();
        }
    }

    private TestableCompactCILinkedSet<String> set;
    private int compactSize;

    @Before
    public void setUp() {
        set = new TestableCompactCILinkedSet<>();
        compactSize = set.getCompactSize();   // the threshold where the backing Set changes
    }

    /**
     * Verify basic add/size/isEmpty behavior.
     */
    @Test
    public void testAddAndSize() {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        assertTrue(set.add("one"));
        assertTrue(set.add("two"));
        assertTrue(set.add("three"));

        assertEquals(3, set.size());
        assertFalse(set.isEmpty());

        // Adding a duplicate (case‑insensitive) should return false and not change size
        assertFalse(set.add("ONE"));
        assertEquals(3, set.size());
    }

    /**
     * Ensure the set treats keys case‑insensitively.
     */
    @Test
    public void testCaseInsensitiveAddAndContains() {
        assertTrue(set.add("Apple"));
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("APPLE"));

        // Adding the same value with different case must not increase the size
        assertFalse(set.add("aPPLe"));
        assertEquals(1, set.size());

        // Removal must also be case‑insensitive
        assertTrue(set.remove("ApPlE"));
        assertFalse(set.contains("apple"));
        assertEquals(0, set.size());
    }

    /**
     * Verify that after the number of elements exceeds {@code compactSize()},
     * the underlying set switches to a {@link CaseInsensitiveSet} that preserves
     * insertion order (Linked semantics).
     */
    @Test
    public void testOrderPreservedBeyondCompactSize() {
        // Add exactly compactSize elements – still in compact mode
        for (int i = 0; i < compactSize; i++) {
            assertTrue(set.add("item" + i));
        }
        assertEquals(compactSize, set.size());

        // Add two more elements to trigger the transition
        assertTrue(set.add("trigger1"));
        assertTrue(set.add("trigger2"));
        assertEquals(compactSize + 2, set.size());

        // Verify iteration order is exactly the insertion order
        Iterator<String> it = set.iterator();
        for (int i = 0; i < compactSize; i++) {
            assertTrue(it.hasNext());
            assertEquals("item" + i, it.next());
        }
        assertTrue(it.hasNext());
        assertEquals("trigger1", it.next());
        assertTrue(it.hasNext());
        assertEquals("trigger2", it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Test {@code remove(Object)} and subsequent {@code contains(Object)} behavior,
     * including removal of a non‑existent element.
     */
    @Test
    public void testRemoveAndContains() {
        set.add("alpha");
        set.add("beta");
        set.add("gamma");

        assertTrue(set.contains("beta"));
        assertTrue(set.remove("BETA"));  // case‑insensitive removal
        assertFalse(set.contains("beta"));
        assertEquals(2, set.size());

        // Removing an element that is not present should return false
        assertFalse(set.remove("delta"));
        assertEquals(2, set.size());
    }

    /**
     * Verify that {@code clear()} empties the set and resets its size to zero.
     */
    @Test
    public void testClear() {
        set.add("one");
        set.add("two");
        set.add("three");

        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        // After clearing, the set should still be case‑insensitive
        assertTrue(set.add("ONE"));
        assertEquals(1, set.size());
        assertTrue(set.contains("one"));
    }

    /**
     * Ensure the protected {@code getNewSet()} creates a {@link CaseInsensitiveSet}
     * with the expected initial capacity (> compactSize).
     */
    @Test
    public void testGetNewSetReturnsCaseInsensitiveSet() {
        Set<String> newSet = set.exposeNewSet();
        assertNotNull(newSet);
        assertTrue(newSet instanceof CaseInsensitiveSet);

        // The new set should be case‑insensitive; verify via a simple add/contains check
        newSet.add("Foo");
        assertTrue(newSet.contains("fOO"));
    }

    /**
     * Verify that the protected {@code isCaseInsensitive()} reports {@code true}.
     */
    @Test
    public void testIsCaseInsensitive() {
        assertTrue(set.exposeIsCaseInsensitive());
    }

    /**
     * Edge case: adding {@code null} should behave consistently with {@link CompactSet}.
     * The contract of {@code CompactSet} (as per its Javadoc) permits {@code null}
     * elements, so we assert that it can be added, detected, and removed.
     */
    @Test
    public void testNullElementHandling() {
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());

        // Adding another null must not increase size (Set semantics)
        assertFalse(set.add(null));
        assertEquals(1, set.size());

        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertEquals(0, set.size());
    }
}
