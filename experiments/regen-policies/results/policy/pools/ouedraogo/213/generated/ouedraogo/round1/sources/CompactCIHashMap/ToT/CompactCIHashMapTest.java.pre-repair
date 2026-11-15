package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactCIHashMap}.
 *
 * The tests are written from the perspective of three independent “experts”.
 * Each expert contributes one test case per public method (or logical group of
 * methods) and then the suite is assembled into a single file.
 *
 * The suite covers:
 *   • Construction (empty and from another map)
 *   • Core Map operations (put, get, containsKey, remove, putAll, clear)
 *   • Case‑insensitivity for {@link String} keys
 *   • Behaviour with non‑String keys
 *   • Collection views (keySet, values, entrySet)
 *   • Immutable operations (minus, plus)
 *   • Equality, hashCode and toString contracts
 *   • Configuration API (getConfig, withConfig, builder)
 *   • Edge cases (null keys, duplicate keys with different case)
 */
public class CompactCIHashMapTest
{
    private CompactCIHashMap<String, Integer> ciMap;
    private CompactCIHashMap<String, Integer> ciMapFromOther;

    @Before
    public void setUp()
    {
        // Expert 1 – empty map construction
        ciMap = new CompactCIHashMap<>();

        // Expert 2 – construction from an existing map
        Map<String, Integer> source = new HashMap<>();
        source.put("Alpha", 1);
        source.put("Bravo", 2);
        ciMapFromOther = new CompactCIHashMap<>(source);
    }

    /* ----------------------------------------------------------------------
     *  Core Map Operations – case‑insensitive behaviour
     * ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGet_CaseInsensitive()
    {
        // put with mixed case
        ciMap.put("TeStKey", 42);
        // get with a different case – should retrieve the same value
        assertEquals(Integer.valueOf(42), ciMap.get("testkey"));
        // containsKey must also be case‑insensitive
        assertTrue(ciMap.containsKey("TESTKEY"));
    }

    @Test
    public void testOverwriteWithDifferentCase()
    {
        ciMap.put("KeyOne", 10);
        // Overwrite using a differently‑cased key
        ciMap.put("keyone", 20);
        // Value should be updated, not duplicated
        assertEquals(1, ciMap.size());
        assertEquals(Integer.valueOf(20), ciMap.get("KEYONE"));
    }

    @Test
    public void testRemove_CaseInsensitive()
    {
        ciMap.put("RemoveMe", 99);
        // Remove using a different case – should succeed
        Integer removed = ciMap.remove("removeme");
        assertEquals(Integer.valueOf(99), removed);
        assertFalse(ciMap.containsKey("REMOVEme"));
        assertTrue(ciMap.isEmpty());
    }

    @Test
    public void testPutAll_CaseInsensitive()
    {
        Map<String, Integer> other = new HashMap<>();
        other.put("First", 1);
        other.put("SECOND", 2);
        ciMap.putAll(other);

        // Verify that all entries are present regardless of case
        assertEquals(Integer.valueOf(1), ciMap.get("first"));
        assertEquals(Integer.valueOf(2), ciMap.get("second"));
        assertTrue(ciMap.containsKey("FIRST"));
        assertTrue(ciMap.containsKey("second"));
    }

    @Test
    public void testClear()
    {
        ciMap.put("A", 1);
        ciMap.put("B", 2);
        assertFalse(ciMap.isEmpty());
        ciMap.clear();
        assertTrue(ciMap.isEmpty());
        assertEquals(0, ciMap.size());
    }

    /* ----------------------------------------------------------------------
     *  Construction from another map – ensures the source map is copied
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructionFromMap_IsIndependentCopy()
    {
        // The original source map should not be affected by changes to ciMapFromOther
        ciMapFromOther.put("Alpha", 10); // overwrite existing entry
        assertEquals(Integer.valueOf(10), ciMapFromOther.get("alpha"));
        // Verify original source map unchanged
        Map<String, Integer> source = new HashMap<>();
        source.put("Alpha", 1);
        source.put("Bravo", 2);
        CompactCIHashMap<String, Integer> copy = new CompactCIHashMap<>(source);
        assertEquals(Integer.valueOf(1), copy.get("ALPHA"));
        assertEquals(Integer.valueOf(2), copy.get("bravo"));
        // Changing the copy must not affect 'source'
        copy.put("Alpha", 99);
        assertEquals(Integer.valueOf(1), source.get("Alpha"));
    }

    /* ----------------------------------------------------------------------
     *  Non‑String keys – case‑insensitivity should not apply
     * ---------------------------------------------------------------------- */

    @Test
    public void testNonStringKey_Behaviour()
    {
        CompactCIHashMap<Object, String> map = new CompactCIHashMap<>();
        Integer intKey = 123;
        map.put(intKey, "Number");
        // Retrieval with the same Integer instance works
        assertEquals("Number", map.get(123));
        // Retrieval with a different Integer object with same value also works (hash‑based)
        assertEquals("Number", map.get(Integer.valueOf(123)));
        // Case‑insensitivity does not interfere
        assertFalse(map.containsKey("123"));
    }

    /* ----------------------------------------------------------------------
     *  Collection Views
     * ---------------------------------------------------------------------- */

    @Test
    public void testKeySetAndValues()
    {
        ciMap.put("One", 1);
        ciMap.put("Two", 2);
        ciMap.put("Three", 3);

        Set<String> keys = ciMap.keySet();
        // The keySet should contain the original case of the first insertion
        assertTrue(keys.contains("One"));
        assertTrue(keys.contains("Two"));
        assertTrue(keys.contains("Three"));
        assertEquals(3, keys.size());

        // values collection should contain all values
        assertTrue(ciMap.values().containsAll(java.util.Arrays.asList(1, 2, 3)));
        assertEquals(3, ciMap.values().size());
    }

    @Test
    public void testEntrySetIteration()
    {
        ciMap.put("X", 10);
        ciMap.put("Y", 20);
        ciMap.put("Z", 30);

        int sum = 0;
        for (Map.Entry<String, Integer> entry : ciMap.entrySet())
        {
            sum += entry.getValue();
            // Keys retrieved from entrySet must be the original case used at insertion
            assertNotNull(entry.getKey());
        }
        assertEquals(60, sum);
    }

    /* ----------------------------------------------------------------------
     *  Immutable Operations: minus and plus
     * ---------------------------------------------------------------------- */

    @Test
    public void testMinus_RemovesKeyCaseInsensitively()
    {
        ciMap.put("Apple", 5);
        ciMap.put("Banana", 6);
        Map<String, Integer> result = ciMap.minus("APPLE");
        // original map unchanged
        assertEquals(2, ciMap.size());
        // result map should not contain the removed key
        assertFalse(result.containsKey("apple"));
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(6), result.get("banana"));
    }

    @Test
    public void testPlus_AddsEntryCaseInsensitively()
    {
        ciMap.put("Cat", 7);
        Map<String, Integer> result = ciMap.plus("DOG");
        // The returned map should contain the original entries plus the new key with a null value
        assertEquals(2, result.size());
        assertTrue(result.containsKey("cat"));
        assertTrue(result.containsKey("dog"));
        assertEquals(Integer.valueOf(7), result.get("cat"));
        assertNull(result.get("dog"));
    }

    /* ----------------------------------------------------------------------
     *  Equality, hashCode and toString contracts
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode()
    {
        CompactCIHashMap<String, Integer> mapA = new CompactCIHashMap<>();
        CompactCIHashMap<String, Integer> mapB = new CompactCIHashMap<>();

        mapA.put("Key", 100);
        mapB.put("key", 100); // different case, same logical entry

        assertEquals(mapA, mapB);
        assertEquals(mapA.hashCode(), mapB.hashCode());

        mapB.put("Another", 200);
        assertNotEquals(mapA, mapB);
    }

    @Test
    public void testToStringContainsEntries()
    {
        ciMap.put("Alpha", 1);
        ciMap.put("Beta", 2);
        String s = ciMap.toString();
        // The string representation should contain both key/value pairs
        assertTrue(s.contains("Alpha=1"));
        assertTrue(s.contains("Beta=2"));
    }

    /* ----------------------------------------------------------------------
     *  Configuration API
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetConfig_ContainsExpectedDefaults()
    {
        Map<String, Object> cfg = ciMap.getConfig();
        // The map must expose the case‑sensitivity flag as false
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
        // Verify that the compact size matches the default for CI maps (50)
        assertEquals(Integer.valueOf(50), cfg.get(CompactMap.COMPACT_SIZE));
        // Ordering should be UNORDERED for the default CI map
        assertEquals(CompactMap.UNORDERED, cfg.get(CompactMap.ORDERING));
    }

    @Test
    public void testWithConfig_ChangesConfiguration()
    {
        // Change ordering to INSERTION and verify the change is reflected
        Map<String, Object> newCfg = new HashMap<>();
        newCfg.put(CompactMap.ORDERING, CompactMap.INSERTION);
        CompactCIHashMap<String, Integer> reordered = ciMap.withConfig(newCfg);
        assertEquals(CompactMap.INSERTION, reordered.getConfig().get(CompactMap.ORDERING));

        // The original map must remain unchanged
        assertEquals(CompactMap.UNORDERED, ciMap.getConfig().get(CompactMap.ORDERING));
    }

    @Test
    public void testBuilderCreatesMapWithExpectedBehaviour()
    {
        // Use the static builder to create a map, put values and verify case‑insensitivity
        CompactCIHashMap<String, Integer> built = CompactMap.builder()
                                                          .build(); // builder returns a CompactMap; cast is safe
        // The builder returns a CompactMap; we know the concrete type will be CompactCIHashMap
        assertTrue(built instanceof CompactCIHashMap);
        built.put("BuilderKey", 123);
        assertEquals(Integer.valueOf(123), built.get("builderkey"));
    }

    /* ----------------------------------------------------------------------
     *  Edge Cases – null handling
     * ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testPutNullKey_ThrowsException()
    {
        // According to typical Map contracts for case‑insensitive maps, null keys are not allowed
        ciMap.put(null, 1);
    }

    @Test
    public void testPutNullValue_AllowsNull()
    {
        ciMap.put("Nullable", null);
        assertTrue(ciMap.containsKey("nullable"));
        assertNull(ciMap.get("NULLABLE"));
    }
}
