package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import static org.junit.Assert.*;

import java.lang.reflect.Method;

/**
 * Unit tests for {@link ByteUtilities}.
 *
 * These tests cover:
 * <ul>
 *   <li>hexadecimal decoding (including edge cases and invalid input)</li>
 *   <li>hexadecimal encoding (empty, single and multiple bytes, signed values)</li>
 *   <li>private {@code convertDigit} method via reflection</li>
 *   <li>GZIP header detection</li>
 * </ul>
 */
public class ByteUtilitiesTest {

    // ----------------------------------------------------------------------
    // decode(String) tests
    // ----------------------------------------------------------------------

    @Test
    public void testDecodeValidHexString() {
        byte[] expected = new byte[] { (byte)0x1F, (byte)0x8B, (byte)0x3C };
        byte[] actual = ByteUtilities.decode("1F8B3C");
        assertArrayEquals("Decoding a normal hex string should produce expected bytes", expected, actual);
    }

    @Test
    public void testDecodeEmptyStringReturnsEmptyArray() {
        byte[] actual = ByteUtilities.decode("");
        assertNotNull("Result should never be null for empty string", actual);
        assertEquals("Empty string must produce empty byte array", 0, actual.length);
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull("Odd length strings must return null", ByteUtilities.decode("ABC"));
        assertNull("Odd length strings must return null", ByteUtilities.decode("1"));
    }

    @Test(expected = NullPointerException.class)
    public void testDecodeNullThrowsNPE() {
        ByteUtilities.decode(null);
    }

    @Test
    public void testDecodeInvalidHexCharacters() {
        // The implementation does NOT guard against non‑hex characters;
        // Character.digit returns -1, which leads to a calculated byte value.
        byte[] actual = ByteUtilities.decode("ZZ");
        assertNotNull("Invalid hex should still return a byte array (per current implementation)", actual);
        assertEquals("Length must be half of the input length", 1, actual.length);
        // Expected calculation: hi = -1, lo = -1 => (-1)*16 + (-1) = -17 => (byte)-17
        assertEquals("Byte value should match calculation with -1 digits", (byte)-17, actual[0]);
    }

    // ----------------------------------------------------------------------
    // encode(byte[]) tests
    // ----------------------------------------------------------------------

    @Test
    public void testEncodeEmptyArrayReturnsEmptyString() {
        assertEquals("Encoding an empty byte array must return empty string", "", ByteUtilities.encode(new byte[0]));
    }

    @Test
    public void testEncodeSingleByte() {
        byte[] data = new byte[] { (byte)0x0A };
        assertEquals("Encoding single byte 0x0A", "0A", ByteUtilities.encode(data));
    }

    @Test
    public void testEncodeMultipleBytesIncludingNegativeValues() {
        byte[] data = new byte[] {
                (byte)0x00, (byte)0x7F, (byte)0x80, (byte)0xFF,
                (byte)-1, (byte)-128, (byte)0x55
        };
        // Expected hex strings for each byte (uppercase)
        String expected = "007F80FFFFFFFF5555";
        // Explanation:
        // 0x00 -> "00"
        // 0x7F -> "7F"
        // 0x80 -> "80"
        // 0xFF -> "FF"
        // -1  -> 0xFF -> "FF"
        // -128 -> 0x80 -> "80"
        // 0x55 -> "55"
        // Concatenated = "007F80FFFFFFFF5555"
        assertEquals("Encoding mixed signed bytes", expected, ByteUtilities.encode(data));
    }

    // ----------------------------------------------------------------------
    // Private convertDigit(int) via reflection
    // ----------------------------------------------------------------------

    @Test
    public void testConvertDigitViaReflection() throws Exception {
        Method method = ByteUtilities.class.getDeclaredMethod("convertDigit", int.class);
        method.setAccessible(true);

        // Verify mapping for all 0‑15 values
        char[] expected = {
                '0','1','2','3','4','5','6','7',
                '8','9','A','B','C','D','E','F'
        };

        for (int i = 0; i < 16; i++) {
            char result = (char) method.invoke(null, i);
            assertEquals("convertDigit(" + i + ") should return correct hex character", expected[i], result);
        }

        // Values outside 0‑15 should be masked with 0x0F (as per implementation)
        assertEquals('A', (char) method.invoke(null, 0x0A));
        assertEquals('0', (char) method.invoke(null, 0x10)); // 0x10 & 0x0F == 0
        assertEquals('F', (char) method.invoke(null, 0x1F)); // 0x1F & 0x0F == 0xF
    }

    // ----------------------------------------------------------------------
    // isGzipped(byte[]) tests
    // ----------------------------------------------------------------------

    @Test
    public void testIsGzippedRecognizesValidHeader() {
        byte[] gzipHeader = new byte[] { (byte)0x1F, (byte)0x8B, 0x00, 0x00 };
        assertTrue("Byte array starting with 0x1F8B must be identified as gzipped", ByteUtilities.isGzipped(gzipHeader));
    }

    @Test
    public void testIsGzippedRejectsNonGzipData() {
        byte[] notGzip = new byte[] { (byte)0x00, (byte)0x00 };
        assertFalse("Array not starting with 0x1F8B must not be identified as gzipped", ByteUtilities.isGzipped(notGzip));

        byte[] shortArray = new byte[] { (byte)0x1F };
        assertFalse("Array shorter than 2 bytes must not be identified as gzipped", ByteUtilities.isGzipped(shortArray));

        assertFalse("Null array must not be identified as gzipped", ByteUtilities.isGzipped(null));
    }

    @Test
    public void testIsGzippedWithExactHeaderOnly() {
        byte[] exactHeader = new byte[] { (byte)0x1F, (byte)0x8B };
        assertTrue("Exactly two bytes forming gzip header should be considered gzipped", ByteUtilities.isGzipped(exactHeader));
    }

    // ----------------------------------------------------------------------
    // Additional round‑trip consistency tests
    // ----------------------------------------------------------------------

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] original = new byte[256];
        for (int i = 0; i < 256; i++) {
            original[i] = (byte) i;
        }
        String encoded = ByteUtilities.encode(original);
        byte[] decoded = ByteUtilities.decode(encoded);
        assertArrayEquals("Encode → Decode round‑trip must return original bytes", original, decoded);
    }
}
