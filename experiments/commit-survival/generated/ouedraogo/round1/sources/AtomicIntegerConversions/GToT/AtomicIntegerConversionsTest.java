package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.time.LocalTime;
import java.time.DateTimeException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for {@link AtomicIntegerConversions}.
 *
 * The tests cover:
 *   – normal behaviour,
 *   – edge‑cases (nulls, wrong types, boundary numeric values),
 *   – and expected exceptions.
 */
public class AtomicIntegerConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is assumed to have a public no‑arg constructor.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------
     *  Tests for toAtomicInteger(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToAtomicInteger_BasicCopy() {
        AtomicInteger original = new AtomicInteger(42);
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, converter);

        // same numeric value
        assertEquals(42, result.intValue());

        // but a different instance
        assertNotSame(original, result);

        // mutating the original does not affect the copy
        original.set(99);
        assertEquals(42, result.intValue());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullFrom() {
        AtomicIntegerConversions.toAtomicInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_WrongType() {
        AtomicIntegerConversions.toAtomicInteger("not an atomic integer", converter);
    }

    /* -------------------------------------------------
     *  Tests for toLocalTime(Object, Converter)
     * ------------------------------------------------- */

    @Test
    public void testToLocalTime_BasicConversion() {
        AtomicInteger atomic = new AtomicInteger(12_345); // 12 345 seconds after midnight
        LocalTime expected = LongConversions.toLocalTime(12_345L, converter);
        LocalTime actual   = AtomicIntegerConversions.toLocalTime(atomic, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime_BoundaryZero() {
        AtomicInteger atomic = new AtomicInteger(0);
        LocalTime expected = LongConversions.toLocalTime(0L, converter);
        assertEquals(expected, AtomicIntegerConversions.toLocalTime(atomic, converter));
    }

    @Test
    public void testToLocalTime_BoundaryLastValidSecond() {
        AtomicInteger atomic = new AtomicInteger(86_399); // 23:59:59
        LocalTime expected = LongConversions.toLocalTime(86_399L, converter);
        assertEquals(expected, AtomicIntegerConversions.toLocalTime(atomic, converter));
    }

    @Test(expected = DateTimeException.class)
    public void testToLocalTime_InvalidSecondValue() {
        // 86 400 seconds = exactly 24:00:00 – not a valid LocalTime
        AtomicInteger atomic = new AtomicInteger(86_400);
        AtomicIntegerConversions.toLocalTime(atomic, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullFrom() {
        AtomicIntegerConversions.toLocalTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTime_WrongType() {
        AtomicIntegerConversions.toLocalTime("string", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullConverter() {
        AtomicInteger atomic = new AtomicInteger(1_234);
        AtomicIntegerConversions.toLocalTime(atomic, null);
    }
}
