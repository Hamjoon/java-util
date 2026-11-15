package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class VoidConversionsTest {

    // Helper to obtain a Converter instance.  The Converter class requires a
    // ConverterOptions object; we use the default constructor (the library
    // provides one) because the VoidConversions methods never actually use the
    // Converter instance.
    private Converter getConverter() {
        return new Converter(new ConverterOptions());
    }

    @Test
    public void testToNullReturnsNullRegardlessOfInput() {
        Converter converter = getConverter();

        // Typical usage – non‑null source object
        Object source = "some string";
        Object result = VoidConversions.toNull(source, converter);
        assertNull("toNull should always return null for a non‑null source", result);

        // Edge case – source is null
        result = VoidConversions.toNull(null, converter);
        assertNull("toNull should return null even when source is null", result);

        // Edge case – converter is null (method does not dereference it)
        result = VoidConversions.toNull(source, null);
        assertNull("toNull should return null even when converter is null", result);
    }

    @Test
    public void testToBooleanAlwaysReturnsFalse() {
        Converter converter = getConverter();

        // Typical usage – various source types
        assertEquals("toBoolean must always return Boolean.FALSE", Boolean.FALSE,
                VoidConversions.toBoolean("true", converter));
        assertEquals("toBoolean must always return Boolean.FALSE", Boolean.FALSE,
                VoidConversions.toBoolean(123, converter));
        assertEquals("toBoolean must always return Boolean.FALSE", Boolean.FALSE,
                VoidConversions.toBoolean(true, converter));

        // Edge case – source is null
        assertEquals("toBoolean must return Boolean.FALSE for null source", Boolean.FALSE,
                VoidConversions.toBoolean(null, converter));

        // Edge case – converter is null
        assertEquals("toBoolean must return Boolean.FALSE when converter is null", Boolean.FALSE,
                VoidConversions.toBoolean("any", null));
    }

    @Test
    public void testToCharacterAlwaysReturnsMinValue() {
        Converter converter = getConverter();

        // Typical usage – various source objects
        assertEquals("toCharacter must always return Character.MIN_VALUE", Character.MIN_VALUE,
                VoidConversions.toCharacter("a", converter).charValue());
        assertEquals("toCharacter must always return Character.MIN_VALUE", Character.MIN_VALUE,
                VoidConversions.toCharacter(42, converter).charValue());
        assertEquals("toCharacter must always return Character.MIN_VALUE", Character.MIN_VALUE,
                VoidConversions.toCharacter(true, converter).charValue());

        // Edge case – source is null
        assertEquals("toCharacter must return Character.MIN_VALUE for null source", Character.MIN_VALUE,
                VoidConversions.toCharacter(null, converter).charValue());

        // Edge case – converter is null
        assertEquals("toCharacter must return Character.MIN_VALUE when converter is null", Character.MIN_VALUE,
                VoidConversions.toCharacter("any", null).charValue());
    }
}
