package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;

import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link MapUtilities}.
 */
public class MapUtilitiesTest {

    /* ---------- 1. get(...) ---------- */

    @Test
    public void testGetFound() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertEquals(Integer.valueOf(1), MapUtilities.get(map, "a", 99));
    }

    @Test
    public void testGetNotFoundReturnsDefault() {
        Map<String, Integer> map = new HashMap<>();
        assertEquals(Integer.valueOf(99), MapUtilities.get(map, "missing", 99));
    }

    @Test
    public void testGetNullMapThrowsNPE() {
        assertThrows(NullPointerException.class,
                () -> MapUtilities.get(null, "k", "def"));
    }

    @Test
    public void testGetKeyPresentWithNullValueReturnsDefault() {
        Map<String, String> map = new HashMap<>();
        map.put("k", null);
        assertEquals("def", MapUtilities.get(map, "k", "def"));
    }

    /* ---------- 2. getOrThrow(...) ---------- */

    @Test
    public void testGetOrThrowFound() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("k", "v");
        assertEquals("v", MapUtilities.getOrThrow(map, "k", new RuntimeException()));
    }

    @Test
    public void testGetOrThrowFoundNullValue() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("k", null);
        assertNull(MapUtilities.getOrThrow(map, "k", new RuntimeException()));
    }

    @Test
    public void testGetOrThrowMissingThrowsProvided() {
        Map<String, String> map = new HashMap<>();
        IllegalStateException ex = new IllegalStateException("missing");
        try {
            MapUtilities.getOrThrow(map, "nope", ex);
            fail("Expected exception not thrown");
        } catch (IllegalStateException e) {
            assertSame(ex, e);
        }
    }

    @Test
    public void testGetOrThrowNullMap() {
        assertThrows(NullPointerException.class,
                () -> MapUtilities.getOrThrow(null, "k", new RuntimeException()));
    }

    @Test
    public void testGetOrThrowNullThrowable() {
        Map<String, String> map = new HashMap<>();
        assertThrows(NullPointerException.class,
                () -> MapUtilities.getOrThrow(map, "k", null));
    }

    /* ---------- 3. isEmpty(...) ---------- */

    @Test
    public void testIsEmpty() {
        assertTrue(MapUtilities.isEmpty(null));
        assertTrue(MapUtilities.isEmpty(Collections.emptyMap()));
        assertFalse(MapUtilities.isEmpty(Collections.singletonMap("x", 1)));
    }

    /* ---------- 4. dupe(...) ---------- */

    @Test
    public void testDupeModifiable() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        Map<Class<?>, Set<String>> src = new HashMap<>();
        src.put(String.class, set);
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(src, false);
        assertEquals(set, copy.get(String.class));

        // modify original – copy must stay unchanged
        set.add("b");
        assertFalse(copy.get(String.class).contains("b"));
    }

    @Test
    public void testDupeUnmodifiable() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        Map<Class<?>, Set<String>> src = new HashMap<>();
        src.put(String.class, set);
        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(src, true);
        assertEquals(set, copy.get(String.class));

        // map and inner set must be unmodifiable
        assertThrows(UnsupportedOperationException.class,
                () -> copy.put(Integer.class, Collections.emptySet()));
        assertThrows(UnsupportedOperationException.class,
                () -> copy.get(String.class).add("b"));
    }

    /* ---------- 5. cloneMapOfSets(...) ---------- */

    @Test
    public void testCloneMapOfSetsMutable() {
        Set<Integer> s = new HashSet<>(Arrays.asList(1, 2));
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("nums", s);
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);
        assertEquals(s, clone.get("nums"));

        // change original – clone must not see it
        s.add(3);
        assertFalse(clone.get("nums").contains(3));

        // clone is mutable
        clone.get("nums").add(4);
        assertTrue(clone.get("nums").contains(4));
    }

    @Test
    public void testCloneMapOfSetsImmutable() {
        Set<Integer> s = new HashSet<>(Arrays.asList(1, 2));
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("nums", s);
        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, true);
        assertThrows(UnsupportedOperationException.class,
                () -> clone.put("other", Collections.emptySet()));
        assertThrows(UnsupportedOperationException.class,
                () -> clone.get("nums").add(5));
    }

    /* ---------- 6. cloneMapOfMaps(...) ---------- */

    @Test
    public void testCloneMapOfMapsMutable() {
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        Map<String, Map<Integer, String>> original = new HashMap<>();
        original.put("inner", inner);
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);
        assertEquals(inner, clone.get("inner"));

        // modify original inner map – clone must stay unchanged
        inner.put(2, "two");
        assertNull(clone.get("inner").get(2));

        // clone is mutable
        clone.get("inner").put(3, "three");
        assertEquals("three", clone.get("inner").get(3));
    }

    @Test
    public void testCloneMapOfMapsImmutable() {
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        Map<String, Map<Integer, String>> original = new HashMap<>();
        original.put("inner", inner);
        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, true);
        assertThrows(UnsupportedOperationException.class,
                () -> clone.put("new", Collections.emptyMap()));
        assertThrows(UnsupportedOperationException.class,
                () -> clone.get("inner").put(2, "two"));
    }

    /* ---------- 7. mapToString(...) ---------- */

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
        Map<Object, Object> m = new HashMap<>();
        m.put(m, "value");
        m.put("key", m);
        assertEquals("{(this Map)=value, key=(this Map)}", MapUtilities.mapToString(m));
    }

    /* ---------- 8. mapOf(...) ---------- */

    @Test
    public void testMapOfEmpty() {
        Map<Object, Object> empty = MapUtilities.mapOf();
        assertTrue(empty.isEmpty());
        // the returned map should be unmodifiable – any write operation fails
        assertThrows(UnsupportedOperationException.class,
                () -> empty.put("x", "y"));
    }

    @Test
    public void testMapOfNormal() {
        Map<String, Integer> map = MapUtilities.mapOf("one", 1, "two", 2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("two"));
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("three", 3));
    }

    @Test
    public void testMapOfOddArgumentCount() {
        assertThrows(IllegalArgumentException.class,
                () -> MapUtilities.mapOf("onlyKey"));
    }

    @Test
    public void testMapOfTooManyEntries() {
        Object[] many = new Object[22]; // 11 key/value pairs → exceeds limit 10
        for (int i = 0; i < many.length; i++) many[i] = i;
        assertThrows(IllegalArgumentException.class,
                () -> MapUtilities.mapOf(many));
    }

    /* ---------- 9. mapOfEntries(...) ---------- */

    @Test
    public void testMapOfEntriesEmpty() {
        Map<String, Integer> map = MapUtilities.mapOfEntries();
        assertTrue(map.isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("x", 1));
    }

    @Test
    public void testMapOfEntriesNormal() {
        Map.Entry<String, Integer> e1 = new SimpleEntry<>("a", 1);
        Map.Entry<String, Integer> e2 = new SimpleEntry<>("b", 2);
        Map<String, Integer> map = MapUtilities.mapOfEntries(e1, e2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertThrows(UnsupportedOperationException.class,
                () -> map.remove("a"));
    }

    @Test
    public void testMapOfEntriesNullArray() {
        assertThrows(NullPointerException.class,
                () -> MapUtilities.mapOfEntries((Map.Entry<?, ?>[]) null));
    }

    @Test
    public void testMapOfEntriesNullEntry() {
        Map.Entry<String, Integer> e = null;
        assertThrows(NullPointerException.class,
                () -> MapUtilities.mapOfEntries(e));
    }
}
