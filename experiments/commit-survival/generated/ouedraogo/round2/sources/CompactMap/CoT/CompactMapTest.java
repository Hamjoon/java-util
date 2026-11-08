package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactMap}.
 *
 * The tests are organised in the order requested:
 *   1. List of public methods.
 *   2. Basic functionality tests.
 *   3. Edge‑case / exception tests.
 *   4. Full combined test class.
 *
 * A small‑size subclass {@code TinyCompactMap} is used to force the
 * transition from the internal array representation to a backing {@link Map}
 * without having to insert dozens of entries.
 */
public class CompactMapTest
{
    /* -------------------------------------------------
     * 1. PUBLIC METHODS OF CompactMap (signatures only)
     * ------------------------------------------------- */
    // int size()
    // boolean isEmpty()
    // boolean containsKey(Object key)
    // boolean containsValue(Object value)
    // V get(Object key)
    // V put(K key, V value)
    // V remove(Object key)
    // void putAll(Map<? extends K, ? extends V> map)
    // void clear()
    // int hashCode()
    // boolean equals(Object obj)
    // String toString()
    // Set<K> keySet()
    // Collection<V> values()
    // Set<Map.Entry<K,V>> entrySet()
    // Map<K,V> minus(Object removeMe)
    // Map<K,V> plus(Object right)

    /* -------------------------------------------------
     * Helper subclass to make compactSize small (3)
     * ------------------------------------------------- */
    private static class TinyCompactMap<K, V> extends CompactMap<K, V>
    {
        @Override
        protected int compactSize()
        {
            return 3;               // very small to trigger map conversion quickly
        }

        @Override
        protected Map<K, V> getNewMap()
        {
            // Use a plain HashMap – the default would work as well
            return new HashMap<>(compactSize() + 1);
        }

        // expose the private "val" field for white‑box checks
        Object internalVal()
        {
            try
            {
                Field f = CompactMap.class.getDeclaredField("val");
                f.setAccessible(true);
                return f.get(this);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private CompactMap<String, Integer> map;          // default map (compactSize = 80)
    private TinyCompactMap<Integer, String> tinyMap; // small map for transition tests

    @Before
    public void setUp()
    {
        map = new CompactMap<>();
        tinyMap = new TinyCompactMap<>();
    }

    /* -------------------------------------------------
     * 2. BASIC FUNCTIONALITY TESTS
     * ------------------------------------------------- */

    @Test
    public void testEmptyMapBehavior()
    {
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());

        assertNull(map.get("any"));
        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue(123));

        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());
        assertTrue(map.entrySet().isEmpty());

        assertEquals("{}", map.toString());
    }

    @Test
    public void testSingleEntryWithDefaultSingleValueKey()
    {
        // default getSingleValueKey() returns the literal "key"
        map.put("key", 42);               // should store the value directly (no entry object)

        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        assertTrue(map.containsKey("key"));
        assertTrue(map.containsValue(42));
        assertEquals(Integer.valueOf(42), map.get("key"));

        // replace the same key – old value must be returned
        Integer old = map.put("key", 99);
        assertEquals(Integer.valueOf(42), old);
        assertEquals(Integer.valueOf(99), map.get("key"));

        // remove
        Integer removed = map.remove("key");
        assertEquals(Integer.valueOf(99), removed);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testSingleEntryWithNonDefaultKey()
    {
        map.put("foo", 1);               // key differs from getSingleValueKey()
        assertEquals(1, map.size());
        assertTrue(map.containsKey("foo"));
        assertTrue(map.containsValue(1));

        // entrySet must contain exactly one entry
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        assertEquals(1, es.size());
        Map.Entry<String, Integer> e = es.iterator().next();
        assertEquals("foo", e.getKey());
        assertEquals(Integer.valueOf(1), e.getValue());

        // remove via key
        Integer removed = map.remove("foo");
        assertEquals(Integer.valueOf(1), removed);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testTwoEntriesCreatesArrayRepresentation()
    {
        map.put("a", 10);
        map.put("b", 20);                // now size == 2 → internal Object[]

        assertEquals(2, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertEquals(Integer.valueOf(10), map.get("a"));
        assertEquals(Integer.valueOf(20), map.get("b"));

        // verify iteration order (implementation stores insertion order)
        Iterator<String> ki = map.keySet().iterator();
        assertEquals("a", ki.next());
        assertEquals("b", ki.next());

        // remove one entry – should shrink back to single entry
        Integer r = map.remove("a");
        assertEquals(Integer.valueOf(10), r);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("b"));
        assertFalse(map.containsKey("a"));
    }

    @Test
    public void testTransitionArrayToMapAndBackUsingTinyMap()
    {
        // TinyCompactMap has compactSize = 3.
        // Add 3 entries → still array, add 4th → switches to Map.
        tinyMap.put(1, "one");
        tinyMap.put(2, "two");
        tinyMap.put(3, "three");          // size == 3 (array)

        assertEquals(3, tinyMap.size());
        assertTrue(tinyMap.internalVal() instanceof Object[]);

        tinyMap.put(4, "four");           // size == 4 → map conversion
        assertEquals(4, tinyMap.size());
        assertTrue(tinyMap.internalVal() instanceof Map);

        // Verify all entries are present
        for (int i = 1; i <= 4; i++)
        {
            assertEquals(""+i, tinyMap.get(i));
        }

        // Remove entries until size drops back to compactSize (3) → should become array again
        tinyMap.remove(4);
        assertEquals(3, tinyMap.size());
        assertTrue(tinyMap.internalVal() instanceof Object[]);

        // Ensure remaining entries are still correct
        assertEquals("one", tinyMap.get(1));
        assertEquals("two", tinyMap.get(2));
        assertEquals("three", tinyMap.get(3));
    }

    @Test
    public void testPutAllTriggersMapWhenLarge()
    {
        Map<Integer, String> many = new HashMap<>();
        for (int i = 0; i < 10; i++) { many.put(i, "v"+i); }

        // tinyMap compactSize = 3, putAll with 10 entries should go straight to Map
        tinyMap.putAll(many);
        assertEquals(10, tinyMap.size());
        assertTrue(tinyMap.internalVal() instanceof Map);
    }

    @Test
    public void testEqualsAndHashCodeAgainstStandardMap()
    {
        Map<String, Integer> std = new HashMap<>();
        std.put("x", 100);
        std.put("y", 200);

        map.put("x", 100);
        map.put("y", 200);

        assertTrue(map.equals(std));
        assertTrue(std.equals(map));
        assertEquals(std.hashCode(), map.hashCode());
    }

    @Test
    public void testNullKeyAndNullValue()
    {
        map.put(null, null);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));

        // replace null key with a real value
        Integer old = map.put(null, 5);
        assertNull(old);
        assertEquals(Integer.valueOf(5), map.get(null));
    }

    @Test
    public void testMinusAndPlusUnsupported()
    {
        try
        {
            map.minus("any");
            fail("minus should throw UnsupportedOperationException");
        }
        catch (UnsupportedOperationException ignored) {}

        try
        {
            map.plus("any");
            fail("plus should throw UnsupportedOperationException");
        }
        catch (UnsupportedOperationException ignored) {}
    }

    /* -------------------------------------------------
     * 3. ITERATOR EDGE CASES
     * ------------------------------------------------- */

    @Test
    public void testCompactKeyIteratorRemove()
    {
        map.put("k1", 1);
        map.put("k2", 2);
        Iterator<String> it = map.keySet().iterator();

        // advance to first element and remove via iterator
        assertTrue(it.hasNext());
        String first = it.next();
        it.remove();                     // should remove the entry we just visited

        assertEquals(1, map.size());
        assertFalse(map.containsKey(first));
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveBeforeNextThrows()
    {
        map.put("a", 1);
        Iterator<String> it = map.keySet().iterator();
        it.remove();                     // illegal – next() not called yet
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorDetectsConcurrentModification()
    {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        map.put("c", 3);                 // structural change after iterator creation
        it.next();                       // should detect modification
    }

    @Test
    public void testCompactEntryIteratorContents()
    {
        map.put("x", 10);
        map.put("y", 20);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();

        Set<String> keys = new HashSet<>();
        Set<Integer> values = new HashSet<>();

        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = it.next();
            keys.add(e.getKey());
            values.add(e.getValue());
        }

        assertEquals(new HashSet<>(Arrays.asList("x", "y")), keys);
        assertEquals(new HashSet<>(Arrays.asList(10, 20)), values);
    }

    @Test
    public void testCopyIteratorsReflectCurrentState()
    {
        map.put("p", 5);
        map.put("q", 6);

        // copy iterator works on a snapshot – modifications after creation should not affect it
        Iterator<String> copyKeyIt = map.keySet().iterator(); // uses copy iterator only when underlying map is SortedMap; default is not, but we can still test behavior
        map.put("r", 7); // modify after iterator creation

        // The iterator should still iterate over the snapshot (2 entries)
        Set<String> seen = new HashSet<>();
        while (copyKeyIt.hasNext())
        {
            seen.add(copyKeyIt.next());
        }
        assertEquals(new HashSet<>(Arrays.asList("p", "q")), seen);
    }

    /* -------------------------------------------------
     * 4. TO_STRING VARIATIONS
     * ------------------------------------------------- */

    @Test
    public void testToStringFormatsCorrectly()
    {
        assertEquals("{}", map.toString());

        map.put("a", 1);
        assertEquals("{a=1}", map.toString());

        map.put("b", 2);
        // order is insertion order for the internal array representation
        assertEquals("{a=1, b=2}", map.toString());
    }
}
