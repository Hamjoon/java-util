package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.stream.*;

@ExtendWith(MockitoExtension.class)
class MapUtilities_mapOf_5_Test {

    /**
     * Helper to build a map from an array of alternating key/value objects.
     */
    private static <K, V> Map<K, V> mapOfArray(Object... kv) {
        @SuppressWarnings("unchecked")
        K k1 = (K) kv[0];
        @SuppressWarnings("unchecked")
        V v1 = (V) kv[1];
        @SuppressWarnings("unchecked")
        K k2 = (K) kv[2];
        @SuppressWarnings("unchecked")
        V v2 = (V) kv[3];
        @SuppressWarnings("unchecked")
        K k3 = (K) kv[4];
        @SuppressWarnings("unchecked")
        V v3 = (V) kv[5];
        @SuppressWarnings("unchecked")
        K k4 = (K) kv[6];
        @SuppressWarnings("unchecked")
        V v4 = (V) kv[7];
        return MapUtilities.mapOf(k1, v1, k2, v2, k3, v3, k4, v4);
    }

    /**
     * Data provider for the “Normal behavior – distinct keys and values” group.
     */
    static Stream<Arguments> normalBehaviorProvider() {
        return Stream.of(Arguments.of(new Object[] { "A", 1, "B", 2, "C", 3, "D", 4 }, Map.of("A", 1, "B", 2, "C", 3, "D", 4), List.of("A", "B", "C", "D")));
    }

    /**
     * Data provider for the “Null handling – null keys and values allowed” group.
     */
    static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(Arguments.of(new Object[] { null, null, "key", null, null, "value", "last", 42 }, // Expected map: last occurrence of null key wins, plus the non‑null entry.
        Map.of("key", null, null, "value", "last", 42), List.of(null, "key", "last")));
    }

    /**
     * Data provider for the “Duplicate keys – later value overwrites earlier” group.
     */
    static Stream<Arguments> duplicateKeyProvider() {
        return Stream.of(Arguments.of(new Object[] { "X", 10, "Y", 20, "X", 30, "Z", 40 }, Map.of("X", 30, "Y", 20, "Z", 40), List.of("X", "Y", "Z")));
    }

    /**
     * Data provider for the “Order preservation – verify iterator order matches insertion order” group.
     */
    static Stream<Arguments> orderPreservationProvider() {
        return Stream.of(Arguments.of(new Object[] { "first", "A", "second", "B", "third", "C", "fourth", "D" }, List.of("first", "second", "third", "fourth")));
    }

    /**
     * Data provider for the “All null inputs” group.
     */
    static Stream<Arguments> allNullProvider() {
        return Stream.of(Arguments.of(new Object[] { null, null, null, null, null, null, null, null }, Map.of((Object) null, null), List.of((Object) null)));
    }

    /**
     * Data provider for the “Mixed null and duplicate keys” group.
     */
    static Stream<Arguments> mixedNullDuplicateProvider() {
        return Stream.of(Arguments.of(new Object[] { "A", null, null, "val1", "A", "val2", null, null }, Map.of("A", "val2", null, null), List.of("A", null)));
    }

    /**
     * Data provider for the “Non‑String keys and values (different types)” group.
     */
    static Stream<Arguments> heterogeneousTypesProvider() {
        Date epoch = new Date(0);
        List<Object> emptyList = new ArrayList<>();
        Set<Object> emptySet = new HashSet<>();
        return Stream.of(Arguments.of(new Object[] { 1, "one", 2L, true, new BigDecimal("3.14"), epoch, emptyList, emptySet }, // Expected map – we use the actual objects as keys/values.
        Map.of(1, "one", 2L, true, new BigDecimal("3.14"), epoch, emptyList, emptySet), List.of(1, 2L, new BigDecimal("3.14"), emptyList)));
    }

    /**
     * Data provider for the “Key objects with equal hashCode but not equal (collision handling)” group.
     */
    static Stream<Arguments> hashCollisionProvider() {
        Integer i = Integer.valueOf(42);
        Long l = Long.valueOf(42L);
        Short s = Short.valueOf((short) 42);
        Byte b = Byte.valueOf((byte) 42);
        return Stream.of(Arguments.of(new Object[] { i, "first", l, "second", s, "third", b, "fourth" }, // All four keys are distinct according to equals()
        Map.of(i, "first", l, "second", s, "third", b, "fourth"), List.of(i, l, s, b)));
    }

    /**
     * Checks whether the supplied map is unmodifiable by attempting a mutating operation.
     */
    private static boolean isUnmodifiable(Map<?, ?> map) {
        try {
            // Attempt a mutation that must fail.
            map.put(null, null);
            // mutation succeeded – not unmodifiable
            return false;
        } catch (UnsupportedOperationException expected) {
            return true;
        }
    }

    @Test
    void mapOfCreatesMapWithCorrectSizeAndEntries() {
        // given
        String k1 = "one";
        Integer v1 = 1;
        String k2 = "two";
        Integer v2 = 2;
        String k3 = "three";
        Integer v3 = 3;
        String k4 = "four";
        Integer v4 = 4;
        // when
        Map<String, Integer> result = MapUtilities.mapOf(k1, v1, k2, v2, k3, v3, k4, v4);
        // then
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(4, result.size(), "Map should contain exactly four entries");
        assertEquals(v1, result.get(k1));
        assertEquals(v2, result.get(k2));
        assertEquals(v3, result.get(k3));
        assertEquals(v4, result.get(k4));
    }

    @Test
    void mapOfPreservesInsertionOrder() {
        // given
        String k1 = "first";
        String k2 = "second";
        String k3 = "third";
        String k4 = "fourth";
        // when
        Map<String, String> result = MapUtilities.mapOf(k1, "1", k2, "2", k3, "3", k4, "4");
        // then
        assertTrue(result instanceof LinkedHashMap, "Implementation should be a LinkedHashMap to preserve order");
        Iterator<String> keyIter = result.keySet().iterator();
        assertEquals(k1, keyIter.next());
        assertEquals(k2, keyIter.next());
        assertEquals(k3, keyIter.next());
        assertEquals(k4, keyIter.next());
    }

    @Test
    void mapOfReturnsUnmodifiableMap() {
        // given
        Map<String, Integer> map = MapUtilities.mapOf("a", 1, "b", 2, "c", 3, "d", 4);
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> map.put("e", 5), "Map should be unmodifiable – put operation must throw");
        assertThrows(UnsupportedOperationException.class, () -> map.remove("a"), "Map should be unmodifiable – remove operation must throw");
        assertThrows(UnsupportedOperationException.class, () -> map.clear(), "Map should be unmodifiable – clear operation must throw");
    }

    @Test
    void mapOfHandlesDuplicateKeysByLastValueWins() {
        // given
        String duplicateKey = "dup";
        // when
        Map<String, Integer> result = MapUtilities.mapOf(duplicateKey, 1, "x", 10, duplicateKey, 2, "y", 20);
        // then
        // The later occurrence of the duplicate key should overwrite the earlier one.
        assertEquals(3, result.size(), "Map should contain three distinct keys");
        assertEquals(2, result.get(duplicateKey), "Value for duplicate key should be the last supplied one");
        assertEquals(10, result.get("x"));
        assertEquals(20, result.get("y"));
    }

    @Test
    void mapOfAllowsNullKeysAndValues() {
        // given
        String key1 = null;
        String key2 = "notNull";
        Integer value1 = null;
        Integer value2 = 42;
        // when
        Map<String, Integer> result = MapUtilities.mapOf(key1, value1, key2, value2, "k3", 3, "k4", 4);
        // then
        assertTrue(result.containsKey(null), "Map should contain a null key");
        assertNull(result.get(null), "Value for null key should be null");
        assertEquals(value2, result.get(key2));
    }

    @ParameterizedTest(name = "[{index}] normal behavior")
    @MethodSource("normalBehaviorProvider")
    void normalBehaviorDistinctKeys(Object[] inputs, Map<String, Integer> expectedMap, List<String> expectedOrder) {
        @SuppressWarnings("unchecked")
        Map<String, Integer> result = mapOfArray(inputs);
        assertEquals(expectedMap, result);
        assertEquals(expectedOrder, new ArrayList<>(result.keySet()));
        assertTrue(isUnmodifiable(result), "Returned map must be unmodifiable");
    }

    @ParameterizedTest(name = "[{index}] null handling")
    @MethodSource("nullHandlingProvider")
    void nullHandling(Object[] inputs, Map<String, Object> expectedMap, List<String> expectedOrder) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = mapOfArray(inputs);
        assertEquals(expectedMap, result);
        assertEquals(expectedOrder, new ArrayList<>(result.keySet()));
        assertTrue(isUnmodifiable(result));
    }

    @ParameterizedTest(name = "[{index}] duplicate keys")
    @MethodSource("duplicateKeyProvider")
    void duplicateKeysOverwrite(Object[] inputs, Map<String, Integer> expectedMap, List<String> expectedOrder) {
        @SuppressWarnings("unchecked")
        Map<String, Integer> result = mapOfArray(inputs);
        assertEquals(expectedMap, result);
        assertEquals(expectedOrder, new ArrayList<>(result.keySet()));
        assertTrue(isUnmodifiable(result));
    }

    @ParameterizedTest(name = "[{index}] order preservation")
    @MethodSource("orderPreservationProvider")
    void orderPreservation(Object[] inputs, List<String> expectedOrder) {
        @SuppressWarnings("unchecked")
        Map<String, String> result = mapOfArray(inputs);
        assertEquals(expectedOrder, new ArrayList<>(result.keySet()));
        assertTrue(isUnmodifiable(result));
    }

    @ParameterizedTest(name = "[{index}] all null inputs")
    @MethodSource("allNullProvider")
    void allNullInputs(Object[] inputs, Map<Object, Object> expectedMap, List<Object> expectedOrder) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> result = mapOfArray(inputs);
        assertEquals(expectedMap, result);
        assertEquals(expectedOrder, new ArrayList<>(result.keySet()));
        assertTrue(isUnmodifiable(result));
    }

    @ParameterizedTest(name = "[{index}] mixed null and duplicate keys")
    @MethodSource("mixedNullDuplicateProvider")
    void mixedNullAndDuplicate(Object[] inputs, Map<Object, Object> expectedMap, List<Object> expectedOrder) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> result = mapOfArray(inputs);
        assertEquals(expectedMap, result);
        assertEquals(expectedOrder, new ArrayList<>(result.keySet()));
        assertTrue(isUnmodifiable(result));
    }

    @ParameterizedTest(name = "[{index}] heterogeneous types")
    @MethodSource("heterogeneousTypesProvider")
    void heterogeneousKeyValueTypes(Object[] inputs, Map<Object, Object> expectedMap, List<Object> expectedOrder) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> result = mapOfArray(inputs);
        assertEquals(expectedMap, result);
        assertEquals(expectedOrder, new ArrayList<>(result.keySet()));
        assertTrue(isUnmodifiable(result));
    }

    @ParameterizedTest(name = "[{index}] hash collisions")
    @MethodSource("hashCollisionProvider")
    void hashCollisionHandling(Object[] inputs, Map<Object, Object> expectedMap, List<Object> expectedOrder) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> result = mapOfArray(inputs);
        assertEquals(expectedMap, result);
        assertEquals(expectedOrder, new ArrayList<>(result.keySet()));
        assertTrue(isUnmodifiable(result));
    }

    @Test
    void entrySetIteratorRemoveThrows() {
        Map<String, Integer> map = MapUtilities.mapOf("x", 1, "y", 2, "z", 3, "w", 4);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        assertTrue(it.hasNext());
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove() on an unmodifiable map must throw");
    }

    @Test
    void clearThrowsUnsupportedOperationException() {
        Map<String, Integer> map = MapUtilities.mapOf("a", 1, "b", 2, "c", 3, "d", 4);
        assertThrows(UnsupportedOperationException.class, map::clear, "clear() on an unmodifiable map must throw");
    }

    @Test
    void putAllThrowsUnsupportedOperationException() {
        Map<String, Integer> map = MapUtilities.mapOf("k1", 1, "k2", 2, "k3", 3, "k4", 4);
        Map<String, Integer> other = Map.of("new", 5);
        assertThrows(UnsupportedOperationException.class, () -> map.putAll(other), "putAll() on an unmodifiable map must throw");
    }

    @Test
    void computeIfAbsentThrowsUnsupportedOperationException() {
        Map<String, Integer> map = MapUtilities.mapOf("x", 10, "y", 20, "z", 30, "w", 40);
        assertThrows(UnsupportedOperationException.class, () -> map.computeIfAbsent("new", k -> 99), "computeIfAbsent() on an unmodifiable map must throw");
    }

    @Test
    void valuesRemoveThrowsUnsupportedOperationException() {
        Map<String, Integer> map = MapUtilities.mapOf("one", 1, "two", 2, "three", 3, "four", 4);
        Collection<Integer> values = map.values();
        assertThrows(UnsupportedOperationException.class, () -> values.remove(1), "Removing via values() collection must throw");
    }

    @Test
    void keySetRemoveThrowsUnsupportedOperationException() {
        Map<String, Integer> map = MapUtilities.mapOf("alpha", 100, "beta", 200, "gamma", 300, "delta", 400);
        Set<String> keys = map.keySet();
        assertThrows(UnsupportedOperationException.class, () -> keys.remove("alpha"), "Removing via keySet() must throw");
    }
}
