package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * JUnit‑4 test suite for {@link ConcurrentList}.
 *
 * The test class follows the requested steps:
 * 1. Public method extraction (see the comment block below).
 * 2. Basic functionality tests for each method.
 * 3. Identification of edge‑cases / exception scenarios.
 * 4. Targeted tests for those edge‑cases.
 */
public class ConcurrentListTest {

    /* -------------------------------------------------------------
     *  PUBLIC METHODS OF ConcurrentList (including constructors)
     * -------------------------------------------------------------
     *
     * Constructors
     *   ConcurrentList()
     *   ConcurrentList(List<E> list)               // throws IllegalArgumentException if list == null
     *
     * Immutable APIs
     *   boolean equals(Object other)
     *   int hashCode()
     *   String toString()
     *   int size()
     *   boolean isEmpty()
     *   boolean contains(Object o)
     *   boolean containsAll(Collection<?> c)
     *   E get(int index)                           // throws IndexOutOfBoundsException
     *   int indexOf(Object o)
     *   int lastIndexOf(Object o)
     *   Iterator<E> iterator()
     *   Object[] toArray()
     *   <T> T[] toArray(T[] a)
     *
     * Mutable APIs
     *   boolean add(E e)                           // permits null
     *   boolean addAll(Collection<? extends E> c)   // permits null elements
     *   boolean addAll(int index, Collection<? extends E> c) // IndexOutOfBoundsException
     *   void add(int index, E element)             // IndexOutOfBoundsException
     *   E set(int index, E element)                // IndexOutOfBoundsException
     *   E remove(int index)                        // IndexOutOfBoundsException
     *   boolean remove(Object o)
     *   boolean removeAll(Collection<?> c)
     *   boolean retainAll(Collection<?> c)
     *   void clear()
     *   ListIterator<E> listIterator()
     *
     * Unsupported operations
     *   ListIterator<E> listIterator(int index)   // UnsupportedOperationException
     *   List<E> subList(int fromIndex, int toIndex) // UnsupportedOperationException
     *
     * ----------------------------------------------------------------- */

    private ConcurrentList<String> emptyList;
    private ConcurrentList<String> populatedList;

    @Before
    public void setUp() {
        emptyList = new ConcurrentList<>();
        populatedList = new ConcurrentList<>(new ArrayList<>(Arrays.asList("alpha", "beta", "gamma")));
    }

    /* ==============================
     *  Constructor Tests
     * ============================== */

    @Test
    public void testNoArgConstructorCreatesEmptyList() {
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
    }

    @Test
    public void testWrappingConstructorUsesProvidedList() {
        List<String> backing = new ArrayList<>(Arrays.asList("one", "two"));
        ConcurrentList<String> cl = new ConcurrentList<>(backing);
        assertEquals(2, cl.size());
        assertTrue(cl.contains("one"));
        // Mutate backing list and verify view reflects change (wrap, not copy)
        backing.add("three");
        assertEquals(3, cl.size());
        assertTrue(cl.contains("three"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrappingConstructorRejectsNull() {
        new ConcurrentList<String>(null);
    }

    /* ==============================
     *  Immutable API Tests
     * ============================== */

    @Test
    public void testEqualsAndHashCode() {
        ConcurrentList<String> another = new ConcurrentList<>(new ArrayList<>(Arrays.asList("alpha", "beta", "gamma")));
        assertTrue(populatedList.equals(another));
        assertEquals(populatedList.hashCode(), another.hashCode());

        // Different content
        another.add("delta");
        assertFalse(populatedList.equals(another));
    }

    @Test
    public void testToString() {
        String s = populatedList.toString();
        assertTrue(s.contains("alpha"));
        assertTrue(s.contains("beta"));
        assertTrue(s.contains("gamma"));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(3, populatedList.size());
        assertFalse(populatedList.isEmpty());

        assertEquals(0, emptyList.size());
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testContainsAndContainsAll() {
        assertTrue(populatedList.contains("beta"));
        assertFalse(populatedList.contains("delta"));

        assertTrue(populatedList.containsAll(Arrays.asList("alpha", "gamma")));
        assertFalse(populatedList.containsAll(Arrays.asList("alpha", "delta")));

        // containsAll with empty collection should be true
        assertTrue(populatedList.containsAll(Collections.emptyList()));
    }

    @Test
    public void testGetValidIndex() {
        assertEquals("alpha", populatedList.get(0));
        assertEquals("gamma", populatedList.get(2));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetNegativeIndexThrows() {
        populatedList.get(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetIndexEqualSizeThrows() {
        populatedList.get(populatedList.size());
    }

    @Test
    public void testIndexOfAndLastIndexOf() {
        populatedList.add("beta"); // duplicate value
        assertEquals(1, populatedList.indexOf("beta"));
        assertEquals(3, populatedList.lastIndexOf("beta"));
        assertEquals(-1, populatedList.indexOf("delta"));
    }

    @Test
    public void testIteratorSnapshotIsReadOnly() {
        Iterator<String> it = populatedList.iterator();
        // Modify underlying list after iterator creation
        populatedList.add("delta");
        // Iterator should still reflect the original snapshot (size 3)
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testToArray() {
        Object[] arr = populatedList.toArray();
        assertArrayEquals(new Object[]{"alpha", "beta", "gamma"}, arr);
    }

    @Test
    public void testToArrayWithTypedArray() {
        String[] a = new String[0];
        String[] result = populatedList.toArray(a);
        assertArrayEquals(new String[]{"alpha", "beta", "gamma"}, result);
    }

    /* ==============================
     *  Mutable API Tests
     * ============================== */

    @Test
    public void testAddElement() {
        assertTrue(emptyList.add("new"));
        assertEquals(1, emptyList.size());
        assertEquals("new", emptyList.get(0));
    }

    @Test
    public void testAddNullElement() {
        assertTrue(emptyList.add(null));
        assertEquals(1, emptyList.size());
        assertNull(emptyList.get(0));
    }

    @Test
    public void testAddAllCollection() {
        List<String> toAdd = Arrays.asList("one", "two");
        assertTrue(emptyList.addAll(toAdd));
        assertEquals(2, emptyList.size());
        assertTrue(emptyList.containsAll(toAdd));
    }

    @Test
    public void testAddAllEmptyCollectionReturnsFalse() {
        assertFalse(emptyList.addAll(Collections.emptyList()));
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testAddAllAtIndex() {
        List<String> more = Arrays.asList("x", "y");
        populatedList.addAll(1, more); // insert after "alpha"
        assertEquals(5, populatedList.size());
        assertEquals("alpha", populatedList.get(0));
        assertEquals("x", populatedList.get(1));
        assertEquals("y", populatedList.get(2));
        assertEquals("beta", populatedList.get(3));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAllAtInvalidIndexThrows() {
        populatedList.addAll(5, Arrays.asList("oops"));
    }

    @Test
    public void testAddAtIndex() {
        populatedList.add(0, "zero");
        assertEquals(4, populatedList.size());
        assertEquals("zero", populatedList.get(0));
        assertEquals("alpha", populatedList.get(1));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAtInvalidIndexThrows() {
        populatedList.add(10, "bad");
    }

    @Test
    public void testSetReplacesAndReturnsOld() {
        String old = populatedList.set(1, "newBeta");
        assertEquals("beta", old);
        assertEquals("newBeta", populatedList.get(1));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetInvalidIndexThrows() {
        populatedList.set(-1, "bad");
    }

    @Test
    public void testRemoveByIndex() {
        String removed = populatedList.remove(1); // removes "beta"
        assertEquals("beta", removed);
        assertEquals(2, populatedList.size());
        assertFalse(populatedList.contains("beta"));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveByInvalidIndexThrows() {
        populatedList.remove(3);
    }

    @Test
    public void testRemoveObject() {
        assertTrue(populatedList.remove("beta"));
        assertFalse(populatedList.contains("beta"));
        assertEquals(2, populatedList.size());

        // Removing non‑existent element returns false
        assertFalse(populatedList.remove("delta"));
    }

    @Test
    public void testRemoveAll() {
        assertTrue(populatedList.removeAll(Arrays.asList("alpha", "gamma")));
        assertEquals(1, populatedList.size());
        assertEquals("beta", populatedList.get(0));
    }

    @Test
    public void testRetainAll() {
        assertTrue(populatedList.retainAll(Collections.singleton("beta")));
        assertEquals(1, populatedList.size());
        assertEquals("beta", populatedList.get(0));
    }

    @Test
    public void testClearEmptiesList() {
        populatedList.clear();
        assertTrue(populatedList.isEmpty());
        assertEquals(0, populatedList.size());
    }

    @Test
    public void testListIteratorSnapshot() {
        ListIterator<String> it = populatedList.listIterator();
        assertTrue(it.hasNext());
        assertEquals("alpha", it.next());
        // Mutate underlying list
        populatedList.add("delta");
        // The iterator should not see the new element
        assertEquals("beta", it.next());
        assertEquals("gamma", it.next());
        assertFalse(it.hasNext());
    }

    /* ==============================
     *  Unsupported Operation Tests
     * ============================== */

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorWithIndexUnsupported() {
        populatedList.listIterator(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSubListUnsupported() {
        populatedList.subList(0, 1);
    }

    /* ==========================================================
     *  Additional Edge‑Case Tests for Concurrency‑Safe APIs
     * ========================================================== */

    @Test
    public void testContainsNullWhenListHasNull() {
        emptyList.add(null);
        assertTrue(emptyList.contains(null));
    }

    @Test
    public void testContainsAllWithNullElements() {
        List<String> withNull = Arrays.asList(null, "alpha");
        emptyList.addAll(withNull);
        assertTrue(emptyList.containsAll(withNull));
    }

    @Test
    public void testAddAllAtIndexWithEmptyCollection() {
        // Should be a no‑op and return false without throwing
        assertFalse(populatedList.addAll(1, Collections.emptyList()));
        assertEquals(3, populatedList.size());
    }

    @Test
    public void testIteratorDoesNotAllowModification() {
        Iterator<String> it = populatedList.iterator();
        try {
            // The iterator returned is from a copy, so remove() is unsupported
            it.remove();
            fail("Expected UnsupportedOperationException from iterator.remove()");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }
}
