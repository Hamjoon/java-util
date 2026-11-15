package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link MapUtilities}.
 * The tests exercise every public method and the two package‑private
 * analysis helpers that can be reached through the public API.
 */
public class MapUtilitiesTest {

    /* --------------------------------------------------------------------- */
    /*  get(..)                                                              */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetReturnsValueWhenPresent() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        assertEquals(Integer.valueOf(1), MapUtilities.get(map, "one", 99));
    }

    @Test
    public void testGetReturnsDefaultWhenNullValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("nullKey", null);
        assertEquals(Integer.valueOf(42), MapUtilities.get(map, "nullKey", 42));
    }

    @Test
    public void testGetReturnsDefaultWhenMissingKey() {
        Map<String, String> map = Collections.emptyMap();
        assertEquals("def", MapUtilities.get(map, "missing", "def"));
    }

    /* --------------------------------------------------------------------- */
    /*  getOrThrow(..)                                                       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetOrThrowReturnsValueWhenKeyPresent() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("k", "v");
        assertEquals("v", MapUtilities.getOrThrow(map, "k", new IllegalStateException()));
    }

    @Test
    public void testGetOrThrowReturnsNullWhenKeyPresentWithNullValue() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("k", null);
        assertNull(MapUtilities.getOrThrow(map, "k", new IllegalArgumentException()));
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullMapThrowsNPE() throws Throwable {
        MapUtilities.getOrThrow(null, "any", new RuntimeException());
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullThrowableThrowsNPE() throws Throwable {
        MapUtilities.getOrThrow(Collections.emptyMap(), "any", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOrThrowMissingKeyThrowsProvidedException() throws Throwable {
        Map<String, Integer> map = Collections.emptyMap();
        MapUtilities.getOrThrow(map, "missing", new IllegalArgumentException("not found"));
    }

    /* --------------------------------------------------------------------- */
    /*  isEmpty(..)                                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testIsEmptyDetectsNullAndEmptyMaps() {
        assertTrue(MapUtilities.isEmpty(null));
        assertTrue(MapUtilities.isEmpty(Collections.emptyMap()));
        assertFalse(MapUtilities.isEmpty(Collections.singletonMap("a", 1)));
    }

    /* --------------------------------------------------------------------- */
    /*  dupe(..)                                                             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testDupeCreatesShallowCopy() {
        Map<Class<?>, java.util.Set<String>> source = new LinkedHashMap<>();
        source.put(String.class, new java.util.HashSet<>(java.util.Collections.singleton("one")));

        Map<Class<?>, java.util.Set<String>> copy = MapUtilities.dupe(source, false);
        assertEquals(source, copy);
        // modifications to copy do not affect source
        copy.get(String.class).add("two");
        assertFalse(source.get(String.class).contains("two"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDupeUnmodifiableMapIsImmutable() {
        Map<Class<?>, java.util.Set<String>> source = new LinkedHashMap<>();
        source.put(Integer.class, new java.util.HashSet<>(java.util.Collections.singleton("val")));

        Map<Class<?>, java.util.Set<String>> copy = MapUtilities.dupe(source, true);
        copy.put(Long.class, java.util.Collections.emptySet()); // should fail
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testDupeUnmodifiableSetIsImmutable() {
        Map<Class<?>, java.util.Set<String>> source = new LinkedHashMap<>();
        source.put(Double.class, new java.util.HashSet<>(java.util.Collections.singleton("inner")));

        Map<Class<?>, java.util.Set<String>> copy = MapUtilities.dupe(source, true);
        copy.get(Double.class).add("new"); // should fail
    }

    /* --------------------------------------------------------------------- */
    /*  cloneMapOfSets(..)                                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCloneMapOfSetsMutable() {
        Map<String, java.util.Set<Integer>> original = new HashMap<>();
        original.put("a", new java.util.HashSet<>(java.util.Collections.singleton(1)));

        Map<String, java.util.Set<Integer>> cloned = MapUtilities.cloneMapOfSets(original, false);
        assertEquals(original, cloned);
        cloned.get("a").add(2);
        assertTrue(original.get("a").contains(2));
    }

    @Test
    public void testCloneMapOfSetsImmutable() {
        Map<String, java.util.Set<Integer>> original = new HashMap<>();
        original.put("b", new java.util.HashSet<>(java.util.Collections.singleton(10)));

        Map<String, java.util.Set<Integer>> cloned = MapUtilities.cloneMapOfSets(original, true);
        assertEquals(original, cloned);
        // the map itself is unmodifiable
        try {
            cloned.put("c", java.util.Collections.emptySet());
            fail("Expected UnsupportedOperationException for map modification");
        } catch (UnsupportedOperationException ignored) {
        }
        // the contained set is also unmodifiable
        try {
            cloned.get("b").add(20);
            fail("Expected UnsupportedOperationException for set modification");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* --------------------------------------------------------------------- */
    /*  cloneMapOfMaps(..)                                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCloneMapOfMapsMutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        original.put("map", inner);

        Map<String, Map<Integer, String>> cloned = MapUtilities.cloneMapOfMaps(original, false);
        assertEquals(original, cloned);
        cloned.get("map").put(2, "two");
        assertTrue(original.get("map").containsKey(2));
    }

    @Test
    public void testCloneMapOfMapsImmutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(5, "five");
        original.put("inner", inner);

        Map<String, Map<Integer, String>> cloned = MapUtilities.cloneMapOfMaps(original, true);
        // map itself immutable
        try {
            cloned.put("new", Collections.emptyMap());
            fail("Expected UnsupportedOperationException for map modification");
        } catch (UnsupportedOperationException ignored) {
        }
        // inner map immutable
        try {
            cloned.get("inner").put(6, "six");
            fail("Expected UnsupportedOperationException for inner map modification");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* --------------------------------------------------------------------- */
    /*  mapToString(..)                                                      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testMapToStringEmpty() {
        assertEquals("{}", MapUtilities.mapToString(Collections.emptyMap()));
    }

    @Test
    public void testMapToStringSimple() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("{a=1, b=2}", MapUtilities.mapToString(map));
    }

    @Test
    public void testMapToStringSelfReference() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("self", map);
        assertEquals("{self=(this Map)}", MapUtilities.mapToString(map));
    }

    @Test
    public void testMapToStringKeyEqualsMapItself() {
        Map<Object, String> map = new LinkedHashMap<>();
        map.put(map, "value");
        assertEquals("{(this Map)=value}", MapUtilities.mapToString(map));
    }

    /* --------------------------------------------------------------------- */
    /*  mapOf(..)                                                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testMapOfEmptyReturnsImmutableEmptyMap() {
        Map<?, ?> result = MapUtilities.mapOf();
        assertTrue(result.isEmpty());
        try {
            result.put("x", "y");
            fail("Expected UnsupportedOperationException for empty immutable map");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testMapOfValidPairs() {
        Map<String, Integer> map = MapUtilities.mapOf("one", 1, "two", 2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("two"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapOfOddNumberOfArgumentsThrows() {
        MapUtilities.mapOf("onlyKey");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapOfTooManyEntriesThrows() {
        // 11 entries => 22 arguments, exceeds MAX_ENTRIES (10)
        Object[] args = new Object[22];
        for (int i = 0; i < 22; i++) {
            args[i] = i;
        }
        MapUtilities.mapOf(args);
    }

    /* --------------------------------------------------------------------- */
    /*  mapOfEntries(..)                                                      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testMapOfEntriesEmpty() {
        Map<String, Integer> map = MapUtilities.mapOfEntries();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testMapOfEntriesValid() {
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("k1", 10);
        Map.Entry<String, Integer> e2 = new AbstractMap.SimpleEntry<>("k2", 20);
        Map<String, Integer> map = MapUtilities.mapOfEntries(e1, e2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(10), map.get("k1"));
        assertEquals(Integer.valueOf(20), map.get("k2"));
    }

    @Test(expected = NullPointerException.class)
    public void testMapOfEntriesNullEntryThrows() {
        MapUtilities.mapOfEntries((Map.Entry<String, Integer>) null);
    }

    /* --------------------------------------------------------------------- */
    /*  getMapStructureString(..) (package‑private)                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetMapStructureStringWithNull() {
        assertEquals("null", invokeGetMapStructureString(null));
    }

    @Test
    public void testGetMapStructureStringSimpleMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "b");
        String structure = invokeGetMapStructureString(map);
        // For a plain LinkedHashMap we expect the class name (LinkedHashMap)
        assertTrue(structure.contains("LinkedHashMap"));
    }

    /* --------------------------------------------------------------------- */
    /*  detectMapOrdering(..) (package‑private)                              */
    /* --------------------------------------------------------------------- */
    @Test
    public void testDetectMapOrderingNullReturnsUnordered() {
        assertEquals(CompactMap.UNORDERED, invokeDetectMapOrdering(null));
    }

    @Test
    public void testDetectMapOrderingSortedMap() {
        Map<String, Integer> tree = new TreeMap<>();
        assertEquals(CompactMap.SORTED, invokeDetectMapOrdering(tree));
    }

    @Test
    public void testDetectMapOrderingLinkedHashMap() {
        Map<String, Integer> linked = new LinkedHashMap<>();
        assertEquals(CompactMap.INSERTION, invokeDetectMapOrdering(linked));
    }

    @Test
    public void testDetectMapOrderingEnumMap() {
        Map<Thread.State, String> enumMap = new EnumMap<>(Thread.State.class);
        assertEquals(CompactMap.INSERTION, invokeDetectMapOrdering(enumMap));
    }

    /* --------------------------------------------------------------------- */
    /*  Helper methods to access package‑private static methods               */
    /* --------------------------------------------------------------------- */
    private static String invokeGetMapStructureString(Map<?, ?> map) {
        try {
            java.lang.reflect.Method m = MapUtilities.class.getDeclaredMethod("getMapStructureString", Map.class);
            m.setAccessible(true);
            return (String) m.invoke(null, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String invokeDetectMapOrdering(Map<?, ?> map) {
        try {
            java.lang.reflect.Method m = MapUtilities.class.getDeclaredMethod("detectMapOrdering", Map.class);
            m.setAccessible(true);
            return (String) m.invoke(null, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
