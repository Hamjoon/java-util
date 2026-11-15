package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CompactCILinkedMap_getNewMap_Test {

    /**
     * Simple subclass that makes the protected method public for testing.
     */
    private static class TestableCompactCILinkedMap<K, V> extends CompactCILinkedMap<K, V> {

        public Map<K, V> callGetNewMap() {
            return super.getNewMap();
        }
    }

    /**
     * Subclass that allows us to control the value returned by {@code compactSize()}.
     * Used for the “invalid initial capacity” test cases.
     */
    private static class SizeOverrideCompactCILinkedMap<K, V> extends CompactCILinkedMap<K, V> {

        private final int overriddenSize;

        SizeOverrideCompactCILinkedMap(int overriddenSize) {
            this.overriddenSize = overriddenSize;
        }

        @Override
        protected int compactSize() {
            return overriddenSize;
        }

        public Map<K, V> callGetNewMap() {
            return super.getNewMap();
        }
    }

    @Test
    void getNewMap_returnsCaseInsensitiveMapInstance() {
        // GIVEN
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        // WHEN
        Map<String, String> newMap = map.callGetNewMap();
        // THEN
        assertNotNull(newMap, "The map returned by getNewMap() must not be null");
        assertTrue(newMap instanceof CaseInsensitiveMap, "The map should be an instance of CaseInsensitiveMap");
        assertTrue(newMap.isEmpty(), "The newly created map should be empty");
        // Verify that the backing map inside CaseInsensitiveMap is a LinkedHashMap.
        try {
            Field backingField = CaseInsensitiveMap.class.getDeclaredField("map");
            backingField.setAccessible(true);
            Object backing = backingField.get(newMap);
            assertTrue(backing instanceof LinkedHashMap, "The internal map of CaseInsensitiveMap must be a LinkedHashMap");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Reflection failed while checking internal map type: " + e);
        }
    }

    @Test
    void getNewMap_isCaseInsensitive() {
        // GIVEN
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        Map<String, String> newMap = map.callGetNewMap();
        // WHEN
        newMap.put("Key", "value");
        // THEN
        assertEquals("value", newMap.get("key"), "Lookup should be case‑insensitive (lower‑case key)");
        assertEquals("value", newMap.get("KEY"), "Lookup should be case‑insensitive (upper‑case key)");
    }

    @Test
    void getNewMap_preservesInsertionOrder() {
        TestableCompactCILinkedMap<String, Integer> map = new TestableCompactCILinkedMap<>();
        Map<String, Integer> newMap = map.callGetNewMap();
        newMap.put("b", 1);
        newMap.put("a", 2);
        newMap.put("c", 3);
        List<Map.Entry<String, Integer>> expected = List.of(new AbstractMap.SimpleEntry<>("b", 1), new AbstractMap.SimpleEntry<>("a", 2), new AbstractMap.SimpleEntry<>("c", 3));
        int i = 0;
        for (Map.Entry<String, Integer> entry : newMap.entrySet()) {
            assertEquals(expected.get(i).getKey(), entry.getKey(), "Insertion order key mismatch at position " + i);
            assertEquals(expected.get(i).getValue(), entry.getValue(), "Insertion order value mismatch at position " + i);
            i++;
        }
        assertEquals(expected.size(), i, "All entries should have been iterated");
    }

    @Test
    void getNewMap_isInitiallyEmpty() {
        TestableCompactCILinkedMap<Integer, String> map = new TestableCompactCILinkedMap<>();
        Map<Integer, String> newMap = map.callGetNewMap();
        assertEquals(0, newMap.size(), "New map size should be 0");
        assertTrue(newMap.isEmpty(), "New map should report isEmpty() == true");
    }

    /* --------------------------------------------------------------
     *  Case‑insensitivity tests
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_caseInsensitiveLookup() {
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        Map<String, String> newMap = map.callGetNewMap();
        newMap.put("Key", "value");
        assertEquals("value", newMap.get("key"), "Lookup should be case‑insensitive (lower‑case key)");
        assertEquals("value", newMap.get("KEY"), "Lookup should be case‑insensitive (upper‑case key)");
    }

    @Test
    void getNewMap_containsKeyIsCaseInsensitive() {
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        Map<String, String> newMap = map.callGetNewMap();
        newMap.put("key", "value");
        assertTrue(newMap.containsKey("KEY"), "containsKey should be case‑insensitive for a stored lower‑case key");
    }

    @Test
    void getNewMap_overwriteSameKeyDifferentCase() {
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        Map<String, String> newMap = map.callGetNewMap();
        newMap.put("Key", "first");
        // same logical key, different case
        newMap.put("kEy", "second");
        assertEquals(1, newMap.size(), "Putting two keys that differ only by case should result in a single entry");
        assertEquals("second", newMap.get("KEY"), "The second put should overwrite the first value");
    }

    /* --------------------------------------------------------------
     *  Non‑String key handling
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_nonStringKeyBehavesLikeNormalMap() {
        TestableCompactCILinkedMap<Integer, String> map = new TestableCompactCILinkedMap<>();
        Map<Integer, String> newMap = map.callGetNewMap();
        newMap.put(42, "answer");
        assertEquals("answer", newMap.get(42), "Non‑String key should be retrieved normally");
        assertTrue(newMap.containsKey(42), "containsKey should return true for the stored non‑String key");
    }

    /* --------------------------------------------------------------
     *  Null key handling
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_nullKeyThrowsNPE() {
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        Map<String, String> newMap = map.callGetNewMap();
        assertThrows(NullPointerException.class, () -> newMap.put(null, "value"), "Putting a null key should throw NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Invalid initial capacity (compactSize) handling
     * -------------------------------------------------------------- */
    @ParameterizedTest
    @ValueSource(ints = { -1, Integer.MAX_VALUE })
    void getNewMap_invalidCompactSizeThrowsIllegalArgumentException(int invalidSize) {
        // GIVEN a subclass that returns the supplied invalid size from compactSize()
        SizeOverrideCompactCILinkedMap<String, String> map = new SizeOverrideCompactCILinkedMap<>(invalidSize);
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, map::callGetNewMap, "Creating a LinkedHashMap with an illegal initial capacity should throw IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     *  Additional sanity: ensure isCaseInsensitive() and useCopyIterator()
     *  are inherited correctly (they are protected, we expose via reflection)
     * -------------------------------------------------------------- */
    @Test
    void getNewMap_inheritedFlagsAreCorrect() throws Exception {
        TestableCompactCILinkedMap<String, String> map = new TestableCompactCILinkedMap<>();
        // Use reflection to invoke the protected methods
        Method isCaseInsensitive = CompactCILinkedMap.class.getDeclaredMethod("isCaseInsensitive");
        isCaseInsensitive.setAccessible(true);
        assertTrue((Boolean) isCaseInsensitive.invoke(map), "CompactCILinkedMap should report isCaseInsensitive() == true");
        Method useCopyIterator = CompactCILinkedMap.class.getDeclaredMethod("useCopyIterator");
        useCopyIterator.setAccessible(true);
        // The actual contract of useCopyIterator() is not defined here; we just ensure it returns a boolean.
        assertNotNull(useCopyIterator.invoke(map), "useCopyIterator() should return a boolean value");
    }
}
