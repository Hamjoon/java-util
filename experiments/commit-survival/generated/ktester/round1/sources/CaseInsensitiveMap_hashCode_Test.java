package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

class CaseInsensitiveMap_hashCode_Test {

    private CaseInsensitiveMap<String, Integer> map;

    /**
     * Object whose {@code hashCode()} always returns 7.
     */
    static final class ConstHash {

        @Override
        public int hashCode() {
            return 7;
        }
    }

    /**
     * Object whose {@code hashCode()} throws a {@link RuntimeException}.
     */
    static final class BadKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("BadKey hashCode");
        }
    }

    /**
     * Object whose {@code hashCode()} throws a {@link RuntimeException}.
     */
    static final class BadValue {

        @Override
        public int hashCode() {
            throw new RuntimeException("BadValue hashCode");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor for a fresh map before each test.
        map = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no accidental reuse.
        map = null;
    }

    // Group: Single entry – non‑null key and non‑null value
    static List<Object[]> singleNonNullProvider() {
        return List.of(new Object[][] { { "Alpha", 123 } });
    }

    // Group: Single entry – null key, non‑null value
    static List<Object[]> singleNullKeyProvider() {
        return List.of(new Object[][] { { null, 456 } });
    }

    // Group: Single entry – non‑null key, null value
    static List<Object[]> singleNullValueProvider() {
        return List.of(new Object[][] { { "456", null } });
    }

    /**
     * An empty map should have a hash code of 0.
     */
    @Test
    void hashCode_EmptyMap_ReturnsZero() {
        assertEquals(0, map.hashCode(), "Hash code of an empty map must be 0");
    }

    /**
     * A map with a single entry should compute the hash code as
     * key.hashCode() ^ value.hashCode().
     */
    @Test
    void hashCode_SingleEntry_ComputesCorrectly() {
        map.put("One", 1);
        int expected = "One".hashCode() ^ Integer.valueOf(1).hashCode();
        assertEquals(expected, map.hashCode(), "Hash code with a single entry does not match expected calculation");
    }

    /**
     * A map with multiple entries, including null key/value, should sum the
     * xor of each key/value hash code.
     */
    @Test
    void hashCode_MultipleEntriesIncludingNulls_ComputesCorrectly() {
        // Populate the map
        // null key
        map.put(null, 100);
        // null value
        map.put("nullValue", null);
        map.put("A", 10);
        map.put("b", 20);
        map.put("C", 30);
        // Manually compute the expected hash code using the same algorithm as the class.
        int expected = 0;
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            Object key = e.getKey();
            int hKey = (key == null) ? 0 : key.hashCode();
            Object value = e.getValue();
            int hValue = (value == null) ? 0 : value.hashCode();
            expected += hKey ^ hValue;
        }
        assertEquals(expected, map.hashCode(), "Hash code with multiple entries (including nulls) does not match expected calculation");
    }

    /**
     * The hashCode should be stable – repeated calls must return the same value
     * as long as the map is not modified.
     */
    @Test
    void hashCode_ConsistencyAcrossCalls() {
        map.put("KeyOne", 1);
        map.put("KeyTwo", 2);
        int first = map.hashCode();
        int second = map.hashCode();
        assertEquals(first, second, "Hash code should be consistent across multiple invocations");
    }

    /**
     * Verify that case‑insensitive handling of string keys does not affect the
     * hash code calculation – the original key case is retained.
     */
    @Test
    void hashCode_CaseInsensitiveKey_DoesNotAffectHash() {
        map.put("TestKey", 123);
        // Retrieve using a different case; the underlying entry remains the same.
        assertEquals(123, map.get("testkey"));
        // Expected hash uses the original stored key ("TestKey").
        int expected = "TestKey".hashCode() ^ Integer.valueOf(123).hashCode();
        assertEquals(expected, map.hashCode(), "Hash code should be based on the original key case, not the lookup case");
    }

    @Test
    void hashCode_NullUnderlyingMap_ThrowsNPE() throws Exception {
        // Use reflection to set the private backing map to null.
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(map, null);
        assertThrows(NullPointerException.class, map::hashCode, "Calling hashCode() when the backing map is null should throw NPE");
    }

    @Test
    void hashCode_KeyHashCodeThrowsException() {
        map.put("goodKey", 1);
        // Insert a bad key via reflection because put() would call hashCode on the key for case‑insensitivity.
        // We'll directly manipulate the internal map.
        try {
            Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Object, Object> internal = (Map<Object, Object>) mapField.get(map);
            internal.put(new BadKey(), 42);
        } catch (Exception e) {
            fail("Reflection setup failed: " + e);
        }
        assertThrows(RuntimeException.class, map::hashCode, "hashCode() should propagate exception from a key's hashCode()");
    }

    @Test
    void hashCode_ValueHashCodeThrowsException() {
        map.put("goodKey", 1);
        try {
            Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Object, Object> internal = (Map<Object, Object>) mapField.get(map);
            internal.put("badValueKey", new BadValue());
        } catch (Exception e) {
            fail("Reflection setup failed: " + e);
        }
        assertThrows(RuntimeException.class, map::hashCode, "hashCode() should propagate exception from a value's hashCode()");
    }

    @Test
    void hashCode_XORCancellation_SingleEntryZeroResult() {
        // Both key and value are Integer 42 (auto‑boxed)
        map.put("42", 42);
        // 0
        int expected = Integer.valueOf(42).hashCode() ^ Integer.valueOf(42).hashCode();
        assertEquals(0, expected, "Pre‑condition check: XOR of identical hashes must be 0");
        assertEquals(0, map.hashCode(), "Map with key and value having identical hash codes should yield hashCode 0");
    }

    @Test
    void hashCode_XORCancellation_TwoEntriesZeroResult() {
        map.put("same", "same");
        map.put("99", 99);
        // each entry contributes 0
        assertEquals(0, map.hashCode(), "Two entries where each key.hashCode() == value.hashCode() should result in 0");
    }

    @Test
    void hashCode_CaseInsensitiveDuplicateKey_OverridesFirst() {
        map.put("Key", 1);
        // should overwrite previous entry
        map.put("key", 2);
        // stored key retains original case ("key")
        int expected = "key".hashCode() ^ Integer.valueOf(2).hashCode();
        assertEquals(expected, map.hashCode(), "After duplicate case‑insensitive key insertion, hashCode should reflect only surviving entry");
    }

    @Test
    void hashCode_CustomObjectConstantHash_SingleEntryZero() {
        map.put("c", new ConstHash());
        // key hash is hash of "c", value hash is 7
        int expected = "c".hashCode() ^ 7;
        // but we also have entry where both key and value are ConstHash
        map.clear();
        // using toString as key to avoid case‑insensitivity issues
        map.put(new ConstHash().toString(), new ConstHash());
        // compute expected manually
        expected = "new ConstHash()".hashCode() ^ 7;
        // Not zero; we need a case where both key and value are ConstHash
        map.clear();
        map.put(new ConstHash().toString(), new ConstHash());
        // Since key is a String, its hash is not 7, so result is non‑zero.
        // Instead directly manipulate internal map:
        try {
            Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Object, Object> internal = (Map<Object, Object>) mapField.get(map);
            internal.clear();
            internal.put(new ConstHash(), new ConstHash());
        } catch (Exception e) {
            fail("Reflection setup failed: " + e);
        }
        assertEquals(0, map.hashCode(), "Map with ConstHash key and value (both hash 7) should yield hashCode 0");
    }

    @Test
    void hashCode_CustomObjectConstantHash_MixedEntries() {
        map.clear();
        try {
            Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
            mapField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Object, Object> internal = (Map<Object, Object>) mapField.get(map);
            // 7 ^ 7 = 0
            internal.put(new ConstHash(), new ConstHash());
            // 7 ^ 123.hashCode()
            internal.put(new ConstHash(), 123);
            // "abc".hashCode() ^ 7
            internal.put("abc", new ConstHash());
        } catch (Exception e) {
            fail("Reflection setup failed: " + e);
        }
        int expected = 0 + (7 ^ Integer.valueOf(123).hashCode()) + ("abc".hashCode() ^ 7);
        assertEquals(expected, map.hashCode(), "Mixed constant‑hash objects should contribute their hashCodes correctly");
    }

    @Test
    void hashCode_ConsistencyAcrossRepeatedCalls() {
        map.put("A", 10);
        map.put("B", 20);
        map.put(null, 30);
        int first = map.hashCode();
        int second = map.hashCode();
        assertEquals(first, second, "hashCode must be stable across multiple invocations without modification");
    }

    @Test
    void hashCode_ChangesAfterMutation() {
        map.put("x", 1);
        map.put("y", 2);
        int before = map.hashCode();
        map.put("z", 3);
        int after = map.hashCode();
        assertNotEquals(before, after, "hashCode should change after the map is mutated");
    }

    @Test
    void hashCode_ConcurrentModification_ThrowsCME() throws Exception {
        // Use a non‑concurrent backing map (HashMap) via reflection.
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(map, new HashMap<>());
        map.put("k1", 1);
        map.put("k2", 2);
        // Start a thread that modifies the map while hashCode() is iterating.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try {
                // Small delay to increase chance of overlap.
                Thread.sleep(10);
                map.put("k3", 3);
            } catch (InterruptedException ignored) {
            }
        });
        // The main thread calls hashCode which should detect the concurrent change.
        assertThrows(ConcurrentModificationException.class, () -> {
            // Loop many times to increase likelihood of hitting the modification window.
            for (int i = 0; i < 1000; i++) {
                map.hashCode();
            }
        }, "Concurrent modification during hashCode iteration should throw CME");
        // ensure thread finishes
        future.get();
        executor.shutdownNow();
    }

    @ParameterizedTest(name = "singleNonNull[{0}]")
    @MethodSource("singleNonNullProvider")
    void hashCode_SingleEntry_NonNullKeyAndValue(String key, Integer value) {
        map.put(key, value);
        int expected = key.hashCode() ^ value.hashCode();
        assertEquals(expected, map.hashCode(), "Hash code for single non‑null entry should be key.hashCode() ^ value.hashCode()");
    }

    @ParameterizedTest(name = "singleNullKey[{0}]")
    @MethodSource("singleNullKeyProvider")
    void hashCode_SingleEntry_NullKey(Object key, Integer value) {
        map.put((String) key, value);
        int expected = 0 ^ value.hashCode();
        assertEquals(expected, map.hashCode(), "Hash code when key is null should be value.hashCode()");
    }

    @ParameterizedTest(name = "singleNullValue[{0}]")
    @MethodSource("singleNullValueProvider")
    void hashCode_SingleEntry_NullValue(String key, Object value) {
        map.put(key, (Integer) value);
        int expected = key.hashCode() ^ 0;
        assertEquals(expected, map.hashCode(), "Hash code when value is null should be key.hashCode()");
    }

    // Group: Multiple entries – mixed nulls
    @Test
    void hashCode_MultipleEntries_MixedNulls() {
        map.put(null, null);
        map.put("X", null);
        map.put(null, 99);
        // store as Integer to keep type consistency
        map.put("Y", "Z".hashCode());
        // Since map is <String,Integer>, we need to store Integer values:
        map.put("Y", (int) "Z".hashCode());
        int expected = // null/null
        0 + ("X".hashCode() ^ 0) + (0 ^ Integer.valueOf(99).hashCode()) + ("Y".hashCode() ^ Integer.valueOf("Z".hashCode()).hashCode());
        assertEquals(expected, map.hashCode(), "Hash code for mixed null entries should match manual calculation");
    }

    // Group: Consistency with Map.entrySet() iteration order
    @Test
    void hashCode_OrderIndependence() {
        // Use a LinkedHashMap to enforce insertion order, but hashCode should ignore order.
        map.put("a", 1);
        map.put("b", 2);
        int expected = "a".hashCode() ^ Integer.valueOf(1).hashCode() + "b".hashCode() ^ Integer.valueOf(2).hashCode();
        assertEquals(expected, map.hashCode(), "Hash code must be order‑independent and equal to sum of XORs");
    }
}
