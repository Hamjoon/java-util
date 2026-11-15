package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.LocalTime;

/**
 * Unit tests for {@link IntegerConversions}.
 *
 * These tests cover the {@code toLocalTime} method, which converts an {@link Integer}
 * representing milliseconds since midnight into a {@link LocalTime} instance.
 *
 * The method delegates to {@link LongConversions#toLocalTime(long, Converter)} so the
 * tests also verify that the delegation works correctly for typical, boundary and
 * exceptional inputs.
 */
public class IntegerConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Construct a Converter with default options – the actual options are not relevant
        // for the behaviour of IntegerConversions.toLocalTime.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Verify that a typical millisecond value is correctly converted to a LocalTime.
     */
    @Test
    public void testTypicalMilliseconds() {
        Integer ms = 12_345; // 12.345 seconds after midnight
        LocalTime expected = LocalTime.ofSecondOfDay(12).plusNanos(345_000_000);
        LocalTime actual = IntegerConversions.toLocalTime(ms, converter);
        assertEquals("LocalTime should match the expected value for typical milliseconds", expected, actual);
    }

    /**
     * Verify that zero milliseconds yields midnight (00:00).
     */
    @Test
    public void testZeroMilliseconds() {
        Integer ms = 0;
        LocalTime expected = LocalTime.MIDNIGHT;
        LocalTime actual = IntegerConversions.toLocalTime(ms, converter);
        assertEquals("Zero milliseconds should convert to MIDNIGHT", expected, actual);
    }

    /**
     * Verify conversion of the maximum positive int value (2^31‑1 ms).
     * This value is within a single day (86_400_000 ms per day), so the conversion
     * should succeed without overflow.
     */
    @Test
    public void testMaxIntMilliseconds() {
        Integer ms = Integer.MAX_VALUE; // 2_147_483_647 ms ≈ 596 hours – exceeds a day
        // LongConversions.toLocalTime should wrap or truncate to a valid time.
        // The expected behaviour of LongConversions is to treat the value as an instant
        // since midnight, modulo 24 hours. Compute expected by using LocalTime.ofNanoOfDay.
        long msModDay = ms % (24L * 60 * 60 * 1000);
        LocalTime expected = LocalTime.ofNanoOfDay(msModDay * 1_000_000L);
        LocalTime actual = IntegerConversions.toLocalTime(ms, converter);
        assertEquals("Maximum int milliseconds should be reduced modulo 24h", expected, actual);
    }

    /**
     * Verify that a negative millisecond value is handled correctly.
     * The underlying LongConversions implementation treats negative values as
     * the time before midnight of the previous day, which LocalTime normalises.
     */
    @Test
    public void testNegativeMilliseconds() {
        Integer ms = -1; // one millisecond before midnight
        // Normalise to the previous day’s last millisecond: 23:59:59.999
        LocalTime expected = LocalTime.of(23, 59, 59, 999_000_000);
        LocalTime actual = IntegerConversions.toLocalTime(ms, converter);
        assertEquals("Negative milliseconds should wrap to the previous day's last millisecond", expected, actual);
    }

    /**
     * Ensure that passing a non‑Integer object results in a {@link ClassCastException}.
     */
    @Test(expected = ClassCastException.class)
    public void testNonIntegerInput() {
        // The method expects an Integer; passing a Long should trigger a cast failure.
        Long notAnInteger = 12345L;
        IntegerConversions.toLocalTime(notAnInteger, converter);
    }

    /**
     * Ensure that a null source object results in a {@link NullPointerException}
     * when the cast to Integer is attempted.
     */
    @Test(expected = NullPointerException.class)
    public void testNullInput() {
        IntegerConversions.toLocalTime(null, converter);
    }
}
