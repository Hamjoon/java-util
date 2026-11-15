package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class TrackingMapTest {

    private Map<String, Integer> baseMap;
    private TrackingMap<String, Integer> trackingMap;

    @Before
    public void setUp() {
        baseMap = new HashMap<>();
        baseMap.put("apple", 1);
        baseMap.put("banana", 2);
        baseMap.put("cherry", 3);
        trackingMap = new TrackingMap<>(new HashMap<>(baseMap));
    }

    /* ----------------------------------------------------------------------
     *  List of public methods (signatures) extracted from TrackingMap
     * ---------------------------------------------------------------------- */
    // public TrackingMap(Map<K,V> map)
    // public V get(Object key)
    // public V put(K key, V value)
    // public boolean containsKey(Object key)
    // public void putAll(Map<? extends K, ? extends V> m)
    // public V remove(Object key)
    // public int size()
    // public boolean isEmpty()
    // public boolean equals(Object other)
    // public int hashCode()
    // public String toString()
    // public void clear()
    // public boolean containsValue(Object value)
    // public Collection<V> values()
    // public Set<K> keySet()
    // public Set<Entry<K,V>> entrySet()
    // public void expungeUnused()
    // public void informAdditionalUsage(Collection<K> additional)
    // public void informAdditionalUsage(TrackingMap<K,V> additional)
    // public Set<K> keysUsed()
    // public Map<K,V> getWrappedMap()
    // public void setWrappedMap(Map<K,V> map)

    /* ----------------------------------------------------------------------
     *  Basic functionality tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testConstructorWithValidMap() {
        Map<String, Integer> m = new HashMap<>();
        m.put("x", 10);
        TrackingMap<String, Integer> tm = new TrackingMap<>(m);
        assertEquals(1, tm.size());
        assertTrue(tm.containsKey("x"));
        assertEquals(Integer.valueOf(10), tm.get("x"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullMap() {
        new TrackingMap<String, Integer>(null);
    }

    @Test
    public void testGetAndKeysUsedTracking() {
        assertEquals(Integer.valueOf(1), trackingMap.get("apple"));
        assertTrue(trackingMap.keysUsed().contains("apple"));
        // accessing a missing key should still be recorded
        assertNull(trackingMap.get("unknown"));
        assertTrue(trackingMap.keysUsed().contains("unknown"));
    }

    @Test
    public void testPut() {
        Integer previous = trackingMap.put("date", 4);
        assertNull(previous);
        assertEquals(Integer.valueOf(4), trackingMap.get("date"));
        assertEquals(4, trackingMap.size());
    }

    @Test
    public void testContainsKeyAndTracking() {
        assertTrue(trackingMap.containsKey("banana"));
        assertTrue(trackingMap.keysUsed().contains("banana"));
        assertFalse(trackingMap.containsKey("nonexistent"));
        assertTrue(trackingMap.keysUsed().contains("nonexistent"));
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

    @Test(expected = NullPointerException.class)
    public void testPutAllWithNull() {
        trackingMap.putAll(null);
    }

    @Test
    public void testRemoveAndReadKeysCleanup() {
        trackingMap.get("apple"); // mark as read
        assertTrue(trackingMap.keysUsed().contains("apple"));
        Integer removed = trackingMap.remove("apple");
        assertEquals(Integer.valueOf(1), removed);
        assertFalse(trackingMap.keysUsed().contains("apple"));
        assertFalse(trackingMap.containsKey("apple"));
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
        Map<String, Integer> other = new HashMap<>(baseMap);
        assertTrue(trackingMap.equals(other));
        assertEquals(other.hashCode(), trackingMap.hashCode());

        // different map
        other.put("new", 99);
        assertFalse(trackingMap.equals(other));
    }

    @Test
    public void testToStringMatchesWrappedMap() {
        assertEquals(trackingMap.getWrappedMap().toString(), trackingMap.toString());
    }

    @Test
    public void testClearResetsMapAndKeysUsed() {
        trackingMap.get("apple");
        trackingMap.get("banana");
        assertFalse(trackingMap.isEmpty());
        assertFalse(trackingMap.keysUsed().isEmpty());

        trackingMap.clear();

        assertTrue(trackingMap.isEmpty());
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    @Test
    public void testContainsValue() {
        assertTrue(trackingMap.containsValue(2));
        assertFalse(trackingMap.containsValue(99));
    }

    @Test
    public void testValuesKeySetEntrySetViews() {
        Collection<Integer> values = trackingMap.values();
        assertTrue(values.containsAll(Arrays.asList(1, 2, 3)));

        Set<String> keys = trackingMap.keySet();
        assertTrue(keys.containsAll(Arrays.asList("apple", "banana", "cherry")));

        Set<Map.Entry<String, Integer>> entries = trackingMap.entrySet();
        for (Map.Entry<String, Integer> e : entries) {
            assertEquals(baseMap.get(e.getKey()), e.getValue());
        }
    }

    @Test
    public void testExpungeUnusedRemovesUnaccessedEntries() {
        // Access only "apple" and "banana"
        trackingMap.get("apple");
        trackingMap.containsKey("banana");

        // Before expunge, all entries present
        assertEquals(3, trackingMap.size());

        trackingMap.expungeUnused();

        // "cherry" should be gone
        assertEquals(2, trackingMap.size());
        assertTrue(trackingMap.containsKey("apple"));
        assertTrue(trackingMap.containsKey("banana"));
        assertFalse(trackingMap.containsKey("cherry"));
    }

    @Test
    public void testExpungeUnusedWhenNoAccesses() {
        // No get/containsKey calls
        trackingMap.expungeUnused();
        assertTrue(trackingMap.isEmpty());
    }

    @Test
    public void testInformAdditionalUsageWithCollection() {
        Collection<String> extraKeys = Arrays.asList("date", "elderberry");
        trackingMap.informAdditionalUsage(extraKeys);
        assertTrue(trackingMap.keysUsed().containsAll(extraKeys));
        // these keys are not in the underlying map yet; expungeUnused should keep map unchanged
        trackingMap.expungeUnused();
        assertEquals(3, trackingMap.size()); // original size unchanged
    }

    @Test
    public void testInformAdditionalUsageWithAnotherTrackingMap() {
        TrackingMap<String, Integer> other = new TrackingMap<>(new HashMap<>());
        other.get("apple"); // mark as used in other
        other.get("fig");   // key not present in original map

        trackingMap.informAdditionalUsage(other);
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("apple"));
        assertTrue(used.contains("fig"));
    }

    @Test
    public void testKeysUsedReturnsLiveSet() {
        trackingMap.get("apple");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("apple"));
        used.add("newKey");
        // the internal readKeys set should reflect the addition
        assertTrue(trackingMap.keysUsed().contains("newKey"));
    }

    @Test
    public void testGetWrappedMapReturnsOriginalReference() {
        Map<String, Integer> wrapped = trackingMap.getWrappedMap();
        assertSame(wrapped, trackingMap.getWrappedMap());
        // modifying the returned map should affect the TrackingMap
        wrapped.put("grape", 7);
        assertTrue(trackingMap.containsKey("grape"));
        assertEquals(Integer.valueOf(7), trackingMap.get("grape"));
    }

    @Test
    public void testSetWrappedMapReplacesContent() {
        Map<String, Integer> newMap = new HashMap<>();
        newMap.put("kiwi", 8);
        newMap.put("lemon", 9);

        trackingMap.setWrappedMap(newMap);

        // old data cleared
        assertFalse(trackingMap.containsKey("apple"));
        assertEquals(2, trackingMap.size());
        assertTrue(trackingMap.containsKey("kiwi"));
        assertTrue(trackingMap.containsKey("lemon"));
        // readKeys should be empty after setWrappedMap (clear is called inside)
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetWrappedMapWithNullThrows() {
        trackingMap.setWrappedMap(null);
    }

    /* ----------------------------------------------------------------------
     *  Edge case tests for null keys/values and map behaviours
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetWithNullKey() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put(null, 100);
        assertEquals(Integer.valueOf(100), map.get(null));
        assertTrue(map.keysUsed().contains(null));
    }

    @Test
    public void testContainsKeyWithNullKey() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put(null, 200);
        assertTrue(map.containsKey(null));
        assertTrue(map.keysUsed().contains(null));
    }

    @Test
    public void testPutWithNullKeyAndValue() {
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertNull(map.get(null));
        // put does not record usage, so keysUsed should still be empty
        assertTrue(map.keysUsed().isEmpty());
    }

    @Test
    public void testRemoveNonExistingKey() {
        assertNull(trackingMap.remove("nonexistent"));
        // removal of a non‑existing key should not affect keysUsed
        assertFalse(trackingMap.keysUsed().contains("nonexistent"));
    }

    @Test
    public void testEqualsWithNonMapObject() {
        assertFalse(trackingMap.equals("someString"));
    }

    @Test
    public void testHashCodeConsistencyAfterModifications() {
        int originalHash = trackingMap.hashCode();
        trackingMap.put("newKey", 10);
        assertNotEquals(originalHash, trackingMap.hashCode());
    }

    @Test
    public void testToStringAfterModifications() {
        String before = trackingMap.toString();
        trackingMap.put("newKey", 10);
        String after = trackingMap.toString();
        assertNotEquals(before, after);
    }
}
