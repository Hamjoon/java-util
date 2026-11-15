package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link ConcurrentNavigableSetNullSafe}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors</li>
 *   <li>Basic Set contract (add, remove, contains, size, clear)</li>
 *   <li>Null‑handling (masking/unmasking) for add/contains/remove</li>
 *   <li>Natural ordering semantics (null is considered greater than any non‑null)</li>
 *   <li>All {@link NavigableSet} navigation methods (lower, floor, ceiling, higher, pollFirst, pollLast)</li>
 *   <li>Views: subSet, headSet, tailSet, descendingSet and their iterators</li>
 *   <li>Preservation of a user supplied comparator</li>
 * </ul>
 */
public class ConcurrentNavigableSetNullSafeTest {

    private ConcurrentNavigableSetNullSafe<Integer> naturalSet;
    private ConcurrentNavigableSetNullSafe<Integer> reverseSet;
    private static final Comparator<Integer> REVERSE_COMPARATOR = (a, b) -> {
        // Define explicit null handling for the reverse comparator.
        // In this test we want null to be considered *smaller* than any non‑null
        // (so it appears first when using reverse order).
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        // Reverse natural order
        return b.compareTo(a);
    };

    @Before
    public void setUp() {
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        reverseSet = new ConcurrentNavigableSetNullSafe<>(REVERSE_COMPARATOR);
    }

    /* --------------------------------------------------------------
     *  Constructor & basic Set contract tests
     * -------------------------------------------------------------- */
    @Test
    public void testEmptySet() {
        assertTrue(naturalSet.isEmpty());
        assertEquals(0, naturalSet.size());
    }

    @Test
    public void testAddAndContains() {
        assertTrue(naturalSet.add(10));
        assertTrue(naturalSet.contains(10));

        // Adding a null element must be accepted
        assertTrue(naturalSet.add(null));
        assertTrue(naturalSet.contains(null));

        // Duplicate add must return false and not change size
        assertFalse(naturalSet.add(10));
        assertFalse(naturalSet.add(null));
        assertEquals(2, naturalSet.size());
    }

    @Test
    public void testRemove() {
        naturalSet.add(5);
        naturalSet.add(null);
        assertTrue(naturalSet.remove(5));
        assertFalse(naturalSet.contains(5));
        assertTrue(naturalSet.remove(null));
        assertFalse(naturalSet.contains(null));
        assertTrue(naturalSet.isEmpty());
    }

    @Test
    public void testClear() {
        naturalSet.addAll(Arrays.asList(1, 2, null));
        assertFalse(naturalSet.isEmpty());
        naturalSet.clear();
        assertTrue(naturalSet.isEmpty());
        assertEquals(0, naturalSet.size());
    }

    @Test
    public void testConstructorWithCollection() {
        List<Integer> source = Arrays.asList(3, null, 1, 2);
        ConcurrentNavigableSetNullSafe<Integer> setFromCollection = new ConcurrentNavigableSetNullSafe<>(source);
        assertEquals(4, setFromCollection.size());
        assertTrue(setFromCollection.containsAll(source));
    }

    @Test
    public void testConstructorWithCollectionAndComparator() {
        List<Integer> source = Arrays.asList(4, null, 2);
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>(source, REVERSE_COMPARATOR);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(source));
        // Verify ordering: reverse comparator + null considered smallest
        Iterator<Integer> it = set.iterator();
        assertEquals((Integer) null, it.next()); // null first
        assertEquals((Integer) 4, it.next());    // then highest (4)
        assertEquals((Integer) 2, it.next());    // then 2
    }

    /* --------------------------------------------------------------
     *  Iterator ordering (natural ordering, null last)
     * -------------------------------------------------------------- */
    @Test
    public void testIteratorOrderNatural() {
        naturalSet.addAll(Arrays.asList(5, 1, 3, null, 2));
        Iterator<Integer> it = naturalSet.iterator();
        assertEquals((Integer) 1, it.next());
        assertEquals((Integer) 2, it.next());
        assertEquals((Integer) 3, it.next());
        assertEquals((Integer) 5, it.next());
        assertEquals((Integer) null, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testIteratorOrderReverseComparator() {
        reverseSet.addAll(Arrays.asList(5, 1, 3, null, 2));
        Iterator<Integer> it = reverseSet.iterator();
        // According to REVERSE_COMPARATOR null is smallest, then descending numbers
        assertEquals((Integer) null, it.next());
        assertEquals((Integer) 5, it.next());
        assertEquals((Integer) 3, it.next());
        assertEquals((Integer) 2, it.next());
        assertEquals((Integer) 1, it.next());
        assertFalse(it.hasNext());
    }

    /* --------------------------------------------------------------
     *  NavigableSet navigation methods (natural ordering)
     * -------------------------------------------------------------- */
    @Test
    public void testNavigationalMethodsNatural() {
        naturalSet.addAll(Arrays.asList(10, 20, 30, null));

        // lower, floor, ceiling, higher with a present element
        assertEquals((Integer) 10, naturalSet.lower(20));
        assertEquals((Integer) 20, naturalSet.floor(20));
        assertEquals((Integer) 20, naturalSet.ceiling(20));
        assertEquals((Integer) 30, naturalSet.higher(20));

        // Relations with null (null is greatest)
        assertEquals((Integer) 30, naturalSet.lower(null));
        assertEquals((Integer) null, naturalSet.floor(null));
        assertEquals((Integer) null, naturalSet.ceiling(null));
        assertNull(naturalSet.higher(null));

        // Relations with a value smaller than any element
        assertNull(naturalSet.lower(5));
        assertNull(naturalSet.floor(5));
        assertEquals((Integer) 10, naturalSet.ceiling(5));
        assertEquals((Integer) 10, naturalSet.higher(5));
    }

    @Test
    public void testPollFirstAndPollLastNatural() {
        naturalSet.addAll(Arrays.asList(7, 3, null, 5));
        // Natural order: 3,5,7,null
        assertEquals((Integer) 3, naturalSet.pollFirst());
        assertEquals((Integer) null, naturalSet.pollLast());
        // Remaining elements should be 5 and 7
        assertEquals(2, naturalSet.size());
        assertTrue(naturalSet.contains(5));
        assertTrue(naturalSet.contains(7));
    }

    /* --------------------------------------------------------------
     *  NavigableSet navigation methods (custom comparator)
     * -------------------------------------------------------------- */
    @Test
    public void testNavigationalMethodsCustomComparator() {
        // Reverse comparator where null is considered smallest.
        reverseSet.addAll(Arrays.asList(10, 20, 30, null));

        // Order: null,30,20,10
        assertEquals((Integer) null, reverseSet.lower(30));   // element before 30 is null
        assertEquals((Integer) 30, reverseSet.floor(30));
        assertEquals((Integer) 30, reverseSet.ceiling(30));
        assertEquals((Integer) 20, reverseSet.higher(30));

        // Test boundaries
        assertEquals((Integer) 10, reverseSet.lower(5)); // 5 not present, element before 5 is 10
        assertNull(reverseSet.higher(10));               // 10 is the smallest non‑null
    }

    /* --------------------------------------------------------------
     *  Sub‑set, head‑set, tail‑set views (natural ordering)
     * -------------------------------------------------------------- */
    @Test
    public void testSubSetInclusiveExclusive() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, 4, 5, null));
        NavigableSet<Integer> sub = naturalSet.subSet(2, true, 4, false);
        // Expected elements: 2,3
        assertEquals(2, sub.size());
        assertTrue(sub.contains(2));
        assertTrue(sub.contains(3));
        assertFalse(sub.contains(4));
        assertFalse(sub.contains(null));

        // Mutating the view must affect the original set
        sub.remove(2);
        assertFalse(naturalSet.contains(2));
        assertEquals(5, naturalSet.size());
    }

    @Test
    public void testHeadSetAndTailSet() {
        naturalSet.addAll(Arrays.asList(10, 20, 30, null));

        // headSet(toElement, inclusive) – exclusive by default
        SortedSet<Integer> head = naturalSet.headSet(20);
        assertEquals(1, head.size());
        assertTrue(head.contains(10));
        assertFalse(head.contains(20));

        // tailSet(fromElement) – inclusive by default
        SortedSet<Integer> tail = naturalSet.tailSet(20);
        assertEquals(3, tail.size());
        assertTrue(tail.contains(20));
        assertTrue(tail.contains(30));
        assertTrue(tail.contains(null));
    }

    /* --------------------------------------------------------------
     *  Descending set and iterator
     * -------------------------------------------------------------- */
    @Test
    public void testDescendingSetAndIterator() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, null));
        NavigableSet<Integer> descending = naturalSet.descendingSet();

        // Expected descending order: null,3,2,1
        Iterator<Integer> it = descending.iterator();
        assertEquals((Integer) null, it.next());
        assertEquals((Integer) 3, it.next());
        assertEquals((Integer) 2, it.next());
        assertEquals((Integer) 1, it.next());
        assertFalse(it.hasNext());

        // first() of descending view should be the original last()
        assertEquals(naturalSet.last(), descending.first());
        // last() of descending view should be the original first()
        assertEquals(naturalSet.first(), descending.last());
    }

    @Test
    public void testDescendingIteratorRemove() {
        naturalSet.addAll(Arrays.asList(5, 10, null));
        Iterator<Integer> descendingIt = naturalSet.descendingIterator();

        // Remove the first element (null) via descending iterator
        assertTrue(descendingIt.hasNext());
        assertNull(descendingIt.next());
        descendingIt.remove();

        assertFalse(naturalSet.contains(null));
        assertEquals(2, naturalSet.size());
        // Remaining order should be 5,10
        Iterator<Integer> it = naturalSet.iterator();
        assertEquals((Integer) 5, it.next());
        assertEquals((Integer) 10, it.next());
    }

    /* --------------------------------------------------------------
     *  Comparator preservation
     * -------------------------------------------------------------- */
    @Test
    public void testComparatorReturned() {
        assertNull(naturalSet.comparator()); // natural order → null comparator
        assertSame(REVERSE_COMPARATOR, reverseSet.comparator());
    }

    @Test
    public void testFirstAndLast() {
        // Natural set: null is greatest
        naturalSet.addAll(Arrays.asList(8, 3, null));
        assertEquals((Integer) 3, naturalSet.first());
        assertEquals((Integer) null, naturalSet.last());

        // Reverse set: null is smallest
        reverseSet.addAll(Arrays.asList(8, 3, null));
        assertEquals((Integer) null, reverseSet.first());
        assertEquals((Integer) 3, reverseSet.last());
    }
}
