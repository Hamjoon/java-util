package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactCIHashMap}.
 *
 * The tests concentrate on the behaviour that distinguishes this class from its
 * superclass {@link CompactMap}:
 * <ul>
 *   <li>Case‑insensitive handling of keys</li>
 *   <li>Correct delegation to the underlying {@code CaseInsensitiveMap}</li>
 *   <li>Standard {@link Map} contract (size, put, get, remove, contains…)</li>
 *   <li>Copy‑constructor and interaction with {@code putAll}</li>
 *   <li>Immutable view methods (keySet, values, entrySet) behave as expected</li>
 * </ul>
 *
 * No external mocking frameworks are used – all objects are real instances
 * created via the public constructors supplied in the source code.
 */
public class CompactCIHashMapTest {

    private CompactCIHashMap<String, String> map;

    @Before
    public void setUp() {
        map = new CompactCIHashMap<>();
    }

    /** Helper to assert that the map contains exactly the supplied entries (ignoring order). */
    private void assertMapContents(Map<String, String> expected) {
        assertEquals("Size mismatch", expected.size(), map.size());
        for (Map.Entry<String, String> e : expected.entrySet()) {
            assertTrue("Missing key: " + e.getKey(), map.containsKey(e.getKey()));
            assertEquals("Value mismatch for key: " + e.getKey(),
                    e.getValue(), map.get(e.getKey()));
        }
    }

    @Test
    public void testPutAndGet_CaseInsensitive() {
        map.put("KeyOne", "value1");
        // Same key with different case should overwrite the previous entry
        map.put("keyone", "value2");

        assertEquals("Map should contain exactly one entry after case‑insensitive overwrite",
                1, map.size());
        assertEquals("value2", map.get("KEYONE"));
        assertTrue(map.containsKey("kEyOnE"));
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put("Alpha", "A");
        map.put("Bravo", "B");

        assertTrue(map.containsKey("ALPHA"));
        assertTrue(map.containsKey("bravo"));
        assertFalse(map.containsKey("charlie"));

        assertTrue(map.containsValue("A"));
        assertTrue(map.containsValue("B"));
        assertFalse(map.containsValue("C"));
    }

    @Test
    public void testRemove_CaseInsensitive() {
        map.put("First", "1");
        map.put("Second", "2");

        String removed = map.remove("FIRST");
        assertEquals("1", removed);
        assertFalse(map.containsKey("first"));
        assertEquals(1, map.size());

        // Removing a non‑existent key should return null
        assertNull(map.remove("nonexistent"));
    }

    @Test
    public void testClearAndIsEmpty() {
        map.put("X", "x");
        map.put("Y", "y");
        assertFalse(map.isEmpty());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testPutAll_CaseInsensitiveMerge() {
        Map<String, String> other = new HashMap<>();
        other.put("One", "1");
        other.put("TWO", "2");
        other.put("three", "3");

        map.putAll(other);
        assertMapContents(other);

        // Add another map with overlapping keys differing only by case
        Map<String, String> overlap = new HashMap<>();
        overlap.put("one", "ONE");
        overlap.put("Four", "4");

        map.putAll(overlap);
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("One", "ONE");   // overwritten by "one"
        expected.put("TWO", "2");
        expected.put("three", "3");
        expected.put("Four", "4");

        assertMapContents(expected);
    }

    @Test
    public void testCopyConstructorCreatesIndependentMap() {
        map.put("A", "a");
        map.put("B", "b");

        CompactCIHashMap<String, String> copy = new CompactCIHashMap<>(map);
        assertEquals(map, copy);
        assertNotSame(map, copy);

        // Mutate original – copy must stay unchanged
        map.put("A", "changed");
        assertEquals("a", copy.get("A"));
        assertEquals("changed", map.get("A"));
    }

    @Test
    public void testKeySetValuesEntrySetViewsReflectChanges() {
        map.put("One", "1");
        map.put("Two", "2");

        Set<String> keys = map.keySet();
        Collection<String> values = map.values();
        Set<Map.Entry<String, String>> entries = map.entrySet();

        // Verify initial contents
        assertTrue(keys.containsAll(Arrays.asList("One", "Two")));
        assertTrue(values.containsAll(Arrays.asList("1", "2")));
        assertEquals(2, entries.size());

        // Remove via keySet view
        assertTrue(keys.remove("ONE")); // case‑insensitive removal
        assertFalse(map.containsKey("One"));
        assertEquals(1, map.size());

        // Add via map and verify views update
        map.put("Three", "3");
        assertTrue(keys.contains("three"));
        assertTrue(values.contains("3"));
        assertTrue(entries.stream().anyMatch(e -> e.getKey().equalsIgnoreCase("three")
                && e.getValue().equals("3")));
    }

    @Test
    public void testMinusAndPlusUtilityMethods() {
        map.put("A", "1");
        map.put("B", "2");

        // minus should return a new map without the specified key (case‑insensitive)
        Map<String, String> minus = map.minus("a");
        assertFalse(minus.containsKey("A"));
        assertTrue(minus.containsKey("B"));
        assertEquals(1, minus.size());

        // plus should return a new map with the supplied entry added
        Map<String, String> plus = map.plus(new AbstractMap.SimpleEntry<>("C", "3"));
        assertTrue(plus.containsKey("C"));
        assertEquals("3", plus.get("c"));
        assertEquals(3, plus.size());

        // Original map must remain untouched
        assertEquals(2, map.size());
        assertTrue(map.containsKey("A"));
        assertTrue(map.containsKey("B"));
    }

    @Test
    public void testEqualsAndHashCodeConsistency() {
        CompactCIHashMap<String, String> m1 = new CompactCIHashMap<>();
        CompactCIHashMap<String, String> m2 = new CompactCIHashMap<>();

        m1.put("Key", "Value");
        m2.put("kEy", "Value"); // same key different case

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());

        // Different value should break equality
        m2.put("Key", "Other");
        assertNotEquals(m1, m2);
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("Alpha", "A");
        map.put("Beta", "B");

        String str = map.toString();
        assertTrue(str.contains("Alpha"));
        assertTrue(str.contains("Beta"));
        assertTrue(str.contains("A"));
        assertTrue(str.contains("B"));
    }
}
