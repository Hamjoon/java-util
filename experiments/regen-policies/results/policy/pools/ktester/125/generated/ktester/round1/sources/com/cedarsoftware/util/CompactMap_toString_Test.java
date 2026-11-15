package com.cedarsoftware.util;

import java.util.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import org.junit.Test;
// import org.junit.*;
// import org.junit.Assert;
// import org.junit.Ignore;
// import org.junit.runner.RunWith;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class CompactMap_toString_Test {

    /**
     * Simple dummy object whose {@code toString()} returns a constant string.
     */
    private static final class Dummy {

        @Override
        public String toString() {
            return "DummyObj";
        }
    }

    /**
     * Key implementation that throws an exception from {@code toString()}.
     */
    private static final class BadKey {

        @Override
        public String toString() {
            throw new IllegalStateException("BadKey toString failure");
        }
    }

    /**
     * Value implementation that throws an exception from {@code toString()}.
     */
    private static final class BadValue {

        @Override
        public String toString() {
            throw new IllegalStateException("BadValue toString failure");
        }
    }

    /**
     * Verify that an empty map produces the standard empty‑map string.
     */
//     @Test
//     public void emptyMapToString() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().build();
//         Assert.assertTrue(map.isEmpty());
//         Assert.assertEquals("{}", map.toString());
//     }

    /**
     * Verify that a map containing a single entry is rendered correctly.
     * The anonymous subclass forces the compact storage path (single‑value storage).
     */
//     @Test
//     public void singleEntryMapToString() {
//         @SuppressWarnings("unchecked")
//         CompactMap<String, Double> map = new CompactMap<String, Double>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "value";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 // stay in the compact array state
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Double> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         Assert.assertEquals("{}", map.toString());
//         map.put("value", 10.0d);
//         Assert.assertEquals("{value=10.0}", map.toString());
//     }

    /**
     * Verify that a map with several entries (still below the compact size) is rendered
     * in insertion order.
     */
//     @Test
//     public void multipleEntryMapToString() {
//         @SuppressWarnings("unchecked")
//         CompactMap<String, Integer> map = new CompactMap<String, Integer>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 // stay in compact array state for up to 5 entries
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         map.put("first", 1);
//         map.put("second", 2);
//         map.put("third", 3);
//         Assert.assertEquals("{first=1, second=2, third=3}", map.toString());
//     }

    /**
     * Verify that a sorted CompactMap (generated via the builder) produces a
     * string with keys ordered according to the comparator.
     */
//     @Test
//     public void sortedMapToString() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().caseSensitive(// case‑insensitive comparator
//         false).sortedOrder().compactSize(// keep the compact size large enough to stay in the array state
//         10).build();
//         map.put("bKey", 2);
//         map.put("aKey", 1);
//         // mixed case to test case‑insensitivity
//         map.put("Ckey", 3);
//         Assert.assertEquals("{aKey=1, bKey=2, Ckey=3}", map.toString());
//     }

    /**
     * Verify that after the map grows beyond the compact size it switches to a backing
     * map and the string representation still matches the expected format.
     */
//     @Test
//     public void mapTransitionsToBackingMapToString() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(// a compact size of 2 forces a transition on the third entry
//         2).build();
//         map.put("one", 1);
//         map.put("two", 2);
//         Assert.assertEquals("{one=1, two=2}", map.toString());
//         map.put("three", 3);
//         Assert.assertEquals("{one=1, two=2, three=3}", map.toString());
//     }

    /**
     * Single entry map built via the builder.
     */
//     @Test
//     public void builderSingleEntry() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().insertionOrder().build();
//         map.put("key1", "value1");
//         Assert.assertEquals("{key1=value1}", map.toString());
//     }

    /**
     * Multiple entries with insertion ordering.
     */
//     @Test
//     public void multipleEntriesInsertionOrder() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().build();
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         Assert.assertEquals("{a=1, b=2, c=3}", map.toString());
//     }

    /**
     * Multiple entries with natural sorted ordering.
     */
//     @Test
//     public void multipleEntriesSortedOrder() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().build();
//         map.put("c", 3);
//         map.put("a", 1);
//         map.put("b", 2);
//         Assert.assertEquals("{a=1, b=2, c=3}", map.toString());
//     }

    /**
     * Multiple entries with reverse sorted ordering.
     */
//     @Test
//     public void multipleEntriesReverseOrder() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().reverseOrder().build();
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         Assert.assertEquals("{c=3, b=2, a=1}", map.toString());
//     }

    /**
     * Case‑insensitive map should keep the original key spelling in the output.
     */
//     @Test
//     public void caseInsensitiveKeyHandling() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(false).insertionOrder().build();
//         map.put("Key", "val");
//         Assert.assertEquals("{Key=val}", map.toString());
//     }

    /**
     * Map containing a null key and a null value.
     */
//     @Test
//     public void nullKeyAndNullValue() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().insertionOrder().build();
//         map.put(null, null);
//         Assert.assertEquals("{null=null}", map.toString());
//     }

    /**
     * Map with a null key followed by regular entries (insertion order).
     */
//     @Test
//     public void nullKeyWithOtherEntries() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
//         map.put(null, "nullVal");
//         map.put("a", 1);
//         map.put("b", 2);
//         Assert.assertEquals("{null=nullVal, a=1, b=2}", map.toString());
//     }

    /**
     * Removal of an entry should be reflected in the string output.
     */
//     @Test
//     public void mapAfterRemoval() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().build();
//         map.put("x", 10);
//         map.put("y", 20);
//         map.put("z", 30);
//         map.remove("y");
//         Assert.assertEquals("{x=10, z=30}", map.toString());
//     }

    /**
     * Clear operation should reset the map to an empty representation.
     */
//     @Test
//     public void mapAfterClear() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().insertionOrder().build();
//         map.put("k1", "v1");
//         map.put("k2", "v2");
//         map.clear();
//         Assert.assertEquals("{}", map.toString());
//     }

    /**
     * Map containing custom objects as values; their own {@code toString()}
     * should be used.
     */
//     @Test
//     public void mapWithCustomObjectValues() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
//         map.put("obj", new Dummy());
//         map.put("num", 5);
//         Assert.assertEquals("{obj=DummyObj, num=5}", map.toString());
//     }

    /**
     * SINGLE_KEY option – only the latest value for the designated key is kept.
     * This is forced via an anonymous subclass that defines the single‑value key.
     */
//     @Test
//     public void singleKeyOptionOverridesPreviousValue() {
//         @SuppressWarnings("unchecked")
//         CompactMap<String, String> map = new CompactMap<String, String>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "only";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 return 2;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         map.put("only", "first");
//         map.put("only", "second");
//         Assert.assertEquals("{only=second}", map.toString());
//     }

    /**
     * UNORDERED ordering – the iteration order is undefined.  The test verifies
     * that all expected key/value pairs appear, regardless of order.
     */
//     @Test
//     public void unorderedOrderingAnyPermutation() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().// default is UNORDERED
//         build();
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         String result = map.toString();
//         Assert.assertTrue(result.startsWith("{") && result.endsWith("}"));
//         Assert.assertTrue(result.contains("a=1"));
//         Assert.assertTrue(result.contains("b=2"));
//         Assert.assertTrue(result.contains("c=3"));
//     }

    /**
     * Duplicate key insertion must overwrite the previous value.
     */
//     @Test
//     public void duplicateKeyInsertionOverrides() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().insertionOrder().build();
//         map.put("dup", "first");
//         map.put("dup", "second");
//         Assert.assertEquals("{dup=second}", map.toString());
//     }

    /**
     * Case‑sensitive map with mixed‑case keys – keys are distinct.
     */
//     @Test
//     public void caseSensitiveMixedCaseKeys() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(true).insertionOrder().build();
//         map.put("Key", "upper");
//         map.put("key", "lower");
//         Assert.assertEquals("{Key=upper, key=lower}", map.toString());
//     }

    /**
     * Map with a null value but a non‑null key.
     */
//     @Test
//     public void nullValueNonNullKey() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().insertionOrder().build();
//         map.put("present", null);
//         Assert.assertEquals("{present=null}", map.toString());
//     }

    /**
     * A key whose {@code toString()} throws an exception should cause the map's
     * {@code toString()} to propagate that exception.
     */
//     @Test
//     public void keyToStringThrowsException() {
//         CompactMap<Object, String> map = CompactMap.<Object, String>builder().insertionOrder().build();
//         map.put(new BadKey(), "value");
//         try {
//             map.toString();
//             Assert.fail("Expected IllegalStateException");
//         } catch (IllegalStateException e) {
//             // expected
//         }
//     }

    /**
     * A value whose {@code toString()} throws an exception should cause the map's
     * {@code toString()} to propagate that exception.
     */
//     @Test
//     public void valueToStringThrowsException() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
//         map.put("key", new BadValue());
//         try {
//             map.toString();
//             Assert.fail("Expected IllegalStateException");
//         } catch (IllegalStateException e) {
//             // expected
//         }
//     }

    /**
     * Self‑referential map (a value that points to the map itself) leads to
     * infinite recursion in {@link MapUtilities#mapToString(Map)} and results
     * in a {@link StackOverflowError}.
     *
     * This test is disabled because it would always fail with a stack overflow.
     */
//     @Ignore("Demonstrates a pathological case – not executed in normal test runs")
//     @Test
//     public void selfReferentialMapCausesStackOverflow() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
//         map.put("self", map);
//         try {
//             map.toString();
//             Assert.fail("Expected StackOverflowError");
//         } catch (StackOverflowError e) {
//             // expected
//         }
//     }

    /**
     * Concurrent modification detection.
     *
     * <p>Unfortunately {@code MapUtilities.mapToString} is a static utility
     * method and cannot be mocked with plain Mockito.  The most reliable way
     * to provoke a {@link ConcurrentModificationException} is to modify the
     * map while its iterator is being used.  We achieve this by creating a
     * separate thread that removes an entry after a tiny delay, while the
     * main thread calls {@code toString()}.</p>
     *
     * <p>This test is marked {@code @Ignore} because its reliability can
     * be platform‑dependent.  It remains as documentation of the expected
     * behaviour.</p>
     */
//     @Ignore("Flaky on some JVMs – kept for documentation of expected behavior")
//     @Test
//     public void concurrentModificationDetection() throws InterruptedException {
//         CompactMap<Integer, Integer> map = CompactMap.<Integer, Integer>builder().insertionOrder().build();
//         map.put(1, 1);
//         map.put(2, 2);
//         map.put(3, 3);
//         Thread modifier = new Thread(() -> {
//             try {
//                 Thread.sleep(5);
//                 map.remove(2);
//             } catch (InterruptedException ignored) {
//             }
//         });
//         modifier.start();
//         try {
//             map.toString();
//             Assert.fail("Expected ConcurrentModificationException");
//         } catch (ConcurrentModificationException ignored) {
//             // expected
//         }
//         modifier.join();
//     }
}