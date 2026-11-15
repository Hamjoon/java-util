package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toInt_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use defaults.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources.
        converter = null;
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        // Normal converter (non‑null) and a mock converter are both acceptable.
        Converter mockConverter = mock(Converter.class);
        return Stream.of(// Empty string (already covered, but kept for completeness)
        Arguments.of("", converter, 0), // Simple positive number
        Arguments.of("123", converter, 123), // Negative number
        Arguments.of("-456", converter, -456), // Integer boundaries
        Arguments.of("-2147483648", converter, Integer.MIN_VALUE), Arguments.of("2147483647", converter, Integer.MAX_VALUE), // Explicit plus sign
        Arguments.of("+123", converter, 123), // Leading zeros are ignored
        Arguments.of("00123", converter, 123), // Zero
        Arguments.of("0", converter, 0), // "-0" is interpreted as zero by Integer.valueOf
        Arguments.of("-0", converter, 0), // Using a mock Converter – the argument is ignored by toInt
        Arguments.of("123", mockConverter, 123));
    }

    private static Stream<Arguments> provideExceptionCases() {
        Converter mockConverter = mock(Converter.class);
        return Stream.of(// Out‑of‑range values (beyond Integer.MAX_VALUE)
        Arguments.of("2147483648", converter, IllegalArgumentException.class), // Below Integer.MIN_VALUE
        Arguments.of("-2147483649", converter, IllegalArgumentException.class), // Completely non‑numeric
        Arguments.of("notANumber", converter, IllegalArgumentException.class), // Null input – the method casts to String without a null‑check
        Arguments.of(null, converter, ClassCastException.class), // Wrong type (Integer instead of String)
        Arguments.of(5, converter, ClassCastException.class), // StringBuilder (implements CharSequence but not String)
        Arguments.of(new StringBuilder("123"), converter, ClassCastException.class), // StringBuffer (implements CharSequence but not String)
        Arguments.of(new StringBuffer("456"), converter, ClassCastException.class), // Leading whitespace – Integer.valueOf does not trim
        Arguments.of(" 123", converter, IllegalArgumentException.class), // Trailing whitespace
        Arguments.of("123 ", converter, IllegalArgumentException.class), // Decimal notation
        Arguments.of("123.0", converter, IllegalArgumentException.class), // Hexadecimal literal without radix
        Arguments.of("0x10", converter, IllegalArgumentException.class), // Number exceeding Long.MAX_VALUE (fallback toLong fails)
        Arguments.of("9223372036854775808", converter, IllegalArgumentException.class), // Embedded newline character
        Arguments.of("\n123", converter, IllegalArgumentException.class), // Whitespace‑only string (StringUtilities.isEmpty returns false)
        Arguments.of("   ", converter, IllegalArgumentException.class), // Single plus sign – not a number
        Arguments.of("+", converter, IllegalArgumentException.class), // Single minus sign – not a number
        Arguments.of("-", converter, IllegalArgumentException.class), // Underscore digit separator (not accepted by Integer.valueOf)
        Arguments.of("1_000", converter, IllegalArgumentException.class), // Embedded space
        Arguments.of("12 3", converter, IllegalArgumentException.class), // Trailing tab character
        Arguments.of("123\t", converter, IllegalArgumentException.class), // Parentheses around number
        Arguments.of("(123)", converter, IllegalArgumentException.class), // Literal string "null"
        Arguments.of("null", converter, IllegalArgumentException.class), // Multiple sign characters
        Arguments.of("++123", converter, IllegalArgumentException.class), // Trailing newline
        Arguments.of("123\n", converter, IllegalArgumentException.class), // Using a mock Converter with an invalid string (behaviour unchanged)
        Arguments.of("abc", mockConverter, IllegalArgumentException.class));
    }

    @Test
    @DisplayName("Empty string should be converted to 0")
    void emptyStringReturnsZero() {
        Integer result = StringConversions.toInt("", converter);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("Valid integer string should be parsed correctly")
    void validIntegerString() {
        Integer result = StringConversions.toInt("123", converter);
        assertEquals(123, result);
    }

    @Test
    @DisplayName("Negative integer string should be parsed correctly")
    void negativeIntegerString() {
        Integer result = StringConversions.toInt("-456", converter);
        assertEquals(-456, result);
    }

    @Test
    @DisplayName("String representing a value larger than Integer.MAX_VALUE should throw IllegalArgumentException")
    void outOfRangeValueThrows() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt("2147483648", converter));
    }

    @Test
    @DisplayName("Non‑numeric string should throw IllegalArgumentException")
    void nonNumericStringThrows() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toInt("abc", converter));
    }

    @ParameterizedTest(name = "[{index}] toInt({0}) => {1}")
    @MethodSource("provideSuccessfulConversions")
    @DisplayName("Successful conversions to Integer")
    void testSuccessfulConversions(Object from, Converter conv, Integer expected) {
        Integer actual = StringConversions.toInt(from, conv);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] toInt({0}) throws {1}")
    @MethodSource("provideExceptionCases")
    @DisplayName("Invalid inputs should throw the expected exception")
    void testExceptionCases(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> StringConversions.toInt(from, conv));
    }
}
