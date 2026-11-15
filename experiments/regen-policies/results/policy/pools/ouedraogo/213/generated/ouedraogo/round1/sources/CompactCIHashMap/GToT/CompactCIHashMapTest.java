package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * JUnit 4 test suite for {@link CompactCIHashMap}.
 *
 * This test class covers:
 * <ul>
 *   <li>All public constructors.</li>
 *   <li>All inherited public methods from {@link CompactMap} that are relevant to the
 *       case‑insensitive behaviour.</li>
 *   <li>Typical usage scenarios, edge cases (null keys/values, duplicate keys with
 *       different case, empty maps) and expected exception handling.</li>
 * </ul>
 */
public class CompactCIHashMapTest
{
    private CompactCIHashMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactCIHashMap<>();
    }

    /* ---------------------------------------------------------------------- */
    /*  Constructor tests                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructor()
    {
        CompactCIHashMap<String, String> empty = new CompactCIHashMap<>();
        assertTrue("New map must be empty", empty.isEmpty());
        assertEquals("Size must be 0", 0, empty.size());
    }

    @Test
    public void testConstructorFromMap()
    {
        Map<String, Integer> source = new HashMap<>();
        source.put("Alpha", 1);
        source.put("beta", 2);
        CompactCIHashMap<String, Integer> fromMap = new CompactCIHashMap<>(source);

        assertEquals("Map must contain 2 entries", 2, fromMap.size());
        assertEquals(Integer.valueOf(1), fromMap.get("ALPHA"));
        assertEquals(Integer.valueOf(2), fromMap.get("BETA"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorFromNullMap()
    {
        new CompactCIHashMap<String, Integer>(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic Map operations (case‑insensitive behaviour)                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetCaseInsensitive()
    {
        map.put("KeyOne", 10);
        assertEquals(Integer.valueOf(10), map.get("keyone"));
        assertTrue("containsKey must be case‑insensitive", map.containsKey("KEYONE"));
    }

    @Test
    public void testPutOverwriteWithDifferentCase()
    {
        map.put("Key", 1);
        Integer previous = map.put("kEy", 2);
        assertEquals(Integer.valueOf(1), previous);
        assertEquals(Integer.valueOf(2), map.get("KEY"));
        assertEquals("Size must remain 1 after overwrite", 1, map.size());
    }

    @Test
    public void testRemoveCaseInsensitive()
    {
        map.put("RemoveMe", 99);
        Integer removed = map.remove("removeme");
        assertEquals(Integer.valueOf(99), removed);
        assertFalse(map.containsKey("REMOVEME"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testContainsKeyAndContainsValue()
    {
        map.put("Exists", 5);
        assertTrue(map.containsKey("exists"));
        assertTrue(map.containsValue(5));
        assertFalse(map.containsKey("nonexistent"));
        assertFalse(map.containsValue(0));
    }

    @Test
    public void testPutAllCaseInsensitive()
    {
        Map<String, Integer> other = new HashMap<>();
        other.put("One", 1);
        other.put("ONE", 2); // duplicate key with different case
        other.put("Two", 3);
        map.putAll(other);

        // "One"/"ONE" should be merged, last value wins
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(2), map.get("one"));
        assertEquals(Integer.valueOf(3), map.get("TWO"));
    }

    @Test
    public void testClear()
    {
        map.put("A", 1);
        map.put("B", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge cases – null keys and null values                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNullKey()
    {
        map.put(null, 42);
        assertTrue("Map must contain null key", map.containsKey(null));
        assertEquals(Integer.valueOf(42), map.get(null));
        assertEquals(Integer.valueOf(42), map.remove(null));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testNullValue()
    {
        map.put("NullValue", null);
        assertTrue(map.containsKey("nullvalue"));
        assertNull(map.get("NULLVALUE"));
        assertTrue(map.containsValue(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  minus() and plus() – functional style helpers                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMinusRemovesKeyCaseInsensitive()
    {
        map.put("Alpha", 1);
        map.put("Beta", 2);
        Map<String, Integer> result = map.minus("ALPHA");
        assertFalse(result.containsKey("Alpha"));
        assertTrue(result.containsKey("Beta"));
        assertEquals(1, result.size());
    }

    @Test
    public void testPlusAddsEntry()
    {
        map.put("X", 10);
        // plus adds a placeholder entry with null value; the exact contract of plus()
        // is to return a new map that contains all existing entries plus the supplied key.
        Map<String, Integer> result = map.plus("Y");
        assertTrue(result.containsKey("X"));
        assertTrue(result.containsKey("Y"));
        assertEquals(Integer.valueOf(10), result.get("X"));
        assertNull("plus should add the key with a null value", result.get("Y"));
        assertEquals(2, result.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  Configuration handling                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testWithConfigAndGetConfig()
    {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("testKey", "testValue");
        CompactCIHashMap<String, Integer> configured = map.withConfig(cfg);
        assertSame("withConfig must return the same map instance", map, configured);
        assertEquals("Configuration map must be stored unchanged", cfg, configured.getConfig());
    }

    /* ---------------------------------------------------------------------- */
    /*  Equality, hashCode and toString                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode()
    {
        CompactCIHashMap<String, Integer> a = new CompactCIHashMap<>();
        CompactCIHashMap<String, Integer> b = new CompactCIHashMap<>();

        a.put("One", 1);
        a.put("Two", 2);

        b.put("ONE", 1);
        b.put("two", 2);

        assertEquals("Maps with same entries (ignoring case) must be equal", a, b);
        assertEquals("Hash codes must be equal for equal maps", a.hashCode(), b.hashCode());
    }

    @Test
    public void testToStringContainsEntries()
    {
        map.put("First", 100);
        map.put("Second", 200);
        String s = map.toString();
        assertTrue("toString should contain entry key", s.contains("First"));
        assertTrue("toString should contain entry value", s.contains("100"));
        assertTrue("toString should contain second key", s.contains("Second"));
        assertTrue("toString should contain second value", s.contains("200"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Miscellaneous public API from CompactMap                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testKeySetValuesAndEntrySet()
    {
        map.put("A", 1);
        map.put("B", 2);

        Set<String> keys = map.keySet();
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));
        assertEquals(2, keys.size());

        assertTrue(map.values().contains(1));
        assertTrue(map.values().contains(2));
        assertEquals(2, map.values().size());

        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        boolean foundA = false, foundB = false;
        for (Map.Entry<String, Integer> e : entries)
        {
            if (e.getKey().equalsIgnoreCase("a") && e.getValue().equals(1)) foundA = true;
            if (e.getKey().equalsIgnoreCase("b") && e.getValue().equals(2)) foundB = true;
        }
        assertTrue("Entry set must contain entry for key A", foundA);
        assertTrue("Entry set must contain entry for key B", foundB);
    }

    @Test
    public void testIsDefaultCompactMap()
    {
        // The default CompactCIHashMap is a compact map; the method is inherited.
        assertTrue("CompactCIHashMap should report as default compact map", map.isDefaultCompactMap());
    }

    @Test
    public void testLogicalValueTypeIsNotNull()
    {
        // LogicalValueType is part of CompactMap API; we only assert that the call does not throw.
        assertNotNull("LogicalValueType must not be null", map.getLogicalValueType());
    }
}
