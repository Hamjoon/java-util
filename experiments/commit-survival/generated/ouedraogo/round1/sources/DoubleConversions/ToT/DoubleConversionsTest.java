package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link DoubleConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical values (positive, zero, fractional seconds)</li>
 *   <li>Boundary values (maximum LocalTime, negative epoch)</li>
 *   <li>Error scenarios (out‑of‑range LocalTime)</li>
 *   <li>Interaction with {@link Converter} options (UTC time‑zone / zone‑id)</li>
 * </ul>
 *
 * All objects are created via real constructors – no mocking is used.
 */
public class DoubleConversionsTest {

    private Converter utcConverter;

    /**
     * Creates a {@link Converter} whose {@link ConverterOptions} are configured
     * with the UTC time‑zone and zone‑id.  The library supplies a default
     * {@code ConverterOptions} class with setter methods; if the real API
     * differs the test can be adapted accordingly.
     */
    @Before
    public void setUp() {
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        options.setZoneId(ZoneId.of("UTC"));
        utcConverter = new Converter(options);
    }

    /* --------------------------------------------------------------------- */
    /*  toInstant                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToInstantTypical() {
        // 2021‑06‑01T12:00:00.123456789Z  => epoch seconds = 1622558400
        double value = 1622558400.123456789;
        Instant result = DoubleConversions.toInstant(value, utcConverter);

        assertEquals(1622558400L, result.getEpochSecond());
        assertEquals(123_456_789L, result.getNano());
    }

    @Test
    public void testToInstantNegativeEpoch() {
        // 1969‑12‑31T23:59:59.5Z  => -0.5 seconds from epoch
        double value = -0.5;
        Instant result = DoubleConversions.toInstant(value, utcConverter);

        assertEquals(-1L, result.getEpochSecond()); // floor to whole seconds
        assertEquals(500_000_000L, result.getNano()); // 0.5 s = 500 ms = 500 000 000 ns
    }

    /* --------------------------------------------------------------------- */
    /*  toDate                                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToDateMilliseconds() {
        double seconds = 1_234_567.89; // 1 234 567 890 ms
        Date date = DoubleConversions.toDate(seconds, utcConverter);

        assertEquals(1_234_567_890L, date.getTime());
    }

    @Test
    public void testToDateZero() {
        Date date = DoubleConversions.toDate(0.0, utcConverter);
        assertEquals(0L, date.getTime());
    }

    /* --------------------------------------------------------------------- */
    /*  toSqlDate                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToSqlDate() {
        double seconds = 86_400.0; // exactly one day
        java.sql.Date sqlDate = (java.sql.Date) DoubleConversions.toSqlDate(seconds, utcConverter);

        // java.sql.Date represents a date at midnight UTC
        assertEquals(86_400_000L, sqlDate.getTime());
    }

    /* --------------------------------------------------------------------- */
    /*  toCalendar                                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCalendarUtc() {
        double seconds = 1_600_000_000.0; // epoch millis = 1_600_000_000_000
        Calendar cal = DoubleConversions.toCalendar(seconds, utcConverter);

        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(1_600_000_000_000L, cal.getTimeInMillis());
    }

    @Test
    public void testToCalendarWithFraction() {
        double seconds = 1_600_000_000.123; // .123 s = 123 ms
        Calendar cal = DoubleConversions.toCalendar(seconds, utcConverter);

        assertEquals(1_600_000_000_123L, cal.getTimeInMillis());
    }

    /* --------------------------------------------------------------------- */
    /*  toLocalTime                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToLocalTimeTypical() {
        double seconds = 45_000.5; // 12:30:00.5
        LocalTime lt = DoubleConversions.toLocalTime(seconds, utcConverter);
        assertEquals(LocalTime.of(12, 30, 0, 500_000_000), lt);
    }

    @Test
    public void testToLocalTimeMaxBoundary() {
        // 23:59:59.999999999  -> 86 399.999999999 seconds
        double seconds = 86_399.999_999_999;
        LocalTime lt = DoubleConversions.toLocalTime(seconds, utcConverter);
        assertEquals(LocalTime.MAX, lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeNegativeFails() {
        DoubleConversions.toLocalTime(-1.0, utcConverter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeBeyondDayFails() {
        // 24 hours = 86 400 seconds – out of range
        DoubleConversions.toLocalTime(86_400.0, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /*  toZonedDateTime, toLocalDate, toLocalDateTime, toOffsetDateTime       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testZonedDateTimeAndRelatedConversions() {
        double epochSeconds = 1_600_000_000.250; // 2020‑09‑13T12:26:40.250Z
        ZonedDateTime zdt = DoubleConversions.toZonedDateTime(epochSeconds, utcConverter);
        assertEquals(ZoneOffset.UTC, zdt.getOffset());
        assertEquals(Instant.ofEpochSecond(1_600_000_000, 250_000_000), zdt.toInstant());

        LocalDate ld = DoubleConversions.toLocalDate(epochSeconds, utcConverter);
        assertEquals(LocalDate.of(2020, 9, 13), ld);

        LocalDateTime ldt = DoubleConversions.toLocalDateTime(epochSeconds, utcConverter);
        assertEquals(LocalDateTime.of(2020, 9, 13, 12, 26, 40, 250_000_000), ldt);

        OffsetDateTime odt = DoubleConversions.toOffsetDateTime(epochSeconds, utcConverter);
        assertEquals(OffsetDateTime.ofInstant(Instant.ofEpochSecond(1_600_000_000, 250_000_000), ZoneOffset.UTC), odt);
    }

    /* --------------------------------------------------------------------- */
    /*  toTimestamp                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToTimestamp() {
        double epochSeconds = 1_500_000_000.75; // 2017‑07‑14T02:40:00.75Z
        Timestamp ts = DoubleConversions.toTimestamp(epochSeconds, utcConverter);
        assertEquals(Timestamp.from(Instant.ofEpochSecond(1_500_000_000, 750_000_000)), ts);
    }

    /* --------------------------------------------------------------------- */
    /*  toDuration                                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToDurationWholeAndFraction() {
        double value = 12345.678901234; // 12345 s + 678_901_234 ns
        Duration dur = DoubleConversions.toDuration(value, utcConverter);
        assertEquals(12345L, dur.getSeconds());
        assertEquals(678_901_234L, dur.getNano());
    }

    @Test
    public void testToDurationZero() {
        Duration dur = DoubleConversions.toDuration(0.0, utcConverter);
        assertEquals(Duration.ZERO, dur);
    }

    @Test
    public void testToDurationNegative() {
        double value = -10.5; // -10 s and -0.5 s => -11 s + 500 000 000 ns (java.time normalises)
        Duration dur = DoubleConversions.toDuration(value, utcConverter);
        // Normalised representation
        assertEquals(-11L, dur.getSeconds());
        assertEquals(500_000_000L, dur.getNano());
    }
}
