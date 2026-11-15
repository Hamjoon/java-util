package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.Date;

/**
 * Comprehensive JUnit‑4 test suite for {@link DateUtilities}.
 *
 * The suite is organized as if three independent experts each contributed one test
 * per public method.  All typical, edge‑case and error scenarios are covered.
 */
public class DateUtilitiesTest {

    /* ----------------------------------------------------------------------
     *  Tests for the original API: DateUtilities.parseDate(String)
     * ---------------------------------------------------------------------- */

    @Test
    public void testParseDate_NullOrEmpty_ReturnsNull() {
        Assert.assertNull(DateUtilities.parseDate(null));
        Assert.assertNull(DateUtilities.parseDate(""));
        Assert.assertNull(DateUtilities.parseDate("   "));
    }

    @Test
    public void testParseDate_EpochMilliseconds() {
        long now = System.currentTimeMillis();
        Date d = DateUtilities.parseDate(Long.toString(now));
        Assert.assertNotNull(d);
        Assert.assertEquals(now, d.getTime());
    }

    @Test
    public void testParseDate_ISODate_NoTime_UsesSystemDefaultZone() {
        // 2024-01-15 (no time) → midnight at system default zone
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 15, 0, 0, 0, 0,
                ZoneId.systemDefault());
        Date actual = DateUtilities.parseDate("2024-01-15");
        Assert.assertEquals(Date.from(expected.toInstant()), actual);
    }

    @Test
    public void testParseDate_ISODate_WithTimeAndOffset() {
        // 2024-01-15 13:30:45.123+02:00
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-15 13:30:45.123+02:00",
                ZoneId.of("UTC"),
                true);
        Assert.assertEquals(2024, zdt.getYear());
        Assert.assertEquals(1, zdt.getMonthValue());
        Assert.assertEquals(15, zdt.getDayOfMonth());
        Assert.assertEquals(13, zdt.getHour());
        Assert.assertEquals(30, zdt.getMinute());
        Assert.assertEquals(45, zdt.getSecond());
        // offset +02:00 → zone ID "GMT+02:00"
        Assert.assertEquals(ZoneId.of("GMT+02:00"), zdt.getZone());
    }

    @Test
    public void testParseDate_AlphaMonth_DayFirst_WithOrdinal() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "15th January 2024",
                ZoneId.of("America/New_York"),
                true);
        Assert.assertEquals(2024, zdt.getYear());
        Assert.assertEquals(1, zdt.getMonthValue());
        Assert.assertEquals(15, zdt.getDayOfMonth());
        Assert.assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
    }

    @Test
    public void testParseDate_UnixStyle_WithAbbreviation() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "Tue Jan 15 13:30:00 EST 2024",
                ZoneId.of("UTC"),
                true);
        Assert.assertEquals(2024, zdt.getYear());
        Assert.assertEquals(1, zdt.getMonthValue());
        Assert.assertEquals(15, zdt.getDayOfMonth());
        Assert.assertEquals(13, zdt.getHour());
        Assert.assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidDateThrows() {
        // month 13 does not exist → should throw from getDate()
        DateUtilities.parseDate("2024-13-01", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_TimeZoneWithoutTimeThrows() {
        // Zone without accompanying time is illegal per implementation
        DateUtilities.parseDate("2024-01-01+02:00", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_ExtraGarbageStrictMode() {
        // ensureDateTimeAlone = true → leftover characters cause exception
        DateUtilities.parseDate("2024-01-01 abc", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_ExtraGarbageLenientMode_IgnoresGarbage() {
        // ensureDateTimeAlone = false → garbage after a valid date is ignored
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-01 abc",
                ZoneId.of("UTC"),
                false);
        Assert.assertEquals(2024, zdt.getYear());
        Assert.assertEquals(1, zdt.getMonthValue());
        Assert.assertEquals(1, zdt.getDayOfMonth());
        Assert.assertEquals(ZoneId.of("UTC"), zdt.getZone());
    }

    /* ----------------------------------------------------------------------
     *  Tests for the main API: DateUtilities.parseDate(String, ZoneId, boolean)
     * ---------------------------------------------------------------------- */

    @Test
    public void testParseDate_WithBracketedZoneId() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-15 13:30 [Europe/Paris]",
                ZoneId.of("UTC"),
                true);
        Assert.assertEquals(ZoneId.of("Europe/Paris"), zdt.getZone());
        Assert.assertEquals(13, zdt.getHour());
        Assert.assertEquals(30, zdt.getMinute());
    }

    @Test
    public void testParseDate_WithNamedOffsetAndFractionalSeconds() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-15T13:30:45.123456-05:00",
                ZoneId.of("UTC"),
                true);
        // -05:00 → GMT-05:00
        Assert.assertEquals(ZoneId.of("GMT-05:00"), zdt.getZone());
        Assert.assertEquals(13, zdt.getHour());
        Assert.assertEquals(30, zdt.getMinute());
        Assert.assertEquals(45, zdt.getSecond());
        // 0.123456 seconds → 123_456_000 nanoseconds
        Assert.assertEquals(123_456_000, zdt.getNano());
    }

    @Test
    public void testParseDate_WithAbbreviationMappedToRegion() {
        // PST should map to America/Los_Angeles per ABBREVIATION_TO_TIMEZONE
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-01 12:00 PST",
                ZoneId.of("UTC"),
                true);
        Assert.assertEquals(ZoneId.of("America/Los_Angeles"), zdt.getZone());
    }

    @Test
    public void testParseDate_InvalidAbbreviationLenientUsesDefaultZone() {
        // "XYZ" is not a known abbreviation – in lenient mode the parser falls back to the default zone
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-01 12:00 XYZ",
                ZoneId.of("Europe/Paris"),
                false);
        Assert.assertEquals(ZoneId.of("Europe/Paris"), zdt.getZone());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidAbbreviationStrictThrows() {
        // In strict mode the unknown abbreviation propagates as exception
        DateUtilities.parseDate(
                "2024-01-01 12:00 XYZ",
                ZoneId.of("UTC"),
                true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_HourOutOfRange() {
        DateUtilities.parseDate("2024-01-01 24:00", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_MinuteOutOfRange() {
        DateUtilities.parseDate("2024-01-01 23:60", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_SecondOutOfRange() {
        DateUtilities.parseDate("2024-01-01 23:59:60", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_DayOutOfRange() {
        DateUtilities.parseDate("2024-02-30", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_NumericMonthFirst_WithDifferentSeparators() {
        // hyphen
        ZonedDateTime hyphen = DateUtilities.parseDate(
                "01-15-2024",
                ZoneId.of("UTC"),
                true);
        // slash
        ZonedDateTime slash = DateUtilities.parseDate(
                "01/15/2024",
                ZoneId.of("UTC"),
                true);
        // dot
        ZonedDateTime dot = DateUtilities.parseDate(
                "01.15.2024",
                ZoneId.of("UTC"),
                true);
        Assert.assertEquals(15, hyphen.getDayOfMonth());
        Assert.assertEquals(15, slash.getDayOfMonth());
        Assert.assertEquals(15, dot.getDayOfMonth());
        Assert.assertEquals(1, hyphen.getMonthValue());
        Assert.assertEquals(1, slash.getMonthValue());
        Assert.assertEquals(1, dot.getMonthValue());
    }

    @Test
    public void testParseDate_YearFirst_WithMonthName() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024 January 31st",
                ZoneId.of("UTC"),
                true);
        Assert.assertEquals(2024, zdt.getYear());
        Assert.assertEquals(1, zdt.getMonthValue());
        Assert.assertEquals(31, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_TimeBeforeDate() {
        // time appears before date, e.g., "13:45 2024-03-10"
        ZonedDateTime zdt = DateUtilities.parseDate(
                "13:45 2024-03-10",
                ZoneId.of("UTC"),
                true);
        Assert.assertEquals(13, zdt.getHour());
        Assert.assertEquals(45, zdt.getMinute());
        Assert.assertEquals(2024, zdt.getYear());
        Assert.assertEquals(3, zdt.getMonthValue());
        Assert.assertEquals(10, zdt.getDayOfMonth());
    }

    /* ----------------------------------------------------------------------
     *  Helper validation for internal conversion (indirectly exercised)
     * ---------------------------------------------------------------------- */

    @Test
    public void testParseDate_FractionToNanosConversion() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-01 00:00:00.999999999",
                ZoneId.of("UTC"),
                true);
        // 0.999999999 seconds = 999_999_999 nanoseconds
        Assert.assertEquals(999_999_999, zdt.getNano());
    }

    @Test
    public void testParseDate_ZeroFractionIsHandled() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-01 00:00:00.0",
                ZoneId.of("UTC"),
                true);
        Assert.assertEquals(0, zdt.getNano());
    }

    @Test
    public void testParseDate_GMTExplicitIsNormalized() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-01 12:00 GMT",
                ZoneId.of("UTC"),
                true);
        Assert.assertEquals(ZoneId.of("Etc/GMT"), zdt.getZone());
    }

    @Test
    public void testParseDate_OffsetWithoutColon() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-01 12:00 +0200",
                ZoneId.of("UTC"),
                true);
        Assert.assertEquals(ZoneId.of("GMT+02:00"), zdt.getZone());
    }

    @Test
    public void testParseDate_ShortHourOffset() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-01 12:00 -5",
                ZoneId.of("UTC"),
                true);
        Assert.assertEquals(ZoneId.of("GMT-05:00"), zdt.getZone());
    }
}
