package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toMonthDay_Test {

    @Mock
    private Converter converter;

    @Mock
    private ConverterOptions options;

    /**
     * Configures the {@code converter} mock so that {@code converter.getOptions()}
     * returns a {@code ConverterOptions} mock whose {@code getZoneId()} returns the
     * supplied {@code zoneId}.
     *
     * @param zoneId the ZoneId to be returned (may be {@code null})
     */
    private void stubConverterWithZoneId(ZoneId zoneId) {
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(zoneId);
    }

    /**
     * Configures the {@code converter} mock to return {@code null} for
     * {@code getOptions()} – used to test the “null options” scenario.
     */
    private void stubConverterWithNullOptions() {
        when(converter.getOptions()).thenReturn(null);
    }

    @Test
    void testToMonthDay_convertsCorrectly() {
        // GIVEN – an OffsetDateTime and a Converter whose options specify a target ZoneId
        OffsetDateTime source = OffsetDateTime.of(// 2023‑03‑15
        2023, // 2023‑03‑15
        3, // 2023‑03‑15
        15, // 10:30:00.000
        10, // 10:30:00.000
        30, // 10:30:00.000
        0, // 10:30:00.000
        0, // +02:00
        ZoneOffset.ofHours(2));
        ZoneId targetZone = ZoneId.of("America/New_York");
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(targetZone);
        // WHEN – the static conversion method is invoked
        MonthDay result = OffsetDateTimeConversions.toMonthDay(source, converter);
        // THEN – the result matches the expected MonthDay after zone conversion
        ZonedDateTime adjusted = source.atZoneSameInstant(targetZone);
        MonthDay expected = MonthDay.from(adjusted.toLocalDate());
        assertEquals(expected, result, "MonthDay should reflect the date in the target zone");
        verify(converter).getOptions();
        verify(options).getZoneId();
    }

    @Test
    @DisplayName("Normal conversion – zone different from source offset")
    void testNormalConversion() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneOffset.UTC);
        stubConverterWithZoneId(ZoneId.of("America/New_York"));
        // WHEN
        MonthDay result = OffsetDateTimeConversions.toMonthDay(source, converter);
        // THEN
        // The conversion uses atZoneSameInstant, then extracts the local date.
        ZonedDateTime adjusted = source.atZoneSameInstant(ZoneId.of("America/New_York"));
        MonthDay expected = MonthDay.from(adjusted.toLocalDate());
        assertEquals(expected, result);
        verify(converter).getOptions();
        verify(options).getZoneId();
    }

    @Test
    @DisplayName("Null `from` argument → NullPointerException")
    void testNullFrom() {
        // GIVEN
        stubConverterWithZoneId(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toMonthDay(null, converter));
        // converter is still accessed before NPE
        verify(converter).getOptions();
    }

    @Test
    @DisplayName("Null `converter` argument → NullPointerException")
    void testNullConverter() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toMonthDay(source, null));
    }

    @Test
    @DisplayName("Converter with null options → NullPointerException")
    void testConverterWithNullOptions() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.now();
        stubConverterWithNullOptions();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toMonthDay(source, converter));
        verify(converter).getOptions();
    }

    @Test
    @DisplayName("Converter with null ZoneId → NullPointerException")
    void testConverterWithNullZoneId() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(2022, 12, 31, 23, 0, 0, 0, ZoneOffset.UTC);
        stubConverterWithZoneId(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toMonthDay(source, converter));
        verify(converter).getOptions();
        verify(options).getZoneId();
    }

    @Test
    @DisplayName("Invalid `from` type → ClassCastException")
    void testInvalidFromType() {
        // GIVEN
        stubConverterWithZoneId(ZoneId.of("UTC"));
        String notAnOffsetDateTime = "2023-05-15T10:30:00Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> OffsetDateTimeConversions.toMonthDay(notAnOffsetDateTime, converter));
        verify(converter).getOptions();
    }

    @Test
    @DisplayName("Zone shift to previous day (America/Los_Angeles)")
    void testZoneShiftPreviousDay() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(2023, 3, 1, 0, 30, 0, 0, ZoneOffset.UTC);
        stubConverterWithZoneId(ZoneId.of("America/Los_Angeles"));
        // WHEN
        MonthDay result = OffsetDateTimeConversions.toMonthDay(source, converter);
        // THEN
        // 2023‑03‑01T00:30Z → 2023‑02‑28 16:30‑08:00
        MonthDay expected = MonthDay.of(2, 28);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Zone shift to next day (Asia/Tokyo)")
    void testZoneShiftNextDay() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(2023, 12, 31, 23, 0, 0, 0, // -02:00
        ZoneOffset.ofHours(-2));
        stubConverterWithZoneId(ZoneId.of("Asia/Tokyo"));
        // WHEN
        MonthDay result = OffsetDateTimeConversions.toMonthDay(source, converter);
        // THEN
        // Instant is 2024‑01‑01T01:00Z → 2024‑01‑01 10:00+09:00
        MonthDay expected = MonthDay.of(1, 1);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Leap day conversion (UTC zone)")
    void testLeapDayConversion() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(2020, 2, 29, 12, 0, 0, 0, ZoneOffset.UTC);
        stubConverterWithZoneId(ZoneId.of("UTC"));
        // WHEN
        MonthDay result = OffsetDateTimeConversions.toMonthDay(source, converter);
        // THEN
        assertEquals(MonthDay.of(2, 29), result);
    }

    @Test
    @DisplayName("Same zone conversion – no date shift")
    void testSameZoneNoShift() {
        // GIVEN
        ZoneId sameZone = ZoneId.of("UTC+02:00");
        OffsetDateTime source = OffsetDateTime.of(2021, 7, 4, 15, 45, 0, 0, ZoneOffset.ofHours(2));
        stubConverterWithZoneId(sameZone);
        // WHEN
        MonthDay result = OffsetDateTimeConversions.toMonthDay(source, converter);
        // THEN
        assertEquals(MonthDay.of(7, 4), result);
    }

    @Test
    @DisplayName("Extreme year conversion – month/day still valid")
    void testExtremeYearConversion() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(999_999_999, 12, 31, 23, 59, 59, 999_999_999, ZoneOffset.UTC);
        stubConverterWithZoneId(ZoneId.of("UTC"));
        // WHEN
        MonthDay result = OffsetDateTimeConversions.toMonthDay(source, converter);
        // THEN
        assertEquals(MonthDay.of(12, 31), result);
    }

    @Test
    @DisplayName("Nanosecond precision ignored")
    void testNanosecondPrecisionIgnored() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(2023, 5, 15, 10, 30, 45, 123_456_789, ZoneOffset.UTC);
        stubConverterWithZoneId(ZoneId.of("UTC"));
        // WHEN
        MonthDay result = OffsetDateTimeConversions.toMonthDay(source, converter);
        // THEN
        assertEquals(MonthDay.of(5, 15), result);
    }

    @Test
    @DisplayName("Zone rules unavailable for extreme year → DateTimeException")
    void testZoneRulesUnavailableForExtremeYear() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(999_999_999, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        stubConverterWithZoneId(ZoneId.of("America/New_York"));
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> OffsetDateTimeConversions.toMonthDay(source, converter));
    }
}
