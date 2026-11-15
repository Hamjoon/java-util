package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.LocalTime;
import java.time.DateTimeException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JUnit‑4 test suite for {@code AtomicIntegerConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical successful conversions for {@code toAtomicInteger} and {@code toLocalTime}</li>
 *   <li>Edge cases such as zero values and extreme integer ranges</li>
 *   <li>Error scenarios – null input and values that {@link LongConversions#toLocalTime(long, Converter)}
 *       cannot translate to a {@link LocalTime}</li>
 * </ul>
 *
 * Only the public API of {@code Converter} and the classes that appear in the source file
 * (or in the signatures supplied) are used – no mocks or subclassing.
 */
public class AtomicIntegerConversionsTest {

    /** A {@link Converter} instance required by the conversion methods.
     *  The concrete behaviour of {@code Converter} is not exercised here;
     *  we only need a non‑null instance to satisfy the method signatures. */
    private Converter converter;

    @Before
    public void setUp() {
        // {@code ConverterOptions} has a public no‑arg constructor in the library.
        // If the library provides other constructors, this call will still compile
        // because the exact constructor signature is not part of the test contract.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     *  Tests for {@link AtomicIntegerConversions#toAtomicInteger(Object, Converter)}
     * -------------------------------------------------------------- */

    @Test
    public void testToAtomicInteger_TypicalValue() {
        AtomicInteger source = new AtomicInteger(42);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, converter);

        // The result must contain the same int value.
        assertEquals("Converted value must match source value", source.intValue(), result.intValue());

        // It must be a *new* instance – the conversion creates a fresh AtomicInteger.
        assertNotSame("Result should be a new instance", source, result);
    }

    @Test
    public void testToAtomicInteger_NegativeValue() {
        AtomicInteger source = new AtomicInteger(-7);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, converter);
        assertEquals(-7, result.intValue());
        assertNotSame(source, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullInput_ThrowsNPE() {
        // The implementation casts the argument to AtomicInteger and then dereferences it,
        // so a null argument must raise a NullPointerException.
        AtomicIntegerConversions.toAtomicInteger(null, converter);
    }

    /* --------------------------------------------------------------
     *  Tests for {@link AtomicIntegerConversions#toLocalTime(Object, Converter)}
     * -------------------------------------------------------------- */

    @Test
    public void testToLocalTime_TypicalMidday() {
        // 12 hours expressed in nanoseconds – this is the value expected by the library
        // conversion (see LongConversions.toLocalTime implementation in the source).
        long nanosOfDay = 12L * 60 * 60 * 1_000_000_000L;
        AtomicInteger source = new AtomicInteger((int) nanosOfDay); // fits into int for test purposes

        LocalTime expected = LongConversions.toLocalTime(nanosOfDay, converter);
        LocalTime actual   = AtomicIntegerConversions.toLocalTime(source, converter);

        assertEquals("LocalTime should represent noon", expected, actual);
    }

    @Test
    public void testToLocalTime_ZeroValue_ReturnsMidnight() {
        AtomicInteger source = new AtomicInteger(0);
        LocalTime expected = LocalTime.MIDNIGHT;
        LocalTime actual   = AtomicIntegerConversions.toLocalTime(source, converter);
        assertEquals("Zero nanoseconds must map to MIDNIGHT", expected, actual);
    }

    @Test
    public void testToLocalTime_MaxIntValue() {
        // The maximum positive int value as nanoseconds – still a valid time of day
        // (it corresponds to 2 147 483 647 nanoseconds ≈ 2.147 seconds).
        AtomicInteger source = new AtomicInteger(Integer.MAX_VALUE);
        long nanos = Integer.toUnsignedLong(Integer.MAX_VALUE);
        LocalTime expected = LongConversions.toLocalTime(nanos, converter);
        LocalTime actual   = AtomicIntegerConversions.toLocalTime(source, converter);
        assertEquals("Conversion of Integer.MAX_VALUE nanoseconds must match LongConversions", expected, actual);
    }

    @Test(expected = DateTimeException.class)
    public void testToLocalTime_NegativeValue_ThrowsDateTimeException() {
        // Negative nanoseconds are not a valid time‑of‑day; LongConversions is expected
        // to throw a DateTimeException (the same exception the JDK throws for out‑of‑range values).
        AtomicInteger source = new AtomicInteger(-1);
        AtomicIntegerConversions.toLocalTime(source, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTime_WrongSourceType_ThrowsClassCastException() {
        // The method unconditionally casts the input to AtomicInteger.
        // Supplying any other type must result in a ClassCastException.
        String notAnAtomicInteger = "I am not an AtomicInteger";
        AtomicIntegerConversions.toLocalTime(notAnAtomicInteger, converter);
    }
}
