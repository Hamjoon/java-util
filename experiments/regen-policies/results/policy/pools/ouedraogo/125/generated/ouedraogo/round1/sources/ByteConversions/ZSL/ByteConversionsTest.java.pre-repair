package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class ByteConversionsTest {

    // Helper method to create a minimal Converter instance.
    // The Converter constructor accepts a ConverterOptions; passing null is sufficient for these tests.
    private Converter createConverter() {
        return new Converter(null);
    }

    @Test
    public void testZeroByteConversion() {
        Byte b = 0;
        Character result = ByteConversions.toCharacter(b, createConverter());
        assertEquals((char)0, (char)result.charValue());
    }

    @Test
    public void testPositiveByteConversion() {
        Byte b = 65; // ASCII 'A'
        Character result = ByteConversions.toCharacter(b, createConverter());
        assertEquals('A', result.charValue());
    }

    @Test
    public void testNegativeByteConversion() {
        Byte b = -1;
        Character result = ByteConversions.toCharacter(b, createConverter());
        // -1 as a signed byte is 0xFF, sign‑extended to 0xFFFFFFFF, then truncated to char -> 0xFFFF
        assertEquals('\uFFFF', result.charValue());
    }

    @Test
    public void testMinByteValueConversion() {
        Byte b = -128;
        Character result = ByteConversions.toCharacter(b, createConverter());
        // -128 = 0x80, sign‑extended -> 0xFFFFFF80 -> char 0xFF80
        assertEquals('\uFF80', result.charValue());
    }

    @Test
    public void testMaxByteValueConversion() {
        Byte b = 127;
        Character result = ByteConversions.toCharacter(b, createConverter());
        assertEquals((char)127, result.charValue());
    }

    @Test
    public void testAllByteValues() {
        Converter converter = createConverter();
        for (int i = -128; i <= 127; i++) {
            Byte b = (byte) i;
            Character ch = ByteConversions.toCharacter(b, converter);
            char expected = (char) b.byteValue(); // same logic as the method under test
            assertEquals("Failed at byte value: " + i, expected, ch.charValue());
        }
    }

    @Test
    public void testNullFromObjectThrowsNPE() {
        Converter converter = createConverter();
        try {
            ByteConversions.toCharacter(null, converter);
            fail("Expected NullPointerException when 'from' is null");
        } catch (NullPointerException expected) {
            // expected outcome
        }
    }

    @Test
    public void testNonByteFromObjectThrowsClassCastException() {
        Converter converter = createConverter();
        Integer notAByte = Integer.valueOf(10);
        try {
            ByteConversions.toCharacter(notAByte, converter);
            fail("Expected ClassCastException when 'from' is not a Byte");
        } catch (ClassCastException expected) {
            // expected outcome
        }
    }

    @Test
    public void testNullConverterIsIgnored() {
        Byte b = 42;
        // The converter argument is not used inside the method, so passing null is valid.
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals((char)42, result.charValue());
    }
}
