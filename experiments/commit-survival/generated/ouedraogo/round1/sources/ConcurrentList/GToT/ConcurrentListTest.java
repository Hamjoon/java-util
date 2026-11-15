package com.cedarsoftware.util;

import org.junit.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentList}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors</li>
 *   <li>All immutable and mutable List APIs</li>
 *   <li>Unsupported operations</li>
 *   <li>Edge‑case handling (null arguments, out‑of‑bounds indices, etc.)</li>
 *   <li>Snapshot semantics of iterators</li>
 *   <li>Equals/ hashCode contract</li>
 * </ul>
 *
 * The suite deliberately avoids any mocking; it uses only the public API defined in the
 * source code supplied.
 */
public class ConcurrentListTest {

    /* ---------------------------------------------------------------------- */
    /*  Constructor tests                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNoArgConstructorCreatesEmptyList() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        assertTrue("List should be empty after no‑arg construction", cl.isEmpty());
        assertEquals(0, cl.size());
    }

    @Test
    public void testWrappingConstructorUsesProvidedList() {
        List<Integer> backing = new ArrayList<>(Arrays.asList(1, 2, 3));
        ConcurrentList<Integer> cl = new ConcurrentList<>(backing);
        assertEquals(3, cl.size());
        assertEquals(Integer.valueOf(2), cl.get(1));
        // Mutating the original list must be reflected in the ConcurrentList
        backing.add(4);
        assertEquals(4, cl.size());
        assertEquals(Integer.valueOf(4), cl.get(3));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrappingConstructorRejectsNull() {
        new ConcurrentList<>(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Immutable (read‑only) API tests                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        List<String> list1 = new ArrayList<>(Arrays.asList("a", "b"));
        List<String> list2 = new ArrayList<>(Arrays.asList("a", "b"));
        ConcurrentList<String> cl = new ConcurrentList<>(list1);

        // Equality with another List having same elements
        assertTrue(cl.equals(list2));
        assertEquals(list2.hashCode(), cl.hashCode());

        // Inequality after modification
        list2.add("c");
        assertFalse(cl.equals(list2));
    }

    @Test
    public void testToString() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList(5, 6, 7)));
        assertEquals("[5, 6, 7]", cl.toString());
    }

    @Test
    public void testSizeAndIsEmpty() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        assertTrue(cl.isEmpty());
        cl.add("x");
        assertFalse(cl.isEmpty());
        assertEquals(1, cl.size());
    }

    @Test
    public void testContainsAndContainsAll() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c")));
        assertTrue(cl.contains("b"));
        assertFalse(cl.contains("z"));

        Collection<String> sub = Arrays.asList("a", "c");
        assertTrue(cl.containsAll(sub));

        Collection<String> sup = Arrays.asList("a", "c", "z");
        assertFalse(cl.containsAll(sup));
    }

    @Test(expected = NullPointerException.class)
    public void testContainsAllNullCollectionThrows() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.containsAll(null);
    }

    @Test
    public void testGetIndexOfLastIndexOf() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("x", "y", "x", "z")));
        assertEquals(0, cl.indexOf("x"));
        assertEquals(2, cl.lastIndexOf("x"));
        assertEquals(-1, cl.indexOf("notThere"));
    }

    @Test
    public void testIteratorSnapshot() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList(1, 2, 3)));
        Iterator<Integer> it = cl.iterator();

        // Mutate the underlying list after iterator creation
        cl.add(4);
        cl.remove(Integer.valueOf(2));

        // Iterator should reflect the state at the moment of creation (i.e., 1,2,3)
        List<Integer> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertEquals(Arrays.asList(1, 2, 3), iterated);
        // Current list reflects modifications
        assertEquals(Arrays.asList(1, 3, 4), cl);
    }

    @Test
    public void testListIteratorSnapshot() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c")));
        ListIterator<String> li = cl.listIterator();

        // Change list after iterator acquisition
        cl.clear();
        cl.add("x");

        // Iterator should still iterate over original snapshot
        List<String> snapshot = new ArrayList<>();
        while (li.hasNext()) {
            snapshot.add(li.next());
        }
        assertEquals(Arrays.asList("a", "b", "c"), snapshot);
        assertEquals(1, cl.size());
        assertEquals("x", cl.get(0));
    }

    @Test
    public void testToArray() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList(10, 20, 30)));
        Object[] arr = cl.toArray();
        assertArrayEquals(new Object[]{10, 20, 30}, arr);
    }

    @Test
    public void testToArrayWithTypedArray() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b")));
        String[] a = new String[1]; // smaller than list size – should be replaced
        String[] result = cl.toArray(a);
        assertArrayEquals(new String[]{"a", "b"}, result);
        assertEquals(2, result.length);
    }

    /* ---------------------------------------------------------------------- */
    /*  Mutable API tests                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndAddAll() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        assertTrue(cl.add("first"));
        assertEquals(1, cl.size());

        Collection<String> more = Arrays.asList("second", "third");
        assertTrue(cl.addAll(more));
        assertEquals(3, cl.size());
        assertEquals(Arrays.asList("first", "second", "third"), cl);
    }

    @Test
    public void testAddAllAtIndex() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList(1, 4)));
        Collection<Integer> coll = Arrays.asList(2, 3);
        assertTrue(cl.addAll(1, coll));
        assertEquals(Arrays.asList(1, 2, 3, 4), cl);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAllAtInvalidIndexThrows() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList(1, 2)));
        cl.addAll(5, Arrays.asList(3));
    }

    @Test
    public void testAddAtIndex() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "c")));
        cl.add(1, "b");
        assertEquals(Arrays.asList("a", "b", "c"), cl);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAtInvalidIndexThrows() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.add(2, "oops");
    }

    @Test
    public void testSet() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("old", "keep")));
        String previous = cl.set(0, "new");
        assertEquals("old", previous);
        assertEquals("new", cl.get(0));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetInvalidIndexThrows() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.set(0, "nothing");
    }

    @Test
    public void testRemoveByIndexAndObject() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList(10, 20, 30)));
        Integer removed = cl.remove(1);
        assertEquals(Integer.valueOf(20), removed);
        assertEquals(Arrays.asList(10, 30), cl);

        assertTrue(cl.remove(Integer.valueOf(10)));
        assertFalse(cl.contains(10));
        assertEquals(1, cl.size());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveByInvalidIndexThrows() {
        ConcurrentList<Integer> cl = new ConcurrentList<>();
        cl.remove(0);
    }

    @Test
    public void testRemoveAllAndRetainAll() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c", "d")));
        assertTrue(cl.removeAll(Arrays.asList("b", "c", "x"))); // 'x' not present
        assertEquals(Arrays.asList("a", "d"), cl);

        assertTrue(cl.retainAll(Collections.singleton("a")));
        assertEquals(Collections.singletonList("a"), cl);
    }

    @Test
    public void testClear() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("x", "y")));
        cl.clear();
        assertTrue(cl.isEmpty());
        assertEquals(0, cl.size());
    }

    @Test
    public void testAddNullElement() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.add(null);
        assertTrue(cl.contains(null));
        assertEquals(1, cl.size());
    }

    @Test(expected = NullPointerException.class)
    public void testAddAllNullCollectionThrows() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.addAll(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Unsupported operation tests                                           */
    /* ---------------------------------------------------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testListIteratorWithIndexUnsupported() {
        ConcurrentList<Integer> cl = new ConcurrentList<>();
        cl.listIterator(0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSubListUnsupported() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList(1, 2, 3)));
        cl.subList(0, 2);
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper: expose underlying list for assertions (via toArray)          */
    /* ---------------------------------------------------------------------- */

    /**
     * Convenience method to compare a {@link ConcurrentList} with a plain {@link List}
     * using {@link Object#equals(Object)} on the array representation.
     */
    private static <T> boolean listEquals(ConcurrentList<T> cl, List<T> expected) {
        return Arrays.equals(cl.toArray(), expected.toArray());
    }

    @Test
    public void testListEqualsHelper() {
        ConcurrentList<String> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList("one", "two")));
        assertTrue(listEquals(cl, Arrays.asList("one", "two")));
    }
}
