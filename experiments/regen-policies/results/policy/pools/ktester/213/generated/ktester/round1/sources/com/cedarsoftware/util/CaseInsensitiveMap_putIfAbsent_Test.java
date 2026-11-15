package com.cedarsoftware.util;

// import org.junit.*;
// import org.junit.runner.RunWith;
// import org.junit.*;
// import org.junit.runners.Parameterized;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_putIfAbsent_Test {

    private CaseInsensitiveMap<String, String> stringMap;

    private CaseInsensitiveMap<Integer, String> intKeyMap;

    //     @RunWith(Parameterized.class)
    public static class BasicScenarios {

        private final String key;

        private final String value;

        private final Object expected;

        private CaseInsensitiveMap<String, String> stringMap;

        public BasicScenarios(String key, String value, Object expected) {
            this.key = key;
            this.value = value;
            this.expected = expected;
        }
        //         @Before
        //         public void init() {
        //             stringMap = new CaseInsensitiveMap<>();
        //             // Pre‑populate according to the scenario description
        //             if ("beta".equals(key)) {
        //                 stringMap.put("beta", "originalValue");
        //             } else if ("CaSe".equals(key)) {
        //                 stringMap.put("case", "VALUE");
        //             } else if ("delta".equals(key) && "second".equals(value)) {
        //                 stringMap.put("delta", "first");
        //             }
        //         }
        //         @Parameters(name = "[{index}] key=''{0}'', value=''{1}'' → expected={2}")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][]{
        //                     {"alpha", "first", null},
        //                     {"beta", "newValue", "originalValue"},
        //                     {"CaSe", "ignored", "VALUE"},
        //                     {null, "any", NullPointerException.class},
        //                     {"gamma", null, null},
        //                     {"delta", "first", null},
        //                     {"delta", "second", "first"}
        //             });
        //         }
        //         @Test
        //         public void putIfAbsent_BasicScenario() {
        //             if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
        //                 try {
        //                     stringMap.putIfAbsent(key, value);
        //                     Assert.fail("Expected exception: " + ((Class<?>) expected).getSimpleName());
        //                 } catch (Throwable t) {
        //                     Assert.assertTrue(((Class<?>) expected).isInstance(t));
        //                 }
        //             } else {
        //                 String returned = stringMap.putIfAbsent(key, value);
        //                 Assert.assertEquals(expected, returned);
        //                 if (expected == null) {
        //                     Assert.assertEquals(value, stringMap.get(key));
        //                 } else {
        //                     Assert.assertEquals(expected, stringMap.get(key));
        //                 }
        //             }
        //         }
    }

    /* --------------------------------------------------------------------- */
//     @BeforeAll
    static void setupBeforeAll() {
        // Ensure the global cache is in its default state for the whole suite.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Fresh maps for each test to avoid cross‑test contamination.
        stringMap = new CaseInsensitiveMap<>();
        intKeyMap = new CaseInsensitiveMap<>();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Reset global configuration after each test (mirrors the original test suite).
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    /* --------------------------------------------------------------------- */
//     private static Stream<Arguments> basicScenariosProvider() {
//         return Stream.of(// absent key
//         Arguments.of("alpha", "first", null), // existing key (case‑insensitive)
//         Arguments.of("beta", "newValue", "originalValue"), // case‑insensitive match
//         Arguments.of("CaSe", "ignored", "VALUE"), // null key
//         Arguments.of(null, "any", NullPointerException.class), // null value on absent key
//         Arguments.of("gamma", null, null), // first insertion of "delta"
//         Arguments.of("delta", "first", null), // second insertion, original value returned
//         Arguments.of("delta", "second", "first"));
//     }

//     @ParameterizedTest(name = "[{index}] key=''{0}'', value=''{1}'' → expected={2}")
//     @MethodSource("basicScenariosProvider")
//     void putIfAbsent_BasicScenarios(String key, String value, Object expected) {
//         // Each entry: {key, value, expectedResult}
//         List<Object[]> scenarios = Arrays.asList(new Object[][] { { "alpha", "first", null }, { "beta", "newValue", "originalValue" }, { "CaSe", "ignored", "VALUE" }, { null, "any", NullPointerException.class }, { "gamma", null, null }, { "delta", "first", null }, { "delta", "second", "first" } });
//         for (Object[] s : scenarios) {
//             String key = (String) s[0];
//             String value = (String) s[1];
//             Object expected = s[2];
//             // Fresh map for each scenario
//             CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//             // Pre‑populate map for the scenarios that require it.
//             if ("beta".equals(key)) {
//                 map.put("beta", "originalValue");
//             } else if ("CaSe".equals(key)) {
//                 map.put("case", "VALUE");
//             } else if ("delta".equals(key) && "second".equals(value)) {
//                 map.put("delta", "first");
//             }
//             if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
//                 // Expected exception case
//                 try {
//                     map.putIfAbsent(key, value);
//                     fail("Expected exception: " + ((Class<?>) expected).getSimpleName() + " for key=" + key);
//                 } catch (Throwable t) {
//                     assertTrue("Unexpected exception type", ((Class<?>) expected).isInstance(t));
//                 }
//             } else {
//                 // Normal execution case
//                 String returned = map.putIfAbsent(key, value);
//                 assertEquals("Unexpected return value for key=" + key + ", value=" + value, expected, returned);
//                 if (expected == null) {
//                     // Insertion should have happened
//                     assertEquals("Value should be stored for newly inserted key", value, map.get(key));
//                 } else {
//                     // No insertion – original value must stay unchanged
//                     assertEquals("Original value must remain unchanged for key=" + key, expected, map.get(key));
//                 }
//             }
//         }
//     }

//     @AfterClass
//     public static void teardownAfterAll() {
//         // Reset global configuration after the suite.
//         CaseInsensitiveMap.setMaxCacheLengthString(100);
//         CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
//     }

    /* --------------------------------------------------------------------- */
//     @Test
//     public void putIfAbsent_InsertsWhenKeyAbsent() {
//         assertTrue(stringMap.isEmpty());
//         String previous = stringMap.putIfAbsent("Key", "Value");
//         assertNull(previous);
//         assertEquals("Value", stringMap.get("Key"));
//         assertEquals(1, stringMap.size());
//     }

//     @Test
//     public void putIfAbsent_DoesNotReplaceExistingValue() {
//         stringMap.put("Existing", "First");
//         assertEquals("First", stringMap.get("Existing"));
//         String previous = stringMap.putIfAbsent("Existing", "Second");
//         assertEquals("First", previous);
//         assertEquals("First", stringMap.get("Existing"));
//         assertEquals(1, stringMap.size());
//     }

//     @Test
//     public void putIfAbsent_IsCaseInsensitiveForStringKeys() {
//         stringMap.put("MiXeD", "Original");
//         String previous = stringMap.putIfAbsent("mixed", "NewValue");
//         assertEquals("Original", previous);
//         assertEquals("Original", stringMap.get("MIXED"));
//         assertEquals(1, stringMap.size());
//     }

//     @Test
//     public void putIfAbsent_AllowsNonStringKeysUnmodified() {
//         intKeyMap.put(10, "Ten");
//         intKeyMap.put(20, "Twenty");
//         String previous = intKeyMap.putIfAbsent(10, "NewTen");
//         assertEquals("Ten", previous);
//         assertEquals("Ten", intKeyMap.get(10));
//         assertEquals(2, intKeyMap.size());
//         String absent = intKeyMap.putIfAbsent(30, "Thirty");
//         assertNull(absent);
//         assertEquals("Thirty", intKeyMap.get(30));
//         assertEquals(3, intKeyMap.size());
//     }

//     @Test
//     public void putIfAbsent_NullKeyAndValueHandling() {
//         assertTrue(stringMap.isEmpty());
//         String previousNullKey = stringMap.putIfAbsent(null, "NullKey");
//         assertNull(previousNullKey);
//         assertEquals("NullKey", stringMap.get(null));
//         String previousAgain = stringMap.putIfAbsent(null, "Another");
//         assertEquals("NullKey", previousAgain);
//         assertEquals("NullKey", stringMap.get(null));
//         assertEquals(1, stringMap.size());
//     }

//     @Test
//     public void putIfAbsent_DigitsAndWhitespaceHandling() {
//         assertNull(stringMap.putIfAbsent("numeric123", "numVal"));
//         assertEquals("numVal", stringMap.putIfAbsent("NUMERIC123", "other"));
//         assertNull(stringMap.putIfAbsent("   spaced   ", "spaceVal"));
//         assertEquals("spaceVal", stringMap.putIfAbsent("   SPACED   ", "new"));
//     }

//     @Test
//     public void putIfAbsent_EmptyStringKey() {
//         assertNull(stringMap.putIfAbsent("", "emptyKey"));
//         assertEquals("emptyKey", stringMap.get(""));
//         assertEquals("emptyKey", stringMap.putIfAbsent("", "another"));
//     }

//     @Test
//     @SuppressWarnings("unchecked")
//     public void putIfAbsent_NonStringKeyCausesClassCastException() {
//         CaseInsensitiveMap rawMap = new CaseInsensitiveMap<>();
//         try {
//             rawMap.putIfAbsent(123, "someValue");
//             fail("Expected ClassCastException");
//         } catch (ClassCastException e) {
//             // expected
//         }
//     }

//     @Test
//     public void putIfAbsent_NullValueWithConcurrentHashMap() {
//         CaseInsensitiveMap<String, String> concurrentMap = new CaseInsensitiveMap<>(new java.util.concurrent.ConcurrentHashMap<>());
//         try {
//             concurrentMap.putIfAbsent("nullValueKey", null);
//             fail("Expected NullPointerException");
//         } catch (NullPointerException e) {
//             // expected
//         }
//     }

//     @Test
//     public void putIfAbsent_UnmodifiableBackingMapThrows() {
//         Map<String, String> base = new java.util.HashMap<>();
//         Map<String, String> unmodifiable = java.util.Collections.unmodifiableMap(base);
//         CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(unmodifiable);
//         try {
//             ciMap.putIfAbsent("immutableKey", "someValue");
//             fail("Expected UnsupportedOperationException");
//         } catch (UnsupportedOperationException e) {
//             // expected
//         }
//     }

//     @Test
//     public void putIfAbsent_AllowsNullValueWhenSupported() {
//         assertNull(stringMap.putIfAbsent("nullValueAllowed", null));
//         assertTrue(stringMap.containsKey("nullValueAllowed"));
//         assertNull(stringMap.get("nullValueAllowed"));
//     }
}