package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * JUnit‑4 test suite for {@link CompactCIHashMap}.
 *
 * The tests focus on the behaviour that differentiates {@code CompactCIHashMap}
 * from its superclass {@code CompactMap}:
 *
 * <ul>
 *   <li>String keys are looked up case‑insensitively, while the original
 *       case of the key is retained in {@code keySet()}, {@code entrySet()},
 *       {@code toString()}, etc.</li>
 *   <li>Non‑String keys behave exactly like a normal {@code HashMap}.</li>
 *   <li>The {@code minus} and {@code plus} convenience methods honour the
 *       case‑insensitivity contract.</li>
 *   <li>Standard {@code Map} contract methods such as {@code equals},
 *       {@code hashCode}, {@code clear}, {@code isEmpty}, {@code size},
 *       {@code containsKey} and {@code containsValue} work correctly.</li>
 * </ul>
 *
 * The underlying {@code CompactMap} implementation is not exercised directly;
 * instead we verify the observable contract of {@code CompactCIHashMap}.
 */
public class CompactCIHashMapTest {

    /**
     * Verify that a String key can be retrieved using any case variation.
     */
    @Test
    public void testCaseInsensitivePutAndGet() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("FooBar", "value1");

        // Different capitalisations must resolve to the same entry
        assertEquals("value1", map.get("foobar"));
        assertEquals("value1", map.get("FOOBAR"));
        assertEquals("value1", map.get("FoObAr"));

        // containsKey must also be case‑insensitive
        assertTrue(map.containsKey("FOObar"));
        assertTrue(map.containsKey("foobar"));
    }

    /**
     * The original case of the key is retained in the keySet view.
     */
    @Test
    public void testOriginalKeyCaseIsRetained() {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        map.put("MyKey", 42);

        Set<String> keys = map.keySet();
        assertTrue("Key set must contain the original spelling", keys.contains("MyKey"));
        assertFalse("Key set must NOT contain a lower‑cased version", keys.contains("mykey"));
    }

    /**
     * Adding a second entry with the same logical (case‑insensitive) key
     * replaces the previous value and updates the stored key case.
     */
    @Test
    public void testOverwriteWithDifferentCase() {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        map.put("TestKey", 1);
        map.put("testkey", 2); // same logical key, different case

        // Size must remain 1 because the logical key is the same
        assertEquals(1, map.size());

        // The value must be the one from the last put()
        assertEquals(Integer.valueOf(2), map.get("TESTKEY"));

        // The retained key case should be the one from the most recent put()
        assertTrue(map.keySet().contains("testkey"));
        assertFalse(map.keySet().contains("TestKey"));
    }

    /**
     * Non‑String keys are treated normally (case‑sensitivity does not apply).
     */
    @Test
    public void testNonStringKeyBehaviour() {
        CompactCIHashMap<Object, String> map = new CompactCIHashMap<>();
        Integer intKey = 123;
        map.put(intKey, "int value");

        assertEquals("int value", map.get(123));
        assertTrue(map.containsKey(123));
        assertFalse(map.containsKey("123")); // different type, should not match
    }

    /**
     * The {@code minus} method returns a new map without the specified key,
     * respecting case‑insensitivity.
     */
    @Test
    public void testMinusRemovesKeyCaseInsensitively() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("Alpha", "A");
        map.put("Beta", "B");

        Map<String, String> result = map.minus("ALPHA"); // different case
        assertEquals(1, result.size());
        assertFalse(result.containsKey("Alpha"));
        assertTrue(result.containsKey("Beta"));
        assertEquals("B", result.get("beta"));
    }

    /**
     * The {@code plus} method returns a new map with the additional entry,
     * respecting case‑insensitivity.
     */
    @Test
    public void testPlusAddsKeyCaseInsensitively() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("One", "1");

        Map<String, String> result = map.plus("ONE"); // same logical key, different case
        // The new map must contain exactly one entry with the value from the original map
        assertEquals(1, result.size());
        assertTrue(result.containsKey("One"));
        assertEquals("1", result.get("one"));
    }

    /**
     * Standard {@code Map} contract methods work as expected.
     */
    @Test
    public void testStandardMapOperations() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("Key1", "V1");
        map.put("Key2", "V2");
        map.put("Key3", "V3");

        assertFalse(map.isEmpty());
        assertEquals(3, map.size());

        // remove by a different case
        assertEquals("V2", map.remove("kEy2"));
        assertEquals(2, map.size());
        assertFalse(map.containsKey("KEY2"));

        // clear
        map.clear();
        assertTrue(map.isEmpty());
    }

    /**
     * Two maps with the same logical entries are equal, regardless of the
     * case used when the keys were inserted.
     */
    @Test
    public void testEqualsAndHashCode() {
        CompactCIHashMap<String, Integer> mapA = new CompactCIHashMap<>();
        mapA.put("First", 1);
        mapA.put("Second", 2);

        CompactCIHashMap<String, Integer> mapB = new CompactCIHashMap<>();
        mapB.put("first", 1);   // different case
        mapB.put("SECOND", 2);  // different case

        assertEquals(mapA, mapB);
        assertEquals(mapA.hashCode(), mapB.hashCode());

        // Changing the value of one entry must break equality
        mapB.put("SECOND", 99);
        assertNotEquals(mapA, mapB);
    }

    /**
     * The {@code toString()} representation must contain the retained key case
     * and the associated values.
     */
    @Test
    public void testToStringContainsEntries() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("Alpha", "A");
        map.put("Beta", "B");

        String text = map.toString();

        // The string should contain both original keys and their values
        assertTrue(text.contains("Alpha"));
        assertTrue(text.contains("A"));
        assertTrue(text.contains("Beta"));
        assertTrue(text.contains("B"));
    }

    /**
     * Verify that the map behaves correctly when the number of entries exceeds
     * the internal {@code compactSize()} threshold.  The observable behaviour
     * (look‑ups, iteration, etc.) must remain unchanged.
     */
    @Test
    public void testBehaviourBeyondCompactSize() {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();

        // Populate more entries than the default compact size (the exact size is
        // implementation‑specific; we use a safe upper bound).
        int entries = 100;
        for (int i = 0; i < entries; i++) {
            map.put("Key" + i, i);
        }

        // Verify size and random look‑ups
        assertEquals(entries, map.size());
        assertEquals(Integer.valueOf(42), map.get("kEy42")); // mixed case lookup

        // Verify that the retained key case for a sample entry is the one from the last put()
        assertTrue(map.keySet().contains("Key42"));
        assertFalse(map.keySet().contains("key42"));
    }

    /**
     * Ensure that {@code values()} returns a collection reflecting the current
     * map contents, and that modifications to the underlying map are reflected
     * in the collection view.
     */
    @Test
    public void testValuesCollectionView() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("A", "alpha");
        map.put("B", "beta");

        Collection<String> values = map.values();
        assertTrue(values.contains("alpha"));
        assertTrue(values.contains("beta"));
        assertEquals(2, values.size());

        // Update an entry and verify the collection reflects the change
        map.put("a", "ALPHA"); // same logical key, different case/value
        assertTrue(values.contains("ALPHA"));
        assertFalse(values.contains("alpha"));
        assertEquals(2, values.size());
    }

    /**
     * Verify that {@code entrySet()} provides entries with the retained key case
     * and the correct values.
     */
    @Test
    public void testEntrySetView() {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        map.put("XyZ", 10);
        map.put("abc", 20);

        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        assertEquals(2, entries.size());

        boolean foundXyZ = false;
        boolean foundAbc = false;
        for (Map.Entry<String, Integer> e : entries) {
            if (e.getKey().equals("XyZ")) {
                foundXyZ = true;
                assertEquals(Integer.valueOf(10), e.getValue());
            } else if (e.getKey().equals("abc")) {
                foundAbc = true;
                assertEquals(Integer.valueOf(20), e.getValue());
            }
        }
        assertTrue(foundXyZ);
        assertTrue(foundAbc);
    }
}
