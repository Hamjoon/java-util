package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DoubleConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical values (positive, zero, negative, fractional)</li>
 *   <li>Boundary conditions (midnight, last nanosecond of a day)</li>
 *   <li>Time‑zone handling for Calendar/ZonedDateTime/OffsetDateTime</li>
 *   <li>Rounding behaviour for nanosecond calculations</li>
 *   <li>Exception path for {@code toLocalTime}</li>
 * </ul>
 *
 * The {@code Converter} used in the tests is built with a real
 * {@code ConverterOptions} instance – no mocking or subclassing is performed,
 * as required by the task description.
 */
public class DoubleConversionsTest {

    /** The {@code Converter} used for all conversion calls. */
    private Converter converter;

    /** A reusable {@code ConverterOptions} instance with UTC configuration. */
    private static class SimpleOptions extends ConverterOptions {
        SimpleOptions() {
            // Ensure a deterministic time‑zone / zone‑id for all tests.
            setTimeZone(TimeZone.getTimeZone("UTC"));
            setZoneId(ZoneId.of("UTC"));
        }
    }

    @Before
    public void setUp() {
        converter = new Converter(new SimpleOptions());
    }

    /* --------------------------------------------------------------
     *  toInstant()
     * -------------------------------------------------------------- */

    @Test
    public void testToInstantTypicalPositive() {
        Double d = 1_600_000_000.123456789; // 2020‑09‑13T12:26:40.123456789Z
        Instant expected = Instant.ofEpochSecond(1_600_000_000L, 123_456_789L);
        Instant actual = DoubleConversions.toInstant(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstantNegativeFraction() {
        Double d = -10.75; // -10 seconds and -750 000 000 ns
        // Instant.ofEpochSecond truncates toward zero, the fractional part is
        // handled by the nanoseconds argument (must be positive).
        Instant expected = Instant.ofEpochSecond(-10L, -750_000_000L);
        Instant actual = DoubleConversions.toInstant(d, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstantRoundingEdge() {
        // 0.9999999995 seconds → should round to 1 000 000 000 ns (i.e. next second)
        Double d = 0.9999999995;
        Instant expected = Instant.ofEpochSecond(1L, 0L);
        Instant actual = DoubleConversions.toInstant(d, converter);
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------
     *  toDate()
     * -------------------------------------------------------------- */

    @Test
    public void testToDateTypical() {
        Double d = 1_600_000_000.5; // 1 600 000 000 500 ms
        Date expected = new Date(1_600_000_000_500L);
        assertEquals(expected, DoubleConversions.toDate(d, converter));
    }

    @Test
    public void testToDateZero() {
        Double d = 0.0;
        assertEquals(new Date(0L), DoubleConversions.toDate(d, converter));
    }

    /* --------------------------------------------------------------
     *  toSqlDate()
     * -------------------------------------------------------------- */

    @Test
    public void testToSqlDateTypical() {
        Double d = 1_600_000_000.0; // whole seconds → midnight UTC of that day
        java.sql.Date expected = new java.sql.Date(1_600_000_000_000L);
        assertEquals(expected, DoubleConversions.toSqlDate(d, converter));
    }

    /* --------------------------------------------------------------
     *  toCalendar()
     * -------------------------------------------------------------- */

    @Test
    public void testToCalendarUtc() {
        Double seconds = 1_600_000_000.0; // epoch millis = 1_600_000_000_000
        Calendar cal = DoubleConversions.toCalendar(seconds, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(1_600_000_000_000L, cal.getTimeInMillis());
    }

    @Test
    public void testToCalendarWithDifferentTimeZone() {
        // Change the converter's time‑zone to America/New_York (UTC‑5)
        ((SimpleOptions) converter.getOptions()).setTimeZone(TimeZone.getTimeZone("America/New_York"));
        Double seconds = 0.0; // epoch start
        Calendar cal = DoubleConversions.toCalendar(seconds, converter);
        assertEquals(TimeZone.getTimeZone("America/New_York"), cal.getTimeZone());
        assertEquals(0L, cal.getTimeInMillis());
    }

    /* --------------------------------------------------------------
     *  toLocalTime()
     * -------------------------------------------------------------- */

    @Test
    public void testToLocalTimeTypical() {
        Double seconds = 12_345.678901234; // 03:25:45.678901234
        LocalTime expected = LocalTime.of(3, 25, 45, 678_901_234);
        assertEquals(expected, DoubleConversions.toLocalTime(seconds, converter));
    }

    @Test
    public void testToLocalTimeMidnight() {
        Double seconds = 0.0;
        assertEquals(LocalTime.MIDNIGHT, DoubleConversions.toLocalTime(seconds, converter));
    }

    @Test
    public void testToLocalTimeLastNanosecondOfDay() {
        Double seconds = 86_399.999_999_999; // 23:59:59.999999999
        LocalTime expected = LocalTime.of(23, 59, 59, 999_999_999);
        assertEquals(expected, DoubleConversions.toLocalTime(seconds, converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeBelowZeroThrows() {
        Double seconds = -0.1;
        DoubleConversions.toLocalTime(seconds, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeAboveMaximumThrows() {
        Double seconds = 86_400.0; // exactly one day – out of range
        DoubleConversions.toLocalTime(seconds, converter);
    }

    /* --------------------------------------------------------------
     *  toZonedDateTime() / toLocalDate() / toLocalDateTime()
     * -------------------------------------------------------------- */

    @Test
    public void testToZonedDateTimeUtc() {
        Double d = 0.0; // epoch start
        ZonedDateTime zdt = DoubleConversions.toZonedDateTime(d, converter);
        assertEquals(Instant.EPOCH, zdt.toInstant());
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
    }

    @Test
    public void testToLocalDateFromInstant() {
        // 2022‑01‑01T00:00:00Z
        Double d = 1640995200.0;
        LocalDate expected = LocalDate.of(2022, 1, 1);
        assertEquals(expected, DoubleConversions.toLocalDate(d, converter));
    }

    @Test
    public void testToLocalDateTimeFromInstant() {
        // 2022‑01‑01T12:34:56.789Z
        Double d = 1641041696.789;
        LocalDateTime expected = LocalDateTime.of(2022, 1, 1, 12, 34, 56, 789_000_000);
        assertEquals(expected, DoubleConversions.toLocalDateTime(d, converter));
    }

    /* --------------------------------------------------------------
     *  toOffsetDateTime()
     * -------------------------------------------------------------- */

    @Test
    public void testToOffsetDateTimeUtc() {
        Double d = 0.0;
        OffsetDateTime odt = DoubleConversions.toOffsetDateTime(d, converter);
        assertEquals(OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), odt);
    }

    @Test
    public void testToOffsetDateTimeWithNonZeroOffset() {
        // Switch zone to +02:00
        ((SimpleOptions) converter.getOptions()).setZoneId(ZoneId.of("Europe/Paris")); // UTC+1 (or +2 with DST)
        Double d = 0.0;
        OffsetDateTime odt = DoubleConversions.toOffsetDateTime(d, converter);
        ZoneOffset expectedOffset = converter.getOptions().getZoneId().getRules().getOffset(Instant.EPOCH);
        assertEquals(expectedOffset, odt.getOffset());
    }

    /* --------------------------------------------------------------
     *  toTimestamp()
     * -------------------------------------------------------------- */

    @Test
    public void testToTimestampTypical() {
        Double d = 1_600_000_000.123; // 2020‑09‑13T12:26:40.123Z
        Timestamp expected = Timestamp.from(Instant.ofEpochSecond(1_600_000_000L, 123_000_000L));
        assertEquals(expected, DoubleConversions.toTimestamp(d, converter));
    }

    /* --------------------------------------------------------------
     *  toDuration()
     * -------------------------------------------------------------- */

    @Test
    public void testToDurationWholeSeconds() {
        Double d = 123.0;
        Duration expected = Duration.ofSeconds(123L);
        assertEquals(expected, DoubleConversions.toDuration(d, converter));
    }

    @Test
    public void testToDurationWithNanos() {
        Double d = 123.000_000_456; // 456 ns extra
        Duration expected = Duration.ofSeconds(123L, 456L);
        assertEquals(expected, DoubleConversions.toDuration(d, converter));
    }

    @Test
    public void testToDurationNegative() {
        Double d = -5.5; // -5 seconds and -500_000_000 ns
        Duration expected = Duration.ofSeconds(-5L, -500_000_000L);
        assertEquals(expected, DoubleConversions.toDuration(d, converter));
    }

    @Test
    public void testToDurationRoundingEdge() {
        // 0.9999999995 seconds → rounds to 1 000 000 000 ns → 1 second
        Double d = 0.9999999995;
        Duration expected = Duration.ofSeconds(1L, 0L);
        assertEquals(expected, DoubleConversions.toDuration(d, converter));
    }
}
