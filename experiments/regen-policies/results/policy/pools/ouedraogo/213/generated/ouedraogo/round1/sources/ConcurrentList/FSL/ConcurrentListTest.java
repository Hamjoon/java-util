package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.junit.Test;

/**
 * Unit tests for {@link ConcurrentList}.  All public methods are exercised,
 * including edge‑cases such as null handling, snapshot iterators and the
 * unsupported operations.
 */
public class ConcurrentListTest {

    /* ---------- Constructor tests ---------- */

    @Test
    public void testDefaultConstructorCreatesEmptyList() {
        ConcurrentList<String> list = new ConcurrentList<>();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testCapacityConstructorCreatesEmptyList() {
        ConcurrentList<Integer> list = new ConcurrentList<>(10);
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testWrapperConstructorUsesProvidedList() {
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b"));
        ConcurrentList<String> list = new ConcurrentList<>(backing);
        // changes through the wrapper must affect the backing list
        list.add("c");
        assertEquals(3, backing.size());
        assertEquals("c", backing.get(2));
        // and vice‑versa
        backing.add("d");
        assertEquals(4, list.size());
        assertEquals("d", list.get(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrapperConstructorRejectsNull() {
        new ConcurrentList<>(null);
    }

    /* ---------- Immutable API tests ---------- */

    @Test
    public void testEqualsAndHashCode() {
        List<String> backing = new ArrayList<>(Arrays.asList("x", "y"));
        ConcurrentList<String> list = new ConcurrentList<>(backing);
        assertEquals(backing, list);
        assertEquals(backing.hashCode(), list.hashCode());
        // equality with another ConcurrentList wrapping same contents
        ConcurrentList<String> other = new ConcurrentList<>(new ArrayList<>(backing));
        assertEquals(list, other);
        assertEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testToString() {
        ConcurrentList<Integer> list = new ConcurrentList<>(Arrays.asList(1, 2, 3));
        assertEquals("[1, 2, 3]", list.toString());
    }

    @Test
    public void testContainsAndContainsAll() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("alpha", "beta", "gamma"));
        assertTrue(list.contains("beta"));
        assertFalse(list.contains("delta"));

        Collection<String> sub = Arrays.asList("alpha", "gamma");
        assertTrue(list.containsAll(sub));

        Collection<String> sup = Arrays.asList("alpha", "beta", "gamma", "delta");
        assertFalse(list.containsAll(sup));
    }

    @Test
    public void testIndexOfAndLastIndexOf() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("a", "b", "a", "c"));
        assertEquals(0, list.indexOf("a"));
        assertEquals(2, list.lastIndexOf("a"));
        assertEquals(-1, list.indexOf("z"));
    }

    @Test
    public void testGetAndSize() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("one", "two"));
        assertEquals(2, list.size());
        assertEquals("one", list.get(0));
        assertEquals("two", list.get(1));
    }

    @Test
    public void testIsEmpty() {
        ConcurrentList<Object> empty = new ConcurrentList<>();
        assertTrue(empty.isEmpty());
        empty.add(new Object());
        assertFalse(empty.isEmpty());
    }

    @Test
    public void testToArray() {
        ConcurrentList<Integer> list = new ConcurrentList<>(Arrays.asList(5, 10, 15));
        Object[] arr = list.toArray();
        assertArrayEquals(new Object[]{5, 10, 15}, arr);

        Integer[] intArr = list.toArray(new Integer[0]);
        assertArrayEquals(new Integer[]{5, 10, 15}, intArr);
    }

    /* ---------- Mutable API tests ---------- */

    @Test
    public void testAddAndAddAtIndex() {
        ConcurrentList<String> list = new ConcurrentList<>();
        list.add("first");
        list.add(0, "zero");
        assertEquals(2, list.size());
        assertEquals("zero", list.get(0));
        assertEquals("first", list.get(1));
    }

    @Test
    public void testAddAllCollection() {
        ConcurrentList<String> list = new ConcurrentList<>();
        Collection<String> col = Arrays.asList("a", "b", "c");
        assertTrue(list.addAll(col));
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
    }

    @Test
    public void testAddAllAtIndex() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("x", "y"));
        Collection<String> extra = Arrays.asList("a", "b");
        list.addAll(1, extra);
        assertEquals(Arrays.asList("x", "a", "b", "y"), list);
    }

    @Test
    public void testSet() {
        ConcurrentList<Integer> list = new ConcurrentList<>(Arrays.asList(1, 2, 3));
        Integer old = list.set(1, 20);
        assertEquals(Integer.valueOf(2), old);
        assertEquals(Integer.valueOf(20), list.get(1));
    }

    @Test
    public void testRemoveByIndexAndObject() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("a", "b", "c"));
        String removed = list.remove(1);
        assertEquals("b", removed);
        assertFalse(list.contains("b"));
        assertTrue(list.remove("c"));
        assertFalse(list.contains("c"));
        assertFalse(list.remove("non‑existent"));
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        ConcurrentList<Integer> list = new ConcurrentList<>(Arrays.asList(1, 2, 3, 4, 5));
        assertTrue(list.removeAll(Arrays.asList(2, 4)));
        assertEquals(Arrays.asList(1, 3, 5), list);
        assertTrue(list.retainAll(Arrays.asList(1, 5)));
        assertEquals(Arrays.asList(1, 5), list);
    }

    @Test
    public void testClear() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("x", "y"));
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testNullElementsSupportedWhenBackingListAllowsThem() {
        List<String> backing = new ArrayList<>();
        ConcurrentList<String> list = new ConcurrentList<>(backing);
        assertTrue(list.add(null));
        assertTrue(list.contains(null));
        assertNull(list.get(0));
        assertTrue(list.remove(null));
        assertFalse(list.contains(null));
    }

    /* ---------- Iterator snapshot tests ---------- */

    @Test
    public void testIteratorIsSnapshot() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("a", "b"));
        Iterator<String> it = list.iterator();

        // modify the list after iterator creation
        list.add("c");
        list.remove("a");

        // iterator should reflect the state at creation time: ["a","b"]
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        assertTrue(it.hasNext());
        assertEquals("b", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testListIteratorIsSnapshot() {
        ConcurrentList<Integer> list = new ConcurrentList<>(Arrays.asList(10, 20, 30));
        ListIterator<Integer> li = list.listIterator();

        // change underlying list
        list.set(1, 99);
        list.add(40);

        // snapshot should still contain original values
        assertEquals(Integer.valueOf(10), li.next());
        assertEquals(Integer.valueOf(20), li.next());
        assertEquals(Integer.valueOf(30), li.next());
        assertFalse(li.hasNext());

        // verify backward traversal works on the snapshot
        assertTrue(li.hasPrevious());
        assertEquals(Integer.valueOf(30), li.previous());
    }

    /* ---------- Unsupported operations ---------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorWithIndexUnsupported() {
        ConcurrentList<String> list = new ConcurrentList<>();
        list.listIterator(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSubListUnsupported() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("x"));
        list.subList(0, 1);
    }

    /* ---------- Equality with non‑list objects ---------- */

    @Test
    public void testEqualsWithDifferentObjectTypes() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("a"));
        assertFalse(list.equals("a"));
        assertFalse(list.equals(null));
    }

    /* ---------- Additional sanity checks ---------- */

    @Test
    public void testHashCodeConsistencyAfterMutations() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("one", "two"));
        int originalHash = list.hashCode();
        list.add("three");
        assertNotEquals(originalHash, list.hashCode());
    }

    @Test
    public void testToStringAfterMutations() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("alpha"));
        String before = list.toString();
        list.add("beta");
        String after = list.toString();
        assertEquals("[alpha]", before);
        assertEquals("[alpha, beta]", after);
    }
}
