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
 * Comprehensive unit tests for {@link AtomicBooleanConversions}.
 * Each conversion method is exercised with both {@code true}
 * and {@code false} {@link AtomicBoolean} values and the
 * results are compared against the expected constants from
 * {@link CommonValues} or the {@link ConverterOptions} configuration.
 */
public class AtomicBooleanConversionsTest {

    private Converter converterTrue;
    private Converter converterFalse;

    @Before
    public void setUp() {
        // The ConverterOptions class provides trueChar() / falseChar().
        // We use the default constructor; the actual characters are
        // implementation‑specific but we can retrieve them for verification.
        ConverterOptions options = new ConverterOptions();
        converterTrue = new Converter(options);
        converterFalse = converterTrue; // the same options are used for both cases
    }

    // -----------------------------------------------------------------
    // Primitive wrapper conversions
    // -----------------------------------------------------------------

    @Test
    public void testToByteTrue() {
        AtomicBoolean source = new AtomicBoolean(true);
        Byte result = AtomicBooleanConversions.toByte(source, converterTrue);
        assertEquals(CommonValues.BYTE_ONE, result);
    }

    @Test
    public void testToByteFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        Byte result = AtomicBooleanConversions.toByte(source, converterFalse);
        assertEquals(CommonValues.BYTE_ZERO, result);
    }

    @Test
    public void testToShortTrue() {
        AtomicBoolean source = new AtomicBoolean(true);
        Short result = AtomicBooleanConversions.toShort(source, converterTrue);
        assertEquals(CommonValues.SHORT_ONE, result);
    }

    @Test
    public void testToShortFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        Short result = AtomicBooleanConversions.toShort(source, converterFalse);
        assertEquals(CommonValues.SHORT_ZERO, result);
    }

    @Test
    public void testToIntTrue() {
        AtomicBoolean source = new AtomicBoolean(true);
        Integer result = AtomicBooleanConversions.toInt(source, converterTrue);
        assertEquals(CommonValues.INTEGER_ONE, result);
    }

    @Test
    public void testToIntFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        Integer result = AtomicBooleanConversions.toInt(source, converterFalse);
        assertEquals(CommonValues.INTEGER_ZERO, result);
    }

    @Test
    public void testToLongTrue() {
        AtomicBoolean source = new AtomicBoolean(true);
        Long result = AtomicBooleanConversions.toLong(source, converterTrue);
        assertEquals(CommonValues.LONG_ONE, result);
    }

    @Test
    public void testToLongFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        Long result = AtomicBooleanConversions.toLong(source, converterFalse);
        assertEquals(CommonValues.LONG_ZERO, result);
    }

    @Test
    public void testToFloatTrue() {
        AtomicBoolean source = new AtomicBoolean(true);
        Float result = AtomicBooleanConversions.toFloat(source, converterTrue);
        assertEquals(CommonValues.FLOAT_ONE, result);
    }

    @Test
    public void testToFloatFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        Float result = AtomicBooleanConversions.toFloat(source, converterFalse);
        assertEquals(CommonValues.FLOAT_ZERO, result);
    }

    @Test
    public void testToDoubleTrue() {
        AtomicBoolean source = new AtomicBoolean(true);
        Double result = AtomicBooleanConversions.toDouble(source, converterTrue);
        assertEquals(CommonValues.DOUBLE_ONE, result);
    }

    @Test
    public void testToDoubleFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        Double result = AtomicBooleanConversions.toDouble(source, converterFalse);
        assertEquals(CommonValues.DOUBLE_ZERO, result);
    }

    @Test
    public void testToBooleanTrue() {
        AtomicBoolean source = new AtomicBoolean(true);
        boolean result = AtomicBooleanConversions.toBoolean(source, converterTrue);
        assertTrue(result);
    }

    @Test
    public void testToBooleanFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        boolean result = AtomicBooleanConversions.toBoolean(source, converterFalse);
        assertFalse(result);
    }

    // -----------------------------------------------------------------
    // Atomic type conversions
    // -----------------------------------------------------------------

    @Test
    public void testToAtomicBooleanTrue() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, converterTrue);
        assertNotSame("Result must be a new instance", source, result);
        assertTrue(result.get());
    }

    @Test
    public void testToAtomicBooleanFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, converterFalse);
        assertNotSame("Result must be a new instance", source, result);
        assertFalse(result.get());
    }

    @Test
    public void testToAtomicIntegerTrue() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, converterTrue);
        assertEquals(1, result.get());
    }

    @Test
    public void testToAtomicIntegerFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, converterFalse);
        assertEquals(0, result.get());
    }

    @Test
    public void testToAtomicLongTrue() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, converterTrue);
        assertEquals(1L, result.get());
    }

    @Test
    public void testToAtomicLongFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, converterFalse);
        assertEquals(0L, result.get());
    }

    // -----------------------------------------------------------------
    // Character, BigDecimal, BigInteger conversions
    // -----------------------------------------------------------------

    @Test
    public void testToCharacterTrue() {
        AtomicBoolean source = new AtomicBoolean(true);
        ConverterOptions options = converterTrue.getOptions();
        char expected = options.trueChar();
        char result = AtomicBooleanConversions.toCharacter(source, converterTrue);
        assertEquals(expected, result);
    }

    @Test
    public void testToCharacterFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        ConverterOptions options = converterFalse.getOptions();
        char expected = options.falseChar();
        char result = AtomicBooleanConversions.toCharacter(source, converterFalse);
        assertEquals(expected, result);
    }

    @Test
    public void testToBigDecimalTrue() {
        AtomicBoolean source = new AtomicBoolean(true);
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, converterTrue);
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    public void testToBigDecimalFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, converterFalse);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testToBigIntegerTrue() {
        AtomicBoolean source = new AtomicBoolean(true);
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, converterTrue);
        assertEquals(BigInteger.ONE, result);
    }

    @Test
    public void testToBigIntegerFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, converterFalse);
        assertEquals(BigInteger.ZERO, result);
    }
}
