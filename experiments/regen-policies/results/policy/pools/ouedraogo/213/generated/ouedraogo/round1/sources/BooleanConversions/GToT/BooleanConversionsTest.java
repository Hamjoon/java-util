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
 * The tests are organised in three logical groups:
 *
 * 1️⃣  **Happy‑path tests** – verify that each conversion method returns the
 *     correct value when given a legitimate {@link Boolean} input.
 *
 * 2️⃣  **Edge‑case / error handling tests** – exercise the behaviour when
 *     {@code null} or an incompatible object is supplied, or when the
 *     {@link Converter} argument is {@code null} (only {@code toCharacter}
 *     depends on the converter).
 *
 * 3️⃣  **Option‑driven test** – confirm that {@code toCharacter} respects the
 *     {@link ConverterOptions} supplied to the {@link Converter}.
 *
 * All tests reside in the same package as {@code BooleanConversions} so that
 * the package‑private static methods are directly accessible without reflection.
 */
public class BooleanConversionsTest {

    /* --------------------------------------------------------------
     *  Helper factory methods
     * -------------------------------------------------------------- */

    /** Simple {@link Converter} that uses default {@link ConverterOptions}. */
    private static Converter defaultConverter() {
        return new Converter(new ConverterOptions());
    }

    /** {@link Converter} with custom true/false characters. */
    private static Converter customCharConverter(char trueChar, char falseChar) {
        ConverterOptions opts = new ConverterOptions();
        opts.setTrueChar(trueChar);
        opts.setFalseChar(falseChar);
        return new Converter(opts);
    }

    /* --------------------------------------------------------------
     *  1️⃣  Happy‑path tests
     * -------------------------------------------------------------- */

    @Test
    public void testToByte_true() {
        assertEquals(CommonValues.BYTE_ONE, BooleanConversions.toByte(Boolean.TRUE, defaultConverter()));
    }

    @Test
    public void testToByte_false() {
        assertEquals(CommonValues.BYTE_ZERO, BooleanConversions.toByte(Boolean.FALSE, defaultConverter()));
    }

    @Test
    public void testToShort_true() {
        assertEquals(CommonValues.SHORT_ONE, BooleanConversions.toShort(Boolean.TRUE, defaultConverter()));
    }

    @Test
    public void testToShort_false() {
        assertEquals(CommonValues.SHORT_ZERO, BooleanConversions.toShort(Boolean.FALSE, defaultConverter()));
    }

    @Test
    public void testToInt_true() {
        assertEquals(CommonValues.INTEGER_ONE, BooleanConversions.toInt(Boolean.TRUE, defaultConverter()));
    }

    @Test
    public void testToInt_false() {
        assertEquals(CommonValues.INTEGER_ZERO, BooleanConversions.toInt(Boolean.FALSE, defaultConverter()));
    }

    @Test
    public void testToAtomicInteger_true() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.TRUE, defaultConverter());
        assertEquals(1, ai.get());
    }

    @Test
    public void testToAtomicInteger_false() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.FALSE, defaultConverter());
        assertEquals(0, ai.get());
    }

    @Test
    public void testToAtomicLong_true() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.TRUE, defaultConverter());
        assertEquals(1L, al.get());
    }

    @Test
    public void testToAtomicLong_false() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.FALSE, defaultConverter());
        assertEquals(0L, al.get());
    }

    @Test
    public void testToAtomicBoolean_true() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.TRUE, defaultConverter());
        assertTrue(ab.get());
    }

    @Test
    public void testToAtomicBoolean_false() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.FALSE, defaultConverter());
        assertFalse(ab.get());
    }

    @Test
    public void testToLong_true() {
        assertEquals(CommonValues.LONG_ONE, BooleanConversions.toLong(Boolean.TRUE, defaultConverter()));
    }

    @Test
    public void testToLong_false() {
        assertEquals(CommonValues.LONG_ZERO, BooleanConversions.toLong(Boolean.FALSE, defaultConverter()));
    }

    @Test
    public void testToBigDecimal_true() {
        assertEquals(BigDecimal.ONE, BooleanConversions.toBigDecimal(Boolean.TRUE, defaultConverter()));
    }

    @Test
    public void testToBigDecimal_false() {
        assertEquals(BigDecimal.ZERO, BooleanConversions.toBigDecimal(Boolean.FALSE, defaultConverter()));
    }

    @Test
    public void testToBigInteger_true() {
        assertEquals(BigInteger.ONE, BooleanConversions.toBigInteger(Boolean.TRUE, defaultConverter()));
    }

    @Test
    public void testToBigInteger_false() {
        assertEquals(BigInteger.ZERO, BooleanConversions.toBigInteger(Boolean.FALSE, defaultConverter()));
    }

    @Test
    public void testToFloat_true() {
        assertEquals(CommonValues.FLOAT_ONE, BooleanConversions.toFloat(Boolean.TRUE, defaultConverter()));
    }

    @Test
    public void testToFloat_false() {
        assertEquals(CommonValues.FLOAT_ZERO, BooleanConversions.toFloat(Boolean.FALSE, defaultConverter()));
    }

    @Test
    public void testToDouble_true() {
        assertEquals(CommonValues.DOUBLE_ONE, BooleanConversions.toDouble(Boolean.TRUE, defaultConverter()));
    }

    @Test
    public void testToDouble_false() {
        assertEquals(CommonValues.DOUBLE_ZERO, BooleanConversions.toDouble(Boolean.FALSE, defaultConverter()));
    }

    @Test
    public void testToCharacter_true_defaultOptions() {
        char expected = defaultConverter().getOptions().trueChar();
        assertEquals(expected, BooleanConversions.toCharacter(Boolean.TRUE, defaultConverter()));
    }

    @Test
    public void testToCharacter_false_defaultOptions() {
        char expected = defaultConverter().getOptions().falseChar();
        assertEquals(expected, BooleanConversions.toCharacter(Boolean.FALSE, defaultConverter()));
    }

    @Test
    public void testToCharacter_true_customOptions() {
        Converter conv = customCharConverter('Y', 'N');
        assertEquals('Y', BooleanConversions.toCharacter(Boolean.TRUE, conv));
    }

    @Test
    public void testToCharacter_false_customOptions() {
        Converter conv = customCharConverter('Y', 'N');
        assertEquals('N', BooleanConversions.toCharacter(Boolean.FALSE, conv));
    }

    /* --------------------------------------------------------------
     *  2️⃣  Edge‑case / error handling tests
     * -------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToByte_nullFrom() {
        BooleanConversions.toByte(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToShort_nullFrom() {
        BooleanConversions.toShort(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToInt_nullFrom() {
        BooleanConversions.toInt(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_nullFrom() {
        BooleanConversions.toAtomicInteger(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_nullFrom() {
        BooleanConversions.toAtomicLong(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBoolean_nullFrom() {
        BooleanConversions.toAtomicBoolean(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_nullFrom() {
        BooleanConversions.toLong(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_nullFrom() {
        BooleanConversions.toBigDecimal(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_nullFrom() {
        BooleanConversions.toBigInteger(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToFloat_nullFrom() {
        BooleanConversions.toFloat(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_nullFrom() {
        BooleanConversions.toDouble(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacter_nullFrom() {
        BooleanConversions.toCharacter(null, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacter_nullConverter() {
        BooleanConversions.toCharacter(Boolean.TRUE, null);
    }

    @Test(expected = ClassCastException.class)
    public void testToByte_nonBooleanFrom() {
        BooleanConversions.toByte("not a boolean", defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToShort_nonBooleanFrom() {
        BooleanConversions.toShort(123, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_nonBooleanFrom() {
        BooleanConversions.toInt(new Object(), defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_nonBooleanFrom() {
        BooleanConversions.toAtomicInteger(0.0, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_nonBooleanFrom() {
        BooleanConversions.toAtomicLong(Boolean.TRUE.toString(), defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBoolean_nonBooleanFrom() {
        BooleanConversions.toAtomicBoolean(Boolean.FALSE ? "yes" : 0, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_nonBooleanFrom() {
        BooleanConversions.toLong(new int[]{1,2,3}, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_nonBooleanFrom() {
        BooleanConversions.toBigDecimal(Boolean.FALSE ? 1 : "oops", defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_nonBooleanFrom() {
        BooleanConversions.toBigInteger(42L, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToFloat_nonBooleanFrom() {
        BooleanConversions.toFloat('c', defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_nonBooleanFrom() {
        BooleanConversions.toDouble(null, defaultConverter()); // null already covered, use another type
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacter_nonBooleanFrom() {
        BooleanConversions.toCharacter(3.14, defaultConverter());
    }
}
