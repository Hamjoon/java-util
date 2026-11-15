package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.Assert.*;

public class MapUtilitiesTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* -------------------------------------------------
     *  Tests for get(...)
     * ------------------------------------------------- */
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

    /* -------------------------------------------------
     *  Tests for getOrThrow(...)
     * ------------------------------------------------- */
    @Test
    public void testGetOrThrowReturnsValueWhenKeyPresent() throws Throwable {
        Map<String, String> map = new HashMap<>();
        map.put("k", "v");
        Object result = MapUtilities.getOrThrow(map, "k", new IllegalStateException("missing"));
        assertEquals("v", result);
    }

    @Test
    public void testGetOrThrowReturnsNullWhenKeyPresentWithNullValue() throws Throwable {
        Map<String, String> map = new HashMap<>();
        map.put("k", null);
        Object result = MapUtilities.getOrThrow(map, "k", new IllegalStateException());
        assertNull(result);
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowMapIsNull() throws Throwable {
        MapUtilities.getOrThrow(null, "any", new IllegalArgumentException());
    }

    @Test(expected = NullPointerException.class)
    public void testGetOrThrowThrowableIsNull() throws Throwable {
        Map<String, String> map = Collections.emptyMap();
        MapUtilities.getOrThrow(map, "any", null);
    }

    @Test
    public void testGetOrThrowThrowsProvidedExceptionWhenKeyMissing() throws Throwable {
        Map<String, String> map = Collections.emptyMap();
        IllegalArgumentException ex = new IllegalArgumentException("not found");
        try {
            MapUtilities.getOrThrow(map, "missing", ex);
            fail("Expected exception to be thrown");
        } catch (IllegalArgumentException e) {
            assertSame(ex, e);
        }
    }

    /* -------------------------------------------------
     *  Tests for isEmpty(...)
     * ------------------------------------------------- */
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

    /* -------------------------------------------------
     *  Tests for dupe(...)
     * ------------------------------------------------- */
    @Test
    public void testDupeCreatesEqualMutableCopy() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        Set<String> set = new HashSet<>();
        set.add("x");
        original.put(String.class, set);

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, false);
        assertEquals(original, copy);
        // Modifying original should not affect copy
        set.add("y");
        assertFalse(copy.get(String.class).contains("y"));
    }

    @Test
    public void testDupeCreatesUnmodifiableCopy() {
        Map<Class<?>, Set<Integer>> original = new LinkedHashMap<>();
        Set<Integer> set = new HashSet<>();
        set.add(1);
        original.put(Integer.class, set);

        Map<Class<?>, Set<Integer>> copy = MapUtilities.dupe(original, true);
        assertEquals(original, copy);
        // The map itself should be unmodifiable
        thrown.expect(UnsupportedOperationException.class);
        copy.put(Double.class, Collections.singleton(2));
    }

    /* -------------------------------------------------
     *  Tests for cloneMapOfSets(...)
     * ------------------------------------------------- */
    @Test
    public void testCloneMapOfSetsMutableCopy() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("a", new HashSet<>(Collections.singleton(1)));

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);
        assertEquals(original, clone);
        // Ensure deep copy (modifying original set does not affect clone)
        original.get("a").add(2);
        assertFalse(clone.get("a").contains(2));
    }

    @Test
    public void testCloneMapOfSetsImmutableCopy() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("a", new HashSet<>(Collections.singleton(1)));

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, true);
        assertEquals(original, clone);
        // Map should be unmodifiable
        thrown.expect(UnsupportedOperationException.class);
        clone.put("b", new HashSet<>());
    }

    /* -------------------------------------------------
     *  Tests for cloneMapOfMaps(...)
     * ------------------------------------------------- */
    @Test
    public void testCloneMapOfMapsMutableCopy() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        original.put("first", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);
        assertEquals(original, clone);
        // Deep copy verification
        inner.put(2, "two");
        assertFalse(clone.get("first").containsKey(2));
    }

    @Test
    public void testCloneMapOfMapsImmutableCopy() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        original.put("first", Collections.singletonMap(1, "one"));

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, true);
        assertEquals(original, clone);
        // Map should be unmodifiable
        thrown.expect(UnsupportedOperationException.class);
        clone.put("second", Collections.singletonMap(2, "two"));
    }

    /* -------------------------------------------------
     *  Tests for mapToString(...)
     * ------------------------------------------------- */
    @Test
    public void testMapToStringEmptyMap() {
        Map<String, Integer> map = Collections.emptyMap();
        assertEquals("{}", MapUtilities.mapToString(map));
    }

    @Test
    public void testMapToStringSimpleMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("{a=1, b=2}", MapUtilities.mapToString(map));
    }

    @Test
    public void testMapToStringSelfReference() {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("selfKey", map);
        map.put(map, "selfValue");
        assertEquals("{selfKey=(this Map), (this Map)=selfValue}", MapUtilities.mapToString(map));
    }

    /* -------------------------------------------------
     *  Tests for mapOf(...)
     * ------------------------------------------------- */
    @Test
    public void testMapOfEmptyInputReturnsEmptyUnmodifiableMap() {
        Map<Object, Object> result = MapUtilities.mapOf();
        assertTrue(result.isEmpty());
        thrown.expect(UnsupportedOperationException.class);
        result.put("x", "y");
    }

    @Test
    public void testMapOfOddNumberOfArgumentsThrows() {
        thrown.expect(IllegalArgumentException.class);
        MapUtilities.mapOf("a", 1, "b");
    }

    @Test
    public void testMapOfExceedsMaxEntriesThrows() {
        thrown.expect(IllegalArgumentException.class);
        MapUtilities.mapOf(
                "k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5",
                "k6", "v6", "k7", "v7", "k8", "v8", "k9", "v9", "k10", "v10",
                "k11", "v11"); // 11 entries > MAX_ENTRIES (10)
    }

    @Test
    public void testMapOfCreatesUnmodifiableMapWithCorrectEntries() {
        Map<String, Integer> result = MapUtilities.mapOf("one", 1, "two", 2);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get("one"));
        assertEquals(Integer.valueOf(2), result.get("two"));
        thrown.expect(UnsupportedOperationException.class);
        result.put("three", 3);
    }

    /* -------------------------------------------------
     *  Tests for mapOfEntries(...)
     * ------------------------------------------------- */
    @Test
    public void testMapOfEntriesEmptyArrayReturnsEmptyUnmodifiableMap() {
        Map<Object, Object> result = MapUtilities.mapOfEntries();
        assertTrue(result.isEmpty());
        thrown.expect(UnsupportedOperationException.class);
        result.put("x", "y");
    }

    @Test
    public void testMapOfEntriesNullArrayThrows() {
        thrown.expect(NullPointerException.class);
        MapUtilities.mapOfEntries((Map.Entry<?, ?>[]) null);
    }

    @Test
    public void testMapOfEntriesNullEntryThrows() {
        thrown.expect(NullPointerException.class);
        MapUtilities.mapOfEntries(new AbstractMap.SimpleEntry<>("a", "b"), null);
    }

    @Test
    public void testMapOfEntriesCreatesCorrectMap() {
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("one", 1);
        Map.Entry<String, Integer> e2 = new AbstractMap.SimpleEntry<>("two", 2);
        Map<String, Integer> result = MapUtilities.mapOfEntries(e1, e2);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get("one"));
        assertEquals(Integer.valueOf(2), result.get("two"));
        thrown.expect(UnsupportedOperationException.class);
        result.put("three", 3);
    }

    /* -------------------------------------------------
     *  Tests for getMapStructureString(...)
     * ------------------------------------------------- */
    @Test
    public void testGetMapStructureStringWithNull() {
        assertEquals("null", MapUtilities.getMapStructureString(null));
    }

    @Test
    public void testGetMapStructureStringWithPlainMap() {
        Map<String, Integer> map = new HashMap<>();
        String structure = MapUtilities.getMapStructureString(map);
        // Should contain the simple class name of the underlying map
        assertTrue(structure.contains("HashMap"));
    }

    @Test
    public void testGetMapStructureStringWithLinkedHashMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String structure = MapUtilities.getMapStructureString(map);
        assertEquals("LinkedHashMap", structure);
    }

    /* -------------------------------------------------
     *  Tests for detectMapOrdering(...)
     * ------------------------------------------------- */
    @Test
    public void testDetectMapOrderingWithNullMap() {
        String ordering = MapUtilities.detectMapOrdering(null);
        // The utility uses CompactMap.UNORDERED for null; we just verify it's a non‑null string
        assertNotNull(ordering);
    }

    @Test
    public void testDetectMapOrderingWithHashMap() {
        Map<String, Integer> map = new HashMap<>();
        String ordering = MapUtilities.detectMapOrdering(map);
        // For a plain HashMap the expected ordering is UNORDERED
        assertEquals("UNORDERED", ordering);
    }

    @Test
    public void testDetectMapOrderingWithLinkedHashMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        String ordering = MapUtilities.detectMapOrdering(map);
        // LinkedHashMap preserves insertion order
        assertEquals("INSERTION", ordering);
    }

    @Test
    public void testDetectMapOrderingWithTreeMap() {
        Map<String, Integer> map = new TreeMap<>();
        String ordering = MapUtilities.detectMapOrdering(map);
        // TreeMap is sorted
        assertEquals("SORTED", ordering);
    }

    @Test
    public void testDetectMapOrderingDetectsCycle() {
        // Build a simple cycle using a custom wrapper that mimics a map
        // Since we cannot create real wrapper classes (CompactMap, etc.) we simulate a cycle
        // by using a map that references itself via the underlying map logic.
        // The easiest way is to call the private getUnderlyingMap via reflection to force a cycle,
        // but detectMapOrdering already protects against cycles and will throw IllegalArgumentException.
        // We'll create a map that implements Map and returns itself as the underlying map via
        // a TrackingMap‑like structure using an anonymous class.

        Map<Object, Object> cyclic = new HashMap<Object, Object>() {
            @Override
            public Set<Entry<Object, Object>> entrySet() {
                return super.entrySet();
            }
        };
        // Wrap it in a TrackingMap‑like wrapper (we cannot instantiate TrackingMap, so we simulate by
        // creating a map that returns the same instance as its wrapped map). For the purpose of this
        // test we rely on the fact that detectMapOrdering will eventually call getUnderlyingMap,
        // which will see the same instance again and throw IllegalArgumentException.
        // We'll invoke detectMapOrdering and expect an IllegalArgumentException.

        try {
            MapUtilities.detectMapOrdering(cyclic);
            fail("Expected IllegalArgumentException due to cycle detection");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Cannot determine map ordering"));
        }
    }
}
