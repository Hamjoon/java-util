package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.*;

public class CompactLinkedMapTest {

    private CompactLinkedMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactLinkedMap<>();
    }

    @Test
    public void testEmptyConstructor() {
        assertTrue("New map should be empty", map.isEmpty());
        assertEquals("Size should be 0", 0, map.size());
    }

    @Test
    public void testConstructorFromExistingMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        CompactLinkedMap<String, Integer> fromMap = new CompactLinkedMap<>(source);

        assertEquals("Size should match source map", source.size(), fromMap.size());
        assertTrue("Should contain key 'a'", fromMap.containsKey("a"));
        assertTrue("Should contain key 'b'", fromMap.containsKey("b"));
        assertEquals(Integer.valueOf(1), fromMap.get("a"));
        assertEquals(Integer.valueOf(2), fromMap.get("b"));
    }

    @Test
    public void testPutGetRemove() {
        assertNull(map.put("one", 1));
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertTrue(map.containsKey("one"));
        assertTrue(map.containsValue(1));

        Integer previous = map.put("one", 11);
        assertEquals(Integer.valueOf(1), previous);
        assertEquals(Integer.valueOf(11), map.get("one"));

        Integer removed = map.remove("one");
        assertEquals(Integer.valueOf(11), removed);
        assertFalse(map.containsKey("one"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, Integer> other = new HashMap<>();
        other.put("x", 10);
        other.put("y", 20);
        other.put("z", 30);

        map.putAll(other);
        assertEquals(3, map.size());
        assertTrue(map.keySet().containsAll(other.keySet()));
        assertTrue(map.values().containsAll(other.values()));

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testMinusAndPlus() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        // minus should return a new map without the specified key
        Map<String, Integer> minusMap = map.minus("b");
        assertFalse(minusMap.containsKey("b"));
        assertTrue(minusMap.containsKey("a"));
        assertTrue(minusMap.containsKey("c"));
        assertEquals(Integer.valueOf(1), minusMap.get("a"));
        assertEquals(Integer.valueOf(3), minusMap.get("c"));
        // original map unchanged
        assertTrue(map.containsKey("b"));

        // plus should return a new map containing the right argument as a key
        Map<String, Integer> plusMap = map.plus("d");
        assertTrue(plusMap.containsKey("d"));
        // value for new key is null (as per CompactMap.plus contract)
        assertNull(plusMap.get("d"));
        // original map unchanged
        assertFalse(map.containsKey("d"));
    }

    @Test
    public void testKeySetValuesEntrySetOrder() {
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        // Verify insertion order is preserved for keySet
        List<String> expectedKeys = Arrays.asList("first", "second", "third");
        List<String> actualKeys = new ArrayList<>(map.keySet());
        assertEquals("Key set order should follow insertion order", expectedKeys, actualKeys);

        // Verify insertion order is preserved for values collection
        List<Integer> expectedValues = Arrays.asList(1, 2, 3);
        List<Integer> actualValues = new ArrayList<>(map.values());
        assertEquals("Values order should follow insertion order", expectedValues, actualValues);

        // Verify entrySet order
        List<Map.Entry<String, Integer>> expectedEntries = new ArrayList<>();
        expectedEntries.add(new AbstractMap.SimpleEntry<>("first", 1));
        expectedEntries.add(new AbstractMap.SimpleEntry<>("second", 2));
        expectedEntries.add(new AbstractMap.SimpleEntry<>("third", 3));

        List<Map.Entry<String, Integer>> actualEntries = new ArrayList<>(map.entrySet());
        for (int i = 0; i < expectedEntries.size(); i++) {
            assertEquals(expectedEntries.get(i).getKey(), actualEntries.get(i).getKey());
            assertEquals(expectedEntries.get(i).getValue(), actualEntries.get(i).getValue());
        }
    }

    @Test
    public void testUnderlyingMapIsLinkedHashMap() throws NoSuchMethodException, IllegalAccessException {
        // Use reflection to invoke protected getNewMap()
        java.lang.reflect.Method m = CompactLinkedMap.class.getDeclaredMethod("getNewMap");
        m.setAccessible(true);
        Map<?, ?> newMap = (Map<?, ?>) m.invoke(new CompactLinkedMap<>());

        assertTrue("Underlying map should be a LinkedHashMap", newMap instanceof LinkedHashMap);

        // Verify the initial capacity is compactSize() + 1
        java.lang.reflect.Method csMethod = CompactLinkedMap.class.getDeclaredMethod("compactSize");
        csMethod.setAccessible(true);
        int compactSize = (int) csMethod.invoke(new CompactLinkedMap<>());

        // LinkedHashMap does not expose capacity, but we can at least ensure no exception was thrown
        assertNotNull(newMap);
        assertEquals(0, newMap.size());
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactLinkedMap<String, Integer> map1 = new CompactLinkedMap<>();
        CompactLinkedMap<String, Integer> map2 = new CompactLinkedMap<>();

        map1.put("k1", 100);
        map1.put("k2", 200);

        map2.put("k1", 100);
        map2.put("k2", 200);

        assertEquals("Maps with same entries should be equal", map1, map2);
        assertEquals("Hash codes should match for equal maps", map1.hashCode(), map2.hashCode());

        map2.put("k3", 300);
        assertNotEquals("Maps with different entries should not be equal", map1, map2);
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("alpha", 10);
        map.put("beta", 20);
        String s = map.toString();

        assertTrue("String representation should contain key 'alpha'", s.contains("alpha"));
        assertTrue("String representation should contain value 10", s.contains("10"));
        assertTrue("String representation should contain key 'beta'", s.contains("beta"));
        assertTrue("String representation should contain value 20", s.contains("20"));
    }
}
