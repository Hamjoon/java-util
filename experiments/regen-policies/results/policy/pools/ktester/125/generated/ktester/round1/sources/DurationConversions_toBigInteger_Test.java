package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class DurationConversions_toBigInteger_Test {

    /**
     * The Converter is not used by the implementation, but the method signature requires it.
     */
    @Mock
    private Converter converter;

    static Stream<Arguments> successfulConversionsProvider() {
        return Stream.of(// zero duration
        arguments(Duration.ZERO, "0", "Convert a zero Duration; result should be BigInteger zero."), // positive whole‑second duration (5 seconds)
        arguments(Duration.ofSeconds(5), "5000000000", "Convert a positive whole‑second Duration; 5 seconds = 5×10⁹ ns."), // positive seconds + nanos
        arguments(Duration.ofSeconds(5, 123_456_789L), "5123456789", "Convert a Duration with both seconds and nanoseconds; 5 s + 123,456,789 ns."), // duration created from nanos only
        arguments(Duration.ofNanos(987), "987", "Convert a Duration created from nanoseconds only."), // negative‑second duration with positive nanosecond adjustment
        arguments(Duration.ofSeconds(-3, 500_000_000L), "-2500000000", "Convert a negative‑second Duration with a positive nanosecond adjustment; -3 s + 500 ms = -2.5 s."));
    }

    @Test
    void zeroDurationProducesZeroBigInteger() {
        Duration duration = Duration.ZERO;
        BigInteger result = DurationConversions.toBigInteger(duration, converter);
        assertEquals(BigInteger.ZERO, result, "Converting a zero duration should yield BigInteger.ZERO");
    }

    @Test
    void positiveDurationWithNanosIsConvertedCorrectly() {
        // 5 seconds + 123,456,789 nanoseconds
        Duration duration = Duration.ofSeconds(5, 123_456_789L);
        BigInteger expected = BigInteger.valueOf(5L).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(123_456_789L));
        BigInteger result = DurationConversions.toBigInteger(duration, converter);
        assertEquals(expected, result, "Positive duration conversion did not match the expected nanosecond total");
    }

    @Test
    void negativeDurationWithNanosIsConvertedCorrectly() {
        // -3 seconds - 500,000,000 nanoseconds  (i.e., -3.5 seconds)
        Duration duration = Duration.ofSeconds(-3).minusNanos(500_000_000L);
        BigInteger expected = BigInteger.valueOf(-3L).multiply(BigInteger.valueOf(1_000_000_000L)).subtract(BigInteger.valueOf(500_000_000L));
        BigInteger result = DurationConversions.toBigInteger(duration, converter);
        assertEquals(expected, result, "Negative duration conversion did not match the expected nanosecond total");
    }

    @Test
    void largePositiveSecondsAreHandledWithoutOverflow() {
        // Use Long.MAX_VALUE seconds – this forces the result into a BigInteger
        Duration duration = Duration.ofSeconds(Long.MAX_VALUE, 0L);
        BigInteger expected = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1_000_000_000L));
        BigInteger result = DurationConversions.toBigInteger(duration, converter);
        assertEquals(expected, result, "Conversion of a very large duration should produce the correct BigInteger value");
    }

    @Test
    void largePositiveSecondsWithNanosAreHandledCorrectly() {
        // Long.MAX_VALUE seconds + 999,999,999 nanoseconds
        Duration duration = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999L);
        BigInteger expected = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(999_999_999L));
        BigInteger result = DurationConversions.toBigInteger(duration, converter);
        assertEquals(expected, result, "Conversion of a large duration with nanos should produce the correct BigInteger");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successfulConversionsProvider")
    void successfulConversions(Duration from, String expectedString, String description) {
        BigInteger expected = new BigInteger(expectedString);
        BigInteger result = DurationConversions.toBigInteger(from, converter);
        assertEquals(expected, result, description);
    }

    @Test
    void nullFromArgumentThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> DurationConversions.toBigInteger(null, converter), "Passing a null `from` argument should raise NullPointerException.");
    }

    @Test
    void nonDurationFromArgumentThrowsClassCastException() {
        Object notADuration = "not a duration";
        assertThrows(ClassCastException.class, () -> DurationConversions.toBigInteger(notADuration, converter), "Passing an object that is not a Duration should cause a ClassCastException.");
    }

    @Test
    void maxPositiveDurationIsHandledCorrectly() {
        // Long.MAX_VALUE seconds + 999,999,999 nanoseconds
        Duration duration = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999L);
        BigInteger expected = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(999_999_999L));
        BigInteger result = DurationConversions.toBigInteger(duration, converter);
        assertEquals(expected, result, "Maximum positive Duration (Long.MAX_VALUE seconds + 999,999,999 ns) should be converted correctly.");
    }

    @Test
    void maxNegativeDurationIsHandledCorrectly() {
        // Long.MIN_VALUE seconds - 999,999,999 nanoseconds
        Duration duration = Duration.ofSeconds(Long.MIN_VALUE, -999_999_999L);
        BigInteger expected = BigInteger.valueOf(Long.MIN_VALUE).multiply(BigInteger.valueOf(1_000_000_000L)).subtract(BigInteger.valueOf(999_999_999L));
        BigInteger result = DurationConversions.toBigInteger(duration, converter);
        assertEquals(expected, result, "Maximum negative Duration (Long.MIN_VALUE seconds - 999,999,999 ns) should be converted correctly.");
    }

    @Test
    void negativeSecondsAndNegativeNanosAreConvertedCorrectly() {
        // -5 seconds - 250,000,000 ns = -5.25 seconds
        Duration duration = Duration.ofSeconds(-5, -250_000_000L);
        BigInteger expected = BigInteger.valueOf(-5L).multiply(BigInteger.valueOf(1_000_000_000L)).subtract(BigInteger.valueOf(250_000_000L));
        BigInteger result = DurationConversions.toBigInteger(duration, converter);
        assertEquals(expected, result, "Duration with negative seconds and a negative nanosecond adjustment should be converted correctly.");
    }

    @Test
    void durationOfNegativeNanosIsConvertedCorrectly() {
        // Duration.ofNanos(-500) normalises internally but still equals -500 ns
        Duration duration = Duration.ofNanos(-500);
        BigInteger expected = BigInteger.valueOf(-500L);
        BigInteger result = DurationConversions.toBigInteger(duration, converter);
        assertEquals(expected, result, "Duration created from a negative nanosecond count should be converted correctly.");
    }

    @Test
    void nonNullConverterIsIgnored() {
        // Create a mock that would throw on any interaction – the method must not call it.
        Converter explodingConverter = org.mockito.Mockito.mock(Converter.class, invocation -> {
            throw new UnsupportedOperationException("Converter should not be used");
        });
        // 2.3 seconds
        Duration duration = Duration.ofSeconds(2, 300_000_000L);
        BigInteger expected = BigInteger.valueOf(2L).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(300_000_000L));
        BigInteger result = DurationConversions.toBigInteger(duration, explodingConverter);
        assertEquals(expected, result, "Method should ignore the provided Converter and compute the correct nanosecond total.");
    }
}
