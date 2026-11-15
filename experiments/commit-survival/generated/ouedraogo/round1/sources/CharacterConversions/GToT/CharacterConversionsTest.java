package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 tests for {@link CharacterConversions}.
 *
 * The tests cover:
 *   • normal/typical usage,
 *   • edge cases (null, non‑Character inputs, overflow, large Unicode values),
 *   • expected exceptions.
 *
 * All methods are package‑private static, therefore the test class resides in the same
 * package to gain direct access.
 */
public class CharacterConversionsTest {

    /** A dummy {@link Converter} instance – the implementation ignores the argument. */
    private static Converter DUMMY_CONVERTER;

    @BeforeClass
    public static void setUpClass() {
        // Converter requires a ConverterOptions argument; passing null is acceptable for these tests.
        DUMMY_CONVERTER = new Converter(null);
    }

    /* -------------------------------------------------
     *  toString()
     * ------------------------------------------------- */
    @Test
    public void testToStringWithNonNullObject() {
        String result = CharacterConversions.toString("hello", DUMMY_CONVERTER);
        assertEquals("hello", result);
    }

    @Test
    public void testToStringWithNull() {
        String result = CharacterConversions.toString(null, DUMMY_CONVERTER);
        assertEquals("null", result);
    }

    /* -------------------------------------------------
     *  toBoolean()
     * ------------------------------------------------- */
    @Test
    public void testToBooleanTrueValues() {
        assertTrue(CharacterConversions.toBoolean((char) 1, DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('t', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('T', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('1', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('y', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('Y', DUMMY_CONVERTER));
    }

    @Test
    public void testToBooleanFalseValues() {
        assertFalse(CharacterConversions.toBoolean('f', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('0', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean(' ', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('Z', DUMMY_CONVERTER));
    }

    @Test(expected = NullPointerException.class)
    public void testToBooleanWithNull() {
        CharacterConversions.toBoolean(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToBooleanWithNonCharacter() {
        CharacterConversions.toBoolean(Integer.valueOf(5), DUMMY_CONVERTER);
    }

    /* -------------------------------------------------
     *  Numeric primitive conversions
     * ------------------------------------------------- */
    @Test
    public void testNumericConversionsTypical() {
        char ch = 'A'; // 65
        assertEquals((byte) 65, CharacterConversions.toByte(ch, DUMMY_CONVERTER));
        assertEquals((short) 65, CharacterConversions.toShort(ch, DUMMY_CONVERTER));
        assertEquals(65, CharacterConversions.toInt(ch, DUMMY_CONVERTER));
        assertEquals(65L, CharacterConversions.toLong(ch, DUMMY_CONVERTER));
        assertEquals(65.0f, CharacterConversions.toFloat(ch, DUMMY_CONVERTER), 0.0f);
        assertEquals(65.0d, CharacterConversions.toDouble(ch, DUMMY_CONVERTER), 0.0d);
    }

    @Test
    public void testNumericConversionsOverflowByteAndShort() {
        char ch = (char) 0x00FF; // 255
        // byte wraps to -1 (255 - 256)
        assertEquals((byte) -1, CharacterConversions.toByte(ch, DUMMY_CONVERTER));
        // short holds full value (255)
        assertEquals((short) 255, CharacterConversions.toShort(ch, DUMMY_CONVERTER));
    }

    @Test
    public void testNumericConversionsLargeChar() {
        char ch = '\uFFFF'; // 65535
        assertEquals((byte) -1, CharacterConversions.toByte(ch, DUMMY_CONVERTER));
        assertEquals((short) -1, CharacterConversions.toShort(ch, DUMMY_CONVERTER));
        assertEquals(65535, CharacterConversions.toInt(ch, DUMMY_CONVERTER));
        assertEquals(65535L, CharacterConversions.toLong(ch, DUMMY_CONVERTER));
        assertEquals(65535.0f, CharacterConversions.toFloat(ch, DUMMY_CONVERTER), 0.0f);
        assertEquals(65535.0d, CharacterConversions.toDouble(ch, DUMMY_CONVERTER), 0.0d);
    }

    @Test(expected = NullPointerException.class)
    public void testNumericConversionWithNull() {
        CharacterConversions.toInt(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testNumericConversionWithNonCharacter() {
        CharacterConversions.toLong("not a char", DUMMY_CONVERTER);
    }

    /* -------------------------------------------------
     *  Atomic conversions
     * ------------------------------------------------- */
    @Test
    public void testAtomicInteger() {
        char ch = 'B'; // 66
        AtomicInteger ai = CharacterConversions.toAtomicInteger(ch, DUMMY_CONVERTER);
        assertEquals(66, ai.get());
    }

    @Test
    public void testAtomicLong() {
        char ch = 'C'; // 67
        AtomicLong al = CharacterConversions.toAtomicLong(ch, DUMMY_CONVERTER);
        assertEquals(67L, al.get());
    }

    @Test
    public void testAtomicBooleanTrueAndFalse() {
        AtomicBoolean abTrue = CharacterConversions.toAtomicBoolean('Y', DUMMY_CONVERTER);
        assertTrue(abTrue.get());

        AtomicBoolean abFalse = CharacterConversions.toAtomicBoolean('N', DUMMY_CONVERTER);
        assertFalse(abFalse.get());
    }

    @Test(expected = NullPointerException.class)
    public void testAtomicConversionWithNull() {
        CharacterConversions.toAtomicInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testAtomicConversionWithNonCharacter() {
        CharacterConversions.toAtomicLong(Boolean.TRUE, DUMMY_CONVERTER);
    }

    /* -------------------------------------------------
     *  BigInteger / BigDecimal conversions
     * ------------------------------------------------- */
    @Test
    public void testBigIntegerConversion() {
        char ch = 'D'; // 68
        BigInteger bi = CharacterConversions.toBigInteger(ch, DUMMY_CONVERTER);
        assertEquals(BigInteger.valueOf(68), bi);
    }

    @Test
    public void testBigDecimalConversion() {
        char ch = '\u00A0'; // 160
        BigDecimal bd = CharacterConversions.toBigDecimal(ch, DUMMY_CONVERTER);
        assertEquals(BigDecimal.valueOf(160), bd);
    }

    @Test(expected = NullPointerException.class)
    public void testBigIntegerWithNull() {
        CharacterConversions.toBigInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testBigDecimalWithNonCharacter() {
        CharacterConversions.toBigDecimal(new Object(), DUMMY_CONVERTER);
    }
}
