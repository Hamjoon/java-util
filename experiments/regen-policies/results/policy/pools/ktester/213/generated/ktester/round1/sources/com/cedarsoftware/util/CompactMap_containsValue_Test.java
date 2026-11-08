package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CompactMap_containsValue_Test {

    /**
     * Helper that creates a {@link CompactMap} with the supplied {@code compactSize}.
     * The returned map overrides the configuration methods that the {@code CompactMap}
     * implementation uses to decide which internal storage strategy to employ.
     *
     * @param compactSize the size at which the map should switch from the compact
     *                    array representation to a backing {@link Map}
     * @return a fresh {@code CompactMap} ready for use in a test
     */
//     private CompactMap<String, Integer> newTestMap(final int compactSize) {
//         return new CompactMap<K, V>() {
// 
//             // the key used for the single‑value optimisation – any value works for the tests
//             @Override
//             @SuppressWarnings("unchecked")
//             protected K getSingleValueKey() {
//                 return (K) "id";
//             }
// 
//             // the threshold that triggers a backing map
//             @Override
//             protected int compactSize() {
//                 return compactSize;
//             }
// 
//             // the concrete Map implementation used when the threshold is exceeded
//             @Override
//             protected Map<K, V> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//     }

    /**
     * Helper that directly sets the private {@code val} field of a {@link CompactMap}
     * instance via reflection.  This is required for tests that want to force a
     * particular internal representation (e.g., an Object[] array) without using the
     * public API, which would otherwise transition the map automatically.
     *
     * @param map   the map whose internal state should be altered
     * @param value the value to assign to the {@code val} field (may be {@code null})
     * @throws Exception if reflection fails
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object value) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        f.set(map, value);
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> edgeCaseProvider() {
        return Stream.of(// null value present
        arguments(new Object[] { "k1", "a", "k2", null }, null, true), // duplicate values – "beta" appears twice
        arguments(new Object[] { "k1", "beta", "k2", "beta", "k3", "c" }, "beta", true), // different type – searching for a String when values are Integers
        arguments(new Object[] { "k1", 1, "k2", 2 }, "delta", false), // integer value 42 present
        arguments(new Object[] { "k1", 42, "k2", "foo" }, 42, true));
    }

    @Test
    void containsValue_emptyMap_returnsFalse() {
        CompactMap<String, Integer> map = newTestMap(3);
        assertTrue(map.isEmpty(), "Map should be empty initially");
        assertFalse(map.containsValue(1), "Empty map must not contain any value");
        assertFalse(map.containsValue(null), "Empty map must not contain null");
    }

    @Test
    void containsValue_singleEntry_matchesValue() {
        CompactMap<String, Integer> map = newTestMap(3);
        map.put("single", 42);
        assertEquals(1, map.size());
        assertTrue(map.containsValue(42), "Map should report the stored value");
        assertFalse(map.containsValue(99), "Map should not report a value that is not present");
        assertFalse(map.containsValue(null), "Map should not report null when no null entry exists");
    }

    @Test
    void containsValue_singleEntry_nullValue() {
        CompactMap<String, Integer> map = newTestMap(3);
        map.put("nullKey", null);
        assertEquals(1, map.size());
        assertTrue(map.containsValue(null), "Map should report presence of a null value");
        assertFalse(map.containsValue(0), "Map should not report a non‑null value");
    }

    @Test
    void containsValue_compactArray_behaviour() {
        // compactSize = 5 → array representation is used for 2‑4 entries
        CompactMap<String, Integer> map = newTestMap(5);
        map.put("a", 1);
        map.put("b", 2);
        // still within compact array range
        map.put("c", 3);
        assertEquals(3, map.size());
        assertTrue(map.containsValue(2), "Value 2 must be found in compact array storage");
        assertFalse(map.containsValue(4), "Value 4 must not be found");
        assertFalse(map.containsValue(null), "Null must not be found when not stored");
    }

    @Test
    void containsValue_backingMap_behaviour() {
        // compactSize = 2 → after adding 3 entries the map switches to a backing Map
        CompactMap<String, Integer> map = newTestMap(2);
        map.put("x", 10);
        map.put("y", 20);
        // At this point we are still in compact array mode (size == 2)
        // triggers transition to backing map
        map.put("z", 30);
        assertEquals(3, map.size());
        assertTrue(map.containsValue(20), "Value 20 must be found after transition to backing map");
        assertFalse(map.containsValue(99), "Value 99 must not be found");
        assertFalse(map.containsValue(null), "Null must not be found when not stored");
    }

    @Test
    void containsValue_backingMap_withNullValue() {
        // Verify that a null value is correctly detected after the map has switched
        CompactMap<String, Integer> map = newTestMap(2);
        map.put("k1", 1);
        map.put("k2", 2);
        // forces transition to backing map and stores null
        map.put("k3", null);
        assertEquals(3, map.size());
        assertTrue(map.containsValue(null), "Null value must be reported after transition to backing map");
    }

    /**
     * Basic Object[] representation – two simple cases: a matching value and a non‑matching one.
     */
//     @ParameterizedTest(name = "containsValue on compact array – search ''{0}'' → expected {1}")
//     @CsvSource({ "'beta', true", "'gamma', false" })
//     void containsValue_objectArray_basic(String search, boolean expected) throws Exception {
//         CompactMap<String, Object> map = newTestMap(10);
//         // internal representation: {"k1","alpha","k2","beta","k3","gamma"}
//         Object[] internal = new Object[] { "k1", "alpha", "k2", "beta", "k3", "gamma" };
//         setInternalVal(map, internal);
//         assertEquals(expected, map.containsValue(search));
//     }

    /**
     * Edge‑case scenarios for the compact array representation.
     * Each argument supplies the internal Object[] array, the value to search for,
     * and the expected boolean result.
     */
//     @ParameterizedTest(name = "compact array edge case – search ''{1}'' → expected {2}")
//     @MethodSource("edgeCaseProvider")
//     void containsValue_objectArray_edgeCases(Object[] internal, Object search, boolean expected) throws Exception {
//         CompactMap<String, Object> map = newTestMap(10);
//         setInternalVal(map, internal);
//         assertEquals(expected, map.containsValue(search));
//     }

    /**
     * A malformed internal array (odd number of elements) should cause the method
     * to throw {@link ArrayIndexOutOfBoundsException} because it blindly accesses
     * {@code entries[i + 1]}.
     */
//     @Test
//     void containsValue_malformedArray_throwsException() throws Exception {
//         CompactMap<String, Object> map = newTestMap(10);
//         // odd length
//         Object[] malformed = new Object[] { "k1", "v1", "k2" };
//         setInternalVal(map, malformed);
//         assertThrows(ArrayIndexOutOfBoundsException.class, () -> map.containsValue("any"));
//     }

    /**
     * If the private {@code val} field is explicitly set to {@code null},
     * the method falls through to the single‑entry path where {@code getLogicalSingleValue()}
     * dereferences the field, resulting in a {@link NullPointerException}.
     */
//     @Test
//     void containsValue_internalValNull_throwsNPE() throws Exception {
//         CompactMap<String, Object> map = newTestMap(10);
//         setInternalVal(map, null);
//         assertThrows(NullPointerException.class, () -> map.containsValue("foo"));
//     }
}