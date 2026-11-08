package com.cedarsoftware.util;

import java.lang.reflect.Field;
// import org.junit.*;
// import static org.junit.Assert.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CompactMap_containsKey_Test {

    /**
     * Creates a map that stays in the compact‑array state for the given size.
     */
//     private <K, V> CompactMap<K, V> newArrayMap(int compactSize) {
//         return new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return compactSize;
//             }
// 
//             @Override
//             protected Map<K, V> getNewMap() {
//                 return new HashMap<>();
//             }
// 
//             @Override
//             protected K getSingleValueKey() {
//                 @SuppressWarnings("unchecked")
//                 K k = (K) "id";
//                 return k;
//             }
//         };
//     }

    /**
     * Builder‑based map with the supplied options.
     */
    private <K, V> CompactMap<K, V> builderMap(boolean caseSensitive, String ordering, int compactSize) {
        CompactMap.Builder<K, V> b = CompactMap.<K, V>builder();
        if (!caseSensitive)
            b.caseSensitive(false);
        if ("sorted".equals(ordering))
            b.sortedOrder();
        if ("reverse".equals(ordering))
            b.sortedOrder().reverseOrder();
        b.compactSize(compactSize);
        return b.build();
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> arrayLinearScanProvider() {
        return Stream.of(Arguments.of("alpha", true), Arguments.of("missing", false));
    }

    private static Stream<Arguments> arrayBinarySearchSortedProvider() {
        return Stream.of(Arguments.of("beta", true), Arguments.of("delta", false));
    }

    private static Stream<Arguments> arrayBinarySearchReverseProvider() {
        return Stream.of(Arguments.of("gamma", true), Arguments.of("epsilon", false));
    }

    private static Stream<Arguments> backingMapProvider() {
        return Stream.of(Arguments.of("second", true), Arguments.of("fourth", false));
    }

    private static Stream<Arguments> caseInsensitiveProvider() {
        return Stream.of(Arguments.of("ALPHA", true), Arguments.of("Gamma", false));
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void emptyMap_containsKey_returnsFalse() {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 // any value >= 2
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new HashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         Assertions.assertTrue(map.isEmpty());
//         Assertions.assertFalse(map.containsKey("anything"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void singleEntryMap_containsKey_behaviour() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 4;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new HashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         Assertions.assertFalse(map.containsKey("key"));
//         map.put("key", "value");
//         Assertions.assertTrue(map.containsKey("key"));
//         Assertions.assertFalse(map.containsKey("other"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void compactArray_containsKey_linearSearch() {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 // stay in array for up to 5 entries
//                 return 6;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new HashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         // still within compact array (size = 4)
//         map.put("d", 4);
//         Assertions.assertTrue(map.containsKey("c"));
//         Assertions.assertFalse(map.containsKey("z"));
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void sortedCompactArray_containsKey_binarySearch() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().compactSize(8).caseSensitive(true).build();
        map.put("delta", 4);
        map.put("alpha", 1);
        map.put("charlie", 3);
        map.put("bravo", 2);
        Assertions.assertTrue(map.containsKey("alpha"));
        Assertions.assertTrue(map.containsKey("bravo"));
        Assertions.assertTrue(map.containsKey("charlie"));
        Assertions.assertTrue(map.containsKey("delta"));
        Assertions.assertFalse(map.containsKey("echo"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void reverseSortedCompactArray_containsKey_binarySearch() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().reverseOrder().compactSize(8).build();
        map.put("delta", 4);
        map.put("alpha", 1);
        map.put("charlie", 3);
        map.put("bravo", 2);
        Assertions.assertTrue(map.containsKey("alpha"));
        Assertions.assertTrue(map.containsKey("bravo"));
        Assertions.assertTrue(map.containsKey("charlie"));
        Assertions.assertTrue(map.containsKey("delta"));
        Assertions.assertFalse(map.containsKey("echo"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void caseInsensitiveSorted_containsKey_ignoresCase() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().caseSensitive(false).compactSize(8).build();
        map.put("KeyOne", 1);
        map.put("KeyTwo", 2);
        Assertions.assertTrue(map.containsKey("keyone"));
        Assertions.assertTrue(map.containsKey("KEYTWO"));
        Assertions.assertFalse(map.containsKey("keythree"));
    }

    /* --------------------------------------------------------------------- */
//     @Test
//     void backingMap_containsKey_delegatesToMap() {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 // switch after 2 entries
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
//                 return "id";
//             }
//         };
//         // 3 entries forces transition to the backing map
//         map.put("first", 1);
//         map.put("second", 2);
//         map.put("third", 3);
//         Assertions.assertTrue(map.containsKey("second"));
//         Assertions.assertFalse(map.containsKey("fourth"));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void mapWithNullKey_containsKey_behaviour() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new HashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         map.put(null, "nullValue");
//         Assertions.assertTrue(map.containsKey(null));
//         Assertions.assertFalse(map.containsKey("notNull"));
//     }

//     @Test
//     void singleEntry_containsKey_behaviour() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 4;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new HashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         // before any put
//         Assertions.assertFalse(map.containsKey("id"));
//         // put the default single‑value key
//         map.put("id", "value");
//         Assertions.assertTrue(map.containsKey("id"));
//         Assertions.assertFalse(map.containsKey("other"));
//     }

    @ParameterizedTest(name = "{index}: key={0} → expected={1}")
    @MethodSource("arrayLinearScanProvider")
    void compactArray_unordered_linearScan(String key, boolean expected) {
        CompactMap<String, Integer> map = newArrayMap(6);
        map.put("alpha", 1);
        map.put("beta", 2);
        Assertions.assertEquals(expected, map.containsKey(key));
    }

    @ParameterizedTest(name = "{index}: key={0} → expected={1}")
    @MethodSource("arrayBinarySearchSortedProvider")
    void compactArray_sorted_binarySearch(String key, boolean expected) {
        CompactMap<String, Integer> map = builderMap(true, "sorted", 8);
        map.put("beta", 2);
        map.put("alpha", 1);
        map.put("gamma", 3);
        Assertions.assertEquals(expected, map.containsKey(key));
    }

    @ParameterizedTest(name = "{index}: key={0} → expected={1}")
    @MethodSource("arrayBinarySearchReverseProvider")
    void compactArray_reverse_binarySearch(String key, boolean expected) {
        CompactMap<String, Integer> map = builderMap(true, "reverse", 8);
        map.put("gamma", 3);
        map.put("alpha", 1);
        map.put("beta", 2);
        Assertions.assertEquals(expected, map.containsKey(key));
    }

//     @ParameterizedTest(name = "{index}: key={0} → expected={1}")
//     @MethodSource("backingMapProvider")
//     void backingMap_containsKey_delegates(String key, boolean expected) {
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
//                 return "id";
//             }
//         };
//         map.put("first", 1);
//         map.put("second", 2);
//         // forces transition to Map mode
//         map.put("third", 3);
//         Assertions.assertEquals(expected, map.containsKey(key));
//     }

//     @Test
//     void nullKey_onEmptyMap_returnsFalse() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new HashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         Assertions.assertFalse(map.containsKey(null));
//     }

//     @Test
//     void nullKey_singleEntry_returnsTrue() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new HashMap<>();
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
//         };
//         map.put(null, "nullValue");
//         Assertions.assertTrue(map.containsKey(null));
//     }

    @Test
    void nullKey_inArrayWithoutNull_returnsFalse() {
        CompactMap<String, Integer> map = newArrayMap(6);
        map.put("a", 1);
        map.put("b", 2);
        Assertions.assertFalse(map.containsKey(null));
    }

    @ParameterizedTest(name = "{index}: query={0} → expected={1}")
    @MethodSource("caseInsensitiveProvider")
    void caseInsensitiveSorted_containsKey(String query, boolean expected) {
        CompactMap<String, Integer> map = builderMap(false, "sorted", 8);
        map.put("alpha", 1);
        map.put("beta", 2);
        Assertions.assertEquals(expected, map.containsKey(query));
    }

    @Test
    void compactSizeBoundary_exactSize_staysArray() {
        CompactMap<String, Integer> map = builderMap(true, "unordered", CompactMap.DEFAULT_COMPACT_SIZE);
        for (int i = 0; i < CompactMap.DEFAULT_COMPACT_SIZE; i++) {
            map.put("k" + i, i);
        }
        Assertions.assertTrue(map.containsKey("k49"));
        Assertions.assertFalse(map.containsKey("missing"));
    }

    @Test
    void compactSizeBoundary_justOver_switchesToMap() {
        CompactMap<String, Integer> map = builderMap(true, "unordered", CompactMap.DEFAULT_COMPACT_SIZE);
        for (int i = 0; i < CompactMap.DEFAULT_COMPACT_SIZE + 1; i++) {
            map.put("k" + i, i);
        }
        Assertions.assertTrue(map.containsKey("k50"));
        Assertions.assertFalse(map.containsKey("nonexistent"));
    }

    @Test
    void mixedKeyTypes_stringVsInteger() {
        CompactMap<String, Integer> map = newArrayMap(10);
        map.put("123", 123);
        Assertions.assertFalse(map.containsKey(123));
        Assertions.assertTrue(map.containsKey("123"));
    }

    @Test
    void afterClear_containsKey_returnsFalse() {
        CompactMap<String, Integer> map = newArrayMap(6);
        map.put("a", 1);
        map.put("b", 2);
        map.clear();
        Assertions.assertTrue(map.isEmpty());
        Assertions.assertFalse(map.containsKey("a"));
    }

    @Test
    void singleEntryRemoval_containsKey_returnsFalse() {
        CompactMap<String, Integer> map = newArrayMap(6);
        map.put("id", 42);
        Assertions.assertTrue(map.containsKey("id"));
        map.remove("id");
        Assertions.assertFalse(map.containsKey("id"));
        Assertions.assertTrue(map.isEmpty());
    }

    @Test
    void duplicateKeyInsertion_containsKey_stillTrue() {
        CompactMap<String, Integer> map = newArrayMap(6);
        map.put("dupKey", 1);
        map.put("dupKey", 2);
        Assertions.assertTrue(map.containsKey("dupKey"));
        Assertions.assertEquals(2, map.get("dupKey"));
    }

    @Test
    void sortedArray_typeMismatch_throwsClassCastException() {
        CompactMap<String, Integer> map = builderMap(true, "sorted", 8);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Assertions.assertThrows(ClassCastException.class, () -> map.containsKey(42));
    }

    @Test
    void reverseArray_typeMismatch_throwsClassCastException() {
        CompactMap<Integer, String> map = builderMap(true, "reverse", 8);
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        Assertions.assertThrows(ClassCastException.class, () -> map.containsKey("42"));
    }

    @Test
    void sortedArray_nullKey_throwsNullPointerException() {
        CompactMap<String, Integer> map = builderMap(true, "sorted", 8);
        map.put("a", 1);
        map.put("b", 2);
        Assertions.assertThrows(NullPointerException.class, () -> map.containsKey(null));
    }

    @Test
    void reverseArray_nullKey_throwsNullPointerException() {
        CompactMap<String, Integer> map = builderMap(true, "reverse", 8);
        map.put("a", 1);
        map.put("b", 2);
        Assertions.assertThrows(NullPointerException.class, () -> map.containsKey(null));
    }

    @Test
    void corruptedArrayBacking_throwsArrayIndexOutOfBoundsException() throws Exception {
        CompactMap<String, Integer> map = newArrayMap(6);
        map.put("k1", 1);
        map.put("k2", 2);
        Field valField = CompactMap.class.getDeclaredField("val");
        valField.setAccessible(true);
        Object[] corrupted = new Object[] { "k1", 1, "orphanKey" };
        valField.set(map, corrupted);
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> map.containsKey("anyKey"));
    }

    @Test
    void removeNonExistingKey_returnsFalse() {
        CompactMap<String, Integer> map = newArrayMap(6);
        map.put("x", 10);
        Assertions.assertFalse(map.containsKey("y"));
        Assertions.assertNull(map.remove("y"));
    }

    @Test
    public void compactArray_unordered_linearScan_alpha_isTrue() {
        CompactMap<String, Integer> map = newArrayMap(6);
        map.put("alpha", 1);
        map.put("beta", 2);
        Assertions.assertTrue(map.containsKey("alpha"));
    }

    @Test
    public void compactArray_unordered_linearScan_missing_isFalse() {
        CompactMap<String, Integer> map = newArrayMap(6);
        map.put("alpha", 1);
        map.put("beta", 2);
        Assertions.assertFalse(map.containsKey("missing"));
    }

    @Test
    public void compactArray_sorted_binarySearch_beta_isTrue() {
        CompactMap<String, Integer> map = builderMap(true, "sorted", 8);
        map.put("beta", 2);
        map.put("alpha", 1);
        map.put("gamma", 3);
        Assertions.assertTrue(map.containsKey("beta"));
    }

    @Test
    public void compactArray_sorted_binarySearch_delta_isFalse() {
        CompactMap<String, Integer> map = builderMap(true, "sorted", 8);
        map.put("beta", 2);
        map.put("alpha", 1);
        map.put("gamma", 3);
        Assertions.assertFalse(map.containsKey("delta"));
    }

    @Test
    public void compactArray_reverse_binarySearch_gamma_isTrue() {
        CompactMap<String, Integer> map = builderMap(true, "reverse", 8);
        map.put("gamma", 3);
        map.put("alpha", 1);
        map.put("beta", 2);
        Assertions.assertTrue(map.containsKey("gamma"));
    }

    @Test
    public void compactArray_reverse_binarySearch_epsilon_isFalse() {
        CompactMap<String, Integer> map = builderMap(true, "reverse", 8);
        map.put("gamma", 3);
        map.put("alpha", 1);
        map.put("beta", 2);
        Assertions.assertFalse(map.containsKey("epsilon"));
    }

//     @Test
//     public void backingMap_containsKey_second_isTrue() {
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
//                 return "id";
//             }
//         };
//         map.put("first", 1);
//         map.put("second", 2);
//         // forces map mode
//         map.put("third", 3);
//         Assertions.assertTrue(map.containsKey("second"));
//     }

//     @Test
//     public void backingMap_containsKey_fourth_isFalse() {
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
//                 return "id";
//             }
//         };
//         map.put("first", 1);
//         map.put("second", 2);
//         // forces map mode
//         map.put("third", 3);
//         Assertions.assertFalse(map.containsKey("fourth"));
//     }

    @Test
    public void caseInsensitiveSorted_containsKey_ALPA_isTrue() {
        CompactMap<String, Integer> map = builderMap(false, "sorted", 8);
        map.put("alpha", 1);
        map.put("beta", 2);
        Assertions.assertTrue(map.containsKey("ALPHA"));
    }

    @Test
    public void caseInsensitiveSorted_containsKey_Gamma_isFalse() {
        CompactMap<String, Integer> map = builderMap(false, "sorted", 8);
        map.put("alpha", 1);
        map.put("beta", 2);
        Assertions.assertFalse(map.containsKey("Gamma"));
    }
}