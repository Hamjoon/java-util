package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
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
 *   <li>Normal/typical values for each conversion method.</li>
 *   <li>Edge cases (boundary characters, sign‑extension, zero, control characters).</li>
 *   <li>Error scenarios (null input, wrong type causing {@code ClassCastException}).</li>
 * </ul>
 *
 * The {@code Converter} argument is not used by the implementation, therefore a {@code null}
 * reference is sufficient for all tests – it mimics the real usage without pulling in the rest
 * of the library.
 */
public class CharacterConversionsTest {

    /** A dummy {@code Converter} reference – the static methods ignore it. */
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter constructor requires a ConverterOptions instance,
        // but the implementation of CharacterConversions never touches the
        // Converter argument, so we safely use {@code null}.
        dummyConverter = null;
    }

    @After
    public void tearDown() {
        dummyConverter = null;
    }

    /* -------------------------------------------------------
     * toString()
     * ------------------------------------------------------- */

    @Test
    public void testToStringWithRegularObject() {
        Integer i = 42;
        assertEquals("42", CharacterConversions.toString(i, dummyConverter));
    }

    @Test
    public void testToStringWithCharacter() {
        Character c = 'Z';
        assertEquals("Z", CharacterConversions.toString(c, dummyConverter));
    }

    @Test
    public void testToStringWithNull() {
        // "" + null results in the literal string "null"
        assertEquals("null", CharacterConversions.toString(null, dummyConverter));
    }

    /* -------------------------------------------------------
     * toBoolean()
     * ------------------------------------------------------- */

    @Test
    public void testToBooleanTrueValues() {
        // true characters according to the implementation
        char[] trueChars = { 1, 't', 'T', '1', 'y', 'Y' };
        for (char ch : trueChars) {
            assertTrue("char '" + ch + "' should be true",
                    CharacterConversions.toBoolean(ch, dummyConverter));
        }
    }

    @Test
    public void testToBooleanFalseValues() {
        char[] falseChars = { 0, 'f', 'F', '0', 'n', 'N', 'a', 'Z', ' ' };
        for (char ch : falseChars) {
            assertFalse("char '" + ch + "' should be false",
                    CharacterConversions.toBoolean(ch, dummyConverter));
        }
    }

    @Test(expected = ClassCastException.class)
    public void testToBooleanWithWrongType() {
        // Passing an Integer forces a cast to char and throws ClassCastException
        CharacterConversions.toBoolean(Integer.valueOf(5), dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBooleanWithNull() {
        CharacterConversions.toBoolean(null, dummyConverter);
    }

    /* -------------------------------------------------------
     * toByte()
     * ------------------------------------------------------- */

    @Test
    public void testToByteWithinByteRange() {
        char ch = 'A'; // 65
        assertEquals((byte) 65, CharacterConversions.toByte(ch, dummyConverter));
    }

    @Test
    public void testToByteOverflow() {
        char ch = (char) 0xFF; // 255 -> byte -1
        assertEquals((byte) -1, CharacterConversions.toByte(ch, dummyConverter));
    }

    @Test(expected = ClassCastException.class)
    public void testToByteWithWrongType() {
        CharacterConversions.toByte("X", dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteWithNull() {
        CharacterConversions.toByte(null, dummyConverter);
    }

    /* -------------------------------------------------------
     * toShort()
     * ------------------------------------------------------- */

    @Test
    public void testToShortWithinShortRange() {
        char ch = (char) 12345;
        assertEquals((short) 12345, CharacterConversions.toShort(ch, dummyConverter));
    }

    @Test
    public void testToShortOverflow() {
        char ch = (char) 0xFFFF; // 65535 -> short -1
        assertEquals((short) -1, CharacterConversions.toShort(ch, dummyConverter));
    }

    @Test(expected = ClassCastException.class)
    public void testToShortWithWrongType() {
        CharacterConversions.toShort(Boolean.TRUE, dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToShortWithNull() {
        CharacterConversions.toShort(null, dummyConverter);
    }

    /* -------------------------------------------------------
     * toInt()
     * ------------------------------------------------------- */

    @Test
    public void testToIntSimple() {
        char ch = 'z';
        assertEquals((int) 'z', CharacterConversions.toInt(ch, dummyConverter));
    }

    @Test
    public void testToIntMaximumChar() {
        char ch = Character.MAX_VALUE; // 0xFFFF == 65535
        assertEquals(65535, CharacterConversions.toInt(ch, dummyConverter));
    }

    @Test(expected = ClassCastException.class)
    public void testToIntWithWrongType() {
        CharacterConversions.toInt(new Object(), dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToIntWithNull() {
        CharacterConversions.toInt(null, dummyConverter);
    }

    /* -------------------------------------------------------
     * toLong()
     * ------------------------------------------------------- */

    @Test
    public void testToLongSimple() {
        char ch = 'g';
        assertEquals((long) 'g', CharacterConversions.toLong(ch, dummyConverter));
    }

    @Test
    public void testToLongMaximumChar() {
        char ch = Character.MAX_VALUE;
        assertEquals(65535L, CharacterConversions.toLong(ch, dummyConverter));
    }

    @Test(expected = ClassCastException.class)
    public void testToLongWithWrongType() {
        CharacterConversions.toLong(Double.valueOf(1.0), dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLongWithNull() {
        CharacterConversions.toLong(null, dummyConverter);
    }

    /* -------------------------------------------------------
     * toFloat()
     * ------------------------------------------------------- */

    @Test
    public void testToFloatSimple() {
        char ch = '5';
        assertEquals((float) '5', CharacterConversions.toFloat(ch, dummyConverter), 0.0f);
    }

    @Test
    public void testToFloatMaximumChar() {
        char ch = Character.MAX_VALUE;
        assertEquals(65535.0f, CharacterConversions.toFloat(ch, dummyConverter), 0.0f);
    }

    @Test(expected = ClassCastException.class)
    public void testToFloatWithWrongType() {
        CharacterConversions.toFloat(Boolean.FALSE, dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToFloatWithNull() {
        CharacterConversions.toFloat(null, dummyConverter);
    }

    /* -------------------------------------------------------
     * toDouble()
     * ------------------------------------------------------- */

    @Test
    public void testToDoubleSimple() {
        char ch = '7';
        assertEquals((double) '7', CharacterConversions.toDouble(ch, dummyConverter), 0.0);
    }

    @Test
    public void testToDoubleMaximumChar() {
        char ch = Character.MAX_VALUE;
        assertEquals(65535.0, CharacterConversions.toDouble(ch, dummyConverter), 0.0);
    }

    @Test(expected = ClassCastException.class)
    public void testToDoubleWithWrongType() {
        CharacterConversions.toDouble(new int[]{1,2,3}, dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleWithNull() {
        CharacterConversions.toDouble(null, dummyConverter);
    }

    /* -------------------------------------------------------
     * toAtomicInteger()
     * ------------------------------------------------------- */

    @Test
    public void testToAtomicInteger() {
        char ch = 'K'; // 75
        AtomicInteger ai = CharacterConversions.toAtomicInteger(ch, dummyConverter);
        assertEquals(75, ai.get());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerWithWrongType() {
        CharacterConversions.toAtomicInteger(Long.valueOf(100L), dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerWithNull() {
        CharacterConversions.toAtomicInteger(null, dummyConverter);
    }

    /* -------------------------------------------------------
     * toAtomicLong()
     * ------------------------------------------------------- */

    @Test
    public void testToAtomicLong() {
        char ch = 'm'; // 109
        AtomicLong al = CharacterConversions.toAtomicLong(ch, dummyConverter);
        assertEquals(109L, al.get());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLongWithWrongType() {
        CharacterConversions.toAtomicLong("oops", dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongWithNull() {
        CharacterConversions.toAtomicLong(null, dummyConverter);
    }

    /* -------------------------------------------------------
     * toAtomicBoolean()
     * ------------------------------------------------------- */

    @Test
    public void testToAtomicBooleanTrue() {
        char ch = 'Y';
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean(ch, dummyConverter);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        char ch = 'n';
        AtomicBoolean ab = CharacterConversions.toAtomicBoolean(ch, dummyConverter);
        assertFalse(ab.get());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBooleanWithWrongType() {
        CharacterConversions.toAtomicBoolean(Boolean.TRUE, dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBooleanWithNull() {
        CharacterConversions.toAtomicBoolean(null, dummyConverter);
    }

    /* -------------------------------------------------------
     * toBigInteger()
     * ------------------------------------------------------- */

    @Test
    public void testToBigIntegerSimple() {
        char ch = 'A'; // 65
        BigInteger bi = CharacterConversions.toBigInteger(ch, dummyConverter);
        assertEquals(BigInteger.valueOf(65), bi);
    }

    @Test
    public void testToBigIntegerMaximumChar() {
        char ch = Character.MAX_VALUE;
        BigInteger bi = CharacterConversions.toBigInteger(ch, dummyConverter);
        assertEquals(BigInteger.valueOf(65535L), bi);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigIntegerWithWrongType() {
        CharacterConversions.toBigInteger(new Object(), dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerWithNull() {
        CharacterConversions.toBigInteger(null, dummyConverter);
    }

    /* -------------------------------------------------------
     * toBigDecimal()
     * ------------------------------------------------------- */

    @Test
    public void testToBigDecimalSimple() {
        char ch = 'B'; // 66
        BigDecimal bd = CharacterConversions.toBigDecimal(ch, dummyConverter);
        assertEquals(BigDecimal.valueOf(66L), bd);
    }

    @Test
    public void testToBigDecimalMaximumChar() {
        char ch = Character.MAX_VALUE;
        BigDecimal bd = CharacterConversions.toBigDecimal(ch, dummyConverter);
        assertEquals(BigDecimal.valueOf(65535L), bd);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimalWithWrongType() {
        CharacterConversions.toBigDecimal(Boolean.FALSE, dummyConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalWithNull() {
        CharacterConversions.toBigDecimal(null, dummyConverter);
    }
}
