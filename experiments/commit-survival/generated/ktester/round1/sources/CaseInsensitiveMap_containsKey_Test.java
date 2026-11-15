package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_containsKey_Test {

    /**
     * The map instance used in each test.
     */
    private CaseInsensitiveMap<Object, String> map;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor – it creates an internal LinkedHashMap.
        map = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to help the garbage collector and ensure isolation.
        if (map != null) {
            map.clear();
            map = null;
        }
    }

    // ---------- Non‑String key handling ----------
    private static Stream<Arguments> nonStringKeyProvider() {
        return Stream.of(// Integer key that exists
        Arguments.of(123, true, "Key is an Integer that exists in the underlying map."), // Null key – map is empty, so containsKey(null) should be false
        Arguments.of(null, false, "Key is null and not stored in the map."), // Arbitrary object not present
        Arguments.of(new Object(), false, "Arbitrary non‑String object not present."));
    }

    // ---------- String key case‑insensitive match ----------
    private static Stream<Arguments> stringCaseInsensitiveProvider() {
        return Stream.of(Arguments.of("TeSt", true, "Mixed‑case key should match underlying case‑insensitive entry."));
    }

    // ---------- String key exact match ----------
    private static Stream<Arguments> stringExactMatchProvider() {
        return Stream.of(Arguments.of("example", true, "Exact same case should match."));
    }

    // ---------- String key not present ----------
    private static Stream<Arguments> stringNotPresentProvider() {
        return Stream.of(Arguments.of("absent", false, "Key not stored should return false."));
    }

    @Test
    @DisplayName("String keys are matched case‑insensitively")
    void testContainsKey_StringCaseInsensitive() {
        // given
        map.put("KeyOne", "value1");
        map.put("AnotherKey", "value2");
        // when / then
        assertTrue(map.containsKey("keyone"), "lower‑case should match");
        assertTrue(map.containsKey("KEYONE"), "upper‑case should match");
        assertTrue(map.containsKey("KeYoNe"), "mixed case should match");
        assertTrue(map.containsKey("anotherkey"), "different key, same case‑insensitivity");
        assertFalse(map.containsKey("missing"), "non‑existent key should return false");
    }

    @Test
    @DisplayName("Non‑String keys use normal equality")
    void testContainsKey_NonStringKey() {
        // given
        Integer intKey = 42;
        map.put(intKey, "the answer");
        map.put("StringKey", "value");
        // when / then
        assertTrue(map.containsKey(42), "exact Integer key should be found");
        assertFalse(map.containsKey("42"), "String \"42\" is not the same as Integer 42");
        assertTrue(map.containsKey("STRINGKEY"), "String key still works case‑insensitively");
    }

    @Test
    @DisplayName("Null key handling")
    void testContainsKey_NullKey() {
        // given
        map.put(null, "nullValue");
        map.put("NotNull", "value");
        // when / then
        assertTrue(map.containsKey(null), "null key should be found");
        assertFalse(map.containsKey("null"), "the literal string \"null\" is not the null key");
    }

    @Test
    @DisplayName("containsKey returns false for absent keys")
    void testContainsKey_AbsentKey() {
        // given: an empty map
        assertTrue(map.isEmpty(), "map should start empty");
        // when / then
        assertFalse(map.containsKey("anything"), "any key on an empty map must be false");
        map.put("present", "yes");
        assertFalse(map.containsKey("absent"), "key not present should be false");
    }

    @ParameterizedTest(name = "[{index}] key={0} → expected={1}")
    @MethodSource("nonStringKeyProvider")
    @DisplayName("Non‑String key handling")
    void testContainsKey_NonStringKeyHandling(Object key, boolean expected, String description) {
        // Arrange – store an Integer key 123 only
        map.put(123, "intValue");
        // Act
        boolean result = map.containsKey(key);
        // Assert
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] key=\"{0}\" → {1}")
    @MethodSource("stringCaseInsensitiveProvider")
    @DisplayName("String key case‑insensitive match")
    void testContainsKey_StringCaseInsensitiveMatch(String key, boolean expected, String description) {
        // Arrange – underlying map stores a CaseInsensitiveString "test"
        // the public API wraps it automatically
        map.put("test", "value");
        // Act & Assert
        assertEquals(expected, map.containsKey(key), description);
    }

    @ParameterizedTest(name = "[{index}] key=\"{0}\" → {1}")
    @MethodSource("stringExactMatchProvider")
    @DisplayName("String key exact match")
    void testContainsKey_StringExactMatch(String key, boolean expected, String description) {
        map.put("example", "val");
        assertEquals(expected, map.containsKey(key), description);
    }

    @ParameterizedTest(name = "[{index}] key=\"{0}\" → {1}")
    @MethodSource("stringNotPresentProvider")
    @DisplayName("String key not present")
    void testContainsKey_StringNotPresent(String key, boolean expected, String description) {
        map.put("something", "val");
        assertEquals(expected, map.containsKey(key), description);
    }

    // ---------- Underlying map stores plain String ----------
    @Test
    @DisplayName("String key when underlying map stores a plain String")
    void testContainsKey_UnderlyingPlainString() throws Exception {
        // Force the internal map to be a plain HashMap that contains a raw String key.
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        Map<Object, String> plainMap = new HashMap<>();
        // plain String, not CaseInsensitiveString
        plainMap.put("Case", "value");
        mapField.set(map, plainMap);
        // The public put() method would normally wrap the key, but we bypass it.
        // Now call containsKey with a matching string (different case) – should be false.
        assertFalse(map.containsKey("case"), "Wrapped CaseInsensitiveString does not equal raw String key");
    }

    // ---------- Empty string key ----------
    @Test
    @DisplayName("Empty string key matches stored empty key")
    void testContainsKey_EmptyStringKey() {
        map.put("", "empty");
        assertTrue(map.containsKey(""), "Empty string should match stored empty key");
    }

    // ---------- String with surrounding whitespace ----------
    @Test
    @DisplayName("Whitespace‑preserving case‑insensitive match")
    void testContainsKey_WhitespaceKey() {
        map.put("  test  ", "spaced");
        assertTrue(map.containsKey("  TEST  "), "Whitespace preserved, case‑insensitive match");
    }

    // ---------- Unicode characters case‑insensitive match ----------
    @Test
    @DisplayName("Unicode characters are compared case‑insensitively")
    void testContainsKey_UnicodeCaseInsensitive() {
        map.put("café", "coffee");
        assertTrue(map.containsKey("CAFÉ"), "Accented characters should be compared case‑insensitively");
    }

    // ---------- Null key with map that disallows null ----------
    @Test
    @DisplayName("Null key on a map that forbids null throws NPE")
    void testContainsKey_NullKeyOnConcurrentHashMap() throws Exception {
        // Replace the internal map with a ConcurrentHashMap (null keys not allowed)
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(map, new ConcurrentHashMap<>());
        // The map is empty; calling containsKey(null) should propagate the NPE from
        // ConcurrentHashMap.containsKey(null)
        assertThrows(NullPointerException.class, () -> map.containsKey(null), "ConcurrentHashMap does not accept null keys, so containsKey(null) must throw NPE");
    }
}
