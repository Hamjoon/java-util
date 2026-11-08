package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_toString_Test {

    /**
     * The map under test – created fresh for each test method.
     */
    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Use the builder (the recommended way) to obtain a default CompactMap.
        map = CompactMap.<String, Integer>builder().build();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the map to help the GC; not strictly required.
        map.clear();
        map = null;
    }

    /**
     * Helper to create a map with the given options via {@code CompactMap.newMap}.
     */
    private static <K, V> CompactMap<K, V> newMapWithOptions(Map<String, Object> options) {
        // Suppress unchecked warning – the factory method returns a correctly typed map.
        @SuppressWarnings("unchecked")
        CompactMap<K, V> map = (CompactMap<K, V>) CompactMap.newMap(options);
        return map;
    }

    @Test
    void emptyMapToString() {
        // An empty CompactMap should render as "{}"
        assertEquals("{}", map.toString(), "toString() of an empty CompactMap should match the standard empty map representation");
    }

    @Test
    void singleEntryToString() {
        map.put("one", 1);
        // Build the expected string using a regular HashMap – its toString() format is well‑defined.
        Map<String, Integer> expected = new HashMap<>();
        expected.put("one", 1);
        assertEquals(expected.toString(), map.toString(), "toString() with a single entry should match the standard map representation");
    }

    @Test
    void multipleEntriesToString() {
        map.put("alpha", 10);
        map.put("beta", 20);
        map.put("gamma", 30);
        // Expected representation using a normal HashMap (unordered, but iteration order of HashMap
        // is not guaranteed – therefore we compare the sets of key‑value pairs rather than the raw string).
        Map<String, Integer> expected = new HashMap<>();
        expected.put("alpha", 10);
        expected.put("beta", 20);
        expected.put("gamma", 30);
        String actual = map.toString();
        String expectedString = expected.toString();
        // Verify that the actual string starts and ends with braces.
        assertTrue(actual.startsWith("{") && actual.endsWith("}"), "toString() should start with '{' and end with '}'");
        // Remove the surrounding braces and split on commas to compare individual entries irrespective of order.
        String[] actualEntries = actual.substring(1, actual.length() - 1).split(",\\s*");
        String[] expectedEntries = expectedString.substring(1, expectedString.length() - 1).split(",\\s*");
        assertEquals(expectedEntries.length, actualEntries.length, "Number of entries in toString() output should match the map size");
        // Convert to sets for order‑independent comparison.
        assertEquals(Set.of(expectedEntries), Set.of(actualEntries), "All key=value pairs should be present in the toString() output");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullKeyAndNullValue() {
        CompactMap<String, String> map = new CompactMap<>();
        map.put(null, "value");
        map.put("key", null);
        // Expected representation using a regular HashMap (order undefined)
        Map<String, String> expected = new HashMap<>();
        expected.put(null, "value");
        expected.put("key", null);
        String actual = map.toString();
        String expectedStr = expected.toString();
        // Compare entry sets irrespective of order.
        assertEquals(Set.of(expectedStr.substring(1, expectedStr.length() - 1).split(",\\s*")), Set.of(actual.substring(1, actual.length() - 1).split(",\\s*")), "toString() must contain both null key/value pairs");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void sortedMapOrdering() {
        Map<String, Object> options = new HashMap<>();
        // request a TreeMap backing
        options.put(CompactMap.MAP_TYPE, TreeMap.class);
        CompactMap<String, Integer> map = newMapWithOptions(options);
        map.put("b", 2);
        map.put("a", 1);
        assertEquals("{a=1, b=2}", map.toString(), "Sorted backing (TreeMap) must render entries in key order");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void selfReferenceValue() {
        CompactMap<String, Object> map = new CompactMap<>();
        // value points to the map itself
        map.put("self", map);
        // The call must not throw; we only assert that the output contains the key prefix.
        String result = assertDoesNotThrow(map::toString, "toString() should handle self‑reference gracefully");
        assertTrue(result.contains("self="), "Result should contain the self‑reference key prefix");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void caseInsensitiveKeyOverwrite() {
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.CASE_SENSITIVE, Boolean.FALSE);
        CompactMap<String, Integer> map = newMapWithOptions(options);
        map.put("Key", 1);
        // should overwrite the previous entry
        map.put("key", 2);
        assertEquals("{key=2}", map.toString(), "Case‑insensitive map must keep only the later value");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void reverseOrderingMap() {
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.ORDERING, CompactMap.REVERSE);
        CompactMap<String, Integer> map = newMapWithOptions(options);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals("{c=3, b=2, a=1}", map.toString(), "Reverse ordering must list entries opposite to insertion order");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nestedMapRepresentation() {
        CompactMap<String, Integer> inner = new CompactMap<>();
        inner.put("x", 10);
        inner.put("y", 20);
        CompactMap<String, Object> outer = new CompactMap<>();
        outer.put("inner", inner);
        String actual = outer.toString();
        // Build expected string using regular maps for deterministic format.
        Map<String, Integer> expectedInner = new HashMap<>();
        expectedInner.put("x", 10);
        expectedInner.put("y", 20);
        String expected = "{" + "inner=" + expectedInner.toString() + "}";
        // Since inner map order is undefined, compare entry sets.
        String innerPart = actual.substring(actual.indexOf("inner=") + 6, actual.length() - 1);
        String[] actualEntries = innerPart.substring(1, innerPart.length() - 1).split(",\\s*");
        String[] expectedEntries = expectedInner.toString().substring(1, expectedInner.toString().length() - 1).split(",\\s*");
        assertEquals(Set.of(expectedEntries), Set.of(actualEntries), "Nested map's inner representation must contain all inner entries");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void concurrentHashMapBacking() {
        Map<String, Object> options = new HashMap<>();
        options.put(CompactMap.MAP_TYPE, java.util.concurrent.ConcurrentHashMap.class);
        CompactMap<String, Integer> map = newMapWithOptions(options);
        map.put("a", 1);
        map.put("b", 2);
        String result = map.toString();
        // Verify that both entries appear, order does not matter.
        assertTrue(result.contains("a=1"), "Result must contain a=1");
        assertTrue(result.contains("b=2"), "Result must contain b=2");
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result must be enclosed in braces");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void selfReferenceKey() {
        CompactMap<String, Object> map = new CompactMap<>();
        // map used as a key
        map.put(map, "value");
        String result = assertDoesNotThrow(map::toString, "toString() should handle self‑reference as key without StackOverflow");
        // The key part will be the map's own toString, which starts with '{' – we just verify the value suffix.
        assertTrue(result.endsWith("=value}"), "Result should end with '=value}' indicating the self‑reference key was rendered");
    }
}
