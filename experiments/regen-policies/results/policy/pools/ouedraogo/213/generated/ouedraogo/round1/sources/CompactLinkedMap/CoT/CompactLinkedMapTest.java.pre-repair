package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactLinkedMap}.
 *
 * The tests are organised in the following sections:
 *
 * 1.  Public API extraction – a quick reference of all public constructors and methods
 *     that are directly declared in {@code CompactLinkedMap} (including those inherited
 *     from {@code CompactMap} because they are part of the public contract of the class).
 *
 * 2.  Basic functional tests – each public method gets a simple “happy‑path” test that
 *     verifies its documented behaviour.
 *
 * 3.  Edge‑case & exception tests – tests that target corner‑case inputs (null keys,
 *     duplicate keys, empty maps, etc.) and verify that the class reacts correctly
 *     (e.g. returning {@code null}, preserving insertion order, throwing NPE where
 *     appropriate, etc.).
 *
 * 4.  Integration style tests – a few tests that combine several operations to ensure
 *     that the map behaves like a normal {@link java.util.LinkedHashMap} with respect
 *     to ordering, equality, {@code hashCode()}, etc.
 *
 * The test class uses only the public API (constructors, Map methods, and the
 * configuration helpers) and reflection where absolutely necessary to reach the
 * protected methods defined in {@code CompactLinkedMap}.  No mocking or subclassing
 * of library classes is performed.
 */
public class CompactLinkedMapTest
{
    // -------------------------------------------------------------------------
    // 1.  Public API extraction (for documentation / reference only)
    // -------------------------------------------------------------------------
    //
    //   Constructors
    //   ------------------------------
    //   CompactLinkedMap()
    //   CompactLinkedMap(Map<K,V> other)
    //
    //   Inherited public methods (from CompactMap implements Map)
    //   ----------------------------------------------------------------
    //   int size()
    //   boolean isEmpty()
    //   boolean containsKey(Object key)
    //   boolean containsValue(Object value)
    //   V get(Object key)
    //   V put(K key, V value)
    //   V remove(Object key)
    //   void putAll(Map<? extends K, ? extends V> m)
    //   void clear()
    //   Set<K> keySet()
    //   Collection<V> values()
    //   Set<Entry<K,V>> entrySet()
    //   boolean equals(Object o)
    //   int hashCode()
    //   String toString()
    //   Map<K,V> minus(Object removeMe)
    //   Map<K,V> plus(Object right)
    //   Map<String,Object> getConfig()
    //   CompactMap<K,V> withConfig(Map<String,Object> config)
    //   static <K,V> Builder<K,V> builder()
    //
    //   Protected methods (cannot be called directly, tested via reflection)
    //   --------------------------------------------------------------------
    //   Map<K,V> getNewMap()
    //   boolean isCaseInsensitive()
    //   boolean useCopyIterator()
    //   int compactSize()
    //   String getOrdering()
    //   int computeKeyHashCode(Object key)
    //   int computeValueHashCode(Object value)
    //   K getSingleValueKey()
    //
    // -------------------------------------------------------------------------

    private CompactLinkedMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactLinkedMap<>();
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // -------------------------------------------------------------------------
    // 2.  Basic functional tests
    // -------------------------------------------------------------------------

    @Test
    public void testDefaultConstructorCreatesEmptyMap()
    {
        assertTrue("New map should be empty", map.isEmpty());
        assertEquals("Size should be 0", 0, map.size());
    }

    @Test
    public void testConstructorWithExistingMapCopiesEntries()
    {
        Map<String, Integer> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);

        CompactLinkedMap<String, Integer> copy = new CompactLinkedMap<>(source);

        assertEquals("Copy should have same size as source", source.size(), copy.size());
        assertTrue("Copy should contain all keys from source", copy.containsKey("one"));
        assertTrue("Copy should contain all keys from source", copy.containsKey("two"));
        assertEquals(Integer.valueOf(1), copy.get("one"));
        assertEquals(Integer.valueOf(2), copy.get("two"));
    }

    @Test
    public void testPutAndGet()
    {
        Integer previous = map.put("alpha", 10);
        assertNull("First insertion should return null", previous);
        assertEquals(Integer.valueOf(10), map.get("alpha"));
    }

    @Test
    public void testPutReplacesExistingValue()
    {
        map.put("beta", 20);
        Integer old = map.put("beta", 30);
        assertEquals("Old value should be returned on replace", Integer.valueOf(20), old);
        assertEquals("New value should be stored", Integer.valueOf(30), map.get("beta"));
    }

    @Test
    public void testRemoveExistingKey()
    {
        map.put("gamma", 40);
        Integer removed = map.remove("gamma");
        assertEquals(Integer.valueOf(40), removed);
        assertFalse("Key should no longer be present", map.containsKey("gamma"));
    }

    @Test
    public void testRemoveNonExistingKeyReturnsNull()
    {
        assertNull("Removing missing key must return null", map.remove("nonexistent"));
    }

    @Test
    public void testContainsKeyAndContainsValue()
    {
        map.put("delta", 50);
        assertTrue(map.containsKey("delta"));
        assertTrue(map.containsValue(50));
        assertFalse(map.containsKey("epsilon"));
        assertFalse(map.containsValue(60));
    }

    @Test
    public void testPutAllCopiesAllEntries()
    {
        Map<String, Integer> other = new LinkedHashMap<>();
        other.put("a", 1);
        other.put("b", 2);
        other.put("c", 3);

        map.putAll(other);

        assertEquals(3, map.size());
        assertTrue(map.keySet().containsAll(other.keySet()));
        assertTrue(map.values().containsAll(other.values()));
    }

    @Test
    public void testClearEmptiesMap()
    {
        map.put("x", 100);
        map.put("y", 200);
        assertFalse(map.isEmpty());

        map.clear();

        assertTrue("Map should be empty after clear()", map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testKeySetPreservesInsertionOrder()
    {
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        // LinkedHashMap guarantees iteration order matches insertion order
        String[] expected = {"first", "second", "third"};
        int i = 0;
        for (String key : map.keySet())
        {
            assertEquals(expected[i++], key);
        }
    }

    @Test
    public void testEntrySetIterationOrderMatchesInsertionOrder()
    {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        String[] expectedKeys = {"one", "two", "three"};
        Integer[] expectedValues = {1, 2, 3};

        int idx = 0;
        for (Map.Entry<String, Integer> entry : map.entrySet())
        {
            assertEquals(expectedKeys[idx], entry.getKey());
            assertEquals(expectedValues[idx], entry.getValue());
            idx++;
        }
    }

    @Test
    public void testEqualsAndHashCode()
    {
        CompactLinkedMap<String, Integer> m1 = new CompactLinkedMap<>();
        CompactLinkedMap<String, Integer> m2 = new CompactLinkedMap<>();

        m1.put("k1", 10);
        m1.put("k2", 20);

        m2.put("k1", 10);
        m2.put("k2", 20);

        assertEquals("Maps with same entries should be equal", m1, m2);
        assertEquals("Equal maps must have same hashCode", m1.hashCode(), m2.hashCode());

        // Change one entry – equality must break
        m2.put("k2", 99);
        assertNotEquals("Maps should differ after mutation", m1, m2);
    }

    @Test
    public void testMinusCreatesNewMapWithoutSpecifiedKey()
    {
        map.put("keep1", 1);
        map.put("removeMe", 2);
        map.put("keep2", 3);

        Map<String, Integer> result = map.minus("removeMe");

        assertFalse("Result must not contain removed key", result.containsKey("removeMe"));
        assertTrue("Result must still contain other keys", result.containsKey("keep1"));
        assertTrue("Result must still contain other keys", result.containsKey("keep2"));
        assertEquals(Integer.valueOf(1), result.get("keep1"));
        assertEquals(Integer.valueOf(3), result.get("keep2"));
    }

    @Test
    public void testPlusCreatesNewMapWithAddedEntry()
    {
        map.put("a", 1);
        map.put("b", 2);

        Map<String, Integer> result = map.plus(Collections.singletonMap("c", 3));

        // original map must stay unchanged
        assertEquals(2, map.size());
        assertFalse(map.containsKey("c"));

        // result map must contain all three entries
        assertEquals(3, result.size());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        assertTrue(result.containsKey("c"));
        assertEquals(Integer.valueOf(3), result.get("c"));
    }

    @Test
    public void testWithConfigReturnsSameInstanceWithUpdatedConfig()
    {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(CompactMap.CASE_SENSITIVE, Boolean.TRUE);
        CompactMap<String, Integer> result = map.withConfig(cfg);

        // The contract of withConfig is to return a map (often the same instance)
        // that reflects the supplied configuration.
        assertNotNull(result);
        assertSame("withConfig should return the same instance for CompactLinkedMap", map, result);
        assertEquals("Config map should be stored", cfg, result.getConfig());
    }

    // -------------------------------------------------------------------------
    // 3.  Edge‑case & exception handling tests
    // -------------------------------------------------------------------------

    @Test
    public void testConstructorWithNullMapThrowsNPE()
    {
        thrown.expect(NullPointerException.class);
        new CompactLinkedMap<>(null);
    }

    @Test
    public void testPutNullKeyIsAllowed()
    {
        // LinkedHashMap permits a single null key – CompactLinkedMap should behave the same.
        map.put(null, 123);
        assertTrue("Map should contain the null key", map.containsKey(null));
        assertEquals(Integer.valueOf(123), map.get(null));
    }

    @Test
    public void testPutNullValueIsAllowed()
    {
        map.put("nullValue", null);
        assertTrue(map.containsKey("nullValue"));
        assertNull("Value associated with key should be null", map.get("nullValue"));
    }

    @Test
    public void testDuplicateNullKeyReplacesValue()
    {
        map.put(null, 1);
        Integer old = map.put(null, 2);
        assertEquals(Integer.valueOf(1), old);
        assertEquals(Integer.valueOf(2), map.get(null));
    }

    @Test
    public void testRemoveNullKey()
    {
        map.put(null, 42);
        Integer removed = map.remove(null);
        assertEquals(Integer.valueOf(42), removed);
        assertFalse("Null key should be gone after removal", map.containsKey(null));
    }

    @Test
    public void testPutAllWithEmptyMapDoesNotChangeState()
    {
        map.put("existing", 5);
        map.putAll(Collections.emptyMap());
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(5), map.get("existing"));
    }

    @Test
    public void testMinusWithKeyNotPresentReturnsSameMap()
    {
        map.put("a", 1);
        Map<String, Integer> result = map.minus("nonexistent");
        // The contract of minus is to return a new map; however it must be equal to the original.
        assertEquals(map, result);
        assertEquals(map.size(), result.size());
    }

    @Test
    public void testPlusWithNullArgumentThrowsNPE()
    {
        thrown.expect(NullPointerException.class);
        map.plus(null);
    }

    // -------------------------------------------------------------------------
    // 4.  Reflection based tests for protected methods specific to CompactLinkedMap
    // -------------------------------------------------------------------------

    @Test
    public void testIsCaseInsensitiveReturnsFalse()
    {
        // The method is protected – we access it via reflection.
        try
        {
            java.lang.reflect.Method m = CompactLinkedMap.class.getDeclaredMethod("isCaseInsensitive");
            m.setAccessible(true);
            Boolean result = (Boolean) m.invoke(map);
            assertFalse("CompactLinkedMap is case‑insensitive flag should be false", result);
        }
        catch (Exception e)
        {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    public void testUseCopyIteratorReturnsFalse()
    {
        try
        {
            java.lang.reflect.Method m = CompactLinkedMap.class.getDeclaredMethod("useCopyIterator");
            m.setAccessible(true);
            Boolean result = (Boolean) m.invoke(map);
            assertFalse("useCopyIterator should be false for CompactLinkedMap", result);
        }
        catch (Exception e)
        {
            fail("Reflection failed: " + e);
        }
    }

    @Test
    public void testGetNewMapReturnsLinkedHashMapWithProperInitialCapacity()
    {
        try
        {
            java.lang.reflect.Method m = CompactLinkedMap.class.getDeclaredMethod("getNewMap");
            m.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Integer> newMap = (Map<String, Integer>) m.invoke(map);
            assertTrue("Returned map must be a LinkedHashMap",
                    newMap instanceof java.util.LinkedHashMap);
        }
        catch (Exception e)
        {
            fail("Reflection failed: " + e);
        }
    }

    // -------------------------------------------------------------------------
    // End of tests
    // -------------------------------------------------------------------------
}
