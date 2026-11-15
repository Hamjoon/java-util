package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;

import static org.junit.Assert.*;

public class ConcurrentNavigableSetNullSafeTest {

    private ConcurrentNavigableSetNullSafe<Integer> naturalSet;
    private ConcurrentNavigableSetNullSafe<Integer> reverseSet;
    private ConcurrentNavigableSetNullSafe<String> stringSet;

    @Before
    public void setUp() {
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        reverseSet = new ConcurrentNavigableSetNullSafe<>(Comparator.<Integer>reverseOrder());
        stringSet = new ConcurrentNavigableSetNullSafe<>();
    }

    /* -------------------- basic Set operations -------------------- */

    @Test
    public void testAddContainsRemoveAndSize() {
        assertTrue(naturalSet.isEmpty());
        assertTrue(naturalSet.add(5));
        assertTrue(naturalSet.contains(5));
        assertEquals(1, naturalSet.size());

        // duplicate add should return false
        assertFalse(naturalSet.add(5));
        assertEquals(1, naturalSet.size());

        assertTrue(naturalSet.remove(5));
        assertFalse(naturalSet.contains(5));
        assertTrue(naturalSet.isEmpty());
    }

    @Test
    public void testAddNullAndContainsNull() {
        assertTrue(naturalSet.add(null));
        assertTrue(naturalSet.contains(null));
        assertEquals(1, naturalSet.size());

        // null is considered greater than any non‑null in natural order
        naturalSet.add(1);
        assertEquals(Integer.valueOf(1), naturalSet.first());
        assertNull(naturalSet.last());

        assertTrue(naturalSet.remove(null));
        assertFalse(naturalSet.contains(null));
    }

    @Test
    public void testClear() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, null));
        assertFalse(naturalSet.isEmpty());
        naturalSet.clear();
        assertTrue(naturalSet.isEmpty());
    }

    /* -------------------- ordering & navigation -------------------- */

    @Test
    public void testNaturalOrdering() {
        naturalSet.addAll(Arrays.asList(3, 1, 2));
        assertEquals(Integer.valueOf(1), naturalSet.first());
        assertEquals(Integer.valueOf(3), naturalSet.last());
        assertEquals(Integer.valueOf(2), naturalSet.lower(3));
        assertEquals(Integer.valueOf(3), naturalSet.higher(2));
        assertEquals(Integer.valueOf(2), naturalSet.floor(2));
        assertEquals(Integer.valueOf(2), naturalSet.ceiling(2));
    }

    @Test
    public void testReverseOrderingWithComparator() {
        reverseSet.addAll(Arrays.asList(1, 2, 3));
        // reverse order: 3,2,1
        assertEquals(Integer.valueOf(3), reverseSet.first());
        assertEquals(Integer.valueOf(1), reverseSet.last());
        assertEquals(Integer.valueOf(3), reverseSet.lower(2)); // element less than 2 in reverse order is 3
        assertEquals(Integer.valueOf(1), reverseSet.higher(2)); // element greater than 2 in reverse order is 1
    }

    @Test
    public void testNavigationWithNulls() {
        naturalSet.addAll(Arrays.asList(null, 10, 20));
        // null is greatest
        assertNull(naturalSet.higher(20));
        assertEquals(Integer.valueOf(20), naturalSet.lower(null));
        assertNull(naturalSet.ceiling(null));
        assertEquals(Integer.valueOf(20), naturalSet.floor(null));
    }

    @Test
    public void testPollFirstAndLast() {
        naturalSet.addAll(Arrays.asList(null, 5, 2));
        assertEquals(Integer.valueOf(2), naturalSet.pollFirst());
        assertNull(naturalSet.pollLast()); // null is last
        assertEquals(1, naturalSet.size());
        assertEquals(Integer.valueOf(5), naturalSet.first());
    }

    /* -------------------- iterator behavior -------------------- */

    @Test
    public void testIteratorOrderAndRemove() {
        naturalSet.addAll(Arrays.asList(3, 1, 2, null));
        Iterator<Integer> it = naturalSet.iterator();
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(1), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        it.remove(); // remove 2
        assertFalse(naturalSet.contains(2));
        assertEquals(Integer.valueOf(3), it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testDescendingIterator() {
        naturalSet.addAll(Arrays.asList(1, 2, 3));
        Iterator<Integer> dit = naturalSet.descendingIterator();
        assertEquals(Integer.valueOf(3), dit.next());
        assertEquals(Integer.valueOf(2), dit.next());
        assertEquals(Integer.valueOf(1), dit.next());
        assertFalse(dit.hasNext());
    }

    /* -------------------- subset, headset, tailset -------------------- */

    @Test
    public void testSubSetInclusiveExclusive() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        NavigableSet<Integer> sub = naturalSet.subSet(2, true, 4, false); // {2,3}
        assertEquals(2, sub.size());
        assertTrue(sub.contains(2));
        assertTrue(sub.contains(3));
        assertFalse(sub.contains(4));
    }

    @Test
    public void testHeadSetInclusive() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, 4));
        NavigableSet<Integer> head = naturalSet.headSet(3, true); // {1,2,3}
        assertEquals(3, head.size());
        assertTrue(head.contains(3));
        assertFalse(head.contains(4));
    }

    @Test
    public void testTailSetInclusive() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, 4));
        NavigableSet<Integer> tail = naturalSet.tailSet(2, true); // {2,3,4}
        assertEquals(3, tail.size());
        assertTrue(tail.contains(2));
        assertFalse(tail.contains(1));
    }

    @Test
    public void testSubSetWithNullElements() {
        naturalSet.addAll(Arrays.asList(null, 1, 2, 3));
        // null is greatest, so a subset up to null (exclusive) should contain 1,2,3
        NavigableSet<Integer> sub = naturalSet.subSet(1, true, null, false);
        assertEquals(3, sub.size());
        assertTrue(sub.containsAll(Arrays.asList(1, 2, 3)));
        assertFalse(sub.contains(null));
    }

    /* -------------------- descendingSet view -------------------- */

    @Test
    public void testDescendingSetView() {
        naturalSet.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> desc = naturalSet.descendingSet();
        assertEquals(Integer.valueOf(3), desc.first());
        assertEquals(Integer.valueOf(1), desc.last());

        // modifications reflect in original set
        desc.add(4);
        assertTrue(naturalSet.contains(4));
        assertEquals(Integer.valueOf(4), naturalSet.last());
    }

    /* -------------------- comparator exposure -------------------- */

    @Test
    public void testComparatorReturned() {
        assertNull(naturalSet.comparator()); // natural ordering
        assertEquals(Comparator.<Integer>reverseOrder(), reverseSet.comparator());
    }

    /* -------------------- constructor from collection -------------------- */

    @Test
    public void testConstructorFromCollection() {
        ConcurrentNavigableSetNullSafe<Integer> fromColl =
                new ConcurrentNavigableSetNullSafe<>(Arrays.asList(5, null, 3));
        assertEquals(3, fromColl.size());
        assertTrue(fromColl.contains(5));
        assertTrue(fromColl.contains(3));
        assertTrue(fromColl.contains(null));
        assertEquals(Integer.valueOf(3), fromColl.first());
        assertNull(fromColl.last()); // null is greatest
    }

    @Test
    public void testConstructorFromCollectionWithComparator() {
        Comparator<String> lengthComp = Comparator.comparingInt(String::length);
        ConcurrentNavigableSetNullSafe<String> collWithComp =
                new ConcurrentNavigableSetNullSafe<>(Arrays.asList("aa", "b", null, "ccc"), lengthComp);
        // ordering by length, null considered greater than any non‑null
        assertEquals("b", collWithComp.first()); // length 1
        assertEquals("ccc", collWithComp.last()); // length 3 (null is after it)
        assertTrue(collWithComp.contains(null));
    }

    /* -------------------- ensure internal sentinel never leaks -------------------- */

    @Test
    public void testSentinelIsNotExposed() {
        naturalSet.add(null);
        // The sentinel string is internal; it should never appear via toString of set elements
        for (Object o : naturalSet) {
            assertFalse(o instanceof String && ((String) o).startsWith("null_"));
        }
        assertTrue(naturalSet.contains(null));
    }
}
