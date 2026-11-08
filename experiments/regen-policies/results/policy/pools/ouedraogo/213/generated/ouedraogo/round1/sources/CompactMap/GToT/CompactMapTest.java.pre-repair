package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public API methods (constructors, size/isEmpty, put/get/remove, collection views, etc.)</li>
 *   <li>Different internal storage states (empty, single entry, compact array, backing map)</li>
 *   <li>Configuration options via the {@link Builder} (case‑sensitivity, ordering, compact size, map type)</li>
 *   <li>Edge‑cases such as null values, duplicate keys, transitions between storage states,
 *       iterator concurrent‑modification detection and removal, equals/hashCode contract,
 *       and invalid builder options.</li>
 *   <li>Special inner classes ({@link CompactMapEntry}, {@link CompactMapComparator},
 *       iterator implementations).</li>
 * </ul>
 */
@RunWith(JUnit4.class)
public class CompactMapTest {

    private CompactMap<String, Integer> map;

    @Before
    public void setUp() {
        // Default map – empty, case‑sensitive, unordered, compact size = 50
        map = new CompactMap<>();
    }

    /*** -----------------------------------------------------------------
     *  1. Constructors & basic state checks
     * ----------------------------------------------------------------- */
    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        assertTrue("New map must be empty", map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    @Test
    public void testCopyConstructorCopiesEntries() {
        map.put("a", 1);
        map.put("b", 2);
        CompactMap<String, Integer> copy = new CompactMap<>(map);
        assertEquals(2, copy.size());
        assertEquals(Integer.valueOf(1), copy.get("a"));
        assertEquals(Integer.valueOf(2), copy.get("b"));
        assertEquals(map.getLogicalValueType(), copy.getLogicalValueType());
    }

    @Test(expected = NullPointerException.class)
    public void testCopyConstructorRejectsNull() {
        new CompactMap<String, Integer>((Map<String, Integer>) null);
    }

    /*** -----------------------------------------------------------------
     *  2. Basic map operations in all storage states
     * ----------------------------------------------------------------- */
    @Test
    public void testPutAndGetSingleEntryOptimized() {
        // Using the default single‑value key ("id") – we store a value directly
        CompactMap<String, String> single = new CompactMap<>();
        single.put("id", "value");
        assertEquals(1, single.size());
        assertEquals("value", single.get("id"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, single.getLogicalValueType());
    }

    @Test
    public void testTransitionFromSingleEntryToArray() {
        map.put("k1", 1);          // first entry – single entry state
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        map.put("k2", 2);          // second entry – should become compact array
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("k1"));
        assertEquals(Integer.valueOf(2), map.get("k2"));
    }

    @Test
    public void testTransitionFromArrayToBackingMap() {
        // Use a tiny compactSize to force a transition quickly
        CompactMap<String, Integer> tiny = CompactMap.builder()
                .compactSize(3)          // array can hold at most 3 entries
                .build();

        tiny.put("a", 1);
        tiny.put("b", 2);
        tiny.put("c", 3); // still array (size == compactSize)
        assertEquals(CompactMap.LogicalValueType.ARRAY, tiny.getLogicalValueType());

        tiny.put("d", 4); // should trigger switch to backing map
        assertEquals(CompactMap.LogicalValueType.MAP, tiny.getLogicalValueType());
        assertEquals(4, tiny.size());
        assertEquals(Integer.valueOf(4), tiny.get("d"));
    }

    @Test
    public void testRemoveTransitionsBackToArrayAndSingleEntry() {
        CompactMap<String, Integer> tiny = CompactMap.builder()
                .compactSize(3)
                .build();

        tiny.put("a", 1);
        tiny.put("b", 2);
        tiny.put("c", 3);
        tiny.put("d", 4); // now in MAP state
        assertEquals(CompactMap.LogicalValueType.MAP, tiny.getLogicalValueType());

        tiny.remove("d"); // size == compactSize -> should go back to ARRAY
        assertEquals(CompactMap.LogicalValueType.ARRAY, tiny.getLogicalValueType());

        tiny.remove("c"); // now only 2 entries -> ARRAY stays
        assertEquals(CompactMap.LogicalValueType.ARRAY, tiny.getLogicalValueType());

        tiny.remove("b"); // only one entry left -> SINGLE ENTRY
        assertEquals(CompactMap.LogicalValueType.ENTRY, tiny.getLogicalValueType());

        tiny.remove("a"); // empty again
        assertTrue(tiny.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, tiny.getLogicalValueType());
    }

    /*** -----------------------------------------------------------------
     *  3. containsKey / containsValue edge cases
     * ----------------------------------------------------------------- */
    @Test
    public void testContainsKeyAndValueInAllStates() {
        // empty
        assertFalse(map.containsKey("x"));
        assertFalse(map.containsValue(10));

        // single entry (optimized object)
        map.put("id", 42);
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsValue(42));

        // add second entry -> array state
        map.put("other", 13);
        assertTrue(map.containsKey("other"));
        assertTrue(map.containsValue(13));

        // add more to trigger map state
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsValue(3));
    }

    /*** -----------------------------------------------------------------
     *  4. Ordering options (sorted, reverse, insertion)
     * ----------------------------------------------------------------- */
    @Test
    public void testSortedOrderIteration() {
        CompactMap<String, Integer> sorted = CompactMap.builder()
                .sortedOrder()
                .compactSize(5)
                .build();

        sorted.put("b", 2);
        sorted.put("c", 3);
        sorted.put("a", 1);

        // Keys should be iterated in natural order a,b,c
        Iterator<String> it = sorted.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());

        // Values should follow the same order
        Iterator<Integer> vit = sorted.values().iterator();
        assertEquals(Integer.valueOf(1), vit.next());
        assertEquals(Integer.valueOf(2), vit.next());
        assertEquals(Integer.valueOf(3), vit.next());
    }

    @Test
    public void testReverseOrderIteration() {
        CompactMap<String, Integer> rev = CompactMap.builder()
                .reverseOrder()
                .compactSize(5)
                .build();

        rev.put("a", 1);
        rev.put("b", 2);
        rev.put("c", 3);

        // Expected reverse natural order: c,b,a
        List<String> keys = new ArrayList<>();
        rev.keySet().forEach(keys::add);
        assertEquals(Arrays.asList("c", "b", "a"), keys);
    }

    @Test
    public void testInsertionOrderIteration() {
        CompactMap<String, Integer> ins = CompactMap.builder()
                .insertionOrder()
                .compactSize(5)
                .build();

        ins.put("first", 1);
        ins.put("second", 2);
        ins.put("third", 3);

        List<String> keys = new ArrayList<>();
        ins.keySet().forEach(keys::add);
        assertEquals(Arrays.asList("first", "second", "third"), keys);
    }

    /*** -----------------------------------------------------------------
     *  5. Case‑sensitivity option
     * ----------------------------------------------------------------- */
    @Test
    public void testCaseInsensitiveKeys() {
        CompactMap<String, Integer> ci = CompactMap.builder()
                .caseSensitive(false)
                .compactSize(5)
                .build();

        ci.put("Key", 100);
        assertTrue(ci.containsKey("key"));
        assertTrue(ci.containsKey("KEY"));
        assertEquals(Integer.valueOf(100), ci.get("kEy"));
    }

    /*** -----------------------------------------------------------------
     *  6. Collection view behaviours (keySet, values, entrySet)
     * ----------------------------------------------------------------- */
    @Test
    public void testKeySetRemove() {
        map.put("x", 10);
        map.put("y", 20);
        assertTrue(map.keySet().contains("x"));
        assertTrue(map.keySet().remove("x"));
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());
    }

    @Test
    public void testValuesRemove() {
        map.put("a", 1);
        map.put("b", 2);
        Collection<Integer> vals = map.values();
        assertTrue(vals.contains(1));
        assertTrue(vals.remove(1));
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetRemoveAndContains() {
        map.put("p", 5);
        map.put("q", 6);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("p", 5);
        assertTrue(entries.contains(entry));

        // remove via entrySet
        assertTrue(entries.remove(entry));
        assertFalse(map.containsKey("p"));
        assertEquals(1, map.size());
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModificationDetection() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        map.put("c", 3); // structural change after iterator creation
        it.next(); // should throw CME
    }

    /*** -----------------------------------------------------------------
     *  7. equals() and hashCode() contract
     * ----------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCodeWithStandardMap() {
        Map<String, Integer> std = new HashMap<>();
        std.put("one", 1);
        std.put("two", 2);

        CompactMap<String, Integer> cm = new CompactMap<>();
        cm.putAll(std);

        assertTrue(cm.equals(std));
        assertTrue(std.equals(cm));
        assertEquals(std.hashCode(), cm.hashCode());
    }

    @Test
    public void testEqualsHandlesNullValues() {
        map.put("k", null);
        Map<String, Integer> std = new HashMap<>();
        std.put("k", null);
        assertTrue(map.equals(std));
    }

    /*** -----------------------------------------------------------------
     *  8. toString() delegating to MapUtilities (basic sanity)
     * ----------------------------------------------------------------- */
    @Test
    public void testToStringFormat() {
        map.put("a", 1);
        map.put("b", 2);
        String s = map.toString();
        // Order is unspecified for default unordered map – just check content
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("a=1"));
        assertTrue(s.contains("b=2"));
    }

    /*** -----------------------------------------------------------------
     *  9. getConfig() and withConfig()
     * ----------------------------------------------------------------- */
    @Test
    public void testGetConfigReflectsBuilderSettings() {
        CompactMap<String, Integer> cfgMap = CompactMap.builder()
                .caseSensitive(false)
                .compactSize(7)
                .sortedOrder()
                .singleValueKey("uid")
                .mapType(LinkedHashMap.class)
                .build();

        Map<String, Object> cfg = cfgMap.getConfig();
        assertEquals(false, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(7, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
        assertEquals("uid", cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(LinkedHashMap.class, cfg.get(CompactMap.MAP_TYPE));
    }

    @Test
    public void testWithConfigCreatesNewMapWithChangedOptions() {
        CompactMap<String, Integer> original = CompactMap.builder()
                .compactSize(4)
                .insertionOrder()
                .build();
        original.put("x", 1);
        original.put("y", 2);

        Map<String, Object> newCfg = new HashMap<>();
        newCfg.put(CompactMap.ORDERING, CompactMap.SORTED);
        CompactMap<String, Integer> sorted = original.withConfig(newCfg);

        // Verify ordering changed while entries are preserved
        List<String> keys = new ArrayList<>();
        sorted.keySet().forEach(keys::add);
        assertEquals(Arrays.asList("x", "y"), keys); // insertion order before sorting
        // After sorting, iteration must be natural order
        sorted.clear();
        sorted.put("b", 2);
        sorted.put("a", 1);
        keys.clear();
        sorted.keySet().forEach(keys::add);
        assertEquals(Arrays.asList("a", "b"), keys);
    }

    /*** -----------------------------------------------------------------
     * 10. Builder validation – illegal options
     * ----------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsCompactSizeBelowTwo() {
        CompactMap.builder().compactSize(1).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsUnsupportedMapType() {
        CompactMap.builder().mapType(IdentityHashMap.class).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsWeakHashMap() {
        CompactMap.builder().mapType(WeakHashMap.class).build();
    }

    /*** -----------------------------------------------------------------
     * 11. CompactMapEntry behavior (setValue write‑through)
     * ----------------------------------------------------------------- */
    @Test
    public void testCompactMapEntrySetValueUpdatesParentMap() {
        CompactMap<String, String> cm = new CompactMap<>();
        cm.put("id", "first"); // optimized single‑value storage
        // Force entry creation
        cm.put("other", "second");

        // Retrieve entry via entrySet iterator
        Map.Entry<String, String> entry = cm.entrySet().iterator().next();
        assertTrue(entry instanceof CompactMap.CompactMapEntry);
        entry.setValue("changed");
        assertEquals("changed", cm.get(entry.getKey()));
    }

    /*** -----------------------------------------------------------------
     * 12. CompactMapComparator correctness
     * ----------------------------------------------------------------- */
    @Test
    public void testCompactMapComparatorStringCaseInsensitive() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(true, false);
        assertEquals(0, comp.compare("Abc", "abc"));
        assertTrue(comp.compare("abc", "def") < 0);
    }

    @Test
    public void testCompactMapComparatorReverseOrdering() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, true);
        assertTrue(comp.compare(1, 2) > 0); // reverse of natural order
    }

    /*** -----------------------------------------------------------------
     * 13. getLogicalValueType for each state (sanity)
     * ----------------------------------------------------------------- */
    @Test
    public void testLogicalValueTypeTransitions() {
        CompactMap<String, Integer> m = new CompactMap<>();
        assertEquals(CompactMap.LogicalValueType.EMPTY, m.getLogicalValueType());

        m.put("id", 99); // optimized object storage
        assertEquals(CompactMap.LogicalValueType.OBJECT, m.getLogicalValueType());

        m.clear();
        m.put("a", 1); // entry storage
        assertEquals(CompactMap.LogicalValueType.ENTRY, m.getLogicalValueType());

        m.put("b", 2); // array storage
        assertEquals(CompactMap.LogicalValueType.ARRAY, m.getLogicalValueType());

        // Force map storage with tiny compactSize
        CompactMap<String, Integer> tiny = CompactMap.builder().compactSize(3).build();
        tiny.put("a", 1);
        tiny.put("b", 2);
        tiny.put("c", 3);
        tiny.put("d", 4);
        assertEquals(CompactMap.LogicalValueType.MAP, tiny.getLogicalValueType());
    }

    /*** -----------------------------------------------------------------
     * 14. isDefaultCompactMap() – should be true for a plain instance
     * ----------------------------------------------------------------- */
    @Test
    public void testIsDefaultCompactMap() {
        CompactMap<String, Integer> defaultMap = new CompactMap<>();
        assertTrue(defaultMap.isDefaultCompactMap());
    }

    /*** -----------------------------------------------------------------
     * 15. putAll with null or empty map does nothing
     * ----------------------------------------------------------------- */
    @Test
    public void testPutAllWithNullOrEmptyDoesNothing() {
        CompactMap<String, Integer> empty = new CompactMap<>();
        empty.putAll(Collections.emptyMap());
        assertTrue(empty.isEmpty());

        try {
            empty.putAll(null);
            fail("Expected NullPointerException for null argument");
        } catch (NullPointerException ignored) {
        }
    }
}
