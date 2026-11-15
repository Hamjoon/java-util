package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingMap_isEmpty_Test {

    private TrackingMap<String, String> trackingMap;

    private Map<String, String> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple HashMap as the backing map for deterministic behaviour.
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC and avoid accidental cross‑test contamination.
        trackingMap = null;
        backingMap = null;
    }

    private static Stream<Arguments> simpleEmptinessScenarios() {
        return Stream.of(Arguments.of(List.of(), true, "newly created map"), Arguments.of(List.of("a"), false, "single entry"), Arguments.of(List.of("a", "b", "c"), false, "multiple entries"));
    }

    @Test
    void isEmpty_whenMapCreated_thenTrue() {
        assertTrue(trackingMap.isEmpty(), "A newly created TrackingMap with an empty backing map should be empty.");
    }

    @Test
    void isEmpty_afterPut_thenFalse() {
        trackingMap.put("key", "value");
        assertFalse(trackingMap.isEmpty(), "After adding an entry, isEmpty() should return false.");
    }

    @Test
    void isEmpty_afterClear_thenTrue() {
        trackingMap.put("k1", "v1");
        trackingMap.put("k2", "v2");
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty(), "After clearing all entries, isEmpty() should return true.");
    }

    @Test
    void isEmpty_afterExpungeUnusedWithNoAccess_thenTrue() {
        trackingMap.put("a", "1");
        trackingMap.put("b", "2");
        // No get/containsKey calls – both keys are considered unused.
        trackingMap.expungeUnused();
        assertTrue(trackingMap.isEmpty(), "expungeUnused() should remove all entries when none were accessed, making the map empty.");
    }

    @Test
    void isEmpty_afterExpungeUnusedWithPartialAccess_thenFalse() {
        trackingMap.put("used", "X");
        trackingMap.put("unused", "Y");
        // Access only one key.
        trackingMap.get("used");
        trackingMap.expungeUnused();
        assertFalse(trackingMap.isEmpty(), "expungeUnused() should leave accessed entries, so the map must not be empty.");
        assertEquals(1, trackingMap.size(), "Only the accessed entry should remain after expungeUnused().");
        assertEquals("X", trackingMap.get("used"));
    }

    @Test
    void isEmpty_reflectsDirectRemovalViaUnderlyingMap() {
        // Directly manipulate the backing map to ensure TrackingMap sees the change.
        backingMap.put("alpha", "A");
        assertFalse(trackingMap.isEmpty(), "Map should not be empty after directly adding an entry to the backing map.");
        backingMap.clear();
        assertTrue(trackingMap.isEmpty(), "Map should be empty after directly clearing the backing map.");
    }

    @Test
    void isEmpty_multiplePuts_thenFalse() {
        trackingMap.put("k1", "v1");
        trackingMap.put("k2", "v2");
        trackingMap.put("k3", "v3");
        assertFalse(trackingMap.isEmpty(), "Putting multiple entries should keep isEmpty() false.");
    }

    @Test
    void isEmpty_removeAllOneByOne_thenTrue() {
        trackingMap.put("a", "1");
        trackingMap.put("b", "2");
        trackingMap.put("c", "3");
        trackingMap.remove("a");
        trackingMap.remove("b");
        trackingMap.remove("c");
        assertTrue(trackingMap.isEmpty(), "Removing all entries one‑by‑one should make the map empty again.");
    }

    @Test
    void isEmpty_setWrappedMapToNull_thenThrowsNPE() {
        trackingMap.setWrappedMap(null);
        assertThrows(NullPointerException.class, trackingMap::isEmpty, "If the wrapped map is set to null, isEmpty() should throw NullPointerException.");
    }

    @Test
    void isEmpty_setWrappedMapPrePopulated_thenFalse() {
        Map<String, String> prePop = new HashMap<>();
        prePop.put("x", "X");
        prePop.put("y", "Y");
        trackingMap.setWrappedMap(prePop);
        assertFalse(trackingMap.isEmpty(), "After setWrappedMap() with a non‑empty map, isEmpty() must reflect the non‑empty state.");
    }

    @Test
    void isEmpty_setWrappedMapEmpty_thenTrue() {
        trackingMap.setWrappedMap(new HashMap<>());
        assertTrue(trackingMap.isEmpty(), "After setWrappedMap() with an empty map, isEmpty() should report true.");
    }

    @Test
    void isEmpty_putAllEmptySource_thenTrue() {
        Map<String, String> source = Collections.emptyMap();
        trackingMap.putAll(source);
        assertTrue(trackingMap.isEmpty(), "Calling putAll() with an empty source map on an empty TrackingMap must leave the map empty.");
    }

    @Test
    void isEmpty_putAllWithEntries_thenFalse() {
        Map<String, String> source = new HashMap<>();
        source.put("a", "A");
        source.put("b", "B");
        source.put("c", "C");
        trackingMap.putAll(source);
        assertFalse(trackingMap.isEmpty(), "Calling putAll() with a source map that has entries must make isEmpty() return false.");
    }

    @Test
    void isEmpty_putDuplicateKey_thenNonEmpty() {
        trackingMap.put("dup", "first");
        // update same key
        trackingMap.put("dup", "second");
        assertFalse(trackingMap.isEmpty(), "Updating an existing key should still leave the map non‑empty.");
    }

    @Test
    void isEmpty_putNullKeyNonNullValue_thenNonEmpty() {
        trackingMap.put(null, "value");
        assertFalse(trackingMap.isEmpty(), "Putting a null key with a non‑null value should make the map non‑empty.");
    }

    @Test
    void isEmpty_putNullKeyNullValue_thenNonEmpty() {
        trackingMap.put(null, null);
        assertFalse(trackingMap.isEmpty(), "Putting a null key with a null value should also result in a non‑empty map.");
    }

    @Test
    void isEmpty_removeNonExistingKey_whenOtherEntriesExist_thenFalse() {
        trackingMap.put("present", "yes");
        // remove non‑existing key
        trackingMap.remove("absent");
        assertFalse(trackingMap.isEmpty(), "Removing a non‑existent key must not change the emptiness state when other entries remain.");
    }

    @Test
    void isEmpty_clearOnAlreadyEmpty_thenTrue() {
        // map is already empty
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty(), "Calling clear() on an already empty TrackingMap must keep isEmpty() true.");
    }

    @Test
    void isEmpty_customMapWrongIsEmptyPropagation() {
        @SuppressWarnings("unchecked")
        Map<String, String> weirdMap = mock(Map.class);
        // lies about emptiness
        when(weirdMap.isEmpty()).thenReturn(false);
        when(weirdMap.size()).thenReturn(0);
        // other methods delegate to a real map to avoid NPEs during other operations
        when(weirdMap.put(anyString(), anyString())).thenCallRealMethod();
        when(weirdMap.get(any())).thenCallRealMethod();
        when(weirdMap.clear()).thenCallRealMethod();
        // Set the mocked map as the backing map
        trackingMap.setWrappedMap(weirdMap);
        assertFalse(trackingMap.isEmpty(), "When the wrapped map's isEmpty() incorrectly returns false, TrackingMap.isEmpty() should propagate that false result.");
    }

    @Test
    void isEmpty_delegateThrowsUnsupportedOperationException_propagates() {
        @SuppressWarnings("unchecked")
        Map<String, String> mockMap = mock(Map.class);
        when(mockMap.isEmpty()).thenThrow(new UnsupportedOperationException("not supported"));
        trackingMap.setWrappedMap(mockMap);
        assertThrows(UnsupportedOperationException.class, trackingMap::isEmpty, "UnsupportedOperationException from the delegate should be propagated.");
    }

    @Test
    void isEmpty_delegateThrowsRuntimeException_propagates() {
        @SuppressWarnings("unchecked")
        Map<String, String> mockMap = mock(Map.class);
        when(mockMap.isEmpty()).thenThrow(new RuntimeException("boom"));
        trackingMap.setWrappedMap(mockMap);
        RuntimeException ex = assertThrows(RuntimeException.class, trackingMap::isEmpty);
        assertEquals("boom", ex.getMessage(), "RuntimeException from the delegate should be propagated with its original message.");
    }

    @Test
    void isEmpty_delegateThrowsIllegalStateException_propagates() {
        @SuppressWarnings("unchecked")
        Map<String, String> mockMap = mock(Map.class);
        when(mockMap.isEmpty()).thenThrow(new IllegalStateException("bad state"));
        trackingMap.setWrappedMap(mockMap);
        IllegalStateException ex = assertThrows(IllegalStateException.class, trackingMap::isEmpty);
        assertEquals("bad state", ex.getMessage(), "IllegalStateException from the delegate should be propagated.");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("simpleEmptinessScenarios")
    void isEmpty_parameterisedScenario(List<String> keys, boolean expectedEmpty, String description) {
        // Populate according to the supplied keys
        for (String k : keys) {
            trackingMap.put(k, "val-" + k);
        }
        assertEquals(expectedEmpty, trackingMap.isEmpty(), "Scenario: " + description);
    }
}
