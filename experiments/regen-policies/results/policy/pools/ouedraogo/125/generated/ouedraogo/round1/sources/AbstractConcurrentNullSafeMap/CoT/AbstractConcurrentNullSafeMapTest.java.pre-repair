package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * Concrete implementation of {@link AbstractConcurrentNullSafeMap} used only for testing.
 */
class TestConcurrentNullSafeMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {
    TestConcurrentNullSafeMap() {
        super(new ConcurrentHashMap<>());
    }
}

/**
 * JUnit‑4 test suite for {@link AbstractConcurrentNullSafeMap}.
 *
 * The suite is organised as follows:
 *
 * 1. **Public method list** – extracted directly from the source.
 * 2. **Basic functional tests** – one test per method exercising the normal contract.
 * 3. **Edge‑case / exception tests** – additional tests that target null handling,
 *    sentinel behaviour, and contract‑specified {@code null} checks (e.g. {@code merge}
 *    requires non‑null arguments).
 *
 * All tests use the concrete {@link TestConcurrentNullSafeMap} defined above; no
 * mocking or subclassing of library classes is performed.
 */
public class AbstractConcurrentNullSafeMapTest {

    private TestConcurrentNullSafeMap<String, String> map;

    @Before
    public void setUp() {
        map = new TestConcurrentNullSafeMap<>();
    }

    /* -------------------------------------------------
     *  1. PUBLIC METHOD SIGNATURES (extracted)
     * -------------------------------------------------
     *
     * int size()
     * boolean isEmpty()
     * boolean containsKey(Object key)
     * boolean containsValue(Object value)
     * V get(Object key)
     * V put(K key, V value)
     * V remove(Object key)
     * void putAll(Map<? extends K, ? extends V> m)
     * void clear()
     * V getOrDefault(Object key, V defaultValue)
     * V putIfAbsent(K key, V value)
     * boolean remove(Object key, Object value)
     * boolean replace(K key, V oldValue, V newValue)
     * V replace(K key, V value)
     * V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)
     * V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)
     * V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)
     * Collection<V> values()
     * Set<K> keySet()
     * Set<Entry<K, V>> entrySet()
     * boolean equals(Object o)
     * int hashCode()
     * String toString()
     */

    /* -------------------------------------------------
     *  2. BASIC FUNCTIONAL TESTS
     * ------------------------------------------------- */

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
        // normal put / get
        assertNull(map.put("k1", "v1"));
        assertEquals("v1", map.get("k1"));

        // replace existing
        assertEquals("v1", map.put("k1", "v2"));
        assertEquals("v2", map.get("k1"));

        // remove
        assertEquals("v2", map.remove("k1"));
        assertNull(map.get("k1"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNullKeyAndValueHandling() {
        // null key
        assertNull(map.put(null, "nullKey"));
        assertEquals("nullKey", map.get(null));
        assertTrue(map.containsKey(null));

        // null value
        assertNull(map.put("nullValue", null));
        assertNull(map.get("nullValue"));
        assertTrue(map.containsValue(null));

        // both null
        assertNull(map.put(null, null));
        assertNull(map.get(null));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
    }

    @Test
    public void testContainsKeyContainsValue() {
        map.put("x", "y");
        map.put(null, "z");
        map.put("w", null);

        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue("y"));
        assertTrue(map.containsValue(null));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> src = new AbstractMap.SimpleEntry<>("a", "1")
                .getKey() == null ? Collections.emptyMap() : Collections.singletonMap("a", "1");
        src = new java.util.HashMap<>();
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
        assertEquals("def", map.getOrDefault("absent", "def"));
        assertEquals("def", map.getOrDefault(null, "def"));
        map.put(null, null);
        assertNull(map.getOrDefault(null, "def"));
    }

    @Test
    public void testPutIfAbsent() {
        assertNull(map.putIfAbsent("k", "v"));
        assertEquals("v", map.get("k"));
        assertEquals("v", map.putIfAbsent("k", "new"));
        assertEquals("v", map.get("k"));
    }

    @Test
    public void testRemoveWithValue() {
        map.put("k", "v");
        assertFalse(map.remove("k", "wrong"));
        assertTrue(map.remove("k", "v"));
        assertFalse(map.containsKey("k"));
    }

    @Test
    public void testReplaceThreeArg() {
        map.put("k", "old");
        assertTrue(map.replace("k", "old", "new"));
        assertEquals("new", map.get("k"));
        assertFalse(map.replace("k", "old", "x"));
    }

    @Test
    public void testReplaceOneArg() {
        map.put("k", "old");
        assertEquals("old", map.replace("k", "new"));
        assertEquals("new", map.get("k"));
        assertNull(map.replace("absent", "any"));
    }

    @Test
    public void testComputeIfAbsent() {
        // compute when absent
        String result = map.computeIfAbsent("c", k -> "computed");
        assertEquals("computed", result);
        assertEquals("computed", map.get("c"));

        // compute when present (non‑null) – mapping function must NOT be called
        map.put("c", "already");
        Function<String, String> spy = new Function<String, String>() {
            @Override
            public String apply(String s) {
                fail("Mapping function should not be invoked for existing non‑null value");
                return null;
            }
        };
        assertEquals("already", map.computeIfAbsent("c", spy));

        // compute when mapped to null – function is called
        map.put("d", null);
        assertEquals("newD", map.computeIfAbsent("d", k -> "newD"));
    }

    @Test
    public void testCompute() {
        map.put("e", "old");
        BiFunction<String, String, String> remapper = (k, v) -> v + "-upd";
        assertEquals("old-upd", map.compute("e", remapper));
        assertEquals("old-upd", map.get("e"));

        // compute with null previous value
        assertEquals("init", map.compute("f", (k, v) -> v == null ? "init" : v));
        assertEquals("init", map.get("f"));

        // compute returning null removes entry
        assertNull(map.compute("f", (k, v) -> null));
        assertFalse(map.containsKey("f"));
    }

    @Test
    public void testMerge() {
        BiFunction<String, String, String> concat = (v1, v2) -> v1 + v2;

        // simple merge – key absent
        assertEquals("first", map.merge("g", "first", concat));
        assertEquals("first", map.get("g"));

        // merge when present
        assertEquals("firstsecond", map.merge("g", "second", concat));
        assertEquals("firstsecond", map.get("g"));

        // merge where remapping returns null – entry removed
        assertNull(map.merge("g", "any", (v1, v2) -> null));
        assertFalse(map.containsKey("g"));
    }

    @Test
    public void testValuesCollection() {
        map.put("k1", "v1");
        map.put(null, null);
        map.put("k2", null);

        // size and iteration
        assertEquals(3, map.values().size());
        assertTrue(map.values().contains("v1"));
        assertTrue(map.values().contains(null));

        // remove via iterator
        map.values().remove(null);
        assertEquals(2, map.size());
        assertFalse(map.containsValue(null));
    }

    @Test
    public void testKeySet() {
        map.put("a", "1");
        map.put(null, "nullKey");

        assertTrue(map.keySet().contains("a"));
        assertTrue(map.keySet().contains(null));
        assertEquals(2, map.keySet().size());

        // remove via keySet
        assertTrue(map.keySet().remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());
    }

    @Test
    public void testEntrySet() {
        map.put("x", "y");
        map.put(null, null);

        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertEquals(2, entrySet.size());

        // verify entry content
        boolean foundX = false, foundNull = false;
        for (Map.Entry<String, String> e : entrySet) {
            if ("x".equals(e.getKey())) {
                foundX = true;
                assertEquals("y", e.getValue());
                // test setValue
                e.setValue("y2");
                assertEquals("y2", map.get("x"));
            } else if (e.getKey() == null) {
                foundNull = true;
                assertNull(e.getValue());
            }
        }
        assertTrue(foundX && foundNull);

        // remove via entrySet
        Map.Entry<String, String> toRemove = null;
        for (Map.Entry<String, String> e : entrySet) {
            if (e.getKey() == null) {
                toRemove = e;
                break;
            }
        }
        assertNotNull(toRemove);
        assertTrue(entrySet.remove(toRemove));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testEqualsAndHashCode() {
        map.put("k", "v");
        map.put(null, null);

        TestConcurrentNullSafeMap<String, String> other = new TestConcurrentNullSafeMap<>();
        other.put("k", "v");
        other.put(null, null);

        assertEquals(map, other);
        assertEquals(map.hashCode(), other.hashCode());

        other.put("extra", "e");
        assertNotEquals(map, other);
    }

    @Test
    public void testToString() {
        map.put("a", "1");
        map.put(null, null);
        String s = map.toString();
        // The exact order is unspecified; just check that both mappings appear
        assertTrue(s.contains("a=1"));
        assertTrue(s.contains("null=null"));
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }

    /* -------------------------------------------------
     *  3. EDGE CASE / EXCEPTION TESTS
     * ------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testMergeNullRemappingFunction() {
        map.merge("h", "value", null);
    }

    @Test(expected = NullPointerException.class)
    public void testMergeNullValueNotAllowed() {
        // According to the implementation, merge() explicitly checks that the value argument is non‑null
        map.merge("i", null, (v1, v2) -> v1);
    }

    @Test
    public void testComputeIfAbsentMappingReturnsNull() {
        // mapping function returns null – entry must be absent after call
        assertNull(map.computeIfAbsent("j", k -> null));
        assertFalse(map.containsKey("j"));
    }

    @Test
    public void testComputeWithRemappingReturningNullRemovesEntry() {
        map.put("k", "v");
        assertNull(map.compute("k", (k1, v1) -> null));
        assertFalse(map.containsKey("k"));
    }

    @Test
    public void testPutAllWithNullKeyAndValueInSourceMap() {
        Map<String, String> src = new java.util.HashMap<>();
        src.put(null, null);
        src.put("a", null);
        src.put(null, "b"); // last put overrides previous null key entry

        map.putAll(src);
        assertEquals(2, map.size());
        assertEquals("b", map.get(null));
        assertNull(map.get("a"));
    }

    @Test
    public void testKeySetRemoveNonExisting() {
        assertFalse(map.keySet().remove("nonexistent"));
    }

    @Test
    public void testEntrySetRemoveNonExisting() {
        Map.Entry<String, String> fake = new AbstractMap.SimpleEntry<>("x", "y");
        assertFalse(map.entrySet().remove(fake));
    }

    @Test
    public void testValuesClear() {
        map.put("a", "1");
        map.put(null, null);
        map.values().clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testKeySetClear() {
        map.put("a", "1");
        map.put(null, null);
        map.keySet().clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testEntrySetClear() {
        map.put("a", "1");
        map.put(null, null);
        map.entrySet().clear();
        assertTrue(map.isEmpty());
    }
}
