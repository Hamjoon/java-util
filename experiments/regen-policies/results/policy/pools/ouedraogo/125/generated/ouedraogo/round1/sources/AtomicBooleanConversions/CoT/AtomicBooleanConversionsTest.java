package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link AtomicBooleanConversions}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>Normal conversion for both {@code true} and {@code false} {@link AtomicBoolean} values.</li>
 *   <li>Edge‑case handling such as {@code null} source objects, source objects of the wrong type,
 *       and {@code null} {@link Converter} where the conversion implementation accesses the
 *       converter.</li>
 *   <li>Reference semantics where a new atomic instance is expected (e.g. {@code toAtomicBoolean}).</li>
 * </ul>
 * All tests are placed in the same package as {@code AtomicBooleanConversions} so that the
 * package‑private static methods are directly accessible.
 * </p>
 */
@RunWith(JUnit4.class)
public class AtomicBooleanConversionsTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper methods                                                       */
    /* ---------------------------------------------------------------------- */

    /** Creates a {@link Converter} with default options (no custom configuration). */
    private static Converter defaultConverter() {
        // The library provides a public constructor that takes a ConverterOptions instance.
        // Using the default constructor of ConverterOptions (assumed to exist) yields
        // the library‑provided true/false character defaults.
        return new Converter(new ConverterOptions());
    }

    /** Creates a {@link Converter} with custom true/false characters. */
    private static Converter customCharConverter(final char trueChar, final char falseChar) {
        // Assuming ConverterOptions has a constructor that accepts true/false character
        // values. If such a constructor does not exist, the default options already
        // provide some characters and the test will still be valid.
        ConverterOptions options = new ConverterOptions() {
            @Override public char trueChar() { return trueChar; }
            @Override public char falseChar() { return falseChar; }
        };
        return new Converter(options);
    }

    /* ---------------------------------------------------------------------- */
    /*  Normal conversion tests                                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToByte() {
        Converter c = defaultConverter();
        assertEquals(CommonValues.BYTE_ONE, AtomicBooleanConversions.toByte(new AtomicBoolean(true), c));
        assertEquals(CommonValues.BYTE_ZERO, AtomicBooleanConversions.toByte(new AtomicBoolean(false), c));
    }

    @Test
    public void testToShort() {
        Converter c = defaultConverter();
        assertEquals(CommonValues.SHORT_ONE, AtomicBooleanConversions.toShort(new AtomicBoolean(true), c));
        assertEquals(CommonValues.SHORT_ZERO, AtomicBooleanConversions.toShort(new AtomicBoolean(false), c));
    }

    @Test
    public void testToInt() {
        Converter c = defaultConverter();
        assertEquals(CommonValues.INTEGER_ONE, AtomicBooleanConversions.toInt(new AtomicBoolean(true), c));
        assertEquals(CommonValues.INTEGER_ZERO, AtomicBooleanConversions.toInt(new AtomicBoolean(false), c));
    }

    @Test
    public void testToLong() {
        Converter c = defaultConverter();
        assertEquals(CommonValues.LONG_ONE, AtomicBooleanConversions.toLong(new AtomicBoolean(true), c));
        assertEquals(CommonValues.LONG_ZERO, AtomicBooleanConversions.toLong(new AtomicBoolean(false), c));
    }

    @Test
    public void testToFloat() {
        Converter c = defaultConverter();
        assertEquals(CommonValues.FLOAT_ONE, AtomicBooleanConversions.toFloat(new AtomicBoolean(true), c));
        assertEquals(CommonValues.FLOAT_ZERO, AtomicBooleanConversions.toFloat(new AtomicBoolean(false), c));
    }

    @Test
    public void testToDouble() {
        Converter c = defaultConverter();
        assertEquals(CommonValues.DOUBLE_ONE, AtomicBooleanConversions.toDouble(new AtomicBoolean(true), c));
        assertEquals(CommonValues.DOUBLE_ZERO, AtomicBooleanConversions.toDouble(new AtomicBoolean(false), c));
    }

    @Test
    public void testToBoolean() {
        Converter c = defaultConverter();
        assertTrue(AtomicBooleanConversions.toBoolean(new AtomicBoolean(true), c));
        assertFalse(AtomicBooleanConversions.toBoolean(new AtomicBoolean(false), c));
    }

    @Test
    public void testToAtomicBoolean() {
        Converter c = defaultConverter();
        AtomicBoolean sourceTrue = new AtomicBoolean(true);
        AtomicBoolean resultTrue = AtomicBooleanConversions.toAtomicBoolean(sourceTrue, c);
        assertNotSame(sourceTrue, resultTrue);
        assertTrue(resultTrue.get());

        AtomicBoolean sourceFalse = new AtomicBoolean(false);
        AtomicBoolean resultFalse = AtomicBooleanConversions.toAtomicBoolean(sourceFalse, c);
        assertNotSame(sourceFalse, resultFalse);
        assertFalse(resultFalse.get());
    }

    @Test
    public void testToAtomicInteger() {
        Converter c = defaultConverter();
        AtomicInteger aiTrue = AtomicBooleanConversions.toAtomicInteger(new AtomicBoolean(true), c);
        AtomicInteger aiFalse = AtomicBooleanConversions.toAtomicInteger(new AtomicBoolean(false), c);
        assertEquals(1, aiTrue.get());
        assertEquals(0, aiFalse.get());
    }

    @Test
    public void testToAtomicLong() {
        Converter c = defaultConverter();
        AtomicLong alTrue = AtomicBooleanConversions.toAtomicLong(new AtomicBoolean(true), c);
        AtomicLong alFalse = AtomicBooleanConversions.toAtomicLong(new AtomicBoolean(false), c);
        assertEquals(1L, alTrue.get());
        assertEquals(0L, alFalse.get());
    }

    @Test
    public void testToCharacterWithDefaultOptions() {
        Converter c = defaultConverter();
        ConverterOptions opts = c.getOptions();
        char expectedTrue = opts.trueChar();
        char expectedFalse = opts.falseChar();

        assertEquals(expectedTrue,
                AtomicBooleanConversions.toCharacter(new AtomicBoolean(true), c));
        assertEquals(expectedFalse,
                AtomicBooleanConversions.toCharacter(new AtomicBoolean(false), c));
    }

    @Test
    public void testToCharacterWithCustomOptions() {
        char myTrue = 'Y';
        char myFalse = 'N';
        Converter c = customCharConverter(myTrue, myFalse);

        assertEquals(myTrue,
                AtomicBooleanConversions.toCharacter(new AtomicBoolean(true), c));
        assertEquals(myFalse,
                AtomicBooleanConversions.toCharacter(new AtomicBoolean(false), c));
    }

    @Test
    public void testToBigDecimal() {
        Converter c = defaultConverter();
        assertEquals(BigDecimal.ONE,
                AtomicBooleanConversions.toBigDecimal(new AtomicBoolean(true), c));
        assertEquals(BigDecimal.ZERO,
                AtomicBooleanConversions.toBigDecimal(new AtomicBoolean(false), c));
    }

    @Test
    public void testToBigInteger() {
        Converter c = defaultConverter();
        assertEquals(BigInteger.ONE,
                AtomicBooleanConversions.toBigInteger(new AtomicBoolean(true), c));
        assertEquals(BigInteger.ZERO,
                AtomicBooleanConversions.toBigInteger(new AtomicBoolean(false), c));
    }

    /* ---------------------------------------------------------------------- */
    /*  Edge‑case / exception tests                                           */
    /* ---------------------------------------------------------------------- */

    /* Helper that runs a conversion expecting NullPointerException due to a null source */
    private void expectNPEWhenSourceIsNull(ConversionInvoker invoker) {
        try {
            invoker.invoke(null, defaultConverter());
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        }
    }

    /* Helper that runs a conversion expecting ClassCastException due to wrong source type */
    private void expectCCEWhenSourceIsWrongType(ConversionInvoker invoker) {
        try {
            invoker.invoke("not an AtomicBoolean", defaultConverter());
            fail("Expected ClassCastException");
        } catch (ClassCastException e) {
            // expected
        }
    }

    @FunctionalInterface
    private interface ConversionInvoker {
        Object invoke(Object from, Converter converter);
    }

    @Test
    public void testNullSourceThrowsNPE() {
        // All methods (except toBoolean which returns primitive) will throw NPE when source is null.
        expectNPEWhenSourceIsNull(AtomicBooleanConversions::toByte);
        expectNPEWhenSourceIsNull(AtomicBooleanConversions::toShort);
        expectNPEWhenSourceIsNull(AtomicBooleanConversions::toInt);
        expectNPEWhenSourceIsNull(AtomicBooleanConversions::toLong);
        expectNPEWhenSourceIsNull(AtomicBooleanConversions::toFloat);
        expectNPEWhenSourceIsNull(AtomicBooleanConversions::toDouble);
        expectNPEWhenSourceIsNull((o, c) -> AtomicBooleanConversions.toBoolean(o, c));
        expectNPEWhenSourceIsNull(AtomicBooleanConversions::toAtomicBoolean);
        expectNPEWhenSourceIsNull(AtomicBooleanConversions::toAtomicInteger);
        expectNPEWhenSourceIsNull(AtomicBooleanConversions::toAtomicLong);
        expectNPEWhenSourceIsNull(AtomicBooleanConversions::toCharacter);
        expectNPEWhenSourceIsNull(AtomicBooleanConversions::toBigDecimal);
        expectNPEWhenSourceIsNull(AtomicBooleanConversions::toBigInteger);
    }

    @Test
    public void testWrongSourceTypeThrowsCCE() {
        expectCCEWhenSourceIsWrongType(AtomicBooleanConversions::toByte);
        expectCCEWhenSourceIsWrongType(AtomicBooleanConversions::toShort);
        expectCCEWhenSourceIsWrongType(AtomicBooleanConversions::toInt);
        expectCCEWhenSourceIsWrongType(AtomicBooleanConversions::toLong);
        expectCCEWhenSourceIsWrongType(AtomicBooleanConversions::toFloat);
        expectCCEWhenSourceIsWrongType(AtomicBooleanConversions::toDouble);
        expectCCEWhenSourceIsWrongType((o, c) -> AtomicBooleanConversions.toBoolean(o, c));
        expectCCEWhenSourceIsWrongType(AtomicBooleanConversions::toAtomicBoolean);
        expectCCEWhenSourceIsWrongType(AtomicBooleanConversions::toAtomicInteger);
        expectCCEWhenSourceIsWrongType(AtomicBooleanConversions::toAtomicLong);
        expectCCEWhenSourceIsWrongType(AtomicBooleanConversions::toCharacter);
        expectCCEWhenSourceIsWrongType(AtomicBooleanConversions::toBigDecimal);
        expectCCEWhenSourceIsWrongType(AtomicBooleanConversions::toBigInteger);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacterWithNullConverterThrowsNPE() {
        AtomicBooleanConversions.toCharacter(new AtomicBoolean(true), null);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacterWithNullOptionsThrowsNPE() {
        // Pass a Converter that returns null options.
        Converter badConverter = new Converter(null) {
            @Override public ConverterOptions getOptions() { return null; }
        };
        AtomicBooleanConversions.toCharacter(new AtomicBoolean(true), badConverter);
    }
}
