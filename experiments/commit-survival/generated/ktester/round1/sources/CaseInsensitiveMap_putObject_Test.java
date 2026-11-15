package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

class CaseInsensitiveMap_putObject_Test {

    /**
     * The map instance used in each test.
     */
    private CaseInsensitiveMap<Object, Object> map;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor – it creates an internal LinkedHashMap.
        map = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the GC; not strictly required but keeps the pattern consistent.
        map = null;
    }

    /**
     * Uses reflection to replace the private {@code map} field of a {@link CaseInsensitiveMap}
     * with a custom backing map (or {@code null}).
     *
     * @param target the {@code CaseInsensitiveMap} instance to modify
     * @param backing the map to set as the internal delegate (may be {@code null})
     */
    private static void injectBackingMap(CaseInsensitiveMap<?, ?> target, Map<?, ?> backing) {
        try {
            Field f = CaseInsensitiveMap.class.getDeclaredField("map");
            f.setAccessible(true);
            f.set(target, backing);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject backing map via reflection", e);
        }
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> keyTypeHandlingProvider() {
        return Stream.of(// String key – first insertion
        Arguments.of("TeSt", "value1", null, "value1"), // String key – overwrite, expect previous value
        Arguments.of("TeSt", "value2", "value1", "value2"), // Non‑String numeric key
        Arguments.of(12345, "numericKey", null, "numericKey"), // Null key
        Arguments.of(null, "nullKey", null, "nullKey"), // Empty string key
        Arguments.of("", "emptyString", null, "emptyString"));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> nullValueHandlingProvider() {
        return Stream.of(Arguments.of("NullValueKey", null, null), Arguments.of(456, null, null));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> nonStringCharSequenceProvider() {
        return Stream.of(// first insertion
        Arguments.of(new StringBuilder("CharSeq"), "csValue", null, "csValue"), // overwrite
        Arguments.of(new StringBuilder("CharSeq"), "newCsValue", "csValue", "newCsValue"));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> whitespaceKeyProvider() {
        return Stream.of(Arguments.of("  SpAcE  ", "spaceVal", null, "spaceVal"), Arguments.of("  SPACE  ", "newSpaceVal", "spaceVal", "newSpaceVal"));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> duplicateCaseInsensitiveProvider() {
        return Stream.of(Arguments.of("Direct", "directVal", null, "directVal"), Arguments.of("direct", "newDirectVal", "directVal", "newDirectVal"));
    }

    // -----------------------------------------------------------------
    private static Stream<Arguments> replacementNonStringKeyProvider() {
        return Stream.of(// First insertion of a String key (sets baseline)
        Arguments.of("KeyObj", "first", null, "first", true), // Insert a different non‑String key
        Arguments.of(789, "second", null, "second", false), // Replace the Integer key
        Arguments.of(789, "updated", "second", "updated", false));
    }

    @Test
    void putObject_withStringKey_isCaseInsensitive() {
        // GIVEN
        String originalKey = "TeStKey";
        String value = "value1";
        // WHEN
        Object previous = map.putObject(originalKey, value);
        // THEN
        assertNull(previous, "First insertion should return null");
        // Retrieval with a different case must succeed.
        assertEquals(value, map.get("testkey"));
        assertEquals(value, map.get("TESTKEY"));
        // The key stored in the map (as exposed by keySet) retains original case.
        assertTrue(map.keySet().contains(originalKey));
        // The internal map size must be 1.
        assertEquals(1, map.size());
    }

    @Test
    void putObject_withStringKey_overwritesExistingEntry_andReturnsPrevious() {
        // GIVEN
        map.putObject("Key", "first");
        // WHEN
        // different case, same logical key
        Object previous = map.putObject("key", "second");
        // THEN
        assertEquals("first", previous, "Should return the previous value for the logical key");
        assertEquals("second", map.get("KEY"));
        assertEquals(1, map.size(), "Map should still contain only one entry");
    }

    @Test
    void putObject_withNonStringKey_isStoredAsIs() {
        // GIVEN
        Integer intKey = 42;
        String value = "answer";
        // WHEN
        Object previous = map.putObject(intKey, value);
        // THEN
        assertNull(previous);
        assertEquals(value, map.get(42));
        // Different object with same numeric value should not be considered equal.
        assertNull(map.get(42L));
        assertTrue(map.keySet().contains(intKey));
        assertEquals(1, map.size());
    }

    @Test
    void putObject_nullKey_isHandledByUnderlyingMap() {
        // GIVEN
        String value = "nullKeyValue";
        // WHEN
        Object previous = map.putObject(null, value);
        // THEN
        assertNull(previous);
        assertEquals(value, map.get(null));
        assertTrue(map.keySet().contains(null));
        assertEquals(1, map.size());
    }

    @ParameterizedTest(name = "[{index}] key={0}, value={1}")
    @MethodSource("keyTypeHandlingProvider")
    void putObject_keyTypeHandling(Object key, Object value, Object expectedPrevious, Object expectedStored) {
        // GIVEN – map is freshly created in @BeforeEach
        // WHEN
        Object previous = map.putObject(key, value);
        // THEN
        assertEquals(expectedPrevious, previous, () -> "Expected previous value to be " + expectedPrevious + " for key=" + key);
        assertEquals(expectedStored, map.get(key), () -> "Stored value for key=" + key + " should be " + expectedStored);
        assertEquals(1, map.size(), "Map should contain exactly one entry after this operation");
    }

    @ParameterizedTest(name = "[{index}] key={0} with null value")
    @MethodSource("nullValueHandlingProvider")
    void putObject_nullValueHandling(Object key, Object value, Object expectedPrevious) {
        // WHEN
        Object previous = map.putObject(key, value);
        // THEN
        assertEquals(expectedPrevious, previous, "First insertion with a null value should return null");
        assertTrue(map.containsKey(key), "Map must contain the key after insertion");
        assertNull(map.get(key), "Stored value should be null");
    }

    @ParameterizedTest(name = "[{index}] CharSequence key={0}")
    @MethodSource("nonStringCharSequenceProvider")
    void putObject_nonStringCharSequenceKey(Object key, Object value, Object expectedPrev, Object expectedStored) {
        // WHEN
        Object previous = map.putObject(key, value);
        // THEN
        assertEquals(expectedPrev, previous);
        assertEquals(expectedStored, map.get(key));
        assertEquals(1, map.size());
    }

    @ParameterizedTest(name = "[{index}] whitespace key={0}")
    @MethodSource("whitespaceKeyProvider")
    void putObject_whitespaceStringKey(Object key, Object value, Object expectedPrev, Object expectedStored) {
        // WHEN
        Object previous = map.putObject(key, value);
        // THEN
        assertEquals(expectedPrev, previous);
        assertEquals(expectedStored, map.get(key));
        assertEquals(1, map.size());
    }

    @ParameterizedTest(name = "[{index}] duplicate case key={0}")
    @MethodSource("duplicateCaseInsensitiveProvider")
    void putObject_duplicateCaseInsensitive(Object key, Object value, Object expectedPrev, Object expectedStored) {
        // WHEN
        Object previous = map.putObject(key, value);
        // THEN
        assertEquals(expectedPrev, previous);
        // Retrieval using a different case must succeed.
        assertEquals(expectedStored, map.get(key.toString().toUpperCase()));
        assertEquals(1, map.size());
    }

    @ParameterizedTest(name = "[{index}] key={0}")
    @MethodSource("replacementNonStringKeyProvider")
    void putObject_replacementForNonStringKey(Object key, Object value, Object expectedPrev, Object expectedStored, boolean isStringKey) {
        // WHEN
        Object previous = map.putObject(key, value);
        // THEN
        assertEquals(expectedPrev, previous);
        assertEquals(expectedStored, map.get(key));
        // Ensure the map size reflects the number of distinct logical keys.
        if (isStringKey) {
            assertEquals(1, map.size());
        } else {
            // After the integer insertion we have two distinct keys.
            int expectedSize = (key instanceof Integer && expectedPrev == null) ? 2 : 2;
            assertEquals(expectedSize, map.size());
        }
    }

    @Test
    void putObject_nullKey_withConcurrentHashMap_throwsNPE() {
        // GIVEN a backing map that disallows null keys
        Map<Object, Object> backing = new ConcurrentHashMap<>();
        CaseInsensitiveMap<Object, Object> customMap = new CaseInsensitiveMap<>();
        injectBackingMap(customMap, backing);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> customMap.putObject(null, "someValue"), "ConcurrentHashMap should reject a null key with NPE");
    }

    @Test
    void putObject_nullValue_withConcurrentHashMap_throwsNPE() {
        // GIVEN a backing map that disallows null values
        Map<Object, Object> backing = new ConcurrentHashMap<>();
        CaseInsensitiveMap<Object, Object> customMap = new CaseInsensitiveMap<>();
        injectBackingMap(customMap, backing);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> customMap.putObject("NonNullKey", null), "ConcurrentHashMap should reject a null value with NPE");
    }

    @Test
    void putObject_keyTypeMismatch_causesClassCastException() {
        // GIVEN a TreeMap<Integer, String> (expects Integer keys)
        Map<Integer, String> backing = new TreeMap<>();
        CaseInsensitiveMap<Object, Object> customMap = new CaseInsensitiveMap<>();
        injectBackingMap(customMap, backing);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> customMap.putObject("StringKey", "value"), "Putting a String key into a Map<Integer,?> should throw CCE");
    }

    @Test
    void putObject_valueTypeMismatch_causesClassCastException() {
        // GIVEN a HashMap<Integer, Integer> (expects Integer values)
        Map<Integer, Integer> backing = new HashMap<>();
        CaseInsensitiveMap<Object, Object> customMap = new CaseInsensitiveMap<>();
        injectBackingMap(customMap, backing);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> customMap.putObject(123, "StringValue"), "Putting a String value into a Map<?, Integer> should throw CCE");
    }

    @Test
    void putObject_underlyingMapIsNull_throwsNPE() {
        // GIVEN a CaseInsensitiveMap whose internal delegate is null
        CaseInsensitiveMap<Object, Object> nullBackedMap = new CaseInsensitiveMap<>();
        injectBackingMap(nullBackedMap, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> nullBackedMap.putObject("anyKey", "anyValue"), "Calling putObject when internal map is null should throw NPE");
    }
}
