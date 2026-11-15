package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.junit.Test;

/**
 * JUnit 4 test suite for {@link CompactCIHashSet}.
 *
 * The tests focus on the contract provided by {@link CompactSet} and the
 * case‑insensitive behavior introduced by {@link CompactCIHashSet}.  They also
 * verify that the transition from the compact internal representation to the
 * delegated {@code CaseInsensitiveSet} occurs correctly when the number of
 * elements exceeds {@code compactSize()}.
 *
 * The {@code CompactCIHashSet} class does not expose its internal delegate,
 * therefore the tests use only the public API (inherited from {@code CompactSet})
 * and a small helper subclass to expose the protected {@code compactSize()}
 * method.
 */
public class CompactCIHashSetTest {

    /**
     * Helper subclass that makes {@code compactSize()} visible for the tests.
     */
    private static class TestableCompactCIHashSet<E> extends CompactCIHashSet<E> {
        TestableCompactCIHashSet() { super(); }
        TestableCompactCIHashSet(Collection<E> c) { super(c); }

        /** Expose the protected method for test logic. */
        public int getCompactSize() {
            return super.compactSize();
        }
    }

    /** Verify that the set treats string keys case‑insensitively on add/contains. */
    @Test
    public void testAddAndContainsCaseInsensitive() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();

        // First insertion succeeds.
        assertTrue("First add should return true", set.add("Foo"));
        assertEquals(1, set.size());

        // The same value with different case is considered a duplicate.
        assertFalse("Adding duplicate with different case should return false", set.add("fOO"));
        assertEquals(1, set.size());

        // contains() must be case‑insensitive.
        assertTrue(set.contains("foo"));
        assertTrue(set.contains("FOO"));
        assertFalse(set.contains("Bar"));

        // Adding a different value works normally.
        assertTrue(set.add("Bar"));
        assertEquals(2, set.size());
        assertTrue(set.contains("bar"));
    }

    /** Verify that removal works case‑insensitively. */
    @Test
    public void testRemoveCaseInsensitive() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.add("TestValue");

        // Removing with a different case should succeed.
        assertTrue("Removal with different case should return true", set.remove("testvalue"));
        assertTrue(set.isEmpty());

        // Removing a non‑existent element returns false.
        assertFalse(set.remove("another"));
    }

    /** Verify clear() and isEmpty() behavior. */
    @Test
    public void testClearAndIsEmpty() {
        CompactCIHashSet<Integer> set = new CompactCIHashSet<>();
        for (int i = 0; i < 5; i++) {
            set.add(i);
        }
        assertFalse(set.isEmpty());
        assertEquals(5, set.size());

        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /** Verify constructor that accepts an existing collection. */
    @Test
    public void testConstructorFromCollection() {
        Collection<String> source = new ArrayList<>();
        source.add("One");
        source.add("TWO");
        source.add("one");   // duplicate, different case
        source.add("Three");

        CompactCIHashSet<String> set = new CompactCIHashSet<>(source);

        // "One" and "one" collapse to a single entry.
        assertEquals(3, set.size());
        assertTrue(set.contains("ONE"));
        assertTrue(set.contains("two"));
        assertTrue(set.contains("THREE"));
    }

    /**
     * Verify that the internal implementation switches to the delegate
     * {@code CaseInsensitiveSet} when the number of elements exceeds
     * {@code compactSize()}.  The test does not inspect the private delegate;
     * instead it ensures that the set continues to behave correctly after the
     * transition.
     */
    @Test
    public void testTransitionBeyondCompactSize() {
        TestableCompactCIHashSet<String> set = new TestableCompactCIHashSet<>();

        int compactThreshold = set.getCompactSize(); // protected value from CompactSet
        assertTrue("compactSize() should be positive", compactThreshold > 0);

        // Fill the set up to the compact threshold.
        for (int i = 0; i < compactThreshold; i++) {
            String value = "key" + i;
            assertTrue("Add should succeed while under threshold", set.add(value));
        }
        assertEquals(compactThreshold, set.size());

        // Add one more element to trigger the transition.
        String extra = "trigger";
        assertTrue("Add that exceeds compactSize() should succeed", set.add(extra));
        assertEquals(compactThreshold + 1, set.size());

        // After transition, case‑insensitivity must still hold.
        assertTrue(set.contains("TRIGGER"));
        assertFalse(set.contains("nonexistent"));

        // Verify that duplicates (case‑insensitive) are rejected after transition.
        assertFalse("Duplicate after transition should be rejected", set.add("TRIGGER"));
        assertEquals(compactThreshold + 1, set.size());

        // Verify iteration contains all expected elements.
        Set<String> expected = new java.util.HashSet<>();
        for (int i = 0; i < compactThreshold; i++) {
            expected.add("key" + i);
        }
        expected.add(extra);

        Set<String> actual = new java.util.HashSet<>();
        for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
            actual.add(it.next());
        }
        assertEquals("Iterator should return all elements", expected, actual);
    }

    /** Verify that the iterator respects case‑insensitivity (no duplicate entries). */
    @Test
    public void testIteratorNoDuplicates() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.add("Alpha");
        set.add("ALPHA"); // duplicate, different case
        set.add("Beta");

        int count = 0;
        while (set.iterator().hasNext()) {
            set.iterator().next(); // consume
            count++;
        }
        assertEquals("Iterator should yield exactly the number of unique entries", 2, count);
    }
}
