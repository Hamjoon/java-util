package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.Date;

/**
 * JUnit‑4 test suite for {@link DateUtilities}.
 *
 * The test class is organised in three sections:
 * 1.  Public method signatures extracted from the source.
 * 2.  Basic “happy‑path” tests that verify normal functionality.
 * 3.  Edge‑case / exception tests that exercise the validation logic
 *     (null/empty inputs, malformed strings, out‑of‑range values,
 *      timezone handling, etc.).
 *
 * All tests are written using only the classes, constructors and methods
 * that are present in the supplied source code (plus standard JDK/JUnit
 * types that are allowed by the problem statement).  No mocking frameworks
 * are used.
 */
public class DateUtilitiesTest {

    /* -------------------------------------------------------------
     * 1.  Public methods of DateUtilities (extracted from source)
     * -------------------------------------------------------------
     *
     *  public static Date parseDate(String dateStr)
     *  public static ZonedDateTime parseDate(String dateStr,
     *                                         ZoneId defaultZoneId,
     *                                         boolean ensureDateTimeAlone)
     *
     * Both methods return {@code null} for a null or empty string and
     * throw {@code IllegalArgumentException} for malformed input when
     * {@code ensureDateTimeAlone} is {@code true}.
     */

    /* -------------------------------------------------------------
     * 2.  Basic functionality tests
     * ------------------------------------------------------------- */

    @Test
    public void testParseDate_NullOrEmpty_ReturnsNull() {
        Assert.assertNull(DateUtilities.parseDate(null));
        Assert.assertNull(DateUtilities.parseDate(""));
        Assert.assertNull(DateUtilities.parseDate("   "));
    }

    @Test
    public void testParseDate_EpochMillisString() {
        // 2022‑01‑01T00:00:00Z  ==  1640995200000 milliseconds since epoch
        String epochStr = "1640995200000";
        Date result = DateUtilities.parseDate(epochStr);
        Assert.assertNotNull(result);
        Instant expected = Instant.ofEpochMilli(1640995200000L);
        Assert.assertEquals(expected, result.toInstant());
    }

    @Test
    public void testParseDate_IsoDate_NoTime_UsesDefaultZone() {
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15", defaultZone, true);
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 15,
                0, 0, 0, 0, defaultZone);
        Assert.assertEquals(expected, zdt);
    }

    @Test
    public void testParseDate_IsoDate_WithTimeAndOffset() {
        // 2024‑01‑15 13:30:45 with offset +02:00
        String input = "2024-01-15 13:30:45+02:00";
        ZonedDateTime zdt = DateUtilities.parseDate(input,
                ZoneId.of("UTC"), true);
        ZoneId expectedZone = ZoneId.ofOffset("GMT", ZoneId.of("+02:00").getRules().getOffset(Instant.now()));
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 15,
                13, 30, 45, 0, expectedZone);
        Assert.assertEquals(expected, zdt);
    }

    @Test
    public void testParseDate_AlphaMonth_WithOrdinal() {
        // “January 6th, 2024” – should be parsed as 2024‑01‑06 at midnight
        ZonedDateTime zdt = DateUtilities.parseDate("January 6th, 2024",
                ZoneId.of("UTC"), true);
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 6,
                0, 0, 0, 0, ZoneId.of("UTC"));
        Assert.assertEquals(expected, zdt);
    }

    @Test
    public void testParseDate_UnixStyle_WithAbbreviation() {
        // “Tue Jan 15 14:30:00 EST 2024”
        String input = "Tue Jan 15 14:30:00 EST 2024";
        ZonedDateTime zdt = DateUtilities.parseDate(input,
                ZoneId.of("UTC"), true);
        ZoneId est = ZoneId.of("America/New_York"); // EST maps to this zone in the static map
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 15,
                14, 30, 0, 0, est);
        Assert.assertEquals(expected, zdt);
    }

    @Test
    public void testParseDate_FractionalSeconds() {
        // “2024-01-01 12:34:56.5” → 0.5 seconds = 500 000 000 nanos
        String input = "2024-01-01 12:34:56.5";
        ZonedDateTime zdt = DateUtilities.parseDate(input,
                ZoneId.of("UTC"), true);
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1,
                12, 34, 56, 500_000_000, ZoneId.of("UTC"));
        Assert.assertEquals(expected, zdt);
    }

    /* -------------------------------------------------------------
     * 3.  Edge‑case / exception tests
     * ------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_UnparseableString_ThrowsException() {
        DateUtilities.parseDate("not-a-date", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_ExtraGarbageWhenEnsureAlone_Throws() {
        // The string contains valid date plus stray characters “abc”.
        DateUtilities.parseDate("2024-01-01 abc", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_ExtraGarbageWhenNotEnsuringAlone_IgnoresGarbage() {
        // ensureDateTimeAlone = false → garbage is ignored
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-01 abc",
                ZoneId.of("UTC"), false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1,
                0, 0, 0, 0, ZoneId.of("UTC"));
        Assert.assertEquals(expected, zdt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidMonth_Throws() {
        // month 13 is out of range
        DateUtilities.parseDate("2024-13-01", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidDay_Throws() {
        // day 32 is out of range
        DateUtilities.parseDate("2024-01-32", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidHour_Throws() {
        // hour 24 is invalid (must be 0‑23)
        DateUtilities.parseDate("2024-01-01 24:00", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidMinute_Throws() {
        // minute 60 is invalid
        DateUtilities.parseDate("2024-01-01 23:60", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidSecond_Throws() {
        // second 60 is invalid
        DateUtilities.parseDate("2024-01-01 23:59:60", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_TimezoneOffset_PositiveAndNegative() {
        // Positive offset
        ZonedDateTime plus = DateUtilities.parseDate("2024-01-01 12:00+02:00",
                ZoneId.of("UTC"), true);
        ZoneId plusZone = ZoneId.ofOffset("GMT", ZoneId.of("+02:00").getRules().getOffset(Instant.now()));
        ZonedDateTime expectedPlus = ZonedDateTime.of(2024, 1, 1,
                12, 0, 0, 0, plusZone);
        Assert.assertEquals(expectedPlus, plus);

        // Negative offset
        ZonedDateTime minus = DateUtilities.parseDate("2024-01-01 12:00-05:00",
                ZoneId.of("UTC"), true);
        ZoneId minusZone = ZoneId.ofOffset("GMT", ZoneId.of("-05:00").getRules().getOffset(Instant.now()));
        ZonedDateTime expectedMinus = ZonedDateTime.of(2024, 1, 1,
                12, 0, 0, 0, minusZone);
        Assert.assertEquals(expectedMinus, minus);
    }

    @Test
    public void testParseDate_TimezoneAbbreviation_MappedCorrectly() {
        // “PST” maps to America/Los_Angeles according to the static map
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-01 08:00 PST",
                ZoneId.of("UTC"), true);
        ZoneId pst = ZoneId.of("America/Los_Angeles");
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1,
                8, 0, 0, 0, pst);
        Assert.assertEquals(expected, zdt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_UnknownAbbreviation_Throws() {
        // “XYZ” is not a known abbreviation nor a valid ZoneId
        DateUtilities.parseDate("2024-01-01 08:00 XYZ",
                ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_NullDefaultZoneId_Throws() {
        try {
            DateUtilities.parseDate("2024-01-01", null, true);
            Assert.fail("Expected an exception when defaultZoneId is null");
        } catch (Exception e) {
            // The library uses Convention.throwIfNull which throws an IllegalArgumentException
            // (or possibly NullPointerException) – we accept either.
            Assert.assertTrue(e instanceof IllegalArgumentException ||
                              e instanceof NullPointerException);
        }
    }

    @Test
    public void testParseDate_OriginalApi_DelegatesToMainApi() {
        // The original API should produce the same instant as the main API.
        String input = "2024-02-29 15:45:30"; // 2024 is a leap year, valid date
        Date viaOriginal = DateUtilities.parseDate(input);
        ZonedDateTime viaMain = DateUtilities.parseDate(input,
                ZoneId.systemDefault(), true);
        Assert.assertEquals(viaMain.toInstant(), viaOriginal.toInstant());
    }
}
