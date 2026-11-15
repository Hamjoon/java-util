package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class LocalTimeConversionsTest {

    /* -------------------------------------------------------------
     * Helper – a simple Converter instance with default options.
     * ------------------------------------------------------------- */
    private static Converter createDefaultConverter() {
        // The library provides a no‑arg constructor for ConverterOptions.
        // The default options use the JVM default TimeZone which is fine for our tests.
        ConverterOptions options = new ConverterOptions();
        return new Converter(options);
    }

    /* -------------------------------------------------------------
     * 1.  toMap()
     * ------------------------------------------------------------- */
    @Test
    public void testToMap_Basic() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        Converter conv = createDefaultConverter();

        Map<String, Object> map = LocalTimeConversions.toMap(lt, conv);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey("time") || map.containsKey("TIME")); // defensive – actual key is MapConversions.TIME
        assertEquals(lt.toString(), map.values().iterator().next());
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_NullFrom() {
        Converter conv = createDefaultConverter();
        LocalTimeConversions.toMap(null, conv);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullConverter() {
        LocalTime lt = LocalTime.now();
        LocalTimeConversions.toMap(lt, null);
    }

    /* -------------------------------------------------------------
     * 2.  toInteger()
     * ------------------------------------------------------------- */
    @Test
    public void testToInteger_Basic() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        Converter conv = createDefaultConverter();

        int ms = LocalTimeConversions.toInteger(lt, conv);
        // 13:45:30.123456789 → nanoseconds = 49_530_123_456_789 → milliseconds = 49_530_123
        assertEquals(49_530_123, ms);
    }

    @Test
    public void testToInteger_Edge_Min() {
        LocalTime lt = LocalTime.MIN;               // 00:00
        Converter conv = createDefaultConverter();

        assertEquals(0, LocalTimeConversions.toInteger(lt, conv));
    }

    @Test
    public void testToInteger_Edge_Max() {
        LocalTime lt = LocalTime.MAX;               // 23:59:59.999999999
        Converter conv = createDefaultConverter();

        // 86_399_999_999_999 ns → milliseconds = 86_399_999
        assertEquals(86_399_999, LocalTimeConversions.toInteger(lt, conv));
    }

    @Test(expected = ClassCastException.class)
    public void testToInteger_NullFrom() {
        Converter conv = createDefaultConverter();
        LocalTimeConversions.toInteger(null, conv);
    }

    /* -------------------------------------------------------------
     * 3.  toLong()
     * ------------------------------------------------------------- */
    @Test
    public void testToLong_Basic() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        Converter conv = createDefaultConverter();

        long ms = LocalTimeConversions.toLong(lt, conv);
        assertEquals(49_530_123L, ms);
    }

    @Test
    public void testToLong_Edge_Max() {
        LocalTime lt = LocalTime.MAX;
        Converter conv = createDefaultConverter();

        assertEquals(86_399_999L, LocalTimeConversions.toLong(lt, conv));
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_NullFrom() {
        Converter conv = createDefaultConverter();
        LocalTimeConversions.toLong(null, conv);
    }

    /* -------------------------------------------------------------
     * 4.  toDouble()
     * ------------------------------------------------------------- */
    @Test
    public void testToDouble_Basic() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        Converter conv = createDefaultConverter();

        double seconds = LocalTimeConversions.toDouble(lt, conv);
        // Expected: 49_530_123_456_789 ns / 1_000_000_000 = 49.530123456789
        assertEquals(49.530123456789, seconds, 1e-12);
    }

    @Test
    public void testToDouble_Edge_Max() {
        LocalTime lt = LocalTime.MAX;
        Converter conv = createDefaultConverter();

        double seconds = LocalTimeConversions.toDouble(lt, conv);
        assertEquals(86_399.999999999, seconds, 1e-12);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_NullFrom() {
        Converter conv = createDefaultConverter();
        LocalTimeConversions.toDouble(null, conv);
    }

    /* -------------------------------------------------------------
     * 5.  toBigInteger()
     * ------------------------------------------------------------- */
    @Test
    public void testToBigInteger_Basic() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        Converter conv = createDefaultConverter();

        BigInteger bi = LocalTimeConversions.toBigInteger(lt, conv);
        assertEquals(BigInteger.valueOf(49_530_123_456_789L), bi);
    }

    @Test
    public void testToBigInteger_Edge_Max() {
        LocalTime lt = LocalTime.MAX;
        Converter conv = createDefaultConverter();

        BigInteger bi = LocalTimeConversions.toBigInteger(lt, conv);
        assertEquals(new BigInteger("86399999999999"), bi);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_NullFrom() {
        Converter conv = createDefaultConverter();
        LocalTimeConversions.toBigInteger(null, conv);
    }

    /* -------------------------------------------------------------
     * 6.  toBigDecimal()
     * ------------------------------------------------------------- */
    @Test
    public void testToBigDecimal_Basic() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        Converter conv = createDefaultConverter();

        BigDecimal bd = LocalTimeConversions.toBigDecimal(lt, conv);
        // Rounded to 9 decimal places → 49.530123457
        assertEquals(new BigDecimal("49.530123457"), bd);
    }

    @Test
    public void testToBigDecimal_Edge_Max() {
        LocalTime lt = LocalTime.MAX;
        Converter conv = createDefaultConverter();

        BigDecimal bd = LocalTimeConversions.toBigDecimal(lt, conv);
        assertEquals(new BigDecimal("86399.999999999"), bd);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_NullFrom() {
        Converter conv = createDefaultConverter();
        LocalTimeConversions.toBigDecimal(null, conv);
    }

    /* -------------------------------------------------------------
     * 7.  toAtomicInteger()
     * ------------------------------------------------------------- */
    @Test
    public void testToAtomicInteger_Basic() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        Converter conv = createDefaultConverter();

        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(lt, conv);
        assertEquals(49_530_123, ai.get());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_NullFrom() {
        Converter conv = createDefaultConverter();
        LocalTimeConversions.toAtomicInteger(null, conv);
    }

    /* -------------------------------------------------------------
     * 8.  toAtomicLong()
     * ------------------------------------------------------------- */
    @Test
    public void testToAtomicLong_Basic() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        Converter conv = createDefaultConverter();

        AtomicLong al = LocalTimeConversions.toAtomicLong(lt, conv);
        assertEquals(49_530_123L, al.get());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_NullFrom() {
        Converter conv = createDefaultConverter();
        LocalTimeConversions.toAtomicLong(null, conv);
    }

    /* -------------------------------------------------------------
     * 9.  toString()
     * ------------------------------------------------------------- */
    @Test
    public void testToString_Basic() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        Converter conv = createDefaultConverter();

        String s = LocalTimeConversions.toString(lt, conv);
        assertEquals("13:45:30.123456789", s);
    }

    @Test
    public void testToString_Edge_Min() {
        LocalTime lt = LocalTime.MIN;
        Converter conv = createDefaultConverter();

        assertEquals("00:00", LocalTimeConversions.toString(lt, conv));
    }

    @Test(expected = ClassCastException.class)
    public void testToString_NullFrom() {
        Converter conv = createDefaultConverter();
        LocalTimeConversions.toString(null, conv);
    }

    /* -------------------------------------------------------------
     * 10. toCalendar()
     * ------------------------------------------------------------- */
    @Test
    public void testToCalendar_Basic() {
        LocalTime lt = LocalTime.of(13, 45, 30, 123_456_789);
        Converter conv = createDefaultConverter();

        Calendar cal = LocalTimeConversions.toCalendar(lt, conv);
        assertNotNull(cal);
        assertEquals(13, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(45, cal.get(Calendar.MINUTE));
        assertEquals(30, cal.get(Calendar.SECOND));
        assertEquals(123, cal.get(Calendar.MILLISECOND)); // 123_456_789 ns → 123 ms
    }

    @Test
    public void testToCalendar_Edge_Max() {
        LocalTime lt = LocalTime.MAX; // 23:59:59.999999999
        Converter conv = createDefaultConverter();

        Calendar cal = LocalTimeConversions.toCalendar(lt, conv);
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(59, cal.get(Calendar.MINUTE));
        assertEquals(59, cal.get(Calendar.SECOND));
        assertEquals(999, cal.get(Calendar.MILLISECOND)); // truncated milliseconds
    }

    @Test(expected = ClassCastException.class)
    public void testToCalendar_NullFrom() {
        Converter conv = createDefaultConverter();
        LocalTimeConversions.toCalendar(null, conv);
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullConverter() {
        LocalTime lt = LocalTime.now();
        LocalTimeConversions.toCalendar(lt, null);
    }
}
