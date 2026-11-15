package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toDuration_Test {

    // not used by the method but required by the signature
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a real Converter – the method under test does not depend on its behaviour.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test.
        converter = null;
    }

    private static Stream<Arguments> provideToDurationNormalCases() {
        return Stream.of(// zero
        Arguments.of(0.0, "PT0S", "Zero value should produce a zero‑length Duration."), // whole seconds
        Arguments.of(5.0, "PT5S", "Whole‑second positive double yields a Duration with only seconds."), // positive fractional
        Arguments.of(2.75, "PT2.75S", "Positive double with fractional part should be split into seconds (2) and nanoseconds (750 000 000)."), // whole negative seconds
        Arguments.of(-3.0, "PT-3S", "Whole‑second negative double yields a negative Duration."), // negative fractional
        Arguments.of(-4.125, "PT-4.125S", "Negative double with fractional part should be split correctly (seconds = -4, nanos = -125 000 000)."), // large finite double that truncates to Long.MAX_VALUE, no nanosecond overflow
        Arguments.of(9.223372036854776e+18, "PT9223372036854775807S", "Double larger than Long.MAX_VALUE is truncated to Long.MAX_VALUE; nanoseconds become 0."), // NaN -> treated as zero
        Arguments.of(Double.NaN, "PT0S", "NaN is cast to long as 0; both seconds and nanoseconds become 0, yielding a zero Duration."), // just below 2 seconds, testing truncation and nanosecond rounding
        Arguments.of(1.9999999995, "PT1.999999999S", "Fractional double just below 2 seconds should be truncated to 1 second and 999,999,999 nanoseconds."), // tiny negative fractional part
        Arguments.of(-1.000000001, "PT-1.000000001S", "Negative double with a tiny fractional part results in a negative duration with both seconds and nanoseconds negative."), // non‑null converter is ignored
        Arguments.of(3.5, "PT3.5S", "A non‑null Converter is ignored; the method still produces a duration of 3.5 seconds."), // smallest positive double (Double.MIN_VALUE)
        Arguments.of(Double.MIN_VALUE, "PT0S", "The smallest positive non‑zero double truncates to 0 seconds and 0 nanoseconds, yielding a zero duration."), // smallest negative double (-Double.MIN_VALUE)
        Arguments.of(-Double.MIN_VALUE, "PT0S", "The smallest negative double also truncates to zero seconds and zero nanoseconds."));
    }

    private static Stream<Arguments> provideToDurationExceptionCases() {
        return Stream.of(Arguments.of(Double.MAX_VALUE, DateTimeException.class, "Double.MAX_VALUE overflows the nanosecond calculation and Duration.ofSeconds throws a DateTimeException."), Arguments.of(Double.POSITIVE_INFINITY, DateTimeException.class, "Double.POSITIVE_INFINITY results in a nanosecond adjustment overflow, causing a DateTimeException."), Arguments.of(Double.NEGATIVE_INFINITY, DateTimeException.class, "Double.NEGATIVE_INFINITY yields a nanosecond adjustment overflow, triggering a DateTimeException."));
    }

    @Test
    void wholeSeconds() {
        Double source = 5.0;
        Duration result = DoubleConversions.toDuration(source, converter);
        assertEquals(Duration.ofSeconds(5, 0), result);
    }

    @Test
    void fractionalSeconds() {
        // 5 seconds + 0.5 sec
        Double source = 5.5;
        Duration result = DoubleConversions.toDuration(source, converter);
        assertEquals(Duration.ofSeconds(5, 500_000_000L), result);
    }

    @Test
    void negativeWithFraction() {
        // (long)-2.3 == -2, remainder -0.3
        Double source = -2.3;
        Duration result = DoubleConversions.toDuration(source, converter);
        // Expected: seconds = -2, nanos = -300_000_000 (Duration normalises this)
        Duration expected = Duration.ofSeconds(-2, -300_000_000L);
        assertEquals(expected, result);
    }

    @Test
    void zero() {
        Double source = 0.0;
        Duration result = DoubleConversions.toDuration(source, converter);
        assertEquals(Duration.ZERO, result);
    }

    @Test
    void tinyFraction() {
        // exactly one nanosecond
        Double source = 0.000000001;
        Duration result = DoubleConversions.toDuration(source, converter);
        assertEquals(Duration.ofNanos(1), result);
    }

    @Test
    void nullSourceThrowsNPE() {
        assertThrows(NullPointerException.class, () -> DoubleConversions.toDuration(null, converter));
    }

    @Test
    void nonDoubleSourceThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> DoubleConversions.toDuration("5.0", converter));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideToDurationNormalCases")
    void toDuration_normalCases(Object from, String expectedIso, String description) {
        Duration result = DoubleConversions.toDuration(from, converter);
        Duration expected = Duration.parse(expectedIso);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideToDurationExceptionCases")
    void toDuration_exceptionCases(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> DoubleConversions.toDuration(from, converter), description);
    }
}
