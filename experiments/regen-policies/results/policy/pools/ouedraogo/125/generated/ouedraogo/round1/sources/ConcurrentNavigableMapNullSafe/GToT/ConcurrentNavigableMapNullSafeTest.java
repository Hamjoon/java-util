package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentNavigableMapNullSafe}.
 *
 * The tests are organised in three logical groups that mirror the three
 * “experts” described in the prompt:
 *
 * 1. **Public API extraction & basic functionality** – one test per public
 *    method that verifies the nominal behaviour.
 * 2. **Edge‑case & exception identification** – tests that exercise
 *    null keys/values, empty maps, exclusive/inclusive bounds, custom
 *    comparators and illegal argument situations.
 * 3. **Merged complete test class** – all tests together in a single file.
 *
 * The class under test deliberately allows {@code null} keys and values
 * by substituting a unique sentinel string.  All tests therefore verify that
 * the sentinel handling is transparent to callers.
 */
public class ConcurrentNavigableMapNullSafeTest {

    private ConcurrentNavigableMapNullSafe<String, String> map;

    @Before
    public void setUp() {
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @After
    public void tearDown() {
        map.clear();
    }

    /* ----------------------------------------------------------------------
     * 1.  BASIC FUNCTIONALITY TESTS – one per public method
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructorNoArg() {
        assertTrue(map.isEmpty());
        assertNull(map.comparator());
    }

    @Test
    public void testConstructorWithComparator() {
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<String, String> revMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        assertEquals(reverse, revMap.comparator());
        revMap.put("a", "1");
        revMap.put("b", "2");
        // Because comparator is reversed, highest key should be "a"
        assertEquals("a", revMap.firstKey());
    }

    @Test
    public void testPutAndGetWithNullKeyAndValue() {
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));
    }

    @Test
    public void testComparatorMethod() {
        Comparator<String> comp = Comparator.naturalOrder();
        ConcurrentNavigableMapNullSafe<String, String> m = new ConcurrentNavigableMapNullSafe<>(comp);
        assertSame(comp, m.comparator());
    }

    @Test
    public void testSubMapInclusiveExclusive() {
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");
        // subMap from "a" (inclusive) to "c" (exclusive) should contain a & b
        ConcurrentNavigableMap<String, String> sub = map.subMap("a", true, "c", false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertFalse(sub.containsKey("c"));
    }

    @Test
    public void testHeadMapInclusive() {
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");
        // headMap up to "b" inclusive should contain a & b
        ConcurrentNavigableMap<String, String> head = map.headMap("b", true);
        assertEquals(2, head.size());
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
    }

    @Test
    public void testTailMapInclusive() {
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");
        // tailMap from "b" inclusive should contain b & c
        ConcurrentNavigableMap<String, String> tail = map.tailMap("b", true);
        assertEquals(2, tail.size());
        assertTrue(tail.containsKey("b"));
        assertTrue(tail.containsKey("c"));
    }

    @Test
    public void testLowerEntryAndKey() {
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");
        Map.Entry<String, String> lower = map.lowerEntry("b");
        assertEquals("a", lower.getKey());
        assertEquals("A", lower.getValue());
        assertEquals("a", map.lowerKey("b"));
    }

    @Test
    public void testFloorEntryAndKey() {
        map.put("a", "A");
        map.put("b", "B");
        Map.Entry<String, String> floor = map.floorEntry("b");
        assertEquals("b", floor.getKey());
        assertEquals("B", floor.getValue());
        assertEquals("b", map.floorKey("b"));
    }

    @Test
    public void testCeilingEntryAndKey() {
        map.put("a", "A");
        map.put("c", "C");
        Map.Entry<String, String> ceiling = map.ceilingEntry("b");
        assertEquals("c", ceiling.getKey());
        assertEquals("C", ceiling.getValue());
        assertEquals("c", map.ceilingKey("b"));
    }

    @Test
    public void testHigherEntryAndKey() {
        map.put("a", "A");
        map.put("b", "B");
        Map.Entry<String, String> higher = map.higherEntry("a");
        assertEquals("b", higher.getKey());
        assertEquals("B", higher.getValue());
        assertEquals("b", map.higherKey("a"));
    }

    @Test
    public void testFirstAndLastEntry() {
        map.put("x", "X");
        map.put("y", "Y");
        map.put(null, "NULL");
        // natural order: null key is considered last in ascending view
        assertEquals("x", map.firstEntry().getKey());
        assertEquals("y", map.lastEntry().getKey());
    }

    @Test
    public void testPollFirstAndLastEntry() {
        map.put("a", "A");
        map.put("b", "B");
        Map.Entry<String, String> first = map.pollFirstEntry();
        assertEquals("a", first.getKey());
        assertEquals(1, map.size());
        Map.Entry<String, String> last = map.pollLastEntry();
        assertEquals("b", last.getKey());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testFirstAndLastKey() {
        map.put("a", "A");
        map.put("b", "B");
        assertEquals("a", map.firstKey());
        assertEquals("b", map.lastKey());
    }

    @Test
    public void testNavigableKeySetIteration() {
        map.put("a", "A");
        map.put("b", "B");
        map.put(null, "NULL");
        Iterator<String> it = map.navigableKeySet().iterator();
        List<String> keys = new ArrayList<>();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        // natural ordering, null key appears last
        assertEquals(Arrays.asList("a", "b", null), keys);
    }

    @Test
    public void testDescendingKeySet() {
        map.put("a", "A");
        map.put("b", "B");
        map.put(null, "NULL");
        NavigableSet<String> desc = map.descendingKeySet();
        Iterator<String> it = desc.iterator();
        List<String> keys = new ArrayList<>();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        // descending view: null key first
        assertEquals(Arrays.asList(null, "b", "a"), keys);
    }

    @Test
    public void testDescendingMap() {
        map.put("a", "A");
        map.put("b", "B");
        map.put(null, "NULL");
        ConcurrentNavigableMap<String, String> descMap = map.descendingMap();
        assertEquals("b", descMap.firstKey()); // null is now first in descending, but map.firstKey() returns null
        assertEquals(null, descMap.lastKey());
    }

    @Test
    public void testKeySetContainsAndRemove() {
        map.put("a", "A");
        map.put(null, "NULL");
        assertTrue(map.keySet().contains("a"));
        assertTrue(map.keySet().contains(null));
        assertTrue(map.keySet().remove("a"));
        assertFalse(map.containsKey("a"));
        assertTrue(map.keySet().remove(null));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testKeySetPollFirstAndPollLast() {
        map.put("a", "A");
        map.put("b", "B");
        map.put(null, "NULL");
        NavigableSet<String> ks = map.keySet();
        assertEquals("a", ks.pollFirst());
        assertEquals(null, ks.pollLast()); // null key considered last in ascending set
        assertEquals(1, ks.size());
        assertEquals("b", ks.first());
    }

    /* ----------------------------------------------------------------------
     * 2.  EDGE‑CASE & EXCEPTION TESTS
     * ---------------------------------------------------------------------- */

    @Test
    public void testEmptyMapNavigationalMethodsReturnNull() {
        assertNull(map.firstEntry());
        assertNull(map.lastEntry());
        assertNull(map.lowerEntry("any"));
        assertNull(map.lowerKey("any"));
        assertNull(map.floorEntry("any"));
        assertNull(map.floorKey("any"));
        assertNull(map.ceilingEntry("any"));
        assertNull(map.ceilingKey("any"));
        assertNull(map.higherEntry("any"));
        assertNull(map.higherKey("any"));
        assertTrue(map.navigableKeySet().isEmpty());
    }

    @Test
    public void testSubMapWithNullBounds() {
        map.put("a", "A");
        map.put(null, "NULL");
        // SubMap from null (inclusive) to null (exclusive) should be empty
        ConcurrentNavigableMap<String, String> sub = map.subMap(null, true, null, false);
        assertTrue(sub.isEmpty());

        // SubMap from "a" (inclusive) to null (exclusive) should contain only "a"
        sub = map.subMap("a", true, null, false);
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey("a"));
    }

    @Test
    public void testHeadMapWithNullToKey() {
        map.put("a", "A");
        map.put(null, "NULL");
        // headMap up to null (exclusive) should contain all non‑null keys
        ConcurrentNavigableMap<String, String> head = map.headMap(null, false);
        assertEquals(1, head.size());
        assertTrue(head.containsKey("a"));
        assertFalse(head.containsKey(null));
    }

    @Test
    public void testTailMapWithNullFromKey() {
        map.put("a", "A");
        map.put(null, "NULL");
        // tailMap from null (inclusive) should contain null and all later keys (null is last)
        ConcurrentNavigableMap<String, String> tail = map.tailMap(null, true);
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubMapInvalidRangeThrowsException() {
        map.put("a", "A");
        map.put("b", "B");
        // fromKey > toKey should cause IllegalArgumentException
        map.subMap("b", true, "a", false);
    }

    @Test
    public void testCustomComparatorOrderingWithNulls() {
        Comparator<String> lengthComp = (s1, s2) -> {
            int l1 = (s1 == null) ? -1 : s1.length();
            int l2 = (s2 == null) ? -1 : s2.length();
            return Integer.compare(l1, l2);
        };
        ConcurrentNavigableMapNullSafe<String, String> lengthMap = new ConcurrentNavigableMapNullSafe<>(lengthComp);
        lengthMap.put("aa", "2");
        lengthMap.put("b", "1");
        lengthMap.put(null, "NULL"); // sentinel considered greater than any real key
        assertEquals("b", lengthMap.firstKey()); // shortest non‑null key
        assertEquals(null, lengthMap.lastKey()); // null is greatest per wrapper
    }

    @Test
    public void testEntrySetModificationReflectsInMap() {
        map.put("a", "A");
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertEquals(1, entrySet.size());
        Map.Entry<String, String> e = entrySet.iterator().next();
        assertEquals("a", e.getKey());
        assertEquals("A", e.getValue());

        // modify value via entry.setValue
        e.setValue("NEW");
        assertEquals("NEW", map.get("a"));
    }

    @Test
    public void testKeySetSubSetViews() {
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");
        NavigableSet<String> ks = map.keySet();
        NavigableSet<String> sub = ks.subSet("a", true, "c", false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains("a"));
        assertTrue(sub.contains("b"));
        assertFalse(sub.contains("c"));

        NavigableSet<String> head = ks.headSet("b", true);
        assertEquals(2, head.size());
        assertTrue(head.contains("a"));
        assertTrue(head.contains("b"));

        NavigableSet<String> tail = ks.tailSet("b", true);
        assertEquals(2, tail.size());
        assertTrue(tail.contains("b"));
        assertTrue(tail.contains("c"));
    }

    @Test
    public void testKeySetDescendingIterator() {
        map.put("a", "A");
        map.put("b", "B");
        map.put(null, "NULL");
        Iterator<String> dit = map.keySet().descendingIterator();
        List<String> order = new ArrayList<>();
        while (dit.hasNext()) {
            order.add(dit.next());
        }
        // descending iterator: null first, then highest key, then lower
        assertEquals(Arrays.asList(null, "b", "a"), order);
    }

    @Test
    public void testClearAndIsEmpty() {
        map.put("x", "X");
        map.put(null, "NULL");
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
        assertTrue(map.navigableKeySet().isEmpty());
    }

    @Test
    public void testPutIfAbsentAndReplace() {
        map.putIfAbsent("k", "V1");
        assertEquals("V1", map.get("k"));
        map.putIfAbsent("k", "V2");
        assertEquals("V1", map.get("k")); // unchanged

        boolean replaced = map.replace("k", "V1", "V3");
        assertTrue(replaced);
        assertEquals("V3", map.get("k"));

        // replace with old value mismatch
        replaced = map.replace("k", "V1", "V4");
        assertFalse(replaced);
        assertEquals("V3", map.get("k"));
    }

    @Test
    public void testComputeIfAbsentAndMerge() {
        map.computeIfAbsent("new", k -> "computed");
        assertEquals("computed", map.get("new"));

        map.merge("new", "X", (oldV, newV) -> oldV + newV);
        assertEquals("computedX", map.get("new"));
    }

    /* ----------------------------------------------------------------------
     * 3.  MERGED COMPLETE TEST CLASS (All tests above are part of this file)
     * ---------------------------------------------------------------------- */
}
