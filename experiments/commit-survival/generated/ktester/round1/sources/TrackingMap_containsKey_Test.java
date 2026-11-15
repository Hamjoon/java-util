package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class TrackingMap_containsKey_Test {

    @Mock
    private Map<String, Integer> // the delegate map used by TrackingMap
    mockMap;

    // system under test
    private TrackingMap<String, Integer> trackingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh TrackingMap instance for each test, wrapping the mocked delegate map.
        trackingMap = new TrackingMap<>(mockMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions with the mock to avoid cross‑test contamination.
        reset(mockMap);
    }

    @Test
    void containsKey_DelegateReturnsTrue_KeyIsTracked() {
        // GIVEN
        String key = "existingKey";
        when(mockMap.containsKey(key)).thenReturn(true);
        // WHEN
        boolean result = trackingMap.containsKey(key);
        // THEN
        assertTrue(result, "containsKey should return true when the delegate map contains the key");
        // Verify delegation
        verify(mockMap, times(1)).containsKey(key);
        // Verify that the key is recorded in the tracking set via the public API
        Set<String> usedKeys = trackingMap.keysUsed();
        assertTrue(usedKeys.contains(key), "keysUsed() should contain the key after containsKey call");
    }

    @Test
    void containsKey_DelegateReturnsFalse_KeyIsTracked() {
        // GIVEN
        String key = "missingKey";
        when(mockMap.containsKey(key)).thenReturn(false);
        // WHEN
        boolean result = trackingMap.containsKey(key);
        // THEN
        assertFalse(result, "containsKey should return false when the delegate map does not contain the key");
        verify(mockMap, times(1)).containsKey(key);
        Set<String> usedKeys = trackingMap.keysUsed();
        assertTrue(usedKeys.contains(key), "keysUsed() should still contain the key even if it was not present");
    }

    @Test
    void constructor_NullMap_ThrowsIllegalArgumentException() {
        // GIVEN / WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TrackingMap<String, Integer>(null));
        assertEquals("Cannot construct a TrackingMap() with null", ex.getMessage(), "Exception message should match the contract");
    }

    @Test
    void containsKey_NullKey_NotPresent_ReturnsFalseAndTracksNull() {
        // GIVEN
        when(mockMap.containsKey(null)).thenReturn(false);
        // WHEN
        boolean result = trackingMap.containsKey(null);
        // THEN
        assertFalse(result, "containsKey(null) should return false when delegate does not contain null");
        verify(mockMap, times(1)).containsKey(null);
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains(null), "keysUsed() must contain null after the call");
    }

    @Test
    void containsKey_NullKey_Present_ReturnsTrueAndTracksNull() {
        // Use a real map to store a null key (mock would need explicit stubbing for null which is fine,
        // but a real map makes the intent clearer).
        Map<String, Integer> realMap = new HashMap<>();
        realMap.put(null, 42);
        TrackingMap<String, Integer> tm = new TrackingMap<>(realMap);
        // WHEN
        boolean result = tm.containsKey(null);
        // THEN
        assertTrue(result, "containsKey(null) should return true when delegate contains a null key");
        Set<String> used = tm.keysUsed();
        assertTrue(used.contains(null), "keysUsed() must contain null after the call");
    }

    @Test
    void containsKey_WrongKeyType_ThrowsClassCastException_ReadKeysUnchanged() {
        // GIVEN
        // delegate returns true, but we care about the cast
        when(mockMap.containsKey(any())).thenReturn(true);
        // WHEN / THEN
        ClassCastException ex = assertThrows(ClassCastException.class, // Integer instead of String
        () -> trackingMap.containsKey(123));
        assertNotNull(ex, "ClassCastException expected due to wrong generic type");
        // readKeys must remain empty because the cast fails before add()
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.isEmpty(), "keysUsed() should remain empty after a ClassCastException");
    }

    @Test
    void containsKey_SameKeyMultipleCalls_IdempotentReadKeys() {
        // GIVEN
        String key = "repeatKey";
        when(mockMap.containsKey(key)).thenReturn(true);
        // WHEN – first call
        boolean first = trackingMap.containsKey(key);
        // WHEN – second call
        boolean second = trackingMap.containsKey(key);
        // THEN
        assertTrue(first);
        assertTrue(second);
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "keysUsed() should contain the key only once");
        assertTrue(used.contains(key));
    }

    @Test
    void containsKey_EmptyMap_ReturnsFalseAndTracksKey() {
        // Use a real empty map instead of a mock to avoid stubbing every possible key.
        TrackingMap<String, Integer> emptyTm = new TrackingMap<>(new HashMap<>());
        String key = "anyKey";
        boolean result = emptyTm.containsKey(key);
        assertFalse(result, "containsKey on an empty map must return false");
        Set<String> used = emptyTm.keysUsed();
        assertTrue(used.contains(key), "keysUsed() must contain the queried key even when absent");
    }

    @Test
    void containsKey_SubclassKey_DifferentNumericType_ReturnsFalseAndTracksLong() {
        // Map<Number, String> with an Integer key
        Map<Number, String> realMap = new HashMap<>();
        realMap.put(5, "five");
        TrackingMap<Number, String> tm = new TrackingMap<>(realMap);
        // Query with a Long having the same numeric value
        boolean result = tm.containsKey(5L);
        assertFalse(result, "containsKey should return false because Integer and Long are not equal");
        Set<Number> used = tm.keysUsed();
        assertTrue(used.contains(5L), "keysUsed() must contain the Long key that was queried");
    }

    @Test
    void containsKey_InternalMapThrowsClassCastException_ReadKeysUnchanged() {
        // TreeMap<Integer, String> will throw ClassCastException when a String key is used.
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(1, "one");
        TrackingMap<Integer, String> tm = new TrackingMap<>(treeMap);
        // WHEN / THEN
        ClassCastException ex = assertThrows(ClassCastException.class, () -> tm.containsKey("stringKey"));
        assertNotNull(ex);
        // readKeys must stay empty because the exception occurs before the add()
        Set<Integer> used = tm.keysUsed();
        assertTrue(used.isEmpty(), "keysUsed() should be empty after internal map ClassCastException");
    }

    @Test
    void containsKey_UnmodifiableReadKeys_ThrowsUnsupportedOperationException() throws Exception {
        // Create a TrackingMap with a normal delegate map.
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        // Replace the private readKeys field with an unmodifiable set via reflection.
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        Set<String> unmod = Collections.unmodifiableSet(new HashSet<>());
        readKeysField.set(tm, unmod);
        // WHEN / THEN
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> tm.containsKey("anyKey"));
        assertNotNull(ex);
    }

    @Test
    void containsKey_NullInternalMap_ThrowsNullPointerException() throws Exception {
        // Construct a normal instance first.
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        // Inject null into the private internalMap field.
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(tm, null);
        // WHEN / THEN
        NullPointerException npe = assertThrows(NullPointerException.class, () -> tm.containsKey("someKey"));
        assertNotNull(npe);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void containsKey_NullKey_VariousDelegateResults_TracksNull(boolean delegateResult) {
        when(mockMap.containsKey(null)).thenReturn(delegateResult);
        boolean result = trackingMap.containsKey(null);
        assertEquals(delegateResult, result, "Result should match delegate's response for null key");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains(null), "keysUsed() must contain null irrespective of delegate result");
    }
}
