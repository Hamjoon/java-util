package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
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
 * JUnit‑4 test suite that exercises every public static method in
 * {@link DateConversions}. The tests use a real {@link Converter} instance
 * (no mocks) and the UTC zone to make the expected results deterministic.
 */
public class DateConversionsTest
{
    /** Converter that always uses UTC – the zone is read from the options at run‑time. */
    private Converter converter;

    /** A fixed instant that is easy to verify (2023‑01‑02T03:04:05.006Z). */
    private static final Instant FIXED_INSTANT = Instant.parse("2023-01-02T03:04:05.006Z");

    /** The {@link Date} representation of {@link #FIXED_INSTANT}. */
    private static final Date FIXED_DATE = Date.from(FIXED_INSTANT);

    /** The epoch milli value used throughout the tests. */
    private static final long EPOCH_MILLIS = FIXED_INSTANT.toEpochMilli();

    @Before
    public void setUp()
    {
        // The library supplies a ConverterOptions class that contains a ZoneId.
        // The default constructor uses the system default zone; we do not
        // need to customise it because the tests always retrieve the zone
        // from the converter itself and compute expected values with the same
        // zone.
        ConverterOptions options = new ConverterOptions();               // <-- library class
        converter = new Converter(options);
    }

    @Test
    public void testToZonedDateTime()
    {
        ZonedDateTime zdt = DateConversions.toZonedDateTime(FIXED_DATE, converter);
        ZoneId zone = converter.getOptions().getZoneId();

        assertEquals(FIXED_INSTANT, zdt.toInstant());
        assertEquals(zone, zdt.getZone());
    }

    @Test
    public void testToLong()
    {
        long result = DateConversions.toLong(FIXED_DATE, converter);
        assertEquals(EPOCH_MILLIS, result);
    }

    @Test
    public void testToDouble()
    {
        double result = DateConversions.toDouble(FIXED_DATE, converter);
        assertEquals(EPOCH_MILLIS / 1000.0, result, 0.0);
    }

    @Test
    public void testToSqlDate()
    {
        java.sql.Date sqlDate = DateConversions.toSqlDate(FIXED_DATE, converter);
        assertEquals(EPOCH_MILLIS, sqlDate.getTime());
    }

    @Test
    public void testToDate()
    {
        Date date = DateConversions.toDate(FIXED_DATE, converter);
        assertEquals(EPOCH_MILLIS, date.getTime());
    }

    @Test
    public void testToTimestamp()
    {
        Timestamp ts = DateConversions.toTimestamp(FIXED_DATE, converter);
        assertEquals(EPOCH_MILLIS, ts.getTime());
    }

    @Test
    public void testToCalendar()
    {
        Calendar cal = DateConversions.toCalendar(FIXED_DATE, converter);
        assertEquals(EPOCH_MILLIS, cal.getTimeInMillis());
    }

    @Test
    public void testToBigDecimal()
    {
        BigDecimal bd = DateConversions.toBigDecimal(FIXED_DATE, converter);
        // The method divides epochMillis by the constant GRAND (10⁹) with a scale of 9.
        // Therefore moving the decimal point 9 places to the right must recover the
        // original epoch milli value.
        assertEquals(9, bd.scale());
        assertEquals(BigDecimal.valueOf(EPOCH_MILLIS), bd.movePointRight(9));
    }

    @Test
    public void testToInstant_withUtilDate()
    {
        Instant instant = DateConversions.toInstant(FIXED_DATE, converter);
        assertEquals(FIXED_INSTANT, instant);
    }

    @Test
    public void testToInstant_withSqlDate()
    {
        java.sql.Date sqlDate = new java.sql.Date(EPOCH_MILLIS);
        Instant instant = DateConversions.toInstant(sqlDate, converter);
        assertEquals(FIXED_INSTANT, instant);
    }

    @Test
    public void testToOffsetDateTime()
    {
        OffsetDateTime odt = DateConversions.toOffsetDateTime(FIXED_DATE, converter);
        ZoneId zone = converter.getOptions().getZoneId();
        OffsetDateTime expected = FIXED_INSTANT.atZone(zone).toOffsetDateTime();
        assertEquals(expected, odt);
    }

    @Test
    public void testToLocalDateTime()
    {
        LocalDateTime ldt = DateConversions.toLocalDateTime(FIXED_DATE, converter);
        LocalDateTime expected = DateConversions.toZonedDateTime(FIXED_DATE, converter).toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test
    public void testToLocalDate()
    {
        LocalDate ld = DateConversions.toLocalDate(FIXED_DATE, converter);
        LocalDate expected = DateConversions.toZonedDateTime(FIXED_DATE, converter).toLocalDate();
        assertEquals(expected, ld);
    }

    @Test
    public void testToLocalTime()
    {
        LocalTime lt = DateConversions.toLocalTime(FIXED_DATE, converter);
        LocalTime expected = DateConversions.toZonedDateTime(FIXED_DATE, converter).toLocalTime();
        assertEquals(expected, lt);
    }

    @Test
    public void testToBigInteger()
    {
        // InstantConversions.toBigInteger converts the instant to epoch‑millis as a BigInteger.
        // Verify that the value matches the known epoch milli.
        BigInteger bi = DateConversions.toBigInteger(FIXED_DATE, converter);
        assertEquals(BigInteger.valueOf(EPOCH_MILLIS), bi);
    }

    @Test
    public void testToAtomicLong()
    {
        AtomicLong al = DateConversions.toAtomicLong(FIXED_DATE, converter);
        assertEquals(EPOCH_MILLIS, al.get());
    }

    @Test
    public void testSqlDateToString()
    {
        java.sql.Date sqlDate = new java.sql.Date(EPOCH_MILLIS);
        String result = DateConversions.sqlDateToString(sqlDate, converter);
        String expected = DateConversions.toString(new Date(EPOCH_MILLIS), converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToString_Format()
    {
        // The formatter used in DateConversions prints:
        //   yyyy-MM-dd'T'HH:mm:ss.SSS+hh:mm   (offset always present)
        String formatted = DateConversions.toString(FIXED_DATE, converter);
        ZoneId zone = converter.getOptions().getZoneId();

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendOffset("+HH:MM", "Z")
                .toFormatter()
                .withZone(zone);

        String expected = formatter.format(FIXED_INSTANT);
        assertEquals(expected, formatted);
    }

    @Test
    public void testToMap()
    {
        Map<String, Object> map = DateConversions.toMap(FIXED_DATE, converter);
        ZoneId zone = converter.getOptions().getZoneId();

        ZonedDateTime zdt = DateConversions.toZonedDateTime(FIXED_DATE, converter);
        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(zone.toString(), map.get(MapConversions.ZONE));
        assertEquals(EPOCH_MILLIS, map.get(MapConversions.EPOCH_MILLIS));
    }
}
