package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class CharacterConversionsTest {

    // -----------------------------------------------------------------
    // Helper – a dummy Converter instance (methods do not use it)
    // -----------------------------------------------------------------
    private static final Converter DUMMY_CONVERTER = null;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // -----------------------------------------------------------------
    // 1. toString
    // -----------------------------------------------------------------
    @Test
    public void testToString_basic() {
        Character input = 'Z';
        String result = CharacterConversions.toString(input, DUMMY_CONVERTER);
        assertEquals("Z", result);
    }

    @Test
    public void testToString_nullInput() {
        thrown.expect(NullPointerException.class);
        CharacterConversions.toString(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // 2. toBoolean
    // -----------------------------------------------------------------
    @Test
    public void testToBoolean_trueValues() {
        assertTrue(CharacterConversions.toBoolean((char) 1, DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('t', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('T', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('1', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('y', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('Y', DUMMY_CONVERTER));
    }

    @Test
    public void testToBoolean_falseValues() {
        assertFalse(CharacterConversions.toBoolean('f', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('0', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('n', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('N', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('a', DUMMY_CONVERTER));
    }

    @Test
    public void testToBoolean_nullInput() {
        thrown.expect(NullPointerException.class);
        CharacterConversions.toBoolean(null, DUMMY_CONVERTER);
    }

    @Test
    public void testToBoolean_invalidType() {
        thrown.expect(ClassCastException.class);
        CharacterConversions.toBoolean(Integer.valueOf(5), DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // 3. toByte
    // -----------------------------------------------------------------
    @Test
    public void testToByte_basic() {
        char ch = 0x7F; // 127, fits in a byte
        byte result = CharacterConversions.toByte(ch, DUMMY_CONVERTER);
        assertEquals((byte) 127, result);
    }

    @Test
    public void testToByte_overflow() {
        char ch = 0x0100; // 256 -> wraps to 0 when cast to byte
        byte result = CharacterConversions.toByte(ch, DUMMY_CONVERTER);
        assertEquals((byte) 0, result);
    }

    @Test
    public void testToByte_nullInput() {
        thrown.expect(NullPointerException.class);
        CharacterConversions.toByte(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // 4. toShort
    // -----------------------------------------------------------------
    @Test
    public void testToShort_basic() {
        char ch = 0x1234;
        short result = CharacterConversions.toShort(ch, DUMMY_CONVERTER);
        assertEquals((short) 0x1234, result);
    }

    @Test
    public void testToShort_overflow() {
        char ch = 0x8000; // 32768 -> -32768 when cast to short
        short result = CharacterConversions.toShort(ch, DUMMY_CONVERTER);
        assertEquals((short) -32768, result);
    }

    @Test
    public void testToShort_nullInput() {
        thrown.expect(NullPointerException.class);
        CharacterConversions.toShort(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // 5. toInt
    // -----------------------------------------------------------------
    @Test
    public void testToInt_basic() {
        char ch = 'A';
        int result = CharacterConversions.toInt(ch, DUMMY_CONVERTER);
        assertEquals(65, result);
    }

    @Test
    public void testToInt_maxChar() {
        char ch = Character.MAX_VALUE; // 65535
        int result = CharacterConversions.toInt(ch, DUMMY_CONVERTER);
        assertEquals(65535, result);
    }

    @Test
    public void testToInt_nullInput() {
        thrown.expect(NullPointerException.class);
        CharacterConversions.toInt(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // 6. toLong
    // -----------------------------------------------------------------
    @Test
    public void testToLong_basic() {
        char ch = 'z';
        long result = CharacterConversions.toLong(ch, DUMMY_CONVERTER);
        assertEquals(122L, result);
    }

    @Test
    public void testToLong_nullInput() {
        thrown.expect(NullPointerException.class);
        CharacterConversions.toLong(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // 7. toFloat
    // -----------------------------------------------------------------
    @Test
    public void testToFloat_basic() {
        char ch = 100;
        float result = CharacterConversions.toFloat(ch, DUMMY_CONVERTER);
        assertEquals(100f, result, 0.0f);
    }

    @Test
    public void testToFloat_nullInput() {
        thrown.expect(NullPointerException.class);
        CharacterConversions.toFloat(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // 8. toDouble
    // -----------------------------------------------------------------
    @Test
    public void testToDouble_basic() {
        char ch = 200;
        double result = CharacterConversions.toDouble(ch, DUMMY_CONVERTER);
        assertEquals(200d, result, 0.0);
    }

    @Test
    public void testToDouble_nullInput() {
        thrown.expect(NullPointerException.class);
        CharacterConversions.toDouble(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // 9. toAtomicInteger
    // -----------------------------------------------------------------
    @Test
    public void testToAtomicInteger_basic() {
        char ch = 42;
        AtomicInteger ai = CharacterConversions.toAtomicInteger(ch, DUMMY_CONVERTER);
        assertNotNull(ai);
        assertEquals(42, ai.get());
    }

    @Test
    public void testToAtomicInteger_nullInput() {
        thrown.expect(NullPointerException.class);
        CharacterConversions.toAtomicInteger(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // 10. toAtomicLong
    // -----------------------------------------------------------------
    @Test
    public void testToAtomicLong_basic() {
        char ch = 12345;
        AtomicLong al = CharacterConversions.toAtomicLong(ch, DUMMY_CONVERTER);
        assertNotNull(al);
        assertEquals(12345L, al.get());
    }

    @Test
    public void testToAtomicLong_nullInput() {
        thrown.expect(NullPointerException.class);
        CharacterConversions.toAtomicLong(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // 11. toAtomicBoolean
    // -----------------------------------------------------------------
    @Test
    public void testToAtomicBoolean_true() {
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean('Y', DUMMY_CONVERTER);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBoolean_false() {
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean('N', DUMMY_CONVERTER);
        assertFalse(ab.get());
    }

    @Test
    public void testToAtomicBoolean_nullInput() {
        thrown.expect(NullPointerException.class);
        CharacterConversions.toAtomicBoolean(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // 12. toBigInteger
    // -----------------------------------------------------------------
    @Test
    public void testToBigInteger_basic() {
        char ch = 5000;
        BigInteger bi = CharacterConversions.toBigInteger(ch, DUMMY_CONVERTER);
        assertEquals(BigInteger.valueOf(5000L), bi);
    }

    @Test
    public void testToBigInteger_nullInput() {
        thrown.expect(NullPointerException.class);
        CharacterConversions.toBigInteger(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // 13. toBigDecimal
    // -----------------------------------------------------------------
    @Test
    public void testToBigDecimal_basic() {
        char ch = 7777;
        BigDecimal bd = CharacterConversions.toBigDecimal(ch, DUMMY_CONVERTER);
        assertEquals(BigDecimal.valueOf(7777L), bd);
    }

    @Test
    public void testToBigDecimal_nullInput() {
        thrown.expect(NullPointerException.class);
        CharacterConversions.toBigDecimal(null, DUMMY_CONVERTER);
    }

    // -----------------------------------------------------------------
    // 14. Invalid type handling (ClassCastException) for numeric conversions
    // -----------------------------------------------------------------
    @Test
    public void testNumericConversion_invalidType() {
        Object notAChar = "not a char";
        // toByte
        thrown.expect(ClassCastException.class);
        CharacterConversions.toByte(notAChar, DUMMY_CONVERTER);
    }

    @Test
    public void testShortConversion_invalidType() {
        thrown.expect(ClassCastException.class);
        CharacterConversions.toShort(123, DUMMY_CONVERTER);
    }

    @Test
    public void testIntConversion_invalidType() {
        thrown.expect(ClassCastException.class);
        CharacterConversions.toInt(new Object(), DUMMY_CONVERTER);
    }

    @Test
    public void testLongConversion_invalidType() {
        thrown.expect(ClassCastException.class);
        CharacterConversions.toLong(Boolean.TRUE, DUMMY_CONVERTER);
    }

    @Test
    public void testFloatConversion_invalidType() {
        thrown.expect(ClassCastException.class);
        CharacterConversions.toFloat(Double.valueOf(1.23), DUMMY_CONVERTER);
    }

    @Test
    public void testDoubleConversion_invalidType() {
        thrown.expect(ClassCastException.class);
        CharacterConversions.toDouble(new int[]{1,2,3}, DUMMY_CONVERTER);
    }
}
