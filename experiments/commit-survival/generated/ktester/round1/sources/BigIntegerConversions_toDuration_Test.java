package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.time.DateTimeException;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toDuration_Test {

    @Mock
    private Converter // not used by the method but required by signature
    converter;

    private static Stream<Arguments> errorHandlingProvider() {
        return Stream.of(arguments("Passing null as the source object should cause NullPointerException", null, null, NullPointerException.class), arguments("Providing a non‑BigInteger object should cause ClassCastException", Integer.valueOf(42), null, ClassCastException.class));
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// Zero nanoseconds
        arguments("Zero nanoseconds should produce a Duration of zero seconds and zero nanos.", BigInteger.valueOf(0), Duration.ZERO), // Exactly one second
        arguments("Exactly one second expressed in nanoseconds should yield a Duration of 1 second and 0 nanos.", BigInteger.valueOf(1_000_000_000L), Duration.ofSeconds(1, 0L)), // One and a half seconds
        arguments("One and a half seconds in nanoseconds should be split into 1 second and 500,000,000 nanos.", BigInteger.valueOf(1_500_000_000L), Duration.ofSeconds(1, 500_000_000L)), // Negative less than one second
        arguments("Negative nanoseconds less than one second should produce a Duration with zero seconds and negative nanos.", new BigInteger("-500000000"), Duration.ofSeconds(0, -500_000_000L)), // Negative more than one second (normalisation)
        arguments("Negative nanoseconds spanning more than one second should be normalised: -1.5 s => -2 s + 500 000 000 ns.", new BigInteger("-1500000000"), Duration.ofSeconds(-2, 500_000_000L)), // Long.MAX_VALUE nanoseconds (edge positive)
        arguments("Maximum positive long nanoseconds should be correctly split into seconds and nanos without overflow.", new BigInteger("9223372036854775807"), Duration.ofSeconds(9_223_372_036L, 854_775_807L)), // Long.MIN_VALUE nanoseconds (edge negative)
        arguments("Maximum negative long nanoseconds should be correctly split, demonstrating handling of edge negative values.", new BigInteger("-9223372036854775808"), Duration.ofSeconds(-9_223_372_037L, 145_224_193L)));
    }

    private static Stream<Arguments> overflowProvider() {
        // (Long.MAX_VALUE + 1) seconds expressed in nanoseconds
        BigInteger oneMoreSecond = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE).multiply(BigIntegerConversions.BILLION);
        // (Long.MIN_VALUE - 1) seconds expressed in nanoseconds
        BigInteger oneLessSecond = BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE).multiply(BigIntegerConversions.BILLION);
        return Stream.of(arguments("Nanoseconds representing (Long.MAX_VALUE + 1) seconds overflow the Duration range and should trigger a DateTimeException.", oneMoreSecond), arguments("Nanoseconds representing (Long.MIN_VALUE - 1) seconds overflow the negative bound of Duration and should trigger a DateTimeException.", oneLessSecond));
    }

    @Test
    void zeroNanosecondsReturnsZeroDuration() {
        // given
        BigInteger nanoseconds = BigInteger.ZERO;
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, converter);
        // then
        assertEquals(Duration.ZERO, result);
    }

    @Test
    void lessThanOneSecondNanoseconds() {
        // given: 500,000,000 nanoseconds = 0.5 seconds
        BigInteger nanoseconds = BigInteger.valueOf(500_000_000L);
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, converter);
        // then
        assertEquals(Duration.ofSeconds(0, 500_000_000L), result);
    }

    @Test
    void exactlyOneSecondNanoseconds() {
        // given: 1,000,000,000 nanoseconds = 1 second
        BigInteger nanoseconds = BigInteger.valueOf(1_000_000_000L);
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, converter);
        // then
        assertEquals(Duration.ofSeconds(1, 0L), result);
    }

    @Test
    void multipleSecondsWithRemainderNanoseconds() {
        // given: 3,500,000,000 nanoseconds = 3 seconds + 500,000,000 nanos
        BigInteger nanoseconds = BigInteger.valueOf(3_500_000_000L);
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, converter);
        // then
        assertEquals(Duration.ofSeconds(3, 500_000_000L), result);
    }

    @Test
    void largeNanosecondsValue() {
        // given: a value larger than Long.MAX_VALUE seconds
        BigInteger nanoseconds = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(2)).multiply(// convert seconds to nanoseconds
        BigIntegerConversions.BILLION).add(// add some nanos
        BigInteger.valueOf(123_456_789L));
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, converter);
        // then
        long expectedSeconds = Long.MAX_VALUE * 2L;
        int expectedNanos = 123_456_789;
        assertEquals(Duration.ofSeconds(expectedSeconds, expectedNanos), result);
    }

    @Test
    void negativeNanosecondsValue() {
        // given: -1,500,000,000 nanoseconds = -2 seconds + 500,000,000 nanos (Java's remainder is negative)
        BigInteger nanoseconds = BigInteger.valueOf(-1_500_000_000L);
        // when
        Duration result = BigIntegerConversions.toDuration(nanoseconds, converter);
        // then
        // Duration.ofSeconds handles negative seconds and positive nanos correctly
        assertEquals(Duration.ofSeconds(-2, 500_000_000L), result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("errorHandlingProvider")
    void errorHandling(String description, Object from, Object conv, Class<? extends Throwable> expectedException) {
        // The converter mock is not used; we pass null when the test case specifies it.
        // may be null
        Converter usedConverter = (Converter) conv;
        assertThrows(expectedException, () -> BigIntegerConversions.toDuration(from, usedConverter), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalConversionProvider")
    void normalConversion(String description, BigInteger from, Duration expected) {
        Duration result = BigIntegerConversions.toDuration(from, converter);
        assertEquals(expected, result, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("overflowProvider")
    void overflowHandling(String description, BigInteger from) {
        // Duration.ofSeconds will throw DateTimeException when the seconds part overflows the long range.
        assertThrows(DateTimeException.class, () -> BigIntegerConversions.toDuration(from, converter), description);
    }
}
