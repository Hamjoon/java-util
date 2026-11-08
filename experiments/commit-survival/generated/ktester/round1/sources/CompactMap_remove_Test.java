package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Iterator;

@ExtendWith(MockitoExtension.class)
class CompactMap_remove_Test {

    private CompactMap<String, String> map;

    /**
     * Subclass that forces a small {@code compactSize()} so that we can
     * trigger the Map → Object[] transition quickly.
     */
    private static class SmallCompactMap extends CompactMap<String, String> {

        @Override
        protected int compactSize() {
            // small value for testing
            return 8;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new CompactMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    /**
     * Retrieves the value of a private field from the given object.
     */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = CompactMap.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Sets the value of a private field on the given object.
     */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = CompactMap.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Empty map – remove must return {@code null}.
     */
    @Test
    void testRemoveFromEmptyMapReturnsNull() {
        assertNull(map.remove("anyKey"));
        assertTrue(map.isEmpty());
    }

    /**
     * Single‑entry map – removing a non‑existent key returns {@code null}.
     */
    @Test
    void testRemoveSingleEntryKeyNotPresentReturnsNull() {
        map.put("key1", "value1");
        assertEquals(1, map.size());
        assertNull(map.remove("missing"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("key1"));
    }

    /**
     * Single‑entry map – removing the existing key returns the value and empties the map.
     */
    @Test
    void testRemoveSingleEntryKeyPresentReturnsValueAndEmptiesMap() {
        map.put("key1", "value1");
        assertEquals("value1", map.remove("key1"));
        assertTrue(map.isEmpty());
    }

    /**
     * Two entries – removal should shrink the internal array to a single entry.
     */
    @Test
    void testRemoveWhenSizeTwoTransitionsToSingleEntry() {
        map.put("a", "A");
        map.put("b", "B");
        assertEquals(2, map.size());
        // Remove one key
        assertEquals("A", map.remove("a"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("b"));
        assertFalse(map.containsKey("a"));
    }

    /**
     * More than two but less than {@code compactSize()} – removal stays in the Object[] representation.
     */
    @Test
    void testRemoveWhenSizeGreaterThanTwoAndLessThanCompactSize() {
        // Default compactSize() is 80, use 5 entries.
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");
        map.put("k4", "v4");
        map.put("k5", "v5");
        assertEquals(5, map.size());
        // Remove a middle entry
        assertEquals("v3", map.remove("k3"));
        assertEquals(4, map.size());
        assertFalse(map.containsKey("k3"));
        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsKey("k2"));
        assertTrue(map.containsKey("k4"));
        assertTrue(map.containsKey("k5"));
    }

    /**
     * Size > {@code compactSize()} – removal should eventually switch back to the array representation.
     */
    @Test
    void testRemoveWhenSizeGreaterThanCompactSizeAndShrinksBackToArray() {
        // default is 80, but we query it to stay robust
        int compactSize = map.compactSize();
        // ensure we are in the delegated Map representation
        int initialSize = compactSize + 5;
        // Fill the map
        for (int i = 0; i < initialSize; i++) {
            map.put("k" + i, "v" + i);
        }
        assertEquals(initialSize, map.size());
        // Remove entries until we reach exactly compactSize()
        for (int i = 0; i < 5; i++) {
            assertNotNull(map.remove("k" + i));
        }
        assertEquals(compactSize, map.size());
        // Verify map still behaves correctly after the transition
        assertTrue(map.containsKey("k" + 5));
        assertEquals("v5", map.get("k5"));
        // Remove one more entry and ensure size decrements correctly
        assertEquals("v5", map.remove("k5"));
        assertEquals(compactSize - 1, map.size());
        assertFalse(map.containsKey("k5"));
    }

    @ParameterizedTest(name = "[{index}] remove({0}) → {1}")
    @CsvSource({ // first entry matches
    "'A',   'value1',  true", // second entry matches
    "'B',   'value2',  true", // no match
    "'C',   ,          false" })
    void testRemoveObjectArraySizeTwo(String key, String expected, boolean present) throws Exception {
        // Setup: two entries → internal Object[] of length 4
        map.put("A", "value1");
        map.put("B", "value2");
        assertEquals(2, map.size());
        // Act
        String result = map.remove(key);
        // Assert return value
        assertEquals(expected, result);
        // Verify internal state after removal
        if (present) {
            // One entry should remain, map size == 1
            assertEquals(1, map.size());
            assertTrue(map.containsKey(present ? (key.equals("A") ? "B" : "A") : null));
        } else {
            // No change – still two entries
            assertEquals(2, map.size());
            assertTrue(map.containsKey("A"));
            assertTrue(map.containsKey("B"));
        }
    }

    @ParameterizedTest(name = "[{index}] remove({0}) → {1}")
    @CsvSource({ "'B', 'valueB'", "'A', 'valueA'" })
    void testRemoveObjectArrayMultiple(String key, String expected) throws Exception {
        // Three entries → internal Object[] of length 6
        map.put("A", "valueA");
        map.put("B", "valueB");
        map.put("C", "valueC");
        assertEquals(3, map.size());
        // Act
        String removed = map.remove(key);
        assertEquals(expected, removed);
        // Size must shrink to 2 and internal representation must stay an Object[]
        assertEquals(2, map.size());
        assertFalse(map.containsKey(key));
        assertTrue(map.containsKey("A"));
        assertTrue(map.containsKey("B"));
        assertTrue(map.containsKey("C"));
        // Verify that the private 'val' field is still an Object[]
        Object internal = getPrivateField(map, "val");
        assertTrue(internal instanceof Object[]);
        // 2 entries * 2
        assertEquals(4, ((Object[]) internal).length);
    }

    @Test
    void testRemoveMapTransitionToArray() throws Exception {
        SmallCompactMap smallMap = new SmallCompactMap();
        // Fill map to size 9 (> compactSize) → internal representation is a Map
        for (int i = 0; i < 9; i++) {
            smallMap.put("k" + i, "v" + i);
        }
        assertEquals(9, smallMap.size());
        assertTrue(getPrivateField(smallMap, "val") instanceof Map);
        // Remove one entry to bring size down to exactly compactSize (8)
        String removed = smallMap.remove("k8");
        assertEquals("v8", removed);
        assertEquals(8, smallMap.size());
        // After removal the internal representation must have switched to Object[]
        Object internal = getPrivateField(smallMap, "val");
        assertTrue(internal instanceof Object[]);
        // compactSize * 2
        assertEquals(16, ((Object[]) internal).length);
        // Verify map still works normally
        assertTrue(smallMap.containsKey("k0"));
        assertEquals("v0", smallMap.get("k0"));
    }

    @Test
    void testRemoveMapNoTransition() throws Exception {
        SmallCompactMap smallMap = new SmallCompactMap();
        // Fill map to size 12 (> compactSize) → stays as a Map
        for (int i = 0; i < 12; i++) {
            smallMap.put("k" + i, "v" + i);
        }
        assertEquals(12, smallMap.size());
        assertTrue(getPrivateField(smallMap, "val") instanceof Map);
        // Remove a key; size stays > compactSize, representation must stay Map
        String removed = smallMap.remove("k5");
        assertEquals("v5", removed);
        assertEquals(11, smallMap.size());
        Object internal = getPrivateField(smallMap, "val");
        assertTrue(internal instanceof Map);
        assertFalse(smallMap.containsKey("k5"));
    }

    @Test
    void testRemoveKeyNotFoundInObjectArray() {
        map.put("X", "xVal");
        map.put("Y", "yVal");
        map.put("Z", "zVal");
        assertEquals(3, map.size());
        assertNull(map.remove("absent"));
        assertEquals(3, map.size());
        assertTrue(map.containsKey("X"));
        assertTrue(map.containsKey("Y"));
        assertTrue(map.containsKey("Z"));
    }

    @Test
    void testRemoveKeyNotFoundInMap() {
        SmallCompactMap smallMap = new SmallCompactMap();
        for (int i = 0; i < 10; i++) {
            smallMap.put("k" + i, "v" + i);
        }
        assertEquals(10, smallMap.size());
        assertNull(smallMap.remove("absent"));
        assertEquals(10, smallMap.size());
    }

    @Test
    void testRemoveNullKey() {
        map.put(null, "nullValue");
        map.put("other", "otherValue");
        assertEquals(2, map.size());
        String removed = map.remove(null);
        assertEquals("nullValue", removed);
        assertEquals(1, map.size());
        assertFalse(map.containsKey(null));
        assertTrue(map.containsKey("other"));
    }

    @Test
    void testRemoveCorruptObjectArrayOddLength() throws Exception {
        // Manually inject a malformed Object[] (odd number of elements)
        // length 3
        Object[] corrupt = new Object[] { "k1", "v1", "k2" };
        setPrivateField(map, "val", corrupt);
        // The remove implementation will attempt to read entries[i+1] for i == 2,
        // causing an ArrayIndexOutOfBoundsException.
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> map.remove("anyKey"));
    }
}
