package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link VoidConversions}.
 *
 * The class under test contains three static conversion methods that deliberately
 * ignore their inputs and return constant values:
 * <ul>
 *   <li>{@code toNull}      → {@code null}</li>
 *   <li>{@code toBoolean}   → {@code Boolean.FALSE}</li>
 *   <li>{@code toCharacter}→ {@code Character.MIN_VALUE}</li>
 * </ul>
 *
 * The tests cover:
 * <ol>
 *   <li>Typical usage with non‑null arguments.</li>
 *   <li>Edge cases where the {@code from} argument and/or the {@code Converter}
 *       argument are {@code null} – the methods must still return the constant
 *       values and must not throw {@link NullPointerException}.</li>
 *   <li>Verification that the returned objects are exactly the expected
 *       constants (reference equality for {@code Boolean.FALSE} and value
 *       equality for {@code Character.MIN_VALUE}).</li>
 * </ol>
 *
 * No exception handling is required because the implementation does not
 * dereference any arguments.
 */
public class VoidConversionsTest {

    /** Helper to obtain a {@link Converter} instance without needing any real configuration. */
    private static Converter getDummyConverter() {
        // {@link Converter} has a public constructor that accepts a ConverterOptions.
        // The options are not used by VoidConversions, so we can safely pass {@code null}.
        return new Converter(null);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for toNull(Object, Converter)                                   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToNull_WithNonNullArguments_ReturnsNull() {
        Object source = new Object();
        Converter converter = getDummyConverter();

        Object result = VoidConversions.toNull(source, converter);

        assertNull("toNull should always return null", result);
    }

    @Test
    public void testToNull_WithNullFromAndConverter_ReturnsNull() {
        Object result = VoidConversions.toNull(null, null);
        assertNull("toNull must return null even when both arguments are null", result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for toBoolean(Object, Converter)                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToBoolean_WithNonNullArguments_ReturnsFalse() {
        String source = "any value";
        Converter converter = getDummyConverter();

        Boolean result = VoidConversions.toBoolean(source, converter);

        assertNotNull("toBoolean must never return null", result);
        assertSame("toBoolean should return Boolean.FALSE constant", Boolean.FALSE, result);
    }

    @Test
    public void testToBoolean_WithNullFromAndConverter_ReturnsFalse() {
        Boolean result = VoidConversions.toBoolean(null, null);
        assertNotNull("toBoolean must never return null even with null arguments", result);
        assertSame("toBoolean should still return Boolean.FALSE when arguments are null", Boolean.FALSE, result);
    }

    /* ---------------------------------------------------------------------- */
    /*  Tests for toCharacter(Object, Converter)                               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToCharacter_WithNonNullArguments_ReturnsMinValue() {
        Integer source = 123;
        Converter converter = getDummyConverter();

        Character result = VoidConversions.toCharacter(source, converter);

        assertNotNull("toCharacter must never return null", result);
        assertEquals("toCharacter should return Character.MIN_VALUE", Character.MIN_VALUE, (char) result);
    }

    @Test
    public void testToCharacter_WithNullFromAndConverter_ReturnsMinValue() {
        Character result = VoidConversions.toCharacter(null, null);
        assertNotNull("toCharacter must never return null even when arguments are null", result);
        assertEquals("toCharacter should still return Character.MIN_VALUE with null arguments",
                Character.MIN_VALUE, (char) result);
    }
}
