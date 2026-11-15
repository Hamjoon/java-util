package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.junit.Test;

/**
 * Comprehensive unit tests for {@link ConcurrentList}.
 */
public class ConcurrentListTest {

    /* ---------- Constructor tests ---------- */

    @Test
    public void testNoArgConstructorCreatesEmptyList() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        assertEquals(0, cl.size());
        assertTrue(cl.isEmpty());
    }

    @Test
    public void testWrappingExistingListReflectsChanges() {
        List<String> backing = new ArrayList<>(Arrays.asList("alpha", "beta"));
        ConcurrentList<String> cl = new ConcurrentList<>(backing);

        // read through ConcurrentList
        assertEquals(2, cl.size());
        assertEquals("alpha", cl.get(0));
        assertEquals("beta", cl.get(1));

        // mutate via ConcurrentList and verify backing list sees the change
        cl.add("gamma");
        assertEquals(3, backing.size());
        assertEquals("gamma", backing.get(2));

        // mutate backing list directly and verify ConcurrentList sees it
        backing.add("delta");
        assertEquals(4, cl.size());
        assertEquals("delta", cl.get(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullThrows() {
        new ConcurrentList<>(null);
    }

    /* ---------- Immutable API tests ---------- */

    @Test
    public void testEqualsAndHashCodeDelegateToBackingList() {
        List<Integer> list1 = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Integer> list2 = new ArrayList<>(Arrays.asList(1, 2, 3));
        ConcurrentList<Integer> cl1 = new ConcurrentList<>(list1);
        ConcurrentList<Integer> cl2 = new ConcurrentList<>(list2);

        assertEquals(list1, cl1);
        assertEquals(cl1, cl2);
        assertEquals(list1.hashCode(), cl1.hashCode());
        assertEquals(cl1.hashCode(), cl2.hashCode());
    }

    @Test
    public void testToStringReflectsCurrentContent() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("x", "y")));
        assertEquals("[x, y]", cl.toString());

        cl.add("z");
        assertEquals("[x, y, z]", cl.toString());
    }

    @Test
    public void testContainsAllUsesSetSemantics() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c")));

        Collection<String> allPresent = Arrays.asList("a", "b", "b", "c");
        assertTrue(cl.containsAll(allPresent));

        Collection<String> missing = Arrays.asList("a", "d");
        assertFalse(cl.containsAll(missing));
    }

    @Test
    public void testIteratorProvidesSnapshot() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("one", "two")));
        Iterator<String> it = cl.iterator();

        // modify underlying list after iterator creation
        cl.add("three");

        // iterator should not see the newly added element
        assertTrue(it.hasNext());
        assertEquals("one", it.next());
        assertTrue(it.hasNext());
        assertEquals("two", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testListIteratorProvidesSnapshot() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList(10, 20, 30)));
        ListIterator<Integer> lit = cl.listIterator();

        // modify underlying list
        cl.remove(Integer.valueOf(20));

        // iterator snapshot should still contain original three elements
        assertTrue(lit.hasNext());
        assertEquals(Integer.valueOf(10), lit.next());
        assertEquals(Integer.valueOf(20), lit.next());
        assertEquals(Integer.valueOf(30), lit.next());
        assertFalse(lit.hasNext());

        // iterator's remove() works on the snapshot only, not on the backing list
        lit = cl.listIterator();
        lit.next(); // points at 10
        lit.remove(); // removes 10 from snapshot
        assertEquals(2, cl.size()); // backing list unchanged
    }

    @Test
    public void testToArrayVariants() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c")));

        Object[] objArray = cl.toArray();
        assertArrayEquals(new Object[]{"a", "b", "c"}, objArray);

        String[] strArray = cl.toArray(new String[0]);
        assertArrayEquals(new String[]{"a", "b", "c"}, strArray);
    }

    /* ---------- Mutable API tests ---------- */

    @Test
    public void testAddAndAddAtIndex() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.add("first");
        cl.add(0, "zero");
        assertEquals(2, cl.size());
        assertEquals("zero", cl.get(0));
        assertEquals("first", cl.get(1));
    }

    @Test
    public void testAddAllCollection() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList(1)));
        Collection<Integer> more = Arrays.asList(2, 3, 4);
        assertTrue(cl.addAll(more));
        assertEquals(Arrays.asList(1, 2, 3, 4), cl);
    }

    @Test
    public void testAddAllAtIndex() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "d")));
        Collection<String> middle = Arrays.asList("b", "c");
        assertTrue(cl.addAll(1, middle));
        assertEquals(Arrays.asList("a", "b", "c", "d"), cl);
    }

    @Test
    public void testSetAndGet() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("old")));
        String previous = cl.set(0, "new");
        assertEquals("old", previous);
        assertEquals("new", cl.get(0));
    }

    @Test
    public void testRemoveByIndexAndObject() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("x", "y", "z")));

        String removed = cl.remove(1);
        assertEquals("y", removed);
        assertEquals(2, cl.size());
        assertFalse(cl.contains("y"));

        assertTrue(cl.remove("z"));
        assertEquals(1, cl.size());
        assertFalse(cl.contains("z"));

        assertFalse(cl.remove("nonexistent"));
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5)));

        Collection<Integer> toRemove = Arrays.asList(2, 4, 6);
        assertTrue(cl.removeAll(toRemove));
        assertEquals(Arrays.asList(1, 3, 5), cl);

        Collection<Integer> toRetain = Arrays.asList(1, 5, 9);
        assertTrue(cl.retainAll(toRetain));
        assertEquals(Arrays.asList(1, 5), cl);
    }

    @Test
    public void testClearEmptiesList() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b")));
        cl.clear();
        assertTrue(cl.isEmpty());
        assertEquals(0, cl.size());
    }

    /* ---------- Unsupported operation tests ---------- */

    @Test
    public void testListIteratorWithIndexUnsupported() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        try {
            cl.listIterator(0);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertEquals("listIterator(index) not implemented for ConcurrentList", e.getMessage());
        }
    }

    @Test
    public void testSubListUnsupported() {
        ConcurrentList<Integer> cl = new ConcurrentList<>();
        try {
            cl.subList(0, 1);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            assertEquals("subList not implemented for ConcurrentList", e.getMessage());
        }
    }

    /* ---------- Additional edge‑case tests ---------- */

    @Test
    public void testContainsNullAndAddNull() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        assertFalse(cl.contains(null));
        cl.add(null);
        assertTrue(cl.contains(null));
        assertNull(cl.get(0));
    }

    @Test
    public void testIndexOfAndLastIndexOf() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "a", "c")));
        assertEquals(0, cl.indexOf("a"));
        assertEquals(2, cl.lastIndexOf("a"));
        assertEquals(-1, cl.indexOf("non"));
    }

    @Test
    public void testSizeAndIsEmptyAfterVariousOperations() {
        ConcurrentList<Integer> cl = new ConcurrentList<>();
        assertTrue(cl.isEmpty());
        cl.add(10);
        assertFalse(cl.isEmpty());
        assertEquals(1, cl.size());
        cl.remove(Integer.valueOf(10));
        assertTrue(cl.isEmpty());
        assertEquals(0, cl.size());
    }
}
