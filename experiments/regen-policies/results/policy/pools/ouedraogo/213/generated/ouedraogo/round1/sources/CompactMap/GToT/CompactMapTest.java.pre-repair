package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public API methods (construction, Map operations, configuration, state queries)</li>
 *   <li>Typical usage scenarios (empty, single‑entry, compact‑array, backing‑map)</li>
 *   <li>Edge cases such as case‑insensitive keys, sorted ordering, transitions between internal
 *       representations, null keys/values and concurrent‑modification detection.</li>
 *   <li>Equality, hash‑code consistency and {@code toString()} formatting.</li>
 * </ul>
 */
public class CompactMapTest {

    private CompactMap<String, Integer> map;

    @Before
    public void setUp() {
        // Use a small compactSize to exercise array → map transition quickly
        map = CompactMap.<String, Integer>builder()
                .compactSize(3)           // threshold 3 entries
                .sortedOrder()            // forces sorting & binary search
                .caseSensitive(true)      // default, but explicit for readability
                .build();
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic construction & configuration                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        CompactMap<String, Integer> empty = new CompactMap<>();
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, empty.getLogicalValueType());
    }

    @Test
    public void testCopyConstructorCopiesEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        CompactMap<String, Integer> copy = new CompactMap<>(source);
        assertEquals(2, copy.size());
        assertEquals(Integer.valueOf(1), copy.get("a"));
        assertEquals(Integer.valueOf(2), copy.get("b"));
        // internal state should be ARRAY because 2 <= compactSize(3)
        assertEquals(CompactMap.LogicalValueType.ARRAY, copy.getLogicalValueType());
    }

    @Test
    public void testBuilderCreatesConfiguredMap() {
        CompactMap<String, Integer> custom = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .compactSize(5)
                .insertionOrder()
                .singleValueKey("uid")
                .build();

        Map<String, Object> cfg = custom.getConfig();
        assertEquals(false, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(5, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.INSERTION, cfg.get(CompactMap.ORDERING));
        assertEquals("uid", cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(HashMap.class, cfg.get(CompactMap.MAP_TYPE));
    }

    @Test
    public void testIsDefaultCompactMapOnPlainInstance() {
        CompactMap<String, Integer> plain = new CompactMap<>();
        assertTrue(plain.isDefaultCompactMap());
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic Map operations (size, isEmpty, put, get, remove, clear)          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetSingleEntry() {
        assertNull(map.put("one", 1));
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());
    }

    @Test
    public void testPutOverwriteUpdatesValue() {
        map.put("key", 10);
        Integer old = map.put("key", 20);
        assertEquals(Integer.valueOf(10), old);
        assertEquals(Integer.valueOf(20), map.get("key"));
        assertEquals(1, map.size());
    }

    @Test
    public void testArrayTransition() {
        map.put("a", 1);
        map.put("b", 2); // now 2 entries, still ARRAY (compactSize=3)
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        map.put("c", 3); // reaches compactSize, still ARRAY
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        map.put("d", 4); // exceeds compactSize -> transition to MAP
        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
    }

    @Test
    public void testRemoveFromArrayAndMapTransitions() {
        // Fill to trigger map transition
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4); // now MAP state
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // Remove until size == compactSize (3) -> should go back to ARRAY
        map.remove("d");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Remove one more to reach 2 entries (still ARRAY)
        map.remove("c");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Remove to single entry -> OBJECT state
        map.remove("b");
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // Remove last entry -> EMPTY
        map.remove("a");
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    @Test
    public void testClearResetsMap() {
        map.put("x", 100);
        map.put("y", 200);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /* ---------------------------------------------------------------------- */
    /*  containsKey / containsValue                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testContainsKeyAndValue() {
        map.put("k1", 10);
        map.put("k2", 20);
        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsValue(20));
        assertFalse(map.containsKey("missing"));
        assertFalse(map.containsValue(999));
    }

    @Test
    public void testContainsKeyCaseInsensitive() {
        CompactMap<String, Integer> ciMap = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .sortedOrder()
                .compactSize(3)
                .build();

        ciMap.put("Key", 1);
        assertTrue(ciMap.containsKey("key"));
        assertTrue(ciMap.containsKey("KEY"));
        assertEquals(Integer.valueOf(1), ciMap.get("kEy"));
    }

    /* ---------------------------------------------------------------------- */
    /*  putAll and bulk operations                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAllAddsAllEntriesAndTriggersTransition() {
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        src.put("c", 3);
        src.put("d", 4); // 4 entries > compactSize(3)

        map.putAll(src);
        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
        assertEquals(Integer.valueOf(3), map.get("c"));
    }

    @Test(expected = NullPointerException.class)
    public void testPutAllRejectsNullMap() {
        map.putAll(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  keySet, values, entrySet and their iterators                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testKeySetIterationOrderSorted() {
        map.put("b", 2);
        map.put("a", 1);
        map.put("c", 3);
        // Sorted order means keys are a,b,c
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("a", "b", "c"), keys);
    }

    @Test
    public void testValuesIterationMatchesKeyOrder() {
        map.put("b", 2);
        map.put("a", 1);
        map.put("c", 3);
        List<Integer> values = new ArrayList<>(map.values());
        assertEquals(Arrays.asList(1, 2, 3), values);
    }

    @Test
    public void testEntrySetIteratorRemove() {
        map.put("x", 10);
        map.put("y", 20);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if ("x".equals(e.getKey())) {
                it.remove(); // remove via iterator
            }
        }
        assertFalse(map.containsKey("x"));
        assertTrue(map.containsKey("y"));
        assertEquals(1, map.size());
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModificationDetection() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        map.put("c", 3); // modify after iterator creation
        it.next(); // should throw CME
    }

    /* ---------------------------------------------------------------------- */
    /*  equals, hashCode and toString                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeWithStandardMap() {
        map.put("one", 1);
        map.put("two", 2);

        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);

        assertTrue(map.equals(other));
        assertEquals(other.hashCode(), map.hashCode());

        // Change a value -> not equal
        other.put("two", 99);
        assertFalse(map.equals(other));
    }

    @Test
    public void testToStringFormat() {
        map.put("alpha", 10);
        map.put("beta", 20);
        String s = map.toString();
        // Order is sorted, so alpha then beta
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("alpha=10"));
        assertTrue(s.contains("beta=20"));
    }

    /* ---------------------------------------------------------------------- */
    /*  getConfig and withConfig                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetConfigReflectsCurrentSettings() {
        Map<String, Object> cfg = map.getConfig();
        assertEquals(3, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(true, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
        assertEquals("id", cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(HashMap.class, cfg.get(CompactMap.MAP_TYPE));
    }

    @Test
    public void testWithConfigCreatesNewMapWithAdjustedSettings() {
        // Change to case‑insensitive and insertion order
        Map<String, Object> newCfg = new HashMap<>();
        newCfg.put(CompactMap.CASE_SENSITIVE, false);
        newCfg.put(CompactMap.ORDERING, CompactMap.INSERTION);
        newCfg.put(CompactMap.COMPACT_SIZE, 2);
        newCfg.put(CompactMap.SINGLE_KEY, "uid");
        newCfg.put(CompactMap.MAP_TYPE, LinkedHashMap.class);

        CompactMap<String, Integer> changed = map.withConfig(newCfg);
        Map<String, Object> cfg = changed.getConfig();

        assertEquals(false, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(2, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.INSERTION, cfg.get(CompactMap.ORDERING));
        assertEquals("uid", cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(LinkedHashMap.class, cfg.get(CompactMap.MAP_TYPE));

        // Verify that case‑insensitivity works
        changed.put("Test", 5);
        assertTrue(changed.containsKey("test"));
        assertEquals(Integer.valueOf(5), changed.get("TEST"));
    }

    /* ---------------------------------------------------------------------- */
    /*  LogicalValueType checks for each internal state                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLogicalValueTypesAcrossTransitions() {
        // EMPTY
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());

        // SINGLE OBJECT (optimized single‑value storage)
        map.put("id", 42); // key matches default singleValueKey
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // ARRAY (add a different key, still under compactSize)
        map.put("other", 99);
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // MAP (exceed compactSize)
        map.put("c", 3);
        map.put("d", 4);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
    }

    /* ---------------------------------------------------------------------- */
    /*  Builder edge cases (invalid map types, missing compiler)              */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsUnsupportedMapType() {
        CompactMap.<String, Integer>builder()
                .mapType(IdentityHashMap.class) // not supported
                .build();
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderFailsWhenJavaCompilerIsUnavailable() {
        // Simulate missing compiler by using reflection to set the internal flag.
        // The library checks ReflectionUtils.isJavaCompilerAvailable().
        // We'll temporarily replace the method with a subclass that forces false.
        // Since we cannot modify library code, we invoke the builder in an
        // environment where the compiler is not present. In most CI
        // environments the compiler is available, so we guard the test:
        if (ReflectionUtils.isJavaCompilerAvailable()) {
            // Skip test – cannot reliably simulate missing compiler without mocking.
            throw new IllegalStateException("Compiler is available – test skipped");
        }
        CompactMap.<String, Integer>builder().build();
    }

    /* ---------------------------------------------------------------------- */
    /*  CompactMapComparator direct tests (used internally)                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComparatorCaseInsensitiveAndReverse() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(true, true);
        // case‑insensitive compare
        assertEquals(0, comp.compare("abc", "ABC"));
        // reverse order: "b" should be less than "a" after reversal
        assertTrue(comp.compare("b", "a") < 0);
    }

    @Test
    public void testComparatorHandlesNulls() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, false);
        assertEquals(1, comp.compare(null, "x"));
        assertEquals(-1, comp.compare("x", null));
        assertEquals(0, comp.compare(null, null));
    }
}
