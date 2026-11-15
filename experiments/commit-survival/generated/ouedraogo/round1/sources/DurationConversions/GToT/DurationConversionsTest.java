package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.DateTimeException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class DurationConversionsTest {

    /* --------------------------------------------------------------------- */
    /* Helper – a dummy Converter (the methods under test ignore it)        */
    /* --------------------------------------------------------------------- */
    private static final Converter DUMMY_CONVERTER = null;   // allowed – not used

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* --------------------------------------------------------------------- */
    /* 1. toMap – normal cases                                                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToMapBasic() {
        Duration d = Duration.ofSeconds(12, 345_678_900); // 12 s + 345 678 900 ns
        Map<String, Object> map = DurationConversions.toMap(d, DUMMY_CONVERTER);
        assertEquals(2, map.size());
        assertTrue(map.containsKey("seconds"));
        assertTrue(map.containsKey("nanos"));
        assertEquals(12L, map.get("seconds"));
        assertEquals(345_678_900, map.get("nanos"));
    }

    @Test
    public void testToMapNegativeAndZero() {
        Duration zero = Duration.ZERO;
        Map<String, Object> zeroMap = DurationConversions.toMap(zero, DUMMY_CONVERTER);
        assertEquals(0L, zeroMap.get("seconds"));
        assertEquals(0, zeroMap.get("nanos"));

        Duration negative = Duration.ofSeconds(-5, -123_456_789);
        // Java normalises: -5 seconds -123_456_789 nanos => -6 seconds + 876_543_211 nanos
        Map<String, Object> negMap = DurationConversions.toMap(negative, DUMMY_CONVERTER);
        assertEquals(-6L, negMap.get("seconds"));
        assertEquals(876_543_211, negMap.get("nanos"));
    }

    @Test
    public void testToMapInvalidInputThrowsClassCastException() {
        thrown.expect(ClassCastException.class);
        DurationConversions.toMap("not a duration", DUMMY_CONVERTER);
    }

    /* --------------------------------------------------------------------- */
    /* 2. toLong – basic & overflow                                            */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToLongBasic() {
        Duration d = Duration.ofSeconds(5, 500_000_000); // 5.5 s
        long millis = DurationConversions.toLong(d, DUMMY_CONVERTER);
        assertEquals(5_500L, millis);
    }

    @Test
    public void testToLongOverflow() {
        // Duration larger than Long.MAX_VALUE milliseconds will overflow
        Duration d = Duration.ofSeconds(Long.MAX_VALUE / 1000 + 1);
        thrown.expect(ArithmeticException.class);
        DurationConversions.toLong(d, DUMMY_CONVERTER);
    }

    @Test
    public void testToLongInvalidInput() {
        thrown.expect(ClassCastException.class);
        DurationConversions.toLong(123, DUMMY_CONVERTER);
    }

    /* --------------------------------------------------------------------- */
    /* 3. toAtomicLong – basic & negative                                       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToAtomicLongBasic() {
        Duration d = Duration.ofMillis(1234);
        AtomicLong al = DurationConversions.toAtomicLong(d, DUMMY_CONVERTER);
        assertEquals(1_234L, al.get());
    }

    @Test
    public void testToAtomicLongNegative() {
        Duration d = Duration.ofSeconds(-2, -300_000_000); // normalises to -3s + 700_000_000ns
        AtomicLong al = DurationConversions.toAtomicLong(d, DUMMY_CONVERTER);
        // -2.3 s = -2300 ms (rounded toward zero by Duration.toMillis)
        assertEquals(-2_300L, al.get());
    }

    @Test
    public void testToAtomicLongInvalidInput() {
        thrown.expect(ClassCastException.class);
        DurationConversions.toAtomicLong(new Object(), DUMMY_CONVERTER);
    }

    /* --------------------------------------------------------------------- */
    /* 4. toBigInteger – conversion to total nanoseconds                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToBigIntegerBasic() {
        Duration d = Duration.ofSeconds(3, 250_000_000); // 3.25 s
        BigInteger bi = DurationConversions.toBigInteger(d, DUMMY_CONVERTER);
        // 3 s = 3 000 000 000 ns + 250 000 000 ns = 3_250_000_000
        assertEquals(BigInteger.valueOf(3_250_000_000L), bi);
    }

    @Test
    public void testToBigIntegerLargeValues() {
        long hugeSeconds = Long.MAX_VALUE / 2; // still safe for multiplication by 1_000_000_000
        Duration d = Duration.ofSeconds(hugeSeconds, 999_999_999);
        BigInteger expected = BigInteger.valueOf(hugeSeconds).multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(999_999_999L));
        assertEquals(expected, DurationConversions.toBigInteger(d, DUMMY_CONVERTER));
    }

    @Test
    public void testToBigIntegerInvalidInput() {
        thrown.expect(ClassCastException.class);
        DurationConversions.toBigInteger(Boolean.TRUE, DUMMY_CONVERTER);
    }

    /* --------------------------------------------------------------------- */
    /* 5. toDouble – uses BigDecimalConversions.secondsAndNanosToDouble      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToDoubleBasic() {
        Duration d = Duration.ofSeconds(2, 500_000_000); // 2.5 s
        double val = DurationConversions.toDouble(d, DUMMY_CONVERTER);
        assertEquals(2.5, val, 0.0);
    }

    @Test
    public void testToDoublePrecision() {
        // Use a value that cannot be represented exactly in binary floating point
        Duration d = Duration.ofSeconds(1, 333_333_333); // 1.333333333 s
        double val = DurationConversions.toDouble(d, DUMMY_CONVERTER);
        // The expected double is the same as converting via BigDecimal then doubleValue()
        BigDecimal expected = BigDecimalConversions.secondsAndNanosToDouble(1, 333_333_333);
        assertEquals(expected.doubleValue(), val, 0.0);
    }

    @Test
    public void testToDoubleInvalidInput() {
        thrown.expect(ClassCastException.class);
        DurationConversions.toDouble(new Object(), DUMMY_CONVERTER);
    }

    /* --------------------------------------------------------------------- */
    /* 6. toBigDecimal – same conversion as toDouble but returns BigDecimal    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToBigDecimalBasic() {
        Duration d = Duration.ofSeconds(0, 123_456_789); // 0.123456789 s
        BigDecimal bd = DurationConversions.toBigDecimal(d, DUMMY_CONVERTER);
        BigDecimal expected = BigDecimalConversions.secondsAndNanosToDouble(0, 123_456_789);
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToBigDecimalLarge() {
        Duration d = Duration.ofSeconds(Long.MAX_VALUE / 2, 0);
        BigDecimal bd = DurationConversions.toBigDecimal(d, DUMMY_CONVERTER);
        BigDecimal expected = BigDecimalConversions.secondsAndNanosToDouble(Long.MAX_VALUE / 2, 0);
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToBigDecimalInvalidInput() {
        thrown.expect(ClassCastException.class);
        DurationConversions.toBigDecimal(null, DUMMY_CONVERTER);
    }

    /* --------------------------------------------------------------------- */
    /* 7. toTimestamp – conversion to java.sql.Timestamp                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToTimestampBasic() {
        Duration d = Duration.ofSeconds(10, 0);
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        // epoch + 10 seconds = 1970‑01‑01 00:00:10 UTC
        assertEquals(Timestamp.from(Instant.EPOCH.plusSeconds(10)), ts);
    }

    @Test
    public void testToTimestampWithNanos() {
        Duration d = Duration.ofSeconds(0, 500_000_000); // 0.5 s
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        Timestamp expected = Timestamp.from(Instant.EPOCH.plusNanos(500_000_000));
        assertEquals(expected, ts);
        // The nanos part of Timestamp is stored in the fractional part of the long value
        assertEquals(500_000_000, ts.getNanos());
    }

    @Test
    public void testToTimestampNegativeDuration() {
        Duration d = Duration.ofSeconds(-5, -200_000_000); // normalises to -6 s + 800_000_000 ns
        Timestamp ts = DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
        Instant expectedInstant = Instant.EPOCH.plus(d);
        assertEquals(Timestamp.from(expectedInstant), ts);
    }

    @Test
    public void testToTimestampOverflowThrows() {
        // Instant.MAX is far beyond what Duration can add without overflow, but we can provoke overflow
        Duration d = Duration.ofSeconds(Long.MAX_VALUE, 0);
        thrown.expect(DateTimeException.class);
        DurationConversions.toTimestamp(d, DUMMY_CONVERTER);
    }

    @Test
    public void testToTimestampInvalidInput() {
        thrown.expect(ClassCastException.class);
        DurationConversions.toTimestamp("not a duration", DUMMY_CONVERTER);
    }
}
