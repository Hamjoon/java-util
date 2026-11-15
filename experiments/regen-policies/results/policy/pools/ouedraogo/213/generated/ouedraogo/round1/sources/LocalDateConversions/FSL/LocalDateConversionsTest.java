package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for {@link LocalDateConversions}. All static conversion methods are exercised
 * with a deterministic {@link Converter} whose options are set to UTC.  The tests
 * verify that the returned objects contain the expected date‑time values and that
 * the map conversion uses the correct key defined by {@link MapConversions#LOCAL_DATE}.
 */
public class LocalDateConversionsTest {

    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");
    private static final LocalDate SAMPLE_DATE = LocalDate.of(2023, Month.MARCH, 15);
    private Converter converter;

    @Before
    public void setUp() {
        // Build ConverterOptions with explicit UTC zone / timezone.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(UTC_ZONE);
        options.setTimeZone(UTC_TZ);
        converter = new Converter(options);
    }

    @Test
    public void testToInstant() {
        Instant expected = ZonedDateTime.of(SAMPLE_DATE, LocalTime.MIDNIGHT, UTC_ZONE).toInstant();
        Instant actual = LocalDateConversions.toInstant(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        long expected = LocalDateConversions.toInstant(SAMPLE_DATE, converter).toEpochMilli();
        long actual = LocalDateConversions.toLong(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime expected = LocalDateTime.of(SAMPLE_DATE, LocalTime.MIDNIGHT);
        LocalDateTime actual = LocalDateConversions.toLocalDateTime(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime expected = ZonedDateTime.of(SAMPLE_DATE, LocalTime.MIDNIGHT, UTC_ZONE);
        ZonedDateTime actual = LocalDateConversions.toZonedDateTime(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime_Utc() {
        // UTC offset is zero seconds.
        OffsetDateTime expected = OffsetDateTime.of(SAMPLE_DATE, LocalTime.MIDNIGHT, ZoneOffset.UTC);
        OffsetDateTime actual = LocalDateConversions.toOffsetDateTime(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDouble() {
        double expected = LocalDateConversions.toInstant(SAMPLE_DATE, converter).toEpochMilli() / 1000d;
        double actual = LocalDateConversions.toDouble(SAMPLE_DATE, converter);
        assertEquals(expected, actual, 0.000001);
    }

    @Test
    public void testToAtomicLong() {
        long expected = LocalDateConversions.toLong(SAMPLE_DATE, converter);
        AtomicLong actual = LocalDateConversions.toAtomicLong(SAMPLE_DATE, converter);
        assertEquals(expected, actual.get());
    }

    @Test
    public void testToTimestamp() {
        // Timestamp stores milliseconds since epoch at midnight UTC.
        long expectedMillis = SAMPLE_DATE.toEpochDay() * 86_400_000L;
        Timestamp expected = new Timestamp(expectedMillis);
        Timestamp actual = LocalDateConversions.toTimestamp(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        Calendar cal = LocalDateConversions.toCalendar(SAMPLE_DATE, converter);
        assertEquals(UTC_TZ, cal.getTimeZone());
        assertEquals(SAMPLE_DATE.getYear(), cal.get(Calendar.YEAR));
        assertEquals(SAMPLE_DATE.getMonthValue() - 1, cal.get(Calendar.MONTH)); // Calendar months are zero‑based
        assertEquals(SAMPLE_DATE.getDayOfMonth(), cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = java.sql.Date.valueOf(SAMPLE_DATE);
        java.sql.Date actual = LocalDateConversions.toSqlDate(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        Date expected = new Date(LocalDateConversions.toLong(SAMPLE_DATE, converter));
        Date actual = LocalDateConversions.toDate(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigInteger() {
        Instant instant = LocalDateConversions.toInstant(SAMPLE_DATE, converter);
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        BigInteger actual = LocalDateConversions.toBigInteger(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = LocalDateConversions.toInstant(SAMPLE_DATE, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(instant, converter);
        BigDecimal actual = LocalDateConversions.toBigDecimal(SAMPLE_DATE, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToYear() {
        Year expected = Year.from(SAMPLE_DATE);
        Year actual = LocalDateConversions.toYear(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToYearMonth() {
        YearMonth expected = YearMonth.from(SAMPLE_DATE);
        YearMonth actual = LocalDateConversions.toYearMonth(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMonthDay() {
        MonthDay expected = MonthDay.from(SAMPLE_DATE);
        MonthDay actual = LocalDateConversions.toMonthDay(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToString() {
        String expected = SAMPLE_DATE.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String actual = LocalDateConversions.toString(SAMPLE_DATE, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = LocalDateConversions.toMap(SAMPLE_DATE, converter);
        assertTrue(map instanceof LinkedHashMap);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.LOCAL_DATE));
        assertEquals(SAMPLE_DATE.toString(), map.get(MapConversions.LOCAL_DATE));
    }

    @Test(expected = NullPointerException.class)
    public void testNullInputThrowsException() {
        // All conversion methods cast the input to LocalDate without null‑check.
        LocalDateConversions.toInstant(null, converter);
    }
}
