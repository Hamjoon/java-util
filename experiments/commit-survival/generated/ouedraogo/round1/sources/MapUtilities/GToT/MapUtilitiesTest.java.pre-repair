package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class MapUtilitiesTest {

    /* ----------------------------------------------------------------------
     *  Public method signatures extracted from MapUtilities
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
     *  Tests for get(...)
     * ---------------------------------------------------------------------- */
    @Test
    public void testGet_FoundValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Integer result = MapUtilities.get(map, "one", 99);
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testGet_NotFoundReturnsDefault() {
        Map<String, Integer> map = new HashMap<>();
        Integer result = MapUtilities.get(map, "missing", 42);
        assertEquals(Integer.valueOf(42), result);
    }

    @Test
    public void testGet_NullValueReturnsDefault() {
        Map<String, String> map = new HashMap<>();
        map.put("key", null);
        String result = MapUtilities.get(map, "key", "default");
        assertEquals("default", result);
    }

    /* ----------------------------------------------------------------------
     *  Tests for getOrThrow(...)
     * ---------------------------------------------------------------------- */
    @Test
    public void testGetOrThrow_FoundValue() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("a", "value");
        Object result = MapUtilities.getOrThrow(map, "a", new IllegalStateException("missing"));
        assertEquals("value", result);
    }

    @Test
    public void testGetOrThrow_FoundNullValue() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("nullKey", null);
        Object result = MapUtilities.getOrThrow(map, "nullKey", new IllegalArgumentException("missing"));
        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOrThrow_KeyMissingThrowsProvidedException() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        MapUtilities.getOrThrow(map, "absent", new IllegalArgumentException("not present"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrow_NullMapThrowsNPE() throws Exception {
        MapUtilities.getOrThrow(null, "key", new RuntimeException());
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrow_NullThrowableThrowsNPE() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        MapUtilities.getOrThrow(map, "key", null);
    }

    /* ----------------------------------------------------------------------
     *  Tests for isEmpty(...)
     * ---------------------------------------------------------------------- */
    @Test
    public void testIsEmpty_NullMap() {
        assertTrue(MapUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmpty_EmptyMap() {
        assertTrue(MapUtilities.isEmpty(new HashMap<>()));
    }

    @Test
    public void testIsEmpty_NonEmptyMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("x", 1);
        assertFalse(MapUtilities.isEmpty(map));
    }

    /* ----------------------------------------------------------------------
     *  Tests for dupe(...)
     * ---------------------------------------------------------------------- */
    private Map<Class<?>, Set<String>> prepareSampleClassSetMap() {
        Map<Class<?>, Set<String>> source = new LinkedHashMap<>();
        source.put(String.class, new LinkedHashSet<>(Arrays.asList("a", "b")));
        source.put(Integer.class, new LinkedHashSet<>(Collections.singletonList("c")));
        return source;
    }

    @Test
    public void testDupe_MutableCopy() {
        Map<Class<?>, Set<String>> original = prepareSampleClassSetMap();
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, false);

        // Verify content equality
        assertEquals(original, copy);

        // Modify original – copy should stay unchanged
        original.get(String.class).add("new");
        assertFalse(copy.get(String.class).contains("new"));

        // Modify copy – original should stay unchanged
        copy.get(Integer.class).add("another");
        assertFalse(original.get(Integer.class).contains("another"));
    }

    @Test
    public void testDupe_UnmodifiableCopy() {
        Map<Class<?>, Set<String>> original = prepareSampleClassSetMap();
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, true);

        // Content must match
        assertEquals(original, copy);

        // Attempt to modify map – expect UnsupportedOperationException
        try {
            copy.put(Double.class, new HashSet<>());
            fail("Expected UnsupportedOperationException when modifying unmodifiable map");
        } catch (UnsupportedOperationException expected) {
            // ok
        }

        // Attempt to modify a set inside the map
        try {
            copy.get(String.class).add("new");
            fail("Expected UnsupportedOperationException when modifying unmodifiable set");
        } catch (UnsupportedOperationException expected) {
            // ok
        }

        // Ensure original is untouched after attempts
        assertFalse(original.get(String.class).contains("new"));
    }

    @Test
    public void testDupe_EmptyMap() {
        Map<Class<?>, Set<Integer>> empty = new LinkedHashMap<>();
        Map<Class<?>, Set<Integer>> copy = MapUtilities.dupe(empty, false);
        assertTrue(copy.isEmpty());
    }

    /* ----------------------------------------------------------------------
     *  Tests for cloneMapOfSets(...)
     * ---------------------------------------------------------------------- */
    private Map<String, Set<Integer>> sampleMapOfSets() {
        Map<String, Set<Integer>> map = new HashMap<>();
        map.put("evens", new HashSet<>(Arrays.asList(2, 4, 6)));
        map.put("odds", new HashSet<>(Arrays.asList(1, 3, 5)));
        return map;
    }

    @Test
    public void testCloneMapOfSets_Mutable() {
        Map<String, Set<Integer>> original = sampleMapOfSets();
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);

        assertEquals(original, clone);

        // Mutate original – clone must not change
        original.get("evens").add(8);
        assertFalse(clone.get("evens").contains(8));

        // Mutate clone – original must not change
        clone.get("odds").remove(1);
        assertTrue(original.get("odds").contains(1));
    }

    @Test
    public void testCloneMapOfSets_Immutable() {
        Map<String, Set<Integer>> original = sampleMapOfSets();
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, true);

        assertEquals(original, clone);

        // Map should be unmodifiable
        try {
            clone.put("primes", new HashSet<>(Arrays.asList(2, 3, 5)));
            fail("Expected UnsupportedOperationException on map modification");
        } catch (UnsupportedOperationException expected) {
            // ok
        }

        // Sets should be unmodifiable
        try {
            clone.get("evens").add(10);
            fail("Expected UnsupportedOperationException on set modification");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    /* ----------------------------------------------------------------------
     *  Tests for cloneMapOfMaps(...)
     * ---------------------------------------------------------------------- */
    private Map<String, Map<Integer, String>> sampleMapOfMaps() {
        Map<String, Map<Integer, String>> outer = new HashMap<>();

        Map<Integer, String> inner1 = new HashMap<>();
        inner1.put(1, "one");
        inner1.put(2, "two");

        Map<Integer, String> inner2 = new HashMap<>();
        inner2.put(3, "three");

        outer.put("first", inner1);
        outer.put("second", inner2);
        return outer;
    }

    @Test
    public void testCloneMapOfMaps_Mutable() {
        Map<String, Map<Integer, String>> original = sampleMapOfMaps();
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);

        assertEquals(original, clone);

        // Change original inner map – clone must stay unchanged
        original.get("first").put(99, "ninety‑nine");
        assertFalse(clone.get("first").containsKey(99));

        // Change clone inner map – original must stay unchanged
        clone.get("second").remove(3);
        assertTrue(original.get("second").containsKey(3));
    }

    @Test
    public void testCloneMapOfMaps_Immutable() {
        Map<String, Map<Integer, String>> original = sampleMapOfMaps();
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, true);

        assertEquals(original, clone);

        // Outer map immutable
        try {
            clone.put("third", new HashMap<>());
            fail("Expected UnsupportedOperationException on outer map modification");
        } catch (UnsupportedOperationException expected) {
            // ok
        }

        // Inner map immutable
        try {
            clone.get("first").put(5, "five");
            fail("Expected UnsupportedOperationException on inner map modification");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    /* ----------------------------------------------------------------------
     *  Tests for mapOf() overloads
     * ---------------------------------------------------------------------- */
    @Test
    public void testMapOf_ZeroArgs() {
        Map<String, Integer> map = MapUtilities.mapOf();
        assertTrue(map.isEmpty());

        // Verify unmodifiable
        try {
            map.put("x", 1);
            fail("Expected UnsupportedOperationException on mapOf() mutable attempt");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void testMapOf_OnePair() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 10);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(10), map.get("a"));

        try {
            map.remove("a");
            fail("Expected UnsupportedOperationException on mapOf(1) modification");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }

    @Test
    public void testMapOf_TwoPairs() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1, "b", 2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertUnmodifiable(map);
    }

    @Test
    public void testMapOf_ThreePairs() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1, "b", 2, "c", 3);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(3), map.get("c"));
        assertUnmodifiable(map);
    }

    @Test
    public void testMapOf_FourPairs() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1, "b", 2, "c", 3, "d", 4);
        assertEquals(4, map.size());
        assertEquals(Integer.valueOf(4), map.get("d"));
        assertUnmodifiable(map);
    }

    @Test
    public void testMapOf_FivePairs() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        assertEquals(5, map.size());
        assertEquals(Integer.valueOf(5), map.get("e"));
        assertUnmodifiable(map);
    }

    @Test
    public void testMapOf_SixPairs() {
        Map<String, Integer> map = MapUtilities.mapOf(
                "a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
        assertEquals(6, map.size());
        assertEquals(Integer.valueOf(6), map.get("f"));
        assertUnmodifiable(map);
    }

    /** Helper to assert a map is unmodifiable (throws UnsupportedOperationException on put). */
    private void assertUnmodifiable(Map<?, ?> map) {
        try {
            map.put("newKey", "newValue");
            fail("Expected UnsupportedOperationException on modifying unmodifiable map");
        } catch (UnsupportedOperationException expected) {
            // ok
        }
    }
}
