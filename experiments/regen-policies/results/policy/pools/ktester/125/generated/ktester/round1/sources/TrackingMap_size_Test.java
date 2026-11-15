package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_size_Test {

    private TrackingMap<String, String> trackingMap;

    private Map<String, String> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple HashMap as the underlying map – it is deterministic and requires no extra setup
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help the garbage collector; not strictly required but keeps the pattern clean
        trackingMap = null;
        backingMap = null;
    }

    /**
     * Supplies a series of map‑setup actions together with the expected size.
     *
     * <p>The {@code Consumer<TrackingMap<String,String>>} mutates the map
     * (e.g., {@code put}, {@code putAll}, {@code remove}, {@code clear}) before the
     * size is asserted.</p>
     */
    private static Stream<Arguments> sizeScenarios() {
        return Stream.of(// 0 – empty map
        Arguments.of((Consumer<TrackingMap<String, String>>) map -> {
        }, // no mutation
        0, "When the underlying internal map is empty, size() should return 0."), // 1 – single entry
        Arguments.of((Consumer<TrackingMap<String, String>>) map -> map.put("k", "v"), 1, "When a single key‑value pair is put into the map, size() should return 1."), // 5 – five distinct entries
        Arguments.of((Consumer<TrackingMap<String, String>>) map -> {
            for (int i = 1; i <= 5; i++) {
                map.put("k" + i, "v" + i);
            }
        }, 5, "When five distinct entries are added, size() should reflect the exact count (5)."), // clear after population
        Arguments.of((Consumer<TrackingMap<String, String>>) map -> {
            map.put("a", "1");
            map.put("b", "2");
            map.clear();
        }, 0, "After populating the map and then calling clear(), size() must return 0."), // remove one of three entries
        Arguments.of((Consumer<TrackingMap<String, String>>) map -> {
            map.put("x", "1");
            map.put("y", "2");
            map.put("z", "3");
            map.remove("y");
        }, 2, "After adding three entries and removing one, size() should return the remaining count (2)."), // null value with non‑null key
        Arguments.of((Consumer<TrackingMap<String, String>>) map -> map.put("nullVal", null), 1, "When a null value is associated with a non‑null key, size() should count the entry (size = 1)."), // null key with non‑null value
        Arguments.of((Consumer<TrackingMap<String, String>>) map -> map.put(null, "nullKey"), 1, "When a null key is inserted with a non‑null value, size() should count the entry (size = 1)."), // replace existing key (size unchanged)
        Arguments.of((Consumer<TrackingMap<String, String>>) map -> {
            map.put("k1", "v1");
            map.put("k2", "v2");
            map.put("k3", "v3");
            map.put("k2", "newV2");
        }, // replace value for existing key
        3, "After inserting three distinct entries and then putting a fourth entry with a key that already exists (replacing its value), size() should remain 3."), // putAll with three entries
        Arguments.of((Consumer<TrackingMap<String, String>>) map -> {
            Map<String, String> src = new HashMap<>();
            src.put("a", "1");
            src.put("b", "2");
            src.put("c", "3");
            map.putAll(src);
        }, 3, "Using putAll() with a source map containing three entries should result in size() returning 3."), // remove non‑existent key (size unchanged)
        Arguments.of((Consumer<TrackingMap<String, String>>) map -> {
            map.put("p", "1");
            map.put("q", "2");
            map.remove("nonexistent");
        }, 2, "Removing a key that is not present does not change the map; after removing a non‑existent key from a map that already contains two entries, size() stays 2."), // clear then add four entries
        Arguments.of((Consumer<TrackingMap<String, String>>) map -> {
            map.put("old1", "x");
            map.put("old2", "y");
            map.clear();
            map.put("n1", "1");
            map.put("n2", "2");
            map.put("n3", "3");
            map.put("n4", "4");
        }, 4, "After calling clear() on a populated map and then adding four new entries, size() should reflect the new count (size = 4)."));
    }

    @Test
    void sizeOnNewEmptyMap() {
        // Given an empty TrackingMap
        // When we query its size
        int size = trackingMap.size();
        // Then it should be zero
        assertEquals(0, size, "Size of a newly created empty map should be 0");
    }

    @Test
    void sizeAfterAddingEntries() {
        // Given a map with three entries
        trackingMap.put("a", "alpha");
        trackingMap.put("b", "bravo");
        trackingMap.put("c", "charlie");
        // When we query its size
        int size = trackingMap.size();
        // Then it should reflect the number of entries added
        assertEquals(3, size, "Size should match the number of inserted entries");
    }

    @Test
    void sizeAfterRemovingAnEntry() {
        // Given a map with two entries
        trackingMap.put("x", "ex");
        trackingMap.put("y", "why");
        // When one entry is removed
        trackingMap.remove("x");
        // Then the size should be decremented
        assertEquals(1, trackingMap.size(), "Size should be 1 after removing one of two entries");
    }

    @Test
    void sizeAfterClear() {
        // Given a map with several entries
        trackingMap.put("k1", "v1");
        trackingMap.put("k2", "v2");
        trackingMap.put("k3", "v3");
        // When clear() is invoked
        trackingMap.clear();
        // Then the size should be zero
        assertEquals(0, trackingMap.size(), "Size should be 0 after clear()");
        assertTrue(trackingMap.isEmpty(), "Map should report empty after clear()");
    }

    @Test
    void sizeReflectsExpungeUnusedEffect() {
        // Given a map with three entries
        trackingMap.put("first", "1");
        trackingMap.put("second", "2");
        trackingMap.put("third", "3");
        // Access only "first" and "second"
        trackingMap.get("first");
        trackingMap.get("second");
        // When expungeUnused() removes the untouched entry
        trackingMap.expungeUnused();
        // Then size should be 2
        assertEquals(2, trackingMap.size(), "Size should reflect only the accessed entries after expungeUnused()");
        assertFalse(trackingMap.isEmpty(), "Map should not be empty after expungeUnused() when some entries remain");
    }

    @Test
    void sizeThrowsWhenConstructedWithNullMap() {
        // When constructing a TrackingMap with a null backing map
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> new TrackingMap<String, String>(null), "Constructor should reject null backing map");
        // Then the exception message should be as documented
        assertTrue(thrown.getMessage().contains("Cannot construct a TrackingMap() with null"));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("sizeScenarios")
    void sizeParameterizedTests(Consumer<TrackingMap<String, String>> mapSetup, int expectedSize, String description) {
        // Apply the scenario‑specific mutations
        mapSetup.accept(trackingMap);
        // Verify the size matches expectation
        assertEquals(expectedSize, trackingMap.size(), () -> "Failed scenario: " + description);
    }

    @Test
    void sizeThrowsWhenInternalMapIsNullViaReflection() throws Exception {
        // Force the private field 'internalMap' to null using reflection
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(trackingMap, null);
        // Invoking size() should now throw a NullPointerException
        NullPointerException npe = assertThrows(NullPointerException.class, trackingMap::size, "Calling size() when internalMap is null should throw NPE");
        // Optional: verify message contains something indicative (implementation‑specific)
        assertTrue(npe.getMessage() == null || npe.getMessage().isEmpty(), "NPE message is not expected to contain custom text");
    }
}
