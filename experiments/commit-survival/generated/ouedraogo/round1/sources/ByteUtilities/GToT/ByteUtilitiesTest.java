package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class ByteUtilitiesTest {

    /* -------------------------------------------------
     *  Tests for ByteUtilities.decode(String)
     * ------------------------------------------------- */

    @Test
    public void testDecodeEvenLength_NormalHex() {
        String hex = "0A1B2C3D4E5F";
        byte[] expected = new byte[] {
                (byte)0x0A, (byte)0x1B, (byte)0x2C, (byte)0x3D, (byte)0x4E, (byte)0x5F
        };
        assertArrayEquals(expected, ByteUtilities.decode(hex));
    }

    @Test
    public void testDecodeOddLength_ReturnsNull() {
        String odd = "ABC";                 // length 3, not even
        assertNull(ByteUtilities.decode(odd));
    }

    @Test
    public void testDecode_InvalidHexCharacters() {
        // 'G' and 'Z' are not valid hex digits, Character.digit returns -1
        String mixed = "0GZ1";
        // Expected calculation:
        // '0' -> 0, 'G' -> -1   => (0*16 + -1) = -1  (0xFF)
        // 'Z' -> -1, '1' -> 1   => (-1*16 + 1) = -15 (0xF1)
        byte[] result = ByteUtilities.decode(mixed);
        assertEquals(2, result.length);
        assertEquals((byte)0xFF, result[0]); // -1 as unsigned byte is 0xFF
        assertEquals((byte)0xF1, result[1]); // -15 as unsigned byte is 0xF1
    }

    @Test(expected = NullPointerException.class)
    public void testDecode_NullString_ThrowsNPE() {
        ByteUtilities.decode(null);
    }

    /* -------------------------------------------------
     *  Tests for ByteUtilities.encode(byte[])
     * ------------------------------------------------- */

    @Test
    public void testEncode_EmptyArray_ReturnsEmptyString() {
        byte[] empty = new byte[0];
        assertEquals("", ByteUtilities.encode(empty));
    }

    @Test
    public void testEncode_SingleByte() {
        byte[] single = new byte[] { (byte)0xAB };
        assertEquals("AB", ByteUtilities.encode(single));
    }

    @Test
    public void testEncode_RoundTrip() {
        byte[] original = new byte[] {
                (byte)0x00, (byte)0x7F, (byte)0x80, (byte)0xFF,
                (byte)0x12, (byte)0x34, (byte)0x56, (byte)0x78
        };
        String hex = ByteUtilities.encode(original);
        assertEquals("007F80FF12345678", hex);
        assertArrayEquals(original, ByteUtilities.decode(hex));
    }

    /* -------------------------------------------------
     *  Tests for ByteUtilities.isGzipped(byte[])
     * ------------------------------------------------- */

    @Test
    public void testIsGzipped_TrueWhenHeaderMatches() {
        byte[] gzipped = new byte[] { (byte)0x1F, (byte)0x8B, 0x00, 0x00 };
        assertTrue(ByteUtilities.isGzipped(gzipped));
    }

    @Test
    public void testIsGzipped_FalseWhenHeaderDoesNotMatch() {
        byte[] notGzipped = new byte[] { (byte)0x00, (byte)0x00 };
        assertFalse(ByteUtilities.isGzipped(notGzipped));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testIsGzipped_ArrayTooShort_ThrowsException() {
        byte[] tooShort = new byte[] { (byte)0x1F }; // only one byte
        ByteUtilities.isGzipped(tooShort);
    }
}
