package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link TrackingMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>construction (including null argument)</li>
 *   <li>all {@link Map} operations delegated to the wrapped map</li>
 *   <li>read‑tracking behaviour of {@code get}, {@code containsKey},
 *       {@code remove}, {@code expungeUnused} and the inform methods</li>
 *   <li>equality, hashCode and toString delegations</li>
 *   <li>access to the wrapped map</li>
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

    /*** Constructor validation ***/
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRejectsNull() {
        new TrackingMap<String, Integer>(null);
    }

    /*** Basic Map delegation ***/
    @Test
    public void testPutAndGet() {
        assertEquals(Integer.valueOf(1), trackingMap.get("one"));
        assertEquals(Integer.valueOf(2), trackingMap.get("two"));

        // put a new entry
        trackingMap.put("four", 4);
        assertEquals(Integer.valueOf(4), trackingMap.get("four"));
        assertTrue(trackingMap.containsKey("four"));
        assertEquals(4, trackingMap.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        assertTrue(trackingMap.containsKey("three"));
        assertTrue(trackingMap.containsValue(3));
        assertFalse(trackingMap.containsKey("absent"));
        assertFalse(trackingMap.containsValue(99));
    }

    @Test
    public void testRemoveUpdatesReadKeys() {
        // Access a key so it is recorded as read
        trackingMap.get("one");
        assertTrue(trackingMap.keysUsed().contains("one"));

        // Remove the key – readKeys should no longer contain it
        Integer removed = trackingMap.remove("one");
        assertEquals(Integer.valueOf(1), removed);
        assertFalse(trackingMap.keysUsed().contains("one"));
        assertFalse(trackingMap.containsKey("one"));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, Integer> extra = new HashMap<>();
        extra.put("five", 5);
        extra.put("six", 6);
        trackingMap.putAll(extra);
        assertEquals(Integer.valueOf(5), trackingMap.get("five"));
        assertEquals(Integer.valueOf(6), trackingMap.get("six"));
        assertEquals(5, trackingMap.size());

        // clear should empty both internal map and read‑keys set
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty());
        assertTrue(trackingMap.keysUsed().isEmpty());
    }

    @Test
    public void testKeySetValuesEntrySet() {
        Set<String> keys = trackingMap.keySet();
        Collection<Integer> values = trackingMap.values();
        Set<Map.Entry<String, Integer>> entries = trackingMap.entrySet();

        assertEquals(new HashSet<>(Arrays.asList("one", "two", "three")), keys);
        assertTrue(values.containsAll(Arrays.asList(1, 2, 3)));
        assertEquals(3, entries.size());

        // Ensure the collections are live views of the backing map
        trackingMap.remove("two");
        assertFalse(keys.contains("two"));
        assertFalse(values.contains(2));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(3, trackingMap.size());
        assertFalse(trackingMap.isEmpty());

        trackingMap.clear();
        assertEquals(0, trackingMap.size());
        assertTrue(trackingMap.isEmpty());
    }

    /*** Equality, hashCode and toString ***/
    @Test
    public void testEqualsAndHashCode() {
        Map<String, Integer> otherMap = new HashMap<>(backingMap);
        assertTrue(trackingMap.equals(otherMap));
        assertEquals(trackingMap.hashCode(), otherMap.hashCode());

        otherMap.put("extra", 100);
        assertFalse(trackingMap.equals(otherMap));
    }

    @Test
    public void testToStringDelegates() {
        String expected = backingMap.toString();
        assertEquals(expected, trackingMap.toString());
    }

    /*** Access to the wrapped map ***/
    @Test
    public void testGetWrappedMapReturnsOriginal() {
        assertSame(backingMap, trackingMap.getWrappedMap());
    }

    /*** Read‑tracking behaviour ***/
    @Test
    public void testKeysUsedAfterGetAndContainsKey() {
        // No reads yet
        assertTrue(trackingMap.keysUsed().isEmpty());

        // get() records the key
        trackingMap.get("two");
        assertTrue(trackingMap.keysUsed().contains("two"));

        // containsKey() also records the key
        trackingMap.containsKey("three");
        assertTrue(trackingMap.keysUsed().containsAll(Arrays.asList("two", "three")));

        // accessing a missing key should still be recorded
        trackingMap.get("absent");
        assertTrue(trackingMap.keysUsed().contains("absent"));
    }

    @Test
    public void testExpungeUnusedRemovesUnaccessedEntries() {
        // Access only "one" and "two"
        trackingMap.get("one");
        trackingMap.containsKey("two");

        // Add a new entry that will not be accessed
        trackingMap.put("four", 4);
        assertEquals(Integer.valueOf(4), trackingMap.get("four")); // make sure it is present

        // Expunge – only accessed keys should remain
        trackingMap.expungeUnused();

        Set<String> remainingKeys = trackingMap.keySet();
        assertEquals(new HashSet<>(Arrays.asList("one", "two")), remainingKeys);
        assertFalse(remainingKeys.contains("three"));
        assertFalse(remainingKeys.contains("four"));
    }

    @Test
    public void testInformAdditionalUsageWithCollection() {
        // Record a key that does not exist in the map
        List<String> extraKeys = Arrays.asList("alpha", "beta");
        trackingMap.informAdditionalUsage(extraKeys);

        // keysUsed should now contain the extra keys
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.containsAll(extraKeys));
        // internal map should be unchanged
        assertEquals(3, trackingMap.size());
    }

    @Test
    public void testInformAdditionalUsageWithOtherTrackingMap() {
        // Create a second TrackingMap with its own read set
        Map<String, Integer> otherBacking = new HashMap<>();
        otherBacking.put("x", 10);
        otherBacking.put("y", 20);
        TrackingMap<String, Integer> other = new TrackingMap<>(otherBacking);
        other.get("x"); // record "x"
        other.containsKey("y"); // record "y"

        // Merge the usage information
        trackingMap.informAdditionalUsage(other);

        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.containsAll(Arrays.asList("x", "y")));
        // The merged keys may not be present in the original map – that's allowed
        assertEquals(3, trackingMap.size()); // original size unchanged
    }

    /*** Edge cases ***/
    @Test
    public void testNullKeyHandling() {
        // HashMap permits null keys; TrackingMap should propagate that behaviour
        trackingMap.put(null, 999);
        assertTrue(trackingMap.containsKey(null));
        assertEquals(Integer.valueOf(999), trackingMap.get(null));
        assertTrue(trackingMap.keysUsed().contains(null));

        // Remove null key – readKeys should no longer contain it
        trackingMap.remove(null);
        assertFalse(trackingMap.keysUsed().contains(null));
        assertFalse(trackingMap.containsKey(null));
    }
}
