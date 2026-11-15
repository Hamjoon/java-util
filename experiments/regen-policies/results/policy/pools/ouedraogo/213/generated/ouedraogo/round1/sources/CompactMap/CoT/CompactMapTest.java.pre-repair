package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

import static org.junit.Assert.*;

public class CompactMapTest {

    private CompactMap<String, String> map;

    @Before
    public void setUp() {
        map = new CompactMap<>();
    }

    @After
    public void tearDown() {
        map = null;
    }

    /* -------------------------------------------------------------
     *  Basic public API tests – empty map
     * ------------------------------------------------------------- */
    @Test
    public void testEmptyMapState() {
        assertTrue("Map should be empty", map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());
        assertTrue(map.entrySet().isEmpty());
    }

    /* -------------------------------------------------------------
     *  Single‑value optimisation (key == getSingleValueKey())
     * ------------------------------------------------------------- */
    @Test
    public void testSingleValueOptimisation() {
        // default single value key is "id"
        map.put("id", "single");
        assertEquals(1, map.size());
        assertEquals("single", map.get("id"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // overwrite same key – should stay OBJECT
        map.put("id", "new");
        assertEquals(1, map.size());
        assertEquals("new", map.get("id"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());
    }

    /* -------------------------------------------------------------
     *  Single entry that is NOT the special key – uses ENTRY
     * ------------------------------------------------------------- */
    @Test
    public void testSingleEntryState() {
        map.put("key", "value");
        assertEquals(1, map.size());
        assertEquals("value", map.get("key"));
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        // overwrite same key – still ENTRY
        map.put("key", "newValue");
        assertEquals(1, map.size());
        assertEquals("newValue", map.get("key"));
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
    }

    /* -------------------------------------------------------------
     *  Transition from ENTRY to ARRAY (2 entries)
     * ------------------------------------------------------------- */
    @Test
    public void testTransitionToArray() {
        map.put("a", "1");
        map.put("b", "2");
        assertEquals(2, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
    }

    /* -------------------------------------------------------------
     *  Transition from ARRAY to MAP (exceed compactSize)
     * ------------------------------------------------------------- */
    @Test
    public void testTransitionToMap() {
        // default compactSize is 50, add 51 entries
        for (int i = 0; i < CompactMap.DEFAULT_COMPACT_SIZE + 1; i++) {
            map.put("k" + i, "v" + i);
        }
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE + 1, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // verify a few values
        assertEquals("v0", map.get("k0"));
        assertEquals("v50", map.get("k50"));
    }

    /* -------------------------------------------------------------
     *  Transition back from MAP to ARRAY when size falls to compactSize
     * ------------------------------------------------------------- */
    @Test
    public void testMapToArrayTransitionOnRemoval() {
        // fill to MAP state
        for (int i = 0; i < CompactMap.DEFAULT_COMPACT_SIZE + 1; i++) {
            map.put("k" + i, "v" + i);
        }
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // remove one entry – size becomes compactSize (50) → should go back to ARRAY
        map.remove("k0");
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
    }

    /* -------------------------------------------------------------
     *  Removal back to single entry / OBJECT state
     * ------------------------------------------------------------- */
    @Test
    public void testRemovalToSingleEntryAndObject() {
        map.put("id", "first"); // OBJECT state
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // add a second entry – becomes ARRAY
        map.put("other", "second");
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // remove the non‑special key – should revert to OBJECT (single value key)
        map.remove("other");
        assertEquals(1, map.size());
        assertEquals("first", map.get("id"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // now remove the special key – map becomes EMPTY
        map.remove("id");
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /* -------------------------------------------------------------
     *  Null key handling (HashMap permits null)
     * ------------------------------------------------------------- */
    @Test
    public void testNullKeySupport() {
        map.put(null, "nullValue");
        assertEquals(1, map.size());
        assertTrue(map.containsKey(null));
        assertEquals("nullValue", map.get(null));

        map.put(null, "newNull");
        assertEquals(1, map.size());
        assertEquals("newNull", map.get(null));

        map.remove(null);
        assertTrue(map.isEmpty());
    }

    /* -------------------------------------------------------------
     *  equals() and hashCode() consistency with a standard HashMap
     * ------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode() {
        Map<String, String> std = new HashMap<>();
        std.put("a", "1");
        std.put("b", "2");
        std.put("c", "3");

        map.putAll(std);

        assertTrue(map.equals(std));
        assertTrue(std.equals(map));
        assertEquals(std.hashCode(), map.hashCode());
    }

    /* -------------------------------------------------------------
     *  keySet, values, entrySet iteration order for default (unordered)
     * ------------------------------------------------------------- */
    @Test
    public void testUnorderedIteration() {
        map.put("x", "1");
        map.put("y", "2");
        map.put("z", "3");

        Set<String> keys = new LinkedHashSet<>(map.keySet()); // preserve iteration order
        Collection<String> values = new ArrayList<>(map.values());

        // Since default ordering is unordered, we only assert that all elements are present
        assertEquals(new HashSet<>(Arrays.asList("x", "y", "z")), keys);
        assertEquals(new HashSet<>(Arrays.asList("1", "2", "3")), new HashSet<>(values));
    }

    /* -------------------------------------------------------------
     *  Iterator concurrent‑modification detection
     * ------------------------------------------------------------- */
    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModification() {
        map.put("a", "1");
        map.put("b", "2");
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        map.put("c", "3"); // structural change after iterator creation
        it.next(); // should throw CME
    }

    /* -------------------------------------------------------------
     *  Builder – case‑insensitive map
     * ------------------------------------------------------------- */
    @Test
    public void testBuilderCaseInsensitive() {
        CompactMap<String, String> ciMap = CompactMap.<String, String>builder()
                .caseSensitive(false)
                .build();

        ciMap.put("Key", "value");
        assertEquals(1, ciMap.size());
        assertTrue(ciMap.containsKey("key"));
        assertTrue(ciMap.containsKey("KEY"));
        assertEquals("value", ciMap.get("kEy"));
        assertEquals(CompactMap.LogicalValueType.ENTRY, ciMap.getLogicalValueType());
    }

    /* -------------------------------------------------------------
     *  Builder – sorted order
     * ------------------------------------------------------------- */
    @Test
    public void testBuilderSortedOrder() {
        CompactMap<String, Integer> sorted = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .build();

        sorted.put("b", 2);
        sorted.put("c", 3);
        sorted.put("a", 1);

        Iterator<String> it = sorted.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());

        // values should follow the same order
        Iterator<Integer> vit = sorted.values().iterator();
        assertEquals(Integer.valueOf(1), vit.next());
        assertEquals(Integer.valueOf(2), vit.next());
        assertEquals(Integer.valueOf(3), vit.next());
    }

    /* -------------------------------------------------------------
     *  Builder – reverse order
     * ------------------------------------------------------------- */
    @Test
    public void testBuilderReverseOrder() {
        CompactMap<String, Integer> rev = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .build();

        rev.put("b", 2);
        rev.put("c", 3);
        rev.put("a", 1);

        Iterator<String> it = rev.keySet().iterator();
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    /* -------------------------------------------------------------
     *  Builder – insertion order
     * ------------------------------------------------------------- */
    @Test
    public void testBuilderInsertionOrder() {
        CompactMap<String, Integer> ins = CompactMap.<String, Integer>builder()
                .insertionOrder()
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
     *  getConfig() – default configuration verification
     * ------------------------------------------------------------- */
    @Test
    public void testGetConfigDefaults() {
        Map<String, Object> cfg = map.getConfig();

        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.TRUE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.UNORDERED, cfg.get(CompactMap.ORDERING));
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(CompactMap.DEFAULT_MAP_TYPE, cfg.get(CompactMap.MAP_TYPE));
    }

    /* -------------------------------------------------------------
     *  withConfig() – change ordering while preserving entries
     * ------------------------------------------------------------- */
    @Test
    public void testWithConfigChangesOrdering() {
        // populate default map
        map.put("x", "1");
        map.put("y", "2");
        map.put("z", "3");

        // request insertion order via config
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(CompactMap.ORDERING, CompactMap.INSERTION);

        CompactMap<String, String> newMap = map.withConfig(cfg);
        assertEquals(3, newMap.size());

        // verify that iteration follows insertion order (x, y, z)
        Iterator<String> it = newMap.keySet().iterator();
        assertEquals("x", it.next());
        assertEquals("y", it.next());
        assertEquals("z", it.next());
        assertFalse(it.hasNext());
    }

    /* -------------------------------------------------------------
     *  validateAndFinalizeOptions – illegal compactSize
     * ------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testValidateAndFinalizeOptionsInvalidCompactSize() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(CompactMap.COMPACT_SIZE, 1); // too small
        CompactMap.validateAndFinalizeOptions(opts);
    }

    /* -------------------------------------------------------------
     *  newMap() – direct static factory with valid options
     * ------------------------------------------------------------- */
    @Test
    public void testStaticNewMapFactory() {
        Map<String, Object> opts = new HashMap<>();
        opts.put(CompactMap.COMPACT_SIZE, 10);
        opts.put(CompactMap.ORDERING, CompactMap.SORTED);
        opts.put(CompactMap.CASE_SENSITIVE, true);
        opts.put(CompactMap.MAP_TYPE, HashMap.class);

        CompactMap<String, Integer> created = CompactMap.newMap(opts);
        created.put("b", 2);
        created.put("a", 1);
        created.put("c", 3);

        Iterator<String> it = created.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
    }
}
