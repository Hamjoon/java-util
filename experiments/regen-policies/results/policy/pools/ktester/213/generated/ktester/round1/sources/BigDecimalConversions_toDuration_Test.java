package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toDuration_Test {

    /**
     * The Converter is not used by {@code toDuration}, but the method signature requires it.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Whole and zero seconds
        arguments("Zero seconds (BigDecimal.ZERO) → Duration.ZERO", "0", "PT0S"), arguments("Positive whole‑second value", "123", "PT123S"), arguments("Negative whole‑second value", "-10", "PT-10S"), arguments("Maximum long value as seconds", "9223372036854775807", "PT9223372036854775807S"), // Fractional seconds
        arguments("Half a second", "0.5", "PT0.5S"), arguments("Exact nine‑digit fractional part", "2.123456789", "PT2.123456789S"), arguments("More than nine fractional digits – truncated", "3.123456789123", "PT3.123456789S"), arguments("Negative fractional seconds – normalised", "-4.75", "-PT4.75S"), // Scientific notation
        arguments("Scientific notation 1E3 → 1000 seconds", "1E3", "PT1000S"), // Sub‑nanosecond truncation
        arguments("Sub‑nanosecond value truncated to 1 ns", "0.0000000015", "PT0.000000001S"), arguments("Negative sub‑nanosecond truncated toward zero", "-0.0000000015", "-PT0.000000001S"), // Large integer with fractional part
        arguments("Max long + fractional part", "9223372036854775807.123", "PT9223372036854775807.123S"), arguments("Very large integer with >9 fractional digits", "123456789012345.987654321987", "PT123456789012345.987654321S"), // Negative zero handling
        arguments("Negative zero normalised to zero", "-0.0", "PT0S"), // Non‑null converter argument (method ignores it)
        arguments("Non‑null converter does not affect result", "5.25", "PT5.25S"));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(arguments("Null 'from' argument → NPE", null, NullPointerException.class), arguments("'from' not a BigDecimal (String) → ClassCastException", "\"123\"", ClassCastException.class));
    }

    /* -------------------------------------------------------------
     *  Helper to convert the JSON‑style 'from' value into the actual
     *  argument passed to the method under test.
     * ------------------------------------------------------------- */
    private static Object createSourceObject(Object from) {
        if (from == null) {
            return null;
        }
        // In the JSON test data, string literals are represented with surrounding quotes.
        // Detect that pattern and return a plain String (to trigger ClassCastException).
        if (from instanceof String) {
            String s = (String) from;
            if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
                // Strip the surrounding quotes to produce a String object, not a BigDecimal.
                return s.substring(1, s.length() - 1);
            }
            // Otherwise treat the string as a numeric literal for BigDecimal construction.
            return new BigDecimal(s);
        }
        // Fallback (should not occur with current data)
        return from;
    }

    @Test
    void toDuration_wholeSeconds() {
        // given
        // exactly 10 seconds, no fractional part
        BigDecimal source = new BigDecimal("10");
        // when
        Duration result = BigDecimalConversions.toDuration(source, mockConverter);
        // then
        assertEquals(Duration.ofSeconds(10), result, "Duration should represent exactly 10 seconds with zero nanos");
    }

    @Test
    void toDuration_fractionalSecondsExactNineDigits() {
        // given
        // 5 seconds + 123,456,789 nanos
        BigDecimal source = new BigDecimal("5.123456789");
        // when
        Duration result = BigDecimalConversions.toDuration(source, mockConverter);
        // then
        assertEquals(Duration.ofSeconds(5, 123_456_789), result, "Duration should correctly translate the 9‑digit fractional part to nanos");
    }

    @Test
    void toDuration_fractionalSecondsMoreThanNineDigits() {
        // given
        // The fractional part has more than 9 digits; the conversion should truncate after 9 digits.
        BigDecimal source = new BigDecimal("2.123456789123");
        // when
        Duration result = BigDecimalConversions.toDuration(source, mockConverter);
        // then
        // Expected nanos = 0.123456789 * 1_000_000_000 = 123_456_789 (extra digits are ignored)
        assertEquals(Duration.ofSeconds(2, 123_456_789), result, "Duration should truncate fractional digits beyond nanosecond precision");
    }

    @Test
    void toDuration_negativeValue() {
        // given
        // -3.5 seconds
        BigDecimal source = new BigDecimal("-3.5");
        // when
        Duration result = BigDecimalConversions.toDuration(source, mockConverter);
        // then
        // Duration.ofSeconds(-3, -500_000_000) normalises to -4 seconds + 500_000_000 nanos,
        // which represents -3.5 seconds.
        Duration expected = Duration.ofSeconds(-3, -500_000_000);
        assertEquals(expected, result, "Duration should correctly handle negative values, preserving sign and magnitude");
    }

    /* -------------------------------------------------------------
     *  Valid conversions – whole seconds, fractional seconds,
     *  scientific notation and edge‑cases.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toDuration_validConversions(String description, Object from, String expectedIso) {
        // given
        Object source = createSourceObject(from);
        // when
        Duration result = BigDecimalConversions.toDuration(source, mockConverter);
        // then
        Duration expected = Duration.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Invalid inputs – null source, wrong type.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidInputProvider")
    void toDuration_invalidInputs(String description, Object from, Class<? extends Throwable> expectedException) {
        // given
        Object source = createSourceObject(from);
        // when / then
        assertThrows(expectedException, () -> BigDecimalConversions.toDuration(source, mockConverter), description);
    }

    /* -------------------------------------------------------------
     *  Overflow scenario – positive nanosecond overflow.
     * ------------------------------------------------------------- */
    @Test
    void toDuration_overflowPositiveNanoseconds() {
        // given
        // Long.MAX_VALUE + 1 ns
        BigDecimal source = new BigDecimal("9223372036854775807.000000001");
        // when / then
        assertThrows(ArithmeticException.class, () -> BigDecimalConversions.toDuration(source, mockConverter), "Overflow should result in ArithmeticException");
    }
}
