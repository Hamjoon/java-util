package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.sql.Timestamp;
import java.sql.Date as SqlDate;
import java.util.UUID;

/**
 * Unit tests for {@link BigDecimalConversions}.
 * <p>
 * The tests exercise every static conversion method in the class.
 * They use a real {@link Converter} instance with default {@link ConverterOptions},
 * which supplies the system default {@link TimeZone} and {@link ZoneId}.
 * </p>
 */
public class BigDecimalConversionsTest {

    /** Helper to create a default Converter (uses system defaults). */
    private static Converter createDefaultConverter() {
        return new Converter(new ConverterOptions());
    }

    @Test
    public void testToCalendar() {
        Converter converter = createDefaultConverter();
        BigDecimal seconds = new BigDecimal("123.456");
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        // 123.456 seconds * 1000 = 123456 milliseconds
        Assert.assertEquals(123456L, cal.getTimeInMillis());
        // Verify the calendar uses the converter's time zone
        Assert.assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testToInstant() {
        Converter converter = createDefaultConverter();
        BigDecimal seconds = new BigDecimal("10.5"); // 10 seconds + 0.5 sec
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        Assert.assertEquals(10L, instant.getEpochSecond());
        Assert.assertEquals(500_000_000L, instant.getNano());
    }

    @Test
    public void testToDuration() {
        Converter converter = createDefaultConverter();
        BigDecimal seconds = new BigDecimal("20.123456789");
        Duration duration = BigDecimalConversions.toDuration(seconds, converter);
        Assert.assertEquals(20L, duration.getSeconds());
        Assert.assertEquals(123_456_789L, duration.getNano());
    }

    @Test
    public void testToLocalTime_Valid() {
        Converter converter = createDefaultConverter();
        // 1 hour, 2 minutes, 3 seconds and 456,789,012 nanoseconds
        BigDecimal seconds = new BigDecimal("3723.456789012");
        LocalTime time = BigDecimalConversions.toLocalTime(seconds, converter);
        Assert.assertEquals(LocalTime.of(1, 2, 3, 456_789_012), time);
    }

    @Test
    public void testToLocalTime_OutOfRange() {
        Converter converter = createDefaultConverter();
        // Exactly 24 hours (out of range, must be < 86400 seconds)
        BigDecimal seconds = new BigDecimal("86400");
        try {
            BigDecimalConversions.toLocalTime(seconds, converter);
            Assert.fail("Expected IllegalArgumentException for out‑of‑range value");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("must be >= 0 && <= 86399.999999999"));
        }
    }

    @Test
    public void testToZonedDateTime_andRelatedTypes() {
        Converter converter = createDefaultConverter();
        // Use epoch second 1609459200 = 2021‑01‑01T00:00:00Z
        BigDecimal seconds = new BigDecimal("1609459200");
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(seconds, converter);
        ZoneId zone = converter.getOptions().getZoneId();

        Assert.assertEquals(Instant.ofEpochSecond(1609459200L), zdt.toInstant());
        Assert.assertEquals(zone, zdt.getZone());

        LocalDate ld = BigDecimalConversions.toLocalDate(seconds, converter);
        Assert.assertEquals(LocalDate.ofInstant(Instant.ofEpochSecond(1609459200L), zone), ld);

        LocalDateTime ldt = BigDecimalConversions.toLocalDateTime(seconds, converter);
        Assert.assertEquals(LocalDateTime.ofInstant(Instant.ofEpochSecond(1609459200L), zone), ldt);

        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(seconds, converter);
        Assert.assertEquals(OffsetDateTime.ofInstant(Instant.ofEpochSecond(1609459200L), zone), odt);
    }

    @Test
    public void testToDate() {
        Converter converter = createDefaultConverter();
        BigDecimal seconds = new BigDecimal("0"); // epoch
        Date date = BigDecimalConversions.toDate(seconds, converter);
        Assert.assertEquals(Date.from(Instant.EPOCH), date);
    }

    @Test
    public void testToSqlDate() {
        Converter converter = createDefaultConverter();
        BigDecimal seconds = new BigDecimal("86400"); // one day after epoch
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(seconds, converter);
        // sqlDate stores the milliseconds since epoch, should be exactly one day
        Assert.assertEquals(86400L * 1000L, sqlDate.getTime());
    }

    @Test
    public void testToTimestamp() {
        Converter converter = createDefaultConverter();
        BigDecimal seconds = new BigDecimal("0.001"); // 1 millisecond after epoch
        Timestamp ts = BigDecimalConversions.toTimestamp(seconds, converter);
        Assert.assertEquals(1L, ts.getTime());
        Assert.assertEquals(0, ts.getNanos()); // nanos part is truncated to millis in Timestamp
    }

    @Test
    public void testToBigInteger() {
        Converter converter = createDefaultConverter();
        BigDecimal bd = new BigDecimal("12345.6789");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        Assert.assertEquals(BigInteger.valueOf(12345L), bi);
    }

    @Test
    public void testToString() {
        Converter converter = createDefaultConverter();
        BigDecimal bd = new BigDecimal("00123.4500");
        String s = BigDecimalConversions.toString(bd, converter);
        Assert.assertEquals("123.45", s);
    }

    @Test
    public void testToUUID() {
        Converter converter = createDefaultConverter();
        // Use a known 128‑bit value: 0x0123456789abcdef0123456789abcdef
        BigInteger bigInt = new BigInteger("0123456789abcdef0123456789abcdef", 16);
        BigDecimal bd = new BigDecimal(bigInt);
        UUID uuid = BigDecimalConversions.toUUID(bd, converter);
        // Verify that the UUID is correctly derived from the same BigInteger via
        // the helper conversion (the helper is part of the library, we trust it).
        Assert.assertEquals(uuid, BigIntegerConversions.toUUID(bigInt, converter));
    }

    @Test
    public void testSecondsAndNanosToDouble() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(5L, 250_000_000L);
        Assert.assertEquals(new BigDecimal("5.250000000"), result);
    }
}
