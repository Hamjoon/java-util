package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ConcurrentNavigableSetNullSafeTest {

    private ConcurrentNavigableSetNullSafe<Integer> naturalSet;
    private ConcurrentNavigableSetNullSafe<String> customSet;
    private Comparator<String> reverseComparator = Comparator.reverseOrder();

    @Before
    public void setUp() {
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        customSet = new ConcurrentNavigableSetNullSafe<>(reverseComparator);
    }

    /* -----------------------------------------------------------------------
     *  Basic functionality tests – one test per public method
     * ----------------------------------------------------------------------- */

    @Test
    public void testComparator() {
        assertNull(naturalSet.comparator());
        assertEquals(reverseComparator, customSet.comparator());
    }

    @Test
    public void testAddAndContains() {
        assertTrue(naturalSet.add(1));
        assertTrue(naturalSet.contains(1));
        assertFalse(naturalSet.add(1)); // duplicate
        assertTrue(customSet.add("a"));
        assertTrue(customSet.contains("a"));
    }

    @Test
    public void testAddNullAndContainsNull() {
        assertTrue(naturalSet.add(null));
        assertTrue(naturalSet.contains(null));
        assertFalse(naturalSet.add(null)); // duplicate null
    }

    @Test
    public void testRemove() {
        naturalSet.add(2);
        assertTrue(naturalSet.remove(2));
        assertFalse(naturalSet.contains(2));
    }

    @Test
    public void testRemoveNull() {
        naturalSet.add(null);
        assertTrue(naturalSet.remove(null));
        assertFalse(naturalSet.contains(null));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(naturalSet.isEmpty());
        assertEquals(0, naturalSet.size());

        naturalSet.add(10);
        naturalSet.add(null);
        assertFalse(naturalSet.isEmpty());
        assertEquals(2, naturalSet.size());
    }

    @Test
    public void testClear() {
        naturalSet.add(5);
        naturalSet.add(null);
        naturalSet.clear();
        assertTrue(naturalSet.isEmpty());
        assertEquals(0, naturalSet.size());
    }

    @Test
    public void testFirstAndLast() {
        naturalSet.add(3);
        naturalSet.add(1);
        naturalSet.add(2);
        assertEquals(Integer.valueOf(1), naturalSet.first());
        assertEquals(Integer.valueOf(3), naturalSet.last());

        // null is considered greater than any non‑null element
        naturalSet.add(null);
        assertEquals(Integer.valueOf(1), naturalSet.first());
        assertNull(naturalSet.last());
    }

    @Test
    public void testPollFirstAndPollLast() {
        naturalSet.add(10);
        naturalSet.add(20);
        naturalSet.add(null);

        assertEquals(Integer.valueOf(10), naturalSet.pollFirst());
        assertEquals(Integer.valueOf(null), naturalSet.pollLast()); // null is greatest
        assertEquals(Integer.valueOf(20), naturalSet.pollFirst());
        assertTrue(naturalSet.isEmpty());
    }

    @Test
    public void testLowerFloorCeilingHigher() {
        naturalSet.add(5);
        naturalSet.add(10);
        naturalSet.add(15);
        naturalSet.add(null); // greatest

        assertEquals(Integer.valueOf(5), naturalSet.lower(10));
        assertEquals(Integer.valueOf(10), naturalSet.floor(10));
        assertEquals(Integer.valueOf(10), naturalSet.ceiling(10));
        assertEquals(Integer.valueOf(15), naturalSet.higher(10));

        // null handling
        assertEquals(Integer.valueOf(15), naturalSet.lower(null));
        assertEquals(Integer.valueOf(null), naturalSet.floor(null));
        assertEquals(Integer.valueOf(null), naturalSet.ceiling(null));
        assertNull(naturalSet.higher(null));
    }

    @Test
    public void testIteratorOrder() {
        naturalSet.add(3);
        naturalSet.add(1);
        naturalSet.add(2);
        naturalSet.add(null);

        Iterator<Integer> it = naturalSet.iterator();
        List<Integer> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }
        // natural order, null last
        assertEquals(Arrays.asList(1, 2, 3, null), result);
    }

    @Test
    public void testDescendingSetAndIterator() {
        naturalSet.add(1);
        naturalSet.add(2);
        naturalSet.add(3);
        naturalSet.add(null);

        NavigableSet<Integer> desc = naturalSet.descendingSet();
        Iterator<Integer> it = desc.iterator();
        List<Integer> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }
        // descending order, null first
        assertEquals(Arrays.asList(null, 3, 2, 1), result);
    }

    @Test
    public void testSubSetInclusiveExclusive() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        NavigableSet<Integer> sub = naturalSet.subSet(2, true, 4, false);
        assertEquals(new TreeSet<>(Arrays.asList(2, 3)), sub);
    }

    @Test
    public void testHeadSetAndTailSet() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        SortedSet<Integer> head = naturalSet.headSet(3); // exclusive
        SortedSet<Integer> tail = naturalSet.tailSet(3); // inclusive

        assertEquals(new TreeSet<>(Arrays.asList(1, 2)), head);
        assertEquals(new TreeSet<>(Arrays.asList(3, 4, 5)), tail);
    }

    /* -----------------------------------------------------------------------
     *  Edge‑case & exception tests
     * ----------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testConstructorCollectionNull() {
        new ConcurrentNavigableSetNullSafe<>(null);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorCollectionAndComparatorNull() {
        new ConcurrentNavigableSetNullSafe<>(null, null);
    }

    @Test
    public void testAddAllWithNullElements() {
        List<Integer> list = Arrays.asList(1, null, 2);
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>(list);
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(null));
        assertEquals(3, set.size());
    }

    @Test
    public void testComparatorWithNullsNaturalOrdering() {
        // natural ordering treats null as greatest
        naturalSet.add(5);
        naturalSet.add(null);
        naturalSet.add(1);
        assertEquals(Integer.valueOf(5), naturalSet.lower(null));
        assertEquals(Integer.valueOf(null), naturalSet.ceiling(null));
    }

    @Test
    public void testCustomComparatorNullOrdering() {
        // reverse order comparator: null is still considered greater because we mask it and compare as null
        customSet.add("a");
        customSet.add(null);
        customSet.add("b");
        List<String> expected = Arrays.asList(null, "b", "a"); // descending, null first
        List<String> actual = new ArrayList<>();
        for (String s : customSet) {
            actual.add(s);
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testSubSetWithNullBoundaries() {
        naturalSet.addAll(Arrays.asList(null, 1, 2, 3));
        NavigableSet<Integer> sub = naturalSet.subSet(null, true, 2, true);
        // Expected: null, 1, 2 (null is greatest, but inclusive lower bound includes it)
        assertEquals(new TreeSet<>(Arrays.asList(1, 2, null)), sub);
    }

    @Test
    public void testDescendingSetMutability() {
        naturalSet.add(1);
        naturalSet.add(2);
        NavigableSet<Integer> desc = naturalSet.descendingSet();
        desc.remove(2);
        assertFalse(naturalSet.contains(2));
        assertTrue(desc.contains(1));
    }

    @Test
    public void testIteratorRemove() {
        naturalSet.addAll(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = naturalSet.iterator();
        while (it.hasNext()) {
            Integer v = it.next();
            if (v.equals(2)) {
                it.remove();
            }
        }
        assertFalse(naturalSet.contains(2));
        assertEquals(2, naturalSet.size());
    }

    @Test
    public void testHeadSetInclusiveFlag() {
        NavigableSet<Integer> head = naturalSet.headSet(3, true);
        naturalSet.addAll(Arrays.asList(1, 2, 3, 4));
        assertTrue(head.contains(3));
        assertFalse(head.contains(4));
    }

    @Test
    public void testTailSetInclusiveFlag() {
        NavigableSet<Integer> tail = naturalSet.tailSet(2, false);
        naturalSet.addAll(Arrays.asList(1, 2, 3, 4));
        assertFalse(tail.contains(2));
        assertTrue(tail.contains(3));
    }

    @Test
    public void testSubSetWithSameFromAndToInclusive() {
        naturalSet.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> sub = naturalSet.subSet(2, true, 2, true);
        assertEquals(Collections.singleton(2), sub);
    }

    @Test
    public void testSubSetWithSameFromAndToExclusive() {
        naturalSet.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> sub = naturalSet.subSet(2, false, 2, false);
        assertTrue(sub.isEmpty());
    }
}
