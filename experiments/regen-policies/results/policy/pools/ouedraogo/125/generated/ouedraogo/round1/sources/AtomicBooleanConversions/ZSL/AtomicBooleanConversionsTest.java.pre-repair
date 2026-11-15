package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class AtomicBooleanConversionsTest {

    private Converter defaultConverter;
    private ConverterOptions options;

    @Before
    public void setUp() {
        // The library provides a public no‑arg constructor for ConverterOptions.
        // If a different constructor is required, adjust accordingly.
        options = new ConverterOptions();
        defaultConverter = new Converter(options);
    }

    /* -------------------------------------------------------------
     * Helper to obtain an AtomicBoolean with the requested value.
     * ------------------------------------------------------------- */
    private static AtomicBoolean ab(boolean value) {
        return new AtomicBoolean(value);
    }

    /* -------------------------------------------------------------
     * Primitive / wrapper conversions
     * ------------------------------------------------------------- */
    @Test
    public void testToByte_true() {
        Byte result = AtomicBooleanConversions.toByte(ab(true), defaultConverter);
        assertEquals(Byte.valueOf((byte) 1), result);
    }

    @Test
    public void testToByte_false() {
        Byte result = AtomicBooleanConversions.toByte(ab(false), defaultConverter);
        assertEquals(Byte.valueOf((byte) 0), result);
    }

    @Test
    public void testToShort_true() {
        Short result = AtomicBooleanConversions.toShort(ab(true), defaultConverter);
        assertEquals(Short.valueOf((short) 1), result);
    }

    @Test
    public void testToShort_false() {
        Short result = AtomicBooleanConversions.toShort(ab(false), defaultConverter);
        assertEquals(Short.valueOf((short) 0), result);
    }

    @Test
    public void testToInt_true() {
        Integer result = AtomicBooleanConversions.toInt(ab(true), defaultConverter);
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testToInt_false() {
        Integer result = AtomicBooleanConversions.toInt(ab(false), defaultConverter);
        assertEquals(Integer.valueOf(0), result);
    }

    @Test
    public void testToLong_true() {
        Long result = AtomicBooleanConversions.toLong(ab(true), defaultConverter);
        assertEquals(Long.valueOf(1L), result);
    }

    @Test
    public void testToLong_false() {
        Long result = AtomicBooleanConversions.toLong(ab(false), defaultConverter);
        assertEquals(Long.valueOf(0L), result);
    }

    @Test
    public void testToFloat_true() {
        Float result = AtomicBooleanConversions.toFloat(ab(true), defaultConverter);
        assertEquals(Float.valueOf(1.0f), result);
    }

    @Test
    public void testToFloat_false() {
        Float result = AtomicBooleanConversions.toFloat(ab(false), defaultConverter);
        assertEquals(Float.valueOf(0.0f), result);
    }

    @Test
    public void testToDouble_true() {
        Double result = AtomicBooleanConversions.toDouble(ab(true), defaultConverter);
        assertEquals(Double.valueOf(1.0d), result);
    }

    @Test
    public void testToDouble_false() {
        Double result = AtomicBooleanConversions.toDouble(ab(false), defaultConverter);
        assertEquals(Double.valueOf(0.0d), result);
    }

    @Test
    public void testToBoolean_true() {
        assertTrue(AtomicBooleanConversions.toBoolean(ab(true), defaultConverter));
    }

    @Test
    public void testToBoolean_false() {
        assertFalse(AtomicBooleanConversions.toBoolean(ab(false), defaultConverter));
    }

    /* -------------------------------------------------------------
     * Atomic* conversions
     * ------------------------------------------------------------- */
    @Test
    public void testToAtomicBoolean_true() {
        AtomicBoolean src = ab(true);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(src, defaultConverter);
        assertNotSame(src, result);
        assertTrue(result.get());
    }

    @Test
    public void testToAtomicBoolean_false() {
        AtomicBoolean src = ab(false);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(src, defaultConverter);
        assertNotSame(src, result);
        assertFalse(result.get());
    }

    @Test
    public void testToAtomicInteger_true() {
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(ab(true), defaultConverter);
        assertEquals(1, result.get());
    }

    @Test
    public void testToAtomicInteger_false() {
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(ab(false), defaultConverter);
        assertEquals(0, result.get());
    }

    @Test
    public void testToAtomicLong_true() {
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(ab(true), defaultConverter);
        assertEquals(1L, result.get());
    }

    @Test
    public void testToAtomicLong_false() {
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(ab(false), defaultConverter);
        assertEquals(0L, result.get());
    }

    /* -------------------------------------------------------------
     * Character conversion – uses ConverterOptions
     * ------------------------------------------------------------- */
    @Test
    public void testToCharacter_true() {
        char expected = options.trueChar();
        char actual = AtomicBooleanConversions.toCharacter(ab(true), defaultConverter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCharacter_false() {
        char expected = options.falseChar();
        char actual = AtomicBooleanConversions.toCharacter(ab(false), defaultConverter);
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     * BigDecimal / BigInteger conversions
     * ------------------------------------------------------------- */
    @Test
    public void testToBigDecimal_true() {
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(ab(true), defaultConverter);
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    public void testToBigDecimal_false() {
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(ab(false), defaultConverter);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testToBigInteger_true() {
        BigInteger result = AtomicBooleanConversions.toBigInteger(ab(true), defaultConverter);
        assertEquals(BigInteger.ONE, result);
    }

    @Test
    public void testToBigInteger_false() {
        BigInteger result = AtomicBooleanConversions.toBigInteger(ab(false), defaultConverter);
        assertEquals(BigInteger.ZERO, result);
    }

    /* -------------------------------------------------------------
     * Negative / edge cases
     * ------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testInvalidSourceThrowsClassCastException() {
        // Passing an object that is NOT an AtomicBoolean should cause a ClassCastException
        AtomicBooleanConversions.toInt("not an atomic boolean", defaultConverter);
    }

    @Test
    public void testNullConverterIsIgnoredForNonCharacterMethods() {
        // All conversion methods (except toCharacter) ignore the converter argument,
        // therefore passing null must not cause NPE.
        assertEquals(Byte.valueOf((byte) 1), AtomicBooleanConversions.toByte(ab(true), null));
        assertEquals(Integer.valueOf(0), AtomicBooleanConversions.toInt(ab(false), null));
        assertEquals(BigDecimal.ONE, AtomicBooleanConversions.toBigDecimal(ab(true), null));
    }
}
