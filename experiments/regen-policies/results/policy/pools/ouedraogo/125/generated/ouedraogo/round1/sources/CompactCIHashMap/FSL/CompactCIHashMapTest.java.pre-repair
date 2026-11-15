package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactCIHashMap}.
 *
 * The tests focus on the observable behaviour of the class:
 * <ul>
 *   <li>default construction</li>
 *   <li>construction from another map (copy constructor)</li>
 *   <li>case‑insensitive key handling (put, get, containsKey, remove)</li>
 *   <li>standard {@link Map} contract methods (size, isEmpty, clear, equals, hashCode)</li>
 *   <li>the {@code minus} and {@code plus} convenience methods</li>
 * </ul>
 *
 * Only the public API of {@code CompactCIHashMap} and its superclass
 * {@code CompactMap} is used – no reflection or internal‑class access.
 */
public class CompactCIHashMapTest {

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        assertTrue("Map should be empty after default construction", map.isEmpty());
        assertEquals("Size must be 0", 0, map.size());
    }

    @Test
    public void testCopyConstructorCopiesEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("One", 1);
        source.put("Two", 2);

        CompactCIHashMap<String, Integer> copy = new CompactCIHashMap<>(source);
        assertEquals("Copy must contain the same number of entries", source.size(), copy.size());
        assertTrue("Copy must contain key \"One\" (any case)", copy.containsKey("ONE"));
        assertEquals("Value for key \"Two\" must be 2", Integer.valueOf(2), copy.get("two"));
    }

    @Test
    public void testCaseInsensitivePutAndGet() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("Key", "value1");
        // Different case should retrieve the same entry
        assertEquals("value1", map.get("key"));
        assertEquals("value1", map.get("KEY"));
        // Overwrite using different case – should replace the same entry
        map.put("kEy", "value2");
        assertEquals("value2", map.get("KEY"));
        assertEquals(1, map.size());
    }

    @Test
    public void testContainsKeyIsCaseInsensitive() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("Hello", "World");
        assertTrue(map.containsKey("hello"));
        assertTrue(map.containsKey("HELLO"));
        assertFalse(map.containsKey("Goodbye"));
    }

    @Test
    public void testRemoveIsCaseInsensitive() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("Alpha", "A");
        map.put("Beta", "B");
        assertEquals("A", map.remove("ALPHA"));
        assertNull("Key should no longer exist after removal", map.get("alpha"));
        assertEquals(1, map.size());
        // Removing a non‑existent key returns null
        assertNull(map.remove("gamma"));
    }

    @Test
    public void testClearEmptiesMap() {
        CompactCIHashMap<String, Integer> map = new CompactCIHashMap<>();
        map.put("One", 1);
        map.put("Two", 2);
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue("Map must be empty after clear()", map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testEqualsAndHashCodeContract() {
        CompactCIHashMap<String, String> map1 = new CompactCIHashMap<>();
        CompactCIHashMap<String, String> map2 = new CompactCIHashMap<>();

        map1.put("A", "1");
        map1.put("B", "2");

        map2.put("a", "1"); // different case, same logical entry
        map2.put("b", "2");

        assertEquals("Maps with same logical entries must be equal", map1, map2);
        assertEquals("Equal maps must have same hashCode", map1.hashCode(), map2.hashCode());

        map2.put("C", "3");
        assertNotEquals("Adding an extra entry must break equality", map1, map2);
    }

    @Test
    public void testMinusCreatesNewMapWithoutSpecifiedKey() {
        CompactCIHashMap<String, String> original = new CompactCIHashMap<>();
        original.put("X", "x");
        original.put("Y", "y");
        original.put("Z", "z");

        Map<String, String> reduced = original.minus("y"); // case‑insensitive removal
        assertFalse("Resulting map must not contain the removed key", reduced.containsKey("Y"));
        assertEquals("Resulting map must still contain the other keys", 2, reduced.size());
        assertTrue("Resulting map must contain key X", reduced.containsKey("x"));
        assertTrue("Resulting map must contain key Z", reduced.containsKey("z"));
        // Original map must stay unchanged
        assertEquals("Original map size must stay unchanged", 3, original.size());
        assertTrue("Original map must still contain the removed key", original.containsKey("Y"));
    }

    @Test
    public void testPlusCreatesNewMapWithAdditionalEntry() {
        CompactCIHashMap<String, Integer> base = new CompactCIHashMap<>();
        base.put("one", 1);
        base.put("two", 2);

        Map<String, Integer> extended = base.plus("THREE");
        // The plus method adds the supplied key with a null value (as per CompactMap contract)
        assertTrue("Extended map must contain the new key (any case)", extended.containsKey("three"));
        assertNull("New key should map to null", extended.get("THREE"));
        assertEquals("Extended map size must be base size + 1", 3, extended.size());

        // Verify original map is untouched
        assertEquals("Base map size must stay unchanged", 2, base.size());
        assertFalse("Base map must not contain the new key", base.containsKey("THREE"));
    }

    @Test
    public void testKeySetAndValuesReflectCaseInsensitiveBehaviour() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("First", "1");
        map.put("second", "2");
        map.put("THIRD", "3");

        // keySet should contain the keys exactly as they were first inserted
        assertTrue(map.keySet().contains("First"));
        assertTrue(map.keySet().contains("second"));
        assertTrue(map.keySet().contains("THIRD"));
        assertEquals(3, map.keySet().size());

        // values collection must contain all values
        assertTrue(map.values().containsAll(java.util.Arrays.asList("1", "2", "3")));
        assertEquals(3, map.values().size());
    }

    @Test
    public void testEntrySetIterationOrderIsUnspecifiedButConsistentWithinMap() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");

        // Collect entries into a secondary map and verify equality
        Map<String, String> collected = new HashMap<>();
        for (Map.Entry<String, String> e : map.entrySet()) {
            collected.put(e.getKey(), e.getValue());
        }
        assertEquals("Collected entries must equal the original map", map, collected);
    }

    @Test
    public void testPutAllCopiesAllEntriesCaseInsensitively() {
        CompactCIHashMap<String, String> target = new CompactCIHashMap<>();
        Map<String, String> source = new HashMap<>();
        source.put("Alpha", "A");
        source.put("BETA", "B");

        target.putAll(source);
        assertEquals("A", target.get("alpha"));
        assertEquals("B", target.get("beta"));
        assertEquals(2, target.size());
    }

    @Test
    public void testComputeKeyHashCodeDelegatesToCaseInsensitiveLogic() {
        // The method is protected; we can invoke it via a subclass created just for testing.
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>() {
            @Override
            public int computeKeyHashCode(Object key) {
                return super.computeKeyHashCode(key);
            }
        };
        int hashUpper = map.computeKeyHashCode("KEY");
        int hashLower = map.computeKeyHashCode("key");
        assertEquals("Hash codes for keys differing only by case must be equal", hashUpper, hashLower);
    }

    @Test
    public void testIsCaseInsensitiveFlag() {
        CompactCIHashMap<String, String> map = new CompactCIHashMap<>();
        // The flag is protected; expose via an anonymous subclass.
        assertTrue("CompactCIHashMap must report case‑insensitive behaviour",
                new CompactCIHashMap<String, String>() {
                    @Override
                    public boolean isCaseInsensitive() {
                        return super.isCaseInsensitive();
                    }
                }.isCaseInsensitive());
    }
}
