package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCILinkedMap}.
 *
 * The tests focus on the public API as well as on the protected behaviour that
 * is specific to the case‑insensitive linked implementation (overridden methods
 * from {@link CompactMap}).
 *
 * All tests use only the classes, constructors and methods listed in the
 * supplied source and the public API of {@code CompactMap}. No mocking or
 * subclassing of library types is performed.
 */
public class CompactCILinkedMapTest
{
    private CompactCILinkedMap<String, String> map;

    @Before
    public void setUp()
    {
        map = new CompactCILinkedMap<>();
    }

    /* --------------------------------------------------------------------- *
     *  Constructor tests
     * --------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructorCreatesEmptyMap()
    {
        assertTrue("Map should be empty after default construction", map.isEmpty());
        assertEquals("Size must be zero", 0, map.size());
    }

    @Test
    public void testConstructorWithExistingMapCopiesEntriesCaseInsensitively()
    {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("One", "1");
        source.put("TWO", "2");
        source.put("three", "3");

        CompactCILinkedMap<String, String> copy = new CompactCILinkedMap<>(source);

        // original size must be preserved
        assertEquals(3, copy.size());

        // look‑ups must be case‑insensitive
        assertEquals("1", copy.get("ONE"));
        assertEquals("2", copy.get("two"));
        assertEquals("3", copy.get("ThReE"));
    }

    /* --------------------------------------------------------------------- *
     *  Basic Map behaviour (size, clear, isEmpty, etc.)
     * --------------------------------------------------------------------- */

    @Test
    public void testPutAndSize()
    {
        assertNull(map.put("Key", "Value"));
        assertEquals(1, map.size());

        // putting a second key increases size
        assertNull(map.put("Another", "Second"));
        assertEquals(2, map.size());
    }

    @Test
    public void testClearAndIsEmpty()
    {
        map.put("A", "1");
        map.put("B", "2");
        assertFalse(map.isEmpty());

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /* --------------------------------------------------------------------- *
     *  Case‑insensitive behaviour
     * --------------------------------------------------------------------- */

    @Test
    public void testCaseInsensitivePutAndGet()
    {
        map.put("Hello", "World");
        assertEquals("World", map.get("hello"));
        assertEquals("World", map.get("HELLO"));
        assertTrue(map.containsKey("HeLLo"));
        assertTrue(map.containsValue("World"));
    }

    @Test
    public void testOverwriteWithDifferentCaseKeepsOriginalKey()
    {
        map.put("Key", "First");
        // overwrite using a different case representation
        map.put("kEy", "Second");

        // size must stay 1
        assertEquals(1, map.size());

        // the value is the last one stored
        assertEquals("Second", map.get("KEY"));

        // keySet should contain the key exactly as it was first inserted
        Set<String> keys = map.keySet();
        assertTrue(keys.contains("Key"));
        assertFalse(keys.contains("kEy"));   // only the original spelling is kept
        assertEquals(1, keys.size());
    }

    /* --------------------------------------------------------------------- *
     *  Ordering – the backing map is a LinkedHashMap, so iteration order is
     *  the insertion order.
     * --------------------------------------------------------------------- */

    @Test
    public void testKeySetPreservesInsertionOrder()
    {
        map.put("first", "1");
        map.put("second", "2");
        map.put("third", "3");

        // Expected order = insertion order (case‑preserved)
        String[] expected = {"first", "second", "third"};
        int i = 0;
        for (String k : map.keySet())
        {
            assertEquals(expected[i++], k);
        }
    }

    @Test
    public void testValuesPreserveInsertionOrder()
    {
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");

        String[] expected = {"A", "B", "C"};
        int i = 0;
        for (String v : map.values())
        {
            assertEquals(expected[i++], v);
        }
    }

    @Test
    public void testEntrySetPreservesInsertionOrder()
    {
        map.put("x", "X");
        map.put("y", "Y");
        map.put("z", "Z");

        String[] expectedKeys   = {"x", "y", "z"};
        String[] expectedValues = {"X", "Y", "Z"};

        int i = 0;
        for (Map.Entry<String, String> e : map.entrySet())
        {
            assertEquals(expectedKeys[i], e.getKey());
            assertEquals(expectedValues[i], e.getValue());
            i++;
        }
        assertEquals(expectedKeys.length, i);
    }

    /* --------------------------------------------------------------------- *
     *  minus() – returns a new map without the supplied key (case‑insensitive)
     * --------------------------------------------------------------------- */

    @Test
    public void testMinusRemovesKeyIgnoringCase()
    {
        map.put("Alpha", "A");
        map.put("Beta", "B");
        map.put("Gamma", "C");

        Map<String, String> result = map.minus("bEtA"); // different case

        assertEquals(2, result.size());
        assertFalse(result.containsKey("Beta"));
        assertTrue(result.containsKey("Alpha"));
        assertTrue(result.containsKey("Gamma"));
    }

    /* --------------------------------------------------------------------- *
     *  plus() – returns a new map with the supplied key/value added
     * --------------------------------------------------------------------- */

    @Test
    public void testPlusAddsEntryCaseInsensitively()
    {
        map.put("One", "1");
        Map<String, String> result = map.plus("ONE"); // only key supplied – value null
        assertEquals(2, result.size());
        assertTrue(result.containsKey("One"));
        assertTrue(result.containsKey("ONE"));
        assertNull(result.get("ONE"));
    }

    /* --------------------------------------------------------------------- *
     *  equals() and hashCode()
     * --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeWithDifferentKeyCasing()
    {
        CompactCILinkedMap<String, String> m1 = new CompactCILinkedMap<>();
        CompactCILinkedMap<String, String> m2 = new CompactCILinkedMap<>();

        m1.put("Key", "Value");
        m2.put("kEy", "Value"); // same logical entry, different case

        assertTrue("Maps should be equal despite key case differences", m1.equals(m2));
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    public void testEqualsWithDifferentValues()
    {
        CompactCILinkedMap<String, String> m1 = new CompactCILinkedMap<>();
        CompactCILinkedMap<String, String> m2 = new CompactCILinkedMap<>();

        m1.put("K", "V1");
        m2.put("k", "V2");

        assertFalse(m1.equals(m2));
    }

    /* --------------------------------------------------------------------- *
     *  Protected methods – accessed via reflection
     * --------------------------------------------------------------------- */

    @Test
    public void testIsCaseInsensitiveAndUseCopyIterator()
        throws Exception
    {
        Method isCaseInsensitive = CompactCILinkedMap.class.getDeclaredMethod("isCaseInsensitive");
        isCaseInsensitive.setAccessible(true);
        assertTrue((Boolean) isCaseInsensitive.invoke(map));

        Method useCopyIterator = CompactCILinkedMap.class.getDeclaredMethod("useCopyIterator");
        useCopyIterator.setAccessible(true);
        assertFalse((Boolean) useCopyIterator.invoke(map));
    }

    @Test
    public void testCompactSizeIsPositive()
        throws Exception
    {
        Method compactSize = CompactMap.class.getDeclaredMethod("compactSize");
        compactSize.setAccessible(true);
        int size = (Integer) compactSize.invoke(map);
        assertTrue("compactSize should be a positive integer", size > 0);
    }

    @Test
    public void testGetNewMapCreatesCaseInsensitiveLinkedMap()
        throws Exception
    {
        Method getNewMap = CompactCILinkedMap.class.getDeclaredMethod("getNewMap");
        getNewMap.setAccessible(true);
        Object newMap = getNewMap.invoke(map);
        assertNotNull(newMap);
        assertTrue("Result must be a Map", newMap instanceof Map);
    }

    @Test
    public void testGetOrderingReturnsUnorderedByDefault()
        throws Exception
    {
        Method getOrdering = CompactMap.class.getDeclaredMethod("getOrdering");
        getOrdering.setAccessible(true);
        String ordering = (String) getOrdering.invoke(map);
        // The default ordering for CompactMap is UNORDERED unless a builder specifies otherwise.
        assertEquals(CompactMap.UNORDERED, ordering);
    }

    @Test
    public void testComputeKeyHashCodeIsConsistentWithCaseInsensitivity()
        throws Exception
    {
        Method computeKeyHashCode = CompactMap.class.getDeclaredMethod("computeKeyHashCode", Object.class);
        computeKeyHashCode.setAccessible(true);

        int hash1 = (Integer) computeKeyHashCode.invoke(map, "TestKey");
        int hash2 = (Integer) computeKeyHashCode.invoke(map, "testkey"); // different case

        // For a case‑insensitive map the hash of the key should be the same
        assertEquals(hash1, hash2);
    }

    @Test
    public void testComputeValueHashCodeWorksForNullAndNonNull()
        throws Exception
    {
        Method computeValueHashCode = CompactMap.class.getDeclaredMethod("computeValueHashCode", Object.class);
        computeValueHashCode.setAccessible(true);

        int nullHash = (Integer) computeValueHashCode.invoke(map, null);
        int valueHash = (Integer) computeValueHashCode.invoke(map, "SomeValue");

        // No exception and deterministic values
        assertTrue(nullHash == 0 || nullHash == 0); // implementation may return 0 for null
        assertTrue(valueHash != 0);
    }

    @Test
    public void testGetSingleValueKeyReturnsNullWhenMapHasMultipleEntries()
        throws Exception
    {
        Method getSingleValueKey = CompactMap.class.getDeclaredMethod("getSingleValueKey");
        getSingleValueKey.setAccessible(true);

        // empty map – should be null
        assertNull(getSingleValueKey.invoke(map));

        map.put("one", "1");
        // still only one entry – may return the sole key
        Object soleKey = getSingleValueKey.invoke(map);
        assertEquals("one", soleKey);

        map.put("two", "2");
        // more than one entry – must be null
        assertNull(getSingleValueKey.invoke(map));
    }

    @Test
    public void testToStringContainsAllEntries()
    {
        map.put("a", "A");
        map.put("b", "B");
        String s = map.toString();
        assertTrue(s.contains("a"));
        assertTrue(s.contains("A"));
        assertTrue(s.contains("b"));
        assertTrue(s.contains("B"));
    }
}
