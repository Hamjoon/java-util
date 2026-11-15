package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link CalendarConversions}.
 *
 * The tests cover every static conversion method defined in the class.
 * Real objects are created (no mocking) and only APIs that appear in the
 * source code or in the supplied public signatures are used.
 */
public class CalendarConversionsTest {

    private static final long EPOCH_MILLIS = 1_600_000_123_456L; // a deterministic instant
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone PST = TimeZone.getTimeZone("America/Los_Angeles"); // UTC‑08:00 (or ‑07:00 with DST)

    private Calendar utcCalendar;
    private Calendar pstCalendar;
    private Converter utcConverter;
    private Converter pstConverter;

    @Before
    public void setUp() {
        // Calendar in UTC
        utcCalendar = new GregorianCalendar(UTC);
        utcCalendar.setTimeInMillis(EPOCH_MILLIS);
        utcCalendar.clear();               // clear fields to avoid leftover values
        utcCalendar.setTimeInMillis(EPOCH_MILLIS);

        // Calendar in Pacific Standard Time (will respect DST rules automatically)
        pstCalendar = new GregorianCalendar(PST);
        pstCalendar.setTimeInMillis(EPOCH_MILLIS);

        // Converter instances – the only thing we need from the options is a TimeZone.
        // The real library provides a mutable ConverterOptions; we use it exactly as
        // advertised by the public API (constructor + getOptions()).
        utcConverter = new Converter(new ConverterOptions().setTimeZone(UTC));
        pstConverter = new Converter(new ConverterOptions().setTimeZone(PST));
    }

    @Test
    public void testToZonedDateTime_Utc() {
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(utcCalendar, utcConverter);
        assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), zdt.toInstant());
        assertEquals(UTC.toZoneId(), zdt.getZone());
    }

    @Test
    public void testToZonedDateTime_Pst() {
        ZonedDateTime zdt = CalendarConversions.toZonedDateTime(pstCalendar, pstConverter);
        assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), zdt.toInstant());
        assertEquals(PST.toZoneId(), zdt.getZone());
    }

    @Test
    public void testToLong() {
        Long epoch = CalendarConversions.toLong(utcCalendar, utcConverter);
        assertEquals(Long.valueOf(EPOCH_MILLIS), epoch);
    }

    @Test
    public void testToDouble() {
        double seconds = CalendarConversions.toDouble(utcCalendar, utcConverter);
        assertEquals(EPOCH_MILLIS / 1000.0, seconds, 0.0);
    }

    @Test
    public void testToDate() {
        java.util.Date date = CalendarConversions.toDate(utcCalendar, utcConverter);
        assertEquals(new java.util.Date(EPOCH_MILLIS), date);
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = CalendarConversions.toSqlDate(utcCalendar, utcConverter);
        assertEquals(new java.sql.Date(EPOCH_MILLIS), sqlDate);
    }

    @Test
    public void testToTimestamp() {
        Timestamp ts = CalendarConversions.toTimestamp(utcCalendar, utcConverter);
        assertEquals(new Timestamp(EPOCH_MILLIS), ts);
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = CalendarConversions.toAtomicLong(utcCalendar, utcConverter);
        assertEquals(new AtomicLong(EPOCH_MILLIS), al);
    }

    @Test
    public void testToInstant() {
        Instant instant = CalendarConversions.toInstant(utcCalendar, utcConverter);
        assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), instant);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = CalendarConversions.toLocalDateTime(utcCalendar, utcConverter);
        assertEquals(LocalDateTime.ofInstant(Instant.ofEpochMilli(EPOCH_MILLIS), UTC.toZoneId()), ldt);
    }

    @Test
    public void testToLocalDate() {
        LocalDate ld = CalendarConversions.toLocalDate(utcCalendar, utcConverter);
        assertEquals(LocalDate.ofInstant(Instant.ofEpochMilli(EPOCH_MILLIS), UTC.toZoneId()), ld);
    }

    @Test
    public void testToLocalTime() {
        LocalTime lt = CalendarConversions.toLocalTime(utcCalendar, utcConverter);
        assertEquals(LocalTime.ofInstant(Instant.ofEpochMilli(EPOCH_MILLIS), UTC.toZoneId()), lt);
    }

    @Test
    public void testToBigDecimal() {
        // The library divides by BigDecimalConversions.GRAND which is 1_000 (ms → seconds)
        BigDecimal expected = BigDecimal.valueOf(EPOCH_MILLIS).divide(BigDecimal.valueOf(1_000));
        BigDecimal actual = CalendarConversions.toBigDecimal(utcCalendar, utcConverter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToBigInteger() {
        BigInteger expected = BigInteger.valueOf(EPOCH_MILLIS).multiply(BigInteger.valueOf(1_000_000L));
        BigInteger actual = CalendarConversions.toBigInteger(utcCalendar, utcConverter);
        assertEquals(expected, actual);
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
        assertEquals(UTC, created.getTimeZone());
        assertEquals(EPOCH_MILLIS, created.getTimeInMillis());
    }

    @Test
    public void testCreate_Pst() {
        Calendar created = CalendarConversions.create(EPOCH_MILLIS, pstConverter);
        assertEquals(PST, created.getTimeZone());
        assertEquals(EPOCH_MILLIS, created.getTimeInMillis());
    }

    @Test
    public void testToString_Utc() {
        String s = CalendarConversions.toString(utcCalendar, utcConverter);
        assertNotNull(s);
        // The exact format depends on DateConversions; we only verify that the
        // produced string contains the epoch milliseconds as a sanity check.
        assertTrue(s.contains(Long.toString(EPOCH_MILLIS)));
    }

    @Test
    public void testToOffsetDateTime_Utc() {
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(utcCalendar, utcConverter);
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(UTC.getOffset(EPOCH_MILLIS) / 1000);
        assertEquals(expectedOffset, odt.getOffset());
        assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), odt.toInstant());
    }

    @Test
    public void testToOffsetDateTime_Pst() {
        OffsetDateTime odt = CalendarConversions.toOffsetDateTime(pstCalendar, pstConverter);
        ZoneOffset expectedOffset = ZoneOffset.ofTotalSeconds(PST.getOffset(EPOCH_MILLIS) / 1000);
        assertEquals(expectedOffset, odt.getOffset());
        assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), odt.toInstant());
    }

    @Test
    public void testToMap_Utc() {
        Map<String, Object> map = CalendarConversions.toMap(utcCalendar, utcConverter);
        assertEquals(4, map.size());

        // DATE – ISO‑8601 local date string
        String expectedDate = LocalDate.of(utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH) + 1,
                utcCalendar.get(Calendar.DAY_OF_MONTH)).toString();
        assertEquals(expectedDate, map.get(MapConversions.DATE));

        // TIME – ISO‑8601 local time string with nanosecond precision
        String expectedTime = LocalTime.of(utcCalendar.get(Calendar.HOUR_OF_DAY),
                utcCalendar.get(Calendar.MINUTE),
                utcCalendar.get(Calendar.SECOND),
                utcCalendar.get(Calendar.MILLISECOND) * 1_000_000).toString();
        assertEquals(expectedTime, map.get(MapConversions.TIME));

        // ZONE – zone ID string
        assertEquals(UTC.toZoneId().toString(), map.get(MapConversions.ZONE));

        // EPOCH_MILLIS – raw long value
        assertEquals(EPOCH_MILLIS, ((Number) map.get(MapConversions.EPOCH_MILLIS)).longValue());
    }

    @Test
    public void testToMap_Pst() {
        Map<String, Object> map = CalendarConversions.toMap(pstCalendar, pstConverter);
        assertEquals(4, map.size());

        String expectedDate = LocalDate.of(pstCalendar.get(Calendar.YEAR),
                pstCalendar.get(Calendar.MONTH) + 1,
                pstCalendar.get(Calendar.DAY_OF_MONTH)).toString();
        assertEquals(expectedDate, map.get(MapConversions.DATE));

        String expectedTime = LocalTime.of(pstCalendar.get(Calendar.HOUR_OF_DAY),
                pstCalendar.get(Calendar.MINUTE),
                pstCalendar.get(Calendar.SECOND),
                pstCalendar.get(Calendar.MILLISECOND) * 1_000_000).toString();
        assertEquals(expectedTime, map.get(MapConversions.TIME));

        assertEquals(PST.toZoneId().toString(), map.get(MapConversions.ZONE));
        assertEquals(EPOCH_MILLIS, ((Number) map.get(MapConversions.EPOCH_MILLIS)).longValue());
    }
}
