package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link TrackingMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (including null‑argument validation)</li>
 *   <li>Standard {@link Map} operations: put, get, remove, containsKey, containsValue,
 *       size, isEmpty, clear, keySet, values, entrySet</li>
 *   <li>Tracking behaviour: {@code keysUsed()}, {@code expungeUnused()}, {@code informAdditionalUsage(...)} </li>
 *   <li>Utility methods: {@code equals()}, {@code hashCode()}, {@code toString()}, {@code getWrappedMap()},
 *       {@code setWrappedMap()}</li>
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

    /* ---------------------------------------------------------------------- */
    /* Construction                                                            */
    /* ---------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void constructorShouldRejectNullMap() {
        new TrackingMap<>(null);
    }

    @Test
    public void constructorInitialisesReadKeysEmpty() {
        assertTrue("readKeys should be empty after construction",
                trackingMap.keysUsed().isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* Basic Map operations                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void putAndGetWorkAsExpected() {
        assertEquals(Integer.valueOf(1), trackingMap.get("apple"));
        assertEquals(Integer.valueOf(2), trackingMap.get("banana"));
        assertEquals(Integer.valueOf(3), trackingMap.get("cherry"));

        // new entry
        trackingMap.put("date", 4);
        assertEquals(Integer.valueOf(4), trackingMap.get("date"));
        assertEquals(4, trackingMap.size());
    }

    @Test
    public void getTracksKeyEvenWhenValueIsNull() {
        trackingMap.put("empty", null);
        assertNull(trackingMap.get("empty"));
        assertTrue(trackingMap.keysUsed().contains("empty"));
    }

    @Test
    public void getTracksNullKeyIfMapSupportsIt() {
        // HashMap supports null keys
        trackingMap.put(null, 99);
        assertEquals(Integer.valueOf(99), trackingMap.get(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    @Test
    public void containsKeyTracksKey() {
        assertTrue(trackingMap.containsKey("banana"));
        assertTrue(trackingMap.keysUsed().contains("banana"));

        assertFalse(trackingMap.containsKey("nonexistent"));
        assertTrue(trackingMap.keysUsed().contains("nonexistent"));
    }

    @Test
    public void removeUpdatesReadKeys() {
        // Access a key first, then remove it
        trackingMap.get("apple");
        assertTrue(trackingMap.keysUsed().contains("apple"));

        trackingMap.remove("apple");
        assertFalse(trackingMap.keysUsed().contains("apple"));
        assertFalse(trackingMap.containsKey("apple"));
    }

    @Test
    public void removeNonExistingKeyDoesNotThrow() {
        assertNull(trackingMap.remove("ghost"));
        // readKeys should not gain a null entry
        assertFalse(trackingMap.keysUsed().contains("ghost"));
    }

    @Test
    public void clearEmptiesMapAndReadKeys() {
        trackingMap.get("apple");
        trackingMap.containsKey("banana");
        assertFalse(trackingMap.keysUsed().isEmpty());

        trackingMap.clear();
        assertTrue(trackingMap.isEmpty());
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    @Test
    public void sizeAndIsEmptyReflectUnderlyingMap() {
        assertEquals(3, trackingMap.size());
        assertFalse(trackingMap.isEmpty());

        trackingMap.clear();
        assertEquals(0, trackingMap.size());
        assertTrue(trackingMap.isEmpty());
    }

    @Test
    public void containsValueDelegatesToUnderlyingMap() {
        assertTrue(trackingMap.containsValue(2));
        assertFalse(trackingMap.containsValue(99));
    }

    @Test
    public void keySetValuesAndEntrySetReflectUnderlyingMap() {
        Set<String> expectedKeys = new HashSet<>();
        Collections.addAll(expectedKeys, "apple", "banana", "cherry");
        assertEquals(expectedKeys, trackingMap.keySet());

        Collection<Integer> expectedValues = new HashSet<>();
        Collections.addAll(expectedValues, 1, 2, 3);
        assertEquals(expectedValues, new HashSet<>(trackingMap.values()));

        Set<Map.Entry<String, Integer>> entries = trackingMap.entrySet();
        assertEquals(3, entries.size());
        for (Map.Entry<String, Integer> e : entries) {
            assertEquals(backingMap.get(e.getKey()), e.getValue());
        }
    }

    @Test
    public void putAllCopiesAllEntries() {
        Map<String, Integer> other = new HashMap<>();
        other.put("eggplant", 5);
        other.put("fig", 6);
        trackingMap.putAll(other);
        assertEquals(Integer.valueOf(5), trackingMap.get("eggplant"));
        assertEquals(Integer.valueOf(6), trackingMap.get("fig"));
        assertEquals(5, trackingMap.size());
    }

    /* ---------------------------------------------------------------------- */
    /* Tracking specific behaviour                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void expungeUnusedRemovesOnlyNeverAccessedKeys() {
        // Access only "apple" and "banana"
        trackingMap.get("apple");
        trackingMap.containsKey("banana");

        // Before expunge, all keys present
        assertEquals(3, trackingMap.size());

        trackingMap.expungeUnused();

        // "cherry" was never accessed, should be gone
        assertFalse(trackingMap.containsKey("cherry"));
        assertEquals(2, trackingMap.size());

        // The used keys set should still contain the accessed keys
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("apple"));
        assertTrue(used.contains("banana"));
        assertTrue(used.contains("cherry")); // note: keysUsed records attempts, even if later removed
    }

    @Test
    public void informAdditionalUsageWithCollectionAddsKeys() {
        Set<String> extra = new HashSet<>();
        extra.add("date");
        extra.add("elderberry");

        trackingMap.informAdditionalUsage(extra);
        assertTrue(trackingMap.keysUsed().containsAll(extra));
    }

    @Test
    public void informAdditionalUsageWithAnotherTrackingMapMergesReadKeys() {
        TrackingMap<String, Integer> other = new TrackingMap<>(new HashMap<>());
        other.get("apple"); // mark apple as used in other
        other.get("fig");   // mark fig (not present in original) as used

        trackingMap.informAdditionalUsage(other);
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("apple"));
        assertTrue(used.contains("fig"));
    }

    @Test
    public void keysUsedReturnsLiveSet() {
        trackingMap.get("apple");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("apple"));

        // Mutate via the returned set should affect the internal tracking set
        used.remove("apple");
        assertFalse(trackingMap.keysUsed().contains("apple"));
    }

    /* ---------------------------------------------------------------------- */
    /* Utility methods                                                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void equalsAndHashCodeDelegateToUnderlyingMap() {
        Map<String, Integer> sameContent = new HashMap<>();
        sameContent.put("apple", 1);
        sameContent.put("banana", 2);
        sameContent.put("cherry", 3);

        assertTrue(trackingMap.equals(sameContent));
        assertEquals(trackingMap.hashCode(), sameContent.hashCode());

        // Different content => not equal
        sameContent.put("date", 4);
        assertFalse(trackingMap.equals(sameContent));
    }

    @Test
    public void toStringMatchesUnderlyingMapToString() {
        assertEquals(backingMap.toString(), trackingMap.toString());
    }

    @Test
    public void getWrappedMapReturnsOriginalReference() {
        assertSame(backingMap, trackingMap.getWrappedMap());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setWrappedMapRejectsNull() {
        trackingMap.setWrappedMap(null);
    }

    @Test
    public void setWrappedMapReplacesContentAndResetsReadKeys() {
        // Access a key so readKeys is non‑empty
        trackingMap.get("apple");
        assertTrue(trackingMap.keysUsed().contains("apple"));

        Map<String, Integer> newMap = new HashMap<>();
        newMap.put("x", 100);
        newMap.put("y", 200);

        trackingMap.setWrappedMap(newMap);

        // Underlying map should be replaced
        assertEquals(2, trackingMap.size());
        assertTrue(trackingMap.containsKey("x"));
        assertTrue(trackingMap.containsKey("y"));
        assertFalse(trackingMap.containsKey("apple"));

        // readKeys must have been cleared by setWrappedMap (clear is called internally)
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* Edge Cases                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void containsKeyDoesNotThrowOnNullKeyWhenUnderlyingMapSupportsIt() {
        // HashMap supports null keys
        trackingMap.put(null, 42);
        assertTrue(trackingMap.containsKey(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    @Test
    public void removeNullKeyUpdatesReadKeysCorrectly() {
        trackingMap.put(null, 11);
        trackingMap.get(null); // adds null to readKeys
        assertTrue(trackingMap.keysUsed().contains(null));

        trackingMap.remove(null);
        assertFalse(trackingMap.keysUsed().contains(null));
        assertFalse(trackingMap.containsKey(null));
    }
}
