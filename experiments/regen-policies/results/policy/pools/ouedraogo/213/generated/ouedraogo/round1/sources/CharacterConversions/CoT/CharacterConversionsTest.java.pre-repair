package com.cedarsoftware.util.convert;

import org.junit.Test;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CharacterConversions}.
 *
 * The class under test has package‑private static methods, therefore the test class
 * resides in the same package to gain access without using reflection.
 *
 * The test cases are divided into three groups:
 *   1. Basic functional tests – one simple assertion for each conversion method.
 *   2. Edge‑case tests – values that exercise overflow, sign‑extension and special
 *      characters.
 *   3. Exception tests – verify that illegal inputs (null or wrong type) raise the
 *      expected runtime exceptions.
 */
public class CharacterConversionsTest {

    /* ---------------------------------------------------------------------- */
    /* 1. BASIC FUNCTIONAL TESTS                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringWithNonNullObject() {
        Integer value = 42;
        String result = CharacterConversions.toString(value, null);
        assertEquals("42", result);
    }

    @Test
    public void testToStringWithNull() {
        String result = CharacterConversions.toString(null, null);
        assertEquals("null", result);
    }

    @Test
    public void testToBooleanTrueValues() {
        assertTrue(CharacterConversions.toBoolean((char) 1, null));
        assertTrue(CharacterConversions.toBoolean('t', null));
        assertTrue(CharacterConversions.toBoolean('T', null));
        assertTrue(CharacterConversions.toBoolean('1', null));
        assertTrue(CharacterConversions.toBoolean('y', null));
        assertTrue(CharacterConversions.toBoolean('Y', null));
    }

    @Test
    public void testToBooleanFalseValues() {
        assertFalse(CharacterConversions.toBoolean('0', null));
        assertFalse(CharacterConversions.toBoolean('n', null));
        assertFalse(CharacterConversions.toBoolean('N', null));
        assertFalse(CharacterConversions.toBoolean('f', null));
        assertFalse(CharacterConversions.toBoolean('F', null));
        assertFalse(CharacterConversions.toBoolean('a', null));
    }

    @Test
    public void testNumericConversions() {
        char c = 'A'; // Unicode 65
        assertEquals((byte) 65, CharacterConversions.toByte(c, null));
        assertEquals((short) 65, CharacterConversions.toShort(c, null));
        assertEquals(65, CharacterConversions.toInt(c, null));
        assertEquals(65L, CharacterConversions.toLong(c, null));
        assertEquals(65.0f, CharacterConversions.toFloat(c, null), 0.0f);
        assertEquals(65.0, CharacterConversions.toDouble(c, null), 0.0);
    }

    @Test
    public void testAtomicConversions() {
        char c = 'B'; // Unicode 66
        assertEquals(66, new AtomicInteger((char) c).get());
        assertEquals(66L, new AtomicLong((char) c).get());

        AtomicInteger ai = CharacterConversions.toAtomicInteger(c, null);
        AtomicLong al = CharacterConversions.toAtomicLong(c, null);
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean('t', null);

        assertEquals(66, ai.get());
        assertEquals(66L, al.get());
        assertTrue(ab.get());
    }

    @Test
    public void testBigNumberConversions() {
        char c = 'C'; // Unicode 67
        BigInteger bi = CharacterConversions.toBigInteger(c, null);
        BigDecimal bd = CharacterConversions.toBigDecimal(c, null);

        assertEquals(BigInteger.valueOf(67), bi);
        assertEquals(BigDecimal.valueOf(67), bd);
    }

    /* ---------------------------------------------------------------------- */
    /* 2. EDGE‑CASE TESTS                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testByteOverflow() {
        char c = (char) 257; // 257 == 0x0101, byte cast should drop high byte => 1
        assertEquals((byte) 1, CharacterConversions.toByte(c, null));
    }

    @Test
    public void testShortOverflow() {
        char c = (char) 40000; // value > Short.MAX_VALUE (32767)
        short expected = (short) 40000; // Java truncates to -25536
        assertEquals(expected, CharacterConversions.toShort(c, null));
    }

    @Test
    public void testMaximumCharValue() {
        char maxChar = Character.MAX_VALUE; // 0xFFFF == 65535
        assertEquals((byte) -1, CharacterConversions.toByte(maxChar, null)); // 0xFFFF -> 0xFF -> -1
        assertEquals((short) -1, CharacterConversions.toShort(maxChar, null));
        assertEquals(65535, CharacterConversions.toInt(maxChar, null));
        assertEquals(65535L, CharacterConversions.toLong(maxChar, null));
        assertEquals(65535.0f, CharacterConversions.toFloat(maxChar, null), 0.0f);
        assertEquals(65535.0, CharacterConversions.toDouble(maxChar, null), 0.0);
        assertEquals(BigInteger.valueOf(65535L), CharacterConversions.toBigInteger(maxChar, null));
        assertEquals(BigDecimal.valueOf(65535L), CharacterConversions.toBigDecimal(maxChar, null));
    }

    @Test
    public void testAtomicBooleanTrueAndFalse() {
        assertTrue(CharacterConversions.toAtomicBoolean('Y', null).get());
        assertFalse(CharacterConversions.toAtomicBoolean('n', null).get());
    }

    /* ---------------------------------------------------------------------- */
    /* 3. EXCEPTION TESTS                                                      */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToBooleanWithNull() {
        CharacterConversions.toBoolean(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToBooleanWithWrongType() {
        CharacterConversions.toBoolean("not a char", null);
    }

    @Test(expected = NullPointerException.class)
    public void testNumericConversionWithNull() {
        CharacterConversions.toInt(null, null);
    }

    @Test(expected = ClassCastException.class)
    public void testNumericConversionWithWrongType() {
        CharacterConversions.toLong(12345, null); // Integer, not Character
    }

    @Test(expected = NullPointerException.class)
    public void testAtomicBooleanWithNull() {
        CharacterConversions.toAtomicBoolean(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testBigIntegerWithNull() {
        CharacterConversions.toBigInteger(null, null);
    }

    @Test(expected = NullPointerException.class)
    public void testBigDecimalWithNull() {
        CharacterConversions.toBigDecimal(null, null);
    }
}
