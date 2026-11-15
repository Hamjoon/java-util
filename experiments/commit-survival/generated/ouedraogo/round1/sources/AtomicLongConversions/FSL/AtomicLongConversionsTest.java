package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.time.LocalTime;

import org.junit.Test;

/**
 * Unit tests for {@link AtomicLongConversions}.
 */
public class AtomicLongConversionsTest {

    // Helper to obtain a default Converter instance.
    // The real library provides a no‑arg constructor for ConverterOptions,
    // which is sufficient for the conversion methods used in this test.
    private static Converter defaultConverter() {
        return new Converter(new ConverterOptions());
    }

    /* ------------------------------------------------------------
     * Tests for toAtomicLong(Object, Converter)
     * ------------------------------------------------------------ */

    @Test
    public void toAtomicLongCreatesNewInstanceWithSameValue() {
        AtomicLong original = new AtomicLong(12345L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(original, defaultConverter());

        assertNotNull("Result must not be null", result);
        assertNotSame("Result must be a different instance from the source", original, result);
        assertEquals("Result must contain the same long value as the source", original.get(), result.get());
    }

    @Test
    public void toAtomicLongHandlesZeroAndNegativeValues() {
        AtomicLong zero = new AtomicLong(0L);
        AtomicLong negative = new AtomicLong(-9876543210L);

        AtomicLong zeroResult = AtomicLongConversions.toAtomicLong(zero, defaultConverter());
        AtomicLong negativeResult = AtomicLongConversions.toAtomicLong(negative, defaultConverter());

        assertEquals(0L, zeroResult.get());
        assertEquals(-9876543210L, negativeResult.get());
    }

    @Test(expected = NullPointerException.class)
    public void toAtomicLongThrowsNPEWhenSourceIsNull() {
        AtomicLongConversions.toAtomicLong(null, defaultConverter());
    }

    @Test
    public void toAtomicLongIsIndependentFromOriginalAfterConversion() {
        AtomicLong original = new AtomicLong(42L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(original, defaultConverter());

        // Change the original after conversion
        original.set(99L);

        // The result must retain the original value captured at conversion time
        assertEquals(42L, result.get());
        // The original now holds the new value
        assertEquals(99L, original.get());
    }

    /* ------------------------------------------------------------
     * Tests for toLocalTime(Object, Converter)
     * ------------------------------------------------------------ */

    @Test
    public void toLocalTimeDelegatesToLongConversions() {
        // Choose a value that corresponds to a known LocalTime.
        // LongConversions.toLocalTime interprets the long as nanoseconds
        // since midnight (the implementation used by the library).
        long nanosSinceMidnight = 12_345_678_901L; // 12 seconds, 345 ms, 678 µs, 901 ns
        AtomicLong source = new AtomicLong(nanosSinceMidnight);

        // Expected value obtained by calling the delegate directly
        LocalTime expected = LongConversions.toLocalTime(nanosSinceMidnight, defaultConverter());

        LocalTime actual = AtomicLongConversions.toLocalTime(source, defaultConverter());

        assertEquals("AtomicLongConversions should produce the same LocalTime as LongConversions", expected, actual);
    }

    @Test
    public void toLocalTimeHandlesZero() {
        AtomicLong source = new AtomicLong(0L);
        LocalTime expected = LongConversions.toLocalTime(0L, defaultConverter());
        LocalTime actual = AtomicLongConversions.toLocalTime(source, defaultConverter());

        assertEquals(expected, actual);
    }

    @Test
    public void toLocalTimeHandlesMaximumNanoOfDay() {
        // LocalTime.MAX is 23:59:59.999999999 which is 86_399_999_999_999 nanoseconds
        long maxNanos = LocalTime.MAX.toNanoOfDay();
        AtomicLong source = new AtomicLong(maxNanos);

        LocalTime expected = LongConversions.toLocalTime(maxNanos, defaultConverter());
        LocalTime actual = AtomicLongConversions.toLocalTime(source, defaultConverter());

        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void toLocalTimeThrowsNPEWhenSourceIsNull() {
        AtomicLongConversions.toLocalTime(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toLocalTimeThrowsClassCastExceptionWhenSourceIsNotAtomicLong() {
        // Pass an object of a different type; the method casts to AtomicLong and will fail.
        AtomicLongConversions.toLocalTime("not an atomic long", defaultConverter());
    }
}
