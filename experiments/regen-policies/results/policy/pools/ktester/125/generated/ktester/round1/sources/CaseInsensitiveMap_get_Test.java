package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

// retained from the template; not used here
@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_get_Test {

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure the CI‑String cache is at its default size for all tests.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset any static configuration that a test might have changed.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    private static Stream<Arguments> stringKeyProvider() {
        return Stream.of(// key, expected value, description
        Arguments.of("TeSt", "value1", "lower‑case stored key, mixed‑case lookup"), Arguments.of("TEST", "value1", "upper‑case lookup"), Arguments.of("tESt", "value1", "mixed‑case lookup"), Arguments.of("Absent", null, "key not present"), Arguments.of("", "emptyKeyValue", "empty string key"), Arguments.of("   ", "whitespaceKeyValue", "whitespace‑only string key"));
    }

    @Test
    void getShouldBeCaseInsensitiveForStringKeys() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");
        map.put("Three", "Four");
        map.put("Five", "Six");
        // Various capitalisations of the stored keys
        assertEquals("Two", map.get("one"));
        assertEquals("Two", map.get("ONE"));
        assertEquals("Two", map.get("OnE"));
        assertEquals("Four", map.get("THREE"));
        assertEquals("Four", map.get("three"));
        assertEquals("Six", map.get("FiVe"));
        assertEquals("Six", map.get("FIVE"));
    }

    @Test
    void getShouldReturnOriginalValueForNonStringKeys() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
        map.put(42, "Answer");
        map.put(3.14, "Pi");
        map.put(true, "True");
        assertEquals("Answer", map.get(42));
        assertEquals("Pi", map.get(3.14));
        assertEquals("True", map.get(Boolean.TRUE));
        // Ensure that a String that looks like a number is *not* treated as the numeric key
        map.put("42", "StringAnswer");
        assertEquals("StringAnswer", map.get("42"));
        assertEquals("Answer", map.get(42));
    }

    @Test
    void getShouldHandleNullKey() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put(null, "nullValue");
        map.put("Key", "value");
        assertEquals("nullValue", map.get(null));
        // Normal string lookup still works
        assertEquals("value", map.get("KEY"));
    }

    @Test
    void getShouldReturnNullWhenKeyIsAbsent() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Existing", "value");
        assertNull(map.get("nonexistent"));
        assertNull(map.get("EXISTING2"));
        // non‑String key not present
        assertNull(map.get(123));
    }

    /**
     * Replaces the private final {@code map} field of the supplied {@link CaseInsensitiveMap}
     * instance with the given {@code newMap}. This is required for tests that need a
     * specific backing implementation (e.g. {@link ConcurrentHashMap} or {@link TreeMap}).
     */
    @SuppressWarnings("unchecked")
    private static <K, V> void setBackingMap(CaseInsensitiveMap<K, V> ciMap, Map<K, V> newMap) {
        try {
            Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
            mapField.setAccessible(true);
            // Remove the 'final' modifier (required for Java 12+ as well)
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(mapField, mapField.getModifiers() & ~Modifier.FINAL);
            mapField.set(ciMap, newMap);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to inject backing map via reflection", e);
        }
    }

    @Test
    void getShouldBeCaseInsensitiveForStringKeys_basic() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("One", "Two");
        map.put("Three", "Four");
        map.put("Five", "Six");
        // Various capitalisations of the stored keys
        assertEquals("Two", map.get("one"));
        assertEquals("Two", map.get("ONE"));
        assertEquals("Two", map.get("OnE"));
        assertEquals("Four", map.get("THREE"));
        assertEquals("Four", map.get("three"));
        assertEquals("Six", map.get("FiVe"));
        assertEquals("Six", map.get("FIVE"));
    }

    /**
     * Parameterized test that verifies case‑insensitive lookup for a variety of
     * {@link String} keys, as well as handling of empty and whitespace‑only keys.
     */
    @ParameterizedTest(name = "[{index}] get({0}) → {1} – {2}")
    @MethodSource("stringKeyProvider")
    void getStringKeyVariations(String key, String expected, String description) {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        // Populate the map according to the description of each case
        // for the first three rows
        map.put("test", "value1");
        // empty string case
        map.put("", "emptyKeyValue");
        // whitespace case
        map.put("   ", "whitespaceKeyValue");
        // Perform the lookup and verify the result
        assertEquals(expected, map.get(key), description);
    }

    @Test
    void getShouldReturnNullForAbsentStringKey() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("test", "value1");
        assertNull(map.get("Absent"));
    }

    @Test
    void getShouldReturnValueForNullKeyWhenPresent() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put(null, "nullKeyValue");
        assertEquals("nullKeyValue", map.get(null));
    }

    @Test
    void getShouldReturnNullForNullKeyWhenAbsent() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        // No null entry added
        assertNull(map.get(null));
    }

    @Test
    void getShouldHandleNonStringKeys() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
        map.put(123, "intValue");
        // No entry for 456
        assertEquals("intValue", map.get(123));
        assertNull(map.get(456));
    }

    @Test
    void getShouldThrowNullPointerExceptionWhenBackingMapDisallowsNull() {
        // Build a map with a ConcurrentHashMap backing (which forbids null keys)
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
        setBackingMap(ciMap, new ConcurrentHashMap<>());
        // The underlying map does not contain any entries; the call itself should
        // trigger the NPE from ConcurrentHashMap.get(null)
        assertThrows(NullPointerException.class, () -> ciMap.get(null));
    }

    @Test
    void getShouldThrowClassCastExceptionWhenKeyTypeIncompatibleWithTreeMap() {
        // TreeMap with natural ordering expects comparable keys (String in this case)
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
        setBackingMap(ciMap, new TreeMap<>());
        // Populate with a proper String key so the map is not empty
        ciMap.put("alpha", "first");
        // Supplying an Integer forces TreeMap to attempt a comparison and throw CCE
        assertThrows(ClassCastException.class, () -> ciMap.get(123));
    }
}
