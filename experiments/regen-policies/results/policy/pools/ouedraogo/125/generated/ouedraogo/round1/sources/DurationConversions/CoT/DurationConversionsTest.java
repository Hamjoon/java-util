package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test suite for {@link DurationConversions}.
 * The test class resides in the same package so it can access the
 * package‑private static methods.
 */
public class DurationConversionsTest {

    /* ---------------------------------------------------------------------- */
    /* -------------------------- BASIC FUNCTIONALITY ----------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMapBasic() {
        Duration d = Duration.ofSeconds(10, 123456789);
        Map<String, Object> map = DurationConversions.toMap(d, null);
        assertEquals("Map should contain exactly two entries", 2, map.size());
        assertEquals("seconds entry", 10L, map.get("seconds"));
        assertEquals("nanos entry", 123456789, map.get("nanos"));
    }

    @Test
    public void testToLongBasic() {
        Duration d = Duration.ofSeconds(5, 500_000_000); // 5.5 seconds
        long millis = DurationConversions.toLong(d, null);
        assertEquals("5.5 seconds = 5500 ms", 5500L, millis);
    }

    @Test
    public void testToAtomicLongBasic() {
        Duration d = Duration.ofSeconds(2, 250_000_000); // 2.25 seconds
        AtomicLong al = DurationConversions.toAtomicLong(d, null);
        assertEquals("AtomicLong should hold 2250 ms", 2250L, al.get());
    }

    @Test
    public void testToBigIntegerBasic() {
        Duration d = Duration.ofSeconds(3, 400_000_000);
        BigInteger bi = DurationConversions.toBigInteger(d, null);
        // 3 seconds = 3_000_000_000 ns + 400_000_000 ns = 3_400_000_000 ns
        assertEquals(BigInteger.valueOf(3_400_000_000L), bi);
    }

    @Test
    public void testToDoubleBasic() {
        Duration d = Duration.ofSeconds(1, 500_000_000); // 1.5 seconds
        double val = DurationConversions.toDouble(d, null);
        assertEquals("1.5 seconds as double", 1.5, val, 0.0);
    }

    @Test
    public void testToBigDecimalBasic() {
        Duration d = Duration.ofSeconds(2, 250_000_000); // 2.25 seconds
        BigDecimal bd = DurationConversions.toBigDecimal(d, null);
        assertEquals("2.25 seconds as BigDecimal", new BigDecimal("2.25"), bd);
    }

    @Test
    public void testToTimestampBasic() {
        Duration d = Duration.ofSeconds(0, 0);
        Timestamp ts = DurationConversions.toTimestamp(d, null);
        assertEquals("Timestamp for epoch", Timestamp.from(Instant.EPOCH), ts);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- EDGE CASES ------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMapNegativeDuration() {
        Duration d = Duration.ofSeconds(-7, -123); // normalized to -8 seconds + 999,999,877 nanos
        Map<String, Object> map = DurationConversions.toMap(d, null);
        assertEquals(-8L, map.get("seconds"));
        assertEquals(999_999_877, map.get("nanos"));
    }

    @Test(expected = ArithmeticException.class)
    public void testToLongOverflow() {
        // This duration cannot be represented in milliseconds as a long.
        Duration d = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999);
        DurationConversions.toLong(d, null);
    }

    @Test(expected = ArithmeticException.class)
    public void testToAtomicLongOverflow() {
        Duration d = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999);
        DurationConversions.toAtomicLong(d, null);
    }

    @Test
    public void testToBigIntegerVeryLarge() {
        Duration d = Duration.ofSeconds(Long.MAX_VALUE, 999_999_999);
        BigInteger result = DurationConversions.toBigInteger(d, null);
        BigInteger expected = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.valueOf(1_000_000_000L))
                                         .add(BigInteger.valueOf(999_999_999L));
        assertEquals(expected, result);
    }

    @Test
    public void testToDoubleOverflowBecomesInfinity() {
        Duration d = Duration.ofSeconds(Long.MAX_VALUE, 0);
        double val = DurationConversions.toDouble(d, null);
        assertTrue("Result should be positive infinity for huge duration", Double.isInfinite(val));
        assertTrue("Infinity should be positive", val > 0);
    }

    @Test
    public void testToBigDecimalVeryLarge() {
        Duration d = Duration.ofSeconds(Long.MAX_VALUE, 0);
        BigDecimal bd = DurationConversions.toBigDecimal(d, null);
        // The expected value is Long.MAX_VALUE seconds as a decimal (no nanos)
        BigDecimal expected = new BigDecimal(Long.MAX_VALUE);
        assertEquals(expected, bd);
    }

    @Test(expected = DateTimeException.class)
    public void testToTimestampOverflow() {
        // Adding this duration to Instant.EPOCH exceeds the supported range.
        Duration d = Duration.ofSeconds(Long.MAX_VALUE);
        DurationConversions.toTimestamp(d, null);
    }
}
