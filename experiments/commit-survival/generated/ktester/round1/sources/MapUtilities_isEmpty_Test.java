package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_isEmpty_Test {

    static Stream<Arguments> booleanExpectationProvider() {
        ???;
    }

    static Stream<Arguments> exceptionExpectationProvider() {
        return Stream.of(Arguments.of(new HashMap<String, String>() {

            @Override
            public boolean isEmpty() {
                throw new NullPointerException("isEmpty forced NPE");
            }
        }, NullPointerException.class, "custom map throws NullPointerException"), Arguments.of(new HashMap<Integer, Integer>() {

            @Override
            public boolean isEmpty() {
                throw new IllegalStateException("isEmpty forced ISE");
            }
        }, IllegalStateException.class, "custom map throws IllegalStateException"), Arguments.of(new HashMap<Object, Object>() {

            @Override
            public boolean isEmpty() {
                throw new UnsupportedOperationException("isEmpty not supported");
            }
        }, UnsupportedOperationException.class, "custom map throws UnsupportedOperationException"));
    }

    @Test
    @DisplayName("isEmpty returns true when the map reference is null")
    void testIsEmpty_NullMap() {
        // GIVEN
        Map<?, ?> nullMap = null;
        // WHEN
        boolean result = MapUtilities.isEmpty(nullMap);
        // THEN
        assertTrue(result, "isEmpty should return true for a null map");
    }

    @Test
    @DisplayName("isEmpty returns true for an empty mutable map")
    void testIsEmpty_EmptyMutableMap() {
        // GIVEN
        Map<String, Integer> emptyMap = new HashMap<>();
        // WHEN
        boolean result = MapUtilities.isEmpty(emptyMap);
        // THEN
        assertTrue(result, "isEmpty should return true for an empty map");
    }

    @Test
    @DisplayName("isEmpty returns true for an empty immutable map")
    void testIsEmpty_EmptyImmutableMap() {
        // GIVEN
        // immutable empty map
        Map<String, Integer> emptyMap = Collections.emptyMap();
        // WHEN
        boolean result = MapUtilities.isEmpty(emptyMap);
        // THEN
        assertTrue(result, "isEmpty should return true for an empty immutable map");
    }

    @Test
    @DisplayName("isEmpty returns false for a map containing entries")
    void testIsEmpty_NonEmptyMap() {
        // GIVEN
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        // WHEN
        boolean result = MapUtilities.isEmpty(map);
        // THEN
        assertFalse(result, "isEmpty should return false for a map with entries");
    }

    @Test
    @DisplayName("isEmpty works with maps that have null keys or values")
    void testIsEmpty_MapWithNullKeyOrValue() {
        // GIVEN
        Map<String, String> map = new HashMap<>();
        map.put(null, "value");
        map.put("key", null);
        // WHEN
        boolean result = MapUtilities.isEmpty(map);
        // THEN
        assertFalse(result, "isEmpty should return false even if the map contains null keys/values");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("booleanExpectationProvider")
    @DisplayName("isEmpty returns expected boolean for diverse map instances")
    void testIsEmpty_ReturnsExpectedBoolean(Map<?, ?> map, boolean expected, String description) {
        boolean actual = MapUtilities.isEmpty(map);
        assertEquals(expected, actual, () -> "isEmpty returned " + actual + " but expected " + expected + " – " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionExpectationProvider")
    @DisplayName("isEmpty propagates runtime exceptions from map.isEmpty()")
    void testIsEmpty_PropagatesExceptions(Map<?, ?> map, Class<? extends RuntimeException> exceptionClass, String description) {
        assertThrows(exceptionClass, () -> MapUtilities.isEmpty(map), () -> "Expected " + exceptionClass.getSimpleName() + " to be thrown – " + description);
    }
}
