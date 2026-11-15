package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.BeforeClass;

import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

public class DateUtilitiesTest {

    private static ZoneId UTC;
    private static ZoneId NEW_YORK;

    @BeforeClass
    public static void setUpClass() {
        UTC = ZoneId.of("UTC");
        NEW_YORK = ZoneId.of("America/New_York");
    }

    /*----------------------------------------------------------------------
     * Helper methods to invoke private static methods via reflection
     * ----------------------------------------------------------------------*/
    private static Object invokePrivateStatic(String methodName, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = DateUtilities.class.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    private static String stripBrackets(String input) throws Exception {
        return (String) invokePrivateStatic("stripBrackets", new Class[]{String.class}, input);
    }

    private static long convertFractionToNanos(String input) throws Exception {
        return (Long) invokePrivateStatic("convertFractionToNanos", new Class[]{String.class}, input);
    }

    private static ZoneId getTimeZone(String tz) throws Exception {
        return (ZoneId) invokePrivateStatic("getTimeZone", new Class[]{String.class}, tz);
    }

    private static void verifyNoGarbageLeft(String remnant) throws Exception {
        invokePrivateStatic("verifyNoGarbageLeft", new Class[]{String.class}, remnant);
    }

    /*----------------------------------------------------------------------
     * Public API tests
     * ----------------------------------------------------------------------*/

    @Test
    public void testParseDate_NullOrEmpty_ReturnsNull() {
        assertNull(DateUtilities.parseDate(null));
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("   "));
    }

    @Test
    public void testParseDate_EpochMillis() {
        // epoch 0 in UTC
        Date d = DateUtilities.parseDate("0");
        assertNotNull(d);
        assertEquals(Instant.EPOCH, d.toInstant());
    }

    @Test
    public void testParseDate_ISODate_NoTime() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(0, zdt.getHour());
        assertEquals(UTC, zdt.getZone());
    }

    @Test
    public void testParseDate_ISODate_WithTimeAndOffset() {
        // 2024-01-15 13:30:45+02:00 (space separator)
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-15 13:30:45+02:00", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(45, zdt.getSecond());
        // offset +02:00 should be represented as ZoneOffset +02:00
        assertEquals(ZoneId.ofOffset("GMT", ZoneOffset.ofHours(2)), zdt.getZone());
    }

    @Test
    public void testParseDate_AlphaMonthPattern() {
        ZonedDateTime zdt = DateUtilities.parseDate("Jan 5, 2024 09:15", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(5, zdt.getDayOfMonth());
        assertEquals(9, zdt.getHour());
        assertEquals(15, zdt.getMinute());
        assertEquals(0, zdt.getSecond());
        assertEquals(UTC, zdt.getZone());
    }

    @Test
    public void testParseDate_UnixStyle_WithAbbreviation() {
        // Example from Javadoc: "Tue Jan 15 13:30:45 EST 2024"
        ZonedDateTime zdt = DateUtilities.parseDate("Tue Jan 15 13:30:45 EST 2024", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(15, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(45, zdt.getSecond());
        // EST maps to America/New_York (standard time, offset -05:00)
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
    }

    @Test
    public void testParseDate_WithFullZoneIdInBrackets() {
        // Using the named zone syntax: "... 12:00 [Europe/Paris]"
        ZonedDateTime zdt = DateUtilities.parseDate("2024-03-10 12:00 [Europe/Paris]", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(3, zdt.getMonthValue());
        assertEquals(10, zdt.getDayOfMonth());
        assertEquals(12, zdt.getHour());
        assertEquals(ZoneId.of("Europe/Paris"), zdt.getZone());
    }

    @Test
    public void testParseDate_StrictMode_IllegalGarbageThrows() {
        try {
            DateUtilities.parseDate("2024-01-01 extra", UTC, true);
            fail("Expected IllegalArgumentException due to extra garbage");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testParseDate_NonStrictIgnoresGarbage() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-01 extra", UTC, false);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(1, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_InvalidMonthThrows() {
        try {
            DateUtilities.parseDate("2024-13-01", UTC, true);
            fail("Expected IllegalArgumentException for month 13");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testParseDate_InvalidHourThrows() {
        try {
            DateUtilities.parseDate("2024-01-01 25:00", UTC, true);
            fail("Expected IllegalArgumentException for hour > 23");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testParseDate_TimeZoneWithoutTimeThrows() {
        // ISO date followed directly by a timezone offset but no time portion
        try {
            DateUtilities.parseDate("2024-01-01+02:00", UTC, true);
            fail("Expected IllegalArgumentException for timezone without time");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /*----------------------------------------------------------------------
     * Private helper method tests (via reflection)
     * ----------------------------------------------------------------------*/

    @Test
    public void testStripBrackets_RemovesLeadingAndTrailingBracket() throws Exception {
        assertEquals("GMT+02:00", stripBrackets("[GMT+02:00]"));
        assertEquals("GMT+02:00", stripBrackets("GMT+02:00"));
        assertEquals("", stripBrackets("[]"));
        assertNull(stripBrackets(null));
    }

    @Test
    public void testConvertFractionToNanos() throws Exception {
        // .123 => 123_000_000 ns
        assertEquals(123_000_000L, convertFractionToNanos(".123"));
        // .0015 => 1_500_000 ns
        assertEquals(1_500_000L, convertFractionToNanos(".0015"));
        // .0 => 0 ns
        assertEquals(0L, convertFractionToNanos(".0"));
    }

    @Test
    public void testGetTimeZone_Offset() throws Exception {
        ZoneId zid = getTimeZone("-05:00");
        assertEquals(ZoneId.ofOffset("GMT", ZoneOffset.ofHours(-5)), zid);
    }

    @Test
    public void testGetTimeZone_AbbreviationMapped() throws Exception {
        ZoneId zid = getTimeZone("EST");
        assertEquals(ZoneId.of("America/New_York"), zid);
    }

    @Test
    public void testGetTimeZone_FullRegionId() throws Exception {
        ZoneId zid = getTimeZone("Europe/Paris");
        assertEquals(ZoneId.of("Europe/Paris"), zid);
    }

    @Test
    public void testGetTimeZone_UnrecognisedFallsBackToTimeZone() throws Exception {
        // "GMT+03" is not a standard offset string for ZoneOffset.of, but TimeZone can handle it.
        ZoneId zid = getTimeZone("GMT+03");
        // The fallback should give a zone with the same raw offset (3 hours)
        assertEquals(ZoneOffset.ofHours(3), zid.getRules().getOffset(Instant.now()));
    }

    @Test
    public void testVerifyNoGarbageLeft_ValidOnlyDayOfWeekAndComma() throws Exception {
        // "Mon" is a day name that should be stripped, leaving nothing else.
        verifyNoGarbageLeft("Mon");
        // "Tue," also should be stripped.
        verifyNoGarbageLeft("Tue,");
    }

    @Test
    public void testVerifyNoGarbageLeft_ThrowsOnRemainingGarbage() throws Exception {
        try {
            verifyNoGarbageLeft("Mon Xtra");
            fail("Expected IllegalArgumentException for leftover garbage");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    /*----------------------------------------------------------------------
     * Integration style tests for the overall parsing logic
     * ----------------------------------------------------------------------*/

    @Test
    public void testParseDate_FractionalSeconds() {
        // 13:30:45.123 (fractional seconds)
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-02 13:30:45.123", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(2, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(45, zdt.getSecond());
        // nanosecond part should be 123_000_000
        assertEquals(123_000_000, zdt.getNano());
    }

    @Test
    public void testParseDate_TimeBeforeDate() {
        // Time first, then date: "13:45 2024-12-31"
        ZonedDateTime zdt = DateUtilities.parseDate("13:45 2024-12-31", UTC, true);
        assertEquals(2024, zdt.getYear());
        assertEquals(12, zdt.getMonthValue());
        assertEquals(31, zdt.getDayOfMonth());
        assertEquals(13, zdt.getHour());
        assertEquals(45, zdt.getMinute());
    }

    @Test
    public void testParseDate_DayFirstWithSuffix() {
        // "21st February 2023"
        ZonedDateTime zdt = DateUtilities.parseDate("21st February 2023", UTC, true);
        assertEquals(2023, zdt.getYear());
        assertEquals(2, zdt.getMonthValue());
        assertEquals(21, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_YearFirstMonthDay() {
        // "2025 March 3rd"
        ZonedDateTime zdt = DateUtilities.parseDate("2025 March 3rd", UTC, true);
        assertEquals(2025, zdt.getYear());
        assertEquals(3, zdt.getMonthValue());
        assertEquals(3, zdt.getDayOfMonth());
    }

    @Test
    public void testParseDate_WithExplicitDefaultZoneWhenNoTZProvided() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-04-01 10:00", NEW_YORK, true);
        assertEquals(NEW_YORK, zdt.getZone());
        assertEquals(10, zdt.getHour());
    }
}
