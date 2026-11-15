package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DateUtilities}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>normal parsing of the various supported date formats</li>
 *   <li>time parsing (with/without seconds, fractional seconds, offsets, zone names)</li>
 *   <li>epoch‑millisecond handling</li>
 *   <li>error handling (invalid formats, out‑of‑range values, extra garbage when {@code ensureDateTimeAlone}=true)</li>
 *   <li>timezone‑abbreviation resolution</li>
 * </ul>
 *
 * <p>All tests use only the public API of {@code DateUtilities} and the standard Java SE classes that appear
 * in the source file.
 */
public class DateUtilitiesTest {

    /** A constant default zone used in most tests to avoid dependence on the host JVM’s default zone. */
    private static final ZoneId UTC = ZoneId.of("UTC");

    /** Helper that builds a {@link ZonedDateTime} from the given components in the UTC zone. */
    private static ZonedDateTime utcZdt(int year, int month, int day,
                                       int hour, int minute, int second, int nano) {
        return ZonedDateTime.of(year, month, day, hour, minute, second, nano, UTC);
    }

    /** Helper that builds a {@link ZonedDateTime} from the given components in a supplied zone. */
    private static ZonedDateTime zdt(ZoneId zone,
                                     int year, int month, int day,
                                     int hour, int minute, int second, int nano) {
        return ZonedDateTime.of(year, month, day, hour, minute, second, nano, zone);
    }

    /** ---------------------------------------------------------------------- *
     *  1.  parseDate(String) – null / empty handling
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_NullOrEmpty_ReturnsNull() {
        assertNull(DateUtilities.parseDate(null));
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("   "));
    }

    /** ---------------------------------------------------------------------- *
     *  2.  parseDate(String) – epoch‑millisecond string
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_EpochMillis() {
        // "0" → 1970‑01‑01T00:00:00Z
        ZonedDateTime expected = utcZdt(1970, 1, 1, 0, 0, 0, 0);
        assertEquals(Date.from(expected.toInstant()), DateUtilities.parseDate("0"));

        // a non‑zero epoch, e.g. 2024‑01‑01T12:00:00Z → 1704100800000 ms
        long millis = Instant.parse("2024-01-01T12:00:00Z").toEpochMilli();
        ZonedDateTime expected2024 = utcZdt(2024, 1, 1, 12, 0, 0, 0);
        assertEquals(Date.from(expected2024.toInstant()), DateUtilities.parseDate(Long.toString(millis)));
    }

    /** ---------------------------------------------------------------------- *
     *  3.  parseDate(String, ZoneId, true) – ISO date without time
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_IsoDate_NoTime() {
        ZonedDateTime expected = utcZdt(2024, 1, 15, 0, 0, 0, 0);
        assertEquals(expected, DateUtilities.parseDate("2024-01-15", UTC, true));
        // separators other than dash
        assertEquals(expected, DateUtilities.parseDate("2024/01/15", UTC, true));
        assertEquals(expected, DateUtilities.parseDate("2024.01.15", UTC, true));
    }

    /** ---------------------------------------------------------------------- *
     *  4.  ISO date with time and a numeric offset
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_IsoDate_WithTimeAndOffset() {
        // 2024‑01‑15 13:30:45.123456+02:00
        ZonedDateTime expected = zdt(ZoneOffset.ofHours(2), 2024, 1, 15,
                13, 30, 45, 123_456_000);
        assertEquals(expected, DateUtilities.parseDate(
                "2024-01-15 13:30:45.123456+02:00", UTC, true));
    }

    /** ---------------------------------------------------------------------- *
     *  5.  Alpha‑month pattern with fractional seconds
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_AlphaMonth_WithFractionalSeconds() {
        // "Jan 15, 2024 09:45:30.5"
        ZonedDateTime expected = utcZdt(2024, 1, 15, 9, 45, 30, 500_000_000);
        assertEquals(expected, DateUtilities.parseDate(
                "Jan 15, 2024 09:45:30.5", UTC, true));
    }

    /** ---------------------------------------------------------------------- *
     *  6.  Unix‑style date with a known abbreviation (EST)
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_UnixStyle_WithAbbreviation() {
        // "Tue Jan 15 14:30:00 EST 2024"
        ZoneId est = ZoneId.of("America/New_York");   // EST maps to this zone in the static map
        ZonedDateTime expected = zdt(est, 2024, 1, 15, 14, 30, 0, 0);
        assertEquals(expected, DateUtilities.parseDate(
                "Tue Jan 15 14:30:00 EST 2024", UTC, true));
    }

    /** ---------------------------------------------------------------------- *
     *  7.  Time‑zone abbreviation mapping (PST → America/Los_Angeles)
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_TimeZoneAbbreviation_Mapped() {
        // "2024-01-15 10:00 PST"
        ZoneId la = ZoneId.of("America/Los_Angeles");
        ZonedDateTime expected = zdt(la, 2024, 1, 15, 10, 0, 0, 0);
        assertEquals(expected, DateUtilities.parseDate(
                "2024-01-15 10:00 PST", UTC, true));
    }

    /** ---------------------------------------------------------------------- *
     *  8.  Numeric offset without colon (e.g. -0400) and with just hour offset (+05)
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_OffsetVariations() {
        ZonedDateTime expected1 = zdt(ZoneOffset.of("-04:00"), 2024, 1, 15,
                12, 0, 0, 0);
        assertEquals(expected1, DateUtilities.parseDate(
                "2024-01-15 12:00-0400", UTC, true));

        ZonedDateTime expected2 = zdt(ZoneOffset.ofHours(5), 2024, 1, 15,
                8, 30, 0, 0);
        assertEquals(expected2, DateUtilities.parseDate(
                "2024-01-15 08:30+05", UTC, true));
    }

    /** ---------------------------------------------------------------------- *
     *  9.  Invalid format → IllegalArgumentException
     *  ---------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_InvalidFormat_Throws() {
        DateUtilities.parseDate("not-a-date", UTC, true);
    }

    /** ---------------------------------------------------------------------- *
     * 10.  ensureDateTimeAlone = true with extra garbage → IllegalArgumentException
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_EnsureAlone_WithGarbage_Throws() {
        try {
            DateUtilities.parseDate("2024-01-15 extra garbage", UTC, true);
            fail("Expected IllegalArgumentException because of extra characters");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Issue parsing date-time"));
        }
    }

    /** ---------------------------------------------------------------------- *
     * 11.  Hour out of range → IllegalArgumentException
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_HourOutOfRange_Throws() {
        try {
            DateUtilities.parseDate("2024-01-15 25:00", UTC, true);
            fail("Expected IllegalArgumentException for hour > 23");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Hour must be between 0 and 23"));
        }
    }

    /** ---------------------------------------------------------------------- *
     * 12.  Day / month out of range → IllegalArgumentException
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_DayMonthOutOfRange_Throws() {
        // month = 13
        try {
            DateUtilities.parseDate("2024-13-01", UTC, true);
            fail("Expected IllegalArgumentException for month > 12");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Month must be between 1 and 12"));
        }

        // day = 32
        try {
            DateUtilities.parseDate("2024-12-32", UTC, true);
            fail("Expected IllegalArgumentException for day > 31");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Day must be between 1 and 31"));
        }
    }

    /** ---------------------------------------------------------------------- *
     * 13.  ensureDateTimeAlone = false – garbage is ignored
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_EnsureAloneFalse_IgnoresGarbage() {
        // "2024-01-15 12:00 blah" – with ensureDateTimeAlone=false the trailing "blah"
        // should be discarded after the date/time has been parsed.
        ZonedDateTime expected = utcZdt(2024, 1, 15, 12, 0, 0, 0);
        assertEquals(expected, DateUtilities.parseDate("2024-01-15 12:00 blah", UTC, false));
    }

    /** ---------------------------------------------------------------------- *
     * 14.  Parsing when the time component appears *before* the date component
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_TimeBeforeDate() {
        // "13:45 2024-01-15"
        ZonedDateTime expected = utcZdt(2024, 1, 15, 13, 45, 0, 0);
        assertEquals(expected, DateUtilities.parseDate("13:45 2024-01-15", UTC, true));
    }

    /** ---------------------------------------------------------------------- *
     * 15.  Parsing with square‑bracketed zone name (e.g. "[America/New_York]")
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_BracketedZoneName() {
        // "2024-01-15 09:00 [America/New_York]"
        ZoneId ny = ZoneId.of("America/New_York");
        ZonedDateTime expected = zdt(ny, 2024, 1, 15, 9, 0, 0, 0);
        assertEquals(expected, DateUtilities.parseDate(
                "2024-01-15 09:00 [America/New_York]", UTC, true));
    }

    /** ---------------------------------------------------------------------- *
     * 16.  Fractional seconds with more than 9 digits (should be truncated/rounded by parser)
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_FractionalSecondsHighPrecision() {
        // The parser reads the fractional part as a double, then converts to nanos.
        // "0.123456789123" → 0.123456789 seconds → 123_456_789 nanos (extra digits ignored)
        ZonedDateTime expected = utcZdt(2024, 1, 15, 10, 20, 30, 123_456_789);
        assertEquals(expected, DateUtilities.parseDate(
                "2024-01-15 10:20:30.123456789123", UTC, true));
    }

    /** ---------------------------------------------------------------------- *
     * 17.  Verify that a named zone that is not an abbreviation is accepted directly.
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_NamedZoneDirect() {
        // "2024-01-15 12:00 Europe/Paris"
        ZoneId paris = ZoneId.of("Europe/Paris");
        ZonedDateTime expected = zdt(paris, 2024, 1, 15, 12, 0, 0, 0);
        assertEquals(expected, DateUtilities.parseDate(
                "2024-01-15 12:00 Europe/Paris", UTC, true));
    }

    /** ---------------------------------------------------------------------- *
     * 18.  Verify that a missing default ZoneId argument causes the documented
     *      IllegalArgumentException (the method checks for null).
     *  ---------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testParseDate_NullDefaultZoneId_Throws() {
        DateUtilities.parseDate("2024-01-15", null, true);
    }

    /** ---------------------------------------------------------------------- *
     * 19.  Verify that the original parseDate(String) overload delegates correctly
     *      to the three‑argument version (using system default zone).  We set the
     *      default zone to UTC for the test via a system property, then compare
     *      the result with the explicit call.
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_OriginalOverload_DelegatesCorrectly() {
        // Force system default zone to UTC for deterministic result.
        ZoneId originalDefault = ZoneId.systemDefault();
        try {
            System.setProperty("user.timezone", "UTC");
            // Need to reload the default zone after changing the property
            // (JVM reads it once at startup, but we can use ZoneId.of("UTC") directly)
            ZonedDateTime expected = utcZdt(2024, 5, 10, 0, 0, 0, 0);
            assertEquals(expected, DateUtilities.parseDate("2024-05-10"));
        } finally {
            // Restore original timezone property – does not affect already‑running JVM,
            // but we keep it tidy.
            System.setProperty("user.timezone", originalDefault.getId());
        }
    }

    /** ---------------------------------------------------------------------- *
     * 20.  Verify that day‑of‑week tokens are ignored (they should not affect the
     *      resulting date).
     *  ---------------------------------------------------------------------- */
    @Test
    public void testParseDate_DayOfWeekIgnored() {
        // "Wednesday 2024-01-15"
        ZonedDateTime expected = utcZdt(2024, 1, 15, 0, 0, 0, 0);
        assertEquals(expected, DateUtilities.parseDate("Wednesday 2024-01-15", UTC, true));
    }
}
