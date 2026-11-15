package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

/**
 * Unit tests for {@link IntegerConversions}.
 *
 * The tests focus on the only public behaviour of this class:
 *
 * <ul>
 *   <li>Correct delegation to {@link LongConversions#toLocalTime(long, Converter)}.</li>
 *   <li>Proper handling of {@code null} and non‑{@code Integer} inputs.</li>
 *   <li>Behaviour with typical, boundary and negative millisecond values.</li>
 * </ul>
 *
 * The {@link Converter} used is the real implementation (no mocking) with default options.
 */
public class IntegerConversionsTest {

    /** Helper method to create a default {@link Converter}. */
    private static Converter defaultConverter() {
        // The library provides a no‑arg constructor for ConverterOptions.
        // Using the default options ensures all built‑in conversions are registered.
        return new Converter(new ConverterOptions());
    }

    /** A collection of representative integer millisecond values for positive testing. */
    private static final Collection<Integer> VALID_VALUES = Arrays.asList(
            0,                      // start of day
            1,                      // 1 ms after midnight
            999,                    // just before 1 second
            1_000,                  // exactly 1 second
            60_000,                 // exactly 1 minute
            3_600_000,              // exactly 1 hour
            12_345_678,             // arbitrary value
            Integer.MAX_VALUE      // largest positive int (2 147 483 647 ms ≈ 596 h)
    );

    @Test
    public void testDelegationMatchesLongConversions() {
        Converter converter = defaultConverter();

        for (Integer intMs : VALID_VALUES) {
            // Expected result obtained by calling the long‑based conversion directly.
            LocalTime expected = LongConversions.toLocalTime(intMs.longValue(), converter);
            // Actual result through the IntegerConversions wrapper.
            LocalTime actual   = IntegerConversions.toLocalTime(intMs, converter);

            assertEquals("Mismatch for ms = " + intMs, expected, actual);
        }
    }

    @Test
    public void testNegativeMilliseconds() {
        Converter converter = defaultConverter();

        // Negative values are legal for LocalTime conversion in the library – they are
        // normalized to the range 0‑23:59:59.999999999.  The test verifies that the
        // integer wrapper behaves identically to the long version.
        int[] negatives = { -1, -1_000, -60_000, -3_600_000, Integer.MIN_VALUE };
        for (int neg : negatives) {
            LocalTime expected = LongConversions.toLocalTime((long) neg, converter);
            LocalTime actual   = IntegerConversions.toLocalTime(neg, converter);
            assertEquals("Negative ms mismatch for " + neg, expected, actual);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testNullInputThrowsNPE() {
        Converter converter = defaultConverter();
        // The method casts the argument to Integer before any null‑check,
        // therefore a NullPointerException is expected.
        IntegerConversions.toLocalTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testNonIntegerInputThrowsClassCastException() {
        Converter converter = defaultConverter();
        // Passing a Long (or any non‑Integer) should cause a ClassCastException
        // because of the unchecked cast "(Integer) from".
        IntegerConversions.toLocalTime(Long.valueOf(1234L), converter);
    }

    @Test
    public void testConverterIsRequiredButNotUsedDirectly() {
        // The IntegerConversions method does not interact with the Converter
        // other than forwarding it to LongConversions.  This test ensures that
        // a Converter instance that lacks any custom conversions still works,
        // i.e., the built‑in conversion from long → LocalTime is available.
        Converter emptyConverter = new Converter(new ConverterOptions());

        for (Integer intMs : VALID_VALUES) {
            LocalTime expected = LongConversions.toLocalTime(intMs.longValue(), emptyConverter);
            LocalTime actual   = IntegerConversions.toLocalTime(intMs, emptyConverter);
            assertEquals("Converter delegation failed for ms = " + intMs, expected, actual);
        }
    }
}
