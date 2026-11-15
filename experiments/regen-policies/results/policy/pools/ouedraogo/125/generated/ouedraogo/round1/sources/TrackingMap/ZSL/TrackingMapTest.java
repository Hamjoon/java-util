package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

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
    public void testGetAndReadKeysTracking() {
        assertEquals(Integer.valueOf(1), trackingMap.get("apple"));
        assertTrue(trackingMap.keysUsed().contains("apple"));

        // Access a non‑existent key – should still be recorded
        assertNull(trackingMap.get("durian"));
        assertTrue(trackingMap.keysUsed().contains("durian"));
    }

    @Test
    public void testContainsKeyAndReadKeysTracking() {
        assertTrue(trackingMap.containsKey("banana"));
        assertTrue(trackingMap.keysUsed().contains("banana"));

        // Check absent key – still recorded
        assertFalse(trackingMap.containsKey("elderberry"));
        assertTrue(trackingMap.keysUsed().contains("elderberry"));
    }

    @Test
    public void testPutAndPutAll() {
        Integer previous = trackingMap.put("date", 4);
        assertNull(previous);
        assertEquals(Integer.valueOf(4), trackingMap.get("date"));
        assertTrue(trackingMap.keysUsed().contains("date"));

        Map<String, Integer> extra = new HashMap<>();
        extra.put("fig", 5);
        extra.put("grape", 6);
        trackingMap.putAll(extra);
        assertEquals(Integer.valueOf(5), trackingMap.get("fig"));
        assertEquals(Integer.valueOf(6), trackingMap.get("grape"));
        // putAll does not automatically mark keys as accessed
        assertFalse(trackingMap.keysUsed().contains("fig"));
        assertFalse(trackingMap.keysUsed().contains("grape"));
    }

    @Test
    public void testRemoveUpdatesReadKeys() {
        // Access a key first so it is in readKeys
        trackingMap.get("cherry");
        assertTrue(trackingMap.keysUsed().contains("cherry"));

        // Remove the key – it should be removed from readKeys as well
        Integer removed = trackingMap.remove("cherry");
        assertEquals(Integer.valueOf(3), removed);
        assertFalse(trackingMap.keysUsed().contains("cherry"));
        assertFalse(trackingMap.containsKey("cherry"));
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
        Map<String, Integer> other = new HashMap<>(backingMap);
        assertTrue(trackingMap.equals(other));
        assertEquals(trackingMap.hashCode(), other.hashCode());

        // Different content
        other.put("apple", 99);
        assertFalse(trackingMap.equals(other));
    }

    @Test
    public void testToStringMatchesUnderlyingMap() {
        assertEquals(backingMap.toString(), trackingMap.toString());
    }

    @Test
    public void testClearResetsBothMapAndReadKeys() {
        trackingMap.get("apple");
        trackingMap.get("banana");
        assertFalse(trackingMap.keysUsed().isEmpty());

        trackingMap.clear();
        assertTrue(trackingMap.isEmpty());
        assertTrue(trackingMap.keysUsed().isEmpty());
        assertTrue(trackingMap.getWrappedMap().isEmpty());
    }

    @Test
    public void testContainsValueValuesKeySetEntrySet() {
        assertTrue(trackingMap.containsValue(2));
        assertFalse(trackingMap.containsValue(99));

        Collection<Integer> values = trackingMap.values();
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));

        Set<String> keys = trackingMap.keySet();
        assertTrue(keys.containsAll(backingMap.keySet()));

        Set<Map.Entry<String, Integer>> entries = trackingMap.entrySet();
        for (Map.Entry<String, Integer> e : entries) {
            assertEquals(backingMap.get(e.getKey()), e.getValue());
        }
    }

    @Test
    public void testExpungeUnusedRemovesOnlyUnaccessedEntries() {
        // Access "apple" and "banana" only
        trackingMap.get("apple");
        trackingMap.containsKey("banana");

        // Before expunge, all three entries exist
        assertEquals(3, trackingMap.size());

        trackingMap.expungeUnused();

        // "cherry" was never accessed and should be gone
        assertEquals(2, trackingMap.size());
        assertFalse(trackingMap.containsKey("cherry"));
        assertTrue(trackingMap.containsKey("apple"));
        assertTrue(trackingMap.containsKey("banana"));
    }

    @Test
    public void testInformAdditionalUsageWithCollection() {
        Set<String> additional = new HashSet<>();
        additional.add("kiwi");
        additional.add("lemon");

        trackingMap.informAdditionalUsage(additional);
        assertTrue(trackingMap.keysUsed().containsAll(additional));
    }

    @Test
    public void testInformAdditionalUsageWithAnotherTrackingMap() {
        TrackingMap<String, Integer> other = new TrackingMap<>(new HashMap<>());
        other.get("mango"); // mark as used
        other.put("nectarine", 7); // not marked as used

        trackingMap.informAdditionalUsage(other);
        // Only the key that other accessed should be added
        assertTrue(trackingMap.keysUsed().contains("mango"));
        assertFalse(trackingMap.keysUsed().contains("nectarine"));
    }

    @Test
    public void testGetWrappedMapReturnsOriginalReference() {
        Map<String, Integer> wrapped = trackingMap.getWrappedMap();
        assertSame(backingMap, wrapped);
        // Mutate via the returned map and verify TrackingMap sees the change
        wrapped.put("orange", 8);
        assertEquals(Integer.valueOf(8), trackingMap.get("orange"));
    }

    @Test
    public void testKeysUsedIsLiveView() {
        trackingMap.get("apple");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("apple"));

        // Adding a new accessed key should be reflected in the previously obtained set
        trackingMap.get("banana");
        assertTrue(used.contains("banana"));
    }
}
