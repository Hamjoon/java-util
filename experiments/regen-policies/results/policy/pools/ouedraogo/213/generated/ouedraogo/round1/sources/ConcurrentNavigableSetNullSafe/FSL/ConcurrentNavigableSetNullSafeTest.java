package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ConcurrentNavigableSetNullSafeTest {

    private ConcurrentNavigableSetNullSafe<String> naturalSet;
    private ConcurrentNavigableSetNullSafe<String> customSet;

    private static final Comparator<String> LENGTH_COMPARATOR = (s1, s2) -> {
        // Treat null as empty string for length comparison
        int len1 = s1 == null ? 0 : s1.length();
        int len2 = s2 == null ? 0 : s2.length();
        return Integer.compare(len1, len2);
    };

    @Before
    public void setUp() {
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        customSet = new ConcurrentNavigableSetNullSafe<>(LENGTH_COMPARATOR);
    }

    /*** Basic add / contains / iteration including null handling ***/
    @Test
    public void testAddContainsAndIterationWithNull() {
        assertTrue(naturalSet.isEmpty());
        assertEquals(0, naturalSet.size());

        // add non‑null elements
        assertTrue(naturalSet.add("apple"));
        assertTrue(naturalSet.add("banana"));
        // add null element
        assertTrue(naturalSet.add(null));

        // duplicate adds must return false
        assertFalse(naturalSet.add("apple"));
        assertFalse(naturalSet.add(null));

        // contains checks
        assertTrue(naturalSet.contains("apple"));
        assertTrue(naturalSet.contains("banana"));
        assertTrue(naturalSet.contains(null));
        assertFalse(naturalSet.contains("cherry"));

        // size / isEmpty
        assertEquals(3, naturalSet.size());
        assertFalse(naturalSet.isEmpty());

        // iteration order – natural ordering with null considered greater
        Iterator<String> it = naturalSet.iterator();
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    /*** Remove null and non‑null elements ***/
    @Test
    public void testRemoveElements() {
        naturalSet.add("x");
        naturalSet.add(null);
        naturalSet.add("y");

        assertTrue(naturalSet.remove(null));
        assertFalse(naturalSet.contains(null));
        assertEquals(2, naturalSet.size());

        assertTrue(naturalSet.remove("x"));
        assertFalse(naturalSet.contains("x"));
        assertEquals(1, naturalSet.size());

        // removing non‑existent element returns false
        assertFalse(naturalSet.remove("nonexistent"));
    }

    /*** lower / floor / ceiling / higher with natural ordering ***/
    @Test
    public void testNavigableMethodsNaturalOrdering() {
        naturalSet.addAll(Arrays.asList("a", "c", "e", null));

        // lower
        assertNull(naturalSet.lower("a")); // nothing lower than first element
        assertEquals("a", naturalSet.lower("c"));
        assertEquals("c", naturalSet.lower("e"));
        assertEquals("e", naturalSet.lower(null)); // null is greatest

        // floor
        assertEquals("a", naturalSet.floor("a"));
        assertEquals("c", naturalSet.floor("c"));
        assertEquals("e", naturalSet.floor("e"));
        assertEquals(null, naturalSet.floor(null));

        // ceiling
        assertEquals("a", naturalSet.ceiling("a"));
        assertEquals("c", naturalSet.ceiling("b"));
        assertEquals("e", naturalSet.ceiling("e"));
        assertEquals(null, naturalSet.ceiling(null));

        // higher
        assertEquals("c", naturalSet.higher("a"));
        assertEquals("e", naturalSet.higher("c"));
        assertNull(naturalSet.higher("e")); // null is greatest, so no higher element
        assertNull(naturalSet.higher(null));
    }

    /*** pollFirst / pollLast ***/
    @Test
    public void testPollFirstAndLast() {
        naturalSet.addAll(Arrays.asList("m", "a", "z", null));

        // first should be "a"
        assertEquals("a", naturalSet.pollFirst());
        // last should be null (null is greatest)
        assertNull(naturalSet.pollLast());

        // remaining elements
        List<String> remaining = new ArrayList<>();
        for (String s : naturalSet) {
            remaining.add(s);
        }
        Collections.sort(remaining); // natural order without null
        assertEquals(Arrays.asList("m", "z"), remaining);
    }

    /*** first / last on empty set should throw NoSuchElementException ***/
    @Test(expected = NoSuchElementException.class)
    public void testFirstOnEmptyThrows() {
        new ConcurrentNavigableSetNullSafe<>().first();
    }

    @Test(expected = NoSuchElementException.class)
    public void testLastOnEmptyThrows() {
        new ConcurrentNavigableSetNullSafe<>().last();
    }

    /*** descendingSet and descendingIterator ***/
    @Test
    public void testDescendingSetAndIterator() {
        naturalSet.addAll(Arrays.asList("1", "2", "3", null));

        NavigableSet<String> descending = naturalSet.descendingSet();
        Iterator<String> it = descending.iterator();

        // descending order: null, "3", "2", "1"
        assertNull(it.next());
        assertEquals("3", it.next());
        assertEquals("2", it.next());
        assertEquals("1", it.next());
        assertFalse(it.hasNext());

        // verify that descendingSet is also a ConcurrentNavigableSetNullSafe instance
        assertTrue(descending instanceof ConcurrentNavigableSetNullSafe);
    }

    /*** subSet with inclusive/exclusive flags ***/
    @Test
    public void testSubSetInclusiveExclusive() {
        naturalSet.addAll(Arrays.asList("alpha", "beta", "gamma", "delta", null));
        // Subset from "beta" (inclusive) to null (exclusive) should contain beta, delta, gamma
        SortedSet<String> sub = naturalSet.subSet("beta", true, null, false);
        List<String> expected = Arrays.asList("beta", "delta", "gamma");
        assertEquals(expected, new ArrayList<>(sub));
    }

    /*** headSet and tailSet ***/
    @Test
    public void testHeadSetAndTailSet() {
        naturalSet.addAll(Arrays.asList("a", "b", "c", null));

        // headSet up to "b" (exclusive) => ["a"]
        SortedSet<String> head = naturalSet.headSet("b");
        assertEquals(Collections.singletonList("a"), new ArrayList<>(head));

        // tailSet from "b" (inclusive) => ["b", "c", null]
        SortedSet<String> tail = naturalSet.tailSet("b");
        List<String> expectedTail = Arrays.asList("b", "c", null);
        assertEquals(expectedTail, new ArrayList<>(tail));
    }

    /*** clear and isEmpty ***/
    @Test
    public void testClearAndIsEmpty() {
        naturalSet.addAll(Arrays.asList("x", null, "y"));
        assertFalse(naturalSet.isEmpty());
        naturalSet.clear();
        assertTrue(naturalSet.isEmpty());
        assertEquals(0, naturalSet.size());
    }

    /*** custom comparator handling of nulls (by length) ***/
    @Test
    public void testCustomComparatorOrdering() {
        // Length comparator: null (length 0) < "a" (1) < "bb" (2) < "ccc" (3)
        customSet.addAll(Arrays.asList("bb", null, "a", "ccc"));
        Iterator<String> it = customSet.iterator();

        assertNull(it.next());   // null first (length 0)
        assertEquals("a", it.next());
        assertEquals("bb", it.next());
        assertEquals("ccc", it.next());
        assertFalse(it.hasNext());

        // lower / floor / ceiling / higher with custom comparator
        assertNull(customSet.lower("a"));          // nothing lower than null
        assertEquals(null, customSet.floor(null)); // null itself
        assertEquals(null, customSet.ceiling(null)); // null itself
        assertEquals("a", customSet.higher(null));
        assertEquals("bb", customSet.higher("a"));
        assertEquals("ccc", customSet.higher("bb"));
        assertNull(customSet.higher("ccc"));
    }

    /*** comparator() returns the original comparator ***/
    @Test
    public void testComparatorMethod() {
        assertNull(naturalSet.comparator()); // natural ordering -> null
        assertSame(LENGTH_COMPARATOR, customSet.comparator());
    }

    /*** iterator remove works correctly ***/
    @Test
    public void testIteratorRemove() {
        naturalSet.addAll(Arrays.asList("one", "two", null));
        Iterator<String> it = naturalSet.iterator();

        while (it.hasNext()) {
            String val = it.next();
            if ("two".equals(val)) {
                it.remove();
            }
        }
        assertFalse(naturalSet.contains("two"));
        assertEquals(2, naturalSet.size());
        // Remaining elements should be "one" and null in order
        List<String> remaining = new ArrayList<>();
        for (String s : naturalSet) {
            remaining.add(s);
        }
        assertEquals(Arrays.asList("one", null), remaining);
    }

    /*** contains works with null ***/
    @Test
    public void testContainsNull() {
        assertFalse(naturalSet.contains(null));
        naturalSet.add(null);
        assertTrue(naturalSet.contains(null));
    }

    /*** addAll via constructor with collection ***/
    @Test
    public void testConstructorWithCollection() {
        List<String> list = Arrays.asList("x", null, "y");
        ConcurrentNavigableSetNullSafe<String> setFromCollection = new ConcurrentNavigableSetNullSafe<>(list);
        assertEquals(3, setFromCollection.size());
        assertTrue(setFromCollection.contains("x"));
        assertTrue(setFromCollection.contains("y"));
        assertTrue(setFromCollection.contains(null));
    }

    /*** subSet, headSet, tailSet retain custom comparator ***/
    @Test
    public void testSubSetsRetainCustomComparator() {
        customSet.addAll(Arrays.asList("a", "bb", "ccc", null));
        SortedSet<String> sub = customSet.subSet("a", "ccc"); // from "a" (inclusive) to "ccc" (exclusive)
        assertSame(LENGTH_COMPARATOR, sub.comparator());

        SortedSet<String> head = customSet.headSet("bb");
        assertSame(LENGTH_COMPARATOR, head.comparator());

        SortedSet<String> tail = customSet.tailSet(null);
        assertSame(LENGTH_COMPARATOR, tail.comparator());
    }
}
