package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link DateConversions}. The tests exercise every public‑static method
 * in the class using real objects (no mocks) and verify the results against the
 * expected Java time / arithmetic behaviour.
 */
public class DateConversionsTest {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private Converter converter;

    @Before
    public void setUp() {
        // The real library provides a mutable ConverterOptions that lets us set the zone.
        // We assume a no‑arg constructor exists and a setter for the ZoneId.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(UTC);
        converter = new Converter(options);
    }

    @Test
    public void testToZonedDateTime() {
        Date date = new Date(1_600_000_000_000L); // 2020‑09‑13T12:26:40Z
        ZonedDateTime zdt = DateConversions.toZonedDateTime(date, converter);
        assertEquals(Instant.ofEpochMilli(date.getTime()).atZone(UTC), zdt);
    }

    @Test
    public void testToLong() {
        Date date = new Date(123_456L);
        long l = DateConversions.toLong(date, converter);
        assertEquals(date.getTime(), l);
    }

    @Test
    public void testToDouble() {
        Date date = new Date(1_234L);
        double d = DateConversions.toDouble(date, converter);
        assertEquals(date.getTime() / 1000.0, d, 0.000_001);
    }

    @Test
    public void testToSqlDate() {
        Date date = new Date(9_876_543_210L);
        java.sql.Date sqlDate = DateConversions.toSqlDate(date, converter);
        assertEquals(date.getTime(), sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        Date source = new Date(42L);
        Date copy = DateConversions.toDate(source, converter);
        assertEquals(source.getTime(), copy.getTime());
        // ensure it is a distinct instance
        assertNotSame(source, copy);
    }

    @Test
    public void testToTimestamp() {
        Date date = new Date(555_555L);
        Timestamp ts = DateConversions.toTimestamp(date, converter);
        assertEquals(date.getTime(), ts.getTime());
    }

    @Test
    public void testToCalendar() {
        Date date = new Date(1_111_111L);
        Calendar cal = DateConversions.toCalendar(date, converter);
        assertEquals(date.getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testToBigDecimal() {
        Date date = new Date(1_234_567_890L);
        // Expected value: epochMillis / 1_000_000_000 (GRAND) with 9 decimal places, rounding down.
        BigDecimal expected = new BigDecimal(date.getTime())
                .divide(BigDecimalConversions.GRAND, 9, RoundingMode.DOWN);
        BigDecimal actual = DateConversions.toBigDecimal(date, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToInstantWithUtilDate() {
        Date date = new Date(2_000_000L);
        Instant instant = DateConversions.toInstant(date, converter);
        assertEquals(date.toInstant(), instant);
    }

    @Test
    public void testToInstantWithSqlDate() {
        java.sql.Date sqlDate = new java.sql.Date(3_000_000L);
        Instant instant = DateConversions.toInstant(sqlDate, converter);
        // The method creates a new java.util.Date from the same millis before calling toInstant()
        assertEquals(Instant.ofEpochMilli(sqlDate.getTime()), instant);
    }

    @Test
    public void testToOffsetDateTime() {
        Date date = new Date(1_600_000_000_123L);
        OffsetDateTime odt = DateConversions.toOffsetDateTime(date, converter);
        OffsetDateTime expected = date.toInstant()
                .atZone(UTC)
                .toOffsetDateTime();
        assertEquals(expected, odt);
    }

    @Test
    public void testToLocalDateTime() {
        Date date = new Date(1_600_000_000_123L);
        LocalDateTime ldt = DateConversions.toLocalDateTime(date, converter);
        LocalDateTime expected = date.toInstant().atZone(UTC).toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test
    public void testToLocalDate() {
        Date date = new Date(1_600_000_000_123L);
        LocalDate ld = DateConversions.toLocalDate(date, converter);
        LocalDate expected = date.toInstant().atZone(UTC).toLocalDate();
        assertEquals(expected, ld);
    }

    @Test
    public void testToLocalTime() {
        Date date = new Date(1_600_000_123_456L);
        LocalTime lt = DateConversions.toLocalTime(date, converter);
        LocalTime expected = LocalDateTime.ofInstant(date.toInstant(), UTC).toLocalTime();
        assertEquals(expected, lt);
    }

    @Test
    public void testToBigInteger() {
        Date date = new Date(9_876_543_210L);
        Instant instant = date.toInstant();
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        BigInteger actual = DateConversions.toBigInteger(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToAtomicLong() {
        Date date = new Date(777_777L);
        AtomicLong atomicLong = DateConversions.toAtomicLong(date, converter);
        assertEquals(date.getTime(), atomicLong.get());
    }

    @Test
    public void testSqlDateToString() {
        java.sql.Date sqlDate = new java.sql.Date(1_600_000_000_123L);
        String str = DateConversions.sqlDateToString(sqlDate, converter);
        // The string should be identical to the normal toString conversion of the same instant.
        String expected = DateConversions.toString(new Date(sqlDate.getTime()), converter);
        assertEquals(expected, str);
    }

    @Test
    public void testToStringFormatting() {
        // Use a fixed instant that includes milliseconds.
        Date date = new Date(1_600_000_000_123L); // 2020‑09‑13T12:26:40.123Z
        String formatted = DateConversions.toString(date, converter);
        // Build the same formatter manually to verify.
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendOffset("+HH:MM", "Z")
                .toFormatter();
        String expected = date.toInstant().atZone(UTC).format(formatter);
        assertEquals(expected, formatted);
    }

    @Test
    public void testToMap() {
        Date date = new Date(1_600_000_000_123L);
        Map<String, Object> map = DateConversions.toMap(date, converter);
        ZonedDateTime zdt = DateConversions.toZonedDateTime(date, converter);

        assertEquals(4, map.size());
        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(UTC.toString(), map.get(MapConversions.ZONE));
        assertEquals(date.getTime(), map.get(MapConversions.EPOCH_MILLIS));
    }
}
