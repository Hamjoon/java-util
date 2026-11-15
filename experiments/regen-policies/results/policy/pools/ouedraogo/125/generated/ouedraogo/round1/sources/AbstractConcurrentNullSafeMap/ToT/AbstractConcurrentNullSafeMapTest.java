package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

public class AbstractConcurrentNullSafeMapTest {

    /**
     * Simple concrete implementation exposing the protected constructor.
     */
    private static class MyMap extends AbstractConcurrentNullSafeMap<String, String> {
        MyMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    private MyMap map;

    @Before
    public void setUp() {
        map = new MyMap();
    }

    /* -------------------- Basic Map Operations -------------------- */

    @Test
    public void testPutGetAndSize() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        // regular put
        assertNull(map.put("a", "1"));
        assertEquals("1", map.get("a"));
        assertEquals(1, map.size());

        // overwrite
        assertEquals("1", map.put("a", "2"));
        assertEquals("2", map.get("a"));
        assertEquals(1, map.size());

        // null key & null value
        assertNull(map.put(null, null));
        assertNull(map.get(null));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertEquals(2, map.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put("x", "y");
        map.put(null, "nullKey");
        map.put("nullVal", null);

        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey(null));
        assertFalse(map.containsKey("missing"));

        assertTrue(map.containsValue("y"));
        assertTrue(map.containsValue(null));
        assertFalse(map.containsValue("missing"));
    }

    @Test
    public void testRemove() {
        map.put("k1", "v1");
        map.put(null, "nullKey");
        map.put("k2", null);

        assertEquals("v1", map.remove("k1"));
        assertFalse(map.containsKey("k1"));
        assertNull(map.remove("k1")); // already removed

        assertEquals("nullKey", map.remove(null));
        assertFalse(map.containsKey(null));

        assertNull(map.remove("k2"));
        assertFalse(map.containsKey("k2"));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> src = new HashMap<>();
        src.put("a", "1");
        src.put(null, "nullKey");
        src.put("b", null);
        map.putAll(src);

        assertEquals(3, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("b"));

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetOrDefault() {
        map.put("present", "value");
        assertEquals("value", map.getOrDefault("present", "def"));
        assertEquals("def", map.getOrDefault("missing", "def"));
        // null default
        assertNull(map.getOrDefault("missing", null));
    }

    /* -------------------- ConcurrentMap Specific Operations -------------------- */

    @Test
    public void testPutIfAbsent() {
        assertNull(map.putIfAbsent("k", "v1"));
        assertEquals("v1", map.putIfAbsent("k", "v2")); // existing value returned
        assertEquals("v1", map.get("k"));
    }

    @Test
    public void testRemoveKeyValue() {
        map.put("k", "v");
        assertTrue(map.remove("k", "v"));
        assertFalse(map.remove("k", "v")); // already removed

        map.put(null, null);
        assertTrue(map.remove(null, null));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testReplaceKeyOldNew() {
        map.put("k", "old");
        assertTrue(map.replace("k", "old", "new"));
        assertEquals("new", map.get("k"));
        assertFalse(map.replace("k", "old", "new2")); // old no longer matches
        assertEquals("new", map.get("k"));
    }

    @Test
    public void testReplaceKeyValue() {
        map.put("k", "old");
        assertEquals("old", map.replace("k", "new"));
        assertEquals("new", map.get("k"));
        assertNull(map.replace("missing", "v"));
    }

    @Test
    public void testComputeIfAbsent() {
        // key absent, mapping returns non‑null
        Function<String, String> f1 = k -> "computed-" + k;
        assertEquals("computed-abc", map.computeIfAbsent("abc", f1));
        assertEquals("computed-abc", map.get("abc"));

        // key present with non‑null value, mapping not called
        Function<String, String> f2 = k -> {
            fail("Mapping function should not be called when value present");
            return null;
        };
        assertEquals("computed-abc", map.computeIfAbsent("abc", f2));

        // key present with null value – mapping called
        map.put("nullVal", null);
        Function<String, String> f3 = k -> "filled";
        assertEquals("filled", map.computeIfAbsent("nullVal", f3));
        assertEquals("filled", map.get("nullVal"));

        // mapping returns null – entry removed / not added
        map.put("removeMe", null);
        Function<String, String> f4 = k -> null;
        assertNull(map.computeIfAbsent("removeMe", f4));
        assertFalse(map.containsKey("removeMe"));
    }

    @Test
    public void testCompute() {
        // existing entry, remapping returns new value
        map.put("k", "v");
        BiFunction<String, String, String> bf1 = (k, v) -> v + "-new";
        assertEquals("v-new", map.compute("k", bf1));
        assertEquals("v-new", map.get("k"));

        // existing entry, remapping returns null – entry removed
        BiFunction<String, String, String> bf2 = (k, v) -> null;
        assertNull(map.compute("k", bf2));
        assertFalse(map.containsKey("k"));

        // absent entry, remapping creates new value
        BiFunction<String, String, String> bf3 = (k, v) -> "created";
        assertEquals("created", map.compute("newKey", bf3));
        assertEquals("created", map.get("newKey"));
    }

    @Test
    public void testMerge() {
        // simple merge where key absent
        BiFunction<String, String, String> concat = (v1, v2) -> v1 + "+" + v2;
        assertEquals("val", map.merge("k", "val", concat));
        assertEquals("val", map.get("k"));

        // key present, remapping combines
        assertEquals("val+more", map.merge("k", "more", concat));
        assertEquals("val+more", map.get("k"));

        // remapping returns null – entry removed
        BiFunction<String, String, String> toNull = (v1, v2) -> null;
        assertNull(map.merge("k", "ignored", toNull));
        assertFalse(map.containsKey("k"));

        // null remapping function should NPE
        try {
            map.merge("any", "v", null);
            fail("Expected NPE for null remapping function");
        } catch (NullPointerException expected) {
        }

        // null value argument should NPE (as per implementation)
        try {
            map.merge("any", null, concat);
            fail("Expected NPE for null value argument");
        } catch (NullPointerException expected) {
        }
    }

    /* -------------------- Collection Views -------------------- */

    @Test
    public void testValuesCollection() {
        map.put("a", "1");
        map.put(null, null);
        map.put("b", "2");

        // size & iteration
        assertEquals(3, map.values().size());
        assertTrue(map.values().contains("1"));
        assertTrue(map.values().contains(null));
        assertFalse(map.values().contains("missing"));

        // remove via iterator
        var it = map.values().iterator();
        while (it.hasNext()) {
            if (it.next() == null) {
                it.remove();
            }
        }
        assertFalse(map.containsValue(null));
        assertEquals(2, map.size());

        // clear via collection
        map.values().clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testKeySetCollection() {
        map.put("x", "1");
        map.put(null, "nullKey");
        map.put("y", null);

        // size & contains
        assertEquals(3, map.keySet().size());
        assertTrue(map.keySet().contains("x"));
        assertTrue(map.keySet().contains(null));
        assertTrue(map.keySet().contains("y"));
        assertFalse(map.keySet().contains("missing"));

        // remove via keySet
        assertTrue(map.keySet().remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(2, map.size());

        // iterator remove
        var it = map.keySet().iterator();
        while (it.hasNext()) {
            if ("x".equals(it.next())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());

        // clear via keySet
        map.keySet().clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testEntrySetCollection() {
        map.put("k1", "v1");
        map.put(null, null);
        map.put("k2", "v2");

        // size & contains
        assertEquals(3, map.entrySet().size());
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("k1", "v1");
        assertTrue(map.entrySet().contains(entry));

        // modify via setValue
        for (Map.Entry<String, String> e : map.entrySet()) {
            if ("k1".equals(e.getKey())) {
                e.setValue("v1-mod");
            }
        }
        assertEquals("v1-mod", map.get("k1"));

        // remove via entrySet
        assertTrue(map.entrySet().remove(new AbstractMap.SimpleEntry<>(null, null)));
        assertFalse(map.containsKey(null));
        assertEquals(2, map.size());

        // iterator remove
        var it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("k2".equals(e.getKey())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("k2"));
        assertEquals(1, map.size());

        // clear via entrySet
        map.entrySet().clear();
        assertTrue(map.isEmpty());
    }

    /* -------------------- Equality, HashCode & toString -------------------- */

    @Test
    public void testEqualsAndHashCode() {
        map.put("a", "1");
        map.put(null, null);
        map.put("b", "2");

        MyMap other = new MyMap();
        other.put("b", "2");
        other.put("a", "1");
        other.put(null, null);

        assertEquals(map, other);
        assertEquals(other, map);
        assertEquals(map.hashCode(), other.hashCode());

        other.put("c", "3");
        assertNotEquals(map, other);
        assertNotEquals(map.hashCode(), other.hashCode());

        // compare with a standard HashMap
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("a", "1");
        hashMap.put(null, null);
        hashMap.put("b", "2");
        assertEquals(map, hashMap);
        assertEquals(hashMap, map);
    }

    @Test
    public void testToString() {
        map.put("x", "1");
        map.put(null, null);
        map.put("y", "2");

        String s = map.toString();
        // Basic format checks – we rely on MapUtilities.mapToString producing
        // a string similar to standard Map#toString
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("x=1"));
        assertTrue(s.contains("y=2"));
        assertTrue(s.contains("null=null"));
        // The order is unspecified; just ensure commas separate entries
        int commas = s.length() - s.replace(",", "").length();
        assertTrue(commas >= 2);
    }

    /* -------------------- Concurrency‑related sanity (single‑threaded) -------------------- */

    @Test
    public void testConcurrentModificationExceptionOnValuesIterator() {
        map.put("a", "1");
        map.put("b", "2");
        var it = map.values().iterator();
        map.put("c", "3"); // structural change
        try {
            it.next();
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException expected) {
        }
    }

    @Test
    public void testConcurrentModificationExceptionOnKeySetIterator() {
        map.put("a", "1");
        var it = map.keySet().iterator();
        map.remove("a");
        try {
            it.next();
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException expected) {
        }
    }

    @Test
    public void testConcurrentModificationExceptionOnEntrySetIterator() {
        map.put("a", "1");
        var it = map.entrySet().iterator();
        map.clear();
        try {
            it.next();
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException expected) {
        }
    }
}
