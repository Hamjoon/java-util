package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

/**
 * JUnit‑4 test suite for {@link CompactLinkedMap}.
 *
 * The tests exercise:
 * <ul>
 *   <li>both constructors</li>
 *   <li>all {@link Map} operations inherited from {@link CompactMap}</li>
 *   <li>the special behaviour of {@link CompactLinkedMap} when the number of entries
 *       exceeds {@code compactSize()} – the underlying delegate should become a
 *       {@link LinkedHashMap} and therefore preserve insertion order.</li>
 * </ul>
 *
 * The test accesses the protected {@code compactSize()} method via reflection
 * (allowed because the test lives in the same package).  No mocking or subclassing
 * of library classes is performed.
 */
public class CompactLinkedMapTest {

    private CompactLinkedMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactLinkedMap<>();
    }

    @After
    public void tearDown() {
        map.clear();
    }

    /** Helper that invokes the protected {@code compactSize()} method via reflection. */
    private int getCompactSize(CompactLinkedMap<?, ?> m) {
        try {
            Method method = CompactMap.class.getDeclaredMethod("compactSize");
            method.setAccessible(true);
            return (int) method.invoke(m);
        } catch (Exception e) {
            throw new RuntimeException("Unable to obtain compactSize()", e);
        }
    }

    @Test
    public void testEmptyConstructor() {
        assertTrue("New map should be empty", map.isEmpty());
        assertEquals("Size of new map should be 0", 0, map.size());
    }

    @Test
    public void testConstructorFromExistingMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        CompactLinkedMap<String, Integer> fromMap = new CompactLinkedMap<>(source);
        assertEquals("Map should contain all entries from source", source, fromMap);
        assertEquals("Size should match source size", source.size(), fromMap.size());
    }

    @Test
    public void testPutAndGet() {
        assertNull("Getting a non‑existing key returns null", map.get("missing"));
        map.put("a", 10);
        assertEquals("Value retrieved must match the one put", Integer.valueOf(10), map.get("a"));
        // replace existing key
        Integer old = map.put("a", 20);
        assertEquals("Old value should be returned on replace", Integer.valueOf(10), old);
        assertEquals("New value must be stored", Integer.valueOf(20), map.get("a"));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(map.isEmpty());
        map.put("k1", 1);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
        map.put("k2", 2);
        assertEquals(2, map.size());
        map.remove("k1");
        assertEquals(1, map.size());
        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put("key", 100);
        assertTrue(map.containsKey("key"));
        assertFalse(map.containsKey("other"));
        assertTrue(map.containsValue(100));
        assertFalse(map.containsValue(200));
    }

    @Test
    public void testRemove() {
        map.put("x", 5);
        map.put("y", 6);
        Integer removed = map.remove("x");
        assertEquals("Removed value must be returned", Integer.valueOf(5), removed);
        assertFalse("Key should no longer be present", map.containsKey("x"));
        assertEquals("Size must decrease", 1, map.size());
        assertNull("Removing non‑existent key returns null", map.remove("z"));
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> other = new HashMap<>();
        other.put("p", 1);
        other.put("q", 2);
        map.put("a", 10);
        map.putAll(other);
        assertEquals("Map must contain all entries after putAll", 3, map.size());
        assertTrue(map.keySet().containsAll(other.keySet()));
        assertTrue(map.values().containsAll(other.values()));
    }

    @Test
    public void testClear() {
        map.put("one", 1);
        map.put("two", 2);
        map.clear();
        assertTrue("Map should be empty after clear", map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testKeySetValuesEntrySet() {
        map.put("k1", 10);
        map.put("k2", 20);
        Set<String> keys = map.keySet();
        Collection<Integer> values = map.values();
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        assertEquals(2, keys.size());
        assertTrue(keys.contains("k1"));
        assertTrue(keys.contains("k2"));

        assertEquals(2, values.size());
        assertTrue(values.contains(10));
        assertTrue(values.contains(20));

        assertEquals(2, entries.size());
        for (Map.Entry<String, Integer> e : entries) {
            if (e.getKey().equals("k1")) {
                assertEquals(Integer.valueOf(10), e.getValue());
            } else if (e.getKey().equals("k2")) {
                assertEquals(Integer.valueOf(20), e.getValue());
            } else {
                fail("Unexpected entry in entrySet: " + e);
            }
        }
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactLinkedMap<String, Integer> a = new CompactLinkedMap<>();
        CompactLinkedMap<String, Integer> b = new CompactLinkedMap<>();

        a.put("x", 1);
        a.put("y", 2);

        b.put("x", 1);
        b.put("y", 2);

        assertEquals("Maps with same entries must be equal", a, b);
        assertEquals("Equal maps must have same hashCode", a.hashCode(), b.hashCode());

        b.put("z", 3);
        assertNotEquals("Maps with different entries must not be equal", a, b);
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("alpha", 100);
        map.put("beta", 200);
        String s = map.toString();
        assertTrue("toString should contain key alpha", s.contains("alpha"));
        assertTrue("toString should contain value 100", s.contains("100"));
        assertTrue("toString should contain key beta", s.contains("beta"));
        assertTrue("toString should contain value 200", s.contains("200"));
    }

    @Test
    public void testMinusCreatesNewMapWithoutKey() {
        map.put("keep", 1);
        map.put("removeMe", 2);
        Map<String, Integer> result = map.minus("removeMe");
        // original map unchanged
        assertTrue(map.containsKey("removeMe"));
        // result does not contain the removed key
        assertFalse(result.containsKey("removeMe"));
        assertTrue(result.containsKey("keep"));
        assertEquals(1, result.size());
    }

    @Test
    public void testPlusMergesTwoMaps() {
        map.put("a", 1);
        CompactLinkedMap<String, Integer> other = new CompactLinkedMap<>();
        other.put("b", 2);
        other.put("c", 3);

        Map<String, Integer> merged = map.plus(other);
        // original maps unchanged
        assertEquals(1, map.size());
        assertEquals(2, other.size());

        // merged map contains all entries
        assertEquals(3, merged.size());
        assertTrue(merged.containsKey("a"));
        assertTrue(merged.containsKey("b"));
        assertTrue(merged.containsKey("c"));
        assertEquals(Integer.valueOf(1), merged.get("a"));
        assertEquals(Integer.valueOf(2), merged.get("b"));
        assertEquals(Integer.valueOf(3), merged.get("c"));
    }

    @Test
    public void testInsertionOrderPreservedWhenDelegateSwitchesToLinkedHashMap() {
        int compactSize = getCompactSize(map);
        // Ensure we go beyond the compact threshold
        int entries = compactSize + 3;
        for (int i = 0; i < entries; i++) {
            map.put("k" + i, i);
        }

        // Capture the order from keySet (LinkedHashMap preserves insertion order)
        int index = 0;
        for (String key : map.keySet()) {
            assertEquals("Insertion order must be preserved", "k" + index, key);
            index++;
        }
        assertEquals("All entries should have been iterated", entries, index);

        // Remove a middle element and verify order of remaining keys
        map.remove("k2");
        String[] expected = {"k0", "k1", "k3", "k4", "k5"};
        index = 0;
        for (String key : map.keySet()) {
            assertEquals("Order after removal must remain consistent", expected[index], key);
            index++;
        }
        assertEquals("Remaining entry count after removal", expected.length, index);
    }

    @Test
    public void testReplaceExistingKeyDoesNotBreakOrder() {
        int compactSize = getCompactSize(map);
        // Fill to trigger LinkedHashMap
        for (int i = 0; i < compactSize + 1; i++) {
            map.put("key" + i, i);
        }
        // Replace an existing key
        map.put("key2", 999);
        // Verify order still respects original insertion sequence
        int idx = 0;
        for (String k : map.keySet()) {
            assertEquals("key" + idx, k);
            idx++;
        }
        // Verify the value was updated
        assertEquals(Integer.valueOf(999), map.get("key2"));
    }

    @Test
    public void testMinusDoesNotAffectOriginalMapWhenBeyondCompactSize() {
        int compactSize = getCompactSize(map);
        for (int i = 0; i < compactSize + 2; i++) {
            map.put("k" + i, i);
        }
        Map<String, Integer> copy = map.minus("k0");
        assertTrue("Original map must still contain the key after minus", map.containsKey("k0"));
        assertFalse("Result of minus must not contain the removed key", copy.containsKey("k0"));
        assertEquals("Result size must be original size - 1", map.size() - 1, copy.size());
    }

    @Test
    public void testPlusDoesNotModifyOriginalMapsWhenBeyondCompactSize() {
        int compactSize = getCompactSize(map);
        for (int i = 0; i < compactSize + 1; i++) {
            map.put("a" + i, i);
        }
        CompactLinkedMap<String, Integer> other = new CompactLinkedMap<>();
        for (int i = 0; i < compactSize + 1; i++) {
            other.put("b" + i, i * 10);
        }

        Map<String, Integer> merged = map.plus(other);
        // originals unchanged
        assertEquals(compactSize + 1, map.size());
        assertEquals(compactSize + 1, other.size());

        // merged contains all entries
        assertEquals((compactSize + 1) * 2, merged.size());
        for (int i = 0; i < compactSize + 1; i++) {
            assertEquals(Integer.valueOf(i), merged.get("a" + i));
            assertEquals(Integer.valueOf(i * 10), merged.get("b" + i));
        }
    }
}
