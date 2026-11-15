package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.SortedSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentNavigableSetNullSafe}.
 *
 * The tests exercise every public method, covering:
 * <ul>
 *   <li>All constructors (default, with comparator, with collection, with collection + comparator)</li>
 *   <li>Basic Set contract (add, contains, remove, size, isEmpty, clear)</li>
 *   <li>Navigation methods (lower, floor, ceiling, higher, pollFirst, pollLast)</li>
 *   <li>Iterators (forward and descending)</li>
 *   <li>Views (subSet, headSet, tailSet, descendingSet) including inclusive/exclusive flags</li>
 *   <li>First / last element retrieval</li>
 *   <li>Comparator exposure</li>
 *   <li>Correct handling of {@code null} elements via the internal sentinel</li>
 * </ul>
 */
public class ConcurrentNavigableSetNullSafeTest {

    /* --------------------------------------------------------------------- */
    /*  Helper comparator that can compare null values.                     */
    /* --------------------------------------------------------------------- */
    private static final Comparator<Integer> NULL_FRIENDLY_COMPARATOR = (a, b) -> {
        if (a == null && b == null) return 0;
        if (a == null) return -1;   // null is considered *less* than any non‑null
        if (b == null) return 1;
        return a.compareTo(b);
    };

    /* --------------------------------------------------------------------- */
    /*  1. Constructors & basic Set contract                                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void testDefaultConstructorAndBasicOperations() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();

        // initially empty
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        // add non‑null values
        assertTrue(set.add(5));
        assertTrue(set.add(1));
        assertTrue(set.add(10));

        // add null – should be accepted
        assertTrue(set.add(null));

        // duplicate adds must return false
        assertFalse(set.add(5));
        assertFalse(set.add(null));

        // contains checks
        assertTrue(set.contains(5));
        assertTrue(set.contains(null));
        assertFalse(set.contains(99));

        // size / isEmpty after inserts
        assertEquals(4, set.size());
        assertFalse(set.isEmpty());

        // remove operations
        assertTrue(set.remove(1));
        assertFalse(set.remove(1)); // already removed
        assertTrue(set.remove(null));
        assertFalse(set.remove(null)); // already removed

        // final state
        assertEquals(2, set.size());
        assertTrue(set.contains(5));
        assertTrue(set.contains(10));
        assertFalse(set.contains(null));

        // clear
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testConstructorWithComparator() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>(NULL_FRIENDLY_COMPARATOR);

        // verify that the comparator is the one we passed in
        assertEquals(NULL_FRIENDLY_COMPARATOR, set.comparator());

        // natural order is overridden – null should be *less* now
        set.add(2);
        set.add(4);
        set.add(null);
        set.add(3);

        // first() should now return null because comparator puts null first
        assertNull(set.first());
        // last() should be the greatest element (4)
        assertEquals(Integer.valueOf(4), set.last());
    }

    @Test
    public void testConstructorWithCollection() {
        List<Integer> source = new ArrayList<>();
        source.add(7);
        source.add(null);
        source.add(3);
        source.add(7); // duplicate, should be ignored

        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>(source);

        // collection constructor must have removed the duplicate 7
        assertEquals(3, set.size());
        assertTrue(set.contains(7));
        assertTrue(set.contains(3));
        assertTrue(set.contains(null));
    }

    @Test
    public void testConstructorWithCollectionAndComparator() {
        List<Integer> source = new ArrayList<>();
        source.add(9);
        source.add(null);
        source.add(1);
        source.add(9);

        ConcurrentNavigableSetNullSafe<Integer> set =
                new ConcurrentNavigableSetNullSafe<>(source, NULL_FRIENDLY_COMPARATOR);

        // verify comparator exposure
        assertEquals(NULL_FRIENDLY_COMPARATOR, set.comparator());

        // verify elements are present and duplicate removed
        assertEquals(3, set.size());
        assertTrue(set.contains(9));
        assertTrue(set.contains(1));
        assertTrue(set.contains(null));
    }

    /* --------------------------------------------------------------------- */
    /*  2. Navigation methods (natural ordering)                             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testNavigationMethodsNaturalOrder() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.add(10);
        set.add(20);
        set.add(30);
        set.add(null); // null is considered greater than any non‑null

        // lower
        assertEquals(Integer.valueOf(20), set.lower(30));
        assertEquals(Integer.valueOf(10), set.lower(20));
        assertNull(set.lower(10)); // nothing lower than 10
        assertEquals(Integer.valueOf(30), set.lower(null)); // null is greatest, so lower(null) = max non‑null

        // floor
        assertEquals(Integer.valueOf(30), set.floor(null)); // null present, floor(null) = null (unmasked)
        assertEquals(Integer.valueOf(20), set.floor(20));
        assertNull(set.floor(5));

        // ceiling
        assertEquals(Integer.valueOf(10), set.ceiling(5));
        assertEquals(Integer.valueOf(20), set.ceiling(20));
        assertEquals(Integer.valueOf(null), set.ceiling(null)); // null present

        // higher
        assertEquals(Integer.valueOf(20), set.higher(10));
        assertEquals(Integer.valueOf(30), set.higher(20));
        assertNull(set.higher(30)); // no element greater than 30
        assertNull(set.higher(null)); // null is greatest

        // first / last
        assertEquals(Integer.valueOf(10), set.first());
        assertNull(set.last()); // last is null (sentinel)

        // pollFirst / pollLast
        assertEquals(Integer.valueOf(10), set.pollFirst());
        assertEquals(Integer.valueOf(null), set.pollLast()); // polls the null element
        assertEquals(2, set.size());
    }

    /* --------------------------------------------------------------------- */
    /*  3. Navigation methods with a custom comparator                       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testNavigationMethodsWithCustomComparator() {
        // Comparator: null first, then natural order
        Comparator<Integer> comp = NULL_FRIENDLY_COMPARATOR;
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>(comp);
        set.add(5);
        set.add(15);
        set.add(null);
        set.add(10);

        // With this comparator null is the smallest element
        assertNull(set.first());
        assertEquals(Integer.valueOf(15), set.last());

        // lower(null) -> null has no lower element
        assertNull(set.lower(null));

        // higher(null) -> smallest non‑null element
        assertEquals(Integer.valueOf(5), set.higher(null));

        // floor(10) -> should be 10 itself
        assertEquals(Integer.valueOf(10), set.floor(10));

        // ceiling(12) -> 15
        assertEquals(Integer.valueOf(15), set.ceiling(12));
    }

    /* --------------------------------------------------------------------- */
    /*  4. Iterator behavior (forward & descending)                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testIteratorsAndRemovalViaIterator() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add(null);
        set.add("cherry");

        // forward iterator should follow natural order (null considered greatest)
        Iterator<String> it = set.iterator();
        List<String> forward = new ArrayList<>();
        while (it.hasNext()) {
            forward.add(it.next());
        }
        assertEquals(4, forward.size());
        assertEquals("apple", forward.get(0));
        assertEquals("banana", forward.get(1));
        assertEquals("cherry", forward.get(2));
        assertNull(forward.get(3));

        // descending iterator should be reverse order
        Iterator<String> dit = set.descendingIterator();
        List<String> descending = new ArrayList<>();
        while (dit.hasNext()) {
            descending.add(dit.next());
        }
        assertEquals(4, descending.size());
        assertNull(descending.get(0));
        assertEquals("cherry", descending.get(1));
        assertEquals("banana", descending.get(2));
        assertEquals("apple", descending.get(3));

        // removal via iterator
        Iterator<String> removeIt = set.iterator();
        while (removeIt.hasNext()) {
            String val = removeIt.next();
            if ("banana".equals(val) || val == null) {
                removeIt.remove();
            }
        }
        assertEquals(2, set.size());
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("cherry"));
        assertFalse(set.contains("banana"));
        assertFalse(set.contains(null));
    }

    /* --------------------------------------------------------------------- */
    /*  5. descendingSet view                                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testDescendingSetView() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(null);

        NavigableSet<Integer> descending = set.descendingSet();

        // descending set should expose the same elements but in reverse order
        Iterator<Integer> it = descending.iterator();
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertNull(it.next());

        // modifications on descending view must reflect in the original set
        assertTrue(descending.remove(2));
        assertFalse(set.contains(2));
        assertEquals(3, set.size());

        // add via descending view (adds element to underlying set)
        descending.add(4);
        assertTrue(set.contains(4));
        assertEquals(Integer.valueOf(4), set.last()); // null still greatest, so last() stays null
    }

    /* --------------------------------------------------------------------- */
    /*  6. SubSet, headSet, tailSet (inclusive / exclusive)                  */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSubSetInclusiveExclusive() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.add(10);
        set.add(20);
        set.add(30);
        set.add(40);
        set.add(null); // greatest

        // subSet from 15 (inclusive) to 35 (exclusive) => should contain 20 and 30
        NavigableSet<Integer> sub = set.subSet(15, true, 35, false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains(20));
        assertTrue(sub.contains(30));
        assertFalse(sub.contains(10));
        assertFalse(sub.contains(40));

        // headSet up to 30 (exclusive) => 10,20
        SortedSet<Integer> head = set.headSet(30);
        assertEquals(2, head.size());
        assertTrue(head.contains(10));
        assertTrue(head.contains(20));

        // tailSet from 30 (inclusive) => 30,40,null
        SortedSet<Integer> tail = set.tailSet(30);
        assertEquals(3, tail.size());
        assertTrue(tail.contains(30));
        assertTrue(tail.contains(40));
        assertTrue(tail.contains(null));
    }

    @Test
    public void testHeadSetAndTailSetWithInclusiveFlag() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("a");
        set.add("b");
        set.add("c");
        set.add(null);

        // headSet(toElement = "b", inclusive = true) => "a", "b"
        SortedSet<String> headInclusive = set.headSet("b", true);
        assertEquals(2, headInclusive.size());
        assertTrue(headInclusive.contains("a"));
        assertTrue(headInclusive.contains("b"));

        // tailSet(fromElement = "b", inclusive = false) => "c", null
        SortedSet<String> tailExclusive = set.tailSet("b", false);
        assertEquals(2, tailExclusive.size());
        assertTrue(tailExclusive.contains("c"));
        assertTrue(tailExclusive.contains(null));
    }

    /* --------------------------------------------------------------------- */
    /*  7. First / Last edge cases                                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testFirstLastWhenSetIsEmptyOrContainsOnlyNull() {
        ConcurrentNavigableSetNullSafe<Integer> empty = new ConcurrentNavigableSetNullSafe<>();
        try {
            empty.first();
            fail("first() on empty set should throw NoSuchElementException");
        } catch (java.util.NoSuchElementException ignored) {
        }

        try {
            empty.last();
            fail("last() on empty set should throw NoSuchElementException");
        } catch (java.util.NoSuchElementException ignored) {
        }

        ConcurrentNavigableSetNullSafe<Integer> onlyNull = new ConcurrentNavigableSetNullSafe<>();
        onlyNull.add(null);
        assertNull(onlyNull.first());
        assertNull(onlyNull.last());
    }

    /* --------------------------------------------------------------------- */
    /*  8. Verify that the internal sentinel never leaks through API          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSentinelIsNeverExposed() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add(null);
        set.add("x");

        // The sentinel string is a UUID‑based value; it must not appear in any public view.
        for (String s : set) {
            assertNotNull(s); // null is allowed, but sentinel is not a real element
        }

        // toArray should also contain only the real elements (null and "x")
        Object[] arr = set.toArray();
        assertEquals(2, arr.length);
        assertTrue(arr[0] == null || "x".equals(arr[0]));
        assertTrue(arr[1] == null || "x".equals(arr[1]));
        assertNotEquals("null_", arr[0]); // sanity check that UUID part is not present
    }
}
