package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class ByteConversionsTest {

    /** 
     * Verify that a normal positive byte value is correctly converted to a character.
     */
    @Test
    public void testPositiveByteConversion() {
        Byte b = Byte.valueOf((byte) 65); // ASCII 'A'
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf('A'), result);
    }

    /**
     * Verify that zero is converted to the null character.
     */
    @Test
    public void testZeroByteConversion() {
        Byte b = Byte.valueOf((byte) 0);
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf('\u0000'), result);
    }

    /**
     * Verify that the maximum byte value (127) is converted correctly.
     */
    @Test
    public void testMaxByteConversion() {
        Byte b = Byte.valueOf(Byte.MAX_VALUE); // 127
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf((char) 127), result);
    }

    /**
     * Verify that the minimum byte value (-128) is converted correctly.
     * In Java, casting a signed byte to char sign‑extends the value,
     * resulting in a char with the numeric value 0xFF80 (65408).
     */
    @Test
    public void testMinByteConversion() {
        Byte b = Byte.valueOf(Byte.MIN_VALUE); // -128
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf((char) 0xFF80), result);
        assertEquals(65408, result.charValue());
    }

    /**
     * Verify that a negative byte (e.g., -1) yields the expected char value (0xFFFF).
     */
    @Test
    public void testNegativeByteConversion() {
        Byte b = Byte.valueOf((byte) -1);
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf((char) 0xFFFF), result);
        assertEquals(65535, result.charValue());
    }

    /**
     * The method casts the input to Byte. Supplying an object of a different type
     * should cause a ClassCastException.
     */
    @Test(expected = ClassCastException.class)
    public void testInvalidSourceTypeThrowsClassCastException() {
        Integer notAByte = Integer.valueOf(10);
        // This line should throw ClassCastException before any other logic runs.
        ByteConversions.toCharacter(notAByte, null);
    }

    /**
     * Passing a null source object should result in a NullPointerException
     * because the method attempts to call byteValue() on the casted Byte.
     */
    @Test(expected = NullPointerException.class)
    public void testNullSourceThrowsNullPointerException() {
        ByteConversions.toCharacter(null, null);
    }

    /**
     * Although the Converter argument is not used by the implementation,
     * we still verify that calling the method with a non‑null Converter
     * does not affect the result.
     */
    @Test
    public void testNonNullConverterIsIgnored() {
        // Create a Converter instance using the public constructor.
        // The ConverterOptions class is not part of the public test scope,
        // but passing null is acceptable for this test because the
        // ByteConversions implementation never dereferences it.
        Converter converter = new Converter(null);
        Byte b = Byte.valueOf((byte) 42);
        Character result = ByteConversions.toCharacter(b, converter);
        assertEquals(Character.valueOf((char) 42), result);
    }
}
