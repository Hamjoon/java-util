package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toLocalTime_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default options.
        converter = new Converter(new ConverterOptions());
    }

    static Stream<Arguments> validConversionsProvider() {
        return Stream.of(Arguments.of("Zero seconds should convert to midnight (00:00:00.000000000)", BigDecimal.ZERO, LocalTime.MIDNIGHT), Arguments.of("A fractional second (1.5) should produce 1 second and 500,000,000 nanoseconds", new BigDecimal("1.5"), LocalTime.ofNanoOfDay(1_500_000_000L)), Arguments.of("Maximum allowed value (one nanosecond before next day) should map to 23:59:59.999999999", new BigDecimal("86399.999999999"), LocalTime.MAX));
    }

    static Stream<Arguments> additionalValidBoundaryProvider() {
        return Stream.of(Arguments.of("Exactly half a day (43200 seconds) should map to 12:00:00", new BigDecimal("43200"), LocalTime.NOON), Arguments.of("Maximum whole‑second value (86399) should produce 23:59:59 with no fractional part", new BigDecimal("86399"), LocalTime.of(23, 59, 59)), Arguments.of("One nanosecond (0.000000001) should be represented as 00:00:00.000000001", new BigDecimal("0.000000001"), LocalTime.ofNanoOfDay(1L)));
    }

    static Stream<Arguments> roundingTruncationProvider() {
        return Stream.of(Arguments.of("Fractional nanoseconds are truncated; 1.1234567895 s becomes 1 s + 123,456,789 ns", new BigDecimal("1.1234567895"), LocalTime.ofNanoOfDay(1_123_456_789L)), Arguments.of("Half‑nanosecond rounds down to zero nanoseconds, resulting in midnight", new BigDecimal("0.0000000005"), LocalTime.MIDNIGHT), Arguments.of("Scientific notation is accepted; 0.001 s equals 1,000,000 ns", new BigDecimal("1E-3"), LocalTime.ofNanoOfDay(1_000_000L)));
    }

    static Stream<Arguments> invalidRangeProvider() {
        return Stream.of(Arguments.of("Negative value should be rejected with an IllegalArgumentException", new BigDecimal("-0.000000001")), Arguments.of("Value equal to 24 hours (86400 seconds) exceeds the allowed maximum", new BigDecimal("86400")));
    }

    static Stream<Arguments> overflowProvider() {
        return Stream.of(Arguments.of("A BigDecimal representing 10 billion seconds overflows the nanosecond long value", new BigDecimal("10000000000")), Arguments.of("A value just above Long.MAX_VALUE / 1_000_000_000 causes overflow when multiplied", new BigDecimal("9223372037")));
    }

    static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of("Providing an Integer should trigger a ClassCastException", Integer.valueOf(123)), Arguments.of("Providing a String should also result in a ClassCastException", "not-a-number"));
    }

    @Test
    void convertsZeroSecondsToMidnight() {
        BigDecimal input = BigDecimal.ZERO;
        LocalTime result = BigDecimalConversions.toLocalTime(input, converter);
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    @Test
    void convertsFractionalSecondsToCorrectLocalTime() {
        // 1.5 seconds = 1 second + 500,000,000 nanoseconds
        BigDecimal input = new BigDecimal("1.5");
        LocalTime expected = LocalTime.ofNanoOfDay(1_500_000_000L);
        LocalTime result = BigDecimalConversions.toLocalTime(input, converter);
        assertEquals(expected, result);
    }

    @Test
    void convertsMaximumAllowedSecondsToLocalTimeMax() {
        // 86 399.999 999 999 seconds = one nanosecond before next day
        BigDecimal input = new BigDecimal("86399.999999999");
        // 23:59:59.999999999
        LocalTime expected = LocalTime.MAX;
        LocalTime result = BigDecimalConversions.toLocalTime(input, converter);
        assertEquals(expected, result);
    }

    @Test
    void throwsExceptionForNegativeSeconds() {
        BigDecimal input = new BigDecimal("-0.001");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toLocalTime(input, converter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"));
    }

    @Test
    void throwsExceptionForSecondsBeyondUpperBound() {
        // exactly 24 hours, out of range
        BigDecimal input = new BigDecimal("86400");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toLocalTime(input, converter));
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionsProvider")
    void testValidConversions(String description, BigDecimal input, LocalTime expected) {
        // Act
        LocalTime result = BigDecimalConversions.toLocalTime(input, converter);
        // Assert
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalValidBoundaryProvider")
    void testAdditionalValidBoundaryValues(String description, BigDecimal input, LocalTime expected) {
        LocalTime result = BigDecimalConversions.toLocalTime(input, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("roundingTruncationProvider")
    void testRoundingAndTruncation(String description, BigDecimal input, LocalTime expected) {
        LocalTime result = BigDecimalConversions.toLocalTime(input, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidRangeProvider")
    void testInvalidRangeConversions(String description, BigDecimal input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toLocalTime(input, converter), description);
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"), "Exception message should contain the standard range text");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("overflowProvider")
    void testOverflowHandling(String description, BigDecimal input) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toLocalTime(input, converter), description);
        assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"), "Overflow should be wrapped in the same IllegalArgumentException message");
    }

    @Test
    void testNullInputThrowsNullPointerException() {
        // The method attempts to call multiply on the casted object, so a NPE is expected.
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toLocalTime(null, converter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidTypeProvider")
    void testInvalidTypeHandling(String description, Object input) {
        // The explicit cast to BigDecimal should cause a ClassCastException.
        assertThrows(ClassCastException.class, () -> BigDecimalConversions.toLocalTime(input, converter), description);
    }
}
