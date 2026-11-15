package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * JUnit 4 test suite for {@link CompactCILinkedMap}.
 *
 * The tests focus on the case‑insensitive behaviour, retention of the original
 * key case, ordering guarantees once the internal delegate switches to a
 * {@link java.util.LinkedHashMap}, and the basic {@link Map} contract inherited
 * from {@link CompactMap}.
 *
 * The test class is placed in the same package as the class under test so that
 * protected members (e.g. {@code compactSize()}) are accessible.
 */
public class CompactCILinkedMapTest {

    /** Verify that string keys are handled case‑insensitively while the original
     *  case is retained in the key set. */
    @Test
    public void testCaseInsensitivePutAndGet() {
        CompactCILinkedMap<String, Integer> map = new CompactCILinkedMap<>();

        // first insertion
        map.put("Hello", 1);
        assertEquals(Integer.valueOf(1), map.get("hello"));
        assertEquals(Integer.valueOf(1), map.get("HELLO"));
        assertTrue(map.containsKey("HeLLo"));
        assertEquals(1, map.size());

        // the key set must contain the original case
        Set<String> keys = map.keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.contains("Hello"));
        assertFalse(keys.contains("hello"));

        // second insertion with a different case – should replace the existing entry
        map.put("hello", 2);
        assertEquals(Integer.valueOf(2), map.get("HELLO"));
        assertEquals(1, map.size());

        // the retained key should now be the most recent case
        keys = map.keySet();
        assertTrue(keys.contains("hello"));
        assertFalse(keys.contains("Hello"));
    }

    /** Non‑String keys must behave exactly like a normal {@link Map}. */
    @Test
    public void testNonStringKeyBehaviour() {
        CompactCILinkedMap<Integer, String> map = new CompactCILinkedMap<>();

        map.put(10, "ten");
        map.put(20, "twenty");

        assertEquals("ten", map.get(10));
        assertEquals("twenty", map.get(20));
        assertTrue(map.containsKey(10));
        assertFalse(map.containsKey("10")); // different type – not case‑insensitive
        assertEquals(2, map.size());
    }

    /** When the number of entries exceeds {@code compactSize()}, the internal
     *  delegate switches to a {@link LinkedHashMap} and preserves insertion order. */
    @Test
    public void testInsertionOrderAfterSwitchToLinkedMap() {
        CompactCILinkedMap<String, Integer> map = new CompactCILinkedMap<>();

        int threshold = map.compactSize() + 5; // ensure we go beyond the compact limit
        for (int i = 0; i < threshold; i++) {
            map.put("k" + i, i);
        }

        // Verify size
        assertEquals(threshold, map.size());

        // Verify that iteration order matches insertion order
        Iterator<String> it = map.keySet().iterator();
        for (int i = 0; i < threshold; i++) {
            assertTrue(it.hasNext());
            assertEquals("k" + i, it.next());
        }
        assertFalse(it.hasNext());
    }

    /** Removing a key using a different case must succeed and the key set
     *  must no longer contain the original entry. */
    @Test
    public void testRemoveWithDifferentCase() {
        CompactCILinkedMap<String, String> map = new CompactCILinkedMap<>();

        map.put("MixedCase", "value");
        assertEquals("value", map.get("mixedcase"));
        assertTrue(map.containsKey("MIXEDCASE"));

        // Remove using a different case
        String removed = map.remove("MIXEDcase");
        assertEquals("value", removed);
        assertFalse(map.containsKey("mixedcase"));
        assertTrue(map.isEmpty());
    }

    /** Verify that {@code putAll}, {@code clear}, {@code isEmpty} and {@code containsValue}
     *  work as expected. */
    @Test
    public void testPutAllClearAndContainsValue() {
        CompactCILinkedMap<String, Integer> map = new CompactCILinkedMap<>();

        CompactCILinkedMap<String, Integer> other = new CompactCILinkedMap<>();
        other.put("One", 1);
        other.put("Two", 2);
        other.put("Three", 3);

        map.putAll(other);
        assertEquals(3, map.size());
        assertTrue(map.containsValue(2));
        assertFalse(map.containsValue(4));

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /** The {@code minus} operation must return a new map without the specified key,
     *  leaving the original map unchanged. */
    @Test
    public void testMinusCreatesNewMapWithoutKey() {
        CompactCILinkedMap<String, String> original = new CompactCILinkedMap<>();
        original.put("Alpha", "A");
        original.put("Beta", "B");
        original.put("Gamma", "G");

        Map<String, String> withoutBeta = original.minus("beta"); // case‑insensitive removal

        // Original map stays intact
        assertEquals(3, original.size());
        assertTrue(original.containsKey("Alpha"));
        assertTrue(original.containsKey("Beta"));
        assertTrue(original.containsKey("Gamma"));

        // Resulting map does not contain the removed key (any case)
        assertEquals(2, withoutBeta.size());
        assertFalse(withoutBeta.containsKey("beta"));
        assertFalse(withoutBeta.containsKey("BETA"));
        assertTrue(withoutBeta.containsKey("Alpha"));
        assertTrue(withoutBeta.containsKey("Gamma"));
    }

    /** The {@code plus} operation must return a new map that contains all entries
     *  of the original map plus the supplied key (with a {@code null} value). */
    @Test
    public void testPlusCreatesNewMapWithAdditionalKey() {
        CompactCILinkedMap<String, Integer> original = new CompactCILinkedMap<>();
        original.put("One", 1);
        original.put("Two", 2);

        Map<String, Integer> extended = original.plus("Three");

        // Original map unchanged
        assertEquals(2, original.size());
        assertFalse(original.containsKey("Three"));

        // New map contains the extra key with a null value
        assertEquals(3, extended.size());
        assertTrue(extended.containsKey("Three"));
        assertNull(extended.get("Three"));
        assertEquals(Integer.valueOf(1), extended.get("One"));
        assertEquals(Integer.valueOf(2), extended.get("Two"));
    }

    /** Two maps that contain the same logical entries (ignoring key case) must be
     *  equal and have identical hash codes. */
    @Test
    public void testEqualsAndHashCodeWithDifferentKeyCases() {
        CompactCILinkedMap<String, String> mapA = new CompactCILinkedMap<>();
        mapA.put("KeyOne", "value1");
        mapA.put("KEYTWO", "value2");

        CompactCILinkedMap<String, String> mapB = new CompactCILinkedMap<>();
        mapB.put("keyone", "value1");
        mapB.put("keytwo", "value2");

        assertEquals(mapA, mapB);
        assertEquals(mapA.hashCode(), mapB.hashCode());

        // Changing the case of a key in one map should still keep them equal
        mapB.put("KeYOnE", "value1");
        assertEquals(mapA, mapB);
    }

    /** {@code toString} must contain all entries in a readable form. */
    @Test
    public void testToStringContainsAllEntries() {
        CompactCILinkedMap<String, Integer> map = new CompactCILinkedMap<>();
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);

        String str = map.toString();

        assertTrue(str.contains("A=1"));
        assertTrue(str.contains("B=2"));
        assertTrue(str.contains("C=3"));
        // The string representation should start with a '{' and end with a '}'
        assertTrue(str.startsWith("{"));
        assertTrue(str.endsWith("}"));
    }

    /** Verify that {@code null} keys are handled according to the underlying
     *  {@link CompactMap} contract (they should be allowed). */
    @Test
    public void testNullKeyBehaviour() {
        CompactCILinkedMap<String, String> map = new CompactCILinkedMap<>();

        map.put(null, "nullValue");
        assertEquals("nullValue", map.get(null));
        assertTrue(map.containsKey(null));
        assertEquals(1, map.size());

        // Removing the null key
        assertEquals("nullValue", map.remove(null));
        assertFalse(map.containsKey(null));
        assertTrue(map.isEmpty());
    }
}
