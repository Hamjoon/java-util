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
        backingMap.put("apple", 1);
        backingMap.put("banana", 2);
        backingMap.put("cherry", 3);
        trackingMap = new TrackingMap<>(backingMap);
    }

    /* ---------------------------------------------------------------------- */
    /*  1. Constructor                                                            */
    /* ---------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructor_nullMap_throwsException() {
        new TrackingMap<>(null);
    }

    @Test
    public void testConstructor_validMap_initialState() {
        assertTrue(trackingMap.getWrappedMap() == backingMap);
        assertTrue(trackingMap.keysUsed().isEmpty());
        assertEquals(3, trackingMap.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  2. get(Object)                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testGet_existingKey_returnsValueAndMarksKey() {
        Integer value = trackingMap.get("apple");
        assertEquals(Integer.valueOf(1), value);
        assertTrue(trackingMap.keysUsed().contains("apple"));
    }

    @Test
    public void testGet_nonExistingKey_returnsNullAndMarksKey() {
        assertNull(trackingMap.get("durian"));
        assertTrue(trackingMap.keysUsed().contains("durian"));
    }

    @Test
    public void testGet_nullKey_behaviour() {
        // HashMap permits null keys; ensure TrackingMap tracks it as well
        backingMap.put(null, 99);
        assertEquals(Integer.valueOf(99), trackingMap.get(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  3. put(K,V)                                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testPut_newKey_returnsNullAndAddsEntry() {
        assertNull(trackingMap.put("date", 4));
        assertEquals(Integer.valueOf(4), trackingMap.get("date"));
        assertEquals(4, trackingMap.size());
    }

    @Test
    public void testPut_existingKey_returnsOldValue() {
        Integer old = trackingMap.put("banana", 20);
        assertEquals(Integer.valueOf(2), old);
        assertEquals(Integer.valueOf(20), trackingMap.get("banana"));
    }

    @Test(expected = NullPointerException.class)
    public void testPut_nullKey_throwsNPE() {
        // HashMap throws NPE for null key when map implementation does not allow it;
        // using HashMap (allows null) so this test verifies that TrackingMap does not
        // interfere – we intentionally use a map that disallows null to provoke NPE.
        Map<String, Integer> noNullMap = new Hashtable<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(noNullMap);
        tm.put(null, 1); // should throw NPE from underlying map
    }

    /* ---------------------------------------------------------------------- */
    /*  4. containsKey(Object)                                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testContainsKey_existingKey_trueAndMarks() {
        assertTrue(trackingMap.containsKey("banana"));
        assertTrue(trackingMap.keysUsed().contains("banana"));
    }

    @Test
    public void testContainsKey_missingKey_falseAndMarks() {
        assertFalse(trackingMap.containsKey("fig"));
        assertTrue(trackingMap.keysUsed().contains("fig"));
    }

    @Test
    public void testContainsKey_nullKey() {
        backingMap.put(null, 0);
        assertTrue(trackingMap.containsKey(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  5. putAll(Map)                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testPutAll_validMap_addsAllEntries() {
        Map<String, Integer> extra = new HashMap<>();
        extra.put("elder", 5);
        extra.put("fig", 6);
        trackingMap.putAll(extra);
        assertEquals(Integer.valueOf(5), trackingMap.get("elder"));
        assertEquals(Integer.valueOf(6), trackingMap.get("fig"));
        assertEquals(5, trackingMap.size());
    }

    @Test(expected = NullPointerException.class)
    public void testPutAll_nullMap_throwsException() {
        trackingMap.putAll(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  6. remove(Object)                                                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testRemove_existingKey_removesEntryAndKeyFromReadSet() {
        // first mark the key as read
        trackingMap.get("apple");
        assertTrue(trackingMap.keysUsed().contains("apple"));
        // now remove
        Integer removed = trackingMap.remove("apple");
        assertEquals(Integer.valueOf(1), removed);
        assertFalse(trackingMap.containsKey("apple"));
        assertFalse(trackingMap.keysUsed().contains("apple"));
    }

    @Test
    public void testRemove_missingKey_returnsNull() {
        assertNull(trackingMap.remove("grape"));
    }

    @Test
    public void testRemove_nullKey() {
        backingMap.put(null, 42);
        trackingMap.get(null); // mark it
        assertTrue(trackingMap.keysUsed().contains(null));
        Integer removed = trackingMap.remove(null);
        assertEquals(Integer.valueOf(42), removed);
        assertFalse(trackingMap.keysUsed().contains(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  7. size()                                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testSize_reflectsUnderlyingMap() {
        assertEquals(3, trackingMap.size());
        trackingMap.put("date", 4);
        assertEquals(4, trackingMap.size());
        trackingMap.remove("banana");
        assertEquals(3, trackingMap.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  8. isEmpty()                                                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testIsEmpty_behaviour() {
        assertFalse(trackingMap.isEmpty());
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /*  9. equals(Object)                                                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEquals_sameContents_true() {
        Map<String, Integer> other = new HashMap<>(backingMap);
        assertTrue(trackingMap.equals(other));
        assertTrue(trackingMap.equals(trackingMap));
    }

    @Test
    public void testEquals_differentContents_false() {
        Map<String, Integer> other = new HashMap<>();
        other.put("apple", 1);
        assertFalse(trackingMap.equals(other));
    }

    @Test
    public void testEquals_nonMapObject_false() {
        assertFalse(trackingMap.equals("not a map"));
    }

    /* ---------------------------------------------------------------------- */
    /* 10. hashCode()                                                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testHashCode_matchesUnderlyingMap() {
        assertEquals(backingMap.hashCode(), trackingMap.hashCode());
    }

    /* ---------------------------------------------------------------------- */
    /* 11. toString()                                                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToString_matchesUnderlyingMap() {
        assertEquals(backingMap.toString(), trackingMap.toString());
    }

    /* ---------------------------------------------------------------------- */
    /* 12. clear()                                                                */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testClear_emptiesMapAndReadSet() {
        trackingMap.get("apple");
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty());
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 13. containsValue(Object)                                                  */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testContainsValue_existingValue_true() {
        assertTrue(trackingMap.containsValue(2));
    }

    @Test
    public void testContainsValue_missingValue_false() {
        assertFalse(trackingMap.containsValue(99));
    }

    /* ---------------------------------------------------------------------- */
    /* 14. values()                                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testValues_reflectsUnderlyingValues() {
        Collection<Integer> values = trackingMap.values();
        assertTrue(values.containsAll(Arrays.asList(1, 2, 3)));
        assertEquals(3, values.size());
    }

    /* ---------------------------------------------------------------------- */
    /* 15. keySet()                                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testKeySet_reflectsUnderlyingKeys() {
        Set<String> keys = trackingMap.keySet();
        assertTrue(keys.containsAll(Arrays.asList("apple", "banana", "cherry")));
        assertEquals(3, keys.size());
    }

    /* ---------------------------------------------------------------------- */
    /* 16. entrySet()                                                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEntrySet_containsAllEntries() {
        Set<Map.Entry<String, Integer>> entries = trackingMap.entrySet();
        Map<String, Integer> expected = new HashMap<>(backingMap);
        for (Map.Entry<String, Integer> e : entries) {
            assertEquals(expected.get(e.getKey()), e.getValue());
        }
        assertEquals(3, entries.size());
    }

    /* ---------------------------------------------------------------------- */
    /* 17. expungeUnused()                                                        */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testExpungeUnused_removesUnaccessedEntries() {
        // Access only "apple" and "banana"
        trackingMap.get("apple");
        trackingMap.containsKey("banana");
        // "cherry" has not been accessed
        trackingMap.expungeUnused();
        assertTrue(trackingMap.containsKey("apple"));
        assertTrue(trackingMap.containsKey("banana"));
        assertFalse(trackingMap.containsKey("cherry"));
        assertEquals(2, trackingMap.size());
    }

    @Test
    public void testExpungeUnused_allAccessed_nothingRemoved() {
        trackingMap.get("apple");
        trackingMap.get("banana");
        trackingMap.get("cherry");
        trackingMap.expungeUnused();
        assertEquals(3, trackingMap.size());
    }

    @Test
    public void testExpungeUnused_noAccesses_allRemoved() {
        trackingMap.expungeUnused();
        assertTrue(trackingMap.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 18. informAdditionalUsage(Collection<K>)                                   */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testInformAdditionalUsage_collectionAddsKeys() {
        Collection<String> extra = Arrays.asList("fig", "grape");
        trackingMap.informAdditionalUsage(extra);
        assertTrue(trackingMap.keysUsed().containsAll(extra));
    }

    @Test
    public void testInformAdditionalUsage_collectionWithNull() {
        Collection<String> extra = Collections.singletonList(null);
        trackingMap.informAdditionalUsage(extra);
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    /* ---------------------------------------------------------------------- */
    /* 19. informAdditionalUsage(TrackingMap<K,V>)                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testInformAdditionalUsage_trackingMapMergesReadKeys() {
        TrackingMap<String, Integer> other = new TrackingMap<>(new HashMap<>());
        other.get("apple");   // marks apple
        other.get("date");    // marks date (not in this map)
        trackingMap.informAdditionalUsage(other);
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("apple"));
        assertTrue(used.contains("date"));
    }

    /* ---------------------------------------------------------------------- */
    /* 20. keysUsed()                                                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testKeysUsed_returnsLiveSet() {
        trackingMap.get("apple");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("apple"));
        // modify via returned set
        used.add("banana");
        assertTrue(trackingMap.keysUsed().contains("banana"));
    }

    /* ---------------------------------------------------------------------- */
    /* 21. getWrappedMap()                                                        */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testGetWrappedMap_returnsOriginalMapReference() {
        Map<String, Integer> wrapped = trackingMap.getWrappedMap();
        assertTrue(wrapped == backingMap);
        wrapped.put("kiwi", 7);
        assertEquals(Integer.valueOf(7), trackingMap.get("kiwi"));
    }
}
