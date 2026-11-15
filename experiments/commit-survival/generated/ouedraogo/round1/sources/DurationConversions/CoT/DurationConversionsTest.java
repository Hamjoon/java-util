package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class DurationConversionsTest {

    // -------------------------------------------------------------------------
    // 1. List of public (package‑visible) static methods in DurationConversions
    // -------------------------------------------------------------------------
    // static Map toMap(Object from, Converter converter)
    // static long toLong(Object from, Converter converter)
    // static AtomicLong toAtomicLong(Object from, Converter converter)
    // static BigInteger toBigInteger(Object from, Converter converter)
    // static double toDouble(Object from, Converter converter)
    // static BigDecimal toBigDecimal(Object from, Converter converter)
    // static Timestamp toTimestamp(Object from, Converter converter)

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // -------------------------------------------------------------------------
    // Basic functional tests
    // -------------------------------------------------------------------------

    @Test
    public void testToMap_basic() {
        Duration d = Duration.ofSeconds(5, 123_456_789);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) DurationConversions.toMap(d, null);
        assertEquals(5L, map.get("seconds"));
        assertEquals(123_456_789, map.get("nanos"));
        assertEquals(2, map.size());
    }

    @Test
    public void testToLong_basic() {
        Duration d = Duration.ofSeconds(2, 500_000_000); // 2.5 seconds
        long millis = DurationConversions.toLong(d, null);
        assertEquals(2500L, millis);
    }

    @Test
    public void testToAtomicLong_basic() {
        Duration d = Duration.ofSeconds(3, 250_000_000); // 3.25 seconds
        AtomicLong al = DurationConversions.toAtomicLong(d, null);
        assertEquals(3250L, al.get());
    }

    @Test
    public void testToBigInteger_basic() {
        Duration d = Duration.ofSeconds(7, 999_999_999);
        BigInteger bi = DurationConversions.toBigInteger(d, null);
        BigInteger expected = BigInteger.valueOf(7).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(999_999_999));
        assertEquals(expected, bi);
    }

    @Test
    public void testToDouble_basic() {
        Duration d = Duration.ofSeconds(4, 250_000_000); // 4.25 seconds
        double val = DurationConversions.toDouble(d, null);
        assertEquals(4.25, val, 0.000_000_001);
    }

    @Test
    public void testToBigDecimal_basic() {
        Duration d = Duration.ofSeconds(1, 500_000_000); // 1.5 seconds
        BigDecimal bd = DurationConversions.toBigDecimal(d, null);
        assertEquals(new BigDecimal("1.5"), bd);
    }

    @Test
    public void testToTimestamp_basic() {
        Duration d = Duration.ofSeconds(1);
        Timestamp ts = DurationConversions.toTimestamp(d, null);
        Timestamp expected = Timestamp.from(Instant.EPOCH.plusSeconds(1));
        assertEquals(expected, ts);
    }

    // -------------------------------------------------------------------------
    // Edge‑case and exception tests
    // -------------------------------------------------------------------------

    @Test
    public void testZeroDuration() {
        Duration zero = Duration.ZERO;
        assertEquals(0L, DurationConversions.toLong(zero, null));
        assertEquals(0L, DurationConversions.toAtomicLong(zero, null).get());
        assertEquals(BigInteger.ZERO, DurationConversions.toBigInteger(zero, null));
        assertEquals(0.0, DurationConversions.toDouble(zero, null), 0.0);
        assertEquals(BigDecimal.ZERO, DurationConversions.toBigDecimal(zero, null));
        assertEquals(Timestamp.from(Instant.EPOCH), DurationConversions.toTimestamp(zero, null));
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) DurationConversions.toMap(zero, null);
        assertEquals(0L, map.get("seconds"));
        assertEquals(0, map.get("nanos"));
    }

    @Test
    public void testNegativeDuration() {
        Duration neg = Duration.ofSeconds(-3, -250_000_000); // -3.25 seconds
        // toLong / toAtomicLong should reflect the negative milliseconds
        assertEquals(-3250L, DurationConversions.toLong(neg, null));
        assertEquals(-3250L, DurationConversions.toAtomicLong(neg, null).get());

        // BigInteger should be negative nanoseconds total
        BigInteger bi = DurationConversions.toBigInteger(neg, null);
        BigInteger expected = BigInteger.valueOf(-3).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(-250_000_000));
        assertEquals(expected, bi);

        // double / BigDecimal should be negative
        assertEquals(-3.25, DurationConversions.toDouble(neg, null), 0.000_000_001);
        assertEquals(new BigDecimal("-3.25"), DurationConversions.toBigDecimal(neg, null));

        // Timestamp: Instant.EPOCH plus a negative duration yields a time before epoch
        Timestamp ts = DurationConversions.toTimestamp(neg, null);
        Timestamp expected = Timestamp.from(Instant.EPOCH.plus(neg));
        assertEquals(expected, ts);
    }

    @Test
    public void testToLong_overflow() {
        // Duration whose millis value exceeds Long.MAX_VALUE -> should throw ArithmeticException
        long seconds = Long.MAX_VALUE / 1000 + 1; // seconds * 1000 will overflow
        Duration d = Duration.ofSeconds(seconds);
        thrown.expect(ArithmeticException.class);
        DurationConversions.toLong(d, null);
    }

    @Test
    public void testToAtomicLong_overflow() {
        long seconds = Long.MAX_VALUE / 1000 + 2;
        Duration d = Duration.ofSeconds(seconds);
        thrown.expect(ArithmeticException.class);
        DurationConversions.toAtomicLong(d, null);
    }

    @Test
    public void testToBigInteger_largeDuration() {
        // Use Long.MAX_VALUE seconds – still fits in BigInteger
        Duration d = Duration.ofSeconds(Long.MAX_VALUE, 123);
        BigInteger bi = DurationConversions.toBigInteger(d, null);
        BigInteger expected = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(123));
        assertEquals(expected, bi);
    }

    @Test
    public void testToTimestamp_outOfRange() {
        // Timestamp can represent up to year 292278994‑08‑17. Choose a duration far beyond that.
        Duration huge = Duration.ofSeconds(Long.MAX_VALUE);
        thrown.expect(java.time.DateTimeException.class);
        DurationConversions.toTimestamp(huge, null);
    }

    @Test
    public void testNullInput() {
        thrown.expect(NullPointerException.class);
        DurationConversions.toMap(null, null);
    }

    @Test
    public void testNullInput_toLong() {
        thrown.expect(NullPointerException.class);
        DurationConversions.toLong(null, null);
    }

    @Test
    public void testNullInput_toAtomicLong() {
        thrown.expect(NullPointerException.class);
        DurationConversions.toAtomicLong(null, null);
    }

    @Test
    public void testNullInput_toBigInteger() {
        thrown.expect(NullPointerException.class);
        DurationConversions.toBigInteger(null, null);
    }

    @Test
    public void testNullInput_toDouble() {
        thrown.expect(NullPointerException.class);
        DurationConversions.toDouble(null, null);
    }

    @Test
    public void testNullInput_toBigDecimal() {
        thrown.expect(NullPointerException.class);
        DurationConversions.toBigDecimal(null, null);
    }

    @Test
    public void testNullInput_toTimestamp() {
        thrown.expect(NullPointerException.class);
        DurationConversions.toTimestamp(null, null);
    }
}
