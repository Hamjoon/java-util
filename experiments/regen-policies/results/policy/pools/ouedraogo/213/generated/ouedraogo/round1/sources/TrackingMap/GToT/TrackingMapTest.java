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
        trackingMap = new TrackingMap<>(baseMap);
    }

    /* ---------- Constructor ---------- */
    @Test
    public void testConstructorWrapsMap() {
        assertSame(baseMap, trackingMap.getWrappedMap());
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNull() {
        new TrackingMap<String, Integer>(null);
    }

    /* ---------- get ---------- */
    @Test
    public void testGetExistingKeyMarksUsage() {
        Integer val = trackingMap.get("apple");
        assertEquals(Integer.valueOf(1), val);
        assertTrue(trackingMap.keysUsed().contains("apple"));
    }

    @Test
    public void testGetMissingKeyReturnsNullAndMarksUsage() {
        assertNull(trackingMap.get("durian"));
        assertTrue(trackingMap.keysUsed().contains("durian"));
    }

    @Test
    public void testGetNullKey() {
        assertNull(trackingMap.get(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    /* ---------- put ---------- */
    @Test
    public void testPutAddsEntry() {
        Integer previous = trackingMap.put("date", 4);
        assertNull(previous);
        assertEquals(Integer.valueOf(4), trackingMap.getWrappedMap().get("date"));
        assertFalse(trackingMap.keysUsed().contains("date")); // put does not mark usage
    }

    @Test
    public void testPutReplacesExistingValue() {
        Integer previous = trackingMap.put("apple", 10);
        assertEquals(Integer.valueOf(1), previous);
        assertEquals(Integer.valueOf(10), trackingMap.getWrappedMap().get("apple"));
    }

    @Test
    public void testPutNullValue() {
        trackingMap.put("eggplant", null);
        assertTrue(trackingMap.getWrappedMap().containsKey("eggplant"));
        assertNull(trackingMap.getWrappedMap().get("eggplant"));
    }

    /* ---------- containsKey ---------- */
    @Test
    public void testContainsKeyTrueMarksUsage() {
        assertTrue(trackingMap.containsKey("banana"));
        assertTrue(trackingMap.keysUsed().contains("banana"));
    }

    @Test
    public void testContainsKeyFalseMarksUsage() {
        assertFalse(trackingMap.containsKey("fig"));
        assertTrue(trackingMap.keysUsed().contains("fig"));
    }

    @Test
    public void testContainsKeyNull() {
        assertFalse(trackingMap.containsKey(null));
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    /* ---------- putAll ---------- */
    @Test
    public void testPutAllCopiesAllEntries() {
        Map<String, Integer> other = new HashMap<>();
        other.put("grape", 7);
        other.put("honeydew", 8);
        trackingMap.putAll(other);
        assertEquals(Integer.valueOf(7), trackingMap.getWrappedMap().get("grape"));
        assertEquals(Integer.valueOf(8), trackingMap.getWrappedMap().get("honeydew"));
    }

    @Test(expected = NullPointerException.class)
    public void testPutAllRejectsNull() {
        trackingMap.putAll(null);
    }

    /* ---------- remove ---------- */
    @Test
    public void testRemoveExistingKey() {
        trackingMap.get("apple"); // mark usage first
        Integer removed = trackingMap.remove("apple");
        assertEquals(Integer.valueOf(1), removed);
        assertFalse(trackingMap.getWrappedMap().containsKey("apple"));
        assertFalse(trackingMap.keysUsed().contains("apple")); // should be removed from usage set
    }

    @Test
    public void testRemoveNonExistingKey() {
        assertNull(trackingMap.remove("kiwi"));
        // usage set should not contain the key (remove also tries to delete from readKeys)
        assertFalse(trackingMap.keysUsed().contains("kiwi"));
    }

    @Test
    public void testRemoveNullKey() {
        trackingMap.put(null, 99);
        trackingMap.get(null); // mark usage
        Integer removed = trackingMap.remove(null);
        assertEquals(Integer.valueOf(99), removed);
        assertFalse(trackingMap.keysUsed().contains(null));
    }

    /* ---------- size / isEmpty ---------- */
    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(3, trackingMap.size());
        assertFalse(trackingMap.isEmpty());

        trackingMap.clear();
        assertEquals(0, trackingMap.size());
        assertTrue(trackingMap.isEmpty());
    }

    /* ---------- equals / hashCode ---------- */
    @Test
    public void testEqualsAndHashCodeDelegate() {
        Map<String, Integer> other = new HashMap<>(baseMap);
        assertTrue(trackingMap.equals(other));
        assertEquals(baseMap.hashCode(), trackingMap.hashCode());
    }

    @Test
    public void testEqualsWithNonMap() {
        assertFalse(trackingMap.equals("not a map"));
    }

    /* ---------- toString ---------- */
    @Test
    public void testToStringMatchesWrappedMap() {
        assertEquals(baseMap.toString(), trackingMap.toString());
    }

    /* ---------- clear ---------- */
    @Test
    public void testClearEmptiesMapAndUsage() {
        trackingMap.get("apple");
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty());
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    /* ---------- containsValue ---------- */
    @Test
    public void testContainsValue() {
        assertTrue(trackingMap.containsValue(2));
        assertFalse(trackingMap.containsValue(99));
    }

    /* ---------- values / keySet / entrySet ---------- */
    @Test
    public void testCollectionViewsReflectWrappedMap() {
        Collection<Integer> values = trackingMap.values();
        assertTrue(values.containsAll(Arrays.asList(1,2,3)));
        assertEquals(3, values.size());

        Set<String> keys = trackingMap.keySet();
        assertTrue(keys.containsAll(Arrays.asList("apple","banana","cherry")));
        assertEquals(3, keys.size());

        Set<Map.Entry<String,Integer>> entries = trackingMap.entrySet();
        for (Map.Entry<String,Integer> e : entries) {
            assertEquals(baseMap.get(e.getKey()), e.getValue());
        }
        assertEquals(3, entries.size());
    }

    /* ---------- expungeUnused ---------- */
    @Test
    public void testExpungeUnusedRemovesNeverAccessed() {
        // Access only "apple"
        trackingMap.get("apple");
        trackingMap.expungeUnused();

        assertTrue(trackingMap.getWrappedMap().containsKey("apple"));
        assertFalse(trackingMap.getWrappedMap().containsKey("banana"));
        assertFalse(trackingMap.getWrappedMap().containsKey("cherry"));
    }

    @Test
    public void testExpungeUnusedWhenAllAccessed() {
        trackingMap.get("apple");
        trackingMap.containsKey("banana");
        trackingMap.get("cherry");
        trackingMap.expungeUnused();

        assertEquals(3, trackingMap.size());
    }

    @Test
    public void testExpungeUnusedWhenNoneAccessed() {
        trackingMap.expungeUnused();
        assertTrue(trackingMap.isEmpty());
    }

    /* ---------- informAdditionalUsage(Collection) ---------- */
    @Test
    public void testInformAdditionalUsageCollectionAddsKeys() {
        List<String> extra = Arrays.asList("banana", "durian", null);
        trackingMap.informAdditionalUsage(extra);
        assertTrue(trackingMap.keysUsed().containsAll(extra));
    }

    @Test(expected = NullPointerException.class)
    public void testInformAdditionalUsageCollectionRejectsNull() {
        trackingMap.informAdditionalUsage(null);
    }

    /* ---------- informAdditionalUsage(TrackingMap) ---------- */
    @Test
    public void testInformAdditionalUsageTrackingMapMergesUsage() {
        TrackingMap<String,Integer> other = new TrackingMap<>(new HashMap<>());
        other.get("apple");
        other.get("fig");
        trackingMap.informAdditionalUsage(other);
        assertTrue(trackingMap.keysUsed().containsAll(Arrays.asList("apple","fig")));
    }

    @Test(expected = NullPointerException.class)
    public void testInformAdditionalUsageTrackingMapRejectsNull() {
        trackingMap.informAdditionalUsage((TrackingMap<String,Integer>)null);
    }

    /* ---------- keysUsed ---------- */
    @Test
    public void testKeysUsedReflectsAccesses() {
        trackingMap.get("apple");
        trackingMap.containsKey("banana");
        Set<String> used = trackingMap.keysUsed();
        assertEquals(new HashSet<>(Arrays.asList("apple","banana")), used);
    }

    /* ---------- getWrappedMap ---------- */
    @Test
    public void testGetWrappedMapReturnsSameInstance() {
        assertSame(baseMap, trackingMap.getWrappedMap());
    }

    /* ---------- setWrappedMap ---------- */
    @Test
    public void testSetWrappedMapReplacesInternalMapAndClearsUsage() {
        // Mark some usage first
        trackingMap.get("apple");
        Map<String,Integer> newMap = new HashMap<>();
        newMap.put("x", 100);
        newMap.put("y", 200);
        trackingMap.setWrappedMap(newMap);

        assertEquals(newMap, trackingMap.getWrappedMap());
        assertEquals(2, trackingMap.size());
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    @Test
    public void testSetWrappedMapRejectsNull() {
        try {
            trackingMap.setWrappedMap(null);
            fail("Expected an exception when setting a null wrapped map");
        } catch (RuntimeException e) {
            // The actual library uses Convention.throwIfNull which throws IllegalArgumentException
            // or NullPointerException – we just assert that *some* RuntimeException is thrown.
            assertTrue(e instanceof IllegalArgumentException ||
                       e instanceof NullPointerException);
        }
    }
}
