package com.cedarsoftware.util.convert;

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
 * Comprehensive JUnit‑4 test suite for {@link OffsetDateTimeConversions}.
 * <p>
 * The tests exercise every static conversion method using a single,
 * well‑known {@link OffsetDateTime} instance.  The expected results are
 * derived from the JDK time API (or from the small helper calculations
 * that mimic the behaviour of the library’s internal conversion utilities).
 * </p>
 */
public class OffsetDateTimeConversionsTest
{
    /** Sample offset date‑time used for all conversions. */
    private OffsetDateTime sampleOdt;

    /** Converter instance with default options (default time‑zone). */
    private Converter converter;

    /** Instant derived from {@link #sampleOdt}. */
    private Instant sampleInstant;

    /** ZoneId obtained from the converter options – used to build expected values. */
    private ZoneId expectedZoneId;

    /** TimeZone obtained from the converter options – used for Calendar tests. */
    private java.util.TimeZone expectedTimeZone;

    @Before
    public void setUp()
    {
        // 2022‑03‑15T10:20:30.123456789+05:00
        sampleOdt = OffsetDateTime.of(
                2022, 3, 15,
                10, 20, 30,
                123_456_789,               // nanoseconds
                ZoneOffset.ofHours(5));    // +05:00 offset

        // The library supplies a ConverterOptions class that contains a
        // default ZoneId/TimeZone.  We rely on the no‑arg constructor to
        // produce sensible defaults (system default zone).
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);

        expectedZoneId = options.getZoneId();
        expectedTimeZone = options.getTimeZone();

        sampleInstant = sampleOdt.toInstant();
    }

    @Test
    public void testToInstant()
    {
        Instant result = OffsetDateTimeConversions.toInstant(sampleOdt, converter);
        assertEquals(sampleInstant, result);
    }

    @Test
    public void testToLong()
    {
        long expected = sampleInstant.toEpochMilli();
        long result = OffsetDateTimeConversions.toLong(sampleOdt, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalDateTime()
    {
        LocalDateTime expected = sampleInstant.atZone(expectedZoneId).toLocalDateTime();
        LocalDateTime result = OffsetDateTimeConversions.toLocalDateTime(sampleOdt, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalDate()
    {
        LocalDate expected = sampleInstant.atZone(expectedZoneId).toLocalDate();
        LocalDate result = OffsetDateTimeConversions.toLocalDate(sampleOdt, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalTime()
    {
        LocalTime expected = sampleInstant.atZone(expectedZoneId).toLocalTime();
        LocalTime result = OffsetDateTimeConversions.toLocalTime(sampleOdt, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToAtomicLong()
    {
        long expected = sampleInstant.toEpochMilli();
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(sampleOdt, converter);
        assertEquals(expected, result.get());
    }

    @Test
    public void testToTimestamp()
    {
        Timestamp expected = Timestamp.from(sampleInstant);
        Timestamp result = OffsetDateTimeConversions.toTimestamp(sampleOdt, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToCalendar()
    {
        Calendar expected = Calendar.getInstance(expectedTimeZone);
        expected.setTimeInMillis(sampleInstant.toEpochMilli());

        Calendar result = OffsetDateTimeConversions.toCalendar(sampleOdt, converter);

        assertEquals(expected.getTimeInMillis(), result.getTimeInMillis());
        assertEquals(expected.getTimeZone(), result.getTimeZone());
    }

    @Test
    public void testToSqlDate()
    {
        java.sql.Date expected = new java.sql.Date(sampleInstant.toEpochMilli());
        java.sql.Date result = OffsetDateTimeConversions.toSqlDate(sampleOdt, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToZonedDateTime()
    {
        ZonedDateTime expected = sampleInstant.atZone(expectedZoneId);
        ZonedDateTime result = OffsetDateTimeConversions.toZonedDateTime(sampleOdt, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToDate()
    {
        Date expected = new Date(sampleInstant.toEpochMilli());
        Date result = OffsetDateTimeConversions.toDate(sampleOdt, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToBigInteger()
    {
        // The library’s InstantConversions.toBigInteger returns the epoch‑millis
        // as a BigInteger.  We assert the same behaviour here.
        BigInteger expected = BigInteger.valueOf(sampleInstant.toEpochMilli());
        BigInteger result = OffsetDateTimeConversions.toBigInteger(sampleOdt, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToBigDecimal()
    {
        // Expected value: seconds + nanos/1_000_000_000 as a decimal.
        BigDecimal expected = new BigDecimal(sampleInstant.getEpochSecond())
                .add(new BigDecimal(sampleInstant.getNano()).movePointLeft(9));
        BigDecimal result = OffsetDateTimeConversions.toBigDecimal(sampleOdt, converter);
        assertEquals(0, expected.compareTo(result));
    }

    @Test
    public void testToOffsetTime()
    {
        OffsetTime expected = sampleOdt.toOffsetTime();
        OffsetTime result = OffsetDateTimeConversions.toOffsetTime(sampleOdt, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToString()
    {
        String expected = sampleOdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String result = OffsetDateTimeConversions.toString(sampleOdt, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToMap()
    {
        Map<String, Object> result = OffsetDateTimeConversions.toMap(sampleOdt, converter);
        ZonedDateTime zdt = sampleInstant.atZone(expectedZoneId);

        assertEquals(3, result.size());
        assertEquals(zdt.toLocalDate().toString(), result.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), result.get(MapConversions.TIME));
        assertEquals(zdt.getOffset().toString(), result.get(MapConversions.OFFSET));
    }

    @Test
    public void testToDouble()
    {
        double expected = sampleInstant.getEpochSecond()
                + sampleInstant.getNano() / 1_000_000_000d;
        double result = OffsetDateTimeConversions.toDouble(sampleOdt, converter);
        // Use a tiny delta because of floating‑point rounding.
        assertEquals(expected, result, 1e-12);
    }
}
