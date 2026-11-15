package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Unit tests for {@link BooleanConversions}.
 * <p>
 * The class under test has package‑private static conversion methods that accept a {@link Boolean}
 * value and a {@link Converter}.  The tests are placed in the same package so the methods are
 * directly accessible.  Each conversion is exercised with both {@code true} and {@code false}
 * inputs, as well as with {@code null} and with an object of the wrong type to verify that the
 * expected {@link NullPointerException} or {@link ClassCastException} is thrown.
 * </p>
 */
public class BooleanConversionsTest {

    // ----------------------------------------------------------------------
    // Helper to create a default Converter (uses default ConverterOptions)
    // ----------------------------------------------------------------------
    private static Converter defaultConverter() {
        // The library provides a public constructor that accepts a ConverterOptions instance.
        // Using the no‑arg constructor of ConverterOptions yields the default true/false chars.
        ConverterOptions options = new ConverterOptions(); // assumes public default ctor
        return new Converter(options);
    }

    // ----------------------------------------------------------------------
    // toByte
    // ----------------------------------------------------------------------
    @Test
    public void testToByteTrue() {
        Byte result = BooleanConversions.toByte(Boolean.TRUE, defaultConverter());
        assertEquals(Byte.valueOf((byte) 1), result);
    }

    @Test
    public void testToByteFalse() {
        Byte result = BooleanConversions.toByte(Boolean.FALSE, defaultConverter());
        assertEquals(Byte.valueOf((byte) 0), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteNull() {
        BooleanConversions.toByte(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToByteWrongType() {
        BooleanConversions.toByte("not a boolean", defaultConverter());
    }

    // ----------------------------------------------------------------------
    // toShort
    // ----------------------------------------------------------------------
    @Test
    public void testToShortTrue() {
        Short result = BooleanConversions.toShort(Boolean.TRUE, defaultConverter());
        assertEquals(Short.valueOf((short) 1), result);
    }

    @Test
    public void testToShortFalse() {
        Short result = BooleanConversions.toShort(Boolean.FALSE, defaultConverter());
        assertEquals(Short.valueOf((short) 0), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToShortNull() {
        BooleanConversions.toShort(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToShortWrongType() {
        BooleanConversions.toShort(123, defaultConverter());
    }

    // ----------------------------------------------------------------------
    // toInt
    // ----------------------------------------------------------------------
    @Test
    public void testToIntTrue() {
        Integer result = BooleanConversions.toInt(Boolean.TRUE, defaultConverter());
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testToIntFalse() {
        Integer result = BooleanConversions.toInt(Boolean.FALSE, defaultConverter());
        assertEquals(Integer.valueOf(0), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToIntNull() {
        BooleanConversions.toInt(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToIntWrongType() {
        BooleanConversions.toInt(new Object(), defaultConverter());
    }

    // ----------------------------------------------------------------------
    // toAtomicInteger
    // ----------------------------------------------------------------------
    @Test
    public void testToAtomicIntegerTrue() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.TRUE, defaultConverter());
        assertEquals(1, ai.get());
    }

    @Test
    public void testToAtomicIntegerFalse() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.FALSE, defaultConverter());
        assertEquals(0, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNull() {
        BooleanConversions.toAtomicInteger(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerWrongType() {
        BooleanConversions.toAtomicInteger(42L, defaultConverter());
    }

    // ----------------------------------------------------------------------
    // toAtomicLong
    // ----------------------------------------------------------------------
    @Test
    public void testToAtomicLongTrue() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.TRUE, defaultConverter());
        assertEquals(1L, al.get());
    }

    @Test
    public void testToAtomicLongFalse() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.FALSE, defaultConverter());
        assertEquals(0L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNull() {
        BooleanConversions.toAtomicLong(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLongWrongType() {
        BooleanConversions.toAtomicLong("true", defaultConverter());
    }

    // ----------------------------------------------------------------------
    // toAtomicBoolean
    // ----------------------------------------------------------------------
    @Test
    public void testToAtomicBooleanTrue() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.TRUE, defaultConverter());
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.FALSE, defaultConverter());
        assertFalse(ab.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBooleanNull() {
        BooleanConversions.toAtomicBoolean(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBooleanWrongType() {
        BooleanConversions.toAtomicBoolean(0, defaultConverter());
    }

    // ----------------------------------------------------------------------
    // toLong
    // ----------------------------------------------------------------------
    @Test
    public void testToLongTrue() {
        Long result = BooleanConversions.toLong(Boolean.TRUE, defaultConverter());
        assertEquals(Long.valueOf(1L), result);
    }

    @Test
    public void testToLongFalse() {
        Long result = BooleanConversions.toLong(Boolean.FALSE, defaultConverter());
        assertEquals(Long.valueOf(0L), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToLongNull() {
        BooleanConversions.toLong(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToLongWrongType() {
        BooleanConversions.toLong(Boolean.TRUE.toString(), defaultConverter());
    }

    // ----------------------------------------------------------------------
    // toBigDecimal
    // ----------------------------------------------------------------------
    @Test
    public void testToBigDecimalTrue() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.TRUE, defaultConverter());
        assertEquals(BigDecimal.ONE, bd);
    }

    @Test
    public void testToBigDecimalFalse() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.FALSE, defaultConverter());
        assertEquals(BigDecimal.ZERO, bd);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalNull() {
        BooleanConversions.toBigDecimal(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimalWrongType() {
        BooleanConversions.toBigDecimal(1, defaultConverter());
    }

    // ----------------------------------------------------------------------
    // toBigInteger
    // ----------------------------------------------------------------------
    @Test
    public void testToBigIntegerTrue() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.TRUE, defaultConverter());
        assertEquals(BigInteger.ONE, bi);
    }

    @Test
    public void testToBigIntegerFalse() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.FALSE, defaultConverter());
        assertEquals(BigInteger.ZERO, bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNull() {
        BooleanConversions.toBigInteger(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToBigIntegerWrongType() {
        BooleanConversions.toBigInteger(new Object(), defaultConverter());
    }

    // ----------------------------------------------------------------------
    // toFloat
    // ----------------------------------------------------------------------
    @Test
    public void testToFloatTrue() {
        Float f = BooleanConversions.toFloat(Boolean.TRUE, defaultConverter());
        assertEquals(Float.valueOf(1.0f), f);
    }

    @Test
    public void testToFloatFalse() {
        Float f = BooleanConversions.toFloat(Boolean.FALSE, defaultConverter());
        assertEquals(Float.valueOf(0.0f), f);
    }

    @Test(expected = NullPointerException.class)
    public void testToFloatNull() {
        BooleanConversions.toFloat(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToFloatWrongType() {
        BooleanConversions.toFloat(Boolean.FALSE.toString(), defaultConverter());
    }

    // ----------------------------------------------------------------------
    // toDouble
    // ----------------------------------------------------------------------
    @Test
    public void testToDoubleTrue() {
        Double d = BooleanConversions.toDouble(Boolean.TRUE, defaultConverter());
        assertEquals(Double.valueOf(1.0d), d);
    }

    @Test
    public void testToDoubleFalse() {
        Double d = BooleanConversions.toDouble(Boolean.FALSE, defaultConverter());
        assertEquals(Double.valueOf(0.0d), d);
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleNull() {
        BooleanConversions.toDouble(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToDoubleWrongType() {
        BooleanConversions.toDouble(0, defaultConverter());
    }

    // ----------------------------------------------------------------------
    // toCharacter
    // ----------------------------------------------------------------------
    @Test
    public void testToCharacterTrueUsesTrueChar() {
        Converter converter = defaultConverter();
        ConverterOptions opts = converter.getOptions();
        char expected = opts.trueChar();
        char result = BooleanConversions.toCharacter(Boolean.TRUE, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToCharacterFalseUsesFalseChar() {
        Converter converter = defaultConverter();
        ConverterOptions opts = converter.getOptions();
        char expected = opts.falseChar();
        char result = BooleanConversions.toCharacter(Boolean.FALSE, converter);
        assertEquals(expected, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacterNull() {
        BooleanConversions.toCharacter(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacterWrongType() {
        BooleanConversions.toCharacter("true", defaultConverter());
    }
}
