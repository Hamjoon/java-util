package com.cedarsoftware.util;

import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class MapUtilitiesTest {

    /* --------------------------  get(...)  -------------------------- */

    @Test
    public void testGetReturnsExistingValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Integer result = MapUtilities.get(map, "one", 99);
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testGetReturnsDefaultWhenValueIsNull() {
        Map<String, String> map = new HashMap<>();
        map.put("key", null);
        String def = "default";
        String result = MapUtilities.get(map, "key", def);
        assertEquals(def, result);
    }

    @Test
    public void testGetReturnsDefaultWhenKeyMissing() {
        Map<String, String> map = new HashMap<>();
        String def = "fallback";
        String result = MapUtilities.get(map, "missing", def);
        assertEquals(def, result);
    }

    /* --------------------------  getOrThrow(...)  -------------------------- */

    @Test
    public void testGetOrThrowReturnsValueWhenKeyPresent() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("a", "alpha");
        Object value = MapUtilities.getOrThrow(map, "a", new IllegalArgumentException("missing"));
        assertEquals("alpha", value);
    }

    @Test(expected = IllegalStateException.class)
    public void testGetOrThrowThrowsProvidedExceptionWhenKeyAbsent() throws Throwable {
        Map<String, String> map = new HashMap<>();
        map.put("a", "alpha");
        // key "b" is not present, should throw the supplied throwable
        MapUtilities.getOrThrow(map, "b", new IllegalStateException("key not found"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullMapThrowsNPE() throws Throwable {
        MapUtilities.getOrThrow(null, "any", new RuntimeException("oops"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullThrowableThrowsNPE() throws Throwable {
        Map<String, String> map = Collections.singletonMap("k", "v");
        MapUtilities.getOrThrow(map, "k", null);
    }

    /* --------------------------  isEmpty(...)  -------------------------- */

    @Test
    public void testIsEmptyHandlesNullAndEmpty() {
        assertTrue(MapUtilities.isEmpty(null));
        assertTrue(MapUtilities.isEmpty(new HashMap<>()));
    }

    @Test
    public void testIsEmptyDetectsNonEmptyMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("x", 1);
        assertFalse(MapUtilities.isEmpty(map));
    }

    /* --------------------------  dupe(...)  -------------------------- */

    @Test
    public void testDupeMutableCreatesDeepCopy() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        Set<String> set = new LinkedHashSet<>();
        set.add("a");
        set.add("b");
        original.put(String.class, set);

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, false);

        // modify original set – copy must stay unchanged
        set.add("c");
        assertFalse(copy.get(String.class).contains("c"));

        // copy should be mutable
        copy.put(Integer.class, new LinkedHashSet<>(Collections.singleton("num")));
        assertTrue(copy.containsKey(Integer.class));
    }

    @Test
    public void testDupeUnmodifiableReturnsImmutableStructures() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        Set<String> set = new LinkedHashSet<>();
        set.add("x");
        original.put(String.class, set);

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, true);

        // Attempt to modify outer map – should throw
        try {
            copy.put(Integer.class, new LinkedHashSet<>());
            fail("Expected UnsupportedOperationException on map modification");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        // Attempt to modify inner set – should throw
        try {
            copy.get(String.class).add("y");
            fail("Expected UnsupportedOperationException on set modification");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    /* --------------------------  cloneMapOfSets(...)  -------------------------- */

    @Test
    public void testCloneMapOfSetsMutableCopiesStructure() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("one", new HashSet<>(Collections.singleton(1)));

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);

        // Mutate original – clone should not reflect change
        original.get("one").add(2);
        assertFalse(clone.get("one").contains(2));

        // Mutate clone – original should stay unchanged
        clone.get("one").add(3);
        assertFalse(original.get("one").contains(3));

        // Verify both map and inner set are mutable
        clone.put("two", new HashSet<>(Collections.singleton(4)));
        assertTrue(clone.containsKey("two"));
    }

    @Test
    public void testCloneMapOfSetsImmutableReturnsUnmodifiableViews() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("k", new HashSet<>(Collections.singleton(10)));

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, true);

        // outer map unmodifiable
        try {
            clone.put("new", new HashSet<>());
            fail("Expected UnsupportedOperationException on map put");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        // inner set unmodifiable
        try {
            clone.get("k").add(20);
            fail("Expected UnsupportedOperationException on set add");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    /* --------------------------  cloneMapOfMaps(...)  -------------------------- */

    @Test
    public void testCloneMapOfMapsMutableCopiesStructure() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        original.put("map", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);

        // Change original inner map – clone must not see the change
        inner.put(2, "two");
        assertNull(clone.get("map").get(2));

        // Change clone inner map – original must not see the change
        clone.get("map").put(3, "three");
        assertNull(original.get("map").get(3));

        // Outer map mutable
        clone.put("newMap", new HashMap<>());
        assertTrue(clone.containsKey("newMap"));
    }

    @Test
    public void testCloneMapOfMapsImmutableReturnsUnmodifiableViews() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(5, "five");
        original.put("inner", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, true);

        // outer map unmodifiable
        try {
            clone.put("another", new HashMap<>());
            fail("Expected UnsupportedOperationException on outer map");
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        // inner map unmodifiable
        try {
            clone.get("inner").put(6, "six");
            fail("Expected UnsupportedOperationException on inner map");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    /* --------------------------  mapOf(...) overloads  -------------------------- */

    @Test
    public void testMapOfZeroEntriesReturnsEmptyUnmodifiableMap() {
        Map<Object, Object> empty = MapUtilities.mapOf();
        assertTrue(empty.isEmpty());

        try {
            empty.put("x", "y");
            fail("Expected UnsupportedOperationException on empty map");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testMapOfOneEntry() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));

        // verify unmodifiable
        try {
            map.put("b", 2);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void testMapOfTwoEntriesPreservesInsertionOrder() {
        Map<String, String> map = MapUtilities.mapOf("first", "1st", "second", "2nd");
        assertEquals(2, map.size());

        // LinkedHashMap preserves order; iterate and assert order
        String[] expectedKeys = {"first", "second"};
        int idx = 0;
        for (String key : map.keySet()) {
            assertEquals(expectedKeys[idx++], key);
        }
    }

    @Test
    public void testMapOfThreeEntries() {
        Map<Integer, String> map = MapUtilities.mapOf(1, "one", 2, "two", 3, "three");
        assertEquals(3, map.size());
        assertEquals("two", map.get(2));
    }

    @Test
    public void testMapOfFourEntries() {
        Map<Character, Integer> map = MapUtilities.mapOf('a', 1, 'b', 2, 'c', 3, 'd', 4);
        assertEquals(Integer.valueOf(3), map.get('c'));
    }

    @Test
    public void testMapOfFiveEntries() {
        Map<String, String> map = MapUtilities.mapOf("k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5");
        assertEquals("v5", map.get("k5"));
        assertEquals(5, map.size());
    }

    @Test
    public void testMapOfSixEntries() {
        Map<Integer, Integer> map = MapUtilities.mapOf(
                1, 10,
                2, 20,
                3, 30,
                4, 40,
                5, 50,
                6, 60);
        assertEquals(Integer.valueOf(40), map.get(4));
        assertEquals(6, map.size());
    }
}
