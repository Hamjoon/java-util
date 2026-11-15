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
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentList}.
 *
 * The tests cover:
 * <ul>
 *   <li>construction (stand‑alone and wrapper mode, null argument validation)</li>
 *   <li>all mutable operations (add, addAll, add at index, set, remove, retainAll, clear, …)</li>
 *   <li>all immutable/query operations (size, isEmpty, contains, containsAll, indexOf, …)</li>
 *   <li>iterator and listIterator snapshot behaviour</li>
 *   <li>unsupported operations (listIterator(int), subList)</li>
 *   <li>equals, hashCode and toString delegating to the backing list</li>
 *   <li>null element handling (ArrayList permits null)</li>
 * </ul>
 */
public class ConcurrentListTest {

    /* -------------------------------------------------
     *  Construction & basic properties
     * ------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void constructorRejectsNullList() {
        new ConcurrentList<String>(null);
    }

    @Test
    public void standaloneConstructorCreatesEmptyList() {
        ConcurrentList<String> list = new ConcurrentList<>();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void wrapperConstructorReflectsUnderlyingList() {
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b"));
        ConcurrentList<String> list = new ConcurrentList<>(backing);

        // changes via ConcurrentList are visible in the backing list
        list.add("c");
        assertEquals(3, backing.size());
        assertTrue(backing.contains("c"));

        // changes via backing list are visible in ConcurrentList
        backing.add("d");
        assertEquals(4, list.size());
        assertTrue(list.contains("d"));
    }

    /* -------------------------------------------------
     *  Mutating operations
     * ------------------------------------------------- */

    @Test
    public void addAndAddAtIndex() {
        ConcurrentList<String> list = new ConcurrentList<>();
        assertTrue(list.add("first"));
        assertTrue(list.add("third"));
        list.add(1, "second"); // insert at index 1

        assertEquals(3, list.size());
        assertEquals("first", list.get(0));
        assertEquals("second", list.get(1));
        assertEquals("third", list.get(2));
    }

    @Test
    public void setReplacesElement() {
        ConcurrentList<Integer> list = new ConcurrentList<>(Arrays.asList(1, 2, 3));
        Integer previous = list.set(1, 20);
        assertEquals(Integer.valueOf(2), previous);
        assertEquals(Integer.valueOf(20), list.get(1));
    }

    @Test
    public void removeByIndexAndObject() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("x", "y", "z"));
        String removed = list.remove(1);
        assertEquals("y", removed);
        assertFalse(list.contains("y"));
        assertTrue(list.remove("z"));
        assertFalse(list.contains("z"));
        assertEquals(1, list.size());
    }

    @Test
    public void addAllAndAddAllAtIndex() {
        ConcurrentList<String> list = new ConcurrentList<>();
        Collection<String> c1 = Arrays.asList("a", "b");
        assertTrue(list.addAll(c1));
        assertEquals(2, list.size());

        Collection<String> c2 = Arrays.asList("c", "d");
        assertTrue(list.addAll(1, c2)); // a c d b
        assertEquals("[a, c, d, b]", list.toString());
    }

    @Test
    public void removeAllAndRetainAll() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("a", "b", "c", "d"));
        assertTrue(list.removeAll(Arrays.asList("b", "c")));
        assertEquals(Arrays.asList("a", "d"), list);
        assertTrue(list.retainAll(Arrays.asList("a"))); // keep only "a"
        assertEquals(1, list.size());
        assertEquals("a", list.get(0));
    }

    @Test
    public void clearEmptiesList() {
        ConcurrentList<Integer> list = new ConcurrentList<>(Arrays.asList(1, 2, 3));
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void supportsNullElements() {
        ConcurrentList<String> list = new ConcurrentList<>();
        assertTrue(list.add(null));
        assertTrue(list.contains(null));
        assertNull(list.get(0));
        assertTrue(list.remove(null));
        assertTrue(list.isEmpty());
    }

    /* -------------------------------------------------
     *  Query operations
     * ------------------------------------------------- */

    @Test
    public void containsAndContainsAll() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("alpha", "beta"));
        assertTrue(list.contains("alpha"));
        assertFalse(list.contains("gamma"));

        Collection<String> subset = Arrays.asList("alpha", "beta");
        assertTrue(list.containsAll(subset));

        Collection<String> superset = Arrays.asList("alpha", "beta", "gamma");
        assertFalse(list.containsAll(superset));
    }

    @Test
    public void indexOfAndLastIndexOf() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("dup", "unique", "dup"));
        assertEquals(0, list.indexOf("dup"));
        assertEquals(2, list.lastIndexOf("dup"));
        assertEquals(1, list.indexOf("unique"));
        assertEquals(-1, list.indexOf("missing"));
    }

    @Test
    public void equalsAndHashCodeDelegateToBackingList() {
        List<String> backing = new ArrayList<>(Arrays.asList("x", "y"));
        ConcurrentList<String> list = new ConcurrentList<>(backing);

        // equals with another list having same contents
        List<String> other = Arrays.asList("x", "y");
        assertTrue(list.equals(other));
        assertEquals(other.hashCode(), list.hashCode());

        // modify backing list – equals reflects change
        backing.add("z");
        assertFalse(list.equals(other));
        assertNotEquals(other.hashCode(), list.hashCode());
    }

    @Test
    public void toStringReflectsContents() {
        ConcurrentList<Integer> list = new ConcurrentList<>(Arrays.asList(1, 2, 3));
        assertEquals("[1, 2, 3]", list.toString());
    }

    @Test
    public void toArrayVariants() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("a", "b"));
        Object[] arr = list.toArray();
        assertArrayEquals(new Object[]{"a", "b"}, arr);

        String[] typed = list.toArray(new String[0]);
        assertArrayEquals(new String[]{"a", "b"}, typed);
    }

    /* -------------------------------------------------
     *  Iterator behaviour (snapshot, read‑only)
     * ------------------------------------------------- */

    @Test
    public void iteratorIsSnapshotAndReadOnly() {
        ConcurrentList<String> list = new ConcurrentList<>(Arrays.asList("one", "two"));
        Iterator<String> it = list.iterator();

        // modify the list after iterator creation
        list.add("three");
        list.remove("one");

        // iterator should still see the original snapshot
        assertTrue(it.hasNext());
        assertEquals("one", it.next());
        assertEquals("two", it.next());
        assertFalse(it.hasNext());

        // iterator does not support remove()
        try {
            it.remove();
            fail("Iterator.remove() should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void listIteratorIsSnapshotAndReadOnly() {
        ConcurrentList<Integer> list = new ConcurrentList<>(Arrays.asList(10, 20, 30));
        ListIterator<Integer> lit = list.listIterator();

        // mutate underlying list
        list.clear();
        list.addAll(Arrays.asList(40, 50));

        // iterator should still iterate over the snapshot taken at creation time
        assertTrue(lit.hasNext());
        assertEquals(Integer.valueOf(10), lit.next());
        assertEquals(Integer.valueOf(20), lit.next());
        assertEquals(Integer.valueOf(30), lit.next());
        assertFalse(lit.hasNext());

        // verify read‑only nature
        try {
            lit.remove();
            fail("ListIterator.remove() should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            lit.set(99);
            fail("ListIterator.set() should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
        try {
            lit.add(99);
            fail("ListIterator.add() should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    /* -------------------------------------------------
     *  Unsupported operations
     * ------------------------------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void listIteratorWithIndexIsUnsupported() {
        new ConcurrentList<>().listIterator(5);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void subListIsUnsupported() {
        new ConcurrentList<>().subList(0, 0);
    }

    /* -------------------------------------------------
     *  Edge cases – invalid indices delegate to underlying list
     * ------------------------------------------------- */

    @Test(expected = IndexOutOfBoundsException.class)
    public void getInvalidIndexThrows() {
        new ConcurrentList<>(Arrays.asList("a")).get(2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void addAtInvalidIndexThrows() {
        new ConcurrentList<String>().add(5, "oops");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void setInvalidIndexThrows() {
        new ConcurrentList<String>(Arrays.asList("x")).set(-1, "y");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void removeInvalidIndexThrows() {
        new ConcurrentList<String>(Arrays.asList("x")).remove(3);
    }
}
