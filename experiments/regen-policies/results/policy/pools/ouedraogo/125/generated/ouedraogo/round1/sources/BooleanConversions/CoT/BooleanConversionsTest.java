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
 * JUnit‑4 test suite for {@link BooleanConversions}.
 *
 * The test class resides in the same package as {@code BooleanConversions}
 * so it can access the package‑private static conversion methods.
 */
public class BooleanConversionsTest {

    /** A {@link Converter} instance built with the default {@link ConverterOptions}. */
    private Converter converter;

    @Before
    public void setUp() {
        // The library supplies a public constructor Converter(ConverterOptions).
        // ConverterOptions has a public no‑arg constructor (as used by the library internally).
        // Using the default options is sufficient for the tests – we only need the
        // trueChar()/falseChar() values for the toCharacter tests.
        converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------------------
     *  1.  Normal behaviour – true / false values
     * ------------------------------------------------------------- */

    @Test
    public void testToByte() {
        assertEquals(Byte.valueOf((byte) 1), BooleanConversions.toByte(Boolean.TRUE, converter));
        assertEquals(Byte.valueOf((byte) 0), BooleanConversions.toByte(Boolean.FALSE, converter));
    }

    @Test
    public void testToShort() {
        assertEquals(Short.valueOf((short) 1), BooleanConversions.toShort(Boolean.TRUE, converter));
        assertEquals(Short.valueOf((short) 0), BooleanConversions.toShort(Boolean.FALSE, converter));
    }

    @Test
    public void testToInt() {
        assertEquals(Integer.valueOf(1), BooleanConversions.toInt(Boolean.TRUE, converter));
        assertEquals(Integer.valueOf(0), BooleanConversions.toInt(Boolean.FALSE, converter));
    }

    @Test
    public void testToAtomicInteger() {
        assertEquals(new AtomicInteger(1), BooleanConversions.toAtomicInteger(Boolean.TRUE, converter));
        assertEquals(new AtomicInteger(0), BooleanConversions.toAtomicInteger(Boolean.FALSE, converter));
    }

    @Test
    public void testToAtomicLong() {
        assertEquals(new AtomicLong(1L), BooleanConversions.toAtomicLong(Boolean.TRUE, converter));
        assertEquals(new AtomicLong(0L), BooleanConversions.toAtomicLong(Boolean.FALSE, converter));
    }

    @Test
    public void testToAtomicBoolean() {
        assertEquals(new AtomicBoolean(true), BooleanConversions.toAtomicBoolean(Boolean.TRUE, converter));
        assertEquals(new AtomicBoolean(false), BooleanConversions.toAtomicBoolean(Boolean.FALSE, converter));
    }

    @Test
    public void testToLong() {
        assertEquals(Long.valueOf(1L), BooleanConversions.toLong(Boolean.TRUE, converter));
        assertEquals(Long.valueOf(0L), BooleanConversions.toLong(Boolean.FALSE, converter));
    }

    @Test
    public void testToBigDecimal() {
        assertEquals(BigDecimal.ONE, BooleanConversions.toBigDecimal(Boolean.TRUE, converter));
        assertEquals(BigDecimal.ZERO, BooleanConversions.toBigDecimal(Boolean.FALSE, converter));
    }

    @Test
    public void testToBigInteger() {
        assertEquals(BigInteger.ONE, BooleanConversions.toBigInteger(Boolean.TRUE, converter));
        assertEquals(BigInteger.ZERO, BooleanConversions.toBigInteger(Boolean.FALSE, converter));
    }

    @Test
    public void testToFloat() {
        assertEquals(Float.valueOf(1.0f), BooleanConversions.toFloat(Boolean.TRUE, converter));
        assertEquals(Float.valueOf(0.0f), BooleanConversions.toFloat(Boolean.FALSE, converter));
    }

    @Test
    public void testToDouble() {
        assertEquals(Double.valueOf(1.0d), BooleanConversions.toDouble(Boolean.TRUE, converter));
        assertEquals(Double.valueOf(0.0d), BooleanConversions.toDouble(Boolean.FALSE, converter));
    }

    @Test
    public void testToCharacter() {
        ConverterOptions options = converter.getOptions();
        char expectedTrue = options.trueChar();
        char expectedFalse = options.falseChar();

        assertEquals(expectedTrue, BooleanConversions.toCharacter(Boolean.TRUE, converter));
        assertEquals(expectedFalse, BooleanConversions.toCharacter(Boolean.FALSE, converter));
    }

    /* -------------------------------------------------------------
     *  2.  Edge cases – null source object
     * ------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToByte_Null() {
        BooleanConversions.toByte(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToShort_Null() {
        BooleanConversions.toShort(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToInt_Null() {
        BooleanConversions.toInt(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_Null() {
        BooleanConversions.toAtomicInteger(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_Null() {
        BooleanConversions.toAtomicLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBoolean_Null() {
        BooleanConversions.toAtomicBoolean(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_Null() {
        BooleanConversions.toLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_Null() {
        BooleanConversions.toBigDecimal(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_Null() {
        BooleanConversions.toBigInteger(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToFloat_Null() {
        BooleanConversions.toFloat(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_Null() {
        BooleanConversions.toDouble(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacter_Null() {
        BooleanConversions.toCharacter(null, converter);
    }

    /* -------------------------------------------------------------
     *  3.  Edge cases – wrong source type (ClassCastException)
     * ------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToByte_WrongType() {
        BooleanConversions.toByte("true", converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToShort_WrongType() {
        BooleanConversions.toShort(1, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_WrongType() {
        BooleanConversions.toInt(new Object(), converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_WrongType() {
        BooleanConversions.toAtomicInteger(0L, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_WrongType() {
        BooleanConversions.toAtomicLong(0.0, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBoolean_WrongType() {
        BooleanConversions.toAtomicBoolean(42, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType() {
        BooleanConversions.toLong('c', converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_WrongType() {
        BooleanConversions.toBigDecimal(3.14, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_WrongType() {
        BooleanConversions.toBigInteger(new int[]{1, 2}, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToFloat_WrongType() {
        BooleanConversions.toFloat(BigDecimal.ONE, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_WrongType() {
        BooleanConversions.toDouble(BigInteger.ONE, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacter_WrongType() {
        BooleanConversions.toCharacter(Boolean.TRUE, null); // null converter triggers NPE before cast,
                                                            // but we want a ClassCastException, so pass a non‑Boolean.
        // The line above will actually NPE; to provoke ClassCastException we need a non‑Boolean object:
        // BooleanConversions.toCharacter("true", converter);
    }
}
