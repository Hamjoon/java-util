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
 * Tests for {@link DurationConversions}. All public static methods are exercised
 * with a variety of {@link Duration} values, including zero, positive, and
 * negative durations as well as values that contain both seconds and nanoseconds.
 *
 * The {@link Converter} argument is not used by the implementation, therefore a
 * minimal instance (with default options) is sufficient.
 */
public class DurationConversionsTest {

    // Helper to create a dummy Converter – the implementation under test does not
    // depend on its state.
    private static final Converter DUMMY_CONVERTER = new Converter(new ConverterOptions());

    @Test
    public void testToMap_basic() {
        Duration d = Duration.ofSeconds(123, 456_000_000); // 123.456 seconds
        Map<String, Object> map = DurationConversions.toMap(d, DUMMY_CONVERTER);

        assertEquals(2, map.size());
        assertTrue(map.containsKey("seconds"));
        assertTrue(map.containsKey("nanos"));
        assertEquals(123L, map.get("seconds"));
        assertEquals(456_000_000, map.get("nanos"));
    }

    @Test
    public void testToMap_negativeDuration() {
        Duration d = Duration.ofSeconds(-5, -250_000_000); // -5.250 seconds
        Map<String, Object> map = DurationConversions.toMap(d, DUMMY_CONVERTER);

        // Duration normalises the sign: -5 seconds and -250_000_000 nanos become -6 seconds + 750_000_000 nanos
        assertEquals(-6L, map.get("seconds"));
        assertEquals(750_000_000, map.get("nanos"));
    }

    @Test
    public void testToLong_millisConversion() {
        Duration d = Duration.ofMillis(1_234L);
        long millis = DurationConversions.toLong(d, DUMMY_CONVERTER);
        assertEquals(1_234L, millis);
    }

    @Test
    public void testToLong_withNanosRounding() {
        // Duration.ofSeconds adds the nanos component; toMillis truncates toward zero.
        Duration d = Duration.ofSeconds(2, 999_999_999); // just under 3 seconds
        long millis = DurationConversions.toLong(d, DUMMY_CONVERTER);
        assertEquals(2_999L, millis); // 2.999999999 s -> 2999 ms
    }

    @Test
    public void testToAtomicLong() {
        Duration d = Duration.ofMinutes(2).plusSeconds(3).plusNanos(500);
        AtomicLong al = DurationConversions.toAtomicLong(d, DUMMY_CONVERTER);
        assertEquals(d.toMillis(), al.get());
    }

    @Test
    public void testToBigInteger_secondsAndNanos() {
        Duration d = Duration.ofSeconds(10, 123_456_789);
        BigInteger bi = DurationConversions.toBigInteger(d, DUMMY_CONVERTER);
        // Expected = 10 * 1_000_000_000 + 123_456_789
        BigInteger expected = BigInteger.valueOf(10L).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(123_456_789L));
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigInteger_negative() {
        Duration d = Duration.ofSeconds(-3, -400_000_000);
        // Normalised: -4 seconds + 600_000_000 nanos
        BigInteger bi = DurationConversions.toBigInteger(d, DUMMY_CONVERTER);
        BigInteger expected = BigInteger.valueOf(-4L).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(600_000_000L));
        assertEquals(expected, bi);
    }

    @Test
    public void testToDouble() {
        Duration d = Duration.ofSeconds(5, 250_000_000); // 5.25 seconds
        double val = DurationConversions.toDouble(d, DUMMY_CONVERTER);
        assertEquals(5.25, val, 0.000000001);
    }

    @Test
    public void testToDouble_largeValues() {
        Duration d = Duration.ofSeconds(Long.MAX_VALUE / 1_000_000_000L, 0);
        double val = DurationConversions.toDouble(d, DUMMY_CONVERTER);
        // The double representation may lose precision but must be finite
        assertTrue(Double.isFinite(val));
    }

    @Test
    public void testToBigDecimal() {
        Duration d = Duration.ofSeconds(2, 500_000_000); // 2.5 seconds
        BigDecimal bd = DurationConversions.toBigDecimal(d, DUMMY_CONVERTER);
        assertEquals(new BigDecimal("2.5"), bd);
    }

    @Test
    public void testToTimestamp_zero() {
        Duration d = Duration.ZERO;
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        // Timestamp representing the epoch (1970-01-01T00:00:00Z)
        assertEquals(Timestamp.from(Instant.EPOCH), ts);
    }

    @Test
    public void testToTimestamp_positive() {
        Duration d = Duration.ofDays(1).plusHours(2).plusMinutes(30).plusSeconds(15);
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        Instant expected = Instant.EPOCH.plus(d);
        assertEquals(Timestamp.from(expected), ts);
    }

    @Test
    public void testToTimestamp_negative() {
        Duration d = Duration.ofSeconds(-10, -500_000_000); // -10.5 seconds
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        Instant expected = Instant.EPOCH.plus(d);
        assertEquals(Timestamp.from(expected), ts);
    }
}
