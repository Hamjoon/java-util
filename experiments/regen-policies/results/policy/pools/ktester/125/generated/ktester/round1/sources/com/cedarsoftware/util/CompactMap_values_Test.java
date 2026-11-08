package com.cedarsoftware.util;

// import org.junit.*;
// import static org.junit.Assert.*;
class CompactMap_values_Test {

    /**
     * The map instance used in the tests.
     */
    private CompactMap<String, Object> map;

    //     @BeforeEach
//     void setupBeforeEach() {
//         map = new CompactMap<String, Object>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 // small size to force state changes quickly
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         // Populate the map with a few entries – enough to be larger than the single‑value state
//         map.put("value", 10.0d);
//         map.put("alpha", "beta");
//         map.put("gamma", 42);
//     }

    //     @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    /**
     * Initialise a small {@link CompactMap} that uses a {@link LinkedHashMap} as its backing store.
     */
//     private void setUp() {
//         map = new CompactMap<String, Object>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 // small size to force state changes quickly
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         // Populate the map with a few entries – enough to be larger than the single‑value state
//         map.put("value", 10.0d);
//         map.put("alpha", "beta");
//         map.put("gamma", 42);
//     }

    /**
     * Clean‑up after a test run.
     */
    private void tearDown() {
        map = null;
    }

    /**
     * Verify that the collection returned by {@code values()} reports the correct size.
     */
//     private void valuesCollectionReportsCorrectSize() {
//         Collection<Object> values = map.values();
//         assert values.size() == map.size() : "values().size() should match map.size()";
//         List<Object> expected = Arrays.asList(10.0d, "beta", 42);
//         assert values.containsAll(expected) && expected.containsAll(values) : "values() should contain exactly the map values";
//     }

    /**
     * Verify that iterating over the collection yields the map's values in insertion order.
     */
//     private void iteratorYieldsAllValuesInOrder() {
//         Collection<Object> values = map.values();
//         Iterator<Object> it = values.iterator();
//         List<Object> iterated = new ArrayList<>();
//         while (it.hasNext()) {
//             iterated.add(it.next());
//         }
//         List<Object> expected = Arrays.asList(10.0d, "beta", 42);
//         assert iterated.equals(expected) : "Iterator should return values in insertion order";
//     }

    /**
     * Verify that removing an element via the iterator updates the underlying map.
     */
//     private void iteratorRemoveUpdatesMap() {
//         Collection<Object> values = map.values();
//         Iterator<Object> it = values.iterator();
//         assert it.hasNext() : "Iterator must have a first element";
//         Object first = it.next();
//         assert Double.valueOf(10.0d).equals(first) : "First value should be 10.0d";
//         it.remove();
//         assert map.size() == 2 : "Map size should decrease after iterator.remove()";
//         assert !map.containsValue(10.0d) : "Removed value should no longer be present in the map";
//         assert !values.contains(10.0d) : "Values collection should reflect removal";
//     }

    /**
     * Verify that calling {@code clear()} on the values collection clears the map.
     */
//     private void valuesClearEmptiesMap() {
//         Collection<Object> values = map.values();
//         assert !values.isEmpty() : "Pre‑condition: values collection is not empty";
//         values.clear();
//         assert map.isEmpty() : "Map should be empty after values.clear()";
//         assert values.isEmpty() : "Values collection should be empty after clear()";
//     }

    /**
     * Verify that the iterator detects concurrent modification.
     */
//     private void iteratorDetectsConcurrentModification() {
//         Collection<Object> values = map.values();
//         Iterator<Object> it = values.iterator();
//         assert it.hasNext() : "Iterator must have a first element";
//         // advance once
//         it.next();
//         // external modification
//         map.put("newKey", "newValue");
//         try {
//             it.next();
//             assert false : "Iterator should have thrown ConcurrentModificationException";
//         } catch (ConcurrentModificationException expected) {
//             // expected – test passes
//         }
//     }

    /**
     * Verify behaviour on an empty CompactMap.
     */
//     private void valuesEmptyMap() {
//         CompactMap<String, Object> emptyMap = new CompactMap<String, Object>() {
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
//         Collection<Object> values = emptyMap.values();
//         assert values.size() == 0 : "Empty map should report collection size 0";
//         Iterator<Object> it = values.iterator();
//         assert !it.hasNext() : "Iterator on empty collection must have hasNext() == false";
//     }

    /**
     * Verify size and order for a three‑entry map.
     */
//     private void valuesNonEmptyMap() {
//         CompactMap<Integer, String> threeMap = new CompactMap<Integer, String>() {
// 
//             @Override
//             protected Integer getSingleValueKey() {
//                 // sentinel
//                 return 0;
//             }
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<Integer, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         threeMap.put(1, "alpha");
//         threeMap.put(2, "beta");
//         threeMap.put(3, "gamma");
//         Collection<String> values = threeMap.values();
//         assert values.size() == 3 : "Collection size must match number of entries";
//         List<String> iterated = new ArrayList<>();
//         for (String v : values) {
//             iterated.add(v);
//         }
//         List<String> expected = Arrays.asList("alpha", "beta", "gamma");
//         assert iterated.equals(expected) : "Iterator must return values in insertion order";
//     }

    /**
     * Test {@code contains()} on the values collection.
     */
//     private void valuesContainsValue() {
//         CompactMap<Integer, String> map = new CompactMap<Integer, String>() {
// 
//             @Override
//             protected Integer getSingleValueKey() {
//                 return 0;
//             }
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<Integer, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         map.put(1, "alpha");
//         map.put(2, "beta");
//         Collection<String> values = map.values();
//         assert values.contains("beta") : "Existing value must be reported as present";
//         assert !values.contains("delta") : "Non‑existing value must be reported as absent";
//     }

    /**
     * Calling {@code remove()} before {@code next()} should fail.
     */
//     private void valuesIteratorRemoveWithoutNext() {
//         Collection<Object> values = map.values();
//         Iterator<Object> it = values.iterator();
//         try {
//             it.remove();
//             assert false : "Calling iterator.remove() before next() must throw IllegalStateException";
//         } catch (IllegalStateException expected) {
//             // expected
//         }
//     }

    /**
     * Calling {@code next()} after iterator is exhausted must fail.
     */
//     private void valuesIteratorNextBeyondEnd() {
//         Collection<Object> values = map.values();
//         Iterator<Object> it = values.iterator();
//         while (it.hasNext()) {
//             it.next();
//         }
//         try {
//             it.next();
//             assert false : "Calling next() after hasNext() == false must throw NoSuchElementException";
//         } catch (NoSuchElementException expected) {
//             // expected
//         }
//     }

    /**
     * The values collection must not support {@code add()}.
     */
//     private void valuesAddUnsupported() {
//         Collection<Object> values = map.values();
//         try {
//             values.add("newValue");
//             assert false : "Adding to values() collection must throw UnsupportedOperationException";
//         } catch (UnsupportedOperationException expected) {
//             // expected
//         }
//     }

    /**
     * Second {@code remove()} without an intervening {@code next()} must fail.
     */
//     private void valuesIteratorDoubleRemove() {
//         Collection<Object> values = map.values();
//         Iterator<Object> it = values.iterator();
//         assert it.hasNext() : "Iterator must have at least one element";
//         // first element
//         it.next();
//         // valid first remove
//         it.remove();
//         try {
//             it.remove();
//             assert false : "Second remove() without next() must throw IllegalStateException";
//         } catch (IllegalStateException expected) {
//             // expected
//         }
//     }

    /**
     * Iterator must detect CME after a valid remove followed by external change.
     */
//     private void valuesIteratorRemoveAfterExternalModification() {
//         Collection<Object> values = map.values();
//         Iterator<Object> it = values.iterator();
//         assert it.hasNext() : "Iterator must have a first element";
//         // advance
//         it.next();
//         // remove that entry – map now size 2
//         it.remove();
//         // external modification
//         map.put("extra", "value");
//         try {
//             it.next();
//             assert false : "Iterator must throw ConcurrentModificationException after external modification post‑remove";
//         } catch (ConcurrentModificationException expected) {
//             // expected
//         }
//     }

    /**
     * Run all demo checks.
     */
    private void runAll() {
        setUp();
        try {
            valuesCollectionReportsCorrectSize();
            iteratorYieldsAllValuesInOrder();
            iteratorRemoveUpdatesMap();
            valuesClearEmptiesMap();
            iteratorDetectsConcurrentModification();
            valuesEmptyMap();
            valuesNonEmptyMap();
            valuesContainsValue();
            valuesIteratorRemoveWithoutNext();
            valuesIteratorNextBeyondEnd();
            valuesAddUnsupported();
            valuesIteratorDoubleRemove();
            valuesIteratorRemoveAfterExternalModification();
        } finally {
            tearDown();
        }
    }

    /**
     * Entry point for the demo.
     */
//     public static void main(String[] args) {
//         new CompactMapValuesDemo().runAll();
//         System.out.println("All CompactMap values() checks passed.");
//     }
}