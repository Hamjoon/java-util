package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentNavigableMapNullSafe}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic map operations (put, get, remove, contains, size)</li>
 *   <li>null‑key handling (masking/unmasking, ordering semantics)</li>
 *   <li>all navigational methods (lower, floor, ceiling, higher, first/last, poll…)</li>
 *   <li>sub‑map, head‑map, tail‑map (inclusive / exclusive variants)</li>
 *   <li>descending view and its key set</li>
 *   <li>the {@code KeyNavigableSet} implementation (contains, remove, iterator order, lower/floor/…)</li>
 *   <li>custom comparator handling</li>
 * </ul>
 */
public class ConcurrentNavigableMapNullSafeTest {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @Before
    public void setUp() {
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    /** Helper to create a map pre‑populated with a known ordering. */
    private ConcurrentNavigableMapNullSafe<String, Integer> createSampleMap() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = new ConcurrentNavigableMapNullSafe<>();
        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);
        m.put(null, 99);   // null key should be last in ascending order
        return m;
    }

    // ----------------------------------------------------------------------
    // Basic map operations and null‑key handling
    // ----------------------------------------------------------------------
    @Test
    public void testPutGetRemoveBasic() {
        assertTrue(map.isEmpty());

        map.put("x", 10);
        map.put(null, 0);
        assertEquals(Integer.valueOf(10), map.get("x"));
        assertEquals(Integer.valueOf(0), map.get(null));

        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey(null));
        assertEquals(2, map.size());

        Integer removed = map.remove("x");
        assertEquals(Integer.valueOf(10), removed);
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());

        removed = map.remove(null);
        assertEquals(Integer.valueOf(0), removed);
        assertFalse(map.containsKey(null));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNullKeyOrderingInKeySet() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();

        // Ascending keySet – null key must be the *last* element
        List<String> ascKeys = new ArrayList<>(m.keySet());
        assertEquals(Arrays.asList("a", "b", "c", null), ascKeys);

        // Descending keySet – null key must be the *first* element
        List<String> descKeys = new ArrayList<>(m.descendingKeySet());
        assertEquals(Arrays.asList(null, "c", "b", "a"), descKeys);
    }

    // ----------------------------------------------------------------------
    // Navigational methods (lower / floor / ceiling / higher)
    // ----------------------------------------------------------------------
    @Test
    public void testNavigationalEntriesAndKeys() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();

        // lower of "b" -> entry for "a"
        Entry<String, Integer> lower = m.lowerEntry("b");
        assertNotNull(lower);
        assertEquals("a", lower.getKey());
        assertEquals(Integer.valueOf(1), lower.getValue());

        // floor of "b" -> entry for "b"
        Entry<String, Integer> floor = m.floorEntry("b");
        assertNotNull(floor);
        assertEquals("b", floor.getKey());

        // ceiling of "b" -> entry for "b"
        Entry<String, Integer> ceiling = m.ceilingEntry("b");
        assertNotNull(ceiling);
        assertEquals("b", ceiling.getKey());

        // higher of "b" -> entry for "c"
        Entry<String, Integer> higher = m.higherEntry("b");
        assertNotNull(higher);
        assertEquals("c", higher.getKey());

        // Keys version
        assertEquals("a", m.lowerKey("b"));
        assertEquals("b", m.floorKey("b"));
        assertEquals("b", m.ceilingKey("b"));
        assertEquals("c", m.higherKey("b"));
    }

    @Test
    public void testNavigationalWithNullKey() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();

        // Null is considered greater than any non‑null key
        assertEquals("c", m.lowerKey(null));
        assertEquals(null, m.floorKey(null));
        assertEquals(null, m.ceilingKey(null));
        assertNull(m.higherKey(null));

        // firstEntry should be "a", lastEntry should be null key
        Entry<String, Integer> first = m.firstEntry();
        Entry<String, Integer> last = m.lastEntry();
        assertEquals("a", first.getKey());
        assertEquals(null, last.getKey());
        assertEquals(Integer.valueOf(99), last.getValue());
    }

    @Test
    public void testPollFirstAndLastEntries() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();

        Entry<String, Integer> polledFirst = m.pollFirstEntry();
        assertEquals("a", polledFirst.getKey());
        assertEquals(Integer.valueOf(1), polledFirst.getValue());

        Entry<String, Integer> polledLast = m.pollLastEntry();
        assertEquals(null, polledLast.getKey());
        assertEquals(Integer.valueOf(99), polledLast.getValue());

        // After polling, map should contain only "b" and "c"
        assertEquals(2, m.size());
        assertTrue(m.containsKey("b"));
        assertTrue(m.containsKey("c"));
        assertFalse(m.containsKey(null));
    }

    // ----------------------------------------------------------------------
    // Sub‑map, head‑map, tail‑map (inclusive / exclusive)
    // ----------------------------------------------------------------------
    @Test
    public void testSubMapInclusiveExclusive() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();

        // subMap from "a" (inclusive) to "c" (exclusive) => a, b
        ConcurrentNavigableMap<String, Integer> sub = m.subMap("a", true, "c", false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertFalse(sub.containsKey("c"));
        assertFalse(sub.containsKey(null));
    }

    @Test
    public void testHeadMapAndTailMap() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();

        // headMap up to "b" (exclusive) => only "a"
        ConcurrentNavigableMap<String, Integer> head = m.headMap("b", false);
        assertEquals(1, head.size());
        assertTrue(head.containsKey("a"));
        assertFalse(head.containsKey("b"));

        // tailMap from "b" (inclusive) => b, c, null
        ConcurrentNavigableMap<String, Integer> tail = m.tailMap("b", true);
        assertEquals(3, tail.size());
        assertTrue(tail.containsKey("b"));
        assertTrue(tail.containsKey("c"));
        assertTrue(tail.containsKey(null));
    }

    // ----------------------------------------------------------------------
    // Descending map and its key set
    // ----------------------------------------------------------------------
    @Test
    public void testDescendingMapViews() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();

        ConcurrentNavigableMap<String, Integer> desc = m.descendingMap();

        // Order should be null, c, b, a
        Iterator<String> it = desc.keySet().iterator();
        assertEquals(null, it.next());
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());

        // firstKey in descending map is null (because null is first in descending order)
        assertNull(desc.firstKey());
        // lastKey in descending map is "a"
        assertEquals("a", desc.lastKey());
    }

    // ----------------------------------------------------------------------
    // KeyNavigableSet specific behaviours
    // ----------------------------------------------------------------------
    @Test
    public void testKeyNavigableSetContainsRemoveClear() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();
        NavigableSet<String> keys = m.navigableKeySet();

        // contains
        assertTrue(keys.contains("b"));
        assertTrue(keys.contains(null));
        assertFalse(keys.contains("z"));

        // remove a non‑null key
        assertTrue(keys.remove("b"));
        assertFalse(m.containsKey("b"));
        assertEquals(3, m.size());

        // remove null key
        assertTrue(keys.remove(null));
        assertFalse(m.containsKey(null));
        assertEquals(2, m.size());

        // clear via set
        keys.clear();
        assertTrue(m.isEmpty());
    }

    @Test
    public void testKeyNavigableSetDirectionalMethods() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();
        NavigableSet<String> keys = m.navigableKeySet();

        // lower / floor / ceiling / higher for "b"
        assertEquals("a", keys.lower("b"));
        assertEquals("b", keys.floor("b"));
        assertEquals("b", keys.ceiling("b"));
        assertEquals("c", keys.higher("b"));

        // null key is last in ascending view: lower(null) -> "c"
        assertEquals("c", keys.lower(null));
        // floor(null) -> null
        assertEquals(null, keys.floor(null));
        // ceiling(null) -> null
        assertEquals(null, keys.ceiling(null));
        // higher(null) -> null (no higher)
        assertNull(keys.higher(null));
    }

    @Test
    public void testKeyNavigableSetPollFirstAndLast() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();
        NavigableSet<String> keys = m.navigableKeySet();

        // pollFirst should return "a"
        assertEquals("a", keys.pollFirst());
        assertFalse(m.containsKey("a"));
        assertEquals(3, m.size());

        // pollLast should return null (null key is last)
        assertEquals(null, keys.pollLast());
        assertFalse(m.containsKey(null));
        assertEquals(2, m.size());
    }

    @Test
    public void testKeyNavigableSetIteratorOrder() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();
        NavigableSet<String> keys = m.navigableKeySet();

        Iterator<String> it = keys.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testKeyNavigableSetDescendingIterator() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();
        NavigableSet<String> keys = m.navigableKeySet();

        Iterator<String> descIt = keys.descendingIterator();
        assertEquals(null, descIt.next());
        assertEquals("c", descIt.next());
        assertEquals("b", descIt.next());
        assertEquals("a", descIt.next());
        assertFalse(descIt.hasNext());
    }

    @Test
    public void testKeyNavigableSetSubSetHeadSetTailSet() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();
        NavigableSet<String> keys = m.navigableKeySet();

        // subSet from "a" (inclusive) to "c" (exclusive) => a, b
        NavigableSet<String> sub = keys.subSet("a", true, "c", false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains("a"));
        assertTrue(sub.contains("b"));
        assertFalse(sub.contains("c"));
        assertFalse(sub.contains(null));

        // headSet up to "b" (exclusive) => a
        NavigableSet<String> head = keys.headSet("b", false);
        assertEquals(1, head.size());
        assertTrue(head.contains("a"));
        assertFalse(head.contains("b"));

        // tailSet from "b" (inclusive) => b, c, null
        NavigableSet<String> tail = keys.tailSet("b", true);
        assertEquals(3, tail.size());
        assertTrue(tail.contains("b"));
        assertTrue(tail.contains("c"));
        assertTrue(tail.contains(null));
    }

    // ----------------------------------------------------------------------
    // Custom comparator handling
    // ----------------------------------------------------------------------
    @Test
    public void testCustomComparatorPreserved() {
        Comparator<String> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<String, Integer> revMap = new ConcurrentNavigableMapNullSafe<>(reverse);

        revMap.put("a", 1);
        revMap.put("b", 2);
        revMap.put(null, 99); // null still last in *ascending* view, but reverse comparator affects non‑null ordering

        // Verify that comparator() returns the original comparator instance
        assertSame(reverse, revMap.comparator());

        // Ascending key set (as provided by the map) should be: "b", "a", null
        List<String> asc = new ArrayList<>(revMap.keySet());
        assertEquals(Arrays.asList("b", "a", null), asc);

        // Descending view should reverse that order: null, a, b
        List<String> desc = new ArrayList<>(revMap.descendingKeySet());
        assertEquals(Arrays.asList(null, "a", "b"), desc);
    }

    // ----------------------------------------------------------------------
    // Equality & hashCode of wrapped entries
    // ----------------------------------------------------------------------
    @Test
    public void testEntryEqualityAndHashCode() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();
        Entry<String, Integer> e1 = m.firstEntry();   // "a"=1
        Entry<String, Integer> e2 = m.entrySet().iterator().next(); // should be same as e1

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());

        // Change value via setValue and verify equality updates
        e1.setValue(42);
        assertEquals(Integer.valueOf(42), m.get("a"));
        assertEquals(e1, m.firstEntry());
    }

    // ----------------------------------------------------------------------
    // toString of wrapped entry
    // ----------------------------------------------------------------------
    @Test
    public void testEntryToString() {
        ConcurrentNavigableMapNullSafe<String, Integer> m = createSampleMap();
        Entry<String, Integer> e = m.firstEntry();
        assertEquals("a=1", e.toString());

        // Null key representation
        Entry<String, Integer> nullEntry = m.lastEntry();
        assertEquals("null=99", nullEntry.toString());
    }
}
