package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCILinkedMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Both public constructors</li>
 *   <li>Protected factory methods {@code getNewMap()}, {@code isCaseInsensitive()}, {@code useCopyIterator()}</li>
 *   <li>Case‑insensitive behaviour of {@code put}/{@code get}/{@code containsKey}</li>
 *   <li>Insertion order preservation (the linked‑hash part)</li>
 *   <li>Typical edge cases (null arguments, empty map)</li>
 * </ul>
 *
 * Only APIs that appear in the supplied source or in the public API of {@link CompactMap}
 * are used – no external mocking or subclassing.
 */
public class CompactCILinkedMapTest
{
    private CompactCILinkedMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactCILinkedMap<>();
    }

    /* --------------------------------------------------------------
     *  Constructor tests
     * -------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap()
    {
        assertTrue("New map should be empty", map.isEmpty());
        assertEquals("Size of new map should be 0", 0, map.size());
    }

    @Test
    public void testConstructorWithExistingMapCopiesEntries()
    {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("One", 1);
        source.put("Two", 2);
        source.put("Three", 3);

        CompactCILinkedMap<String, Integer> copy = new CompactCILinkedMap<>(source);

        // verify size and content
        assertEquals("Copy should have same size as source", source.size(), copy.size());
        for (Map.Entry<String, Integer> e : source.entrySet())
        {
            assertTrue("Copy should contain key " + e.getKey(), copy.containsKey(e.getKey()));
            assertEquals("Value for key " + e.getKey() + " should match", e.getValue(), copy.get(e.getKey()));
        }

        // verify case‑insensitivity in the copy
        assertEquals(Integer.valueOf(1), copy.get("one"));
        assertEquals(Integer.valueOf(2), copy.get("TWO"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullMapThrows()
    {
        // According to typical Map contract, passing null should NPE.
        new CompactCILinkedMap<>(null);
    }

    /* --------------------------------------------------------------
     *  Protected factory / flag methods (accessed via reflection)
     * -------------------------------------------------------------- */

    @Test
    public void testGetNewMapCreatesCaseInsensitiveLinkedHashMap()
    {
        // Use reflection to invoke the protected method
        try
        {
            java.lang.reflect.Method m = CompactCILinkedMap.class.getDeclaredMethod("getNewMap");
            m.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Integer> newMap = (Map<String, Integer>) m.invoke(map);

            // The returned map must be a CaseInsensitiveMap (runtime type check)
            assertTrue("Returned map should be an instance of CaseInsensitiveMap",
                    newMap.getClass().getSimpleName().contains("CaseInsensitiveMap"));

            // It must be backed by a LinkedHashMap – we can verify ordering behaviour
            newMap.put("A", 10);
            newMap.put("B", 20);
            newMap.put("C", 30);
            Set<String> keys = newMap.keySet();

            // LinkedHashMap preserves insertion order
            assertArrayEquals("Insertion order should be preserved",
                    new String[]{"A", "B", "C"},
                    keys.toArray(new String[0]));
        }
        catch (Exception e)
        {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    public void testIsCaseInsensitiveFlag()
    {
        try
        {
            java.lang.reflect.Method m = CompactCILinkedMap.class.getDeclaredMethod("isCaseInsensitive");
            m.setAccessible(true);
            boolean result = (Boolean) m.invoke(map);
            assertTrue("CompactCILinkedMap must report case‑insensitive = true", result);
        }
        catch (Exception e)
        {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    public void testUseCopyIteratorFlag()
    {
        try
        {
            java.lang.reflect.Method m = CompactCILinkedMap.class.getDeclaredMethod("useCopyIterator");
            m.setAccessible(true);
            boolean result = (Boolean) m.invoke(map);
            assertFalse("CompactCILinkedMap must report useCopyIterator = false", result);
        }
        catch (Exception e)
        {
            fail("Reflection failed: " + e);
        }
    }

    /* --------------------------------------------------------------
     *  Behavioural tests – case‑insensitivity and ordering
     * -------------------------------------------------------------- */

    @Test
    public void testPutAndGetAreCaseInsensitive()
    {
        map.put("Alpha", 100);
        map.put("Beta", 200);

        // Different capitalisation should retrieve the same values
        assertEquals(Integer.valueOf(100), map.get("alpha"));
        assertEquals(Integer.valueOf(200), map.get("BETA"));

        // containsKey must also be case‑insensitive
        assertTrue(map.containsKey("ALPHA"));
        assertTrue(map.containsKey("beta"));
    }

    @Test
    public void testOverwriteWithDifferentCaseUpdatesExistingEntry()
    {
        map.put("KeyOne", 1);
        assertEquals(Integer.valueOf(1), map.get("keyone"));

        // Overwrite using a different case – should replace the original entry
        map.put("KEYONE", 42);
        assertEquals(Integer.valueOf(42), map.get("keyone"));
        assertEquals(1, map.size()); // size must stay 1
    }

    @Test
    public void testInsertionOrderIsPreservedInKeySet()
    {
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        // keySet from CompactCILinkedMap should reflect insertion order because we delegate to LinkedHashMap
        Set<String> keySet = map.keySet();
        assertArrayEquals("Key set order must follow insertion order",
                new String[]{"first", "second", "third"},
                keySet.toArray(new String[0]));
    }

    @Test
    public void testRemoveIsCaseInsensitive()
    {
        map.put("RemoveMe", 999);
        assertEquals(Integer.valueOf(999), map.remove("removeme"));
        assertFalse("Map should no longer contain the key after removal", map.containsKey("REMOVEME"));
        assertTrue("Map should be empty after removal", map.isEmpty());
    }

    @Test
    public void testClearEmptiesMap()
    {
        map.put("A", 1);
        map.put("B", 2);
        map.clear();
        assertTrue("Map should be empty after clear()", map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testPutAllCopiesCaseInsensitiveEntries()
    {
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("X", 10);
        src.put("Y", 20);
        map.putAll(src);

        // Retrieve using different case
        assertEquals(Integer.valueOf(10), map.get("x"));
        assertEquals(Integer.valueOf(20), map.get("y"));
        assertEquals(2, map.size());
    }

    @Test
    public void testContainsValueWorksNormally()
    {
        map.put("One", 1);
        map.put("Two", 2);
        assertTrue(map.containsValue(1));
        assertTrue(map.containsValue(2));
        assertFalse(map.containsValue(3));
    }

    @Test
    public void testMinusAndPlusReturnNewMaps()
    {
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);

        Map<String, Integer> afterMinus = map.minus("B");
        assertFalse(afterMinus.containsKey("B"));
        assertEquals(2, afterMinus.size());

        Map<String, Integer> afterPlus = map.plus("D");
        // The contract of plus(Object) in CompactMap returns a new map with the
        // supplied object as a key (value null). Verify that behaviour.
        assertTrue(afterPlus.containsKey("D"));
        assertNull(afterPlus.get("D"));
        assertEquals(4, afterPlus.size());
    }

    /* --------------------------------------------------------------
     *  Edge‑case tests
     * -------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testPutNullKeyThrows()
    {
        // Most map implementations used by Cedar (including CaseInsensitiveMap) reject null keys.
        map.put(null, 123);
    }

    @Test
    public void testPutNullValueIsAllowed()
    {
        // Null values are permitted – they should be stored and retrieved.
        map.put("NullValue", null);
        assertTrue(map.containsKey("nullvalue"));
        assertNull(map.get("NULLVALUE"));
        assertEquals(1, map.size());
    }

    @Test
    public void testEmptyMapBehavesCorrectly()
    {
        assertTrue("New map must be empty", map.isEmpty());
        assertEquals(0, map.size());
        assertFalse(map.containsKey("anything"));
        assertFalse(map.containsValue(42));
        assertTrue(map.keySet().isEmpty());
        assertTrue(map.values().isEmpty());
        assertTrue(map.entrySet().isEmpty());
    }
}
