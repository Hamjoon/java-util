package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactLinkedMap}.
 *
 * The tests focus on the public behaviour inherited from {@link CompactMap}
 * while also exercising the specifics of the {@code CompactLinkedMap}
 * implementation (insertion‑order preservation and case‑sensitivity).
 *
 * Only APIs that appear in the supplied source code or in the listed
 * {@code CompactMap} public API are used.
 */
public class CompactLinkedMapTest {

    /* --------------------------------------------------------------------- *
     *  Basic construction & emptiness checks
     * --------------------------------------------------------------------- */

    @Test
    public void testEmptyMap() {
        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>();
        assertTrue("New map should be empty", map.isEmpty());
        assertEquals("Size of new map should be 0", 0, map.size());
        assertTrue("keySet should be empty", map.keySet().isEmpty());
        assertTrue("values collection should be empty", map.values().isEmpty());
    }

    @Test
    public void testConstructorWithExistingMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);

        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>(source);
        assertEquals("Map should contain all entries from source", 2, map.size());
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("two"));

        // Mutating the source map must NOT affect the CompactLinkedMap
        source.put("three", 3);
        assertNull("New entry in source must not appear in CompactLinkedMap", map.get("three"));
        assertEquals("Size must stay unchanged after source mutation", 2, map.size());
    }

    /* --------------------------------------------------------------------- *
     *  Put / Get / Contains / Remove
     * --------------------------------------------------------------------- */

    @Test
    public void testPutAndGet() {
        CompactLinkedMap<String, String> map = new CompactLinkedMap<>();
        assertNull("Getting a non‑existent key returns null", map.get("missing"));

        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");

        assertEquals("Size after three puts", 3, map.size());
        assertEquals("alpha", map.get("a"));
        assertEquals("beta", map.get("b"));
        assertEquals("gamma", map.get("c"));

        assertTrue("containsKey should be true for existing key", map.containsKey("b"));
        assertFalse("containsKey should be false for missing key", map.containsKey("z"));
        assertTrue("containsValue should be true for existing value", map.containsValue("gamma"));
        assertFalse("containsValue should be false for missing value", map.containsValue("delta"));
    }

    @Test
    public void testRemove() {
        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>();
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        Integer removed = map.remove("y");
        assertEquals(Integer.valueOf(20), removed);
        assertEquals("Size after removal", 2, map.size());
        assertFalse("Key y should no longer be present", map.containsKey("y"));
        assertNull("Getting removed key returns null", map.get("y"));
    }

    /* --------------------------------------------------------------------- *
     *  Insertion‑order preservation (LinkedHashMap semantics)
     * --------------------------------------------------------------------- */

    @Test
    public void testInsertionOrderPreserved() {
        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>();
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        // Verify keySet order
        Iterator<String> keyIter = map.keySet().iterator();
        assertTrue(keyIter.hasNext());
        assertEquals("first", keyIter.next());
        assertEquals("second", keyIter.next());
        assertEquals("third", keyIter.next());
        assertFalse(keyIter.hasNext());

        // Verify values order
        Iterator<Integer> valIter = map.values().iterator();
        assertTrue(valIter.hasNext());
        assertEquals(Integer.valueOf(1), valIter.next());
        assertEquals(Integer.valueOf(2), valIter.next());
        assertEquals(Integer.valueOf(3), valIter.next());
        assertFalse(valIter.hasNext());
    }

    /* --------------------------------------------------------------------- *
     *  Case‑sensitivity (CompactLinkedMap is case‑sensitive)
     * --------------------------------------------------------------------- */

    @Test
    public void testCaseSensitivity() {
        CompactLinkedMap<String, String> map = new CompactLinkedMap<>();
        map.put("Key", "upper");
        map.put("key", "lower");

        assertEquals(2, map.size());
        assertEquals("upper", map.get("Key"));
        assertEquals("lower", map.get("key"));
        assertTrue(map.containsKey("Key"));
        assertTrue(map.containsKey("key"));
        assertFalse(map.containsKey("KEY"));
    }

    /* --------------------------------------------------------------------- *
     *  Bulk operations: putAll, clear, equals, hashCode, toString
     * --------------------------------------------------------------------- */

    @Test
    public void testPutAllAndClear() {
        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Map<String, Integer> other = new HashMap<>();
        other.put("c", 3);
        other.put("d", 4);

        map.putAll(other);
        assertEquals(4, map.size());
        assertEquals(Integer.valueOf(3), map.get("c"));
        assertEquals(Integer.valueOf(4), map.get("d"));

        map.clear();
        assertTrue("Map should be empty after clear()", map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactLinkedMap<String, String> map1 = new CompactLinkedMap<>();
        CompactLinkedMap<String, String> map2 = new CompactLinkedMap<>();

        map1.put("one", "1");
        map1.put("two", "2");

        map2.put("one", "1");
        map2.put("two", "2");

        assertTrue("Maps with same entries must be equal", map1.equals(map2));
        assertEquals("Equal maps must have same hashCode", map1.hashCode(), map2.hashCode());

        map2.put("three", "3");
        assertFalse("Maps with different entries must not be equal", map1.equals(map2));
    }

    @Test
    public void testToStringContainsEntries() {
        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>();
        map.put("alpha", 100);
        map.put("beta", 200);

        String s = map.toString();
        assertNotNull("toString should not return null", s);
        assertTrue("toString should contain key 'alpha'", s.contains("alpha"));
        assertTrue("toString should contain value '200'", s.contains("200"));
    }

    /* --------------------------------------------------------------------- *
     *  minus() – returns a new map without the specified key
     * --------------------------------------------------------------------- */

    @Test
    public void testMinusCreatesNewMapWithoutKey() {
        CompactLinkedMap<String, String> original = new CompactLinkedMap<>();
        original.put("keep1", "v1");
        original.put("removeMe", "gone");
        original.put("keep2", "v2");

        Map<String, String> reduced = original.minus("removeMe");

        // The returned map must be a new instance and must not contain the removed key
        assertNotSame("minus should return a new map instance", original, reduced);
        assertEquals(2, reduced.size());
        assertFalse(reduced.containsKey("removeMe"));
        assertTrue(reduced.containsKey("keep1"));
        assertTrue(reduced.containsKey("keep2"));

        // Original map must stay unchanged
        assertEquals(3, original.size());
        assertTrue(original.containsKey("removeMe"));
    }

    /* --------------------------------------------------------------------- *
     *  Config handling (withConfig / getConfig)
     * --------------------------------------------------------------------- */

    @Test
    public void testConfigRoundTrip() {
        CompactLinkedMap<String, String> map = new CompactLinkedMap<>();

        // Retrieve the default config and add a custom entry
        Map<String, Object> cfg = map.getConfig();
        cfg.put("customKey", "customValue");

        // Apply the modified config back to the map
        CompactLinkedMap<String, String> returned = map.withConfig(cfg);
        assertSame("withConfig should return the same map instance", map, returned);

        // The map's config must now contain the custom entry
        Map<String, Object> after = map.getConfig();
        assertEquals("customValue", after.get("customKey"));
    }
}
