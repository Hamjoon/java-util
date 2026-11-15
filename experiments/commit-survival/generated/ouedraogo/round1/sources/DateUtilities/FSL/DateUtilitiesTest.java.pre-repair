package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import org.junit.Test;

public class DateUtilitiesTest {

    // ----------------------------------------------------------------------
    //  Helper methods for assertions
    // ----------------------------------------------------------------------
    private static void assertZonedEquals(ZonedDateTime expected, ZonedDateTime actual) {
        assertEquals("Year mismatch", expected.getYear(), actual.getYear());
        assertEquals("Month mismatch", expected.getMonthValue(), actual.getMonthValue());
        assertEquals("Day mismatch", expected.getDayOfMonth(), actual.getDayOfMonth());
        assertEquals("Hour mismatch", expected.getHour(), actual.getHour());
        assertEquals("Minute mismatch", expected.getMinute(), actual.getMinute());
        assertEquals("Second mismatch", expected.getSecond(), actual.getSecond());
        assertEquals("Nano mismatch", expected.getNano(), actual.getNano());
        assertEquals("ZoneId mismatch", expected.getZone(), actual.getZone());
    }

    // ----------------------------------------------------------------------
    //  parseDate(String) – null / empty handling
    // ----------------------------------------------------------------------
    @Test
    public void testParseDate_NullOrEmpty_ReturnsNull() {
        assertNull(DateUtilities.parseDate(null));
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("   "));
    }

    // ----------------------------------------------------------------------
    //  Epoch millisecond parsing
    // ----------------------------------------------------------------------
    @Test
    public void testParseDate_EpochMillis() {
        // 0 = 1970-01-01T00:00:00Z
        Date d0 = DateUtilities.parseDate("0");
        assertNotNull(d0);
        ZonedDateTime z0 = ZonedDateTime.ofInstant(d0.toInstant(), ZoneId.systemDefault());
        assertEquals(1970, z0.getYear());
        assertEquals(1, z0.getMonthValue());
        assertEquals(1, z0.getDayOfMonth());

        // Some arbitrary epoch value
        String epochStr = "1609459200000"; // 2021-01-01T00:00:00Z
        Date d = DateUtilities.parseDate(epochStr);
        ZonedDateTime z = ZonedDateTime.ofInstant(d.toInstant(), ZoneId.of("UTC"));
        assertEquals(2021, z.getYear());
        assertEquals(1, z.getMonthValue());
        assertEquals(1, z.getDayOfMonth());
        assertEquals(0, z.getHour());
    }

    // ----------------------------------------------------------------------
    //  ISO date patterns (yyyy/MM/dd and mm/dd/yyyy)
    // ----------------------------------------------------------------------
    @Test
    public void testParseDate_IsoYearFirst() {
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 21, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime actual = DateUtilities.parseDate("2024/01/21", ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    @Test
    public void testParseDate_IsoYearFirst_WithHyphen() {
        ZonedDateTime expected = ZonedDateTime.of(1999, 12, 31, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime actual = DateUtilities.parseDate("1999-12-31", ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    @Test
    public void testParseDate_IsoMonthFirst() {
        ZonedDateTime expected = ZonedDateTime.of(2025, 7, 4, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime actual = DateUtilities.parseDate("07/04/2025", ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    // ----------------------------------------------------------------------
    //  Alpha month patterns with ordinals and various commas / spaces
    // ----------------------------------------------------------------------
    @Test
    public void testParseDate_AlphaMonth_WithOrdinalAndComma() {
        ZonedDateTime expected = ZonedDateTime.of(2023, 3, 5, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime actual = DateUtilities.parseDate("Mar 5th, 2023", ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    @Test
    public void testParseDate_AlphaMonth_DayFirst() {
        ZonedDateTime expected = ZonedDateTime.of(2022, 11, 22, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime actual = DateUtilities.parseDate("22 Nov 2022", ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    @Test
    public void testParseDate_AlphaMonth_YearFirst() {
        ZonedDateTime expected = ZonedDateTime.of(2020, 12, 31, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime actual = DateUtilities.parseDate("2020 December 31st", ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    // ----------------------------------------------------------------------
    //  Unix style date with day-of-week and timezone abbreviation
    // ----------------------------------------------------------------------
    @Test
    public void testParseDate_UnixStyle_WithTZAbbreviation() {
        // "Sat Jan 6 11:06:10 EST 2024"
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 6, 11, 6, 10, 0,
                ZoneId.of("America/New_York")); // EST maps to America/New_York
        ZonedDateTime actual = DateUtilities.parseDate("Sat Jan 6 11:06:10 EST 2024",
                ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    // ----------------------------------------------------------------------
    //  Time only (hh:mm) with default zone
    // ----------------------------------------------------------------------
    @Test
    public void testParseDate_DateWithTime_HourMinute() {
        ZonedDateTime expected = ZonedDateTime.of(2021, 5, 15, 14, 30, 0, 0, ZoneId.systemDefault());
        ZonedDateTime actual = DateUtilities.parseDate("2021-05-15 14:30", ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    // ----------------------------------------------------------------------
    //  Time with seconds and fractional seconds
    // ----------------------------------------------------------------------
    @Test
    public void testParseDate_TimeWithFractionalSeconds() {
        ZonedDateTime expected = ZonedDateTime.of(2022, 8, 1, 9, 15, 45, 123_000_000, ZoneId.systemDefault());
        ZonedDateTime actual = DateUtilities.parseDate("2022/08/01 09:15:45.123", ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    // ----------------------------------------------------------------------
    //  Timezone offset formats
    // ----------------------------------------------------------------------
    @Test
    public void testParseDate_TimeWithOffset_HHMM() {
        // Offset +02:00
        ZonedDateTime expected = ZonedDateTime.of(2023, 3, 10, 12, 0, 0, 0, ZoneId.ofOffset("GMT", ZoneOffset.ofHours(2)));
        ZonedDateTime actual = DateUtilities.parseDate("2023-03-10 12:00+02", ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    @Test
    public void testParseDate_TimeWithOffset_HHMMSS() {
        // Offset -05:30
        ZonedDateTime expected = ZonedDateTime.of(2023, 3, 10, 12, 0, 0, 0,
                ZoneId.ofOffset("GMT", ZoneOffset.ofHoursMinutes(-5, -30)));
        ZonedDateTime actual = DateUtilities.parseDate("2023-03-10 12:00-05:30", ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    @Test
    public void testParseDate_TimeWithZSuffix() {
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
        ZonedDateTime actual = DateUtilities.parseDate("2024-01-01 00:00Z", ZoneId.of("America/Los_Angeles"), true);
        assertZonedEquals(expected, actual);
    }

    // ----------------------------------------------------------------------
    //  Named timezone handling (including bracketed form)
    // ----------------------------------------------------------------------
    @Test
    public void testParseDate_NamedTimezone() {
        ZonedDateTime expected = ZonedDateTime.of(2023, 12, 25, 10, 0, 0, 0, ZoneId.of("Asia/Tokyo"));
        ZonedDateTime actual = DateUtilities.parseDate("2023-12-25 10:00[Asia/Tokyo]",
                ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    @Test
    public void testParseDate_NamedTimezone_WithBracketsAndSpaces() {
        ZonedDateTime expected = ZonedDateTime.of(2023, 12, 25, 10, 0, 0, 0, ZoneId.of("Asia/Tokyo"));
        ZonedDateTime actual = DateUtilities.parseDate("2023-12-25 10:00 [Asia/Tokyo] ",
                ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    // ----------------------------------------------------------------------
    //  Ensure garbage detection (ensureDateTimeAlone = true)
    // ----------------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_GarbageAfterDateThrows() {
        DateUtilities.parseDate("2023-01-01 extra", ZoneId.systemDefault(), true);
    }

    @Test
    public void testParseDate_GarbageAllowedWhenFlagFalse() {
        // Should parse date portion and ignore trailing garbage because flag is false
        ZonedDateTime expected = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime actual = DateUtilities.parseDate("2023-01-01 extra", ZoneId.systemDefault(), false);
        assertZonedEquals(expected, actual);
    }

    // ----------------------------------------------------------------------
    //  Invalid month / day / hour values leading to IllegalArgumentException
    // ----------------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidMonth() {
        DateUtilities.parseDate("2023-13-01", ZoneId.systemDefault(), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidDay() {
        DateUtilities.parseDate("2023-02-30", ZoneId.systemDefault(), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidHour() {
        DateUtilities.parseDate("2023-01-01 25:00", ZoneId.systemDefault(), true);
    }

    // ----------------------------------------------------------------------
    //  Day‑of‑week ignored when present
    // ----------------------------------------------------------------------
    @Test
    public void testParseDate_DayOfWeekIgnored() {
        ZonedDateTime expected = ZonedDateTime.of(2024, 4, 15, 0, 0, 0, 0, ZoneId.systemDefault());
        ZonedDateTime actual = DateUtilities.parseDate("Monday 2024-04-15", ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    // ----------------------------------------------------------------------
    //  verify getTimeZone resolves offset, abbreviation and named zone correctly
    // ----------------------------------------------------------------------
    @Test
    public void testGetTimeZone_Offset() {
        // Directly exercising private method via reflection is not required – we can verify through parseDate
        ZonedDateTime expected = ZonedDateTime.of(2023, 5, 5, 12, 0, 0, 0,
                ZoneId.ofOffset("GMT", ZoneOffset.ofHoursMinutes(5, 30)));
        ZonedDateTime actual = DateUtilities.parseDate("2023-05-05 12:00+05:30",
                ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    @Test
    public void testGetTimeZone_AbbreviationMapped() {
        // "IST" should map to Asia/Kolkata (UTC+5:30)
        ZonedDateTime expected = ZonedDateTime.of(2023, 6, 1, 8, 0, 0, 0,
                ZoneId.of("Asia/Kolkata"));
        ZonedDateTime actual = DateUtilities.parseDate("2023-06-01 08:00 IST",
                ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    @Test
    public void testGetTimeZone_UnknownAbbreviationFallsBackToTimeZone() {
        // "GMT" is a known abbreviation but also a valid ZoneId; should resolve to Europe/London per map
        ZonedDateTime expected = ZonedDateTime.of(2023, 7, 1, 0, 0, 0, 0, ZoneId.of("Europe/London"));
        ZonedDateTime actual = DateUtilities.parseDate("2023-07-01 GMT",
                ZoneId.systemDefault(), true);
        assertZonedEquals(expected, actual);
    }

    // ----------------------------------------------------------------------
    //  parseDate(String) convenience method uses system default zone
    // ----------------------------------------------------------------------
    @Test
    public void testParseDate_ConvenienceMethodUsesSystemDefault() {
        ZoneId system = ZoneId.systemDefault();
        ZonedDateTime expected = ZonedDateTime.of(2022, 9, 9, 0, 0, 0, 0, system);
        Date d = DateUtilities.parseDate("2022-09-09");
        ZonedDateTime actual = ZonedDateTime.ofInstant(d.toInstant(), system);
        assertZonedEquals(expected, actual);
    }
}
