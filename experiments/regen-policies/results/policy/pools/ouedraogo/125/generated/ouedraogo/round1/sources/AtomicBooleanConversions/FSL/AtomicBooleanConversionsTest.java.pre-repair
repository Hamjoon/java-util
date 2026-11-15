package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link AtomicBooleanConversions}.
 *
 * The tests cover each conversion method for both {@code true} and {@code false}
 * {@link AtomicBoolean} values, as well as edge‑cases such as object identity
 * for the {@code toAtomicBoolean} conversion and the character conversion that
 * depends on {@link ConverterOptions}.
 */
public class AtomicBooleanConversionsTest {

    private Converter trueConverter;
    private Converter falseConverter;

    @Before
    public void setUp() {
        // The real ConverterOptions class provides default true/false characters.
        // For the purpose of these tests we rely on its default behaviour.
        // Two Converter instances are created – they are functionally identical,
        // but having separate variables makes the intent of the test clearer.
        trueConverter = new Converter(new ConverterOptions());
        falseConverter = new Converter(new ConverterOptions());
    }

    // ----------------------------------------------------------------------
    // Primitive numeric conversions
    // ----------------------------------------------------------------------
    @Test
    public void testToByte_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        Byte result = AtomicBooleanConversions.toByte(source, trueConverter);
        assertEquals(CommonValues.BYTE_ONE, result);
    }

    @Test
    public void testToByte_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        Byte result = AtomicBooleanConversions.toByte(source, falseConverter);
        assertEquals(CommonValues.BYTE_ZERO, result);
    }

    @Test
    public void testToShort_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        Short result = AtomicBooleanConversions.toShort(source, trueConverter);
        assertEquals(CommonValues.SHORT_ONE, result);
    }

    @Test
    public void testToShort_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        Short result = AtomicBooleanConversions.toShort(source, falseConverter);
        assertEquals(CommonValues.SHORT_ZERO, result);
    }

    @Test
    public void testToInt_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        Integer result = AtomicBooleanConversions.toInt(source, trueConverter);
        assertEquals(CommonValues.INTEGER_ONE, result);
    }

    @Test
    public void testToInt_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        Integer result = AtomicBooleanConversions.toInt(source, falseConverter);
        assertEquals(CommonValues.INTEGER_ZERO, result);
    }

    @Test
    public void testToLong_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        Long result = AtomicBooleanConversions.toLong(source, trueConverter);
        assertEquals(CommonValues.LONG_ONE, result);
    }

    @Test
    public void testToLong_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        Long result = AtomicBooleanConversions.toLong(source, falseConverter);
        assertEquals(CommonValues.LONG_ZERO, result);
    }

    @Test
    public void testToFloat_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        Float result = AtomicBooleanConversions.toFloat(source, trueConverter);
        assertEquals(CommonValues.FLOAT_ONE, result);
    }

    @Test
    public void testToFloat_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        Float result = AtomicBooleanConversions.toFloat(source, falseConverter);
        assertEquals(CommonValues.FLOAT_ZERO, result);
    }

    @Test
    public void testToDouble_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        Double result = AtomicBooleanConversions.toDouble(source, trueConverter);
        assertEquals(CommonValues.DOUBLE_ONE, result);
    }

    @Test
    public void testToDouble_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        Double result = AtomicBooleanConversions.toDouble(source, falseConverter);
        assertEquals(CommonValues.DOUBLE_ZERO, result);
    }

    // ----------------------------------------------------------------------
    // Boolean conversion
    // ----------------------------------------------------------------------
    @Test
    public void testToBoolean_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        assertTrue(AtomicBooleanConversions.toBoolean(source, trueConverter));
    }

    @Test
    public void testToBoolean_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        assertFalse(AtomicBooleanConversions.toBoolean(source, falseConverter));
    }

    // ----------------------------------------------------------------------
    // Atomic type conversions
    // ----------------------------------------------------------------------
    @Test
    public void testToAtomicBoolean_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, trueConverter);
        assertNotSame(source, result);
        assertTrue(result.get());
    }

    @Test
    public void testToAtomicBoolean_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, falseConverter);
        assertNotSame(source, result);
        assertFalse(result.get());
    }

    @Test
    public void testToAtomicInteger_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, trueConverter);
        assertEquals(1, result.get());
    }

    @Test
    public void testToAtomicInteger_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, falseConverter);
        assertEquals(0, result.get());
    }

    @Test
    public void testToAtomicLong_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, trueConverter);
        assertEquals(1L, result.get());
    }

    @Test
    public void testToAtomicLong_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, falseConverter);
        assertEquals(0L, result.get());
    }

    // ----------------------------------------------------------------------
    // Character conversion – depends on ConverterOptions
    // ----------------------------------------------------------------------
    @Test
    public void testToCharacter_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        ConverterOptions opts = trueConverter.getOptions();
        char expected = opts.trueChar();

        char result = AtomicBooleanConversions.toCharacter(source, trueConverter);
        assertEquals(expected, result);
    }

    @Test
    public void testToCharacter_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        ConverterOptions opts = falseConverter.getOptions();
        char expected = opts.falseChar();

        char result = AtomicBooleanConversions.toCharacter(source, falseConverter);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    // BigDecimal / BigInteger conversion
    // ----------------------------------------------------------------------
    @Test
    public void testToBigDecimal_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, trueConverter);
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    public void testToBigDecimal_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, falseConverter);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testToBigInteger_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, trueConverter);
        assertEquals(BigInteger.ONE, result);
    }

    @Test
    public void testToBigInteger_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, falseConverter);
        assertEquals(BigInteger.ZERO, result);
    }
}
