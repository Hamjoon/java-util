package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TrackingMap}.
 *
 * The tests are written as if three independent experts each contributed
 * one test case per public method, shared their findings and then proceeded
 * to the next method.  The final suite therefore covers typical usage,
 * edge‑cases (null keys/values, empty maps, duplicate keys, etc.) and
 * error scenarios (null constructor argument, illegal state after removal,
 * etc.).
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

    /*** Constructor ***/
    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldThrowWhenMapIsNull() {
        new TrackingMap<>(null);
    }

    @Test
    public void constructorShouldWrapGivenMap() {
        assertSame("The wrapped map should be the one supplied at construction",
                backingMap, trackingMap.getWrappedMap());
        assertTrue("Initially no keys are marked as accessed", trackingMap.keysUsed().isEmpty());
    }

    /*** get ***/
    @Test
    public void getShouldReturnValueAndMarkKeyAsAccessed() {
        Integer val = trackingMap.get("apple");
        assertEquals(Integer.valueOf(1), val);
        assertTrue(trackingMap.keysUsed().contains("apple"));
    }

    @Test
    public void getWithMissingKeyReturnsNullAndMarksKey() {
        assertNull(trackingMap.get("durian"));
        assertTrue(trackingMap.keysUsed().contains("durian"));
    }

    @Test
    public void getWithNullKeyWorksWhenUnderlyingMapAllowsNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put(null, 99);
        TrackingMap<String, Integer> tm = new TrackingMap<>(map);
        assertEquals(Integer.valueOf(99), tm.get(null));
        assertTrue(tm.keysUsed().contains(null));
    }

    /*** put ***/
    @Test
    public void putShouldAddOrReplaceEntry() {
        Integer previous = trackingMap.put("date", 4);
        assertNull(previous);
        assertEquals(Integer.valueOf(4), trackingMap.get("date"));
        // put does NOT mark the key as accessed
        assertFalse(trackingMap.keysUsed().contains("date"));
    }

    @Test
    public void putShouldReturnPreviousValueWhenKeyExists() {
        Integer previous = trackingMap.put("banana", 20);
        assertEquals(Integer.valueOf(2), previous);
        assertEquals(Integer.valueOf(20), trackingMap.get("banana"));
    }

    /*** containsKey ***/
    @Test
    public void containsKeyShouldReturnTrueAndMarkKey() {
        assertTrue(trackingMap.containsKey("banana"));
        assertTrue(trackingMap.keysUsed().contains("banana"));
    }

    @Test
    public void containsKeyShouldReturnFalseAndMarkKey() {
        assertFalse(trackingMap.containsKey("fig"));
        assertTrue(trackingMap.keysUsed().contains("fig"));
    }

    /*** putAll ***/
    @Test
    public void putAllShouldCopyAllMappings() {
        Map<String, Integer> extra = new HashMap<>();
        extra.put("elder", 5);
        extra.put("fig", 6);
        trackingMap.putAll(extra);
        assertEquals(Integer.valueOf(5), trackingMap.get("elder"));
        assertEquals(Integer.valueOf(6), trackingMap.get("fig"));
        // putAll does not affect the accessed‑keys set
        assertFalse(trackingMap.keysUsed().contains("elder"));
        assertFalse(trackingMap.keysUsed().contains("fig"));
    }

    /*** remove ***/
    @Test
    public void removeShouldDeleteEntryAndForgetAccess() {
        // first access the key so it is recorded
        trackingMap.get("cherry");
        assertTrue(trackingMap.keysUsed().contains("cherry"));
        // now remove it
        Integer removed = trackingMap.remove("cherry");
        assertEquals(Integer.valueOf(3), removed);
        assertFalse(trackingMap.containsKey("cherry"));
        assertFalse(trackingMap.keysUsed().contains("cherry"));
    }

    @Test
    public void removeNonExistingKeyReturnsNull() {
        assertNull(trackingMap.remove("nonexistent"));
    }

    /*** size & isEmpty ***/
    @Test
    public void sizeAndIsEmptyReflectUnderlyingMap() {
        assertEquals(3, trackingMap.size());
        assertFalse(trackingMap.isEmpty());

        trackingMap.clear();
        assertEquals(0, trackingMap.size());
        assertTrue(trackingMap.isEmpty());
    }

    /*** equals & hashCode ***/
    @Test
    public void equalsShouldDelegateToUnderlyingMap() {
        Map<String, Integer> other = new HashMap<>(backingMap);
        assertTrue(trackingMap.equals(other));
        other.put("new", 7);
        assertFalse(trackingMap.equals(other));
    }

    @Test
    public void hashCodeShouldMatchUnderlyingMap() {
        assertEquals(backingMap.hashCode(), trackingMap.hashCode());
        trackingMap.put("new", 7);
        assertEquals(backingMap.hashCode(), trackingMap.hashCode());
    }

    /*** toString ***/
    @Test
    public void toStringShouldMirrorUnderlyingMapToString() {
        assertEquals(backingMap.toString(), trackingMap.toString());
    }

    /*** clear ***/
    @Test
    public void clearShouldEmptyMapAndAccessSet() {
        trackingMap.get("apple");
        trackingMap.get("banana");
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty());
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    /*** containsValue ***/
    @Test
    public void containsValueShouldDelegateToWrappedMap() {
        assertTrue(trackingMap.containsValue(2));
        assertFalse(trackingMap.containsValue(99));
    }

    /*** values, keySet, entrySet ***/
    @Test
    public void collectionViewsShouldReflectWrappedMap() {
        Collection<Integer> values = trackingMap.values();
        Set<String> keys = trackingMap.keySet();
        Set<Map.Entry<String, Integer>> entries = trackingMap.entrySet();

        assertEquals(3, values.size());
        assertTrue(values.containsAll(Arrays.asList(1, 2, 3)));

        assertEquals(3, keys.size());
        assertTrue(keys.containsAll(Arrays.asList("apple", "banana", "cherry")));

        assertEquals(3, entries.size());
        for (Map.Entry<String, Integer> e : entries) {
            assertEquals(backingMap.get(e.getKey()), e.getValue());
        }
    }

    /*** expungeUnused ***/
    @Test
    public void expungeUnusedShouldRemoveNeverAccessedEntries() {
        // Access only "apple"
        trackingMap.get("apple");
        // Before expunge
        assertEquals(3, trackingMap.size());
        trackingMap.expungeUnused();
        // Only "apple" should remain
        assertEquals(1, trackingMap.size());
        assertTrue(trackingMap.containsKey("apple"));
        assertFalse(trackingMap.containsKey("banana"));
        assertFalse(trackingMap.containsKey("cherry"));
    }

    @Test
    public void expungeUnusedShouldKeepEntriesIfTheirKeyWasOnlyMarkedViaContainsKey() {
        trackingMap.containsKey("banana");
        trackingMap.expungeUnused();
        assertTrue(trackingMap.containsKey("banana"));
    }

    @Test
    public void expungeUnusedShouldIgnoreKeysInReadSetThatAreNotInMap() {
        // Add a key to the used‑set that does not exist in the map
        Set<String> extra = new HashSet<>();
        extra.add("ghost");
        trackingMap.informAdditionalUsage(extra);
        // No exception should be thrown and map stays unchanged after expunge
        trackingMap.expungeUnused();
        assertEquals(3, trackingMap.size());
        assertTrue(trackingMap.containsKey("apple"));
    }

    /*** informAdditionalUsage(Collection) ***/
    @Test
    public void informAdditionalUsageCollectionShouldAddAllGivenKeysToReadSet() {
        Collection<String> extra = Arrays.asList("banana", "date");
        trackingMap.informAdditionalUsage(extra);
        assertTrue(trackingMap.keysUsed().containsAll(extra));
        // After expunge, "date" (not present) is ignored, "banana" stays
        trackingMap.expungeUnused();
        assertTrue(trackingMap.containsKey("banana"));
        assertFalse(trackingMap.containsKey("apple")); // not accessed
    }

    /*** informAdditionalUsage(TrackingMap) ***/
    @Test
    public void informAdditionalUsageTrackingMapShouldMergeReadKeys() {
        TrackingMap<String, Integer> other = new TrackingMap<>(new HashMap<>());
        other.get("apple");   // mark apple
        other.get("durian");  // mark durian (not in original map)
        trackingMap.informAdditionalUsage(other);
        assertTrue(trackingMap.keysUsed().containsAll(Arrays.asList("apple", "durian")));
        // Expunge should keep apple (present) and drop durian (absent)
        trackingMap.expungeUnused();
        assertTrue(trackingMap.containsKey("apple"));
        assertFalse(trackingMap.containsKey("banana"));
        assertFalse(trackingMap.containsKey("durian"));
    }

    /*** keysUsed ***/
    @Test
    public void keysUsedShouldReflectAllAccessedKeysSoFar() {
        trackingMap.get("apple");
        trackingMap.containsKey("banana");
        Set<String> used = trackingMap.keysUsed();
        assertEquals(2, used.size());
        assertTrue(used.containsAll(Arrays.asList("apple", "banana")));
    }

    /*** getWrappedMap ***/
    @Test
    public void getWrappedMapShouldReturnOriginalReference() {
        Map<String, Integer> ref = trackingMap.getWrappedMap();
        assertSame(backingMap, ref);
        // Mutating through the reference should be visible through TrackingMap
        ref.put("kiwi", 8);
        assertEquals(Integer.valueOf(8), trackingMap.get("kiwi"));
    }
}
