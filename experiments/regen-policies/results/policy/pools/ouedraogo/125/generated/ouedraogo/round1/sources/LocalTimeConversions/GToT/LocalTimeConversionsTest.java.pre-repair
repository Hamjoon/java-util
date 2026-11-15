package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

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
 *   • Normal behaviour for all conversion methods.
 *   • Edge‑case values (midnight and the maximum representable LocalTime).
 *   • Null and wrong‑type arguments that should raise exceptions.
 *
 * The test class resides in the same package as {@code LocalTimeConversions}
 * so it can access the package‑private static methods directly.
 */
public class LocalTimeConversionsTest {

    private Converter converter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // Create a Converter with a deterministic TimeZone (UTC) so Calendar‑based tests are repeatable.
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        converter = new Converter(options);
    }

    /* ---------------------------------------------------------------------- */
    /*  Helper – verify that a Calendar produced from a LocalTime contains   */
    /*  the same hour/minute/second/millisecond fields.                      */
    /* ---------------------------------------------------------------------- */
    private void assertCalendarMatches(LocalTime lt, Calendar cal) {
        assertEquals(lt.getHour(), cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(lt.getMinute(), cal.get(Calendar.MINUTE));
        assertEquals(lt.getSecond(), cal.get(Calendar.SECOND));
        assertEquals(lt.getNano() / 1_000_000, cal.get(Calendar.MILLISECOND));
    }

    /* ---------------------------------------------------------------------- */
    /*  1. toMap() – basic functionality and edge cases                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToMap() {
        LocalTime lt = LocalTime.of(12, 34, 56, 123_456_789);
        Map<String, Object> map = LocalTimeConversions.toMap(lt, converter);

        // MapConversions.TIME is defined in the library – we assume it equals "time".
        assertEquals(1, map.size());
        assertEquals(lt.toString(), map.get(MapConversions.TIME));
    }

    @Test
    public void testToMapMidnight() {
        LocalTime lt = LocalTime.MIDNIGHT;
        Map<String, Object> map = LocalTimeConversions.toMap(lt, converter);
        assertEquals(lt.toString(), map.get(MapConversions.TIME));
    }

    @Test
    public void testToMapMaximum() {
        LocalTime lt = LocalTime.of(23, 59, 59, 999_999_999);
        Map<String, Object> map = LocalTimeConversions.toMap(lt, converter);
        assertEquals(lt.toString(), map.get(MapConversions.TIME));
    }

    /* ---------------------------------------------------------------------- */
    /*  2. toInteger() – milliseconds of day (int)                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToInteger() {
        LocalTime lt = LocalTime.of(01, 02, 03, 004_005_000); // 1:02:03.004005
        int expected = (int) (lt.toNanoOfDay() / 1_000_000L);
        assertEquals(expected, LocalTimeConversions.toInteger(lt, converter));
    }

    @Test
    public void testToIntegerEdgeMidnight() {
        assertEquals(0, LocalTimeConversions.toInteger(LocalTime.MIDNIGHT, converter));
    }

    @Test
    public void testToIntegerEdgeMaximum() {
        // The maximum value fits into an int because it is less than Integer.MAX_VALUE (≈2.1B ms)
        int expected = (int) (LocalTime.MAX.toNanoOfDay() / 1_000_000L);
        assertEquals(expected, LocalTimeConversions.toInteger(LocalTime.MAX, converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  3. toLong() – milliseconds of day (long)                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToLong() {
        LocalTime lt = LocalTime.of(12, 34, 56, 789_000_000);
        long expected = lt.toNanoOfDay() / 1_000_000L;
        assertEquals(expected, LocalTimeConversions.toLong(lt, converter));
    }

    @Test
    public void testToLongMidnight() {
        assertEquals(0L, LocalTimeConversions.toLong(LocalTime.MIDNIGHT, converter));
    }

    @Test
    public void testToLongMaximum() {
        long expected = LocalTime.MAX.toNanoOfDay() / 1_000_000L;
        assertEquals(expected, LocalTimeConversions.toLong(LocalTime.MAX, converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  4. toDouble() – seconds of day (double)                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToDouble() {
        LocalTime lt = LocalTime.of(01, 00, 00, 500_000_000); // 1 hour + 0.5 sec
        double expected = lt.toNanoOfDay() / 1_000_000_000.0;
        assertEquals(expected, LocalTimeConversions.toDouble(lt, converter), 1e-12);
    }

    @Test
    public void testToDoubleMaximumPrecision() {
        LocalTime lt = LocalTime.of(23, 59, 59, 999_999_999);
        double expected = lt.toNanoOfDay() / 1_000_000_000.0;
        assertEquals(expected, LocalTimeConversions.toDouble(lt, converter), 1e-12);
    }

    /* ---------------------------------------------------------------------- */
    /*  5. toBigInteger() – nanoseconds of day                                   */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        LocalTime lt = LocalTime.of(12, 0, 0, 0);
        BigInteger expected = BigInteger.valueOf(lt.toNanoOfDay());
        assertEquals(expected, LocalTimeConversions.toBigInteger(lt, converter));
    }

    @Test
    public void testToBigIntegerMaximum() {
        BigInteger expected = BigInteger.valueOf(LocalTime.MAX.toNanoOfDay());
        assertEquals(expected, LocalTimeConversions.toBigInteger(LocalTime.MAX, converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  6. toBigDecimal() – seconds of day with 9‑scale rounding                */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToBigDecimal() {
        LocalTime lt = LocalTime.of(02, 30, 15, 250_000_000); // 2h30m15.250s
        BigDecimal expected = new BigDecimal(lt.toNanoOfDay())
                .divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        assertEquals(expected, LocalTimeConversions.toBigDecimal(lt, converter));
    }

    @Test
    public void testToBigDecimalMaximum() {
        BigDecimal expected = new BigDecimal(LocalTime.MAX.toNanoOfDay())
                .divide(LocalTimeConversions.BILLION, 9, java.math.RoundingMode.HALF_UP);
        assertEquals(expected, LocalTimeConversions.toBigDecimal(LocalTime.MAX, converter));
    }

    /* ---------------------------------------------------------------------- */
    /*  7. toAtomicInteger() – wraps the long‑to‑int conversion                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicInteger() {
        LocalTime lt = LocalTime.of(00, 00, 01, 234_000_000); // 1.234 seconds
        int expected = (int) LocalTimeConversions.toLong(lt, converter);
        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(lt, converter);
        assertEquals(expected, ai.get());
    }

    @Test
    public void testToAtomicIntegerMaximum() {
        int expected = (int) LocalTimeConversions.toLong(LocalTime.MAX, converter);
        AtomicInteger ai = LocalTimeConversions.toAtomicInteger(LocalTime.MAX, converter);
        assertEquals(expected, ai.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  8. toAtomicLong() – wraps the long conversion                           */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicLong() {
        LocalTime lt = LocalTime.of(10, 20, 30, 400_000_000);
        long expected = LocalTimeConversions.toLong(lt, converter);
        AtomicLong al = LocalTimeConversions.toAtomicLong(lt, converter);
        assertEquals(expected, al.get());
    }

    @Test
    public void testToAtomicLongMaximum() {
        long expected = LocalTimeConversions.toLong(LocalTime.MAX, converter);
        AtomicLong al = LocalTimeConversions.toAtomicLong(LocalTime.MAX, converter);
        assertEquals(expected, al.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  9. toString() – ISO‑8601 formatting                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToString() {
        LocalTime lt = LocalTime.of(13, 45, 59, 123_000_000);
        String expected = lt.format(DateTimeFormatter.ISO_LOCAL_TIME);
        assertEquals(expected, LocalTimeConversions.toString(lt, converter));
    }

    @Test
    public void testToStringMidnight() {
        assertEquals("00:00", LocalTimeConversions.toString(LocalTime.MIDNIGHT, converter));
    }

    @Test
    public void testToStringMaximum() {
        String expected = LocalTime.MAX.format(DateTimeFormatter.ISO_LOCAL_TIME);
        assertEquals(expected, LocalTimeConversions.toString(LocalTime.MAX, converter));
    }

    /* ---------------------------------------------------------------------- */
    /* 10. toCalendar() – Calendar fields match LocalTime                      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testToCalendar() {
        LocalTime lt = LocalTime.of(07, 08, 09, 987_654_321);
        Calendar cal = LocalTimeConversions.toCalendar(lt, converter);
        assertCalendarMatches(lt, cal);
    }

    @Test
    public void testToCalendarMidnight() {
        Calendar cal = LocalTimeConversions.toCalendar(LocalTime.MIDNIGHT, converter);
        assertCalendarMatches(LocalTime.MIDNIGHT, cal);
    }

    @Test
    public void testToCalendarMaximum() {
        Calendar cal = LocalTimeConversions.toCalendar(LocalTime.MAX, converter);
        assertCalendarMatches(LocalTime.MAX, cal);
    }

    /* ---------------------------------------------------------------------- */
    /* 11. Null handling – each method should throw NPE when given null.      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testNullInputAllMethods() {
        Object nullObj = null;

        // toMap
        thrown.expect(NullPointerException.class);
        LocalTimeConversions.toMap(nullObj, converter);
    }

    @Test
    public void testNullToInteger() {
        thrown.expect(NullPointerException.class);
        LocalTimeConversions.toInteger(null, converter);
    }

    @Test
    public void testNullToLong() {
        thrown.expect(NullPointerException.class);
        LocalTimeConversions.toLong(null, converter);
    }

    @Test
    public void testNullToDouble() {
        thrown.expect(NullPointerException.class);
        LocalTimeConversions.toDouble(null, converter);
    }

    @Test
    public void testNullToBigInteger() {
        thrown.expect(NullPointerException.class);
        LocalTimeConversions.toBigInteger(null, converter);
    }

    @Test
    public void testNullToBigDecimal() {
        thrown.expect(NullPointerException.class);
        LocalTimeConversions.toBigDecimal(null, converter);
    }

    @Test
    public void testNullToAtomicInteger() {
        thrown.expect(NullPointerException.class);
        LocalTimeConversions.toAtomicInteger(null, converter);
    }

    @Test
    public void testNullToAtomicLong() {
        thrown.expect(NullPointerException.class);
        LocalTimeConversions.toAtomicLong(null, converter);
    }

    @Test
    public void testNullToString() {
        thrown.expect(NullPointerException.class);
        LocalTimeConversions.toString(null, converter);
    }

    @Test
    public void testNullToCalendar() {
        thrown.expect(NullPointerException.class);
        LocalTimeConversions.toCalendar(null, converter);
    }

    /* ---------------------------------------------------------------------- */
    /* 12. Wrong‑type handling – passing an object that is not a LocalTime   */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testWrongTypeToInteger() {
        thrown.expect(ClassCastException.class);
        LocalTimeConversions.toInteger("not a LocalTime", converter);
    }

    @Test
    public void testWrongTypeToLong() {
        thrown.expect(ClassCastException.class);
        LocalTimeConversions.toLong(12345, converter);
    }

    @Test
    public void testWrongTypeToDouble() {
        thrown.expect(ClassCastException.class);
        LocalTimeConversions.toDouble(new Object(), converter);
    }

    @Test
    public void testWrongTypeToMap() {
        thrown.expect(ClassCastException.class);
        LocalTimeConversions.toMap(Boolean.TRUE, converter);
    }

    // The same ClassCastException expectation applies to all remaining overloads.
    @Test
    public void testWrongTypeToBigInteger() {
        thrown.expect(ClassCastException.class);
        LocalTimeConversions.toBigInteger(3.14, converter);
    }

    @Test
    public void testWrongTypeToBigDecimal() {
        thrown.expect(ClassCastException.class);
        LocalTimeConversions.toBigDecimal(new int[]{1,2,3}, converter);
    }

    @Test
    public void testWrongTypeToAtomicInteger() {
        thrown.expect(ClassCastException.class);
        LocalTimeConversions.toAtomicInteger(new java.util.Date(), converter);
    }

    @Test
    public void testWrongTypeToAtomicLong() {
        thrown.expect(ClassCastException.class);
        LocalTimeConversions.toAtomicLong(new java.util.Random(), converter);
    }

    @Test
    public void testWrongTypeToString() {
        thrown.expect(ClassCastException.class);
        LocalTimeConversions.toString(42L, converter);
    }

    @Test
    public void testWrongTypeToCalendar() {
        thrown.expect(ClassCastException.class);
        LocalTimeConversions.toCalendar(new java.math.BigDecimal("10.5"), converter);
    }
}
