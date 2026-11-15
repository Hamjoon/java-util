package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_mapOf_3_Test {

    static Stream<Arguments> duplicateKeyProvider() {
        return Stream.of(Arguments.of("a", 1, "a", 2), Arguments.of(null, "first", null, "second"));
    }

    @Test
    @DisplayName("mapOf(K,V,K,V) creates an unmodifiable LinkedHashMap with correct order and entries")
    void testMapOfCreatesCorrectMap() {
        // given
        String key1 = "first";
        Integer value1 = 1;
        String key2 = "second";
        Integer value2 = 2;
        // when
        Map<String, Integer> result = MapUtilities.mapOf(key1, value1, key2, value2);
        // then
        // map must contain both entries
        assertEquals(2, result.size(), "Map should contain exactly two entries");
        assertEquals(value1, result.get(key1), "First key should map to its value");
        assertEquals(value2, result.get(key2), "Second key should map to its value");
        // order must be insertion order (LinkedHashMap semantics)
        Iterator<Map.Entry<String, Integer>> iterator = result.entrySet().iterator();
        assertTrue(iterator.hasNext(), "Map should have a first entry");
        Map.Entry<String, Integer> first = iterator.next();
        assertEquals(key1, first.getKey(), "First entry key should be the first supplied key");
        assertEquals(value1, first.getValue(), "First entry value should be the first supplied value");
        assertTrue(iterator.hasNext(), "Map should have a second entry");
        Map.Entry<String, Integer> second = iterator.next();
        assertEquals(key2, second.getKey(), "Second entry key should be the second supplied key");
        assertEquals(value2, second.getValue(), "Second entry value should be the second supplied value");
        assertFalse(iterator.hasNext(), "Map should contain only two entries");
        // map must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> result.put("third", 3), "Attempting to modify the map should throw UnsupportedOperationException");
        assertThrows(UnsupportedOperationException.class, () -> result.remove(key1), "Attempting to remove from the map should throw UnsupportedOperationException");
        assertThrows(UnsupportedOperationException.class, () -> result.clear(), "Attempting to clear the map should throw UnsupportedOperationException");
        // also test that the unmodifiable view returned by Collections.unmodifiableMap behaves as expected
        assertTrue(Collections.unmodifiableMap(result) == result, "The returned map should already be an unmodifiable view");
    }

    @Test
    @DisplayName("mapOf(K,V,K,V) handles null keys and values")
    void testMapOfHandlesNulls() {
        // given
        String key1 = null;
        String value1 = null;
        String key2 = "notNullKey";
        String value2 = null;
        // when
        Map<String, String> result = MapUtilities.mapOf(key1, value1, key2, value2);
        // then
        assertEquals(2, result.size(), "Map should contain two entries even when keys/values are null");
        assertTrue(result.containsKey(null), "Map should contain the null key");
        assertNull(result.get(null), "Null key should map to null value");
        assertEquals(null, result.get(key2), "Non‑null key should map to its null value");
    }

    // ---------- Duplicate key handling ----------
    @Test
    @DisplayName("Duplicate keys: second entry overwrites the first")
    void testDuplicateKeyOverwrites() {
        // given duplicate key "a"
        String k1 = "a";
        Integer v1 = 1;
        String k2 = "a";
        Integer v2 = 2;
        // when
        Map<String, Integer> result = MapUtilities.mapOf(k1, v1, k2, v2);
        // then
        assertEquals(1, result.size(), "Map should contain only one entry after duplicate key");
        assertEquals(v2, result.get("a"), "Value should be the one from the second put");
        // verify immutability
        assertThrows(UnsupportedOperationException.class, () -> result.put("b", 3));
    }

    // ---------- Null handling (additional cases) ----------
    @Test
    @DisplayName("Null key with non‑null value and subsequent non‑null key")
    void testNullKeyWithValue() {
        Map<String, String> result = MapUtilities.mapOf(null, "first", "key", "second");
        assertEquals(2, result.size());
        assertTrue(result.containsKey(null));
        assertEquals("first", result.get(null));
        assertEquals("second", result.get("key"));
    }

    @Test
    @DisplayName("Null values are stored and retrievable")
    void testNullValuesStored() {
        Map<String, String> result = MapUtilities.mapOf("x", null, "y", null);
        assertEquals(2, result.size());
        assertNull(result.get("x"));
        assertNull(result.get("y"));
    }

    // ---------- Duplicate null key ----------
    @Test
    @DisplayName("Duplicate null keys: second entry overwrites the first")
    void testDuplicateNullKey() {
        Map<String, String> result = MapUtilities.mapOf(null, "first", null, "second");
        assertEquals(1, result.size(), "Only one entry should remain for duplicate null keys");
        assertTrue(result.containsKey(null));
        assertEquals("second", result.get(null));
    }

    // ---------- Different type handling ----------
    @Test
    @DisplayName("mapOf works with heterogeneous key/value types")
    void testDifferentTypes() {
        Map<Integer, Object> result = MapUtilities.mapOf(100, "hundred", 200, 2.0);
        assertEquals(2, result.size());
        assertEquals("hundred", result.get(100));
        assertEquals(2.0, result.get(200));
        // verify insertion order
        Iterator<Integer> it = result.keySet().iterator();
        assertEquals(100, it.next());
        assertEquals(200, it.next());
        // immutability
        assertThrows(UnsupportedOperationException.class, () -> result.put(300, "extra"));
    }

    // ---------- Mutable value handling ----------
    @Test
    @DisplayName("Map is unmodifiable but mutable collection values can be changed")
    void testMutableValues() {
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2));
        Set<String> set = new HashSet<>(Collections.singleton("a"));
        Map<String, Object> result = MapUtilities.mapOf("list", list, "set", set);
        assertEquals(2, result.size());
        // map itself cannot be modified
        assertThrows(UnsupportedOperationException.class, () -> result.put("new", "value"));
        // but the mutable objects stored as values can be mutated
        @SuppressWarnings("unchecked")
        List<Integer> retrievedList = (List<Integer>) result.get("list");
        retrievedList.add(3);
        assertEquals(Arrays.asList(1, 2, 3), retrievedList);
        @SuppressWarnings("unchecked")
        Set<String> retrievedSet = (Set<String>) result.get("set");
        retrievedSet.add("b");
        assertTrue(retrievedSet.containsAll(Arrays.asList("a", "b")));
    }

    // ---------- Advanced immutability operations ----------
    @Test
    @DisplayName("All mutating default Map methods throw UnsupportedOperationException")
    void testAllMutatingOperations() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1, "b", 2);
        // clear()
        assertThrows(UnsupportedOperationException.class, map::clear);
        // putAll()
        assertThrows(UnsupportedOperationException.class, () -> map.putAll(Collections.singletonMap("c", 3)));
        // compute()
        assertThrows(UnsupportedOperationException.class, () -> map.compute("a", (k, v) -> v + 10));
        // computeIfAbsent()
        assertThrows(UnsupportedOperationException.class, () -> map.computeIfAbsent("c", k -> 5));
        // computeIfPresent()
        assertThrows(UnsupportedOperationException.class, () -> map.computeIfPresent("a", (k, v) -> v + 5));
        // merge()
        assertThrows(UnsupportedOperationException.class, () -> map.merge("b", 4, Integer::sum));
        // replaceAll()
        assertThrows(UnsupportedOperationException.class, () -> map.replaceAll((k, v) -> v * 2));
        // replace(K,V)
        assertThrows(UnsupportedOperationException.class, () -> map.replace("a", 10));
        // replace(K,V, V)
        assertThrows(UnsupportedOperationException.class, () -> map.replace("a", 1, 10));
        // putIfAbsent()
        assertThrows(UnsupportedOperationException.class, () -> map.putIfAbsent("c", 3));
    }

    // ---------- Self‑referential / nested map values ----------
    @Test
    @DisplayName("Nested map values retain immutability when outer map is unmodifiable")
    void testNestedUnmodifiableMap() {
        // inner map created via mapOf -> already unmodifiable
        Map<String, Integer> inner = MapUtilities.mapOf("a", 1, "b", 2);
        Map<String, Object> outer = MapUtilities.mapOf("nested", inner, "other", null);
        // outer map immutability
        assertThrows(UnsupportedOperationException.class, () -> outer.put("new", "value"));
        // inner map immutability
        @SuppressWarnings("unchecked")
        Map<String, Integer> retrievedInner = (Map<String, Integer>) outer.get("nested");
        assertThrows(UnsupportedOperationException.class, () -> retrievedInner.put("c", 3));
    }

    // ---------- Null key/value type mismatch ----------
    @Test
    @DisplayName("Null key with non‑null value and non‑null key with null value")
    void testNullKeyValueMismatch() {
        Map<String, Integer> result = MapUtilities.mapOf(null, 123, "nonNullKey", null);
        assertEquals(2, result.size());
        assertTrue(result.containsKey(null));
        assertEquals(123, result.get(null));
        assertTrue(result.containsKey("nonNullKey"));
        assertNull(result.get("nonNullKey"));
    }

    @Test
    @DisplayName("Second entry has both null key and null value")
    void testSecondEntryNullKeyAndValue() {
        Map<String, String> result = MapUtilities.mapOf("key", null, null, null);
        assertEquals(2, result.size());
        assertTrue(result.containsKey("key"));
        assertNull(result.get("key"));
        assertTrue(result.containsKey(null));
        assertNull(result.get(null));
        // verify order: "key" first, then null
        Iterator<String> it = result.keySet().iterator();
        assertEquals("key", it.next());
        assertNull(it.next());
    }

    // ---------- Parameterized test for duplicate key scenarios ----------
    @ParameterizedTest(name = "{index} => k1={0}, v1={1}, k2={2}, v2={3}")
    @MethodSource("duplicateKeyProvider")
    @DisplayName("Parameterized duplicate key handling")
    void testDuplicateKeyParameterized(Object k1, Object v1, Object k2, Object v2) {
        Map<Object, Object> map = MapUtilities.mapOf(k1, v1, k2, v2);
        assertEquals(1, map.size(), "Duplicate key should result in a single entry");
        assertEquals(v2, map.get(k2), "Value should be from the second put");
    }
}
