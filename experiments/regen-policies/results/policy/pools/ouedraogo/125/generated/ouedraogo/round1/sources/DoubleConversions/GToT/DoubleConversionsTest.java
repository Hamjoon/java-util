package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Comprehensive JUnit‑4 test suite for {@link DoubleConversions}.
 *
 * The suite follows the steps required in the prompt:
 *   1. Lists all static methods (they are package‑private, therefore the test class
 *      lives in the same package to gain access).
 *   2. Provides a basic “happy‑path” test for each method.
 *   3. Identifies edge‑cases and exception scenarios.
 *   4. Adds dedicated tests for those edge‑cases.
 *   5. Merges everything into a single test file.
 *
 * A tiny stub {@code ConverterOptions} implementation is supplied so that a
 * real {@link Converter} instance can be created without pulling in the whole
 * library.  Only the two methods used by {@code DoubleConversions} are required:
 * {@code getTimeZone()} and {@code getZoneId()}.
 */
public class DoubleConversionsTest
{
    /* --------------------------------------------------------------------- *
     *  Stub supporting class – only the API used by DoubleConversions.
     * --------------------------------------------------------------------- */
    static final class StubConverterOptions
    {
        private final TimeZone timeZone;
        private final ZoneId zoneId;

        StubConverterOptions(TimeZone tz, ZoneId zid)
        {
            this.timeZone = tz;
            this.zoneId   = zid;
        }

        public TimeZone getTimeZone()
        {
            return timeZone;
        }

        public ZoneId getZoneId()
        {
            return zoneId;
        }
    }

    private Converter converter;   // real Converter (final class)

    @Before
    public void setUp()
    {
        // The library's Converter is final and expects a ConverterOptions object.
        // Our stub satisfies the two accessor methods that DoubleConversions uses.
        StubConverterOptions options = new StubConverterOptions(TimeZone.getTimeZone("UTC"),
                                                               ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- *
     *  1.  List of public (package‑private static) methods in DoubleConversions
     * --------------------------------------------------------------------- *
     *
     *  static Instant          toInstant(Object from, Converter converter)
     *  static Date            toDate(Object from, Converter converter)
     *  static Date            toSqlDate(Object from, Converter converter)
     *  static Calendar        toCalendar(Object from, Converter converter)
     *  static LocalTime       toLocalTime(Object from, Converter converter)
     *  static LocalDate       toLocalDate(Object from, Converter converter)
     *  static LocalDateTime   toLocalDateTime(Object from, Converter converter)
     *  static ZonedDateTime   toZonedDateTime(Object from, Converter converter)
     *  static OffsetDateTime  toOffsetDateTime(Object from, Converter converter)
     *  static Timestamp       toTimestamp(Object from, Converter converter)
     *  static Duration        toDuration(Object from, Converter converter)
     */

    /* --------------------------------------------------------------------- *
     *  2. Basic happy‑path tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToInstant_Basic()
    {
        // 2021‑01‑01T00:00:00.123456789Z  → epoch seconds = 1609459200
        double d = 1609459200.123456789;
        Instant expected = Instant.ofEpochSecond(1609459200, 123_456_789);
        Instant actual   = DoubleConversions.toInstant(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate_Basic()
    {
        double d = 1.5;                 // 1.5 seconds → 1500 ms
        Date expected = new Date(1500L);
        Date actual   = DoubleConversions.toDate(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToSqlDate_Basic()
    {
        double d = 86400.0;             // exactly one day
        java.sql.Date expected = new java.sql.Date(86_400_000L);
        java.sql.Date actual   = (java.sql.Date) DoubleConversions.toSqlDate(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar_Basic()
    {
        double seconds = 12345.678;     // → 12 345 678 ms
        Calendar cal = DoubleConversions.toCalendar(seconds, converter);
        assertEquals(12_345_678L, cal.getTimeInMillis());
        // UTC time‑zone is enforced by the stub options
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testToLocalTime_Basic()
    {
        double seconds = 3661.5; // 1 h 1 m 1.5 s → 01:01:01.500000000
        LocalTime expected = LocalTime.of(1, 1, 1, 500_000_000);
        LocalTime actual   = DoubleConversions.toLocalTime(seconds, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDate_Basic()
    {
        double epochSeconds = 0; // 1970‑01‑01
        LocalDate expected = LocalDate.of(1970, 1, 1);
        LocalDate actual   = DoubleConversions.toLocalDate(epochSeconds, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime_Basic()
    {
        double epochSeconds = 0; // 1970‑01‑01T00:00:00
        LocalDateTime expected = LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime actual   = DoubleConversions.toLocalDateTime(epochSeconds, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToZonedDateTime_Basic()
    {
        double epochSeconds = 0;
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));
        ZonedDateTime actual   = DoubleConversions.toZonedDateTime(epochSeconds, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime_Basic()
    {
        double epochSeconds = 0;
        OffsetDateTime expected = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));
        OffsetDateTime actual   = DoubleConversions.toOffsetDateTime(epochSeconds, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToTimestamp_Basic()
    {
        double epochSeconds = 0;
        Timestamp expected = new Timestamp(0L);
        Timestamp actual   = DoubleConversions.toTimestamp(epochSeconds, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDuration_Basic()
    {
        double d = 2.75; // 2 seconds + 750 ms
        Duration expected = Duration.ofSeconds(2, 750_000_000);
        Duration actual   = DoubleConversions.toDuration(d, converter);
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------------- *
     *  3. Edge‑case & exception tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToInstant_RoundingOverflow()
    {
        // 1.9999999995 → seconds = 1, fractional part rounds to 1_000_000_000
        double d = 1.9999999995;
        try
        {
            DoubleConversions.toInstant(d, converter);
            fail("Expected DateTimeException because nanos = 1_000_000_000 is out of range");
        }
        catch (DateTimeException ex)
        {
            // expected
        }
    }

    @Test
    public void testToInstant_NegativeInstant()
    {
        double d = -0.5; // -0.5 seconds → -0 seconds, -500_000_000 nanos
        Instant expected = Instant.ofEpochSecond(0, -500_000_000);
        Instant actual   = DoubleConversions.toInstant(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime_EdgeMaximum()
    {
        // maximum allowed value: 86 399.999 999 999 seconds (one nanosecond before next day)
        double max = 86_399.999_999_999;
        LocalTime expected = LocalTime.MAX; // 23:59:59.999999999
        LocalTime actual   = DoubleConversions.toLocalTime(max, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime_Negative_Throws()
    {
        double negative = -1.0;
        try
        {
            DoubleConversions.toLocalTime(negative, converter);
            fail("Expected IllegalArgumentException for negative seconds");
        }
        catch (IllegalArgumentException ex)
        {
            assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    @Test
    public void testToLocalTime_AboveMaximum_Throws()
    {
        double above = 86_400.0; // exactly 24 hours – out of range
        try
        {
            DoubleConversions.toLocalTime(above, converter);
            fail("Expected IllegalArgumentException for seconds > maximum");
        }
        catch (IllegalArgumentException ex)
        {
            assertTrue(ex.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    @Test
    public void testToDuration_NegativeFraction()
    {
        double d = -2.3; // -2 seconds -300 ms
        // Duration normalises the negative nanos part.
        Duration expected = Duration.ofSeconds(-2, -300_000_000);
        Duration actual   = DoubleConversions.toDuration(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDuration_Rounding()
    {
        double d = 0.0000000004; // less than one nanosecond, should round to 0
        Duration expected = Duration.ofSeconds(0, 0);
        Duration actual   = DoubleConversions.toDuration(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar_TimeZoneEffect()
    {
        // The stub forces UTC; verify that the Calendar's zone matches it.
        double seconds = 0;
        Calendar cal = DoubleConversions.toCalendar(seconds, converter);
        assertEquals("UTC", cal.getTimeZone().getID());
    }
}
