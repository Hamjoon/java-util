package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit‑4 test suite for {@code AtomicBooleanConversions}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Normal conversion for both {@code true} and {@code false} values.</li>
 *   <li>Edge‑case handling – {@code null} source objects and objects of the wrong type.</li>
 *   <li>Verification that mutable targets are newly created (no aliasing).</li>
 *   <li>Special handling for {@code toCharacter} which depends on {@link ConverterOptions}.</li>
 * </ul>
 *
 * <p>The test class lives in the same package as {@code AtomicBooleanConversions}
 * so it can access the package‑private static conversion methods.
 */
public class AtomicBooleanConversionsTest {

    /** Helper that creates a {@link Converter} with default options. */
    private static Converter defaultConverter() {
        // Assuming ConverterOptions has a public no‑arg constructor.
        ConverterOptions options = new ConverterOptions();
        return new Converter(options);
    }

    /** Helper that creates a {@link Converter} with custom true/false characters. */
    private static Converter customCharConverter(char trueChar, char falseChar) {
        // Assuming ConverterOptions provides setters for the characters.
        ConverterOptions options = new ConverterOptions();
        options.setTrueChar(trueChar);
        options.setFalseChar(falseChar);
        return new Converter(options);
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- BASIC FUNCTIONALITY ----------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByte_true() {
        AtomicBoolean ab = new AtomicBoolean(true);
        Byte result = AtomicBooleanConversions.toByte(ab, defaultConverter());
        assertEquals(CommonValues.BYTE_ONE, result);
    }

    @Test
    public void testToByte_false() {
        AtomicBoolean ab = new AtomicBoolean(false);
        Byte result = AtomicBooleanConversions.toByte(ab, defaultConverter());
        assertEquals(CommonValues.BYTE_ZERO, result);
    }

    @Test
    public void testToShort_true() {
        AtomicBoolean ab = new AtomicBoolean(true);
        Short result = AtomicBooleanConversions.toShort(ab, defaultConverter());
        assertEquals(CommonValues.SHORT_ONE, result);
    }

    @Test
    public void testToShort_false() {
        AtomicBoolean ab = new AtomicBoolean(false);
        Short result = AtomicBooleanConversions.toShort(ab, defaultConverter());
        assertEquals(CommonValues.SHORT_ZERO, result);
    }

    @Test
    public void testToInt_true() {
        AtomicBoolean ab = new AtomicBoolean(true);
        Integer result = AtomicBooleanConversions.toInt(ab, defaultConverter());
        assertEquals(CommonValues.INTEGER_ONE, result);
    }

    @Test
    public void testToInt_false() {
        AtomicBoolean ab = new AtomicBoolean(false);
        Integer result = AtomicBooleanConversions.toInt(ab, defaultConverter());
        assertEquals(CommonValues.INTEGER_ZERO, result);
    }

    @Test
    public void testToLong_true() {
        AtomicBoolean ab = new AtomicBoolean(true);
        Long result = AtomicBooleanConversions.toLong(ab, defaultConverter());
        assertEquals(CommonValues.LONG_ONE, result);
    }

    @Test
    public void testToLong_false() {
        AtomicBoolean ab = new AtomicBoolean(false);
        Long result = AtomicBooleanConversions.toLong(ab, defaultConverter());
        assertEquals(CommonValues.LONG_ZERO, result);
    }

    @Test
    public void testToFloat_true() {
        AtomicBoolean ab = new AtomicBoolean(true);
        Float result = AtomicBooleanConversions.toFloat(ab, defaultConverter());
        assertEquals(CommonValues.FLOAT_ONE, result);
    }

    @Test
    public void testToFloat_false() {
        AtomicBoolean ab = new AtomicBoolean(false);
        Float result = AtomicBooleanConversions.toFloat(ab, defaultConverter());
        assertEquals(CommonValues.FLOAT_ZERO, result);
    }

    @Test
    public void testToDouble_true() {
        AtomicBoolean ab = new AtomicBoolean(true);
        Double result = AtomicBooleanConversions.toDouble(ab, defaultConverter());
        assertEquals(CommonValues.DOUBLE_ONE, result);
    }

    @Test
    public void testToDouble_false() {
        AtomicBoolean ab = new AtomicBoolean(false);
        Double result = AtomicBooleanConversions.toDouble(ab, defaultConverter());
        assertEquals(CommonValues.DOUBLE_ZERO, result);
    }

    @Test
    public void testToBoolean_true() {
        AtomicBoolean ab = new AtomicBoolean(true);
        boolean result = AtomicBooleanConversions.toBoolean(ab, defaultConverter());
        assertTrue(result);
    }

    @Test
    public void testToBoolean_false() {
        AtomicBoolean ab = new AtomicBoolean(false);
        boolean result = AtomicBooleanConversions.toBoolean(ab, defaultConverter());
        assertFalse(result);
    }

    @Test
    public void testToAtomicBoolean_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, defaultConverter());
        assertNotSame(source, result);
        assertTrue(result.get());
    }

    @Test
    public void testToAtomicBoolean_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, defaultConverter());
        assertNotSame(source, result);
        assertFalse(result.get());
    }

    @Test
    public void testToAtomicInteger_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, defaultConverter());
        assertEquals(1, result.get());
    }

    @Test
    public void testToAtomicInteger_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, defaultConverter());
        assertEquals(0, result.get());
    }

    @Test
    public void testToAtomicLong_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, defaultConverter());
        assertEquals(1L, result.get());
    }

    @Test
    public void testToAtomicLong_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, defaultConverter());
        assertEquals(0L, result.get());
    }

    @Test
    public void testToCharacter_trueDefault() {
        AtomicBoolean source = new AtomicBoolean(true);
        char expected = defaultConverter().getOptions().trueChar();
        char result = AtomicBooleanConversions.toCharacter(source, defaultConverter());
        assertEquals(expected, result);
    }

    @Test
    public void testToCharacter_falseDefault() {
        AtomicBoolean source = new AtomicBoolean(false);
        char expected = defaultConverter().getOptions().falseChar();
        char result = AtomicBooleanConversions.toCharacter(source, defaultConverter());
        assertEquals(expected, result);
    }

    @Test
    public void testToCharacter_customOptions() {
        Converter conv = customCharConverter('Y', 'N');
        AtomicBoolean trueSrc = new AtomicBoolean(true);
        AtomicBoolean falseSrc = new AtomicBoolean(false);
        assertEquals('Y', AtomicBooleanConversions.toCharacter(trueSrc, conv));
        assertEquals('N', AtomicBooleanConversions.toCharacter(falseSrc, conv));
    }

    @Test
    public void testToBigDecimal_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, defaultConverter());
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    public void testToBigDecimal_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, defaultConverter());
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testToBigInteger_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, defaultConverter());
        assertEquals(BigInteger.ONE, result);
    }

    @Test
    public void testToBigInteger_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, defaultConverter());
        assertEquals(BigInteger.ZERO, result);
    }

    /* ---------------------------------------------------------------------- */
    /* ---------------------------- EDGE CASES ----------------------------- */
    /* ---------------------------------------------------------------------- */

    // ----- null source object ------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToByte_nullSource() {
        AtomicBooleanConversions.toByte(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToShort_nullSource() {
        AtomicBooleanConversions.toShort(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToInt_nullSource() {
        AtomicBooleanConversions.toInt(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_nullSource() {
        AtomicBooleanConversions.toLong(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToFloat_nullSource() {
        AtomicBooleanConversions.toFloat(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_nullSource() {
        AtomicBooleanConversions.toDouble(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToBoolean_nullSource() {
        AtomicBooleanConversions.toBoolean(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBoolean_nullSource() {
        AtomicBooleanConversions.toAtomicBoolean(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_nullSource() {
        AtomicBooleanConversions.toAtomicInteger(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_nullSource() {
        AtomicBooleanConversions.toAtomicLong(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacter_nullSource() {
        AtomicBooleanConversions.toCharacter(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_nullSource() {
        AtomicBooleanConversions.toBigDecimal(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_nullSource() {
        AtomicBooleanConversions.toBigInteger(null, defaultConverter());
    }

    // ----- source of wrong type (not AtomicBoolean) -------------------------
    @Test(expected = ClassCastException.class)
    public void testToByte_wrongType() {
        AtomicBooleanConversions.toByte("not an atomic boolean", defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToShort_wrongType() {
        AtomicBooleanConversions.toShort(123, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_wrongType() {
        AtomicBooleanConversions.toInt(Boolean.TRUE, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_wrongType() {
        AtomicBooleanConversions.toLong(new Object(), defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToFloat_wrongType() {
        AtomicBooleanConversions.toFloat(new AtomicInteger(1), defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_wrongType() {
        AtomicBooleanConversions.toDouble(new AtomicLong(1L), defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToBoolean_wrongType() {
        AtomicBooleanConversions.toBoolean(Boolean.FALSE, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBoolean_wrongType() {
        AtomicBooleanConversions.toAtomicBoolean(Boolean.TRUE, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_wrongType() {
        AtomicBooleanConversions.toAtomicInteger(Boolean.FALSE, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_wrongType() {
        AtomicBooleanConversions.toAtomicLong("true", defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacter_wrongType() {
        AtomicBooleanConversions.toCharacter(1, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_wrongType() {
        AtomicBooleanConversions.toBigDecimal(Boolean.TRUE, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_wrongType() {
        AtomicBooleanConversions.toBigInteger(new Object(), defaultConverter());
    }

    /* ---------------------------------------------------------------------- */
    /* -------------------------- ALIASING CHECKS -------------------------- */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAtomicBooleanConversionCreatesNewInstance() {
        AtomicBoolean original = new AtomicBoolean(true);
        AtomicBoolean converted = AtomicBooleanConversions.toAtomicBoolean(original, defaultConverter());
        assertNotSame("Conversion should not return the original instance", original, converted);
        // Changing the original must not affect the converted value.
        original.set(false);
        assertTrue("Converted instance must retain its own state", converted.get());
    }

    @Test
    public void testAtomicIntegerConversionCreatesNewInstance() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, defaultConverter());
        assertNotNull(result);
        assertEquals(0, result.get());
        // The result is independent of future changes to the source.
        source.set(true);
        assertEquals(0, result.get());
    }

    @Test
    public void testAtomicLongConversionCreatesNewInstance() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, defaultConverter());
        assertNotNull(result);
        assertEquals(1L, result.get());
        source.set(false);
        assertEquals(1L, result.get());
    }
}
