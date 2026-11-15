package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.junit.runners.JUnit4;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(JUnit4.class)
class CompactMap_toString_Test {

    /**
     * Verify that {@code CompactMap.toString()} returns the expected string
     * representation for a map with several entries.  The map is created with
     * insertion ordering so the iteration order (and therefore the string order)
     * is predictable.
     */
//     @Test
//     void testToStringWithEntries() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().build();
//         map.put("alpha", 1);
//         map.put("beta", 2);
//         map.put("gamma", 3);
//         assertEquals("{alpha=1, beta=2, gamma=3}", map.toString());
//     }

    /**
     * Verify that an empty {@code CompactMap} renders as an empty map string.
     */
//     @Test
//     void testToStringEmptyMap() {
//         CompactMap<String, Object> emptyMap = CompactMap.<String, Object>builder().insertionOrder().build();
//         assertEquals("{}", emptyMap.toString());
//     }

//     @Test
//     void testToStringSingleEntry() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().build();
//         map.put("id", 123);
//         assertEquals("{id=123}", map.toString());
//     }

//     @Test
//     void testToStringMultipleEntries() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().build();
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         assertEquals("{a=1, b=2, c=3}", map.toString());
//     }

//     @Test
//     void testToStringNullKey() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().insertionOrder().build();
//         map.put(null, "val");
//         assertEquals("{null=val}", map.toString());
//     }

//     @Test
//     void testToStringNullValue() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().insertionOrder().build();
//         map.put("key", null);
//         assertEquals("{key=null}", map.toString());
//     }

//     @Test
//     void testToStringNestedCompactMap() {
//         CompactMap<String, Integer> inner = CompactMap.<String, Integer>builder().insertionOrder().build();
//         inner.put("inner", 42);
//         CompactMap<String, Object> outer = CompactMap.<String, Object>builder().insertionOrder().build();
//         outer.put("outer", inner);
//         assertEquals("{outer={inner=42}}", outer.toString());
//     }

//     @Test
//     void testToStringSpecialCharacters() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().insertionOrder().build();
//         map.put("key with spaces", "value,with,commas");
//         // Expected quoting as defined by MapUtilities.mapToString
//         assertEquals("{\"key with spaces\"=\"value,with,commas\"}", map.toString());
//     }

//     @Test
//     void testToStringSortedOrdering() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().sortedOrder().build();
//         map.put("b", 2);
//         map.put("a", 1);
//         map.put("c", 3);
//         assertEquals("{a=1, b=2, c=3}", map.toString());
//     }

//     @Test
//     void testToStringReverseOrdering() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().reverseOrder().build();
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         assertEquals("{c=3, b=2, a=1}", map.toString());
//     }

//     @Test
//     void testToStringDuplicateKeyOverwrite() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().build();
//         map.put("id", 123);
//         // overwrite
//         map.put("id", 456);
//         assertEquals("{id=456}", map.toString());
//     }

//     @Test
//     void testToStringNestedKeyCompactMap() {
//         CompactMap<String, Integer> inner = CompactMap.<String, Integer>builder().insertionOrder().build();
//         inner.put("inner", 42);
//         CompactMap<Object, String> map = CompactMap.<Object, String>builder().insertionOrder().build();
//         map.put(inner, "val");
//         assertEquals("{{inner=42}=val}", map.toString());
//     }

//     @Test
//     void testToStringCollectionValue() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
//         List<Integer> list = Arrays.asList(1, 2, 3);
//         map.put("list", list);
//         // Expected quoting of the collection's toString()
//         assertEquals("{list=\"[1, 2, 3]\"}", map.toString());
//     }

//     @Test
//     void testToStringCaseInsensitiveKey() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().caseSensitive(false).insertionOrder().build();
//         map.put("Key", 1);
//         // should overwrite because case‑insensitive
//         map.put("key", 2);
//         assertEquals("{key=2}", map.toString());
//     }

//     @Test
//     void testToStringCustomSingleKey() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().singleValueKey("code").insertionOrder().build();
//         map.put("code", 789);
//         assertEquals("{code=789}", map.toString());
//     }

//     @Test
//     void testConcurrentModification() throws Exception {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().build();
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         CountDownLatch startLatch = new CountDownLatch(1);
//         CountDownLatch modifyLatch = new CountDownLatch(1);
//         ExecutorService executor = Executors.newSingleThreadExecutor();
//         Future<String> future = executor.submit(() -> {
//             try {
//                 // signal iteration start
//                 startLatch.countDown();
//                 // wait for concurrent modification
//                 modifyLatch.await();
//                 // should throw CME
//                 return map.toString();
//             } catch (ConcurrentModificationException e) {
//                 return "ConcurrentModificationException";
//             }
//         });
//         // ensure the toString thread is iterating
//         startLatch.await();
//         // concurrent modification
//         map.put("d", 4);
//         modifyLatch.countDown();
//         String result = future.get(2, TimeUnit.SECONDS);
//         executor.shutdownNow();
//         assertEquals("ConcurrentModificationException", result);
//     }

//     @Test
//     void testSelfReferenceStackOverflow() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
//         map.put("self", map);
//         assertThrows(StackOverflowError.class, map::toString);
//     }

//     @Test
//     void testSortedNonComparableKeys() {
//         CompactMap<Object, String> map = CompactMap.<Object, String>builder().sortedOrder().build();
//         map.put(1, "one");
//         // heterogeneous keys – not mutually comparable
//         map.put("b", "two");
//         assertThrows(ClassCastException.class, map::toString);
//     }

//     @Test
//     void testNullInternalMap() throws Exception {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().insertionOrder().build();
//         // Force internal storage field `val` to null via reflection
//         Field valField = CompactMap.class.getDeclaredField("val");
//         valField.setAccessible(true);
//         valField.set(map, null);
//         assertThrows(NullPointerException.class, map::toString);
//     }

//     @ParameterizedTest
//     @MethodSource("emptyMapProvider")
//     void parameterizedEmptyMapTest(CompactMap<?, ?> map, String expected) {
//         assertEquals(expected, map.toString(), "Empty map should be represented as {}");
//     }

    // runnable under JUnit 4.
//     @Test
//     public void testEmptyMapDirectly() {
//         CompactMap<?, ?> map = CompactMap.<Object, Object>builder().insertionOrder().build();
//         assertEquals("{}", map.toString());
//     }
}