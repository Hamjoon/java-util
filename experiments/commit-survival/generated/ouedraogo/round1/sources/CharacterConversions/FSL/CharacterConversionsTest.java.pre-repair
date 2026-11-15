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
 * All public static methods are exercised with a variety of inputs,
 * including boundary values and typical use‑cases.
 */
public class CharacterConversionsTest {

    // Helper: a minimal Converter instance – the methods under test never
    // actually use the Converter, they only require a non‑null reference.
    private static final Converter DUMMY_CONVERTER = new Converter(null);

    @Test
    public void testToString_WithNonNullObject() {
        assertEquals("A", CharacterConversions.toString('A', DUMMY_CONVERTER));
        assertEquals("65", CharacterConversions.toString(65, DUMMY_CONVERTER));
        assertEquals("true", CharacterConversions.toString(true, DUMMY_CONVERTER));
    }

    @Test
    public void testToString_WithNull() {
        // "" + null yields the literal string "null"
        assertEquals("null", CharacterConversions.toString(null, DUMMY_CONVERTER));
    }

    @Test
    public void testToBoolean_TrueValues() {
        // numeric 1 as char
        assertTrue(CharacterConversions.toBoolean((char) 1, DUMMY_CONVERTER));

        // lower‑case t, T, 1, y, Y
        assertTrue(CharacterConversions.toBoolean('t', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('T', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('1', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('y', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('Y', DUMMY_CONVERTER));
    }

    @Test
    public void testToBoolean_FalseValues() {
        // any other character should be false
        assertFalse(CharacterConversions.toBoolean('0', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('f', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('F', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('n', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('N', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean(' ', DUMMY_CONVERTER));
    }

    @Test
    public void testNumericConversions() {
        char testChar = 'A'; // Unicode 65

        assertEquals((byte) 65, CharacterConversions.toByte(testChar, DUMMY_CONVERTER));
        assertEquals((short) 65, CharacterConversions.toShort(testChar, DUMMY_CONVERTER));
        assertEquals(65, CharacterConversions.toInt(testChar, DUMMY_CONVERTER));
        assertEquals(65L, CharacterConversions.toLong(testChar, DUMMY_CONVERTER));
        assertEquals(65.0f, CharacterConversions.toFloat(testChar, DUMMY_CONVERTER), 0.0f);
        assertEquals(65.0d, CharacterConversions.toDouble(testChar, DUMMY_CONVERTER), 0.0d);
    }

    @Test
    public void testNumericConversions_WithZeroChar() {
        char zero = '\0'; // Unicode 0

        assertEquals((byte) 0, CharacterConversions.toByte(zero, DUMMY_CONVERTER));
        assertEquals((short) 0, CharacterConversions.toShort(zero, DUMMY_CONVERTER));
        assertEquals(0, CharacterConversions.toInt(zero, DUMMY_CONVERTER));
        assertEquals(0L, CharacterConversions.toLong(zero, DUMMY_CONVERTER));
        assertEquals(0.0f, CharacterConversions.toFloat(zero, DUMMY_CONVERTER), 0.0f);
        assertEquals(0.0d, CharacterConversions.toDouble(zero, DUMMY_CONVERTER), 0.0d);
    }

    @Test
    public void testAtomicConversions() {
        char testChar = 'z'; // Unicode 122

        AtomicInteger atomicInt = CharacterConversions.toAtomicInteger(testChar, DUMMY_CONVERTER);
        assertNotNull(atomicInt);
        assertEquals(122, atomicInt.get());

        AtomicLong atomicLong = CharacterConversions.toAtomicLong(testChar, DUMMY_CONVERTER);
        assertNotNull(atomicLong);
        assertEquals(122L, atomicLong.get());

        AtomicBoolean atomicBoolTrue = CharacterConversions.toAtomicBoolean('Y', DUMMY_CONVERTER);
        assertTrue(atomicBoolTrue.get());

        AtomicBoolean atomicBoolFalse = CharacterConversions.toAtomicBoolean('n', DUMMY_CONVERTER);
        assertFalse(atomicBoolFalse.get());
    }

    @Test
    public void testBigIntegerAndBigDecimalConversions() {
        char testChar = '5'; // Unicode 53

        BigInteger bi = CharacterConversions.toBigInteger(testChar, DUMMY_CONVERTER);
        assertNotNull(bi);
        assertEquals(BigInteger.valueOf(53), bi);

        BigDecimal bd = CharacterConversions.toBigDecimal(testChar, DUMMY_CONVERTER);
        assertNotNull(bd);
        assertEquals(BigDecimal.valueOf(53), bd);
    }

    @Test
    public void testBigIntegerAndBigDecimal_WithZeroChar() {
        char zero = '\0';

        assertEquals(BigInteger.ZERO, CharacterConversions.toBigInteger(zero, DUMMY_CONVERTER));
        assertEquals(BigDecimal.ZERO, CharacterConversions.toBigDecimal(zero, DUMMY_CONVERTER));
    }
}
