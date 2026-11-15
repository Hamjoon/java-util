package com.cedarsoftware.util;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DateUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic functionality of each public method.</li>
 *   <li>All supported date‑time formats (ISO, numeric with separators, month‑name formats, Unix style).</li>
 *   <li>Time specifications (offsets, named zones, fractional seconds).</li>
 *   <li>Edge cases such as null/empty input, epoch milliseconds, out‑of‑range values.</li>
 *   <li>Exception handling for invalid formats, invalid field values and illegal arguments.</li>
 * </ul>
 */
public class DateUtilitiesTest {

    /* ----------------------------------------------------------------------
     *  Public method signatures extracted from DateUtilities
     * ----------------------------------------------------------------------
     *
     *  1. public static Date parseDate(String dateStr)
     *
     *  2. public static ZonedDateTime parseDate(String dateStr,
     *                                            ZoneId defaultZoneId,
     *                                            boolean ensureDateTimeAlone)
     *
     * ---------------------------------------------------------------------- */

    /* -------------------------- 1. parseDate(String) -------------------------- */

    @Test
    public void testParseDate_NullOrEmpty_ReturnsNull() {
        assertNull(DateUtilities.parseDate(null));
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("   "));
    }

    @Test
    public void testParseDate_EpochMillis() {
        // "0" → 1970‑01‑01T00:00:00Z
        Date date = DateUtilities.parseDate("0");
        assertNotNull(date);
        assertEquals(Instant.ofEpochMilli(0L), date.toInstant());

        // a realistic epoch value
        String epochStr = "1640995200000"; // 2022‑01‑01T00:00:00Z
        Date epochDate = DateUtilities.parseDate(epochStr);
        assertEquals(Instant.ofEpochMilli(1640995200000L), epochDate.toInstant());
    }

    @Test
    public void testParseDate_ISO_YearMonthDay() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(0, zdt.getHour());
        assertEquals(0, zdt.getMinute());
        assertEquals(0, zdt.getSecond());
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
    }

    @Test
    public void testParseDate_ISO_WithTimeAndOffset() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15T13:45:30+02:00",
                ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(45, zdt.getMinute());
        assertEquals(30, zdt.getSecond());
        assertEquals(ZoneOffset.ofHours(2), zdt.getOffset());
    }

    @Test
    public void testParseDate_NumericWithSeparators_MMDDYYYY() {
        ZonedDateTime zdt = DateUtilities.parseDate("01/15/2024", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_AlphaMonth_DayFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("January 15, 2024", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_AlphaMonth_MonthFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("15 Jan 2024", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_AlphaMonth_YearFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024 Jan 15", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_UnixStyle_WithAbbreviation() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "Tue Jan 15 14:30:00 EST 2024", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(14, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(0, zdt.getSecond());
        // EST → America/New_York (standard offset -05:00)
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
    }

    @Test
    public void testParseDate_TimeWithNamedZone() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-15 13:45 PST", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(45, zdt.getMinute());
        // PST maps to America/Los_Angeles (offset -08:00 in winter)
        assertEquals(ZoneId.of("America/Los_Angeles"), zdt.getZone());
    }

    @Test
    public void testParseDate_FractionalSeconds() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-15T13:45:30.123456789+00:00", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(45, zdt.getMinute());
        assertEquals(30, zdt.getSecond());
        assertEquals(123_456_789, zdt.getNano());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidMonth_ThrowsException() {
        // month 13 is out of range
        DateUtilities.parseDate("2024-13-01", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidDay_ThrowsException() {
        // day 32 is out of range
        DateUtilities.parseDate("2024-01-32", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidHour_ThrowsException() {
        // hour 24 is invalid (must be 0‑23)
        DateUtilities.parseDate("2024-01-15T24:00", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidMinute_ThrowsException() {
        DateUtilities.parseDate("2024-01-15T23:60", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidSecond_ThrowsException() {
        DateUtilities.parseDate("2024-01-15T23:59:60", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_EnsureDateTimeAlone_ExtraGarbage() {
        // extra trailing text should cause an exception when ensureDateTimeAlone == true
        DateUtilities.parseDate("2024-01-15 extra", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_EnsureDateTimeAlone_ExtraGarbageIgnoredWhenFalse() {
        // same string but with ensureDateTimeAlone == false → parsing succeeds,
        // garbage is simply ignored.
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 extra",
                ZoneId.of("UTC"), false);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_NullZoneId_ThrowsException() {
        DateUtilities.parseDate("2024-01-15", null, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidTimezoneString_ThrowsException() {
        // "XYZ" is not a known zone, offset, or abbreviation
        DateUtilities.parseDate("2024-01-15 XYZ", ZoneId.of("UTC"), true);
    }

    /* -------------------------- 2. parseDate(String, ZoneId, boolean) -------------------------- */

    @Test
    public void testParseDate_WithDefaultZoneWhenNoTZProvided() {
        // No explicit zone in the string → defaultZoneId must be used.
        ZoneId defaultZone = ZoneId.of("America/New_York");
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15", defaultZone, true);
        assertEquals(defaultZone, zdt.getZone());
    }

    @Test
    public void testParseDate_EpochMillis_WithDefaultZone() {
        // Even when a default zone is supplied, an all‑digit string is treated as epoch ms.
        ZoneId defaultZone = ZoneId.of("Europe/London");
        ZonedDateTime zdt = DateUtilities.parseDate("0", defaultZone, true);
        assertEquals(Instant.EPOCH, zdt.toInstant());
        assertEquals(defaultZone, zdt.getZone());
    }

    @Test
    public void testParseDate_TimezoneOffsetPositive() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-15 10:00 +03:00", ZoneId.of("UTC"), true);
        assertEquals(ZoneOffset.ofHours(3), zdt.getOffset());
        assertEquals(10, zdt.getHour());
    }

    @Test
    public void testParseDate_TimezoneOffsetNegativeNoColon() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-15 10:00 -0500", ZoneId.of("UTC"), true);
        assertEquals(ZoneOffset.ofHours(-5), zdt.getOffset());
        assertEquals(10, zdt.getHour());
    }

    @Test
    public void testParseDate_TimezoneBracketedName() {
        // Zone name may be surrounded by brackets.
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-15 12:00 [America/Los_Angeles]", ZoneId.of("UTC"), true);
        assertEquals(ZoneId.of("America/Los_Angeles"), zdt.getZone());
    }

    @Test
    public void testParseDate_ComplexMixedOrder_TimeBeforeDate() {
        // Time appears before the date portion.
        ZonedDateTime zdt = DateUtilities.parseDate(
                "13:30:45 2024/01/15", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(45, zdt.getSecond());
    }

    @Test
    public void testParseDate_DayOfWeekIgnored() {
        // Day of week should be ignored for calculation.
        ZonedDateTime zdt = DateUtilities.parseDate(
                "Friday, 2024-01-15", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
    }
}
