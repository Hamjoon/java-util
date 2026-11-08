package com.cedarsoftware.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class CompactMap_isEmpty_Test {

    private CompactMap<String, Object> map;

//     @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor – it creates an empty CompactMap.
        map = new CompactMap<>();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required.
        map = null;
    }

    public static void main(String[] args) {
        // 1. New map must be empty
        CompactMap<String, Object> map = new CompactMap<>();
        assert map.isEmpty() : "A newly constructed CompactMap must be empty";
        assert map.size() == 0 : "Size of a newly constructed CompactMap must be 0";
        // 2. After a put the map is non‑empty
        Object previous = map.put("key", "value");
        assert previous == null : "First insertion should return null";
        assert !map.isEmpty() : "Map should not be empty after a put";
        assert map.size() == 1 : "Size must be 1 after one entry";
        // 3. Removing the only entry makes the map empty again
        map.remove("key");
        assert map.isEmpty() : "Map must be empty after removing the sole entry";
        assert map.size() == 0 : "Size must be 0 after removal";
        // 4. clear() empties the map
        map.put("a", 1);
        map.put("b", 2);
        assert !map.isEmpty() : "Map should not be empty before clear";
        map.clear();
        assert map.isEmpty() : "Map must be empty after clear()";
        assert map.size() == 0 : "Size must be 0 after clear()";
        // 5. Builder‑created map starts empty
        CompactMap<String, Object> built = CompactMap.<String, Object>builder().caseSensitive(false).sortedOrder().compactSize(10).build();
        assert built.isEmpty() : "Builder‑created CompactMap must be empty initially";
        assert built.size() == 0 : "Size must be 0 for a newly built map";
        // 6. Builder‑created map respects insert/remove operations
        CompactMap<String, Object> built2 = CompactMap.<String, Object>builder().insertionOrder().mapType(LinkedHashMap.class).build();
        assert built2.isEmpty() : "New builder map should start empty";
        built2.put("x", 100);
        assert !built2.isEmpty() : "Map should not be empty after first put";
        built2.remove("x");
        assert built2.isEmpty() : "Map should be empty again after removing the only entry";
        // 7. Constructing from a non‑empty source yields a non‑empty map
        Map<String, Object> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        CompactMap<String, Object> fromSource = new CompactMap<>(source);
        assert !fromSource.isEmpty() : "Map constructed with a non‑empty source must not be empty";
        assert fromSource.size() == 2 : "Size must reflect the source map entries";
        // 8. putAll with an empty map leaves the map empty
        map.clear();
        map.putAll(Collections.emptyMap());
        assert map.isEmpty() : "putAll(empty) must not change emptiness";
        assert map.size() == 0 : "Size must remain 0 after putAll(empty)";
        // 9. putAll with a non‑empty map makes the map non‑empty
        Map<String, Object> other = new HashMap<>();
        other.put("k", "v");
        map.putAll(other);
        assert !map.isEmpty() : "putAll(non‑empty) must make map non‑empty";
        assert map.size() == 1 : "Size must reflect entries added via putAll";
        // 10. Removing a missing key from an empty map keeps it empty
        map.clear();
        Object removed = map.remove("missing");
        assert removed == null : "Removing absent key should return null";
        assert map.isEmpty() : "Removing absent key from empty map must stay empty";
        // 11. Removing a missing key from a non‑empty map keeps the map non‑empty
        map.put("present", 123);
        removed = map.remove("missing");
        assert removed == null : "Removing absent key should return null";
        assert !map.isEmpty() : "Map should stay non‑empty after removing a non‑existent key";
        assert map.size() == 1 : "Size must be unchanged after failed removal";
        // 12. entrySet().clear() empties the map
        map.clear();
        map.put("e", "value");
        map.entrySet().clear();
        assert map.isEmpty() : "entrySet().clear() must empty the map";
        // 13. values().clear() empties the map
        map.put("k", "v");
        map.values().clear();
        assert map.isEmpty() : "values().clear() must empty the map";
        // 14. keySet().clear() empties the map
        map.put("a", 1);
        map.put("b", 2);
        map.keySet().clear();
        assert map.isEmpty() : "keySet().clear() must empty the map";
        // 15. Null key handling
        map.put(null, "nullKey");
        assert !map.isEmpty() : "Putting a null key must make map non‑empty";
        assert map.size() == 1 : "Size must be 1 after inserting null key";
        removed = map.remove(null);
        assert "nullKey".equals(removed) : "Removed value must match the one stored under null key";
        assert map.isEmpty() : "Map must be empty after removing the null key entry";
        // 16. Null value handling
        map.put("nullValue", null);
        assert !map.isEmpty() : "Putting a null value must make map non‑empty";
        assert map.size() == 1 : "Size must be 1 after inserting null value";
        removed = map.remove("nullValue");
        assert removed == null : "Removed value should be null (the stored value)";
        assert map.isEmpty() : "Map must be empty after removing the entry with null value";
        // 17. Multiple puts and removes leave map empty
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assert map.size() == 3 : "Size must be 3 after three insertions";
        map.remove("a");
        assert map.size() == 2 : "Size must be 2 after removing first entry";
        map.remove("b");
        assert map.size() == 1 : "Size must be 1 after removing second entry";
        map.remove("c");
        assert map.isEmpty() : "Map must be empty after removing the last entry";
        // 18. Checks for putAll with varying sizes
        for (int entryCount : new int[] { 0, 1, 3 }) {
            map.clear();
            Map<String, Object> src = new HashMap<>();
            for (int i = 0; i < entryCount; i++) {
                src.put("k" + i, i);
            }
            map.putAll(src);
            boolean expectsEmpty = entryCount == 0;
            assert (expectsEmpty == map.isEmpty()) : "isEmpty() result does not match expectation for putAll with " + entryCount + " entries";
            assert map.size() == entryCount : "Size should equal number of entries supplied to putAll";
        }
        System.out.println("All checks passed");
    }

//     @Test
//     void newMapIsEmpty() {
//         assertTrue(map.isEmpty());
//         assertEquals(0, map.size());
//     }

//     @Test
//     void mapIsNotEmptyAfterPut() {
//         Object previous = map.put("key", "value");
//         assertNull(previous);
//         assertFalse(map.isEmpty());
//         assertEquals(1, map.size());
//     }

//     @Test
//     void mapBecomesEmptyAfterRemoveOnlyEntry() {
//         map.put("only", 42);
//         assertFalse(map.isEmpty());
//         Object removed = map.remove("only");
//         assertEquals(42, removed);
//         assertTrue(map.isEmpty());
//         assertEquals(0, map.size());
//     }

//     @Test
//     void mapIsEmptyAfterClear() {
//         map.put("a", 1);
//         map.put("b", 2);
//         assertFalse(map.isEmpty());
//         map.clear();
//         assertTrue(map.isEmpty());
//         assertEquals(0, map.size());
//     }

//     @Test
//     void builderCreatedMapIsEmptyInitially() {
//         CompactMap<String, Object> built = CompactMap.<String, Object>builder().caseSensitive(false).sortedOrder().compactSize(10).build();
//         assertTrue(built.isEmpty());
//         assertEquals(0, built.size());
//     }

//     @Test
//     void builderCreatedMapHandlesEmptinessAfterOperations() {
//         CompactMap<String, Object> built = CompactMap.<String, Object>builder().insertionOrder().mapType(LinkedHashMap.class).build();
//         assertTrue(built.isEmpty());
//         built.put("x", 100);
//         assertFalse(built.isEmpty());
//         built.remove("x");
//         assertTrue(built.isEmpty());
//     }

//     @Test
//     void mapCreatedFromSourceIsNotEmpty() {
//         Map<String, Object> source = new HashMap<>();
//         source.put("one", 1);
//         source.put("two", 2);
//         CompactMap<String, Object> fromSource = new CompactMap<>(source);
//         assertFalse(fromSource.isEmpty());
//         assertEquals(2, fromSource.size());
//     }

//     @Test
//     void putAllWithEmptyMapLeavesEmpty() {
//         map.putAll(Collections.emptyMap());
//         assertTrue(map.isEmpty());
//         assertEquals(0, map.size());
//     }

//     @Test
//     void putAllWithNonEmptyMapMakesMapNonEmpty() {
//         Map<String, Object> other = new HashMap<>();
//         other.put("k", "v");
//         map.putAll(other);
//         assertFalse(map.isEmpty());
//         assertEquals(1, map.size());
//     }

//     @Test
//     void removeAbsentKeyFromEmptyMapKeepsEmpty() {
//         Object removed = map.remove("missing");
//         assertNull(removed);
//         assertTrue(map.isEmpty());
//     }

//     @Test
//     void removeAbsentKeyFromNonEmptyMapKeepsNonEmpty() {
//         map.put("present", 123);
//         Object removed = map.remove("missing");
//         assertNull(removed);
//         assertFalse(map.isEmpty());
//         assertEquals(1, map.size());
//     }

//     @Test
//     void clearOnAlreadyEmptyMapLeavesEmpty() {
//         // first clear – map is already empty
//         map.clear();
//         assertTrue(map.isEmpty(), "clear() on an already empty map must stay empty");
//         assertEquals(0, map.size(), "Size must remain 0 after clear on empty map");
//     }

//     @Test
//     void entrySetClearEmptiesMap() {
//         map.put("e", "value");
//         map.entrySet().clear();
//         assertTrue(map.isEmpty());
//         assertEquals(0, map.size());
//     }

//     @Test
//     void valuesClearEmptiesMap() {
//         map.put("k", "v");
//         map.values().clear();
//         assertTrue(map.isEmpty());
//         assertEquals(0, map.size());
//     }

//     @Test
//     void keySetClearEmptiesMap() {
//         map.put("a", 1);
//         map.put("b", 2);
//         map.keySet().clear();
//         assertTrue(map.isEmpty());
//         assertEquals(0, map.size());
//     }

//     @Test
//     void nullKeyMakesMapNonEmptyAndRemovable() {
//         map.put(null, "nullKey");
//         assertFalse(map.isEmpty());
//         assertEquals(1, map.size());
//         Object removed = map.remove(null);
//         assertEquals("nullKey", removed);
//         assertTrue(map.isEmpty());
//     }

//     @Test
//     void nullValueMakesMapNonEmptyAndRemovable() {
//         map.put("nullValue", null);
//         assertFalse(map.isEmpty());
//         assertEquals(1, map.size());
//         Object removed = map.remove("nullValue");
//         assertNull(removed);
//         assertTrue(map.isEmpty());
//     }

//     @Test
//     void multiplePutsAndRemovesLeaveMapEmpty() {
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         assertFalse(map.isEmpty());
//         assertEquals(3, map.size());
//         map.remove("a");
//         assertFalse(map.isEmpty());
//         map.remove("b");
//         assertFalse(map.isEmpty());
//         map.remove("c");
//         assertTrue(map.isEmpty());
//         assertEquals(0, map.size());
//     }

//     @ParameterizedTest(name = "[{index}] putAll size={0} expectsEmpty={1}")
//     @CsvSource({ "0, true", "1, false", "3, false" })
//     void putAllVariousSizes(int entryCount, boolean expectsEmpty) {
//         for (int entryCount : new int[] { 0, 1, 3 }) {
//             map.clear();
//             Map<String, Object> source = new HashMap<>();
//             for (int i = 0; i < entryCount; i++) {
//                 source.put("k" + i, i);
//             }
//             map.putAll(source);
//             boolean expectsEmpty = entryCount == 0;
//             assertEquals(!expectsEmpty, !map.isEmpty());
//             assertEquals(entryCount, map.size());
//         }
//     }
}