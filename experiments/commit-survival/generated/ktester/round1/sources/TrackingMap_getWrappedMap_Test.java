package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class TrackingMap_getWrappedMap_Test {

    private Map<String, Integer> backingMap;

    private TrackingMap<String, Integer> trackingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Use a real HashMap so we can verify that the returned reference is functional
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        backingMap = null;
        trackingMap = null;
    }

    @Test
    void getWrappedMap_returnsSameInstanceProvidedAtConstruction() {
        // GIVEN the TrackingMap was constructed with 'backingMap' (set up in @BeforeEach)
        // WHEN we retrieve the wrapped map
        Map<String, Integer> result = trackingMap.getWrappedMap();
        // THEN the returned map should be the exact same instance we supplied
        assertSame(backingMap, result, "getWrappedMap should return the original map instance");
    }

    @Test
    void getWrappedMap_allowsInteractionWithUnderlyingMap() {
        // GIVEN a map with some entries
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        // WHEN we obtain the wrapped map and query it
        Map<String, Integer> wrapped = trackingMap.getWrappedMap();
        // THEN the wrapped map reflects the current state of the underlying map
        assertEquals(2, wrapped.size());
        assertTrue(wrapped.containsKey("one"));
        assertEquals(1, wrapped.get("one"));
        assertTrue(wrapped.containsKey("two"));
        assertEquals(2, wrapped.get("two"));
    }

    @Test
    void constructorRejectsNullMap() {
        // GIVEN a null reference
        // WHEN constructing a TrackingMap with null
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> new TrackingMap<String, Integer>(null), "Expected constructor to throw IllegalArgumentException for null map");
        // THEN the exception message matches the contract
        assertEquals("Cannot construct a TrackingMap() with null", thrown.getMessage());
    }

    @Test
    void getWrappedMap_returnsPrepopulatedLinkedHashMapWithOriginalContents() {
        // GIVEN a pre‑populated LinkedHashMap
        Map<String, Integer> linkedMap = new LinkedHashMap<>();
        linkedMap.put("a", 1);
        linkedMap.put("b", 2);
        TrackingMap<String, Integer> tm = new TrackingMap<>(linkedMap);
        // WHEN we retrieve the wrapped map
        Map<String, Integer> result = tm.getWrappedMap();
        // THEN the returned map is the exact same instance and contains the original entries
        assertSame(linkedMap, result, "Returned map should be the same LinkedHashMap instance");
        assertEquals(2, result.size());
        assertEquals(1, result.get("a"));
        assertEquals(2, result.get("b"));
    }

    @Test
    void getWrappedMap_reflectsEntryAddedViaTrackingMapPut() {
        // GIVEN an empty TrackingMap (created in @BeforeEach)
        // WHEN we put a new entry through TrackingMap
        trackingMap.put("key", 42);
        // THEN the wrapped map reflects that entry
        Map<String, Integer> wrapped = trackingMap.getWrappedMap();
        assertEquals(1, wrapped.size());
        assertTrue(wrapped.containsKey("key"));
        assertEquals(42, wrapped.get("key"));
    }

    @Test
    void getWrappedMap_returnsEmptyMapAfterClear() {
        // GIVEN a TrackingMap with some entries
        trackingMap.put("x", 10);
        trackingMap.put("y", 20);
        assertFalse(trackingMap.isEmpty(), "Precondition: map should not be empty");
        // WHEN we clear the TrackingMap
        trackingMap.clear();
        // THEN the wrapped map is empty
        Map<String, Integer> wrapped = trackingMap.getWrappedMap();
        assertTrue(wrapped.isEmpty(), "Wrapped map should be empty after clear()");
        assertEquals(0, wrapped.size());
    }

    @Test
    void getWrappedMap_reflectsRemovalOfKey() {
        // GIVEN a map containing a specific key
        trackingMap.put("removeKey", 99);
        assertTrue(trackingMap.containsKey("removeKey"));
        // WHEN we remove that key via TrackingMap
        trackingMap.remove("removeKey");
        // THEN the wrapped map no longer contains the key
        Map<String, Integer> wrapped = trackingMap.getWrappedMap();
        assertFalse(wrapped.containsKey("removeKey"));
        assertNull(wrapped.get("removeKey"));
    }

    @Test
    void getWrappedMap_returnsNullWhenInternalMapSetToNullViaReflection() throws Exception {
        // GIVEN a TrackingMap with a valid internal map
        assertNotNull(trackingMap.getWrappedMap());
        // WHEN we forcibly set the private internalMap field to null using reflection
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(trackingMap, null);
        // THEN getWrappedMap() should return null rather than throwing an exception
        assertNull(trackingMap.getWrappedMap(), "When internalMap is null, getWrappedMap() should return null");
    }

    @Test
    void getWrappedMap_referenceStaysSameAfterMultipleMutatingOperations() {
        // GIVEN a TrackingMap backed by a HashMap
        // same instance used in @BeforeEach
        Map<String, Integer> originalMap = backingMap;
        assertSame(originalMap, trackingMap.getWrappedMap());
        // WHEN we perform a series of mutating operations
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        trackingMap.remove("a");
        trackingMap.clear();
        Map<String, Integer> anotherMap = new HashMap<>();
        anotherMap.put("c", 3);
        anotherMap.put("d", 4);
        trackingMap.putAll(anotherMap);
        // THEN the wrapped map reference is still the original map instance
        Map<String, Integer> afterOps = trackingMap.getWrappedMap();
        assertSame(originalMap, afterOps, "Reference to the underlying map must not change after mutating operations");
        // Also verify the final contents are as expected
        assertEquals(2, afterOps.size());
        assertEquals(3, afterOps.get("c"));
        assertEquals(4, afterOps.get("d"));
    }

    @Test
    void getWrappedMap_referenceUnchangedAfterTrackingSpecificCalls() {
        // GIVEN a TrackingMap with some data
        trackingMap.put("x", 1);
        trackingMap.put("y", 2);
        Map<String, Integer> original = trackingMap.getWrappedMap();
        // WHEN we invoke non‑mutating tracking methods
        // assumed to be a no‑op for the underlying map
        trackingMap.expungeUnused();
        TrackingMap<String, Integer> additional = new TrackingMap<>(new HashMap<>());
        additional.put("z", 3);
        trackingMap.informAdditionalUsage(additional);
        trackingMap.informAdditionalUsage(Arrays.asList("x", "y"));
        // THEN the wrapped map reference is still the original instance
        Map<String, Integer> afterCalls = trackingMap.getWrappedMap();
        assertSame(original, afterCalls, "Non‑mutating tracking calls must not replace the underlying map reference");
    }
}
