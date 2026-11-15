package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapUtilities_mapOf_Test {

    @Test
    @DisplayName("mapOf with null argument returns empty unmodifiable map")
    void testMapOfNullArgument() {
        Map<?, ?> result = MapUtilities.mapOf((Object[]) null);
        assertNotNull(result, "Result should never be null");
        assertTrue(result.isEmpty(), "Map should be empty");
        assertThrows(UnsupportedOperationException.class, () -> result.put("k", "v"));
    }

    @Test
    @DisplayName("mapOf with no arguments returns empty unmodifiable map")
    void testMapOfEmptyArgument() {
        Map<?, ?> result = MapUtilities.mapOf();
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.put("k", "v"));
    }

    @Test
    @DisplayName("mapOf creates map with correct entries and preserves order")
    void testMapOfNormalCase() {
        Map<String, Integer> result = MapUtilities.mapOf("first", 1, "second", 2, "third", 3, null, 99, "nullValue", null);
        // Verify size and content
        assertEquals(5, result.size());
        assertEquals(1, result.get("first"));
        assertEquals(2, result.get("second"));
        assertEquals(3, result.get("third"));
        assertEquals(99, result.get(null));
        assertNull(result.get("nullValue"));
        // Verify iteration order (LinkedHashMap semantics)
        Iterator<String> it = result.keySet().iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
        // null key
        assertNull(it.next());
        assertEquals("nullValue", it.next());
        // Verify map is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> result.put("newKey", 123));
    }

    @Test
    @DisplayName("mapOf throws IllegalArgumentException when an odd number of arguments is supplied")
    void testMapOfOddNumberOfArguments() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapUtilities.mapOf("onlyKey"));
        assertTrue(ex.getMessage().contains("Invalid number of arguments"));
    }

    @Test
    @DisplayName("mapOf throws IllegalArgumentException when more than MAX_ENTRIES are supplied")
    void testMapOfTooManyEntries() {
        // Build 11 key/value pairs (22 arguments)
        Object[] many = new Object[22];
        for (int i = 0; i < 22; i++) {
            // simple distinct objects for keys and values
            many[i] = i;
        }
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapUtilities.mapOf(many));
        assertTrue(ex.getMessage().contains("Too many entries"));
    }

    @Test
    @DisplayName("mapOf - single entry returns immutable map of size 1")
    void testMapOfSingleEntry() {
        Map<String, Integer> result = MapUtilities.mapOf("oneKey", 123);
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals(123, result.get("oneKey"));
        // verify immutability
        assertAll("unmodifiable checks", () -> assertThrows(UnsupportedOperationException.class, () -> result.put("another", 456)), () -> assertThrows(UnsupportedOperationException.class, () -> result.remove("oneKey")), () -> assertThrows(UnsupportedOperationException.class, result::clear));
    }

    @Test
    @DisplayName("mapOf - max entries (10) succeed and preserve insertion order")
    void testMapOfMaxEntries() {
        // 10 key/value pairs => 20 var‑args
        Object[] args = new Object[] { "k1", "v1", "k2", "v2", "k3", "v3", "k4", "v4", "k5", "v5", "k6", "v6", "k7", "v7", "k8", "v8", "k9", "v9", "k10", "v10" };
        Map<String, String> result = MapUtilities.mapOf(args);
        assertEquals(10, result.size(), "Map should contain exactly 10 entries");
        // verify order
        Iterator<String> it = result.keySet().iterator();
        for (int i = 1; i <= 10; i++) {
            assertEquals("k" + i, it.next(), "Key order should follow insertion order");
        }
        // verify immutability
        assertThrows(UnsupportedOperationException.class, () -> result.put("extra", "x"));
    }

    @Test
    @DisplayName("mapOf - duplicate keys keep last value, order unchanged")
    void testMapOfDuplicateKeys() {
        Map<String, Integer> result = MapUtilities.mapOf("dup", 1, "unique", 2, "dup", 99);
        assertEquals(2, result.size(), "Duplicate key should be overwritten, leaving 2 entries");
        assertEquals(99, result.get("dup"), "Last value for duplicate key should win");
        assertEquals(2, result.get("unique"));
        // order: first occurrence of "dup" stays at its original position
        Iterator<String> it = result.keySet().iterator();
        assertEquals("dup", it.next());
        assertEquals("unique", it.next());
        // immutability check
        assertThrows(UnsupportedOperationException.class, () -> result.put("new", 0));
    }

    @Test
    @DisplayName("mapOf - null keys and null values are stored correctly")
    void testMapOfNullKeysAndValues() {
        Map<Object, Object> result = MapUtilities.mapOf(null, "valueForNullKey", "nullValueKey", null);
        assertEquals(2, result.size(), "Map should contain two entries");
        assertEquals("valueForNullKey", result.get(null));
        assertNull(result.get("nullValueKey"));
        // order: null key first, then "nullValueKey"
        Iterator<Object> it = result.keySet().iterator();
        assertNull(it.next());
        assertEquals("nullValueKey", it.next());
        // unmodifiable verification
        assertThrows(UnsupportedOperationException.class, () -> result.put("x", "y"));
    }

    @Test
    @DisplayName("mapOf - mixed type keys and values are accepted")
    void testMapOfMixedTypes() {
        Map<Object, Object> result = MapUtilities.mapOf(42, true, "stringKey", 3.14, false, "anotherValue");
        assertEquals(3, result.size(), "Map should contain three entries");
        assertEquals(true, result.get(42));
        assertEquals(3.14, result.get("stringKey"));
        assertEquals("anotherValue", result.get(false));
        // verify order matches insertion order
        Iterator<Object> it = result.keySet().iterator();
        assertEquals(42, it.next());
        assertEquals("stringKey", it.next());
        assertEquals(false, it.next());
        // immutability
        assertThrows(UnsupportedOperationException.class, () -> result.put("newKey", "newValue"));
    }

    @Test
    @DisplayName("mapOf - unmodifiable behavior for all mutating operations")
    void testMapOfUnmodifiableBehavior() {
        Map<String, Integer> result = MapUtilities.mapOf("a", 1, "b", 2);
        assertAll("all mutating ops should throw", () -> assertThrows(UnsupportedOperationException.class, () -> result.put("c", 3)), () -> assertThrows(UnsupportedOperationException.class, () -> result.remove("a")), () -> assertThrows(UnsupportedOperationException.class, () -> result.putAll(Map.of("x", 9))), () -> assertThrows(UnsupportedOperationException.class, result::clear));
    }
}
