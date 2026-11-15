package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link CompactCILinkedMap}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (empty and from another map)</li>
 *   <li>Basic Map contract (size, isEmpty, clear, containsKey/Value, get, put, remove, putAll)</li>
 *   <li>Case‑insensitive key handling</li>
 *   <li>Insertion order preservation (LinkedHashMap behaviour)</li>
 *   <li>Edge cases such as null keys/values, duplicate keys with different case,
 *       and modifications during iteration.</li>
 *   <li>Behaviour of the additional methods inherited from {@link CompactMap}
 *       (minus, plus, getConfig, withConfig, builder).</li>
 * </ul>
 *
 * Only public API and protected methods that are accessible from this subclass are used.
 * No mocking or subclassing of library internals is performed.
 */
public class CompactCILinkedMapTest
{
    private CompactCILinkedMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactCILinkedMap<>();
    }

    /* ---------------------------------------------------------------------- */
    /*  Construction tests                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructor()
    {
        assertTrue("New map should be empty", map.isEmpty());
        assertEquals("Size should be 0", 0, map.size());
    }

    @Test
    public void testConstructorFromMap()
    {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("One", 1);
        source.put("Two", 2);
        CompactCILinkedMap<String, Integer> fromMap = new CompactCILinkedMap<>(source);

        assertEquals("All entries must be copied", source, fromMap);
        assertEquals("Insertion order must be preserved", 
                     new ArrayList<>(source.keySet()), 
                     new ArrayList<>(fromMap.keySet()));
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic Map contract tests                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGet()
    {
        assertNull("Getting a non‑existing key returns null", map.get("abc"));
        map.put("Key", 10);
        assertEquals(Integer.valueOf(10), map.get("Key"));
        // case‑insensitive retrieval
        assertEquals(Integer.valueOf(10), map.get("kEy"));
    }

    @Test
    public void testPutOverwrite()
    {
        map.put("Alpha", 1);
        Integer previous = map.put("ALPHA", 2); // different case, same logical key
        assertEquals(Integer.valueOf(1), previous);
        assertEquals(Integer.valueOf(2), map.get("alpha"));
        assertEquals(1, map.size());
    }

    @Test
    public void testContainsKey()
    {
        map.put("Test", 5);
        assertTrue(map.containsKey("test"));
        assertTrue(map.containsKey("TEST"));
        assertFalse(map.containsKey("different"));
    }

    @Test
    public void testContainsValue()
    {
        map.put("A", 100);
        map.put("B", 200);
        assertTrue(map.containsValue(100));
        assertFalse(map.containsValue(300));
    }

    @Test
    public void testRemove()
    {
        map.put("RemoveMe", 42);
        Integer removed = map.remove("removeme"); // case‑insensitive
        assertEquals(Integer.valueOf(42), removed);
        assertFalse(map.containsKey("REMOVEme"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testPutAllAndClear()
    {
        Map<String, Integer> another = new HashMap<>();
        another.put("X", 1);
        another.put("Y", 2);
        map.putAll(another);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("y"));

        map.clear();
        assertTrue(map.isEmpty());
    }

    @Test
    public void testKeySetAndValuesOrder()
    {
        map.put("First", 1);
        map.put("Second", 2);
        map.put("Third", 3);

        List<String> expectedKeys = Arrays.asList("First", "Second", "Third");
        List<Integer> expectedValues = Arrays.asList(1, 2, 3);

        assertEquals(expectedKeys, new ArrayList<>(map.keySet()));
        assertEquals(expectedValues, new ArrayList<>(map.values()));
    }

    @Test
    public void testEntrySetIterationAndModification()
    {
        map.put("A", 10);
        map.put("B", 20);
        map.put("C", 30);

        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, Integer> e = it.next();
            if (e.getKey().equalsIgnoreCase("b"))
            {
                it.remove(); // remove via iterator
            }
        }
        assertFalse(map.containsKey("b"));
        assertEquals(2, map.size());
    }

    /* ---------------------------------------------------------------------- */
    /*  Null handling tests                                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNullKeyAllowed()
    {
        // CompactCILinkedMap (via CompactMap) delegates to CaseInsensitiveMap,
        // which permits a single null key (like LinkedHashMap).
        map.put(null, 99);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(99), map.get(null));
        assertEquals(1, map.size());

        // Adding another null key should replace the previous value
        map.put(null, 100);
        assertEquals(Integer.valueOf(100), map.get(null));
        assertEquals(1, map.size());
    }

    @Test
    public void testNullValueAllowed()
    {
        map.put("KeyWithNull", null);
        assertTrue(map.containsKey("keywithnull"));
        assertTrue(map.containsValue(null));
        assertNull(map.get("KEYWITHNULL"));
    }

    /* ---------------------------------------------------------------------- */
    /*  CompactMap‑specific methods                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMinusCreatesNewMapWithoutKey()
    {
        map.put("keep", 1);
        map.put("remove", 2);
        Map<String, Integer> result = map.minus("REMOVE"); // case‑insensitive

        assertFalse(result.containsKey("remove"));
        assertTrue(result.containsKey("keep"));
        // original map must stay unchanged
        assertTrue(map.containsKey("remove"));
    }

    @Test
    public void testPlusCreatesNewMapWithAdditionalEntry()
    {
        map.put("exist", 5);
        Map<String, Integer> result = map.plus(new AbstractMap.SimpleEntry<>("NewKey", 10));

        assertTrue(result.containsKey("exist"));
        assertTrue(result.containsKey("newkey"));
        assertEquals(Integer.valueOf(10), result.get("NEWKEY"));
        // original map unchanged
        assertFalse(map.containsKey("newkey"));
    }

    @Test
    public void testBuilderCreatesEmptyMap()
    {
        CompactCILinkedMap<String, String> built = CompactMap.builder()
                .withConfig(Collections.singletonMap(CompactMap.CASE_SENSITIVE, false))
                .build();

        assertTrue(built.isEmpty());
        built.put("Hello", "World");
        assertEquals("World", built.get("HELLO"));
    }

    @Test
    public void testWithConfigChangesCaseSensitivity()
    {
        // By default this map is case‑insensitive; we verify that withConfig can
        // toggle the flag without breaking functionality.
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(CompactMap.CASE_SENSITIVE, Boolean.TRUE);
        CompactCILinkedMap<String, Integer> csMap = (CompactCILinkedMap<String, Integer>) map.withConfig(cfg);

        csMap.put("Key", 1);
        assertTrue(csMap.containsKey("Key"));
        assertFalse(csMap.containsKey("key")); // now case‑sensitive
        assertEquals(Integer.valueOf(1), csMap.get("Key"));
    }

    @Test
    public void testGetConfigReflectsDefaults()
    {
        Map<String, Object> cfg = map.getConfig();
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.INSERTION, cfg.get(CompactMap.ORDERING));
    }

    /* ---------------------------------------------------------------------- */
    /*  Equality and hashCode tests                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode()
    {
        CompactCILinkedMap<String, Integer> other = new CompactCILinkedMap<>();
        map.put("A", 1);
        map.put("B", 2);
        other.put("a", 1);
        other.put("b", 2);

        assertEquals(map, other);
        assertEquals(map.hashCode(), other.hashCode());

        other.put("C", 3);
        assertNotEquals(map, other);
    }

    /* ---------------------------------------------------------------------- */
    /*  toString sanity check                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringContainsEntries()
    {
        map.put("X", 9);
        map.put("Y", 8);
        String s = map.toString();
        assertTrue(s.contains("X=9"));
        assertTrue(s.contains("Y=8"));
        // order must be preserved in the string representation
        assertTrue(s.indexOf("X=9") < s.indexOf("Y=8"));
    }
}
