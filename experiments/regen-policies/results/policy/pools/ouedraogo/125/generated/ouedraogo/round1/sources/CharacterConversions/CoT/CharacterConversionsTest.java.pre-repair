package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Assert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JUnit 4 test suite for {@link CharacterConversions}.
 * All tests are placed in the same package to allow access to the package‑private class.
 */
public class CharacterConversionsTest {

    /* ----------------------------------------------------------------------
     * Helper – a dummy Converter instance (the methods under test never use it).
     * ---------------------------------------------------------------------- */
    private static final Converter DUMMY_CONVERTER = null;

    /* ----------------------------------------------------------------------
     * 1. toString
     * ---------------------------------------------------------------------- */
    @Test
    public void testToStringWithCharacter() {
        Character ch = 'Z';
        String result = CharacterConversions.toString(ch, DUMMY_CONVERTER);
        Assert.assertEquals("Z", result);
    }

    @Test
    public void testToStringWithNull() {
        String result = CharacterConversions.toString(null, DUMMY_CONVERTER);
        Assert.assertEquals("null", result);
    }

    /* ----------------------------------------------------------------------
     * 2. toBoolean – true cases
     * ---------------------------------------------------------------------- */
    @Test
    public void testToBooleanTrueValues() {
        char[] trueChars = {'t', 'T', '1', 'y', 'Y', 1};
        for (char c : trueChars) {
            boolean b = CharacterConversions.toBoolean(c, DUMMY_CONVERTER);
            Assert.assertTrue("Expected true for char: " + (int) c, b);
        }
    }

    /* ----------------------------------------------------------------------
     * 3. toBoolean – false cases
     * ---------------------------------------------------------------------- */
    @Test
    public void testToBooleanFalseValues() {
        char[] falseChars = {'f', 'F', '0', 'n', 'N', 'a', 'Z', 0};
        for (char c : falseChars) {
            boolean b = CharacterConversions.toBoolean(c, DUMMY_CONVERTER);
            Assert.assertFalse("Expected false for char: " + (int) c, b);
        }
    }

    @Test(expected = ClassCastException.class)
    public void testToBooleanWithInvalidType() {
        // Integer cannot be cast to char → ClassCastException
        CharacterConversions.toBoolean(Integer.valueOf(5), DUMMY_CONVERTER);
    }

    /* ----------------------------------------------------------------------
     * 4. toByte – normal and overflow cases
     * ---------------------------------------------------------------------- */
    @Test
    public void testToByteNormal() {
        char c = 'A'; // 65
        byte b = CharacterConversions.toByte(c, DUMMY_CONVERTER);
        Assert.assertEquals((byte) 65, b);
    }

    @Test
    public void testToByteOverflow() {
        char c = '\uFFFF'; // 65535
        byte b = CharacterConversions.toByte(c, DUMMY_CONVERTER);
        // 65535 modulo 256 = 255 → signed byte = -1
        Assert.assertEquals((byte) -1, b);
    }

    /* ----------------------------------------------------------------------
     * 5. toShort – normal and overflow cases
     * ---------------------------------------------------------------------- */
    @Test
    public void testToShortNormal() {
        char c = 'a'; // 97
        short s = CharacterConversions.toShort(c, DUMMY_CONVERTER);
        Assert.assertEquals((short) 97, s);
    }

    @Test
    public void testToShortOverflow() {
        char c = '\uFFFF'; // 65535
        short s = CharacterConversions.toShort(c, DUMMY_CONVERTER);
        // 65535 modulo 65536 = 65535 → signed short = -1
        Assert.assertEquals((short) -1, s);
    }

    /* ----------------------------------------------------------------------
     * 6. toInt, toLong, toFloat, toDouble – basic numeric conversion
     * ---------------------------------------------------------------------- */
    @Test
    public void testToInt() {
        char c = '\u1234'; // 4660
        int i = CharacterConversions.toInt(c, DUMMY_CONVERTER);
        Assert.assertEquals(4660, i);
    }

    @Test
    public void testToLong() {
        char c = '\u00FF'; // 255
        long l = CharacterConversions.toLong(c, DUMMY_CONVERTER);
        Assert.assertEquals(255L, l);
    }

    @Test
    public void testToFloat() {
        char c = '\u0100'; // 256
        float f = CharacterConversions.toFloat(c, DUMMY_CONVERTER);
        Assert.assertEquals(256.0f, f, 0.0f);
    }

    @Test
    public void testToDouble() {
        char c = '\u00A0'; // 160
        double d = CharacterConversions.toDouble(c, DUMMY_CONVERTER);
        Assert.assertEquals(160.0, d, 0.0);
    }

    /* ----------------------------------------------------------------------
     * 7. toAtomicInteger / toAtomicLong
     * ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicInteger() {
        char c = 'Z'; // 90
        AtomicInteger ai = CharacterConversions.toAtomicInteger(c, DUMMY_CONVERTER);
        Assert.assertEquals(90, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        char c = '\u7FFF'; // 32767
        AtomicLong al = CharacterConversions.toAtomicLong(c, DUMMY_CONVERTER);
        Assert.assertEquals(32767L, al.get());
    }

    /* ----------------------------------------------------------------------
     * 8. toAtomicBoolean – true & false paths
     * ---------------------------------------------------------------------- */
    @Test
    public void testToAtomicBooleanTrue() {
        char c = 'y';
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean(c, DUMMY_CONVERTER);
        Assert.assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        char c = 'n';
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean(c, DUMMY_CONVERTER);
        Assert.assertFalse(ab.get());
    }

    /* ----------------------------------------------------------------------
     * 9. toBigInteger / toBigDecimal
     * ---------------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        char c = '\u00C8'; // 200
        BigInteger bi = CharacterConversions.toBigInteger(c, DUMMY_CONVERTER);
        Assert.assertEquals(BigInteger.valueOf(200), bi);
    }

    @Test
    public void testToBigDecimal() {
        char c = '\u03E8'; // 1000
        BigDecimal bd = CharacterConversions.toBigDecimal(c, DUMMY_CONVERTER);
        Assert.assertEquals(BigDecimal.valueOf(1000), bd);
    }

    /* ----------------------------------------------------------------------
     * 10. Invalid source type – should throw ClassCastException for all methods
     * ---------------------------------------------------------------------- */
    @Test(expected = ClassCastException.class)
    public void testToByteInvalidSource() {
        CharacterConversions.toByte("not a char", DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToShortInvalidSource() {
        CharacterConversions.toShort(Boolean.TRUE, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToIntInvalidSource() {
        CharacterConversions.toInt(12345, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToLongNullSource() {
        CharacterConversions.toLong(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToFloatInvalidSource() {
        CharacterConversions.toFloat(new Object(), DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleNullSource() {
        CharacterConversions.toDouble(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerInvalidSource() {
        CharacterConversions.toAtomicInteger(42, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNullSource() {
        CharacterConversions.toAtomicLong(null, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBooleanInvalidSource() {
        CharacterConversions.toAtomicBoolean(new int[]{1}, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNullSource() {
        CharacterConversions.toBigInteger(null, DUMMY_CONVERTER);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalNullSource() {
        CharacterConversions.toBigDecimal(null, DUMMY_CONVERTER);
    }
}
