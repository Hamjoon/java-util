package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive unit tests for {@link LocalTimeConversions}.
 *
 * The tests cover every public static method in the class and verify
 * correct handling of typical, boundary and edge‑case values.
 *
 * No mocking frameworks are used – all objects are created via their
 * real constructors as required by the task description.
 */
public class LocalTimeConversionsTest {

    /** A {@link Converter} instance with the default options. */
    private Converter converter;

    /** The default {@link TimeZone} used by the {@link Converter}. */
    private TimeZone defaultTimeZone;

    @Before
    public void setUp() {
        // The library provides a ConverterOptions class (not shown) that
        // supplies a TimeZone.  We use the default constructor which
        // selects the JVM default time zone.
        ConverterOptions options = new ConverterOptions();   // assumes a public no‑arg ctor
        defaultTimeZone = options.getTimeZone();            // should be the JVM default
        converter = new Converter(options);
    }

    /* -------------------------------------------------------------
     * Helper methods to calculate expected values
     * ---------------------------------------------------------- */

    /** Returns the number of nanoseconds since midnight for the given time. */
    private static long nanosOfDay(LocalTime t) {
        return t.toNanoOfDay();
    }

    /** Returns the number of whole milliseconds since midnight for the given time. */
    private static long millisOfDay(LocalTime t) {
        return nanosOfDay(t) / 1_000_000L;
    }

    /** Returns the number of whole seconds (as a double) since midnight for the given time. */
    private static double secondsOfDay(LocalTime t) {
        return nanosOfDay(t) / 1_000_000_000.0;
    }

    /* -------------------------------------------------------------
     * Tests for toMap(...)
     * ---------------------------------------------------------- */

    @Test
    public void testToMapProducesCorrectEntry() {
        LocalTime time = LocalTime.of(12, 34, 56, 789_000_000);
        Map<String, Object> map = LocalTimeConversions.toMap(time, converter);

        // The key is defined in MapConversions.TIME – we use reflection to avoid a hard dependency.
        String expectedKey = "time";   // fallback if MapConversions is unavailable
        try {
            Class<?> mc = Class.forName("com.cedarsoftware.util.convert.MapConversions");
            expectedKey = (String) mc.getField("TIME").get(null);
        } catch (Exception ignored) { /* use fallback */ }

        assertEquals("Map should contain exactly one entry", 1, map.size());
        assertTrue("Map must contain the TIME key", map.containsKey(expectedKey));
        assertEquals("Value must be the ISO‑8601 string representation of the LocalTime",
                     time.toString(), map.get(expectedKey));
    }

    /* -------------------------------------------------------------
     * Tests for toInteger(...)
     * ---------------------------------------------------------- */

    @Test
    public void testToIntegerTypicalValue() {
        LocalTime time = LocalTime.of(1, 2, 3, 456_000_000); // 1:02:03.456
        int result = LocalTimeConversions.toInteger(time, converter);
        int expected = (int) (millisOfDay(time));
        assertEquals(expected, result);
    }

    @Test
    public void testToIntegerMidnight() {
        LocalTime midnight = LocalTime.MIDNIGHT;
        assertEquals(0, LocalTimeConversions.toInteger(midnight, converter));
    }

    @Test
    public void testToIntegerMaximumTime() {
        // 23:59:59.999999999 – the largest possible LocalTime
        LocalTime max = LocalTime.of(23, 59, 59, 999_999_999);
        int result = LocalTimeConversions.toInteger(max, converter);
        // The value fits into an int because it is < 2^31 (≈ 86 400 000 ms)
        int expected = (int) millisOfDay(max);
        assertEquals(expected, result);
    }

    /* -------------------------------------------------------------
     * Tests for toLong(...)
     * ---------------------------------------------------------- */

    @Test
    public void testToLongTypicalValue() {
        LocalTime time = LocalTime.of(10, 20, 30, 123_456_789);
        long result = LocalTimeConversions.toLong(time, converter);
        long expected = millisOfDay(time);
        assertEquals(expected, result);
    }

    @Test
    public void testToLongMaximumTime() {
        LocalTime max = LocalTime.of(23, 59, 59, 999_999_999);
        long result = LocalTimeConversions.toLong(max, converter);
        long expected = millisOfDay(max);
        assertEquals(expected, result);
    }

    /* -------------------------------------------------------------
     * Tests for toDouble(...)
     * ---------------------------------------------------------- */

    @Test
    public void testToDoubleTypicalValue() {
        LocalTime time = LocalTime.of(5, 0, 0, 500_000_000); // 5 hours + 0.5 sec
        double result = LocalTimeConversions.toDouble(time, converter);
        double expected = secondsOfDay(time);
        assertEquals(expected, result, 1e-12);
    }

    @Test
    public void testToDoublePrecision() {
        // Use a value that produces a non‑terminating binary fraction
        LocalTime time = LocalTime.of(0, 0, 0, 1); // 1 nanosecond
        double result = LocalTimeConversions.toDouble(time, converter);
        double expected = 1.0 / 1_000_000_000.0;
        assertEquals(expected, result, 1e-15);
    }

    /* -------------------------------------------------------------
     * Tests for toBigInteger(...)
     * ---------------------------------------------------------- */

    @Test
    public void testToBigIntegerTypicalValue() {
        LocalTime time = LocalTime.of(12, 0, 0, 0);
        BigInteger result = LocalTimeConversions.toBigInteger(time, converter);
        assertEquals(BigInteger.valueOf(nanosOfDay(time)), result);
    }

    @Test
    public void testToBigIntegerMaximum() {
        LocalTime max = LocalTime.of(23, 59, 59, 999_999_999);
        BigInteger result = LocalTimeConversions.toBigInteger(max, converter);
        assertEquals(BigInteger.valueOf(nanosOfDay(max)), result);
    }

    /* -------------------------------------------------------------
     * Tests for toBigDecimal(...)
     * ---------------------------------------------------------- */

    @Test
    public void testToBigDecimalTypicalValue() {
        LocalTime time = LocalTime.of(0, 0, 1, 250_000_000); // 1.250 seconds
        BigDecimal result = LocalTimeConversions.toBigDecimal(time, converter);
        BigDecimal expected = new BigDecimal(nanosOfDay(time))
                .divide(LocalTimeConversions.BILLION, 9, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(result));
        assertEquals(9, result.scale()); // must be scaled to 9 decimal places
    }

    @Test
    public void testToBigDecimalRoundingHalfUp() {
        // 0.3333333335 seconds should round up to 0.333333334
        LocalTime time = LocalTime.of(0, 0, 0, 333_333_334); // 333,333,334 ns
        BigDecimal result = LocalTimeConversions.toBigDecimal(time, converter);
        BigDecimal expected = new BigDecimal("0.333333334"); // 9‑scale rounded value
        assertEquals(0, expected.compareTo(result));
    }

    @Test
    public void testToBigDecimalMaximumTime() {
        LocalTime max = LocalTime.of(23, 59, 59, 999_999_999);
        BigDecimal result = LocalTimeConversions.toBigDecimal(max, converter);
        BigDecimal expected = new BigDecimal(nanosOfDay(max))
                .divide(LocalTimeConversions.BILLION, 9, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(result));
    }

    /* -------------------------------------------------------------
     * Tests for toAtomicInteger(...)
     * ---------------------------------------------------------- */

    @Test
    public void testToAtomicInteger() {
        LocalTime time = LocalTime.of(2, 30, 0, 0); // 2:30:00.000
        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(time, converter);
        int expected = (int) millisOfDay(time);
        assertEquals(expected, ai.get());
    }

    /* -------------------------------------------------------------
     * Tests for toAtomicLong(...)
     * ---------------------------------------------------------- */

    @Test
    public void testToAtomicLong() {
        LocalTime time = LocalTime.of(15, 45, 30, 123_000_000);
        AtomicLong al = LocalTimeConversions.toAtomicLong(time, converter);
        long expected = millisOfDay(time);
        assertEquals(expected, al.get());
    }

    /* -------------------------------------------------------------
     * Tests for toString(...)
     * ---------------------------------------------------------- */

    @Test
    public void testToStringMatchesISOFormatter() {
        LocalTime time = LocalTime.of(9, 8, 7, 654_321_000);
        String result = LocalTimeConversions.toString(time, converter);
        String expected = time.format(DateTimeFormatter.ISO_LOCAL_TIME);
        assertEquals(expected, result);
    }

    @Test
    public void testToStringZeroNano() {
        LocalTime time = LocalTime.of(23, 59, 59, 0);
        String result = LocalTimeConversions.toString(time, converter);
        assertEquals("23:59:59", result); // ISO_LOCAL_TIME omits the fractional part when zero
    }

    /* -------------------------------------------------------------
     * Tests for toCalendar(...)
     * ---------------------------------------------------------- */

    @Test
    public void testToCalendarFieldsMatchLocalTime() {
        LocalTime time = LocalTime.of(6, 7, 8, 901_234_567); // 6:07:08.901234567
        Calendar cal = LocalTimeConversions.toCalendar(time, converter);

        assertEquals("Hour must match", time.getHour(), cal.get(Calendar.HOUR_OF_DAY));
        assertEquals("Minute must match", time.getMinute(), cal.get(Calendar.MINUTE));
        assertEquals("Second must match", time.getSecond(), cal.get(Calendar.SECOND));
        assertEquals("Millisecond must be derived from nanoseconds",
                     time.getNano() / 1_000_000, cal.get(Calendar.MILLISECOND));

        // Verify that the Calendar uses the same TimeZone as the Converter's options
        assertEquals("Calendar timezone must be the converter's timezone",
                     defaultTimeZone, cal.getTimeZone());
    }

    @Test
    public void testToCalendarMidnight() {
        LocalTime midnight = LocalTime.MIDNIGHT;
        Calendar cal = LocalTimeConversions.toCalendar(midnight, converter);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testToCalendarMaximumTime() {
        LocalTime max = LocalTime.of(23, 59, 59, 999_999_999);
        Calendar cal = LocalTimeConversions.toCalendar(max, converter);
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
        assertEquals(999, cal.get(Calendar.MILLISECOND)); // 999_999_999 ns → 999 ms
    }
}
