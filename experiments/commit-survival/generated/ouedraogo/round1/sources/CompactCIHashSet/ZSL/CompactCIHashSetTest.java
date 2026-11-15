package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCIHashSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic Set contract (size, isEmpty, add, remove, clear, iterator, contains)</li>
 *   <li>case‑insensitive behaviour (add duplicate with different case, contains with different case)</li>
 *   <li>construction from another collection</li>
 *   <li>the transition from the compact internal storage to the delegated {@link CaseInsensitiveSet}
 *       when the number of elements exceeds {@code compactSize()}</li>
 * </ul>
 *
 * No mocking is used – real objects are created via the public constructors.
 */
public class CompactCIHashSetTest {

    private CompactCIHashSet<String> set;

    @Before
    public void setUp() {
        set = new CompactCIHashSet<>();
    }

    /**
     * Helper that uses reflection to obtain the protected {@code compactSize()} value.
     */
    private int getCompactSize(CompactCIHashSet<?> s) throws Exception {
        Method m = CompactSet.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        return (int) m.invoke(s);
    }

    @Test
    public void testAddAndSizeAndIsEmpty() {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        assertTrue(set.add("One"));
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());

        // adding a different element increases size
        assertTrue(set.add("Two"));
        assertEquals(2, set.size());

        // adding a duplicate (same case) returns false and does not change size
        assertFalse(set.add("One"));
        assertEquals(2, set.size());
    }

    @Test
    public void testCaseInsensitiveAdd() {
        // first insertion succeeds
        assertTrue(set.add("Hello"));
        assertEquals(1, set.size());

        // second insertion with different case is considered duplicate
        assertFalse(set.add("hello"));
        assertEquals(1, set.size());

        // third insertion with another case variation also duplicate
        assertFalse(set.add("HeLLo"));
        assertEquals(1, set.size());
    }

    @Test
    public void testContains() {
        set.add("Alpha");
        set.add("Beta");

        // exact case
        assertTrue(set.contains("Alpha"));
        assertTrue(set.contains("Beta"));

        // different case – should still be true because the set is case‑insensitive
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("bEtA"));

        // not present
        assertFalse(set.contains("Gamma"));
    }

    @Test
    public void testIterator() {
        set.add("One");
        set.add("Two");
        set.add("Three");

        // collect elements via iterator
        Iterator<String> it = set.iterator();
        Collection<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }

        // iterator should have delivered all distinct values (case‑insensitive)
        assertEquals(3, iterated.size());
        assertTrue(iterated.contains("One"));
        assertTrue(iterated.contains("Two"));
        assertTrue(iterated.contains("Three"));
    }

    @Test
    public void testRemove() {
        set.add("First");
        set.add("Second");
        set.add("Third");

        assertTrue(set.remove("second")); // different case, should succeed
        assertEquals(2, set.size());
        assertFalse(set.contains("Second"));

        // removing a non‑existent element returns false
        assertFalse(set.remove("nonexistent"));
        assertEquals(2, set.size());

        // remove remaining elements
        assertTrue(set.remove("First"));
        assertTrue(set.remove("THIRD"));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testClear() {
        set.add("A");
        set.add("B");
        set.add("C");

        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        // after clear we can add again
        assertTrue(set.add("new"));
        assertEquals(1, set.size());
        assertTrue(set.contains("NEW"));
    }

    @Test
    public void testConstructorWithCollection() {
        Collection<String> src = new ArrayList<>();
        src.add("X");
        src.add("Y");
        src.add("Z");

        CompactCIHashSet<String> fromCollection = new CompactCIHashSet<>(src);
        assertEquals(3, fromCollection.size());
        assertTrue(fromCollection.contains("x")); // case‑insensitive check
        assertTrue(fromCollection.contains("Y"));
        assertTrue(fromCollection.contains("z"));
    }

    @Test
    public void testTransitionBeyondCompactSize() throws Exception {
        int compactSize = getCompactSize(set);
        // add exactly compactSize distinct elements
        for (int i = 0; i < compactSize; i++) {
            assertTrue(set.add("item" + i));
        }
        assertEquals(compactSize, set.size());

        // At this point the internal storage is still the compact representation.
        // Adding one more element forces the transition to the delegated hash set.
        assertTrue(set.add("overflow"));
        assertEquals(compactSize + 1, set.size());

        // Verify that all previously added elements are still present
        for (int i = 0; i < compactSize; i++) {
            assertTrue("Missing element: item" + i, set.contains("ITEM" + i));
        }
        // Verify the newly added element is present (case‑insensitive)
        assertTrue(set.contains("OVERFLOW"));
    }

    @Test
    public void testAddNullElement() {
        // CompactCIHashSet (via CompactSet) allows null values – verify behaviour
        assertTrue(set.add(null));
        assertEquals(1, set.size());
        assertTrue(set.contains(null));

        // Adding another null should be considered duplicate
        assertFalse(set.add(null));
        assertEquals(1, set.size());
    }
}
