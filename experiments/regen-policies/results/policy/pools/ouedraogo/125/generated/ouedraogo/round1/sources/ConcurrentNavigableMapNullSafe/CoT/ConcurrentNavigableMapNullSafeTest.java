package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import static org.junit.Assert.*;

public class ConcurrentNavigableMapNullSafeTest {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;
    private ConcurrentNavigableMapNullSafe<String, Integer> revMap; // with custom comparator

    @Before
    public void setUp() {
        map = new ConcurrentNavigableMapNullSafe<>();
        revMap = new ConcurrentNavigableMapNullSafe<>(Comparator.reverseOrder());
    }

    /* ----------------------------------------------------------------------
     *  List of public methods (including inherited ones) that will be tested
     * ----------------------------------------------------------------------
     *
     * Constructors
     *   ConcurrentNavigableMapNullSafe()
     *   ConcurrentNavigableMapNullSafe(Comparator<? super K> comparator)
     *
     * Map interface (inherited from AbstractConcurrentNullSafeMap)
     *   int size()
     *   boolean isEmpty()
     *   boolean containsKey(Object key)
     *   boolean containsValue(Object value)
     *   V get(Object key)
     *   V put(K key, V value)
     *   V remove(Object key)
     *   void putAll(Map<? extends K, ? extends V> m)
     *   void clear()
     *   V getOrDefault(Object key, V defaultValue)
     *   V putIfAbsent(K key, V value)
     *   boolean remove(Object key, Object value)
     *   boolean replace(K key, V oldValue, V newValue)
     *   V replace(K key, V value)
     *   V computeIfAbsent(K key, java.util.function.Function<? super K, ? extends V> mappingFunction)
     *   V compute(K key, java.util.function.BiFunction<? super K, ? super V, ? extends V> remappingFunction)
     *   V merge(K key, V value, java.util.function.BiFunction<? super V, ? super V, ? extends V> remappingFunction)
     *   Collection<V> values()
     *   Set<K> keySet()
     *   Set<Entry<K, V>> entrySet()
     *
     * NavigableMap specific methods
     *   Comparator<? super K> comparator()
     *   ConcurrentNavigableMap<K,V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive)
     *   ConcurrentNavigableMap<K,V> headMap(K toKey, boolean inclusive)
     *   ConcurrentNavigableMap<K,V> tailMap(K fromKey, boolean inclusive)
     *   ConcurrentNavigableMap<K,V> subMap(K fromKey, K toKey)
     *   ConcurrentNavigableMap<K,V> headMap(K toKey)
     *   ConcurrentNavigableMap<K,V> tailMap(K fromKey)
     *   Entry<K,V> lowerEntry(K key)
     *   K lowerKey(K key)
     *   Entry<K,V> floorEntry(K key)
     *   K floorKey(K key)
     *   Entry<K,V> ceilingEntry(K key)
     *   K ceilingKey(K key)
     *   Entry<K,V> higherEntry(K key)
     *   K higherKey(K key)
     *   Entry<K,V> firstEntry()
     *   Entry<K,V> lastEntry()
     *   Entry<K,V> pollFirstEntry()
     *   Entry<K,V> pollLastEntry()
     *   K firstKey()
     *   K lastKey()
     *   NavigableSet<K> navigableKeySet()
     *   NavigableSet<K> descendingKeySet()
     *   ConcurrentNavigableMap<K,V> descendingMap()
     *
     *   // Inner class KeyNavigableSet public methods are exercised via the
     *   // returned NavigableSet instances.
     */

    /* ----------------------------------------------------------------------
     *  Basic functionality tests (put / get / null handling)
     * ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetNormalKey() {
        map.put("a", 1);
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testPutAndGetNullKeyAndNullValue() {
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));
        assertEquals(1, map.size());
    }

    @Test
    public void testReplaceAndRemove() {
        map.put("x", 10);
        map.put("y", 20);
        assertTrue(map.replace("x", 10, 15));
        assertEquals(Integer.valueOf(15), map.get("x"));
        assertFalse(map.replace("x", 10, 30)); // old value no longer matches
        assertEquals(Integer.valueOf(20), map.remove("y"));
        assertFalse(map.containsKey("y"));
    }

    @Test
    public void testPutIfAbsentAndComputeIfAbsent() {
        map.putIfAbsent("k", 100);
        assertEquals(Integer.valueOf(100), map.get("k"));
        map.putIfAbsent("k", 200);
        assertEquals(Integer.valueOf(100), map.get("k")); // unchanged

        map.computeIfAbsent("m", k -> 5);
        assertEquals(Integer.valueOf(5), map.get("m"));
        // computeIfAbsent does not replace existing value
        map.computeIfAbsent("m", k -> 99);
        assertEquals(Integer.valueOf(5), map.get("m"));
    }

    @Test
    public void testMerge() {
        map.put("p", 1);
        map.merge("p", 3, Integer::sum);
        assertEquals(Integer.valueOf(4), map.get("p"));
        map.merge("q", 7, Integer::sum);
        assertEquals(Integer.valueOf(7), map.get("q"));
    }

    /* ----------------------------------------------------------------------
     *  Comparator related tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testNaturalOrderingComparatorIsNull() {
        assertNull(map.comparator());
    }

    @Test
    public void testCustomComparatorIsPreserved() {
        assertNotNull(revMap.comparator());
        // reverse order: "b" should be before "a"
        revMap.put("a", 1);
        revMap.put("b", 2);
        assertEquals("b", revMap.firstKey());
        assertEquals("a", revMap.lastKey());
    }

    /* ----------------------------------------------------------------------
     *  Navigational methods – basic ordering, including null keys
     * ---------------------------------------------------------------------- */

    @Test
    public void testFirstAndLastEntry_WithNullKey() {
        map.put("a", 1);
        map.put(null, 99);
        map.put("z", 2);

        // natural order: null key is considered greater → last
        Entry<String, Integer> first = map.firstEntry();
        Entry<String, Integer> last = map.lastEntry();

        assertEquals("a", first.getKey());
        assertEquals(Integer.valueOf(1), first.getValue());

        assertEquals(null, last.getKey());
        assertEquals(Integer.valueOf(99), last.getValue());
    }

    @Test
    public void testLowerAndHigherEntries() {
        map.put("a", 1);
        map.put("c", 3);
        map.put("e", 5);

        assertEquals("a", map.lowerKey("c"));
        assertEquals(Integer.valueOf(1), map.lowerEntry("c").getValue());

        assertEquals("e", map.higherKey("c"));
        assertEquals(Integer.valueOf(5), map.higherEntry("c").getValue());

        // Edge: lower of first key → null
        assertNull(map.lowerKey("a"));
        assertNull(map.lowerEntry("a"));

        // Edge: higher of last key → null
        assertNull(map.higherKey("e"));
        assertNull(map.higherEntry("e"));
    }

    @Test
    public void testFloorAndCeilingEntries_WithNullKey() {
        map.put("a", 1);
        map.put(null, 99);
        map.put("c", 3);

        // floor of null key = last normal key (c)
        assertEquals("c", map.floorKey(null));
        assertEquals(Integer.valueOf(3), map.floorEntry(null).getValue());

        // ceiling of null key = null itself (since null is considered greatest)
        assertNull(map.ceilingKey(null));
        assertNull(map.ceilingEntry(null));
    }

    @Test
    public void testPollFirstAndLastEntry() {
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);

        Entry<String, Integer> polledFirst = map.pollFirstEntry();
        assertEquals("a", polledFirst.getKey());
        assertEquals(Integer.valueOf(1), polledFirst.getValue());

        Entry<String, Integer> polledLast = map.pollLastEntry();
        assertNull(polledLast.getKey());
        assertEquals(Integer.valueOf(99), polledLast.getValue());

        // after polling, size should be 1 (only "b" remains)
        assertEquals(1, map.size());
        assertEquals("b", map.firstKey());
        assertEquals("b", map.lastKey());
    }

    /* ----------------------------------------------------------------------
     *  Sub‑map, head‑map, tail‑map boundaries (including inclusive/exclusive)
     * ---------------------------------------------------------------------- */

    @Test
    public void testSubMapInclusiveExclusive() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        ConcurrentNavigableMap<String, Integer> sub = map.subMap("b", true, "c", false);
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey("b"));
        assertFalse(sub.containsKey("c"));
    }

    @Test
    public void testHeadMapAndTailMap_WithNullKey() {
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);

        // headMap up to null (exclusive) → contains only "a" and "b"
        ConcurrentNavigableMap<String, Integer> head = map.headMap(null, false);
        assertEquals(2, head.size());
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertFalse(head.containsKey(null));

        // tailMap from null (inclusive) → only null key
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(null, true);
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSubMapFromKeyGreaterThanToKeyThrows() {
        map.put("a", 1);
        map.put("b", 2);
        // fromKey > toKey should trigger IllegalArgumentException in underlying SkipListMap
        map.subMap("b", true, "a", false);
    }

    /* ----------------------------------------------------------------------
     *  Descending map and key set order (null key first in descending view)
     * ---------------------------------------------------------------------- */

    @Test
    public void testDescendingMapOrderWithNullKey() {
        map.put("a", 1);
        map.put("c", 3);
        map.put(null, 99);

        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();

        // In descending view, null key should be first
        assertNull(desc.firstKey());
        assertEquals(Integer.valueOf(99), desc.firstEntry().getValue());

        // Last key in descending view should be the smallest natural key
        assertEquals("a", desc.lastKey());
    }

    @Test
    public void testNavigableKeySetIterationOrder() {
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);

        NavigableSet<String> keySet = map.navigableKeySet();
        Iterator<String> it = keySet.iterator();

        // natural order: "a", "b", null (last)
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testDescendingKeySetIterationOrder() {
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);

        NavigableSet<String> descSet = map.descendingKeySet();
        Iterator<String> it = descSet.iterator();

        // descending order: null, "b", "a"
        assertNull(it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    /* ----------------------------------------------------------------------
     *  KeyNavigableSet specific operations (lower, floor, ceiling, higher, poll)
     * ---------------------------------------------------------------------- */

    @Test
    public void testKeyNavigableSetLowerFloorCeilingHigher() {
        map.put("a", 1);
        map.put("c", 3);
        map.put(null, 99);

        NavigableSet<String> ks = map.navigableKeySet();

        assertNull(ks.lower("a"));          // nothing lower than first key
        assertEquals("a", ks.ceiling("a"));
        assertEquals("c", ks.higher("a"));
        assertEquals("c", ks.floor("c"));
        assertEquals("a", ks.lower("c"));
    }

    @Test
    public void testKeyNavigableSetPollFirstAndPollLast() {
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);

        NavigableSet<String> ks = map.navigableKeySet();

        assertEquals("a", ks.pollFirst());
        assertEquals(null, ks.pollLast()); // null key is last in natural order

        // After polling, only "b" should remain
        assertEquals(1, ks.size());
        assertTrue(ks.contains("b"));
    }

    @Test
    public void testKeyNavigableSetDescendingSetAndIterator() {
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);

        NavigableSet<String> ks = map.navigableKeySet();
        NavigableSet<String> desc = ks.descendingSet();

        Iterator<String> it = desc.iterator();
        // descending order: null, "b", "a"
        assertNull(it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    /* ----------------------------------------------------------------------
     *  Sub‑set, head‑set, tail‑set views from the KeyNavigableSet
     * ---------------------------------------------------------------------- */

    @Test
    public void testKeySetSubSetViews() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put(null, 99);

        NavigableSet<String> ks = map.navigableKeySet();

        NavigableSet<String> sub = ks.subSet("a", true, "c", false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains("a"));
        assertTrue(sub.contains("b"));
        assertFalse(sub.contains("c"));

        NavigableSet<String> head = ks.headSet("b", false);
        assertEquals(1, head.size());
        assertTrue(head.contains("a"));

        NavigableSet<String> tail = ks.tailSet("b", true);
        assertEquals(2, tail.size());
        assertTrue(tail.contains("b"));
        assertTrue(tail.contains("c"));
    }

    /* ----------------------------------------------------------------------
     *  Edge cases: empty map behavior and null comparator handling
     * ---------------------------------------------------------------------- */

    @Test
    public void testOperationsOnEmptyMap() {
        assertTrue(map.isEmpty());
        assertNull(map.firstKey());
        assertNull(map.lastKey());
        assertNull(map.lowerKey("any"));
        assertNull(map.higherKey("any"));
        assertNull(map.pollFirstEntry());
        assertNull(map.pollLastEntry());

        NavigableSet<String> ks = map.navigableKeySet();
        assertTrue(ks.isEmpty());
        assertFalse(ks.iterator().hasNext());
    }

    @Test
    public void testMapWithCustomComparatorHandlesNullKeyCorrectly() {
        // Reverse order comparator: null key is still considered greatest (last in natural,
        // first in descending). Verify behavior matches natural ordering regarding null.
        revMap.put("a", 1);
        revMap.put(null, 99);
        revMap.put("b", 2);

        // In reverse order, "b" is first, "a" is last (null still greatest)
        assertEquals("b", revMap.firstKey());
        assertEquals(null, revMap.lastKey());

        // descending view of reverse order = natural order
        ConcurrentNavigableMap<String, Integer> desc = revMap.descendingMap();
        assertEquals(null, desc.firstKey());
        assertEquals("a", desc.lastKey());
    }
}
