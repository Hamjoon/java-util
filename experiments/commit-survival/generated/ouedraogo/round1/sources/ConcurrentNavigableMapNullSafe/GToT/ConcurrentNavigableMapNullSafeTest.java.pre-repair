package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentNavigableMapNullSafe}.
 *
 * The tests are organised as follows:
 * <ul>
 *   <li>Basic map operations (put, get, remove, size, clear, etc.)</li>
 *   <li>Null‑key and null‑value handling</li>
 *   <li>Comparator handling (natural order and custom comparator)</li>
 *   <li>Navigational methods (lower, floor, ceiling, higher, first/last, poll…)</li>
 *   <li>Sub‑map, head‑map and tail‑map views (including inclusive flags)</li>
 *   <li>Descending map / key set behaviour</li>
 *   <li>KeyNavigableSet specific operations</li>
 *   <li>Edge‑case / exception scenarios (empty map, illegal arguments, etc.)</li>
 * </ul>
 */
public class ConcurrentNavigableMapNullSafeTest {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @Before
    public void setUp() {
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    /* ----------------------------------------------------
     *  Basic map operations
     * ---------------------------------------------------- */

    @Test
    public void testPutGetRemoveSize() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        assertNull(map.put("a", 1));
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(1, map.size());

        assertNull(map.put("b", 2));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(2, map.size());

        assertEquals(Integer.valueOf(1), map.remove("a"));
        assertNull(map.get("a"));
        assertEquals(1, map.size());

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testPutAllContainsKeyContainsValue() {
        Map<String, Integer> src = new HashMap<>();
        src.put("x", 10);
        src.put("y", 20);
        map.putAll(src);

        assertTrue(map.containsKey("x"));
        assertTrue(map.containsValue(20));
        assertEquals(2, map.size());
    }

    @Test
    public void testGetOrDefault() {
        map.put("k", 5);
        assertEquals(Integer.valueOf(5), map.getOrDefault("k", 0));
        assertEquals(Integer.valueOf(99), map.getOrDefault("missing", 99));
    }

    @Test
    public void testPutIfAbsent() {
        assertNull(map.putIfAbsent("p", 7));
        assertEquals(Integer.valueOf(7), map.get("p"));
        // second call does not replace existing value
        assertEquals(Integer.valueOf(7), map.putIfAbsent("p", 8));
        assertEquals(Integer.valueOf(7), map.get("p"));
    }

    @Test
    public void testRemoveKeyValue() {
        map.put("k1", 1);
        assertFalse(map.remove("k1", 2)); // wrong value
        assertTrue(map.remove("k1", 1));  // correct value
        assertFalse(map.containsKey("k1"));
    }

    @Test
    public void testReplace() {
        map.put("r", 100);
        assertTrue(map.replace("r", 100, 200));
        assertEquals(Integer.valueOf(200), map.get("r"));

        // replace with only new value
        assertEquals(Integer.valueOf(200), map.replace("r", 300));
        assertEquals(Integer.valueOf(300), map.get("r"));
    }

    @Test
    public void testComputeIfAbsent() {
        Integer val = map.computeIfAbsent("c", k -> k.length());
        assertEquals(Integer.valueOf(1), val);
        // second call does not recompute
        Integer val2 = map.computeIfAbsent("c", k -> 999);
        assertEquals(Integer.valueOf(1), val2);
    }

    @Test
    public void testCompute() {
        map.put("x", 5);
        Integer result = map.compute("x", (k, v) -> v + 10);
        assertEquals(Integer.valueOf(15), result);
        assertEquals(Integer.valueOf(15), map.get("x"));
    }

    @Test
    public void testMerge() {
        map.put("m", 1);
        Integer merged = map.merge("m", 4, Integer::sum);
        assertEquals(Integer.valueOf(5), merged);
        assertEquals(Integer.valueOf(5), map.get("m"));
    }

    @Test
    public void testEqualsAndHashCode() {
        map.put("a", 1);
        map.put("b", 2);
        ConcurrentNavigableMapNullSafe<String, Integer> other = new ConcurrentNavigableMapNullSafe<>();
        other.put("a", 1);
        other.put("b", 2);
        assertEquals(other, map);
        assertEquals(other.hashCode(), map.hashCode());
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("x", 10);
        map.put("y", 20);
        String s = map.toString();
        assertTrue(s.contains("x=10"));
        assertTrue(s.contains("y=20"));
    }

    /* ----------------------------------------------------
     *  Null key / null value handling
     * ---------------------------------------------------- */

    @Test
    public void testNullKeyHandling() {
        // null key is allowed
        assertNull(map.put(null, 42));
        assertEquals(Integer.valueOf(42), map.get(null));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(42));

        // navigation methods with null key
        assertNull(map.lowerKey(null));
        assertNull(map.floorKey(null));
        assertNull(map.ceilingKey(null));
        assertNull(map.higherKey(null));

        // firstKey / lastKey ordering (null considered last in ascending)
        map.put("a", 1);
        assertEquals("a", map.firstKey());
        assertNull(map.lastKey()); // null key is last
    }

    @Test
    public void testNullValueHandling() {
        map.put("k", null);
        assertTrue(map.containsKey("k"));
        assertTrue(map.containsValue(null));
        assertNull(map.get("k"));

        // entry wrapper should expose null value
        Map.Entry<String, Integer> e = map.firstEntry();
        assertEquals("k", e.getKey());
        assertNull(e.getValue());

        // replace null value
        map.replace("k", null, 5);
        assertEquals(Integer.valueOf(5), map.get("k"));
    }

    /* ----------------------------------------------------
     *  Comparator handling
     * ---------------------------------------------------- */

    @Test
    public void testCustomComparatorReverseOrder() {
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<String, Integer> revMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        revMap.put("a", 1);
        revMap.put("b", 2);
        revMap.put(null, 99); // null sentinel should be considered greater than any key

        // natural order reversed, so "b" is first, null is last in ascending view
        assertEquals("b", revMap.firstKey());
        assertNull(revMap.lastKey());

        // descending view should invert ordering: null first, "a" last
        ConcurrentNavigableMap<String, Integer> desc = revMap.descendingMap();
        assertNull(desc.firstKey());
        assertEquals("a", desc.lastKey());
    }

    /* ----------------------------------------------------
     *  Navigational methods
     * ---------------------------------------------------- */

    @Test
    public void testNavigationalMethods() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put(null, 99);

        assertEquals(Integer.valueOf(1), map.lowerKey("b"));
        assertEquals(Integer.valueOf(2), map.floorKey("b"));
        assertEquals(Integer.valueOf(2), map.ceilingKey("b"));
        assertEquals(Integer.valueOf(3), map.higherKey("b"));

        // Entry variants
        assertEquals("a", map.lowerEntry("b").getKey());
        assertEquals(Integer.valueOf(1), map.lowerEntry("b").getValue());

        assertEquals("b", map.floorEntry("b").getKey());
        assertEquals(Integer.valueOf(2), map.floorEntry("b").getValue());

        assertEquals("b", map.ceilingEntry("b").getKey());
        assertEquals(Integer.valueOf(2), map.ceilingEntry("b").getValue());

        assertEquals("c", map.higherEntry("b").getKey());
        assertEquals(Integer.valueOf(3), map.higherEntry("b").getValue());

        // first / last entries
        assertEquals("a", map.firstEntry().getKey());
        assertEquals(null, map.lastEntry().getKey()); // null key is last
    }

    @Test
    public void testPollFirstAndLastEntry() {
        map.put("x", 10);
        map.put("y", 20);
        map.put(null, 99);

        Map.Entry<String, Integer> first = map.pollFirstEntry();
        assertEquals("x", first.getKey());
        assertEquals(Integer.valueOf(10), first.getValue());
        assertFalse(map.containsKey("x"));

        Map.Entry<String, Integer> last = map.pollLastEntry();
        assertNull(last.getKey()); // null key
        assertEquals(Integer.valueOf(99), last.getValue());
        assertFalse(map.containsKey(null));
    }

    @Test(expected = NoSuchElementException.class)
    public void testFirstKeyOnEmptyMapThrows() {
        map.firstKey(); // should throw NoSuchElementException from underlying SkipListMap
    }

    /* ----------------------------------------------------
     *  Sub‑map, head‑map and tail‑map views
     * ---------------------------------------------------- */

    @Test
    public void testSubMapInclusiveFlags() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put(null, 99);

        // subMap from "b" (inclusive) to "d" (exclusive) should contain b and c
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("b", true, "d", false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("b"));
        assertTrue(sub.containsKey("c"));
        assertFalse(sub.containsKey("d"));
    }

    @Test
    public void testHeadMapAndTailMap() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put(null, 99);

        ConcurrentNavigableMap<String, Integer> head = map.headMap("b", false);
        assertEquals(1, head.size());
        assertTrue(head.containsKey("a"));

        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", true);
        assertEquals(3, tail.size());
        assertTrue(tail.containsKey("b"));
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey(null));
    }

    /* ----------------------------------------------------
     *  Descending map / key set behaviour
     * ---------------------------------------------------- */

    @Test
    public void testDescendingKeySetOrder() {
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);

        NavigableSet<String> descSet = map.descendingKeySet();
        Iterator<String> it = descSet.iterator();

        // descending order: null key first, then "b", then "a"
        assertNull(it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testKeySetNavigableOperations() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put(null, 99);

        NavigableSet<String> ks = map.navigableKeySet();

        assertEquals("a", ks.first());
        assertNull(ks.last()); // null is last in ascending view

        assertEquals("a", ks.lower("b"));
        assertEquals("b", ks.floor("b"));
        assertEquals("b", ks.ceiling("b"));
        assertEquals("c", ks.higher("b"));

        // pollFirst removes "a"
        assertEquals("a", ks.pollFirst());
        assertFalse(ks.contains("a"));

        // pollLast removes null key
        assertNull(ks.pollLast());
        assertFalse(ks.contains(null));
    }

    @Test
    public void testDescendingSetFromKeySet() {
        NavigableSet<String> desc = map.descendingKeySet();
        NavigableSet<String> ascAgain = desc.descendingSet();
        assertEquals(map.navigableKeySet(), ascAgain);
    }

    /* ----------------------------------------------------
     *  KeyNavigableSet subSet/headSet/tailSet delegations
     * ---------------------------------------------------- */

    @Test
    public void testKeySetSubSetHeadSetTailSet() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put(null, 99);

        NavigableSet<String> ks = map.navigableKeySet();

        // subSet from "b" (inclusive) to "d" (exclusive)
        NavigableSet<String> sub = ks.subSet("b", true, "d", false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains("b"));
        assertTrue(sub.contains("c"));
        assertFalse(sub.contains("d"));

        // headSet up to "c" (exclusive)
        NavigableSet<String> head = ks.headSet("c", false);
        assertEquals(2, head.size());
        assertTrue(head.contains("a"));
        assertTrue(head.contains("b"));

        // tailSet from "c" (inclusive)
        NavigableSet<String> tail = ks.tailSet("c", true);
        assertEquals(3, tail.size());
        assertTrue(tail.contains("c"));
        assertTrue(tail.contains("d"));
        assertTrue(tail.contains(null));
    }

    /* ----------------------------------------------------
     *  Edge cases for wrapEntry (null internal entry)
     * ---------------------------------------------------- */

    @Test
    public void testWrapEntryReturnsNullForNullInternalEntry() throws Exception {
        // Use reflection to invoke the private wrapEntry method with a null argument
        java.lang.reflect.Method m = ConcurrentNavigableMapNullSafe.class
                .getDeclaredMethod("wrapEntry", Entry.class);
        m.setAccessible(true);
        Object result = m.invoke(map, (Object) null);
        assertNull(result);
    }

    /* ----------------------------------------------------
     *  Verify that comparator() returns the original comparator
     * ---------------------------------------------------- */

    @Test
    public void testComparatorReturnsOriginal() {
        Comparator<String> comp = String::compareToIgnoreCase;
        ConcurrentNavigableMapNullSafe<String, Integer> m = new ConcurrentNavigableMapNullSafe<>(comp);
        assertSame(comp, m.comparator());
    }
}
