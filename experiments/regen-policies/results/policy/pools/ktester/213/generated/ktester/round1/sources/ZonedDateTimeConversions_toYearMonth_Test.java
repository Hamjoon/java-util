package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toYearMonth_Test {

    /**
     * Converter used for all test cases – created once per test method.
     */
    private Converter converter;

    /**
     * Common {@link ConverterOptions} instance – configured in {@code @BeforeEach}.
     */
    private ConverterOptions options;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise ConverterOptions with a default zone (will be overridden per test if needed).
        options = new ConverterOptions();
        // The default zone does not matter for the tests that explicitly set a zone,
        // but we set it to UTC to avoid a null‑pointer inside the Converter.
        options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up – the Converter holds no external resources.
        converter = null;
        options = null;
    }

    /**
     * Provider for all *valid* conversion scenarios.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Valid conversions – group “Valid conversions”
        Arguments.of(ZonedDateTime.parse("2023-05-15T10:15:30+02:00[Europe/Paris]"), ZoneId.of("UTC"), YearMonth.of(2023, 5), "Standard conversion where the source ZonedDateTime is already in the target month after adjusting to UTC."), Arguments.of(ZonedDateTime.parse("2023-01-31T23:00:00-05:00[America/New_York]"), ZoneId.of("UTC"), YearMonth.of(2023, 2), "Cross‑month conversion: 2023‑01‑31 23:00‑05:00 becomes 2023‑02‑01 04:00 UTC."), Arguments.of(ZonedDateTime.parse("2022-12-31T23:30:00+01:00[Europe/Berlin]"), ZoneId.of("Asia/Tokyo"), YearMonth.of(2023, 1), "Conversion to a zone ahead of UTC that rolls the date into the next year."), // Boundary month shifts (backward)
        Arguments.of(ZonedDateTime.parse("2023-03-01T00:30:00+02:00[Europe/Paris]"), ZoneId.of("UTC"), YearMonth.of(2023, 2), "Conversion from a zone ahead of UTC that moves the instant back into the previous month."), Arguments.of(ZonedDateTime.parse("2023-01-01T00:15:00+01:00[Europe/Paris]"), ZoneId.of("America/New_York"), YearMonth.of(2022, 12), "Cross‑year backward shift."), // Leap year handling
        Arguments.of(ZonedDateTime.parse("2020-02-29T23:00:00-02:00[America/Noronha]"), ZoneId.of("UTC"), YearMonth.of(2020, 3), "Leap‑day at 23:00 in a –02:00 offset rolls over to March 1st UTC."), // Extreme year values
        Arguments.of(ZonedDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), ZoneId.of("UTC"), YearMonth.of(1, 1), "Earliest supported year (0001) with no zone shift."), Arguments.of(ZonedDateTime.of(9999, 12, 31, 23, 0, 0, 0, ZoneOffset.ofHours(-2)), ZoneId.of("UTC"), YearMonth.of(9999, 12), "Far future date where the zone conversion does not cross a month boundary."), // Same‑zone conversion (no change)
        Arguments.of(ZonedDateTime.parse("2023-07-20T12:00:00Z[UTC]"), ZoneId.of("UTC"), YearMonth.of(2023, 7), "Source ZonedDateTime already in the target zone."), // Non‑hour offset zones
        Arguments.of(ZonedDateTime.parse("2023-08-15T10:00:00+05:30[Asia/Kolkata]"), ZoneId.of("UTC"), YearMonth.of(2023, 8), "Conversion from a zone with a 30‑minute offset; date remains in August."));
    }

    /**
     * Provider for all *invalid* input scenarios.
     */
    private static Stream<Arguments> invalidConversionProvider() {
        // Helper mock that returns null from getOptions()
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        // Helper mock that returns options with a null ZoneId
        ConverterOptions nullZoneOptions = new ConverterOptions();
        nullZoneOptions.setZoneId(null);
        Converter converterWithNullZone = new Converter(nullZoneOptions);
        return Stream.of(// Null source object
        Arguments.of(null, new Converter(options), NullPointerException.class, "The source object is null; the cast to ZonedDateTime should throw NPE."), // Wrong type for source
        Arguments.of(LocalDate.of(2023, 5, 15), new Converter(options), ClassCastException.class, "The source object is not a ZonedDateTime; expect CCE."), // Null converter argument
        Arguments.of(ZonedDateTime.parse("2023-05-15T10:15:30+02:00[Europe/Paris]"), null, NullPointerException.class, "Converter argument is null; accessing its options must NPE."), // Converter with null ZoneId inside options
        Arguments.of(ZonedDateTime.parse("2023-05-15T10:15:30+02:00[Europe/Paris]"), converterWithNullZone, NullPointerException.class, "Converter’s options contain a null ZoneId; withZoneSameInstant(null) should NPE."), // Converter whose getOptions() itself returns null
        Arguments.of(ZonedDateTime.parse("2023-05-15T10:15:30+02:00[Europe/Paris]"), converterWithNullOptions, NullPointerException.class, "Converter.getOptions() is null; calling getZoneId() on it should NPE."));
    }

    /**
     * Verify that a ZonedDateTime that stays on the same calendar day after the zone
     * conversion yields the expected YearMonth.
     */
    @Test
    void toYearMonth_SameDay_NoDateShift() {
        // Source ZonedDateTime in UTC.
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC"));
        // Target zone – also results in the same local date (America/New_York is UTC‑4 at this moment).
        options.setZoneId(ZoneId.of("America/New_York"));
        // Re‑create the converter so it picks up the new zone id.
        converter = new Converter(options);
        // 2023‑05
        YearMonth expected = YearMonth.of(2023, 5);
        YearMonth actual = ZonedDateTimeConversions.toYearMonth(source, converter);
        assertEquals(expected, actual, "YearMonth should reflect the local date in the target zone when the date does not cross midnight.");
    }

    /**
     * Verify that a ZonedDateTime whose instant crosses midnight when converted to the
     * target zone results in the correct (previous) YearMonth.
     */
    @Test
    void toYearMonth_DateShift_PreviousMonth() {
        // 2023‑01‑01T01:00Z – one hour after midnight UTC.
        ZonedDateTime source = ZonedDateTime.of(2023, 1, 1, 1, 0, 0, 0, ZoneId.of("UTC"));
        // Target zone is UTC‑10 (Hawaii). The same instant is 2022‑12‑31 15:00 in Honolulu.
        options.setZoneId(ZoneId.of("Pacific/Honolulu"));
        converter = new Converter(options);
        // 2022‑12
        YearMonth expected = YearMonth.of(2022, 12);
        YearMonth actual = ZonedDateTimeConversions.toYearMonth(source, converter);
        assertEquals(expected, actual, "YearMonth should reflect the previous month when the zone conversion moves the date back across month boundary.");
    }

    /**
     * Verify that the method works when the source ZonedDateTime already uses the
     * target zone – the result should be the YearMonth of the original date.
     */
    @Test
    void toYearMonth_SourceAlreadyInTargetZone() {
        ZoneId targetZone = ZoneId.of("Europe/Paris");
        // Source ZonedDateTime already in the target zone.
        ZonedDateTime source = ZonedDateTime.of(2022, 11, 20, 12, 0, 0, 0, targetZone);
        options.setZoneId(targetZone);
        converter = new Converter(options);
        YearMonth expected = YearMonth.of(2022, 11);
        YearMonth actual = ZonedDateTimeConversions.toYearMonth(source, converter);
        assertEquals(expected, actual, "When source zone matches target zone, the YearMonth should be unchanged.");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionProvider")
    void toYearMonth_ValidConversions(ZonedDateTime source, ZoneId targetZone, YearMonth expected, String description) {
        // Arrange – configure the converter with the target zone for this case
        options.setZoneId(targetZone);
        converter = new Converter(options);
        // Act
        YearMonth actual = ZonedDateTimeConversions.toYearMonth(source, converter);
        // Assert
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidConversionProvider")
    void toYearMonth_InvalidInputs(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // Act & Assert
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toYearMonth(from, conv), description);
    }
}
