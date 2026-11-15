package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.BeforeClass;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

public class DateUtilitiesTest {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");

    @BeforeClass
    public static void setUpClass() {
        // Ensure consistent default zone for tests that rely on system default
        // (the parseDate(String) overload uses ZoneId.systemDefault()).
        // Setting it to UTC makes the behaviour deterministic.
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
    }

    @Test
    public void testParseNullReturnsNull() {
        assertNull(DateUtilities.parseDate(null));
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("   "));
    }

    @Test
    public void testParseEpochMillis() {
        // "0" should be the epoch instant at UTC
        ZonedDateTime zdt = DateUtilities.parseDate("0", UTC, true);
        assertNotNull(zdt);
        assertEquals(0L, zdt.toInstant().toEpochMilli());
        assertEquals(UTC, zdt.getZone());
    }

    @Test
    public void testIsoDateYMD() {
        // yyyy-MM-dd format
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-06", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
        assertEquals(0, zdt.getHour());
        assertEquals(0, zdt.getMinute());
        assertEquals(UTC, zdt.getZone());
    }

    @Test
    public void testIsoDateMDY() {
        // mm/dd/yyyy format
        ZonedDateTime zdt = DateUtilities.parseDate("01/06/2024", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
    }

    @Test
    public void testAlphaMonthFormat_MonthFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("Jan 6, 2024", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
    }

    @Test
    public void testAlphaMonthFormat_DayFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("6 Jan 2024", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
    }

    @Test
    public void testAlphaMonthFormat_YearFirst() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024 Jan 6", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
    }

    @Test
    public void testUnixStyleDateTime() {
        // "Sat Jan 6 11:06:10 EST 2024"
        ZonedDateTime zdt = DateUtilities.parseDate("Sat Jan 6 11:06:10 EST 2024", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
        assertEquals(11, zdt.getHour());
        assertEquals(6, zdt.getMinute());
        assertEquals(10, zdt.getSecond());
        // EST maps to America/New_York (UTC‑05:00 for the given date)
        assertEquals(NEW_YORK, zdt.getZone());
        assertEquals(ZoneOffset.ofHours(-5), zdt.getOffset());
    }

    @Test
    public void testTimeWithOffset() {
        // Date with explicit offset
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-06 12:34+02:00", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
        assertEquals(12, zdt.getHour());
        assertEquals(34, zdt.getMinute());
        assertEquals(ZoneOffset.ofHours(2), zdt.getOffset());
    }

    @Test
    public void testTimeWithNamedZone() {
        // Named zone "PST" maps to America/Los_Angeles (UTC‑08:00 on 2024‑01‑06)
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-06T12:34 PST", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
        assertEquals(12, zdt.getHour());
        assertEquals(34, zdt.getMinute());
        assertEquals(LOS_ANGELES, zdt.getZone());
        assertEquals(ZoneOffset.ofHours(-8), zdt.getOffset());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEnsureDateTimeAloneThrowsOnGarbage() {
        // Extra characters after a valid date should trigger an exception when ensureDateTimeAlone is true
        DateUtilities.parseDate("2024-01-06 extra", UTC, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMonthThrows() {
        // Month 13 is out of range
        DateUtilities.parseDate("2024-13-01", UTC, true);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidHourThrows() {
        // Hour 25 is invalid
        DateUtilities.parseDate("2024-01-01 25:00", UTC, true);
    }

    @Test
    public void testDayOfWeekIgnored() {
        // Day of week should be ignored, parsing should succeed
        ZonedDateTime zdt = DateUtilities.parseDate("Monday 2024-01-06", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
        assertEquals(0, zdt.getHour());
        assertEquals(0, zdt.getMinute());
    }

    @Test
    public void testParseDateConvenienceMethodUsesSystemDefault() {
        // With system default set to UTC in @BeforeClass, the convenience overload should behave like the explicit call
        ZonedDateTime viaConvenient = DateUtilities.parseDate("2024-01-06");
        ZonedDateTime viaExplicit = DateUtilities.parseDate("2024-01-06", UTC, true);
        assertEquals(viaExplicit, viaConvenient);
    }

    @Test
    public void testParseDateReturnsDateObject() {
        Date date = DateUtilities.parseDate("2024-01-06");
        assertNotNull(date);
        // Convert back to Instant and compare with expected epoch milliseconds
        Instant expected = ZonedDateTime.of(2024, 1, 6, 0, 0, 0, 0, UTC).toInstant();
        assertEquals(expected.toEpochMilli(), date.getTime());
    }
}
