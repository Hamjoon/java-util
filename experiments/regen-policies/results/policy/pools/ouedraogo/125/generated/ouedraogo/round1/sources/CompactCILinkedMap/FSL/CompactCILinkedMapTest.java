package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link CompactCILinkedMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (default and copy‑constructor)</li>
 *   <li>Case‑insensitive key handling (put, get, containsKey, remove)</li>
 *   <li>Insertion order preservation (iteration over {@code entrySet()})</li>
 *   <li>Behaviour of the protected methods via reflection</li>
 *   <li>Standard {@link Map} contract methods (size, isEmpty, clear, equals, hashCode)</li>
 *   <li>Minus/plus operations inherited from {@link CompactMap}</li>
 * </ul>
 */
public class CompactCILinkedMapTest
{
    private CompactCILinkedMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactCILinkedMap<>();
    }

    /**
     * Verify that the default constructor creates an empty map.
     */
    @Test
    public void testDefaultConstructorCreatesEmptyMap()
    {
        assertTrue("Map should be empty after default construction", map.isEmpty());
        assertEquals("Size must be 0", 0, map.size());
    }

    /**
     * Verify that the copy‑constructor copies all entries from the supplied map.
     */
    @Test
    public void testCopyConstructorCopiesEntries()
    {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("One", 1);
        source.put("Two", 2);
        source.put("Three", 3);

        CompactCILinkedMap<String, Integer> copy = new CompactCILinkedMap<>(source);

        assertEquals("Copy must contain the same number of entries", source.size(), copy.size());
        assertEquals("Value for key \"One\" must be copied", Integer.valueOf(1), copy.get("One"));
        assertEquals("Value for key \"Two\" must be copied", Integer.valueOf(2), copy.get("Two"));
        assertEquals("Value for key \"Three\" must be copied", Integer.valueOf(3), copy.get("Three"));
    }

    /**
     * The map must treat String keys case‑insensitively.
     */
    @Test
    public void testCaseInsensitiveKeyHandling()
    {
        map.put("KeyOne", 10);
        map.put("KEYTWO", 20);
        map.put("keyThree", 30);

        // Retrieval with different cases
        assertEquals(Integer.valueOf(10), map.get("keyone"));
        assertEquals(Integer.valueOf(20), map.get("keytwo"));
        assertEquals(Integer.valueOf(30), map.get("KEYTHREE"));

        // containsKey must be case‑insensitive
        assertTrue(map.containsKey("KEYONE"));
        assertTrue(map.containsKey("keytwo"));
        assertTrue(map.containsKey("KeYtHrEe"));

        // Removing with a different case must succeed
        Integer removed = map.remove("kEyOnE");
        assertEquals(Integer.valueOf(10), removed);
        assertFalse(map.containsKey("keyone"));
        assertEquals(2, map.size());
    }

    /**
     * Non‑String keys should behave like a normal {@link Map}.
     */
    @Test
    public void testNonStringKeysAreNotCaseInsensitive()
    {
        CompactCILinkedMap<Integer, String> intMap = new CompactCILinkedMap<>();
        intMap.put(1, "one");
        intMap.put(2, "two");

        assertEquals("one", intMap.get(1));
        assertNull("Case‑insensitivity does not apply to non‑String keys", intMap.get("1"));
    }

    /**
     * Verify that the map preserves insertion order (LinkedHashMap semantics).
     */
    @Test
    public void testInsertionOrderPreserved()
    {
        map.put("First", 1);
        map.put("Second", 2);
        map.put("Third", 3);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();

        assertTrue(it.hasNext());
        Map.Entry<String, Integer> e1 = it.next();
        assertEquals("First", e1.getKey());
        assertEquals(Integer.valueOf(1), e1.getValue());

        assertTrue(it.hasNext());
        Map.Entry<String, Integer> e2 = it.next();
        assertEquals("Second", e2.getKey());
        assertEquals(Integer.valueOf(2), e2.getValue());

        assertTrue(it.hasNext());
        Map.Entry<String, Integer> e3 = it.next();
        assertEquals("Third", e3.getKey());
        assertEquals(Integer.valueOf(3), e3.getValue());

        assertFalse(it.hasNext());
    }

    /**
     * The {@code isCaseInsensitive()} protected method must return {@code true}.
     */
    @Test
    public void testIsCaseInsensitiveProtectedMethod()
    throws Exception
    {
        Method m = CompactCILinkedMap.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        Boolean result = (Boolean) m.invoke(map);
        assertTrue("isCaseInsensitive() should return true for CompactCILinkedMap", result);
    }

    /**
     * The {@code useCopyIterator()} protected method must return {@code false}.
     */
    @Test
    public void testUseCopyIteratorProtectedMethod()
    throws Exception
    {
        Method m = CompactCILinkedMap.class.getDeclaredMethod("useCopyIterator");
        m.setAccessible(true);
        Boolean result = (Boolean) m.invoke(map);
        assertFalse("useCopyIterator() should return false for CompactCILinkedMap", result);
    }

    /**
     * Verify that {@code getNewMap()} creates a {@link CaseInsensitiveMap}
     * backed by a {@link LinkedHashMap} with the expected initial capacity.
     */
    @Test
    public void testGetNewMapCreatesCorrectInternalStructure()
    throws Exception
    {
        Method m = CompactCILinkedMap.class.getDeclaredMethod("getNewMap");
        m.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Integer> internal = (Map<String, Integer>) m.invoke(map);

        // The returned map must be a CaseInsensitiveMap (cannot refer to the class directly,
        // but we can test its behaviour)
        internal.put("ABC", 123);
        assertEquals(Integer.valueOf(123), internal.get("abc")); // case‑insensitive

        // Verify that the underlying map preserves insertion order
        internal.put("def", 1);
        internal.put("ghi", 2);
        Iterator<String> keyIt = internal.keySet().iterator();
        assertEquals("ABC", keyIt.next()); // first inserted key (original case preserved)
        assertEquals("def", keyIt.next());
        assertEquals("ghi", keyIt.next());
    }

    /**
     * Test the {@code minus} and {@code plus} convenience methods inherited from {@link CompactMap}.
     */
    @Test
    public void testMinusAndPlusOperations()
    {
        map.put("Alpha", 100);
        map.put("Beta", 200);
        map.put("Gamma", 300);

        // minus should return a new map without the specified key (case‑insensitive)
        Map<String, Integer> withoutBeta = map.minus("beta");
        assertEquals(2, withoutBeta.size());
        assertFalse(withoutBeta.containsKey("Beta"));
        assertTrue(withoutBeta.containsKey("Alpha"));
        assertTrue(withoutBeta.containsKey("Gamma"));

        // original map must stay unchanged
        assertEquals(3, map.size());
        assertTrue(map.containsKey("Beta"));

        // plus should return a new map with the additional entry
        Map<String, Integer> plusDelta = map.plus("Delta");
        assertEquals(4, plusDelta.size());
        assertTrue(plusDelta.containsKey("Delta"));
        // The value for the newly added key is null (as per CompactMap.plus contract)
        assertNull(plusDelta.get("Delta"));
    }

    /**
     * Verify {@code equals} and {@code hashCode} follow the Map contract and are
     * case‑insensitive for string keys.
     */
    @Test
    public void testEqualsAndHashCode()
    {
        CompactCILinkedMap<String, Integer> mapA = new CompactCILinkedMap<>();
        CompactCILinkedMap<String, Integer> mapB = new CompactCILinkedMap<>();

        mapA.put("Key", 1);
        mapB.put("key", 1); // different case

        assertEquals("Maps with same entries ignoring case must be equal", mapA, mapB);
        assertEquals("Hash codes must be equal for equal maps", mapA.hashCode(), mapB.hashCode());

        mapB.put("Another", 2);
        assertNotEquals("Maps with different entry sets must not be equal", mapA, mapB);
    }

    /**
     * Ensure {@code clear()} empties the map and that subsequent operations work as expected.
     */
    @Test
    public void testClear()
    {
        map.put("One", 1);
        map.put("Two", 2);
        assertFalse(map.isEmpty());

        map.clear();

        assertTrue("Map must be empty after clear()", map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get("one"));
    }
}
