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

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link TimestampConversions}.
 *
 * The tests are placed in the same package as {@code TimestampConversions}
 * so that the package‑private static methods are directly accessible.
 *
 * The test class creates a real {@link Converter} instance using the default
 * {@link ConverterOptions}.  All conversions are exercised with normal
 * values, with edge values (epoch, negative epoch, maximum nanos) and with
 * invalid inputs to verify that the expected {@link ClassCastException}
 * is thrown.
 */
public class TimestampConversionsTest
{
    private Converter converter;

    @Before
    public void setUp()
    {
        // ConverterOptions is part of the library – we assume it has a public
        // no‑arg constructor that yields sensible defaults (UTC zone, default
        // time‑zone, etc.).
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- */
    /*  List of package‑private static methods (extracted from the source)   */
    /* --------------------------------------------------------------------- */
    // double   toDouble(Object, Converter)
    // BigDecimal toBigDecimal(Object, Converter)
    // BigInteger toBigInteger(Object, Converter)
    // LocalDateTime toLocalDateTime(Object, Converter)
    // Duration toDuration(Object, Converter)
    // OffsetDateTime toOffsetDateTime(Object, Converter)
    // Calendar toCalendar(Object, Converter)
    // Date toDate(Object, Converter)
    // java.sql.Date toSqlDate(Object, Converter)
    // long toLong(Object, Converter)
    // Year toYear(Object, Converter)
    // YearMonth toYearMonth(Object, Converter)
    // MonthDay toMonthDay(Object, Converter)
    // String toString(Object, Converter)
    // Map<String,Object> toMap(Object, Converter)

    /* --------------------------------------------------------------------- */
    /*  Helper utilities                                                   */
    /* --------------------------------------------------------------------- */

    /** Creates a {@link Timestamp} from epoch milliseconds and an optional
     *  nanosecond adjustment (0‑999_999). */
    private static Timestamp ts(long epochMilli, int nanosAdjustment)
    {
        Timestamp t = new Timestamp(epochMilli);
        // Timestamp stores nanos as the full nanosecond part of the second.
        // The constructor above already sets the millisecond part; we replace
        // the nanos field with the desired value.
        t.setNanos((int) ((epochMilli % 1000) * 1_000_000) + nanosAdjustment);
        return t;
    }

    /** Returns a Timestamp representing the exact epoch (1970‑01‑01T00:00:00Z). */
    private static Timestamp epoch()
    {
        return new Timestamp(0L);
    }

    /* --------------------------------------------------------------------- */
    /*  Basic functionality tests                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToDouble()
    {
        Timestamp ts = ts(1_600_000_000_000L, 123_456_789); // 2020‑09‑13T12:26:40.123456789Z
        double result = TimestampConversions.toDouble(ts, converter);
        // Convert manually for verification
        Duration d = Duration.between(Instant.EPOCH, ts.toInstant());
        double expected = BigDecimalConversions.secondsAndNanosToDouble(d.getSeconds(),
                                                                        d.getNano()).doubleValue();
        assertEquals(expected, result, 0.0);
    }

    @Test
    public void testToBigDecimal()
    {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        BigDecimal bd = TimestampConversions.toBigDecimal(ts, converter);
        Instant instant = ts.toInstant();
        BigDecimal expected = InstantConversions.toBigDecimal(instant, converter);
        assertEquals(expected, bd);
    }

    @Test
    public void testToBigInteger()
    {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        BigInteger bi = TimestampConversions.toBigInteger(ts, converter);
        Instant instant = ts.toInstant();
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, bi);
    }

    @Test
    public void testToLocalDateTime()
    {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        LocalDateTime ldt = TimestampConversions.toLocalDateTime(ts, converter);
        LocalDateTime expected = ts.toInstant()
                                  .atZone(converter.getOptions().getZoneId())
                                  .toLocalDateTime();
        assertEquals(expected, ldt);
    }

    @Test
    public void testToDuration()
    {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        Duration d = TimestampConversions.toDuration(ts, converter);
        Duration expected = Duration.between(Instant.EPOCH, ts.toInstant());
        assertEquals(expected, d);
    }

    @Test
    public void testToOffsetDateTime()
    {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        OffsetDateTime odt = TimestampConversions.toOffsetDateTime(ts, converter);
        ZonedDateTime zdt = ZonedDateTime.ofInstant(ts.toInstant(),
                                                    converter.getOptions().getZoneId());
        assertEquals(zdt.toOffsetDateTime(), odt);
    }

    @Test
    public void testToCalendar()
    {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        Calendar cal = TimestampConversions.toCalendar(ts, converter);
        Calendar expected = Calendar.getInstance(converter.getOptions().getTimeZone());
        expected.setTimeInMillis(ts.getTime());
        assertEquals(expected.getTimeInMillis(), cal.getTimeInMillis());
        assertEquals(expected.getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testToDate()
    {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        Date date = TimestampConversions.toDate(ts, converter);
        Date expected = Date.from(ts.toInstant());
        assertEquals(expected, date);
    }

    @Test
    public void testToSqlDate()
    {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        java.sql.Date sqlDate = TimestampConversions.toSqlDate(ts, converter);
        java.sql.Date expected = java.sql.Date.valueOf(
                ts.toInstant()
                  .atZone(converter.getOptions().getZoneId())
                  .toLocalDate());
        assertEquals(expected, sqlDate);
    }

    @Test
    public void testToLong()
    {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        long l = TimestampConversions.toLong(ts, converter);
        assertEquals(ts.getTime(), l);
    }

    @Test
    public void testToYear()
    {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        Year y = TimestampConversions.toYear(ts, converter);
        Year expected = Year.from(
                ts.toInstant()
                  .atZone(converter.getOptions().getZoneId())
                  .toLocalDate());
        assertEquals(expected, y);
    }

    @Test
    public void testToYearMonth()
    {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        YearMonth ym = TimestampConversions.toYearMonth(ts, converter);
        YearMonth expected = YearMonth.from(
                ts.toInstant()
                  .atZone(converter.getOptions().getZoneId())
                  .toLocalDate());
        assertEquals(expected, ym);
    }

    @Test
    public void testToMonthDay()
    {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        MonthDay md = TimestampConversions.toMonthDay(ts, converter);
        MonthDay expected = MonthDay.from(
                ts.toInstant()
                  .atZone(converter.getOptions().getZoneId())
                  .toLocalDate());
        assertEquals(expected, md);
    }

    @Test
    public void testToString_MillisecondPrecision()
    {
        // Nanos divisible by 1_000_000 → millisecond precision pattern
        Timestamp ts = ts(1_600_000_000_123L, 0); // 123 ms, 0 nanos
        String formatted = TimestampConversions.toString(ts, converter);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                                 .withZone(ZoneOffset.UTC);
        String expected = fmt.format(ts.toInstant());
        assertEquals(expected, formatted);
    }

    @Test
    public void testToString_NanosecondPrecision()
    {
        // Nanos NOT divisible by 1_000_000 → nanosecond pattern
        Timestamp ts = ts(1_600_000_000_123L, 456_789); // 123 ms + 456_789 ns
        String formatted = TimestampConversions.toString(ts, converter);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
                                                 .withZone(ZoneOffset.UTC);
        String expected = fmt.format(ts.toInstant());
        assertEquals(expected, formatted);
    }

    @Test
    public void testToMap()
    {
        Timestamp ts = ts(1_600_000_000_123L, 0);
        Map<String, Object> map = TimestampConversions.toMap(ts, converter);
        assertNotNull(map);
        assertTrue(map.containsKey(MapConversions.TIMESTAMP));
        assertEquals(TimestampConversions.toString(ts, converter), map.get(MapConversions.TIMESTAMP));
    }

    /* --------------------------------------------------------------------- */
    /*  Edge‑case and exception tests                                        */
    /* --------------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToDouble_WithInvalidSource()
    {
        TimestampConversions.toDouble("not a timestamp", converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_WithInvalidSource()
    {
        TimestampConversions.toBigDecimal(12345, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalDateTime_WithNull()
    {
        TimestampConversions.toLocalDateTime(null, converter);
    }

    @Test
    public void testToDuration_Epoch()
    {
        Timestamp ts = epoch();
        Duration d = TimestampConversions.toDuration(ts, converter);
        assertEquals(Duration.ZERO, d);
    }

    @Test
    public void testToString_NegativeTimestamp()
    {
        // Timestamp before epoch (e.g., 1960‑01‑01)
        Timestamp ts = new Timestamp(-315619200000L); // 1960‑01‑01T00:00:00Z
        ts.setNanos(0);
        String formatted = TimestampConversions.toString(ts, converter);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                                                 .withZone(ZoneOffset.UTC);
        String expected = fmt.format(ts.toInstant());
        assertEquals(expected, formatted);
    }

    @Test
    public void testToString_MaxNanoseconds()
    {
        // Max nanos within a second: 999,999,999 (not divisible by 1_000_000)
        Timestamp ts = ts(1_600_000_000_000L, 999_999_999);
        String formatted = TimestampConversions.toString(ts, converter);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'")
                                                 .withZone(ZoneOffset.UTC);
        String expected = fmt.format(ts.toInstant());
        assertEquals(expected, formatted);
    }

    @Test(expected = ClassCastException.class)
    public void testToCalendar_WithInvalidObject()
    {
        TimestampConversions.toCalendar(new Object(), converter);
    }

    @Test
    public void testToLong_NegativeValue()
    {
        Timestamp ts = new Timestamp(-123456789L);
        ts.setNanos(0);
        long l = TimestampConversions.toLong(ts, converter);
        assertEquals(-123456789L, l);
    }
}
