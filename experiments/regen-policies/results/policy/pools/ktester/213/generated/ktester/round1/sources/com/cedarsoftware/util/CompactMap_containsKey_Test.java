package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
// import org.junit.runners.Parameterized.Parameters;
// import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.*;

class CompactMap_containsKey_Test {

    /* --------------------------------------------------------------------- */
//     @Test
//     void containsKey_EmptyMap_ReturnsFalse() {
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
//         };
//         assertTrue(map.isEmpty());
//         assertFalse(map.containsKey("anything"));
//         assertFalse(map.containsKey(null));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void containsKey_SingleEntry_ReturnsTrueForExistingKeyAndFalseOtherwise() {
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
//         assertTrue(map.isEmpty());
//         map.put("id", 42);
//         assertEquals(1, map.size());
//         assertTrue(map.containsKey("id"));
//         assertFalse(map.containsKey("other"));
//         assertFalse(map.containsKey(null));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void containsKey_CompactArray_FindsExistingKeyAndRejectsMissingKey() {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         map.put("one", 1);
//         map.put("two", 2);
//         map.put("three", 3);
//         assertTrue(map.containsKey("one"));
//         assertTrue(map.containsKey("two"));
//         assertTrue(map.containsKey("three"));
//         assertFalse(map.containsKey("four"));
//         assertFalse(map.containsKey(null));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     void containsKey_BackingMap_DelegatesToUnderlyingMap() {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 2;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new HashMap<>();
//             }
//         };
//         map.put("a", 10);
//         map.put("b", 20);
//         map.put("c", 30);
//         assertEquals(3, map.size());
//         assertTrue(map.containsKey("a"));
//         assertTrue(map.containsKey("b"));
//         assertTrue(map.containsKey("c"));
//         assertFalse(map.containsKey("d"));
//     }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_SortedCaseInsensitive_FindsKeyIgnoringCase() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().caseSensitive(false).sortedOrder().compactSize(10).build();
        map.put("Alpha", 1);
        map.put("Beta", 2);
        assertTrue(map.containsKey("Alpha"));
        assertTrue(map.containsKey("alpha"));
        assertTrue(map.containsKey("ALPHA"));
        assertFalse(map.containsKey("Gamma"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_ReverseOrder_CaseSensitive() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().caseSensitive(true).reverseOrder().compactSize(10).build();
        map.put("One", 1);
        map.put("Two", 2);
        assertTrue(map.containsKey("One"));
        assertTrue(map.containsKey("Two"));
        assertFalse(map.containsKey("one"));
        assertFalse(map.containsKey("two"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_CompactArray_InsertionOrdering_KeyPresent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().compactSize(5).build();
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put("gamma", 3);
        assertTrue(map.containsKey("alpha"));
    }

    @Test
    void containsKey_CompactArray_InsertionOrdering_KeyAbsent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().compactSize(5).build();
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put("gamma", 3);
        assertFalse(map.containsKey("delta"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_CompactArray_SortedOrdering_KeyPresent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().compactSize(10).build();
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(map.containsKey("b"));
    }

    @Test
    void containsKey_CompactArray_SortedOrdering_KeyAbsent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().compactSize(10).build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertFalse(map.containsKey("z"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_CompactArray_ReverseOrdering_KeyPresent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().reverseOrder().compactSize(10).build();
        map.put("c", 3);
        map.put("b", 2);
        map.put("a", 1);
        assertTrue(map.containsKey("c"));
    }

    @Test
    void containsKey_CompactArray_ReverseOrdering_KeyAbsent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().reverseOrder().compactSize(10).build();
        map.put("c", 3);
        map.put("b", 2);
        map.put("a", 1);
        assertFalse(map.containsKey("x"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_BackingMap_KeyPresent() {
        CompactMap<Integer, String> map = CompactMap.<Integer, String>builder().compactSize(2).build();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        assertTrue(map.containsKey(2));
    }

    @Test
    void containsKey_BackingMap_KeyAbsent() {
        CompactMap<Integer, String> map = CompactMap.<Integer, String>builder().compactSize(2).build();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        assertFalse(map.containsKey(99));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_SortedCaseInsensitive_KeyPresent_DifferentCase() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().caseSensitive(false).sortedOrder().compactSize(10).build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertTrue(map.containsKey("B"));
    }

    @Test
    void containsKey_SortedCaseInsensitive_KeyAbsent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().caseSensitive(false).sortedOrder().compactSize(10).build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertFalse(map.containsKey("Z"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_SortedOrdering_NullKeyPresent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().compactSize(10).build();
        map.put(null, 0);
        map.put("a", 1);
        map.put("b", 2);
        assertTrue(map.containsKey(null));
    }

    @Test
    void containsKey_SortedOrdering_NullKeyAbsent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().compactSize(10).build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertFalse(map.containsKey(null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_BackingMap_NullKeyPresent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(2).build();
        map.put("x", 1);
        map.put("y", 2);
        map.put(null, 0);
        assertTrue(map.containsKey(null));
    }

    @Test
    void containsKey_BackingMap_NullKeyAbsent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(2).build();
        map.put("x", 1);
        map.put("y", 2);
        map.put("z", 3);
        assertFalse(map.containsKey(null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_CompactArray_Sorted_NonComparableKey_ThrowsClassCastException() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().compactSize(10).build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertThrows(ClassCastException.class, () -> map.containsKey(42));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_InsertionOrdering_NullKeyPresent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().compactSize(10).build();
        map.put(null, 0);
        map.put("x", 1);
        map.put("y", 2);
        assertTrue(map.containsKey(null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_ReverseOrdering_CaseInsensitive_KeyPresent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().caseSensitive(false).reverseOrder().compactSize(10).build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertTrue(map.containsKey("C"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_SortedCaseSensitive_KeyPresent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().compactSize(10).build();
        map.put("Alpha", 1);
        map.put("Beta", 2);
        assertTrue(map.containsKey("Alpha"));
        assertFalse(map.containsKey("alpha"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_ReverseCaseSensitive_KeyPresent() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().reverseOrder().compactSize(10).build();
        map.put("One", 1);
        map.put("Two", 2);
        assertTrue(map.containsKey("One"));
        assertFalse(map.containsKey("one"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_CompactArray_Insertion_Parameterized_Alpha() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().compactSize(5).build();
        map.put("alpha", 1);
        map.put("gamma", 3);
        assertTrue(map.containsKey("alpha"));
    }

    @Test
    void containsKey_CompactArray_Insertion_Parameterized_Beta() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().compactSize(5).build();
        map.put("alpha", 1);
        map.put("gamma", 3);
        assertFalse(map.containsKey("beta"));
    }

    @Test
    void containsKey_CompactArray_SortedCaseInsensitive_Parameterized_A() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().caseSensitive(false).sortedOrder().compactSize(10).build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertTrue(map.containsKey("A"));
    }

    @Test
    void containsKey_CompactArray_SortedCaseInsensitive_Parameterized_B() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().caseSensitive(false).sortedOrder().compactSize(10).build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertTrue(map.containsKey("b"));
    }

    @Test
    void containsKey_CompactArray_SortedCaseInsensitive_Parameterized_Z() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().caseSensitive(false).sortedOrder().compactSize(10).build();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertFalse(map.containsKey("Z"));
    }
}