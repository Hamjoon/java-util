package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_secondsAndNanosToDouble_Test {

    /**
     * Supplies the test data extracted from the provided JSON specification.
     */
    private static Stream<Arguments> secondsAndNanosProvider() {
        return Stream.of(// Both seconds and nanos are zero
        Arguments.of(0L, 0L, "0", "Both seconds and nanos are zero; result should be exactly zero."), // Whole seconds only
        Arguments.of(5L, 0L, "5", "Whole seconds only, no fractional part."), // Only fractional part (half a second)
        Arguments.of(0L, 500_000_000L, "0.5", "Only fractional part (half a second) should be represented as 0.5."), // Combination of whole seconds and a quarter‑second fraction
        Arguments.of(3L, 250_000_000L, "3.25", "Combination of whole seconds and a quarter‑second fraction."), // Negative whole seconds with a positive fraction
        Arguments.of(-2L, 300_000_000L, "-1.7", "Negative whole seconds with a positive fraction; -2 + 0.3 = -1.7."), // Maximum long value for seconds, no fraction
        Arguments.of(Long.MAX_VALUE, 0L, "9223372036854775807", "Maximum long value for seconds, no fraction."), // Minimum long value for seconds, no fraction
        Arguments.of(Long.MIN_VALUE, 0L, "-9223372036854775808", "Minimum long value for seconds, no fraction."), // Nanos larger than one second (treated as 1.5 seconds)
        Arguments.of(0L, 1_500_000_000L, "1.5", "Nanos larger than one second; method treats it as 1.5 seconds."), // Positive seconds with a negative nanos fraction
        Arguments.of(10L, -250_000_000L, "9.75", "Positive seconds with a negative nanos fraction; 10 - 0.25 = 9.75."), // Both seconds and nanos are negative
        Arguments.of(-5L, -500_000_000L, "-5.5", "Both seconds and nanos are negative; -5 + (-0.5) = -5.5."), // Zero seconds with a negative half‑second nanos
        Arguments.of(0L, -500_000_000L, "-0.5", "Zero seconds with a negative half‑second nanos; result should be -0.5."), // Negative whole second combined with a positive half‑second nanos
        Arguments.of(-1L, 500_000_000L, "-0.5", "Negative whole second combined with a positive half‑second nanos; -1 + 0.5 = -0.5."), // Positive second with a negative 1.5‑second nanos
        Arguments.of(1L, -1_500_000_000L, "-0.5", "Positive second with a negative 1.5‑second nanos; 1 - 1.5 = -0.5."), // Maximum long seconds plus 1.5 seconds from nanos (overflow beyond Long.MAX_VALUE)
        Arguments.of(Long.MAX_VALUE, 1_500_000_000L, "9223372036854775808.5", "Maximum long seconds plus 1.5 seconds from nanos; verifies handling of overflow beyond Long.MAX_VALUE."), // Maximum long seconds with a negative one‑second nanos
        Arguments.of(Long.MAX_VALUE, -1_000_000_000L, "9223372036854775806", "Maximum long seconds with a negative one‑second nanos; should subtract exactly one second."), // Minimum long seconds plus one second from nanos
        Arguments.of(Long.MIN_VALUE, 1_000_000_000L, "-9223372036854775807", "Minimum long seconds plus one second from nanos; result moves one step toward zero."), // Maximum positive nanos just below one second
        Arguments.of(0L, 999_999_999L, "0.999999999", "Maximum positive nanos just below one second; ensures full 9‑digit fractional precision."), // Maximum negative nanos just above -1 second
        Arguments.of(0L, -999_999_999L, "-0.999999999", "Maximum negative nanos just above -1 second; tests negative fractional precision."), // Nanos representing exactly two seconds
        Arguments.of(0L, 2_000_000_000L, "2", "Nanos representing exactly two seconds; method should normalize to integer '2'."), // Negative seconds combined with four‑second nanos
        Arguments.of(-3L, 4_000_000_000L, "1", "Negative seconds combined with four‑second nanos; -3 + 4 = 1, testing cross‑sign aggregation."));
    }

    @Test
    void zeroSecondsZeroNanos_returnsZero() {
        // given
        long seconds = 0L;
        long nanos = 0L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        assertEquals(BigDecimal.ZERO, result, "0 seconds + 0 nanos should be 0");
    }

    @Test
    void positiveSecondsAndNanos_combinesCorrectly() {
        // given
        long seconds = 5L;
        // 0.25 seconds
        long nanos = 250_000_000L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = new BigDecimal("5.25");
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    @Test
    void negativeSecondsWithPositiveNanos_combinesCorrectly() {
        // given
        long seconds = -3L;
        // +0.5 seconds
        long nanos = 500_000_000L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        // -3 + 0.5 = -2.5
        BigDecimal expected = new BigDecimal("-2.5");
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    @Test
    void nanosOnly_whenSecondsZero_returnsFractionalPart() {
        // given
        long seconds = 0L;
        // 0.123456789 seconds
        long nanos = 123_456_789L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = new BigDecimal("0.123456789");
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    @Test
    void maxNanosValue_isHandledCorrectly() {
        // given
        long seconds = 2L;
        // just under one second
        long nanos = 999_999_999L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = new BigDecimal("2.999999999");
        assertEquals(0, expected.compareTo(result), () -> "Expected " + expected + " but got " + result);
    }

    @ParameterizedTest(name = "[{index}] seconds={0}, nanos={1} → expected={2}")
    @MethodSource("secondsAndNanosProvider")
    void secondsAndNanosToDouble_parameterized(long seconds, long nanos, String expectedStr, String description) {
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(0, expected.compareTo(result), () -> String.format("%s – Expected %s but got %s", description, expected, result));
    }
}
