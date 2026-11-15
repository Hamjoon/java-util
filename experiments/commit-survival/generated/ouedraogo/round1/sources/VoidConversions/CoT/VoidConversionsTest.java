package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@code VoidConversions}.
 *
 * The class under test contains three package‑private static conversion methods.
 * The test class is placed in the same package so that it can access those methods.
 *
 * The tests are divided into two groups:
 *   1. Basic behaviour – verify that each method returns the constant value that
 *      the implementation specifies regardless of the input arguments.
 *   2. Edge‑case / robustness – pass {@code null} for the {@code from} argument,
 *      pass unusual objects, and pass {@code null} for the {@code Converter}
 *      argument to ensure that no {@link NullPointerException} is thrown and that
 *      the constant result is still returned.
 */
public class VoidConversionsTest {

    /** Helper to create a (real) {@link Converter} instance. */
    private Converter createConverter() {
        // The library’s {@code Converter} constructor only needs a {@code ConverterOptions}
        // instance; the tests do not depend on any option values, so we can safely pass null.
        return new Converter(null);
    }

    /* --------------------------------------------------------------
     *  1. BASIC FUNCTIONALITY TESTS
     * -------------------------------------------------------------- */

    @Test
    public void testToNullReturnsNull() {
        Converter conv = createConverter();
        Object result = VoidConversions.toNull("any input", conv);
        assertNull("toNull should always return null", result);
    }

    @Test
    public void testToBooleanReturnsFalse() {
        Converter conv = createConverter();
        Boolean result = VoidConversions.toBoolean(123, conv);
        assertEquals("toBoolean should always return Boolean.FALSE", Boolean.FALSE, result);
    }

    @Test
    public void testToCharacterReturnsMinValue() {
        Converter conv = createConverter();
        Character result = VoidConversions.toCharacter(new Object(), conv);
        assertEquals("toCharacter should always return Character.MIN_VALUE",
                Character.valueOf(Character.MIN_VALUE), result);
    }

    /* --------------------------------------------------------------
     *  2. EDGE CASE / EXCEPTION‑FREE TESTS
     * -------------------------------------------------------------- */

    @Test
    public void testToNullWithNullFromAndNullConverter() {
        // Both arguments are null – method must still return null without throwing.
        Object result = VoidConversions.toNull(null, null);
        assertNull("toNull should return null even when both arguments are null", result);
    }

    @Test
    public void testToBooleanWithNullFromAndNullConverter() {
        // Passing null for both parameters – expect Boolean.FALSE and no exception.
        Boolean result = VoidConversions.toBoolean(null, null);
        assertEquals("toBoolean should return Boolean.FALSE even when arguments are null",
                Boolean.FALSE, result);
    }

    @Test
    public void testToCharacterWithNullFromAndNullConverter() {
        // Null arguments – expect Character.MIN_VALUE.
        Character result = VoidConversions.toCharacter(null, null);
        assertEquals("toCharacter should return Character.MIN_VALUE even when arguments are null",
                Character.valueOf(Character.MIN_VALUE), result);
    }

    @Test
    public void testToBooleanWithVariousObjectTypes() {
        Converter conv = createConverter();

        // String input
        Boolean fromString = VoidConversions.toBoolean("true", conv);
        assertEquals(Boolean.FALSE, fromString);

        // Integer input
        Boolean fromInteger = VoidConversions.toBoolean(0, conv);
        assertEquals(Boolean.FALSE, fromInteger);

        // Custom object input
        Boolean fromCustom = VoidConversions.toBoolean(new Object(), conv);
        assertEquals(Boolean.FALSE, fromCustom);
    }

    @Test
    public void testToCharacterWithVariousObjectTypes() {
        Converter conv = createConverter();

        // String input
        Character fromString = VoidConversions.toCharacter("c", conv);
        assertEquals(Character.valueOf(Character.MIN_VALUE), fromString);

        // Integer input
        Character fromInteger = VoidConversions.toCharacter(65, conv);
        assertEquals(Character.valueOf(Character.MIN_VALUE), fromInteger);

        // Custom object input
        Character fromCustom = VoidConversions.toCharacter(new Object(), conv);
        assertEquals(Character.valueOf(Character.MIN_VALUE), fromCustom);
    }

    @Test
    public void testToNullIgnoresConverterImplementation() {
        // Create a Converter with a non‑null options object to prove that the method
        // does not inspect the Converter instance.
        Converter conv = new Converter(new ConverterOptions()); // assuming a public ctor
        Object result = VoidConversions.toNull("ignored", conv);
        assertNull(result);
    }
}
