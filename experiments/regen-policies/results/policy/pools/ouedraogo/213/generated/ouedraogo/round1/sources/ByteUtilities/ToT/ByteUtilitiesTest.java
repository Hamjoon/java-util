package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class ByteUtilitiesTest {

    /* ----------------------------------------------------------------------
     * toHexChar(int) – basic mapping and bit‑mask behavior
     * ---------------------------------------------------------------------- */
    @Test
    public void testToHexCharBasicMapping() {
        // 0 .. 15 map to their obvious hex characters
        char[] expected = {
                '0','1','2','3','4','5','6','7',
                '8','9','A','B','C','D','E','F'
        };
        for (int i = 0; i < 16; i++) {
            assertEquals("value " + i, expected[i], ByteUtilities.toHexChar(i));
        }
    }

    @Test
    public void testToHexCharMasking() {
        // The method masks with 0x0F, therefore values outside 0‑15 wrap around
        assertEquals('0', ByteUtilities.toHexChar(16));   // 16 & 0x0F == 0
        assertEquals('F', ByteUtilities.toHexChar(31));   // 31 & 0x0F == 15
        assertEquals('F', ByteUtilities.toHexChar(-1));   // -1 & 0x0F == 15
        assertEquals('0', ByteUtilities.toHexChar(-16));  // -16 & 0x0F == 0
    }

    /* ----------------------------------------------------------------------
     * encode(byte[]) – normal, empty and negative‑byte handling
     * ---------------------------------------------------------------------- */
    @Test
    public void testEncodeEmptyArray() {
        assertEquals("", ByteUtilities.encode(new byte[0]));
    }

    @Test
    public void testEncodeTypicalValues() {
        byte[] data = {0x00, 0x01, 0x0A, 0x1F, (byte)0x80, (byte)0xFF};
        // Expected string: "00010A1F80FF"
        assertEquals("00010A1F80FF", ByteUtilities.encode(data));
    }

    @Test
    public void testEncodeRoundTrip() {
        byte[] original = {(byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF, 0x00, 0x7F};
        String hex = ByteUtilities.encode(original);
        byte[] decoded = ByteUtilities.decode(hex);
        assertArrayEquals("Round‑trip encode→decode must reproduce original bytes",
                original, decoded);
    }

    /* ----------------------------------------------------------------------
     * decode(String) – valid, odd length, invalid characters, non‑ASCII, empty
     * ---------------------------------------------------------------------- */
    @Test
    public void testDecodeValidUppercase() {
        String hex = "0A1B2C3D4E5F";
        byte[] expected = {
                0x0A, 0x1B, 0x2C, 0x3D, 0x4E, 0x5F
        };
        assertArrayEquals(expected, ByteUtilities.decode(hex));
    }

    @Test
    public void testDecodeValidMixedCase() {
        String hex = "aBcDeF012345";
        byte[] expected = {
                (byte)0xAB, (byte)0xCD, (byte)0xEF,
                0x01, 0x23, 0x45
        };
        assertArrayEquals(expected, ByteUtilities.decode(hex));
    }

    @Test
    public void testDecodeEmptyString() {
        assertArrayEquals(new byte[0], ByteUtilities.decode(""));
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull(ByteUtilities.decode("ABC")); // 3 characters
    }

    @Test
    public void testDecodeInvalidCharacterReturnsNull() {
        assertNull(ByteUtilities.decode("01G2")); // 'G' is not a hex digit
    }

    @Test
    public void testDecodeNonAsciiCharacterReturnsNull() {
        // Unicode character beyond ASCII range (e.g., 'Ω' = U+03A9)
        String hex = "01" + "\u03A9" + "02";
        assertNull(ByteUtilities.decode(hex));
    }

    /* ----------------------------------------------------------------------
     * isGzipped(byte[]) – null, short, correct header, wrong header
     * ---------------------------------------------------------------------- */
    @Test
    public void testIsGzippedNull() {
        assertFalse(ByteUtilities.isGzipped(null));
    }

    @Test
    public void testIsGzippedTooShort() {
        assertFalse(ByteUtilities.isGzipped(new byte[]{0x1F}));
    }

    @Test
    public void testIsGzippedTrue() {
        byte[] gzippedHeader = {(byte)0x1F, (byte)0x8B, 0x08, 0x00};
        assertTrue(ByteUtilities.isGzipped(gzippedHeader));
    }

    @Test
    public void testIsGzippedFalseDifferentHeader() {
        byte[] notGzipped = {(byte)0x1F, (byte)0x00, 0x00, 0x00};
        assertFalse(ByteUtilities.isGzipped(notGzipped));
    }

    @Test
    public void testIsGzippedFalseRandomData() {
        byte[] random = {0x00, 0x01, 0x02, 0x03};
        assertFalse(ByteUtilities.isGzipped(random));
    }
}
