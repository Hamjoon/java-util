package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalTime;

/**
 * JUnit‑4 test suite for {@code LongConversions}.
 *
 * <p>The class under test is package‑private, therefore the test class resides in the same
 * package to gain access to the {@code toLocalTime} method.</p>
 *
 * <p>The tests cover:</p>
 * <ul>
 *   <li>Typical successful conversions (zero, a normal value, the maximum allowed value).</li>
 *   <li>Boundary conditions that must trigger {@code IllegalArgumentException} (negative
 *       values and values larger than the number of milliseconds in a day).</li>
 *   <li>Incorrect input type leading to {@code ClassCastException}.</li>
 * </ul>
 */
public class LongConversionsTest {

    /** Zero milliseconds must map to {@code 00:00:00}. */
    @Test
    public void testZeroMillis() {
        Long millis = 0L;
        LocalTime result = LongConversions.toLocalTime(millis, null);
        assertEquals(LocalTime.MIDNIGHT, result);
    }

    /** A typical conversion (e.g., 12 345 ms → 00:00:12.345). */
    @Test
    public void testTypicalConversion() {
        Long millis = 12_345L;
        LocalTime expected = LocalTime.ofNanoOfDay(millis * 1_000_000L);
        LocalTime actual = LongConversions.toLocalTime(millis, null);
        assertEquals(expected, actual);
    }

    /** The maximum allowed millisecond value (23:59:59.999). */
    @Test
    public void testMaxMillis() {
        Long maxMillis = 86_399_999L; // 24h - 1 ms
        LocalTime expected = LocalTime.ofNanoOfDay(maxMillis * 1_000_000L);
        LocalTime actual = LongConversions.toLocalTime(maxMillis, null);
        assertEquals(expected, actual);
    }

    /** Negative millisecond values must cause an IllegalArgumentException. */
    @Test(expected = IllegalArgumentException.class)
    public void testNegativeMillisThrows() {
        Long negative = -1L;
        LongConversions.toLocalTime(negative, null);
    }

    /** Values greater than the maximum millisecond count must cause an IllegalArgumentException. */
    @Test(expected = IllegalArgumentException.class)
    public void testTooLargeMillisThrows() {
        Long tooLarge = 86_400_000L; // exactly 24 h, which is out of range
        LongConversions.toLocalTime(tooLarge, null);
    }

    /** Supplying a non‑Long object should result in a ClassCastException before validation. */
    @Test(expected = ClassCastException.class)
    public void testInvalidTypeThrows() {
        Integer notLong = 123;
        LongConversions.toLocalTime(notLong, null);
    }
}
