package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class TrackingMap_isEmpty_Test {

    private TrackingMap<String, String> trackingMap;

    private Map<String, String> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh backing map and TrackingMap for each test
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern consistent
        trackingMap = null;
        backingMap = null;
    }

    @Test
    @DisplayName("isEmpty() returns true for a newly constructed TrackingMap")
    void testIsEmptyOnNewMap() {
        assertTrue(trackingMap.isEmpty(), "A newly created TrackingMap should be empty");
    }

    @Test
    @DisplayName("isEmpty() returns true after putting entries and expunging unused ones")
    void testIsEmptyAfterPutAndExpungeUnused_NoAccess() {
        trackingMap.put("a", "alpha");
        trackingMap.put("b", "beta");
        // No access to keys – both are considered unused
        trackingMap.expungeUnused();
        assertTrue(trackingMap.isEmpty(), "All entries should have been removed, leaving the map empty");
        assertEquals(0, trackingMap.size(), "Size should be zero after expungeUnused");
    }

    @Test
    @DisplayName("isEmpty() returns false when at least one accessed entry remains after expunge")
    void testIsEmptyAfterPutAndExpungeUnused_WithAccess() {
        trackingMap.put("a", "alpha");
        trackingMap.put("b", "beta");
        // Access only key "a"
        trackingMap.get("a");
        trackingMap.expungeUnused();
        assertFalse(trackingMap.isEmpty(), "Map should not be empty because key \"a\" was accessed");
        assertEquals(1, trackingMap.size(), "Only the accessed entry should remain");
        assertEquals("alpha", trackingMap.get("a"));
    }

    @Test
    @DisplayName("isEmpty() returns true after clear() is called")
    void testIsEmptyAfterClear() {
        trackingMap.put("x", "xray");
        trackingMap.put("y", "yankee");
        assertFalse(trackingMap.isEmpty(), "Map should not be empty before clear");
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, trackingMap.size(), "Size must be zero after clear()");
    }

    @Test
    @DisplayName("isEmpty() reflects removal of the only entry")
    void testIsEmptyAfterRemoveOnlyEntry() {
        trackingMap.put("only", "single");
        assertFalse(trackingMap.isEmpty(), "Map should contain the entry before removal");
        trackingMap.remove("only");
        assertTrue(trackingMap.isEmpty(), "Map should be empty after removing its only entry");
        assertEquals(0, trackingMap.size(), "Size must be zero after removal");
    }

    @Test
    @DisplayName("isEmpty() works correctly after a mix of operations")
    void testIsEmptyMixedOperations() {
        // Populate
        trackingMap.put("k1", "v1");
        trackingMap.put("k2", "v2");
        trackingMap.put("k3", "v3");
        // Access some keys
        trackingMap.get("k1");
        trackingMap.containsKey("k2");
        // Remove one accessed key explicitly
        trackingMap.remove("k1");
        // Expunge unused (k3 should disappear)
        trackingMap.expungeUnused();
        // At this point only "k2" should remain
        assertFalse(trackingMap.isEmpty(), "Map should contain one accessed entry");
        assertEquals(1, trackingMap.size());
        assertTrue(trackingMap.containsKey("k2"));
        assertFalse(trackingMap.containsKey("k1"));
        assertFalse(trackingMap.containsKey("k3"));
    }

    @Test
    @DisplayName("isEmpty() after putAll() with non‑empty source map")
    void testIsEmptyAfterPutAllNonEmpty() {
        Map<String, String> source = new HashMap<>();
        source.put("one", "1");
        source.put("two", "2");
        trackingMap.putAll(source);
        assertFalse(trackingMap.isEmpty(), "putAll with entries should make the map non‑empty");
        assertEquals(2, trackingMap.size());
    }

    @Test
    @DisplayName("isEmpty() after putAll() with an empty source map")
    void testIsEmptyAfterPutAllEmpty() {
        Map<String, String> emptySource = Collections.emptyMap();
        trackingMap.putAll(emptySource);
        assertTrue(trackingMap.isEmpty(), "putAll with an empty map should leave the map empty");
        assertEquals(0, trackingMap.size());
    }

    @Test
    @DisplayName("Construction with a pre‑filled backing map")
    void testIsEmptyWithPreFilledBackingMap() {
        Map<String, String> preFilled = new HashMap<>();
        preFilled.put("pre", "filled");
        TrackingMap<String, String> preFilledTrackingMap = new TrackingMap<>(preFilled);
        assertFalse(preFilledTrackingMap.isEmpty(), "A TrackingMap wrapping a non‑empty map should not be empty");
        assertEquals(1, preFilledTrackingMap.size());
    }

    @Test
    @DisplayName("isEmpty() after inserting a null key")
    void testIsEmptyAfterPutNullKey() {
        trackingMap.put(null, "value");
        assertFalse(trackingMap.isEmpty(), "Map should not be empty after putting a null key");
        assertEquals(1, trackingMap.size());
        assertEquals("value", trackingMap.get(null));
    }

    @Test
    @DisplayName("isEmpty() after inserting a null value")
    void testIsEmptyAfterPutNullValue() {
        trackingMap.put("key", null);
        assertFalse(trackingMap.isEmpty(), "Map should not be empty after putting a null value");
        assertEquals(1, trackingMap.size());
        assertNull(trackingMap.get("key"));
    }

    @Test
    @DisplayName("isEmpty() after inserting null key and null value")
    void testIsEmptyAfterPutNullKeyAndValue() {
        trackingMap.put(null, null);
        assertFalse(trackingMap.isEmpty(), "Map should not be empty after putting null key and null value");
        assertEquals(1, trackingMap.size());
        assertNull(trackingMap.get(null));
    }

    @Test
    @DisplayName("isEmpty() after removing a non‑existent key from an empty map")
    void testIsEmptyRemoveNonExistentFromEmpty() {
        trackingMap.remove("missing");
        assertTrue(trackingMap.isEmpty(), "Removing a missing key from an empty map should keep it empty");
        assertEquals(0, trackingMap.size());
    }

    @Test
    @DisplayName("isEmpty() after clear() on an already empty map")
    void testIsEmptyClearOnEmpty() {
        // map is already empty
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty(), "Calling clear on an empty map should leave it empty");
        assertEquals(0, trackingMap.size());
    }

    @Test
    @DisplayName("isEmpty() after clearing the values collection")
    void testIsEmptyAfterValuesCollectionClear() {
        trackingMap.put("k", "v");
        Collection<String> values = trackingMap.values();
        // clears underlying map
        values.clear();
        assertTrue(trackingMap.isEmpty(), "Clearing the values collection should empty the map");
        assertEquals(0, trackingMap.size());
    }

    @Test
    @DisplayName("isEmpty() after removing the only entry via keySet")
    void testIsEmptyAfterKeySetRemove() {
        trackingMap.put("solo", "entry");
        Set<String> keys = trackingMap.keySet();
        assertTrue(keys.remove("solo"), "keySet().remove should return true for existing key");
        assertTrue(trackingMap.isEmpty(), "Map should be empty after removing its sole key via keySet");
        assertEquals(0, trackingMap.size());
    }

    @Test
    @DisplayName("isEmpty() after putAll() with a map containing a null key")
    void testIsEmptyAfterPutAllWithNullKey() {
        Map<String, String> source = new HashMap<>();
        source.put(null, "nullKeyValue");
        source.put("normal", "value");
        trackingMap.putAll(source);
        assertFalse(trackingMap.isEmpty(), "putAll with a null key should result in a non‑empty map");
        assertEquals(2, trackingMap.size());
        assertEquals("nullKeyValue", trackingMap.get(null));
    }

    @Test
    @DisplayName("informAdditionalUsage with empty collection does not affect emptiness")
    void testInformAdditionalUsageEmptyCollection() {
        trackingMap.informAdditionalUsage(Collections.emptyList());
        assertTrue(trackingMap.isEmpty(), "Calling informAdditionalUsage with an empty collection should not change emptiness");
    }

    @Test
    @DisplayName("isEmpty() throws NullPointerException when internalMap is null")
    void testIsEmptyWhenInternalMapIsNull() throws Exception {
        // Use reflection to set the private internalMap field to null
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(trackingMap, null);
        assertThrows(NullPointerException.class, () -> trackingMap.isEmpty(), "Calling isEmpty() with a null internalMap should throw NPE");
    }
}
