package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class ByteUtilitiesTest {

    /* ------------------------------------------------------------
     *  Tests for decode(String)
     * ------------------------------------------------------------ */

    @Test
    public void testDecodeEvenLengthValidHex() {
        // "00FF7A" -> {0x00, 0xFF, 0x7A}
        byte[] expected = new byte[] {0x00, (byte)0xFF, 0x7A};
        assertArrayEquals(expected, ByteUtilities.decode("00FF7A"));
    }

    @Test
    public void testDecodeLowerCaseHex() {
        // lower‑case hex characters are also valid
        byte[] expected = new byte[] {0x0A, 0x1B, 0x2C};
        assertArrayEquals(expected, ByteUtilities.decode("0a1b2c"));
    }

    @Test
    public void testDecodeMixedCaseHex() {
        byte[] expected = new byte[] {0x0F, (byte)0xE0};
        assertArrayEquals(expected, ByteUtilities.decode("0Fe0"));
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull(ByteUtilities.decode("ABC")); // length 3
        assertNull(ByteUtilities.decode("1"));   // length 1
    }

    @Test
    public void testDecodeInvalidCharactersYieldNegativeDigits() {
        // 'G' and '?' are not valid hex digits → Character.digit returns -1
        // Resulting byte should be (hi*16 + lo) where hi or lo is -1.
        // For "G?" we expect ((-1)*16 + (-1)) = -17 -> (byte)0xEF
        byte[] expected = new byte[] {(byte)0xEF};
        assertArrayEquals(expected, ByteUtilities.decode("G?"));
    }

    @Test(expected = NullPointerException.class)
    public void testDecodeNullStringThrowsNPE() {
        ByteUtilities.decode(null);
    }

    /* ------------------------------------------------------------
     *  Tests for encode(byte[])
     * ------------------------------------------------------------ */

    @Test
    public void testEncodeEmptyArray() {
        assertEquals("", ByteUtilities.encode(new byte[0]));
    }

    @Test
    public void testEncodeSingleByte() {
        assertEquals("00", ByteUtilities.encode(new byte[] {0x00}));
        assertEquals("FF", ByteUtilities.encode(new byte[] {(byte)0xFF}));
        assertEquals("7A", ByteUtilities.encode(new byte[] {0x7A}));
    }

    @Test
    public void testEncodeMultipleBytes() {
        byte[] input = new byte[] {0x12, (byte)0xAB, 0x00, 0x5F};
        // Expected: "12AB005F"
        assertEquals("12AB005F", ByteUtilities.encode(input));
    }

    @Test
    public void testEncodeRoundTripWithDecode() {
        String hex = "DEADBEEFCAFEBABE";
        byte[] decoded = ByteUtilities.decode(hex);
        assertNotNull(decoded);
        String reEncoded = ByteUtilities.encode(decoded);
        assertEquals(hex, reEncoded);
    }

    @Test(expected = NullPointerException.class)
    public void testEncodeNullArrayThrowsNPE() {
        ByteUtilities.encode(null);
    }

    /* ------------------------------------------------------------
     *  Tests for isGzipped(byte[])
     * ------------------------------------------------------------ */

    @Test
    public void testIsGzippedTrue() {
        byte[] gzipped = new byte[] {(byte)0x1F, (byte)0x8B, 0x08, 0x00};
        assertTrue(ByteUtilities.isGzipped(gzipped));
    }

    @Test
    public void testIsGzippedFalseDifferentHeader() {
        byte[] notGzipped = new byte[] {0x00, 0x00, 0x1F, (byte)0x8B};
        assertFalse(ByteUtilities.isGzipped(notGzipped));
    }

    @Test
    public void testIsGzippedFalseSingleByteArray() {
        byte[] single = new byte[] {(byte)0x1F};
        // Only first byte matches, second byte is missing → should be false
        try {
            ByteUtilities.isGzipped(single);
            fail("Expected ArrayIndexOutOfBoundsException for array length < 2");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }

    @Test
    public void testIsGzippedFalseEmptyArray() {
        byte[] empty = new byte[0];
        try {
            ByteUtilities.isGzipped(empty);
            fail("Expected ArrayIndexOutOfBoundsException for empty array");
        } catch (ArrayIndexOutOfBoundsException e) {
            // expected
        }
    }
}
