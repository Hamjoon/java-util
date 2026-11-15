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
 *   <li>Verification that modifications to the source {@code AtomicInteger}
 *       do not affect the converted instance.</li>
 *   <li>Handling of {@code null} and non‑{@code AtomicInteger} inputs (expected to
 *       throw {@link NullPointerException} or {@link ClassCastException}).</li>
 *   <li>Conversion of an {@code AtomicInteger} to {@link LocalTime} via
 *       {@link LongConversions#toLocalTime(long, Converter)} and that the result
 *       matches a direct call to the same utility method.</li>
 * </ul>
 *
 * The test class uses only the public API of {@code Converter} (constructor and
 * the static {@code identity} method) and the classes that appear in the source
 * of {@code AtomicIntegerConversions}.
 */
public class AtomicIntegerConversionsTest {

    /** A minimal {@code Converter} instance – the implementation details are irrelevant
     *  for the conversion utilities we are testing, because the conversion logic
     *  resides in {@code LongConversions}. */
    private static final Converter DUMMY_CONVERTER = new Converter(null);

    @Test
    public void testToAtomicIntegerCreatesNewInstanceWithSameValue() {
        AtomicInteger original = new AtomicInteger(42);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, DUMMY_CONVERTER);

        // The value must be identical
        assertEquals(original.intValue(), result.intValue());

        // The result must be a different object (new instance)
        assertNotSame("Result should be a new AtomicInteger, not the original", original, result);

        // Changing the original must not affect the result
        original.set(99);
        assertEquals(42, result.intValue());
        assertEquals(99, original.intValue());
    }

    @Test
    public void testToAtomicIntegerWithZeroAndNegativeValues() {
        AtomicInteger zero = new AtomicInteger(0);
        AtomicInteger neg = new AtomicInteger(-12345);

        AtomicInteger zeroResult = AtomicIntegerConversions.toAtomicInteger(zero, DUMMY_CONVERTER);
        AtomicInteger negResult = AtomicIntegerConversions.toAtomicInteger(neg, DUMMY_CONVERTER);

        assertEquals(0, zeroResult.intValue());
        assertEquals(-12345, negResult.intValue());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNullInputThrowsNPE() {
        AtomicIntegerConversions.toAtomicInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerWrongTypeThrowsClassCastException() {
        // Pass an object that is not an AtomicInteger
        AtomicIntegerConversions.toAtomicInteger("not an atomic", DUMMY_CONVERTER);
    }

    @Test
    public void testToLocalTimeDelegatesToLongConversions() {
        // Choose a value that corresponds to a well‑known time.
        // For example, 0 seconds after midnight → 00:00
        AtomicInteger atomic = new AtomicInteger(0);
        LocalTime expected = LongConversions.toLocalTime(0L, DUMMY_CONVERTER);
        LocalTime actual = AtomicIntegerConversions.toLocalTime(atomic, DUMMY_CONVERTER);
        assertEquals("LocalTime conversion for zero should be midnight", expected, actual);

        // Another value: 12 hours, 34 minutes, 56 seconds → 12:34:56
        int secondsSinceMidnight = (12 * 60 * 60) + (34 * 60) + 56;
        atomic.set(secondsSinceMidnight);
        expected = LongConversions.toLocalTime((long) secondsSinceMidnight, DUMMY_CONVERTER);
        actual = AtomicIntegerConversions.toLocalTime(atomic, DUMMY_CONVERTER);
        assertEquals("LocalTime conversion for 12:34:56 should match", expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTimeNullInputThrowsNPE() {
        AtomicIntegerConversions.toLocalTime(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTimeWrongTypeThrowsClassCastException() {
        AtomicIntegerConversions.toLocalTime("not atomic", DUMMY_CONVERTER);
    }

    @Test
    public void testToLocalTimeDoesNotMutateSourceAtomicInteger() {
        AtomicInteger atomic = new AtomicInteger(42);
        LocalTime before = LongConversions.toLocalTime(42L, DUMMY_CONVERTER);
        LocalTime after = AtomicIntegerConversions.toLocalTime(atomic, DUMMY_CONVERTER);
        assertEquals(before, after);
        // Ensure the atomic integer value is unchanged
        assertEquals(42, atomic.get());
    }
}
