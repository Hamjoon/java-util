package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class TrackingMap_expungeUnused_Test {

    /**
     * The map under test – a TrackingMap that wraps a plain {@link HashMap}.
     */
    private TrackingMap<String, Integer> trackingMap;

    /**
     * A fresh backing map for each test.
     */
    private Map<String, Integer> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Create a new backing map and a TrackingMap wrapper before each test.
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear references – not strictly necessary but keeps the test tidy.
        trackingMap.clear();
        backingMap.clear();
    }

    /**
     * Verify that calling {@code expungeUnused()} on an empty map leaves it empty.
     */
    @Test
    void expungeUnusedOnEmptyMap() {
        // GIVEN: an empty TrackingMap (setup in @BeforeEach)
        assertTrue(trackingMap.isEmpty(), "Map should start empty");
        // WHEN
        trackingMap.expungeUnused();
        // THEN
        assertTrue(trackingMap.isEmpty(), "Map should remain empty after expunge");
        assertEquals(0, trackingMap.size());
    }

    /**
     * Verify that keys accessed via {@code get} are retained while others are removed.
     */
    @Test
    void expungeUnusedKeepsAccessedKeys() {
        // GIVEN
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        trackingMap.put("c", 3);
        // Access only "b"
        assertEquals(2, trackingMap.get("b"));
        // WHEN
        trackingMap.expungeUnused();
        // THEN
        assertEquals(1, trackingMap.size(), "Only one entry should remain");
        assertTrue(trackingMap.containsKey("b"));
        assertFalse(trackingMap.containsKey("a"));
        assertFalse(trackingMap.containsKey("c"));
    }

    /**
     * Verify that keys accessed via {@code containsKey} are also retained.
     */
    @Test
    void expungeUnusedKeepsContainsKeyAccessed() {
        // GIVEN
        trackingMap.put("x", 10);
        trackingMap.put("y", 20);
        trackingMap.put("z", 30);
        // Access "y" via containsKey
        assertTrue(trackingMap.containsKey("y"));
        // WHEN
        trackingMap.expungeUnused();
        // THEN
        assertEquals(1, trackingMap.size());
        assertTrue(trackingMap.containsKey("y"));
        assertFalse(trackingMap.containsKey("x"));
        assertFalse(trackingMap.containsKey("z"));
    }

    /**
     * Verify that accessing a non‑existent key still records the key in {@code keysUsed},
     * but does not prevent other entries from being removed.
     */
    @Test
    void expungeUnusedWithNonExistentAccess() {
        // GIVEN
        trackingMap.put("p", 100);
        trackingMap.put("q", 200);
        // Access a key that does not exist
        assertNull(trackingMap.get("nonexistent"));
        // keysUsed should now contain the nonexistent key
        assertTrue(trackingMap.keysUsed().contains("nonexistent"));
        // WHEN
        trackingMap.expungeUnused();
        // THEN
        // No real entries were accessed, so both "p" and "q" should be removed.
        assertTrue(trackingMap.isEmpty());
        assertEquals(0, trackingMap.size());
    }

    /**
     * Verify that calling {@code expungeUnused()} multiple times is idempotent.
     */
    @Test
    void expungeUnusedIdempotent() {
        // GIVEN
        trackingMap.put("k1", 1);
        trackingMap.put("k2", 2);
        trackingMap.put("k3", 3);
        // only "k2" is accessed
        trackingMap.get("k2");
        // WHEN – first expunge
        trackingMap.expungeUnused();
        // THEN – only "k2" remains
        assertEquals(1, trackingMap.size());
        assertTrue(trackingMap.containsKey("k2"));
        // WHEN – second expunge (no additional accesses)
        trackingMap.expungeUnused();
        // THEN – state should be unchanged
        assertEquals(1, trackingMap.size());
        assertTrue(trackingMap.containsKey("k2"));
    }

    /**
     * Verify that {@code expungeUnused()} does not affect keys that were accessed
     * before a previous expunge (i.e., the usage set is cleared after each call).
     */
    @Test
    void expungeUnusedResetsUsageTracking() {
        // GIVEN
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        trackingMap.put("c", 3);
        // access "a"
        trackingMap.get("a");
        // should keep only "a"
        trackingMap.expungeUnused();
        // Verify state after first expunge
        assertEquals(1, trackingMap.size());
        assertTrue(trackingMap.containsKey("a"));
        // Add new entries and access a different one
        trackingMap.put("d", 4);
        trackingMap.put("e", 5);
        // access "e"
        trackingMap.get("e");
        // WHEN
        trackingMap.expungeUnused();
        // THEN – only "e" (newly accessed) should survive together with previously retained "a"
        assertEquals(2, trackingMap.size());
        assertTrue(trackingMap.containsKey("a"));
        assertTrue(trackingMap.containsKey("e"));
        assertFalse(trackingMap.containsKey("d"));
    }

    /**
     * Basic functionality – when no keys have been accessed (readKeys empty) the map should become empty.
     */
    @Test
    void expungeUnusedRemovesAllWhenReadKeysEmpty() {
        // GIVEN
        trackingMap.put("one", 1);
        trackingMap.put("two", 2);
        trackingMap.put("three", 3);
        // No accesses → readKeys is empty
        // WHEN
        trackingMap.expungeUnused();
        // THEN
        assertTrue(trackingMap.isEmpty(), "All entries should be removed when no keys were accessed");
        assertEquals(0, trackingMap.size());
    }

    /**
     * Basic functionality – when every key has been accessed the map should stay unchanged.
     */
    @Test
    void expungeUnusedKeepsAllWhenAllKeysAccessed() {
        // GIVEN
        trackingMap.put("a", 10);
        trackingMap.put("b", 20);
        trackingMap.put("c", 30);
        // Access all keys
        trackingMap.get("a");
        trackingMap.get("b");
        trackingMap.get("c");
        // WHEN
        trackingMap.expungeUnused();
        // THEN
        assertEquals(3, trackingMap.size(), "All entries should be retained");
        assertTrue(trackingMap.containsKey("a"));
        assertTrue(trackingMap.containsKey("b"));
        assertTrue(trackingMap.containsKey("c"));
    }

    /**
     * Basic functionality – subset of keys accessed should be retained.
     */
    @Test
    void expungeUnusedKeepsSubsetOfAccessedKeys() {
        // GIVEN
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        trackingMap.put("c", 3);
        // Access "a" and "c"
        trackingMap.get("a");
        trackingMap.get("c");
        // WHEN
        trackingMap.expungeUnused();
        // THEN
        Set<String> remaining = trackingMap.keySet();
        assertEquals(2, remaining.size());
        assertTrue(remaining.containsAll(Arrays.asList("a", "c")));
        assertFalse(remaining.contains("b"));
    }

    /**
     * Basic functionality – readKeys may contain keys not present in the map.
     * The map must stay unchanged and no new entries are added.
     */
    @Test
    void expungeUnusedIgnoresNonexistentReadKeys() {
        // GIVEN
        trackingMap.put("x", 100);
        trackingMap.put("y", 200);
        trackingMap.put("z", 300);
        // Simulate external addition to readKeys via reflection
        try {
            Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
            readKeysField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> readKeys = (Set<String>) readKeysField.get(trackingMap);
            readKeys.add("nonexistent");
        } catch (Exception e) {
            fail("Reflection failed: " + e);
        }
        // WHEN
        trackingMap.expungeUnused();
        // THEN
        assertEquals(3, trackingMap.size(), "Map size should remain unchanged");
        assertTrue(trackingMap.containsKey("x"));
        assertTrue(trackingMap.containsKey("y"));
        assertTrue(trackingMap.containsKey("z"));
    }

    /**
     * Basic functionality – when the wrapped map is already empty, expungeUnused does nothing.
     */
    @Test
    void expungeUnusedOnAlreadyEmptyWrappedMap() {
        // GIVEN: backingMap is empty (created in @BeforeEach)
        assertTrue(trackingMap.isEmpty());
        // WHEN
        trackingMap.expungeUnused();
        // THEN
        assertTrue(trackingMap.isEmpty());
        assertEquals(0, trackingMap.size());
    }

    /**
     * Verify that the internal {@code readKeys} set remains unchanged after expunge.
     */
    @Test
    void readKeysImmutabilityAfterExpunge() {
        // GIVEN
        trackingMap.put("x", 1);
        trackingMap.put("y", 2);
        // Access both keys
        trackingMap.get("x");
        trackingMap.get("y");
        Set<String> before = new HashSet<>(trackingMap.keysUsed());
        // WHEN
        trackingMap.expungeUnused();
        // THEN
        Set<String> after = trackingMap.keysUsed();
        assertEquals(before, after, "readKeys should remain the same after expunge");
        assertEquals(2, trackingMap.size(), "Both entries should still be present");
    }

    /**
     * Verify that a {@code NullPointerException} is thrown when the internal map is null.
     */
    @Test
    void expungeUnusedNullInternalMapThrowsNPE() {
        // GIVEN
        trackingMap.setWrappedMap(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, trackingMap::expungeUnused, "Calling expungeUnused with a null internal map should throw NPE");
    }

    /**
     * Verify that a {@code NullPointerException} is thrown when {@code readKeys} is null.
     * The test uses reflection to set the private field to null.
     */
    @Test
    void expungeUnusedNullReadKeysThrowsNPE() throws Exception {
        // GIVEN
        trackingMap.put("a", 1);
        // Use reflection to nullify the readKeys field
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        readKeysField.set(trackingMap, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, trackingMap::expungeUnused, "Calling expungeUnused with null readKeys should throw NPE");
    }

    /**
     * Verify that an {@code UnsupportedOperationException} is thrown when the wrapped map
     * is unmodifiable (its keySet does not support {@code retainAll}).
     */
    @Test
    void expungeUnusedWithUnmodifiableMapThrowsUnsupportedOperation() {
        // GIVEN
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(new HashMap<>(Map.of("a", 1, "b", 2, "c", 3)));
        trackingMap.setWrappedMap(unmodifiable);
        // Access only "b"
        trackingMap.get("b");
        // WHEN / THEN
        assertThrows(UnsupportedOperationException.class, trackingMap::expungeUnused, "expungeUnused on an unmodifiable map should throw UnsupportedOperationException");
    }
}
