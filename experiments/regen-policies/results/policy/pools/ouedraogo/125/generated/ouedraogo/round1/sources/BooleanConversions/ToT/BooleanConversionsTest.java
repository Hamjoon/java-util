package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit‑4 test suite for {@code BooleanConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Typical conversions for {@code true} and {@code false}.</li>
 *   <li>Edge cases – {@code null} input and non‑Boolean input – which must result in
 *       {@link NullPointerException} or {@link ClassCastException} respectively.</li>
 *   <li>Verification that the {@code Converter} argument is ignored for all methods
 *       except {@code toCharacter} and that {@code toCharacter} honours the
 *       {@link ConverterOptions} configuration.</li>
 * </ul>
 *
 * The test class resides in the same package as {@code BooleanConversions} so that
 * package‑private static methods are directly accessible.
 */
public class BooleanConversionsTest {

    /** Helper that creates a {@link Converter} with default options. */
    private static Converter defaultConverter() {
        // The public API shows a constructor that accepts a ConverterOptions instance.
        // A no‑arg constructor for ConverterOptions is assumed to exist.
        return new Converter(new ConverterOptions());
    }

    /* -------------------------------------------------
     *  Boolean → Byte
     * ------------------------------------------------- */
    @Test
    public void toByte_true_returnsOne() {
        Byte result = BooleanConversions.toByte(Boolean.TRUE, defaultConverter());
        assertEquals(CommonValues.BYTE_ONE, result);
    }

    @Test
    public void toByte_false_returnsZero() {
        Byte result = BooleanConversions.toByte(Boolean.FALSE, defaultConverter());
        assertEquals(CommonValues.BYTE_ZERO, result);
    }

    @Test(expected = NullPointerException.class)
    public void toByte_nullInput_throwsNPE() {
        BooleanConversions.toByte(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toByte_nonBooleanInput_throwsCCE() {
        BooleanConversions.toByte("not a boolean", defaultConverter());
    }

    /* -------------------------------------------------
     *  Boolean → Short
     * ------------------------------------------------- */
    @Test
    public void toShort_true_returnsOne() {
        Short result = BooleanConversions.toShort(Boolean.TRUE, defaultConverter());
        assertEquals(CommonValues.SHORT_ONE, result);
    }

    @Test
    public void toShort_false_returnsZero() {
        Short result = BooleanConversions.toShort(Boolean.FALSE, defaultConverter());
        assertEquals(CommonValues.SHORT_ZERO, result);
    }

    @Test(expected = NullPointerException.class)
    public void toShort_nullInput_throwsNPE() {
        BooleanConversions.toShort(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toShort_nonBooleanInput_throwsCCE() {
        BooleanConversions.toShort(123, defaultConverter());
    }

    /* -------------------------------------------------
     *  Boolean → Integer
     * ------------------------------------------------- */
    @Test
    public void toInt_true_returnsOne() {
        Integer result = BooleanConversions.toInt(Boolean.TRUE, defaultConverter());
        assertEquals(CommonValues.INTEGER_ONE, result);
    }

    @Test
    public void toInt_false_returnsZero() {
        Integer result = BooleanConversions.toInt(Boolean.FALSE, defaultConverter());
        assertEquals(CommonValues.INTEGER_ZERO, result);
    }

    @Test(expected = NullPointerException.class)
    public void toInt_nullInput_throwsNPE() {
        BooleanConversions.toInt(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toInt_nonBooleanInput_throwsCCE() {
        BooleanConversions.toInt(new Object(), defaultConverter());
    }

    /* -------------------------------------------------
     *  Boolean → AtomicInteger
     * ------------------------------------------------- */
    @Test
    public void toAtomicInteger_true_returnsOne() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.TRUE, defaultConverter());
        assertEquals(1, ai.get());
    }

    @Test
    public void toAtomicInteger_false_returnsZero() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.FALSE, defaultConverter());
        assertEquals(0, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void toAtomicInteger_nullInput_throwsNPE() {
        BooleanConversions.toAtomicInteger(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toAtomicInteger_nonBooleanInput_throwsCCE() {
        BooleanConversions.toAtomicInteger(3.14, defaultConverter());
    }

    /* -------------------------------------------------
     *  Boolean → AtomicLong
     * ------------------------------------------------- */
    @Test
    public void toAtomicLong_true_returnsOne() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.TRUE, defaultConverter());
        assertEquals(1L, al.get());
    }

    @Test
    public void toAtomicLong_false_returnsZero() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.FALSE, defaultConverter());
        assertEquals(0L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void toAtomicLong_nullInput_throwsNPE() {
        BooleanConversions.toAtomicLong(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toAtomicLong_nonBooleanInput_throwsCCE() {
        BooleanConversions.toAtomicLong(new Object(), defaultConverter());
    }

    /* -------------------------------------------------
     *  Boolean → AtomicBoolean
     * ------------------------------------------------- */
    @Test
    public void toAtomicBoolean_true_returnsTrue() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.TRUE, defaultConverter());
        assertTrue(ab.get());
    }

    @Test
    public void toAtomicBoolean_false_returnsFalse() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.FALSE, defaultConverter());
        assertFalse(ab.get());
    }

    @Test(expected = NullPointerException.class)
    public void toAtomicBoolean_nullInput_throwsNPE() {
        BooleanConversions.toAtomicBoolean(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toAtomicBoolean_nonBooleanInput_throwsCCE() {
        BooleanConversions.toAtomicBoolean(42, defaultConverter());
    }

    /* -------------------------------------------------
     *  Boolean → Long
     * ------------------------------------------------- */
    @Test
    public void toLong_true_returnsOne() {
        Long result = BooleanConversions.toLong(Boolean.TRUE, defaultConverter());
        assertEquals(CommonValues.LONG_ONE, result);
    }

    @Test
    public void toLong_false_returnsZero() {
        Long result = BooleanConversions.toLong(Boolean.FALSE, defaultConverter());
        assertEquals(CommonValues.LONG_ZERO, result);
    }

    @Test(expected = NullPointerException.class)
    public void toLong_nullInput_throwsNPE() {
        BooleanConversions.toLong(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toLong_nonBooleanInput_throwsCCE() {
        BooleanConversions.toLong(new Object(), defaultConverter());
    }

    /* -------------------------------------------------
     *  Boolean → BigDecimal
     * ------------------------------------------------- */
    @Test
    public void toBigDecimal_true_returnsOne() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.TRUE, defaultConverter());
        assertEquals(BigDecimal.ONE, bd);
    }

    @Test
    public void toBigDecimal_false_returnsZero() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.FALSE, defaultConverter());
        assertEquals(BigDecimal.ZERO, bd);
    }

    @Test(expected = NullPointerException.class)
    public void toBigDecimal_nullInput_throwsNPE() {
        BooleanConversions.toBigDecimal(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toBigDecimal_nonBooleanInput_throwsCCE() {
        BooleanConversions.toBigDecimal(123L, defaultConverter());
    }

    /* -------------------------------------------------
     *  Boolean → BigInteger
     * ------------------------------------------------- */
    @Test
    public void toBigInteger_true_returnsOne() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.TRUE, defaultConverter());
        assertEquals(BigInteger.ONE, bi);
    }

    @Test
    public void toBigInteger_false_returnsZero() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.FALSE, defaultConverter());
        assertEquals(BigInteger.ZERO, bi);
    }

    @Test(expected = NullPointerException.class)
    public void toBigInteger_nullInput_throwsNPE() {
        BooleanConversions.toBigInteger(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toBigInteger_nonBooleanInput_throwsCCE() {
        BooleanConversions.toBigInteger(new Object(), defaultConverter());
    }

    /* -------------------------------------------------
     *  Boolean → Float
     * ------------------------------------------------- */
    @Test
    public void toFloat_true_returnsOne() {
        Float f = BooleanConversions.toFloat(Boolean.TRUE, defaultConverter());
        assertEquals(CommonValues.FLOAT_ONE, f);
    }

    @Test
    public void toFloat_false_returnsZero() {
        Float f = BooleanConversions.toFloat(Boolean.FALSE, defaultConverter());
        assertEquals(CommonValues.FLOAT_ZERO, f);
    }

    @Test(expected = NullPointerException.class)
    public void toFloat_nullInput_throwsNPE() {
        BooleanConversions.toFloat(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toFloat_nonBooleanInput_throwsCCE() {
        BooleanConversions.toFloat(new Object(), defaultConverter());
    }

    /* -------------------------------------------------
     *  Boolean → Double
     * ------------------------------------------------- */
    @Test
    public void toDouble_true_returnsOne() {
        Double d = BooleanConversions.toDouble(Boolean.TRUE, defaultConverter());
        assertEquals(CommonValues.DOUBLE_ONE, d);
    }

    @Test
    public void toDouble_false_returnsZero() {
        Double d = BooleanConversions.toDouble(Boolean.FALSE, defaultConverter());
        assertEquals(CommonValues.DOUBLE_ZERO, d);
    }

    @Test(expected = NullPointerException.class)
    public void toDouble_nullInput_throwsNPE() {
        BooleanConversions.toDouble(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toDouble_nonBooleanInput_throwsCCE() {
        BooleanConversions.toDouble(99, defaultConverter());
    }

    /* -------------------------------------------------
     *  Boolean → char (via ConverterOptions)
     * ------------------------------------------------- */
    @Test
    public void toCharacter_true_usesTrueCharFromOptions() {
        // Create options with known true/false characters.
        ConverterOptions opts = new ConverterOptions() {
            @Override public char trueChar()  { return 'Y'; }
            @Override public char falseChar() { return 'N'; }
        };
        Converter conv = new Converter(opts);

        char c = BooleanConversions.toCharacter(Boolean.TRUE, conv);
        assertEquals('Y', c);
    }

    @Test
    public void toCharacter_false_usesFalseCharFromOptions() {
        ConverterOptions opts = new ConverterOptions() {
            @Override public char trueChar()  { return '+'; }
            @Override public char falseChar() { return '-'; }
        };
        Converter conv = new Converter(opts);

        char c = BooleanConversions.toCharacter(Boolean.FALSE, conv);
        assertEquals('-', c);
    }

    @Test(expected = NullPointerException.class)
    public void toCharacter_nullInput_throwsNPE() {
        BooleanConversions.toCharacter(null, defaultConverter());
    }

    @Test(expected = ClassCastException.class)
    public void toCharacter_nonBooleanInput_throwsCCE() {
        BooleanConversions.toCharacter(0, defaultConverter());
    }

    @Test(expected = NullPointerException.class)
    public void toCharacter_nullConverter_throwsNPE() {
        // The method dereferences converter.getOptions(); passing null must NPE.
        BooleanConversions.toCharacter(Boolean.TRUE, null);
    }
}
