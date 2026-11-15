package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class ByteUtilitiesTest {

    /* ---------- toHexChar ---------- */

    @Test
    public void testToHexCharBasicValues() {
        // Verify mapping for 0‑15
        char[] expected = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
        for (int i = 0; i < 16; i++) {
            assertEquals("Failed at value " + i, expected[i], ByteUtilities.toHexChar(i));
        }
    }

    @Test
    public void testToHexCharMaskingHigherBits() {
        // Values beyond 0‑15 should be masked with 0x0F
        assertEquals('A', ByteUtilities.toHexChar(0x1A)); // 0x1A & 0x0F = 0xA
        assertEquals('5', ByteUtilities.toHexChar(0x25)); // 0x25 & 0x0F = 0x5
        assertEquals('F', ByteUtilities.toHexChar(0xFF)); // 0xFF & 0x0F = 0xF
    }

    @Test
    public void testToHexCharNegativeValue() {
        // Negative values are also masked
        assertEquals('F', ByteUtilities.toHexChar(-1)); // -1 & 0x0F = 0xF
        assertEquals('0', ByteUtilities.toHexChar(-16)); // -16 & 0x0F = 0x0
    }

    /* ---------- encode ---------- */

    @Test
    public void testEncodeEmptyArray() {
        assertEquals("", ByteUtilities.encode(new byte[0]));
    }

    @Test
    public void testEncodeSingleByte() {
        assertEquals("00", ByteUtilities.encode(new byte[]{0x00}));
        assertEquals("FF", ByteUtilities.encode(new byte[]{(byte)0xFF}));
        assertEquals("7A", ByteUtilities.encode(new byte[]{0x7A}));
    }

    @Test
    public void testEncodeMultipleBytes() {
        byte[] data = { (byte)0x1F, (byte)0x8B, (byte)0x3C, (byte)0x00, (byte)0xAB };
        String expected = "1F8B3C00AB";
        assertEquals(expected, ByteUtilities.encode(data));
    }

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] original = new byte[256];
        for (int i = 0; i < original.length; i++) {
            original[i] = (byte) i;
        }
        String hex = ByteUtilities.encode(original);
        byte[] decoded = ByteUtilities.decode(hex);
        assertArrayEquals("Round‑trip encode/decode failed", original, decoded);
    }

    /* ---------- decode ---------- */

    @Test
    public void testDecodeValidUppercase() {
        String hex = "1F8B3C";
        byte[] expected = { (byte)0x1F, (byte)0x8B, (byte)0x3C };
        assertArrayEquals(expected, ByteUtilities.decode(hex));
    }

    @Test
    public void testDecodeValidMixedCase() {
        String hex = "aBcDeF";
        byte[] expected = { (byte)0xAB, (byte)0xCD, (byte)0xEF };
        assertArrayEquals(expected, ByteUtilities.decode(hex));
    }

    @Test
    public void testDecodeEmptyString() {
        assertArrayEquals(new byte[0], ByteUtilities.decode(""));
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull(ByteUtilities.decode("ABC")); // length 3
        assertNull(ByteUtilities.decode("1"));   // length 1
    }

    @Test
    public void testDecodeInvalidCharactersReturnNull() {
        assertNull(ByteUtilities.decode("ZZ"));   // not a hex digit
        assertNull(ByteUtilities.decode("12G4")); // 'G' invalid
        assertNull(ByteUtilities.decode("00 11")); // space invalid
        // Character beyond ASCII range (e.g., Unicode emoji) should also cause null
        assertNull(ByteUtilities.decode("\u263A\u263B"));
    }

    @Test
    public void testDecodeAllHexDigits() {
        // Build a string containing all valid hex chars twice (upper and lower)
        StringBuilder sb = new StringBuilder();
        for (char c = '0'; c <= '9'; c++) sb.append(c);
        for (char c = 'A'; c <= 'F'; c++) sb.append(c);
        for (char c = 'a'; c <= 'f'; c++) sb.append(c);
        // Duplicate to make even length
        String hex = sb.toString() + sb.toString();

        // Decode and then re‑encode – should be identical
        byte[] decoded = ByteUtilities.decode(hex);
        assertNotNull(decoded);
        String reEncoded = ByteUtilities.encode(decoded);
        assertEquals(hex.toUpperCase(), reEncoded);
    }

    /* ---------- isGzipped ---------- */

    @Test
    public void testIsGzippedTrue() {
        byte[] gzipped = { (byte)0x1F, (byte)0x8B, 0x00, 0x01 };
        assertTrue(ByteUtilities.isGzipped(gzipped));
    }

    @Test
    public void testIsGzippedFalseWrongHeader() {
        byte[] notGz = { 0x00, 0x00 };
        assertFalse(ByteUtilities.isGzipped(notGz));
    }

    @Test
    public void testIsGzippedFalseTooShort() {
        byte[] single = { (byte)0x1F };
        assertFalse(ByteUtilities.isGzipped(single));
    }

    @Test
    public void testIsGzippedNull() {
        assertFalse(ByteUtilities.isGzipped(null));
    }

    @Test
    public void testIsGzippedBoundaryValues() {
        // Header bytes correct but additional data missing
        byte[] exactHeader = { (byte)0x1F, (byte)0x8B };
        assertTrue(ByteUtilities.isGzipped(exactHeader));
    }
}
