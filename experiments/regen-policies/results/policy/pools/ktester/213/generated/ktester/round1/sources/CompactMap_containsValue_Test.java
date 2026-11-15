package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class CompactMap_containsValue_Test {

    /* --------------------------------------------------------------------- */
    private static void setInternalVal(CompactMap<?, ?> map, Object newVal) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        f.set(map, newVal);
    }

    /**
     * Creates a {@link CompactMap} with the given {@code compactSize} and a
     *  {@code singleValueKey}.  The map starts empty.
     */
    private <K, V> CompactMap<K, V> newTestMap(final int compactSize, K singleKey) {
        return CompactMap.<K, V>builder().compactSize(compactSize).singleValueKey(singleKey).build();
    }

    private static Stream<Arguments> arrayStorageProvider() {
        // compactSize = 5 → array used for 2‑5 entries
        CompactMap<String, Integer> map1 = new CompactMap_containsValue_Test().newTestMap(5, "id");
        map1.put("a", 1);
        map1.put("b", 2);
        Arguments a1 = Arguments.of(map1, 2, true, "Array storage – value exists");
        CompactMap<String, Integer> map2 = new CompactMap_containsValue_Test().newTestMap(5, "id");
        map2.put("a", 1);
        map2.put("b", 2);
        Arguments a2 = Arguments.of(map2, 3, false, "Array storage – value absent");
        CompactMap<String, Integer> map3 = new CompactMap_containsValue_Test().newTestMap(5, "id");
        map3.put("a", null);
        map3.put("b", 2);
        Arguments a3 = Arguments.of(map3, null, true, "Array storage – null value present");
        return Stream.of(a1, a2, a3);
    }

    private static Stream<Arguments> mapStorageProvider() {
        // compactSize = 3 → after 4 entries we are in Map storage
        CompactMap<Integer, String> map1 = new CompactMap_containsValue_Test().newTestMap(3, 0);
        for (int i = 0; i < 60; i++) {
            map1.put(i, "val" + i);
        }
        map1.put(42, "answer");
        Arguments a1 = Arguments.of(map1, "answer", true, "Map storage – existing value");
        CompactMap<Integer, String> map2 = new CompactMap_containsValue_Test().newTestMap(3, 0);
        for (int i = 0; i < 60; i++) {
            map2.put(i, "val" + i);
        }
        Arguments a2 = Arguments.of(map2, "missing", false, "Map storage – absent value");
        CompactMap<Integer, String> map3 = new CompactMap_containsValue_Test().newTestMap(3, 0);
        for (int i = 0; i < 60; i++) {
            map3.put(i, "val" + i);
        }
        map3.put(99, null);
        Arguments a3 = Arguments.of(map3, null, true, "Map storage – null value present");
        return Stream.of(a1, a2, a3);
    }

    private static Stream<Arguments> singleEntryProvider() {
        // map with exactly one entry ("id" = "single")
        CompactMap<String, String> map1 = new CompactMap_containsValue_Test().newTestMap(5, "id");
        map1.put("id", "single");
        Arguments a1 = Arguments.of(map1, "single", true, "Single‑entry – matching value");
        Arguments a2 = Arguments.of(map1, "other", false, "Single‑entry – non‑matching value");
        CompactMap<String, String> map2 = new CompactMap_containsValue_Test().newTestMap(5, "id");
        map2.put("id", null);
        Arguments a3 = Arguments.of(map2, null, true, "Single‑entry – null stored value");
        return Stream.of(a1, a2, a3);
    }

//     @ParameterizedTest(name = "{index}: {2}")
//     @MethodSource("arrayStorageProvider")
//     void testContainsValueArrayStorage(CompactMap<String, Integer> map, Object search, boolean expected, String description) {
//         assertEquals(expected, map.containsValue(search), description);
//     }

//     @ParameterizedTest(name = "{index}: {2}")
//     @MethodSource("mapStorageProvider")
//     void testContainsValueMapStorage(CompactMap<Integer, String> map, Object search, boolean expected, String description) {
//         assertEquals(expected, map.containsValue(search), description);
//     }

//     @ParameterizedTest(name = "{index}: {2}")
//     @MethodSource("singleEntryProvider")
//     void testContainsValueSingleEntryMap(CompactMap<String, String> map, Object search, boolean expected, String description) {
//         assertEquals(expected, map.containsValue(search), description);
//     }

//     @Test
//     void testContainsValueExactlyCompactSize_ValuePresent() throws Exception {
//         // compactSize = 50, add exactly 50 entries → still array storage
//         CompactMap<Integer, String> map = newTestMap(CompactMap.DEFAULT_COMPACT_SIZE, 0);
//         for (int i = 0; i < CompactMap.DEFAULT_COMPACT_SIZE; i++) {
//             map.put(i, "v" + i);
//         }
//         // ensure the value we look for is present
//         assertTrue(map.containsValue("v25"), "Exact compact size – present value must be found");
//     }

//     @Test
//     void testContainsValueExactlyCompactSize_ValueAbsent() throws Exception {
//         CompactMap<Integer, String> map = newTestMap(CompactMap.DEFAULT_COMPACT_SIZE, 0);
//         for (int i = 0; i < CompactMap.DEFAULT_COMPACT_SIZE; i++) {
//             map.put(i, "v" + i);
//         }
//         assertFalse(map.containsValue("absent"), "Exact compact size – absent value must return false");
//     }

//     @Test
//     void testContainsValueOddLengthArray_ThrowsArrayIndexOutOfBoundsException() throws Exception {
//         CompactMap<String, Integer> map = newTestMap(5, "id");
//         // Force the internal val to an odd‑length array via reflection
//         setInternalVal(map, new Object[] { "k1", 1, "k2" });
//         // The loop expects pairs; accessing entries[ i + 1 ] will overflow
//         assertThrows(ArrayIndexOutOfBoundsException.class, () -> map.containsValue(1), "Odd‑length internal array must cause ArrayIndexOutOfBoundsException");
//     }

//     @Test
//     void testContainsValueNullInternalVal_ThrowsNullPointerException() throws Exception {
//         CompactMap<String, Integer> map = newTestMap(5, "id");
//         setInternalVal(map, null);
//         assertThrows(NullPointerException.class, () -> map.containsValue(42), "Null internal 'val' should propagate NullPointerException from getLogicalSingleValue()");
//     }

//     @Test
//     void testContainsValueDelegateMapThrowsRuntimeException() throws Exception {
//         CompactMap<String, Integer> map = newTestMap(5, "id");
//         // custom map that throws from containsValue
//         Map<String, Integer> explodingMap = new HashMap<>() {
// 
//             @Override
//             public boolean containsValue(Object value) {
//                 throw new RuntimeException("Deliberate exception from delegate map");
//             }
//         };
//         setInternalVal(map, explodingMap);
//         RuntimeException ex = assertThrows(RuntimeException.class, () -> map.containsValue(99), "Exception from delegate map must propagate unchanged");
//         assertEquals("Deliberate exception from delegate map", ex.getMessage());
//     }
}