package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class CompactMapTest {

    private CompactMap<String, Object> map;

    @Before
    public void setUp() {
        map = new CompactMap<>();
    }

    /** Helper to read the private field {@code val} used to store the internal state. */
    private Object getInternalVal(CompactMap<?, ?> m) throws Exception {
        java.lang.reflect.Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        return f.get(m);
    }

    @Test
    public void testEmptyMap() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());
        assertTrue(map.entrySet().isEmpty());
        assertEquals("{}", map.toString());
    }

    @Test
    public void testPutAndGetSingleEntry_OptimizedSingleValueKey() throws Exception {
        // default single value key is "id"
        map.put("id", "12345");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("id"));
        assertEquals("12345", map.get("id"));

        // internal representation should be the raw value, not a CompactMapEntry
        Object val = getInternalVal(map);
        assertEquals("12345", val);
    }

    @Test
    public void testPutMultipleTriggersArrayStorage() throws Exception {
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));

        Object val = getInternalVal(map);
        assertTrue(val instanceof Object[]);
        Object[] arr = (Object[]) val;
        // array should contain alternating key/value
        assertEquals(4, arr.length);
        // order is insertion order for compact array
        assertEquals("a", arr[0]);
        assertEquals(1, arr[1]);
        assertEquals("b", arr[2]);
        assertEquals(2, arr[3]);
    }

    @Test
    public void testTransitionToBackingMapWhenExceedingCompactSize() throws Exception {
        // Use a very small compact size to force transition quickly
        CompactMap<String, Integer> smallMap = CompactMap.<String, Integer>builder()
                .compactSize(3)
                .build();

        smallMap.put("k1", 1);
        smallMap.put("k2", 2);
        smallMap.put("k3", 3);
        // still array (size == compactSize)
        assertTrue(getInternalVal(smallMap) instanceof Object[]);

        smallMap.put("k4", 4); // should trigger transition to Map
        assertTrue(getInternalVal(smallMap) instanceof Map);
        assertEquals(4, smallMap.size());
        assertEquals(Integer.valueOf(4), smallMap.get("k4"));
    }

    @Test
    public void testCaseInsensitiveBehavior() {
        CompactMap<String, String> ciMap = CompactMap.<String, String>builder()
                .caseSensitive(false)
                .build();

        ciMap.put("Key", "value");
        assertTrue(ciMap.containsKey("key"));
        assertEquals("value", ciMap.get("KEY"));
        assertEquals(1, ciMap.size());

        // ensure that a different key with different letters is not considered equal
        ciMap.put("another", "x");
        assertFalse(ciMap.containsKey("different"));
        assertEquals(2, ciMap.size());
    }

    @Test
    public void testRemoveCausesTransitionBackToSingleEntry() throws Exception {
        CompactMap<String, Integer> testMap = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();

        testMap.put("first", 10);
        testMap.put("second", 20);
        assertEquals(2, testMap.size());

        // remove one entry; should transition back to single-entry representation
        testMap.remove("first");
        assertEquals(1, testMap.size());
        assertEquals(Integer.valueOf(20), testMap.get("second"));

        // internal storage should be either raw value or CompactMapEntry, not an array or map
        Object val = getInternalVal(testMap);
        assertFalse(val instanceof Object[]);
        assertFalse(val instanceof Map);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorDetectsConcurrentModification() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        it.next(); // advance once
        // modify the map directly
        map.put("c", 3);
        // next() should now throw CME
        it.next();
    }

    @Test
    public void testEqualsAndHashCodeConsistency() {
        CompactMap<String, Integer> map1 = CompactMap.<String, Integer>builder()
                .compactSize(4)
                .build();
        CompactMap<String, Integer> map2 = CompactMap.<String, Integer>builder()
                .compactSize(4)
                .build();

        map1.put("x", 100);
        map1.put("y", 200);
        map2.put("y", 200);
        map2.put("x", 100);

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());

        // change one map and verify inequality
        map2.put("z", 300);
        assertNotEquals(map1, map2);
    }

    @Test
    public void testNullKeyAndNullValueHandling() {
        map.put(null, "nullKey");
        map.put("nullValue", null);

        assertTrue(map.containsKey(null));
        assertEquals("nullKey", map.get(null));
        assertTrue(map.containsKey("nullValue"));
        assertNull(map.get("nullValue"));

        assertEquals(2, map.size());
    }

    @Test
    public void testSingleValueKeyWithMapValueFallsBackToEntryStorage() throws Exception {
        // key matches the default single value key ("id") but value is a Map,
        // which should not be stored as raw value.
        Map<String, String> inner = new HashMap<>();
        inner.put("innerKey", "innerVal");

        map.put("id", inner);
        assertEquals(1, map.size());
        assertEquals(inner, map.get("id"));

        // internal representation must be a CompactMapEntry, not the raw map
        Object val = getInternalVal(map);
        assertTrue(val instanceof CompactMap.CompactMapEntry);
    }

    @Test
    public void testBuilderWithSourceMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("srcKey", 42);

        CompactMap<String, Integer> built = CompactMap.<String, Integer>builder()
                .sourceMap(source)
                .build();

        assertEquals(1, built.size());
        assertTrue(built.containsKey("srcKey"));
        assertEquals(Integer.valueOf(42), built.get("srcKey"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMinusIsUnsupported() {
        map.minus("anything");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlusIsUnsupported() {
        map.plus("anything");
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullMapThrows() {
        new CompactMap<>(null);
    }

    @Test
    public void testSubclassWithInvalidCompactSizeThrows() {
        try {
            new CompactMap<String, String>() {
                @Override
                protected int compactSize() {
                    return 1; // invalid, less than 2
                }
            };
            fail("Expected IllegalArgumentException due to compactSize < 2");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testPutAllIgnoresNullOrEmptyMap() {
        CompactMap<String, Integer> m = CompactMap.<String, Integer>builder().build();
        m.putAll(null); // should be a no‑op, not throw
        m.putAll(Collections.emptyMap()); // also a no‑op
        assertTrue(m.isEmpty());
    }

    @Test
    public void testEntrySetContainsAndRemove() {
        map.put("one", 1);
        map.put("two", 2);
        Set<Map.Entry<String, Integer>> entries = (Set) map.entrySet();

        Map.Entry<String, Integer> e = new AbstractMap.SimpleEntry<>("one", 1);
        assertTrue(entries.contains(e));

        // remove via entrySet
        assertTrue(entries.remove(e));
        assertFalse(map.containsKey("one"));
        assertEquals(1, map.size());
    }

    @Test
    public void testKeySetRemoveAllAndRetainAll() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Set<String> ks = map.keySet();

        // removeAll
        ks.removeAll(Arrays.asList("a", "b"));
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
        assertEquals(1, map.size());

        // retainAll (keep only "c")
        ks.retainAll(Collections.singleton("c"));
        assertTrue(map.containsKey("c"));
        assertEquals(1, map.size());
    }

    @Test
    public void testValuesIteratorRemove() {
        map.put("x", 10);
        map.put("y", 20);
        Iterator<Integer> it = map.values().iterator();
        while (it.hasNext()) {
            Integer v = it.next();
            if (v.equals(10)) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(20), map.get("y"));
    }
}
