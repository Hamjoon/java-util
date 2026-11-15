package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentNavigableSetNullSafe}.
 *
 * The tests cover:
 *  • All public constructors
 *  • All public methods (including NavigableSet, SortedSet and Set contracts)
 *  • Typical usage, edge‑cases (null handling, duplicate elements, empty set)
 *  • Behaviour with a custom comparator
 *  • Exception scenarios (e.g. null collection argument)
 *
 * No mocking is used – real instances of {@code ConcurrentNavigableSetNullSafe}
 * and its internal {@code ConcurrentSkipListSet} are created directly.
 */
@RunWith(JUnit4.class)
public class ConcurrentNavigableSetNullSafeTest {

    private ConcurrentNavigableSetNullSafe<Integer> set;

    @Before
    public void setUp() {
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @After
    public void tearDown() {
        set.clear();
    }

    /* ----------------------------------------------------------------------
     * Constructor tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructor_EmptySet() {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testConstructorWithComparator_NaturalOrderReversed() {
        Comparator<Integer> reverse = (a, b) -> b.compareTo(a);
        ConcurrentNavigableSetNullSafe<Integer> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        revSet.add(1);
        revSet.add(3);
        revSet.add(2);
        // Expected order: 3,2,1 (null is considered greater than any non‑null)
        Iterator<Integer> it = revSet.iterator();
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testConstructorFromCollection_NonNullAndNullElements() {
        List<Integer> list = Arrays.asList(5, null, 2, 5);
        ConcurrentNavigableSetNullSafe<Integer> fromColl = new ConcurrentNavigableSetNullSafe<>(list);
        // Duplicates are eliminated, null is kept once
        assertEquals(3, fromColl.size());
        assertTrue(fromColl.contains(null));
        assertTrue(fromColl.contains(2));
        assertTrue(fromColl.contains(5));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorFromCollection_NullArgumentThrows() {
        new ConcurrentNavigableSetNullSafe<Integer>((Collection<Integer>) null);
    }

    @Test
    public void testConstructorFromCollectionWithComparator() {
        Comparator<Integer> comp = Comparator.naturalOrder();
        List<Integer> list = Arrays.asList(9, 1, null, 4);
        ConcurrentNavigableSetNullSafe<Integer> setWithComp =
                new ConcurrentNavigableSetNullSafe<>(list, comp);
        assertEquals(4, setWithComp.size());
        assertTrue(setWithComp.contains(null));
        // natural order: 1,4,9,null (null considered greater)
        Iterator<Integer> it = setWithComp.iterator();
        assertEquals(Integer.valueOf(1), it.next());
        assertEquals(Integer.valueOf(4), it.next());
        assertEquals(Integer.valueOf(9), it.next());
        assertEquals(null, it.next());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorFromCollectionWithComparator_NullCollectionThrows() {
        new ConcurrentNavigableSetNullSafe<Integer>((Collection<Integer>) null, null);
    }

    /* ----------------------------------------------------------------------
     * Basic Set operations
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddContainsRemoveAndSize() {
        assertTrue(set.add(10));
        assertTrue(set.add(null));
        assertTrue(set.add(5));
        // duplicate adds return false
        assertFalse(set.add(10));
        assertFalse(set.add(null));

        assertEquals(3, set.size());
        assertTrue(set.contains(10));
        assertTrue(set.contains(null));
        assertTrue(set.contains(5));
        assertFalse(set.contains(99));

        // remove existing element
        assertTrue(set.remove(5));
        assertFalse(set.contains(5));
        assertEquals(2, set.size());

        // remove null
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertEquals(1, set.size());
    }

    @Test
    public void testClearAndIsEmpty() {
        set.add(1);
        set.add(2);
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /* ----------------------------------------------------------------------
     * NavigableSet specific methods
     * ---------------------------------------------------------------------- */

    @Test
    public void testFirstLastWithNull() {
        set.add(20);
        set.add(10);
        set.add(null); // null is considered greater than any non‑null
        assertEquals(Integer.valueOf(10), set.first());
        assertEquals(null, set.last());
    }

    @Test
    public void testLowerFloorCeilingHigher() {
        set.add(1);
        set.add(3);
        set.add(5);
        set.add(null); // highest element

        // lower/floor/ceiling/higher relative to 3
        assertEquals(Integer.valueOf(1), set.lower(3));
        assertEquals(Integer.valueOf(3), set.floor(3));
        assertEquals(Integer.valueOf(3), set.ceiling(3));
        assertEquals(Integer.valueOf(5), set.higher(3));

        // edge cases with null
        assertEquals(Integer.valueOf(5), set.lower(null));
        assertEquals(null, set.floor(null));
        assertEquals(null, set.ceiling(null));
        assertNull(set.higher(null));
    }

    @Test
    public void testPollFirstAndPollLast() {
        set.add(100);
        set.add(50);
        set.add(null);
        // natural order: 50,100,null
        assertEquals(Integer.valueOf(50), set.pollFirst());
        assertEquals(Integer.valueOf(null), set.pollLast());
        // remaining element
        assertEquals(Integer.valueOf(100), set.pollFirst());
        assertTrue(set.isEmpty());
    }

    @Test
    public void testDescendingSetAndIterator() {
        set.add(1);
        set.add(3);
        set.add(2);
        set.add(null);
        NavigableSet<Integer> descending = set.descendingSet();

        // descending order should be: null,3,2,1
        Iterator<Integer> it = descending.iterator();
        assertEquals(null, it.next());
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertFalse(it.hasNext());

        // remove via descending iterator
        it = descending.iterator();
        it.next(); // points to null
        it.remove(); // removes null from original set
        assertFalse(set.contains(null));
        assertEquals(3, set.size());
    }

    @Test
    public void testSubSet_InclusiveExclusive() {
        set.add(1);
        set.add(3);
        set.add(5);
        set.add(7);
        set.add(null); // highest

        // Subset from 3 (inclusive) to null (exclusive)
        NavigableSet<Integer> sub = set.subSet(3, true, null, false);
        // Expected elements: 3,5,7
        List<Integer> expected = Arrays.asList(3, 5, 7);
        List<Integer> actual = new ArrayList<>(sub);
        assertEquals(expected, actual);
    }

    @Test
    public void testHeadSetAndTailSet() {
        set.add(10);
        set.add(20);
        set.add(30);
        set.add(null);

        // headSet(toElement = 20, inclusive = false) => {10}
        SortedSet<Integer> head = set.headSet(20, false);
        assertEquals(1, head.size());
        assertTrue(head.contains(10));
        assertFalse(head.contains(20));

        // tailSet(fromElement = 20, inclusive = true) => {20,30,null}
        SortedSet<Integer> tail = set.tailSet(20, true);
        assertEquals(3, tail.size());
        assertTrue(tail.contains(20));
        assertTrue(tail.contains(30));
        assertTrue(tail.contains(null));
    }

    /* ----------------------------------------------------------------------
     * Iterator behaviour (including remove)
     * ---------------------------------------------------------------------- */

    @Test
    public void testIteratorOrderAndRemove() {
        set.add(2);
        set.add(null);
        set.add(1);
        // natural order: 1,2,null
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(1), it.next());
        it.remove(); // remove 1
        assertFalse(set.contains(1));
        assertEquals(2, set.size());

        assertEquals(Integer.valueOf(2), it.next());
        it.remove(); // remove 2
        assertFalse(set.contains(2));
        assertEquals(1, set.size());

        assertEquals(null, it.next());
        it.remove(); // remove null
        assertFalse(set.contains(null));
        assertTrue(set.isEmpty());
    }

    /* ----------------------------------------------------------------------
     * Edge cases for comparator() and toArray (inherited) – basic sanity
     * ---------------------------------------------------------------------- */

    @Test
    public void testComparatorReturned() {
        // default (natural ordering) returns null
        assertNull(set.comparator());

        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableSetNullSafe<Integer> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        assertEquals(reverse, revSet.comparator());
    }

    @Test
    public void testContainsWithNonMatchingType() {
        set.add(1);
        // contains should return false for unrelated type (e.g., String)
        assertFalse(set.contains("1"));
    }

    @Test
    public void testAddAllViaConstructorDoesNotAllowNullCollection() {
        try {
            new ConcurrentNavigableSetNullSafe<Integer>((Collection<Integer>) null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }
}
