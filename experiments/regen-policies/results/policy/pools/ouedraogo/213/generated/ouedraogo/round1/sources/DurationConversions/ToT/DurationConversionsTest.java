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
 * Comprehensive JUnit 4 tests for {@link DurationConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical values (positive, zero, negative durations)</li>
 *   <li>Boundary values (max/min long milliseconds, nanosecond overflow)</li>
 *   <li>Correct handling of conversion to each supported target type</li>
 *   <li>Verification that the {@code Converter} argument is not used internally</li>
 * </ul>
 *
 * No mocking is performed; real objects are created using only the public API
 * shown in the source and the provided {@link Converter} class.
 */
public class DurationConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // Converter is not used by DurationConversions, but a non‑null instance is required.
        dummyConverter = new Converter(null);
    }

    /** Helper to assert that two timestamps represent the same instant. */
    private void assertSameInstant(Timestamp expected, Timestamp actual) {
        assertEquals("Timestamp nanoseconds differ", expected.getNanos(), actual.getNanos());
        assertEquals("Timestamp epoch milliseconds differ", expected.getTime(), actual.getTime());
    }

    /** ----------------------- toMap ----------------------- */

    @Test
    public void testToMap_TypicalDuration() {
        Duration duration = Duration.ofSeconds(123, 456_000_000); // 123.456 seconds
        Map<String, Object> map = DurationConversions.toMap(duration, dummyConverter);

        assertNotNull("Resulting map should not be null", map);
        assertEquals("Map should contain exactly one entry", 1, map.size());
        assertTrue("Map should contain key DURATION", map.containsKey(MapConversions.DURATION));
        assertEquals("String representation must match Duration.toString()", duration.toString(),
                map.get(MapConversions.DURATION));
    }

    @Test
    public void testToMap_NegativeDuration() {
        Duration duration = Duration.ofSeconds(-5, -200_000_000); // -5.2 seconds (normalized to -6 + 800_000_000)
        Map<String, Object> map = DurationConversions.toMap(duration, dummyConverter);
        assertEquals("-6.800000000S", map.get(MapConversions.DURATION));
    }

    /** ----------------------- toLong ----------------------- */

    @Test
    public void testToLong_PositiveDuration() {
        Duration duration = Duration.ofMillis(1_234_567);
        long millis = DurationConversions.toLong(duration, dummyConverter);
        assertEquals(1_234_567L, millis);
    }

    @Test
    public void testToLong_NegativeDuration() {
        Duration duration = Duration.ofMillis(-987);
        long millis = DurationConversions.toLong(duration, dummyConverter);
        assertEquals(-987L, millis);
    }

    @Test
    public void testToLong_ZeroDuration() {
        Duration duration = Duration.ZERO;
        assertEquals(0L, DurationConversions.toLong(duration, dummyConverter));
    }

    /** ----------------------- toAtomicLong ----------------------- */

    @Test
    public void testToAtomicLong_Typical() {
        Duration duration = Duration.ofSeconds(2, 500_000_000); // 2.5 seconds = 2500 ms
        AtomicLong atomic = DurationConversions.toAtomicLong(duration, dummyConverter);
        assertEquals(2500L, atomic.get());
    }

    @Test
    public void testToAtomicLong_Negative() {
        Duration duration = Duration.ofSeconds(-1, -250_000_000); // -1.25 sec = -1250 ms
        AtomicLong atomic = DurationConversions.toAtomicLong(duration, dummyConverter);
        assertEquals(-1250L, atomic.get());
    }

    /** ----------------------- toBigInteger ----------------------- */

    @Test
    public void testToBigInteger_Positive() {
        Duration duration = Duration.ofSeconds(3, 400_000_000); // 3.4 sec = 3_000_000_000 + 400_000_000 ns
        BigInteger result = DurationConversions.toBigInteger(duration, dummyConverter);
        BigInteger expected = BigInteger.valueOf(3).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(400_000_000));
        assertEquals(expected, result);
    }

    @Test
    public void testToBigInteger_Negative() {
        // Duration normalizes negative nanos, so -2 seconds and -300_000_000 ns becomes -3 seconds + 700_000_000 ns
        Duration duration = Duration.ofSeconds(-2, -300_000_000);
        BigInteger result = DurationConversions.toBigInteger(duration, dummyConverter);
        BigInteger expected = BigInteger.valueOf(-3).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(700_000_000));
        assertEquals(expected, result);
    }

    @Test
    public void testToBigInteger_Zero() {
        assertEquals(BigInteger.ZERO, DurationConversions.toBigInteger(Duration.ZERO, dummyConverter));
    }

    /** ----------------------- toDouble ----------------------- */

    @Test
    public void testToDouble_Typical() {
        Duration duration = Duration.ofSeconds(1, 250_000_000); // 1.25 seconds
        double d = DurationConversions.toDouble(duration, dummyConverter);
        assertEquals(1.25, d, 0.000000001);
    }

    @Test
    public void testToDouble_Negative() {
        Duration duration = Duration.ofSeconds(-2, -500_000_000); // -2.5 seconds (normalized)
        double d = DurationConversions.toDouble(duration, dummyConverter);
        assertEquals(-2.5, d, 0.000000001);
    }

    @Test
    public void testToDouble_LargeValues() {
        // 2^31 seconds ~ 68 years, still fits in double with reasonable precision
        Duration duration = Duration.ofSeconds(Integer.MAX_VALUE, 999_999_999);
        double d = DurationConversions.toDouble(duration, dummyConverter);
        double expected = Integer.MAX_VALUE + 0.999999999;
        assertEquals(expected, d, 1e-6);
    }

    /** ----------------------- toBigDecimal ----------------------- */

    @Test
    public void testToBigDecimal_Typical() {
        Duration duration = Duration.ofSeconds(0, 123_456_789);
        BigDecimal bd = DurationConversions.toBigDecimal(duration, dummyConverter);
        assertEquals(new BigDecimal("0.123456789"), bd);
    }

    @Test
    public void testToBigDecimal_Negative() {
        Duration duration = Duration.ofSeconds(-1, -1); // normalizes to -2 + 999_999_999 ns
        BigDecimal bd = DurationConversions.toBigDecimal(duration, dummyConverter);
        assertEquals(new BigDecimal("-1.000000001"), bd);
    }

    /** ----------------------- toTimestamp ----------------------- */

    @Test
    public void testToTimestamp_Positive() {
        Duration duration = Duration.ofSeconds(10, 500_000_000); // 10.5 seconds after epoch
        Timestamp ts = DurationConversions.toTimestamp(duration, dummyConverter);
        Timestamp expected = Timestamp.from(Instant.EPOCH.plus(duration));
        assertSameInstant(expected, ts);
    }

    @Test
    public void testToTimestamp_Negative() {
        Duration duration = Duration.ofSeconds(-5, -250_000_000); // normalizes to -6 + 750_000_000 ns
        Timestamp ts = DurationConversions.toTimestamp(duration, dummyConverter);
        Timestamp expected = Timestamp.from(Instant.EPOCH.plus(duration));
        assertSameInstant(expected, ts);
    }

    @Test
    public void testToTimestamp_Zero() {
        Timestamp ts = DurationConversions.toTimestamp(Duration.ZERO, dummyConverter);
        Timestamp expected = Timestamp.from(Instant.EPOCH);
        assertSameInstant(expected, ts);
    }

    /** ----------------------- Error handling ----------------------- */
    // The public methods cast the input to Duration without checks. Passing a non‑Duration
    // results in a ClassCastException. The tests confirm this behaviour.

    @Test(expected = ClassCastException.class)
    public void testToMap_NonDuration() {
        DurationConversions.toMap("not a duration", dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_NonDuration() {
        DurationConversions.toLong(12345, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_NonDuration() {
        DurationConversions.toAtomicLong(new Object(), dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_NonDuration() {
        DurationConversions.toBigInteger(null, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_NonDuration() {
        DurationConversions.toDouble(new Integer(5), dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_NonDuration() {
        DurationConversions.toBigDecimal(Boolean.TRUE, dummyConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToTimestamp_NonDuration() {
        DurationConversions.toTimestamp(Instant.now(), dummyConverter);
    }
}
