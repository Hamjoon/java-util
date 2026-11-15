import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ConcurrentModificationException;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_clear_Test {

    /**
     * Helper subclass that exposes the protected {@code compactSize()} method.
     */
//     class LocalTestCompactMap<K, V> extends CompactMap<K, V> {

        private final int customSize;

        LocalTestCompactMap(int size) {
            this.customSize = size;
        }

//         @Override
//         protected int compactSize() {
//             return customSize;
//         }

//         int exposedCompactSize() {
//             return super.compactSize();
//         }
    }

    /**
     * Helper subclass that exposes the protected {@code compactSize()} method.
     */
//     class TestCompactMap<K, V> extends CompactMap<K, V> {

        private final int customSize;

        TestCompactMap(int size) {
            this.customSize = size;
        }

//         @Override
//         protected int compactSize() {
//             return customSize;
//         }

        /**
         * Expose for test verification.
         */
//         int exposedCompactSize() {
//             return super.compactSize();
//         }
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void clearOnEmptyMapLeavesItEmpty() {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         // Verify the map starts empty
//         assertTrue(map.isEmpty(), "Map should be empty initially");
//         assertEquals(0, map.size(), "Size should be 0 initially");
//         // Invoke clear on an already‑empty map
//         map.clear();
//         // Verify the state is still empty
//         assertTrue(map.isEmpty(), "Map should remain empty after clear()");
//         assertEquals(0, map.size(), "Size should remain 0 after clear()");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void clearAfterSingleEntryResultsInEmptyMap() {
//         CompactMap<String, Double> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Double> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
//         };
//         // Add a single entry – this exercises the “single entry” internal state.
//         assertNull(map.put("value", 10.0), "First put should return null");
//         assertEquals(1, map.size(), "Size should be 1 after one put");
//         assertFalse(map.isEmpty(), "Map should not be empty after one put");
//         // Clear the map
//         map.clear();
//         // Verify that the map is empty again
//         assertTrue(map.isEmpty(), "Map should be empty after clear()");
//         assertEquals(0, map.size(), "Size should be 0 after clear()");
//         assertNull(map.get("value"), "Getting a removed key should return null");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void clearAfterMultipleEntriesResetsMap() {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 2;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "single";
//             }
//         };
//         // Populate the map with more entries than the compact size.
//         map.put("a", 1);
//         map.put("b", 2);
//         // At this point the internal representation should be a backing map.
//         map.put("c", 3);
//         assertEquals(3, map.size(), "Size should be 3 after three puts");
//         assertFalse(map.isEmpty(), "Map should not be empty after three puts");
//         assertEquals(1, map.get("a"));
//         assertEquals(2, map.get("b"));
//         assertEquals(3, map.get("c"));
//         // Now clear the map.
//         map.clear();
//         // All observable state should be reset.
//         assertTrue(map.isEmpty(), "Map should be empty after clear()");
//         assertEquals(0, map.size(), "Size should be 0 after clear()");
//         assertNull(map.get("a"));
//         assertNull(map.get("b"));
//         assertNull(map.get("c"));
//         // Ensure the map can be reused after clear().
//         map.put("x", 42);
//         assertEquals(1, map.size());
//         assertEquals(42, map.get("x"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void clearOnNonEmptyMapLeavesItEmpty() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(5).build();
//         map.put("one", 1);
//         map.put("two", 2);
//         map.put("three", 3);
//         // verify presence before clear
//         assertEquals(3, map.size());
//         assertEquals(1, map.get("one"));
//         assertEquals(2, map.get("two"));
//         assertEquals(3, map.get("three"));
//         assertFalse(map.isEmpty());
//         map.clear();
//         // expectations after clear
//         assertTrue(map.isEmpty(), "Map should be empty after clear()");
//         assertEquals(0, map.size(), "Size should be 0 after clear()");
//         assertNull(map.get("one"));
//         assertNull(map.get("two"));
//         assertNull(map.get("three"));
//         assertTrue(map.entrySet().isEmpty(), "Entry set must be empty after clear()");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void iterationViewsAreEmptyAfterClear() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().compactSize(4).build();
//         map.put("a", "A");
//         map.put("b", "B");
//         map.put("c", "C");
//         map.clear();
//         // keySet
//         assertFalse(map.keySet().iterator().hasNext(), "keySet iterator should be empty");
//         // values
//         assertFalse(map.values().iterator().hasNext(), "values iterator should be empty");
//         // entrySet
//         assertFalse(map.entrySet().iterator().hasNext(), "entrySet iterator should be empty");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void internalValResetToEmptyMapAfterClear() throws Exception {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         // Populate to force a non‑EMPTY_MAP state
//         map.put("x", 1);
//         map.put("y", 2);
//         // sanity check: val is not the sentinel yet
//         Field valField = CompactMap.class.getDeclaredField("val");
//         valField.setAccessible(true);
//         Object before = valField.get(map);
//         assertNotEquals(CompactMap.EMPTY_MAP, before, "val should not be EMPTY_MAP before clear()");
//         map.clear();
//         Object after = valField.get(map);
//         assertEquals(CompactMap.EMPTY_MAP, after, "val must be reset to EMPTY_MAP after clear()");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void clearOnAlreadyEmptyMapDoesNotThrowAndResetsVal() throws Exception {
//         CompactMap<String, Integer> map = new CompactMap<>();
//         // verify initial state
//         assertTrue(map.isEmpty());
//         Field valField = CompactMap.class.getDeclaredField("val");
//         valField.setAccessible(true);
//         assertEquals(CompactMap.EMPTY_MAP, valField.get(map));
//         // clear should be a no‑op but must not throw
//         assertDoesNotThrow(map::clear, "clear() on empty map must not throw");
//         // still empty and val unchanged
//         assertTrue(map.isEmpty());
//         assertEquals(CompactMap.EMPTY_MAP, valField.get(map));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void caseInsensitiveMapClearedProperly() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(false).compactSize(5).build();
//         map.put("key", "value");
//         map.put("Another", "val2");
//         assertTrue(map.containsKey("KEY"));
//         assertTrue(map.containsKey("another"));
//         map.clear();
//         assertTrue(map.isEmpty());
//         assertFalse(map.containsKey("KEY"));
//         assertFalse(map.containsKey("another"));
//         // configuration must stay case‑insensitive
//         Map<String, Object> cfg = map.getConfig();
//         assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void sortedOrderingMapClearedAndRetainsOrdering() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().compactSize(5).build();
//         map.put("b", 2);
//         map.put("a", 1);
//         map.put("c", 3);
//         // verify natural order before clear
//         List<String> before = new ArrayList<>();
//         for (Map.Entry<String, Integer> e : map.entrySet()) {
//             before.add(e.getKey());
//         }
//         assertEquals(List.of("a", "b", "c"), before, "Keys must be sorted before clear()");
//         map.clear();
//         // after clear, insert new entries and verify ordering again
//         map.put("d", 4);
//         map.put("e", 5);
//         List<String> after = new ArrayList<>();
//         for (Map.Entry<String, Integer> e : map.entrySet()) {
//             after.add(e.getKey());
//         }
//         assertEquals(List.of("d", "e"), after, "Keys must stay sorted after clear()");
//         // ordering config unchanged
//         assertEquals(CompactMap.SORTED, map.getConfig().get(CompactMap.ORDERING));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void insertionOrderingMapClearedAndPreservesInsertionOrder() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().compactSize(5).build();
//         map.put("A", 1);
//         map.put("B", 2);
//         map.put("C", 3);
//         List<String> before = new ArrayList<>();
//         for (Map.Entry<String, Integer> e : map.entrySet()) {
//             before.add(e.getKey());
//         }
//         assertEquals(List.of("A", "B", "C"), before, "Insertion order before clear()");
//         map.clear();
//         // new inserts after clear
//         map.put("X", 10);
//         map.put("Y", 20);
//         List<String> after = new ArrayList<>();
//         for (Map.Entry<String, Integer> e : map.entrySet()) {
//             after.add(e.getKey());
//         }
//         assertEquals(List.of("X", "Y"), after, "Insertion order after clear()");
//         // config unchanged
//         assertEquals(CompactMap.INSERTION, map.getConfig().get(CompactMap.ORDERING));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void singleKeyOptionClearedProperly() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().singleValueKey("id").compactSize(5).build();
//         map.put("id", "first");
//         assertEquals(1, map.size());
//         assertEquals("first", map.get("id"));
//         map.clear();
//         assertTrue(map.isEmpty());
//         assertEquals(0, map.size());
//         assertNull(map.get("id"));
//         // configuration must still contain the SINGLE_KEY option
//         assertEquals("id", map.getConfig().get(CompactMap.SINGLE_KEY));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void subsequentPutsAfterClearWorkAsExpected() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(3).build();
//         map.put("a", 1);
//         map.put("b", 2);
//         map.clear();
//         map.put("x", 100);
//         map.put("y", 200);
//         map.put("z", 300);
//         assertEquals(3, map.size());
//         assertEquals(100, map.get("x"));
//         assertEquals(200, map.get("y"));
//         assertEquals(300, map.get("z"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void iteratorInvalidatedAfterClearThrowsCME() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(5).build();
//         map.put("one", 1);
//         map.put("two", 2);
//         Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
//         // clear the map while iterator is still alive
//         map.clear();
//         assertThrows(ConcurrentModificationException.class, iterator::next, "Iterator should detect concurrent modification after clear()");
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void compactSizeRemainsUnchangedAfterClear() throws Exception {
//         LocalTestCompactMap<String, Integer> map = new LocalTestCompactMap<>(7);
//         map.put("a", 1);
//         map.put("b", 2);
//         map.clear();
//         assertEquals(7, map.exposedCompactSize(), "compactSize() must return the same value after clear()");
//     }
}