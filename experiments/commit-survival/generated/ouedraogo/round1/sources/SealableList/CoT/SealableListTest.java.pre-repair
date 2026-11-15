package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class SealableListTest {

    /* ----------------------------------------------------------------------
     *  Helper to create a mutable/immutable supplier that can be toggled.
     * ---------------------------------------------------------------------- */
    private static class ToggleSupplier implements Supplier<Boolean> {
        private final AtomicBoolean sealed = new AtomicBoolean(false);
        @Override public Boolean get() { return sealed.get(); }
        void setSealed(boolean b) { sealed.set(b); }
    }

    private ToggleSupplier supplier;
    private SealableList<String> list;

    @Before
    public void setUp() {
        supplier = new ToggleSupplier();               // starts un‑sealed (mutable)
        list = new SealableList<>(supplier);
    }

    /* ----------------------------------------------------------------------
     *  1.  Public methods (signatures) extracted from SealableList
     * ----------------------------------------------------------------------
     *
     * Constructors
     *   SealableList(Supplier<Boolean> sealedSupplier)
     *   SealableList(Collection<T> col, Supplier<Boolean> sealedSupplier)
     *   SealableList(List<T> list, Supplier<Boolean> sealedSupplier)
     *
     * Immutable (read‑only) APIs
     *   boolean equals(Object other)
     *   int hashCode()
     *   String toString()
     *   int size()
     *   boolean isEmpty()
     *   boolean contains(Object o)
     *   boolean containsAll(Collection<?> col)
     *   int indexOf(Object o)
     *   int lastIndexOf(Object o)
     *   T get(int index)
     *   Object[] toArray()
     *   <T1> T1[] toArray(T1[] a)
     *   Iterator<T> iterator()
     *   ListIterator<T> listIterator()
     *   ListIterator<T> listIterator(int index)
     *   List<T> subList(int fromIndex, int toIndex)
     *
     * Mutable APIs (throw UnsupportedOperationException when sealed)
     *   boolean add(T t)
     *   boolean remove(Object o)
     *   boolean addAll(Collection<? extends T> col)
     *   boolean addAll(int index, Collection<? extends T> col)
     *   boolean removeAll(Collection<?> col)
     *   boolean retainAll(Collection<?> col)
     *   void clear()
     *   T set(int index, T element)
     *   void add(int index, T element)
     *   T remove(int index)
     *
     * ---------------------------------------------------------------------- */

    /* ----------------------------------------------------------------------
     *  2.  Basic functionality tests for each public method
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructorWithCollection() {
        List<String> src = Arrays.asList("a", "b", "c");
        SealableList<String> l = new SealableList<>(src, supplier);
        assertEquals(3, l.size());
        assertTrue(l.containsAll(src));
    }

    @Test
    public void testConstructorWithListWrapper() {
        List<String> backing = new ArrayList<>(Arrays.asList("x", "y"));
        SealableList<String> wrapper = new SealableList<>(backing, supplier);
        assertEquals(2, wrapper.size());
        wrapper.add("z");                     // mutable, should affect backing list
        assertEquals(3, backing.size());
        assertTrue(backing.contains("z"));
    }

    @Test
    public void testAddWhenMutable() {
        assertTrue(list.add("one"));
        assertEquals(1, list.size());
        assertEquals("one", list.get(0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddWhenSealedThrows() {
        supplier.setSealed(true);
        list.add("shouldFail");
    }

    @Test
    public void testRemoveWhenMutable() {
        list.add("a");
        list.add("b");
        assertTrue(list.remove("a"));
        assertEquals(1, list.size());
        assertFalse(list.contains("a"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealedThrows() {
        list.add("x");
        supplier.setSealed(true);
        list.remove("x");
    }

    @Test
    public void testAddAllMutable() {
        Collection<String> col = Arrays.asList("one", "two");
        assertTrue(list.addAll(col));
        assertEquals(2, list.size());
        assertTrue(list.containsAll(col));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllSealedThrows() {
        supplier.setSealed(true);
        list.addAll(Arrays.asList("a"));
    }

    @Test
    public void testClearMutable() {
        list.add("a");
        list.add("b");
        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearSealedThrows() {
        list.add("a");
        supplier.setSealed(true);
        list.clear();
    }

    @Test
    public void testSetMutable() {
        list.add("old");
        String prev = list.set(0, "new");
        assertEquals("old", prev);
        assertEquals("new", list.get(0));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSetSealedThrows() {
        list.add("x");
        supplier.setSealed(true);
        list.set(0, "y");
    }

    @Test
    public void testIteratorRemoveWhenMutable() {
        list.addAll(Arrays.asList("a", "b", "c"));
        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String v = it.next();
            if ("b".equals(v)) {
                it.remove();
            }
        }
        assertFalse(list.contains("b"));
        assertEquals(2, list.size());
    }

    @Test
    public void testIteratorRemoveWhenSealedThrows() {
        list.addAll(Arrays.asList("a", "b"));
        supplier.setSealed(true);
        Iterator<String> it = list.iterator();
        it.next(); // move to first element
        try {
            it.remove();
            fail("Expected UnsupportedOperationException from iterator.remove() when sealed");
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    @Test
    public void testListIteratorSetAddRemoveWhenMutable() {
        list.addAll(Arrays.asList("a", "b"));
        ListIterator<String> li = list.listIterator();
        li.next();                 // points at "a"
        li.set("alpha");           // replace
        li.add("beta");            // insert after "alpha"
        li.previous();            // move back to "beta"
        li.remove();              // remove "beta"
        assertEquals(Arrays.asList("alpha", "b"), list);
    }

    @Test
    public void testListIteratorOperationsWhenSealedThrow() {
        list.addAll(Arrays.asList("1", "2"));
        supplier.setSealed(true);
        ListIterator<String> li = list.listIterator();
        li.next(); // now at "1"
        try {
            li.set("X");
            fail("Expected UnsupportedOperationException from listIterator.set() when sealed");
        } catch (UnsupportedOperationException ignored) { }
        try {
            li.add("Y");
            fail("Expected UnsupportedOperationException from listIterator.add() when sealed");
        } catch (UnsupportedOperationException ignored) { }
        try {
            li.remove();
            fail("Expected UnsupportedOperationException from listIterator.remove() when sealed");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testSubListMutableBehavior() {
        list.addAll(Arrays.asList("a", "b", "c", "d"));
        List<String> sub = list.subList(1, 3); // should contain ["b","c"]
        assertEquals(2, sub.size());
        sub.set(0, "beta");
        sub.add("gamma"); // adds at end of sublist (index 2)
        assertEquals(Arrays.asList("a", "beta", "c", "gamma", "d"), list);
    }

    @Test
    public void testSubListSealedThrowsOnModification() {
        list.addAll(Arrays.asList("x", "y", "z"));
        supplier.setSealed(true);
        List<String> sub = list.subList(0, 2);
        try {
            sub.add("new");
            fail("Expected UnsupportedOperationException from subList.add() when sealed");
        } catch (UnsupportedOperationException ignored) { }
        try {
            sub.set(0, "changed");
            fail("Expected UnsupportedOperationException from subList.set() when sealed");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testContainsAllEdgeCases() {
        list.addAll(Arrays.asList("a", "b"));
        Collection<String> superset = Arrays.asList("a", "b", "c");
        assertFalse(list.containsAll(superset)); // missing "c"
        Collection<String> empty = Collections.emptyList();
        assertTrue(list.containsAll(empty));    // vacuously true
    }

    @Test
    public void testEqualsAndHashCode() {
        SealableList<String> l1 = new SealableList<>(supplier);
        SealableList<String> l2 = new SealableList<>(supplier);
        l1.addAll(Arrays.asList("x", "y"));
        l2.addAll(Arrays.asList("x", "y"));
        assertEquals(l1, l2);
        assertEquals(l1.hashCode(), l2.hashCode());

        l2.add("z");
        assertNotEquals(l1, l2);
    }

    @Test
    public void testToArrayVariants() {
        list.addAll(Arrays.asList("a", "b"));
        Object[] arr = list.toArray();
        assertArrayEquals(new Object[]{"a", "b"}, arr);

        String[] strArr = list.toArray(new String[0]);
        assertArrayEquals(new String[]{"a", "b"}, strArr);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetInvalidIndexThrows() {
        list.add("only");
        list.get(5); // out of bounds
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testAddAtInvalidIndexThrows() {
        list.add(10, "oops");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetInvalidIndexThrows() {
        list.add("first");
        list.set(2, "nope");
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testRemoveInvalidIndexThrows() {
        list.add("one");
        list.remove(3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubListInvalidRangeThrows() {
        list.addAll(Arrays.asList("a", "b"));
        list.subList(2, 1); // fromIndex > toIndex
    }
}
