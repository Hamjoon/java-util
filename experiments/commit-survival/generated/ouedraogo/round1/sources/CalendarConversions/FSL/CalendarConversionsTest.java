package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link CalendarConversions}.
 *
 * The tests exercise every static conversion method, checking both the
 * returned value and, where appropriate, that the returned object is a
 * distinct instance from the source.
 *
 * The library’s {@link Converter} class is a final class with a single
 * constructor that takes a {@link ConverterOptions} instance.  The
 * concrete implementation of {@code ConverterOptions} is not part of the
 * source provided, but the library supplies a public no‑arg constructor.
 * The tests therefore create a real {@code Converter} using a default
 * {@code ConverterOptions} instance.  The options are configured with a
 * UTC time‑zone – this is sufficient for the conversion methods that
 * consult {@code converter.getOptions().getTimeZone()}.
 */
public class CalendarConversionsTest {

    private static final long EPOCH_MILLIS = 1_617_823_200_123L; // 2021‑04‑07T12:30:00.123Z
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone NEW_YORK = TimeZone.getTimeZone("America/New_York");

    private Calendar utcCalendar;
    private Calendar nyCalendar;
    private Converter utcConverter;
    private Converter nyConverter;

    @Before
    public void setUp() {
        // Calendar in UTC
        utcCalendar = Calendar.getInstance(UTC);
        utcCalendar.clear();
        utcCalendar.setTimeInMillis(EPOCH_MILLIS);
        utcConverter = new Converter(new ConverterOptions().setTimeZone(UTC));

        // Calendar in New York (may have DST offset)
        nyCalendar = Calendar.getInstance(NEW_YORK);
        nyCalendar.clear();
        nyCalendar.setTimeInMillis(EPOCH_MILLIS);
        nyConverter = new Converter(new ConverterOptions().setTimeZone(NEW_YORK));
    }

    @Test
    public void testToZonedDateTime_Utc() {
        ZonedDateTime expected = Instant.ofEpochMilli(EPOCH_MILLIS).atZone(UTC.toZoneId());
        ZonedDateTime actual = CalendarConversions.toZonedDateTime(utcCalendar, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToZonedDateTime_NewYork() {
        ZonedDateTime expected = Instant.ofEpochMilli(EPOCH_MILLIS).atZone(NEW_YORK.toZoneId());
        ZonedDateTime actual = CalendarConversions.toZonedDateTime(nyCalendar, nyConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        Long expected = EPOCH_MILLIS;
        assertEquals(expected, CalendarConversions.toLong(utcCalendar, utcConverter));
    }

    @Test
    public void testToDouble() {
        double expected = EPOCH_MILLIS / 1000.0;
        assertEquals(expected, CalendarConversions.toDouble(utcCalendar, utcConverter), 0.0);
    }

    @Test
    public void testToDate() {
        Date expected = new Date(EPOCH_MILLIS);
        assertEquals(expected, CalendarConversions.toDate(utcCalendar, utcConverter));
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = new java.sql.Date(EPOCH_MILLIS);
        assertEquals(expected, CalendarConversions.toSqlDate(utcCalendar, utcConverter));
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = new Timestamp(EPOCH_MILLIS);
        assertEquals(expected, CalendarConversions.toTimestamp(utcCalendar, utcConverter));
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong expected = new AtomicLong(EPOCH_MILLIS);
        assertEquals(expected, CalendarConversions.toAtomicLong(utcCalendar, utcConverter));
    }

    @Test
    public void testToInstant() {
        Instant expected = Instant.ofEpochMilli(EPOCH_MILLIS);
        assertEquals(expected, CalendarConversions.toInstant(utcCalendar, utcConverter));
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(EPOCH_MILLIS), UTC.toZoneId());
        assertEquals(expected, CalendarConversions.toLocalDateTime(utcCalendar, utcConverter));
    }

    @Test
    public void testToLocalDate() {
        LocalDate expected = LocalDate.ofInstant(Instant.ofEpochMilli(EPOCH_MILLIS), UTC.toZoneId());
        assertEquals(expected, CalendarConversions.toLocalDate(utcCalendar, utcConverter));
    }

    @Test
    public void testToLocalTime() {
        LocalTime expected = LocalTime.ofInstant(Instant.ofEpochMilli(EPOCH_MILLIS), UTC.toZoneId());
        assertEquals(expected, CalendarConversions.toLocalTime(utcCalendar, utcConverter));
    }

    @Test
    public void testToBigDecimal() {
        // BigDecimalConversions.GRAND is 1_000_000 (microseconds per second)
        BigDecimal expected = new BigDecimal(EPOCH_MILLIS).divide(BigDecimalConversions.GRAND);
        assertEquals(0, expected.compareTo(CalendarConversions.toBigDecimal(utcCalendar, utcConverter)));
    }

    @Test
    public void testToBigInteger() {
        BigInteger expected = BigInteger.valueOf(EPOCH_MILLIS * 1_000_000L);
        assertEquals(expected, CalendarConversions.toBigInteger(utcCalendar, utcConverter));
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
        Calendar created = CalendarConversions.create(EPOCH_MILLIS, utcConverter);
        assertEquals(EPOCH_MILLIS, created.getTimeInMillis());
        assertEquals(UTC, created.getTimeZone());
    }

    @Test
    public void testToString() {
        String expected = DateConversions.toString(utcCalendar.getTime(), utcConverter);
        String actual = CalendarConversions.toString(utcCalendar, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime_Utc() {
        OffsetDateTime expected = Instant.ofEpochMilli(EPOCH_MILLIS)
                .atOffset(ZoneOffset.ofTotalSeconds(UTC.getOffset(EPOCH_MILLIS) / 1000));
        OffsetDateTime actual = CalendarConversions.toOffsetDateTime(utcCalendar, utcConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime_NewYork() {
        OffsetDateTime expected = Instant.ofEpochMilli(EPOCH_MILLIS)
                .atOffset(ZoneOffset.ofTotalSeconds(NEW_YORK.getOffset(EPOCH_MILLIS) / 1000));
        OffsetDateTime actual = CalendarConversions.toOffsetDateTime(nyCalendar, nyConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap_Utc() {
        Map<String, Object> map = CalendarConversions.toMap(utcCalendar, utcConverter);

        String expectedDate = LocalDate.of(2021, 4, 7).toString(); // 2021‑04‑07
        String expectedTime = LocalTime.of(12, 30, 0, 123_000_000).toString(); // 12:30:00.123
        String expectedZone = UTC.toZoneId().toString();
        Long expectedEpoch = EPOCH_MILLIS;

        assertEquals(expectedDate, map.get(MapConversions.DATE));
        assertEquals(expectedTime, map.get(MapConversions.TIME));
        assertEquals(expectedZone, map.get(MapConversions.ZONE));
        assertEquals(expectedEpoch, map.get(MapConversions.EPOCH_MILLIS));
    }

    @Test
    public void testToMap_NewYork() {
        Map<String, Object> map = CalendarConversions.toMap(nyCalendar, nyConverter);

        // The date part is the same calendar date (local to the zone)
        LocalDate localDate = LocalDate.of(
                nyCalendar.get(Calendar.YEAR),
                nyCalendar.get(Calendar.MONTH) + 1,
                nyCalendar.get(Calendar.DAY_OF_MONTH));
        String expectedDate = localDate.toString();

        LocalTime localTime = LocalTime.of(
                nyCalendar.get(Calendar.HOUR_OF_DAY),
                nyCalendar.get(Calendar.MINUTE),
                nyCalendar.get(Calendar.SECOND),
                nyCalendar.get(Calendar.MILLISECOND) * 1_000_000);
        String expectedTime = localTime.toString();

        String expectedZone = NEW_YORK.toZoneId().toString();

        assertEquals(expectedDate, map.get(MapConversions.DATE));
        assertEquals(expectedTime, map.get(MapConversions.TIME));
        assertEquals(expectedZone, map.get(MapConversions.ZONE));
        assertEquals(EPOCH_MILLIS, map.get(MapConversions.EPOCH_MILLIS));
    }
}
