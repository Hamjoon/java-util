package com.cedarsoftware.util;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactMap}.  The tests cover
 * <ul>
 *   <li>All public API methods (constructors, size, put, get, remove, clear, etc.)</li>
 *   <li>Edge‑case behaviour such as the single‑value optimisation, state transitions,
 *       case‑insensitive handling and ordering.</li>
 *   <li>Fail‑fast iterators and the {@code equals}/{@code hashCode} contract.</li>
 *   <li>Configuration related methods {@code getConfig()}, {@code withConfig()} and the
 *       builder API (when a JDK compiler is present).</li>
 * </ul>
 *
 * The test class does **not** use any mocking framework – all objects are created
 * via the public constructors or factory methods that exist in the source code.
 */
@RunWith(JUnit4.class)
public class CompactMapTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ---------------------------------------------------------------------- */
    /*  Helper to build a case‑insensitive, sorted CompactMap using the Builder */
    /* ---------------------------------------------------------------------- */
    private static <K, V> CompactMap<K, V> buildCaseInsensitiveSortedMap() {
        try {
            // Builder requires the Java compiler; if it is not available the test will be skipped.
            return CompactMap.<K, V>builder()
                    .caseSensitive(false)
                    .sortedOrder()
                    .compactSize(5)          // small size makes it easy to trigger array → map transition
                    .build();
        } catch (IllegalStateException e) {
            // No Java compiler – skip the test that needs the builder.
            Assume.assumeNoException("Java compiler not available; skipping builder‑based tests", e);
            return null; // unreachable, but required for compilation
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  1.  Public constructors and basic state queries                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEmptyMapConstruction() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    @Test
    public void testCopyConstructor() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        CompactMap<String, Integer> map = new CompactMap<>(source);
        assertFalse(map.isEmpty());
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    /* ---------------------------------------------------------------------- */
    /*  2.  Single‑value optimisation (key == getSingleValueKey())            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testSingleValueOptimisation() {
        CompactMap<String, String> map = new CompactMap<>();
        // default single‑value key is "id"
        map.put("id", "single");
        assertEquals(1, map.size());
        assertEquals("single", map.get("id"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // Overwrite the same key – should replace the stored object
        String old = map.put("id", "new");
        assertEquals("single", old);
        assertEquals("new", map.get("id"));
    }

    @Test
    public void testSingleEntryWithoutOptimisation() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("other", "value");
        assertEquals(1, map.size());
        assertEquals("value", map.get("other"));
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
    }

    /* ---------------------------------------------------------------------- */
    /*  3.  Transition from empty → single → array → backing map               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testTransitionToArrayAndBack() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);                     // single entry (ENTRY)
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        map.put("b", 2);                     // now array (ARRAY)
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));

        // Remove one entry – should go back to single entry state
        Integer removed = map.remove("a");
        assertEquals(Integer.valueOf(1), removed);
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
        assertFalse(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
    }

    @Test
    public void testTransitionToBackingMap() {
        // Use a small compactSize via builder to force map transition quickly
        CompactMap<String, Integer> map = buildCaseInsensitiveSortedMap();
        // Insert entries up to the compactSize (5) – they stay in ARRAY state
        for (int i = 0; i < 5; i++) {
            map.put("k" + i, i);
        }
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Adding the 6th entry forces a transition to MAP state
        map.put("k5", 5);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
        assertEquals(6, map.size());
        for (int i = 0; i < 6; i++) {
            assertEquals(Integer.valueOf(i), map.get("k" + i));
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  4.  Case‑insensitive behaviour (builder)                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testCaseInsensitiveContainsAndGet() {
        CompactMap<String, String> map = buildCaseInsensitiveSortedMap();
        map.put("Key", "value");
        // Different case should be considered equal
        assertTrue(map.containsKey("key"));
        assertEquals("value", map.get("KEY"));
    }

    @Test
    public void testCaseInsensitiveOrdering() {
        CompactMap<String, Integer> map = buildCaseInsensitiveSortedMap();
        map.put("b", 2);
        map.put("A", 1);
        map.put("c", 3);
        // The iterator should return keys in natural order ignoring case: A, b, c
        Iterator<String> it = map.keySet().iterator();
        assertEquals("A", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());
    }

    /* ---------------------------------------------------------------------- */
    /*  5.  containsValue, clear, equals, hashCode, toString                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testContainsValueAndClear() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("x", "val");
        map.put("y", "val"); // duplicate value
        assertTrue(map.containsValue("val"));
        assertFalse(map.containsValue("missing"));
        map.clear();
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("x"));
        assertFalse(map.containsValue("val"));
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactMap<String, Integer> map1 = new CompactMap<>();
        CompactMap<String, Integer> map2 = new CompactMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map2.put("b", 2);
        map2.put("a", 1);
        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());

        // Different content -> not equal
        map2.put("c", 3);
        assertNotEquals(map1, map2);
    }

    @Test
    public void testToStringRepresentation() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);
        String s = map.toString();
        // The string must contain both entries, order is not important for unordered map
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }

    /* ---------------------------------------------------------------------- */
    /*  6.  keySet, values, entrySet iterators and fail‑fast behaviour       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testKeySetIteratorAndRemoval() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        assertEquals("a", it.next());
        it.remove();                     // remove key "a"
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testValueIteratorFailFast() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 10);
        map.put("y", 20);
        Iterator<Integer> it = map.values().iterator();
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(10), it.next());

        // Structural modification after iterator creation
        map.put("z", 30);
        thrown.expect(ConcurrentModificationException.class);
        it.next();                         // should throw
    }

    @Test
    public void testEntrySetIteratorAndRemoveAll() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");

        Set<Map.Entry<String, String>> entries = map.entrySet();
        Iterator<Map.Entry<String, String>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (e.getKey().equals("k2")) {
                it.remove();
            }
        }
        assertEquals(2, map.size());
        assertFalse(map.containsKey("k2"));
    }

    /* ---------------------------------------------------------------------- */
    /*  7.  getConfig and withConfig                                           */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testGetConfigDefaults() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Map<String, Object> cfg = map.getConfig();

        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(true, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.UNORDERED, cfg.get(CompactMap.ORDERING));
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(CompactMap.DEFAULT_MAP_TYPE, cfg.get(CompactMap.MAP_TYPE));
    }

    @Test
    public void testWithConfigCreatesNewMapWithSameEntries() {
        CompactMap<String, Integer> original = new CompactMap<>();
        original.put("a", 1);
        original.put("b", 2);

        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put(CompactMap.COMPACT_SIZE, 10);
        newConfig.put(CompactMap.CASE_SENSITIVE, false);
        newConfig.put(CompactMap.ORDERING, CompactMap.SORTED);
        newConfig.put(CompactMap.SINGLE_KEY, "uid");
        newConfig.put(CompactMap.MAP_TYPE, LinkedHashMap.class);

        CompactMap<String, Integer> copy = original.withConfig(newConfig);
        assertEquals(2, copy.size());
        assertEquals(Integer.valueOf(1), copy.get("a"));
        assertEquals(Integer.valueOf(2), copy.get("b"));
        // The copy should reflect the new configuration
        Map<String, Object> cfg = copy.getConfig();
        assertEquals(10, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(false, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
        assertEquals("uid", cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(LinkedHashMap.class, cfg.get(CompactMap.MAP_TYPE));
    }

    /* ---------------------------------------------------------------------- */
    /*  8.  Deprecated unsupported operations                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testUnsupportedMinusAndPlus() {
        CompactMap<String, String> map = new CompactMap<>();
        thrown.expect(UnsupportedOperationException.class);
        map.minus("anything");
    }

    @Test
    public void testUnsupportedPlus() {
        CompactMap<String, String> map = new CompactMap<>();
        thrown.expect(UnsupportedOperationException.class);
        map.plus("anything");
    }

    /* ---------------------------------------------------------------------- */
    /*  9.  Builder static method (basic usage) – only checks that it returns
     *      a non‑null map when the compiler is available.
     * ---------------------------------------------------------------------- */
    @Test
    public void testBuilderCreatesMap() {
        try {
            CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                    .compactSize(8)
                    .build();
            assertNotNull(map);
            // Basic put/get to ensure the map works.
            map.put("one", 1);
            assertEquals(Integer.valueOf(1), map.get("one"));
        } catch (IllegalStateException e) {
            // Builder requires a JDK compiler; skip if not present.
            Assume.assumeNoException("Java compiler not available – skipping builder test", e);
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  10.  Verify that putAll correctly copies entries and respects null   */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testPutAllCopiesEntries() {
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 10);
        src.put("b", 20);
        CompactMap<String, Integer> map = new CompactMap<>();
        map.putAll(src);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(10), map.get("a"));
        assertEquals(Integer.valueOf(20), map.get("b"));
    }

    @Test
    public void testPutAllWithNullMapThrowsNPE() {
        CompactMap<String, Integer> map = new CompactMap<>();
        thrown.expect(NullPointerException.class);
        map.putAll(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  11.  Verify that isDefaultCompactMap reports true for a plain map   */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testIsDefaultCompactMap() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertTrue(map.isDefaultCompactMap());
    }
}
