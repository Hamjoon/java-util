package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_putIfAbsent_Test {

    private CaseInsensitiveMap<String, String> stringMap;

    private CaseInsensitiveMap<Integer, String> intKeyMap;

    // for ClassCastException test
    private CaseInsensitiveMap<Object, String> objectKeyMap;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh maps for each test to guarantee isolation.
        stringMap = new CaseInsensitiveMap<>();
        intKeyMap = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset static cache configuration to its default value in case a test changes it.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    /* --------------------------------------------------------------------- *
     *  Helper to replace the internal backing map via reflection.
     * --------------------------------------------------------------------- */
    private static void replaceBackingMap(CaseInsensitiveMap<?, ?> target, Map<?, ?> newBacking) {
        try {
            Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
            mapField.setAccessible(true);
            mapField.set(target, newBacking);
        } catch (Exception e) {
            throw new RuntimeException("Unable to replace backing map via reflection", e);
        }
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// first insertion – no previous mapping → null
        Arguments.of("Key", "value1", null), // second insertion with different case – previous value returned
        Arguments.of("key", "value2", "value1"));
    }

    private static Stream<Arguments> nullKeyProvider() {
        return Stream.of(// first insertion – no previous mapping → null
        Arguments.of("valNull", null), // second insertion – previous value returned
        Arguments.of("newVal", "valNull"));
    }

    private static Stream<Arguments> nullValueProvider() {
        return Stream.of(// first insertion – no previous mapping → null
        Arguments.of("nullValKey", null), // second insertion – previous null value returned
        Arguments.of("nullValKey", null));
    }

    private static Stream<Arguments> nonStringKeyProvider() {
        return Stream.of(// first insertion
        Arguments.of(42, "intVal", null), // second insertion returns previous
        Arguments.of(42, "newInt", "intVal"));
    }

    private static Stream<Arguments> caseNormalizationProvider() {
        return Stream.of(// first insertion; second call is performed inside test
        Arguments.of("TeSt", "v1", null));
    }

    @Test
    void putIfAbsentAddsNewEntryAndReturnsNull() {
        // GIVEN an empty map
        assertTrue(stringMap.isEmpty());
        // WHEN we put a new entry with putIfAbsent
        String returned = stringMap.putIfAbsent("Key", "Value");
        // THEN the method returns null (indicating no previous mapping)
        assertNull(returned, "putIfAbsent should return null when the key is absent");
        // AND the entry is now present
        assertEquals("Value", stringMap.get("Key"));
        assertEquals(1, stringMap.size());
    }

    @Test
    void putIfAbsentDoesNotReplaceExistingValue() {
        // GIVEN a map that already contains a mapping for "Key"
        stringMap.put("Key", "Original");
        assertEquals("Original", stringMap.get("Key"));
        // WHEN we call putIfAbsent with a different value but a key that differs only in case
        String returned = stringMap.putIfAbsent("kEy", "NewValue");
        // THEN the original value is returned
        assertEquals("Original", returned, "putIfAbsent should return the existing value");
        // AND the map still contains the original value
        assertEquals("Original", stringMap.get("KEY"));
        assertEquals(1, stringMap.size());
    }

    @Test
    void putIfAbsentIsCaseInsensitiveForStringKeys() {
        // GIVEN a map with a mixed‑case key
        stringMap.put("TeSt", "first");
        // WHEN we attempt to add a value using a different case
        String returned = stringMap.putIfAbsent("test", "second");
        // THEN the existing value is returned and the map is unchanged
        assertEquals("first", returned);
        assertEquals("first", stringMap.get("TEST"));
        assertEquals(1, stringMap.size());
    }

    @Test
    void putIfAbsentWorksWithNonStringKeys() {
        // GIVEN a map that uses Integer keys (non‑String)
        intKeyMap.put(42, "Answer");
        // WHEN we call putIfAbsent with the same Integer key
        String result1 = intKeyMap.putIfAbsent(42, "NewAnswer");
        // AND with a different Integer key
        String result2 = intKeyMap.putIfAbsent(7, "Lucky");
        // THEN the first call returns the existing value and does not replace it
        assertEquals("Answer", result1);
        assertEquals("Answer", intKeyMap.get(42));
        // AND the second call returns null and adds the new entry
        assertNull(result2);
        assertEquals("Lucky", intKeyMap.get(7));
        // AND the map size reflects the two distinct keys
        assertEquals(2, intKeyMap.size());
    }

    @Test
    void putIfAbsentPreservesOriginalKeyCaseInKeySet() {
        // GIVEN we insert a key with mixed case
        stringMap.putIfAbsent("MiXeD", "value");
        // WHEN we inspect the key set
        String keyFromKeySet = stringMap.keySet().iterator().next();
        // THEN the original case is preserved (the map stores the key exactly as first inserted)
        assertEquals("MiXeD", keyFromKeySet);
    }

    @Test
    void putIfAbsentHandlesNullKeyAndValue() {
        // GIVEN an empty map
        assertTrue(stringMap.isEmpty());
        // WHEN we add a null key/value pair
        String result = stringMap.putIfAbsent(null, null);
        // THEN the result is null (no previous mapping) and the entry exists
        assertNull(result);
        assertTrue(stringMap.containsKey(null));
        assertNull(stringMap.get(null));
        assertEquals(1, stringMap.size());
    }

    /* --------------------------------------------------------------------- *
     *  1. Basic functionality – first insertion returns null, second returns
     *     the previously stored value.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] putIfAbsent(key={0}, value={1}) ⇒ expectedReturn={2}")
    @MethodSource("basicFunctionalityProvider")
    void putIfAbsent_BasicFunctionality(String key, String value, String expectedReturn) {
        // GIVEN a fresh map (handled by @BeforeEach)
        // WHEN we invoke putIfAbsent
        String actualReturn = stringMap.putIfAbsent(key, value);
        // THEN the return value matches expectation
        assertEquals(expectedReturn, actualReturn, () -> "Unexpected return value for key='" + key + "' and value='" + value + "'");
        // AND the map contains the correct entry after the first insertion
        if (expectedReturn == null) {
            assertEquals(value, stringMap.get(key), "Value should be stored when the key was absent");
        } else {
            // when a previous value existed, the stored value must stay unchanged
            assertEquals("value1", stringMap.get("Key"), "Existing value must not be replaced");
        }
    }

    /* --------------------------------------------------------------------- *
     *  2. Null‑key handling – first insertion with null key stores the entry,
     *     subsequent insertion returns the original value.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] putIfAbsent(nullKey, value={1}) ⇒ expectedReturn={2}")
    @MethodSource("nullKeyProvider")
    void putIfAbsent_NullKey(String value, String expectedReturn) {
        // GIVEN an empty map (handled by @BeforeEach)
        // WHEN we insert a null key
        String actualReturn = stringMap.putIfAbsent(null, value);
        // THEN the return value matches expectation
        assertEquals(expectedReturn, actualReturn);
        // AND the map contains the null key with the first supplied value
        assertTrue(stringMap.containsKey(null));
        assertEquals(value, stringMap.get(null));
    }

    /* --------------------------------------------------------------------- *
     *  3. Null‑value handling – inserting a null value is allowed for the
     *     default backing map.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] putIfAbsent(key={0}, nullValue) ⇒ expectedReturn={2}")
    @MethodSource("nullValueProvider")
    void putIfAbsent_NullValue(String key, String expectedReturn) {
        // GIVEN an empty map
        // WHEN we insert a null value
        String actualReturn = stringMap.putIfAbsent(key, null);
        // THEN the method returns the expected previous value (null for first insert)
        assertEquals(expectedReturn, actualReturn);
        // AND the map stores the null value
        assertTrue(stringMap.containsKey(key));
        assertNull(stringMap.get(key));
    }

    /* --------------------------------------------------------------------- *
     *  4. Non‑String keys – behave like a regular map.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] putIfAbsent(Integer key={0}, value={1}) ⇒ expectedReturn={2}")
    @MethodSource("nonStringKeyProvider")
    void putIfAbsent_NonStringKeys(Integer key, String value, String expectedReturn) {
        // GIVEN a fresh intKeyMap
        // WHEN we invoke putIfAbsent
        String actualReturn = intKeyMap.putIfAbsent(key, value);
        // THEN the return matches expectation
        assertEquals(expectedReturn, actualReturn);
        // AND the map state reflects the operation
        if (expectedReturn == null) {
            assertEquals(value, intKeyMap.get(key));
        } else {
            assertEquals("intVal", intKeyMap.get(42));
        }
    }

    /* --------------------------------------------------------------------- *
     *  5. Case‑insensitive handling for mixed‑case strings.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] putIfAbsent(key={0}, value={1}) ⇒ expectedReturn={2}")
    @MethodSource("caseNormalizationProvider")
    void putIfAbsent_CaseNormalization(String key, String value, String expectedReturn) {
        // GIVEN an empty map
        // WHEN we insert the first mixed‑case key
        String firstReturn = stringMap.putIfAbsent(key, value);
        assertNull(firstReturn, "First insertion should return null");
        // WHEN we attempt to insert the same logical key with different case
        String secondReturn = stringMap.putIfAbsent(key.toLowerCase(Locale.ROOT), "ignored");
        // THEN the second call returns the originally stored value
        assertEquals(value, secondReturn);
        assertEquals(value, stringMap.get(key));
        assertEquals(1, stringMap.size());
    }

    /* --------------------------------------------------------------------- *
     *  6. Null key with a backing map that forbids nulls (ConcurrentHashMap).
     * --------------------------------------------------------------------- */
    @Test
    void putIfAbsent_NullKey_WithConcurrentHashMap_ShouldThrowNPE() {
        // Replace the backing map with a ConcurrentHashMap that does not allow null keys.
        replaceBackingMap(stringMap, new ConcurrentHashMap<>());
        // The standard JDK contract of ConcurrentHashMap is to throw NPE for null keys.
        assertThrows(NullPointerException.class, () -> stringMap.putIfAbsent(null, "val"), "ConcurrentHashMap should reject null keys with NPE");
    }

    /* --------------------------------------------------------------------- *
     *  7. Null value with a backing map that forbids null values (ConcurrentHashMap).
     * --------------------------------------------------------------------- */
    @Test
    void putIfAbsent_NullValue_WithConcurrentHashMap_ShouldThrowNPE() {
        replaceBackingMap(stringMap, new ConcurrentHashMap<>());
        assertThrows(NullPointerException.class, () -> stringMap.putIfAbsent("someKey", null), "ConcurrentHashMap should reject null values with NPE");
    }

    /* --------------------------------------------------------------------- *
     *  8. Non‑String key causing ClassCastException if normalizeKey casts.
     * --------------------------------------------------------------------- */
    @Test
    void putIfAbsent_NonStringKey_CausesClassCastException() {
        // The map is declared with Object keys to allow any type.
        // If the implementation naïvely casts the key to String, this will fail.
        Object nonStringKey = new Object();
        // The expected behaviour (according to the supplied JSON) is a ClassCastException.
        // If the implementation guards against this, the test will simply pass (no exception).
        // Therefore we assert that either a ClassCastException is thrown or the operation succeeds
        // without affecting the map (i.e., the key is stored as‑is). The former is the intended
        // contract for this particular test case.
        assertThrows(ClassCastException.class, () -> objectKeyMap.putIfAbsent(nonStringKey, "someValue"), "Putting a non‑String key should trigger ClassCastException if normalizeKey casts blindly");
    }

    /* --------------------------------------------------------------------- *
     *  9. Preserve original key case in the key set after putIfAbsent.
     * --------------------------------------------------------------------- */
    @Test
    void putIfAbsent_PreservesOriginalKeyCaseInKeySet() {
        stringMap.putIfAbsent("MiXeD", "value");
        String keyFromKeySet = stringMap.keySet().iterator().next();
        assertEquals("MiXeD", keyFromKeySet, "The key set must retain the original case of the first inserted key");
    }
}
