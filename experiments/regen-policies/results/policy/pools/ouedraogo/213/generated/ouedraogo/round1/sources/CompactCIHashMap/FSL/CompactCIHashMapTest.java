package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Unit tests for {@link CompactCIHashMap}. The tests focus on the
 * case‑insensitive behavior, the interaction with the underlying
 * {@link CompactMap} API and the special map operations provided by the
 * base class (minus, plus, getConfig, etc.).
 */
public class CompactCIHashMapTest {

    /* --------------------------------------------------------------------- *
     *  Basic Map contract – put / get / size / isEmpty / clear
     * --------------------------------------------------------------------- */

    @Test
    public void testPutGetBasicOperations() {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        // put a value
        map.put("KeyOne", 1);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        // get with the exact case
        assertEquals(Integer.valueOf(1), map.get("KeyOne"));

        // get with a different case – should be case‑insensitive
        assertEquals(Integer.valueOf(1), map.get("keyone"));
        assertEquals(Integer.valueOf(1), map.get("KEYONE"));
        assertTrue(map.containsKey("kEyOnE"));
    }

    @Test
    public void testOverwriteSameKeyDifferentCase() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();

        map.put("Alpha", "first");
        // Overwrite using a different case – should replace the previous entry
        map.put("aLPHa", "second");

        assertEquals(1, map.size());
        assertEquals("second", map.get("ALPHA"));
    }

    @Test
    public void testRemoveKeyCaseInsensitive() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("RemoveMe", "value");

        // remove using a different case
        String removed = map.remove("removeme");
        assertEquals("value", removed);
        assertFalse(map.containsKey("REMOVEme"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testClearEmptiesMap() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("A", "a");
        map.put("B", "b");
        assertEquals(2, map.size());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /* --------------------------------------------------------------------- *
     *  putAll and constructor that copies another map
     * --------------------------------------------------------------------- */

    @Test
    public void testPutAllCopiesAllEntriesCaseInsensitive() {
        Map<String, Integer> source = new HashMap<>();
        source.put("One", 1);
        source.put("Two", 2);

        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        map.putAll(source);

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("TWO"));
    }

    @Test
    public void testConstructorFromMapCopiesCaseInsensitive() {
        Map<String, String> src = new HashMap<>();
        src.put("Hello", "World");
        src.put("Foo", "Bar");

        CompactCIHashMap<String, String> map = new CompactCIHashMap<>(src);
        assertEquals(2, map.size());
        assertEquals("World", map.get("hello"));
        assertEquals("Bar", map.get("FOO"));
    }

    /* --------------------------------------------------------------------- *
     *  minus / plus – functional style helpers
     * --------------------------------------------------------------------- */

    @Test
    public void testMinusRemovesKeyAndReturnsNewMap() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("A", "a");
        map.put("B", "b");
        map.put("C", "c");

        // minus should not modify the original map
        Map<String, String> result = map.minus("b");   // key in different case
        assertEquals(2, result.size());
        assertFalse(result.containsKey("B"));
        assertTrue(map.containsKey("B")); // original unchanged
    }

    @Test
    public void testPlusAddsEntryAndReturnsNewMap() {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        map.put("X", 10);

        Map<String, Integer> result = map.plus("y"); // adds entry with null value
        assertEquals(2, result.size());
        assertTrue(result.containsKey("Y"));
        assertNull(result.get("Y"));

        // original map unchanged
        assertEquals(1, map.size());
        assertFalse(map.containsKey("Y"));
    }

    /* --------------------------------------------------------------------- *
     *  equals / hashCode – maps with different key cases should be equal
     * --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeCaseInsensitive() {
        CompactCIHashMap<String, String> map1 = new CompactCIHashMap<>();
        map1.put("Key", "value");

        CompactCIHashMap<String, String> map2 = new CompactCIHashMap<>();
        map2.put("kEy", "value");

        assertTrue(map1.equals(map2));
        assertTrue(map2.equals(map1));
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testEqualsDifferentValues() {
        CompactCIHashMap<String, String> map1 = new CompactCIHashMap<>();
        map1.put("Key", "value1");

        CompactCIHashMap<String, String> map2 = new CompactCIHashMap<>();
        map2.put("KEY", "value2");

        assertFalse(map1.equals(map2));
    }

    /* --------------------------------------------------------------------- *
     *  getConfig – verify that the default configuration reflects the CI nature
     * --------------------------------------------------------------------- */

    @Test
    public void testDefaultConfigContainsCaseInsensitiveFlag() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        Map<String, Object> config = map.getConfig();

        // The config must contain the flag that signals case‑insensitivity
        assertTrue(config.containsKey(CompactMap.CASE_SENSITIVE));
        assertEquals(Boolean.FALSE, config.get(CompactMap.CASE_SENSITIVE));

        // Compact size should be the default for CI maps (50)
        assertTrue(config.containsKey(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, config.get(CompactMap.COMPACT_SIZE));
    }

    /* --------------------------------------------------------------------- *
     *  toString – should contain the entries in a readable form
     * --------------------------------------------------------------------- */

    @Test
    public void testToStringRepresentation() {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        map.put("One", 1);
        map.put("Two", 2);

        String s = map.toString();
        // The exact format is not guaranteed, but it must contain both keys (any case)
        assertTrue(s.contains("One"));
        assertTrue(s.contains("Two"));
        assertTrue(s.contains("1"));
        assertTrue(s.contains("2"));
    }

    /* --------------------------------------------------------------------- *
     *  Internal behavior – verify that the map uses the CaseInsensitiveMap
     *  implementation by checking that a key with different case collides.
     * --------------------------------------------------------------------- */

    @Test
    public void testUnderlyingMapIsCaseInsensitive() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        // Insert two keys that differ only by case – the second should replace the first
        map.put("Test", "first");
        map.put("test", "second");

        assertEquals(1, map.size());
        assertEquals("second", map.get("TEST"));
    }

    /* --------------------------------------------------------------------- *
     *  Edge cases – null keys and null values
     * --------------------------------------------------------------------- */

    @Test
    public void testNullValueIsAllowed() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("Nullable", null);
        assertTrue(map.containsKey("nullable"));
        assertNull(map.get("NULLABLE"));
    }

    @Test(expected = NullPointerException.class)
    public void testNullKeyThrowsException() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        // According to CompactMap contract, null keys are not permitted
        map.put(null, "value");
    }
}
