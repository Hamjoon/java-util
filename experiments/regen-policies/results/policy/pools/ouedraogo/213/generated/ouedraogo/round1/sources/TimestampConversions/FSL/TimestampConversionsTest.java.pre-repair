package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit 4 test suite that exercises every public static method in
 * {@link TimestampConversions}.  The tests use real {@link Timestamp}
 * instances and a real {@link Converter} (with default options) – no
 * mocking or subclassing of library types is performed.
 */
public class TimestampConversionsTest {

    private Converter converter;

    /** A Timestamp with a full‑nanosecond value (not a clean millisecond). */
    private Timestamp tsNanosecond;

    /** A Timestamp that ends exactly on a millisecond boundary. */
    private Timestamp tsMillisecond;

    @Before
    public void setUp() {
        // The library provides a no‑arg constructor for ConverterOptions.
        // Using the default options gives a ZoneId of the JVM default
        // and a matching TimeZone, which is sufficient for the tests.
        converter = new Converter(new ConverterOptions());

        // 2023‑03‑15T12:34:56.789123456Z  (nanosecond precision)
        LocalDateTime ldtNano = LocalDateTime.of(
                2023, 3, 15,
                12, 34, 56,
                789_123_456);               // nanoseconds
        tsNanosecond = Timestamp.valueOf(ldtNano);

        // 2023‑03‑15T12:34:56.789Z  (exact millisecond)
        LocalDateTime ldtMilli = LocalDateTime.of(
                2023, 3, 15,
                12, 34, 56,
                789_000_000);               // nanoseconds = 789 ms
        tsMillisecond = Timestamp.valueOf(ldtMilli);
    }

    /* -------------------------------------------------------------
     *  toDouble
     * ------------------------------------------------------------- */
    @Test
    public void testToDouble() {
        double actual = TimestampConversions.toDouble(tsNanosecond, converter);
        Duration d = Duration.between(Instant.EPOCH, tsNanosecond.toInstant());
        BigDecimal expectedBd = BigDecimalConversions
                .secondsAndNanosToDouble(d.getSeconds(), d.getNano());
        double expected = expectedBd.doubleValue();

        assertEquals("double conversion must match the Duration‑based calculation",
                expected, actual, 0.0);
    }

    /* -------------------------------------------------------------
     *  toBigDecimal
     * ------------------------------------------------------------- */
    @Test
    public void testToBigDecimal() {
        BigDecimal actual = TimestampConversions.toBigDecimal(tsNanosecond, converter);
        Instant instant = tsNanosecond.toInstant();

        // Expected: seconds + nanos / 1_000_000_000 (scale = 9)
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano(), 9));

        assertEquals("BigDecimal should represent epoch seconds with 9‑digit nanos",
                0, expected.compareTo(actual));
    }

    /* -------------------------------------------------------------
     *  toBigInteger
     * ------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        BigInteger actual = TimestampConversions.toBigInteger(tsNanosecond, converter);
        Instant instant = tsNanosecond.toInstant();

        // Expected: epoch seconds as a BigInteger
        BigInteger expected = BigInteger.valueOf(instant.getEpochSecond());

        assertEquals("BigInteger conversion should yield epoch seconds", expected, actual);
    }

    /* -------------------------------------------------------------
     *  toLocalDateTime
     * ------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime() {
        LocalDateTime actual = TimestampConversions.toLocalDateTime(tsNanosecond, converter);
        // The conversion uses the converter's ZoneId; with default options that
        // is the JVM default zone.  For reproducibility we compare against the
        // same calculation performed manually.
        LocalDateTime expected = tsNanosecond.toInstant()
                .atZone(converter.getOptions().getZoneId())
                .toLocalDateTime();

        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  toDuration
     * ------------------------------------------------------------- */
    @Test
    public void testToDuration() {
        Duration actual = TimestampConversions.toDuration(tsNanosecond, converter);
        Duration expected = Duration.between(Instant.EPOCH, tsNanosecond.toInstant());
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  toOffsetDateTime
     * ------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime actual = TimestampConversions.toOffsetDateTime(tsNanosecond, converter);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(
                tsNanosecond.toInstant(),
                converter.getOptions().getZoneId());

        OffsetDateTime expected = zdt.toOffsetDateTime();
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  toCalendar
     * ------------------------------------------------------------- */
    @Test
    public void testToCalendar() {
        Calendar actual = TimestampConversions.toCalendar(tsNanosecond, converter);
        Calendar expected = Calendar.getInstance(converter.getOptions().getTimeZone());
        expected.setTimeInMillis(tsNanosecond.getTime());

        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
        assertEquals(expected.getTimeZone(), actual.getTimeZone());
    }

    /* -------------------------------------------------------------
     *  toDate
     * ------------------------------------------------------------- */
    @Test
    public void testToDate() {
        Date actual = TimestampConversions.toDate(tsNanosecond, converter);
        Date expected = Date.from(tsNanosecond.toInstant());
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  toSqlDate
     * ------------------------------------------------------------- */
    @Test
    public void testToSqlDate() {
        java.sql.Date actual = TimestampConversions.toSqlDate(tsNanosecond, converter);
        java.sql.Date expected = java.sql.Date.valueOf(
                tsNanosecond.toInstant()
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate());
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  toLong
     * ------------------------------------------------------------- */
    @Test
    public void testToLong() {
        long actual = TimestampConversions.toLong(tsNanosecond, converter);
        long expected = tsNanosecond.getTime();
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  toYear
     * ------------------------------------------------------------- */
    @Test
    public void testToYear() {
        Year actual = TimestampConversions.toYear(tsNanosecond, converter);
        Year expected = Year.from(
                tsNanosecond.toInstant()
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate());
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  toYearMonth
     * ------------------------------------------------------------- */
    @Test
    public void testToYearMonth() {
        YearMonth actual = TimestampConversions.toYearMonth(tsNanosecond, converter);
        YearMonth expected = YearMonth.from(
                tsNanosecond.toInstant()
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate());
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  toMonthDay
     * ------------------------------------------------------------- */
    @Test
    public void testToMonthDay() {
        MonthDay actual = TimestampConversions.toMonthDay(tsNanosecond, converter);
        MonthDay expected = MonthDay.from(
                tsNanosecond.toInstant()
                        .atZone(converter.getOptions().getZoneId())
                        .toLocalDate());
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  toString – nanosecond precision
     * ------------------------------------------------------------- */
    @Test
    public void testToString_NanosecondPrecision() {
        String actual = TimestampConversions.toString(tsNanosecond, converter);
        // Expected pattern: 9‑digit fraction, UTC (Z)
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
                .withZone(ZoneOffset.UTC);
        String expected = fmt.format(tsNanosecond.toInstant());

        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  toString – millisecond precision
     * ------------------------------------------------------------- */
    @Test
    public void testToString_MillisecondPrecision() {
        String actual = TimestampConversions.toString(tsMillisecond, converter);
        // Expected pattern: 3‑digit fraction, UTC (Z)
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC);
        String expected = fmt.format(tsMillisecond.toInstant());

        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  toMap
     * ------------------------------------------------------------- */
    @Test
    public void testToMap() {
        Map<String, Object> map = TimestampConversions.toMap(tsNanosecond, converter);
        assertNotNull("Map must not be null", map);
        assertTrue("Map must contain the TIMESTAMP key",
                map.containsKey(MapConversions.TIMESTAMP));

        String formatted = TimestampConversions.toString(tsNanosecond, converter);
        assertEquals("Map value must be the formatted timestamp string",
                formatted, map.get(MapConversions.TIMESTAMP));
    }
}
