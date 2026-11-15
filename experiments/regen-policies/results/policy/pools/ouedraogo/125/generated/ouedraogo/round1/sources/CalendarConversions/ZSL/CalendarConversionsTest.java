package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link CalendarConversions}.
 * <p>
 * The test class lives in the same package as {@code CalendarConversions}
 * so that it can access the package‑private static methods.
 * </p>
 */
public class CalendarConversionsTest {

    private Calendar utcCalendar;
    private Calendar nyCalendar;
    private Converter utcConverter;
    private Converter nyConverter;
    private long epochMilli;

    @Before
    public void setUp() {
        // Fixed instant: 2022‑03‑15T12:34:56.789Z
        epochMilli = Instant.parse("2022-03-15T12:34:56.789Z").toEpochMilli();

        // Calendar in UTC
        utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.clear();
        utcCalendar.setTimeInMillis(epochMilli);

        // Calendar in New York (America/New_York) – observes DST on the chosen date
        nyCalendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        nyCalendar.clear();
        nyCalendar.setTimeInMillis(epochMilli);

        // Converter with UTC timezone option
        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setTimeZone(TimeZone.getTimeZone("UTC"));
        utcConverter = new Converter(utcOptions);

        // Converter with New York timezone option
        ConverterOptions nyOptions = new ConverterOptions();
        nyOptions.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        nyConverter = new Converter(nyOptions);
    }

    @Test
    public void testToZonedDateTime_Utc() {
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(utcCalendar, utcConverter);
        ZonedDateTime expected = Instant.ofEpochMilli(epochMilli).atZone(ZoneId.of("UTC"));
        assertEquals(expected, zdt);
    }

    @Test
    public void testToZonedDateTime_NewYork() {
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(nyCalendar, nyConverter);
        ZonedDateTime expected = Instant.ofEpochMilli(epochMilli).atZone(ZoneId.of("America/New_York"));
        assertEquals(expected, zdt);
    }

    @Test
    public void testToLong() {
        Long result = CalendarConversions.toLong(utcCalendar, utcConverter);
        assertEquals(Long.valueOf(epochMilli), result);
    }

    @Test
    public void testToDouble() {
        double result = CalendarConversions.toDouble(utcCalendar, utcConverter);
        double expected = epochMilli / 1000.0;
        assertEquals(expected, result, 0.000001);
    }

    @Test
    public void testToDate() {
        java.util.Date result = CalendarConversions.toDate(utcCalendar, utcConverter);
        assertEquals(new java.util.Date(epochMilli), result);
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date result = CalendarConversions.toSqlDate(utcCalendar, utcConverter);
        assertEquals(new java.sql.Date(epochMilli), result);
    }

    @Test
    public void testToTimestamp() {
        Timestamp result = CalendarConversions.toTimestamp(utcCalendar, utcConverter);
        assertEquals(new Timestamp(epochMilli), result);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong result = CalendarConversions.toAtomicLong(utcCalendar, utcConverter);
        assertEquals(new AtomicLong(epochMilli).get(), result.get());
    }

    @Test
    public void testToInstant() {
        Instant result = CalendarConversions.toInstant(utcCalendar, utcConverter);
        assertEquals(Instant.ofEpochMilli(epochMilli), result);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime result = CalendarConversions.toLocalDateTime(utcCalendar, utcConverter);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.of("UTC"));
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalDate() {
        LocalDate result = CalendarConversions.toLocalDate(utcCalendar, utcConverter);
        LocalDate expected = LocalDate.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.of("UTC"));
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalTime() {
        LocalTime result = CalendarConversions.toLocalTime(utcCalendar, utcConverter);
        LocalTime expected = LocalTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.of("UTC"));
        assertEquals(expected, result);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal result = CalendarConversions.toBigDecimal(utcCalendar, utcConverter);
        // Verify that result * GRAND == epochMilli
        BigDecimal recon = result.multiply(BigDecimalConversions.GRAND);
        assertEquals(BigDecimal.valueOf(epochMilli), recon);
    }

    @Test
    public void testToBigInteger() {
        BigInteger result = CalendarConversions.toBigInteger(utcCalendar, utcConverter);
        BigInteger expected = BigInteger.valueOf(epochMilli).multiply(BigInteger.valueOf(1_000_000L));
        assertEquals(expected, result);
    }

    @Test
    public void testClone() {
        Calendar cloned = CalendarConversions.clone(utcCalendar, utcConverter);
        assertNotSame(utcCalendar, cloned);
        assertEquals(utcCalendar.getTimeInMillis(), cloned.getTimeInMillis());
        assertEquals(utcCalendar.getTimeZone(), cloned.getTimeZone());
    }

    @Test
    public void testCreate_Utc() {
        Calendar created = CalendarConversions.create(epochMilli, utcConverter);
        assertEquals(TimeZone.getTimeZone("UTC"), created.getTimeZone());
        assertEquals(epochMilli, created.getTimeInMillis());
    }

    @Test
    public void testCreate_NewYork() {
        Calendar created = CalendarConversions.create(epochMilli, nyConverter);
        assertEquals(TimeZone.getTimeZone("America/New_York"), created.getTimeZone());
        assertEquals(epochMilli, created.getTimeInMillis());
    }

    @Test
    public void testToString() {
        String result = CalendarConversions.toString(utcCalendar, utcConverter);
        assertNotNull(result);
        // The string should contain the ISO‑8601 representation of the instant.
        assertTrue(result.contains("2022-03-15T12:34:56"));
    }

    @Test
    public void testToOffsetDateTime_Utc() {
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(utcCalendar, utcConverter);
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(utcCalendar.getTimeZone().getOffset(epochMilli) / 1000);
        OffsetDateTime expected = Instant.ofEpochMilli(epochMilli).atOffset(expectedOffset);
        assertEquals(expected, odt);
    }

    @Test
    public void testToOffsetDateTime_NewYork() {
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(nyCalendar, nyConverter);
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(nyCalendar.getTimeZone().getOffset(epochMilli) / 1000);
        OffsetDateTime expected = Instant.ofEpochMilli(epochMilli).atOffset(expectedOffset);
        assertEquals(expected, odt);
    }

    @Test
    public void testToMap_Utc() {
        Map<String, Object> map = CalendarConversions.toMap(utcCalendar, utcConverter);

        String expectedDate = LocalDate.of(2022, 3, 15).toString(); // 2022-03-15
        String expectedTime = LocalTime.of(12, 34, 56, 789_000_000).toString(); // 12:34:56.789
        String expectedZone = "UTC";

        assertEquals(expectedDate, map.get(MapConversions.DATE));
        assertEquals(expectedTime, map.get(MapConversions.TIME));
        assertEquals(expectedZone, map.get(MapConversions.ZONE));
        assertEquals(epochMilli, map.get(MapConversions.EPOCH_MILLIS));
    }

    @Test
    public void testToMap_NewYork() {
        Map<String, Object> map = CalendarConversions.toMap(nyCalendar, nyConverter);

        // New York offset on 2022‑03‑15 is -04:00 (EDT)
        String expectedDate = LocalDate.of(2022, 3, 15).toString();
        // The time part reflects the local NY time (UTC‑4)
        String expectedTime = LocalTime.of(8, 34, 56, 789_000_000).toString(); // 08:34:56.789
        String expectedZone = "America/New_York";

        assertEquals(expectedDate, map.get(MapConversions.DATE));
        assertEquals(expectedTime, map.get(MapConversions.TIME));
        assertEquals(expectedZone, map.get(MapConversions.ZONE));
        assertEquals(epochMilli, map.get(MapConversions.EPOCH_MILLIS));
    }
}
