package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentNavigableSetNullSafe}.
 *
 * The tests are organized to cover:
 * <ul>
 *   <li>All constructors – normal and error paths</li>
 *   <li>Basic {@link Set} contract (add, remove, contains, size, clear, isEmpty)</li>
 *   <li>Null handling via the internal sentinel</li>
 *   <li>Natural ordering (null considered greater) and custom comparator ordering</li>
 *   <li>All {@link NavigableSet} navigation methods (lower, floor, ceiling, higher, pollFirst, pollLast)</li>
 *   <li>Iterators (forward and descending) and their remove operation</li>
 *   <li>Subset views (subSet, headSet, tailSet) – inclusive/exclusive and null edge cases</li>
 *   <li>Descending set view</li>
 * </ul>
 *
 * No mocking or subclassing of library types is used – all objects are created via the
 * public API of {@code ConcurrentNavigableSetNullSafe} and the standard JDK collections.
 */
public class ConcurrentNavigableSetNullSafeTest {

    private ConcurrentNavigableSetNullSafe<Integer> naturalSet;
    private ConcurrentNavigableSetNullSafe<Integer> customComparatorSet;

    /** Comparator that treats {@code null} as the smallest element. */
    private static final Comparator<Integer> NULL_FIRST_COMPARATOR = (a, b) -> {
        if (a == null && b == null) return 0;
        if (a == null) return -1;          // null < any non‑null
        if (b == null) return 1;           // any non‑null > null
        return a.compareTo(b);
    };

    @Before
    public void setUp() {
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        customComparatorSet = new ConcurrentNavigableSetNullSafe<>(NULL_FIRST_COMPARATOR);
    }

    /* ==============================
       Constructor tests
       ============================== */

    @Test
    public void testDefaultConstructor_NaturalOrdering() {
        assertTrue(naturalSet.isEmpty());
        assertNull(naturalSet.comparator());
    }

    @Test
    public void testConstructorWithComparator() {
        assertTrue(customComparatorSet.isEmpty());
        assertSame(NULL_FIRST_COMPARATOR, customComparatorSet.comparator());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrows() {
        new ConcurrentNavigableSetNullSafe<>(null);
    }

    @Test
    public void testConstructorFromCollection_NaturalOrdering() {
        List<Integer> source = Arrays.asList(3, 1, 2, null);
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>(source);
        assertEquals(4, set.size());
        // natural ordering: null is considered greatest, so last() must be null
        assertEquals(Integer.valueOf(3), set.first());
        assertNull(set.last());
    }

    @Test
    public void testConstructorFromCollection_WithComparator() {
        List<Integer> source = Arrays.asList(3, null, 1, 2);
        ConcurrentNavigableSetNullSafe<Integer> set =
                new ConcurrentNavigableSetNullSafe<>(source, NULL_FIRST_COMPARATOR);
        assertEquals(4, set.size());
        // comparator puts null first
        assertNull(set.first());
        assertEquals(Integer.valueOf(3), set.last());
    }

    /* ==============================
       Basic Set contract
       ============================== */

    @Test
    public void testAddAndContains_NullAndNonNull() {
        assertTrue(naturalSet.add(5));
        assertTrue(naturalSet.add(null));
        assertTrue(naturalSet.contains(5));
        assertTrue(naturalSet.contains(null));
        // duplicate adds must return false
        assertFalse(naturalSet.add(5));
        assertFalse(naturalSet.add(null));
        assertEquals(2, naturalSet.size());
    }

    @Test
    public void testRemove_NullAndNonNull() {
        naturalSet.add(10);
        naturalSet.add(null);
        assertTrue(naturalSet.remove(10));
        assertFalse(naturalSet.contains(10));
        assertTrue(naturalSet.remove(null));
        assertFalse(naturalSet.contains(null));
        assertTrue(naturalSet.isEmpty());
    }

    @Test
    public void testClearAndIsEmpty() {
        naturalSet.add(1);
        naturalSet.add(2);
        naturalSet.clear();
        assertTrue(naturalSet.isEmpty());
        assertEquals(0, naturalSet.size());
    }

    /* ==============================
       NavigableSet navigation (natural ordering)
       ============================== */

    @Test
    public void testNavigationMethods_NaturalOrdering() {
        naturalSet.addAll(Arrays.asList(1, 3, 5, null));
        // Order: 1 < 3 < 5 < null
        assertEquals(Integer.valueOf(1), naturalSet.lower(3));
        assertEquals(Integer.valueOf(3), naturalSet.lower(5));
        assertNull(naturalSet.lower(null)); // null is greatest, no lower element

        assertEquals(Integer.valueOf(1), naturalSet.floor(1));
        assertEquals(Integer.valueOf(3), naturalSet.floor(4));
        assertEquals(Integer.valueOf(5), naturalSet.floor(null));

        assertEquals(Integer.valueOf(1), naturalSet.ceiling(0));
        assertEquals(Integer.valueOf(3), naturalSet.ceiling(2));
        assertEquals(Integer.valueOf(5), naturalSet.ceiling(5));
        assertNull(naturalSet.ceiling(null)); // null is greatest, no ceiling above it

        assertEquals(Integer.valueOf(3), naturalSet.higher(1));
        assertEquals(Integer.valueOf(5), naturalSet.higher(3));
        assertNull(naturalSet.higher(5)); // null is not higher than itself
        assertNull(naturalSet.higher(null));
    }

    @Test
    public void testPollFirstAndLast_NaturalOrdering() {
        naturalSet.addAll(Arrays.asList(2, 4, null));
        // first -> 2, last -> null
        assertEquals(Integer.valueOf(2), naturalSet.pollFirst());
        assertEquals(Integer.valueOf(4), naturalSet.pollFirst());
        assertNull(naturalSet.pollFirst()); // now only null remains, poll returns null and set empty
        assertTrue(naturalSet.isEmpty());

        // repopulate
        naturalSet.addAll(Arrays.asList(1, null, 3));
        assertNull(naturalSet.pollLast()); // null is greatest
        assertEquals(Integer.valueOf(3), naturalSet.pollLast());
        assertEquals(Integer.valueOf(1), naturalSet.pollLast());
        assertTrue(naturalSet.isEmpty());
    }

    /* ==============================
       NavigableSet navigation (custom comparator where null is smallest)
       ============================== */

    @Test
    public void testNavigationMethods_CustomComparator_NullFirst() {
        customComparatorSet.addAll(Arrays.asList(10, null, 20));
        // Order: null < 10 < 20
        assertNull(customComparatorSet.lower(null)); // nothing lower than null
        assertNull(customComparatorSet.lower(10));    // lower(null) -> null

        assertNull(customComparatorSet.floor(null));
        assertEquals(Integer.valueOf(10), customComparatorSet.floor(10));
        assertEquals(Integer.valueOf(10), customComparatorSet.floor(15));

        assertNull(customComparatorSet.ceiling(null));
        assertEquals(Integer.valueOf(10), customComparatorSet.ceiling(5));
        assertEquals(Integer.valueOf(20), customComparatorSet.ceiling(15));

        assertEquals(Integer.valueOf(10), customComparatorSet.higher(null));
        assertEquals(Integer.valueOf(20), customComparatorSet.higher(10));
        assertNull(customComparatorSet.higher(20));
    }

    /* ==============================
       Iterator behavior
       ============================== */

    @Test
    public void testIterator_OrderAndRemove() {
        naturalSet.addAll(Arrays.asList(3, 1, null, 2));
        Iterator<Integer> it = naturalSet.iterator();
        List<Integer> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // natural order: 1,2,3,null
        assertEquals(Arrays.asList(1, 2, 3, null), iterated);

        // test remove via iterator
        it = naturalSet.iterator();
        while (it.hasNext()) {
            Integer val = it.next();
            if (val != null && val % 2 == 1) { // remove odd numbers
                it.remove();
            }
        }
        assertEquals(new HashSet<>(Arrays.asList(2, null)), new HashSet<>(naturalSet));
    }

    @Test
    public void testDescendingIterator() {
        naturalSet.addAll(Arrays.asList(1, 2, null));
        Iterator<Integer> dit = naturalSet.descendingIterator();
        List<Integer> desc = new ArrayList<>();
        while (dit.hasNext()) {
            desc.add(dit.next());
        }
        // descending natural order: null,2,1
        assertEquals(Arrays.asList(null, 2, 1), desc);
    }

    /* ==============================
       Sub‑set views
       ============================== */

    @Test
    public void testSubSet_InclusiveExclusive_NaturalOrdering() {
        naturalSet.addAll(Arrays.asList(1, 2, 3, null));
        NavigableSet<Integer> sub = naturalSet.subSet(2, true, null, false);
        // Expected elements: 2,3 (null excluded)
        assertEquals(new HashSet<>(Arrays.asList(2, 3)), new HashSet<>(sub));
        assertTrue(sub.add(4)); // 4 < null, still within bounds
        assertFalse(sub.contains(null));
        assertTrue(sub.contains(4));
        // changes must reflect in the original set
        assertTrue(naturalSet.contains(4));
        assertEquals(5, naturalSet.size());
    }

    @Test
    public void testHeadSet_TailSet_WithNullBoundaries() {
        naturalSet.addAll(Arrays.asList(5, 10, null));
        // headSet up to 10 (exclusive) -> should contain 5 only
        NavigableSet<Integer> head = naturalSet.headSet(10, false);
        assertEquals(Collections.singleton(5), new HashSet<>(head));

        // tailSet from null (inclusive) -> should contain only null
        NavigableSet<Integer> tail = naturalSet.tailSet(null, true);
        assertEquals(Collections.singleton(null), new HashSet<>(tail));

        // Mutating the view must affect the backing set
        assertTrue(head.remove(5));
        assertFalse(naturalSet.contains(5));
        assertTrue(tail.remove(null));
        assertFalse(naturalSet.contains(null));
    }

    @Test
    public void testDescendingSet_ViewAndComparatorPreservation() {
        customComparatorSet.addAll(Arrays.asList(null, 2, 1));
        NavigableSet<Integer> desc = customComparatorSet.descendingSet();
        // descending order with null‑first comparator becomes: 2,1,null
        Iterator<Integer> it = desc.iterator();
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertNull(it.next());

        // Comparator of the descending view must be the same as original
        assertSame(NULL_FIRST_COMPARATOR, desc.comparator());

        // Adding via descending view must respect original ordering
        assertTrue(desc.add(3));
        // In original set order (null first) the set now contains null,1,2,3
        assertEquals(new HashSet<>(Arrays.asList(null, 1, 2, 3)), new HashSet<>(customComparatorSet));
    }

    /* ==============================
       First/Last element access
       ============================== */

    @Test
    public void testFirstAndLast_NaturalOrdering() {
        naturalSet.addAll(Arrays.asList(7, 3, null));
        assertEquals(Integer.valueOf(3), naturalSet.first());
        assertNull(naturalSet.last());
    }

    @Test
    public void testFirstAndLast_CustomComparator_NullFirst() {
        customComparatorSet.addAll(Arrays.asList(5, null, 8));
        assertNull(customComparatorSet.first());
        assertEquals(Integer.valueOf(8), customComparatorSet.last());
    }

    /* ==============================
       Edge case: duplicate null handling
       ============================== */

    @Test
    public void testDuplicateNullsAreIgnored() {
        assertTrue(naturalSet.add(null));
        assertFalse(naturalSet.add(null)); // second add returns false
        assertEquals(1, naturalSet.size());
        assertTrue(naturalSet.contains(null));
        assertTrue(naturalSet.remove(null));
        assertFalse(naturalSet.contains(null));
        assertTrue(naturalSet.isEmpty());
    }

    /* ==============================
       Verify internal sentinel never leaks
       ============================== */

    @Test
    public void testSentinelNeverExposedThroughPublicAPI() {
        naturalSet.add(null);
        // The sentinel is a String that cannot be cast to Integer, so any attempt to retrieve it
        // via the public API must return null, not the sentinel.
        for (Object o : naturalSet) {
            assertTrue(o == null || o instanceof Integer);
        }
        assertNull(naturalSet.first());
        assertNull(naturalSet.last());
        assertNull(naturalSet.pollFirst());
        assertNull(naturalSet.pollLast());
    }
}
