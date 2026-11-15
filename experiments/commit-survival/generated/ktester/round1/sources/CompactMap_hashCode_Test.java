package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class CompactMap_hashCode_Test {

    /**
     * A CompactMap with a very small {@code compactSize} to force map‑backing.
     */
    private static class SmallCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            // after 2 entries the map switches to a backing Map
            return 2;
        }

        @Override
        protected K getSingleValueKey() {
            // default behaviour – not used in these tests
            return null;
        }
    }

    /**
     * A CompactMap that uses the default {@code compactSize} (80).
     */
    private static class DefaultCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected K getSingleValueKey() {
            return null;
        }
    }

    /**
     * Subclass that makes the protected hash helpers public for test calculations.
     */
    private static class TestableCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected K getSingleValueKey() {
            // default behaviour – not used in these tests
            return null;
        }

        /**
         * Expose the protected helpers for expected‑value calculation.
         */
        public int keyHash(Object key) {
            return computeKeyHashCode(key);
        }

        public int valueHash(Object value) {
            return computeValueHashCode(value);
        }

        /**
         * Helper to set the private {@code val} field via reflection.
         */
        public void setInternalVal(Object newVal) {
            try {
                Field f = CompactMap.class.getDeclaredField("val");
                f.setAccessible(true);
                f.set(this, newVal);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Helper to build an Object[] that mimics the internal array representation.
     */
    private static Object[] arrayFrom(Object... elements) {
        return elements;
    }

    private static Stream<Arguments> arrayBackedProvider() {
        return Stream.of(// multiple entries, including nulls
        Arguments.of(arrayFrom("key1", "value1", "key2", null, null, "value3"), "multiple entries with nulls"), // exactly one entry (length == 2)
        Arguments.of(arrayFrom("singleKey", "singleValue"), "single entry array"), // all nulls
        Arguments.of(arrayFrom(null, null, null, null), "all null entries"));
    }

    @Test
    void hashCode_emptyMap_returnsZero() {
        TestableCompactMap<String, Integer> map = new TestableCompactMap<>();
        // No puts – internal val stays as EMPTY_MAP sentinel
        assertEquals(0, map.hashCode(), "hashCode() of an empty CompactMap should be 0");
    }

    @Test
    void hashCode_singleEntry_returnsKeyHashXorValueHash() {
        CompactMap<String, Integer> map = new DefaultCompactMap<>();
        String key = "one";
        Integer value = 42;
        map.put(key, value);
        int expected = Objects.hashCode(key) ^ Objects.hashCode(value);
        Assertions.assertEquals(expected, map.hashCode(), "hashCode() with a single entry should be keyHash ^ valueHash");
    }

    @Test
    void hashCode_arrayBackedMultipleEntries_returnsSumOfXors() {
        CompactMap<String, Integer> map = new DefaultCompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        // still below default compactSize (80) → array‑backed
        map.put("c", 3);
        int expected = 0;
        expected += Objects.hashCode("a") ^ Objects.hashCode(1);
        expected += Objects.hashCode("b") ^ Objects.hashCode(2);
        expected += Objects.hashCode("c") ^ Objects.hashCode(3);
        Assertions.assertEquals(expected, map.hashCode(), "hashCode() for array‑backed entries should be the sum of (keyHash ^ valueHash)");
    }

    @Test
    void hashCode_mapBackedEntries_delegatesToBackingMapHashCode() {
        CompactMap<String, Integer> map = new SmallCompactMap<>();
        map.put("x", 10);
        map.put("y", 20);
        // exceeds compactSize (2) → switches to a backing Map
        map.put("z", 30);
        // Build the expected backing map manually – CompactMap uses HashMap by default.
        Map<String, Integer> expectedBacking = new HashMap<>();
        expectedBacking.put("x", 10);
        expectedBacking.put("y", 20);
        expectedBacking.put("z", 30);
        Assertions.assertEquals(expectedBacking.hashCode(), map.hashCode(), "When CompactMap switches to a backing Map, hashCode() should delegate to that Map");
    }

    @ParameterizedTest(name = "[{index}] Array‑backed: {1}")
    @MethodSource("arrayBackedProvider")
    void hashCode_arrayBackedVarious(Object[] internalArray, String description) {
        TestableCompactMap<String, String> map = new TestableCompactMap<>();
        map.setInternalVal(internalArray);
        // Compute expected hash: sum of (keyHash ^ valueHash) for each pair
        int expected = 0;
        for (int i = 0; i < internalArray.length; i += 2) {
            Object key = internalArray[i];
            Object value = internalArray[i + 1];
            expected += map.keyHash(key) ^ map.valueHash(value);
        }
        assertEquals(expected, map.hashCode(), "hashCode() for array‑backed representation (" + description + ") should match the summed XORs");
    }

    @Test
    void hashCode_mapBacked_delegatesToMapHashCode() {
        TestableCompactMap<String, Integer> map = new TestableCompactMap<>();
        Map<String, Integer> backing = new HashMap<>();
        backing.put("a", 1);
        backing.put("b", 2);
        // force map‑backed state
        map.setInternalVal(backing);
        assertEquals(backing.hashCode(), map.hashCode(), "When CompactMap uses a backing Map, hashCode() must delegate to that Map");
    }

    @Test
    void hashCode_mapBackedEmptyMap_returnsZero() {
        TestableCompactMap<String, Integer> map = new TestableCompactMap<>();
        Map<String, Integer> empty = new HashMap<>();
        map.setInternalVal(empty);
        assertEquals(0, map.hashCode(), "Empty backing map should yield hashCode() == 0");
    }

    @Test
    void hashCode_singleEntry_normalPath() {
        TestableCompactMap<String, Integer> map = new TestableCompactMap<>();
        // size == 1, will use the single‑entry path
        map.put("onlyKey", 42);
        int expected = map.keyHash("onlyKey") ^ map.valueHash(42);
        assertEquals(expected, map.hashCode(), "Single entry (non‑array) hashCode should be keyHash ^ valueHash");
    }

    @Test
    void hashCode_singleEntry_nullKey() {
        TestableCompactMap<String, Integer> map = new TestableCompactMap<>();
        map.put(null, 99);
        int expected = map.keyHash(null) ^ map.valueHash(99);
        assertEquals(expected, map.hashCode(), "Single entry with null key should XOR 0 (key hash) with value hash");
    }

    @Test
    void hashCode_singleEntry_nullValue() {
        TestableCompactMap<String, Integer> map = new TestableCompactMap<>();
        map.put("presentKey", null);
        int expected = map.keyHash("presentKey") ^ map.valueHash(null);
        assertEquals(expected, map.hashCode(), "Single entry with null value should XOR key hash with 0 (value hash)");
    }

    @Test
    void hashCode_internalValNull_throwsNPE() {
        TestableCompactMap<String, String> map = new TestableCompactMap<>();
        // corrupt internal state
        map.setInternalVal(null);
        assertThrows(NullPointerException.class, map::hashCode, "When internal val is null, hashCode() should throw NPE");
    }

    @Test
    void hashCode_oddLengthArray_throwsAIOOBE_singleElement() {
        TestableCompactMap<String, String> map = new TestableCompactMap<>();
        // length 1 → odd
        map.setInternalVal(arrayFrom("onlyKey"));
        assertThrows(ArrayIndexOutOfBoundsException.class, map::hashCode, "Odd‑length internal array should cause AIOOBE");
    }

    @Test
    void hashCode_oddLengthArray_throwsAIOOBE_threeElements() {
        TestableCompactMap<String, String> map = new TestableCompactMap<>();
        // length 3 → odd
        map.setInternalVal(arrayFrom("k1", "v1", "k2"));
        assertThrows(ArrayIndexOutOfBoundsException.class, map::hashCode, "Odd‑length internal array (3) should cause AIOOBE");
    }

    @Test
    void hashCode_invalidSingleEntryState_throwsNPE() {
        TestableCompactMap<String, String> map = new TestableCompactMap<>();
        // not EMPTY_MAP, not array, not map
        map.setInternalVal("someStringNotMapOrArray");
        assertThrows(NullPointerException.class, map::hashCode, "Invalid internal representation should eventually lead to NPE");
    }
}
