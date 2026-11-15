package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

// @ExtendWith(MockitoExtension.class)
class CompactMap_put_Test {

//     public static void main(String[] args) throws Exception {
//         // Enable assertions when running from the command line:
//         //   java -ea com.cedarsoftware.util.CompactMapPutTestRunner
//         CompactMapPutTestRunner runner = new CompactMapPutTestRunner();
//         runner.testPutSingleValueKeyOptimizedStorage();
//         runner.testPutOverwriteReturnsPrevious();
//         runner.testPutTransitionToCompactArray();
//         runner.testPutTransitionToBackingMap();
//         runner.testPutWithMapValueNotOptimized();
//         runner.testEmptyMapCreatesCompactMapEntry();
//         runner.testBackingMapPutDelegatesAndReturnsPrevious();
//         runner.testCompactArrayPutAddsEntry();
//         runner.testEmptyMapWithObjectArrayValueNotOptimized();
//         runner.testEmptyMapNullKeyCreatesEntry();
//         runner.testEmptyMapNullValueOptimized();
//         runner.testSingleEntryCompactMapEntryOverwrite();
//         runner.testSingleEntryToCompactArrayConversion();
//         runner.testCompactArrayTransitionToBackingMap();
//         runner.testBackingMapOverwriteReturnsPrevious();
//         runner.testCaseInsensitivePutReplacesIgnoringCase();
//         runner.testTreeMapNullKeyThrowsNPE();
//         runner.testTreeMapNonComparableKeyThrowsCCE();
//         runner.testConcurrentHashMapNullKeyThrowsNPE();
//         runner.testConcurrentHashMapNullValueThrowsNPE();
//         runner.testCaseInsensitiveTreeMapNullKeyThrowsNPE();
//         System.out.println("All CompactMap tests passed.");
//     }

//     @Test
//     void testPutSingleValueKeyOptimizedStorage() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().singleValueKey("id").compactSize(5).mapType(LinkedHashMap.class).build();
//         assert map.isEmpty();
//         assert map.put("id", 42) == null;
//         assert map.size() == 1;
//         assert map.get("id").equals(42);
//     }

//     @Test
//     void testPutOverwriteReturnsPrevious() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().singleValueKey("key").compactSize(4).mapType(HashMap.class).build();
//         assert map.put("key", "first") == null;
//         assert map.get("key").equals("first");
//         // Overwrite same key – should return previous value
//         assert map.put("key", "second").equals("first");
//         assert map.get("key").equals("second");
//         assert map.size() == 1;
//     }

//     @Test
//     void testPutTransitionToCompactArray() {
//         CompactMap<Integer, String> map = CompactMap.<Integer, String>builder().compactSize(// allow array up to size 3
//         3).mapType(LinkedHashMap.class).build();
//         // first entry – single entry state
//         assert map.put(1, "one") == null;
//         // second distinct entry – should move to compact array
//         assert map.put(2, "two") == null;
//         assert map.size() == 2;
//         assert map.get(1).equals("one");
//         assert map.get(2).equals("two");
//         // third entry – still within compact array limits
//         assert map.put(3, "three") == null;
//         assert map.size() == 3;
//         assert map.get(3).equals("three");
//     }

//     @Test
//     void testPutTransitionToBackingMap() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(// transition after the 2nd entry
//         2).mapType(HashMap.class).build();
//         // Populate three entries – the third forces the transition
//         assert map.put("a", 1) == null;
//         assert map.put("b", 2) == null;
//         // transition point
//         assert map.put("c", 3) == null;
//         assert map.size() == 3;
//         assert map.get("a") == 1;
//         assert map.get("b") == 2;
//         assert map.get("c") == 3;
//     }

//     @Test
//     void testPutWithMapValueNotOptimized() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().singleValueKey("meta").compactSize(4).mapType(LinkedHashMap.class).build();
//         Map<String, String> inner = new HashMap<>();
//         inner.put("x", "y");
//         // Because the value is a Map, the optimized single‑value path must be bypassed
//         assert map.put("meta", inner) == null;
//         assert map.size() == 1;
//         assert map.get("meta") == inner;
//     }

//     @Test
//     void testEmptyMapCreatesCompactMapEntry() throws Exception {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().singleValueKey("id").compactSize(5).mapType(LinkedHashMap.class).build();
//         assert map.put("foo", 99) == null;
//         Field f = CompactMap.class.getDeclaredField("val");
//         f.setAccessible(true);
//         Object internal = f.get(map);
//         assert internal instanceof CompactMap.CompactMapEntry : "Expected a CompactMapEntry after first put when key != singleValueKey";
//         assert map.get("foo") == 99;
//     }

//     @Test
//     void testBackingMapPutDelegatesAndReturnsPrevious() throws Exception {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(// forces immediate backing‑map usage
//         1).mapType(HashMap.class).build();
//         map.put("alpha", 123);
//         map.put("beta", 456);
//         assert map.size() >= 2;
//         assert map.put("alpha", 999) == 123;
//         assert map.get("alpha") == 999;
//         Field f = CompactMap.class.getDeclaredField("val");
//         f.setAccessible(true);
//         Object internal = f.get(map);
//         assert internal instanceof Map : "Expected internal storage to be a Map";
//     }

//     @Test
//     void testCompactArrayPutAddsEntry() throws Exception {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(5).mapType(LinkedHashMap.class).build();
//         map.put("a", 1);
//         map.put("b", 2);
//         assert map.put("c", 3) == null;
//         Field f = CompactMap.class.getDeclaredField("val");
//         f.setAccessible(true);
//         Object internal = f.get(map);
//         assert internal instanceof Object[] : "Internal storage should be an Object[]";
//         Object[] arr = (Object[]) internal;
//         assert "a".equals(arr[0]);
//         assert Integer.valueOf(1).equals(arr[1]);
//         assert "b".equals(arr[2]);
//         assert Integer.valueOf(2).equals(arr[3]);
//         assert "c".equals(arr[4]);
//         assert Integer.valueOf(3).equals(arr[5]);
//     }

//     @Test
//     void testEmptyMapWithObjectArrayValueNotOptimized() throws Exception {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().singleValueKey("id").compactSize(4).mapType(LinkedHashMap.class).build();
//         Object[] arrayValue = new Object[] { "a", 1 };
//         assert map.put("id", arrayValue) == null;
//         Field f = CompactMap.class.getDeclaredField("val");
//         f.setAccessible(true);
//         Object internal = f.get(map);
//         assert internal instanceof CompactMap.CompactMapEntry : "Expected a CompactMapEntry when value is an Object[]";
//         assert map.get("id") == arrayValue;
//     }

//     @Test
//     void testEmptyMapNullKeyCreatesEntry() throws Exception {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().singleValueKey("id").compactSize(5).mapType(LinkedHashMap.class).build();
//         assert map.put(null, 99) == null;
//         Field f = CompactMap.class.getDeclaredField("val");
//         f.setAccessible(true);
//         Object internal = f.get(map);
//         assert internal instanceof CompactMap.CompactMapEntry : "Expected CompactMapEntry for null key";
//         assert map.get(null) == 99;
//     }

//     @Test
//     void testEmptyMapNullValueOptimized() throws Exception {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().singleValueKey("id").compactSize(5).mapType(LinkedHashMap.class).build();
//         assert map.put("id", null) == null;
//         Field f = CompactMap.class.getDeclaredField("val");
//         f.setAccessible(true);
//         Object internal = f.get(map);
//         assert internal == null : "When optimized with a null value, internal val should be null";
//         assert map.containsKey("id");
//         assert map.get("id") == null;
//     }

//     @Test
//     void testSingleEntryCompactMapEntryOverwrite() throws Exception {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(5).mapType(LinkedHashMap.class).build();
//         map.put("foo", 99);
//         assert map.get("foo") == 99;
//         assert map.put("foo", 55) == 99;
//         assert map.get("foo") == 55;
//         Field f = CompactMap.class.getDeclaredField("val");
//         f.setAccessible(true);
//         assert f.get(map) instanceof CompactMap.CompactMapEntry;
//     }

//     @Test
//     void testSingleEntryToCompactArrayConversion() throws Exception {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().compactSize(5).mapType(LinkedHashMap.class).build();
//         map.put("foo", 99);
//         assert map.put("bar", 77) == null;
//         Field f = CompactMap.class.getDeclaredField("val");
//         f.setAccessible(true);
//         Object internal = f.get(map);
//         assert internal instanceof Object[] : "Expected conversion to Object[]";
//         Object[] arr = (Object[]) internal;
//         assert "foo".equals(arr[0]);
//         assert Integer.valueOf(99).equals(arr[1]);
//         assert "bar".equals(arr[2]);
//         assert Integer.valueOf(77).equals(arr[3]);
//         assert map.get("foo") == 99;
//         assert map.get("bar") == 77;
//     }

//     @Test
//     void testCompactArrayTransitionToBackingMap() throws Exception {
//         CompactMap<Integer, Integer> map = CompactMap.<Integer, Integer>builder().compactSize(3).mapType(HashMap.class).build();
//         map.put(1, 10);
//         map.put(2, 20);
//         map.put(3, 30);
//         Field f = CompactMap.class.getDeclaredField("val");
//         f.setAccessible(true);
//         assert f.get(map) instanceof Object[] : "Should be in array mode before overflow";
//         assert map.put(4, 40) == null;
//         Object internal = f.get(map);
//         assert internal instanceof Map : "Expected internal storage to be a Map after overflow";
//         assert map.get(1) == 10;
//         assert map.get(2) == 20;
//         assert map.get(3) == 30;
//         assert map.get(4) == 40;
//     }

//     @Test
//     void testBackingMapOverwriteReturnsPrevious() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().mapType(HashMap.class).compactSize(2).build();
//         map.put("alpha", 123);
//         map.put("beta", 456);
//         map.put("gamma", 789);
//         assert map.size() > 2;
//         assert map.put("alpha", 999) == 123;
//         assert map.get("alpha") == 999;
//     }

//     @Test
//     void testCaseInsensitivePutReplacesIgnoringCase() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(false).build();
//         map.put("name", "Bob");
//         assert "Bob".equals(map.put("Name", "Alice"));
//         assert "Alice".equals(map.get("name"));
//         assert "Alice".equals(map.get("Name"));
//     }

//     @Test
//     void testTreeMapNullKeyThrowsNPE() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().mapType(TreeMap.class).build();
//         try {
//             map.put(null, "someValue");
//             throw new AssertionError("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

//     @Test
//     void testTreeMapNonComparableKeyThrowsCCE() {
//         CompactMap<Object, Integer> map = CompactMap.<Object, Integer>builder().mapType(TreeMap.class).build();
//         try {
//             map.put(new Object(), 123);
//             throw new AssertionError("Expected ClassCastException");
//         } catch (ClassCastException ignored) {
//         }
//     }

//     @Test
//     void testConcurrentHashMapNullKeyThrowsNPE() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().mapType(ConcurrentHashMap.class).build();
//         try {
//             map.put(null, 456);
//             throw new AssertionError("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

//     @Test
//     void testConcurrentHashMapNullValueThrowsNPE() {
//         CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().mapType(ConcurrentHashMap.class).build();
//         try {
//             map.put("validKey", null);
//             throw new AssertionError("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

//     @Test
//     void testCaseInsensitiveTreeMapNullKeyThrowsNPE() {
//         CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(false).mapType(TreeMap.class).build();
//         try {
//             map.put(null, "value");
//             throw new AssertionError("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }
}