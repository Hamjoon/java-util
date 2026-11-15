package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_remove_Test {

    private TrackingMap<String, String> trackingMap;

    private Map<String, String> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple HashMap as the underlying map for most tests
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
        // a fresh mutable backing map for each test
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        trackingMap = null;
        backingMap = null;
        trackingMap = null;
        backingMap = null;
    }

    /* --------------------------------------------------------------
     *  Helper methods for test data
     * -------------------------------------------------------------- */
    private static Stream<Arguments> existingKeyProvider() {
        return Stream.of(// String key, value
        Arguments.of("alpha", "A"), // Integer key, value
        Arguments.of(42, 100));
    }

    private static Stream<Arguments> nonExistingKeyProvider() {
        return Stream.of(Arguments.of("beta"), Arguments.of(999));
    }

    private static Stream<Arguments> keyOnlyInReadKeysProvider() {
        return Stream.of(Arguments.of("delta"));
    }

    private static Stream<Arguments> keyOnlyInMapProvider() {
        return Stream.of(Arguments.of("epsilon", "E"), Arguments.of(12345, 555));
    }

    private static Stream<Arguments> nullKeyProvider() {
        return Stream.of(Arguments.of(null, "nullVal"));
    }

    private static Stream<Arguments> nullValueProvider() {
        return Stream.of(Arguments.of("zeta"));
    }

    private static Stream<Arguments> emptyMapProvider() {
        return Stream.of(Arguments.of("any"));
    }

    private static Stream<Arguments> unmodifiableMapProvider() {
        return Stream.of(Arguments.of("anyKey"), Arguments.of(null));
    }

    @Test
    void removeExistingKeyReturnsValueAndCleansReadKeys() {
        // Arrange
        backingMap.put("a", "alpha");
        backingMap.put("b", "beta");
        // Access key "a" so it is recorded in readKeys
        assertEquals("alpha", trackingMap.get("a"));
        assertTrue(trackingMap.keysUsed().contains("a"));
        // Act
        String removed = trackingMap.remove("a");
        // Assert
        assertEquals("alpha", removed, "remove should return the previous value");
        assertFalse(trackingMap.containsKey("a"), "Key should no longer be present in the map");
        assertEquals(1, trackingMap.size(), "Map size should be reduced by one");
        assertFalse(trackingMap.keysUsed().contains("a"), "Removed key must also be removed from the set of accessed keys");
    }

    @Test
    void removeNonExistingKeyReturnsNullAndDoesNotAffectReadKeys() {
        // Arrange
        backingMap.put("x", "ex");
        // No prior access to any key
        // Act
        // "y" does not exist
        String removed = trackingMap.remove("y");
        // Assert
        assertNull(removed, "remove should return null for a non‑existent key");
        assertEquals(1, trackingMap.size(), "Map size should remain unchanged");
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed should remain empty when removing a non‑existent key");
    }

    @Test
    void removeDelegatesToUnderlyingMap() {
        // Arrange
        Map<String, String> spyBacking = spy(new HashMap<>());
        spyBacking.put("k", "value");
        TrackingMap<String, String> spyTrackingMap = new TrackingMap<>(spyBacking);
        // Act
        String result = spyTrackingMap.remove("k");
        // Assert
        assertEquals("value", result);
        verify(spyBacking, times(1)).remove("k");
    }

    @Test
    void removeKeyPreviouslyAccessedViaContainsKeyCleansReadKeys() {
        // Arrange
        backingMap.put("p", "pi");
        // Record access via containsKey
        assertTrue(trackingMap.containsKey("p"));
        assertTrue(trackingMap.keysUsed().contains("p"));
        // Act
        String removed = trackingMap.remove("p");
        // Assert
        assertEquals("pi", removed);
        assertFalse(trackingMap.containsKey("p"));
        assertFalse(trackingMap.keysUsed().contains("p"), "Key should be removed from keysUsed after removal");
    }

    @ParameterizedTest(name = "remove existing key ''{0}'' returns value and cleans keysUsed")
    @MethodSource("existingKeyProvider")
    void removeExistingKey(Object key, Object expectedValue) {
        // arrange
        backingMap.put(key, expectedValue);
        // make the key part of the read‑keys set
        assertEquals(expectedValue, trackingMap.get(key));
        assertTrue(trackingMap.keysUsed().contains(key));
        // act
        Object removed = trackingMap.remove(key);
        // assert
        assertEquals(expectedValue, removed, "remove should return the previous value");
        assertFalse(trackingMap.containsKey(key), "key must no longer be present");
        assertFalse(trackingMap.keysUsed().contains(key), "key must be removed from keysUsed");
    }

    @ParameterizedTest(name = "remove non‑existing key ''{0}'' returns null and does not affect keysUsed")
    @MethodSource("nonExistingKeyProvider")
    void removeNonExistingKey(Object key) {
        // arrange
        // ensure map is not empty
        backingMap.put("someOther", "value");
        // no prior access to any key
        // act
        Object removed = trackingMap.remove(key);
        // assert
        assertNull(removed, "remove should return null for missing key");
        assertEquals(1, trackingMap.size(), "size must stay unchanged");
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed must stay empty");
    }

    @ParameterizedTest(name = "remove key present only in read‑keys ''{0}'' returns null and cleans keysUsed")
    @MethodSource("keyOnlyInReadKeysProvider")
    void removeKeyOnlyInReadKeys(Object key) {
        // arrange: put key in map, then remove directly from backing map
        backingMap.put(key, "someValue");
        // access it so it appears in readKeys
        assertEquals("someValue", trackingMap.get(key));
        assertTrue(trackingMap.keysUsed().contains(key));
        // now remove directly from the underlying map to simulate external deletion
        backingMap.remove(key);
        assertFalse(backingMap.containsKey(key));
        // act
        Object removed = trackingMap.remove(key);
        // assert
        assertNull(removed, "remove should return null because underlying map has no entry");
        assertFalse(trackingMap.keysUsed().contains(key), "key must be removed from keysUsed");
    }

    @ParameterizedTest(name = "remove key present only in internal map ''{0}'' returns value, keysUsed unchanged")
    @MethodSource("keyOnlyInMapProvider")
    void removeKeyOnlyInMap(Object key, Object expectedValue) {
        // arrange
        backingMap.put(key, expectedValue);
        // do NOT access the key beforehand
        // act
        Object removed = trackingMap.remove(key);
        // assert
        assertEquals(expectedValue, removed, "remove should return the stored value");
        assertFalse(trackingMap.containsKey(key), "key must be removed from map");
        assertFalse(trackingMap.keysUsed().contains(key), "key was never in keysUsed");
    }

    @ParameterizedTest(name = "remove null key with existing mapping returns value and cleans keysUsed")
    @MethodSource("nullKeyProvider")
    void removeNullKeyWithMapping(Object key, Object expectedValue) {
        // arrange
        backingMap.put(null, expectedValue);
        // optionally access null key to ensure it is tracked
        assertEquals(expectedValue, trackingMap.get(null));
        assertTrue(trackingMap.keysUsed().contains(null));
        // act
        Object removed = trackingMap.remove(null);
        // assert
        assertEquals(expectedValue, removed, "remove should return the value for null key");
        assertFalse(trackingMap.containsKey(null), "null key must be removed");
        assertFalse(trackingMap.keysUsed().contains(null), "null key must be removed from keysUsed");
    }

    @ParameterizedTest(name = "remove key with null value ''{0}'' returns null and cleans keysUsed")
    @MethodSource("nullValueProvider")
    void removeKeyWithNullValue(Object key) {
        // arrange
        backingMap.put(key, null);
        // access to add to readKeys
        assertNull(trackingMap.get(key));
        assertTrue(trackingMap.keysUsed().contains(key));
        // act
        Object removed = trackingMap.remove(key);
        // assert
        assertNull(removed, "remove should return null because stored value is null");
        assertFalse(trackingMap.containsKey(key), "key must be removed");
        assertFalse(trackingMap.keysUsed().contains(key), "key must be removed from keysUsed");
    }

    @ParameterizedTest(name = "remove from empty map key ''{0}'' returns null, map stays empty")
    @MethodSource("emptyMapProvider")
    void removeFromEmptyMap(Object key) {
        // arrange: map is already empty, no readKeys
        // act
        Object removed = trackingMap.remove(key);
        // assert
        assertNull(removed, "remove on empty map must return null");
        assertTrue(trackingMap.isEmpty(), "map should still be empty");
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed must remain empty");
    }

    @ParameterizedTest(name = "remove with unmodifiable internal map key ''{0}'' throws UnsupportedOperationException")
    @MethodSource("unmodifiableMapProvider")
    void removeWithUnmodifiableMap(Object key) {
        // arrange: create an unmodifiable map containing the key
        Map<Object, Object> modifiable = new HashMap<>();
        modifiable.put(key, "value");
        Map<Object, Object> unmodifiable = Collections.unmodifiableMap(modifiable);
        TrackingMap<Object, Object> tm = new TrackingMap<>(unmodifiable);
        // act & assert
        assertThrows(UnsupportedOperationException.class, () -> tm.remove(key), "remove must propagate UnsupportedOperationException from the underlying map");
    }

    @Test
    void removeDelegatesToUnderlyingMap_VerifyInteraction() {
        // arrange a spy backing map to verify delegation
        Map<String, String> spyMap = spy(new HashMap<>());
        spyMap.put("k", "v");
        TrackingMap<String, String> tm = new TrackingMap<>(spyMap);
        // act
        String result = tm.remove("k");
        // assert
        assertEquals("v", result);
        verify(spyMap, times(1)).remove("k");
    }
}
