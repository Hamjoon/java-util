package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link BooleanConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Conversion of {@code Boolean.TRUE} and {@code Boolean.FALSE} to every supported numeric type.</li>
 *   <li>Conversion to the various {@code Atomic*} wrappers.</li>
 *   <li>Conversion to {@link java.math.BigDecimal} and {@link java.math.BigInteger}.</li>
 *   <li>Conversion to {@code char} using the {@link ConverterOptions} supplied by a real {@link Converter} instance.</li>
 *   <li>Behaviour when {@code null} is supplied – a {@link NullPointerException} is expected because the
 *       implementation casts the argument directly to {@code Boolean}.</li>
 * </ul>
 */
public class BooleanConversionsTest {

    /* ----------------------------------------------------------------------
     * Helper – a Converter with default options.
     * ---------------------------------------------------------------------- */
    private static Converter defaultConverter() {
        // The library provides a public constructor that takes a ConverterOptions instance.
        // The default constructor of ConverterOptions yields the standard true/false characters.
        return new Converter(new ConverterOptions());
    }

    /* ----------------------------------------------------------------------
     * Positive / negative numeric conversions
     * ---------------------------------------------------------------------- */

    @Test
    public void testToByte() {
        Converter c = defaultConverter();
        assertEquals(Byte.valueOf((byte) 1), BooleanConversions.toByte(Boolean.TRUE, c));
        assertEquals(Byte.valueOf((byte) 0), BooleanConversions.toByte(Boolean.FALSE, c));
    }

    @Test
    public void testToShort() {
        Converter c = defaultConverter();
        assertEquals(Short.valueOf((short) 1), BooleanConversions.toShort(Boolean.TRUE, c));
        assertEquals(Short.valueOf((short) 0), BooleanConversions.toShort(Boolean.FALSE, c));
    }

    @Test
    public void testToInt() {
        Converter c = defaultConverter();
        assertEquals(Integer.valueOf(1), BooleanConversions.toInt(Boolean.TRUE, c));
        assertEquals(Integer.valueOf(0), BooleanConversions.toInt(Boolean.FALSE, c));
    }

    @Test
    public void testToLong() {
        Converter c = defaultConverter();
        assertEquals(Long.valueOf(1L), BooleanConversions.toLong(Boolean.TRUE, c));
        assertEquals(Long.valueOf(0L), BooleanConversions.toLong(Boolean.FALSE, c));
    }

    @Test
    public void testToFloat() {
        Converter c = defaultConverter();
        assertEquals(Float.valueOf(1.0f), BooleanConversions.toFloat(Boolean.TRUE, c));
        assertEquals(Float.valueOf(0.0f), BooleanConversions.toFloat(Boolean.FALSE, c));
    }

    @Test
    public void testToDouble() {
        Converter c = defaultConverter();
        assertEquals(Double.valueOf(1.0d), BooleanConversions.toDouble(Boolean.TRUE, c));
        assertEquals(Double.valueOf(0.0d), BooleanConversions.toDouble(Boolean.FALSE, c));
    }

    @Test
    public void testToBigDecimal() {
        Converter c = defaultConverter();
        assertEquals(BigDecimal.ONE, BooleanConversions.toBigDecimal(Boolean.TRUE, c));
        assertEquals(BigDecimal.ZERO, BooleanConversions.toBigDecimal(Boolean.FALSE, c));
    }

    @Test
    public void testToBigInteger() {
        Converter c = defaultConverter();
        assertEquals(BigInteger.ONE, BooleanConversions.toBigInteger(Boolean.TRUE, c));
        assertEquals(BigInteger.ZERO, BooleanConversions.toBigInteger(Boolean.FALSE, c));
    }

    /* ----------------------------------------------------------------------
     * Atomic wrapper conversions
     * ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicInteger() {
        Converter c = defaultConverter();
        AtomicInteger aiTrue = BooleanConversions.toAtomicInteger(Boolean.TRUE, c);
        AtomicInteger aiFalse = BooleanConversions.toAtomicInteger(Boolean.FALSE, c);
        assertEquals(1, aiTrue.get());
        assertEquals(0, aiFalse.get());
    }

    @Test
    public void testToAtomicLong() {
        Converter c = defaultConverter();
        AtomicLong alTrue = BooleanConversions.toAtomicLong(Boolean.TRUE, c);
        AtomicLong alFalse = BooleanConversions.toAtomicLong(Boolean.FALSE, c);
        assertEquals(1L, alTrue.get());
        assertEquals(0L, alFalse.get());
    }

    @Test
    public void testToAtomicBoolean() {
        Converter c = defaultConverter();
        AtomicBoolean abTrue = BooleanConversions.toAtomicBoolean(Boolean.TRUE, c);
        AtomicBoolean abFalse = BooleanConversions.toAtomicBoolean(Boolean.FALSE, c);
        assertTrue(abTrue.get());
        assertFalse(abFalse.get());
    }

    /* ----------------------------------------------------------------------
     * Character conversion – uses ConverterOptions
     * ---------------------------------------------------------------------- */

    @Test
    public void testToCharacterUsesConverterOptions() {
        ConverterOptions opts = new ConverterOptions(); // default options
        Converter conv = new Converter(opts);

        char trueChar = BooleanConversions.toCharacter(Boolean.TRUE, conv);
        char falseChar = BooleanConversions.toCharacter(Boolean.FALSE, conv);

        assertEquals(opts.trueChar(), trueChar);
        assertEquals(opts.falseChar(), falseChar);
    }

    /* ----------------------------------------------------------------------
     * Null handling – the implementation casts directly to Boolean, so a NPE is expected.
     * ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToByteWithNull() {
        BooleanConversions.toByte(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToShortWithNull() {
        BooleanConversions.toShort(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToIntWithNull() {
        BooleanConversions.toInt(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerWithNull() {
        BooleanConversions.toAtomicInteger(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongWithNull() {
        BooleanConversions.toAtomicLong(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBooleanWithNull() {
        BooleanConversions.toAtomicBoolean(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToLongWithNull() {
        BooleanConversions.toLong(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalWithNull() {
        BooleanConversions.toBigDecimal(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerWithNull() {
        BooleanConversions.toBigInteger(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToFloatWithNull() {
        BooleanConversions.toFloat(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleWithNull() {
        BooleanConversions.toDouble(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacterWithNull() {
        BooleanConversions.toCharacter(null, defaultConverter());
    }
}
