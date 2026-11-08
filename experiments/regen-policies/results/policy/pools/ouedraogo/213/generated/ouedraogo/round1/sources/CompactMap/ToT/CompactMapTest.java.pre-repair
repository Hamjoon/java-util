package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Construction and default configuration</li>
 *   <li>All public mutating operations (put, remove, putAll, clear)</li>
 *   <li>State transitions (EMPTY → OBJECT → ARRAY → MAP and the reverse)</li>
 *   <li>Case‑sensitivity, ordering (sorted / reverse / insertion) and the effect on lookup</li>
 *   <li>Views (keySet, values, entrySet) including remove / retainAll behaviour</li>
 *   <li>Equality, hashCode and toString contracts</li>
 *   <li>Builder validation and configuration propagation (including {@code withConfig})</li>
 * </ul>
 * </p>
 *
 * <p>All tests use only the public API of {@code CompactMap} and its nested classes,
 * as required by the exercise.</p>
 */
public class CompactMapTest
{
    private CompactMap<String, Object> emptyMap;

    @Before
    public void setUp()
    {
        emptyMap = new CompactMap<>();
    }

    /* -------------------------------------------------------------
     *  Construction / default configuration
     * ------------------------------------------------------------- */
    @Test
    public void testDefaultConstructionAndEmptyState()
    {
        assertTrue(emptyMap.isEmpty());
        assertEquals(0, emptyMap.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, emptyMap.getLogicalValueType());

        // default configuration values
        Map<String, Object> cfg = emptyMap.getConfig();
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE));
        assertTrue((Boolean) cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.UNORDERED, cfg.get(CompactMap.ORDERING));
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(CompactMap.DEFAULT_MAP_TYPE, cfg.get(CompactMap.MAP_TYPE));

        // isDefaultCompactMap should be true for a plain instance
        assertTrue(emptyMap.isDefaultCompactMap());
    }

    /* -------------------------------------------------------------
     *  Single‑entry optimised storage (OBJECT state)
     * ------------------------------------------------------------- */
    @Test
    public void testPutSingleEntryOptimisedStorage()
    {
        // use the default single‑value key ("id")
        emptyMap.put(CompactMap.DEFAULT_SINGLE_KEY, "singleValue");

        assertEquals(1, emptyMap.size());
        assertEquals("singleValue", emptyMap.get(CompactMap.DEFAULT_SINGLE_KEY));
        assertEquals(CompactMap.LogicalValueType.OBJECT, emptyMap.getLogicalValueType());

        // overwrite the same key – should stay in OBJECT state
        Object old = emptyMap.put(CompactMap.DEFAULT_SINGLE_KEY, "newValue");
        assertEquals("singleValue", old);
        assertEquals("newValue", emptyMap.get(CompactMap.DEFAULT_SINGLE_KEY));
        assertEquals(CompactMap.LogicalValueType.OBJECT, emptyMap.getLogicalValueType());
    }

    /* -------------------------------------------------------------
     *  Transition from OBJECT/ENTRY to ARRAY (2‑to‑compactSize entries)
     * ------------------------------------------------------------- */
    @Test
    public void testPutTwoEntriesCreatesArrayStorage()
    {
        emptyMap.put("a", 1);
        emptyMap.put("b", 2);               // second distinct key -> array

        assertEquals(2, emptyMap.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, emptyMap.getLogicalValueType());
        assertEquals(1, emptyMap.get("a"));
        assertEquals(2, emptyMap.get("b"));
    }

    /* -------------------------------------------------------------
     *  Transition to MAP when exceeding compactSize
     * ------------------------------------------------------------- */
    @Test
    public void testExceedCompactSizeTransitionsToMap()
    {
        // Builder with a tiny compactSize to force MAP transition quickly
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(3)                // 3 entries stay in array, 4th goes to map
                .build();

        map.put("k1", 1);
        map.put("k2", 2);
        map.put("k3", 3);                   // still ARRAY
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        map.put("k4", 4);                   // should switch to MAP
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
        assertEquals(4, map.size());
        assertEquals(Integer.valueOf(4), map.get("k4"));
    }

    /* -------------------------------------------------------------
     *  Case‑insensitive key handling
     * ------------------------------------------------------------- */
    @Test
    public void testCaseInsensitiveLookup()
    {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .caseSensitive(false)
                .build();

        map.put("Key", "value");
        assertTrue(map.containsKey("key"));
        assertEquals("value", map.get("KEY"));
        assertEquals(1, map.size());
    }

    /* -------------------------------------------------------------
     *  Sorted ordering and binary‑search based lookups
     * ------------------------------------------------------------- */
    @Test
    public void testSortedOrderIterationAndBinarySearch()
    {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(5)                // keep us in ARRAY for the test
                .build();

        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);                    // inserted out of order

        // iteration order must be sorted (a, b, c)
        List<String> keys = new ArrayList<>();
        for (String k : map.keySet())
        {
            keys.add(k);
        }
        assertEquals(Arrays.asList("a", "b", "c"), keys);

        // binary search should find values correctly
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertNull(map.get("z"));
    }

    /* -------------------------------------------------------------
     *  Reverse ordering
     * ------------------------------------------------------------- */
    @Test
    public void testReverseOrderIteration()
    {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .compactSize(5)
                .build();

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        List<String> keys = new ArrayList<>();
        for (String k : map.keySet())
        {
            keys.add(k);
        }
        // reverse order of natural sorting
        assertEquals(Arrays.asList("c", "b", "a"), keys);
    }

    /* -------------------------------------------------------------
     *  Removal causing transition back to single‑entry state
     * ------------------------------------------------------------- */
    @Test
    public void testRemoveFromArrayTransitionsToSingle()
    {
        emptyMap.put("one", 1);
        emptyMap.put("two", 2);                     // now ARRAY

        assertEquals(CompactMap.LogicalValueType.ARRAY, emptyMap.getLogicalValueType());

        // remove one entry – should become single entry (OBJECT/ENTRY)
        Object removed = emptyMap.remove("one");
        assertEquals(1, removed);
        assertEquals(1, emptyMap.size());
        assertTrue(emptyMap.containsKey("two"));
        assertTrue(emptyMap.getLogicalValueType() == CompactMap.LogicalValueType.OBJECT ||
                   emptyMap.getLogicalValueType() == CompactMap.LogicalValueType.ENTRY);
    }

    /* -------------------------------------------------------------
     *  putAll and sourceMap initialisation
     * ------------------------------------------------------------- */
    @Test
    public void testPutAllAndSourceMap()
    {
        Map<String, Integer> src = new HashMap<>();
        src.put("x", 10);
        src.put("y", 20);

        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sourceMap(src)
                .build();

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(10), map.get("x"));
        assertEquals(Integer.valueOf(20), map.get("y"));
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // add another entry via putAll – should stay consistent
        Map<String, Integer> extra = Collections.singletonMap("z", 30);
        map.putAll(extra);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(30), map.get("z"));
    }

    /* -------------------------------------------------------------
     *  Equality, hashCode and toString contracts
     * ------------------------------------------------------------- */
    @Test
    public void testEqualsHashCodeAndToString()
    {
        CompactMap<String, Integer> m1 = CompactMap.<String, Integer>builder()
                .compactSize(4)                 // stay in ARRAY
                .build();
        CompactMap<String, Integer> m2 = CompactMap.<String, Integer>builder()
                .compactSize(10)                // also ARRAY
                .build();

        m1.put("a", 1);
        m1.put("b", 2);
        m2.put("b", 2);
        m2.put("a", 1);

        assertEquals(m1, m2);
        assertEquals(m2, m1);
        assertEquals(m1.hashCode(), m2.hashCode());

        String str = m1.toString();
        // format is like {a=1, b=2} – order may differ for unordered maps
        assertTrue(str.startsWith("{") && str.endsWith("}"));
        assertTrue(str.contains("a=1"));
        assertTrue(str.contains("b=2"));
    }

    /* -------------------------------------------------------------
     *  keySet view – remove, retainAll, removeAll
     * ------------------------------------------------------------- */
    @Test
    public void testKeySetOperations()
    {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();

        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Set<String> ks = map.keySet();

        // remove via iterator
        Iterator<String> it = ks.iterator();
        while (it.hasNext())
        {
            String k = it.next();
            if ("two".equals(k))
            {
                it.remove();
            }
        }
        assertFalse(map.containsKey("two"));
        assertEquals(2, map.size());

        // retainAll – keep only "one"
        ks.retainAll(Collections.singleton("one"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("one"));
        assertFalse(map.containsKey("three"));

        // removeAll – clear remaining key
        ks.removeAll(Collections.singleton("one"));
        assertTrue(map.isEmpty());
    }

    /* -------------------------------------------------------------
     *  values view – iterator remove
     * ------------------------------------------------------------- */
    @Test
    public void testValuesIteratorRemove()
    {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Collection<Integer> vals = map.values();
        Iterator<Integer> it = vals.iterator();
        while (it.hasNext())
        {
            Integer v = it.next();
            if (v == 2)
            {
                it.remove();
            }
        }
        assertFalse(map.containsValue(2));
        assertEquals(2, map.size());
    }

    /* -------------------------------------------------------------
     *  entrySet view – contains, remove, retainAll
     * ------------------------------------------------------------- */
    @Test
    public void testEntrySetOperations()
    {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();

        map.put("x", 10);
        map.put("y", 20);

        Set<Map.Entry<String, Integer>> es = map.entrySet();

        // contains
        Map.Entry<String, Integer> e = new AbstractMap.SimpleEntry<>("x", 10);
        assertTrue(es.contains(e));

        // remove via iterator
        Iterator<Map.Entry<String, Integer>> it = es.iterator();
        while (it.hasNext())
        {
            Map.Entry<String, Integer> entry = it.next();
            if ("x".equals(entry.getKey()))
            {
                it.remove();
            }
        }
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());

        // retainAll – keep only y=20
        es.retainAll(Collections.singleton(new AbstractMap.SimpleEntry<>("y", 20)));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("y"));
    }

    /* -------------------------------------------------------------
     *  withConfig – create a new map with altered configuration
     * ------------------------------------------------------------- */
    @Test
    public void testWithConfigCreatesNewMap()
    {
        CompactMap<String, Integer> original = CompactMap.<String, Integer>builder()
                .compactSize(4)
                .caseSensitive(true)
                .sortedOrder()
                .build();

        original.put("b", 2);
        original.put("a", 1);
        original.put("c", 3);

        // change ordering to insertion order via config
        Map<String, Object> cfg = new HashMap<>(original.getConfig());
        cfg.put(CompactMap.ORDERING, CompactMap.INSERTION);
        CompactMap<String, Integer> newMap = original.withConfig(cfg);

        // same entries must be present
        assertEquals(original, newMap);
        assertEquals(3, newMap.size());

        // verify new ordering (insertion order)
        List<String> keys = new ArrayList<>();
        for (String k : newMap.keySet())
        {
            keys.add(k);
        }
        // insertion order should be the order elements were added to the new map
        // (which is the order of putAll inside withConfig – that preserves source iteration order)
        assertEquals(Arrays.asList("b", "a", "c"), keys);
    }

    /* -------------------------------------------------------------
     *  Builder validation – compactSize < 2 should throw IllegalArgumentException
     * ------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsInvalidCompactSize()
    {
        CompactMap.<String, Integer>builder()
                .compactSize(1)    // invalid, must be >=2
                .build();
    }

    /* -------------------------------------------------------------
     *  Ensure clear resets to EMPTY state
     * ------------------------------------------------------------- */
    @Test
    public void testClearResetsState()
    {
        emptyMap.put("k", "v");
        assertFalse(emptyMap.isEmpty());
        emptyMap.clear();
        assertTrue(emptyMap.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, emptyMap.getLogicalValueType());
        assertEquals(0, emptyMap.size());
    }

    /* -------------------------------------------------------------
     *  Test CompactMapComparator directly (used internally for sorting)
     * ------------------------------------------------------------- */
    @Test
    public void testCompactMapComparatorBehaviour()
    {
        CompactMap.CompactMapComparator cmp = new CompactMap.CompactMapComparator(false, false);
        assertTrue(cmp.compare("a", "b") < 0);
        assertEquals(0, cmp.compare("same", "same"));
        assertTrue(cmp.compare("b", "a") > 0);

        // reverse order
        CompactMap.CompactMapComparator rev = new CompactMap.CompactMapComparator(false, true);
        assertTrue(rev.compare("a", "b") > 0);
        assertTrue(rev.compare("b", "a") < 0);

        // case‑insensitive
        CompactMap.CompactMapComparator ci = new CompactMap.CompactMapComparator(true, false);
        assertEquals(0, ci.compare("Key", "kEy"));
    }

    /* -------------------------------------------------------------
     *  Test CompactMapEntry equality and hashCode (write‑through behaviour)
     * ------------------------------------------------------------- */
    @Test
    public void testCompactMapEntrySetValueWritesThrough()
    {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("id", 100);               // uses OBJECT optimisation

        // obtain entry via entrySet iterator
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        assertTrue(entry instanceof CompactMap.CompactMapEntry);
        assertEquals(Integer.valueOf(100), entry.getValue());

        // setValue should update the map as well
        entry.setValue(200);
        assertEquals(Integer.valueOf(200), map.get("id"));
        assertEquals(Integer.valueOf(200), entry.getValue());

        // equals / hashCode contract
        CompactMap.CompactMapEntry another = map.new CompactMapEntry("id", 200);
        assertEquals(entry, another);
        assertEquals(entry.hashCode(), another.hashCode());
    }
}
