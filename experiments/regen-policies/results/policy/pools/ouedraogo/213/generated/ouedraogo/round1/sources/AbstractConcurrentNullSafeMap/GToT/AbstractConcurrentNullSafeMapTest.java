package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Concrete implementation of {@link AbstractConcurrentNullSafeMap} used only for testing.
 */
class TestConcurrentNullSafeMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {
    public TestConcurrentNullSafeMap() {
        super(new ConcurrentHashMap<>());
    }

    // expose internal map for verification if needed
    ConcurrentMap<Object, Object> getInternalMap() {
        return internalMap;
    }
}

/**
 * JUnit 4 test suite for {@link AbstractConcurrentNullSafeMap}.
 */
public class AbstractConcurrentNullSafeMapTest {

    private TestConcurrentNullSafeMap<String, String> map;

    @Before
    public void setUp() {
        map = new TestConcurrentNullSafeMap<>();
    }

    /* ----------------------------------------------------------------------
     * Basic public API tests (one test per method, covering normal behaviour)
     * ---------------------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());

        map.put("a", "1");
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
    }

    @Test
    public void testContainsKey() {
        map.put(null, "nullKey");
        map.put("x", "y");
        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("x"));
        assertFalse(map.containsKey("missing"));
    }

    @Test
    public void testContainsValue() {
        map.put("k1", null);
        map.put("k2", "v2");
        assertTrue(map.containsValue(null));
        assertTrue(map.containsValue("v2"));
        assertFalse(map.containsValue("absent"));
    }

    @Test
    public void testGet() {
        map.put(null, "nullKey");
        map.put("k", null);
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("k"));
        assertNull(map.get("missing"));
    }

    @Test
    public void testPutAndRemove() {
        assertNull(map.put("a", "1"));
        assertEquals("1", map.put("a", "2"));
        assertEquals("2", map.get("a"));
        assertEquals("2", map.remove("a"));
        assertNull(map.get("a"));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> other = new HashMap<>();
        other.put("x", "1");
        other.put(null, null);
        map.putAll(other);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetOrDefault() {
        map.put("a", null);
        assertEquals("def", map.getOrDefault("missing", "def"));
        assertEquals("def", map.getOrDefault("a", "def"));
        assertNull(map.getOrDefault("a", null));
    }

    @Test
    public void testPutIfAbsent() {
        assertNull(map.putIfAbsent("k", null));
        assertNull(map.get("k"));
        assertEquals("v", map.putIfAbsent("k", "v"));
        assertNull(map.get("k"));
    }

    @Test
    public void testRemoveKeyValue() {
        map.put("k", "v");
        assertFalse(map.remove("k", "wrong"));
        assertTrue(map.remove("k", "v"));
        assertFalse(map.remove(null, null)); // not present
    }

    @Test
    public void testReplaceThreeArg() {
        map.put("k", "v1");
        assertFalse(map.replace("k", "wrong", "v2"));
        assertTrue(map.replace("k", "v1", "v2"));
        assertEquals("v2", map.get("k"));
        // null handling
        map.put(null, null);
        assertTrue(map.replace(null, null, "notNull"));
        assertEquals("notNull", map.get(null));
    }

    @Test
    public void testReplaceTwoArg() {
        map.put("k", "v1");
        assertEquals("v1", map.replace("k", "v2"));
        assertEquals("v2", map.get("k"));
        // replace absent key returns null
        assertNull(map.replace("missing", "x"));
    }

    @Test
    public void testComputeIfAbsent() {
        Function<String, String> mapping = s -> s == null ? "nullKey" : s.toUpperCase();

        // key absent
        assertEquals("A", map.computeIfAbsent("a", mapping));
        // key present with non‑null value, mapping not called again
        assertEquals("A", map.computeIfAbsent("a", k -> fail("Mapping should not be called")));

        // null key handling
        assertEquals("nullKey", map.computeIfAbsent(null, mapping));
        // key mapped to null -> compute
        map.put("b", null);
        assertEquals("B", map.computeIfAbsent("b", mapping));
    }

    @Test
    public void testCompute() {
        BiFunction<String, String, String> remap = (k, v) -> (v == null) ? "init" : v + k;

        // absent key
        assertEquals("init", map.compute("x", remap));
        // present key
        assertEquals("initx", map.compute("x", remap));
        // null key handling
        assertEquals("init", map.compute(null, remap));
        assertEquals("initnull", map.compute(null, remap));
        // returning null removes mapping
        assertNull(map.compute("x", (k, v) -> null));
        assertFalse(map.containsKey("x"));
    }

    @Test
    public void testMerge() {
        BiFunction<String, String, String> concat = (v1, v2) -> v1 + v2;

        // absent key
        assertEquals("v1", map.merge("k", "v1", concat));
        // present key
        assertEquals("v1v2", map.merge("k", "v2", concat));

        // null key handling
        assertEquals("nullV", map.merge(null, "nullV", concat));
        assertEquals("nullVnullV2", map.merge(null, "nullV2", concat));
    }

    @Test
    public void testValuesCollection() {
        map.put("a", "1");
        map.put(null, null);
        Set<String> values = Collections.unmodifiableSet(Set.copyOf(map.values()));
        assertTrue(values.contains("1"));
        assertTrue(values.contains(null));
        assertEquals(2, values.size());

        // iterator remove
        var it = map.values().iterator();
        while (it.hasNext()) {
            if (it.next() == null) {
                it.remove();
            }
        }
        assertFalse(map.containsValue(null));
    }

    @Test
    public void testKeySet() {
        map.put("a", "1");
        map.put(null, "nullVal");
        Set<String> keys = map.keySet();
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains(null));
        assertEquals(2, keys.size());

        // remove via keySet
        assertTrue(keys.remove(null));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testEntrySet() {
        map.put("a", "1");
        map.put(null, null);
        Set<Map.Entry<String, String>> entries = map.entrySet();

        // verify contains
        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>("a", "1")));
        assertTrue(entries.contains(new AbstractMap.SimpleEntry<>(null, null)));

        // modify entry value
        for (Map.Entry<String, String> e : entries) {
            if ("a".equals(e.getKey())) {
                e.setValue("2");
            }
        }
        assertEquals("2", map.get("a"));

        // remove via entrySet
        assertTrue(entries.remove(new AbstractMap.SimpleEntry<>(null, null)));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testEqualsAndHashCode() {
        map.put("x", "1");
        map.put(null, null);

        Map<String, String> other = new HashMap<>();
        other.put("x", "1");
        other.put(null, null);

        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());

        other.put("extra", "y");
        assertFalse(map.equals(other));
    }

    @Test
    public void testToString() {
        map.put("a", "1");
        map.put(null, null);
        String s = map.toString();
        // Order is not guaranteed; just check substrings
        assertTrue(s.contains("a=1"));
        assertTrue(s.contains("null=null"));
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }

    /* ----------------------------------------------------------------------
     * Edge‑case / exception tests
     * ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testMergeRejectsNullRemappingFunction() {
        map.merge("k", "v", null);
    }

    @Test(expected = NullPointerException.class)
    public void testMergeRejectsNullValueArgument() {
        // The implementation explicitly forbids a null value argument
        map.merge("k", null, (v1, v2) -> v1);
    }

    @Test
    public void testComputeIfAbsentNullMappingFunctionReturnsNull() {
        // mapping returns null -> entry should be removed / not created
        assertNull(map.computeIfAbsent("missing", k -> null));
        assertFalse(map.containsKey("missing"));
    }

    @Test
    public void testComputeIfAbsentConcurrentBehaviourSimulation() {
        // Simulate another thread inserting a value between get and compute
        map.put("c", "initial");
        Function<String, String> delayed = k -> {
            // another thread puts a new value
            map.put(k, "other");
            return "computed";
        };
        // Since internal compute uses atomic operation, the result should be "initial"
        assertEquals("initial", map.computeIfAbsent("c", delayed));
        assertEquals("initial", map.get("c"));
    }

    @Test
    public void testValuesIteratorConcurrentModification() {
        map.put("x", "1");
        var values = map.values();
        var it = values.iterator();
        map.put("y", "2"); // concurrent modification is allowed for ConcurrentMap
        // iterator should still see both elements without throwing CME
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testKeySetIteratorRemoveAll() {
        map.put("a", "1");
        map.put("b", "2");
        var ks = map.keySet();
        ks.removeIf(k -> true);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testEntrySetRemoveNonexistent() {
        map.put("a", "1");
        assertFalse(map.entrySet().remove(new AbstractMap.SimpleEntry<>("a", "2")));
    }

    @Test
    public void testEqualsWithDifferentImplementation() {
        // Compare with a regular HashMap that also contains nulls
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("k", "v");
        hashMap.put(null, null);
        map.put("k", "v");
        map.put(null, null);
        assertTrue(map.equals(hashMap));
        assertTrue(hashMap.equals(map));
    }

    @Test
    public void testHashCodeConsistencyAfterMutations() {
        int h1 = map.hashCode();
        map.put("a", "1");
        int h2 = map.hashCode();
        assertNotEquals(h1, h2);
        map.remove("a");
        assertEquals(h1, map.hashCode());
    }
}
