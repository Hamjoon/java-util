package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Comprehensive JUnit 4 tests for {@link CharacterConversions}.
 * The tests cover all conversion methods, edge‑cases, and
 * verify that the {@code Converter} argument is not required for
 * the current implementation.
 */
public class CharacterConversionsTest {

    /* -------------------------------------------------------------------- */
    /* Helper method to create a Character object from an int value.       */
    private static Character chr(int codePoint) {
        return (char) codePoint;
    }

    /* --------------------------- toString ----------------------------- */

    @Test
    public void testToStringWithNull() {
        // "" + null yields the string "null"
        assertEquals("null", CharacterConversions.toString(null, null));
    }

    @Test
    public void testToStringWithVariousObjects() {
        assertEquals("42", CharacterConversions.toString(42, null));
        assertEquals("hello", CharacterConversions.toString("hello", null));
        assertEquals("A", CharacterConversions.toString('A', null));
        // When passing a Character, toString should invoke its toString()
        assertEquals("Z", CharacterConversions.toString(chr('Z'), null));
    }

    /* --------------------------- toBoolean ---------------------------- */

    @Test
    public void testToBooleanTrueValues() {
        assertTrue(CharacterConversions.toBoolean(chr('t'), null));
        assertTrue(CharacterConversions.toBoolean(chr('T'), null));
        assertTrue(CharacterConversions.toBoolean(chr('1'), null));
        assertTrue(CharacterConversions.toBoolean(chr('y'), null));
        assertTrue(CharacterConversions.toBoolean(chr('Y'), null));
        assertTrue(CharacterConversions.toBoolean(chr(1), null)); // control char 0x01
    }

    @Test
    public void testToBooleanFalseValues() {
        // Any character not listed above must be false
        assertFalse(CharacterConversions.toBoolean(chr('f'), null));
        assertFalse(CharacterConversions.toBoolean(chr('0'), null));
        assertFalse(CharacterConversions.toBoolean(chr('n'), null));
        assertFalse(CharacterConversions.toBoolean(chr(' '), null));
        assertFalse(CharacterConversions.toBoolean(chr(0), null)); // NUL
        assertFalse(CharacterConversions.toBoolean(chr(2), null)); // control char 0x02
    }

    /* --------------------------- numeric casts ------------------------ */

    @Test
    public void testToByteWithinRange() {
        assertEquals((byte) 0, CharacterConversions.toByte(chr(0), null));
        assertEquals((byte) 127, CharacterConversions.toByte(chr(127), null));
        assertEquals((byte) -128, CharacterConversions.toByte(chr(128), null)); // overflow
    }

    @Test
    public void testToByteOverflowBehaviour() {
        // Java truncates the high‑order bits when casting to byte
        assertEquals((byte) -56, CharacterConversions.toByte(chr(200), null)); // 200 - 256 = -56
        assertEquals((byte) -1, CharacterConversions.toByte(chr(255), null)); // 0xFF -> -1
    }

    @Test
    public void testToShortWithinRange() {
        assertEquals((short) 0, CharacterConversions.toShort(chr(0), null));
        assertEquals((short) 32767, CharacterConversions.toShort(chr(32767), null));
        assertEquals((short) -32768, CharacterConversions.toShort(chr(32768), null)); // overflow
    }

    @Test
    public void testToShortOverflowBehaviour() {
        assertEquals((short) -1, CharacterConversions.toShort(chr(65535), null)); // 0xFFFF -> -1
    }

    @Test
    public void testToInt() {
        assertEquals(0, CharacterConversions.toInt(chr(0), null));
        assertEquals(65, CharacterConversions.toInt(chr('A'), null));
        assertEquals(65535, CharacterConversions.toInt(chr(65535), null));
    }

    @Test
    public void testToLong() {
        assertEquals(0L, CharacterConversions.toLong(chr(0), null));
        assertEquals(97L, CharacterConversions.toLong(chr('a'), null));
        assertEquals(65535L, CharacterConversions.toLong(chr(65535), null));
    }

    @Test
    public void testToFloat() {
        assertEquals(0.0f, CharacterConversions.toFloat(chr(0), null), 0.0f);
        assertEquals(65.0f, CharacterConversions.toFloat(chr('A'), null), 0.0f);
        assertEquals(65535.0f, CharacterConversions.toFloat(chr(65535), null), 0.0f);
    }

    @Test
    public void testToDouble() {
        assertEquals(0.0, CharacterConversions.toDouble(chr(0), null), 0.0);
        assertEquals(98.0, CharacterConversions.toDouble(chr('b'), null), 0.0);
        assertEquals(65535.0, CharacterConversions.toDouble(chr(65535), null), 0.0);
    }

    /* --------------------------- Atomic types -------------------------- */

    @Test
    public void testToAtomicInteger() {
        AtomicInteger ai = CharacterConversions.toAtomicInteger(chr(123), null);
        assertEquals(123, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong al = CharacterConversions.toAtomicLong(chr(40000), null);
        assertEquals(40000L, al.get());
    }

    @Test
    public void testToAtomicBoolean() {
        AtomicBoolean abTrue = CharacterConversions.toAtomicBoolean(chr('Y'), null);
        AtomicBoolean abFalse = CharacterConversions.toAtomicBoolean(chr('n'), null);
        assertTrue(abTrue.get());
        assertFalse(abFalse.get());
    }

    /* --------------------------- BigInteger / BigDecimal -------------- */

    @Test
    public void testToBigInteger() {
        BigInteger bi = CharacterConversions.toBigInteger(chr(5000), null);
        assertEquals(BigInteger.valueOf(5000L), bi);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = CharacterConversions.toBigDecimal(chr(12345), null);
        assertEquals(BigDecimal.valueOf(12345L), bd);
    }

    /* --------------------------- Additional Edge Cases --------------- */

    @Test
    public void testAllConversionsWithMinimumChar() {
        char min = (char) 0;
        assertEquals((byte) 0, CharacterConversions.toByte(min, null));
        assertEquals((short) 0, CharacterConversions.toShort(min, null));
        assertEquals(0, CharacterConversions.toInt(min, null));
        assertEquals(0L, CharacterConversions.toLong(min, null));
        assertEquals(0.0f, CharacterConversions.toFloat(min, null), 0.0f);
        assertEquals(0.0, CharacterConversions.toDouble(min, null), 0.0);
        assertEquals(0, CharacterConversions.toAtomicInteger(min, null).get());
        assertEquals(0L, CharacterConversions.toAtomicLong(min, null).get());
        assertFalse(CharacterConversions.toAtomicBoolean(min, null).get());
        assertEquals(BigInteger.ZERO, CharacterConversions.toBigInteger(min, null));
        assertEquals(BigDecimal.ZERO, CharacterConversions.toBigDecimal(min, null));
    }

    @Test
    public void testAllConversionsWithMaximumChar() {
        char max = (char) 65535;
        assertEquals((byte) -1, CharacterConversions.toByte(max, null));
        assertEquals((short) -1, CharacterConversions.toShort(max, null));
        assertEquals(65535, CharacterConversions.toInt(max, null));
        assertEquals(65535L, CharacterConversions.toLong(max, null));
        assertEquals(65535.0f, CharacterConversions.toFloat(max, null), 0.0f);
        assertEquals(65535.0, CharacterConversions.toDouble(max, null), 0.0);
        assertEquals(65535, CharacterConversions.toAtomicInteger(max, null).get());
        assertEquals(65535L, CharacterConversions.toAtomicLong(max, null).get());
        // max char is not a true boolean value
        assertFalse(CharacterConversions.toAtomicBoolean(max, null).get());
        assertEquals(BigInteger.valueOf(65535L), CharacterConversions.toBigInteger(max, null));
        assertEquals(BigDecimal.valueOf(65535L), CharacterConversions.toBigDecimal(max, null));
    }
}
