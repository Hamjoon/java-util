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
 *   <li>Typical values for each conversion method.</li>
 *   <li>Edge values (e.g., {@code '\\0'}, {@code Character.MAX_VALUE}).</li>
 *   <li>Overflow / truncation behaviour for numeric casts.</li>
 *   <li>Incorrect input types that should trigger {@link ClassCastException}.</li>
 * </ul>
 *
 * The {@code Converter} argument is required by the API but is not used by the
 * implementation; a minimal instance is created with a {@code null}
 * {@code ConverterOptions} (the constructor accepts {@code null}).
 */
public class CharacterConversionsTest {

    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter implementation is not exercised by CharacterConversions,
        // but it must be supplied to satisfy the method signatures.
        dummyConverter = new Converter(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  toString()                                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringTypical() {
        Character ch = 'Z';
        assertEquals("Z", CharacterConversions.toString(ch, dummyConverter));
    }

    @Test
    public void testToStringNull() {
        assertEquals("null", CharacterConversions.toString(null, dummyConverter));
    }

    /* ---------------------------------------------------------------------- */
    /*  toBoolean()                                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBooleanTrueValues() {
        // All characters that should be interpreted as true.
        char[] trueChars = {1, 't', 'T', '1', 'y', 'Y'};
        for (char c : trueChars) {
            assertTrue("Failed for char: " + c,
                    CharacterConversions.toBoolean(c, dummyConverter));
        }
    }

    @Test
    public void testToBooleanFalseValues() {
        // Characters that are not mapped to true.
        char[] falseChars = {'0', 'f', 'F', 'n', 'N', ' ', 'a', '\u0002'};
        for (char c : falseChars) {
            assertFalse("Failed for char: " + c,
                    CharacterConversions.toBoolean(c, dummyConverter));
        }
    }

    @Test(expected = ClassCastException.class)
    public void testToBooleanInvalidType() {
        // Passing a non‑Character object must cause a ClassCastException.
        CharacterConversions.toBoolean("t", dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /*  toByte()                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByteWithinRange() {
        char c = 0x7F; // 127, max value for signed byte
        assertEquals((byte) 127, CharacterConversions.toByte(c, dummyConverter));
    }

    @Test
    public void testToByteOverflow() {
        char c = 0x80; // 128, overflows signed byte to -128
        assertEquals((byte) -128, CharacterConversions.toByte(c, dummyConverter));
    }

    @Test(expected = ClassCastException.class)
    public void testToByteInvalidType() {
        CharacterConversions.toByte(Boolean.TRUE, dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /*  toShort()                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToShortTypical() {
        char c = 12345;
        assertEquals((short) 12345, CharacterConversions.toShort(c, dummyConverter));
    }

    @Test
    public void testToShortMaxValue() {
        char c = Character.MAX_VALUE; // 65535, truncates to -1 in signed short
        assertEquals((short) -1, CharacterConversions.toShort(c, dummyConverter));
    }

    @Test(expected = ClassCastException.class)
    public void testToShortInvalidType() {
        CharacterConversions.toShort(123, dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /*  toInt()                                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToIntTypical() {
        char c = 'A'; // 65
        assertEquals(65, CharacterConversions.toInt(c, dummyConverter));
    }

    @Test
    public void testToIntMaxValue() {
        assertEquals(65535, CharacterConversions.toInt(Character.MAX_VALUE, dummyConverter));
    }

    @Test(expected = ClassCastException.class)
    public void testToIntInvalidType() {
        CharacterConversions.toInt(new Object(), dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /*  toLong()                                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToLongTypical() {
        char c = 'z'; // 122
        assertEquals(122L, CharacterConversions.toLong(c, dummyConverter));
    }

    @Test
    public void testToLongMaxValue() {
        assertEquals(65535L, CharacterConversions.toLong(Character.MAX_VALUE, dummyConverter));
    }

    @Test(expected = ClassCastException.class)
    public void testToLongInvalidType() {
        CharacterConversions.toLong(null, dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /*  toFloat()                                                              */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToFloatTypical() {
        char c = 123; // 123.0f
        assertEquals(123.0f, CharacterConversions.toFloat(c, dummyConverter), 0.0f);
    }

    @Test
    public void testToFloatMaxValue() {
        assertEquals(65535.0f, CharacterConversions.toFloat(Character.MAX_VALUE, dummyConverter), 0.0f);
    }

    @Test(expected = ClassCastException.class)
    public void testToFloatInvalidType() {
        CharacterConversions.toFloat(Boolean.FALSE, dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /*  toDouble()                                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToDoubleTypical() {
        char c = 200; // 200.0
        assertEquals(200.0, CharacterConversions.toDouble(c, dummyConverter), 0.0);
    }

    @Test
    public void testToDoubleMaxValue() {
        assertEquals(65535.0, CharacterConversions.toDouble(Character.MAX_VALUE, dummyConverter), 0.0);
    }

    @Test(expected = ClassCastException.class)
    public void testToDoubleInvalidType() {
        CharacterConversions.toDouble(3.14, dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /*  toAtomicInteger()                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicIntegerTypical() {
        char c = 42;
        AtomicInteger ai = CharacterConversions.toAtomicInteger(c, dummyConverter);
        assertNotNull(ai);
        assertEquals(42, ai.get());
    }

    @Test
    public void testToAtomicIntegerOverflow() {
        char c = (char) 300; // 300 > Byte.MAX_VALUE but fits int
        AtomicInteger ai = CharacterConversions.toAtomicInteger(c, dummyConverter);
        assertEquals(300, ai.get());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerInvalidType() {
        CharacterConversions.toAtomicInteger(new int[]{1,2,3}, dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /*  toAtomicLong()                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicLongTypical() {
        char c = 1234;
        AtomicLong al = CharacterConversions.toAtomicLong(c, dummyConverter);
        assertNotNull(al);
        assertEquals(1234L, al.get());
    }

    @Test
    public void testToAtomicLongMaxValue() {
        AtomicLong al = CharacterConversions.toAtomicLong(Character.MAX_VALUE, dummyConverter);
        assertEquals(65535L, al.get());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLongInvalidType() {
        CharacterConversions.toAtomicLong(Boolean.TRUE, dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /*  toAtomicBoolean()                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToAtomicBooleanTrue() {
        char c = 'y';
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean(c, dummyConverter);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        char c = 'n';
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean(c, dummyConverter);
        assertFalse(ab.get());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBooleanInvalidType() {
        CharacterConversions.toAtomicBoolean(new Object(), dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /*  toBigInteger()                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigIntegerTypical() {
        char c = 5000;
        BigInteger bi = CharacterConversions.toBigInteger(c, dummyConverter);
        assertEquals(BigInteger.valueOf(5000L), bi);
    }

    @Test
    public void testToBigIntegerMaxValue() {
        BigInteger bi = CharacterConversions.toBigInteger(Character.MAX_VALUE, dummyConverter);
        assertEquals(BigInteger.valueOf(65535L), bi);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigIntegerInvalidType() {
        CharacterConversions.toBigInteger(null, dummyConverter);
    }

    /* ---------------------------------------------------------------------- */
    /*  toBigDecimal()                                                         */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBigDecimalTypical() {
        char c = 42;
        BigDecimal bd = CharacterConversions.toBigDecimal(c, dummyConverter);
        assertEquals(BigDecimal.valueOf(42L), bd);
    }

    @Test
    public void testToBigDecimalMaxValue() {
        BigDecimal bd = CharacterConversions.toBigDecimal(Character.MAX_VALUE, dummyConverter);
        assertEquals(BigDecimal.valueOf(65535L), bd);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimalInvalidType() {
        CharacterConversions.toBigDecimal(new String("abc"), dummyConverter);
    }
}
