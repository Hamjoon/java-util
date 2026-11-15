package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_getWrappedMap_Test {

    static Stream<Arguments> provideMapsForReturnWrappedMap() {
        return Stream.of(Arguments.of("HashMap", new HashMap<String, Integer>()), Arguments.of("LinkedHashMap", new LinkedHashMap<String, Integer>()));
    }

    static Stream<Arguments> provideMapsForReturnWrappedMapAfterSet() {
        return Stream.of(Arguments.of("TreeMap", new HashMap<String, Integer>(), new TreeMap<String, Integer>()), Arguments.of("unmodifiable HashMap", new LinkedHashMap<String, Integer>(), Collections.unmodifiableMap(new HashMap<String, Integer>())));
    }

    static Stream<Arguments> provideVariousMapTypes() {
        return Stream.of(Arguments.of("TreeMap", new TreeMap<String, Integer>()), Arguments.of("ConcurrentHashMap", new ConcurrentHashMap<String, Integer>()), Arguments.of("synchronized HashMap", Collections.synchronizedMap(new HashMap<String, Integer>())));
    }

    @Test
    void getWrappedMapReturnsOriginalHashMapInstance() {
        Map<String, Integer> backing = new HashMap<>();
        backing.put("one", 1);
        backing.put("two", 2);
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(backing);
        // The wrapped map should be the exact same instance supplied to the constructor.
        assertSame(backing, trackingMap.getWrappedMap(), "getWrappedMap() must return the original backing map instance");
    }

    @Test
    void getWrappedMapWorksWithCaseInsensitiveMap() {
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
        ciMap.put("Key", "value");
        TrackingMap<String, String> trackingMap = new TrackingMap<>(ciMap);
        assertSame(ciMap, trackingMap.getWrappedMap(), "getWrappedMap() must return the CaseInsensitiveMap instance that was wrapped");
        assertTrue(trackingMap.getWrappedMap() instanceof CaseInsensitiveMap, "Returned map should be an instance of CaseInsensitiveMap");
    }

    @Test
    void constructorRejectsNullBackingMap() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TrackingMap<String, String>(null), "Constructor should throw IllegalArgumentException when passed a null map");
        assertEquals("Cannot construct a TrackingMap() with null", ex.getMessage(), "Exception message should match the contract");
    }

    @Test
    void modificationsThroughTrackingMapAreReflectedInWrappedMap() {
        Map<String, String> backing = new HashMap<>();
        TrackingMap<String, String> trackingMap = new TrackingMap<>(backing);
        trackingMap.put("a", "alpha");
        trackingMap.put("b", "bravo");
        // The backing map should now contain the entries added via TrackingMap.
        assertEquals(2, backing.size(), "Backing map size must reflect puts made through TrackingMap");
        assertEquals("alpha", backing.get("a"));
        assertEquals("bravo", backing.get("b"));
    }

    @Test
    void modificationsDirectlyOnWrappedMapAreVisibleThroughTrackingMap() {
        Map<String, String> backing = new HashMap<>();
        TrackingMap<String, String> trackingMap = new TrackingMap<>(backing);
        // Directly mutate the backing map.
        backing.put("x", "xray");
        backing.put("y", "yankee");
        // TrackingMap should see those entries.
        assertEquals(2, trackingMap.size(), "TrackingMap size must reflect direct changes to the wrapped map");
        assertEquals("xray", trackingMap.get("x"));
        assertEquals("yankee", trackingMap.get("y"));
    }

    @Test
    void getWrappedMapAfterSetWrappedMapReflectsNewBackingMap() {
        Map<String, Integer> first = new HashMap<>();
        first.put("one", 1);
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(first);
        Map<String, Integer> second = new HashMap<>();
        second.put("two", 2);
        trackingMap.setWrappedMap(second);
        // After calling setWrappedMap, getWrappedMap should return the new map.
        assertSame(second, trackingMap.getWrappedMap(), "After setWrappedMap, getWrappedMap must return the newly supplied map");
        // The old map should no longer be visible through TrackingMap.
        assertNull(trackingMap.get("one"));
        assertEquals(2, trackingMap.get("two"));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Constructor with {0}")
    @MethodSource("provideMapsForReturnWrappedMap")
    void returnWrappedMap_ShouldReturnOriginalInstance(String description, Map<String, Integer> backingMap) {
        // Arrange
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(backingMap);
        // Act
        Map<String, Integer> result = trackingMap.getWrappedMap();
        // Assert
        assertSame(backingMap, result, () -> "Expected getWrappedMap() to return the same instance for " + description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] setWrappedMap with {1}")
    @MethodSource("provideMapsForReturnWrappedMapAfterSet")
    void returnWrappedMapAfterSet_ShouldReturnNewInstance(String description, Map<String, Integer> initialMap, Map<String, Integer> newMap) {
        // Arrange
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(initialMap);
        trackingMap.setWrappedMap(newMap);
        // Act
        Map<String, Integer> result = trackingMap.getWrappedMap();
        // Assert
        assertSame(newMap, result, () -> "After setWrappedMap, getWrappedMap should return the new " + description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] Constructor with {0}")
    @MethodSource("provideVariousMapTypes")
    void returnWrappedMap_WithDifferentMapImplementations(String description, Map<String, Integer> map) {
        // Arrange
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(map);
        // Act
        Map<String, Integer> result = trackingMap.getWrappedMap();
        // Assert
        assertSame(map, result, () -> "Expected getWrappedMap() to return the same " + description + " instance");
    }

    // -----------------------------------------------------------------
    @Test
    void returnWrappedMap_AfterClear_ShouldRemainSameInstanceAndBeEmpty() {
        // Arrange
        Map<String, Integer> backing = new HashMap<>();
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(backing);
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        // Act
        // clear via TrackingMap façade
        trackingMap.clear();
        // Assert
        assertSame(backing, trackingMap.getWrappedMap(), "The wrapped map reference must stay unchanged after clear()");
        assertTrue(backing.isEmpty(), "The underlying map should be empty after clear()");
    }

    @Test
    void returnWrappedMap_AfterPutAll_ShouldContainAddedEntries() {
        // Arrange
        Map<String, Integer> backing = new LinkedHashMap<>();
        TrackingMap<String, Integer> trackingMap = new TrackingMap<>(backing);
        Map<String, Integer> toPut = new HashMap<>();
        toPut.put("x", 10);
        toPut.put("y", 20);
        // Act
        trackingMap.putAll(toPut);
        // Assert
        assertSame(backing, trackingMap.getWrappedMap(), "The wrapped map reference must stay unchanged after putAll()");
        assertEquals(2, backing.size(), "Underlying map should contain the entries added via putAll");
        assertEquals(10, backing.get("x"));
        assertEquals(20, backing.get("y"));
    }
}
