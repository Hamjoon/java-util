package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactMap}.  The tests cover:
 * <ul>
 *   <li>All public API methods (basic functionality)</li>
 *   <li>Edge‑cases such as empty map, single‑value optimisation, case‑insensitivity,
 *       transition from compact array to backing map, null keys, and concurrent‑modification checks</li>
 *   <li>Exception‑throwing behaviour of the deprecated {@code plus} / {@code minus} methods</li>
 * </ul>
 *
 * The test class is deliberately self‑contained – it creates real {@code CompactMap}
 * instances via the public builder or constructors and never uses mocking.
 */
public class CompactMapTest {

    private CompactMap<String, Integer> map;

    @Before
    public void setUp() {
        // default map (empty, case‑sensitive, unordered, compactSize = 70)
        map = new CompactMap<>();
    }

    /* ---------------------------------------------------------------------- */
    /*  1.  Public API – basic behaviour                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("a", 1);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        map.put("b", 2);
        assertEquals(2, map.size());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put("x", 10);
        map.put("y", 20);

        assertTrue(map.containsKey("x"));
        assertFalse(map.containsKey("z"));

        assertTrue(map.containsValue(10));
        assertFalse(map.containsValue(99));
    }

    @Test
    public void testGetAndPut() {
        assertNull(map.get("missing"));

        Integer prev = map.put("k1", 100);
        assertNull(prev);
        assertEquals(Integer.valueOf(100), map.get("k1"));

        // overwrite
        prev = map.put("k1", 200);
        assertEquals(Integer.valueOf(100), prev);
        assertEquals(Integer.valueOf(200), map.get("k1"));
    }

    @Test
    public void testRemove() {
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(Integer.valueOf(1), map.remove("a"));
        assertNull(map.get("a"));
        assertEquals(1, map.size());

        // removing non‑existent key returns null
        assertNull(map.remove("nonexistent"));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, Integer> src = new HashMap<>();
        src.put("one", 1);
        src.put("two", 2);
        map.putAll(src);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("one"));
        assertTrue(map.containsKey("two"));

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testHashCodeAndEquals() {
        CompactMap<String, Integer> other = new CompactMap<>();
        map.put("a", 10);
        map.put("b", 20);
        other.put("a", 10);
        other.put("b", 20);

        assertEquals(map, other);
        assertEquals(map.hashCode(), other.hashCode());

        // different content -> not equal
        other.put("c", 30);
        assertNotEquals(map, other);
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("x", 1);
        map.put("y", 2);
        String s = map.toString();
        // order is not guaranteed for the default unordered map, so just check substrings
        assertTrue(s.contains("x=1"));
        assertTrue(s.contains("y=2"));
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }

    @Test
    public void testKeySetView() {
        map.put("k1", 1);
        map.put("k2", 2);
        Set<String> keys = map.keySet();

        assertEquals(2, keys.size());
        assertTrue(keys.contains("k1"));
        assertTrue(keys.contains("k2"));

        // remove via keySet
        keys.remove("k1");
        assertFalse(map.containsKey("k1"));
        assertEquals(1, map.size());
    }

    @Test
    public void testValuesView() {
        map.put("a", 10);
        map.put("b", 20);
        Collection<Integer> vals = map.values();

        assertEquals(2, vals.size());
        assertTrue(vals.contains(10));
        assertTrue(vals.contains(20));

        // remove via values collection
        vals.remove(10);
        assertFalse(map.containsValue(10));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetView() {
        map.put("p", 5);
        map.put("q", 6);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        assertEquals(2, entries.size());

        // contains test
        Map.Entry<String, Integer> sample = new AbstractMap.SimpleEntry<>("p", 5);
        assertTrue(entries.contains(sample));

        // remove via entrySet
        entries.remove(sample);
        assertFalse(map.containsKey("p"));
        assertEquals(1, map.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMinusUnsupported() {
        map.minus("any");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlusUnsupported() {
        map.plus("any");
    }

    /* ---------------------------------------------------------------------- */
    /*  2.  Edge‑case tests                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNullKeyAllowedByDefaultHashMap() {
        // Default backing map is HashMap which permits null keys
        map.put(null, 123);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(123), map.get(null));
        assertEquals(Integer.valueOf(123), map.remove(null));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testCaseInsensitiveBehaviour() {
        CompactMap<String, Integer> ciMap = CompactMap.<String, Integer>builder()
                .caseSensitive(false)   // case‑insensitive
                .build();

        ciMap.put("Key", 42);
        // different case should resolve to the same entry
        assertTrue(ciMap.containsKey("key"));
        assertEquals(Integer.valueOf(42), ciMap.get("kEy"));
        // overwriting with another case works as well
        ciMap.put("kEy", 99);
        assertEquals(Integer.valueOf(99), ciMap.get("KEY"));
        assertEquals(1, ciMap.size());
    }

    @Test
    public void testSingleValueKeyOptimization() {
        // default single value key is "id"
        CompactMap<String, String> single = new CompactMap<>();
        // put using the special key – should be stored directly (OBJECT state)
        single.put("id", "value1");
        assertEquals(1, single.size());
        assertEquals("value1", single.get("id"));

        // adding a second key forces transition to array storage
        single.put("other", "value2");
        assertEquals(2, single.size());
        assertEquals("value2", single.get("other"));
        assertEquals("value1", single.get("id"));
    }

    @Test
    public void testTransitionFromArrayToBackingMap() throws Exception {
        // Use a very small compactSize to force transition after a few puts
        CompactMap<String, Integer> tiny = CompactMap.<String, Integer>builder()
                .compactSize(2)          // array can hold at most 2 entries
                .mapType(HashMap.class)  // explicit backing map type
                .build();

        // first two puts stay in compact array (ARRAY state)
        tiny.put("a", 1);
        tiny.put("b", 2);
        assertEquals(2, tiny.size());

        // third put triggers transition to a real Map (MAP state)
        tiny.put("c", 3);
        assertEquals(3, tiny.size());

        // Verify that the internal representation is now a Map via reflection
        java.lang.reflect.Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        Object internal = f.get(tiny);
        assertTrue("Expected backing Map after transition", internal instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Integer> backing = (Map<String, Integer>) internal;
        assertTrue(backing.containsKey("a"));
        assertTrue(backing.containsKey("b"));
        assertTrue(backing.containsKey("c"));
    }

    @Test
    public void testIteratorRemoveAndConcurrentModification() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Iterator<String> keyIter = map.keySet().iterator();
        assertTrue(keyIter.hasNext());
        assertEquals("one", keyIter.next());
        keyIter.remove();               // remove first entry via iterator
        assertEquals(2, map.size());
        assertFalse(map.containsKey("one"));

        // modify map directly -> iterator should detect concurrent modification
        map.put("four", 4);
        try {
            keyIter.hasNext();          // any subsequent operation must fail
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException e) {
            // expected
        }
    }

    @Test
    public void testEntryIteratorRemove() {
        map.put("a", 10);
        map.put("b", 20);
        Iterator<Map.Entry<String, Integer>> entryIter = map.entrySet().iterator();

        while (entryIter.hasNext()) {
            Map.Entry<String, Integer> e = entryIter.next();
            if (e.getKey().equals("a")) {
                entryIter.remove();
            }
        }
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEqualsWithDifferentInternalStates() {
        // map1 uses default settings (will stay in ARRAY state for up to 70 entries)
        CompactMap<String, Integer> map1 = new CompactMap<>();
        map1.put("x", 1);
        map1.put("y", 2);

        // map2 forces MAP state by using a tiny compactSize
        CompactMap<String, Integer> map2 = CompactMap.<String, Integer>builder()
                .compactSize(1)      // any size >1 will use backing map
                .build();
        map2.put("x", 1);
        map2.put("y", 2);

        // Different internal representations but same logical content
        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testBuilderWithInsertionOrder() {
        CompactMap<String, Integer> ordered = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .build();
        ordered.put("first", 1);
        ordered.put("second", 2);
        ordered.put("third", 3);

        // Verify iteration order matches insertion order
        Iterator<String> it = ordered.keySet().iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
    }

    @Test
    public void testBuilderWithSortedOrder() {
        CompactMap<String, Integer> sorted = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .build();
        sorted.put("b", 2);
        sorted.put("a", 1);
        sorted.put("c", 3);

        // Keys should be iterated in natural order
        Iterator<String> it = sorted.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
    }

    @Test
    public void testBuilderWithReverseOrder() {
        CompactMap<String, Integer> rev = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .build();
        rev.put("a", 1);
        rev.put("b", 2);
        rev.put("c", 3);

        // Reverse order iterator
        Iterator<String> it = rev.keySet().iterator();
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
    }

    @Test
    public void testBuilderWithSourceMap() {
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("one", 1);
        src.put("two", 2);

        CompactMap<String, Integer> fromSrc = CompactMap.<String, Integer>builder()
                .sourceMap(src)
                .build();

        assertEquals(2, fromSrc.size());
        assertEquals(Integer.valueOf(1), fromSrc.get("one"));
        assertEquals(Integer.valueOf(2), fromSrc.get("two"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidateAndFinalizeOptionsRejectsCompactSizeBelowTwo() {
        // Directly invoke the static validation method with illegal compactSize
        Map<String, Object> opts = new HashMap<>();
        opts.put(CompactMap.COMPACT_SIZE, 1); // illegal, must be >=2
        CompactMap.validateAndFinalizeOptions(opts);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetermineMapTypeRejectsIdentityHashMap() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(CompactMap.MAP_TYPE, IdentityHashMap.class);
        // ordering can be default UNORDERED
        CompactMap.determineMapType(opts, CompactMap.UNORDERED);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDetermineMapTypeRejectsIncompatibleOrdering() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(CompactMap.MAP_TYPE, HashMap.class);
        // HashMap cannot satisfy SORTED ordering
        CompactMap.determineMapType(opts, CompactMap.SORTED);
    }

    @Test
    public void testCompactMapComparatorCaseInsensitiveAndReverse() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(true, true);
        // case‑insensitive compare
        assertEquals(0, comp.compare("abc", "ABC"));
        // reverse order: "b" should be less than "a" after reversal
        assertTrue(comp.compare("a", "b") > 0);
    }

    @Test
    public void testCompactMapComparatorHandlesNulls() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, false);
        assertTrue(comp.compare(null, "a") > 0); // null is last
        assertTrue(comp.compare("a", null) < 0);
        assertEquals(0, comp.compare(null, null));
    }
}
