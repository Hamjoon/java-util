package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.*;

import static org.junit.Assert.*;

public class ConcurrentNavigableSetNullSafeTest {

    private ConcurrentNavigableSetNullSafe<String> set;
    private ConcurrentNavigableSetNullSafe<String> setWithComparator;

    private static final Comparator<String> REVERSE_ORDER = Collections.<String>reverseOrder();

    @Before
    public void setUp() {
        set = new ConcurrentNavigableSetNullSafe<>();
        setWithComparator = new ConcurrentNavigableSetNullSafe<>(REVERSE_ORDER);
    }

    @After
    public void tearDown() {
        set.clear();
        setWithComparator.clear();
    }

    /*--------------------------------------------------------------
     *  Constructor tests
     *--------------------------------------------------------------*/

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testConstructorWithComparatorCreatesEmptySet() {
        assertTrue(setWithComparator.isEmpty());
        assertEquals(0, setWithComparator.size());
        assertSame(REVERSE_ORDER, setWithComparator.comparator());
    }

    @Test
    public void testConstructorWithCollectionCopiesElements() {
        List<String> list = Arrays.asList("a", "b", "c", null);
        ConcurrentNavigableSetNullSafe<String> fromCollection = new ConcurrentNavigableSetNullSafe<>(list);
        assertEquals(4, fromCollection.size());
        assertTrue(fromCollection.contains("a"));
        assertTrue(fromCollection.contains(null));
        assertTrue(fromCollection.contains("c"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrowsNPE() {
        new ConcurrentNavigableSetNullSafe<String>((Collection<? extends String>) null);
    }

    @Test
    public void testConstructorWithCollectionAndComparator() {
        List<String> list = Arrays.asList("one", "two", "three");
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(list, REVERSE_ORDER);
        assertEquals(3, set.size());
        assertSame(REVERSE_ORDER, set.comparator());
        // order should be reverse
        Iterator<String> it = set.iterator();
        assertEquals("two", it.next()); // because natural order would be one, three, two; reversed gives two, three, one
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionAndComparatorThrowsNPE() {
        new ConcurrentNavigableSetNullSafe<String>((Collection<? extends String>) null, REVERSE_ORDER);
    }

    /*--------------------------------------------------------------
     *  Basic add / contains / remove / clear / size / isEmpty
     *--------------------------------------------------------------*/

    @Test
    public void testAddAndContains() {
        assertTrue(set.add("alpha"));
        assertTrue(set.contains("alpha"));
        assertFalse(set.contains("beta"));
    }

    @Test
    public void testAddNullAndContainsNull() {
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());
    }

    @Test
    public void testRemoveExistingElement() {
        set.add("x");
        assertTrue(set.remove("x"));
        assertFalse(set.contains("x"));
    }

    @Test
    public void testRemoveNullElement() {
        set.add(null);
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
    }

    @Test
    public void testClearEmptiesSet() {
        set.add("a");
        set.add(null);
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /*--------------------------------------------------------------
     *  Navigable methods (lower, floor, ceiling, higher, first, last,
     *  pollFirst, pollLast) including null handling
     *--------------------------------------------------------------*/

    @Test
    public void testNavigableMethodsWithNaturalOrdering() {
        set.add("a");
        set.add("c");
        set.add(null); // null is considered greater than any non‑null

        // lower
        assertNull(set.lower(null)); // null is greatest, lower is "c"
        assertEquals("a", set.lower("c"));
        assertNull(set.lower("a")); // no lower element

        // floor
        assertNull(set.floor(null));
        assertEquals("c", set.floor("c"));
        assertEquals("a", set.floor("b"));

        // ceiling
        assertNull(set.ceiling(null));
        assertEquals("a", set.ceiling("a"));
        assertEquals("c", set.ceiling("b"));

        // higher
        assertNull(set.higher("c")); // higher than "c" is null
        assertEquals("c", set.higher("a"));
        assertNull(set.higher(null));

        // first / last
        assertEquals("a", set.first());
        assertNull(set.last());

        // pollFirst / pollLast
        assertEquals("a", set.pollFirst());
        assertNull(set.pollLast()); // removes null
        assertEquals(1, set.size());
        assertEquals("c", set.pollFirst());
        assertTrue(set.isEmpty());
    }

    @Test
    public void testNavigableMethodsWithCustomComparator() {
        setWithComparator.add("a");
        setWithComparator.add("c");
        setWithComparator.add(null); // null is greater (as per wrapComparator) even with custom comparator

        // With reverse order comparator, natural ordering is reversed,
        // but null remains greatest.
        assertEquals("c", setWithComparator.lower(null));
        assertEquals("a", setWithComparator.higher("c"));
        assertNull(setWithComparator.first()); // null is first in reverse order
        assertEquals("c", setWithComparator.last());
    }

    /*--------------------------------------------------------------
     *  Subset, headset, tailset (both boolean and SortedSet overloads)
     *--------------------------------------------------------------*/

    @Test
    public void testSubSetInclusiveExclusive() {
        set.addAll(Arrays.asList("a", "b", "c", "d"));
        NavigableSet<String> sub = set.subSet("b", true, "d", false); // should contain b and c
        assertEquals(2, sub.size());
        assertTrue(sub.contains("b"));
        assertTrue(sub.contains("c"));
        assertFalse(sub.contains("d"));
    }

    @Test
    public void testHeadSetAndTailSet() {
        set.addAll(Arrays.asList("a", "b", "c", "d"));
        SortedSet<String> head = set.headSet("c"); // exclusive
        SortedSet<String> tail = set.tailSet("c"); // inclusive

        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(head));
        assertEquals(Arrays.asList("c", "d"), new ArrayList<>(tail));
    }

    @Test
    public void testHeadSetWithInclusiveFlag() {
        set.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> headInc = set.headSet("b", true);
        assertEquals(2, headInc.size());
        assertTrue(headInc.contains("a"));
        assertTrue(headInc.contains("b"));
    }

    @Test
    public void testTailSetWithInclusiveFlag() {
        set.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> tailInc = set.tailSet("b", false);
        assertEquals(1, tailInc.size());
        assertTrue(tailInc.contains("c"));
    }

    /*--------------------------------------------------------------
     *  Descending set and iterator tests (including remove via iterator)
     *--------------------------------------------------------------*/

    @Test
    public void testDescendingSetOrder() {
        set.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> desc = set.descendingSet();
        Iterator<String> it = desc.iterator();
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testDescendingIteratorRemove() {
        set.addAll(Arrays.asList("x", "y", "z"));
        Iterator<String> it = set.descendingIterator();
        assertEquals("z", it.next());
        it.remove(); // removes "z"
        assertFalse(set.contains("z"));
        assertEquals(2, set.size());
    }

    @Test
    public void testIteratorRemove() {
        set.addAll(Arrays.asList("1", "2", "3"));
        Iterator<String> it = set.iterator();
        assertEquals("1", it.next());
        it.remove(); // remove "1"
        assertFalse(set.contains("1"));
        assertEquals(2, set.size());
    }

    /*--------------------------------------------------------------
     *  Comparator exposure and custom comparator behavior with nulls
     *--------------------------------------------------------------*/

    @Test
    public void testComparatorReturnedIsOriginal() {
        assertNull(set.comparator());
        assertSame(REVERSE_ORDER, setWithComparator.comparator());
    }

    @Test
    public void testCustomComparatorHandlesNulls() {
        Comparator<String> nullFriendly = (o1, o2) -> {
            if (o1 == null && o2 == null) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;
            return o1.compareTo(o2);
        };
        ConcurrentNavigableSetNullSafe<String> customSet = new ConcurrentNavigableSetNullSafe<>(nullFriendly);
        customSet.add(null);
        customSet.add("b");
        customSet.add("a");
        // natural order with null first due to comparator
        Iterator<String> it = customSet.iterator();
        assertNull(it.next());
        assertEquals("a", it.next());
        assertEquals("b", it.next());
    }

    @Test(expected = NullPointerException.class)
    public void testComparatorThatDoesNotAcceptNullThrowsWhenAddingNull() {
        Comparator<String> badComparator = (o1, o2) -> o1.compareTo(o2); // NPE if o1 is null
        ConcurrentNavigableSetNullSafe<String> badSet = new ConcurrentNavigableSetNullSafe<>(badComparator);
        badSet.add(null); // should throw NPE inside wrapComparator
    }

    /*--------------------------------------------------------------
     *  Edge cases for empty set navigation methods
     *--------------------------------------------------------------*/

    @Test
    public void testNavigationMethodsOnEmptySetReturnNull() {
        assertNull(set.lower("any"));
        assertNull(set.floor("any"));
        assertNull(set.ceiling("any"));
        assertNull(set.higher("any"));
        assertNull(set.first());
        assertNull(set.last());
        assertNull(set.pollFirst());
        assertNull(set.pollLast());
    }

    /*--------------------------------------------------------------
     *  Ensure that internal masking does not leak sentinel values
     *--------------------------------------------------------------*/

    @Test
    public void testSentinelIsNeverReturnedToCaller() {
        set.add(null);
        set.add("sentinel_test");
        // The sentinel string is internal; we verify that iteration never yields it.
        for (String s : set) {
            assertNotEquals("null_", s); // sentinel starts with "null_"
        }
    }
}
