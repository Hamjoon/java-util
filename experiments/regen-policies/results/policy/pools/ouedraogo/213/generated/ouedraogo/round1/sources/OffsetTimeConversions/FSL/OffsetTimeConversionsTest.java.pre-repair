package com.cedarsoftware.util.convert;

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

import org.junit.Test;

/**
 * Unit tests for {@link OffsetTimeConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>String conversion using ISO_OFFSET_TIME format.</li>
 *   <li>Map conversion – the map must contain a single entry whose value equals {@code OffsetTime.toString()}.</li>
 *   <li>Numeric conversions (int, long, double, BigInteger, BigDecimal).</li>
 *   <li>Atomic conversions.</li>
 *   <li>Behaviour when a {@code null} source is supplied (expected NPE).</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore {@code null}
 * is passed in all test cases.
 */
public class OffsetTimeConversionsTest {

    private static final Converter DUMMY_CONVERTER = null; // not used by the methods under test

    /** Helper to create an {@link OffsetTime} with the given components. */
    private static OffsetTime ot(int hour, int minute, int second, int nano, String offset) {
        return OffsetTime.of(hour, minute, second, nano, ZoneOffset.of(offset));
    }

    @Test
    public void testToString() {
        OffsetTime time = ot(13, 45, 30, 123_456_789, "+02:00");
        String result = OffsetTimeConversions.toString(time, DUMMY_CONVERTER);
        assertEquals(time.format(DateTimeFormatter.ISO_OFFSET_TIME), result);
    }

    @Test
    public void testToMap() {
        OffsetTime time = ot(9, 15, 0, 0, "-05:00");
        Map<String, Object> map = OffsetTimeConversions.toMap(time, DUMMY_CONVERTER);
        assertNotNull(map);
        assertEquals(1, map.size());
        // The key is defined in MapConversions.OFFSET_TIME; we verify the value only.
        Object value = map.values().iterator().next();
        assertEquals(time.toString(), value);
    }

    @Test
    public void testToLongAndInteger() {
        OffsetTime time = ot(0, 0, 0, 0, "Z"); // midnight UTC
        long expectedMillis = time.atDate(LocalDate.of(1970, 1, 1))
                                   .toInstant()
                                   .toEpochMilli();
        assertEquals(expectedMillis, OffsetTimeConversions.toLong(time, DUMMY_CONVERTER));
        assertEquals((int) expectedMillis, OffsetTimeConversions.toInteger(time, DUMMY_CONVERTER));
    }

    @Test
    public void testToLongWithNanos() {
        // 01:02:03.004005006 +03:00
        OffsetTime time = ot(1, 2, 3, 4_005_006, "+03:00");
        long expected = time.atDate(LocalDate.of(1970, 1, 1))
                             .toInstant()
                             .toEpochMilli();
        assertEquals(expected, OffsetTimeConversions.toLong(time, DUMMY_CONVERTER));
    }

    @Test
    public void testToDouble() {
        OffsetTime time = ot(23, 59, 59, 999_000_000, "-04:00");
        Instant epoch = time.atDate(LocalDate.of(1970, 1, 1)).toInstant();
        double expected = epoch.getEpochSecond() + (epoch.getNano() / 1_000_000_000.0);
        assertEquals(expected, OffsetTimeConversions.toDouble(time, DUMMY_CONVERTER), 0.0);
    }

    @Test
    public void testToBigInteger() {
        OffsetTime time = ot(12, 30, 15, 250_000_000, "+01:00");
        Instant epoch = time.atDate(LocalDate.of(1970, 1, 1)).toInstant();

        BigInteger expected = BigInteger.valueOf(epoch.getEpochSecond())
                                         .multiply(BigInteger.valueOf(1_000_000_000L))
                                         .add(BigInteger.valueOf(epoch.getNano()));
        assertEquals(expected, OffsetTimeConversions.toBigInteger(time, DUMMY_CONVERTER));
    }

    @Test
    public void testToBigDecimal() {
        OffsetTime time = ot(6, 0, 0, 500_000_000, "Z");
        Instant epoch = time.atDate(LocalDate.of(1970, 1, 1)).toInstant();

        BigDecimal seconds = BigDecimal.valueOf(epoch.getEpochSecond());
        BigDecimal nanos = BigDecimal.valueOf(epoch.getNano())
                                     .divide(BigDecimal.valueOf(1_000_000_000L));
        BigDecimal expected = seconds.add(nanos);

        assertEquals(0, expected.compareTo(OffsetTimeConversions.toBigDecimal(time, DUMMY_CONVERTER)));
    }

    @Test
    public void testToAtomicInteger() {
        OffsetTime time = ot(0, 0, 0, 0, "Z");
        long millis = OffsetTimeConversions.toLong(time, DUMMY_CONVERTER);
        AtomicInteger atomic = OffsetTimeConversions.toAtomicInteger(time, DUMMY_CONVERTER);
        assertEquals((int) millis, atomic.get());
    }

    @Test
    public void testToAtomicLong() {
        OffsetTime time = ot(5, 30, 0, 0, "+02:00");
        long expected = OffsetTimeConversions.toLong(time, DUMMY_CONVERTER);
        AtomicLong atomic = OffsetTimeConversions.toAtomicLong(time, DUMMY_CONVERTER);
        assertEquals(expected, atomic.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        OffsetTimeConversions.toString(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        OffsetTimeConversions.toMap(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        OffsetTimeConversions.toLong(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput() {
        OffsetTimeConversions.toDouble(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        OffsetTimeConversions.toBigInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        OffsetTimeConversions.toBigDecimal(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullInput() {
        OffsetTimeConversions.toAtomicInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput() {
        OffsetTimeConversions.toAtomicLong(null, DUMMY_CONVERTER);
    }
}
