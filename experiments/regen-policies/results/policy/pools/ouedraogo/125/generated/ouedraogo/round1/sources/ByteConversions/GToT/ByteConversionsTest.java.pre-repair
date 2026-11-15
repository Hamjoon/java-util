package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class ByteConversionsTest {

    /**
     * Basic functionality test – converts a normal Byte value to a Character.
     * The expected character is the Unicode code point equal to the signed byte value.
     */
    @Test
    public void testToCharacter_Basic() {
        Byte b = Byte.valueOf((byte) 65);               // ASCII 'A'
        Character result = ByteConversions.toCharacter(b, new Converter(new ConverterOptions()));
        assertEquals(Character.valueOf('A'), result);
    }

    /**
     * Edge case: the most negative byte value (-128).  When cast to char the
     * signed byte is first sign‑extended to an int (-128), then narrowed to a char,
     * resulting in the same 16‑bit pattern 0xFF80 (character '\uFF80').
     */
    @Test
    public void testToCharacter_NegativeBoundary() {
        Byte b = Byte.valueOf((byte) -128);
        Character result = ByteConversions.toCharacter(b, new Converter(new ConverterOptions()));
        assertEquals(Character.valueOf('\uFF80'), result);
    }

    /**
     * Edge case: the most positive byte value (127).  The resulting character
     * should be '\u007F' (the DELETE control character).
     */
    @Test
    public void testToCharacter_PositiveBoundary() {
        Byte b = Byte.valueOf((byte) 127);
        Character result = ByteConversions.toCharacter(b, new Converter(new ConverterOptions()));
        assertEquals(Character.valueOf('\u007F'), result);
    }

    /**
     * Edge case: zero – should map to the null character '\u0000'.
     */
    @Test
    public void testToCharacter_Zero() {
        Byte b = Byte.valueOf((byte) 0);
        Character result = ByteConversions.toCharacter(b, new Converter(new ConverterOptions()));
        assertEquals(Character.valueOf('\u0000'), result);
    }

    /**
     * Verify that passing a null Byte reference throws a NullPointerException.
     * The method casts the argument to Byte before dereferencing, so a null
     * value results in NPE at runtime.
     */
    @Test(expected = NullPointerException.class)
    public void testToCharacter_NullInput() {
        ByteConversions.toCharacter(null, new Converter(new ConverterOptions()));
    }

    /**
     * Verify that passing an object of a type other than Byte results in a
     * ClassCastException.  The method performs an unchecked cast to Byte.
     */
    @Test(expected = ClassCastException.class)
    public void testToCharacter_WrongType() {
        Integer notAByte = Integer.valueOf(10);
        ByteConversions.toCharacter(notAByte, new Converter(new ConverterOptions()));
    }

    /**
     * Even though the Converter argument is not used by the current implementation,
     * we still pass a fully‑initialised instance to ensure the method signature works
     * with any valid Converter object.
     */
    @Test
    public void testToCharacter_WithCustomConverter() {
        Byte b = Byte.valueOf((byte) 42);
        // Create a Converter with default options – the conversion logic ignores it.
        Converter converter = new Converter(new ConverterOptions());
        Character result = ByteConversions.toCharacter(b, converter);
        assertEquals(Character.valueOf('*'), result); // 42 is '*'
    }
}
