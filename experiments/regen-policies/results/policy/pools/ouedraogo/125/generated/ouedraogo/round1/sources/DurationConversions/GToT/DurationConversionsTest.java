package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JUnit‑4 test suite for {@link DurationConversions}.
 *
 * The tests are written in the same package so that the package‑private static
 * methods of {@code DurationConversions} are directly accessible.
 *
 * The {@code Converter} argument is not used by any conversion method, therefore
 * a {@code null} reference is supplied where required.
 */
public class DurationConversionsTest {

    private static final Converter NULL_CONVERTER = null;

    /* --------------------------------------------------------------------- */
    /* Helper methods                                                       */
    /* --------------------------------------------------------------------- */

    /**
     * Creates a {@link Duration} from seconds and nanoseconds, normalising the
     * nanosecond part to the range 0‑999,999,999 as required by {@code Duration}.
     */
    private Duration durationOf(long seconds, int nanos) {
        return Duration.ofSeconds(seconds, nanos);
    }

    /* --------------------------------------------------------------------- */
    /* 1. toMap() – basic functionality                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToMap_Basic() {
        Duration d = durationOf(123L, 456_789);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = DurationConversions.toMap(d, NULL_CONVERTER);

        assertEquals("Map should contain exactly two entries", 2, map.size());
        assertTrue("Map should contain key 'seconds'", map.containsKey("seconds"));
        assertTrue("Map should contain key 'nanos'", map.containsKey("nanos"));
        assertEquals(123L, map.get("seconds"));
        assertEquals(456_789, map.get("nanos"));
    }

    @Test
    public void testToMap_NegativeValues() {
        Duration d = durationOf(-10L, -500); // Duration normalises the nanos
        @SuppressWarnings("unchecked")
        Map<String, Object> map = DurationConversions.toMap(d, NULL_CONVERTER);

        // Normalised values: -10 seconds and 999_999_500 nanos (because -500 ns = -1 µs)
        assertEquals(-10L, map.get("seconds"));
        assertEquals(999_999_500, map.get("nanos"));
    }

    @Test
    public void testToMap_ZeroDuration() {
        Duration d = Duration.ZERO;
        @SuppressWarnings("unchecked")
        Map<String, Object> map = DurationConversions.toMap(d, NULL_CONVERTER);
        assertEquals(0L, map.get("seconds"));
        assertEquals(0, map.get("nanos"));
    }

    /* --------------------------------------------------------------------- */
    /* 2. toLong() – conversion to milliseconds                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToLong_Basic() {
        Duration d = Duration.ofSeconds(5, 123_456_789); // 5.123456789 s
        long expected = d.toMillis(); // truncates nanos -> 5123 ms
        assertEquals(expected, DurationConversions.toLong(d, NULL_CONVERTER));
    }

    @Test
    public void testToLong_NegativeWithNanos() {
        Duration d = Duration.ofSeconds(-3, -500_000_000); // -3.5 s
        long expected = d.toMillis(); // -3500 ms
        assertEquals(expected, DurationConversions.toLong(d, NULL_CONVERTER));
    }

    @Test
    public void testToLong_OverflowSafe() {
        // Long.MAX_VALUE ms = about 292 million years – well within Duration range
        Duration d = Duration.ofMillis(Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, DurationConversions.toLong(d, NULL_CONVERTER));
    }

    /* --------------------------------------------------------------------- */
    /* 3. toAtomicLong() – AtomicLong wrapper                                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToAtomicLong_Basic() {
        Duration d = Duration.ofSeconds(2, 250_000_000); // 2.25 s
        AtomicLong al = DurationConversions.toAtomicLong(d, NULL_CONVERTER);
        assertEquals(d.toMillis(), al.get());
    }

    @Test
    public void testToAtomicLong_Negative() {
        Duration d = Duration.ofSeconds(-1, -750_000_000); // -1.75 s
        AtomicLong al = DurationConversions.toAtomicLong(d, NULL_CONVERTER);
        assertEquals(d.toMillis(), al.get());
    }

    /* --------------------------------------------------------------------- */
    /* 4. toBigInteger() – total nanoseconds                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToBigInteger_Basic() {
        Duration d = Duration.ofSeconds(3, 123_456_789);
        BigInteger expected = BigInteger.valueOf(3L).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(123_456_789L));
        assertEquals(expected, DurationConversions.toBigInteger(d, NULL_CONVERTER));
    }

    @Test
    public void testToBigInteger_Negative() {
        Duration d = Duration.ofSeconds(-2, -1); // -2.000000001 s
        // Normalised: -3 seconds + 999_999_999 nanos
        Duration norm = d.normalized();
        BigInteger expected = BigInteger.valueOf(norm.getSeconds()).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(norm.getNano()));
        assertEquals(expected, DurationConversions.toBigInteger(d, NULL_CONVERTER));
    }

    @Test
    public void testToBigInteger_LargeSeconds() {
        long huge = Long.MAX_VALUE; // 9,223,372,036,854,775,807
        Duration d = Duration.ofSeconds(huge, 999_999_999);
        BigInteger expected = BigInteger.valueOf(huge).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(999_999_999L));
        assertEquals(expected, DurationConversions.toBigInteger(d, NULL_CONVERTER));
    }

    /* --------------------------------------------------------------------- */
    /* 5. toDouble() – conversion to double (via BigDecimalConversions)    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToDouble_Basic() {
        Duration d = Duration.ofSeconds(1, 500_000_000); // 1.5 s
        double expected = 1.5d;
        assertEquals(expected, DurationConversions.toDouble(d, NULL_CONVERTER), 0.0);
    }

    @Test
    public void testToDouble_Negative() {
        Duration d = Duration.ofSeconds(-2, -250_000_000); // -2.25 s
        double expected = -2.25d;
        assertEquals(expected, DurationConversions.toDouble(d, NULL_CONVERTER), 0.0);
    }

    @Test
    public void testToDouble_LargeValue() {
        // Use a value that exceeds Double.MAX_VALUE -> expect Infinity
        Duration d = Duration.ofSeconds(Long.MAX_VALUE, 0);
        double result = DurationConversions.toDouble(d, NULL_CONVERTER);
        assertTrue("Result should be Infinity for overflow", Double.isInfinite(result));
    }

    /* --------------------------------------------------------------------- */
    /* 6. toBigDecimal() – conversion to BigDecimal                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToBigDecimal_Basic() {
        Duration d = Duration.ofSeconds(4, 250_000_000); // 4.25 s
        BigDecimal expected = BigDecimal.valueOf(4).add(
                BigDecimal.valueOf(250_000_000L).divide(BigDecimal.valueOf(1_000_000_000L)));
        assertEquals(0, expected.compareTo(DurationConversions.toBigDecimal(d, NULL_CONVERTER)));
    }

    @Test
    public void testToBigDecimal_Negative() {
        Duration d = Duration.ofSeconds(-5, -125_000_000); // -5.125 s
        // Normalised: -6 seconds + 875_000_000 nanos
        Duration norm = d.normalized();
        BigDecimal expected = BigDecimal.valueOf(norm.getSeconds()).add(
                BigDecimal.valueOf(norm.getNano()).divide(BigDecimal.valueOf(1_000_000_000L)));
        assertEquals(0, expected.compareTo(DurationConversions.toBigDecimal(d, NULL_CONVERTER)));
    }

    @Test
    public void testToBigDecimal_Precision() {
        // A duration with many nanoseconds to verify scale is kept
        Duration d = Duration.ofSeconds(0, 123_456_789);
        BigDecimal expected = BigDecimal.valueOf(123_456_789L).divide(BigDecimal.valueOf(1_000_000_000L));
        assertEquals(0, expected.compareTo(DurationConversions.toBigDecimal(d, NULL_CONVERTER)));
    }

    /* --------------------------------------------------------------------- */
    /* 7. toTimestamp() – conversion to java.sql.Timestamp                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToTimestamp_Basic() {
        Duration d = Duration.ofSeconds(10, 0);
        Timestamp expected = Timestamp.from(Instant.EPOCH.plus(d));
        assertEquals(expected, DurationConversions.toTimestamp(d, NULL_CONVERTER));
    }

    @Test
    public void testToTimestamp_WithNanos() {
        Duration d = Duration.ofSeconds(0, 500_000_000); // 0.5 s
        Timestamp expected = Timestamp.from(Instant.EPOCH.plus(d));
        assertEquals(expected, DurationConversions.toTimestamp(d, NULL_CONVERTER));
    }

    @Test
    public void testToTimestamp_NegativeDuration() {
        Duration d = Duration.ofSeconds(-1, -250_000_000); // -1.25 s
        // Normalised to -2 seconds + 750_000_000 nanos
        Duration norm = d.normalized();
        Timestamp expected = Timestamp.from(Instant.EPOCH.plus(norm));
        assertEquals(expected, DurationConversions.toTimestamp(d, NULL_CONVERTER));
    }

    @Test
    public void testToTimestamp_Overflow() {
        // Timestamp stores milliseconds; using a duration that exceeds the range
        // should result in an exception from Timestamp.from()
        Duration d = Duration.ofMillis(Long.MAX_VALUE);
        try {
            DurationConversions.toTimestamp(d, NULL_CONVERTER);
            fail("Expected IllegalArgumentException or ArithmeticException due to overflow");
        } catch (Exception e) {
            // Accept any runtime exception indicating overflow
            assertTrue(e instanceof IllegalArgumentException ||
                       e instanceof ArithmeticException ||
                       e instanceof java.time.DateTimeException);
        }
    }
}
