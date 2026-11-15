package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

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
 * <ul>
 *   <li>Construction (default and with comparator)</li>
 *   <li>Masking / unmasking of {@code null} keys</li>
 *   <li>All navigation methods (subMap, headMap, tailMap, lower/ floor/ ceiling/ higher …)</li>
 *   <li>First / last entry & key behaviour with {@code null} keys</li>
 *   <li>Descending view ordering (null key becomes first)</li>
 *   <li>KeyNavigableSet functionality (contains, remove, iterator, pollFirst/Last, comparator, subset operations)</li>
 *   <li>Entry wrapper setValue handling of {@code null} values</li>
 * </ul>
 *
 * Only the public API of {@code ConcurrentNavigableMapNullSafe} and its
 * super‑type {@code AbstractConcurrentNullSafeMap} are used – no mocks or
 * subclassing of library types.
 */
public class ConcurrentNavigableMapNullSafeTest {

    private ConcurrentNavigableMapNullSafe<String, String> map;
    private ConcurrentNavigableMapNullSafe<String, String> mapWithComparator;

    @Before
    public void setUp() {
        map = new ConcurrentNavigableMapNullSafe<>();
        mapWithComparator = new ConcurrentNavigableMapNullSafe<>(Comparator.reverseOrder());
    }

    /*** Construction & basic put / get ***/
    @Test
    public void testSimplePutGet() {
        assertTrue(map.isEmpty());
        map.put("a", "A");
        map.put("b", "B");
        assertEquals(2, map.size());
        assertEquals("A", map.get("a"));
        assertEquals("B", map.get("b"));
    }

    /*** Null key handling – mask / unmask ***/
    @Test
    public void testNullKeyMasking() throws Exception {
        // Directly invoke protected methods via reflection (allowed because they are in the same package)
        java.lang.reflect.Method mask = AbstractConcurrentNullSafeMap.class.getDeclaredMethod("maskNullKey", Object.class);
        java.lang.reflect.Method unmask = AbstractConcurrentNullSafeMap.class.getDeclaredMethod("unmaskNullKey", Object.class);
        mask.setAccessible(true);
        unmask.setAccessible(true);

        Object sentinel = mask.invoke(map, (Object) null);
        assertNotNull(sentinel);
        assertTrue(sentinel instanceof String);
        // The sentinel should be a non‑null string that is not equal to any real key
        assertFalse("null".equals(sentinel));

        Object back = unmask.invoke(map, sentinel);
        assertNull(back);
    }

    /*** Null key insertion and retrieval ***/
    @Test
    public void testNullKeyInsertion() {
        map.put(null, "NULL_VALUE");
        assertEquals(1, map.size());
        assertEquals("NULL_VALUE", map.get(null));

        // The sentinel should be stored internally, not visible externally
        assertTrue(map.containsKey(null));
        assertFalse(map.keySet().contains("null")); // the literal string "null" is not a key
    }

    /*** Comparator exposure ***/
    @Test
    public void testComparatorExposure() {
        assertNull(map.comparator()); // natural ordering
        assertEquals(Comparator.reverseOrder(), mapWithComparator.comparator());
    }

    /*** Sub‑map boundaries with null key ***/
    @Test
    public void testSubMapWithNullKey() {
        map.put("a", "A");
        map.put("b", "B");
        map.put(null, "NULL");

        // subMap from "a" (inclusive) to null (exclusive) should contain only "a" and "b"
        ConcurrentNavigableMap<String, String> sub = map.subMap("a", true, null, false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertFalse(sub.containsKey(null));

        // subMap from null (inclusive) to "z" (exclusive) should be empty because null is last in ascending order
        ConcurrentNavigableMap<String, String> tail = map.subMap(null, true, "z", false);
        assertTrue(tail.isEmpty());
    }

    /*** Head & tail map with null key ***/
    @Test
    public void testHeadTailMapNullKey() {
        map.put("a", "A");
        map.put("b", "B");
        map.put(null, "NULL");

        // headMap to null (exclusive) should contain only "a" and "b"
        ConcurrentNavigableMap<String, String> head = map.headMap(null, false);
        assertEquals(2, head.size());
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertFalse(head.containsKey(null));

        // tailMap from null (inclusive) should contain only the null key
        ConcurrentNavigableMap<String, String> tail = map.tailMap(null, true);
        assertEquals(1, tail.size());
        assertTrue(tail.containsKey(null));
    }

    /*** lower / floor / ceiling / higher entry & key ***/
    @Test
    public void testNavigationMethods() {
        map.put("a", "A");
        map.put("c", "C");
        map.put(null, "NULL");

        // lower of "b" is "a"
        assertEquals("a", map.lowerKey("b"));
        assertEquals("a", map.lowerEntry("b").getKey());

        // floor of "c" is "c"
        assertEquals("c", map.floorKey("c"));
        assertEquals("c", map.floorEntry("c").getKey());

        // ceiling of "b" is "c"
        assertEquals("c", map.ceilingKey("b"));
        assertEquals("c", map.ceilingEntry("b").getKey());

        // higher of "a" is "c"
        assertEquals("c", map.higherKey("a"));
        assertEquals("c", map.higherEntry("a").getKey());

        // For null key, which is last, lower should be "c"
        assertEquals("c", map.lowerKey(null));
        assertEquals("c", map.lowerEntry(null).getKey());

        // floor of null is null itself
        assertNull(map.floorKey(null));
        assertEquals(null, map.floorEntry(null).getKey());

        // higher of null is null (no greater key)
        assertNull(map.higherKey(null));
        assertNull(map.higherEntry(null));
    }

    /*** First / last entry & key handling of null ***/
    @Test
    public void testFirstLastEntryAndKey() {
        // Empty map – firstKey/lastKey should throw NoSuchElementException from underlying SkipListMap
        try {
            map.firstKey();
            fail("Expected NoSuchElementException");
        } catch (NoSuchElementException ignored) {}

        // Populate
        map.put("a", "A");
        map.put("b", "B");
        map.put(null, "NULL");

        // In ascending view null is last
        assertEquals("a", map.firstKey());
        assertEquals(null, map.lastKey());

        assertEquals("a", map.firstEntry().getKey());
        assertEquals(null, map.lastEntry().getKey());

        // pollFirst removes "a"
        Map.Entry<String, String> polledFirst = map.pollFirstEntry();
        assertEquals("a", polledFirst.getKey());
        assertEquals(2, map.size());
        assertNull(map.get("a"));

        // pollLast removes null key
        Map.Entry<String, String> polledLast = map.pollLastEntry();
        assertNull(polledLast.getKey());
        assertEquals(1, map.size());
        assertFalse(map.containsKey(null));
    }

    /*** Descending map ordering – null key becomes first ***/
    @Test
    public void testDescendingMapOrdering() {
        map.put("a", "A");
        map.put("b", "B");
        map.put(null, "NULL");

        ConcurrentNavigableMap<String, String> desc = map.descendingMap();

        // In descending view, null key should be first
        assertEquals(null, desc.firstKey());
        assertEquals("b", desc.lastKey());

        // Verify iterator order
        Iterator<String> it = desc.keySet().iterator();
        assertEquals(null, it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    /*** KeyNavigableSet basic operations ***/
    @Test
    public void testKeyNavigableSetBasic() {
        map.put("x", "X");
        map.put("y", "Y");
        map.put(null, "NULL");

        NavigableSet<String> keySet = map.navigableKeySet();

        // size & contains
        assertEquals(3, keySet.size());
        assertTrue(keySet.contains("x"));
        assertTrue(keySet.contains(null));
        assertFalse(keySet.contains("z"));

        // remove via set
        assertTrue(keySet.remove("x"));
        assertFalse(keySet.contains("x"));
        assertEquals(2, map.size());

        // clear
        keySet.clear();
        assertTrue(map.isEmpty());
    }

    /*** KeyNavigableSet navigation methods (lower, floor, ceiling, higher) ***/
    @Test
    public void testKeyNavigableSetNavigation() {
        map.put("a", "A");
        map.put("c", "C");
        map.put(null, "NULL");

        NavigableSet<String> ks = map.navigableKeySet();

        // lower of "b" -> "a"
        assertEquals("a", ks.lower("b"));
        // floor of "c" -> "c"
        assertEquals("c", ks.floor("c"));
        // ceiling of "b" -> "c"
        assertEquals("c", ks.ceiling("b"));
        // higher of "a" -> "c"
        assertEquals("c", ks.higher("a"));

        // For null key (last in asc), lower is "c"
        assertEquals("c", ks.lower(null));
        // floor of null is null itself
        assertNull(ks.floor(null));
        // higher of null is null (no greater)
        assertNull(ks.higher(null));
    }

    /*** KeyNavigableSet pollFirst / pollLast ***/
    @Test
    public void testKeyNavigableSetPoll() {
        map.put("a", "A");
        map.put(null, "NULL");

        NavigableSet<String> ks = map.navigableKeySet();

        // pollFirst removes "a"
        assertEquals("a", ks.pollFirst());
        assertFalse(ks.contains("a"));
        assertTrue(ks.contains(null));

        // pollLast removes null key
        assertNull(ks.pollLast());
        assertFalse(ks.contains(null));
        assertTrue(ks.isEmpty());
    }

    /*** KeyNavigableSet comparator matches map comparator ***/
    @Test
    public void testKeyNavigableSetComparator() {
        assertNull(map.navigableKeySet().comparator());
        assertEquals(Comparator.reverseOrder(), mapWithComparator.navigableKeySet().comparator());
    }

    /*** SubSet, headSet, tailSet views ***/
    @Test
    public void testSubsetViews() {
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");
        map.put(null, "NULL");

        NavigableSet<String> ks = map.navigableKeySet();

        // subSet from "a" (inclusive) to "c" (exclusive) => "a", "b"
        NavigableSet<String> sub = ks.subSet("a", true, "c", false);
        assertEquals(2, sub.size());
        assertTrue(sub.contains("a"));
        assertTrue(sub.contains("b"));
        assertFalse(sub.contains("c"));

        // headSet up to "b" (exclusive) => "a"
        NavigableSet<String> head = ks.headSet("b", false);
        assertEquals(1, head.size());
        assertTrue(head.contains("a"));

        // tailSet from "b" (inclusive) => "b", "c", null
        NavigableSet<String> tail = ks.tailSet("b", true);
        assertEquals(3, tail.size());
        assertTrue(tail.contains("b"));
        assertTrue(tail.contains("c"));
        assertTrue(tail.contains(null));
    }

    /*** Entry wrapper setValue with null value ***/
    @Test
    public void testEntrySetValueWithNull() {
        map.put("k", "v");
        Map.Entry<String, String> entry = map.firstEntry();
        assertNotNull(entry);
        assertEquals("v", entry.getValue());

        // Change value to null
        entry.setValue(null);
        assertNull(map.get("k"));
        // The entry should now reflect the null value
        assertNull(entry.getValue());
    }

    /*** Verify that map respects natural ordering for mixed key types ***/
    @Test
    public void testMixedKeyTypesOrdering() {
        // Use raw map to insert an Integer key alongside String keys
        ConcurrentNavigableMap<Object, Object> raw = new ConcurrentSkipListMap<>(ConcurrentNavigableMapNullSafe.wrapComparator(null));
        ConcurrentNavigableMapNullSafe<Object, Object> mixedMap = new ConcurrentNavigableMapNullSafe<>(raw, null);

        mixedMap.put("a", "A");
        mixedMap.put(1, "ONE");
        mixedMap.put(null, "NULL");

        // Expected order: 1 (class name "java.lang.Integer"), "a" (class name "java.lang.String"), null (sentinel last)
        Iterator<Object> it = mixedMap.keySet().iterator();
        assertEquals(1, it.next());
        assertEquals("a", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    /*** Ensure that computeIfAbsent and merge work with null values ***/
    @Test
    public void testComputeAndMergeWithNullValues() {
        // computeIfAbsent with a function returning null should store null (masked internally)
        map.computeIfAbsent("k", k -> null);
        assertTrue(map.containsKey("k"));
        assertNull(map.get("k"));

        // merge with null old value and a non‑null remapping function
        map.put("m", null);
        map.merge("m", "M", (oldV, newV) -> oldV == null ? newV : oldV);
        assertEquals("M", map.get("m"));
    }

    /*** Verify descendingKeySet returns a NavigableSet with proper order ***/
    @Test
    public void testDescendingKeySetOrder() {
        map.put("a", "A");
        map.put("b", "B");
        map.put(null, "NULL");

        NavigableSet<String> descSet = map.descendingKeySet();
        Iterator<String> it = descSet.iterator();

        // In descending order null first, then "b", then "a"
        assertNull(it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }
}
