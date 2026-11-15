package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link AbstractConcurrentNullSafeMap}.
 * <p>
 * The abstract class is exercised via a tiny concrete implementation that
 * delegates to a {@link ConcurrentHashMap}.  All public API methods are
 * covered, including normal behaviour, null handling and the documented
 * exception cases.
 */
public class AbstractConcurrentNullSafeMapTest {

    /** Simple concrete subclass used for testing. */
    private static class SimpleMap extends AbstractConcurrentNullSafeMap<String, String> {
        SimpleMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    private SimpleMap map;

    @Before
    public void setUp() {
        map = new SimpleMap();
    }

    /* --------------------------------------------------------------------- */
    /*  Basic Map contract methods                                           */
    /* --------------------------------------------------------------------- */

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
    public void testPutAndGetNonNull() {
        String previous = map.put("key", "value");
        assertNull(previous);
        assertEquals("value", map.get("key"));
    }

    @Test
    public void testPutAndGetNullKeyAndValue() {
        // null key
        map.put(null, "v1");
        assertEquals("v1", map.get(null));

        // null value
        map.put("k2", null);
        assertNull(map.get("k2"));

        // both null
        map.put(null, null);
        assertNull(map.get(null));
    }

    @Test
    public void testContainsKeyAndValue() {
        map.put("k1", "v1");
        map.put(null, null);
        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsKey(null));
        assertFalse(map.containsKey("missing"));

        assertTrue(map.containsValue("v1"));
        assertTrue(map.containsValue(null));
        assertFalse(map.containsValue("missing"));
    }

    @Test
    public void testRemoveKey() {
        map.put("x", "y");
        map.put(null, "z");
        assertEquals("y", map.remove("x"));
        assertNull(map.get("x"));
        assertEquals("z", map.remove(null));
        assertNull(map.get(null));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "1");
        source.put(null, "2");
        source.put("c", null);
        map.putAll(source);
        assertEquals(3, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get(null));
        assertNull(map.get("c"));

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testGetOrDefault() {
        map.put("k", null);
        assertEquals("def", map.getOrDefault("missing", "def"));
        assertEquals("def", map.getOrDefault(null, "def"));
        assertNull(map.getOrDefault("k", "def"));
    }

    @Test
    public void testPutIfAbsent() {
        map.put("k", "v1");
        assertEquals("v1", map.putIfAbsent("k", "v2"));
        assertEquals("v1", map.get("k"));

        assertNull(map.putIfAbsent("new", "val"));
        assertEquals("val", map.get("new"));
    }

    @Test
    public void testRemoveKeyValue() {
        map.put("k", "v");
        assertTrue(map.remove("k", "v"));
        assertFalse(map.containsKey("k"));

        map.put(null, null);
        assertTrue(map.remove(null, null));
        assertFalse(map.containsKey(null));
    }

    @Test
    public void testReplaceThreeArgs() {
        map.put("k", "old");
        assertTrue(map.replace("k", "old", "new"));
        assertEquals("new", map.get("k"));

        assertFalse(map.replace("k", "old", "new2"));
        assertEquals("new", map.get("k"));
    }

    @Test
    public void testReplaceOneArg() {
        map.put("k", "v1");
        assertEquals("v1", map.replace("k", "v2"));
        assertEquals("v2", map.get("k"));

        // replace on missing key returns null
        assertNull(map.replace("missing", "x"));
    }

    /* --------------------------------------------------------------------- */
    /*  Compute / Merge API                                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testComputeIfAbsentCreatesValue() {
        Function<String, String> mapping = k -> k.toUpperCase();
        String result = map.computeIfAbsent("key", mapping);
        assertEquals("KEY", result);
        assertEquals("KEY", map.get("key"));
    }

    @Test
    public void testComputeIfAbsentExistingValueIsReturned() {
        map.put("k", "existing");
        Function<String, String> mapping = k -> "new";
        String result = map.computeIfAbsent("k", mapping);
        assertEquals("existing", result);
    }

    @Test
    public void testComputeIfAbsentMappingReturnsNullRemovesEntry() {
        map.put("k", null);
        Function<String, String> mapping = k -> null;
        String result = map.computeIfAbsent("k", mapping);
        assertNull(result);
        assertFalse(map.containsKey("k"));
    }

    @Test
    public void testComputeUpdatesValue() {
        map.put("k", "v1");
        BiFunction<String, String, String> remap = (k, v) -> v + "2";
        String result = map.compute("k", remap);
        assertEquals("v12", result);
        assertEquals("v12", map.get("k"));
    }

    @Test
    public void testComputeRemovesWhenNullReturned() {
        map.put("k", "v");
        BiFunction<String, String, String> remap = (k, v) -> null;
        String result = map.compute("k", remap);
        assertNull(result);
        assertFalse(map.containsKey("k"));
    }

    @Test
    public void testMergeCombinesValues() {
        BiFunction<String, String, String> merger = (v1, v2) -> v1 + v2;
        map.put("k", "a");
        String result = map.merge("k", "b", merger);
        assertEquals("ab", result);
        assertEquals("ab", map.get("k"));
    }

    @Test(expected = NullPointerException.class)
    public void testMergeNullRemappingFunctionThrows() {
        map.merge("k", "v", null);
    }

    @Test(expected = NullPointerException.class)
    public void testMergeNullValueThrows() {
        BiFunction<String, String, String> merger = (v1, v2) -> v1 + v2;
        map.merge("k", null, merger);
    }

    /* --------------------------------------------------------------------- */
    /*  Collection views (values, keySet, entrySet)                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testValuesCollection() {
        map.put("a", "1");
        map.put(null, null);
        map.put("b", "2");

        assertTrue(map.values().contains("1"));
        assertTrue(map.values().contains(null));
        assertFalse(map.values().contains("3"));
        assertEquals(3, map.values().size());

        // test iterator remove
        Iterator<String> it = map.values().iterator();
        while (it.hasNext()) {
            if ("1".equals(it.next())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("a"));
    }

    @Test
    public void testKeySetOperations() {
        map.put("x", "1");
        map.put(null, "2");
        map.put("y", null);

        assertTrue(map.keySet().contains("x"));
        assertTrue(map.keySet().contains(null));
        assertFalse(map.keySet().contains("missing"));
        assertEquals(3, map.keySet().size());

        assertTrue(map.keySet().remove(null));
        assertFalse(map.containsKey(null));

        // iterator remove
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            if ("x".equals(it.next())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("x"));
    }

    @Test
    public void testEntrySetOperations() {
        map.put("k1", "v1");
        map.put(null, null);
        map.put("k2", "v2");

        assertEquals(3, map.entrySet().size());

        // contains
        Map.Entry<String, String> e = new AbstractMap.SimpleEntry<>("k1", "v1");
        assertTrue(map.entrySet().contains(e));

        // remove via entrySet
        assertTrue(map.entrySet().remove(e));
        assertFalse(map.containsKey("k1"));

        // iterator setValue
        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            if (entry.getKey() == null) {
                entry.setValue("newNullValue");
            }
        }
        assertEquals("newNullValue", map.get(null));
    }

    /* --------------------------------------------------------------------- */
    /*  equals / hashCode / toString                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        map.put("a", "1");
        map.put(null, null);
        map.put("b", "2");

        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        other.put(null, null);
        other.put("b", "2");

        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());

        other.put("c", "3");
        assertFalse(map.equals(other));
    }

    @Test
    public void testToStringEmptyAndNonEmpty() {
        assertEquals("{}", map.toString());

        map.put("x", "1");
        map.put(null, null);
        String s = map.toString();
        // order is not guaranteed, so verify both possible key/value strings are present
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("x=1"));
        assertTrue(s.contains("null=null"));
        // ensure there is a comma between entries when size > 1
        assertTrue(s.contains(", "));
    }

    /* --------------------------------------------------------------------- */
    /*  Edge case: concurrent‑modification safety of the view iterators is not */
    /*  directly testable without multi‑threading; a simple sanity check is   */
    /*  performed to ensure no unexpected RuntimeException is thrown.        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testValuesIteratorNoConcurrentModificationException() {
        map.put("a", "1");
        map.put("b", "2");
        Iterator<String> it = map.values().iterator();
        map.put("c", "3"); // modify map after iterator creation
        // The iterator should reflect the change without throwing CME because
        // it delegates to the underlying ConcurrentHashMap iterator.
        while (it.hasNext()) {
            it.next(); // just consume
        }
    }
}
