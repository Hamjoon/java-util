package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactCIHashMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic Map contract (size, isEmpty, clear, put, get, remove, putAll)</li>
 *   <li>Case‑insensitive handling of {@code String} keys – lookup, containment and key retention</li>
 *   <li>Behaviour with non‑String keys (should be case‑sensitive)</li>
 *   <li>Boundary behaviour when the map grows beyond {@code compactSize()}</li>
 *   <li>Utility methods {@code minus}, {@code plus}, {@code equals}, {@code hashCode}, {@code toString}</li>
 *   <li>Construction from another map</li>
 * </ul>
 *
 * Only public APIs and the protected methods declared in {@code CompactMap} are used – no
 * reflection, mocking or subclassing of library types.
 */
public class CompactCIHashMapTest
{
    private CompactCIHashMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactCIHashMap<>();
    }

    /* --------------------------------------------------------------
     *  1. Basic Map contract
     * -------------------------------------------------------------- */

    @Test
    public void testEmptyMap()
    {
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testPutAndGet()
    {
        assertNull(map.put("One", 1));
        assertEquals(Integer.valueOf(1), map.get("One"));
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
    }

    @Test
    public void testOverwriteValue()
    {
        map.put("Key", 10);
        Integer previous = map.put("Key", 20);
        assertEquals(Integer.valueOf(10), previous);
        assertEquals(Integer.valueOf(20), map.get("Key"));
        assertEquals(1, map.size());
    }

    @Test
    public void testRemove()
    {
        map.put("A", 100);
        map.put("B", 200);
        Integer removed = map.remove("A");
        assertEquals(Integer.valueOf(100), removed);
        assertFalse(map.containsKey("A"));
        assertTrue(map.containsKey("B"));
        assertEquals(1, map.size());
    }

    @Test
    public void testPutAll()
    {
        Map<String, Integer> other = new HashMap<>();
        other.put("X", 9);
        other.put("Y", 8);
        map.putAll(other);
        assertEquals(Integer.valueOf(9), map.get("X"));
        assertEquals(Integer.valueOf(8), map.get("Y"));
        assertEquals(2, map.size());
    }

    @Test
    public void testClear()
    {
        map.put("alpha", 1);
        map.put("beta", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /* --------------------------------------------------------------
     *  2. Case‑insensitive handling of String keys
     * -------------------------------------------------------------- */

    @Test
    public void testCaseInsensitiveLookup()
    {
        map.put("MixedCase", 42);
        // lookup with different capitalisation
        assertEquals(Integer.valueOf(42), map.get("mixedcase"));
        assertEquals(Integer.valueOf(42), map.get("MIXEDCASE"));
        assertTrue(map.containsKey("MiXeDcAsE"));
    }

    @Test
    public void testKeyRetention()
    {
        map.put("OriginalKey", 5);
        Set<String> keys = map.keySet();
        // The key stored should retain the original case
        assertTrue(keys.contains("OriginalKey"));
        // but the case‑insensitive view should not contain a different case string
        assertFalse(keys.contains("originalkey"));
    }

    @Test
    public void testCaseInsensitiveRemove()
    {
        map.put("ToRemove", 99);
        Integer removed = map.remove("toremove"); // different case
        assertEquals(Integer.valueOf(99), removed);
        assertFalse(map.containsKey("ToRemove"));
    }

    /* --------------------------------------------------------------
     *  3. Non‑String keys (should behave like a normal map)
     * -------------------------------------------------------------- */

    @Test
    public void testNonStringKeyIsCaseSensitive()
    {
        CompactCIHashMap<Integer, String> intKeyMap = new CompactCIHashMap<>();
        intKeyMap.put(1, "one");
        intKeyMap.put(2, "two");
        // Different Integer objects with same value are equal – normal map behaviour
        assertEquals("one", intKeyMap.get(Integer.valueOf(1)));
        // No case‑insensitivity applies to non‑String keys
        assertFalse(intKeyMap.containsKey("1"));
    }

    /* --------------------------------------------------------------
     *  4. Behaviour when size exceeds compactSize()
     * -------------------------------------------------------------- */

    @Test
    public void testTransitionToHashMapWhenLarge()
    {
        // compactSize() is protected; we can infer a safe "large" size by adding more than 20 entries,
        // which is well above the default compact threshold used in the library.
        int large = 30;
        for (int i = 0; i < large; i++)
        {
            map.put("k" + i, i);
        }
        assertEquals(large, map.size());

        // All keys must still be case‑insensitive
        assertEquals(Integer.valueOf(15), map.get("K15"));
        // Ensure no data loss after the internal switch
        for (int i = 0; i < large; i++)
        {
            assertEquals(Integer.valueOf(i), map.get("k" + i));
        }
    }

    /* --------------------------------------------------------------
     *  5. Utility methods: minus, plus, equals, hashCode, toString
     * -------------------------------------------------------------- */

    @Test
    public void testMinusCreatesNewMapWithoutKey()
    {
        map.put("keep", 1);
        map.put("drop", 2);
        Map<String, Integer> result = map.minus("DROP"); // different case
        assertEquals(1, result.size());
        assertTrue(result.containsKey("keep"));
        assertFalse(result.containsKey("drop"));
        // original map unchanged
        assertEquals(2, map.size());
    }

    @Test
    public void testPlusCreatesNewMapWithAdditionalEntry()
    {
        map.put("a", 10);
        Map<String, Integer> result = map.plus("b");
        // The plus overload adds a key with a null value
        assertEquals(2, result.size());
        assertTrue(result.containsKey("a"));
        assertTrue(result.containsKey("b"));
        assertNull(result.get("b"));
        // original map unchanged
        assertEquals(1, map.size());
    }

    @Test
    public void testEqualsAndHashCode()
    {
        CompactCIHashMap<String, Integer> map1 = new CompactCIHashMap<>();
        CompactCIHashMap<String, Integer> map2 = new CompactCIHashMap<>();

        map1.put("KeyOne", 1);
        map1.put("KeyTwo", 2);

        map2.put("keyone", 1); // different case – should resolve to same entry
        map2.put("KEYTWO", 2);

        assertTrue(map1.equals(map2));
        assertTrue(map2.equals(map1));
        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testToStringContainsEntries()
    {
        map.put("First", 100);
        map.put("Second", 200);
        String s = map.toString();
        assertTrue(s.contains("First=100"));
        assertTrue(s.contains("Second=200"));
        // The class name should appear (implementation uses default Map toString)
        assertTrue(s.startsWith("{") || s.startsWith("["));
    }

    /* --------------------------------------------------------------
     *  6. Construction from another map
     * -------------------------------------------------------------- */

    @Test
    public void testConstructorWithExistingMap()
    {
        Map<String, Integer> source = new HashMap<>();
        source.put("Alpha", 1);
        source.put("Beta", 2);
        CompactCIHashMap<String, Integer> fromMap = new CompactCIHashMap<>(source);
        assertEquals(2, fromMap.size());
        assertEquals(Integer.valueOf(1), fromMap.get("alpha"));
        assertEquals(Integer.valueOf(2), fromMap.get("BETA"));
    }

    /* --------------------------------------------------------------
     *  7. Edge cases – null handling
     * -------------------------------------------------------------- */

    @Test
    public void testNullKeyAndValue()
    {
        // Null keys are allowed by the underlying HashMap – they are *not* case‑insensitive.
        map.put(null, 123);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(123), map.get(null));

        // Null values are also allowed
        map.put("nullValue", null);
        assertTrue(map.containsKey("nullvalue"));
        assertNull(map.get("NULLVALUE"));
    }
}
