package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link TrackingMap}.
 *
 * The test cases are organised as follows:
 * <ul>
 *   <li>Listing of all public methods (including signatures) of {@code TrackingMap}.</li>
 *   <li>Basic functional tests that verify the normal behaviour of each method.</li>
 *   <li>Edge‑case / exceptional‑scenario tests that target null handling,
 *       empty collections, and behaviour of {@code expungeUnused} and the
 *       {@code informAdditionalUsage} overloads.</li>
 * </ul>
 */
public class TrackingMapTest {

    // -------------------------------------------------------------------------
    //  Public methods of TrackingMap (including signatures)
    // -------------------------------------------------------------------------
    // 1. public TrackingMap(Map<K,V> map)
    // 2. public V get(Object key)
    // 3. public V put(K key, V value)
    // 4. public boolean containsKey(Object key)
    // 5. public void putAll(Map<? extends K, ? extends V> m)
    // 6. public V remove(Object key)
    // 7. public int size()
    // 8. public boolean isEmpty()
    // 9. public boolean equals(Object other)
    //10. public int hashCode()
    //11. public String toString()
    //12. public void clear()
    //13. public boolean containsValue(Object value)
    //14. public Collection<V> values()
    //15. public Set<K> keySet()
    //16. public Set<Entry<K,V>> entrySet()
    //17. public void expungeUnused()
    //18. public void informAdditionalUsage(Collection<K> additional)
    //19. public void informAdditionalUsage(TrackingMap<K,V> additional)
    //20. public Set<K> keysUsed()
    //21. public Map<K,V> getWrappedMap()

    // -------------------------------------------------------------------------
    //  Test fixtures
    // -------------------------------------------------------------------------
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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // -------------------------------------------------------------------------
    //  Basic functional tests
    // -------------------------------------------------------------------------

    @Test
    public void constructorRejectsNullMap() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot construct a TrackingMap() with null");
        new TrackingMap<>(null);
    }

    @Test
    public void putAndGetWorkTogether() {
        trackingMap.put("date", 4);
        assertEquals(Integer.valueOf(4), trackingMap.get("date"));
        assertTrue(trackingMap.keysUsed().contains("date"));
    }

    @Test
    public void getMarksKeyAsAccessed() {
        Integer val = trackingMap.get("apple");
        assertEquals(Integer.valueOf(1), val);
        assertTrue(trackingMap.keysUsed().contains("apple"));
    }

    @Test
    public void containsKeyMarksKeyAsAccessed() {
        assertTrue(trackingMap.containsKey("banana"));
        assertTrue(trackingMap.keysUsed().contains("banana"));
    }

    @Test
    public void removeDeletesEntryAndReadKey() {
        // Access the key first so it appears in readKeys
        trackingMap.get("cherry");
        assertTrue(trackingMap.keysUsed().contains("cherry"));

        // Now remove it
        Integer removed = trackingMap.remove("cherry");
        assertEquals(Integer.valueOf(3), removed);
        assertFalse(trackingMap.keySet().contains("cherry"));
        assertFalse(trackingMap.keysUsed().contains("cherry"));
    }

    @Test
    public void sizeAndIsEmptyReflectMapState() {
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

        other.put("date", 4);
        assertFalse(trackingMap.equals(other));
    }

    @Test
    public void toStringMatchesWrappedMap() {
        assertEquals(backingMap.toString(), trackingMap.toString());
    }

    @Test
    public void clearEmptiesMapAndReadKeys() {
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
    public void valuesKeySetAndEntrySetAreProxiesToWrappedMap() {
        Collection<Integer> values = trackingMap.values();
        Set<String> keys = trackingMap.keySet();
        Set<Map.Entry<String,Integer>> entries = trackingMap.entrySet();

        assertTrue(values.containsAll(Arrays.asList(1,2,3)));
        assertTrue(keys.containsAll(Arrays.asList("apple","banana","cherry")));
        assertEquals(3, entries.size());
    }

    @Test
    public void putAllCopiesAllEntries() {
        Map<String, Integer> extra = new HashMap<>();
        extra.put("date", 4);
        extra.put("elderberry", 5);

        trackingMap.putAll(extra);
        assertEquals(Integer.valueOf(4), trackingMap.get("date"));
        assertEquals(Integer.valueOf(5), trackingMap.get("elderberry"));
        assertEquals(5, trackingMap.size());
    }

    @Test
    public void putAllRejectsNullArgument() {
        thrown.expect(NullPointerException.class);
        trackingMap.putAll(null);
    }

    @Test
    public void expungeUnusedRemovesNeverAccessedEntries() {
        // Access only "apple" and "banana"
        trackingMap.get("apple");
        trackingMap.containsKey("banana");
        // "cherry" has never been accessed

        trackingMap.expungeUnused();

        assertTrue(trackingMap.keySet().containsAll(Arrays.asList("apple","banana")));
        assertFalse(trackingMap.keySet().contains("cherry"));
        assertEquals(2, trackingMap.size());
    }

    @Test
    public void expungeUnusedWithNoAccessedKeysClearsMap() {
        // No key has been accessed yet
        trackingMap.expungeUnused();
        assertTrue(trackingMap.isEmpty());
    }

    @Test
    public void informAdditionalUsageWithCollectionKeepsThoseKeysDuringExpunge() {
        // No accesses yet, map has three entries
        Collection<String> extraUsed = Arrays.asList("cherry");
        trackingMap.informAdditionalUsage(extraUsed);

        trackingMap.expungeUnused();

        // "cherry" should survive because it was added to readKeys via informAdditionalUsage
        assertTrue(trackingMap.keySet().contains("cherry"));
        assertEquals(1, trackingMap.size());
    }

    @Test
    public void informAdditionalUsageWithTrackingMapMergesReadKeys() {
        // Create a second TrackingMap that accessed "banana"
        Map<String, Integer> otherBacking = new HashMap<>(backingMap);
        TrackingMap<String, Integer> other = new TrackingMap<>(otherBacking);
        other.get("banana"); // mark as used in the other map

        // Merge usage info
        trackingMap.informAdditionalUsage(other);
        trackingMap.expungeUnused();

        // "banana" should be retained even though we never accessed it in the primary map
        assertTrue(trackingMap.keySet().contains("banana"));
        assertEquals(1, trackingMap.size());
    }

    @Test
    public void keysUsedReturnsLiveSet() {
        trackingMap.get("apple");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("apple"));

        // Mutating the returned set should affect the internal state
        used.remove("apple");
        assertFalse(trackingMap.keysUsed().contains("apple"));
    }

    @Test
    public void getWrappedMapReturnsOriginalReference() {
        assertSame(backingMap, trackingMap.getWrappedMap());
    }

    // -------------------------------------------------------------------------
    //  Edge‑case / exceptional‑scenario tests
    // -------------------------------------------------------------------------

    @Test
    public void getWithNullKeyWorksAndTracksNull() {
        // HashMap allows null keys, so we expect a null value and a null entry in readKeys
        assertNull(trackingMap.get(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    @Test
    public void containsKeyWithNullKeyWorksAndTracksNull() {
        // No null key present, should return false but still track null
        assertFalse(trackingMap.containsKey(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    @Test
    public void putAllowsNullKeyAndValue() {
        trackingMap.put(null, 42);
        trackingMap.put("nullValue", null);
        assertEquals(Integer.valueOf(42), trackingMap.get(null));
        assertNull(trackingMap.get("nullValue"));
        assertTrue(trackingMap.keySet().containsAll(Arrays.asList(null, "nullValue")));
    }

    @Test
    public void removeNonExistingKeyReturnsNullAndDoesNotAffectReadKeys() {
        assertNull(trackingMap.remove("nonexistent"));
        // readKeys should not contain the nonexistent key
        assertFalse(trackingMap.keysUsed().contains("nonexistent"));
    }

    @Test
    public void informAdditionalUsageWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        trackingMap.informAdditionalUsage((Collection<String>) null);
    }

    @Test
    public void informAdditionalUsageWithEmptyCollectionDoesNotChangeState() {
        Set<String> before = new HashSet<>(trackingMap.keysUsed());
        trackingMap.informAdditionalUsage(Collections.emptyList());
        assertEquals(before, trackingMap.keysUsed());
    }

    @Test
    public void informAdditionalUsageWithNullTrackingMapThrowsNPE() {
        thrown.expect(NullPointerException.class);
        trackingMap.informAdditionalUsage((TrackingMap<String,Integer>) null);
    }

    @Test
    public void expungeUnusedAfterInformAdditionalUsageAndRemoveEntry() {
        // Add a key via usage info that does not exist in the map
        trackingMap.informAdditionalUsage(Collections.singleton("ghost"));
        // Now remove an existing key that is also in readKeys
        trackingMap.get("apple");
        trackingMap.remove("apple");

        trackingMap.expungeUnused();

        // "ghost" is not in the map, so map should be empty
        assertTrue(trackingMap.isEmpty());
    }
}
