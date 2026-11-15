package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class DurationConversionsTest {

    // ----------------------------------------------------------------------
    // Helper – a dummy Converter that can be passed to the static methods.
    // The Converter argument is not used by DurationConversions, so a null
    // reference is sufficient.
    // ----------------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = null;

    // ----------------------------------------------------------------------
    // toMap(...)
    // ----------------------------------------------------------------------
    @Test
    public void testToMapTypical() {
        Duration d = Duration.ofSeconds(123, 456_789);
        Map<String, Object> map = DurationConversions.toMap(d, DUMMY_CONVERTER);
        assertEquals(2, map.size());
        assertTrue(map instanceof CompactLinkedMap);
        assertEquals(123L, map.get("seconds"));
        assertEquals(456_789, map.get("nanos"));
    }

    @Test
    public void testToMapZeroDuration() {
        Duration d = Duration.ZERO;
        Map<String, Object> map = DurationConversions.toMap(d, DUMMY_CONVERTER);
        assertEquals(0L, map.get("seconds"));
        assertEquals(0, map.get("nanos"));
    }

    @Test(expected = ClassCastException.class)
    public void testToMapWithInvalidObject() {
        DurationConversions.toMap("not a duration", DUMMY_CONVERTER);
    }

    // ----------------------------------------------------------------------
    // toLong(...)
    // ----------------------------------------------------------------------
    @Test
    public void testToLongTypical() {
        Duration d = Duration.ofSeconds(5, 500_000_000); // 5.5 seconds
        long millis = DurationConversions.toLong(d, DUMMY_CONVERTER);
        assertEquals(5_500L, millis);
    }

    @Test
    public void testToLongNegative() {
        Duration d = Duration.ofSeconds(-2, -250_000_000); // -2.25 seconds
        long millis = DurationConversions.toLong(d, DUMMY_CONVERTER);
        assertEquals(-2_250L, millis);
    }

    @Test(expected = ClassCastException.class)
    public void testToLongInvalidObject() {
        DurationConversions.toLong(42, DUMMY_CONVERTER);
    }

    // ----------------------------------------------------------------------
    // toAtomicLong(...)
    // ----------------------------------------------------------------------
    @Test
    public void testToAtomicLongTypical() {
        Duration d = Duration.ofMillis(3_333);
        AtomicLong al = DurationConversions.toAtomicLong(d, DUMMY_CONVERTER);
        assertEquals(3_333L, al.get());
    }

    @Test
    public void testToAtomicLongZero() {
        AtomicLong al = DurationConversions.toAtomicLong(Duration.ZERO, DUMMY_CONVERTER);
        assertEquals(0L, al.get());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLongInvalidObject() {
        DurationConversions.toAtomicLong(new Object(), DUMMY_CONVERTER);
    }

    // ----------------------------------------------------------------------
    // toBigInteger(...)
    // ----------------------------------------------------------------------
    @Test
    public void testToBigIntegerTypical() {
        Duration d = Duration.ofSeconds(2, 123_456_789);
        // Expected: 2 * 1_000_000_000 + 123_456_789 = 2_123_456_789
        BigInteger expected = BigInteger.valueOf(2L).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(123_456_789L));
        assertEquals(expected, DurationConversions.toBigInteger(d, DUMMY_CONVERTER));
    }

    @Test
    public void testToBigIntegerNegative() {
        Duration d = Duration.ofSeconds(-1, -999_999_999);
        // Expected: -1 * 1_000_000_000 + (-999_999_999) = -1_999_999_999
        BigInteger expected = BigInteger.valueOf(-1L).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(-999_999_999L));
        assertEquals(expected, DurationConversions.toBigInteger(d, DUMMY_CONVERTER));
    }

    @Test(expected = ClassCastException.class)
    public void testToBigIntegerInvalidObject() {
        DurationConversions.toBigInteger(null, DUMMY_CONVERTER);
    }

    // ----------------------------------------------------------------------
    // toDouble(...)
    // ----------------------------------------------------------------------
    @Test
    public void testToDoubleTypical() {
        Duration d = Duration.ofSeconds(7, 250_000_000); // 7.25 seconds
        double result = DurationConversions.toDouble(d, DUMMY_CONVERTER);
        assertEquals(7.25, result, 0.0);
    }

    @Test
    public void testToDoubleLargeValues() {
        // 2^30 seconds ~ 34 years, still comfortably fits in a double
        long seconds = 1L << 30;
        Duration d = Duration.ofSeconds(seconds, 0);
        double result = DurationConversions.toDouble(d, DUMMY_CONVERTER);
        assertEquals((double) seconds, result, 0.0);
    }

    @Test(expected = ClassCastException.class)
    public void testToDoubleInvalidObject() {
        DurationConversions.toDouble(new Object(), DUMMY_CONVERTER);
    }

    // ----------------------------------------------------------------------
    // toBigDecimal(...)
    // ----------------------------------------------------------------------
    @Test
    public void testToBigDecimalTypical() {
        Duration d = Duration.ofSeconds(3, 333_000_000); // 3.333 seconds
        BigDecimal result = DurationConversions.toBigDecimal(d, DUMMY_CONVERTER);
        assertEquals(new BigDecimal("3.333"), result);
    }

    @Test
    public void testToBigDecimalNegative() {
        Duration d = Duration.ofSeconds(-4, -500_000_000); // -4.5 seconds
        BigDecimal result = DurationConversions.toBigDecimal(d, DUMMY_CONVERTER);
        assertEquals(new BigDecimal("-4.5"), result);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimalInvalidObject() {
        DurationConversions.toBigDecimal(123, DUMMY_CONVERTER);
    }

    // ----------------------------------------------------------------------
    // toTimestamp(...)
    // ----------------------------------------------------------------------
    @Test
    public void testToTimestampTypical() {
        Duration d = Duration.ofSeconds(10, 123_456_789);
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        // Expected epoch millis = 10_000 (seconds) + 0.123456789 seconds = 10_000 + 123 ms
        // Timestamp stores milliseconds, so fraction is truncated.
        assertEquals(10_000L + 123L, ts.getTime());
    }

    @Test
    public void testToTimestampZero() {
        Timestamp ts = DurationConversions.toTimestamp(Duration.ZERO, DUMMY_CONVERTER);
        assertEquals(0L, ts.getTime());
    }

    @Test
    public void testToTimestampNegative() {
        Duration d = Duration.ofSeconds(-5, -500_000_000); // -5.5 seconds
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        // -5500 ms (truncated toward zero by Timestamp)
        assertEquals(-5_500L, ts.getTime());
    }

    @Test(expected = ClassCastException.class)
    public void testToTimestampInvalidObject() {
        DurationConversions.toTimestamp("bad", DUMMY_CONVERTER);
    }
}
