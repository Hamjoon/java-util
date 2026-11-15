package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

class TrackingMap_equals_Test {

    private TrackingMap<String, Integer> trackingMap;

    private Map<String, Integer> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh backing map for each test to guarantee isolation.
        backingMap = new HashMap<>();
        // default entries used by many test cases
        backingMap.put("key1", "value1");
        backingMap.put("key2", "value2");
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required.
        trackingMap = null;
        backingMap = null;
    }

    private static Stream<Arguments> equalityScenarios() {
        return Stream.of(// null argument
        Arguments.of(null, false, "When the argument is null, instanceof Map is false, so equals should return false."), // non‑Map argument
        Arguments.of("some string", false, "When the argument is not a Map (e.g., a String), instanceof Map is false, so equals should return false."), // both maps empty
        Arguments.of(new HashMap<String, String>(), true, "Both the internal map and the argument map are empty; equals should return true."), // same content using Map.of
        Arguments.of(Map.of("key1", "value1", "key2", "value2"), true, "The argument map contains exactly the same entries as the internal map; equals should return true."), // same keys, different value
        Arguments.of(Map.of("key1", "value1", "key2", "DIFFERENT"), false, "The argument map has the same keys but a different value for one key; equals should return false."), // completely different map
        Arguments.of(Map.of("key3", "value3"), false, "The argument map has different keys/values from the internal map; equals should return false."), // custom empty Map implementation
        Arguments.of(new AbstractMap<String, String>() {

            @Override
            public Set<Entry<String, String>> entrySet() {
                return Set.of();
            }
        }, true, "A custom empty Map implementation; equals should return true because internal map is also empty after reset."), // null key with value
        Arguments.of(new HashMap<String, String>() {

            {
                put(null, "nullKey");
            }
        }, true, "Both internal map and argument map contain a null key mapped to \"nullKey\"; equals should return true."), // same key with null value
        Arguments.of(new HashMap<String, String>() {

            {
                put("key1", null);
                put("key2", null);
            }
        }, false, "Internal map has non‑null values, argument map has null values; equals should return false."), // internal null vs non‑null value
        Arguments.of(new HashMap<String, String>() {

            {
                put("key1", "value1");
                put("key2", "notnull");
            }
        }, false, "Internal map maps \"key2\" to \"value2\" while argument map maps it to a different non‑null value; equals should return false."), // other TrackingMap with same content
        Arguments.of(new TrackingMap<String, String>() {

            {
                put("key1", "value1");
                put("key2", "value2");
            }
        }, true, "Argument is another TrackingMap with the exact same entry; equals should return true."), // other TrackingMap with different content
        Arguments.of(new TrackingMap<String, String>() {

            {
                put("different", "value");
            }
        }, false, "Argument is a TrackingMap but its content differs; equals should return false."), // TreeMap with same entries
        Arguments.of(new TreeMap<String, String>() {

            {
                put("key1", "value1");
                put("key2", "value2");
            }
        }, true, "Argument is a TreeMap with the same entries; equals should return true regardless of implementation."), // map with extra entry
        Arguments.of(new HashMap<String, String>() {

            {
                put("key1", "value1");
                put("key2", "value2");
                put("extra", "val");
            }
        }, false, "Argument map contains an extra entry; equals should return false."), // map with null key/value while internal map empty
        Arguments.of(new HashMap<String, String>() {

            {
                put(null, null);
            }
        }, false, "Internal map is empty while argument map contains a null key/value; equals should return false."));
    }

    @Test
    void equals_sameBackingMap_returnsTrue() {
        // given
        Map<String, Integer> other = new HashMap<>(backingMap);
        // when
        boolean result = trackingMap.equals(other);
        // then
        assertTrue(result, "TrackingMap should be equal to another Map with the same entries");
    }

    @Test
    void equals_differentMapContent_returnsFalse() {
        // given
        Map<String, Integer> other = new HashMap<>();
        // missing "two"
        other.put("one", 1);
        // when
        boolean result = trackingMap.equals(other);
        // then
        assertFalse(result, "TrackingMap should not be equal to a Map with different content");
    }

    @Test
    void equals_sameInstance_returnsTrue() {
        // given / when
        boolean result = trackingMap.equals(trackingMap);
        // then
        assertTrue(result, "TrackingMap should be equal to itself");
    }

    @Test
    void equals_null_returnsFalse() {
        // when
        boolean result = trackingMap.equals(null);
        // then
        assertFalse(result, "TrackingMap should not be equal to null");
    }

    @Test
    void equals_nonMapObject_returnsFalse() {
        // given
        Object notAMap = "I am not a map";
        // when
        boolean result = trackingMap.equals(notAMap);
        // then
        assertFalse(result, "TrackingMap should not be equal to an object that is not a Map");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("equalityScenarios")
    void equals_variousScenarios(Object other, boolean expected, String description) throws Exception {
        // For the custom empty map case we need the internal map to be empty as well.
        if (other instanceof Map && ((Map<?, ?>) other).isEmpty()) {
            // reset trackingMap to wrap an empty map
            Field field = TrackingMap.class.getDeclaredField("internalMap");
            field.setAccessible(true);
            field.set(trackingMap, new HashMap<String, String>());
        }
        // when
        boolean result = trackingMap.equals(other);
        // then
        assertEquals(expected, result, description);
    }

    @Test
    void equals_internalMapIsNull_throwsNullPointerException() throws Exception {
        // Arrange: set internalMap to null using reflection
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(trackingMap, null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> trackingMap.equals(new HashMap<>()), "When internalMap is null, equals should throw NullPointerException");
    }

    @Test
    void equals_internalMapEqualsThrowsIllegalStateException_propagates() throws Exception {
        // Arrange: create a Map whose equals method always throws IllegalStateException
        Map<String, String> explodingMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, String>> entrySet() {
                return Set.of();
            }

            @Override
            public boolean equals(Object o) {
                throw new IllegalStateException("boom");
            }
        };
        // Replace the internalMap field with the exploding map
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(trackingMap, explodingMap);
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> trackingMap.equals(new HashMap<>()), "Exception from internalMap.equals should propagate");
        assertEquals("boom", ex.getMessage());
    }
}
