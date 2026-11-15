package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
class TrackingMap_isEmpty_Test {

    @Mock
    private Map<String, Integer> // mocked internal map
    mockMap;

    // system under test
    private TrackingMap<String, Integer> trackingMap;

    @BeforeEach
    void setupBeforeEach() {
        // By default we create a TrackingMap backed by the mock – tests that need a real map will
        // instantiate their own instance inside the test method.
        trackingMap = new TrackingMap<>(mockMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to keep tests isolated
        reset(mockMap);
    }

    @Test
    void isEmpty_ReturnsTrue_WhenInternalMapIsEmpty() {
        // given
        when(mockMap.isEmpty()).thenReturn(true);
        // when
        boolean result = trackingMap.isEmpty();
        // then
        assertTrue(result, "TrackingMap.isEmpty() should return true when the wrapped map is empty");
        verify(mockMap, times(1)).isEmpty();
    }

    @Test
    void isEmpty_ReturnsFalse_WhenInternalMapIsNotEmpty() {
        // given
        when(mockMap.isEmpty()).thenReturn(false);
        // when
        boolean result = trackingMap.isEmpty();
        // then
        assertFalse(result, "TrackingMap.isEmpty() should return false when the wrapped map is not empty");
        verify(mockMap, times(1)).isEmpty();
    }

    @Test
    void constructor_ThrowsIllegalArgumentException_WhenProvidedMapIsNull() {
        // given / when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new TrackingMap<String, Integer>(null), "Constructor should reject a null map");
        assertEquals("Cannot construct a TrackingMap() with null", ex.getMessage());
    }

    /* -------------------------------------------------------------
     *  Behavioural tests using a real HashMap as the internal map
     * ------------------------------------------------------------- */
    @Test
    void isEmpty_True_OnNewlyCreatedEmptyBackingMap() {
        // given a fresh TrackingMap backed by an empty HashMap
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        // when
        boolean empty = map.isEmpty();
        // then
        assertTrue(empty, "A newly created TrackingMap wrapping an empty HashMap should be empty");
    }

    @Test
    void isEmpty_False_AfterSinglePut() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("key", 1);
        assertFalse(map.isEmpty(), "After inserting a single entry, isEmpty() must return false");
    }

    @Test
    void isEmpty_True_AfterClear() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("a", 1);
        map.put("b", 2);
        map.clear();
        assertTrue(map.isEmpty(), "Calling clear() on a non‑empty map should make it empty");
    }

    @Test
    void isEmpty_True_AfterRemovingAllEntriesIndividually() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.remove("one");
        map.remove("two");
        map.remove("three");
        assertTrue(map.isEmpty(), "Removing each entry should leave the map empty");
    }

    @Test
    void isEmpty_False_WhenOnlySomeEntriesRemoved() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);
        map.remove("x");
        assertFalse(map.isEmpty(), "At least one entry remains, so isEmpty() should be false");
    }

    @Test
    void isEmpty_False_WithPrepopulatedBackingMap() {
        Map<String, Integer> pre = new HashMap<>();
        pre.put("a", 1);
        pre.put("b", 2);
        TrackingMap<String, Integer> map = new TrackingMap<>(pre);
        assertFalse(map.isEmpty(), "A TrackingMap constructed with a pre‑populated map must report non‑empty");
    }

    @Test
    void isEmpty_True_PutAllWithEmptySource() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.putAll(Collections.emptyMap());
        assertTrue(map.isEmpty(), "putAll with an empty source map must leave the TrackingMap empty");
    }

    @Test
    void isEmpty_False_PutAllWithNonEmptySource() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);
        map.putAll(source);
        assertFalse(map.isEmpty(), "putAll with a non‑empty source map must make the TrackingMap non‑empty");
    }

    @Test
    void isEmpty_False_WhenNullKeyInserted() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put(null, 42);
        assertFalse(map.isEmpty(), "Inserting a null key creates an entry, so isEmpty() should be false");
    }

    @Test
    void isEmpty_False_WhenNullValueInserted() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put("key", null);
        assertFalse(map.isEmpty(), "Inserting a null value creates an entry, so isEmpty() should be false");
    }

    @Test
    void isEmpty_True_AfterRemovingNullKeyEntry() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put(null, null);
        map.remove(null);
        assertTrue(map.isEmpty(), "Removing the only null key/value entry should make the map empty");
    }

    @Test
    void isEmpty_True_ClearOnAlreadyEmptyMap() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        // clear on empty map
        map.clear();
        assertTrue(map.isEmpty(), "Calling clear() on an already empty map must keep it empty");
    }

    /* -------------------------------------------------------------
     *  Edge case: internalMap field is null (simulated via reflection)
     * ------------------------------------------------------------- */
    @Test
    void isEmpty_ThrowsNullPointerException_WhenInternalMapIsNull() throws Exception {
        // Create a normal instance first
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        // Use reflection to set the private final field to null
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(map, null);
        // Expect NPE when invoking isEmpty()
        assertThrows(NullPointerException.class, map::isEmpty, "Calling isEmpty() with a null internalMap should throw NullPointerException");
    }
}
