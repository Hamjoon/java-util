package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_mapOf_Test {

    static Stream<Arguments> duplicateKeyProvider() {
        return Stream.of(Arguments.of(new Object[] { "x", 10, "x", 20 }, "x", 20), Arguments.of(new Object[] { 1, "a", 1, "b", 1, "c" }, 1, "c"));
    }

    static Stream<Arguments> typicalEntriesProvider() {
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        return Stream.of(Arguments.of(new Object[] { "a", 1, "b", 2 }, expected));
    }

    @Test
    void testMapOfWithNullArgumentReturnsEmptyUnmodifiableMap() {
        Map<?, ?> result = MapUtilities.mapOf((Object[]) null);
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "Map should be empty");
        assertThrows(UnsupportedOperationException.class, () -> result.put("a", "b"));
    }

    @Test
    void testMapOfWithNoArgumentsReturnsEmptyUnmodifiableMap() {
        Map<?, ?> result = MapUtilities.mapOf();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.put("x", "y"));
    }

    @Test
    void testMapOfCreatesCorrectUnmodifiableMap() {
        Map<String, Integer> result = MapUtilities.mapOf("one", 1, "two", 2, "three", 3);
        // Verify contents
        assertEquals(3, result.size());
        assertEquals(1, result.get("one"));
        assertEquals(2, result.get("two"));
        assertEquals(3, result.get("three"));
        // Verify insertion order is preserved (LinkedHashMap semantics)
        Map<String, Integer> expectedOrder = new LinkedHashMap<>();
        expectedOrder.put("one", 1);
        expectedOrder.put("two", 2);
        expectedOrder.put("three", 3);
        assertEquals(expectedOrder, result);
        // Verify immutability
        assertThrows(UnsupportedOperationException.class, () -> result.put("four", 4));
        assertThrows(UnsupportedOperationException.class, () -> result.remove("one"));
    }

    @Test
    void testMapOfWithOddNumberOfArgumentsThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapUtilities.mapOf("onlyKey"));
        assertTrue(ex.getMessage().contains("Invalid number of arguments"));
    }

    @Test
    void testMapOfExceedsMaximumEntriesThrowsException() {
        // Build an array with 11 entries (22 objects) which exceeds MAX_ENTRIES (10)
        Object[] manyPairs = new Object[22];
        for (int i = 0; i < 22; i++) {
            manyPairs[i] = i % 2 == 0 ? "key" + (i / 2) : "value" + (i / 2);
        }
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapUtilities.mapOf(manyPairs));
        assertTrue(ex.getMessage().contains("Too many entries"));
    }

    @Test
    void testMapOfAllowsNullKeysAndValues() {
        Map<String, String> result = MapUtilities.mapOf(null, "nullKey", "nullValue", null, "bothNull", null);
        assertEquals(3, result.size());
        assertEquals("nullKey", result.get(null));
        assertNull(result.get("nullValue"));
        assertNull(result.get("bothNull"));
    }

    /**
     * Group: mapOf - duplicate keys
     * Verifies that later occurrences of the same key overwrite earlier ones.
     */
    @ParameterizedTest
    @MethodSource("duplicateKeyProvider")
    void testMapOfDuplicateKeys(Object[] keyValues, String duplicateKey, Integer expectedValue) {
        @SuppressWarnings("unchecked")
        Map<String, Integer> result = MapUtilities.mapOf(keyValues);
        assertEquals(1, result.size(), "Map should contain a single entry after duplicate key handling");
        assertEquals(expectedValue, result.get(duplicateKey), "The last value supplied for the duplicate key should win");
        // Ensure immutability
        assertThrows(UnsupportedOperationException.class, () -> result.put("newKey", 99));
    }

    /**
     * Group: mapOf - max entries allowed
     * Exactly ten key/value pairs are accepted and the resulting map is immutable
     * and preserves insertion order.
     */
    @Test
    void testMapOfMaximumAllowedEntries() {
        Object[] tenPairs = new Object[] { "k0", 0, "k1", 1, "k2", 2, "k3", 3, "k4", 4, "k5", 5, "k6", 6, "k7", 7, "k8", 8, "k9", 9 };
        @SuppressWarnings("unchecked")
        Map<String, Integer> result = MapUtilities.mapOf(tenPairs);
        assertEquals(10, result.size(), "Map should contain exactly 10 entries");
        // Verify each entry is present and correct
        for (int i = 0; i < 10; i++) {
            assertEquals(i, result.get("k" + i));
        }
        // Verify insertion order (LinkedHashMap semantics)
        List<String> expectedOrder = new ArrayList<>();
        for (int i = 0; i < 10; i++) expectedOrder.add("k" + i);
        List<String> actualOrder = new ArrayList<>(result.keySet());
        assertEquals(expectedOrder, actualOrder, "Insertion order must be preserved");
        // Verify immutability
        assertThrows(UnsupportedOperationException.class, () -> result.put("extra", 10));
    }

    /**
     * Group: mapOf - insertion order preserved
     * Confirms that iterating over the map yields keys in the order they were supplied.
     */
    @Test
    void testMapOfInsertionOrderPreserved() {
        Object[] keyValues = new Object[] { "first", 1, "second", 2, "third", 3 };
        @SuppressWarnings("unchecked")
        Map<String, Integer> result = MapUtilities.mapOf(keyValues);
        Iterator<String> iterator = result.keySet().iterator();
        assertEquals("first", iterator.next());
        assertEquals("second", iterator.next());
        assertEquals("third", iterator.next());
        assertFalse(iterator.hasNext(), "No more keys should be present");
    }

    /**
     * Group: mapOf - typical entries (parameterised for readability)
     * Covers the normal case of an even number of arguments within the limit.
     */
    @ParameterizedTest
    @MethodSource("typicalEntriesProvider")
    void testMapOfTypicalEntries(Object[] keyValues, Map<?, ?> expected) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> result = MapUtilities.mapOf(keyValues);
        assertEquals(expected, result);
        // Verify immutability
        assertThrows(UnsupportedOperationException.class, () -> result.put("new", "value"));
    }

    /**
     * Group: mapOf - unmodifiable map behavior (already covered, but kept for completeness)
     */
    @Test
    void testMapOfReturnedMapIsUnmodifiable() {
        Map<String, Integer> result = MapUtilities.mapOf("a", 1);
        assertThrows(UnsupportedOperationException.class, () -> result.put("b", 2));
        assertThrows(UnsupportedOperationException.class, () -> result.remove("a"));
    }
}
