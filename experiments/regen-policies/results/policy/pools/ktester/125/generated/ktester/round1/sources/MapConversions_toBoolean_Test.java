package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toBoolean_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter sharedConverter;

    /**
     * Instance used for each test (currently the same as {@code sharedConverter} but kept
     *  separate in case future tests need per‑test configuration).
     */
    private Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter works with the default options; it is thread‑safe for read‑only operations.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // For now we just reuse the shared instance; cloning is not required because
        // Converter is thread‑safe for read‑only operations.
        converter = sharedConverter;
    }

    private static Stream<Arguments> directBooleanProvider() {
        return Stream.of(Arguments.of("Direct Boolean true should be returned as TRUE", true, Boolean.TRUE), Arguments.of("Direct Boolean false should be returned as FALSE", false, Boolean.FALSE));
    }

    private static Stream<Arguments> stringRepresentationProvider() {
        return Stream.of(Arguments.of("String \"true\" (lower‑case) → TRUE", "true", Boolean.TRUE), Arguments.of("String \"TRUE\" (upper‑case) → TRUE", "TRUE", Boolean.TRUE), Arguments.of("String \"false\" (lower‑case) → FALSE", "false", Boolean.FALSE), Arguments.of("String \"FaLsE\" (mixed‑case) → FALSE", "FaLsE", Boolean.FALSE), Arguments.of("String with leading/trailing spaces → TRUE", "  true  ", Boolean.TRUE), Arguments.of("String with newline/tab whitespace → FALSE", "\nfalse\t", Boolean.FALSE));
    }

    private static Stream<Arguments> numericRepresentationProvider() {
        return Stream.of(Arguments.of("Numeric 1 (int) → TRUE", 1, Boolean.TRUE), Arguments.of("Numeric 0 (int) → FALSE", 0, Boolean.FALSE), Arguments.of("Numeric 2L (long, non‑zero) → TRUE", 2L, Boolean.TRUE), Arguments.of("Numeric -5 (negative, non‑zero) → TRUE", -5, Boolean.TRUE));
    }

    private static Stream<Arguments> mapWithKnownKeysProvider() {
        return Stream.of(Arguments.of("Map {\"value\": true} → TRUE", Map.of("value", true), Boolean.TRUE), Arguments.of("Map {\"_v\": false} → FALSE", Map.of("_v", false), Boolean.FALSE), Arguments.of("Map {\"value\": \"true\"} → TRUE", Map.of("value", "true"), Boolean.TRUE), Arguments.of("Map {\"_v\": \"false\"} → FALSE", Map.of("_v", "false"), Boolean.FALSE), Arguments.of("Map {\"value\": 0} → FALSE", Map.of("value", 0), Boolean.FALSE), Arguments.of("Map {\"_v\": 5} → TRUE", Map.of("_v", 5), Boolean.TRUE));
    }

    private static Stream<Arguments> nullSourceProvider() {
        return Stream.of(Arguments.of("Null source should return null", null));
    }

    private static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(// Unsupported object types
        Arguments.of("Unsupported plain Object should throw ConversionException", new Object()), // Collection that is not a recognized Boolean representation
        Arguments.of("Unsupported List should throw ConversionException", List.of(1, 2, 3)), // Invalid string literals
        Arguments.of("String \"yes\" is not a Boolean literal", "yes"), Arguments.of("String \"maybe\" is not a Boolean literal", "maybe"), Arguments.of("Empty string should throw ConversionException", ""), Arguments.of("String with only whitespace should throw ConversionException", "   "), // Invalid map structures
        Arguments.of("Map without recognised keys should throw ConversionException", Map.of("other", true)), Arguments.of("Map with recognised key but unsupported value type (nested map)", Map.of("value", Map.of())), Arguments.of("Map with recognised key but null value should throw ConversionException", Map.of("_v", null)), // Invalid numeric representations
        Arguments.of("Double.NaN should throw ConversionException", Double.NaN), Arguments.of("Double.POSITIVE_INFINITY should throw ConversionException", Double.POSITIVE_INFINITY), Arguments.of("Double.NEGATIVE_INFINITY should throw ConversionException", Double.NEGATIVE_INFINITY));
    }

    @Test
    void toBoolean_fromMapWithValueKey_trueString() {
        // given
        Map<String, Object> map = new HashMap<>();
        // String that Converter can turn into Boolean
        map.put("value", "true");
        // when
        Boolean result = MapConversions.toBoolean(map, converter);
        // then
        assertTrue(result, "String \"true\" should convert to Boolean true");
    }

    @Test
    void toBoolean_fromMapWithValueKey_falseString() {
        // given
        Map<String, Object> map = Map.of("value", "false");
        // when
        Boolean result = MapConversions.toBoolean(map, converter);
        // then
        assertFalse(result, "String \"false\" should convert to Boolean false");
    }

    @Test
    void toBoolean_fromMapWithValueKey_boolean() {
        // given
        Map<String, Object> map = Map.of("value", Boolean.TRUE);
        // when
        Boolean result = MapConversions.toBoolean(map, converter);
        // then
        assertTrue(result);
    }

    @Test
    void toBoolean_fromMapWithUnderscoreVKey_numericOne() {
        // given
        // numeric 1 should become true
        Map<String, Object> map = Map.of("_v", 1);
        // when
        Boolean result = MapConversions.toBoolean(map, converter);
        // then
        assertTrue(result);
    }

    @Test
    void toBoolean_fromMapWithUnderscoreVKey_numericZero() {
        // given
        // numeric 0 should become false
        Map<String, Object> map = Map.of("_v", 0);
        // when
        Boolean result = MapConversions.toBoolean(map, converter);
        // then
        assertFalse(result);
    }

    @Test
    void toBoolean_directBooleanInput() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        Boolean result = MapConversions.toBoolean(source, converter);
        // then
        assertFalse(result);
    }

    @Test
    void toBoolean_stringTrueInput() {
        // given
        String source = "true";
        // when
        Boolean result = MapConversions.toBoolean(source, converter);
        // then
        assertTrue(result);
    }

    @Test
    void toBoolean_stringFalseInput() {
        // given
        String source = "false";
        // when
        Boolean result = MapConversions.toBoolean(source, converter);
        // then
        assertFalse(result);
    }

    @Test
    void toBoolean_nullInputReturnsNull() {
        // given
        Object source = null;
        // when
        Boolean result = MapConversions.toBoolean(source, converter);
        // then
        assertNull(result, "Null source should result in null Boolean");
    }

    @Test
    void toBoolean_unsupportedTypeReturnsNull() {
        // given
        // No map key, not directly convertible
        Object source = new Object();
        // when
        Boolean result = MapConversions.toBoolean(source, converter);
        // then
        // Converter returns null for object types when it cannot perform a conversion.
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("directBooleanProvider")
    void toBoolean_directBooleanValues(String description, Object from, Boolean expected) {
        Boolean result = MapConversions.toBoolean(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringRepresentationProvider")
    void toBoolean_stringRepresentations(String description, String from, Boolean expected) {
        Boolean result = MapConversions.toBoolean(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericRepresentationProvider")
    void toBoolean_numericRepresentations(String description, Number from, Boolean expected) {
        Boolean result = MapConversions.toBoolean(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mapWithKnownKeysProvider")
    void toBoolean_mapWithKnownKeys(String description, Map<String, Object> from, Boolean expected) {
        Boolean result = MapConversions.toBoolean(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullSourceProvider")
    void toBoolean_nullSource(String description, Object from) {
        Boolean result = MapConversions.toBoolean(from, converter);
        assertNull(result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedSourceProvider")
    void toBoolean_unsupportedSources(String description, Object from) {
        assertThrows(ConversionException.class, () -> MapConversions.toBoolean(from, converter), description);
    }
}
