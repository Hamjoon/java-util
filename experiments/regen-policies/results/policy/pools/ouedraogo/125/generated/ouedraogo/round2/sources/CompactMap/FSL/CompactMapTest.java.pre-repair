package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests exercise every public method, the builder, the internal state
 * transitions (EMPTY → SINGLE → ARRAY → MAP and back), case‑insensitive key
 * handling, ordering, the optimized single‑value storage, iterator behaviour,
 * equality, hashCode and the {@link CompactMapEntry} write‑through semantics.
 */
public class CompactMapTest {

    /* ---------------------------------------------------------------------- */
    /* Helper to expose the protected LogicalValueType for assertions.        */
    private CompactMap.LogicalValueType typeOf(CompactMap<?, ?> map) {
        return map.getLogicalValueType();
    }

    /* ---------------------------------------------------------------------- */
    /** 1. Basic put / get / contains / size / empty handling. */
    @Test
    public void testBasicOperations() {
        CompactMap<String, Integer> map = new CompactMap<>();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        // put first entry – should be single entry storage
        assertNull(map.put("one", 1));
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertTrue(map.containsKey("one"));
        assertTrue(map.containsValue(1));

        // overwrite same key – previous value returned
        assertEquals(Integer.valueOf(1), map.put("one", 10));
        assertEquals(Integer.valueOf(10), map.get("one"));
        assertEquals(1, map.size());

        // put second entry – should transition to compact array
        assertNull(map.put("two", 2));
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(2), map.get("two"));
        assertTrue(map.containsKey("two"));
        assertTrue(map.containsValue(2));
        assertEquals(CompactMap.LogicalValueType.ARRAY, typeOf(map));

        // clear the map – back to empty
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, typeOf(map));
    }

    /* ---------------------------------------------------------------------- */
    /** 2. Transition from array to backing map when compactSize is exceeded. */
    @Test
    public void testTransitionToBackingMap() {
        // compactSize = 2 → third entry forces map storage
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(2)
                .build();

        map.put("a", 1);                     // SINGLE
        assertEquals(CompactMap.LogicalValueType.OBJECT, typeOf(map));

        map.put("b", 2);                     // ARRAY (size 2)
        assertEquals(CompactMap.LogicalValueType.ARRAY, typeOf(map));

        map.put("c", 3);                     // MAP (size 3)
        assertEquals(CompactMap.LogicalValueType.MAP, typeOf(map));
        assertEquals(3, map.size());

        // Verify map behaviour after transition
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(Integer.valueOf(3), map.get("c"));
    }

    /* ---------------------------------------------------------------------- */
    /** 3. Removing entries causes a reverse transition back to array and single. */
    @Test
    public void testReverseTransition() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(2)          // 3rd entry → map
                .build();

        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);                // now in MAP state
        assertEquals(CompactMap.LogicalValueType.MAP, typeOf(map));

        // Remove one entry – still MAP because size (2) == compactSize, triggers array transition
        map.remove("z");
        assertEquals(CompactMap.LogicalValueType.ARRAY, typeOf(map));
        assertEquals(2, map.size());

        // Remove another entry – should go back to SINGLE (OBJECT)
        map.remove("y");
        assertEquals(CompactMap.LogicalValueType.OBJECT, typeOf(map));
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(10), map.get("x"));

        // Remove last entry – EMPTY
        map.remove("x");
        assertEquals(CompactMap.LogicalValueType.EMPTY, typeOf(map));
        assertTrue(map.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /** 4. Case‑insensitive key handling. */
    @Test
    public void testCaseInsensitiveKeys() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .caseSensitive(false)    // case‑insensitive
                .compactSize(5)
                .build();

        map.put("Key", "value");
        assertEquals("value", map.get("key"));
        assertTrue(map.containsKey("KEY"));
        assertTrue(map.containsValue("value"));
        assertEquals(1, map.size());

        // Overwrite using different case – previous value returned
        assertEquals("value", map.put("kEy", "new"));
        assertEquals("new", map.get("KEY"));
        assertEquals(1, map.size());
    }

    /* ---------------------------------------------------------------------- */
    /** 5. Ordering – insertion order. */
    @Test
    public void testInsertionOrder() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .compactSize(5)
                .build();

        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("first", "second", "third"), keys);
    }

    /* ---------------------------------------------------------------------- */
    /** 6. Ordering – natural sorted order. */
    @Test
    public void testSortedOrder() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(5)
                .build();

        map.put("b", 2);
        map.put("c", 3);
        map.put("a", 1);

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("a", "b", "c"), keys);
    }

    /* ---------------------------------------------------------------------- */
    /** 7. Ordering – reverse sorted order. */
    @Test
    public void testReverseOrder() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .compactSize(5)
                .build();

        map.put("b", 2);
        map.put("c", 3);
        map.put("a", 1);

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("c", "b", "a"), keys);
    }

    /* ---------------------------------------------------------------------- */
    /** 8. Optimized single‑value storage using custom singleValueKey. */
    @Test
    public void testSingleValueKeyOptimization() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .singleValueKey("uuid")
                .compactSize(5)
                .build();

        // Put entry with the special key – should store the value directly (OBJECT state)
        map.put("uuid", "12345");
        assertEquals(CompactMap.LogicalValueType.OBJECT, typeOf(map));
        assertEquals("12345", map.get("uuid"));
        assertEquals(1, map.size());

        // Adding a different key forces array storage
        map.put("other", "x");
        assertEquals(CompactMap.LogicalValueType.ARRAY, typeOf(map));
        assertEquals(2, map.size());
    }

    /* ---------------------------------------------------------------------- */
    /** 9. Equality and hashCode consistency. */
    @Test
    public void testEqualsAndHashCode() {
        CompactMap<String, Integer> map1 = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();
        CompactMap<String, Integer> map2 = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();

        map1.put("a", 1);
        map1.put("b", 2);
        map2.put("b", 2);
        map2.put("a", 1);

        assertEquals(map1, map2);
        assertEquals(map2, map1);
        assertEquals(map1.hashCode(), map2.hashCode());

        // Change one map – equality breaks
        map2.put("c", 3);
        assertNotEquals(map1, map2);
    }

    /* ---------------------------------------------------------------------- */
    /** 10. toString contains all entries (order not asserted for unordered maps). */
    @Test
    public void testToStringContainsEntries() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();
        map.put("one", 1);
        map.put("two", 2);
        String s = map.toString();

        // The string should start with '{' and end with '}' and contain both mappings
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
    }

    /* ---------------------------------------------------------------------- */
    /** 11. entrySet contains, remove and iterator remove works. */
    @Test
    public void testEntrySetOperations() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .compactSize(5)
                .build();
        map.put("k1", "v1");
        map.put("k2", "v2");

        Set<Map.Entry<String, String>> entries = map.entrySet();

        // contains
        Map.Entry<String, String> e = new AbstractMap.SimpleEntry<>("k1", "v1");
        assertTrue(entries.contains(e));

        // remove via entrySet.remove()
        assertTrue(entries.remove(e));
        assertFalse(map.containsKey("k1"));
        assertEquals(1, map.size());

        // iterator remove
        Iterator<Map.Entry<String, String>> it = entries.iterator();
        assertTrue(it.hasNext());
        it.next();               // should be the remaining entry "k2"
        it.remove();             // remove it
        assertTrue(map.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /** 12. CompactMapEntry setValue writes through to the map. */
    @Test
    public void testCompactMapEntrySetValue() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("id", "original");   // uses default singleValueKey = "id"

        // Retrieve the entry via entrySet()
        CompactMap.CompactMapEntry entry = (CompactMap.CompactMapEntry) map.entrySet().iterator().next();

        // setValue should update the underlying map
        entry.setValue("changed");
        assertEquals("changed", map.get("id"));
        assertEquals(1, map.size());
    }

    /* ---------------------------------------------------------------------- */
    /** 13. Iterator concurrent‑modification detection. */
    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModification() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();
        map.put("a", 1);
        map.put("b", 2);

        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        it.next();

        // Modify map directly while iterator is alive
        map.put("c", 3);
        // Next call should detect the modification
        it.next();
    }

    /* ---------------------------------------------------------------------- */
    /** 14. Iterator remove works for keySet, values and entrySet. */
    @Test
    public void testIteratorRemove() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        // keySet iterator remove
        Iterator<String> keyIt = map.keySet().iterator();
        while (keyIt.hasNext()) {
            String k = keyIt.next();
            if (k.equals("two")) {
                keyIt.remove();
            }
        }
        assertFalse(map.containsKey("two"));
        assertEquals(2, map.size());

        // values iterator remove (remove value 3)
        Iterator<Integer> valIt = map.values().iterator();
        while (valIt.hasNext()) {
            Integer v = valIt.next();
            if (v == 3) {
                valIt.remove();
            }
        }
        assertFalse(map.containsValue(3));
        assertEquals(1, map.size());

        // entrySet iterator remove (remove remaining entry)
        Iterator<Map.Entry<String, Integer>> entryIt = map.entrySet().iterator();
        while (entryIt.hasNext()) {
            entryIt.next();
            entryIt.remove();
        }
        assertTrue(map.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /** 15. Verify that null keys are accepted when the backing map permits them. */
    @Test
    public void testNullKeyHandling() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .mapType(HashMap.class)   // HashMap permits null keys
                .compactSize(5)
                .build();

        map.put(null, "nullKey");
        assertEquals("nullKey", map.get(null));
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue("nullKey"));
        assertEquals(1, map.size());

        // Remove null key
        assertEquals("nullKey", map.remove(null));
        assertFalse(map.containsKey(null));
        assertEquals(0, map.size());
    }
}
