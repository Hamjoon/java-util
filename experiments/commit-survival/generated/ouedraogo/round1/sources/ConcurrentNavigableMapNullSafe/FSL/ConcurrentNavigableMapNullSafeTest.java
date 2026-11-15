package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.Map.Entry;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentNavigableMapNullSafe}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic put / get with null keys and/or null values</li>
 *   <li>ordering semantics – null keys are last in the ascending view and first in the descending view</li>
 *   <li>all navigational methods (lower / floor / ceiling / higher, first / last, poll…)</li>
 *   <li>subMap / headMap / tailMap (inclusive / exclusive variants)</li>
 *   <li>the {@code KeyNavigableSet} implementation (contains, remove, lower, floor, ceiling, higher, pollFirst, pollLast, iterator order, descending iterator)</li>
 *   <li>the wrapper {@link Entry} returned by the map (key/value un‑masking, setValue, equals / hashCode)</li>
 *   <li>behavior when a custom comparator is supplied</li>
 * </ul>
 */
public class ConcurrentNavigableMapNullSafeTest {

    /* ---------------------------------------------------------------------- */
    /*  Basic put / get, null handling                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetWithNullKeyAndNullValue() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();

        // put null key with null value
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));

        // replace null value with a real value
        map.put(null, "value");
        assertEquals("value", map.get(null));

        // replace real value with null again
        map.put(null, null);
        assertNull(map.get(null));
    }

    @Test
    public void testPutAndGetWithMixedNullAndNonNull() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", "A");
        map.put(null, "NULL");
        map.put("b", null);

        assertEquals("A", map.get("a"));
        assertEquals("NULL", map.get(null));
        assertNull(map.get("b"));
        assertEquals(3, map.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  Ordering – null key is last in ascending view, first in descending   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testOrderingWithNullKey_Ascending() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("alpha", 1);
        map.put("gamma", 3);
        map.put(null, 99);
        map.put("beta", 2);

        // natural ordering of strings: alpha < beta < gamma < null (sentinel)
        assertEquals("alpha", map.firstKey());
        assertEquals(null, map.lastKey());

        // iterator order of keySet should reflect the same ordering
        List<String> keys = new ArrayList<>();
        for (String k : map.keySet()) {
            keys.add(k);
        }
        assertEquals(Arrays.asList("alpha", "beta", "gamma", null), keys);
    }

    @Test
    public void testOrderingWithNullKey_Descending() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("alpha", 1);
        map.put("gamma", 3);
        map.put(null, 99);
        map.put("beta", 2);

        ConcurrentNavigableMap<String, Integer> descending = map.descendingMap();

        // In descending view null is considered the smallest key
        assertEquals(null, descending.firstKey());
        assertEquals("gamma", descending.lastKey());

        List<String> descKeys = new ArrayList<>();
        for (String k : descending.keySet()) {
            descKeys.add(k);
        }
        assertEquals(Arrays.asList(null, "gamma", "beta", "alpha"), descKeys);
    }

    /* ---------------------------------------------------------------------- */
    /*  Navigational methods (lower / floor / ceiling / higher)               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNavigationalMethods_NonNullKeys() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(10, "ten");
        map.put(20, "twenty");
        map.put(30, "thirty");

        assertEquals(Integer.valueOf(10), map.lowerKey(20));
        assertEquals(Integer.valueOf(20), map.floorKey(20));
        assertEquals(Integer.valueOf(20), map.ceilingKey(20));
        assertEquals(Integer.valueOf(30), map.higherKey(20));

        // Entry versions
        assertEquals(Integer.valueOf(10), map.lowerEntry(20).getKey());
        assertEquals("ten", map.lowerEntry(20).getValue());

        assertEquals(Integer.valueOf(20), map.floorEntry(20).getKey());
        assertEquals("twenty", map.floorEntry(20).getValue());

        assertEquals(Integer.valueOf(20), map.ceilingEntry(20).getKey());
        assertEquals("twenty", map.ceilingEntry(20).getValue());

        assertEquals(Integer.valueOf(30), map.higherEntry(20).getKey());
        assertEquals("thirty", map.higherEntry(20).getValue());
    }

    @Test
    public void testNavigationalMethods_WithNullKey() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);

        // null is greatest, so lowerKey(null) should be the greatest non‑null key
        assertEquals("b", map.lowerKey(null));
        assertEquals(null, map.higherKey(null)); // nothing greater than null

        // floor/ceiling of null
        assertEquals(null, map.floorKey(null));
        assertEquals(null, map.ceilingKey(null));

        // Entry equivalents
        assertEquals("b", map.lowerEntry(null).getKey());
        assertEquals(Integer.valueOf(2), map.lowerEntry(null).getValue());

        assertEquals(null, map.floorEntry(null).getKey());
        assertEquals(Integer.valueOf(99), map.floorEntry(null).getValue());

        assertEquals(null, map.ceilingEntry(null).getKey());
        assertEquals(Integer.valueOf(99), map.ceilingEntry(null).getValue());

        assertNull(map.higherEntry(null));
    }

    @Test
    public void testFirstAndLastEntryAndPoll() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("x", "X");
        map.put(null, "NULL");
        map.put("a", "A");

        // firstEntry should be "a"
        Entry<String, String> first = map.firstEntry();
        assertEquals("a", first.getKey());
        assertEquals("A", first.getValue());

        // lastEntry should be null key
        Entry<String, String> last = map.lastEntry();
        assertEquals(null, last.getKey());
        assertEquals("NULL", last.getValue());

        // pollFirstEntry removes "a"
        Entry<String, String> polledFirst = map.pollFirstEntry();
        assertEquals("a", polledFirst.getKey());
        assertFalse(map.containsKey("a"));

        // pollLastEntry removes null key
        Entry<String, String> polledLast = map.pollLastEntry();
        assertEquals(null, polledLast.getKey());
        assertFalse(map.containsKey(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  Sub‑map, head‑map, tail‑map (inclusive / exclusive)                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSubMapInclusiveExclusive() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(null, "null");

        // subMap(1, true, 3, false) => keys 1 and 2
        ConcurrentNavigableMap<Integer, String> sub = map.subMap(1, true, 3, false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(1));
        assertTrue(sub.containsKey(2));
        assertFalse(sub.containsKey(3));
        assertFalse(sub.containsKey(null));

        // Verify that the sub‑map view reflects changes in the backing map
        map.put(2, "TWO");
        assertEquals("TWO", sub.get(2));
    }

    @Test
    public void testHeadMapAndTailMap() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put(null, 99);

        // headMap up to "b" (exclusive) => "a"
        ConcurrentNavigableMap<String, Integer> head = map.headMap("b", false);
        assertEquals(1, head.size());
        assertTrue(head.containsKey("a"));
        assertFalse(head.containsKey("b"));

        // tailMap from "b" (inclusive) => "b","c",null
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", true);
        assertEquals(3, tail.size());
        assertTrue(tail.containsKey("b"));
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  KeyNavigableSet – contains / remove / lower / floor / ceiling / higher */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testKeyNavigableSetBasicOperations() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("x", 10);
        map.put(null, 99);
        map.put("a", 1);

        NavigableSet<String> keySet = map.navigableKeySet();

        // contains
        assertTrue(keySet.contains("x"));
        assertTrue(keySet.contains(null));
        assertFalse(keySet.contains("missing"));

        // remove
        assertTrue(keySet.remove("x"));
        assertFalse(map.containsKey("x"));
        assertFalse(keySet.contains("x"));

        // lower / floor / ceiling / higher relative to null
        assertEquals("a", keySet.lower(null));
        assertEquals(null, keySet.floor(null));
        assertEquals(null, keySet.ceiling(null));
        assertNull(keySet.higher(null));
    }

    @Test
    public void testKeyNavigableSetPollFirstAndPollLast() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(5, "five");
        map.put(10, "ten");
        map.put(null, "null");

        NavigableSet<Integer> ks = map.navigableKeySet();

        // pollFirst should return the smallest non‑null key (5)
        assertEquals(Integer.valueOf(5), ks.pollFirst());
        assertFalse(map.containsKey(5));

        // pollLast should return the null key (null is last in ascending view)
        assertNull(ks.pollLast());
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testKeyNavigableSetDescendingIteratorOrder() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);
        map.put("c", 3);

        NavigableSet<String> descending = map.descendingKeySet();

        List<String> order = new ArrayList<>();
        for (String k : descending) {
            order.add(k);
        }
        // descending order: null, c, b, a
        assertEquals(Arrays.asList(null, "c", "b", "a"), order);
    }

    @Test
    public void testKeyNavigableSetComparatorMatchesOriginal() {
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>(reverse);
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);

        NavigableSet<String> ks = map.navigableKeySet();
        assertSame(reverse, ks.comparator());

        // Verify that iteration follows the reverse comparator (largest first, null last)
        List<String> iter = new ArrayList<>();
        for (String k : ks) {
            iter.add(k);
        }
        // natural order reversed: b, a, null
        assertEquals(Arrays.asList("b", "a", null), iter);
    }

    /* ---------------------------------------------------------------------- */
    /*  Entry wrapper – unmasking, setValue, equals / hashCode                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEntryWrapperBehaviour() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(null, null);
        map.put("k", "v");

        // firstEntry corresponds to key "k"
        Entry<String, String> e = map.firstEntry();
        assertEquals("k", e.getKey());
        assertEquals("v", e.getValue());

        // setValue should update the underlying map and return old value
        String old = e.setValue("newV");
        assertEquals("v", old);
        assertEquals("newV", map.get("k"));

        // equals / hashCode
        Entry<String, String> same = map.firstEntry();
        assertEquals(e, same);
        assertEquals(e.hashCode(), same.hashCode());

        // toString format
        assertEquals("k=newV", e.toString());

        // Entry for null key
        Entry<String, String> nullEntry = map.lastEntry(); // null key is last
        assertNull(nullEntry.getKey());
        assertNull(nullEntry.getValue());
    }

    /* ---------------------------------------------------------------------- */
    /*  Sub‑set, head‑set, tail‑set views from the KeyNavigableSet             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testKeySetSubSetHeadSetTailSet() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = new ConcurrentNavigableMapNullSafe<>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(4, "four");
        map.put(null, "null");

        NavigableSet<Integer> ks = map.navigableKeySet();

        // subSet 2 (inclusive) .. 4 (exclusive) => 2,3
        NavigableSet<Integer> sub = ks.subSet(2, true, 4, false);
        assertEquals(new HashSet<>(Arrays.asList(2, 3)), new HashSet<>(sub));

        // headSet up to 3 (exclusive) => 1,2
        NavigableSet<Integer> head = ks.headSet(3, false);
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), new HashSet<>(head));

        // tailSet from 3 (inclusive) => 3,4,null
        NavigableSet<Integer> tail = ks.tailSet(3, true);
        assertTrue(tail.contains(3));
        assertTrue(tail.contains(4));
        assertTrue(tail.contains(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  Verify that original comparator is exposed via comparator() method   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComparatorExposure() {
        Comparator<String> lengthComp = Comparator.comparingInt(String::length);
        ConcurrentNavigableMapNullSafe<String, Integer> map = new ConcurrentNavigableMapNullSafe<>(lengthComp);
        assertSame(lengthComp, map.comparator());
    }

    /* ---------------------------------------------------------------------- */
    /*  Ensure that clear, size, isEmpty behave as expected                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testBasicMapOperations() {
        ConcurrentNavigableMapNullSafe<String, String> map = new ConcurrentNavigableMapNullSafe<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("a", "A");
        map.put(null, "NULL");
        assertFalse(map.isEmpty());
        assertEquals(2, map.size());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }
}
