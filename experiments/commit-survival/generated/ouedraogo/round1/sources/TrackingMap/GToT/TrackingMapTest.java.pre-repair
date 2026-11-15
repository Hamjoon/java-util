package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class TrackingMapTest {

    private Map<String, Integer> backingMap;
    private TrackingMap<String, Integer> trackingMap;

    @Before
    public void setUp() {
        backingMap = new HashMap<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        trackingMap = new TrackingMap<>(backingMap);
    }

    /* ---------------------------------------------------------------------- */
    /* Constructor                                                             */
    /* ---------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldRejectNullMap() {
        new TrackingMap<>(null);
    }

    /* ---------------------------------------------------------------------- */
    /* get()                                                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void getShouldReturnValueAndMarkKeyAsRead() {
        assertEquals(Integer.valueOf(2), trackingMap.get("two"));
        assertTrue(trackingMap.keysUsed().contains("two"));
    }

    @Test
    public void getWithMissingKeyReturnsNullAndMarksKey() {
        assertNull(trackingMap.get("missing"));
        assertTrue(trackingMap.keysUsed().contains("missing"));
    }

    @Test
    public void getWithNullKey() {
        // HashMap permits null keys, so this should work and be recorded.
        backingMap.put(null, 99);
        assertEquals(Integer.valueOf(99), trackingMap.get(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    /* ---------------------------------------------------------------------- */
    /* put()                                                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void putShouldAddEntry() {
        assertNull(trackingMap.put("four", 4));
        assertEquals(Integer.valueOf(4), trackingMap.get("four"));
        assertEquals(4, trackingMap.size());
    }

    @Test
    public void putOverwriteShouldReturnOldValue() {
        Integer old = trackingMap.put("one", 10);
        assertEquals(Integer.valueOf(1), old);
        assertEquals(Integer.valueOf(10), trackingMap.get("one"));
    }

    /* ---------------------------------------------------------------------- */
    /* containsKey()                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void containsKeyShouldReturnTrueAndMarkKey() {
        assertTrue(trackingMap.containsKey("three"));
        assertTrue(trackingMap.keysUsed().contains("three"));
    }

    @Test
    public void containsKeyWithMissingKeyReturnsFalseAndMarksKey() {
        assertFalse(trackingMap.containsKey("missing"));
        assertTrue(trackingMap.keysUsed().contains("missing"));
    }

    @Test
    public void containsKeyWithNull() {
        backingMap.put(null, 0);
        assertTrue(trackingMap.containsKey(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    /* ---------------------------------------------------------------------- */
    /* putAll()                                                                  */
    /* ---------------------------------------------------------------------- */
    @Test
    public void putAllShouldCopyAllEntries() {
        Map<String, Integer> extra = new HashMap<>();
        extra.put("five", 5);
        extra.put("six", 6);
        trackingMap.putAll(extra);
        assertEquals(Integer.valueOf(5), trackingMap.get("five"));
        assertEquals(Integer.valueOf(6), trackingMap.get("six"));
        assertEquals(5, trackingMap.size());
    }

    /* ---------------------------------------------------------------------- */
    /* remove()                                                                  */
    /* ---------------------------------------------------------------------- */
    @Test
    public void removeShouldDeleteEntryAndUnmarkKey() {
        // first read the key so it is in readKeys
        trackingMap.get("one");
        assertTrue(trackingMap.keysUsed().contains("one"));
        // now remove
        assertEquals(Integer.valueOf(1), trackingMap.remove("one"));
        assertFalse(trackingMap.containsKey("one"));
        // key should no longer be considered read
        assertFalse(trackingMap.keysUsed().contains("one"));
    }

    @Test
    public void removeNonExistingKeyReturnsNull() {
        assertNull(trackingMap.remove("missing"));
    }

    @Test
    public void removeNullKey() {
        backingMap.put(null, 0);
        trackingMap.get(null); // mark it as read
        assertEquals(Integer.valueOf(0), trackingMap.remove(null));
        assertFalse(trackingMap.keysUsed().contains(null));
    }

    /* ---------------------------------------------------------------------- */
    /* size(), isEmpty()                                                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void sizeAndIsEmptyReflectBackingMap() {
        assertEquals(3, trackingMap.size());
        assertFalse(trackingMap.isEmpty());

        trackingMap.clear();
        assertEquals(0, trackingMap.size());
        assertTrue(trackingMap.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* clear()                                                                   */
    /* ---------------------------------------------------------------------- */
    @Test
    public void clearShouldEmptyBothMapsAndReadKeys() {
        trackingMap.get("one");
        trackingMap.get("two");
        assertFalse(trackingMap.keysUsed().isEmpty());

        trackingMap.clear();
        assertTrue(trackingMap.isEmpty());
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* equals() & hashCode()                                                     */
    /* ---------------------------------------------------------------------- */
    @Test
    public void equalsAndHashCodeDelegateToBackingMap() {
        Map<String, Integer> other = new HashMap<>(backingMap);
        assertTrue(trackingMap.equals(other));
        assertEquals(backingMap.hashCode(), trackingMap.hashCode());

        other.put("extra", 99);
        assertFalse(trackingMap.equals(other));
    }

    @Test
    public void equalsShouldReturnFalseForNonMap() {
        assertFalse(trackingMap.equals("not a map"));
    }

    /* ---------------------------------------------------------------------- */
    /* toString()                                                                */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toStringShouldMatchBackingMapToString() {
        assertEquals(backingMap.toString(), trackingMap.toString());
    }

    /* ---------------------------------------------------------------------- */
    /* containsValue(), values(), keySet(), entrySet()                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void delegateCollectionMethods() {
        assertTrue(trackingMap.containsValue(2));
        assertFalse(trackingMap.containsValue(99));

        Collection<Integer> values = trackingMap.values();
        assertTrue(values.containsAll(Arrays.asList(1, 2, 3)));
        assertEquals(3, values.size());

        Set<String> keys = trackingMap.keySet();
        assertTrue(keys.containsAll(Arrays.asList("one", "two", "three")));
        assertEquals(3, keys.size());

        Set<Map.Entry<String, Integer>> entries = trackingMap.entrySet();
        for (Map.Entry<String, Integer> e : entries) {
            assertEquals(backingMap.get(e.getKey()), e.getValue());
        }
        assertEquals(3, entries.size());
    }

    /* ---------------------------------------------------------------------- */
    /* expungeUnused()                                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void expungeUnusedShouldRemoveUnaccessedEntries() {
        // Access only "one" and "two"
        trackingMap.get("one");
        trackingMap.containsKey("two");

        // "three" has never been read
        assertTrue(trackingMap.keySet().contains("three"));
        trackingMap.expungeUnused();

        // After expunge, only accessed keys remain
        assertTrue(trackingMap.keySet().containsAll(Arrays.asList("one", "two")));
        assertFalse(trackingMap.keySet().contains("three"));
        assertEquals(2, trackingMap.size());
    }

    @Test
    public void expungeUnusedWhenAllKeysUsedLeavesMapIntact() {
        trackingMap.get("one");
        trackingMap.get("two");
        trackingMap.get("three");
        trackingMap.expungeUnused();
        assertEquals(3, trackingMap.size());
    }

    @Test
    public void expungeUnusedWhenNoKeysUsedClearsMap() {
        trackingMap.clear(); // also clears readKeys
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        // readKeys is empty
        trackingMap.expungeUnused();
        assertTrue(trackingMap.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* informAdditionalUsage(Collection)                                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void informAdditionalUsageCollectionAddsKeys() {
        Collection<String> extra = Arrays.asList("extra1", "extra2");
        trackingMap.informAdditionalUsage(extra);
        assertTrue(trackingMap.keysUsed().containsAll(extra));
    }

    @Test
    public void informAdditionalUsageCollectionWithExistingKeysDoesNotDuplicate() {
        trackingMap.get("one");
        Collection<String> extra = Collections.singletonList("one");
        trackingMap.informAdditionalUsage(extra);
        // Set semantics – still only one occurrence
        assertEquals(1, trackingMap.keysUsed().stream().filter(k -> "one".equals(k)).count());
    }

    @Test
    public void informAdditionalUsageCollectionEmptyNoEffect() {
        int before = trackingMap.keysUsed().size();
        trackingMap.informAdditionalUsage(Collections.emptyList());
        assertEquals(before, trackingMap.keysUsed().size());
    }

    /* ---------------------------------------------------------------------- */
    /* informAdditionalUsage(TrackingMap)                                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void informAdditionalUsageTrackingMapMergesReadKeys() {
        TrackingMap<String, Integer> other = new TrackingMap<>(new HashMap<>());
        other.get("alpha");
        other.get("beta");
        trackingMap.get("one");
        trackingMap.informAdditionalUsage(other);
        assertTrue(trackingMap.keysUsed().containsAll(Arrays.asList("one", "alpha", "beta")));
    }

    @Test
    public void informAdditionalUsageTrackingMapWithNoReadKeysNoEffect() {
        TrackingMap<String, Integer> other = new TrackingMap<>(new HashMap<>());
        int before = trackingMap.keysUsed().size();
        trackingMap.informAdditionalUsage(other);
        assertEquals(before, trackingMap.keysUsed().size());
    }

    /* ---------------------------------------------------------------------- */
    /* keysUsed() & getWrappedMap()                                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void keysUsedReturnsLiveSet() {
        trackingMap.get("one");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("one"));
        // modify via returned set should affect internal state
        used.remove("one");
        assertFalse(trackingMap.keysUsed().contains("one"));
    }

    @Test
    public void getWrappedMapReturnsOriginalMapReference() {
        Map<String, Integer> wrapped = trackingMap.getWrappedMap();
        assertSame(backingMap, wrapped);
        // Mutating the wrapped map reflects in the TrackingMap
        wrapped.put("newKey", 42);
        assertEquals(Integer.valueOf(42), trackingMap.get("newKey"));
    }
}
