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
 * Comprehensive JUnit‑4 test suite for {@link BooleanConversions}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Normal conversion for {@code true} and {@code false} values.</li>
 *   <li>Edge cases – {@code null} input and input of an incompatible type.</li>
 *   <li>Verification of the {@link ConverterOptions} character conversion.</li>
 *   <li>Exception expectations for illegal arguments.</li>
 * </ul>
 */
public class BooleanConversionsTest {

    private Converter converter;

    /**
     * Creates a {@link Converter} with default {@link ConverterOptions}.
     * The real library provides a public no‑arg constructor for {@code ConverterOptions};
     * its defaults are used for the character conversion tests.
     */
    @Before
    public void setUp() {
        ConverterOptions options = new ConverterOptions();   // assumes public default ctor
        converter = new Converter(options);
    }

    /* -------------------------------------------------------
     *  Helper to invoke the private static methods via reflection.
     *  All conversion methods are package‑private static, so we can
     *  call them directly from the same package (the test class is
     *  placed in the same package).
     * ------------------------------------------------------- */

    // -----------------------------------------------------------------
    //  Boolean → Byte
    // -----------------------------------------------------------------
    @Test
    public void testToByteTrue() {
        assertEquals(Byte.valueOf((byte)1), BooleanConversions.toByte(Boolean.TRUE, converter));
    }

    @Test
    public void testToByteFalse() {
        assertEquals(Byte.valueOf((byte)0), BooleanConversions.toByte(Boolean.FALSE, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToByteNull() {
        BooleanConversions.toByte(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteWrongType() {
        BooleanConversions.toByte("not a boolean", converter);
    }

    // -----------------------------------------------------------------
    //  Boolean → Short
    // -----------------------------------------------------------------
    @Test
    public void testToShortTrue() {
        assertEquals(Short.valueOf((short)1), BooleanConversions.toShort(Boolean.TRUE, converter));
    }

    @Test
    public void testToShortFalse() {
        assertEquals(Short.valueOf((short)0), BooleanConversions.toShort(Boolean.FALSE, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToShortNull() {
        BooleanConversions.toShort(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToShortWrongType() {
        BooleanConversions.toShort(123, converter);
    }

    // -----------------------------------------------------------------
    //  Boolean → Integer
    // -----------------------------------------------------------------
    @Test
    public void testToIntTrue() {
        assertEquals(Integer.valueOf(1), BooleanConversions.toInt(Boolean.TRUE, converter));
    }

    @Test
    public void testToIntFalse() {
        assertEquals(Integer.valueOf(0), BooleanConversions.toInt(Boolean.FALSE, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToIntNull() {
        BooleanConversions.toInt(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToIntWrongType() {
        BooleanConversions.toInt(new Object(), converter);
    }

    // -----------------------------------------------------------------
    //  Boolean → AtomicInteger
    // -----------------------------------------------------------------
    @Test
    public void testToAtomicIntegerTrue() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.TRUE, converter);
        assertEquals(1, ai.get());
    }

    @Test
    public void testToAtomicIntegerFalse() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.FALSE, converter);
        assertEquals(0, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNull() {
        BooleanConversions.toAtomicInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerWrongType() {
        BooleanConversions.toAtomicInteger(5L, converter);
    }

    // -----------------------------------------------------------------
    //  Boolean → AtomicLong
    // -----------------------------------------------------------------
    @Test
    public void testToAtomicLongTrue() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.TRUE, converter);
        assertEquals(1L, al.get());
    }

    @Test
    public void testToAtomicLongFalse() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.FALSE, converter);
        assertEquals(0L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNull() {
        BooleanConversions.toAtomicLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLongWrongType() {
        BooleanConversions.toAtomicLong("true", converter);
    }

    // -----------------------------------------------------------------
    //  Boolean → AtomicBoolean
    // -----------------------------------------------------------------
    @Test
    public void testToAtomicBooleanTrue() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.TRUE, converter);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.FALSE, converter);
        assertFalse(ab.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBooleanNull() {
        BooleanConversions.toAtomicBoolean(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBooleanWrongType() {
        BooleanConversions.toAtomicBoolean(0, converter);
    }

    // -----------------------------------------------------------------
    //  Boolean → Long
    // -----------------------------------------------------------------
    @Test
    public void testToLongTrue() {
        assertEquals(Long.valueOf(1L), BooleanConversions.toLong(Boolean.TRUE, converter));
    }

    @Test
    public void testToLongFalse() {
        assertEquals(Long.valueOf(0L), BooleanConversions.toLong(Boolean.FALSE, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToLongNull() {
        BooleanConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLongWrongType() {
        BooleanConversions.toLong(Boolean.TRUE.toString(), converter);
    }

    // -----------------------------------------------------------------
    //  Boolean → BigDecimal
    // -----------------------------------------------------------------
    @Test
    public void testToBigDecimalTrue() {
        assertEquals(BigDecimal.ONE, BooleanConversions.toBigDecimal(Boolean.TRUE, converter));
    }

    @Test
    public void testToBigDecimalFalse() {
        assertEquals(BigDecimal.ZERO, BooleanConversions.toBigDecimal(Boolean.FALSE, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalNull() {
        BooleanConversions.toBigDecimal(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimalWrongType() {
        BooleanConversions.toBigDecimal(1, converter);
    }

    // -----------------------------------------------------------------
    //  Boolean → BigInteger
    // -----------------------------------------------------------------
    @Test
    public void testToBigIntegerTrue() {
        assertEquals(BigInteger.ONE, BooleanConversions.toBigInteger(Boolean.TRUE, converter));
    }

    @Test
    public void testToBigIntegerFalse() {
        assertEquals(BigInteger.ZERO, BooleanConversions.toBigInteger(Boolean.FALSE, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNull() {
        BooleanConversions.toBigInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigIntegerWrongType() {
        BooleanConversions.toBigInteger(new Object(), converter);
    }

    // -----------------------------------------------------------------
    //  Boolean → Float
    // -----------------------------------------------------------------
    @Test
    public void testToFloatTrue() {
        assertEquals(Float.valueOf(1.0f), BooleanConversions.toFloat(Boolean.TRUE, converter));
    }

    @Test
    public void testToFloatFalse() {
        assertEquals(Float.valueOf(0.0f), BooleanConversions.toFloat(Boolean.FALSE, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToFloatNull() {
        BooleanConversions.toFloat(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToFloatWrongType() {
        BooleanConversions.toFloat(Boolean.FALSE.toString(), converter);
    }

    // -----------------------------------------------------------------
    //  Boolean → Double
    // -----------------------------------------------------------------
    @Test
    public void testToDoubleTrue() {
        assertEquals(Double.valueOf(1.0d), BooleanConversions.toDouble(Boolean.TRUE, converter));
    }

    @Test
    public void testToDoubleFalse() {
        assertEquals(Double.valueOf(0.0d), BooleanConversions.toDouble(Boolean.FALSE, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleNull() {
        BooleanConversions.toDouble(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDoubleWrongType() {
        BooleanConversions.toDouble(0, converter);
    }

    // -----------------------------------------------------------------
    //  Boolean → char (via ConverterOptions)
    // -----------------------------------------------------------------
    @Test
    public void testToCharacterTrueUsesTrueChar() {
        char expected = converter.getOptions().trueChar();
        assertEquals(expected, BooleanConversions.toCharacter(Boolean.TRUE, converter));
    }

    @Test
    public void testToCharacterFalseUsesFalseChar() {
        char expected = converter.getOptions().falseChar();
        assertEquals(expected, BooleanConversions.toCharacter(Boolean.FALSE, converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacterNull() {
        BooleanConversions.toCharacter(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacterWrongType() {
        BooleanConversions.toCharacter(123, converter);
    }
}
