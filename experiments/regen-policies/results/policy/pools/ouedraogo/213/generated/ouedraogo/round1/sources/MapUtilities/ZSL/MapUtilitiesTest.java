package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

public class MapUtilitiesTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Map<String, Integer> sampleMap;

    @Before
    public void setUp() {
        sampleMap = new HashMap<>();
        sampleMap.put("one", 1);
        sampleMap.put("two", 2);
    }

    /* -------------------------------------------------
     * Tests for get(...)
     * ------------------------------------------------- */
    @Test
    public void testGetReturnsValueWhenPresent() {
        Integer result = MapUtilities.get(sampleMap, "one", -1);
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testGetReturnsDefaultWhenNullValue() {
        sampleMap.put("nullKey", null);
        String def = "default";
        String result = MapUtilities.get(sampleMap, "nullKey", def);
        assertEquals(def, result);
    }

    @Test
    public void testGetReturnsDefaultWhenKeyMissing() {
        String def = "default";
        String result = MapUtilities.get(sampleMap, "missing", def);
        assertEquals(def, result);
    }

    /* -------------------------------------------------
     * Tests for getOrThrow(...)
     * ------------------------------------------------- */
    @Test
    public void testGetOrThrowReturnsValueWhenKeyExists() throws Exception {
        Object value = MapUtilities.getOrThrow(sampleMap, "two", new IllegalStateException("should not happen"));
        assertEquals(2, value);
    }

    @Test
    public void testGetOrThrowThrowsProvidedExceptionWhenKeyMissing() throws Exception {
        IllegalArgumentException iae = new IllegalArgumentException("missing key");
        try {
            MapUtilities.getOrThrow(sampleMap, "missing", iae);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertSame(iae, e);
        }
    }

    @Test
    public void testGetOrThrowNullMapThrowsNPE() throws Throwable {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Map parameter cannot be null");
        MapUtilities.getOrThrow(null, "any", new Exception());
    }

    @Test
    public void testGetOrThrowNullThrowableThrowsNPE() throws Throwable {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("Throwable object cannot be null");
        MapUtilities.getOrThrow(sampleMap, "any", null);
    }

    /* -------------------------------------------------
     * Tests for isEmpty(...)
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
        assertFalse(MapUtilities.isEmpty(sampleMap));
    }

    /* -------------------------------------------------
     * Tests for dupe(...)
     * ------------------------------------------------- */
    @Test
    public void testDupeCreatesEqualMap() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b"));
        original.put(String.class, set);

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, false);
        assertEquals(original, copy);
        // ensure deep copy of the set
        assertNotSame(set, copy.get(String.class));
    }

    @Test
    public void testDupeUnmodifiableResult() {
        Map<Class<?>, Set<String>> original = new LinkedHashMap<>();
        original.put(Integer.class, new HashSet<>(Arrays.asList("x")));

        Map<Class<?>, Set<String>> copy = MapUtilities.dupe(original, true);
        assertEquals(original, copy);
        try {
            copy.put(Long.class, Collections.emptySet());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }

        // also verify the inner set is unmodifiable
        try {
            copy.get(Integer.class).add("new");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* -------------------------------------------------
     * Tests for cloneMapOfSets(...)
     * ------------------------------------------------- */
    @Test
    public void testCloneMapOfSetsMutableCopy() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("nums", new HashSet<>(Arrays.asList(1, 2)));

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, false);
        assertEquals(original, clone);
        // Mutating clone should not affect original
        clone.get("nums").add(3);
        assertFalse(original.get("nums").contains(3));
    }

    @Test
    public void testCloneMapOfSetsImmutableCopy() {
        Map<String, Set<Integer>> original = new HashMap<>();
        original.put("nums", new HashSet<>(Arrays.asList(1, 2)));

        Map<String, Set<Integer>> clone = MapUtilities.cloneMapOfSets(original, true);
        assertEquals(original, clone);
        // inner set should be unmodifiable
        try {
            clone.get("nums").add(3);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }

        // outer map should be unmodifiable
        try {
            clone.put("new", Collections.emptySet());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* -------------------------------------------------
     * Tests for cloneMapOfMaps(...)
     * ------------------------------------------------- */
    @Test
    public void testCloneMapOfMapsMutableCopy() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        original.put("inner", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, false);
        assertEquals(original, clone);
        // Mutating clone's inner map should not affect original
        clone.get("inner").put(2, "two");
        assertNull(original.get("inner").get(2));
    }

    @Test
    public void testCloneMapOfMapsImmutableCopy() {
        Map<String, Map<Integer, String>> original = new HashMap<>();
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        original.put("inner", inner);

        Map<String, Map<Integer, String>> clone = MapUtilities.cloneMapOfMaps(original, true);
        assertEquals(original, clone);
        // outer map unmodifiable
        try {
            clone.put("new", Collections.emptyMap());
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
        // inner map unmodifiable
        try {
            clone.get("inner").put(2, "two");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    /* -------------------------------------------------
     * Tests for mapToString(...)
     * ------------------------------------------------- */
    @Test
    public void testMapToStringEmpty() {
        Map<String, Integer> empty = Collections.emptyMap();
        assertEquals("{}", MapUtilities.mapToString(empty));
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
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("self", map);
        map.put("key", "value");
        // Expected: key for self is "(this Map)" per implementation
        assertEquals("{self=(this Map), key=value}", MapUtilities.mapToString(map));
    }

    /* -------------------------------------------------
     * Tests for mapOf(...)
     * ------------------------------------------------- */
    @Test
    public void testMapOfEmptyVarargsReturnsEmptyUnmodifiableMap() {
        Map<Object, Object> result = MapUtilities.mapOf();
        assertTrue(result.isEmpty());
        try {
            result.put("x", "y");
            fail("Expected UnsupportedOperationException");
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

    @Test
    public void testMapOfOddNumberOfArgumentsThrows() {
        try {
            MapUtilities.mapOf("a", 1, "b");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Invalid number of arguments"));
        }
    }

    @Test
    public void testMapOfTooManyEntriesThrows() {
        Object[] many = new Object[22]; // 11 pairs > MAX_ENTRIES (10)
        for (int i = 0; i < many.length; i++) many[i] = i;
        try {
            MapUtilities.mapOf(many);
            fail("Expected IllegalArgumentException for too many entries");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Too many entries"));
        }
    }

    /* -------------------------------------------------
     * Tests for mapOfEntries(...)
     * ------------------------------------------------- */
    @Test
    public void testMapOfEntriesEmptyReturnsEmptyUnmodifiableMap() {
        Map<Object, Object> result = MapUtilities.mapOfEntries();
        assertTrue(result.isEmpty());
        try {
            result.put("x", "y");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
        }
    }

    @Test
    public void testMapOfEntriesValid() {
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("a", 1);
        Map.Entry<String, Integer> e2 = new AbstractMap.SimpleEntry<>("b", 2);
        Map<String, Integer> map = MapUtilities.mapOfEntries(e1, e2);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
    }

    @Test
    public void testMapOfEntriesNullEntryThrows() {
        try {
            MapUtilities.mapOfEntries((Map.Entry<String, Integer>) null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("Entries must not be null"));
        }
    }

    /* -------------------------------------------------
     * Tests for detectMapOrdering(...)
     * Note: The implementation falls back to detecting standard JDK map types.
     * Since we cannot instantiate CompactMap, CaseInsensitiveMap, or TrackingMap
     * without their constructors, we test the fallback logic.
     * ------------------------------------------------- */
    @Test
    public void testDetectMapOrderingNullReturnsUnordered() {
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(null));
    }

    @Test
    public void testDetectMapOrderingSortedMap() {
        SortedMap<String, Integer> treeMap = new TreeMap<>();
        assertEquals(CompactMap.SORTED, MapUtilities.detectMapOrdering(treeMap));
    }

    @Test
    public void testDetectMapOrderingLinkedHashMap() {
        Map<String, Integer> linked = new LinkedHashMap<>();
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(linked));
    }

    @Test
    public void testDetectMapOrderingEnumMap() {
        EnumMap<TestEnum, String> enumMap = new EnumMap<>(TestEnum.class);
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(enumMap));
    }

    @Test
    public void testDetectMapOrderingHashMapUnordered() {
        Map<String, Integer> hash = new HashMap<>();
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(hash));
    }

    private enum TestEnum { ONE, TWO }
}
