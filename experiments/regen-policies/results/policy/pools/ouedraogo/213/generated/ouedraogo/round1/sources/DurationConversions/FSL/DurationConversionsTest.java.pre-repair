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
 * The tests exercise every public static conversion method in the class.
 * A {@code null} {@link Converter} is passed because the implementation does not
 * use the {@code Converter} argument.
 */
public class DurationConversionsTest {

    // ----------------------------------------------------------------------
    // Helper methods
    // ----------------------------------------------------------------------

    /** Creates a {@link Duration} from seconds and nanoseconds. */
    private static Duration duration(long seconds, int nanos) {
        return Duration.ofSeconds(seconds).plusNanos(nanos);
    }

    /** Expected nanoseconds as {@link BigInteger} for a given {@link Duration}. */
    private static BigInteger expectedNanos(Duration d) {
        BigInteger seconds = BigInteger.valueOf(d.getSeconds());
        BigInteger nanos   = BigInteger.valueOf(d.getNano());
        return seconds.multiply(BigInteger.valueOf(1_000_000_000L)).add(nanos);
    }

    /** Expected {@code double} value for a given {@link Duration}. */
    private static double expectedDouble(Duration d) {
        return d.getSeconds() + d.getNano() / 1_000_000_000d;
    }

    // ----------------------------------------------------------------------
    // Tests for toMap
    // ----------------------------------------------------------------------

    @Test
    public void testToMapZeroDuration() {
        Duration d = Duration.ZERO;
        Map<String, Object> map = DurationConversions.toMap(d, null);
        assertNotNull("Map must not be null", map);
        assertEquals("Map must contain exactly one entry", 1, map.size());
        assertTrue("Map must contain the DURATION key", map.containsKey(MapConversions.DURATION));
        assertEquals("Value must be the ISO‑8601 string representation", d.toString(), map.get(MapConversions.DURATION));
    }

    @Test
    public void testToMapComplexDuration() {
        // PT1H30M15.123456789S
        Duration d = Duration.ofHours(1)
                             .plusMinutes(30)
                             .plusSeconds(15)
                             .plusNanos(123_456_789);
        Map<String, Object> map = DurationConversions.toMap(d, null);
        assertEquals(d.toString(), map.get(MapConversions.DURATION));
    }

    // ----------------------------------------------------------------------
    // Tests for toLong (millis)
    // ----------------------------------------------------------------------

    @Test
    public void testToLongPositive() {
        Duration d = Duration.ofSeconds(10).plusMillis(250);
        long expected = d.toMillis();
        assertEquals(expected, DurationConversions.toLong(d, null));
    }

    @Test
    public void testToLongNegative() {
        Duration d = Duration.ofSeconds(-5).minusMillis(500);
        long expected = d.toMillis();
        assertEquals(expected, DurationConversions.toLong(d, null));
    }

    // ----------------------------------------------------------------------
    // Tests for toAtomicLong
    // ----------------------------------------------------------------------

    @Test
    public void testToAtomicLong() {
        Duration d = Duration.ofMinutes(2).plusMillis(75);
        AtomicLong al = DurationConversions.toAtomicLong(d, null);
        assertNotNull(al);
        assertEquals(d.toMillis(), al.get());
    }

    // ----------------------------------------------------------------------
    // Tests for toBigInteger (nanoseconds)
    // ----------------------------------------------------------------------

    @Test
    public void testToBigIntegerZero() {
        Duration d = Duration.ZERO;
        assertEquals(BigInteger.ZERO, DurationConversions.toBigInteger(d, null));
    }

    @Test
    public void testToBigIntegerPositive() {
        Duration d = duration(123, 456_789_012);
        assertEquals(expectedNanos(d), DurationConversions.toBigInteger(d, null));
    }

    @Test
    public void testToBigIntegerNegative() {
        Duration d = duration(-7, -123_456_789); // Duration normalises the sign, so we build it via API
        assertEquals(expectedNanos(d), DurationConversions.toBigInteger(d, null));
    }

    // ----------------------------------------------------------------------
    // Tests for toDouble
    // ----------------------------------------------------------------------

    @Test
    public void testToDoubleExact() {
        Duration d = Duration.ofSeconds(3).plusNanos(500_000_000); // 3.5 seconds
        double expected = expectedDouble(d);
        double actual = DurationConversions.toDouble(d, null);
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToDoubleNegative() {
        Duration d = Duration.ofSeconds(-2).minusNanos(250_000_000); // -2.25 seconds
        double expected = expectedDouble(d);
        double actual = DurationConversions.toDouble(d, null);
        assertEquals(expected, actual, 0.0);
    }

    // ----------------------------------------------------------------------
    // Tests for toBigDecimal
    // ----------------------------------------------------------------------

    @Test
    public void testToBigDecimal() {
        Duration d = Duration.ofSeconds(1).plusNanos(1);
        BigDecimal expected = BigDecimal.valueOf(d.getSeconds())
                                         .add(BigDecimal.valueOf(d.getNano(), 9)); // nanos / 10^9
        BigDecimal actual = DurationConversions.toBigDecimal(d, null);
        assertEquals(0, expected.compareTo(actual));
    }

    // ----------------------------------------------------------------------
    // Tests for toTimestamp
    // ----------------------------------------------------------------------

    @Test
    public void testToTimestampEpoch() {
        Duration d = Duration.ZERO;
        Timestamp ts = DurationConversions.toTimestamp(d, null);
        Timestamp expected = Timestamp.from(Instant.EPOCH);
        assertEquals(expected, ts);
    }

    @Test
    public void testToTimestampPositive() {
        Duration d = Duration.ofSeconds(2).plusMillis(500);
        Timestamp ts = DurationConversions.toTimestamp(d, null);
        Timestamp expected = Timestamp.from(Instant.EPOCH.plus(d));
        assertEquals(expected, ts);
    }

    @Test
    public void testToTimestampNegative() {
        Duration d = Duration.ofSeconds(-3).minusNanos(250_000_000);
        Timestamp ts = DurationConversions.toTimestamp(d, null);
        Timestamp expected = Timestamp.from(Instant.EPOCH.plus(d));
        assertEquals(expected, ts);
    }
}
