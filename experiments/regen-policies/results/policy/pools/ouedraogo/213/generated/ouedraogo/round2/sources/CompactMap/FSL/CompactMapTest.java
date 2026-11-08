package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assume;

import java.util.*;
import java.util.stream.Collectors;
import javax.tools.ToolProvider;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 * Covers default configuration, state transitions, case‑insensitivity,
 * ordering strategies, collection views, equals/hashCode, configuration
 * handling and iterator behaviours.
 */
public class CompactMapTest {

    @BeforeClass
    public static void requireJDK() {
        // CompactMap uses runtime code generation which needs a JavaCompiler (JDK).
        Assume.assumeTrue("JDK compiler not available – skipping tests", ToolProvider.getSystemJavaCompiler() != null);
    }

    @Test
    public void testDefaultConfiguration() {
        CompactMap<String, Object> map = new CompactMap<>();
        assertTrue("Default map should be recognised as default", map.isDefaultCompactMap());
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    @Test
    public void testSingleValueStorage() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("id", "singleValue");
        assertEquals(1, map.size());
        assertEquals("singleValue", map.get("id"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());
        assertFalse(map.isEmpty());
    }

    @Test
    public void testTransitionArrayToMap() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .compactSize(3)          // small threshold to trigger transitions quickly
                .build();

        // first entry – stored as ENTRY (single entry)
        map.put("first", 1);
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        // second entry – moves to ARRAY representation
        map.put("second", 2);
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // third entry – still ARRAY (size == compactSize)
        map.put("third", 3);
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // fourth entry – exceeds compactSize, should switch to MAP backing
        map.put("fourth", 4);
        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
    }

    @Test
    public void testCaseInsensitiveKeys() {
        CompactMap<String, String> map = CompactMap.builder()
                .caseSensitive(false)
                .build();

        map.put("Key", "value");
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsKey("KEY"));
        assertEquals("value", map.get("kEy"));
        assertEquals(1, map.size());

        // overwrite using different case
        map.put("kEy", "new");
        assertEquals("new", map.get("KEY"));
        assertEquals(1, map.size());
    }

    @Test
    public void testSortedOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .sortedOrder()
                .compactSize(10)
                .build();

        map.put("b", 2);
        map.put("a", 1);
        map.put("c", 3);

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("a", "b", "c"), keys);
    }

    @Test
    public void testInsertionOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .insertionOrder()
                .compactSize(10)
                .build();

        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("first", "second", "third"), keys);
    }

    @Test
    public void testReverseOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .reverseOrder()
                .compactSize(10)
                .build();

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("c", "b", "a"), keys);
    }

    @Test
    public void testKeySetRemoveAndIteratorRemove() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .compactSize(5)
                .build();

        map.put("x", 10);
        map.put("y", 20);
        assertTrue(map.keySet().remove("x"));
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());

        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            if ("y".equals(k)) {
                it.remove();
            }
        }
        assertTrue(map.isEmpty());
    }

    @Test
    public void testEntrySetContainsAndRemove() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .compactSize(5)
                .build();

        map.put("k1", 100);
        map.put("k2", 200);

        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        Map.Entry<String, Integer> probe = new AbstractMap.SimpleEntry<>("k1", 100);
        assertTrue(entrySet.contains(probe));

        assertTrue(entrySet.remove(probe));
        assertFalse(map.containsKey("k1"));
        assertEquals(1, map.size());
    }

    @Test
    public void testValuesCollectionBehaviour() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .compactSize(5)
                .build();

        map.put("a", 1);
        map.put("b", 2);
        Collection<Integer> vals = map.values();

        assertTrue(vals.containsAll(Arrays.asList(1, 2)));
        assertEquals(2, vals.size());

        map.remove("a");
        assertFalse(vals.contains(1));
        assertEquals(1, vals.size());
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactMap<String, Integer> map1 = CompactMap.builder()
                .compactSize(5)
                .build();
        map1.put("a", 1);
        map1.put("b", 2);

        CompactMap<String, Integer> map2 = new CompactMap<>();
        map2.putAll(map1);

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());

        // modify map2 and verify inequality
        map2.put("c", 3);
        assertNotEquals(map1, map2);
    }

    @Test
    public void testToStringContainsEntries() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .compactSize(5)
                .build();
        map.put("x", 10);
        map.put("y", 20);
        String s = map.toString();
        assertTrue(s.contains("x=10"));
        assertTrue(s.contains("y=20"));
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }

    @Test
    public void testGetConfigReflectsBuilderSettings() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .caseSensitive(false)
                .sortedOrder()
                .compactSize(7)
                .singleValueKey("uid")
                .mapType(LinkedHashMap.class)
                .build();

        Map<String, Object> cfg = map.getConfig();

        assertEquals(7, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
        assertEquals("uid", cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(LinkedHashMap.class, cfg.get(CompactMap.MAP_TYPE));
    }

    @Test
    public void testWithConfigCreatesEquivalentMap() {
        CompactMap<String, Integer> original = CompactMap.builder()
                .compactSize(5)
                .build();
        original.put("a", 1);
        original.put("b", 2);

        // Request a new map with the same entries but default configuration (empty config map)
        CompactMap<String, Integer> copy = original.withConfig(Collections.emptyMap());

        assertEquals(original, copy);
        assertNotSame(original, copy);
        assertEquals(original.size(), copy.size());
    }

    @Test
    public void testCompactMapComparatorCaseInsensitive() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(true, false);
        assertTrue(comp.compare("apple", "Banana") < 0); // case‑insensitive alphabetical order
        assertEquals(0, comp.compare("Test", "test"));
    }

    @Test
    public void testCompactMapComparatorReverseOrder() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, true);
        assertTrue(comp.compare(5, 3) < 0); // reverse numeric order
        assertTrue(comp.compare("a", "b") > 0); // reverse string order
    }

    @Test
    public void testClearResetsState() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .compactSize(3)
                .build();

        map.put("x", 1);
        map.put("y", 2);
        map.put("z", 3); // should be ARRAY (size == compactSize)
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
        assertEquals(0, map.size());
    }

    @Test
    public void testContainsValue() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("k1", "v1");
        map.put("k2", "v2");
        assertTrue(map.containsValue("v1"));
        assertFalse(map.containsValue("missing"));
        map.remove("k1");
        assertFalse(map.containsValue("v1"));
    }

    @Test
    public void testRemoveFromArrayAndMapTransitions() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .compactSize(3)
                .build();

        map.put("a", 1);
        map.put("b", 2); // ARRAY state (size 2)
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        map.remove("a");
        // After removal size is 1 -> should transition to single entry (ENTRY)
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        map.put("c", 3);
        map.put("d", 4); // size 3, still ARRAY
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        map.put("e", 5); // exceeds compactSize -> MAP
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());
    }
}
