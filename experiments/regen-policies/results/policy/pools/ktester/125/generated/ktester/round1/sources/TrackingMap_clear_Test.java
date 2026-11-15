package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_clear_Test {

    /**
     * Backing map used for constructing the TrackingMap under test.
     */
    private Map<String, String> backingMap;

    /**
     * The TrackingMap instance that will be cleared in each test.
     */
    private TrackingMap<String, String> trackingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh backing map and TrackingMap before each test.
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear references to help GC; not strictly necessary.
        trackingMap = null;
        backingMap = null;
    }

    /**
     * Supplies the different clear‑behaviour scenarios.
     */
    private static Stream<Arguments> clearBehaviorProvider() {
        return Stream.of(// 1. clear on a newly constructed (empty) map.
        Arguments.of((Consumer<TrackingMap<String, Integer>>) (m) -> {
        }, /* no setup */
        (Consumer<TrackingMap<String, Integer>>) (m) -> {
            assertEquals(0, m.size(), "size() must be 0 after clear on empty map");
            assertTrue(m.keysUsed().isEmpty(), "keysUsed() must be empty after clear on empty map");
        }, "clear() on an empty map leaves it empty and does not affect the read‑keys set"), // 2. clear after inserting entries but without any reads.
        Arguments.of((Consumer<TrackingMap<String, Integer>>) (m) -> {
            m.put("a", 1);
            m.put("b", 2);
        }, (Consumer<TrackingMap<String, Integer>>) (m) -> {
            assertEquals(0, m.size(), "size() must be 0 after clear");
            assertTrue(m.keysUsed().isEmpty(), "keysUsed() must be empty after clear");
        }, "clear() after inserting entries (\"a\"->1, \"b\"->2) removes all entries"), // 3. clear after reading one of the inserted keys.
        Arguments.of((Consumer<TrackingMap<String, Integer>>) (m) -> {
            m.put("x", 10);
            m.put("y", 20);
            m.get("x");
        }, // mark "x" as read
        (Consumer<TrackingMap<String, Integer>>) (m) -> {
            assertEquals(0, m.size(), "size() must be 0 after clear");
            assertTrue(m.keysUsed().isEmpty(), "keysUsed() must be empty after clear");
        }, "clear() after reading key \"x\" via get() empties map and read‑keys"), // 4. clear after reading all inserted keys.
        Arguments.of((Consumer<TrackingMap<String, Integer>>) (m) -> {
            m.put("k1", 100);
            m.put("k2", 200);
            m.put("k3", 300);
            m.get("k1");
            m.get("k2");
            m.get("k3");
        }, (Consumer<TrackingMap<String, Integer>>) (m) -> {
            assertEquals(0, m.size(), "size() must be 0 after clear");
            assertTrue(m.keysUsed().isEmpty(), "keysUsed() must be empty after clear");
        }, "clear() after reading all keys clears map and used‑keys set"), // 5. clear then add a new entry; keysUsed stays empty until the new key is accessed.
        Arguments.of((Consumer<TrackingMap<String, Integer>>) (m) -> {
            m.put("p", 1);
            m.put("q", 2);
            m.clear();
            m.put("new", 999);
        }, // Populate, clear, then add a new entry.
        // bring map to empty state
        (Consumer<TrackingMap<String, Integer>>) (m) -> {
            assertEquals(1, m.size(), "size() must be 1 after adding new entry post‑clear");
            assertEquals(999, m.get("new"));
            Set<String> used = m.keysUsed();
            assertEquals(1, used.size(), "Only the newly accessed key should be recorded");
            assertTrue(used.contains("new"));
        }, // After the get() above, the key becomes "used".
        "clear() followed by a new put/get works and starts a fresh read‑keys set"));
    }

    /**
     * Verify that {@code clear()} removes all entries from the internal map
     * and also clears the set of read keys.
     */
    @Test
    void testClearEmptiesMapAndReadKeys() {
        // GIVEN: a map with two entries and both keys have been accessed.
        trackingMap.put("alpha", "A");
        trackingMap.put("beta", "B");
        // Access both keys to populate readKeys.
        trackingMap.get("alpha");
        trackingMap.containsKey("beta");
        // Sanity checks before clear.
        assertEquals(2, trackingMap.size(), "Pre‑condition: map should contain two entries");
        Set<String> usedBefore = trackingMap.keysUsed();
        assertTrue(usedBefore.contains("alpha"));
        assertTrue(usedBefore.contains("beta"));
        assertEquals(2, usedBefore.size(), "Both keys should be recorded as used");
        // WHEN: clear is invoked.
        trackingMap.clear();
        // THEN: map is empty and no read keys are recorded.
        assertEquals(0, trackingMap.size(), "Map should be empty after clear");
        assertTrue(trackingMap.isEmpty(), "isEmpty() must return true after clear");
        assertTrue(trackingMap.keysUsed().isEmpty(), "Read‑keys set must be cleared");
    }

    /**
     * Verify that after a clear, the TrackingMap can be reused normally:
     * new entries can be added and accessed without residual state.
     */
    @Test
    void testClearResetsStateForSubsequentOperations() {
        // GIVEN: populate map and read keys, then clear.
        trackingMap.put("x", "X");
        trackingMap.put("y", "Y");
        trackingMap.get("x");
        trackingMap.clear();
        // WHEN: add new entries after clear.
        trackingMap.put("z", "Z");
        trackingMap.get("z");
        // THEN: only the new entry exists and its key is the sole used key.
        assertEquals(1, trackingMap.size(), "Only one entry should exist after re‑adding");
        assertEquals("Z", trackingMap.get("z"));
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "Only the newly accessed key should be recorded");
        assertTrue(used.contains("z"));
    }

    /**
     * Verify that invoking {@code clear()} on an already empty TrackingMap
     * does not throw and leaves the map in a consistent empty state.
     */
    @Test
    void testClearOnAlreadyEmptyMap() {
        // GIVEN: a freshly constructed (empty) map.
        assertTrue(trackingMap.isEmpty(), "Map should start empty");
        // WHEN: clear is called.
        trackingMap.clear();
        // THEN: map remains empty and no keys are recorded.
        assertTrue(trackingMap.isEmpty(), "Map should remain empty after clear");
        assertEquals(0, trackingMap.size(), "Size must be zero after clear");
        assertTrue(trackingMap.keysUsed().isEmpty(), "No read keys should be present");
    }

    /**
     * Parameterized test that covers the various clear‑behaviour scenarios
     * described in the JSON test cases.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("clearBehaviorProvider")
    void testClearBehavior(Consumer<TrackingMap<String, Integer>> setup, Consumer<TrackingMap<String, Integer>> afterClearAssertions, String description) {
        // Use an Integer‑valued map for these scenarios.
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        // Apply the scenario‑specific setup (populate, read, etc.).
        setup.accept(map);
        // Perform the operation under test.
        map.clear();
        // Verify expectations after clear.
        afterClearAssertions.accept(map);
    }

    /**
     * Verify that invoking {@code clear()} multiple times has no adverse effect.
     */
    @Test
    void testClearIdempotence() {
        // GIVEN: a map with some entries.
        trackingMap.put("p", "1");
        trackingMap.put("q", "2");
        // mark a key as used
        trackingMap.get("p");
        // WHEN: clear is called twice.
        trackingMap.clear();
        // first clear assertions
        assertEquals(0, trackingMap.size(), "After first clear size should be 0");
        assertTrue(trackingMap.keysUsed().isEmpty(), "After first clear keysUsed should be empty");
        // second clear
        trackingMap.clear();
        // THEN: still empty and no used keys.
        assertEquals(0, trackingMap.size(), "After second clear size should still be 0");
        assertTrue(trackingMap.keysUsed().isEmpty(), "After second clear keysUsed should remain empty");
    }

    /**
     * Inject a {@code null} into the private final field {@code internalMap}
     * and verify that {@code clear()} throws {@link NullPointerException}.
     */
    @Test
    void testClearThrowsNPEWhenInternalMapIsNull() throws Exception {
        // Use reflection to set internalMap to null.
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(trackingMap, null);
        // Expect NPE when clear() attempts to delegate to internalMap.clear().
        assertThrows(NullPointerException.class, trackingMap::clear, "clear() should throw NullPointerException when internalMap is null");
    }

    /**
     * Inject a {@code null} into the private final field {@code readKeys}
     * and verify that {@code clear()} throws {@link NullPointerException}.
     */
    @Test
    void testClearThrowsNPEWhenReadKeysIsNull() throws Exception {
        // Use reflection to set readKeys to null.
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        readKeysField.set(trackingMap, null);
        // Expect NPE when clear() attempts to call readKeys.clear().
        assertThrows(NullPointerException.class, trackingMap::clear, "clear() should throw NullPointerException when readKeys is null");
    }

    /**
     * Replace the internal map with an unmodifiable map and verify that
     * {@code clear()} propagates {@link UnsupportedOperationException}.
     */
    @Test
    void testClearThrowsUOEWhenInternalMapIsUnmodifiable() throws Exception {
        // Prepare an unmodifiable map.
        Map<String, String> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        // Inject it via reflection.
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(trackingMap, unmodifiable);
        // clear() should attempt to clear the unmodifiable map and thus throw.
        assertThrows(UnsupportedOperationException.class, trackingMap::clear, "clear() should propagate UnsupportedOperationException from unmodifiable internalMap");
    }

    /**
     * Replace the read‑keys set with an unmodifiable set and verify that
     * {@code clear()} throws {@link UnsupportedOperationException}.
     */
    @Test
    void testClearThrowsUOEWhenReadKeysIsUnmodifiable() throws Exception {
        // Prepare an unmodifiable set.
        Set<String> unmodifiableSet = Collections.unmodifiableSet(new HashSet<>());
        // Inject it via reflection.
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        readKeysField.set(trackingMap, unmodifiableSet);
        // clear() should attempt to clear the unmodifiable set and thus throw.
        assertThrows(UnsupportedOperationException.class, trackingMap::clear, "clear() should propagate UnsupportedOperationException from unmodifiable readKeys");
    }
}
