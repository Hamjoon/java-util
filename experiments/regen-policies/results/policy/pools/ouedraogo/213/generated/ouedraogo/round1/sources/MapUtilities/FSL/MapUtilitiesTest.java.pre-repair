package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;

import static org.junit.Assert.*;

public class MapUtilitiesTest {

    private Map<String, Integer> sampleMap;

    @Before
    public void setUp() {
        sampleMap = new HashMap<>();
        sampleMap.put("one", 1);
        sampleMap.put("two", 2);
    }

    /* ---------- get ---------- */
    @Test
    public void testGetFound() {
        Integer val = MapUtilities.get(sampleMap, "one", -1);
        assertEquals(Integer.valueOf(1), val);
    }

    @Test
    public void testGetNotFoundReturnsDefault() {
        Integer val = MapUtilities.get(sampleMap, "missing", -1);
        assertEquals(Integer.valueOf(-1), val);
    }

    @Test
    public void testGetNullValueReturnsDefault() {
        sampleMap.put("nullKey", null);
        String result = MapUtilities.get(sampleMap, "nullKey", "def");
        assertEquals("def", result);
    }

    /* ---------- getOrThrow ---------- */
    @Test
    public void testGetOrThrowFound() throws Exception {
        Object value = MapUtilities.getOrThrow(sampleMap, "two", new IllegalStateException("missing"));
        assertEquals(2, value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOrThrowMissingThrowsProvided() throws Throwable {
        MapUtilities.getOrThrow(sampleMap, "absent", new IllegalArgumentException("not present"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullMap() throws Throwable {
        MapUtilities.getOrThrow(null, "any", new IllegalArgumentException());
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullThrowable() throws Throwable {
        MapUtilities.getOrThrow(sampleMap, "any", null);
    }

    /* ---------- isEmpty ---------- */
    @Test
    public void testIsEmptyNull() {
        assertTrue(MapUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmptyEmptyMap() {
        assertTrue(MapUtilities.isEmpty(new HashMap<>()));
    }

    @Test
    public void testIsEmptyNonEmptyMap() {
        assertFalse(MapUtilities.isEmpty(sampleMap));
    }

    /* ---------- dupe ---------- */
    @Test
    public void testDupeModifiable() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        original.put(String.class, new HashSet<>(Arrays.asList("a", "b")));
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, false);

        assertNotSame(original, copy);
        assertEquals(original, copy);
        // modify copy should not affect original
        copy.get(String.class).add("c");
        assertFalse(original.get(String.class).contains("c"));
    }

    @Test
    public void testDupeUnmodifiable() {
        Map<Class<?>, Set<Integer>> original = new LinkedHashMap<>();
        original.put(Integer.class, new HashSet<>(Arrays.asList(1, 2)));
        Map<Class<?>, Set<Integer>> copy = MapUtilities.dupe(original, true);

        assertNotSame(original, copy);
        assertEquals(original, copy);
        // map itself should be unmodifiable
        try {
            copy.put(Double.class, new HashSet<>());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
        // inner set should also be unmodifiable
        try {
            copy.get(Integer.class).add(3);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    /* ---------- cloneMapOfSets ---------- */
    @Test
    public void testCloneMapOfSetsMutable() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("x", new HashSet<>(Arrays.asList(1, 2)));
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);

        assertNotSame(original, clone);
        assertEquals(original, clone);
        // modifying clone should not affect original
        clone.get("x").add(3);
        assertFalse(original.get("x").contains(3));
        // original modifications should not affect clone
        original.get("x").add(4);
        assertFalse(clone.get("x").contains(4));
    }

    @Test
    public void testCloneMapOfSetsImmutable() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("y", new HashSet<>(Arrays.asList(5, 6)));
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, true);

        assertNotSame(original, clone);
        assertEquals(original, clone);
        // map should be unmodifiable
        try {
            clone.put("z", new HashSet<>());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
        // inner set should be unmodifiable
        try {
            clone.get("y").add(7);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    /* ---------- cloneMapOfMaps ---------- */
    @Test
    public void testCloneMapOfMapsMutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        original.put("inner", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);
        assertNotSame(original, clone);
        assertEquals(original, clone);

        // modify clone's inner map
        clone.get("inner").put(2, "two");
        assertFalse(original.get("inner").containsKey(2));
    }

    @Test
    public void testCloneMapOfMapsImmutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(10, "ten");
        original.put("inner", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, true);
        assertNotSame(original, clone);
        assertEquals(original, clone);

        // map should be immutable
        try {
            clone.put("new", new HashMap<>());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
        // inner map should be immutable
        try {
            clone.get("inner").put(11, "eleven");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
        }
    }

    /* ---------- mapToString ---------- */
    @Test
    public void testMapToStringEmpty() {
        assertEquals("{}", MapUtilities.mapToString(Collections.emptyMap()));
    }

    @Test
    public void testMapToStringSimple() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        assertEquals("{a=1, b=2}", MapUtilities.mapToString(m));
    }

    @Test
    public void testMapToStringSelfReference() {
        Map<Object, Object> selfMap = new LinkedHashMap<>();
        selfMap.put(selfMap, selfMap);
        assertEquals("{(this Map)=(this Map)}", MapUtilities.mapToString(selfMap));
    }

    /* ---------- mapOf ---------- */
    @Test
    public void testMapOfEmpty() {
        Map<?, ?> empty = MapUtilities.mapOf();
        assertTrue(empty.isEmpty());
        try {
            empty.put("x", "y");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testMapOfValidPairs() {
        Map<String, Integer> m = MapUtilities.mapOf("one", 1, "two", 2);
        assertEquals(2, m.size());
        assertEquals(Integer.valueOf(1), m.get("one"));
        assertEquals(Integer.valueOf(2), m.get("two"));
        // immutable
        try {
            m.put("three", 3);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapOfOddNumberOfArguments() {
        MapUtilities.mapOf("keyOnly");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapOfTooManyEntries() {
        // 11 entries => 22 arguments, exceeds MAX_ENTRIES (10)
        Object[] args = new Object[22];
        for (int i = 0; i < 22; i++) args[i] = i;
        MapUtilities.mapOf(args);
    }

    /* ---------- mapOfEntries ---------- */
    @Test
    public void testMapOfEntriesEmpty() {
        Map<?, ?> empty = MapUtilities.mapOfEntries();
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testMapOfEntriesValid() {
        Map.Entry<String, Integer> e1 = new SimpleEntry<>("a", 1);
        Map.Entry<String, Integer> e2 = new SimpleEntry<>("b", 2);
        Map<String, Integer> m = MapUtilities.mapOfEntries(e1, e2);
        assertEquals(2, m.size());
        assertEquals(Integer.valueOf(1), m.get("a"));
        assertEquals(Integer.valueOf(2), m.get("b"));
        try {
            m.put("c", 3);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test(expected = NullPointerException.class)
    public void testMapOfEntriesNullEntry() {
        MapUtilities.mapOfEntries((Map.Entry<String, Integer>) null);
    }

    /* ---------- getMapStructureString ---------- */
    @Test
    public void testGetMapStructureStringNull() {
        assertEquals("null", MapUtilities.getMapStructureString(null));
    }

    @Test
    public void testGetMapStructureStringSimpleMap() {
        Map<String, Integer> map = new HashMap<>();
        String s = MapUtilities.getMapStructureString(map);
        // HashMap has no special wrapper, expect class name
        assertTrue(s.contains("HashMap"));
    }

    @Test
    public void testGetMapStructureStringLinkedHashMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String s = MapUtilities.getMapStructureString(map);
        assertTrue(s.contains("LinkedHashMap"));
    }

    /* ---------- detectMapOrdering ---------- */
    @Test
    public void testDetectMapOrderingNull() {
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(null));
    }

    @Test
    public void testDetectMapOrderingLinkedHashMap() {
        Map<String, Integer> linked = new LinkedHashMap<>();
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(linked));
    }

    @Test
    public void testDetectMapOrderingEnumMap() {
        EnumMap<TestEnum, String> enumMap = new EnumMap<>(TestEnum.class);
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(enumMap));
    }

    @Test
    public void testDetectMapOrderingTreeMap() {
        Map<String, Integer> tree = new TreeMap<>();
        assertEquals(CompactMap.SORTED, MapUtilities.detectMapOrdering(tree));
    }

    @Test
    public void testDetectMapOrderingHashMap() {
        Map<String, Integer> hash = new HashMap<>();
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(hash));
    }

    // Helper enum for EnumMap test
    private enum TestEnum { A, B, C }
}
