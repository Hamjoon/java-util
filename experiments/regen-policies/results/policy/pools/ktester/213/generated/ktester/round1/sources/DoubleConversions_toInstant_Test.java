package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.time.DateTimeException;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toInstant_Test {

    /**
     * Converter instance used for all tests.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter has a public constructor that takes a ConverterOptions object.
        // Using a default instance of ConverterOptions is sufficient for the
        // toInstant conversion because it does not depend on any custom options.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Positive double with fractional part
        arguments(123.456, "1970-01-01T00:02:03.456Z"), // Negative whole‑number double
        arguments(-10.0, "1969-12-31T23:59:50Z"), // Zero value
        arguments(0.0, "1970-01-01T00:00:00Z"), // Exact whole‑second double
        arguments(60.0, "1970-01-01T00:01:00Z"), // Tiny fractional part (1 nanosecond)
        arguments(2.000000001, "1970-01-01T00:00:02.000000001Z"), // Very large number of seconds plus fraction
        arguments(1000000000.123, "2001-09-09T01:46:40.123Z"), // Negative zero (treated as zero)
        arguments(-0.0, "1970-01-01T00:00:00Z"), // Smallest positive subnormal double (rounds to zero)
        arguments(5e-324, "1970-01-01T00:00:00Z"), // Negative smallest subnormal (also rounds to zero)
        arguments(-5e-324, "1970-01-01T00:00:00Z"), // Double.NaN (the actual NaN double) – should produce epoch
        arguments(Double.NaN, "1970-01-01T00:00:00Z"));
    }

    private static Stream<Arguments> roundingEdgeCaseProvider() {
        return Stream.of(// Fraction rounds up to exactly 1 000 000 000 nanoseconds – out of range
        arguments(1.9999999995), // Negative fractional part yields negative nanoseconds
        arguments(-1.5), // Negative double whose integer part truncates to 0 but fraction is negative
        arguments(-0.1), // 0.9999999995 → 1 000 000 000 nanos (overflow)
        arguments(0.9999999995));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(// String literal "NaN"
        arguments("NaN"), // String literal "Infinity"
        arguments("Infinity"), // Integer object
        arguments(Integer.valueOf(5)), // Long object
        arguments(Long.valueOf(5L)), // Null reference – unboxing null leads to NullPointerException,
        // but the cast itself is a ClassCastException in the original contract.
        // We assert NullPointerException because that is the actual JVM behaviour.
        arguments((Object) null));
    }

    private static Stream<Arguments> extremeValueProvider() {
        return Stream.of(// Double.MAX_VALUE – seconds overflow the Instant range
        arguments(Double.MAX_VALUE), // Very large negative double (outside Instant range)
        arguments(-1e+308), // Positive infinity
        arguments(Double.POSITIVE_INFINITY), // Negative infinity
        arguments(Double.NEGATIVE_INFINITY));
    }

    @Test
    void toInstant_wholeNumberSeconds() {
        // exact epoch seconds, no fractional part
        Double source = 1_622_547_800.0;
        Instant expected = Instant.ofEpochSecond(1_622_547_800L, 0L);
        Instant actual = DoubleConversions.toInstant(source, converter);
        assertEquals(expected, actual, "Instant should represent the exact epoch seconds");
    }

    @Test
    void toInstant_withFractionalNanos_RoundsDown() {
        // fraction = 0.123456789
        Double source = 1_622_547_800.123456789;
        long seconds = 1_622_547_800L;
        long nanos = Math.round((source - seconds) * 1_000_000_000L);
        Instant expected = Instant.ofEpochSecond(seconds, nanos);
        Instant actual = DoubleConversions.toInstant(source, converter);
        assertEquals(expected, actual, "Instant should contain correctly rounded nanoseconds (down)");
    }

    @Test
    void toInstant_withFractionalNanos_RoundsUpAndCarries() {
        // 0.9999999995 should round to 1_000_000_000 nanoseconds,
        // which means the seconds part is incremented.
        Double source = 1_622_547_800.9999999995;
        long seconds = 1_622_547_800L;
        // = 1_000_000_000
        long nanos = Math.round((source - seconds) * 1_000_000_000L);
        // Instant normalises the overflow automatically.
        Instant expected = Instant.ofEpochSecond(seconds, nanos);
        Instant actual = DoubleConversions.toInstant(source, converter);
        assertEquals(expected, actual, "Instant should round up and carry overflow to the next second");
    }

    @Test
    void toInstant_negativeDouble() {
        // seconds = -10, fraction = -0.5
        Double source = -10.5;
        // -10
        long seconds = (long) source;
        // -500_000_000
        long nanos = Math.round((source - seconds) * 1_000_000_000L);
        Instant expected = Instant.ofEpochSecond(seconds, nanos);
        Instant actual = DoubleConversions.toInstant(source, converter);
        assertEquals(expected, actual, "Instant should correctly handle negative values");
    }

    @Test
    void toInstant_invalidSourceType_throwsClassCastException() {
        // The method casts the source to Double without checking, so a non‑Double should
        // result in a ClassCastException.
        Integer invalidSource = 42;
        assertThrows(ClassCastException.class, () -> DoubleConversions.toInstant(invalidSource, converter), "Passing a non‑Double should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] from={0} → {1}")
    @MethodSource("validConversionProvider")
    void toInstant_validConversions(Object from, String expectedIso) {
        Instant expected = Instant.parse(expectedIso);
        Instant actual = DoubleConversions.toInstant(from, converter);
        assertEquals(expected, actual, () -> "Instant conversion of " + from + " should equal " + expectedIso);
    }

    @ParameterizedTest(name = "[{index}] from={0} → DateTimeException")
    @MethodSource("roundingEdgeCaseProvider")
    void toInstant_roundingEdgeCases(Object from) {
        assertThrows(DateTimeException.class, () -> DoubleConversions.toInstant(from, converter), () -> "Conversion of " + from + " should throw DateTimeException due to invalid nanosecond adjustment");
    }

    @ParameterizedTest(name = "[{index}] from={0} (type={1}) → ClassCastException")
    @MethodSource("invalidTypeProvider")
    void toInstant_invalidSourceTypes(Object from) {
        assertThrows(ClassCastException.class, () -> DoubleConversions.toInstant(from, converter), () -> "Passing a non‑Double (" + (from == null ? "null" : from.getClass().getSimpleName()) + ") should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] from={0} → DateTimeException")
    @MethodSource("extremeValueProvider")
    void toInstant_extremeValues(Object from) {
        assertThrows(DateTimeException.class, () -> DoubleConversions.toInstant(from, converter), () -> "Conversion of extreme value " + from + " should throw DateTimeException");
    }
}
