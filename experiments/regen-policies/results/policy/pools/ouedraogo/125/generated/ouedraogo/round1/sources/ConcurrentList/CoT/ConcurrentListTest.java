package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ConcurrentList}.
 *
 * The test class is organised as follows:
 *
 * 1.  List of all public constructors and methods (including signatures).
 * 2.  Basic functional tests – one test per public method that verifies the
 *     normal behaviour.
 * 3.  Edge‑case / exception tests – tests that provoke IllegalArgumentException,
 *     IndexOutOfBoundsException, UnsupportedOperationException, etc.
 * 4.  A merged, runnable JUnit‑4 test file.
 *
 * The tests use only the public API that is present in the source code supplied
 * in the prompt – no mocking, no external libraries beyond JUnit and the JDK.
 */
public class ConcurrentListTest {

    /** The list used in most tests – a fresh instance for each test method. */
    private ConcurrentList<String> list;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        list = new ConcurrentList<>();
    }

    /* ---------------------------------------------------------------------- */
    /* 1.  PUBLIC CONSTRUCTORS & METHOD SIGNATURES (for reference)           */
    /* ---------------------------------------------------------------------- */
    // Constructors
    //   ConcurrentList()
    //   ConcurrentList(int size)
    //   ConcurrentList(List<E> list)

    // Immutable APIs
    //   boolean equals(Object other)
    //   int hashCode()
    //   String toString()
    //   int size()
    //   boolean isEmpty()
    //   boolean contains(Object o)
    //   boolean containsAll(Collection<?> c)
    //   E get(int index)
    //   int indexOf(Object o)
    //   int lastIndexOf(Object o)
    //   Iterator<E> iterator()
    //   Object[] toArray()
    //   <T> T[] toArray(T[] a)

    // Mutable APIs
    //   boolean add(E e)
    //   boolean addAll(Collection<? extends E> c)
    //   boolean addAll(int index, Collection<? extends E> c)
    //   void add(int index, E element)
    //   E set(int index, E element)
    //   E remove(int index)
    //   boolean remove(Object o)
    //   boolean removeAll(Collection<?> c)
    //   boolean retainAll(Collection<?> c)
    //   void clear()
    //   ListIterator<E> listIterator()

    // Unsupported operations
    //   ListIterator<E> listIterator(int index)          // throws UnsupportedOperationException
    //   List<E> subList(int fromIndex, int toIndex)      // throws UnsupportedOperationException

    /* ---------------------------------------------------------------------- */
    /* 2.  BASIC FUNCTIONAL TESTS                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConstructorNoArgCreatesEmptyList() {
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        ConcurrentList<Integer> intList = new ConcurrentList<>(10);
        assertEquals(0, intList.size());
        assertTrue(intList.isEmpty());
    }

    @Test
    public void testConstructorWrappingExistingList() {
        List<String> backing = new ArrayList<>();
        backing.add("alpha");
        ConcurrentList<String> wrapped = new ConcurrentList<>(backing);
        assertEquals(1, wrapped.size());
        assertEquals("alpha", wrapped.get(0));
    }

    @Test
    public void testAddAndSize() {
        assertTrue(list.add("one"));
        assertTrue(list.add("two"));
        assertEquals(2, list.size());
        assertFalse(list.isEmpty());
    }

    @Test
    public void testAddNullElement() {
        assertTrue(list.add(null));
        assertEquals(1, list.size());
        assertNull(list.get(0));
    }

    @Test
    public void testAddAllCollection() {
        Collection<String> coll = Arrays.asList("a", "b", "c");
        assertTrue(list.addAll(coll));
        assertEquals(3, list.size());
        assertEquals("b", list.get(1));
    }

    @Test
    public void testAddAllAtIndex() {
        list.addAll(Arrays.asList("x", "y"));
        List<String> extra = Arrays.asList("a", "b");
        assertTrue(list.addAll(1, extra)); // list becomes x a b y
        assertEquals(Arrays.asList("x", "a", "b", "y"), toArrayList(list));
    }

    @Test
    public void testAddAtIndex() {
        list.add("first");
        list.add(0, "zero");
        assertEquals(Arrays.asList("zero", "first"), toArrayList(list));
    }

    @Test
    public void testSet() {
        list.addAll(Arrays.asList("old", "value"));
        String previous = list.set(1, "new");
        assertEquals("value", previous);
        assertEquals("new", list.get(1));
    }

    @Test
    public void testRemoveByIndex() {
        list.addAll(Arrays.asList("a", "b", "c"));
        String removed = list.remove(1);
        assertEquals("b", removed);
        assertEquals(Arrays.asList("a", "c"), toArrayList(list));
    }

    @Test
    public void testRemoveObject() {
        list.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(list.remove("b"));
        assertFalse(list.contains("b"));
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveAll() {
        list.addAll(Arrays.asList("a", "b", "c", "b"));
        assertTrue(list.removeAll(Arrays.asList("b", "c")));
        assertEquals(Arrays.asList("a"), toArrayList(list));
    }

    @Test
    public void testRetainAll() {
        list.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(list.retainAll(Arrays.asList("b", "c")));
        assertEquals(Arrays.asList("b", "c"), toArrayList(list));
    }

    @Test
    public void testClear() {
        list.addAll(Arrays.asList("x", "y"));
        list.clear();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    public void testContainsAndIndexOf() {
        list.addAll(Arrays.asList("foo", "bar", "baz"));
        assertTrue(list.contains("bar"));
        assertEquals(1, list.indexOf("bar"));
        assertEquals(2, list.lastIndexOf("baz"));
    }

    @Test
    public void testContainsAll() {
        list.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(list.containsAll(Arrays.asList("a", "c")));
        assertFalse(list.containsAll(Arrays.asList("a", "d")));
    }

    @Test
    public void testIteratorSnapshot() {
        list.addAll(Arrays.asList("1", "2", "3"));
        Iterator<String> it = list.iterator(); // snapshot
        list.add("4"); // modify after iterator creation
        assertTrue(it.hasNext());
        assertEquals("1", it.next());
        assertEquals("2", it.next());
        assertEquals("3", it.next());
        assertFalse(it.hasNext()); // iterator does NOT see "4"
    }

    @Test
    public void testListIteratorSnapshot() {
        list.addAll(Arrays.asList("a", "b"));
        ListIterator<String> lit = list.listIterator();
        list.add("c");
        assertTrue(lit.hasNext());
        assertEquals("a", lit.next());
        assertEquals("b", lit.next());
        assertFalse(lit.hasNext()); // snapshot, no "c"
    }

    @Test
    public void testToArray() {
        list.addAll(Arrays.asList("x", "y"));
        Object[] arr = list.toArray();
        assertArrayEquals(new Object[]{"x", "y"}, arr);
    }

    @Test
    public void testToArrayWithTypedArray() {
        list.addAll(Arrays.asList("x", "y"));
        String[] a = new String[2];
        String[] result = list.toArray(a);
        assertSame(a, result);
        assertArrayEquals(new String[]{"x", "y"}, result);
    }

    @Test
    public void testEqualsAndHashCode() {
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b"));
        ConcurrentList<String> other = new ConcurrentList<>(backing);
        assertEquals(list, list); // reflexive
        assertEquals(other, other);
        list.addAll(Arrays.asList("a", "b"));
        assertEquals(other, list);
        assertEquals(other.hashCode(), list.hashCode());
    }

    @Test
    public void testToString() {
        list.addAll(Arrays.asList("one", "two"));
        assertEquals("[one, two]", list.toString());
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  EDGE‑CASE / EXCEPTION TESTS                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConstructorWithNullListThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("List cannot be null");
        new ConcurrentList<String>(null);
    }

    @Test
    public void testGetWithInvalidIndexThrows() {
        thrown.expect(IndexOutOfBoundsException.class);
        list.get(0); // empty list
    }

    @Test
    public void testSetWithInvalidIndexThrows() {
        thrown.expect(IndexOutOfBoundsException.class);
        list.set(5, "oops");
    }

    @Test
    public void testRemoveByIndexInvalidThrows() {
        thrown.expect(IndexOutOfBoundsException.class);
        list.remove(2);
    }

    @Test
    public void testAddAtInvalidIndexThrows() {
        thrown.expect(IndexOutOfBoundsException.class);
        list.add(3, "bad");
    }

    @Test
    public void testAddAllAtInvalidIndexThrows() {
        thrown.expect(IndexOutOfBoundsException.class);
        list.addAll(1, Arrays.asList("x"));
    }

    @Test
    public void testListIteratorWithIndexUnsupported() {
        thrown.expect(UnsupportedOperationException.class);
        list.listIterator(0);
    }

    @Test
    public void testSubListUnsupported() {
        thrown.expect(UnsupportedOperationException.class);
        list.subList(0, 1);
    }

    @Test
    public void testContainsAllWithNullElementInCollection() {
        list.addAll(Arrays.asList("a", null, "c"));
        assertTrue(list.containsAll(Arrays.asList(null)));
    }

    @Test
    public void testToArrayWithSmallerTypedArray() {
        list.addAll(Arrays.asList("x", "y", "z"));
        String[] small = new String[2];
        String[] result = list.toArray(small);
        // JDK spec: when supplied array is too small, a new array of same runtime type is allocated
        assertNotSame(small, result);
        assertArrayEquals(new String[]{"x", "y", "z"}, result);
    }

    @Test
    public void testToArrayWithLargerTypedArray() {
        list.addAll(Arrays.asList("p", "q"));
        String[] large = new String[5];
        String[] result = list.toArray(large);
        assertSame(large, result);
        assertArrayEquals(new String[]{"p", "q", null, null, null}, result);
    }

    /* ---------------------------------------------------------------------- */
    /* Helper methods                                                          */
    /* ---------------------------------------------------------------------- */

    /** Convenience conversion of a {@link ConcurrentList} to a plain {@link List}. */
    private static <E> List<E> toArrayList(ConcurrentList<E> cl) {
        List<E> result = new ArrayList<>();
        for (E e : cl) {
            result.add(e);
        }
        return result;
    }
}
