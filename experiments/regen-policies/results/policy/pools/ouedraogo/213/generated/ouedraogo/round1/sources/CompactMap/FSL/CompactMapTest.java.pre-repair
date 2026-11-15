package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests focus on the core functionality that is available when a
 * {@link CompactMap} instance is created via the public constructor
 * (i.e. without using the Builder which requires a JDK compiler at runtime).
 *
 * The suite verifies:
 * <ul>
 *   <li>initial empty state and {@link #isEmpty()}</li>
 *   <li>single‑value optimisation using the default {@code "id"} key</li>
 *   <li>transition from OBJECT → ENTRY → ARRAY storage</li>
 *   <li>basic {@code put}, {@code get}, {@code containsKey}/{@code containsValue}</li>
 *   <li>removal logic and transition back to EMPTY</li>
 *   <li>iteration (key, value, entry) including {@code remove()} and concurrent‑modification detection</li>
 *   <li>equality and {@code hashCode()} contract</li>
 *   <li>exposure of the internal {@link LogicalValueType}</li>
 * </ul>
 *
 * All tests use only the public API declared in the source file.
 */
public class CompactMapTest {

    /** Helper to create a fresh map for each test. */
    private CompactMap<String, Object> newMap() {
        return new CompactMap<>();
    }

    @Test
    public void testEmptyMapState() {
        CompactMap<String, Object> map = newMap();
        assertTrue("Map should be empty initially", map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());

        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue("any"));
        assertNull(map.get("any"));
        assertEquals("{}", map.toString());
    }

    @Test
    public void testSingleValueOptimisation() {
        CompactMap<String, Object> map = newMap();

        // The default single‑value key is "id"
        map.put("id", "single");
        assertEquals(1, map.size());
        assertEquals("single", map.get("id"));
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsValue("single"));

        // The internal state should be OBJECT (value stored directly)
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // Overwrite the same key – value should be replaced
        Object old = map.put("id", "new");
        assertEquals("single", old);
        assertEquals("new", map.get("id"));
    }

    @Test
    public void testTransitionToEntryAndArray() {
        CompactMap<String, Object> map = newMap();

        // First entry uses the single‑value optimisation (OBJECT)
        map.put("id", 1);
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // Adding a second key forces transition to ENTRY then ARRAY
        map.put("foo", "bar");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Verify both entries are present
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("foo"));
        assertEquals(1, map.get("id"));
        assertEquals("bar", map.get("foo"));
    }

    @Test
    public void testContainsAndGetBehaviour() {
        CompactMap<String, Object> map = newMap();

        map.put("id", "value1");
        map.put("alpha", "value2");
        map.put("beta", "value3");

        // containsKey / containsValue
        assertTrue(map.containsKey("alpha"));
        assertTrue(map.containsValue("value3"));
        assertFalse(map.containsKey("gamma"));
        assertFalse(map.containsValue("valueX"));

        // get for existing and missing keys
        assertEquals("value2", map.get("alpha"));
        assertNull(map.get("gamma"));
    }

    @Test
    public void testRemoveAndStateTransitions() {
        CompactMap<String, Object> map = newMap();

        // Populate three entries (OBJECT → ARRAY)
        map.put("id", "v1");
        map.put("a", "v2");
        map.put("b", "v3");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // Remove a non‑existent key – map unchanged
        assertNull(map.remove("notThere"));
        assertEquals(3, map.size());

        // Remove one entry – still ARRAY
        Object removed = map.remove("a");
        assertEquals("v2", removed);
        assertEquals(2, map.size());

        // Remove second entry – should transition back to ENTRY (single entry)
        map.remove("b");
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        // Remove the last entry – map becomes EMPTY
        map.remove("id");
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    @Test
    public void testKeySetIterationAndRemove() {
        CompactMap<String, Integer> map = newMap();
        map.put("id", 1);
        map.put("x", 2);
        map.put("y", 3);

        Set<String> keys = map.keySet();
        List<String> iterated = new ArrayList<>();
        for (String k : keys) {
            iterated.add(k);
        }
        // The order is undefined for the default unordered map – just verify contents
        assertTrue(iterated.containsAll(Arrays.asList("id", "x", "y")));
        assertEquals(3, iterated.size());

        // Remove via iterator
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String k = it.next();
            if ("x".equals(k)) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("x"));
        assertEquals(2, map.size());

        // Attempting to remove again without next() should throw IllegalStateException
        Iterator<String> it2 = keys.iterator();
        it2.next(); // advance once
        it2.remove(); // valid removal
        try {
            it2.remove();
            fail("Expected IllegalStateException when calling remove() twice without next()");
        } catch (IllegalStateException expected) {
            // expected
        }
    }

    @Test
    public void testValueCollectionIterationAndRemove() {
        CompactMap<String, String> map = newMap();
        map.put("id", "a");
        map.put("b", "b");
        map.put("c", "c");

        Collection<String> values = map.values();
        List<String> list = new ArrayList<>(values);
        assertTrue(list.containsAll(Arrays.asList("a", "b", "c")));
        assertEquals(3, list.size());

        // Remove a value via iterator – should delete the corresponding entry
        Iterator<String> vit = values.iterator();
        while (vit.hasNext()) {
            if ("b".equals(vit.next())) {
                vit.remove();
            }
        }
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());
    }

    @Test
    public void testEntrySetIterationAndRemove() {
        CompactMap<String, Integer> map = newMap();
        map.put("id", 10);
        map.put("p", 20);
        map.put("q", 30);

        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(3, entries.size());

        // Verify that entry equality works (uses CompactMapEntry.equals)
        Map.Entry<String, Integer> expected = new AbstractMap.SimpleEntry<>("p", 20);
        assertTrue(entries.contains(expected));

        // Remove an entry via iterator
        Iterator<Map.Entry<String, Integer>> eit = entries.iterator();
        while (eit.hasNext()) {
            Map.Entry<String, Integer> e = eit.next();
            if ("q".equals(e.getKey())) {
                eit.remove();
            }
        }
        assertFalse(map.containsKey("q"));
        assertEquals(2, map.size());
    }

    @Test
    public void testEqualsAndHashCodeContract() {
        CompactMap<String, String> map1 = newMap();
        map1.put("id", "v1");
        map1.put("a", "v2");

        CompactMap<String, String> map2 = newMap();
        map2.put("a", "v2");
        map2.put("id", "v1");

        // Equality should be true irrespective of insertion order
        assertTrue(map1.equals(map2));
        assertTrue(map2.equals(map1));
        assertEquals(map1.hashCode(), map2.hashCode());

        // Different content – not equal
        map2.put("extra", "x");
        assertFalse(map1.equals(map2));
        assertNotEquals(map1.hashCode(), map2.hashCode());

        // Equality with a plain HashMap
        Map<String, String> plain = new HashMap<>();
        plain.put("id", "v1");
        plain.put("a", "v2");
        assertTrue(map1.equals(plain));
        assertTrue(plain.equals(map1));
    }

    @Test
    public void testConcurrentModificationDetection() {
        CompactMap<String, Integer> map = newMap();
        map.put("id", 1);
        map.put("x", 2);
        map.put("y", 3);

        Iterator<String> keyIt = map.keySet().iterator();
        assertTrue(keyIt.hasNext());
        keyIt.next(); // advance

        // Modify the map directly while iterator is active
        map.put("z", 4);
        try {
            keyIt.hasNext();
            fail("Expected ConcurrentModificationException after external modification");
        } catch (ConcurrentModificationException expected) {
            // expected
        }

        // Same for entry iterator
        Iterator<Map.Entry<String, Integer>> entryIt = map.entrySet().iterator();
        entryIt.next();
        map.remove("x");
        try {
            entryIt.next();
            fail("Expected ConcurrentModificationException after removal");
        } catch (ConcurrentModificationException expected) {
            // expected
        }
    }

    @Test
    public void testCompactMapComparatorBehaviour() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, false);
        assertTrue(comp.compare(1, 2) < 0);
        assertEquals(0, comp.compare("a", "a"));
        assertTrue(comp.compare("b", "a") > 0);

        // Reverse order
        CompactMap.CompactMapComparator rev = new CompactMap.CompactMapComparator(false, true);
        assertTrue(rev.compare(1, 2) > 0);
        assertTrue(rev.compare("a", "b") > 0);

        // Case‑insensitive
        CompactMap.CompactMapComparator ci = new CompactMap.CompactMapComparator(true, false);
        assertEquals(0, ci.compare("TeSt", "test"));
    }

    @Test
    public void testGetConfigReflectsDefaults() {
        CompactMap<String, String> map = newMap();
        Map<String, Object> cfg = map.getConfig();

        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.TRUE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.UNORDERED, cfg.get(CompactMap.ORDERING));
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(CompactMap.DEFAULT_MAP_TYPE, cfg.get(CompactMap.MAP_TYPE));
    }
}
