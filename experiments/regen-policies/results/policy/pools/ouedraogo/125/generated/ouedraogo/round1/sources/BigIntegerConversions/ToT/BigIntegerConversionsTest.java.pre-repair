package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;

public class BigIntegerConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The real library provides a default constructor for ConverterOptions.
        // We rely on the defaults (system time‑zone and zone‑id) which are sufficient
        // for the deterministic tests below.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     *  toBigDecimal
     * -------------------------------------------------------------- */
    @Test
    public void testToBigDecimalTypical() {
        BigInteger bi = new BigInteger("12345678901234567890");
        BigDecimal expected = new BigDecimal(bi);
        assertEquals(expected, BigIntegerConversions.toBigDecimal(bi, converter));
    }

    @Test
    public void testToBigDecimalZeroAndNegative() {
        assertEquals(BigDecimal.ZERO, BigIntegerConversions.toBigDecimal(BigInteger.ZERO, converter));

        BigInteger neg = new BigInteger("-42");
        assertEquals(new BigDecimal(neg), BigIntegerConversions.toBigDecimal(neg, converter));
    }

    /* --------------------------------------------------------------
     *  toUUID
     * -------------------------------------------------------------- */
    @Test
    public void testToUuidFromZero() {
        UUID expected = UUID.fromString("00000000-0000-0000-0000-000000000000");
        assertEquals(expected, BigIntegerConversions.toUUID(BigInteger.ZERO, converter));
    }

    @Test
    public void testToUuidFromOne() {
        UUID expected = UUID.fromString("00000000-0000-0000-0000-000000000001");
        assertEquals(expected, BigIntegerConversions.toUUID(BigInteger.ONE, converter));
    }

    @Test
    public void testToUuidFromLargeNumber() {
        // 0x1234567890abcdef1234567890abcdef (128‑bit value)
        BigInteger bi = new BigInteger("1234567890abcdef1234567890abcdef", 16);
        UUID expected = UUID.fromString("12345678-90ab-cdef-1234-567890abcdef");
        assertEquals(expected, BigIntegerConversions.toUUID(bi, converter));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUuidFromNegativeThrows() {
        BigInteger negative = new BigInteger("-1");
        BigIntegerConversions.toUUID(negative, converter);
    }

    /* --------------------------------------------------------------
     *  toInstant
     * -------------------------------------------------------------- */
    @Test
    public void testToInstantExactSecond() {
        // 5 seconds = 5_000_000_000 nanoseconds
        BigInteger nanos = BigInteger.valueOf(5L).multiply(BigIntegerConversions.BILLION);
        Instant expected = Instant.ofEpochSecond(5L, 0);
        assertEquals(expected, BigIntegerConversions.toInstant(nanos, converter));
    }

    @Test
    public void testToInstantWithNanosRemainder() {
        // 2 seconds + 123,456,789 nanoseconds
        BigInteger nanos = BigInteger.valueOf(2L).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(123_456_789L));
        Instant expected = Instant.ofEpochSecond(2L, 123_456_789L);
        assertEquals(expected, BigIntegerConversions.toInstant(nanos, converter));
    }

    /* --------------------------------------------------------------
     *  toDuration
     * -------------------------------------------------------------- */
    @Test
    public void testToDurationSameAsInstant() {
        BigInteger nanos = BigInteger.valueOf(10L).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(999_999_999L));
        Duration expected = Duration.ofSeconds(10L, 999_999_999L);
        assertEquals(expected, BigIntegerConversions.toDuration(nanos, converter));
    }

    /* --------------------------------------------------------------
     *  toDate, toSqlDate, toTimestamp
     * -------------------------------------------------------------- */
    @Test
    public void testToDateAndRelatedTypes() {
        // 1.5 seconds = 1_500_000_000 nanoseconds -> 1500 milliseconds
        BigInteger nanos = BigInteger.valueOf(1_500_000_000L);
        Instant instant = Instant.ofEpochMilli(1500L);
        Date expectedDate = Date.from(instant);
        java.sql.Date expectedSqlDate = new java.sql.Date(1500L);
        Timestamp expectedTimestamp = Timestamp.from(instant);

        assertEquals(expectedDate, BigIntegerConversions.toDate(nanos, converter));
        assertEquals(expectedSqlDate, BigIntegerConversions.toSqlDate(nanos, converter));
        assertEquals(expectedTimestamp, BigIntegerConversions.toTimestamp(nanos, converter));
    }

    /* --------------------------------------------------------------
     *  toCalendar
     * -------------------------------------------------------------- */
    @Test
    public void testToCalendar() {
        // 3 seconds = 3_000_000_000 nanoseconds -> 3000 milliseconds
        BigInteger nanos = BigInteger.valueOf(3_000_000_000L);
        Calendar cal = BigIntegerConversions.toCalendar(nanos, converter);
        assertEquals(3000L, cal.getTimeInMillis());
        // verify the calendar uses the converter's timezone (system default)
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone());
    }

    /* --------------------------------------------------------------
     *  toZonedDateTime
     * -------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime() {
        // 0 nanoseconds -> epoch start
        BigInteger nanos = BigInteger.ZERO;
        ZonedDateTime expected = Instant.EPOCH.atZone(converter.getOptions().getZoneId());
        assertEquals(expected, BigIntegerConversions.toZonedDateTime(nanos, converter));
    }

    /* --------------------------------------------------------------
     *  toLocalTime
     * -------------------------------------------------------------- */
    @Test
    public void testToLocalTimeTypical() {
        // 12:34:56.789123456 = (12*3600 + 34*60 + 56) seconds + 789_123_456 nanos
        long seconds = 12 * 3600 + 34 * 60 + 56;
        long nanos = seconds * 1_000_000_000L + 789_123_456L;
        BigInteger bi = BigInteger.valueOf(nanos);
        LocalTime expected = LocalTime.of(12, 34, 56, 789_123_456);
        assertEquals(expected, BigIntegerConversions.toLocalTime(bi, converter));
    }

    @Test
    public void testToLocalTimeBounds() {
        // minimum (0) -> 00:00
        assertEquals(LocalTime.MIDNIGHT, BigIntegerConversions.toLocalTime(BigInteger.ZERO, converter));

        // maximum nanoseconds of a day = 86_399_999_999_999
        BigInteger maxNanos = BigInteger.valueOf(86_399_999_999_999L);
        LocalTime expected = LocalTime.of(23, 59, 59, 999_999_999);
        assertEquals(expected, BigIntegerConversions.toLocalTime(maxNanos, converter));
    }

    @Test
    public void testToLocalTimeNegativeThrows() {
        try {
            BigIntegerConversions.toLocalTime(BigInteger.valueOf(-1L), converter);
            fail("Expected IllegalArgumentException for negative nanoseconds");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0"));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeOverflowThrows() {
        // 24 hours in nanoseconds (one nanosecond too many)
        BigInteger overflow = BigInteger.valueOf(86_400_000_000_000L);
        BigIntegerConversions.toLocalTime(overflow, converter);
    }

    /* --------------------------------------------------------------
     *  toLocalDate, toLocalDateTime, toOffsetDateTime
     * -------------------------------------------------------------- */
    @Test
    public void testDateTimeConversionsConsistency() {
        // 2022‑03‑14T15:09:26.123456789Z  -> nanoseconds since epoch
        Instant instant = Instant.parse("2022-03-14T15:09:26.123456789Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));

        ZonedDateTime expectedZoned = instant.atZone(converter.getOptions().getZoneId());

        assertEquals(expectedZoned.toLocalDate(),
                BigIntegerConversions.toLocalDate(nanos, converter));

        assertEquals(expectedZoned.toLocalDateTime(),
                BigIntegerConversions.toLocalDateTime(nanos, converter));

        assertEquals(expectedZoned.toOffsetDateTime(),
                BigIntegerConversions.toOffsetDateTime(nanos, converter));
    }
}
