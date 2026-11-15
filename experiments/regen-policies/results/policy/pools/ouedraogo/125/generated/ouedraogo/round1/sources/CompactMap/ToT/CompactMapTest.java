package com.cedarsoftware.util;
import com.cedarsoftware.util.CompactMap.CompactMapComparator;


import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (default & builder) and validation of configuration options</li>
 *   <li>All public Map methods (size, isEmpty, get, put, remove, putAll, clear, …)</li>
 *   <li>Special storage states (EMPTY, OBJECT, ENTRY, ARRAY, MAP) via reflection</li>
 *   <li>Case‑insensitive key handling</li>
 *   <li>Ordering (sorted, reverse, insertion)</li>
 *   <li>Transition between storage states (single → array → map and back)</li>
 *   <li>Iterators (key, value, entry) including concurrent‑modification detection</li>
 *   <li>hashCode / equals contract</li>
 *   <li>CompactMapComparator behaviour</li>
 *   <li>Builder error handling (invalid compactSize, unsupported map types)</li>
 * </ul>
 */
@RunWith(JUnit4.class)
public class CompactMapTest {

    private CompactMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactMap<>();
    }

    @After
    public void tearDown() {
        map = null;
    }

    /* -------------------------------------------------------------
     * Helper utilities for accessing protected members via reflection
     * ------------------------------------------------------------- */
    private CompactMap.LogicalValueType getLogicalValueType(CompactMap<?, ?> m) throws Exception {
        java.lang.reflect.Method mtd = CompactMap.class.getDeclaredMethod("getLogicalValueType");
        mtd.setAccessible(true);
        return (CompactMap.LogicalValueType) mtd.invoke(m);
    }

    private Object getLogicalSingleKey(CompactMap<?, ?> m) throws Exception {
        java.lang.reflect.Method mtd = CompactMap.class.getDeclaredMethod("getLogicalSingleKey");
        mtd.setAccessible(true);
        return mtd.invoke(m);
    }

    private Object getLogicalSingleValue(CompactMap<?, ?> m) throws Exception {
        java.lang.reflect.Method mtd = CompactMap.class.getDeclaredMethod("getLogicalSingleValue");
        mtd.setAccessible(true);
        return mtd.invoke(m);
    }

    /* -------------------------------------------------------------
     * 1. Basic empty map behaviour
     * ------------------------------------------------------------- */
    @Test
    public void testEmptyMapBehavior() throws Exception {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertNull(map.get("any"));
        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue(123));
        assertEquals(CompactMap.LogicalValueType.EMPTY, getLogicalValueType(map));
        // toString on empty map should be {}
        assertEquals("{}", map.toString());
    }

    /* -------------------------------------------------------------
     * 2. Single entry stored as OBJECT (optimized singleValueKey)
     * ------------------------------------------------------------- */
    @Test
    public void testSingleEntryOptimizedStorage() throws Exception {
        // DEFAULT_SINGLE_KEY = "id"
        map.put("id", 42);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(42), map.get("id"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, getLogicalValueType(map));
        assertEquals("id", getLogicalSingleKey(map));
        assertEquals(Integer.valueOf(42), getLogicalSingleValue(map));
    }

    /* -------------------------------------------------------------
     * 3. Single entry stored as ENTRY (different key)
     * ------------------------------------------------------------- */
    @Test
    public void testSingleEntryRegularStorage() throws Exception {
        map.put("foo", 7);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(7), map.get("foo"));
        assertEquals(CompactMap.LogicalValueType.ENTRY, getLogicalValueType(map));
        assertEquals("foo", getLogicalSingleKey(map));
        assertEquals(Integer.valueOf(7), getLogicalSingleValue(map));
    }

    /* -------------------------------------------------------------
     * 4. Compact array storage and transition to backing map
     * ------------------------------------------------------------- */
    @Test
    public void testCompactArrayAndMapTransition() throws Exception {
        // Use a tiny compactSize to force transition quickly
        CompactMap<String, Integer> tiny = CompactMap.<String, Integer>builder()
                .compactSize(3)                // threshold = 3
                .mapType(HashMap.class)        // explicit map type
                .build();

        // Add three entries – should be in ARRAY state
        tiny.put("a", 1);
        tiny.put("b", 2);
        tiny.put("c", 3);
        assertEquals(3, tiny.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, getLogicalValueType(tiny));

        // Adding fourth entry forces transition to MAP state
        tiny.put("d", 4);
        assertEquals(4, tiny.size());
        assertEquals(CompactMap.LogicalValueType.MAP, getLogicalValueType(tiny));

        // Verify all entries are present
        assertTrue(tiny.containsKey("a"));
        assertTrue(tiny.containsKey("d"));
        assertEquals(Integer.valueOf(2), tiny.get("b"));
    }

    /* -------------------------------------------------------------
     * 5. Case‑insensitive key handling
     * ------------------------------------------------------------- */
    @Test
    public void testCaseInsensitiveKeys() throws Exception {
        CompactMap<String, Integer> ci = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .build();

        ci.put("Key", 10);
        assertTrue(ci.containsKey("key"));
        assertTrue(ci.containsKey("KEY"));
        assertEquals(Integer.valueOf(10), ci.get("kEy"));
        // Ensure only one entry stored
        assertEquals(1, ci.size());
    }

    /* -------------------------------------------------------------
     * 6. Sorted ordering (natural order)
     * ------------------------------------------------------------- */
    @Test
    public void testSortedOrderingIteration() throws Exception {
        CompactMap<Integer, String> sorted = CompactMap.<Integer, String>builder()
                .sortedOrder()
                .compactSize(10)   // keep in ARRAY state for easier inspection
                .build();

        sorted.put(5, "five");
        sorted.put(2, "two");
        sorted.put(8, "eight");

        // Keys should be iterated in natural ascending order
        Iterator<Integer> it = sorted.keySet().iterator();
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(5), it.next());
        assertEquals(Integer.valueOf(8), it.next());
        assertFalse(it.hasNext());
    }

    /* -------------------------------------------------------------
     * 7. Reverse ordering
     * ------------------------------------------------------------- */
    @Test
    public void testReverseOrderingIteration() throws Exception {
        CompactMap<Integer, String> rev = CompactMap.<Integer, String>builder()
                .reverseOrder()
                .compactSize(10)
                .build();

        rev.put(1, "one");
        rev.put(3, "three");
        rev.put(2, "two");

        Iterator<Integer> it = rev.keySet().iterator();
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(2), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertFalse(it.hasNext());
    }

    /* -------------------------------------------------------------
     * 8. Insertion ordering
     * ------------------------------------------------------------- */
    @Test
    public void testInsertionOrderIteration() throws Exception {
        CompactMap<String, Integer> ins = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .compactSize(10)
                .build();

        ins.put("first", 1);
        ins.put("second", 2);
        ins.put("third", 3);

        Iterator<String> it = ins.keySet().iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
        assertFalse(it.hasNext());
    }

    /* -------------------------------------------------------------
     * 9. Iterator concurrent‑modification detection
     * ------------------------------------------------------------- */
    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModification() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        // Modify map after iterator creation
        map.put("c", 3);
        // Next should detect the structural change
        it.next();   // throws ConcurrentModificationException
    }

    /* -------------------------------------------------------------
     * 10. Remove causing transition back to single entry
     * ------------------------------------------------------------- */
    @Test
    public void testRemoveTransitionToSingleEntry() throws Exception {
        CompactMap<String, Integer> tiny = CompactMap.<String, Integer>builder()
                .compactSize(3)
                .build();

        tiny.put("x", 100);
        tiny.put("y", 200);    // now in ARRAY state (size 2)
        assertEquals(CompactMap.LogicalValueType.ARRAY, getLogicalValueType(tiny));

        // Remove one key – should become single entry (OBJECT or ENTRY)
        tiny.remove("y");
        assertEquals(1, tiny.size());
        CompactMap.LogicalValueType type = getLogicalValueType(tiny);
        assertTrue(type == CompactMap.LogicalValueType.OBJECT || type == CompactMap.LogicalValueType.ENTRY);
        assertEquals(Integer.valueOf(100), tiny.get("x"));
    }

    /* -------------------------------------------------------------
     * 11. hashCode / equals contract with a standard HashMap
     * ------------------------------------------------------------- */
    @Test
    public void testHashCodeAndEquals() {
        Map<String, Integer> std = new HashMap<>();
        std.put("a", 1);
        std.put("b", 2);
        std.put("c", 3);

        CompactMap<String, Integer> cm = new CompactMap<>();
        cm.putAll(std);

        assertEquals(std.hashCode(), cm.hashCode());
        assertTrue(cm.equals(std));
        assertTrue(std.equals(cm));
    }

    /* -------------------------------------------------------------
     * 12. entrySet retainAll / removeAll semantics
     * ------------------------------------------------------------- */
    @Test
    public void testEntrySetRetainAllAndRemoveAll() {
        CompactMap<String, Integer> m = new CompactMap<>();
        m.put("one", 1);
        m.put("two", 2);
        m.put("three", 3);

        Set<Map.Entry<String, Integer>> entries = m.entrySet();

        // retain only "one" and "three"
        Set<Map.Entry<String, Integer>> toKeep = new HashSet<>();
        toKeep.add(new AbstractMap.SimpleEntry<>("one", 1));
        toKeep.add(new AbstractMap.SimpleEntry<>("three", 3));
        entries.retainAll(toKeep);
        assertEquals(2, m.size());
        assertTrue(m.containsKey("one"));
        assertTrue(m.containsKey("three"));
        assertFalse(m.containsKey("two"));

        // remove all remaining entries via removeAll
        entries.removeAll(toKeep);
        assertTrue(m.isEmpty());
    }

    /* -------------------------------------------------------------
     * 13. CompactMapComparator behaviour
     * ------------------------------------------------------------- */
    @Test
    public void testCompactMapComparator() {
        CompactMapComparator cmp = new CompactMapComparator(true, false); // case‑insensitive, natural order

        // case‑insensitive comparison
        assertEquals(0, cmp.compare("ABC", "abc"));
        assertTrue(cmp.compare("a", "b") < 0);
        assertTrue(cmp.compare("z", "y") > 0);

        // reverse order
        CompactMapComparator rev = new CompactMapComparator(false, true);
        assertTrue(rev.compare(1, 2) > 0); // reversed
        assertEquals(0, rev.compare(null, null));
        assertTrue(rev.compare(null, 5) > 0); // nulls are always last, reverse makes them first
    }

    /* -------------------------------------------------------------
     * 14. Builder validation – compactSize too small
     * ------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsInvalidCompactSize() {
        CompactMap.<String, Integer>builder()
                .compactSize(1)    // less than required minimum 2
                .build();
    }

    /* -------------------------------------------------------------
     * 15. Builder validation – unsupported map type (IdentityHashMap)
     * ------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsIdentityHashMap() {
        CompactMap.<String, Integer>builder()
                .mapType(IdentityHashMap.class)   // not allowed
                .build();
    }

    /* -------------------------------------------------------------
     * 16. putAll with source map larger than compactSize
     * ------------------------------------------------------------- */
    @Test
    public void testPutAllTransitionsDirectlyToMap() {
        // Builder with compactSize = 2
        CompactMap<String, Integer> m = CompactMap.<String, Integer>builder()
                .compactSize(2)
                .build();

        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        src.put("c", 3); // size 3 > compactSize

        m.putAll(src);
        assertEquals(3, m.size());
        assertEquals(CompactMap.LogicalValueType.MAP, getLogicalValueType(m));
    }

    /* -------------------------------------------------------------
     * 17. clear resets to EMPTY state
     * ------------------------------------------------------------- */
    @Test
    public void testClearResetsState() throws Exception {
        map.put("x", 10);
        map.put("y", 20);
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, getLogicalValueType(map));
    }

    /* -------------------------------------------------------------
     * 18. CompactMapEntry setValue writes through to map
     * ------------------------------------------------------------- */
    @Test
    public void testCompactMapEntrySetValueWritesThrough() {
        CompactMap<String, Integer> m = new CompactMap<>();
        m.put("key", 5);
        // Retrieve entry via entrySet iterator
        Map.Entry<String, Integer> entry = m.entrySet().iterator().next();
        assertTrue(entry instanceof CompactMap.CompactMapEntry);
        entry.setValue(99);
        // The change must be reflected in the map
        assertEquals(Integer.valueOf(99), m.get("key"));
    }

    /* -------------------------------------------------------------
     * 19. toString format matches MapUtilities.mapToString (braces)
     * ------------------------------------------------------------- */
    @Test
    public void testToStringFormatting() {
        map.put("a", 1);
        map.put("b", 2);
        String s = map.toString();
        // Order is unspecified for unordered map, but format must be {k=v, ...}
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("a=1"));
        assertTrue(s.contains("b=2"));
    }

    /* -------------------------------------------------------------
     * 20. values collection reflects map changes
     * ------------------------------------------------------------- */
    @Test
    public void testValuesCollectionReflectsUpdates() {
        map.put("x", 100);
        map.put("y", 200);
        Collection<Integer> vals = map.values();
        assertTrue(vals.contains(100));
        assertTrue(vals.contains(200));

        // Remove a key via map.remove – collection must update
        map.remove("x");
        assertFalse(vals.contains(100));
        assertEquals(1, vals.size());

        // Clear via collection
        vals.clear();
        assertTrue(map.isEmpty());
    }

    /* -------------------------------------------------------------
     * 21. keySet remove returns true only when a key was actually removed
     * ------------------------------------------------------------- */
    @Test
    public void testKeySetRemoveBehaviour() {
        map.put("alpha", 1);
        map.put("beta", 2);
        Set<String> ks = map.keySet();

        assertTrue(ks.remove("alpha"));
        assertFalse(ks.remove("gamma")); // not present
        assertEquals(1, map.size());
        assertFalse(map.containsKey("alpha"));
    }

    /* -------------------------------------------------------------
     * 22. entrySet contains works with equal entries (case‑insensitive key)
     * ------------------------------------------------------------- */
    @Test
    public void testEntrySetContainsWithCaseInsensitiveKey() {
        CompactMap<String, Integer> ci = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .build();

        ci.put("KeyOne", 10);
        Map.Entry<String, Integer> probe = new AbstractMap.SimpleEntry<>("keyone", 10);
        assertTrue(ci.entrySet().contains(probe));
    }

    /* -------------------------------------------------------------
     * 23. remove from map when in MAP state reduces size and may transition back to ARRAY
     * ------------------------------------------------------------- */
    @Test
    public void testRemoveFromMapTransitionsBackToArray() throws Exception {
        // compactSize = 3, so 4 entries -> MAP, then removal to 3 -> ARRAY
        CompactMap<String, Integer> m = CompactMap.<String, Integer>builder()
                .compactSize(3)
                .build();

        m.put("a", 1);
        m.put("b", 2);
        m.put("c", 3);
        m.put("d", 4); // now MAP
        assertEquals(CompactMap.LogicalValueType.MAP, getLogicalValueType(m));

        // Remove one entry, size becomes 3 == compactSize, should transition to ARRAY
        m.remove("d");
        assertEquals(3, m.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, getLogicalValueType(m));
    }

    /* -------------------------------------------------------------
     * 24. builder sourceMap initialization
     * ------------------------------------------------------------- */
    @Test
    public void testBuilderSourceMapInitialization() {
        Map<String, Integer> src = new HashMap<>();
        src.put("one", 1);
        src.put("two", 2);
        CompactMap<String, Integer> m = CompactMap.<String, Integer>builder()
                .sourceMap(src)
                .build();

        assertEquals(2, m.size());
        assertEquals(Integer.valueOf(1), m.get("one"));
        assertEquals(Integer.valueOf(2), m.get("two"));
    }

    /* -------------------------------------------------------------
     * 25. verify that put with null value works and does not affect state
     * ------------------------------------------------------------- */
    @Test
    public void testPutNullValue() throws Exception {
        map.put("nullKey", null);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("nullKey"));
        assertTrue(map.containsValue(null));
        assertNull(map.get("nullKey"));
        // State should be OBJECT because key equals default singleValueKey ("id")? No, key is different -> ENTRY
        assertEquals(CompactMap.LogicalValueType.ENTRY, getLogicalValueType(map));
    }
}
