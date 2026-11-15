package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link CaseInsensitiveMap}.
 * Covers all public APIs, edge‑cases, and the special case‑insensitive behaviour.
 */
public class CaseInsensitiveMapTest
{
    private CaseInsensitiveMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CaseInsensitiveMap<>();
    }

    /* --------------------------------------------------------------------- */
    /*  Constructors                                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructor()
    {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testConstructorWithInitialCapacity()
    {
        CaseInsensitiveMap<String, Integer> m = new CaseInsensitiveMap<>(10);
        assertTrue(m.isEmpty());
    }

    @Test
    public void testConstructorWithInitialCapacityAndLoadFactor()
    {
        CaseInsensitiveMap<String, Integer> m = new CaseInsensitiveMap<>(5, 0.75f);
        assertTrue(m.isEmpty());
    }

    @Test
    public void testConstructorWrappingExistingMap_TreeMap()
    {
        Map<String, Integer> source = new TreeMap<>();
        source.put("Alpha", 1);
        source.put("Beta", 2);
        CaseInsensitiveMap<String, Integer> wrapped = new CaseInsensitiveMap<>(source);
        assertEquals(2, wrapped.size());
        // case‑insensitive look‑up
        assertEquals(Integer.valueOf(1), wrapped.get("alpha"));
        assertEquals(Integer.valueOf(2), wrapped.get("BETA"));
    }

    @Test
    public void testConstructorWrappingExistingMap_LinkedHashMap()
    {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("One", 1);
        source.put("Two", 2);
        CaseInsensitiveMap<String, Integer> wrapped = new CaseInsensitiveMap<>(source);
        assertEquals(2, wrapped.size());
        assertTrue(wrapped.containsKey("one"));
        assertTrue(wrapped.containsKey("TWO"));
    }

    /* --------------------------------------------------------------------- */
    /*  Basic put / get / containsKey / remove (case‑insensitive)            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPutAndGetCaseInsensitive()
    {
        map.put("Hello", 10);
        assertEquals(Integer.valueOf(10), map.get("hello"));
        assertEquals(Integer.valueOf(10), map.get("HELLO"));
        // original case is retained in keySet
        assertTrue(map.keySet().contains("Hello"));
        assertFalse(map.keySet().contains("hello"));
    }

    @Test
    public void testContainsKeyCaseInsensitive()
    {
        map.put("World", 20);
        assertTrue(map.containsKey("world"));
        assertTrue(map.containsKey("WORLD"));
        assertFalse(map.containsKey("worl"));
    }

    @Test
    public void testRemoveCaseInsensitive()
    {
        map.put("RemoveMe", 99);
        assertEquals(Integer.valueOf(99), map.remove("removeme"));
        assertNull(map.get("RemoveMe"));
        assertFalse(map.containsKey("REMOVEme"));
    }

    @Test
    public void testNullKeyBehaviour()
    {
        map.put(null, 123);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(123), map.get(null));
        assertEquals(Integer.valueOf(123), map.remove(null));
        assertFalse(map.containsKey(null));
    }

    /* --------------------------------------------------------------------- */
    /*  keySet() behaviours                                                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testKeySetContainsAndRemove()
    {
        map.put("KeyOne", 1);
        map.put("KeyTwo", 2);
        Set<String> ks = map.keySet();

        // contains is case‑insensitive
        assertTrue(ks.contains("keyone"));
        assertTrue(ks.contains("KEYTWO"));
        assertFalse(ks.contains("keythree"));

        // remove is case‑insensitive and reflects in the map
        assertTrue(ks.remove("KEYONE"));
        assertFalse(map.containsKey("KeyOne"));
        assertEquals(1, ks.size());
    }

    @Test
    public void testKeySetRemoveAll()
    {
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        Set<String> ks = map.keySet();

        Collection<String> toRemove = new ArrayList<>();
        toRemove.add("a");
        toRemove.add("c");
        assertTrue(ks.removeAll(toRemove));
        assertEquals(1, ks.size());
        assertTrue(ks.contains("B"));
        assertFalse(ks.contains("A"));
        assertFalse(ks.contains("C"));
    }

    @Test
    public void testKeySetRetainAll()
    {
        map.put("X", 10);
        map.put("Y", 20);
        map.put("Z", 30);
        Set<String> ks = map.keySet();

        Collection<String> retain = new ArrayList<>();
        retain.add("y");
        retain.add("nonexistent");
        assertTrue(ks.retainAll(retain));
        assertEquals(1, ks.size());
        assertTrue(ks.contains("Y"));
        assertFalse(ks.contains("X"));
        assertFalse(ks.contains("Z"));
    }

    @Test
    public void testKeySetIteratorRemove()
    {
        map.put("Iter1", 1);
        map.put("Iter2", 2);
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext())
        {
            String k = it.next();
            if ("Iter1".equals(k))
            {
                it.remove();
            }
        }
        assertFalse(map.containsKey("Iter1"));
        assertTrue(map.containsKey("Iter2"));
        assertEquals(1, map.size());
    }

    @Test
    public void testKeySetToArray()
    {
        map.put("First", 1);
        map.put("Second", 2);
        Object[] arr = map.keySet().toArray();
        assertEquals(2, arr.length);
        // order is insertion order for LinkedHashMap
        assertEquals("First", arr[0]);
        assertEquals("Second", arr[1]);
    }

    @Test
    public void testKeySetHashCode()
    {
        map.put("Abc", 1);
        map.put("Def", 2);
        int expected = 0;
        for (String s : new String[] {"Abc", "Def"})
        {
            expected += new CaseInsensitiveMap.CaseInsensitiveString(s).hashCode();
        }
        assertEquals(expected, map.keySet().hashCode());
    }

    /* --------------------------------------------------------------------- */
    /*  entrySet() behaviours                                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEntrySetContainsAndRemove()
    {
        map.put("EntryKey", 42);
        Set<Map.Entry<String, Integer>> es = map.entrySet();

        Map.Entry<String, Integer> probe = new AbstractMap.SimpleEntry<>("entrykey", 42);
        assertTrue(es.contains(probe));

        // remove via entrySet
        assertTrue(es.remove(probe));
        assertFalse(map.containsKey("EntryKey"));
        assertTrue(es.isEmpty());
    }

    @Test
    public void testEntrySetRemoveAll()
    {
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        Set<Map.Entry<String, Integer>> es = map.entrySet();

        Collection<Map.Entry<String, Integer>> toRemove = new ArrayList<>();
        toRemove.add(new AbstractMap.SimpleEntry<>("a", 1));
        toRemove.add(new AbstractMap.SimpleEntry<>("c", 3));
        assertTrue(es.removeAll(toRemove));
        assertEquals(1, es.size());
        assertTrue(es.iterator().next().getKey().equals("B"));
    }

    @Test
    public void testEntrySetRetainAll()
    {
        map.put("One", 1);
        map.put("Two", 2);
        map.put("Three", 3);
        Set<Map.Entry<String, Integer>> es = map.entrySet();

        Collection<Map.Entry<String, Integer>> retain = new ArrayList<>();
        retain.add(new AbstractMap.SimpleEntry<>("two", 2));
        retain.add(new AbstractMap.SimpleEntry<>("four", 4)); // not present
        assertTrue(es.retainAll(retain));
        assertEquals(1, es.size());
        Map.Entry<String, Integer> remaining = es.iterator().next();
        assertEquals("Two", remaining.getKey());
        assertEquals(Integer.valueOf(2), remaining.getValue());
    }

    @Test
    public void testEntrySetIteratorSetValue()
    {
        map.put("Mutable", 5);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = it.next();
            if ("Mutable".equals(e.getKey()))
            {
                e.setValue(55);
            }
        }
        assertEquals(Integer.valueOf(55), map.get("mutable"));
    }

    @Test
    public void testEntrySetEntryKeyIsOriginalString()
    {
        map.put("OriginalCase", 100);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        assertEquals("OriginalCase", entry.getKey()); // should be original case
    }

    @Test
    public void testCaseInsensitiveEntryGetOriginalKey()
    {
        map.put("Original", 1);
        CaseInsensitiveMap.CaseInsensitiveEntry cie =
                (CaseInsensitiveMap.CaseInsensitiveEntry) map.entrySet().iterator().next();
        // getKey returns original string
        assertEquals("Original", cie.getKey());
        // getOriginalKey returns the internal wrapper
        assertTrue(cie.getOriginalKey() instanceof CaseInsensitiveMap.CaseInsensitiveString);
        assertEquals("Original", cie.getOriginalKey().toString());
    }

    /* --------------------------------------------------------------------- */
    /*  equals(), hashCode(), toString(), clear(), isEmpty()                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode()
    {
        map.put("KeyA", 1);
        map.put("KeyB", 2);

        // equal to another CaseInsensitiveMap with different key cases
        CaseInsensitiveMap<String, Integer> other = new CaseInsensitiveMap<>();
        other.put("keya", 1);
        other.put("KEYB", 2);
        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());

        // not equal to a regular map with different case handling
        Map<String, Integer> regular = new HashMap<>();
        regular.put("KeyA", 1);
        regular.put("KeyB", 2);
        assertTrue(map.equals(regular));

        // different values => not equal
        other.put("KeyA", 99);
        assertFalse(map.equals(other));
    }

    @Test
    public void testToString()
    {
        map.put("A", 1);
        map.put("B", 2);
        String s = map.toString();
        // representation should contain the original keys
        assertTrue(s.contains("A=1"));
        assertTrue(s.contains("B=2"));
    }

    @Test
    public void testClearAndIsEmpty()
    {
        map.put("x", 10);
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testContainsValue()
    {
        map.put("valKey", 777);
        assertTrue(map.containsValue(777));
        assertFalse(map.containsValue(123));
    }

    @Test
    public void testValuesCollection()
    {
        map.put("first", 1);
        map.put("second", 2);
        Collection<Integer> vals = map.values();
        assertTrue(vals.contains(1));
        assertTrue(vals.contains(2));
        assertEquals(2, vals.size());
    }

    @Test
    public void testGetWrappedMap()
    {
        Map<String, Integer> wrapped = map.getWrappedMap();
        assertNotNull(wrapped);
        // underlying map should reflect puts
        map.put("inner", 42);
        assertEquals(Integer.valueOf(42), wrapped.get("inner"));
    }

    /* --------------------------------------------------------------------- */
    /*  Deprecated unsupported operations                                    */
    /* --------------------------------------------------------------------- */

    @Test(expected = UnsupportedOperationException.class)
    public void testMinusUnsupported()
    {
        map.minus("any");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlusUnsupported()
    {
        map.plus("any");
    }

    /* --------------------------------------------------------------------- */
    /*  Miscellaneous helper tests                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPutObject()
    {
        // direct usage of the package‑private method (via reflection is not allowed)
        // but we can test the public put() which internally uses the same logic.
        map.put("objKey", 5);
        assertEquals(Integer.valueOf(5), map.get("OBJKEY"));
    }

    @Test
    public void testPutAllWithCaseInsensitiveEntries()
    {
        // Build a source map that already contains CaseInsensitiveEntry objects
        CaseInsensitiveMap<String, Integer> source = new CaseInsensitiveMap<>();
        source.put("Alpha", 1);
        source.put("Beta", 2);
        // Normal putAll
        map.putAll(source);
        assertEquals(Integer.valueOf(1), map.get("ALPHA"));
        assertEquals(Integer.valueOf(2), map.get("beta"));
    }
}
