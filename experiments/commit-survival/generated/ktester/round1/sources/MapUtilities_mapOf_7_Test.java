package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_mapOf_7_Test {

    // Sample keys and values used across the tests
    private static final String K1 = "key1";

    private static final String K2 = "key2";

    private static final String K3 = "key3";

    private static final String K4 = "key4";

    private static final String K5 = "key5";

    private static final String K6 = "key6";

    private static final Integer V1 = 1;

    private static final Integer V2 = 2;

    private static final Integer V3 = 3;

    private static final Integer V4 = 4;

    private static final Integer V5 = 5;

    private static final Integer V6 = 6;

    private static Stream<Arguments> basicScenariosProvider() {
        return Stream.of(Arguments.of("All distinct simple entries", "a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6)), Arguments.of("Duplicate key 'dup' with last value winning", "dup", 1, "dup", 2, "dup", 3, "dup", 4, "dup", 5, "dup", 6, Map.of("dup", 6)));
    }

    @Test
    void mapOfCreatesCorrectUnmodifiableMapWithSixEntries() {
        // GIVEN – the six key/value pairs are defined as static constants
        // WHEN – we invoke the method under test
        Map<String, Integer> result = MapUtilities.mapOf(K1, V1, K2, V2, K3, V3, K4, V4, K5, V5, K6, V6);
        // THEN – the map contains exactly the supplied entries
        assertEquals(6, result.size(), "Map should contain six entries");
        assertEquals(V1, result.get(K1));
        assertEquals(V2, result.get(K2));
        assertEquals(V3, result.get(K3));
        assertEquals(V4, result.get(K4));
        assertEquals(V5, result.get(K5));
        assertEquals(V6, result.get(K6));
        // AND – the iteration order matches insertion order (LinkedHashMap semantics)
        List<String> expectedOrder = List.of(K1, K2, K3, K4, K5, K6);
        List<String> actualOrder = new ArrayList<>(result.keySet());
        assertEquals(expectedOrder, actualOrder, "Map should preserve insertion order");
        // AND – the map is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> result.put("newKey", 999), "Map should be unmodifiable");
    }

    @Test
    void mapOfReturnsUnmodifiableMapEvenWhenValuesAreNull() {
        // GIVEN – a mixture of null values
        Map<String, Integer> result = MapUtilities.mapOf(K1, null, K2, V2, K3, null, K4, V4, K5, null, K6, V6);
        // THEN – null values are stored correctly
        assertNull(result.get(K1));
        assertEquals(V2, result.get(K2));
        assertNull(result.get(K3));
        assertEquals(V4, result.get(K4));
        assertNull(result.get(K5));
        assertEquals(V6, result.get(K6));
        // AND – map remains unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> result.remove(K2), "Map should be unmodifiable even when it contains null values");
    }

    /* --------------------------------------------------------------
     *  Duplicate key handling – last put wins, order is based on first
     *  insertion of each distinct key.
     * -------------------------------------------------------------- */
    @Test
    void duplicateKeysAllSameResultsInSingleEntryWithLastValue() {
        Map<String, Integer> result = MapUtilities.mapOf("dup", 1, "dup", 2, "dup", 3, "dup", 4, "dup", 5, "dup", 6);
        assertEquals(1, result.size(), "Only one distinct key should remain");
        assertEquals(6, result.get("dup"), "The last supplied value should be retained");
        assertThrows(UnsupportedOperationException.class, () -> result.put("dup", 7));
    }

    @Test
    void duplicateKeysInterleavedWithDistinctKeys() {
        Map<String, Integer> result = MapUtilities.mapOf("x", 1, "y", 2, "x", 3, "z", 4, "y", 5, "x", 6);
        // Expected final map: {x=6, y=5, z=4}
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("x", 6);
        expected.put("y", 5);
        expected.put("z", 4);
        assertEquals(expected, result, "Map should contain the last value for each duplicate key");
        // Verify iteration order – LinkedHashMap keeps order of first insertion
        List<String> expectedOrder = List.of("x", "y", "z");
        assertEquals(expectedOrder, new ArrayList<>(result.keySet()));
    }

    /* --------------------------------------------------------------
     *  Null key overwrite order
     * -------------------------------------------------------------- */
    @Test
    void nullKeyOverwritePreservesInsertionPositionAndKeepsLastValue() {
        Map<String, String> result = MapUtilities.mapOf(null, "first", "second", "val2", null, "third", "fourth", "val4", "fifth", "val5", null, "sixth");
        // Distinct keys: null, "second", "fourth", "fifth"
        assertEquals(4, result.size(), "Map should have four distinct keys");
        assertEquals("sixth", result.get(null), "Last value for null key should win");
        assertEquals("val2", result.get("second"));
        assertEquals("val4", result.get("fourth"));
        assertEquals("val5", result.get("fifth"));
        // Order: null, second, fourth, fifth (position of null stays at first insertion)
        List<String> expectedOrder = List.of(null, "second", "fourth", "fifth");
        assertEquals(expectedOrder, new ArrayList<>(result.keySet()));
    }

    /* --------------------------------------------------------------
     *  Mixed type keys and values
     * -------------------------------------------------------------- */
    @Test
    void mixedTypeKeysAndValuesAreHandledCorrectly() {
        Integer key1 = 1;
        String val1 = "one";
        String key2 = "two";
        Long val2 = 2L;
        BigDecimal key3 = new BigDecimal("3.0");
        Date val3 = new Date(0);
        Boolean key4 = true;
        Boolean val4 = false;
        List<String> key5 = new ArrayList<>();
        List<String> val5 = Collections.singletonList("list");
        Object key6 = new Object();
        Object val6 = null;
        Map<Object, Object> result = MapUtilities.mapOf(key1, val1, key2, val2, key3, val3, key4, val4, key5, val5, key6, val6);
        assertEquals(6, result.size());
        assertEquals(val1, result.get(key1));
        assertEquals(val2, result.get(key2));
        assertEquals(val3, result.get(key3));
        assertEquals(val4, result.get(key4));
        assertEquals(val5, result.get(key5));
        assertNull(result.get(key6));
        // Verify order of insertion
        List<Object> expectedOrder = List.of(key1, key2, key3, key4, key5, key6);
        assertEquals(expectedOrder, new ArrayList<>(result.keySet()));
    }

    /* --------------------------------------------------------------
     *  Array values handling
     * -------------------------------------------------------------- */
    @Test
    void mapOfWorksWithArrayValuesPreservingOrder() {
        int[] a1 = { 1, 2 };
        int[] a2 = { 3, 4 };
        int[] a3 = { 5, 6 };
        int[] a4 = { 7, 8 };
        int[] a5 = { 9, 10 };
        int[] a6 = { 11, 12 };
        Map<String, int[]> result = MapUtilities.mapOf("a", a1, "b", a2, "c", a3, "d", a4, "e", a5, "f", a6);
        assertEquals(6, result.size());
        assertArrayEquals(a1, result.get("a"));
        assertArrayEquals(a2, result.get("b"));
        assertArrayEquals(a3, result.get("c"));
        assertArrayEquals(a4, result.get("d"));
        assertArrayEquals(a5, result.get("e"));
        assertArrayEquals(a6, result.get("f"));
        List<String> expectedOrder = List.of("a", "b", "c", "d", "e", "f");
        assertEquals(expectedOrder, new ArrayList<>(result.keySet()));
    }

    /* --------------------------------------------------------------
     *  Immutability – all mutating operations should throw
     * -------------------------------------------------------------- */
    @Test
    void allMutatingOperationsThrowUnsupportedOperationException() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
        // clear()
        assertThrows(UnsupportedOperationException.class, map::clear);
        // put()
        assertThrows(UnsupportedOperationException.class, () -> map.put("g", 7));
        // remove()
        assertThrows(UnsupportedOperationException.class, () -> map.remove("a"));
        // putAll()
        assertThrows(UnsupportedOperationException.class, () -> map.putAll(Map.of("h", 8)));
        // replace()
        assertThrows(UnsupportedOperationException.class, () -> map.replace("a", 10));
        // compute()
        assertThrows(UnsupportedOperationException.class, () -> map.compute("a", (k, v) -> 99));
    }

    /* --------------------------------------------------------------
     *  Empty string and zero/negative zero values
     * -------------------------------------------------------------- */
    @Test
    void emptyAndWhitespaceStringKeysWithZeroValuesAreHandledCorrectly() {
        Map<String, Object> result = MapUtilities.mapOf("", 0, " ", -0, "\n", 0L, "\t", 0.0, "null", false, "false", true);
        assertEquals(6, result.size());
        assertEquals(0, result.get(""));
        assertEquals(-0, result.get(" "));
        assertEquals(0L, result.get("\n"));
        assertEquals(0.0, result.get("\t"));
        assertEquals(false, result.get("null"));
        assertEquals(true, result.get("false"));
        List<String> expectedOrder = List.of("", " ", "\n", "\t", "null", "false");
        assertEquals(expectedOrder, new ArrayList<>(result.keySet()));
    }

    /* --------------------------------------------------------------
     *  Parameterised test for basic scenarios (optional demonstration)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicScenariosProvider")
    void basicScenarios(String description, String k1, Object v1, String k2, Object v2, String k3, Object v3, String k4, Object v4, String k5, Object v5, String k6, Object v6, Map<String, Object> expected) {
        Map<String, Object> result = MapUtilities.mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);
        assertEquals(expected, result);
        assertThrows(UnsupportedOperationException.class, () -> result.put("extra", 0));
    }
}
