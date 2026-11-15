package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntFunction;
// import org.junit.Test;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class CompactMapEqualsTest {

    /**
     * Creates a {@link CompactMap} with the supplied {@code compactSize}. The map uses a
     * {@link LinkedHashMap} as its backing store so that iteration order is deterministic.
     */
    private <K, V> CompactMap<K, V> newCompactMap(int compactSize) {
        return new CompactMap<K, V>() {

            @Override
            protected int compactSize() {
                return compactSize;
            }

            @Override
            protected Map<K, V> getNewMap() {
                // deterministic ordering
                return new LinkedHashMap<>();
            }
        };
    }

    /**
     * Helper that builds a {@link LinkedHashMap} with the given size using the supplied
     * key/value generator functions.
     */
    private static Map<String, Integer> generateMap(int size, IntFunction<String> keyFn, IntFunction<Integer> valueFn) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            m.put(keyFn.apply(i), valueFn.apply(i));
        }
        return m;
    }

//     @Test
//     void equalsSameInstance() {
//         CompactMap<String, Integer> map = newCompactMap(5);
//         map.put("a", 1);
//         assertTrue("Map should be equal to itself", map.equals(map));
//     }

//     @Test
//     void equalsDifferentType() {
//         CompactMap<String, Integer> map = newCompactMap(5);
//         map.put("a", 1);
//         assertFalse("Map should not be equal to a non‑Map object", map.equals("not a map"));
//     }

//     @Test
//     void equalsBothEmpty() {
//         CompactMap<String, Integer> map1 = newCompactMap(5);
//         CompactMap<String, Integer> map2 = newCompactMap(5);
//         assertTrue("Two empty CompactMaps must be equal", map1.equals(map2));
//         assertTrue("Empty CompactMap must be equal to empty HashMap", map1.equals(new HashMap<>()));
//     }

//     @Test
//     void equalsSingleEntry() {
//         CompactMap<String, Integer> map1 = newCompactMap(5);
//         CompactMap<String, Integer> map2 = newCompactMap(5);
//         map1.put("key", 42);
//         map2.put("key", 42);
//         assertTrue("Maps with the same single entry must be equal", map1.equals(map2));
//         // different value
//         map2.put("key", 43);
//         assertFalse("Maps with different values for the same key must not be equal", map1.equals(map2));
//     }

//     @Test
//     void equalsWithNullValues() {
//         CompactMap<String, Integer> map1 = newCompactMap(5);
//         CompactMap<String, Integer> map2 = newCompactMap(5);
//         map1.put("k1", null);
//         map2.put("k1", null);
//         assertTrue("Maps containing null values should compare equal when both values are null", map1.equals(map2));
//         map2.put("k1", 1);
//         assertFalse("Maps should not be equal when one contains null and the other a non‑null value", map1.equals(map2));
//     }

//     @Test
//     void equalsCompactArrayState() {
//         // compactSize = 4, we will add 2 entries → internal Object[] representation
//         CompactMap<String, Integer> map = newCompactMap(4);
//         map.put("one", 1);
//         map.put("two", 2);
//         Map<String, Integer> other = new HashMap<>();
//         other.put("one", 1);
//         other.put("two", 2);
//         assertTrue("CompactMap in compact‑array state must be equal to a regular Map with same entries", map.equals(other));
//         other.put("two", 99);
//         assertFalse("Different values must cause inequality", map.equals(other));
//     }

//     @Test
//     void equalsBackingMapState() {
//         // compactSize = 2, we will add 4 entries → internal Map representation
//         CompactMap<String, Integer> map = newCompactMap(2);
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         map.put("d", 4);
//         Map<String, Integer> other = new LinkedHashMap<>();
//         other.put("a", 1);
//         other.put("b", 2);
//         other.put("c", 3);
//         other.put("d", 4);
//         assertTrue("CompactMap in backing‑Map state must be equal to another Map with same entries", map.equals(other));
//         other.remove("d");
//         assertFalse("Different sizes must cause inequality", map.equals(other));
//     }

//     @Test
//     void equalsAgainstDifferentMapImplementation() {
//         CompactMap<String, String> map = newCompactMap(3);
//         map.put("x", "X");
//         map.put("y", "Y");
//         // TreeMap with same entries (sorted order)
//         Map<String, String> treeMap = new TreeMap<>();
//         treeMap.put("x", "X");
//         treeMap.put("y", "Y");
//         assertTrue("CompactMap should be equal to any Map implementation with identical entries", map.equals(treeMap));
//     }

//     @Test
//     void equalitySelf() {
//         CompactMap<String, Integer> map = newCompactMap(5);
//         map.put("a", 1);
//         assertTrue("The argument is the same instance as the CompactMap under test; equals should return true due to the identity check.", map.equals(map));
//     }

//     @Test
//     void equalityNonMap() {
//         CompactMap<String, Integer> map = newCompactMap(5);
//         map.put("a", 1);
//         assertFalse("Argument is not a Map; equals must return false.", map.equals("not a map"));
//     }

//     @Test
//     void equalityDifferentSize() {
//         CompactMap<String, Integer> map = newCompactMap(5);
//         map.put("a", 1);
//         Map<String, Integer> other = new HashMap<>() {
// 
//             {
//                 put("a", 1);
//                 put("b", 2);
//             }
//         };
//         assertFalse("CompactMap contains one entry while the other map contains two entries; size mismatch should cause equals to return false.", map.equals(other));
//     }

//     @Test
//     void equalityEmptyMap_BothEmpty() {
//         CompactMap<String, Integer> map1 = newCompactMap(5);
//         CompactMap<String, Integer> map2 = newCompactMap(5);
//         assertTrue("Both CompactMaps are empty; should be equal.", map1.equals(map2));
//         assertTrue("Empty CompactMap must be equal to empty HashMap.", map1.equals(new HashMap<>()));
//     }

//     @Test
//     void equalityEmptyMap_OneNonEmpty() {
//         CompactMap<String, Integer> map = newCompactMap(5);
//         Map<String, Integer> other = new HashMap<>() {
// 
//             {
//                 put("x", 10);
//             }
//         };
//         assertFalse("CompactMap is empty but the other map has one entry; equals should return false.", map.equals(other));
//     }

//     @Test
//     void equalitySingleEntry_Equal() {
//         CompactMap<String, String> map = newCompactMap(5);
//         map.put("key", "value");
//         Map<String, String> other = new HashMap<>() {
// 
//             {
//                 put("key", "value");
//             }
//         };
//         assertTrue("Both maps contain exactly the same single entry; equals should return true.", map.equals(other));
//     }

//     @Test
//     void equalitySingleEntry_DifferentValue() {
//         CompactMap<String, String> map = newCompactMap(5);
//         map.put("key", "value");
//         Map<String, String> other = new HashMap<>() {
// 
//             {
//                 put("key", "different");
//             }
//         };
//         assertFalse("Same key but different values; equals should return false.", map.equals(other));
//     }

//     @Test
//     void equalitySingleEntry_DifferentKey() {
//         CompactMap<String, String> map = newCompactMap(5);
//         map.put("key", "value");
//         Map<String, String> other = new HashMap<>() {
// 
//             {
//                 put("otherKey", "value");
//             }
//         };
//         assertFalse("Different keys; equals should return false.", map.equals(other));
//     }

//     @Test
//     void equalityCompactArray_EqualIncludingNull() {
//         // compactSize = 5, add 3 entries → internal Object[] representation
//         CompactMap<String, Integer> map = newCompactMap(5);
//         map.put("k1", 1);
//         map.put("k2", 2);
//         map.put("k3", null);
//         Map<String, Integer> other = new HashMap<>() {
// 
//             {
//                 put("k1", 1);
//                 put("k2", 2);
//                 put("k3", null);
//             }
//         };
//         assertTrue("CompactMap in compact‑array state must be equal to a regular Map with identical entries including a null value.", map.equals(other));
//     }

//     @Test
//     void equalityCompactArray_MissingKey() {
//         CompactMap<String, Integer> map = newCompactMap(5);
//         map.put("k1", 1);
//         map.put("k2", 2);
//         map.put("k3", 3);
//         Map<String, Integer> other = new HashMap<>() {
// 
//             {
//                 put("k1", 1);
//                 put("k2", 2);
//             }
//         };
//         assertFalse("Other map missing a key present in CompactMap; should return false.", map.equals(other));
//     }

//     @Test
//     void equalityCompactArray_NullValueMismatch() {
//         CompactMap<String, Integer> map = newCompactMap(5);
//         map.put("k1", 1);
//         map.put("k2", 2);
//         map.put("k3", null);
//         Map<String, Integer> other = new HashMap<>() {
// 
//             {
//                 put("k1", 1);
//                 put("k2", 2);
//                 put("k3", 99);
//             }
//         };
//         assertFalse("CompactMap stores null for k3 while other map stores non‑null; should return false.", map.equals(other));
//     }

//     @Test
//     void equalityLargeMap_Equal() {
//         // compactSize = 10, add 51 entries → backing map
//         CompactMap<String, Integer> map = newCompactMap(10);
//         for (int i = 0; i < 51; i++) {
//             map.put("key" + i, i);
//         }
//         Map<String, Integer> other = generateMap(51, i -> "key" + i, i -> i);
//         assertTrue("CompactMap with >compactSize entries should be equal to another map with identical entries.", map.equals(other));
//     }

//     @Test
//     void equalityLargeMap_UnequalValue() {
//         CompactMap<String, Integer> map = newCompactMap(10);
//         for (int i = 0; i < 51; i++) {
//             map.put("key" + i, i);
//         }
//         Map<String, Integer> other = generateMap(51, i -> "key" + i, i -> i + 1000);
//         assertFalse("Same keys but different values; equals must return false.", map.equals(other));
//     }

//     @Test
//     void equalityAgainstDifferentMapImplementation() {
//         CompactMap<String, String> map = newCompactMap(3);
//         map.put("x", "X");
//         map.put("y", "Y");
//         Map<String, String> treeMap = new TreeMap<>();
//         treeMap.put("x", "X");
//         treeMap.put("y", "Y");
//         assertTrue("CompactMap should be equal to any Map implementation with identical entries regardless of ordering implementation.", map.equals(treeMap));
//     }
}