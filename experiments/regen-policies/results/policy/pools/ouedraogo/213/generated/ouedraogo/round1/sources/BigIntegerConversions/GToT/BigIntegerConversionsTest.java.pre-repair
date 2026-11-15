package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@code BigIntegerConversions}.
 *
 * The tests cover:
 *   – normal (happy‑path) conversions,
 *   – edge‑case values (zero, negative, very large numbers),
 *   – and expected exception scenarios.
 *
 * The class under test has package‑private static methods.  Because this test class
 * resides in the same package (com.cedarsoftware.util.convert) it can invoke them
 * directly.
 */
public class BigIntegerConversionsTest {

    private Converter converter;

    /**
     * Creates a {@code Converter} with default options.  The real library supplies a
     * {@code ConverterOptions} class with a public no‑arg constructor that sets the
     * system default ZoneId and TimeZone.  Those defaults are sufficient for the
     * conversion methods under test.
     */
    @Before
    public void setUp() {
        // The library provides a default ConverterOptions constructor.
        ConverterOptions options = new ConverterOptions(); // assumed to exist
        this.converter = new Converter(options);
    }

    /* ---------------------------------------------------------------------- */
    /* 1. toBigDecimal                                                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToBigDecimal_HappyPath() {
        BigInteger bi = new BigInteger("12345678901234567890");
        BigDecimal result = BigIntegerConversions.toBigDecimal(bi, converter);
        assertEquals(new BigDecimal(bi), result);
    }

    /* ---------------------------------------------------------------------- */
    /* 2. toUUID                                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToUUID_HappyPath() {
        // 0x112233445566778899aabbccddeeff00 => UUID "11223344-5566-7788-99aa-bbccddeeff00"
        BigInteger bi = new BigInteger("112233445566778899aabbccddeeff00", 16);
        UUID uuid = BigIntegerConversions.toUUID(bi, converter);
        assertEquals(UUID.fromString("11223344-5566-7788-99aa-bbccddeeff00"), uuid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_NegativeNumber() {
        BigInteger negative = BigInteger.valueOf(-1);
        BigIntegerConversions.toUUID(negative, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 3. toDate                                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToDate_PositiveNanos() {
        // 1_500_000_000 nanoseconds = 1.5 seconds after epoch
        BigInteger nanos = BigInteger.valueOf(1_500_000_000L);
        Date date = BigIntegerConversions.toDate(nanos, converter);
        assertEquals(1500L, date.getTime()); // milliseconds
    }

    @Test
    public void testToDate_NegativeNanos() {
        // -2_000_000_000 nanoseconds = -2 seconds
        BigInteger nanos = BigInteger.valueOf(-2_000_000_000L);
        Date date = BigIntegerConversions.toDate(nanos, converter);
        assertEquals(-2000L, date.getTime());
    }

    /* ---------------------------------------------------------------------- */
    /* 4. toSqlDate                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToSqlDate_KnownDate() {
        // 2023‑01‑01 00:00:00 UTC => epoch seconds = 1672531200
        long epochSeconds = 1672531200L;
        BigInteger nanos = BigInteger.valueOf(epochSeconds).multiply(BigIntegerConversions.BILLION);
        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(nanos, converter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2023, 1, 1)), sqlDate);
    }

    @Test
    public void testToSqlDate_NonMidnight() {
        // 2023‑01‑01 12:34:56.789 UTC
        Instant instant = Instant.parse("2023-01-01T12:34:56.789Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(nanos, converter);
        // sqlDate should still represent the calendar date part only.
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2023, 1, 1)), sqlDate);
    }

    /* ---------------------------------------------------------------------- */
    /* 5. toTimestamp                                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToTimestamp_Positive() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        BigInteger nanos = BigInteger.valueOf(now.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(now.getNano()));
        Timestamp ts = BigIntegerConversions.toTimestamp(nanos, converter);
        assertEquals(Timestamp.from(now), ts);
    }

    @Test
    public void testToTimestamp_Negative() {
        Instant past = Instant.ofEpochSecond(-10, -500_000_000L); // -10.5 seconds
        BigInteger nanos = BigInteger.valueOf(past.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(past.getNano()));
        Timestamp ts = BigIntegerConversions.toTimestamp(nanos, converter);
        assertEquals(Timestamp.from(past), ts);
    }

    /* ---------------------------------------------------------------------- */
    /* 6. toCalendar                                                           */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToCalendar_Basic() {
        // 2022‑12‑31 23:59:59.123 UTC
        Instant instant = Instant.parse("2022-12-31T23:59:59.123Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        Calendar cal = BigIntegerConversions.toCalendar(nanos, converter);
        assertEquals(2022, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
        // Milliseconds part
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testToCalendar_VeryLargeValue() {
        // Use a value larger than Long.MAX_VALUE after division to ensure no overflow.
        BigInteger huge = BigInteger.ONE.shiftLeft(100); // 2^100 nanoseconds
        Calendar cal = BigIntegerConversions.toCalendar(huge, converter);
        // The exact calendar fields are not critical; we just verify no exception and
        // that the resulting time is consistent with the computed epoch millis.
        long expectedMillis = huge.divide(BigIntegerConversions.MILLION).longValue();
        assertEquals(expectedMillis, cal.getTimeInMillis());
    }

    /* ---------------------------------------------------------------------- */
    /* 7. toZonedDateTime                                                      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime_DefaultZone() {
        Instant instant = Instant.parse("2021-05-04T10:15:30Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        ZonedDateTime zdt = BigIntegerConversions.toZonedDateTime(nanos, converter);
        assertEquals(instant, zdt.toInstant());
        assertEquals(ZoneId.systemDefault(), zdt.getZone());
    }

    /* ---------------------------------------------------------------------- */
    /* 8. toLocalTime                                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLocalTime_HappyPath() {
        // 12:34:56.789012345 => nanoseconds of day = 45296789012345
        long nanosOfDay = 12L * 3_600_000_000_000L   // hours
                + 34L * 60_000_000_000L             // minutes
                + 56L * 1_000_000_000L              // seconds
                + 789_012_345L;                     // nanos
        BigInteger big = BigInteger.valueOf(nanosOfDay);
        LocalTime lt = BigIntegerConversions.toLocalTime(big, converter);
        assertEquals(LocalTime.of(12, 34, 56, 789_012_345), lt);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_Negative() {
        BigInteger negative = BigInteger.valueOf(-1);
        BigIntegerConversions.toLocalTime(negative, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_TooLarge() {
        // 24:00:00.000 (one nanosecond beyond the day)
        BigInteger tooBig = BigInteger.valueOf(86_400_000_000_000L);
        BigIntegerConversions.toLocalTime(tooBig, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 9. toLocalDate                                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLocalDate_HappyPath() {
        Instant instant = Instant.parse("2000-02-29T00:00:00Z"); // leap day
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION);
        LocalDate ld = BigIntegerConversions.toLocalDate(nanos, converter);
        assertEquals(LocalDate.of(2000, 2, 29), ld);
    }

    /* ---------------------------------------------------------------------- */
    /* 10. toLocalDateTime                                                     */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime_HappyPath() {
        Instant instant = Instant.parse("1999-12-31T23:59:59.999999999Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        LocalDateTime ldt = BigIntegerConversions.toLocalDateTime(nanos, converter);
        assertEquals(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()), ldt);
    }

    /* ---------------------------------------------------------------------- */
    /* 11. toOffsetTime                                                        */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToOffsetTime_HappyPath() {
        // 08:15:30.123456789 (nanoseconds of day)
        long nanosOfDay = 8L * 3_600_000_000_000L
                + 15L * 60_000_000_000L
                + 30L * 1_000_000_000L
                + 123_456_789L;
        BigInteger big = BigInteger.valueOf(nanosOfDay);
        OffsetTime ot = BigIntegerConversions.toOffsetTime(big, converter);
        Instant expectedInstant = Instant.ofEpochSecond(0, nanosOfDay);
        OffsetTime expected = OffsetTime.ofInstant(expectedInstant, ZoneId.systemDefault());
        assertEquals(expected, ot);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_Negative() {
        BigInteger negative = BigInteger.valueOf(-10);
        BigIntegerConversions.toOffsetTime(negative, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTime_TooLarge() {
        // One nanosecond past the end of the day
        BigInteger tooBig = BigInteger.valueOf(86_400_000_000_000L);
        BigIntegerConversions.toOffsetTime(tooBig, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 12. toOffsetDateTime                                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime_HappyPath() {
        Instant instant = Instant.parse("2020-07-15T13:45:00Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION);
        OffsetDateTime odt = BigIntegerConversions.toOffsetDateTime(nanos, converter);
        assertEquals(OffsetDateTime.ofInstant(instant, ZoneId.systemDefault()), odt);
    }

    /* ---------------------------------------------------------------------- */
    /* 13. toInstant                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToInstant_HappyPath() {
        Instant instant = Instant.parse("2010-01-01T00:00:01.123456789Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        Instant result = BigIntegerConversions.toInstant(nanos, converter);
        assertEquals(instant, result);
    }

    @Test
    public void testToInstant_Negative() {
        Instant instant = Instant.ofEpochSecond(-5, -250_000_000L); // -5.25 seconds
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        Instant result = BigIntegerConversions.toInstant(nanos, converter);
        assertEquals(instant, result);
    }

    /* ---------------------------------------------------------------------- */
    /* 14. toDuration                                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToDuration_HappyPath() {
        Duration duration = Duration.ofDays(2).plusHours(3).plusMinutes(4).plusSeconds(5).plusNanos(678_901_234);
        BigInteger nanos = BigInteger.valueOf(duration.getSeconds()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(duration.getNano()));
        Duration result = BigIntegerConversions.toDuration(nanos, converter);
        assertEquals(duration, result);
    }

    @Test
    public void testToDuration_Negative() {
        Duration duration = Duration.ofSeconds(-10, -500_000_000); // -10.5 seconds
        BigInteger nanos = BigInteger.valueOf(duration.getSeconds()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(duration.getNano()));
        Duration result = BigIntegerConversions.toDuration(nanos, converter);
        assertEquals(duration, result);
    }
}
---  
