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
 * JUnit‑4 test suite for {@code BooleanConversions}.
 *
 * The class under test contains only static conversion methods that accept an {@code Object}
 * (expected to be a {@link Boolean}) and a {@link Converter}.  The test cases therefore focus on:
 *
 * <ul>
 *   <li>Correct conversion of {@code true} and {@code false} values.</li>
 *   <li>Proper handling of {@code null} as the source value (should throw {@link NullPointerException}).</li>
 *   <li>Behaviour when the source value is not a {@code Boolean} (should throw {@link ClassCastException}).</li>
 *   <li>Verification that {@code toCharacter} respects the {@link ConverterOptions} configuration.</li>
 * </ul>
 *
 * The {@link Converter} and {@link ConverterOptions} classes are part of the same library.
 * For the purpose of these tests we use the real implementations (no mocking) and rely on the
 * default behaviour of {@code ConverterOptions}.  The default implementation returns the characters
 * {@code 'T'} for {@code true} and {@code 'F'} for {@code false}; the tests assert against those
 * values but also verify that the returned characters match the values supplied by the options
 * object, so the test remains valid if the defaults ever change.
 */
public class BooleanConversionsTest {

    /** A Converter instance that will be passed to every conversion method. */
    private Converter converter;

    /** The options object extracted from the converter – used for assertions on {@code toCharacter}. */
    private ConverterOptions options;

    @Before
    public void setUp() {
        // The library provides a public constructor that accepts a ConverterOptions instance.
        // Using the no‑arg constructor of ConverterOptions gives us the library defaults.
        this.options = new ConverterOptions();
        this.converter = new Converter(this.options);
    }

    /* --------------------------------------------------------------------- */
    /*  1.  List of public (actually package‑private static) methods to test  */
    /* --------------------------------------------------------------------- */
    // static Byte          toByte(Object from, Converter converter)
    // static Short         toShort(Object from, Converter converter)
    // static Integer       toInt(Object from, Converter converter)
    // static AtomicInteger toAtomicInteger(Object from, Converter converter)
    // static AtomicLong    toAtomicLong(Object from, Converter converter)
    // static AtomicBoolean toAtomicBoolean(Object from, Converter converter)
    // static Long          toLong(Object from, Converter converter)
    // static BigDecimal    toBigDecimal(Object from, Converter converter)
    // static BigInteger    toBigInteger(Object from, Converter converter)
    // static Float         toFloat(Object from, Converter converter)
    // static Double        toDouble(Object from, Converter converter)
    // static char          toCharacter(Object from, Converter converter)

    /* --------------------------- */
    /*  2.  Basic true/false tests  */
    /* --------------------------- */

    @Test
    public void testToByte() {
        assertEquals(Byte.valueOf((byte) 1), BooleanConversions.toByte(Boolean.TRUE, converter));
        assertEquals(Byte.valueOf((byte) 0), BooleanConversions.toByte(Boolean.FALSE, converter));
    }

    @Test
    public void testToShort() {
        assertEquals(Short.valueOf((short) 1), BooleanConversions.toShort(Boolean.TRUE, converter));
        assertEquals(Short.valueOf((short) 0), BooleanConversions.toShort(Boolean.FALSE, converter));
    }

    @Test
    public void testToInt() {
        assertEquals(Integer.valueOf(1), BooleanConversions.toInt(Boolean.TRUE, converter));
        assertEquals(Integer.valueOf(0), BooleanConversions.toInt(Boolean.FALSE, converter));
    }

    @Test
    public void testToAtomicInteger() {
        AtomicInteger aiTrue = BooleanConversions.toAtomicInteger(Boolean.TRUE, converter);
        AtomicInteger aiFalse = BooleanConversions.toAtomicInteger(Boolean.FALSE, converter);
        assertEquals(1, aiTrue.get());
        assertEquals(0, aiFalse.get());
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong alTrue = BooleanConversions.toAtomicLong(Boolean.TRUE, converter);
        AtomicLong alFalse = BooleanConversions.toAtomicLong(Boolean.FALSE, converter);
        assertEquals(1L, alTrue.get());
        assertEquals(0L, alFalse.get());
    }

    @Test
    public void testToAtomicBoolean() {
        AtomicBoolean abTrue = BooleanConversions.toAtomicBoolean(Boolean.TRUE, converter);
        AtomicBoolean abFalse = BooleanConversions.toAtomicBoolean(Boolean.FALSE, converter);
        assertTrue(abTrue.get());
        assertFalse(abFalse.get());
    }

    @Test
    public void testToLong() {
        assertEquals(Long.valueOf(1L), BooleanConversions.toLong(Boolean.TRUE, converter));
        assertEquals(Long.valueOf(0L), BooleanConversions.toLong(Boolean.FALSE, converter));
    }

    @Test
    public void testToBigDecimal() {
        assertEquals(BigDecimal.ONE, BooleanConversions.toBigDecimal(Boolean.TRUE, converter));
        assertEquals(BigDecimal.ZERO, BooleanConversions.toBigDecimal(Boolean.FALSE, converter));
    }

    @Test
    public void testToBigInteger() {
        assertEquals(BigInteger.ONE, BooleanConversions.toBigInteger(Boolean.TRUE, converter));
        assertEquals(BigInteger.ZERO, BooleanConversions.toBigInteger(Boolean.FALSE, converter));
    }

    @Test
    public void testToFloat() {
        assertEquals(Float.valueOf(1.0f), BooleanConversions.toFloat(Boolean.TRUE, converter));
        assertEquals(Float.valueOf(0.0f), BooleanConversions.toFloat(Boolean.FALSE, converter));
    }

    @Test
    public void testToDouble() {
        assertEquals(Double.valueOf(1.0d), BooleanConversions.toDouble(Boolean.TRUE, converter));
        assertEquals(Double.valueOf(0.0d), BooleanConversions.toDouble(Boolean.FALSE, converter));
    }

    @Test
    public void testToCharacter() {
        char expectedTrue = options.trueChar();
        char expectedFalse = options.falseChar();

        assertEquals(expectedTrue, BooleanConversions.toCharacter(Boolean.TRUE, converter));
        assertEquals(expectedFalse, BooleanConversions.toCharacter(Boolean.FALSE, converter));
    }

    /* --------------------------------------------------- */
    /*  3.  Edge‑case tests – null and wrong type handling */
    /* --------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToByte_NullInput() {
        BooleanConversions.toByte(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToShort_NullInput() {
        BooleanConversions.toShort(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToInt_NullInput() {
        BooleanConversions.toInt(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullInput() {
        BooleanConversions.toAtomicInteger(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullInput() {
        BooleanConversions.toAtomicLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBoolean_NullInput() {
        BooleanConversions.toAtomicBoolean(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        BooleanConversions.toLong(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullInput() {
        BooleanConversions.toBigDecimal(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullInput() {
        BooleanConversions.toBigInteger(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToFloat_NullInput() {
        BooleanConversions.toFloat(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullInput() {
        BooleanConversions.toDouble(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacter_NullInput() {
        BooleanConversions.toCharacter(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToByte_WrongType() {
        BooleanConversions.toByte("not a boolean", converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToShort_WrongType() {
        BooleanConversions.toShort(123, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_WrongType() {
        BooleanConversions.toInt(new Object(), converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicInteger_WrongType() {
        BooleanConversions.toAtomicInteger(1.23, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_WrongType() {
        BooleanConversions.toAtomicLong(new int[]{1, 2}, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicBoolean_WrongType() {
        BooleanConversions.toAtomicBoolean(Boolean.valueOf(true).toString(), converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType() {
        BooleanConversions.toLong(Boolean.valueOf(true).hashCode(), converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_WrongType() {
        BooleanConversions.toBigDecimal(Boolean.TRUE ? "yes" : "no", converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_WrongType() {
        BooleanConversions.toBigInteger(Boolean.FALSE ? 0 : 1, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToFloat_WrongType() {
        BooleanConversions.toFloat('c', converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_WrongType() {
        BooleanConversions.toDouble(3.14f, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacter_WrongType() {
        BooleanConversions.toCharacter(Boolean.TRUE ? 1 : 0, converter);
    }

    /* ----------------------------------------------------- */
    /*  4.  Additional sanity check – custom option values   */
    /* ----------------------------------------------------- */

    @Test
    public void testToCharacter_WithCustomOptions() {
        // Create a custom options instance where the true/false chars are non‑standard.
        ConverterOptions customOptions = new ConverterOptions() {
            @Override public char trueChar()  { return 'Y'; }
            @Override public char falseChar() { return 'N'; }
        };
        Converter customConverter = new Converter(customOptions);

        assertEquals('Y', BooleanConversions.toCharacter(Boolean.TRUE, customConverter));
        assertEquals('N', BooleanConversions.toCharacter(Boolean.FALSE, customConverter));
    }
}
