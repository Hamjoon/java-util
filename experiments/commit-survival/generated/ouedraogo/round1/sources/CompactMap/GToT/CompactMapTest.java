package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import java.util.*;
import java.lang.reflect.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests are organised as follows:
 * <ul>
 *   <li>Extraction of all public methods (see {@link #listPublicMethods()})</li>
 *   <li>Basic functional tests for each public API</li>
 *   <li>Edge‑case / exception tests (nulls, size transitions, iterator misuse, etc.)</li>
 *   <li>Tests that use a small {@code compactSize} by subclassing {@code CompactMap}
 *       so that array‑to‑map transitions can be exercised quickly.</li>
 *   <li>Reflection‑based checks for the protected {@code getLogicalValueType()} method.</li>
 * </ul>
 *
 * The class is deliberately self‑contained – no external mocking frameworks are used.
 */
public class CompactMapTest
{
    /* --------------------------------------------------------------------- */
    /* Helper subclass that allows us to control compactSize and the Map   */
    /* returned by getNewMap().                                            */
    /* --------------------------------------------------------------------- */
    private static class TestCompactMap<K, V> extends CompactMap<K, V>
    {
        private final int cs;
        private final Map<K, V> suppliedMap;   // may be null → use super.getNewMap()

        TestCompactMap(int compactSize, Map<K, V> map)
        {
            this.cs = compactSize;
            this.suppliedMap = map;
        }

        @Override
        protected int compactSize()
        {
            return cs;
        }

        @Override
        protected Map<K, V> getNewMap()
        {
            return suppliedMap != null ? suppliedMap : super.getNewMap();
        }
    }

    /* --------------------------------------------------------------------- */
    /* Helper to invoke protected getLogicalValueType() via reflection.      */
    /* --------------------------------------------------------------------- */
    private static CompactMap.LogicalValueType logicalValueType(CompactMap<?,?> map)
            throws Exception
    {
        Method m = CompactMap.class.getDeclaredMethod("getLogicalValueType");
        m.setAccessible(true);
        return (CompactMap.LogicalValueType) m.invoke(map);
    }

    /* --------------------------------------------------------------------- */
    /* 1.  List all public methods (used only for documentation purposes).   */
    /* --------------------------------------------------------------------- */
    @Test
    public void listPublicMethods()
    {
        Method[] methods = CompactMap.class.getMethods();
        Set<String> signatures = new TreeSet<>();
        for (Method m : methods)
        {
            // Only keep methods declared in CompactMap (exclude Object, Map default methods)
            if (m.getDeclaringClass() == CompactMap.class)
            {
                signatures.add(m.toString());
            }
        }
        // This assertion is just a sanity‑check – the set must not be empty.
        assertFalse("CompactMap should expose public methods", signatures.isEmpty());

        // Print the signatures – useful when reading test output.
        for (String s : signatures)
        {
            System.out.println(s);
        }
    }

    /* --------------------------------------------------------------------- */
    /* 2.  Basic behaviour of an empty map                                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void testEmptyMap()
    {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue(123));
        assertNull(map.get("any"));
        assertEquals("{}", map.toString());

        // equals & hashCode with another empty map
        CompactMap<String, Integer> other = new CompactMap<>();
        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());

        // minus / plus must throw UnsupportedOperationException
        try
        {
            map.minus("x");
            fail("minus() should throw UnsupportedOperationException");
        }
        catch (UnsupportedOperationException ignored) {}

        try
        {
            map.plus(Collections.singletonMap("a", 1));
            fail("plus() should throw UnsupportedOperationException");
        }
        catch (UnsupportedOperationException ignored) {}
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Single entry where key equals the special single‑value key ("key") */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSingleEntryKeyEqualsSingleValueKey()
    {
        CompactMap<String, String> map = new CompactMap<>();
        assertNull(map.put("key", "value"));   // first insertion returns null
        assertEquals(1, map.size());
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsValue("value"));
        assertEquals("value", map.get("key"));
        assertEquals("{key=value}", map.toString());

        // Overwrite the same key – should return old value
        assertEquals("value", map.put("key", "new"));
        assertEquals("new", map.get("key"));

        // Remove
        assertEquals("new", map.remove("key"));
        assertTrue(map.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 4.  Single entry where key is different from the special key        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSingleEntryDifferentKey()
    {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 10);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsValue(10));
        assertEquals(Integer.valueOf(10), map.get("a"));

        // remove
        assertEquals(Integer.valueOf(10), map.remove("a"));
        assertTrue(map.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 5.  Null key and null value handling                                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void testNullKeyAndValue()
    {
        CompactMap<Object, Object> map = new CompactMap<>();
        map.put(null, null);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));

        // Overwrite null key with a real value
        map.put(null, "notNull");
        assertEquals("notNull", map.get(null));
        assertEquals(1, map.size());
    }

    /* --------------------------------------------------------------------- */
    /* 6.  Transition from empty → single → array → map (using small size) */
    /* --------------------------------------------------------------------- */
    @Test
    public void testTransitionsWithSmallCompactSize()
            throws Exception
    {
        // compactSize = 2 : after 2 entries we still use array, after 3 we switch to Map
        CompactMap<String, Integer> map = new TestCompactMap<>(2, null);

        // first entry – single value
        map.put("key", 1);
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.OBJECT,
                logicalValueType(map));

        // second entry – array
        map.put("a", 2);
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY,
                logicalValueType(map));

        // third entry – should switch to Map implementation
        map.put("b", 3);
        assertEquals(3, map.size());
        assertEquals(CompactMap.LogicalValueType.MAP,
                logicalValueType(map));

        // verify map semantics
        assertTrue(map.containsKey("a"));
        assertEquals(Integer.valueOf(2), map.get("a"));
        assertTrue(map.containsValue(3));

        // remove one entry – size becomes 2, should convert back to array
        map.remove("b");
        assertEquals(2, map.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY,
                logicalValueType(map));

        // remove another – back to single entry (object)
        map.remove("a");
        assertEquals(1, map.size());
        assertEquals(CompactMap.LogicalValueType.OBJECT,
                logicalValueType(map));
    }

    /* --------------------------------------------------------------------- */
    /* 7.  putAll() behaviour – both small and large collections            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testPutAll()
    {
        // small map – should use repeated put()
        CompactMap<String, Integer> map1 = new TestCompactMap<>(5, null);
        Map<String, Integer> src = new HashMap<>();
        src.put("x", 1);
        src.put("y", 2);
        map1.putAll(src);
        assertEquals(2, map1.size());
        assertTrue(map1.containsKey("x"));
        assertTrue(map1.containsKey("y"));

        // large map – trigger direct map insertion
        CompactMap<String, Integer> map2 = new TestCompactMap<>(2, null);
        Map<String, Integer> big = new HashMap<>();
        for (int i = 0; i < 10; i++) big.put("k"+i, i);
        map2.putAll(big);
        assertEquals(10, map2.size());
        assertEquals(CompactMap.LogicalValueType.MAP, logicalValueType(map2));
    }

    /* --------------------------------------------------------------------- */
    /* 8.  keySet iterator – normal iteration and remove() misuse         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testKeySetIterator()
    {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Iterator<String> it = map.keySet().iterator();

        // remove() before next() must throw IllegalStateException
        try
        {
            it.remove();
            fail("Expected IllegalStateException when remove() called before next()");
        }
        catch (IllegalStateException ignored) {}

        // normal iteration
        Set<String> seen = new HashSet<>();
        while (it.hasNext())
        {
            String k = it.next();
            seen.add(k);
            if ("b".equals(k))
                it.remove();   // remove element during iteration – should succeed
        }
        assertEquals(2, map.size());
        assertFalse(map.containsKey("b"));
        assertTrue(seen.containsAll(Arrays.asList("a","b","c")));

        // concurrent modification detection
        Iterator<String> it2 = map.keySet().iterator();
        map.put("d", 4);   // modify map after iterator creation
        try
        {
            it2.hasNext(); // may or may not throw; next() will definitely detect
            it2.next();
            fail("Expected ConcurrentModificationException after map modification");
        }
        catch (ConcurrentModificationException ignored) {}
    }

    /* --------------------------------------------------------------------- */
    /* 9.  Copy iterator path – force useCopyIterator() == true               */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCopyIteratorPath()
    {
        // TreeMap implements SortedMap → useCopyIterator() will be true
        CompactMap<String, Integer> map = new TestCompactMap<>(5, new TreeMap<>());
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        // entrySet iterator should be a CopyEntryIterator (uses a copy of the map)
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        Set<String> keys = new HashSet<>();
        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = it.next();
            keys.add(e.getKey());
            // test iterator.remove() (which removes from the underlying map)
            if ("b".equals(e.getKey()))
                it.remove();
        }
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());
        assertTrue(keys.containsAll(Arrays.asList("a","b","c")));
    }

    /* --------------------------------------------------------------------- */
    /* 10. entrySet retainAll & removeAll semantics                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testEntrySetRetainAndRemoveAll()
    {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        // retain only a and c
        Set<Map.Entry<String, Integer>> retain = new HashSet<>();
        retain.add(new AbstractMap.SimpleEntry<>("a", 1));
        retain.add(new AbstractMap.SimpleEntry<>("c", 3));
        assertTrue(map.entrySet().retainAll(retain));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("c"));
        assertFalse(map.containsKey("b"));

        // removeAll the remaining entries
        Set<Map.Entry<String, Integer>> toRemove = new HashSet<>(map.entrySet());
        assertTrue(map.entrySet().removeAll(toRemove));
        assertTrue(map.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /* 11. equals() and hashCode() consistency with standard maps          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode()
    {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("x", 10);
        map.put("y", 20);

        Map<String, Integer> javaMap = new HashMap<>();
        javaMap.put("x", 10);
        javaMap.put("y", 20);

        assertTrue(map.equals(javaMap));
        assertTrue(javaMap.equals(map));
        assertEquals(map.hashCode(), javaMap.hashCode());

        // change one value – equality should break
        map.put("y", 99);
        assertFalse(map.equals(javaMap));
    }

    /* --------------------------------------------------------------------- */
    /* 12. Constructor validation – compactSize() < 2 must throw IllegalStateException */
    /* --------------------------------------------------------------------- */
    @Test
    public void testIllegalCompactSize()
    {
        try
        {
            new TestCompactMap<>(1, null);
            fail("Expected IllegalStateException when compactSize() < 2");
        }
        catch (IllegalStateException ignored) {}
    }

    /* --------------------------------------------------------------------- */
    /* 13. CompactMapEntry setValue() write‑through test                     */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCompactMapEntrySetValue()
    {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        // retrieve the entry via entrySet()
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        assertTrue(entry instanceof CompactMap.CompactMapEntry);
        CompactMap.CompactMapEntry cmEntry = (CompactMap.CompactMapEntry) entry;

        // change value through entry
        Integer old = cmEntry.setValue(42);
        assertEquals(Integer.valueOf(1), old);
        assertEquals(Integer.valueOf(42), map.get("a"));
    }

    /* --------------------------------------------------------------------- */
    /* 14. verify computeKeyHashCode / computeValueHashCode indirectly via hashCode() */
    /* --------------------------------------------------------------------- */
    @Test
    public void testHashCodeComputation()
    {
        CompactMap<String, String> map = new CompactMap<>();
        map.put("key", "val");
        int expected = "key".hashCode() ^ "val".hashCode();
        assertEquals(expected, map.hashCode());

        // test with null key/value
        map.clear();
        map.put(null, null);
        // null key hash = 0, null value hash = 0 → overall 0
        assertEquals(0, map.hashCode());
    }

    /* --------------------------------------------------------------------- */
    /* 15. toString formatting with various internal states                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToStringVariousStates()
    {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertEquals("{}", map.toString());

        map.put("k", 5);
        assertEquals("{k=5}", map.toString());

        map.put("x", 9);
        // order for two entries in array form is insertion order
        assertEquals("{k=5, x=9}", map.toString());

        map.put("z", 12); // now uses internal Map (HashMap) – order not guaranteed
        String s = map.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        // ensure all three pairs appear
        assertTrue(s.contains("k=5"));
        assertTrue(s.contains("x=9"));
        assertTrue(s.contains("z=12"));
    }

    /* --------------------------------------------------------------------- */
    /* 16. values() collection iterator test (including remove)            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testValuesIterator()
    {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Iterator<Integer> it = map.values().iterator();
        Set<Integer> collected = new HashSet<>();
        while (it.hasNext())
        {
            Integer v = it.next();
            collected.add(v);
            if (v == 1)
                it.remove();
        }
        assertEquals(1, map.size());
        assertFalse(map.containsKey("a"));
        assertTrue(collected.containsAll(Arrays.asList(1, 2)));
    }

    /* --------------------------------------------------------------------- */
    /* 17. verify that clear() returns the map to EMPTY logical state      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testClearResetsState()
            throws Exception
    {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, logicalValueType(map));
    }
}
