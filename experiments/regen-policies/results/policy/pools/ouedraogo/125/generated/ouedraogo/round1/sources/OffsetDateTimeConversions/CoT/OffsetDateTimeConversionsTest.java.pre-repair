package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class OffsetDateTimeConversionsTest {

    private Converter converter;
    private OffsetDateTime sample;
    private OffsetDateTime maxOffset;
    private OffsetDateTime minOffset;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // Assume ConverterOptions has a public no‑arg constructor.
        converter = new Converter(new ConverterOptions());

        // A typical OffsetDateTime used for the majority of tests.
        sample = OffsetDateTime.of(
                2023, 5, 10,
                15, 30, 45, 123_456_789,
                ZoneOffset.ofHoursMinutes(2, 0));   // +02:00

        // Edge offsets: maximum (+18:00) and minimum (‑18:00) allowed by ZoneOffset.
        maxOffset = OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.MAX);
        minOffset = OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.MIN);
    }

    // -----------------------------------------------------------------
    // Basic happy‑path tests for each conversion method
    // -----------------------------------------------------------------

    @Test
    public void testToInstant() {
        Instant expected = sample.toInstant();
        assertEquals(expected, OffsetDateTimeConversions.toInstant(sample, converter));
    }

    @Test
    public void testToLong() {
        long expected = sample.toInstant().toEpochMilli();
        assertEquals(expected, OffsetDateTimeConversions.toLong(sample, converter));
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime expected = sample.toInstant()
                .atZone(converter.getOptions().getZoneId())
                .toLocalDateTime();
        assertEquals(expected, OffsetDateTimeConversions.toLocalDateTime(sample, converter));
    }

    @Test
    public void testToLocalDate() {
        LocalDate expected = sample.toInstant()
                .atZone(converter.getOptions().getZoneId())
                .toLocalDate();
        assertEquals(expected, OffsetDateTimeConversions.toLocalDate(sample, converter));
    }

    @Test
    public void testToLocalTime() {
        LocalTime expected = sample.toInstant()
                .atZone(converter.getOptions().getZoneId())
                .toLocalTime();
        assertEquals(expected, OffsetDateTimeConversions.toLocalTime(sample, converter));
    }

    @Test
    public void testToAtomicLong() {
        long expected = sample.toInstant().toEpochMilli();
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(sample, converter);
        assertEquals(expected, result.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(sample.toInstant());
        assertEquals(expected, OffsetDateTimeConversions.toTimestamp(sample, converter));
    }

    @Test
    public void testToCalendar() {
        Calendar cal = OffsetDateTimeConversions.toCalendar(sample, converter);
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone());
        assertEquals(sample.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = new java.sql.Date(sample.toInstant().toEpochMilli());
        assertEquals(expected, OffsetDateTimeConversions.toSqlDate(sample, converter));
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime expected = sample.toInstant().atZone(converter.getOptions().getZoneId());
        assertEquals(expected, OffsetDateTimeConversions.toZonedDateTime(sample, converter));
    }

    @Test
    public void testToDate() {
        Date expected = new Date(sample.toInstant().toEpochMilli());
        assertEquals(expected, OffsetDateTimeConversions.toDate(sample, converter));
    }

    @Test
    public void testToBigInteger() {
        Instant instant = sample.toInstant();
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, OffsetDateTimeConversions.toBigInteger(sample, converter));
    }

    @Test
    public void testToBigDecimal() {
        Instant instant = sample.toInstant();
        BigDecimal expected = InstantConversions.toBigDecimal(instant, converter);
        assertEquals(0, expected.compareTo(OffsetDateTimeConversions.toBigDecimal(sample, converter)));
    }

    @Test
    public void testToOffsetTime() {
        OffsetTime expected = sample.toOffsetTime();
        assertEquals(expected, OffsetDateTimeConversions.toOffsetTime(sample, converter));
    }

    @Test
    public void testToString() {
        String expected = sample.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(expected, OffsetDateTimeConversions.toString(sample, converter));
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = OffsetDateTimeConversions.toMap(sample, converter);
        ZonedDateTime zdt = sample.toInstant().atZone(converter.getOptions().getZoneId());

        assertEquals(3, map.size());
        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(zdt.getOffset().toString(), map.get(MapConversions.OFFSET));
    }

    @Test
    public void testToDouble() {
        Instant instant = sample.toInstant();
        double expected = BigDecimalConversions.secondsAndNanosToDouble(
                instant.getEpochSecond(),
                instant.getNano()).doubleValue();
        assertEquals(expected, OffsetDateTimeConversions.toDouble(sample, converter), 0.0);
    }

    // -----------------------------------------------------------------
    // Edge‑case and exception tests
    // -----------------------------------------------------------------

    @Test
    public void testNullFromObjectThrowsNPE() {
        thrown.expect(NullPointerException.class);
        OffsetDateTimeConversions.toInstant(null, converter);
    }

    @Test
    public void testNullConverterThrowsNPE() {
        thrown.expect(NullPointerException.class);
        OffsetDateTimeConversions.toInstant(sample, null);
    }

    @Test
    public void testInvalidSourceTypeThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        OffsetDateTimeConversions.toInstant("not an OffsetDateTime", converter);
    }

    @Test
    public void testMaxOffsetConversion() {
        // Verify that extreme positive offset does not break any conversion
        Instant instant = maxOffset.toInstant();
        long epochMilli = instant.toEpochMilli();

        assertEquals(instant, OffsetDateTimeConversions.toInstant(maxOffset, converter));
        assertEquals(epochMilli, OffsetDateTimeConversions.toLong(maxOffset, converter));
        assertEquals(OffsetTime.MAX, OffsetDateTimeConversions.toOffsetTime(maxOffset, converter).getOffset());
    }

    @Test
    public void testMinOffsetConversion() {
        // Verify that extreme negative offset does not break any conversion
        Instant instant = minOffset.toInstant();
        long epochMilli = instant.toEpochMilli();

        assertEquals(instant, OffsetDateTimeConversions.toInstant(minOffset, converter));
        assertEquals(epochMilli, OffsetDateTimeConversions.toLong(minOffset, converter));
        assertEquals(OffsetTime.MIN, OffsetDateTimeConversions.toOffsetTime(minOffset, converter).getOffset());
    }

    @Test
    public void testToMapContainsCorrectKeys() {
        Map<String, Object> map = OffsetDateTimeConversions.toMap(sample, converter);
        assertTrue(map.containsKey(MapConversions.DATE));
        assertTrue(map.containsKey(MapConversions.TIME));
        assertTrue(map.containsKey(MapConversions.OFFSET));
    }

    @Test
    public void testToDoublePrecision() {
        // Using a value with nanosecond precision to ensure conversion keeps fractional part
        OffsetDateTime odt = OffsetDateTime.of(2020, 2, 29, 23, 59, 59, 999_999_999, ZoneOffset.UTC);
        double result = OffsetDateTimeConversions.toDouble(odt, converter);
        double expected = BigDecimalConversions.secondsAndNanosToDouble(
                odt.toInstant().getEpochSecond(),
                odt.toInstant().getNano()).doubleValue();
        assertEquals(expected, result, 0.0);
    }
}
