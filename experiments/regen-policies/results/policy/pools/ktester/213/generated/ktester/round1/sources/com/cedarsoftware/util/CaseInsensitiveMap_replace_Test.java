package com.cedarsoftware.util;

import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class CaseInsensitiveMap_replace_Test {

    private CaseInsensitiveMap<String, String> ciMap;

    private CaseInsensitiveMap<Integer, String> intMap;

    /* ----------------------------------------------------------------------
     * Helper class to mimic the JUnit‑5 Arguments container
     * ---------------------------------------------------------------------- */
    private static final class Arguments extends ArrayList<Object> {

        private Arguments(Object... values) {
            super(Arrays.asList(values));
        }

        static Arguments of(Object... values) {
            return new Arguments(values);
        }

//         Object get(int index) {
//             return super.get(index);
//         }
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Map for String keys – case‑insensitive behaviour is exercised here.
        ciMap = new CaseInsensitiveMap<>();
        ciMap.put("Alpha", "first");
        ciMap.put("Beta", "second");
        // null value entry
        ciMap.put("Gamma", null);
        // null key entry for some tests
        ciMap.put(null, "oldNullKey");
        // Map for non‑String keys – should behave like a normal map.
        intMap = new CaseInsensitiveMap<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
    }

//     @AfterEach
    void teardownAfterEach() {
        ciMap = null;
        intMap = null;
    }

    static Stream<Arguments> basicBehaviorProvider() {
        return // key exists (case‑insensitively) and oldValue matches
        Stream.// key exists (case‑insensitively) and oldValue matches
        of(// key exists but oldValue does not match
        Arguments.of("Key exists and old value matches", "key", "value1", "value2", true), // key absent
        Arguments.of("Key exists but old value differs", "KEY", "different", "value2", false), // null key with non‑null old value
        Arguments.of("Missing key", "missing", "any", "new", false), // key exists, current value null, oldValue null
        Arguments.of("Null key with matching old value", null, "oldNullKey", "newNullKey", true), // key exists, oldValue matches, newValue null
        Arguments.of("Key with null current value, oldValue null", "test", null, "newValue", true), Arguments.of("Replace with null newValue", "test2", "oldValue", null, true));
    }

    static Stream<Arguments> caseInsensitivityEdgeProvider() {
        return Stream.of(Arguments.of("Mixed case key should succeed", "Key", "value1", "value3", true), Arguments.of("Another mixed case key should succeed", "kEy", "value1", "value4", true));
    }

    static Stream<Arguments> nullHandlingProvider() {
        return // null key, null old, non‑null new
        Stream.// null key, null old, non‑null new
        of(// key with null value, old null, new null (no change)
        Arguments.of("null key, null old, new value", null, null, "newValueForNullKey", true), // key with null value, old does not match, new null
        Arguments.of("key with null value, old null, new null", "nullKey", null, null, true), Arguments.of("key with null value, mismatched old", "nullKey", "someValue", null, false));
    }

    static Stream<Arguments> nonStringKeyProvider() {
        return // integer key present
        Stream.// integer key present
        of(// boolean key absent
        Arguments.of("Integer key present", 123, "intValue", "newIntValue", true), Arguments.of("Boolean key absent", true, "boolOld", "boolNew", false));
    }

    static Stream<Arguments> backingMapProvider() {
        // ConcurrentHashMap backing
        Map<String, String> concurrent = new ConcurrentHashMap<>();
        // TreeMap backing (ordered)
        Map<String, String> tree = new TreeMap<>();
        return Stream.of(Arguments.of("ConcurrentHashMap backing – replace succeeds", concurrent, "concurrent", "cVal", "cNew", true), Arguments.of("TreeMap backing – replace succeeds", tree, "tree", "tVal", "tNew", true));
    }

//     @Test
//     void replace_SucceedsWhenOldValueMatches_IgnoresKeyCase() {
//         boolean replaced = ciMap.replace("aLpHa", "first", "uno");
//         Assert.assertTrue("replace should return true when old value matches", replaced);
//         Assert.assertEquals("uno", ciMap.get("ALPHA"));
//     }

//     @Test
//     void replace_FailsWhenOldValueDoesNotMatch_IgnoresKeyCase() {
//         boolean replaced = ciMap.replace("BeTa", "WRONG", "new");
//         Assert.assertFalse("replace should return false when old value does not match", replaced);
//         Assert.assertEquals("second", ciMap.get("beta"));
//     }

//     @Test
//     void replace_SucceedsWhenOldValueIsNull() {
//         Assert.assertNull(ciMap.get("gamma"));
//         boolean replaced = ciMap.replace("GAMMA", null, "third");
//         Assert.assertTrue("replace should succeed when the stored value is null and oldValue is null", replaced);
//         Assert.assertEquals("third", ciMap.get("gamma"));
//     }

//     @Test
//     void replace_ReturnsFalseWhenKeyIsAbsent() {
//         boolean replaced = ciMap.replace("Delta", "any", "new");
//         Assert.assertFalse("replace should return false for a missing key", replaced);
//         Assert.assertNull(ciMap.get("Delta"));
//     }

//     @Test
//     void replace_WithNonStringKeyBehavesLikeStandardMap() {
//         // Successful replace
//         boolean first = intMap.replace(1, "one", "uno");
//         Assert.assertTrue(first);
//         Assert.assertEquals("uno", intMap.get(1));
//         // Unsuccessful replace (wrong old value)
//         boolean second = intMap.replace(2, "wrong", "dos");
//         Assert.assertFalse(second);
//         Assert.assertEquals("two", intMap.get(2));
//         // Missing key
//         boolean missing = intMap.replace(3, "any", "tres");
//         Assert.assertFalse(missing);
//         Assert.assertNull(intMap.get(3));
//     }

//     @ParameterizedTest(name = "{index}: {0}")
//     @MethodSource("basicBehaviorProvider")
//     void replace_BasicBehavior(String description, Object key, String oldValue, String newValue, boolean expected) {
//         Stream<Arguments> data = Stream.of(// description, key, oldValue, newValue, expectedResult
//         Arguments.of("Key exists and old value matches", "key", "value1", "value2", true), Arguments.of("Key exists but old value does not match", "KEY", "different", "value2", false), Arguments.of("Missing key", "missing", "any", "new", false), Arguments.of("Null key with matching old value", null, "oldNullKey", "newNullKey", true), Arguments.of("Key with null current value, oldValue null", "test", null, "newValue", true), Arguments.of("Replace with null newValue", "test2", "oldValue", null, true));
//         data.forEach(arg -> {
//             String description = (String) arg.get(0);
//             Object key = arg.get(1);
//             String oldVal = (String) arg.get(2);
//             String newVal = (String) arg.get(3);
//             boolean expected = (Boolean) arg.get(4);
//             @SuppressWarnings("unchecked")
//             CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
//             if (key != null) {
//                 map.put(key, oldVal);
//             } else {
//                 map.put(null, "oldNullKey");
//             }
//             boolean result = map.replace(key, oldVal, newVal);
//             Assert.assertEquals(description, expected, result);
//             if (expected) {
//                 Assert.assertEquals(newVal, map.get(key));
//             } else {
//                 Assert.assertEquals(oldVal, map.get(key));
//             }
//         });
//     }

//     @ParameterizedTest(name = "{index}: {0}")
//     @MethodSource("caseInsensitivityEdgeProvider")
//     void replace_CaseInsensitivityEdge(String description, String key, String oldValue, String newValue, boolean expected) {
//         Stream<Arguments> data = Stream.of(Arguments.of("Mixed case key should succeed", "Key", "value1", "value3", true), Arguments.of("Another mixed case key should succeed", "kEy", "value1", "value4", true));
//         data.forEach(arg -> {
//             String description = (String) arg.get(0);
//             String key = (String) arg.get(1);
//             String oldVal = (String) arg.get(2);
//             String newVal = (String) arg.get(3);
//             boolean expected = (Boolean) arg.get(4);
//             CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//             map.put("key", "value1");
//             boolean result = map.replace(key, oldVal, newVal);
//             Assert.assertEquals(description, expected, result);
//             if (expected) {
//                 Assert.assertEquals(newVal, map.get("KEY"));
//             }
//         });
//     }

//     @ParameterizedTest(name = "{index}: {0}")
//     @MethodSource("nullHandlingProvider")
//     void replace_NullHandlingVariations(String description, Object key, String oldValue, String newValue, boolean expected) {
//         Stream<Arguments> data = Stream.of(Arguments.of("null key, null old, new value", null, null, "newValueForNullKey", true), Arguments.of("key with null value, old null, new null", "nullKey", null, null, true), Arguments.of("key with null value, mismatched old", "nullKey", "someValue", null, false));
//         data.forEach(arg -> {
//             String description = (String) arg.get(0);
//             Object key = arg.get(1);
//             String oldVal = (String) arg.get(2);
//             String newVal = (String) arg.get(3);
//             boolean expected = (Boolean) arg.get(4);
//             @SuppressWarnings("unchecked")
//             CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
//             if (key == null) {
//                 map.put(null, null);
//             } else {
//                 map.put(key, null);
//             }
//             boolean result = map.replace(key, oldVal, newVal);
//             Assert.assertEquals(description, expected, result);
//             if (expected) {
//                 Assert.assertEquals(newVal, map.get(key));
//             } else {
//                 Assert.assertNull(map.get(key));
//             }
//         });
//     }

//     @ParameterizedTest(name = "{index}: {0}")
//     @MethodSource("nonStringKeyProvider")
//     void replace_NonStringKeys(String description, Object key, String oldValue, String newValue, boolean expected) {
//         Stream<Arguments> data = Stream.of(Arguments.of("Integer key present", 123, "intValue", "newIntValue", true), Arguments.of("Boolean key absent", true, "boolOld", "boolNew", false));
//         data.forEach(arg -> {
//             String description = (String) arg.get(0);
//             Object key = arg.get(1);
//             String oldVal = (String) arg.get(2);
//             String newVal = (String) arg.get(3);
//             boolean expected = (Boolean) arg.get(4);
//             @SuppressWarnings("unchecked")
//             CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
//             map.put(123, "intValue");
//             boolean result = map.replace(key, oldVal, newVal);
//             Assert.assertEquals(description, expected, result);
//             if (expected) {
//                 Assert.assertEquals(newVal, map.get(key));
//             } else {
//                 Assert.assertEquals("intValue", map.get(123));
//             }
//         });
//     }

//     @ParameterizedTest(name = "{index}: {0}")
//     @MethodSource("backingMapProvider")
//     void replace_WithDifferentBackingMaps(String description, Map<String, String> backing, String key, String oldValue, String newValue, boolean expected) throws Exception {
//         Map<String, String> concurrent = new ConcurrentHashMap<>();
//         Map<String, String> tree = new TreeMap<>();
//         Stream<Arguments> data = Stream.of(Arguments.of("ConcurrentHashMap backing – replace succeeds", concurrent, "concurrent", "cVal", "cNew", true), Arguments.of("TreeMap backing – replace succeeds", tree, "tree", "tVal", "tNew", true));
//         data.forEach(arg -> {
//             try {
//                 String description = (String) arg.get(0);
//                 @SuppressWarnings("unchecked")
//                 Map<String, String> backing = (Map<String, String>) arg.get(1);
//                 String key = (String) arg.get(2);
//                 String oldVal = (String) arg.get(3);
//                 String newVal = (String) arg.get(4);
//                 boolean expected = (Boolean) arg.get(5);
//                 CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//                 Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
//                 mapField.setAccessible(true);
//                 mapField.set(map, backing);
//                 map.put(key, oldVal);
//                 boolean result = map.replace(key, oldVal, newVal);
//                 Assert.assertEquals(description, expected, result);
//                 if (expected) {
//                     Assert.assertEquals(newVal, map.get(key));
//                 } else {
//                     Assert.assertEquals(oldVal, map.get(key));
//                 }
//             } catch (Exception e) {
//                 throw new RuntimeException(e);
//             }
//         });
//     }

//     @Test
//     void replace_KeyNotPresentAfterCaseTransformation() {
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         map.put("related", "value");
//         boolean result = map.replace("UNRELATED", "any", "new");
//         Assert.assertFalse(result);
//         Assert.assertNull(map.get("UNRELATED"));
//     }

//     @Test
//     void replace_DelegatesToUnderlyingMapWithConvertedKey() throws Exception {
//         @SuppressWarnings("unchecked")
//         Map<String, String> mockMap = Mockito.mock(Map.class);
//         Mockito.when(mockMap.replace(Mockito.eq("key"), Mockito.eq("old"), Mockito.eq("new"))).thenReturn(true);
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
//         mapField.setAccessible(true);
//         mapField.set(map, mockMap);
//         boolean result = map.replace("KeY", "old", "new");
//         Assert.assertTrue(result);
//         Mockito.verify(mockMap, Mockito.times(1)).replace("key", "old", "new");
//     }
}