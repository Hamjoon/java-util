package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Arrays;

/**
 * Comprehensive JUnit‑4 test suite for {@link TrackingMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (including null‑argument validation)</li>
 *   <li>All {@link Map} operations delegated to the wrapped map</li>
 *   <li>Tracking behaviour of {@code get}, {@code containsKey}, {@code remove},
 *       {@code clear}, {@code expungeUnused}, and the two {@code informAdditionalUsage}
 *       overloads</li>
 *   <li>Utility methods {@code keysUsed}, {@code getWrappedMap},
 *       {@code equals}, {@code hashCode}, {@code toString}</li>
 * </ul>
 */
public class TrackingMapTest {

    private TrackingMap<String, Integer> trackingMap;
    private Map<String, Integer> backingMap;

    @Before
    public void setUp() {
        backingMap = new HashMap<>();
        backingMap.put("apple", 1);
        backingMap.put("banana", 2);
        backingMap.put("cherry", 3);
        trackingMap = new TrackingMap<>(backingMap);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNull() {
        new TrackingMap<String, Integer>(null);
    }

    @Test
    public void testPutAndGet() {
        // put a new entry
        Integer previous = trackingMap.put("date", 4);
        assertNull(previous);
        assertEquals(Integer.valueOf(4), trackingMap.get("date"));
        // get should mark the key as accessed
        assertTrue(trackingMap.keysUsed().contains("date"));
    }

    @Test
    public void testGetNonExistingKey() {
        assertNull(trackingMap.get("nonexistent"));
        // even a miss is recorded as an accessed key
        assertTrue(trackingMap.keysUsed().contains("nonexistent"));
    }

    @Test
    public void testContainsKey() {
        assertTrue(trackingMap.containsKey("banana"));
        assertTrue(trackingMap.keysUsed().contains("banana"));

        assertFalse(trackingMap.containsKey("fig"));
        assertTrue(trackingMap.keysUsed().contains("fig")); // miss is still recorded
    }

    @Test
    public void testRemoveUpdatesReadKeys() {
        // Access a key first so it appears in readKeys
        trackingMap.get("apple");
        assertTrue(trackingMap.keysUsed().contains("apple"));

        // Remove it – should disappear from readKeys
        Integer removed = trackingMap.remove("apple");
        assertEquals(Integer.valueOf(1), removed);
        assertFalse(trackingMap.keysUsed().contains("apple"));
        // The underlying map no longer contains the key
        assertFalse(trackingMap.containsKey("apple")); // this call will re‑add it to readKeys
        assertTrue(trackingMap.keysUsed().contains("apple"));
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> extra = new HashMap<>();
        extra.put("elderberry", 5);
        extra.put("fig", 6);
        trackingMap.putAll(extra);
        assertEquals(Integer.valueOf(5), trackingMap.get("elderberry"));
        assertEquals(Integer.valueOf(6), trackingMap.get("fig"));
        assertEquals(5, trackingMap.size());
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(3, trackingMap.size());
        assertFalse(trackingMap.isEmpty());

        trackingMap.clear();
        assertEquals(0, trackingMap.size());
        assertTrue(trackingMap.isEmpty());
    }

    @Test
    public void testEqualsAndHashCode() {
        Map<String, Integer> other = new HashMap<>();
        other.put("apple", 1);
        other.put("banana", 2);
        other.put("cherry", 3);
        assertTrue(trackingMap.equals(other));
        assertEquals(other.hashCode(), trackingMap.hashCode());

        other.put("date", 4);
        assertFalse(trackingMap.equals(other));
    }

    @Test
    public void testToStringMatchesWrappedMap() {
        // The string representation should be exactly that of the backing map
        assertEquals(backingMap.toString(), trackingMap.toString());
    }

    @Test
    public void testClearResetsReadKeysAndMap() {
        trackingMap.get("apple");
        trackingMap.get("banana");
        assertEquals(2, trackingMap.keysUsed().size());

        trackingMap.clear();
        assertTrue(trackingMap.keysUsed().isEmpty());
        assertTrue(trackingMap.isEmpty());
    }

    @Test
    public void testContainsValue() {
        assertTrue(trackingMap.containsValue(2));
        assertFalse(trackingMap.containsValue(99));
    }

    @Test
    public void testValuesCollection() {
        Collection<Integer> values = trackingMap.values();
        assertTrue(values.containsAll(Arrays.asList(1, 2, 3)));
        assertEquals(3, values.size());
    }

    @Test
    public void testKeySetAndEntrySet() {
        Set<String> keys = trackingMap.keySet();
        assertTrue(keys.containsAll(Arrays.asList("apple", "banana", "cherry")));
        assertEquals(3, keys.size());

        Set<Map.Entry<String, Integer>> entries = trackingMap.entrySet();
        assertEquals(3, entries.size());
        for (Map.Entry<String, Integer> e : entries) {
            assertEquals(backingMap.get(e.getKey()), e.getValue());
        }
    }

    @Test
    public void testExpungeUnusedRemovesUnaccessedEntries() {
        // Access only "apple" and "banana"
        trackingMap.get("apple");
        trackingMap.containsKey("banana");

        // Before expunge, all three keys are present
        assertEquals(3, trackingMap.size());

        trackingMap.expungeUnused();

        // "cherry" was never accessed → removed
        assertEquals(2, trackingMap.size());
        assertFalse(trackingMap.containsKey("cherry"));
        assertTrue(trackingMap.containsKey("apple"));
        assertTrue(trackingMap.containsKey("banana"));
    }

    @Test
    public void testInformAdditionalUsageWithCollection() {
        // Access no keys initially
        assertTrue(trackingMap.keysUsed().isEmpty());

        // Add external usage information
        trackingMap.informAdditionalUsage(Arrays.asList("apple", "nonexistent"));
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("apple"));
        assertTrue(used.contains("nonexistent"));
        // After informing, expungeUnused should keep "apple" even if it wasn't accessed via get/containsKey
        trackingMap.expungeUnused();
        assertTrue(trackingMap.containsKey("apple"));
        assertFalse(trackingMap.containsKey("banana")); // not marked, should be removed
        assertFalse(trackingMap.containsKey("cherry"));
    }

    @Test
    public void testInformAdditionalUsageWithAnotherTrackingMap() {
        // Create a second TrackingMap and mark some keys
        Map<String, Integer> otherMap = new HashMap<>();
        otherMap.put("x", 10);
        otherMap.put("y", 20);
        TrackingMap<String, Integer> otherTracking = new TrackingMap<>(otherMap);
        otherTracking.get("x"); // mark "x"
        otherTracking.containsKey("y"); // mark "y"

        // Merge usage into the first map
        trackingMap.informAdditionalUsage(otherTracking);

        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.containsAll(Arrays.asList("x", "y")));

        // After merge, expungeUnused should retain any keys that match the merged usage set,
        // even if they are not present in the original backing map.
        trackingMap.expungeUnused();
        // Original keys were "apple","banana","cherry". None of them are in the merged set,
        // so all should be removed.
        assertTrue(trackingMap.isEmpty());
    }

    @Test
    public void testKeysUsedReflectsAllAccesses() {
        trackingMap.get("apple");
        trackingMap.containsKey("banana");
        trackingMap.get("nonexistent");
        Set<String> used = trackingMap.keysUsed();
        assertEquals(3, used.size());
        assertTrue(used.containsAll(Arrays.asList("apple", "banana", "nonexistent")));
    }

    @Test
    public void testGetWrappedMapReturnsOriginalReference() {
        assertSame(backingMap, trackingMap.getWrappedMap());
    }

    @Test
    public void testNullKeyBehaviour() {
        // HashMap permits null keys
        trackingMap.put(null, 0);
        assertTrue(trackingMap.containsKey(null));
        assertEquals(Integer.valueOf(0), trackingMap.get(null));

        // Access tracking should include null
        assertTrue(trackingMap.keysUsed().contains(null));

        // Remove null key
        trackingMap.remove(null);
        assertFalse(trackingMap.containsKey(null));
        // After removal, null should no longer be in the accessed set
        assertFalse(trackingMap.keysUsed().contains(null));
    }
}
