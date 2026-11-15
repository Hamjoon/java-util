package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import java.util.*;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 test suite for {@link CompactMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction and basic empty‑state behavior</li>
 *   <li>Single‑entry optimisations (OBJECT and ENTRY storage)</li>
 *   <li>Transitions between the internal storage states (ARRAY → MAP)</li>
 *   <li>Removal handling and transition back to SINGLE</li>
 *   <li>Standard Map API methods (putAll, clear, keySet, values, entrySet)</li>
 *   <li>Case‑insensitive key handling</li>
 *   <li>hashCode / equals contract</li>
 *   <li>toString format</li>
 *   <li>Iterator concurrent‑modification detection</li>
 *   <li>Builder validation (invalid compactSize and mapType)</li>
 * </ul>
 *
 * The test suite deliberately avoids the full template‑generation path
 * (which requires a JDK compiler) by using anonymous subclasses that
 * override {@code compactSize()} and {@code isCaseInsensitive()} where
 * needed.
 */
public class CompactMapTest {

    /** Simple subclass exposing the protected {@code getLogicalValueType()} for assertions. */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {
        private final int forcedCompactSize;
        private final boolean forcedCaseInsensitive;

        TestCompactMap() {
            this(CompactMap.DEFAULT_COMPACT_SIZE, false);
        }

        TestCompactMap(int compactSize, boolean caseInsensitive) {
            this.forcedCompactSize = compactSize;
            this.forcedCaseInsensitive = caseInsensitive;
        }

        @Override
        protected int compactSize() {
            return forcedCompactSize;
        }

        @Override
        protected boolean isCaseInsensitive() {
            return forcedCaseInsensitive;
        }

        /** Expose the internal state for verification. */
        public LogicalValueType state() {
            return super.getLogicalValueType();
        }
    }

    /** Helper to create a map with a very small compactSize so we can trigger state changes quickly. */
    private TestCompactMap<String, Integer> createSmallMap() {
        return new TestCompactMap<>(3, false); // compactSize = 3
    }

    /** Helper to create a case‑insensitive map with a tiny compactSize. */
    private TestCompactMap<String, Integer> createCaseInsensitiveMap() {
        return new TestCompactMap<>(3, true);
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic construction and empty‑state behaviour                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConstructorAndEmptyState() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertTrue("New map must be empty", map.isEmpty());
        assertEquals("Size of new map must be 0", 0, map.size());
        assertEquals("hashCode of empty map must be 0", 0, map.hashCode());
        assertEquals("toString of empty map", "{}", map.toString());
    }

    /* ---------------------------------------------------------------------- */
    /*  Single‑entry optimisations                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetSingleEntry_OptimizedObjectStorage() {
        CompactMap<String, String> map = new CompactMap<>();
        // The default single‑value key is "id"
        String previous = map.put("id", "value");
        assertNull("First put should return null", previous);
        assertEquals("Map must contain the stored value", "value", map.get("id"));
        assertEquals("Size must be 1", 1, map.size());

        // Overwrite the same key – old value should be returned
        previous = map.put("id", "new");
        assertEquals("Old value must be returned on overwrite", "value", previous);
        assertEquals("New value must be stored", "new", map.get("id"));
    }

    @Test
    public void testPutAndGetSingleEntry_CompactMapEntryStorage() {
        CompactMap<String, String> map = new CompactMap<>();
        // Use a key different from the default single‑value key
        map.put("other", "val");
        assertEquals("Size must be 1", 1, map.size());
        assertEquals("Stored value must be retrievable", "val", map.get("other"));
        assertTrue("containsKey must work", map.containsKey("other"));
        assertFalse("containsKey must be false for missing key", map.containsKey("missing"));
    }

    @Test
    public void testOverwriteSingleEntry() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("id", "first");
        String old = map.put("id", "second");
        assertEquals("Old value must be returned", "first", old);
        assertEquals("New value must replace old", "second", map.get("id"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Transition from SINGLE → ARRAY → MAP                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTransitionToArray() {
        TestCompactMap<String, Integer> map = createSmallMap(); // compactSize = 3
        map.put("a", 1);
        map.put("b", 2); // now size = 2 -> should be ARRAY state
        assertEquals("Internal state must be ARRAY after 2 entries", 
                     CompactMap.LogicalValueType.ARRAY, map.state());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testTransitionToMap() {
        TestCompactMap<String, Integer> map = createSmallMap(); // compactSize = 3
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3); // still ARRAY (size == compactSize)
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.state());
        map.put("d", 4); // exceeds compactSize -> should become MAP
        assertEquals(CompactMap.LogicalValueType.MAP, map.state());
        assertEquals(Integer.valueOf(4), map.get("d"));
        assertEquals(4, map.size());
    }

    @Test
    public void testRemoveTransitionBackToSingle() {
        TestCompactMap<String, Integer> map = createSmallMap(); // compactSize = 3
        map.put("a", 1);
        map.put("b", 2); // ARRAY state
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.state());

        // Remove one entry – size becomes 1, should transition to SINGLE (ENTRY)
        Integer removed = map.remove("b");
        assertEquals(Integer.valueOf(2), removed);
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.state());
        assertEquals(Integer.valueOf(1), map.get("a"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Bulk operations (putAll, clear)                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAllAndClear() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 10);
        source.put("y", 20);
        source.put("z", 30);
        map.putAll(source);
        assertEquals(3, map.size());
        assertTrue(map.containsKey("y"));
        assertEquals(Integer.valueOf(20), map.get("y"));

        map.clear();
        assertTrue("Map must be empty after clear()", map.isEmpty());
        assertEquals(0, map.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  Case‑insensitive key handling                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCaseInsensitiveKeys() {
        TestCompactMap<String, Integer> map = createCaseInsensitiveMap(); // case‑insensitive
        map.put("Key", 1);
        assertTrue("containsKey must be true for different case", map.containsKey("key"));
        assertEquals(Integer.valueOf(1), map.get("KEY"));
        // Overwrite using different case
        Integer old = map.put("kEy", 2);
        assertEquals(Integer.valueOf(1), old);
        assertEquals(Integer.valueOf(2), map.get("key"));
    }

    /* ---------------------------------------------------------------------- */
    /*  hashCode / equals contract                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHashCodeAndEquals() {
        CompactMap<String, Integer> map1 = new CompactMap<>();
        CompactMap<String, Integer> map2 = new CompactMap<>();

        map1.put("a", 1);
        map1.put("b", 2);
        map2.put("b", 2);
        map2.put("a", 1);

        assertEquals("Maps with same entries must be equal", map1, map2);
        assertEquals("Equal maps must have same hashCode", map1.hashCode(), map2.hashCode());

        map2.put("c", 3);
        assertNotEquals("Maps with different entries must not be equal", map1, map2);
    }

    /* ---------------------------------------------------------------------- */
    /*  toString format                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToString() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);
        String s = map.toString();
        // Order is undefined for the default unordered map, so we just check both entries are present
        assertTrue("toString must contain first entry", s.contains("one=1"));
        assertTrue("toString must contain second entry", s.contains("two=2"));
        assertTrue("toString must start with { and end with }", s.startsWith("{") && s.endsWith("}"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Collection views (keySet, values, entrySet) and iterator behavior       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testKeySetAndIterator() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Set<String> keys = map.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("a"));
        assertTrue(keys.contains("b"));

        Iterator<String> it = keys.iterator();
        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertEquals(keys, seen);
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testKeySetIteratorConcurrentModification() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        map.put("c", 3); // structural modification after iterator creation
        it.next(); // should throw CME
    }

    @Test
    public void testValuesCollection() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 10);
        map.put("y", 20);
        Collection<Integer> values = map.values();
        assertEquals(2, values.size());
        assertTrue(values.contains(10));
        assertTrue(values.contains(20));
    }

    @Test
    public void testEntrySetRemove() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("p", 5);
        map.put("q", 6);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(2, entries.size());

        // Remove one entry via the iterator
        Iterator<Map.Entry<String, Integer>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getKey().equals("p")) {
                it.remove();
                break;
            }
        }
        assertEquals(1, map.size());
        assertFalse(map.containsKey("p"));
        assertTrue(map.containsKey("q"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Builder validation (error scenarios)                                    */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsCompactSizeBelowTwo() {
        CompactMap.Builder<String, Integer> builder = CompactMap.builder();
        builder.compactSize(1).build(); // should fail during validation
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderRejectsUnsupportedMapType() {
        CompactMap.Builder<String, Integer> builder = CompactMap.builder();
        builder.mapType(IdentityHashMap.class).build(); // IdentityHashMap is explicitly rejected
    }
}
