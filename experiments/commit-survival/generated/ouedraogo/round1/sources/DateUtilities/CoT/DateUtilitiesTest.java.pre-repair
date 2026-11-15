package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

public class DateUtilitiesTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* --------------------------------------------------------------
     * Basic functionality tests
     * -------------------------------------------------------------- */

    @Test
    public void testParseDate_NullOrEmpty_ReturnsNull() {
        assertNull(DateUtilities.parseDate((String) null));
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("   "));
    }

    @Test
    public void testParseDate_EpochMillis() {
        Date epoch = DateUtilities.parseDate("0");
        assertNotNull(epoch);
        assertEquals(0L, epoch.getTime());          // epoch is 0 ms since 1970‑01‑01T00:00:00Z
    }

    @Test
    public void testParseDate_IsoFormat_yyyyMMdd() {
        ZonedDateTime zdt = DateUtilities.parseDate("2023-12-31", ZoneId.of("UTC"), true);
        assertEquals(2023, zdt.getYear());
        assertEquals(12,   zdt.getMonthValue());
        assertEquals(31,   zdt.getDayOfMonth());
        assertEquals(0,    zdt.getHour());
        assertEquals(0,    zdt.getMinute());
        assertEquals(0,    zdt.getSecond());
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
    }

    @Test
    public void testParseDate_IsoFormat_mmddyyyy() {
        ZonedDateTime zdt = DateUtilities.parseDate("12/31/2023", ZoneId.of("UTC"), true);
        assertEquals(2023, zdt.getYear());
        assertEquals(12,   zdt.getMonthValue());
        assertEquals(31,   zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_AlphaMonth_WithOrdinal() {
        ZonedDateTime zdt = DateUtilities.parseDate("January 1st, 2024", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1,    zdt.getMonthValue());
        assertEquals(1,    zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_AlphaMonth_DayFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("1st Jan 2024", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1,    zdt.getMonthValue());
        assertEquals(1,    zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_UnixStyle_WithTimezoneAbbreviation() {
        // “Sat Jan 6 11:06:10 EST 2024”
        ZonedDateTime zdt = DateUtilities.parseDate(
                "Sat Jan 6 11:06:10 EST 2024",
                ZoneId.of("UTC"),
                true);

        // EST maps to America/New_York (offset -05:00 in winter)
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
        assertEquals(2024, zdt.getYear());
        assertEquals(1,    zdt.getMonthValue());
        assertEquals(6,    zdt.getDayOfMonth());
        assertEquals(11,   zdt.getHour());
        assertEquals(6,    zdt.getMinute());
        assertEquals(10,   zdt.getSecond());
    }

    @Test
    public void testParseDate_TimeOffset() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-06T15:30+02:00",
                ZoneId.of("UTC"),
                true);

        assertEquals(ZoneId.ofOffset("GMT", java.time.ZoneOffset.ofHours(2)), zdt.getZone());
        assertEquals(15, zdt.getHour());
        assertEquals(30, zdt.getMinute());
    }

    @Test
    public void testParseDate_TimeZoneNamedBracket() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-06 12:00 [America/Los_Angeles]",
                ZoneId.of("UTC"),
                true);

        assertEquals(ZoneId.of("America/Los_Angeles"), zdt.getZone());
        assertEquals(12, zdt.getHour());
    }

    @Test
    public void testParseDate_FractionalSeconds() {
        ZonedDateTime zdt = DateUtilities.parseDate(
                "2024-01-01 12:34:56.789",
                ZoneId.of("UTC"),
                true);

        assertEquals(12,   zdt.getHour());
        assertEquals(34,   zdt.getMinute());
        assertEquals(56,   zdt.getSecond());
        // 0.789 second = 789,000,000 nanos
        assertEquals(789_000_000, zdt.getNano());
    }

    @Test
    public void testParseDate_MultipleOffsetFormats() {
        // +0100
        ZonedDateTime zdt1 = DateUtilities.parseDate(
                "2024-01-01 10:00+0100",
                ZoneId.of("UTC"),
                true);
        assertEquals(ZoneId.ofOffset("GMT", java.time.ZoneOffset.ofHours(1)), zdt1.getZone());

        // -01
        ZonedDateTime zdt2 = DateUtilities.parseDate(
                "2024-01-01 10:00-01",
                ZoneId.of("UTC"),
                true);
        assertEquals(ZoneId.ofOffset("GMT", java.time.ZoneOffset.ofHours(-1)), zdt2.getZone());

        // Z (Zulu / UTC)
        ZonedDateTime zdt3 = DateUtilities.parseDate(
                "2024-01-01 10:00Z",
                ZoneId.of("UTC"),
                true);
        assertEquals(ZoneId.of("Z"), zdt3.getZone());
    }

    /* --------------------------------------------------------------
     * Edge‑case / exception tests
     * -------------------------------------------------------------- */

    @Test
    public void testParseDate_EnsureDateTimeAlone_ExtraGarbageThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Issue parsing date-time");
        DateUtilities.parseDate("2024-01-06 abc", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_InvalidMonthThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Month must be between 1 and 12");
        DateUtilities.parseDate("2024-13-01", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_InvalidDayThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Day must be between 1 and 31");
        DateUtilities.parseDate("2024-02-30", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_InvalidHourThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Hour must be between 0 and 23");
        DateUtilities.parseDate("2024-01-01 25:00", ZoneId.of("UTC"), true);
    }

    @Test
    public void testParseDate_NullDefaultZoneIdThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("ZoneId cannot be null");
        DateUtilities.parseDate("2024-01-01", null, true);
    }

    @Test
    public void testParseDate_EnsureDateTimeAlone_FalseIgnoresGarbage() {
        // When ensureDateTimeAlone = false the garbage is ignored
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-01 abc", ZoneId.of("UTC"), false);
        assertEquals(2024, zdt.getYear());
        assertEquals(1,    zdt.getMonthValue());
        assertEquals(1,    zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_WhitespaceAndCommaVariations() {
        // Various whitespace/comma combos should be tolerated
        ZonedDateTime zdt = DateUtilities.parseDate(
                "  January   2 , 2024  ",
                ZoneId.of("UTC"),
                true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1,    zdt.getMonthValue());
        assertEquals(2,    zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_EpochStringWithLargeNumber() {
        // Very large epoch value – just verify that parsing does not throw and returns a Date
        long bigMillis = 9_999_999_999_999L; // approx year 2286
        Date d = DateUtilities.parseDate(Long.toString(bigMillis));
        assertNotNull(d);
        assertEquals(bigMillis, d.getTime());
    }

    @Test
    public void testParseDate_OriginalApi_UsesSystemDefaultZone() {
        // This test uses the single‑argument overload which falls back to the system default zone.
        // We cannot control the system zone in the test environment, but we can verify that
        // the returned Date corresponds to the same instant as the ZonedDateTime produced by
        // the three‑argument overload with ZoneId.systemDefault().
        String input = "2024-03-15T08:15:00";
        Date d = DateUtilities.parseDate(input);
        ZonedDateTime zdt = DateUtilities.parseDate(input, ZoneId.systemDefault(), true);
        assertEquals(d.toInstant(), zdt.toInstant());
    }
}
