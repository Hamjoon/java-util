package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Test suite for {@link AbstractConcurrentNullSafeMap}.  Since the class is abstract we
 * provide a simple concrete implementation that delegates to a {@link ConcurrentHashMap}
 * as the internal map.  The tests cover all public methods, with special emphasis on
 * the handling of {@code null} keys and values via the sentinel objects.
 */
public class AbstractConcurrentNullSafeMapTest {

    /**
     * Simple concrete subclass used only for testing.  It does not add any behaviour;
     * it merely supplies the required constructor.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {
        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    private TestMap<String, String> map;

    @Before
    public void setUp() {
        map = new TestMap<>();
    }

    /* ---------------------------------------------------------------------- */
    /* Basic map operations (size, isEmpty, clear, put, get, remove, etc.)   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetNonNull() {
        assertNull(map.put("a", "1"));
        assertEquals("1", map.get("a"));
        assertEquals("1", map.put("a", "2"));
        assertEquals("2", map.get("a"));
    }

    @Test
    public void testPutAndGetNullKeyAndValue() {
        // null key
        assertNull(map.put(null, "value"));
        assertEquals("value", map.get(null));

        // null value
        assertNull(map.put("k", null));
        assertNull(map.get("k"));

        // both null
        assertNull(map.put(null, null));
        assertNull(map.get(null));
    }

    @Test
    public void testContainsKeyAndValue() {
        map.put(null, "vnullkey");
        map.put("knullvalue", null);
        map.put("both", null);
        map.put("normal", "val");

        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("knullvalue"));
        assertTrue(map.containsKey("both"));
        assertTrue(map.containsKey("normal"));

        assertTrue(map.containsValue(null));
        assertTrue(map.containsValue("vnullkey"));
        assertTrue(map.containsValue("val"));
    }

    @Test
    public void testSizeAndIsEmptyAndClear() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("a", "1");
        map.put(null, null);
        assertFalse(map.isEmpty());
        assertEquals(2, map.size());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testRemove() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);

        assertEquals("1", map.remove("a"));
        assertNull(map.remove("a")); // already removed

        assertEquals("nullKey", map.remove(null));
        assertNull(map.remove(null));

        assertNull(map.remove("b"));
        assertNull(map.get("b"));
    }

    @Test
    public void testPutAll() {
        Map<String, String> other = new AbstractMap<String, String>() {
            @Override
            public Set<Entry<String, String>> entrySet() {
                return Set.of(
                        Map.entry("x", "1"),
                        Map.entry(null, "nullKey"),
                        Map.entry("y", null)
                );
            }
        };
        map.putAll(other);
        assertEquals(3, map.size());
        assertEquals("1", map.get("x"));
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("y"));
    }

    @Test
    public void testGetOrDefault() {
        map.put("a", "1");
        assertEquals("1", map.getOrDefault("a", "def"));
        assertEquals("def", map.getOrDefault("missing", "def"));
        assertEquals("def", map.getOrDefault(null, "def"));
    }

    @Test
    public void testPutIfAbsent() {
        assertNull(map.putIfAbsent("k", "v"));
        assertEquals("v", map.putIfAbsent("k", "new"));
        assertEquals("v", map.get("k"));

        // null handling
        assertNull(map.putIfAbsent(null, null));
        assertNull(map.putIfAbsent(null, "ignored"));
        assertNull(map.get(null));
    }

    @Test
    public void testRemoveWithValue() {
        map.put("k", "v");
        assertFalse(map.remove("k", "wrong"));
        assertTrue(map.remove("k", "v"));
        assertFalse(map.remove("k", "v"));
    }

    @Test
    public void testReplaceConditional() {
        map.put("k", "v1");
        assertFalse(map.replace("k", "no", "v2"));
        assertTrue(map.replace("k", "v1", "v2"));
        assertEquals("v2", map.get("k"));

        // null handling
        map.put(null, null);
        assertTrue(map.replace(null, null, "now"));
        assertEquals("now", map.get(null));
    }

    @Test
    public void testReplaceUnconditional() {
        map.put("k", "old");
        assertEquals("old", map.replace("k", "new"));
        assertEquals("new", map.get("k"));

        // replace null value
        map.put("n", null);
        assertNull(map.replace("n", "filled"));
        assertEquals("filled", map.get("n"));
    }

    /* ---------------------------------------------------------------------- */
    /* computeIfAbsent, compute, merge */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeIfAbsent() {
        Function<String, String> mapping = k -> k == null ? "nullKey" : k.toUpperCase();

        // key absent
        assertEquals("A", map.computeIfAbsent("a", mapping));
        assertEquals("A", map.get("a"));

        // key present with non‑null value – mapping not called
        map.put("b", "B");
        assertEquals("B", map.computeIfAbsent("b", k -> {
            fail("Mapping function should not be invoked for existing non‑null value");
            return null;
        }));

        // key present with null value – mapping called
        map.put("c", null);
        assertEquals("C", map.computeIfAbsent("c", mapping));
        assertEquals("C", map.get("c"));

        // null key handling
        assertEquals("nullKey", map.computeIfAbsent(null, mapping));
        assertEquals("nullKey", map.get(null));
    }

    @Test
    public void testCompute() {
        BiFunction<String, String, String> remap = (k, v) -> {
            if (v == null) return k == null ? "nullKey" : k + "-new";
            return v + "-appended";
        };

        // absent key – old value null
        assertEquals("a-new", map.compute("a", remap));
        assertEquals("a-new", map.get("a"));

        // present non‑null value
        map.put("b", "old");
        assertEquals("old-appended", map.compute("b", remap));
        assertEquals("old-appended", map.get("b"));

        // present null value – should replace per function
        map.put("c", null);
        assertEquals("c-new", map.compute("c", remap));
        assertEquals("c-new", map.get("c"));

        // null key
        assertEquals("nullKey", map.compute(null, remap));
        assertEquals("nullKey", map.get(null));
    }

    @Test
    public void testMerge() {
        BiFunction<String, String, String> concat = (v1, v2) -> v1 + "+" + v2;

        // key absent – value stored
        assertEquals("first", map.merge("k", "first", concat));
        assertEquals("first", map.get("k"));

        // key present – remapping applied
        assertEquals("first+second", map.merge("k", "second", concat));
        assertEquals("first+second", map.get("k"));

        // null key handling
        assertEquals("nullVal", map.merge(null, "nullVal", concat));
        assertEquals("nullVal", map.get(null));
    }

    /* ---------------------------------------------------------------------- */
    /* Collection views (keySet, values, entrySet) */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testKeySetView() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);

        Set<String> ks = map.keySet();
        assertEquals(3, ks.size());
        assertTrue(ks.contains("a"));
        assertTrue(ks.contains(null));
        assertTrue(ks.contains("b"));

        // remove via iterator
        ks.remove("a");
        assertFalse(map.containsKey("a"));
        assertEquals(2, map.size());
    }

    @Test
    public void testValuesView() {
        map.put("a", "1");
        map.put(null, null);
        map.put("b", "2");

        Collection<String> vals = map.values();
        assertEquals(3, vals.size());
        assertTrue(vals.contains("1"));
        assertTrue(vals.contains(null));
        assertTrue(vals.contains("2"));

        // remove via iterator
        vals.remove(null);
        assertFalse(map.containsValue(null));
        assertEquals(2, map.size());
    }

    @Test
    public void testEntrySetView() {
        map.put("x", "1");
        map.put(null, "nullKey");
        map.put("y", null);

        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(3, entries.size());

        // verify contains
        assertTrue(entries.contains(Map.entry("x", "1")));
        assertTrue(entries.contains(Map.entry(null, "nullKey")));
        assertTrue(entries.contains(Map.entry("y", null)));

        // modify via setValue
        for (Map.Entry<String, String> e : entries) {
            if ("x".equals(e.getKey())) {
                e.setValue("changed");
            }
        }
        assertEquals("changed", map.get("x"));
    }

    /* ---------------------------------------------------------------------- */
    /* equals, hashCode, toString */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        map.put("a", "1");
        map.put(null, null);
        map.put("b", "2");

        Map<String, String> other = new java.util.HashMap<>();
        other.put("a", "1");
        other.put(null, null);
        other.put("b", "2");

        assertTrue(map.equals(other));
        assertTrue(other.equals(map));
        assertEquals(map.hashCode(), other.hashCode());

        // modify other
        other.put("c", "3");
        assertFalse(map.equals(other));
    }

    @Test
    public void testToString() {
        assertEquals("{}", map.toString());

        map.put("a", "1");
        map.put(null, null);
        map.put("self", null); // will be replaced by "(this Map)" in toString check

        // Build expected string manually (order is the iterator order of entrySet)
        StringBuilder expected = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!first) expected.append(", ");
            Object k = e.getKey() == map ? "(this Map)" : e.getKey();
            Object v = e.getValue() == map ? "(this Map)" : e.getValue();
            expected.append(k).append('=').append(v);
            first = false;
        }
        expected.append('}');

        assertEquals(expected.toString(), map.toString());
    }
}
