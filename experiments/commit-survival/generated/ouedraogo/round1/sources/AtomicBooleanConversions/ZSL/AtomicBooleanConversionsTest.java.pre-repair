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
 * The tests cover every conversion method, exercising both {@code true} and {@code false}
 * values of {@link AtomicBoolean}.  The {@link Converter} instance is created with a
 * default {@link ConverterOptions} object – the only requirement for the class under test
 * is that {@code Converter#getOptions()} returns a non‑null instance that provides
 * {@code trueChar()} and {@code falseChar()} values.
 *
 * The test class resides in the same package as {@code AtomicBooleanConversions} so it
 * can access the package‑private static methods.
 */
public class AtomicBooleanConversionsTest
{
    private Converter converter;
    private ConverterOptions options;

    @Before
    public void setUp()
    {
        // The library supplies a public constructor for ConverterOptions.
        // Using the default constructor gives us the standard true/false characters.
        this.options = new ConverterOptions();
        this.converter = new Converter(options);
    }

    /* ---------------------------------------------------------------------- */
    /*  Byte conversion                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toByte_true_returnsOne()
    {
        AtomicBoolean source = new AtomicBoolean(true);
        Byte result = AtomicBooleanConversions.toByte(source, converter);
        assertEquals(Byte.valueOf((byte) 1), result);
    }

    @Test
    public void toByte_false_returnsZero()
    {
        AtomicBoolean source = new AtomicBoolean(false);
        Byte result = AtomicBooleanConversions.toByte(source, converter);
        assertEquals(Byte.valueOf((byte) 0), result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Short conversion                                                      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toShort_true_returnsOne()
    {
        AtomicBoolean source = new AtomicBoolean(true);
        Short result = AtomicBooleanConversions.toShort(source, converter);
        assertEquals(Short.valueOf((short) 1), result);
    }

    @Test
    public void toShort_false_returnsZero()
    {
        AtomicBoolean source = new AtomicBoolean(false);
        Short result = AtomicBooleanConversions.toShort(source, converter);
        assertEquals(Short.valueOf((short) 0), result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Integer conversion                                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toInt_true_returnsOne()
    {
        AtomicBoolean source = new AtomicBoolean(true);
        Integer result = AtomicBooleanConversions.toInt(source, converter);
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void toInt_false_returnsZero()
    {
        AtomicBoolean source = new AtomicBoolean(false);
        Integer result = AtomicBooleanConversions.toInt(source, converter);
        assertEquals(Integer.valueOf(0), result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Long conversion                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toLong_true_returnsOne()
    {
        AtomicBoolean source = new AtomicBoolean(true);
        Long result = AtomicBooleanConversions.toLong(source, converter);
        assertEquals(Long.valueOf(1L), result);
    }

    @Test
    public void toLong_false_returnsZero()
    {
        AtomicBoolean source = new AtomicBoolean(false);
        Long result = AtomicBooleanConversions.toLong(source, converter);
        assertEquals(Long.valueOf(0L), result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Float conversion                                                      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toFloat_true_returnsOne()
    {
        AtomicBoolean source = new AtomicBoolean(true);
        Float result = AtomicBooleanConversions.toFloat(source, converter);
        assertEquals(Float.valueOf(1.0f), result);
    }

    @Test
    public void toFloat_false_returnsZero()
    {
        AtomicBoolean source = new AtomicBoolean(false);
        Float result = AtomicBooleanConversions.toFloat(source, converter);
        assertEquals(Float.valueOf(0.0f), result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Double conversion                                                     */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toDouble_true_returnsOne()
    {
        AtomicBoolean source = new AtomicBoolean(true);
        Double result = AtomicBooleanConversions.toDouble(source, converter);
        assertEquals(Double.valueOf(1.0d), result);
    }

    @Test
    public void toDouble_false_returnsZero()
    {
        AtomicBoolean source = new AtomicBoolean(false);
        Double result = AtomicBooleanConversions.toDouble(source, converter);
        assertEquals(Double.valueOf(0.0d), result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Primitive boolean conversion                                          */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toBoolean_true_returnsTrue()
    {
        AtomicBoolean source = new AtomicBoolean(true);
        boolean result = AtomicBooleanConversions.toBoolean(source, converter);
        assertTrue(result);
    }

    @Test
    public void toBoolean_false_returnsFalse()
    {
        AtomicBoolean source = new AtomicBoolean(false);
        boolean result = AtomicBooleanConversions.toBoolean(source, converter);
        assertFalse(result);
    }

    /* ---------------------------------------------------------------------- */
    /*  AtomicBoolean conversion                                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toAtomicBoolean_true_returnsNewTrue()
    {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, converter);
        assertNotSame(source, result);
        assertTrue(result.get());
    }

    @Test
    public void toAtomicBoolean_false_returnsNewFalse()
    {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, converter);
        assertNotSame(source, result);
        assertFalse(result.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  AtomicInteger conversion                                              */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toAtomicInteger_true_returnsOne()
    {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, converter);
        assertEquals(1, result.get());
    }

    @Test
    public void toAtomicInteger_false_returnsZero()
    {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, converter);
        assertEquals(0, result.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  AtomicLong conversion                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toAtomicLong_true_returnsOne()
    {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, converter);
        assertEquals(1L, result.get());
    }

    @Test
    public void toAtomicLong_false_returnsZero()
    {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, converter);
        assertEquals(0L, result.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  Character conversion                                                  */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toCharacter_true_usesTrueChar()
    {
        AtomicBoolean source = new AtomicBoolean(true);
        char expected = options.trueChar();
        char result = AtomicBooleanConversions.toCharacter(source, converter);
        assertEquals(expected, result);
    }

    @Test
    public void toCharacter_false_usesFalseChar()
    {
        AtomicBoolean source = new AtomicBoolean(false);
        char expected = options.falseChar();
        char result = AtomicBooleanConversions.toCharacter(source, converter);
        assertEquals(expected, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  BigDecimal conversion                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toBigDecimal_true_returnsOne()
    {
        AtomicBoolean source = new AtomicBoolean(true);
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, converter);
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    public void toBigDecimal_false_returnsZero()
    {
        AtomicBoolean source = new AtomicBoolean(false);
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, converter);
        assertEquals(BigDecimal.ZERO, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  BigInteger conversion                                                 */
    /* ---------------------------------------------------------------------- */
    @Test
    public void toBigInteger_true_returnsOne()
    {
        AtomicBoolean source = new AtomicBoolean(true);
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, converter);
        assertEquals(BigInteger.ONE, result);
    }

    @Test
    public void toBigInteger_false_returnsZero()
    {
        AtomicBoolean source = new AtomicBoolean(false);
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, converter);
        assertEquals(BigInteger.ZERO, result);
    }
}
