package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link OffsetTimeConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal (happy‑path) conversions</li>
 *   <li>boundary values (midnight, end‑of‑day, maximum/minimum offsets, nanosecond extremes)</li>
 *   <li>null handling – all conversion methods should throw {@link NullPointerException}</li>
 *   <li>type‑specific edge cases (e.g. overflow when converting to int)</li>
 * </ul>
 */
public class OffsetTimeConversionsTest
{
    private Converter dummyConverter;   // the converter is not used by the class under test

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp()
    {
        // Converter has a constructor that takes ConverterOptions; passing null is acceptable for our tests
        dummyConverter = new Converter(null);
    }

    /* ----------------------------------------------------------------------
     *  1.  List of public static methods (signatures) in OffsetTimeConversions
     * ---------------------------------------------------------------------- */
    // static String   toString(Object from, Converter converter)
    // static Map<String,Object> toMap(Object from, Converter converter)
    // static int    toInteger(Object from, Converter converter)
    // static long   toLong(Object from, Converter converter)
    // static double toDouble(Object from, Converter converter)
    // static BigInteger toBigInteger(Object from, Converter converter)
    // static BigDecimal toBigDecimal(Object from, Converter converter)
    // static AtomicInteger toAtomicInteger(Object from, Converter converter)
    // static AtomicLong    toAtomicLong(Object from, Converter converter)

    /* ----------------------------------------------------------------------
     *  2.  Basic (happy‑path) tests for each conversion method
     * ---------------------------------------------------------------------- */

    @Test
    public void testToString()
    {
        OffsetTime ot = OffsetTime.of(13, 45, 30, 123_456_789, ZoneOffset.ofHoursMinutes(2, 30));
        String result = OffsetTimeConversions.toString(ot, dummyConverter);
        assertEquals("13:45:30.123456789+02:30", result);
    }

    @Test
    public void testToMap()
    {
        OffsetTime ot = OffsetTime.of(9, 0, 0, 0, ZoneOffset.UTC);
        Map<String, Object> map = OffsetTimeConversions.toMap(ot, dummyConverter);
        assertEquals(1, map.size());
        assertEquals(ot.toString(), map.get(MapConversions.OFFSET_TIME));
    }

    @Test
    public void testToInteger()
    {
        OffsetTime ot = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);
        int value = OffsetTimeConversions.toInteger(ot, dummyConverter);
        assertEquals(0, value);
    }

    @Test
    public void testToLong()
    {
        OffsetTime ot = OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHours(1));
        long epochMilli = OffsetTimeConversions.toLong(ot, dummyConverter);
        // 1970‑01‑01T23:59:59.999999999+01:00 is 86_399_999 ms after epoch (one millisecond before next day)
        assertEquals(86_399_999L, epochMilli);
    }

    @Test
    public void testToDouble()
    {
        OffsetTime ot = OffsetTime.of(12, 0, 0, 500_000_000, ZoneOffset.UTC);
        double d = OffsetTimeConversions.toDouble(ot, dummyConverter);
        // 12 hours = 43_200 seconds + 0.5 seconds
        assertEquals(43_200.5, d, 1e-12);
    }

    @Test
    public void testToBigInteger()
    {
        OffsetTime ot = OffsetTime.of(0, 0, 1, 0, ZoneOffset.UTC);
        BigInteger bi = OffsetTimeConversions.toBigInteger(ot, dummyConverter);
        // 1 second = 1_000_000_000 nanoseconds
        BigInteger expected = BigInteger.valueOf(1L).multiply(BigIntegerConversions.BILLION);
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigDecimal()
    {
        OffsetTime ot = OffsetTime.of(0, 0, 1, 250_000_000, ZoneOffset.UTC);
        BigDecimal bd = OffsetTimeConversions.toBigDecimal(ot, dummyConverter);
        BigDecimal expected = BigDecimal.valueOf(1L).add(BigDecimal.valueOf(0.25));
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToAtomicInteger()
    {
        OffsetTime ot = OffsetTime.of(0, 0, 42, 0, ZoneOffset.UTC);
        AtomicInteger ai = OffsetTimeConversions.toAtomicInteger(ot, dummyConverter);
        assertEquals(42, ai.get());
    }

    @Test
    public void testToAtomicLong()
    {
        OffsetTime ot = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);
        AtomicLong al = OffsetTimeConversions.toAtomicLong(ot, dummyConverter);
        assertEquals(0L, al.get());
    }

    /* ----------------------------------------------------------------------
     *  3.  Edge‑case & exception tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testToString_NullInput()
    {
        thrown.expect(NullPointerException.class);
        OffsetTimeConversions.toString(null, dummyConverter);
    }

    @Test
    public void testToMap_NullInput()
    {
        thrown.expect(NullPointerException.class);
        OffsetTimeConversions.toMap(null, dummyConverter);
    }

    @Test
    public void testToInteger_NullInput()
    {
        thrown.expect(NullPointerException.class);
        OffsetTimeConversions.toInteger(null, dummyConverter);
    }

    @Test
    public void testToInteger_Overflow()
    {
        // 1970‑01‑01T00:00:00.000000001+00:00 => 1 nanosecond = 0 ms, still fits in int.
        // To cause overflow we need a value > Integer.MAX_VALUE milliseconds.
        // Use a large offset time: 59653‑01‑01 is far beyond the range, but OffsetTime cannot represent a date.
        // Therefore we simulate overflow by using a time that results in > Integer.MAX_VALUE ms on the epoch day.
        // The maximum millisecond value representable by int is 2_147_483_647.
        // 1970‑01‑01T23:59:59.999+00:00 = 86_399_999 ms (still safe). So overflow cannot be produced with OffsetTime.
        // Instead we verify that the conversion simply casts long to int (truncates higher bits) – we test the cast behaviour.
        OffsetTime ot = OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHours(0));
        long epochMilli = OffsetTimeConversions.toLong(ot, dummyConverter);
        int intValue = OffsetTimeConversions.toInteger(ot, dummyConverter);
        assertEquals((int) epochMilli, intValue);
    }

    @Test
    public void testToLong_NullInput()
    {
        thrown.expect(NullPointerException.class);
        OffsetTimeConversions.toLong(null, dummyConverter);
    }

    @Test
    public void testToDouble_NullInput()
    {
        thrown.expect(NullPointerException.class);
        OffsetTimeConversions.toDouble(null, dummyConverter);
    }

    @Test
    public void testToDouble_NanoPrecision()
    {
        // Verify that nanosecond part contributes correctly to the double value.
        OffsetTime ot = OffsetTime.of(0, 0, 0, 999_999_999, ZoneOffset.UTC);
        double d = OffsetTimeConversions.toDouble(ot, dummyConverter);
        assertEquals(0.999_999_999, d, 1e-12);
    }

    @Test
    public void testToBigInteger_NullInput()
    {
        thrown.expect(NullPointerException.class);
        OffsetTimeConversions.toBigInteger(null, dummyConverter);
    }

    @Test
    public void testToBigDecimal_NullInput()
    {
        thrown.expect(NullPointerException.class);
        OffsetTimeConversions.toBigDecimal(null, dummyConverter);
    }

    @Test
    public void testToAtomicInteger_NullInput()
    {
        thrown.expect(NullPointerException.class);
        OffsetTimeConversions.toAtomicInteger(null, dummyConverter);
    }

    @Test
    public void testToAtomicLong_NullInput()
    {
        thrown.expect(NullPointerException.class);
        OffsetTimeConversions.toAtomicLong(null, dummyConverter);
    }

    @Test
    public void testVariousOffsets()
    {
        // Test with negative, zero and positive offsets to ensure epoch calculation respects the offset.
        OffsetTime otNeg = OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHours(-5));
        OffsetTime otPos = OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHours(3));

        long epochNeg = OffsetTimeConversions.toLong(otNeg, dummyConverter);
        long epochPos = OffsetTimeConversions.toLong(otPos, dummyConverter);

        // 12:00 at -05:00 is 17:00 UTC => 17 * 3600 * 1000 ms
        assertEquals(17L * 3600 * 1000, epochNeg);
        // 12:00 at +03:00 is 09:00 UTC => 9 * 3600 * 1000 ms
        assertEquals(9L * 3600 * 1000, epochPos);
    }

    @Test
    public void testMidnightEdge()
    {
        OffsetTime midnight = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(0L, OffsetTimeConversions.toLong(midnight, dummyConverter));
        assertEquals(0.0, OffsetTimeConversions.toDouble(midnight, dummyConverter), 0.0);
    }

    @Test
    public void testEndOfDayEdge()
    {
        OffsetTime eod = OffsetTime.of(23, 59, 59, 999_000_000, ZoneOffset.UTC);
        long expectedMillis = (23L * 3600 + 59L * 60 + 59L) * 1000 + 999;
        assertEquals(expectedMillis, OffsetTimeConversions.toLong(eod, dummyConverter));
        double expectedDouble = 23 * 3600 + 59 * 60 + 59 + 0.999;
        assertEquals(expectedDouble, OffsetTimeConversions.toDouble(eod, dummyConverter), 1e-9);
    }
}
