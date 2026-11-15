package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentList}.
 *
 * <p>The test class is written as if three independent experts each contributed
 * one test case per public method.  All cases have been merged into a single,
 * runnable test file.</p>
 */
public class ConcurrentListTest {

    private ConcurrentList<String> emptyList;   // freshly created in each @Before
    private ConcurrentList<String> populatedList; // contains ["a","b","c"]

    @Before
    public void setUp() {
        emptyList = new ConcurrentList<>();

        populatedList = new ConcurrentList<>();
        populatedList.add("a");
        populatedList.add("b");
        populatedList.add("c");
    }

    /* -------------------- Constructors -------------------- */

    @Test
    public void testNoArgConstructorCreatesEmptyList() {
        assertEquals(0, emptyList.size());
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testConstructorWithInitialCapacityCreatesEmptyList() {
        ConcurrentList<Integer> list = new ConcurrentList<>(10);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testWrappingConstructorUsesSameUnderlyingList() {
        List<String> backing = new ArrayList<>();
        backing.add("x");
        ConcurrentList<String> wrapper = new ConcurrentList<>(backing);
        // changes via wrapper affect backing list
        wrapper.add("y");
        assertEquals(2, backing.size());
        assertEquals("y", backing.get(1));
        // changes via backing affect wrapper
        backing.add("z");
        assertEquals(3, wrapper.size());
        assertEquals("z", wrapper.get(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrappingConstructorRejectsNull() {
        new ConcurrentList<>(null);
    }

    /* -------------------- Immutable APIs -------------------- */

    @Test
    public void testEqualsAndHashCode() {
        List<String> other = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertTrue(populatedList.equals(other));
        assertEquals(other.hashCode(), populatedList.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("[a, b, c]", populatedList.toString());
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(3, populatedList.size());
        assertFalse(populatedList.isEmpty());

        emptyList.clear();
        assertEquals(0, emptyList.size());
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testContainsAndContainsAll() {
        assertTrue(populatedList.contains("b"));
        assertFalse(populatedList.contains("z"));

        Collection<String> subset = Arrays.asList("a", "c");
        Collection<String> superset = Arrays.asList("a", "b", "c", "d");
        assertTrue(populatedList.containsAll(subset));
        assertFalse(populatedList.containsAll(superset));
    }

    @Test
    public void testGetIndexOfLastIndexOf() {
        assertEquals(1, populatedList.indexOf("b"));
        assertEquals(-1, populatedList.indexOf("z"));
        assertEquals(2, populatedList.lastIndexOf("c"));
        // duplicate element test
        populatedList.add("a");
        assertEquals(0, populatedList.indexOf("a"));
        assertEquals(3, populatedList.lastIndexOf("a"));
    }

    @Test
    public void testIteratorSnapshotIsImmutable() {
        Iterator<String> it = populatedList.iterator();
        // modify list after iterator creation
        populatedList.add("d");
        // iterator should not see the new element
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testListIteratorSnapshotIsImmutable() {
        ListIterator<String> lit = populatedList.listIterator();
        // modify underlying list
        populatedList.remove("b");
        // iterator snapshot should still contain original elements
        assertTrue(lit.hasNext());
        assertEquals("a", lit.next());
        assertEquals("b", lit.next()); // 'b' still present in snapshot
        assertEquals("c", lit.next());
        assertFalse(lit.hasNext());
    }

    @Test
    public void testToArray() {
        Object[] arr = populatedList.toArray();
        assertArrayEquals(new Object[]{"a", "b", "c"}, arr);
    }

    @Test
    public void testToArrayWithTypedArray() {
        String[] dest = new String[5];
        dest[3] = "x"; // ensure extra slots are untouched
        String[] result = populatedList.toArray(dest);
        // result should be the same array instance
        assertSame(dest, result);
        assertArrayEquals(new String[]{"a", "b", "c", null, "x"}, result);
    }

    /* -------------------- Mutable APIs -------------------- */

    @Test
    public void testAddAndAddAtIndex() {
        assertTrue(emptyList.add("first"));
        assertEquals(1, emptyList.size());
        assertEquals("first", emptyList.get(0));

        emptyList.add(0, "zero");
        assertEquals(2, emptyList.size());
        assertEquals("zero", emptyList.get(0));
        assertEquals("first", emptyList.get(1));
    }

    @Test
    public void testAddAllCollection() {
        Collection<String> coll = Arrays.asList("x", "y");
        assertTrue(emptyList.addAll(coll));
        assertEquals(2, emptyList.size());
        assertEquals("x", emptyList.get(0));
        assertEquals("y", emptyList.get(1));
    }

    @Test
    public void testAddAllAtIndex() {
        Collection<String> coll = Arrays.asList("x", "y");
        populatedList.addAll(1, coll);
        assertEquals(5, populatedList.size());
        assertEquals("a", populatedList.get(0));
        assertEquals("x", populatedList.get(1));
        assertEquals("y", populatedList.get(2));
        assertEquals("b", populatedList.get(3));
        assertEquals("c", populatedList.get(4));
    }

    @Test
    public void testSet() {
        String old = populatedList.set(1, "newB");
        assertEquals("b", old);
        assertEquals("newB", populatedList.get(1));
    }

    @Test
    public void testRemoveByIndex() {
        String removed = populatedList.remove(0);
        assertEquals("a", removed);
        assertEquals(2, populatedList.size());
        assertEquals("b", populatedList.get(0));
    }

    @Test
    public void testRemoveObject() {
        assertTrue(populatedList.remove("b"));
        assertFalse(populatedList.contains("b"));
        assertFalse(populatedList.remove("nonexistent"));
    }

    @Test
    public void testRemoveAll() {
        Collection<String> toRemove = Arrays.asList("a", "c");
        assertTrue(populatedList.removeAll(toRemove));
        assertEquals(1, populatedList.size());
        assertEquals("b", populatedList.get(0));
    }

    @Test
    public void testRetainAll() {
        Collection<String> toRetain = Arrays.asList("a", "c");
        assertTrue(populatedList.retainAll(toRetain));
        assertEquals(2, populatedList.size());
        assertTrue(populatedList.containsAll(toRetain));
    }

    @Test
    public void testClear() {
        populatedList.clear();
        assertTrue(populatedList.isEmpty());
        assertEquals(0, populatedList.size());
    }

    @Test
    public void testAddNullElementsSupported() {
        emptyList.add(null);
        assertEquals(1, emptyList.size());
        assertNull(emptyList.get(0));
        assertTrue(emptyList.contains(null));
    }

    /* -------------------- Unsupported Operations -------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorWithIndexUnsupported() {
        populatedList.listIterator(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSubListUnsupported() {
        populatedList.subList(0, 1);
    }

    /* -------------------- Boundary & Error Conditions -------------------- */

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetOutOfBounds() {
        emptyList.get(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAtInvalidIndex() {
        emptyList.add(5, "oops");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetOutOfBounds() {
        populatedList.set(10, "nope");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveAtInvalidIndex() {
        populatedList.remove(5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAllAtInvalidIndex() {
        populatedList.addAll(4, Arrays.asList("x"));
    }

    /* -------------------- Concurrency Visibility Check -------------------- */

    @Test
    public void testIteratorSnapshotDuringConcurrentModification() throws Exception {
        // Create iterator snapshot
        Iterator<String> it = populatedList.iterator();

        // Simulate concurrent modification in another thread
        Thread modifier = new Thread(() -> populatedList.add("concurrent"));
        modifier.start();
        modifier.join();

        // The iterator must still reflect the original state
        assertEquals(3, populatedList.size()); // after join, size is 4, but iterator snapshot size is 3
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());
    }
}
