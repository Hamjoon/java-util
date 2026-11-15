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
 * Unit tests for {@link CharacterConversions}. All public static conversion
 * methods are exercised with a variety of input characters, including edge
 * values, to verify correct casting and boolean logic.
 *
 * The {@link Converter} class required by the conversion methods does not
 * influence the behavior of the methods under test, therefore a minimal
 * instance (created with a {@code null} options argument) is sufficient.
 */
public class CharacterConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The Converter constructor accepts a ConverterOptions argument; passing null is safe for these tests.
        this.converter = new Converter(null);
    }

    /* -----------------------------------------------------------
     * toString()
     * ----------------------------------------------------------- */
    @Test
    public void testToString_WithVariousObjects() {
        assertEquals("null", CharacterConversions.toString(null, converter));

        Character ch = 'Z';
        assertEquals("Z", CharacterConversions.toString(ch, converter));

        String str = "hello";
        assertEquals("hello", CharacterConversions.toString(str, converter));

        Integer i = 123;
        assertEquals("123", CharacterConversions.toString(i, converter));
    }

    /* -----------------------------------------------------------
     * toBoolean()
     * ----------------------------------------------------------- */
    @Test
    public void testToBoolean_TrueValues() {
        // Control character 0x0001
        assertTrue(CharacterConversions.toBoolean((char) 1, converter));

        // Lowercase true characters
        assertTrue(CharacterConversions.toBoolean('t', converter));
        assertTrue(CharacterConversions.toBoolean('y', converter));
        assertTrue(CharacterConversions.toBoolean('1', converter));

        // Uppercase true characters
        assertTrue(CharacterConversions.toBoolean('T', converter));
        assertTrue(CharacterConversions.toBoolean('Y', converter));
    }

    @Test
    public void testToBoolean_FalseValues() {
        // Some representative false characters
        assertFalse(CharacterConversions.toBoolean('f', converter));
        assertFalse(CharacterConversions.toBoolean('0', converter));
        assertFalse(CharacterConversions.toBoolean('N', converter));
        assertFalse(CharacterConversions.toBoolean('n', converter));
        assertFalse(CharacterConversions.toBoolean(' ', converter));
        assertFalse(CharacterConversions.toBoolean('A', converter));
        assertFalse(CharacterConversions.toBoolean((char) 0, converter));
    }

    /* -----------------------------------------------------------
     * Numeric primitive conversions
     * ----------------------------------------------------------- */
    @Test
    public void testNumericConversions_BasicChar() {
        char ch = 'A'; // Unicode 65

        assertEquals((byte) 65, CharacterConversions.toByte(ch, converter));
        assertEquals((short) 65, CharacterConversions.toShort(ch, converter));
        assertEquals(65, CharacterConversions.toInt(ch, converter));
        assertEquals(65L, CharacterConversions.toLong(ch, converter));
        assertEquals(65.0f, CharacterConversions.toFloat(ch, converter), 0.0f);
        assertEquals(65.0d, CharacterConversions.toDouble(ch, converter), 0.0d);
    }

    @Test
    public void testNumericConversions_EdgeChars() {
        char maxChar = Character.MAX_VALUE; // 65535
        char minChar = Character.MIN_VALUE; // 0

        // max char
        assertEquals((byte) -1, CharacterConversions.toByte(maxChar, converter)); // 65535 -> 0xFFFF -> -1 as byte
        assertEquals((short) -1, CharacterConversions.toShort(maxChar, converter)); // -1 as short
        assertEquals(65535, CharacterConversions.toInt(maxChar, converter));
        assertEquals(65535L, CharacterConversions.toLong(maxChar, converter));
        assertEquals(65535.0f, CharacterConversions.toFloat(maxChar, converter), 0.0f);
        assertEquals(65535.0d, CharacterConversions.toDouble(maxChar, converter), 0.0d);

        // min char
        assertEquals((byte) 0, CharacterConversions.toByte(minChar, converter));
        assertEquals((short) 0, CharacterConversions.toShort(minChar, converter));
        assertEquals(0, CharacterConversions.toInt(minChar, converter));
        assertEquals(0L, CharacterConversions.toLong(minChar, converter));
        assertEquals(0.0f, CharacterConversions.toFloat(minChar, converter), 0.0f);
        assertEquals(0.0d, CharacterConversions.toDouble(minChar, converter), 0.0d);
    }

    /* -----------------------------------------------------------
     * Atomic wrappers
     * ----------------------------------------------------------- */
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
    public void testToAtomicBoolean() {
        // true case
        AtomicBoolean abTrue = CharacterConversions.toAtomicBoolean('Y', converter);
        assertTrue(abTrue.get());

        // false case
        AtomicBoolean abFalse = CharacterConversions.toAtomicBoolean('n', converter);
        assertFalse(abFalse.get());
    }

    /* -----------------------------------------------------------
     * BigInteger / BigDecimal conversions
     * ----------------------------------------------------------- */
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

    /* -----------------------------------------------------------
     * Helper overloads using Character wrapper (Object)
     * ----------------------------------------------------------- */
    @Test
    public void testConversions_WithCharacterWrapper() {
        Character wrapper = Character.valueOf('Z'); // 90

        // Primitive numeric conversions
        assertEquals((byte) 90, CharacterConversions.toByte(wrapper, converter));
        assertEquals((short) 90, CharacterConversions.toShort(wrapper, converter));
        assertEquals(90, CharacterConversions.toInt(wrapper, converter));
        assertEquals(90L, CharacterConversions.toLong(wrapper, converter));
        assertEquals(90.0f, CharacterConversions.toFloat(wrapper, converter), 0.0f);
        assertEquals(90.0d, CharacterConversions.toDouble(wrapper, converter), 0.0d);

        // Atomic and big types
        assertEquals(90, CharacterConversions.toAtomicInteger(wrapper, converter).get());
        assertEquals(90L, CharacterConversions.toAtomicLong(wrapper, converter).get());
        assertEquals(BigInteger.valueOf(90L), CharacterConversions.toBigInteger(wrapper, converter));
        assertEquals(BigDecimal.valueOf(90L), CharacterConversions.toBigDecimal(wrapper, converter));
    }
}
