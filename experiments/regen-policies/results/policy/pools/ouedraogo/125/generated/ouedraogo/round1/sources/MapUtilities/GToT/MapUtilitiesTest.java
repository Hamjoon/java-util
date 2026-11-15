package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Comprehensive JUnit‑4 test suite for {@link MapUtilities}.
 *
 * The tests cover:
 *   • Normal behaviour of each public method.
 *   • Edge‑cases, null handling and illegal arguments.
 *   • Immutability contracts where applicable.
 *   • Special handling in {@code mapToString}, {@code mapOf},
 *     {@code mapOfEntries}, {@code getMapStructureString} and
 *     {@code detectMapOrdering}.
 *
 * All tests use only the classes, constructors and methods that are
 * visible in the supplied source code (or standard JDK classes).
 */
public class MapUtilitiesTest {

    /* -------------------------------------------------------------
     *  Tests for get(Map, Object, T)
     * ------------------------------------------------------------- */
    @Test
    public void testGetReturnsValueWhenPresent() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Integer result = MapUtilities.get(map, "one", -1);
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testGetReturnsDefaultWhenMissing() {
        Map<String, Integer> map = new HashMap<>();
        Integer result = MapUtilities.get(map, "missing", -1);
        assertEquals(Integer.valueOf(-1), result);
    }

    @Test
    public void testGetReturnsDefaultWhenValueIsNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put("nullKey", null);
        Integer result = MapUtilities.get(map, "nullKey", 42);
        assertEquals(Integer.valueOf(42), result);
    }

    /* -------------------------------------------------------------
     *  Tests for getOrThrow(Map, Object, T)
     * ------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testGetOrThrowMapIsNull() throws Throwable {
        MapUtilities.getOrThrow(null, "key", new IllegalArgumentException());
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowThrowableIsNull() throws Throwable {
        Map<String, String> map = Collections.singletonMap("a", "b");
        MapUtilities.getOrThrow(map, "a", null);
    }

    @Test
    public void testGetOrThrowReturnsValueWhenKeyPresent() throws Throwable {
        Map<String, String> map = Collections.singletonMap("k", "v");
        Object result = MapUtilities.getOrThrow(map, "k", new IllegalStateException());
        assertEquals("v", result);
    }

    @Test
    public void testGetOrThrowReturnsNullWhenKeyPresentAndValueIsNull() throws Throwable {
        Map<String, String> map = new HashMap<>();
        map.put("k", null);
        Object result = MapUtilities.getOrThrow(map, "k", new IllegalStateException());
        assertNull(result);
    }

    @Test
    public void testGetOrThrowThrowsProvidedExceptionWhenKeyMissing() {
        Map<String, String> map = Collections.singletonMap("a", "b");
        IllegalArgumentException ex = new IllegalArgumentException("missing");
        try {
            MapUtilities.getOrThrow(map, "missingKey", ex);
            fail("Expected exception to be thrown");
        } catch (IllegalArgumentException thrown) {
            assertSame(ex, thrown);
        }
    }

    /* -------------------------------------------------------------
     *  Tests for isEmpty(Map)
     * ------------------------------------------------------------- */
    @Test
    public void testIsEmptyWithNull() {
        assertTrue(MapUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmptyWithEmptyMap() {
        assertTrue(MapUtilities.isEmpty(new HashMap<>()));
    }

    @Test
    public void testIsEmptyWithNonEmptyMap() {
        Map<String, Integer> map = Collections.singletonMap("x", 1);
        assertFalse(MapUtilities.isEmpty(map));
    }

    /* -------------------------------------------------------------
     *  Tests for dupe(Map<Class<?>, Set<T>>, boolean)
     * ------------------------------------------------------------- */
    @Test
    public void testDupeModifiable() {
        Map<Class<?>, Set<String>> source = new LinkedHashMap<>();
        source.put(String.class, new HashSet<>(Collections.singleton("a")));

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, false);
        assertNotSame(source, copy);
        assertEquals(source, copy);

        // modify copy – should not affect source
        copy.get(String.class).add("b");
        assertFalse(source.get(String.class).contains("b"));
    }

    @Test
    public void testDupeUnmodifiable() {
        Map<Class<?>, Set<String>> source = new LinkedHashMap<>();
        source.put(Integer.class, new HashSet<>(Collections.singleton("x")));

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(source, true);
        assertNotSame(source, copy);
        assertEquals(source, copy);

        // both map and inner set must be unmodifiable
        try {
            copy.put(Double.class, Collections.emptySet());
            fail("Expected UnsupportedOperationException for map modification");
        } catch (UnsupportedOperationException ignored) {}

        try {
            copy.get(Integer.class).add("y");
            fail("Expected UnsupportedOperationException for set modification");
        } catch (UnsupportedOperationException ignored) {}
    }

    /* -------------------------------------------------------------
     *  Tests for cloneMapOfSets(Map<T, Set<V>>, boolean)
     * ------------------------------------------------------------- */
    @Test
    public void testCloneMapOfSetsMutable() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("a", new HashSet<>(Collections.singleton(1)));

        Map<String, Set<Integer>> cloned = MapUtilities.cloneMapOfSets(original, false);
        assertEquals(original, cloned);
        assertNotSame(original, cloned);
        assertNotSame(original.get("a"), cloned.get("a"));

        // verify mutability
        cloned.get("a").add(2);
        assertTrue(cloned.get("a").contains(2));
    }

    @Test
    public void testCloneMapOfSetsImmutable() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("b", new HashSet<>(Collections.singleton(3)));

        Map<String, Set<Integer>> cloned = MapUtilities.cloneMapOfSets(original, true);
        assertEquals(original, cloned);

        // map must be unmodifiable
        try {
            cloned.put("c", Collections.emptySet());
            fail("Expected UnsupportedOperationException for map modification");
        } catch (UnsupportedOperationException ignored) {}

        // inner set must be unmodifiable
        try {
            cloned.get("b").add(4);
            fail("Expected UnsupportedOperationException for set modification");
        } catch (UnsupportedOperationException ignored) {}
    }

    /* -------------------------------------------------------------
     *  Tests for cloneMapOfMaps(Map<T, Map<U,V>>, boolean)
     * ------------------------------------------------------------- */
    @Test
    public void testCloneMapOfMapsMutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        original.put("x", Collections.singletonMap(1, "one"));

        Map<String, Map<Integer, String>> cloned = MapUtilities.cloneMapOfMaps(original, false);
        assertEquals(original, cloned);
        assertNotSame(original, cloned);
        assertNotSame(original.get("x"), cloned.get("x"));

        // mutability check
        cloned.get("x").put(2, "two");
        assertTrue(cloned.get("x").containsKey(2));
    }

    @Test
    public void testCloneMapOfMapsImmutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        original.put("y", Collections.singletonMap(5, "five"));

        Map<String, Map<Integer, String>> cloned = MapUtilities.cloneMapOfMaps(original, true);
        assertEquals(original, cloned);

        // map unmodifiable
        try {
            cloned.put("z", Collections.emptyMap());
            fail("Expected UnsupportedOperationException for map modification");
        } catch (UnsupportedOperationException ignored) {}

        // inner map unmodifiable
        try {
            cloned.get("y").put(6, "six");
            fail("Expected UnsupportedOperationException for inner map modification");
        } catch (UnsupportedOperationException ignored) {}
    }

    /* -------------------------------------------------------------
     *  Tests for mapToString(Map)
     * ------------------------------------------------------------- */
    @Test
    public void testMapToStringEmpty() {
        assertEquals("{}", MapUtilities.mapToString(Collections.emptyMap()));
    }

    @Test
    public void testMapToStringSingleEntry() {
        Map<String, Integer> map = Collections.singletonMap("a", 1);
        assertEquals("{a=1}", MapUtilities.mapToString(map));
    }

    @Test
    public void testMapToStringMultipleEntriesPreservesOrder() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);
        assertEquals("{first=1, second=2, third=3}", MapUtilities.mapToString(map));
    }

    @Test
    public void testMapToStringSelfReferenceKeyAndValue() {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put(map, map); // key and value are the map itself
        assertEquals("{(this Map)=(this Map)}", MapUtilities.mapToString(map));
    }

    /* -------------------------------------------------------------
     *  Tests for mapOf(Object...)
     * ------------------------------------------------------------- */
    @Test
    public void testMapOfEmpty() {
        Map<?, ?> result = MapUtilities.mapOf();
        assertTrue(result.isEmpty());
        assertTrue(result instanceof java.util.Collections.UnmodifiableMap);
    }

    @Test
    public void testMapOfOddNumberOfArguments() {
        try {
            MapUtilities.mapOf("k1", "v1", "k2"); // 3 arguments – odd
            fail("Expected IllegalArgumentException for odd number of arguments");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("odd"));
        }
    }

    @Test
    public void testMapOfTooManyEntries() {
        Object[] many = new Object[22]; // 11 key/value pairs > MAX_ENTRIES (10)
        for (int i = 0; i < many.length; i++) many[i] = i;
        try {
            MapUtilities.mapOf(many);
            fail("Expected IllegalArgumentException for exceeding max entries");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Too many entries"));
        }
    }

    @Test
    public void testMapOfNormalUsage() {
        Map<String, Integer> map = MapUtilities.mapOf("one", 1, "two", 2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("two"));
        assertTrue(map instanceof java.util.Collections.UnmodifiableMap);
    }

    @Test
    public void testMapOfNullKeyAndValue() {
        Map<String, String> map = MapUtilities.mapOf(null, null);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
    }

    /* -------------------------------------------------------------
     *  Tests for mapOfEntries(Map.Entry...)
     * ------------------------------------------------------------- */
    @Test
    public void testMapOfEntriesEmpty() {
        Map<?, ?> result = MapUtilities.mapOfEntries();
        assertTrue(result.isEmpty());
        assertTrue(result instanceof java.util.Collections.UnmodifiableMap);
    }

    @Test
    public void testMapOfEntriesNullArray() {
        Map<?, ?> result = MapUtilities.mapOfEntries((Map.Entry<?, ?>[]) null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapOfEntriesNullEntryInArray() {
        try {
            MapUtilities.mapOfEntries(new AbstractMap.SimpleEntry<>("a", "b"), null);
            fail("Expected NullPointerException for null entry");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("Entries must not be null"));
        }
    }

    @Test
    public void testMapOfEntriesNormal() {
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("x", 10);
        Map.Entry<String, Integer> e2 = new AbstractMap.SimpleEntry<>("y", 20);
        Map<String, Integer> map = MapUtilities.mapOfEntries(e1, e2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(10), map.get("x"));
        assertEquals(Integer.valueOf(20), map.get("y"));
        assertTrue(map instanceof java.util.Collections.UnmodifiableMap);
    }

    @Test
    public void testMapOfEntriesNullKeyOrValue() {
        Map.Entry<String, String> e = new AbstractMap.SimpleEntry<>(null, null);
        Map<String, String> map = MapUtilities.mapOfEntries(e);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
    }

    /* -------------------------------------------------------------
     *  Tests for getMapStructureString(Map)
     * ------------------------------------------------------------- */
    @Test
    public void testGetMapStructureStringNull() {
        assertEquals("null", MapUtilities.getMapStructureString(null));
    }

    @Test
    public void testGetMapStructureStringPlainMap() {
        Map<String, Integer> map = new HashMap<>();
        String result = MapUtilities.getMapStructureString(map);
        // Should contain the simple class name of the map
        assertTrue(result.contains("HashMap"));
    }

    /* -------------------------------------------------------------
     *  Tests for detectMapOrdering(Map)
     * ------------------------------------------------------------- */
    @Test
    public void testDetectMapOrderingNull() {
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(null));
    }

    @Test
    public void testDetectMapOrderingPlainHashMap() {
        Map<String, String> map = new HashMap<>();
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(map));
    }

    @Test
    public void testDetectMapOrderingLinkedHashMap() {
        Map<String, String> map = new LinkedHashMap<>();
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(map));
    }

    @Test
    public void testDetectMapOrderingEnumMap() {
        EnumMap<TestEnum, String> map = new EnumMap<>(TestEnum.class);
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(map));
    }

    @Test
    public void testDetectMapOrderingTreeMap() {
        Map<String, String> map = new TreeMap<>();
        assertEquals(CompactMap.SORTED, MapUtilities.detectMapOrdering(map));
    }

    // Helper enum for EnumMap test
    private enum TestEnum { ONE, TWO }
}
