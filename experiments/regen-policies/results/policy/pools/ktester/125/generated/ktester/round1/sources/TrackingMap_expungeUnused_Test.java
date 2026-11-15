package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_expungeUnused_Test {

    private TrackingMap<String, String> trackingMap;

    private Map<String, String> backingMap;

    /* --------------------------------------------------------------
     * Group: custom key equality
     * -------------------------------------------------------------- */
    private static final class CustomKey {

        private final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Key{id=" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple HashMap as the backing map for most tests.
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test tidy.
        trackingMap = null;
        backingMap = null;
    }

    /* --------------------------------------------------------------
     * Helper utilities
     * -------------------------------------------------------------- */
    private static Set<String> readKeysOf(TrackingMap<?, ?> map) throws Exception {
        Field f = TrackingMap.class.getDeclaredField("readKeys");
        f.setAccessible(true);
        //noinspection unchecked
        return (Set<String>) f.get(map);
    }

    private static void setReadKeys(TrackingMap<?, ?> map, Set<?> newSet) throws Exception {
        Field f = TrackingMap.class.getDeclaredField("readKeys");
        f.setAccessible(true);
        f.set(map, newSet);
    }

    private static void setInternalMap(TrackingMap<?, ?> map, Map<?, ?> newMap) throws Exception {
        Field f = TrackingMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(map, newMap);
    }

    private static Stream<Arguments> behaviorScenarios() {
        return Stream.of(// readKeys empty → map becomes empty
        Arguments.of("readKeys empty, internal map contains entries → map becomes empty", Map.of("A", "alpha", "B", "beta"), Set.of(), Set.of()), // readKeys contains all keys → map unchanged
        Arguments.of("readKeys includes all keys → map unchanged", Map.of("A", "alpha", "B", "beta", "C", "charlie"), Set.of("A", "B", "C"), Set.of("A", "B", "C")), // subset of keys → only intersecting remain
        Arguments.of("readKeys is a proper subset of map keys → map retains intersecting entries", Map.of("A", "alpha", "B", "beta", "C", "charlie"), Set.of("A", "C"), Set.of("A", "C")), // extra non‑existent keys in readKeys → ignored
        Arguments.of("readKeys has extra non‑existent keys → map retains only existing intersecting keys", Map.of("A", "alpha", "B", "beta", "C", "charlie"), Set.of("A", "C", "D", "E"), Set.of("A", "C")), // empty internal map with non‑empty readKeys → stays empty
        Arguments.of("empty internal map, non‑empty readKeys → map remains empty", Map.of(), Set.of("X", "Y"), Set.of()), // both empty → stays empty
        Arguments.of("both internal map and readKeys empty → map stays empty", Map.of(), Set.of(), Set.of()));
    }

    @Test
    void expungeUnusedWhenNoKeyWasAccessed() {
        trackingMap.put("a", "alpha");
        trackingMap.put("b", "beta");
        // No get/containsKey calls – nothing is marked as read.
        trackingMap.expungeUnused();
        assertEquals(0, trackingMap.size(), "All entries should be removed when none were accessed");
        assertTrue(trackingMap.isEmpty(), "Map should be empty after expunge");
    }

    @Test
    void expungeUnusedKeepsKeyAccessedViaGet() {
        trackingMap.put("first", "one");
        trackingMap.put("second", "two");
        // Access only the first key.
        assertEquals("one", trackingMap.get("first"));
        trackingMap.expungeUnused();
        assertEquals(1, trackingMap.size(), "Only the accessed entry should remain");
        assertEquals("one", trackingMap.get("first"));
        assertFalse(trackingMap.containsKey("second"));
    }

    @Test
    void expungeUnusedKeepsKeyAccessedViaContainsKey() {
        trackingMap.put("x", "ex");
        trackingMap.put("y", "why");
        // Mark "y" as read via containsKey.
        assertTrue(trackingMap.containsKey("y"));
        trackingMap.expungeUnused();
        assertEquals(1, trackingMap.size(), "Only the key read via containsKey should remain");
        assertEquals("why", trackingMap.get("y"));
        assertFalse(trackingMap.containsKey("x"));
    }

    @Test
    void expungeUnusedWithCaseInsensitiveMapAndMultipleAccesses() {
        // Replace the backing map with a CaseInsensitiveMap to verify case‑insensitive behaviour.
        trackingMap = new TrackingMap<>(new CaseInsensitiveMap<>());
        trackingMap.put("First", "value1");
        trackingMap.put("Second", "value2");
        trackingMap.put("Third", "value3");
        // Access the same logical key using different capitalisations.
        assertEquals("value1", trackingMap.get("first"));
        assertTrue(trackingMap.containsKey("FIRST"));
        assertEquals("value1", trackingMap.get("FiRsT"));
        trackingMap.expungeUnused();
        assertEquals(1, trackingMap.size(), "Only the case‑insensitively accessed key should remain");
        assertTrue(trackingMap.containsKey("First"));
        assertFalse(trackingMap.containsKey("Second"));
        assertFalse(trackingMap.containsKey("Third"));
    }

    @Test
    void expungeUnusedWhenOnlyNonexistentKeyWasAccessed() {
        trackingMap.put("p", "pee");
        trackingMap.put("q", "cue");
        // Access a key that does not exist in the map.
        assertNull(trackingMap.get("nonexistent"));
        // Alternatively, containsKey could be used; both count as a read.
        // trackingMap.containsKey("nonexistent");
        trackingMap.expungeUnused();
        // No real entries were marked as read, so the map should become empty.
        assertEquals(0, trackingMap.size(), "Map should be empty because only a nonexistent key was accessed");
        assertTrue(trackingMap.isEmpty());
    }

    @Test
    void expungeUnusedDoesNotAffectKeysUsedSet() {
        trackingMap.put("a", "A");
        trackingMap.put("b", "B");
        trackingMap.get("a");
        // Verify that the internal tracking set contains the accessed key before expunge.
        assertEquals(1, trackingMap.keysUsed().size());
        assertTrue(trackingMap.keysUsed().contains("a"));
        trackingMap.expungeUnused();
        // After expunge, the used‑keys set should remain unchanged.
        assertEquals(1, trackingMap.keysUsed().size());
        assertTrue(trackingMap.keysUsed().contains("a"));
    }

    /* --------------------------------------------------------------
     * Group: expungeUnused behavior – exhaustive set based tests
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("behaviorScenarios")
    void expungeUnusedBehavior(String description, Map<String, String> initialEntries, Set<String> preReadKeys, Set<String> expectedKeys) throws Exception {
        // populate map
        initialEntries.forEach(trackingMap::put);
        // simulate reads by directly inserting into readKeys (equivalent to get/containsKey)
        Set<String> readKeys = readKeysOf(trackingMap);
        readKeys.clear();
        readKeys.addAll(preReadKeys);
        // act
        trackingMap.expungeUnused();
        // verify size and keySet
        assertEquals(expectedKeys.size(), trackingMap.size(), "Size mismatch after expungeUnused for scenario: " + description);
        assertEquals(expectedKeys, trackingMap.keySet(), "Key set mismatch after expungeUnused for scenario: " + description);
    }

    /* --------------------------------------------------------------
     * Group: readKeys unchanged after expungeUnused
     * -------------------------------------------------------------- */
    @Test
    void expungeUnusedDoesNotModifyReadKeysSet() throws Exception {
        trackingMap.put("A", "alpha");
        trackingMap.put("B", "beta");
        // Simulate reads for A and B
        trackingMap.get("A");
        trackingMap.containsKey("B");
        Set<String> before = new HashSet<>(readKeysOf(trackingMap));
        trackingMap.expungeUnused();
        Set<String> after = readKeysOf(trackingMap);
        assertEquals(before, after, "readKeys set should remain unchanged after expungeUnused");
    }

    /* --------------------------------------------------------------
     * Group: null key handling
     * -------------------------------------------------------------- */
    @Test
    void expungeUnusedRetainsNullKeyWhenRead() {
        trackingMap.put(null, "nullValue");
        // Access null key via get (marks it as read)
        assertEquals("nullValue", trackingMap.get(null));
        trackingMap.expungeUnused();
        assertTrue(trackingMap.containsKey(null), "Null key should be retained when it was read");
        assertEquals(1, trackingMap.size());
    }

    @Test
    void expungeUnusedRemovesNullKeyWhenNotRead() {
        trackingMap.put(null, "nullValue");
        // Do NOT read the null key
        trackingMap.expungeUnused();
        assertFalse(trackingMap.containsKey(null), "Null key should be removed when not read");
        assertEquals(0, trackingMap.size());
    }

    /* --------------------------------------------------------------
     * Group: idempotence
     * -------------------------------------------------------------- */
    @Test
    void expungeUnusedIdempotent() {
        trackingMap.put("A", "alpha");
        trackingMap.put("C", "charlie");
        trackingMap.put("D", "delta");
        // Mark A and C as read
        trackingMap.get("A");
        trackingMap.containsKey("C");
        // first call
        trackingMap.expungeUnused();
        Set<String> afterFirst = new HashSet<>(trackingMap.keySet());
        // second call – should be no‑op
        trackingMap.expungeUnused();
        Set<String> afterSecond = new HashSet<>(trackingMap.keySet());
        assertEquals(Set.of("A", "C"), afterFirst, "First expungeUnused should keep only accessed keys");
        assertEquals(afterFirst, afterSecond, "Second expungeUnused should not change the map");
    }

    @Test
    void expungeUnusedHandlesCustomKeyEquality() {
        TrackingMap<CustomKey, String> map = new TrackingMap<>(new HashMap<>());
        CustomKey k1 = new CustomKey(1);
        // equal to k1
        CustomKey k2 = new CustomKey(1);
        map.put(k1, "value1");
        // Access via a different but equal instance
        assertEquals("value1", map.get(k2));
        map.expungeUnused();
        assertEquals(1, map.size(), "Map should retain entry with equal custom key");
        assertTrue(map.containsKey(k1));
        assertEquals("value1", map.get(k1));
    }

    /* --------------------------------------------------------------
     * Group: values untouched
     * -------------------------------------------------------------- */
    @Test
    void expungeUnusedLeavesValuesIntactForRetainedKeys() {
        trackingMap.put("A", "alpha");
        trackingMap.put("C", "charlie");
        trackingMap.put("D", "delta");
        // Access A and C
        trackingMap.get("A");
        trackingMap.containsKey("C");
        trackingMap.expungeUnused();
        Map<String, String> expected = Map.of("A", "alpha", "C", "charlie");
        assertEquals(expected, trackingMap);
    }

    /* --------------------------------------------------------------
     * Group: entrySet reflects removal
     * -------------------------------------------------------------- */
    @Test
    void expungeUnusedEntrySetConsistentWithFilteredContent() {
        trackingMap.put("A", "alpha");
        trackingMap.put("C", "charlie");
        trackingMap.put("D", "delta");
        // Access A and C
        trackingMap.get("A");
        trackingMap.containsKey("C");
        trackingMap.expungeUnused();
        Set<Map.Entry<String, String>> entrySet = trackingMap.entrySet();
        assertEquals(2, entrySet.size(), "Entry set size should match retained keys");
        Map<String, String> fromEntrySet = new HashMap<>();
        for (Map.Entry<String, String> e : entrySet) {
            fromEntrySet.put(e.getKey(), e.getValue());
        }
        assertEquals(Map.of("A", "alpha", "C", "charlie"), fromEntrySet);
    }

    /* --------------------------------------------------------------
     * Group: internal map reference unchanged
     * -------------------------------------------------------------- */
    @Test
    void expungeUnusedDoesNotReplaceInternalMapInstance() throws Exception {
        Map<String, String> original = trackingMap.getWrappedMap();
        trackingMap.put("X", "xray");
        // mark as read
        trackingMap.get("X");
        trackingMap.expungeUnused();
        assertSame(original, trackingMap.getWrappedMap(), "Internal map reference should stay the same after expungeUnused");
    }

    /* --------------------------------------------------------------
     * Group: null internalMap handling
     * -------------------------------------------------------------- */
    @Test
    void expungeUnusedThrowsWhenInternalMapIsNull() throws Exception {
        // Construct normally then force internalMap to null via reflection
        setInternalMap(trackingMap, null);
        // Populate readKeys to avoid NPE on that side
        setReadKeys(trackingMap, new HashSet<>(List.of("any")));
        assertThrows(NullPointerException.class, trackingMap::expungeUnused, "Calling expungeUnused on a TrackingMap with null internalMap should throw NPE");
    }

    /* --------------------------------------------------------------
     * Group: null readKeys handling
     * -------------------------------------------------------------- */
    @Test
    void expungeUnusedThrowsWhenReadKeysIsNull() throws Exception {
        // Force readKeys to null via reflection
        setReadKeys(trackingMap, null);
        // Ensure internalMap has something to avoid early NPE
        trackingMap.put("A", "alpha");
        assertThrows(NullPointerException.class, trackingMap::expungeUnused, "Calling expungeUnused when readKeys is null should throw NPE");
    }

    /* --------------------------------------------------------------
     * Group: unmodifiable internal map handling
     * -------------------------------------------------------------- */
    @Test
    void expungeUnusedThrowsOnUnmodifiableInternalMap() {
        Map<String, String> unmod = Collections.unmodifiableMap(new HashMap<>(Map.of("A", "alpha", "B", "beta")));
        TrackingMap<String, String> map = new TrackingMap<>(unmod);
        // Access A to make it a read key
        map.get("A");
        assertThrows(UnsupportedOperationException.class, map::expungeUnused, "expungeUnused on an unmodifiable internal map should throw UnsupportedOperationException");
    }
}
