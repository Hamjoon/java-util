package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_mapOfEntries_Test {

    private static Stream<Arguments> nullOrEmptyInputProvider() {
        return Stream.of(// null var‑args array
        Arguments.of((Object) null), // empty array (no entries)
        Arguments.of(new Map.Entry[0]));
    }

    private static Stream<Arguments> nullEntryProvider() {
        return Stream.of(// Single null entry
        Arguments.of(new Map.Entry[] { null }), // Mixed array with a null in the middle
        Arguments.of(new Map.Entry[] { new AbstractMap.SimpleEntry<>("k1", "v1"), null, new AbstractMap.SimpleEntry<>("k2", "v2") }));
    }

    private static Stream<Arguments> validEntriesProvider() {
        return Stream.of(// Single entry
        Arguments.of(new Map.Entry[] { new AbstractMap.SimpleEntry<>("a", 1) }, Map.of("a", 1), "Single non‑null entry"), // Multiple entries, order preserved
        Arguments.of(new Map.Entry[] { new AbstractMap.SimpleEntry<>("k1", "v1"), new AbstractMap.SimpleEntry<>("k2", "v2") }, Map.of("k1", "v1", "k2", "v2"), "Multiple entries with insertion order"), // Duplicate keys – later entry wins
        Arguments.of(new Map.Entry[] { new AbstractMap.SimpleEntry<>("dup", 1), new AbstractMap.SimpleEntry<>("dup", 2) }, Map.of("dup", 2), "Duplicate keys, last value overrides"), // Null key and null value are allowed by LinkedHashMap
        Arguments.of(new Map.Entry[] { new AbstractMap.SimpleEntry<>(null, "nullKey"), new AbstractMap.SimpleEntry<>("notNull", null) }, // Use a LinkedHashMap to preserve order for the expected map
        new LinkedHashMap<>() {

            {
                put(null, "nullKey");
                put("notNull", null);
            }
        }, "Null key and null value handling"));
    }

    @Test
    void mapOfEntriesReturnsEmptyUnmodifiableMapWhenNullArray() {
        Map<String, Integer> result = MapUtilities.mapOfEntries((Map.Entry<String, Integer>[]) null);
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "Map should be empty");
        assertThrows(UnsupportedOperationException.class, () -> result.put("a", 1));
        assertTrue(result instanceof java.util.Collections.UnmodifiableMap, "Map should be unmodifiable");
    }

    @Test
    void mapOfEntriesReturnsEmptyUnmodifiableMapWhenEmptyArray() {
        Map<String, Integer> result = MapUtilities.mapOfEntries();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.put("a", 1));
    }

    @Test
    void mapOfEntriesCreatesMapWithProvidedEntries() {
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("one", 1);
        Map.Entry<String, Integer> e2 = new AbstractMap.SimpleEntry<>("two", 2);
        Map.Entry<String, Integer> e3 = new AbstractMap.SimpleEntry<>("three", 3);
        Map<String, Integer> result = MapUtilities.mapOfEntries(e1, e2, e3);
        assertEquals(3, result.size());
        assertEquals(1, result.get("one"));
        assertEquals(2, result.get("two"));
        assertEquals(3, result.get("three"));
        // insertion order must be preserved (LinkedHashMap semantics)
        List<String> keys = new ArrayList<>(result.keySet());
        assertEquals(List.of("one", "two", "three"), keys);
    }

    @Test
    void mapOfEntriesOverridesDuplicateKeysWithLastValue() {
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("dup", 1);
        // later entry wins
        Map.Entry<String, Integer> e2 = new AbstractMap.SimpleEntry<>("dup", 2);
        Map<String, Integer> result = MapUtilities.mapOfEntries(e1, e2);
        assertEquals(1, result.size());
        assertEquals(2, result.get("dup"));
    }

    @Test
    void mapOfEntriesThrowsNPEWhenAnyEntryIsNull() {
        Map.Entry<String, Integer> good = new AbstractMap.SimpleEntry<>("good", 1);
        Map.Entry<String, Integer> nullEntry = null;
        assertThrows(NullPointerException.class, () -> MapUtilities.mapOfEntries(good, nullEntry));
    }

    @Test
    void mapOfEntriesResultIsUnmodifiable() {
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("k", 10);
        Map<String, Integer> map = MapUtilities.mapOfEntries(e1);
        assertAll("unmodifiable checks", () -> assertThrows(UnsupportedOperationException.class, () -> map.put("new", 20)), () -> assertThrows(UnsupportedOperationException.class, () -> map.remove("k")), () -> assertThrows(UnsupportedOperationException.class, map::clear));
    }

    @Test
    void mapOfEntriesResultIsImmutableEvenWhenOriginalArrayIsModified() {
        // Build an array of entries, then modify the array after the call.
        @SuppressWarnings("unchecked")
        Map.Entry<String, Integer>[] array = new Map.Entry[] { new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2) };
        Map<String, Integer> result = MapUtilities.mapOfEntries(array);
        // Change the original array contents – the returned map must stay unchanged.
        array[0] = new AbstractMap.SimpleEntry<>("a", 99);
        array[1] = new AbstractMap.SimpleEntry<>("b", 88);
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    void mapOfEntriesResultHasCorrectToStringBehaviour() {
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("x", 42);
        Map<String, Integer> map = MapUtilities.mapOfEntries(e1);
        // The toString of an unmodifiable map delegates to the underlying map's toString.
        assertEquals("{x=42}", map.toString());
    }

    @Test
    void mapOfEntriesResultIsSameInstanceWhenCalledMultipleTimesWithSameContent() {
        // Not required by contract, but we verify that each call returns a distinct map instance.
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("k", 5);
        Map<String, Integer> first = MapUtilities.mapOfEntries(e1);
        Map<String, Integer> second = MapUtilities.mapOfEntries(e1);
        assertNotSame(first, second);
        assertEquals(first, second);
    }

    /* -------------------------------------------------------------
     *  Null or Empty Input
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] entries = {0}")
    @MethodSource("nullOrEmptyInputProvider")
    void mapOfEntriesReturnsEmptyUnmodifiableMapWhenNullOrEmpty(Map.Entry<?, ?>[] entries) {
        Map<?, ?> result = MapUtilities.mapOfEntries(entries);
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "Map should be empty");
        // The map must be immutable – any mutating operation should fail.
        assertThrows(UnsupportedOperationException.class, () -> result.put("a", 1));
        // The concrete type is a Collections$UnmodifiableMap wrapping a LinkedHashMap.
        assertTrue(result.getClass().getName().contains("UnmodifiableMap"), "Map should be an unmodifiable view");
    }

    /* -------------------------------------------------------------
     *  Valid Entries
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] description: {2}")
    @MethodSource("validEntriesProvider")
    void mapOfEntriesCreatesCorrectUnmodifiableMap(Map.Entry<?, ?>[] entries, Map<Object, Object> expected, String description) {
        Map<Object, Object> result = MapUtilities.mapOfEntries(entries);
        // Verify content
        assertEquals(expected, result, "Map content must match expected");
        // Verify insertion order (LinkedHashMap semantics)
        List<Object> expectedOrder = new ArrayList<>(expected.keySet());
        List<Object> actualOrder = new ArrayList<>(result.keySet());
        assertEquals(expectedOrder, actualOrder, "Insertion order must be preserved");
        // Verify immutability
        assertThrows(UnsupportedOperationException.class, () -> result.put("newKey", "newVal"));
        assertThrows(UnsupportedOperationException.class, () -> result.remove(expectedOrder.get(0)));
        assertThrows(UnsupportedOperationException.class, result::clear);
    }

    /* -------------------------------------------------------------
     *  Distinct instances on successive calls (contract not required,
     *  but useful sanity check)
     * ------------------------------------------------------------- */
    @Test
    void mapOfEntriesResultIsDistinctInstancePerInvocation() {
        Map.Entry<String, Integer> e1 = new AbstractMap.SimpleEntry<>("k", 5);
        Map<String, Integer> first = MapUtilities.mapOfEntries(e1);
        Map<String, Integer> second = MapUtilities.mapOfEntries(e1);
        assertNotSame(first, second, "Each call should produce a new map instance");
        assertEquals(first, second, "Maps with same content must be equal");
    }
}
