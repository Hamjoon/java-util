package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic map contract (size, isEmpty, get, put, remove, clear)</li>
 *   <li>All internal storage states (EMPTY, OBJECT, ENTRY, ARRAY, MAP)</li>
 *   <li>Transition logic between states</li>
 *   <li>Case‑insensitive key handling</li>
 *   <li>Ordering (sorted, reverse, insertion)</li>
 *   <li>Builder validation (compactSize, illegal map types)</li>
 *   <li>equals / hashCode consistency across different internal representations</li>
 *   <li>Iterator behaviour (concurrent‑modification, remove)</li>
 *   <li>Special {@link CompactMapEntry} behaviour (setValue, equals, hashCode)</li>
 * </ul>
 */
public class CompactMapTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /** Helper to expose the protected {@code getLogicalValueType()} method. */
    private CompactMap.LogicalValueType getLogicalValueType(CompactMap<?, ?> map) throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("getLogicalValueType");
        m.setAccessible(true);
        return (CompactMap.LogicalValueType) m.invoke(map);
    }

    /** Helper to expose the protected {@code getLogicalSingleKey()} method. */
    private Object getLogicalSingleKey(CompactMap<?, ?> map) throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("getLogicalSingleKey");
        m.setAccessible(true);
        return m.invoke(map);
    }

    /** Helper to expose the protected {@code getLogicalSingleValue()} method. */
    private Object getLogicalSingleValue(CompactMap<?, ?> map) throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("getLogicalSingleValue");
        m.setAccessible(true);
        return m.invoke(map);
    }

    /* ---------------------------------------------------------------------- */
    /*  1.  Empty map basic contract                                           */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEmptyMap() throws Exception {
        CompactMap<String, Integer> map = new CompactMap<>();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertNull(map.get("any"));
        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue(123));

        // internal state must be EMPTY
        assertEquals(CompactMap.LogicalValueType.EMPTY, getLogicalValueType(map));
    }

    /* ---------------------------------------------------------------------- */
    /*  2.  Single‑entry optimized storage (key == singleValueKey)           */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testSingleEntryOptimizedStorage() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();

        // default single value key is "id"
        assertNull(map.put("id", "first"));
        assertEquals(1, map.size());
        assertEquals("first", map.get("id"));
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsValue("first"));

        // internal state should be OBJECT (value stored directly)
        assertEquals(CompactMap.LogicalValueType.OBJECT, getLogicalValueType(map));
        assertEquals("id", getLogicalSingleKey(map));
        assertEquals("first", getLogicalSingleValue(map));

        // Overwrite same key – should return previous value
        assertEquals("first", map.put("id", "second"));
        assertEquals("second", map.get("id"));
        assertEquals(1, map.size());

        // Remove the entry – map becomes EMPTY again
        assertEquals("second", map.remove("id"));
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, getLogicalValueType(map));
    }

    /* ---------------------------------------------------------------------- */
    /*  3.  Single‑entry non‑optimized storage (different key)                */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testSingleEntryNonOptimizedStorage() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();

        // key different from default single value key -> CompactMapEntry used
        assertNull(map.put("other", "value"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("other"));
        assertEquals("value", map.get("other"));

        // internal state must be ENTRY
        assertEquals(CompactMap.LogicalValueType.ENTRY, getLogicalValueType(map));
        assertEquals("other", getLogicalSingleKey(map));
        assertEquals("value", getLogicalSingleValue(map));

        // Overwrite same key – should return previous value
        assertEquals("value", map.put("other", "new"));
        assertEquals("new", map.get("other"));

        // Remove – map becomes EMPTY
        assertEquals("new", map.remove("other"));
        assertTrue(map.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /*  4.  Transition from ARRAY to MAP when exceeding compactSize        */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testArrayToMapTransition() throws Exception {
        // Use a very small compactSize to trigger transition quickly
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(3)          // 0‑2 entries stay in array, 3+ -> backing map
                .build();

        // add three entries – still ARRAY
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(CompactMap.LogicalValueType.ARRAY, getLogicalValueType(map));
        assertEquals(3, map.size());

        // add fourth entry – should switch to MAP
        map.put("d", 4);
        assertEquals(CompactMap.LogicalValueType.MAP, getLogicalValueType(map));
        assertEquals(4, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("d"));
    }

    /* ---------------------------------------------------------------------- */
    /*  5.  Transition from MAP back to ARRAY when size drops to compactSize */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testMapToArrayTransition() throws Exception {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(3)
                .build();

        // Fill to MAP state
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4); // now MAP
        assertEquals(CompactMap.LogicalValueType.MAP, getLogicalValueType(map));

        // Remove one entry – size becomes 3 (== compactSize) → should become ARRAY
        map.remove("a");
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, getLogicalValueType(map));
    }

    /* ---------------------------------------------------------------------- */
    /*  6.  Transition from ARRAY to SINGLE when size becomes 1 after removal */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testArrayToSingleTransitionOnRemoval() throws Exception {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(5)
                .build();

        map.put("x", 10);
        map.put("y", 20); // now ARRAY (2 entries)

        assertEquals(CompactMap.LogicalValueType.ARRAY, getLogicalValueType(map));

        // Remove one entry – should become SINGLE (ENTRY)
        map.remove("x");
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.ENTRY, getLogicalValueType(map));
        assertTrue(map.containsKey("y"));
        assertEquals(Integer.valueOf(20), map.get("y"));
    }

    /* ---------------------------------------------------------------------- */
    /*  7.  Case‑insensitive handling                                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testCaseInsensitiveKeys() {
        CompactMap<String, String> map = CompactMap.<String, String>builder()
                .caseSensitive(false)
                .build();

        map.put("KeyOne", "v1");
        assertEquals("v1", map.get("keyone"));
        assertTrue(map.containsKey("KEYONE"));
        assertFalse(map.containsKey("another"));
        // Overwrite using different case
        assertEquals("v1", map.put("KEYONE", "v2"));
        assertEquals("v2", map.get("keyone"));
    }

    /* ---------------------------------------------------------------------- */
    /*  8.  Sorted ordering (natural)                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testSortedOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sortedOrder()
                .build();

        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);

        Iterator<String> it = map.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());

        // Verify entrySet order matches key order
        Iterator<Map.Entry<String, Integer>> eit = map.entrySet().iterator();
        assertEquals("a", eit.next().getKey());
        assertEquals("b", eit.next().getKey());
        assertEquals("c", eit.next().getKey());
    }

    /* ---------------------------------------------------------------------- */
    /*  9.  Reverse ordering                                                   */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testReverseOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .reverseOrder()
                .build();

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Iterator<String> it = map.keySet().iterator();
        assertEquals("c", it.next());
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    /* ---------------------------------------------------------------------- */
    /* 10. Insertion order                                                     */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testInsertionOrderIteration() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .insertionOrder()
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

    /* ---------------------------------------------------------------------- */
    /* 11. Builder sourceMap initialization                                      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testBuilderWithSourceMap() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("x", 100);
        source.put("y", 200);

        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .sourceMap(source)
                .insertionOrder()
                .build();

        assertEquals(2, map.size());
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("y"));
        // order must follow insertion order of source map
        Iterator<String> it = map.keySet().iterator();
        assertEquals("x", it.next());
        assertEquals("y", it.next());
    }

    /* ---------------------------------------------------------------------- */
    /* 12. equals / hashCode across different internal representations          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCodeAcrossStates() throws Exception {
        // Map A: built with builder, compactSize 2 (forces MAP after 3 entries)
        CompactMap<String, Integer> a = CompactMap.<String, Integer>builder()
                .compactSize(2)
                .build();
        a.put("one", 1);
        a.put("two", 2);
        a.put("three", 3); // now MAP

        // Map B: default constructor, same entries added one‑by‑one
        CompactMap<String, Integer> b = new CompactMap<>();
        b.put("one", 1);
        b.put("two", 2);
        b.put("three", 3); // also MAP (default compactSize 70, still ARRAY but same entries)

        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());

        // Verify internal types differ (one MAP, the other ARRAY)
        assertEquals(CompactMap.LogicalValueType.MAP, getLogicalValueType(a));
        // b stays in ARRAY because default compactSize is large
        assertEquals(CompactMap.LogicalValueType.ARRAY, getLogicalValueType(b));
    }

    /* ---------------------------------------------------------------------- */
    /* 13. entrySet iterator remove                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEntrySetIteratorRemove() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getKey().equals("b")) {
                it.remove();
            }
        }

        assertEquals(2, map.size());
        assertFalse(map.containsKey("b"));
    }

    /* ---------------------------------------------------------------------- */
    /* 14. keySet iterator remove                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testKeySetIteratorRemove() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String k = it.next();
            if (k.equals("y")) {
                it.remove();
            }
        }

        assertEquals(2, map.size());
        assertFalse(map.containsKey("y"));
    }

    /* ---------------------------------------------------------------------- */
    /* 15. values iterator remove                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testValuesIteratorRemove() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("p", 5);
        map.put("q", 6);
        map.put("r", 7);

        Iterator<Integer> it = map.values().iterator();
        while (it.hasNext()) {
            Integer v = it.next();
            if (v == 6) {
                it.remove();
            }
        }

        assertEquals(2, map.size());
        assertFalse(map.containsValue(6));
    }

    /* ---------------------------------------------------------------------- */
    /* 16. CompactMapEntry setValue writes through to parent map               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testCompactMapEntrySetValue() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("foo", "bar"); // not the singleValueKey -> ENTRY

        // Retrieve the entry via entrySet iterator
        CompactMap<String, String>.CompactEntryIterator entryIt =
                (CompactMap<String, String>.CompactEntryIterator) map.entrySet().iterator();

        Map.Entry<String, String> entry = entryIt.next();
        assertTrue(entry instanceof CompactMap.CompactMapEntry);

        // Change the value through the entry
        entry.setValue("baz");
        assertEquals("baz", map.get("foo"));
    }

    /* ---------------------------------------------------------------------- */
    /* 17. clear resets to EMPTY state                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testClearResetsState() throws Exception {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("id", 1); // optimized OBJECT state
        map.clear();

        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, getLogicalValueType(map));
    }

    /* ---------------------------------------------------------------------- */
    /* 18. Builder validation – compactSize < 2 throws IllegalArgumentException */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testBuilderInvalidCompactSize() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("compactSize must be >= 2");

        CompactMap.<String, Integer>builder()
                .compactSize(1)   // illegal
                .build();
    }

    /* ---------------------------------------------------------------------- */
    /* 19. Builder validation – unsupported map types (IdentityHashMap)        */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testBuilderInvalidMapType() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("IdentityHashMap is not supported");

        CompactMap.<String, Integer>builder()
                .mapType(IdentityHashMap.class)
                .build();
    }

    /* ---------------------------------------------------------------------- */
    /* 20. toString produces a non‑null representation                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToStringNonNull() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        String s = map.toString();
        assertNotNull(s);
        assertTrue(s.contains("a=1"));
        assertTrue(s.contains("b=2"));
    }

    /* ---------------------------------------------------------------------- */
    /* 21. containsValue works for null values                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testContainsValueWithNull() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("k1", null);
        assertTrue(map.containsValue(null));
        assertFalse(map.containsValue("not-present"));
    }

    /* ---------------------------------------------------------------------- */
    /* 22. remove on empty map returns null                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testRemoveOnEmptyReturnsNull() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertNull(map.remove("nothing"));
    }

    /* ---------------------------------------------------------------------- */
    /* 23. putAll with null or empty source does nothing (no NPE)             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testPutAllWithNullOrEmpty() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.putAll(Collections.emptyMap()); // should be a no‑op

        // Null map should cause NPE according to Javadoc – verify it
        thrown.expect(NullPointerException.class);
        map.putAll(null);
    }

    /* ---------------------------------------------------------------------- */
    /* 24. verify that CompactMapComparator respects case‑insensitivity       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testCompactMapComparatorCaseInsensitive() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(true, false);
        assertEquals(0, comp.compare("ABC", "abc"));
        assertTrue(comp.compare("a", "b") < 0);
    }

    /* ---------------------------------------------------------------------- */
    /* 25. verify that CompactMapComparator respects reverse ordering          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testCompactMapComparatorReverse() {
        CompactMap.CompactMapComparator comp = new CompactMap.CompactMapComparator(false, true);
        assertTrue(comp.compare("a", "b") > 0); // reversed
    }

    /* ---------------------------------------------------------------------- */
    /* 26. verify that iterator throws ConcurrentModificationException      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testIteratorConcurrentModification() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);
        Iterator<String> it = map.keySet().iterator();
        assertEquals("one", it.next());

        // Modify map directly
        map.put("three", 3);

        thrown.expect(ConcurrentModificationException.class);
        it.next(); // should detect concurrent modification
    }

    /* ---------------------------------------------------------------------- */
    /* 27. verify that remove via entrySet iterator works when map is in MAP state */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEntrySetRemoveWhenBackingMap() {
        // Force MAP state by using a tiny compactSize
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(2)
                .build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3); // now MAP

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if (e.getKey().equals("b")) {
                it.remove();
            }
        }
        assertEquals(2, map.size());
        assertFalse(map.containsKey("b"));
    }

    /* ---------------------------------------------------------------------- */
    /* 28. verify that put with null key behaves according to underlying map (HashMap allows null) */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testPutNullKeyAllowedByHashMap() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put(null, 42);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(42), map.get(null));
    }

    /* ---------------------------------------------------------------------- */
    /* 29. verify that builder with caseSensitive(false) and sortedOrder() works */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testCaseInsensitiveSortedOrder() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .sortedOrder()
                .build();

        map.put("b", 2);
        map.put("A", 1);
        map.put("c", 3);

        Iterator<String> it = map.keySet().iterator();
        assertEquals("A", it.next()); // case‑insensitive sort treats "A" and "a" same; natural order
        assertEquals("b", it.next());
        assertEquals("c", it.next());
    }

    /* ---------------------------------------------------------------------- */
    /* 30. verify that equals returns false for non‑Map objects                */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEqualsWithNonMap() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("k", 1);
        assertFalse(map.equals("not a map"));
    }

}
