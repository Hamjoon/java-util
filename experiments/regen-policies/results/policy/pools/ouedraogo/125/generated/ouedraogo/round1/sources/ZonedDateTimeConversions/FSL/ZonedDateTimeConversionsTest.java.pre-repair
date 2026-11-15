package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link ZonedDateTimeConversions}.
 *
 * The tests exercise every public static conversion method using real
 * {@link ZonedDateTime} instances and a real {@link Converter}.  The
 * {@link Converter} is created with default {@link ConverterOptions}
 * (i.e., the JVM default time‑zone).  For the methods that adjust the
 * zone based on the converter's options we verify that the result is
 * identical to the source when the default zone is used.
 */
public class ZonedDateTimeConversionsTest {

    private Converter converter;
    private ZonedDateTime utcZdt;          // 2023‑01‑01T12:34:56.789Z
    private ZonedDateTime nyZdt;           // same instant, America/New_York

    @Before
    public void setUp() {
        // Create a Converter with default options (system default zone)
        converter = new Converter(new ConverterOptions());

        utcZdt = ZonedDateTime.of(
                2023, 1, 1,
                12, 34, 56, 789_000_000,
                ZoneId.of("UTC"));

        nyZdt = utcZdt.withZoneSameInstant(ZoneId.of("America/New_York"));
    }

    @Test
    public void testToLong() {
        long expected = utcZdt.toInstant().toEpochMilli();
        assertEquals(expected, ZonedDateTimeConversions.toLong(utcZdt, converter));
    }

    @Test
    public void testToDouble() {
        // InstantConversions.toDouble is expected to return the epoch milli as double
        double expected = (double) utcZdt.toInstant().toEpochMilli();
        assertEquals(expected, ZonedDateTimeConversions.toDouble(utcZdt, converter), 0.0);
    }

    @Test
    public void testToInstant() {
        Instant expected = utcZdt.toInstant();
        assertEquals(expected, ZonedDateTimeConversions.toInstant(utcZdt, converter));
    }

    @Test
    public void testToLocalDateTime_DefaultZone() {
        // With default options (system zone) the conversion should be a no‑op
        LocalDateTime expected = utcZdt.toLocalDateTime();
        assertEquals(expected, ZonedDateTimeConversions.toLocalDateTime(utcZdt, converter));
    }

    @Test
    public void testToLocalDate_DefaultZone() {
        LocalDate expected = utcZdt.toLocalDate();
        assertEquals(expected, ZonedDateTimeConversions.toLocalDate(utcZdt, converter));
    }

    @Test
    public void testToLocalTime_DefaultZone() {
        LocalTime expected = utcZdt.toLocalTime();
        assertEquals(expected, ZonedDateTimeConversions.toLocalTime(utcZdt, converter));
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime expected = utcZdt.toOffsetDateTime();
        assertEquals(expected, ZonedDateTimeConversions.toOffsetDateTime(utcZdt, converter));
    }

    @Test
    public void testToAtomicLong() {
        long expected = utcZdt.toInstant().toEpochMilli();
        AtomicLong atomic = ZonedDateTimeConversions.toAtomicLong(utcZdt, converter);
        assertNotNull(atomic);
        assertEquals(expected, atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(utcZdt.toInstant());
        Timestamp actual = ZonedDateTimeConversions.toTimestamp(utcZdt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        Calendar cal = ZonedDateTimeConversions.toCalendar(utcZdt, converter);
        assertNotNull(cal);
        // Calendar should be created with the converter's time‑zone (system default)
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone());
        assertEquals(utcZdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = new java.sql.Date(utcZdt.toInstant().toEpochMilli());
        assertEquals(expected, ZonedDateTimeConversions.toSqlDate(utcZdt, converter));
    }

    @Test
    public void testToDate() {
        Date expected = new Date(utcZdt.toInstant().toEpochMilli());
        assertEquals(expected, ZonedDateTimeConversions.toDate(utcZdt, converter));
    }

    @Test
    public void testToBigInteger() {
        // InstantConversions.toBigInteger is expected to return the epoch milli as BigInteger
        BigInteger expected = BigInteger.valueOf(utcZdt.toInstant().toEpochMilli());
        assertEquals(expected, ZonedDateTimeConversions.toBigInteger(utcZdt, converter));
    }

    @Test
    public void testToBigDecimal() {
        // InstantConversions.toBigDecimal is expected to return the epoch milli as BigDecimal
        BigDecimal expected = BigDecimal.valueOf(utcZdt.toInstant().toEpochMilli());
        assertEquals(0, expected.compareTo(ZonedDateTimeConversions.toBigDecimal(utcZdt, converter)));
    }

    @Test
    public void testToString() {
        String expected = utcZdt.format(DateTimeFormatter.ISO_DATE_TIME);
        assertEquals(expected, ZonedDateTimeConversions.toString(utcZdt, converter));
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = ZonedDateTimeConversions.toMap(utcZdt, converter);
        assertNotNull(map);
        assertEquals(3, map.size());

        assertEquals(utcZdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(utcZdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(utcZdt.getZone().toString(), map.get(MapConversions.ZONE));
    }

    @Test
    public void testZoneConversionWithDifferentTargetZone() {
        // Create a Converter whose options specify America/New_York as the target zone.
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(ZoneId.of("America/New_York"));
        opts.setTimeZone(java.util.TimeZone.getTimeZone("America/New_York"));
        Converter nyConverter = new Converter(opts);

        // LocalDate should reflect the New York date for the same instant.
        LocalDate expectedDate = utcZdt.withZoneSameInstant(ZoneId.of("America/New_York")).toLocalDate();
        assertEquals(expectedDate, ZonedDateTimeConversions.toLocalDate(utcZdt, nyConverter));

        // LocalTime should reflect the New York time.
        LocalTime expectedTime = utcZdt.withZoneSameInstant(ZoneId.of("America/New_York")).toLocalTime();
        assertEquals(expectedTime, ZonedDateTimeConversions.toLocalTime(utcZdt, nyConverter));

        // LocalDateTime conversion should also reflect the New York zone.
        LocalDateTime expectedLdt = utcZdt.withZoneSameInstant(ZoneId.of("America/New_York")).toLocalDateTime();
        assertEquals(expectedLdt, ZonedDateTimeConversions.toLocalDateTime(utcZdt, nyConverter));
    }
}
