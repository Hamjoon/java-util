package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.convert.MapConversions.DURATION;
import static org.junit.Assert.*;

public class DurationConversionsTest {

    // The Converter argument is never used by the current implementation,
    // therefore we can safely pass `null` in all tests.
    private static final Converter DUMMY_CONVERTER = null;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* --------------------------------------------------------------------- *
     *  1.  List of public (package‑private static) methods under test
     * --------------------------------------------------------------------- *
     *
     * static Map   toMap(Object from, Converter converter)
     * static long   toLong(Object from, Converter converter)
     * static AtomicLong toAtomicLong(Object from, Converter converter)
     * static BigInteger toBigInteger(Object from, Converter converter)
     * static double toDouble(Object from, Converter converter)
     * static BigDecimal toBigDecimal(Object from, Converter converter)
     * static Timestamp toTimestamp(Object from, Converter converter)
     *
     * --------------------------------------------------------------------- */

    /* --------------------------------------------------------------------- *
     *  2.  Basic functional tests
     * --------------------------------------------------------------------- */

    @Test
    public void testToMap() {
        Duration d = Duration.ofSeconds(123, 456_789_000); // 123.456789 seconds
        Map<?,?> map = DurationConversions.toMap(d, DUMMY_CONVERTER);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(DURATION));
        assertEquals(d.toString(), map.get(DURATION));
    }

    @Test
    public void testToLong() {
        Duration d = Duration.ofSeconds(10, 500_000_000); // 10.5 seconds
        long millis = DurationConversions.toLong(d, DUMMY_CONVERTER);
        assertEquals(10_500L, millis);
    }

    @Test
    public void testToAtomicLong() {
        Duration d = Duration.ofMinutes(2); // 120,000 ms
        AtomicLong atomic = DurationConversions.toAtomicLong(d, DUMMY_CONVERTER);
        assertEquals(120_000L, atomic.get());
    }

    @Test
    public void testToBigInteger() {
        Duration d = Duration.ofSeconds(5, 250_000_000); // 5.25 sec = 5_250_000_000 ns
        BigInteger bi = DurationConversions.toBigInteger(d, DUMMY_CONVERTER);
        BigInteger expected = BigInteger.valueOf(5).multiply(BigInteger.valueOf(1_000_000_000L))
                                          .add(BigInteger.valueOf(250_000_000L));
        assertEquals(expected, bi);
    }

    @Test
    public void testToDouble() {
        Duration d = Duration.ofSeconds(2, 333_000_000); // 2.333 seconds
        double val = DurationConversions.toDouble(d, DUMMY_CONVERTER);
        assertEquals(2.333, val, 0.000_001);
    }

    @Test
    public void testToBigDecimal() {
        Duration d = Duration.ofSeconds(0, 1); // 1 nanosecond = 1e‑9 seconds
        BigDecimal bd = DurationConversions.toBigDecimal(d, DUMMY_CONVERTER);
        assertEquals(new BigDecimal("0.000000001"), bd);
    }

    @Test
    public void testToTimestamp() {
        Duration d = Duration.ofSeconds(1); // epoch + 1 second
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        Timestamp expected = Timestamp.from(Instant.EPOCH.plusSeconds(1));
        assertEquals(expected, ts);
    }

    /* --------------------------------------------------------------------- *
     *  3.  Edge‑case and exception tests
     * --------------------------------------------------------------------- */

    @Test
    public void testZeroDuration() {
        Duration zero = Duration.ZERO;

        assertEquals(0L, DurationConversions.toLong(zero, DUMMY_CONVERTER));
        assertEquals(0L, DurationConversions.toAtomicLong(zero, DUMMY_CONVERTER).get());
        assertEquals(BigInteger.ZERO, DurationConversions.toBigInteger(zero, DUMMY_CONVERTER));
        assertEquals(0.0, DurationConversions.toDouble(zero, DUMMY_CONVERTER), 0.0);
        assertEquals(BigDecimal.ZERO, DurationConversions.toBigDecimal(zero, DUMMY_CONVERTER));
        assertEquals(Timestamp.from(Instant.EPOCH), DurationConversions.toTimestamp(zero, DUMMY_CONVERTER));
    }

    @Test
    public void testNegativeDuration() {
        Duration neg = Duration.ofSeconds(-3, -500_000_000); // -3.5 seconds

        // toLong should return negative millis
        assertEquals(-3_500L, DurationConversions.toLong(neg, DUMMY_CONVERTER));

        // toAtomicLong stores the same (negative) value
        assertEquals(-3_500L, DurationConversions.toAtomicLong(neg, DUMMY_CONVERTER).get());

        // toBigInteger gives negative nanoseconds
        BigInteger expected = BigInteger.valueOf(-3).multiply(BigInteger.valueOf(1_000_000_000L))
                                          .add(BigInteger.valueOf(-500_000_000L));
        assertEquals(expected, DurationConversions.toBigInteger(neg, DUMMY_CONVERTER));

        // toDouble and toBigDecimal reflect the negative seconds
        assertEquals(-3.5, DurationConversions.toDouble(neg, DUMMY_CONVERTER), 0.000_001);
        assertEquals(new BigDecimal("-3.5"), DurationConversions.toBigDecimal(neg, DUMMY_CONVERTER));

        // Timestamp before epoch
        Timestamp ts = DurationConversions.toTimestamp(neg, DUMMY_CONVERTER);
        Timestamp expectedTs = Timestamp.from(Instant.EPOCH.plus(neg));
        assertEquals(expectedTs, ts);
    }

    @Test
    public void testLongOverflowInToLong() {
        // Duration that exceeds Long.MAX_VALUE milliseconds
        // Long.MAX_VALUE ms ≈ 9223372036854775807 ms ≈ 106751 days
        // Use a duration slightly larger than that.
        long secondsTooBig = Long.MAX_VALUE / 1000 + 1; // each second = 1000 ms
        Duration d = Duration.ofSeconds(secondsTooBig);

        thrown.expect(ArithmeticException.class);
        DurationConversions.toLong(d, DUMMY_CONVERTER);
    }

    @Test
    public void testAtomicLongOverflowInToAtomicLong() {
        long secondsTooBig = Long.MAX_VALUE / 1000 + 1;
        Duration d = Duration.ofSeconds(secondsTooBig);

        thrown.expect(ArithmeticException.class);
        DurationConversions.toAtomicLong(d, DUMMY_CONVERTER);
    }

    @Test
    public void testBigIntegerLargeDuration() {
        // Use a duration that would overflow a long in millis but is fine for nanoseconds.
        long hugeSeconds = Long.MAX_VALUE / 1000 + 10_000L; // far beyond millis range
        Duration d = Duration.ofSeconds(hugeSeconds, 123); // add tiny nanos

        // toBigInteger must succeed and give the exact nanosecond count
        BigInteger bi = DurationConversions.toBigInteger(d, DUMMY_CONVERTER);
        BigInteger expected = BigInteger.valueOf(hugeSeconds).multiply(BigInteger.valueOf(1_000_000_000L))
                                          .add(BigInteger.valueOf(123L));
        assertEquals(expected, bi);
    }

    @Test
    public void testNullInputThrowsNPE() {
        thrown.expect(NullPointerException.class);
        DurationConversions.toLong(null, DUMMY_CONVERTER);
    }

    @Test
    public void testInvalidTypeThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        // Pass an object that is not a Duration; the cast inside the method will fail.
        DurationConversions.toLong("not a duration", DUMMY_CONVERTER);
    }
}
