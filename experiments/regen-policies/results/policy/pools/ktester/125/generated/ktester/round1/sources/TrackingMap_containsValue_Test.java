package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_containsValue_Test {

    private TrackingMap<String, String> trackingMap;

    private Map<String, String> backingMap;

    /**
     * Helper POJO used in equality tests.
     */
    private static final class Point {

        final int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Point))
                return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /**
     * Object whose {@code equals} deliberately throws a {@link RuntimeException}.
     */
    private static final class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("BadEquals.equals invoked");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh backing map and wrap it with TrackingMap before each test
        backingMap = new HashMap<>();
        backingMap.put("alpha", "A");
        backingMap.put("beta", "B");
        // include a null value for null‑value test
        backingMap.put("gamma", null);
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no cross‑test contamination
        trackingMap = null;
        backingMap = null;
    }

    /**
     * Supplies the majority of normal (non‑exceptional) test scenarios.
     */
    private static Stream<Arguments> provideContainsValueScenarios() {
        return Stream.of(// 1. value present ("apple")
        Arguments.of(mapOf(entry("k1", "apple")), "apple", true, "The wrapped internal map contains the value \"apple\"; containsValue should return true."), // 2. value absent ("banana")
        Arguments.of(mapOf(entry("k1", "apple")), "banana", false, "The wrapped internal map does not contain the value \"banana\"; containsValue should return false."), // 3. null value present
        Arguments.of(mapOf(entry("k1", null)), null, true, "The wrapped internal map contains a null value; containsValue(null) should return true."), // 4. null value absent (empty map)
        Arguments.of(Collections.emptyMap(), null, false, "The wrapped internal map does not contain any null values; containsValue(null) should return false."), // 5. empty map, any value
        Arguments.of(Collections.emptyMap(), "any", false, "The wrapped internal map is empty; any containsValue call should return false."), // 6. duplicate values
        Arguments.of(mapOf(entry("k1", "duplicate"), entry("k2", "duplicate")), "duplicate", true, "The wrapped internal map contains the value \"duplicate\" for multiple keys; containsValue should still return true."), // 7. Integer 42 present
        Arguments.of(mapOf(entry("k1", 42)), 42, true, "The internal map stores the Integer 42 as a value; containsValue(42) should return true."), // 8. Long 42L queried against Integer 42 stored
        Arguments.of(mapOf(entry("k1", 42)), 42L, false, "The internal map stores the Integer 42, but the query is for a Long 42L; different types are not considered equal, so containsValue should return false."), // 9. Custom Point equality
        Arguments.of(mapOf(entry("k1", new Point(1, 2))), new Point(1, 2), true, "A custom Point object with coordinates (1,2) is stored in the map; a different Point instance with the same coordinates is passed to containsValue. Because Point overrides equals, the method should return true."), // 10. StringBuilder does not override equals
        Arguments.of(mapOf(entry("k1", new StringBuilder("test"))), new StringBuilder("test"), false, "The map contains a StringBuilder instance created with \"test\". containsValue is called with a new StringBuilder(\"test\") which is a different object (StringBuilder does not override equals), so the method should return false."), // 11. Double.NaN handling
        Arguments.of(mapOf(entry("k1", Double.NaN)), Double.NaN, true, "The internal map holds Double.NaN as a value; Double.equals treats NaN as equal to NaN, therefore containsValue(Double.NaN) should return true."));
    }

    /**
     * Simple holder for a map entry used by {@link #mapOf(Map.Entry[])}.
     */
    private static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    @Test
    void containsValueReturnsTrueWhenValueExists() {
        assertTrue(trackingMap.containsValue("A"), "containsValue should return true for a value that exists in the map");
        assertTrue(trackingMap.containsValue("B"), "containsValue should return true for another existing value");
    }

    @Test
    void containsValueReturnsFalseWhenValueDoesNotExist() {
        assertFalse(trackingMap.containsValue("Z"), "containsValue should return false for a value that is not present");
        assertFalse(trackingMap.containsValue(""), "containsValue should return false for an empty string not present");
    }

    @Test
    void containsValueDoesNotAffectKeyUsageTracking() {
        // Pre‑condition: no keys have been accessed yet
        assertEquals(0, trackingMap.keysUsed().size(), "Initially no keys should be marked as used");
        // Invoke containsValue – this must not count as a key access
        trackingMap.containsValue("A");
        // Verify that usage tracking is unchanged
        assertEquals(0, trackingMap.keysUsed().size(), "containsValue must not add any keys to the used‑keys set");
    }

    @Test
    void containsValueHandlesNullValueCorrectly() {
        // The map contains a null value for key "gamma"
        assertTrue(trackingMap.containsValue(null), "containsValue should return true when searching for a null value present in the map");
        // Ensure that calling containsValue(null) does not affect key usage tracking
        assertEquals(0, trackingMap.keysUsed().size(), "Calling containsValue(null) must not record any key usage");
    }

    @Test
    void containsValueDoesNotModifyUnderlyingMap() {
        // Capture the size before the call
        int sizeBefore = trackingMap.size();
        // Call containsValue with an existing and a non‑existing value
        trackingMap.containsValue("A");
        trackingMap.containsValue("nonexistent");
        // The underlying map must remain unchanged
        assertEquals(sizeBefore, trackingMap.size(), "containsValue must not modify the map's size or contents");
        assertEquals(backingMap, trackingMap.getWrappedMap(), "The wrapped map should remain the original backing map");
    }

    @Test
    void containsValueWorksWithCaseInsensitiveBackingMap() {
        // Use a CaseInsensitiveMap as the backing map to ensure delegation works correctly
        Map<String, String> ciMap = new CaseInsensitiveMap<>();
        ciMap.put("One", "1");
        ciMap.put("Two", "2");
        TrackingMap<String, String> ciTrackingMap = new TrackingMap<>(ciMap);
        assertTrue(ciTrackingMap.containsValue("1"), "containsValue should correctly find values in a case‑insensitive backing map");
        assertFalse(ciTrackingMap.containsValue("3"), "containsValue should correctly report missing values in a case‑insensitive backing map");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideContainsValueScenarios")
    void containsValueParameterizedTest(Map<String, Object> mapData, Object query, boolean expected, String description) {
        // Build a fresh TrackingMap for each scenario
        TrackingMap<String, Object> map = new TrackingMap<>(new HashMap<>(mapData));
        // Verify that containsValue returns the expected result
        assertEquals(expected, map.containsValue(query), () -> "Scenario failed: " + description);
        // Ensure that containsValue does NOT affect key‑usage tracking
        assertEquals(0, map.keysUsed().size(), "containsValue must not record any key usage");
    }

    @Test
    void containsValueAfterRemovingTheOnlyEntryWithThatValue() {
        // Setup map with a single entry ("key" -> "temp")
        Map<String, Object> map = new HashMap<>();
        map.put("key", "temp");
        TrackingMap<String, Object> tm = new TrackingMap<>(map);
        // Remove the entry
        tm.remove("key");
        // Now the value "temp" is no longer present
        assertFalse(tm.containsValue("temp"), "After removing the entry, containsValue(\"temp\") should return false.");
    }

    @Test
    void containsValueWhenOneOfTwoDuplicateValuesIsRemoved() {
        // Two keys map to the same value "shared"
        Map<String, Object> map = new HashMap<>();
        map.put("k1", "shared");
        map.put("k2", "shared");
        TrackingMap<String, Object> tm = new TrackingMap<>(map);
        // Remove one key
        tm.remove("k1");
        // The other key still maps to "shared"
        assertTrue(tm.containsValue("shared"), "After removing one of the duplicate keys, containsValue(\"shared\") should still return true.");
    }

    @Test
    void containsValueThrowsWhenInternalMapIsNull() {
        // Construct a TrackingMap with a null backing map – the constructor allows it
        TrackingMap<String, Object> tm = new TrackingMap<>(null);
        // Expect a NullPointerException when delegating to the internal map
        assertThrows(NullPointerException.class, () -> tm.containsValue("any"), "Calling containsValue on a TrackingMap with a null internal map should throw NullPointerException");
    }

    @Test
    void containsValuePropagatesRuntimeExceptionFromBadEquals() {
        // Map contains a regular value; the query object throws from equals()
        Map<String, Object> map = new HashMap<>();
        map.put("k1", "someValue");
        TrackingMap<String, Object> tm = new TrackingMap<>(map);
        BadEquals bad = new BadEquals();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> tm.containsValue(bad), "containsValue should propagate RuntimeException thrown from the query object's equals()");
        assertEquals("BadEquals.equals invoked", ex.getMessage(), "Exception message should match the one thrown by BadEquals.equals()");
    }

    /**
     * Creates a mutable HashMap populated with the supplied entries.
     */
    @SafeVarargs
    private static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        Map<K, V> map = new HashMap<>();
        for (Map.Entry<K, V> e : entries) {
            map.put(e.getKey(), e.getValue());
        }
        return map;
    }
}
