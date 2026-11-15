package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.*;

class TrackingMap_clear_Test {

    private TrackingMap<String, Integer> trackingMap;

    private Map<String, Integer> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh backing map with a few entries for each test.
        backingMap = new HashMap<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        // Construct the TrackingMap that wraps the backing map.
        trackingMap = new TrackingMap<>(backingMap);
        backingMap = new HashMap<>();
        backingMap.put("a", 10);
        backingMap.put("b", 20);
        // include a null key
        backingMap.put(null, 0);
        // include a null value
        backingMap.put("c", null);
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear references to help GC; not strictly necessary.
        trackingMap = null;
        backingMap = null;
        trackingMap = null;
        backingMap = null;
    }

    @Test
    void clearShouldEmptyInternalMapAndReadKeys() {
        // GIVEN: a key has been read, which populates the internal read‑keys set.
        // read "one"
        assertEquals(1, trackingMap.get("one"));
        assertTrue(trackingMap.keysUsed().contains("one"), "keysUsed should contain the accessed key before clear");
        // AND: the map contains entries.
        assertEquals(3, trackingMap.size(), "Map should contain three entries before clear");
        // WHEN: clear is invoked.
        trackingMap.clear();
        // THEN: the internal map is empty.
        assertEquals(0, trackingMap.size(), "Map size should be zero after clear");
        // AND: the set of read keys is cleared.
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed should be empty after clear");
    }

    @Test
    void clearOnAlreadyEmptyMapDoesNotThrow() {
        // GIVEN: an empty TrackingMap.
        // ensure underlying map is empty
        backingMap.clear();
        trackingMap = new TrackingMap<>(backingMap);
        assertEquals(0, trackingMap.size());
        // WHEN: clear is called.
        trackingMap.clear();
        // THEN: no exception and map remains empty.
        assertEquals(0, trackingMap.size(), "Map should remain empty after clear");
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed should be empty after clear on an empty map");
    }

    /**
     * ---------- Basic behavior when readKeys was never populated ----------
     */
    @Test
    void clearWhenNoKeysReadShouldEmptyMapAndReadKeys() {
        // No call to get() -> readKeys set stays empty.
        assertTrue(trackingMap.keysUsed().isEmpty(), "pre‑condition: keysUsed should be empty before clear");
        trackingMap.clear();
        assertEquals(0, trackingMap.size(), "size must be zero after clear");
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed must be empty after clear even if it was never populated");
    }

    /**
     * ---------- EntrySet view reflects clearing ----------
     */
    @Test
    void entrySetViewBecomesEmptyAfterClear() {
        Set<Map.Entry<String, Integer>> view = trackingMap.entrySet();
        assertEquals(4, view.size(), "pre‑condition: entrySet should contain all entries");
        trackingMap.clear();
        assertEquals(0, view.size(), "the same entrySet instance must report size 0 after clear");
        assertTrue(view.isEmpty(), "entrySet should be empty after clear");
    }

    /**
     * ---------- Wrapped map identity is preserved ----------
     */
    @Test
    void wrappedMapReferenceRemainsSameAfterClear() {
        Map<String, Integer> wrappedBefore = trackingMap.getWrappedMap();
        assertSame(backingMap, wrappedBefore, "pre‑condition: getWrappedMap should return the original map");
        trackingMap.clear();
        Map<String, Integer> wrappedAfter = trackingMap.getWrappedMap();
        assertSame(wrappedBefore, wrappedAfter, "clear must not replace the underlying map instance");
        assertEquals(0, wrappedAfter.size(), "underlying map size must be zero after clear");
    }

    /**
     * ---------- Subsequent put works as if freshly constructed ----------
     */
    @Test
    void putAfterClearWorksAndKeysUsedIsReset() {
        // read a key to populate readKeys, then clear
        trackingMap.get("a");
        assertFalse(trackingMap.keysUsed().isEmpty(), "pre‑condition: keysUsed contains a key after get");
        trackingMap.clear();
        // map should behave like a new instance
        trackingMap.put("newKey", 99);
        assertEquals(1, trackingMap.size(), "size should reflect the newly added entry");
        assertEquals(99, trackingMap.get("newKey"));
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed must be empty after clear before any further reads");
    }

    /**
     * ---------- clear on map with null key/value ----------
     */
    @Test
    void clearRemovesNullKeyAndNullValueEntries() {
        assertTrue(trackingMap.containsKey(null), "pre‑condition: map contains a null key");
        assertTrue(trackingMap.containsValue(null), "pre‑condition: map contains a null value");
        trackingMap.clear();
        assertEquals(0, trackingMap.size(), "map must be empty after clear, removing null entries as well");
        assertFalse(trackingMap.containsKey(null));
        assertFalse(trackingMap.containsValue(null));
    }

    /**
     * ---------- readKeys may contain stale keys (removed before clear) ----------
     */
    @Test
    void clearWhenReadKeysContainStaleKeys() {
        // read a key, then remove it directly from the backing map
        trackingMap.get("a");
        assertTrue(trackingMap.keysUsed().contains("a"));
        // simulate external removal
        backingMap.remove("a");
        // now clear – readKeys still holds "a"
        trackingMap.clear();
        assertEquals(0, trackingMap.size(), "internal map must be empty after clear");
        assertTrue(trackingMap.keysUsed().isEmpty(), "readKeys must be cleared even if it contained stale entries");
    }

    /**
     * ---------- Exception scenarios – null internalMap ----------
     */
    @Test
    void clearWithNullInternalMapThrowsNPE() throws Exception {
        // Use reflection to set internalMap to null
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(trackingMap, null);
        assertThrows(NullPointerException.class, trackingMap::clear, "clear should throw NPE when internalMap is null");
    }

    /**
     * ---------- Exception scenarios – null readKeys ----------
     */
    @Test
    void clearWithNullReadKeysThrowsNPE() throws Exception {
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        readKeysField.set(trackingMap, null);
        assertThrows(NullPointerException.class, trackingMap::clear, "clear should throw NPE when readKeys is null");
    }

    /**
     * ---------- Exception scenarios – unmodifiable internal map ----------
     */
    @Test
    void clearOnUnmodifiableInternalMapPropagatesUOE() {
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(new HashMap<>(backingMap));
        TrackingMap<String, Integer> map = new TrackingMap<>(unmodifiable);
        assertThrows(UnsupportedOperationException.class, map::clear, "clear must propagate UnsupportedOperationException from an unmodifiable internal map");
    }

    /**
     * ---------- Exception scenarios – unmodifiable readKeys set ----------
     */
    @Test
    void clearOnUnmodifiableReadKeysPropagatesUOE() throws Exception {
        // Replace readKeys with an unmodifiable set via reflection
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        Set<String> unmodifiableSet = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("a", "b")));
        readKeysField.set(trackingMap, unmodifiableSet);
        assertThrows(UnsupportedOperationException.class, trackingMap::clear, "clear must propagate UnsupportedOperationException when readKeys.clear() is unsupported");
    }

    /**
     * ---------- Exception scenarios – custom map that throws RuntimeException ----------
     */
    @Test
    void clearPropagatesRuntimeExceptionFromWrappedMap() {
        Map<String, Integer> explodingMap = new HashMap<String, Integer>() {

            @Override
            public void clear() {
                throw new RuntimeException("boom");
            }
        };
        TrackingMap<String, Integer> map = new TrackingMap<>(explodingMap);
        RuntimeException ex = assertThrows(RuntimeException.class, map::clear, "clear must propagate RuntimeException thrown by the wrapped map");
        assertEquals("boom", ex.getMessage());
    }

    /**
     * ---------- Parameterized test: clear on already empty map ----------
     */
    @ParameterizedTest(name = "clear on empty map iteration {index}")
    @ValueSource(booleans = { true, false })
    void clearOnEmptyMapDoesNotThrow(boolean readKeyFirst) {
        // Ensure map is empty
        backingMap.clear();
        trackingMap = new TrackingMap<>(backingMap);
        assertEquals(0, trackingMap.size());
        if (readKeyFirst) {
            // invoke get on a non‑existent key to potentially populate readKeys
            assertNull(trackingMap.get("nonexistent"));
        }
        // clear should not throw and both structures stay empty
        assertDoesNotThrow(trackingMap::clear);
        assertEquals(0, trackingMap.size());
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed must be empty after clear on an empty map");
    }
}
