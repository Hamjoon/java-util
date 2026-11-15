package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_mapOf_Test {

    private interface Mutator {

        void mutate(Map<String, Integer> map);
    }

    private static Stream<Arguments> singleEntryProvider() {
        return Stream.of(Arguments.of("Normal non‑null key/value", "alpha", 42, 1, List.of("alpha"), Map.of("alpha", 42)), Arguments.of("Null key with non‑null value", null, 99, 1, List.of((Object) null), Map.of(null, 99)), Arguments.of("Non‑null key with null value", "beta", null, 1, List.of("beta"), Map.of("beta", null)));
    }

    private static Stream<Arguments> twoEntryProvider() {
        return Stream.of(Arguments.of("Distinct keys", "first", 1, "second", 2, 2, List.of("first", "second"), Map.of("first", 1, "second", 2)), Arguments.of("Duplicate keys – later wins", "dup", 10, "dup", 20, 1, List.of("dup"), Map.of("dup", 20)), Arguments.of("Null key + non‑null key", null, 5, "nonNull", 6, 2, List.of(null, "nonNull"), Map.of(null, 5, "nonNull", 6)), Arguments.of("Duplicate null keys – later wins", null, 7, null, 8, 1, List.of(null), Map.of(null, 8)));
    }

    private static Stream<Arguments> threeEntryProvider() {
        return Stream.of(Arguments.of("All distinct keys", "a", 1, "b", 2, "c", 3, 3, List.of("a", "b", "c"), Map.of("a", 1, "b", 2, "c", 3)), Arguments.of("Duplicate first/third key – later wins", "x", 100, "y", 200, "x", 300, 2, List.of("x", "y"), Map.of("x", 300, "y", 200)));
    }

    private static Stream<Arguments> sixEntryProvider() {
        return Stream.of(Arguments.of("All distinct keys", new Object[] { "k1", "k2", "k3", "k4", "k5", "k6" }, new Object[] { 1, 2, 3, 4, 5, 6 }, 6, List.of("k1", "k2", "k3", "k4", "k5", "k6"), Map.of("k1", 1, "k2", 2, "k3", 3, "k4", 4, "k5", 5, "k6", 6)), Arguments.of("Multiple duplicate keys – last wins, position of first occurrence kept", new Object[] { "dup", "dup", "dup", "unique1", "unique2", "unique3" }, new Object[] { 1, 2, 3, 4, 5, 6 }, 4, List.of("dup", "unique1", "unique2", "unique3"), Map.of("dup", 3, "unique1", 4, "unique2", 5, "unique3", 6)));
    }

    private static Stream<Arguments> collectionViewMutationsProvider() {
        return Stream.of(Arguments.of("keySet().remove(\"anyKey\")", (Mutator) m -> m.keySet().remove("anyKey")), Arguments.of("keySet().add(\"newKey\")", (Mutator) m -> {
            @SuppressWarnings("unchecked")
            Set<String> ks = (Set<String>) m.keySet();
            ((Set<String>) ks).add("newKey");
        }), Arguments.of("values().remove(42)", (Mutator) m -> m.values().remove(42)), Arguments.of("entrySet().iterator().remove()", (Mutator) m -> {
            Iterator<Map.Entry<String, Integer>> it = m.entrySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }), Arguments.of("replace(\"key\", 1, 2)", (Mutator) m -> m.replace("key", 1, 2)), Arguments.of("compute(\"key\", (k,v)->v)", (Mutator) m -> m.compute("key", (k, v) -> v)));
    }

    @Test
    void mapOf_returnsEmptyUnmodifiableMap() {
        // given
        Map<String, Integer> result = MapUtilities.mapOf();
        // then – the map must be empty
        assertTrue(result.isEmpty(), "The map should be empty");
        // then – the map must be unmodifiable
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> result.put("key", 1), "Putting a value should throw UnsupportedOperationException"), () -> assertThrows(UnsupportedOperationException.class, () -> result.remove("key"), "Removing a value should throw UnsupportedOperationException"));
        // then – the concrete type should be the unmodifiable wrapper from Collections
        String className = result.getClass().getName();
        assertTrue(className.startsWith("java.util.Collections$UnmodifiableMap"), "Expected a Collections$UnmodifiableMap implementation, but got: " + className);
        // then – verify the hidden backing map is a LinkedHashMap (preserves order)
        try {
            // field name used by JDK
            Field f = result.getClass().getDeclaredField("m");
            f.setAccessible(true);
            Object backing = f.get(result);
            assertNotNull(backing, "Backing map should not be null");
            assertTrue(backing instanceof LinkedHashMap, "Backing map should be a LinkedHashMap, but was: " + backing.getClass());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Reflection failed to access backing map: " + e);
        }
    }

    @Test
    void mapOf_multipleCallsReturnDistinctInstances() {
        // given
        Map<String, Integer> first = MapUtilities.mapOf();
        Map<String, Integer> second = MapUtilities.mapOf();
        // then – each call should return a new map instance
        assertNotSame(first, second, "Each invocation should produce a distinct map instance");
        // both must still be empty and unmodifiable
        assertAll(() -> assertTrue(first.isEmpty()), () -> assertTrue(second.isEmpty()), () -> assertThrows(UnsupportedOperationException.class, () -> first.put("a", 1)), () -> assertThrows(UnsupportedOperationException.class, () -> second.put("b", 2)));
    }

    /* --------------------------------------------------------------------- *
     *  Single‑entry overload (mapOf(K,V))                                 *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("singleEntryProvider")
    void mapOf_singleEntry(String description, Object key, Object value, int expectedSize, List<Object> expectedOrder, Map<Object, Object> expectedEntries) {
        // when
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = (Map<Object, Object>) MapUtilities.mapOf(key, value);
        // then – size & contents
        assertEquals(expectedSize, map.size(), "Size mismatch");
        assertEquals(expectedEntries, map, "Map entries mismatch");
        // then – iteration order (LinkedHashMap preserves insertion order)
        List<Object> actualOrder = new ArrayList<>();
        for (Object k : map.keySet()) {
            actualOrder.add(k);
        }
        assertEquals(expectedOrder, actualOrder, "Iteration order mismatch");
        // then – immutability checks
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> map.put("newKey", 123)), () -> assertThrows(UnsupportedOperationException.class, () -> map.remove(key)), () -> assertThrows(UnsupportedOperationException.class, () -> map.clear()));
    }

    /* --------------------------------------------------------------------- *
     *  Two‑entry overload (mapOf(K,V,K,V))                                 *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("twoEntryProvider")
    void mapOf_twoEntries(String description, Object k1, Object v1, Object k2, Object v2, int expectedSize, List<Object> expectedOrder, Map<Object, Object> expectedEntries) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = (Map<Object, Object>) MapUtilities.mapOf(k1, v1, k2, v2);
        assertEquals(expectedSize, map.size(), "Size mismatch");
        assertEquals(expectedEntries, map, "Map entries mismatch");
        List<Object> actualOrder = new ArrayList<>(map.keySet());
        assertEquals(expectedOrder, actualOrder, "Iteration order mismatch");
        // immutability
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> map.put("any", 0)), () -> assertThrows(UnsupportedOperationException.class, () -> map.remove(k1)), () -> assertThrows(UnsupportedOperationException.class, () -> map.clear()));
    }

    /* --------------------------------------------------------------------- *
     *  Three‑entry overload (mapOf(K,V,K,V,K,V))                           *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("threeEntryProvider")
    void mapOf_threeEntries(String description, Object k1, Object v1, Object k2, Object v2, Object k3, Object v3, int expectedSize, List<Object> expectedOrder, Map<Object, Object> expectedEntries) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = (Map<Object, Object>) MapUtilities.mapOf(k1, v1, k2, v2, k3, v3);
        assertEquals(expectedSize, map.size(), "Size mismatch");
        assertEquals(expectedEntries, map, "Map entries mismatch");
        assertEquals(expectedOrder, new ArrayList<>(map.keySet()), "Iteration order mismatch");
        // immutability
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> map.put("x", 0)), () -> assertThrows(UnsupportedOperationException.class, () -> map.remove(k2)), () -> assertThrows(UnsupportedOperationException.class, () -> map.clear()));
    }

    /* --------------------------------------------------------------------- *
     *  Six‑entry overload (mapOf(K,V,…,K,V))                               *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("sixEntryProvider")
    void mapOf_sixEntries(String description, Object[] keys, Object[] values, int expectedSize, List<Object> expectedOrder, Map<Object, Object> expectedEntries) {
        // Build the call via reflection because the overload has 12 parameters
        try {
            // obtain the method with 12 parameters (K,V) repeated six times
            java.lang.reflect.Method m = MapUtilities.class.getMethod("mapOf", Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class, Object.class);
            Object mapObj = m.invoke(null, keys[0], values[0], keys[1], values[1], keys[2], values[2], keys[3], values[3], keys[4], values[4], keys[5], values[5]);
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) mapObj;
            assertEquals(expectedSize, map.size(), "Size mismatch");
            assertEquals(expectedEntries, map, "Map entries mismatch");
            assertEquals(expectedOrder, new ArrayList<>(map.keySet()), "Iteration order mismatch");
            // immutability
            assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> map.put("any", 0)), () -> assertThrows(UnsupportedOperationException.class, () -> map.remove(keys[0])), () -> assertThrows(UnsupportedOperationException.class, () -> map.clear()));
        } catch (Exception e) {
            fail("Reflection invocation failed: " + e);
        }
    }

    /* --------------------------------------------------------------------- *
     *  Immutability verification across overloads (generic helper)       *
     * --------------------------------------------------------------------- */
    @Test
    void mapOf_immutabilityAcrossAllOverloads() {
        // single entry
        Map<String, Integer> m1 = MapUtilities.mapOf("immutable", 123);
        // two entries
        Map<String, Integer> m2 = MapUtilities.mapOf("a", 1, "b", 2);
        // three entries
        Map<String, Integer> m3 = MapUtilities.mapOf("x", 10, "y", 20, "z", 30);
        List<Map<?, ?>> maps = List.of(m1, m2, m3);
        for (Map<?, ?> map : maps) {
            assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> map.put("any", 0), "put should fail"), () -> assertThrows(UnsupportedOperationException.class, () -> map.remove(map.keySet().iterator().next()), "remove should fail"), () -> assertThrows(UnsupportedOperationException.class, map::clear, "clear should fail"), () -> assertThrows(UnsupportedOperationException.class, () -> map.putAll(Map.of("x", 1)), "putAll should fail"));
        }
    }

    /* --------------------------------------------------------------------- *
     *  Mutating operations via collection views on the zero‑arg map      *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("collectionViewMutationsProvider")
    void mapOf_collectionViewMutations(String description, Mutator mutator) {
        Map<String, Integer> map = MapUtilities.mapOf();
        assertThrows(UnsupportedOperationException.class, () -> mutator.mutate(map), description);
    }
}
