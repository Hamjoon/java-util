package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CompactCIHashMapTest {

    private CompactCIHashMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactCIHashMap<>();
    }

    /*--------------------------------------------------------------
     *  Constructor tests
     *--------------------------------------------------------------*/

    @Test
    public void testEmptyConstructorCreatesEmptyMap() {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructorWithExistingMapCopiesEntries() {
        Map<String, Integer> src = new HashMap<>();
        src.put("One", 1);
        src.put("Two", 2);
        CompactCIHashMap<String, Integer> copy = new CompactCIHashMap<>(src);
        assertEquals(2, copy.size());
        assertTrue(copy.containsKey("One"));
        assertTrue(copy.containsKey("Two"));
        assertEquals(Integer.valueOf(1), copy.get("One"));
        assertEquals(Integer.valueOf(2), copy.get("Two"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullMapThrowsNPE() {
        new CompactCIHashMap<>(null);
    }

    /*--------------------------------------------------------------
     *  Basic put / get / case‑insensitivity
     *--------------------------------------------------------------*/

    @Test
    public void testPutAndGetCaseInsensitiveStringKey() {
        map.put("Key", 100);
        // Different case must retrieve the same value
        assertEquals(Integer.valueOf(100), map.get("key"));
        assertEquals(Integer.valueOf(100), map.get("KEY"));
    }

    @Test
    public void testOriginalKeyCaseIsPreservedInKeySet() {
        map.put("MiXeD", 42);
        Set<String> keys = map.keySet();
        assertTrue(keys.contains("MiXeD"));
        // The stored key should keep the original case, not the queried case
        assertFalse(keys.contains("mixed"));
    }

    @Test
    public void testNonStringKeyIsCaseSensitive() {
        // Use an Integer as a key – should behave like a normal HashMap
        CompactCIHashMap<Integer, String> intKeyMap = new CompactCIHashMap<>();
        intKeyMap.put(10, "ten");
        assertEquals("ten", intKeyMap.get(10));
        assertNull(intKeyMap.get(new Integer(10))); // different object, but equals() true – still works
    }

    @Test
    public void testNullKeyIsAccepted() {
        // HashMap permits a single null key; CompactCIHashMap should as well
        map.put(null, 999);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(999), map.get(null));
        // Case‑insensitivity does not affect null
        assertEquals(Integer.valueOf(999), map.remove(null));
        assertFalse(map.containsKey(null));
    }

    /*--------------------------------------------------------------
     *  containsKey / containsValue
     *--------------------------------------------------------------*/

    @Test
    public void testContainsKeyIsCaseInsensitive() {
        map.put("Hello", 1);
        assertTrue(map.containsKey("hello"));
        assertTrue(map.containsKey("HELLO"));
    }

    @Test
    public void testContainsValue() {
        map.put("A", 10);
        map.put("B", 20);
        assertTrue(map.containsValue(10));
        assertFalse(map.containsValue(30));
    }

    /*--------------------------------------------------------------
     *  remove / putAll / clear
     *--------------------------------------------------------------*/

    @Test
    public void testRemoveWithDifferentCase() {
        map.put("RemoveMe", 5);
        assertEquals(Integer.valueOf(5), map.remove("removeme"));
        assertFalse(map.containsKey("RemoveMe"));
    }

    @Test
    public void testPutAllCopiesAllEntriesCaseInsensitively() {
        Map<String, Integer> src = new HashMap<>();
        src.put("Alpha", 1);
        src.put("Beta", 2);
        map.putAll(src);
        assertEquals(Integer.valueOf(1), map.get("alpha"));
        assertEquals(Integer.valueOf(2), map.get("BETA"));
    }

    @Test
    public void testClearEmptiesMap() {
        map.put("X", 1);
        map.put("Y", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /*--------------------------------------------------------------
     *  minus / plus – functional (non‑mutating) operations
     *--------------------------------------------------------------*/

    @Test
    public void testMinusRemovesEntryAndReturnsNewMap() {
        map.put("keep", 1);
        map.put("drop", 2);
        @SuppressWarnings("unchecked")
        Map<String, Integer> result = (Map<String, Integer>) map.minus("DROP");
        // original map unchanged
        assertEquals(2, map.size());
        // result map should not contain the removed key (case‑insensitive)
        assertFalse(result.containsKey("drop"));
        assertTrue(result.containsKey("keep"));
        assertEquals(Integer.valueOf(1), result.get("keep"));
    }

    @Test
    public void testPlusAddsEntryAndReturnsNewMap() {
        map.put("a", 1);
        @SuppressWarnings("unchecked")
        Map<String, Integer> result = (Map<String, Integer>) map.plus(Collections.singletonMap("B", 2));
        // original map unchanged
        assertEquals(1, map.size());
        // result map should contain both entries, case‑insensitive for the new key
        assertTrue(result.containsKey("b"));
        assertEquals(Integer.valueOf(2), result.get("B"));
        assertEquals(Integer.valueOf(1), result.get("a"));
    }

    /*--------------------------------------------------------------
     *  equals / hashCode / toString
     *--------------------------------------------------------------*/

    @Test
    public void testEqualsIgnoresStringKeyCase() {
        CompactCIHashMap<String, Integer> other = new CompactCIHashMap<>();
        map.put("Case", 10);
        other.put("cAsE", 10);
        assertTrue(map.equals(other));
        assertTrue(other.equals(map));
        // hashCode must be consistent with equals
        assertEquals(map.hashCode(), other.hashCode());
    }

    @Test
    public void testEqualsWithDifferentTypes() {
        map.put("x", 1);
        HashMap<String, Integer> plain = new HashMap<>();
        plain.put("x", 1);
        // Different concrete classes – equals should return false
        assertFalse(map.equals(plain));
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("One", 1);
        map.put("Two", 2);
        String s = map.toString();
        assertNotNull(s);
        assertTrue(s.contains("One"));
        assertTrue(s.contains("Two"));
        assertTrue(s.contains("1"));
        assertTrue(s.contains("2"));
    }

    /*--------------------------------------------------------------
     *  keySet, values, entrySet – basic collection contracts
     *--------------------------------------------------------------*/

    @Test
    public void testKeySetReflectsCurrentEntries() {
        map.put("First", 10);
        map.put("Second", 20);
        Set<String> keys = map.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("First"));
        assertTrue(keys.contains("Second"));
        // Removing via iterator should affect the map
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            if ("First".equals(it.next())) {
                it.remove();
            }
        }
        assertFalse(map.containsKey("First"));
        assertEquals(1, map.size());
    }

    @Test
    public void testValuesCollection() {
        map.put("A", 5);
        map.put("B", 10);
        Collection<Integer> vals = map.values();
        assertEquals(2, vals.size());
        assertTrue(vals.contains(5));
        assertTrue(vals.contains(10));
    }

    @Test
    public void testEntrySetIteration() {
        map.put("Key1", 100);
        map.put("Key2", 200);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        Map<String, Integer> rebuilt = new HashMap<>();
        for (Map.Entry<String, Integer> e : entries) {
            rebuilt.put(e.getKey(), e.getValue());
        }
        assertEquals(2, rebuilt.size());
        assertEquals(Integer.valueOf(100), rebuilt.get("Key1"));
        assertEquals(Integer.valueOf(200), rebuilt.get("Key2"));
    }
}
