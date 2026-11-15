package com.cedarsoftware.util.convert;

import org.junit.Test;
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
 *   <li>Typical usage for each conversion method.</li>
 *   <li>Edge cases such as the minimum and maximum {@code char} values.</li>
 *   <li>Invalid input types that should result in {@link ClassCastException}.</li>
 * </ul>
 *
 * A single dummy {@link Converter} instance is used because the conversion
 * methods ignore the {@code Converter} argument – they operate solely on the
 * {@code from} object.
 */
public class CharacterConversionsTest {

    /** The {@code Converter} argument is not used by the static methods, so a dummy instance is sufficient. */
    private static final Converter DUMMY_CONVERTER = new Converter(null);

    /* --------------------------------------------------------------------- */
    /* toString                                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToStringWithVariousObjects() {
        assertEquals("A", CharacterConversions.toString('A', DUMMY_CONVERTER));
        assertEquals("123", CharacterConversions.toString(123, DUMMY_CONVERTER));
        assertEquals("true", CharacterConversions.toString(true, DUMMY_CONVERTER));
        assertEquals("java.lang.Object@",
                CharacterConversions.toString(new Object(), DUMMY_CONVERTER).substring(0, "java.lang.Object@".length()));
    }

    @Test
    public void testToStringWithNull() {
        assertEquals("null", CharacterConversions.toString(null, DUMMY_CONVERTER));
    }

    /* --------------------------------------------------------------------- */
    /* toBoolean                                                              */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToBooleanTrueValues() {
        // Standard true characters
        assertTrue(CharacterConversions.toBoolean('t', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('T', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('1', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('y', DUMMY_CONVERTER));
        assertTrue(CharacterConversions.toBoolean('Y', DUMMY_CONVERTER));
        // Control character with numeric value 1
        assertTrue(CharacterConversions.toBoolean((char) 1, DUMMY_CONVERTER));
    }

    @Test
    public void testToBooleanFalseValues() {
        assertFalse(CharacterConversions.toBoolean('f', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('F', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('0', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('n', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('N', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean('a', DUMMY_CONVERTER));
        assertFalse(CharacterConversions.toBoolean(' ', DUMMY_CONVERTER));
    }

    @Test(expected = ClassCastException.class)
    public void testToBooleanInvalidTypeThrows() {
        // Passing a String should cause a ClassCastException when casting to char
        CharacterConversions.toBoolean("t", DUMMY_CONVERTER);
    }

    /* --------------------------------------------------------------------- */
    /* Numeric primitive conversions                                           */
    /* --------------------------------------------------------------------- */

    @Test
    public void testNumericConversionsTypicalChar() {
        char c = 'A'; // 65
        assertEquals((byte) 65, CharacterConversions.toByte(c, DUMMY_CONVERTER));
        assertEquals((short) 65, CharacterConversions.toShort(c, DUMMY_CONVERTER));
        assertEquals(65, CharacterConversions.toInt(c, DUMMY_CONVERTER));
        assertEquals(65L, CharacterConversions.toLong(c, DUMMY_CONVERTER));
        assertEquals(65.0f, CharacterConversions.toFloat(c, DUMMY_CONVERTER), 0.0f);
        assertEquals(65.0d, CharacterConversions.toDouble(c, DUMMY_CONVERTER), 0.0d);
    }

    @Test
    public void testNumericConversionsEdgeChars() {
        // Minimum char value
        char min = '\u0000'; // 0
        assertEquals((byte) 0, CharacterConversions.toByte(min, DUMMY_CONVERTER));
        assertEquals((short) 0, CharacterConversions.toShort(min, DUMMY_CONVERTER));
        assertEquals(0, CharacterConversions.toInt(min, DUMMY_CONVERTER));
        assertEquals(0L, CharacterConversions.toLong(min, DUMMY_CONVERTER));
        assertEquals(0.0f, CharacterConversions.toFloat(min, DUMMY_CONVERTER), 0.0f);
        assertEquals(0.0d, CharacterConversions.toDouble(min, DUMMY_CONVERTER), 0.0d);

        // Maximum char value (65535)
        char max = '\uFFFF';
        assertEquals((byte) -1, CharacterConversions.toByte(max, DUMMY_CONVERTER)); // 65535 -> 0xFFFF -> -1 as byte
        assertEquals((short) -1, CharacterConversions.toShort(max, DUMMY_CONVERTER)); // -1 as short
        assertEquals(65535, CharacterConversions.toInt(max, DUMMY_CONVERTER));
        assertEquals(65535L, CharacterConversions.toLong(max, DUMMY_CONVERTER));
        assertEquals(65535.0f, CharacterConversions.toFloat(max, DUMMY_CONVERTER), 0.0f);
        assertEquals(65535.0d, CharacterConversions.toDouble(max, DUMMY_CONVERTER), 0.0d);
    }

    @Test(expected = ClassCastException.class)
    public void testNumericConversionInvalidTypeThrows() {
        CharacterConversions.toInt("A", DUMMY_CONVERTER);
    }

    /* --------------------------------------------------------------------- */
    /* Atomic conversions                                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAtomicIntegerConversion() {
        char c = 'b'; // 98
        AtomicInteger ai = CharacterConversions.toAtomicInteger(c, DUMMY_CONVERTER);
        assertEquals(98, ai.get());
    }

    @Test
    public void testAtomicLongConversion() {
        char c = '\u00FF'; // 255
        AtomicLong al = CharacterConversions.toAtomicLong(c, DUMMY_CONVERTER);
        assertEquals(255L, al.get());
    }

    @Test
    public void testAtomicBooleanConversionTrue() {
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean('Y', DUMMY_CONVERTER);
        assertTrue(ab.get());
    }

    @Test
    public void testAtomicBooleanConversionFalse() {
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean('n', DUMMY_CONVERTER);
        assertFalse(ab.get());
    }

    @Test(expected = ClassCastException.class)
    public void testAtomicConversionInvalidTypeThrows() {
        CharacterConversions.toAtomicInteger(123, DUMMY_CONVERTER);
    }

    /* --------------------------------------------------------------------- */
    /* BigInteger / BigDecimal conversions                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testBigIntegerConversion() {
        char c = 'Z'; // 90
        BigInteger bi = CharacterConversions.toBigInteger(c, DUMMY_CONVERTER);
        assertEquals(BigInteger.valueOf(90), bi);
    }

    @Test
    public void testBigDecimalConversion() {
        char c = '\u00A0'; // 160
        BigDecimal bd = CharacterConversions.toBigDecimal(c, DUMMY_CONVERTER);
        assertEquals(BigDecimal.valueOf(160), bd);
    }

    @Test(expected = ClassCastException.class)
    public void testBigIntegerInvalidTypeThrows() {
        CharacterConversions.toBigInteger(true, DUMMY_CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testBigDecimalInvalidTypeThrows() {
        CharacterConversions.toBigDecimal(new Object(), DUMMY_CONVERTER);
    }
}
