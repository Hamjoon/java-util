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
 * Comprehensive JUnit‑4 test suite for {@link CharacterConversions}.
 *
 * The tests cover every static conversion method, exercising typical,
 * boundary and “false‑positive” values to ensure the implementation
 * behaves exactly as specified.
 */
public class CharacterConversionsTest {

    /** A dummy {@link Converter} instance – the conversion methods under test do not use it. */
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The Converter API requires a ConverterOptions instance; passing {@code null}
        // is acceptable for the purposes of these tests because the methods under test
        // never dereference the Converter argument.
        dummyConverter = new Converter(null);
    }

    /* --------------------------------------------------------------------- *
     *  toString
     * --------------------------------------------------------------------- */

    @Test
    public void testToString_WithNull() {
        assertEquals("null", CharacterConversions.toString(null, dummyConverter));
    }

    @Test
    public void testToString_WithCharacterObject() {
        Character ch = 'Z';
        assertEquals("Z", CharacterConversions.toString(ch, dummyConverter));
    }

    @Test
    public void testToString_WithPrimitiveCharBoxed() {
        // Autoboxed char is a Character, same as above – ensure no special handling.
        assertEquals("a", CharacterConversions.toString('a', dummyConverter));
    }

    /* --------------------------------------------------------------------- *
     *  toBoolean
     * --------------------------------------------------------------------- */

    @Test
    public void testToBoolean_TrueValues() {
        // Values that must evaluate to true according to the implementation
        assertTrue(CharacterConversions.toBoolean((char) 1, dummyConverter));
        assertTrue(CharacterConversions.toBoolean('t', dummyConverter));
        assertTrue(CharacterConversions.toBoolean('T', dummyConverter));
        assertTrue(CharacterConversions.toBoolean('1', dummyConverter));
        assertTrue(CharacterConversions.toBoolean('y', dummyConverter));
        assertTrue(CharacterConversions.toBoolean('Y', dummyConverter));
    }

    @Test
    public void testToBoolean_FalseValues() {
        // Anything else should be false
        assertFalse(CharacterConversions.toBoolean('0', dummyConverter));
        assertFalse(CharacterConversions.toBoolean('f', dummyConverter));
        assertFalse(CharacterConversions.toBoolean('F', dummyConverter));
        assertFalse(CharacterConversions.toBoolean('n', dummyConverter));
        assertFalse(CharacterConversions.toBoolean('N', dummyConverter));
        assertFalse(CharacterConversions.toBoolean('z', dummyConverter));
        assertFalse(CharacterConversions.toBoolean('\u0002', dummyConverter)); // another non‑true char
    }

    /* --------------------------------------------------------------------- *
     *  Primitive numeric conversions (byte, short, int, long, float, double)
     * --------------------------------------------------------------------- */

    @Test
    public void testNumericConversions_AllTypes() {
        char sample = 'A'; // Unicode 65
        assertEquals((byte) 65, CharacterConversions.toByte(sample, dummyConverter));
        assertEquals((short) 65, CharacterConversions.toShort(sample, dummyConverter));
        assertEquals(65, CharacterConversions.toInt(sample, dummyConverter));
        assertEquals(65L, CharacterConversions.toLong(sample, dummyConverter));
        assertEquals(65.0f, CharacterConversions.toFloat(sample, dummyConverter), 0.0f);
        assertEquals(65.0d, CharacterConversions.toDouble(sample, dummyConverter), 0.0d);
    }

    @Test
    public void testNumericConversions_EdgeValues() {
        // Minimum char value
        char min = Character.MIN_VALUE; // '\u0000' => 0
        assertEquals((byte) 0, CharacterConversions.toByte(min, dummyConverter));
        assertEquals((short) 0, CharacterConversions.toShort(min, dummyConverter));
        assertEquals(0, CharacterConversions.toInt(min, dummyConverter));
        assertEquals(0L, CharacterConversions.toLong(min, dummyConverter));
        assertEquals(0.0f, CharacterConversions.toFloat(min, dummyConverter), 0.0f);
        assertEquals(0.0d, CharacterConversions.toDouble(min, dummyConverter), 0.0d);

        // Maximum char value
        char max = Character.MAX_VALUE; // '\uffff' => 65535
        assertEquals((byte) (max & 0xFF), CharacterConversions.toByte(max, dummyConverter));
        assertEquals((short) max, CharacterConversions.toShort(max, dummyConverter));
        assertEquals((int) max, CharacterConversions.toInt(max, dummyConverter));
        assertEquals((long) max, CharacterConversions.toLong(max, dummyConverter));
        assertEquals((float) max, CharacterConversions.toFloat(max, dummyConverter), 0.0f);
        assertEquals((double) max, CharacterConversions.toDouble(max, dummyConverter), 0.0d);
    }

    /* --------------------------------------------------------------------- *
     *  Atomic conversions
     * --------------------------------------------------------------------- */

    @Test
    public void testToAtomicInteger() {
        char ch = 'b'; // 98
        AtomicInteger ai = CharacterConversions.toAtomicInteger(ch, dummyConverter);
        assertNotNull(ai);
        assertEquals(98, ai.get());
    }

    @Test
    public void testToAtomicLong() {
        char ch = 'c'; // 99
        AtomicLong al = CharacterConversions.toAtomicLong(ch, dummyConverter);
        assertNotNull(al);
        assertEquals(99L, al.get());
    }

    @Test
    public void testToAtomicBoolean_TrueAndFalse() {
        // True case
        AtomicBoolean abTrue = CharacterConversions.toAtomicBoolean('Y', dummyConverter);
        assertTrue(abTrue.get());

        // False case
        AtomicBoolean abFalse = CharacterConversions.toAtomicBoolean('N', dummyConverter);
        assertFalse(abFalse.get());
    }

    /* --------------------------------------------------------------------- *
     *  BigInteger / BigDecimal conversions
     * --------------------------------------------------------------------- */

    @Test
    public void testToBigInteger() {
        char ch = '5'; // Unicode 53
        BigInteger bi = CharacterConversions.toBigInteger(ch, dummyConverter);
        assertEquals(BigInteger.valueOf(53), bi);
    }

    @Test
    public void testToBigDecimal() {
        char ch = '€'; // Unicode 8364
        BigDecimal bd = CharacterConversions.toBigDecimal(ch, dummyConverter);
        assertEquals(BigDecimal.valueOf(8364), bd);
    }

    /* --------------------------------------------------------------------- *
     *  Additional sanity checks – ensure no accidental state sharing.
     * --------------------------------------------------------------------- */

    @Test
    public void testMultipleConversionsDoNotInterfere() {
        char a = 'a'; // 97
        char z = 'z'; // 122

        // First conversion
        int intA = CharacterConversions.toInt(a, dummyConverter);
        AtomicBoolean boolA = CharacterConversions.toAtomicBoolean(a, dummyConverter);
        BigDecimal bdA = CharacterConversions.toBigDecimal(a, dummyConverter);

        // Second conversion with a different character
        int intZ = CharacterConversions.toInt(z, dummyConverter);
        AtomicBoolean boolZ = CharacterConversions.toAtomicBoolean(z, dummyConverter);
        BigDecimal bdZ = CharacterConversions.toBigDecimal(z, dummyConverter);

        // Verify each result corresponds to its input
        assertEquals(97, intA);
        assertFalse(boolA.get()); // 'a' is not a true boolean char
        assertEquals(BigDecimal.valueOf(97), bdA);

        assertEquals(122, intZ);
        assertFalse(boolZ.get()); // 'z' is not a true boolean char
        assertEquals(BigDecimal.valueOf(122), bdZ);
    }
}
