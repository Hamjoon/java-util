package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link TrackingMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (including illegal arguments)</li>
 *   <li>All Map‑interface methods</li>
 *   <li>Tracking specific behaviour (keysUsed, expungeUnused, informAdditionalUsage)</li>
 *   <li>Object contract methods (equals, hashCode, toString)</li>
 *   <li>Interaction between the wrapped map and the tracking wrapper</li>
 * </ul>
 */
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

    /* ---------- Constructor ---------- */

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldRejectNullMap() {
        new TrackingMap<>(null);
    }

    @Test
    public void constructorShouldWrapGivenMap() {
        assertSame("Wrapped map must be the same instance passed to the constructor",
                backingMap, trackingMap.getWrappedMap());
    }

    /* ---------- get ---------- */

    @Test
    public void getShouldReturnValueAndMarkKeyAsRead() {
        assertEquals(Integer.valueOf(2), trackingMap.get("two"));
        assertTrue(trackingMap.keysUsed().contains("two"));
    }

    @Test
    public void getWithMissingKeyReturnsNullButMarksKeyAsRead() {
        assertNull(trackingMap.get("missing"));
        assertTrue(trackingMap.keysUsed().contains("missing"));
    }

    /* ---------- put ---------- */

    @Test
    public void putShouldDelegateToWrappedMap() {
        Integer previous = trackingMap.put("four", 4);
        assertNull(previous);
        assertEquals(Integer.valueOf(4), backingMap.get("four"));
        assertEquals(4, trackingMap.size());
    }

    @Test
    public void putShouldReplaceExistingValue() {
        Integer previous = trackingMap.put("one", 10);
        assertEquals(Integer.valueOf(1), previous);
        assertEquals(Integer.valueOf(10), backingMap.get("one"));
    }

    /* ---------- containsKey ---------- */

    @Test
    public void containsKeyShouldReturnTrueAndMarkKey() {
        assertTrue(trackingMap.containsKey("three"));
        assertTrue(trackingMap.keysUsed().contains("three"));
    }

    @Test
    public void containsKeyShouldReturnFalseAndMarkKey() {
        assertFalse(trackingMap.containsKey("absent"));
        assertTrue(trackingMap.keysUsed().contains("absent"));
    }

    /* ---------- putAll ---------- */

    @Test
    public void putAllShouldAddAllEntriesFromOtherMap() {
        Map<String, Integer> extra = new HashMap<>();
        extra.put("five", 5);
        extra.put("six", 6);
        trackingMap.putAll(extra);

        assertEquals(Integer.valueOf(5), trackingMap.get("five"));
        assertEquals(Integer.valueOf(6), trackingMap.get("six"));
        assertEquals(5, trackingMap.size());
    }

    /* ---------- remove ---------- */

    @Test
    public void removeShouldDeleteEntryAndUntrackKey() {
        // First read the key so it is tracked
        trackingMap.get("one");
        assertTrue(trackingMap.keysUsed().contains("one"));

        // Now remove it
        Integer removed = trackingMap.remove("one");
        assertEquals(Integer.valueOf(1), removed);
        assertFalse(backingMap.containsKey("one"));
        assertFalse(trackingMap.keysUsed().contains("one"));
    }

    @Test
    public void removeNonExistingKeyShouldReturnNullAndLeaveReadKeysUntouched() {
        trackingMap.keysUsed().add("ghost");
        assertNull(trackingMap.remove("ghost"));
        assertFalse(trackingMap.keysUsed().contains("ghost"));
    }

    /* ---------- size & isEmpty ---------- */

    @Test
    public void sizeShouldReflectWrappedMapSize() {
        assertEquals(3, trackingMap.size());
        trackingMap.put("new", 7);
        assertEquals(4, trackingMap.size());
    }

    @Test
    public void isEmptyShouldDelegateToWrappedMap() {
        assertFalse(trackingMap.isEmpty());
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty());
    }

    /* ---------- equals ---------- */

    @Test
    public void equalsShouldReturnTrueForEquivalentMaps() {
        Map<String, Integer> other = new HashMap<>(backingMap);
        assertTrue(trackingMap.equals(other));
        assertTrue(trackingMap.equals(new TrackingMap<>(other)));
    }

    @Test
    public void equalsShouldReturnFalseForDifferentContent() {
        Map<String, Integer> other = new HashMap<>();
        other.put("different", 99);
        assertFalse(trackingMap.equals(other));
    }

    @Test
    public void equalsShouldReturnFalseForNonMapObjects() {
        assertFalse(trackingMap.equals("not a map"));
    }

    /* ---------- hashCode ---------- */

    @Test
    public void hashCodeShouldMatchWrappedMapHashCode() {
        assertEquals(backingMap.hashCode(), trackingMap.hashCode());
    }

    /* ---------- toString ---------- */

    @Test
    public void toStringShouldDelegateToWrappedMap() {
        assertEquals(backingMap.toString(), trackingMap.toString());
    }

    /* ---------- clear ---------- */

    @Test
    public void clearShouldEmptyBothMapAndReadKeys() {
        // Access some keys first
        trackingMap.get("one");
        trackingMap.containsKey("two");
        assertFalse(trackingMap.keysUsed().isEmpty());

        trackingMap.clear();

        assertTrue(trackingMap.isEmpty());
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    /* ---------- containsValue ---------- */

    @Test
    public void containsValueShouldDelegateToWrappedMap() {
        assertTrue(trackingMap.containsValue(1));
        assertFalse(trackingMap.containsValue(99));
    }

    /* ---------- values ---------- */

    @Test
    public void valuesShouldReturnCollectionBackedByWrappedMap() {
        Collection<Integer> vals = trackingMap.values();
        assertTrue(vals.containsAll(Arrays.asList(1, 2, 3)));
        // Mutate via the collection
        vals.remove(2);
        assertFalse(backingMap.containsValue(2));
        assertEquals(2, trackingMap.size());
    }

    /* ---------- keySet ---------- */

    @Test
    public void keySetShouldReturnWrappedMapKeySet() {
        Set<String> keys = trackingMap.keySet();
        assertEquals(new HashSet<>(Arrays.asList("one", "two", "three")), keys);
        // Mutate via the set
        keys.remove("three");
        assertFalse(backingMap.containsKey("three"));
        assertEquals(2, trackingMap.size());
    }

    /* ---------- entrySet ---------- */

    @Test
    public void entrySetShouldReflectWrappedMapEntries() {
        Set<Map.Entry<String, Integer>> entries = trackingMap.entrySet();
        assertEquals(3, entries.size());

        // Modify an entry
        for (Map.Entry<String, Integer> e : entries) {
            if (e.getKey().equals("one")) {
                e.setValue(42);
            }
        }
        assertEquals(Integer.valueOf(42), backingMap.get("one"));
    }

    /* ---------- expungeUnused ---------- */

    @Test
    public void expungeUnusedShouldRetainOnlyAccessedKeys() {
        // Access only "one" and "two"
        trackingMap.get("one");
        trackingMap.containsKey("two");
        // "three" remains unaccessed

        trackingMap.expungeUnused();

        assertTrue(backingMap.containsKey("one"));
        assertTrue(backingMap.containsKey("two"));
        assertFalse(backingMap.containsKey("three"));
        assertEquals(2, trackingMap.size());
    }

    @Test
    public void expungeUnusedWhenNoKeysReadShouldClearMap() {
        // No reads performed
        trackingMap.expungeUnused();
        assertTrue(trackingMap.isEmpty());
    }

    /* ---------- informAdditionalUsage(Collection) ---------- */

    @Test
    public void informAdditionalUsageWithCollectionShouldAddKeysToReadSet() {
        Collection<String> extra = Arrays.asList("alpha", "beta");
        trackingMap.informAdditionalUsage(extra);
        assertTrue(trackingMap.keysUsed().containsAll(extra));
    }

    @Test
    public void informAdditionalUsageCollectionCanAddNonexistentKey() {
        Collection<String> extra = Collections.singletonList("ghost");
        trackingMap.informAdditionalUsage(extra);
        assertTrue(trackingMap.keysUsed().contains("ghost"));
        // The underlying map is unchanged
        assertFalse(backingMap.containsKey("ghost"));
    }

    @Test
    public void informAdditionalUsageWithNullKeyInCollection() {
        Collection<String> extra = Collections.singletonList(null);
        trackingMap.informAdditionalUsage(extra);
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    /* ---------- informAdditionalUsage(TrackingMap) ---------- */

    @Test
    public void informAdditionalUsageWithAnotherTrackingMapShouldMergeReadKeys() {
        TrackingMap<String, Integer> other = new TrackingMap<>(new HashMap<>());
        other.get("x"); // marks "x"
        other.containsKey("y"); // marks "y"
        trackingMap.get("one"); // mark "one"

        trackingMap.informAdditionalUsage(other);
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.containsAll(Arrays.asList("one", "x", "y")));
    }

    /* ---------- keysUsed ---------- */

    @Test
    public void keysUsedShouldReflectAllReadOperations() {
        trackingMap.get("one");
        trackingMap.containsKey("two");
        trackingMap.get("missing");
        Set<String> used = trackingMap.keysUsed();
        assertEquals(new HashSet<>(Arrays.asList("one", "two", "missing")), used);
    }

    /* ---------- getWrappedMap ---------- */

    @Test
    public void getWrappedMapShouldReturnOriginalMapReference() {
        Map<String, Integer> wrapped = trackingMap.getWrappedMap();
        assertSame(backingMap, wrapped);
        // Mutate via returned reference and verify wrapper sees it
        wrapped.put("newKey", 100);
        assertEquals(Integer.valueOf(100), trackingMap.get("newKey"));
    }
}
