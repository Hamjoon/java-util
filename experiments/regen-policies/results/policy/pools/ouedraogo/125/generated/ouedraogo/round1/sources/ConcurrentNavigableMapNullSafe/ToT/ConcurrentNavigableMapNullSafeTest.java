package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentNavigableMapNullSafe}.
 *
 * The tests are grouped by logical feature (construction, null‑key handling,
 * navigational methods, key‑set view, descending view and custom comparator).
 *
 * All objects are created through the public API – no mocking or subclassing
 * of library types is performed.
 */
public class ConcurrentNavigableMapNullSafeTest {

    private ConcurrentNavigableMapNullSafe<Integer, String> naturalMap;
    private ConcurrentNavigableMapNullSafe<Integer, String> reverseMap;

    @Before
    public void setUp() {
        // natural ordering map
        naturalMap = new ConcurrentNavigableMapNullSafe<>();

        // reverse ordering map (custom comparator)
        Comparator<Integer> reverse = (a, b) -> b.compareTo(a);
        reverseMap = new ConcurrentNavigableMapNullSafe<>(reverse);
    }

    /* --------------------------------------------------------------------- *
     *  Construction & basic map operations (including null handling)
     * --------------------------------------------------------------------- */

    @Test
    public void testEmptyMapBehaviour() {
        assertTrue(naturalMap.isEmpty());
        assertEquals(0, naturalMap.size());

        assertNull(naturalMap.firstKey());
        assertNull(naturalMap.lastKey());

        assertNull(naturalMap.firstEntry());
        assertNull(naturalMap.lastEntry());
    }

    @Test
    public void testPutAndGetWithNullKeyAndNullValue() {
        // put a regular entry
        assertNull(naturalMap.put(1, "one"));
        // put a null key with a non‑null value
        assertNull(naturalMap.put(null, "nullValue"));
        // put a null key with a null value (value null is also masked internally)
        assertEquals("nullValue", naturalMap.put(null, null));

        // verify retrieval
        assertEquals("one", naturalMap.get(1));
        assertNull(naturalMap.get(null));

        // verify size and key set contain the null key
        assertEquals(2, naturalMap.size());
        assertTrue(naturalMap.containsKey(null));
        assertTrue(naturalMap.containsKey(1));
        assertTrue(naturalMap.keySet().contains(null));
    }

    @Test
    public void testComparatorReturnedIsOriginal() {
        assertNull(naturalMap.comparator()); // natural ordering -> null
        Comparator<Integer> reverse = (a, b) -> b.compareTo(a);
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>(reverse);
        assertSame(reverse, map.comparator());
    }

    /* --------------------------------------------------------------------- *
     *  Ordering semantics – null key considered last in ascending view
     * --------------------------------------------------------------------- */

    @Test
    public void testOrderingWithNullKey_Natural() {
        naturalMap.put(2, "two");
        naturalMap.put(1, "one");
        naturalMap.put(null, "null");

        // Ascending order: 1, 2, null
        assertEquals(Integer.valueOf(1), naturalMap.firstKey());
        assertEquals(Integer.valueOf(2), naturalMap.higherKey(1));
        assertEquals(null, naturalMap.higherKey(2));
        assertEquals(null, naturalMap.lastKey());

        // descending view: null first, then 2, then 1
        ConcurrentNavigableMap<Integer, String> desc = naturalMap.descendingMap();
        assertEquals(null, desc.firstKey());
        assertEquals(Integer.valueOf(2), desc.higherKey(null));
        assertEquals(Integer.valueOf(1), desc.lowerKey(null));
    }

    @Test
    public void testOrderingWithNullKey_CustomComparator() {
        Comparator<Integer> reverse = (a, b) -> b.compareTo(a);
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>(reverse);

        map.put(1, "one");
        map.put(2, "two");
        map.put(null, "null");

        // With reverse comparator the “smallest” (first) element is 2,
        // then 1, then null (sentinel is always greatest)
        assertEquals(Integer.valueOf(2), map.firstKey());
        assertEquals(Integer.valueOf(1), map.higherKey(2));
        assertEquals(null, map.higherKey(1));
        assertEquals(null, map.lastKey());

        // descending view should flip the order again:
        ConcurrentNavigableMap<Integer, String> desc = map.descendingMap();
        assertEquals(null, desc.firstKey());
        assertEquals(Integer.valueOf(1), desc.lowerKey(null));
        assertEquals(Integer.valueOf(2), desc.lowerKey(1));
    }

    /* --------------------------------------------------------------------- *
     *  Navigational methods (lower, floor, ceiling, higher) and entry variants
     * --------------------------------------------------------------------- */

    @Test
    public void testLowerAndFloorMethods() {
        naturalMap.put(1, "one");
        naturalMap.put(2, "two");
        naturalMap.put(null, "null");

        // lowerEntry(2) → entry for 1
        Map.Entry<Integer, String> lower = naturalMap.lowerEntry(2);
        assertNotNull(lower);
        assertEquals(Integer.valueOf(1), lower.getKey());
        assertEquals("one", lower.getValue());

        // floorEntry(2) → entry for 2 itself
        Map.Entry<Integer, String> floor = naturalMap.floorEntry(2);
        assertNotNull(floor);
        assertEquals(Integer.valueOf(2), floor.getKey());

        // floorEntry(null) → entry for null (sentinel)
        assertEquals(null, naturalMap.floorKey(null));
        assertNotNull(naturalMap.floorEntry(null));
        assertEquals(null, naturalMap.floorEntry(null).getKey());

        // lowerKey(null) should be the greatest non‑null key (2)
        assertEquals(Integer.valueOf(2), naturalMap.lowerKey(null));
    }

    @Test
    public void testCeilingAndHigherMethods() {
        naturalMap.put(1, "one");
        naturalMap.put(2, "two");
        naturalMap.put(null, "null");

        // ceilingEntry(1) → entry for 1
        Map.Entry<Integer, String> ceiling = naturalMap.ceilingEntry(1);
        assertNotNull(ceiling);
        assertEquals(Integer.valueOf(1), ceiling.getKey());

        // higherEntry(1) → entry for 2
        Map.Entry<Integer, String> higher = naturalMap.higherEntry(1);
        assertNotNull(higher);
        assertEquals(Integer.valueOf(2), higher.getKey());

        // higherKey(2) → null (sentinel)
        assertEquals(null, naturalMap.higherKey(2));
    }

    @Test
    public void testFirstLastAndPollMethods() {
        naturalMap.put(1, "one");
        naturalMap.put(2, "two");
        naturalMap.put(null, "null");

        // firstEntry / lastEntry
        assertEquals(Integer.valueOf(1), naturalMap.firstEntry().getKey());
        assertEquals(null, naturalMap.lastEntry().getKey());

        // pollFirstEntry removes 1
        Map.Entry<Integer, String> polledFirst = naturalMap.pollFirstEntry();
        assertEquals(Integer.valueOf(1), polledFirst.getKey());
        assertFalse(naturalMap.containsKey(1));
        assertEquals(Integer.valueOf(2), naturalMap.firstKey());

        // pollLastEntry removes null sentinel
        Map.Entry<Integer, String> polledLast = naturalMap.pollLastEntry();
        assertEquals(null, polledLast.getKey());
        assertFalse(naturalMap.containsKey(null));
        assertEquals(Integer.valueOf(2), naturalMap.lastKey());
    }

    /* --------------------------------------------------------------------- *
     *  Sub‑map, head‑map and tail‑map views (including null‑key edge cases)
     * --------------------------------------------------------------------- */

    @Test
    public void testSubMapExcludingNullKey() {
        naturalMap.put(1, "one");
        naturalMap.put(2, "two");
        naturalMap.put(3, "three");
        naturalMap.put(null, "null");

        // subMap from 1 (inclusive) to 3 (exclusive) → should contain 1 and 2
        ConcurrentNavigableMap<Integer, String> sub = naturalMap.subMap(1, true, 3, false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(1));
        assertTrue(sub.containsKey(2));
        assertFalse(sub.containsKey(3));
        assertFalse(sub.containsKey(null));
    }

    @Test
    public void testHeadMapInclusiveFalseStopsBeforeNull() {
        naturalMap.put(1, "one");
        naturalMap.put(2, "two");
        naturalMap.put(null, "null");

        // headMap(toKey=null, inclusive=false) → should contain 1 and 2 only
        ConcurrentNavigableMap<Integer, String> head = naturalMap.headMap(null, false);
        assertEquals(2, head.size());
        assertTrue(head.containsKey(1));
        assertTrue(head.containsKey(2));
        assertFalse(head.containsKey(null));
    }

    @Test
    public void testTailMapFromNullInclusiveTrueContainsOnlyNull() {
        naturalMap.put(1, "one");
        naturalMap.put(2, "two");
        naturalMap.put(null, "null");

        // tailMap(fromKey=null, inclusive=true) → only the null entry
        ConcurrentNavigableMap<Integer, String> tail = naturalMap.tailMap(null, true);
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
        assertFalse(tail.containsKey(1));
        assertFalse(tail.containsKey(2));
    }

    /* --------------------------------------------------------------------- *
     *  KeyNavigableSet behaviour (contains, remove, lower/floor/ceiling/higher,
     *  pollFirst/pollLast, subSet/headSet/tailSet)
     * --------------------------------------------------------------------- */

    @Test
    public void testKeySetBasicOperations() {
        naturalMap.put(1, "one");
        naturalMap.put(2, "two");
        naturalMap.put(null, "null");

        NavigableSet<Integer> ks = naturalMap.navigableKeySet();

        // size and containment
        assertEquals(3, ks.size());
        assertTrue(ks.contains(1));
        assertTrue(ks.contains(null));

        // lower/floor/ceiling/higher relative to null
        assertEquals(Integer.valueOf(2), ks.lower(null));
        assertEquals(null, ks.floor(null));
        assertEquals(null, ks.ceiling(null));
        assertNull(ks.higher(null));

        // lower/floor/ceiling/higher relative to 1
        assertNull(ks.lower(1));
        assertEquals(Integer.valueOf(1), ks.floor(1));
        assertEquals(Integer.valueOf(1), ks.ceiling(1));
        assertEquals(Integer.valueOf(2), ks.higher(1));

        // pollFirst removes the smallest (1)
        assertEquals(Integer.valueOf(1), ks.pollFirst());
        assertFalse(ks.contains(1));
        assertEquals(2, ks.size());

        // pollLast removes null sentinel
        assertEquals(null, ks.pollLast());
        assertFalse(ks.contains(null));
        assertEquals(1, ks.size());
        assertEquals(Integer.valueOf(2), ks.first());
        assertEquals(Integer.valueOf(2), ks.last());

        // remove via set
        assertTrue(ks.remove(2));
        assertTrue(ks.isEmpty());
    }

    @Test
    public void testKeySetSubSetViews() {
        naturalMap.put(1, "one");
        naturalMap.put(2, "two");
        naturalMap.put(3, "three");
        naturalMap.put(null, "null");

        NavigableSet<Integer> ks = naturalMap.navigableKeySet();

        // subSet 1 (inclusive) to 3 (exclusive) → 1,2
        NavigableSet<Integer> sub = ks.subSet(1, true, 3, false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains(1));
        assertTrue(sub.contains(2));
        assertFalse(sub.contains(3));
        assertFalse(sub.contains(null));

        // headSet up to 2 (inclusive) → 1,2
        NavigableSet<Integer> head = ks.headSet(2, true);
        assertEquals(2, head.size());
        assertTrue(head.contains(1));
        assertTrue(head.contains(2));

        // tailSet from 2 (exclusive) → 3, null
        NavigableSet<Integer> tail = ks.tailSet(2, false);
        assertEquals(2, tail.size());
        assertTrue(tail.contains(3));
        assertTrue(tail.contains(null));
    }

    @Test
    public void testDescendingKeySetAndIterator() {
        naturalMap.put(1, "one");
        naturalMap.put(2, "two");
        naturalMap.put(null, "null");

        NavigableSet<Integer> descSet = naturalMap.descendingKeySet();
        Iterator<Integer> it = descSet.iterator();

        // Expected order: null, 2, 1
        assertTrue(it.hasNext());
        assertEquals(null, it.next());
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(2), it.next());
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(1), it.next());
        assertFalse(it.hasNext());
    }

    /* --------------------------------------------------------------------- *
     *  Interaction between map and its descending view (mutations reflect both)
     * --------------------------------------------------------------------- */

    @Test
    public void testMutationsThroughDescendingMap() {
        naturalMap.put(1, "one");
        naturalMap.put(2, "two");
        naturalMap.put(null, "null");

        ConcurrentNavigableMap<Integer, String> desc = naturalMap.descendingMap();

        // remove the first (null) via descending map
        desc.remove(null);
        assertFalse(naturalMap.containsKey(null));
        assertEquals(Integer.valueOf(2), naturalMap.lastKey());

        // add a new entry through descending map (adds to underlying map)
        desc.put(-1, "minusOne");
        assertTrue(naturalMap.containsKey(-1));
        assertEquals(Integer.valueOf(-1), naturalMap.firstKey());
    }

    /* --------------------------------------------------------------------- *
     *  Verify that the internal wrapper comparator correctly handles mixed
     *  key types (Integer vs String) – this exercises the fallback class‑name
     *  comparison path.
     * --------------------------------------------------------------------- */

    @Test
    public void testMixedKeyTypesOrdering() {
        // raw map with natural ordering (no comparator) – permits mixed types
        ConcurrentNavigableMapNullSafe<Object, String> mixedMap = new ConcurrentNavigableMapNullSafe<>();

        mixedMap.put(10, "int10");
        mixedMap.put("a", "stringA");
        mixedMap.put(null, "null");

        // The ordering should be: 10 (Integer) < "a" (String) < null (sentinel)
        assertEquals(10, mixedMap.firstKey());
        assertEquals("a", mixedMap.higherKey(10));
        assertEquals(null, mixedMap.higherKey("a"));
        assertEquals(null, mixedMap.lastKey());
    }

    /* --------------------------------------------------------------------- *
     *  Verify that entry#setValue correctly masks and unmasks null values
     * --------------------------------------------------------------------- */

    @Test
    public void testEntrySetValueMasking() {
        naturalMap.put(1, "one");
        Map.Entry<Integer, String> entry = naturalMap.firstEntry();
        assertEquals("one", entry.getValue());

        // set value to null – should be stored as the internal null sentinel
        entry.setValue(null);
        assertNull(naturalMap.get(1));
        assertEquals(null, entry.getValue());

        // set back to a non‑null value
        entry.setValue("ONE");
        assertEquals("ONE", naturalMap.get(1));
        assertEquals("ONE", entry.getValue());
    }
}
