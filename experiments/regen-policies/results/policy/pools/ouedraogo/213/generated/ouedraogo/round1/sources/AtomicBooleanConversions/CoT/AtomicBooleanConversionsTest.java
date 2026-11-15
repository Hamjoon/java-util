package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit tests for {@link AtomicBooleanConversions}.
 * <p>
 * The test class resides in the same package as {@code AtomicBooleanConversions}
 * so it can access the package‑private static conversion methods.
 * </p>
 */
public class AtomicBooleanConversionsTest
{
    private Converter defaultConverter;
    private ConverterOptions defaultOptions;

    @Before
    public void setUp()
    {
        // The library supplies a public no‑arg constructor for ConverterOptions.
        // If the real library uses a different constructor, adjust accordingly.
        defaultOptions = new ConverterOptions();               // trueChar()/falseChar() are defined here
        defaultConverter = new Converter(defaultOptions);
    }

    /* -------------------------------------------------
       Helper to create an AtomicBoolean with the given value
       ------------------------------------------------- */
    private static AtomicBoolean ab(boolean value)
    {
        return new AtomicBoolean(value);
    }

    /* -------------------------------------------------
       BASIC FUNCTIONALITY TESTS
       ------------------------------------------------- */

    @Test
    public void testToByte_true()
    {
        Byte result = AtomicBooleanConversions.toByte(ab(true), defaultConverter);
        assertEquals(CommonValues.BYTE_ONE, result);
    }

    @Test
    public void testToByte_false()
    {
        Byte result = AtomicBooleanConversions.toByte(ab(false), defaultConverter);
        assertEquals(CommonValues.BYTE_ZERO, result);
    }

    @Test
    public void testToShort_true()
    {
        Short result = AtomicBooleanConversions.toShort(ab(true), defaultConverter);
        assertEquals(CommonValues.SHORT_ONE, result);
    }

    @Test
    public void testToShort_false()
    {
        Short result = AtomicBooleanConversions.toShort(ab(false), defaultConverter);
        assertEquals(CommonValues.SHORT_ZERO, result);
    }

    @Test
    public void testToInt_true()
    {
        Integer result = AtomicBooleanConversions.toInt(ab(true), defaultConverter);
        assertEquals(CommonValues.INTEGER_ONE, result);
    }

    @Test
    public void testToInt_false()
    {
        Integer result = AtomicBooleanConversions.toInt(ab(false), defaultConverter);
        assertEquals(CommonValues.INTEGER_ZERO, result);
    }

    @Test
    public void testToLong_true()
    {
        Long result = AtomicBooleanConversions.toLong(ab(true), defaultConverter);
        assertEquals(CommonValues.LONG_ONE, result);
    }

    @Test
    public void testToLong_false()
    {
        Long result = AtomicBooleanConversions.toLong(ab(false), defaultConverter);
        assertEquals(CommonValues.LONG_ZERO, result);
    }

    @Test
    public void testToFloat_true()
    {
        Float result = AtomicBooleanConversions.toFloat(ab(true), defaultConverter);
        assertEquals(CommonValues.FLOAT_ONE, result);
    }

    @Test
    public void testToFloat_false()
    {
        Float result = AtomicBooleanConversions.toFloat(ab(false), defaultConverter);
        assertEquals(CommonValues.FLOAT_ZERO, result);
    }

    @Test
    public void testToDouble_true()
    {
        Double result = AtomicBooleanConversions.toDouble(ab(true), defaultConverter);
        assertEquals(CommonValues.DOUBLE_ONE, result);
    }

    @Test
    public void testToDouble_false()
    {
        Double result = AtomicBooleanConversions.toDouble(ab(false), defaultConverter);
        assertEquals(CommonValues.DOUBLE_ZERO, result);
    }

    @Test
    public void testToBoolean_true()
    {
        boolean result = AtomicBooleanConversions.toBoolean(ab(true), defaultConverter);
        assertTrue(result);
    }

    @Test
    public void testToBoolean_false()
    {
        boolean result = AtomicBooleanConversions.toBoolean(ab(false), defaultConverter);
        assertFalse(result);
    }

    @Test
    public void testToAtomicBoolean_true()
    {
        AtomicBoolean src = ab(true);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(src, defaultConverter);
        assertNotSame(src, result);
        assertTrue(result.get());
    }

    @Test
    public void testToAtomicBoolean_false()
    {
        AtomicBoolean src = ab(false);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(src, defaultConverter);
        assertNotSame(src, result);
        assertFalse(result.get());
    }

    @Test
    public void testToAtomicInteger_true()
    {
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(ab(true), defaultConverter);
        assertEquals(1, result.get());
    }

    @Test
    public void testToAtomicInteger_false()
    {
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(ab(false), defaultConverter);
        assertEquals(0, result.get());
    }

    @Test
    public void testToAtomicLong_true()
    {
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(ab(true), defaultConverter);
        assertEquals(1L, result.get());
    }

    @Test
    public void testToAtomicLong_false()
    {
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(ab(false), defaultConverter);
        assertEquals(0L, result.get());
    }

    @Test
    public void testToCharacter_true()
    {
        char expected = defaultOptions.trueChar();
        char result = AtomicBooleanConversions.toCharacter(ab(true), defaultConverter);
        assertEquals(expected, result);
    }

    @Test
    public void testToCharacter_false()
    {
        char expected = defaultOptions.falseChar();
        char result = AtomicBooleanConversions.toCharacter(ab(false), defaultConverter);
        assertEquals(expected, result);
    }

    @Test
    public void testToBigDecimal_true()
    {
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(ab(true), defaultConverter);
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    public void testToBigDecimal_false()
    {
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(ab(false), defaultConverter);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testToBigInteger_true()
    {
        BigInteger result = AtomicBooleanConversions.toBigInteger(ab(true), defaultConverter);
        assertEquals(BigInteger.ONE, result);
    }

    @Test
    public void testToBigInteger_false()
    {
        BigInteger result = AtomicBooleanConversions.toBigInteger(ab(false), defaultConverter);
        assertEquals(BigInteger.ZERO, result);
    }

    /* -------------------------------------------------
       EDGE‑CASE & EXCEPTION TESTS
       ------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToByte_nullFrom()
    {
        AtomicBooleanConversions.toByte(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToByte_wrongType()
    {
        AtomicBooleanConversions.toByte("not an AtomicBoolean", defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacter_nullConverter()
    {
        // The method dereferences converter.getOptions(), so a null converter must cause NPE.
        AtomicBooleanConversions.toCharacter(ab(true), null);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacter_nullFrom()
    {
        AtomicBooleanConversions.toCharacter(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacter_wrongFromType()
    {
        AtomicBooleanConversions.toCharacter(Boolean.TRUE, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_nullFrom()
    {
        AtomicBooleanConversions.toBigInteger(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_wrongFromType()
    {
        AtomicBooleanConversions.toBigInteger(Integer.valueOf(1), defaultConverter);
    }

    @Test
    public void testToAtomicBoolean_isNewInstance()
    {
        AtomicBoolean src = ab(true);
        AtomicBoolean conv = AtomicBooleanConversions.toAtomicBoolean(src, defaultConverter);
        // Mutate the source after conversion – the result must stay unchanged.
        src.set(false);
        assertTrue(conv.get()); // still true
    }

    @Test
    public void testToAtomicInteger_isIndependentInstance()
    {
        AtomicBoolean src = ab(false);
        AtomicInteger conv = AtomicBooleanConversions.toAtomicInteger(src, defaultConverter);
        src.set(true);
        assertEquals(0, conv.get()); // conversion result should not reflect later changes
    }

    @Test
    public void testToAtomicLong_isIndependentInstance()
    {
        AtomicBoolean src = ab(false);
        AtomicLong conv = AtomicBooleanConversions.toAtomicLong(src, defaultConverter);
        src.set(true);
        assertEquals(0L, conv.get());
    }
}
