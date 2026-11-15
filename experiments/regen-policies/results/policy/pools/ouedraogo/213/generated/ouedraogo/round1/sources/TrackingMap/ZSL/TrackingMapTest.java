package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

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
    public void constructorShouldRejectNullMap() {
        new TrackingMap<>(null);
    }

    @Test
    public void getShouldReturnValueAndMarkKeyAsRead() {
        assertEquals(Integer.valueOf(1), trackingMap.get("apple"));
        assertTrue(trackingMap.keysUsed().contains("apple"));
        // Access a non‑existing key – should still be recorded
        assertNull(trackingMap.get("durian"));
        assertTrue(trackingMap.keysUsed().contains("durian"));
    }

    @Test
    public void containsKeyShouldReturnCorrectResultAndMarkKeyAsRead() {
        assertTrue(trackingMap.containsKey("banana"));
        assertTrue(trackingMap.keysUsed().contains("banana"));

        assertFalse(trackingMap.containsKey("eggplant"));
        assertTrue(trackingMap.keysUsed().contains("eggplant"));
    }

    @Test
    public void putShouldStoreValueAndReturnPrevious() {
        Integer previous = trackingMap.put("apple", 10);
        assertEquals(Integer.valueOf(1), previous);
        assertEquals(Integer.valueOf(10), trackingMap.get("apple"));
        // New key
        assertNull(trackingMap.put("fig", 6));
        assertEquals(Integer.valueOf(6), trackingMap.get("fig"));
    }

    @Test
    public void putAllShouldCopyAllEntries() {
        Map<String, Integer> extra = new HashMap<>();
        extra.put("grape", 7);
        extra.put("honeydew", 8);
        trackingMap.putAll(extra);
        assertEquals(Integer.valueOf(7), trackingMap.get("grape"));
        assertEquals(Integer.valueOf(8), trackingMap.get("honeydew"));
        assertEquals(5, trackingMap.size());
    }

    @Test
    public void removeShouldDeleteEntryAndForgetReadState() {
        // Mark key as read first
        trackingMap.get("cherry");
        assertTrue(trackingMap.keysUsed().contains("cherry"));
        // Now remove it
        Integer removed = trackingMap.remove("cherry");
        assertEquals(Integer.valueOf(3), removed);
        assertFalse(trackingMap.containsKey("cherry"));
        assertFalse(trackingMap.keysUsed().contains("cherry"));
    }

    @Test
    public void sizeAndIsEmptyBehaviour() {
        assertEquals(3, trackingMap.size());
        assertFalse(trackingMap.isEmpty());

        trackingMap.clear();
        assertEquals(0, trackingMap.size());
        assertTrue(trackingMap.isEmpty());
    }

    @Test
    public void equalsAndHashCodeDelegateToWrappedMap() {
        Map<String, Integer> other = new HashMap<>(backingMap);
        assertTrue(trackingMap.equals(other));
        assertEquals(backingMap.hashCode(), trackingMap.hashCode());

        other.put("newKey", 99);
        assertFalse(trackingMap.equals(other));
    }

    @Test
    public void toStringReflectsWrappedMap() {
        String expected = backingMap.toString();
        assertEquals(expected, trackingMap.toString());
    }

    @Test
    public void clearEmptiesBothMapAndReadKeys() {
        trackingMap.get("apple");
        trackingMap.get("banana");
        assertFalse(trackingMap.keysUsed().isEmpty());
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty());
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    @Test
    public void containsValueWorks() {
        assertTrue(trackingMap.containsValue(2));
        assertFalse(trackingMap.containsValue(99));
    }

    @Test
    public void valuesKeySetAndEntrySetReflectInternalState() {
        Collection<Integer> values = trackingMap.values();
        assertTrue(values.containsAll(Arrays.asList(1, 2, 3)));

        Set<String> keys = trackingMap.keySet();
        assertTrue(keys.containsAll(Arrays.asList("apple", "banana", "cherry")));

        Set<Map.Entry<String, Integer>> entries = trackingMap.entrySet();
        for (Map.Entry<String, Integer> e : entries) {
            assertEquals(backingMap.get(e.getKey()), e.getValue());
        }
    }

    @Test
    public void expungeUnusedRemovesNeverReadEntries() {
        // Access only apple and banana
        trackingMap.get("apple");
        trackingMap.containsKey("banana");
        // Do NOT touch "cherry"
        trackingMap.expungeUnused();

        assertTrue(trackingMap.containsKey("apple"));
        assertTrue(trackingMap.containsKey("banana"));
        assertFalse(trackingMap.containsKey("cherry"));
        assertEquals(2, trackingMap.size());

        // The readKeys set should still contain "cherry" because it was never read,
        // but after expungeUnused it is not present in the map.
        assertTrue(trackingMap.keysUsed().contains("cherry"));
    }

    @Test
    public void informAdditionalUsageCollectionAddsKeys() {
        Collection<String> extra = Arrays.asList("x", "y");
        trackingMap.informAdditionalUsage(extra);
        assertTrue(trackingMap.keysUsed().containsAll(extra));
    }

    @Test
    public void informAdditionalUsageTrackingMapMergesReadKeys() {
        TrackingMap<String, Integer> other = new TrackingMap<>(new HashMap<>());
        other.get("alpha"); // marks "alpha"
        other.get("beta");  // marks "beta"

        trackingMap.informAdditionalUsage(other);
        assertTrue(trackingMap.keysUsed().containsAll(Arrays.asList("alpha", "beta")));
    }

    @Test
    public void keysUsedReturnsLiveSet() {
        trackingMap.get("apple");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("apple"));
        used.add("newKey");
        // The underlying readKeys set should reflect the addition
        assertTrue(trackingMap.keysUsed().contains("newKey"));
    }

    @Test
    public void getWrappedMapReturnsUnderlyingMap() {
        Map<String, Integer> wrapped = trackingMap.getWrappedMap();
        assertSame(backingMap, wrapped);
    }

    @Test
    public void setWrappedMapReplacesContentAndResetsReadKeys() {
        Map<String, Integer> newMap = new HashMap<>();
        newMap.put("zeta", 26);
        trackingMap.get("apple"); // mark a key before replacement
        assertTrue(trackingMap.keysUsed().contains("apple"));

        trackingMap.setWrappedMap(newMap);
        // New map should be in place
        assertEquals(Integer.valueOf(26), trackingMap.get("zeta"));
        assertEquals(1, trackingMap.size());

        // Previous read keys must have been cleared
        assertFalse(trackingMap.keysUsed().contains("apple"));
        // The newly accessed key should be present now
        assertTrue(trackingMap.keysUsed().contains("zeta"));
    }

    @Test
    public void setWrappedMapShouldRejectNull() {
        try {
            trackingMap.setWrappedMap(null);
            fail("Expected an exception when setting a null wrapped map");
        } catch (Exception e) {
            // The exact exception type depends on Convention.throwIfNull implementation.
            // We only assert that an exception is thrown.
            assertNotNull(e);
        }
    }
}
