package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link AbstractConcurrentNullSafeMap}.
 * A concrete implementation is provided via an inner class that delegates to a {@link ConcurrentHashMap}.
 */
public class AbstractConcurrentNullSafeMapTest {

    /** Simple concrete implementation used for testing. */
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

    /* -------------------------------------------------
     * Basic put / get / remove / size / isEmpty tests
     * ------------------------------------------------- */
    @Test
    public void testBasicOperationsIncludingNulls() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        // put non‑null key/value
        assertNull(map.put("a", "alpha"));
        assertEquals("alpha", map.get("a"));
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        // put null key and null value
        assertNull(map.put(null, null));
        assertNull(map.get(null));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));

        // replace existing value
        assertEquals("alpha", map.put("a", "beta"));
        assertEquals("beta", map.get("a"));

        // remove entries
        assertEquals("beta", map.remove("a"));
        assertNull(map.get("a"));
        assertFalse(map.containsKey("a"));

        assertNull(map.remove(null));
        assertFalse(map.containsKey(null));
        assertFalse(map.containsValue(null));

        assertTrue(map.isEmpty());
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> source = new HashMap<>();
        source.put("x", "X");
        source.put(null, "nullKey");
        source.put("y", null);
        map.putAll(source);

        assertEquals(3, map.size());
        assertEquals("X", map.get("x"));
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("y"));
        assertTrue(map.containsValue(null));

        map.clear();
        assertTrue(map.isEmpty());
    }

    /* -------------------------------------------------
     * containsKey / containsValue edge cases
     * ------------------------------------------------- */
    @Test
    public void testContainsKeyAndValueWithSentinels() {
        map.put(null, "v1");
        map.put("k1", null);
        map.put("k2", "v2");

        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsKey("k2"));
        assertFalse(map.containsKey("missing"));

        assertTrue(map.containsValue(null));
        assertTrue(map.containsValue("v1"));
        assertTrue(map.containsValue("v2"));
        assertFalse(map.containsValue("absent"));
    }

    /* -------------------------------------------------
     * getOrDefault
     * ------------------------------------------------- */
    @Test
    public void testGetOrDefault() {
        map.put("a", null);
        assertEquals("def", map.getOrDefault("a", "def"));
        assertEquals("def", map.getOrDefault("missing", "def"));
        map.put(null, "nullKey");
        assertEquals("nullKey", map.getOrDefault(null, "def"));
    }

    /* -------------------------------------------------
     * putIfAbsent, remove(key,value), replace variants
     * ------------------------------------------------- */
    @Test
    public void testConditionalOperations() {
        // putIfAbsent
        assertNull(map.putIfAbsent("k", "v1"));
        assertEquals("v1", map.putIfAbsent("k", "v2"));
        assertEquals("v1", map.get("k"));

        // remove(key,value)
        assertFalse(map.remove("k", "wrong"));
        assertTrue(map.remove("k", "v1"));
        assertFalse(map.containsKey("k"));

        // replace(key, old, new)
        map.put("p", "old");
        assertTrue(map.replace("p", "old", "new"));
        assertEquals("new", map.get("p"));
        assertFalse(map.replace("p", "old", "another"));

        // replace(key, value)
        assertEquals("new", map.replace("p", "final"));
        assertEquals("final", map.get("p"));
    }

    /* -------------------------------------------------
     * computeIfAbsent
     * ------------------------------------------------- */
    @Test
    public void testComputeIfAbsent() {
        // key absent, mapping returns non‑null
        Function<String, String> mapper = k -> k.toUpperCase();
        assertEquals("ABC", map.computeIfAbsent("abc", mapper));
        assertEquals("ABC", map.get("abc"));

        // key present with non‑null value – mapper not called
        mapper = k -> { fail("Mapper should not be invoked"); return "X"; };
        assertEquals("ABC", map.computeIfAbsent("abc", mapper));

        // key present with null value – mapper called
        map.put("nullVal", null);
        final boolean[] called = {false};
        mapper = k -> { called[0] = true; return "computed"; };
        assertEquals("computed", map.computeIfAbsent("nullVal", mapper));
        assertTrue(called[0]);

        // mapper returns null – entry removed
        map.put("removeMe", null);
        mapper = k -> null;
        assertNull(map.computeIfAbsent("removeMe", mapper));
        assertFalse(map.containsKey("removeMe"));
    }

    /* -------------------------------------------------
     * compute
     * ------------------------------------------------- */
    @Test
    public void testCompute() {
        BiFunction<String, String, String> remap = (k, v) -> (v == null) ? "init" : v + k;
        // absent key
        assertEquals("init", map.compute("c", remap));
        assertEquals("init", map.get("c"));

        // present key with non‑null value
        assertEquals("initc", map.compute("c", remap));
        assertEquals("initc", map.get("c"));

        // remap returns null – entry removed
        BiFunction<String, String, String> remove = (k, v) -> null;
        assertNull(map.compute("c", remove));
        assertFalse(map.containsKey("c"));
    }

    /* -------------------------------------------------
     * merge
     * ------------------------------------------------- */
    @Test
    public void testMerge() {
        BiFunction<String, String, String> concat = (v1, v2) -> v1 + v2;

        // absent key – value stored directly
        assertEquals("first", map.merge("m", "first", concat));
        assertEquals("first", map.get("m"));

        // present key – remapping applied
        assertEquals("firstsecond", map.merge("m", "second", concat));
        assertEquals("firstsecond", map.get("m"));

        // remapping returns null – entry removed
        BiFunction<String, String, String> toNull = (v1, v2) -> null;
        assertNull(map.merge("m", "any", toNull));
        assertFalse(map.containsKey("m"));
    }

    /* -------------------------------------------------
     * values view
     * ------------------------------------------------- */
    @Test
    public void testValuesCollection() {
        map.put("k1", "v1");
        map.put(null, null);
        map.put("k2", "v2");

        Collection<String> values = map.values();
        assertEquals(3, values.size());
        assertTrue(values.contains("v1"));
        assertTrue(values.contains("v2"));
        assertTrue(values.contains(null));

        // iterator remove
        Iterator<String> it = values.iterator();
        while (it.hasNext()) {
            if ("v1".equals(it.next())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("k1"));
        assertEquals(2, map.size());
    }

    /* -------------------------------------------------
     * keySet view
     * ------------------------------------------------- */
    @Test
    public void testKeySet() {
        map.put("a", "1");
        map.put(null, "2");
        map.put("b", null);

        Set<String> keys = map.keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains(null));
        assertTrue(keys.contains("b"));

        // remove via keySet
        assertTrue(keys.remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(2, map.size());
    }

    /* -------------------------------------------------
     * entrySet view and entry mutation
     * ------------------------------------------------- */
    @Test
    public void testEntrySetAndEntryMutation() {
        map.put("x", "X");
        map.put(null, null);

        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(2, entries.size());

        // locate entry with key "x"
        Map.Entry<String, String> entryX = null;
        for (Map.Entry<String, String> e : entries) {
            if ("x".equals(e.getKey())) {
                entryX = e;
                break;
            }
        }
        assertNotNull(entryX);
        assertEquals("X", entryX.getValue());

        // mutate value via setValue
        assertEquals("X", entryX.setValue("newX"));
        assertEquals("newX", map.get("x"));

        // remove entry via entrySet.remove
        Map.Entry<String, String> nullEntry = null;
        for (Map.Entry<String, String> e : entries) {
            if (e.getKey() == null) {
                nullEntry = e;
                break;
            }
        }
        assertNotNull(nullEntry);
        assertTrue(entries.remove(nullEntry));
        assertFalse(map.containsKey(null));
    }

    /* -------------------------------------------------
     * equals, hashCode and toString
     * ------------------------------------------------- */
    @Test
    public void testEqualsHashCodeAndToString() {
        map.put("k1", "v1");
        map.put(null, null);
        map.put("k2", "v2");

        // Build a standard HashMap with same content
        Map<String, String> other = new HashMap<>();
        other.put("k1", "v1");
        other.put(null, null);
        other.put("k2", "v2");

        // Equality
        assertTrue(map.equals(other));
        assertTrue(other.equals(map));

        // Hash code consistency
        assertEquals(map.hashCode(), other.hashCode());

        // toString format – order is unspecified, so verify content pieces
        String s = map.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("k1=v1"));
        assertTrue(s.contains("k2=v2"));
        assertTrue(s.contains("null=null"));
    }

    /* -------------------------------------------------
     * equals with different size / mismatched entries
     * ------------------------------------------------- */
    @Test
    public void testEqualsNegativeCases() {
        map.put("a", "1");
        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        other.put("b", "2");
        assertFalse(map.equals(other));

        other.clear();
        other.put("a", "different");
        assertFalse(map.equals(other));

        assertFalse(map.equals("not a map"));
    }
}
