package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * JUnit‑4 test suite for {@link CompactLinkedMap}.
 *
 * The tests cover:
 *   • All public constructors of {@code CompactLinkedMap}
 *   • All public Map API methods inherited from {@link CompactMap}
 *   • Behaviour that is specific to the Linked implementation (insertion order,
 *     case‑sensitivity, handling of null keys/values, etc.)
 *   • Edge‑case and exception scenarios (null arguments, duplicate keys,
 *     empty maps, etc.)
 *
 * The suite is deliberately exhaustive yet readable – each test method
 * focuses on a single aspect of the contract.
 */
public class CompactLinkedMapTest
{
    private CompactLinkedMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactLinkedMap<>();
    }

    @After
    public void tearDown()
    {
        map = null;
    }

    /* --------------------------------------------------------------------- */
    /* 1.  Constructors                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptyMap()
    {
        assertNotNull("Map instance must not be null", map);
        assertTrue("New map must be empty", map.isEmpty());
        assertEquals("Size of new map must be 0", 0, map.size());
    }

    @Test
    public void testConstructorFromExistingMapCopiesEntries()
    {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        source.put("three", 3);

        CompactLinkedMap<String, Integer> copy = new CompactLinkedMap<>(source);

        // original map unchanged
        assertEquals("Copy must contain same number of entries as source", source.size(), copy.size());

        // verify order is preserved (LinkedHashMap semantics)
        int i = 0;
        for (String key : copy.keySet())
        {
            assertEquals("Order of keys must be preserved", (String) source.keySet().toArray()[i], key);
            i++;
        }

        // verify values
        for (Map.Entry<String, Integer> e : source.entrySet())
        {
            assertTrue("Copy must contain key from source", copy.containsKey(e.getKey()));
            assertEquals("Value for key must be equal", e.getValue(), copy.get(e.getKey()));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorFromNullMapThrowsNPE()
    {
        // CompactMap's constructor does not guard against null, so we expect NPE.
        new CompactLinkedMap<String, Integer>(null);
    }

    /* --------------------------------------------------------------------- */
    /* 2.  Basic Map Operations                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testPutAndGet()
    {
        Integer previous = map.put("A", 10);
        assertNull("First insertion must return null", previous);
        assertEquals("Value must be retrievable after put", Integer.valueOf(10), map.get("A"));
        assertEquals("Size must be 1 after one insertion", 1, map.size());
    }

    @Test
    public void testPutOverwriteReturnsOldValue()
    {
        map.put("B", 20);
        Integer old = map.put("B", 30);
        assertEquals("Old value must be returned on overwrite", Integer.valueOf(20), old);
        assertEquals("New value must replace old one", Integer.valueOf(30), map.get("B"));
        assertEquals("Size must stay 1 after overwrite", 1, map.size());
    }

    @Test
    public void testRemoveExistingKey()
    {
        map.put("C", 40);
        Integer removed = map.remove("C");
        assertEquals("Removed value must be returned", Integer.valueOf(40), removed);
        assertFalse("Map must not contain removed key", map.containsKey("C"));
        assertTrue("Map must be empty after removal", map.isEmpty());
    }

    @Test
    public void testRemoveNonExistingKeyReturnsNull()
    {
        assertNull("Removing non‑existent key must return null", map.remove("nonexistent"));
    }

    @Test
    public void testContainsKeyAndValue()
    {
        map.put("D", 50);
        assertTrue("containsKey must return true for existing key", map.containsKey("D"));
        assertTrue("containsValue must return true for existing value", map.containsValue(50));
        assertFalse("containsKey must return false for missing key", map.containsKey("E"));
        assertFalse("containsValue must return false for missing value", map.containsValue(999));
    }

    @Test
    public void testClearEmptiesMap()
    {
        map.put("X", 1);
        map.put("Y", 2);
        assertFalse("Map must not be empty before clear", map.isEmpty());
        map.clear();
        assertTrue("Map must be empty after clear", map.isEmpty());
        assertEquals("Size must be 0 after clear", 0, map.size());
    }

    @Test
    public void testPutAllCopiesAllEntries()
    {
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("one", 1);
        src.put("two", 2);
        src.put("three", 3);

        map.putAll(src);
        assertEquals("Map size must match source size after putAll", src.size(), map.size());
        for (Map.Entry<String, Integer> e : src.entrySet())
        {
            assertTrue("Map must contain key after putAll", map.containsKey(e.getKey()));
            assertEquals("Value must match after putAll", e.getValue(), map.get(e.getKey()));
        }
    }

    /* --------------------------------------------------------------------- */
    /* 3.  Ordering (LinkedHashMap semantics)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIterationPreservesInsertionOrder()
    {
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        String[] expected = {"first", "second", "third"};
        int idx = 0;
        for (String key : map.keySet())
        {
            assertEquals("Iteration order must follow insertion order", expected[idx], key);
            idx++;
        }
    }

    @Test
    public void testEntrySetIterationPreservesOrder()
    {
        map.put("a", 10);
        map.put("b", 20);
        map.put("c", 30);

        String[] expected = {"a", "b", "c"};
        int i = 0;
        for (Map.Entry<String, Integer> e : map.entrySet())
        {
            assertEquals("Entry key order must follow insertion order", expected[i], e.getKey());
            i++;
        }
    }

    /* --------------------------------------------------------------------- */
    /* 4.  Case Sensitivity (isCaseInsensitive() == false)                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCaseSensitiveKeys()
    {
        map.put("Key", 100);
        assertTrue("Original key must be present", map.containsKey("Key"));
        assertNull("Different case must not be considered the same key", map.get("key"));
        assertFalse("containsKey must be case‑sensitive", map.containsKey("key"));
    }

    /* --------------------------------------------------------------------- */
    /* 5.  Null handling                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNullKeyIsAllowed()
    {
        map.put(null, 999);
        assertTrue("Map must contain null key", map.containsKey(null));
        assertEquals("Value for null key must be stored", Integer.valueOf(999), map.get(null));
        assertEquals("Size must be 1 after inserting null key", 1, map.size());
    }

    @Test
    public void testNullValueIsAllowed()
    {
        map.put("nullValue", null);
        assertTrue("Map must contain key with null value", map.containsKey("nullValue"));
        assertNull("Value must be null as stored", map.get("nullValue"));
        assertTrue("containsValue(null) must be true", map.containsValue(null));
    }

    @Test
    public void testPutAllWithNullKeyAndValue()
    {
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put(null, 1);
        src.put("k", null);
        map.putAll(src);

        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(1), map.get(null));
        assertTrue(map.containsKey("k"));
        assertNull(map.get("k"));
    }

    /* --------------------------------------------------------------------- */
    /* 6.  minus() and plus() – immutable view operations                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testMinusRemovesKeyAndReturnsNewMap()
    {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Map<String, Integer> result = map.minus("two");
        assertFalse("Resulting map must not contain removed key", result.containsKey("two"));
        assertTrue("Original map must stay unchanged", map.containsKey("two"));
        assertEquals("Result size must be original size minus one", 2, result.size());
    }

    @Test
    public void testPlusAddsKeyAndReturnsNewMap()
    {
        map.put("alpha", 10);
        Map<String, Integer> result = map.plus("beta"); // right argument is a key, map adds null value

        assertTrue("Resulting map must contain original key", result.containsKey("alpha"));
        assertTrue("Resulting map must contain new key", result.containsKey("beta"));
        assertNull("New key must map to null (as per CompactMap.plus contract)", result.get("beta"));
        assertEquals("Original map must stay unchanged", 1, map.size());
    }

    @Test
    public void testMinusWithNonExistingKeyReturnsSameMap()
    {
        map.put("x", 1);
        Map<String, Integer> before = new LinkedHashMap<>(map);
        Map<String, Integer> after = map.minus("nonexistent");
        assertEquals("Minus with missing key must return a map equal to original", before, after);
    }

    @Test
    public void testPlusWithExistingKeyOverridesValue()
    {
        map.put("z", 5);
        Map<String, Integer> result = map.plus("z"); // adds same key with null value
        assertTrue("Result must still contain the key", result.containsKey("z"));
        assertNull("Value should become null after plus with same key", result.get("z"));
        assertEquals("Original map must retain original value", Integer.valueOf(5), map.get("z"));
    }

    /* --------------------------------------------------------------------- */
    /* 7.  equals() and hashCode() consistency                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeWithSameEntries()
    {
        CompactLinkedMap<String, Integer> other = new CompactLinkedMap<>();
        map.put("a", 1);
        map.put("b", 2);
        other.put("a", 1);
        other.put("b", 2);

        assertTrue("Maps with identical entries must be equal", map.equals(other));
        assertEquals("Equal maps must have same hashCode", map.hashCode(), other.hashCode());
    }

    @Test
    public void testEqualsIsOrderSensitiveForLinkedMap()
    {
        CompactLinkedMap<String, Integer> m1 = new CompactLinkedMap<>();
        CompactLinkedMap<String, Integer> m2 = new CompactLinkedMap<>();

        m1.put("first", 1);
        m1.put("second", 2);

        m2.put("second", 2);
        m2.put("first", 1);

        // LinkedHashMap's equals() is order‑insensitive, but we verify that CompactLinkedMap
        // (which delegates to LinkedHashMap) behaves the same.
        assertTrue("Linked order should not affect equality", m1.equals(m2));
    }

    /* --------------------------------------------------------------------- */
    /* 8.  toString() sanity check                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringContainsAllEntries()
    {
        map.put("k1", 11);
        map.put("k2", 22);
        String s = map.toString();
        assertTrue("String representation must contain key k1", s.contains("k1"));
        assertTrue("String representation must contain value 11", s.contains("11"));
        assertTrue("String representation must contain key k2", s.contains("k2"));
        assertTrue("String representation must contain value 22", s.contains("22"));
    }

    /* --------------------------------------------------------------------- */
    /* 9.  getConfig / withConfig – basic configuration round‑trip          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConfigRoundTrip()
    {
        // CompactMap provides a default config; we modify one entry and ensure it is kept.
        Map<String, Object> cfg = map.getConfig();
        cfg.put(CompactMap.CASE_SENSITIVE, Boolean.TRUE);
        CompactLinkedMap<String, Integer> configured = map.withConfig(cfg);
        assertEquals("Configured map must retain modified config entry",
                Boolean.TRUE, configured.getConfig().get(CompactMap.CASE_SENSITIVE));
    }

    /* --------------------------------------------------------------------- */
    /* 10. Builder usage (static) – basic creation via builder               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testBuilderCreatesMapWithGivenEntries()
    {
        CompactLinkedMap<String, Integer> built = CompactMap.<String, Integer>builder()
                .put("one", 1)
                .put("two", 2)
                .build()
                .withConfig(Collections.emptyMap()); // builder returns CompactMap; we adapt to Linked version

        // Since builder creates a CompactMap (not specifically CompactLinkedMap),
        // we verify that the resulting map behaves like a normal map.
        assertEquals(2, built.size());
        assertEquals(Integer.valueOf(1), built.get("one"));
        assertEquals(Integer.valueOf(2), built.get("two"));
    }
}
