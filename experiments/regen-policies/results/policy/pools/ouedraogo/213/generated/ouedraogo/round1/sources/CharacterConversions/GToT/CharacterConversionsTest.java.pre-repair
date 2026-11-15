package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit‑4 test suite for {@link CharacterConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal/typical usage for every public static conversion method</li>
 *   <li>edge‑case values (e.g. {@code '\\u0000'}, {@code '\\uFFFF'}, high/low numeric ranges)</li>
 *   <li>null handling – the implementation does not guard against {@code null},
 *       so a {@link NullPointerException} is expected.</li>
 *   <li>invalid source types – casting a non‑character object should raise a
 *       {@link ClassCastException} at runtime.</li>
 * </ul>
 *
 * The {@link Converter} instance required by the conversion methods is created
 * with a default {@link ConverterOptions} (the real implementation is not
 * needed for these static helpers – they ignore the converter argument).
 */
public class CharacterConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        this.converter = new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------
     *  toString()
     * ------------------------------------------------- */

    @Test
    public void testToString_WithCharacter() {
        Character ch = 'A';
        String result = CharacterConversions.toString(ch, converter);
        assertEquals("A", result);
    }

    @Test
    public void testToString_WithNull() {
        String result = CharacterConversions.toString(null, converter);
        // "" + null produces the literal "null"
        assertEquals("null", result);
    }

    /* -------------------------------------------------
     *  toBoolean()
     * ------------------------------------------------- */

    @Test
    public void testToBoolean_TrueValues() {
        char[] trueChars = {1, 't', 'T', '1', 'y', 'Y'};
        for (char c : trueChars) {
            assertTrue("Expected true for char: " + (int) c,
                    CharacterConversions.toBoolean(c, converter));
        }
    }

    @Test
    public void testToBoolean_FalseValues() {
        char[] falseChars = {'0', 'f', 'F', 'n', 'N', 'a', 'Z', '\\u0000', '\\uFFFF'};
        for (char c : falseChars) {
            assertFalse("Expected false for char: " + (int) c,
                    CharacterConversions.toBoolean(c, converter));
        }
    }

    @Test(expected = NullPointerException.class)
    public void testToBoolean_NullInput() {
        CharacterConversions.toBoolean(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBoolean_InvalidType() {
        // Passing an Integer should cause a ClassCastException when casting to char
        CharacterConversions.toBoolean(Integer.valueOf(65), converter);
    }

    /* -------------------------------------------------
     *  toByte()
     * ------------------------------------------------- */

    @Test
    public void testToByte_TypicalValue() {
        Character ch = 'A'; // 65
        byte result = CharacterConversions.toByte(ch, converter);
        assertEquals((byte) 65, result);
    }

    @Test
    public void testToByte_EdgeValues() {
        // char max value 0xFFFF = 65535 -> low 8 bits = 0xFF = -1 as signed byte
        Character ch = '\uFFFF';
        byte result = CharacterConversions.toByte(ch, converter);
        assertEquals((byte) 0xFF, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToByte_NullInput() {
        CharacterConversions.toByte(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToByte_InvalidType() {
        CharacterConversions.toByte(Integer.valueOf(100), converter);
    }

    /* -------------------------------------------------
     *  toShort()
     * ------------------------------------------------- */

    @Test
    public void testToShort_TypicalValue() {
        Character ch = 'Ā'; // 256
        short result = CharacterConversions.toShort(ch, converter);
        assertEquals((short) 256, result);
    }

    @Test
    public void testToShort_EdgeValues() {
        Character ch = '\uFFFF'; // 65535
        short result = CharacterConversions.toShort(ch, converter);
        // 65535 as signed short = -1
        assertEquals((short) -1, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToShort_NullInput() {
        CharacterConversions.toShort(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToShort_InvalidType() {
        CharacterConversions.toShort(Boolean.TRUE, converter);
    }

    /* -------------------------------------------------
     *  toInt()
     * ------------------------------------------------- */

    @Test
    public void testToInt_TypicalValue() {
        Character ch = 'Ω'; // Unicode 937
        int result = CharacterConversions.toInt(ch, converter);
        assertEquals(937, result);
    }

    @Test
    public void testToInt_EdgeValues() {
        Character ch = '\uFFFF'; // 65535
        int result = CharacterConversions.toInt(ch, converter);
        assertEquals(65535, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToInt_NullInput() {
        CharacterConversions.toInt(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_InvalidType() {
        CharacterConversions.toInt("X", converter);
    }

    /* -------------------------------------------------
     *  toLong()
     * ------------------------------------------------- */

    @Test
    public void testToLong_TypicalValue() {
        Character ch = '€'; // 8364
        long result = CharacterConversions.toLong(ch, converter);
        assertEquals(8364L, result);
    }

    @Test
    public void testToLong_EdgeValues() {
        Character ch = '\uFFFF';
        long result = CharacterConversions.toLong(ch, converter);
        assertEquals(65535L, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        CharacterConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_InvalidType() {
        CharacterConversions.toLong(Double.valueOf(1.0), converter);
    }

    /* -------------------------------------------------
     *  toFloat()
     * ------------------------------------------------- */

    @Test
    public void testToFloat_TypicalValue() {
        Character ch = 'π'; // 960
        float result = CharacterConversions.toFloat(ch, converter);
        assertEquals(960.0f, result, 0.0f);
    }

    @Test
    public void testToFloat_EdgeValues() {
        Character ch = '\uFFFF';
        float result = CharacterConversions.toFloat(ch, converter);
        assertEquals(65535.0f, result, 0.0f);
    }

    @Test(expected = NullPointerException.class)
    public void testToFloat_NullInput() {
        CharacterConversions.toFloat(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToFloat_InvalidType() {
        CharacterConversions.toFloat(new Object(), converter);
    }

    /* -------------------------------------------------
     *  toDouble()
     * ------------------------------------------------- */

    @Test
    public void testToDouble_TypicalValue() {
        Character ch = 'λ'; // 955
        double result = CharacterConversions.toDouble(ch, converter);
        assertEquals(955.0, result, 0.0);
    }

    @Test
    public void testToDouble_EdgeValues() {
        Character ch = '\uFFFF';
        double result = CharacterConversions.toDouble(ch, converter);
        assertEquals(65535.0, result, 0.0);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput() {
        CharacterConversions.toDouble(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_InvalidType() {
        CharacterConversions.toDouble(Long.valueOf(10L), converter);
    }

    /* -------------------------------------------------
     *  toAtomicInteger()
     * ------------------------------------------------- */

    @Test
    public void testToAtomicInteger_Typical() {
        Character ch = 'B'; // 66
        AtomicInteger ai = CharacterConversions.toAtomicInteger(ch, converter);
        assertEquals(66, ai.get());
    }

    @Test
    public void testToAtomicInteger_Edge() {
        Character ch = '\uFFFF';
        AtomicInteger ai = CharacterConversions.toAtomicInteger(ch, converter);
        assertEquals(65535, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullInput() {
        CharacterConversions.toAtomicInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_InvalidType() {
        CharacterConversions.toAtomicInteger(Boolean.FALSE, converter);
    }

    /* -------------------------------------------------
     *  toAtomicLong()
     * ------------------------------------------------- */

    @Test
    public void testToAtomicLong_Typical() {
        Character ch = 'C'; // 67
        AtomicLong al = CharacterConversions.toAtomicLong(ch, converter);
        assertEquals(67L, al.get());
    }

    @Test
    public void testToAtomicLong_Edge() {
        Character ch = '\uFFFF';
        AtomicLong al = CharacterConversions.toAtomicLong(ch, converter);
        assertEquals(65535L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput() {
        CharacterConversions.toAtomicLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_InvalidType() {
        CharacterConversions.toAtomicLong("Z", converter);
    }

    /* -------------------------------------------------
     *  toAtomicBoolean()
     * ------------------------------------------------- */

    @Test
    public void testToAtomicBoolean_True() {
        Character ch = 't';
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean(ch, converter);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBoolean_False() {
        Character ch = 'f';
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean(ch, converter);
        assertFalse(ab.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBoolean_NullInput() {
        CharacterConversions.toAtomicBoolean(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBoolean_InvalidType() {
        CharacterConversions.toAtomicBoolean(Integer.valueOf(0), converter);
    }

    /* -------------------------------------------------
     *  toBigInteger()
     * ------------------------------------------------- */

    @Test
    public void testToBigInteger_Typical() {
        Character ch = 'D'; // 68
        BigInteger bi = CharacterConversions.toBigInteger(ch, converter);
        assertEquals(BigInteger.valueOf(68), bi);
    }

    @Test
    public void testToBigInteger_Edge() {
        Character ch = '\uFFFF';
        BigInteger bi = CharacterConversions.toBigInteger(ch, converter);
        assertEquals(BigInteger.valueOf(65535L), bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        CharacterConversions.toBigInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_InvalidType() {
        CharacterConversions.toBigInteger(new Object(), converter);
    }

    /* -------------------------------------------------
     *  toBigDecimal()
     * ------------------------------------------------- */

    @Test
    public void testToBigDecimal_Typical() {
        Character ch = 'E'; // 69
        BigDecimal bd = CharacterConversions.toBigDecimal(ch, converter);
        assertEquals(BigDecimal.valueOf(69), bd);
    }

    @Test
    public void testToBigDecimal_Edge() {
        Character ch = '\uFFFF';
        BigDecimal bd = CharacterConversions.toBigDecimal(ch, converter);
        assertEquals(BigDecimal.valueOf(65535L), bd);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        CharacterConversions.toBigDecimal(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_InvalidType() {
        CharacterConversions.toBigDecimal(Boolean.TRUE, converter);
    }
}
