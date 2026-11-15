package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentNavigableMapNullSafe}.
 *
 * The tests cover:
 *   • All public constructors and methods (including those inherited from {@link AbstractConcurrentNullSafeMap})
 *   • Normal operation with non‑null keys/values
 *   • Null‑key and null‑value handling
 *   • Edge cases for inclusive / exclusive bounds, empty maps, and ordering
 *   • The {@link KeyNavigableSet} view and its navigational helpers
 *   • Expected exception scenarios (e.g., ClassCastException for incomparable keys)
 */
public class ConcurrentNavigableMapNullSafeTest {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;
    private ConcurrentNavigableMapNullSafe<String, Integer> mapWithComparator;

    @Before
    public void setUp() {
        map = new ConcurrentNavigableMapNullSafe<>();
        mapWithComparator = new ConcurrentNavigableMapNullSafe<>(Comparator.naturalOrder());
    }

    /* -------------------------------------------------------------
     *  Basic put/get/remove operations, including null handling
     * ------------------------------------------------------------- */
    @Test
    public void testPutGetRemoveBasic() {
        assertTrue(map.isEmpty());

        // normal put / get
        assertNull(map.put("a", 1));
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(1, map.size());

        // replace existing value
        assertEquals(Integer.valueOf(1), map.put("a", 2));
        assertEquals(Integer.valueOf(2), map.get("a"));

        // null value
        assertNull(map.put("b", null));
        assertNull(map.get("b"));

        // null key
        assertNull(map.put(null, 99));
        assertEquals(Integer.valueOf(99), map.get(null));

        // remove
        assertEquals(Integer.valueOf(99), map.remove(null));
        assertFalse(map.containsKey(null));
        assertNull(map.remove("non‑existent"));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, Integer> src = new HashMap<>();
        src.put("x", 10);
        src.put("y", 20);
        src.put(null, 30);
        map.putAll(src);

        assertEquals(3, map.size());
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(30), map.get(null));

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetOrDefaultAndPutIfAbsent() {
        assertNull(map.getOrDefault("missing", 5));
        assertEquals(Integer.valueOf(5), map.getOrDefault("missing", 5));

        assertNull(map.putIfAbsent("p", 7));
        assertEquals(Integer.valueOf(7), map.get("p"));
        // second putIfAbsent should not replace
        assertEquals(Integer.valueOf(7), map.putIfAbsent("p", 9));
        assertEquals(Integer.valueOf(7), map.get("p"));
    }

    @Test
    public void testComputeIfAbsentAndCompute() {
        Function<String, Integer> mapping = k -> k.length();

        // computeIfAbsent
        assertEquals(Integer.valueOf(3), map.computeIfAbsent("foo", mapping));
        assertEquals(Integer.valueOf(3), map.get("foo"));

        // compute (replace existing)
        BiFunction<String, Integer, Integer> remap = (k, v) -> v + 10;
        assertEquals(Integer.valueOf(13), map.compute("foo", remap));
        assertEquals(Integer.valueOf(13), map.get("foo"));

        // compute (remove entry by returning null)
        assertNull(map.compute("foo", (k, v) -> null));
        assertFalse(map.containsKey("foo"));
    }

    @Test
    public void testMerge() {
        // first insertion
        assertNull(map.merge("k", 5, (oldV, newV) -> oldV + newV));
        assertEquals(Integer.valueOf(5), map.get("k"));

        // merge with existing value
        assertEquals(Integer.valueOf(12), map.merge("k", 7, (oldV, newV) -> oldV + newV));
        assertEquals(Integer.valueOf(12), map.get("k"));

        // merge with null result removes the entry
        assertNull(map.merge("k", 0, (oldV, newV) -> null));
        assertFalse(map.containsKey("k"));
    }

    /* -------------------------------------------------------------
     *  Comparator handling
     * ------------------------------------------------------------- */
    @Test
    public void testComparatorExposure() {
        assertNull(map.comparator()); // natural ordering
        assertNotNull(mapWithComparator.comparator());
        assertEquals(Comparator.naturalOrder(), mapWithComparator.comparator());
    }

    /* -------------------------------------------------------------
     *  Navigational methods (lower / floor / ceiling / higher)
     * ------------------------------------------------------------- */
    @Test
    public void testNavigationalMethodsWithoutNullKey() {
        map.put("a", 1);
        map.put("c", 3);
        map.put("e", 5);

        // lower
        assertNull(map.lowerKey("a"));
        assertEquals("a", map.lowerKey("c"));
        assertEquals("c", map.lowerKey("e"));

        // floor
        assertEquals("a", map.floorKey("a"));
        assertEquals("c", map.floorKey("c"));
        assertEquals("c", map.floorKey("d")); // d not present, floor is c

        // ceiling
        assertEquals("a", map.ceilingKey("a"));
        assertEquals("c", map.ceilingKey("b"));
        assertEquals("e", map.ceilingKey("e"));
        assertNull(map.ceilingKey("f"));

        // higher
        assertEquals("c", map.higherKey("a"));
        assertEquals("e", map.higherKey("c"));
        assertNull(map.higherKey("e"));
    }

    @Test
    public void testNavigationalMethodsWithNullKey() {
        map.put("a", 1);
        map.put(null, 99);
        map.put("z", 2);

        // In ascending view, null key is considered last
        assertEquals("z", map.lowerKey(null));
        assertEquals("z", map.floorKey(null));
        assertNull(map.ceilingKey(null));
        assertNull(map.higherKey(null));

        // In descending view (via descendingMap()), null key becomes first
        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        assertNull(desc.lowerKey(null));          // there is no key lower than null in descending order
        assertNull(desc.floorKey(null));          // null is the first key
        assertEquals("z", desc.ceilingKey(null)); // next key after null is "z"
        assertEquals("z", desc.higherKey(null));
    }

    @Test
    public void testEntryMethods() {
        map.put("a", 1);
        map.put("c", 3);
        map.put(null, 99);

        // firstEntry / lastEntry
        Entry<String, Integer> first = map.firstEntry();
        assertEquals("a", first.getKey());
        assertEquals(Integer.valueOf(1), first.getValue());

        Entry<String, Integer> last = map.lastEntry();
        assertNull(last.getKey()); // null key is last in ascending view
        assertEquals(Integer.valueOf(99), last.getValue());

        // pollFirstEntry removes the first entry
        Entry<String, Integer> polledFirst = map.pollFirstEntry();
        assertEquals("a", polledFirst.getKey());
        assertFalse(map.containsKey("a"));
        assertEquals(2, map.size());

        // pollLastEntry removes the null key entry
        Entry<String, Integer> polledLast = map.pollLastEntry();
        assertNull(polledLast.getKey());
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());
    }

    /* -------------------------------------------------------------
     *  Sub‑map, head‑map, tail‑map with inclusive / exclusive bounds
     * ------------------------------------------------------------- */
    @Test
    public void testSubMapInclusiveExclusive() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put(null, 99);

        // subMap from "b" (inclusive) to "d" (exclusive) -> should contain b and c
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("b", true, "d", false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("b"));
        assertTrue(sub.containsKey("c"));
        assertFalse(sub.containsKey("d"));
        assertFalse(sub.containsKey(null));

        // headMap up to "c" (exclusive) -> a and b
        ConcurrentNavigableMap<String, Integer> head = map.headMap("c", false);
        assertEquals(2, head.size());
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertFalse(head.containsKey("c"));

        // tailMap from "c" (inclusive) -> c, d, null
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("c", true);
        assertEquals(3, tail.size());
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey("d"));
        assertTrue(tail.containsKey(null));
    }

    @Test
    public void testSubMapConvenienceMethods() {
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        // subMap(K,K) defaults to [fromKey, toKey)
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("x", "y");
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey("x"));
        assertFalse(sub.containsKey("y"));
    }

    @Test
    public void testHeadMapAndTailMapConvenienceMethods() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        ConcurrentNavigableMap<String, Integer> head = map.headMap("b"); // exclusive
        assertEquals(1, head.size());
        assertTrue(head.containsKey("a"));

        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b"); // inclusive
        assertEquals(2, tail.size());
        assertTrue(tail.containsKey("b"));
        assertTrue(tail.containsKey("c"));
    }

    /* -------------------------------------------------------------
     *  Descending map and key set ordering
     * ------------------------------------------------------------- */
    @Test
    public void testDescendingMapOrder() {
        map.put("a", 1);
        map.put("c", 3);
        map.put(null, 99);

        NavigableSet<String> descendingKeys = map.descendingKeySet();
        Iterator<String> it = descendingKeys.iterator();

        // Expected order: null, "c", "a"
        assertNull(it.next());
        assertEquals("c", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testKeySetContainsAndRemove() {
        map.put("k1", 10);
        map.put(null, 20);
        map.put("k2", 30);

        NavigableSet<String> ks = map.keySet();
        assertEquals(3, ks.size());
        assertTrue(ks.contains("k1"));
        assertTrue(ks.contains(null));
        assertFalse(ks.contains("missing"));

        // remove via keySet view
        assertTrue(ks.remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(2, ks.size());

        ks.clear();
        assertTrue(map.isEmpty());
    }

    /* -------------------------------------------------------------
     *  KeyNavigableSet specific navigational helpers
     * ------------------------------------------------------------- */
    @Test
    public void testKeyNavigableSetHelpers() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put(null, 99);

        NavigableSet<String> ks = map.keySet();

        // lower / floor / ceiling / higher
        assertNull(ks.lower("a"));
        assertEquals("a", ks.floor("a"));
        assertEquals("a", ks.ceiling("a"));
        assertEquals("b", ks.higher("a"));

        assertEquals("b", ks.lower("c"));
        assertEquals("c", ks.floor("c"));
        assertEquals("c", ks.ceiling("c"));
        assertNull(ks.higher("c")); // "c" is the last non‑null key; null is considered after it

        // pollFirst / pollLast
        assertEquals("a", ks.pollFirst());
        assertEquals(Integer.valueOf(2), map.get("b")); // a removed
        assertEquals(null, ks.pollLast()); // last key is null sentinel
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testKeyNavigableSetDescendingIterator() {
        map.put("1", 1);
        map.put("2", 2);
        map.put(null, 99);

        NavigableSet<String> ks = map.keySet();
        Iterator<String> descIt = ks.descendingIterator();

        // Expected descending order: null, "2", "1"
        assertNull(descIt.next());
        assertEquals("2", descIt.next());
        assertEquals("1", descIt.next());
        assertFalse(descIt.hasNext());
    }

    @Test
    public void testKeyNavigableSetSubSetHeadSetTailSet() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put(null, 99);

        NavigableSet<String> ks = map.keySet();

        // subSet a..c (inclusive a, exclusive c) -> a, b
        NavigableSet<String> sub = ks.subSet("a", true, "c", false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains("a"));
        assertTrue(sub.contains("b"));
        assertFalse(sub.contains("c"));

        // headSet up to "c" inclusive -> a, b, c
        NavigableSet<String> head = ks.headSet("c", true);
        assertEquals(3, head.size());
        assertTrue(head.contains("a"));
        assertTrue(head.contains("b"));
        assertTrue(head.contains("c"));

        // tailSet from "b" exclusive -> c, d, null
        NavigableSet<String> tail = ks.tailSet("b", false);
        assertEquals(3, tail.size());
        assertTrue(tail.contains("c"));
        assertTrue(tail.contains("d"));
        assertTrue(tail.contains(null));
    }

    /* -------------------------------------------------------------
     *  Edge case: incomparable keys without a comparator
     * ------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testIncomparableKeysThrowException() {
        // Raw map so we can insert different key types
        @SuppressWarnings("unchecked")
        ConcurrentNavigableMapNullSafe rawMap = new ConcurrentNavigableMapNullSafe();

        rawMap.put("string", 1);
        rawMap.put(42, 2); // Integer key, not comparable with String

        // Attempt to retrieve lowerKey for a String should trigger ClassCastException
        rawMap.lowerKey("string");
    }

    /* -------------------------------------------------------------
     *  Verify that entrySet reflects unmasked keys/values
     * ------------------------------------------------------------- */
    @Test
    public void testEntrySetUnmasking() {
        map.put("k", null);
        map.put(null, 77);

        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(2, entries.size());

        boolean foundNullKey = false;
        boolean foundNullValue = false;
        for (Map.Entry<String, Integer> e : entries) {
            if (e.getKey() == null) {
                foundNullKey = true;
                assertEquals(Integer.valueOf(77), e.getValue());
            }
            if (e.getValue() == null) {
                foundNullValue = true;
                assertEquals("k", e.getKey());
            }
        }
        assertTrue(foundNullKey);
        assertTrue(foundNullValue);
    }

    /* -------------------------------------------------------------
     *  Verify that toString of wrapped entries is correct
     * ------------------------------------------------------------- */
    @Test
    public void testEntryToString() {
        map.put("alpha", 10);
        map.put(null, 20);

        Entry<String, Integer> e1 = map.firstEntry(); // "alpha"
        assertEquals("alpha=10", e1.toString());

        Entry<String, Integer> e2 = map.lastEntry(); // null key
        assertEquals("null=20", e2.toString());
    }
}
