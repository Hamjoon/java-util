package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.*;

public class BigIntegerConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Assuming ConverterOptions has a public no‑arg constructor and reasonable defaults.
        ConverterOptions options = new ConverterOptions();
        // Use UTC to make tests deterministic.
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        options.setZoneId(ZoneId.of("UTC"));
        this.converter = new Converter(options);
    }

    /* -------------------------------------------------
     *  toBigDecimal
     * ------------------------------------------------- */
    @Test
    public void testToBigDecimalTypical() {
        BigInteger bi = new BigInteger("12345678901234567890");
        BigDecimal bd = BigIntegerConversions.toBigDecimal(bi, converter);
        assertEquals(new BigDecimal(bi), bd);
    }

    /* -------------------------------------------------
     *  toUUID
     * ------------------------------------------------- */
    @Test
    public void testToUUIDTypical() {
        // 0x00112233445566778899AABBCCDDEEFF => UUID 00112233-4455-6677-8899-aabbccddeeff
        BigInteger bi = new BigInteger("00112233445566778899AABBCCDDEEFF", 16);
        UUID uuid = BigIntegerConversions.toUUID(bi, converter);
        assertEquals(UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"), uuid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUIDNegativeNumber() {
        BigInteger bi = BigInteger.valueOf(-1);
        BigIntegerConversions.toUUID(bi, converter);
    }

    @Test
    public void testToUUIDZero() {
        UUID uuid = BigIntegerConversions.toUUID(BigInteger.ZERO, converter);
        // All zero UUID
        assertEquals(UUID.fromString("00000000-0000-0000-0000-000000000000"), uuid);
    }

    /* -------------------------------------------------
     *  toDate / toSqlDate / toTimestamp
     * ------------------------------------------------- */
    @Test
    public void testToDateAndSqlDateAndTimestamp() {
        // 1,600,000,000,123,456,789 nanoseconds = 1600000000 seconds + 123456789 nanos
        BigInteger nanos = BigInteger.valueOf(1_600_000_000L).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(123_456_789L));

        Instant instant = BigIntegerConversions.toInstant(nanos, converter);
        Date date = BigIntegerConversions.toDate(nanos, converter);
        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(nanos, converter);
        Timestamp ts = BigIntegerConversions.toTimestamp(nanos, converter);

        assertEquals(Date.from(instant), date);
        assertEquals(new java.sql.Date(instant.toEpochMilli()), sqlDate);
        assertEquals(Timestamp.from(instant), ts);
    }

    /* -------------------------------------------------
     *  toCalendar
     * ------------------------------------------------- */
    @Test
    public void testToCalendar() {
        // 5,000,000,000 nanoseconds = 5,000 milliseconds
        BigInteger nanos = BigInteger.valueOf(5_000_000_000L);
        Calendar cal = BigIntegerConversions.toCalendar(nanos, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(5_000L, cal.getTimeInMillis());
    }

    /* -------------------------------------------------
     *  toZonedDateTime
     * ------------------------------------------------- */
    @Test
    public void testToZonedDateTime() {
        // 2022-03-14T15:09:26.123456789Z  => epoch seconds + nanos
        Instant instant = Instant.parse("2022-03-14T15:09:26.123456789Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        ZonedDateTime zdt = BigIntegerConversions.toZonedDateTime(nanos, converter);
        assertEquals(instant.atZone(ZoneId.of("UTC")), zdt);
    }

    /* -------------------------------------------------
     *  toLocalTime
     * ------------------------------------------------- */
    @Test
    public void testToLocalTimeTypical() {
        // 12:34:56.789012345 = 4529678912345 nanoseconds of day
        long nanosOfDay = 12L * 60 * 60 * 1_000_000_000L
                + 34L * 60 * 1_000_000_000L
                + 56L * 1_000_000_000L
                + 789_012_345L;
        BigInteger bi = BigInteger.valueOf(nanosOfDay);
        LocalTime lt = BigIntegerConversions.toLocalTime(bi, converter);
        assertEquals(LocalTime.of(12, 34, 56, 789_012_345), lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeNegative() {
        BigInteger bi = BigInteger.valueOf(-1);
        BigIntegerConversions.toLocalTime(bi, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeTooLarge() {
        // One nanosecond beyond the max allowed (24h = 86_400_000_000_000 ns)
        BigInteger bi = BigInteger.valueOf(86_400_000_000_001L);
        BigIntegerConversions.toLocalTime(bi, converter);
    }

    /* -------------------------------------------------
     *  toLocalDate, toLocalDateTime, toOffsetDateTime
     * ------------------------------------------------- */
    @Test
    public void testDateTimeConversions() {
        // 2021-12-31T23:59:59.999999999Z
        Instant instant = Instant.parse("2021-12-31T23:59:59.999999999Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));

        LocalDate ld = BigIntegerConversions.toLocalDate(nanos, converter);
        LocalDateTime ldt = BigIntegerConversions.toLocalDateTime(nanos, converter);
        OffsetDateTime odt = BigIntegerConversions.toOffsetDateTime(nanos, converter);

        assertEquals(LocalDate.of(2021, 12, 31), ld);
        assertEquals(LocalDateTime.of(2021, 12, 31, 23, 59, 59, 999_999_999), ldt);
        assertEquals(OffsetDateTime.ofInstant(instant, ZoneId.of("UTC")), odt);
    }

    /* -------------------------------------------------
     *  toInstant
     * ------------------------------------------------- */
    @Test
    public void testToInstantTypical() {
        Instant expected = Instant.parse("2000-01-01T00:00:00.123456789Z");
        BigInteger nanos = BigInteger.valueOf(expected.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(expected.getNano()));
        Instant actual = BigIntegerConversions.toInstant(nanos, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstantZero() {
        Instant instant = BigIntegerConversions.toInstant(BigInteger.ZERO, converter);
        assertEquals(Instant.EPOCH, instant);
    }

    /* -------------------------------------------------
     *  toDuration
     * ------------------------------------------------- */
    @Test
    public void testToDurationTypical() {
        Duration expected = Duration.ofSeconds(12345, 678_901_234);
        BigInteger nanos = BigInteger.valueOf(expected.getSeconds()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(expected.getNano()));
        Duration actual = BigIntegerConversions.toDuration(nanos, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDurationZero() {
        Duration duration = BigIntegerConversions.toDuration(BigInteger.ZERO, converter);
        assertEquals(Duration.ZERO, duration);
    }
}
