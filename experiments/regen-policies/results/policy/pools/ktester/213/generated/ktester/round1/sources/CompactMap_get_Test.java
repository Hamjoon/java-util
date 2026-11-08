package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class CompactMap_get_Test {

    @Nested
    class ObjectArraySortedOrReverseBinarySearch {

        @Test
        void sortedOrderingBinarySearchFindsKey() {
            // Force sorted ordering, case‑sensitive.
            CompactMap<String, String> map = mapWithOrdering(CompactMap.SORTED, false);
            // Manually inject a correctly sorted compact array.
            Object[] entries = new Object[] { "Apple", "red", "Banana", "yellow", "Cherry", "dark" };
            setInternalVal(map, entries);
            assertEquals("red", map.get("Apple"));
        }

        @Test
        void sortedOrderingBinarySearchMissingKeyReturnsNull() {
            CompactMap<String, String> map = mapWithOrdering(CompactMap.SORTED, false);
            Object[] entries = new Object[] { "Apple", "red", "Banana", "yellow", "Cherry", "dark" };
            setInternalVal(map, entries);
            // case‑sensitive miss
            assertNull(map.get("BANANA"));
        }

        @Test
        void reverseOrderingBinarySearchFindsKey() {
            CompactMap<String, String> map = mapWithOrdering(CompactMap.REVERSE, false);
            // Reverse‑sorted array (largest to smallest)
            Object[] entries = new Object[] { "Cherry", "dark", "Banana", "yellow", "Apple", "red" };
            setInternalVal(map, entries);
            assertEquals("dark", map.get("Cherry"));
        }
    }

    @Nested
    class ObjectArrayUnsortedLinearScan {

        @Test
        void insertionOrderingLinearScanFindsExistingKey() {
            CompactMap<String, String> map = mapWithOrdering("insertion", false);
            Object[] entries = new Object[] { "key1", "value1", "key2", "value2" };
            setInternalVal(map, entries);
            assertEquals("value1", map.get("key1"));
        }

        @Test
        void insertionOrderingLinearScanMissingKeyReturnsNull() {
            CompactMap<String, String> map = mapWithOrdering("insertion", false);
            Object[] entries = new Object[] { "key1", "value1", "key2", "value2" };
            setInternalVal(map, entries);
            assertNull(map.get("missing"));
        }
    }

    @Nested
    class BackingMapScenarios {

//         @Test
//         void backingMapReturnsExistingValue() {
//             CompactMap<Integer, String> map = new CompactMap<Integer, String>() {
// 
//                 @Override
//                 protected int getCompactSize() {
//                     return 1;
//                 }
// 
//                 @Override
//                 protected Map<Integer, String> getNewMap() {
//                     return new HashMap<>();
//                 }
//             };
//             // Insert three entries → transition to backing map.
//             map.put(1, "one");
//             map.put(2, "two");
//             map.put(42, "answer");
//             assertEquals("answer", map.get(42));
//         }

//         @Test
//         void backingMapMissingKeyReturnsNull() {
//             CompactMap<Integer, String> map = new CompactMap<Integer, String>() {
// 
//                 @Override
//                 protected int getCompactSize() {
//                     return 1;
//                 }
// 
//                 @Override
//                 protected Map<Integer, String> getNewMap() {
//                     return new HashMap<>();
//                 }
//             };
//             map.put(1, "one");
//             map.put(2, "two");
//             map.put(42, "answer");
//             assertNull(map.get(99));
//         }

//         @Test
//         void backingMapExplicitNullValueIsReturned() {
//             CompactMap<String, String> map = new CompactMap<String, String>() {
// 
//                 @Override
//                 protected int getCompactSize() {
//                     return 1;
//                 }
// 
//                 @Override
//                 protected Map<String, String> getNewMap() {
//                     return new HashMap<>();
//                 }
//             };
//             map.put("present", null);
//             map.put("other", "value");
//             // value is explicitly null
//             assertNull(map.get("present"));
//         }
    }

    @Nested
    class SingleEntryMapScenarios {

//         @Test
//         void singleEntryKeyMatchesReturnsValue() {
//             CompactMap<String, String> map = new CompactMap<String, String>() {
// 
//                 @Override
//                 protected String getSingleValueKey() {
//                     return "singleKey";
//                 }
// 
//                 @Override
//                 protected String getLogicalSingleKey() {
//                     return "singleKey";
//                 }
// 
//                 @Override
//                 protected String getLogicalSingleValue() {
//                     return "singleValue";
//                 }
//             };
//             // Force internal state to be the single‑entry optimisation.
//             // any non‑array, non‑Map, non‑EMPTY_MAP value
//             setInternalVal(map, new Object());
//             assertEquals("singleValue", map.get("singleKey"));
//         }

//         @Test
//         void singleEntryKeyMismatchReturnsNull() {
//             CompactMap<String, String> map = new CompactMap<String, String>() {
// 
//                 @Override
//                 protected String getSingleValueKey() {
//                     return "singleKey";
//                 }
// 
//                 @Override
//                 protected String getLogicalSingleKey() {
//                     return "singleKey";
//                 }
// 
//                 @Override
//                 protected String getLogicalSingleValue() {
//                     return "singleValue";
//                 }
//             };
//             setInternalVal(map, new Object());
//             assertNull(map.get("otherKey"));
//         }
    }

    @Nested
    class SortedCaseInsensitiveBinarySearch {

        @Test
        void caseInsensitiveSortedSearchFindsKeyRegardlessOfCase() {
            CompactMap<String, String> map = mapWithOrdering(CompactMap.SORTED, true);
            // Sorted array with capitalised key.
            Object[] entries = new Object[] { "Apple", "red", "Banana", "yellow", "Cherry", "dark" };
            setInternalVal(map, entries);
            assertEquals("red", map.get("apple"));
        }

        @Test
        void caseInsensitiveSortedSearchMissingKeyReturnsNull() {
            CompactMap<String, String> map = mapWithOrdering(CompactMap.SORTED, true);
            Object[] entries = new Object[] { "Apple", "red", "Banana", "yellow", "Cherry", "dark" };
            setInternalVal(map, entries);
            // key exists but we are looking for a different string
            assertNull(map.get("BANANA"));
        }
    }

    @Nested
    class UnsortedArrayWithNullKeyOrValue {

        @Test
        void nullKeyLookupReturnsAssociatedValue() {
            CompactMap<String, String> map = mapWithOrdering("insertion", false);
            Object[] entries = new Object[] { null, "nullKeyValue", "k2", null };
            setInternalVal(map, entries);
            assertEquals("nullKeyValue", map.get(null));
        }

        @Test
        void keyWithStoredNullValueReturnsNull() {
            CompactMap<String, String> map = mapWithOrdering("insertion", false);
            Object[] entries = new Object[] { null, "nullKeyValue", "k2", null };
            setInternalVal(map, entries);
            assertNull(map.get("k2"));
        }
    }

    @Nested
    class TypeMismatchKeyInArray {

        @Test
        void integerKeyDoesNotMatchStringLookup() {
            CompactMap<String, String> map = mapWithOrdering("insertion", false);
            Object[] entries = new Object[] { 100, "hundred", "foo", "bar" };
            setInternalVal(map, entries);
            // String does not equal Integer 100
            assertNull(map.get("100"));
        }
    }

    @Nested
    class DuplicateKeysFirstOccurrence {

        @Test
        void linearScanReturnsFirstValueWhenKeysDuplicate() {
            CompactMap<String, String> map = mapWithOrdering("insertion", false);
            Object[] entries = new Object[] { "dup", "first", "dup", "second" };
            setInternalVal(map, entries);
            assertEquals("first", map.get("dup"));
        }
    }

    @Nested
    class UnknownOrderingFallsBackToLinearScan {

        @Test
        void customOrderingUsesLinearScan() {
            CompactMap<String, String> map = mapWithOrdering("custom", false);
            Object[] entries = new Object[] { "c", "valueC", "a", "valueA", "b", "valueB" };
            setInternalVal(map, entries);
            assertEquals("valueC", map.get("c"));
        }
    }

    @Nested
    class OddLengthArrayTriggersException {

        @Test
        void oddLengthArrayCausesArrayIndexOutOfBoundsException() {
            CompactMap<String, String> map = mapWithOrdering("insertion", false);
            // Deliberately odd number of elements – last key has no matching value.
            Object[] oddArray = new Object[] { "k1", "v1", "k2" };
            setInternalVal(map, oddArray);
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> map.get("anyKey"));
        }
    }

    /**
     * Directly sets the private {@code val} field of a {@link CompactMap}
     * instance via reflection.
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object newVal) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, newVal);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a {@link CompactMap} subclass that forces a specific ordering
     * and case‑sensitivity behaviour.  All other behaviour is delegated to the
     * original implementation.
     */
    private static <K, V> CompactMap<K, V> mapWithOrdering(String ordering, boolean caseInsensitive) {
        return new CompactMap<K, V>() {

            @Override
            protected String getOrdering() {
                return ordering;
            }

            @Override
            protected boolean isCaseInsensitive() {
                return caseInsensitive;
            }
        };
    }

    @Test
    void getFromEmptyMapReturnsNull() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertNull(map.get("anyKey"), "Getting any key from an empty map should return null");
    }

//     @Test
//     void getFromSingleEntryMap() {
//         // Force the map to use the single‑entry optimisation by overriding the
//         // configuration methods.
//         CompactMap<String, Double> map = new CompactMap<String, Double>() {
// 
//             @Override
//             protected int getCompactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "singleKey";
//             }
// 
//             @Override
//             protected Map<String, Double> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         // No entry yet – should be null.
//         assertNull(map.get("singleKey"));
//         // Insert the single entry.
//         map.put("singleKey", 42.0);
//         assertEquals(42.0, map.get("singleKey"));
//         // A different key still returns null.
//         assertNull(map.get("other"));
//     }

//     @Test
//     void getFromCompactArrayRepresentation() {
//         // compactSize = 5 → array is used for sizes 2‑5.
//         CompactMap<String, Integer> map = new CompactMap<String, Integer>() {
// 
//             @Override
//             protected int getCompactSize() {
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
//         assertEquals(1, map.get("one"));
//         assertEquals(2, map.get("two"));
//         assertEquals(3, map.get("three"));
//         // not present
//         assertNull(map.get("four"));
//     }

//     @Test
//     void getFromBackingMapWhenSizeExceedsCompactSize() {
//         // compactSize = 2 → after 3 inserts we switch to a regular Map.
//         CompactMap<String, String> map = new CompactMap<String, String>() {
// 
//             @Override
//             protected int getCompactSize() {
//                 return 2;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new HashMap<>();
//             }
//         };
//         map.put("a", "A");
//         map.put("b", "B");
//         // Still in compact array (size == 2)
//         assertEquals("A", map.get("a"));
//         assertEquals("B", map.get("b"));
//         // Third entry forces transition to a backing map.
//         map.put("c", "C");
//         assertEquals("C", map.get("c"));
//         assertEquals("A", map.get("a"));
//         assertEquals("B", map.get("b"));
//     }

    @Test
    void getWithSortedOrderingUsesBinarySearch() {
        // Builder creates a generated subclass that uses the sorted array path.
        // forces SORTED ordering
        CompactMap<String, Integer> // forces SORTED ordering
        map = // keep us in the array representation
        CompactMap.<String, Integer>builder().// keep us in the array representation
        sortedOrder().// default, but explicit
        compactSize(// default, but explicit
        10).caseSensitive(true).build();
        // Insert keys out of order to verify that internal sorting works.
        map.put("delta", 4);
        map.put("alpha", 1);
        map.put("charlie", 3);
        map.put("bravo", 2);
        // Retrieval must succeed regardless of insertion order.
        assertEquals(1, map.get("alpha"));
        assertEquals(2, map.get("bravo"));
        assertEquals(3, map.get("charlie"));
        assertEquals(4, map.get("delta"));
        // Missing key → null
        assertNull(map.get("echo"));
    }

    @Test
    void getWithCaseInsensitiveSortedOrdering() {
        // Case‑insensitive sorted map.
        // makes the comparator case‑insensitive
        CompactMap<String, String> // makes the comparator case‑insensitive
        map = // makes the comparator case‑insensitive
        CompactMap.<String, String>builder().sortedOrder().caseSensitive(false).compactSize(10).build();
        map.put("KeyOne", "value1");
        map.put("keyTwo", "value2");
        // Look‑ups with different case must succeed.
        assertEquals("value1", map.get("keyone"));
        assertEquals("value2", map.get("KEYTWO"));
        // Non‑existent key still returns null.
        assertNull(map.get("keythree"));
    }

    @Test
    void getWithReverseOrderingUsesBinarySearch() {
        // Reverse order also uses the binary‑search branch.
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().reverseOrder().compactSize(10).build();
        map.put("a", 1);
        map.put("c", 3);
        map.put("b", 2);
        // Retrieval works irrespective of the reverse ordering.
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
        assertNull(map.get("d"));
    }
}