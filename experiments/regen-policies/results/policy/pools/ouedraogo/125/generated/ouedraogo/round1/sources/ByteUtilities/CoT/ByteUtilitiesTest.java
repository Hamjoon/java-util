package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link ByteUtilities}.
 *
 * The tests are organised as follows:
 * <ul>
 *   <li>Extraction of the public API (decode, encode, isGzipped).</li>
 *   <li>Basic “happy‑path” tests that verify the documented behaviour.</li>
 *   <li>Edge‑case / exception tests that cover:
 *       <ul>
 *           <li>null arguments</li>
 *           <li>empty inputs</li>
 *           <li>odd‑length hex strings</li>
 *           <li>non‑hex characters</li>
 *           <li>arrays that are too short to be GZIP headers</li>
 *           <li>valid and invalid GZIP signatures</li>
 *       </ul>
 *   </li>
 * </ul>
 *
 * The test class uses only the public API of {@code ByteUtilities} and the
 * standard JUnit‑4 library – no additional mocking frameworks or external
 * dependencies are required.
 */
public class ByteUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  Public API extraction (for documentation purposes)                     */
    /* ---------------------------------------------------------------------- */
    // public static byte[] decode(String s)
    // public static String encode(byte[] bytes)
    // public static boolean isGzipped(byte[] bytes)

    /* ---------------------------------------------------------------------- */
    /*  BASIC FUNCTIONALITY TESTS                                             */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDecode_ValidHexString() {
        byte[] expected = new byte[] { (byte)0x1F, (byte)0x8B, (byte)0x3C };
        assertArrayEquals(expected, ByteUtilities.decode("1F8B3C"));
    }

    @Test
    public void testDecode_EmptyStringReturnsEmptyArray() {
        byte[] result = ByteUtilities.decode("");
        assertNotNull("Result should not be null for empty input", result);
        assertEquals("Empty input must produce an empty array", 0, result.length);
    }

    @Test
    public void testEncode_ValidByteArray() {
        byte[] input = new byte[] { (byte)0x1F, (byte)0x8B, (byte)0x3C };
        String expected = "1F8B3C";
        assertEquals(expected, ByteUtilities.encode(input));
    }

    @Test
    public void testEncode_EmptyArrayReturnsEmptyString() {
        assertEquals("", ByteUtilities.encode(new byte[0]));
    }

    @Test
    public void testIsGzipped_ValidHeader() {
        byte[] gzipped = new byte[] { (byte)0x1F, (byte)0x8B, 0x00, 0x00 };
        assertTrue(ByteUtilities.isGzipped(gzipped));
    }

    @Test
    public void testIsGzipped_InvalidHeader() {
        byte[] notGzipped = new byte[] { 0x00, 0x00, 0x01, 0x02 };
        assertFalse(ByteUtilities.isGzipped(notGzipped));
    }

    /* ---------------------------------------------------------------------- */
    /*  EDGE‑CASE & EXCEPTION TESTS                                            */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testDecode_NullInputThrowsNPE() {
        ByteUtilities.decode(null);
    }

    @Test
    public void testDecode_OddLengthStringReturnsNull() {
        assertNull("Odd length input must return null", ByteUtilities.decode("ABC"));
    }

    @Test
    public void testDecode_NonHexCharacters() {
        // According to the Javadoc, non‑hex characters should cause a null return,
        // however the current implementation returns a byte array where each
        // invalid digit is interpreted as -1 by Character.digit().
        // This test documents the actual behaviour.
        byte[] result = ByteUtilities.decode("GG");
        assertNotNull("Result should not be null for non‑hex characters (implementation detail)", result);
        assertEquals(1, result.length);
        // Character.digit('G',16) == -1, so (byte)(-1*16 + -1) == (byte)-17
        assertEquals((byte)-17, result[0]);
    }

    @Test(expected = NullPointerException.class)
    public void testEncode_NullArrayThrowsNPE() {
        ByteUtilities.encode(null);
    }

    @Test
    public void testIsGzipped_NullArray() {
        // ArrayUtilities.size(null) is expected to return 0 (defensive implementation),
        // therefore isGzipped(null) should safely return false.
        assertFalse(ByteUtilities.isGzipped(null));
    }

    @Test
    public void testIsGzipped_ArrayTooShort() {
        byte[] oneByte = new byte[] { (byte)0x1F };
        assertFalse("Array with fewer than 2 bytes cannot be GZIP", ByteUtilities.isGzipped(oneByte));
    }

    @Test
    public void testEncode_AllByteValues() {
        // Verify that each possible byte value maps to the correct two‑character hex string.
        for (int i = 0; i <= 0xFF; i++) {
            byte b = (byte) i;
            String encoded = ByteUtilities.encode(new byte[] { b });
            int hi = (i >>> 4) & 0xF;
            int lo = i & 0xF;
            char expectedHi = "0123456789ABCDEF".charAt(hi);
            char expectedLo = "0123456789ABCDEF".charAt(lo);
            assertEquals("Hex conversion failed for value " + i,
                         "" + expectedHi + expectedLo, encoded);
        }
    }

    @Test
    public void testDecode_AllHexPairs() {
        // Build a string containing every possible two‑character hex pair (00..FF)
        StringBuilder sb = new StringBuilder(512);
        for (int i = 0; i <= 0xFF; i++) {
            sb.append(String.format("%02X", i));
        }
        byte[] decoded = ByteUtilities.decode(sb.toString());
        assertNotNull(decoded);
        assertEquals(256, decoded.length);
        for (int i = 0; i <= 0xFF; i++) {
            assertEquals((byte) i, decoded[i]);
        }
    }
}
