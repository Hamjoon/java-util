package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link AtomicIntegerConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical usage – normal positive and negative values.</li>
 *   <li>Edge cases – zero, {@code null}, and wrong source types.</li>
 *   <li>Behaviour verification – the returned {@link AtomicInteger} is a new instance
 *       with the same numeric value, and {@code toLocalTime} delegates to
 *       {@link LongConversions#toLocalTime(long, Converter)} correctly.</li>
 * </ul>
 *
 * No mocking frameworks are used; all objects are created through their public
 * constructors as required by the task description.
 */
public class AtomicIntegerConversionsTest {

    // A dummy Converter instance – the conversion methods under test do not use it,
    // but it must be supplied to match the method signatures.
    private Converter dummyConverter;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // Converter is a final class; we can safely instantiate it with a null
        // ConverterOptions because the methods we test never dereference the options.
        dummyConverter = new Converter(null);
    }

    /* -------------------------------------------------
     *  Tests for toAtomicInteger(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void toAtomicInteger_ShouldReturnNewInstanceWithSameValue() {
        AtomicInteger original = new AtomicInteger(42);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, dummyConverter);

        assertNotNull("Result must not be null", result);
        assertNotSame("Result must be a different instance", original, result);
        assertEquals("Result must contain the same int value as the original",
                     original.intValue(), result.intValue());
    }

    @Test
    public void toAtomicInteger_ShouldHandleNegativeValues() {
        AtomicInteger original = new AtomicInteger(-7);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, dummyConverter);

        assertEquals(-7, result.intValue());
    }

    @Test
    public void toAtomicInteger_ShouldHandleZero() {
        AtomicInteger original = new AtomicInteger(0);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, dummyConverter);

        assertEquals(0, result.intValue());
    }

    @Test
    public void toAtomicInteger_NullInput_ShouldThrowNullPointerException() {
        thrown.expect(NullPointerException.class);
        AtomicIntegerConversions.toAtomicInteger(null, dummyConverter);
    }

    @Test
    public void toAtomicInteger_WrongSourceType_ShouldThrowClassCastException() {
        thrown.expect(ClassCastException.class);
        // Passing a String instead of AtomicInteger forces the cast in the method to fail.
        AtomicIntegerConversions.toAtomicInteger("not an AtomicInteger", dummyConverter);
    }

    /* -------------------------------------------------
     *  Tests for toLocalTime(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void toLocalTime_ShouldDelegateToLongConversions() {
        // Choose a value that is easy to verify – 0 milliseconds corresponds to 00:00.
        AtomicInteger atomic = new AtomicInteger(0);
        LocalTime expected = LongConversions.toLocalTime(0L, dummyConverter);
        LocalTime actual = AtomicIntegerConversions.toLocalTime(atomic, dummyConverter);

        assertEquals("Delegation to LongConversions must produce identical result",
                     expected, actual);
    }

    @Test
    public void toLocalTime_ShouldConvertPositiveValue() {
        // 1 hour = 60 * 60 * 1000 milliseconds.
        int oneHourMillis = 60 * 60 * 1000;
        AtomicInteger atomic = new AtomicInteger(oneHourMillis);
        LocalTime expected = LongConversions.toLocalTime((long) oneHourMillis, dummyConverter);
        LocalTime actual = AtomicIntegerConversions.toLocalTime(atomic, dummyConverter);

        assertEquals(expected, actual);
    }

    @Test
    public void toLocalTime_ShouldConvertNegativeValue() {
        // Negative milliseconds should map to a time before midnight of the same day.
        int negativeMillis = -15 * 60 * 1000; // -15 minutes
        AtomicInteger atomic = new AtomicInteger(negativeMillis);
        LocalTime expected = LongConversions.toLocalTime((long) negativeMillis, dummyConverter);
        LocalTime actual = AtomicIntegerConversions.toLocalTime(atomic, dummyConverter);

        assertEquals(expected, actual);
    }

    @Test
    public void toLocalTime_NullInput_ShouldThrowNullPointerException() {
        thrown.expect(NullPointerException.class);
        AtomicIntegerConversions.toLocalTime(null, dummyConverter);
    }

    @Test
    public void toLocalTime_WrongSourceType_ShouldThrowClassCastException() {
        thrown.expect(ClassCastException.class);
        // Passing an Integer instead of AtomicInteger triggers the cast exception.
        AtomicIntegerConversions.toLocalTime(Integer.valueOf(123), dummyConverter);
    }
}
