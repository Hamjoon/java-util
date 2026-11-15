package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DateUtilities}.
 *
 * The tests are organized as follows:
 *
 * 1. Basic public API validation (null/empty handling, epoch millis).
 * 2. Standard ISO / numeric date parsing.
 * 3. Alpha‑month formats (e.g. “Jan 21st, 2024”).
 * 4. Unix‑style format (e.g. “Sat Jan 6 11:06:10 EST 2024”).
 * 5. Time component handling (hour/minute/second/fractional seconds).
 * 6. Time‑zone handling (offsets, abbreviations, invalid zones).
 * 7. Edge‑case validation (invalid month/day/hour/minute/second, stray characters,
 *    time‑zone without a time component).
 *
 * All tests use only the public API of {@code DateUtilities} and the standard Java
 * classes that appear in the source file.
 */
public class DateUtilitiesTest {

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    /**
     * Convert a {@link ZonedDateTime} to the equivalent {@link Date} using the
     * same instant.  Used for comparing the {@code parseDate(String)} overload
     * with the {@code parseDate(String, ZoneId, boolean)} overload.
     */
    private static Date toDate(ZonedDateTime zdt) {
        return Date.from(zdt.toInstant());
    }

    /**
     * Helper that asserts two {@link ZonedDateTime} instances represent the
     * same instant and have the same zone.
     */
    private static void assertZonedDateTimeEquals(ZonedDateTime expected, ZonedDateTime actual) {
        assertEquals("Instant mismatch", expected.toInstant(), actual.toInstant());
        assertEquals("ZoneId mismatch", expected.getZone(), actual.getZone());
    }

    // -------------------------------------------------------------------------
    // 1. Basic API tests (null / empty / epoch millis)
    // -------------------------------------------------------------------------

    @Test
    public void testParseDate_NullString_ReturnsNull() {
        assertNull(DateUtilities.parseDate(null));
    }

    @Test
    public void testParseDate_EmptyString_ReturnsNull() {
        assertNull(DateUtilities.parseDate("   "));
    }

    @Test
    public void testParseDate_EpochMillis() {
        long epoch = 1_640_995_200_000L; // 2022‑01‑01T00:00:00Z
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch),
                ZoneId.systemDefault());
        ZonedDateTime actual = DateUtilities.parseDate(Long.toString(epoch),
                ZoneId.systemDefault(), true);
        assertZonedDateTimeEquals(expected, actual);
    }

    @Test
    public void testParseDate_EpochMillis_UsingConvenientMethod() {
        long epoch = 1_640_995_200_000L;
        Date expected = new Date(epoch);
        Date actual = DateUtilities.parseDate(Long.toString(epoch));
        assertEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // 2. ISO / numeric date parsing (yyyy‑mm‑dd, mm/dd/yyyy etc.)
    // -------------------------------------------------------------------------

    @Test
    public void testParseDate_IsoDate_NoTime_DefaultZone() {
        String input = "2024-03-15";
        ZoneId defaultZone = ZoneId.of("America/New_York");
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 15,
                0, 0, 0, 0, defaultZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, defaultZone, true);
        assertZonedDateTimeEquals(expected, actual);
    }

    @Test
    public void testParseDate_IsoDate_WithTimeAndOffset() {
        String input = "2024-03-15T13:45:30+02:00";
        // The offset should be interpreted as a zone offset, not as a region.
        ZoneId expectedZone = ZoneId.ofOffset("GMT", java.time.ZoneOffset.ofHours(2));
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 15,
                13, 45, 30, 0, expectedZone);
        ZonedDateTime actual = DateUtilities.parseDate(input,
                ZoneId.of("UTC"), true);
        assertZonedDateTimeEquals(expected, actual);
    }

    @Test
    public void testParseDate_NumericDate_WithSlashes() {
        String input = "12/31/2023";
        ZoneId defaultZone = ZoneId.of("Europe/London");
        ZonedDateTime expected = ZonedDateTime.of(2023, 12, 31,
                0, 0, 0, 0, defaultZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, defaultZone, true);
        assertZonedDateTimeEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // 3. Alpha‑month formats
    // -------------------------------------------------------------------------

    @Test
    public void testParseDate_MonthFirst_WithOrdinal() {
        String input = "January 6th, 2024";
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 6,
                0, 0, 0, 0, defaultZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, defaultZone, true);
        assertZonedDateTimeEquals(expected, actual);
    }

    @Test
    public void testParseDate_DayFirst_WithCommaAndTime() {
        String input = "6 Jan, 2024 14:30";
        ZoneId defaultZone = ZoneId.of("America/Los_Angeles");
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 6,
                14, 30, 0, 0, defaultZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, defaultZone, true);
        assertZonedDateTimeEquals(expected, actual);
    }

    @Test
    public void testParseDate_YearFirst_WithAbbreviationAndFractionalSec() {
        String input = "2024 Jan 6th 13:45:30.123";
        ZoneId defaultZone = ZoneId.of("Europe/Berlin");
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 6,
                13, 45, 30,
                // 0.123 seconds = 123_000_000 nanoseconds
                123_000_000,
                defaultZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, defaultZone, true);
        assertZonedDateTimeEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // 4. Unix‑style format
    // -------------------------------------------------------------------------

    @Test
    public void testParseDate_UnixStyle_WithAbbreviation() {
        String input = "Sat Jan 6 11:06:10 EST 2024";
        ZoneId expectedZone = ZoneId.of("America/New_York"); // EST maps to this zone
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 6,
                11, 6, 10, 0, expectedZone);
        ZonedDateTime actual = DateUtilities.parseDate(input,
                ZoneId.of("UTC"), true);
        assertZonedDateTimeEquals(expected, actual);
    }

    @Test
    public void testParseDate_UnixStyle_WithoutDayOfWeek() {
        String input = "Jan 6 11:06:10 EST 2024";
        ZoneId expectedZone = ZoneId.of("America/New_York");
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 6,
                11, 6, 10, 0, expectedZone);
        ZonedDateTime actual = DateUtilities.parseDate(input,
                ZoneId.of("UTC"), true);
        assertZonedDateTimeEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // 5. Time component edge cases (hour/minute/second/fraction)
    // -------------------------------------------------------------------------

    @Test
    public void testParseDate_TimeWithFractionalSeconds() {
        String input = "2024-03-01T12:34:56.789";
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 1,
                12, 34, 56,
                789_000_000,
                defaultZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, defaultZone, true);
        assertZonedDateTimeEquals(expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidHour_Throws() {
        // Hour 25 is out of range
        DateUtilities.parseDate("2024-01-01 25:00", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidMinute_Throws() {
        // Minute 61 is out of range
        DateUtilities.parseDate("2024-01-01 12:61", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidSecond_Throws() {
        // Second 70 is out of range
        DateUtilities.parseDate("2024-01-01 12:30:70", ZoneId.of("UTC"), true);
    }

    // -------------------------------------------------------------------------
    // 6. Time‑zone handling
    // -------------------------------------------------------------------------

    @Test
    public void testParseDate_TimeZoneOffset_WithoutTime_ShouldThrow() {
        // According to the implementation, a zone without a time component is illegal.
        try {
            DateUtilities.parseDate("2024-01-01+02:00", ZoneId.of("UTC"), true);
            fail("Expected IllegalArgumentException because zone appears without a time");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testParseDate_AbbreviationToZone_MappedCorrectly() {
        String input = "2024-01-01 12:00 PST";
        // PST maps to America/Los_Angeles per the static map
        ZoneId expectedZone = ZoneId.of("America/Los_Angeles");
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1,
                12, 0, 0, 0, expectedZone);
        ZonedDateTime actual = DateUtilities.parseDate(input,
                ZoneId.of("UTC"), true);
        assertZonedDateTimeEquals(expected, actual);
    }

    @Test
    public void testParseDate_InvalidAbbreviation_StrictMode_Throws() {
        // "XYZ" is not a known abbreviation nor a valid ZoneId
        try {
            DateUtilities.parseDate("2024-01-01 12:00 XYZ",
                    ZoneId.of("UTC"), true);
            fail("Expected exception for unknown timezone abbreviation in strict mode");
        } catch (Exception e) {
            // IllegalArgumentException or DateTimeException are acceptable;
            // the key is that an exception propagates.
        }
    }

    @Test
    public void testParseDate_InvalidAbbreviation_NonStrictMode_Ignores() {
        // In non‑strict mode the method should fall back to the default zone.
        ZoneId defaultZone = ZoneId.of("Europe/Paris");
        ZonedDateTime actual = DateUtilities.parseDate("2024-01-01 12:00 XYZ",
                defaultZone, false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1,
                12, 0, 0, 0, defaultZone);
        assertZonedDateTimeEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // 7. Garbage / stray characters handling
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_ExtraGarbage_StrictMode_Throws() {
        // The trailing "abc" should cause an exception when ensureDateTimeAlone = true
        DateUtilities.parseDate("2024-01-01 abc", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_ExtraGarbage_NonStrictMode_Ignores() {
        // In non‑strict mode the garbage is ignored.
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1,
                0, 0, 0, 0, defaultZone);
        ZonedDateTime actual = DateUtilities.parseDate("2024-01-01 abc",
                defaultZone, false);
        assertZonedDateTimeEquals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // 8. Month / day range validation
    // -------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidMonth_Throws() {
        DateUtilities.parseDate("2024-13-01", ZoneId.of("UTC"), true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidDay_Throws() {
        DateUtilities.parseDate("2024-02-30", ZoneId.of("UTC"), true);
    }

    // -------------------------------------------------------------------------
    // 9. Verify the convenience parseDate(String) returns a java.util.Date
    // -------------------------------------------------------------------------

    @Test
    public void testParseDate_ConvenientMethod_ReturnsCorrectDate() {
        String input = "2024-05-20 15:45:30";
        ZoneId systemDefault = ZoneId.systemDefault();
        ZonedDateTime expectedZdt = ZonedDateTime.of(2024, 5, 20,
                15, 45, 30, 0, systemDefault);
        Date expected = Date.from(expectedZdt.toInstant());

        Date actual = DateUtilities.parseDate(input);
        assertEquals(expected, actual);
    }
}
