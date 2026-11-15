package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.Duration;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toBigInteger_Test {

    @Mock
    private Converter // Converter is not used by the method, a mock is sufficient.
    mockConverter;

    /**
     * Simple holder for a test case's parameters and expected result.
     */
    private static final class ArgumentsHolder {

        final Duration duration;

        // decimal string representation of the expected BigInteger
        final String expected;

        final String description;

        ArgumentsHolder(Duration duration, String expected, String description) {
            this.duration = duration;
            this.expected = expected;
            this.description = description;
        }
    }

    /**
     * Test data for the "Standard conversions" group.
     */
    private static Stream<ArgumentsHolder> standardConversions() {
        return Stream.of(new ArgumentsHolder(Duration.ofSeconds(5, 300), "5000000300", "Convert a positive Duration of 5 seconds and 300 nanoseconds to total nanoseconds."), new ArgumentsHolder(Duration.ofSeconds(0, 0), "0", "Convert a zero Duration to zero nanoseconds."), new ArgumentsHolder(// Duration normalises the sign, so -2 seconds + 500 nanos becomes -1.9999995 seconds
        Duration.ofSeconds(-2, 500), "-1999999500", "Convert a negative Duration with positive nanosecond part."));
    }

    /**
     * Test data for the "Edge cases" group.
     */
    private static Stream<ArgumentsHolder> edgeCases() {
        return Stream.of(new ArgumentsHolder(Duration.ofSeconds(Long.MAX_VALUE, 999_999_999L), // (Long.MAX_VALUE * 1_000_000_000) + 999_999_999
        "9223372036854775807000000000999999999", "Convert the maximum long seconds plus max nanosecond fraction; result exceeds long range."), new ArgumentsHolder(Duration.ofSeconds(Long.MIN_VALUE, 0), "-9223372036854775808000000000", "Convert the minimum long seconds (no nanoseconds) to verify handling of large negative values."));
    }

    /**
     * Test data for the "Additional conversions" group.
     */
    private static Stream<ArgumentsHolder> additionalConversions() {
        return Stream.of(new ArgumentsHolder(Duration.ofSeconds(0, 123_456_789L), "123456789", "Duration with zero seconds and a positive nanosecond part should return the same nanoseconds."), new ArgumentsHolder(Duration.ofSeconds(1, 0), "1000000000", "One whole second should be converted to exactly one billion nanoseconds."), new ArgumentsHolder(Duration.ofSeconds(-1, 0), "-1000000000", "Negative one second should be represented as negative one billion nanoseconds."), new ArgumentsHolder(// Duration.ofSeconds normalises negative nanos, so this yields -5_000_000_300
        Duration.ofSeconds(-5, -300), "-5000000300", "Negative seconds with a negative nanosecond adjustment are normalized; total nanoseconds should be -5 000 000 300."), new ArgumentsHolder(Duration.ofSeconds(0, -500), "-500", "Zero seconds with a negative nanosecond adjustment normalizes to -500 nanoseconds."), new ArgumentsHolder(Duration.ofSeconds(Long.MAX_VALUE, 0), "9223372036854775807000000000", "Maximum long seconds without any nanosecond fraction should be correctly multiplied by one billion."));
    }

    /**
     * Test data for the "Converter parameter handling" group.
     */
    private static Stream<ArgumentsHolder> converterHandling() {
        return Stream.of(new ArgumentsHolder(Duration.ofSeconds(2, 250), "2000000250", "A non‑null Converter instance is ignored by the method; conversion should still produce correct nanoseconds."));
    }

    /**
     * Test data for the "Parsing based Duration" group.
     */
    private static Stream<ArgumentsHolder> parsingBased() {
        return Stream.of(new ArgumentsHolder(Duration.parse("PT0.000000001S"), "1", "Duration parsed from an ISO‑8601 string representing one nanosecond should return 1."), new ArgumentsHolder(Duration.parse("PT1.5S"), "1500000000", "Parsing a duration of 1.5 seconds should yield 1 500 000 000 nanoseconds."));
    }

    @Test
    void toBigInteger_zeroDuration_returnsZero() {
        // GIVEN
        Duration duration = Duration.ZERO;
        // WHEN
        BigInteger result = DurationConversions.toBigInteger(duration, mockConverter);
        // THEN
        assertEquals(BigInteger.ZERO, result, "Zero duration should convert to BigInteger zero");
    }

    @Test
    void toBigInteger_positiveDuration_returnsCorrectNanoseconds() {
        // GIVEN
        // 5 seconds and 123,456,789 nanoseconds
        Duration duration = Duration.ofSeconds(5, 123_456_789L);
        // Expected nanoseconds = 5 * 1_000_000_000 + 123_456_789
        BigInteger expected = BigInteger.valueOf(5L).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(123_456_789L));
        // WHEN
        BigInteger result = DurationConversions.toBigInteger(duration, mockConverter);
        // THEN
        assertEquals(expected, result, "Positive duration should be converted to total nanoseconds");
    }

    @Test
    void toBigInteger_negativeDuration_returnsCorrectNanoseconds() {
        // GIVEN
        // -3 seconds and -500,000,000 nanoseconds (i.e., -3.5 seconds)
        // Duration normalises the representation, so we build it via minusNanos.
        Duration duration = Duration.ofSeconds(-3).minusNanos(500_000_000L);
        // Compute expected value using the same logic as the implementation.
        BigInteger expected = BigInteger.valueOf(duration.getSeconds()).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(duration.getNano()));
        // WHEN
        BigInteger result = DurationConversions.toBigInteger(duration, mockConverter);
        // THEN
        assertEquals(expected, result, "Negative duration should be converted to total nanoseconds");
    }

    @Test
    void toBigInteger_largeDuration_handlesBigValues() {
        // GIVEN
        // 2^31 seconds (about 68 years) and 999,999,999 nanoseconds
        // 2,147,483,648
        long seconds = 1L << 31;
        Duration duration = Duration.ofSeconds(seconds, 999_999_999L);
        // Expected = seconds * 1_000_000_000 + 999_999_999
        BigInteger expected = BigInteger.valueOf(seconds).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(999_999_999L));
        // WHEN
        BigInteger result = DurationConversions.toBigInteger(duration, mockConverter);
        // THEN
        assertEquals(expected, result, "Large duration should be correctly converted to a BigInteger");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource({ "standardConversions", "edgeCases", "additionalConversions", "converterHandling", "parsingBased" })
    void toBigInteger_variousInputs(ArgumentsHolder holder) {
        // WHEN
        BigInteger result = DurationConversions.toBigInteger(holder.duration, mockConverter);
        // THEN
        assertEquals(new BigInteger(holder.expected), result, () -> "Failed for case: " + holder.description);
    }

    @Test
    void toBigInteger_nullFrom_throwsNullPointerException() {
        // GIVEN
        Object nullDuration = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DurationConversions.toBigInteger(nullDuration, mockConverter), "Passing null as the source object should result in NullPointerException.");
    }

    @Test
    void toBigInteger_nonDurationObject_throwsClassCastException() {
        // GIVEN
        Object notADuration = "not a duration";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DurationConversions.toBigInteger(notADuration, mockConverter), "Passing an object that is not a Duration should cause a ClassCastException.");
    }
}
