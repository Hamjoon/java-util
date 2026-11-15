package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

public class AbstractConcurrentNullSafeMapTest {

    /**
     * Simple concrete implementation of the abstract class using a ConcurrentHashMap as the backing store.
     */
    private static class SimpleMap extends AbstractConcurrentNullSafeMap<String, String> {
        SimpleMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    private SimpleMap map;

    @Before
    public void setUp() {
        map = new SimpleMap();
    }

    /* --------------------------------------------------------------------- */
    /*  Basic put / get tests */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPutAndGetNonNull() {
        assertNull(map.put("a", "alpha"));
        assertEquals("alpha", map.get("a"));
    }

    @Test
    public void testPutAndGetNullKey() {
        assertNull(map.put(null, "nullKey"));
        assertEquals("nullKey", map.get(null));
    }

    @Test
    public void testPutAndGetNullValue() {
        assertNull(map.put("b", null));
        assertNull(map.get("b"));
    }

    @Test
    public void testContainsKeyAndValue() {
        map.put("k1", "v1");
        map.put(null, "nullKey");
        map.put("k2", null);

        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("k2"));

        assertTrue(map.containsValue("v1"));
        assertTrue(map.containsValue("nullKey"));
        assertTrue(map.containsValue(null));
    }

    @Test
    public void testRemoveKey() {
        map.put("x", "xValue");
        map.put(null, "nullValue");
        map.put("y", null);

        assertEquals("xValue", map.remove("x"));
        assertNull(map.get("x"));

        assertEquals("nullValue", map.remove(null));
        assertNull(map.get(null));

        assertNull(map.remove("y")); // previous value was null
        assertFalse(map.containsKey("y"));
    }

    @Test
    public void testPutIfAbsent() {
        assertNull(map.putIfAbsent("p", "first"));
        assertEquals("first", map.get("p"));

        // subsequent call should return existing value and not replace it
        assertEquals("first", map.putIfAbsent("p", "second"));
        assertEquals("first", map.get("p"));

        // null key/value handling
        assertNull(map.putIfAbsent(null, null));
        assertNull(map.get(null));
        assertNull(map.putIfAbsent(null, "nowNotNull"));
        assertEquals("nowNotNull", map.get(null));
    }

    @Test
    public void testRemoveKeyValuePair() {
        map.put("k", "v");
        map.put("k2", null);
        map.put(null, "nullKey");

        // successful removal
        assertTrue(map.remove("k", "v"));
        assertFalse(map.containsKey("k"));

        // removal with null value
        assertTrue(map.remove("k2", null));
        assertFalse(map.containsKey("k2"));

        // removal with null key
        assertTrue(map.remove(null, "nullKey"));
        assertFalse(map.containsKey(null));

        // unsuccessful removal
        map.put("a", "b");
        assertFalse(map.remove("a", "different"));
    }

    @Test
    public void testReplaceThreeArg() {
        map.put("r", "old");
        assertTrue(map.replace("r", "old", "new"));
        assertEquals("new", map.get("r"));

        // old value mismatch
        assertFalse(map.replace("r", "old", "another"));
        assertEquals("new", map.get("r"));

        // null handling
        map.put(null, "nullOld");
        assertTrue(map.replace(null, "nullOld", "nullNew"));
        assertEquals("nullNew", map.get(null));

        map.put("nullVal", null);
        assertTrue(map.replace("nullVal", null, "nowNotNull"));
        assertEquals("nowNotNull", map.get("nullVal"));
    }

    @Test
    public void testReplaceOneArg() {
        map.put("s", "first");
        assertEquals("first", map.replace("s", "second"));
        assertEquals("second", map.get("s"));

        // replace on missing key returns null
        assertNull(map.replace("missing", "any"));
        assertNull(map.get("missing"));

        // null handling
        map.put(null, "nullFirst");
        assertEquals("nullFirst", map.replace(null, "nullSecond"));
        assertEquals("nullSecond", map.get(null));
    }

    @Test
    public void testGetOrDefault() {
        map.put("exists", "value");
        assertEquals("value", map.getOrDefault("exists", "default"));
        assertEquals("default", map.getOrDefault("missing", "default"));
        // null key with default
        assertEquals("def", map.getOrDefault(null, "def"));
    }

    /* --------------------------------------------------------------------- */
    /*  computeIfAbsent tests */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeIfAbsentCreatesValue() {
        Function<String, String> mapping = k -> k.toUpperCase();
        assertEquals("KEY", map.computeIfAbsent("key", mapping));
        assertEquals("KEY", map.get("key"));
    }

    @Test
    public void testComputeIfAbsentDoesNotOverwriteExistingNonNull() {
        map.put("present", "already");
        Function<String, String> mapping = k -> "newValue";
        assertEquals("already", map.computeIfAbsent("present", mapping));
    }

    @Test
    public void testComputeIfAbsentHandlesNullMappingResult() {
        Function<String, String> mapping = k -> null;
        // key not present, mapping returns null => no entry should be created
        assertNull(map.computeIfAbsent("noEntry", mapping));
        assertFalse(map.containsKey("noEntry"));
    }

    @Test(expected = NullPointerException.class)
    public void testComputeIfAbsentNullFunctionThrows() {
        map.computeIfAbsent("any", null);
    }

    /* --------------------------------------------------------------------- */
    /*  compute tests */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeReplacesValue() {
        map.put("c", "old");
        BiFunction<String, String, String> remap = (k, v) -> v + "X";
        assertEquals("oldX", map.compute("c", remap));
        assertEquals("oldX", map.get("c"));
    }

    @Test
    public void testComputeCreatesValueWhenAbsent() {
        BiFunction<String, String, String> remap = (k, v) -> k + "value";
        assertEquals("newvalue", map.compute("new", remap));
        assertEquals("newvalue", map.get("new"));
    }

    @Test
    public void testComputeRemapToNullRemovesEntry() {
        map.put("t", "temp");
        BiFunction<String, String, String> remap = (k, v) -> null;
        assertNull(map.compute("t", remap));
        assertFalse(map.containsKey("t"));
    }

    @Test(expected = NullPointerException.class)
    public void testComputeNullRemappingFunctionThrows() {
        map.compute("any", null);
    }

    /* --------------------------------------------------------------------- */
    /*  merge tests */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMergeNewKey() {
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV + newV;
        assertEquals("value", map.merge("m", "value", remap));
        assertEquals("value", map.get("m"));
    }

    @Test
    public void testMergeExistingKey() {
        map.put("m", "old");
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV + "-" + newV;
        assertEquals("old-new", map.merge("m", "new", remap));
        assertEquals("old-new", map.get("m"));
    }

    @Test
    public void testMergeRemapReturnsNullRemovesEntry() {
        map.put("z", "keep");
        BiFunction<String, String, String> remap = (oldV, newV) -> null;
        assertNull(map.merge("z", "any", remap));
        assertFalse(map.containsKey("z"));
    }

    @Test(expected = NullPointerException.class)
    public void testMergeNullRemappingFunctionThrows() {
        map.merge("k", "v", null);
    }

    @Test(expected = NullPointerException.class)
    public void testMergeNullValueThrows() {
        BiFunction<String, String, String> remap = (a, b) -> a;
        map.merge("k", null, remap);
    }

    /* --------------------------------------------------------------------- */
    /*  values, keySet and entrySet collections */
    /* --------------------------------------------------------------------- */

    @Test
    public void testValuesCollection() {
        map.put("a", "1");
        map.put(null, "nullVal");
        map.put("b", null);

        Collection<String> values = map.values();
        assertTrue(values.contains("1"));
        assertTrue(values.contains("nullVal"));
        assertTrue(values.contains(null));
        assertEquals(3, values.size());

        // iterator test
        Set<String> iterated = new HashSet<>();
        for (String v : values) {
            iterated.add(v);
        }
        assertEquals(new HashSet<>(Arrays.asList("1", "nullVal", null)), iterated);
    }

    @Test
    public void testKeySetCollection() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);

        Set<String> keys = map.keySet();
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains(null));
        assertTrue(keys.contains("b"));
        assertEquals(3, keys.size());

        // remove via keySet
        assertTrue(keys.remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(2, map.size());
    }

    @Test
    public void testEntrySetOperations() {
        map.put("k1", "v1");
        map.put(null, "nullV");
        map.put("k2", null);

        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(3, entries.size());

        // find a specific entry
        boolean found = false;
        for (Map.Entry<String, String> e : entries) {
            if (Objects.equals(e.getKey(), "k1") && Objects.equals(e.getValue(), "v1")) {
                found = true;
                // test setValue
                e.setValue("v1-modified");
                break;
            }
        }
        assertTrue(found);
        assertEquals("v1-modified", map.get("k1"));

        // test remove via entrySet
        Map.Entry<String, String> toRemove = null;
        for (Map.Entry<String, String> e : entries) {
            if (e.getKey() == null) {
                toRemove = e;
                break;
            }
        }
        assertNotNull(toRemove);
        assertTrue(entries.remove(toRemove));
        assertFalse(map.containsKey(null));
    }

    /* --------------------------------------------------------------------- */
    /*  equals, hashCode and toString */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        map.put("x", "1");
        map.put(null, null);
        map.put("y", "2");

        SimpleMap other = new SimpleMap();
        other.put("x", "1");
        other.put(null, null);
        other.put("y", "2");

        assertEquals(map, other);
        assertEquals(other, map);
        assertEquals(map.hashCode(), other.hashCode());

        // change one entry
        other.put("y", "different");
        assertNotEquals(map, other);
    }

    @Test
    public void testToStringContainsAllEntries() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);
        String s = map.toString();

        // The exact order is not guaranteed, but all key/value pairs must appear.
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("a=1"));
        assertTrue(s.contains("nullKey")); // null key representation
        assertTrue(s.contains("b=null"));
    }

    /* --------------------------------------------------------------------- */
    /*  putAll and clear */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPutAll() {
        Map<String, String> source = new HashMap<>();
        source.put("p", "1");
        source.put(null, "nullKey");
        source.put("q", null);

        map.putAll(source);
        assertEquals(3, map.size());
        assertEquals("1", map.get("p"));
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("q"));
    }

    @Test
    public void testClear() {
        map.put("a", "1");
        map.put(null, null);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }
}
