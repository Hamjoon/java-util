package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class ByteConversionsTest {

    // ------------------------------------------------------------------------
    // 1. List of public (package‑private static) methods in ByteConversions
    // ------------------------------------------------------------------------
    //   static Character toCharacter(Object from, Converter converter)
    // ------------------------------------------------------------------------

    /**
     * Helper to create a minimal Converter instance required by the
     * {@code toCharacter} method.  The method does not use the Converter at
     * all, so a default instance with default options is sufficient.
     */
    private static Converter createConverter() {
        // ConverterOptions has a public no‑arg constructor (not shown here)
        // but we can use the default constructor of Converter directly.
        return new Converter(new ConverterOptions());
    }

    // ------------------------------------------------------------------------
    // 2. Basic functional test for toCharacter
    // ------------------------------------------------------------------------
    @Test
    public void testToCharacterBasic() {
        Byte input = (byte) 65;                 // ASCII 'A'
        Converter converter = createConverter();

        Character result = ByteConversions.toCharacter(input, converter);

        assertEquals(Character.valueOf('A'), result);
    }

    // ------------------------------------------------------------------------
    // 3. Edge‑case tests and exception handling
    // ------------------------------------------------------------------------

    @Test
    public void testToCharacterWithZero() {
        Byte input = (byte) 0;
        Converter converter = createConverter();

        Character result = ByteConversions.toCharacter(input, converter);

        assertEquals(Character.valueOf('\u0000'), result);
    }

    @Test
    public void testToCharacterWithNegativeValue() {
        Byte input = (byte) -1;                 // 0xFF => 255 when unsigned, but cast to char yields 0xFFFF
        Converter converter = createConverter();

        Character result = ByteConversions.toCharacter(input, converter);

        // In Java, casting a signed byte -1 to char yields 0xFFFF (65535)
        assertEquals(Character.valueOf('\uFFFF'), result);
    }

    @Test
    public void testToCharacterWithMaxPositiveByte() {
        Byte input = Byte.MAX_VALUE;            // 127
        Converter converter = createConverter();

        Character result = ByteConversions.toCharacter(input, converter);

        assertEquals(Character.valueOf((char) 127), result);
    }

    @Test
    public void testToCharacterWithMinByte() {
        Byte input = Byte.MIN_VALUE;            // -128
        Converter converter = createConverter();

        Character result = ByteConversions.toCharacter(input, converter);

        // -128 -> 0xFF80 -> char value 0xFF80 (65408)
        assertEquals(Character.valueOf('\uFF80'), result);
    }

    @Test(expected = ClassCastException.class)
    public void testToCharacterWithNonByteObject() {
        // Passing an object that is not a Byte should trigger a ClassCastException
        // because the method casts the argument to Byte without checking.
        String notAByte = "not a byte";
        Converter converter = createConverter();

        ByteConversions.toCharacter(notAByte, converter);
    }

    @Test(expected = NullPointerException.class)
    public void testToCharacterWithNullFrom() {
        // The method dereferences the 'from' argument to cast it to Byte,
        // so a null value will cause a NullPointerException.
        Converter converter = createConverter();

        ByteConversions.toCharacter(null, converter);
    }

    @Test
    public void testToCharacterIgnoresConverter() {
        // The converter argument is not used; ensure that a null converter does not affect the result.
        Byte input = (byte) 66; // 'B'
        Character result = ByteConversions.toCharacter(input, null);
        assertEquals(Character.valueOf('B'), result);
    }
}
