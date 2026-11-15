package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.math.BigInteger;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.time.DateTimeException;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toInstant_Test {

    /**
     * Mocked {@link Converter} – not used by the method under test but required for the signature.
     */
    @Mock
    Converter mockConverter;

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// zero nanoseconds → epoch
        Arguments.of("Convert zero nanoseconds to Instant (epoch start)", BigInteger.ZERO, null, Instant.ofEpochSecond(0, 0)), // 1.5 seconds → 1 sec + 500_000_000 nanos
        Arguments.of("Nanoseconds greater than one second: 1.5 seconds should yield 1 second and 500,000,000 nanos", BigInteger.valueOf(1_500_000_000L), null, Instant.ofEpochSecond(1, 500_000_000)), // -2.5 seconds → -3 seconds + 750_000_000 nanos (Java normalises the remainder)
        Arguments.of("Negative nanoseconds: -2.5 seconds should be normalised to -3 seconds and 750,000,000 nanos", new BigInteger("-2500000000"), null, Instant.ofEpochSecond(-3, 750_000_000)), // Long.MAX_VALUE seconds, zero nanos
        Arguments.of("Maximum long seconds with zero nanos; tests overflow handling of the seconds part", BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)), null, Instant.ofEpochSecond(Long.MAX_VALUE, 0)), // Long.MIN_VALUE seconds, positive remainder
        Arguments.of("Minimum long seconds with a positive nanosecond remainder", BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(123_456_789)), null, Instant.ofEpochSecond(Long.MIN_VALUE, 123_456_789)), // Just below one second
        Arguments.of("Nanoseconds just below one second; should not increment the seconds part", BigInteger.valueOf(999_999_999L), null, Instant.ofEpochSecond(0, 999_999_999)), // Exact one second
        Arguments.of("Exact one‑second nanoseconds; remainder should be zero", BigInteger.valueOf(1_000_000_000L), null, Instant.ofEpochSecond(1, 0)), // Exact negative one second
        Arguments.of("Exact negative one‑second nanoseconds should produce -1 second and 0 nanos", BigInteger.valueOf(-1_000_000_000L), null, Instant.ofEpochSecond(-1, 0)), // Two seconds plus one nanosecond
        Arguments.of("Two seconds plus one nanosecond; verifies correct split into seconds and nanos", new BigInteger("2000000001"), null, Instant.ofEpochSecond(2, 1)), // Non‑null converter is ignored
        Arguments.of("A non‑null Converter is ignored by the method; conversion should still succeed", BigInteger.valueOf(3_000_000_000L), mock(Converter.class), Instant.ofEpochSecond(3, 0)));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Overflow: seconds exceed Long.MAX_VALUE after conversion
        Arguments.of("Seconds exceed Long.MAX_VALUE after conversion; Instant.ofEpochSecond should throw DateTimeException", // (Long.MAX_VALUE + 1) * 1_000_000_000
        BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE).multiply(BigInteger.valueOf(1_000_000_000L)), null, DateTimeException.class), // Underflow: seconds less than Long.MIN_VALUE after conversion
        Arguments.of("Seconds less than Long.MIN_VALUE after conversion; Instant.ofEpochSecond should throw DateTimeException", // (Long.MIN_VALUE - 1) * 1_000_000_000
        BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE).multiply(BigInteger.valueOf(1_000_000_000L)), null, DateTimeException.class), // Null 'from' argument → NullPointerException when casting
        Arguments.of("Null 'from' argument should cause a NullPointerException when cast to BigInteger", null, null, NullPointerException.class), // Non‑BigInteger argument → ClassCastException
        Arguments.of("Passing a non‑BigInteger object should result in a ClassCastException", "notABigInteger", null, ClassCastException.class));
    }

    @Test
    void toInstant_zeroNanoseconds_returnsEpoch() {
        // GIVEN
        BigInteger nanoseconds = BigInteger.ZERO;
        // WHEN
        Instant actual = BigIntegerConversions.toInstant(nanoseconds, mockConverter);
        // THEN
        assertEquals(Instant.ofEpochSecond(0, 0), actual, "Zero nanoseconds should map to the epoch instant");
    }

    @Test
    void toInstant_secondsOnly_returnsCorrectInstant() {
        // GIVEN: 5 seconds expressed in nanoseconds (5 * 1_000_000_000)
        BigInteger nanoseconds = BigInteger.valueOf(5L).multiply(BigInteger.valueOf(1_000_000_000L));
        // WHEN
        Instant actual = BigIntegerConversions.toInstant(nanoseconds, mockConverter);
        // THEN
        assertEquals(Instant.ofEpochSecond(5, 0), actual, "5 seconds in nanoseconds should map to an instant at epoch+5s");
    }

    @Test
    void toInstant_secondsAndNanos_returnsCorrectInstant() {
        // GIVEN: 3 seconds and 123,456,789 nanoseconds
        long seconds = 3L;
        int nanos = 123_456_789;
        BigInteger totalNanos = BigInteger.valueOf(seconds).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(nanos));
        // WHEN
        Instant actual = BigIntegerConversions.toInstant(totalNanos, mockConverter);
        // THEN
        assertEquals(Instant.ofEpochSecond(seconds, nanos), actual, "Combined seconds and nanoseconds should be interpreted correctly");
    }

    @Test
    void toInstant_largeValueBeyondIntRange_returnsCorrectInstant() {
        // GIVEN: 2^31 seconds (just over the int range) and 500 nanoseconds
        // 2,147,483,648
        long seconds = (1L << 31);
        int nanos = 500;
        BigInteger totalNanos = BigInteger.valueOf(seconds).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(nanos));
        // WHEN
        Instant actual = BigIntegerConversions.toInstant(totalNanos, mockConverter);
        // THEN
        assertEquals(Instant.ofEpochSecond(seconds, nanos), actual, "Large second values must be handled without overflow");
    }

    @Test
    void toInstant_negativeNanoseconds_returnsCorrectInstant() {
        // GIVEN: -2 seconds and -300 nanoseconds (i.e., -2.000000300 seconds)
        // The implementation uses divideAndRemainder which yields a negative remainder,
        // but Instant.ofEpochSecond handles negative nanos correctly.
        long seconds = -2L;
        int nanos = -300;
        BigInteger totalNanos = BigInteger.valueOf(seconds).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(nanos));
        // WHEN
        Instant actual = BigIntegerConversions.toInstant(totalNanos, mockConverter);
        // THEN
        assertEquals(Instant.ofEpochSecond(seconds, nanos), actual, "Negative nanoseconds should produce a correctly signed instant");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionProvider")
    void toInstant_successfulConversions(String description, Object from, Converter converter, Instant expected) {
        // WHEN
        Instant actual = BigIntegerConversions.toInstant(from, converter);
        // THEN
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void toInstant_expectedExceptions(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> BigIntegerConversions.toInstant(from, converter), description);
    }
}
