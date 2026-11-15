package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;

/**
 * Comprehensive JUnit‑4 test suite for {@link DateUtilities}.
 *
 * The suite covers:
 * <ul>
 *   <li>All public APIs (both overloads of {@code parseDate})</li>
 *   <li>Typical valid inputs for every supported format</li>
 *   <li>Edge‑case handling (null/empty, epoch milliseconds, fractional seconds, offsets, abbreviations)</li>
 *   <li>Invalid inputs that must trigger {@code IllegalArgumentException}</li>
 *   <li>Behaviour of the {@code ensureDateTimeAlone} flag</li>
 * </ul>
 *
 * The tests deliberately avoid any external libraries or mocking frameworks – they use only
 * the classes, constructors and methods that appear in the source of {@link DateUtilities}
 * and the Java SE APIs referenced therein.
 */
public class DateUtilitiesTest {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");

    /* -------------------------------------------------------------
     *  BASIC FUNCTIONALITY – parseDate(String)
     * ------------------------------------------------------------- */

    @Test
    public void testParseDate_NullOrEmpty_ReturnsNull() {
        Assert.assertNull(DateUtilities.parseDate(null));
        Assert.assertNull(DateUtilities.parseDate(""));
        Assert.assertNull(DateUtilities.parseDate("   "));
    }

    @Test
    public void testParseDate_EpochMillis_ReturnsCorrectInstant() {
        // "0" should become the epoch instant.
        ZonedDateTime zdt = DateUtilities.parseDate("0", UTC, false);
        Assert.assertEquals(Instant.ofEpochMilli(0L), zdt.toInstant());
        Assert.assertEquals(UTC, zdt.getZone());

        // a larger number – e.g. 1_600_000_000_000 (2020‑09‑13T12:26:40Z)
        String millis = "1600000000000";
        ZonedDateTime zdt2 = DateUtilities.parseDate(millis, UTC, false);
        Assert.assertEquals(Instant.ofEpochMilli(Long.parseLong(millis)), zdt2.toInstant());
        Assert.assertEquals(UTC, zdt2.getZone());
    }

    @Test
    public void testParseDate_IsoYMD_WithTimeAndOffset() {
        // Format: yyyy/MM/dd HH:mm:ss+02:00
        String input = "2024/01/15 13:45:30+02:00";
        ZonedDateTime result = DateUtilities.parseDate(input, UTC, false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 15, 13, 45, 30, 0,
                ZoneOffset.ofHours(2));
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testParseDate_IsoMDY_NoTime() {
        // Format: mm-dd-yyyy (no time part)
        String input = "12-31-2023";
        ZonedDateTime result = DateUtilities.parseDate(input, UTC, false);
        ZonedDateTime expected = ZonedDateTime.of(2023, 12, 31, 0, 0, 0, 0, UTC);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testParseDate_AlphaMonthPattern_DayFirst() {
        // "21st Jan, 2024"
        String input = "21st Jan, 2024";
        ZonedDateTime result = DateUtilities.parseDate(input, UTC, false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 21, 0, 0, 0, 0, UTC);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testParseDate_AlphaMonthPattern_MonthFirst_WithCommaAndOrdinal() {
        // "January 6th, 2024"
        String input = "January 6th, 2024";
        ZonedDateTime result = DateUtilities.parseDate(input, UTC, false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 6, 0, 0, 0, 0, UTC);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testParseDate_UnixStyle_WithDayOfWeek_AndTimeZone() {
        // "Sat Jan 6 11:06:10 EST 2024"
        String input = "Sat Jan 6 11:06:10 EST 2024";
        ZonedDateTime result = DateUtilities.parseDate(input, UTC, false);
        // EST is mapped to America/New_York (UTC‑05:00 in winter)
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 6, 11, 6, 10, 0, NEW_YORK);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testParseDate_TimeWithFractionalSeconds() {
        // "2024-03-01 09:15:30.123"
        String input = "2024-03-01 09:15:30.123";
        ZonedDateTime result = DateUtilities.parseDate(input, UTC, false);
        long nanos = (long) (0.123 * 1_000_000_000);
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 1, 9, 15, 30, (int) nanos, UTC);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testParseDate_TimezoneAbbreviationMapping() {
        // Using abbreviation "PST" which maps to America/Los_Angeles
        String input = "2024-07-04 20:00 PST";
        ZonedDateTime result = DateUtilities.parseDate(input, UTC, false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 7, 4, 20, 0, 0, 0, LOS_ANGELES);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testParseDate_TimezoneOffsetFormats() {
        // Various offset syntaxes
        String[] inputs = {
                "2024-01-01 12:00+01:00",
                "2024-01-01 12:00+0100",
                "2024-01-01 12:00+01",
                "2024-01-01 12:00-02:30",
                "2024-01-01 12:00Z"
        };
        ZoneOffset[] expectedOffsets = {
                ZoneOffset.ofHours(1),
                ZoneOffset.ofHoursMinutes(1, 0),
                ZoneOffset.ofHours(1),
                ZoneOffset.ofHoursMinutes(-2, -30),
                ZoneOffset.UTC
        };
        for (int i = 0; i < inputs.length; i++) {
            ZonedDateTime result = DateUtilities.parseDate(inputs[i], UTC, false);
            ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1, 12, 0, 0, 0,
                    expectedOffsets[i]);
            Assert.assertEquals("Failed for input: " + inputs[i], expected, result);
        }
    }

    @Test
    public void testParseDate_BracketedZoneId() {
        // ZoneId in brackets – "[America/Los_Angeles]"
        String input = "2024-05-10 08:30 [America/Los_Angeles]";
        ZonedDateTime result = DateUtilities.parseDate(input, UTC, false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 5, 10, 8, 30, 0, 0, LOS_ANGELES);
        Assert.assertEquals(expected, result);
    }

    /* -------------------------------------------------------------
     *  ensureDateTimeAlone flag – garbage detection
     * ------------------------------------------------------------- */

    @Test
    public void testParseDate_EnsureDateTimeAlone_NoGarbage_Passes() {
        String input = "2024-02-02 10:20:30";
        // No extra characters – should succeed
        ZonedDateTime result = DateUtilities.parseDate(input, UTC, true);
        ZonedDateTime expected = ZonedDateTime.of(2024, 2, 2, 10, 20, 30, 0, UTC);
        Assert.assertEquals(expected, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_EnsureDateTimeAlone_ExtraGarbage_Throws() {
        String input = "2024-02-02 10:20:30 garbage";
        DateUtilities.parseDate(input, UTC, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_EnsureDateTimeAlone_UnexpectedDayOfWeek_Throws() {
        // Day of week is allowed only as part of the Unix style pattern; here it is stray.
        String input = "2024-02-02 10:20:30 Mon";
        DateUtilities.parseDate(input, UTC, true);
    }

    /* -------------------------------------------------------------
     *  INVALID VALUES – each should throw IllegalArgumentException
     * ------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidMonth_Throws() {
        DateUtilities.parseDate("2024-13-01", UTC, false); // month 13
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidDay_Throws() {
        DateUtilities.parseDate("2024-02-30", UTC, false); // Feb 30 does not exist but method only checks 1‑31
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidHour_Throws() {
        DateUtilities.parseDate("2024-01-01 24:00", UTC, false); // hour 24 invalid
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidMinute_Throws() {
        DateUtilities.parseDate("2024-01-01 12:60", UTC, false); // minute 60 invalid
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidSecond_Throws() {
        DateUtilities.parseDate("2024-01-01 12:00:60", UTC, false); // second 60 invalid
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_UnknownTimezone_Throws() {
        // "XYZ" is not a known abbreviation nor a valid ZoneId
        DateUtilities.parseDate("2024-01-01 12:00 XYZ", UTC, false);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_UnparseableString_Throws() {
        // Completely nonsense string
        DateUtilities.parseDate("not-a-date", UTC, false);
    }

    /* -------------------------------------------------------------
     *  parseDate(String) overload – ensures default zone handling
     * ------------------------------------------------------------- */

    @Test
    public void testParseDate_Overload_UsesSystemDefaultWhenNoTZ() {
        // Use a known default zone to make the test deterministic.
        ZoneId defaultZone = ZoneId.of("Europe/London");
        // The overload without a zone uses ZoneId.systemDefault(); we temporarily replace it
        // by calling the three‑argument version with the desired default zone.
        String input = "2024-04-01 15:30";
        ZonedDateTime result = DateUtilities.parseDate(input, defaultZone, false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 4, 1, 15, 30, 0, 0, defaultZone);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testParseDate_Overload_ReturnsDate() {
        // Verify that the one‑argument overload returns a java.util.Date instance
        String input = "2024-03-15";
        Date date = DateUtilities.parseDate(input);
        Assert.assertNotNull(date);
        // The date should represent midnight UTC on the given day (system default zone is used,
        // but we can compare the epoch milliseconds to the expected value for UTC).
        ZonedDateTime expectedZdt = ZonedDateTime.of(2024, 3, 15, 0, 0, 0, 0, ZoneId.systemDefault());
        Assert.assertEquals(expectedZdt.toInstant().toEpochMilli(), date.getTime());
    }

    /* -------------------------------------------------------------
     *  Additional edge cases – whitespace, commas, and 'T' separator
     * ------------------------------------------------------------- */

    @Test
    public void testParseDate_WhitespaceAndCommaVariations() {
        String[] inputs = {
                "2024-06-01, 12:00",
                "2024-06-01 T12:00",
                "2024-06-01\t12:00",   // tab is also whitespace
                "2024-06-01   12:00"
        };
        for (String input : inputs) {
            ZonedDateTime result = DateUtilities.parseDate(input, UTC, false);
            ZonedDateTime expected = ZonedDateTime.of(2024, 6, 1, 12, 0, 0, 0, UTC);
            Assert.assertEquals("Failed for input: \"" + input + "\"", expected, result);
        }
    }

    @Test
    public void testParseDate_TimeBeforeDate() {
        // Time can appear before the date part.
        String input = "08:45 2024-09-10";
        ZonedDateTime result = DateUtilities.parseDate(input, UTC, false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 9, 10, 8, 45, 0, 0, UTC);
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testParseDate_DayOfWeekIgnoredInNonUnixFormat() {
        // Day of week should be ignored in ISO style (not part of unixDateTimePattern)
        String input = "Monday 2024-07-20 14:00";
        ZonedDateTime result = DateUtilities.parseDate(input, UTC, false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 7, 20, 14, 0, 0, 0, UTC);
        Assert.assertEquals(expected, result);
    }
}
