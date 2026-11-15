package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link SealableNavigableMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>All constructors</li>
 *   <li>Immutable (read‑only) API behavior</li>
 *   <li>Mutable API behavior when the map is sealed vs. unsealed</li>
 *   <li>Views returned by {@code keySet()}, {@code values()}, {@code entrySet()},
 *       {@code subMap()}, {@code headMap()}, {@code tailMap()}, {@code descendingMap()},
 *       {@code navigableKeySet()}, {@code descendingKeySet()}</li>
 *   <li>Edge cases such as empty map operations and null handling</li>
 *   <li>Equality and hash‑code delegation</li>
 * </ul>
 *
 * The {@code sealedSupplier} is backed by an {@link AtomicBoolean} so the sealed state
 * can be toggled during a test without recreating the map.
 */
public class SealableNavigableMapTest {

    /** Supplier that reflects the current value of {@link #sealed} */
    private Supplier<Boolean> sealedSupplier;
    /** Controls the sealed state for the map under test */
    private AtomicBoolean sealed;

    /** A fresh map for each test method */
    private SealableNavigableMap<Integer, String> map;

    @Before
    public void setUp() {
        sealed = new AtomicBoolean(false);
        sealedSupplier = sealed::get;
        map = new SealableNavigableMap<>(sealedSupplier);
    }

    /* -----------------------------------------------------------
     *  Helper methods
     * ----------------------------------------------------------- */

    /** Populate the map with a deterministic set of entries */
    private void populateMap() {
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(4, "four");
        map.put(5, "five");
    }

    /** Returns a new map that is a copy of {@code map} (used for equality tests) */
    private Map<Integer, String> copyOfMap() {
        Map<Integer, String> copy = new TreeMap<>();
        for (Map.Entry<Integer, String> e : map.entrySet()) {
            copy.put(e.getKey(), e.getValue());
        }
        return copy;
    }

    /* -----------------------------------------------------------
     *  Constructor tests
     * ----------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testSortedMapConstructorCopiesEntries() {
        SortedMap<Integer, String> src = new TreeMap<>();
        src.put(10, "ten");
        src.put(20, "twenty");

        SealableNavigableMap<Integer, String> m = new SealableNavigableMap<>(src, sealedSupplier);
        assertEquals(2, m.size());
        assertEquals("ten", m.get(10));
        assertEquals("twenty", m.get(20));
    }

    @Test
    public void testNavigableMapConstructorWrapsWithoutCopy() {
        NavigableMap<Integer, String> src = new TreeMap<>();
        src.put(100, "hundred");
        src.put(200, "two hundred");

        SealableNavigableMap<Integer, String> wrapper = new SealableNavigableMap<>(src, sealedSupplier);
        // Modifying the original should reflect in the wrapper (unsealed)
        src.put(300, "three hundred");
        assertEquals("three hundred", wrapper.get(300));
    }

    /* -----------------------------------------------------------
     *  Immutable API tests (read‑only behavior)
     * ----------------------------------------------------------- */

    @Test
    public void testImmutableOperationsReflectUnderlyingMap() {
        populateMap();

        assertFalse(map.isEmpty());
        assertEquals(5, map.size());

        assertTrue(map.containsKey(3));
        assertTrue(map.containsValue("four"));
        assertFalse(map.containsKey(999));

        assertEquals("three", map.get(3));
        assertEquals(Integer.valueOf(1), map.firstKey());
        assertEquals(Integer.valueOf(5), map.lastKey());

        assertEquals(Integer.valueOf(2), map.lowerKey(3));
        assertEquals(Integer.valueOf(3), map.floorKey(3));
        assertEquals(Integer.valueOf(3), map.ceilingKey(3));
        assertEquals(Integer.valueOf(4), map.higherKey(3));

        assertEquals(Integer.valueOf(1), map.firstEntry().getKey());
        assertEquals(Integer.valueOf(5), map.lastEntry().getKey());

        // toString, equals and hashCode are delegated
        String txt = map.toString();
        assertTrue(txt.contains("one") && txt.contains("five"));

        assertEquals(copyOfMap(), map);
        assertEquals(copyOfMap().hashCode(), map.hashCode());
    }

    @Test(expected = NoSuchElementException.class)
    public void testFirstKeyOnEmptyMapThrows() {
        map.firstKey(); // should throw
    }

    @Test(expected = NoSuchElementException.class)
    public void testLastKeyOnEmptyMapThrows() {
        map.lastKey(); // should throw
    }

    /* -----------------------------------------------------------
     *  Mutable API tests – unsealed (allowed)
     * ----------------------------------------------------------- */

    @Test
    public void testPutAndRemoveWhenUnsealed() {
        assertNull(map.put(1, "one"));
        assertEquals("one", map.get(1));

        assertEquals("one", map.put(1, "uno"));
        assertEquals("uno", map.get(1));

        assertEquals("uno", map.remove(1));
        assertNull(map.get(1));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testPutAllAndClearWhenUnsealed() {
        Map<Integer, String> other = new HashMap<>();
        other.put(10, "ten");
        other.put(20, "twenty");

        map.putAll(other);
        assertEquals(2, map.size());
        assertEquals("ten", map.get(10));
        assertEquals("twenty", map.get(20));

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testPollFirstAndLastEntryWhenUnsealed() {
        populateMap();

        Map.Entry<Integer, String> first = map.pollFirstEntry();
        assertEquals(Integer.valueOf(1), first.getKey());
        assertEquals("one", first.getValue());
        assertFalse(map.containsKey(1));

        Map.Entry<Integer, String> last = map.pollLastEntry();
        assertEquals(Integer.valueOf(5), last.getKey());
        assertEquals("five", last.getValue());
        assertFalse(map.containsKey(5));

        assertEquals(3, map.size());
    }

    /* -----------------------------------------------------------
     *  Mutable API tests – sealed (should reject)
     * ----------------------------------------------------------- */

    @Test
    public void testMutableMethodsThrowWhenSealed() {
        populateMap();
        sealed.set(true); // seal the map

        // put
        try {
            map.put(6, "six");
            fail("Expected UnsupportedOperationException for put()");
        } catch (UnsupportedOperationException ignored) {}

        // remove
        try {
            map.remove(1);
            fail("Expected UnsupportedOperationException for remove()");
        } catch (UnsupportedOperationException ignored) {}

        // putAll
        try {
            map.putAll(Collections.singletonMap(7, "seven"));
            fail("Expected UnsupportedOperationException for putAll()");
        } catch (UnsupportedOperationException ignored) {}

        // clear
        try {
            map.clear();
            fail("Expected UnsupportedOperationException for clear()");
        } catch (UnsupportedOperationException ignored) {}

        // pollFirstEntry
        try {
            map.pollFirstEntry();
            fail("Expected UnsupportedOperationException for pollFirstEntry()");
        } catch (UnsupportedOperationException ignored) {}

        // pollLastEntry
        try {
            map.pollLastEntry();
            fail("Expected UnsupportedOperationException for pollLastEntry()");
        } catch (UnsupportedOperationException ignored) {}
    }

    /* -----------------------------------------------------------
     *  View collections – keySet, values, entrySet
     * ----------------------------------------------------------- */

    @Test
    public void testKeySetViewRespectsSealedState() {
        populateMap();

        Set<Integer> ks = map.keySet();

        // Unsealed – mutation allowed
        assertTrue(ks.add(6));
        assertTrue(map.containsKey(6));

        // Seal and attempt mutation
        sealed.set(true);
        try {
            ks.add(7);
            fail("Expected UnsupportedOperationException from keySet.add() when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // Removal also prohibited when sealed
        try {
            ks.remove(1);
            fail("Expected UnsupportedOperationException from keySet.remove() when sealed");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testValuesViewRespectsSealedState() {
        populateMap();

        Collection<String> vals = map.values();

        // Unsealed – mutation allowed
        assertTrue(vals.add("six"));
        assertTrue(map.containsValue("six"));

        // Seal and try to add
        sealed.set(true);
        try {
            vals.add("seven");
            fail("Expected UnsupportedOperationException from values.add() when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // Remove should also be blocked
        try {
            vals.remove("one");
            fail("Expected UnsupportedOperationException from values.remove() when sealed");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testEntrySetViewRespectsSealedState() {
        populateMap();

        Set<Map.Entry<Integer, String>> entries = map.entrySet();

        // Unsealed – mutation allowed via iterator
        Iterator<Map.Entry<Integer, String>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, String> e = it.next();
            if (e.getKey().equals(2)) {
                e.setValue("deux");
            }
        }
        assertEquals("deux", map.get(2));

        // Seal and attempt mutation through entry.setValue()
        sealed.set(true);
        it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, String> e = it.next();
            if (e.getKey().equals(3)) {
                try {
                    e.setValue("trois");
                    fail("Expected UnsupportedOperationException from entry.setValue() when sealed");
                } catch (UnsupportedOperationException ignored) {}
            }
        }
    }

    /* -----------------------------------------------------------
     *  Sub‑map, head‑map, tail‑map, descending map & navigable key set
     * ----------------------------------------------------------- */

    @Test
    public void testSubMapViewReflectsChanges() {
        populateMap(); // 1..5

        NavigableMap<Integer, String> sub = map.subMap(2, true, 4, true); // keys 2,3,4
        assertEquals(3, sub.size());
        assertTrue(sub.containsKey(3));

        // Mutate through sub‑map
        sub.put(3, "trois");
        assertEquals("trois", map.get(3));

        // Remove through sub‑map
        sub.remove(2);
        assertFalse(map.containsKey(2));
    }

    @Test
    public void testHeadMapAndTailMapSealedBehavior() {
        populateMap(); // 1..5

        // headMap up to 3 (exclusive)
        NavigableMap<Integer, String> head = map.headMap(3, false);
        assertEquals(2, head.size()); // 1,2

        // tailMap from 3 (inclusive)
        NavigableMap<Integer, String> tail = map.tailMap(3, true);
        assertEquals(3, tail.size()); // 3,4,5

        // Seal map and ensure sub‑views also reject mutation
        sealed.set(true);
        try {
            head.put(0, "zero");
            fail("Expected UnsupportedOperationException from headMap.put() when sealed");
        } catch (UnsupportedOperationException ignored) {}

        try {
            tail.remove(4);
            fail("Expected UnsupportedOperationException from tailMap.remove() when sealed");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testDescendingMapOrderAndMutability() {
        populateMap(); // 1..5

        NavigableMap<Integer, String> desc = map.descendingMap();
        Iterator<Integer> it = desc.keySet().iterator();

        // Verify descending order
        List<Integer> expected = Arrays.asList(5, 4, 3, 2, 1);
        List<Integer> actual = new ArrayList<>();
        while (it.hasNext()) {
            actual.add(it.next());
        }
        assertEquals(expected, actual);

        // Unsealed – mutation allowed via descending map
        desc.put(0, "zero");
        assertEquals("zero", map.get(0));
        assertTrue(map.containsKey(0));

        // Seal and verify mutation is blocked on descending view
        sealed.set(true);
        try {
            desc.remove(0);
            fail("Expected UnsupportedOperationException from descendingMap.remove() when sealed");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testNavigableKeySetAndDescendingKeySetSealedBehavior() {
        populateMap(); // 1..5

        NavigableSet<Integer> navSet = map.navigableKeySet();
        assertTrue(navSet.contains(3));
        assertEquals(5, navSet.size());

        // Unsealed – mutation allowed
        assertTrue(navSet.remove(2));
        assertFalse(map.containsKey(2));

        // Seal and verify mutation blocked
        sealed.set(true);
        try {
            navSet.add(6);
            fail("Expected UnsupportedOperationException from navigableKeySet.add() when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // Descending key set
        NavigableSet<Integer> descSet = map.descendingKeySet();
        Iterator<Integer> it = descSet.iterator();
        List<Integer> descOrder = new ArrayList<>();
        while (it.hasNext()) {
            descOrder.add(it.next());
        }
        List<Integer> expected = Arrays.asList(5, 4, 3, 1); // 2 was removed earlier
        assertEquals(expected, descOrder);
    }

    /* -----------------------------------------------------------
     *  Equality & hashCode delegation
     * ----------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeDelegateToUnderlyingMap() {
        populateMap();

        Map<Integer, String> copy = new TreeMap<>(copyOfMap());
        assertEquals(copy, map);
        assertEquals(copy.hashCode(), map.hashCode());

        // Change underlying map and verify inequality
        map.put(6, "six");
        assertNotEquals(copy, map);
    }

    /* -----------------------------------------------------------
     *  Null handling (ConcurrentSkipListMap does not allow null keys)
     * ----------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testPutNullKeyThrows() {
        map.put(null, "nullKey");
    }

    @Test(expected = NullPointerException.class)
    public void testPutNullValueAllowedButKeyMustBeNonNull() {
        // Null values are allowed in ConcurrentSkipListMap
        map.put(10, null);
        assertTrue(map.containsKey(10));
        assertNull(map.get(10));
    }
}
