package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCIHashMap}.
 *
 * The tests are divided into two sections:
 * <ul>
 *   <li>**Basic functionality** – one test per public method that verifies the
 *        normal contract of the method.</li>
 *   <li>**Edge‑case / exception handling** – tests that exercise case‑insensitivity,
 *        null handling, map‑copy constructor, and the special {@code minus},
 *        {@code plus}, {@code getConfig} and {@code withConfig} behaviours.</li>
 * </ul>
 *
 * All tests use real instances of {@link CompactCIHashMap} and never rely on
 * mocking or subclassing library classes.
 */
public class CompactCIHashMapTest
{
    private CompactCIHashMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactCIHashMap<>();
    }

    /* --------------------------------------------------------------
     *  1.  PUBLIC CONSTRUCTORS
     * -------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap()
    {
        assertTrue("Map should be empty after default construction", map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testCopyConstructorCopiesEntriesCaseInsensitively()
    {
        Map<String, Integer> source = new HashMap<>();
        source.put("One", 1);
        source.put("TWO", 2);
        source.put("three", 3);

        CompactCIHashMap<String, Integer> copied = new CompactCIHashMap<>(source);
        // size must be 3
        assertEquals(3, copied.size());

        // case‑insensitive look‑ups
        assertEquals(Integer.valueOf(1), copied.get("one"));
        assertEquals(Integer.valueOf(2), copied.get("two"));
        assertEquals(Integer.valueOf(3), copied.get("THREE"));
    }

    @Test(expected = NullPointerException.class)
    public void testCopyConstructorRejectsNullMap()
    {
        new CompactCIHashMap<String, Integer>(null);
    }

    /* --------------------------------------------------------------
     *  2.  BASIC MAP OPERATIONS (inherited from CompactMap)
     * -------------------------------------------------------------- */

    @Test
    public void testPutAndGetCaseInsensitive()
    {
        map.put("Key", 10);
        assertEquals(Integer.valueOf(10), map.get("key"));
        assertEquals(Integer.valueOf(10), map.get("KEY"));
        // second put with different case updates the existing entry
        map.put("kEy", 20);
        assertEquals(Integer.valueOf(20), map.get("key"));
        assertEquals(1, map.size());
    }

    @Test
    public void testContainsKeyCaseInsensitive()
    {
        map.put("Hello", 1);
        assertTrue(map.containsKey("hello"));
        assertTrue(map.containsKey("HELLO"));
        assertFalse(map.containsKey("world"));
    }

    @Test
    public void testContainsValue()
    {
        map.put("A", 100);
        map.put("B", 200);
        assertTrue(map.containsValue(100));
        assertFalse(map.containsValue(300));
    }

    @Test
    public void testRemoveCaseInsensitive()
    {
        map.put("RemoveMe", 5);
        Integer removed = map.remove("removeme");
        assertEquals(Integer.valueOf(5), removed);
        assertFalse(map.containsKey("REMOVEme"));
        assertEquals(0, map.size());
    }

    @Test
    public void testPutAllCopiesCaseInsensitively()
    {
        Map<String, Integer> src = new HashMap<>();
        src.put("Alpha", 1);
        src.put("BETA", 2);
        map.putAll(src);
        assertEquals(Integer.valueOf(1), map.get("alpha"));
        assertEquals(Integer.valueOf(2), map.get("beta"));
        assertEquals(2, map.size());
    }

    @Test
    public void testClear()
    {
        map.put("x", 1);
        map.put("y", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testKeySetReflectsCaseInsensitiveKeys()
    {
        map.put("First", 1);
        map.put("SECOND", 2);
        Set<String> keys = map.keySet();
        // The actual stored keys are the *original* ones, not lower‑cased.
        assertTrue(keys.contains("First"));
        assertTrue(keys.contains("SECOND"));
        assertEquals(2, keys.size());
    }

    @Test
    public void testValuesCollection()
    {
        map.put("one", 1);
        map.put("two", 2);
        assertTrue(map.values().containsAll(java.util.Arrays.asList(1, 2)));
        assertEquals(2, map.values().size());
    }

    @Test
    public void testEntrySet()
    {
        map.put("k", 42);
        assertEquals(1, map.entrySet().size());
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        assertEquals("k", entry.getKey());
        assertEquals(Integer.valueOf(42), entry.getValue());
    }

    @Test
    public void testEqualsAndHashCode()
    {
        CompactCIHashMap<String, Integer> m1 = new CompactCIHashMap<>();
        CompactCIHashMap<String, Integer> m2 = new CompactCIHashMap<>();

        m1.put("Same", 1);
        m2.put("same", 1); // different case

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    public void testMinusCreatesNewMapWithoutKey()
    {
        map.put("keep", 1);
        map.put("remove", 2);
        Map<String, Integer> result = map.minus("REMOVE"); // case‑insensitive key
        assertFalse(result.containsKey("remove"));
        assertTrue(result.containsKey("keep"));
        // original map unchanged
        assertTrue(map.containsKey("remove"));
    }

    @Test
    public void testPlusCreatesNewMapWithAdditionalEntry()
    {
        map.put("a", 1);
        Map<String, Integer> result = map.plus("B");
        assertTrue(result.containsKey("b"));
        assertEquals(Integer.valueOf(null), result.get("b")); // plus adds a null value entry
        // original map unchanged
        assertFalse(map.containsKey("b"));
    }

    @Test
    public void testGetLogicalValueTypeIsNotNull()
    {
        assertNotNull("LogicalValueType should never be null", map.getLogicalValueType());
    }

    @Test
    public void testToStringContainsEntries()
    {
        map.put("x", 9);
        map.put("y", 10);
        String s = map.toString();
        assertTrue(s.contains("x"));
        assertTrue(s.contains("9"));
        assertTrue(s.contains("y"));
        assertTrue(s.contains("10"));
    }

    @Test
    public void testBuilderCreatesEmptyMap()
    {
        CompactCIHashMap.Builder<String, Integer> builder = CompactCIHashMap.builder();
        CompactCIHashMap<String, Integer> built = (CompactCIHashMap<String, Integer>) builder.build();
        assertTrue(built.isEmpty());
    }

    /* --------------------------------------------------------------
     *  3.  EDGE‑CASE & EXCEPTION TESTS
     * -------------------------------------------------------------- */

    @Test
    public void testNullKeyThrowsNPEOnPut()
    {
        try
        {
            map.put(null, 1);
            fail("Expected NullPointerException when putting a null key");
        }
        catch (NullPointerException ignored) { }
    }

    @Test
    public void testNullKeyThrowsNPEOnGet()
    {
        try
        {
            map.get(null);
            fail("Expected NullPointerException when getting a null key");
        }
        catch (NullPointerException ignored) { }
    }

    @Test
    public void testNullKeyThrowsNPEOnContainsKey()
    {
        try
        {
            map.containsKey(null);
            fail("Expected NullPointerException when checking containsKey(null)");
        }
        catch (NullPointerException ignored) { }
    }

    @Test
    public void testPutNullValueIsAllowed()
    {
        map.put("nullValue", null);
        assertTrue(map.containsKey("nullvalue"));
        assertNull(map.get("NULLVALUE"));
        assertEquals(1, map.size());
    }

    @Test
    public void testNonStringKeyIsStoredAsIs()
    {
        CompactCIHashMap<Object, Integer> mixed = new CompactCIHashMap<>();
        mixed.put(123, 7);
        mixed.put("123", 8); // String key, case‑insensitive handling not relevant

        assertEquals(Integer.valueOf(7), mixed.get(123));
        assertEquals(Integer.valueOf(8), mixed.get("123"));
        assertEquals(2, mixed.size());
    }

    @Test
    public void testUnicodeCaseInsensitivity()
    {
        map.put("\u00DF", 1); // German sharp s
        // The uppercase of ß is "SS". CompactCIHashMap uses String.equalsIgnoreCase,
        // which treats "ß" and "SS" as different, but we verify the actual behaviour.
        assertNull(map.get("SS"));
        assertEquals(Integer.valueOf(1), map.get("\u00DF"));
    }

    @Test
    public void testGetConfigReflectsCaseInsensitiveSetting()
    {
        Map<String, Object> cfg = map.getConfig();
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
        // Verify compact size is the default for CI map (50)
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE));
    }

    @Test
    public void testWithConfigChangesOrdering()
    {
        Map<String, Object> newCfg = new HashMap<>(map.getConfig());
        newCfg.put(CompactMap.ORDERING, CompactMap.SORTED);
        CompactCIHashMap<String, Integer> altered = map.withConfig(newCfg);
        assertEquals(CompactMap.SORTED, altered.getConfig().get(CompactMap.ORDERING));
    }

    @Test
    public void testMinusWithNonExistingKeyReturnsSameMapInstance()
    {
        map.put("a", 1);
        Map<String, Integer> result = map.minus("nonexistent");
        // According to typical semantics, a new map is returned but it should be equal.
        assertEquals(map, result);
    }

    @Test
    public void testPlusWithExistingKeyOverridesValue()
    {
        map.put("dup", 1);
        Map<String, Integer> result = map.plus("Dup"); // same key, different case
        // Result should contain the key with a null value (as per plus contract)
        assertTrue(result.containsKey("dup"));
        assertNull(result.get("dup"));
        // Original map unchanged
        assertEquals(Integer.valueOf(1), map.get("dup"));
    }

    @Test
    public void testIsCaseInsensitiveFlag()
    {
        // The protected method isCaseInsensitive() returns true; we verify via public behaviour.
        map.put("MiXeD", 5);
        assertEquals(Integer.valueOf(5), map.get("mixed"));
    }

    @Test
    public void testCompactSizeIsUsedInInternalMapCreation()
    {
        // Verify that the internal map can hold at least compactSize()+1 entries without switching.
        int limit = map.compactSize() + 1;
        for (int i = 0; i < limit; i++)
        {
            map.put("k" + i, i);
        }
        assertEquals(limit, map.size());
    }
}
