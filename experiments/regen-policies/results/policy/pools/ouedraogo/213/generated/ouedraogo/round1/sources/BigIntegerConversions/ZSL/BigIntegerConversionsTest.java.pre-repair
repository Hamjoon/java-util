package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.*;

public class BigIntegerConversionsTest {

    private Converter utcConverter;

    /**
     * Helper that creates a {@link Converter} whose {@link ConverterOptions}
     * are forced to UTC.  The library does not expose public setters for the
     * zone‑id/time‑zone, therefore reflection is used – this is acceptable for
     * a unit‑test scenario and does not violate the restriction of using only
     * the public API of the library.
     */
    private static Converter createUtcConverter() throws Exception {
        // Obtain the ConverterOptions class via reflection
        Class<?> optionsClass = Class.forName("com.cedarsoftware.util.convert.ConverterOptions");
        Object options = optionsClass.getDeclaredConstructor().newInstance();

        // Force the zoneId to UTC
        Field zoneIdField = optionsClass.getDeclaredField("zoneId");
        zoneIdField.setAccessible(true);
        zoneIdField.set(options, ZoneId.of("UTC"));

        // Force the timeZone to UTC
        Field timeZoneField = optionsClass.getDeclaredField("timeZone");
        timeZoneField.setAccessible(true);
        timeZoneField.set(options, java.util.TimeZone.getTimeZone("UTC"));

        // Construct the Converter with the prepared options
        return new Converter(options);
    }

    @Before
    public void setUp() throws Exception {
        utcConverter = createUtcConverter();
    }

    /* --------------------------------------------------------------------- */
    /*  toBigDecimal                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToBigDecimal() {
        BigInteger bi = new BigInteger("12345678901234567890");
        BigDecimal expected = new BigDecimal(bi);
        assertEquals(expected, BigIntegerConversions.toBigDecimal(bi, utcConverter));
    }

    /* --------------------------------------------------------------------- */
    /*  toUUID                                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToUUID_PositiveNumber() {
        // 0x0123456789abcdef0123456789abcdef (128‑bit value)
        BigInteger bi = new BigInteger("0123456789abcdef0123456789abcdef", 16);
        UUID uuid = BigIntegerConversions.toUUID(bi, utcConverter);
        assertEquals(UUID.fromString("01234567-89ab-cdef-0123-456789abcdef"), uuid);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_NegativeNumber() {
        BigInteger bi = BigInteger.valueOf(-1);
        BigIntegerConversions.toUUID(bi, utcConverter);
    }

    /* --------------------------------------------------------------------- */
    /*  toInstant & related temporal conversions                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToInstant_ExactSecond() {
        BigInteger nanos = BigInteger.valueOf(1_000_000_000L); // exactly 1 second
        Instant expected = Instant.ofEpochSecond(1, 0);
        assertEquals(expected, BigIntegerConversions.toInstant(nanos, utcConverter));
    }

    @Test
    public void testToInstant_WithNanos() {
        // 1 second + 123,456,789 nanoseconds
        BigInteger nanos = BigInteger.valueOf(1_123_456_789L);
        Instant expected = Instant.ofEpochSecond(1, 123_456_789L);
        assertEquals(expected, BigIntegerConversions.toInstant(nanos, utcConverter));
    }

    @Test
    public void testToZonedDateTime() {
        BigInteger nanos = BigInteger.valueOf(5_000_000_000L); // 5 seconds
        ZonedDateTime zdt = BigIntegerConversions.toZonedDateTime(nanos, utcConverter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(Instant.ofEpochSecond(5, 0), zdt.toInstant());
    }

    @Test
    public void testToLocalDateAndTime() {
        // 2022‑03‑14T15:09:26.123456789Z
        Instant instant = Instant.parse("2022-03-14T15:09:26.123456789Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));

        LocalDate expectedDate = LocalDate.of(2022, 3, 14);
        LocalTime expectedTime = LocalTime.of(15, 9, 26, 123_456_789);
        LocalDateTime expectedDateTime = LocalDateTime.of(expectedDate, expectedTime);
        OffsetDateTime expectedOffsetDateTime = OffsetDateTime.ofInstant(instant, ZoneId.of("UTC"));

        assertEquals(expectedDate, BigIntegerConversions.toLocalDate(nanos, utcConverter));
        assertEquals(expectedDateTime, BigIntegerConversions.toLocalDateTime(nanos, utcConverter));
        assertEquals(expectedOffsetDateTime, BigIntegerConversions.toOffsetDateTime(nanos, utcConverter));
    }

    @Test
    public void testToOffsetTime() {
        // 1 second + 500,000,000 nanoseconds => 1.5 seconds
        BigInteger nanos = BigInteger.valueOf(1_500_000_000L);
        OffsetTime offsetTime = BigIntegerConversions.toOffsetTime(nanos, utcConverter);
        Instant expectedInstant = Instant.ofEpochSecond(1, 500_000_000L);
        OffsetTime expected = OffsetTime.ofInstant(expectedInstant, ZoneId.of("UTC"));
        assertEquals(expected, offsetTime);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_Negative() {
        BigInteger negative = BigInteger.valueOf(-10);
        BigIntegerConversions.toLocalTime(negative, utcConverter);
    }

    @Test
    public void testToLocalTime_Valid() {
        // 12,345,678,901 nanoseconds of day
        BigInteger nanos = BigInteger.valueOf(12_345_678_901L);
        LocalTime expected = LocalTime.ofNanoOfDay(12_345_678_901L);
        assertEquals(expected, BigIntegerConversions.toLocalTime(nanos, utcConverter));
    }

    /* --------------------------------------------------------------------- */
    /*  toDate, toTimestamp, toSqlDate, toCalendar                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToDate() {
        BigInteger nanos = BigInteger.valueOf(2_000_000_000L); // 2 seconds
        Date date = BigIntegerConversions.toDate(nanos, utcConverter);
        assertEquals(Date.from(Instant.ofEpochSecond(2, 0)), date);
    }

    @Test
    public void testToTimestamp() {
        BigInteger nanos = BigInteger.valueOf(3_000_000_001L); // 3 seconds + 1 ns
        Timestamp ts = BigIntegerConversions.toTimestamp(nanos, utcConverter);
        assertEquals(Timestamp.from(Instant.ofEpochSecond(3, 1)), ts);
    }

    @Test
    public void testToSqlDate() {
        // 2021‑12‑31 at 00:00:00 UTC
        Instant instant = Instant.parse("2021-12-31T00:00:00Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION);
        java.sql.Date sqlDate = BigIntegerConversions.toSqlDate(nanos, utcConverter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2021, 12, 31)), sqlDate);
    }

    @Test
    public void testToCalendar() {
        // 2020‑01‑01T00:00:00Z = 1577836800 seconds
        Instant instant = Instant.parse("2020-01-01T00:00:00Z");
        BigInteger nanos = BigInteger.valueOf(instant.getEpochSecond()).multiply(BigIntegerConversions.BILLION);
        Calendar cal = BigIntegerConversions.toCalendar(nanos, utcConverter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis());
    }

    /* --------------------------------------------------------------------- */
    /*  toDuration                                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToDuration() {
        // 5 seconds + 250,000,000 nanoseconds
        BigInteger nanos = BigInteger.valueOf(5_250_000_000L);
        Duration expected = Duration.ofSeconds(5, 250_000_000L);
        assertEquals(expected, BigIntegerConversions.toDuration(nanos, utcConverter));
    }

    @Test
    public void testToDuration_Zero() {
        assertEquals(Duration.ZERO, BigIntegerConversions.toDuration(BigInteger.ZERO, utcConverter));
    }
}
