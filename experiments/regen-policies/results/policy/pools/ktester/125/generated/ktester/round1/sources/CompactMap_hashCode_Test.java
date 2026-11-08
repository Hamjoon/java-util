package com.cedarsoftware.util;

import java.lang.reflect.Field;
// import org.junit.*;
// import static org.junit.Assert.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CompactMap_hashCode_Test {

    /**
     * Simple subclass that lets us control the internal thresholds and hash
     * calculations.
     */
    private static class TestCompactMap extends CompactMap<String, Integer> {

        private final int compactSize;

//         private final Map<String, Integer> backingMap;

        TestCompactMap(int compactSize) {
            this.compactSize = compactSize;
//             this.backingMap = new LinkedHashMap<>();
        }

        @Override
        protected int compactSize() {
            return compactSize;
        }

//         @Override
//         protected Map<String, Integer> getNewMap() {
//             return backingMap;
//         }

        /* Force deterministic hash codes so we can predict the result. */
        @Override
        protected int computeKeyHashCode(Object key) {
            // arbitrary constant for any key
            return 7;
        }

        @Override
        protected int computeValueHashCode(Object value) {
            // arbitrary constant for any value
            return 11;
        }
    }

    /**
     * Helper to inject a value into the private {@code val} field.
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object newVal) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        f.set(map, newVal);
    }

    /* -------------------------------------------------------------
     *  Object[] storage tests
     * ------------------------------------------------------------- */
//     private static Stream<Arguments> objectArrayProvider() {
//         return Stream.of(Arguments.of("two entries", new Object[] { "a", 1, "b", 2 }, (Objects.hashCode("a") ^ Objects.hashCode(1)) + (Objects.hashCode("b") ^ Objects.hashCode(2))), Arguments.of("empty array", new Object[0], 0), Arguments.of("null key/value mixed", new Object[] { null, "x", "y", null }, (Objects.hashCode(null) ^ Objects.hashCode("x")) + (Objects.hashCode("y") ^ Objects.hashCode(null))));
//     }

    /* -------------------------------------------------------------
     *  Underlying Map storage tests
     * ------------------------------------------------------------- */
//     private static Stream<Arguments> backingMapProvider() {
//         Map<String, Integer> normalMap = new HashMap<>();
//         normalMap.put("x", 10);
//         normalMap.put("y", 20);
//         Map<String, Integer> nullMap = new HashMap<>();
//         nullMap.put(null, 100);
//         nullMap.put("key", null);
//         return Stream.of(Arguments.of("regular map with two entries", normalMap), Arguments.of("map with null key and null value", nullMap));
//     }

    @Test
    void hashCode_emptyMap_returnsZero() {
        // compactSize > 0, map is empty
        TestCompactMap map = new TestCompactMap(3);
        Assertions.assertEquals(0, map.hashCode(), "Empty CompactMap should have hashCode 0");
    }

    @Test
    void hashCode_singleEntry_returnsXorOfKeyAndValueHashCodes() {
        TestCompactMap map = new TestCompactMap(3);
        map.put("key", 42);
        // computeKeyHashCode ^ computeValueHashCode
        int expected = 7 ^ 11;
        Assertions.assertEquals(expected, map.hashCode(), "Single‑entry map should return XOR of key/value hash codes");
    }

    @Test
    void hashCode_compactArray_returnsSumOfEntryXors() {
        // keep size below compactSize so an array is used
        TestCompactMap map = new TestCompactMap(5);
        map.put("a", 1);
        map.put("b", 2);
        // Two entries → each contributes (7 ^ 11) = 12, total = 24
        int expected = (7 ^ 11) + (7 ^ 11);
        Assertions.assertEquals(expected, map.hashCode(), "Compact array storage should sum the XOR of each entry's hashes");
    }

    @Test
    void hashCode_backingMap_returnsBackingMapHashCode() {
        // compactSize = 2, inserting 3 entries forces a transition to a backing Map
        TestCompactMap map = new TestCompactMap(2);
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);
        // Map's contract: hashCode == entrySet().hashCode()
        int expected = map.entrySet().hashCode();
        Assertions.assertEquals(expected, map.hashCode(), "When the internal storage is a backing Map, hashCode should delegate to that map");
    }

    @ParameterizedTest(name = "[{index}] Object[] case: {0}")
    @MethodSource("objectArrayProvider")
    void hashCode_objectArray_branches(String description, Object[] internalArray, int expected) throws Exception {
        CompactMap<String, Integer> map = new CompactMap<>();
        setInternalVal(map, internalArray);
        Assertions.assertEquals(expected, map.hashCode(), "hashCode should follow the Object[] branch for case: " + description);
    }

    @Test
    void hashCode_objectArray_oddLength_throwsArrayIndexOutOfBoundsException() throws Exception {
        CompactMap<String, Integer> map = new CompactMap<>();
        // length 1 -> missing value
        Object[] oddArray = new Object[] { "onlyKey" };
        setInternalVal(map, oddArray);
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, map::hashCode, "hashCode should throw ArrayIndexOutOfBoundsException for an odd‑length internal array");
    }

//     @ParameterizedTest(name = "[{index}] Backing Map case: {0}")
//     @MethodSource("backingMapProvider")
//     void hashCode_backingMap_delegatesToMap(String description, Map<String, Integer> backing) throws Exception {
//         CompactMap<String, Integer> map = new CompactMap<>();
//         setInternalVal(map, backing);
//         // The contract of Map.hashCode() is the hash of its entrySet()
//         int expected = backing.hashCode();
//         Assertions.assertEquals(expected, map.hashCode(), "hashCode should delegate to the underlying Map for case: " + description);
//     }
}