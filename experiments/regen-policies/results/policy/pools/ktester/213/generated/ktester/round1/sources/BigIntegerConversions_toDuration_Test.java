package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import java.time.Duration;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toDuration_Test {

    /**
     * The Converter dependency required by the method under test.
     */
    @Mock
    private Converter // Mockito will initialise this mock before each test
    converter;

    private static Stream<Arguments> provideNormalAndBoundaryValues() {
        return Stream.of(// zero
        arguments(BigInteger.valueOf(0), Duration.ZERO, "Zero nanoseconds should produce a Duration of zero seconds and zero nanos."), // sub‑second
        arguments(BigInteger.valueOf(500_000_000L), Duration.ofSeconds(0, 500_000_000L), "Nanoseconds less than one second should yield seconds=0 and nanos=500,000,000."), // exactly one second
        arguments(BigInteger.valueOf(1_000_000_000L), Duration.ofSeconds(1, 0), "Exactly one second in nanoseconds should produce seconds=1 and nanos=0."), // larger than one second with remainder
        arguments(BigInteger.valueOf(3_456_789_012L), Duration.ofSeconds(3, 456_789_012L), "A value larger than one second with a remainder should be split correctly."), // very large value (beyond long seconds range but still fits into Duration)
        arguments(new BigInteger("12345678901234567890"), Duration.ofSeconds(12_345_678_901L, 234_567_890), "Very large nanosecond count should be handled without overflow, using long seconds and int nanos."), // negative sub‑second
        arguments(BigInteger.valueOf(-500_000_000L), Duration.ofSeconds(0, -500_000_000L), "Negative nanoseconds less than a second should produce a negative nanoAdjustment; Duration normalises it."), // negative crossing a second boundary
        arguments(BigInteger.valueOf(-1_500_000_000L), Duration.ofSeconds(-2, 500_000_000L), "Negative nanoseconds crossing a second boundary should be split into seconds=-2 and positive nanos=500,000,000 after normalisation."), // edge: one nanosecond less than a full second
        arguments(BigInteger.valueOf(999_999_999L), Duration.ofSeconds(0, 999_999_999L), "One nanosecond less than a full second should stay within the same second with nanos = 999,999,999."), // edge: negative just shy of -1 second
        arguments(BigInteger.valueOf(-999_999_999L), Duration.ofSeconds(-1, 1L), "Negative nanoseconds just shy of -1 second are normalised to -1 second + 1 nanosecond."), // edge: negative slightly beyond -1 second
        arguments(BigInteger.valueOf(-1_000_000_001L), Duration.ofSeconds(-2, 999_999_999L), "Negative nanoseconds slightly beyond -1 second should be normalised to -2 seconds + 999,999,999 nanos."), // extreme positive: Long.MAX_VALUE seconds, zero nanos
        arguments(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)), Duration.ofSeconds(Long.MAX_VALUE, 0), "Maximum positive seconds (Long.MAX_VALUE) with zero nanoseconds should be represented correctly."), // extreme positive with remainder
        arguments(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(123_456_789L)), Duration.ofSeconds(Long.MAX_VALUE, 123_456_789L), "Maximum positive seconds with a positive nanosecond remainder should be split into seconds and nanos without overflow."), // extreme negative: Long.MIN_VALUE seconds, zero nanos
        arguments(BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)), Duration.ofSeconds(Long.MIN_VALUE, 0), "Minimum negative seconds (Long.MIN_VALUE) with zero nanoseconds should be represented correctly."), // extreme negative crossing a second boundary
        arguments(BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)).subtract(BigInteger.valueOf(500_000_000L)), Duration.ofSeconds(Long.MIN_VALUE - 1, 500_000_000L), "Negative nanoseconds that cross a second boundary should be normalised to one less second and a positive nanosecond part."));
    }

    @Test
    @DisplayName("Zero nanoseconds → Duration.ZERO")
    void testZeroNanoseconds() {
        // given
        BigInteger nanoseconds = BigInteger.ZERO;
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, converter);
        // then
        assertEquals(Duration.ZERO, result, "Duration for zero nanoseconds should be ZERO");
    }

    @Test
    @DisplayName("Sub‑second nanoseconds are converted correctly")
    void testSubSecondNanoseconds() {
        // given: 0.5 seconds = 500,000,000 nanoseconds
        BigInteger nanoseconds = BigInteger.valueOf(500_000_000L);
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, converter);
        // then
        Duration expected = Duration.ofNanos(500_000_000L);
        assertEquals(expected, result, "Duration should represent 0.5 seconds");
    }

    @Test
    @DisplayName("Nanoseconds spanning whole seconds are split correctly")
    void testNanosecondsWithWholeSeconds() {
        // given: 1.5 seconds = 1,500,000,000 nanoseconds
        BigInteger nanoseconds = BigInteger.valueOf(1_500_000_000L);
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, converter);
        // then
        Duration expected = Duration.ofSeconds(1, 500_000_000L);
        assertEquals(expected, result, "Duration should be 1 second and 500,000,000 nanos");
    }

    @Test
    @DisplayName("Large but still within long range values are handled")
    void testLargeNanoseconds() {
        // given: 2 seconds = 2,000,000,000 nanoseconds
        BigInteger nanoseconds = BigInteger.valueOf(2_000_000_000L);
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, converter);
        // then
        Duration expected = Duration.ofSeconds(2, 0);
        assertEquals(expected, result, "Duration should be exactly 2 seconds");
    }

    @Test
    @DisplayName("Negative nanoseconds are converted correctly")
    void testNegativeNanoseconds() {
        // given: -0.75 seconds = -750,000,000 nanoseconds
        BigInteger nanoseconds = BigInteger.valueOf(-750_000_000L);
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, converter);
        // then
        Duration expected = Duration.ofSeconds(0, -750_000_000L);
        assertEquals(expected, result, "Duration should represent -0.75 seconds");
    }

    @Test
    @DisplayName("Converter argument is not used – passing null works")
    void testNullConverter() {
        // given
        BigInteger nanoseconds = BigInteger.valueOf(123_456_789L);
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, null);
        // then
        Duration expected = Duration.ofNanos(123_456_789L);
        assertEquals(expected, result, "Duration should be based solely on the BigInteger value");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideNormalAndBoundaryValues")
    @DisplayName("toDuration – normal and boundary values")
    void testToDurationNormalAndBoundary(BigInteger from, Duration expected, String description) {
        // when
        Duration result = BigIntegerConversions.toDuration(from, converter);
        // then
        assertEquals(expected, result, description);
    }

    @Test
    @DisplayName("Null 'from' argument should throw NullPointerException")
    void testNullFromThrowsNPE() {
        // given
        BigInteger from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toDuration(from, converter), "Passing a null 'from' argument should cause a NullPointerException.");
    }

    @Test
    @DisplayName("Non‑BigInteger 'from' argument should throw ClassCastException")
    void testInvalidTypeThrowsCCE() {
        // given
        // not a BigInteger
        Object from = Integer.valueOf(42);
        // when / then
        assertThrows(ClassCastException.class, () -> BigIntegerConversions.toDuration(from, converter), "Providing an object that is not a BigInteger should result in a ClassCastException.");
    }

    @Test
    @DisplayName("Converter argument is ignored – passing null works")
    void testNullConverterIsIgnored() {
        // given
        BigInteger nanoseconds = BigInteger.valueOf(123_456_789L);
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, null);
        // then
        Duration expected = Duration.ofNanos(123_456_789L);
        assertEquals(expected, result, "Duration should be based solely on the BigInteger value.");
    }
}
