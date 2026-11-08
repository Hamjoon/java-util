package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic map operations (size, put, get, remove, clear)</li>
 *   <li>state transitions (EMPTY → OBJECT → ARRAY → MAP)</li>
 *   <li>configuration options via the {@link Builder}</li>
 *   <li>ordering (sorted, reverse, insertion)</li>
 *   <li>case‑insensitive handling</li>
 *   <li>collection view behaviours (keySet, values, entrySet)</li>
 *   <li>equals / hashCode contracts</li>
 *   <li>error scenarios (illegal configuration, unsupported operations)</li>
 * </ul>
 */
public class CompactMapTest
{
    private CompactMap<String, String> map;

    @Before
    public void setUp()
    {
        map = new CompactMap<>();
    }

    /* --------------------------------------------------------------
     *  Basic empty‑map behaviour
     * -------------------------------------------------------------- */
    @Test
    public void testEmptyMap()
    {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /* --------------------------------------------------------------
     *  Single‑entry (optimized OBJECT) behaviour
     * -------------------------------------------------------------- */
    @Test
    public void testPutSingleEntryOptimized()
    {
        // default single‑value key is "id"
        String key = "id";
        String value = "first";
        assertNull(map.put(key, value));
        assertEquals(1, map.size());
        assertEquals(value, map.get(key));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // overwrite – should return previous value
        String newValue = "second";
        assertEquals(value, map.put(key, newValue));
        assertEquals(newValue, map.get(key));
        assertEquals(1, map.size());
    }

    @Test
    public void testPutSingleEntryRegular()
    {
        // key different from the single‑value key forces ENTRY storage
        assertNull(map.put("A", "alpha"));
        assertEquals(1, map.size());
        assertEquals("alpha", map.get("A"));
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
    }

    @Test
    public void testRemoveSingleEntry()
    {
        map.put("id", "value");
        assertEquals("value", map.remove("id"));
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /* --------------------------------------------------------------
     *  Transition to compact array (2 … compactSize)
     * -------------------------------------------------------------- */
    @Test
    public void testTransitionToArray()
    {
        map.put("A", "alpha");
        map.put("B", "beta");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // both keys must be present
        assertTrue(map.containsKey("A"));
        assertTrue(map.containsKey("B"));
        assertTrue(map.containsValue("alpha"));
        assertTrue(map.containsValue("beta"));
    }

    /* --------------------------------------------------------------
     *  Transition to backing map (size > compactSize)
     * -------------------------------------------------------------- */
    @Test
    public void testTransitionToMap()
    {
        // Use a builder with a tiny compact size to force the transition quickly
        CompactMap<String, String> small = CompactMap.<String, String>builder()
                .compactSize(3)          // 0‑2 entries → array, 3+ → map
                .build();

        small.put("k1", "v1");
        small.put("k2", "v2");
        small.put("k3", "v3"); // still array (size == compactSize)
        assertEquals(CompactMap.LogicalValueType.ARRAY, small.getLogicalValueType());

        small.put("k4", "v4"); // exceeds compactSize → map
        assertEquals(CompactMap.LogicalValueType.MAP, small.getLogicalValueType());
        assertEquals(4, small.size());
        assertTrue(small.containsKey("k4"));
        assertEquals("v4", small.get("k4"));
    }

    /* --------------------------------------------------------------
     *  Collection view behaviours
     * -------------------------------------------------------------- */
    @Test
    public void testKeySetAndIteratorRemove()
    {
        map.put("A", "alpha");
        map.put("B", "beta");
        Set<String> keys = map.keySet();
        assertEquals(2, keys.size());
        Iterator<String> it = keys.iterator();
        assertTrue(it.hasNext());
        assertEquals("A", it.next());
        it.remove(); // remove key "A"
        assertFalse(map.containsKey("A"));
        assertEquals(1, map.size());
    }

    @Test
    public void testValuesAndIteratorRemove()
    {
        map.put("X", "xray");
        map.put("Y", "yankee");
        Collection<String> values = map.values();
        assertEquals(2, values.size());

        Iterator<String> it = values.iterator();
        assertTrue(it.hasNext());
        assertEquals("xray", it.next());
        it.remove(); // removes entry with key "X"
        assertFalse(map.containsKey("X"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetAndIteratorRemove()
    {
        map.put("K1", "v1");
        map.put("K2", "v2");
        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(2, entries.size());

        Iterator<Map.Entry<String, String>> it = entries.iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, String> e = it.next();
        assertEquals("K1", e.getKey());
        it.remove(); // removes the entry
        assertFalse(map.containsKey("K1"));
        assertEquals(1, map.size());
    }

    /* --------------------------------------------------------------
     *  equals / hashCode / toString
     * -------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode()
    {
        map.put("a", "1");
        map.put("b", "2");

        CompactMap<String, String> other = new CompactMap<>();
        other.put("a", "1");
        other.put("b", "2");

        assertTrue(map.equals(other));
        assertTrue(other.equals(map));
        assertEquals(map.hashCode(), other.hashCode());

        other.put("c", "3");
        assertFalse(map.equals(other));
    }

    @Test
    public void testToString()
    {
        map.put("one", "1");
        map.put("two", "2");
        String s = map.toString();
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }

    /* --------------------------------------------------------------
     *  Builder configuration – ordering & case sensitivity
     * -------------------------------------------------------------- */
    @Test
    public void testSortedOrder()
    {
        CompactMap<String, Integer> sorted = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(10)
                .build();

        sorted.put("b", 2);
        sorted.put("a", 1);
        sorted.put("c", 3);

        Iterator<String> it = sorted.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
    }

    @Test
    public void testReverseOrder()
    {
        CompactMap<String, Integer> rev = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .compactSize(10)
                .build();

        rev.put("a", 1);
        rev.put("b", 2);
        rev.put("c", 3);

        Iterator<String> it = rev.keySet().iterator();
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
    }

    @Test
    public void testInsertionOrder()
    {
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
    }

    @Test
    public void testCaseInsensitiveSorted()
    {
        CompactMap<String, String> ci = CompactMap.<String, String>builder()
                .caseSensitive(false)
                .sortedOrder()
                .compactSize(10)
                .build();

        ci.put("a", "lower");
        ci.put("B", "upper");
        // "A" should be considered equal to "a"
        assertTrue(ci.containsKey("A"));
        assertEquals("lower", ci.get("A"));

        // iteration must be case‑insensitive sorted
        Iterator<String> it = ci.keySet().iterator();
        assertEquals("a", it.next()); // "a" and "A" are the same key, stored as first inserted
        assertEquals("B", it.next());
    }

    /* --------------------------------------------------------------
     *  withConfig – create a new map with altered configuration
     * -------------------------------------------------------------- */
    @Test
    public void testWithConfigCreatesNewMap()
    {
        map.put("x", "1");
        map.put("y", "2");

        Map<String, Object> cfg = new HashMap<>(map.getConfig());
        cfg.put(CompactMap.ORDERING, CompactMap.SORTED); // change ordering

        CompactMap<String, String> changed = map.withConfig(cfg);
        assertNotSame(map, changed);
        assertEquals(2, changed.size());

        // Verify new map respects sorted order
        Iterator<String> it = changed.keySet().iterator();
        assertEquals("x", it.next());
        assertEquals("y", it.next());
    }

    /* --------------------------------------------------------------
     *  Builder validation – illegal configurations
     * -------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderInvalidCompactSize()
    {
        CompactMap.<String, String>builder()
                .compactSize(1) // must be >= 2
                .build();
    }

    /* --------------------------------------------------------------
     *  Unsupported operations (minus / plus)
     * -------------------------------------------------------------- */
    @Test(expected = UnsupportedOperationException.class)
    public void testMinusUnsupported()
    {
        map.minus("any");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlusUnsupported()
    {
        map.plus("any");
    }

    /* --------------------------------------------------------------
     *  Null handling – keys and values
     * -------------------------------------------------------------- */
    @Test
    public void testNullKeyAndValue()
    {
        // Null key is allowed (HashMap permits it)
        map.put(null, "nullKey");
        assertTrue(map.containsKey(null));
        assertEquals("nullKey", map.get(null));

        // Null value
        map.put("nullValue", null);
        assertTrue(map.containsKey("nullValue"));
        assertNull(map.get("nullValue"));
        assertTrue(map.containsValue(null));
    }

    /* --------------------------------------------------------------
     *  clear() resets internal state
     * -------------------------------------------------------------- */
    @Test
    public void testClearResetsState()
    {
        map.put("a", "1");
        map.put("b", "2");
        assertFalse(map.isEmpty());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /* --------------------------------------------------------------
     *  getConfig returns an immutable map with expected entries
     * -------------------------------------------------------------- */
    @Test
    public void testGetConfig()
    {
        Map<String, Object> cfg = map.getConfig();
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(!CompactMap.DEFAULT_CASE_SENSITIVE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.UNORDERED, cfg.get(CompactMap.ORDERING));
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(CompactMap.DEFAULT_MAP_TYPE, cfg.get(CompactMap.MAP_TYPE));

        // The map must be unmodifiable
        try
        {
            cfg.put("newKey", "value");
            fail("Expected UnsupportedOperationException");
        }
        catch (UnsupportedOperationException ignored) {}
    }

    /* --------------------------------------------------------------
     *  verify internal state reporting (LogicalValueType)
     * -------------------------------------------------------------- */
    @Test
    public void testLogicalValueTypeTransitions()
    {
        // EMPTY
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());

        // OBJECT (single entry with default single key)
        map.put("id", "single");
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // ENTRY (single entry with other key)
        map.clear();
        map.put("other", "value");
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        // ARRAY (two entries)
        map.put("second", "v2");
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // MAP (exceed compact size)
        CompactMap<String, String> big = CompactMap.<String, String>builder()
                .compactSize(2) // after 3 entries we must be in MAP state
                .build();
        big.put("a", "1");
        big.put("b", "2");
        big.put("c", "3");
        assertEquals(CompactMap.LogicalValueType.MAP, big.getLogicalValueType());
    }
}
