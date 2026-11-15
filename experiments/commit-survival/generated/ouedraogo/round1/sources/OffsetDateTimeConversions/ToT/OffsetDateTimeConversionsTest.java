package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link OffsetDateTimeConversions}.
 *
 * The tests cover:
 *   • Normal (typical) conversions for every method.
 *   • Edge cases such as null input (expected {@link NullPointerException}).
 *   • Different offsets and explicit UTC configuration via {@link ConverterOptions}
 *     to guarantee deterministic results regardless of the host JVM default zone.
 */
public class OffsetDateTimeConversionsTest {

    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    private Converter converter;
    private OffsetDateTime sampleOffsetDateTime;
    private OffsetDateTime negativeOffsetDateTime;

    @Before
    public void setUp() {
        // Build a Converter whose options explicitly use UTC – this makes all
        // zone‑dependent conversions deterministic.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(UTC_ZONE);
        options.setTimeZone(UTC_TZ);
        converter = new Converter(options);

        // Sample OffsetDateTime with a positive offset (+02:30)
        sampleOffsetDateTime = OffsetDateTime.of(
                2023, 5, 15,          // date
                10, 30, 45, 123_456_789, // time + nanos
                ZoneOffset.ofHoursMinutes(2, 30));

        // Sample OffsetDateTime with a negative offset (‑05:00)
        negativeOffsetDateTime = OffsetDateTime.of(
                2021, 12, 31,
                23, 59, 59, 0,
                ZoneOffset.ofHours(-5));
    }

    /* --------------------------------------------------------------------- *
     *  toInstant
     * --------------------------------------------------------------------- */
    @Test
    public void testToInstant_Typical() {
        Instant expected = sampleOffsetDateTime.toInstant();
        Instant actual = OffsetDateTimeConversions.toInstant(sampleOffsetDateTime, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput() {
        OffsetDateTimeConversions.toInstant(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toLong
     * --------------------------------------------------------------------- */
    @Test
    public void testToLong_Typical() {
        long expected = sampleOffsetDateTime.toInstant().toEpochMilli();
        long actual = OffsetDateTimeConversions.toLong(sampleOffsetDateTime, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        OffsetDateTimeConversions.toLong(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toLocalDateTime
     * --------------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime_Typical() {
        LocalDateTime expected = OffsetDateTimeConversions
                .toZonedDateTime(sampleOffsetDateTime, converter)
                .toLocalDateTime();

        LocalDateTime actual = OffsetDateTimeConversions
                .toLocalDateTime(sampleOffsetDateTime, converter);

        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullInput() {
        OffsetDateTimeConversions.toLocalDateTime(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toLocalDate
     * --------------------------------------------------------------------- */
    @Test
    public void testToLocalDate_Typical() {
        LocalDate expected = OffsetDateTimeConversions
                .toZonedDateTime(sampleOffsetDateTime, converter)
                .toLocalDate();

        LocalDate actual = OffsetDateTimeConversions
                .toLocalDate(sampleOffsetDateTime, converter);

        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullInput() {
        OffsetDateTimeConversions.toLocalDate(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toLocalTime
     * --------------------------------------------------------------------- */
    @Test
    public void testToLocalTime_Typical() {
        LocalTime expected = OffsetDateTimeConversions
                .toZonedDateTime(sampleOffsetDateTime, converter)
                .toLocalTime();

        LocalTime actual = OffsetDateTimeConversions
                .toLocalTime(sampleOffsetDateTime, converter);

        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullInput() {
        OffsetDateTimeConversions.toLocalTime(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toAtomicLong
     * --------------------------------------------------------------------- */
    @Test
    public void testToAtomicLong_Typical() {
        long epochMilli = sampleOffsetDateTime.toInstant().toEpochMilli();
        AtomicLong expected = new AtomicLong(epochMilli);
        AtomicLong actual = OffsetDateTimeConversions.toAtomicLong(sampleOffsetDateTime, converter);
        assertEquals(expected.get(), actual.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput() {
        OffsetDateTimeConversions.toAtomicLong(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toTimestamp
     * --------------------------------------------------------------------- */
    @Test
    public void testToTimestamp_Typical() {
        Timestamp expected = Timestamp.from(sampleOffsetDateTime.toInstant());
        Timestamp actual = OffsetDateTimeConversions.toTimestamp(sampleOffsetDateTime, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullInput() {
        OffsetDateTimeConversions.toTimestamp(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toCalendar
     * --------------------------------------------------------------------- */
    @Test
    public void testToCalendar_Typical() {
        long epochMilli = sampleOffsetDateTime.toInstant().toEpochMilli();
        Calendar cal = OffsetDateTimeConversions.toCalendar(sampleOffsetDateTime, converter);
        assertEquals(UTC_TZ, cal.getTimeZone());
        assertEquals(epochMilli, cal.getTimeInMillis());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullInput() {
        OffsetDateTimeConversions.toCalendar(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toSqlDate
     * --------------------------------------------------------------------- */
    @Test
    public void testToSqlDate_Typical() {
        long epochMilli = sampleOffsetDateTime.toInstant().toEpochMilli();
        java.sql.Date expected = new java.sql.Date(epochMilli);
        java.sql.Date actual = OffsetDateTimeConversions.toSqlDate(sampleOffsetDateTime, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullInput() {
        OffsetDateTimeConversions.toSqlDate(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toZonedDateTime
     * --------------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime_Typical() {
        ZonedDateTime expected = sampleOffsetDateTime.toInstant().atZone(UTC_ZONE);
        ZonedDateTime actual = OffsetDateTimeConversions.toZonedDateTime(sampleOffsetDateTime, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullInput() {
        OffsetDateTimeConversions.toZonedDateTime(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toDate
     * --------------------------------------------------------------------- */
    @Test
    public void testToDate_Typical() {
        long epochMilli = sampleOffsetDateTime.toInstant().toEpochMilli();
        Date expected = new Date(epochMilli);
        Date actual = OffsetDateTimeConversions.toDate(sampleOffsetDateTime, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullInput() {
        OffsetDateTimeConversions.toDate(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toBigInteger
     * --------------------------------------------------------------------- */
    @Test
    public void testToBigInteger_Typical() {
        // The exact representation depends on InstantConversions; we only verify
        // that the conversion succeeds and the value is non‑negative for the
        // epoch‑millis we use.
        BigInteger result = OffsetDateTimeConversions.toBigInteger(sampleOffsetDateTime, converter);
        assertNotNull(result);
        assertTrue(result.compareTo(BigInteger.ZERO) >= 0);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        OffsetDateTimeConversions.toBigInteger(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toBigDecimal
     * --------------------------------------------------------------------- */
    @Test
    public void testToBigDecimal_Typical() {
        BigDecimal result = OffsetDateTimeConversions.toBigDecimal(sampleOffsetDateTime, converter);
        assertNotNull(result);
        // The decimal representation of the instant should be close to the epoch
        // seconds plus the fractional nanos.
        Instant instant = sampleOffsetDateTime.toInstant();
        BigDecimal expected = new BigDecimal(instant.getEpochSecond())
                .add(new BigDecimal(instant.getNano()).movePointLeft(9));
        assertEquals(0, expected.compareTo(result));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        OffsetDateTimeConversions.toBigDecimal(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toOffsetTime
     * --------------------------------------------------------------------- */
    @Test
    public void testToOffsetTime_Typical() {
        OffsetTime expected = sampleOffsetDateTime.toOffsetTime();
        OffsetTime actual = OffsetDateTimeConversions.toOffsetTime(sampleOffsetDateTime, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetTime_NullInput() {
        OffsetDateTimeConversions.toOffsetTime(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toString
     * --------------------------------------------------------------------- */
    @Test
    public void testToString_Typical() {
        String expected = sampleOffsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String actual = OffsetDateTimeConversions.toString(sampleOffsetDateTime, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        OffsetDateTimeConversions.toString(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toMap
     * --------------------------------------------------------------------- */
    @Test
    public void testToMap_Typical() {
        Map<String, Object> map = OffsetDateTimeConversions.toMap(sampleOffsetDateTime, converter);
        ZonedDateTime zdt = OffsetDateTimeConversions.toZonedDateTime(sampleOffsetDateTime, converter);

        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(zdt.getOffset().toString(), map.get(MapConversions.OFFSET));
        assertEquals(3, map.size());
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        OffsetDateTimeConversions.toMap(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  toDouble
     * --------------------------------------------------------------------- */
    @Test
    public void testToDouble_Typical() {
        Instant instant = sampleOffsetDateTime.toInstant();
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        double actual = OffsetDateTimeConversions.toDouble(sampleOffsetDateTime, converter);
        assertEquals(expected, actual, 1e-12);
    }

    @Test
    public void testToDouble_NegativeOffset() {
        // Verify that the conversion works for a date‑time with a negative offset.
        Instant instant = negativeOffsetDateTime.toInstant();
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        double actual = OffsetDateTimeConversions.toDouble(negativeOffsetDateTime, converter);
        assertEquals(expected, actual, 1e-12);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput() {
        OffsetDateTimeConversions.toDouble(null, converter);
    }
}
