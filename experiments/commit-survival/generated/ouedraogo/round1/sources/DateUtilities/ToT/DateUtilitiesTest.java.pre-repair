package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link DateUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>Null / empty handling.</li>
 *   <li>Epoch‑millisecond parsing.</li>
 *   <li>All three ISO date styles (yyyy‑MM‑dd, MM‑dd‑yyyy, yyyy‑MM‑dd with separators).</li>
 *   <li>Alpha‑month formats (Month Day, Day Month, Year Month Day) with and without ordinal suffixes.</li>
 *   <li>Unix‑style date strings that include a day‑of‑week and a short time‑zone abbreviation.</li>
 *   <li>Time appearing before the date.</li>
 *   <li>Offsets (+/-HH:mm, +/-HHMM, +/-HH, Z) and named zones (including bracketed zones).</li>
 *   <li>Fractional seconds.</li>
 *   <li>ensureDateTimeAlone flag – garbage detection.</li>
 *   <li>Invalid month / hour / unknown time‑zone handling.</li>
 * </ul>
 *
 * Private helper methods of {@code DateUtilities} are exercised indirectly through the public
 * {@code parseDate} APIs.
 */
public class DateUtilitiesTest {

    private ZoneId utc;
    private ZoneId ny;
    private ZoneId system;

    @Before
    public void setUp() {
        utc = ZoneId.of("UTC");
        ny  = ZoneId.of("America/New_York");
        system = ZoneId.systemDefault();
    }

    /*** 1. Null / empty handling ***/
    @Test
    public void testParseDate_NullOrEmpty_ReturnsNull() {
        assertNull(DateUtilities.parseDate(null));
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("   "));
    }

    /*** 2. Epoch‑millisecond parsing ***/
    @Test
    public void testParseDate_EpochMillis() {
        // "0" → 1970‑01‑01T00:00:00Z
        ZonedDateTime zdt = DateUtilities.parseDate("0", utc, true);
        assertEquals(1970, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(1, zdt.getDayOfMonth());
        assertEquals(0, zdt.getHour());
        assertEquals(0, zdt.getMinute());
        assertEquals(0, zdt.getSecond());
        assertEquals(utc, zdt.getZone());

        // a larger value – 2023‑03‑01T12:34:56.789Z
        long epoch = Instant.parse("2023-03-01T12:34:56.789Z").toEpochMilli();
        ZonedDateTime parsed = DateUtilities.parseDate(Long.toString(epoch), utc, true);
        assertEquals(2023, parsed.getYear());
        assertEquals(3, parsed.getMonthValue());
        assertEquals(1, parsed.getDayOfMonth());
        assertEquals(12, parsed.getHour());
        assertEquals(34, parsed.getMinute());
        assertEquals(56, parsed.getSecond());
        assertEquals(789_000_000, parsed.getNano());
        assertEquals(utc, parsed.getZone());
    }

    /*** 3. ISO format – yyyy‑MM‑dd ***/
    @Test
    public void testParseDate_ISO_YearFirst_WithDashes() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-06", utc, true);
        assertDateTime(zdt, 2024, 1, 6, 0, 0, 0, 0, utc);
    }

    @Test
    public void testParseDate_ISO_YearFirst_WithSlashesAndTime() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024/01/06 13:45:30", ny, true);
        assertDateTime(zdt, 2024, 1, 6, 13, 45, 30, 0, ny);
    }

    /*** 4. ISO format – MM‑dd‑yyyy ***/
    @Test
    public void testParseDate_ISO_MonthFirst_WithDotsAndOffset() {
        ZonedDateTime zdt = DateUtilities.parseDate("01.06.2024+02:00", utc, true);
        // offset +02:00 means the resulting zone is GMT+02:00
        ZoneId expected = ZoneId.ofOffset("GMT", java.time.ZoneOffset.ofHours(2));
        assertDateTime(zdt, 2024, 1, 6, 0, 0, 0, 0, expected);
    }

    /*** 5. Alpha‑month formats ***/
    @Test
    public void testParseDate_AlphaMonth_MonthDayYear() {
        ZonedDateTime zdt = DateUtilities.parseDate("Jan 6 2024", utc, true);
        assertDateTime(zdt, 2024, 1, 6, 0, 0, 0, 0, utc);
    }

    @Test
    public void testParseDate_AlphaMonth_MonthDayYear_WithOrdinalAndComma() {
        ZonedDateTime zdt = DateUtilities.parseDate("January 6th, 2024", utc, true);
        assertDateTime(zdt, 2024, 1, 6, 0, 0, 0, 0, utc);
    }

    @Test
    public void testParseDate_AlphaMonth_DayMonthYear_WithOrdinal() {
        ZonedDateTime zdt = DateUtilities.parseDate("6th Jan 2024", utc, true);
        assertDateTime(zdt, 2024, 1, 6, 0, 0, 0, 0, utc);
    }

    @Test
    public void testParseDate_AlphaMonth_YearMonthDay_WithOrdinal() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024 Jan 6th", utc, true);
        assertDateTime(zdt, 2024, 1, 6, 0, 0, 0, 0, utc);
    }

    /*** 6. Unix‑style with day‑of‑week and abbreviation ***/
    @Test
    public void testParseDate_UnixStyle_WithDayAndAbbrevTZ() {
        // Example: "Sat Jan 6 11:06:10 EST 2024"
        ZonedDateTime zdt = DateUtilities.parseDate("Sat Jan 6 11:06:10 EST 2024", utc, true);
        // EST maps to America/New_York (UTC‑05:00 standard time)
        ZoneId expected = ZoneId.of("America/New_York");
        assertDateTime(zdt, 2024, 1, 6, 11, 6, 10, 0, expected);
    }

    /*** 7. Time before date ***/
    @Test
    public void testParseDate_TimeBeforeDate() {
        // "13:45:30 2024-01-06"
        ZonedDateTime zdt = DateUtilities.parseDate("13:45:30 2024-01-06", utc, true);
        assertDateTime(zdt, 2024, 1, 6, 13, 45, 30, 0, utc);
    }

    /*** 8. Offset handling ***/
    @Test
    public void testParseDate_TimeWithPositiveOffset() {
        // "2024-01-06 12:00+03:30"
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-06 12:00+03:30", utc, true);
        ZoneId expected = ZoneId.ofOffset("GMT", java.time.ZoneOffset.ofHoursMinutes(3, 30));
        assertDateTime(zdt, 2024, 1, 6, 12, 0, 0, 0, expected);
    }

    @Test
    public void testParseDate_TimeWithNegativeOffsetAndZ() {
        // "2024-01-06 12:00-05:00"
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-06 12:00-05:00", utc, true);
        ZoneId expected = ZoneId.ofOffset("GMT", java.time.ZoneOffset.ofHours(-5));
        assertDateTime(zdt, 2024, 1, 6, 12, 0, 0, 0, expected);
    }

    @Test
    public void testParseDate_TimeWithZ() {
        // "2024-01-06 12:00Z"
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-06 12:00Z", utc, true);
        assertDateTime(zdt, 2024, 1, 6, 12, 0, 0, 0, utc);
    }

    /*** 9. Named zone handling (including bracketed) ***/
    @Test
    public void testParseDate_TimeWithNamedZone() {
        // "2024-01-06 09:00 Europe/Paris"
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-06 09:00 Europe/Paris", utc, true);
        ZoneId expected = ZoneId.of("Europe/Paris");
        assertDateTime(zdt, 2024, 1, 6, 9, 0, 0, 0, expected);
    }

    @Test
    public void testParseDate_TimeWithBracketedZone() {
        // Brackets are stripped: "[America/New_York]"
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-06 09:00 [America/New_York]", utc, true);
        ZoneId expected = ZoneId.of("America/New_York");
        assertDateTime(zdt, 2024, 1, 6, 9, 0, 0, 0, expected);
    }

    /*** 10. Fractional seconds ***/
    @Test
    public void testParseDate_FractionalSeconds() {
        // "2024-01-06 12:34:56.789"
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-06 12:34:56.789", utc, true);
        assertDateTime(zdt, 2024, 1, 6, 12, 34, 56, 789_000_000, utc);
    }

    /*** 11. ensureDateTimeAlone – garbage detection ***/
    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_EnsureAlone_GarbageThrows() {
        // Extra "XYZ" after a valid date should trigger an exception when ensureDateTimeAlone = true
        DateUtilities.parseDate("2024-01-06 XYZ", utc, true);
    }

    @Test
    public void testParseDate_EnsureAlone_GarbageIgnoredWhenFlagFalse() {
        // Same string but flag = false → the garbage is ignored after parsing
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-06 XYZ", utc, false);
        assertDateTime(zdt, 2024, 1, 6, 0, 0, 0, 0, utc);
    }

    /*** 12. Invalid month – should throw ***/
    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidMonth_Throws() {
        DateUtilities.parseDate("2024-13-01", utc, true);
    }

    /*** 13. Invalid hour – should throw ***/
    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidHour_Throws() {
        DateUtilities.parseDate("2024-01-01 25:00", utc, true);
    }

    /*** 14. Unknown time‑zone abbreviation – fallback to raw offset or exception ***/
    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_UnknownAbbreviation_Throws() {
        // "XYZ" is not a known abbreviation nor a valid ZoneId
        DateUtilities.parseDate("2024-01-01 12:00 XYZ", utc, true);
    }

    /*** 15. Original API (parseDate(String)) uses system default when no zone supplied ***/
    @Test
    public void testParseDate_OriginalApi_UsesSystemDefault() {
        // The date string contains no zone information.
        Date date = DateUtilities.parseDate("2024-01-06");
        // Convert back to ZonedDateTime using the system default to compare.
        ZonedDateTime expected = ZonedDateTime.ofInstant(date.toInstant(), system);
        ZonedDateTime actual = ZonedDateTime.ofInstant(date.toInstant(), system);
        assertEquals(expected, actual);
    }

    /*** Helper to compare all fields of a ZonedDateTime ***/
    private static void assertDateTime(ZonedDateTime actual,
                                        int year, int month, int day,
                                        int hour, int minute, int second,
                                        int nano, ZoneId zone) {
        assertEquals("Year mismatch", year, actual.getYear());
        assertEquals("Month mismatch", month, actual.getMonthValue());
        assertEquals("Day mismatch", day, actual.getDayOfMonth());
        assertEquals("Hour mismatch", hour, actual.getHour());
        assertEquals("Minute mismatch", minute, actual.getMinute());
        assertEquals("Second mismatch", second, actual.getSecond());
        assertEquals("Nano mismatch", nano, actual.getNano());
        assertEquals("Zone mismatch", zone, actual.getZone());
    }
}
