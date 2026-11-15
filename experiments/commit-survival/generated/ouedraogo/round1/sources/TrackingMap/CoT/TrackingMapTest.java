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
        trackingMap = new TrackingMap<>(backingMap);
    }

    /* ---------- 1. Constructor ---------- */
    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldThrowWhenMapIsNull() {
        new TrackingMap<String, Integer>(null);
    }

    /* ---------- 2. put & get ---------- */
    @Test
    public void putAndGetShouldStoreAndRetrieveValue() {
        assertNull(trackingMap.put("a", 1));
        assertEquals(Integer.valueOf(1), trackingMap.get("a"));
        assertTrue(trackingMap.keysUsed().contains("a"));
    }

    @Test
    public void getWithNullKeyShouldWorkAndTrackNull() {
        backingMap.put(null, 99);
        assertEquals(Integer.valueOf(99), trackingMap.get(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    /* ---------- 3. containsKey ---------- */
    @Test
    public void containsKeyShouldReportPresenceAndTrackKey() {
        backingMap.put("x", 10);
        assertTrue(trackingMap.containsKey("x"));
        assertTrue(trackingMap.keysUsed().contains("x"));
    }

    @Test
    public void containsKeyWithNullShouldTrackNull() {
        backingMap.put(null, 5);
        assertTrue(trackingMap.containsKey(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    /* ---------- 4. putAll ---------- */
    @Test
    public void putAllShouldCopyAllEntries() {
        Map<String, Integer> other = new HashMap<>();
        other.put("b", 2);
        other.put("c", 3);
        trackingMap.putAll(other);
        assertEquals(Integer.valueOf(2), trackingMap.get("b"));
        assertEquals(Integer.valueOf(3), trackingMap.get("c"));
    }

    /* ---------- 5. remove ---------- */
    @Test
    public void removeShouldDeleteEntryAndNotTrackKey() {
        trackingMap.put("del", 7);
        trackingMap.get("del"); // mark as read first
        assertEquals(Integer.valueOf(7), trackingMap.remove("del"));
        assertFalse(trackingMap.keySet().contains("del"));
        // after removal the key should no longer be in the read set
        assertFalse(trackingMap.keysUsed().contains("del"));
    }

    @Test
    public void removeNullKeyShouldWork() {
        backingMap.put(null, 42);
        assertEquals(Integer.valueOf(42), trackingMap.remove(null));
        assertFalse(trackingMap.keysUsed().contains(null));
    }

    /* ---------- 6. size & isEmpty ---------- */
    @Test
    public void sizeAndIsEmptyReflectBackingMap() {
        assertTrue(trackingMap.isEmpty());
        assertEquals(0, trackingMap.size());

        trackingMap.put("k", 1);
        assertFalse(trackingMap.isEmpty());
        assertEquals(1, trackingMap.size());
    }

    /* ---------- 7. equals & hashCode ---------- */
    @Test
    public void equalsShouldReturnTrueForEqualUnderlyingMaps() {
        Map<String, Integer> otherMap = new HashMap<>();
        otherMap.put("a", 1);
        trackingMap.put("a", 1);

        TrackingMap<String, Integer> otherTracking = new TrackingMap<>(otherMap);
        assertTrue(trackingMap.equals(otherTracking));
    }

    @Test
    public void equalsShouldReturnFalseForNonMapObject() {
        assertFalse(trackingMap.equals("not a map"));
    }

    @Test
    public void hashCodeShouldMatchUnderlyingMapHashCode() {
        trackingMap.put("x", 10);
        trackingMap.put("y", 20);
        assertEquals(backingMap.hashCode(), trackingMap.hashCode());
    }

    /* ---------- 8. toString ---------- */
    @Test
    public void toStringShouldDelegateToBackingMap() {
        trackingMap.put("z", 99);
        assertEquals(backingMap.toString(), trackingMap.toString());
    }

    /* ---------- 9. clear ---------- */
    @Test
    public void clearShouldEmptyBothMapAndReadKeys() {
        trackingMap.put("a", 1);
        trackingMap.get("a"); // mark as read
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty());
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    /* ---------- 10. containsValue ---------- */
    @Test
    public void containsValueShouldDelegateToBackingMap() {
        trackingMap.put("k1", 100);
        assertTrue(trackingMap.containsValue(100));
        assertFalse(trackingMap.containsValue(200));
    }

    /* ---------- 11. values, keySet, entrySet ---------- */
    @Test
    public void valuesKeySetAndEntrySetShouldReflectBackingMap() {
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);

        Collection<Integer> values = trackingMap.values();
        assertTrue(values.containsAll(Arrays.asList(1, 2)));

        Set<String> keys = trackingMap.keySet();
        assertTrue(keys.containsAll(Arrays.asList("a", "b")));

        Set<Map.Entry<String, Integer>> entries = trackingMap.entrySet();
        for (Map.Entry<String, Integer> e : entries) {
            assertEquals(trackingMap.get(e.getKey()), e.getValue());
        }
    }

    /* ---------- 12. expungeUnused ---------- */
    @Test
    public void expungeUnusedShouldRemoveNeverReadEntries() {
        trackingMap.put("keep", 1);
        trackingMap.put("discard", 2);
        trackingMap.get("keep"); // only "keep" is read
        trackingMap.expungeUnused();

        assertTrue(trackingMap.containsKey("keep"));
        assertFalse(trackingMap.containsKey("discard"));
        assertEquals(1, trackingMap.size());
    }

    @Test
    public void expungeUnusedWhenNoKeysReadShouldClearMap() {
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        // no reads
        trackingMap.expungeUnused();
        assertTrue(trackingMap.isEmpty());
    }

    /* ---------- 13. informAdditionalUsage(Collection) ---------- */
    @Test
    public void informAdditionalUsageWithEmptyCollectionShouldNotChangeKeysUsed() {
        trackingMap.put("x", 5);
        Set<String> before = new HashSet<>(trackingMap.keysUsed());

        trackingMap.informAdditionalUsage(Collections.emptyList());
        assertEquals(before, trackingMap.keysUsed());
    }

    @Test
    public void informAdditionalUsageWithExtraKeysShouldAddThemToKeysUsed() {
        trackingMap.put("present", 1);
        List<String> extra = Arrays.asList("present", "absent");
        trackingMap.informAdditionalUsage(extra);

        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("present"));
        assertTrue(used.contains("absent"));
    }

    /* ---------- 14. informAdditionalUsage(TrackingMap) ---------- */
    @Test
    public void informAdditionalUsageWithAnotherTrackingMapShouldMergeReadKeys() {
        TrackingMap<String, Integer> other = new TrackingMap<>(new HashMap<>());
        other.put("alpha", 10);
        other.get("alpha"); // mark as read in other

        other.put("beta", 20);
        // do NOT read "beta" in other

        trackingMap.put("gamma", 30);
        trackingMap.get("gamma"); // read in first map

        trackingMap.informAdditionalUsage(other);
        Set<String> used = trackingMap.keysUsed();

        assertTrue(used.contains("alpha"));   // from other
        assertTrue(used.contains("gamma"));  // original
        assertFalse(used.contains("beta"));  // not read in other
    }

    /* ---------- 15. keysUsed & getWrappedMap ---------- */
    @Test
    public void keysUsedShouldReflectAllAccessesAndAdditionalInfo() {
        trackingMap.put("k1", 1);
        trackingMap.get("k1");
        trackingMap.containsKey("k1");

        trackingMap.informAdditionalUsage(Collections.singleton("extra"));
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.containsAll(Arrays.asList("k1", "extra")));
    }

    @Test
    public void getWrappedMapShouldReturnOriginalMapReference() {
        Map<String, Integer> inner = new HashMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(inner);
        assertSame(inner, tm.getWrappedMap());
    }
}
