package com.cedarsoftware.util.convert;

import org.junit.Test;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AtomicIntegerConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct copying of the integer value when converting {@code AtomicInteger} → {@code AtomicInteger}.</li>
 *   <li>Ensuring the returned {@code AtomicInteger} is a new instance (no reference sharing).</li>
 *   <li>Behaviour with typical, zero, negative and extreme integer values.</li>
 *   <li>Forwarding of the underlying {@code long} value to {@link LongConversions#toLocalTime(long, Converter)}.</li>
 *   <li>Null handling – the class deliberately casts without null‑checks, so a {@code NullPointerException}
 *       is expected.</li>
 * </ul>
 *
 * The {@link Converter} class does not expose any public factory method besides the constructor.
 * The library permits passing {@code null} for {@link ConverterOptions}; the implementation tolerates it
 * for the purposes of these tests (the conversion logic we invoke does not depend on the options).
 */
public class AtomicIntegerConversionsTest {

    /** Helper to create a minimal {@link Converter} instance. */
    private static Converter createConverter() {
        // The library permits a null options argument; the conversion paths used in these tests
        // (AtomicInteger → AtomicInteger and AtomicInteger → LocalTime) do not require any configuration.
        return new Converter(null);
    }

    @Test
    public void testToAtomicIntegerCopiesValue() {
        Converter converter = createConverter();

        int[] testValues = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, 12345, -54321};

        for (int val : testValues) {
            AtomicInteger original = new AtomicInteger(val);
            AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, converter);

            // The result must have the same int value.
            assertEquals("AtomicInteger value should be copied", val, result.intValue());

            // Must be a new instance – not the same object reference.
            assertNotSame("Result should be a new AtomicInteger instance", original, result);
        }
    }

    @Test
    public void testToAtomicIntegerNullInputThrowsNPE() {
        Converter converter = createConverter();
        try {
            AtomicIntegerConversions.toAtomicInteger(null, converter);
            fail("Expected NullPointerException when source is null");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testToLocalTimeForTypicalValues() {
        Converter converter = createConverter();

        // Choose a range of values that are easy to reason about when interpreted as seconds-of‑day.
        // The LongConversions implementation (as shipped with the library) treats the long as
        // a number of seconds since midnight, converting it via LocalTime.ofSecondOfDay.
        long[] seconds = {0L, 1L, 60L, 3600L, 12 * 3600L + 34 * 60 + 56L, 23 * 3600L + 59 * 60L + 59L};

        for (long sec : seconds) {
            AtomicInteger atomic = new AtomicInteger((int) sec);
            LocalTime expected = LongConversions.toLocalTime(sec, converter);
            LocalTime actual = AtomicIntegerConversions.toLocalTime(atomic, converter);
            assertEquals("LocalTime conversion should forward the same long value", expected, actual);
        }
    }

    @Test
    public void testToLocalTimeWithNegativeAndOverflowValues() {
        Converter converter = createConverter();

        // Negative values and values larger than a day are still processed by LongConversions;
        // we simply verify that the forwarding logic does not alter the result.
        long[] edgeValues = {-1L, -12345L, Long.MAX_VALUE, Long.MIN_VALUE};

        for (long val : edgeValues) {
            // Cast to int because AtomicInteger stores int; overflow is intentional.
            AtomicInteger atomic = new AtomicInteger((int) val);
            long asLong = (long) atomic.get(); // replicates the cast performed in the method under test
            LocalTime expected = LongConversions.toLocalTime(asLong, converter);
            LocalTime actual = AtomicIntegerConversions.toLocalTime(atomic, converter);
            assertEquals("Forwarded conversion must match direct LongConversions call", expected, actual);
        }
    }

    @Test
    public void testToLocalTimeNullInputThrowsNPE() {
        Converter converter = createConverter();
        try {
            AtomicIntegerConversions.toLocalTime(null, converter);
            fail("Expected NullPointerException when source is null");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testToLocalTimeWithNullConverterDoesNotThrow() {
        // The library’s LongConversions methods are tolerant of a null Converter.
        // This test confirms that our wrapper does not introduce a NPE.
        AtomicInteger atomic = new AtomicInteger(12345);
        LocalTime result = AtomicIntegerConversions.toLocalTime(atomic, null);
        // Compare with a direct call that also receives null.
        LocalTime expected = LongConversions.toLocalTime(12345L, null);
        assertEquals("Result should be identical when passing null Converter", expected, result);
    }
}
