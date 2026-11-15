package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Unit tests for {@link CharacterConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>String conversion (including {@code null})</li>
 *   <li>Boolean conversion for all true‑case characters and several false‑case characters</li>
 *   <li>Numeric conversions (byte, short, int, long, float, double) including overflow behaviour</li>
 *   <li>Atomic wrapper conversions</li>
 *   <li>BigInteger / BigDecimal conversions</li>
 *   <li>Invalid input handling (ClassCastException)</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore {@code null}
 * is supplied for all calls.
 */
public class CharacterConversionsTest {

    /* ---------------------------------------------------------------------- */
    /* Helper constants                                                       */
    /* ---------------------------------------------------------------------- */

    private static final Converter NULL_CONVERTER = null;

    /* ---------------------------------------------------------------------- */
    /* toString                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringWithCharacter() {
        Character ch = 'Z';
        assertEquals("Z", CharacterConversions.toString(ch, NULL_CONVERTER));
    }

    @Test
    public void testToStringWithNull() {
        assertEquals("null", CharacterConversions.toString(null, NULL_CONVERTER));
    }

    @Test
    public void testToStringWithNonCharacterObject() {
        Integer i = 123;
        assertEquals("123", CharacterConversions.toString(i, NULL_CONVERTER));
    }

    /* ---------------------------------------------------------------------- */
    /* toBoolean                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBooleanTrueCases() {
        // Control character with value 1
        assertTrue(CharacterConversions.toBoolean((char) 1, NULL_CONVERTER));

        // Lower‑case true indicators
        assertTrue(CharacterConversions.toBoolean('t', NULL_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('y', NULL_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('1', NULL_CONVERTER));

        // Upper‑case true indicators
        assertTrue(CharacterConversions.toBoolean('T', NULL_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('Y', NULL_CONVERTER));
    }

    @Test
    public void testToBooleanFalseCases() {
        assertFalse(CharacterConversions.toBoolean('f', NULL_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('F', NULL_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('0', NULL_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('n', NULL_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('N', NULL_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('a', NULL_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('Z', NULL_CONVERTER));
    }

    /* ---------------------------------------------------------------------- */
    /* Numeric conversions (byte, short, int, long, float, double)            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testNumericConversionsSimpleValues() {
        char c = 'A'; // 65
        assertEquals((byte) 65, CharacterConversions.toByte(c, NULL_CONVERTER));
        assertEquals((short) 65, CharacterConversions.toShort(c, NULL_CONVERTER));
        assertEquals(65, CharacterConversions.toInt(c, NULL_CONVERTER));
        assertEquals(65L, CharacterConversions.toLong(c, NULL_CONVERTER));
        assertEquals(65.0f, CharacterConversions.toFloat(c, NULL_CONVERTER), 0.0f);
        assertEquals(65.0d, CharacterConversions.toDouble(c, NULL_CONVERTER), 0.0d);
    }

    @Test
    public void testNumericConversionsOverflowByte() {
        char c = (char) 200; // 200 > Byte.MAX_VALUE (127)
        assertEquals((byte) 200, CharacterConversions.toByte(c, NULL_CONVERTER)); // expected -56
        // Verify the overflow behaviour explicitly
        assertEquals(-56, CharacterConversions.toByte(c, NULL_CONVERTER));
    }

    @Test
    public void testNumericConversionsOverflowShort() {
        char c = (char) 65535; // max char value
        // short overflow: 65535 -> -1
        assertEquals((short) -1, CharacterConversions.toShort(c, NULL_CONVERTER));
    }

    @Test
    public void testNumericConversionsMaxValues() {
        char max = Character.MAX_VALUE; // 0xFFFF == 65535
        assertEquals(65535, CharacterConversions.toInt(max, NULL_CONVERTER));
        assertEquals(65535L, CharacterConversions.toLong(max, NULL_CONVERTER));
        assertEquals(65535.0f, CharacterConversions.toFloat(max, NULL_CONVERTER), 0.0f);
        assertEquals(65535.0d, CharacterConversions.toDouble(max, NULL_CONVERTER), 0.0d);
    }

    /* ---------------------------------------------------------------------- */
    /* Atomic wrapper conversions                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicInteger() {
        char c = 'b'; // 98
        AtomicInteger ai = CharacterConversions.toAtomicInteger(c, NULL_CONVERTER);
        assertNotNull(ai);
        assertEquals(98, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        char c = 'c'; // 99
        AtomicLong al = CharacterConversions.toAtomicLong(c, NULL_CONVERTER);
        assertNotNull(al);
        assertEquals(99L, al.get());
    }

    @Test
    public void testToAtomicBooleanTrue() {
        // Any true‑case character should result in true
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean('Y', NULL_CONVERTER);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean('n', NULL_CONVERTER);
        assertFalse(ab.get());
    }

    /* ---------------------------------------------------------------------- */
    /* BigInteger / BigDecimal conversions                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigInteger() {
        char c = 'd'; // 100
        BigInteger bi = CharacterConversions.toBigInteger(c, NULL_CONVERTER);
        assertEquals(BigInteger.valueOf(100), bi);
    }

    @Test
    public void testToBigDecimal() {
        char c = 'e'; // 101
        BigDecimal bd = CharacterConversions.toBigDecimal(c, NULL_CONVERTER);
        assertEquals(BigDecimal.valueOf(101), bd);
    }

    /* ---------------------------------------------------------------------- */
    /* Invalid input handling (ClassCastException)                           */
    /* ---------------------------------------------------------------------- */

    @Test(expected = ClassCastException.class)
    public void testToBooleanWithInvalidObject() {
        // Passing a non‑Character object should cause a ClassCastException
        CharacterConversions.toBoolean("not a char", NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteWithInvalidObject() {
        CharacterConversions.toByte(123, NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerWithInvalidObject() {
        CharacterConversions.toAtomicInteger(Boolean.TRUE, NULL_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigIntegerWithInvalidObject() {
        CharacterConversions.toBigInteger(new Object(), NULL_CONVERTER);
    }
}
