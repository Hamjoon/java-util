package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Unit tests for {@link AtomicBooleanConversions}.
 * <p>
 * The tests cover every conversion method for both {@code true}
 * and {@code false} {@link AtomicBoolean} values, as well as a few
 * negative‑case checks (null source and wrong source type).
 * </p>
 */
public class AtomicBooleanConversionsTest {

    // Helper objects used by many tests
    private final AtomicBoolean TRUE_AB = new AtomicBoolean(true);
    private final AtomicBoolean FALSE_AB = new AtomicBoolean(false);
    private final Converter CONVERTER = new Converter(new ConverterOptions());

    /* -------------------------------------------------------------------- */
    /*  Byte conversion                                                     */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToByteTrue() {
        Byte result = AtomicBooleanConversions.toByte(TRUE_AB, CONVERTER);
        assertEquals(Byte.valueOf((byte) 1), result);
    }

    @Test
    public void testToByteFalse() {
        Byte result = AtomicBooleanConversions.toByte(FALSE_AB, CONVERTER);
        assertEquals(Byte.valueOf((byte) 0), result);
    }

    @Test(expected = NullPointerException.class)
    public void testToByteNullSource() {
        AtomicBooleanConversions.toByte(null, CONVERTER);
    }

    @Test(expected = ClassCastException.class)
    public void testToByteInvalidSource() {
        AtomicBooleanConversions.toByte(new Object(), CONVERTER);
    }

    /* -------------------------------------------------------------------- */
    /*  Short conversion                                                    */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToShortTrue() {
        Short result = AtomicBooleanConversions.toShort(TRUE_AB, CONVERTER);
        assertEquals(Short.valueOf((short) 1), result);
    }

    @Test
    public void testToShortFalse() {
        Short result = AtomicBooleanConversions.toShort(FALSE_AB, CONVERTER);
        assertEquals(Short.valueOf((short) 0), result);
    }

    /* -------------------------------------------------------------------- */
    /*  Integer conversion                                                  */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToIntTrue() {
        Integer result = AtomicBooleanConversions.toInt(TRUE_AB, CONVERTER);
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testToIntFalse() {
        Integer result = AtomicBooleanConversions.toInt(FALSE_AB, CONVERTER);
        assertEquals(Integer.valueOf(0), result);
    }

    /* -------------------------------------------------------------------- */
    /*  Long conversion                                                     */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToLongTrue() {
        Long result = AtomicBooleanConversions.toLong(TRUE_AB, CONVERTER);
        assertEquals(Long.valueOf(1L), result);
    }

    @Test
    public void testToLongFalse() {
        Long result = AtomicBooleanConversions.toLong(FALSE_AB, CONVERTER);
        assertEquals(Long.valueOf(0L), result);
    }

    /* -------------------------------------------------------------------- */
    /*  Float conversion                                                    */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToFloatTrue() {
        Float result = AtomicBooleanConversions.toFloat(TRUE_AB, CONVERTER);
        assertEquals(Float.valueOf(1.0f), result);
    }

    @Test
    public void testToFloatFalse() {
        Float result = AtomicBooleanConversions.toFloat(FALSE_AB, CONVERTER);
        assertEquals(Float.valueOf(0.0f), result);
    }

    /* -------------------------------------------------------------------- */
    /*  Double conversion                                                   */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToDoubleTrue() {
        Double result = AtomicBooleanConversions.toDouble(TRUE_AB, CONVERTER);
        assertEquals(Double.valueOf(1.0d), result);
    }

    @Test
    public void testToDoubleFalse() {
        Double result = AtomicBooleanConversions.toDouble(FALSE_AB, CONVERTER);
        assertEquals(Double.valueOf(0.0d), result);
    }

    /* -------------------------------------------------------------------- */
    /*  Boolean conversion                                                  */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToBooleanTrue() {
        assertTrue(AtomicBooleanConversions.toBoolean(TRUE_AB, CONVERTER));
    }

    @Test
    public void testToBooleanFalse() {
        assertFalse(AtomicBooleanConversions.toBoolean(FALSE_AB, CONVERTER));
    }

    /* -------------------------------------------------------------------- */
    /*  AtomicBoolean conversion                                            */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToAtomicBooleanTrue() {
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(TRUE_AB, CONVERTER);
        assertNotSame(TRUE_AB, result);               // must be a new instance
        assertTrue(result.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(FALSE_AB, CONVERTER);
        assertNotSame(FALSE_AB, result);
        assertFalse(result.get());
    }

    /* -------------------------------------------------------------------- */
    /*  AtomicInteger conversion                                            */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToAtomicIntegerTrue() {
        AtomicInteger ai = AtomicBooleanConversions.toAtomicInteger(TRUE_AB, CONVERTER);
        assertEquals(1, ai.get());
    }

    @Test
    public void testToAtomicIntegerFalse() {
        AtomicInteger ai = AtomicBooleanConversions.toAtomicInteger(FALSE_AB, CONVERTER);
        assertEquals(0, ai.get());
    }

    /* -------------------------------------------------------------------- */
    /*  AtomicLong conversion                                               */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToAtomicLongTrue() {
        AtomicLong al = AtomicBooleanConversions.toAtomicLong(TRUE_AB, CONVERTER);
        assertEquals(1L, al.get());
    }

    @Test
    public void testToAtomicLongFalse() {
        AtomicLong al = AtomicBooleanConversions.toAtomicLong(FALSE_AB, CONVERTER);
        assertEquals(0L, al.get());
    }

    /* -------------------------------------------------------------------- */
    /*  Character conversion                                                */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToCharacterTrue() {
        ConverterOptions options = CONVERTER.getOptions();
        Character result = AtomicBooleanConversions.toCharacter(TRUE_AB, CONVERTER);
        assertEquals(options.trueChar(), result.charValue());
    }

    @Test
    public void testToCharacterFalse() {
        ConverterOptions options = CONVERTER.getOptions();
        Character result = AtomicBooleanConversions.toCharacter(FALSE_AB, CONVERTER);
        assertEquals(options.falseChar(), result.charValue());
    }

    /* -------------------------------------------------------------------- */
    /*  BigDecimal conversion                                               */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToBigDecimalTrue() {
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(TRUE_AB, CONVERTER);
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    public void testToBigDecimalFalse() {
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(FALSE_AB, CONVERTER);
        assertEquals(BigDecimal.ZERO, result);
    }

    /* -------------------------------------------------------------------- */
    /*  BigInteger conversion                                               */
    /* -------------------------------------------------------------------- */
    @Test
    public void testToBigIntegerTrue() {
        BigInteger result = AtomicBooleanConversions.toBigInteger(TRUE_AB, CONVERTER);
        assertEquals(BigInteger.ONE, result);
    }

    @Test
    public void testToBigIntegerFalse() {
        BigInteger result = AtomicBooleanConversions.toBigInteger(FALSE_AB, CONVERTER);
        assertEquals(BigInteger.ZERO, result);
    }
}
