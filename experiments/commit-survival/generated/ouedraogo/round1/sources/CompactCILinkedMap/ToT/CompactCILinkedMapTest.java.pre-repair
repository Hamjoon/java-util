package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactCILinkedMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic Map contract (size, isEmpty, clear, etc.)</li>
 *   <li>Case‑insensitive handling of {@code String} keys while preserving original case</li>
 *   <li>Behaviour with non‑String keys (must be case‑sensitive)</li>
 *   <li>Null keys / values handling</li>
 *   <li>minus / plus operations</li>
 *   <li>equals / hashCode consistency</li>
 *   <li>toString representation</li>
 *   <li>Insertion‑order preservation once the internal map grows beyond {@code compactSize()}</li>
 *   <li>Copy‑constructor semantics</li>
 * </ul>
 *
 * Only APIs that appear in the supplied source or its declared dependencies are used.
 */
public class CompactCILinkedMapTest
{
    private CompactCILinkedMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactCILinkedMap<>();
    }

    /** Helper to obtain the protected {@code compactSize()} value via reflection. */
    private int getCompactSize()
    {
        try
        {
            Method m = CompactMap.class.getDeclaredMethod("compactSize");
            m.setAccessible(true);
            return (int) m.invoke(map);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  1. Basic put / get respecting case‑insensitivity of String keys       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testPutAndGetCaseInsensitiveStringKey()
    {
        map.put("KeyOne", 1);
        // Different case must retrieve the same value
        assertEquals(Integer.valueOf(1), map.get("keyone"));
        assertEquals(Integer.valueOf(1), map.get("KEYONE"));
        // Original case is retained in keySet
        assertTrue(map.keySet().contains("KeyOne"));
        assertFalse(map.keySet().contains("keyone"));
    }

    /* --------------------------------------------------------------------- */
    /*  2. Non‑String keys remain case‑sensitive (they are not altered)       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testNonStringKeyIsCaseSensitive()
    {
        Integer key = 42;          // non‑String key
        map.put(key, 100);
        assertEquals(Integer.valueOf(100), map.get(42));
        assertNull(map.get("42")); // different type => not found
        // Adding a String that looks like the integer must not clash
        map.put("42", 200);
        assertEquals(Integer.valueOf(200), map.get("42"));
        assertEquals(Integer.valueOf(100), map.get(42));
    }

    /* --------------------------------------------------------------------- */
    /*  3. Null key and null value handling                                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void testNullKeyAndValue()
    {
        // Null key is allowed (delegates to underlying map)
        map.put(null, 5);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(5), map.get(null));

        // Null value is allowed
        map.put("SomeKey", null);
        assertTrue(map.containsKey("somekey"));
        assertTrue(map.containsKey("SOMEKEY"));
        assertNull(map.get("SOMEKEY"));
    }

    /* --------------------------------------------------------------------- */
    /*  4. containsKey / containsValue behavior                              */
    /* --------------------------------------------------------------------- */
    @Test
    public void testContainsKeyAndValue()
    {
        map.put("Alpha", 10);
        map.put("Beta", 20);
        assertTrue(map.containsKey("ALPHA"));
        assertTrue(map.containsKey("beta"));
        assertFalse(map.containsKey("gamma"));

        assertTrue(map.containsValue(10));
        assertTrue(map.containsValue(20));
        assertFalse(map.containsValue(30));
    }

    /* --------------------------------------------------------------------- */
    /*  5. remove operation (including case‑insensitive lookup)             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testRemove()
    {
        map.put("ToRemove", 99);
        assertEquals(Integer.valueOf(99), map.remove("toremove"));
        assertNull(map.get("TOREMOVE"));
        assertFalse(map.containsKey("toremove"));
        // Removing a non‑existent key returns null
        assertNull(map.remove("doesNotExist"));
    }

    /* --------------------------------------------------------------------- */
    /*  6. putAll and clear                                                    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testPutAllAndClear()
    {
        Map<String, Integer> other = new HashMap<>();
        other.put("One", 1);
        other.put("TWO", 2);
        other.put("three", 3);
        map.putAll(other);

        assertEquals(3, map.size());
        // case‑insensitive lookups
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("two"));
        assertEquals(Integer.valueOf(3), map.get("THREE"));

        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /* --------------------------------------------------------------------- */
    /*  7. minus and plus (functional‑style) operations                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testMinusAndPlus()
    {
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);

        // minus removes a key (case‑insensitive)
        Map<String, Integer> minusMap = map.minus("b");
        assertFalse(minusMap.containsKey("B"));
        assertEquals(Integer.valueOf(1), minusMap.get("a"));
        assertEquals(Integer.valueOf(3), minusMap.get("c"));
        // original map unchanged
        assertTrue(map.containsKey("B"));

        // plus adds/overwrites a key
        Map<String, Integer> plusMap = map.plus("c"); // adding a single key without a value leaves map unchanged
        assertEquals(map, plusMap); // same content

        Map<String, Integer> plusMap2 = map.plus(new AbstractMap.SimpleEntry<>("d", 4));
        assertEquals(Integer.valueOf(4), plusMap2.get("D"));
        assertEquals(4, plusMap2.size());
    }

    /* --------------------------------------------------------------------- */
    /*  8. equals and hashCode consistency                                    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode()
    {
        CompactCILinkedMap<String, Integer> other = new CompactCILinkedMap<>();
        map.put("X", 10);
        map.put("Y", 20);
        other.put("x", 10);
        other.put("y", 20);

        assertEquals(map, other);
        assertEquals(other, map);
        assertEquals(map.hashCode(), other.hashCode());

        // Different content => not equal
        other.put("Z", 30);
        assertNotEquals(map, other);
    }

    /* --------------------------------------------------------------------- */
    /*  9. toString representation                                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToString()
    {
        map.put("first", 1);
        map.put("second", 2);
        String s = map.toString();
        // The exact format is not specified, but it must contain the entries
        assertTrue(s.contains("first=1"));
        assertTrue(s.contains("second=2"));
        // Braces are typical for Map#toString()
        assertTrue(s.startsWith("{") && s.endsWith("}"));
    }

    /* --------------------------------------------------------------------- */
    /* 10. Insertion order is preserved after exceeding compactSize()         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testInsertionOrderAfterGrowth()
    {
        int limit = getCompactSize() + 5; // go beyond the compact threshold
        for (int i = 0; i < limit; i++)
        {
            map.put("k" + i, i);
        }

        // The entrySet iterator of a LinkedHashMap respects insertion order.
        // Verify that order matches the sequence we added.
        List<String> observed = new ArrayList<>();
        for (Map.Entry<String, Integer> e : map.entrySet())
        {
            observed.add(e.getKey());
        }

        List<String> expected = new ArrayList<>();
        for (int i = 0; i < limit; i++)
        {
            expected.add("k" + i);
        }
        assertEquals(expected, observed);
    }

    /* --------------------------------------------------------------------- */
    /* 11. Copy‑constructor copies all entries preserving case                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testCopyConstructor()
    {
        map.put("Original", 123);
        map.put("Another", 456);
        CompactCILinkedMap<String, Integer> copy = new CompactCILinkedMap<>(map);

        assertEquals(2, copy.size());
        // Case is preserved
        assertTrue(copy.keySet().contains("Original"));
        assertTrue(copy.keySet().contains("Another"));
        // Case‑insensitive lookups work on the copy as well
        assertEquals(Integer.valueOf(123), copy.get("original"));
        assertEquals(Integer.valueOf(456), copy.get("another"));
        // Modifying the copy does not affect the original
        copy.put("NewKey", 789);
        assertFalse(map.containsKey("NewKey"));
    }

    /* --------------------------------------------------------------------- */
    /* 12. size, isEmpty, keySet, values, entrySet basic contract              */
    /* --------------------------------------------------------------------- */
    @Test
    public void testBasicCollectionViews()
    {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());

        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);

        assertFalse(map.isEmpty());
        assertEquals(3, map.size());

        Set<String> keys = map.keySet();
        assertTrue(keys.containsAll(Arrays.asList("A", "B", "C")));
        assertEquals(3, keys.size());

        Collection<Integer> vals = map.values();
        assertTrue(vals.containsAll(Arrays.asList(1, 2, 3)));
        assertEquals(3, vals.size());

        Set<Map.Entry<String, Integer>> entries = map.entrySet();
        for (Map.Entry<String, Integer> e : entries)
        {
            assertEquals(Integer.valueOf(e.getKey().charAt(0) - 'A' + 1), e.getValue());
        }
        assertEquals(3, entries.size());
    }
}
