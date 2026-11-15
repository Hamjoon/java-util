package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.junit.Assert.*;

public class ConcurrentNavigableSetNullSafeTest {

    private ConcurrentNavigableSetNullSafe<Integer> naturalSet;
    private ConcurrentNavigableSetNullSafe<String> customSet;

    @Before
    public void setUp() {
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        Comparator<String> reverse = (a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return -1;   // null is considered smaller in this custom comparator
            if (b == null) return 1;
            return b.compareTo(a);
        };
        customSet = new ConcurrentNavigableSetNullSafe<>(reverse);
    }

    /*** Basic add / contains / remove / size ***/
    @Test
    public void testBasicSetOperations() {
        assertTrue(naturalSet.isEmpty());
        assertEquals(0, naturalSet.size());

        // add non‑null elements
        assertTrue(naturalSet.add(5));
        assertTrue(naturalSet.add(1));
        assertTrue(naturalSet.add(10));
        assertFalse(naturalSet.add(5)); // duplicate
        assertEquals(3, naturalSet.size());

        // contains
        assertTrue(naturalSet.contains(5));
        assertFalse(naturalSet.contains(2));

        // remove
        assertTrue(naturalSet.remove(Integer.valueOf(1)));
        assertFalse(naturalSet.remove(Integer.valueOf(99)));
        assertEquals(2, naturalSet.size());

        // clear
        naturalSet.clear();
        assertTrue(naturalSet.isEmpty());
    }

    /*** Null handling – add, contains, remove, iterator ***/
    @Test
    public void testNullElements() {
        assertTrue(naturalSet.add(null));
        assertTrue(naturalSet.contains(null));
        assertEquals(1, naturalSet.size());

        // adding another null should be ignored (set semantics)
        assertFalse(naturalSet.add(null));
        assertEquals(1, naturalSet.size());

        // iterator should return null, not sentinel
        Iterator<Integer> it = naturalSet.iterator();
        assertTrue(it.hasNext());
        assertNull(it.next());
        assertFalse(it.hasNext());

        // remove null
        assertTrue(naturalSet.remove(null));
        assertFalse(naturalSet.contains(null));
        assertTrue(naturalSet.isEmpty());
    }

    /*** Ordering – natural ordering with null considered greater ***/
    @Test
    public void testNaturalOrderingWithNulls() {
        naturalSet.add(3);
        naturalSet.add(1);
        naturalSet.add(null);
        naturalSet.add(2);

        // first should be the smallest non‑null element (1)
        assertEquals(Integer.valueOf(1), naturalSet.first());

        // last should be null because null is considered greater
        assertNull(naturalSet.last());

        // lower/ floor/ ceiling/ higher behavior around null
        assertEquals(Integer.valueOf(3), naturalSet.lower(null)); // element just before null
        assertEquals(Integer.valueOf(3), naturalSet.floor(null)); // null itself is greater, so floor is 3
        assertNull(naturalSet.ceiling(null)); // null is present, so ceiling is null
        assertNull(naturalSet.higher(null)); // nothing after null

        // lower of 2 is 1, higher of 2 is 3
        assertEquals(Integer.valueOf(1), naturalSet.lower(2));
        assertEquals(Integer.valueOf(3), naturalSet.higher(2));
    }

    /*** Custom comparator – reverse order, null considered smallest ***/
    @Test
    public void testCustomComparatorOrdering() {
        customSet.add("b");
        customSet.add("a");
        customSet.add(null);
        customSet.add("c");

        // With reverse comparator, order is: null, "c", "b", "a"
        assertNull(customSet.first());
        assertEquals("a", customSet.last());

        // lower/ higher respect custom ordering
        assertEquals("c", customSet.lower("b"));   // element before "b" in reverse order is "c"
        assertEquals("b", customSet.higher("c")); // element after "c" is "b"

        // null is smallest, so lower(null) is null, higher(null) is "c"
        assertNull(customSet.lower(null));
        assertEquals("c", customSet.higher(null));
    }

    /*** subSet, headSet, tailSet with inclusive/exclusive flags and nulls ***/
    @Test
    public void testViewOperations() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, null, 5));

        // subSet 2 (inclusive) .. 5 (exclusive) -> {2,3,null}
        NavigableSet<Integer> sub = naturalSet.subSet(2, true, 5, false);
        assertEquals(3, sub.size());
        assertTrue(sub.contains(2));
        assertTrue(sub.contains(3));
        assertTrue(sub.contains(null));
        assertFalse(sub.contains(5));

        // headSet up to null (exclusive) -> {1,2,3}
        SortedSet<Integer> head = naturalSet.headSet(null);
        assertEquals(3, head.size());
        assertTrue(head.containsAll(Arrays.asList(1, 2, 3)));
        assertFalse(head.contains(null));

        // tailSet from null (inclusive) -> {null,5}
        SortedSet<Integer> tail = naturalSet.tailSet(null);
        assertEquals(2, tail.size());
        assertTrue(tail.contains(null));
        assertTrue(tail.contains(5));
    }

    /*** descendingSet and descendingIterator ***/
    @Test
    public void testDescendingViews() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, null));

        NavigableSet<Integer> descending = naturalSet.descendingSet();
        Iterator<Integer> it = descending.iterator();

        // Expected order: null,3,2,1
        assertTrue(it.hasNext());
        assertNull(it.next());
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertFalse(it.hasNext());

        // descendingIterator should give same order
        Iterator<Integer> dit = naturalSet.descendingIterator();
        assertTrue(dit.hasNext());
        assertNull(dit.next());
        assertEquals(Integer.valueOf(3), dit.next());
    }

    /*** pollFirst and pollLast semantics with nulls ***/
    @Test
    public void testPollOperations() {
        naturalSet.addAll(Arrays.asList(10, 5, null, 1));

        // pollFirst should remove the smallest non‑null element (1)
        assertEquals(Integer.valueOf(1), naturalSet.pollFirst());
        assertFalse(naturalSet.contains(1));

        // pollLast should remove null because it is considered greatest
        assertNull(naturalSet.pollLast());
        assertFalse(naturalSet.contains(null));

        // remaining elements are 5 and 10, ordered naturally
        assertEquals(Integer.valueOf(5), naturalSet.first());
        assertEquals(Integer.valueOf(10), naturalSet.last());
    }

    /*** Construction from collections ***/
    @Test
    public void testConstructionFromCollection() {
        // collection with nulls
        java.util.List<String> list = Arrays.asList("x", null, "y", "x");
        ConcurrentNavigableSetNullSafe<String> setFromList = new ConcurrentNavigableSetNullSafe<>(list);
        assertEquals(3, setFromList.size()); // duplicates removed, null kept
        assertTrue(setFromList.contains(null));
        assertTrue(setFromList.contains("x"));
        assertTrue(setFromList.contains("y"));
    }

    /*** Illegal arguments – null collection should throw NPE ***/
    @Test(expected = NullPointerException.class)
    public void testConstructorNullCollectionThrows() {
        new ConcurrentNavigableSetNullSafe<String>(null);
    }

    /*** Comparator method returns the original comparator ***/
    @Test
    public void testComparatorMethod() {
        assertNull(naturalSet.comparator());

        Comparator<String> rev = (a, b) -> 0;
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(rev);
        assertSame(rev, set.comparator());
    }

    /*** iterator remove works correctly ***/
    @Test
    public void testIteratorRemove() {
        naturalSet.addAll(Arrays.asList(1, 2, null));
        Iterator<Integer> it = naturalSet.iterator();

        while (it.hasNext()) {
            Integer v = it.next();
            if (v == null || v == 2) {
                it.remove();
            }
        }

        assertEquals(1, naturalSet.size());
        assertTrue(naturalSet.contains(1));
        assertFalse(naturalSet.contains(2));
        assertFalse(naturalSet.contains(null));
    }

    /*** Ensure internal sentinel is never exposed ***/
    @Test
    public void testSentinelIsHidden() {
        naturalSet.add(null);
        // The sentinel is a unique string; it should never be returned by any public API
        for (Object o : naturalSet) {
            assertFalse(o instanceof String && ((String) o).startsWith("null_"));
        }
        assertNull(naturalSet.first());
        assertNull(naturalSet.last());
    }
}
