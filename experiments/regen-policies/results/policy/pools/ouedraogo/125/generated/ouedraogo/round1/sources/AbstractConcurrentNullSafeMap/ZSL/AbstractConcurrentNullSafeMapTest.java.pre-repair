package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

public class AbstractConcurrentNullSafeMapTest {

    /** Simple concrete implementation for testing purposes. */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {
        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    private TestMap map;

    @Before
    public void setUp() {
        map = new TestMap();
    }

    /* --------------------------- Basic Map Operations --------------------------- */

    @Test
    public void testPutAndGet_WithNullKeyAndValue() {
        assertNull(map.put(null, "nullKey"));
        assertEquals("nullKey", map.get(null));

        assertNull(map.put("key", null));
        assertNull(map.get("key"));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("a", "1");
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put(null, "nullKey");
        map.put("k1", null);
        map.put("k2", "v2");

        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsKey("k2"));
        assertFalse(map.containsKey("missing"));

        assertTrue(map.containsValue(null));
        assertTrue(map.containsValue("nullKey"));
        assertTrue(map.containsValue("v2"));
        assertFalse(map.containsValue("nope"));
    }

    @Test
    public void testRemove() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);

        assertEquals("1", map.remove("a"));
        assertNull(map.remove("a")); // already removed

        assertEquals("nullKey", map.remove(null));

        assertNull(map.remove("b")); // value was null, remove returns null per contract
        assertFalse(map.containsKey("b"));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> other = new HashMap<>();
        other.put("x", "100");
        other.put(null, "nullKey");
        other.put("y", null);

        map.putAll(other);
        assertEquals(3, map.size());
        assertEquals("100", map.get("x"));
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("y"));

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetOrDefault() {
        map.put("present", "value");
        assertEquals("value", map.getOrDefault("present", "def"));
        assertEquals("def", map.getOrDefault("absent", "def"));
        // null key with default
        assertEquals("def", map.getOrDefault(null, "def"));
        map.put(null, "nullKey");
        assertEquals("nullKey", map.getOrDefault(null, "def"));
    }

    @Test
    public void testPutIfAbsent() {
        map.put("a", "first");
        assertEquals("first", map.putIfAbsent("a", "second"));
        assertEquals("first", map.get("a"));

        assertNull(map.putIfAbsent("b", null));
        assertNull(map.get("b"));

        assertNull(map.putIfAbsent(null, "nullKey"));
        assertEquals("nullKey", map.get(null));
    }

    @Test
    public void testRemoveKeyValue() {
        map.put("k", "v");
        assertFalse(map.remove("k", "wrong"));
        assertTrue(map.remove("k", "v"));
        assertFalse(map.containsKey("k"));

        map.put(null, null);
        assertTrue(map.remove(null, null));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testReplaceKeyOldNew() {
        map.put("k", "old");
        assertTrue(map.replace("k", "old", "new"));
        assertEquals("new", map.get("k"));

        assertFalse(map.replace("k", "old", "another"));
        assertEquals("new", map.get("k"));

        // replace with null values
        map.put("n", null);
        assertTrue(map.replace("n", null, "now"));
        assertEquals("now", map.get("n"));
    }

    @Test
    public void testReplaceKeyValue() {
        map.put("k", "v1");
        assertEquals("v1", map.replace("k", "v2"));
        assertEquals("v2", map.get("k"));

        // replace non‑existing key returns null
        assertNull(map.replace("missing", "x"));
    }

    /* --------------------------- Compute & Merge --------------------------- */

    @Test
    public void testComputeIfAbsent_WithExistingNonNull() {
        map.put("a", "val");
        String result = map.computeIfAbsent("a", k -> "computed");
        assertEquals("val", result);
        assertEquals("val", map.get("a"));
    }

    @Test
    public void testComputeIfAbsent_WithNullAndComputeNonNull() {
        map.put("b", null);
        String result = map.computeIfAbsent("b", k -> "newVal");
        assertEquals("newVal", result);
        assertEquals("newVal", map.get("b"));
    }

    @Test
    public void testComputeIfAbsent_WithAbsentAndComputeNull() {
        String result = map.computeIfAbsent("c", k -> null);
        assertNull(result);
        assertFalse(map.containsKey("c"));
    }

    @Test
    public void testCompute_ModifyAndRemove() {
        map.put("x", "1");
        String res = map.compute("x", (k, v) -> v + "2");
        assertEquals("12", res);
        assertEquals("12", map.get("x"));

        // returning null removes the entry
        res = map.compute("x", (k, v) -> null);
        assertNull(res);
        assertFalse(map.containsKey("x"));
    }

    @Test
    public void testMerge_WithRemapping() {
        map.put("m", "first");
        // merge where key exists
        String merged = map.merge("m", "second", (v1, v2) -> v1 + "+" + v2);
        assertEquals("first+second", merged);
        assertEquals("first+second", map.get("m"));

        // merge new key
        merged = map.merge("new", "value", (v1, v2) -> v1); // remapping not used
        assertEquals("value", merged);
        assertEquals("value", map.get("new"));
    }

    @Test(expected = NullPointerException.class)
    public void testMerge_NullRemappingFunction() {
        map.merge("k", "v", null);
    }

    @Test(expected = NullPointerException.class)
    public void testMerge_NullValueArgument() {
        map.merge("k", null, (v1, v2) -> v1);
    }

    /* --------------------------- Collection Views --------------------------- */

    @Test
    public void testValuesCollection() {
        map.put("a", "1");
        map.put(null, null);
        map.put("b", null);

        // size reflects three entries
        assertEquals(3, map.values().size());

        // contains works for null and non‑null values
        assertTrue(map.values().contains("1"));
        assertTrue(map.values().contains(null));

        // iterator returns unmasked values
        int nullCount = 0;
        int nonNullCount = 0;
        for (String v : map.values()) {
            if (v == null) nullCount++;
            else nonNullCount++;
        }
        assertEquals(2, nullCount);
        assertEquals(1, nonNullCount);
    }

    @Test
    public void testKeySetCollection() {
        map.put("k1", "v1");
        map.put(null, "nullKey");
        map.put("k2", null);

        assertEquals(3, map.keySet().size());
        assertTrue(map.keySet().contains("k1"));
        assertTrue(map.keySet().contains(null));
        assertTrue(map.keySet().contains("k2"));

        // remove via keySet
        assertTrue(map.keySet().remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(2, map.size());
    }

    @Test
    public void testEntrySetOperations() {
        map.put("e1", "v1");
        map.put(null, null);
        map.put("e2", null);

        // contains entry with null value
        Map.Entry<String, String> entryNull = new AbstractMap.SimpleEntry<>(null, null);
        assertTrue(map.entrySet().contains(entryNull));

        // remove entry via entrySet
        assertTrue(map.entrySet().remove(entryNull));
        assertFalse(map.containsKey(null));
        assertEquals(2, map.size());

        // modify value via setValue()
        for (Map.Entry<String, String> e : map.entrySet()) {
            if ("e1".equals(e.getKey())) {
                e.setValue("newV1");
            }
        }
        assertEquals("newV1", map.get("e1"));
    }

    /* --------------------------- Equality, HashCode, ToString --------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        map.put("k", "v");
        map.put(null, null);
        map.put("n", null);

        Map<String, String> other = new HashMap<>();
        other.put("k", "v");
        other.put(null, null);
        other.put("n", null);

        assertTrue(map.equals(other));
        assertTrue(other.equals(map));
        assertEquals(map.hashCode(), other.hashCode());

        // modify other -> inequality
        other.put("extra", "x");
        assertFalse(map.equals(other));
    }

    @Test
    public void testToStringUsesMapUtilities() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);
        String s = map.toString();

        // Order is not guaranteed, so just verify that all pairs appear
        assertTrue(s.contains("a=1"));
        assertTrue(s.contains("null=nullKey"));
        assertTrue(s.contains("b=null"));
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }
}
