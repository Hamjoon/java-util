package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit 4 test suite that exercises every public static method in
 * {@link BigIntegerConversions}.  The tests use real {@link Converter}
 * and {@link ConverterOptions} instances (no mocks) and verify both the
 * “happy path” and the expected failure cases.
 */
public class BigIntegerConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Build a Converter with deterministic zone / time‑zone settings.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        converter = new Converter(options);
    }

    // -----------------------------------------------------------------
    // toBigDecimal
    // -----------------------------------------------------------------
    @Test
    public void testToBigDecimal() {
        BigInteger bi = new BigInteger("12345678901234567890");
        BigDecimal result = BigIntegerConversions.toBigDecimal(bi, converter);
        assertEquals(new BigDecimal(bi), result);
    }

    // -----------------------------------------------------------------
    // toUUID
    // -----------------------------------------------------------------
    @Test
    public void testToUUID_Zero() {
        UUID uuid = BigIntegerConversions.toUUID(BigInteger.ZERO, converter);
        // UUID of all zeros
        assertEquals(new UUID(0L, 0L), uuid);
    }

    @Test
    public void testToUUID_PositiveValue() {
        // 0x00112233445566778899AABBCCDDEEFF (128‑bit)
        BigInteger bi = new BigInteger("00112233445566778899AABBCCDDEEFF", 16);
        UUID uuid = BigIntegerConversions.toUUID(bi, converter);
        assertEquals(UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"), uuid);
    }

    @Test
    public void testToUUID_NegativeThrows() {
        try {
            BigIntegerConversions.toUUID(BigInteger.valueOf(-1), converter);
            fail("Expected IllegalArgumentException for negative BigInteger");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Cannot convert a negative number"));
        }
    }

    // -----------------------------------------------------------------
    // toDate / toTimestamp
    // -----------------------------------------------------------------
    @Test
    public void testToDateAndTimestamp() {
        // 5 seconds = 5_000_000_000 nanoseconds
        BigInteger nanos = BigInteger.valueOf(5_000_000_000L);
        Date date = BigIntegerConversions.toDate(nanos, converter);
        Timestamp ts = BigIntegerConversions.toTimestamp(nanos, converter);

        Instant expected = Instant.ofEpochSecond(5);
        assertEquals(Date.from(expected), date);
        assertEquals(Timestamp.from(expected), ts);
    }

    // -----------------------------------------------------------------
    // toSqlDate
    // -----------------------------------------------------------------
    @Test
    public void testToSqlDate() {
        // 2020‑01‑02 00:00:00 UTC -> epoch milli = 1577923200000
        // Convert to nanoseconds for the method.
        long epochMilli = 1577923200000L;
        BigInteger nanos = BigInteger.valueOf(epochMilli).multiply(BigIntegerConversions.MILLION);
        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(nanos, converter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2020, 1, 2)), sqlDate);
    }

    // -----------------------------------------------------------------
    // toCalendar
    // -----------------------------------------------------------------
    @Test
    public void testToCalendar() {
        // 2021‑12‑31 23:59:59.123 UTC
        Instant instant = Instant.parse("2021-12-31T23:59:59.123Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        Calendar cal = BigIntegerConversions.toCalendar(nanos, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis());
    }

    // -----------------------------------------------------------------
    // toZonedDateTime / toLocalDate / toLocalDateTime
    // -----------------------------------------------------------------
    @Test
    public void testZonedAndLocalDateTime() {
        Instant instant = Instant.parse("1999-12-31T23:59:59.999999999Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));

        ZonedDateTime zdt = BigIntegerConversions.toZonedDateTime(nanos, converter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(instant, zdt.toInstant());

        LocalDate ld = BigIntegerConversions.toLocalDate(nanos, converter);
        assertEquals(LocalDate.of(1999, 12, 31), ld);

        LocalDateTime ldt = BigIntegerConversions.toLocalDateTime(nanos, converter);
        assertEquals(LocalDateTime.of(1999, 12, 31, 23, 59, 59, 999_999_999), ldt);
    }

    // -----------------------------------------------------------------
    // toLocalTime
    // -----------------------------------------------------------------
    @Test
    public void testToLocalTime_Valid() {
        // 12:34:56.789123456 = 45296789123456 nanoseconds
        long nanosOfDay = 12L * 3_600_000_000_000L   // hours
                        + 34L * 60_000_000_000L    // minutes
                        + 56L * 1_000_000_000L    // seconds
                        + 789_123_456L;           // nanos
        BigInteger bi = BigInteger.valueOf(nanosOfDay);
        LocalTime lt = BigIntegerConversions.toLocalTime(bi, converter);
        assertEquals(LocalTime.of(12, 34, 56, 789_123_456), lt);
    }

    @Test
    public void testToLocalTime_OutOfRange() {
        // One nanosecond beyond the maximum allowed (24 h – 1 ns)
        BigInteger tooBig = BigInteger.valueOf(86_400_000_000_000L);
        try {
            BigIntegerConversions.toLocalTime(tooBig, converter);
            fail("Expected IllegalArgumentException for out‑of‑range nanoseconds");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399999999999"));
        }
    }

    // -----------------------------------------------------------------
    // toOffsetTime
    // -----------------------------------------------------------------
    @Test
    public void testToOffsetTime_Valid() {
        // 01:02:03.004005006 UTC -> total seconds = 3723, nanos = 4_005_006
        long seconds = 1 * 3600 + 2 * 60 + 3;
        long nanos = 4_005_006L;
        BigInteger totalNanos = BigInteger.valueOf(seconds).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(nanos));
        OffsetTime ot = BigIntegerConversions.toOffsetTime(totalNanos, converter);
        assertEquals(OffsetTime.of(1, 2, 3, 4_005_006, ZoneOffset.UTC), ot);
    }

    @Test
    public void testToOffsetTime_NegativeThrows() {
        try {
            BigIntegerConversions.toOffsetTime(BigInteger.valueOf(-1), converter);
            fail("Expected IllegalArgumentException for negative nanoseconds");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0 && <= 86399999999999"));
        }
    }

    // -----------------------------------------------------------------
    // toOffsetDateTime
    // -----------------------------------------------------------------
    @Test
    public void testToOffsetDateTime() {
        Instant instant = Instant.parse("2022-03-04T05:06:07.080Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        OffsetDateTime odt = BigIntegerConversions.toOffsetDateTime(nanos, converter);
        assertEquals(OffsetDateTime.ofInstant(instant, ZoneId.of("UTC")), odt);
    }

    // -----------------------------------------------------------------
    // toInstant
    // -----------------------------------------------------------------
    @Test
    public void testToInstant() {
        Instant expected = Instant.parse("2000-01-01T00:00:00.123456789Z");
        BigInteger nanos = BigInteger.valueOf(expected.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(expected.getNano()));
        Instant actual = BigIntegerConversions.toInstant(nanos, converter);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    // toDuration
    // -----------------------------------------------------------------
    @Test
    public void testToDuration() {
        // 2 days, 3 hours, 4 minutes, 5 seconds, 6 milliseconds, 7 microseconds, 8 nanoseconds
        long totalNanos = Duration.ofDays(2).toNanos()
                + Duration.ofHours(3).toNanos()
                + Duration.ofMinutes(4).toNanos()
                + Duration.ofSeconds(5).toNanos()
                + Duration.ofMillis(6).toNanos()
                + Duration.ofNanos(7).toNanos()
                + 8L;
        BigInteger bi = BigInteger.valueOf(totalNanos);
        Duration d = BigIntegerConversions.toDuration(bi, converter);
        assertEquals(Duration.ofDays(2).plusHours(3).plusMinutes(4).plusSeconds(5)
                .plusMillis(6).plusNanos(7 + 8), d);
    }
}
