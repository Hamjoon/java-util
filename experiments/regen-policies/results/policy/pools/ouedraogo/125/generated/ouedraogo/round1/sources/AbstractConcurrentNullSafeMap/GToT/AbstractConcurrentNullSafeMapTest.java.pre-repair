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

import static org.junit.Assert.*;

public class AbstractConcurrentNullSafeMapTest {

    /** Simple concrete implementation for testing */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {
        public TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    private TestMap<String, String> map;

    @Before
    public void setUp() {
        map = new TestMap<>();
    }

    /* ---------------------------------------------------------------------- */
    /* Basic Map Operations                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGet_WithNullKeyAndValue() {
        assertNull(map.put(null, "nullKey"));
        assertNull(map.put("key", null));

        assertEquals("nullKey", map.get(null));
        assertNull(map.get("key"));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("a", "1");
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put(null, "nullKey");
        map.put("k", null);
        map.put("x", "y");

        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("k"));
        assertTrue(map.containsKey("x"));
        assertFalse(map.containsKey("missing"));

        assertTrue(map.containsValue("nullKey"));
        assertTrue(map.containsValue(null));
        assertTrue(map.containsValue("y"));
        assertFalse(map.containsValue("nope"));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "1");
        source.put(null, "nullKey");
        source.put("b", null);
        map.putAll(source);

        assertEquals(3, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("b"));

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetOrDefault() {
        map.put("a", "1");
        assertEquals("1", map.getOrDefault("a", "default"));
        assertEquals("default", map.getOrDefault("missing", "default"));
        assertEquals("default", map.getOrDefault(null, "default"));
    }

    @Test
    public void testPutIfAbsent() {
        map.put("a", "1");
        assertEquals("1", map.putIfAbsent("a", "new"));
        assertEquals("1", map.get("a"));

        assertNull(map.putIfAbsent("b", null));
        assertNull(map.get("b"));
    }

    @Test
    public void testRemoveKey() {
        map.put("a", "1");
        map.put(null, "nullKey");
        assertEquals("1", map.remove("a"));
        assertNull(map.remove("a"));
        assertEquals("nullKey", map.remove(null));
    }

    @Test
    public void testRemoveKeyValue() {
        map.put("a", "1");
        map.put(null, null);
        assertTrue(map.remove("a", "1"));
        assertFalse(map.remove("a", "1"));
        assertTrue(map.remove(null, null));
        assertFalse(map.remove(null, null));
    }

    @Test
    public void testReplace() {
        map.put("a", "1");
        assertTrue(map.replace("a", "1", "2"));
        assertEquals("2", map.get("a"));
        assertFalse(map.replace("a", "1", "3")); // old value no longer matches

        // replace(key, value) variant
        map.put("b", "x");
        assertEquals("x", map.replace("b", "y"));
        assertEquals("y", map.get("b"));
    }

    /* ---------------------------------------------------------------------- */
    /* Compute and Merge Operations                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testComputeIfAbsent_ExistingNonNull() {
        map.put("a", "existing");
        map.computeIfAbsent("a", k -> "computed");
        assertEquals("existing", map.get("a"));
    }

    @Test
    public void testComputeIfAbsent_ComputesWhenAbsent() {
        map.computeIfAbsent("a", k -> "computed");
        assertEquals("computed", map.get("a"));
    }

    @Test
    public void testComputeIfAbsent_ComputesWhenMappedToNull() {
        map.put("a", null);
        map.computeIfAbsent("a", k -> "computed");
        assertEquals("computed", map.get("a"));
    }

    @Test
    public void testComputeIfAbsent_MappingReturnsNull() {
        map.computeIfAbsent("a", k -> null);
        assertFalse(map.containsKey("a"));
    }

    @Test
    public void testCompute_ReplacesValue() {
        map.put("a", "old");
        map.compute("a", (k, v) -> v + "2");
        assertEquals("old2", map.get("a"));
    }

    @Test
    public void testCompute_ReturnsNullRemovesEntry() {
        map.put("a", "willBeRemoved");
        map.compute("a", (k, v) -> null);
        assertFalse(map.containsKey("a"));
    }

    @Test
    public void testMerge_WithExistingValue() {
        map.put("a", "first");
        map.merge("a", "second", (v1, v2) -> v1 + "+" + v2);
        assertEquals("first+second", map.get("a"));
    }

    @Test
    public void testMerge_WithNoExistingValue() {
        map.merge("b", "only", (v1, v2) -> fail("Should not be called"));
        assertEquals("only", map.get("b"));
    }

    @Test(expected = NullPointerException.class)
    public void testMerge_NullRemappingFunctionThrows() {
        map.merge("c", "val", null);
    }

    @Test(expected = NullPointerException.class)
    public void testMerge_NullValueThrows() {
        map.merge("c", null, (v1, v2) -> "x");
    }

    @Test
    public void testMerge_RemappingReturnsNullRemovesEntry() {
        map.put("a", "value");
        map.merge("a", "ignored", (v1, v2) -> null);
        assertFalse(map.containsKey("a"));
    }

    /* ---------------------------------------------------------------------- */
    /* Views (values, keySet, entrySet)                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testValuesViewHandlesNulls() {
        map.put(null, "nullKey");
        map.put("k", null);
        map.put("x", "y");

        Set<String> values = Collections.unmodifiableSet(Set.copyOf(map.values()));
        assertTrue(values.contains("nullKey"));
        assertTrue(values.contains(null));
        assertTrue(values.contains("y"));
        assertEquals(3, values.size());
    }

    @Test
    public void testKeySetViewHandlesNulls() {
        map.put(null, "nullKey");
        map.put("k", null);
        map.put("x", "y");

        Set<String> keys = map.keySet();
        assertTrue(keys.contains(null));
        assertTrue(keys.contains("k"));
        assertTrue(keys.contains("x"));
        assertEquals(3, keys.size());

        // remove via keySet
        assertTrue(keys.remove(null));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testEntrySetViewOperations() {
        map.put(null, "nullKey");
        map.put("a", null);
        map.put("b", "val");

        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(3, entries.size());

        // verify iterator returns unmasked key/value
        for (Map.Entry<String, String> e : entries) {
            if (e.getKey() == null) {
                assertEquals("nullKey", e.getValue());
            } else if ("a".equals(e.getKey())) {
                assertNull(e.getValue());
            } else if ("b".equals(e.getKey())) {
                assertEquals("val", e.getValue());
            }
        }

        // test setValue through entry
        for (Map.Entry<String, String> e : entries) {
            if ("b".equals(e.getKey())) {
                e.setValue("newVal");
            }
        }
        assertEquals("newVal", map.get("b"));
    }

    @Test
    public void testEntrySetContainsAndRemove() {
        map.put("x", "y");
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("x", "y");
        assertTrue(map.entrySet().contains(entry));

        // remove via entrySet
        assertTrue(map.entrySet().remove(entry));
        assertFalse(map.containsKey("x"));
    }

    /* ---------------------------------------------------------------------- */
    /* Equality, HashCode and toString                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        map.put(null, "nullKey");
        map.put("a", null);
        map.put("b", "val");

        Map<String, String> other = new HashMap<>();
        other.put(null, "nullKey");
        other.put("a", null);
        other.put("b", "val");

        assertEquals(other, map);
        assertEquals(map, other);
        assertEquals(other.hashCode(), map.hashCode());
    }

    @Test
    public void testToStringContainsAllEntries() {
        map.put(null, "nullKey");
        map.put("a", null);
        map.put("b", "val");

        String s = map.toString();
        // The exact order is not guaranteed, but all entries must appear
        assertTrue(s.contains("null=nullKey"));
        assertTrue(s.contains("a=null"));
        assertTrue(s.contains("b=val"));
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
    }

    /* ---------------------------------------------------------------------- */
    /* Additional Edge Cases                                                   */
    /* ---------------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testContainsKey_WithWrongTypeThrows() {
        // internal cast (K) key in containsKey – passing an incompatible type should cause CCE
        map.put("string", "value");
        map.containsKey(123); // Integer cannot be cast to String
    }

    @Test
    public void testConcurrentModificationDuringIteration() {
        map.put("a", "1");
        map.put("b", "2");

        try {
            for (String key : map.keySet()) {
                map.remove(key);
            }
            // ConcurrentHashMap does not throw CME; just ensure map is empty after iteration
            assertTrue(map.isEmpty());
        } catch (ConcurrentModificationException e) {
            fail("ConcurrentHashMap should not throw CME");
        }
    }
}
