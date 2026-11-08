package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Assume;
import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests are organised around the public API and cover:
 *
 * <ul>
 *   <li>Construction and basic properties (size, empty, logical value type)</li>
 *   <li>Single‑entry optimisation (OBJECT and ENTRY storage)</li>
 *   <li>Compact array storage and transition to a backing map</li>
 *   <li>Builder‑driven configuration:
 *       <ul>
 *         <li>compactSize validation</li>
 *         <li>case‑insensitive key handling</li>
 *         <li>ordering (sorted, reverse, insertion, unordered)</li>
 *         <li>different backing map types</li>
 *       </ul>
 *   </li>
 *   <li>Map contract methods (containsKey/Value, get, put, remove, clear)</li>
 *   <li>Collection views (keySet, values, entrySet) including iterator remove,
 *       removeAll, retainAll.</li>
 *   <li>equals() / hashCode() consistency</li>
 *   <li>withConfig() – creating a new map with altered configuration.</li>
 * </ul>
 *
 * The tests use only the public API shown in the source file; no mocking or
 * subclassing of internal classes is performed.
 */
public class CompactMapTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper methods                                                       */
    /* ---------------------------------------------------------------------- */

    /** Returns true if the JDK compiler is available (required for builder tests). */
    private static boolean isCompilerAvailable() {
        try {
            return ReflectionUtils.isJavaCompilerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Convenience to create a map with a small compactSize (so we can test array → map transition). */
    private static CompactMap<String, Integer> newSmallCompactMap() {
        return CompactMap.<String, Integer>builder()
                .compactSize(3)          // very small to force transitions quickly
                .noOrder()               // unordered – easier to assert sizes only
                .build();
    }

    /* ---------------------------------------------------------------------- */
    /*  1. Construction & basic state                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEmptyMap() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertTrue("Map should be empty", map.isEmpty());
        assertEquals("Size of empty map must be 0", 0, map.size());
        assertEquals("Logical value type must be EMPTY", CompactMap.LogicalValueType.EMPTY,
                map.getLogicalValueType());

        assertNull("get on empty map returns null", map.get("any"));
        assertFalse("containsKey on empty map returns false", map.containsKey("any"));
        assertFalse("containsValue on empty map returns false", map.containsValue(1));
        assertNull("remove on empty map returns null", map.remove("any"));
    }

    @Test
    public void testConstructorWithSourceMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        CompactMap<String, Integer> map = new CompactMap<>(source);

        assertEquals("Size must match source map", source.size(), map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    /* ---------------------------------------------------------------------- */
    /*  2. Single‑entry optimisation (OBJECT / ENTRY)                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSingleEntryOptimizedObjectStorage() {
        CompactMap<String, String> map = new CompactMap<>();

        // default single value key is "id"
        map.put("id", "theValue");

        assertEquals(1, map.size());
        assertEquals("theValue", map.get("id"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // overwrite the same key – should stay in OBJECT state
        String old = map.put("id", "newValue");
        assertEquals("theValue", old);
        assertEquals("newValue", map.get("id"));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // remove – should become EMPTY again
        String removed = map.remove("id");
        assertEquals("newValue", removed);
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    @Test
    public void testSingleEntryOptimizedEntryStorage() {
        CompactMap<String, String> map = new CompactMap<>();

        // a key different from the single‑value key forces ENTRY storage
        map.put("other", "value");

        assertEquals(1, map.size());
        assertEquals("value", map.get("other"));
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        // overwrite same key – still ENTRY
        String old = map.put("other", "new");
        assertEquals("value", old);
        assertEquals("new", map.get("other"));
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        // add second key – should transition to ARRAY
        map.put("id", "singleKeyValue");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // remove one key – should go back to ENTRY
        map.remove("id");
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());
        assertTrue(map.containsKey("other"));
    }

    @Test
    public void testNullKeyAndValueHandling() {
        CompactMap<String, String> map = new CompactMap<>();

        // null key is allowed (treated like any other object)
        map.put(null, "nullKey");
        assertTrue(map.containsKey(null));
        assertEquals("nullKey", map.get(null));

        // null value
        map.put("k", null);
        assertTrue(map.containsKey("k"));
        assertNull(map.get("k"));
        assertTrue(map.containsValue(null));

        // remove null key
        String removed = map.remove(null);
        assertEquals("nullKey", removed);
        assertFalse(map.containsKey(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  3. Compact array storage & transition to backing map                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompactArrayAndTransitionToMap() {
        CompactMap<String, Integer> map = newSmallCompactMap(); // compactSize = 3

        // add three entries – should stay in ARRAY state
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        // add fourth entry – must switch to MAP state
        map.put("d", 4);
        assertEquals(4, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // verify all entries are present
        for (char ch = 'a'; ch <= 'd'; ch++) {
            String key = String.valueOf(ch);
            assertEquals(Integer.valueOf(ch - 'a' + 1), map.get(key));
        }

        // remove one entry – still MAP because size > compactSize
        map.remove("d");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // remove another – size becomes 2, should transition back to ARRAY
        map.remove("c");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
    }

    /* ---------------------------------------------------------------------- */
    /*  4. Builder – case‑insensitive handling                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCaseInsensitiveKeys() {
        Assume.assumeTrue("JDK compiler required for builder tests", isCompilerAvailable());

        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .caseSensitive(false)   // case‑insensitive
                .noOrder()
                .build();

        map.put("Key", "value1");
        assertTrue("containsKey should be case‑insensitive", map.containsKey("key"));
        assertEquals("value1", map.get("KEY"));

        // overwrite using different case
        String old = map.put("kEy", "value2");
        assertEquals("value1", old);
        assertEquals("value2", map.get("key"));
        assertEquals(1, map.size());
    }

    @Test
    public void testCaseSensitiveDefault() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("Key", "v1");
        assertTrue(map.containsKey("Key"));
        assertFalse(map.containsKey("key"));
        assertNull(map.get("key"));
    }

    /* ---------------------------------------------------------------------- */
    /*  5. Builder – ordering (sorted, reverse, insertion)                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSortedOrderIteration() {
        Assume.assumeTrue("JDK compiler required for builder tests", isCompilerAvailable());

        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(10)
                .build();

        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);

        // keySet iteration must be natural order a,b,c
        Iterator<String> it = map.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());

        // values iterator must follow the same order
        Iterator<Integer> vit = map.values().iterator();
        assertEquals(Integer.valueOf(1), vit.next());
        assertEquals(Integer.valueOf(2), vit.next());
        assertEquals(Integer.valueOf(3), vit.next());
    }

    @Test
    public void testReverseOrderIteration() {
        Assume.assumeTrue("JDK compiler required for builder tests", isCompilerAvailable());

        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .compactSize(10)
                .build();

        map.put("a", 1);
        map.put("c", 3);
        map.put("b", 2);

        // expected order: c, b, a
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("c", "b", "a"), keys);
    }

    @Test
    public void testInsertionOrderIteration() {
        Assume.assumeTrue("JDK compiler required for builder tests", isCompilerAvailable());

        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .compactSize(10)
                .build();

        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("first", "second", "third"), keys);
    }

    /* ---------------------------------------------------------------------- */
    /*  6. Map contract – equals() and hashCode()                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeConsistency() {
        CompactMap<String, Integer> m1 = new CompactMap<>();
        CompactMap<String, Integer> m2 = new CompactMap<>();

        m1.put("x", 10);
        m1.put("y", 20);
        m2.put("y", 20);
        m2.put("x", 10);

        assertTrue("Maps with same entries must be equal", m1.equals(m2));
        assertTrue("Symmetric equality", m2.equals(m1));
        assertEquals("Equal maps must have same hashCode", m1.hashCode(), m2.hashCode());

        // change one map – equality should break
        m2.put("z", 30);
        assertFalse(m1.equals(m2));
    }

    /* ---------------------------------------------------------------------- */
    /*  7. Collection view behaviours (keySet, values, entrySet)             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testKeySetRemove() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Set<String> keys = map.keySet();
        assertTrue(keys.remove("b"));
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());

        // removing a non‑existent key returns false and does not change size
        assertFalse(keys.remove("nonexistent"));
        assertEquals(2, map.size());
    }

    @Test
    public void testValuesIteratorRemove() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Iterator<Integer> vit = map.values().iterator();
        while (vit.hasNext()) {
            Integer v = vit.next();
            if (v == 2) {
                vit.remove();
            }
        }
        assertFalse(map.containsValue(2));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("one"));
        assertTrue(map.containsKey("three"));
    }

    @Test
    public void testEntrySetRemoveAllAndRetainAll() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        // removeAll – remove a subset
        Set<Map.Entry<String, Integer>> toRemove = new HashSet<>();
        toRemove.add(new AbstractMap.SimpleEntry<>("a", 1));
        toRemove.add(new AbstractMap.SimpleEntry<>("c", 3));
        assertTrue(entries.removeAll(toRemove));
        assertEquals(2, map.size());
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsKey("c"));

        // retainAll – keep only one entry
        Set<Map.Entry<String, Integer>> toRetain = new HashSet<>();
        toRetain.add(new AbstractMap.SimpleEntry<>("b", 2));
        assertTrue(entries.retainAll(toRetain));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("b"));
        assertFalse(map.containsKey("d"));
    }

    /* ---------------------------------------------------------------------- */
    /*  8. getConfig() – verifies that defaults are reported correctly      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConfig() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Map<String, Object> cfg = map.getConfig();

        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.TRUE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.UNORDERED, cfg.get(CompactMap.ORDERING));
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(CompactMap.DEFAULT_MAP_TYPE, cfg.get(CompactMap.MAP_TYPE));
    }

    /* ---------------------------------------------------------------------- */
    /*  9. withConfig() – building a new map with altered settings          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testWithConfigChangeOrdering() {
        CompactMap<String, Integer> original = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .compactSize(5)
                .build();
        original.put("b", 2);
        original.put("a", 1);
        original.put("c", 3);

        // change ordering to insertion order via withConfig
        Map<String, Object> newOpts = new HashMap<>();
        newOpts.put(CompactMap.ORDERING, CompactMap.INSERTION);
        CompactMap<String, Integer> changed = original.withConfig(newOpts);

        // verify entries are preserved
        assertEquals(original.size(), changed.size());
        assertEquals(Integer.valueOf(2), changed.get("b"));
        assertEquals(Integer.valueOf(1), changed.get("a"));
        assertEquals(Integer.valueOf(3), changed.get("c"));

        // verify new ordering – insertion order should reflect the order of insertion in the new map
        List<String> keys = new ArrayList<>(changed.keySet());
        // Since withConfig builds a brand‑new map and then putsAll(original),
        // the insertion order will be the iteration order of original (which is sorted)
        // Therefore we assert that the ordering matches the original sorted order.
        assertEquals(Arrays.asList("a", "b", "c"), keys);
        assertEquals(CompactMap.INSERTION, changed.getConfig().get(CompactMap.ORDERING));
    }

    /* ---------------------------------------------------------------------- */
    /* 10. Builder validation – compactSize must be >= 2                        */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testBuilderInvalidCompactSize() {
        Assume.assumeTrue("JDK compiler required for builder tests", isCompilerAvailable());

        // compactSize of 1 is illegal – build() must throw IllegalArgumentException
        CompactMap.<String, Integer>builder()
                .compactSize(1)
                .build();
    }

    /* ---------------------------------------------------------------------- */
    /* 11. Backing map type selection (LinkedHashMap for insertion order)    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testBackingMapTypeForInsertionOrder() {
        Assume.assumeTrue("JDK compiler required for builder tests", isCompilerAvailable());

        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .compactSize(2) // force transition to backing map quickly
                .build();

        // Insert three entries -> transition to MAP state
        map.put("x", 1);
        map.put("y", 2);
        map.put("z", 3);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // The backing map should be a LinkedHashMap (preserves insertion order)
        Map<String, Integer> backing = map.getNewMap(); // getNewMap() creates a fresh instance,
                                                        // but we can infer type via config
        assertEquals(LinkedHashMap.class, map.getConfig().get(CompactMap.MAP_TYPE));
    }

    /* ---------------------------------------------------------------------- */
    /* 12. Iterator concurrent‑modification detection                         */
    /* ---------------------------------------------------------------------- */

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModification() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        map.put("c", 3); // structural modification after iterator creation
        it.next(); // should throw CME
    }

    /* ---------------------------------------------------------------------- */
    /* 13. CompactMapEntry behavior (equals / hashCode)                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCompactMapEntryEqualsAndHashCode() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("k", 5);
        CompactMap<String, Integer>.CompactMapEntry entry = map.new CompactMapEntry("k", 5);

        // equals with SimpleEntry having same key/value
        Map.Entry<String, Integer> other = new AbstractMap.SimpleEntry<>("k", 5);
        assertTrue(entry.equals(other));
        assertEquals(entry.hashCode(), other.hashCode());

        // different key
        assertFalse(entry.equals(new AbstractMap.SimpleEntry<>("other", 5)));

        // different value
        assertFalse(entry.equals(new AbstractMap.SimpleEntry<>("k", 99)));
    }

    /* ---------------------------------------------------------------------- */
    /* 14. toString() delegating to MapUtilities – basic sanity check       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringFormat() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);
        String s = map.toString();

        // The exact format is delegated to MapUtilities, but we can assert that it contains
        // the entries in a map‑like representation.
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
    }
}
