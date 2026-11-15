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

    /*** Basic put / get including null keys and null values ***/
    @Test
    public void testPutAndGetWithNullKeyAndValue() {
        // null key, non‑null value
        assertNull(map.put(null, "nullKeyValue"));
        assertEquals("nullKeyValue", map.get(null));

        // non‑null key, null value
        assertNull(map.put("k1", null));
        assertNull(map.get("k1"));

        // overwrite existing entries
        assertEquals("nullKeyValue", map.put(null, "newNullKeyValue"));
        assertEquals("newNullKeyValue", map.get(null));

        assertNull(map.put("k1", "v1"));
        assertEquals("v1", map.get("k1"));

        // contains checks
        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsValue(null));
        assertTrue(map.containsValue("newNullKeyValue"));
    }

    /*** Ordering – null keys are last in ascending view ***/
    @Test
    public void testOrderingWithNullKey() {
        map.put("b", "B");
        map.put(null, "NULL");
        map.put("a", "A");

        // natural order: a, b, null
        Iterator<String> it = map.navigableKeySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertNull(it.next());

        // first / last keys
        assertEquals("a", map.firstKey());
        assertNull(map.lastKey());

        // descending view: null first
        ConcurrentNavigableMap<String, String> desc = map.descendingMap();
        Iterator<String> dit = desc.navigableKeySet().iterator();
        assertNull(dit.next());
        assertEquals("b", dit.next());
        assertEquals("a", dit.next());

        assertNull(desc.firstKey());
        assertEquals("a", desc.lastKey());
    }

    /*** Navigational methods (lower / floor / ceiling / higher) ***/
    @Test
    public void testNavigationalMethods() {
        map.put("a", "A");
        map.put("c", "C");
        map.put(null, "NULL");
        map.put("b", "B");

        // lower / floor / ceiling / higher for key "b"
        assertEquals("a", map.lowerKey("b"));
        assertEquals("b", map.floorKey("b"));
        assertEquals("b", map.ceilingKey("b"));
        assertEquals("c", map.higherKey("b"));

        // entry variants
        assertEquals("a", map.lowerEntry("b").getKey());
        assertEquals("b", map.floorEntry("b").getKey());
        assertEquals("b", map.ceilingEntry("b").getKey());
        assertEquals("c", map.higherEntry("b").getKey());

        // for null key (should be greatest)
        assertEquals("c", map.lowerKey(null));
        assertNull(map.floorKey(null));
        assertNull(map.ceilingKey(null));
        assertNull(map.higherKey(null));

        assertEquals("c", map.lowerEntry(null).getKey());
        assertNull(map.floorEntry(null));
        assertNull(map.ceilingEntry(null));
        assertNull(map.higherEntry(null));
    }

    /*** firstEntry / lastEntry and poll variants ***/
    @Test
    public void testFirstLastEntryAndPoll() {
        map.put("x", "X");
        map.put(null, "NULL");
        map.put("a", "A");

        // first / last entry
        assertEquals("a", map.firstEntry().getKey());
        assertEquals("X", map.lastEntry().getKey());

        // poll first
        Map.Entry<String, String> polledFirst = map.pollFirstEntry();
        assertEquals("a", polledFirst.getKey());
        assertEquals("A", polledFirst.getValue());
        assertFalse(map.containsKey("a"));
        assertEquals("x", map.firstKey());

        // poll last (should be null key)
        Map.Entry<String, String> polledLast = map.pollLastEntry();
        assertNull(polledLast.getKey());
        assertEquals("NULL", polledLast.getValue());
        assertFalse(map.containsKey(null));
        assertEquals("x", map.lastKey());
    }

    /*** subMap, headMap, tailMap with inclusive / exclusive bounds ***/
    @Test
    public void testSubMapHeadMapTailMap() {
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        intMap.put(null, "null");

        // subMap 2 (inclusive) to null (exclusive) => 2,3
        ConcurrentNavigableMap<Integer, String> sub = intMap.subMap(2, true, null, false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(2));
        assertTrue(sub.containsKey(3));
        assertFalse(sub.containsKey(null));

        // headMap up to 2 (exclusive) => 1
        ConcurrentNavigableMap<Integer, String> head = intMap.headMap(2, false);
        assertEquals(1, head.size());
        assertTrue(head.containsKey(1));
        assertFalse(head.containsKey(2));

        // tailMap from 2 (inclusive) => 2,3,null
        ConcurrentNavigableMap<Integer, String> tail = intMap.tailMap(2, true);
        assertEquals(3, tail.size());
        assertTrue(tail.containsKey(2));
        assertTrue(tail.containsKey(3));
        assertTrue(tail.containsKey(null));
    }

    /*** keySet basic operations ***/
    @Test
    public void testKeySetContainsRemoveAndIterator() {
        map.put("alpha", "A");
        map.put(null, "NULL");
        map.put("beta", "B");

        // contains
        assertTrue(map.keySet().contains("alpha"));
        assertTrue(map.keySet().contains(null));
        assertFalse(map.keySet().contains("gamma"));

        // iterator order
        Iterator<String> it = map.keySet().iterator();
        assertEquals("alpha", it.next());
        assertEquals("beta", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());

        // remove via keySet
        assertTrue(map.keySet().remove("beta"));
        assertFalse(map.containsKey("beta"));
        assertEquals(2, map.size());

        // clear via keySet
        map.keySet().clear();
        assertTrue(map.isEmpty());
    }

    /*** keySet navigational methods ***/
    @Test
    public void testKeySetNavigationalMethods() {
        map.put("a", "A");
        map.put("c", "C");
        map.put(null, "NULL");
        map.put("b", "B");

        NavigableSet<String> ks = map.navigableKeySet();

        assertEquals("a", ks.lower("b"));
        assertEquals("a", ks.floor("b"));
        assertEquals("b", ks.ceiling("b"));
        assertEquals("c", ks.higher("b"));

        // null key navigational (null is greatest)
        assertEquals("c", ks.lower(null));
        assertNull(ks.floor(null));
        assertNull(ks.ceiling(null));
        assertNull(ks.higher(null));

        // pollFirst / pollLast
        assertEquals("a", ks.pollFirst());
        assertEquals(null, ks.pollLast());
        assertFalse(ks.contains("a"));
        assertFalse(ks.contains(null));
        assertEquals(2, ks.size());
    }

    /*** descendingKeySet and its iterator ***/
    @Test
    public void testDescendingKeySet() {
        map.put("1", "one");
        map.put("2", "two");
        map.put(null, "null");
        map.put("0", "zero");

        NavigableSet<String> desc = map.descendingKeySet();
        Iterator<String> it = desc.iterator();

        assertNull(it.next());   // null first in descending view
        assertEquals("2", it.next());
        assertEquals("1", it.next());
        assertEquals("0", it.next());
        assertFalse(it.hasNext());

        // descendingSet should be equal to map.descendingMap().navigableKeySet()
        assertEquals(map.descendingMap().navigableKeySet(), desc);
    }

    /*** subSet, headSet, tailSet of the keySet ***/
    @Test
    public void testKeySetSubCollections() {
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");
        map.put(null, "NULL");

        NavigableSet<String> ks = map.navigableKeySet();

        // subSet a (inclusive) to c (exclusive) => a, b
        NavigableSet<String> sub = ks.subSet("a", true, "c", false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains("a"));
        assertTrue(sub.contains("b"));
        assertFalse(sub.contains("c"));

        // headSet up to b (exclusive) => a
        NavigableSet<String> head = ks.headSet("b", false);
        assertEquals(1, head.size());
        assertTrue(head.contains("a"));
        assertFalse(head.contains("b"));

        // tailSet from b (inclusive) => b, c, null
        NavigableSet<String> tail = ks.tailSet("b", true);
        assertEquals(3, tail.size());
        assertTrue(tail.contains("b"));
        assertTrue(tail.contains("c"));
        assertTrue(tail.contains(null));
    }

    /*** Comparator preservation and custom comparator behavior ***/
    @Test
    public void testCustomComparatorAndComparatorMethod() {
        Comparator<Integer> reverse = Collections.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        intMap.put(1, "one");
        intMap.put(3, "three");
        intMap.put(2, "two");
        intMap.put(null, "null");

        // comparator() should return the original comparator
        assertSame(reverse, intMap.comparator());

        // ordering: 3,2,1,null (reverse order, null last)
        Iterator<Integer> it = intMap.navigableKeySet().iterator();
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertNull(it.next());

        // first / last keys
        assertEquals(Integer.valueOf(3), intMap.firstKey());
        assertNull(intMap.lastKey());

        // descendingMap should flip the order: null first, then 1,2,3
        ConcurrentNavigableMap<Integer, String> desc = intMap.descendingMap();
        Iterator<Integer> dit = desc.navigableKeySet().iterator();
        assertNull(dit.next());
        assertEquals(Integer.valueOf(1), dit.next());
        assertEquals(Integer.valueOf(2), dit.next());
        assertEquals(Integer.valueOf(3), dit.next());
    }

    /*** Entry set mutation via setValue ***/
    @Test
    public void testEntrySetSetValue() {
        map.put("key", "old");
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        assertEquals("key", entry.getKey());
        assertEquals("old", entry.getValue());

        // change value via entry
        entry.setValue("new");
        assertEquals("new", map.get("key"));
        assertEquals("new", entry.getValue());
    }

    /*** Clear and isEmpty behavior ***/
    @Test
    public void testClearAndIsEmpty() {
        map.put("x", "X");
        map.put(null, "NULL");
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertTrue(map.keySet().isEmpty());
        assertTrue(map.entrySet().isEmpty());
    }

    /*** getOrDefault, putIfAbsent, computeIfAbsent, compute, merge ***/
    @Test
    public void testAdditionalMapOperations() {
        // getOrDefault
        assertEquals("def", map.getOrDefault("missing", "def"));

        // putIfAbsent
        assertNull(map.putIfAbsent("k", "v"));
        assertEquals("v", map.get("k"));
        assertEquals("v", map.putIfAbsent("k", "newV")); // returns existing value

        // computeIfAbsent
        String computed = map.computeIfAbsent("c", k -> "computed");
        assertEquals("computed", computed);
        assertEquals("computed", map.get("c"));

        // compute (replace existing)
        String replaced = map.compute("c", (k, old) -> old + "-updated");
        assertEquals("computed-updated", replaced);
        assertEquals("computed-updated", map.get("c"));

        // merge
        String merged = map.merge("c", "mergeVal", (oldV, newV) -> oldV + "+" + newV);
        assertEquals("computed-updated+mergeVal", merged);
        assertEquals("computed-updated+mergeVal", map.get("c"));
    }
}
