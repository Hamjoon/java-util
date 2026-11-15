package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

/**
 * Unit tests for {@link AtomicIntegerConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct conversion of an {@link AtomicInteger} to a new {@link AtomicInteger}
 *       with the same numeric value but a different object reference.</li>
 *   <li>Edge‑case numeric values (zero, negative, {@link Integer#MAX_VALUE}).</li>
 *   <li>Behaviour when the source object is {@code null} or of an unexpected type
 *       (expected {@link ClassCastException} or {@link NullPointerException}).</li>
 *   <li>Conversion of an {@link AtomicInteger} to {@link LocalTime} by delegating to
 *       {@link LongConversions#toLocalTime(long, Converter)} – the test verifies that
 *       the delegation is correct for a variety of values.</li>
 * </ul>
 *
 * The {@link Converter} argument is not used by the methods under test, therefore
 * {@code null} is passed safely.
 */
public class AtomicIntegerConversionsTest {

    // -----------------------------------------------------------------------
    // toAtomicInteger(...)
    // -----------------------------------------------------------------------

    @Test
    public void toAtomicIntegerCreatesNewInstanceWithSameValue() {
        AtomicInteger original = new AtomicInteger(42);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, null);

        // Different object reference
        assertNotSame("Result should be a new instance", original, result);
        // Same numeric value
        assertEquals("Numeric value must be preserved", original.intValue(), result.intValue());
    }

    @Test
    public void toAtomicIntegerHandlesZeroAndNegativeValues() {
        AtomicInteger zero = new AtomicInteger(0);
        AtomicInteger negative = new AtomicInteger(-12345);
        AtomicInteger max = new AtomicInteger(Integer.MAX_VALUE);

        assertEquals(0, AtomicIntegerConversions.toAtomicInteger(zero, null).intValue());
        assertEquals(-12345, AtomicIntegerConversions.toAtomicInteger(negative, null).intValue());
        assertEquals(Integer.MAX_VALUE, AtomicIntegerConversions.toAtomicInteger(max, null).intValue());
    }

    @Test(expected = NullPointerException.class)
    public void toAtomicIntegerThrowsNPEWhenSourceIsNull() {
        AtomicIntegerConversions.toAtomicInteger(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void toAtomicIntegerThrowsCCEWhenSourceIsNotAtomicInteger() {
        // Passing a plain Integer should cause a ClassCastException because the method
        // casts the argument to AtomicInteger without checking.
        AtomicIntegerConversions.toAtomicInteger(Integer.valueOf(10), null);
    }

    @Test
    public void toAtomicIntegerResultIsImmutableFromOriginalChanges() {
        AtomicInteger original = new AtomicInteger(5);
        AtomicInteger converted = AtomicIntegerConversions.toAtomicInteger(original, null);

        // Change the original after conversion
        original.set(99);

        // The converted instance must retain the value captured at conversion time
        assertEquals(5, converted.intValue());
        // The original now holds the new value
        assertEquals(99, original.intValue());
    }

    // -----------------------------------------------------------------------
    // toLocalTime(...)
    // -----------------------------------------------------------------------

    @Test
    public void toLocalTimeDelegatesToLongConversions() {
        // Test a set of representative values.  The expected result is obtained
        // by calling the delegate directly; this guarantees that the test stays
        // in sync with any future implementation changes in LongConversions.
        int[] testValues = {0, 1, 59, 60, 3600, 86399, 123456, -1};

        for (int v : testValues) {
            AtomicInteger src = new AtomicInteger(v);
            LocalTime expected = LongConversions.toLocalTime((long) src.get(), null);
            LocalTime actual   = AtomicIntegerConversions.toLocalTime(src, null);
            assertEquals("LocalTime conversion mismatch for value: " + v, expected, actual);
        }
    }

    @Test(expected = NullPointerException.class)
    public void toLocalTimeThrowsNPEWhenSourceIsNull() {
        AtomicIntegerConversions.toLocalTime(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void toLocalTimeThrowsCCEWhenSourceIsNotAtomicInteger() {
        // Passing a non‑AtomicInteger should result in a ClassCastException
        AtomicIntegerConversions.toLocalTime("not an atomic integer", null);
    }

    @Test
    public void toLocalTimeResultIsIndependentOfSourceMutation() {
        AtomicInteger src = new AtomicInteger(5000);
        LocalTime first = AtomicIntegerConversions.toLocalTime(src, null);

        // Mutate the source after conversion
        src.set(12345);
        LocalTime second = AtomicIntegerConversions.toLocalTime(src, null);

        // The first result must stay unchanged
        assertNotEquals("Subsequent conversion should reflect mutated source",
                first, second);
    }
}
