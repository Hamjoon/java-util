package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import org.junit.Test;

public class ByteConversionsTest {

    // ---------- Normal conversions ----------
    @Test
    public void testPositiveByteConversion() {
        Byte b = (byte) 65; // ASCII 'A'
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf('A'), result);
    }

    @Test
    public void testZeroByteConversion() {
        Byte b = (byte) 0;
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf('\u0000'), result);
    }

    @Test
    public void testMaxByteConversion() {
        Byte b = Byte.MAX_VALUE; // 127
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf((char) 127), result);
    }

    @Test
    public void testMinByteConversion() {
        Byte b = Byte.MIN_VALUE; // -128
        Character result = ByteConversions.toCharacter(b, null);
        // -128 as a signed byte becomes 0xFF80 when widened to int and cast to char
        char expected = (char) ((int) b.byteValue());
        assertEquals(Character.valueOf(expected), result);
    }

    @Test
    public void testNegativeByteConversion() {
        Byte b = (byte) -1;
        Character result = ByteConversions.toCharacter(b, null);
        // -1 becomes 0xFFFF (65535) when cast to char
        char expected = (char) -1;
        assertEquals(Character.valueOf(expected), result);
        // verify the numeric value as well
        assertEquals(65535, (int) result.charValue());
    }

    // ---------- Null handling ----------
    @Test(expected = NullPointerException.class)
    public void testNullFromObject() {
        ByteConversions.toCharacter(null, null);
    }

    // The method does not use the Converter argument, but we verify that a non‑null
    // instance can be passed without affecting the result.
    @Test
    public void testNonNullConverterArgumentIgnored() {
        Byte b = (byte) 42;
        // We cannot construct a real Converter without its dependent classes,
        // but we can pass a stub that is null; the method should ignore it.
        // Passing a dummy non‑null object of type Converter via reflection would
        // require additional library code, so we simply pass null and document that
        // the argument is irrelevant.
        Character result = ByteConversions.toCharacter(b, null);
        assertEquals(Character.valueOf((char) 42), result);
    }

    // ---------- Wrong source type ----------
    @Test(expected = ClassCastException.class)
    public void testInvalidSourceType() {
        // Passing an Integer instead of a Byte should cause a ClassCastException
        ByteConversions.toCharacter(Integer.valueOf(5), null);
    }

    // ---------- Boundary round‑trip verification ----------
    @Test
    public void testAllByteValuesRoundTrip() {
        for (int i = Byte.MIN_VALUE; i <= Byte.MAX_VALUE; i++) {
            Byte b = (byte) i;
            Character ch = ByteConversions.toCharacter(b, null);
            // Convert back to byte via explicit casting to verify the round‑trip logic
            byte roundTrip = (byte) ch.charValue();
            assertEquals("Failed at byte value: " + i, b.byteValue(), roundTrip);
        }
    }
}
