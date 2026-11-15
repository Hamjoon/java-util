package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link BigIntegerConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical successful conversions for each method.</li>
 *   <li>Boundary conditions (zero, maximum allowed values).</li>
 *   <li>Error scenarios (negative numbers where not allowed).</li>
 * </ul>
 *
 * The {@link Converter} is instantiated with default {@link ConverterOptions}
 * (which use UTC zone and UTC time‑zone). This keeps the tests deterministic.
 */
public class BigIntegerConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The concrete implementation of ConverterOptions is not shown,
        // but it provides a no‑arg constructor that defaults to UTC.
        ConverterOptions options = new ConverterOptions(); // assumes UTC defaults
        this.converter = new Converter(options);
    }

    /* -------------------------------------------------------------
     *  toBigDecimal
     * ------------------------------------------------------------- */
    @Test
    public void testToBigDecimal_Typical() {
        BigInteger source = new BigInteger("12345678901234567890");
        BigDecimal result = BigIntegerConversions.toBigDecimal(source, converter);
        assertEquals(new BigDecimal(source), result);
    }

    /* -------------------------------------------------------------
     *  toUUID
     * ------------------------------------------------------------- */
    @Test
    public void testToUUID_Typical() {
        // 128‑bit value 1 -> UUID 00000000-0000-0000-0000-000000000001
        BigInteger source = BigInteger.ONE;
        UUID uuid = BigIntegerConversions.toUUID(source, converter);
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000001"), uuid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_NegativeNumber() {
        BigInteger source = new BigInteger("-1");
        BigIntegerConversions.toUUID(source, converter);
    }

    /* -------------------------------------------------------------
     *  toInstant (used by many other methods)
     * ------------------------------------------------------------- */
    @Test
    public void testToInstant_EpochAndBeyond() {
        // 0 nanoseconds => Instant.EPOCH
        BigInteger zero = BigInteger.ZERO;
        Instant instantZero = BigIntegerConversions.toInstant(zero, converter);
        assertEquals(Instant.EPOCH, instantZero);

        // 1_500_000_000 nanoseconds => 1.5 seconds
        BigInteger oneAndHalfSec = BigInteger.valueOf(1_500_000_000L);
        Instant instant = BigIntegerConversions.toInstant(oneAndHalfSec, converter);
        assertEquals(Instant.ofEpochSecond(1, 500_000_000L), instant);
    }

    /* -------------------------------------------------------------
     *  toDate
     * ------------------------------------------------------------- */
    @Test
    public void testToDate_Typical() {
        // 2 seconds = 2_000_000_000 nanoseconds
        BigInteger source = BigInteger.valueOf(2_000_000_000L);
        Date date = BigIntegerConversions.toDate(source, converter);
        assertEquals(Date.from(Instant.ofEpochSecond(2)), date);
    }

    /* -------------------------------------------------------------
     *  toSqlDate
     * ------------------------------------------------------------- */
    @Test
    public void testToSqlDate_Typical() {
        // 2021‑03‑15 00:00:00 UTC => compute epoch milliseconds then to nanoseconds
        LocalDate localDate = LocalDate.of(2021, 3, 15);
        long epochMilli = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        BigInteger nanos = BigInteger.valueOf(epochMilli).multiply(BigIntegerConversions.MILLION);

        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(nanos, converter);
        assertEquals(java.sql.Date.valueOf(localDate), sqlDate);
    }

    /* -------------------------------------------------------------
     *  toTimestamp
     * ------------------------------------------------------------- */
    @Test
    public void testToTimestamp_Typical() {
        // 1234 seconds + 567_890_123 nanoseconds
        BigInteger source = BigInteger.valueOf(1_234_567_890_123L);
        Timestamp ts = BigIntegerConversions.toTimestamp(source, converter);
        Instant expected = Instant.ofEpochSecond(1_234, 567_890_123L);
        assertEquals(Timestamp.from(expected), ts);
    }

    /* -------------------------------------------------------------
     *  toCalendar
     * ------------------------------------------------------------- */
    @Test
    public void testToCalendar_Typical() {
        // 5 minutes after epoch => 300_000_000_000 nanoseconds
        BigInteger source = BigInteger.valueOf(300_000_000_000L);
        Calendar cal = BigIntegerConversions.toCalendar(source, converter);
        assertEquals(0, cal.getTimeZone().getRawOffset()); // UTC
        assertEquals(300_000L, cal.getTimeInMillis()); // 5 minutes = 300_000 ms
    }

    /* -------------------------------------------------------------
     *  toZonedDateTime
     * ------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime_Typical() {
        // 2020‑01‑01T12:34:56.789Z
        Instant instant = Instant.parse("2020-01-01T12:34:56.789Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        ZonedDateTime zdt = BigIntegerConversions.toZonedDateTime(nanos, converter);
        assertEquals(ZonedDateTime.ofInstant(instant, converter.getOptions().getZoneId()), zdt);
    }

    /* -------------------------------------------------------------
     *  toLocalTime
     * ------------------------------------------------------------- */
    @Test
    public void testToLocalTime_Typical() {
        // 13:45:30.123456789 => nanoseconds since start of day
        long nanos = Duration.ofHours(13).plusMinutes(45).plusSeconds(30).toNanos() + 123_456_789L;
        BigInteger source = BigInteger.valueOf(nanos);
        LocalTime lt = BigIntegerConversions.toLocalTime(source, converter);
        assertEquals(LocalTime.of(13, 45, 30, 123_456_789), lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_Negative() {
        BigInteger source = BigInteger.valueOf(-1);
        BigIntegerConversions.toLocalTime(source, converter);
    }

    @Test
    public void testToLocalTime_Maximum() {
        // 23:59:59.999999999 = 86_399_999_999_999 nanoseconds
        long maxNanos = 86_399_999_999_999L;
        LocalTime lt = BigIntegerConversions.toLocalTime(BigInteger.valueOf(maxNanos), converter);
        assertEquals(LocalTime.MAX, lt);
    }

    /* -------------------------------------------------------------
     *  toLocalDate
     * ------------------------------------------------------------- */
    @Test
    public void testToLocalDate_Typical() {
        // 1970‑01‑02 (epoch + 1 day)
        Instant instant = Instant.EPOCH.plus(Duration.ofDays(1));
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION);
        LocalDate ld = BigIntegerConversions.toLocalDate(nanos, converter);
        assertEquals(LocalDate.of(1970, 1, 2), ld);
    }

    /* -------------------------------------------------------------
     *  toLocalDateTime
     * ------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime_Typical() {
        Instant instant = Instant.parse("2022-12-31T23:59:59.999999999Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        LocalDateTime ldt = BigIntegerConversions.toLocalDateTime(nanos, converter);
        assertEquals(LocalDateTime.ofInstant(instant, converter.getOptions().getZoneId()), ldt);
    }

    /* -------------------------------------------------------------
     *  toOffsetTime
     * ------------------------------------------------------------- */
    @Test
    public void testToOffsetTime_Typical() {
        // 00:00:01.500000000 UTC
        BigInteger source = BigInteger.valueOf(1_500_000_000L);
        OffsetTime ot = BigIntegerConversions.toOffsetTime(source, converter);
        assertEquals(OffsetTime.ofInstant(Instant.ofEpochSecond(1, 500_000_000L),
                converter.getOptions().getZoneId()), ot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_Negative() {
        BigInteger source = BigInteger.valueOf(-10);
        BigIntegerConversions.toOffsetTime(source, converter);
    }

    /* -------------------------------------------------------------
     *  toOffsetDateTime
     * ------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime_Typical() {
        Instant instant = Instant.parse("2000-02-29T12:00:00Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION);
        OffsetDateTime odt = BigIntegerConversions.toOffsetDateTime(nanos, converter);
        assertEquals(OffsetDateTime.ofInstant(instant, converter.getOptions().getZoneId()), odt);
    }

    /* -------------------------------------------------------------
     *  toDuration
     * ------------------------------------------------------------- */
    @Test
    public void testToDuration_Typical() {
        // 2 days, 3 hours, 4 minutes, 5 seconds, 6 nanoseconds
        Duration expected = Duration.ofDays(2)
                .plusHours(3)
                .plusMinutes(4)
                .plusSeconds(5)
                .plusNanos(6);
        BigInteger nanos = BigInteger.valueOf(expected.getSeconds()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(expected.getNano()));
        Duration actual = BigIntegerConversions.toDuration(nanos, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDuration_Zero() {
        Duration d = BigIntegerConversions.toDuration(BigInteger.ZERO, converter);
        assertEquals(Duration.ZERO, d);
    }

    /* -------------------------------------------------------------
     *  toInstant (additional edge case)
     * ------------------------------------------------------------- */
    @Test
    public void testToInstant_MaxLongSeconds() {
        // Use Long.MAX_VALUE seconds, zero nanos
        BigInteger source = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigIntegerConversions.BILLION);
        Instant instant = BigIntegerConversions.toInstant(source, converter);
        assertEquals(Instant.ofEpochSecond(Long.MAX_VALUE), instant);
    }
}
