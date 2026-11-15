package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class DateUtilitiesTest {

    private ZoneId utcZone;

    @Before
    public void setUp() {
        utcZone = ZoneId.of("UTC");
    }

    /*** Basic null / empty handling ***/
    @Test
    public void testParseDate_NullOrEmpty_ReturnsNull() {
        assertNull(DateUtilities.parseDate(null));
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("   "));
        assertNull(DateUtilities.parseDate(null, utcZone, true));
        assertNull(DateUtilities.parseDate("   ", utcZone, false));
    }

    /*** Epoch milliseconds parsing ***/
    @Test
    public void testParseDate_EpochMillis() {
        // epoch 0
        ZonedDateTime zdt = DateUtilities.parseDate("0", utcZone, true);
        assertEquals(Instant.EPOCH, zdt.toInstant());
        assertEquals(utcZone, zdt.getZone());

        // a known timestamp
        String millis = "1640995200000"; // 2022-01-01T00:00:00Z
        ZonedDateTime zdt2 = DateUtilities.parseDate(millis, utcZone, true);
        assertEquals(Instant.ofEpochMilli(1640995200000L), zdt2.toInstant());
        assertEquals(utcZone, zdt2.getZone());
    }

    /*** ISO numeric dates ***/
    @Test
    public void testParseDate_IsoDate_YearMonthDay() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15", utcZone, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(0, zdt.getHour());
        assertEquals(0, zdt.getMinute());
        assertEquals(0, zdt.getSecond());
        assertEquals(utcZone, zdt.getZone());
    }

    @Test
    public void testParseDate_IsoDate_WithTime() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024/01/15 13:45:30", utcZone, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(45, zdt.getMinute());
        assertEquals(30, zdt.getSecond());
        assertEquals(utcZone, zdt.getZone());
    }

    @Test
    public void testParseDate_IsoDate_WithOffset() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15T13:45:30+02:00", utcZone, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(45, zdt.getMinute());
        assertEquals(30, zdt.getSecond());
        assertEquals(ZoneId.of("GMT+02:00"), zdt.getZone());
    }

    /*** Alpha month patterns ***/
    @Test
    public void testParseDate_AlphaMonth_MonthFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("January 6th, 2024", utcZone, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_AlphaMonth_DayFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("6th January 2024", utcZone, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_AlphaMonth_YearFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024 January 6th", utcZone, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
    }

    /*** Unix style date ***/
    @Test
    public void testParseDate_UnixStyle_WithAbbreviation() {
        // "Sat Jan 6 11:06:10 EST 2024"
        ZonedDateTime zdt = DateUtilities.parseDate("Sat Jan 6 11:06:10 EST 2024", utcZone, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
        assertEquals(11, zdt.getHour());
        assertEquals(6, zdt.getMinute());
        assertEquals(10, zdt.getSecond());
        // EST should map to America/New_York
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
    }

    /*** Timezone abbreviation handling ***/
    @Test
    public void testParseDate_TimezoneAbbreviation_PST() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 13:00 PST", utcZone, true);
        assertEquals(ZoneId.of("America/Los_Angeles"), zdt.getZone());
    }

    /*** Invalid month / day / hour / minute / second ***/
    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidMonth() {
        DateUtilities.parseDate("2024-13-01", utcZone, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidDay() {
        DateUtilities.parseDate("2024-02-30", utcZone, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidHour() {
        DateUtilities.parseDate("2024-01-01 24:00", utcZone, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidMinute() {
        DateUtilities.parseDate("2024-01-01 23:60", utcZone, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidSecond() {
        DateUtilities.parseDate("2024-01-01 23:59:60", utcZone, true);
    }

    /*** EnsureDateTimeAlone flag ***/
    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_StrictExtraGarbage_Throws() {
        DateUtilities.parseDate("2024-01-01 abc", utcZone, true);
    }

    @Test
    public void testParseDate_NonStrictExtraGarbage_IgnoresGarbage() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-01 abc", utcZone, false);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(1, zdt.getDayOfMonth());
        assertEquals(0, zdt.getHour());
        assertEquals(0, zdt.getMinute());
    }

    /*** Fractional seconds handling ***/
    @Test
    public void testParseDate_FractionalSeconds() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 13:30:45.123456", utcZone, true);
        assertEquals(13, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(45, zdt.getSecond());
        // Fractional part .123456 => 123456000 nanoseconds
        assertEquals(123_456_000, zdt.getNano());
    }

    /*** Timezone offset without colon (e.g., +0200) ***/
    @Test
    public void testParseDate_TimezoneOffset_HHMM() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 13:45:30+0200", utcZone, true);
        assertEquals(ZoneId.of("GMT+02:00"), zdt.getZone());
    }

    /*** Timezone named with brackets ***/
    @Test
    public void testParseDate_TimezoneBracketed() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 13:45:30[America/New_York]", utcZone, true);
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
    }

    /*** parseDate(String) returning java.util.Date ***/
    @Test
    public void testParseDate_StringReturnsDate() {
        // Using default system zone for this test; compare instant only
        Date date = DateUtilities.parseDate("2024-01-15 13:45:30+02:00");
        Instant expectedInstant = Instant.parse("2024-01-15T13:45:30+02:00");
        assertEquals(expectedInstant, date.toInstant());
    }

    /*** Ensure that defaultZoneId is respected when no zone supplied ***/
    @Test
    public void testParseDate_DefaultZoneApplied() {
        ZoneId pacific = ZoneId.of("America/Los_Angeles");
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15", pacific, true);
        assertEquals(pacific, zdt.getZone());
    }

    /*** Verify that getTimeZone correctly interprets GMT and offset strings ***/
    @Test
    public void testGetTimeZone_GmtAndOffset() throws Exception {
        // Access via parseDate to indirectly test getTimeZone
        ZonedDateTime gmt = DateUtilities.parseDate("2024-01-15 00:00 GMT", utcZone, true);
        assertEquals(ZoneId.of("Etc/GMT"), gmt.getZone());

        ZonedDateTime offset = DateUtilities.parseDate("2024-01-15 00:00 -05:00", utcZone, true);
        assertEquals(ZoneId.of("GMT-05:00"), offset.getZone());
    }

    /*** Verify that unknown timezone abbreviation falls back to default in non‑strict mode ***/
    @Test
    public void testParseDate_UnknownAbbreviation_NonStrictFallsBack() {
        // "XYZ" is not in the map; with strict mode it would throw, non‑strict should ignore
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 12:00 XYZ", utcZone, false);
        assertEquals(utcZone, zdt.getZone());
    }

    /*** Verify that unknown timezone abbreviation throws in strict mode ***/
    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_UnknownAbbreviation_StrictThrows() {
        DateUtilities.parseDate("2024-01-15 12:00 XYZ", utcZone, true);
    }

    /*** Verify that day-of-week is ignored ***/
    @Test
    public void testParseDate_DayOfWeekIgnored() {
        ZonedDateTime zdt = DateUtilities.parseDate("Monday 2024-01-15", utcZone, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
    }

    /*** Verify that whitespace variations are accepted ***/
    @Test
    public void testParseDate_VariousSeparatorsAndWhitespace() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024 . 01 . 15   13 : 30 : 45", utcZone, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(45, zdt.getSecond());
    }
}
