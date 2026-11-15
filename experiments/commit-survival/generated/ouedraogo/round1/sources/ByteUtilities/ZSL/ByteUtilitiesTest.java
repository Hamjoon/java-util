package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

/**
 * Comprehensive JUnit‑4 test suite for {@link ByteUtilities}.
 * <p>
 * The tests cover:
 * <ul>
 *   <li>hex string decoding (including odd length, invalid characters)</li>
 *   <li>hex string encoding (empty array, single‑byte values, round‑trip)</li>
 *   <li>private {@code convertDigit} logic via {@code encode}</li>
 *   <li>gzip‑header detection</li>
 *   <li>behaviour on {@code null} arguments and insufficient input length</li>
 * </ul>
 * </p>
 */
public class ByteUtilitiesTest {

    /* -------------------------------------------------
     *  decode(..) tests
     * ------------------------------------------------- */

    @Test
    public void testDecodeEvenLengthValidHex() {
        String hex = "00FF7aC3";
        byte[] expected = new byte[] {0x00, (byte)0xFF, 0x7A, (byte)0xC3};
        byte[] actual = ByteUtilities.decode(hex);
        Assert.assertArrayEquals("Decoding valid even‑length hex failed", expected, actual);
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        String odd = "ABC"; // length 3
        Assert.assertNull("Odd‑length string should return null", ByteUtilities.decode(odd));
    }

    @Test
    public void testDecodeInvalidHexCharacters() {
        // 'G' and '?' are not valid hex digits – Character.digit returns -1
        String mixed = "0G?1";
        byte[] result = ByteUtilities.decode(mixed);
        // Expected: 0x0G => hi=0, lo=-1 => byte = -1
        //           ?1  => hi=-1, lo=1 => byte = (-1*16)+1 = -15
        byte[] expected = new byte[] {(byte)-1, (byte)-15};
        Assert.assertArrayEquals("Invalid characters should produce -1 based values", expected, result);
    }

    @Test(expected = NullPointerException.class)
    public void testDecodeNullThrowsNPE() {
        ByteUtilities.decode(null);
    }

    /* -------------------------------------------------
     *  encode(..) tests
     * ------------------------------------------------- */

    @Test
    public void testEncodeEmptyArray() {
        byte[] empty = new byte[0];
        Assert.assertEquals("Empty array should encode to empty string", "", ByteUtilities.encode(empty));
    }

    @Test
    public void testEncodeSingleByteValues() {
        // Test every possible nibble (0‑15) by constructing bytes where the high or low nibble varies.
        for (int hi = 0; hi <= 0xF; hi++) {
            for (int lo = 0; lo <= 0xF; lo++) {
                byte value = (byte) ((hi << 4) | lo);
                String encoded = ByteUtilities.encode(new byte[]{value});
                char expectedHi = "0123456789ABCDEF".charAt(hi);
                char expectedLo = "0123456789ABCDEF".charAt(lo);
                Assert.assertEquals(
                        String.format("Encoding failed for byte 0x%02X", value & 0xFF),
                        "" + expectedHi + expectedLo,
                        encoded);
            }
        }
    }

    @Test
    public void testEncodeDecodeRoundTrip() {
        String originalHex = "a1b2c3d4e5f6";
        byte[] decoded = ByteUtilities.decode(originalHex);
        Assert.assertNotNull("Decode should succeed for even length input", decoded);
        String reEncoded = ByteUtilities.encode(decoded);
        // encode produces upper‑case hex digits
        Assert.assertEquals("Round‑trip encode/decode failed", originalHex.toUpperCase(), reEncoded);
    }

    @Test(expected = NullPointerException.class)
    public void testEncodeNullThrowsNPE() {
        ByteUtilities.encode(null);
    }

    /* -------------------------------------------------
     *  isGzipped(..) tests
     * ------------------------------------------------- */

    @Test
    public void testIsGzippedTrue() {
        byte[] gzipped = new byte[] {(byte)0x1F, (byte)0x8B, 0x08, 0x00};
        Assert.assertTrue("Header 0x1F8B should be recognised as gzipped", ByteUtilities.isGzipped(gzipped));
    }

    @Test
    public void testIsGzippedFalse() {
        byte[] notGzipped = new byte[] {0x00, 0x00, 0x01, 0x02};
        Assert.assertFalse("Non‑gzip header should return false", ByteUtilities.isGzipped(notGzipped));
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testIsGzippedTooShortArray() {
        // The method accesses bytes[0] and bytes[1] without length checks.
        byte[] tooShort = new byte[] {0x1F};
        ByteUtilities.isGzipped(tooShort);
    }

    /* -------------------------------------------------
     *  Additional edge cases
     * ------------------------------------------------- */

    @Test
    public void testDecodeAllZeroes() {
        String zeros = "000000";
        byte[] expected = new byte[] {0x00, 0x00, 0x00};
        Assert.assertArrayEquals("Decoding all zeroes failed", expected, ByteUtilities.decode(zeros));
    }

    @Test
    public void testEncodeAllOnes() {
        // 0xFF => "FF"
        byte[] allOnes = new byte[] {(byte)0xFF, (byte)0xFF};
        Assert.assertEquals("FF repeated expected", "FFFF", ByteUtilities.encode(allOnes));
    }
}
