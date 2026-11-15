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
 * <p>All public methods (including constructors) are exercised with:
 * <ul>
 *   <li>basic functional tests</li>
 *   <li>edge‑case scenarios (empty list, null elements, out‑of‑bounds indices)</li>
 *   <li>exception‑handling verification</li>
 *   <li>behaviour specific to the read‑only snapshot iterators</li>
 * </ul>
 * </p>
 */
public class ConcurrentListTest {

    private ConcurrentList<String> emptyList;          // created with no‑arg ctor
    private ConcurrentList<String> sizedList;          // created with initial capacity ctor
    private ConcurrentList<String> wrappedList;        // wraps an existing ArrayList
    private List<String> backingList;                  // the list wrapped by {@code wrappedList}

    @Before
    public void setUp() {
        emptyList = new ConcurrentList<>();
        sizedList = new ConcurrentList<>(10);
        backingList = new ArrayList<>();
        wrappedList = new ConcurrentList<>(backingList);
    }

    /* ==================== Constructor Tests ==================== */

    @Test
    public void testNoArgConstructorCreatesEmptyList() {
        assertEquals(0, emptyList.size());
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testSizeConstructorCreatesEmptyList() {
        assertEquals(0, sizedList.size());
        assertTrue(sizedList.isEmpty());
    }

    @Test
    public void testWrapperConstructorUsesProvidedList() {
        backingList.add("alpha");
        assertEquals(1, wrappedList.size());
        assertEquals("alpha", wrappedList.get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrapperConstructorRejectsNull() {
        new ConcurrentList<String>(null);
    }

    /* ==================== Immutable API Tests ==================== */

    @Test
    public void testEqualsAndHashCode() {
        List<String> a = new ArrayList<>(Arrays.asList("x", "y"));
        List<String> b = new ArrayList<>(Arrays.asList("x", "y"));
        ConcurrentList<String> cl = new ConcurrentList<>(a);
        assertTrue(cl.equals(a));
        assertTrue(cl.equals(b)); // equal to another list with same contents
        assertEquals(a.hashCode(), cl.hashCode());
    }

    @Test
    public void testToStringContainsElements() {
        emptyList.add("one");
        emptyList.add("two");
        String s = emptyList.toString();
        assertTrue(s.contains("one"));
        assertTrue(s.contains("two"));
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(emptyList.isEmpty());
        emptyList.add("a");
        assertEquals(1, emptyList.size());
        assertFalse(emptyList.isEmpty());
    }

    @Test
    public void testContainsAndContainsAll() {
        emptyList.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(emptyList.contains("b"));
        assertFalse(emptyList.contains("z"));

        Collection<String> subset = Arrays.asList("a", "c");
        Collection<String> superset = Arrays.asList("a", "b", "c", "d");
        assertTrue(emptyList.containsAll(subset));
        assertFalse(emptyList.containsAll(superset));
    }

    @Test
    public void testGetIndexOfLastIndexOf() {
        emptyList.addAll(Arrays.asList("x", "y", "x"));
        assertEquals(0, emptyList.indexOf("x"));
        assertEquals(2, emptyList.lastIndexOf("x"));
        assertEquals(-1, emptyList.indexOf("z"));
    }

    @Test
    public void testIteratorSnapshotIsReadOnly() {
        emptyList.addAll(Arrays.asList("a", "b"));
        Iterator<String> it = emptyList.iterator(); // snapshot at this moment
        emptyList.add("c");                         // modify underlying list

        // iterator should not see the newly added element
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testListIteratorSnapshotIsReadOnly() {
        emptyList.addAll(Arrays.asList("1", "2"));
        ListIterator<String> li = emptyList.listIterator();
        emptyList.add("3"); // modify after iterator creation

        assertTrue(li.hasNext());
        assertEquals("1", li.next());
        assertEquals("2", li.next());
        assertFalse(li.hasNext());
    }

    @Test
    public void testToArray() {
        emptyList.addAll(Arrays.asList("x", "y"));
        Object[] arr = emptyList.toArray();
        assertArrayEquals(new Object[]{"x", "y"}, arr);
    }

    @Test
    public void testToArrayWithTypedArray() {
        emptyList.addAll(Arrays.asList("p", "q"));
        String[] a = new String[0];
        String[] result = emptyList.toArray(a);
        assertArrayEquals(new String[]{"p", "q"}, result);
    }

    /* ==================== Mutable API Tests ==================== */

    @Test
    public void testAddAndAddAtIndex() {
        assertTrue(emptyList.add("first"));
        assertEquals(1, emptyList.size());
        emptyList.add(0, "zero");
        assertEquals("zero", emptyList.get(0));
        assertEquals("first", emptyList.get(1));
    }

    @Test
    public void testAddAllAndAddAllAtIndex() {
        Collection<String> coll = Arrays.asList("a", "b");
        assertTrue(emptyList.addAll(coll));
        assertEquals(2, emptyList.size());

        Collection<String> more = Arrays.asList("c", "d");
        emptyList.addAll(1, more); // list becomes a,c,d,b
        assertEquals(Arrays.asList("a", "c", "d", "b"), toArrayList(emptyList));
    }

    @Test
    public void testSet() {
        emptyList.addAll(Arrays.asList("old"));
        String previous = emptyList.set(0, "new");
        assertEquals("old", previous);
        assertEquals("new", emptyList.get(0));
    }

    @Test
    public void testRemoveByIndexAndByObject() {
        emptyList.addAll(Arrays.asList("x", "y", "z"));
        String removed = emptyList.remove(1);
        assertEquals("y", removed);
        assertFalse(emptyList.contains("y"));

        assertTrue(emptyList.remove("z"));
        assertFalse(emptyList.contains("z"));
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        emptyList.addAll(Arrays.asList("a", "b", "c", "d"));
        assertTrue(emptyList.removeAll(Arrays.asList("b", "c")));
        assertEquals(Arrays.asList("a", "d"), toArrayList(emptyList));

        assertTrue(emptyList.retainAll(Arrays.asList("a"))); // keep only "a"
        assertEquals(Arrays.asList("a"), toArrayList(emptyList));
    }

    @Test
    public void testClearEmptiesTheList() {
        emptyList.addAll(Arrays.asList("one", "two"));
        emptyList.clear();
        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
    }

    @Test
    public void testNullElementsSupported() {
        emptyList.add(null);
        assertTrue(emptyList.contains(null));
        assertNull(emptyList.get(0));
        assertTrue(emptyList.remove(null));
        assertFalse(emptyList.contains(null));
    }

    /* ==================== Exception & Edge‑Case Tests ==================== */

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetWithInvalidIndexThrows() {
        emptyList.get(5);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetWithInvalidIndexThrows() {
        emptyList.set(-1, "x");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveWithInvalidIndexThrows() {
        emptyList.remove(0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAtInvalidIndexThrows() {
        emptyList.add(2, "y");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAllAtInvalidIndexThrows() {
        emptyList.addAll(3, Arrays.asList("a"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorWithIndexUnsupported() {
        emptyList.listIterator(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSubListUnsupported() {
        emptyList.subList(0, 1);
    }

    /* ==================== Wrapper‑Mode Specific Tests ==================== */

    @Test
    public void testWrapperReflectsExternalModifications() {
        backingList.add("external");
        assertEquals(1, wrappedList.size());
        assertEquals("external", wrappedList.get(0));

        // modify via wrapper
        wrappedList.add("wrapper");
        assertEquals(2, backingList.size());
        assertEquals("wrapper", backingList.get(1));
    }

    @Test
    public void testWrapperAndStandaloneAreIndependent() {
        emptyList.add("standalone");
        wrappedList.add("wrapped");
        assertEquals(1, emptyList.size());
        assertEquals(1, wrappedList.size());
        assertEquals("standalone", emptyList.get(0));
        assertEquals("wrapped", wrappedList.get(0));
    }

    /* ==================== Helper ==================== */

    /**
     * Convenience method to materialise a {@link ConcurrentList} as a normal {@link ArrayList}
     * for easy equality checks.
     */
    private static <E> List<E> toArrayList(ConcurrentList<E> cl) {
        List<E> result = new ArrayList<>(cl.size());
        for (E e : cl) {
            result.add(e);
        }
        return result;
    }
}
