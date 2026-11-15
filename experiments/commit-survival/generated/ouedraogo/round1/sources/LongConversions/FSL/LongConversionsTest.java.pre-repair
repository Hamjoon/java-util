package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.time.LocalTime;

/**
 * Unit tests for {@link LongConversions}.
 *
 * The class under test is package‑private, so the test class resides in the same
 * package to gain access.  The {@code Converter} argument of {@code toLocalTime}
 * is not used by the implementation; therefore a {@code null} reference is
 * supplied in all tests.
 */
public class LongConversionsTest {

    /** Helper that invokes the method under test. */
    private static LocalTime convert(Object value) {
        return LongConversions.toLocalTime(value, null);
    }

    /** Verify conversion of the minimum valid millisecond value (midnight). */
    @Test
    public void testZeroMillis() {
        LocalTime result = convert(0L);
        assertNotNull("Result must not be null", result);
        assertEquals("Midnight hour", 0, result.getHour());
        assertEquals("Midnight minute", 0, result.getMinute());
        assertEquals("Midnight second", 0, result.getSecond());
        assertEquals("Midnight nano of day", 0L, result.toNanoOfDay());
    }

    /** Verify conversion of a single millisecond. */
    @Test
    public void testOneMillisecond() {
        long millis = 1L;
        LocalTime result = convert(millis);
        assertEquals("Nanoseconds of day must match", millis * 1_000_000L, result.toNanoOfDay());
        assertEquals("Hour must be zero", 0, result.getHour());
        assertEquals("Minute must be zero", 0, result.getMinute());
        assertEquals("Second must be zero", 0, result.getSecond());
        assertEquals("Nano part must be 1_000_000", 1_000_000, result.getNano());
    }

    /** Verify conversion of a typical time (13:45:30.123). */
    @Test
    public void testTypicalTime() {
        // 13 hours, 45 minutes, 30 seconds, 123 milliseconds
        long millis = ((13L * 60 + 45) * 60 + 30) * 1000 + 123;
        LocalTime result = convert(millis);
        assertEquals(13, result.getHour());
        assertEquals(45, result.getMinute());
        assertEquals(30, result.getSecond());
        assertEquals(123_000_000, result.getNano()); // 123 ms -> 123,000,000 ns
        assertEquals(millis * 1_000_000L, result.toNanoOfDay());
    }

    /** Verify conversion of the maximum valid millisecond value (23:59:59.999). */
    @Test
    public void testMaxValidMillis() {
        long maxMillis = 86_399_999L; // 24*60*60*1000 - 1
        LocalTime result = convert(maxMillis);
        assertEquals(23, result.getHour());
        assertEquals(59, result.getMinute());
        assertEquals(59, result.getSecond());
        assertEquals(999_000_000, result.getNano()); // 999 ms
        assertEquals(maxMillis * 1_000_000L, result.toNanoOfDay());
    }

    /** Negative millisecond values must result in IllegalArgumentException. */
    @Test
    public void testNegativeMillis() {
        long negative = -1L;
        try {
            convert(negative);
            fail("Expected IllegalArgumentException for negative input");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should contain the invalid value",
                    e.getMessage().contains("[" + negative + "]"));
        }
    }

    /** Millisecond values greater than the number of milliseconds in a day must fail. */
    @Test
    public void testTooLargeMillis() {
        long tooLarge = 86_400_000L; // exactly 24 hours
        try {
            convert(tooLarge);
            fail("Expected IllegalArgumentException for out‑of‑range input");
        } catch (IllegalArgumentException e) {
            assertTrue("Exception message should mention the range",
                    e.getMessage().contains(">= 0 && <= 86399999"));
        }
    }

    /** Passing a non‑Long object should result in a ClassCastException (not wrapped). */
    @Test
    public void testNonLongInput() {
        try {
            convert(Integer.valueOf(1000));
            fail("Expected ClassCastException when input is not a Long");
        } catch (ClassCastException e) {
            // Expected path – the implementation casts directly to Long.
        }
    }

    /** Passing null should result in a NullPointerException during unboxing. */
    @Test
    public void testNullInput() {
        try {
            convert(null);
            fail("Expected NullPointerException for null input");
        } catch (NullPointerException e) {
            // Expected – unboxing a null Long throws NPE.
        }
    }
}
