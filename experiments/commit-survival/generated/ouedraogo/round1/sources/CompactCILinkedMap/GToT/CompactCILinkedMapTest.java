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
 *   <li>All public constructors.</li>
 *   <li>All inherited public Map operations.</li>
 *   <li>Case‑insensitive handling of {@link String} keys while preserving the original case.</li>
 *   <li>Correct handling of non‑String keys.</li>
 *   <li>Transition to a {@link LinkedHashMap} when the number of entries exceeds {@code compactSize()}.</li>
 *   <li>Edge cases such as {@code null} keys/values and replacement of existing entries.</li>
 *   <li>Exception safety (no unexpected exceptions are thrown for supported operations).</li>
 * </ul>
 */
public class CompactCILinkedMapTest {

    private CompactCILinkedMap<String, Integer> map;

    @Before
    public void setUp() {
        map = new CompactCILinkedMap<>();
    }

    /** -----------------------------------------------------------------------
     *  1. Constructors
     * ----------------------------------------------------------------------- */
    @Test
    public void testDefaultConstructorCreatesEmptyMap() {
        assertTrue("Map should be empty after default construction", map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    public void testMapConstructorCopiesEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("One", 1);
        source.put("Two", 2);
        CompactCILinkedMap<String, Integer> copy = new CompactCILinkedMap<>(source);

        assertEquals(2, copy.size());
        assertTrue(copy.containsKey("One"));
        assertTrue(copy.containsKey("Two"));
        assertEquals(Integer.valueOf(1), copy.get("ONE")); // case‑insensitive check
        assertEquals(Integer.valueOf(2), copy.get("two")); // case‑insensitive check
    }

    /** -----------------------------------------------------------------------
     *  2. Basic Map operations – put / get / containsKey / containsValue
     * ----------------------------------------------------------------------- */
    @Test
    public void testPutAndGetCaseInsensitiveStringKey() {
        map.put("KeyOne", 100);
        // Retrieval with different case must succeed
        assertEquals(Integer.valueOf(100), map.get("keyone"));
        assertEquals(Integer.valueOf(100), map.get("KEYONE"));
        // The key stored should retain the original case
        assertTrue(map.keySet().contains("KeyOne"));
        assertFalse(map.keySet().contains("keyone"));
    }

    @Test
    public void testPutReplacesExistingKeyRegardlessOfCase() {
        map.put("Alpha", 1);
        Integer previous = map.put("ALPHA", 2); // same logical key, different case
        assertEquals(Integer.valueOf(1), previous); // old value returned
        assertEquals(Integer.valueOf(2), map.get("alpha"));
        // The key stored should now be the case of the most recent put
        assertTrue(map.keySet().contains("ALPHA"));
        assertFalse(map.keySet().contains("Alpha"));
    }

    @Test
    public void testNonStringKeyBehavesNormally() {
        CompactCILinkedMap<Integer, String> intKeyMap = new CompactCILinkedMap<>();
        intKeyMap.put(10, "ten");
        intKeyMap.put(20, "twenty");

        assertEquals("ten", intKeyMap.get(10));
        assertEquals("twenty", intKeyMap.get(20));
        // Different Integer objects that are equal should map to the same entry
        assertEquals("ten", intKeyMap.get(new Integer(10)));
    }

    @Test
    public void testContainsKeyAndContainsValue() {
        map.put("Exists", 42);
        assertTrue(map.containsKey("exists"));
        assertTrue(map.containsValue(42));
        assertFalse(map.containsKey("missing"));
        assertFalse(map.containsValue(99));
    }

    /** -----------------------------------------------------------------------
     *  3. Null handling
     * ----------------------------------------------------------------------- */
    @Test
    public void testNullKeyAndNullValue() {
        // CompactCILinkedMap (via CaseInsensitiveMap) permits a single null key
        map.put(null, 123);
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(123), map.get(null));

        // Overwrite null key with null value
        map.put(null, null);
        assertTrue(map.containsKey(null));
        assertNull(map.get(null));
    }

    /** -----------------------------------------------------------------------
     *  4. Bulk operations – putAll, clear, remove, minus, plus
     * ----------------------------------------------------------------------- */
    @Test
    public void testPutAllCopiesAllEntriesCaseInsensitively() {
        Map<String, Integer> other = new HashMap<>();
        other.put("A", 1);
        other.put("b", 2);
        map.putAll(other);

        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("B"));
    }

    @Test
    public void testRemoveByKeyCaseInsensitive() {
        map.put("RemoveMe", 7);
        Integer removed = map.remove("removeme");
        assertEquals(Integer.valueOf(7), removed);
        assertFalse(map.containsKey("RemoveMe"));
    }

    @Test
    public void testMinusCreatesNewMapWithoutSpecifiedKey() {
        map.put("One", 1);
        map.put("Two", 2);
        Map<String, Integer> result = map.minus("ONE"); // case‑insensitive removal

        assertEquals(1, result.size());
        assertFalse(result.containsKey("One"));
        assertTrue(result.containsKey("Two"));
        // original map unchanged
        assertEquals(2, map.size());
    }

    @Test
    public void testPlusCreatesNewMapWithAdditionalEntry() {
        map.put("Base", 10);
        Map<String, Integer> result = map.plus("NEWKEY");

        assertEquals(2, result.size());
        assertTrue(result.containsKey("Base"));
        assertTrue(result.containsKey("NEWKEY"));
        // original map unchanged
        assertEquals(1, map.size());
    }

    @Test
    public void testClearEmptiesMap() {
        map.put("A", 1);
        map.put("B", 2);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    /** -----------------------------------------------------------------------
     *  5. Equality, hashCode and toString
     * ----------------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCodeConsistency() {
        CompactCILinkedMap<String, Integer> other = new CompactCILinkedMap<>();
        map.put("X", 5);
        other.put("x", 5); // same logical entry

        assertTrue(map.equals(other));
        assertEquals(map.hashCode(), other.hashCode());
    }

    @Test
    public void testToStringContainsEntries() {
        map.put("Alpha", 1);
        map.put("Beta", 2);
        String s = map.toString();
        assertTrue(s.contains("Alpha=1"));
        assertTrue(s.contains("Beta=2"));
    }

    /** -----------------------------------------------------------------------
     *  6. Transition to LinkedHashMap (order preservation) when size > compactSize()
     * ----------------------------------------------------------------------- */
    @Test
    public void testInsertionOrderPreservedAfterExceedingCompactSize() throws Exception {
        // Obtain the compact size via reflection (protected method)
        int compactSize;
        {
            java.lang.reflect.Method m = CompactMap.class.getDeclaredMethod("compactSize");
            m.setAccessible(true);
            compactSize = (Integer) m.invoke(map);
        }

        // Insert compactSize + 2 entries to force the internal map to become a LinkedHashMap
        for (int i = 0; i < compactSize + 2; i++) {
            map.put("k" + i, i);
        }

        // Verify size
        assertEquals(compactSize + 2, map.size());

        // Verify that iteration order matches insertion order
        List<String> expectedOrder = new ArrayList<>();
        for (int i = 0; i < compactSize + 2; i++) {
            expectedOrder.add("k" + i);
        }

        List<String> actualOrder = new ArrayList<>();
        for (String key : map.keySet()) {
            actualOrder.add(key);
        }

        assertEquals("Insertion order should be preserved after transition to LinkedHashMap",
                expectedOrder, actualOrder);
    }

    /** -----------------------------------------------------------------------
     *  7. Edge case: Adding a duplicate key after transition to LinkedHashMap
     * ----------------------------------------------------------------------- */
    @Test
    public void testDuplicateKeyAfterTransitionUpdatesValueAndRetainsNewCase() throws Exception {
        // Force transition first
        int compactSize;
        {
            java.lang.reflect.Method m = CompactMap.class.getDeclaredMethod("compactSize");
            m.setAccessible(true);
            compactSize = (Integer) m.invoke(map);
        }
        for (int i = 0; i < compactSize + 1; i++) {
            map.put("k" + i, i);
        }

        // Now replace an existing key with different case
        map.put("K0", 999);
        assertEquals(Integer.valueOf(999), map.get("k0"));
        // The key stored should now be "K0"
        assertTrue(map.keySet().contains("K0"));
        assertFalse(map.keySet().contains("k0"));
    }

    /** -----------------------------------------------------------------------
     *  8. Verify that map view collections reflect current state
     * ----------------------------------------------------------------------- */
    @Test
    public void testKeySetAndValuesReflectUpdates() {
        map.put("First", 1);
        map.put("Second", 2);
        Set<String> keys = map.keySet();
        Collection<Integer> values = map.values();

        assertTrue(keys.contains("First"));
        assertTrue(keys.contains("Second"));
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));

        // Remove an entry and verify the views are updated
        map.remove("first");
        assertFalse(keys.contains("First"));
        assertFalse(values.contains(1));
    }
}
