package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactMap}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Basic Map contract (size, isEmpty, put, get, remove, clear, equals, hashCode)</li>
 *   <li>All internal storage states (EMPTY, OBJECT, ENTRY, ARRAY, MAP)</li>
 *   <li>Case‑sensitivity handling for String keys</li>
 *   <li>Ordering options (unordered, sorted, reverse, insertion)</li>
 *   <li>Transition logic when crossing the {@code compactSize()} threshold</li>
 *   <li>Builder API, {@code withConfig()} and configuration map exposure</li>
 *   <li>Iterators (key, value, entry) including concurrent‑modification detection</li>
 * </ul>
 * </p>
 *
 * <p>Only public APIs from the supplied source are used – no mocking or subclassing of internal
 * classes.</p>
 */
public class CompactMapTest
{
    private static final String SINGLE_KEY = "id";

    private CompactMap<String, Integer> map;

    @Before
    public void setUp()
    {
        // Use the default constructor – this gives us the default configuration
        map = new CompactMap<>();
    }

    /*------------------------------------------------------------*
     *  Basic contract tests
     *------------------------------------------------------------*/

    @Test
    public void testEmptyMapBasics()
    {
        assertTrue("new map must be empty", map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());

        assertFalse(map.containsKey("any"));
        assertFalse(map.containsValue(1));
        assertNull(map.get("any"));
        assertNull(map.remove("any"));
    }

    @Test
    public void testSingleEntryOptimisedStorage()
    {
        // The default single‑value key is "id".  Storing a value with that key
        // should use the OBJECT state (value stored directly, not a CompactMapEntry)
        map.put(SINGLE_KEY, 42);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(42), map.get(SINGLE_KEY));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // Overwrite the value – still OBJECT state
        Integer old = map.put(SINGLE_KEY, 99);
        assertEquals(Integer.valueOf(42), old);
        assertEquals(Integer.valueOf(99), map.get(SINGLE_KEY));
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType());

        // Remove – should return to EMPTY
        Integer removed = map.remove(SINGLE_KEY);
        assertEquals(Integer.valueOf(99), removed);
        assertTrue(map.isEmpty());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    @Test
    public void testSingleEntryEntryState()
    {
        // Use a key different from the single‑value key – we get ENTRY state
        map.put("other", 7);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(7), map.get("other"));
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType());

        // Overwrite – still ENTRY
        Integer old = map.put("other", 8);
        assertEquals(Integer.valueOf(7), old);
        assertEquals(Integer.valueOf(8), map.get("other"));

        // Remove – back to EMPTY
        Integer removed = map.remove("other");
        assertEquals(Integer.valueOf(8), removed);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testCompactArrayStateAndBinarySearch()
    {
        // Use the builder to set a small compactSize so we can test the ARRAY state easily
        CompactMap<String, Integer> small = CompactMap.<String, Integer>builder()
                .compactSize(5)          // keep threshold low
                .sortedOrder()           // force binary‑search logic
                .build();

        // Insert 3 entries – still ARRAY (2 .. compactSize)
        small.put("b", 2);
        small.put("a", 1);
        small.put("c", 3);

        assertEquals(3, small.size());
        assertEquals(CompactMap.LogicalValueType.ARRAY, small.getLogicalValueType());

        // Keys must be sorted – get should succeed irrespective of insertion order
        assertEquals(Integer.valueOf(1), small.get("a"));
        assertEquals(Integer.valueOf(2), small.get("b"));
        assertEquals(Integer.valueOf(3), small.get("c"));

        // containsKey uses binary search
        assertTrue(small.containsKey("b"));
        assertFalse(small.containsKey("z"));
    }

    @Test
    public void testTransitionToBackingMap()
    {
        // compactSize = 3, insert 4 entries → transition to MAP state
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(3)
                .build();

        map.put("k1", 1);
        map.put("k2", 2);
        map.put("k3", 3);
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());

        map.put("k4", 4); // triggers transition
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // Verify all entries are present
        for (int i = 1; i <= 4; i++)
        {
            assertEquals(Integer.valueOf(i), map.get("k" + i));
        }
    }

    @Test
    public void testRemovalCausingBackTransition()
    {
        // compactSize = 3, start in MAP state (4 entries), then remove one → back to ARRAY
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .compactSize(3)
                .build();

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4); // now MAP

        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType());

        // Remove one entry – size becomes compactSize (3) → should go back to ARRAY
        map.remove("d");
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType());
        assertEquals(3, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
    }

    @Test
    public void testClearResetsState()
    {
        map.put("x", 10);
        map.put("y", 20);
        map.clear();

        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType());
    }

    @Test
    public void testEqualsAndHashCode()
    {
        CompactMap<String, Integer> m1 = new CompactMap<>();
        CompactMap<String, Integer> m2 = new CompactMap<>();

        // both empty
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());

        // add same entries in different order
        m1.put("a", 1);
        m1.put("b", 2);

        m2.put("b", 2);
        m2.put("a", 1);

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());

        // different value
        m2.put("a", 99);
        assertNotEquals(m1, m2);
    }

    @Test
    public void testGetConfigAndWithConfig()
    {
        CompactMap<String, Integer> original = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .sortedOrder()
                .compactSize(10)
                .singleValueKey("uuid")
                .mapType(LinkedHashMap.class)
                .build();

        original.put("uuid", 123);
        original.put("foo", 456);

        Map<String, Object> cfg = original.getConfig();

        assertEquals(10, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
        assertEquals("uuid", cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(LinkedHashMap.class, cfg.get(CompactMap.MAP_TYPE));

        // Build a new map with the same configuration but different entries
        CompactMap<String, Integer> copy = original.withConfig(Collections.emptyMap());

        assertEquals(original, copy);
        assertNotSame(original, copy);
        assertEquals(original.getConfig(), copy.getConfig());
    }

    /*------------------------------------------------------------*
     *  Builder‑related tests
     *------------------------------------------------------------*/

    @Test
    public void testBuilderCreatesCaseInsensitiveSortedMap()
    {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .sortedOrder()
                .build();

        // Keys are compared ignoring case
        map.put("Key", 1);
        assertTrue(map.containsKey("key"));
        assertEquals(Integer.valueOf(1), map.get("KEY"));

        // Sorted order – iterator should return keys in natural order (case‑insensitive)
        map.put("apple", 2);
        map.put("Banana", 3);

        Iterator<String> it = map.keySet().iterator();
        assertEquals("apple", it.next());
        assertEquals("Banana", it.next());
        assertEquals("Key", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testBuilderInsertionOrder()
    {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder()
                .insertionOrder()
                .build();

        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        List<String> keys = new ArrayList<>();
        for (String k : map.keySet())
        {
            keys.add(k);
        }
        assertEquals(Arrays.asList("first", "second", "third"), keys);
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderFailsWhenJavaCompilerNotAvailable()
    {
        // Simulate missing compiler by temporarily disabling the check.
        // The public API throws IllegalStateException when the compiler is absent.
        // In a normal JVM with JDK tools this test will pass because the
        // exception is *not* thrown – we therefore guard it with a runtime
        // check.  If the compiler is available, we simply skip the test.
        if (ReflectionUtils.isJavaCompilerAvailable())
        {
            // Skip test; we cannot force the environment to lack a compiler.
            return;
        }
        CompactMap.<String, Integer>builder().build();
    }

    /*------------------------------------------------------------*
     *  Iterator behavior tests
     *------------------------------------------------------------*/

    @Test
    public void testKeyIterator()
    {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Set<String> expected = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> actual = new HashSet<>();

        for (String k : map.keySet())
        {
            actual.add(k);
        }

        assertEquals(expected, actual);
    }

    @Test
    public void testValueIterator()
    {
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);

        Set<Integer> expected = new HashSet<>(Arrays.asList(10, 20, 30));
        Set<Integer> actual = new HashSet<>();

        for (Integer v : map.values())
        {
            actual.add(v);
        }

        assertEquals(expected, actual);
    }

    @Test
    public void testEntryIteratorAndRemove()
    {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = it.next();
            if (e.getKey().equals("two"))
            {
                it.remove(); // should remove the entry
            }
        }

        assertFalse(map.containsKey("two"));
        assertEquals(2, map.size());
    }

    @Test(expected = ConcurrentModificationException.class)
    public void testIteratorConcurrentModification()
    {
        map.put("a", 1);
        map.put("b", 2);

        Iterator<String> it = map.keySet().iterator();
        assertTrue(it.hasNext());
        map.put("c", 3); // modify map after iterator creation
        it.next(); // should throw CME
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveWithoutNext()
    {
        map.put("solo", 42);
        Iterator<String> it = map.keySet().iterator();
        // directly call remove without a preceding next()
        it.remove();
    }

    /*------------------------------------------------------------*
     *  Edge‑case tests for computeKeyHashCode / computeValueHashCode
     *------------------------------------------------------------*/

    @Test
    public void testHashCodeWithNullsAndSelfReference()
    {
        CompactMap<String, Object> m = new CompactMap<>();
        m.put(null, null);
        m.put("self", m); // value references the map itself

        int h = m.hashCode();

        // Manual calculation: key null → 0, value null → 0
        // key "self" → hashCode of string, value → 17 (self‑reference)
        int expected = 0 ^ 0; // first entry
        expected += "self".hashCode() ^ 17; // second entry

        assertEquals(expected, h);
    }

    @Test
    public void testCaseInsensitiveKeyHash()
    {
        CompactMap<String, Integer> ciMap = CompactMap.<String, Integer>builder()
                .caseSensitive(false)
                .build();

        ciMap.put("AbC", 1);
        // The hash code must be based on case‑insensitive hash
        int expected = StringUtilities.hashCodeIgnoreCase("AbC") ^ 1;
        assertEquals(expected, ciMap.hashCode());
    }

    /*------------------------------------------------------------*
     *  Miscellaneous tests (toString, minus/plus unsupported)
     *------------------------------------------------------------*/

    @Test
    public void testToStringFormat()
    {
        map.put("one", 1);
        map.put("two", 2);
        String s = map.toString();

        // The exact order is not guaranteed for unordered maps,
        // but the string must start with '{' and end with '}' and contain both entries.
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("one=1"));
        assertTrue(s.contains("two=2"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testMinusUnsupported()
    {
        map.minus("any");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testPlusUnsupported()
    {
        map.plus("any");
    }
}
