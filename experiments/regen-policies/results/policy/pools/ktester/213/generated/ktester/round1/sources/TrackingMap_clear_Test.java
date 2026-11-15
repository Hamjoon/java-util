package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class TrackingMap_clear_Test {

    /**
     * fields used by the “real” tests **
     */
    private TrackingMap<String, String> realTrackingMap;

    private Map<String, String> realBackingMap;

    /**
     * fields used by the “mock” test **
     */
    @Mock
    private Map<String, String> mockBackingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh TrackingMap backed by a real HashMap for the majority of tests
        realBackingMap = new HashMap<>();
        realTrackingMap = new TrackingMap<>(realBackingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC – not strictly required but keeps the pattern tidy
        realTrackingMap = null;
        realBackingMap = null;
    }

    /**
     * Verify that {@code clear()} empties both the underlying map
     * and the set that tracks accessed keys.
     */
    @Test
    void clearEmptiesInternalMapAndReadKeys() {
        // Populate the map
        realTrackingMap.put("a", "alpha");
        realTrackingMap.put("b", "bravo");
        realTrackingMap.put("c", "charlie");
        // Access a couple of keys – this should populate the read‑keys set
        realTrackingMap.get("a");
        realTrackingMap.containsKey("b");
        // Preconditions – map is not empty and some keys have been recorded as used
        assertFalse(realTrackingMap.isEmpty(), "Map should contain entries before clear");
        assertEquals(3, realTrackingMap.size(), "Map should contain three entries before clear");
        assertEquals(2, realTrackingMap.keysUsed().size(), "Two keys should be recorded as used before clear");
        // Invoke the method under test
        realTrackingMap.clear();
        // Post‑conditions – both the backing map and the read‑keys set must be empty
        assertTrue(realTrackingMap.isEmpty(), "Map should be empty after clear");
        assertEquals(0, realTrackingMap.size(), "Size should be zero after clear");
        assertTrue(realTrackingMap.keysUsed().isEmpty(), "The set of used keys should be empty after clear");
    }

    /**
     * Verify that {@code clear()} delegates to {@code Map.clear()} on the wrapped map.
     * This test uses a Mockito mock to ensure the delegation occurs exactly once.
     */
    @Test
    void clearDelegatesToWrappedMapClear() {
        // The mock is injected by MockitoExtension; construct a TrackingMap with it
        TrackingMap<String, String> trackingMapWithMock = new TrackingMap<>(mockBackingMap);
        // Stubbing put/get is not strictly necessary for this test,
        // but we provide benign behaviour to avoid NullPointerException
        when(mockBackingMap.put(anyString(), anyString())).thenReturn(null);
        when(mockBackingMap.get(anyString())).thenReturn(null);
        when(mockBackingMap.containsKey(anyString())).thenReturn(false);
        // Populate the map via the TrackingMap API – this will call the mock's put method
        trackingMapWithMock.put("x", "xray");
        trackingMapWithMock.put("y", "yankee");
        // Verify that the mock's put method was indeed called (sanity check)
        verify(mockBackingMap, times(2)).put(anyString(), anyString());
        // Reset interactions so that only the clear call is verified
        clearInvocations(mockBackingMap);
        // Call the method under test
        trackingMapWithMock.clear();
        // Ensure the mock's clear() method was invoked exactly once
        verify(mockBackingMap, times(1)).clear();
    }

    /**
     * Clear on an already empty TrackingMap – should remain empty and not throw.
     */
    @Test
    void clearOnAlreadyEmptyMap() {
        // Preconditions – map is empty
        assertTrue(realTrackingMap.isEmpty(), "Pre‑condition: map must be empty");
        // Invoke clear
        realTrackingMap.clear();
        // Post‑conditions – still empty, no exceptions
        assertTrue(realTrackingMap.isEmpty(), "Map should stay empty after clear");
        assertEquals(0, realTrackingMap.size(), "Size must remain zero");
        assertTrue(realTrackingMap.keysUsed().isEmpty(), "Read‑keys set must stay empty");
    }

    /**
     * Clear after only read operations (no puts). The read‑keys set should be cleared.
     */
    @Test
    void clearAfterOnlyReadOperations() {
        // Perform a read on a non‑existent key – this records the key in readKeys
        realTrackingMap.get("nonexistent");
        realTrackingMap.containsKey("another");
        // Verify that read‑keys have been recorded even though the map is empty
        assertTrue(realTrackingMap.isEmpty(), "Map should still be empty");
        assertEquals(2, realTrackingMap.keysUsed().size(), "Two keys should be recorded as used before clear");
        // Clear the map
        realTrackingMap.clear();
        // After clear both map and read‑keys must be empty
        assertTrue(realTrackingMap.isEmpty(), "Map should be empty after clear");
        assertTrue(realTrackingMap.keysUsed().isEmpty(), "Read‑keys set must be empty after clear");
    }

    /**
     * After a clear, the map should behave like a fresh instance – a subsequent put works.
     */
    @Test
    void clearThenPutWorks() {
        // Populate and then clear
        realTrackingMap.put("a", "1");
        realTrackingMap.clear();
        // New put after clear
        realTrackingMap.put("a", "1");
        // Verify expected state
        assertFalse(realTrackingMap.isEmpty(), "Map should contain the newly added entry");
        assertEquals(1, realTrackingMap.size(), "Size should be 1 after new put");
        assertTrue(realTrackingMap.keysUsed().contains("a"), "keysUsed() must contain the newly put key");
    }

    /**
     * Clear after using {@code putAll()} and reading a few keys.
     */
    @Test
    void clearAfterPutAll() {
        Map<String, String> bulk = new HashMap<>();
        bulk.put("one", "1");
        bulk.put("two", "2");
        bulk.put("three", "3");
        realTrackingMap.putAll(bulk);
        // Read a subset of keys
        realTrackingMap.get("one");
        realTrackingMap.containsKey("two");
        // Preconditions
        assertEquals(3, realTrackingMap.size(), "Map should have three entries before clear");
        assertEquals(2, realTrackingMap.keysUsed().size(), "Two keys should be recorded as used before clear");
        // Clear
        realTrackingMap.clear();
        // Post‑conditions
        assertTrue(realTrackingMap.isEmpty(), "Map should be empty after clear");
        assertEquals(0, realTrackingMap.size(), "Size must be zero after clear");
        assertTrue(realTrackingMap.keysUsed().isEmpty(), "Read‑keys set must be empty after clear");
    }

    /**
     * Clear when the internal map reference is {@code null}. Expected: NullPointerException.
     */
    @Test
    void clearWithNullInternalMapThrowsNPE() {
        // Break the internal map via the public API
        realTrackingMap.setWrappedMap(null);
        // Expect NPE when clear() attempts to delegate to the null map
        assertThrows(NullPointerException.class, () -> realTrackingMap.clear(), "clear() should throw NullPointerException when internal map is null");
    }

    /**
     * Clear when the wrapped map is unmodifiable. Expected: UnsupportedOperationException.
     */
    @Test
    void clearWithUnmodifiableInternalMapThrowsUOE() {
        Map<String, String> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        TrackingMap<String, String> tm = new TrackingMap<>(unmodifiable);
        // Populate via reflection to bypass unmodifiable restrictions for the test setup
        // (or simply attempt a put that will fail – we only need the map to be non‑empty)
        assertThrows(UnsupportedOperationException.class, () -> tm.put("x", "y"), "Putting into an unmodifiable map should fail, confirming the setup");
        // Now invoke clear – it should propagate the same exception
        assertThrows(UnsupportedOperationException.class, tm::clear, "clear() should propagate UnsupportedOperationException from the wrapped map");
    }

    /**
     * Clear when the read‑keys set is unmodifiable. Expected: UnsupportedOperationException.
     */
    @Test
    void clearWithUnmodifiableReadKeysSetThrowsUOE() throws Exception {
        // Populate the map normally so that readKeys is non‑empty
        realTrackingMap.put("k1", "v1");
        // records the key
        realTrackingMap.get("k1");
        // Replace the private readKeys field with an unmodifiable view
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        Set<String> mutableCopy = new HashSet<>(realTrackingMap.keysUsed());
        Set<String> unmodifiableSet = Collections.unmodifiableSet(mutableCopy);
        readKeysField.set(realTrackingMap, unmodifiableSet);
        // Attempt to clear – should throw because clear() tries to clear the unmodifiable set
        assertThrows(UnsupportedOperationException.class, () -> realTrackingMap.clear(), "clear() should propagate UnsupportedOperationException from readKeys.clear()");
    }
}
