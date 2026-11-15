package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.*;

public class CompactLinkedMapTest
{
    private CompactLinkedMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactLinkedMap<>();
    }

    /** -----------------------------------------------------------------
     *  1.  Default constructor creates an empty map.
     * ----------------------------------------------------------------- */
    @Test
    public void testDefaultConstructorCreatesEmptyMap()
    {
        assertTrue("Map should be empty right after construction", map.isEmpty());
        assertEquals("Size must be 0", 0, map.size());
    }

    /** -----------------------------------------------------------------
     *  2.  Copy‑constructor copies all entries from the supplied map.
     * ----------------------------------------------------------------- */
    @Test
    public void testCopyConstructorCopiesAllEntries()
    {
        Map<String,Integer> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);

        CompactLinkedMap<String,Integer> copy = new CompactLinkedMap<>(source);
        assertEquals("Copy must have same size as source", source.size(), copy.size());
        assertTrue("Copy must contain all keys", copy.keySet().containsAll(source.keySet()));
        assertEquals("Values must be equal", source, copy);
    }

    /** -----------------------------------------------------------------
     *  3.  Basic put / get / contains semantics.
     * ----------------------------------------------------------------- */
    @Test
    public void testPutGetAndContains()
    {
        assertNull("Getting a non‑existing key must return null", map.get("missing"));
        assertFalse("containsKey must be false for missing key", map.containsKey("missing"));

        Integer previous = map.put("one", 1);
        assertNull("First put must return null", previous);
        assertEquals("Size after one put", 1, map.size());
        assertTrue("containsKey must be true after put", map.containsKey("one"));
        assertTrue("containsValue must be true after put", map.containsValue(1));
        assertEquals("Returned value must match", Integer.valueOf(1), map.get("one"));

        // replace existing key
        previous = map.put("one", 11);
        assertEquals("Previous value must be returned on replace", Integer.valueOf(1), previous);
        assertEquals("New value must be stored", Integer.valueOf(11), map.get("one"));
        assertEquals("Size must stay unchanged after replace", 1, map.size());
    }

    /** -----------------------------------------------------------------
     *  4.  Remove works and returns the removed value.
     * ----------------------------------------------------------------- */
    @Test
    public void testRemove()
    {
        map.put("x", 10);
        map.put("y", 20);
        assertEquals(2, map.size());

        Integer removed = map.remove("x");
        assertEquals(Integer.valueOf(10), removed);
        assertFalse(map.containsKey("x"));
        assertEquals(1, map.size());

        // removing a non‑existent key should return null
        assertNull(map.remove("notThere"));
    }

    /** -----------------------------------------------------------------
     *  5.  putAll copies all entries from another map.
     * ----------------------------------------------------------------- */
    @Test
    public void testPutAll()
    {
        Map<String,Integer> other = new LinkedHashMap<>();
        other.put("a", 1);
        other.put("b", 2);
        other.put("c", 3);

        map.putAll(other);
        assertEquals(other.size(), map.size());
        assertEquals(other, map);
    }

    /** -----------------------------------------------------------------
     *  6.  clear empties the map.
     * ----------------------------------------------------------------- */
    @Test
    public void testClear()
    {
        map.put("k1", 100);
        map.put("k2", 200);
        assertFalse(map.isEmpty());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /** -----------------------------------------------------------------
     *  7.  When the number of entries exceeds compactSize() the underlying
     *     map becomes a LinkedHashMap, therefore iteration order must be
     *     insertion order.
     * ----------------------------------------------------------------- */
    @Test
    public void testInsertionOrderAfterExceedingCompactSize()
    {
        // We are in the same package, so we can call the protected method.
        int compactSize = map.compactSize();

        // Fill the map with (compactSize + 2) entries to guarantee that the
        // delegate has switched to a LinkedHashMap.
        for (int i = 0; i < compactSize + 2; i++)
        {
            map.put("k" + i, i);
        }

        // Verify that iteration order matches insertion order.
        List<String> expectedOrder = new ArrayList<>();
        for (int i = 0; i < compactSize + 2; i++)
        {
            expectedOrder.add("k" + i);
        }

        List<String> actualOrder = new ArrayList<>();
        for (String key : map.keySet())
        {
            actualOrder.add(key);
        }

        assertEquals("Key set order must be insertion order after exceeding compactSize()",
                     expectedOrder, actualOrder);
    }

    /** -----------------------------------------------------------------
     *  8.  The iterator returned by entrySet() is *not* a copy iterator.
     * ----------------------------------------------------------------- */
    @Test
    public void testUseCopyIteratorIsFalse()
    {
        assertFalse("CompactLinkedMap must report useCopyIterator() == false",
                    map.useCopyIterator());
    }

    /** -----------------------------------------------------------------
     *  9.  minus(Object) returns a new map without the given key.
     * ----------------------------------------------------------------- */
    @Test
    public void testMinus()
    {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Map<String,Integer> result = map.minus("b");
        assertFalse("Result must not contain the removed key", result.containsKey("b"));
        assertTrue("Other keys must stay", result.containsKey("a"));
        assertTrue("Other keys must stay", result.containsKey("c"));
        assertEquals("Original map must stay unchanged", 3, map.size());
    }

    /** -----------------------------------------------------------------
     * 10.  plus(Object) returns a new map with the given entry added.
     * ----------------------------------------------------------------- */
    @Test
    public void testPlus()
    {
        map.put("x", 10);
        Map<String,Integer> result = map.plus(new AbstractMap.SimpleEntry<>("y", 20));

        // original map unchanged
        assertEquals(1, map.size());
        assertTrue(map.containsKey("x"));
        assertFalse(map.containsKey("y"));

        // result contains both entries
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(10), result.get("x"));
        assertEquals(Integer.valueOf(20), result.get("y"));
    }

    /** -----------------------------------------------------------------
     * 11.  equals() and hashCode() behave consistently with Map contract.
     * ----------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode()
    {
        CompactLinkedMap<String,Integer> other = new CompactLinkedMap<>();
        map.put("one", 1);
        map.put("two", 2);

        other.put("one", 1);
        other.put("two", 2);

        assertTrue("Maps with same entries must be equal", map.equals(other));
        assertEquals("Equal maps must have same hashCode", map.hashCode(), other.hashCode());

        other.put("three", 3);
        assertFalse("Maps with different entries must not be equal", map.equals(other));
    }

    /** -----------------------------------------------------------------
     * 12.  toString() contains the map entries (basic sanity check).
     * ----------------------------------------------------------------- */
    @Test
    public void testToStringContainsEntries()
    {
        map.put("alpha", 100);
        map.put("beta", 200);
        String s = map.toString();

        assertTrue("toString must contain key alpha", s.contains("alpha"));
        assertTrue("toString must contain value 100", s.contains("100"));
        assertTrue("toString must contain key beta", s.contains("beta"));
        assertTrue("toString must contain value 200", s.contains("200"));
    }

    /** -----------------------------------------------------------------
     * 13.  Handling of null keys and null values (behaviour defined by
     *     CompactMap – we only verify that the map does not throw
     *     unexpected exceptions).
     * ----------------------------------------------------------------- */
    @Test
    public void testNullKeyAndValue()
    {
        // Null key
        map.put(null, 42);
        assertTrue("Map must contain null key after insertion", map.containsKey(null));
        assertEquals(Integer.valueOf(42), map.get(null));

        // Null value
        map.put("nullValue", null);
        assertTrue("Map must contain key with null value", map.containsKey("nullValue"));
        assertNull(map.get("nullValue"));
    }
}
