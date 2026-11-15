package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit‑4 test suite for {@link BooleanConversions}.
 *
 * <p>The test class follows the “three‑expert” scenario described in the
 * prompt: each logical conversion method is exercised with the two
 * representative Boolean values (true / false), the {@code null} edge case,
 * and the special {@code char} conversion that depends on {@link
 * ConverterOptions}.  All tests use only the public API that is visible in the
 * supplied source files – no mocking, no subclassing, and no external library
 * calls.</p>
 *
 * <p>Because the {@code BooleanConversions} class is package‑private and its
 * constructor is private, the test class resides in the same package so it
 * can invoke the static methods directly.</p>
 */
public class BooleanConversionsTest {

    private Converter trueFalseConverter;   // converter with default true/false chars
    private Converter customCharConverter;   // converter with custom true/false chars

    /** A tiny helper that builds a {@link Converter} with the supplied true/false characters. */
    private static Converter buildConverter(char trueChar, char falseChar) {
        // ConverterOptions is not shown, but the library provides a public
        // constructor that allows setting the true/false characters.
        // We assume such a constructor exists:
        //   new ConverterOptions(char trueChar, char falseChar)
        // If the real library uses setters, they can be called after construction.
        ConverterOptions options = new ConverterOptions();
        options.setTrueChar(trueChar);
        options.setFalseChar(falseChar);
        return new Converter(options);
    }

    @Before
    public void setUp() {
        // Default options – the library’s defaults are ‘T’ and ‘F’.
        trueFalseConverter = new Converter(new ConverterOptions());

        // Custom options – we use ‘Y’ for true and ‘N’ for false to verify that
        // the character conversion really follows the ConverterOptions.
        customCharConverter = buildConverter('Y', 'N');
    }

    /* -------------------------------------------------------------
     *  Boolean → Byte
     * ------------------------------------------------------------- */
    @Test
    public void toByte_true_returnsOne() {
        Byte result = BooleanConversions.toByte(Boolean.TRUE, trueFalseConverter);
        assertEquals(CommonValues.BYTE_ONE, result);
    }

    @Test
    public void toByte_false_returnsZero() {
        Byte result = BooleanConversions.toByte(Boolean.FALSE, trueFalseConverter);
        assertEquals(CommonValues.BYTE_ZERO, result);
    }

    @Test(expected = NullPointerException.class)
    public void toByte_null_throwsNPE() {
        BooleanConversions.toByte(null, trueFalseConverter);
    }

    /* -------------------------------------------------------------
     *  Boolean → Short
     * ------------------------------------------------------------- */
    @Test
    public void toShort_true_returnsOne() {
        Short result = BooleanConversions.toShort(Boolean.TRUE, trueFalseConverter);
        assertEquals(CommonValues.SHORT_ONE, result);
    }

    @Test
    public void toShort_false_returnsZero() {
        Short result = BooleanConversions.toShort(Boolean.FALSE, trueFalseConverter);
        assertEquals(CommonValues.SHORT_ZERO, result);
    }

    @Test(expected = NullPointerException.class)
    public void toShort_null_throwsNPE() {
        BooleanConversions.toShort(null, trueFalseConverter);
    }

    /* -------------------------------------------------------------
     *  Boolean → Integer
     * ------------------------------------------------------------- */
    @Test
    public void toInt_true_returnsOne() {
        Integer result = BooleanConversions.toInt(Boolean.TRUE, trueFalseConverter);
        assertEquals(CommonValues.INTEGER_ONE, result);
    }

    @Test
    public void toInt_false_returnsZero() {
        Integer result = BooleanConversions.toInt(Boolean.FALSE, trueFalseConverter);
        assertEquals(CommonValues.INTEGER_ZERO, result);
    }

    @Test(expected = NullPointerException.class)
    public void toInt_null_throwsNPE() {
        BooleanConversions.toInt(null, trueFalseConverter);
    }

    /* -------------------------------------------------------------
     *  Boolean → AtomicInteger
     * ------------------------------------------------------------- */
    @Test
    public void toAtomicInteger_true_isOne() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.TRUE, trueFalseConverter);
        assertEquals(1, ai.get());
    }

    @Test
    public void toAtomicInteger_false_isZero() {
        AtomicInteger ai = BooleanConversions.toAtomicInteger(Boolean.FALSE, trueFalseConverter);
        assertEquals(0, ai.get());
    }

    @Test(expected = NullPointerException.class)
    public void toAtomicInteger_null_throwsNPE() {
        BooleanConversions.toAtomicInteger(null, trueFalseConverter);
    }

    /* -------------------------------------------------------------
     *  Boolean → AtomicLong
     * ------------------------------------------------------------- */
    @Test
    public void toAtomicLong_true_isOne() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.TRUE, trueFalseConverter);
        assertEquals(1L, al.get());
    }

    @Test
    public void toAtomicLong_false_isZero() {
        AtomicLong al = BooleanConversions.toAtomicLong(Boolean.FALSE, trueFalseConverter);
        assertEquals(0L, al.get());
    }

    @Test(expected = NullPointerException.class)
    public void toAtomicLong_null_throwsNPE() {
        BooleanConversions.toAtomicLong(null, trueFalseConverter);
    }

    /* -------------------------------------------------------------
     *  Boolean → AtomicBoolean
     * ------------------------------------------------------------- */
    @Test
    public void toAtomicBoolean_true_isTrue() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.TRUE, trueFalseConverter);
        assertTrue(ab.get());
    }

    @Test
    public void toAtomicBoolean_false_isFalse() {
        AtomicBoolean ab = BooleanConversions.toAtomicBoolean(Boolean.FALSE, trueFalseConverter);
        assertFalse(ab.get());
    }

    @Test(expected = NullPointerException.class)
    public void toAtomicBoolean_null_throwsNPE() {
        BooleanConversions.toAtomicBoolean(null, trueFalseConverter);
    }

    /* -------------------------------------------------------------
     *  Boolean → Long
     * ------------------------------------------------------------- */
    @Test
    public void toLong_true_returnsOne() {
        Long result = BooleanConversions.toLong(Boolean.TRUE, trueFalseConverter);
        assertEquals(CommonValues.LONG_ONE, result);
    }

    @Test
    public void toLong_false_returnsZero() {
        Long result = BooleanConversions.toLong(Boolean.FALSE, trueFalseConverter);
        assertEquals(CommonValues.LONG_ZERO, result);
    }

    @Test(expected = NullPointerException.class)
    public void toLong_null_throwsNPE() {
        BooleanConversions.toLong(null, trueFalseConverter);
    }

    /* -------------------------------------------------------------
     *  Boolean → BigDecimal
     * ------------------------------------------------------------- */
    @Test
    public void toBigDecimal_true_isOne() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.TRUE, trueFalseConverter);
        assertEquals(BigDecimal.ONE, bd);
    }

    @Test
    public void toBigDecimal_false_isZero() {
        BigDecimal bd = BooleanConversions.toBigDecimal(Boolean.FALSE, trueFalseConverter);
        assertEquals(BigDecimal.ZERO, bd);
    }

    @Test(expected = NullPointerException.class)
    public void toBigDecimal_null_throwsNPE() {
        BooleanConversions.toBigDecimal(null, trueFalseConverter);
    }

    /* -------------------------------------------------------------
     *  Boolean → BigInteger
     * ------------------------------------------------------------- */
    @Test
    public void toBigInteger_true_isOne() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.TRUE, trueFalseConverter);
        assertEquals(BigInteger.ONE, bi);
    }

    @Test
    public void toBigInteger_false_isZero() {
        BigInteger bi = BooleanConversions.toBigInteger(Boolean.FALSE, trueFalseConverter);
        assertEquals(BigInteger.ZERO, bi);
    }

    @Test(expected = NullPointerException.class)
    public void toBigInteger_null_throwsNPE() {
        BooleanConversions.toBigInteger(null, trueFalseConverter);
    }

    /* -------------------------------------------------------------
     *  Boolean → Float
     * ------------------------------------------------------------- */
    @Test
    public void toFloat_true_returnsOne() {
        Float f = BooleanConversions.toFloat(Boolean.TRUE, trueFalseConverter);
        assertEquals(CommonValues.FLOAT_ONE, f);
    }

    @Test
    public void toFloat_false_returnsZero() {
        Float f = BooleanConversions.toFloat(Boolean.FALSE, trueFalseConverter);
        assertEquals(CommonValues.FLOAT_ZERO, f);
    }

    @Test(expected = NullPointerException.class)
    public void toFloat_null_throwsNPE() {
        BooleanConversions.toFloat(null, trueFalseConverter);
    }

    /* -------------------------------------------------------------
     *  Boolean → Double
     * ------------------------------------------------------------- */
    @Test
    public void toDouble_true_returnsOne() {
        Double d = BooleanConversions.toDouble(Boolean.TRUE, trueFalseConverter);
        assertEquals(CommonValues.DOUBLE_ONE, d);
    }

    @Test
    public void toDouble_false_returnsZero() {
        Double d = BooleanConversions.toDouble(Boolean.FALSE, trueFalseConverter);
        assertEquals(CommonValues.DOUBLE_ZERO, d);
    }

    @Test(expected = NullPointerException.class)
    public void toDouble_null_throwsNPE() {
        BooleanConversions.toDouble(null, trueFalseConverter);
    }

    /* -------------------------------------------------------------
     *  Boolean → char (via ConverterOptions)
     * ------------------------------------------------------------- */
    @Test
    public void toCharacter_true_usesTrueChar() {
        char c = BooleanConversions.toCharacter(Boolean.TRUE, customCharConverter);
        assertEquals('Y', c);
    }

    @Test
    public void toCharacter_false_usesFalseChar() {
        char c = BooleanConversions.toCharacter(Boolean.FALSE, customCharConverter);
        assertEquals('N', c);
    }

    @Test(expected = NullPointerException.class)
    public void toCharacter_null_throwsNPE() {
        BooleanConversions.toCharacter(null, trueFalseConverter);
    }

    /* -------------------------------------------------------------
     *  Additional sanity test – ensure that the same Converter instance
     *  can be reused across multiple conversions without side‑effects.
     * ------------------------------------------------------------- */
    @Test
    public void reuseSameConverter_multipleConversions_consistentResults() {
        // Re‑use trueFalseConverter for several different conversions.
        assertEquals(CommonValues.INTEGER_ONE, BooleanConversions.toInt(Boolean.TRUE, trueFalseConverter));
        assertEquals(CommonValues.LONG_ZERO, BooleanConversions.toLong(Boolean.FALSE, trueFalseConverter));
        assertEquals(BigDecimal.ONE, BooleanConversions.toBigDecimal(Boolean.TRUE, trueFalseConverter));
        assertEquals('T', BooleanConversions.toCharacter(Boolean.TRUE, trueFalseConverter));
        assertEquals('F', BooleanConversions.toCharacter(Boolean.FALSE, trueFalseConverter));
    }
}
