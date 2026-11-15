package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * JUnit 4 test suite for {@link TrackingMap}.
 */
public class TrackingMapTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNull() {
        new TrackingMap<>(null);
    }

    @Test
    public void testGetAddsKeyToReadSet() {
        Map<String, Integer> backing = new HashMap<>();
        backing.put("one", 1);
        backing.put("two", 2);
        TrackingMap<String, Integer> tm = new TrackingMap<>(backing);

        // key present
        assertEquals(Integer.valueOf(1), tm.get("one"));
        assertTrue(tm.keysUsed().contains("one"));

        // key absent – should still be recorded as read and return null
        assertNull(tm.get("missing"));
        assertTrue(tm.keysUsed().contains("missing"));
    }

    @Test
    public void testContainsKeyAddsKeyToReadSet() {
        Map<String, String> backing = new HashMap<>();
        backing.put("a", "A");
        TrackingMap<String, String> tm = new TrackingMap<>(backing);

        assertTrue(tm.containsKey("a"));
        assertTrue(tm.keysUsed().contains("a"));

        // non‑existent key
        assertFalse(tm.containsKey("b"));
        assertTrue(tm.keysUsed().contains("b"));
    }

    @Test
    public void testRemoveCleansReadSet() {
        Map<Integer, String> backing = new HashMap<>();
        backing.put(1, "one");
        backing.put(2, "two");
        TrackingMap<Integer, String> tm = new TrackingMap<>(backing);

        // read both keys first
        tm.get(1);
        tm.get(2);
        assertEquals(2, tm.keysUsed().size());

        // now remove key 1 – it should be removed from the read‑set
        assertEquals("one", tm.remove(1));
        assertFalse(tm.keysUsed().contains(1));
        assertTrue(tm.keysUsed().contains(2));

        // removing a key that was never read should not affect the set
        tm.remove(3);
        assertEquals(1, tm.keysUsed().size());
    }

    @Test
    public void testExpungeUnusedKeepsOnlyReadEntries() {
        Map<String, Integer> backing = new HashMap<>();
        backing.put("keep", 10);
        backing.put("discard", 20);
        backing.put("alsoKeep", 30);
        TrackingMap<String, Integer> tm = new TrackingMap<>(backing);

        // Access only "keep" and "alsoKeep"
        tm.get("keep");
        tm.containsKey("alsoKeep");

        // Before expunge, map size is 3
        assertEquals(3, tm.size());

        // Expunge unused entries
        tm.expungeUnused();

        // Only the accessed entries should remain
        assertEquals(2, tm.size());
        assertTrue(tm.getWrappedMap().containsKey("keep"));
        assertTrue(tm.getWrappedMap().containsKey("alsoKeep"));
        assertFalse(tm.getWrappedMap().containsKey("discard"));
    }

    @Test
    public void testInformAdditionalUsageCollection() {
        Map<String, String> backing = new HashMap<>();
        backing.put("present", "yes");
        TrackingMap<String, String> tm = new TrackingMap<>(backing);

        // No reads yet
        assertTrue(tm.keysUsed().isEmpty());

        // Add an extra key that does NOT exist in the backing map
        Collection<String> extra = new HashSet<>();
        extra.add("ghost");
        tm.informAdditionalUsage(extra);

        // The extra key should now be reported as used
        assertTrue(tm.keysUsed().contains("ghost"));
        assertEquals(1, tm.keysUsed().size());

        // Expunge should NOT remove the existing entry because "present" was never read
        // but the map should stay unchanged (retainAll will keep only keys that are in read set)
        tm.expungeUnused();
        assertTrue(tm.getWrappedMap().isEmpty()); // because nothing was read, only "ghost" is in read set, which is not a map key
    }

    @Test
    public void testInformAdditionalUsageTrackingMap() {
        Map<Integer, String> mapA = new HashMap<>();
        mapA.put(1, "one");
        mapA.put(2, "two");
        TrackingMap<Integer, String> tmA = new TrackingMap<>(mapA);
        tmA.get(1); // read key 1

        Map<Integer, String> mapB = new HashMap<>();
        mapB.put(3, "three");
        TrackingMap<Integer, String> tmB = new TrackingMap<>(mapB);
        tmB.get(3); // read key 3

        // Merge usage from tmB into tmA
        tmA.informAdditionalUsage(tmB);

        // tmA should now consider both 1 and 3 as used
        Set<Integer> used = tmA.keysUsed();
        assertTrue(used.contains(1));
        assertTrue(used.contains(3));
        assertEquals(2, used.size());

        // Expunge unused on tmA – only key 1 exists in its backing map, so it stays
        tmA.expungeUnused();
        assertTrue(tmA.getWrappedMap().containsKey(1));
        assertFalse(tmA.getWrappedMap().containsKey(2));
    }

    @Test
    public void testClearEmptiesBothStructures() {
        Map<String, String> backing = new HashMap<>();
        backing.put("x", "X");
        TrackingMap<String, String> tm = new TrackingMap<>(backing);

        tm.get("x");
        assertFalse(tm.keysUsed().isEmpty());
        assertFalse(tm.getWrappedMap().isEmpty());

        tm.clear();

        assertTrue(tm.keysUsed().isEmpty());
        assertTrue(tm.getWrappedMap().isEmpty());
    }

    @Test
    public void testEqualsAndHashCodeDelegateToWrappedMap() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        TrackingMap<String, Integer> tm = new TrackingMap<>(map1);

        // Equal to another map with same entries
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);
        assertTrue(tm.equals(map2));
        assertEquals(map2.hashCode(), tm.hashCode());

        // Not equal to a map with different content
        map2.put("c", 3);
        assertFalse(tm.equals(map2));

        // Not equal to a non‑map object
        assertFalse(tm.equals("not a map"));
    }

    @Test
    public void testToStringMatchesWrappedMap() {
        Map<String, String> backing = new HashMap<>();
        backing.put("k", "v");
        TrackingMap<String, String> tm = new TrackingMap<>(backing);
        assertEquals(backing.toString(), tm.toString());
    }

    @Test
    public void testSizeIsEmptyDelegate() {
        Map<String, String> backing = new HashMap<>();
        TrackingMap<String, String> tm = new TrackingMap<>(backing);
        assertTrue(tm.isEmpty());
        assertEquals(0, tm.size());

        backing.put("a", "A");
        assertFalse(tm.isEmpty());
        assertEquals(1, tm.size());
    }

    @Test
    public void testContainsValueDelegates() {
        Map<String, Integer> backing = new HashMap<>();
        backing.put("x", 10);
        TrackingMap<String, Integer> tm = new TrackingMap<>(backing);
        assertTrue(tm.containsValue(10));
        assertFalse(tm.containsValue(20));
    }

    @Test
    public void testValuesKeySetEntrySetDelegate() {
        Map<String, Integer> backing = new HashMap<>();
        backing.put("a", 1);
        backing.put("b", 2);
        TrackingMap<String, Integer> tm = new TrackingMap<>(backing);

        Collection<Integer> values = tm.values();
        assertTrue(values.containsAll(backing.values()));

        Set<String> keys = tm.keySet();
        assertTrue(keys.containsAll(backing.keySet()));

        Set<Map.Entry<String, Integer>> entries = tm.entrySet();
        assertEquals(backing.entrySet(), entries);
    }

    @Test
    public void testGetWrappedMapReturnsOriginalReference() {
        Map<String, String> backing = new HashMap<>();
        backing.put("hello", "world");
        TrackingMap<String, String> tm = new TrackingMap<>(backing);
        assertSame(backing, tm.getWrappedMap());
    }
}
