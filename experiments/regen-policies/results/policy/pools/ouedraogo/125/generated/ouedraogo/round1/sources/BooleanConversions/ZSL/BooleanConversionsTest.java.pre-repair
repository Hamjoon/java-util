package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unit tests for {@link BooleanConversions}. All conversion methods are exercised
 * with both {@code true} and {@code false} Boolean values. The tests are placed
 * in the same package so that the package‑private static methods are directly
 * accessible.
 */
public class BooleanConversionsTest {

    private Converter converter;
    private ConverterOptions options;

    @Before
    public void setUp() {
        // Assume ConverterOptions has a public no‑arg constructor.
        options = new ConverterOptions();
        converter = new Converter(options);
    }

    @Test
    public void testToByte_trueAndFalse() {
        Byte trueResult = BooleanConversions.toByte(Boolean.TRUE, converter);
        Byte falseResult = BooleanConversions.toByte(Boolean.FALSE, converter);

        assertEquals("TRUE should map to BYTE_ONE", CommonValues.BYTE_ONE, trueResult);
        assertEquals("FALSE should map to BYTE_ZERO", CommonValues.BYTE_ZERO, falseResult);
    }

    @Test
    public void testToShort_trueAndFalse() {
        Short trueResult = BooleanConversions.toShort(Boolean.TRUE, converter);
        Short falseResult = BooleanConversions.toShort(Boolean.FALSE, converter);

        assertEquals("TRUE should map to SHORT_ONE", CommonValues.SHORT_ONE, trueResult);
        assertEquals("FALSE should map to SHORT_ZERO", CommonValues.SHORT_ZERO, falseResult);
    }

    @Test
    public void testToInt_trueAndFalse() {
        Integer trueResult = BooleanConversions.toInt(Boolean.TRUE, converter);
        Integer falseResult = BooleanConversions.toInt(Boolean.FALSE, converter);

        assertEquals("TRUE should map to INTEGER_ONE", CommonValues.INTEGER_ONE, trueResult);
        assertEquals("FALSE should map to INTEGER_ZERO", CommonValues.INTEGER_ZERO, falseResult);
    }

    @Test
    public void testToAtomicInteger_trueAndFalse() {
        AtomicInteger trueResult = BooleanConversions.toAtomicInteger(Boolean.TRUE, converter);
        AtomicInteger falseResult = BooleanConversions.toAtomicInteger(Boolean.FALSE, converter);

        assertEquals("TRUE should map to AtomicInteger value 1", 1, trueResult.get());
        assertEquals("FALSE should map to AtomicInteger value 0", 0, falseResult.get());
    }

    @Test
    public void testToAtomicLong_trueAndFalse() {
        AtomicLong trueResult = BooleanConversions.toAtomicLong(Boolean.TRUE, converter);
        AtomicLong falseResult = BooleanConversions.toAtomicLong(Boolean.FALSE, converter);

        assertEquals("TRUE should map to AtomicLong value 1", 1L, trueResult.get());
        assertEquals("FALSE should map to AtomicLong value 0", 0L, falseResult.get());
    }

    @Test
    public void testToAtomicBoolean_trueAndFalse() {
        AtomicBoolean trueResult = BooleanConversions.toAtomicBoolean(Boolean.TRUE, converter);
        AtomicBoolean falseResult = BooleanConversions.toAtomicBoolean(Boolean.FALSE, converter);

        assertTrue("TRUE should map to AtomicBoolean true", trueResult.get());
        assertFalse("FALSE should map to AtomicBoolean false", falseResult.get());
    }

    @Test
    public void testToLong_trueAndFalse() {
        Long trueResult = BooleanConversions.toLong(Boolean.TRUE, converter);
        Long falseResult = BooleanConversions.toLong(Boolean.FALSE, converter);

        assertEquals("TRUE should map to LONG_ONE", CommonValues.LONG_ONE, trueResult);
        assertEquals("FALSE should map to LONG_ZERO", CommonValues.LONG_ZERO, falseResult);
    }

    @Test
    public void testToBigDecimal_trueAndFalse() {
        BigDecimal trueResult = BooleanConversions.toBigDecimal(Boolean.TRUE, converter);
        BigDecimal falseResult = BooleanConversions.toBigDecimal(Boolean.FALSE, converter);

        assertEquals("TRUE should map to BigDecimal.ONE", BigDecimal.ONE, trueResult);
        assertEquals("FALSE should map to BigDecimal.ZERO", BigDecimal.ZERO, falseResult);
    }

    @Test
    public void testToBigInteger_trueAndFalse() {
        BigInteger trueResult = BooleanConversions.toBigInteger(Boolean.TRUE, converter);
        BigInteger falseResult = BooleanConversions.toBigInteger(Boolean.FALSE, converter);

        assertEquals("TRUE should map to BigInteger.ONE", BigInteger.ONE, trueResult);
        assertEquals("FALSE should map to BigInteger.ZERO", BigInteger.ZERO, falseResult);
    }

    @Test
    public void testToFloat_trueAndFalse() {
        Float trueResult = BooleanConversions.toFloat(Boolean.TRUE, converter);
        Float falseResult = BooleanConversions.toFloat(Boolean.FALSE, converter);

        assertEquals("TRUE should map to FLOAT_ONE", CommonValues.FLOAT_ONE, trueResult);
        assertEquals("FALSE should map to FLOAT_ZERO", CommonValues.FLOAT_ZERO, falseResult);
    }

    @Test
    public void testToDouble_trueAndFalse() {
        Double trueResult = BooleanConversions.toDouble(Boolean.TRUE, converter);
        Double falseResult = BooleanConversions.toDouble(Boolean.FALSE, converter);

        assertEquals("TRUE should map to DOUBLE_ONE", CommonValues.DOUBLE_ONE, trueResult);
        assertEquals("FALSE should map to DOUBLE_ZERO", CommonValues.DOUBLE_ZERO, falseResult);
    }

    @Test
    public void testToCharacter_trueAndFalse() {
        // The character mapping depends on the ConverterOptions configuration.
        char expectedTrue = options.trueChar();
        char expectedFalse = options.falseChar();

        char trueResult = BooleanConversions.toCharacter(Boolean.TRUE, converter);
        char falseResult = BooleanConversions.toCharacter(Boolean.FALSE, converter);

        assertEquals("TRUE should map to options.trueChar()", expectedTrue, trueResult);
        assertEquals("FALSE should map to options.falseChar()", expectedFalse, falseResult);
    }

    @Test(expected = ClassCastException.class)
    public void testInvalidInput_throwsClassCastException() {
        // The conversion methods expect a Boolean; passing a non‑Boolean should cause
        // a ClassCastException when they attempt to cast.
        BooleanConversions.toInt("not a Boolean", converter);
    }

    @Test
    public void testNullInput_throwsNullPointerException() {
        try {
            BooleanConversions.toInt(null, converter);
            fail("Expected NullPointerException when input is null");
        } catch (NullPointerException ignored) {
            // Expected path
        }
    }
}
