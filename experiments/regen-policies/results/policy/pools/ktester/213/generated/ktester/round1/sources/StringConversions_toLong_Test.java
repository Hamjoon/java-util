package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class StringConversions_toLong_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance – the default constructor provides sensible defaults.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    private static Stream<Arguments> provideBasicStringToLongCases() {
        return Stream.of(// Empty string → 0L
        Arguments.of("", null, 0L, "Empty string should be treated as missing value and return 0L."), // Simple integer string
        Arguments.of("12345", null, 12345L, "A well‑formed decimal string that fits in a long is parsed directly by Long.valueOf."), // Fractional number – fallback truncates
        Arguments.of("123.45", null, 123L, "String contains a fractional part; Long.valueOf throws, the fallback parses the number and returns its long value (truncates the fraction)."), // Non‑numeric string → IllegalArgumentException
        Arguments.of("abc", null, IllegalArgumentException.class, "Non‑numeric string cannot be parsed by Long.valueOf nor by the BigDecimal fallback, resulting in an IllegalArgumentException."), // Out‑of‑range number → IllegalArgumentException
        Arguments.of("9223372036854775808", null, IllegalArgumentException.class, "String represents a number larger than Long.MAX_VALUE; Long.valueOf throws, fallback returns null, causing IllegalArgumentException."));
    }

    private static Stream<Arguments> provideEdgeStringToLongCases() {
        return Stream.of(// Negative integer
        Arguments.of("-123", null, -123L, "Negative decimal string that fits in a long is parsed directly by Long.valueOf."), // Long.MIN_VALUE
        Arguments.of("-9223372036854775808", null, Long.MIN_VALUE, "String representing Long.MIN_VALUE parses successfully via Long.valueOf."), // Long.MAX_VALUE
        Arguments.of("9223372036854775807", null, Long.MAX_VALUE, "String representing Long.MAX_VALUE parses successfully via Long.valueOf."), // Explicit plus sign
        Arguments.of("+456", null, 456L, "String with an explicit plus sign is accepted by Long.valueOf."), // Leading zeros
        Arguments.of("00000123", null, 123L, "String with leading zeros is parsed correctly."), // Fractional < 1 → truncates to 0
        Arguments.of("0.999", null, 0L, "Fractional number less than 1 truncates to 0 after fallback to BigDecimal."), // Negative fractional
        Arguments.of("-123.45", null, -123L, "Negative fractional string is truncated toward zero after BigDecimal fallback."), // Fractional part on max value – still within range
        Arguments.of("9223372036854775807.999", null, Long.MAX_VALUE, "Fractional part is truncated; result equals Long.MAX_VALUE and is within range."), // Below Long.MIN_VALUE
        Arguments.of("-9223372036854775809", null, IllegalArgumentException.class, "Value just below Long.MIN_VALUE is out of range; fallback returns null, causing IllegalArgumentException."), // Scientific notation without decimal point
        Arguments.of("1e3", null, 1000L, "Scientific notation without decimal point is parsed by BigDecimal fallback and truncated to long."), // Scientific notation with decimal part
        Arguments.of("1.23e2", null, 123L, "Scientific notation with decimal part is parsed by BigDecimal fallback; result is truncated to long."), // Whitespace‑surrounded string – not accepted
        Arguments.of(" 123 ", null, IllegalArgumentException.class, "String with surrounding whitespace is not accepted by Long.valueOf nor by BigDecimal constructor; method throws IllegalArgumentException."), // Null input → NullPointerException (cast to String fails)
        Arguments.of(null, null, NullPointerException.class, "Passing a null reference for `from` results in a NullPointerException during the cast to String."), // Non‑String object → ClassCastException
        Arguments.of((Object) 123, null, ClassCastException.class, "Non‑String object (Integer) cannot be cast to String; method throws ClassCastException before any conversion logic."), // Providing a non‑null Converter (real instance)
        Arguments.of("123", new Converter(new ConverterOptions()), 123L, "Providing a non‑null Converter does not affect the conversion; the method still returns the correct long value."), // Providing a mocked Converter (Mockito)
        Arguments.of("456", mock(Converter.class), 456L, "A mocked Converter should also be ignored by the conversion logic."));
    }

    private static Stream<Arguments> provideExceptionStringToLongCases() {
        return Stream.of(Arguments.of("NaN", null, IllegalArgumentException.class, "String 'NaN' is not a numeric representation; both Long.valueOf and the BigDecimal fallback fail, resulting in IllegalArgumentException."), Arguments.of("Infinity", null, IllegalArgumentException.class, "String 'Infinity' cannot be parsed as a long; the method throws IllegalArgumentException."), Arguments.of("0xFF", null, IllegalArgumentException.class, "Hexadecimal notation is not supported by Long.valueOf or the BigDecimal fallback, causing IllegalArgumentException."), Arguments.of("+ 123", null, IllegalArgumentException.class, "A plus sign followed by a space is not a valid numeric format; parsing fails and IllegalArgumentException is thrown."), Arguments.of("1e1000", null, IllegalArgumentException.class, "Scientific notation with an exponent far beyond Long.MAX_VALUE results in a value outside the allowed range; the fallback returns null and the method throws IllegalArgumentException."));
    }

    @Test
    @DisplayName("Empty string should be converted to 0L")
    void emptyStringReturnsZero() {
        String input = "";
        Long result = StringConversions.toLong(input, converter);
        assertEquals(0L, result);
    }

    @Test
    @DisplayName("Valid numeric string should be converted to its Long value")
    void validNumericString() {
        String input = "1234567890";
        Long result = StringConversions.toLong(input, converter);
        assertEquals(1234567890L, result);
    }

    @Test
    @DisplayName("String that cannot be parsed as a Long but is within Long range should be converted")
    void decimalStringWithinRange() {
        // "12345.67" cannot be parsed by Long.valueOf, but the fallback uses BigDecimal.
        // The private toLong method truncates the fractional part via longValue().
        String input = "12345.67";
        Long result = StringConversions.toLong(input, converter);
        assertEquals(12345L, result);
    }

    @Test
    @DisplayName("String representing a number outside Long range should throw IllegalArgumentException")
    void outOfRangeStringThrowsException() {
        // Value larger than Long.MAX_VALUE
        // Long.MAX_VALUE + 1
        String input = "9223372036854775808";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toLong(input, converter));
        assertTrue(ex.getMessage().contains("not parseable as a long value"));
    }

    @Test
    @DisplayName("Non‑numeric string should throw IllegalArgumentException")
    void nonNumericStringThrowsException() {
        String input = "not-a-number";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toLong(input, converter));
        assertTrue(ex.getMessage().contains("not parseable as a long value"));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideBasicStringToLongCases")
    void basicStringToLongCases(String input, Converter conv, Object expected, String description) {
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            Throwable thrown = assertThrows(exClass, () -> StringConversions.toLong(input, conv), description);
            // For IllegalArgumentException we also check that the message mentions parsing
            if (exClass == IllegalArgumentException.class) {
                assertTrue(thrown.getMessage().contains("not parseable as a long value"), "Exception message should contain parsing hint");
            }
        } else {
            // Expected a concrete Long value
            Long result = StringConversions.toLong(input, conv);
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideEdgeStringToLongCases")
    void edgeStringToLongCases(String input, Converter conv, Object expected, String description) {
        if (expected instanceof Class) {
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> StringConversions.toLong(input, conv), description);
        } else {
            Long result = StringConversions.toLong(input, conv);
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideExceptionStringToLongCases")
    void exceptionStringToLongCases(String input, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> StringConversions.toLong(input, conv), description);
    }
}
