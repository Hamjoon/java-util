package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;
import java.util.Arrays;

public class ByteUtilitiesTest {

    // ---------- toHexChar ----------
    @Test
    public void testToHexCharValidRange() {
        // 0-15 map to '0'..'F'
        char[] expected = "0123456789ABCDEF".toCharArray();
        for (int i = 0; i < 16; i++) {
            assertEquals("toHexChar(" + i + ")", expected[i], ByteUtilities.toHexChar(i));
        }
    }

    @Test
    public void testToHexCharMasking() {
        // Values outside 0-15 should be masked by &0x0F
        assertEquals('A', ByteUtilities.toHexChar(0x0A));
        assertEquals('A', ByteUtilities.toHexChar(0x1A)); // 0x1A & 0x0F == 0x0A
        assertEquals('F', ByteUtilities.toHexChar(0xFF)); // 0xFF & 0x0F == 0x0F
        assertEquals('0', ByteUtilities.toHexChar(0x10)); // 0x10 & 0x0F == 0x00
    }

    // ---------- encode ----------
    @Test
    public void testEncodeEmptyArray() {
        assertEquals("", ByteUtilities.encode(new byte[0]));
    }

    @Test
    public void testEncodeSingleBytes() {
        assertEquals("00", ByteUtilities.encode(new byte[]{0x00}));
        assertEquals("0F", ByteUtilities.encode(new byte[]{0x0F}));
        assertEquals("10", ByteUtilities.encode(new byte[]{0x10}));
        assertEquals("FF", ByteUtilities.encode(new byte[]{(byte) 0xFF}));
    }

    @Test
    public void testEncodeMultipleBytes() {
        byte[] data = {(byte)0x1F, (byte)0x8B, (byte)0x3C, (byte)0x00, (byte)0xAB};
        assertEquals("1F8B3C00AB", ByteUtilities.encode(data));
    }

    // ---------- decode ----------
    @Test
    public void testDecodeValidEvenLength() {
        String hex = "1F8B3C00AB";
        byte[] expected = {(byte)0x1F, (byte)0x8B, (byte)0x3C, (byte)0x00, (byte)0xAB};
        assertArrayEquals(expected, ByteUtilities.decode(hex));
    }

    @Test
    public void testDecodeLowerCaseHex() {
        String hex = "deadbeef";
        byte[] expected = {(byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF};
        assertArrayEquals(expected, ByteUtilities.decode(hex));
    }

    @Test
    public void testDecodeEmptyString() {
        assertArrayEquals(new byte[0], ByteUtilities.decode(""));
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull(ByteUtilities.decode("ABC")); // length 3
    }

    @Test
    public void testDecodeInvalidCharactersReturnNull() {
        assertNull(ByteUtilities.decode("GG")); // G not a hex digit
        assertNull(ByteUtilities.decode("0Z1")); // odd length, also Z invalid
        assertNull(ByteUtilities.decode("0Z1A")); // even length but contains Z
        assertNull(ByteUtilities.decode("12 34")); // space invalid
    }

    @Test
    public void testDecodeNonAsciiCharactersReturnNull() {
        // Char beyond lookup table (e.g., Unicode emoji)
        assertNull(ByteUtilities.decode("\u2603\u2603")); // snowman chars
    }

    // ---------- round‑trip ----------
    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] original = new byte[256];
        for (int i = 0; i < 256; i++) {
            original[i] = (byte) i;
        }
        String encoded = ByteUtilities.encode(original);
        byte[] decoded = ByteUtilities.decode(encoded);
        assertArrayEquals(original, decoded);
    }

    // ---------- isGzipped ----------
    @Test
    public void testIsGzippedTrue() {
        byte[] gzipped = {(byte)0x1F, (byte)0x8B, 0x00, 0x01};
        assertTrue(ByteUtilities.isGzipped(gzipped));
    }

    @Test
    public void testIsGzippedFalseDifferentHeader() {
        byte[] notGzipped = {(byte)0x1F, (byte)0x00, 0x00};
        assertFalse(ByteUtilities.isGzipped(notGzipped));
    }

    @Test
    public void testIsGzippedFalseShortArray() {
        assertFalse(ByteUtilities.isGzipped(new byte[]{(byte)0x1F}));
        assertFalse(ByteUtilities.isGzipped(new byte[0]));
    }

    @Test
    public void testIsGzippedNull() {
        assertFalse(ByteUtilities.isGzipped(null));
    }

    // ---------- additional edge cases ----------
    @Test
    public void testEncodeDecodeWithAllZeroes() {
        byte[] zeros = new byte[10];
        String encoded = ByteUtilities.encode(zeros);
        assertEquals("00000000000000000000", encoded);
        assertArrayEquals(zeros, ByteUtilities.decode(encoded));
    }

    @Test
    public void testEncodeDecodeWithNegativeBytes() {
        byte[] data = {(byte)-1, (byte)-128, (byte)-2};
        // -1 -> 0xFF, -128 -> 0x80, -2 -> 0xFE
        assertEquals("FF80FE", ByteUtilities.encode(data));
        assertArrayEquals(data, ByteUtilities.decode("FF80FE"));
    }
}
