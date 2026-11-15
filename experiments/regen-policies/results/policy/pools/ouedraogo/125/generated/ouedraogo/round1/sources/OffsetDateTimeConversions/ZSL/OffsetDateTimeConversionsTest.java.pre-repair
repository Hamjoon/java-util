package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 tests for {@link OffsetDateTimeConversions}.
 *
 * The tests cover every static conversion method, exercising typical
 * values as well as edge‑case values (e.g., negative offsets, nanosecond
 * precision, different time‑zones).  The {@link Converter} instance is
 * created with default {@link ConverterOptions} – the only requirement
 * for the tested methods is that the options provide a {@link TimeZone}
 * and a {@link ZoneId}.  The default constructor of {@code ConverterOptions}
 * supplies sensible defaults (UTC) which are sufficient for the tests.
 */
public class OffsetDateTimeConversionsTest {

    private Converter converter;
    private OffsetDateTime sampleOffsetDateTime;
    private Instant sampleInstant;

    @Before
    public void setUp() {
        // Use default options (UTC) – sufficient for all conversion methods.
        converter = new Converter(new ConverterOptions());

        // Sample OffsetDateTime with nanosecond precision and a non‑UTC offset.
        sampleOffsetDateTime = OffsetDateTime.of(
                LocalDateTime.of(2022, Month.MARCH, 15, 10, 20, 30, 123_456_789),
                ZoneOffset.ofHoursMinutes(5, 30)); // +05:30 offset
        sampleInstant = sampleOffsetDateTime.toInstant();
    }

    @Test
    public void testToInstant() {
        Instant result = OffsetDateTimeConversions.toInstant(sampleOffsetDateTime, converter);
        assertEquals(sampleInstant, result);
    }

    @Test
    public void testToLong() {
        long expected = sampleInstant.toEpochMilli();
        long result = OffsetDateTimeConversions.toLong(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime expected = sampleOffsetDateTime.atZoneSameInstant(converter.getOptions().getZoneId()).toLocalDateTime();
        LocalDateTime result = OffsetDateTimeConversions.toLocalDateTime(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalDate() {
        LocalDate expected = sampleOffsetDateTime.atZoneSameInstant(converter.getOptions().getZoneId()).toLocalDate();
        LocalDate result = OffsetDateTimeConversions.toLocalDate(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalTime() {
        LocalTime expected = sampleOffsetDateTime.atZoneSameInstant(converter.getOptions().getZoneId()).toLocalTime();
        LocalTime result = OffsetDateTimeConversions.toLocalTime(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToAtomicLong() {
        long expected = sampleInstant.toEpochMilli();
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(sampleOffsetDateTime, converter);
        assertEquals(expected, result.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(sampleInstant);
        Timestamp result = OffsetDateTimeConversions.toTimestamp(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToCalendar() {
        Calendar calendar = OffsetDateTimeConversions.toCalendar(sampleOffsetDateTime, converter);
        assertEquals(converter.getOptions().getTimeZone(), calendar.getTimeZone());
        assertEquals(sampleInstant.toEpochMilli(), calendar.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = new java.sql.Date(sampleInstant.toEpochMilli());
        java.sql.Date result = OffsetDateTimeConversions.toSqlDate(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime expected = sampleInstant.atZone(converter.getOptions().getZoneId());
        ZonedDateTime result = OffsetDateTimeConversions.toZonedDateTime(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToDate() {
        Date expected = new Date(sampleInstant.toEpochMilli());
        Date result = OffsetDateTimeConversions.toDate(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToBigInteger() {
        // Expected: epoch milliseconds as a BigInteger (the behaviour of InstantConversions.toBigInteger)
        BigInteger expected = BigInteger.valueOf(sampleInstant.toEpochMilli());
        BigInteger result = OffsetDateTimeConversions.toBigInteger(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToBigDecimal() {
        // Expected: seconds with fractional nanos expressed as a decimal.
        BigDecimal seconds = BigDecimal.valueOf(sampleInstant.getEpochSecond());
        BigDecimal nanosFraction = BigDecimal.valueOf(sampleInstant.getNano(), 9); // divide by 1_000_000_000
        BigDecimal expected = seconds.add(nanosFraction);
        BigDecimal result = OffsetDateTimeConversions.toBigDecimal(sampleOffsetDateTime, converter);
        assertEquals(0, expected.compareTo(result));
    }

    @Test
    public void testToOffsetTime() {
        OffsetTime expected = sampleOffsetDateTime.toOffsetTime();
        OffsetTime result = OffsetDateTimeConversions.toOffsetTime(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToString() {
        String expected = sampleOffsetDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String result = OffsetDateTimeConversions.toString(sampleOffsetDateTime, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = OffsetDateTimeConversions.toMap(sampleOffsetDateTime, converter);
        assertNotNull(map);
        assertEquals(3, map.size());

        ZonedDateTime zdt = sampleInstant.atZone(converter.getOptions().getZoneId());

        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(zdt.getOffset().toString(), map.get(MapConversions.OFFSET));
    }

    @Test
    public void testToDouble() {
        // Expected double = seconds + nanos/1_000_000_000
        double expected = sampleInstant.getEpochSecond() + sampleInstant.getNano() / 1_000_000_000.0;
        double result = OffsetDateTimeConversions.toDouble(sampleOffsetDateTime, converter);
        assertEquals(expected, result, 1e-12);
    }

    /**
     * Additional edge‑case test: an OffsetDateTime with a negative offset and
     * zero nanoseconds to ensure the conversion logic works across the full
     * range of possible offsets.
     */
    @Test
    public void testNegativeOffsetEdgeCase() {
        OffsetDateTime negativeOffset = OffsetDateTime.of(
                LocalDateTime.of(1970, 1, 1, 0, 0, 0, 0),
                ZoneOffset.ofHours(-12)); // -12:00 offset

        // Verify a few conversions manually.
        assertEquals(Instant.EPOCH.plusSeconds(12 * 3600), OffsetDateTimeConversions.toInstant(negativeOffset, converter));
        assertEquals(12L * 3600_000, OffsetDateTimeConversions.toLong(negativeOffset, converter));

        // toMap must reflect the correct offset string.
        Map<String, Object> map = OffsetDateTimeConversions.toMap(negativeOffset, converter);
        assertEquals("-12:00", map.get(MapConversions.OFFSET));
    }
}
