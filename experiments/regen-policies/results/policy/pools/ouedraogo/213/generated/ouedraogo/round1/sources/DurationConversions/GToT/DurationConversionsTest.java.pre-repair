package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.BeforeClass;
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
 * The tests cover:
 * <ul>
 *   <li>basic successful conversions for all static methods</li>
 *   <li>edge cases such as zero, negative and very large durations</li>
 *   <li>error handling – passing an object of the wrong type should raise {@link ClassCastException}</li>
 * </ul>
 *
 * The class under test has package‑private static methods, therefore the test class lives in the same
 * package so it can invoke them directly.
 */
public class DurationConversionsTest
{
    /** Converter is required by the signatures but never used by the conversion logic. */
    private static Converter DUMMY_CONVERTER;

    @BeforeClass
    public static void setUpClass()
    {
        // ConverterOptions is not needed for the tested methods; passing null is acceptable.
        DUMMY_CONVERTER = new Converter(null);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- toMap() ----------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToMap_Basic()
    {
        Duration d = Duration.ofSeconds(5, 123_456_789);
        Map<String, Object> map = DurationConversions.toMap(d, DUMMY_CONVERTER);
        assertEquals("Map should contain exactly one entry", 1, map.size());
        assertTrue("Map must contain the DURATION key", map.containsKey(MapConversions.DURATION));
        assertEquals("Stored string must be the duration's ISO‑8601 representation",
                     d.toString(),
                     map.get(MapConversions.DURATION));
    }

    @Test
    public void testToMap_ZeroDuration()
    {
        Duration d = Duration.ZERO;
        Map<String, Object> map = DurationConversions.toMap(d, DUMMY_CONVERTER);
        assertEquals("ZERO duration must be represented correctly", d.toString(),
                     map.get(MapConversions.DURATION));
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongType()
    {
        DurationConversions.toMap("not a duration", DUMMY_CONVERTER);
    }

    /* ---------------------------------------------------------------------- */
    /* --------------------------- toLong() ---------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLong_Basic()
    {
        Duration d = Duration.ofMinutes(2).plusSeconds(30).plusMillis(250);
        long expected = d.toMillis();
        assertEquals(expected, DurationConversions.toLong(d, DUMMY_CONVERTER));
    }

    @Test
    public void testToLong_Negative()
    {
        Duration d = Duration.ofSeconds(-10).minusNanos(500_000_000); // -10.5 seconds
        long expected = d.toMillis(); // should be -10500
        assertEquals(expected, DurationConversions.toLong(d, DUMMY_CONVERTER));
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType()
    {
        DurationConversions.toLong(12345, DUMMY_CONVERTER);
    }

    /* ---------------------------------------------------------------------- */
    /* ------------------------ toAtomicLong() ------------------------------ */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicLong_Basic()
    {
        Duration d = Duration.ofHours(1).plusMillis(1);
        AtomicLong al = DurationConversions.toAtomicLong(d, DUMMY_CONVERTER);
        assertEquals(d.toMillis(), al.get());
    }

    @Test
    public void testToAtomicLong_Negative()
    {
        Duration d = Duration.ofSeconds(-5).minusNanos(250_000_000); // -5.25 s
        AtomicLong al = DurationConversions.toAtomicLong(d, DUMMY_CONVERTER);
        assertEquals(d.toMillis(), al.get());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_WrongType()
    {
        DurationConversions.toAtomicLong(new Object(), DUMMY_CONVERTER);
    }

    /* ---------------------------------------------------------------------- */
    /* ------------------------ toBigInteger() ----------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigInteger_Basic()
    {
        Duration d = Duration.ofSeconds(3, 250_000_000); // 3.25 s
        // Expected = seconds * 1_000_000_000 + nanos
        BigInteger expected = BigInteger.valueOf(3).multiply(BigIntegerConversions.BILLION)
                                         .add(BigInteger.valueOf(250_000_000));
        assertEquals(expected, DurationConversions.toBigInteger(d, DUMMY_CONVERTER));
    }

    @Test
    public void testToBigInteger_Negative()
    {
        Duration d = Duration.ofSeconds(-2).minusNanos(500_000_000); // -2.5 s
        // Normalized representation: -3 seconds + 500_000_000 nanos
        // Duration#getSeconds() returns -3, getNano() returns 500_000_000
        BigInteger expected = BigInteger.valueOf(-3).multiply(BigIntegerConversions.BILLION)
                                         .add(BigInteger.valueOf(500_000_000));
        assertEquals(expected, DurationConversions.toBigInteger(d, DUMMY_CONVERTER));
    }

    @Test
    public void testToBigInteger_VeryLarge()
    {
        // Use a value close to Long.MAX_VALUE seconds (still fits in a Duration)
        long seconds = Long.MAX_VALUE / 1_000_000_000L; // about 9.22e9 seconds
        Duration d = Duration.ofSeconds(seconds);
        BigInteger expected = BigInteger.valueOf(seconds).multiply(BigIntegerConversions.BILLION);
        assertEquals(expected, DurationConversions.toBigInteger(d, DUMMY_CONVERTER));
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_WrongType()
    {
        DurationConversions.toBigInteger(Boolean.TRUE, DUMMY_CONVERTER);
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- toDouble() -------------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDouble_Basic()
    {
        Duration d = Duration.ofSeconds(1, 500_000_000); // 1.5 seconds
        double expected = 1.5;
        assertEquals(expected, DurationConversions.toDouble(d, DUMMY_CONVERTER), 0.000_001);
    }

    @Test
    public void testToDouble_Negative()
    {
        Duration d = Duration.ofSeconds(-3).minusNanos(250_000_000); // -3.25 s
        double expected = -3.25;
        assertEquals(expected, DurationConversions.toDouble(d, DUMMY_CONVERTER), 0.000_001);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_WrongType()
    {
        DurationConversions.toDouble(3.14, DUMMY_CONVERTER);
    }

    /* ---------------------------------------------------------------------- */
    /* ------------------------ toBigDecimal() ------------------------------ */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigDecimal_Basic()
    {
        Duration d = Duration.ofSeconds(2, 75_000_000); // 2.075 seconds
        BigDecimal expected = BigDecimal.valueOf(2.075);
        assertEquals(0, expected.compareTo(DurationConversions.toBigDecimal(d, DUMMY_CONVERTER)));
    }

    @Test
    public void testToBigDecimal_Negative()
    {
        Duration d = Duration.ofSeconds(-1).minusNanos(400_000_000); // -1.4 s
        BigDecimal expected = BigDecimal.valueOf(-1.4);
        assertEquals(0, expected.compareTo(DurationConversions.toBigDecimal(d, DUMMY_CONVERTER)));
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_WrongType()
    {
        DurationConversions.toBigDecimal(new Object(), DUMMY_CONVERTER);
    }

    /* ---------------------------------------------------------------------- */
    /* ------------------------ toTimestamp() ------------------------------ */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToTimestamp_Basic()
    {
        Duration d = Duration.ofSeconds(10, 123_456_789);
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        Instant expectedInstant = Instant.EPOCH.plus(d);
        assertEquals(Timestamp.from(expectedInstant), ts);
    }

    @Test
    public void testToTimestamp_Negative()
    {
        Duration d = Duration.ofSeconds(-5).minusNanos(500_000_000); // -5.5 s
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        Instant expectedInstant = Instant.EPOCH.plus(d);
        assertEquals(Timestamp.from(expectedInstant), ts);
    }

    @Test(expected = ClassCastException.class)
    public void testToTimestamp_WrongType()
    {
        DurationConversions.toTimestamp(Instant.now(), DUMMY_CONVERTER);
    }
}
