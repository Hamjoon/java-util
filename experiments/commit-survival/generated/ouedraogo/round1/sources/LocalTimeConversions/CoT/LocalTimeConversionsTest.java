package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
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

public class LocalTimeConversionsTest {

    /** A Converter instance that can be used for the methods that need it.
     *  The real library supplies ConverterOptions – we rely on its default
     *  constructor which provides a default TimeZone. */
    private Converter converter;

    @Before
    public void setUp() {
        // The library supplies a no‑arg constructor for ConverterOptions.
        // If it does not, the test will fail to compile, which indicates that
        // the real library must be on the test class‑path.
        ConverterOptions options = new ConverterOptions(); // default options
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- *
     *  1.  Extracted public (actually package‑private static) method signatures
     * --------------------------------------------------------------------- *
     *
     * static Map<String,Object> toMap(Object from, Converter converter)
     * static int               toInteger(Object from, Converter converter)
     * static long              toLong(Object from, Converter converter)
     * static double            toDouble(Object from, Converter converter)
     * static BigInteger        toBigInteger(Object from, Converter converter)
     * static BigDecimal        toBigDecimal(Object from, Converter converter)
     * static AtomicInteger     toAtomicInteger(Object from, Converter converter)
     * static AtomicLong        toAtomicLong(Object from, Converter converter)
     * static String            toString(Object from, Converter converter)
     * static Calendar          toCalendar(Object from, Converter converter)
     *
     * --------------------------------------------------------------------- */

    /* --------------------------------------------------------------------- *
     *  2.  Basic functional tests for each method
     * --------------------------------------------------------------------- */

    @Test
    public void testToMap_basic() {
        LocalTime lt = LocalTime.of(12, 34, 56, 123_456_789);
        Map<String, Object> map = LocalTimeConversions.toMap(lt, converter);
        assertNotNull(map);
        assertEquals(1, map.size());

        // The key used by the implementation is MapConversions.TIME.
        // We cannot reference that constant directly, so we inspect the entry.
        String key = map.keySet().iterator().next();
        assertEquals(lt.toString(), map.get(key));
    }

    @Test
    public void testToInteger_basic() {
        LocalTime lt = LocalTime.of(01, 02, 03, 456_000_000); // 1:02:03.456
        int expected = (int) (lt.toNanoOfDay() / 1_000_000L);
        assertEquals(expected, LocalTimeConversions.toInteger(lt, converter));
    }

    @Test
    public void testToLong_basic() {
        LocalTime lt = LocalTime.of(23, 59, 59, 999_000_000);
        long expected = lt.toNanoOfDay() / 1_000_000L;
        assertEquals(expected, LocalTimeConversions.toLong(lt, converter));
    }

    @Test
    public void testToDouble_basic() {
        LocalTime lt = LocalTime.of(12, 0, 0, 0);
        double expected = lt.toNanoOfDay() / 1_000_000_000.0;
        assertEquals(expected, LocalTimeConversions.toDouble(lt, converter), 1e-12);
    }

    @Test
    public void testToBigInteger_basic() {
        LocalTime lt = LocalTime.of(0, 0, 0, 1);
        BigInteger expected = BigInteger.valueOf(lt.toNanoOfDay());
        assertEquals(expected, LocalTimeConversions.toBigInteger(lt, converter));
    }

    @Test
    public void testToBigDecimal_basic() {
        LocalTime lt = LocalTime.of(0, 0, 0, 123_456_789);
        BigDecimal expected = new BigDecimal(lt.toNanoOfDay())
                .divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(LocalTimeConversions.toBigDecimal(lt, converter)));
    }

    @Test
    public void testToAtomicInteger_basic() {
        LocalTime lt = LocalTime.of(0, 0, 1, 0); // 1 second = 1000 ms
        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(lt, converter);
        assertEquals(1000, ai.get());
    }

    @Test
    public void testToAtomicLong_basic() {
        LocalTime lt = LocalTime.of(0, 0, 1, 0);
        AtomicLong al = LocalTimeConversions.toAtomicLong(lt, converter);
        assertEquals(1000L, al.get());
    }

    @Test
    public void testToString_basic() {
        LocalTime lt = LocalTime.of(15, 30, 45, 123_000_000);
        String expected = lt.format(DateTimeFormatter.ISO_LOCAL_TIME);
        assertEquals(expected, LocalTimeConversions.toString(lt, converter));
    }

    @Test
    public void testToCalendar_basic() {
        LocalTime lt = LocalTime.of(10, 20, 30, 400_000_000);
        Calendar cal = LocalTimeConversions.toCalendar(lt, converter);
        assertEquals(lt.getHour(), cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(lt.getMinute(), cal.get(Calendar.MINUTE));
        assertEquals(lt.getSecond(), cal.get(Calendar.SECOND));
        assertEquals(lt.getNano() / 1_000_000, cal.get(Calendar.MILLISECOND));
        // The date part is the current date in the converter's time zone – we only
        // verify that the fields we set are correct.
    }

    /* --------------------------------------------------------------------- *
     *  3.  Edge‑case & exception tests
     * --------------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToMap_nullFrom() {
        LocalTimeConversions.toMap(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInteger_nullFrom() {
        LocalTimeConversions.toInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_nullFrom() {
        LocalTimeConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_nullFrom() {
        LocalTimeConversions.toDouble(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_nullFrom() {
        LocalTimeConversions.toBigInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_nullFrom() {
        LocalTimeConversions.toBigDecimal(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_nullFrom() {
        LocalTimeConversions.toAtomicInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_nullFrom() {
        LocalTimeConversions.toAtomicLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_nullFrom() {
        LocalTimeConversions.toString(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCalendar_nullFrom() {
        LocalTimeConversions.toCalendar(null, converter);
    }

    @Test
    public void testMidnightConversions() {
        LocalTime midnight = LocalTime.MIDNIGHT; // 00:00
        assertEquals(0, LocalTimeConversions.toInteger(midnight, converter));
        assertEquals(0L, LocalTimeConversions.toLong(midnight, converter));
        assertEquals(0.0, LocalTimeConversions.toDouble(midnight, converter), 0.0);
        assertEquals(BigInteger.ZERO, LocalTimeConversions.toBigInteger(midnight, converter));
        assertEquals(BigDecimal.ZERO.setScale(9), LocalTimeConversions.toBigDecimal(midnight, converter));
        assertEquals("00:00", LocalTimeConversions.toString(midnight, converter));
        Calendar cal = LocalTimeConversions.toCalendar(midnight, converter);
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal.get(Calendar.MINUTE));
        assertEquals(0, cal.get(Calendar.SECOND));
        assertEquals(0, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testMaximumTimeConversions() {
        // 23:59:59.999999999 – the maximum LocalTime value
        LocalTime max = LocalTime.of(23, 59, 59, 999_999_999);
        long nanoOfDay = max.toNanoOfDay(); // 86_399_999_999_999_999

        // integer (milliseconds) should be within int range
        int expectedMs = (int) (nanoOfDay / 1_000_000L);
        assertEquals(expectedMs, LocalTimeConversions.toInteger(max, converter));

        long expectedLong = nanoOfDay / 1_000_000L;
        assertEquals(expectedLong, LocalTimeConversions.toLong(max, converter));

        double expectedDouble = nanoOfDay / 1_000_000_000.0;
        assertEquals(expectedDouble, LocalTimeConversions.toDouble(max, converter), 1e-12);

        assertEquals(BigInteger.valueOf(nanoOfDay), LocalTimeConversions.toBigInteger(max, converter));

        BigDecimal expectedBd = new BigDecimal(nanoOfDay)
                .divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        assertEquals(0, expectedBd.compareTo(LocalTimeConversions.toBigDecimal(max, converter)));
    }

    @Test
    public void testAtomicIntegerAndLongBoundaries() {
        LocalTime lt = LocalTime.of(0, 0, 0, 0);
        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(lt, converter);
        assertEquals(0, ai.get());

        AtomicLong al = LocalTimeConversions.toAtomicLong(lt, converter);
        assertEquals(0L, al.get());
    }

    @Test
    public void testToMap_usesCompactLinkedMap() {
        LocalTime lt = LocalTime.of(1, 1);
        Map<String, Object> map = LocalTimeConversions.toMap(lt, converter);
        assertTrue(map instanceof CompactLinkedMap);
    }
}
