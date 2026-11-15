package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Date;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.time.ZoneId;
import java.util.GregorianCalendar;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toSqlDate_Test {

    // UTC+9
    private static final TimeZone TOKYO = TimeZone.getTimeZone("Asia/Tokyo");

    // UTC+1/+2
    private static final TimeZone PARIS = TimeZone.getTimeZone("Europe/Paris");

    // UTC-5/-4
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – they contain the default ZoneId (system default)
        // For deterministic tests we rely on the ConverterOptions to respect the
        // Calendar's time‑zone when converting, so we simply instantiate it.
        converter = new Converter(new DefaultConverterOptions());
    }

    /**
     * Helper that creates a Calendar with the supplied fields.
     */
    private Calendar createCalendar(int year, int month, int day, int hour, int minute, int second, int millis, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.clear();
        // Calendar months are zero‑based
        cal.set(year, month - 1, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, millis);
        return cal;
    }

    /**
     * Helper that creates a Calendar set to the given {@link Instant} in the supplied time‑zone.
     */
    private Calendar createCalendarFromInstant(Instant instant, TimeZone tz) {
        Calendar cal = Calendar.getInstance(tz);
        cal.setTimeInMillis(instant.toEpochMilli());
        return cal;
    }

    @Test
    void testToSqlDate_TokyoTimezone() {
        Calendar cal = createCalendar(2023, 6, 15, 15, 30, 0, 500, TOKYO);
        Date sqlDate = (Date) CalendarConversions.toSqlDate(cal, converter);
        // The expected java.sql.Date corresponds to the local date in Tokyo (2023‑06‑15)
        assertEquals(Date.valueOf("2023-06-15"), sqlDate);
    }

    @Test
    void testToSqlDate_ParisTimezone() {
        Calendar cal = createCalendar(1969, 12, 31, 23, 59, 59, 999, PARIS);
        Date sqlDate = (Date) CalendarConversions.toSqlDate(cal, converter);
        // In Paris the local date is still 1969‑12‑31
        assertEquals(Date.valueOf("1969-12-31"), sqlDate);
    }

    @Test
    void testToSqlDate_NewYorkTimezone_CrossesMidnight() {
        // 1970‑01‑01 00:30 in New York is still 1970‑01‑01 locally,
        // but the instant is 1970‑01‑01T05:30Z, which yields the same local date.
        Calendar cal = createCalendar(1970, 1, 1, 0, 30, 0, 0, NEW_YORK);
        Date sqlDate = (Date) CalendarConversions.toSqlDate(cal, converter);
        assertEquals(Date.valueOf("1970-01-01"), sqlDate);
    }

    @Test
    void testToSqlDate_EpochStart_Utc() {
        // Calendar in UTC at the epoch start should produce 1970‑01‑01
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.setTimeInMillis(0L);
        Date sqlDate = (Date) CalendarConversions.toSqlDate(cal, converter);
        assertEquals(Date.valueOf("1970-01-01"), sqlDate);
    }

    /**
     * Valid conversion where both Calendar and target ZoneId are UTC.
     */
    @Test
    void testValidConversion_UtcZone() {
        Instant instant = Instant.parse("2023-03-15T10:00:00Z");
        Calendar cal = createCalendarFromInstant(instant, TimeZone.getTimeZone("UTC"));
        // Mock a Converter that returns UTC as its ZoneId
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        Date result = (Date) CalendarConversions.toSqlDate(cal, mockConverter);
        assertEquals(Date.valueOf("2023-03-15"), result);
    }

    /**
     * Conversion with a non‑UTC zone (America/New_York). 10:00 UTC → 05:00/06:00 local,
     * same calendar day.
     */
    @Test
    void testValidConversion_NewYorkZone() {
        Instant instant = Instant.parse("2023-03-15T10:00:00Z");
        Calendar cal = createCalendarFromInstant(instant, TimeZone.getTimeZone("UTC"));
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
        Date result = (Date) CalendarConversions.toSqlDate(cal, mockConverter);
        assertEquals(Date.valueOf("2023-03-15"), result);
    }

    /**
     * Cross‑midnight forward: 23:30 UTC on 15 Mar becomes 08:30 on 16 Mar in Tokyo (+9).
     */
    @Test
    void testCrossMidnight_Forward_Tokyo() {
        Instant instant = Instant.parse("2023-03-15T23:30:00Z");
        Calendar cal = createCalendarFromInstant(instant, TimeZone.getTimeZone("UTC"));
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("Asia/Tokyo"));
        Date result = (Date) CalendarConversions.toSqlDate(cal, mockConverter);
        assertEquals(Date.valueOf("2023-03-16"), result);
    }

    /**
     * Zone causing previous day: 02:00 UTC on 1 Mar → 18:00 on 28 Feb in Los Angeles (UTC‑8).
     */
    @Test
    void testCrossMidnight_Backward_LosAngeles() {
        Instant instant = Instant.parse("2023-03-01T02:00:00Z");
        Calendar cal = createCalendarFromInstant(instant, TimeZone.getTimeZone("UTC"));
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/Los_Angeles"));
        Date result = (Date) CalendarConversions.toSqlDate(cal, mockConverter);
        assertEquals(Date.valueOf("2023-02-28"), result);
    }

    /**
     * No date change when offset does not cross midnight (Honolulu, UTC‑10).
     */
    @Test
    void testNoDateChange_Honolulu() {
        Instant instant = Instant.parse("2021-12-31T23:30:00Z");
        Calendar cal = createCalendarFromInstant(instant, TimeZone.getTimeZone("UTC"));
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("Pacific/Honolulu"));
        Date result = (Date) CalendarConversions.toSqlDate(cal, mockConverter);
        assertEquals(Date.valueOf("2021-12-31"), result);
    }

    @Test
    void testNullFromParameter_ShouldThrowNPE() {
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        assertThrows(NullPointerException.class, () -> CalendarConversions.toSqlDate(null, mockConverter));
    }

    @Test
    void testInvalidFromParameter_ShouldThrowClassCastException() {
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        String invalidFrom = "2023-03-15";
        assertThrows(ClassCastException.class, () -> CalendarConversions.toSqlDate(invalidFrom, mockConverter));
    }

    @Test
    void testNullConverterParameter_ShouldThrowNPE() {
        Calendar cal = createCalendarFromInstant(Instant.parse("2023-03-15T10:00:00Z"), TimeZone.getTimeZone("UTC"));
        assertThrows(NullPointerException.class, () -> CalendarConversions.toSqlDate(cal, null));
    }

    @Test
    void testConverterWithNullOptions_ShouldThrowNPE() {
        Calendar cal = createCalendarFromInstant(Instant.parse("2023-03-15T10:00:00Z"), TimeZone.getTimeZone("UTC"));
        Converter mockConverter = mock(Converter.class);
        // options are null
        when(mockConverter.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> CalendarConversions.toSqlDate(cal, mockConverter));
    }

    @Test
    void testConverterOptionsWithNullZoneId_ShouldThrowNPE() {
        Calendar cal = createCalendarFromInstant(Instant.parse("2023-03-15T10:00:00Z"), TimeZone.getTimeZone("UTC"));
        Converter mockConverter = mock(Converter.class);
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // ZoneId is null
        when(mockOptions.getZoneId()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> CalendarConversions.toSqlDate(cal, mockConverter));
    }
}
