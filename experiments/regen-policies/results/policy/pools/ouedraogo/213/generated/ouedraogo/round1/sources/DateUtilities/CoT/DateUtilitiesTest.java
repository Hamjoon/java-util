package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.Assert.*;

public class DateUtilitiesTest {

    /* -------------------------------------------------
     *  List of public methods extracted from DateUtilities
     * -------------------------------------------------
     *
     * 1. public static Date parseDate(String dateStr)
     * 2. public static ZonedDateTime parseDate(String dateStr, ZoneId defaultZoneId, boolean ensureDateTimeAlone)
     *
     * -------------------------------------------------
     *  Basic test cases for each public method
     * -------------------------------------------------
     */

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ------------------- Basic Tests ------------------- */

    @Test
    public void testParseDate_String_NullOrEmpty_ReturnsNull() {
        assertNull(DateUtilities.parseDate(null));
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("   "));
    }

    @Test
    public void testParseDate_String_EpochMillis() {
        long epoch = 1640995200000L; // 2022-01-01T00:00:00Z
        Date d = DateUtilities.parseDate(Long.toString(epoch));
        assertNotNull(d);
        assertEquals(epoch, d.getTime());
    }

    @Test
    public void testParseDate_String_ISODate_NoTime() {
        // Default zone is system default; we use UTC for deterministic test
        ZoneId utc = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15", utc, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(0, zdt.getHour());
        assertEquals(utc, zdt.getZone());
    }

    @Test
    public void testParseDate_String_ISODate_WithTimeAndOffset() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15T13:30:45+02:00",
                ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(45, zdt.getSecond());
        // Offset +02:00 should result in zone GMT+02:00
        assertEquals(ZoneId.ofOffset("GMT", ZoneOffset.ofHours(2)), zdt.getZone());
    }

    @Test
    public void testParseDate_String_AlphaMonth_WithOrdinal() {
        ZonedDateTime zdt = DateUtilities.parseDate("January 6th, 2024",
                ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_String_UnixStyle_WithTZAbbreviation() {
        // "Sat Jan 6 11:06:10 EST 2024"
        ZonedDateTime zdt = DateUtilities.parseDate("Sat Jan 6 11:06:10 EST 2024",
                ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
        assertEquals(11, zdt.getHour());
        assertEquals(6, zdt.getMinute());
        assertEquals(10, zdt.getSecond());
        // EST maps to America/New_York
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
    }

    @Test
    public void testParseDate_String_WithFractionalSeconds() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15T13:30:45.123456",
                ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(13, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(45, zdt.getSecond());
        // 0.123456 seconds = 123,456,000 nanoseconds
        assertEquals(123_456_000, zdt.getNano());
    }

    @Test
    public void testParseDate_String_WithNamedZoneInBrackets() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 13:30 [Europe/Paris]",
                ZoneId.of("UTC"), true);
        assertEquals(ZoneId.of("Europe/Paris"), zdt.getZone());
    }

    @Test
    public void testParseDate_String_TimeBeforeDate() {
        ZonedDateTime zdt = DateUtilities.parseDate("13:30 2024-01-15",
                ZoneId.of("UTC"), true);
        assertEquals(13, zdt.getHour());
        assertEquals(15, zdt.getDayOfMonth());
    }

    /* -------------------------------------------------
     *  Edge cases & exception handling
     * -------------------------------------------------
     */

    @Test
    public void testParseDate_String_InvalidMonth_ThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Month must be between 1 and 12 inclusive");
        DateUtilities.parseDate("2024-13-01", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_String_InvalidDay_ThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Day must be between 1 and 31 inclusive");
        DateUtilities.parseDate("2024-02-30", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_String_InvalidHour_ThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Hour must be between 0 and 23 inclusive");
        DateUtilities.parseDate("2024-01-15 24:00", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_String_InvalidMinute_ThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Minute must be between 0 and 59 inclusive");
        DateUtilities.parseDate("2024-01-15 12:60", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_String_InvalidSecond_ThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Second must be between 0 and 59 inclusive");
        DateUtilities.parseDate("2024-01-15 12:30:60", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_String_TimezoneWithoutTime_ThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Time zone information without time is invalid");
        // Date part followed by a zone without a time component
        DateUtilities.parseDate("2024-01-15+02:00", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_String_UnrecognizedZone_InStrictMode_Rethrows() {
        thrown.expect(RuntimeException.class); // ZoneId.of throws DateTimeException (a RuntimeException)
        DateUtilities.parseDate("2024-01-15 13:30 XYZ", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_String_UnrecognizedZone_NonStrict_IgnoresAndUsesDefault() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 13:30 XYZ",
                ZoneId.of("UTC"), false);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
    }

    @Test
    public void testParseDate_String_ExtraGarbageInStrictMode_ThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Issue parsing date-time, other characters present");
        DateUtilities.parseDate("2024-01-15 13:30 extra", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_String_ExtraGarbageInNonStrictMode_IgnoresGarbage() {
        // The garbage after the valid date-time should be ignored when ensureDateTimeAlone is false
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 13:30 extra", ZoneId.of("UTC"), false);
        assertEquals(13, zdt.getHour());
    }

    @Test
    public void testParseDate_String_EmptyDefaultZone_ThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        DateUtilities.parseDate("2024-01-15", null, true);
    }

    @Test
    public void testParseDate_String_EpochMillis_WithNegativeValue() {
        // Negative epoch corresponds to dates before 1970
        long negativeEpoch = -86400000L; // 1969-12-31T00:00:00Z
        Date d = DateUtilities.parseDate(Long.toString(negativeEpoch));
        assertEquals(negativeEpoch, d.getTime());
    }

    @Test
    public void testParseDate_String_FractionalSeconds_WithNoLeadingZero() {
        // ".5" seconds = 500,000,000 nanoseconds
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15T13:30:45.5",
                ZoneId.of("UTC"), true);
        assertEquals(500_000_000, zdt.getNano());
    }

    @Test
    public void testParseDate_String_ZoneOffset_Zero() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15T13:30Z",
                ZoneId.of("UTC"), true);
        assertEquals(ZoneId.ofOffset("GMT", ZoneOffset.UTC), zdt.getZone());
    }

    @Test
    public void testParseDate_String_GMT_Explicit() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 GMT",
                ZoneId.of("UTC"), true);
        assertEquals(ZoneId.of("Etc/GMT"), zdt.getZone());
    }

    @Test
    public void testParseDate_String_AlphaMonth_DayFirst_WithOrdinal() {
        ZonedDateTime zdt = DateUtilities.parseDate("21st Jan 2024",
                ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(21, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_String_AlphaMonth_YearFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024 January 31st",
                ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(31, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_String_WithBracketedGMTOffset() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 13:30 [+02:00]",
                ZoneId.of("UTC"), true);
        assertEquals(ZoneId.ofOffset("GMT", ZoneOffset.ofHours(2)), zdt.getZone());
    }

    @Test
    public void testParseDate_String_WithMultipleSpacesAndCommas() {
        ZonedDateTime zdt = DateUtilities.parseDate("  January  6 , 2024  ",
                ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
    }

    /* -------------------------------------------------
     *  Integration test for the original API that returns Date
     * -------------------------------------------------
     */

    @Test
    public void testParseDate_ReturnsDate_ObjectMatchesZonedDateTime() {
        String input = "2024-01-15T13:30:45.123456+01:00";
        Date date = DateUtilities.parseDate(input);
        assertNotNull(date);
        ZonedDateTime expected = DateUtilities.parseDate(input,
                ZoneId.systemDefault(), true);
        assertEquals(expected.toInstant().toEpochMilli(), date.getTime());
    }
}
