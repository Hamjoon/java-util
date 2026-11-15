package com.cedarsoftware.util.convert;

import java.time.DateTimeException;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@code AtomicLongConversions}.
 *
 * <p>The class under test contains two package‑private static conversion methods:
 * <ul>
 *   <li>{@code toAtomicLong(Object, Converter)}</li>
 *   <li>{@code toLocalTime(Object, Converter)}</li>
 * </ul>
 * The tests are placed in the same package so that the default visibility methods are
 * accessible without reflection.</p>
 *
 * <p>All tests are written without any mocking – only real instances of {@link AtomicLong},
 * {@link LocalTime} and {@link Converter} (created with a default {@link ConverterOptions})
 * are used.  The {@code Converter} argument is not used by the methods themselves, but it
 * must be supplied to match the method signature.</p>
 */
public class AtomicLongConversionsTest {

    /* ----------------------------------------------------------------------
     * Helper utilities
     * ---------------------------------------------------------------------- */

    /** Simple {@code Converter} instance – the concrete implementation does not
     * affect the behaviour of the methods under test because the methods either
     * ignore the argument or forward it to {@code LongConversions}, which is
     * exercised indirectly by comparing results. */
    private static Converter defaultConverter() {
        // The library defines a public constructor taking ConverterOptions.
        // An empty options object is sufficient for our tests.
        return new Converter(new ConverterOptions());
    }

    /** Creates an {@code AtomicLong} with the supplied value. */
    private static AtomicLong atomic(long value) {
        return new AtomicLong(value);
    }

    /** Delegates to the library's {@code LongConversions.toLocalTime} so that
     * we can compare the result of {@code AtomicLongConversions.toLocalTime}
     * against the expected behaviour. */
    private static LocalTime expectedLocalTime(long epochMillis) {
        return LongConversions.toLocalTime(epochMillis, defaultConverter());
    }

    /* ----------------------------------------------------------------------
     * Tests for toAtomicLong(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicLong_BasicCopy() {
        AtomicLong original = atomic(12345L);
        AtomicLong result = AtomicLongConversions.toAtomicLong(original, defaultConverter());

        assertNotNull("Result must not be null", result);
        assertEquals("Value must be copied from the original", original.get(), result.get());
        assertNotSame("Result must be a new instance, not the original", original, result);
    }

    @Test
    public void testToAtomicLong_NullArgument() {
        try {
            AtomicLongConversions.toAtomicLong(null, defaultConverter());
            fail("Expected NullPointerException when 'from' is null");
        } catch (NullPointerException expected) {
            // Expected path
        }
    }

    @Test
    public void testToAtomicLong_WrongType() {
        try {
            // Pass a String instead of AtomicLong – should cause ClassCastException
            AtomicLongConversions.toAtomicLong("not an atomic long", defaultConverter());
            fail("Expected ClassCastException for incompatible source type");
        } catch (ClassCastException expected) {
            // Expected path
        }
    }

    @Test
    public void testToAtomicLong_ZeroAndNegativeValues() {
        AtomicLong zero = atomic(0L);
        AtomicLong negative = atomic(-9876L);

        AtomicLong zeroResult = AtomicLongConversions.toAtomicLong(zero, defaultConverter());
        AtomicLong negativeResult = AtomicLongConversions.toAtomicLong(negative, defaultConverter());

        assertEquals(0L, zeroResult.get());
        assertEquals(-9876L, negativeResult.get());
    }

    /* ----------------------------------------------------------------------
     * Tests for toLocalTime(Object, Converter)
     * ---------------------------------------------------------------------- */

    @Test
    public void testToLocalTime_BasicConversion() {
        // 01:23:45.678 corresponds to 5,023,678 milliseconds after midnight
        long millis = (1 * 60 * 60 * 1000)      // 1 hour
                    + (23 * 60 * 1000)         // 23 minutes
                    + (45 * 1000)              // 45 seconds
                    + 678;                     // 678 milliseconds

        AtomicLong source = atomic(millis);
        LocalTime result = AtomicLongConversions.toLocalTime(source, defaultConverter());

        // Verify that the conversion result matches the library's direct conversion.
        LocalTime expected = expectedLocalTime(millis);
        assertEquals("LocalTime conversion must match LongConversions result", expected, result);
    }

    @Test
    public void testToLocalTime_NullArgument() {
        try {
            AtomicLongConversions.toLocalTime(null, defaultConverter());
            fail("Expected NullPointerException when 'from' is null");
        } catch (NullPointerException expected) {
            // Expected path
        }
    }

    @Test
    public void testToLocalTime_WrongType() {
        try {
            // Passing an Integer should cause a ClassCastException during the cast to AtomicLong
            AtomicLongConversions.toLocalTime(Integer.valueOf(42), defaultConverter());
            fail("Expected ClassCastException for incompatible source type");
        } catch (ClassCastException expected) {
            // Expected path
        }
    }

    @Test
    public void testToLocalTime_NegativeMillis() {
        // Negative epoch milliseconds represent a time before midnight of the reference day.
        // LocalTime.ofSecondOfDay will accept negative values modulo 24h, but LongConversions
        // may throw DateTimeException – we assert the same behaviour is observed.
        long negativeMillis = -1_000L; // -1 second
        AtomicLong source = atomic(negativeMillis);

        try {
            LocalTime result = AtomicLongConversions.toLocalTime(source, defaultConverter());
            // If no exception, the result must equal the direct conversion.
            LocalTime expected = expectedLocalTime(negativeMillis);
            assertEquals(expected, result);
        } catch (DateTimeException e) {
            // Propagation of the exception is also acceptable – the test ensures consistency.
            // Verify that the direct conversion throws the same exception.
            try {
                expectedLocalTime(negativeMillis);
                fail("Direct LongConversions should also have thrown DateTimeException");
            } catch (DateTimeException expected) {
                // Both threw – behaviour is consistent.
            }
        }
    }

    @Test
    public void testToLocalTime_ValueBeyondOneDay() {
        // 27 hours in milliseconds – should wrap around to 03:00:00
        long millis = 27L * 60 * 60 * 1000; // 27 hours
        AtomicLong source = atomic(millis);

        LocalTime result = AtomicLongConversions.toLocalTime(source, defaultConverter());
        LocalTime expected = expectedLocalTime(millis);
        assertEquals("Conversion should correctly handle values > 24h (wrap‑around)", expected, result);
    }

    @Test
    public void testToLocalTime_MaxLongValue() {
        // Very large value – may cause overflow in the underlying conversion.
        // The test asserts that the method either returns a LocalTime or throws
        // a DateTimeException, matching the behaviour of LongConversions.
        AtomicLong source = atomic(Long.MAX_VALUE);
        try {
            LocalTime result = AtomicLongConversions.toLocalTime(source, defaultConverter());
            // If conversion succeeds, compare with direct conversion.
            LocalTime expected = expectedLocalTime(Long.MAX_VALUE);
            assertEquals(expected, result);
        } catch (DateTimeException e) {
            // Ensure the direct conversion also throws.
            try {
                expectedLocalTime(Long.MAX_VALUE);
                fail("Direct LongConversions should have thrown DateTimeException as well");
            } catch (DateTimeException expected) {
                // Consistent behaviour.
            }
        }
    }
}
