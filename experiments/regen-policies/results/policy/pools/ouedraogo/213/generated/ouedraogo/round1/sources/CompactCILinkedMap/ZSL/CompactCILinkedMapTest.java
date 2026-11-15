/**
 * CompactCILinkedMapTest.java
 *
 * Comprehensive JUnit‑4 test suite for {@link com.cedarsoftware.util.CompactCILinkedMap}.
 *
 * The tests exercise:
 *   • Basic Map contract (size, isEmpty, clear, put, get, remove, containsKey, containsValue)
 *   • Case‑insensitive key handling (inherited from {@link CompactMap})
 *   • Insertion order preservation (LinkedHashMap behaviour)
 *   • Immutable‑style operations {@code minus} and {@code plus}
 *   • Configuration handling via {@code getConfig} / {@code withConfig}
 *   • Equality / hashCode semantics
 *
 * Only public API from {@code CompactMap} and standard JDK classes are used,
 * as required by the task description.
 */

package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class CompactCILinkedMapTest
{
    private CompactCILinkedMap<String, Integer> map;

    @Before
    public void setUp()
    {
        map = new CompactCILinkedMap<>();
    }

    /* ---------------------------------------------------------------------- */
    /*  Basic Map contract                                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testPutAndGetCaseInsensitive()
    {
        map.put("Key", 1);
        // Different case must resolve to the same entry
        assertEquals(Integer.valueOf(1), map.get("key"));
        assertEquals(Integer.valueOf(1), map.get("KEY"));
    }

    @Test
    public void testOverwriteWithDifferentCaseKeepsSingleEntry()
    {
        map.put("Key", 1);
        map.put("kEy", 2);               // should replace the previous entry
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(2), map.get("KEY"));
    }

    @Test
    public void testRemoveIsCaseInsensitive()
    {
        map.put("RemoveMe", 99);
        assertTrue(map.containsKey("removeme"));
        map.remove("REMOVEME");
        assertFalse(map.containsKey("removeMe"));
        assertTrue(map.isEmpty());
    }

    @Test
    public void testContainsKeyAndContainsValue()
    {
        map.put("Alpha", 10);
        assertTrue(map.containsKey("ALPHA"));
        assertTrue(map.containsValue(10));
        assertFalse(map.containsKey("Beta"));
        assertFalse(map.containsValue(20));
    }

    @Test
    public void testClearAndIsEmpty()
    {
        map.put("A", 1);
        map.put("B", 2);
        assertFalse(map.isEmpty());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testPutAll()
    {
        Map<String, Integer> other = new HashMap<>();
        other.put("One", 1);
        other.put("Two", 2);
        map.putAll(other);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("one"));
        assertEquals(Integer.valueOf(2), map.get("TWO"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Ordering (LinkedHashMap semantics)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testInsertionOrderPreservedInKeySet()
    {
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);

        List<String> expectedOrder = Arrays.asList("first", "second", "third");
        List<String> actualOrder = new ArrayList<>(map.keySet());

        assertEquals(expectedOrder, actualOrder);
    }

    @Test
    public void testInsertionOrderPreservedInValues()
    {
        map.put("first", 10);
        map.put("second", 20);
        map.put("third", 30);

        List<Integer> expected = Arrays.asList(10, 20, 30);
        List<Integer> actual = new ArrayList<>(map.values());

        assertEquals(expected, actual);
    }

    @Test
    public void testInsertionOrderPreservedInEntrySet()
    {
        map.put("A", 100);
        map.put("B", 200);
        map.put("C", 300);

        List<String> expectedKeys = Arrays.asList("A", "B", "C");
        List<Integer> expectedVals = Arrays.asList(100, 200, 300);

        List<String> actualKeys = new ArrayList<>();
        List<Integer> actualVals = new ArrayList<>();

        for (Map.Entry<String, Integer> e : map.entrySet())
        {
            actualKeys.add(e.getKey());
            actualVals.add(e.getValue());
        }

        assertEquals(expectedKeys, actualKeys);
        assertEquals(expectedVals, actualVals);
    }

    /* ---------------------------------------------------------------------- */
    /*  minus / plus – immutable style operations                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testMinusCreatesNewMapWithoutKey()
    {
        map.put("keep", 1);
        map.put("remove", 2);
        Map<String, Integer> result = map.minus("REMOVE"); // case‑insensitive

        // original map unchanged
        assertEquals(2, map.size());
        assertTrue(map.containsKey("keep"));
        assertTrue(map.containsKey("remove"));

        // result map does not contain the removed entry
        assertEquals(1, result.size());
        assertTrue(result.containsKey("keep"));
        assertFalse(result.containsKey("remove"));
    }

    @Test
    public void testPlusCreatesNewMapWithAdditionalEntry()
    {
        map.put("existing", 5);
        Map<String, Integer> result = map.plus("newKey");

        // original map unchanged
        assertEquals(1, map.size());
        assertTrue(map.containsKey("existing"));
        assertFalse(map.containsKey("newKey"));

        // result map contains the additional key with a null value (per CompactMap contract)
        assertEquals(2, result.size());
        assertTrue(result.containsKey("newKey"));
        assertNull(result.get("newKey"));
    }

    /* ---------------------------------------------------------------------- */
    /*  Equality and hashCode                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeAreCaseInsensitive()
    {
        CompactCILinkedMap<String, Integer> m1 = new CompactCILinkedMap<>();
        CompactCILinkedMap<String, Integer> m2 = new CompactCILinkedMap<>();

        m1.put("KeyOne", 1);
        m1.put("KeyTwo", 2);

        m2.put("keyone", 1);
        m2.put("KEYTWO", 2);

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    /* ---------------------------------------------------------------------- */
    /*  Configuration handling                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConfigIsCaseInsensitiveAndCanBeModified()
    {
        // The default config must contain the CASE_SENSITIVE flag set to false
        Map<String, Object> cfg = map.getConfig();
        assertTrue(cfg.containsKey(CompactMap.CASE_SENSITIVE));
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));

        // Change a config value using withConfig and verify the new map reflects it
        Map<String, Object> newCfg = new HashMap<>(cfg);
        newCfg.put(CompactMap.CASE_SENSITIVE, Boolean.TRUE);

        CompactCILinkedMap<String, Integer> altered = (CompactCILinkedMap<String, Integer>) map.withConfig(newCfg);
        Map<String, Object> alteredCfg = altered.getConfig();

        assertEquals(Boolean.TRUE, alteredCfg.get(CompactMap.CASE_SENSITIVE));
        // The original map must stay unchanged
        assertEquals(Boolean.FALSE, map.getConfig().get(CompactMap.CASE_SENSITIVE));
    }

    /* ---------------------------------------------------------------------- */
    /*  Builder usage (static method on CompactMap)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testBuilderCreatesMapWithProvidedEntries()
    {
        // The builder is defined on CompactMap; we use it to create a CompactCILinkedMap
        CompactMap.Builder<String, Integer> builder = CompactMap.builder();
        CompactMap<String, Integer> built = builder
                .put("Alpha", 1)
                .put("Beta", 2)
                .build();

        // The built map should behave like a CompactCILinkedMap in terms of case‑insensitivity
        assertEquals(Integer.valueOf(1), built.get("ALPHA"));
        assertEquals(Integer.valueOf(2), built.get("beta"));

        // Verify ordering is preserved
        List<String> keys = new ArrayList<>(built.keySet());
        assertEquals(Arrays.asList("Alpha", "Beta"), keys);
    }
}
