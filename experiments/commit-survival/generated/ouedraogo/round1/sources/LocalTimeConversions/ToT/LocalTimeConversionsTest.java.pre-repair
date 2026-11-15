package com.cedarsoftware.util.convert;

import org.junit.BeforeClass;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
import java.time.ZoneId;
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
 * <ul>
 *   <li>normal usage for every conversion method</li>
 *   <li>boundary values (midnight, last nanosecond of the day)</li>
 *   <li>error scenarios (null input, wrong type)</li>
 *   <li>verification of rounding/scale for {@code BigDecimal}</li>
 *   <li>time‑zone handling for {@code toCalendar}</li>
 * </ul>
 *
 * Only classes, constructors and methods that appear in the supplied source
 * (or in the public API of {@code Converter} and {@code CompactLinkedMap})
 * are used – no mocking or external libraries.
 */
public class LocalTimeConversionsTest {

    /** A reusable {@code Converter} instance with default options. */
    private static Converter converter;

    @BeforeClass
    public static void setUpClass() {
        // The public API shows a constructor taking ConverterOptions.
        // We rely on the existence of a no‑arg constructor for ConverterOptions.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------
     * Helper methods
     * ------------------------------------------------------------- */

    /** Returns the expected number of milliseconds since start of day. */
    private static long expectedMillis(LocalTime t) {
        return t.toNanoOfDay() / 1_000_000L;
    }

    /** Returns the expected number of seconds (including fraction) as double. */
    private static double expectedSeconds(LocalTime t) {
        return t.toNanoOfDay() / 1_000_000_000.0;
    }

    /* -------------------------------------------------------------
     *  Tests for toMap
     * ------------------------------------------------------------- */
    @Test
    public void testToMapNormal() {
        LocalTime time = LocalTime.of(13, 45, 30, 123_456_789);
        Map<String, Object> map = LocalTimeConversions.toMap(time, converter);
        assertEquals("Map should contain exactly one entry", 1, map.size());
        // The key used by the implementation is MapConversions.TIME.
        // We cannot reference that constant directly, but we can verify the value.
        Object value = map.values().iterator().next();
        assertTrue("Map value must be a String", value instanceof String);
        assertEquals("String representation must match LocalTime.toString()", time.toString(), value);
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNull() {
        LocalTimeConversions.toMap(null, converter);
    }

    /* -------------------------------------------------------------
     *  Tests for toInteger
     * ------------------------------------------------------------- */
    @Test
    public void testToIntegerMidnight() {
        LocalTime midnight = LocalTime.MIDNIGHT;
        int result = LocalTimeConversions.toInteger(midnight, converter);
        assertEquals(0, result);
    }

    @Test
    public void testToIntegerTypical() {
        LocalTime time = LocalTime.of(12, 34, 56, 789_000_000); // 12:34:56.789
        int result = LocalTimeConversions.toInteger(time, converter);
        assertEquals((int) expectedMillis(time), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToIntegerNull() {
        LocalTimeConversions.toInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToIntegerWrongType() {
        LocalTimeConversions.toInteger("not a LocalTime", converter);
    }

    /* -------------------------------------------------------------
     *  Tests for toLong
     * ------------------------------------------------------------- */
    @Test
    public void testToLongEndOfDay() {
        // 23:59:59.999999999 – the last nanosecond of the day
        LocalTime last = LocalTime.of(23, 59, 59, 999_999_999);
        long result = LocalTimeConversions.toLong(last, converter);
        assertEquals(expectedMillis(last), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToLongNull() {
        LocalTimeConversions.toLong(null, converter);
    }

    /* -------------------------------------------------------------
     *  Tests for toDouble
     * ------------------------------------------------------------- */
    @Test
    public void testToDoubleFractionalSeconds() {
        LocalTime time = LocalTime.of(01, 02, 03, 456_000_000); // 1:02:03.456
        double result = LocalTimeConversions.toDouble(time, converter);
        assertEquals(expectedSeconds(time), result, 1e-12);
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleNull() {
        LocalTimeConversions.toDouble(null, converter);
    }

    /* -------------------------------------------------------------
     *  Tests for toBigInteger
     * ------------------------------------------------------------- */
    @Test
    public void testToBigIntegerTypical() {
        LocalTime time = LocalTime.of(10, 20, 30, 400_000_000);
        BigInteger bi = LocalTimeConversions.toBigInteger(time, converter);
        assertEquals(BigInteger.valueOf(time.toNanoOfDay()), bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNull() {
        LocalTimeConversions.toBigInteger(null, converter);
    }

    /* -------------------------------------------------------------
     *  Tests for toBigDecimal
     * ------------------------------------------------------------- */
    @Test
    public void testToBigDecimalRounding() {
        // 12:00:00.123456789 → 43200.123456789 seconds
        LocalTime time = LocalTime.of(12, 0, 0, 123_456_789);
        BigDecimal bd = LocalTimeConversions.toBigDecimal(time, converter);
        BigDecimal expected = new BigDecimal(time.toNanoOfDay())
                .divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        assertEquals("Scale must be 9", 9, bd.scale());
        assertEquals("Value must be correctly rounded", expected, bd);
    }

    @Test
    public void testToBigDecimalMaxValue() {
        // 23:59:59.999999999 → 86 399.999999999 seconds
        LocalTime time = LocalTime.of(23, 59, 59, 999_999_999);
        BigDecimal bd = LocalTimeConversions.toBigDecimal(time, converter);
        assertEquals(9, bd.scale());
        assertEquals(new BigDecimal("86399.999999999"), bd);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalNull() {
        LocalTimeConversions.toBigDecimal(null, converter);
    }

    /* -------------------------------------------------------------
     *  Tests for toAtomicInteger
     * ------------------------------------------------------------- */
    @Test
    public void testToAtomicIntegerTypical() {
        LocalTime time = LocalTime.of(00, 00, 01, 500_000_000); // 1.5 seconds → 1500 ms
        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(time, converter);
        assertEquals((int) expectedMillis(time), ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNull() {
        LocalTimeConversions.toAtomicInteger(null, converter);
    }

    /* -------------------------------------------------------------
     *  Tests for toAtomicLong
     * ------------------------------------------------------------- */
    @Test
    public void testToAtomicLongTypical() {
        LocalTime time = LocalTime.of(02, 30, 0, 0); // 2h30m → 9 000 000 ms
        AtomicLong al = LocalTimeConversions.toAtomicLong(time, converter);
        assertEquals(expectedMillis(time), al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNull() {
        LocalTimeConversions.toAtomicLong(null, converter);
    }

    /* -------------------------------------------------------------
     *  Tests for toString
     * ------------------------------------------------------------- */
    @Test
    public void testToStringMatchesISO() {
        LocalTime time = LocalTime.of(09, 15, 30, 250_000_000);
        String s = LocalTimeConversions.toString(time, converter);
        assertEquals(time.format(DateTimeFormatter.ISO_LOCAL_TIME), s);
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNull() {
        LocalTimeConversions.toString(null, converter);
    }

    /* -------------------------------------------------------------
     *  Tests for toCalendar
     * ------------------------------------------------------------- */
    @Test
    public void testToCalendarFieldsAndTimeZone() {
        // Force a known timezone (UTC) to avoid flaky results on CI machines.
        ConverterOptions opts = new ConverterOptions();
        opts.setTimeZone(TimeZone.getTimeZone(ZoneId.of("UTC")));
        Converter utcConverter = new Converter(opts);

        LocalTime time = LocalTime.of(15, 45, 30, 123_000_000); // 15:45:30.123
        Calendar cal = LocalTimeConversions.toCalendar(time, utcConverter);

        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());

        assertEquals(15, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(45, cal.get(Calendar.MINUTE));
        assertEquals(30, cal.get(Calendar.SECOND));
        // Milliseconds derived from nanoseconds
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendarNull() {
        LocalTimeConversions.toCalendar(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendarNullConverter() {
        LocalTime time = LocalTime.NOON;
        LocalTimeConversions.toCalendar(time, null);
    }
}
