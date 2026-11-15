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

public class BigDecimalConversionsTest {

    private Converter utcConverter;

    @Before
    public void setUp() {
        // Build ConverterOptions with UTC zone and timezone.
        // The real library provides setters; we use them here.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(options);
    }

    @Test
    public void testToCalendar() {
        BigDecimal seconds = new BigDecimal("1.5"); // 1.5 seconds = 1500 ms
        Calendar cal = BigDecimalConversions.toCalendar(seconds, utcConverter);
        assertEquals(1500L, cal.getTimeInMillis());
        // Verify that the calendar is using the UTC timezone
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testToInstantWholeAndFractional() {
        BigDecimal seconds = new BigDecimal("10.123456789");
        Instant instant = BigDecimalConversions.toInstant(seconds, utcConverter);
        assertEquals(10L, instant.getEpochSecond());
        assertEquals(123456789L, instant.getNano());
    }

    @Test
    public void testToDuration() {
        BigDecimal seconds = new BigDecimal("5.5"); // 5 seconds + 0.5 sec = 500,000,000 ns
        Duration duration = BigDecimalConversions.toDuration(seconds, utcConverter);
        assertEquals(5L, duration.getSeconds());
        assertEquals(500_000_000L, duration.getNano());
    }

    @Test
    public void testToLocalTimeWithinDay() {
        // 12:34:56.789012345 = 45296.789012345 seconds
        BigDecimal seconds = new BigDecimal("45296.789012345");
        LocalTime time = BigDecimalConversions.toLocalTime(seconds, utcConverter);
        assertEquals(LocalTime.of(12, 34, 56, 789_012_345), time);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeOutOfRangeNegative() {
        BigDecimal seconds = new BigDecimal("-1");
        BigDecimalConversions.toLocalTime(seconds, utcConverter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTimeOutOfRangeTooLarge() {
        // One second beyond the last nanosecond of the day
        BigDecimal seconds = new BigDecimal("86400");
        BigDecimalConversions.toLocalTime(seconds, utcConverter);
    }

    @Test
    public void testToLocalDateAndLocalDateTime() {
        // Epoch second 0 => 1970-01-01T00:00:00Z
        BigDecimal seconds = BigDecimal.ZERO;
        LocalDate date = BigDecimalConversions.toLocalDate(seconds, utcConverter);
        LocalDateTime dateTime = BigDecimalConversions.toLocalDateTime(seconds, utcConverter);
        assertEquals(LocalDate.of(1970, 1, 1), date);
        assertEquals(LocalDateTime.of(1970, 1, 1, 0, 0), dateTime);
    }

    @Test
    public void testToZonedDateTimeAndOffsetDateTime() {
        // 2021-03-14T15:09:26Z (epoch seconds 1615734566)
        BigDecimal seconds = new BigDecimal("1615734566");
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(seconds, utcConverter);
        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(seconds, utcConverter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(Instant.ofEpochSecond(1615734566L), zdt.toInstant());
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testToOffsetTimeWithinDay() {
        // 23:59:59.999999999 => 86399.999999999 seconds
        BigDecimal seconds = new BigDecimal("86399.999999999");
        OffsetTime offsetTime = BigDecimalConversions.toOffsetTime(seconds, utcConverter);
        assertEquals(OffsetTime.of(LocalTime.of(23, 59, 59, 999_999_999), ZoneOffset.UTC), offsetTime);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToOffsetTimeOutOfRange() {
        BigDecimal seconds = new BigDecimal("86401"); // beyond a day
        BigDecimalConversions.toOffsetTime(seconds, utcConverter);
    }

    @Test
    public void testToDate() {
        BigDecimal seconds = new BigDecimal("0");
        Date date = BigDecimalConversions.toDate(seconds, utcConverter);
        assertEquals(Date.from(Instant.EPOCH), date);
    }

    @Test
    public void testToSqlDate() {
        // 2022-01-02 UTC midnight => epoch seconds for 2022-01-02T00:00:00Z
        BigDecimal seconds = new BigDecimal("1641081600"); // 2022-01-02
        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(seconds, utcConverter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2022, 1, 2)), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        BigDecimal seconds = new BigDecimal("12345.678");
        Timestamp ts = BigDecimalConversions.toTimestamp(seconds, utcConverter);
        Instant expected = Instant.ofEpochSecond(12345L, 678_000_000L);
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    public void testToBigInteger() {
        BigDecimal bd = new BigDecimal("12345678901234567890");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, utcConverter);
        assertEquals(new BigInteger("12345678901234567890"), bi);
    }

    @Test
    public void testToStringStripsTrailingZeros() {
        BigDecimal bd = new BigDecimal("123.45000");
        String str = BigDecimalConversions.toString(bd, utcConverter);
        assertEquals("123.45", str);
    }

    @Test
    public void testToUUIDZero() {
        // Zero should map to UUID(0,0) according to typical conversion.
        BigDecimal bd = BigDecimal.ZERO;
        UUID uuid = BigDecimalConversions.toUUID(bd, utcConverter);
        assertEquals(new UUID(0L, 0L), uuid);
    }

    @Test
    public void testSecondsAndNanosToDouble() {
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(10, 250_000_000);
        assertEquals(new BigDecimal("10.250000000"), result);
    }
}
