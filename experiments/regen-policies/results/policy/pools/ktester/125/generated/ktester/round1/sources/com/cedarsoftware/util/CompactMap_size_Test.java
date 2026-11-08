import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_size_Test {

    /**
     * Simple dummy class used in one of the tests.
     */
    class Dummy {

        @Override
        public String toString() {
            return "Dummy";
        }
    }

    @Nested
    @DisplayName("Sanity checks using public API")
    class PublicApiSanity {

//         @Test
//         @DisplayName("size() after normal puts (covers single‑value and compact array)")
//         void sizeViaPuts() {
//             CompactMap<String, Object> map = newTestMap();
//             // single‑value state
//             map.put("value", "single");
//             assertEquals(1, map.size());
//             // transition to compact array (compactSize = 4)
//             map.put("a", 1);
//             map.put("b", 2);
//             assertEquals(3, map.size());
//         }
    }

    private static Stream<Object[]> objectArrayProvider() {
        return Stream.of(new Object[] { new Object[] { "k1", "v1", "k2", "v2" }, 2, "length 4 (even) → 2" }, new Object[] { new Object[] { "k1", "v1", "k2" }, 1, "length 3 (odd) → 1" }, new Object[] { new Object[] {}, 0, "empty array → 0" }, new Object[] { new Object[] { "onlyKey" }, 0, "length 1 → 0 (shift right)" }, new Object[] { new Object[] { "k1", "v1" }, 1, "length 2 → 1" });
    }

    private static Stream<Object[]> mapProvider() {
        // Empty HashMap
        Map<String, Object> empty = new HashMap<>();
        // Map with three entries
        Map<String, Object> three = new HashMap<>();
        three.put("a", 1);
        three.put("b", 2);
        three.put("c", 3);
        // Map with a single null key/value
        Map<String, Object> nullEntry = new HashMap<>();
        nullEntry.put(null, null);
        // TreeMap with five entries
        Map<String, Object> tree = new TreeMap<>();
        for (int i = 0; i < 5; i++) {
            tree.put("k" + i, i);
        }
        // ConcurrentHashMap with ten entries
        Map<String, Object> concurrent = new ConcurrentHashMap<>();
        for (int i = 0; i < 10; i++) {
            concurrent.put("c" + i, i);
        }
        return Stream.of(new Object[] { empty, 0, "empty HashMap" }, new Object[] { three, 3, "HashMap with three entries" }, new Object[] { nullEntry, 1, "Map with a single null key/value" }, new Object[] { tree, 5, "TreeMap with five entries" }, new Object[] { concurrent, 10, "ConcurrentHashMap with ten entries" });
    }

    /**
     * Creates a concrete {@link CompactMap} with the abstract methods
     * overridden using simple defaults. The concrete instance is suitable
     * for reflection‑based manipulation of the {@code val} field.
     */
//     private static CompactMap<String, Object> newTestMap() {
//         return new CompactMap<>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 // any positive number – not used in these tests
//                 return 4;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new HashMap<>();
//             }
//         };
//     }

    /**
     * Utility that forces the private {@code val} field of a CompactMap
     * instance to the supplied value using reflection.
     */
//     private static void setInternalVal(CompactMap<?, ?> map, Object newVal) {
//         try {
//             Field f = CompactMap.class.getDeclaredField("val");
//             f.setAccessible(true);
//             f.set(map, newVal);
//         } catch (ReflectiveOperationException e) {
//             throw new RuntimeException("Unable to set CompactMap.val via reflection", e);
//         }
//     }

    /**
     * Verify size() on a freshly created map (empty state).
     */
//     @Test
//     void sizeEmptyMap() {
//         Map<String, Object> map = new CompactMap<>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 // any value >= 2
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new HashMap<>();
//             }
//         };
//         assertEquals(0, map.size(), "Empty map should report size 0");
//         assertTrue(map.isEmpty(), "Empty map should be empty");
//     }

    /**
     * Verify size() when the map holds exactly one entry (single‑value state).
     */
//     @Test
//     void sizeSingleEntry() {
//         Map<String, Object> map = new CompactMap<>() {
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
//                 return new HashMap<>();
//             }
//         };
//         assertNull(map.put("value", 42), "First put should return null");
//         assertEquals(1, map.size(), "Map with a single entry must report size 1");
//         assertFalse(map.isEmpty(), "Map should not be empty after one put");
//     }

    /**
     * Verify size() while the internal storage is a compact Object[] array.
     * This occurs when the number of entries is between 2 and {@code compactSize()} (inclusive).
     */
//     @Test
//     void sizeCompactArray() {
//         // compactSize = 4 → array state for 2,3,4 entries
//         Map<String, Object> map = new CompactMap<>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 return 4;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new HashMap<>();
//             }
//         };
//         // add first entry (single‑value state)
//         map.put("value", "first");
//         // add second entry – triggers transition to compact array
//         map.put("alpha", "A");
//         assertEquals(2, map.size(), "After two puts size should be 2 (compact array)");
//         // add a third entry – still compact array
//         map.put("beta", "B");
//         assertEquals(3, map.size(), "After three puts size should be 3 (compact array)");
//         // add a fourth entry – still compact array because compactSize == 4
//         map.put("gamma", "C");
//         assertEquals(4, map.size(), "After four puts size should be 4 (still compact array)");
//     }

    /**
     * Verify size() when the map has grown beyond {@code compactSize()} and therefore uses a backing {@link Map}.
     */
//     @Test
//     void sizeBackingMap() {
//         // compactSize = 3 → after 4 entries we expect a backing map
//         Map<String, Object> map = new CompactMap<>() {
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
//                 // Use LinkedHashMap to keep insertion order (any Map implementation works)
//                 return new LinkedHashMap<>();
//             }
//         };
//         // Populate up to compactSize (array state)
//         map.put("value", 0);
//         map.put("k1", 1);
//         map.put("k2", 2);
//         assertEquals(3, map.size(), "Size should be 3 before exceeding compactSize");
//         // Adding the fourth entry forces transition to a backing map
//         map.put("k3", 3);
//         assertEquals(4, map.size(), "Size should be 4 after exceeding compactSize (backing map)");
//         // Verify that further puts continue to reflect the correct size
//         map.put("k4", 4);
//         map.put("k5", 5);
//         assertEquals(6, map.size(), "Size should reflect total entries in backing map");
//     }

    /**
     * Verify size() after removals that cause transitions between states.
     */
//     @Test
//     void sizeTransitionsOnRemoval() {
//         // compactSize = 3 → transition point at 4 entries
//         Map<String, Object> map = new CompactMap<>() {
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
//                 return new HashMap<>();
//             }
//         };
//         // Build up to backing‑map state
//         map.put("value", "v");
//         map.put("a", 1);
//         map.put("b", 2);
//         // now size == 4, backing map
//         map.put("c", 3);
//         assertEquals(4, map.size());
//         // Remove entries until we drop back to compact array (size == 3)
//         map.remove("c");
//         assertEquals(3, map.size(), "After removal should fall back to compact array (size 3)");
//         // Remove another entry to reach single‑value state
//         map.remove("b");
//         assertEquals(2, map.size(), "After another removal size should be 2 (compact array)");
//         // Remove to single entry
//         map.remove("a");
//         assertEquals(1, map.size(), "After removal size should be 1 (single‑value state)");
//         assertFalse(map.isEmpty());
//         // Finally remove the last entry
//         map.remove("value");
//         assertEquals(0, map.size(), "Map should be empty after removing last entry");
//         assertTrue(map.isEmpty());
//     }

//     @ParameterizedTest(name = "Object[] case: {2}")
//     @MethodSource("objectArrayProvider")
//     @DisplayName("size() with internal Object[] storage")
//     void sizeWithObjectArray(Object[] internalArray, int expectedSize, String description) {
//         CompactMap<String, Object> map = newTestMap();
//         setInternalVal(map, internalArray);
//         assertEquals(expectedSize, map.size(), () -> "Unexpected size for Object[] case: " + description);
//     }

//     @Test
//     @DisplayName("size() when internal value is a primitive int[]")
//     void sizeWithPrimitiveArray() {
//         CompactMap<String, Object> map = newTestMap();
//         // primitive array, not an Object[]
//         setInternalVal(map, new int[] { 1, 2, 3 });
//         assertEquals(1, map.size(), "Primitive arrays are not instanceof Object[]; size() should return 1");
//     }

//     @ParameterizedTest(name = "Map case: {2}")
//     @MethodSource("mapProvider")
//     @DisplayName("size() when internal value is a Map")
//     void sizeWithMap(Map<String, Object> backingMap, int expectedSize, String description) {
//         CompactMap<String, Object> map = newTestMap();
//         setInternalVal(map, backingMap);
//         assertEquals(expectedSize, map.size(), () -> "Unexpected size for Map case: " + description);
//     }

//     @Test
//     @DisplayName("size() when internal value equals CompactMap.EMPTY_MAP")
//     void sizeWhenEmptyMapSentinel() {
//         CompactMap<String, Object> map = newTestMap();
//         // The sentinel is a private static final String, access via reflection
//         try {
//             Field emptyField = CompactMap.class.getDeclaredField("EMPTY_MAP");
//             emptyField.setAccessible(true);
//             Object emptySentinel = emptyField.get(null);
//             setInternalVal(map, emptySentinel);
//         } catch (ReflectiveOperationException e) {
//             throw new RuntimeException(e);
//         }
//         assertEquals(0, map.size(), "EMPTY_MAP sentinel should result in size 0");
//     }

//     @Test
//     @DisplayName("size() with a custom non‑Map, non‑Array object")
//     void sizeWithCustomObject() {
//         CompactMap<String, Object> map = newTestMap();
//         setInternalVal(map, new Dummy());
//         assertEquals(1, map.size(), "Any non‑array, non‑Map value should fall through to the final return of 1");
//     }
}