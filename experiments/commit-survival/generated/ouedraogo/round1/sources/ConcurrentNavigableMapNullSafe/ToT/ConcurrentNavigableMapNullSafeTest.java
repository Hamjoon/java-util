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
 * The tests are organised by the public API of the class.  For each
 * method a “typical”, an “edge‑case” and a “error‑scenario” test is
 * provided where appropriate.  The suite deliberately avoids any
 * mocking – it constructs real instances using only the constructors
 * and methods that appear in the source file (or its super‑class
 * {@code AbstractConcurrentNullSafeMap}).
 */
public class ConcurrentNavigableMapNullSafeTest {

    private ConcurrentNavigableMapNullSafe<Integer, String> map;
    private ConcurrentNavigableMapNullSafe<String, Integer> strMap;
    private ConcurrentNavigableMapNullSafe<Integer, String> reverseMap;

    @Before
    public void setUp() {
        map = new ConcurrentNavigableMapNullSafe<>();
        strMap = new ConcurrentNavigableMapNullSafe<>();
        reverseMap = new ConcurrentNavigableMapNullSafe<>(Comparator.<Integer>reverseOrder());
    }

    /* --------------------------------------------------------------------- *
     *  Constructors & basic map operations (inherited from AbstractConcurrentNullSafeMap)
     * --------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructorWithComparatorPreservesComparator() {
        assertEquals(Comparator.<Integer>reverseOrder(), reverseMap.comparator());
    }

    @Test
    public void testPutAndGetWithNullKeyAndNullValue() {
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));
        assertEquals(1, map.size());
    }

    @Test
    public void testPutAllAndClear() {
        Map<Integer, String> src = new HashMap<>();
        src.put(1, "one");
        src.put(2, "two");
        src.put(null, "nullKey");
        map.putAll(src);
        assertEquals(3, map.size());
        assertTrue(map.containsKey(null));
        map.clear();
        assertTrue(map.isEmpty());
    }

    /* --------------------------------------------------------------------- *
     *  Comparator handling (including sentinel handling)
     * --------------------------------------------------------------------- */

    @Test
    public void testNaturalOrderingPlacesNullKeyLast() {
        map.put(10, "ten");
        map.put(5, "five");
        map.put(null, "null");
        assertEquals(Integer.valueOf(5), map.firstKey());
        assertNull(map.lastKey()); // null is considered greater than any other key
    }

    @Test
    public void testDescendingMapPlacesNullKeyFirst() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(null, "null");
        ConcurrentNavigableMap<Integer, String> desc = map.descendingMap();
        assertNull(desc.firstKey()); // null should be first in descending view
        assertEquals(Integer.valueOf(2), desc.lastKey());
    }

    @Test
    public void testCustomComparatorIsReturnedUnchanged() {
        Comparator<Integer> comp = Comparator.comparingInt(i -> i % 5);
        ConcurrentNavigableMapNullSafe<Integer, String> custom = new ConcurrentNavigableMapNullSafe<>(comp);
        assertSame(comp, custom.comparator());
    }

    /* --------------------------------------------------------------------- *
     *  Navigational methods (subMap, headMap, tailMap) – typical & edge cases
     * --------------------------------------------------------------------- */

    @Test
    public void testSubMapExcludesUpperBoundWhenExclusive() {
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        ConcurrentNavigableMap<Integer, String> sub = map.subMap(1, true, 3, false);
        assertTrue(sub.containsKey(1));
        assertFalse(sub.containsKey(3));
        assertEquals(2, sub.size());
    }

    @Test
    public void testSubMapWithNullKeyAsUpperBound() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(null, "null");
        // subMap from 1 (inclusive) to null (exclusive) should contain 1 and 2
        ConcurrentNavigableMap<Integer, String> sub = map.subMap(1, true, null, false);
        assertTrue(sub.containsKey(1));
        assertTrue(sub.containsKey(2));
        assertFalse(sub.containsKey(null));
        assertEquals(2, sub.size());
    }

    @Test
    public void testHeadMapInclusive() {
        map.put(5, "five");
        map.put(10, "ten");
        map.put(null, "null");
        ConcurrentNavigableMap<Integer, String> head = map.headMap(10, true);
        assertTrue(head.containsKey(5));
        assertTrue(head.containsKey(10));
        assertFalse(head.containsKey(null));
        assertEquals(2, head.size());
    }

    @Test
    public void testTailMapExclusive() {
        map.put(5, "five");
        map.put(10, "ten");
        map.put(null, "null");
        ConcurrentNavigableMap<Integer, String> tail = map.tailMap(5, false);
        assertTrue(tail.containsKey(10));
        assertTrue(tail.containsKey(null));
        assertFalse(tail.containsKey(5));
        assertEquals(2, tail.size());
    }

    /* --------------------------------------------------------------------- *
     *  Entry navigation (lower, floor, ceiling, higher) – edge cases with null
     * --------------------------------------------------------------------- */

    @Test
    public void testLowerKeyWithNullKeyReturnsHighestNonNull() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(null, "null");
        assertEquals(Integer.valueOf(2), map.lowerKey(null));
    }

    @Test
    public void testFloorKeyWithExactMatch() {
        map.put(3, "three");
        map.put(null, "null");
        assertEquals(Integer.valueOf(3), map.floorKey(3));
    }

    @Test
    public void testCeilingKeyWithNullKeyReturnsNull() {
        map.put(1, "one");
        map.put(null, "null");
        assertNull(map.ceilingKey(null));
    }

    @Test
    public void testHigherKeyWithHighestKeyReturnsNullKey() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(null, "null");
        assertNull(map.higherKey(2)); // null is considered greater than any other key
    }

    @Test
    public void testEntryNavigationMethodsReturnWrappedEntries() {
        map.put(5, "five");
        map.put(null, "null");
        Map.Entry<Integer, String> lower = map.lowerEntry(null);
        assertNotNull(lower);
        assertEquals(Integer.valueOf(5), lower.getKey());
        assertEquals("five", lower.getValue());

        Map.Entry<Integer, String> floor = map.floorEntry(null);
        assertNotNull(floor);
        assertNull(floor.getKey()); // floor of null is null itself
        assertEquals("null", floor.getValue());

        Map.Entry<Integer, String> ceiling = map.ceilingEntry(5);
        assertNotNull(ceiling);
        assertEquals(Integer.valueOf(5), ceiling.getKey());

        Map.Entry<Integer, String> higher = map.higherEntry(5);
        assertNotNull(higher);
        assertNull(higher.getKey()); // higher than 5 is null sentinel
    }

    /* --------------------------------------------------------------------- *
     *  First / Last entry & key – empty map edge case
     * --------------------------------------------------------------------- */

    @Test
    public void testFirstAndLastEntryOnEmptyMapReturnNull() {
        assertNull(map.firstEntry());
        assertNull(map.lastEntry());
        assertNull(map.firstKey());
        assertNull(map.lastKey());
    }

    @Test
    public void testPollFirstAndLastEntryBehaveLikeFirstAndLast() {
        map.put(2, "two");
        map.put(1, "one");
        map.put(null, "null");
        Map.Entry<Integer, String> firstPolled = map.pollFirstEntry();
        assertEquals(Integer.valueOf(1), firstPolled.getKey());

        Map.Entry<Integer, String> lastPolled = map.pollLastEntry();
        assertNull(lastPolled.getKey()); // null sentinel is last
    }

    /* --------------------------------------------------------------------- *
     *  NavigableKeySet and KeyNavigableSet behaviours
     * --------------------------------------------------------------------- */

    @Test
    public void testKeySetIteratorUnmasksNullKey() {
        map.put(10, "ten");
        map.put(null, "null");
        Iterator<Integer> it = map.navigableKeySet().iterator();
        Set<Integer> keys = new HashSet<>();
        while (it.hasNext()) {
            keys.add(it.next());
        }
        assertTrue(keys.contains(10));
        assertTrue(keys.contains(null));
        assertEquals(2, keys.size());
    }

    @Test
    public void testKeySetContainsHandlesNullCorrectly() {
        map.put(null, "null");
        assertTrue(map.keySet().contains(null));
        assertFalse(map.keySet().contains(5));
    }

    @Test
    public void testKeySetRemoveNullKey() {
        map.put(null, "null");
        assertTrue(map.keySet().remove(null));
        assertFalse(map.containsKey(null));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testKeySetLowerFloorCeilingHigherWithNull() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(null, "null");
        NavigableSet<Integer> ks = map.keySet();

        assertEquals(Integer.valueOf(1), ks.lower(2));
        assertEquals(Integer.valueOf(2), ks.floor(2));
        assertNull(ks.ceiling(null)); // null is the greatest element
        assertNull(ks.higher(2));     // higher than max is null sentinel
    }

    @Test
    public void testKeySetPollFirstAndPollLast() {
        map.put(5, "five");
        map.put(null, "null");
        NavigableSet<Integer> ks = map.keySet();

        assertEquals(Integer.valueOf(5), ks.pollFirst());
        assertNull(ks.pollLast()); // null sentinel is last in ascending view
        assertTrue(ks.isEmpty());
    }

    @Test
    public void testKeySetComparatorMatchesMapComparator() {
        Comparator<Integer> comp = Comparator.naturalOrder();
        ConcurrentNavigableMapNullSafe<Integer, String> c = new ConcurrentNavigableMapNullSafe<>(comp);
        assertSame(comp, c.keySet().comparator());
    }

    @Test
    public void testKeySetFirstAndLast() {
        map.put(3, "three");
        map.put(1, "one");
        map.put(null, "null");
        NavigableSet<Integer> ks = map.keySet();
        assertEquals(Integer.valueOf(1), ks.first());
        assertNull(ks.last()); // null is last
    }

    @Test
    public void testKeySetDescendingSetOrder() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(null, "null");
        NavigableSet<Integer> desc = map.keySet().descendingSet();
        Iterator<Integer> it = desc.iterator();
        List<Integer> order = new ArrayList<>();
        while (it.hasNext()) {
            order.add(it.next());
        }
        // descending order: null, 2, 1
        assertEquals(Arrays.asList((Integer) null, 2, 1), order);
    }

    @Test
    public void testKeySetSubSetInclusiveExclusive() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(null, "null");
        NavigableSet<Integer> sub = map.keySet().subSet(1, true, 3, false);
        assertTrue(sub.contains(1));
        assertTrue(sub.contains(2));
        assertFalse(sub.contains(3));
        assertFalse(sub.contains(null));
        assertEquals(2, sub.size());
    }

    @Test
    public void testKeySetHeadSetAndTailSet() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(null, "null");

        NavigableSet<Integer> head = map.keySet().headSet(2, true);
        assertTrue(head.contains(1));
        assertTrue(head.contains(2));
        assertFalse(head.contains(3));

        NavigableSet<Integer> tail = map.keySet().tailSet(2, false);
        assertTrue(tail.contains(3));
        assertTrue(tail.contains(null));
        assertFalse(tail.contains(2));
    }

    /* --------------------------------------------------------------------- *
     *  Descending map and its key set – verification of ordering inversion
     * --------------------------------------------------------------------- */

    @Test
    public void testDescendingMapKeySetOrderIsReversed() {
        map.put(10, "ten");
        map.put(20, "twenty");
        map.put(null, "null");
        NavigableSet<Integer> descKeys = map.descendingKeySet();
        Iterator<Integer> it = descKeys.iterator();
        List<Integer> order = new ArrayList<>();
        while (it.hasNext()) {
            order.add(it.next());
        }
        // Expected order: null, 20, 10
        assertEquals(Arrays.asList((Integer) null, 20, 10), order);
    }

    @Test
    public void testDescendingMapOperationsReflectOriginalMap() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(null, "null");
        ConcurrentNavigableMap<Integer, String> desc = map.descendingMap();

        // Removing via descending map should affect the original map
        desc.remove(2);
        assertFalse(map.containsKey(2));
        assertEquals(2, map.size());

        // Adding via descending map
        desc.put(3, "three");
        assertTrue(map.containsKey(3));
        assertEquals("three", map.get(3));
    }

    /* --------------------------------------------------------------------- *
     *  Compute/merge functions – ensure null handling does not break internal logic
     * --------------------------------------------------------------------- */

    @Test
    public void testComputeIfAbsentCreatesEntryWithNullKey() {
        Function<Integer, String> mapping = k -> "computed-" + k;
        String result = map.computeIfAbsent(null, k -> "nullComputed");
        assertEquals("nullComputed", result);
        assertTrue(map.containsKey(null));
    }

    @Test
    public void testMergeUpdatesExistingEntryAndHandlesNullValue() {
        map.put(1, "one");
        BiFunction<String, String, String> remapper = (oldV, newV) -> oldV + "+" + newV;
        String merged = map.merge(1, "uno", remapper);
        assertEquals("one+uno", merged);
        assertEquals("one+uno", map.get(1));

        // Merging with null value should remove the entry
        map.merge(1, null, (oldV, newV) -> null);
        assertFalse(map.containsKey(1));
    }

    /* --------------------------------------------------------------------- *
     *  Equality, hashCode and toString – sanity checks
     * --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeWithNullKey() {
        map.put(1, "one");
        map.put(null, "null");
        ConcurrentNavigableMapNullSafe<Integer, String> other = new ConcurrentNavigableMapNullSafe<>();
        other.put(1, "one");
        other.put(null, "null");
        assertEquals(map, other);
        assertEquals(map.hashCode(), other.hashCode());
    }

    @Test
    public void testToStringContainsAllEntriesIncludingNullKey() {
        map.put(5, "five");
        map.put(null, "null");
        String s = map.toString();
        assertTrue(s.contains("5=five"));
        assertTrue(s.contains("null=null"));
    }

    /* --------------------------------------------------------------------- *
     *  Mixed‑type keys – verify wrapComparator can order different classes
     * --------------------------------------------------------------------- */

    @Test
    public void testMixedKeyTypesAreOrderedByClassName() {
        // Use a map with raw Object keys to force mixed types
        ConcurrentNavigableMapNullSafe<Object, String> mixed = new ConcurrentNavigableMapNullSafe<>();
        mixed.put("a", "string");
        mixed.put(1, "integer");
        mixed.put(null, "null");

        // Class names: java.lang.Integer < java.lang.String, so firstKey should be 1
        assertEquals(1, mixed.firstKey());
        // lastKey should be null sentinel (null considered greatest)
        assertNull(mixed.lastKey());
    }
}
