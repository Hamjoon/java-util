package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link AtomicBooleanConversions}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>Normal conversion for {@code true} and {@code false} values.</li>
 *   <li>Verification that new mutable objects are created (no accidental reuse of the source).</li>
 *   <li>Edge‑case handling – {@code null} input and wrong source type.</li>
 *   <li>Behaviour of the {@code char} conversion that depends on {@link ConverterOptions}.</li>
 * </ul>
 *
 * <p>The tests use only the public API of {@link Converter} and the concrete classes that appear
 * in the source file. No mocking frameworks are used, and no subclassing of library types occurs.
 */
public class AtomicBooleanConversionsTest {

    private Converter converter;               // real Converter instance
    private ConverterOptions options;          // options used by the converter

    @Before
    public void setUp() {
        // The library provides a public no‑arg constructor for ConverterOptions.
        // If the library does not expose one, the test will fail to compile,
        // which correctly signals that the test environment does not match the
        // library contract.
        options = new ConverterOptions();
        converter = new Converter(options);
    }

    /* -------------------------------------------------
     * Helper – creates an AtomicBoolean with the given value.
     * ------------------------------------------------- */
    private static AtomicBoolean atomic(boolean value) {
        return new AtomicBoolean(value);
    }

    /* -------------------------------------------------
     * BYTE
     * ------------------------------------------------- */
    @Test
    public void testToByte_trueAndFalse() {
        assertEquals(Byte.valueOf((byte) 1), AtomicBooleanConversions.toByte(atomic(true), converter));
        assertEquals(Byte.valueOf((byte) 0), AtomicBooleanConversions.toByte(atomic(false), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToByte_nullInput() {
        AtomicBooleanConversions.toByte(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToByte_wrongType() {
        AtomicBooleanConversions.toByte("not an atomic boolean", converter);
    }

    /* -------------------------------------------------
     * SHORT
     * ------------------------------------------------- */
    @Test
    public void testToShort_trueAndFalse() {
        assertEquals(Short.valueOf((short) 1), AtomicBooleanConversions.toShort(atomic(true), converter));
        assertEquals(Short.valueOf((short) 0), AtomicBooleanConversions.toShort(atomic(false), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToShort_nullInput() {
        AtomicBooleanConversions.toShort(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToShort_wrongType() {
        AtomicBooleanConversions.toShort(123, converter);
    }

    /* -------------------------------------------------
     * INTEGER
     * ------------------------------------------------- */
    @Test
    public void testToInt_trueAndFalse() {
        assertEquals(Integer.valueOf(1), AtomicBooleanConversions.toInt(atomic(true), converter));
        assertEquals(Integer.valueOf(0), AtomicBooleanConversions.toInt(atomic(false), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToInt_nullInput() {
        AtomicBooleanConversions.toInt(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_wrongType() {
        AtomicBooleanConversions.toInt(new AtomicLong(5L), converter);
    }

    /* -------------------------------------------------
     * LONG
     * ------------------------------------------------- */
    @Test
    public void testToLong_trueAndFalse() {
        assertEquals(Long.valueOf(1L), AtomicBooleanConversions.toLong(atomic(true), converter));
        assertEquals(Long.valueOf(0L), AtomicBooleanConversions.toLong(atomic(false), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_nullInput() {
        AtomicBooleanConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_wrongType() {
        AtomicBooleanConversions.toLong(Boolean.TRUE, converter);
    }

    /* -------------------------------------------------
     * FLOAT
     * ------------------------------------------------- */
    @Test
    public void testToFloat_trueAndFalse() {
        assertEquals(Float.valueOf(1.0f), AtomicBooleanConversions.toFloat(atomic(true), converter));
        assertEquals(Float.valueOf(0.0f), AtomicBooleanConversions.toFloat(atomic(false), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToFloat_nullInput() {
        AtomicBooleanConversions.toFloat(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToFloat_wrongType() {
        AtomicBooleanConversions.toFloat(new Object(), converter);
    }

    /* -------------------------------------------------
     * DOUBLE
     * ------------------------------------------------- */
    @Test
    public void testToDouble_trueAndFalse() {
        assertEquals(Double.valueOf(1.0d), AtomicBooleanConversions.toDouble(atomic(true), converter));
        assertEquals(Double.valueOf(0.0d), AtomicBooleanConversions.toDouble(atomic(false), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_nullInput() {
        AtomicBooleanConversions.toDouble(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_wrongType() {
        AtomicBooleanConversions.toDouble(AtomicBoolean.TRUE, converter);
    }

    /* -------------------------------------------------
     * BOOLEAN (primitive)
     * ------------------------------------------------- */
    @Test
    public void testToBoolean_trueAndFalse() {
        assertTrue(AtomicBooleanConversions.toBoolean(atomic(true), converter));
        assertFalse(AtomicBooleanConversions.toBoolean(atomic(false), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToBoolean_nullInput() {
        AtomicBooleanConversions.toBoolean(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBoolean_wrongType() {
        AtomicBooleanConversions.toBoolean(Integer.valueOf(1), converter);
    }

    /* -------------------------------------------------
     * ATOMIC BOOLEAN
     * ------------------------------------------------- */
    @Test
    public void testToAtomicBoolean_returnsNewInstanceWithSameValue() {
        AtomicBoolean source = atomic(true);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, converter);
        assertNotSame("Result must be a new instance", source, result);
        assertEquals("Value must be copied", source.get(), result.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBoolean_nullInput() {
        AtomicBooleanConversions.toAtomicBoolean(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBoolean_wrongType() {
        AtomicBooleanConversions.toAtomicBoolean("string", converter);
    }

    /* -------------------------------------------------
     * ATOMIC INTEGER
     * ------------------------------------------------- */
    @Test
    public void testToAtomicInteger_trueAndFalse() {
        AtomicInteger trueResult = AtomicBooleanConversions.toAtomicInteger(atomic(true), converter);
        AtomicInteger falseResult = AtomicBooleanConversions.toAtomicInteger(atomic(false), converter);
        assertEquals(1, trueResult.get());
        assertEquals(0, falseResult.get());
        assertNotSame(trueResult, falseResult);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_nullInput() {
        AtomicBooleanConversions.toAtomicInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_wrongType() {
        AtomicBooleanConversions.toAtomicInteger(new AtomicLong(2L), converter);
    }

    /* -------------------------------------------------
     * ATOMIC LONG
     * ------------------------------------------------- */
    @Test
    public void testToAtomicLong_trueAndFalse() {
        AtomicLong trueResult = AtomicBooleanConversions.toAtomicLong(atomic(true), converter);
        AtomicLong falseResult = AtomicBooleanConversions.toAtomicLong(atomic(false), converter);
        assertEquals(1L, trueResult.get());
        assertEquals(0L, falseResult.get());
        assertNotSame(trueResult, falseResult);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_nullInput() {
        AtomicBooleanConversions.toAtomicLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_wrongType() {
        AtomicBooleanConversions.toAtomicLong(Boolean.FALSE, converter);
    }

    /* -------------------------------------------------
     * CHARACTER
     * ------------------------------------------------- */
    @Test
    public void testToCharacter_usesOptions() {
        char expectedTrue = options.trueChar();
        char expectedFalse = options.falseChar();

        assertEquals(expectedTrue, AtomicBooleanConversions.toCharacter(atomic(true), converter).charValue());
        assertEquals(expectedFalse, AtomicBooleanConversions.toCharacter(atomic(false), converter).charValue());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacter_nullInput() {
        AtomicBooleanConversions.toCharacter(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacter_wrongType() {
        AtomicBooleanConversions.toCharacter(AtomicInteger.valueOf(5), converter);
    }

    /* -------------------------------------------------
     * BIGDECIMAL
     * ------------------------------------------------- */
    @Test
    public void testToBigDecimal_trueAndFalse() {
        assertEquals(BigDecimal.ONE, AtomicBooleanConversions.toBigDecimal(atomic(true), converter));
        assertEquals(BigDecimal.ZERO, AtomicBooleanConversions.toBigDecimal(atomic(false), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_nullInput() {
        AtomicBooleanConversions.toBigDecimal(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_wrongType() {
        AtomicBooleanConversions.toBigDecimal(new Object(), converter);
    }

    /* -------------------------------------------------
     * BIGINTEGER
     * ------------------------------------------------- */
    @Test
    public void testToBigInteger_trueAndFalse() {
        assertEquals(BigInteger.ONE, AtomicBooleanConversions.toBigInteger(atomic(true), converter));
        assertEquals(BigInteger.ZERO, AtomicBooleanConversions.toBigInteger(atomic(false), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_nullInput() {
        AtomicBooleanConversions.toBigInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_wrongType() {
        AtomicBooleanConversions.toBigInteger(Boolean.TRUE, converter);
    }
}
