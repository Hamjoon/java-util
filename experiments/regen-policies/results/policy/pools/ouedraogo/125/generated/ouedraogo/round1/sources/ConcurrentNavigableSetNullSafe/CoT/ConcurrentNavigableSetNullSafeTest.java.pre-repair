package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ConcurrentNavigableSetNullSafe}.
 *
 * The tests are grouped as follows:
 * <ul>
 *   <li>Construction & basic add/contains/remove</li>
 *   <li>Null handling (masking/unmasking)</li>
 *   <li>Comparator handling (including a custom comparator that orders nulls)</li>
 *   <li>NavigableSet navigation methods (lower, floor, ceiling, higher, pollFirst, pollLast)</li>
 *   <li>View methods (subSet, headSet, tailSet, descendingSet, descendingIterator)</li>
 *   <li>Iterator remove and clear</li>
 *   <li>Edge‑case & exception tests</li>
 * </ul>
 */
public class ConcurrentNavigableSetNullSafeTest {

    private ConcurrentNavigableSetNullSafe<Integer> set;

    @Before
    public void setUp() {
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    /* --------------------------------------------------------------------- */
    /* 1.  Public constructors – signatures (extracted for reference only)   */
    /* --------------------------------------------------------------------- */
    // ConcurrentNavigableSetNullSafe()
    // ConcurrentNavigableSetNullSafe(Comparator<? super E> comparator)
    // ConcurrentNavigableSetNullSafe(Collection<? extends E> c)
    // ConcurrentNavigableSetNullSafe(Collection<? extends E> c,
    //                               Comparator<? super E> comparator)

    /* --------------------------------------------------------------------- */
    /* 2.  Basic add / contains / remove tests                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndContainsNonNullElements() {
        assertTrue(set.add(10));
        assertTrue(set.add(5));
        assertTrue(set.add(20));

        assertEquals(3, set.size());
        assertTrue(set.contains(5));
        assertTrue(set.contains(10));
        assertTrue(set.contains(20));
        assertFalse(set.contains(99));

        // natural ordering: iterator should return 5,10,20
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(5), it.next());
        assertEquals(Integer.valueOf(10), it.next());
        assertEquals(Integer.valueOf(20), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testAddNullElement() {
        assertTrue(set.add(null));
        assertEquals(1, set.size());
        assertTrue(set.contains(null));

        // iterator should return the null element
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testAddDuplicateElements() {
        assertTrue(set.add(1));
        assertFalse(set.add(1));               // duplicate non‑null
        assertTrue(set.add(null));
        assertFalse(set.add(null));            // duplicate null
        assertEquals(2, set.size());
    }

    @Test
    public void testRemoveElements() {
        set.add(100);
        set.add(null);
        assertTrue(set.remove(100));
        assertFalse(set.contains(100));
        assertEquals(1, set.size());

        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertTrue(set.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Constructor edge cases                                             */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrows() {
        new ConcurrentNavigableSetNullSafe<Integer>((Collection<Integer>) null);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionAndComparatorThrows() {
        new ConcurrentNavigableSetNullSafe<Integer>((Collection<Integer>) null, null);
    }

    @Test
    public void testConstructorWithCollectionCopiesElements() {
        List<Integer> source = Arrays.asList(3, 1, null, 2);
        ConcurrentNavigableSetNullSafe<Integer> copy = new ConcurrentNavigableSetNullSafe<>(source);

        assertEquals(4, copy.size());
        assertTrue(copy.containsAll(source));

        // natural ordering: null is considered greatest, so iterator = 1,2,3,null
        Iterator<Integer> it = copy.iterator();
        assertEquals(Integer.valueOf(1), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(3), it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    /* --------------------------------------------------------------------- */
    /* 4.  Custom comparator handling                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCustomComparatorOrdersNullAsSmallest() {
        Comparator<Integer> descendingWithNullFirst = (a, b) -> {
            if (a == null && b == null) return 0;
            if (a == null) return -1;          // null is smallest
            if (b == null) return 1;
            return b.compareTo(a);             // descending order for non‑null
        };

        ConcurrentNavigableSetNullSafe<Integer> customSet =
                new ConcurrentNavigableSetNullSafe<>(descendingWithNullFirst);

        customSet.add(5);
        customSet.add(1);
        customSet.add(null);
        customSet.add(3);

        // Expected order: null, 5, 3, 1
        Iterator<Integer> it = customSet.iterator();
        assertNull(it.next());
        assertEquals(Integer.valueOf(5), it.next());
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertFalse(it.hasNext());

        // first() should be null, last() should be 1
        assertNull(customSet.first());
        assertEquals(Integer.valueOf(1), customSet.last());
    }

    /* --------------------------------------------------------------------- */
    /* 5.  NavigableSet navigation methods                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNavigationMethodsWithNullAndNonNull() {
        // natural ordering: null is greater than any non‑null
        set.add(10);
        set.add(20);
        set.add(null);
        set.add(5);

        // lower(null) -> greatest element < null, i.e. 20
        assertEquals(Integer.valueOf(20), set.lower(null));

        // floor(null) -> null itself (present)
        assertNull(set.floor(null));

        // ceiling(15) -> 20
        assertEquals(Integer.valueOf(20), set.ceiling(15));

        // higher(20) -> null (since null is greater)
        assertNull(set.higher(20));

        // lower(5) -> null (nothing smaller)
        assertNull(set.lower(5));
    }

    @Test
    public void testPollFirstAndPollLast() {
        // natural ordering: iterator = 5,10,20,null
        set.add(10);
        set.add(20);
        set.add(5);
        set.add(null);

        assertEquals(Integer.valueOf(5), set.pollFirst());   // smallest non‑null
        assertEquals(Integer.valueOf(null), set.pollLast()); // null is greatest

        // Remaining element should be 10 and 20
        assertEquals(2, set.size());
        assertTrue(set.contains(10));
        assertTrue(set.contains(20));
    }

    /* --------------------------------------------------------------------- */
    /* 6.  View methods (subSet, headSet, tailSet, descendingSet)           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testSubSetInclusiveExclusive() {
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));
        NavigableSet<Integer> sub = set.subSet(2, true, 4, false); // [2,4)

        assertEquals(2, sub.size());
        assertTrue(sub.contains(2));
        assertTrue(sub.contains(3));
        assertFalse(sub.contains(4));
    }

    @Test
    public void testHeadSetAndTailSet() {
        set.addAll(Arrays.asList(10, 20, 30, 40));
        SortedSet<Integer> head = set.headSet(30); // exclusive
        SortedSet<Integer> tail = set.tailSet(30); // inclusive

        assertEquals(Arrays.asList(10, 20), new ArrayList<>(head));
        assertEquals(Arrays.asList(30, 40), new ArrayList<>(tail));
    }

    @Test
    public void testDescendingSetAndIterator() {
        set.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> desc = set.descendingSet();

        // descending order should be 3,2,1
        Iterator<Integer> it = desc.iterator();
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertFalse(it.hasNext());

        // descendingIterator on original set should give same order
        Iterator<Integer> dIt = set.descendingIterator();
        assertEquals(Integer.valueOf(3), dIt.next());
        assertEquals(Integer.valueOf(2), dIt.next());
        assertEquals(Integer.valueOf(1), dIt.next());
        assertFalse(dIt.hasNext());
    }

    /* --------------------------------------------------------------------- */
    /* 7.  Iterator remove and clear                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIteratorRemove() {
        set.addAll(Arrays.asList(100, 200, 300));
        Iterator<Integer> it = set.iterator();

        while (it.hasNext()) {
            Integer val = it.next();
            if (val.equals(200)) {
                it.remove();
            }
        }

        assertEquals(2, set.size());
        assertFalse(set.contains(200));
        assertTrue(set.contains(100));
        assertTrue(set.contains(300));
    }

    @Test
    public void testClearEmptiesSet() {
        set.addAll(Arrays.asList(1, 2, null));
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /* --------------------------------------------------------------------- */
    /* 8.  Edge‑case checks for navigation with absent elements               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNavigationWhenElementAbsent() {
        // Empty set
        assertNull(set.lower(10));
        assertNull(set.floor(10));
        assertNull(set.ceiling(10));
        assertNull(set.higher(10));
        assertNull(set.pollFirst());
        assertNull(set.pollLast());

        // Populate without null
        set.addAll(Arrays.asList(5, 15));
        // lower(5) -> null (nothing smaller)
        assertNull(set.lower(5));
        // higher(15) -> null (nothing greater)
        assertNull(set.higher(15));
    }

    /* --------------------------------------------------------------------- */
    /* 9.  Comparator accessor                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComparatorAccessor() {
        Comparator<Integer> comp = Integer::compareTo;
        ConcurrentNavigableSetNullSafe<Integer> withComp =
                new ConcurrentNavigableSetNullSafe<>(comp);
        assertSame(comp, withComp.comparator());
    }
}
