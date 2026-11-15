package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JUnit‑4 test suite for {@link BooleanConversions}.
 *
 * The tests cover:
 *   • normal conversion for Boolean.TRUE and Boolean.FALSE,
 *   • NullPointerException when the source value is null,
 *   • ClassCastException when the source value is not a Boolean,
 *   • the special handling of {@code toCharacter} which depends on {@link ConverterOptions}.
 *
 * The class under test has package‑private static methods; the test class lives in the same
 * package so it can call them directly.
 */
public class BooleanConversionsTest {

    /* --------------------------------------------------------------------- *
     *  Helper to create a Converter with deterministic true/false chars   *
     * --------------------------------------------------------------------- */
    private static Converter createConverter(char trueChar, char falseChar) {
        // The real library supplies a constructor ConverterOptions(char, char).
        // If the actual API differs, adjust this helper accordingly.
        ConverterOptions options = new ConverterOptions(trueChar, falseChar);
        return new Converter(options);
    }

    /* --------------------------- toByte --------------------------------- */
    @Test
    public void testToByteTrue() {
        Byte result = BooleanConversions.toByte(Boolean.TRUE, createConverter('T','F'));
        assertEquals(Byte.valueOf((byte)1), result);
    }

    @Test
    public void testToByteFalse() {
        Byte result = BooleanConversions.toByte(Boolean.FALSE, createConverter('T','F'));
        assertEquals(Byte.valueOf((byte)0), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteNull() {
        BooleanConversions.toByte(null, createConverter('T','F'));
    }

    @Test(expected = ClassCastException.class)
    public void testToByteWrongType() {
        BooleanConversions.toByte("not a boolean", createConverter('T','F'));
    }

    /* --------------------------- toShort -------------------------------- */
    @Test
    public void testToShortTrue() {
        Short result = BooleanConversions.toShort(Boolean.TRUE, createConverter('T','F'));
        assertEquals(Short.valueOf((short)1), result);
    }

    @Test
    public void testToShortFalse() {
        Short result = BooleanConversions.toShort(Boolean.FALSE, createConverter('T','F'));
        assertEquals(Short.valueOf((short)0), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToShortNull() {
        BooleanConversions.toShort(null, createConverter('T','F'));
    }

    @Test(expected = ClassCastException.class)
    public void testToShortWrongType() {
        BooleanConversions.toShort(123, createConverter('T','F'));
    }

    /* --------------------------- toInt ---------------------------------- */
    @Test
    public void testToIntTrue() {
        Integer result = BooleanConversions.toInt(Boolean.TRUE, createConverter('T','F'));
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testToIntFalse() {
        Integer result = BooleanConversions.toInt(Boolean.FALSE, createConverter('T','F'));
        assertEquals(Integer.valueOf(0), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToIntNull() {
        BooleanConversions.toInt(null, createConverter('T','F'));
    }

    @Test(expected = ClassCastException.class)
    public void testToIntWrongType() {
        BooleanConversions.toInt(new Object(), createConverter('T','F'));
    }

    /* --------------------------- toAtomicInteger ------------------------ */
    @Test
    public void testToAtomicIntegerTrue() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.TRUE, createConverter('T','F'));
        assertEquals(1, ai.get());
    }

    @Test
    public void testToAtomicIntegerFalse() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.FALSE, createConverter('T','F'));
        assertEquals(0, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNull() {
        BooleanConversions.toAtomicInteger(null, createConverter('T','F'));
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicIntegerWrongType() {
        BooleanConversions.toAtomicInteger(42L, createConverter('T','F'));
    }

    /* --------------------------- toAtomicLong --------------------------- */
    @Test
    public void testToAtomicLongTrue() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.TRUE, createConverter('T','F'));
        assertEquals(1L, al.get());
    }

    @Test
    public void testToAtomicLongFalse() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.FALSE, createConverter('T','F'));
        assertEquals(0L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNull() {
        BooleanConversions.toAtomicLong(null, createConverter('T','F'));
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLongWrongType() {
        BooleanConversions.toAtomicLong("true", createConverter('T','F'));
    }

    /* --------------------------- toAtomicBoolean ------------------------ */
    @Test
    public void testToAtomicBooleanTrue() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.TRUE, createConverter('T','F'));
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.FALSE, createConverter('T','F'));
        assertFalse(ab.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBooleanNull() {
        BooleanConversions.toAtomicBoolean(null, createConverter('T','F'));
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBooleanWrongType() {
        BooleanConversions.toAtomicBoolean(0, createConverter('T','F'));
    }

    /* --------------------------- toLong --------------------------------- */
    @Test
    public void testToLongTrue() {
        Long result = BooleanConversions.toLong(Boolean.TRUE, createConverter('T','F'));
        assertEquals(Long.valueOf(1L), result);
    }

    @Test
    public void testToLongFalse() {
        Long result = BooleanConversions.toLong(Boolean.FALSE, createConverter('T','F'));
        assertEquals(Long.valueOf(0L), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToLongNull() {
        BooleanConversions.toLong(null, createConverter('T','F'));
    }

    @Test(expected = ClassCastException.class)
    public void testToLongWrongType() {
        BooleanConversions.toLong(Boolean.TRUE.toString(), createConverter('T','F'));
    }

    /* --------------------------- toBigDecimal --------------------------- */
    @Test
    public void testToBigDecimalTrue() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.TRUE, createConverter('T','F'));
        assertEquals(BigDecimal.ONE, bd);
    }

    @Test
    public void testToBigDecimalFalse() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.FALSE, createConverter('T','F'));
        assertEquals(BigDecimal.ZERO, bd);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalNull() {
        BooleanConversions.toBigDecimal(null, createConverter('T','F'));
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimalWrongType() {
        BooleanConversions.toBigDecimal(1.0, createConverter('T','F'));
    }

    /* --------------------------- toBigInteger --------------------------- */
    @Test
    public void testToBigIntegerTrue() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.TRUE, createConverter('T','F'));
        assertEquals(BigInteger.ONE, bi);
    }

    @Test
    public void testToBigIntegerFalse() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.FALSE, createConverter('T','F'));
        assertEquals(BigInteger.ZERO, bi);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNull() {
        BooleanConversions.toBigInteger(null, createConverter('T','F'));
    }

    @Test(expected = ClassCastException.class)
    public void testToBigIntegerWrongType() {
        BooleanConversions.toBigInteger(new Object(), createConverter('T','F'));
    }

    /* --------------------------- toFloat -------------------------------- */
    @Test
    public void testToFloatTrue() {
        Float f = BooleanConversions.toFloat(Boolean.TRUE, createConverter('T','F'));
        assertEquals(Float.valueOf(1.0f), f);
    }

    @Test
    public void testToFloatFalse() {
        Float f = BooleanConversions.toFloat(Boolean.FALSE, createConverter('T','F'));
        assertEquals(Float.valueOf(0.0f), f);
    }

    @Test(expected = NullPointerException.class)
    public void testToFloatNull() {
        BooleanConversions.toFloat(null, createConverter('T','F'));
    }

    @Test(expected = ClassCastException.class)
    public void testToFloatWrongType() {
        BooleanConversions.toFloat(Boolean.TRUE.toString(), createConverter('T','F'));
    }

    /* --------------------------- toDouble ------------------------------- */
    @Test
    public void testToDoubleTrue() {
        Double d = BooleanConversions.toDouble(Boolean.TRUE, createConverter('T','F'));
        assertEquals(Double.valueOf(1.0d), d);
    }

    @Test
    public void testToDoubleFalse() {
        Double d = BooleanConversions.toDouble(Boolean.FALSE, createConverter('T','F'));
        assertEquals(Double.valueOf(0.0d), d);
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleNull() {
        BooleanConversions.toDouble(null, createConverter('T','F'));
    }

    @Test(expected = ClassCastException.class)
    public void testToDoubleWrongType() {
        BooleanConversions.toDouble(0, createConverter('T','F'));
    }

    /* --------------------------- toCharacter ---------------------------- */
    @Test
    public void testToCharacterTrue() {
        char trueChar = 'Y';
        Converter conv = createConverter(trueChar, 'N');
        char result = BooleanConversions.toCharacter(Boolean.TRUE, conv);
        assertEquals(trueChar, result);
    }

    @Test
    public void testToCharacterFalse() {
        char falseChar = 'N';
        Converter conv = createConverter('Y', falseChar);
        char result = BooleanConversions.toCharacter(Boolean.FALSE, conv);
        assertEquals(falseChar, result);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacterNull() {
        BooleanConversions.toCharacter(null, createConverter('Y','N'));
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacterWrongType() {
        BooleanConversions.toCharacter(1, createConverter('Y','N'));
    }
}
