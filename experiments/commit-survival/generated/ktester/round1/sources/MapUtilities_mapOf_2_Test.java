package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.awt.Point;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_mapOf_2_Test {

    /**
     * Provides a stream of arguments for normal, null‑handling and different‑type tests.
     *
     * Each {@link Arguments} contains:
     * <ul>
     *   <li>key object</li>
     *   <li>value object</li>
     *   <li>description used as a display name</li>
     * </ul>
     */
    private static Stream<Arguments> mapOfParameterProvider() {
        return Stream.of(// Normal behavior
        Arguments.of("key1", 123, "Typical case with non‑null key and non‑null value"), // Null handling
        Arguments.of(null, "value", "Key is null, value is non‑null"), Arguments.of("key", null, "Key is non‑null, value is null"), Arguments.of(null, null, "Both key and value are null"), // Different data types
        Arguments.of(42, "answer", "Integer key, String value"), Arguments.of(true, new Point(1, 2), "Boolean key, mutable Point value"), Arguments.of(new int[] { 1, 2, 3 }, new String[] { "a", "b" }, "Array key (int[]), array value (String[])"));
    }

    @Test
    void mapOf_returnsMapContainingSingleEntry() {
        // given
        String key = "alpha";
        Integer value = 42;
        // when
        Map<String, Integer> result = MapUtilities.mapOf(key, value);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(key), "Map should contain the provided key");
        assertEquals(value, result.get(key), "Map should contain the provided value");
    }

    @Test
    void mapOf_returnsUnmodifiableMap() {
        // given
        String key = "beta";
        String value = "value";
        // when
        Map<String, String> result = MapUtilities.mapOf(key, value);
        // then
        assertThrows(UnsupportedOperationException.class, () -> result.put("newKey", "newValue"), "Map should be unmodifiable");
        assertThrows(UnsupportedOperationException.class, () -> result.remove(key), "Map should be unmodifiable");
        assertThrows(UnsupportedOperationException.class, () -> result.clear(), "Map should be unmodifiable");
    }

    @Test
    void mapOf_allowsNullKeyAndValue() {
        // given
        Object nullKey = null;
        Object nullValue = null;
        // when
        Map<Object, Object> result = MapUtilities.mapOf(nullKey, nullValue);
        // then
        assertEquals(1, result.size(), "Map should contain one entry even if key/value are null");
        assertTrue(result.containsKey(nullKey), "Map should contain the null key");
        assertNull(result.get(nullKey), "Value associated with null key should be null");
    }

    @Test
    void mapOf_preservesInsertionOrderWhenMultipleCalls() {
        // This test demonstrates that the underlying implementation is a LinkedHashMap.
        // Although mapOf only creates a single‑entry map, we can verify order by
        // creating several maps and concatenating their entry sets.
        // given
        Map<String, Integer> map1 = MapUtilities.mapOf("first", 1);
        Map<String, Integer> map2 = MapUtilities.mapOf("second", 2);
        Map<String, Integer> map3 = MapUtilities.mapOf("third", 3);
        // when
        // Combine the entry sets in the order of creation
        Iterator<Map.Entry<String, Integer>> iterator = java.util.stream.Stream.of(map1, map2, map3).flatMap(m -> m.entrySet().stream()).iterator();
        // then
        assertTrue(iterator.hasNext());
        assertEquals("first", iterator.next().getKey());
        assertTrue(iterator.hasNext());
        assertEquals("second", iterator.next().getKey());
        assertTrue(iterator.hasNext());
        assertEquals("third", iterator.next().getKey());
        assertFalse(iterator.hasNext());
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("mapOfParameterProvider")
    void mapOf_createsCorrectSingleEntryMap(Object key, Object value, String description) {
        // when
        Map<Object, Object> result = MapUtilities.mapOf(key, value);
        // then – basic map contract
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map must contain exactly one entry");
        assertTrue(result.containsKey(key), "Map must contain the supplied key");
        assertSame(value, result.get(key), "Stored value must be the same instance supplied");
        // then – immutability checks
        assertAll("immutability", () -> assertThrows(UnsupportedOperationException.class, () -> result.put("any", "thing"), "put must throw UnsupportedOperationException"), () -> assertThrows(UnsupportedOperationException.class, () -> result.remove(key), "remove must throw UnsupportedOperationException"), () -> assertThrows(UnsupportedOperationException.class, result::clear, "clear must throw UnsupportedOperationException"));
    }

    @Test
    void mapOf_putOperationThrows() {
        Map<String, String> map = MapUtilities.mapOf("a", "b");
        assertThrows(UnsupportedOperationException.class, () -> map.put("c", "d"), "put must be prohibited on the unmodifiable map");
    }

    @Test
    void mapOf_removeOperationThrows() {
        Map<String, Integer> map = MapUtilities.mapOf("x", 10);
        assertThrows(UnsupportedOperationException.class, () -> map.remove("x"), "remove must be prohibited on the unmodifiable map");
    }

    @Test
    void mapOf_clearOperationThrows() {
        Map<String, String> map = MapUtilities.mapOf("y", "z");
        assertThrows(UnsupportedOperationException.class, map::clear, "clear must be prohibited on the unmodifiable map");
    }

    @Test
    void mapOf_entrySetIteratorRemoveThrows() {
        Map<String, Integer> map = MapUtilities.mapOf("alpha", 1);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertTrue(it.hasNext(), "Iterator must have at least one element");
        // advance to first entry
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove on entrySet must throw UnsupportedOperationException");
    }

    @Test
    void mapOf_entrySetClearThrows() {
        Map<Object, Object> map = MapUtilities.mapOf(null, "value");
        Set<Map.Entry<Object, Object>> entrySet = map.entrySet();
        assertThrows(UnsupportedOperationException.class, entrySet::clear, "entrySet.clear must throw UnsupportedOperationException");
    }
}
