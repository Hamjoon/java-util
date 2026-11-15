package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit tests for {@link BooleanConversions}.
 * <p>
 * All conversion methods are exercised with both {@code true} and {@code false}
 * Boolean values, as well as with invalid inputs to verify that the expected
 * unchecked exceptions are thrown.
 * </p>
 */
public class BooleanConversionsTest {

    /** Helper creates a {@link Converter} with default {@link ConverterOptions}. */
    private static Converter createDefaultConverter() {
        // The library supplies a public constructor that accepts ConverterOptions.
        // An instance with default true/false characters is sufficient for the tests.
        return new Converter(new ConverterOptions());
    }

    @Test
    public void testToByteTrue() {
        Byte result = BooleanConversions.toByte(Boolean.TRUE, createDefaultConverter());
        assertEquals(Byte.valueOf((byte) 1), result);
    }

    @Test
    public void testToByteFalse() {
        Byte result = BooleanConversions.toByte(Boolean.FALSE, createDefaultConverter());
        assertEquals(Byte.valueOf((byte) 0), result);
    }

    @Test
    public void testToShortTrue() {
        Short result = BooleanConversions.toShort(Boolean.TRUE, createDefaultConverter());
        assertEquals(Short.valueOf((short) 1), result);
    }

    @Test
    public void testToShortFalse() {
        Short result = BooleanConversions.toShort(Boolean.FALSE, createDefaultConverter());
        assertEquals(Short.valueOf((short) 0), result);
    }

    @Test
    public void testToIntTrue() {
        Integer result = BooleanConversions.toInt(Boolean.TRUE, createDefaultConverter());
        assertEquals(Integer.valueOf(1), result);
    }

    @Test
    public void testToIntFalse() {
        Integer result = BooleanConversions.toInt(Boolean.FALSE, createDefaultConverter());
        assertEquals(Integer.valueOf(0), result);
    }

    @Test
    public void testToAtomicIntegerTrue() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.TRUE, createDefaultConverter());
        assertEquals(1, ai.get());
    }

    @Test
    public void testToAtomicIntegerFalse() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.FALSE, createDefaultConverter());
        assertEquals(0, ai.get());
    }

    @Test
    public void testToAtomicLongTrue() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.TRUE, createDefaultConverter());
        assertEquals(1L, al.get());
    }

    @Test
    public void testToAtomicLongFalse() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.FALSE, createDefaultConverter());
        assertEquals(0L, al.get());
    }

    @Test
    public void testToAtomicBooleanTrue() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.TRUE, createDefaultConverter());
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.FALSE, createDefaultConverter());
        assertFalse(ab.get());
    }

    @Test
    public void testToLongTrue() {
        Long result = BooleanConversions.toLong(Boolean.TRUE, createDefaultConverter());
        assertEquals(Long.valueOf(1L), result);
    }

    @Test
    public void testToLongFalse() {
        Long result = BooleanConversions.toLong(Boolean.FALSE, createDefaultConverter());
        assertEquals(Long.valueOf(0L), result);
    }

    @Test
    public void testToBigDecimalTrue() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.TRUE, createDefaultConverter());
        assertEquals(BigDecimal.ONE, bd);
    }

    @Test
    public void testToBigDecimalFalse() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.FALSE, createDefaultConverter());
        assertEquals(BigDecimal.ZERO, bd);
    }

    @Test
    public void testToBigIntegerTrue() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.TRUE, createDefaultConverter());
        assertEquals(BigInteger.ONE, bi);
    }

    @Test
    public void testToBigIntegerFalse() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.FALSE, createDefaultConverter());
        assertEquals(BigInteger.ZERO, bi);
    }

    @Test
    public void testToFloatTrue() {
        Float f = BooleanConversions.toFloat(Boolean.TRUE, createDefaultConverter());
        assertEquals(Float.valueOf(1.0f), f);
    }

    @Test
    public void testToFloatFalse() {
        Float f = BooleanConversions.toFloat(Boolean.FALSE, createDefaultConverter());
        assertEquals(Float.valueOf(0.0f), f);
    }

    @Test
    public void testToDoubleTrue() {
        Double d = BooleanConversions.toDouble(Boolean.TRUE, createDefaultConverter());
        assertEquals(Double.valueOf(1.0d), d);
    }

    @Test
    public void testToDoubleFalse() {
        Double d = BooleanConversions.toDouble(Boolean.FALSE, createDefaultConverter());
        assertEquals(Double.valueOf(0.0d), d);
    }

    @Test
    public void testToCharacterTrue() {
        Converter converter = createDefaultConverter();
        ConverterOptions options = converter.getOptions();
        char expected = options.trueChar();
        char result = BooleanConversions.toCharacter(Boolean.TRUE, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToCharacterFalse() {
        Converter converter = createDefaultConverter();
        ConverterOptions options = converter.getOptions();
        char expected = options.falseChar();
        char result = BooleanConversions.toCharacter(Boolean.FALSE, converter);
        assertEquals(expected, result);
    }

    // ----------------------------------------------------------------------
    // Negative tests – ensure the class fails fast when inappropriate input is supplied.
    // ----------------------------------------------------------------------

    @Test(expected = NullPointerException.class)
    public void testToByteNull() {
        BooleanConversions.toByte(null, createDefaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToByteInvalidType() {
        BooleanConversions.toByte("not a boolean", createDefaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToShortNull() {
        BooleanConversions.toShort(null, createDefaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToShortInvalidType() {
        BooleanConversions.toShort(123, createDefaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToIntNull() {
        BooleanConversions.toInt(null, createDefaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToIntInvalidType() {
        BooleanConversions.toInt(Boolean.TRUE, null); // null converter is allowed; the NPE is from casting
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicIntegerNull() {
        BooleanConversions.toAtomicInteger(null, createDefaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLongNull() {
        BooleanConversions.toAtomicLong(null, createDefaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBooleanNull() {
        BooleanConversions.toAtomicBoolean(null, createDefaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToLongNull() {
        BooleanConversions.toLong(null, createDefaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimalNull() {
        BooleanConversions.toBigDecimal(null, createDefaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToBigIntegerNull() {
        BooleanConversions.toBigInteger(null, createDefaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToFloatNull() {
        BooleanConversions.toFloat(null, createDefaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToDoubleNull() {
        BooleanConversions.toDouble(null, createDefaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacterNull() {
        BooleanConversions.toCharacter(null, createDefaultConverter());
    }
}
