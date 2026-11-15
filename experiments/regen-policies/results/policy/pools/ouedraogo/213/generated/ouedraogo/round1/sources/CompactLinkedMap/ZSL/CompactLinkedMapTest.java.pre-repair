package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

public class CompactLinkedMapTest {

    private CompactLinkedMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactLinkedMap<>();
    }

    @Test
    public void testInitialState() {
        assertTrue("Map should be empty initially", map.isEmpty());
        assertEquals("Initial size should be 0", 0, map.size());
    }

    @Test
    public void testPutAndGet() {
        Integer previous = map.put("one", 1);
        assertNull("Previous value for new key should be null", previous);
        assertEquals("Size should be 1 after put", 1, map.size());
        assertEquals("Value retrieved should match inserted value", Integer.valueOf(1), map.get("one"));
    }

    @Test
    public void testOverwriteExistingKey() {
        map.put("key", 10);
        Integer previous = map.put("key", 20);
        assertEquals("Previous value should be the old value", Integer.valueOf(10), previous);
        assertEquals("New value should replace old value", Integer.valueOf(20), map.get("key"));
        assertEquals("Size should remain 1 after overwrite", 1, map.size());
    }

    @Test
    public void testRemove() {
        map.put("a", 1);
        map.put("b", 2);
        Integer removed = map.remove("a");
        assertEquals("Removed value should be returned", Integer.valueOf(1), removed);
        assertFalse("Map should no longer contain removed key", map.containsKey("a"));
        assertEquals("Size should decrease after removal", 1, map.size());
        assertNull("Getting removed key should return null", map.get("a"));
    }

    @Test
    public void testClear() {
        map.put("x", 100);
        map.put("y", 200);
        map.clear();
        assertTrue("Map should be empty after clear", map.isEmpty());
        assertEquals("Size should be 0 after clear", 0, map.size());
    }

    @Test
    public void testContainsKeyAndValue() {
        map.put("alpha", 5);
        assertTrue("containsKey should be true for existing key", map.containsKey("alpha"));
        assertFalse("containsKey should be false for missing key", map.containsKey("beta"));
        assertTrue("containsValue should be true for existing value", map.containsValue(5));
        assertFalse("containsValue should be false for missing value", map.containsValue(10));
    }

    @Test
    public void testPutAll() {
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);
        other.put("three", 3);
        map.putAll(other);
        assertEquals("Size should match number of inserted entries", 3, map.size());
        assertEquals("Values should be present after putAll", Integer.valueOf(2), map.get("two"));
    }

    @Test
    public void testKeySetValuesAndEntrySetOrder() {
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        // Verify insertion order for keySet
        List<String> expectedKeys = Arrays.asList("first", "second", "third");
        List<String> actualKeys = new ArrayList<>(map.keySet());
        assertEquals("keySet should preserve insertion order", expectedKeys, actualKeys);

        // Verify insertion order for values
        List<Integer> expectedValues = Arrays.asList(1, 2, 3);
        List<Integer> actualValues = new ArrayList<>(map.values());
        assertEquals("values collection should preserve insertion order", expectedValues, actualValues);

        // Verify insertion order for entrySet
        List<String> actualEntryKeys = new ArrayList<>();
        List<Integer> actualEntryValues = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            actualEntryKeys.add(entry.getKey());
            actualEntryValues.add(entry.getValue());
        }
        assertEquals("entrySet keys order", expectedKeys, actualEntryKeys);
        assertEquals("entrySet values order", expectedValues, actualEntryValues);
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactLinkedMap<String, Integer> other = new CompactLinkedMap<>();
        map.put("a", 1);
        map.put("b", 2);
        other.put("a", 1);
        other.put("b", 2);
        assertEquals("Maps with same entries should be equal", map, other);
        assertEquals("Hash codes should be equal for equal maps", map.hashCode(), other.hashCode());

        other.put("c", 3);
        assertNotEquals("Maps with different entries should not be equal", map, other);
    }

    @Test
    public void testMinusCreatesNewMapWithoutKey() {
        map.put("keep", 10);
        map.put("remove", 20);
        Map<String, Integer> result = map.minus("remove");
        assertFalse("Resulting map should not contain the removed key", result.containsKey("remove"));
        assertTrue("Resulting map should still contain other keys", result.containsKey("keep"));
        assertEquals("Original map should remain unchanged", Integer.valueOf(20), map.get("remove"));
    }

    @Test
    public void testPlusCreatesCombinedMap() {
        map.put("a", 1);
        map.put("b", 2);
        // plus should add a new entry; the API expects an Object, but typical usage is a Map entry.
        // We'll pass a single-entry map.
        Map<String, Integer> addition = new HashMap<>();
        addition.put("c", 3);
        Map<String, Integer> combined = map.plus(addition);
        assertTrue("Combined map should contain original key a", combined.containsKey("a"));
        assertTrue("Combined map should contain original key b", combined.containsKey("b"));
        assertTrue("Combined map should contain newly added key c", combined.containsKey("c"));
        assertEquals("New entry should have correct value", Integer.valueOf(3), combined.get("c"));
        // Original map unchanged
        assertFalse("Original map should not contain the new key", map.containsKey("c"));
    }

    @Test
    public void testGetConfigAndWithConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(CompactMap.CASE_SENSITIVE, Boolean.TRUE);
        CompactLinkedMap<String, Integer> configured = map.withConfig(config);
        Map<String, Object> retrievedConfig = configured.getConfig();
        assertNotNull("Config map should not be null", retrievedConfig);
        assertTrue("Config should contain the CASE_SENSITIVE entry", retrievedConfig.containsKey(CompactMap.CASE_SENSITIVE));
        assertEquals("Config value should match", Boolean.TRUE, retrievedConfig.get(CompactMap.CASE_SENSITIVE));
    }

    @Test
    public void testProtectedMethodsViaReflection() throws Exception {
        // getNewMap should return a LinkedHashMap instance
        Method getNewMapMethod = CompactLinkedMap.class.getDeclaredMethod("getNewMap");
        getNewMapMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Integer> newMap = (Map<String, Integer>) getNewMapMethod.invoke(map);
        assertTrue("getNewMap should return a LinkedHashMap", newMap instanceof LinkedHashMap);

        // isCaseInsensitive should return false
        Method caseInsensitiveMethod = CompactLinkedMap.class.getDeclaredMethod("isCaseInsensitive");
        caseInsensitiveMethod.setAccessible(true);
        Boolean caseInsensitive = (Boolean) caseInsensitiveMethod.invoke(map);
        assertFalse("CompactLinkedMap should be case-sensitive (isCaseInsensitive == false)", caseInsensitive);

        // useCopyIterator is defined in CompactLinkedMap (protected) – should be false
        Method copyIterMethod = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        copyIterMethod.setAccessible(true);
        Boolean useCopy = (Boolean) copyIterMethod.invoke(map);
        assertFalse("useCopyIterator should be false for CompactLinkedMap", useCopy);
    }

    @Test
    public void testLogicalValueTypeIsNotNull() {
        // LogicalValueType is part of the public API; ensure it returns a non-null value
        assertNotNull("LogicalValueType should not be null", map.getLogicalValueType());
    }

    @Test
    public void testConstructionFromExistingMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("x", 100);
        source.put("y", 200);
        CompactLinkedMap<String, Integer> fromMap = new CompactLinkedMap<>(source);
        assertEquals("Size should match source map size", source.size(), fromMap.size());
        assertTrue("All entries from source should be present", fromMap.keySet().containsAll(source.keySet()));
        assertEquals("Values should match source map values", source, fromMap);
    }
}
