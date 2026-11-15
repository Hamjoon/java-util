package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toInstant_Test {

    /**
     * The Converter is not used by the current implementation of {@code toInstant},
     *  but it is part of the method signature.  A mock is supplied to verify that the
     *  method does not interact with it.
     */
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test to ensure a clean interaction history.
        reset(mockConverter);
    }

    private static Stream<Arguments> validConversions() {
        return Stream.of(Arguments.of(BigInteger.ZERO, Instant.parse("1970-01-01T00:00:00Z"), "Zero nanoseconds should produce the epoch instant."), Arguments.of(BigInteger.valueOf(123_456_789L), Instant.parse("1970-01-01T00:00:00.123456789Z"), "Positive nanoseconds less than one second – seconds part is 0, nanos part is 123,456,789."), Arguments.of(BigInteger.valueOf(3_000_000_000L), Instant.parse("1970-01-01T00:00:03Z"), "Exactly three seconds (no remainder)."), Arguments.of(BigInteger.valueOf(3_500_000_123L), Instant.parse("1970-01-01T00:00:03.500000123Z"), "Three seconds with a nanosecond remainder."), Arguments.of(BigInteger.valueOf(-2_500_000_000L), Instant.parse("1969-12-31T23:59:57.500000000Z"), "Negative nanoseconds – the method must correctly handle sign and produce the instant equivalent to -2 seconds‑500 ms."));
    }

    private static Stream<Arguments> errorHandlingEdgeCases() {
        return Stream.of(// Nanoseconds that translate to epoch seconds greater than Long.MAX_VALUE
        Arguments.of(new BigInteger("9223372036854775808"), DateTimeException.class, "Nanoseconds that translate to epoch seconds greater than Long.MAX_VALUE (Long.MAX_VALUE+1 seconds)."), // Not a BigInteger -> ClassCastException
        Arguments.of("notABigInteger", ClassCastException.class, "Passing a value that is not a BigInteger must result in a ClassCastException."));
    }

    private static Stream<Arguments> boundaryConditions() {
        return Stream.of(// Maximum nanosecond value that fits within a single second
        Arguments.of(BigInteger.valueOf(999_999_999L), Instant.parse("1970-01-01T00:00:00.999999999Z"), "Maximum nanosecond value that fits within a single second; seconds part is 0, nanos part is 999,999,999."), // Single negative nanosecond
        Arguments.of(BigInteger.valueOf(-1L), Instant.parse("1969-12-31T23:59:59.999999999Z"), "Single negative nanosecond; BigInteger division yields 0 seconds and -1 nanosecond, which Instant normalises to the last nanosecond of the previous second."), // Instant.MAX (upper bound)
        Arguments.of(new BigInteger("31556889864403199000000000"), Instant.parse("1000000000-01-01T00:00:00Z"), "Instant at the upper bound of the Java Instant range (Instant.MAX)."), // Instant.MIN (lower bound)
        Arguments.of(new BigInteger("-31557014167219200000000000"), Instant.parse("-1000000000-01-01T00:00:00Z"), "Instant at the lower bound of the Java Instant range (Instant.MIN)."));
    }

    private static Stream<Arguments> additionalErrorHandling() {
        return Stream.of(// Null from argument
        Arguments.of(null, NullPointerException.class, "Passing a null reference for `from` triggers a NullPointerException during the unchecked cast to BigInteger."), // Nanoseconds just beyond Long.MAX_VALUE seconds (different overflow scenario)
        Arguments.of(new BigInteger("9223372036854775808"), DateTimeException.class, "Nanoseconds that translate to epoch seconds just beyond Long.MAX_VALUE cause Instant.ofEpochSecond to throw DateTimeException."));
    }

    @Test
    void toInstant_NanosecondsLessThanOneSecond() {
        // 123,456,789 nanoseconds = 0 seconds + 123,456,789 nanos
        BigInteger nanos = BigInteger.valueOf(123_456_789L);
        Instant result = BigIntegerConversions.toInstant(nanos, mockConverter);
        assertEquals(0L, result.getEpochSecond(), "Seconds part should be zero");
        assertEquals(123_456_789, result.getNano(), "Nanoseconds part should match input");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInstant_NanosecondsMoreThanOneSecond() {
        // 1,500,000,000 nanoseconds = 1 second + 500,000,000 nanos
        BigInteger nanos = BigInteger.valueOf(1_500_000_000L);
        Instant result = BigIntegerConversions.toInstant(nanos, mockConverter);
        assertEquals(1L, result.getEpochSecond(), "Seconds part should be one");
        assertEquals(500_000_000, result.getNano(), "Nanoseconds part should be the remainder");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInstant_ZeroNanoseconds() {
        BigInteger nanos = BigInteger.ZERO;
        Instant result = BigIntegerConversions.toInstant(nanos, mockConverter);
        assertEquals(Instant.EPOCH, result, "Zero nanoseconds must map to the epoch instant");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInstant_NegativeNanoseconds() {
        // -2,500,000,001 nanoseconds = -3 seconds + 499,999,999 nanos
        // Java's divideAndRemainder yields: quotient = -2, remainder = -500,000,001,
        // but Instant.ofEpochSecond normalises the negative nanos.
        BigInteger nanos = BigInteger.valueOf(-2_500_000_001L);
        Instant result = BigIntegerConversions.toInstant(nanos, mockConverter);
        // Expected normalised instant:
        // seconds = -3, nanos = 499_999_999
        assertEquals(-3L, result.getEpochSecond(), "Seconds part should be -3 after normalisation");
        assertEquals(499_999_999, result.getNano(), "Nanoseconds part should be normalised to positive range");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInstant_LargeNanosecondsWithinLongRange() {
        // Use a value that fits into a long after division.
        // 9,223,372,036,854,775,807 (Long.MAX_VALUE) seconds * 1_000_000_000 + 123 nanoseconds
        BigInteger seconds = BigInteger.valueOf(Long.MAX_VALUE);
        BigInteger nanos = seconds.multiply(BigIntegerConversions.BILLION).add(BigInteger.valueOf(123L));
        Instant result = BigIntegerConversions.toInstant(nanos, mockConverter);
        assertEquals(Long.MAX_VALUE, result.getEpochSecond(), "Seconds should equal Long.MAX_VALUE");
        assertEquals(123, result.getNano(), "Nanoseconds should be the remainder");
        verifyNoInteractions(mockConverter);
    }

    /**
     * Group: Valid conversions
     */
    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("validConversions")
    void toInstant_ValidConversions(BigInteger from, Instant expected, String description) {
        Instant result = BigIntegerConversions.toInstant(from, mockConverter);
        assertEquals(expected, result, description);
        verifyNoInteractions(mockConverter);
    }

    /**
     * Group: Error handling / edge cases
     */
    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("errorHandlingEdgeCases")
    void toInstant_ErrorHandling(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> BigIntegerConversions.toInstant(from, mockConverter), description);
        verifyNoInteractions(mockConverter);
    }

    /**
     * Group: Boundary conditions
     */
    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("boundaryConditions")
    void toInstant_BoundaryConditions(BigInteger from, Instant expected, String description) {
        Instant result = BigIntegerConversions.toInstant(from, mockConverter);
        assertEquals(expected, result, description);
        verifyNoInteractions(mockConverter);
    }

    /**
     * Group: Additional error handling
     */
    @ParameterizedTest(name = "{index} ⇒ {2}")
    @MethodSource("additionalErrorHandling")
    void toInstant_AdditionalErrorHandling(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> BigIntegerConversions.toInstant(from, mockConverter), description);
        verifyNoInteractions(mockConverter);
    }

    /**
     * Group: Converter parameter ignored
     */
    @Test
    void toInstant_ConverterParameterIgnored() {
        BigInteger from = BigInteger.valueOf(123_456_789L);
        Instant result = BigIntegerConversions.toInstant(from, mockConverter);
        assertEquals(Instant.parse("1970-01-01T00:00:00.123456789Z"), result, "A non‑null Converter instance is supplied; the method does not use it, so the conversion result must be identical to the standard case.");
        verifyNoInteractions(mockConverter);
    }

    /**
     * Group: Underflow seconds
     */
    @Test
    void toInstant_UnderflowSeconds() {
        // Value just below Instant.MIN seconds * 1_000_000_000
        BigInteger underflow = new BigInteger("-31557014167219200000000001");
        assertThrows(DateTimeException.class, () -> BigIntegerConversions.toInstant(underflow, mockConverter), "Nanoseconds that translate to epoch seconds less than Long.MIN_VALUE cause Instant.ofEpochSecond to throw DateTimeException.");
        verifyNoInteractions(mockConverter);
    }
}
