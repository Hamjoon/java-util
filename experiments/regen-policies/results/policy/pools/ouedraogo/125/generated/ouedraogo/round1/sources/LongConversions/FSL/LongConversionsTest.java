package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.time.LocalTime;

import org.junit.Test;

/**
 * Unit tests for {@link LongConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Successful conversion of valid millisecond values to {@link LocalTime}</li>
 *   <li>Boundary values (zero and the maximum allowed milliseconds for a day)</li>
 *   <li>Illegal arguments (negative values and values exceeding the maximum allowed)</li>
 * </ul>
 */
public class LongConversionsTest {

    /** A {@link Converter} instance is required by the method signature, but the implementation does not use it.
     *  We can safely pass {@code null} or a minimal instance.  Using {@code null} keeps the test simple and
     *  does not depend on the internal details of {@code Converter}. */
    private static final Converter DUMMY_CONVERTER = null;

    @Test
    public void testToLocalTime_ZeroMillis() {
        LocalTime result = LongConversions.toLocalTime(0L, DUMMY_CONVERTER);
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    @Test
    public void testToLocalTime_OneMillisecond() {
        // 1 ms = 1_000_000 nanoseconds
        LocalTime expected = LocalTime.ofNanoOfDay(1_000_000L);
        LocalTime result = LongConversions.toLocalTime(1L, DUMMY_CONVERTER);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalTime_SomeTypicalValue() {
        // 12:34:56.789 corresponds to
        // ((12 * 60 + 34) * 60 + 56) seconds = 45296 seconds
        // + 0.789 seconds = 45296.789 seconds
        // Convert to milliseconds: 45296_789 ms
        long millis = 45_296_789L;
        LocalTime expected = LocalTime.ofNanoOfDay(millis * 1_000_000L);
        LocalTime result = LongConversions.toLocalTime(millis, DUMMY_CONVERTER);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalTime_MaximumAllowedMillis() {
        // The maximum millisecond value that fits in a day is 86_399_999
        // (24 * 60 * 60 * 1000) - 1
        long maxMillis = 86_399_999L;
        LocalTime expected = LocalTime.ofNanoOfDay(maxMillis * 1_000_000L);
        LocalTime result = LongConversions.toLocalTime(maxMillis, DUMMY_CONVERTER);
        assertEquals(expected, result);
    }

    @Test
    public void testToLocalTime_NegativeMillis_ThrowsException() {
        long negative = -1L;
        try {
            LongConversions.toLocalTime(negative, DUMMY_CONVERTER);
            fail("Expected IllegalArgumentException for negative milliseconds");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Input value [" + negative + "]"));
        }
    }

    @Test
    public void testToLocalTime_TooLargeMillis_ThrowsException() {
        // One millisecond beyond the allowed maximum
        long tooLarge = 86_400_000L; // 24 * 60 * 60 * 1000
        try {
            LongConversions.toLocalTime(tooLarge, DUMMY_CONVERTER);
            fail("Expected IllegalArgumentException for milliseconds exceeding a day");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Input value [" + tooLarge + "]"));
        }
    }

    @Test
    public void testToLocalTime_NullInput_ThrowsClassCastException() {
        // The method casts the input to Long without a null check,
        // so a null argument results in a ClassCastException before the
        // IllegalArgumentException logic is reached.
        try {
            LongConversions.toLocalTime(null, DUMMY_CONVERTER);
            fail("Expected ClassCastException for null input");
        } catch (ClassCastException expected) {
            // expected path
        }
    }
}
