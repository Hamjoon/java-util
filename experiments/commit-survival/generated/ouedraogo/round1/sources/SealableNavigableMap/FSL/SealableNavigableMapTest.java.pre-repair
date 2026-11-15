package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link SealableNavigableMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>All immutable (read‑only) operations – they must delegate to the wrapped map.</li>
 *   <li>All mutable operations – they must succeed when the map is unsealed and throw
 *       {@link UnsupportedOperationException} when sealed.</li>
 *   <li>Views returned by {@code keySet()}, {@code entrySet()}, {@code values()},
 *       {@code navigableKeySet()}, {@code descendingKeySet()}, {@code subMap()},
 *       {@code headMap()}, {@code tailMap()}, {@code descendingMap()} – the views must
 *       honour the same sealed/unsealed contract.</li>
 *   <li>Construction variants (empty, from a {@link SortedMap}, and wrapping an existing
 *       {@link NavigableMap}).</li>
 * </ul>
 */
public class SealableNavigableMapTest {

    private AtomicBoolean sealedFlag;
    private Supplier<Boolean> sealedSupplier;

    @Before
    public void setUp() {
        sealedFlag = new AtomicBoolean(false);
        sealedSupplier = () -> sealedFlag.get();
    }

    /** Helper to create a map pre‑populated with three entries. */
    private SealableNavigableMap<String, Integer> createSampleMap() {
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(sealedSupplier);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        return map;
    }

    /** -----------------------------------------------------------------------
     *  Immutable (read‑only) API tests
     *  --------------------------------------------------------------------- */
    @Test
    public void testImmutableReadOperations() {
        SealableNavigableMap<String, Integer> map = createSampleMap();

        // size / isEmpty
        assertEquals(3, map.size());
        assertFalse(map.isEmpty());

        // containsKey / containsValue
        assertTrue(map.containsKey("b"));
        assertFalse(map.containsKey("z"));
        assertTrue(map.containsValue(3));
        assertFalse(map.containsValue(99));

        // get / firstKey / lastKey
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals("a", map.firstKey());
        assertEquals("c", map.lastKey());

        // comparator (ConcurrentNavigableMapNullSafe uses natural order for Strings)
        Comparator<? super String> comp = map.comparator();
        assertNull(comp); // natural ordering => null comparator

        // navigation methods
        assertEquals("a", map.lowerKey("b"));
        assertEquals("b", map.floorKey("b"));
        assertEquals("b", map.ceilingKey("b"));
        assertEquals("c", map.higherKey("b"));

        assertEquals(Integer.valueOf(1), map.lowerEntry("b").getValue());
        assertEquals(Integer.valueOf(2), map.floorEntry("b").getValue());
        assertEquals(Integer.valueOf(2), map.ceilingEntry("b").getValue());
        assertEquals(Integer.valueOf(3), map.higherEntry("b").getValue());

        assertEquals(Integer.valueOf(1), map.firstEntry().getValue());
        assertEquals(Integer.valueOf(3), map.lastEntry().getValue());

        // toString, equals, hashCode
        String expectedString = "{a=1, b=2, c=3}";
        assertEquals(expectedString, map.toString());

        SealableNavigableMap<String, Integer> same = createSampleMap();
        assertTrue(map.equals(same));
        assertEquals(map.hashCode(), same.hashCode());
    }

    /** -----------------------------------------------------------------------
     *  Mutable API – unsealed (should succeed)
     *  --------------------------------------------------------------------- */
    @Test
    public void testMutableOperationsWhenUnsealed() {
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(sealedSupplier);

        // put
        assertNull(map.put("x", 10));
        assertEquals(Integer.valueOf(10), map.get("x"));

        // putAll
        Map<String, Integer> extra = new HashMap<>();
        extra.put("y", 20);
        extra.put("z", 30);
        map.putAll(extra);
        assertEquals(Integer.valueOf(20), map.get("y"));
        assertEquals(Integer.valueOf(30), map.get("z"));

        // remove
        assertEquals(Integer.valueOf(20), map.remove("y"));
        assertNull(map.get("y"));

        // pollFirstEntry / pollLastEntry
        Map.Entry<String, Integer> first = map.pollFirstEntry(); // should be "x"
        assertEquals("x", first.getKey());
        assertEquals(Integer.valueOf(10), first.getValue());

        Map.Entry<String, Integer> last = map.pollLastEntry(); // should be "z"
        assertEquals("z", last.getKey());
        assertEquals(Integer.valueOf(30), last.getValue());

        // clear
        map.clear();
        assertTrue(map.isEmpty());
    }

    /** -----------------------------------------------------------------------
     *  Mutable API – sealed (must throw)
     *  --------------------------------------------------------------------- */
    @Test
    public void testMutableOperationsWhenSealed() {
        sealedFlag.set(true); // seal the map
        SealableNavigableMap<String, Integer> map = createSampleMap();

        // put
        try {
            map.put("new", 99);
            fail("Expected UnsupportedOperationException on put when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // putAll
        try {
            map.putAll(Collections.singletonMap("new", 99));
            fail("Expected UnsupportedOperationException on putAll when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // remove
        try {
            map.remove("a");
            fail("Expected UnsupportedOperationException on remove when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // pollFirstEntry
        try {
            map.pollFirstEntry();
            fail("Expected UnsupportedOperationException on pollFirstEntry when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // pollLastEntry
        try {
            map.pollLastEntry();
            fail("Expected UnsupportedOperationException on pollLastEntry when sealed");
        } catch (UnsupportedOperationException ignored) {}

        // clear
        try {
            map.clear();
            fail("Expected UnsupportedOperationException on clear when sealed");
        } catch (UnsupportedOperationException ignored) {}
    }

    /** -----------------------------------------------------------------------
     *  View collections – modification respects sealed state
     *  --------------------------------------------------------------------- */
    @Test
    public void testKeySetViewModification() {
        SealableNavigableMap<String, Integer> map = createSampleMap();

        // keySet is a SealableSet – removal should work while unsealed
        Set<String> ks = map.keySet();
        assertTrue(ks.remove("b"));
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());

        // seal and attempt removal again
        sealedFlag.set(true);
        try {
            ks.remove("a");
            fail("Expected UnsupportedOperationException when removing via keySet while sealed");
        } catch (UnsupportedOperationException ignored) {}

        // unseal again and ensure removal works again
        sealedFlag.set(false);
        assertTrue(ks.remove("a"));
        assertFalse(map.containsKey("a"));
    }

    @Test
    public void testEntrySetViewModification() {
        SealableNavigableMap<String, Integer> map = createSampleMap();

        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();

        // Remove an entry via iterator
        Iterator<Map.Entry<String, Integer>> it = entrySet.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if ("c".equals(e.getKey())) {
                it.remove();
                break;
            }
        }
        assertFalse(map.containsKey("c"));
        assertEquals(2, map.size());

        // Seal and attempt a modification
        sealedFlag.set(true);
        try {
            entrySet.clear();
            fail("Expected UnsupportedOperationException when clearing entrySet while sealed");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testValuesViewModification() {
        SealableNavigableMap<String, Integer> map = createSampleMap();

        Collection<Integer> values = map.values();

        // values is a SealableList – remove an element
        assertTrue(values.remove(Integer.valueOf(2)));
        assertFalse(map.containsValue(2));
        assertEquals(2, map.size());

        // Seal and try to add (add is not supported by the underlying list, but clear should be blocked)
        sealedFlag.set(true);
        try {
            values.clear();
            fail("Expected UnsupportedOperationException when clearing values while sealed");
        } catch (UnsupportedOperationException ignored) {}
    }

    /** -----------------------------------------------------------------------
     *  Navigable key set, descending key set and descending map ordering
     *  --------------------------------------------------------------------- */
    @Test
    public void testNavigableKeySetOrdering() {
        SealableNavigableMap<String, Integer> map = createSampleMap();

        NavigableSet<String> navKeySet = map.navigableKeySet();
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(navKeySet));

        // descendingKeySet should be reverse order
        NavigableSet<String> descKeySet = map.descendingKeySet();
        assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(descKeySet));
    }

    @Test
    public void testDescendingMapBehaviour() {
        SealableNavigableMap<String, Integer> map = createSampleMap();

        NavigableMap<String, Integer> descMap = map.descendingMap();

        // The firstKey of the descending map should be the original lastKey
        assertEquals("c", descMap.firstKey());
        assertEquals("a", descMap.lastKey());

        // Mutations through the descending map must respect seal state
        descMap.put("d", 4);
        assertTrue(map.containsKey("d"));
        assertEquals(Integer.valueOf(4), map.get("d"));

        // Seal and verify mutation throws
        sealedFlag.set(true);
        try {
            descMap.remove("a");
            fail("Expected UnsupportedOperationException when removing via descendingMap while sealed");
        } catch (UnsupportedOperationException ignored) {}
    }

    /** -----------------------------------------------------------------------
     *  Sub‑map, head‑map and tail‑map views – they must share the same sealedSupplier
     *  --------------------------------------------------------------------- */
    @Test
    public void testSubMapView() {
        SealableNavigableMap<String, Integer> map = createSampleMap();

        // subMap from "a" (inclusive) to "c" (exclusive) should contain "a" and "b"
        NavigableMap<String, Integer> sub = map.subMap("a", true, "c", false);
        assertEquals(2, sub.size());
        assertTrue(sub.containsKey("a"));
        assertTrue(sub.containsKey("b"));
        assertFalse(sub.containsKey("c"));

        // Mutate sub‑map while unsealed
        sub.put("aa", 11);
        assertTrue(map.containsKey("aa"));
        assertEquals(Integer.valueOf(11), map.get("aa"));

        // Seal and verify mutation is blocked
        sealedFlag.set(true);
        try {
            sub.remove("a");
            fail("Expected UnsupportedOperationException when removing via subMap while sealed");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testHeadMapAndTailMapViews() {
        SealableNavigableMap<String, Integer> map = createSampleMap();

        // headMap(toKey="b", inclusive=false) -> only "a"
        NavigableMap<String, Integer> head = map.headMap("b", false);
        assertEquals(Collections.singleton("a"), head.keySet());

        // tailMap(fromKey="b", inclusive=true) -> "b" and "c"
        NavigableMap<String, Integer> tail = map.tailMap("b", true);
        assertEquals(new HashSet<>(Arrays.asList("b", "c")), new HashSet<>(tail.keySet()));

        // Mutate tail map while unsealed
        tail.put("d", 4);
        assertTrue(map.containsKey("d"));

        // Seal and try to clear head map
        sealedFlag.set(true);
        try {
            head.clear();
            fail("Expected UnsupportedOperationException when clearing headMap while sealed");
        } catch (UnsupportedOperationException ignored) {}
    }

    /** -----------------------------------------------------------------------
     *  Constructors – verify that initial data is copied correctly
     *  --------------------------------------------------------------------- */
    @Test
    public void testConstructorWithSortedMapCopiesData() {
        SortedMap<String, Integer> sorted = new TreeMap<>();
        sorted.put("k1", 100);
        sorted.put("k2", 200);

        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(sorted, sealedSupplier);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(100), map.get("k1"));
        assertEquals(Integer.valueOf(200), map.get("k2"));
    }

    @Test
    public void testConstructorWrappingExistingNavigableMap() {
        NavigableMap<String, Integer> underlying = new TreeMap<>();
        underlying.put("x", 1);
        underlying.put("y", 2);

        SealableNavigableMap<String, Integer> wrapper = new SealableNavigableMap<>(underlying, sealedSupplier);
        assertEquals(2, wrapper.size());

        // Ensure modifications affect the original map when unsealed
        wrapper.put("z", 3);
        assertTrue(underlying.containsKey("z"));
        assertEquals(Integer.valueOf(3), underlying.get("z"));
    }

    /** -----------------------------------------------------------------------
     *  Equality and hashCode delegation – compare against a plain NavigableMap
     *  --------------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCodeAgainstPlainMap() {
        SealableNavigableMap<String, Integer> sealable = createSampleMap();
        NavigableMap<String, Integer> plain = new TreeMap<>();
        plain.put("a", 1);
        plain.put("b", 2);
        plain.put("c", 3);

        assertTrue(sealable.equals(plain));
        assertEquals(sealable.hashCode(), plain.hashCode());
    }
}
