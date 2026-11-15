package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_ceilingKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test
        map = new ConcurrentNavigableMapNullSafe<>();
        // Populate with a known ordering
        map.put("a", 1);
        map.put("c", 3);
        map.put("e", 5);
        // Include a null key (treated as last in ascending order)
        map.put(null, 0);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; map is recreated before each test
        map = null;
    }

    static Stream<Arguments> ceilingKeyNormalCases() {
        return Stream.of(// Map [1,2,3]; exact match
        Arguments.of(new Integer[] { 1, 2, 3 }, false, 2, 2, "Exact match – key 2 exists"), // Map [1,3,5]; between existing keys
        Arguments.of(new Integer[] { 1, 3, 5 }, false, 2, 3, "Key 2 not present – smallest greater key is 3"), // Map [1,4,7]; greater than all keys
        Arguments.of(new Integer[] { 1, 4, 7 }, false, 10, null, "Key 10 larger than any key – should return null"), // Map [1,4,9]; less than all keys
        Arguments.of(new Integer[] { 1, 4, 9 }, false, -5, 1, "Key -5 smaller than smallest – should return first key 1"), // Map contains null key and a non‑null key 5
        Arguments.of(new Integer[] { 5 }, true, null, null, "Null key argument when map already contains a null key"), // Map does NOT contain a null key, only key 5
        Arguments.of(new Integer[] { 5 }, false, null, 5, "Null key argument when map has no null key – returns smallest non‑null key"), // Empty map
        Arguments.of(new Integer[] {}, false, 42, null, "Empty map – any key should return null"));
    }

    static Stream<Arguments> ceilingKeyAdditionalCases() {
        // Helper to create a map with Integer keys and optional comparator.
        java.util.function.BiFunction<Integer[], Comparator<Integer>, ConcurrentNavigableMapNullSafe<Integer, Integer>> intMapBuilder = (keys, comp) -> {
            ConcurrentNavigableMapNullSafe<Integer, Integer> m = (comp == null) ? new ConcurrentNavigableMapNullSafe<>() : new ConcurrentNavigableMapNullSafe<>(comp);
            for (Integer k : keys) {
                m.put(k, k);
            }
            return m;
        };
        // 1) Exact greatest key in natural order
        ConcurrentNavigableMapNullSafe<Integer, Integer> map1 = intMapBuilder.apply(new Integer[] { 5, 10, 15 }, null);
        // 2) Between keys in natural order
        ConcurrentNavigableMapNullSafe<Integer, Integer> map2 = intMapBuilder.apply(new Integer[] { 5, 10, 15 }, null);
        // 3) Smaller than smallest key
        ConcurrentNavigableMapNullSafe<Integer, Integer> map3 = intMapBuilder.apply(new Integer[] { 5, 10, 15 }, null);
        // 4) Only a null key
        ConcurrentNavigableMapNullSafe<Integer, Integer> map4 = new ConcurrentNavigableMapNullSafe<>();
        map4.put(null, 0);
        // 5) Empty map
        ConcurrentNavigableMapNullSafe<Integer, Integer> map5 = new ConcurrentNavigableMapNullSafe<>();
        // 6) Reverse‑order comparator
        ConcurrentNavigableMapNullSafe<Integer, Integer> map6 = intMapBuilder.apply(new Integer[] { 5, 3, 1 }, Collections.reverseOrder());
        // 7) Case‑insensitive String comparator
        ConcurrentNavigableMapNullSafe<String, Integer> map7 = new ConcurrentNavigableMapNullSafe<>(String.CASE_INSENSITIVE_ORDER);
        map7.put("Apple", 1);
        map7.put("banana", 2);
        // 8) Integer keys – class‑cast scenario (handled in exception tests)
        // 9) UUID keys
        ConcurrentNavigableMapNullSafe<UUID, Integer> map9 = new ConcurrentNavigableMapNullSafe<>();
        UUID uuid1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        UUID uuid2 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        map9.put(uuid1, 1);
        map9.put(uuid2, 2);
        // 10) Single key [0]
        ConcurrentNavigableMapNullSafe<Integer, Integer> map10 = intMapBuilder.apply(new Integer[] { 0 }, null);
        return Stream.of(Arguments.of(map1, 15, 15, "Exact greatest key – should return 15"), Arguments.of(map2, 12, 15, "Key 12 not present – smallest greater key is 15"), Arguments.of(map3, 0, 5, "Key 0 smaller than smallest – should return 5"), Arguments.of(map4, null, null, "Only null key present – ceilingKey(null) returns null"), Arguments.of(map5, null, null, "Empty map – ceilingKey(null) returns null"), Arguments.of(map6, 4, 3, "Reverse order comparator – ceilingKey(4) returns 3"), Arguments.of(map7, "apple", "Apple", "Case‑insensitive comparator – matches existing \"Apple\" key"), Arguments.of(map9, uuid1, uuid1, "UUID key matches – should return identical UUID"), Arguments.of(map10, 0, 0, "Single key map – exact match returns 0"));
    }

    static Stream<Arguments> ceilingKeyExceptionCases() {
        // 1) Integer map – query with Double → ClassCastException
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, 1);
        intMap.put(2, 2);
        intMap.put(3, 3);
        // 2) Integer map without null key – query null → ClassCastException
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMapNoNull = new ConcurrentNavigableMapNullSafe<>();
        intMapNoNull.put(1, 1);
        intMapNoNull.put(2, 2);
        intMapNoNull.put(3, 3);
        // 3) Comparator that throws IllegalArgumentException for key == 5
        Comparator<Integer> illegalComparator = (a, b) -> {
            if (Objects.equals(a, 5) || Objects.equals(b, 5)) {
                throw new IllegalArgumentException("Key 5 is not allowed");
            }
            return Integer.compare(a, b);
        };
        ConcurrentNavigableMapNullSafe<Integer, Integer> illegalMap = new ConcurrentNavigableMapNullSafe<>(illegalComparator);
        illegalMap.put(1, 1);
        illegalMap.put(5, 5);
        illegalMap.put(10, 10);
        // 4) Comparator that throws NullPointerException when encountering the internal null sentinel
        Comparator<Integer> npeComparator = (a, b) -> {
            // The sentinel used by the map is a unique String; any attempt to cast it to Integer will NPE
            return Integer.compare(a, b);
        };
        // To provoke NPE we need a map that contains a null key (masked internally)
        ConcurrentNavigableMapNullSafe<Integer, Integer> npeMap = new ConcurrentNavigableMapNullSafe<>(npeComparator);
        npeMap.put(null, 0);
        npeMap.put(1, 1);
        npeMap.put(2, 2);
        return Stream.of(Arguments.of(intMap, 2.5, ClassCastException.class, "Double key on Integer map should cause ClassCastException"), Arguments.of(intMapNoNull, null, ClassCastException.class, "Null argument on map without null key should cause ClassCastException"), Arguments.of(illegalMap, 5, IllegalArgumentException.class, "Comparator deliberately throws IllegalArgumentException for key 5"), Arguments.of(npeMap, null, NullPointerException.class, "Comparator does not handle internal null sentinel – NullPointerException expected"));
    }

    @Test
    void ceilingKey_exactMatch() {
        assertEquals("a", map.ceilingKey("a"));
        assertEquals("c", map.ceilingKey("c"));
        assertEquals("e", map.ceilingKey("e"));
    }

    @Test
    void ceilingKey_betweenExistingKeys() {
        // "b" is between "a" and "c"
        assertEquals("c", map.ceilingKey("b"));
        // "d" is between "c" and "e"
        assertEquals("e", map.ceilingKey("d"));
    }

    @Test
    void ceilingKey_greaterThanAllNonNullKeys() {
        // Anything greater than "e" should return the null key (last in ordering)
        assertNull(map.ceilingKey("f"));
        assertNull(map.ceilingKey("zzzz"));
    }

    @Test
    void ceilingKey_lessThanAllKeys() {
        // Anything less than the smallest key should return the first key
        assertEquals("a", map.ceilingKey(""));
        assertEquals("a", map.ceilingKey("0"));
    }

    @Test
    void ceilingKey_nullKeyArgument() {
        // According to the contract, passing null returns the null key itself
        // because null is considered the greatest element.
        assertNull(map.ceilingKey(null));
    }

    @Test
    void ceilingKey_emptyMap() {
        ConcurrentNavigableMapNullSafe<String, Integer> empty = new ConcurrentNavigableMapNullSafe<>();
        assertNull(empty.ceilingKey("any"));
        assertNull(empty.ceilingKey(null));
    }

    /**
     * Normal (non‑exceptional) scenarios for {@code ceilingKey}.
     */
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("ceilingKeyNormalCases")
    void ceilingKey_normalScenarios(Integer[] keys, boolean includeNullKey, Integer queryKey, Integer expectedResult, String description) {
        // Build a map with the supplied keys and optional null key.
        ConcurrentNavigableMapNullSafe<Integer, Integer> testMap = new ConcurrentNavigableMapNullSafe<>();
        for (Integer k : keys) {
            // dummy value = key
            testMap.put(k, k);
        }
        if (includeNullKey) {
            testMap.put(null, 0);
        }
        Integer actual = testMap.ceilingKey(queryKey);
        assertEquals(expectedResult, actual, description);
    }

    /**
     * Additional scenarios covering custom comparators, different key types,
     * and edge cases.
     */
    @ParameterizedTest(name = "[{index}] {5}")
    @MethodSource("ceilingKeyAdditionalCases")
    void ceilingKey_additionalScenarios(Object mapInstance, Object queryKey, Object expectedResult, String description) {
        // The map instance is already prepared by the method source.
        @SuppressWarnings("unchecked")
        ConcurrentNavigableMapNullSafe<Object, Object> testMap = (ConcurrentNavigableMapNullSafe<Object, Object>) mapInstance;
        Object actual = testMap.ceilingKey(queryKey);
        assertEquals(expectedResult, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("ceilingKeyExceptionCases")
    void ceilingKey_exceptionScenarios(Object mapInstance, Object queryKey, Class<? extends Throwable> expectedException, String description) {
        @SuppressWarnings("unchecked")
        ConcurrentNavigableMapNullSafe<Object, Object> testMap = (ConcurrentNavigableMapNullSafe<Object, Object>) mapInstance;
        assertThrows(expectedException, () -> testMap.ceilingKey(queryKey), description);
    }
}
