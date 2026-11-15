package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

// Mockito not required but kept from template
@ExtendWith(MockitoExtension.class)
class TrackingMap_setWrappedMap_Test {

    private TrackingMap<String, String> trackingMap;

    private Map<String, String> initialMap;

    private Map<String, String> replacementMap;

    @BeforeEach
    void setupBeforeEach() {
        // Create a simple backing map with a couple of entries
        initialMap = new HashMap<>();
        initialMap.put("a", "alpha");
        initialMap.put("b", "bravo");
        // The map that will later replace the backing map
        replacementMap = new HashMap<>();
        replacementMap.put("x", "xray");
        replacementMap.put("y", "yankee");
        replacementMap.put("z", "zulu");
        // Construct the TrackingMap using the initial backing map
        trackingMap = new TrackingMap<>(initialMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps the garbage collector and isolates tests
        trackingMap = null;
        initialMap = null;
        replacementMap = null;
    }

    @Test
    void setWrappedMap_NullArgument_ThrowsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> trackingMap.setWrappedMap(null));
        // Verify that the exception message matches the one used in the source
        assertTrue(ex.getMessage().contains("Cannot set a TrackingMap() with null"));
    }

    @Test
    void setWrappedMap_ReplacesBackingMapAndClearsTrackingData() {
        // Access some keys in the original map so that they are recorded in keysUsed
        trackingMap.get("a");
        trackingMap.containsKey("b");
        assertEquals(2, trackingMap.keysUsed().size(), "keysUsed should contain the two accessed keys before replacement");
        // Replace the internal map
        trackingMap.setWrappedMap(replacementMap);
        // The backing map should now be the replacement map
        assertSame(replacementMap, trackingMap.getWrappedMap(), "The wrapped map should be the one supplied to setWrappedMap");
        // All previously stored entries must be gone; size must equal replacementMap size
        assertEquals(replacementMap.size(), trackingMap.size(), "Size after replacement must match the new backing map size");
        // The tracking of accessed keys must be cleared by the clear() call inside setWrappedMap
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed should be empty after setWrappedMap because clear() was called");
        // Verify that the new entries are reachable via get()
        assertEquals("xray", trackingMap.get("x"));
        assertEquals("yankee", trackingMap.get("y"));
        assertEquals("zulu", trackingMap.get("z"));
    }

    @Test
    void setWrappedMap_CopiesEntriesAndIsIndependentFromSourceMap() {
        // Perform the replacement
        trackingMap.setWrappedMap(replacementMap);
        // Mutate the source replacementMap after the call
        replacementMap.put("newKey", "newValue");
        // The TrackingMap must NOT see the newly added entry because putAll performed a copy
        assertFalse(trackingMap.containsKey("newKey"), "TrackingMap should not reflect changes made to the source map after setWrappedMap");
        // Its size must remain equal to the size at the moment of replacement
        assertEquals(3, trackingMap.size(), "Size should remain the original number of entries copied from replacementMap");
    }

    @Test
    void setWrappedMap_ReplaceWithEmptyMap_ResultsInEmptyTrackingMap() {
        // Populate trackingMap with some data first
        trackingMap.get("a");
        trackingMap.put("c", "charlie");
        Map<String, String> emptyMap = new HashMap<>();
        trackingMap.setWrappedMap(emptyMap);
        // Backing map should be the empty one and size must be zero
        assertSame(emptyMap, trackingMap.getWrappedMap(), "Wrapped map should be the empty map supplied");
        assertEquals(0, trackingMap.size(), "TrackingMap size becomes 0 after setting an empty map");
        assertTrue(trackingMap.isEmpty(), "TrackingMap reports empty after replacement with empty map");
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed set must be empty after replacement");
    }

    @Test
    void setWrappedMap_ReplaceWithPopulatedMap_ContainsExactlySuppliedEntries() {
        Map<String, Integer> newMap = Map.of("a", 1, "b", 2, "c", 3);
        // Use a TrackingMap<Integer> to match the value type
        TrackingMap<String, Integer> intTrackingMap = new TrackingMap<>(new HashMap<>());
        intTrackingMap.setWrappedMap(newMap);
        assertEquals(3, intTrackingMap.size(), "Size must be 3 after replacement with a map of three entries");
        assertEquals(1, intTrackingMap.get("a"));
        assertEquals(2, intTrackingMap.get("b"));
        assertEquals(3, intTrackingMap.get("c"));
        assertTrue(intTrackingMap.keysUsed().isEmpty(), "keysUsed set must be empty after setWrappedMap");
    }

    @Test
    void setWrappedMap_MapWithNullKeyAndNullValue_IsHandledCorrectly() {
        Map<String, String> mapWithNulls = new HashMap<>();
        mapWithNulls.put(null, "nullKey");
        mapWithNulls.put("nullValue", null);
        TrackingMap<String, String> nullMapTracking = new TrackingMap<>(new HashMap<>());
        nullMapTracking.setWrappedMap(mapWithNulls);
        assertEquals(2, nullMapTracking.size(), "Size should be 2 after copying map with null key/value");
        assertEquals("nullKey", nullMapTracking.get(null), "get(null) must return the associated value");
        assertNull(nullMapTracking.get("nullValue"), "get(\"nullValue\") must return null");
        assertTrue(nullMapTracking.keysUsed().isEmpty(), "keysUsed must be empty after replacement");
    }

    @Test
    void setWrappedMap_SelfAssignment_RetainsEntriesWithoutError() {
        // Record usage before self‑assignment
        trackingMap.get("a");
        trackingMap.containsKey("b");
        assertFalse(trackingMap.keysUsed().isEmpty(), "keysUsed should contain entries before self‑assignment");
        // Pass the current wrapped map back to setWrappedMap
        Map<String, String> currentWrapped = trackingMap.getWrappedMap();
        trackingMap.setWrappedMap(currentWrapped);
        // The map should still contain the original entries
        assertEquals(initialMap.size(), trackingMap.size(), "Size must remain unchanged after self‑assignment");
        assertEquals("alpha", trackingMap.get("a"));
        assertEquals("bravo", trackingMap.get("b"));
        // keysUsed must have been cleared by the internal clear()
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed should be empty after self‑assignment");
    }

    @Test
    void setWrappedMap_ResetsUsageTracking_AfterInformAdditionalUsage() {
        // Track usage of key "x"
        trackingMap.informAdditionalUsage(Collections.singleton("x"));
        assertTrue(trackingMap.keysUsed().contains("x"), "keysUsed should contain \"x\" after informAdditionalUsage");
        // Replace the map – usage tracking must be cleared
        Map<String, String> newMap = Map.of("x", "10");
        trackingMap.setWrappedMap(newMap);
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed set becomes empty after setWrappedMap");
        assertEquals("10", trackingMap.get("x"));
    }

    @Test
    void setWrappedMap_IncompatibleKeyType_ThrowsClassCastException() {
        // TrackingMap expects String keys, we supply a map containing an Integer key.
        @SuppressWarnings("unchecked")
        Map rawMap = new HashMap<>();
        rawMap.put(123, "one");
        rawMap.put("stringKey", "two");
        TrackingMap<String, String> map = new TrackingMap<>(new HashMap<>());
        assertThrows(ClassCastException.class, () -> map.setWrappedMap(rawMap), "Providing a map with a non‑String key should result in ClassCastException");
    }

    @Test
    void setWrappedMap_IncompatibleValueType_ThrowsClassCastException() {
        // TrackingMap expects Integer values, we supply a map with a non‑Integer value.
        @SuppressWarnings("unchecked")
        Map rawMap = new HashMap<>();
        // String instead of Integer
        rawMap.put("a", "value");
        // Object instead of Integer
        rawMap.put("b", new Object());
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        assertThrows(ClassCastException.class, () -> map.setWrappedMap(rawMap), "Providing a map with non‑Integer values should result in ClassCastException");
    }

    @Test
    void setWrappedMap_NullKeyOnHashtable_ThrowsNullPointerException() {
        Hashtable<String, Integer> hashtable = new Hashtable<>();
        // This line itself throws NPE, so we need to construct via reflection
        hashtable.put(null, 1);
        // Build a Hashtable that contains a null key using a double‑brace initializer (will throw at runtime)
        Hashtable<String, Integer> badTable = new Hashtable<String, Integer>() {

            {
                // The put will throw NPE; we catch it to create the map reference
                try {
                    put(null, 1);
                } catch (NullPointerException ignored) {
                }
            }
        };
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        assertThrows(NullPointerException.class, () -> map.setWrappedMap(badTable), "Hashtable does not permit null keys, should throw NullPointerException");
    }

    @Test
    void setWrappedMap_NullValueOnHashtable_ThrowsNullPointerException() {
        Hashtable<String, Integer> badTable = new Hashtable<String, Integer>() {

            {
                try {
                    put("key", null);
                } catch (NullPointerException ignored) {
                }
            }
        };
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        assertThrows(NullPointerException.class, () -> map.setWrappedMap(badTable), "Hashtable does not permit null values, should throw NullPointerException");
    }

    @Test
    void setWrappedMap_NullKeyOnTreeMap_ThrowsNullPointerException() {
        TreeMap<String, Integer> treeMap = new TreeMap<>();
        // TreeMap throws NPE when a null key is inserted
        assertThrows(NullPointerException.class, () -> treeMap.put(null, 5), "TreeMap does not accept null keys");
        // Even though the map is empty, passing it to setWrappedMap should be fine.
        // To test the NPE path we need a TreeMap that already contains a null key,
        // which is impossible without catching the exception during insertion.
        // Therefore we use a raw TreeMap and suppress the warning.
        @SuppressWarnings("unchecked")
        TreeMap rawTreeMap = new TreeMap();
        try {
            rawTreeMap.put(null, 5);
        } catch (NullPointerException ignored) {
        }
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        assertThrows(NullPointerException.class, () -> map.setWrappedMap(rawTreeMap), "TreeMap containing a null key should cause NullPointerException during setWrappedMap");
    }
}
