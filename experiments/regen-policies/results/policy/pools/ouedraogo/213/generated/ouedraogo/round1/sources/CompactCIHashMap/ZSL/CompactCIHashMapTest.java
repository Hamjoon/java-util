package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCIHashMap}.
 * <p>
 * The tests focus on the public API that {@code CompactCIHashMap}
 * inherits from {@code CompactMap} and on the case‑insensitive behaviour
 * that the class adds by overriding {@code getNewMap()} and {@code isCaseInsensitive()}.
 * </p>
 */
public class CompactCIHashMapTest
{
    private CompactCIHashMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactCIHashMap<>();
    }

    /* --------------------------------------------------------------------- *
     *  Construction & basic Map contract
     * --------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap()
    {
        assertTrue("Map should be empty after default construction", map.isEmpty());
        assertEquals("Size should be zero", 0, map.size());
    }

    @Test
    public void testCopyConstructorCopiesAllEntries()
    {
        Map<String, Integer> source = new HashMap<>();
        source.put("One", 1);
        source.put("Two", 2);
        source.put("THREE", 3);

        CompactCIHashMap<String, Integer> copy = new CompactCIHashMap<>(source);

        // the copy must contain all entries (case‑insensitively)
        assertEquals(3, copy.size());
        assertEquals(Integer.valueOf(1), copy.get("one"));
        assertEquals(Integer.valueOf(2), copy.get("TWO"));
        assertEquals(Integer.valueOf(3), copy.get("three"));
    }

    /* --------------------------------------------------------------------- *
     *  Case‑insensitive key handling
     * --------------------------------------------------------------------- */

    @Test
    public void testPutAndGetAreCaseInsensitive()
    {
        map.put("Foo", 10);
        assertEquals("Retrieving with different case should succeed", Integer.valueOf(10), map.get("foo"));
        assertEquals("Retrieving with upper case should succeed", Integer.valueOf(10), map.get("FOO"));
    }

    @Test
    public void testContainsKeyIsCaseInsensitive()
    {
        map.put("Bar", 20);
        assertTrue(map.containsKey("bar"));
        assertTrue(map.containsKey("BAR"));
        assertTrue(map.containsKey("BaR"));
    }

    @Test
    public void testRemoveIsCaseInsensitive()
    {
        map.put("Key", 99);
        assertEquals(Integer.valueOf(99), map.remove("kEy"));
        assertFalse("Key should no longer be present after removal", map.containsKey("KEY"));
        assertTrue("Map should be empty after removal", map.isEmpty());
    }

    @Test
    public void testPutAllPreservesCaseInsensitivity()
    {
        Map<String, Integer> other = new HashMap<>();
        other.put("Alpha", 1);
        other.put("Beta", 2);
        map.putAll(other);

        // original map must contain the entries, case‑insensitively
        assertEquals(Integer.valueOf(1), map.get("ALPHA"));
        assertEquals(Integer.valueOf(2), map.get("beta"));
        assertTrue(map.containsKey("aLpHa"));
    }

    @Test
    public void testKeySetValuesAndEntrySetAreCaseInsensitive()
    {
        map.put("Xyz", 5);
        map.put("abc", 7);

        // keySet
        Set<String> keys = map.keySet();
        assertTrue(keys.contains("XYZ"));
        assertTrue(keys.contains("AbC"));
        assertEquals(2, keys.size());

        // values
        Collection<Integer> values = map.values();
        assertTrue(values.contains(5));
        assertTrue(values.contains(7));
        assertEquals(2, values.size());

        // entrySet
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        boolean foundXyz = false, foundAbc = false;
        for (Map.Entry<String, Integer> e : entries)
        {
            if (e.getKey().equalsIgnoreCase("xyz"))
                foundXyz = e.getValue().equals(5);
            if (e.getKey().equalsIgnoreCase("abc"))
                foundAbc = e.getValue().equals(7);
        }
        assertTrue("Entry for 'xyz' should be present", foundXyz);
        assertTrue("Entry for 'abc' should be present", foundAbc);
    }

    /* --------------------------------------------------------------------- *
     *  Equality, hashCode and toString
     * --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeRespectCaseInsensitivity()
    {
        CompactCIHashMap<String, Integer> a = new CompactCIHashMap<>();
        CompactCIHashMap<String, Integer> b = new CompactCIHashMap<>();

        a.put("One", 1);
        a.put("Two", 2);

        b.put("ONE", 1);
        b.put("two", 2);

        assertEquals("Maps with same logical entries should be equal", a, b);
        assertEquals("Hash codes must be identical for equal maps", a.hashCode(), b.hashCode());
    }

    @Test
    public void testToStringContainsEntries()
    {
        map.put("First", 100);
        map.put("Second", 200);
        String s = map.toString();

        assertNotNull("toString must not return null", s);
        assertTrue("String representation should contain key 'First'", s.contains("First"));
        assertTrue("String representation should contain key 'Second'", s.contains("Second"));
        assertTrue("String representation should contain value 100", s.contains("100"));
        assertTrue("String representation should contain value 200", s.contains("200"));
    }

    /* --------------------------------------------------------------------- *
     *  minus() and plus() – functional, non‑mutating operations
     * --------------------------------------------------------------------- */

    @Test
    public void testMinusRemovesSpecifiedKeyWithoutMutatingOriginal()
    {
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);

        Map<String, Integer> result = map.minus("b"); // case‑insensitive removal

        // original map unchanged
        assertEquals(3, map.size());
        assertTrue(map.containsKey("B"));

        // result map must not contain the removed key
        assertEquals(2, result.size());
        assertFalse(result.containsKey("b"));
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("c"));
    }

    @Test
    public void testPlusAddsEntriesFromAnotherMapWithoutMutatingOriginal()
    {
        map.put("X", 10);
        map.put("Y", 20);

        Map<String, Integer> extra = new HashMap<>();
        extra.put("Z", 30);
        extra.put("y", 200); // should overwrite existing entry (case‑insensitively)

        Map<String, Integer> result = map.plus(extra);

        // original map unchanged
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(20), map.get("Y"));

        // result map contains merged data
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(10), result.get("x"));
        assertEquals(Integer.valueOf(200), result.get("Y")); // overwritten
        assertEquals(Integer.valueOf(30), result.get("z"));
    }

    /* --------------------------------------------------------------------- *
     *  Configuration and logical value type (lightweight sanity checks)
     * --------------------------------------------------------------------- */

    @Test
    public void testGetConfigContainsCaseSensitiveFlag()
    {
        Map<String, Object> cfg = map.getConfig();
        assertNotNull("Configuration map must not be null", cfg);
        // The key for the flag is defined in CompactMap; we verify its presence.
        assertTrue("Configuration should contain CASE_SENSITIVE flag",
                cfg.containsKey(CompactMap.CASE_SENSITIVE));
        assertEquals("CASE_SENSITIVE flag must be false for CI map",
                Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
    }

    @Test
    public void testWithConfigPreservesCaseInsensitiveBehaviour()
    {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(CompactMap.CASE_SENSITIVE, Boolean.FALSE);
        CompactCIHashMap<String, Integer> configured = map.withConfig(cfg);

        // The returned instance should still be case‑insensitive.
        configured.put("Mixed", 123);
        assertEquals(Integer.valueOf(123), configured.get("mixed"));
    }

    @Test
    public void testLogicalValueTypeIsNotNull()
    {
        // The exact enum values are not important for the test – we only verify that
        // the method can be called and does not return null.
        assertNotNull("LogicalValueType must be non‑null", map.getLogicalValueType());
    }
}
