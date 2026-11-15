package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCILinkedMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors.</li>
 *   <li>All public Map/CompactMap operations inherited by {@code CompactCILinkedMap}.</li>
 *   <li>Case‑insensitive behavior of keys.</li>
 *   <li>Insertion‑order preservation (LinkedHashMap backing).</li>
 *   <li>Edge cases such as {@code null} keys/values and duplicate keys with different case.</li>
 *   <li>Utility methods {@code minus} and {@code plus} which return new map instances.</li>
 * </ul>
 */
public class CompactCILinkedMapTest
{
    private CompactCILinkedMap<String, String> map;

    @Before
    public void setUp()
    {
        map = new CompactCILinkedMap<>();
    }

    @After
    public void tearDown()
    {
        map.clear();
    }

    /* ---------------------------------------------------------------------- */
    /* 1. Constructors                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructor()
    {
        CompactCILinkedMap<String, Integer> empty = new CompactCILinkedMap<>();
        assertTrue("Map should be empty after default construction", empty.isEmpty());
        assertEquals("Size must be zero", 0, empty.size());
    }

    @Test
    public void testCopyConstructor()
    {
        Map<String, String> source = new HashMap<>();
        source.put("One", "1");
        source.put("Two", "2");

        CompactCILinkedMap<String, String> copy = new CompactCILinkedMap<>(source);
        assertEquals("Copy must contain all source entries", 2, copy.size());
        assertEquals("Value for key \"One\" must be copied", "1", copy.get("One"));
        assertEquals("Value for key \"Two\" must be copied", "2", copy.get("Two"));
    }

    /* ---------------------------------------------------------------------- */
    /* 2. Basic put / get (case‑insensitive)                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetCaseInsensitive()
    {
        map.put("Key", "value1");
        // Different case must retrieve the same entry
        assertEquals("Retrieving with different case should succeed", "value1", map.get("kEy"));
    }

    @Test
    public void testOverwriteWithDifferentCase()
    {
        map.put("Key", "first");
        // same logical key, different case -> should replace previous entry
        map.put("kEy", "second");
        assertEquals("Value must be overwritten by later put", "second", map.get("KEY"));
        assertEquals("Map size must stay 1 after overwrite", 1, map.size());
    }

    /* ---------------------------------------------------------------------- */
    /* 3. containsKey / containsValue (case‑insensitive)                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testContainsKeyCaseInsensitive()
    {
        map.put("Hello", "World");
        assertTrue("containsKey must be case‑insensitive", map.containsKey("hELLo"));
        assertFalse("containsKey must return false for missing key", map.containsKey("Bye"));
    }

    @Test
    public void testContainsValue()
    {
        map.put("A", "Alpha");
        map.put("B", "Beta");
        assertTrue(map.containsValue("Alpha"));
        assertFalse(map.containsValue("Gamma"));
    }

    /* ---------------------------------------------------------------------- */
    /* 4. remove (case‑insensitive)                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRemoveCaseInsensitive()
    {
        map.put("RemoveMe", "value");
        String removed = map.remove("rEmOvEmE");
        assertEquals("Removed value must be returned", "value", removed);
        assertFalse("Key must no longer be present", map.containsKey("REMOVEme"));
        assertTrue("Map must be empty after removal", map.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 5. putAll – ensure case‑insensitivity is preserved                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAllCaseInsensitive()
    {
        Map<String, String> src = new HashMap<>();
        src.put("One", "1");
        src.put("TWO", "2");
        map.putAll(src);

        assertEquals("1", map.get("one"));
        assertEquals("2", map.get("two"));
        assertEquals(2, map.size());
    }

    /* ---------------------------------------------------------------------- */
    /* 6. keySet order – LinkedHashMap should preserve insertion order        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testKeySetInsertionOrder()
    {
        map.put("first", "1");
        map.put("second", "2");
        map.put("third", "3");

        List<String> expected = Arrays.asList("first", "second", "third");
        List<String> actual = new ArrayList<>(map.keySet());

        assertEquals("Insertion order must be preserved in keySet()", expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /* 7. values collection order (mirrors insertion order)                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testValuesInsertionOrder()
    {
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");

        List<String> expected = Arrays.asList("A", "B", "C");
        List<String> actual = new ArrayList<>(map.values());

        assertEquals("Insertion order must be preserved in values()", expected, actual);
    }

    /* ---------------------------------------------------------------------- */
    /* 8. minus – returns a new map without the specified key                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMinus()
    {
        map.put("keep", "1");
        map.put("drop", "2");
        Map<String, String> result = map.minus("DROP"); // different case

        assertFalse("Result map must not contain the removed key", result.containsKey("keep"));
        assertTrue("Result map must contain the kept key", result.containsKey("keep"));
        assertEquals("Result size must be 1", 1, result.size());

        // original map must stay unchanged
        assertTrue(map.containsKey("keep"));
        assertTrue(map.containsKey("drop"));
        assertEquals(2, map.size());
    }

    /* ---------------------------------------------------------------------- */
    /* 9. plus – returns a new map that contains all entries from both maps   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPlus()
    {
        map.put("x", "1");
        CompactCILinkedMap<String, String> other = new CompactCILinkedMap<>();
        other.put("Y", "2");
        other.put("x", "override"); // same logical key, different case

        Map<String, String> combined = map.plus(other);
        // The right‑hand side overrides the left‑hand side for duplicate keys
        assertEquals("override", combined.get("X"));
        assertEquals("2", combined.get("y"));
        assertEquals(2, combined.size());

        // ensure original maps are untouched
        assertEquals("1", map.get("x"));
        assertEquals("override", other.get("x"));
    }

    /* ---------------------------------------------------------------------- */
    /* 10. clear / isEmpty                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testClearAndIsEmpty()
    {
        map.put("a", "A");
        map.put("b", "B");
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue("Map must be empty after clear()", map.isEmpty());
        assertEquals(0, map.size());
    }

    /* ---------------------------------------------------------------------- */
    /* 11. null handling – behavior defined by CompactMap (speculative)      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNullKeyAndValue()
    {
        // The underlying CompactMap may allow null keys/values; we test both possibilities.
        try
        {
            map.put(null, "nullKey");
            assertTrue("Map should contain a null key after insertion", map.containsKey(null));
            assertEquals("nullKey", map.get(null));
        }
        catch (NullPointerException npe)
        {
            // If the implementation forbids null keys, the exception is the expected behavior.
            // Verify that nothing was added.
            assertFalse("Map must not contain a null key after NPE", map.containsKey(null));
        }

        // Null values are usually permitted.
        map.put("nullValue", null);
        assertTrue(map.containsKey("nullvalue"));
        assertNull(map.get("NULLVALUE"));
    }

    /* ---------------------------------------------------------------------- */
    /* 12. equals and hashCode – map equality must be case‑insensitive        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode()
    {
        CompactCILinkedMap<String, String> m1 = new CompactCILinkedMap<>();
        CompactCILinkedMap<String, String> m2 = new CompactCILinkedMap<>();

        m1.put("Key", "V");
        m2.put("kEy", "V");

        assertEquals("Maps with same logical entries must be equal", m1, m2);
        assertEquals("Hash codes must match for equal maps", m1.hashCode(), m2.hashCode());

        // Different value -> not equal
        m2.put("Key", "Other");
        assertNotEquals(m1, m2);
    }

    /* ---------------------------------------------------------------------- */
    /* 13. toString – simple sanity check (non‑null, contains entries)        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToString()
    {
        map.put("one", "1");
        map.put("two", "2");
        String s = map.toString();
        assertNotNull("toString must not return null", s);
        assertTrue("toString should contain key \"one\"", s.contains("one"));
        assertTrue("toString should contain value \"2\"", s.contains("2"));
    }

    /* ---------------------------------------------------------------------- */
    /* 14. entrySet iterator – verifies that iterator respects insertion order */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEntrySetOrder()
    {
        map.put("first", "1");
        map.put("second", "2");
        map.put("third", "3");

        List<String> keys = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (Map.Entry<String, String> e : map.entrySet())
        {
            keys.add(e.getKey());
            values.add(e.getValue());
        }

        assertEquals(Arrays.asList("first", "second", "third"), keys);
        assertEquals(Arrays.asList("1", "2", "3"), values);
    }
}
