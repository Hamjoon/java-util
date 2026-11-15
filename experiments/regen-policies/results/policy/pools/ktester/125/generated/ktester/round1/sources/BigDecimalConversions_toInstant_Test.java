package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.time.DateTimeException;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toInstant_Test {

    @Mock
    Converter // not used by toInstant, but required by the signature
    converter;

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// typical positive value with fractional seconds
        arguments(new BigDecimal("123.456789012"), "1970-01-01T00:02:03.456789012Z", "Converts a positive BigDecimal containing seconds and nanoseconds to the correct Instant."), // zero seconds
        arguments(BigDecimal.ZERO, "1970-01-01T00:00:00Z", "Zero BigDecimal should map to the epoch Instant."), // negative value with fractional part
        arguments(new BigDecimal("-1.5"), "1969-12-31T23:59:58.500000000Z", "Negative BigDecimal: -1.5 seconds is normalized to -2 seconds + 500 000 000 ns."), // fractional part with leading zeros (1 ns)
        arguments(new BigDecimal("1.000000001"), "1970-01-01T00:00:01.000000001Z", "Ensures that a fractional part that starts with many zeros is correctly scaled to nanoseconds (1 ns)."), // maximum long seconds (Long.MAX_VALUE)
        arguments(new BigDecimal("9223372036854775807"), "292278994-08-17T07:12:55.807Z", "BigDecimal equal to Long.MAX_VALUE seconds should produce the far‑future Instant supported by java.time."), // converter argument is null (method does not use it)
        arguments(new BigDecimal("10.25"), "1970-01-01T00:00:10.250000000Z", "Even when the Converter argument is null, the method should still succeed because it does not use the converter."), // fractional part exceeding nanosecond precision (truncation)
        arguments(new BigDecimal("1.123456789123"), "1970-01-01T00:00:01.123456789Z", "Fractional part has more than 9 digits; extra digits are truncated."), // negative fractional part exceeding nanosecond precision
        arguments(new BigDecimal("-2.987654321987"), "1969-12-31T23:59:57.012345679Z", "Negative value with >9 fractional digits; remainder is normalized correctly."), // maximum nanosecond value
        arguments(new BigDecimal("0.999999999"), "1970-01-01T00:00:00.999999999Z", "Fractional part exactly 0.999999999 seconds should map to the maximum 9‑digit nanosecond value."), // negative zero representation
        arguments(new BigDecimal("-0.0"), "1970-01-01T00:00:00Z", "Negative zero should be treated the same as zero, yielding the epoch instant."), // integer value with a non‑zero scale
        arguments(new BigDecimal("5.0"), "1970-01-01T00:00:05Z", "A BigDecimal that is mathematically an integer but has a non‑zero scale should still produce an exact second instant."), // exponent notation (scientific)
        arguments(new BigDecimal("1E3"), "1970-01-01T00:16:40Z", "BigDecimal expressed in scientific notation (1000 seconds) must be correctly interpreted."), // fractional part smaller than 1 ns (truncates to zero)
        arguments(new BigDecimal("0.0000000005"), "1970-01-01T00:00:00Z", "Fractional part less than one nanosecond is truncated to zero nanoseconds."));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// null source object
        arguments(null, NullPointerException.class, "Method casts the source to BigDecimal; passing null must throw NullPointerException."), // wrong source type (String)
        arguments("123.45", ClassCastException.class, "If the source object is not a BigDecimal (e.g., a String), the cast fails and a ClassCastException is thrown."), // Long.MIN_VALUE seconds – out of range for Instant
        arguments(new BigDecimal("-9223372036854775808"), DateTimeException.class, "Instant.ofEpochSecond(Long.MIN_VALUE) is outside the supported range; the method should propagate a DateTimeException."), // seconds overflow beyond Long.MAX_VALUE
        arguments(new BigDecimal("9223372036854775808"), DateTimeException.class, "Value exceeds Long.MAX_VALUE; after casting to long the epoch second is out of range, causing Instant.ofEpochSecond to throw DateTimeException."));
    }

    @Test
    void toInstant_WithWholeSeconds_ReturnsExactInstant() {
        // given
        // 12345 seconds, no fraction
        BigDecimal source = new BigDecimal("12345");
        // when
        Instant result = BigDecimalConversions.toInstant(source, converter);
        // then
        Instant expected = Instant.ofEpochSecond(12_345L, 0L);
        assertEquals(expected, result, "Instant should match whole‑second value");
    }

    @Test
    void toInstant_WithFractionalSeconds_ReturnsInstantWithNanos() {
        // given
        // 12345 seconds + 0.6789 sec
        BigDecimal source = new BigDecimal("12345.6789");
        // when
        Instant result = BigDecimalConversions.toInstant(source, converter);
        // then
        // 0.6789 sec = 678,900,000 nanoseconds
        Instant expected = Instant.ofEpochSecond(12_345L, 678_900_000L);
        assertEquals(expected, result, "Instant should include correct nanosecond component");
    }

    @Test
    void toInstant_WithNegativeFractionalSeconds_NormalizesCorrectly() {
        // given
        // -1.5 seconds
        BigDecimal source = new BigDecimal("-1.5");
        // when
        Instant result = BigDecimalConversions.toInstant(source, converter);
        // then
        // -1.5 seconds = -2 seconds + 0.5 seconds (500,000,000 ns)
        Instant expected = Instant.ofEpochSecond(-2L, 500_000_000L);
        assertEquals(expected, result, "Negative fractional seconds should be normalized");
    }

    @Test
    void toInstant_WithZero_ReturnsEpoch() {
        // given
        BigDecimal source = BigDecimal.ZERO;
        // when
        Instant result = BigDecimalConversions.toInstant(source, converter);
        // then
        assertEquals(Instant.EPOCH, result, "Zero should map to the epoch instant");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toInstant_ValidConversions(Object from, String expectedIso, String description) {
        // given
        // (the 'from' object is already prepared by the provider)
        // when
        // converter not required
        Instant result = BigDecimalConversions.toInstant(from, null);
        // then
        Instant expected = Instant.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidConversionProvider")
    void toInstant_InvalidConversions(Object from, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> BigDecimalConversions.toInstant(from, null), description);
    }
}
