package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JUnit‑4 test suite for {@link OffsetTimeConversions}.
 *
 * All static conversion methods are exercised with a variety of
 * {@link OffsetTime} instances, including different offsets, nanosecond
 * values and edge‑case times (midnight, end‑of‑day).  The {@code Converter}
 * argument is not used by the implementation, therefore {@code null}
 * is passed safely.
 */
public class OffsetTimeConversionsTest {

    /** Helper to create an {@link OffsetTime} with the supplied components. */
    private static OffsetTime ot(int hour, int minute, int second,
                                 int nano, ZoneOffset offset) {
        return OffsetTime.of(hour, minute, second, nano, offset);
    }

    /** Central reference date used by the conversion methods (1970‑01‑01). */
    private static final LocalDate EPOCH_DATE = LocalDate.of(1970, 1, 1);

    @Test
    public void testToString() {
        OffsetTime time = ot(13, 45, 30, 123_456_789, ZoneOffset.ofHours(2));
        String expected = time.format(DateTimeFormatter.ISO_OFFSET_TIME);
        String actual = OffsetTimeConversions.toString(time, null);
        assertEquals("ISO formatted string should match", expected, actual);
    }

    @Test
    public void testToMap() {
        OffsetTime time = ot(23, 59, 59, 0, ZoneOffset.UTC);
        Map<String, Object> map = OffsetTimeConversions.toMap(time, null);
        assertNotNull("Map should not be null", map);
        assertEquals("Map should contain exactly one entry", 1, map.size());
        // The value must be the standard {@code OffsetTime#toString()} representation.
        assertTrue("Map value must be the OffsetTime string",
                map.containsValue(time.toString()));
    }

    @Test
    public void testToLong_UtcMidnight() {
        OffsetTime time = ot(0, 0, 0, 0, ZoneOffset.UTC);
        long expected = 0L; // epoch start
        long actual = OffsetTimeConversions.toLong(time, null);
        assertEquals("Midnight UTC on epoch date should be 0 ms", expected, actual);
    }

    @Test
    public void testToLong_WithPositiveOffset() {
        // 00:00 at +02:00 is 2 hours *ahead* of UTC → instant is previous day 22:00 UTC
        OffsetTime time = ot(0, 0, 0, 0, ZoneOffset.ofHours(2));
        long expected = -2L * 60 * 60 * 1000; // -7200000 ms
        long actual = OffsetTimeConversions.toLong(time, null);
        assertEquals("Positive offset should produce negative epoch milliseconds", expected, actual);
    }

    @Test
    public void testToLong_WithNegativeOffsetAndNanos() {
        // 12:34:56.789012345 at -05:30
        OffsetTime time = ot(12, 34, 56, 789_012_345, ZoneOffset.ofHoursMinutes(-5, -30));
        Instant expectedInstant = time.atDate(EPOCH_DATE).toInstant();
        long expected = expectedInstant.toEpochMilli();
        long actual = OffsetTimeConversions.toLong(time, null);
        assertEquals("Computed long epoch millis must match Instant conversion", expected, actual);
    }

    @Test
    public void testToInteger() {
        OffsetTime time = ot(1, 0, 0, 0, ZoneOffset.UTC);
        int expected = (int) OffsetTimeConversions.toLong(time, null);
        int actual = OffsetTimeConversions.toInteger(time, null);
        assertEquals("Integer conversion should be a cast of long conversion", expected, actual);
    }

    @Test
    public void testToDouble() {
        OffsetTime time = ot(23, 59, 59, 500_000_000, ZoneOffset.ofHours(-3));
        Instant instant = time.atDate(EPOCH_DATE).toInstant();
        double expected = instant.getEpochSecond() + instant.getNano() / 1_000_000_000.0;
        double actual = OffsetTimeConversions.toDouble(time, null);
        assertEquals("Double conversion should combine seconds and nanos", expected, actual, 1e-12);
    }

    @Test
    public void testToBigInteger() {
        OffsetTime time = ot(6, 30, 15, 250_000_000, ZoneOffset.ofHours(5));
        Instant instant = time.atDate(EPOCH_DATE).toInstant();

        BigInteger expected = BigInteger.valueOf(instant.getEpochSecond())
                .multiply(BigIntegerConversions.BILLION)
                .add(BigInteger.valueOf(instant.getNano()));
        BigInteger actual = OffsetTimeConversions.toBigInteger(time, null);
        assertEquals("BigInteger conversion must match manual calculation", expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        OffsetTime time = ot(18, 0, 0, 125_000_000, ZoneOffset.ofHoursMinutes(-4, -45));
        Instant instant = time.atDate(EPOCH_DATE).toInstant();

        BigDecimal seconds = BigDecimal.valueOf(instant.getEpochSecond());
        BigDecimal nanos = BigDecimal.valueOf(instant.getNano())
                .divide(BigDecimalConversions.BILLION);
        BigDecimal expected = seconds.add(nanos);
        BigDecimal actual = OffsetTimeConversions.toBigDecimal(time, null);
        assertEquals("BigDecimal conversion must be precise", 0, expected.compareTo(actual));
    }

    @Test
    public void testToAtomicInteger() {
        OffsetTime time = ot(2, 0, 0, 0, ZoneOffset.UTC);
        AtomicInteger atomic = OffsetTimeConversions.toAtomicInteger(time, null);
        int expected = (int) OffsetTimeConversions.toLong(time, null);
        assertEquals("AtomicInteger value must equal long cast to int", expected, atomic.get());
    }

    @Test
    public void testToAtomicLong() {
        OffsetTime time = ot(12, 0, 0, 0, ZoneOffset.ofHours(1));
        AtomicLong atomic = OffsetTimeConversions.toAtomicLong(time, null);
        long expected = OffsetTimeConversions.toLong(time, null);
        assertEquals("AtomicLong value must equal long conversion", expected, atomic.get());
    }
}
