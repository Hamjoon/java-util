package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

class TrackingMap_expungeUnused_Test {

    private Map<String, Integer> backingMap;

    private TrackingMap<String, Integer> trackingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Create a simple mutable backing map and wrap it with TrackingMap.
        backingMap = new HashMap<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly necessary.
        backingMap = null;
        trackingMap = null;
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// Subset of keys read
        Arguments.of(// keys we will read
        Set.of("one", "two"), // expected keys after expunge
        Set.of("one", "two"), "When readKeys contains a subset of the keys present in internalMap, expungeUnused should retain only those keys."), // All keys read
        Arguments.of(Set.of("one", "two", "three"), Set.of("one", "two", "three"), "When readKeys contains all keys from internalMap, expungeUnused should not remove any entries."), // No keys read (empty readKeys)
        Arguments.of(Collections.emptySet(), Collections.emptySet(), "When readKeys is empty, expungeUnused should clear all entries from internalMap."));
    }

    @Test
    void expungeUnused_removesOnlyUnreadEntries() {
        // GIVEN – we read two of the three keys
        assertEquals(1, trackingMap.get("one"));
        assertEquals(2, trackingMap.get("two"));
        // "three" is never read
        // WHEN – we expunge unused entries
        trackingMap.expungeUnused();
        // THEN – only the read keys remain
        assertEquals(2, trackingMap.size(), "Map should contain only the two read entries");
        assertTrue(trackingMap.containsKey("one"));
        assertTrue(trackingMap.containsKey("two"));
        assertFalse(trackingMap.containsKey("three"));
        // The backing map should reflect the same state
        assertEquals(2, backingMap.size());
        assertFalse(backingMap.containsKey("three"));
    }

    @Test
    void expungeUnused_whenNoKeysRead_clearsMap() {
        // GIVEN – we do NOT read any keys
        // WHEN
        trackingMap.expungeUnused();
        // THEN – the map should be empty
        assertTrue(trackingMap.isEmpty(), "Map should be empty after expunge with no reads");
        assertTrue(backingMap.isEmpty(), "Backing map should also be empty");
    }

    /**
     * Replaces the private final {@code readKeys} field with the supplied set.
     *
     * @param map   the {@link TrackingMap} instance to modify
     * @param keys  the new {@link Set} that should become {@code readKeys}
     */
    @SuppressWarnings("unchecked")
    private static <K, V> void setReadKeys(TrackingMap<K, V> map, Set<K> keys) {
        try {
            Field field = TrackingMap.class.getDeclaredField("readKeys");
            field.setAccessible(true);
            // Remove the final modifier so we can assign a new value.
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(map, keys);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieves the current {@code readKeys} set from the {@link TrackingMap}.
     *
     * @return a copy of the internal {@code readKeys} set
     */
    @SuppressWarnings("unchecked")
    private static <K, V> Set<K> getReadKeys(TrackingMap<K, V> map) {
        try {
            Field field = TrackingMap.class.getDeclaredField("readKeys");
            field.setAccessible(true);
            return new HashSet<>((Set<K>) field.get(map));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicFunctionalityProvider")
    void expungeUnused_basicFunctionality(Set<String> keysToRead, Set<String> expectedRemaining, String description) {
        // GIVEN – optionally read a subset of keys
        keysToRead.forEach(k -> {
            // Access via get() to populate readKeys automatically
            assertEquals(backingMap.get(k), trackingMap.get(k));
        });
        // WHEN – force readKeys to exactly the set we want (covers the empty‑read case)
        setReadKeys(trackingMap, new HashSet<>(keysToRead));
        trackingMap.expungeUnused();
        // THEN – internal map contains exactly the expected keys
        assertEquals(expectedRemaining.size(), trackingMap.size(), "Map size should match expected remaining keys");
        expectedRemaining.forEach(k -> assertTrue(trackingMap.containsKey(k), "Expected key '" + k + "' to be present after expunge"));
        // Ensure no unexpected keys remain
        Set<String> unexpected = new HashSet<>(backingMap.keySet());
        unexpected.removeAll(expectedRemaining);
        unexpected.forEach(k -> assertFalse(trackingMap.containsKey(k), "Unexpected key '" + k + "' should have been removed"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void expungeUnused_internalMapEmpty_readKeysNonEmpty() {
        // GIVEN – an empty backing map but a non‑empty readKeys set
        // now internalMap is empty
        backingMap.clear();
        setReadKeys(trackingMap, new HashSet<>(Set.of("ghost")));
        // WHEN
        trackingMap.expungeUnused();
        // THEN – internalMap stays empty
        assertTrue(trackingMap.isEmpty(), "Empty internalMap should stay empty even if readKeys has entries");
    }

    @Test
    void expungeUnused_readKeysContainNonExistingKeys() {
        // GIVEN – internalMap has three entries, readKeys contains an extra key "four"
        setReadKeys(trackingMap, new HashSet<>(Set.of("one", "two", "three", "four")));
        // WHEN
        trackingMap.expungeUnused();
        // THEN – only the original three keys remain (no removal)
        assertEquals(3, trackingMap.size());
        assertTrue(trackingMap.keySet().containsAll(Set.of("one", "two", "three")));
        assertFalse(trackingMap.keySet().contains("four"));
    }

    @Test
    void expungeUnused_nullKeyRetainedWhenReadKeysContainsNull() {
        // GIVEN – map contains a null key, readKeys also contains null
        backingMap.put(null, 0);
        // Force readKeys to contain null (the public API does not expose null reads)
        setReadKeys(trackingMap, new HashSet<>(Set.of((String) null)));
        // WHEN
        trackingMap.expungeUnused();
        // THEN – null key entry is retained
        assertTrue(trackingMap.containsKey(null));
        assertEquals(0, trackingMap.get(null));
    }

    @Test
    void expungeUnused_nullKeyIgnoredWhenNotInReadKeys() {
        // GIVEN – map contains a null key, but readKeys does NOT contain null
        backingMap.put(null, 0);
        setReadKeys(trackingMap, new HashSet<>(Set.of("one", "two", "three")));
        // WHEN
        trackingMap.expungeUnused();
        // THEN – null key entry is removed
        assertFalse(trackingMap.containsKey(null));
        // only the three non‑null keys remain
        assertEquals(3, trackingMap.size());
    }

    @Test
    void expungeUnused_unmodifiableReadKeysWorksCorrectly() {
        // GIVEN – an unmodifiable readKeys set containing a subset of keys
        Set<String> modifiable = new HashSet<>(Set.of("one", "three"));
        Set<String> unmodifiable = Collections.unmodifiableSet(modifiable);
        setReadKeys(trackingMap, unmodifiable);
        // WHEN
        trackingMap.expungeUnused();
        // THEN – only the keys present in the unmodifiable set remain
        assertEquals(2, trackingMap.size());
        assertTrue(trackingMap.containsKey("one"));
        assertTrue(trackingMap.containsKey("three"));
        assertFalse(trackingMap.containsKey("two"));
    }

    @Test
    void expungeUnused_retainsEntriesWithNullValuesWhenKeyIsRead() {
        // GIVEN – map contains a key with a null value, and that key is in readKeys
        backingMap.put("nullVal", null);
        setReadKeys(trackingMap, new HashSet<>(Set.of("nullVal")));
        // WHEN
        trackingMap.expungeUnused();
        // THEN – entry is retained despite null value
        assertTrue(trackingMap.containsKey("nullVal"));
        assertNull(trackingMap.get("nullVal"));
        // original 3 + the null‑value entry
        assertEquals(4, trackingMap.size());
    }

    @Test
    void expungeUnused_readKeysRemainsUnchangedAfterExpunge() {
        // GIVEN – read a couple of keys to populate readKeys, then capture a copy
        trackingMap.get("one");
        trackingMap.get("two");
        Set<String> before = getReadKeys(trackingMap);
        // WHEN
        trackingMap.expungeUnused();
        // THEN – the readKeys set is exactly the same instance content (no mutation)
        Set<String> after = getReadKeys(trackingMap);
        assertEquals(before, after, "readKeys set should remain unchanged after expungeUnused");
    }
}
