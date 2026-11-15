package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_secondsAndNanosToDouble_Test {

    /**
     * Basic conversions – zero/positive values without edge cases.
     */
    private static Stream<Arguments> basicConversions() {
        return Stream.of(// Both seconds and nanos are zero
        arguments(0L, 0L, "0"), // Whole seconds only
        arguments(1L, 0L, "1"), // Zero seconds with half‑second nanos
        arguments(0L, 500_000_000L, "0.5"), // Positive seconds with quarter‑second nanos
        arguments(2L, 250_000_000L, "2.25"));
    }

    /**
     * Boundary and extreme values – max/min longs and nanos roll‑over cases.
     */
    private static Stream<Arguments> boundaryAndExtremeValues() {
        return Stream.of(// Max long seconds
        arguments(Long.MAX_VALUE, 0L, "9223372036854775807"), // Min long seconds
        arguments(Long.MIN_VALUE, 0L, "-9223372036854775808"), // Largest valid nanosecond fraction (< 1 second)
        arguments(0L, 999_999_999L, "0.999999999"), // Exactly one‑billion nanos → should add one whole second
        arguments(0L, 1_000_000_000L, "1"), // Negative one‑billion nanos → should subtract one whole second
        arguments(0L, -1_000_000_000L, "-1"));
    }

    /**
     * Negative and mixed sign scenarios.
     */
    private static Stream<Arguments> negativeAndMixedValues() {
        return Stream.of(// Negative whole seconds, no nanos
        arguments(-1L, 0L, "-1"), // Negative seconds + positive half‑second nanos → -0.5
        arguments(-1L, 500_000_000L, "-0.5"), // Zero seconds + negative half‑second nanos
        arguments(0L, -500_000_000L, "-0.5"), // Positive seconds + negative quarter‑second nanos → 0.75
        arguments(1L, -250_000_000L, "0.75"), // Large negative seconds with positive fractional part
        arguments(-123L, 123_456_789L, "-122.876543211"));
    }

    /**
     * Mixed magnitude and fractional‑precision cases, including min‑nano values.
     */
    private static Stream<Arguments> mixedMagnitudeAndFractionalPrecision() {
        return Stream.of(// Typical positive seconds with full 9‑digit nanos
        arguments(123L, 123_456_789L, "123.123456789"), // Positive seconds with negative nanos
        arguments(123L, -123_456_789L, "122.876543211"), // Both seconds and nanos negative
        arguments(-123L, -123_456_789L, "-123.123456789"), // Minimum positive nanosecond (scale‑9)
        arguments(0L, 1L, "0.000000001"), // Minimum negative nanosecond
        arguments(0L, -1L, "-0.000000001"));
    }

    @Test
    @DisplayName("Zero seconds and zero nanos → 0.0")
    void zeroSecondsZeroNanos() {
        // given
        long seconds = 0L;
        long nanos = 0L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        assertEquals(BigDecimal.ZERO, result, "Result should be exactly zero");
    }

    @Test
    @DisplayName("Positive seconds without nanos")
    void positiveSecondsNoNanos() {
        // given
        long seconds = 12345L;
        long nanos = 0L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        assertEquals(BigDecimal.valueOf(seconds), result, "Result should equal the seconds value when nanos are zero");
    }

    @Test
    @DisplayName("Positive seconds with nanos")
    void positiveSecondsWithNanos() {
        // given
        long seconds = 10L;
        // 0.25 seconds
        long nanos = 250_000_000L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanos, 9));
        assertEquals(expected, result, "Result should combine seconds and nanos correctly");
    }

    @Test
    @DisplayName("Maximum nanos (just under one second)")
    void maxNanos() {
        // given
        long seconds = 5L;
        // one nanosecond shy of a full second
        long nanos = 999_999_999L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanos, 9));
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Negative seconds and nanos")
    void negativeValues() {
        // given
        long seconds = -3L;
        // -0.5 seconds
        long nanos = -500_000_000L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanos, 9));
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Mixed sign: negative seconds, positive nanos")
    void mixedSignNegativeSecondsPositiveNanos() {
        // given
        long seconds = -2L;
        // +0.3 seconds
        long nanos = 300_000_000L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanos, 9));
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Mixed sign: positive seconds, negative nanos")
    void mixedSignPositiveSecondsNegativeNanos() {
        // given
        long seconds = 7L;
        // -0.25 seconds
        long nanos = -250_000_000L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanos, 9));
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Large values to ensure no overflow")
    void largeValues() {
        // given
        // a large positive number
        long seconds = Long.MAX_VALUE / 2;
        long nanos = 123_456_789L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanos, 9));
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Zero seconds with positive nanos")
    void zeroSecondsPositiveNanos() {
        // given
        long seconds = 0L;
        // 0.042 seconds
        long nanos = 42_000_000L;
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = BigDecimal.valueOf(nanos, 9);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] seconds={0}, nanos={1} → expected={2}")
    @MethodSource("basicConversions")
    @DisplayName("Basic conversions")
    void testBasicConversions(long seconds, long nanos, String expectedStr) {
        // given – parameters are supplied by the method source
        // when
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        // then
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(expected, result, () -> String.format("seconds=%d, nanos=%d → expected %s but got %s", seconds, nanos, expected, result));
    }

    @ParameterizedTest(name = "[{index}] seconds={0}, nanos={1} → expected={2}")
    @MethodSource("boundaryAndExtremeValues")
    @DisplayName("Boundary and extreme values")
    void testBoundaryAndExtremeValues(long seconds, long nanos, String expectedStr) {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(expected, result, () -> String.format("seconds=%d, nanos=%d → expected %s but got %s", seconds, nanos, expected, result));
    }

    @ParameterizedTest(name = "[{index}] seconds={0}, nanos={1} → expected={2}")
    @MethodSource("negativeAndMixedValues")
    @DisplayName("Negative and mixed values")
    void testNegativeAndMixedValues(long seconds, long nanos, String expectedStr) {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(expected, result, () -> String.format("seconds=%d, nanos=%d → expected %s but got %s", seconds, nanos, expected, result));
    }

    @ParameterizedTest(name = "[{index}] seconds={0}, nanos={1} → expected={2}")
    @MethodSource("mixedMagnitudeAndFractionalPrecision")
    @DisplayName("Mixed magnitude and fractional precision")
    void testMixedMagnitudeAndFractionalPrecision(long seconds, long nanos, String expectedStr) {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(seconds, nanos);
        BigDecimal expected = new BigDecimal(expectedStr);
        assertEquals(expected, result, () -> String.format("seconds=%d, nanos=%d → expected %s but got %s", seconds, nanos, expected, result));
    }
}
