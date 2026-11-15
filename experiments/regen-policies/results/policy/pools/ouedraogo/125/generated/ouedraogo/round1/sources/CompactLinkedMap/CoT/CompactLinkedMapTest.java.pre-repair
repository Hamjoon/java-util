package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class CompactLinkedMapTest {

    private CompactLinkedMap<String, String> map;

    @Before
    public void setUp() {
        map = new CompactLinkedMap<>();
    }

    /* -------------------------------------------------------------------- */
    /*  Basic constructor / empty‑map behaviour tests                        */
    /* -------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        assertTrue("Map should be empty after default construction", map.isEmpty());
        assertEquals("Size must be 0", 0, map.size());
    }

    @Test
    public void testConstructorWithMapCopiesEntries() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("a", "alpha");
        source.put("b", "bravo");
        CompactLinkedMap<String, String> copy = new CompactLinkedMap<>(source);

        // original map unchanged
        assertEquals(2, copy.size());
        assertEquals("alpha", copy.get("a"));
        assertEquals("bravo", copy.get("b"));

        // ensure insertion order is preserved
        Set<String> keys = copy.keySet();
        assertArrayEquals(new String[]{"a", "b"}, keys.toArray());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullMapThrowsNPE() {
        new CompactLinkedMap<String, String>(null);
    }

    /* -------------------------------------------------------------------- */
    /*  put / get / overwrite tests                                          */
    /* -------------------------------------------------------------------- */

    @Test
    public void testPutAndGetSimple() {
        map.put("k1", "v1");
        assertEquals("v1", map.get("k1"));
        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsValue("v1"));
    }

    @Test
    public void testPutOverwriteKeepsInsertionOrder() {
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k1", "v1-updated"); // overwrite existing key

        assertEquals(2, map.size());
        assertEquals("v1-updated", map.get("k1"));
        // insertion order: k1 (first), k2, k1 (still at first position)
        String[] expectedOrder = {"k1", "k2"};
        assertArrayEquals(expectedOrder, map.keySet().toArray());
    }

    @Test
    public void testPutNullKeyAndNullValue() {
        map.put(null, "nullKey");
        map.put("nullValue", null);

        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("nullValue"));
    }

    /* -------------------------------------------------------------------- */
    /*  Removal / clear tests                                                */
    /* -------------------------------------------------------------------- */

    @Test
    public void testRemoveExistingKey() {
        map.put("x", "X");
        map.put("y", "Y");
        String removed = map.remove("x");
        assertEquals("X", removed);
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());
    }

    @Test
    public void testRemoveNonExistingKeyReturnsNull() {
        map.put("a", "A");
        assertNull(map.remove("nonexistent"));
        assertEquals(1, map.size());
    }

    @Test
    public void testClearEmptiesMap() {
        map.put("a", "A");
        map.put("b", "B");
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /* -------------------------------------------------------------------- */
    /*  Collection view order tests                                          */
    /* -------------------------------------------------------------------- */

    @Test
    public void testInsertionOrderPreservedInKeySet() {
        map.put("first", "1");
        map.put("second", "2");
        map.put("third", "3");

        String[] expected = {"first", "second", "third"};
        assertArrayEquals(expected, map.keySet().toArray());
    }

    @Test
    public void testInsertionOrderPreservedInValues() {
        map.put("first", "1");
        map.put("second", "2");
        map.put("third", "3");

        String[] expected = {"1", "2", "3"};
        assertArrayEquals(expected, map.values().toArray());
    }

    @Test
    public void testInsertionOrderPreservedInEntrySet() {
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");

        String[] expectedKeys = {"k1", "k2", "k3"};
        String[] expectedValues = {"v1", "v2", "v3"};

        int i = 0;
        for (Map.Entry<String, String> e : map.entrySet()) {
            assertEquals(expectedKeys[i], e.getKey());
            assertEquals(expectedValues[i], e.getValue());
            i++;
        }
        assertEquals(3, i);
    }

    /* -------------------------------------------------------------------- */
    /*  Map contract helpers (minus / plus)                                 */
    /* -------------------------------------------------------------------- */

    @Test
    public void testMinusRemovesKeyAndReturnsNewMap() {
        map.put("a", "A");
        map.put("b", "B");
        Map<String, String> result = map.minus("a");
        assertFalse(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        // original map must stay unchanged
        assertTrue(map.containsKey("a"));
    }

    @Test
    public void testPlusAddsEntryFromAnotherMap() {
        map.put("a", "A");
        Map<String, String> other = Collections.singletonMap("b", "B");
        Map<String, String> result = map.plus(other);
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        // original map unchanged
        assertEquals(1, map.size());
    }

    @Test
    public void testMinusWithNullArgumentReturnsSameMap() {
        map.put("x", "X");
        Map<String, String> result = map.minus(null);
        assertEquals(map, result);
    }

    @Test
    public void testPlusWithNullArgumentReturnsSameMap() {
        map.put("x", "X");
        Map<String, String> result = map.plus(null);
        assertEquals(map, result);
    }

    /* -------------------------------------------------------------------- */
    /*  Protected methods accessed via reflection                           */
    /* -------------------------------------------------------------------- */

    @Test
    public void testGetNewMapCreatesLinkedHashMap() throws Exception {
        Method m = CompactLinkedMap.class.getDeclaredMethod("getNewMap");
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> newMap = (Map<String, String>) m.invoke(map);
        assertTrue("Returned map should be a LinkedHashMap", newMap instanceof LinkedHashMap);
        // The map should be initially empty
        assertTrue(newMap.isEmpty());
    }

    @Test
    public void testUseCopyIteratorReturnsFalse() throws Exception {
        Method m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
        m.setAccessible(true);
        Boolean result = (Boolean) m.invoke(map);
        assertFalse("useCopyIterator should return false", result);
    }

    /* -------------------------------------------------------------------- */
    /*  Miscellaneous contract checks                                         */
    /* -------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        CompactLinkedMap<String, String> other = new CompactLinkedMap<>();
        map.put("k", "v");
        other.put("k", "v");

        assertEquals(map, other);
        assertEquals(map.hashCode(), other.hashCode());

        other.put("k2", "v2");
        assertNotEquals(map, other);
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("one", "1");
        map.put("two", "2");
        String s = map.toString();
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
    }
}
