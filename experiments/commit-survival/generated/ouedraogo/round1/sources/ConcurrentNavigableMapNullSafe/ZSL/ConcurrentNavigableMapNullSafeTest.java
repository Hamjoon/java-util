package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.junit.Assert.*;

public class ConcurrentNavigableMapNullSafeTest {

    private ConcurrentNavigableMapNullSafe<Integer, String> map;

    @Before
    public void setUp() {
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    /*** Basic put/get including null key and null value ***/
    @Test
    public void testPutAndGetWithNullKeyAndValue() {
        // regular entry
        assertNull(map.put(1, "one"));
        assertEquals("one", map.get(1));

        // null value
        assertNull(map.put(2, null));
        assertNull(map.get(2));

        // null key with non‑null value
        assertNull(map.put(null, "nullKey"));
        assertEquals("nullKey", map.get(null));

        // overwrite null key
        assertEquals("nullKey", map.put(null, "newNull"));
        assertEquals("newNull", map.get(null));

        // size should be 3
        assertEquals(3, map.size());
    }

    /*** Verify ordering – null key is considered last in the natural (ascending) view ***/
    @Test
    public void testOrderingWithNullKey() {
        map.put(10, "ten");
        map.put(5, "five");
        map.put(null, "nullKey");

        // natural order first/last
        assertEquals(Integer.valueOf(5), map.firstKey());
        assertEquals(null, map.lastKey());

        // descending map reverses ordering – null becomes first
        ConcurrentNavigableMap<Integer, String> desc = map.descendingMap();
        assertEquals(null, desc.firstKey());
        assertEquals(Integer.valueOf(10), desc.lastKey());
    }

    /*** Test all navigational methods for both normal and null keys ***/
    @Test
    public void testNavigationalMethods() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(null, "nullKey");

        // lower / floor / ceiling / higher for regular keys
        assertEquals(Integer.valueOf(1), map.lowerKey(2));
        assertEquals(Integer.valueOf(2), map.floorKey(2));
        assertEquals(Integer.valueOf(2), map.ceilingKey(2));
        assertEquals(Integer.valueOf(3), map.higherKey(2));

        // lower / floor / ceiling / higher for null key (treated as greatest)
        assertEquals(Integer.valueOf(3), map.lowerKey(null));
        assertEquals(null, map.floorKey(null));
        assertEquals(null, map.ceilingKey(null));
        assertNull(map.higherKey(null));

        // entry variants
        assertEquals("one", map.lowerEntry(2).getValue());
        assertEquals("two", map.floorEntry(2).getValue());
        assertEquals("two", map.ceilingEntry(2).getValue());
        assertEquals("three", map.higherEntry(2).getValue());

        // first / last entries
        assertEquals("one", map.firstEntry().getValue());
        assertEquals("nullKey", map.lastEntry().getValue());

        // pollFirstEntry removes the smallest key (1)
        Map.Entry<Integer, String> polledFirst = map.pollFirstEntry();
        assertEquals(Integer.valueOf(1), polledFirst.getKey());
        assertEquals("one", polledFirst.getValue());
        assertFalse(map.containsKey(1));

        // pollLastEntry removes the null key (now the greatest)
        Map.Entry<Integer, String> polledLast = map.pollLastEntry();
        assertEquals(null, polledLast.getKey());
        assertEquals("nullKey", polledLast.getValue());
        assertFalse(map.containsKey(null));
    }

    /*** Test subMap, headMap and tailMap views, especially with null as boundary ***/
    @Test
    public void testSubMapViews() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(null, "nullKey");

        // subMap from 1 (inclusive) to null (exclusive) should contain 1,2,3
        ConcurrentNavigableMap<Integer, String> sub = map.subMap(1, true, null, false);
        assertEquals(3, sub.size());
        assertTrue(sub.containsKey(1));
        assertTrue(sub.containsKey(2));
        assertTrue(sub.containsKey(3));
        assertFalse(sub.containsKey(null));

        // headMap up to 2 (exclusive) should contain only 1
        ConcurrentNavigableMap<Integer, String> head = map.headMap(2, false);
        assertEquals(1, head.size());
        assertTrue(head.containsKey(1));
        assertFalse(head.containsKey(2));

        // headMap up to null (inclusive) should contain all entries
        ConcurrentNavigableMap<Integer, String> headAll = map.headMap(null, true);
        assertEquals(4, headAll.size());

        // tailMap from 2 (inclusive) should contain 2,3,null
        ConcurrentNavigableMap<Integer, String> tail = map.tailMap(2, true);
        assertEquals(3, tail.size());
        assertTrue(tail.containsKey(2));
        assertTrue(tail.containsKey(3));
        assertTrue(tail.containsKey(null));
    }

    /*** Test keySet implementation, including navigable operations ***/
    @Test
    public void testKeySetOperations() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(null, "nullKey");

        NavigableSet<Integer> ks = map.navigableKeySet();

        // size and contains
        assertEquals(4, ks.size());
        assertTrue(ks.contains(2));
        assertTrue(ks.contains(null));

        // lower/floor/ceiling/higher on key set
        assertEquals(Integer.valueOf(1), ks.lower(2));
        assertEquals(Integer.valueOf(2), ks.floor(2));
        assertEquals(Integer.valueOf(2), ks.ceiling(2));
        assertEquals(Integer.valueOf(3), ks.higher(2));

        // lower/floor/ceiling/higher with null key
        assertEquals(Integer.valueOf(3), ks.lower(null));
        assertNull(ks.floor(null));
        assertNull(ks.ceiling(null));
        assertNull(ks.higher(null));

        // pollFirst removes smallest (1)
        assertEquals(Integer.valueOf(1), ks.pollFirst());
        assertFalse(ks.contains(1));
        assertEquals(3, ks.size());

        // pollLast removes null key (greatest)
        assertEquals(null, ks.pollLast());
        assertFalse(ks.contains(null));
        assertEquals(2, ks.size());

        // first/last after removals
        assertEquals(Integer.valueOf(2), ks.first());
        assertEquals(Integer.valueOf(3), ks.last());

        // descendingSet order: should be 3 then 2
        NavigableSet<Integer> descSet = ks.descendingSet();
        Iterator<Integer> it = descSet.iterator();
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertFalse(it.hasNext());

        // subSet view
        NavigableSet<Integer> subSet = ks.subSet(2, true, 3, true);
        assertEquals(2, subSet.size());
        assertTrue(subSet.contains(2));
        assertTrue(subSet.contains(3));

        // headSet view (to 3 exclusive)
        NavigableSet<Integer> headSet = ks.headSet(3, false);
        assertEquals(1, headSet.size());
        assertTrue(headSet.contains(2));

        // tailSet view (from 2 inclusive)
        NavigableSet<Integer> tailSet = ks.tailSet(2, true);
        assertEquals(2, tailSet.size());
        assertTrue(tailSet.contains(2));
        assertTrue(tailSet.contains(3));

        // remove via keySet
        assertTrue(ks.remove(2));
        assertFalse(ks.contains(2));
        assertEquals(1, ks.size());
    }

    /*** Verify that the original comparator (if supplied) is returned unchanged ***/
    @Test
    public void testComparatorPreserved() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> mapWithComp = new ConcurrentNavigableMapNullSafe<>(reverse);
        assertSame(reverse, mapWithComp.comparator());

        // natural ordering map should return null comparator
        assertNull(map.comparator());
    }

    /*** Ensure that null values are handled correctly (mask/unmask) ***/
    @Test
    public void testNullValuesSupported() {
        map.put(1, null);
        map.put(null, null);

        assertTrue(map.containsKey(1));
        assertTrue(map.containsKey(null));

        assertNull(map.get(1));
        assertNull(map.get(null));

        // entry reflects null values
        Map.Entry<Integer, String> e1 = map.firstEntry(); // key 1
        assertEquals(Integer.valueOf(1), e1.getKey());
        assertNull(e1.getValue());

        Map.Entry<Integer, String> e2 = map.lastEntry(); // null key
        assertNull(e2.getKey());
        assertNull(e2.getValue());
    }
}
