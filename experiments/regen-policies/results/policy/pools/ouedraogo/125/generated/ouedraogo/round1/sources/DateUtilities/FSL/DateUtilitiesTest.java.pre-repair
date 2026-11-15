package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.Date;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link DateUtilities}.
 * <p>
 * The tests exercise the public API (both overloads of {@code parseDate})
 * and verify the behaviour of the internal parsing logic indirectly.
 * All edge‑cases, timezone handling, fractional seconds, epoch‑millis,
 * garbage detection and error handling are covered.
 */
public class DateUtilitiesTest {

    /* -------------------------------------------------------------
     *  Helper utilities used only inside this test class
     * ------------------------------------------------------------- */

    private static ZonedDateTime zdt(int year, int month, int day,
                                    int hour, int minute, int second,
                                    ZoneId zone) {
        return ZonedDateTime.of(year, month, day, hour, minute, second, 0, zone);
    }

    private static ZonedDateTime zdt(int year, int month, int day,
                                    int hour, int minute, int second,
                                    int nano, ZoneId zone) {
        return ZonedDateTime.of(year, month, day, hour, minute, second, nano, zone);
    }

    /* -------------------------------------------------------------
     *  Tests for parseDate(String) – the original API
     * ------------------------------------------------------------- */

    @Test
    public void testParseDate_NullOrEmpty_ReturnsNull() {
        assertNull(DateUtilities.parseDate(null));
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("   "));
    }

    @Test
    public void testParseDate_EpochMilliseconds() {
        // 1640995200000L == 2022‑01‑01T00:00:00Z
        long epoch = 1640995200000L;
        Date d = DateUtilities.parseDate(Long.toString(epoch));
        assertNotNull(d);
        assertEquals(Instant.ofEpochMilli(epoch), d.toInstant());
    }

    @Test
    public void testParseDate_IsoDate_NoTime_UsesSystemDefault() {
        // Use a known default zone to make test deterministic
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 15, 0, 0, 0, 0, defaultZone);

        // The original API always uses the system default, so we set the JVM default temporarily
        ZoneId original = ZoneId.systemDefault();
        try {
            System.setProperty("user.timezone", "UTC");
            // Force the JVM to recompute the default zone
            java.util.TimeZone.setDefault(null);
            Date result = DateUtilities.parseDate("2024-03-15");
            assertEquals(Date.from(expected.toInstant()), result);
        } finally {
            System.setProperty("user.timezone", original.getId());
            java.util.TimeZone.setDefault(null);
        }
    }

    @Test
    public void testParseDate_IsoDate_WithTimeAndOffset() {
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 15,
                13, 45, 30, 0,
                ZoneId.ofOffset("GMT", java.time.ZoneOffset.ofHoursMinutes(2, 0)));
        ZonedDateTime result = DateUtilities.parseDate("2024-03-15 13:45:30+02:00",
                ZoneId.of("UTC"), true);
        assertEquals(expected, result);
    }

    @Test
    public void testParseDate_AlphaMonth_WithCommaAndTime() {
        ZonedDateTime expected = ZonedDateTime.of(2023, 1, 5,
                14, 20, 0, 0,
                ZoneId.of("America/New_York"));
        ZonedDateTime result = DateUtilities.parseDate("Jan 5, 2023 14:20",
                ZoneId.of("America/New_York"), true);
        assertEquals(expected, result);
    }

    @Test
    public void testParseDate_UnixStyle_WithAbbreviation() {
        // "Tue Jan 15 14:30:00 EST 2024"
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 15,
                14, 30, 0, 0,
                ZoneId.of("America/New_York")); // EST maps to America/New_York
        ZonedDateTime result = DateUtilities.parseDate("Tue Jan 15 14:30:00 EST 2024",
                ZoneId.of("UTC"), true);
        assertEquals(expected, result);
    }

    @Test
    public void testParseDate_FractionalSeconds() {
        // 0.123456 seconds = 123456000 nanoseconds
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1,
                12, 0, 0, 123_456_000,
                ZoneId.of("UTC"));
        ZonedDateTime result = DateUtilities.parseDate("2024-01-01 12:00:00.123456",
                ZoneId.of("UTC"), true);
        assertEquals(expected, result);
    }

    @Test
    public void testParseDate_TimezoneWithBrackets_StripsBrackets() {
        // Timezone given as "[America/New_York]"
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1,
                12, 0, 0, 0,
                ZoneId.of("America/New_York"));
        ZonedDateTime result = DateUtilities.parseDate("2024-01-01 12:00[America/New_York]",
                ZoneId.of("UTC"), true);
        assertEquals(expected, result);
    }

    @Test
    public void testParseDate_EnsureDateTimeAlone_RejectsGarbage() {
        try {
            DateUtilities.parseDate("2024-01-01 extra", ZoneId.of("UTC"), true);
            fail("Expected IllegalArgumentException due to extra garbage");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("other characters present"));
        }
    }

    @Test
    public void testParseDate_EnsureDateTimeAlone_AllowsGarbageWhenDisabled() {
        // Garbage after the date is ignored when ensureDateTimeAlone == false
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1,
                0, 0, 0, 0,
                ZoneId.of("UTC"));
        ZonedDateTime result = DateUtilities.parseDate("2024-01-01 some random text",
                ZoneId.of("UTC"), false);
        assertEquals(expected, result);
    }

    /* -------------------------------------------------------------
     *  Tests for error handling (invalid components)
     * ------------------------------------------------------------- */

    @Test
    public void testParseDate_InvalidMonth_Throws() {
        try {
            DateUtilities.parseDate("2024-13-01", ZoneId.of("UTC"), true);
            fail("Expected IllegalArgumentException for month 13");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Month must be between 1 and 12"));
        }
    }

    @Test
    public void testParseDate_InvalidDay_Throws() {
        try {
            DateUtilities.parseDate("2024-02-30", ZoneId.of("UTC"), true);
            fail("Expected IllegalArgumentException for day 30 in February");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Day must be between 1 and 31"));
        }
    }

    @Test
    public void testParseDate_InvalidHour_Throws() {
        try {
            DateUtilities.parseDate("2024-01-01 25:00", ZoneId.of("UTC"), true);
            fail("Expected IllegalArgumentException for hour > 23");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Hour must be between 0 and 23"));
        }
    }

    @Test
    public void testParseDate_InvalidMinute_Throws() {
        try {
            DateUtilities.parseDate("2024-01-01 12:60", ZoneId.of("UTC"), true);
            fail("Expected IllegalArgumentException for minute > 59");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Minute must be between 0 and 59"));
        }
    }

    @Test
    public void testParseDate_InvalidSecond_Throws() {
        try {
            DateUtilities.parseDate("2024-01-01 12:00:60", ZoneId.of("UTC"), true);
            fail("Expected IllegalArgumentException for second > 59");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Second must be between 0 and 59"));
        }
    }

    /* -------------------------------------------------------------
     *  Direct tests for the private helper methods via reflection
     *  (only to increase coverage – not required for functional tests)
     * ------------------------------------------------------------- */

    @Test
    public void testConvertFractionToNanos_ExactConversion() throws Exception {
        // Access private static convertFractionToNanos via reflection
        java.lang.reflect.Method m = DateUtilities.class.getDeclaredMethod("convertFractionToNanos", String.class);
        m.setAccessible(true);
        // ".123" -> 0.123 seconds => 123,000,000 nanoseconds
        long nanos = (Long) m.invoke(null, ".123");
        assertEquals(123_000_000L, nanos);
        // ".0015" -> 0.0015 seconds => 1,500,000 nanoseconds
        nanos = (Long) m.invoke(null, ".0015");
        assertEquals(1_500_000L, nanos);
    }

    @Test
    public void testStripBrackets_RemovesLeadingAndTrailing() throws Exception {
        java.lang.reflect.Method m = DateUtilities.class.getDeclaredMethod("stripBrackets", String.class);
        m.setAccessible(true);
        assertEquals("America/New_York", m.invoke(null, "[America/New_York]"));
        assertEquals("UTC", m.invoke(null, "UTC"));
        assertEquals("", m.invoke(null, ""));
        assertNull(m.invoke(null, (Object) null));
    }

    @Test
    public void testGetTimeZone_OffsetAndNamed() {
        // Offset
        ZoneId offset = DateUtilities.getTimeZone("+05:30");
        assertEquals(ZoneId.ofOffset("GMT", java.time.ZoneOffset.ofHoursMinutes(5, 30)), offset);

        // Named zone (valid)
        ZoneId ny = DateUtilities.getTimeZone("America/New_York");
        assertEquals(ZoneId.of("America/New_York"), ny);

        // Abbreviation mapping (EST -> America/New_York)
        ZoneId est = DateUtilities.getTimeZone("EST");
        assertEquals(ZoneId.of("America/New_York"), est);
    }

    @Test
    public void testVerifyNoGarbageLeft_AllowsDayOfWeekAndSeparatorsOnly() throws Exception {
        java.lang.reflect.Method m = DateUtilities.class.getDeclaredMethod("verifyNoGarbageLeft", String.class);
        m.setAccessible(true);
        // Only a day-of-week should be ignored
        m.invoke(null, "Mon");
        // Only a stray "T" or "," should be ignored
        m.invoke(null, "T");
        m.invoke(null, ",");
        // Nothing left – should not throw
        m.invoke(null, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testVerifyNoGarbageLeft_ThrowsOnUnexpectedChars() throws Exception {
        java.lang.reflect.Method m = DateUtilities.class.getDeclaredMethod("verifyNoGarbageLeft", String.class);
        m.setAccessible(true);
        // Any non‑day‑of‑week character other than T or , will cause an exception
        m.invoke(null, "abc");
    }
}
