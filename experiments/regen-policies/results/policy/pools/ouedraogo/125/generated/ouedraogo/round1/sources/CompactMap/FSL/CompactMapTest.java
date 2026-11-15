package com.cedarsoftware.util;
import com.cedarsoftware.util.CompactMap.CompactMapComparator;


import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class CompactMapTest {

    private CompactMap<String, String> emptyMap;

    @Before
    public void setUp() {
        emptyMap = new CompactMap<>();
    }

    /* -------------------------------------------------------------
     * Basic empty‑state behavior
     * ----------------------------------------------------------- */
    @Test
    public void testEmptyMap() {
        assertTrue(emptyMap.isEmpty());
        assertEquals(0, emptyMap.size());
        assertNull(emptyMap.get("any"));
        assertFalse(emptyMap.containsKey("any"));
        assertFalse(emptyMap.containsValue("any"));
        assertEquals("{}", emptyMap.toString());
        emptyMap.clear(); // should be a no‑op
        assertTrue(emptyMap.isEmpty());
    }

    /* -------------------------------------------------------------
     * Single‑value optimized storage (default single key = "id")
     * ----------------------------------------------------------- */
    @Test
    public void testSingleValueOptimization() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .singleValueKey("id")
                .compactSize(5)
                .build();

        // first put – should use the OBJECT storage path
        map.put("id", "123");
        assertEquals(1, map.size());
        assertEquals("123", map.get("id"));
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsValue("123"));

        // update the same key – value should be replaced, size unchanged
        map.put("id", "456");
        assertEquals(1, map.size());
        assertEquals("456", map.get("id"));

        // add a different key – transition to compact array storage
        map.put("other", "value");
        assertEquals(2, map.size());
        assertEquals("value", map.get("other"));
        assertTrue(map.containsKey("other"));
    }

    /* -------------------------------------------------------------
     * Transition from array to backing map when compactSize is exceeded
     * ----------------------------------------------------------- */
    @Test
    public void testTransitionToBackingMap() {
        // compactSize = 2 will force a map after the third entry
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(2)
                .build();

        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, map.size());
        assertTrue(map.getLogicalValueType() == CompactMap.LogicalValueType.ARRAY);

        // third entry triggers transition to a real Map
        map.put("c", 3);
        assertEquals(3, map.size());
        assertTrue(map.getLogicalValueType() == CompactMap.LogicalValueType.MAP);
        assertEquals(Integer.valueOf(3), map.get("c"));
    }

    /* -------------------------------------------------------------
     * Transition back from backing map to compact array when size drops
     * to compactSize
     * ----------------------------------------------------------- */
    @Test
    public void testTransitionBackToArray() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(2)
                .build();

        // Fill to map storage
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30); // now in MAP state

        assertEquals(3, map.size());
        assertTrue(map.getLogicalValueType() == CompactMap.LogicalValueType.MAP);

        // Remove one entry – size becomes 2 == compactSize → should go back to ARRAY
        map.remove("z");
        assertEquals(2, map.size());
        assertTrue(map.getLogicalValueType() == CompactMap.LogicalValueType.ARRAY);
    }

    /* -------------------------------------------------------------
     * Case‑insensitive key handling
     * ----------------------------------------------------------- */
    @Test
    public void testCaseInsensitiveKeys() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .caseSensitive(false) // enable case‑insensitivity
                .compactSize(5)
                .build();

        map.put("KeyOne", "value1");
        assertTrue(map.containsKey("keyone"));
        assertTrue(map.containsKey("KEYONE"));
        assertEquals("value1", map.get("kEyOnE"));

        // different key with same letters but different case should be considered equal
        map.put("KEYONE", "newValue");
        assertEquals(1, map.size());
        assertEquals("newValue", map.get("keyone"));
    }

    /* -------------------------------------------------------------
     * Sorted ordering – natural order of keys
     * ----------------------------------------------------------- */
    @Test
    public void testSortedOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(5)
                .build();

        map.put("b", 2);
        map.put("c", 3);
        map.put("a", 1);

        List<String> keys = new ArrayList<>();
        for (String k : map.keySet()) {
            keys.add(k);
        }
        assertEquals(Arrays.asList("a", "b", "c"), keys);
    }

    /* -------------------------------------------------------------
     * Reverse ordering – keys in descending order
     * ----------------------------------------------------------- */
    @Test
    public void testReverseOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .compactSize(5)
                .build();

        map.put("b", 2);
        map.put("c", 3);
        map.put("a", 1);

        List<String> keys = new ArrayList<>();
        for (String k : map.keySet()) {
            keys.add(k);
        }
        assertEquals(Arrays.asList("c", "b", "a"), keys);
    }

    /* -------------------------------------------------------------
     * Iterator remove and concurrent‑modification detection
     * ----------------------------------------------------------- */
    @Test
    public void testIteratorRemoveAndConcurrentModification() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();

        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getKey().equals("two")) {
                it.remove();
            }
        }
        assertEquals(2, map.size());
        assertFalse(map.containsKey("two"));

        // verify concurrent‑modification exception
        Iterator<String> keyIt = map.keySet().iterator();
        map.put("four", 4); // modify map after iterator creation
        try {
            keyIt.next();
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException ignored) {
        }
    }

    /* -------------------------------------------------------------
     * Equality and hashCode contract
     * ----------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode() {
        CompactMap<String, Integer> map1 = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();
        CompactMap<String, Integer> map2 = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();

        map1.put("x", 100);
        map1.put("y", 200);
        map2.put("y", 200);
        map2.put("x", 100);

        assertTrue(map1.equals(map2));
        assertTrue(map2.equals(map1));
        assertEquals(map1.hashCode(), map2.hashCode());

        map2.put("z", 300);
        assertFalse(map1.equals(map2));
    }

    /* -------------------------------------------------------------
     * toString produces a conventional map representation
     * ----------------------------------------------------------- */
    @Test
    public void testToString() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(5)
                .build();
        map.put("alpha", "A");
        map.put("beta", "B");

        String s = map.toString();
        // The exact order for unordered maps is not guaranteed, so we test containment
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("alpha=A"));
        assertTrue(s.contains("beta=B"));
    }

    /* -------------------------------------------------------------
     * CompactMapComparator behavior – case sensitivity and reverse flag
     * ----------------------------------------------------------- */
    @Test
    public void testCompactMapComparator() {
        CompactMapComparator ci = new CompactMapComparator(true, false); // case‑insensitive, normal order
        assertTrue(ci.compare("apple", "Banana") < 0);
        assertEquals(0, ci.compare("Test", "test"));

        CompactMapComparator rev = new CompactMapComparator(false, true); // case‑sensitive, reverse
        assertTrue(rev.compare("a", "b") > 0); // reverse order
        assertTrue(rev.compare(null, "x") > 0); // null is considered greater (last)
    }

    /* -------------------------------------------------------------
     * putAll with a source map and verify ordering for sorted map
     * ----------------------------------------------------------- */
    @Test
    public void testPutAllAndSortedOrdering() {
        Map<String, Integer> source = new HashMap<>();
        source.put("delta", 4);
        source.put("alpha", 1);
        source.put("charlie", 3);
        source.put("bravo", 2);

        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(10)
                .sourceMap(source)
                .build();

        List<String> keys = new ArrayList<>();
        for (String k : map.keySet()) {
            keys.add(k);
        }
        assertEquals(Arrays.asList("alpha", "bravo", "charlie", "delta"), keys);
    }

    /* -------------------------------------------------------------
     * Verify that values() collection reflects map changes
     * ----------------------------------------------------------- */
    @Test
    public void testValuesCollectionReflectsChanges() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();

        map.put("one", 1);
        map.put("two", 2);
        Collection<Integer> values = map.values();
        assertTrue(values.containsAll(Arrays.asList(1, 2)));

        // remove via iterator
        Iterator<Integer> it = values.iterator();
        while (it.hasNext()) {
            if (it.next().equals(1)) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("one"));
        assertEquals(1, map.size());
    }

    /* -------------------------------------------------------------
     * Verify that keySet().remove works correctly
     * ----------------------------------------------------------- */
    @Test
    public void testKeySetRemove() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(5)
                .build();

        map.put("k1", "v1");
        map.put("k2", "v2");
        assertTrue(map.keySet().remove("k1"));
        assertFalse(map.containsKey("k1"));
        assertEquals(1, map.size());
    }

    /* -------------------------------------------------------------
     * Verify handling of null keys and null values (HashMap permits both)
     * ----------------------------------------------------------- */
    @Test
    public void testNullKeyAndValue() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(5)
                .build();

        map.put(null, "nullKey");
        map.put("nullValue", null);

        assertEquals("nullKey", map.get(null));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get("nullValue"));
        assertEquals(2, map.size());

        map.remove(null);
        assertFalse(map.containsKey(null));
        assertEquals(1, map.size());
    }
}
