package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class OffsetTimeConversionsTest {

    // Helper method to create a Converter instance.
    // The Converter constructor requires a ConverterOptions object, but the
    // conversion methods in OffsetTimeConversions never use the Converter,
    // so we can safely pass null.
    private static final Converter DUMMY_CONVERTER = null;

    // -----------------------------------------------------------------
    // toString tests
    // -----------------------------------------------------------------
    @Test
    public void testToString_WithUtcOffset() {
        OffsetTime ot = OffsetTime.of(13, 45, 30, 0, ZoneOffset.UTC);
        String result = OffsetTimeConversions.toString(ot, DUMMY_CONVERTER);
        assertEquals("13:45:30Z", result);
    }

    @Test
    public void testToString_WithPositiveOffsetAndNanos() {
        OffsetTime ot = OffsetTime.of(23, 59, 59, 123_456_789, ZoneOffset.ofHoursMinutes(5, 30));
        String result = OffsetTimeConversions.toString(ot, DUMMY_CONVERTER);
        // The standard ISO_OFFSET_TIME format includes the offset without a 'Z'.
        assertEquals("23:59:59.123456789+05:30", result);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullInput() {
        OffsetTimeConversions.toString(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // toMap tests
    // -----------------------------------------------------------------
    @Test
    public void testToMap_ContainsSingleEntryWithCorrectValue() {
        OffsetTime ot = OffsetTime.of(08, 15, 0, 0, ZoneOffset.ofHours(-4));
        Map<String, Object> map = OffsetTimeConversions.toMap(ot, DUMMY_CONVERTER);
        assertNotNull(map);
        assertEquals(1, map.size());

        // The key is defined in MapConversions.OFFSET_TIME; we don't have the
        // constant here, so we just fetch the sole entry.
        String key = map.keySet().iterator().next();
        Object value = map.get(key);
        assertEquals(ot.toString(), value);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullInput() {
        OffsetTimeConversions.toMap(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // toInteger tests
    // -----------------------------------------------------------------
    @Test
    public void testToInteger_WithinIntRange() {
        OffsetTime ot = OffsetTime.of(00, 00, 01, 0, ZoneOffset.UTC); // 1 second after epoch
        int result = OffsetTimeConversions.toInteger(ot, DUMMY_CONVERTER);
        assertEquals(1000, result); // 1 second = 1000 ms
    }

    @Test(expected = NullPointerException.class)
    public void testToInteger_NullInput() {
        OffsetTimeConversions.toInteger(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // toLong tests
    // -----------------------------------------------------------------
    @Test
    public void testToLong_UtcMidnight() {
        OffsetTime ot = OffsetTime.of(0, 0, 0, 0, ZoneOffset.UTC);
        long result = OffsetTimeConversions.toLong(ot, DUMMY_CONVERTER);
        assertEquals(0L, result);
    }

    @Test
    public void testToLong_WithPositiveOffset() {
        // 02:30 with +02:00 offset => instant = 1970-01-01T00:30Z => 30 minutes = 1,800,000 ms
        OffsetTime ot = OffsetTime.of(2, 30, 0, 0, ZoneOffset.ofHours(2));
        long result = OffsetTimeConversions.toLong(ot, DUMMY_CONVERTER);
        assertEquals(1_800_000L, result);
    }

    @Test
    public void testToLong_WithNegativeOffsetAndNanos() {
        // 00:00 with -03:00 offset => instant = 1970-01-01T03:00Z => 3 hours = 10,800,000 ms
        // plus 500,000,000 nanos = 0.5 seconds => 500 ms
        OffsetTime ot = OffsetTime.of(0, 0, 0, 500_000_000, ZoneOffset.ofHours(-3));
        long result = OffsetTimeConversions.toLong(ot, DUMMY_CONVERTER);
        assertEquals(10_800_500L, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        OffsetTimeConversions.toLong(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // toDouble tests
    // -----------------------------------------------------------------
    @Test
    public void testToDouble_UtcExactSecond() {
        OffsetTime ot = OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC);
        double result = OffsetTimeConversions.toDouble(ot, DUMMY_CONVERTER);
        assertEquals(43_200.0, result, 0.0); // 12 hours = 43,200 seconds
    }

    @Test
    public void testToDouble_WithNanosFraction() {
        // 01:00:00.250 with +01:00 offset => instant = 1970-01-01T00:00:00.250Z
        OffsetTime ot = OffsetTime.of(1, 0, 0, 250_000_000, ZoneOffset.ofHours(1));
        double result = OffsetTimeConversions.toDouble(ot, DUMMY_CONVERTER);
        assertEquals(0.25, result, 1e-9);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput() {
        OffsetTimeConversions.toDouble(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // toBigInteger tests
    // -----------------------------------------------------------------
    @Test
    public void testToBigInteger_UtcWithNanos() {
        OffsetTime ot = OffsetTime.of(0, 0, 0, 123_456_789, ZoneOffset.UTC);
        BigInteger result = OffsetTimeConversions.toBigInteger(ot, DUMMY_CONVERTER);
        // epoch seconds = 0, nanos = 123,456,789
        BigInteger expected = BigInteger.valueOf(0)
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(123_456_789L));
        assertEquals(expected, result);
    }

    @Test
    public void testToBigInteger_WithNegativeOffset() {
        // 00:00 with -02:00 offset => instant = 1970-01-01T02:00Z => 7,200 seconds
        OffsetTime ot = OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(-2));
        BigInteger result = OffsetTimeConversions.toBigInteger(ot, DUMMY_CONVERTER);
        BigInteger expected = BigInteger.valueOf(7_200L)
                .multiply(BigInteger.valueOf(1_000_000_000L));
        assertEquals(expected, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        OffsetTimeConversions.toBigInteger(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // toBigDecimal tests
    // -----------------------------------------------------------------
    @Test
    public void testToBigDecimal_UtcWithNanos() {
        OffsetTime ot = OffsetTime.of(0, 0, 0, 500_000_000, ZoneOffset.UTC);
        BigDecimal result = OffsetTimeConversions.toBigDecimal(ot, DUMMY_CONVERTER);
        BigDecimal expected = BigDecimal.valueOf(0).add(BigDecimal.valueOf(0.5));
        assertEquals(0, result.compareTo(expected));
    }

    @Test
    public void testToBigDecimal_WithPositiveOffset() {
        // 03:00 with +01:00 => instant = 1970-01-01T02:00Z => 7,200 seconds
        OffsetTime ot = OffsetTime.of(3, 0, 0, 0, ZoneOffset.ofHours(1));
        BigDecimal result = OffsetTimeConversions.toBigDecimal(ot, DUMMY_CONVERTER);
        BigDecimal expected = BigDecimal.valueOf(7_200);
        assertEquals(0, result.compareTo(expected));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        OffsetTimeConversions.toBigDecimal(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // toAtomicInteger tests
    // -----------------------------------------------------------------
    @Test
    public void testToAtomicInteger_Utc() {
        OffsetTime ot = OffsetTime.of(0, 0, 2, 0, ZoneOffset.UTC); // 2 seconds = 2000 ms
        AtomicInteger ai = OffsetTimeConversions.toAtomicInteger(ot, DUMMY_CONVERTER);
        assertEquals(2000, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullInput() {
        OffsetTimeConversions.toAtomicInteger(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // toAtomicLong tests
    // -----------------------------------------------------------------
    @Test
    public void testToAtomicLong_WithNegativeOffset() {
        // 00:00 with -01:00 offset => instant = 1970-01-01T01:00Z => 3,600,000 ms
        OffsetTime ot = OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(-1));
        AtomicLong al = OffsetTimeConversions.toAtomicLong(ot, DUMMY_CONVERTER);
        assertEquals(3_600_000L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput() {
        OffsetTimeConversions.toAtomicLong(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // Additional edge case: extreme offsets and nanosecond precision
    // -----------------------------------------------------------------
    @Test
    public void testExtremeOffsetAndNanoseconds() {
        // Offset of +18:00 (maximum allowed) and -18:00 (minimum allowed)
        OffsetTime maxOffset = OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHours(18));
        OffsetTime minOffset = OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(-18));

        // Verify that conversion does not overflow and returns expected values
        long maxLong = OffsetTimeConversions.toLong(maxOffset, DUMMY_CONVERTER);
        long minLong = OffsetTimeConversions.toLong(minOffset, DUMMY_CONVERTER);

        // For maxOffset: instant = 1970-01-01T05:59:59.999999999Z (because 23:59:59+18 => UTC = 05:59:59)
        assertEquals(5 * 3600_000L + 59 * 60_000L + 59_000L + 0L, maxLong / 1_000_000L * 1_000L); // sanity check on seconds part
        // For minOffset: instant = 1970-01-01T18:00:00Z
        assertEquals(18 * 3600_000L, minLong);
    }
}
