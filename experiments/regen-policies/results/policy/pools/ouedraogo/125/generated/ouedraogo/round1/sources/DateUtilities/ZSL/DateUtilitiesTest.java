package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.Assert.*;

public class DateUtilitiesTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // No special setup required – the tests pass an explicit default ZoneId (UTC) to avoid
        // dependence on the JVM's default time‑zone.
    }

    /*** Null / empty handling ***/
    @Test
    public void testParseDate_NullString_ReturnsNull() {
        assertNull(DateUtilities.parseDate(null));
        assertNull(DateUtilities.parseDate("", UTC, true));
    }

    @Test
    public void testParseDate_EmptyString_ReturnsNull() {
        assertNull(DateUtilities.parseDate("   "));
        assertNull(DateUtilities.parseDate("   ", UTC, true));
    }

    /*** Epoch milliseconds handling ***/
    @Test
    public void testParseDate_EpochMilliseconds() {
        // "0" represents the Unix epoch instant.
        Date epoch = DateUtilities.parseDate("0", UTC, true);
        assertNotNull(epoch);
        assertEquals(0L, epoch.getTime());

        ZonedDateTime zdt = DateUtilities.parseDate("0", UTC, true);
        assertEquals(1970, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(1, zdt.getDayOfMonth());
        assertEquals(0, zdt.getHour());
        assertEquals(0, zdt.getMinute());
        assertEquals(0, zdt.getSecond());
        assertEquals(ZoneOffset.UTC, zdt.getOffset());
    }

    /*** ISO date without time ***/
    @Test
    public void testParseDate_IsoDate_NoTime() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(0, zdt.getHour());
        assertEquals(0, zdt.getMinute());
        assertEquals(0, zdt.getSecond());
        assertEquals(UTC, zdt.getZone());
    }

    /*** ISO date with time ***/
    @Test
    public void testParseDate_IsoDate_WithTime() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 13:45:30", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(45, zdt.getMinute());
        assertEquals(30, zdt.getSecond());
        assertEquals(UTC, zdt.getZone());
    }

    /*** Time before date ***/
    @Test
    public void testParseDate_TimeBeforeDate() {
        ZonedDateTime zdt = DateUtilities.parseDate("13:30:45 2024-01-15", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(45, zdt.getSecond());
        assertEquals(UTC, zdt.getZone());
    }

    /*** Timezone offset handling ***/
    @Test
    public void testParseDate_WithOffset() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 12:00+02:00", UTC, true);
        assertEquals(ZoneOffset.ofHours(2), zdt.getOffset());
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(12, zdt.getHour());
    }

    /*** Timezone abbreviation handling (EST → America/New_York) ***/
    @Test
    public void testParseDate_WithAbbreviation() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 12:00 EST", UTC, true);
        assertEquals("America/New_York", zdt.getZone().getId());
        // On 2024‑01‑15 New York is on standard time (UTC‑05)
        assertEquals(ZoneOffset.ofHours(-5), zdt.getOffset());
    }

    /*** Unix‑style date string ***/
    @Test
    public void testParseDate_UnixStyle() {
        ZonedDateTime zdt = DateUtilities.parseDate("Tue Jan 15 12:00:00 EST 2024", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(12, zdt.getHour());
        assertEquals("America/New_York", zdt.getZone().getId());
    }

    /*** Alpha month formats ***/
    @Test
    public void testParseDate_MonthFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("January 6th, 2024", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_DayFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("6th January 2024", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_YearFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024 January 6th", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
    }

    /*** Fractional seconds handling ***/
    @Test
    public void testParseDate_FractionalSeconds() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 12:00:00.123", UTC, true);
        // .123 seconds = 123,000,000 nanoseconds
        assertEquals(123_000_000, zdt.getNano());
    }

    /*** Bracketed zone id handling ***/
    @Test
    public void testParseDate_BracketedZoneId() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 12:00 [America/Los_Angeles]", UTC, true);
        assertEquals("America/Los_Angeles", zdt.getZone().getId());
        assertEquals(ZoneOffset.ofHours(-8), zdt.getOffset());
    }

    /*** ensureDateTimeAlone = false should ignore trailing garbage ***/
    @Test
    public void testParseDate_IgnoreGarbageWhenNotEnsured() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 abc", UTC, false);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
    }

    /*** ensureDateTimeAlone = true should reject garbage ***/
    @Test
    public void testParseDate_GarbageWithEnsureThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Issue parsing date-time");
        DateUtilities.parseDate("2024-01-15 abc", UTC, true);
    }

    /*** Invalid month (13) throws ***/
    @Test
    public void testParseDate_InvalidMonthThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Month must be between 1 and 12");
        DateUtilities.parseDate("2024-13-01", UTC, true);
    }

    /*** Invalid day (32) throws ***/
    @Test
    public void testParseDate_InvalidDayThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Day must be between 1 and 31");
        DateUtilities.parseDate("2024-01-32", UTC, true);
    }

    /*** Invalid hour (24) throws ***/
    @Test
    public void testParseDate_InvalidHourThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Hour must be between 0 and 23");
        DateUtilities.parseDate("2024-01-15 24:00", UTC, true);
    }

    /*** Invalid minute (60) throws ***/
    @Test
    public void testParseDate_InvalidMinuteThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Minute must be between 0 and 59");
        DateUtilities.parseDate("2024-01-15 23:60", UTC, true);
    }

    /*** Invalid second (60) throws ***/
    @Test
    public void testParseDate_InvalidSecondThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Second must be between 0 and 59");
        DateUtilities.parseDate("2024-01-15 23:59:60", UTC, true);
    }

    /*** Verify that a null default ZoneId results in a clear exception ***/
    @Test
    public void testParseDate_NullZoneIdThrows() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("ZoneId cannot be null");
        DateUtilities.parseDate("2024-01-15", null, true);
    }
}
