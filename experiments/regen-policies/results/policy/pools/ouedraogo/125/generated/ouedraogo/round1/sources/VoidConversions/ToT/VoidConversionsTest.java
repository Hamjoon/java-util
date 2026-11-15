package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link VoidConversions}.
 *
 * The methods in {@code VoidConversions} deliberately ignore their input parameters
 * and always return a constant value:
 * <ul>
 *   <li>{@code toNull}    → {@code null}</li>
 *   <li>{@code toBoolean} → {@code Boolean.FALSE}</li>
 *   <li>{@code toCharacter} → {@code Character.MIN_VALUE}</li>
 * </ul>
 *
 * The test suite therefore focuses on:
 * <ul>
 *   <li>Typical use – passing ordinary objects.</li>
 *   <li>Edge cases – passing {@code null} as the source object.</li>
 *   <li>Error‑scenario – passing {@code null} as the {@link Converter} instance.</li>
 *   <li>Verification that the exact constant objects are returned (identity checks).</li>
 * </ul>
 *
 * No mocking frameworks are used; real {@link Converter} instances are created
 * with a {@code null} {@link ConverterOptions} argument because the API permits
 * it (the conversion methods never dereference the options).
 */
public class VoidConversionsTest {

    /**
     * Helper that creates a {@link Converter} suitable for the tests.
     * The library allows a {@code null} {@code ConverterOptions} argument;
     * the resulting instance is sufficient because {@code VoidConversions}
     * never calls any method on the converter.
     */
    private static Converter createConverter() {
        return new Converter(null);
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for toNull                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void toNull_ReturnsNull_ForVariousInputs() {
        Converter converter = createConverter();

        assertNull("toNull should return null for a non‑null source object",
                VoidConversions.toNull("some string", converter));

        assertNull("toNull should return null for an Integer source object",
                VoidConversions.toNull(123, converter));

        assertNull("toNull should return null for a null source object",
                VoidConversions.toNull(null, converter));
    }

    @Test
    public void toNull_ReturnsNull_WhenConverterIsNull() {
        // The method does not use the converter, so a null value is acceptable.
        assertNull("toNull should return null even when the converter argument is null",
                VoidConversions.toNull("anything", null));
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for toBoolean                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void toBoolean_AlwaysReturnsBooleanFalse() {
        Converter converter = createConverter();

        // Typical non‑null objects
        assertSame("toBoolean must always return Boolean.FALSE (String input)",
                Boolean.FALSE, VoidConversions.toBoolean("true", converter));

        assertSame("toBoolean must always return Boolean.FALSE (Integer input)",
                Boolean.FALSE, VoidConversions.toBoolean(1, converter));

        // Null source object
        assertSame("toBoolean must always return Boolean.FALSE (null source)",
                Boolean.FALSE, VoidConversions.toBoolean(null, converter));
    }

    @Test
    public void toBoolean_ReturnsFalse_WhenConverterIsNull() {
        // The method does not dereference the converter.
        assertSame("toBoolean must return Boolean.FALSE even if converter is null",
                Boolean.FALSE, VoidConversions.toBoolean(Boolean.TRUE, null));
    }

    /* --------------------------------------------------------------------- */
    /*  Tests for toCharacter                                               */
    /* --------------------------------------------------------------------- */

    @Test
    public void toCharacter_AlwaysReturnsMinValue() {
        Converter converter = createConverter();

        // Various source objects
        assertEquals("toCharacter must always return Character.MIN_VALUE (String input)",
                Character.MIN_VALUE, VoidConversions.toCharacter("a", converter).charValue());

        assertEquals("toCharacter must always return Character.MIN_VALUE (Integer input)",
                Character.MIN_VALUE, VoidConversions.toCharacter(42, converter).charValue());

        // Null source object
        assertEquals("toCharacter must always return Character.MIN_VALUE (null source)",
                Character.MIN_VALUE, VoidConversions.toCharacter(null, converter).charValue());
    }

    @Test
    public void toCharacter_ReturnsMinValue_WhenConverterIsNull() {
        // The conversion does not depend on the converter.
        assertEquals("toCharacter must return Character.MIN_VALUE even if converter is null",
                Character.MIN_VALUE, VoidConversions.toCharacter('x', null).charValue());
    }
}
