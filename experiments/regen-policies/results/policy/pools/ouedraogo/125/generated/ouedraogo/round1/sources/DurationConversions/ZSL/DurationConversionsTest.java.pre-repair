package com.cedarsoftware.util.convert;

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

    /**
     * Helper that returns a dummy {@link Converter}.  The implementation of
     * {@code DurationConversions} never touches the converter, so {@code null}
     * is sufficient for the tests.
     */
    private static Converter dummyConverter() {
        return null;
    }

    @Test
    public void testToMap_PositiveDuration() {
        Duration dur = Duration.ofSeconds(123, 456_789_123);
        Map<String, Object> map = DurationConversions.toMap(dur, dummyConverter());

        assertNotNull("Map should not be null", map);
        assertEquals("Map should contain exactly two entries", 2, map.size());

        assertTrue("Map should contain key 'seconds'", map.containsKey("seconds"));
        assertTrue("Map should contain key 'nanos'", map.containsKey("nanos"));

        assertEquals("Seconds value is incorrect", 123L, map.get("seconds"));
        assertEquals("Nanos value is incorrect", 456_789_123, map.get("nanos"));
    }

    @Test
    public void testToMap_NegativeDuration() {
        // -10 seconds and -250,000,000 nanos = -10.250 seconds
        Duration dur = Duration.ofSeconds(-10).minusNanos(250_000_000);
        Map<String, Object> map = DurationConversions.toMap(dur, dummyConverter());

        assertEquals(-10L, map.get("seconds"));
        assertEquals(-250_000_000, map.get("nanos"));
    }

    @Test
    public void testToLong() {
        Duration dur = Duration.ofSeconds(5, 250_000_000); // 5.25 seconds = 5250 ms
        long millis = DurationConversions.toLong(dur, dummyConverter());

        assertEquals("Millis conversion is incorrect", 5_250L, millis);
    }

    @Test
    public void testToAtomicLong() {
        Duration dur = Duration.ofSeconds(2, 500_000_000); // 2.5 seconds = 2500 ms
        AtomicLong atomic = DurationConversions.toAtomicLong(dur, dummyConverter());

        assertNotNull("AtomicLong should not be null", atomic);
        assertEquals("AtomicLong value is incorrect", 2_500L, atomic.get());
    }

    @Test
    public void testToBigInteger() {
        Duration dur = Duration.ofSeconds(3, 123_456_789);
        BigInteger result = DurationConversions.toBigInteger(dur, dummyConverter());

        // Expected nanoseconds = seconds * 1_000_000_000 + nanos
        BigInteger expected = BigInteger.valueOf(3).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(123_456_789L));

        assertEquals("BigInteger conversion (nanoseconds) is incorrect", expected, result);
    }

    @Test
    public void testToDoubleAndBigDecimal_Positive() {
        Duration dur = Duration.ofSeconds(7, 800_000_000); // 7.8 seconds
        double d = DurationConversions.toDouble(dur, dummyConverter());
        BigDecimal bd = DurationConversions.toBigDecimal(dur, dummyConverter());

        double expectedDouble = 7 + 800_000_000d / 1_000_000_000d; // 7.8
        BigDecimal expectedBD = BigDecimal.valueOf(expectedDouble);

        assertEquals("Double conversion is incorrect", expectedDouble, d, 0.0);
        assertEquals("BigDecimal conversion is incorrect", 0, expectedBD.compareTo(bd));
    }

    @Test
    public void testToDoubleAndBigDecimal_Negative() {
        // -3 seconds and -400,000,000 nanos = -3.4 seconds
        Duration dur = Duration.ofSeconds(-3).minusNanos(400_000_000);
        double d = DurationConversions.toDouble(dur, dummyConverter());
        BigDecimal bd = DurationConversions.toBigDecimal(dur, dummyConverter());

        double expectedDouble = -3 - 400_000_000d / 1_000_000_000d; // -3.4
        BigDecimal expectedBD = BigDecimal.valueOf(expectedDouble);

        assertEquals("Negative double conversion is incorrect", expectedDouble, d, 0.0);
        assertEquals("Negative BigDecimal conversion is incorrect", 0, expectedBD.compareTo(bd));
    }

    @Test
    public void testToTimestamp() {
        Duration dur = Duration.ofSeconds(10, 123_456_789);
        Timestamp ts = DurationConversions.toTimestamp(dur, dummyConverter());

        Instant expectedInstant = Instant.EPOCH.plus(dur);
        Timestamp expectedTs = Timestamp.from(expectedInstant);

        assertEquals("Timestamp milliseconds part differs", expectedTs.getTime(), ts.getTime());
        assertEquals("Timestamp nanos part differs", expectedTs.getNanos(), ts.getNanos());
    }

    @Test
    public void testToTimestamp_ZeroDuration() {
        Duration dur = Duration.ZERO;
        Timestamp ts = DurationConversions.toTimestamp(dur, dummyConverter());

        Timestamp expected = Timestamp.from(Instant.EPOCH);
        assertEquals("Zero duration should map to epoch timestamp", expected, ts);
    }
}
