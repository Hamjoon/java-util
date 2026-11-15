package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalTime;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@code IntegerConversions}.
 *
 * The class under test is package‑private, therefore the test class lives in the same
 * package so it can access the static method {@code toLocalTime(Object, Converter)}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversion – delegating to {@code LongConversions.toLocalTime}</li>
 *   <li>Typical positive values, zero, negative values and values that exceed a single day</li>
 *   <li>Invalid input types and null arguments that should trigger exceptions</li>
 * </ul>
 */
public class IntegerConversionsTest
{
    private Converter converter;

    /** Create a default {@link Converter} that can be passed to the conversion methods. */
    @Before
    public void setUp()
    {
        // ConverterOptions is part of the public API of the library; it has a public no‑arg ctor.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------
     *  Basic functionality – delegating to LongConversions
     * ------------------------------------------------- */

    @Test
    public void testToLocalTime_ZeroMilliseconds()
    {
        Integer ms = 0;
        LocalTime expected = LongConversions.toLocalTime(0L, converter);
        LocalTime actual   = IntegerConversions.toLocalTime(ms, converter);
        assertEquals("Conversion of 0 ms should match LongConversions result", expected, actual);
    }

    @Test
    public void testToLocalTime_TypicalPositiveValue()
    {
        Integer ms = 12_345_678; // arbitrary positive number of milliseconds
        LocalTime expected = LongConversions.toLocalTime(ms.longValue(), converter);
        LocalTime actual   = IntegerConversions.toLocalTime(ms, converter);
        assertEquals("Typical positive value should be delegated correctly", expected, actual);
    }

    @Test
    public void testToLocalTime_NegativeMilliseconds()
    {
        Integer ms = -5_000; // negative milliseconds (before midnight)
        LocalTime expected = LongConversions.toLocalTime(ms.longValue(), converter);
        LocalTime actual   = IntegerConversions.toLocalTime(ms, converter);
        assertEquals("Negative milliseconds should be delegated correctly", expected, actual);
    }

    @Test
    public void testToLocalTime_ValueExceedingOneDay()
    {
        // 30 hours expressed in milliseconds – exceeds a single 24‑hour day
        Integer ms = 30 * 60 * 60 * 1_000;
        LocalTime expected = LongConversions.toLocalTime(ms.longValue(), converter);
        LocalTime actual   = IntegerConversions.toLocalTime(ms, converter);
        assertEquals("Values > 24h should be delegated without alteration", expected, actual);
    }

    /* -------------------------------------------------
     *  Edge‑case & exception handling
     * ------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToLocalTime_InvalidFromType()
    {
        // The method casts the first argument to Integer, so a non‑Integer triggers CCE.
        IntegerConversions.toLocalTime("not an integer", converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullFromArgument()
    {
        // Null cannot be cast to Integer – NPE is the expected outcome.
        IntegerConversions.toLocalTime(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullConverter()
    {
        // The delegated LongConversions method will receive a null Converter and should NPE.
        IntegerConversions.toLocalTime(1_000, null);
    }

    /* -------------------------------------------------
     *  Additional sanity check – maximum int value
     * ------------------------------------------------- */

    @Test
    public void testToLocalTime_MaxIntValue()
    {
        Integer ms = Integer.MAX_VALUE; // 2,147,483,647 ms ≈ 596 hours
        LocalTime expected = LongConversions.toLocalTime(ms.longValue(), converter);
        LocalTime actual   = IntegerConversions.toLocalTime(ms, converter);
        assertEquals("Maximum int value should be handled correctly", expected, actual);
    }
}
