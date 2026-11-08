package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

class CompactMap_hashCode_Test {

    /**
     * Simple subclass that allows us to control {@code compactSize} and the single‑value key.
     */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        private final int compactSize;

        private final K singleValueKey;

        TestCompactMap(int compactSize) {
            this(compactSize, null);
        }

        TestCompactMap(int compactSize, K singleValueKey) {
            this.compactSize = compactSize;
            this.singleValueKey = singleValueKey;
        }

        @Override
        protected int compactSize() {
            return compactSize;
        }

        @Override
        protected K getSingleValueKey() {
            return singleValueKey;
        }
    }

    /**
     * Helper key whose {@code hashCode()} throws an {@link IllegalArgumentException}.
     */
    private static final class BadHashKey {

        @Override
        public int hashCode() {
            throw new IllegalArgumentException("Bad hash!");
        }
    }

    /**
     * Helper that forces the private {@code val} field to a given object.
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object newVal) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, newVal);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void hashCode_emptyMap_returnsZero() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertEquals(0, map.hashCode(), "Empty CompactMap should have hashCode 0");
    }

    @Test
    void hashCode_singleEntry_returnsXorOfKeyAndValueHashCodes() {
        // Use default compactSize (80) and no special single‑value key.
        CompactMap<String, Integer> map = new CompactMap<>();
        String key = "key1";
        Integer value = 42;
        map.put(key, value);
        int expected = Objects.hashCode(key) ^ Objects.hashCode(value);
        assertEquals(expected, map.hashCode(), "CompactMap with a single entry should return keyHash ^ valueHash");
    }

    @Test
    void hashCode_multipleEntries_usingObjectArray_returnsSumOfXors() {
        // With default compactSize, 2 entries stay in the Object[] representation.
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        int expected = (Objects.hashCode("a") ^ Objects.hashCode(1)) + (Objects.hashCode("b") ^ Objects.hashCode(2));
        assertEquals(expected, map.hashCode(), "CompactMap with 2 entries should compute sum of (keyHash ^ valueHash) for each pair");
    }

    @Test
    void hashCode_whenSizeExceedsCompactSize_delegatesToUnderlyingMap() {
        // Force compactSize to 2 so that adding 3 entries switches to a regular Map.
        TestCompactMap<String, Integer> map = new TestCompactMap<>(2);
        map.put("x", 10);
        map.put("y", 20);
        // triggers switch to underlying Map
        map.put("z", 30);
        // Build an equivalent HashMap to compute the expected hashCode.
        Map<String, Integer> reference = new HashMap<>();
        reference.put("x", 10);
        reference.put("y", 20);
        reference.put("z", 30);
        assertEquals(reference.hashCode(), map.hashCode(), "When CompactMap exceeds compactSize it should delegate hashCode to the internal Map");
    }

    @Test
    void hashCode_valIsObjectArray_twoPairs_returnsExpectedSum() {
        // ["a",1,"b",2] -> each xor = 96, sum = 192
        CompactMap<String, Integer> map = new CompactMap<>();
        Object[] internal = new Object[] { "a", 1, "b", 2 };
        setInternalVal(map, internal);
        assertEquals(192, map.hashCode(), "Object[] with two pairs should yield hashCode 192");
    }

    @Test
    void hashCode_valIsMap_twoPairs_returnsSameAsMapHashCode() {
        Map<String, Integer> backing = new HashMap<>();
        backing.put("a", 1);
        backing.put("b", 2);
        CompactMap<String, Integer> map = new CompactMap<>();
        setInternalVal(map, backing);
        assertEquals(backing.hashCode(), map.hashCode(), "When val is a Map, hashCode should delegate to that Map");
    }

    @Test
    void hashCode_valIsEmptySentinel_returnsZero() throws Exception {
        CompactMap<String, Integer> map = new CompactMap<>();
        // The EMPTY_MAP sentinel is a private static final field.
        Field emptyField = CompactMap.class.getDeclaredField("EMPTY_MAP");
        emptyField.setAccessible(true);
        Object emptySentinel = emptyField.get(null);
        setInternalVal(map, emptySentinel);
        assertEquals(0, map.hashCode(), "Empty sentinel should produce hashCode 0");
    }

    @Test
    void hashCode_singleEntry_customKeyValue_returnsXor() {
        CompactMap<String, Integer> map = new CompactMap<>();
        // Force the internal representation to the single‑entry case.
        // The easiest way is to use the public API.
        map.put("x", 10);
        int expected = Objects.hashCode("x") ^ Objects.hashCode(10);
        assertEquals(expected, map.hashCode(), "Single entry should return keyHash ^ valueHash (expected 114)");
    }

    @Test
    void hashCode_arraySingleNullKey_returnsKeyHashXorZero() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Object[] internal = new Object[] { "k", null };
        setInternalVal(map, internal);
        // Objects.hashCode(null) == 0
        int expected = Objects.hashCode("k") ^ 0;
        assertEquals(expected, map.hashCode(), "Array with key 'k' and null value should yield correct xor");
    }

    @Test
    void hashCode_arraySingleNullValue_returnsZeroXorValueHash() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Object[] internal = new Object[] { null, 5 };
        setInternalVal(map, internal);
        int expected = 0 ^ Objects.hashCode(5);
        assertEquals(expected, map.hashCode(), "Array with null key and value 5 should yield xor = 5");
    }

    @Test
    void hashCode_arrayMultipleThreePairs_returnsSumOfXors() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Object[] internal = new Object[] { "a", 1, "b", 2, "c", 3 };
        setInternalVal(map, internal);
        // For each pair: Objects.hashCode("a")==97, 1==1 => 96; same for others.
        // 288
        int expected = 96 + 96 + 96;
        assertEquals(288, map.hashCode(), "Array with three pairs should sum to 288");
    }

    @Test
    void hashCode_mapEmpty_returnsZero() {
        Map<String, Integer> backing = new HashMap<>();
        CompactMap<String, Integer> map = new CompactMap<>();
        setInternalVal(map, backing);
        assertEquals(0, map.hashCode(), "Empty backing map should produce hashCode 0");
    }

    @Test
    void hashCode_mapWithNullKeyAndValue_returnsZero() {
        Map<String, Integer> backing = new HashMap<>();
        backing.put(null, null);
        CompactMap<String, Integer> map = new CompactMap<>();
        setInternalVal(map, backing);
        assertEquals(0, map.hashCode(), "Map with a single null key/value should have hashCode 0");
    }

    @Test
    void hashCode_mapMultipleThreePairs_returnsSumOfXors() {
        Map<String, Integer> backing = new HashMap<>();
        backing.put("a", 1);
        backing.put("b", 2);
        backing.put("c", 3);
        CompactMap<String, Integer> map = new CompactMap<>();
        setInternalVal(map, backing);
        assertEquals(288, map.hashCode(), "Map with three pairs should have hashCode 288");
    }

    @Test
    void hashCode_arrayOddLength_throwsArrayIndexOutOfBoundsException() {
        CompactMap<String, Integer> map = new CompactMap<>();
        // Odd length: ["x",1,"y"]
        Object[] malformed = new Object[] { "x", 1, "y" };
        setInternalVal(map, malformed);
        assertThrows(ArrayIndexOutOfBoundsException.class, map::hashCode, "Odd‑length internal array should cause ArrayIndexOutOfBoundsException");
    }

    @Test
    void hashCode_arrayKeyHashThrowsException_propagatesIllegalArgumentException() {
        CompactMap<BadHashKey, Integer> map = new CompactMap<>();
        BadHashKey badKey = new BadHashKey();
        Object[] internal = new Object[] { badKey, 123 };
        setInternalVal(map, internal);
        assertThrows(IllegalArgumentException.class, map::hashCode, "Exception from key.hashCode() should propagate");
    }
}
