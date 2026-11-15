package com.cedarsoftware.util.convert;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link AtomicIntegerConversions}.
 *
 * The tests are placed in the same package as the class under test so that the
 * package‑private static methods can be accessed directly.
 *
 * The test cases cover:
 *   1. Normal (happy‑path) behaviour.
 *   2. Edge‑case values (zero, negative, Integer.MAX_VALUE).
 *   3. Incorrect argument types (ClassCastException).
 *   4. Null arguments (NullPointerException).
 */
public class AtomicIntegerConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // Converter has a public constructor that takes a ConverterOptions instance.
        // Using the default options is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     *  Tests for toAtomicInteger(Object, Converter)
     * -------------------------------------------------------------- */

    @Test
    public void testToAtomicInteger_BasicConversion() {
        AtomicInteger source = new AtomicInteger(42);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, converter);

        assertNotNull("Result must not be null", result);
        assertEquals("Result must contain the same int value as the source",
                source.intValue(), result.intValue());

        // Ensure a new instance is created (the method must not return the original object)
        assertNotSame("Result should be a new instance, not the original", source, result);
    }

    @Test
    public void testToAtomicInteger_WithZero() {
        AtomicInteger source = new AtomicInteger(0);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, converter);
        assertEquals(0, result.intValue());
    }

    @Test
    public void testToAtomicInteger_WithNegativeValue() {
        AtomicInteger source = new AtomicInteger(-123);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, converter);
        assertEquals(-123, result.intValue());
    }

    @Test
    public void testToAtomicInteger_WithMaxInt() {
        AtomicInteger source = new AtomicInteger(Integer.MAX_VALUE);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, converter);
        assertEquals(Integer.MAX_VALUE, result.intValue());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_ClassCastException() {
        // Passing a non‑AtomicInteger object should cause a ClassCastException
        AtomicIntegerConversions.toAtomicInteger("not an AtomicInteger", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullSource() {
        // The method casts the argument before dereferencing; null leads to NPE
        AtomicIntegerConversions.toAtomicInteger(null, converter);
    }

    @Test
    public void testToAtomicInteger_NullConverterIsIgnored() {
        // The converter argument is not used in the implementation, so null is allowed
        AtomicInteger source = new AtomicInteger(7);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, null);
        assertEquals(7, result.intValue());
    }

    /* --------------------------------------------------------------
     *  Tests for toLocalTime(Object, Converter)
     * -------------------------------------------------------------- */

    @Test
    public void testToLocalTime_BasicConversion() {
        AtomicInteger source = new AtomicInteger(12_345L); // arbitrary value
        LocalTime expected = LongConversions.toLocalTime((long) source.get(), converter);
        LocalTime actual   = AtomicIntegerConversions.toLocalTime(source, converter);

        assertNotNull("Resulting LocalTime must not be null", actual);
        assertEquals("Conversion must delegate correctly to LongConversions",
                expected, actual);
    }

    @Test
    public void testToLocalTime_ZeroValue() {
        AtomicInteger source = new AtomicInteger(0);
        LocalTime expected = LongConversions.toLocalTime(0L, converter);
        LocalTime actual   = AtomicIntegerConversions.toLocalTime(source, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime_MaxIntValue() {
        AtomicInteger source = new AtomicInteger(Integer.MAX_VALUE);
        LocalTime expected = LongConversions.toLocalTime((long) Integer.MAX_VALUE, converter);
        LocalTime actual   = AtomicIntegerConversions.toLocalTime(source, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTime_ClassCastException() {
        // Passing a non‑AtomicInteger should trigger a ClassCastException before LongConversions is called
        AtomicIntegerConversions.toLocalTime("string", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullSource() {
        // Null source leads to NPE when casting to AtomicInteger
        AtomicIntegerConversions.toLocalTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullConverter() {
        // LongConversions.toLocalTime is expected to require a non‑null Converter;
        // passing null should therefore result in NPE inside that method.
        AtomicInteger source = new AtomicInteger(123);
        AtomicIntegerConversions.toLocalTime(source, null);
    }
}
