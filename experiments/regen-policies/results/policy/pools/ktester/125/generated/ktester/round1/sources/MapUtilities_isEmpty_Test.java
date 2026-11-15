package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.TreeMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DayOfWeek;
import java.util.Collections;
import java.util.EnumMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_isEmpty_Test {

    /**
     * Provides a stream of arguments: map instance, expected result, and a description.
     */
    private static Stream<Arguments> mapProvider() {
        // 1. null map
        Map<String, Object> nullMap = null;
        // 2. Empty HashMap
        Map<String, Object> emptyHashMap = new HashMap<>();
        // 3. Empty LinkedHashMap
        Map<String, Object> emptyLinkedHashMap = new LinkedHashMap<>();
        // 4. Empty TreeMap (SortedMap)
        Map<String, Object> emptyTreeMap = new TreeMap<>();
        // 5. HashMap with a single entry
        Map<String, Object> hashMapWithEntry = new HashMap<>();
        hashMapWithEntry.put("key", "value");
        // 6. Unmodifiable empty map
        Map<String, Object> unmodifiableEmpty = Collections.unmodifiableMap(new HashMap<>());
        // 7. Unmodifiable map with an entry
        Map<String, Object> tmp = new HashMap<>();
        tmp.put("k", 1);
        Map<String, Object> unmodifiableWithEntry = Collections.unmodifiableMap(tmp);
        // 8. Map containing null key and null value
        Map<String, Object> mapNullKeyValue = new HashMap<>();
        mapNullKeyValue.put(null, null);
        // 9. Map containing empty string key/value
        Map<String, Object> mapEmptyString = new HashMap<>();
        mapEmptyString.put("", "");
        // 10. Synchronized empty map
        Map<String, Object> synchronizedEmpty = Collections.synchronizedMap(new HashMap<>());
        // 11. Synchronized map with an entry
        Map<String, Object> syncTmp = new HashMap<>();
        syncTmp.put("a", 1);
        Map<String, Object> synchronizedWithEntry = Collections.synchronizedMap(syncTmp);
        // 12. Empty EnumMap
        Map<DayOfWeek, Object> emptyEnumMap = new EnumMap<>(DayOfWeek.class);
        // 13. EnumMap with one entry
        Map<DayOfWeek, Object> enumMapWithEntry = new EnumMap<>(DayOfWeek.class);
        enumMapWithEntry.put(DayOfWeek.MONDAY, "work");
        // 14. LinkedHashMap that receives an empty map via putAll
        Map<String, Object> linkedPutAllEmpty = new LinkedHashMap<>();
        linkedPutAllEmpty.putAll(Collections.emptyMap());
        return Stream.of(Arguments.of(nullMap, true, "null map should be considered empty"), Arguments.of(emptyHashMap, true, "empty HashMap should be considered empty"), Arguments.of(emptyLinkedHashMap, true, "empty LinkedHashMap should be considered empty"), Arguments.of(emptyTreeMap, true, "empty TreeMap should be considered empty"), Arguments.of(hashMapWithEntry, false, "HashMap with one entry should not be empty"), Arguments.of(unmodifiableEmpty, true, "unmodifiable empty map should be considered empty"), Arguments.of(unmodifiableWithEntry, false, "unmodifiable map with entry should not be empty"), Arguments.of(mapNullKeyValue, false, "map with null key/value should not be empty"), Arguments.of(mapEmptyString, false, "map with empty string key/value should not be empty"), Arguments.of(synchronizedEmpty, true, "synchronized empty map should be considered empty"), Arguments.of(synchronizedWithEntry, false, "synchronized map with entry should not be empty"), Arguments.of(emptyEnumMap, true, "empty EnumMap should be considered empty"), Arguments.of(enumMapWithEntry, false, "EnumMap with one entry should not be empty"), Arguments.of(linkedPutAllEmpty, true, "LinkedHashMap after putAll(empty) should remain empty"));
    }

    @Test
    @DisplayName("isEmpty should return true for a null map")
    void testIsEmpty_NullMap() {
        assertTrue(MapUtilities.isEmpty(null), "null map must be considered empty");
    }

    @Test
    @DisplayName("isEmpty should return true for an empty map (HashMap)")
    void testIsEmpty_EmptyHashMap() {
        Map<String, Object> empty = new HashMap<>();
        assertTrue(MapUtilities.isEmpty(empty), "empty HashMap must be considered empty");
    }

    @Test
    @DisplayName("isEmpty should return true for an empty map (TreeMap)")
    void testIsEmpty_EmptyTreeMap() {
        Map<String, Object> empty = new TreeMap<>();
        assertTrue(MapUtilities.isEmpty(empty), "empty TreeMap must be considered empty");
    }

    @Test
    @DisplayName("isEmpty should return true for an empty map (LinkedHashMap)")
    void testIsEmpty_EmptyLinkedHashMap() {
        Map<String, Object> empty = new LinkedHashMap<>();
        assertTrue(MapUtilities.isEmpty(empty), "empty LinkedHashMap must be considered empty");
    }

    @Test
    @DisplayName("isEmpty should return false for a non‑empty map")
    void testIsEmpty_NonEmptyMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        assertFalse(MapUtilities.isEmpty(map), "map containing entries must not be considered empty");
    }

    @Test
    @DisplayName("isEmpty should correctly handle maps that become empty after clear()")
    void testIsEmpty_AfterClear() {
        Map<String, Integer> map = new TreeMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertFalse(MapUtilities.isEmpty(map), "map should not be empty before clear");
        map.clear();
        assertTrue(MapUtilities.isEmpty(map), "map should be empty after clear");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("mapProvider")
    @DisplayName("isEmpty should correctly evaluate various map implementations and wrappers")
    void testIsEmpty_VariousMaps(Map<?, ?> map, boolean expected, String description) {
        assertEquals(expected, MapUtilities.isEmpty(map), description);
    }
}
