package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_containsKey_Test {

    /**
     * The map instance used by most tests.
     */
    private CaseInsensitiveMap<String, String> stringMap;

    /**
     * A map that uses a non‑String key type.
     */
    private CaseInsensitiveMap<Integer, String> intKeyMap;

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure the global cache is in a known state for all tests.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test so that state does not leak.
        stringMap = new CaseInsensitiveMap<>();
        stringMap.put("Key", "Value");
        stringMap.put("One", "Two");
        stringMap.put("Three", "Four");
        // empty‑string key
        stringMap.put("", "empty");
        // single‑space key
        stringMap.put(" ", "space");
        intKeyMap = new CaseInsensitiveMap<>();
        intKeyMap.put(1, "One");
        intKeyMap.put(2, "Two");
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset any global modifications made by a single test.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    @AfterAll
    public static void teardownAfterAll() {
        // Reset any global modifications made by the tests.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    /**
     * Provides basic functionality test cases.
     */
//     private static Stream<Arguments> basicFunctionalityProvider() {
//         return Stream.of(Arguments.of("Key", true), Arguments.of("kEy", true), Arguments.of("missing", false), Arguments.of(null, false));
//     }

    /**
     * Provides test cases where a null key is present in the underlying map.
     */
//     private static Stream<Arguments> nullKeyPresentProvider() {
//         return Stream.of(Arguments.of(null, true));
//     }

    /**
     * Provides test cases for non‑String keys.
     */
//     private static Stream<Arguments> nonStringKeyProvider() {
//         return Stream.of(Arguments.of(123, true), Arguments.of(124, false));
//     }

//     @Test
//     void containsKey_isCaseInsensitiveForStringKeys() {
//         assertTrue(stringMap.containsKey("One"));
//         assertTrue(stringMap.containsKey("one"));
//         assertTrue(stringMap.containsKey("ONE"));
//         assertTrue(stringMap.containsKey("OnE"));
//         assertTrue(stringMap.containsKey("oNe"));
//         assertTrue(stringMap.containsKey("oNE"));
//         assertTrue(stringMap.containsKey("ONe"));
//         // Keys that were never added should return false.
//         assertFalse(stringMap.containsKey("Two"));
//         assertFalse(stringMap.containsKey("four"));
//     }

//     @Test
//     void containsKey_handlesNonStringKeysNormally() {
//         assertTrue(intKeyMap.containsKey(1));
//         assertTrue(intKeyMap.containsKey(2));
//         // Different numeric type – autoboxing does not convert, so should be false.
//         assertFalse(intKeyMap.containsKey(1L));
//         assertFalse(intKeyMap.containsKey(3));
//     }

//     @Test
//     void containsKey_acceptsNullKeyWhenPresent() {
//         CaseInsensitiveMap<String, String> mapWithNull = new CaseInsensitiveMap<>();
//         mapWithNull.put(null, "nullValue");
//         assertTrue(mapWithNull.containsKey(null));
//         // original map has no null key
//         assertFalse(stringMap.containsKey(null));
//     }

//     @Test
//     void containsKey_behaviourWithDifferentBackingMap() {
//         // Source map is a TreeMap – the constructor should preserve case‑insensitivity.
//         Map<String, String> source = new TreeMap<>();
//         source.put("Alpha", "A");
//         source.put("Beta", "B");
//         CaseInsensitiveMap<String, String> mapFromTree = new CaseInsensitiveMap<>(source);
//         assertTrue(mapFromTree.containsKey("alpha"));
//         assertTrue(mapFromTree.containsKey("ALPHA"));
//         assertTrue(mapFromTree.containsKey("Beta"));
//         assertTrue(mapFromTree.containsKey("bEtA"));
//         // Verify that a key not present returns false.
//         assertFalse(mapFromTree.containsKey("Gamma"));
//     }

//     @Test
//     void containsKey_isConsistentWithGet() {
//         // The contract of containsKey should match get() returning non‑null or null with a present mapping.
//         assertEquals("Two", stringMap.get("ONE"));
//         assertTrue(stringMap.containsKey("ONE"));
//         // A missing key returns null from get() and false from containsKey().
//         assertNull(stringMap.get("missing"));
//         assertFalse(stringMap.containsKey("missing"));
//     }

//     @Test
//     void containsKey_doesNotAffectOriginalKeyCaseInKeySet() {
//         // Ensure that after a successful containsKey the original case is still retained in keySet().
//         Set<String> keys = stringMap.keySet();
//         // original case
//         assertTrue(keys.contains("One"));
//         // keySet() returns original case only
//         assertFalse(keys.contains("one"));
//     }

//     @ParameterizedTest(name = "containsKey basic – key=''{0}'' → expected={1}")
//     @MethodSource("basicFunctionalityProvider")
//     void containsKey_basicFunctionality(Object key, boolean expected) {
//         Object[][] data = { { "Key", true }, { "kEy", true }, { "missing", false }, { null, false } };
//         for (Object[] row : data) {
//             Object key = row[0];
//             boolean expected = (boolean) row[1];
//             if (expected) {
//                 assertTrue(stringMap.containsKey(key), () -> "Expected key '" + key + "' to be present");
//             } else {
//                 assertFalse(stringMap.containsKey(key), () -> "Expected key '" + key + "' to be absent");
//             }
//         }
//     }

//     @ParameterizedTest(name = "containsKey – null key present → expected={1}")
//     @MethodSource("nullKeyPresentProvider")
//     void containsKey_nullKeyPresent(Object key, boolean expected) {
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         map.put(null, "nullValue");
//         assertTrue(map.containsKey(null));
//     }

//     @ParameterizedTest(name = "containsKey non‑String – key={0} → expected={1}")
//     @MethodSource("nonStringKeyProvider")
//     void containsKey_nonStringKeys(Object key, boolean expected) {
//         CaseInsensitiveMap<Integer, String> map = new CaseInsensitiveMap<>();
//         map.put(123, "oneHundredTwentyThree");
//         Object[][] data = { { 123, true }, { 124, false } };
//         for (Object[] row : data) {
//             Object key = row[0];
//             boolean expected = (boolean) row[1];
//             if (expected) {
//                 assertTrue(map.containsKey(key));
//             } else {
//                 assertFalse(map.containsKey(key));
//             }
//         }
//     }

    /**
     * CharSequence (StringBuilder) key test.
     */
//     @Test
//     void containsKey_charSequenceKey() {
//         // The map already contains the String "Key".
//         StringBuilder sb = new StringBuilder("Key");
//         assertTrue(stringMap.containsKey(sb));
//     }

    /**
     * Empty string and single‑space keys.
     */
//     @Test
//     void containsKey_emptyAndSpaceKeys() {
//         // Empty string key is present.
//         assertTrue(stringMap.containsKey(""));
//         // Single space key is present.
//         assertTrue(stringMap.containsKey(" "));
//         // A different whitespace string should not be found.
//         assertFalse(stringMap.containsKey("\t"));
//     }

    /**
     * Verify that case‑insensitivity works when the backing map is a {@link ConcurrentHashMap}.
     */
//     @Test
//     void containsKey_differentBackingMap_ConcurrentHashMap() {
//         Map<String, String> source = new ConcurrentHashMap<>();
//         source.put("Concurrent", "Value");
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(source);
//         assertTrue(map.containsKey("concurrent"));
//         assertTrue(map.containsKey("CONCURRENT"));
//     }

    /**
     * Expect a {@link NullPointerException} when the backing map (ConcurrentHashMap) does not accept null keys.
     */
//     @Test
//     void containsKey_exception_nullKeyOnConcurrentHashMap() {
//         Map<String, String> source = new ConcurrentHashMap<>();
//         source.put("A", "a");
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(source);
//         assertThrows(NullPointerException.class, () -> map.containsKey(null));
//     }

    /**
     * Expect a {@link ClassCastException} when a non‑String key is supplied to a TreeMap‑backed map.
     */
//     @Test
//     void containsKey_exception_nonStringKeyOnTreeMap() {
//         Map<String, String> source = new TreeMap<>();
//         source.put("Key", "Value");
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(source);
//         assertThrows(ClassCastException.class, () -> map.containsKey(123));
//     }

    /**
     * Expect a {@link ClassCastException} when the generic type is {@code String}
     * but a non‑String key is passed (conversion happens before delegating to the backing map).
     */
//     @Test
//     void containsKey_exception_keyTypeMismatchWithGenericString() {
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         map.put("Key", "Value");
//         assertThrows(ClassCastException.class, () -> map.containsKey(123));
//     }
}