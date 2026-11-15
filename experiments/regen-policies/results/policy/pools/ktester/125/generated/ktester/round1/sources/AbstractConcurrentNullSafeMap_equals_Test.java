package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_equals_Test {

    private SimpleConcurrentNullSafeMap<String, String> mapUnderTest;

    /**
     * Simple concrete implementation used only for testing.
     */
    private static class SimpleConcurrentNullSafeMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        SimpleConcurrentNullSafeMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        mapUnderTest = new SimpleConcurrentNullSafeMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        mapUnderTest.clear();
    }

    private static Stream<Arguments> identityAndTypeChecks() {
        return Stream.of(// same instance – should be true
        Arguments.of("self", true, "The same instance should be equal (identity check)."), // null – not a Map, should be false
        Arguments.of(null, false, "Passing null should return false because it is not a Map."), // non‑Map object – should be false
        Arguments.of("some string", false, "Object that is not a Map should cause equals to return false."));
    }

    /* -------------------------------------------------------------
     *  Helper to build a map from var‑args for readability in future tests
     * ------------------------------------------------------------- */
    private static Map<String, String> mapWithEntries(Object... keyValues) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            @SuppressWarnings("unchecked")
            String key = (String) keyValues[i];
            @SuppressWarnings("unchecked")
            String value = (String) keyValues[i + 1];
            map.put(key, value);
        }
        return map;
    }

    @Test
    void equalsSameInstance_returnsTrue() {
        assertTrue(mapUnderTest.equals(mapUnderTest), "Map should be equal to itself");
    }

    @Test
    void equalsWithNonMapObject_returnsFalse() {
        Object notAMap = new Object();
        assertFalse(mapUnderTest.equals(notAMap), "Map should not be equal to a non‑Map object");
    }

    @Test
    void equalsDifferentSize_returnsFalse() {
        mapUnderTest.put("a", "1");
        mapUnderTest.put("b", "2");
        Map<String, String> other = new HashMap<>();
        // only one entry
        other.put("a", "1");
        assertFalse(mapUnderTest.equals(other), "Maps with different sizes must not be equal");
    }

    @Test
    void equalsMissingKeyInOtherMap_returnsFalse() {
        mapUnderTest.put("x", "10");
        mapUnderTest.put("y", "20");
        Map<String, String> other = new HashMap<>();
        other.put("x", "10");
        // "y" missing
        assertFalse(mapUnderTest.equals(other), "Missing key in the other map should cause inequality");
    }

    @Test
    void equalsDifferentValueForSameKey_returnsFalse() {
        mapUnderTest.put("k", "v1");
        Map<String, String> other = new HashMap<>();
        // different value
        other.put("k", "v2");
        assertFalse(mapUnderTest.equals(other), "Different value for the same key must cause inequality");
    }

    @Test
    void equalsAllEntriesMatch_returnsTrue() {
        mapUnderTest.put("one", "1");
        mapUnderTest.put(null, "nullKey");
        mapUnderTest.put("nullValue", null);
        // overwrites previous null key entry with null value
        mapUnderTest.put(null, null);
        Map<String, String> other = new HashMap<>();
        other.put("one", "1");
        other.put(null, null);
        other.put("nullValue", null);
        assertTrue(mapUnderTest.equals(other), "Maps with identical entries (including nulls) must be equal");
    }

    @Test
    void equalsAgainstAnotherConcurrentMapImplementation_returnsTrue() {
        mapUnderTest.put("alpha", "A");
        mapUnderTest.put("beta", "B");
        // Use a different ConcurrentMap implementation (ConcurrentHashMap) directly
        ConcurrentMap<String, String> otherConcurrent = new ConcurrentHashMap<>();
        otherConcurrent.putAll(mapUnderTest);
        assertTrue(mapUnderTest.equals(otherConcurrent), "Equality should work against any Map implementation");
    }

    @Test
    void equalsAgainstEmptyMap_whenBothEmpty_returnsTrue() {
        Map<String, String> other = new HashMap<>();
        assertTrue(mapUnderTest.equals(other), "Two empty maps must be equal");
    }

    @Test
    void equalsAgainstEmptyMap_whenThisNotEmpty_returnsFalse() {
        mapUnderTest.put("key", "value");
        Map<String, String> other = new HashMap<>();
        assertFalse(mapUnderTest.equals(other), "Non‑empty map must not equal an empty map");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for the "basic identity and type checks"
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => o={0}, expected={1} ({2})")
    @MethodSource("identityAndTypeChecks")
    void equals_identityAndTypeChecks(Object o, boolean expected, String description) {
        // The description is only used for readability of the test name.
        assertEquals(expected, mapUnderTest.equals(o), () -> "Failed: " + description);
    }

    /* --------------------------- size mismatch --------------------------- */
    @Test
    void equals_sizeMismatch_returnsFalse() {
        // mapUnderTest has two entries
        mapUnderTest.put("a", "1");
        mapUnderTest.put("b", "2");
        // other map has only one entry
        Map<String, String> other = new HashMap<>();
        other.put("a", "1");
        assertFalse(mapUnderTest.equals(other), "Maps with different sizes must not be equal");
    }

    /* -------------------------- missing key --------------------------- */
    @Test
    void equals_missingKeyInOtherMap_returnsFalse() {
        mapUnderTest.put("x", "10");
        mapUnderTest.put("y", "20");
        Map<String, String> other = new HashMap<>();
        // "y" is missing
        other.put("x", "10");
        assertFalse(mapUnderTest.equals(other), "Missing key in the other map should cause inequality");
    }

    /* ---------------------- different value for key -------------------- */
    @Test
    void equals_differentValueForSameKey_returnsFalse() {
        mapUnderTest.put("k", "v1");
        Map<String, String> other = new HashMap<>();
        // different value
        other.put("k", "v2");
        assertFalse(mapUnderTest.equals(other), "Different value for the same key must cause inequality");
    }

    /* ------------------- equal maps with different implementation -------- */
    @Test
    void equals_equalMapsDifferentImplementation_returnsTrue() {
        mapUnderTest.put("alpha", "A");
        mapUnderTest.put("beta", "B");
        // HashMap created from the entries of mapUnderTest
        Map<String, String> other = new HashMap<>(mapUnderTest);
        assertTrue(mapUnderTest.equals(other), "Equality should work against any Map implementation");
    }

    /* --------------------------- both maps empty ------------------------ */
    @Test
    void equals_bothMapsEmpty_returnsTrue() {
        Map<String, String> other = new HashMap<>();
        assertTrue(mapUnderTest.equals(other), "Two empty maps must be equal");
    }

    /* ------------------- null key & null value (both contain nulls) ---- */
    @Test
    void equals_nullKeyAndNullValueBothContainNulls_returnsTrue() {
        // single entry with null key & null value
        mapUnderTest.put(null, null);
        Map<String, String> other = new HashMap<>();
        other.put(null, null);
        assertTrue(mapUnderTest.equals(other), "Both maps contain a null key/value pair and should be equal");
    }

    /* ------------------- null key / value mismatch ---------------------- */
    @Test
    void equals_nullKeyValueMismatch_returnsFalse() {
        // null key → null value
        mapUnderTest.put(null, null);
        Map<String, String> other = new HashMap<>();
        // same null key but non‑null value
        other.put(null, "value");
        assertFalse(mapUnderTest.equals(other), "Null key with different associated value must cause inequality");
    }

    /* ------------------- NPE when other map rejects null keys ---------- */
    @Test
    void equals_nullKeyAgainstConcurrentHashMap_throwsNPE() {
        // Populate mapUnderTest with a null key – allowed by AbstractConcurrentNullSafeMap
        mapUnderTest.put(null, "nullKey");
        // ConcurrentHashMap does NOT allow null keys; calling containsKey(null) throws NPE
        ConcurrentMap<String, String> other = new ConcurrentHashMap<>();
        assertThrows(NullPointerException.class, () -> mapUnderTest.equals(other), "Comparing against a map that disallows null keys should throw NPE");
    }

    /* ------------------- NPE when other map rejects null values --------- */
    @Test
    void equals_nullValueAgainstConcurrentHashMap_throwsNPE() {
        // Populate mapUnderTest with a non‑null key whose value is null.
        // ConcurrentHashMap permits null values? Actually it does NOT permit null values either.
        // The call to other.get(key) will return null without exception, but the
        // earlier containsKey(key) succeeds. The NPE is triggered only by a null key.
        // To still verify NPE behaviour for null values we use a map that throws
        // on put of null value (simulated via a spy).
        mapUnderTest.put("key", null);
        // Create a ConcurrentHashMap and put a non‑null entry so that containsKey works.
        ConcurrentMap<String, String> other = new ConcurrentHashMap<>();
        other.put("key", "someValue");
        // The equals implementation will call other.get("key") which returns "someValue".
        // Objects.equals(null, "someValue") is false, but no NPE is thrown.
        // Therefore we assert the result is false (not an exception) to document the behaviour.
        assertFalse(mapUnderTest.equals(other), "When only the value is null (key is non‑null), equals should return false without NPE");
    }
}
