package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * JUnit 4 test suite for {@link CompactCIHashMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (default and copy‑constructor)</li>
 *   <li>Case‑insensitive handling of {@link String} keys while preserving the original case</li>
 *   <li>Normal handling of non‑String keys</li>
 *   <li>All standard {@link Map} operations inherited from {@link CompactMap}</li>
 *   <li>Special {@code minus} and {@code plus} methods that return new map instances</li>
 *   <li>Equality and hash‑code contracts</li>
 * </ul>
 */
public class CompactCIHashMapTest {

    /** Helper to create a map pre‑populated with a few entries. */
    private CompactCIHashMap<String, String> createSampleMap() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("Alpha", "A");
        map.put("Bravo", "B");
        map.put("CHARLIE", "C");
        return map;
    }

    /* -------------------------------------------------------------
     *  Constructor tests
     * ------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        assertTrue("Map should be empty after default construction", map.isEmpty());
        assertEquals("Size should be 0", 0, map.size());
    }

    @Test
    public void testCopyConstructorCopiesAllEntries() {
        Map<String, String> source = new HashMap<>();
        source.put("One", "1");
        source.put("Two", "2");

        CompactCIHashMap<String, String> copy = new CompactCIHashMap<>(source);
        assertEquals("Copy should have same size as source", source.size(), copy.size());
        assertEquals("Value for key 'One' should be copied", "1", copy.get("One"));
        assertEquals("Value for key 'Two' should be copied", "2", copy.get("Two"));
    }

    /* -------------------------------------------------------------
     *  Case‑insensitive key handling
     * ------------------------------------------------------------- */

    @Test
    public void testPutAndGetCaseInsensitive() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();

        map.put("KeyOne", "value1");
        // Retrieval with different casings
        assertEquals("value1 should be returned for lower‑case key", "value1", map.get("keyone"));
        assertEquals("value1 should be returned for upper‑case key", "value1", map.get("KEYONE"));
        // Original case is retained in keySet
        Set<String> keys = map.keySet();
        assertTrue("KeySet must contain the original case key", keys.contains("KeyOne"));
        assertFalse("KeySet must not contain a differently cased key", keys.contains("keyone"));
    }

    @Test
    public void testOverwriteWithDifferentCaseKeepsSingleEntry() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();

        map.put("TestKey", "first");
        assertEquals(1, map.size());

        // Overwrite using a different case
        map.put("testkey", "second");
        assertEquals("Map size must remain 1 after case‑insensitive overwrite", 1, map.size());
        assertEquals("Value should be overwritten with the second entry", "second", map.get("TESTKEY"));
        // The key retained should be the most recent insertion case
        assertTrue("KeySet should contain the latest case version", map.keySet().contains("testkey"));
        assertFalse("KeySet should not contain the older case version", map.keySet().contains("TestKey"));
    }

    @Test
    public void testContainsKeyIsCaseInsensitive() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("MiXeD", "value");

        assertTrue("containsKey should be true for different case", map.containsKey("mixed"));
        assertTrue("containsKey should be true for upper case", map.containsKey("MIXED"));
        assertFalse("containsKey should be false for unrelated key", map.containsKey("other"));
    }

    @Test
    public void testRemoveIsCaseInsensitive() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("RemoveMe", "gone");
        assertEquals(1, map.size());

        // Remove using a different case
        String removed = map.remove("removeme");
        assertEquals("Removed value must be returned", "gone", removed);
        assertTrue("Map should be empty after removal", map.isEmpty());
    }

    /* -------------------------------------------------------------
     *  Non‑String key handling (should behave like a normal map)
     * ------------------------------------------------------------- */

    @Test
    public void testNonStringKeyIsHandledNormally() {
        CompactCIHashMap<Integer, String> map = new CompactCIHashMap<>();

        map.put(42, "answer");
        assertEquals("answer", map.get(42));
        assertTrue("containsKey should be true for the exact Integer key", map.containsKey(42));
        assertFalse("containsKey should be false for a different Integer", map.containsKey(43));
        assertEquals("Size should be 1", 1, map.size());

        // Removing with a different (but equal) Integer object
        Integer key = Integer.valueOf(42);
        String removed = map.remove(key);
        assertEquals("Removed value must match", "answer", removed);
        assertTrue("Map should be empty after removal", map.isEmpty());
    }

    /* -------------------------------------------------------------
     *  Standard Map operations
     * ------------------------------------------------------------- */

    @Test
    public void testPutAllAndClear() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();

        Map<String, String> other = new HashMap<>();
        other.put("Alpha", "A");
        other.put("Bravo", "B");
        other.put("charlie", "C"); // lower case key to test case handling

        map.putAll(other);
        assertEquals(3, map.size());
        assertEquals("A", map.get("ALPHA"));
        assertEquals("B", map.get("bravo"));
        assertEquals("C", map.get("CHARLIE"));

        map.clear();
        assertTrue("Map should be empty after clear()", map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testKeySetAndValuesReflectChanges() {
        CompactCIHashMap<String, String> map = createSampleMap();

        Set<String> keys = map.keySet();
        assertTrue(keys.contains("Alpha"));
        assertTrue(keys.contains("Bravo"));
        assertTrue(keys.contains("CHARLIE"));
        assertEquals(3, keys.size());

        // Modify map and verify view updates
        map.put("Delta", "D");
        assertTrue(map.keySet().contains("Delta"));
        assertEquals(4, map.keySet().size());

        // Remove a key and check view
        map.remove("bravo"); // case‑insensitive remove
        assertFalse(map.keySet().contains("Bravo"));
        assertEquals(3, map.keySet().size());
    }

    @Test
    public void testMinusCreatesNewMapWithoutSpecifiedKey() {
        CompactCIHashMap<String, String> original = createSampleMap();

        // minus should not mutate the original map
        Map<String, String> result = original.minus("bravo"); // case‑insensitive argument
        assertEquals("Original map size must stay unchanged", 3, original.size());
        assertFalse("Result map must not contain the removed key", result.containsKey("Bravo"));
        assertTrue("Result map must still contain other keys", result.containsKey("Alpha"));
        assertEquals("Result map should have size 2", 2, result.size());

        // Ensure the returned map is also case‑insensitive
        assertEquals("A", result.get("ALPHA"));
    }

    @Test
    public void testPlusCreatesNewMapWithAdditionalEntry() {
        CompactCIHashMap<String, String> original = createSampleMap();

        // plus should not mutate the original map
        Map<String, String> result = original.plus(Collections.singletonMap("echo", "E"));
        assertEquals("Original map size must stay unchanged", 3, original.size());
        assertTrue("Result map must contain the new entry (case‑insensitive)", result.containsKey("ECHO"));
        assertEquals("E", result.get("Echo"));
        assertEquals("Result map size should be original + 1", 4, result.size());
    }

    @Test
    public void testEqualsAndHashCodeContract() {
        CompactCIHashMap<String, String> map1 = new CompactCIHashMap<>();
        CompactCIHashMap<String, String> map2 = new CompactCIHashMap<>();

        map1.put("KeyOne", "1");
        map1.put("KeyTwo", "2");

        // Insert same logical entries but with different key casings
        map2.put("keyone", "1");
        map2.put("KEYTWO", "2");

        assertTrue("Maps with same logical entries must be equal", map1.equals(map2));
        assertTrue("Equality must be symmetric", map2.equals(map1));
        assertEquals("Hash codes must be equal for equal maps", map1.hashCode(), map2.hashCode());

        // Changing a value breaks equality
        map2.put("KeyTwo", "different");
        assertFalse("Maps should not be equal after a value change", map1.equals(map2));
    }

    @Test
    public void testIsEmptyAndSizeBehavior() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        assertTrue("New map must be empty", map.isEmpty());
        assertEquals(0, map.size());

        map.put("A", "a");
        assertFalse("Map should not be empty after put", map.isEmpty());
        assertEquals(1, map.size());

        map.remove("a");
        assertTrue("Map should be empty after removal", map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testNullKeyAndValueHandling() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();

        // Null key (non‑String) should be allowed
        map.put(null, "nullKey");
        assertEquals("nullKey", map.get(null));
        assertTrue(map.containsKey(null));

        // Null value
        map.put("someKey", null);
        assertTrue(map.containsKey("SOMEKEY"));
        assertNull("Value for 'someKey' should be null", map.get("somekey"));
    }

    @Test
    public void testToStringContainsAllEntries() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("One", "1");
        map.put("Two", "2");

        String str = map.toString();
        assertTrue("toString should contain first entry", str.contains("One=1"));
        assertTrue("toString should contain second entry", str.contains("Two=2"));
    }
}
