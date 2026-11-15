package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 test suite for {@link CaseInsensitiveMap}.
 */
public class CaseInsensitiveMapTest
{
    private CaseInsensitiveMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CaseInsensitiveMap<>();
    }

    /* --------------------------------------------------------------
     *  Basic put / get / containsKey / remove behavior (String keys)
     * -------------------------------------------------------------- */
    @Test
    public void testPutGetCaseInsensitive()
    {
        assertNull(map.put("KeyOne", 1));
        // retrieval with different cases
        assertEquals(Integer.valueOf(1), map.get("keyone"));
        assertEquals(Integer.valueOf(1), map.get("KEYONE"));
        // original key is retained in keySet()
        Set<String> keys = map.keySet();
        assertTrue(keys.contains("KeyOne"));
        assertEquals(1, keys.size());
    }

    @Test
    public void testContainsKey()
    {
        map.put("AbC", 10);
        assertTrue(map.containsKey("abc"));
        assertTrue(map.containsKey("ABC"));
        assertFalse(map.containsKey("ab"));
        // non‑String key works as usual
        map.put((String) null, 99);
        assertTrue(map.containsKey(null));
    }

    @Test
    public void testRemove()
    {
        map.put("RemoveMe", 5);
        assertEquals(Integer.valueOf(5), map.remove("removeme"));
        assertFalse(map.containsKey("RemoveMe"));
        assertTrue(map.isEmpty());
    }

    /* --------------------------------------------------------------
     *  putObject (raw Object API) and non‑String keys
     * -------------------------------------------------------------- */
    @Test
    public void testPutObjectAndNonStringKey()
    {
        // String key via putObject
        map.putObject("ObjKey", 42);
        assertEquals(Integer.valueOf(42), map.get("objkey"));

        // Integer key (non‑String) should be stored unchanged
        CaseInsensitiveMap<Integer, String> intMap = new CaseInsensitiveMap<>();
        intMap.putObject(123, "value");
        assertEquals("value", intMap.get(123));
        assertNull(intMap.get("123")); // not a String key conversion
    }

    /* --------------------------------------------------------------
     *  putAll handling of CaseInsensitiveEntry and regular entries
     * -------------------------------------------------------------- */
    @Test
    public void testPutAll()
    {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("Alpha", 1);
        source.put("BETA", 2);
        source.put(null, 0);
        map.putAll(source);

        // values are present regardless of case
        assertEquals(Integer.valueOf(1), map.get("alpha"));
        assertEquals(Integer.valueOf(2), map.get("beta"));
        assertEquals(Integer.valueOf(0), map.get(null));

        // ensure original case is retained in keySet()
        Set<String> ks = map.keySet();
        assertTrue(ks.contains("Alpha"));
        assertTrue(ks.contains("BETA"));
        assertTrue(ks.contains(null));
    }

    /* --------------------------------------------------------------
     *  keySet view – contains, remove, removeAll, retainAll, iterator
     * -------------------------------------------------------------- */
    @Test
    public void testKeySetBasicOperations()
    {
        map.put("One", 1);
        map.put("Two", 2);
        map.put("Three", 3);

        Set<String> ks = map.keySet();

        // contains is case‑insensitive
        assertTrue(ks.contains("ONE"));
        assertTrue(ks.contains("two"));
        assertFalse(ks.contains("four"));

        // iterator returns original case strings
        List<String> iterated = new ArrayList<>();
        for (String k : ks) {
            iterated.add(k);
        }
        assertEquals(Arrays.asList("One", "Two", "Three"), iterated);

        // remove via keySet (different case)
        assertTrue(ks.remove("tWo"));
        assertFalse(map.containsKey("Two"));
        assertEquals(2, ks.size());

        // removeAll
        ks.removeAll(Arrays.asList("ONE", "three"));
        assertTrue(map.isEmpty());

        // re‑populate for retainAll test
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        ks = map.keySet();
        ks.retainAll(Arrays.asList("b", "c", "D"));
        assertEquals(2, ks.size());
        assertTrue(ks.contains("B"));
        assertTrue(ks.contains("C"));
        assertFalse(ks.contains("A"));
    }

    /* --------------------------------------------------------------
     *  entrySet view – contains, remove, retainAll, removeAll
     * -------------------------------------------------------------- */
    @Test
    public void testEntrySetOperations()
    {
        map.put("X", 10);
        map.put("Y", 20);
        map.put("Z", 30);

        Set<Map.Entry<String, Integer>> es = map.entrySet();

        // contains with different case key
        Map.Entry<String, Integer> look = new AbstractMap.SimpleEntry<>("x", 10);
        assertTrue(es.contains(look));

        // remove entry using different case
        assertTrue(es.remove(new AbstractMap.SimpleEntry<>("Y", 20)));
        assertFalse(map.containsKey("y"));
        assertEquals(2, es.size());

        // retainAll – keep only entry "z"
        Map.Entry<String, Integer> keep = new AbstractMap.SimpleEntry<>("Z", 30);
        es.retainAll(Collections.singleton(keep));
        assertEquals(1, es.size());
        assertTrue(map.containsKey("z"));
    }

    /* --------------------------------------------------------------
     *  equals() and hashCode() contract
     * -------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode()
    {
        map.put("Case", 100);
        map.put("test", 200);

        // a regular HashMap with same entries (different key case)
        Map<String, Integer> other = new HashMap<>();
        other.put("case", 100);
        other.put("TEST", 200);

        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), new CaseInsensitiveMap<>(other).hashCode());

        // inequality when a value differs
        other.put("test", 999);
        assertFalse(map.equals(other));
    }

    /* --------------------------------------------------------------
     *  toString, clear, isEmpty, values, containsValue
     * -------------------------------------------------------------- */
    @Test
    public void testUtilityMethods()
    {
        map.put("Alpha", 1);
        map.put("Beta", 2);
        map.put(null, 0);

        String str = map.toString();
        assertTrue(str.contains("Alpha"));
        assertTrue(str.contains("Beta"));
        assertTrue(str.contains("null"));

        Collection<Integer> vals = map.values();
        assertTrue(vals.containsAll(Arrays.asList(1, 2, 0)));
        assertTrue(map.containsValue(2));
        assertFalse(map.containsValue(99));

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /* --------------------------------------------------------------
     *  getWrappedMap() returns the internal map instance
     * -------------------------------------------------------------- */
    @Test
    public void testGetWrappedMap()
    {
        Map<String, Integer> wrapped = map.getWrappedMap();
        assertNotNull(wrapped);
        assertTrue(wrapped.isEmpty());

        map.put("Wrapped", 55);
        assertEquals(Integer.valueOf(55), wrapped.get(new CaseInsensitiveMap.CaseInsensitiveString("wrapped")));
    }

    /* --------------------------------------------------------------
     *  Direct tests for CaseInsensitiveString behavior
     * -------------------------------------------------------------- */
    @Test
    public void testCaseInsensitiveStringEqualsAndCompare()
    {
        CaseInsensitiveMap.CaseInsensitiveString s1 = new CaseInsensitiveMap.CaseInsensitiveString("Hello");
        CaseInsensitiveMap.CaseInsensitiveString s2 = new CaseInsensitiveMap.CaseInsensitiveString("hello");
        CaseInsensitiveMap.CaseInsensitiveString s3 = new CaseInsensitiveMap.CaseInsensitiveString("World");

        // equality (case‑insensitive)
        assertTrue(s1.equals(s2));
        assertTrue(s2.equals("HELLO"));
        assertFalse(s1.equals(s3));

        // hashCode must be identical for equal strings
        assertEquals(s1.hashCode(), s2.hashCode());

        // compareTo (case‑insensitive ordering)
        assertEquals(0, s1.compareTo(s2));
        assertTrue(s1.compareTo(s3) < 0);
        assertTrue(s3.compareTo(s1) > 0);
        assertEquals(-1, s1.compareTo(new Object())); // non‑String comparison rule
    }

    /* --------------------------------------------------------------
     *  Verify that non‑String keys are stored unchanged
     * -------------------------------------------------------------- */
    @Test
    public void testNonStringKeyPreservation()
    {
        CaseInsensitiveMap<Integer, String> intKeyMap = new CaseInsensitiveMap<>();
        intKeyMap.put(10, "ten");
        intKeyMap.put(20, "twenty");

        assertEquals("ten", intKeyMap.get(10));
        assertNull(intKeyMap.get("10")); // not converted

        // keySet should contain the original Integer objects
        Set<Integer> ks = intKeyMap.keySet();
        assertTrue(ks.contains(10));
        assertTrue(ks.contains(20));
    }
}
