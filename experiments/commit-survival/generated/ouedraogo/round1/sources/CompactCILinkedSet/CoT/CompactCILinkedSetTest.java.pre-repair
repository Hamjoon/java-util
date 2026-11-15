package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCILinkedSet}.
 *
 * The tests are organised as follows:
 * <ul>
 *   <li>List of all public methods (including inherited ones) with their signatures.</li>
 *   <li>Basic functionality tests for each public method.</li>
 *   <li>Edge‑case / exception tests that target the special behaviour of this class
 *       (case‑insensitivity, transition to a linked set when the size exceeds {@code compactSize()},
 *       handling of duplicate / null values, etc.).</li>
 * </ul>
 */
public class CompactCILinkedSetTest {

    /** The object under test – a fresh instance for each test method. */
    private CompactCILinkedSet<String> set;

    @Before
    public void setUp() {
        set = new CompactCILinkedSet<>();
    }

    /* ----------------------------------------------------------------------
     *  1. PUBLIC METHODS (including inherited ones) and their signatures
     * ----------------------------------------------------------------------
     *
     *   CompactCILinkedSet()
     *   CompactCILinkedSet(Collection<E> other)
     *
     *   // inherited from CompactSet<E> (which extends AbstractSet<E>)
     *   int          size()
     *   boolean      isEmpty()
     *   boolean      contains(Object item)
     *   Iterator<E>  iterator()
     *   boolean      add(E item)
     *   boolean      remove(Object item)
     *   void         clear()
     *
     * ---------------------------------------------------------------------- */

    /* ----------------------------------------------------------------------
     *  2. BASIC FUNCTIONALITY TESTS
     * ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue("New set must be empty", set.isEmpty());
        assertEquals("Size must be zero", 0, set.size());
    }

    @Test
    public void testConstructorWithCollectionCopiesElements() {
        List<String> src = new ArrayList<>();
        src.add("Alpha");
        src.add("Beta");
        src.add("Gamma");

        CompactCILinkedSet<String> copy = new CompactCILinkedSet<>(src);
        assertEquals("Copy must contain same number of elements", src.size(), copy.size());
        assertTrue("Copy must contain Alpha", copy.contains("Alpha"));
        assertTrue("Copy must contain Beta", copy.contains("Beta"));
        assertTrue("Copy must contain Gamma", copy.contains("Gamma"));
    }

    @Test
    public void testAddAndContains() {
        assertTrue(set.add("One"));
        assertTrue(set.contains("One"));
        assertEquals(1, set.size());

        // adding duplicate should return false and not increase size
        assertFalse(set.add("One"));
        assertEquals(1, set.size());
    }

    @Test
    public void testRemoveExistingAndNonExisting() {
        set.add("A");
        set.add("B");

        assertTrue("Removing existing element must return true", set.remove("A"));
        assertFalse("Set must no longer contain removed element", set.contains("A"));
        assertEquals(1, set.size());

        assertFalse("Removing non‑existing element must return false", set.remove("C"));
        assertEquals(1, set.size());
    }

    @Test
    public void testClearEmptiesSet() {
        set.add("X");
        set.add("Y");
        assertFalse(set.isEmpty());

        set.clear();
        assertTrue("Set must be empty after clear()", set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testIteratorOrderBeforeTransition() {
        // Add a few elements (less than compactSize) and verify iterator order.
        set.add("first");
        set.add("second");
        set.add("third");

        Iterator<String> it = set.iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
        assertFalse(it.hasNext());
    }

    /* ----------------------------------------------------------------------
     *  3. EDGE CASE / SPECIAL BEHAVIOUR TESTS
     * ---------------------------------------------------------------------- */

    /**
     * Helper that uses reflection to call the protected {@code compactSize()} method.
     */
    private int getCompactSize(CompactCILinkedSet<?> s) throws Exception {
        Method m = CompactSet.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        return (int) m.invoke(s);
    }

    @Test
    public void testCaseInsensitivity() throws Exception {
        assertTrue(set.add("Hello"));
        // Adding same word with different case must be considered duplicate.
        assertFalse("Adding case‑insensitive duplicate must return false", set.add("hello"));
        assertEquals("Size must remain 1 after case‑insensitive duplicate", 1, set.size());

        // contains should also be case‑insensitive
        assertTrue("contains() should be case‑insensitive", set.contains("HELLO"));
        assertTrue(set.remove("HeLLo"));
        assertTrue("Set must be empty after removing case‑insensitive element", set.isEmpty());
    }

    @Test
    public void testTransitionToLinkedSetPreservesInsertionOrder() throws Exception {
        int compactSize = getCompactSize(set);
        // Fill the set to just below the threshold.
        for (int i = 0; i < compactSize; i++) {
            assertTrue(set.add("item" + i));
        }
        assertEquals("Size must equal compactSize before transition", compactSize, set.size());

        // Add one more element to trigger the transition to a linked set.
        assertTrue(set.add("trigger"));
        assertEquals(compactSize + 1, set.size());

        // Verify that iteration now follows insertion order for *all* elements.
        List<String> expected = new ArrayList<>();
        for (int i = 0; i < compactSize; i++) {
            expected.add("item" + i);
        }
        expected.add("trigger");

        List<String> actual = new ArrayList<>();
        for (String s : set) {
            actual.add(s);
        }
        assertEquals("Iteration order must match insertion order after transition", expected, actual);
    }

    @Test
    public void testAddNullElement() {
        // The underlying CompactSet permits null (it extends AbstractSet); we verify behaviour.
        assertTrue("Adding null should succeed the first time", set.add(null));
        assertTrue("Set must contain null after addition", set.contains(null));
        assertEquals(1, set.size());

        // Adding null again should be a duplicate.
        assertFalse("Adding duplicate null must return false", set.add(null));
        assertEquals(1, set.size());

        // Removing null should succeed.
        assertTrue("Removing null must return true", set.remove(null));
        assertFalse(set.contains(null));
        assertTrue(set.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrowsNPE() {
        // CompactSet(Collection) does not guard against a null argument; it will throw NPE.
        new CompactCILinkedSet<String>((Collection<String>) null);
    }

    @Test
    public void testIteratorRemoveWorks() {
        set.add("A");
        set.add("B");
        set.add("C");

        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String v = it.next();
            if ("B".equals(v)) {
                it.remove(); // should remove element B
            }
        }

        assertFalse("Set must not contain B after iterator.remove()", set.contains("B"));
        assertEquals(2, set.size());
    }

    @Test
    public void testAddAllViaConstructorAndTransition() throws Exception {
        // Build a collection larger than compactSize to test the constructor path that
        // directly creates the linked (CaseInsensitive) delegate.
        int compactSize = getCompactSize(set);
        List<String> bigList = new ArrayList<>();
        for (int i = 0; i < compactSize + 3; i++) {
            bigList.add("val" + i);
        }

        CompactCILinkedSet<String> bigSet = new CompactCILinkedSet<>(bigList);
        assertEquals(compactSize + 3, bigSet.size());

        // Verify case‑insensitivity still works.
        assertFalse(bigSet.add("VAL0"));
        assertTrue(bigSet.contains("val0"));
        assertTrue(bigSet.contains("VAL0"));
    }
}
