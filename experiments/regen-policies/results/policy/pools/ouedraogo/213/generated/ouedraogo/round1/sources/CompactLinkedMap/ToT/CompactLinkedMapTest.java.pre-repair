package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactLinkedMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>basic construction and emptiness</li>
 *   <li>put / get semantics, including null keys/values</li>
 *   <li>insertion order preservation (LinkedHashMap behaviour)</li>
 *   <li>copy‑constructor behaviour</li>
 *   <li>case‑sensitivity (the map must be case‑sensitive)</li>
 *   <li>remove, putAll, clear, minus and plus helpers</li>
 *   <li>configuration API (getConfig / withConfig)</li>
 *   <li>exposure of protected members via a test‑only subclass</li>
 * </ul>
 *
 * Only public API (or protected members accessed through a test subclass) is used,
 * adhering to the constraint of not mocking or subclassing abstract library types.
 */
@RunWith(JUnit4.class)
public class CompactLinkedMapTest
{
    private CompactLinkedMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactLinkedMap<>();
    }

    /*** 1. Construction & emptiness ***/
    @Test
    public void testEmptyConstruction()
    {
        assertTrue("New map must be empty", map.isEmpty());
        assertEquals("Size of new map must be 0", 0, map.size());
    }

    /*** 2. Simple put / get ***/
    @Test
    public void testPutAndGet()
    {
        assertNull("Getting a non‑existent key returns null", map.get("a"));
        map.put("a", 1);
        assertEquals("Value associated with key 'a' must be 1", Integer.valueOf(1), map.get("a"));
        assertTrue("containsKey must be true after put", map.containsKey("a"));
        assertTrue("containsValue must be true after put", map.containsValue(1));
        assertEquals("Map size must be 1 after a single put", 1, map.size());
    }

    /*** 3. Null key handling (LinkedHashMap permits null) ***/
    @Test
    public void testNullKey()
    {
        map.put(null, 42);
        assertTrue("Map must contain the null key", map.containsKey(null));
        assertEquals("Value for null key must be 42", Integer.valueOf(42), map.get(null));
        assertEquals("Size must be 1 after inserting null key", 1, map.size());

        map.remove(null);
        assertFalse("Map must not contain null key after removal", map.containsKey(null));
        assertEquals("Size must be 0 after removing null key", 0, map.size());
    }

    /*** 4. Overwrite existing key – size unchanged, value updated ***/
    @Test
    public void testOverwriteKey()
    {
        map.put("x", 10);
        map.put("x", 20); // overwrite
        assertEquals("Value must be overwritten to 20", Integer.valueOf(20), map.get("x"));
        assertEquals("Size must stay 1 after overwriting a key", 1, map.size());
    }

    /*** 5. Insertion order is preserved (LinkedHashMap semantics) ***/
    @Test
    public void testInsertionOrderPreserved()
    {
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        // The keySet iterator of LinkedHashMap returns keys in insertion order.
        Set<String> keys = map.keySet();
        String[] expected = {"first", "second", "third"};
        int i = 0;
        for (String k : keys)
        {
            assertEquals("Insertion order must be preserved", expected[i++], k);
        }
    }

    /*** 6. Constructor that copies another map ***/
    @Test
    public void testCopyConstructor()
    {
        // Use a LinkedHashMap as source to have a deterministic iteration order.
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("alpha", 100);
        source.put("beta", 200);
        source.put("gamma", 300);

        CompactLinkedMap<String, Integer> copy = new CompactLinkedMap<>(source);
        assertEquals("Copy must have same size as source", source.size(), copy.size());

        // Verify that all entries are present and order is identical.
        int idx = 0;
        String[] expectedOrder = {"alpha", "beta", "gamma"};
        for (String key : copy.keySet())
        {
            assertEquals("Key order must match source map", expectedOrder[idx], key);
            assertEquals("Value must match source map", source.get(key), copy.get(key));
            idx++;
        }
    }

    /*** 7. Case‑sensitivity – map must be case‑sensitive ***/
    @Test
    public void testCaseSensitivity()
    {
        map.put("Key", 123);
        assertTrue("Original key must be found", map.containsKey("Key"));
        assertFalse("Different case must NOT be found", map.containsKey("key"));
        assertNull("Lookup with different case must return null", map.get("key"));
    }

    /*** 8. remove() behaviour for existing and non‑existing keys ***/
    @Test
    public void testRemove()
    {
        map.put("a", 1);
        map.put("b", 2);
        Integer removed = map.remove("a");
        assertEquals("remove must return the previous value", Integer.valueOf(1), removed);
        assertFalse("Key 'a' must no longer be present", map.containsKey("a"));
        assertEquals("Size must decrease after removal", 1, map.size());

        // Removing a non‑existent key should return null and not affect size.
        assertNull("Removing absent key must return null", map.remove("nonexistent"));
        assertEquals("Size must stay unchanged after removing absent key", 1, map.size());
    }

    /*** 9. putAll() and clear() ***/
    @Test
    public void testPutAllAndClear()
    {
        Map<String, Integer> other = new LinkedHashMap<>();
        other.put("one", 1);
        other.put("two", 2);
        other.put("three", 3);

        map.putAll(other);
        assertEquals("Map size must reflect putAll", other.size(), map.size());
        assertTrue("All keys must be present after putAll", map.keySet().containsAll(other.keySet()));

        map.clear();
        assertTrue("Map must be empty after clear()", map.isEmpty());
        assertEquals("Size must be 0 after clear()", 0, map.size());
    }

    /*** 10. minus() – returns a new map without the specified key ***/
    @Test
    public void testMinus()
    {
        map.put("keep", 1);
        map.put("removeMe", 2);
        map.put("alsoKeep", 3);

        Map<String, Integer> result = map.minus("removeMe");
        assertFalse("Resulting map must not contain the removed key", result.containsKey("removeMe"));
        assertTrue("Other keys must still be present", result.containsKey("keep"));
        assertTrue("Other keys must still be present", result.containsKey("alsoKeep"));
        assertEquals("Resulting map size must be original size minus one", 2, result.size());
    }

    /*** 11. plus() – returns a new map that contains all entries of this map plus the supplied map ***/
    @Test
    public void testPlus()
    {
        map.put("a", 1);
        map.put("b", 2);

        Map<String, Integer> extra = new LinkedHashMap<>();
        extra.put("c", 3);
        extra.put("d", 4);

        Map<String, Integer> result = map.plus(extra);
        assertEquals("Resulting map must contain all original entries plus extras", 4, result.size());
        assertTrue("Result must contain key 'a'", result.containsKey("a"));
        assertTrue("Result must contain key 'c'", result.containsKey("c"));
        assertEquals("Value for 'c' must be 3", Integer.valueOf(3), result.get("c"));
    }

    /*** 12. getLogicalValueType() – basic sanity check (non‑null) ***/
    @Test
    public void testLogicalValueType()
    {
        assertNotNull("LogicalValueType must not be null", map.getLogicalValueType());
    }

    /*** 13. Configuration API – getConfig() returns a mutable map, withConfig() returns a new map ***/
    @Test
    public void testConfiguration()
    {
        Map<String, Object> cfg = map.getConfig();
        assertNotNull("Configuration map must be non‑null", cfg);
        int originalSize = cfg.size();

        // Change a known config entry (e.g., CASE_SENSITIVE) and verify the new map reflects it.
        cfg.put(CompactMap.CASE_SENSITIVE, Boolean.TRUE);
        CompactLinkedMap<String, Integer> withCfg = map.withConfig(cfg);
        assertEquals("New map must have same config size", cfg.size(), withCfg.getConfig().size());
        assertEquals("CASE_SENSITIVE must be true in new map", Boolean.TRUE,
                withCfg.getConfig().get(CompactMap.CASE_SENSITIVE));
        // Original map's config should remain unchanged (immutable contract of withConfig)
        assertEquals("Original map config should stay unchanged", originalSize, map.getConfig().size());
    }

    /*** 14. Protected methods exposure – ensure getNewMap() creates a LinkedHashMap with expected initial capacity ***/
    @Test
    public void testProtectedGetNewMap()
    {
        // Expose protected methods via an anonymous subclass.
        CompactLinkedMap<String, Integer> probe = new CompactLinkedMap<String, Integer>()
        {
            @Override
            public Map<String, Integer> getNewMap()
            {
                // Call the super implementation and expose it.
                return super.getNewMap();
            }

            @Override
            protected int compactSize()
            {
                // Force a known compact size to make the test deterministic.
                return 5;
            }
        };

        Map<String, Integer> internal = probe.getNewMap();
        assertTrue("The internal map created by getNewMap must be a LinkedHashMap",
                internal instanceof LinkedHashMap);
        // The initial capacity of LinkedHashMap is (compactSize + 1) as per implementation.
        // Unfortunately LinkedHashMap does not expose its capacity, but we can verify that
        // the map is empty and ready for insertion.
        assertTrue("New map must be empty initially", internal.isEmpty());
        internal.put("x", 99);
        assertEquals("Inserted entry must be retrievable", Integer.valueOf(99), internal.get("x"));
    }
}
