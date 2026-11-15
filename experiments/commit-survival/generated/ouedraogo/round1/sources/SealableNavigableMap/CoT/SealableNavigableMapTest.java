package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class SealableNavigableMapTest {

    private SealableNavigableMap<Integer, String> mutableMap;
    private SealableNavigableMap<Integer, String> sealedMap;
    private AtomicBoolean sealedFlag;

    @Before
    public void setUp() {
        // supplier that can be toggled at runtime
        sealedFlag = new AtomicBoolean(false);
        Supplier<Boolean> supplier = sealedFlag::get;

        // start with a few entries
        SortedMap<Integer, String> initial = new TreeMap<>();
        initial.put(1, "one");
        initial.put(2, "two");
        initial.put(3, "three");

        mutableMap = new SealableNavigableMap<>(initial, supplier);
        sealedMap   = new SealableNavigableMap<>(initial, supplier);
    }

    /* -------------------------------------------------
     *  Immutable API tests (should work irrespective of sealed state)
     * ------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        Map<Integer, String> other = new TreeMap<>();
        other.put(1, "one");
        other.put(2, "two");
        other.put(3, "three");

        assertTrue(mutableMap.equals(other));
        assertEquals(other.hashCode(), mutableMap.hashCode());
    }

    @Test
    public void testToStringContainsEntries() {
        String s = mutableMap.toString();
        assertTrue(s.contains("1=one"));
        assertTrue(s.contains("2=two"));
        assertTrue(s.contains("3=three"));
    }

    @Test
    public void testIsEmptyAndSize() {
        assertFalse(mutableMap.isEmpty());
        assertEquals(3, mutableMap.size());

        mutableMap.clear();
        assertTrue(mutableMap.isEmpty());
        assertEquals(0, mutableMap.size());
    }

    @Test
    public void testContainsKeyAndValue() {
        assertTrue(mutableMap.containsKey(2));
        assertFalse(mutableMap.containsKey(99));

        assertTrue(mutableMap.containsValue("three"));
        assertFalse(mutableMap.containsValue("four"));
    }

    @Test
    public void testGetAndComparator() {
        assertEquals("two", mutableMap.get(2));
        assertNull(mutableMap.get(99));
        // TreeMap uses natural ordering for Integer keys
        assertNotNull(mutableMap.comparator());
        assertEquals(Integer::compareTo, mutableMap.comparator());
    }

    @Test
    public void testFirstAndLastKey() {
        assertEquals(Integer.valueOf(1), mutableMap.firstKey());
        assertEquals(Integer.valueOf(3), mutableMap.lastKey());
    }

    @Test(expected = NoSuchElementException.class)
    public void testFirstKeyWhenEmpty() {
        SealableNavigableMap<Integer, String> empty = new SealableNavigableMap<>(Collections.emptySortedMap(), () -> false);
        empty.firstKey(); // should throw NoSuchElementException
    }

    @Test(expected = NoSuchElementException.class)
    public void testLastKeyWhenEmpty() {
        SealableNavigableMap<Integer, String> empty = new SealableNavigableMap<>(Collections.emptySortedMap(), () -> false);
        empty.lastKey(); // should throw NoSuchElementException
    }

    @Test
    public void testEntrySetAndKeySetAndValues() {
        Set<Integer> keys = mutableMap.keySet();
        assertTrue(keys.containsAll(Arrays.asList(1, 2, 3)));
        assertEquals(3, keys.size());

        Collection<String> values = mutableMap.values();
        assertTrue(values.containsAll(Arrays.asList("one", "two", "three")));
        assertEquals(3, values.size());

        Set<Map.Entry<Integer, String>> entries = mutableMap.entrySet();
        assertEquals(3, entries.size());
        for (Map.Entry<Integer, String> e : entries) {
            assertEquals(""+e.getKey(), e.getValue());
        }
    }

    @Test
    public void testNavigationalEntries() {
        assertEquals(Integer.valueOf(1), mutableMap.lowerKey(2));
        assertEquals(Integer.valueOf(2), mutableMap.floorKey(2));
        assertEquals(Integer.valueOf(2), mutableMap.ceilingKey(2));
        assertEquals(Integer.valueOf(3), mutableMap.higherKey(2));

        assertNull(mutableMap.lowerKey(1));
        assertNull(mutableMap.higherKey(3));
    }

    @Test
    public void testSubMapInclusiveExclusive() {
        // subMap from 1 (inclusive) to 3 (exclusive) should contain 1 and 2
        NavigableMap<Integer, String> sub = mutableMap.subMap(1, true, 3, false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(1));
        assertTrue(sub.containsKey(2));
        assertFalse(sub.containsKey(3));
    }

    @Test
    public void testHeadMapAndTailMap() {
        SortedMap<Integer, String> head = mutableMap.headMap(2);
        assertEquals(1, head.size());
        assertTrue(head.containsKey(1));

        NavigableMap<Integer, String> headIncl = mutableMap.headMap(2, true);
        assertEquals(2, headIncl.size());
        assertTrue(headIncl.containsKey(2));

        SortedMap<Integer, String> tail = mutableMap.tailMap(2);
        assertEquals(2, tail.size());
        assertTrue(tail.containsKey(2));
        assertTrue(tail.containsKey(3));

        NavigableMap<Integer, String> tailExcl = mutableMap.tailMap(2, false);
        assertEquals(1, tailExcl.size());
        assertFalse(tailExcl.containsKey(2));
        assertTrue(tailExcl.containsKey(3));
    }

    @Test
    public void testDescendingMapOrder() {
        NavigableMap<Integer, String> descending = mutableMap.descendingMap();
        Iterator<Integer> it = descending.keySet().iterator();
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
    }

    @Test
    public void testNavigableKeySets() {
        NavigableSet<Integer> navKeySet = mutableMap.navigableKeySet();
        assertEquals(3, navKeySet.size());
        assertTrue(navKeySet.containsAll(Arrays.asList(1, 2, 3)));

        NavigableSet<Integer> descKeySet = mutableMap.descendingKeySet();
        Iterator<Integer> it = descKeySet.iterator();
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
    }

    /* -------------------------------------------------
     *  Mutable API tests – when map is NOT sealed
     * ------------------------------------------------- */

    @Test
    public void testPutAndGetWhenMutable() {
        mutableMap.put(4, "four");
        assertEquals("four", mutableMap.get(4));
        assertEquals(4, mutableMap.size());
    }

    @Test
    public void testRemoveWhenMutable() {
        String removed = mutableMap.remove(2);
        assertEquals("two", removed);
        assertFalse(mutableMap.containsKey(2));
        assertEquals(2, mutableMap.size());
    }

    @Test
    public void testPollFirstAndLastEntryWhenMutable() {
        Map.Entry<Integer, String> first = mutableMap.pollFirstEntry();
        assertEquals(Integer.valueOf(1), first.getKey());
        assertEquals("one", first.getValue());
        assertFalse(mutableMap.containsKey(1));

        Map.Entry<Integer, String> last = mutableMap.pollLastEntry();
        assertEquals(Integer.valueOf(3), last.getKey());
        assertEquals("three", last.getValue());
        assertFalse(mutableMap.containsKey(3));

        assertEquals(1, mutableMap.size()); // only "two" remains
    }

    @Test
    public void testPutAllAndClearWhenMutable() {
        Map<Integer, String> extra = new HashMap<>();
        extra.put(5, "five");
        extra.put(6, "six");
        mutableMap.putAll(extra);
        assertEquals(5, mutableMap.size());
        assertTrue(mutableMap.containsKey(5));
        assertTrue(mutableMap.containsKey(6));

        mutableMap.clear();
        assertTrue(mutableMap.isEmpty());
    }

    /* -------------------------------------------------
     *  Mutable API tests – when map IS sealed (should throw)
     * ------------------------------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testPutWhenSealedThrows() {
        sealedFlag.set(true);
        sealedMap.put(10, "ten");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRemoveWhenSealedThrows() {
        sealedFlag.set(true);
        sealedMap.remove(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPollFirstEntryWhenSealedThrows() {
        sealedFlag.set(true);
        sealedMap.pollFirstEntry();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPollLastEntryWhenSealedThrows() {
        sealedFlag.set(true);
        sealedMap.pollLastEntry();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPutAllWhenSealedThrows() {
        sealedFlag.set(true);
        sealedMap.putAll(Collections.singletonMap(7, "seven"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testClearWhenSealedThrows() {
        sealedFlag.set(true);
        sealedMap.clear();
    }

    /* -------------------------------------------------
     *  View mutability – attempts to modify through the
     *  Set/Collection views must respect the sealed flag.
     * ------------------------------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testKeySetRemoveWhenSealedThrows() {
        sealedFlag.set(true);
        Set<Integer> ks = sealedMap.keySet();
        ks.remove(1);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testEntrySetRemoveWhenSealedThrows() {
        sealedFlag.set(true);
        Set<Map.Entry<Integer, String>> es = sealedMap.entrySet();
        // remove the first entry via iterator
        Iterator<Map.Entry<Integer, String>> it = es.iterator();
        it.next();
        it.remove(); // should throw
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testValuesAddWhenSealedThrows() {
        sealedFlag.set(true);
        Collection<String> vals = sealedMap.values();
        vals.add("new"); // should throw
    }

    /* -------------------------------------------------
     *  Constructors – behaviour verification
     * ------------------------------------------------- */

    @Test
    public void testConstructorWithSortedMapCopiesEntries() {
        SortedMap<Integer, String> src = new TreeMap<>();
        src.put(8, "eight");
        src.put(9, "nine");

        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(src, () -> false);
        assertEquals(2, map.size());
        assertTrue(map.containsKey(8));
        assertTrue(map.containsKey(9));
        // ensure changes to source do NOT affect the SealableNavigableMap
        src.put(10, "ten");
        assertFalse(map.containsKey(10));
    }

    @Test
    public void testConstructorWithNavigableMapWrapsReference() {
        NavigableMap<Integer, String> backing = new TreeMap<>();
        backing.put(11, "eleven");
        SealableNavigableMap<Integer, String> wrapped = new SealableNavigableMap<>(backing, () -> false);
        assertEquals("eleven", wrapped.get(11));

        // mutate through the original map and see the change reflected
        backing.put(12, "twelve");
        assertEquals("twelve", wrapped.get(12));
    }

    /* -------------------------------------------------
     *  Additional edge cases
     * ------------------------------------------------- */

    @Test
    public void testSubMapZeroLength() {
        NavigableMap<Integer, String> sub = mutableMap.subMap(2, true, 2, true);
        assertTrue(sub.isEmpty());
    }

    @Test
    public void testHeadMapWithInclusiveTrueOnFirstElement() {
        NavigableMap<Integer, String> head = mutableMap.headMap(1, true);
        assertEquals(1, head.size());
        assertTrue(head.containsKey(1));
    }

    @Test
    public void testTailMapWithInclusiveFalseOnLastElement() {
        NavigableMap<Integer, String> tail = mutableMap.tailMap(3, false);
        assertTrue(tail.isEmpty());
    }
}
