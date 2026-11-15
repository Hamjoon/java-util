package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.time.OffsetTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toOffsetTime_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the real Converter with default options (UTC zone by default)
        ConverterOptions options = new ConverterOptions();
        // Ensure the zone is deterministic for the test
        options.setZoneId(ZoneOffset.UTC);
        converter = new Converter(options);
    }

    /**
     * Creates a {@link Converter} whose {@link ConverterOptions} return the given {@link ZoneId}.
     * The method uses a Mockito mock so that we can inject any zone without relying on
     * the real {@code Converter} implementation details.
     */
    private static Converter mockConverterWithZone(ZoneId zone) {
        Converter mockConverter = mock(Converter.class);
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zone);
        when(mockConverter.getOptions()).thenReturn(options);
        return mockConverter;
    }

    /**
     * Parses an ISO‑8601 offset‑time string (e.g. {@code "12:00:00+00:00"}) into an {@link OffsetTime}.
     */
    private static OffsetTime parseOffsetTime(String iso) {
        return OffsetTime.parse(iso);
    }

    private static Stream<Arguments> validTypicalProvider() {
        return Stream.of(Arguments.of(43200.0, ZoneOffset.UTC, "12:00:00+00:00", "Convert 43200 seconds (12:00:00) to OffsetTime using UTC zone."), Arguments.of(0.0, ZoneOffset.UTC, "00:00:00+00:00", "Convert the start‑of‑day value (0 seconds) to OffsetTime."), Arguments.of(86399.999999999, ZoneOffset.UTC, "23:59:59.999999999+00:00", "Convert the maximum allowed value (just before next day) to OffsetTime."));
    }

    private static Stream<Arguments> invalidOutOfRangeProvider() {
        return Stream.of(Arguments.of(-1.0, ZoneOffset.UTC, IllegalArgumentException.class, "Negative seconds are not allowed."), Arguments.of(86400.0, ZoneOffset.UTC, IllegalArgumentException.class, "Seconds equal to 86400 exceed the day limit."), Arguments.of(1.0E20, ZoneOffset.UTC, IllegalArgumentException.class, "Very large number of seconds overflows the valid epoch second range."));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of("not a number", ZoneOffset.UTC, ClassCastException.class, "String input should cause ClassCastException."), Arguments.of(Integer.valueOf(12345), ZoneOffset.UTC, ClassCastException.class, "Integer input should cause ClassCastException."));
    }

    private static Stream<Arguments> validFractionalProvider() {
        return Stream.of(Arguments.of(0.123456789, ZoneOffset.UTC, "00:00:00.123456789+00:00", "Small fractional second value using UTC."), Arguments.of(43200.5, ZoneId.of("America/New_York"), "12:00:00.500000000-05:00", "Fractional seconds with a -05:00 offset."), Arguments.of(86399.999999999, ZoneId.of("Asia/Kolkata"), "23:59:59.999999999+05:30", "Maximum allowed value with a +05:30 offset."));
    }

    private static Stream<Arguments> edgeExtremeProvider() {
        return Stream.of(// Double.MAX_VALUE – overflow
        Arguments.of(Double.MAX_VALUE, ZoneOffset.UTC, IllegalArgumentException.class, "Double.MAX_VALUE far exceeds the seconds‑in‑a‑day range."), // Positive infinity
        Arguments.of(Double.POSITIVE_INFINITY, ZoneOffset.UTC, IllegalArgumentException.class, "Positive infinity cannot be represented as an epoch second."), // Negative infinity
        Arguments.of(Double.NEGATIVE_INFINITY, ZoneOffset.UTC, IllegalArgumentException.class, "Negative infinity is out of the allowed range."), // NaN – special case: should return midnight UTC
        Arguments.of(Double.NaN, ZoneOffset.UTC, null, "NaN is cast to a double; method should return midnight UTC."));
    }

    private static Stream<Arguments> nullArgumentProvider() {
        Converter utcConverter = mockConverterWithZone(ZoneOffset.UTC);
        return Stream.of(Arguments.of(null, utcConverter, NullPointerException.class, "Null source value should cause NullPointerException."), Arguments.of(0.0, null, NullPointerException.class, "Null converter should cause NullPointerException."));
    }

    private static Stream<Arguments> boundaryRoundingProvider() {
        return Stream.of(Arguments.of(0.0000000015, ZoneOffset.UTC, "00:00:00.000000001+00:00", "1.5 nanoseconds should be truncated to 1 nanosecond."), Arguments.of(0.9999999999, ZoneOffset.UTC, "00:00:00.999999999+00:00", "Fraction just under 1 second should truncate to 999,999,999 nanoseconds."));
    }

    @Test
    void toOffsetTime_wholeSeconds() {
        // 01:00:00
        double input = 3600.0;
        OffsetTime expected = OffsetTime.of(1, 0, 0, 0, ZoneOffset.UTC);
        OffsetTime actual = DoubleConversions.toOffsetTime(input, mockConverterWithZone(ZoneOffset.UTC));
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetTime_fractionalSeconds() {
        // 01:01:01.123456789
        double input = 3661.123456789;
        OffsetTime expected = OffsetTime.of(1, 1, 1, 123_456_789, ZoneOffset.UTC);
        OffsetTime actual = DoubleConversions.toOffsetTime(input, mockConverterWithZone(ZoneOffset.UTC));
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetTime_zero() {
        double input = 0.0;
        OffsetTime expected = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);
        OffsetTime actual = DoubleConversions.toOffsetTime(input, mockConverterWithZone(ZoneOffset.UTC));
        assertEquals(expected, actual);
    }

    @Test
    void toOffsetTime_negativeValue_throwsIllegalArgumentException() {
        double input = -1.0;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toOffsetTime(input, mockConverterWithZone(ZoneOffset.UTC)));
        assertTrue(ex.getMessage().contains("Input value [-1.0]"));
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validTypicalProvider")
    void toOffsetTime_validTypical(double input, ZoneId zone, String expectedIso, String description) {
        Converter converter = mockConverterWithZone(zone);
        OffsetTime expected = parseOffsetTime(expectedIso);
        OffsetTime actual = DoubleConversions.toOffsetTime(input, converter);
        assertEquals(expected, actual, description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidOutOfRangeProvider")
    void toOffsetTime_invalidOutOfRange(double input, ZoneId zone, Class<? extends Exception> expectedException, String description) {
        Converter converter = mockConverterWithZone(zone);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toOffsetTime(input, converter), description);
        assertTrue(ex.getMessage().contains("Input value [" + input + "]"), "Exception message should contain the offending input value.");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidTypeProvider")
    void toOffsetTime_invalidType(Object input, ZoneId zone, Class<? extends Exception> expectedException, String description) {
        Converter converter = mockConverterWithZone(zone);
        assertThrows(expectedException, () -> DoubleConversions.toOffsetTime(input, converter), description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validFractionalProvider")
    void toOffsetTime_validFractional(double input, ZoneId zone, String expectedIso, String description) {
        Converter converter = mockConverterWithZone(zone);
        OffsetTime expected = parseOffsetTime(expectedIso);
        OffsetTime actual = DoubleConversions.toOffsetTime(input, converter);
        assertEquals(expected, actual, description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("edgeExtremeProvider")
    void toOffsetTime_edgeExtreme(Object input, ZoneId zone, Class<? extends Exception> expectedException, String description) {
        Converter converter = mockConverterWithZone(zone);
        // The method expects a double primitive, but we pass Object to allow NaN case.
        double value = (input instanceof Double) ? (Double) input : Double.NaN;
        if (expectedException != null) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DoubleConversions.toOffsetTime(value, converter), description);
            assertTrue(ex.getMessage().contains("Input value [" + value + "]"));
        } else {
            // Expected normal result (NaN case)
            OffsetTime actual = DoubleConversions.toOffsetTime(value, converter);
            OffsetTime expected = parseOffsetTime("00:00:00+00:00");
            assertEquals(expected, actual, description);
        }
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullArgumentProvider")
    void toOffsetTime_nullArguments(Object from, Converter converter, Class<? extends Exception> expectedException, String description) {
        assertThrows(expectedException, () -> DoubleConversions.toOffsetTime(from, converter), description);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("boundaryRoundingProvider")
    void toOffsetTime_boundaryRounding(double input, ZoneId zone, String expectedIso, String description) {
        Converter converter = mockConverterWithZone(zone);
        OffsetTime expected = parseOffsetTime(expectedIso);
        OffsetTime actual = DoubleConversions.toOffsetTime(input, converter);
        assertEquals(expected, actual, description);
    }
}
