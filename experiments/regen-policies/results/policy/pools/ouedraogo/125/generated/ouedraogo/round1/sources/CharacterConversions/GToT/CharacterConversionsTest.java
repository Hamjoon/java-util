/****************************************************************************************
 *  JUnit‑4 test suite for com.cedarsoftware.util.convert.CharacterConversions
 *
 *  The tests cover:
 *   • Normal conversion for each static method
 *   • Edge values for the primitive char range (0, 1, 65535)
 *   • Boolean conversion true/false cases
 *   • Null‑input handling (expected NPE)
 *   • Invalid‑type handling (expected ClassCastException)
 *
 *  The test class resides in the same package as CharacterConversions so that the
 *  package‑private static methods are directly accessible without reflection.
 *
 *  No external mocking frameworks are used – only the public API of Converter is
 *  employed (a simple instance created with a null options argument is sufficient
 *  because the Converter is not consulted by the methods under test).
 *
 *  Author:  OpenAI ChatGPT
 *  Date:    2026‑11‑14
 ****************************************************************************************/

package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;
import static org.junit.Assert.*;

public class CharacterConversionsTest {

    /* -------------------------------------------------------------------------
     *  Helper – a dummy Converter instance (the methods under test do not use it)
     * ------------------------------------------------------------------------- */
    private static final Converter DUMMY_CONVERTER = new Converter(null);

    /* -------------------------------------------------------------------------
     *  1. toString()
     * ------------------------------------------------------------------------- */
    @Test
    public void testToString() {
        assertEquals("A", CharacterConversions.toString('A', DUMMY_CONVERTER));
        assertEquals("123", CharacterConversions.toString(123, DUMMY_CONVERTER));
        assertEquals("true", CharacterConversions.toString(true, DUMMY_CONVERTER));
        assertEquals("null", CharacterConversions.toString(null, DUMMY_CONVERTER));
    }

    /* -------------------------------------------------------------------------
     *  2. toBoolean() – true cases
     * ------------------------------------------------------------------------- */
    @Test
    public void testToBooleanTrueCases() {
        assertTrue(CharacterConversions.toBoolean('t', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('T', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('1', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('y', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('Y', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean((char)1, DUMMY_CONVERTER)); // control char 0x01
    }

    /* -------------------------------------------------------------------------
     *  3. toBoolean() – false cases
     * ------------------------------------------------------------------------- */
    @Test
    public void testToBooleanFalseCases() {
        assertFalse(CharacterConversions.toBoolean('0', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('n', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('N', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('f', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('F', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('z', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean((char)0, DUMMY_CONVERTER)); // null char
    }

    /* -------------------------------------------------------------------------
     *  4. Primitive numeric conversions – verify correct widening/narrowing
     * ------------------------------------------------------------------------- */
    @Test
    public void testNumericConversions() {
        char c = 'A';                // 65
        assertEquals((byte)65,   CharacterConversions.toByte(c, DUMMY_CONVERTER));
        assertEquals((short)65,  CharacterConversions.toShort(c, DUMMY_CONVERTER));
        assertEquals(65,          CharacterConversions.toInt(c, DUMMY_CONVERTER));
        assertEquals(65L,         CharacterConversions.toLong(c, DUMMY_CONVERTER));
        assertEquals(65f,         CharacterConversions.toFloat(c, DUMMY_CONVERTER), 0.0f);
        assertEquals(65d,         CharacterConversions.toDouble(c, DUMMY_CONVERTER), 0.0d);
    }

    @Test
    public void testNumericEdgeValues() {
        // Minimum char value
        char min = (char)0;
        assertEquals((byte)0,   CharacterConversions.toByte(min, DUMMY_CONVERTER));
        assertEquals((short)0,  CharacterConversions.toShort(min, DUMMY_CONVERTER));
        assertEquals(0,          CharacterConversions.toInt(min, DUMMY_CONVERTER));
        assertEquals(0L,         CharacterConversions.toLong(min, DUMMY_CONVERTER));
        assertEquals(0f,         CharacterConversions.toFloat(min, DUMMY_CONVERTER), 0.0f);
        assertEquals(0d,         CharacterConversions.toDouble(min, DUMMY_CONVERTER), 0.0d);

        // Maximum char value (65535)
        char max = Character.MAX_VALUE; // '\uffff'
        assertEquals((byte)-1,   CharacterConversions.toByte(max, DUMMY_CONVERTER)); // overflow
        assertEquals((short)-1,  CharacterConversions.toShort(max, DUMMY_CONVERTER)); // overflow
        assertEquals(65535,      CharacterConversions.toInt(max, DUMMY_CONVERTER));
        assertEquals(65535L,     CharacterConversions.toLong(max, DUMMY_CONVERTER));
        assertEquals(65535f,     CharacterConversions.toFloat(max, DUMMY_CONVERTER), 0.0f);
        assertEquals(65535d,     CharacterConversions.toDouble(max, DUMMY_CONVERTER), 0.0d);
    }

    /* -------------------------------------------------------------------------
     *  5. Atomic conversions
     * ------------------------------------------------------------------------- */
    @Test
    public void testAtomicConversions() {
        char c = 'B'; // 66
        AtomicInteger ai = CharacterConversions.toAtomicInteger(c, DUMMY_CONVERTER);
        assertEquals(66, ai.get());

        AtomicLong al = CharacterConversions.toAtomicLong(c, DUMMY_CONVERTER);
        assertEquals(66L, al.get());

        AtomicBoolean abTrue = CharacterConversions.toAtomicBoolean('t', DUMMY_CONVERTER);
        assertTrue(abTrue.get());

        AtomicBoolean abFalse = CharacterConversions.toAtomicBoolean('0', DUMMY_CONVERTER);
        assertFalse(abFalse.get());
    }

    /* -------------------------------------------------------------------------
     *  6. BigInteger / BigDecimal conversions
     * ------------------------------------------------------------------------- */
    @Test
    public void testBigIntegerAndBigDecimal() {
        char c = 'C'; // 67
        BigInteger bi = CharacterConversions.toBigInteger(c, DUMMY_CONVERTER);
        assertEquals(BigInteger.valueOf(67), bi);

        BigDecimal bd = CharacterConversions.toBigDecimal(c, DUMMY_CONVERTER);
        assertEquals(BigDecimal.valueOf(67), bd);
    }

    /* -------------------------------------------------------------------------
     *  7. Null input – all methods should throw NullPointerException when 'from' is null
     * ------------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testToBoolean_NullInput() {
        CharacterConversions.toBoolean(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToByte_NullInput() {
        CharacterConversions.toByte(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToShort_NullInput() {
        CharacterConversions.toShort(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToInt_NullInput() {
        CharacterConversions.toInt(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        CharacterConversions.toLong(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToFloat_NullInput() {
        CharacterConversions.toFloat(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput() {
        CharacterConversions.toDouble(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullInput() {
        CharacterConversions.toAtomicInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput() {
        CharacterConversions.toAtomicLong(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBoolean_NullInput() {
        CharacterConversions.toAtomicBoolean(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        CharacterConversions.toBigInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        CharacterConversions.toBigDecimal(null, DUMMY_CONVERTER);
    }

    /* -------------------------------------------------------------------------
     *  8. Invalid type – passing an object that is not a Character should cause ClassCastException
     * ------------------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testToBoolean_InvalidType() {
        CharacterConversions.toBoolean(Integer.valueOf(5), DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToByte_InvalidType() {
        CharacterConversions.toByte("not a char", DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToShort_InvalidType() {
        CharacterConversions.toShort(Boolean.TRUE, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_InvalidType() {
        CharacterConversions.toInt(new Object(), DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_InvalidType() {
        CharacterConversions.toAtomicInteger(3.14, DUMMY_CONVERTER);
    }

    // Additional invalid‑type checks for a few remaining methods
    @Test(expected = ClassCastException.class)
    public void testToAtomicBoolean_InvalidType() {
        CharacterConversions.toAtomicBoolean(123L, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_InvalidType() {
        CharacterConversions.toBigInteger(new int[]{1,2,3}, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_InvalidType() {
        CharacterConversions.toBigDecimal(new java.util.Date(), DUMMY_CONVERTER);
    }
}
