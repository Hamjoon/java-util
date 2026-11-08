package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CompactMapTest {

    private CompactMap<String, Integer> defaultMap;

    @Before
    public void setUp() {
        defaultMap = new CompactMap<>();
    }

    /* ----------------------------------------------------------------------
     * Basic map contract tests (size, isEmpty, clear, put, get, remove)
     * ---------------------------------------------------------------------- */
    @Test
    public void testEmptyMapContract() {
        assertTrue(defaultMap.isEmpty());
        assertEquals(0, defaultMap.size());
        assertNull(defaultMap.get("any"));
        assertFalse(defaultMap.containsKey("any"));
        assertFalse(defaultMap.containsValue(1));
        assertNull(defaultMap.remove("any"));
        defaultMap.clear(); // should be a no‑op
        assertTrue(defaultMap.isEmpty());
    }

    @Test
    public void testSingleEntryStorageAndSingleValueKeyOptimization() {
        // default single value key is "id"
        defaultMap.put("id", 42);
        // value stored directly (OBJECT state)
        assertEquals(CompactMap.LogicalValueType.OBJECT, defaultMap.getLogicalValueType());
        assertEquals(Integer.valueOf(42), defaultMap.get("id"));
        assertTrue(defaultMap.containsKey("id"));
        assertTrue(defaultMap.containsValue(42));

        // Overwrite the same key – should stay in OBJECT state
        defaultMap.put("id", 99);
        assertEquals(CompactMap.LogicalValueType.OBJECT, defaultMap.getLogicalValueType());
        assertEquals(Integer.valueOf(99), defaultMap.get("id"));

        // Adding a different key forces transition to ENTRY state
        defaultMap.put("other", 1);
        assertEquals(CompactMap.LogicalValueType.ENTRY, defaultMap.getLogicalValueType());
        assertEquals(Integer.valueOf(99), defaultMap.get("id"));
        assertEquals(Integer.valueOf(1), defaultMap.get("other"));
    }

    @Test
    public void testArrayStorageAndTransitionToMap() {
        // Use a small compactSize to trigger transition quickly
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(3)   // 2‑3 entries stay in array, 4+ go to backing map
                .build();

        // Add two entries – should be ARRAY state
        map.put("a", "A");
        map.put("b", "B");
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Add third entry – still ARRAY (size == compactSize)
        map.put("c", "C");
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Adding fourth entry forces transition to MAP state
        map.put("d", "D");
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // Verify all entries are present
        assertEquals(4, map.size());
        assertEquals("A", map.get("a"));
        assertEquals("B", map.get("b"));
        assertEquals("C", map.get("c"));
        assertEquals("D", map.get("d"));
    }

    @Test
    public void testRemovalTransitionsBackToArrayAndSingle() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(3)
                .build();

        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C"); // ARRAY state (size 3)

        // Remove one entry – should stay ARRAY (size 2)
        map.remove("c");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Remove another entry – should transition to SINGLE (size 1)
        map.remove("b");
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        // Remove last entry – back to EMPTY
        map.remove("a");
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    /* ----------------------------------------------------------------------
     * Builder option validation tests
     * ---------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsInvalidCompactSize() {
        CompactMap.<String, String>builder()
                .compactSize(1)   // must be >= 2
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsUnsupportedMapTypeIdentityHashMap() {
        CompactMap.<String, String>builder()
                .mapType(IdentityHashMap.class)
                .build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsUnsupportedMapTypeWeakHashMap() {
        CompactMap.<String, String>builder()
                .mapType(WeakHashMap.class)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testPutAllRejectsNullMap() {
        defaultMap.putAll(null);
    }

    /* ----------------------------------------------------------------------
     * Case‑insensitive key handling
     * ---------------------------------------------------------------------- */
    @Test
    public void testCaseInsensitiveKeys() {
        CompactMap<String, String> ciMap = CompactMap.<String, String>builder()
                .caseSensitive(false)
                .build();

        ciMap.put("Key", "value");
        assertTrue(ciMap.containsKey("key"));
        assertTrue(ciMap.containsKey("KEY"));
        assertEquals("value", ciMap.get("kEy"));
        assertEquals(1, ciMap.size());

        // Overwrite with different case – should replace the same entry
        ciMap.put("kEy", "new");
        assertEquals(1, ciMap.size());
        assertEquals("new", ciMap.get("KEY"));
    }

    /* ----------------------------------------------------------------------
     * Ordering tests (sorted, reverse, insertion)
     * ---------------------------------------------------------------------- */
    @Test
    public void testSortedOrderIteration() {
        CompactMap<String, Integer> sorted = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(5) // keep it in array for easier inspection
                .build();

        sorted.put("b", 2);
        sorted.put("c", 3);
        sorted.put("a", 1);

        Iterator<Map.Entry<String, Integer>> it = sorted.entrySet().iterator();
        Map.Entry<String, Integer> e1 = it.next();
        Map.Entry<String, Integer> e2 = it.next();
        Map.Entry<String, Integer> e3 = it.next();

        assertEquals("a", e1.getKey());
        assertEquals(Integer.valueOf(1), e1.getValue());
        assertEquals("b", e2.getKey());
        assertEquals(Integer.valueOf(2), e2.getValue());
        assertEquals("c", e3.getKey());
        assertEquals(Integer.valueOf(3), e3.getValue());
        assertFalse(it.hasNext());
    }

    @Test
    public void testReverseOrderIteration() {
        CompactMap<String, Integer> rev = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .compactSize(5)
                .build();

        rev.put("a", 1);
        rev.put("b", 2);
        rev.put("c", 3);

        Iterator<Map.Entry<String, Integer>> it = rev.entrySet().iterator();
        List<String> keys = new ArrayList<>();
        while (it.hasNext()) {
            keys.add(it.next().getKey());
        }
        // Reverse order of natural sorting: c, b, a
        assertEquals(Arrays.asList("c", "b", "a"), keys);
    }

    @Test
    public void testInsertionOrderIteration() {
        CompactMap<String, Integer> ins = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .compactSize(5)
                .build();

        ins.put("first", 1);
        ins.put("second", 2);
        ins.put("third", 3);

        Iterator<Map.Entry<String, Integer>> it = ins.entrySet().iterator();
        List<String> keys = new ArrayList<>();
        while (it.hasNext()) {
            keys.add(it.next().getKey());
        }
        assertEquals(Arrays.asList("first", "second", "third"), keys);
    }

    /* ----------------------------------------------------------------------
     * Equality, hashCode and toString contract
     * ---------------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCodeAcrossRepresentations() {
        // Map built via builder (array state)
        CompactMap<String, Integer> map1 = CompactMap.<String, Integer>builder()
                .compactSize(10)
                .build();
        map1.put("x", 10);
        map1.put("y", 20);

        // Same data via plain HashMap
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("x", 10);
        hashMap.put("y", 20);

        assertTrue(map1.equals(hashMap));
        assertTrue(hashMap.equals(map1));
        assertEquals(map1.hashCode(), hashMap.hashCode());

        // Different map – should not be equal
        hashMap.put("z", 30);
        assertFalse(map1.equals(hashMap));
    }

    @Test
    public void testToStringFormatting() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();
        map.put("one", 1);
        map.put("two", 2);
        String s = map.toString();
        // The string must contain both entries in any order (unordered default)
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
    }

    /* ----------------------------------------------------------------------
     * Collection view tests (keySet, values, entrySet)
     * ---------------------------------------------------------------------- */
    @Test
    public void testKeySetOperations() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Set<String> keys = map.keySet();
        assertEquals(3, keys.size());
        assertTrue(keys.containsAll(Arrays.asList("a", "b", "c")));

        // Remove via keySet view
        keys.remove("b");
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());

        // Clear via keySet view
        keys.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testValuesCollectionOperations() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        Collection<Integer> values = map.values();
        assertEquals(3, values.size());
        assertTrue(values.containsAll(Arrays.asList(10, 20, 30)));

        // Remove a value via iterator
        Iterator<Integer> it = values.iterator();
        while (it.hasNext()) {
            if (it.next().equals(20)) {
                it.remove();
            }
        }
        assertFalse(map.containsValue(20));
        assertEquals(2, map.size());
    }

    @Test
    public void testEntrySetRetainAllAndRemoveAll() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();

        // retainAll – keep only a and c
        Set<Map.Entry<String, Integer>> toKeep = new HashSet<>();
        toKeep.add(new AbstractMap.SimpleEntry<>("a", 1));
        toKeep.add(new AbstractMap.SimpleEntry<>("c", 3));
        entrySet.retainAll(toKeep);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("b"));
        assertFalse(map.containsKey("d"));

        // removeAll – clear remaining entries
        entrySet.removeAll(toKeep);
        assertTrue(map.isEmpty());
    }

    /* ----------------------------------------------------------------------
     * Iterator behaviour (hasNext, next, remove, concurrent modification)
     * ---------------------------------------------------------------------- */
    @Test
    public void testKeyIteratorRemoveAndConcurrentModification() {
        CompactMap<Integer, String> map = CompactMap.<Integer, String>builder()
                .compactSize(5)
                .build();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        Iterator<Integer> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        Integer first = it.next();
        assertEquals(Integer.valueOf(1), first);
        it.remove(); // remove key 1

        assertFalse(map.containsKey(1));
        assertEquals(2, map.size());

        // After removal, iterator should still be usable for remaining elements
        List<Integer> remaining = new ArrayList<>();
        while (it.hasNext()) {
            remaining.add(it.next());
        }
        assertEquals(Arrays.asList(2, 3), remaining);

        // Verify concurrent modification detection
        Iterator<Integer> it2 = map.keySet().iterator();
        map.put(4, "four"); // structural change
        try {
            it2.next();
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException expected) {
            // expected
        }
    }

    @Test
    public void testEntryIteratorRemoveAndConcurrentModification() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(5)
                .build();
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");

        Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if ("b".equals(e.getKey())) {
                it.remove(); // remove entry with key "b"
            }
        }
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());

        // Concurrent modification check
        Iterator<Map.Entry<String, String>> it2 = map.entrySet().iterator();
        map.remove("a");
        try {
            it2.next();
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException expected) {
            // expected
        }
    }

    /* ----------------------------------------------------------------------
     * CompactMapEntry specific behavior
     * ---------------------------------------------------------------------- */
    @Test
    public void testCompactMapEntrySetValueUpdatesParentMap() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("key", "old");
        // At this point we have a CompactMapEntry (ENTRY state)
        CompactMap.CompactMapEntry entry = map.new CompactMapEntry("key", "old");
        // Change value via entry
        entry.setValue("new");
        // The parent map must reflect the new value
        assertEquals("new", map.get("key"));
    }

    @Test
    public void testCompactMapEntryEqualsAndHashCode() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("k", 10);
        CompactMap.CompactMapEntry e1 = map.new CompactMapEntry("k", 10);
        CompactMap.CompactMapEntry e2 = map.new CompactMapEntry("k", 10);
        CompactMap.CompactMapEntry e3 = map.new CompactMapEntry("k", 20);
        CompactMap.CompactMapEntry e4 = map.new CompactMapEntry("K", 10); // case‑sensitive default

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotEquals(e1, e3);
        assertNotEquals(e1, e4);
    }

    /* ----------------------------------------------------------------------
     * CompactMapComparator tests (direct usage)
     * ---------------------------------------------------------------------- */
    @Test
    public void testCompactMapComparatorCaseInsensitiveAndReverse() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(true, true);
        // case‑insensitive, reverse order
        assertTrue(comp.compare("a", "B") > 0); // reverse of natural order, case ignored
        assertEquals(0, comp.compare("Test", "test"));
    }

    @Test
    public void testCompactMapComparatorHandlesNulls() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, false);
        assertTrue(comp.compare(null, "a") > 0); // null is considered greater (last)
        assertTrue(comp.compare("a", null) < 0);
        assertEquals(0, comp.compare(null, null));
    }

    /* ----------------------------------------------------------------------
     * Builder sourceMap initialization and ordering compatibility check
     * ---------------------------------------------------------------------- */
    @Test
    public void testBuilderWithSourceMapInitializesCorrectly() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("first", 1);
        source.put("second", 2);

        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .sourceMap(source)
                .build();

        // Verify entries are present and order is preserved
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            keys.add(e.getKey());
        }
        assertEquals(Arrays.asList("first", "second"), keys);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderDetectsOrderingConflictWithSourceMap() {
        // Source map is insertion‑ordered, but we request sorted order – should fail
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        CompactMap.<String, Integer>builder()
                .sortedOrder()
                .sourceMap(source)
                .build();
    }

    /* ----------------------------------------------------------------------
     * Verify that compactSize validation also works when using the default constructor
     * ---------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testDefaultConstructorRejectsInvalidCompactSizeViaSubclass() {
        // Subclass to override compactSize to an illegal value
        CompactMap<String, String> illegal = new CompactMap<String, String>() {
            @Override
            protected int compactSize() {
                return 1; // less than required minimum
            }
        };
    }

    /* ----------------------------------------------------------------------
     * Verify that putAll correctly handles empty source map (no change)
     * ---------------------------------------------------------------------- */
    @Test
    public void testPutAllWithEmptyMapDoesNothing() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("existing", 1);
        map.putAll(Collections.emptyMap());
        assertEquals(1, map.size());
        assertTrue(map.containsKey("existing"));
    }
}
