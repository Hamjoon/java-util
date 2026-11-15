package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.convert.MapConversions.DURATION;
import static org.junit.Assert.*;

public class DurationConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Converter is not used by DurationConversions, but a non‑null instance is required.
        // The library provides a no‑arg constructor for ConverterOptions, which we can use.
        this.converter = new Converter(new ConverterOptions());
    }

    @Test
    public void testToMapProducesCorrectEntry() {
        Duration duration = Duration.ofSeconds(5, 123_456_789); // PT5.123456789S
        Map<String, Object> map = DurationConversions.toMap(duration, converter);

        assertNotNull("Resulting map must not be null", map);
        assertEquals("Map must contain exactly one entry", 1, map.size());
        assertTrue("Map must contain the DURATION key", map.containsKey(DURATION));

        Object value = map.get(DURATION);
        assertTrue("Value must be a String", value instanceof String);
        assertEquals("String representation must match Duration.toString()", duration.toString(), value);
    }

    @Test
    public void testToLongReturnsMilliseconds() {
        Duration duration = Duration.ofMillis(12_345);
        long millis = DurationConversions.toLong(duration, converter);
        assertEquals("Milliseconds should match Duration.toMillis()", 12_345L, millis);
    }

    @Test
    public void testToAtomicLongReturnsMilliseconds() {
        Duration duration = Duration.ofMillis(987_654);
        AtomicLong atomic = DurationConversions.toAtomicLong(duration, converter);
        assertNotNull("AtomicLong should not be null", atomic);
        assertEquals("AtomicLong value must equal Duration.toMillis()", 987_654L, atomic.get());
    }

    @Test
    public void testToBigIntegerConvertsToNanoseconds_Positive() {
        Duration duration = Duration.ofSeconds(2, 500); // 2 seconds + 500 nanos
        BigInteger result = DurationConversions.toBigInteger(duration, converter);
        BigInteger expected = BigInteger.valueOf(2L).multiply(BigIntegerConversions.BILLION).add(BigInteger.valueOf(500L));
        assertEquals("BigInteger should represent total nanoseconds", expected, result);
    }

    @Test
    public void testToBigIntegerConvertsToNanoseconds_Negative() {
        Duration duration = Duration.ofSeconds(-3, -250); // -3 seconds -250 nanos
        // Java Duration normalises the sign, resulting in -3 seconds and -250 nanos = -3.000000250 seconds
        BigInteger result = DurationConversions.toBigInteger(duration, converter);
        BigInteger expected = BigInteger.valueOf(-3L).multiply(BigIntegerConversions.BILLION).add(BigInteger.valueOf(-250L));
        assertEquals("BigInteger should correctly handle negative durations", expected, result);
    }

    @Test
    public void testToDoubleReturnsSecondsWithFraction() {
        Duration duration = Duration.ofSeconds(1, 500_000_000); // 1.5 seconds
        double d = DurationConversions.toDouble(duration, converter);
        assertEquals("Double value must be 1.5", 1.5, d, 0.0000001);
    }

    @Test
    public void testToBigDecimalReturnsExactDecimal() {
        Duration duration = Duration.ofSeconds(3, 250_000_000); // 3.25 seconds
        BigDecimal bd = DurationConversions.toBigDecimal(duration, converter);
        BigDecimal expected = BigDecimal.valueOf(3.25);
        // Use compareTo to avoid scale differences
        assertEquals("BigDecimal should represent 3.25 seconds", 0, expected.compareTo(bd));
    }

    @Test
    public void testToTimestampMatchesInstantEpochPlusDuration() {
        Duration duration = Duration.ofSeconds(1, 123_456_789);
        Timestamp ts = DurationConversions.toTimestamp(duration, converter);

        Instant expectedInstant = Instant.EPOCH.plus(duration);
        Timestamp expectedTimestamp = Timestamp.from(expectedInstant);

        assertEquals("Timestamp milliseconds must match expected Instant", expectedTimestamp.getTime(), ts.getTime());
        assertEquals("Timestamp nanos must match expected Instant", expectedTimestamp.getNanos(), ts.getNanos());
    }

    @Test
    public void testToTimestampWithZeroDuration() {
        Duration zero = Duration.ZERO;
        Timestamp ts = DurationConversions.toTimestamp(zero, converter);
        Timestamp expected = Timestamp.from(Instant.EPOCH);
        assertEquals("Zero duration should yield the epoch timestamp", expected.getTime(), ts.getTime());
    }
}
