package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_size_Test {

    private TrackingMap<String, Integer> trackingMap;

    private Map<String, Integer> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple HashMap as the wrapped map for deterministic behaviour
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        trackingMap = null;
        backingMap = null;
    }

    private static Stream<Arguments> provideSizeAfterPutScenarios() {
        return Stream.of(Arguments.of("Add new key to empty map", Collections.emptyMap(), "k1", 1, 1), Arguments.of("Replace existing key, size unchanged", Map.of("k1", 10), "k1", 99, 1), Arguments.of("Add new key when map already has entries", Map.of("a", 1, "b", 2), "c", 3, 3));
    }

    @Test
    void sizeOnEmptyMap() {
        // Given: a freshly created TrackingMap with no entries
        assertTrue(trackingMap.isEmpty(), "Map should be empty initially");
        // When: size() is called
        int size = trackingMap.size();
        // Then: size should be 0
        assertEquals(0, size, "Size of an empty map must be 0");
    }

    @Test
    void sizeAfterAddingEntries() {
        // Given: map with three entries
        trackingMap.put("one", 1);
        trackingMap.put("two", 2);
        trackingMap.put("three", 3);
        // When: size() is called
        int size = trackingMap.size();
        // Then: size reflects the number of entries added
        assertEquals(3, size, "Size should match the number of inserted entries");
    }

    @Test
    void sizeAfterExpungeUnusedWhenNoKeysAccessed() {
        // Given: map with two entries that are never accessed
        trackingMap.put("a", 10);
        trackingMap.put("b", 20);
        // When: expungeUnused() is invoked (no get/containsKey calls made)
        trackingMap.expungeUnused();
        // Then: all entries are removed and size is 0
        assertEquals(0, trackingMap.size(), "All entries should be removed when none were accessed");
        assertTrue(trackingMap.isEmpty(), "Map should be empty after expungeUnused");
    }

    @Test
    void sizeAfterExpungeUnusedWhenSomeKeysAccessed() {
        // Given: map with three entries
        trackingMap.put("x", 100);
        trackingMap.put("y", 200);
        trackingMap.put("z", 300);
        // Access two of them
        trackingMap.get("x");
        trackingMap.containsKey("y");
        // When: expungeUnused() is called
        trackingMap.expungeUnused();
        // Then: only accessed keys remain
        assertEquals(2, trackingMap.size(), "Only accessed entries should remain after expungeUnused");
        assertTrue(trackingMap.containsKey("x"));
        assertTrue(trackingMap.containsKey("y"));
        assertFalse(trackingMap.containsKey("z"));
    }

    @Test
    void sizeAfterUpdatingExistingKey() {
        // Insert two distinct keys
        trackingMap.put("alpha", 1);
        trackingMap.put("beta", 2);
        // Replace value of an existing key
        trackingMap.put("alpha", 99);
        // Size must stay at 2 because no new key was added
        assertEquals(2, trackingMap.size(), "Inserting a value for an existing key must not change the map size");
    }

    @Test
    void sizeAfterRemovingEntries() {
        // Populate with three entries
        trackingMap.put("k1", 10);
        trackingMap.put("k2", 20);
        trackingMap.put("k3", 30);
        // Remove two of them
        trackingMap.remove("k1");
        trackingMap.remove("k3");
        // Only one entry should remain
        assertEquals(1, trackingMap.size(), "After removing two of three entries, size should be 1");
        assertTrue(trackingMap.containsKey("k2"));
    }

    @Test
    void sizeAfterClear() {
        // Fill the map
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        trackingMap.put("c", 3);
        // Clear all entries
        trackingMap.clear();
        // Size must be zero
        assertEquals(0, trackingMap.size(), "Size must be 0 after clear() is invoked");
        assertTrue(trackingMap.isEmpty());
    }

    @Test
    void sizeAfterPutAll() {
        // Prepare a source map with four entries
        Map<String, Integer> source = new HashMap<>();
        source.put("p", 1);
        source.put("q", 2);
        source.put("r", 3);
        source.put("s", 4);
        // Apply putAll
        trackingMap.putAll(source);
        // Verify size
        assertEquals(4, trackingMap.size(), "putAll() with four distinct entries should result in size 4");
    }

    @Test
    void sizeAfterRemovingNonExistentKey() {
        // Insert two entries
        trackingMap.put("x", 10);
        trackingMap.put("y", 20);
        // Attempt to remove a key that does not exist
        trackingMap.remove("nonexistent");
        // Size must remain unchanged
        assertEquals(2, trackingMap.size(), "Removing a non‑existent key must not affect the map size");
    }

    @Test
    void sizeAfterAddingNullKey() {
        // Null keys are allowed; add one with a non‑null value
        trackingMap.put(null, 42);
        assertEquals(1, trackingMap.size(), "Map should report size 1 after inserting a null key");
        assertTrue(trackingMap.containsKey(null));
    }

    @Test
    void sizeAfterPutAllWithOverlappingKeys() {
        // Start with three distinct entries
        trackingMap.put("A", 1);
        trackingMap.put("B", 2);
        trackingMap.put("C", 3);
        // Source map contains an overlapping key (B) and two new keys (D, E)
        Map<String, Integer> source = new HashMap<>();
        // overlap
        source.put("B", 22);
        source.put("D", 4);
        source.put("E", 5);
        // Apply putAll
        trackingMap.putAll(source);
        // Expected size: original 3 + 2 new keys = 5
        assertEquals(5, trackingMap.size(), "putAll() with overlapping keys should increase size only for new keys");
        assertTrue(trackingMap.containsKey("D"));
        assertTrue(trackingMap.containsKey("E"));
        // value should be overwritten
        assertEquals(22, trackingMap.get("B"));
    }

    @Test
    void sizeAfterPutAllWithEmptyMap() {
        // Populate with two entries
        trackingMap.put("one", 1);
        trackingMap.put("two", 2);
        // PutAll with an empty map
        trackingMap.putAll(Collections.emptyMap());
        // Size must stay the same
        assertEquals(2, trackingMap.size(), "Putting an empty map must not change the size");
    }

    @Test
    void sizeAfterSetWrappedMapToAnotherMap() {
        // Fill the tracking map with some entries (they will be replaced)
        trackingMap.put("old1", 10);
        trackingMap.put("old2", 20);
        // Create a brand‑new underlying map with four entries
        Map<String, Integer> newUnderlying = new HashMap<>();
        newUnderlying.put("n1", 1);
        newUnderlying.put("n2", 2);
        newUnderlying.put("n3", 3);
        newUnderlying.put("n4", 4);
        // Replace the wrapped map
        trackingMap.setWrappedMap(newUnderlying);
        // Size must now reflect the new underlying map
        assertEquals(4, trackingMap.size(), "After setWrappedMap() the size should reflect the new map's size");
    }

    @Test
    void sizeAfterExternalModificationViaGetWrappedMap() {
        // Insert three entries
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        trackingMap.put("c", 3);
        // Retrieve the underlying map and modify it directly
        Map<String, Integer> raw = trackingMap.getWrappedMap();
        raw.remove("b");
        // Size should now be 2
        assertEquals(2, trackingMap.size(), "Direct modification of the wrapped map must be reflected in size()");
        assertFalse(trackingMap.containsKey("b"));
    }

    @Test
    void sizeAfterExpungeUnusedWithUnusedEntries() {
        // Add three entries
        trackingMap.put("A", 10);
        trackingMap.put("B", 20);
        trackingMap.put("C", 30);
        // Access only one entry
        trackingMap.get("A");
        // Expunge unused entries
        trackingMap.expungeUnused();
        // Only the accessed entry should remain
        assertEquals(1, trackingMap.size(), "expungeUnused() should leave only accessed entries");
        assertTrue(trackingMap.containsKey("A"));
        assertFalse(trackingMap.containsKey("B"));
        assertFalse(trackingMap.containsKey("C"));
    }

    @Test
    void sizeWhenInternalMapIsNull() {
        // Construct a TrackingMap with a null underlying map
        TrackingMap<String, Integer> nullMap = new TrackingMap<>(null);
        // Expect a NullPointerException when size() is called
        assertThrows(NullPointerException.class, nullMap::size, "Calling size() on a TrackingMap with a null internal map must throw NPE");
    }

    /* ----------------------------------------------------------------------
     * Parameterized test example (optional – demonstrates usage)
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => description=''{0}''")
    @MethodSource("provideSizeAfterPutScenarios")
    void sizeAfterPutScenarios(String description, Map<String, Integer> initial, String key, Integer value, int expectedSize) {
        // Arrange
        initial.forEach(trackingMap::put);
        // Act
        trackingMap.put(key, value);
        // Assert
        assertEquals(expectedSize, trackingMap.size(), description);
    }
}
