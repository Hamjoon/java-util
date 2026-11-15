package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link CompactCIHashMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors.</li>
 *   <li>All public Map operations inherited from {@link CompactMap}.</li>
 *   <li>Case‑insensitive behavior (keys differing only by case are treated as the same).</li>
 *   <li>Edge cases such as {@code null} keys, empty maps, and the {@code minus}/{@code plus} helpers.</li>
 * </ul>
 */
public class CompactCIHashMapTest
{
    private CompactCIHashMap<String, String> map;

    @Before
    public void setUp()
    {
        map = new CompactCIHashMap<>();
    }

    /* ----------------------------------------------------------------------
     *  Constructor tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructor()
    {
        assertTrue("New map should be empty", map.isEmpty());
        assertEquals("Size of new map should be 0", 0, map.size());
    }

    @Test
    public void testConstructorFromExistingMap()
    {
        Map<String, String> source = new HashMap<>();
        source.put("One", "1");
        source.put("Two", "2");
        CompactCIHashMap<String, String> fromMap = new CompactCIHashMap<>(source);

        // original map unchanged
        assertEquals(2, fromMap.size());
        assertEquals("1", fromMap.get("ONE"));   // case‑insensitive lookup
        assertEquals("2", fromMap.get("two"));   // case‑insensitive lookup
    }

    /* ----------------------------------------------------------------------
     *  Basic put / get / containsKey / remove – case‑insensitive behavior
     * ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetCaseInsensitive()
    {
        map.put("Key", "value");
        assertEquals("value", map.get("key"));
        assertEquals("value", map.get("KEY"));
        assertTrue(map.containsKey("kEy"));
        assertEquals(1, map.size());
    }

    @Test
    public void testPutOverridesExistingKeyIgnoringCase()
    {
        map.put("Key", "first");
        map.put("kEy", "second"); // should replace the previous entry
        assertEquals(1, map.size());
        assertEquals("second", map.get("KEY"));
    }

    @Test
    public void testRemoveCaseInsensitive()
    {
        map.put("Alpha", "A");
        map.put("Beta", "B");
        assertEquals("A", map.remove("ALPHA"));
        assertFalse(map.containsKey("alpha"));
        assertEquals(1, map.size());
        assertNull(map.remove("gamma")); // removing non‑existent key returns null
    }

    /* ----------------------------------------------------------------------
     *  Edge case: null key handling
     * ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testPutNullKeyThrows()
    {
        map.put(null, "nullValue");
    }

    @Test(expected = NullPointerException.class)
    public void testGetNullKeyThrows()
    {
        map.get(null);
    }

    @Test(expected = NullPointerException.class)
    public void testContainsKeyNullThrows()
    {
        map.containsKey(null);
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveNullKeyThrows()
    {
        map.remove(null);
    }

    /* ----------------------------------------------------------------------
     *  putAll – case‑insensitive merging
     * ---------------------------------------------------------------------- */

    @Test
    public void testPutAllCaseInsensitive()
    {
        Map<String, String> other = new HashMap<>();
        other.put("One", "1");
        other.put("TWO", "2");
        other.put("three", "3");

        map.putAll(other);
        assertEquals(3, map.size());

        // keys should be accessible regardless of case
        assertEquals("1", map.get("one"));
        assertEquals("2", map.get("two"));
        assertEquals("3", map.get("THREE"));
    }

    @Test
    public void testPutAllOverridesExistingKeyIgnoringCase()
    {
        map.put("Key", "old");
        Map<String, String> other = new HashMap<>();
        other.put("kEy", "new");
        map.putAll(other);
        assertEquals(1, map.size());
        assertEquals("new", map.get("KEY"));
    }

    /* ----------------------------------------------------------------------
     *  minus / plus – functional helpers that return new maps
     * ---------------------------------------------------------------------- */

    @Test
    public void testMinusReturnsNewMapWithoutKey()
    {
        map.put("A", "1");
        map.put("B", "2");
        map.put("C", "3");

        Map<String, String> result = map.minus("b"); // case‑insensitive removal
        assertEquals(2, result.size());
        assertFalse(result.containsKey("B"));
        assertTrue(result.containsKey("A"));
        assertTrue(result.containsKey("C"));

        // original map must stay unchanged
        assertEquals(3, map.size());
        assertTrue(map.containsKey("B"));
    }

    @Test
    public void testPlusReturnsNewMapWithAddedEntry()
    {
        map.put("X", "x");
        Map<String, String> result = map.plus("y"); // adds entry with key "y" and null value
        assertEquals(2, result.size());
        assertTrue(result.containsKey("X"));
        assertTrue(result.containsKey("Y"));
        assertNull(result.get("Y"));

        // original map unchanged
        assertEquals(1, map.size());
        assertFalse(map.containsKey("Y"));
    }

    /* ----------------------------------------------------------------------
     *  equals / hashCode – case‑insensitive equality
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsIgnoresKeyCase()
    {
        CompactCIHashMap<String, String> other = new CompactCIHashMap<>();
        map.put("KeyOne", "1");
        map.put("KeyTwo", "2");

        other.put("keyone", "1");
        other.put("KEYTWO", "2");

        assertTrue("Maps with same entries (ignoring case) should be equal", map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());
    }

    @Test
    public void testEqualsDifferentValues()
    {
        map.put("SameKey", "A");
        CompactCIHashMap<String, String> other = new CompactCIHashMap<>();
        other.put("samekey", "B");
        assertFalse(map.equals(other));
    }

    /* ----------------------------------------------------------------------
     *  keySet, values, entrySet – reflect case‑insensitive storage
     * ---------------------------------------------------------------------- */

    @Test
    public void testKeySetPreservesOriginalCase()
    {
        map.put("First", "1");
        map.put("SECOND", "2");
        Set<String> keys = map.keySet();

        // The keySet should contain the keys as they were originally inserted
        assertTrue(keys.contains("First"));
        assertTrue(keys.contains("SECOND"));
        assertEquals(2, keys.size());
    }

    @Test
    public void testValuesCollection()
    {
        map.put("One", "1");
        map.put("Two", "2");
        Collection<String> values = map.values();
        assertTrue(values.containsAll(Arrays.asList("1", "2")));
        assertEquals(2, values.size());
    }

    @Test
    public void testEntrySetIteration()
    {
        map.put("Alpha", "A");
        map.put("beta", "B");
        Set<Map.Entry<String, String>> entries = map.entrySet();

        Map<String, String> collected = new HashMap<>();
        for (Map.Entry<String, String> e : entries)
        {
            collected.put(e.getKey(), e.getValue());
        }

        assertEquals(2, collected.size());
        assertEquals("A", collected.get("Alpha"));
        assertEquals("B", collected.get("beta"));
    }

    /* ----------------------------------------------------------------------
     *  toString – basic sanity check (structure, not exact format)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToStringContainsEntries()
    {
        map.put("Key", "Value");
        String s = map.toString();
        assertTrue("toString should contain key", s.contains("Key"));
        assertTrue("toString should contain value", s.contains("Value"));
    }
}
