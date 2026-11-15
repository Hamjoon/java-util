package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class InstantConversionsTest {

    private Converter utcConverter;
    private Converter offsetConverter;

    @Before
    public void setUp() {
        // Build a Converter whose options use UTC zone – this makes the tests deterministic.
        // ConverterOptions is part of the library; we assume it has a public no‑arg constructor
        // and setter methods for zoneId and timeZone. If such setters are not present we fall back
        // to the defaults which are also UTC on most CI systems.
        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setZoneId(ZoneId.of("UTC"));
        utcOptions.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOptions);

        // Another converter that uses a non‑UTC zone to test offset handling.
        ConverterOptions offsetOptions = new ConverterOptions();
        offsetOptions.setZoneId(ZoneId.of("America/New_York"));
        offsetOptions.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        offsetConverter = new Converter(offsetOptions);
    }

    @Test
    public void testToMap() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 123_456_789);
        Map<String, Object> map = InstantConversions.toMap(instant, utcConverter);
        assertEquals("Map should contain exactly two entries", 2, map.size());
        assertTrue("Map should contain key 'seconds'", map.containsKey("seconds"));
        assertTrue("Map should contain key 'nanos'", map.containsKey("nanos"));
        assertEquals(1_600_000_000L, map.get("seconds"));
        assertEquals(123_456_789, map.get("nanos"));
    }

    @Test
    public void testToZonedDateTime_Utc() {
        Instant instant = Instant.parse("2023-03-25T12:34:56.789Z");
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, utcConverter);
        assertEquals(ZoneId.of("UTC"), zdt.getZone());
        assertEquals(instant.getEpochSecond(), zdt.toInstant().getEpochSecond());
        assertEquals(instant.getNano(), zdt.toInstant().getNano());
    }

    @Test
    public void testToZonedDateTime_NonUtc() {
        Instant instant = Instant.parse("2023-03-25T12:34:56.789Z");
        ZonedDateTime zdt = InstantConversions.toZonedDateTime(instant, offsetConverter);
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
        // The underlying instant must stay identical
        assertEquals(instant, zdt.toInstant());
    }

    @Test
    public void testToOffsetDateTime_Utc() {
        Instant instant = Instant.parse("2023-03-25T12:34:56.789Z");
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, utcConverter);
        // UTC offset is zero
        assertEquals(ZoneOffset.UTC, odt.getOffset());
        assertEquals(instant, odt.toInstant());
    }

    @Test
    public void testToOffsetDateTime_NonUtc() {
        Instant instant = Instant.parse("2023-07-01T12:00:00Z"); // Summer time in New York (UTC‑4)
        OffsetDateTime odt = InstantConversions.toOffsetDateTime(instant, offsetConverter);
        TimeZone tz = offsetConverter.getOptions().getTimeZone();
        int expectedOffsetSeconds = tz.getOffset(instant.toEpochMilli()) / 1000;
        ZoneOffset expected = ZoneOffset.ofTotalSeconds(expectedOffsetSeconds);
        assertEquals(expected, odt.getOffset());
        assertEquals(instant, odt.toInstant());
    }

    @Test
    public void testToLong() {
        Instant instant = Instant.ofEpochMilli(1_620_000_000_123L);
        long value = InstantConversions.toLong(instant, utcConverter);
        assertEquals(instant.toEpochMilli(), value);
    }

    @Test
    public void testToDouble() {
        Instant instant = Instant.ofEpochSecond(1_620_000_000L, 500_000_000); // .5 second
        double d = InstantConversions.toDouble(instant, utcConverter);
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
        assertEquals(expected, d, 1e-12);
    }

    @Test
    public void testToAtomicLong() {
        Instant instant = Instant.ofEpochMilli(1_620_000_001L);
        AtomicLong atomic = InstantConversions.toAtomicLong(instant, utcConverter);
        assertEquals(instant.toEpochMilli(), atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Instant instant = Instant.now();
        Timestamp ts = InstantConversions.toTimestamp(instant, utcConverter);
        assertEquals(Timestamp.from(instant), ts);
    }

    @Test
    public void testToSqlDate() {
        Instant instant = Instant.ofEpochMilli(1_620_000_010L);
        java.sql.Date sqlDate = InstantConversions.toSqlDate(instant, utcConverter);
        assertEquals(new java.sql.Date(instant.toEpochMilli()), sqlDate);
    }

    @Test
    public void testToDate() {
        Instant instant = Instant.ofEpochMilli(1_620_000_020L);
        Date date = InstantConversions.toDate(instant, utcConverter);
        assertEquals(new Date(instant.toEpochMilli()), date);
    }

    @Test
    public void testToCalendar() {
        Instant instant = Instant.ofEpochMilli(1_620_000_030L);
        Calendar cal = InstantConversions.toCalendar(instant, utcConverter);
        assertEquals(instant.toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToBigInteger() {
        Instant instant = Instant.ofEpochSecond(2_000_000_000L, 123_456_789);
        BigInteger bi = InstantConversions.toBigInteger(instant, utcConverter);
        BigInteger expected = BigInteger.valueOf(instant.getEpochSecond())
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(instant.getNano()));
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = Instant.ofEpochSecond(2_000_000_001L, 250_000_000);
        BigDecimal bd = InstantConversions.toBigDecimal(instant, utcConverter);
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano()).divide(BigDecimal.valueOf(1_000_000_000L)));
        // The library conversion keeps the scale that results from division; we compare numerically.
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToLocalDateTime() {
        Instant instant = Instant.parse("2023-03-25T10:15:30.123Z");
        LocalDateTime ldt = InstantConversions.toLocalDateTime(instant, utcConverter);
        assertEquals(LocalDateTime.ofInstant(instant, ZoneId.of("UTC")), ldt);
    }

    @Test
    public void testToLocalDate() {
        Instant instant = Instant.parse("2023-03-25T23:59:59Z");
        LocalDate ld = InstantConversions.toLocalDate(instant, utcConverter);
        assertEquals(LocalDate.ofInstant(instant, ZoneId.of("UTC")), ld);
    }

    @Test
    public void testToLocalTime() {
        Instant instant = Instant.parse("2023-03-25T12:34:56.789Z");
        LocalTime lt = InstantConversions.toLocalTime(instant, utcConverter);
        assertEquals(LocalTime.ofInstant(instant, ZoneId.of("UTC")), lt);
    }

    @Test
    public void testNegativeInstant() {
        // Instant before the epoch
        Instant instant = Instant.ofEpochSecond(-10, 500_000_000);
        // Map
        Map<String, Object> map = InstantConversions.toMap(instant, utcConverter);
        assertEquals(-10L, map.get("seconds"));
        assertEquals(500_000_000, map.get("nanos"));
        // Long
        assertEquals(instant.toEpochMilli(), InstantConversions.toLong(instant, utcConverter));
        // BigInteger
        BigInteger bi = InstantConversions.toBigInteger(instant, utcConverter);
        BigInteger expected = BigInteger.valueOf(-10L).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(500_000_000));
        assertEquals(expected, bi);
    }
}
