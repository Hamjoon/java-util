package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.DateUtilities;
import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link CalendarConversions}.
 *
 * The tests exercise every public static conversion method, checking
 * correct values, proper handling of time‑zones, and that mutable objects
 * are cloned rather than returned directly.
 *
 * NOTE: The test suite relies only on classes, constructors and methods
 * that appear in the source code of {@code CalendarConversions} or in the
 * supplied dependency signatures.
 */
public class CalendarConversionsTest {

    private static final long FIXED_EPOCH_MILLIS = 1615734566123L; // 2021‑03‑14T15:09:26.123Z
    private Calendar utcCalendar;
    private Converter utcConverter;

    @Before
    public void setUp() {
        // Calendar in UTC with the fixed instant
        utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCalendar.setTimeInMillis(FIXED_EPOCH_MILLIS);

        // Converter with default options (assumed to use UTC)
        ConverterOptions options = new ConverterOptions();          // assumes a public no‑arg ctor
        utcConverter = new Converter(options);
    }

    @Test
    public void testToLong() {
        Long result = CalendarConversions.toLong(utcCalendar, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, result.longValue());
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong result = CalendarConversions.toAtomicLong(utcCalendar, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, result.get());
    }

    @Test
    public void testToDouble() {
        double result = CalendarConversions.toDouble(utcCalendar, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS / 1000.0, result, 0.000000001);
    }

    @Test
    public void testToBigDecimal() {
        // Expected: epochMillis / BigDecimalConversions.GRAND
        BigDecimal expected = new BigDecimal(FIXED_EPOCH_MILLIS)
                .divide(BigDecimalConversions.GRAND);
        BigDecimal result = CalendarConversions.toBigDecimal(utcCalendar, utcConverter);
        assertEquals(0, expected.compareTo(result));
    }

    @Test
    public void testToBigInteger() {
        BigInteger expected = BigInteger.valueOf(FIXED_EPOCH_MILLIS * 1_000_000L);
        BigInteger result = CalendarConversions.toBigInteger(utcCalendar, utcConverter);
        assertEquals(expected, result);
    }

    @Test
    public void testToDate() {
        Date result = CalendarConversions.toDate(utcCalendar, utcConverter);
        assertEquals(new Date(FIXED_EPOCH_MILLIS), result);
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date result = CalendarConversions.toSqlDate(utcCalendar, utcConverter);
        // The SQL date should represent the UTC date portion (2021‑03‑14)
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2021, 3, 14)), result);
    }

    @Test
    public void testToTimestamp() {
        Timestamp result = CalendarConversions.toTimestamp(utcCalendar, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, result.getTime());
        assertEquals(123, result.getNanos() / 1_000_000); // milliseconds part becomes nanoseconds
    }

    @Test
    public void testToInstant() {
        Instant result = CalendarConversions.toInstant(utcCalendar, utcConverter);
        assertEquals(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), result);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime result = CalendarConversions.toZonedDateTime(utcCalendar, utcConverter);
        assertEquals(ZoneId.of("UTC"), result.getZone());
        assertEquals(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), result.toInstant());
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime result = CalendarConversions.toLocalDateTime(utcCalendar, utcConverter);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), ZoneOffset.UTC);
        assertEquals(expected, result);
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime result = CalendarConversions.toOffsetDateTime(utcCalendar, utcConverter);
        OffsetDateTime expected = OffsetDateTime.ofInstant(Instant.ofEpochMilli(FIXED_EPOCH_MILLIS), ZoneOffset.UTC);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalDate() {
        LocalDate result = CalendarConversions.toLocalDate(utcCalendar, utcConverter);
        LocalDate expected = LocalDate.of(2021, 3, 14);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalTime() {
        LocalTime result = CalendarConversions.toLocalTime(utcCalendar, utcConverter);
        LocalTime expected = LocalTime.of(15, 9, 26, 123_000_000);
        assertEquals(expected, result);
    }

    @Test
    public void testClone() {
        Calendar clone = CalendarConversions.clone(utcCalendar, utcConverter);
        assertNotSame(utcCalendar, clone);
        assertEquals(utcCalendar.getTimeInMillis(), clone.getTimeInMillis());
        // Mutate original to ensure clone is independent
        utcCalendar.setTimeInMillis(FIXED_EPOCH_MILLIS + 1);
        assertNotEquals(utcCalendar.getTimeInMillis(), clone.getTimeInMillis());
    }

    @Test
    public void testCreate() {
        Calendar created = CalendarConversions.create(FIXED_EPOCH_MILLIS, utcConverter);
        assertEquals(FIXED_EPOCH_MILLIS, created.getTimeInMillis());
        // The timezone of the created calendar should match the converter's timezone
        assertEquals(utcConverter.getOptions().getTimeZone(), created.getTimeZone());
    }

    @Test
    public void testToYear() {
        Year result = CalendarConversions.toYear(utcCalendar, utcConverter);
        assertEquals(Year.of(2021), result);
    }

    @Test
    public void testToYearMonth() {
        YearMonth result = CalendarConversions.toYearMonth(utcCalendar, utcConverter);
        assertEquals(YearMonth.of(2021, 3), result);
    }

    @Test
    public void testToMonthDay() {
        MonthDay result = CalendarConversions.toMonthDay(utcCalendar, utcConverter);
        assertEquals(MonthDay.of(3, 14), result);
    }

    @Test
    public void testToString_WithOffsetZone() {
        // Calendar already in UTC (ZoneOffset)
        String formatted = CalendarConversions.toString(utcCalendar, utcConverter);
        // Expected pattern: 2021-03-14T15:09:26.123Z (or +00:00)
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3,9}Z"));
    }

    @Test
    public void testToString_WithNamedZone() {
        // Use a named zone (America/New_York) to trigger the second formatter branch
        Calendar nyCalendar = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        nyCalendar.setTimeInMillis(FIXED_EPOCH_MILLIS);
        String formatted = CalendarConversions.toString(nyCalendar, utcConverter);
        // Expected pattern includes zone ID in brackets, e.g. ...-05:00[America/New_York]
        assertTrue(formatted.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3,9}[+-]\\d{2}:\\d{2}\\[America/New_York\\]"));
    }

    @Test
    public void testToString_WithAbbreviationMapping() {
        // "EST" is a known abbreviation; the utilities map should translate it to a full zone.
        Calendar abbrevCal = Calendar.getInstance(TimeZone.getTimeZone("EST"));
        abbrevCal.setTimeInMillis(FIXED_EPOCH_MILLIS);
        String formatted = CalendarConversions.toString(abbrevCal, utcConverter);
        // After mapping, the zone should be a full IANA ID (e.g., America/New_York)
        assertTrue(formatted.contains("[America/New_York]") ||
                   formatted.contains("[US/Eastern]"));
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = CalendarConversions.toMap(utcCalendar, utcConverter);
        assertEquals(1, map.size());
        // The key is defined in MapConversions.CALENDAR – we cannot reference it directly,
        // but we can assert that the sole entry's value matches toString().
        Object value = map.values().iterator().next();
        assertEquals(CalendarConversions.toString(utcCalendar, utcConverter), value);
    }
}
