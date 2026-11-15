package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for {@link VoidConversions}.
 * All methods return constant values and do not depend on the {@link Converter}
 * argument, so the tests verify that the returned values are exactly those
 * constants for a variety of input objects, including {@code null}.
 */
public class VoidConversionsTest {

    @Test
    public void testToNullAlwaysReturnsNull() {
        // Using null as the converter (allowed because the method does not use it)
        assertNull(VoidConversions.toNull(null, null));
        assertNull(VoidConversions.toNull("", null));
        assertNull(VoidConversions.toNull(123, null));
        assertNull(VoidConversions.toNull(new Object(), null));
    }

    @Test
    public void testToBooleanAlwaysReturnsFalse() {
        // The method should always return Boolean.FALSE regardless of input
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean(null, null));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean("", null));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean(Boolean.TRUE, null));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean(0, null));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean(new Object(), null));
    }

    @Test
    public void testToCharacterAlwaysReturnsMinValue() {
        // The method should always return the minimum char value (\\u0000)
        assertEquals(Character.MIN_VALUE, (char) VoidConversions.toCharacter(null, null));
        assertEquals(Character.MIN_VALUE, (char) VoidConversions.toCharacter("", null));
        assertEquals(Character.MIN_VALUE, (char) VoidConversions.toCharacter('A', null));
        assertEquals(Character.MIN_VALUE, (char) VoidConversions.toCharacter(123, null));
        assertEquals(Character.MIN_VALUE, (char) VoidConversions.toCharacter(new Object(), null));
    }

    @Test
    public void testMethodsWithNonNullConverter() {
        // Even when a real Converter instance is supplied, the methods must still
        // return their constant values.  We can obtain a Converter via its
        // public constructor using a default options instance.
        Converter converter = new Converter(new ConverterOptions());

        assertNull(VoidConversions.toNull("any", converter));
        assertEquals(Boolean.FALSE, VoidConversions.toBoolean("any", converter));
        assertEquals(Character.MIN_VALUE, (char) VoidConversions.toCharacter("any", converter));
    }
}
