/********************************************************************
 *  JUnit‑4 test suite for {@link com.cedarsoftware.util.SealableNavigableMap}
 *
 *  The tests cover:
 *   • All immutable delegating methods (size, get, containsKey, …)
 *   • All mutable methods (put, remove, clear, pollFirstEntry, …) in
 *     both sealed and unsealed states.
 *   • The view‑map methods (subMap, headMap, tailMap, descendingMap)
 *     and that they honour the same sealed supplier.
 *   • That modifications performed through a view affect the original
 *     map when the map is unsealed and are blocked when sealed.
 *
 *  The implementation deliberately uses only the public API of the
 *  class under test and standard JDK classes – no mocking or external
 *  libraries are required.
 ********************************************************************/

package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class SealableNavigableMapTest {

    /** Supplier that can be toggled between sealed / unsealed */
    private AtomicBoolean sealedFlag;
    private Supplier<Boolean> sealedSupplier;

    /** Underlying mutable map used for the tests */
    private NavigableMap<Integer, String> backingMap;

    /** The SealableNavigableMap instance under test */
    private SealableNavigableMap<Integer, String> sealableMap;

    @Before
    public void setUp() {
        sealedFlag = new AtomicBoolean(false);
        sealedSupplier = sealedFlag::get;

        // use a TreeMap (implements NavigableMap) as the wrapped map
        backingMap = new TreeMap<>();
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    /* -------------------------------------------------------------
     *  Helper methods
     * ------------------------------------------------------------- */

    /** Populate the map with a few deterministic entries. */
    private void populateMap() {
        sealableMap.put(1, "one");
        sealableMap.put(2, "two");
        sealableMap.put(3, "three");
        sealableMap.put(5, "five");
    }

    /** Assert that the given operation on the supplied map throws
     *  UnsupportedOperationException when the map is sealed. */
    private void assertSealed(Runnable operation) {
        sealedFlag.set(true);               // now the map is sealed
        try {
            operation.run();
            fail("Expected UnsupportedOperationException because the map is sealed");
        } catch (UnsupportedOperationException expected) {
            // expected – nothing to do
        } finally {
            sealedFlag.set(false);          // restore unsealed state for other tests
        }
    }

    /* -------------------------------------------------------------
     *  Immutable delegating API tests
     * ------------------------------------------------------------- */

    @Test
    public void testImmutableOperationsDelegateCorrectly() {
        populateMap();

        // size / isEmpty
        assertEquals(4, sealableMap.size());
        assertFalse(sealableMap.isEmpty());

        // containsKey / containsValue
        assertTrue(sealableMap.containsKey(2));
        assertFalse(sealableMap.containsKey(99));
        assertTrue(sealableMap.containsValue("three"));
        assertFalse(sealableMap.containsValue("nine"));

        // get, firstKey, lastKey
        assertEquals("two", sealableMap.get(2));
        assertEquals(Integer.valueOf(1), sealableMap.firstKey());
        assertEquals(Integer.valueOf(5), sealableMap.lastKey());

        // navigation methods
        assertEquals(Integer.valueOf(1), sealableMap.lowerKey(2));
        assertEquals(Integer.valueOf(2), sealableMap.floorKey(2));
        assertEquals(Integer.valueOf(2), sealableMap.ceilingKey(2));
        assertEquals(Integer.valueOf(3), sealableMap.higherKey(2));

        // entry methods
        assertEquals("one", sealableMap.firstEntry().getValue());
        assertEquals("five", sealableMap.lastEntry().getValue());

        // comparator (TreeMap uses natural ordering => null)
        assertNull(sealableMap.comparator());

        // toString / equals / hashCode
        assertEquals(backingMap.toString(), sealableMap.toString());
        assertEquals(backingMap, sealableMap);
        assertEquals(backingMap.hashCode(), sealableMap.hashCode());

        // collections
        Set<Integer> keySet = sealableMap.keySet();
        Collection<String> values = sealableMap.values();
        Set<Map.Entry<Integer, String>> entrySet = sealableMap.entrySet();

        assertEquals(4, keySet.size());
        assertTrue(keySet.containsAll(Arrays.asList(1, 2, 3, 5)));

        assertEquals(4, values.size());
        assertTrue(values.containsAll(Arrays.asList("one", "two", "three", "five")));

        assertEquals(4, entrySet.size());
        for (Map.Entry<Integer, String> e : entrySet) {
            assertEquals(backingMap.get(e.getKey()), e.getValue());
        }
    }

    /* -------------------------------------------------------------
     *  Mutable API – unsealed (should succeed)
     * ------------------------------------------------------------- */

    @Test
    public void testMutableOperationsWhenUnsealed() {
        // put
        assertNull(sealableMap.put(10, "ten"));
        assertEquals("ten", sealableMap.get(10));

        // putAll
        Map<Integer, String> others = new HashMap<>();
        others.put(20, "twenty");
        others.put(30, "thirty");
        sealableMap.putAll(others);
        assertEquals("twenty", sealableMap.get(20));
        assertEquals("thirty", sealableMap.get(30));

        // remove
        assertEquals("twenty", sealableMap.remove(20));
        assertNull(sealableMap.get(20));

        // pollFirstEntry / pollLastEntry
        sealableMap.put(1, "one");
        sealableMap.put(2, "two");
        sealableMap.put(3, "three");
        Map.Entry<Integer, String> first = sealableMap.pollFirstEntry();
        assertEquals(Integer.valueOf(1), first.getKey());
        assertEquals("one", first.getValue());
        assertNull(sealableMap.get(1));

        Map.Entry<Integer, String> last = sealableMap.pollLastEntry();
        assertEquals(Integer.valueOf(3), last.getKey());
        assertEquals("three", last.getValue());
        assertNull(sealableMap.get(3));

        // clear
        sealableMap.clear();
        assertTrue(sealableMap.isEmpty());
    }

    /* -------------------------------------------------------------
     *  Mutable API – sealed (should throw)
     * ------------------------------------------------------------- */

    @Test
    public void testMutableOperationsWhenSealed() {
        // Ensure map has at least one entry to exercise remove / poll
        sealableMap.put(1, "one");
        sealableMap.put(2, "two");

        // put
        assertSealed(() -> sealableMap.put(3, "three"));

        // putAll
        assertSealed(() -> sealableMap.putAll(Collections.singletonMap(4, "four")));

        // remove
        assertSealed(() -> sealableMap.remove(1));

        // pollFirstEntry
        assertSealed(() -> sealableMap.pollFirstEntry());

        // pollLastEntry
        assertSealed(() -> sealableMap.pollLastEntry());

        // clear
        assertSealed(sealableMap::clear);
    }

    /* -------------------------------------------------------------
     *  View maps – subMap, headMap, tailMap, descendingMap
     * ------------------------------------------------------------- */

    @Test
    public void testSubMapViewRespectsSealedState() {
        populateMap(); // map now contains 1,2,3,5

        // subMap from 2 (inclusive) to 5 (exclusive) => should contain 2,3
        NavigableMap<Integer, String> sub = sealableMap.subMap(2, true, 5, false);
        assertTrue(sub instanceof SealableNavigableMap);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey(2));
        assertTrue(sub.containsKey(3));

        // modify through subMap while unsealed – should affect original map
        sub.put(4, "four");
        assertEquals("four", sealableMap.get(4));
        assertTrue(sealableMap.containsKey(4));

        // now seal the map and verify that the subMap becomes immutable
        sealedFlag.set(true);
        try {
            sub.put(6, "six");
            fail("Expected UnsupportedOperationException from sealed subMap");
        } catch (UnsupportedOperationException expected) {
            // ok
        }

        // removal via subMap should also be blocked when sealed
        try {
            sub.remove(2);
            fail("Expected UnsupportedOperationException from sealed subMap");
        } catch (UnsupportedOperationException ignored) {
        }

        // unseal again and verify removal works again
        sealedFlag.set(false);
        sub.remove(2);
        assertFalse(sealableMap.containsKey(2));
        assertEquals(3, sealableMap.size());
    }

    @Test
    public void testHeadAndTailMapViews() {
        populateMap(); // 1,2,3,5

        // headMap up to 3 (exclusive) => 1,2
        NavigableMap<Integer, String> head = sealableMap.headMap(3, false);
        assertEquals(2, head.size());
        assertTrue(head.containsKey(1));
        assertTrue(head.containsKey(2));

        // tailMap from 3 (inclusive) => 3,5
        NavigableMap<Integer, String> tail = sealableMap.tailMap(3, true);
        assertEquals(2, tail.size());
        assertTrue(tail.containsKey(3));
        assertTrue(tail.containsKey(5));

        // Mutate through headMap when unsealed
        head.put(0, "zero");
        assertEquals("zero", sealableMap.get(0));

        // Seal and verify mutation is blocked on tailMap
        sealedFlag.set(true);
        try {
            tail.put(6, "six");
            fail("Expected UnsupportedOperationException from sealed tailMap");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void testDescendingMapAndKeySetSealedBehaviour() {
        populateMap(); // 1,2,3,5

        // descendingMap should present keys in reverse order
        NavigableMap<Integer, String> desc = sealableMap.descendingMap();
        assertTrue(desc instanceof SealableNavigableMap);
        Iterator<Integer> it = desc.keySet().iterator();
        List<Integer> reversed = new ArrayList<>();
        while (it.hasNext()) {
            reversed.add(it.next());
        }
        assertEquals(Arrays.asList(5, 3, 2, 1), reversed);

        // Modifying through descendingMap when unsealed works
        desc.put(4, "four");
        assertEquals("four", sealableMap.get(4));

        // When sealed, both descendingMap and its keySet become immutable
        sealedFlag.set(true);

        // put via descendingMap should fail
        try {
            desc.put(6, "six");
            fail("Expected UnsupportedOperationException from sealed descendingMap");
        } catch (UnsupportedOperationException ignored) {
        }

        // keySet removal should fail
        Set<Integer> dKeySet = desc.keySet();
        try {
            dKeySet.remove(5);
            fail("Expected UnsupportedOperationException from sealed keySet");
        } catch (UnsupportedOperationException ignored) {
        }

        // descendingKeySet should also be a SealableNavigableSet and be immutable
        NavigableSet<Integer> dNavigableKeySet = sealableMap.descendingKeySet();
        try {
            dNavigableKeySet.remove(5);
            fail("Expected UnsupportedOperationException from sealed descendingKeySet");
        } catch (UnsupportedOperationException ignored) {
        }

        // Unseal again and verify removals succeed
        sealedFlag.set(false);
        assertTrue(dKeySet.remove(5));
        assertFalse(sealableMap.containsKey(5));
    }

    /* -------------------------------------------------------------
     *  Additional sanity checks for equals / hashCode consistency
     * ------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeConsistency() {
        populateMap();

        // Create another SealableNavigableMap wrapping the same backing map
        SealableNavigableMap<Integer, String> other = new SealableNavigableMap<>(backingMap, sealedSupplier);
        assertEquals(sealableMap, other);
        assertEquals(sealableMap.hashCode(), other.hashCode());

        // Change the other map – equality should break
        other.put(99, "ninety‑nine");
        assertNotEquals(sealableMap, other);
    }
}
