package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link AbstractConcurrentNullSafeMap}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>All public methods with normal (happy‑path) usage.</li>
 *   <li>Edge cases involving {@code null} keys and/or values.</li>
 *   <li>Exception‑handling scenarios (e.g. {@code null} remapping functions).</li>
 *   <li>Correct behaviour of {@code equals}, {@code hashCode} and {@code toString}.</li>
 * </ul>
 *
 * <p>A concrete subclass {@code SimpleNullSafeMap} is defined inside the test
 *   file; it delegates to a {@link ConcurrentHashMap} as the internal map.
 */
public class AbstractConcurrentNullSafeMapTest {

    /** Simple concrete implementation used for testing. */
    private static class SimpleNullSafeMap extends AbstractConcurrentNullSafeMap<String, String> {
        SimpleNullSafeMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    private SimpleNullSafeMap map;

    @Before
    public void setUp() {
        map = new SimpleNullSafeMap();
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic functionality tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("a", "1");
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
    }

    @Test
    public void testPutGetRemove() {
        assertNull(map.put("k1", "v1"));
        assertEquals("v1", map.get("k1"));
        assertEquals("v1", map.remove("k1"));
        assertNull(map.get("k1"));
    }

    @Test
    public void testPutNullKeyAndValue() {
        // null key
        assertNull(map.put(null, "nullKey"));
        assertEquals("nullKey", map.get(null));
        // null value
        assertNull(map.put("nullValue", null));
        assertNull(map.get("nullValue"));
        // both null
        assertNull(map.put(null, null));
        assertNull(map.get(null));
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put("x", "y");
        map.put(null, "z");
        map.put("w", null);

        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey(null));
        assertFalse(map.containsKey("missing"));

        assertTrue(map.containsValue("y"));
        assertTrue(map.containsValue(null));
        assertFalse(map.containsValue("absent"));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> src = new AbstractMap.SimpleEntry<>("a", "1")
                .getKey() != null ? Collections.singletonMap("a", "1") : Collections.emptyMap();
        src = new java.util.HashMap<>();
        src.put("a", "1");
        src.put(null, null);
        src.put("b", null);

        map.putAll(src);
        assertEquals(3, map.size());
        assertEquals("1", map.get("a"));
        assertNull(map.get(null));
        assertNull(map.get("b"));

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetOrDefault() {
        map.put("k", "v");
        assertEquals("v", map.getOrDefault("k", "def"));
        assertEquals("def", map.getOrDefault("missing", "def"));
    }

    @Test
    public void testPutIfAbsent() {
        map.put("k", "v1");
        assertEquals("v1", map.putIfAbsent("k", "v2"));
        assertEquals("v1", map.get("k"));

        assertNull(map.putIfAbsent("new", null));
        assertNull(map.get("new"));
    }

    @Test
    public void testRemoveKeyValue() {
        map.put("k", "v");
        assertTrue(map.remove("k", "v"));
        assertFalse(map.remove("k", "v")); // already removed
        assertFalse(map.remove("missing", "v"));
    }

    @Test
    public void testReplaceThreeArg() {
        map.put("k", "old");
        assertTrue(map.replace("k", "old", "new"));
        assertEquals("new", map.get("k"));
        assertFalse(map.replace("k", "old", "another"));
        assertFalse(map.replace("missing", "x", "y"));
    }

    @Test
    public void testReplaceTwoArg() {
        map.put("k", "old");
        assertEquals("old", map.replace("k", "new"));
        assertEquals("new", map.get("k"));
        assertNull(map.replace("missing", "value"));
    }

    @Test
    public void testComputeIfAbsent() {
        // key absent, mapping returns non‑null
        Function<String, String> f = k -> k.toUpperCase();
        assertEquals("A", map.computeIfAbsent("a", f));
        assertEquals("A", map.get("a"));

        // key present with non‑null value -> mapping not invoked
        map.put("b", "B");
        Function<String, String> shouldNotRun = k -> { fail("Mapping function should not be called"); return null; };
        assertEquals("B", map.computeIfAbsent("b", shouldNotRun));

        // key present with null value -> mapping invoked
        map.put("c", null);
        assertEquals("C", map.computeIfAbsent("c", f));
        assertEquals("C", map.get("c"));

        // mapping returns null -> entry removed
        map.put("d", null);
        Function<String, String> returnsNull = k -> null;
        assertNull(map.computeIfAbsent("d", returnsNull));
        assertFalse(map.containsKey("d"));
    }

    @Test
    public void testCompute() {
        BiFunction<String, String, String> concat = (k, v) -> (v == null ? "" : v) + k;
        // absent key
        assertEquals("x", map.compute("x", concat));
        assertEquals("x", map.get("x"));

        // present key with existing value
        map.put("y", "y0");
        assertEquals("y0y", map.compute("y", concat));
        assertEquals("y0y", map.get("y"));

        // remapping returns null => entry removed
        BiFunction<String, String, String> returnsNull = (k, v) -> null;
        map.put("z", "z0");
        assertNull(map.compute("z", returnsNull));
        assertFalse(map.containsKey("z"));
    }

    @Test
    public void testMerge() {
        BiFunction<String, String, String> merger = (v1, v2) -> v1 + "+" + v2;

        // absent key => value stored
        assertEquals("v1", map.merge("k1", "v1", merger));
        assertEquals("v1", map.get("k1"));

        // present key => merger applied
        assertEquals("v1+v2", map.merge("k1", "v2", merger));
        assertEquals("v1+v2", map.get("k1"));

        // merger returns null => entry removed
        BiFunction<String, String, String> returnsNull = (v1, v2) -> null;
        assertNull(map.merge("k1", "any", returnsNull));
        assertFalse(map.containsKey("k1"));
    }

    @Test(expected = NullPointerException.class)
    public void testMergeNullRemappingFunction() {
        map.merge("k", "v", null);
    }

    @Test(expected = NullPointerException.class)
    public void testMergeNullValueNotAllowed() {
        // According to the implementation, value argument must be non‑null
        map.merge("k", null, (v1, v2) -> "x");
    }

    /* ---------------------------------------------------------------------- */
    /*  Collection view tests                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testValuesCollection() {
        map.put("a", "1");
        map.put(null, null);
        Collection<String> vals = map.values();
        assertTrue(vals.contains("1"));
        assertTrue(vals.contains(null));
        assertEquals(2, vals.size());

        // remove via iterator
        vals.iterator().remove(); // will throw IllegalStateException if called before next()
        // Instead remove a specific element
        assertTrue(vals.remove("1"));
        assertFalse(vals.contains("1"));
        assertEquals(1, vals.size());
    }

    @Test
    public void testKeySetCollection() {
        map.put("k1", "v1");
        map.put(null, "v2");
        java.util.Set<String> keys = map.keySet();
        assertTrue(keys.contains("k1"));
        assertTrue(keys.contains(null));
        assertEquals(2, keys.size());

        // remove via set
        assertTrue(keys.remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(1, keys.size());
    }

    @Test
    public void testEntrySetCollection() {
        map.put("k", "v");
        map.put(null, null);
        java.util.Set<Map.Entry<String, String>> entries = map.entrySet();

        // verify contains
        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>("k", "v")));
        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>(null, null)));
        assertEquals(2, entries.size());

        // test setValue through entry
        for (Map.Entry<String, String> e : entries) {
            if ("k".equals(e.getKey())) {
                e.setValue("newV");
            }
        }
        assertEquals("newV", map.get("k"));
    }

    /* ---------------------------------------------------------------------- */
    /*  equals, hashCode, toString                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        map.put("a", "1");
        map.put(null, null);
        Map<String, String> other = new java.util.HashMap<>();
        other.put("a", "1");
        other.put(null, null);

        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());

        other.put("b", "2");
        assertFalse(map.equals(other));
    }

    @Test
    public void testToString() {
        assertEquals("{}", map.toString());

        map.put("x", "y");
        assertEquals("{x=y}", map.toString());

        map.put(null, null);
        // order is unspecified for hash map; we accept both possible orders
        String s = map.toString();
        assertTrue(s.equals("{x=y, null=null}") || s.equals("{null=null, x=y}"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper – fail method for lambda checks                                 */
    /* ---------------------------------------------------------------------- */

    private static void fail(String message) {
        org.junit.Assert.fail(message);
    }
}
