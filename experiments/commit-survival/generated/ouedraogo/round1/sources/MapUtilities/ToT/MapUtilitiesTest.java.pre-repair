package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class MapUtilitiesTest {

    /* ----------  get(...) tests ---------- */

    @Test
    public void testGetReturnsValueWhenPresent() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Integer result = MapUtilities.get(map, "one", 99);
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testGetReturnsDefaultWhenKeyMissing() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Integer result = MapUtilities.get(map, "two", 99);
        assertEquals(Integer.valueOf(99), result);
    }

    @Test
    public void testGetReturnsDefaultWhenValueIsNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put("nullKey", null);
        Integer result = MapUtilities.get(map, "nullKey", 55);
        // map.get returns null → default is returned
        assertEquals(Integer.valueOf(55), result);
    }

    /* ----------  getOrThrow(...) tests ---------- */

    @Test
    public void testGetOrThrowReturnsValueWhenKeyPresent() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("k", "v");
        Object result = MapUtilities.getOrThrow(map, "k", new IllegalStateException());
        assertEquals("v", result);
    }

    @Test
    public void testGetOrThrowReturnsNullWhenKeyPresentAndValueIsNull() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("k", null);
        Object result = MapUtilities.getOrThrow(map, "k", new IllegalArgumentException());
        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetOrThrowThrowsProvidedExceptionWhenKeyMissing() throws Exception {
        Map<String, String> map = new HashMap<>();
        MapUtilities.getOrThrow(map, "missing", new IllegalArgumentException("missing key"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullMapThrowsNPE() throws Exception {
        MapUtilities.getOrThrow(null, "k", new RuntimeException());
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullThrowableThrowsNPE() throws Exception {
        Map<String, String> map = new HashMap<>();
        MapUtilities.getOrThrow(map, "k", null);
    }

    /* ----------  isEmpty(...) tests ---------- */

    @Test
    public void testIsEmptyWithNullMap() {
        assertTrue(MapUtilities.isEmpty(null));
    }

    @Test
    public void testIsEmptyWithEmptyMap() {
        assertTrue(MapUtilities.isEmpty(new HashMap<>()));
    }

    @Test
    public void testIsEmptyWithNonEmptyMap() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "b");
        assertFalse(MapUtilities.isEmpty(map));
    }

    /* ----------  dupe(...) tests ---------- */

    @Test
    public void testDupeMutableCreatesDeepCopy() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        original.put(String.class, new LinkedHashSet<>(Arrays.asList("a", "b")));
        original.put(Integer.class, new LinkedHashSet<>(Collections.singletonList("c")));

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, false);

        // Content equality
        assertEquals(original, copy);

        // Ensure new Set instances (deep copy)
        for (Class<?> key : original.keySet()) {
            assertNotSame(original.get(key), copy.get(key));
        }

        // Modifying original should not affect copy
        original.get(String.class).add("new");
        assertFalse(copy.get(String.class).contains("new"));
    }

    @Test
    public void testDupeUnmodifiableReturnsUnmodifiableMapAndSets() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        original.put(String.class, new LinkedHashSet<>(Arrays.asList("x", "y")));

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, true);

        // Map should be unmodifiable
        try {
            copy.put(Integer.class, Collections.emptySet());
            fail("Expected UnsupportedOperationException when modifying map");
        } catch (UnsupportedOperationException ignored) {}

        // Sets inside map should be unmodifiable
        try {
            copy.get(String.class).add("z");
            fail("Expected UnsupportedOperationException when modifying inner set");
        } catch (UnsupportedOperationException ignored) {}

        // Content should match original
        assertEquals(original, copy);
    }

    /* ----------  cloneMapOfSets(...) tests ---------- */

    @Test
    public void testCloneMapOfSetsMutableDeepCopy() {
        Map<Integer, Set<String>> original = new HashMap<>();
        original.put(1, new HashSet<>(Arrays.asList("a", "b")));

        Map<Integer, Set<String>> clone = MapUtilities.cloneMapOfSets(original, false);

        // Verify equality but not same instances
        assertEquals(original, clone);
        assertNotSame(original.get(1), clone.get(1));

        // Changing original does not affect clone
        original.get(1).add("c");
        assertFalse(clone.get(1).contains("c"));
    }

    @Test
    public void testCloneMapOfSetsImmutableUnmodifiable() {
        Map<Integer, Set<String>> original = new HashMap<>();
        original.put(2, new HashSet<>(Collections.singleton("x")));

        Map<Integer, Set<String>> clone = MapUtilities.cloneMapOfSets(original, true);

        // Map unmodifiable
        try {
            clone.put(3, Collections.emptySet());
            fail("Map should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}

        // Inner set unmodifiable
        try {
            clone.get(2).add("y");
            fail("Inner set should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}
    }

    /* ----------  cloneMapOfMaps(...) tests ---------- */

    @Test
    public void testCloneMapOfMapsMutableDeepCopy() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        original.put("first", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);

        assertEquals(original, clone);
        assertNotSame(original.get("first"), clone.get("first"));

        // Mutating original inner map does not affect clone
        original.get("first").put(2, "two");
        assertFalse(clone.get("first").containsKey(2));
    }

    @Test
    public void testCloneMapOfMapsImmutableUnmodifiable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(10, "ten");
        original.put("key", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, true);

        // Outer map unmodifiable
        try {
            clone.put("newKey", new HashMap<>());
            fail("Outer map should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}

        // Inner map unmodifiable
        try {
            clone.get("key").put(20, "twenty");
            fail("Inner map should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}
    }

    /* ----------  mapOf(...) overloads tests ---------- */

    @Test
    public void testMapOfZeroReturnsEmptyUnmodifiableMap() {
        Map<Object, Object> map = MapUtilities.mapOf();
        assertTrue(map.isEmpty());

        try {
            map.put("a", "b");
            fail("Map should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testMapOfOneEntry() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));

        try {
            map.remove("a");
            fail("Map should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}
    }

    @Test
    public void testMapOfTwoEntries() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1, "b", 2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testMapOfSixEntries() {
        Map<String, Integer> map = MapUtilities.mapOf(
                "k1", 1,
                "k2", 2,
                "k3", 3,
                "k4", 4,
                "k5", 5,
                "k6", 6);

        assertEquals(6, map.size());
        for (int i = 1; i <= 6; i++) {
            assertEquals(Integer.valueOf(i), map.get("k" + i));
        }

        // Verify unmodifiable
        try {
            map.clear();
            fail("Map should be unmodifiable");
        } catch (UnsupportedOperationException ignored) {}
    }

    /* ----------  Additional edge case: duplicate keys in mapOf ---------- */
    @Test
    public void testMapOfDuplicateKeyKeepsLastValue() {
        // The LinkedHashMap.put will overwrite previous value for same key
        Map<String, Integer> map = MapUtilities.mapOf("dup", 1, "dup", 2);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(2), map.get("dup"));
    }
}
