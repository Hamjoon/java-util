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
 * Comprehensive JUnit‑4 tests for {@link AtomicBooleanConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Normal conversion for {@code true} and {@code false} values.</li>
 *   <li>Edge cases – {@code null} source, wrong source type and {@code null} converter where required.</li>
 *   <li>Verification that the {@code ConverterOptions} supplied to {@link Converter}
 *       are honoured by {@code toCharacter}.</li>
 * </ul>
 */
public class AtomicBooleanConversionsTest {

    private Converter converter;
    private ConverterOptions options;

    @Before
    public void setUp() {
        // The library provides a public constructor for ConverterOptions (no‑arg).
        // If a different constructor is required the test can be adjusted accordingly.
        this.options = new ConverterOptions();               // default options
        this.converter = new Converter(this.options);
    }

    /* --------------------------------------------------------------------- */
    /* Helper to create AtomicBoolean instances for the two possible values */
    private AtomicBoolean trueAtomic()  { return new AtomicBoolean(true);  }
    private AtomicBoolean falseAtomic() { return new AtomicBoolean(false); }

    /* --------------------------------------------------------------------- */
    @Test
    public void testToByte() {
        assertEquals(CommonValues.BYTE_ONE,  AtomicBooleanConversions.toByte(trueAtomic(),  converter));
        assertEquals(CommonValues.BYTE_ZERO, AtomicBooleanConversions.toByte(falseAtomic(), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToByte_NullSource() {
        AtomicBooleanConversions.toByte(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToByte_WrongSourceType() {
        AtomicBooleanConversions.toByte("not an AtomicBoolean", converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    public void testToShort() {
        assertEquals(CommonValues.SHORT_ONE,  AtomicBooleanConversions.toShort(trueAtomic(),  converter));
        assertEquals(CommonValues.SHORT_ZERO, AtomicBooleanConversions.toShort(falseAtomic(), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToShort_NullSource() {
        AtomicBooleanConversions.toShort(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToShort_WrongSourceType() {
        AtomicBooleanConversions.toShort(123, converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    public void testToInt() {
        assertEquals(CommonValues.INTEGER_ONE,  AtomicBooleanConversions.toInt(trueAtomic(),  converter));
        assertEquals(CommonValues.INTEGER_ZERO, AtomicBooleanConversions.toInt(falseAtomic(), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToInt_NullSource() {
        AtomicBooleanConversions.toInt(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_WrongSourceType() {
        AtomicBooleanConversions.toInt(new Object(), converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    public void testToLong() {
        assertEquals(CommonValues.LONG_ONE,  AtomicBooleanConversions.toLong(trueAtomic(),  converter));
        assertEquals(CommonValues.LONG_ZERO, AtomicBooleanConversions.toLong(falseAtomic(), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullSource() {
        AtomicBooleanConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongSourceType() {
        AtomicBooleanConversions.toLong(Boolean.TRUE, converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    public void testToFloat() {
        assertEquals(CommonValues.FLOAT_ONE,  AtomicBooleanConversions.toFloat(trueAtomic(),  converter));
        assertEquals(CommonValues.FLOAT_ZERO, AtomicBooleanConversions.toFloat(falseAtomic(), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToFloat_NullSource() {
        AtomicBooleanConversions.toFloat(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToFloat_WrongSourceType() {
        AtomicBooleanConversions.toFloat(new AtomicInteger(5), converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    public void testToDouble() {
        assertEquals(CommonValues.DOUBLE_ONE,  AtomicBooleanConversions.toDouble(trueAtomic(),  converter));
        assertEquals(CommonValues.DOUBLE_ZERO, AtomicBooleanConversions.toDouble(falseAtomic(), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullSource() {
        AtomicBooleanConversions.toDouble(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_WrongSourceType() {
        AtomicBooleanConversions.toDouble(Boolean.FALSE, converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    public void testToBoolean() {
        assertTrue(AtomicBooleanConversions.toBoolean(trueAtomic(), converter));
        assertFalse(AtomicBooleanConversions.toBoolean(falseAtomic(), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToBoolean_NullSource() {
        AtomicBooleanConversions.toBoolean(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBoolean_WrongSourceType() {
        AtomicBooleanConversions.toBoolean(0, converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    public void testToAtomicBoolean() {
        AtomicBoolean src = trueAtomic();
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(src, converter);
        assertNotSame(src, result);                  // must be a new instance
        assertTrue(result.get());
    }

    @Test
    public void testToAtomicBoolean_False() {
        AtomicBoolean src = falseAtomic();
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(src, converter);
        assertNotSame(src, result);
        assertFalse(result.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBoolean_NullSource() {
        AtomicBooleanConversions.toAtomicBoolean(null, converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    public void testToAtomicInteger() {
        AtomicInteger resultTrue  = AtomicBooleanConversions.toAtomicInteger(trueAtomic(),  converter);
        AtomicInteger resultFalse = AtomicBooleanConversions.toAtomicInteger(falseAtomic(), converter);
        assertEquals(1, resultTrue.get());
        assertEquals(0, resultFalse.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_NullSource() {
        AtomicBooleanConversions.toAtomicInteger(null, converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    public void testToAtomicLong() {
        AtomicLong resultTrue  = AtomicBooleanConversions.toAtomicLong(trueAtomic(),  converter);
        AtomicLong resultFalse = AtomicBooleanConversions.toAtomicLong(falseAtomic(), converter);
        assertEquals(1L, resultTrue.get());
        assertEquals(0L, resultFalse.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullSource() {
        AtomicBooleanConversions.toAtomicLong(null, converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    public void testToCharacter_UsesConverterOptions() {
        // The default ConverterOptions define the characters used for true/false.
        // We simply verify that the conversion returns exactly those values.
        char expectedTrue  = options.trueChar();
        char expectedFalse = options.falseChar();

        assertEquals(expectedTrue,  AtomicBooleanConversions.toCharacter(trueAtomic(),  converter));
        assertEquals(expectedFalse, AtomicBooleanConversions.toCharacter(falseAtomic(), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacter_NullSource() {
        AtomicBooleanConversions.toCharacter(null, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacter_NullConverter() {
        AtomicBooleanConversions.toCharacter(trueAtomic(), null);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacter_WrongSourceType() {
        AtomicBooleanConversions.toCharacter(Boolean.TRUE, converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    public void testToBigDecimal() {
        assertEquals(BigDecimal.ONE,  AtomicBooleanConversions.toBigDecimal(trueAtomic(),  converter));
        assertEquals(BigDecimal.ZERO, AtomicBooleanConversions.toBigDecimal(falseAtomic(), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullSource() {
        AtomicBooleanConversions.toBigDecimal(null, converter);
    }

    /* --------------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        assertEquals(BigInteger.ONE,  AtomicBooleanConversions.toBigInteger(trueAtomic(),  converter));
        assertEquals(BigInteger.ZERO, AtomicBooleanConversions.toBigInteger(falseAtomic(), converter));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullSource() {
        AtomicBooleanConversions.toBigInteger(null, converter);
    }
}
