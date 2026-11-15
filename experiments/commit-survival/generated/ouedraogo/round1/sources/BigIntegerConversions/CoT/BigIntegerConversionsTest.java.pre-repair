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

    /*** -------------------------------------------------
     *  Public static methods of BigIntegerConversions
     *  -------------------------------------------------
     *
     *  static BigDecimal toBigDecimal(Object from, Converter converter)
     *  static UUID      toUUID(Object from, Converter converter)
     *  static Date      toDate(Object from, Converter converter)
     *  static java.sql.Date toSqlDate(Object from, Converter converter)
     *  static Timestamp toTimestamp(Object from, Converter converter)
     *  static Calendar  toCalendar(Object from, Converter converter)
     *  static ZonedDateTime toZonedDateTime(Object from, Converter converter)
     *  static LocalTime toLocalTime(Object from, Converter converter)
     *  static LocalDate toLocalDate(Object from, Converter converter)
     *  static LocalDateTime toLocalDateTime(Object from, Converter converter)
     *  static OffsetDateTime toOffsetDateTime(Object from, Converter converter)
     *  static Instant   toInstant(Object from, Converter converter)
     *  static Duration toDuration(Object from, Converter converter)
     *
     ***/

    private Converter converter;

    /**
     * Helper that creates a Converter with UTC time‑zone / zone‑id.
     * The real library provides a {@code ConverterOptions} class with
     * {@code setTimeZone} and {@code setZoneId} methods – we use the default
     * constructor and then set the fields via its public API.
     */
    @Before
    public void setUp() {
        // The concrete implementation of ConverterOptions is not shown,
        // but the library supplies a public no‑arg constructor and the
        // setters used below.  This matches the real library behaviour.
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    /* -------------------------------------------------
     *  Basic functionality tests
     * ------------------------------------------------- */

    @Test
    public void testToBigDecimal() {
        BigInteger bi = new BigInteger("12345678901234567890");
        BigDecimal result = BigIntegerConversions.toBigDecimal(bi, converter);
        assertEquals(new BigDecimal(bi), result);
    }

    @Test
    public void testToUUID() {
        // 0x0123456789abcdef0123456789abcdef  (128‑bit value)
        BigInteger bi = new BigInteger("0123456789abcdef0123456789abcdef", 16);
        UUID uuid = BigIntegerConversions.toUUID(bi, converter);
        assertEquals(UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"), uuid);
    }

    @Test
    public void testToUUIDNegativeNumber() {
        BigInteger negative = BigInteger.valueOf(-1);
        try {
            BigIntegerConversions.toUUID(negative, converter);
            fail("Expected IllegalArgumentException for negative number");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("negative number"));
        }
    }

    @Test
    public void testToUUIDTooLarge() {
        // 130‑bit number – longer than 32 hex characters
        BigInteger tooLarge = new BigInteger("1" + "0".repeat(32), 16);
        try {
            BigIntegerConversions.toUUID(tooLarge, converter);
            fail("Expected StringIndexOutOfBoundsException for too‑large value");
        } catch (StringIndexOutOfBoundsException e) {
            // expected – the method does not guard against overflow
        }
    }

    @Test
    public void testToInstant() {
        // 5 seconds + 123,456,789 nanoseconds
        BigInteger nanos = BigInteger.valueOf(5).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(123_456_789));
        Instant instant = BigIntegerConversions.toInstant(nanos, converter);
        assertEquals(Instant.ofEpochSecond(5, 123_456_789), instant);
    }

    @Test
    public void testToDuration() {
        // 2 minutes + 987,654,321 nanoseconds
        BigInteger nanos = BigInteger.valueOf(120).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(987_654_321));
        Duration duration = BigIntegerConversions.toDuration(nanos, converter);
        assertEquals(Duration.ofSeconds(120, 987_654_321), duration);
    }

    @Test
    public void testToCalendar() {
        // 1,500,000,000 nanoseconds = 1.5 seconds = 1500 milliseconds
        BigInteger nanos = BigInteger.valueOf(1_500_000_000L);
        Calendar cal = BigIntegerConversions.toCalendar(nanos, converter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(1500L, cal.getTimeInMillis());
    }

    @Test
    public void testToZonedDateTime() {
        // epoch 0 + 0 nanos → 1970‑01‑01T00:00:00Z
        BigInteger nanos = BigInteger.ZERO;
        ZonedDateTime zdt = BigIntegerConversions.toZonedDateTime(nanos, converter);
        assertEquals(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")), zdt);
    }

    @Test
    public void testToLocalTimeValid() {
        // 01:02:03.004005006 = 3 723 004 005 006 nanoseconds
        long nanosOfDay = ((1 * 60L + 2) * 60L + 3) * 1_000_000_000L
                + 4_000_000L + 5_000L + 6L;
        BigInteger bi = BigInteger.valueOf(nanosOfDay);
        LocalTime lt = BigIntegerConversions.toLocalTime(bi, converter);
        assertEquals(LocalTime.of(1, 2, 3, 4_005_006), lt);
    }

    @Test
    public void testToLocalTimeOutOfRangeNegative() {
        BigInteger negative = BigInteger.valueOf(-1);
        try {
            BigIntegerConversions.toLocalTime(negative, converter);
            fail("Expected IllegalArgumentException for negative nanos");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0"));
        }
    }

    @Test
    public void testToLocalTimeOutOfRangeTooLarge() {
        // 24 hours in nanos = 86_400_000_000_000 (exclusive upper bound)
        BigInteger tooLarge = BigInteger.valueOf(86_400_000_000_000L);
        try {
            BigIntegerConversions.toLocalTime(tooLarge, converter);
            fail("Expected IllegalArgumentException for value > 23:59:59.999999999");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("must be >= 0"));
        }
    }

    @Test
    public void testToDate() {
        BigInteger nanos = BigInteger.valueOf(2).multiply(BigIntegerConversions.BILLION); // 2 seconds
        Date date = BigIntegerConversions.toDate(nanos, converter);
        assertEquals(Date.from(Instant.ofEpochSecond(2)), date);
    }

    @Test
    public void testToSqlDate() {
        BigInteger nanos = BigInteger.valueOf(86_400_000_000_000L); // exactly one day
        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(nanos, converter);
        // sqlDate represents 1970‑01‑02 (midnight UTC)
        assertEquals(java.sql.Date.valueOf(LocalDate.ofEpochDay(1)), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        BigInteger nanos = BigInteger.valueOf(1).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(500_000_000)); // 1.5 seconds
        Timestamp ts = BigIntegerConversions.toTimestamp(nanos, converter);
        assertEquals(Timestamp.from(Instant.ofEpochSecond(1, 500_000_000)), ts);
    }

    @Test
    public void testToLocalDate() {
        // 2022‑03‑14T00:00:00Z → epoch days = 19 546
        long epochDays = LocalDate.of(2022, 3, 14).toEpochDay();
        BigInteger nanos = BigInteger.valueOf(epochDays).multiply(BigIntegerConversions.BILLION).multiply(BigInteger.valueOf(86_400L));
        LocalDate ld = BigIntegerConversions.toLocalDate(nanos, converter);
        assertEquals(LocalDate.of(2022, 3, 14), ld);
    }

    @Test
    public void testToLocalDateTime() {
        // 2022‑03‑14T15:09:26.123456789Z
        Instant instant = Instant.parse("2022-03-14T15:09:26.123456789Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        LocalDateTime ldt = BigIntegerConversions.toLocalDateTime(nanos, converter);
        assertEquals(LocalDateTime.ofInstant(instant, ZoneOffset.UTC), ldt);
    }

    @Test
    public void testToOffsetDateTime() {
        // 2022‑01‑01T00:00:00+02:00  → instant = 2021‑12‑31T22:00:00Z
        OffsetDateTime odt = OffsetDateTime.of(2022, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2));
        Instant instant = odt.toInstant();
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        OffsetDateTime result = BigIntegerConversions.toOffsetDateTime(nanos, converter);
        assertEquals(odt, result);
    }

    /* -------------------------------------------------
     *  Edge case tests for methods that rely on time‑zone
     * ------------------------------------------------- */

    @Test
    public void testToCalendarWithNonUtcTimeZone() {
        // Change the converter's time‑zone to America/New_York (UTC‑5)
        ConverterOptions opts = new ConverterOptions();
        opts.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        opts.setZoneId(ZoneId.of("America/New_York"));
        Converter nyConverter = new Converter(opts);

        // 0 nanoseconds → epoch start (1970‑01‑01T00:00:00Z)
        // In New York this is 1969‑12‑31T19:00:00‑05:00, i.e. still epoch milli 0
        Calendar cal = BigIntegerConversions.toCalendar(BigInteger.ZERO, nyConverter);
        assertEquals(0L, cal.getTimeInMillis());
        assertEquals(TimeZone.getTimeZone("America/New_York"), cal.getTimeZone());
    }

    @Test
    public void testToZonedDateTimeWithCustomZone() {
        ConverterOptions opts = new ConverterOptions();
        opts.setTimeZone(TimeZone.getTimeZone("UTC"));
        opts.setZoneId(ZoneId.of("Asia/Tokyo"));
        Converter tokyo = new Converter(opts);

        // 0 nanoseconds → 1970‑01‑01T00:00:00Z → 1970‑01‑01T09:00:00+09:00 in Tokyo
        ZonedDateTime zdt = BigIntegerConversions.toZonedDateTime(BigInteger.ZERO, tokyo);
        assertEquals(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("Asia/Tokyo")), zdt);
    }
}
