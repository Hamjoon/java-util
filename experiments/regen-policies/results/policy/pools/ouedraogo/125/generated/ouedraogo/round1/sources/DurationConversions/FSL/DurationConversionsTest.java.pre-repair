package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Unit tests for {@link DurationConversions}.
 *
 * The tests cover all conversion methods provided by the class and verify
 * correct handling of typical, edge‑case and negative values.
 *
 * The {@code Converter} argument is not used by the implementation, therefore
 * {@code null} is passed to keep the tests simple.
 */
public class DurationConversionsTest {

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /** Small helper to compare two {@link Timestamp}s ignoring the nanos part that may be lost in conversion. */
    private static void assertTimestampEquals(Timestamp expected, Timestamp actual) {
        assertEquals("Timestamp seconds differ", expected.getTime() / 1000, actual.getTime() / 1000);
        assertEquals("Timestamp nanos differ", expected.getNanos(), actual.getNanos());
    }

    /* --------------------------------------------------------------------- */
    /* toMap tests                                                          */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToMapZeroDuration() {
        Duration d = Duration.ZERO;
        Map<String, Object> map = DurationConversions.toMap(d, null);
        assertEquals(2, map.size());
        assertEquals(Long.valueOf(0L), map.get("seconds"));
        assertEquals(Integer.valueOf(0), map.get("nanos"));
    }

    @Test
    public void testToMapPositiveDuration() {
        Duration d = Duration.ofSeconds(123, 456_789_012);
        Map<String, Object> map = DurationConversions.toMap(d, null);
        assertEquals(Long.valueOf(123L), map.get("seconds"));
        assertEquals(Integer.valueOf(456_789_012), map.get("nanos"));
    }

    @Test
    public void testToMapNegativeDuration() {
        // Duration.ofSeconds normalises the nanos part to be positive.
        Duration d = Duration.ofSeconds(-5, -300_000_000); // becomes -6 seconds + 700_000_000 nanos
        Map<String, Object> map = DurationConversions.toMap(d, null);
        assertEquals(Long.valueOf(-6L), map.get("seconds"));
        assertEquals(Integer.valueOf(700_000_000), map.get("nanos"));
    }

    /* --------------------------------------------------------------------- */
    /* toLong tests                                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToLongExactMilliseconds() {
        Duration d = Duration.ofMillis(1234);
        long millis = DurationConversions.toLong(d, null);
        assertEquals(1234L, millis);
    }

    @Test
    public void testToLongWithNanosTruncation() {
        // 1 second + 500_000_001 nanos => 1500 ms (nanoseconds are truncated)
        Duration d = Duration.ofSeconds(1, 500_000_001);
        long millis = DurationConversions.toLong(d, null);
        assertEquals(1500L, millis);
    }

    @Test
    public void testToLongNegativeDuration() {
        Duration d = Duration.ofSeconds(-2, -250_000_000); // normalises to -3 seconds + 750_000_000 nanos
        long millis = DurationConversions.toLong(d, null);
        // -3 seconds = -3000 ms, + 750 ms = -2250 ms
        assertEquals(-2250L, millis);
    }

    /* --------------------------------------------------------------------- */
    /* toAtomicLong tests                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToAtomicLongPositive() {
        Duration d = Duration.ofMillis(42);
        AtomicLong al = DurationConversions.toAtomicLong(d, null);
        assertEquals(42L, al.get());
    }

    @Test
    public void testToAtomicLongNegative() {
        Duration d = Duration.ofSeconds(-1, -1); // normalises to -2 seconds + 999_999_999 nanos
        AtomicLong al = DurationConversions.toAtomicLong(d, null);
        // -2 seconds = -2000 ms, + 999 ms = -1001 ms
        assertEquals(-1001L, al.get());
    }

    /* --------------------------------------------------------------------- */
    /* toBigInteger tests                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToBigIntegerZero() {
        Duration d = Duration.ZERO;
        BigInteger bi = DurationConversions.toBigInteger(d, null);
        assertEquals(BigInteger.ZERO, bi);
    }

    @Test
    public void testToBigIntegerPositive() {
        Duration d = Duration.ofSeconds(3, 123_456_789);
        // expected = 3 * 1_000_000_000 + 123_456_789 = 3_123_456_789
        BigInteger expected = BigInteger.valueOf(3L).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(123_456_789L));
        assertEquals(expected, DurationConversions.toBigInteger(d, null));
    }

    @Test
    public void testToBigIntegerNegative() {
        Duration d = Duration.ofSeconds(-4, -250_000_000); // normalises to -5 sec + 750_000_000 nanos
        // expected = -5 * 1_000_000_000 + 750_000_000 = -4_250_000_000
        BigInteger expected = BigInteger.valueOf(-5L).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(750_000_000L));
        assertEquals(expected, DurationConversions.toBigInteger(d, null));
    }

    /* --------------------------------------------------------------------- */
    /* toDouble tests                                                       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToDoubleSimple() {
        Duration d = Duration.ofSeconds(2, 500_000_000); // 2.5 seconds
        double actual = DurationConversions.toDouble(d, null);
        assertEquals(2.5, actual, 0.0);
    }

    @Test
    public void testToDoublePrecision() {
        Duration d = Duration.ofSeconds(0, 123_456_789);
        double actual = DurationConversions.toDouble(d, null);
        assertEquals(0.123456789, actual, 1e-12);
    }

    @Test
    public void testToDoubleNegative() {
        Duration d = Duration.ofSeconds(-1, -250_000_000); // normalises to -2 sec + 750_000_000 nanos -> -1.25 seconds
        double actual = DurationConversions.toDouble(d, null);
        assertEquals(-1.25, actual, 0.0);
    }

    /* --------------------------------------------------------------------- */
    /* toBigDecimal tests                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToBigDecimalPositive() {
        Duration d = Duration.ofSeconds(5, 250_000_000); // 5.25 seconds
        BigDecimal bd = DurationConversions.toBigDecimal(d, null);
        assertEquals(new BigDecimal("5.25"), bd);
    }

    @Test
    public void testToBigDecimalZero() {
        Duration d = Duration.ZERO;
        BigDecimal bd = DurationConversions.toBigDecimal(d, null);
        assertEquals(BigDecimal.ZERO, bd);
    }

    @Test
    public void testToBigDecimalNegative() {
        Duration d = Duration.ofSeconds(-3, -500_000_000); // normalises to -4 sec + 500_000_000 nanos -> -3.5 seconds
        BigDecimal bd = DurationConversions.toBigDecimal(d, null);
        assertEquals(new BigDecimal("-3.5"), bd);
    }

    /* --------------------------------------------------------------------- */
    /* toTimestamp tests                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToTimestampZero() {
        Duration d = Duration.ZERO;
        Timestamp ts = DurationConversions.toTimestamp(d, null);
        Timestamp expected = Timestamp.from(Instant.EPOCH);
        assertTimestampEquals(expected, ts);
    }

    @Test
    public void testToTimestampPositive() {
        Duration d = Duration.ofSeconds(10, 123_456_789);
        Timestamp ts = DurationConversions.toTimestamp(d, null);
        Instant expectedInstant = Instant.EPOCH.plus(d);
        Timestamp expected = Timestamp.from(expectedInstant);
        assertTimestampEquals(expected, ts);
    }

    @Test
    public void testToTimestampNegative() {
        Duration d = Duration.ofSeconds(-2, -500_000_000); // normalises to -3 sec + 500_000_000 nanos
        Timestamp ts = DurationConversions.toTimestamp(d, null);
        Instant expectedInstant = Instant.EPOCH.plus(d);
        Timestamp expected = Timestamp.from(expectedInstant);
        assertTimestampEquals(expected, ts);
    }
}
