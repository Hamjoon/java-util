package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JUnit 4 test suite for {@link CompactCILinkedMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (empty and from another map)</li>
 *   <li>Case‑insensitive key handling</li>
 *   <li>LinkedHashMap ordering semantics (insertion order preserved)</li>
 *   <li>Standard {@link Map} operations delegated to the internal map</li>
 *   <li>Edge cases such as null keys/values, duplicate keys with different case,
 *       removal of non‑existent keys, and behavior when the map is empty.</li>
 * </ul>
 *
 * The class under test does not expose many public methods itself; it inherits
 * the full {@link Map} contract from {@link CompactMap}.  Therefore the tests
 * exercise the inherited API while also verifying the specific behaviours
 * introduced by {@code CompactCILinkedMap} (case‑insensitivity and linked‑hash
 * ordering).
 */
public class CompactCILinkedMapTest
{
    private CompactCILinkedMap<String, String> map;

    @Before
    public void setUp()
    {
        map = new CompactCILinkedMap<>();
    }

    /* ---------------------------------------------------------------------- */
    /*  Constructor tests                                                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructorCreatesEmptyMap()
    {
        assertTrue("Map should be empty after default construction", map.isEmpty());
        assertEquals("Size should be 0", 0, map.size());
    }

    @Test
    public void testConstructorFromOtherMapCopiesEntries()
    {
        Map<String, String> src = new LinkedHashMap<>();
        src.put("One", "1");
        src.put("Two", "2");
        src.put("Three", "3");

        CompactCILinkedMap<String, String> fromOther = new CompactCILinkedMap<>(src);

        assertEquals("Map should contain all entries from source", src.size(), fromOther.size());
        assertEquals("Value for key 'One' should match", "1", fromOther.get("One"));
        assertEquals("Value for key 'Two' should match", "2", fromOther.get("Two"));
        assertEquals("Value for key 'Three' should match", "3", fromOther.get("Three"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic Map operations (inherited from CompactMap)                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetRespectCaseInsensitivity()
    {
        map.put("Key", "value1");
        // Different case should retrieve the same entry
        assertEquals("Retrieving with different case should succeed", "value1", map.get("kEy"));
        // Overwrite with different case should replace the original entry
        map.put("kEy", "value2");
        assertEquals("Value should be overwritten", "value2", map.get("KEY"));
        assertEquals("Map size should remain 1 after case‑insensitive overwrite", 1, map.size());
    }

    @Test
    public void testContainsKeyIsCaseInsensitive()
    {
        map.put("Alpha", "A");
        assertTrue(map.containsKey("ALPHA"));
        assertTrue(map.containsKey("alpha"));
        assertFalse(map.containsKey("Beta"));
    }

    @Test
    public void testRemoveIsCaseInsensitive()
    {
        map.put("RemoveMe", "x");
        V removed = map.remove("removeme"); // lower case
        assertEquals("Removed value should be returned", "x", removed);
        assertFalse("Key should no longer be present", map.containsKey("REMOVEme"));
        assertTrue("Map should be empty after removal", map.isEmpty());
    }

    @Test
    public void testPutAllCopiesEntriesCaseInsensitively()
    {
        Map<String, String> src = new LinkedHashMap<>();
        src.put("First", "1");
        src.put("Second", "2");
        map.putAll(src);
        assertEquals("Size after putAll", 2, map.size());
        assertTrue(map.containsKey("first"));
        assertTrue(map.containsKey("SECOND"));
        assertEquals("1", map.get("FIRST"));
        assertEquals("2", map.get("second"));
    }

    @Test
    public void testClearEmptiesMap()
    {
        map.put("A", "a");
        map.put("B", "b");
        map.clear();
        assertTrue("Map should be empty after clear()", map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testKeySetPreservesInsertionOrder()
    {
        map.put("first", "1");
        map.put("second", "2");
        map.put("third", "3");

        // Because the underlying map is a LinkedHashMap, the iterator order must be insertion order
        assertArrayEquals(new Object[]{"first", "second", "third"}, map.keySet().toArray());
    }

    @Test
    public void testEntrySetPreservesInsertionOrder()
    {
        map.put("x", "X");
        map.put("y", "Y");
        map.put("z", "Z");

        Object[] expectedKeys = {"x", "y", "z"};
        int i = 0;
        for (Map.Entry<String, String> e : map.entrySet())
        {
            assertEquals("Entry key order should follow insertion order", expectedKeys[i], e.getKey());
            i++;
        }
        assertEquals("All entries should have been iterated", expectedKeys.length, i);
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge‑case & exception handling tests                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNullKeyIsHandledAccordingTo Underlying Map()
    {
        // LinkedHashMap (and thus CaseInsensitiveMap) permits a single null key.
        map.put(null, "nullValue");
        assertTrue("Map should contain null key", map.containsKey(null));
        assertEquals("nullValue", map.get(null));

        // Adding another entry with null key should overwrite the previous one
        map.put(null, "newNullValue");
        assertEquals("newNullValue", map.get(null));
        assertEquals("Size should be 1 after overwriting null key", 1, map.size());
    }

    @Test
    public void testNullValueIsAccepted()
    {
        map.put("keyWithNull", null);
        assertTrue("Map should contain the key", map.containsKey("keyWithNull"));
        assertNull("Value should be null", map.get("keyWithNull"));
        assertEquals(1, map.size());
    }

    @Test
    public void testRemoveNonExistentKeyReturnsNull()
    {
        assertNull("Removing a key that does not exist should return null", map.remove("noSuchKey"));
    }

    @Test
    public void testMinusCreatesNewMapWithoutSpecifiedKey()
    {
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");

        Map<String, String> result = map.minus("b");
        assertFalse("Result should not contain removed key", result.containsKey("b"));
        assertTrue("Result should still contain other keys", result.containsKey("a"));
        assertTrue("Result should still contain other keys", result.containsKey("c"));
        // Original map must stay unchanged
        assertTrue("Original map should still contain 'b'", map.containsKey("b"));
    }

    @Test
    public void testPlusCreatesNewMapWithAdditionalEntry()
    {
        map.put("x", "1");
        Map<String, String> result = map.plus("y");
        // The 'plus' method in CompactMap expects an Object that is a Map.Entry or a Map;
        // when a plain key is supplied it creates a singleton map with null value.
        assertTrue("Result should contain original key", result.containsKey("x"));
        assertTrue("Result should contain the new key", result.containsKey("y"));
        assertNull("New key should map to null (as per CompactMap.plus contract)", result.get("y"));
    }

    @Test
    public void testEqualsAndHashCodeContract()
    {
        CompactCILinkedMap<String, String> other = new CompactCILinkedMap<>();
        map.put("k1", "v1");
        map.put("k2", "v2");
        other.put("K1", "v1"); // different case should be considered equal
        other.put("k2", "v2");

        assertEquals("Maps with same entries (case‑insensitive) should be equal", map, other);
        assertEquals("Hash codes should match for equal maps", map.hashCode(), other.hashCode());
    }

    @Test
    public void testToStringContainsEntries()
    {
        map.put("one", "1");
        map.put("two", "2");
        String s = map.toString();
        assertTrue("String representation should contain key 'one'", s.contains("one"));
        assertTrue("String representation should contain value '2'", s.contains("2"));
    }

    @Test
    public void testGetConfigReturnsNonNullMap()
    {
        Map<String, Object> cfg = map.getConfig();
        assertNotNull("Configuration map should never be null", cfg);
        // Verify that default configuration entries exist
        assertTrue("Config should contain COMPACT_SIZE key", cfg.containsKey(CompactMap.COMPACT_SIZE));
        assertTrue("Config should contain CASE_SENSITIVE key", cfg.containsKey(CompactMap.CASE_SENSITIVE));
    }

    @Test
    public void testWithConfigAppliesConfiguration()
    {
        Map<String, Object> newCfg = Collections.singletonMap(CompactMap.CASE_SENSITIVE, Boolean.FALSE);
        CompactCILinkedMap<String, String> configured = map.withConfig(newCfg);
        // The map is case‑insensitive by definition; the config flag should be ignored but
        // the method must return a map instance (could be the same instance)
        assertNotNull("withConfig should return a map instance", configured);
        assertTrue("Map should still be case‑insensitive", configured.isCaseInsensitive());
    }

    /* ---------------------------------------------------------------------- */
    /*  Builder usage (static method from CompactMap)                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testBuilderCreatesMapWithProvidedEntries()
    {
        CompactCILinkedMap<String, Integer> built = CompactMap.<String, Integer>builder()
                .put("a", 1)
                .put("B", 2)
                .build()
                .withConfig(Collections.emptyMap()); // ensure we have a concrete instance

        // The builder returns a CompactMap; we cast to our concrete type for testing
        assertEquals("Size should be 2", 2, built.size());
        assertEquals(Integer.valueOf(1), built.get("A"));
        assertEquals(Integer.valueOf(2), built.get("b"));
    }
}
