package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link AtomicBooleanConversions}.
 *
 * <p>Each public static conversion method is exercised with:
 * <ul>
 *   <li>Typical values – {@code true} and {@code false} AtomicBoolean instances.</li>
 *   <li>Edge cases – {@code null} input and an object of the wrong type.</li>
 *   <li>Behaviour checks – new objects are created where required and the
 *       {@link ConverterOptions} supplied to {@link Converter} are honoured.</li>
 * </ul>
 *
 * <p>Only APIs that appear in the supplied source or in the listed
 * {@code Converter} signatures are used; no mocking frameworks are employed.
 */
public class AtomicBooleanConversionsTest
{
    /** A {@link Converter} with default options – used for all tests that need a Converter. */
    private Converter converter;

    @Before
    public void setUp()
    {
        // The Converter constructor that accepts a ConverterOptions instance is public.
        // Passing {@code null} makes the Converter fall back to its default options.
        this.converter = new Converter(null);
    }

    /* --------------------------------------------------------------
     *  toByte / toShort / toInt / toLong / toFloat / toDouble
     * -------------------------------------------------------------- */

    @Test
    public void testToByte_TrueAndFalse()
    {
        assertEquals(CommonValues.BYTE_ONE, AtomicBooleanConversions.toByte(new AtomicBoolean(true), converter));
        assertEquals(CommonValues.BYTE_ZERO, AtomicBooleanConversions.toByte(new AtomicBoolean(false), converter));
    }

    @Test
    public void testToShort_TrueAndFalse()
    {
        assertEquals(CommonValues.SHORT_ONE, AtomicBooleanConversions.toShort(new AtomicBoolean(true), converter));
        assertEquals(CommonValues.SHORT_ZERO, AtomicBooleanConversions.toShort(new AtomicBoolean(false), converter));
    }

    @Test
    public void testToInt_TrueAndFalse()
    {
        assertEquals(CommonValues.INTEGER_ONE, AtomicBooleanConversions.toInt(new AtomicBoolean(true), converter));
        assertEquals(CommonValues.INTEGER_ZERO, AtomicBooleanConversions.toInt(new AtomicBoolean(false), converter));
    }

    @Test
    public void testToLong_TrueAndFalse()
    {
        assertEquals(CommonValues.LONG_ONE, AtomicBooleanConversions.toLong(new AtomicBoolean(true), converter));
        assertEquals(CommonValues.LONG_ZERO, AtomicBooleanConversions.toLong(new AtomicBoolean(false), converter));
    }

    @Test
    public void testToFloat_TrueAndFalse()
    {
        assertEquals(CommonValues.FLOAT_ONE, AtomicBooleanConversions.toFloat(new AtomicBoolean(true), converter));
        assertEquals(CommonValues.FLOAT_ZERO, AtomicBooleanConversions.toFloat(new AtomicBoolean(false), converter));
    }

    @Test
    public void testToDouble_TrueAndFalse()
    {
        assertEquals(CommonValues.DOUBLE_ONE, AtomicBooleanConversions.toDouble(new AtomicBoolean(true), converter));
        assertEquals(CommonValues.DOUBLE_ZERO, AtomicBooleanConversions.toDouble(new AtomicBoolean(false), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToByte_NullInput()
    {
        AtomicBooleanConversions.toByte(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToByte_WrongType()
    {
        AtomicBooleanConversions.toByte(Boolean.TRUE, converter);
    }

    /* --------------------------------------------------------------
     *  toBoolean
     * -------------------------------------------------------------- */

    @Test
    public void testToBoolean_TrueAndFalse()
    {
        assertTrue(AtomicBooleanConversions.toBoolean(new AtomicBoolean(true), converter));
        assertFalse(AtomicBooleanConversions.toBoolean(new AtomicBoolean(false), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToBoolean_NullInput()
    {
        AtomicBooleanConversions.toBoolean(null, converter);
    }

    /* --------------------------------------------------------------
     *  toAtomicBoolean
     * -------------------------------------------------------------- */

    @Test
    public void testToAtomicBooleanCreatesNewInstance()
    {
        AtomicBoolean src = new AtomicBoolean(true);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(src, converter);
        assertNotSame("Result must be a new instance", src, result);
        assertEquals("Result must hold the same boolean value", src.get(), result.get());
    }

    @Test
    public void testToAtomicBoolean_False()
    {
        AtomicBoolean src = new AtomicBoolean(false);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(src, converter);
        assertFalse(result.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBoolean_NullInput()
    {
        AtomicBooleanConversions.toAtomicBoolean(null, converter);
    }

    /* --------------------------------------------------------------
     *  toAtomicInteger / toAtomicLong
     * -------------------------------------------------------------- */

    @Test
    public void testToAtomicInteger_TrueAndFalse()
    {
        AtomicInteger trueResult = AtomicBooleanConversions.toAtomicInteger(new AtomicBoolean(true), converter);
        AtomicInteger falseResult = AtomicBooleanConversions.toAtomicInteger(new AtomicBoolean(false), converter);
        assertEquals(1, trueResult.get());
        assertEquals(0, falseResult.get());
    }

    @Test
    public void testToAtomicLong_TrueAndFalse()
    {
        AtomicLong trueResult = AtomicBooleanConversions.toAtomicLong(new AtomicBoolean(true), converter);
        AtomicLong falseResult = AtomicBooleanConversions.toAtomicLong(new AtomicBoolean(false), converter);
        assertEquals(1L, trueResult.get());
        assertEquals(0L, falseResult.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullInput()
    {
        AtomicBooleanConversions.toAtomicInteger(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput()
    {
        AtomicBooleanConversions.toAtomicLong(null, converter);
    }

    /* --------------------------------------------------------------
     *  toCharacter
     * -------------------------------------------------------------- */

    @Test
    public void testToCharacter_UsesConverterOptions()
    {
        ConverterOptions opts = converter.getOptions();
        char trueChar = opts.trueChar();
        char falseChar = opts.falseChar();

        char resultTrue = AtomicBooleanConversions.toCharacter(new AtomicBoolean(true), converter);
        char resultFalse = AtomicBooleanConversions.toCharacter(new AtomicBoolean(false), converter);

        assertEquals("True AtomicBoolean should map to options.trueChar()", trueChar, resultTrue);
        assertEquals("False AtomicBoolean should map to options.falseChar()", falseChar, resultFalse);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacter_NullInput()
    {
        AtomicBooleanConversions.toCharacter(null, converter);
    }

    /* --------------------------------------------------------------
     *  toBigDecimal / toBigInteger
     * -------------------------------------------------------------- */

    @Test
    public void testToBigDecimal_TrueAndFalse()
    {
        BigDecimal trueResult = AtomicBooleanConversions.toBigDecimal(new AtomicBoolean(true), converter);
        BigDecimal falseResult = AtomicBooleanConversions.toBigDecimal(new AtomicBoolean(false), converter);
        assertEquals(BigDecimal.ONE, trueResult);
        assertEquals(BigDecimal.ZERO, falseResult);
    }

    @Test
    public void testToBigInteger_TrueAndFalse()
    {
        BigInteger trueResult = AtomicBooleanConversions.toBigInteger(new AtomicBoolean(true), converter);
        BigInteger falseResult = AtomicBooleanConversions.toBigInteger(new AtomicBoolean(false), converter);
        assertEquals(BigInteger.ONE, trueResult);
        assertEquals(BigInteger.ZERO, falseResult);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput()
    {
        AtomicBooleanConversions.toBigDecimal(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput()
    {
        AtomicBooleanConversions.toBigInteger(null, converter);
    }
}
