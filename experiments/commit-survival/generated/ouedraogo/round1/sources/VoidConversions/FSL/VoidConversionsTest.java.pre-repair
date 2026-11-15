package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link VoidConversions}.
 *
 * The methods in {@code VoidConversions} deliberately ignore their inputs
 * and always return a constant value:
 * <ul>
 *   <li>{@code toNull}    → {@code null}</li>
 *   <li>{@code toBoolean} → {@code Boolean.FALSE}</li>
 *   <li>{@code toCharacter} → {@code Character.MIN_VALUE}</li>
 * </ul>
 *
 * The tests verify that this contract holds for a variety of
 * {@code from} arguments (including {@code null}) and for both a
 * {@code null} {@link Converter} reference and a real {@link Converter}
 * instance created with default {@link ConverterOptions}.
 */
public class VoidConversionsTest {

    /** Helper to create a default {@link Converter} instance. */
    private static Converter defaultConverter() {
        // The library provides a public constructor that accepts a ConverterOptions.
        // ConverterOptions has a public no‑arg constructor in the original library,
        // so we can safely instantiate it here.
        return new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------------- *
     * toNull()
     * --------------------------------------------------------------------- */

    @Test
    public void toNull_ShouldAlwaysReturnNull_WithNullConverter() {
        assertNull(VoidConversions.toNull(null, null));
        assertNull(VoidConversions.toNull("any string", null));
        assertNull(VoidConversions.toNull(12345, null));
        assertNull(VoidConversions.toNull(new Object(), null));
    }

    @Test
    public void toNull_ShouldAlwaysReturnNull_WithRealConverter() {
        Converter converter = defaultConverter();

        assertNull(VoidConversions.toNull(null, converter));
        assertNull(VoidConversions.toNull("another string", converter));
        assertNull(VoidConversions.toNull(Boolean.TRUE, converter));
        assertNull(VoidConversions.toNull(new Object(), converter));
    }

    /* --------------------------------------------------------------------- *
     * toBoolean()
     * --------------------------------------------------------------------- */

    @Test
    public void toBoolean_ShouldAlwaysReturnFalse_WithNullConverter() {
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean(null, null));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean("true", null));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean(Boolean.TRUE, null));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean(0, null));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean(new Object(), null));
    }

    @Test
    public void toBoolean_ShouldAlwaysReturnFalse_WithRealConverter() {
        Converter converter = defaultConverter();

        assertEquals(Boolean.FALSE, VoidConversions.toBoolean(null, converter));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean("false", converter));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean(Boolean.FALSE, converter));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean(42, converter));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean(new Object(), converter));
    }

    @Test
    public void toBoolean_ReturnedInstanceIsTheCanonicalFalse() {
        Boolean result = VoidConversions.toBoolean("anything", defaultConverter());
        assertSame(Boolean.FALSE, result);
    }

    /* --------------------------------------------------------------------- *
     * toCharacter()
     * --------------------------------------------------------------------- */

    @Test
    public void toCharacter_ShouldAlwaysReturnMinValue_WithNullConverter() {
        assertEquals(Character.valueOf(Character.MIN_VALUE), VoidConversions.toCharacter(null, null));
        assertEquals(Character.valueOf(Character.MIN_VALUE), VoidConversions.toCharacter("x", null));
        assertEquals(Character.valueOf(Character.MIN_VALUE), VoidConversions.toCharacter(123, null));
        assertEquals(Character.valueOf(Character.MIN_VALUE), VoidConversions.toCharacter(new Object(), null));
    }

    @Test
    public void toCharacter_ShouldAlwaysReturnMinValue_WithRealConverter() {
        Converter converter = defaultConverter();

        assertEquals(Character.valueOf(Character.MIN_VALUE), VoidConversions.toCharacter(null, converter));
        assertEquals(Character.valueOf(Character.MIN_VALUE), VoidConversions.toCharacter("a", converter));
        assertEquals(Character.valueOf(Character.MIN_VALUE), VoidConversions.toCharacter(Boolean.TRUE, converter));
        assertEquals(Character.valueOf(Character.MIN_VALUE), VoidConversions.toCharacter(new Object(), converter));
    }

    @Test
    public void toCharacter_ReturnedValueEqualsCharMinValue() {
        Character result = VoidConversions.toCharacter("anything", defaultConverter());
        assertEquals(Character.MIN_VALUE, (char) result);
    }
}
