package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link MapUtilities}.
 */
public class MapUtilitiesTest {

    /* ----------------------------------------------------------------------
     *  1.  Public method signatures extracted from MapUtilities
     * ---------------------------------------------------------------------- */
    // public static <T> T get(Map<?, T> map, Object key, T def)
    // public static <T extends Throwable> Object getOrThrow(Map<?, ?> map, Object key, T throwable) throws T
    // public static boolean isEmpty(Map map)
    // public static <T> Map<Class<?>, Set<T>> dupe(Map<Class<?>, Set<T>> other, boolean unmodifiable)
    // public static <T, V> Map<T, Set<V>> cloneMapOfSets(Map<T, Set<V>> original, boolean immutable)
    // public static <T, U, V> Map<T, Map<U, V>> cloneMapOfMaps(Map<T, Map<U, V>> original, boolean immutable)
    // public static <K, V> Map<K, V> mapOf()
    // public static <K, V> Map<K, V> mapOf(K k, V v)
    // public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2)
    // public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3)
    // public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4)
    // public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5)
    // public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6)

    /* ----------------------------------------------------------------------
     *  2.  Basic functional tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetFound() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Integer result = MapUtilities.get(map, "one", 99);
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testGetNotFoundReturnsDefault() {
        Map<String, Integer> map = new HashMap<>();
        Integer result = MapUtilities.get(map, "missing", 42);
        assertEquals(Integer.valueOf(42), result);
    }

    @Test(expected = NullPointerException.class)
    public void testGetWithNullMapThrowsNPE() {
        MapUtilities.get(null, "any", "def");
    }

    @Test
    public void testGetOrThrowKeyPresentReturnsValueIncludingNull() {
        Map<String, String> map = new HashMap<>();
        map.put("a", null);
        Object value = MapUtilities.getOrThrow(map, "a", new IllegalArgumentException("missing"));
        assertNull(value);
    }

    @Test
    public void testGetOrThrowKeyPresentReturnsNonNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put("x", 10);
        Object value = MapUtilities.getOrThrow(map, "x", new IllegalStateException());
        assertEquals(10, value);
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullMap() throws Throwable {
        MapUtilities.getOrThrow(null, "k", new Exception("boom"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullThrowable() throws Throwable {
        MapUtilities.getOrThrow(new HashMap<>(), "k", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOrThrowKeyAbsentThrowsProvidedThrowable() throws Throwable {
        Map<String, Integer> map = new HashMap<>();
        MapUtilities.getOrThrow(map, "missing", new IllegalArgumentException("not found"));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(MapUtilities.isEmpty(null));
        assertTrue(MapUtilities.isEmpty(new HashMap<>()));
        Map<String, String> map = new HashMap<>();
        map.put("k", "v");
        assertFalse(MapUtilities.isEmpty(map));
    }

    @Test
    public void testDupeEmptyMap() {
        Map<Class<?>, Set<String>> src = new HashMap<>();
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(src, false);
        assertTrue(copy.isEmpty());
    }

    @Test
    public void testDupeCreatesDeepCopyAndHandlesUnmodifiableFlag() {
        Map<Class<?>, Set<String>> src = new HashMap<>();
        src.put(String.class, new HashSet<>(Arrays.asList("a", "b")));

        // mutable copy
        Map<Class<?>, Set<String>> mutableCopy = MapUtilities.dupe(src, false);
        assertEquals(1, mutableCopy.size());
        assertTrue(mutableCopy.get(String.class).containsAll(Arrays.asList("a", "b")));

        // modify original – should NOT affect copy
        src.get(String.class).add("c");
        assertFalse(mutableCopy.get(String.class).contains("c"));

        // unmodifiable copy
        Map<Class<?>, Set<String>> unmodCopy = MapUtilities.dupe(src, true);
        assertEquals(1, unmodCopy.size());
        assertTrue(unmodCopy.get(String.class).containsAll(Arrays.asList("a", "b", "c")));

        // verify unmodifiable behavior
        try {
            unmodCopy.put(Integer.class, new HashSet<>());
            fail("Expected UnsupportedOperationException for map modification");
        } catch (UnsupportedOperationException ignored) { }

        try {
            unmodCopy.get(String.class).add("d");
            fail("Expected UnsupportedOperationException for set modification");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testCloneMapOfSetsMutable() {
        Map<Integer, Set<String>> original = new HashMap<>();
        original.put(1, new HashSet<>(Arrays.asList("x")));
        Map<Integer, Set<String>> clone = MapUtilities.cloneMapOfSets(original, false);

        // content equality
        assertEquals(original, clone);
        // mutability of inner set
        clone.get(1).add("y");
        assertTrue(clone.get(1).contains("y"));
    }

    @Test
    public void testCloneMapOfSetsImmutable() {
        Map<Integer, Set<String>> original = new HashMap<>();
        original.put(2, new HashSet<>(Arrays.asList("z")));

        Map<Integer, Set<String>> clone = MapUtilities.cloneMapOfSets(original, true);

        // map itself should be unmodifiable
        try {
            clone.put(3, new HashSet<>());
            fail("Expected UnsupportedOperationException for map modification");
        } catch (UnsupportedOperationException ignored) { }

        // inner set should be unmodifiable
        try {
            clone.get(2).add("new");
            fail("Expected UnsupportedOperationException for set modification");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testCloneMapOfMapsMutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(10, "ten");
        original.put("first", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);

        assertEquals(original, clone);
        // modify clone's inner map – should not affect original
        clone.get("first").put(20, "twenty");
        assertFalse(original.get("first").containsKey(20));
    }

    @Test
    public void testCloneMapOfMapsImmutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        original.put("key", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, true);

        // map unmodifiable
        try {
            clone.put("newKey", new HashMap<>());
            fail("Expected UnsupportedOperationException for outer map");
        } catch (UnsupportedOperationException ignored) { }

        // inner map unmodifiable
        try {
            clone.get("key").put(2, "two");
            fail("Expected UnsupportedOperationException for inner map");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testMapOfZeroArgs() {
        Map<String, Integer> map = MapUtilities.mapOf();
        assertTrue(map.isEmpty());
        try {
            map.put("a", 1);
            fail("Expected map to be unmodifiable");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testMapOfOnePair() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        try {
            map.put("b", 2);
            fail("Expected unmodifiable map");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testMapOfTwoPairs() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1, "b", 2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testMapOfThreePairs() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1, "b", 2, "c", 3);
        List<String> expectedOrder = Arrays.asList("a", "b", "c");
        assertEquals(expectedOrder, new ArrayList<>(map.keySet()));
    }

    @Test
    public void testMapOfFourPairs() {
        Map<Integer, String> map = MapUtilities.mapOf(1, "one", 2, "two", 3, "three", 4, "four");
        assertEquals(4, map.size());
        assertEquals("three", map.get(3));
    }

    @Test
    public void testMapOfFivePairs() {
        Map<String, String> map = MapUtilities.mapOf("k1","v1","k2","v2","k3","v3","k4","v4","k5","v5");
        assertEquals(5, map.size());
        assertTrue(map.containsKey("k5"));
        assertEquals("v5", map.get("k5"));
    }

    @Test
    public void testMapOfSixPairs() {
        Map<Integer, Integer> map = MapUtilities.mapOf(1,10, 2,20, 3,30, 4,40, 5,50, 6,60);
        assertEquals(6, map.size());
        assertEquals(Integer.valueOf(60), map.get(6));
    }

    @Test
    public void testMapOfDuplicateKeyKeepsLastValue() {
        Map<String, Integer> map = MapUtilities.mapOf("dup", 1, "dup", 2);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(2), map.get("dup")); // last wins
    }

    /* ----------------------------------------------------------------------
     *  3.  Edge‑case & exception tests already integrated above
     * ---------------------------------------------------------------------- */
}
