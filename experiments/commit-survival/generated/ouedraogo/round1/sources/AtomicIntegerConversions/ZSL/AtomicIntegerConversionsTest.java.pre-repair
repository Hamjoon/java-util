package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for {@link AtomicIntegerConversions}.
 * <p>
 * The tests are placed in the same package as the class under test so that the
 * package‑private static methods can be accessed directly.
 * </p>
 */
public class AtomicIntegerConversionsTest {

    /** A {@link Converter} instance used for the delegation tests.
     *  The {@code Converter} class is final and only requires a {@code ConverterOptions}
     *  argument.  Passing {@code null} is acceptable for the purpose of these tests
     *  because the conversion logic exercised here does not depend on the options. */
    private Converter converter;

    @Before
    public void setUp() {
        // The constructor of Converter accepts a ConverterOptions instance.
        // Using null is safe for the current tests because the conversion methods
        // we invoke do not dereference the options object.
        converter = new Converter(null);
    }

    /* -----------------------------------------------------------------------
     * Tests for toAtomicInteger(Object, Converter)
     * -----------------------------------------------------------------------
     */

    @Test
    public void testToAtomicIntegerCreatesNewInstance() {
        AtomicInteger original = new AtomicInteger(42);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, converter);
        assertNotNull("Resulting AtomicInteger should not be null", result);
        assertNotSame("Result should be a new instance, not the original", original, result);
        assertEquals("Result should contain the same int value as the original", original.intValue(), result.intValue());
    }

    @Test
    public void testToAtomicIntegerCopiesPositiveValue() {
        AtomicInteger original = new AtomicInteger(Integer.MAX_VALUE);
        AtomicInteger copy = AtomicIntegerConversions.toAtomicInteger(original, converter);
        assertEquals(Integer.MAX_VALUE, copy.intValue());
    }

    @Test
    public void testToAtomicIntegerCopiesNegativeValue() {
        AtomicInteger original = new AtomicInteger(-12345);
        AtomicInteger copy = AtomicIntegerConversions.toAtomicInteger(original, converter);
        assertEquals(-12345, copy.intValue());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerWithInvalidSourceThrowsException() {
        // Passing an object that is not an AtomicInteger should result in a ClassCastException
        String notAnAtomic = "not an atomic integer";
        AtomicIntegerConversions.toAtomicInteger(notAnAtomic, converter);
    }

    /* -----------------------------------------------------------------------
     * Tests for toLocalTime(Object, Converter)
     * -----------------------------------------------------------------------
     */

    @Test
    public void testToLocalTimeDelegatesToLongConversions() {
        AtomicInteger atomic = new AtomicInteger(10_000); // 10,000 seconds
        LocalTime expected = LongConversions.toLocalTime((long) atomic.get(), converter);
        LocalTime actual = AtomicIntegerConversions.toLocalTime(atomic, converter);
        assertEquals("toLocalTime should delegate to LongConversions with the same result", expected, actual);
    }

    @Test
    public void testToLocalTimeZeroValue() {
        AtomicInteger atomic = new AtomicInteger(0);
        LocalTime expected = LongConversions.toLocalTime(0L, converter);
        LocalTime actual = AtomicIntegerConversions.toLocalTime(atomic, converter);
        assertEquals("Conversion of zero should match LongConversions result", expected, actual);
    }

    @Test
    public void testToLocalTimeMaximumDaySecond() {
        // 23:59:59 = 86,399 seconds
        AtomicInteger atomic = new AtomicInteger(86_399);
        LocalTime expected = LongConversions.toLocalTime(86_399L, converter);
        LocalTime actual = AtomicIntegerConversions.toLocalTime(atomic, converter);
        assertEquals("Conversion of the last second of the day should match LongConversions", expected, actual);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTimeWithInvalidSourceThrowsException() {
        // Passing a non‑AtomicInteger should cause a ClassCastException before any delegation
        Integer notAtomic = 123;
        AtomicIntegerConversions.toLocalTime(notAtomic, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTimeWithNullConverterThrowsException() {
        // The underlying LongConversions implementation expects a non‑null Converter.
        // Passing null should result in a NullPointerException.
        AtomicInteger atomic = new AtomicInteger(1);
        AtomicIntegerConversions.toLocalTime(atomic, null);
    }
}
