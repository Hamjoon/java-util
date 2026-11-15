package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ConcurrentNavigableSetNullSafeTest {

    private ConcurrentNavigableSetNullSafe<Integer> naturalSet;
    private ConcurrentNavigableSetNullSafe<Integer> customSet;

    /**
     * Custom comparator that treats {@code null} as the smallest element.
     * For non‑null values it uses natural ordering.
     */
    private static final Comparator<Integer> NULL_FIRST_COMPARATOR = (o1, o2) -> {
        if (o1 == null && o2 == null) return 0;
        if (o1 == null) return -1;   // null is smaller
        if (o2 == null) return 1;
        return Integer.compare(o1, o2);
    };

    @Before
    public void setUp() {
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        customSet = new ConcurrentNavigableSetNullSafe<>(NULL_FIRST_COMPARATOR);
    }

    /* -------------------------------------------------
     *  Basic Set operations and null handling (natural)
     * ------------------------------------------------- */
    @Test
    public void testAddContainsRemoveAndSize() {
        assertTrue(naturalSet.isEmpty());
        assertEquals(0, naturalSet.size());

        // add elements including null
        assertTrue(naturalSet.add(1));
        assertTrue(naturalSet.add(2));
        assertTrue(naturalSet.add(null));
        assertTrue(naturalSet.add(3));

        // duplicate adds must return false
        assertFalse(naturalSet.add(2));
        assertFalse(naturalSet.add(null));

        assertEquals(4, naturalSet.size());
        assertFalse(naturalSet.isEmpty());

        // contains
        assertTrue(naturalSet.contains(1));
        assertTrue(naturalSet.contains(null));
        assertFalse(naturalSet.contains(99));

        // remove
        assertTrue(naturalSet.remove(2));
        assertFalse(naturalSet.contains(2));
        assertEquals(3, naturalSet.size());

        assertTrue(naturalSet.remove(null));
        assertFalse(naturalSet.contains(null));
        assertEquals(2, naturalSet.size());
    }

    @Test
    public void testIterationOrderNatural() {
        naturalSet.addAll(Arrays.asList(3, 1, null, 2));

        // natural ordering: null is considered greater than any non‑null
        List<Integer> expected = Arrays.asList(1, 2, 3, null);
        List<Integer> actual = new ArrayList<>();
        for (Integer i : naturalSet) {
            actual.add(i);
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testFirstLastAndPoll() {
        naturalSet.addAll(Arrays.asList(5, 1, null, 3));

        assertEquals(Integer.valueOf(1), naturalSet.first());
        assertEquals(null, naturalSet.last());

        // pollFirst removes the smallest (1)
        assertEquals(Integer.valueOf(1), naturalSet.pollFirst());
        assertFalse(naturalSet.contains(1));

        // pollLast removes the greatest (null)
        assertEquals(null, naturalSet.pollLast());
        assertFalse(naturalSet.contains(null));

        // remaining elements
        List<Integer> remaining = new ArrayList<>();
        for (Integer i : naturalSet) {
            remaining.add(i);
        }
        assertEquals(Arrays.asList(3, 5), remaining);
    }

    @Test
    public void testNavigationalMethodsNatural() {
        naturalSet.addAll(Arrays.asList(10, 20, 30, null));

        // lower
        assertEquals(Integer.valueOf(20), naturalSet.lower(30));
        assertEquals(Integer.valueOf(30), naturalSet.lower(null)); // null is greatest
        assertNull(naturalSet.lower(10));

        // floor
        assertEquals(Integer.valueOf(20), naturalSet.floor(20));
        assertEquals(null, naturalSet.floor(null));
        assertNull(naturalSet.floor(5));

        // ceiling
        assertEquals(Integer.valueOf(20), naturalSet.ceiling(20));
        assertEquals(null, naturalSet.ceiling(null));
        assertEquals(Integer.valueOf(10), naturalSet.ceiling(5));

        // higher
        assertEquals(Integer.valueOf(30), naturalSet.higher(20));
        assertNull(naturalSet.higher(null));
        assertEquals(Integer.valueOf(10), naturalSet.higher(5));
    }

    /* -------------------------------------------------
     *  Custom comparator (null first) tests
     * ------------------------------------------------- */
    @Test
    public void testComparatorMethod() {
        assertNull(naturalSet.comparator());
        assertEquals(NULL_FIRST_COMPARATOR, customSet.comparator());
    }

    @Test
    public void testIterationOrderCustomComparator() {
        customSet.addAll(Arrays.asList(3, null, 1, 2));

        // With NULL_FIRST_COMPARATOR, null should appear first
        List<Integer> expected = Arrays.asList(null, 1, 2, 3);
        List<Integer> actual = new ArrayList<>();
        for (Integer i : customSet) {
            actual.add(i);
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testNavigationalMethodsCustomComparator() {
        customSet.addAll(Arrays.asList(10, 20, null, 30));

        // Because null is smallest:
        assertNull(customSet.lower(null)); // nothing smaller than null
        assertNull(customSet.floor(null));
        assertEquals(Integer.valueOf(null), customSet.ceiling(null)); // returns null itself
        assertEquals(Integer.valueOf(10), customSet.higher(null));

        // regular numbers
        assertEquals(Integer.valueOf(10), customSet.lower(20));
        assertEquals(Integer.valueOf(20), customSet.floor(20));
        assertEquals(Integer.valueOf(20), customSet.ceiling(20));
        assertEquals(Integer.valueOf(30), customSet.higher(20));
    }

    /* -------------------------------------------------
     *  SubSet / HeadSet / TailSet views
     * ------------------------------------------------- */
    @Test
    public void testSubSetInclusiveExclusiveNatural() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, null));

        // Subset from 2 (inclusive) to null (exclusive) -> should contain 2 and 3
        NavigableSet<Integer> sub = naturalSet.subSet(2, true, null, false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains(2));
        assertTrue(sub.contains(3));
        assertFalse(sub.contains(null));

        // Mutating the subset must affect the original set
        assertTrue(sub.remove(2));
        assertFalse(naturalSet.contains(2));
        assertEquals(3, naturalSet.size());
    }

    @Test
    public void testHeadSetAndTailSetNatural() {
        naturalSet.addAll(Arrays.asList(5, 10, 15, null));

        // headSet(toElement=10, inclusive=false) -> elements < 10
        SortedSet<Integer> head = naturalSet.headSet(10, false);
        assertEquals(1, head.size());
        assertTrue(head.contains(5));

        // tailSet(fromElement=10, inclusive=true) -> elements >= 10
        SortedSet<Integer> tail = naturalSet.tailSet(10, true);
        assertEquals(3, tail.size());
        assertTrue(tail.contains(10));
        assertTrue(tail.contains(15));
        assertTrue(tail.contains(null));

        // Verify modifications via view affect the original
        assertTrue(tail.remove(null));
        assertFalse(naturalSet.contains(null));
    }

    @Test
    public void testDescendingSetAndIteratorNatural() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, null));

        NavigableSet<Integer> descending = naturalSet.descendingSet();
        List<Integer> expected = Arrays.asList(null, 3, 2, 1);
        List<Integer> actual = new ArrayList<>(descending);
        assertEquals(expected, actual);

        // descendingIterator should produce the same order
        Iterator<Integer> it = naturalSet.descendingIterator();
        List<Integer> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertEquals(expected, iterated);
    }

    @Test
    public void testDescendingSetModificationReflectsOriginal() {
        naturalSet.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> descending = naturalSet.descendingSet();

        // remove via descending set
        assertTrue(descending.remove(3));
        assertFalse(naturalSet.contains(3));
        assertEquals(2, naturalSet.size());

        // add via descending set (adds to underlying set)
        assertTrue(descending.add(4));
        assertTrue(naturalSet.contains(4));
        assertEquals(3, naturalSet.size());
    }

    /* -------------------------------------------------
     *  Constructors that accept collections
     * ------------------------------------------------- */
    @Test
    public void testCollectionConstructorWithNulls() {
        List<Integer> list = Arrays.asList(1, null, 2, null, 3);
        ConcurrentNavigableSetNullSafe<Integer> setFromCollection = new ConcurrentNavigableSetNullSafe<>(list);

        // Duplicates and multiple nulls should be collapsed
        assertEquals(4, setFromCollection.size());
        assertTrue(setFromCollection.contains(null));
        assertTrue(setFromCollection.contains(1));
        assertTrue(setFromCollection.contains(2));
        assertTrue(setFromCollection.contains(3));
    }

    @Test
    public void testCollectionConstructorWithComparator() {
        List<Integer> list = Arrays.asList(2, null, 1);
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>(list, NULL_FIRST_COMPARATOR);
        assertEquals(NULL_FIRST_COMPARATOR, set.comparator());

        // Expected order: null,1,2
        List<Integer> expected = Arrays.asList(null, 1, 2);
        List<Integer> actual = new ArrayList<>();
        for (Integer i : set) {
            actual.add(i);
        }
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------
     *  Clear and isEmpty
     * ------------------------------------------------- */
    @Test
    public void testClearAndIsEmpty() {
        naturalSet.addAll(Arrays.asList(1, null, 2));
        assertFalse(naturalSet.isEmpty());
        assertEquals(3, naturalSet.size());

        naturalSet.clear();
        assertTrue(naturalSet.isEmpty());
        assertEquals(0, naturalSet.size());
    }

    /* -------------------------------------------------
     *  Edge cases: operations on empty set
     * ------------------------------------------------- */
    @Test
    public void testOperationsOnEmptySetReturnNullOrFalse() {
        assertNull(naturalSet.first());
        assertNull(naturalSet.last());
        assertNull(naturalSet.lower(10));
        assertNull(naturalSet.floor(10));
        assertNull(naturalSet.ceiling(10));
        assertNull(naturalSet.higher(10));
        assertNull(naturalSet.pollFirst());
        assertNull(naturalSet.pollLast());

        assertFalse(naturalSet.contains(5));
        assertFalse(naturalSet.remove(5));
        assertTrue(naturalSet.isEmpty());
    }
}
