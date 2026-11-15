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
 * JUnit 4 test suite for {@link AtomicBooleanConversions}.
 *
 * The test class resides in the same package as {@code AtomicBooleanConversions}
 * so it can access the package‑private static conversion methods.
 */
public class AtomicBooleanConversionsTest {

    private Converter defaultConverter;
    private Converter customConverter;
    private ConverterOptions customOptions;

    @Before
    public void setUp() {
        // The library provides a public constructor for Converter that accepts a ConverterOptions instance.
        // We assume that ConverterOptions has a public no‑arg constructor and allows us to set custom true/false characters.
        // If the real library uses a builder or different API, these lines would need to be adapted accordingly.
        defaultConverter = new Converter(new ConverterOptions());

        // Create a ConverterOptions instance with distinct true/false characters to verify toCharacter behaviour.
        customOptions = new ConverterOptions();
        // Assuming the options expose setters (or are mutable) – the exact API is not shown, so we use reflection as a fallback.
        try {
            java.lang.reflect.Method setTrueChar = ConverterOptions.class.getMethod("setTrueChar", char.class);
            java.lang.reflect.Method setFalseChar = ConverterOptions.class.getMethod("setFalseChar", char.class);
            setTrueChar.invoke(customOptions, 'Y');
            setFalseChar.invoke(customOptions, 'N');
        } catch (Exception ignored) {
            // If the real API does not expose setters, the test will fall back to the default characters.
        }
        customConverter = new Converter(customOptions);
    }

    /* --------------------------------------------------------------------- */
    /*  Basic functionality tests – each conversion method for true/false    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testToByte_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        Byte result = AtomicBooleanConversions.toByte(source, defaultConverter);
        assertEquals(CommonValues.BYTE_ONE, result);
    }

    @Test
    public void testToByte_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        Byte result = AtomicBooleanConversions.toByte(source, defaultConverter);
        assertEquals(CommonValues.BYTE_ZERO, result);
    }

    @Test
    public void testToShort_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        Short result = AtomicBooleanConversions.toShort(source, defaultConverter);
        assertEquals(CommonValues.SHORT_ONE, result);
    }

    @Test
    public void testToShort_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        Short result = AtomicBooleanConversions.toShort(source, defaultConverter);
        assertEquals(CommonValues.SHORT_ZERO, result);
    }

    @Test
    public void testToInt_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        Integer result = AtomicBooleanConversions.toInt(source, defaultConverter);
        assertEquals(CommonValues.INTEGER_ONE, result);
    }

    @Test
    public void testToInt_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        Integer result = AtomicBooleanConversions.toInt(source, defaultConverter);
        assertEquals(CommonValues.INTEGER_ZERO, result);
    }

    @Test
    public void testToLong_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        Long result = AtomicBooleanConversions.toLong(source, defaultConverter);
        assertEquals(CommonValues.LONG_ONE, result);
    }

    @Test
    public void testToLong_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        Long result = AtomicBooleanConversions.toLong(source, defaultConverter);
        assertEquals(CommonValues.LONG_ZERO, result);
    }

    @Test
    public void testToFloat_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        Float result = AtomicBooleanConversions.toFloat(source, defaultConverter);
        assertEquals(CommonValues.FLOAT_ONE, result);
    }

    @Test
    public void testToFloat_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        Float result = AtomicBooleanConversions.toFloat(source, defaultConverter);
        assertEquals(CommonValues.FLOAT_ZERO, result);
    }

    @Test
    public void testToDouble_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        Double result = AtomicBooleanConversions.toDouble(source, defaultConverter);
        assertEquals(CommonValues.DOUBLE_ONE, result);
    }

    @Test
    public void testToDouble_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        Double result = AtomicBooleanConversions.toDouble(source, defaultConverter);
        assertEquals(CommonValues.DOUBLE_ZERO, result);
    }

    @Test
    public void testToBoolean_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        boolean result = AtomicBooleanConversions.toBoolean(source, defaultConverter);
        assertTrue(result);
    }

    @Test
    public void testToBoolean_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        boolean result = AtomicBooleanConversions.toBoolean(source, defaultConverter);
        assertFalse(result);
    }

    @Test
    public void testToAtomicBoolean_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, defaultConverter);
        assertNotSame(source, result);               // must be a new instance
        assertTrue(result.get());
    }

    @Test
    public void testToAtomicBoolean_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, defaultConverter);
        assertNotSame(source, result);
        assertFalse(result.get());
    }

    @Test
    public void testToAtomicInteger_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, defaultConverter);
        assertEquals(1, result.get());
    }

    @Test
    public void testToAtomicInteger_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, defaultConverter);
        assertEquals(0, result.get());
    }

    @Test
    public void testToAtomicLong_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, defaultConverter);
        assertEquals(1L, result.get());
    }

    @Test
    public void testToAtomicLong_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, defaultConverter);
        assertEquals(0L, result.get());
    }

    @Test
    public void testToCharacter_true_defaultOptions() {
        // Using the default ConverterOptions – we cannot guarantee the exact chars,
        // but we can assert that the returned char equals the option's trueChar().
        AtomicBoolean source = new AtomicBoolean(true);
        char expected = defaultConverter.getOptions().trueChar();
        char result = AtomicBooleanConversions.toCharacter(source, defaultConverter);
        assertEquals(expected, result);
    }

    @Test
    public void testToCharacter_false_defaultOptions() {
        AtomicBoolean source = new AtomicBoolean(false);
        char expected = defaultConverter.getOptions().falseChar();
        char result = AtomicBooleanConversions.toCharacter(source, defaultConverter);
        assertEquals(expected, result);
    }

    @Test
    public void testToCharacter_true_customOptions() {
        AtomicBoolean source = new AtomicBoolean(true);
        char expected = customConverter.getOptions().trueChar(); // should be 'Y' if setters worked
        char result = AtomicBooleanConversions.toCharacter(source, customConverter);
        assertEquals(expected, result);
    }

    @Test
    public void testToCharacter_false_customOptions() {
        AtomicBoolean source = new AtomicBoolean(false);
        char expected = customConverter.getOptions().falseChar(); // should be 'N' if setters worked
        char result = AtomicBooleanConversions.toCharacter(source, customConverter);
        assertEquals(expected, result);
    }

    @Test
    public void testToBigDecimal_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, defaultConverter);
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    public void testToBigDecimal_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, defaultConverter);
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    public void testToBigInteger_true() {
        AtomicBoolean source = new AtomicBoolean(true);
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, defaultConverter);
        assertEquals(BigInteger.ONE, result);
    }

    @Test
    public void testToBigInteger_false() {
        AtomicBoolean source = new AtomicBoolean(false);
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, defaultConverter);
        assertEquals(BigInteger.ZERO, result);
    }

    /* --------------------------------------------------------------------- */
    /*  Edge‑case / exception handling tests                                 */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToByte_nullSource() {
        AtomicBooleanConversions.toByte(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToByte_wrongType() {
        AtomicInteger notABoolean = new AtomicInteger(1);
        AtomicBooleanConversions.toByte(notABoolean, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToShort_nullSource() {
        AtomicBooleanConversions.toShort(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToShort_wrongType() {
        AtomicLong notABoolean = new AtomicLong(1L);
        AtomicBooleanConversions.toShort(notABoolean, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToInt_nullSource() {
        AtomicBooleanConversions.toInt(null, defaultConverter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInt_wrongType() {
        AtomicBoolean notABoolean = new AtomicBoolean(true);
        // Cast to Object that is not AtomicBoolean (simulate misuse)
        Object bogus = new Object();
        AtomicBooleanConversions.toInt(bogus, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_nullSource() {
        AtomicBooleanConversions.toLong(null, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToFloat_nullSource() {
        AtomicBooleanConversions.toFloat(null, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_nullSource() {
        AtomicBooleanConversions.toDouble(null, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBoolean_nullSource() {
        AtomicBooleanConversions.toBoolean(null, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicBoolean_nullSource() {
        AtomicBooleanConversions.toAtomicBoolean(null, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicInteger_nullSource() {
        AtomicBooleanConversions.toAtomicInteger(null, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_nullSource() {
        AtomicBooleanConversions.toAtomicLong(null, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacter_nullSource() {
        AtomicBooleanConversions.toCharacter(null, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_nullSource() {
        AtomicBooleanConversions.toBigDecimal(null, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_nullSource() {
        AtomicBooleanConversions.toBigInteger(null, defaultConverter);
    }

    @Test
    public void testToAtomicBoolean_isIndependent() {
        AtomicBoolean original = new AtomicBoolean(true);
        AtomicBoolean copy = AtomicBooleanConversions.toAtomicBoolean(original, defaultConverter);
        // Change original – copy must stay unchanged
        original.set(false);
        assertTrue("Copy should retain original true value", copy.get());
        assertFalse("Original was mutated", original.get());
    }

    @Test
    public void testToAtomicInteger_isIndependent() {
        AtomicBoolean original = new AtomicBoolean(true);
        AtomicInteger copy = AtomicBooleanConversions.toAtomicInteger(original, defaultConverter);
        original.set(false);
        assertEquals(1, copy.get()); // value must not change after original mutates
    }

    @Test
    public void testToAtomicLong_isIndependent() {
        AtomicBoolean original = new AtomicBoolean(false);
        AtomicLong copy = AtomicBooleanConversions.toAtomicLong(original, defaultConverter);
        original.set(true);
        assertEquals(0L, copy.get()); // value must stay as initialized
    }
}
