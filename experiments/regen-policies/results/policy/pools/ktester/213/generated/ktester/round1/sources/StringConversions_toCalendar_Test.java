package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.time.*;
import java.util.Date;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCalendar_Test {

    private static Converter converter;

    // used by the original tests
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter with default options – used by the original tests.
        defaultConverter = new Converter(new ConverterOptions());
    }

    /**
     * Returns a mock Converter that always returns the supplied {@code zdt}.
     */
    private static Converter mockConverterReturning(ZonedDateTime zdt) {
        Converter mock = mock(Converter.class);
        // The real StringConversions.toZonedDateTime likely calls converter.convert(..., ZonedDateTime.class)
        when(mock.convert(any(), eq(ZonedDateTime.class))).thenReturn(zdt);
        return mock;
    }

    /**
     * Returns a mock Converter that returns {@code null} for any conversion request.
     */
    private static Converter mockConverterReturningNull() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(ZonedDateTime.class))).thenReturn(null);
        return mock;
    }

    /**
     * Returns a mock Converter that parses ISO‑8601 strings to ZonedDateTime,
     *  converts java.util.Date to UTC ZonedDateTime and otherwise returns null.
     */
    private static Converter mockConverterParsingISO() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(ZonedDateTime.class))).thenAnswer(invocation -> {
            Object src = invocation.getArgument(0);
            if (src == null) {
                return null;
            }
            if (src instanceof String) {
                // ISO‑8601 parsing – let ZonedDateTime handle it
                return ZonedDateTime.parse((String) src);
            }
            if (src instanceof Date) {
                // Convert Date → Instant → UTC ZonedDateTime
                Instant instant = Instant.ofEpochMilli(((Date) src).getTime());
                return instant.atZone(ZoneOffset.UTC);
            }
            // For any other type we return null (not used in our tests)
            return null;
        });
        return mock;
    }

    /**
     * Returns a mock Converter that simply returns the supplied object
     *  if it is already a ZonedDateTime, otherwise null.
     */
    private static Converter mockConverterReturningInputIfZonedDateTime() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(ZonedDateTime.class))).thenAnswer(invocation -> {
            Object src = invocation.getArgument(0);
            return (src instanceof ZonedDateTime) ? (ZonedDateTime) src : null;
        });
        return mock;
    }

    @Test
    void toCalendar_givenNull_returnsNull() {
        Calendar result = StringConversions.toCalendar(null, defaultConverter);
        assertNull(result, "Converting a null source should return null");
    }

    @Test
    void toCalendar_givenZonedDateTime_returnsEquivalentCalendar() {
        // Given – a ZonedDateTime with a known instant and zone
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 12, 34, 56, 0, ZoneOffset.ofHours(-5));
        // When
        Calendar calendar = StringConversions.toCalendar(zdt, converter);
        // Then
        assertNotNull(calendar, "Resulting Calendar should not be null");
        // The Calendar should represent the same instant in time
        assertEquals(zdt.toInstant().toEpochMilli(), calendar.getTimeInMillis(), "Calendar milliseconds should match ZonedDateTime instant");
        // The Calendar's time zone should match the ZonedDateTime's zone
        TimeZone expectedTz = TimeZone.getTimeZone(zdt.getZone());
        assertEquals(expectedTz, calendar.getTimeZone(), "Calendar time zone should match ZonedDateTime zone");
    }

    @Test
    void toCalendar_givenYearLessThanTwo_throwsIllegalArgumentException() {
        // Given – a ZonedDateTime with year 1 (which is illegal for Calendar)
        ZonedDateTime zdt = ZonedDateTime.of(1, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        // When / Then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toCalendar(zdt, converter), "Conversion should fail for years less than 2");
        assertTrue(ex.getMessage().contains("year less than 2"), "Exception message should mention the year limitation");
    }

    @Test
    void toCalendar_givenZonedDateTimeIsNull_returnsNull() {
        // from = null, converter returns null ZonedDateTime
        Converter mock = mockConverterReturningNull();
        Calendar result = StringConversions.toCalendar(null, mock);
        assertNull(result, "When internal conversion yields null, result should be null");
    }

    @Test
    void toCalendar_givenValidISOString_returnsCorrectCalendar() {
        String iso = "2023-08-01T12:34:56Z";
        Converter mock = mockConverterParsingISO();
        Calendar cal = StringConversions.toCalendar(iso, mock);
        assertNotNull(cal, "Calendar should not be null for valid ISO string");
        // Expected values
        ZonedDateTime expectedZdt = ZonedDateTime.parse(iso);
        assertEquals(expectedZdt.getYear(), cal.get(Calendar.YEAR));
        // Calendar months are 0‑based
        assertEquals(expectedZdt.getMonthValue() - 1, cal.get(Calendar.MONTH));
        assertEquals(expectedZdt.getDayOfMonth(), cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(expectedZdt.getHour(), cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(expectedZdt.getMinute(), cal.get(Calendar.MINUTE));
        assertEquals(expectedZdt.getSecond(), cal.get(Calendar.SECOND));
        // Time‑zone must be UTC
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "Calendar time zone should be UTC");
        // Epoch milliseconds must match
        assertEquals(expectedZdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "Epoch milliseconds must match the parsed instant");
    }

    @Test
    void toCalendar_givenISOStringWithYearLessThanTwo_throwsIllegalArgumentException() {
        String iso = "0001-01-01T00:00:00Z";
        Converter mock = mockConverterParsingISO();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toCalendar(iso, mock), "Year 1 should trigger IllegalArgumentException");
        assertTrue(ex.getMessage().contains("year less than 2"));
    }

    @Test
    void toCalendar_givenNonUTCOffset_preservesZoneAndInstant() {
        String iso = "2023-08-01T12:34:56+02:00";
        Converter mock = mockConverterParsingISO();
        Calendar cal = StringConversions.toCalendar(iso, mock);
        assertNotNull(cal, "Calendar should be produced for non‑UTC offset string");
        ZonedDateTime expectedZdt = ZonedDateTime.parse(iso);
        // Verify zone ID – Java represents +02:00 as "GMT+02:00"
        assertEquals(TimeZone.getTimeZone(expectedZdt.getZone()), cal.getTimeZone(), "Calendar time zone should reflect the original offset");
        // Verify instant (epoch millis) is identical
        assertEquals(expectedZdt.toInstant().toEpochMilli(), cal.getTimeInMillis(), "Epoch milliseconds must be identical to the parsed ZonedDateTime");
    }

    @Test
    void toCalendar_givenZonedDateTimeObject_returnsEquivalentCalendar() {
        ZonedDateTime source = ZonedDateTime.of(2022, 5, 15, 10, 20, 30, 0, ZoneId.of("America/New_York"));
        Converter mock = mockConverterReturningInputIfZonedDateTime();
        Calendar cal = StringConversions.toCalendar(source, mock);
        assertNotNull(cal, "Calendar should not be null when source is a ZonedDateTime");
        // Verify fields
        assertEquals(2022, cal.get(Calendar.YEAR));
        // May -> 4 (0‑based)
        assertEquals(4, cal.get(Calendar.MONTH));
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(20, cal.get(Calendar.MINUTE));
        assertEquals(30, cal.get(Calendar.SECOND));
        // Verify zone
        assertEquals(TimeZone.getTimeZone(source.getZone()), cal.getTimeZone(), "Calendar time zone should match the source ZonedDateTime zone");
        // Verify instant
        assertEquals(source.toInstant().toEpochMilli(), cal.getTimeInMillis(), "Instant milliseconds must match");
    }

    @Test
    void toCalendar_givenJavaUtilDate_convertsToUTCCalendar() {
        // epoch start
        Date date = new Date(0L);
        // this mock also handles Date conversion
        Converter mock = mockConverterParsingISO();
        Calendar cal = StringConversions.toCalendar(date, mock);
        assertNotNull(cal, "Calendar should be produced from java.util.Date");
        // Expected UTC ZonedDateTime at epoch
        ZonedDateTime expected = Instant.ofEpochMilli(0L).atZone(ZoneOffset.UTC);
        assertEquals(1970, cal.get(Calendar.YEAR));
        // January
        assertEquals(0, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "Calendar time zone must be UTC for Date conversion");
        assertEquals(expected.toInstant().toEpochMilli(), cal.getTimeInMillis(), "Epoch milliseconds must be zero");
    }

    @Test
    void toCalendar_givenNullSourceButConverterProvidesZonedDateTime_returnsCalendar() {
        ZonedDateTime fixed = ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        Converter mock = mockConverterReturning(fixed);
        Calendar cal = StringConversions.toCalendar(null, mock);
        assertNotNull(cal, "Calendar should be created when converter supplies a ZonedDateTime even if source is null");
        assertEquals(2021, cal.get(Calendar.YEAR));
        // December -> 11
        assertEquals(11, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "Time zone should be UTC");
        assertEquals(fixed.toInstant().toEpochMilli(), cal.getTimeInMillis(), "Epoch milliseconds must match the fixed ZonedDateTime");
    }

    @Test
    void toCalendar_givenNullConverter_throwsNullPointerException() {
        String iso = "2023-08-01T12:34:56Z";
        assertThrows(NullPointerException.class, () -> StringConversions.toCalendar(iso, null), "Passing a null Converter should result in NPE");
    }
}
