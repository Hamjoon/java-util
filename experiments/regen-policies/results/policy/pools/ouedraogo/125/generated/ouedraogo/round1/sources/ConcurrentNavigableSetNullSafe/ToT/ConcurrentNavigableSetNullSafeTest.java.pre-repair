package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.junit.Assert.*;

public class ConcurrentNavigableSetNullSafeTest {

    private ConcurrentNavigableSetNullSafe<Integer> naturalSet;
    private ConcurrentNavigableSetNullSafe<Integer> customSet;
    private Comparator<Integer> reverseComparator;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // Natural ordering set (no comparator)
        naturalSet = new ConcurrentNavigableSetNullSafe<>();

        // Custom comparator that orders integers in reverse (nulls are still considered greater)
        reverseComparator = (a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return 1;
            if (b == null) return -1;
            return b.compareTo(a);
        };
        customSet = new ConcurrentNavigableSetNullSafe<>(reverseComparator);
    }

    /* --------------------------- Constructor Tests --------------------------- */

    @Test
    public void testDefaultConstructorAddsNullCorrectly() {
        naturalSet.add(null);
        naturalSet.add(5);
        assertEquals(2, naturalSet.size());
        assertTrue(naturalSet.contains(null));
        assertTrue(naturalSet.contains(5));
    }

    @Test
    public void testConstructorWithComparatorOrdersReverse() {
        customSet.add(1);
        customSet.add(3);
        customSet.add(2);
        assertEquals(Integer.valueOf(3), customSet.first()); // reverse order, highest element first
        assertEquals(Integer.valueOf(1), customSet.last());
    }

    @Test
    public void testCollectionConstructorCopiesElementsAndMasksNull() {
        List<Integer> list = Arrays.asList(10, null, 20);
        ConcurrentNavigableSetNullSafe<Integer> setFromCollection = new ConcurrentNavigableSetNullSafe<>(list);
        assertEquals(3, setFromCollection.size());
        assertTrue(setFromCollection.contains(null));
        assertTrue(setFromCollection.contains(10));
        assertTrue(setFromCollection.contains(20));
    }

    @Test
    public void testCollectionConstructorWithComparator() {
        List<Integer> list = Arrays.asList(7, null, 4);
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>(list, reverseComparator);
        assertEquals(Integer.valueOf(7), set.first());
        assertEquals(Integer.valueOf(4), set.last());
        assertTrue(set.contains(null));
    }

    @Test
    public void testCollectionConstructorNullArgumentThrowsNPE() {
        thrown.expect(NullPointerException.class);
        new ConcurrentNavigableSetNullSafe<>(null);
    }

    /* --------------------------- Basic Set Operations --------------------------- */

    @Test
    public void testAddRemoveContainsAndClear() {
        assertTrue(naturalSet.add(1));
        assertTrue(naturalSet.add(null));
        assertFalse(naturalSet.add(1)); // duplicate
        assertTrue(naturalSet.contains(1));
        assertTrue(naturalSet.contains(null));
        assertTrue(naturalSet.remove(1));
        assertFalse(naturalSet.contains(1));
        assertTrue(naturalSet.remove(null));
        assertFalse(naturalSet.contains(null));
        assertTrue(naturalSet.isEmpty());

        // clear on non‑empty set
        naturalSet.add(10);
        naturalSet.add(20);
        assertFalse(naturalSet.isEmpty());
        naturalSet.clear();
        assertTrue(naturalSet.isEmpty());
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(naturalSet.isEmpty());
        assertEquals(0, naturalSet.size());

        naturalSet.add(5);
        assertFalse(naturalSet.isEmpty());
        assertEquals(1, naturalSet.size());

        naturalSet.add(null);
        assertEquals(2, naturalSet.size());
    }

    @Test
    public void testIteratorAndDescendingIterator() {
        naturalSet.add(3);
        naturalSet.add(1);
        naturalSet.add(null);
        naturalSet.add(2);

        // natural ordering: 1,2,3,null (null considered greatest)
        Iterator<Integer> it = naturalSet.iterator();
        List<Integer> forward = new ArrayList<>();
        while (it.hasNext()) {
            forward.add(it.next());
        }
        assertEquals(Arrays.asList(1, 2, 3, null), forward);

        // descending iterator should reverse the order
        Iterator<Integer> dit = naturalSet.descendingIterator();
        List<Integer> backward = new ArrayList<>();
        while (dit.hasNext()) {
            backward.add(dit.next());
        }
        assertEquals(Arrays.asList(null, 3, 2, 1), backward);
    }

    /* --------------------------- NavigableSet Specific Tests --------------------------- */

    @Test
    public void testLowerFloorCeilingHigherWithNullsNaturalOrder() {
        naturalSet.add(10);
        naturalSet.add(20);
        naturalSet.add(null);

        // lower(null) -> 20 (null is greatest)
        assertEquals(Integer.valueOf(20), naturalSet.lower(null));
        // floor(null) -> null
        assertNull(naturalSet.floor(null));
        // ceiling(null) -> null
        assertNull(naturalSet.ceiling(null));
        // higher(null) -> null (no element greater than null)
        assertNull(naturalSet.higher(null));

        // lower(15) -> 10
        assertEquals(Integer.valueOf(10), naturalSet.lower(15));
        // floor(15) -> 10
        assertEquals(Integer.valueOf(10), naturalSet.floor(15));
        // ceiling(15) -> 20
        assertEquals(Integer.valueOf(20), naturalSet.ceiling(15));
        // higher(15) -> 20
        assertEquals(Integer.valueOf(20), naturalSet.higher(15));
    }

    @Test
    public void testLowerFloorCeilingHigherWithCustomComparator() {
        customSet.add(10);
        customSet.add(20);
        customSet.add(null); // null considered greatest even in reverse order

        // In reverse order the sequence is: null,20,10
        assertEquals(Integer.valueOf(20), customSet.lower(null)); // element just below null
        assertNull(customSet.floor(null)); // floor of null is null itself
        assertNull(customSet.ceiling(null)); // ceiling of null is null itself
        assertNull(customSet.higher(null)); // nothing greater than null

        // lower(15) -> 20 (because reverse order: 20 > 10)
        assertEquals(Integer.valueOf(20), customSet.lower(15));
        // floor(15) -> 20
        assertEquals(Integer.valueOf(20), customSet.floor(15));
        // ceiling(15) -> 10
        assertEquals(Integer.valueOf(10), customSet.ceiling(15));
        // higher(15) -> 10
        assertEquals(Integer.valueOf(10), customSet.higher(15));
    }

    @Test
    public void testPollFirstAndPollLastNaturalOrder() {
        naturalSet.add(5);
        naturalSet.add(15);
        naturalSet.add(null);

        // natural order: 5,15,null
        assertEquals(Integer.valueOf(5), naturalSet.pollFirst());
        assertEquals(Integer.valueOf(null), naturalSet.pollLast()); // null is greatest
        assertEquals(1, naturalSet.size());
        assertFalse(naturalSet.contains(null));
        assertFalse(naturalSet.contains(5));
        assertTrue(naturalSet.contains(15));
    }

    @Test
    public void testPollFirstAndPollLastCustomComparator() {
        customSet.add(5);
        customSet.add(15);
        customSet.add(null);

        // reverse order: null,15,5
        assertNull(customSet.pollFirst()); // first is null
        assertEquals(Integer.valueOf(5), customSet.pollLast()); // last is smallest element
        assertEquals(1, customSet.size());
        assertFalse(customSet.contains(null));
        assertFalse(customSet.contains(5));
        assertTrue(customSet.contains(15));
    }

    @Test
    public void testFirstAndLastMethods() {
        naturalSet.add(2);
        naturalSet.add(4);
        naturalSet.add(null);
        assertEquals(Integer.valueOf(2), naturalSet.first());
        assertNull(naturalSet.last());

        customSet.add(2);
        customSet.add(4);
        customSet.add(null);
        assertNull(customSet.first());
        assertEquals(Integer.valueOf(2), customSet.last());
    }

    @Test
    public void testDescendingSetViewReflectsChanges() {
        naturalSet.add(1);
        naturalSet.add(3);
        naturalSet.add(null);
        NavigableSet<Integer> descending = naturalSet.descendingSet();

        // descending view should be: null,3,1
        List<Integer> descList = new ArrayList<>(descending);
        assertEquals(Arrays.asList(null, 3, 1), descList);

        // modify original set and verify view updates
        naturalSet.add(2);
        assertTrue(descending.contains(2));
        assertEquals(Integer.valueOf(2), descending.lower(null)); // element just below null
    }

    @Test
    public void testSubSetInclusiveExclusiveBoundaries() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, 4, null));
        // subSet from 2 (inclusive) to null (exclusive) => 2,3,4
        NavigableSet<Integer> sub = naturalSet.subSet(2, true, null, false);
        assertEquals(new HashSet<>(Arrays.asList(2, 3, 4)), new HashSet<>(sub));

        // subSet from null (inclusive) to null (exclusive) should be empty
        NavigableSet<Integer> emptySub = naturalSet.subSet(null, true, null, false);
        assertTrue(emptySub.isEmpty());
    }

    @Test
    public void testHeadSetAndTailSetWithNulls() {
        naturalSet.addAll(Arrays.asList(5, 10, null));
        // headSet up to null (exclusive) => 5,10
        SortedSet<Integer> head = naturalSet.headSet(null);
        assertEquals(new HashSet<>(Arrays.asList(5, 10)), new HashSet<>(head));

        // tailSet from null (inclusive) => null only
        SortedSet<Integer> tail = naturalSet.tailSet(null);
        assertEquals(1, tail.size());
        assertTrue(tail.contains(null));
    }

    @Test
    public void testComparatorMethodReturnsOriginalComparator() {
        assertNull(naturalSet.comparator());
        assertSame(reverseComparator, customSet.comparator());
    }

    /* --------------------------- Edge Cases and Error Handling --------------------------- */

    @Test
    public void testAddDuplicateNullOnlyOnce() {
        assertTrue(naturalSet.add(null));
        assertFalse(naturalSet.add(null)); // duplicate null should not be added
        assertEquals(1, naturalSet.size());
    }

    @Test
    public void testRemoveNonExistingElementReturnsFalse() {
        naturalSet.add(1);
        assertFalse(naturalSet.remove(2));
        assertFalse(naturalSet.remove(null));
    }

    @Test
    public void testContainsOnEmptySet() {
        assertFalse(naturalSet.contains(1));
        assertFalse(naturalSet.contains(null));
    }

    @Test
    public void testSubSetWhenFromElementIsNull() {
        naturalSet.addAll(Arrays.asList(null, 1, 2));
        // from null (inclusive) to 2 (exclusive) => null,1
        NavigableSet<Integer> sub = naturalSet.subSet(null, true, 2, false);
        assertEquals(new HashSet<>(Arrays.asList(null, 1)), new HashSet<>(sub));
    }

    @Test
    public void testHeadSetInclusiveFlag() {
        naturalSet.addAll(Arrays.asList(1, 2, 3));
        // headSet up to 2 inclusive => 1,2
        SortedSet<Integer> headInc = naturalSet.headSet(2, true);
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), new HashSet<>(headInc));

        // headSet up to 2 exclusive => 1
        SortedSet<Integer> headExc = naturalSet.headSet(2, false);
        assertEquals(Collections.singleton(1), new HashSet<>(headExc));
    }

    @Test
    public void testTailSetInclusiveFlag() {
        naturalSet.addAll(Arrays.asList(1, 2, 3));
        // tailSet from 2 inclusive => 2,3
        SortedSet<Integer> tailInc = naturalSet.tailSet(2, true);
        assertEquals(new HashSet<>(Arrays.asList(2, 3)), new HashSet<>(tailInc));

        // tailSet from 2 exclusive => 3
        SortedSet<Integer> tailExc = naturalSet.tailSet(2, false);
        assertEquals(Collections.singleton(3), new HashSet<>(tailExc));
    }

    @Test
    public void testIteratorRemoveWorks() {
        naturalSet.addAll(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = naturalSet.iterator();
        while (it.hasNext()) {
            Integer val = it.next();
            if (val.equals(2)) {
                it.remove();
            }
        }
        assertFalse(naturalSet.contains(2));
        assertEquals(2, naturalSet.size());
    }
}
