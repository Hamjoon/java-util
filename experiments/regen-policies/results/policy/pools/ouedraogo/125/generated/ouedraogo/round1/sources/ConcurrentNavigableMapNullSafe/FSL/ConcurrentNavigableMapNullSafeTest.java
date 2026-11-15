package com.cedarsoftware.util;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ConcurrentNavigableMapNullSafeTest {

    /** Helper to create a map with natural ordering (String keys). */
    private ConcurrentNavigableMapNullSafe<String, String> createStringMap() {
        return new ConcurrentNavigableMapNullSafe<>();
    }

    /** Helper to create a map with a custom comparator (reverse order). */
    private ConcurrentNavigableMapNullSafe<Integer, String> createReverseIntMap() {
        Comparator<Integer> reverse = Comparator.<Integer>reverseOrder();
        return new ConcurrentNavigableMapNullSafe<>(reverse);
    }

    /* ---------------------------------------------------------------------- */
    /* Basic put / get / null‑key handling                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetWithNullKeyAndValue() {
        ConcurrentNavigableMapNullSafe<String, String> map = createStringMap();

        // null key with non‑null value
        assertNull(map.put(null, "nullKey"));
        assertEquals("nullKey", map.get(null));

        // non‑null key with null value
        assertNull(map.put("a", null));
        assertNull(map.get("a"));

        // replace existing null key
        assertEquals("nullKey", map.put(null, "newNull"));
        assertEquals("newNull", map.get(null));

        // size should be 2
        assertEquals(2, map.size());
    }

    @Test
    public void testContainsKeyAndValueIncludingNulls() {
        ConcurrentNavigableMapNullSafe<String, String> map = createStringMap();
        map.put(null, "nullKey");
        map.put("x", null);
        map.put("y", "val");

        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsValue(null));
        assertTrue(map.containsValue("nullKey"));
        assertFalse(map.containsKey("missing"));
        assertFalse(map.containsValue("missing"));
    }

    /* ---------------------------------------------------------------------- */
    /* Comparator handling                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComparatorReturnsOriginal() {
        ConcurrentNavigableMapNullSafe<String, String> natural = new ConcurrentNavigableMapNullSafe<>();
        assertNull(natural.comparator());

        ConcurrentNavigableMapNullSafe<Integer, String> reverse = createReverseIntMap();
        assertNotNull(reverse.comparator());
        assertEquals(Comparator.<Integer>reverseOrder(), reverse.comparator());
    }

    /* ---------------------------------------------------------------------- */
    /* Ordering of null key (ascending & descending)                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNullKeyOrderingInAscendingAndDescendingViews() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = createStringMap();
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);
        map.put("c", 3);

        // Ascending order: null key should be last
        Iterator<String> ascIter = map.navigableKeySet().iterator();
        List<String> ascKeys = new ArrayList<>();
        while (ascIter.hasNext()) {
            ascKeys.add(ascIter.next());
        }
        assertEquals(Arrays.asList("a", "b", "c", null), ascKeys);

        // Descending order: null key should be first
        Iterator<String> descIter = map.descendingKeySet().iterator();
        List<String> descKeys = new ArrayList<>();
        while (descIter.hasNext()) {
            descKeys.add(descIter.next());
        }
        assertEquals(Arrays.asList(null, "c", "b", "a"), descKeys);
    }

    /* ---------------------------------------------------------------------- */
    /* Navigational entry methods (lower / floor / ceiling / higher)        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNavigationalEntryMethods() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = createStringMap();
        map.put("a", 1);
        map.put("c", 3);
        map.put(null, 99);
        map.put("e", 5);

        // lowerEntry for "c" should be "a"
        assertEquals("a", map.lowerEntry("c").getKey());
        assertEquals(Integer.valueOf(1), map.lowerEntry("c").getValue());

        // floorEntry for "c" should be "c"
        assertEquals("c", map.floorEntry("c").getKey());

        // ceilingEntry for "b" should be "c"
        assertEquals("c", map.ceilingEntry("b").getKey());

        // higherEntry for "c" should be "e"
        assertEquals("e", map.higherEntry("c").getKey());

        // firstEntry (ascending) should be "a"
        assertEquals("a", map.firstEntry().getKey());

        // lastEntry (ascending) should be null (sentinel)
        assertNull(map.lastEntry().getKey());

        // pollFirstEntry removes "a"
        Map.Entry<String, Integer> polledFirst = map.pollFirstEntry();
        assertEquals("a", polledFirst.getKey());
        assertEquals(Integer.valueOf(1), polledFirst.getValue());
        assertFalse(map.containsKey("a"));

        // pollLastEntry removes null key
        Map.Entry<String, Integer> polledLast = map.pollLastEntry();
        assertNull(polledLast.getKey());
        assertEquals(Integer.valueOf(99), polledLast.getValue());
        assertFalse(map.containsKey(null));
    }

    /* ---------------------------------------------------------------------- */
    /* Sub‑map, head‑map, tail‑map with null boundaries                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSubMapIncludingNullKey() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = createStringMap();
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);
        map.put("c", 3);

        // subMap from "a" (inclusive) to null (exclusive) should contain a,b
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("a", true, null, false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertFalse(sub.containsKey("c"));
        assertFalse(sub.containsKey(null));

        // headMap up to null (inclusive) should contain a,b,c,null
        ConcurrentNavigableMap<String, Integer> head = map.headMap(null, true);
        assertEquals(4, head.size());
        assertTrue(head.containsKey(null));

        // tailMap from null (inclusive) should contain only null
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap(null, true);
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
    }

    @Test
    public void testHeadAndTailMapWithCustomComparator() {
        ConcurrentNavigableMapNullSafe<Integer, String> map = createReverseIntMap();
        map.put(5, "five");
        map.put(2, "two");
        map.put(null, "null");
        map.put(8, "eight");

        // With reverse order, higher numeric values are "lower" in order.
        // headMap(toKey=5, inclusive=false) should contain 8 only (since 8 < 5 in reverse order)
        ConcurrentNavigableMap<Integer, String> head = map.headMap(5, false);
        assertEquals(1, head.size());
        assertTrue(head.containsKey(8));

        // tailMap(fromKey=5, inclusive=true) should contain 5,2,null
        ConcurrentNavigableMap<Integer, String> tail = map.tailMap(5, true);
        assertEquals(3, tail.size());
        assertTrue(tail.containsKey(5));
        assertTrue(tail.containsKey(2));
        assertTrue(tail.containsKey(null));
    }

    /* ---------------------------------------------------------------------- */
    /* KeyNavigableSet behavior                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testKeySetBasicOperations() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = createStringMap();
        map.put("x", 10);
        map.put(null, 0);
        map.put("y", 20);

        NavigableSet<String> keySet = map.navigableKeySet();
        assertEquals(3, keySet.size());
        assertTrue(keySet.contains("x"));
        assertTrue(keySet.contains(null));

        // lower/floor/ceiling/higher
        assertNull(keySet.lower(null)); // null is last, nothing lower
        assertEquals("y", keySet.lower("z"));
        assertEquals("x", keySet.floor("x"));
        assertEquals("x", keySet.ceiling("w"));
        assertEquals("y", keySet.higher("x"));

        // pollFirst should remove "x"
        assertEquals("x", keySet.pollFirst());
        assertFalse(map.containsKey("x"));
        assertEquals(2, keySet.size());

        // pollLast should remove null key
        assertNull(keySet.pollLast());
        assertFalse(map.containsKey(null));
        assertEquals(1, keySet.size());
    }

    @Test
    public void testKeySetIteratorAndDescendingIterator() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = createStringMap();
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);
        map.put("c", 3);

        List<String> forward = new ArrayList<>();
        for (String k : map.navigableKeySet()) {
            forward.add(k);
        }
        assertEquals(Arrays.asList("a", "b", "c", null), forward);

        List<String> backward = new ArrayList<>();
        Iterator<String> descIter = map.descendingKeySet().iterator();
        while (descIter.hasNext()) {
            backward.add(descIter.next());
        }
        assertEquals(Arrays.asList(null, "c", "b", "a"), backward);
    }

    @Test
    public void testKeySetSubSetHeadSetTailSet() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = createStringMap();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put(null, 99);

        NavigableSet<String> set = map.navigableKeySet();

        // subSet a..c (inclusive a, exclusive c) => a,b
        NavigableSet<String> sub = set.subSet("a", true, "c", false);
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(sub));

        // headSet up to "c" (exclusive) => a,b
        NavigableSet<String> head = set.headSet("c", false);
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(head));

        // tailSet from "c" (inclusive) => c,null
        NavigableSet<String> tail = set.tailSet("c", true);
        assertEquals(Arrays.asList("c", null), new ArrayList<>(tail));
    }

    @Test
    public void testKeySetRemoveAndClear() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = createStringMap();
        map.put("x", 1);
        map.put("y", 2);
        map.put(null, 0);

        NavigableSet<String> keySet = map.navigableKeySet();

        assertTrue(keySet.remove("y"));
        assertFalse(map.containsKey("y"));
        assertEquals(2, keySet.size());

        assertTrue(keySet.remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(1, keySet.size());

        keySet.clear();
        assertTrue(map.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* Entry set behaviour (wrapEntry)                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEntrySetAndEntryModification() {
        ConcurrentNavigableMapNullSafe<String, String> map = createStringMap();
        map.put("k1", "v1");
        map.put(null, "nullVal");

        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertEquals(2, entrySet.size());

        // Find entry for null key and modify its value via setValue
        for (Map.Entry<String, String> e : entrySet) {
            if (e.getKey() == null) {
                String old = e.setValue("newNull");
                assertEquals("nullVal", old);
                assertEquals("newNull", map.get(null));
            }
        }
    }

    /* ---------------------------------------------------------------------- */
    /* First/last key convenience methods                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFirstAndLastKeyWithNulls() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = createStringMap();
        assertNull(map.firstKey()); // empty map returns null (ConcurrentSkipListMap throws NoSuchElementException, but wrapper handles null via unmask)
        assertNull(map.lastKey());

        map.put("a", 1);
        map.put(null, 99);
        map.put("b", 2);

        assertEquals("a", map.firstKey());
        assertNull(map.lastKey()); // null key considered last
    }

    /* ---------------------------------------------------------------------- */
    /* descendingMap view respects null ordering                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDescendingMapKeyOrder() {
        ConcurrentNavigableMapNullSafe<String, Integer> map = createStringMap();
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);
        map.put("c", 3);

        ConcurrentNavigableMap<String, Integer> desc = map.descendingMap();
        List<String> descKeys = new ArrayList<>(desc.keySet());
        assertEquals(Arrays.asList(null, "c", "b", "a"), descKeys);
    }
}
