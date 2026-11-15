package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalTimeConversions}.
 *
 * The tests cover:
 *   • Typical values (mid‑day, midnight, last nanosecond of the day)
 *   • Edge cases (maximum nanosecond, conversion to int/long overflow concerns)
 *   • Error scenarios (null source, wrong source type)
 *   • All conversion methods defined in {@code LocalTimeConversions}
 *
 * No mocking frameworks are used – real objects are constructed via the
 * public API of {@link Converter} and {@link ConverterOptions}.
 */
public class LocalTimeConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Use default options – they provide a non‑null TimeZone (system default)
        ConverterOptions options = new ConverterOptions();
        // Ensure a known time zone to make Calendar assertions deterministic
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        converter = new Converter(options);
    }

    /* -------------------------------------------------------------
     *  toMap()
     * ------------------------------------------------------------- */
    @Test
    public void testToMapTypical() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        Map<String, Object> map = LocalTimeConversions.toMap(lt, converter);

        // The key is defined in MapConversions.LOCAL_TIME – we do not import that class
        // directly, but we can check that the map contains exactly one entry whose value
        // matches lt.toString().
        assertEquals(1, map.size());
        Object value = map.values().iterator().next();
        assertEquals(lt.toString(), value);
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullSource() {
        LocalTimeConversions.toMap(null, converter);
    }

    /* -------------------------------------------------------------
     *  toInteger()
     * ------------------------------------------------------------- */
    @Test
    public void testToIntegerMidday() {
        LocalTime lt = LocalTime.NOON; // 12:00:00.000000000
        int millis = LocalTimeConversions.toInteger(lt, converter);
        assertEquals(12 * 60 * 60 * 1000, millis);
    }

    @Test
    public void testToIntegerEdgeMaximum() {
        // 23:59:59.999999999 → 86_399_999 ms (the last whole millisecond of the day)
        LocalTime lt = LocalTime.of(23, 59, 59, 999_999_999);
        int millis = LocalTimeConversions.toInteger(lt, converter);
        assertEquals(86_399_999, millis);
    }

    @Test(expected = NullPointerException.class)
    public void testToIntegerNullSource() {
        LocalTimeConversions.toInteger(null, converter);
    }

    /* -------------------------------------------------------------
     *  toLong()
     * ------------------------------------------------------------- */
    @Test
    public void testToLongMidnight() {
        LocalTime lt = LocalTime.MIDNIGHT;
        long millis = LocalTimeConversions.toLong(lt, converter);
        assertEquals(0L, millis);
    }

    @Test
    public void testToLongEdgeMaximum() {
        LocalTime lt = LocalTime.of(23, 59, 59, 999_999_999);
        long millis = LocalTimeConversions.toLong(lt, converter);
        assertEquals(86_399_999L, millis);
    }

    @Test(expected = NullPointerException.class)
    public void testToLongNullSource() {
        LocalTimeConversions.toLong(null, converter);
    }

    /* -------------------------------------------------------------
     *  toDouble()
     * ------------------------------------------------------------- */
    @Test
    public void testToDoubleTypical() {
        LocalTime lt = LocalTime.of(1, 2, 3, 400_000_000); // 1:02:03.400
        double seconds = LocalTimeConversions.toDouble(lt, converter);
        // Expected: total nanoseconds / 1_000_000_000.0
        double expected = lt.toNanoOfDay() / 1_000_000_000.0;
        assertEquals(expected, seconds, 1e-12);
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleNullSource() {
        LocalTimeConversions.toDouble(null, converter);
    }

    /* -------------------------------------------------------------
     *  toBigInteger()
     * ------------------------------------------------------------- */
    @Test
    public void testToBigIntegerTypical() {
        LocalTime lt = LocalTime.of(0, 0, 0, 123_456_789);
        BigInteger bi = LocalTimeConversions.toBigInteger(lt, converter);
        assertEquals(BigInteger.valueOf(lt.toNanoOfDay()), bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNullSource() {
        LocalTimeConversions.toBigInteger(null, converter);
    }

    /* -------------------------------------------------------------
     *  toBigDecimal()
     * ------------------------------------------------------------- */
    @Test
    public void testToBigDecimalPrecision() {
        LocalTime lt = LocalTime.of(12, 34, 56, 789_123_456);
        BigDecimal bd = LocalTimeConversions.toBigDecimal(lt, converter);
        // The method divides by 1_000_000_000 with scale 9 and HALF_UP rounding.
        BigDecimal expected = new BigDecimal(lt.toNanoOfDay())
                .divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        assertEquals(expected, bd);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalNullSource() {
        LocalTimeConversions.toBigDecimal(null, converter);
    }

    /* -------------------------------------------------------------
     *  toAtomicInteger()
     * ------------------------------------------------------------- */
    @Test
    public void testToAtomicInteger() {
        LocalTime lt = LocalTime.of(2, 0, 0); // 2 hours → 7_200_000 ms
        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(lt, converter);
        assertEquals(7_200_000, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNullSource() {
        LocalTimeConversions.toAtomicInteger(null, converter);
    }

    /* -------------------------------------------------------------
     *  toAtomicLong()
     * ------------------------------------------------------------- */
    @Test
    public void testToAtomicLong() {
        LocalTime lt = LocalTime.of(23, 59, 59, 0);
        AtomicLong al = LocalTimeConversions.toAtomicLong(lt, converter);
        assertEquals(86_399_000L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullSource() {
        LocalTimeConversions.toAtomicLong(null, converter);
    }

    /* -------------------------------------------------------------
     *  toString()
     * ------------------------------------------------------------- */
    @Test
    public void testToStringStandard() {
        LocalTime lt = LocalTime.of(9, 8, 7, 654_321_000);
        String s = LocalTimeConversions.toString(lt, converter);
        assertEquals(lt.format(DateTimeFormatter.ISO_LOCAL_TIME), s);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullSource() {
        LocalTimeConversions.toString(null, converter);
    }

    /* -------------------------------------------------------------
     *  toCalendar()
     * ------------------------------------------------------------- */
    @Test
    public void testToCalendarTypical() {
        LocalTime lt = LocalTime.of(15, 30, 45, 250_000_000); // 15:30:45.250
        Calendar cal = LocalTimeConversions.toCalendar(lt, converter);

        // Calendar should be in UTC (as set in setUp)
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());

        assertEquals(15, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
        assertEquals(45, cal.get(Calendar.SECOND));
        // Milliseconds derived from nanoseconds
        assertEquals(250, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testToCalendarMidnight() {
        LocalTime lt = LocalTime.MIDNIGHT;
        Calendar cal = LocalTimeConversions.toCalendar(lt, converter);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendarNullSource() {
        LocalTimeConversions.toCalendar(null, converter);
    }

    /* -------------------------------------------------------------
     *  Incorrect source type – should throw ClassCastException
     * ------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testToIntegerWrongType() {
        LocalTimeConversions.toInteger("not a LocalTime", converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLongWrongType() {
        LocalTimeConversions.toLong(12345, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWrongType() {
        LocalTimeConversions.toMap(Boolean.TRUE, converter);
    }
}
