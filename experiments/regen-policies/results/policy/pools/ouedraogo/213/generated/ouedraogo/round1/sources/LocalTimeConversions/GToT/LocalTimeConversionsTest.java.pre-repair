/*********************************************************************
 *  JUnit‑4 test suite for com.cedarsoftware.util.convert.LocalTimeConversions
 *
 *  The tests cover:
 *   • Extraction of public (package‑private static) methods and their signatures.
 *   • Basic functional tests for each conversion method.
 *   • Edge‑case and exception tests (null arguments, extreme LocalTime values,
 *     rounding behaviour, etc.).
 *
 *  The test class lives in the same package as the class under test so that it
 *  can access the package‑private static methods.
 *********************************************************************/

package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class LocalTimeConversionsTest {

    /** Converter instance required by the conversion methods */
    private Converter converter;

    @Before
    public void setUp() {
        // The concrete implementation of ConverterOptions is not shown,
        // but a no‑arg constructor is available according to the public API.
        // Using the default options (including the default TimeZone) is fine
        // for the assertions we perform.
        converter = new Converter(new ConverterOptions());
    }

    /* -----------------------------------------------------------------
     *  1.  toMap
     * ----------------------------------------------------------------- */

    @Test
    public void testToMap_Basic() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_000_000); // 13:45:30.123
        Map<String, Object> map = LocalTimeConversions.toMap(lt, converter);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals(lt.toString(), map.get(MapConversions.LOCAL_TIME));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullArgument() {
        LocalTimeConversions.toMap(null, converter);
    }

    /* -----------------------------------------------------------------
     *  2.  toInteger
     * ----------------------------------------------------------------- */

    @Test
    public void testToInteger_Basic() {
        LocalTime lt = LocalTime.of(0, 0, 1, 0); // 1 second = 1 000 ms
        int ms = LocalTimeConversions.toInteger(lt, converter);
        assertEquals(1_000, ms);
    }

    @Test
    public void testToInteger_EdgeStartOfDay() {
        LocalTime lt = LocalTime.MIDNIGHT; // 00:00:00.000000000
        assertEquals(0, LocalTimeConversions.toInteger(lt, converter));
    }

    @Test
    public void testToInteger_EdgeEndOfDay() {
        // 23:59:59.999999999 -> 86 399 999 ms (the fraction is truncated)
        LocalTime lt = LocalTime.of(23, 59, 59, 999_999_999);
        int ms = LocalTimeConversions.toInteger(lt, converter);
        assertEquals(86_399_999, ms);
    }

    @Test(expected = NullPointerException.class)
    public void testToInteger_NullArgument() {
        LocalTimeConversions.toInteger(null, converter);
    }

    /* -----------------------------------------------------------------
     *  3.  toLong
     * ----------------------------------------------------------------- */

    @Test
    public void testToLong_Basic() {
        LocalTime lt = LocalTime.of(12, 0, 0, 0); // 12 hours = 43 200 000 ms
        long ms = LocalTimeConversions.toLong(lt, converter);
        assertEquals(43_200_000L, ms);
    }

    @Test
    public void testToLong_EdgeEndOfDay() {
        LocalTime lt = LocalTime.of(23, 59, 59, 999_999_999);
        long ms = LocalTimeConversions.toLong(lt, converter);
        assertEquals(86_399_999L, ms);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullArgument() {
        LocalTimeConversions.toLong(null, converter);
    }

    /* -----------------------------------------------------------------
     *  4.  toDouble
     * ----------------------------------------------------------------- */

    @Test
    public void testToDouble_Basic() {
        LocalTime lt = LocalTime.of(1, 0, 0, 0); // 1 hour = 3 600 seconds
        double seconds = LocalTimeConversions.toDouble(lt, converter);
        assertEquals(3_600.0, seconds, 0.000_000_001);
    }

    @Test
    public void testToDouble_EdgeNanosecondPrecision() {
        LocalTime lt = LocalTime.of(0, 0, 0, 1); // one nanosecond
        double seconds = LocalTimeConversions.toDouble(lt, converter);
        assertEquals(0.000_000_001, seconds, 0.0);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullArgument() {
        LocalTimeConversions.toDouble(null, converter);
    }

    /* -----------------------------------------------------------------
     *  5.  toBigInteger
     * ----------------------------------------------------------------- */

    @Test
    public void testToBigInteger_Basic() {
        LocalTime lt = LocalTime.of(0, 0, 0, 1_234_567_890);
        BigInteger bi = LocalTimeConversions.toBigInteger(lt, converter);
        assertEquals(BigInteger.valueOf(1_234_567_890L), bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullArgument() {
        LocalTimeConversions.toBigInteger(null, converter);
    }

    /* -----------------------------------------------------------------
     *  6.  toBigDecimal
     * ----------------------------------------------------------------- */

    @Test
    public void testToBigDecimal_Basic() {
        LocalTime lt = LocalTime.of(0, 0, 1, 500_000_000); // 1.5 seconds
        BigDecimal bd = LocalTimeConversions.toBigDecimal(lt, converter);
        // Expected: 1.5 seconds = 1.5 (scale 9) => 1.500000000
        assertEquals(new BigDecimal("1.500000000"), bd);
    }

    @Test
    public void testToBigDecimal_Rounding() {
        // 1 nanosecond -> 0.000000001 (scale 9)
        LocalTime lt = LocalTime.of(0, 0, 0, 1);
        BigDecimal bd = LocalTimeConversions.toBigDecimal(lt, converter);
        assertEquals(new BigDecimal("0.000000001"), bd);
        assertEquals(9, bd.scale());
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullArgument() {
        LocalTimeConversions.toBigDecimal(null, converter);
    }

    /* -----------------------------------------------------------------
     *  7.  toAtomicInteger
     * ----------------------------------------------------------------- */

    @Test
    public void testToAtomicInteger_Basic() {
        LocalTime lt = LocalTime.of(0, 0, 2, 0); // 2 seconds = 2000 ms
        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(lt, converter);
        assertEquals(2000, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullArgument() {
        LocalTimeConversions.toAtomicInteger(null, converter);
    }

    /* -----------------------------------------------------------------
     *  8.  toAtomicLong
     * ----------------------------------------------------------------- */

    @Test
    public void testToAtomicLong_Basic() {
        LocalTime lt = LocalTime.of(0, 5, 0, 0); // 5 minutes = 300 000 ms
        AtomicLong al = LocalTimeConversions.toAtomicLong(lt, converter);
        assertEquals(300_000L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullArgument() {
        LocalTimeConversions.toAtomicLong(null, converter);
    }

    /* -----------------------------------------------------------------
     *  9.  toString
     * ----------------------------------------------------------------- */

    @Test
    public void testToString_WithFraction() {
        LocalTime lt = LocalTime.of(12, 34, 56, 123_000_000); // 12:34:56.123
        String s = LocalTimeConversions.toString(lt, converter);
        assertEquals("12:34:56.123", s);
    }

    @Test
    public void testToString_WithoutFraction() {
        LocalTime lt = LocalTime.of(9, 8, 7, 0); // 09:08:07
        String s = LocalTimeConversions.toString(lt, converter);
        assertEquals("09:08:07", s);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullArgument() {
        LocalTimeConversions.toString(null, converter);
    }

    /* -----------------------------------------------------------------
     * 10. toCalendar
     * ----------------------------------------------------------------- */

    @Test
    public void testToCalendar_Basic() {
        LocalTime lt = LocalTime.of(15, 30, 45, 250_000_000); // 15:30:45.250
        Calendar cal = LocalTimeConversions.toCalendar(lt, converter);
        assertEquals(15, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal.get(Calendar.MINUTE));
        assertEquals(45, cal.get(Calendar.SECOND));
        assertEquals(250, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testToCalendar_EdgeStartOfDay() {
        LocalTime lt = LocalTime.MIDNIGHT;
        Calendar cal = LocalTimeConversions.toCalendar(lt, converter);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullArgument() {
        LocalTimeConversions.toCalendar(null, converter);
    }

    /* -----------------------------------------------------------------
     *  Helper – confirm that all expected signatures are present.
     * ----------------------------------------------------------------- */

    @Test
    public void testMethodSignaturesExist() throws NoSuchMethodException {
        // The class is package‑private; we use reflection on the declared methods.
        Class<?> cls = LocalTimeConversions.class;

        cls.getDeclaredMethod("toMap", Object.class, Converter.class);
        cls.getDeclaredMethod("toInteger", Object.class, Converter.class);
        cls.getDeclaredMethod("toLong", Object.class, Converter.class);
        cls.getDeclaredMethod("toDouble", Object.class, Converter.class);
        cls.getDeclaredMethod("toBigInteger", Object.class, Converter.class);
        cls.getDeclaredMethod("toBigDecimal", Object.class, Converter.class);
        cls.getDeclaredMethod("toAtomicInteger", Object.class, Converter.class);
        cls.getDeclaredMethod("toAtomicLong", Object.class, Converter.class);
        cls.getDeclaredMethod("toString", Object.class, Converter.class);
        cls.getDeclaredMethod("toCalendar", Object.class, Converter.class);
    }
}
