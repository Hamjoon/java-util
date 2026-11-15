package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class MapUtilitiesTest {

    /* -------------------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------------------- */

    @Test
    public void testGetFoundAndDefault() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        assertEquals(Integer.valueOf(1), MapUtilities.get(map, "one", 99));
        assertEquals(Integer.valueOf(99), MapUtilities.get(map, "two", 99));
    }

    @Test
    public void testGetOrThrowFound() throws Throwable {
        Map<String, String> map = new HashMap<>();
        map.put("k", null);           // key present with null value
        map.put("v", "value");
        assertNull(MapUtilities.getOrThrow(map, "k", new IllegalStateException()));
        assertEquals("value", MapUtilities.getOrThrow(map, "v", new IllegalStateException()));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(MapUtilities.isEmpty(null));
        assertTrue(MapUtilities.isEmpty(new HashMap<>()));
        Map<String, String> map = new HashMap<>();
        map.put("a", "b");
        assertFalse(MapUtilities.isEmpty(map));
    }

    @Test
    public void testDupeMutableCopy() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        Set<String> setA = new HashSet<>(Arrays.asList("a1", "a2"));
        Set<String> setB = new HashSet<>(Collections.singletonList("b1"));
        original.put(String.class, setA);
        original.put(Integer.class, setB);

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, false);

        // content equality
        assertEquals(original, copy);
        // modifications to original do not affect copy
        setA.add("a3");
        assertFalse(copy.get(String.class).contains("a3"));
    }

    @Test
    public void testDupeUnmodifiable() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        original.put(String.class, new HashSet<>(Arrays.asList("x", "y")));

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, true);
        assertEquals(original, copy);
        try {
            copy.put(Long.class, new HashSet<>());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testCloneMapOfSetsMutable() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("nums", new HashSet<>(Arrays.asList(1, 2)));

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);
        assertEquals(original, clone);

        // ensure deep copy
        original.get("nums").add(3);
        assertFalse(clone.get("nums").contains(3));
    }

    @Test
    public void testCloneMapOfSetsImmutable() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("nums", new HashSet<>(Arrays.asList(1, 2)));

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, true);
        try {
            clone.get("nums").add(5);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }

        // the outer map itself is also immutable
        try {
            clone.put("more", new HashSet<>());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testCloneMapOfMapsMutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        original.put("inner", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);
        assertEquals(original, clone);

        // deep copy verification
        inner.put(2, "two");
        assertFalse(clone.get("inner").containsKey(2));
    }

    @Test
    public void testCloneMapOfMapsImmutable() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        original.put("inner", new HashMap<>(Collections.singletonMap(1, "one")));

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, true);
        try {
            clone.get("inner").put(2, "two");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }

        try {
            clone.put("new", new HashMap<>());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
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
        map.put("key", "value");
        // Expected: {self=(this Map), key=value}
        assertEquals("{self=(this Map), key=value}", MapUtilities.mapToString(map));
    }

    @Test
    public void testMapOfBasic() {
        Map<String, Integer> map = MapUtilities.mapOf("x", 10, "y", 20);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(10), map.get("x"));
        assertEquals(Integer.valueOf(20), map.get("y"));
        // verify immutability
        try {
            map.put("z", 30);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testMapOfEmpty() {
        Map<Object, Object> empty = MapUtilities.mapOf();
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testMapOfEntriesBasic() {
        Map.Entry<String, String> e1 = new AbstractMap.SimpleEntry<>("k1", "v1");
        Map.Entry<String, String> e2 = new AbstractMap.SimpleEntry<>("k2", "v2");
        Map<String, String> map = MapUtilities.mapOfEntries(e1, e2);
        assertEquals(2, map.size());
        assertEquals("v1", map.get("k1"));
        assertEquals("v2", map.get("k2"));
        try {
            map.put("k3", "v3");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) { }
    }

    @Test
    public void testGetMapStructureStringPlainMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String description = MapUtilities.getMapStructureString(map);
        assertTrue(description.contains("LinkedHashMap"));
    }

    @Test
    public void testDetectMapOrderingVariousMaps() {
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(null));

        Map<String, String> hash = new HashMap<>();
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(hash));

        Map<String, String> linked = new LinkedHashMap<>();
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(linked));

        Map<String, String> tree = new TreeMap<>();
        assertEquals(CompactMap.SORTED, MapUtilities.detectMapOrdering(tree));

        // EnumMap also yields INSERTION
        Map<Thread.State, String> enumMap = new EnumMap<>(Thread.State.class);
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(enumMap));
    }

    /* -------------------------------------------------------------
     *  Edge‑case / exception tests
     * ------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullMap() throws Throwable {
        MapUtilities.getOrThrow(null, "any", new IllegalArgumentException());
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowNullThrowable() throws Throwable {
        Map<String, String> map = new HashMap<>();
        map.put("a", "b");
        MapUtilities.getOrThrow(map, "c", null);
    }

    @Test
    public void testGetOrThrowKeyAbsentThrowsProvided() {
        Map<String, Integer> map = new HashMap<>();
        map.put("x", 1);
        IllegalStateException ex = new IllegalStateException("missing");
        try {
            MapUtilities.getOrThrow(map, "y", ex);
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertSame(ex, e);
        }
    }

    @Test
    public void testMapOfNullKeyValues() {
        Map<?, ?> map = MapUtilities.mapOf((Object[]) null);
        assertTrue(map.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapOfOddNumberOfArguments() {
        MapUtilities.mapOf("onlyKey");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMapOfTooManyEntries() {
        // 11 entries => 22 arguments > MAX_ENTRIES (10)
        Object[] args = new Object[22];
        for (int i = 0; i < 22; i++) {
            args[i] = i;
        }
        MapUtilities.mapOf(args);
    }

    @Test
    public void testMapOfAllowsNullKeyAndValue() {
        Map<String, String> map = MapUtilities.mapOf(null, "v", "k", null);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertEquals("v", map.get(null));
        assertNull(map.get("k"));
    }

    @Test
    public void testMapOfEntriesNullArray() {
        Map<Object, Object> map = MapUtilities.mapOfEntries((Map.Entry<?, ?>[]) null);
        assertTrue(map.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void testMapOfEntriesContainsNullEntry() {
        Map.Entry<String, String> e1 = new AbstractMap.SimpleEntry<>("k", "v");
        MapUtilities.mapOfEntries(e1, null);
    }

    /* -------------------------------------------------------------
     *  Helper methods (none needed)
     * ------------------------------------------------------------- */
}
