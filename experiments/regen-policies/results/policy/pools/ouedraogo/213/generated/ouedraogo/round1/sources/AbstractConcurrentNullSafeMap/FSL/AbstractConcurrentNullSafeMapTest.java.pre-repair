package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

/**
 * Concrete implementation of {@link AbstractConcurrentNullSafeMap} used only for testing.
 */
class TestConcurrentNullSafeMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {
    TestConcurrentNullSafeMap() {
        super(new ConcurrentHashMap<>());
    }
}

/**
 * JUnit 4 test suite that exercises every public method of {@link AbstractConcurrentNullSafeMap}.
 */
public class AbstractConcurrentNullSafeMapTest {

    private TestConcurrentNullSafeMap<String, String> map;

    @Before
    public void setUp() {
        map = new TestConcurrentNullSafeMap<>();
    }

    /* ---------- Basic Map Operations ---------- */

    @Test
    public void testPutAndGet_WithNullKeyAndValue() {
        assertNull(map.put(null, "nullKey"));
        assertEquals("nullKey", map.get(null));

        assertNull(map.put("nullValue", null));
        assertNull(map.get("nullValue"));
    }

    @Test
    public void testPutAndGet_Overwrite() {
        map.put("a", "1");
        assertEquals("1", map.put("a", "2"));
        assertEquals("2", map.get("a"));
    }

    @Test
    public void testRemove_WithNullKey() {
        map.put(null, "value");
        assertEquals("value", map.remove(null));
        assertNull(map.get(null));
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put(null, null);
        map.put("k1", "v1");
        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("k1"));
        assertFalse(map.containsKey("missing"));

        assertTrue(map.containsValue(null));
        assertTrue(map.containsValue("v1"));
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
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> other = new HashMap<>();
        other.put("k1", "v1");
        other.put(null, "nullKey");
        other.put("nullVal", null);

        map.putAll(other);
        assertEquals(3, map.size());
        assertEquals("v1", map.get("k1"));
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("nullVal"));

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetOrDefault() {
        map.put("present", "value");
        assertEquals("value", map.getOrDefault("present", "def"));
        assertEquals("def", map.getOrDefault("absent", "def"));
        assertEquals("def", map.getOrDefault(null, "def"));
    }

    @Test
    public void testPutIfAbsent() {
        assertNull(map.putIfAbsent("k", "v"));
        assertEquals("v", map.get("k"));
        // second call should not replace
        assertEquals("v", map.putIfAbsent("k", "new"));
        assertEquals("v", map.get("k"));
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
    public void testReplaceOneArg() {
        map.put("k", "old");
        assertEquals("old", map.replace("k", "new"));
        assertEquals("new", map.get("k"));
        assertNull(map.replace("missing", "x"));
    }

    /* ---------- computeIfAbsent ---------- */

    @Test
    public void testComputeIfAbsent_KeyAbsent_ComputesValue() {
        Function<String, String> mapping = k -> "computed-" + k;
        assertEquals("computed-a", map.computeIfAbsent("a", mapping));
        assertEquals("computed-a", map.get("a"));
    }

    @Test
    public void testComputeIfAbsent_KeyPresent_NonNull_NoCompute() {
        map.put("b", "existing");
        Function<String, String> mapping = k -> {
            fail("Mapping function should not be invoked when key already has non‑null value");
            return null;
        };
        assertEquals("existing", map.computeIfAbsent("b", mapping));
    }

    @Test
    public void testComputeIfAbsent_KeyPresent_Null_Computes() {
        map.put("c", null);
        Function<String, String> mapping = k -> "new-" + k;
        assertEquals("new-c", map.computeIfAbsent("c", mapping));
        assertEquals("new-c", map.get("c"));
    }

    @Test
    public void testComputeIfAbsent_MappingReturnsNull_RemovesEntry() {
        map.put("d", null);
        Function<String, String> mapping = k -> null;
        assertNull(map.computeIfAbsent("d", mapping));
        assertFalse(map.containsKey("d"));
    }

    /* ---------- compute ---------- */

    @Test
    public void testCompute_ReplacesValue() {
        map.put("e", "old");
        BiFunction<String, String, String> remap = (k, v) -> v + "-updated";
        assertEquals("old-updated", map.compute("e", remap));
        assertEquals("old-updated", map.get("e"));
    }

    @Test
    public void testCompute_KeyAbsent_CreatesEntry() {
        BiFunction<String, String, String> remap = (k, v) -> "created-" + k;
        assertEquals("created-f", map.compute("f", remap));
        assertEquals("created-f", map.get("f"));
    }

    @Test
    public void testCompute_ReturnsNull_RemovesEntry() {
        map.put("g", "toBeRemoved");
        BiFunction<String, String, String> remap = (k, v) -> null;
        assertNull(map.compute("g", remap));
        assertFalse(map.containsKey("g"));
    }

    /* ---------- merge ---------- */

    @Test
    public void testMerge_NoExistingValue() {
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV + newV; // not used
        assertEquals("v1", map.merge("h", "v1", remap));
        assertEquals("v1", map.get("h"));
    }

    @Test
    public void testMerge_WithExistingValue() {
        map.put("i", "old");
        BiFunction<String, String, String> remap = (oldV, newV) -> oldV + "+" + newV;
        assertEquals("old+new", map.merge("i", "new", remap));
        assertEquals("old+new", map.get("i"));
    }

    @Test
    public void testMerge_RemappingReturnsNull_RemovesEntry() {
        map.put("j", "present");
        BiFunction<String, String, String> remap = (oldV, newV) -> null;
        assertNull(map.merge("j", "ignored", remap));
        assertFalse(map.containsKey("j"));
    }

    /* ---------- Collection Views ---------- */

    @Test
    public void testValuesCollection() {
        map.put("k1", "v1");
        map.put(null, null);
        map.put("k2", null);
        // values should contain "v1", null, null (order not guaranteed)
        assertTrue(map.values().contains("v1"));
        assertTrue(map.values().contains(null));
        assertEquals(3, map.values().size());

        // remove via iterator
        map.values().remove(null);
        assertEquals(2, map.size());
        assertFalse(map.containsValue(null));
    }

    @Test
    public void testKeySetCollection() {
        map.put("a", "1");
        map.put(null, "nullKey");
        map.put("b", null);
        assertTrue(map.keySet().contains("a"));
        assertTrue(map.keySet().contains(null));
        assertTrue(map.keySet().contains("b"));
        assertEquals(3, map.keySet().size());

        // remove via keySet
        assertTrue(map.keySet().remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(2, map.size());
    }

    @Test
    public void testEntrySetOperations() {
        map.put("x", "1");
        map.put(null, null);
        map.put("y", null);

        // verify contains
        Map.Entry<String, String> e1 = new AbstractMap.SimpleEntry<>("x", "1");
        Map.Entry<String, String> e2 = new AbstractMap.SimpleEntry<>(null, null);
        assertTrue(map.entrySet().contains(e1));
        assertTrue(map.entrySet().contains(e2));

        // test setValue through entry
        for (Map.Entry<String, String> e : map.entrySet()) {
            if ("x".equals(e.getKey())) {
                assertEquals("1", e.setValue("new1"));
                assertEquals("new1", e.getValue());
            }
        }
        assertEquals("new1", map.get("x"));

        // remove via entrySet
        assertTrue(map.entrySet().remove(e2));
        assertFalse(map.containsKey(null));
        assertEquals(2, map.size());
    }

    /* ---------- equals, hashCode, toString ---------- */

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
        assertEquals(map.hashCode(), other.hashCode());

        // modify other and ensure inequality
        other.put("c", "3");
        assertFalse(map.equals(other));
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("k1", "v1");
        map.put(null, null);
        String s = map.toString();
        // The exact format depends on MapUtilities.mapToString, but we can verify that
        // the key/value pairs appear in the string.
        assertTrue(s.contains("k1=v1"));
        assertTrue(s.contains("null=null"));
        // Ensure braces are present
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
    }
}
