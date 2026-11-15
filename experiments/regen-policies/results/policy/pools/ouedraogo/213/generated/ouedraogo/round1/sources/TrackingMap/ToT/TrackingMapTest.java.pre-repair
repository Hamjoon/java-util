package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TrackingMap}.
 *
 * The tests are written as if three independent experts each contributed one
 * test case per public method, then merged their work.  All typical usage,
 * edge‑cases and error scenarios are covered.
 */
public class TrackingMapTest {

    private Map<String, Integer> backingMap;
    private TrackingMap<String, Integer> trackingMap;

    @Before
    public void setUp() {
        backingMap = new HashMap<>();
        backingMap.put("apple", 1);
        backingMap.put("banana", 2);
        backingMap.put("cherry", 3);
        trackingMap = new TrackingMap<>(backingMap);
    }

    /* -----------------------------------------------------------------
     * Constructor tests
     * ----------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldRejectNullMap() {
        new TrackingMap<>(null);
    }

    @Test
    public void constructorCreatesIndependentReadKeySet() {
        trackingMap.get("apple"); // mark one key as read
        TrackingMap<String, Integer> second = new TrackingMap<>(new HashMap<>(backingMap));
        // second map must have an empty read‑key set
        assertTrue(second.keysUsed().isEmpty());
    }

    /* -----------------------------------------------------------------
     * get() – normal and edge cases
     * ----------------------------------------------------------------- */

    @Test
    public void getShouldReturnValueAndMarkKeyAsRead() {
        assertEquals(Integer.valueOf(1), trackingMap.get("apple"));
        assertTrue(trackingMap.keysUsed().contains("apple"));
    }

    @Test
    public void getWithMissingKeyReturnsNullAndMarksKey() {
        assertNull(trackingMap.get("durian"));
        assertTrue(trackingMap.keysUsed().contains("durian"));
    }

    @Test
    public void getWithNullKeyIsHandledGracefully() {
        // HashMap permits null keys, so we expect the same behaviour.
        backingMap.put(null, 99);
        assertEquals(Integer.valueOf(99), trackingMap.get(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    /* -----------------------------------------------------------------
     * containsKey() – normal and edge cases
     * ----------------------------------------------------------------- */

    @Test
    public void containsKeyShouldReturnTrueAndMarkKey() {
        assertTrue(trackingMap.containsKey("banana"));
        assertTrue(trackingMap.keysUsed().contains("banana"));
    }

    @Test
    public void containsKeyWithMissingKeyReturnsFalseAndMarksKey() {
        assertFalse(trackingMap.containsKey("durian"));
        assertTrue(trackingMap.keysUsed().contains("durian"));
    }

    @Test
    public void containsKeyWithNullKeyIsHandledGracefully() {
        backingMap.put(null, 0);
        assertTrue(trackingMap.containsKey(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    /* -----------------------------------------------------------------
     * put() and putAll()
     * ----------------------------------------------------------------- */

    @Test
    public void putShouldDelegateToInternalMap() {
        Integer previous = trackingMap.put("date", 4);
        assertNull(previous);
        assertEquals(Integer.valueOf(4), trackingMap.get("date"));
        assertEquals(4, trackingMap.size());
    }

    @Test
    public void putShouldReturnPreviousValue() {
        Integer previous = trackingMap.put("apple", 10);
        assertEquals(Integer.valueOf(1), previous);
        assertEquals(Integer.valueOf(10), trackingMap.get("apple"));
    }

    @Test
    public void putAllShouldAddAllEntries() {
        Map<String, Integer> extra = new HashMap<>();
        extra.put("elderberry", 5);
        extra.put("fig", 6);
        trackingMap.putAll(extra);
        assertEquals(Integer.valueOf(5), trackingMap.get("elderberry"));
        assertEquals(Integer.valueOf(6), trackingMap.get("fig"));
        assertEquals(5, trackingMap.size());
    }

    @Test(expected = NullPointerException.class)
    public void putAllShouldRejectNullArgument() {
        trackingMap.putAll(null);
    }

    /* -----------------------------------------------------------------
     * remove()
     * ----------------------------------------------------------------- */

    @Test
    public void removeShouldDeleteEntryAndForgetReadState() {
        // Mark a key as read first
        trackingMap.get("cherry");
        assertTrue(trackingMap.keysUsed().contains("cherry"));

        // Now remove it
        Integer removed = trackingMap.remove("cherry");
        assertEquals(Integer.valueOf(3), removed);
        assertFalse(trackingMap.containsKey("cherry"));
        assertFalse(trackingMap.keysUsed().contains("cherry"));
    }

    @Test
    public void removeNonExistingKeyReturnsNull() {
        assertNull(trackingMap.remove("nonexistent"));
    }

    @Test
    public void removeNullKeyIsHandledGracefully() {
        backingMap.put(null, 0);
        trackingMap.get(null); // mark it as read
        assertTrue(trackingMap.keysUsed().contains(null));
        Integer removed = trackingMap.remove(null);
        assertEquals(Integer.valueOf(0), removed);
        assertFalse(trackingMap.keysUsed().contains(null));
    }

    /* -----------------------------------------------------------------
     * size() and isEmpty()
     * ----------------------------------------------------------------- */

    @Test
    public void sizeReflectsNumberOfMappings() {
        assertEquals(3, trackingMap.size());
        trackingMap.put("grape", 7);
        assertEquals(4, trackingMap.size());
    }

    @Test
    public void isEmptyReflectsMapContent() {
        assertFalse(trackingMap.isEmpty());
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty());
    }

    /* -----------------------------------------------------------------
     * equals() and hashCode()
     * ----------------------------------------------------------------- */

    @Test
    public void equalsShouldCompareBasedOnUnderlyingMap() {
        Map<String, Integer> other = new HashMap<>(backingMap);
        assertTrue(trackingMap.equals(other));
        other.put("extra", 9);
        assertFalse(trackingMap.equals(other));
    }

    @Test
    public void equalsShouldReturnFalseForNonMapObjects() {
        assertFalse(trackingMap.equals("not a map"));
    }

    @Test
    public void hashCodeShouldMatchUnderlyingMapHashCode() {
        assertEquals(backingMap.hashCode(), trackingMap.hashCode());
    }

    /* -----------------------------------------------------------------
     * toString()
     * ----------------------------------------------------------------- */

    @Test
    public void toStringShouldDelegateToInternalMap() {
        assertEquals(backingMap.toString(), trackingMap.toString());
    }

    /* -----------------------------------------------------------------
     * clear()
     * ----------------------------------------------------------------- */

    @Test
    public void clearShouldEmptyBothMapAndReadKeySet() {
        trackingMap.get("apple");
        trackingMap.get("banana");
        assertFalse(trackingMap.isEmpty());
        assertFalse(trackingMap.keysUsed().isEmpty());

        trackingMap.clear();
        assertTrue(trackingMap.isEmpty());
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    /* -----------------------------------------------------------------
     * containsValue(), values(), keySet(), entrySet()
     * ----------------------------------------------------------------- */

    @Test
    public void containsValueShouldDelegateToInternalMap() {
        assertTrue(trackingMap.containsValue(2));
        assertFalse(trackingMap.containsValue(99));
    }

    @Test
    public void valuesShouldReflectCurrentMapValues() {
        Collection<Integer> vals = trackingMap.values();
        assertTrue(vals.containsAll(Arrays.asList(1, 2, 3)));
        trackingMap.put("date", 4);
        assertTrue(vals.contains(4));
    }

    @Test
    public void keySetShouldReflectCurrentMapKeys() {
        Set<String> keys = trackingMap.keySet();
        assertTrue(keys.containsAll(Arrays.asList("apple", "banana", "cherry")));
        trackingMap.put("date", 4);
        assertTrue(keys.contains("date"));
    }

    @Test
    public void entrySetShouldReflectCurrentMapEntries() {
        Set<Map.Entry<String, Integer>> entries = trackingMap.entrySet();
        Map<String, Integer> copy = new HashMap<>();
        for (Map.Entry<String, Integer> e : entries) {
            copy.put(e.getKey(), e.getValue());
        }
        assertEquals(backingMap, copy);
    }

    /* -----------------------------------------------------------------
     * expungeUnused()
     * ----------------------------------------------------------------- */

    @Test
    public void expungeUnusedShouldRemoveNeverReadEntries() {
        // Access only "apple" and "banana"
        trackingMap.get("apple");
        trackingMap.containsKey("banana");

        // Before expunge all three entries exist
        assertEquals(3, trackingMap.size());

        trackingMap.expungeUnused();

        // "cherry" was never accessed → removed
        assertEquals(2, trackingMap.size());
        assertFalse(trackingMap.containsKey("cherry"));
        assertTrue(trackingMap.containsKey("apple"));
        assertTrue(trackingMap.containsKey("banana"));
    }

    @Test
    public void expungeUnusedShouldLeaveAllIfAllKeysRead() {
        trackingMap.get("apple");
        trackingMap.get("banana");
        trackingMap.get("cherry");
        trackingMap.expungeUnused();
        assertEquals(3, trackingMap.size());
    }

    @Test
    public void expungeUnusedWithNoReadKeysClearsMap() {
        trackingMap.clear(); // also clears readKeys
        trackingMap.putAll(backingMap);
        // readKeys is empty
        trackingMap.expungeUnused();
        assertTrue(trackingMap.isEmpty());
    }

    /* -----------------------------------------------------------------
     * informAdditionalUsage(Collection) and informAdditionalUsage(TrackingMap)
     * ----------------------------------------------------------------- */

    @Test
    public void informAdditionalUsageCollectionShouldAddProvidedKeysToReadSet() {
        Collection<String> extra = Arrays.asList("cherry", "date");
        trackingMap.informAdditionalUsage(extra);
        assertTrue(trackingMap.keysUsed().containsAll(extra));
    }

    @Test
    public void informAdditionalUsageTrackingMapShouldMergeReadKeys() {
        TrackingMap<String, Integer> other = new TrackingMap<>(new HashMap<>(backingMap));
        other.get("apple");
        other.get("cherry");
        trackingMap.informAdditionalUsage(other);
        assertTrue(trackingMap.keysUsed().containsAll(Arrays.asList("apple", "cherry")));
    }

    /* -----------------------------------------------------------------
     * keysUsed() and getWrappedMap()
     * ----------------------------------------------------------------- */

    @Test
    public void keysUsedShouldReturnLiveSet() {
        trackingMap.get("apple");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("apple"));
        // modifying the returned set must affect the internal state
        used.add("banana");
        assertTrue(trackingMap.keysUsed().contains("banana"));
    }

    @Test
    public void getWrappedMapShouldReturnOriginalReference() {
        assertSame(backingMap, trackingMap.getWrappedMap());
    }

    /* -----------------------------------------------------------------
     * setWrappedMap()
     * ----------------------------------------------------------------- */

    @Test
    public void setWrappedMapShouldReplaceUnderlyingMapAndResetReadKeys() {
        Map<String, Integer> newMap = new HashMap<>();
        newMap.put("kiwi", 10);
        newMap.put("lemon", 11);

        // mark some keys as read before replacement
        trackingMap.get("apple");
        trackingMap.get("banana");

        trackingMap.setWrappedMap(newMap);

        // underlying map replaced
        assertEquals(2, trackingMap.size());
        assertTrue(trackingMap.containsKey("kiwi"));
        assertTrue(trackingMap.containsKey("lemon"));
        assertFalse(trackingMap.containsKey("apple"));

        // read‑key set cleared
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    @Test
    public void setWrappedMapShouldRejectNullArgument() {
        try {
            trackingMap.setWrappedMap(null);
            fail("Expected an exception when passing null to setWrappedMap");
        } catch (RuntimeException e) {
            // Convention.throwIfNull is expected to throw a RuntimeException
            // (IllegalArgumentException or NullPointerException).  Any such
            // exception is acceptable for this test.
        }
    }
}
