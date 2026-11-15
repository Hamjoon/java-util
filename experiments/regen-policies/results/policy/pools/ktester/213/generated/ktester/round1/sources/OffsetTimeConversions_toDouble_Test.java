package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffsetTimeConversions_toDouble_Test {

    /**
     * Shared Converter instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validOffsetTimeProvider() {
        return Stream.of(// typical positive offset (+02:00)
        Arguments.of("Typical OffsetTime with +02:00", OffsetTime.of(12, 34, 56, 123_456_789, ZoneOffset.ofHours(2)), null, // 12:34:56+02:00 => 10:34:56 UTC
        computeDouble(OffsetTime.of(12, 34, 56, 123_456_789, ZoneOffset.ofHours(2)))), // negative offset (-05:00)
        Arguments.of("OffsetTime with -05:00", OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(-5)), null, computeDouble(OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(-5)))), // exactly epoch
        Arguments.of("OffsetTime at epoch (UTC)", OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC), null, 0.0), // maximum time of the day with full nanosecond precision
        Arguments.of("23:59:59.999_999_999 UTC", OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.UTC), null, computeDouble(OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.UTC))), // non‑null Converter (unused)
        Arguments.of("OffsetTime with +00:30 and custom Converter", OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHoursMinutes(0, 30)), // dummy non‑null instance
        new Converter(new ConverterOptions()), computeDouble(OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHoursMinutes(0, 30)))));
    }

    private static Stream<Arguments> extremeOffsetProvider() {
        return Stream.of(// +18:00 (maximum positive offset) => UTC = previous day 06:00 => -21600 seconds
        Arguments.of("Maximum positive offset (+18:00)", OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(18)), -21_600.0), // -18:00 (maximum negative offset) => UTC = next day 17:59:59.999999999
        Arguments.of("Maximum negative offset (-18:00)", OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHours(-18)), 151_199.999_999_999));
    }

    private static Stream<Arguments> fractionalSecondsProvider() {
        return Stream.of(// half‑second
        Arguments.of("Half‑second nanosecond value", OffsetTime.of(12, 0, 0, 500_000_000, ZoneOffset.UTC), 43_200.5), // mixed offset with 0.25 second
        Arguments.of("Mixed offset (-03:15) with 0.25 s", OffsetTime.of(6, 30, 15, 250_000_000, ZoneOffset.ofHoursMinutes(-3, -15)), 41_415.25));
    }

    private static Stream<Arguments> midnightOffsetProvider() {
        return Stream.of(// -12:00 offset => UTC = 12:00 on epoch day
        Arguments.of("Midnight with -12:00 offset", OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(-12)), 43_200.0), // +12:00 offset => UTC = previous day 12:00 => -43200 seconds
        Arguments.of("Midnight with +12:00 offset", OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(12)), -43_200.0));
    }

    /* --------------------------------------------------------------
     *  Helper method to compute the expected double from an OffsetTime.
     * -------------------------------------------------------------- */
    private static double computeDouble(OffsetTime ot) {
        Instant instant = ot.toInstant();
        return instant.getEpochSecond() + (instant.getNano() / 1.0E9);
    }

    @Test
    void toDouble_returnsCorrectEpochSecondsAndNanos() {
        // GIVEN
        OffsetTime offsetTime = OffsetTime.of(LocalTime.of(12, 34, 56, 123_456_789), ZoneOffset.ofHoursMinutes(2, 30));
        Instant expectedInstant = offsetTime.toInstant();
        // WHEN
        double result = OffsetTimeConversions.toDouble(offsetTime, converter);
        // THEN
        double expected = expectedInstant.getEpochSecond() + (expectedInstant.getNano() / 1.0E9);
        assertEquals(expected, result, 1e-12, "The double representation of the OffsetTime should match the epoch seconds plus nanos fraction.");
    }

    @Test
    void toDouble_withZeroNanos() {
        // GIVEN
        OffsetTime offsetTime = OffsetTime.of(LocalTime.of(0, 0, 0, 0), ZoneOffset.UTC);
        Instant expectedInstant = offsetTime.toInstant();
        // WHEN
        double result = OffsetTimeConversions.toDouble(offsetTime, converter);
        // THEN
        // nanos are zero
        double expected = expectedInstant.getEpochSecond();
        assertEquals(expected, result, 0.0, "OffsetTime at epoch with zero nanos should convert to an exact whole‑second double.");
    }

    @Test
    void toDouble_nullSource_throwsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetTimeConversions.toDouble(nullSource, converter), "Passing a null source should result in a NullPointerException due to the cast.");
    }

    @Test
    void toDouble_invalidSource_throwsClassCastException() {
        // GIVEN
        Object notAnOffsetTime = "2023-01-01T12:00:00Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetTimeConversions.toDouble(notAnOffsetTime, converter), "Passing a non‑OffsetTime object should cause a ClassCastException.");
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the "Valid OffsetTime conversions" group
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validOffsetTimeProvider")
    void toDouble_validConversions(String description, OffsetTime input, Converter conv, double expected) {
        // WHEN
        double result = OffsetTimeConversions.toDouble(input, conv);
        // THEN
        assertEquals(expected, result, 1e-12, () -> "Failed conversion: " + description);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for extreme offset handling
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("extremeOffsetProvider")
    void toDouble_extremeOffsets(String description, OffsetTime input, double expected) {
        double result = OffsetTimeConversions.toDouble(input, null);
        assertEquals(expected, result, 1e-12, () -> "Extreme offset test failed: " + description);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for fractional‑second precision
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("fractionalSecondsProvider")
    void toDouble_fractionalSeconds(String description, OffsetTime input, double expected) {
        double result = OffsetTimeConversions.toDouble(input, null);
        assertEquals(expected, result, 1e-12, () -> "Fractional‑seconds test failed: " + description);
    }

    /* --------------------------------------------------------------
     *  Test that the Converter argument is never touched
     * -------------------------------------------------------------- */
    @Test
    void toDouble_converterIsNotUsed() {
        // Create a strict mock that throws on any interaction.
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new AssertionError("Converter should not be used by toDouble");
        });
        OffsetTime ot = OffsetTime.of(9, 45, 30, 0, ZoneOffset.UTC);
        double result = OffsetTimeConversions.toDouble(ot, throwingConverter);
        double expected = computeDouble(ot);
        assertEquals(expected, result, 1e-12, "Result should be correct even when a throwing Converter is supplied.");
        // Verify that no interaction happened (the mock would already have thrown if it did).
        verifyNoInteractions(throwingConverter);
    }

    /* --------------------------------------------------------------
     *  Edge‑case midnight with non‑zero offset
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("midnightOffsetProvider")
    void toDouble_midnightWithLargeOffset(String description, OffsetTime input, double expected) {
        double result = OffsetTimeConversions.toDouble(input, null);
        assertEquals(expected, result, 1e-12, () -> "Midnight offset test failed: " + description);
    }
}
