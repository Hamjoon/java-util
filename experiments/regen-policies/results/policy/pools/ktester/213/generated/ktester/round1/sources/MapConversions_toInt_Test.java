package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toInt_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter class now requires a ConverterOptions instance.
        // Use the default options for these tests.
//         converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    static Stream<Arguments> validConversions() {
        return Stream.of(// Direct Integer instance
        Arguments.of("Direct Integer instance should be returned unchanged.", 42, 42), // Long within int range
        Arguments.of("Long value that fits into Integer range should be converted via Number.intValue().", 12345L, 12345), // String numeric literal
        Arguments.of("String containing a valid integer literal should be parsed.", "56", 56), // Floating‑point truncation
        Arguments.of("Floating‑point number should be truncated to its integer part via Number.intValue().", 3.14, 3), // Boolean (unsupported) -> null
        Arguments.of("Boolean is not a supported source type for Integer conversion; method should return null.", true, null));
    }

    static Stream<Arguments> nullAndEmptyInputs() {
        return Stream.of(Arguments.of("Null source should result in null return value.", null, null), Arguments.of("Empty string cannot be parsed as an integer; method should return null.", "", null));
    }

    static Stream<Arguments> stringEdgeCases() {
        return Stream.of(Arguments.of("String with surrounding whitespace is not a valid integer literal for parseInt; expect null.", "  78  ", null), Arguments.of("String with explicit plus sign should be parsed correctly.", "+99", 99), Arguments.of("String with negative sign should be parsed correctly.", "-42", -42), Arguments.of("String containing non‑numeric characters should cause parsing failure and return null.", "12a3", null));
    }

    static Stream<Arguments> numberOverflowCases() {
        return Stream.of(Arguments.of("Integer.MAX_VALUE should be returned unchanged.", Integer.MAX_VALUE, Integer.MAX_VALUE), Arguments.of("Integer.MIN_VALUE should be returned unchanged.", Integer.MIN_VALUE, Integer.MIN_VALUE), // Long just beyond Integer.MAX_VALUE – overflow to Integer.MIN_VALUE
        Arguments.of("Long value just beyond Integer.MAX_VALUE overflows when intValue() is applied; expected Java overflow result.", 2147483648L, -2147483648), // Long just below Integer.MIN_VALUE – overflow to Integer.MAX_VALUE
        Arguments.of("Long value just below Integer.MIN_VALUE overflows to Integer.MAX_VALUE.", -2147483649L, 2147483647), // String "NaN" – not a number
        Arguments.of("String \"NaN\" is not a numeric integer literal; conversion should fail.", "NaN", null), // Floating‑point with fractional part – truncation
        Arguments.of("Floating‑point number with fractional part should be truncated via Number.intValue().", 4.0, 4));
    }

    static Stream<Arguments> mapBasedConversions() {
        return Stream.of(Arguments.of("Map with key \"value\" holding a numeric string should be extracted and parsed.", mapOf("value", "123"), 123), Arguments.of("Map with shortcut key \"_v\" holding an integer should be returned.", mapOf("_v", 456), 456), Arguments.of("Map value is a floating‑point number; conversion should truncate to integer.", mapOf("value", 78.9), 78), Arguments.of("Map contains a Boolean under the expected key; unsupported type should yield null.", mapOf("value", true), null), Arguments.of("Map does not contain any of the recognized value keys; method should return null.", mapOf("otherKey", 999), null), Arguments.of("Map key exists but the associated value is null; conversion should result in null.", mapOf("value", null), null));
    }

    static Stream<Arguments> otherNumericTypes() {
        return Stream.of(Arguments.of("Byte value should be promoted to Integer via Number.intValue().", (byte) 127, 127), Arguments.of("Short value should be promoted to Integer via Number.intValue().", (short) 32767, 32767), // Custom Number implementation
        Arguments.of("Custom object that implements Number should be handled; expected intValue() extraction yields 55.", new Number() {

            @Override
            public int intValue() {
                return 55;
            }

            @Override
            public long longValue() {
                return 55L;
            }

            @Override
            public float floatValue() {
                return 55f;
            }

            @Override
            public double doubleValue() {
                return 55d;
            }
        }, 55), Arguments.of("Zero value from any supported numeric source should return 0.", 0, 0));
    }

    static Stream<Arguments> numberFormatOverflow() {
        return Stream.of(Arguments.of("String represents a numeric value outside the range of Integer; Integer.parseInt should throw NumberFormatException.", "9999999999"), Arguments.of("Negative numeric string beyond Integer.MIN_VALUE should cause NumberFormatException.", "-9999999999"));
    }

    static Stream<Arguments> unsupportedSourceTypes() {
        return Stream.of(Arguments.of("Plain Object does not implement any supported conversion interface; dispatch attempts a cast and throws ClassCastException.", new Object()), Arguments.of("Another unsupported object type triggers a ClassCastException during conversion.", new Thread()));
    }

    static Stream<Arguments> mapNonConvertible() {
        return Stream.of(Arguments.of("Map contains a recognized key but the associated value is not a Number, String, or Map; conversion attempts to cast and throws ClassCastException.", mapOf("value", new Object())), Arguments.of("Shortcut key \"_v\" holds an unsupported object; conversion should fail with ClassCastException.", mapOf("_v", new Thread())));
    }

    static Stream<Arguments> mapNumericStringOverflow() {
        return Stream.of(Arguments.of("Map value is a numeric string exceeding Integer range; parsing should throw NumberFormatException.", mapOf("value", "9999999999")), Arguments.of("Shortcut key holds an out‑of‑range numeric string; parsing should raise NumberFormatException.", mapOf("_v", "-9999999999")));
    }

    /**
     * Convenience method to create a mutable map with a single entry.
     */
    private static Map<String, Object> mapOf(String key, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(key, value);
        return m;
    }

    @Test
    void toInt_usesUnderscoreVKey_WhenPresent() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("_v", "123");
        Integer result = MapConversions.toInt(map, converter);
        assertEquals(123, result);
    }

    @Test
    void toInt_usesValueKey_WhenUnderscoreVMissing() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("value", 45);
        Integer result = MapConversions.toInt(map, converter);
        assertEquals(45, result);
    }

    @Test
    void toInt_returnsNull_WhenNoRelevantKey() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("other", "99");
        Integer result = MapConversions.toInt(map, converter);
        assertNull(result);
    }

    @Test
    void toInt_returnsNull_WhenSourceIsNull() {
        Integer result = MapConversions.toInt(null, converter);
        assertNull(result);
    }

    @Test
    void toInt_returnsNull_WhenSourceIsNotAMap() {
        Integer result = MapConversions.toInt(7, converter);
        assertNull(result);
    }

    // ---------- Valid conversions ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversions")
    void toInt_validConversions(String description, Object from, Integer expected) {
        Integer result = MapConversions.toInt(from, converter);
        assertEquals(expected, result, description);
    }

    // ---------- Null and empty inputs ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullAndEmptyInputs")
    void toInt_nullAndEmptyInputs(String description, Object from, Integer expected) {
        Integer result = MapConversions.toInt(from, converter);
        assertEquals(expected, result, description);
    }

    // ---------- String edge cases ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringEdgeCases")
    void toInt_stringEdgeCases(String description, String from, Integer expected) {
        Integer result = MapConversions.toInt(from, converter);
        assertEquals(expected, result, description);
    }

    // ---------- Number overflow and special values ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numberOverflowCases")
    void toInt_numberOverflow(String description, Number from, Integer expected) {
        Integer result = MapConversions.toInt(from, converter);
        assertEquals(expected, result, description);
    }

    // ---------- Map based conversions ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mapBasedConversions")
    void toInt_mapBasedConversions(String description, Map<String, Object> map, Integer expected) {
        Integer result = MapConversions.toInt(map, converter);
        assertEquals(expected, result, description);
    }

    // ---------- Other numeric types ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("otherNumericTypes")
    void toInt_otherNumericTypes(String description, Object from, Integer expected) {
        Integer result = MapConversions.toInt(from, converter);
        assertEquals(expected, result, description);
    }

    // ---------- Null arguments ----------
    @Test
    void toInt_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toInt(123, null), "Converter argument is null; method should throw NullPointerException.");
    }

    @Test
    void toInt_nullBothArguments_throwsNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toInt(null, null), "Both arguments null; method should throw NullPointerException before any other logic.");
    }

    // ---------- Number format overflow ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numberFormatOverflow")
    void toInt_numberFormatOverflow(String description, String from) {
        assertThrows(NumberFormatException.class, () -> MapConversions.toInt(from, converter), description);
    }

    // ---------- Unsupported source type ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedSourceTypes")
    void toInt_unsupportedSourceTypes(String description, Object from) {
        assertThrows(ClassCastException.class, () -> MapConversions.toInt(from, converter), description);
    }

    // ---------- Map with non‑convertible value ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mapNonConvertible")
    void toInt_mapNonConvertible(String description, Map<String, Object> map) {
        assertThrows(ClassCastException.class, () -> MapConversions.toInt(map, converter), description);
    }

    // ---------- Map with numeric string overflow ----------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mapNumericStringOverflow")
    void toInt_mapNumericStringOverflow(String description, Map<String, Object> map) {
        assertThrows(NumberFormatException.class, () -> MapConversions.toInt(map, converter), description);
    }
}