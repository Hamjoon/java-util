package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import static org.junit.Assert.*;

public class CompactMapTest {

    private CompactMap<String, Integer> defaultMap;

    @Before
    public void setUp() {
        defaultMap = new CompactMap<>();
    }

    /*** Basic empty map behavior ***/
    @Test
    public void testEmptyMap() {
        assertTrue(defaultMap.isEmpty());
        assertEquals(0, defaultMap.size());
        assertEquals("{}", defaultMap.toString());
        assertEquals(0, defaultMap.hashCode());
    }

    /*** Single entry – optimized object storage (key = default singleValueKey "id") ***/
    @Test
    public void testSingleEntryOptimizedStorage() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("id", "value");
        assertEquals(1, map.size());
        assertTrue(map.containsKey("id"));
        assertEquals("value", map.get("id"));

        // Verify internal logical type is OBJECT
        CompactMap.LogicalValueType type = (CompactMap.LogicalValueType)
                CompactMap.class.getDeclaredMethod("getLogicalValueType")
                        .setAccessible(true)
                        .invoke(map);
        assertEquals(CompactMap.LogicalValueType.OBJECT, type);
    }

    /*** Single entry – NOT optimized when value is a Map or Object[] ***/
    @Test
    public void testSingleEntryNonOptimizedWhenValueIsMap() throws Exception {
        CompactMap<String, Object> map = new CompactMap<>();
        Map<String, String> inner = new HashMap<>();
        inner.put("a", "b");
        map.put("id", inner); // value is a Map, should fall back to ENTRY storage

        assertEquals(1, map.size());
        CompactMap.LogicalValueType type = (CompactMap.LogicalValueType)
                CompactMap.class.getDeclaredMethod("getLogicalValueType")
                        .setAccessible(true)
                        .invoke(map);
        assertEquals(CompactMap.LogicalValueType.ENTRY, type);
    }

    /*** Transition from EMPTY → SINGLE → ARRAY → MAP and back ***/
    @Test
    public void testTransitionsBetweenStorageStates() throws Exception {
        // compactSize = 3 forces quick transitions
        CompactMap<String, Integer> map = CompactMap.builder()
                .compactSize(3)
                .build();

        // ---- EMPTY → SINGLE (optimized) ----
        map.put("id", 10);
        assertEquals(1, map.size());
        CompactMap.LogicalValueType type = (CompactMap.LogicalValueType)
                CompactMap.class.getDeclaredMethod("getLogicalValueType")
                        .setAccessible(true)
                        .invoke(map);
        assertEquals(CompactMap.LogicalValueType.OBJECT, type);

        // ---- SINGLE → ARRAY (2 entries) ----
        map.put("a", 1);
        assertEquals(2, map.size());
        type = (CompactMap.LogicalValueType) CompactMap.class
                .getDeclaredMethod("getLogicalValueType")
                .setAccessible(true)
                .invoke(map);
        assertEquals(CompactMap.LogicalValueType.ARRAY, type);

        // ---- ARRAY → ARRAY (still within compactSize) ----
        map.put("b", 2);
        assertEquals(3, map.size());
        type = (CompactMap.LogicalValueType) CompactMap.class
                .getDeclaredMethod("getLogicalValueType")
                .setAccessible(true)
                .invoke(map);
        assertEquals(CompactMap.LogicalValueType.ARRAY, type);

        // ---- ARRAY → MAP (exceeds compactSize) ----
        map.put("c", 3);
        assertEquals(4, map.size());
        type = (CompactMap.LogicalValueType) CompactMap.class
                .getDeclaredMethod("getLogicalValueType")
                .setAccessible(true)
                .invoke(map);
        assertEquals(CompactMap.LogicalValueType.MAP, type);

        // ---- MAP → ARRAY (remove to compactSize) ----
        map.remove("c"); // size goes back to 3 == compactSize
        assertEquals(3, map.size());
        type = (CompactMap.LogicalValueType) CompactMap.class
                .getDeclaredMethod("getLogicalValueType")
                .setAccessible(true)
                .invoke(map);
        assertEquals(CompactMap.LogicalValueType.ARRAY, type);

        // ---- ARRAY → SINGLE (remove to 1) ----
        map.remove("a");
        map.remove("b");
        assertEquals(1, map.size());
        type = (CompactMap.LogicalValueType) CompactMap.class
                .getDeclaredMethod("getLogicalValueType")
                .setAccessible(true)
                .invoke(map);
        assertEquals(CompactMap.LogicalValueType.OBJECT, type);
    }

    /*** Case‑insensitive key handling ***/
    @Test
    public void testCaseInsensitiveKeys() {
        CompactMap<String, String> map = CompactMap.builder()
                .caseSensitive(false) // make it case‑insensitive
                .compactSize(5)
                .build();

        map.put("Key", "value");
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsKey("KEY"));
        assertEquals("value", map.get("kEy"));
    }

    /*** Sorted ordering – natural order ***/
    @Test
    public void testSortedOrdering() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .sortedOrder()
                .compactSize(5)
                .build();

        map.put("b", 2);
        map.put("c", 3);
        map.put("a", 1);

        Iterator<String> it = map.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());
    }

    /*** Reverse ordering – descending ***/
    @Test
    public void testReverseOrdering() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .reverseOrder()
                .compactSize(5)
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

    /*** Insertion ordering ***/
    @Test
    public void testInsertionOrder() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .insertionOrder()
                .compactSize(5)
                .build();

        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        Iterator<String> it = map.keySet().iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
        assertFalse(it.hasNext());
    }

    /*** Equality and hashCode with standard maps ***/
    @Test
    public void testEqualsAndHashCode() {
        CompactMap<String, Integer> map1 = CompactMap.builder()
                .compactSize(5)
                .build();
        map1.put("x", 10);
        map1.put("y", 20);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("x", 10);
        map2.put("y", 20);

        assertTrue(map1.equals(map2));
        assertTrue(map2.equals(map1));
        assertEquals(map1.hashCode(), map2.hashCode());

        // Different content
        map2.put("z", 30);
        assertFalse(map1.equals(map2));
    }

    /*** entrySet iterator returns CompactMapEntry and setValue writes through ***/
    @Test
    public void testEntrySetAndCompactMapEntrySetValue() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("k", "old");

        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        assertEquals(1, entrySet.size());

        Map.Entry<String, String> entry = entrySet.iterator().next();
        assertTrue(entry instanceof CompactMap.CompactMapEntry);
        CompactMap.CompactMapEntry cmapEntry = (CompactMap.CompactMapEntry) entry;

        // setValue should update the map as well
        cmapEntry.setValue("new");
        assertEquals("new", map.get("k"));
    }

    /*** keySet remove operation ***/
    @Test
    public void testKeySetRemove() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        assertTrue(map.keySet().remove("b"));
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());
    }

    /*** values collection removal ***/
    @Test
    public void testValuesRemove() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        assertTrue(map.values().remove(2));
        assertFalse(map.containsValue(2));
        assertEquals(2, map.size());
    }

    /*** iterator behavior – NoSuchElementException and IllegalStateException ***/
    @Test(expected = NoSuchElementException.class)
    public void testIteratorNoSuchElementException() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("only", 42);
        Iterator<Integer> it = map.values().iterator();
        assertTrue(it.hasNext());
        it.next(); // first element
        assertFalse(it.hasNext());
        it.next(); // should throw NoSuchElementException
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorIllegalStateExceptionOnRemoveWithoutNext() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 1);
        Iterator<Integer> it = map.values().iterator();
        it.remove(); // illegal because next() not called yet
    }

    /*** iterator concurrent modification detection ***/
    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModification() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        map.put("c", 3); // modify map after iterator creation
        it.next(); // should throw CME
    }

    /*** CompactMapComparator behavior ***/
    @Test
    public void testCompactMapComparator() {
        CompactMap.CompactMapComparator cmp = new CompactMap.CompactMapComparator(true, false);
        // case‑insensitive ordering
        assertEquals(0, cmp.compare("abc", "ABC"));
        assertTrue(cmp.compare("a", "b") < 0);

        // reverse ordering
        CompactMap.CompactMapComparator revCmp = new CompactMap.CompactMapComparator(false, true);
        assertTrue(revCmp.compare("a", "b") > 0);
    }

    /*** clear resets to empty state ***/
    @Test
    public void testClear() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 100);
        map.put("y", 200);
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /*** retainAll on entrySet works correctly ***/
    @Test
    public void testEntrySetRetainAll() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Set<Map.Entry<String, Integer>> entrySet = map.entrySet();
        // retain only entry a=1 and c=3
        Set<Map.Entry<String, Integer>> toRetain = new java.util.HashSet<>();
        toRetain.add(new AbstractMap.SimpleEntry<>("a", 1));
        toRetain.add(new AbstractMap.SimpleEntry<>("c", 3));

        assertTrue(entrySet.retainAll(toRetain));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("b"));
    }

    /*** removeAll on keySet works correctly ***/
    @Test
    public void testKeySetRemoveAll() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Set<String> toRemove = new java.util.HashSet<>();
        toRemove.add("one");
        toRemove.add("three");

        assertTrue(map.keySet().removeAll(toRemove));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("two"));
        assertFalse(map.containsKey("one"));
        assertFalse(map.containsKey("three"));
    }

    /*** verify that builder respects supplied map type (LinkedHashMap → insertion order) ***/
    @Test
    public void testBuilderWithMapTypeInsertionOrder() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .mapType(LinkedHashMap.class)
                .insertionOrder()
                .compactSize(5)
                .build();

        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        Iterator<String> it = map.keySet().iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
    }

    /*** verify that builder with compactSize less than 2 throws IllegalArgumentException ***/
    @Test(expected = IllegalArgumentException.class)
    public void testBuilderInvalidCompactSize() {
        CompactMap.builder().compactSize(1).build(); // compactSize must be >= 2
    }

    /*** verify that map created from another map copies entries correctly ***/
    @Test
    public void testCopyConstructor() {
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 10);
        source.put("y", 20);
        CompactMap<String, Integer> copy = new CompactMap<>(source);
        assertEquals(2, copy.size());
        assertEquals(Integer.valueOf(10), copy.get("x"));
        assertEquals(Integer.valueOf(20), copy.get("y"));
        assertTrue(copy.equals(source));
    }

    /*** verify that containsValue works for null values ***/
    @Test
    public void testContainsValueWithNull() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("a", null);
        assertTrue(map.containsValue(null));
        assertFalse(map.containsValue("notPresent"));
    }

    /*** verify that putAll respects compactSize threshold ***/
    @Test
    public void testPutAllTransition() {
        CompactMap<String, Integer> map = CompactMap.builder()
                .compactSize(3)
                .build();

        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        src.put("c", 3);
        src.put("d", 4); // exceeds compactSize

        map.putAll(src);
        assertEquals(4, map.size());

        CompactMap.LogicalValueType type = (CompactMap.LogicalValueType)
                CompactMap.class.getDeclaredMethod("getLogicalValueType")
                        .setAccessible(true)
                        .invoke(map);
        assertEquals(CompactMap.LogicalValueType.MAP, type);
    }

    /*** verify that toString contains all key‑value pairs ***/
    @Test
    public void testToStringContents() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("one", 1);
        map.put("two", 2);
        String s = map.toString();
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }
}
