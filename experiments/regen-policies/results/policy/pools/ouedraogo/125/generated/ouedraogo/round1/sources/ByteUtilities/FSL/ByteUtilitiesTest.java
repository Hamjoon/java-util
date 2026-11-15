package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Comprehensive JUnit 4 test suite for {@link ByteUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>normal and edge cases for {@code decode}</li>
 *   <li>normal and edge cases for {@code encode}</li>
 *   <li>behaviour of {@code isGzipped}</li>
 *   <li>the contract that {@code encode} and {@code decode} are inverses for valid data</li>
 * </ul>
 */
public class ByteUtilitiesTest {

    /* --------------------------------------------------------------------- */
    /* decode()                                                            */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void decode_NullInput_ThrowsNPE() {
        ByteUtilities.decode(null);
    }

    @Test
    public void decode_EmptyString_ReturnsEmptyArray() {
        byte[] result = ByteUtilities.decode("");
        assertNotNull("Result should not be null", result);
        assertEquals("Length of empty decode should be 0", 0, result.length);
    }

    @Test
    public void decode_OddLength_ReturnsNull() {
        assertNull("Odd length string should yield null", ByteUtilities.decode("A"));
        assertNull("Odd length string should yield null", ByteUtilities.decode("123"));
    }

    @Test
    public void decode_ValidUpperCaseHex() {
        byte[] expected = new byte[] { 0x1F, (byte) 0x8B, 0x3C };
        assertArrayEquals("Upper‑case hex decoding failed", expected,
                ByteUtilities.decode("1F8B3C"));
    }

    @Test
    public void decode_ValidLowerCaseHex() {
        byte[] expected = new byte[] { 0x0A, 0x0B, 0x0C };
        assertArrayEquals("Lower‑case hex decoding failed", expected,
                ByteUtilities.decode("0a0b0c"));
    }

    @Test
    public void decode_MixedCaseHex() {
        byte[] expected = new byte[] { (byte) 0xAB, 0x12 };
        assertArrayEquals("Mixed‑case hex decoding failed", expected,
                ByteUtilities.decode("Ab12"));
    }

    @Test
    public void decode_InvalidHexCharacters_ReturnsArrayWithConvertedDigits() {
        // According to the implementation, invalid characters are converted
        // via Character.digit which returns -1. The resulting byte is
        // (byte)(-1 * 16 + -1) = (byte)0xEF.
        byte[] result = ByteUtilities.decode("GG");
        assertNotNull("Result should not be null for invalid chars", result);
        assertEquals("Result length should be 1", 1, result.length);
        assertEquals("Invalid chars should produce 0xEF", (byte) 0xEF, result[0]);
    }

    @Test
    public void decode_AllPossibleByteValues() {
        // Build a hex string representing bytes 0x00 .. 0xFF
        StringBuilder sb = new StringBuilder(512);
        for (int i = 0; i <= 0xFF; i++) {
            sb.append(String.format("%02X", i));
        }
        String hex = sb.toString();

        byte[] decoded = ByteUtilities.decode(hex);
        assertNotNull("Decoded array should not be null", decoded);
        assertEquals("Decoded length should be 256", 256, decoded.length);
        for (int i = 0; i <= 0xFF; i++) {
            assertEquals("Byte value mismatch at index " + i,
                    (byte) i, decoded[i]);
        }
    }

    /* --------------------------------------------------------------------- */
    /* encode()                                                            */
    /* --------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void encode_NullInput_ThrowsNPE() {
        ByteUtilities.encode(null);
    }

    @Test
    public void encode_EmptyArray_ReturnsEmptyString() {
        assertEquals("Encoding empty array should yield empty string",
                "", ByteUtilities.encode(new byte[0]));
    }

    @Test
    public void encode_SimpleByteArray() {
        byte[] input = new byte[] { 0x00, 0x0F, 0x10, (byte) 0xFF };
        String expected = "000F10FF";
        assertEquals("Simple encode failed", expected, ByteUtilities.encode(input));
    }

    @Test
    public void encode_NegativeSignedBytes() {
        byte[] input = new byte[] { (byte) 0x80, (byte) 0xAB, (byte) 0xFF };
        String expected = "80ABFF";
        assertEquals("Encoding of negative signed bytes failed", expected,
                ByteUtilities.encode(input));
    }

    @Test
    public void encode_Decode_RoundTrip() {
        byte[] original = new byte[] {
                0x01, 0x23, 0x45, 0x67,
                (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF,
                0x00, (byte) 0xFF
        };
        String hex = ByteUtilities.encode(original);
        byte[] decoded = ByteUtilities.decode(hex);
        assertArrayEquals("Round‑trip encode/decode failed", original, decoded);
    }

    @Test
    public void encode_AllPossibleByteValues() {
        byte[] allBytes = new byte[256];
        for (int i = 0; i <= 0xFF; i++) {
            allBytes[i] = (byte) i;
        }
        String hex = ByteUtilities.encode(allBytes);
        assertEquals("Hex string length should be 512", 512, hex.length());

        // Verify a few spot checks
        assertEquals("First byte should be 00", "00", hex.substring(0, 2));
        assertEquals("Byte 0x0A should be 0A", "0A", hex.substring(20, 22));
        assertEquals("Byte 0x7F should be 7F", "7F", hex.substring(254, 256));
        assertEquals("Byte 0x80 should be 80", "80", hex.substring(256, 258));
        assertEquals("Last byte should be FF", "FF", hex.substring(510, 512));
    }

    /* --------------------------------------------------------------------- */
    /* isGzipped()                                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void isGzipped_NullArray_ReturnsFalse() {
        // The implementation delegates to ArrayUtilities.size(null). We assume
        // that it returns 0, leading to a false result rather than an exception.
        assertFalse("Null array should not be considered gzipped", ByteUtilities.isGzipped(null));
    }

    @Test
    public void isGzipped_TooShortArray_ReturnsFalse() {
        assertFalse("Array of length 0 should not be gzipped", ByteUtilities.isGzipped(new byte[0]));
        assertFalse("Array of length 1 should not be gzipped", ByteUtilities.isGzipped(new byte[] { (byte) 0x1F }));
    }

    @Test
    public void isGzipped_ValidGzipHeader_ReturnsTrue() {
        byte[] gzipped = new byte[] { (byte) 0x1F, (byte) 0x8B, 0x08, 0x00 };
        assertTrue("Valid gzip header should be recognised", ByteUtilities.isGzipped(gzipped));
    }

    @Test
    public void isGzipped_InvalidHeader_ReturnsFalse() {
        byte[] notGzipped = new byte[] { (byte) 0x1F, 0x00, 0x00 };
        assertFalse("Incorrect second byte should cause false", ByteUtilities.isGzipped(notGzipped));
    }

    @Test
    public void isGzipped_HeaderPresentButArrayLonger_ReturnsTrue() {
        byte[] data = new byte[100];
        data[0] = (byte) 0x1F;
        data[1] = (byte) 0x8B;
        // Fill the rest with arbitrary data
        for (int i = 2; i < data.length; i++) {
            data[i] = (byte) i;
        }
        assertTrue("Array starting with gzip magic numbers should be true", ByteUtilities.isGzipped(data));
    }

    /* --------------------------------------------------------------------- */
    /* Additional internal behaviour (convertDigit) – accessed via reflection */
    /* --------------------------------------------------------------------- */

    @Test
    public void convertDigit_ReflectiveAccess_ProducesExpectedChars() throws Exception {
        // Using reflection to call the private convertDigit method.
        java.lang.reflect.Method method = ByteUtilities.class.getDeclaredMethod("convertDigit", int.class);
        method.setAccessible(true);

        // Test the full range 0‑15
        char[] expected = {
                '0','1','2','3','4','5','6','7',
                '8','9','A','B','C','D','E','F'
        };
        for (int i = 0; i <= 15; i++) {
            char result = (char) method.invoke(null, i);
            assertEquals("convertDigit(" + i + ") mismatch", expected[i], result);
        }

        // Values outside the range should be masked with 0x0F
        assertEquals('0', method.invoke(null, 16));   // 16 & 0x0F == 0
        assertEquals('F', method.invoke(null, 31));   // 31 & 0x0F == 15
        assertEquals('A', method.invoke(null, 26));   // 26 & 0x0F == 10
    }
}
