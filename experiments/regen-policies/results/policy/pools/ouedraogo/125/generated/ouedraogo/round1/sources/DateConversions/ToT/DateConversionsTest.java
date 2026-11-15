package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

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

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link DateConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Normal conversions (typical values)</li>
 *   <li>Edge cases (null, wrong type)</li>
 *   <li>Special handling for {@link java.sql.Date}</li>
 *   <li>String formatting and map representation</li>
 * </ul>
 * No mocking is used – all objects are created via public constructors or factory
 * methods that appear in the source code or the supplied dependency signatures.
 */
public class DateConversionsTest
{
    private Converter converter;
    private ZoneId zoneId;
    private Date sampleDate;                // 2021‑03‑14T15:09:26.535Z (epoch millis 1615734566535)
    private java.sql.Date sampleSqlDate;    // same instant as sampleDate, but java.sql.Date strips time‑of‑day

    @Before
    public void setUp()
    {
        // ConverterOptions is part of the library; a default instance is sufficient.
        // The default zone is used for expectations (usually the system default).
        // The test does not rely on a particular zone – it derives the expected values
        // from the zone supplied by the Converter itself.
        converter = new Converter(new ConverterOptions());
        zoneId = converter.getOptions().getZoneId();

        // Fixed instant for reproducible results.
        Instant instant = Instant.ofEpochMilli(1_615_734_566_535L); // 2021‑03‑14T15:09:26.535Z
        sampleDate = Date.from(instant);
        sampleSqlDate = new java.sql.Date(instant.toEpochMilli());
    }

    /*-----------------------------------------------------------------------
     *  toZonedDateTime
     *-----------------------------------------------------------------------*/
    @Test
    public void testToZonedDateTime_Typical()
    {
        ZonedDateTime zdt = DateConversions.toZonedDateTime(sampleDate, converter);
        assertEquals(zoneId, zdt.getZone());
        assertEquals(sampleDate.getTime(), zdt.toInstant().toEpochMilli());
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_Null()
    {
        DateConversions.toZonedDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongType()
    {
        DateConversions.toZonedDateTime("not a date", converter);
    }

    /*-----------------------------------------------------------------------
     *  toLong
     *-----------------------------------------------------------------------*/
    @Test
    public void testToLong()
    {
        long epoch = DateConversions.toLong(sampleDate, converter);
        assertEquals(sampleDate.getTime(), epoch);
    }

    /*-----------------------------------------------------------------------
     *  toDouble
     *-----------------------------------------------------------------------*/
    @Test
    public void testToDouble()
    {
        double seconds = DateConversions.toDouble(sampleDate, converter);
        assertEquals(sampleDate.getTime() / 1000.0, seconds, 0.000001);
    }

    /*-----------------------------------------------------------------------
     *  toSqlDate
     *-----------------------------------------------------------------------*/
    @Test
    public void testToSqlDate()
    {
        java.sql.Date sqlDate = DateConversions.toSqlDate(sampleDate, converter);
        assertEquals(sampleDate.getTime(), sqlDate.getTime());
    }

    /*-----------------------------------------------------------------------
     *  toDate
     *-----------------------------------------------------------------------*/
    @Test
    public void testToDate()
    {
        Date newDate = DateConversions.toDate(sampleDate, converter);
        assertEquals(sampleDate.getTime(), newDate.getTime());
        assertNotSame(sampleDate, newDate);
    }

    /*-----------------------------------------------------------------------
     *  toTimestamp
     *-----------------------------------------------------------------------*/
    @Test
    public void testToTimestamp()
    {
        Timestamp ts = DateConversions.toTimestamp(sampleDate, converter);
        assertEquals(sampleDate.getTime(), ts.getTime());
    }

    /*-----------------------------------------------------------------------
     *  toCalendar
     *-----------------------------------------------------------------------*/
    @Test
    public void testToCalendar()
    {
        Calendar cal = DateConversions.toCalendar(sampleDate, converter);
        assertEquals(sampleDate.getTime(), cal.getTimeInMillis());
    }

    /*-----------------------------------------------------------------------
     *  toBigDecimal
     *-----------------------------------------------------------------------*/
    @Test
    public void testToBigDecimal()
    {
        // The library divides epoch‑millis by 1_000_000_000 (the constant
        // BigDecimalConversions.GRAND).  We reproduce the same calculation.
        BigDecimal expected = new BigDecimal(sampleDate.getTime())
                .divide(new BigDecimal("1000000000"), 9, RoundingMode.DOWN);
        BigDecimal actual = DateConversions.toBigDecimal(sampleDate, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    /*-----------------------------------------------------------------------
     *  toInstant (regular Date)
     *-----------------------------------------------------------------------*/
    @Test
    public void testToInstant_FromDate()
    {
        Instant instant = DateConversions.toInstant(sampleDate, converter);
        assertEquals(sampleDate.toInstant(), instant);
    }

    /*-----------------------------------------------------------------------
     *  toInstant (java.sql.Date)
     *-----------------------------------------------------------------------*/
    @Test
    public void testToInstant_FromSqlDate()
    {
        Instant instant = DateConversions.toInstant(sampleSqlDate, converter);
        // java.sql.Date loses the time‑of‑day, but DateConversions creates a new java.util.Date
        // with the same epoch millis, so the instant must be identical.
        assertEquals(Instant.ofEpochMilli(sampleSqlDate.getTime()), instant);
    }

    /*-----------------------------------------------------------------------
     *  toOffsetDateTime
     *-----------------------------------------------------------------------*/
    @Test
    public void testToOffsetDateTime()
    {
        OffsetDateTime odt = DateConversions.toOffsetDateTime(sampleDate, converter);
        assertEquals(DateConversions.toInstant(sampleDate, converter), odt.toInstant());
        assertEquals(zoneId, odt.getOffset().getRules().getOffset(odt.toInstant()));
    }

    /*-----------------------------------------------------------------------
     *  toLocalDateTime
     *-----------------------------------------------------------------------*/
    @Test
    public void testToLocalDateTime()
    {
        LocalDateTime ldt = DateConversions.toLocalDateTime(sampleDate, converter);
        ZonedDateTime expected = DateConversions.toZonedDateTime(sampleDate, converter);
        assertEquals(expected.toLocalDateTime(), ldt);
    }

    /*-----------------------------------------------------------------------
     *  toLocalDate
     *-----------------------------------------------------------------------*/
    @Test
    public void testToLocalDate()
    {
        LocalDate ld = DateConversions.toLocalDate(sampleDate, converter);
        assertEquals(DateConversions.toZonedDateTime(sampleDate, converter).toLocalDate(), ld);
    }

    /*-----------------------------------------------------------------------
     *  toLocalTime
     *-----------------------------------------------------------------------*/
    @Test
    public void testToLocalTime()
    {
        LocalTime lt = DateConversions.toLocalTime(sampleDate, converter);
        ZonedDateTime zdt = DateConversions.toZonedDateTime(sampleDate, converter);
        assertEquals(zdt.toLocalTime(), lt);
    }

    /*-----------------------------------------------------------------------
     *  toBigInteger
     *-----------------------------------------------------------------------*/
    @Test
    public void testToBigInteger()
    {
        // The library delegates to InstantConversions.toBigInteger.
        // We verify that the resulting value represents the same instant as the original date.
        BigInteger bi = DateConversions.toBigInteger(sampleDate, converter);
        // Convert back to Instant for comparison.
        Instant instant = Instant.ofEpochMilli(sampleDate.getTime());
        BigInteger expected = new BigInteger(Long.toString(instant.getEpochSecond()));
        // The exact scaling performed by InstantConversions is not public, but the value
        // must be non‑null and correspond to the epoch seconds (or a deterministic transformation).
        assertNotNull(bi);
        assertEquals(0, expected.compareTo(bi));
    }

    /*-----------------------------------------------------------------------
     *  toAtomicLong
     *-----------------------------------------------------------------------*/
    @Test
    public void testToAtomicLong()
    {
        AtomicLong al = DateConversions.toAtomicLong(sampleDate, converter);
        assertEquals(sampleDate.getTime(), al.get());
    }

    /*-----------------------------------------------------------------------
     *  sqlDateToString
     *-----------------------------------------------------------------------*/
    @Test
    public void testSqlDateToString()
    {
        String fromMethod = DateConversions.sqlDateToString(sampleSqlDate, converter);
        String fromToString = DateConversions.toString(new Date(sampleSqlDate.getTime()), converter);
        assertEquals(fromToString, fromMethod);
    }

    /*-----------------------------------------------------------------------
     *  toString
     *-----------------------------------------------------------------------*/
    @Test
    public void testToString_Format()
    {
        String formatted = DateConversions.toString(sampleDate, converter);
        // Build the same formatter that the implementation uses.
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendOffset("+HH:MM", "Z")
                .toFormatter()
                .withZone(zoneId);
        String expected = formatter.format(sampleDate.toInstant());
        assertEquals(expected, formatted);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_Null()
    {
        DateConversions.toString(null, converter);
    }

    /*-----------------------------------------------------------------------
     *  toMap
     *-----------------------------------------------------------------------*/
    @Test
    public void testToMap()
    {
        Map<String, Object> map = DateConversions.toMap(sampleDate, converter);
        // Verify keys (the constants are defined in MapConversions)
        assertTrue(map.containsKey(MapConversions.DATE));
        assertTrue(map.containsKey(MapConversions.TIME));
        assertTrue(map.containsKey(MapConversions.ZONE));
        assertTrue(map.containsKey(MapConversions.EPOCH_MILLIS));

        // Verify values
        ZonedDateTime zdt = DateConversions.toZonedDateTime(sampleDate, converter);
        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(zoneId.toString(), map.get(MapConversions.ZONE));
        assertEquals(sampleDate.getTime(), map.get(MapConversions.EPOCH_MILLIS));
    }

    /*-----------------------------------------------------------------------
     *  Edge cases – null and wrong type for every public static method
     *-----------------------------------------------------------------------*/
    @Test
    public void testAllMethods_NullInput()
    {
        // Helper array of method references (via reflection) would be ideal,
        // but we stay simple and just check a representative subset.
        try
        {
            DateConversions.toLong(null, converter);
            fail("Expected NullPointerException");
        }
        catch (NullPointerException ignored) {}

        try
        {
            DateConversions.toDouble(null, converter);
            fail("Expected NullPointerException");
        }
        catch (NullPointerException ignored) {}

        try
        {
            DateConversions.toSqlDate(null, converter);
            fail("Expected NullPointerException");
        }
        catch (NullPointerException ignored) {}
    }

    @Test
    public void testAllMethods_WrongType()
    {
        Object notADate = Integer.valueOf(42);
        try
        {
            DateConversions.toLong(notADate, converter);
            fail("Expected ClassCastException");
        }
        catch (ClassCastException ignored) {}

        try
        {
            DateConversions.toZonedDateTime(notADate, converter);
            fail("Expected ClassCastException");
        }
        catch (ClassCastException ignored) {}
    }
}
