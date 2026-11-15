package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;

public class BigIntegerConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Use default options – they provide the system default zone and time‑zone.
        ConverterOptions options = new ConverterOptions();
        // Ensure the options are set to known values for deterministic tests.
        options.setZoneId(ZoneId.of("UTC"));
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.converter = new Converter(options);
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
    // toUUID – positive and negative cases
    // -----------------------------------------------------------------
    @Test
    public void testToUUIDPositive() {
        // 32‑hex‑digit value: 1234567890abcdef1234567890abcdef
        String hex = "1234567890abcdef1234567890abcdef";
        BigInteger bi = new BigInteger(hex, 16);
        UUID uuid = BigIntegerConversions.toUUID(bi, converter);
        assertEquals(UUID.fromString("12345678-90ab-cdef-1234-567890abcdef"), uuid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUIDNegative() {
        BigInteger negative = BigInteger.valueOf(-1);
        BigIntegerConversions.toUUID(negative, converter);
    }

    // -----------------------------------------------------------------
    // toInstant and related date‑time conversions
    // -----------------------------------------------------------------
    @Test
    public void testToInstantExactSecond() {
        // 1,000,000,000 nanoseconds = 1 second
        BigInteger nanos = BigInteger.valueOf(1_000_000_000L);
        Instant instant = BigIntegerConversions.toInstant(nanos, converter);
        assertEquals(Instant.ofEpochSecond(1), instant);
    }

    @Test
    public void testToDate() {
        BigInteger nanos = BigInteger.valueOf(2_500_000_000L); // 2.5 seconds
        Date date = BigIntegerConversions.toDate(nanos, converter);
        assertEquals(Date.from(Instant.ofEpochSecond(2, 500_000_000)), date);
    }

    @Test
    public void testToSqlDate() {
        BigInteger nanos = BigInteger.valueOf(86_400_000_000_000L); // exactly 1 day
        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(nanos, converter);
        // java.sql.Date truncates the time‑part, so we expect epoch + 1 day at midnight UTC
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(86_400_000L); // milliseconds in one day
        assertEquals(new java.sql.Date(cal.getTimeInMillis()), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        BigInteger nanos = BigInteger.valueOf(3_000_000_001L); // 3 seconds + 1 ns
        Timestamp ts = BigIntegerConversions.toTimestamp(nanos, converter);
        assertEquals(Timestamp.from(Instant.ofEpochSecond(3, 1)), ts);
    }

    @Test
    public void testToCalendar() {
        // 1,500,000,000 nanoseconds = 1500 milliseconds
        BigInteger nanos = BigInteger.valueOf(1_500_000_000L);
        Calendar cal = BigIntegerConversions.toCalendar(nanos, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(1500L, cal.getTimeInMillis());
    }

    @Test
    public void testToZonedDateTime() {
        // 5,000,000,000 nanoseconds = 5 seconds
        BigInteger nanos = BigInteger.valueOf(5_000_000_000L);
        ZonedDateTime zdt = BigIntegerConversions.toZonedDateTime(nanos, converter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(Instant.ofEpochSecond(5), zdt.toInstant());
    }

    // -----------------------------------------------------------------
    // LocalTime conversion – valid and invalid ranges
    // -----------------------------------------------------------------
    @Test
    public void testToLocalTimeMidnight() {
        BigInteger nanos = BigInteger.ZERO;
        LocalTime lt = BigIntegerConversions.toLocalTime(nanos, converter);
        assertEquals(LocalTime.MIDNIGHT, lt);
    }

    @Test
    public void testToLocalTimeNoon() {
        // 12 hours in nanoseconds
        long noonNanos = 12L * 60 * 60 * 1_000_000_000L;
        BigInteger nanos = BigInteger.valueOf(noonNanos);
        LocalTime lt = BigIntegerConversions.toLocalTime(nanos, converter);
        assertEquals(LocalTime.NOON, lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeNegative() {
        BigInteger negative = BigInteger.valueOf(-1);
        BigIntegerConversions.toLocalTime(negative, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeOverflow() {
        // 24 hours in nanoseconds (one nanosecond too many)
        BigInteger overflow = BigInteger.valueOf(24L * 60 * 60 * 1_000_000_000L);
        BigIntegerConversions.toLocalTime(overflow, converter);
    }

    // -----------------------------------------------------------------
    // LocalDate / LocalDateTime / OffsetDateTime derived from ZonedDateTime
    // -----------------------------------------------------------------
    @Test
    public void testToLocalDate() {
        // epoch (1970‑01‑01) plus 1 day = 1970‑01‑02
        BigInteger nanos = BigInteger.valueOf(86_400_000_000_000L);
        LocalDate ld = BigIntegerConversions.toLocalDate(nanos, converter);
        assertEquals(LocalDate.of(1970, 1, 2), ld);
    }

    @Test
    public void testToLocalDateTime() {
        // epoch + 1 hour + 30 minutes
        long nanos = (1L * 60 * 60 + 30 * 60) * 1_000_000_000L;
        BigInteger bi = BigInteger.valueOf(nanos);
        LocalDateTime ldt = BigIntegerConversions.toLocalDateTime(bi, converter);
        assertEquals(LocalDateTime.of(1970, 1, 1, 1, 30), ldt);
    }

    @Test
    public void testToOffsetDateTime() {
        // epoch + 2 seconds
        BigInteger nanos = BigInteger.valueOf(2_000_000_000L);
        OffsetDateTime odt = BigIntegerConversions.toOffsetDateTime(nanos, converter);
        assertEquals(OffsetDateTime.ofInstant(Instant.ofEpochSecond(2), ZoneOffset.UTC), odt);
    }

    // -----------------------------------------------------------------
    // Duration conversion
    // -----------------------------------------------------------------
    @Test
    public void testToDuration() {
        // 3 seconds + 250 ms = 3,250,000,000 nanoseconds
        BigInteger nanos = BigInteger.valueOf(3_250_000_000L);
        Duration dur = BigIntegerConversions.toDuration(nanos, converter);
        assertEquals(Duration.ofSeconds(3, 250_000_000), dur);
    }

    // -----------------------------------------------------------------
    // Additional edge cases for toInstant (large values)
    // -----------------------------------------------------------------
    @Test
    public void testToInstantLargeValue() {
        // 2^60 nanoseconds – well beyond typical ranges but still fits in BigInteger
        BigInteger nanos = BigInteger.ONE.shiftLeft(60);
        Instant instant = BigIntegerConversions.toInstant(nanos, converter);
        // Verify that the computed seconds and nanos match manual division
        BigInteger[] divRem = nanos.divideAndRemainder(BigIntegerConversions.BILLION);
        long expectedSeconds = divRem[0].longValue();
        int expectedNanos = divRem[1].intValue();
        assertEquals(Instant.ofEpochSecond(expectedSeconds, expectedNanos), instant);
    }
}
