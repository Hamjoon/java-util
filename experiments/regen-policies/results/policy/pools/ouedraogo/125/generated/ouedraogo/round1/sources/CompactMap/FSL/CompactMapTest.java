package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/**
 * Comprehensive JUnit 4 tests for {@link CompactMap}.
 * Covers storage states, case‑sensitivity, ordering, transitions,
 * iterator behavior, equality, hashCode and custom configuration via the Builder.
 */
public class CompactMapTest {

    /** Helper subclass exposing the protected {@code getLogicalValueType()} for assertions. */
    static class TestCompactMap<K, V> extends CompactMap<K, V> {
        LogicalValueType state() {
            return super.getLogicalValueType();
        }
    }

    @Test
    public void testEmptyMapBehavior() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertNull(map.get("any"));
        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue(1));
        assertEquals("{}", map.toString());

        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());
        assertTrue(map.entrySet().isEmpty());

        // iterator on empty map must have no next
        assertFalse(map.keySet().iterator().hasNext());
        assertFalse(map.values().iterator().hasNext());
        assertFalse(map.entrySet().iterator().hasNext());
    }

    @Test
    public void testSingleEntryOptimizedStorage() {
        CompactMap<String, String> map = new CompactMap<>();
        // default singleValueKey is "id"
        map.put("id", "single");
        assertEquals(1, map.size());
        assertEquals("single", map.get("id"));
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsValue("single"));
        assertEquals("id=single", map.toString().replaceAll("[{} ]", "")); // mapToString format

        // Overwrite same key – should stay in OBJECT state
        map.put("id", "changed");
        assertEquals(1, map.size());
        assertEquals("changed", map.get("id"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, ((TestCompactMap<String, String>) map).state());

        // Remove the only entry – should become EMPTY
        map.remove("id");
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, ((TestCompactMap<String, String>) map).state());
    }

    @Test
    public void testSingleEntryWithNormalKey() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("foo", 42);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(42), map.get("foo"));
        assertTrue(map.containsKey("foo"));
        assertTrue(map.containsValue(42));
        assertEquals(CompactMap.LogicalValueType.ENTRY, ((TestCompactMap<String, Integer>) map).state());

        // Adding a second key forces transition to compact array
        map.put("bar", 7);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(7), map.get("bar"));
        assertEquals(CompactMap.LogicalValueType.ARRAY, ((TestCompactMap<String, Integer>) map).state());
    }

    @Test
    public void testCaseInsensitiveBehavior() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .caseSensitive(false)
                .build();

        map.put("Key", "value1");
        // Same key differing only by case should be considered equal
        assertEquals("value1", map.get("key"));
        assertTrue(map.containsKey("KEY"));
        // Overwrite using different case
        map.put("kEy", "value2");
        assertEquals("value2", map.get("KEY"));
        assertEquals(1, map.size());

        // Ensure entrySet contains the stored key (original case)
        Set<Map.Entry<String, String>> entries = map.entrySet();
        assertEquals(1, entries.size());
        Map.Entry<String, String> entry = entries.iterator().next();
        assertEquals("Key", entry.getKey()); // original key retained
        assertEquals("value2", entry.getValue());
    }

    @Test
    public void testSortedOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(3)          // keep small to stay in array state
                .build();

        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);

        // Keys should be iterated in natural order: a, b, c
        List<String> keyIter = new ArrayList<>();
        for (String k : map.keySet()) {
            keyIter.add(k);
        }
        assertEquals(Arrays.asList("a", "b", "c"), keyIter);

        // Values should follow the same ordering
        List<Integer> valIter = new ArrayList<>();
        for (Integer v : map.values()) {
            valIter.add(v);
        }
        assertEquals(Arrays.asList(1, 2, 3), valIter);
    }

    @Test
    public void testReverseOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .compactSize(3)
                .build();

        map.put("a", 1);
        map.put("c", 3);
        map.put("b", 2);

        // Expected order: c, b, a
        List<String> keys = new ArrayList<>();
        for (String k : map.keySet()) {
            keys.add(k);
        }
        assertEquals(Arrays.asList("c", "b", "a"), keys);
    }

    @Test
    public void testInsertionOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .compactSize(3)
                .build();

        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        List<String> keys = new ArrayList<>();
        for (String k : map.keySet()) {
            keys.add(k);
        }
        assertEquals(Arrays.asList("first", "second", "third"), keys);
    }

    @Test
    public void testCompactSizeTransitionToBackingMap() {
        // Compact size set to 2 to force transition after 3 entries
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(2)
                .build();

        // First entry – OBJECT or ENTRY
        map.put("one", 1);
        assertEquals(1, map.size());

        // Second entry – ARRAY (2 entries)
        map.put("two", 2);
        assertEquals(CompactMap.LogicalValueType.ARRAY,
                ((TestCompactMap<String, Integer>) map).state());

        // Third entry – should trigger transition to MAP
        map.put("three", 3);
        assertEquals(CompactMap.LogicalValueType.MAP,
                ((TestCompactMap<String, Integer>) map).state());

        // Verify map still contains all entries
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("two"));
        assertEquals(Integer.valueOf(3), map.get("three"));
    }

    @Test
    public void testRemovalTransitions() {
        // compactSize 2 => array after 2 entries, map after 3
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(2)
                .build();

        map.put("a", 1);
        map.put("b", 2); // ARRAY state
        assertEquals(CompactMap.LogicalValueType.ARRAY,
                ((TestCompactMap<String, Integer>) map).state());

        map.put("c", 3); // MAP state
        assertEquals(CompactMap.LogicalValueType.MAP,
                ((TestCompactMap<String, Integer>) map).state());

        // Remove one entry – should stay MAP (size 2 == compactSize, transition back to ARRAY)
        map.remove("c");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY,
                ((TestCompactMap<String, Integer>) map).state());

        // Remove another entry – should become SINGLE (OBJECT because key is "id"? No, key is not "id")
        map.remove("b");
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY,
                ((TestCompactMap<String, Integer>) map).state());

        // Remove last entry – becomes EMPTY
        map.remove("a");
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY,
                ((TestCompactMap<String, Integer>) map).state());
    }

    @Test
    public void testEntrySetIteratorRemove() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("x", "1");
        map.put("y", "2");
        map.put("z", "3");

        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("y".equals(e.getKey())) {
                it.remove(); // remove via iterator
            }
        }
        assertEquals(2, map.size());
        assertFalse(map.containsKey("y"));
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("z"));
    }

    @Test
    public void testEqualsAndHashCodeConsistency() {
        CompactMap<String, Integer> map1 = new CompactMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> stdMap = new HashMap<>();
        stdMap.put("a", 1);
        stdMap.put("b", 2);

        assertTrue(map1.equals(stdMap));
        assertTrue(stdMap.equals(map1));
        assertEquals(stdMap.hashCode(), map1.hashCode());

        // Change one value – equality should break
        map1.put("b", 3);
        assertFalse(map1.equals(stdMap));
        assertNotEquals(map1.hashCode(), stdMap.hashCode());
    }

    @Test
    public void testPutAllWithSourceMap() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("first", 10);
        source.put("second", 20);

        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sourceMap(source)
                .insertionOrder()
                .compactSize(5)
                .build();

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(10), map.get("first"));
        assertEquals(Integer.valueOf(20), map.get("second"));

        // Verify insertion order is preserved
        List<String> keys = new ArrayList<>();
        for (String k : map.keySet()) {
            keys.add(k);
        }
        assertEquals(Arrays.asList("first", "second"), keys);
    }

    @Test
    public void testCustomSingleValueKey() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .singleValueKey("uuid")
                .build();

        // Use the custom key – should use OBJECT storage
        map.put("uuid", "1234");
        assertEquals(1, map.size());
        assertEquals("1234", map.get("uuid"));
        assertEquals(CompactMap.LogicalValueType.OBJECT,
                ((TestCompactMap<String, String>) map).state());

        // Adding a different key forces transition to ARRAY
        map.put("other", "value");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY,
                ((TestCompactMap<String, String>) map).state());
    }

    @Test
    public void testBuilderMapTypeAndOrderingCompatibility() {
        // Use LinkedHashMap with insertion order – should be compatible
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .mapType(LinkedHashMap.class)
                .insertionOrder()
                .compactSize(4)
                .build();

        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);
        map.put("fourth", 4);

        // Verify iteration order matches insertion order
        List<String> keys = new ArrayList<>();
        for (String k : map.keySet()) {
            keys.add(k);
        }
        assertEquals(Arrays.asList("first", "second", "third", "fourth"), keys);
    }

    @Test
    public void testKeySetRemoveAndRetainAll() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        // remove via keySet.remove
        assertTrue(map.keySet().remove("b"));
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());

        // retainAll – keep only "c"
        Set<String> toRetain = new HashSet<>(Arrays.asList("c", "nonexistent"));
        map.keySet().retainAll(toRetain);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("a"));
    }

    @Test
    public void testValueCollectionRemove() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("x", "alpha");
        map.put("y", "beta");
        map.put("z", "gamma");

        // Remove a value via the collection view
        assertTrue(map.values().remove("beta"));
        assertFalse(map.containsValue("beta"));
        assertEquals(2, map.size());
        assertNull(map.get("y"));
    }

    @Test
    public void testCompactMapEntrySetBehavior() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("k1", 100);
        map.put("k2", 200);

        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        for (Map.Entry<String, Integer> e : entrySet) {
            if (e.getKey().equals("k1")) {
                e.setValue(150);
            }
        }
        assertEquals(Integer.valueOf(150), map.get("k1"));
        // Ensure entrySet reflects the change
        assertTrue(entrySet.contains(new AbstractMap.SimpleEntry<>("k1", 150)));
    }
}
