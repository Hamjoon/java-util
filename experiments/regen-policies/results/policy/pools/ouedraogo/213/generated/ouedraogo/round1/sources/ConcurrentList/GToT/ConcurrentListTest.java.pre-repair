package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class ConcurrentListTest {

    private ConcurrentList<String> emptyList;
    private ConcurrentList<String> populatedList;
    private List<String> backingList;

    /* ----------------------------------------------------------------------
     * Helper methods to create test data
     * ---------------------------------------------------------------------- */
    private static List<String> asList(String... elems) {
        return new ArrayList<>(Arrays.asList(elems));
    }

    /* ----------------------------------------------------------------------
     * Setup
     * ---------------------------------------------------------------------- */
    @Before
    public void setUp() {
        emptyList = new ConcurrentList<>();
        backingList = asList("a", "b", "c", "d");
        populatedList = new ConcurrentList<>(backingList);
    }

    /* ----------------------------------------------------------------------
     * 1. Constructors
     * ---------------------------------------------------------------------- */
    @Test
    public void testNoArgConstructorCreatesEmptyList() {
        assertEquals(0, emptyList.size());
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testCapacityConstructorCreatesEmptyList() {
        ConcurrentList<Integer> list = new ConcurrentList<>(10);
        assertEquals(0, list.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrappingNullListThrowsException() {
        new ConcurrentList<String>(null);
    }

    @Test
    public void testWrappingExistingListReflectsChanges() {
        List<String> original = new ArrayList<>(asList("x", "y"));
        ConcurrentList<String> wrapper = new ConcurrentList<>(original);
        wrapper.add("z");
        assertEquals(3, original.size());
        assertEquals("z", original.get(2));
    }

    /* ----------------------------------------------------------------------
     * 2. Immutable APIs
     * ---------------------------------------------------------------------- */
    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(4, populatedList.size());
        assertFalse(populatedList.isEmpty());

        emptyList.add("one");
        assertEquals(1, emptyList.size());
        assertFalse(emptyList.isEmpty());
    }

    @Test
    public void testContainsAndContainsAll() {
        assertTrue(populatedList.contains("b"));
        assertFalse(populatedList.contains("z"));

        assertTrue(populatedList.containsAll(Arrays.asList("a", "c")));
        assertFalse(populatedList.containsAll(Arrays.asList("a", "z")));
    }

    @Test
    public void testGetAndIndexOf() {
        assertEquals("c", populatedList.get(2));
        assertEquals(2, populatedList.indexOf("c"));
        assertEquals(-1, populatedList.indexOf("z"));
        assertEquals(2, populatedList.lastIndexOf("c"));
    }

    @Test
    public void testToArray() {
        Object[] arr = populatedList.toArray();
        assertArrayEquals(new Object[]{"a", "b", "c", "d"}, arr);

        String[] typed = new String[0];
        String[] result = populatedList.toArray(typed);
        assertArrayEquals(new String[]{"a", "b", "c", "d"}, result);
    }

    @Test
    public void testIteratorIsSnapshotAndReadOnly() {
        Iterator<String> it = populatedList.iterator();
        // modify underlying list after iterator creation
        populatedList.add("e");
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        assertEquals("b", it.next());

        // iterator should not see the newly added element
        List<String> collected = new ArrayList<>();
        it.forEachRemaining(collected::add);
        assertEquals(Arrays.asList("c", "d"), collected);
    }

    @Test
    public void testListIteratorIsSnapshot() {
        ListIterator<String> it = populatedList.listIterator();
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        assertEquals(1, it.nextIndex());

        // modify list after iterator creation
        populatedList.remove("b");
        // iterator should still see original element at next position
        assertEquals("b", it.next());
        assertEquals("c", it.next());
    }

    @Test
    public void testEqualsAndHashCode() {
        ConcurrentList<String> another = new ConcurrentList<>(asList("a", "b", "c", "d"));
        assertTrue(populatedList.equals(another));
        assertEquals(populatedList.hashCode(), another.hashCode());

        // after mutation they are no longer equal
        another.add("x");
        assertFalse(populatedList.equals(another));
    }

    @Test
    public void testToString() {
        String s = populatedList.toString();
        assertEquals("[a, b, c, d]", s);
    }

    /* ----------------------------------------------------------------------
     * 3. Mutable APIs
     * ---------------------------------------------------------------------- */
    @Test
    public void testAddAndAddAtIndex() {
        assertTrue(emptyList.add("first"));
        assertEquals(1, emptyList.size());
        emptyList.add(0, "zero");
        assertEquals("zero", emptyList.get(0));
        assertEquals("first", emptyList.get(1));
    }

    @Test
    public void testAddAllCollection() {
        List<String> toAdd = asList("x", "y");
        assertTrue(emptyList.addAll(toAdd));
        assertEquals(2, emptyList.size());
        assertEquals(toAdd, emptyList);
    }

    @Test
    public void testAddAllAtIndex() {
        List<String> toAdd = asList("x", "y");
        emptyList.addAll(0, toAdd);
        assertEquals(Arrays.asList("x", "y"), emptyList);
    }

    @Test
    public void testSet() {
        String old = populatedList.set(1, "beta");
        assertEquals("b", old);
        assertEquals("beta", populatedList.get(1));
    }

    @Test
    public void testRemoveByIndexAndObject() {
        String removed = populatedList.remove(2);
        assertEquals("c", removed);
        assertFalse(populatedList.contains("c"));
        assertTrue(populatedList.remove("d"));
        assertFalse(populatedList.contains("d"));
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        List<String> toRemove = asList("a", "c");
        assertTrue(populatedList.removeAll(toRemove));
        assertEquals(Arrays.asList("b", "d"), populatedList);

        List<String> toRetain = asList("b");
        assertTrue(populatedList.retainAll(toRetain));
        assertEquals(Collections.singletonList("b"), populatedList);
    }

    @Test
    public void testClear() {
        populatedList.clear();
        assertTrue(populatedList.isEmpty());
        assertEquals(0, populatedList.size());
    }

    /* ----------------------------------------------------------------------
     * 4. Unsupported Operations
     * ---------------------------------------------------------------------- */
    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorWithIndexUnsupported() {
        populatedList.listIterator(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSubListUnsupported() {
        populatedList.subList(0, 2);
    }

    /* ----------------------------------------------------------------------
     * 5. Edge Cases & Exception Scenarios
     * ---------------------------------------------------------------------- */
    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetInvalidIndexThrows() {
        emptyList.get(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAtInvalidIndexThrows() {
        emptyList.add(5, "oops");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetInvalidIndexThrows() {
        emptyList.set(-1, "no");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveInvalidIndexThrows() {
        emptyList.remove(0);
    }

    @Test
    public void testAddNullElementIfBackingAllows() {
        ConcurrentList<String> list = new ConcurrentList<>(new ArrayList<String>());
        assertTrue(list.add(null));
        assertNull(list.get(0));
        assertTrue(list.contains(null));
    }

    @Test
    public void testIteratorThrowsNoSuchElementException() {
        Iterator<String> it = emptyList.iterator();
        try {
            it.next();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException ignored) {
        }
    }

    @Test
    public void testListIteratorThrowsNoSuchElementException() {
        ListIterator<String> it = emptyList.listIterator();
        try {
            it.previous();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException ignored) {
        }
    }

    @Test
    public void testConcurrentModificationDuringIterationDoesNotAffectSnapshot() {
        // iterator snapshot
        Iterator<String> it = populatedList.iterator();
        // modify list concurrently
        populatedList.add("new");
        populatedList.remove("a");
        // iterator should still iterate over original snapshot
        List<String> snapshot = new ArrayList<>();
        it.forEachRemaining(snapshot::add);
        assertEquals(Arrays.asList("a", "b", "c", "d"), snapshot);
        // final state reflects modifications
        assertEquals(Arrays.asList("b", "c", "d", "new"), populatedList);
    }

    @Test
    public void testReadWriteLockReentrancy() {
        // Simple re-entrancy test: call read operation from within another read operation
        int size = emptyList.readOperation(() -> emptyList.readOperation(() -> emptyList.size()));
        assertEquals(0, size);
    }

    /* ----------------------------------------------------------------------
     * 6. Verification of List contract (equals, hashCode, toArray, etc.)
     * ---------------------------------------------------------------------- */
    @Test
    public void testContractMethodsBehaveLikeStandardList() {
        List<String> std = new ArrayList<>(asList("a", "b", "c"));
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(asList("a", "b", "c")));

        assertEquals(std, cl);
        assertEquals(std.hashCode(), cl.hashCode());
        assertArrayEquals(std.toArray(), cl.toArray());
        assertArrayEquals(std.toArray(new String[0]), cl.toArray(new String[0]));
    }
}
