package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactCIHashMap_getNewMap_Test {

    /**
     * Simple subclass that makes the protected {@code getNewMap()} method public for testing.
     */
    private static class TestableCompactCIHashMap<K, V> extends CompactCIHashMap<K, V> {

        public Map<K, V> getNewMapPublic() {
            return super.getNewMap();
        }
    }

    /**
     * Subclass that forces an illegal initial capacity (negative) for the backing HashMap.
     */
    private static class NegativeCapacityCompactCIHashMap<K, V> extends CompactCIHashMap<K, V> {

        @Override
        protected int compactSize() {
            // illegal value
            return -1;
        }

        public Map<K, V> getNewMapPublic() {
            return super.getNewMap();
        }
    }

    private static Stream<Arguments> nullValueScenarios() {
        return Stream.of(Arguments.of("keyOne", null), Arguments.of("KEYTWO", null));
    }

    @Test
    void getNewMap_returnsCaseInsensitiveMapAndIsEmpty() {
        // GIVEN
        TestableCompactCIHashMap<String, Integer> map = new TestableCompactCIHashMap<>();
        // WHEN
        Map<String, Integer> newMap = map.getNewMapPublic();
        // THEN
        assertTrue(newMap instanceof CaseInsensitiveMap, "The map returned by getNewMap() should be a CaseInsensitiveMap");
        assertTrue(newMap.isEmpty(), "The newly created map should be empty");
    }

    @Test
    void getNewMap_caseInsensitiveBehavior() {
        // GIVEN
        TestableCompactCIHashMap<String, Integer> map = new TestableCompactCIHashMap<>();
        Map<String, Integer> ciMap = map.getNewMapPublic();
        // WHEN
        ciMap.put("Key", 1);
        // same logical key, different case
        ciMap.put("key", 2);
        // THEN
        assertEquals(1, ciMap.size(), "CaseInsensitiveMap should treat keys differing only by case as the same entry");
        assertEquals(2, ciMap.get("KEY"), "Retrieving with a different case should return the latest value");
        assertEquals(2, ciMap.get("kEy"), "Retrieving with any case variation should return the stored value");
    }

    @Test
    void getNewMap_backingMapIsHashMapWithCorrectInitialCapacity() throws Exception {
        // GIVEN
        TestableCompactCIHashMap<String, Integer> map = new TestableCompactCIHashMap<>();
        // WHEN
        Map<String, Integer> ciMap = map.getNewMapPublic();
        // Access the private field that holds the delegate map inside CaseInsensitiveMap.
        Field delegateField = CaseInsensitiveMap.class.getDeclaredField("map");
        delegateField.setAccessible(true);
        Object delegate = delegateField.get(ciMap);
        // THEN
        assertNotNull(delegate, "Delegate map inside CaseInsensitiveMap must not be null");
        assertTrue(delegate instanceof HashMap, "Delegate map should be an instance of java.util.HashMap");
        // Verify the initial capacity is compactSize()+1.
        // HashMap stores the calculated threshold (capacity * loadFactor) in a private field "threshold".
        // We can compute the expected threshold and compare.
        int expectedCapacity = map.compactSize() + 1;
        // default load factor
        float loadFactor = 0.75f;
        int expectedThreshold = (int) Math.min(expectedCapacity * loadFactor, HashMap.MAXIMUM_CAPACITY + 1);
        Field thresholdField = HashMap.class.getDeclaredField("threshold");
        thresholdField.setAccessible(true);
        int actualThreshold = (int) thresholdField.get(delegate);
        assertEquals(expectedThreshold, actualThreshold, "HashMap threshold (derived from initial capacity) should match compactSize()+1");
    }

    @Test
    void getNewMap_containsKeyIsCaseInsensitive() {
        // GIVEN
        TestableCompactCIHashMap<String, Integer> map = new TestableCompactCIHashMap<>();
        Map<String, Integer> ciMap = map.getNewMapPublic();
        ciMap.put("Alpha", 10);
        // WHEN / THEN
        assertTrue(ciMap.containsKey("alpha"), "containsKey should be case‑insensitive");
        assertTrue(ciMap.containsKey("ALPHA"), "containsKey should be case‑insensitive for any case");
    }

    @Test
    void getNewMap_removeIsCaseInsensitive() {
        // GIVEN
        TestableCompactCIHashMap<String, Integer> map = new TestableCompactCIHashMap<>();
        Map<String, Integer> ciMap = map.getNewMapPublic();
        ciMap.put("Beta", 42);
        // WHEN
        Integer removed = ciMap.remove("bEtA");
        // THEN
        assertEquals(42, removed, "remove should return the associated value");
        assertNull(ciMap.get("Beta"), "After removal, get should return null");
        assertFalse(ciMap.containsKey("beta"), "Map should no longer contain the key");
    }

    @Test
    void getNewMap_clearEmptiesMap() {
        // GIVEN
        TestableCompactCIHashMap<String, Integer> map = new TestableCompactCIHashMap<>();
        Map<String, Integer> ciMap = map.getNewMapPublic();
        ciMap.put("One", 1);
        ciMap.put("Two", 2);
        assertEquals(2, ciMap.size(), "Pre‑condition: map should contain two entries");
        // WHEN
        ciMap.clear();
        // THEN
        assertEquals(0, ciMap.size(), "clear() should empty the map");
        assertTrue(ciMap.isEmpty(), "Map should be empty after clear");
    }

    @Test
    void getNewMap_putAllCopiesEntriesCaseInsensitively() {
        // GIVEN
        TestableCompactCIHashMap<String, Integer> map = new TestableCompactCIHashMap<>();
        Map<String, Integer> ciMap = map.getNewMapPublic();
        Map<String, Integer> source = new HashMap<>();
        source.put("Gamma", 3);
        source.put("delta", 4);
        // WHEN
        ciMap.putAll(source);
        // THEN
        assertEquals(2, ciMap.size(), "All entries should be copied");
        assertEquals(3, ciMap.get("GAMMA"));
        assertEquals(4, ciMap.get("DELTA"));
    }

    @Test
    void getNewMap_putNullKeyThrowsNPE() {
        // GIVEN
        TestableCompactCIHashMap<String, String> map = new TestableCompactCIHashMap<>();
        Map<String, String> ciMap = map.getNewMapPublic();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ciMap.put(null, "value"), "Putting a null key should throw NullPointerException");
    }

    @Test
    void getNewMap_putNullValueIsAllowed() {
        // GIVEN
        TestableCompactCIHashMap<String, String> map = new TestableCompactCIHashMap<>();
        Map<String, String> ciMap = map.getNewMapPublic();
        // WHEN
        ciMap.put("KeyWithNull", null);
        // THEN
        assertTrue(ciMap.containsKey("keywithnull"), "Key should be present case‑insensitively");
        assertNull(ciMap.get("KEYWITHNULL"), "Retrieving the key should return null value");
    }

    @Test
    void getNewMap_negativeCompactSizeThrowsIllegalArgumentException() {
        // GIVEN
        NegativeCapacityCompactCIHashMap<String, Integer> map = new NegativeCapacityCompactCIHashMap<>();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, map::getNewMapPublic, "A negative compactSize() should cause HashMap constructor to throw IllegalArgumentException");
    }

    @ParameterizedTest
    @MethodSource("nullValueScenarios")
    void getNewMap_allowsNullValues(String key, String nullValue) {
        TestableCompactCIHashMap<String, String> map = new TestableCompactCIHashMap<>();
        Map<String, String> ciMap = map.getNewMapPublic();
        ciMap.put(key, nullValue);
        assertTrue(ciMap.containsKey(key.toLowerCase()), "Key should be stored case‑insensitively");
        assertNull(ciMap.get(key), "Stored value should be null");
    }
}
