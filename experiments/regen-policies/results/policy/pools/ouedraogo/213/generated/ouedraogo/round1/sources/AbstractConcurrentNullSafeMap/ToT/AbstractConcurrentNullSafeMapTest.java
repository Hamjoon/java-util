package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.ConcurrentMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link AbstractConcurrentNullSafeMap}.
 *
 * <p>
 * The abstract class is exercised via a minimal concrete subclass that
 * delegates to a {@link ConcurrentHashMap}.  All public methods are covered,
 * including typical usage, edge cases (null keys/values), and error
 * scenarios (e.g. {@code null} arguments that must cause {@link NullPointerException}).
 * </p>
 */
public class AbstractConcurrentNullSafeMapTest {

    /** Simple concrete implementation used for testing. */
    private static final class TestMap extends AbstractConcurrentNullSafeMap<String, String> {
        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    private TestMap map;

    @Before
    public void setUp() {
        map = new TestMap();
    }

    /* --------------------------------------------------------------------- *
     *  Basic Map contract (size, isEmpty, clear, put, get, remove, etc.)    *
     * --------------------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("a", "1");
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        map.put(null, "nullKey");
        map.put("nullValue", null);
        assertEquals(3, map.size());
    }

    @Test
    public void testPutGetRemove() {
        // regular entry
        assertNull(map.put("k1", "v1"));
        assertEquals("v1", map.get("k1"));
        assertEquals("v1", map.remove("k1"));
        assertNull(map.get("k1"));

        // null key
        assertNull(map.put(null, "nullKey"));
        assertEquals("nullKey", map.get(null));
        assertEquals("nullKey", map.remove(null));
        assertNull(map.get(null));

        // null value
        assertNull(map.put("k2", null));
        assertNull(map.get("k2"));
        assertNull(map.remove("k2"));
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);

        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey(null));
        assertFalse(map.containsKey("missing"));

        assertTrue(map.containsValue("1"));
        assertTrue(map.containsValue(null));
        assertTrue(map.containsValue("nullKey"));
        assertFalse(map.containsValue("missing"));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> src = new HashMap<>();
        src.put("x", "10");
        src.put(null, "nullKey2");
        src.put("y", null);

        map.putAll(src);
        assertEquals(3, map.size());
        assertEquals("10", map.get("x"));
        assertEquals("nullKey2", map.get(null));
        assertNull(map.get("y"));

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetOrDefault() {
        map.put("a", "1");
        assertEquals("1", map.getOrDefault("a", "default"));
        assertEquals("default", map.getOrDefault("missing", "default"));
        assertEquals("default", map.getOrDefault(null, "default")); // no mapping yet

        map.put(null, null);
        assertNull(map.getOrDefault(null, "default"));
    }

    /* --------------------------------------------------------------------- *
     *  ConcurrentMap specific methods                                      *
     * --------------------------------------------------------------------- */

    @Test
    public void testPutIfAbsent() {
        assertNull(map.putIfAbsent("k", "v"));
        assertEquals("v", map.get("k"));
        // second call should not replace
        assertEquals("v", map.putIfAbsent("k", "new"));
        assertEquals("v", map.get("k"));

        // null key/value handling
        assertNull(map.putIfAbsent(null, "nullKey"));
        assertEquals("nullKey", map.get(null));

        assertNull(map.putIfAbsent("nullVal", null));
        assertNull(map.get("nullVal"));
    }

    @Test
    public void testRemoveKeyValue() {
        map.put("a", "1");
        assertFalse(map.remove("a", "wrong"));
        assertTrue(map.remove("a", "1"));
        assertFalse(map.containsKey("a"));

        // null handling
        map.put(null, "nullKey");
        assertTrue(map.remove(null, "nullKey"));
        assertFalse(map.containsKey(null));

        map.put("b", null);
        assertTrue(map.remove("b", null));
        assertFalse(map.containsKey("b"));
    }

    @Test
    public void testReplaceKeyOldNew() {
        map.put("k", "old");
        assertTrue(map.replace("k", "old", "new"));
        assertEquals("new", map.get("k"));

        assertFalse(map.replace("k", "old", "new2"));
        assertEquals("new", map.get("k"));

        // null handling
        map.put(null, "nullKey");
        assertTrue(map.replace(null, "nullKey", null));
        assertNull(map.get(null));

        map.put("nullVal", null);
        assertTrue(map.replace("nullVal", null, "now"));
        assertEquals("now", map.get("nullVal"));
    }

    @Test
    public void testReplaceKeyValue() {
        map.put("k", "old");
        assertEquals("old", map.replace("k", "new"));
        assertEquals("new", map.get("k"));

        // key absent => null
        assertNull(map.replace("missing", "any"));
        assertNull(map.get("missing"));

        // null handling
        map.put(null, "nullKey");
        assertEquals("nullKey", map.replace(null, null));
        assertNull(map.get(null));
    }

    @Test
    public void testComputeIfAbsent() {
        // compute when absent
        Function<String, String> mapping = k -> k.toUpperCase();
        assertEquals("ABC", map.computeIfAbsent("abc", mapping));
        assertEquals("ABC", map.get("abc"));

        // compute when present (non‑null) – mapping should NOT be called
        Function<String, String> fail = k -> { fail("Mapping function should not be invoked"); return null; };
        assertEquals("ABC", map.computeIfAbsent("abc", fail));

        // key present with null value – mapping should be invoked
        map.put("nullVal", null);
        assertEquals("CALC", map.computeIfAbsent("nullVal", k -> "CALC"));
        assertEquals("CALC", map.get("nullVal"));

        // mapping returns null – entry should be removed
        map.put("toRemove", "x");
        assertNull(map.computeIfAbsent("toRemove", k -> null));
        assertFalse(map.containsKey("toRemove"));
    }

    @Test
    public void testCompute() {
        BiFunction<String, String, String> concat = (k, v) -> (v == null ? "" : v) + k;
        map.put("a", "1");
        assertEquals("1a", map.compute("a", concat));
        assertEquals("1a", map.get("a"));

        // compute on missing key – oldValue is null
        assertEquals("b", map.compute("b", (k, v) -> k));
        assertEquals("b", map.get("b"));

        // compute returning null removes entry
        map.put("c", "cVal");
        assertNull(map.compute("c", (k, v) -> null));
        assertFalse(map.containsKey("c"));

        // null key handling
        map.put(null, "nullKey");
        assertEquals("nullKeynull", map.compute(null, (k, v) -> v + "null"));
        assertEquals("nullKeynull", map.get(null));
    }

    @Test
    public void testMerge() {
        BiFunction<String, String, String> concat = (v1, v2) -> v1 + v2;

        // simple merge when key absent
        assertEquals("val", map.merge("k", "val", concat));
        assertEquals("val", map.get("k"));

        // merge when key present
        assertEquals("valX", map.merge("k", "X", concat));
        assertEquals("valX", map.get("k"));

        // merge where remapping returns null – entry removed
        assertNull(map.merge("k", "ignored", (v1, v2) -> null));
        assertFalse(map.containsKey("k"));

        // null key handling
        assertEquals("nullKey", map.merge(null, "nullKey", concat));
        assertEquals("nullKey", map.get(null));
    }

    @Test(expected = NullPointerException.class)
    public void testMergeNullRemappingFunction() {
        map.merge("k", "v", null);
    }

    @Test(expected = NullPointerException.class)
    public void testMergeNullValueNotAllowed() {
        // The implementation calls Objects.requireNonNull(value);
        map.merge("k", null, (v1, v2) -> "x");
    }

    /* --------------------------------------------------------------------- *
     *  Collection views (values, keySet, entrySet)                         *
     * --------------------------------------------------------------------- */

    @Test
    public void testValuesCollection() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);

        Collection<String> values = map.values();
        assertEquals(3, values.size());
        assertTrue(values.contains("1"));
        assertTrue(values.contains("nullKey"));
        assertTrue(values.contains(null));

        // iterator remove
        Iterator<String> it = values.iterator();
        while (it.hasNext()) {
            if ("1".equals(it.next())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("a"));
        assertEquals(2, map.size());
    }

    @Test
    public void testKeySetCollection() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);

        Set<String> keys = map.keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains(null));
        assertTrue(keys.contains("b"));

        // contains & remove
        assertTrue(keys.contains("a"));
        assertTrue(keys.remove("a"));
        assertFalse(map.containsKey("a"));
        assertEquals(2, keys.size());

        // iterator remove for null key
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            if (it.next() == null) {
                it.remove();
            }
        }
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetCollection() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);

        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(3, entries.size());

        // verify entry content
        Map<String, String> expected = new HashMap<>();
        expected.put("a", "1");
        expected.put(null, "nullKey");
        expected.put("b", null);
        for (Map.Entry<String, String> e : entries) {
            assertEquals(expected.get(e.getKey()), e.getValue());
        }

        // test setValue
        for (Map.Entry<String, String> e : entries) {
            if ("a".equals(e.getKey())) {
                assertEquals("1", e.setValue("new"));
                assertEquals("new", map.get("a"));
            }
        }

        // test remove via entrySet
        Map.Entry<String, String> toRemove = new AbstractMap.SimpleEntry<>(null, "nullKey");
        assertTrue(entries.remove(toRemove));
        assertFalse(map.containsKey(null));
        assertEquals(2, entries.size());

        // iterator remove
        Iterator<Map.Entry<String, String>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("b".equals(e.getKey())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("b"));
        assertEquals(1, map.size());
    }

    /* --------------------------------------------------------------------- *
     *  Equality, hashCode, toString                                         *
     * --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        map.put("x", "10");
        map.put(null, null);
        map.put("y", "20");

        Map<String, String> other = new HashMap<>();
        other.put("x", "10");
        other.put(null, null);
        other.put("y", "20");

        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());

        // Different map
        other.put("z", "30");
        assertFalse(map.equals(other));
        assertNotEquals(map.hashCode(), other.hashCode());

        // Not a map
        assertFalse(map.equals("someString"));
    }

    @Test
    public void testToString() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);

        String s = map.toString();

        // The exact order is unspecified, but each entry must appear as key=value
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        // Ensure each mapping is present (order‑independent)
        List<String> parts = new ArrayList<>();
        for (String part : s.substring(1, s.length() - 1).split(",\\s*")) {
            parts.add(part);
        }
        Collections.sort(parts);
        List<String> expected = new ArrayList<>();
        expected.add("a=1");
        expected.add("b=null");
        expected.add("null=nullKey");
        Collections.sort(expected);
        assertEquals(expected, parts);
    }

    /* --------------------------------------------------------------------- *
     *  Additional sanity checks for internal masking logic (indirect)        *
     * --------------------------------------------------------------------- */

    @Test
    public void testMaskUnmaskThroughPublicApi() {
        // Directly test that null keys/values survive a round‑trip via public methods
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));

        // Ensure that after removing, internal map is empty
        map.remove(null);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testConcurrentModificationBehavior() {
        // Ensure that the collection views are backed by the map
        map.put("a", "1");
        map.put("b", "2");

        Set<String> keys = map.keySet();
        Collection<String> values = map.values();

        map.put("c", "3"); // modify underlying map

        // Views should reflect the new state
        assertTrue(keys.contains("c"));
        assertTrue(values.contains("3"));
        assertEquals(3, keys.size());
        assertEquals(3, values.size());
    }
}
