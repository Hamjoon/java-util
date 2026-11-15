package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCILinkedMap}.
 *
 * The test cases cover:
 * <ul>
 *   <li>All public constructors.</li>
 *   <li>All public instance methods inherited from {@link CompactMap}.</li>
 *   <li>Static builder method.</li>
 *   <li>Case‑insensitivity behaviour.</li>
 *   <li>Insertion‑order preservation (LinkedHashMap semantics).</li>
 *   <li>Edge cases such as {@code null} keys/values, duplicate keys, removal of absent keys, etc.</li>
 *   <li>Exception safety – the class does not throw checked exceptions, but we verify that illegal
 *       arguments do not corrupt the map.</li>
 * </ul>
 */
public class CompactCILinkedMapTest {

    private CompactCILinkedMap<String, String> emptyMap;
    private CompactCILinkedMap<String, String> populatedMap;

    @Before
    public void setUp() {
        emptyMap = new CompactCILinkedMap<>();

        Map<String, String> init = new LinkedHashMap<>();
        init.put("One", "1");
        init.put("Two", "2");
        init.put("Three", "3");
        populatedMap = new CompactCILinkedMap<>(init);
    }

    /* -------------------------------------------------
     *  Public constructors
     * ------------------------------------------------- */

    @Test
    public void testNoArgConstructorCreatesEmptyMap() {
        assertTrue(emptyMap.isEmpty());
        assertEquals(0, emptyMap.size());
    }

    @Test
    public void testMapConstructorCopiesEntries() {
        assertEquals(3, populatedMap.size());
        assertEquals("1", populatedMap.get("One"));
        assertEquals("2", populatedMap.get("Two"));
        assertEquals("3", populatedMap.get("Three"));
    }

    /* -------------------------------------------------
     *  Core Map operations
     * ------------------------------------------------- */

    @Test
    public void testPutAndGet_CaseInsensitive() {
        emptyMap.put("Key", "value");
        // different case should retrieve the same entry
        assertEquals("value", emptyMap.get("key"));
        assertEquals("value", emptyMap.get("KEY"));
    }

    @Test
    public void testPut_ReplacesExistingCaseInsensitiveKey() {
        emptyMap.put("Case", "first");
        emptyMap.put("case", "second"); // should replace the previous entry
        assertEquals(1, emptyMap.size());
        assertEquals("second", emptyMap.get("CASE"));
    }

    @Test
    public void testPut_NullKeyAndNullValue() {
        emptyMap.put(null, null);
        assertTrue(emptyMap.containsKey(null));
        assertTrue(emptyMap.containsValue(null));
        assertNull(emptyMap.get(null));
        assertEquals(1, emptyMap.size());
    }

    @Test
    public void testRemove_ExistingAndNonExistingKeys() {
        String removed = populatedMap.remove("Two");
        assertEquals("2", removed);
        assertFalse(populatedMap.containsKey("Two"));
        assertEquals(2, populatedMap.size());

        // removing a key that does not exist should return null and leave the map unchanged
        assertNull(populatedMap.remove("Missing"));
        assertEquals(2, populatedMap.size());
    }

    @Test
    public void testContainsKey_CaseInsensitive() {
        assertTrue(populatedMap.containsKey("one"));
        assertTrue(populatedMap.containsKey("ONE"));
        assertFalse(populatedMap.containsKey("four"));
    }

    @Test
    public void testContainsValue() {
        assertTrue(populatedMap.containsValue("1"));
        assertFalse(populatedMap.containsValue("10"));
    }

    @Test
    public void testPutAll() {
        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("Four", "4");
        extra.put("five", "5");
        populatedMap.putAll(extra);
        assertEquals(5, populatedMap.size());
        assertEquals("4", populatedMap.get("four"));
        assertEquals("5", populatedMap.get("FIVE"));
    }

    @Test
    public void testClear() {
        populatedMap.clear();
        assertTrue(populatedMap.isEmpty());
        assertEquals(0, populatedMap.size());
    }

    @Test
    public void testIsEmptyAndSize() {
        assertTrue(emptyMap.isEmpty());
        assertEquals(0, emptyMap.size());

        assertFalse(populatedMap.isEmpty());
        assertEquals(3, populatedMap.size());
    }

    /* -------------------------------------------------
     *  Collection view semantics (keySet, values, entrySet)
     * ------------------------------------------------- */

    @Test
    public void testKeySet_InsertionOrderPreserved() {
        List<String> expectedOrder = Arrays.asList("One", "Two", "Three");
        List<String> actualOrder = new ArrayList<>(populatedMap.keySet());
        assertEquals(expectedOrder, actualOrder);
    }

    @Test
    public void testValues_InsertionOrderPreserved() {
        List<String> expected = Arrays.asList("1", "2", "3");
        List<String> actual = new ArrayList<>(populatedMap.values());
        assertEquals(expected, actual);
    }

    @Test
    public void testEntrySet_IteratesInInsertionOrder() {
        Iterator<Map.Entry<String, String>> it = populatedMap.entrySet().iterator();
        assertTrue(it.hasNext());
        Map.Entry<String, String> e1 = it.next();
        assertEquals("One", e1.getKey());
        assertEquals("1", e1.getValue());

        Map.Entry<String, String> e2 = it.next();
        assertEquals("Two", e2.getKey());
        assertEquals("2", e2.getValue());

        Map.Entry<String, String> e3 = it.next();
        assertEquals("Three", e3.getKey());
        assertEquals("3", e3.getValue());

        assertFalse(it.hasNext());
    }

    /* -------------------------------------------------
     *  equals, hashCode and toString
     * ------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        CompactCILinkedMap<String, String> copy = new CompactCILinkedMap<>(populatedMap);
        assertEquals(populatedMap, copy);
        assertEquals(populatedMap.hashCode(), copy.hashCode());

        // differ by case on a key – should still be equal
        copy.put("ONE", "1");
        assertEquals(populatedMap, copy);
    }

    @Test
    public void testToStringContainsEntries() {
        String s = populatedMap.toString();
        assertTrue(s.contains("One=1"));
        assertTrue(s.contains("Two=2"));
        assertTrue(s.contains("Three=3"));
    }

    /* -------------------------------------------------
     *  minus() and plus() – functional, non‑mutating operations
     * ------------------------------------------------- */

    @Test
    public void testMinus_RemovesKeyFromReturnedMapOnly() {
        Map<String, String> minusMap = populatedMap.minus("Two");
        // original map unchanged
        assertTrue(populatedMap.containsKey("Two"));
        // result does not contain the key (case‑insensitive)
        assertFalse(minusMap.containsKey("two"));
        assertEquals(2, minusMap.size());
    }

    @Test
    public void testMinus_NullArgument() {
        Map<String, String> result = populatedMap.minus(null);
        // null is a valid key for this map – the returned map should simply lack a null entry
        assertEquals(populatedMap.size(), result.size());
        assertFalse(result.containsKey(null));
    }

    @Test
    public void testPlus_AddsEntryToReturnedMapOnly() {
        Map<String, String> plusMap = populatedMap.plus("Four");
        // The plus method in CompactMap expects an Object that is a Map.Entry or a Map.
        // For the purpose of this test we use a simple Map.Entry implementation.
        // Since we cannot create a Map.Entry directly without a concrete class, we use
        // java.util.AbstractMap.SimpleEntry.
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("Four", "4");
        plusMap = populatedMap.plus(entry); // overload that accepts an entry

        // original map unchanged
        assertFalse(populatedMap.containsKey("Four"));
        // result contains the new mapping (case‑insensitive)
        assertTrue(plusMap.containsKey("four"));
        assertEquals("4", plusMap.get("FOUR"));
        assertEquals(populatedMap.size() + 1, plusMap.size());
    }

    @Test
    public void testPlus_NullEntry() {
        // Adding a null entry should behave like a no‑op and not throw.
        Map<String, String> result = populatedMap.plus(null);
        assertEquals(populatedMap, result);
    }

    /* -------------------------------------------------
     *  Static builder() method
     * ------------------------------------------------- */

    @Test
    public void testBuilderCreatesMapWithExpectedDefaults() {
        CompactMap.Builder<String, String> builder = CompactCILinkedMap.builder();
        CompactMap<String, String> built = builder.build();
        assertNotNull(built);
        assertTrue(built.isEmpty());
        // The builder creates a CompactMap (not specifically a CompactCILinkedMap),
        // but it must still respect case‑insensitivity when we request it.
        built.put("Alpha", "A");
        assertEquals("A", built.get("ALPHA"));
    }

    /* -------------------------------------------------
     *  Additional edge‑case tests
     * ------------------------------------------------- */

    @Test
    public void testPutAll_WithSelfReferenceDoesNotCauseInfiniteLoop() {
        // putAll with the map itself should be a no‑op and not corrupt the map.
        populatedMap.putAll(populatedMap);
        assertEquals(3, populatedMap.size());
        assertEquals("1", populatedMap.get("One"));
    }

    @Test
    public void testIterationAfterRemovalPreservesRemainingOrder() {
        populatedMap.remove("Two");
        List<String> expected = Arrays.asList("One", "Three");
        List<String> actual = new ArrayList<>(populatedMap.keySet());
        assertEquals(expected, actual);
    }

    @Test
    public void testContainsKey_NullKeyWhenMapIsEmpty() {
        assertFalse(emptyMap.containsKey(null));
        emptyMap.put(null, "nullValue");
        assertTrue(emptyMap.containsKey(null));
    }

    @Test
    public void testGet_NonExistingKeyReturnsNull() {
        assertNull(populatedMap.get("nonexistent"));
    }
}
