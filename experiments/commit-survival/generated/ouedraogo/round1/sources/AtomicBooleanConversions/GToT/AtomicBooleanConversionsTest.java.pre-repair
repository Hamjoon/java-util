package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Test suite for {@code AtomicBooleanConversions}.
 *
 * <p>All public (package‑private static) conversion methods are exercised with:
 * <ul>
 *   <li>normal values (true / false)</li>
 *   <li>edge cases (null input, wrong type)</li>
 *   <li>exception scenarios (e.g. {@code toCharacter} when {@code ConverterOptions} is null)</li>
 * </ul>
 *
 * <p>Only the classes, constructors and methods that appear in the supplied source
 * or in the listed public API of {@link Converter} are used – no mocks or external
 * libraries are required.
 *
 * @author  OpenAI ChatGPT
 */
public class AtomicBooleanConversionsTest {

    /** A minimal {@link Converter} instance – the conversion methods ignore the
     *  {@code Converter} argument except for {@code toCharacter}, which will
     *  deliberately provoke a {@code NullPointerException} when its options are {@code null}. */
    private Converter dummyConverter;

    @Before
    public void setUp() {
        // The real Converter expects a ConverterOptions object; passing {@code null}
        // is allowed for the purposes of these tests because the conversion methods
        // (except toCharacter) never dereference the options.
        dummyConverter = new Converter(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  List of conversion methods (extracted from the source)                */
    /* ---------------------------------------------------------------------- */
    // static Byte   toByte(Object from, Converter converter)
    // static Short  toShort(Object from, Converter converter)
    // static Integer toInt(Object from, Converter converter)
    // static Long    toLong(Object from, Converter converter)
    // static Float   toFloat(Object from, Converter converter)
    // static Double  toDouble(Object from, Converter converter)
    // static boolean toBoolean(Object from, Converter converter)
    // static AtomicBoolean  toAtomicBoolean(Object from, Converter converter)
    // static AtomicInteger  toAtomicInteger(Object from, Converter converter)
    // static AtomicLong     toAtomicLong(Object from, Converter converter)
    // static Character     toCharacter(Object from, Converter converter)
    // static BigDecimal    toBigDecimal(Object from, Converter converter)
    // public static BigInteger toBigInteger(Object from, Converter converter)

    /* ---------------------------------------------------------------------- */
    /*  Basic functionality tests                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByte_true() {
        AtomicBoolean src = new AtomicBoolean(true);
        Byte result = AtomicBooleanConversions.toByte(src, dummyConverter);
        assertEquals(CommonValues.BYTE_ONE, result);
    }

    @Test
    public void testToByte_false() {
        AtomicBoolean src = new AtomicBoolean(false);
        Byte result = AtomicBooleanConversions.toByte(src, dummyConverter);
        assertEquals(CommonValues.BYTE_ZERO, result);
    }

    @Test
    public void testToShort_true() {
        AtomicBoolean src = new AtomicBoolean(true);
        Short result = AtomicBooleanConversions.toShort(src, dummyConverter);
        assertEquals(CommonValues.SHORT_ONE, result);
    }

    @Test
    public void testToShort_false() {
        AtomicBoolean src = new AtomicBoolean(false);
        Short result = AtomicBooleanConversions.toShort(src, dummyConverter);
        assertEquals(CommonValues.SHORT_ZERO, result);
    }

    @Test
    public void testToInt_true() {
        AtomicBoolean src = new AtomicBoolean(true);
        Integer result = AtomicBooleanConversions.toInt(src, dummyConverter);
        assertEquals(CommonValues.INTEGER_ONE, result);
    }

    @Test
    public void testToInt_false() {
        AtomicBoolean src = new AtomicBoolean(false);
        Integer result = AtomicBooleanConversions.toInt(src, dummyConverter);
        assertEquals(CommonValues.INTEGER_ZERO, result);
    }

    @Test
    public void testToLong_true() {
        AtomicBoolean src = new AtomicBoolean(true);
        Long result = AtomicBooleanConversions.toLong(src, dummyConverter);
        assertEquals(CommonValues.LONG_ONE, result);
    }

    @Test
    public void testToLong_false() {
        AtomicBoolean src = new AtomicBoolean(false);
        Long result = AtomicBooleanConversions.toLong(src, dummyConverter);
        assertEquals(CommonValues.LONG_ZERO, result);
    }

    @Test
    public void testToFloat_true() {
        AtomicBoolean src = new AtomicBoolean(true);
        Float result = AtomicBooleanConversions.toFloat(src, dummyConverter);
        assertEquals(CommonValues.FLOAT_ONE, result);
    }

    @Test
    public void testToFloat_false() {
        AtomicBoolean src = new AtomicBoolean(false);
        Float result = AtomicBooleanConversions.toFloat(src, dummyConverter);
        assertEquals(CommonValues.FLOAT_ZERO, result);
    }

    @Test
    public void testToDouble_true() {
        AtomicBoolean src = new AtomicBoolean(true);
        Double result = AtomicBooleanConversions.toDouble(src, dummyConverter);
        assertEquals(CommonValues.DOUBLE_ONE, result);
    }

    @Test
    public void testToDouble_false() {
        AtomicBoolean src = new AtomicBoolean(false);
        Double result = AtomicBooleanConversions.toDouble(src, dummyConverter);
        assertEquals(CommonValues.DOUBLE_ZERO, result);
    }

    @Test
    public void testToBoolean_true() {
        AtomicBoolean src = new AtomicBoolean(true);
        boolean result = AtomicBooleanConversions.toBoolean(src, dummyConverter);
        assertTrue(result);
    }

    @Test
    public void testToBoolean_false() {
        AtomicBoolean src = new AtomicBoolean(false);
        boolean result = AtomicBooleanConversions.toBoolean(src, dummyConverter);
        assertFalse(result);
    }

    @Test
    public void testToAtomicBoolean_true() {
        AtomicBoolean src = new AtomicBoolean(true);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(src, dummyConverter);
        assertNotSame(src, result);
        assertTrue(result.get());
    }

    @Test
    public void testToAtomicBoolean_false() {
        AtomicBoolean src = new AtomicBoolean(false);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(src, dummyConverter);
        assertNotSame(src, result);
        assertFalse(result.get());
    }

    @Test
    public void testToAtomicInteger_true() {
        AtomicBoolean src = new AtomicBoolean(true);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(src, dummyConverter);
        assertEquals(1, result.get());
    }

    @Test
    public void testToAtomicInteger_false() {
        AtomicBoolean src = new AtomicBoolean(false);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(src, dummyConverter);
        assertEquals(0, result.get());
    }

    @Test
    public void testToAtomicLong_true() {
        AtomicBoolean src = new AtomicBoolean(true);
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(src, dummyConverter);
        assertEquals(1L, result.get());
    }

    @Test
    public void testToAtomicLong_false() {
        AtomicBoolean src = new AtomicBoolean(false);
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(src, dummyConverter);
        assertEquals(0L, result.get());
    }

    @Test
    public void testToBigDecimal_true() {
        AtomicBoolean src = new AtomicBoolean(true);
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(src, dummyConverter);
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    public void testToBigDecimal_false() {
        AtomicBoolean src = new AtomicBoolean(false);
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(src, dummyConverter);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testToBigInteger_true() {
        AtomicBoolean src = new AtomicBoolean(true);
        BigInteger result = AtomicBooleanConversions.toBigInteger(src, dummyConverter);
        assertEquals(BigInteger.ONE, result);
    }

    @Test
    public void testToBigInteger_false() {
        AtomicBoolean src = new AtomicBoolean(false);
        BigInteger result = AtomicBooleanConversions.toBigInteger(src, dummyConverter);
        assertEquals(BigInteger.ZERO, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge‑case & exception tests                                          */
    /* ---------------------------------------------------------------------- */

    /** All conversion methods (except {@code toCharacter}) cast the {@code from}
     *  argument to {@code AtomicBoolean}. Passing {@code null} therefore results in
     *  a {@code NullPointerException} (the cast succeeds, but the subsequent
     *  {@code get()} call dereferences {@code null}). */
    @Test(expected = NullPointerException.class)
    public void testToInt_nullSource() {
        AtomicBooleanConversions.toInt(null, dummyConverter);
    }

    /** Passing an object of an incompatible type should cause a {@code ClassCastException}. */
    @Test(expected = ClassCastException.class)
    public void testToLong_wrongType() {
        AtomicBooleanConversions.toLong("not an AtomicBoolean", dummyConverter);
    }

    /** {@code toCharacter} relies on {@link ConverterOptions}. With a {@code Converter}
     *  constructed with {@code null} options, a {@code NullPointerException} is expected. */
    @Test(expected = NullPointerException.class)
    public void testToCharacter_nullOptions() {
        AtomicBoolean src = new AtomicBoolean(true);
        // This will invoke: converter.getOptions().trueChar()
        AtomicBooleanConversions.toCharacter(src, dummyConverter);
    }

    /** Verify that {@code toAtomicBoolean} returns a *new* instance – mutating the
     *  result must not affect the source. */
    @Test
    public void testToAtomicBoolean_isCopy() {
        AtomicBoolean src = new AtomicBoolean(true);
        AtomicBoolean copy = AtomicBooleanConversions.toAtomicBoolean(src, dummyConverter);
        copy.set(false);
        assertTrue(src.get());   // source unchanged
        assertFalse(copy.get()); // copy changed
    }

    /** Ensure that {@code toAtomicInteger} returns distinct objects for true and false. */
    @Test
    public void testToAtomicInteger_distinctInstances() {
        AtomicBoolean trueSrc = new AtomicBoolean(true);
        AtomicBoolean falseSrc = new AtomicBoolean(false);
        AtomicInteger aiTrue = AtomicBooleanConversions.toAtomicInteger(trueSrc, dummyConverter);
        AtomicInteger aiFalse = AtomicBooleanConversions.toAtomicInteger(falseSrc, dummyConverter);
        assertNotEquals(aiTrue.get(), aiFalse.get());
        assertEquals(1, aiTrue.get());
        assertEquals(0, aiFalse.get());
    }

    /** Ensure that {@code toAtomicLong} returns distinct objects for true and false. */
    @Test
    public void testToAtomicLong_distinctInstances() {
        AtomicBoolean trueSrc = new AtomicBoolean(true);
        AtomicBoolean falseSrc = new AtomicBoolean(false);
        AtomicLong alTrue = AtomicBooleanConversions.toAtomicLong(trueSrc, dummyConverter);
        AtomicLong alFalse = AtomicBooleanConversions.toAtomicLong(falseSrc, dummyConverter);
        assertNotEquals(alTrue.get(), alFalse.get());
        assertEquals(1L, alTrue.get());
        assertEquals(0L, alFalse.get());
    }
}
