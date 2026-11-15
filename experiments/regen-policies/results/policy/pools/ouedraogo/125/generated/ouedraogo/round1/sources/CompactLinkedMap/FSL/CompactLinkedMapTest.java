package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

/**
 * JUnit‑4 test suite that exercises every public and protected behaviour of {@link CompactLinkedMap}.
 * The tests focus on:
 * <ul>
 *   <li>Construction (empty and from another map)</li>
 *   <li>Standard {@link Map} operations (put, get, remove, contains*, clear, putAll)</li>
 *   <li>Insertion‑order semantics (keySet, values, entrySet)</li>
 *   <li>Utility methods defined in {@link CompactMap} (minus, plus)</li>
 *   <li>Protected hooks specific to {@link CompactLinkedMap} (getNewMap, useCopyIterator)</li>
 * </ul>
 *
 * All assertions are performed with JUnit‑4 static imports.
 */
public class CompactLinkedMapTest {

    /** Verify that a freshly constructed map is empty and reports the correct size. */
    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>();
        assertTrue("Map should be empty after default construction", map.isEmpty());
        assertEquals("Size should be 0", 0, map.size());
        assertNull("Getting a non‑existent key must return null", map.get("any"));
    }

    /** Verify that the copy‑constructor copies all entries from the supplied map. */
    @Test
    public void testConstructorWithExistingMapCopiesEntries() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("a", "alpha");
        source.put("b", "bravo");
        source.put("c", "charlie");

        CompactLinkedMap<String, String> map = new CompactLinkedMap<>(source);
        assertEquals("Map should contain the same number of entries as source", source.size(), map.size());
        for (Map.Entry<String, String> e : source.entrySet()) {
            assertTrue("Map must contain key: " + e.getKey(), map.containsKey(e.getKey()));
            assertEquals("Value for key " + e.getKey() + " must match", e.getValue(), map.get(e.getKey()));
        }
    }

    /** Basic put / get / remove workflow. */
    @Test
    public void testPutGetRemoveOperations() {
        CompactLinkedMap<Integer, String> map = new CompactLinkedMap<>();
        assertNull("Absent key should return null", map.get(1));

        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        assertEquals("Size after three puts", 3, map.size());
        assertEquals("Value for key 2", "two", map.get(2));
        assertTrue("Map must contain key 3", map.containsKey(3));
        assertTrue("Map must contain value 'one'", map.containsValue("one"));

        String removed = map.remove(2);
        assertEquals("Removed value must be the previous one", "two", removed);
        assertFalse("Key 2 must no longer be present", map.containsKey(2));
        assertEquals("Size after removal", 2, map.size());
    }

    /** putAll and clear should behave like a normal Map. */
    @Test
    public void testPutAllAndClear() {
        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>();
        Map<String, Integer> extra = new LinkedHashMap<>();
        extra.put("x", 100);
        extra.put("y", 200);
        extra.put("z", 300);

        map.putAll(extra);
        assertEquals("Map size after putAll", extra.size(), map.size());
        assertTrue("Map must contain key 'y'", map.containsKey("y"));
        assertEquals("Value for 'z' must match", Integer.valueOf(300), map.get("z"));

        map.clear();
        assertTrue("Map must be empty after clear", map.isEmpty());
        assertEquals("Size must be 0 after clear", 0, map.size());
    }

    /** Insertion order must be preserved for keySet, values and entrySet. */
    @Test
    public void testInsertionOrderPreserved() {
        CompactLinkedMap<String, String> map = new CompactLinkedMap<>();
        map.put("first", "1");
        map.put("second", "2");
        map.put("third", "3");

        // keySet order
        Set<String> keys = map.keySet();
        String[] expectedKeys = {"first", "second", "third"};
        assertArrayEquals("keySet order must follow insertion order", expectedKeys, keys.toArray());

        // values order
        Collection<String> values = map.values();
        String[] expectedValues = {"1", "2", "3"};
        assertArrayEquals("values order must follow insertion order", expectedValues, values.toArray());

        // entrySet order
        Set<Map.Entry<String, String>> entries = map.entrySet();
        int i = 0;
        for (Map.Entry<String, String> e : entries) {
            assertEquals("Entry key order", expectedKeys[i], e.getKey());
            assertEquals("Entry value order", expectedValues[i], e.getValue());
            i++;
        }
        assertEquals("All entries must have been iterated", expectedKeys.length, i);
    }

    /** The minus method must return a new map without the specified key, leaving the original untouched. */
    @Test
    public void testMinusCreatesNewMapWithoutKey() {
        CompactLinkedMap<String, String> original = new CompactLinkedMap<>();
        original.put("keep", "yes");
        original.put("remove", "no");
        original.put("alsoKeep", "maybe");

        Map<String, String> result = original.minus("remove");

        // original must stay unchanged
        assertTrue("Original must still contain the removed key", original.containsKey("remove"));
        assertEquals("Original size unchanged", 3, original.size());

        // result must not contain the key
        assertFalse("Result must not contain the removed key", result.containsKey("remove"));
        assertEquals("Result size must be original size minus one", 2, result.size());
        assertTrue("Result must still contain other keys", result.containsKey("keep"));
        assertTrue("Result must still contain other keys", result.containsKey("alsoKeep"));
    }

    /** The plus method must merge two maps, giving precedence to entries from the right‑hand map. */
    @Test
    public void testPlusMergesMaps() {
        CompactLinkedMap<String, Integer> left = new CompactLinkedMap<>();
        left.put("a", 1);
        left.put("b", 2);

        CompactLinkedMap<String, Integer> right = new CompactLinkedMap<>();
        right.put("b", 20);   // overrides left
        right.put("c", 30);

        Map<String, Integer> merged = left.plus(right);

        assertEquals("Merged map must have three entries", 3, merged.size());
        assertEquals("Value for 'a' must come from left", Integer.valueOf(1), merged.get("a"));
        assertEquals("Value for 'b' must be overridden by right", Integer.valueOf(20), merged.get("b"));
        assertEquals("Value for 'c' must come from right", Integer.valueOf(30), merged.get("c"));
    }

    /** Verify that the protected getNewMap() returns a LinkedHashMap instance. */
    @Test
    public void testGetNewMapReturnsLinkedHashMap() throws Exception {
        CompactLinkedMap<String, String> map = new CompactLinkedMap<>();
        Method getNewMap = CompactLinkedMap.class.getDeclaredMethod("getNewMap");
        getNewMap.setAccessible(true);
        Object newMap = getNewMap.invoke(map);
        assertTrue("getNewMap must return a LinkedHashMap", newMap instanceof LinkedHashMap);
    }

    /** Verify that the protected useCopyIterator() returns false as documented. */
    @Test
    public void testUseCopyIteratorIsFalse() throws Exception {
        CompactLinkedMap<String, String> map = new CompactLinkedMap<>();
        Method useCopyIterator = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        useCopyIterator.setAccessible(true);
        Object result = useCopyIterator.invoke(map);
        assertTrue("useCopyIterator must return a Boolean", result instanceof Boolean);
        assertFalse("useCopyIterator must be false for CompactLinkedMap", (Boolean) result);
    }

    /** Ensure that the map works correctly when many entries are added (beyond the default compact size). */
    @Test
    public void testLargeNumberOfEntries() {
        CompactLinkedMap<Integer, String> map = new CompactLinkedMap<>();
        int total = 200; // well above the default compactSize (70)
        for (int i = 0; i < total; i++) {
            map.put(i, "val" + i);
        }
        assertEquals("Map must contain all inserted entries", total, map.size());

        // Spot‑check a few entries
        assertEquals("val0", map.get(0));
        assertEquals("val99", map.get(99));
        assertEquals("val199", map.get(199));
    }

    /** Verify that equals() and hashCode() behave consistently with a standard LinkedHashMap. */
    @Test
    public void testEqualsAndHashCodeAgainstLinkedHashMap() {
        CompactLinkedMap<String, String> compact = new CompactLinkedMap<>();
        compact.put("k1", "v1");
        compact.put("k2", "v2");

        Map<String, String> linked = new LinkedHashMap<>();
        linked.put("k1", "v1");
        linked.put("k2", "v2");

        assertTrue("CompactLinkedMap should be equal to an equivalent LinkedHashMap", compact.equals(linked));
        assertEquals("hashCode must match that of an equivalent LinkedHashMap", linked.hashCode(), compact.hashCode());
    }

    /** toString() should contain all key‑value pairs in insertion order. */
    @Test
    public void testToStringContainsEntriesInOrder() {
        CompactLinkedMap<String, Integer> map = new CompactLinkedMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        String s = map.toString();
        // The exact format is implementation dependent, but the string must contain the entries in order.
        int idxOne = s.indexOf("one=1");
        int idxTwo = s.indexOf("two=2");
        int idxThree = s.indexOf("three=3");

        assertTrue("toString must contain 'one=1'", idxOne >= 0);
        assertTrue("toString must contain 'two=2'", idxTwo >= 0);
        assertTrue("toString must contain 'three=3'", idxThree >= 0);
        assertTrue("Entries must appear in insertion order", idxOne < idxTwo && idxTwo < idxThree);
    }

    /** Test that the map correctly handles null keys and values (allowed by CompactMap). */
    @Test
    public void testNullKeyAndValueSupport() {
        CompactLinkedMap<String, String> map = new CompactLinkedMap<>();
        map.put(null, "nullKey");
        map.put("nullValue", null);

        assertTrue("Map must contain the null key", map.containsKey(null));
        assertEquals("Value for null key must match", "nullKey", map.get(null));

        assertTrue("Map must contain the key with null value", map.containsKey("nullValue"));
        assertNull("Value for key 'nullValue' must be null", map.get("nullValue"));
    }

    /** Verify that Collections.unmodifiableMap works with CompactLinkedMap (no mutation). */
    @Test(expected = UnsupportedOperationException.class)
    public void testUnmodifiableMapThrowsOnMutation() {
        CompactLinkedMap<String, String> map = new CompactLinkedMap<>();
        map.put("a", "A");
        Map<String, String> unmod = Collections.unmodifiableMap(map);
        unmod.put("b", "B"); // should throw
    }
}
