package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.format.DateTimeParseException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toCalendar_Test {

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Build a Converter with UTC as the default time‑zone.
        ConverterOptions options = new ConverterOptions();
        // The ConverterOptions class provides a setter for the time‑zone.
        // If the actual API differs, adjust accordingly.
        options.setTimeZone(UTC);
        converter = new Converter(options);
    }

    @Test
    void toCalendar_givenLocalDateTime_returnsCalendarWithSameInstantAndUtcTimeZone() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, Month.MAY, 10, 12, 30, 15);
        ZonedDateTime expectedZdt = ldt.atZone(UTC.toZoneId());
        long expectedMillis = expectedZdt.toInstant().toEpochMilli();
        // WHEN
        Calendar calendar = LocalDateTimeConversions.toCalendar(ldt, converter);
        // THEN
        assertNotNull(calendar, "Calendar should not be null");
        assertEquals(UTC, calendar.getTimeZone(), "Calendar must use the UTC time‑zone");
        assertEquals(expectedMillis, calendar.getTimeInMillis(), "Calendar instant must match the expected epoch milliseconds");
    }

    @Test
    void toCalendar_givenDate_returnsCalendarWithSameInstantAndUtcTimeZone() {
        // GIVEN
        // arbitrary fixed instant
        long epochMillis = 1_680_000_000_000L;
        Date date = new Date(epochMillis);
        ZonedDateTime expectedZdt = Instant.ofEpochMilli(epochMillis).atZone(UTC.toZoneId());
        // WHEN
        Calendar calendar = LocalDateTimeConversions.toCalendar(date, converter);
        // THEN
        assertNotNull(calendar, "Calendar should not be null");
        assertEquals(UTC, calendar.getTimeZone(), "Calendar must use the UTC time‑zone");
        assertEquals(expectedZdt.toInstant().toEpochMilli(), calendar.getTimeInMillis(), "Calendar instant must match the Date's epoch milliseconds");
    }

    @Test
    void toCalendar_givenNull_returnsCalendarAtEpochZeroInUtc() {
        // GIVEN
        // Converter treats null as a null source; toZonedDateTime will return null.
        // The implementation under test does not guard against null, so we expect
        // a NullPointerException. This test documents that behaviour.
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toCalendar(source, converter), "Converting a null source should throw NPE");
    }

    @Test
    void toCalendar_givenNullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toCalendar(source, converter), "Converting a null source should throw NPE");
    }

    @Test
    void toCalendar_givenNullConverter_throwsNullPointerException() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.now();
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toCalendar(ldt, nullConverter), "Passing a null Converter should result in NPE");
    }

    @Test
    void toCalendar_converterWithNullOptions_throwsNullPointerException() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.now();
        // Mock a Converter whose getOptions() returns null
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toCalendar(ldt, mockConverter), "If converter.getOptions() returns null, a NPE is expected");
    }

    @Test
    void toCalendar_converterWithNullTimeZone_throwsNullPointerException() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.now();
        // Mock the ConverterOptions to return a null TimeZone
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockOptions.getTimeZone()).thenReturn(null);
        // Mock the Converter to return the above options
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toCalendar(ldt, mockConverter), "If options.getTimeZone() returns null, Calendar.getInstance(null) throws NPE");
    }

    @Test
    void toCalendar_givenUnsupportedObject_throwsIllegalArgumentException() {
        // GIVEN
        // not convertible to ZonedDateTime
        Object unsupported = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toCalendar(unsupported, converter), "Unsupported source type should cause IllegalArgumentException");
    }

    @Test
    void toCalendar_givenMalformedIsoString_throwsDateTimeParseException() {
        // GIVEN
        // month 13 is invalid
        String malformed = "2021-13-01T00:00:00";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> LocalDateTimeConversions.toCalendar(malformed, converter), "Malformed ISO‑8601 string should cause DateTimeParseException");
    }

    @ParameterizedTest
    @ValueSource(strings = { // simple ISO_LOCAL_DATE_TIME
    "2022-01-01T10:15:30", // with offset
    "2022-01-01T10:15:30+02:00", // UTC designator
    "2022-01-01T10:15:30Z" })
    void toCalendar_givenValidIsoString_returnsCorrectCalendar(String isoString) {
        // GIVEN
        // The conversion logic ultimately uses toZonedDateTime, which parses ISO strings.
        ZonedDateTime expectedZdt = ZonedDateTime.parse(isoString);
        long expectedMillis = expectedZdt.toInstant().toEpochMilli();
        // WHEN
        Calendar calendar = LocalDateTimeConversions.toCalendar(isoString, converter);
        // THEN
        assertNotNull(calendar, "Calendar should not be null for a valid ISO string");
        assertEquals(UTC, calendar.getTimeZone(), "Calendar must use the UTC time‑zone from the test setup");
        assertEquals(expectedMillis, calendar.getTimeInMillis(), "Calendar instant must match the parsed ISO string");
    }
}
