package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class ByteUtilitiesTest {

    /* -------------------------------------------------
     *  Tests for decode(String)
     * ------------------------------------------------- */

    @Test
    public void testDecodeNormalHexString() {
        String hex = "0A1B";
        byte[] expected = new byte[] { (byte)0x0A, (byte)0x1B };
        assertArrayEquals(expected, ByteUtilities.decode(hex));
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull(ByteUtilities.decode("ABC")); // length 3 -> odd
    }

    @Test
    public void testDecodeInvalidHexCharacters() {
        // 'Z' is not a valid hex digit, Character.digit returns -1
        // Expected byte = (hi * 16 + lo) = (-1 * 16 + -1) = -17 => (byte)0xEF
        byte[] result = ByteUtilities.decode("ZZ");
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals((byte)0xEF, result[0]);
    }

    @Test(expected = NullPointerException.class)
    public void testDecodeNullStringThrowsNPE() {
        ByteUtilities.decode(null);
    }

    /* -------------------------------------------------
     *  Tests for encode(byte[])
     * ------------------------------------------------- */

    @Test
    public void testEncodeNormalByteArray() {
        byte[] data = new byte[] { (byte)0x0A, (byte)0x1B };
        String expected = "0A1B";
        assertEquals(expected, ByteUtilities.encode(data));
    }

    @Test
    public void testEncodeEmptyArrayReturnsEmptyString() {
        assertEquals("", ByteUtilities.encode(new byte[0]));
    }

    @Test(expected = NullPointerException.class)
    public void testEncodeNullArrayThrowsNPE() {
        ByteUtilities.encode(null);
    }

    /* -------------------------------------------------
     *  Tests for isGzipped(byte[])
     * ------------------------------------------------- */

    @Test
    public void testIsGzippedTrueWhenHeaderMatches() {
        byte[] gzipped = new byte[] { (byte)0x1F, (byte)0x8B, 0x00, 0x01 };
        assertTrue(ByteUtilities.isGzipped(gzipped));
    }

    @Test
    public void testIsGzippedFalseWhenHeaderDoesNotMatch() {
        byte[] notGzipped = new byte[] { (byte)0x00, (byte)0x00 };
        assertFalse(ByteUtilities.isGzipped(notGzipped));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testIsGzippedWithTooShortArrayThrowsAIOOBE() {
        ByteUtilities.isGzipped(new byte[] { (byte)0x1F });
    }

    @Test(expected = NullPointerException.class)
    public void testIsGzippedNullArrayThrowsNPE() {
        ByteUtilities.isGzipped(null);
    }
}
