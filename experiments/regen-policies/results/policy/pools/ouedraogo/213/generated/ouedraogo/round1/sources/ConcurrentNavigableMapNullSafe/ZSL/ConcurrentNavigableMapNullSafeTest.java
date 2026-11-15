package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.junit.Assert.*;

public class ConcurrentNavigableMapNullSafeTest {

    private ConcurrentNavigableMapNullSafe<String, String> map;

    @Before
    public void setUp() {
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    /*** Basic put / get operations including null keys/values ***/
    @Test
    public void testPutGetNonNull() {
        assertNull(map.put("one", "1"));
        assertEquals("1", map.get("one"));
        assertNull(map.put("two", null));
        assertNull(map.get("two"));
    }

    @Test
    public void testPutGetNullKey() {
        assertNull(map.put(null, "nullValue"));
        assertEquals("nullValue", map.get(null));

        // replace null key value
        assertEquals("nullValue", map.put(null, "newNullValue"));
        assertEquals("newNullValue", map.get(null));
    }

    /*** Ordering – null key is last in ascending, first in descending ***/
    @Test
    public void testAscendingKeyOrderWithNull() {
        map.put("b", "B");
        map.put(null, "NULL");
        map.put("a", "A");

        Iterator<String> it = map.navigableKeySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testDescendingKeyOrderWithNull() {
        map.put("b", "B");
        map.put(null, "NULL");
        map.put("a", "A");

        Iterator<String> it = map.descendingKeySet().iterator();
        assertEquals(null, it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    /*** Comparator handling – custom comparator with null key ordering ***/
    @Test
    public void testCustomComparator() {
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<String, Integer> customMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        customMap.put("a", 1);
        customMap.put("c", 3);
        customMap.put(null, 0);
        customMap.put("b", 2);

        // natural order is reversed, but null still last in ascending view
        Iterator<String> ascIt = customMap.navigableKeySet().iterator();
        assertEquals("c", ascIt.next());
        assertEquals("b", ascIt.next());
        assertEquals("a", ascIt.next());
        assertEquals(null, ascIt.next());

        // descending view should have null first
        Iterator<String> descIt = customMap.descendingKeySet().iterator();
        assertEquals(null, descIt.next());
        assertEquals("a", descIt.next());
        assertEquals("b", descIt.next());
        assertEquals("c", descIt.next());

        // comparator method should return the original comparator
        assertSame(reverse, customMap.comparator());
    }

    /*** SubMap, HeadMap, TailMap behavior with null sentinel ***/
    @Test
    public void testSubMapExcludesNullWhenUpperBoundIsNullExclusive() {
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");
        map.put(null, "null");

        ConcurrentNavigableMap<String, String> sub = map.subMap("2", true, null, false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("2"));
        assertTrue(sub.containsKey("3"));
        assertFalse(sub.containsKey(null));
    }

    @Test
    public void testHeadMapExcludesUpperBound() {
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");
        map.put(null, "null");

        ConcurrentNavigableMap<String, String> head = map.headMap("2", false);
        assertEquals(1, head.size());
        assertTrue(head.containsKey("1"));
        assertFalse(head.containsKey("2"));
        assertFalse(head.containsKey(null));
    }

    @Test
    public void testTailMapIncludesFromKey() {
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");
        map.put(null, "null");

        ConcurrentNavigableMap<String, String> tail = map.tailMap("2", true);
        assertEquals(3, tail.size());
        assertTrue(tail.containsKey("2"));
        assertTrue(tail.containsKey("3"));
        assertTrue(tail.containsKey(null));
    }

    /*** Navigational entry methods ***/
    @Test
    public void testLowerAndHigherEntries() {
        map.put("a", "A");
        map.put("c", "C");
        map.put(null, "NULL");
        map.put("b", "B");

        // lowerEntry of "b" should be "a"
        Map.Entry<String, String> lower = map.lowerEntry("b");
        assertEquals("a", lower.getKey());
        assertEquals("A", lower.getValue());

        // higherEntry of "b" should be "c"
        Map.Entry<String, String> higher = map.higherEntry("b");
        assertEquals("c", higher.getKey());
        assertEquals("C", higher.getValue());

        // lowerEntry of null should be "c" (last non‑null)
        Map.Entry<String, String> lowerNull = map.lowerEntry(null);
        assertEquals("c", lowerNull.getKey());

        // higherEntry of null should be null (null is greatest)
        assertNull(map.higherEntry(null));
    }

    @Test
    public void testFloorAndCeilingEntries() {
        map.put("a", "A");
        map.put("c", "C");
        map.put(null, "NULL");
        map.put("b", "B");

        // floor of "b" is "b"
        Map.Entry<String, String> floor = map.floorEntry("b");
        assertEquals("b", floor.getKey());

        // ceiling of "b" is "b"
        Map.Entry<String, String> ceiling = map.ceilingEntry("b");
        assertEquals("b", ceiling.getKey());

        // floor of null is null (null key itself)
        Map.Entry<String, String> floorNull = map.floorEntry(null);
        assertEquals(null, floorNull.getKey());

        // ceiling of null is null
        Map.Entry<String, String> ceilingNull = map.ceilingEntry(null);
        assertEquals(null, ceilingNull.getKey());
    }

    @Test
    public void testFirstAndLastEntry() {
        map.put("a", "A");
        map.put("c", "C");
        map.put(null, "NULL");
        map.put("b", "B");

        Map.Entry<String, String> first = map.firstEntry();
        assertEquals("a", first.getKey());

        Map.Entry<String, String> last = map.lastEntry();
        assertEquals(null, last.getKey());
    }

    @Test
    public void testPollFirstAndLastEntry() {
        map.put("a", "A");
        map.put("c", "C");
        map.put(null, "NULL");
        map.put("b", "B");

        Map.Entry<String, String> polledFirst = map.pollFirstEntry();
        assertEquals("a", polledFirst.getKey());
        assertFalse(map.containsKey("a"));

        Map.Entry<String, String> polledLast = map.pollLastEntry();
        assertEquals(null, polledLast.getKey());
        assertFalse(map.containsKey(null));

        // remaining keys should be "b" and "c"
        assertEquals(2, map.size());
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
    }

    /*** descendingMap view and its keySet ***/
    @Test
    public void testDescendingMapKeySetOrder() {
        map.put("x", "X");
        map.put("y", "Y");
        map.put(null, "NULL");
        map.put("z", "Z");

        ConcurrentNavigableMap<String, String> desc = map.descendingMap();
        Iterator<String> it = desc.navigableKeySet().iterator();
        assertEquals(null, it.next());
        assertEquals("z", it.next());
        assertEquals("y", it.next());
        assertEquals("x", it.next());
        assertFalse(it.hasNext());
    }

    /*** KeyNavigableSet functionality ***/
    @Test
    public void testKeySetContainsAndRemove() {
        map.put("k1", "v1");
        map.put(null, "nullV");
        map.put("k2", "v2");

        NavigableSet<String> keySet = map.navigableKeySet();

        assertTrue(keySet.contains("k1"));
        assertTrue(keySet.contains(null));
        assertFalse(keySet.contains("missing"));

        assertTrue(keySet.remove("k1"));
        assertFalse(map.containsKey("k1"));
        assertEquals(2, map.size());

        assertTrue(keySet.remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());
    }

    @Test
    public void testKeySetNavigationalMethods() {
        map.put("a", "A");
        map.put("c", "C");
        map.put(null, "NULL");
        map.put("b", "B");

        NavigableSet<String> ks = map.navigableKeySet();

        assertEquals("a", ks.first());
        assertEquals(null, ks.last());

        assertEquals("b", ks.lower(null));
        assertEquals("c", ks.higher("b"));
        assertEquals("b", ks.floor("b"));
        assertEquals("c", ks.ceiling("b"));

        // pollFirst should remove "a"
        assertEquals("a", ks.pollFirst());
        assertFalse(map.containsKey("a"));
        // pollLast should remove null
        assertEquals(null, ks.pollLast());
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testKeySetSubSetHeadSetTailSet() {
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");
        map.put(null, "null");

        NavigableSet<String> ks = map.navigableKeySet();

        // subSet from "1" inclusive to "3" exclusive -> ["1","2"]
        NavigableSet<String> sub = ks.subSet("1", true, "3", false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains("1"));
        assertTrue(sub.contains("2"));
        assertFalse(sub.contains("3"));
        assertFalse(sub.contains(null));

        // headSet up to "2" exclusive -> ["1"]
        NavigableSet<String> head = ks.headSet("2", false);
        assertEquals(1, head.size());
        assertTrue(head.contains("1"));

        // tailSet from "2" inclusive -> ["2","3",null]
        NavigableSet<String> tail = ks.tailSet("2", true);
        assertEquals(3, tail.size());
        assertTrue(tail.contains("2"));
        assertTrue(tail.contains("3"));
        assertTrue(tail.contains(null));
    }

    @Test
    public void testDescendingSetFromKeySet() {
        map.put("a", "A");
        map.put("b", "B");
        map.put(null, "NULL");

        NavigableSet<String> ks = map.navigableKeySet();
        NavigableSet<String> descSet = ks.descendingSet();

        Iterator<String> it = descSet.iterator();
        assertEquals(null, it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    /*** Ensure that the map reports its size and emptiness correctly ***/
    @Test
    public void testSizeAndEmpty() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("x", "X");
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        map.remove("x");
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /*** Test entrySet behavior (unmasked keys/values) ***/
    @Test
    public void testEntrySetUnmasking() {
        map.put(null, null);
        map.put("k", "v");

        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(2, entries.size());

        boolean foundNull = false, foundKV = false;
        for (Map.Entry<String, String> e : entries) {
            if (e.getKey() == null) {
                assertNull(e.getValue());
                foundNull = true;
            } else if ("k".equals(e.getKey())) {
                assertEquals("v", e.getValue());
                foundKV = true;
            }
        }
        assertTrue(foundNull);
        assertTrue(foundKV);
    }

    /*** Test that comparator() returns null when natural ordering is used ***/
    @Test
    public void testNaturalOrderingComparatorIsNull() {
        assertNull(map.comparator());
    }
}
