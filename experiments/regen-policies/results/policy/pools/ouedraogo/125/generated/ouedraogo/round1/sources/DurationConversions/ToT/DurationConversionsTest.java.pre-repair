package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link DurationConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Typical values (positive, zero, negative)</li>
 *   <li>Boundary / edge values (large seconds, nanosecond overflow handling)</li>
 *   <li>Incorrect usage (null source, wrong source type)</li>
 *   <li>Behaviour of the {@code CompactMap} produced by {@code toMap}</li>
 * </ul>
 * All tests are written in the same package as {@code DurationConversions}
 * so that the package‑private static methods are directly accessible.
 * The {@code Converter} argument is unused by the implementation; a {@code null}
 * reference is therefore sufficient.
 */
public class DurationConversionsTest {

    /* ---------------------------------------------------------------------- */
    /* Helper utilities                                                       */
    /* ---------------------------------------------------------------------- */

    /** A dummy {@code Converter} instance – not used by the methods under test. */
    private static final Converter DUMMY_CONVERTER = null;

    /** Convenience wrapper to assert that a {@code ClassCastException} is thrown. */
    private static void assertClassCast(Runnable r) {
        try {
            r.run();
            fail("Expected ClassCastException");
        } catch (ClassCastException expected) {
            // success
        }
    }

    /** Convenience wrapper to assert that a {@code NullPointerException} is thrown. */
    private static void assertNPE(Runnable r) {
        try {
            r.run();
            fail("Expected NullPointerException");
        } catch (NullPointerException expected) {
            // success
        }
    }

    /* ---------------------------------------------------------------------- */
    /* toMap                                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toMapTypicalPositiveDuration() {
        Duration d = Duration.ofSeconds(12, 345_678_901);
        Map<String, Object> map = DurationConversions.toMap(d, DUMMY_CONVERTER);

        assertEquals(2, map.size());
        assertTrue(map.containsKey("seconds"));
        assertTrue(map.containsKey("nanos"));
        assertEquals(12L, map.get("seconds"));
        assertEquals(345_678_901, map.get("nanos"));

        // Verify insertion order – seconds first, nanos second
        Iterator<String> it = map.keySet().iterator();
        assertEquals("seconds", it.next());
        assertEquals("nanos", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void toMapZeroDuration() {
        Duration d = Duration.ZERO;
        Map<String, Object> map = DurationConversions.toMap(d, DUMMY_CONVERTER);
        assertEquals(0L, map.get("seconds"));
        assertEquals(0, map.get("nanos"));
    }

    @Test
    public void toMapNegativeDuration() {
        // Duration normalises negative nanos, e.g. -1 sec + 500_000_000 ns => -500_000_000 ns
        Duration d = Duration.ofSeconds(-3, -200_000_000);
        Map<String, Object> map = DurationConversions.toMap(d, DUMMY_CONVERTER);
        // Normalised values:
        // seconds = -4, nanos = 800_000_000
        assertEquals(-4L, map.get("seconds"));
        assertEquals(800_000_000, map.get("nanos"));
    }

    @Test
    public void toMapNullSourceThrowsNPE() {
        assertNPE(() -> DurationConversions.toMap(null, DUMMY_CONVERTER));
    }

    @Test
    public void toMapWrongSourceTypeThrowsClassCast() {
        assertClassCast(() -> DurationConversions.toMap("not a duration", DUMMY_CONVERTER));
    }

    /* ---------------------------------------------------------------------- */
    /* toLong                                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toLongTypical() {
        Duration d = Duration.ofSeconds(5, 123_456_789);
        long millis = DurationConversions.toLong(d, DUMMY_CONVERTER);
        // 5 seconds = 5000 ms, plus 123 ms from nanoseconds (fraction ignored)
        assertEquals(5_000L + 123L, millis);
    }

    @Test
    public void toLongNegativeDuration() {
        Duration d = Duration.ofSeconds(-2, -500_000_000); // normalized to -3 sec + 500_000_000 ns
        long millis = DurationConversions.toLong(d, DUMMY_CONVERTER);
        // -3 seconds = -3000 ms, +500 ms = -2500 ms
        assertEquals(-2_500L, millis);
    }

    @Test
    public void toLongZeroDuration() {
        assertEquals(0L, DurationConversions.toLong(Duration.ZERO, DUMMY_CONVERTER));
    }

    @Test
    public void toLongNullSourceThrowsNPE() {
        assertNPE(() -> DurationConversions.toLong(null, DUMMY_CONVERTER));
    }

    @Test
    public void toLongWrongSourceTypeThrowsClassCast() {
        assertClassCast(() -> DurationConversions.toLong(Boolean.TRUE, DUMMY_CONVERTER));
    }

    /* ---------------------------------------------------------------------- */
    /* toAtomicLong                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toAtomicLongTypical() {
        Duration d = Duration.ofSeconds(1, 999_999_999);
        AtomicLong al = DurationConversions.toAtomicLong(d, DUMMY_CONVERTER);
        assertEquals(1_999L, al.get()); // 1 second = 1000 ms + 999 ms (nanoseconds truncated)
    }

    @Test
    public void toAtomicLongNegative() {
        Duration d = Duration.ofSeconds(-1, -1); // normalises to -2 seconds + 999_999_999 ns
        AtomicLong al = DurationConversions.toAtomicLong(d, DUMMY_CONVERTER);
        // -2 seconds = -2000 ms, +999 ms = -1001 ms
        assertEquals(-1_001L, al.get());
    }

    @Test
    public void toAtomicLongZero() {
        AtomicLong al = DurationConversions.toAtomicLong(Duration.ZERO, DUMMY_CONVERTER);
        assertEquals(0L, al.get());
    }

    @Test
    public void toAtomicLongNullSourceThrowsNPE() {
        assertNPE(() -> DurationConversions.toAtomicLong(null, DUMMY_CONVERTER));
    }

    @Test
    public void toAtomicLongWrongSourceTypeThrowsClassCast() {
        assertClassCast(() -> DurationConversions.toAtomicLong(42, DUMMY_CONVERTER));
    }

    /* ---------------------------------------------------------------------- */
    /* toBigInteger                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toBigIntegerTypical() {
        Duration d = Duration.ofSeconds(2, 500_000_000);
        BigInteger bi = DurationConversions.toBigInteger(d, DUMMY_CONVERTER);
        // 2 seconds = 2_000_000_000 ns + 500_000_000 ns = 2_500_000_000 ns
        assertEquals(BigInteger.valueOf(2_500_000_000L), bi);
    }

    @Test
    public void toBigIntegerLargeValues() {
        long seconds = Long.MAX_VALUE / 2; // large but safe for multiplication by 1_000_000_000
        Duration d = Duration.ofSeconds(seconds, 999_999_999);
        BigInteger expected = BigInteger.valueOf(seconds).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(999_999_999L));
        assertEquals(expected, DurationConversions.toBigInteger(d, DUMMY_CONVERTER));
    }

    @Test
    public void toBigIntegerNegativeDuration() {
        Duration d = Duration.ofSeconds(-3, 200_000_000); // -2 seconds + -800_000_000 ns after normalisation
        // Normalised: seconds = -3, nanos = 200_000_000 (still negative total nanoseconds)
        BigInteger bi = DurationConversions.toBigInteger(d, DUMMY_CONVERTER);
        BigInteger expected = BigInteger.valueOf(-3L).multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(200_000_000L));
        assertEquals(expected, bi);
    }

    @Test
    public void toBigIntegerZero() {
        assertEquals(BigInteger.ZERO, DurationConversions.toBigInteger(Duration.ZERO, DUMMY_CONVERTER));
    }

    @Test
    public void toBigIntegerNullSourceThrowsNPE() {
        assertNPE(() -> DurationConversions.toBigInteger(null, DUMMY_CONVERTER));
    }

    @Test
    public void toBigIntegerWrongSourceTypeThrowsClassCast() {
        assertClassCast(() -> DurationConversions.toBigInteger(new Object(), DUMMY_CONVERTER));
    }

    /* ---------------------------------------------------------------------- */
    /* toDouble                                                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toDoubleTypical() {
        Duration d = Duration.ofSeconds(3, 250_000_000);
        double val = DurationConversions.toDouble(d, DUMMY_CONVERTER);
        assertEquals(3.25, val, 0.000_000_001);
    }

    @Test
    public void toDoubleNegative() {
        Duration d = Duration.ofSeconds(-1, -500_000_000); // normalised to -2 sec + 500_000_000 ns
        double val = DurationConversions.toDouble(d, DUMMY_CONVERTER);
        // -2 + 0.5 = -1.5
        assertEquals(-1.5, val, 0.000_000_001);
    }

    @Test
    public void toDoubleZero() {
        assertEquals(0.0, DurationConversions.toDouble(Duration.ZERO, DUMMY_CONVERTER), 0.0);
    }

    @Test
    public void toDoubleNullSourceThrowsNPE() {
        assertNPE(() -> DurationConversions.toDouble(null, DUMMY_CONVERTER));
    }

    @Test
    public void toDoubleWrongSourceTypeThrowsClassCast() {
        assertClassCast(() -> DurationConversions.toDouble("string", DUMMY_CONVERTER));
    }

    /* ---------------------------------------------------------------------- */
    /* toBigDecimal                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toBigDecimalTypical() {
        Duration d = Duration.ofSeconds(7, 123_456_789);
        BigDecimal bd = DurationConversions.toBigDecimal(d, DUMMY_CONVERTER);
        // Expected: 7.123456789
        assertEquals(new BigDecimal("7.123456789"), bd);
    }

    @Test
    public void toBigDecimalNegative() {
        Duration d = Duration.ofSeconds(-4, -250_000_000); // normalises to -5 sec + 750_000_000 ns
        BigDecimal bd = DurationConversions.toBigDecimal(d, DUMMY_CONVERTER);
        assertEquals(new BigDecimal("-4.25"), bd);
    }

    @Test
    public void toBigDecimalZero() {
        assertEquals(BigDecimal.ZERO, DurationConversions.toBigDecimal(Duration.ZERO, DUMMY_CONVERTER));
    }

    @Test
    public void toBigDecimalNullSourceThrowsNPE() {
        assertNPE(() -> DurationConversions.toBigDecimal(null, DUMMY_CONVERTER));
    }

    @Test
    public void toBigDecimalWrongSourceTypeThrowsClassCast() {
        assertClassCast(() -> DurationConversions.toBigDecimal(Instant.now(), DUMMY_CONVERTER));
    }

    /* ---------------------------------------------------------------------- */
    /* toTimestamp                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void toTimestampTypical() {
        Duration d = Duration.ofSeconds(10, 500_000_000); // 10.5 seconds after epoch
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        long expectedMillis = 10_500L;
        assertEquals(expectedMillis, ts.getTime());
    }

    @Test
    public void toTimestampNegative() {
        Duration d = Duration.ofSeconds(-2, -250_000_000); // normalises to -3 sec + 750_000_000 ns
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        // -3 seconds = -3000 ms, +750 ms = -2250 ms
        assertEquals(-2_250L, ts.getTime());
    }

    @Test
    public void toTimestampZero() {
        Timestamp ts = DurationConversions.toTimestamp(Duration.ZERO, DUMMY_CONVERTER);
        assertEquals(Instant.EPOCH.toEpochMilli(), ts.getTime());
    }

    @Test
    public void toTimestampLargeDuration() {
        // 2^31 milliseconds ≈ 24 days – well within Timestamp range
        Duration d = Duration.ofMillis(Integer.MAX_VALUE);
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        assertEquals(Integer.MAX_VALUE, ts.getTime());
    }

    @Test
    public void toTimestampNullSourceThrowsNPE() {
        assertNPE(() -> DurationConversions.toTimestamp(null, DUMMY_CONVERTER));
    }

    @Test
    public void toTimestampWrongSourceTypeThrowsClassCast() {
        assertClassCast(() -> DurationConversions.toTimestamp(BigDecimal.ONE, DUMMY_CONVERTER));
    }
}
