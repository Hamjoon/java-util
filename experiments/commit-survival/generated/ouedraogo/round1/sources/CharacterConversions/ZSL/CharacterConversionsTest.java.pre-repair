package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link CharacterConversions}.
 *
 * The tests are written in the same package so they can access the
 * package‑private static methods directly.
 */
public class CharacterConversionsTest {

    /** A Converter instance – the implementation does not matter for these tests. */
    private Converter converter;

    @Before
    public void setUp() {
        // The Converter constructor only requires a ConverterOptions instance.
        // Passing {@code null} is acceptable for the purpose of these tests
        // because the methods under test never invoke any Converter behaviour.
        this.converter = new Converter(null);
    }

    /* --------------------------------------------------------------------- */
    /*                     toString(..)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringWithCharacterObject() {
        Character ch = 'Z';
        String result = CharacterConversions.toString(ch, converter);
        assertEquals("Z", result);
    }

    @Test
    public void testToStringWithPrimitiveCharWrappedInObject() {
        // Autoboxing creates a Character instance.
        Object ch = '!';
        String result = CharacterConversions.toString(ch, converter);
        assertEquals("!", result);
    }

    /* --------------------------------------------------------------------- */
    /*                     toBoolean(..)                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToBooleanTrueValues() {
        // 1 (control char)
        assertTrue(CharacterConversions.toBoolean((char) 1, converter));
        // lower case 't' and 'y'
        assertTrue(CharacterConversions.toBoolean('t', converter));
        assertTrue(CharacterConversions.toBoolean('T', converter));
        assertTrue(CharacterConversions.toBoolean('y', converter));
        assertTrue(CharacterConversions.toBoolean('Y', converter));
        // digit '1'
        assertTrue(CharacterConversions.toBoolean('1', converter));
    }

    @Test
    public void testToBooleanFalseValues() {
        // Any character not listed in the true set should be false
        assertFalse(CharacterConversions.toBoolean('f', converter));
        assertFalse(CharacterConversions.toBoolean('F', converter));
        assertFalse(CharacterConversions.toBoolean('0', converter));
        assertFalse(CharacterConversions.toBoolean('n', converter));
        assertFalse(CharacterConversions.toBoolean('N', converter));
        assertFalse(CharacterConversions.toBoolean(' ', converter));
        assertFalse(CharacterConversions.toBoolean('Z', converter));
    }

    /* --------------------------------------------------------------------- */
    /*                     Numeric Conversions                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNumericConversionsWithStandardChar() {
        char ch = 'A'; // Unicode code point 65

        assertEquals((byte) 65, CharacterConversions.toByte(ch, converter));
        assertEquals((short) 65, CharacterConversions.toShort(ch, converter));
        assertEquals(65, CharacterConversions.toInt(ch, converter));
        assertEquals(65L, CharacterConversions.toLong(ch, converter));
        assertEquals(65.0f, CharacterConversions.toFloat(ch, converter), 0.0f);
        assertEquals(65.0d, CharacterConversions.toDouble(ch, converter), 0.0d);
    }

    @Test
    public void testByteOverflowBehaviour() {
        // Char value 200 exceeds signed byte range (‑128..127)
        char ch = (char) 200; // Unicode code point 200
        byte expected = (byte) 200; // -56
        assertEquals(expected, CharacterConversions.toByte(ch, converter));
    }

    @Test
    public void testShortAndIntPreserveFullRange() {
        // Choose a value larger than 32767 to ensure short truncates correctly
        char ch = (char) 40000; // 40000 - 65536 = 40000 (fits in char)
        assertEquals((short) 40000, CharacterConversions.toShort(ch, converter));
        assertEquals(40000, CharacterConversions.toInt(ch, converter));
    }

    /* --------------------------------------------------------------------- */
    /*                     Atomic Conversions                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToAtomicInteger() {
        char ch = 'b'; // 98
        AtomicInteger ai = CharacterConversions.toAtomicInteger(ch, converter);
        assertNotNull(ai);
        assertEquals(98, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        char ch = 'c'; // 99
        AtomicLong al = CharacterConversions.toAtomicLong(ch, converter);
        assertNotNull(al);
        assertEquals(99L, al.get());
    }

    @Test
    public void testToAtomicBooleanTrueAndFalse() {
        // True case
        AtomicBoolean abTrue = CharacterConversions.toAtomicBoolean('t', converter);
        assertTrue(abTrue.get());

        // False case
        AtomicBoolean abFalse = CharacterConversions.toAtomicBoolean('f', converter);
        assertFalse(abFalse.get());
    }

    /* --------------------------------------------------------------------- */
    /*                     BigInteger / BigDecimal Conversions                */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToBigInteger() {
        char ch = 'd'; // 100
        BigInteger bi = CharacterConversions.toBigInteger(ch, converter);
        assertNotNull(bi);
        assertEquals(BigInteger.valueOf(100L), bi);
    }

    @Test
    public void testToBigDecimal() {
        char ch = 'e'; // 101
        BigDecimal bd = CharacterConversions.toBigDecimal(ch, converter);
        assertNotNull(bd);
        assertEquals(BigDecimal.valueOf(101L), bd);
    }

    /* --------------------------------------------------------------------- */
    /*                     Edge Cases / Null Handling                         */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToStringWithNullObjectThrowsNPE() {
        // The implementation does a string concatenation which triggers NPE
        // when the argument is null.
        CharacterConversions.toString(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBooleanWithNullObjectThrowsNPE() {
        // Cast to char will cause NPE before the method body executes.
        CharacterConversions.toBoolean(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testNumericConversionWithNullThrowsNPE() {
        CharacterConversions.toInt(null, converter);
    }
}
