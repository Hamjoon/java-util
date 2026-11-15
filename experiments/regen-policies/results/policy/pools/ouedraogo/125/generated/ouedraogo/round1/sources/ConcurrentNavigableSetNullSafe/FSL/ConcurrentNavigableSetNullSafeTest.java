package com.cedarsoftware.util;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ConcurrentNavigableSetNullSafeTest {

    /*---------------------- Helper methods ----------------------*/

    private static NavigableSet<String> createNaturalSet(String... elements) {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        for (String e : elements) {
            set.add(e);
        }
        return set;
    }

    private static NavigableSet<Integer> createNaturalIntSet(Integer... elements) {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        for (Integer e : elements) {
            set.add(e);
        }
        return set;
    }

    /*---------------------- Constructor Tests ----------------------*/

    @Test
    public void testEmptyConstructorNaturalOrdering() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertNull(set.comparator());
    }

    @Test
    public void testConstructorWithCollectionIncludingNulls() {
        List<String> list = Arrays.asList("alpha", null, "beta", null, "alpha");
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(list);
        // Set should contain three distinct elements: "alpha", "beta", null
        assertEquals(3, set.size());
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("beta"));
        assertTrue(set.contains(null));
    }

    @Test
    public void testConstructorWithComparator() {
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(reverse);
        assertEquals(reverse, set.comparator());
        set.add("a");
        set.add("b");
        // In reverse order, "b" should be first
        assertEquals("b", set.first());
        assertEquals("a", set.last());
    }

    /*---------------------- Basic Set Operations ----------------------*/

    @Test
    public void testAddRemoveContainsAndSize() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(set.add("x"));
        assertFalse(set.add("x")); // duplicate
        assertTrue(set.add(null));
        assertFalse(set.add(null)); // duplicate null

        assertEquals(2, set.size());
        assertTrue(set.contains("x"));
        assertTrue(set.contains(null));
        assertFalse(set.contains("y"));

        assertTrue(set.remove("x"));
        assertFalse(set.remove("x"));
        assertTrue(set.remove(null));
        assertFalse(set.remove(null));

        assertTrue(set.isEmpty());
    }

    @Test
    public void testClearEmptiesSet() {
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.add(1);
        set.add(2);
        set.add(null);
        assertFalse(set.isEmpty());

        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /*---------------------- Natural Ordering with Null ----------------------*/

    @Test
    public void testNaturalOrderingIncludingNull() {
        NavigableSet<String> set = createNaturalSet("a", "b", null);
        // Natural order: "a" < "b" < null (null considered greatest)
        assertEquals("a", set.first());
        assertEquals(null, set.last());

        assertEquals("a", set.lower("b"));
        assertEquals("b", set.lower(null));
        assertNull(set.lower("a")); // nothing less than "a"

        assertEquals("b", set.higher("a"));
        assertNull(set.higher(null)); // nothing greater than null
        assertEquals(null, set.higher("b"));

        assertEquals("b", set.floor(null));
        assertEquals("a", set.ceiling("a"));
        assertEquals("b", set.ceiling("b"));
        assertEquals(null, set.ceiling(null));
    }

    @Test
    public void testPollFirstAndLastWithNull() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("first");
        set.add(null);
        set.add("mid");

        // Order: "first" < "mid" < null
        assertEquals("first", set.pollFirst()); // removes "first"
        assertEquals(null, set.pollLast());   // removes null
        assertEquals(1, set.size());
        assertEquals("mid", set.first());
        assertEquals("mid", set.last());
    }

    /*---------------------- Iterator Tests ----------------------*/

    @Test
    public void testIteratorOrder() {
        NavigableSet<String> set = createNaturalSet("c", "a", "b", null);
        List<String> expected = Arrays.asList("a", "b", "c", null);
        List<String> actual = new ArrayList<>();
        for (String s : set) {
            actual.add(s);
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testDescendingIteratorOrder() {
        NavigableSet<String> set = createNaturalSet("c", "a", "b", null);
        Iterator<String> descendingIt = set.descendingIterator();
        List<String> actual = new ArrayList<>();
        while (descendingIt.hasNext()) {
            actual.add(descendingIt.next());
        }
        List<String> expected = Arrays.asList(null, "c", "b", "a");
        assertEquals(expected, actual);
    }

    /*---------------------- Descending Set ----------------------*/

    @Test
    public void testDescendingSetView() {
        NavigableSet<String> set = createNaturalSet("a", "b", null);
        NavigableSet<String> descending = set.descendingSet();

        // descending view should have reverse order
        List<String> expected = Arrays.asList(null, "b", "a");
        List<String> actual = new ArrayList<>(descending);
        assertEquals(expected, actual);

        // Verify first/last on descending view
        assertEquals(null, descending.first());
        assertEquals("a", descending.last());
    }

    /*---------------------- SubSet, HeadSet, TailSet ----------------------*/

    @Test
    public void testSubSetInclusiveExclusive() {
        NavigableSet<Integer> set = createNaturalIntSet(1, 2, 3, 4, 5);
        NavigableSet<Integer> sub = set.subSet(2, true, 4, false); // should contain 2,3
        assertEquals(2, sub.size());
        assertTrue(sub.contains(2));
        assertTrue(sub.contains(3));
        assertFalse(sub.contains(4));

        // Exclusive both ends
        NavigableSet<Integer> sub2 = set.subSet(2, false, 5, false); // 3,4
        assertEquals(Arrays.asList(3, 4), new ArrayList<>(sub2));
    }

    @Test
    public void testHeadSetAndTailSet() {
        NavigableSet<Integer> set = createNaturalIntSet(1, 2, 3, 4, 5);
        // headSet up to 3 (exclusive)
        SortedSet<Integer> head = set.headSet(3);
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(head));

        // tailSet from 3 (inclusive)
        SortedSet<Integer> tail = set.tailSet(3);
        assertEquals(Arrays.asList(3, 4, 5), new ArrayList<>(tail));
    }

    @Test
    public void testHeadSetAndTailSetWithNullNaturalOrdering() {
        NavigableSet<String> set = createNaturalSet("a", "b", null);
        // headSet up to "b" (exclusive) -> should contain "a"
        SortedSet<String> head = set.headSet("b");
        assertEquals(Collections.singletonList("a"), new ArrayList<>(head));

        // tailSet from "b" (inclusive) -> "b", null
        SortedSet<String> tail = set.tailSet("b");
        assertEquals(Arrays.asList("b", null), new ArrayList<>(tail));
    }

    /*---------------------- Custom Comparator Handling Null ----------------------*/

    @Test
    public void testCustomComparatorOrdersByLengthAndHandlesNull() {
        Comparator<String> lengthComparator = (s1, s2) -> {
            if (s1 == null && s2 == null) return 0;
            if (s1 == null) return -1; // null is smallest
            if (s2 == null) return 1;
            return Integer.compare(s1.length(), s2.length());
        };

        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        set.add("a");      // length 1
        set.add("bb");     // length 2
        set.add("ccc");    // length 3
        set.add(null);     // should be first

        // Expected order: null, "a", "bb", "ccc"
        List<String> expected = Arrays.asList(null, "a", "bb", "ccc");
        List<String> actual = new ArrayList<>(set);
        assertEquals(expected, actual);

        // Verify comparator is the one we passed
        assertEquals(lengthComparator, set.comparator());

        // Test navigation methods with custom ordering
        assertNull(set.lower("a"));          // null is less than "a"
        assertEquals("a", set.higher(null)); // next after null is "a"
        assertEquals("bb", set.higher("a"));
        assertEquals("ccc", set.last());
    }

    /*---------------------- SubSet Views Preserve Comparator ----------------------*/

    @Test
    public void testSubSetPreservesCustomComparator() {
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(reverse);
        set.add("a");
        set.add("b");
        set.add("c");

        NavigableSet<String> sub = set.subSet("c", true, "a", false); // reverse order, should contain "c","b"
        assertEquals(reverse, sub.comparator());

        List<String> expected = Arrays.asList("c", "b");
        assertEquals(expected, new ArrayList<>(sub));
    }

    /*---------------------- Equality and HashCode Not Overridden (Inherited) ----------------------*/

    @Test
    public void testContainsWorksForAllElementsIncludingNull() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("x");
        set.add(null);
        assertTrue(set.contains("x"));
        assertTrue(set.contains(null));
        assertFalse(set.contains("y"));
    }

    /*---------------------- Ensure Mask/Unmask Works Through Public API ----------------------*/

    @Test
    public void testMaskUnmaskThroughPublicMethods() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());

        // The internal sentinel should never be visible via iterator or other public methods
        for (String s : set) {
            assertNull(s);
        }
    }
}
