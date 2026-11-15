package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class BigDecimalConversionsTest {

    private Converter converter;
    private ConverterOptions options;

    @Before
    public void setUp() {
        // The library provides a default options constructor; we rely on it.
        options = new ConverterOptions();               // uses default time‑zone / zone‑id
        converter = new Converter(options);
    }

    @Test
    public void testToCalendar() {
        BigDecimal seconds = new BigDecimal("1234.567"); // 1234.567 seconds
        Calendar cal = BigDecimalConversions.toCalendar(seconds, converter);
        long expectedMillis = seconds.multiply(BigDecimalConversions.GRAND).longValue();
        assertEquals(expectedMillis, cal.getTimeInMillis());
        assertEquals(options.getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testToInstant() {
        BigDecimal seconds = new BigDecimal("1609459200.123456789"); // 2021‑01‑01T00:00:00.123456789Z
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);
        Instant expected = Instant.ofEpochSecond(1609459200L, 123456789L);
        assertEquals(expected, instant);
    }

    @Test
    public void testToDuration() {
        BigDecimal seconds = new BigDecimal("3600.5"); // 1 hour + 0.5 sec
        Duration duration = BigDecimalConversions.toDuration(seconds, converter);
        Duration expected = Duration.ofSeconds(3600L, 500_000_000L);
        assertEquals(expected, duration);
    }

    @Test
    public void testToLocalTime_Valid() {
        // 12:34:56.789012345 -> seconds = 45296.789012345
        BigDecimal seconds = new BigDecimal("45296.789012345");
        LocalTime time = BigDecimalConversions.toLocalTime(seconds, converter);
        LocalTime expected = LocalTime.of(12, 34, 56, 789_012_345);
        assertEquals(expected, time);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_InvalidNegative() {
        BigDecimal seconds = new BigDecimal("-1");
        BigDecimalConversions.toLocalTime(seconds, converter);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocalTime_InvalidTooLarge() {
        // 24 hours = 86400 seconds, which is outside the allowed range
        BigDecimal seconds = new BigDecimal("86400");
        BigDecimalConversions.toLocalTime(seconds, converter);
    }

    @Test
    public void testToZonedDateTime_andRelated() {
        // 2022‑03‑14T15:09:26.535897932Z
        BigDecimal seconds = new BigDecimal("1647271766.535897932");
        ZonedDateTime zdt = BigDecimalConversions.toZonedDateTime(seconds, converter);
        ZonedDateTime expected = Instant.ofEpochSecond(1647271766L, 535_897_932L)
                .atZone(options.getZoneId());
        assertEquals(expected, zdt);

        LocalDate ld = BigDecimalConversions.toLocalDate(seconds, converter);
        assertEquals(zdt.toLocalDate(), ld);

        LocalDateTime ldt = BigDecimalConversions.toLocalDateTime(seconds, converter);
        assertEquals(zdt.toLocalDateTime(), ldt);

        OffsetDateTime odt = BigDecimalConversions.toOffsetDateTime(seconds, converter);
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testToDate_andSqlDate_andTimestamp() {
        BigDecimal seconds = new BigDecimal("1622505600.250"); // 2021‑06‑01T00:00:00.250Z
        Instant instant = BigDecimalConversions.toInstant(seconds, converter);

        Date date = BigDecimalConversions.toDate(seconds, converter);
        assertEquals(Date.from(instant), date);

        java.sql.Date sqlDate = BigDecimalConversions.toSqlDate(seconds, converter);
        // sqlDate drops the time‑part, so compare epoch day
        assertEquals(Instant.ofEpochMilli(sqlDate.getTime()).atZone(ZoneOffset.UTC).toLocalDate(),
                     instant.atZone(ZoneOffset.UTC).toLocalDate());

        Timestamp ts = BigDecimalConversions.toTimestamp(seconds, converter);
        assertEquals(Timestamp.from(instant), ts);
    }

    @Test
    public void testToBigInteger() {
        BigDecimal bd = new BigDecimal("12345678901234567890");
        BigInteger bi = BigDecimalConversions.toBigInteger(bd, converter);
        assertEquals(new BigInteger("12345678901234567890"), bi);
    }

    @Test
    public void testToString() {
        BigDecimal bd = new BigDecimal("123.45000");
        String s = BigDecimalConversions.toString(bd, converter);
        assertEquals("123.45", s);
    }

    @Test
    public void testToUUID() {
        // Use a known UUID and convert it to a BigDecimal via its most‑significant bits.
        UUID original = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
        BigInteger bi = new BigInteger(original.toString().replace("-", ""), 16);
        BigDecimal bd = new BigDecimal(bi);
        UUID result = BigDecimalConversions.toUUID(bd, converter);
        assertEquals(original, result);
    }

    @Test
    public void testSecondsAndNanosToDouble() {
        long secs = 42L;
        long nanos = 500_000_000L; // .5 second
        BigDecimal result = BigDecimalConversions.secondsAndNanosToDouble(secs, nanos);
        BigDecimal expected = new BigDecimal("42.5");
        assertEquals(0, expected.compareTo(result));
    }
}
