package com.cedarsoftware.util;

// import org.junit.*;
// import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompactMap_isEmpty_Test {

    /**
     * Helper to create a CompactMap via the builder with optional configuration.
     */
//     private static <K, V> CompactMap<K, V> buildMap(Map<String, Object> config) {
//         // Use a type‑witness so the builder is correctly parameterised.
//         CompactMap.Builder<K, V> builder = CompactMap.<K, V>builder();
//         if (config != null) {
//             config.forEach((k, v) -> {
//                 // The builder API is fluent; we handle only the options we need for the tests.
//                 switch(k) {
//                     case CompactMap.CASE_SENSITIVE:
//                         builder.caseSensitive((Boolean) v);
//                         break;
//                     case CompactMap.COMPACT_SIZE:
//                         builder.compactSize((Integer) v);
//                         break;
//                     case CompactMap.MAP_TYPE:
//                         builder.mapType((Class<? extends Map>) v);
//                         break;
//                     case CompactMap.SOURCE_MAP:
//                         builder.sourceMap((Map<K, V>) v);
//                         break;
//                     default:
//                 }
//             });
//         }
//         return builder.build();
//     }

    /**
     * Verify that a freshly created CompactMap reports {@code true} for {@code isEmpty()}.
     */
    @Test
    void testIsEmptyOnNewMap() {
        CompactMap<String, Object> map = new CompactMap<>();
        assertTrue(map.isEmpty(), "Newly created CompactMap should be empty");
        assertEquals(0, map.size(), "Size of a new CompactMap should be 0");
    }

    /**
     * After inserting a single entry the map must no longer be empty.
     */
    @Test
    void testIsEmptyAfterPut() {
        CompactMap<String, Object> map = new CompactMap<>();
        map.put("key", "value");
        assertFalse(map.isEmpty(), "Map should not be empty after a put");
        assertEquals(1, map.size(), "Size should be 1 after one insertion");
    }

    /**
     * Removing the only entry should bring the map back to the empty state.
     */
    @Test
    void testIsEmptyAfterRemoveOnlyEntry() {
        CompactMap<String, Object> map = new CompactMap<>();
        map.put("only", 42);
        assertFalse(map.isEmpty(), "Map should not be empty after insertion");
        Object removed = map.remove("only");
        assertEquals(42, removed, "Removed value should be the one that was inserted");
        assertTrue(map.isEmpty(), "Map should be empty after removing the only entry");
        assertEquals(0, map.size(), "Size should be 0 after removal");
    }

    /**
     * Calling {@code clear()} must reset the map to the empty state regardless of its prior size.
     */
    @Test
    void testIsEmptyAfterClear() {
        CompactMap<String, Object> map = new CompactMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertFalse(map.isEmpty(), "Map should not be empty after multiple puts");
        map.clear();
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, map.size(), "Size should be 0 after clear()");
    }

    /**
     * Verify the behaviour with a custom configuration (compactSize = 3 and a LinkedHashMap backing).
     * This mirrors the pattern used in the original test suite and ensures that the internal sentinel
     * handling works for subclassed implementations.
     */
//     @Test
//     void testIsEmptyWithCustomSubclass() {
//         CompactMap<String, Object> map = new CompactMap<String, Object>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         // Initially empty
//         assertTrue(map.isEmpty(), "Custom CompactMap should start empty");
//         assertEquals(0, map.size());
//         // Add a single entry – should become non‑empty
//         assertNull(map.put("value", 10.0d), "First put should return null");
//         assertFalse(map.isEmpty(), "Map should not be empty after first insertion");
//         assertEquals(1, map.size());
//         // Add a second entry – still non‑empty
//         assertNull(map.put("alpha", "beta"), "Second put should return null");
//         assertFalse(map.isEmpty(), "Map should remain non‑empty after second insertion");
//         assertEquals(2, map.size());
//         // Remove one entry – still non‑empty
//         assertEquals("beta", map.remove("alpha"), "Removed value should be 'beta'");
//         assertFalse(map.isEmpty(), "Map should stay non‑empty after removing one of two entries");
//         assertEquals(1, map.size());
//         // Remove the last entry – should become empty again
//         assertEquals(10.0d, map.remove("value"), "Removed value should be 10.0d");
//         assertTrue(map.isEmpty(), "Map should be empty after removing the last entry");
//         assertEquals(0, map.size());
//     }

    @Test
    void testIsEmpty_NewMapViaBuilder() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();
        assertTrue(map.isEmpty(), "Builder‑created map should be empty");
    }

    @Test
    void testIsEmpty_AfterPutViaBuilder() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();
        map.put("k", "v");
        assertFalse(map.isEmpty(), "Map should not be empty after a put");
    }

    @Test
    void testIsEmpty_InsertThenRemoveSameKey() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();
        map.put("k", "v");
        map.remove("k");
        assertTrue(map.isEmpty(), "Map should be empty after removing the only entry");
    }

    @Test
    void testIsEmpty_InsertThenClear() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();
        map.put("k", "v");
        map.clear();
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
    }

//     @Test
//     void testIsEmpty_NewMapWithPrepopulatedSource() {
//         Map<String, Integer> source = Map.of("a", 1);
//         Map<String, Object> options = new HashMap<>();
//         options.put(CompactMap.SOURCE_MAP, source);
//         CompactMap<String, Integer> map = CompactMap.newMap(options);
//         assertFalse(map.isEmpty(), "Map created with a non‑empty source should not be empty");
//         assertEquals(1, map.size());
//     }

    @Test
    void testIsEmpty_TwoPutsOneRemove() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        map.put("first", 1);
        map.put("second", 2);
        map.remove("first");
        assertFalse(map.isEmpty(), "Map should still be non‑empty after removing only one of two entries");
        assertEquals(1, map.size());
    }

    @Test
    void testIsEmpty_RemoveNonExistentFromEmpty() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        map.remove("ghost");
        assertTrue(map.isEmpty(), "Removing a non‑existent key from an empty map should keep it empty");
    }

    @Test
    void testIsEmpty_RemoveDifferentKeyFromNonEmpty() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        map.put("real", 10);
        map.remove("fake");
        assertFalse(map.isEmpty(), "Map should stay non‑empty when removing a key that does not exist");
        assertEquals(1, map.size());
    }

//     @Test
//     void testIsEmpty_PutAllEmptyMap() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
//         map.putAll(Collections.emptyMap());
//         assertTrue(map.isEmpty(), "putAll with an empty map should leave CompactMap empty");
//     }

//     @Test
//     void testIsEmpty_PutAllNonEmptyMap() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
//         Map<String, Integer> source = Map.of("x", 1, "y", 2);
//         map.putAll(source);
//         assertFalse(map.isEmpty(), "putAll with a non‑empty source should make CompactMap non‑empty");
//         assertEquals(2, map.size());
//     }

    @Test
    void testIsEmpty_ClearAlreadyEmpty() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        // clear on empty map
        map.clear();
        assertTrue(map.isEmpty(), "Calling clear on an already empty map should keep it empty");
    }

    @Test
    void testIsEmpty_NullKey() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        map.put(null, 123);
        assertFalse(map.isEmpty(), "Map should not be empty after inserting a null key");
        assertEquals(1, map.size());
    }

    @Test
    void testIsEmpty_NullValue() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        map.put("key", null);
        assertFalse(map.isEmpty(), "Map should not be empty after inserting a null value");
        assertEquals(1, map.size());
    }

    @Test
    void testIsEmpty_ReplaceExistingKey() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
        map.put("k", 1);
        // replace
        map.put("k", 2);
        assertFalse(map.isEmpty(), "Map should stay non‑empty after replacing the value of an existing key");
        assertEquals(1, map.size());
        assertEquals(2, map.get("k").intValue());
    }

    @Test
    void testIsEmpty_BuilderCustomConfigNoSource() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().caseSensitive(false).build();
        assertTrue(map.isEmpty(), "Builder with custom config but no entries should produce an empty map");
        assertEquals(0, map.size());
    }
}