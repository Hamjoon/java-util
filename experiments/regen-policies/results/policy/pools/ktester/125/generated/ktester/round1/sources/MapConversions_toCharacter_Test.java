package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toCharacter_Test {

    /**
     * Converter used for all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – they are sufficient for the built‑in conversions.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long test runs.
        converter = null;
    }

    /**
     * Helper to build a map that mimics the structure expected by MapConversions.
     */
    private static Map<String, Object> mapWithValue(Object value) {
        Map<String, Object> m = new HashMap<>();
        m.put(MapConversions.VALUE, value);
        return m;
    }

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// Already a Character – should be returned unchanged.
        Arguments.of("Character instance", new Character('a'), 'a'), // Single‑character String.
        Arguments.of("Single‑character String", "b", 'b'), // Integer representing a Unicode code point.
        Arguments.of("Integer code point 65 → 'A'", Integer.valueOf(65), 'A'), // Map with the modern key "value".
        Arguments.of("Map with key \"value\" and single‑char String", Map.of(MapConversions.VALUE, "x"), 'x'), // Map with the legacy key "_v".
        Arguments.of("Map with legacy key \"_v\" and single‑char String", Map.of(MapConversions.V, "y"), 'y'));
    }

    private static Stream<Arguments> edgeCaseExceptionsProvider() {
        return Stream.of(// Empty string cannot be mapped.
        Arguments.of("Empty String", "", IllegalArgumentException.class), // String longer than one character.
        Arguments.of("String longer than one character", "cd", IllegalArgumentException.class), // Map without recognised keys.
        Arguments.of("Map missing recognised keys", Map.of("unknown", "z"), IllegalArgumentException.class), // Boolean values are not convertible to Character.
        Arguments.of("Boolean TRUE", Boolean.TRUE, IllegalArgumentException.class));
    }

    private static Stream<Arguments> additionalExceptionProvider() {
        // Helper map with null value – Map.of does not allow null values.
        Map<String, Object> mapWithNull = new HashMap<>();
        mapWithNull.put(MapConversions.VALUE, null);
        // Helper map with a Boolean value under a recognised key.
        Map<String, Object> mapWithBoolean = new HashMap<>();
        mapWithBoolean.put(MapConversions.VALUE, Boolean.TRUE);
        // Helper map with a too‑long String under a recognised key.
        Map<String, Object> mapWithLongString = new HashMap<>();
        mapWithLongString.put(MapConversions.V, "ab");
        return Stream.of(// Map contains recognised key but value is null.
        Arguments.of("Map with recognised key but null value", mapWithNull, converter, IllegalArgumentException.class), // Map contains recognised key but value is a String longer than one character.
        Arguments.of("Map with recognised key but long String value", mapWithLongString, converter, IllegalArgumentException.class), // Number below the valid char range.
        Arguments.of("Integer -1 (below char range)", Integer.valueOf(-1), converter, IllegalArgumentException.class), // Number above the valid char range.
        Arguments.of("Integer 70000 (above char range)", Integer.valueOf(70000), converter, IllegalArgumentException.class), // Non‑integral numeric value.
        Arguments.of("Double 65.5 (non‑integral)", Double.valueOf(65.5), converter, IllegalArgumentException.class), // Unsupported source type (plain Object).
        Arguments.of("Plain Object instance", new Object(), converter, IllegalArgumentException.class), // Array type (char[]).
        Arguments.of("char[] array", new char[] { 'z' }, converter, IllegalArgumentException.class), // Map with Boolean value under recognised key.
        Arguments.of("Map with Boolean value under recognised key", mapWithBoolean, converter, IllegalArgumentException.class), // Null Converter should cause NPE.
        Arguments.of("Null Converter", "c", null, NullPointerException.class));
    }

    @Test
    void toCharacter_fromMapWithString_returnsFirstChar() {
        Map<String, Object> source = mapWithValue("xyz");
        Character result = MapConversions.toCharacter(source, converter);
        assertEquals('x', result);
    }

    @Test
    void toCharacter_fromMapWithNumber_returnsCorrespondingChar() {
        // 65 is the Unicode code point for 'A'
        Map<String, Object> source = mapWithValue(65);
        Character result = MapConversions.toCharacter(source, converter);
        assertEquals('A', result);
    }

    @Test
    void toCharacter_fromMapWithChar_returnsSameChar() {
        Map<String, Object> source = mapWithValue('Q');
        Character result = MapConversions.toCharacter(source, converter);
        assertEquals('Q', result);
    }

    @Test
    void toCharacter_fromNull_returnsNullCharacter() {
        // According to Converter's contract, a null source for a primitive char
        // conversion yields the null character '\u0000'.
        Character result = MapConversions.toCharacter(null, converter);
        assertEquals('\u0000', result);
    }

    @Test
    void toCharacter_fromMapMissingValueKey_returnsNullCharacter() {
        // An empty map should behave like a null source for the conversion.
        Map<String, Object> empty = new HashMap<>();
        Character result = MapConversions.toCharacter(empty, converter);
        assertEquals('\u0000', result);
    }

    @Test
    void toCharacter_fromMapWithBooleanTrue_returnsCharOne() {
        // Boolean true is converted to numeric 1, which maps to '\u0001'.
        Map<String, Object> source = mapWithValue(true);
        Character result = MapConversions.toCharacter(source, converter);
        assertEquals('\u0001', result);
    }

    @Test
    void toCharacter_fromMapWithBooleanFalse_returnsCharZero() {
        // Boolean false is converted to numeric 0, which maps to '\u0000'.
        Map<String, Object> source = mapWithValue(false);
        Character result = MapConversions.toCharacter(source, converter);
        assertEquals('\u0000', result);
    }

    /**
     * Verify that a variety of legitimate inputs are correctly converted
     * to a {@link Character}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionsProvider")
    void toCharacter_validConversions(String description, Object from, Character expected) {
        Character result = MapConversions.toCharacter(from, converter);
        assertEquals(expected, result, description);
    }

    @Test
    void toCharacter_nullSource_returnsNull() {
        // According to the JSON spec, a null source yields null (no conversion).
        Character result = MapConversions.toCharacter(null, converter);
        assertNull(result, "Null source should return null");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("edgeCaseExceptionsProvider")
    void toCharacter_edgeCases(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MapConversions.toCharacter(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalExceptionProvider")
    void toCharacter_additionalExceptions(String description, Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MapConversions.toCharacter(from, conv), description);
    }
}
