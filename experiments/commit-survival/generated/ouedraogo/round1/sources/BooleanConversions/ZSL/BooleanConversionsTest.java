package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit tests for {@link BooleanConversions}. All conversion methods are exercised
 * for both {@code true} and {@code false} Boolean values as well as for the
 * {@code null} edge‑case (which should result in a {@link NullPointerException}
 * because the implementation performs a direct cast to {@link Boolean}).
 *
 * The tests use a real {@link Converter} instance with its default
 * {@link ConverterOptions}.  The {@code trueChar()} and {@code falseChar()}
 * values are obtained from the options object itself, so the test does not rely
 * on any particular default character.
 */
public class BooleanConversionsTest {

    /** A shared Converter instance used by all tests. */
    private static Converter converter;
    /** Cached options for easy reference in the character test. */
    private static ConverterOptions options;

    @BeforeClass
    public static void setUpClass() {
        // The library provides a public no‑arg constructor for ConverterOptions.
        // If the real library requires configuration, the default constructor
        // will still give us deterministic trueChar()/falseChar() values.
        options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* --------------------------------------------------------------------- */
    /*  Boolean → primitive wrapper numeric types                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByteTrue() {
        Byte result = BooleanConversions.toByte(Boolean.TRUE, converter);
        assertEquals(Byte.valueOf((byte) 1), result);
    }

    @Test
    public void testToByteFalse() {
        Byte result = BooleanConversions.toByte(Boolean.FALSE, converter);
        assertEquals(Byte.valueOf((byte) 0), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteNull() {
        BooleanConversions.toByte(null, converter);
    }

    @Test
    public void testToShortTrue() {
        Short result = BooleanConversions.toShort(Boolean.TRUE, converter);
        assertEquals(Short.valueOf((short) 1), result);
    }

    @Test
    public void testToShortFalse() {
        Short result = BooleanConversions.toShort(Boolean.FALSE, converter);
        assertEquals(Short.valueOf((short) 0), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToShortNull() {
        BooleanConversions.toShort(null, converter);
    }

    @Test
    public void testToIntTrue() {
        Integer result = BooleanConversions.toInt(Boolean.TRUE, converter);
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testToIntFalse() {
        Integer result = BooleanConversions.toInt(Boolean.FALSE, converter);
        assertEquals(Integer.valueOf(0), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToIntNull() {
        BooleanConversions.toInt(null, converter);
    }

    @Test
    public void testToLongTrue() {
        Long result = BooleanConversions.toLong(Boolean.TRUE, converter);
        assertEquals(Long.valueOf(1L), result);
    }

    @Test
    public void testToLongFalse() {
        Long result = BooleanConversions.toLong(Boolean.FALSE, converter);
        assertEquals(Long.valueOf(0L), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToLongNull() {
        BooleanConversions.toLong(null, converter);
    }

    @Test
    public void testToFloatTrue() {
        Float result = BooleanConversions.toFloat(Boolean.TRUE, converter);
        assertEquals(Float.valueOf(1.0f), result);
    }

    @Test
    public void testToFloatFalse() {
        Float result = BooleanConversions.toFloat(Boolean.FALSE, converter);
        assertEquals(Float.valueOf(0.0f), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToFloatNull() {
        BooleanConversions.toFloat(null, converter);
    }

    @Test
    public void testToDoubleTrue() {
        Double result = BooleanConversions.toDouble(Boolean.TRUE, converter);
        assertEquals(Double.valueOf(1.0d), result);
    }

    @Test
    public void testToDoubleFalse() {
        Double result = BooleanConversions.toDouble(Boolean.FALSE, converter);
        assertEquals(Double.valueOf(0.0d), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleNull() {
        BooleanConversions.toDouble(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  Boolean → atomic numeric/boolean types                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToAtomicIntegerTrue() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.TRUE, converter);
        assertEquals(1, ai.get());
    }

    @Test
    public void testToAtomicIntegerFalse() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.FALSE, converter);
        assertEquals(0, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNull() {
        BooleanConversions.toAtomicInteger(null, converter);
    }

    @Test
    public void testToAtomicLongTrue() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.TRUE, converter);
        assertEquals(1L, al.get());
    }

    @Test
    public void testToAtomicLongFalse() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.FALSE, converter);
        assertEquals(0L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNull() {
        BooleanConversions.toAtomicLong(null, converter);
    }

    @Test
    public void testToAtomicBooleanTrue() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.TRUE, converter);
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.FALSE, converter);
        assertFalse(ab.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBooleanNull() {
        BooleanConversions.toAtomicBoolean(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  Boolean → big numeric types                                            */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToBigDecimalTrue() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.TRUE, converter);
        assertEquals(BigDecimal.ONE, bd);
    }

    @Test
    public void testToBigDecimalFalse() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.FALSE, converter);
        assertEquals(BigDecimal.ZERO, bd);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalNull() {
        BooleanConversions.toBigDecimal(null, converter);
    }

    @Test
    public void testToBigIntegerTrue() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.TRUE, converter);
        assertEquals(BigInteger.ONE, bi);
    }

    @Test
    public void testToBigIntegerFalse() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.FALSE, converter);
        assertEquals(BigInteger.ZERO, bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNull() {
        BooleanConversions.toBigInteger(null, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  Boolean → char (uses ConverterOptions)                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToCharacterTrue() {
        char result = BooleanConversions.toCharacter(Boolean.TRUE, converter);
        assertEquals(options.trueChar(), result);
    }

    @Test
    public void testToCharacterFalse() {
        char result = BooleanConversions.toCharacter(Boolean.FALSE, converter);
        assertEquals(options.falseChar(), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacterNull() {
        BooleanConversions.toCharacter(null, converter);
    }
}
