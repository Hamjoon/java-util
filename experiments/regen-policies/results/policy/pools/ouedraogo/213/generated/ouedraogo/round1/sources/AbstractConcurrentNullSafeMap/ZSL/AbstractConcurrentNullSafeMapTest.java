package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

public class AbstractConcurrentNullSafeMapTest {

    /**
     * Simple concrete implementation of the abstract class using a ConcurrentHashMap as the backing store.
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

    @Test
    public void testBasicPutGetRemoveAndSize() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        // put normal entry
        assertNull(map.put("a", "alpha"));
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertEquals("alpha", map.get("a"));

        // put null key and null value
        assertNull(map.put(null, null));
        assertEquals(2, map.size());
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));

        // replace existing value
        assertEquals("alpha", map.put("a", "beta"));
        assertEquals("beta", map.get("a"));

        // remove entries
        assertEquals("beta", map.remove("a"));
        assertNull(map.get("a"));
        assertEquals(1, map.size());

        assertNull(map.remove(null));
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put("k1", "v1");
        map.put(null, "nullKey");
        map.put("k2", null);

        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("k2"));

        assertTrue(map.containsValue("v1"));
        assertTrue(map.containsValue("nullKey"));
        assertTrue(map.containsValue(null));
        assertFalse(map.containsValue("missing"));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> src = new HashMap<>();
        src.put("x", "1");
        src.put(null, "nullKey");
        src.put("y", null);
        map.putAll(src);

        assertEquals(3, map.size());
        assertEquals("1", map.get("x"));
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("y"));

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetOrDefault() {
        map.put("exists", "value");
        assertEquals("value", map.getOrDefault("exists", "default"));
        assertEquals("default", map.getOrDefault("missing", "default"));
        // null key handling
        assertEquals("default", map.getOrDefault(null, "default"));
        map.put(null, "nullVal");
        assertEquals("nullVal", map.getOrDefault(null, "default"));
    }

    @Test
    public void testPutIfAbsent() {
        assertNull(map.putIfAbsent("k", "v"));
        assertEquals("v", map.get("k"));
        // subsequent call should not replace
        assertEquals("v", map.putIfAbsent("k", "new"));
        assertEquals("v", map.get("k"));

        // null key/value handling
        assertNull(map.putIfAbsent(null, null));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.putIfAbsent(null, "ignored"));
        assertNull(map.get(null));
    }

    @Test
    public void testRemoveKeyValue() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);

        assertTrue(map.remove("a", "1"));
        assertFalse(map.containsKey("a"));

        assertTrue(map.remove(null, "nullKey"));
        assertFalse(map.containsKey(null));

        // removing entry with null value
        assertTrue(map.remove("b", null));
        assertFalse(map.containsKey("b"));
    }

    @Test
    public void testReplace() {
        map.put("k", "old");
        assertTrue(map.replace("k", "old", "new"));
        assertEquals("new", map.get("k"));

        // replace with wrong old value
        assertFalse(map.replace("k", "old", "another"));
        assertEquals("new", map.get("k"));

        // replace without old value
        assertEquals("new", map.replace("k", "final"));
        assertEquals("final", map.get("k"));

        // null key/value
        map.put(null, null);
        assertTrue(map.replace(null, null, "now"));
        assertEquals("now", map.get(null));

        assertTrue(map.replace(null, "now", null));
        assertNull(map.get(null));
    }

    @Test
    public void testComputeIfAbsent() {
        // key not present, mapping returns non-null
        Function<String, String> mapper = k -> k.toUpperCase();
        assertEquals("ABC", map.computeIfAbsent("abc", mapper));
        assertEquals("ABC", map.get("abc"));

        // key present with non-null value, mapper should not be called
        final boolean[] called = {false};
        Function<String, String> shouldNotCall = k -> {
            called[0] = true;
            return "X";
        };
        assertEquals("ABC", map.computeIfAbsent("abc", shouldNotCall));
        assertFalse(called[0]);

        // key present with null value, mapper should be called
        map.put("nullKey", null);
        called[0] = false;
        assertEquals("VALUE", map.computeIfAbsent("nullKey", k -> "VALUE"));
        assertTrue(called[0]);
        assertEquals("VALUE", map.get("nullKey"));

        // mapper returns null -> entry removed
        map.put("removeMe", null);
        assertNull(map.computeIfAbsent("removeMe", k -> null));
        assertFalse(map.containsKey("removeMe"));
    }

    @Test
    public void testCompute() {
        map.put("c", "1");
        BiFunction<String, String, String> remapper = (k, v) -> v == null ? "init" : v + k;
        assertEquals("11c", map.compute("c", remapper));
        assertEquals("11c", map.get("c"));

        // compute on missing key
        assertEquals("init", map.compute("newKey", remapper));
        assertEquals("init", map.get("newKey"));

        // compute returning null removes mapping
        assertNull(map.compute("newKey", (k, v) -> null));
        assertFalse(map.containsKey("newKey"));
    }

    @Test
    public void testMerge() {
        // initial merge where key absent
        BiFunction<String, String, String> concat = (v1, v2) -> v1 + v2;
        assertEquals("A", map.merge("m", "A", concat));
        assertEquals("A", map.get("m"));

        // merge when key present
        assertEquals("AB", map.merge("m", "B", concat));
        assertEquals("AB", map.get("m"));

        // merge where remapping returns null -> entry removed
        BiFunction<String, String, String> returnNull = (v1, v2) -> null;
        assertNull(map.merge("m", "C", returnNull));
        assertFalse(map.containsKey("m"));
    }

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

        // remove via iterator
        Iterator<String> it = values.iterator();
        while (it.hasNext()) {
            if ("v1".equals(it.next())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("k1"));
        assertEquals(2, map.size());
    }

    @Test
    public void testKeySet() {
        map.put("a", "1");
        map.put(null, "nullKey");
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

        // test iterator remove
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            if ("a".equals(it.next())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySet() {
        map.put("x", "X");
        map.put(null, null);
        map.put("y", "Y");

        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(3, entries.size());

        // verify entry contents
        for (Map.Entry<String, String> e : entries) {
            if (e.getKey() == null) {
                assertNull(e.getValue());
            } else if ("x".equals(e.getKey())) {
                assertEquals("X", e.getValue());
            } else if ("y".equals(e.getKey())) {
                assertEquals("Y", e.getValue());
            }
        }

        // modify value via setValue
        for (Map.Entry<String, String> e : entries) {
            if ("x".equals(e.getKey())) {
                e.setValue("newX");
            }
        }
        assertEquals("newX", map.get("x"));

        // remove entry via entrySet.remove
        Map.Entry<String, String> toRemove = new AbstractMap.SimpleEntry<>("y", "Y");
        assertTrue(entries.remove(toRemove));
        assertFalse(map.containsKey("y"));
        assertEquals(2, map.size());
    }

    @Test
    public void testEqualsAndHashCode() {
        map.put("a", "1");
        map.put(null, null);
        map.put("b", null);

        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        other.put(null, null);
        other.put("b", null);

        assertTrue(map.equals(other));
        assertTrue(other.equals(map));
        assertEquals(map.hashCode(), other.hashCode());

        // change other map
        other.put("c", "3");
        assertFalse(map.equals(other));
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("k1", "v1");
        map.put(null, null);
        map.put("k2", null);

        String s = map.toString();
        assertNotNull(s);
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        // order is not guaranteed; verify presence of each entry substring
        assertTrue(s.contains("k1=v1"));
        assertTrue(s.contains("null=null"));
        assertTrue(s.contains("k2=null"));
    }
}
