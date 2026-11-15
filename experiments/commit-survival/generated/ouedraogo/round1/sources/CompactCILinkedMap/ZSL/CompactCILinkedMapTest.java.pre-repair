package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

public class CompactCILinkedMapTest {

    private CompactCILinkedMap<Object, Object> map;

    @Before
    public void setUp() {
        map = new CompactCILinkedMap<>();
    }

    /** Helper to obtain the protected compactSize() value via reflection. */
    private int getCompactSize() {
        try {
            Method m = CompactMap.class.getDeclaredMethod("compactSize");
            m.setAccessible(true);
            return (int) m.invoke(map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCaseInsensitivePutAndGet() {
        map.put("Foo", "bar");
        assertEquals("bar", map.get("foo"));
        assertEquals("bar", map.get("FOO"));
        assertTrue(map.containsKey("fOo"));
        assertEquals(1, map.size());

        // keySet should retain the original case of the key that was first inserted
        Set<Object> keys = map.keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.contains("Foo"));
        assertFalse(keys.contains("foo"));
    }

    @Test
    public void testCaseInsensitiveOverwriteKeepsOriginalKeyCase() {
        map.put("KeyOne", "first");
        map.put("kEyOnE", "second"); // should overwrite case‑insensitively
        assertEquals(1, map.size());
        assertEquals("second", map.get("KEYONE"));
        // The key stored should be the original case from the first insertion
        assertTrue(map.keySet().contains("KeyOne"));
        assertFalse(map.keySet().contains("kEyOnE"));
    }

    @Test
    public void testNonStringKeyBehaviour() {
        Integer intKey = 42;
        map.put(intKey, "answer");
        assertEquals("answer", map.get(intKey));
        assertNull(map.get("42"));               // String "42" is NOT the same key
        assertTrue(map.containsKey(intKey));
        assertFalse(map.containsKey("42"));
    }

    @Test
    public void testContainsKeyAndValue() {
        map.put("Hello", "World");
        assertTrue(map.containsKey("hello"));
        assertTrue(map.containsKey("HELLO"));
        assertTrue(map.containsValue("World"));
        assertFalse(map.containsValue("world"));
    }

    @Test
    public void testPutAllAndClear() {
        Map<String, String> other = new LinkedHashMap<>();
        other.put("A", "1");
        other.put("B", "2");
        other.put("C", "3");

        map.putAll(other);
        assertEquals(3, map.size());
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
        assertEquals("3", map.get("c"));

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testMinusCreatesNewMapWithoutKey() {
        map.put("Alpha", "A");
        map.put("Beta", "B");
        map.put("Gamma", "G");

        // minus should remove the key case‑insensitively and return a new map
        Map<Object, Object> withoutAlpha = map.minus("alpha");
        assertFalse(withoutAlpha.containsKey("Alpha"));
        assertTrue(withoutAlpha.containsKey("Beta"));
        assertTrue(withoutAlpha.containsKey("Gamma"));
        // original map must stay unchanged
        assertTrue(map.containsKey("Alpha"));
        assertEquals(3, map.size());
    }

    @Test
    public void testPlusCreatesNewMapWithAdditionalEntry() {
        map.put("One", "1");
        map.put("Two", "2");

        // The semantics of plus(Object) are not documented, but it must return a map.
        Map<Object, Object> plusMap = map.plus("Three");
        assertNotNull(plusMap);
        // The returned map should contain all original entries
        assertTrue(plusMap.containsKey("One"));
        assertTrue(plusMap.containsKey("Two"));
        // The original map must remain unchanged
        assertEquals(2, map.size());
        assertFalse(map.containsKey("Three"));
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactCILinkedMap<String, String> map1 = new CompactCILinkedMap<>();
        CompactCILinkedMap<String, String> map2 = new CompactCILinkedMap<>();

        map1.put("Key", "Value");
        map2.put("kEy", "Value");

        assertEquals(map1, map2);
        assertEquals(map1.hashCode(), map2.hashCode());

        map2.put("Other", "X");
        assertNotEquals(map1, map2);
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("X", "1");
        map.put("Y", "2");
        String s = map.toString();
        assertTrue(s.contains("X"));
        assertTrue(s.contains("Y"));
        assertTrue(s.contains("1"));
        assertTrue(s.contains("2"));
    }

    @Test
    public void testKeySetValuesAndEntrySetOrder() {
        map.put("first", "1");
        map.put("second", "2");
        map.put("third", "3");

        // keySet order must follow insertion order (LinkedHashMap semantics)
        List<Object> expectedKeys = Arrays.asList("first", "second", "third");
        assertEquals(expectedKeys, new ArrayList<>(map.keySet()));

        // values order must match key order
        List<Object> expectedValues = Arrays.asList("1", "2", "3");
        assertEquals(expectedValues, new ArrayList<>(map.values()));

        // entrySet iteration order must match insertion order
        List<String> entryStrings = new ArrayList<>();
        for (Map.Entry<Object, Object> e : map.entrySet()) {
            entryStrings.add(e.getKey() + "=" + e.getValue());
        }
        List<String> expectedEntries = Arrays.asList("first=1", "second=2", "third=3");
        assertEquals(expectedEntries, entryStrings);
    }

    @Test
    public void testSwitchToLinkedHashMapWhenExceedingCompactSize() {
        int compactSize = getCompactSize(); // protected value from CompactMap
        // Ensure we add more than compactSize entries
        for (int i = 0; i <= compactSize; i++) {
            map.put("key" + i, i);
        }
        assertEquals(compactSize + 1, map.size());

        // Verify that all entries are retrievable case‑insensitively
        for (int i = 0; i <= compactSize; i++) {
            assertEquals(i, map.get("KEY" + i));
        }

        // Verify insertion order is preserved even after the internal switch
        List<Object> expectedKeys = new ArrayList<>();
        for (int i = 0; i <= compactSize; i++) {
            expectedKeys.add("key" + i);
        }
        assertEquals(expectedKeys, new ArrayList<>(map.keySet()));
    }

    @Test
    public void testRemove() {
        map.put("RemoveMe", "value");
        assertTrue(map.containsKey("removeme"));
        Object removed = map.remove("REMOVEME");
        assertEquals("value", removed);
        assertFalse(map.containsKey("RemoveMe"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testIsEmptyAndSize() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("A", 1);
        assertFalse(map.isEmpty());
        assertEquals(1, map.size());
    }
}
