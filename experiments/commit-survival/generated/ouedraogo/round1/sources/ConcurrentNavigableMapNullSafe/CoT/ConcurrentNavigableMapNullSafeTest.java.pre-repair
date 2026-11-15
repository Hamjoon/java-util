package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.junit.Assert.*;

public class ConcurrentNavigableMapNullSafeTest {

    private ConcurrentNavigableMapNullSafe<Integer, String> map;

    @Before
    public void setUp() {
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    /* -------------------------------------------------------------
     *  1.  Basic put / get / size behaviour (non‑null keys/values)
     * ------------------------------------------------------------- */
    @Test
    public void testPutAndGetBasic() {
        assertTrue(map.isEmpty());

        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        assertEquals(3, map.size());
        assertEquals("one", map.get(1));
        assertEquals("two", map.get(2));
        assertEquals("three", map.get(3));
        assertNull(map.get(4));
    }

    /* -------------------------------------------------------------
     *  2.  Null‑key handling (sentinel logic)
     * ------------------------------------------------------------- */
    @Test
    public void testNullKeyHandling() {
        map.put(null, "nullValue");
        map.put(10, "ten");
        map.put(20, "twenty");

        // size includes the null key
        assertEquals(3, map.size());

        // retrieval works
        assertEquals("nullValue", map.get(null));
        assertEquals("ten", map.get(10));

        // ordering: null is considered the *last* key in the ascending view
        assertEquals(Integer.valueOf(10), map.firstKey());
        assertEquals(null, map.lastKey());

        // descending view: null becomes the *first* key
        NavigableSet<Integer> descending = map.descendingKeySet();
        Iterator<Integer> it = descending.iterator();
        assertTrue(it.hasNext());
        assertNull(it.next());               // null first
        assertEquals(Integer.valueOf(20), it.next());
        assertEquals(Integer.valueOf(10), it.next());
    }

    /* -------------------------------------------------------------
     *  3.  Natural ordering (no custom comparator)
     * ------------------------------------------------------------- */
    @Test
    public void testNaturalOrdering() {
        map.put(5, "five");
        map.put(1, "one");
        map.put(3, "three");

        NavigableSet<Integer> keys = map.navigableKeySet();
        Iterator<Integer> it = keys.iterator();

        assertEquals(Integer.valueOf(1), it.next());
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(5), it.next());
        assertFalse(it.hasNext());
    }

    /* -------------------------------------------------------------
     *  4.  Custom comparator (reverse order)
     * ------------------------------------------------------------- */
    @Test
    public void testCustomComparator() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> revMap =
                new ConcurrentNavigableMapNullSafe<>(reverse);

        revMap.put(1, "one");
        revMap.put(2, "two");
        revMap.put(3, "three");

        // natural order is reversed, so firstKey should be the greatest key
        assertEquals(Integer.valueOf(3), revMap.firstKey());
        assertEquals(Integer.valueOf(1), revMap.lastKey());

        // descending view flips the order again – should look like natural order
        NavigableSet<Integer> desc = revMap.descendingKeySet();
        Iterator<Integer> it = desc.iterator();
        assertEquals(Integer.valueOf(1), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(3), it.next());
        assertFalse(it.hasNext());
    }

    /* -------------------------------------------------------------
     *  5.  Sub‑map, head‑map and tail‑map with null boundaries
     * ------------------------------------------------------------- */
    @Test
    public void testSubMapBoundaries() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(null, "nullVal");

        // subMap from 1 (inclusive) to 3 (exclusive) → {1,2}
        ConcurrentNavigableMap<Integer, String> sub = map.subMap(1, true, 3, false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(1));
        assertTrue(sub.containsKey(2));
        assertFalse(sub.containsKey(3));
        assertFalse(sub.containsKey(null));

        // headMap up to null (exclusive) → all non‑null keys
        ConcurrentNavigableMap<Integer, String> head = map.headMap(null, false);
        assertEquals(3, head.size());
        assertTrue(head.containsKey(1));
        assertTrue(head.containsKey(2));
        assertTrue(head.containsKey(3));
        assertFalse(head.containsKey(null));

        // tailMap from null (inclusive) → only the null key
        ConcurrentNavigableMap<Integer, String> tail = map.tailMap(null, true);
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
    }

    /* -------------------------------------------------------------
     *  6.  Navigational entry methods (lower / floor / ceiling / higher)
     * ------------------------------------------------------------- */
    @Test
    public void testNavigationalEntryMethods() {
        map.put(10, "ten");
        map.put(20, "twenty");
        map.put(30, "thirty");
        map.put(null, "nullVal");

        // lowerEntry(20) → 10
        Map.Entry<Integer, String> lower = map.lowerEntry(20);
        assertNotNull(lower);
        assertEquals(Integer.valueOf(10), lower.getKey());
        assertEquals("ten", lower.getValue());

        // floorEntry(20) → 20
        Map.Entry<Integer, String> floor = map.floorEntry(20);
        assertNotNull(floor);
        assertEquals(Integer.valueOf(20), floor.getKey());

        // ceilingEntry(25) → 30
        Map.Entry<Integer, String> ceiling = map.ceilingEntry(25);
        assertNotNull(ceiling);
        assertEquals(Integer.valueOf(30), ceiling.getKey());

        // higherEntry(30) → null (because null key is considered greater)
        Map.Entry<Integer, String> higher = map.higherEntry(30);
        assertNull(higher);

        // higherEntry(null) → null (null is the greatest)
        assertNull(map.higherEntry(null));

        // lowerKey(null) → 30 (largest non‑null key)
        assertEquals(Integer.valueOf(30), map.lowerKey(null));
    }

    /* -------------------------------------------------------------
     *  7.  First / last entry and poll operations
     * ------------------------------------------------------------- */
    @Test
    public void testFirstLastEntryAndPoll() {
        map.put(5, "five");
        map.put(15, "fifteen");
        map.put(null, "nullVal");

        // firstEntry → key 5
        Map.Entry<Integer, String> first = map.firstEntry();
        assertNotNull(first);
        assertEquals(Integer.valueOf(5), first.getKey());

        // lastEntry → null key
        Map.Entry<Integer, String> last = map.lastEntry();
        assertNotNull(last);
        assertNull(last.getKey());

        // pollFirstEntry removes 5
        Map.Entry<Integer, String> polledFirst = map.pollFirstEntry();
        assertEquals(Integer.valueOf(5), polledFirst.getKey());
        assertFalse(map.containsKey(5));

        // pollLastEntry removes null key
        Map.Entry<Integer, String> polledLast = map.pollLastEntry();
        assertNull(polledLast.getKey());
        assertFalse(map.containsKey(null));
    }

    /* -------------------------------------------------------------
     *  8.  KeySet basic behaviour and iterator ordering
     * ------------------------------------------------------------- */
    @Test
    public void testKeySetBasicOperations() {
        map.put(2, "two");
        map.put(1, "one");
        map.put(3, "three");
        map.put(null, "nullVal");

        NavigableSet<Integer> ks = map.keySet();

        // size & contains
        assertEquals(4, ks.size());
        assertTrue(ks.contains(1));
        assertTrue(ks.contains(null));

        // iterator order (ascending, null last)
        Iterator<Integer> it = ks.iterator();
        assertEquals(Integer.valueOf(1), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(3), it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());

        // remove via keySet
        assertTrue(ks.remove(2));
        assertFalse(map.containsKey(2));
        assertEquals(3, ks.size());

        // clear via keySet
        ks.clear();
        assertTrue(map.isEmpty());
    }

    /* -------------------------------------------------------------
     *  9.  KeySet navigable methods (lower, floor, ceiling, higher)
     * ------------------------------------------------------------- */
    @Test
    public void testKeySetNavigableMethods() {
        map.put(10, "ten");
        map.put(20, "twenty");
        map.put(30, "thirty");
        map.put(null, "nullVal");

        NavigableSet<Integer> ks = map.keySet();

        // lower, floor, ceiling, higher with a concrete key
        assertEquals(Integer.valueOf(10), ks.lower(20));
        assertEquals(Integer.valueOf(20), ks.floor(20));
        assertEquals(Integer.valueOf(20), ks.ceiling(20));
        assertEquals(Integer.valueOf(30), ks.higher(20));

        // with null key (null is greatest in ascending view)
        assertEquals(Integer.valueOf(30), ks.lower(null));
        assertEquals(null, ks.floor(null));
        assertEquals(null, ks.ceiling(null));
        assertNull(ks.higher(null));
    }

    /* -------------------------------------------------------------
     * 10.  KeySet pollFirst / pollLast
     * ------------------------------------------------------------- */
    @Test
    public void testKeySetPollFirstLast() {
        map.put(100, "hundred");
        map.put(200, "twoHundred");
        map.put(null, "nullVal");

        NavigableSet<Integer> ks = map.keySet();

        // pollFirst returns the smallest non‑null key
        Integer first = ks.pollFirst();
        assertEquals(Integer.valueOf(100), first);
        assertFalse(map.containsKey(100));

        // pollLast returns the null key (greatest)
        Integer last = ks.pollLast();
        assertNull(last);
        assertFalse(map.containsKey(null));
    }

    /* -------------------------------------------------------------
     * 11.  DescendingMap order verification
     * ------------------------------------------------------------- */
    @Test
    public void testDescendingMapOrder() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(null, "nullVal");

        ConcurrentNavigableMap<Integer, String> desc = map.descendingMap();

        // In descending view, null is first
        assertNull(desc.firstKey());
        assertEquals(Integer.valueOf(3), desc.lastKey());

        // descendingKeySet iterator should start with null
        Iterator<Integer> it = desc.descendingKeySet().iterator();
        assertEquals(Integer.valueOf(1), it.next()); // because descendingKeySet() of descendingMap is the original ascending order
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(3), it.next());
        assertNull(it.next());
    }

    /* -------------------------------------------------------------
     * 12.  Edge case – mixing incomparable key types without a comparator
     * ------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testMixedKeyTypesWithoutComparatorThrows() {
        // No comparator supplied → natural ordering required.
        // Mixing Integer and String should cause a ClassCastException on the first put
        ConcurrentNavigableMapNullSafe<Object, String> mixedMap = new ConcurrentNavigableMapNullSafe<>();
        mixedMap.put(1, "int");
        mixedMap.put("a", "string"); // should throw
    }

    /* -------------------------------------------------------------
     * 13.  Edge case – custom comparator handling of null sentinel
     * ------------------------------------------------------------- */
    @Test
    public void testCustomComparatorWithNullKey() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> revMap = new ConcurrentNavigableMapNullSafe<>(reverse);

        revMap.put(5, "five");
        revMap.put(10, "ten");
        revMap.put(null, "nullVal");

        // In reverse order, the greatest key (according to comparator) is 5,
        // so firstKey should be 5, and lastKey should be null (still considered greatest overall)
        assertEquals(Integer.valueOf(10), revMap.firstKey()); // because reverse order: 10 > 5
        assertNull(revMap.lastKey());

        // descendingMap flips the order again → natural order, null still last
        NavigableSet<Integer> descKeys = revMap.descendingKeySet();
        Iterator<Integer> it = descKeys.iterator();
        assertEquals(Integer.valueOf(5), it.next());
        assertEquals(Integer.valueOf(10), it.next());
        assertNull(it.next());
    }

    /* -------------------------------------------------------------
     * 14.  Verify that comparator() returns the user‑supplied comparator
     * ------------------------------------------------------------- */
    @Test
    public void testComparatorMethod() {
        Comparator<Integer> comp = Comparator.naturalOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> cmpMap = new ConcurrentNavigableMapNullSafe<>(comp);
        assertSame(comp, cmpMap.comparator());
    }

    /* -------------------------------------------------------------
     * 15.  Verify wrapEntry returns an entry with unmasked key/value
     * ------------------------------------------------------------- */
    @Test
    public void testWrapEntryUnmasking() {
        map.put(null, "nullValue");
        Map.Entry<Integer, String> entry = map.firstEntry(); // firstEntry should be the smallest non‑null key
        // there is only a null key, so firstEntry is null; add another key to test
        map.put(1, "one");
        entry = map.firstEntry();
        assertNotNull(entry);
        assertEquals(Integer.valueOf(1), entry.getKey());
        assertEquals("one", entry.getValue());

        // modify via setValue and ensure unmasking works
        String old = entry.setValue("ONE");
        assertEquals("one", old);
        assertEquals("ONE", map.get(1));
    }
}
