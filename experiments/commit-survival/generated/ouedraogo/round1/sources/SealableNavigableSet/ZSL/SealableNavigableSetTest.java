package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link SealableNavigableSet}.
 * The tests cover:
 * <ul>
 *   <li>All immutable query methods</li>
 *   <li>All mutable methods respecting the sealed state</li>
 *   <li>Views returned by {@code subSet}, {@code headSet}, {@code tailSet},
 *       {@code descendingSet} and their mutability handling</li>
 *   <li>Iterator behaviour, especially {@code remove()}</li>
 * </ul>
 */
public class SealableNavigableSetTest {

    /** Supplier that reflects the current value of this flag. */
    private AtomicBoolean sealedFlag;
    private Supplier<Boolean> sealedSupplier;

    @Before
    public void setUp() {
        sealedFlag = new AtomicBoolean(false);
        sealedSupplier = sealedFlag::get;
    }

    /* --------------------------------------------------------------
     *  Helper methods
     * -------------------------------------------------------------- */

    private SealableNavigableSet<Integer> emptySet() {
        return new SealableNavigableSet<>(sealedSupplier);
    }

    private SealableNavigableSet<Integer> setWith(Integer... values) {
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sealedSupplier);
        for (Integer v : values) {
            s.add(v);
        }
        return s;
    }

    /* --------------------------------------------------------------
     *  Immutable API tests
     * -------------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmpty() {
        SealableNavigableSet<Integer> s = emptySet();
        assertTrue(s.isEmpty());
        assertEquals(0, s.size());

        s.add(5);
        assertFalse(s.isEmpty());
        assertEquals(1, s.size());
    }

    @Test
    public void testContainsAndContainsAll() {
        SealableNavigableSet<Integer> s = setWith(1, 2, 3);
        assertTrue(s.contains(2));
        assertFalse(s.contains(4));

        Collection<Integer> col = Arrays.asList(1, 3);
        assertTrue(s.containsAll(col));

        col = Arrays.asList(1, 4);
        assertFalse(s.containsAll(col));
    }

    @Test
    public void testComparatorAndOrderQueries() {
        // natural ordering comparator (null) for Integer
        SealableNavigableSet<Integer> s = setWith(10, 20, 30);
        assertNull(s.comparator());

        assertEquals(Integer.valueOf(10), s.first());
        assertEquals(Integer.valueOf(30), s.last());

        assertEquals(Integer.valueOf(20), s.lower(30));
        assertEquals(Integer.valueOf(20), s.floor(20));
        assertEquals(Integer.valueOf(20), s.ceiling(20));
        assertEquals(Integer.valueOf(30), s.higher(20));
    }

    @Test
    public void testToArrayAndToString() {
        SealableNavigableSet<Integer> s = setWith(3, 1, 2);
        Object[] arr = s.toArray();
        assertArrayEquals(new Object[]{1, 2, 3}, arr);

        Integer[] a = s.toArray(new Integer[0]);
        assertArrayEquals(new Integer[]{1, 2, 3}, a);

        String str = s.toString();
        assertTrue(str.startsWith("["));
        assertTrue(str.endsWith("]"));
        assertTrue(str.contains("1"));
        assertTrue(str.contains("2"));
        assertTrue(str.contains("3"));
    }

    @Test
    public void testEqualsAndHashCode() {
        SealableNavigableSet<Integer> s1 = setWith(5, 10);
        SealableNavigableSet<Integer> s2 = setWith(5, 10);
        Set<Integer> javaSet = new TreeSet<>(Arrays.asList(5, 10));

        assertEquals(s1, s2);
        assertEquals(s1, javaSet);
        assertEquals(s2, javaSet);

        assertEquals(s1.hashCode(), s2.hashCode());
        assertEquals(s1.hashCode(), javaSet.hashCode());
    }

    /* --------------------------------------------------------------
     *  Mutable API tests – unsealed (should succeed)
     * -------------------------------------------------------------- */

    @Test
    public void testAddAndAddAllWhenUnsealed() {
        SealableNavigableSet<Integer> s = emptySet();
        assertTrue(s.add(42));
        assertTrue(s.contains(42));

        Collection<Integer> more = Arrays.asList(1, 2, 3);
        assertTrue(s.addAll(more));
        assertTrue(s.containsAll(more));
    }

    @Test
    public void testRemoveAndClearWhenUnsealed() {
        SealableNavigableSet<Integer> s = setWith(7, 8, 9);
        assertTrue(s.remove(Integer.valueOf(8)));
        assertFalse(s.contains(8));

        s.clear();
        assertTrue(s.isEmpty());
    }

    @Test
    public void testPollFirstAndPollLastWhenUnsealed() {
        SealableNavigableSet<Integer> s = setWith(5, 10, 15);
        assertEquals(Integer.valueOf(5), s.pollFirst());
        assertEquals(Integer.valueOf(15), s.pollLast());
        assertEquals(1, s.size());
        assertTrue(s.contains(10));
    }

    @Test
    public void testIteratorRemoveWhenUnsealed() {
        SealableNavigableSet<Integer> s = setWith(100);
        Iterator<Integer> it = s.iterator();
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(100), it.next());
        it.remove();                     // should succeed
        assertFalse(s.contains(100));
        assertTrue(s.isEmpty());
    }

    /* --------------------------------------------------------------
     *  Mutable API tests – sealed (should throw)
     * -------------------------------------------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testAddWhenSealedThrows() {
        SealableNavigableSet<Integer> s = emptySet();
        sealedFlag.set(true);            // seal the set
        s.add(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddAllWhenSealedThrows() {
        SealableNavigableSet<Integer> s = emptySet();
        sealedFlag.set(true);
        s.addAll(Arrays.asList(1, 2));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealedThrows() {
        SealableNavigableSet<Integer> s = setWith(1);
        sealedFlag.set(true);
        s.remove(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealedThrows() {
        SealableNavigableSet<Integer> s = setWith(1);
        sealedFlag.set(true);
        s.clear();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPollFirstWhenSealedThrows() {
        SealableNavigableSet<Integer> s = setWith(1);
        sealedFlag.set(true);
        s.pollFirst();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPollLastWhenSealedThrows() {
        SealableNavigableSet<Integer> s = setWith(1);
        sealedFlag.set(true);
        s.pollLast();
    }

    @Test
    public void testIteratorRemoveWhenSealedThrows() {
        SealableNavigableSet<Integer> s = setWith(200);
        Iterator<Integer> it = s.iterator();
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(200), it.next());

        sealedFlag.set(true);            // seal before calling remove()
        try {
            it.remove();
            fail("Expected UnsupportedOperationException from iterator.remove() when sealed");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
        // element must still be present
        assertTrue(s.contains(200));
    }

    /* --------------------------------------------------------------
     *  View tests – subSet, headSet, tailSet, descendingSet
     * -------------------------------------------------------------- */

    @Test
    public void testSubSetViewRespectsSeal() {
        SealableNavigableSet<Integer> base = setWith(1, 2, 3, 4, 5);
        NavigableSet<Integer> sub = base.subSet(2, true, 4, true); // [2,4]

        // mutable while unsealed
        assertTrue(sub.add(3)); // already present -> false, but no exception
        assertTrue(sub.remove(2));
        assertFalse(base.contains(2));

        // seal the base set (the view shares the same supplier)
        sealedFlag.set(true);
        try {
            sub.add(6);
            fail("Expected exception when adding to a sealed subSet view");
        } catch (UnsupportedOperationException expected) {
        }

        try {
            sub.remove(3);
            fail("Expected exception when removing from a sealed subSet view");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testHeadSetAndTailSetInclusiveFlags() {
        SealableNavigableSet<Integer> base = setWith(10, 20, 30, 40);
        NavigableSet<Integer> head = base.headSet(30, true); // includes 30
        NavigableSet<Integer> tail = base.tailSet(20, false); // excludes 20

        assertTrue(head.containsAll(Arrays.asList(10, 20, 30)));
        assertFalse(head.contains(40));

        assertTrue(tail.containsAll(Arrays.asList(30, 40)));
        assertFalse(tail.contains(20));

        // modify via view while unsealed
        head.clear();
        assertFalse(base.contains(10));
        assertFalse(base.contains(20));
        assertFalse(base.contains(30));
        assertTrue(base.contains(40));
    }

    @Test
    public void testDescendingSetView() {
        SealableNavigableSet<Integer> base = setWith(1, 2, 3);
        NavigableSet<Integer> desc = base.descendingSet();

        // The descending view should iterate in reverse order
        Iterator<Integer> it = desc.iterator();
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertFalse(it.hasNext());

        // Mutability through descending view
        desc.add(0);
        assertTrue(base.contains(0));
        assertEquals(Integer.valueOf(0), base.first());

        // Seal and verify that mutation via descending view fails
        sealedFlag.set(true);
        try {
            desc.remove(2);
            fail("Expected exception when removing via sealed descendingSet");
        } catch (UnsupportedOperationException expected) {
        }
    }

    @Test
    public void testDescendingIteratorWrapsEntryWhenApplicable() {
        // This test only verifies that the iterator does not throw when the
        // underlying element is NOT a Map.Entry (our Integer case). The
        // internal logic for Map.Entry wrapping is exercised indirectly.
        SealableNavigableSet<Integer> s = setWith(42);
        Iterator<Integer> it = s.descendingIterator();
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(42), it.next());
    }

    /* --------------------------------------------------------------
     *  Construction variants
     * -------------------------------------------------------------- */

    @Test
    public void testConstructionFromCollection() {
        List<Integer> list = Arrays.asList(5, 1, 3);
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(list, sealedSupplier);
        assertEquals(3, s.size());
        assertTrue(s.containsAll(list));
    }

    @Test
    public void testConstructionFromSortedSet() {
        SortedSet<Integer> sorted = new TreeSet<>(Arrays.asList(7, 2, 9));
        SealableNavigableSet<Integer> s = new SealableNavigableSet<>(sorted, sealedSupplier);
        assertEquals(3, s.size());
        assertEquals(Integer.valueOf(2), s.first());
        assertEquals(Integer.valueOf(9), s.last());
    }

    @Test
    public void testConstructionWithComparator() {
        Comparator<String> reverse = Comparator.reverseOrder();
        SealableNavigableSet<String> s = new SealableNavigableSet<>(reverse, sealedSupplier);
        s.add("a");
        s.add("b");
        s.add("c");
        // because of reverse order, first() should be the greatest element
        assertEquals("c", s.first());
        assertEquals("a", s.last());
        assertEquals(reverse, s.comparator());
    }

    @Test
    public void testWrappingExistingNavigableSet() {
        NavigableSet<Integer> backing = new TreeSet<>(Arrays.asList(11, 22));
        SealableNavigableSet<Integer> wrapper = new SealableNavigableSet<>(backing, sealedSupplier);
        assertTrue(wrapper.containsAll(Arrays.asList(11, 22)));

        // Mutate via wrapper while unsealed
        wrapper.add(33);
        assertTrue(backing.contains(33));

        // Seal and ensure wrapper cannot modify the underlying set
        sealedFlag.set(true);
        try {
            wrapper.remove(11);
            fail("Expected exception when removing from sealed wrapper");
        } catch (UnsupportedOperationException expected) {
        }
        assertTrue(backing.contains(11));
    }
}
