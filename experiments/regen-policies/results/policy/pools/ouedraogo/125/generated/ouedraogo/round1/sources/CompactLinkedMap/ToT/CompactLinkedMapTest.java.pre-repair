package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactLinkedMap}.
 *
 * <p>The tests focus on the public contract of {@code CompactLinkedMap},
 * which is inherited from {@link CompactMap}.  Because {@code CompactLinkedMap}
 * only overrides three protected methods, the behaviour exercised here is
 * the same as for a normal {@code CompactMap} but with insertion‑order
 * guarantees provided by the internal {@link java.util.LinkedHashMap}.</p>
 *
 * <p>Each test method validates a distinct aspect of the API:
 * <ul>
 *   <li>Construction (empty and from another map)</li>
 *   <li>Basic map operations (put, get, remove, clear, size, isEmpty)</li>
 *   <li>Ordering guarantees (keySet, values, entrySet iteration order)</li>
 *   <li>Special {@code CompactMap} operations ({@code plus}, {@code minus})</li>
 *   <li>Equality, hashCode and toString contracts</li>
 *   <li>Edge cases such as {@code null} keys/values and duplicate keys</li>
 * </ul>
 * </p>
 */
public class CompactLinkedMapTest {

    private CompactLinkedMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactLinkedMap<>();
    }

    /* -----------------------------------------------------------
     *  Construction tests
     * ----------------------------------------------------------- */

    @Test
    public void testEmptyConstructor() {
        assertTrue("New map must be empty", map.isEmpty());
        assertEquals("Size of new map must be 0", 0, map.size());
    }

    @Test
    public void testConstructorFromExistingMap() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        source.put("three", 3);

        CompactLinkedMap<String, Integer> copy = new CompactLinkedMap<>(source);
        assertEquals("Copy must have same size as source", source.size(), copy.size());
        assertEquals("Copy must contain all entries from source", source, copy);
        // verify insertion order is preserved
        assertEquals("Key order must be preserved", new ArrayList<>(source.keySet()), new ArrayList<>(copy.keySet()));
    }

    /* -----------------------------------------------------------
     *  Basic map operations
     * ----------------------------------------------------------- */

    @Test
    public void testPutAndGet() {
        assertNull("Put returns previous value (null for new key)", map.put("a", 10));
        assertEquals("Value must be retrievable after put", Integer.valueOf(10), map.get("a"));
        assertEquals("Size must be 1 after one insertion", 1, map.size());

        // replace existing key
        assertEquals("Put must return old value when key exists", Integer.valueOf(10), map.put("a", 20));
        assertEquals("New value must replace old one", Integer.valueOf(20), map.get("a"));
        assertEquals("Size must stay unchanged after replacement", 1, map.size());
    }

    @Test
    public void testRemove() {
        map.put("x", 100);
        map.put("y", 200);
        assertEquals("Remove must return associated value", Integer.valueOf(100), map.remove("x"));
        assertNull("Removed key must no longer be present", map.get("x"));
        assertFalse("Map must not contain removed key", map.containsKey("x"));
        assertEquals("Size must be reduced", 1, map.size());

        // removing non‑existent key returns null
        assertNull("Removing absent key must return null", map.remove("nonexistent"));
    }

    @Test
    public void testClearAndIsEmpty() {
        map.put("k1", 1);
        map.put("k2", 2);
        assertFalse("Map must not be empty before clear", map.isEmpty());
        map.clear();
        assertTrue("Map must be empty after clear", map.isEmpty());
        assertEquals("Size must be 0 after clear", 0, map.size());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put("alpha", 42);
        assertTrue("containsKey must return true for existing key", map.containsKey("alpha"));
        assertFalse("containsKey must return false for missing key", map.containsKey("beta"));
        assertTrue("containsValue must return true for existing value", map.containsValue(42));
        assertFalse("containsValue must return false for missing value", map.containsValue(99));
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);
        map.putAll(other);
        assertEquals("All entries must be present after putAll", other, map);
    }

    @Test
    public void testNullKeyAndValue() {
        // The underlying CompactMap permits null keys/values (behaviour mirrors java.util.HashMap)
        map.put(null, 123);
        map.put("nullValue", null);
        assertTrue("Map must contain null key", map.containsKey(null));
        assertTrue("Map must contain key with null value", map.containsKey("nullValue"));
        assertEquals("Value for null key must be correct", Integer.valueOf(123), map.get(null));
        assertNull("Value for key with null value must be null", map.get("nullValue"));
        assertEquals("Size must be 2 after inserting nulls", 2, map.size());
    }

    /* -----------------------------------------------------------
     *  Ordering guarantees
     * ----------------------------------------------------------- */

    @Test
    public void testKeySetInsertionOrder() {
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        List<String> expected = Arrays.asList("first", "second", "third");
        List<String> actual = new ArrayList<>(map.keySet());

        assertEquals("keySet iteration order must follow insertion order", expected, actual);
    }

    @Test
    public void testValuesInsertionOrder() {
        map.put("a", 10);
        map.put("b", 20);
        map.put("c", 30);

        List<Integer> expected = Arrays.asList(10, 20, 30);
        List<Integer> actual = new ArrayList<>(map.values());

        assertEquals("values iteration order must follow insertion order", expected, actual);
    }

    @Test
    public void testEntrySetInsertionOrder() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        List<Map.Entry<String, Integer>> expected = new ArrayList<>();
        expected.add(new AbstractMap.SimpleEntry<>("one", 1));
        expected.add(new AbstractMap.SimpleEntry<>("two", 2));
        expected.add(new AbstractMap.SimpleEntry<>("three", 3));

        List<Map.Entry<String, Integer>> actual = new ArrayList<>(map.entrySet());

        // Compare entry by entry (key and value)
        assertEquals("Entry set size must match", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            Map.Entry<String, Integer> e1 = expected.get(i);
            Map.Entry<String, Integer> e2 = actual.get(i);
            assertEquals("Entry key order must follow insertion order", e1.getKey(), e2.getKey());
            assertEquals("Entry value order must follow insertion order", e1.getValue(), e2.getValue());
        }
    }

    /* -----------------------------------------------------------
     *  CompactMap‑specific operations
     * ----------------------------------------------------------- */

    @Test
    public void testMinusCreatesNewMapWithoutKey() {
        map.put("keep", 1);
        map.put("removeMe", 2);
        map.put("alsoKeep", 3);

        Map<String, Integer> result = map.minus("removeMe");
        assertFalse("Result must not contain the removed key", result.containsKey("removeMe"));
        assertTrue("Result must still contain other keys", result.containsKey("keep"));
        assertTrue("Result must still contain other keys", result.containsKey("alsoKeep"));
        assertEquals("Result size must be original size minus one", 2, result.size());

        // Original map must stay unchanged
        assertTrue("Original map must still contain the removed key", map.containsKey("removeMe"));
        assertEquals("Original map size must be unchanged", 3, map.size());
    }

    @Test
    public void testPlusMergesTwoMaps() {
        map.put("a", 1);
        map.put("b", 2);

        Map<String, Integer> other = new HashMap<>();
        other.put("b", 20); // duplicate key – value from 'other' should win
        other.put("c", 3);

        Map<String, Integer> merged = map.plus(other);
        assertEquals("Merged map must contain three keys", 3, merged.size());
        assertEquals("Value for duplicate key must come from right-hand map", Integer.valueOf(20), merged.get("b"));
        assertEquals("Value for new key must be present", Integer.valueOf(3), merged.get("c"));
        assertEquals("Value for untouched key must be unchanged", Integer.valueOf(1), merged.get("a"));

        // Ensure original maps are untouched
        assertEquals("Original left map must retain original value for 'b'", Integer.valueOf(2), map.get("b"));
        assertNull("Original left map must not contain key 'c'", map.get("c"));
        assertEquals("Original right map must stay unchanged", Integer.valueOf(20), other.get("b"));
    }

    /* -----------------------------------------------------------
     *  Equality, hashCode and toString
     * ----------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        CompactLinkedMap<String, Integer> m1 = new CompactLinkedMap<>();
        CompactLinkedMap<String, Integer> m2 = new CompactLinkedMap<>();

        m1.put("x", 9);
        m1.put("y", 8);

        m2.put("x", 9);
        m2.put("y", 8);

        assertEquals("Maps with same entries must be equal", m1, m2);
        assertEquals("Equal maps must have same hashCode", m1.hashCode(), m2.hashCode());

        // Different order should still be equal because insertion order is part of map state
        CompactLinkedMap<String, Integer> m3 = new CompactLinkedMap<>();
        m3.put("y", 8);
        m3.put("x", 9);
        assertNotEquals("Different insertion order must make maps unequal", m1, m3);
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("one", 1);
        map.put("two", 2);
        String s = map.toString();
        assertTrue("toString should contain first key", s.contains("one"));
        assertTrue("toString should contain second key", s.contains("two"));
        assertTrue("toString should contain first value", s.contains("1"));
        assertTrue("toString should contain second value", s.contains("2"));
    }

    /* -----------------------------------------------------------
     *  Additional sanity checks (iterator behavior)
     * ----------------------------------------------------------- */

    @Test
    public void testIteratorRemoveIsSupported() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if ("b".equals(key)) {
                it.remove();
            }
        }
        assertFalse("Key 'b' must have been removed via iterator", map.containsKey("b"));
        assertEquals("Size must be reduced after iterator remove", 2, map.size());
    }
}
