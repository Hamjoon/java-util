package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalTimeConversions}.
 * <p>
 * The tests cover every public static conversion method in the class,
 * exercising typical values, edge values and the interaction with
 * {@link Converter} (especially the {@code toCalendar} method which uses
 * the converter’s {@link ConverterOptions#getTimeZone()}).
 * </p>
 */
public class LocalTimeConversionsTest {

    /** Helper that builds a {@link Converter} with a known time‑zone (UTC). */
    private static Converter createUtcConverter() {
        // The library provides a public ctor Converter(ConverterOptions).
        // ConverterOptions has a default ctor and a setter for the time‑zone.
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(java.util.TimeZone.getTimeZone(ZoneId.of("UTC")));
        return new Converter(options);
    }

    @Test
    public void testToMap() {
        LocalTime time = LocalTime.of(12, 34, 56, 123_456_789);
        Map<String, Object> map = LocalTimeConversions.toMap(time, createUtcConverter());

        // The key used by MapConversions is "localTime".  We verify the map contains it.
        assertTrue("Map should contain key 'localTime'", map.containsKey("localTime"));
        assertEquals("String representation must match LocalTime.toString()", time.toString(), map.get("localTime"));
    }

    @Test
    public void testToIntegerAndLong() {
        // 01:02:03.004005006  =>  1h 2m 3s 4ms 5µs 6ns
        LocalTime time = LocalTime.of(1, 2, 3, 4_005_006);
        long expectedNanos = time.toNanoOfDay();          // full nanoseconds of day
        int expectedMillis = (int) (expectedNanos / 1_000_000L);
        long expectedLongMillis = expectedNanos / 1_000_000L;

        assertEquals(expectedMillis, LocalTimeConversions.toInteger(time, createUtcConverter()));
        assertEquals(expectedLongMillis, LocalTimeConversions.toLong(time, createUtcConverter()));
    }

    @Test
    public void testToDouble() {
        // 00:00:01.500000000 => 1.5 seconds
        LocalTime time = LocalTime.ofSecondOfDay(1).plusNanos(500_000_000);
        double expected = 1.5; // seconds as double
        assertEquals(expected, LocalTimeConversions.toDouble(time, createUtcConverter()), 1e-12);
    }

    @Test
    public void testToBigInteger() {
        LocalTime time = LocalTime.of(23, 59, 59, 999_999_999);
        BigInteger expected = BigInteger.valueOf(time.toNanoOfDay());
        assertEquals(expected, LocalTimeConversions.toBigInteger(time, createUtcConverter()));
    }

    @Test
    public void testToBigDecimal() {
        // 00:00:00.123456789 => 0.123456789 seconds
        LocalTime time = LocalTime.ofNanoOfDay(123_456_789);
        BigDecimal expected = new BigDecimal(time.toNanoOfDay())
                .divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        assertEquals(expected, LocalTimeConversions.toBigDecimal(time, createUtcConverter()));
    }

    @Test
    public void testToAtomicIntegerAndLong() {
        LocalTime time = LocalTime.of(0, 0, 0, 500_000_000); // 0.5 seconds => 500 ms
        int expectedInt = (int) (time.toNanoOfDay() / 1_000_000L);
        long expectedLong = time.toNanoOfDay() / 1_000_000L;

        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(time, createUtcConverter());
        AtomicLong al = LocalTimeConversions.toAtomicLong(time, createUtcConverter());

        assertEquals(expectedInt, ai.get());
        assertEquals(expectedLong, al.get());
    }

    @Test
    public void testToString() {
        LocalTime time = LocalTime.of(9, 8, 7, 654_321_000);
        String expected = time.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME);
        assertEquals(expected, LocalTimeConversions.toString(time, createUtcConverter()));
    }

    @Test
    public void testToCalendarMidnight() {
        LocalTime time = LocalTime.MIDNIGHT; // 00:00:00.000
        Calendar cal = LocalTimeConversions.toCalendar(time, createUtcConverter());

        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
        assertEquals(java.util.TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testToCalendarTypical() {
        // 13:45:30.250000000 => 13h 45m 30s 250ms
        LocalTime time = LocalTime.of(13, 45, 30, 250_000_000);
        Calendar cal = LocalTimeConversions.toCalendar(time, createUtcConverter());

        assertEquals(13, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(45, cal.get(Calendar.MINUTE));
        assertEquals(30, cal.get(Calendar.SECOND));
        assertEquals(250, cal.get(Calendar.MILLISECOND));
        assertEquals(java.util.TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test
    public void testToCalendarMaximum() {
        // 23:59:59.999999999 => 23h 59m 59s 999ms (nanoseconds rounded down to ms)
        LocalTime time = LocalTime.MAX;
        Calendar cal = LocalTimeConversions.toCalendar(time, createUtcConverter());

        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
        assertEquals(999, cal.get(Calendar.MILLISECOND));
        assertEquals(java.util.TimeZone.getTimeZone("UTC"), cal.getTimeZone());
    }

    @Test(expected = NullPointerException.class)
    public void testToMapNullThrowsNPE() {
        LocalTimeConversions.toMap(null, createUtcConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToIntegerNullThrowsNPE() {
        LocalTimeConversions.toInteger(null, createUtcConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToLongNullThrowsNPE() {
        LocalTimeConversions.toLong(null, createUtcConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleNullThrowsNPE() {
        LocalTimeConversions.toDouble(null, createUtcConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNullThrowsNPE() {
        LocalTimeConversions.toBigInteger(null, createUtcConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalNullThrowsNPE() {
        LocalTimeConversions.toBigDecimal(null, createUtcConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNullThrowsNPE() {
        LocalTimeConversions.toAtomicInteger(null, createUtcConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullThrowsNPE() {
        LocalTimeConversions.toAtomicLong(null, createUtcConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToStringNullThrowsNPE() {
        LocalTimeConversions.toString(null, createUtcConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendarNullThrowsNPE() {
        LocalTimeConversions.toCalendar(null, createUtcConverter());
    }
}
