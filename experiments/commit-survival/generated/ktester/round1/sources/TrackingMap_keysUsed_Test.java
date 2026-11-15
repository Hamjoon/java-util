package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

class TrackingMap_keysUsed_Test {

    private TrackingMap<String, Integer> trackingMap;

    private Map<String, Integer> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a simple backing map with a few entries
        backingMap = new HashMap<>();
        backingMap.put("alpha", 1);
        backingMap.put("beta", 2);
        backingMap.put("gamma", 3);
        // Create the TrackingMap instance under test
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        trackingMap = null;
        backingMap = null;
    }

    @Test
    void keysUsed_IsInitiallyEmpty() {
        Set<String> used = trackingMap.keysUsed();
        assertNotNull(used, "keysUsed() should never return null");
        assertTrue(used.isEmpty(), "No keys should be reported as used before any access");
    }

    @Test
    void keysUsed_TracksSingleGet() {
        // Access a key through the map
        Integer value = trackingMap.get("one");
        assertEquals(1, value, "Returned value should match the backing map");
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "Exactly one key should be recorded as used");
        assertTrue(used.contains("one"), "The accessed key should be present in the used‑keys set");
    }

    @Test
    void keysUsed_TracksMultipleGets() {
        trackingMap.get("one");
        trackingMap.get("two");
        trackingMap.get("three");
        Set<String> used = trackingMap.keysUsed();
        assertEquals(3, used.size(), "All accessed keys should be recorded");
        assertTrue(used.containsAll(Arrays.asList("one", "two", "three")), "The used‑keys set should contain all keys that were accessed");
    }

    @Test
    void keysUsed_ReturnsSameSetInstance() {
        Set<String> firstCall = trackingMap.keysUsed();
        Set<String> secondCall = trackingMap.keysUsed();
        assertSame(firstCall, secondCall, "keysUsed() should return the same Set instance on each call");
    }

    @Test
    void keysUsed_DoesNotDuplicateWhenReadingSameKeyTwice() {
        trackingMap.get("alpha");
        // second read
        trackingMap.get("alpha");
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "Duplicate reads must not create duplicate entries");
        assertTrue(used.contains("alpha"));
    }

    @Test
    void keysUsed_TracksContainsKey() {
        // According to the API, containsKey should also be considered a read
        assertTrue(trackingMap.containsKey("beta"));
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size());
        assertTrue(used.contains("beta"));
    }

    @Test
    void keysUsed_TracksEntrySetIteration() {
        // Access a key via entrySet iterator
        Iterator<Map.Entry<String, Integer>> it = trackingMap.entrySet().iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, Integer> entry = it.next();
        // The key read through the iterator should be recorded
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains(entry.getKey()));
    }

    @Test
    void keysUsed_KeySetContainsTracking() {
        // keySet().contains triggers a read of the key
        assertTrue(trackingMap.keySet().contains("gamma"));
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size());
        assertTrue(used.contains("gamma"));
    }

    @Test
    void keysUsed_KeySetIteratorTracking() {
        Iterator<String> it = trackingMap.keySet().iterator();
        assertTrue(it.hasNext());
        // reading the key
        String key = it.next();
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains(key));
    }

    @Test
    void keysUsed_ValuesIterationDoesNotTrackKeys() {
        // Reading values should not affect the used‑keys set
        Iterator<Integer> it = trackingMap.values().iterator();
        assertTrue(it.hasNext());
        // just consume a value
        it.next();
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.isEmpty(), "Reading values must not be considered a key read");
    }

    @Test
    void keysUsed_NullKeyGetIsTracked() {
        trackingMap.put(null, 99);
        Integer v = trackingMap.get(null);
        assertEquals(99, v);
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains(null));
    }

    @Test
    void keysUsed_NullKeyContainsKeyIsTracked() {
        trackingMap.put(null, 0);
        assertTrue(trackingMap.containsKey(null));
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains(null));
    }

    @Test
    void keysUsed_GetAbsentKeyDoesNotTrack() {
        Integer v = trackingMap.get("missing");
        assertNull(v);
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.isEmpty(), "Absent key reads must not be recorded");
    }

    @Test
    void keysUsed_PutDoesNotCountAsRead() {
        trackingMap.put("delta", 4);
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.isEmpty(), "Put operation should not be considered a read");
    }

    @Test
    void keysUsed_RemoveDoesNotCountAsRead() {
        trackingMap.remove("alpha");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.isEmpty(), "Remove operation should not be considered a read");
    }

    @Test
    void keysUsed_ClearResetsTracking() {
        trackingMap.get("alpha");
        // clear underlying map + tracking
        trackingMap.clear();
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.isEmpty(), "clear() must reset the read‑keys set");
    }

    @Test
    void keysUsed_ExpungeUnusedKeepsOnlyExistingKeys() {
        // read alpha
        trackingMap.get("alpha");
        // add beta without reading it
        trackingMap.put("beta", 2);
        // should remove beta from tracking (if present) but keep alpha
        trackingMap.expungeUnused();
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("alpha"));
        assertFalse(used.contains("beta"));
    }

    @Test
    void keysUsed_InformAdditionalUsageCollectionMerges() {
        trackingMap.get("alpha");
        trackingMap.get("beta");
        trackingMap.informAdditionalUsage(Arrays.asList("gamma", "delta"));
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.containsAll(Arrays.asList("alpha", "beta", "gamma", "delta")));
    }

    @Test
    void keysUsed_InformAdditionalUsageFromAnotherTrackingMapMerges() {
        TrackingMap<String, Integer> mapA = new TrackingMap<>(new HashMap<>());
        TrackingMap<String, Integer> mapB = new TrackingMap<>(new HashMap<>());
        // read alpha in A
        mapA.get("alpha");
        // read beta in B
        mapB.get("beta");
        mapA.informAdditionalUsage(mapB);
        Set<String> usedA = mapA.keysUsed();
        assertTrue(usedA.containsAll(Arrays.asList("alpha", "beta")));
    }

    @Test
    void keysUsed_PutAllDoesNotAffectTracking() {
        Map<String, Integer> other = new HashMap<>();
        other.put("x", 10);
        other.put("y", 20);
        trackingMap.putAll(other);
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.isEmpty(), "putAll should not be considered a read operation");
    }

    @Test
    void keysUsed_NullInternalSetThrowsNPE() throws Exception {
        // Create a map and then force its internal readKeys to null via reflection
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        Field f = TrackingMap.class.getDeclaredField("readKeys");
        f.setAccessible(true);
        // corrupt internal state
        f.set(map, null);
        assertThrows(NullPointerException.class, map::keysUsed, "If internal readKeys is null, keysUsed() must throw NPE");
    }

    @Test
    void keysUsed_ReturnedSetIsUnmodifiable() {
        Set<String> used = trackingMap.keysUsed();
        // The contract of the implementation may return an unmodifiable view.
        // Attempt to modify it and expect UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, () -> used.add("someKey"));
    }

    @Test
    void keysUsed_ConcurrentModificationDuringIteration() {
        trackingMap.get("alpha");
        Set<String> used = trackingMap.keysUsed();
        Iterator<String> it = used.iterator();
        // Modify the underlying tracking set via clear()
        trackingMap.clear();
        // The iterator should detect concurrent modification
        assertThrows(ConcurrentModificationException.class, it::next);
    }
}
