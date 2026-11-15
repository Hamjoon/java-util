package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link AbstractConcurrentNullSafeMap}.
 *
 * The tests use a simple concrete subclass that delegates to a {@link ConcurrentHashMap}
 * as the internal map.  Every public method of {@code AbstractConcurrentNullSafeMap}
 * is exercised with typical values, edge‑cases (null keys/values), and error scenarios.
 */
public class AbstractConcurrentNullSafeMapTest {

    /** Concrete implementation used for testing. */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {
        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
    }

    private TestMap map;

    @Before
    public void setUp() {
        map = new TestMap(new ConcurrentHashMap<>());
    }

    /* ------------------- basic Map contract ------------------- */

    @Test
    public void testPutGetRemoveBasic() {
        assertNull(map.put("a", "alpha"));
        assertEquals("alpha", map.get("a"));
        assertEquals("alpha", map.remove("a"));
        assertNull(map.get("a"));
    }

    @Test
    public void testPutWithNullKeyAndValue() {
        // null key
        assertNull(map.put(null, "value"));
        assertEquals("value", map.get(null));
        // null value
        assertNull(map.put("key", null));
        assertNull(map.get("key"));
        // both null
        assertNull(map.put(null, null));
        assertNull(map.get(null));
    }

    @Test
    public void testContainsKeyAndValue() {
        map.put("k1", "v1");
        map.put(null, "nullKey");
        map.put("nullVal", null);
        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue("v1"));
        assertTrue(map.containsValue(null));
        assertFalse(map.containsKey("missing"));
        assertFalse(map.containsValue("missing"));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("x", "y");
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        other.put(null, "nullKey");
        other.put("b", null);
        map.putAll(other);

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
        // default when stored value is null
        map.put("nullVal", null);
        assertEquals("def", map.getOrDefault("nullVal", "def"));
    }

    @Test
    public void testPutIfAbsent() {
        assertNull(map.putIfAbsent("k", "v"));
        assertEquals("v", map.get("k"));
        // second call should not replace existing value
        assertEquals("v", map.putIfAbsent("k", "new"));
        assertEquals("v", map.get("k"));
    }

    @Test
    public void testRemoveKeyValuePair() {
        map.put("k", "v");
        assertTrue(map.remove("k", "v"));
        assertFalse(map.remove("k", "v")); // already removed
        // removal with mismatched value
        map.put("k2", "v2");
        assertFalse(map.remove("k2", "wrong"));
    }

    @Test
    public void testReplaceThreeArg() {
        map.put("k", "old");
        assertTrue(map.replace("k", "old", "new"));
        assertEquals("new", map.get("k"));
        // replace with wrong old value
        assertFalse(map.replace("k", "old", "x"));
        // replace non‑existing key
        assertFalse(map.replace("missing", "a", "b"));
    }

    @Test
    public void testReplaceOneArg() {
        map.put("k", "old");
        assertEquals("old", map.replace("k", "new"));
        assertEquals("new", map.get("k"));
        // replace on missing key returns null
        assertNull(map.replace("missing", "v"));
    }

    /* ------------------- compute / merge ------------------- */

    @Test
    public void testComputeIfAbsentWhenAbsent() {
        Function<String, String> mapping = k -> k.toUpperCase();
        assertEquals("ABC", map.computeIfAbsent("abc", mapping));
        // value now present, mapping should not be called again
        assertEquals("ABC", map.computeIfAbsent("abc", k -> { fail("Should not compute again"); return null; }));
    }

    @Test
    public void testComputeIfAbsentWhenMappingReturnsNull() {
        Function<String, String> mapping = k -> null;
        assertNull(map.computeIfAbsent("nullComp", mapping));
        assertFalse(map.containsKey("nullComp"));
    }

    @Test
    public void testComputeIfAbsentWithNullKey() {
        Function<String, String> mapping = k -> "val";
        assertEquals("val", map.computeIfAbsent(null, mapping));
        assertEquals("val", map.get(null));
    }

    @Test
    public void testComputeReplaceExisting() {
        map.put("k", "old");
        BiFunction<String, String, String> remap = (k, v) -> v + "+";
        assertEquals("old+", map.compute("k", remap));
        assertEquals("old+", map.get("k"));
    }

    @Test
    public void testComputeRemoveWhenFunctionReturnsNull() {
        map.put("k", "toRemove");
        BiFunction<String, String, String> remap = (k, v) -> null;
        assertNull(map.compute("k", remap));
        assertFalse(map.containsKey("k"));
    }

    @Test
    public void testMergeWithExistingKey() {
        map.put("k", "v1");
        BiFunction<String, String, String> remap = (v1, v2) -> v1 + v2;
        assertEquals("v1v2", map.merge("k", "v2", remap));
        assertEquals("v1v2", map.get("k"));
    }

    @Test
    public void testMergeWhenKeyAbsent() {
        BiFunction<String, String, String> remap = (v1, v2) -> fail("Should not be called");
        assertEquals("new", map.merge("newKey", "new", remap));
        assertEquals("new", map.get("newKey"));
    }

    @Test
    public void testMergeWhenRemappingReturnsNull() {
        map.put("k", "v1");
        BiFunction<String, String, String> remap = (v1, v2) -> null;
        assertNull(map.merge("k", "v2", remap));
        assertFalse(map.containsKey("k"));
    }

    @Test(expected = NullPointerException.class)
    public void testMergeRejectsNullRemappingFunction() {
        map.merge("k", "v", null);
    }

    @Test(expected = NullPointerException.class)
    public void testMergeRejectsNullValueArgument() {
        // According to the implementation, value may not be null
        map.merge("k", null, (a, b) -> a);
    }

    /* ------------------- collection views ------------------- */

    @Test
    public void testValuesCollection() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);
        Collection<String> vals = map.values();

        List<String> expected = Arrays.asList("1", "nullKey", null);
        assertTrue(vals.containsAll(expected));
        assertEquals(3, vals.size());

        // remove via iterator
        Iterator<String> it = vals.iterator();
        while (it.hasNext()) {
            if (Objects.equals(it.next(), "1")) {
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
        Set<String> keys = map.keySet();

        Set<String> expected = new HashSet<>(Arrays.asList("a", null));
        assertEquals(expected, keys);
        assertTrue(keys.contains(null));
        assertTrue(keys.remove("a"));
        assertFalse(map.containsKey("a"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySetOperations() {
        map.put("k1", "v1");
        map.put(null, "nullKey");
        Set<Map.Entry<String, String>> entries = map.entrySet();

        // verify contains
        Map.Entry<String, String> e1 = new AbstractMap.SimpleEntry<>("k1", "v1");
        Map.Entry<String, String> e2 = new AbstractMap.SimpleEntry<>(null, "nullKey");
        assertTrue(entries.contains(e1));
        assertTrue(entries.contains(e2));

        // modify via setValue
        for (Map.Entry<String, String> e : entries) {
            if ("k1".equals(e.getKey())) {
                e.setValue("v2");
            }
        }
        assertEquals("v2", map.get("k1"));

        // remove via entrySet.remove
        assertTrue(entries.remove(e2));
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());

        // clear via entrySet
        entries.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testEntrySetEqualsAndHashCode() {
        map.put("a", "1");
        map.put("b", "2");
        Set<Map.Entry<String, String>> set = map.entrySet();

        // Build an equivalent set using SimpleEntry
        Set<Map.Entry<String, String>> expected = new HashSet<>();
        expected.add(new AbstractMap.SimpleEntry<>("a", "1"));
        expected.add(new AbstractMap.SimpleEntry<>("b", "2"));

        assertEquals(expected, set);
        assertEquals(expected.hashCode(), set.hashCode());
    }

    /* ------------------- equals, hashCode, toString ------------------- */

    @Test
    public void testEqualsAndHashCode() {
        map.put("x", "1");
        map.put(null, null);
        Map<String, String> other = new HashMap<>();
        other.put("x", "1");
        other.put(null, null);

        assertEquals(other, map);
        assertEquals(map, other);
        assertEquals(other.hashCode(), map.hashCode());

        other.put("extra", "value");
        assertNotEquals(other, map);
    }

    @Test
    public void testToStringEmptyAndNonEmpty() {
        assertEquals("{}", map.toString());

        map.put("k1", "v1");
        map.put(null, "nullKey");
        map.put("self", null); // test value null

        String s = map.toString();
        // order is not guaranteed; verify substrings
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("k1=v1"));
        assertTrue(s.contains("null=nullKey"));
        // self key with null value should appear as "self=null"
        assertTrue(s.contains("self=null"));
    }

    @Test
    public void testToStringSelfReference() {
        // The map may contain itself as a key or value; test defensive handling
        map.put("selfKey", null);
        map.put("selfVal", null);
        // Insert the map itself as a key and as a value
        map.put("mapKey", null);
        map.put(null, null);
        // Using the map as a value
        map.put("valueIsMap", null);
        // No actual self‑reference in this implementation because nulls are masked,
        // but we can simulate by putting the map instance into the underlying map directly.
        map.internalMap.put("directKey", map);
        map.internalMap.put(map, "directValue");

        String s = map.toString();
        assertTrue(s.contains("(this Map)"));
    }
}
