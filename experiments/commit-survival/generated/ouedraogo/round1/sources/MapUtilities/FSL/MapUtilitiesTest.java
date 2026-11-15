package com.cedarsoftware.util;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class MapUtilitiesTest {

    /* -------------------------------------------------------
     * Tests for get(...)
     * ------------------------------------------------------- */
    @Test
    public void testGetReturnsExistingValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Integer result = MapUtilities.get(map, "one", -1);
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testGetReturnsDefaultWhenKeyMissing() {
        Map<String, String> map = new HashMap<>();
        map.put("a", "alpha");
        String result = MapUtilities.get(map, "b", "default");
        assertEquals("default", result);
    }

    @Test
    public void testGetReturnsDefaultWhenValueIsNull() {
        Map<String, String> map = new HashMap<>();
        map.put("nullKey", null);
        String result = MapUtilities.get(map, "nullKey", "fallback");
        // The method treats a null *value* as “not found”, therefore default is returned
        assertEquals("fallback", result);
    }

    @Test
    public void testGetWorksWithDifferentKeyTypes() {
        Map<Integer, String> map = new HashMap<>();
        map.put(10, "ten");
        String result = MapUtilities.get(map, 10, "none");
        assertEquals("ten", result);
    }

    /* -------------------------------------------------------
     * Tests for getOrThrow(...)
     * ------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullMapThrowsNPE() throws Throwable {
        MapUtilities.getOrThrow(null, "key", new IllegalArgumentException("bad"));
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullThrowableThrowsNPE() throws Throwable {
        Map<String, String> map = Collections.singletonMap("k", "v");
        MapUtilities.getOrThrow(map, "k", null);
    }

    @Test
    public void testGetOrThrowReturnsValueWhenKeyPresent() throws Throwable {
        Map<String, String> map = new HashMap<>();
        map.put("present", "value");
        Object result = MapUtilities.getOrThrow(map, "present", new IllegalStateException());
        assertEquals("value", result);
    }

    @Test
    public void testGetOrThrowReturnsNullWhenKeyPresentAndValueIsNull() throws Throwable {
        Map<String, String> map = new HashMap<>();
        map.put("present", null);
        Object result = MapUtilities.getOrThrow(map, "present", new IllegalStateException());
        assertNull(result);
    }

    @Test
    public void testGetOrThrowThrowsProvidedThrowableWhenKeyMissing() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        IllegalArgumentException expected = new IllegalArgumentException("missing");
        try {
            MapUtilities.getOrThrow(map, "b", expected);
            fail("Expected the supplied throwable to be thrown");
        } catch (IllegalArgumentException e) {
            assertSame(expected, e);
        } catch (Throwable t) {
            fail("Unexpected throwable type: " + t);
        }
    }

    /* -------------------------------------------------------
     * Tests for isEmpty(...)
     * ------------------------------------------------------- */
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
        map.put("x", "y");
        assertFalse(MapUtilities.isEmpty(map));
    }

    /* -------------------------------------------------------
     * Tests for dupe(...)
     * ------------------------------------------------------- */
    @Test
    public void testDupeCreatesDeepCopyWhenMutable() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        original.put(String.class, new LinkedHashSet<>(Arrays.asList("a", "b")));

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, false);

        // Verify content equality
        assertEquals(original, copy);
        // Verify that modifying the original does NOT affect the copy
        original.get(String.class).add("c");
        assertFalse(copy.get(String.class).contains("c"));
        // Verify that the copy is mutable
        copy.get(String.class).add("d");
        assertTrue(copy.get(String.class).contains("d"));
    }

    @Test
    public void testDupeCreatesUnmodifiableStructuresWhenRequested() {
        Map<Class<?>, Set<Integer>> original = new LinkedHashMap<>();
        original.put(Integer.class, new LinkedHashSet<>(Arrays.asList(1, 2)));

        Map<Class<?>, Set<Integer>> copy = MapUtilities.dupe(original, true);

        // The map itself must be unmodifiable
        try {
            copy.put(Double.class, new HashSet<>());
            fail("Expected UnsupportedOperationException on map modification");
        } catch (UnsupportedOperationException ignored) {
        }

        // The inner set must be unmodifiable
        try {
            copy.get(Integer.class).add(3);
            fail("Expected UnsupportedOperationException on set modification");
        } catch (UnsupportedOperationException ignored) {
        }

        // Ensure the content matches the original at the time of duplication
        assertEquals(original, copy);
    }

    /* -------------------------------------------------------
     * Tests for cloneMapOfSets(...)
     * ------------------------------------------------------- */
    @Test
    public void testCloneMapOfSetsMutableCopy() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("nums", new HashSet<>(Arrays.asList(1, 2)));

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);

        // Content equality
        assertEquals(original, clone);
        // Mutability of outer map
        clone.put("more", new HashSet<>(Collections.singletonList(3)));
        assertTrue(clone.containsKey("more"));
        // Mutability of inner set
        clone.get("nums").add(4);
        assertTrue(clone.get("nums").contains(4));
        // Original must stay unchanged
        assertFalse(original.get("nums").contains(4));
    }

    @Test
    public void testCloneMapOfSetsImmutableCopy() {
        Map<String, Set<String>> original = new HashMap<>();
        original.put("letters", new HashSet<>(Arrays.asList("a", "b")));

        Map<String, Set<String>> clone = MapUtilities.cloneMapOfSets(original, true);

        // Outer map unmodifiable
        try {
            clone.put("new", new HashSet<>());
            fail("Expected UnsupportedOperationException on map modification");
        } catch (UnsupportedOperationException ignored) {
        }

        // Inner set unmodifiable
        try {
            clone.get("letters").add("c");
            fail("Expected UnsupportedOperationException on set modification");
        } catch (UnsupportedOperationException ignored) {
        }

        // Content must match original at duplication time
        assertEquals(original, clone);
    }

    /* -------------------------------------------------------
     * Tests for cloneMapOfMaps(...)
     * ------------------------------------------------------- */
    @Test
    public void testCloneMapOfMapsMutableCopy() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        original.put("first", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);

        // Verify deep copy
        assertEquals(original, clone);
        // Mutate clone's outer map
        clone.put("second", new HashMap<>());
        assertTrue(clone.containsKey("second"));
        // Mutate inner map in clone
        clone.get("first").put(2, "two");
        assertTrue(clone.get("first").containsKey(2));
        // Original must remain unchanged
        assertFalse(original.get("first").containsKey(2));
    }

    @Test
    public void testCloneMapOfMapsImmutableCopy() {
        Map<String, Map<String, Integer>> original = new HashMap<>();
        Map<String, Integer> inner = new HashMap<>();
        inner.put("a", 10);
        original.put("map", inner);

        Map<String, Map<String, Integer>> clone = MapUtilities.cloneMapOfMaps(original, true);

        // Outer map unmodifiable
        try {
            clone.put("new", new HashMap<>());
            fail("Expected UnsupportedOperationException on outer map");
        } catch (UnsupportedOperationException ignored) {
        }

        // Inner map unmodifiable
        try {
            clone.get("map").put("b", 20);
            fail("Expected UnsupportedOperationException on inner map");
        } catch (UnsupportedOperationException ignored) {
        }

        // Content matches original at duplication time
        assertEquals(original, clone);
    }

    /* -------------------------------------------------------
     * Tests for mapOf() overloads
     * ------------------------------------------------------- */
    @Test
    public void testMapOfZeroArgsReturnsEmptyUnmodifiableMap() {
        Map<String, Integer> map = MapUtilities.mapOf();
        assertTrue(map.isEmpty());
        try {
            map.put("x", 1);
            fail("Expected UnsupportedOperationException on modification");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testMapOfOnePair() {
        Map<String, String> map = MapUtilities.mapOf("k", "v");
        assertEquals(1, map.size());
        assertEquals("v", map.get("k"));
        try {
            map.put("another", "value");
            fail("Expected UnsupportedOperationException on modification");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testMapOfTwoPairsAndOverwriteBehavior() {
        // Duplicate key: second pair should overwrite the first
        Map<Integer, String> map = MapUtilities.mapOf(1, "one", 1, "uno");
        assertEquals(1, map.size());
        assertEquals("uno", map.get(1));
    }

    @Test
    public void testMapOfThreePairsPreservesInsertionOrder() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1, "b", 2, "c", 3);
        Iterator<String> it = map.keySet().iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
    }

    @Test
    public void testMapOfFivePairs() {
        Map<String, String> map = MapUtilities.mapOf(
                "k1", "v1",
                "k2", "v2",
                "k3", "v3",
                "k4", "v4",
                "k5", "v5");
        assertEquals(5, map.size());
        assertEquals("v3", map.get("k3"));
    }

    @Test
    public void testMapOfSixPairsIsUnmodifiable() {
        Map<Integer, Integer> map = MapUtilities.mapOf(
                1, 10,
                2, 20,
                3, 30,
                4, 40,
                5, 50,
                6, 60);
        try {
            map.remove(1);
            fail("Expected UnsupportedOperationException on removal");
        } catch (UnsupportedOperationException ignored) {
        }
        assertEquals(Integer.valueOf(60), map.get(6));
    }
}
