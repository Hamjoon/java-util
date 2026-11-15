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

public class DateConversionsTest
{
    private static final long EPOCH_MILLIS = 1609459200123L; // 2021‑01‑01T00:00:00.123Z
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private Date date;
    private java.sql.Date sqlDate;
    private Converter utcConverter;
    private Converter nyConverter;

    @Before
    public void setUp()
    {
        date = new Date(EPOCH_MILLIS);
        sqlDate = new java.sql.Date(EPOCH_MILLIS);

        // Converter with UTC zone
        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setZoneId(UTC);
        utcConverter = new Converter(utcOptions);

        // Converter with New‑York zone (used for offset‑format edge‑case)
        ConverterOptions nyOptions = new ConverterOptions();
        nyOptions.setZoneId(NEW_YORK);
        nyConverter = new Converter(nyOptions);
    }

    /* ----------------------------------------------------------------------
     *  1.  List of package‑private static methods (they are visible because the
     *      test class lives in the same package)
     * ---------------------------------------------------------------------- */
    // ZonedDateTime toZonedDateTime(Object, Converter)
    // long          toLong(Object, Converter)
    // double        toDouble(Object, Converter)
    // java.sql.Date toSqlDate(Object, Converter)
    // Date          toDate(Object, Converter)
    // Timestamp     toTimestamp(Object, Converter)
    // Calendar      toCalendar(Object, Converter)
    // BigDecimal    toBigDecimal(Object, Converter)
    // Instant       toInstant(Object, Converter)
    // OffsetDateTime toOffsetDateTime(Object, Converter)
    // LocalDateTime toLocalDateTime(Object, Converter)
    // LocalDate     toLocalDate(Object, Converter)
    // LocalTime     toLocalTime(Object, Converter)
    // BigInteger    toBigInteger(Object, Converter)
    // AtomicLong    toAtomicLong(Object, Converter)
    // String        sqlDateToString(Object, Converter)
    // String        toString(Object, Converter)
    // Map<String,Object> toMap(Object, Converter)

    /* ----------------------------------------------------------------------
     *  2.  Basic functional tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToZonedDateTime()
    {
        ZonedDateTime zdt = DateConversions.toZonedDateTime(date, utcConverter);
        assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), zdt.toInstant());
        assertEquals(UTC, zdt.getZone());
    }

    @Test
    public void testToLong()
    {
        assertEquals(EPOCH_MILLIS, DateConversions.toLong(date, utcConverter));
    }

    @Test
    public void testToDouble()
    {
        double expected = EPOCH_MILLIS / 1000.0;
        assertEquals(expected, DateConversions.toDouble(date, utcConverter), 0.0);
    }

    @Test
    public void testToSqlDate()
    {
        java.sql.Date result = DateConversions.toSqlDate(date, utcConverter);
        assertEquals(EPOCH_MILLIS, result.getTime());
    }

    @Test
    public void testToDate()
    {
        Date result = DateConversions.toDate(date, utcConverter);
        assertEquals(EPOCH_MILLIS, result.getTime());
    }

    @Test
    public void testToTimestamp()
    {
        Timestamp ts = DateConversions.toTimestamp(date, utcConverter);
        assertEquals(EPOCH_MILLIS, ts.getTime());
    }

    @Test
    public void testToCalendar()
    {
        Calendar cal = DateConversions.toCalendar(date, utcConverter);
        assertEquals(EPOCH_MILLIS, cal.getTimeInMillis());
        assertEquals(UTC, cal.getTimeZone().toZoneId());
    }

    @Test
    public void testToBigDecimal()
    {
        // The library divides by BigDecimalConversions.GRAND (10^9) with 9‑scale rounding down
        BigDecimal expected = new BigDecimal(EPOCH_MILLIS)
                .divide(new BigDecimal("1000000000"), 9, RoundingMode.DOWN);
        assertEquals(0, expected.compareTo(DateConversions.toBigDecimal(date, utcConverter)));
    }

    @Test
    public void testToInstantWithUtilDate()
    {
        Instant instant = DateConversions.toInstant(date, utcConverter);
        assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), instant);
    }

    @Test
    public void testToInstantWithSqlDate()
    {
        Instant instant = DateConversions.toInstant(sqlDate, utcConverter);
        assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), instant);
    }

    @Test
    public void testToOffsetDateTime()
    {
        OffsetDateTime odt = DateConversions.toOffsetDateTime(date, utcConverter);
        assertEquals(Instant.ofEpochMilli(EPOCH_MILLIS), odt.toInstant());
        assertEquals(UTC, odt.getOffset());
    }

    @Test
    public void testToLocalDateTime()
    {
        LocalDateTime ldt = DateConversions.toLocalDateTime(date, utcConverter);
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(EPOCH_MILLIS), UTC);
        assertEquals(expected, ldt);
    }

    @Test
    public void testToLocalDate()
    {
        LocalDate ld = DateConversions.toLocalDate(date, utcConverter);
        LocalDate expected = LocalDate.ofInstant(Instant.ofEpochMilli(EPOCH_MILLIS), UTC);
        assertEquals(expected, ld);
    }

    @Test
    public void testToLocalTime()
    {
        LocalTime lt = DateConversions.toLocalTime(date, utcConverter);
        LocalTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(EPOCH_MILLIS), UTC).toLocalTime();
        assertEquals(expected, lt);
    }

    @Test
    public void testToBigInteger()
    {
        // InstantConversions.toBigInteger is not visible, but the contract is to return
        // the epoch‑millis as a BigInteger (the most common implementation).
        BigInteger bi = DateConversions.toBigInteger(date, utcConverter);
        assertEquals(BigInteger.valueOf(EPOCH_MILLIS), bi);
    }

    @Test
    public void testToAtomicLong()
    {
        AtomicLong al = DateConversions.toAtomicLong(date, utcConverter);
        assertEquals(EPOCH_MILLIS, al.get());
    }

    @Test
    public void testSqlDateToString()
    {
        String s = DateConversions.sqlDateToString(sqlDate, utcConverter);
        String expected = DateConversions.toString(new Date(EPOCH_MILLIS), utcConverter);
        assertEquals(expected, s);
    }

    @Test
    public void testToStringUtc()
    {
        String formatted = DateConversions.toString(date, utcConverter);
        // Expected format: 2021-01-01T00:00:00.123Z
        assertEquals("2021-01-01T00:00:00.123Z", formatted);
    }

    @Test
    public void testToStringWithNonUtcOffset()
    {
        // New‑York on 2021‑01‑01 is UTC‑05:00 (standard time)
        String formatted = DateConversions.toString(date, nyConverter);
        // The local time in New York is 2020‑12‑31T19:00:00.123‑05:00
        assertEquals("2020-12-31T19:00:00.123-05:00", formatted);
    }

    @Test
    public void testToMap()
    {
        Map<String, Object> map = DateConversions.toMap(date, utcConverter);
        assertEquals("2021-01-01", map.get(MapConversions.DATE));
        assertEquals("00:00:00.123", map.get(MapConversions.TIME));
        assertEquals(UTC.toString(), map.get(MapConversions.ZONE));
        assertEquals(EPOCH_MILLIS, map.get(MapConversions.EPOCH_MILLIS));
    }

    /* ----------------------------------------------------------------------
     *  3.  Edge‑case and exception tests
     * ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_Null()
    {
        DateConversions.toZonedDateTime(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongType()
    {
        DateConversions.toZonedDateTime("not a date", utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_Null()
    {
        DateConversions.toLong(null, utcConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType()
    {
        DateConversions.toLong(12345, utcConverter);
    }

    @Test
    public void testToBigDecimal_RoundingDown()
    {
        // epoch millis that would produce a repeating decimal when divided by 1e9
        long millis = 1L; // 0.000000001
        Date d = new Date(millis);
        BigDecimal result = DateConversions.toBigDecimal(d, utcConverter);
        BigDecimal expected = new BigDecimal(millis)
                .divide(new BigDecimal("1000000000"), 9, RoundingMode.DOWN);
        assertEquals(0, expected.compareTo(result));
    }

    @Test
    public void testToInstant_Null()
    {
        try
        {
            DateConversions.toInstant(null, utcConverter);
            fail("Expected NullPointerException");
        }
        catch (NullPointerException ignored) { }
    }

    @Test
    public void testToString_Null()
    {
        try
        {
            DateConversions.toString(null, utcConverter);
            fail("Expected NullPointerException");
        }
        catch (NullPointerException ignored) { }
    }

    @Test
    public void testSqlDateToString_Null()
    {
        try
        {
            DateConversions.sqlDateToString(null, utcConverter);
            fail("Expected NullPointerException");
        }
        catch (NullPointerException ignored) { }
    }

    @Test
    public void testToMap_Null()
    {
        try
        {
            DateConversions.toMap(null, utcConverter);
            fail("Expected NullPointerException");
        }
        catch (NullPointerException ignored) { }
    }

    @Test
    public void testNegativeEpoch()
    {
        // 1969‑12‑31T23:59:59.999Z  (one millisecond before the epoch)
        Date beforeEpoch = new Date(-1L);
        assertEquals(-1L, DateConversions.toLong(beforeEpoch, utcConverter));
        assertEquals("-1", DateConversions.toBigDecimal(beforeEpoch, utcConverter).toPlainString());
        assertEquals(Instant.ofEpochMilli(-1L), DateConversions.toInstant(beforeEpoch, utcConverter));
    }
}
