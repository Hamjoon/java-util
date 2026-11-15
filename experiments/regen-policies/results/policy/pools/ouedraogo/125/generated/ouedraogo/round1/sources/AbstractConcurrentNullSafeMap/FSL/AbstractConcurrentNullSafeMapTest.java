package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link AbstractConcurrentNullSafeMap}.
 *
 * The tests use a concrete subclass that delegates to a {@link ConcurrentHashMap}
 * as the backing map.  All public behaviour, especially the handling of {@code null}
 * keys and values via the internal {@link NullSentinel}s, is exercised.
 */
public class AbstractConcurrentNullSafeMapTest {

    /** Simple concrete implementation for testing. */
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

    /* ---------------------------------------------------------------------- */
    /* Basic Map operations                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutGetAndSize() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        // regular put/get
        assertNull(map.put("a", "A"));
        assertEquals("A", map.get("a"));
        assertEquals(1, map.size());

        // null key and null value handling
        assertNull(map.put(null, "nullKey"));
        assertEquals("nullKey", map.get(null));

        assertNull(map.put("b", null));
        assertNull(map.get("b"));
        assertEquals(3, map.size()); // a, null key, b

        // replace existing value
        assertEquals("A", map.put("a", "AA"));
        assertEquals("AA", map.get("a"));
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put("x", "X");
        map.put(null, "nullKey");
        map.put("y", null);

        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey(null));
        assertFalse(map.containsKey("missing"));

        assertTrue(map.containsValue("X"));
        assertTrue(map.containsValue(null));
        assertFalse(map.containsValue("missing"));
    }

    @Test
    public void testRemoveAndClear() {
        map.put("k1", "v1");
        map.put(null, "nullKey");
        map.put("k2", null);

        assertEquals("v1", map.remove("k1"));
        assertNull(map.remove("k1")); // already removed

        assertEquals("nullKey", map.remove(null));
        assertNull(map.remove(null));

        assertEquals(1, map.size());
        assertEquals(null, map.get("k2")); // still there (null value)

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testPutAll() {
        Map<String, String> other = new HashMap<>();
        other.put("a", "A");
        other.put(null, "nullKey");
        other.put("b", null);

        map.putAll(other);
        assertEquals(3, map.size());
        assertEquals("A", map.get("a"));
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("b"));
    }

    @Test
    public void testGetOrDefault() {
        map.put("present", "value");
        assertEquals("value", map.getOrDefault("present", "def"));
        assertEquals("def", map.getOrDefault("missing", "def"));
        // null mapping
        map.put("nullVal", null);
        assertEquals(null, map.getOrDefault("nullVal", "def"));
    }

    @Test
    public void testPutIfAbsent() {
        assertNull(map.putIfAbsent("k", "v"));
        assertEquals("v", map.get("k"));
        assertEquals("v", map.putIfAbsent("k", "new"));
        // null key/value
        assertNull(map.putIfAbsent(null, "nullKey"));
        assertEquals("nullKey", map.get(null));
        assertNull(map.putIfAbsent(null, "ignored"));
    }

    @Test
    public void testRemoveKeyValue() {
        map.put("k", "v");
        map.put(null, "nullKey");
        map.put("n", null);

        assertTrue(map.remove("k", "v"));
        assertFalse(map.remove("k", "v")); // already removed

        // null value case
        assertTrue(map.remove("n", null));
        assertFalse(map.remove("n", null));

        // null key case
        assertTrue(map.remove(null, "nullKey"));
        assertFalse(map.remove(null, "nullKey"));
    }

    @Test
    public void testReplaceMethods() {
        map.put("k", "v1");
        map.put(null, "nullKey");
        map.put("n", null);

        // conditional replace
        assertTrue(map.replace("k", "v1", "v2"));
        assertEquals("v2", map.get("k"));
        assertFalse(map.replace("k", "v1", "v3")); // old value no longer matches

        // replace with null old value
        assertTrue(map.replace("n", null, "new"));
        assertEquals("new", map.get("n"));

        // replace(key, value) – unconditional
        assertEquals("new", map.replace("n", "final"));
        assertEquals("final", map.get("n"));

        // null key replace
        assertTrue(map.replace(null, "nullKey", "newNull"));
        assertEquals("newNull", map.get(null));
    }

    /* ---------------------------------------------------------------------- */
    /* computeIfAbsent, compute, merge                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeIfAbsentWhenAbsent() {
        Function<String, String> mapping = k -> k.toUpperCase();
        assertEquals("A", map.computeIfAbsent("a", mapping));
        assertEquals("A", map.get("a"));
    }

    @Test
    public void testComputeIfAbsentWhenPresentNonNull() {
        map.put("a", "A");
        Function<String, String> mapping = k -> "B";
        assertEquals("A", map.computeIfAbsent("a", mapping)); // should not recompute
    }

    @Test
    public void testComputeIfAbsentWhenPresentNull() {
        map.put("a", null);
        Function<String, String> mapping = k -> "computed";
        assertEquals("computed", map.computeIfAbsent("a", mapping));
        assertEquals("computed", map.get("a"));
    }

    @Test
    public void testComputeIfAbsentMappingReturnsNull() {
        Function<String, String> mapping = k -> null;
        assertNull(map.computeIfAbsent("missing", mapping));
        assertFalse(map.containsKey("missing"));
    }

    @Test
    public void testComputeReplaceAndRemove() {
        // initial null mapping
        map.put("k", null);
        BiFunction<String, String, String> remap = (k, v) -> v == null ? "first" : null;

        // first compute – replaces null with "first"
        assertEquals("first", map.compute("k", remap));
        assertEquals("first", map.get("k"));

        // second compute – returns null -> entry removed
        assertNull(map.compute("k", remap));
        assertFalse(map.containsKey("k"));
    }

    @Test
    public void testMergeWithExistingValue() {
        map.put("k", "old");
        BiFunction<String, String, String> remap = (v1, v2) -> v1 + v2;

        assertEquals("oldnew", map.merge("k", "new", remap));
        assertEquals("oldnew", map.get("k"));
    }

    @Test
    public void testMergeWhenNoExistingValue() {
        BiFunction<String, String, String> remap = (v1, v2) -> v1 + v2;
        assertEquals("new", map.merge("k", "new", remap));
        assertEquals("new", map.get("k"));
    }

    @Test
    public void testMergeRemappingReturnsNullRemovesEntry() {
        map.put("k", "old");
        BiFunction<String, String, String> remap = (v1, v2) -> null;
        assertNull(map.merge("k", "new", remap));
        assertFalse(map.containsKey("k"));
    }

    /* ---------------------------------------------------------------------- */
    /* Collection views: values, keySet, entrySet                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testValuesCollection() {
        map.put("a", "A");
        map.put(null, "nullKey");
        map.put("b", null);

        Set<String> expected = Set.of("A", "nullKey", null);
        assertEquals(3, map.values().size());
        assertTrue(map.values().containsAll(expected));

        // remove via iterator
        map.values().remove("A");
        assertFalse(map.containsKey("a"));
        assertEquals(2, map.size());

        // clear via collection
        map.values().clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testKeySetCollection() {
        map.put("a", "A");
        map.put(null, "nullKey");
        map.put("b", null);

        Set<String> expected = Set.of("a", null, "b");
        assertEquals(3, map.keySet().size());
        assertTrue(map.keySet().containsAll(expected));

        // remove a key
        assertTrue(map.keySet().remove("a"));
        assertFalse(map.containsKey("a"));
        assertEquals(2, map.size());

        // iterator remove
        java.util.Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            if (it.next() == null) {
                it.remove();
                break;
            }
        }
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());

        // clear via set
        map.keySet().clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testEntrySetCollection() {
        map.put("x", "X");
        map.put(null, "nullKey");
        map.put("y", null);

        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertEquals(3, entrySet.size());

        // verify contains
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("x", "X")));
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>(null, "nullKey")));
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("y", null)));

        // modify via setValue
        for (Map.Entry<String, String> e : entrySet) {
            if ("x".equals(e.getKey())) {
                e.setValue("XX");
            }
        }
        assertEquals("XX", map.get("x"));

        // remove an entry
        assertTrue(entrySet.remove(new AbstractMap.SimpleEntry<>(null, "nullKey")));
        assertFalse(map.containsKey(null));
        assertEquals(2, map.size());

        // iterator remove
        java.util.Iterator<Map.Entry<String, String>> it = entrySet.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("y".equals(e.getKey())) {
                it.remove();
                break;
            }
        }
        assertFalse(map.containsKey("y"));
        assertEquals(1, map.size());

        // clear via entrySet
        entrySet.clear();
        assertTrue(map.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* equals, hashCode, toString                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        map.put("a", "A");
        map.put(null, null);
        map.put("b", null);

        Map<String, String> other = new HashMap<>();
        other.put("a", "A");
        other.put(null, null);
        other.put("b", null);

        assertTrue(map.equals(other));
        assertTrue(other.equals(map));
        assertEquals(map.hashCode(), other.hashCode());

        // different map
        other.put("c", "C");
        assertFalse(map.equals(other));
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("k1", "v1");
        map.put(null, "nullKey");
        map.put("k2", null);

        String s = map.toString();

        // The exact order is not guaranteed, but all pairs must appear.
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("k1=v1"));
        assertTrue(s.contains("null=nullKey"));
        assertTrue(s.contains("k2=null"));
    }
}
