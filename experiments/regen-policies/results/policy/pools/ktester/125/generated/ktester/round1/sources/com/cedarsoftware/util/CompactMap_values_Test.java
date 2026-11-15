package com.cedarsoftware.util;

import java.util.*;
// import java.util.concurrent.ConcurrentModificationException;
// import org.junit.*;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class CompactMap_values_Test {

    /**
     * Map under test – created as an anonymous subclass so we can control the configuration.
     */
    private CompactMap<String, Integer> map;

    //     @RunWith(Parameterized.class)
    public static class OrderingBehavesAccordingToConfiguration {

        private final String ordering;

        public OrderingBehavesAccordingToConfiguration(String ordering) {
            this.ordering = ordering;
        }

        //         @Parameterized.Parameters(name = "{0}")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][] { { "insertion" }, { "sorted" }, { "reverse" } });
        //         }
//         @Test
//         public void orderingBehavesAccordingToConfiguration() {
//             CompactMap.Builder<String, Integer> builder = CompactMap.builder();
//             switch(ordering) {
//                 case "insertion":
//                     builder.insertionOrder();
//                     break;
//                 case "sorted":
//                     builder.sortedOrder();
//                     break;
//                 case "reverse":
//                     builder.reverseOrder();
//                     break;
//             }
//             CompactMap<String, Integer> orderedMap = builder.build();
//             orderedMap.put("c", 3);
//             orderedMap.put("a", 1);
//             orderedMap.put("b", 2);
//             List<Integer> iterated = new ArrayList<>(orderedMap.values());
//             switch(ordering) {
//                 case "insertion":
//                     assertEquals(Arrays.asList(3, 1, 2), iterated);
//                     break;
//                 case "sorted":
//                     assertEquals(Arrays.asList(1, 2, 3), iterated);
//                     break;
//                 case "reverse":
//                     assertEquals(Arrays.asList(2, 1, 3), iterated);
//                     break;
//                 default:
//                     fail("Unexpected ordering: " + ordering);
//             }
//         }
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Create a CompactMap with a very small compact size so we exercise the
        // different internal states (empty, single entry, compact array, backing map).
        map = new CompactMap<String, Integer>() {

            @Override
            protected String getSingleValueKey() {
                // Use a key that will not clash with our test data.
                return "singleKey";
            }

            @Override
            protected int compactSize() {
                // Small size to force a transition to a backing map during the test.
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // Backing map type – LinkedHashMap preserves insertion order,
                // which is convenient for deterministic iteration in the tests.
                return new LinkedHashMap<>();
            }
        };
    }

//     @AfterEach
    void teardownAfterEach() {
        if (map != null) {
            map.clear();
        }
        map = null;
    }

    /**
     * Verify that the collection returned by {@code values()} reports the correct size.
     */
//     @Test
//     void valuesCollectionSizeReflectsMapSize() {
//         Collection<Integer> values = map.values();
//         assertEquals(0, values.size());
//         assertTrue(values.isEmpty());
//         map.put("a", 1);
//         assertEquals(1, values.size());
//         assertFalse(values.isEmpty());
//         map.put("b", 2);
//         map.put("c", 3);
//         assertEquals(3, values.size());
//         // Adding a fourth entry forces a transition to the backing map (compactSize = 3)
//         map.put("d", 4);
//         assertEquals(4, values.size());
//     }

    /**
     * {@code clear()} on the values collection must clear the underlying map.
     */
//     @Test
//     void valuesCollectionClearEmptiesMap() {
//         map.put("x", 10);
//         map.put("y", 20);
//         map.put("z", 30);
//         assertEquals(3, map.size());
//         Collection<Integer> values = map.values();
//         values.clear();
//         assertEquals(0, map.size());
//         assertTrue(map.isEmpty());
//         assertTrue(values.isEmpty());
//     }

    /**
     * The iterator returned by the values collection must iterate over the map's values
     *  and support {@code remove()}.
     */
//     @Test
//     void valuesIteratorIteratesAndRemoves() {
//         map.put("one", 1);
//         map.put("two", 2);
//         map.put("three", 3);
//         Collection<Integer> values = map.values();
//         Iterator<Integer> it = values.iterator();
//         List<Integer> iterated = new ArrayList<>();
//         while (it.hasNext()) {
//             Integer v = it.next();
//             iterated.add(v);
//             if (v == 2) {
//                 it.remove();
//             }
//         }
//         assertEquals(Arrays.asList(1, 2, 3), iterated);
//         assertFalse(map.containsValue(2));
//         assertEquals(2, map.size());
//         assertEquals(new HashSet<>(Arrays.asList(1, 3)), new HashSet<>(values));
//     }

    /**
     * The iterator must throw {@link NoSuchElementException} when {@code next()}
     *  is called after the collection has been exhausted.
     */
//     @Test
//     void iteratorThrowsNoSuchElementExceptionWhenExhausted() {
//         map.put("k", 42);
//         Iterator<Integer> it = map.values().iterator();
//         assertTrue(it.hasNext());
//         assertEquals(Integer.valueOf(42), it.next());
//         assertFalse(it.hasNext());
//         try {
//             it.next();
//             fail("Expected NoSuchElementException");
//         } catch (NoSuchElementException ignored) {
//         }
//     }

    /**
     * Modifications to the map after obtaining the values collection must be visible
     *  through the collection.
     */
//     @Test
//     void valuesReflectMapChangesAfterCreation() {
//         Collection<Integer> values = map.values();
//         assertTrue(values.isEmpty());
//         map.put("a", 100);
//         map.put("b", 200);
//         assertEquals(2, values.size());
//         assertTrue(values.containsAll(Arrays.asList(100, 200)));
//         map.remove("a");
//         assertEquals(1, values.size());
//         assertFalse(values.contains(100));
//         assertTrue(values.contains(200));
//     }

//     @Test
//     void collectionDoesNotSupportAddOperation() {
//         Collection<Integer> values = map.values();
//         try {
//             values.add(99);
//             fail("add should throw UnsupportedOperationException");
//         } catch (UnsupportedOperationException ignored) {
//         }
//         try {
//             values.addAll(Collections.singleton(1));
//             fail("addAll should throw UnsupportedOperationException");
//         } catch (UnsupportedOperationException ignored) {
//         }
//     }

//     @Test
//     void collectionRemoveByValueDeletesFirstMatchingEntry() {
//         map.put("k1", 5);
//         map.put("k2", 5);
//         map.put("k3", 7);
//         Collection<Integer> values = map.values();
//         assertTrue(values.remove(5));
//         assertEquals(2, map.size());
//         assertFalse(map.containsKey("k1"));
//         assertTrue(map.containsKey("k2"));
//         assertFalse(values.remove(999));
//         assertEquals(2, map.size());
//     }

//     @Test
//     void collectionContainsReflectsMapState() {
//         map.put("a", 1);
//         map.put("b", 2);
//         Collection<Integer> values = map.values();
//         assertTrue(values.contains(1));
//         assertTrue(values.contains(2));
//         assertFalse(values.contains(3));
//         map.remove("a");
//         assertFalse(values.contains(1));
//         assertTrue(values.contains(2));
//     }

//     @Test
//     void toArrayReturnsSnapshotInIterationOrder() {
//         map.put("first", 10);
//         map.put("second", 20);
//         map.put("third", 30);
//         Collection<Integer> values = map.values();
//         Object[] array = values.toArray();
//         assertEquals(3, array.length);
//         assertArrayEquals(new Object[] { 10, 20, 30 }, array);
//     }

//     @Test
//     void valuesIncludeNullsAndDuplicateValues() {
//         map.put("n1", null);
//         map.put("n2", null);
//         map.put("v1", 42);
//         map.put("v2", 42);
//         Collection<Integer> values = map.values();
//         assertEquals(4, values.size());
//         List<Integer> list = new ArrayList<>(values);
//         assertEquals(2, Collections.frequency(list, null));
//         assertEquals(2, Collections.frequency(list, 42));
//     }

//     @Test
//     void iteratorDetectsConcurrentModificationOnPut() {
//         map.put("a", 1);
//         map.put("b", 2);
//         Iterator<Integer> it = map.values().iterator();
//         assertTrue(it.hasNext());
//         assertEquals(Integer.valueOf(1), it.next());
//         map.put("c", 3);
//         try {
//             it.next();
//             fail("Expected ConcurrentModificationException");
//         } catch (ConcurrentModificationException ignored) {
//         }
//     }

//     @Test
//     void iteratorDetectsConcurrentModificationOnClear() {
//         map.put("x", 9);
//         Iterator<Integer> it = map.values().iterator();
//         assertTrue(it.hasNext());
//         assertEquals(Integer.valueOf(9), it.next());
//         map.clear();
//         try {
//             it.next();
//             fail("Expected ConcurrentModificationException");
//         } catch (ConcurrentModificationException ignored) {
//         }
//     }

//     @Test
//     void iteratorRemoveWithoutNextThrowsIllegalStateException() {
//         map.put("a", 1);
//         Iterator<Integer> it = map.values().iterator();
//         try {
//             it.remove();
//             fail("Expected IllegalStateException");
//         } catch (IllegalStateException ignored) {
//         }
//     }

//     @Test
//     void iteratorRemoveCalledTwiceThrowsIllegalStateException() {
//         map.put("a", 1);
//         map.put("b", 2);
//         Iterator<Integer> it = map.values().iterator();
//         assertTrue(it.hasNext());
//         it.next();
//         it.remove();
//         try {
//             it.remove();
//             fail("Expected IllegalStateException");
//         } catch (IllegalStateException ignored) {
//         }
//     }

//     @Test
//     void iteratorRemoveAfterExternalRemovalThrowsIllegalStateException() {
//         map.put("a", 1);
//         map.put("b", 2);
//         Iterator<Integer> it = map.values().iterator();
//         assertTrue(it.hasNext());
//         Integer val = it.next();
//         assertEquals(Integer.valueOf(1), val);
//         map.remove("a");
//         try {
//             it.remove();
//             fail("Expected IllegalStateException");
//         } catch (IllegalStateException ignored) {
//         }
//     }

//     @ParameterizedTest
//     @ValueSource(strings = { "insertion", "sorted", "reverse" })
//     void orderingBehavesAccordingToConfiguration(String ordering) {
//         // Build a map with the requested ordering using the builder API.
//         CompactMap.Builder<String, Integer> builder = CompactMap.builder();
//         switch(ordering) {
//             case "insertion":
//                 builder.insertionOrder();
//                 break;
//             case "sorted":
//                 builder.sortedOrder();
//                 break;
//             case "reverse":
//                 builder.reverseOrder();
//                 break;
//             default:
//                 fail("Unexpected ordering: " + ordering);
//         }
//         CompactMap<String, Integer> orderedMap = builder.build();
//         // Insert keys in a non‑natural order to expose ordering behaviour.
//         orderedMap.put("c", 3);
//         orderedMap.put("a", 1);
//         orderedMap.put("b", 2);
//         List<Integer> iterated = new ArrayList<>(orderedMap.values());
//         switch(ordering) {
//             case "insertion":
//                 assertEquals(Arrays.asList(3, 1, 2), iterated, "Insertion order should preserve the order of puts");
//                 break;
//             case "sorted":
//                 // Keys sorted alphabetically: a, b, c
//                 assertEquals(Arrays.asList(1, 2, 3), iterated, "Sorted order should follow natural key ordering");
//                 break;
//             case "reverse":
//                 // Reverse of insertion: b, a, c
//                 assertEquals(Arrays.asList(2, 1, 3), iterated, "Reverse order should be the opposite of insertion order");
//                 break;
//         }
//     }

//     @Test
//     void collectionSizeUpdatesAfterDirectMapRemoval() {
//         map.put("x", 10);
//         map.put("y", 20);
//         map.put("z", 30);
//         Collection<Integer> values = map.values();
//         assertEquals(3, values.size());
//         map.remove("y");
//         assertEquals(2, values.size());
//         assertFalse(values.contains(20));
//     }

//     @Test
//     void collectionEmptyAfterDirectMapClear() {
//         map.put("p", 5);
//         map.put("q", 6);
//         Collection<Integer> values = map.values();
//         assertFalse(values.isEmpty());
//         map.clear();
//         assertTrue(values.isEmpty());
//         assertEquals(0, values.size());
//     }

//     @Test
//     public void orderingInsertion() {
//         orderingBehavesAccordingToConfiguration("insertion");
//     }

//     @Test
//     public void orderingSorted() {
//         orderingBehavesAccordingToConfiguration("sorted");
//     }

//     @Test
//     public void orderingReverse() {
//         orderingBehavesAccordingToConfiguration("reverse");
//     }
}