package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.junit.Assert.*;

public class ConcurrentNavigableMapNullSafeTest {

    private ConcurrentNavigableMapNullSafe<Integer, String> map;
    private ConcurrentNavigableMapNullSafe<Integer, String> mapWithComparator;

    @Before
    public void setUp() {
        // natural ordering
        map = new ConcurrentNavigableMapNullSafe<>();

        // custom comparator (reverse order)
        Comparator<Integer> reverse = Collections.reverseOrder();
        mapWithComparator = new ConcurrentNavigableMapNullSafe<>(reverse);
    }

    /* -------------------------------------------------
       1. Constructor and basic put/get (including nulls)
       ------------------------------------------------- */
    @Test
    public void testPutAndGet_NullKeyAndNullValue() {
        // null key, non‑null value
        map.put(null, "nullValue");
        assertEquals("nullValue", map.get(null));
        assertTrue(map.containsKey(null));

        // non‑null key, null value
        map.put(1, null);
        assertNull(map.get(1));
        assertTrue(map.containsValue(null));

        // both null
        map.put(null, null);
        assertNull(map.get(null));
        assertTrue(map.containsKey(null));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(map.isEmpty());
        map.put(1, "a");
        map.put(2, "b");
        assertEquals(2, map.size());
        assertFalse(map.isEmpty());
    }

    /* -------------------------------------------------
       2. Comparator handling
       ------------------------------------------------- */
    @Test
    public void testComparator_NaturalOrdering() {
        assertNull(map.comparator()); // natural ordering -> null comparator
    }

    @Test
    public void testComparator_CustomComparator() {
        Comparator<Integer> cmp = mapWithComparator.comparator();
        assertNotNull(cmp);
        // reverse order: compare 1 and 2 => positive (1 > 2 in reverse)
        assertTrue(cmp.compare(1, 2) > 0);
    }

    /* -------------------------------------------------
       3. Navigational methods – basic ordering
       ------------------------------------------------- */
    @Test
    public void testFirstAndLastKey_WithNullKey() {
        map.put(10, "ten");
        map.put(5, "five");
        map.put(null, "nullKey");

        // ascending view: null key is last
        assertEquals(Integer.valueOf(5), map.firstKey());
        assertNull(map.lastKey());

        // descending view: null key becomes first
        ConcurrentNavigableMap<Integer, String> desc = map.descendingMap();
        assertNull(desc.firstKey());
        assertEquals(Integer.valueOf(10), desc.lastKey());
    }

    @Test
    public void testLowerHigherFloorCeiling_StandardKeys() {
        map.put(1, "one");
        map.put(3, "three");
        map.put(5, "five");

        // lower / higher
        assertNull(map.lowerKey(1));
        assertEquals(Integer.valueOf(1), map.higherKey(1));

        assertEquals(Integer.valueOf(3), map.lowerKey(5));
        assertNull(map.higherKey(5));

        // floor / ceiling
        assertEquals(Integer.valueOf(1), map.floorKey(1));
        assertEquals(Integer.valueOf(1), map.ceilingKey(1));

        assertEquals(Integer.valueOf(3), map.floorKey(4));
        assertEquals(Integer.valueOf(5), map.ceilingKey(4));
    }

    @Test
    public void testNavigationalMethods_WithNullKey() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(null, "nullKey");

        // lowerKey(null) should be the greatest non‑null key (2)
        assertEquals(Integer.valueOf(2), map.lowerKey(null));

        // floorKey(null) is null itself
        assertNull(map.floorKey(null));

        // ceilingKey(null) is null itself
        assertNull(map.ceilingKey(null));

        // higherKey(null) -> null (nothing greater than null in ascending view)
        assertNull(map.higherKey(null));
    }

    @Test
    public void testEntryMethods() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(null, "nullKey");

        // firstEntry / lastEntry
        assertEquals(Integer.valueOf(1), map.firstEntry().getKey());
        assertEquals("one", map.firstEntry().getValue());

        assertNull(map.lastEntry().getKey());
        assertEquals("nullKey", map.lastEntry().getValue());

        // pollFirstEntry removes the first entry
        Map.Entry<Integer, String> polledFirst = map.pollFirstEntry();
        assertEquals(Integer.valueOf(1), polledFirst.getKey());
        assertEquals(2, map.size());
        assertFalse(map.containsKey(1));

        // pollLastEntry removes the null key entry
        Map.Entry<Integer, String> polledLast = map.pollLastEntry();
        assertNull(polledLast.getKey());
        assertEquals("nullKey", polledLast.getValue());
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());
    }

    /* -------------------------------------------------
       4. Sub‑map, head‑map, tail‑map (inclusive / exclusive)
       ------------------------------------------------- */
    @Test
    public void testSubMap_InclusiveExclusive() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(null, "nullKey");

        // subMap(1, true, 3, false) => keys 1,2
        ConcurrentNavigableMap<Integer, String> sub = map.subMap(1, true, 3, false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(1));
        assertTrue(sub.containsKey(2));
        assertFalse(sub.containsKey(3));
        assertFalse(sub.containsKey(null));
    }

    @Test
    public void testHeadMap_Inclusive() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(null, "nullKey");

        // headMap(null, true) includes all keys (null is last)
        ConcurrentNavigableMap<Integer, String> head = map.headMap(null, true);
        assertEquals(3, head.size());
        assertTrue(head.containsKey(null));
    }

    @Test
    public void testTailMap_Exclusive() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(null, "nullKey");

        // tailMap(2, false) => keys >2 i.e., 3 and null
        ConcurrentNavigableMap<Integer, String> tail = map.tailMap(2, false);
        assertEquals(2, tail.size());
        assertTrue(tail.containsKey(3));
        assertTrue(tail.containsKey(null));
        assertFalse(tail.containsKey(2));
    }

    /* -------------------------------------------------
       5. KeySet and NavigableSet behaviour
       ------------------------------------------------- */
    @Test
    public void testKeySet_ContainsAndRemove_NullKey() {
        map.put(1, "one");
        map.put(null, "nullKey");

        NavigableSet<Integer> keySet = map.keySet();
        assertTrue(keySet.contains(1));
        assertTrue(keySet.contains(null));

        // remove null key via the set
        assertTrue(keySet.remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());
    }

    @Test
    public void testKeySet_LowerFloorCeilingHigher() {
        map.put(10, "ten");
        map.put(20, "twenty");
        map.put(null, "nullKey");

        NavigableSet<Integer> ks = map.keySet();

        // lower/floor/ceiling/higher relative to 15
        assertEquals(Integer.valueOf(10), ks.lower(15));
        assertEquals(Integer.valueOf(10), ks.floor(15));
        assertEquals(Integer.valueOf(20), ks.ceiling(15));
        assertEquals(Integer.valueOf(20), ks.higher(15));

        // relative to null (null is last)
        assertEquals(Integer.valueOf(20), ks.lower(null));
        assertNull(ks.higher(null));
    }

    @Test
    public void testKeySet_PollFirstAndPollLast() {
        map.put(5, "five");
        map.put(10, "ten");
        map.put(null, "nullKey");

        NavigableSet<Integer> ks = map.keySet();

        // pollFirst removes smallest (5)
        Integer firstPolled = ks.pollFirst();
        assertEquals(Integer.valueOf(5), firstPolled);
        assertFalse(map.containsKey(5));

        // pollLast removes null key (last in ascending order)
        Integer lastPolled = ks.pollLast();
        assertNull(lastPolled);
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());
    }

    @Test
    public void testKeySet_DescendingIterator_Order() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(null, "nullKey");

        NavigableSet<Integer> ksDesc = map.descendingKeySet();
        Iterator<Integer> it = ksDesc.iterator();

        // Expected order: null, 2, 1
        assertTrue(it.hasNext());
        assertNull(it.next());

        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(2), it.next());

        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(1), it.next());

        assertFalse(it.hasNext());
    }

    @Test
    public void testKeySet_SubSet_HeadSet_TailSet() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(null, "nullKey");

        NavigableSet<Integer> ks = map.keySet();

        // Subset 1 (inclusive) to 2 (exclusive) => {1}
        NavigableSet<Integer> sub = ks.subSet(1, true, 2, false);
        assertEquals(1, sub.size());
        assertTrue(sub.contains(1));

        // HeadSet up to 2 (inclusive) => {1,2}
        NavigableSet<Integer> head = ks.headSet(2, true);
        assertEquals(2, head.size());
        assertTrue(head.contains(1));
        assertTrue(head.contains(2));

        // TailSet from 2 (exclusive) => {3, null}
        NavigableSet<Integer> tail = ks.tailSet(2, false);
        assertEquals(2, tail.size());
        assertTrue(tail.contains(3));
        assertTrue(tail.contains(null));
    }

    /* -------------------------------------------------
       6. Edge cases – empty map operations
       ------------------------------------------------- */
    @Test
    public void testOperationsOnEmptyMap() {
        assertNull(map.firstKey());
        assertNull(map.lastKey());
        assertNull(map.lowerKey(1));
        assertNull(map.higherKey(1));
        assertNull(map.pollFirstEntry());
        assertNull(map.pollLastEntry());

        NavigableSet<Integer> ks = map.keySet();
        assertNull(ks.pollFirst());
        assertNull(ks.pollLast());
        assertFalse(ks.iterator().hasNext());
    }

    /* -------------------------------------------------
       7. Verify that custom comparator is respected in ordering
       ------------------------------------------------- */
    @Test
    public void testCustomComparatorOrdering() {
        mapWithComparator.put(1, "one");
        mapWithComparator.put(2, "two");
        mapWithComparator.put(null, "nullKey");

        // reverse order: firstKey should be the greatest (2), lastKey should be null (null is considered greatest still)
        assertEquals(Integer.valueOf(2), mapWithComparator.firstKey());
        assertNull(mapWithComparator.lastKey());

        // descending view of a reverse‑ordered map should give natural order
        NavigableSet<Integer> descSet = mapWithComparator.descendingKeySet();
        Iterator<Integer> it = descSet.iterator();
        // Expected natural order: 1, 2, null
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(1), it.next());
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(2), it.next());
        assertTrue(it.hasNext());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }
}
