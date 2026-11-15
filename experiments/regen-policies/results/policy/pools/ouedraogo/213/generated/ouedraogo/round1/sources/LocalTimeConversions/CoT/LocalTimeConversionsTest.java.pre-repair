package com.cedarsoftware.util.convert;

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
 * JUnit‑4 test suite for {@link LocalTimeConversions}.
 *
 * The tests are placed in the same package so that the package‑private static
 * conversion methods are directly accessible.
 *
 * The test class follows the step‑by‑step plan requested:
 *   1. List all conversion methods (see comments below).
 *   2. Provide a basic functional test for each method.
 *   3. Identify edge‑cases (null input, midnight, maximal time, overflow
 *      for integer conversion, etc.) and exception scenarios.
 *   4. Add dedicated tests for those edge‑cases.
 *   5. Combine everything into a single, compilable JUnit‑4 source file.
 */
public class LocalTimeConversionsTest {

    // -----------------------------------------------------------------------
    // 1. PUBLIC (actually package‑private) conversion method signatures
    // -----------------------------------------------------------------------
    // static Map<String, Object> toMap(Object from, Converter converter)
    // static int               toInteger(Object from, Converter converter)
    // static long              toLong(Object from, Converter converter)
    // static double            toDouble(Object from, Converter converter)
    // static BigInteger        toBigInteger(Object from, Converter converter)
    // static BigDecimal        toBigDecimal(Object from, Converter converter)
    // static AtomicInteger     toAtomicInteger(Object from, Converter converter)
    // static AtomicLong        toAtomicLong(Object from, Converter converter)
    // static String            toString(Object from, Converter converter)
    // static Calendar          toCalendar(Object from, Converter converter)

    private Converter converter;               // a real Converter instance
    private ConverterOptions defaultOptions;   // holds the TimeZone used by toCalendar

    @Before
    public void setUp() {
        // The concrete implementation of ConverterOptions is not shown,
        // but the library provides a public no‑arg constructor.
        defaultOptions = new ConverterOptions();
        defaultOptions.setTimeZone(TimeZone.getTimeZone("UTC"));
        converter = new Converter(defaultOptions);
    }

    // -----------------------------------------------------------------------
    // 2. BASIC functional tests for each conversion method
    // -----------------------------------------------------------------------

    @Test
    public void testToMap_basic() {
        LocalTime time = LocalTime.of(12, 34, 56, 123_456_789);
        Map<String, Object> map = LocalTimeConversions.toMap(time, converter);

        assertNotNull(map);
        assertEquals(1, map.size());
        // The key is defined in MapConversions.LOCAL_TIME which is not visible here,
        // therefore we just verify the stored value.
        assertTrue(map.containsValue(time.toString()));
    }

    @Test
    public void testToInteger_basic() {
        LocalTime time = LocalTime.of(01, 02, 03, 004_005_006);
        int expected = (int) (time.toNanoOfDay() / 1_000_000L); // milliseconds of day
        assertEquals(expected, LocalTimeConversions.toInteger(time, converter));
    }

    @Test
    public void testToLong_basic() {
        LocalTime time = LocalTime.of(23, 59, 58, 999_999_999);
        long expected = time.toNanoOfDay() / 1_000_000L; // milliseconds of day
        assertEquals(expected, LocalTimeConversions.toLong(time, converter));
    }

    @Test
    public void testToDouble_basic() {
        LocalTime time = LocalTime.of(12, 0, 0, 0);
        double expected = time.toNanoOfDay() / 1_000_000_000.0; // seconds of day as double
        assertEquals(expected, LocalTimeConversions.toDouble(time, converter), 0.0);
    }

    @Test
    public void testToBigInteger_basic() {
        LocalTime time = LocalTime.of(6, 30, 15, 250_000_000);
        BigInteger expected = BigInteger.valueOf(time.toNanoOfDay());
        assertEquals(expected, LocalTimeConversions.toBigInteger(time, converter));
    }

    @Test
    public void testToBigDecimal_basic() {
        LocalTime time = LocalTime.of(6, 30, 15, 250_000_000);
        // 9 decimal places are forced by the conversion implementation
        BigDecimal expected = new BigDecimal(time.toNanoOfDay())
                .divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        assertEquals(expected, LocalTimeConversions.toBigDecimal(time, converter));
    }

    @Test
    public void testToAtomicInteger_basic() {
        LocalTime time = LocalTime.of(00, 00, 01, 000_000_000); // 1 second = 1000 ms
        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(time, converter);
        assertEquals(1000, ai.get());
    }

    @Test
    public void testToAtomicLong_basic() {
        LocalTime time = LocalTime.of(00, 00, 01, 500_000_000); // 1.5 seconds = 1500 ms
        AtomicLong al = LocalTimeConversions.toAtomicLong(time, converter);
        assertEquals(1500L, al.get());
    }

    @Test
    public void testToString_basic() {
        LocalTime time = LocalTime.of(13, 45, 30, 123_000_000);
        String expected = time.format(DateTimeFormatter.ISO_LOCAL_TIME);
        assertEquals(expected, LocalTimeConversions.toString(time, converter));
    }

    @Test
    public void testToCalendar_basic() {
        LocalTime time = LocalTime.of(09, 15, 45, 987_654_321);
        Calendar cal = LocalTimeConversions.toCalendar(time, converter);

        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(time.getHour(), cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(time.getMinute(), cal.get(Calendar.MINUTE));
        assertEquals(time.getSecond(), cal.get(Calendar.SECOND));
        assertEquals(time.getNano() / 1_000_000, cal.get(Calendar.MILLISECOND));
    }

    // -----------------------------------------------------------------------
    // 3. Edge‑case & exception scenarios
    // -----------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testToMap_nullInput() {
        LocalTimeConversions.toMap(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToInteger_nullInput() {
        LocalTimeConversions.toInteger(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_nullInput() {
        LocalTimeConversions.toLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_nullInput() {
        LocalTimeConversions.toDouble(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_nullInput() {
        LocalTimeConversions.toBigInteger(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_nullInput() {
        LocalTimeConversions.toBigDecimal(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_nullInput() {
        LocalTimeConversions.toAtomicInteger(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_nullInput() {
        LocalTimeConversions.toAtomicLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_nullInput() {
        LocalTimeConversions.toString(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_nullInput() {
        LocalTimeConversions.toCalendar(null, converter);
    }

    // Edge case: midnight (00:00) – should give zero for numeric conversions
    @Test
    public void testMidnightConversions() {
        LocalTime midnight = LocalTime.MIDNIGHT;

        assertEquals(0, LocalTimeConversions.toInteger(midnight, converter));
        assertEquals(0L, LocalTimeConversions.toLong(midnight, converter));
        assertEquals(0.0, LocalTimeConversions.toDouble(midnight, converter), 0.0);
        assertEquals(BigInteger.ZERO, LocalTimeConversions.toBigInteger(midnight, converter));
        assertEquals(BigDecimal.ZERO.setScale(9), LocalTimeConversions.toBigDecimal(midnight, converter));
        assertEquals(0, LocalTimeConversions.toAtomicInteger(midnight, converter).get());
        assertEquals(0L, LocalTimeConversions.toAtomicLong(midnight, converter).get());
        assertEquals("00:00", LocalTimeConversions.toString(midnight, converter));
        Calendar cal = LocalTimeConversions.toCalendar(midnight, converter);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    // Edge case: maximum representable LocalTime (23:59:59.999999999)
    @Test
    public void testMaxTimeConversions() {
        LocalTime max = LocalTime.MAX; // 23:59:59.999999999

        // Expected numeric values
        long nanoOfDay = max.toNanoOfDay();                     // 86_399_999_999_999_999
        long milliOfDay = nanoOfDay / 1_000_000L;                // 86_399_999_999
        double seconds = nanoOfDay / 1_000_000_000.0;           // 86_399.999999999

        assertEquals((int) milliOfDay, LocalTimeConversions.toInteger(max, converter));
        assertEquals(milliOfDay, LocalTimeConversions.toLong(max, converter));
        assertEquals(seconds, LocalTimeConversions.toDouble(max, converter), 0.0);
        assertEquals(BigInteger.valueOf(nanoOfDay), LocalTimeConversions.toBigInteger(max, converter));

        BigDecimal expectedBD = new BigDecimal(nanoOfDay)
                .divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        assertEquals(expectedBD, LocalTimeConversions.toBigDecimal(max, converter));

        assertEquals((int) milliOfDay, LocalTimeConversions.toAtomicInteger(max, converter).get());
        assertEquals(milliOfDay, LocalTimeConversions.toAtomicLong(max, converter).get());

        assertEquals(max.format(DateTimeFormatter.ISO_LOCAL_TIME), LocalTimeConversions.toString(max, converter));

        Calendar cal = LocalTimeConversions.toCalendar(max, converter);
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
        // 999_999_999 nanoseconds -> 999 milliseconds (truncated)
        assertEquals(999, cal.get(Calendar.MILLISECOND));
    }

    // Edge case: verify that the Calendar respects the Converter's TimeZone
    @Test
    public void testToCalendar_respectsTimeZone() {
        // Change the converter's timezone to America/New_York
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        Converter nyConverter = new Converter(options);

        LocalTime time = LocalTime.of(12, 0);
        Calendar cal = LocalTimeConversions.toCalendar(time, nyConverter);

        assertEquals(TimeZone.getTimeZone("America/New_York"), cal.getTimeZone());
        // The hour/minute/second fields must still reflect the LocalTime values
        assertEquals(12, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
    }
}
