package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;

/**
 * JUnit 4 test suite for {@link ByteUtilities}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods with basic functionality checks.</li>
 *   <li>Edge‑case handling such as null arguments, empty inputs, odd‑length strings,
 *       non‑hex characters, and values outside the expected range.</li>
 *   <li>Verification that the methods behave as documented (e.g., {@code decode}
 *       returns {@code null} for invalid input).</li>
 * </ul>
 */
public class ByteUtilitiesTest {

    /* -------------------------------------------------------------
     *  Public method signatures extracted from ByteUtilities
     * -------------------------------------------------------------
     *
     * public static char toHexChar(final int value)
     * public static byte[] decode(final String s)
     * public static String encode(final byte[] bytes)
     * public static boolean isGzipped(byte[] bytes)
     *
     * ------------------------------------------------------------- */

    /* ----------------------- toHexChar ----------------------- */

    @Test
    public void testToHexCharBasic() {
        // Verify mapping for values 0‑15
        char[] expected = "0123456789ABCDEF".toCharArray();
        for (int i = 0; i < 16; i++) {
            assertEquals("Mismatch at value " + i, expected[i], ByteUtilities.toHexChar(i));
        }
    }

    @Test
    public void testToHexCharMaskingBehaviour() {
        // Values outside 0‑15 are masked with 0x0F
        assertEquals('A', ByteUtilities.toHexChar(0x0A));          // normal
        assertEquals('A', ByteUtilities.toHexChar(0x1A));          // 0x1A & 0x0F == 0x0A
        assertEquals('0', ByteUtilities.toHexChar(0x10));          // 0x10 & 0x0F == 0x00
        assertEquals('F', ByteUtilities.toHexChar(-1));           // -1 & 0x0F == 0x0F
        assertEquals('5', ByteUtilities.toHexChar(0x25));          // 0x25 & 0x0F == 0x05
    }

    /* -------------------------- encode -------------------------- */

    @Test
    public void testEncodeBasic() {
        byte[] data = {(byte) 0x1F, (byte) 0x8B, (byte) 0x3C};
        String expected = "1F8B3C";
        assertEquals(expected, ByteUtilities.encode(data));
    }

    @Test
    public void testEncodeEmptyArray() {
        assertEquals("", ByteUtilities.encode(new byte[0]));
    }

    @Test(expected = NullPointerException.class)
    public void testEncodeNullArray() {
        ByteUtilities.encode(null);
    }

    /* -------------------------- decode -------------------------- */

    @Test
    public void testDecodeBasic() {
        String hex = "1F8B3C";
        byte[] expected = {(byte) 0x1F, (byte) 0x8B, (byte) 0x3C};
        assertArrayEquals(expected, ByteUtilities.decode(hex));
    }

    @Test
    public void testDecodeEmptyString() {
        assertArrayEquals(new byte[0], ByteUtilities.decode(""));
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull(ByteUtilities.decode("ABC")); // length 3 (odd)
    }

    @Test
    public void testDecodeInvalidCharacterReturnsNull() {
        assertNull(ByteUtilities.decode("ZZ")); // 'Z' is not a valid hex digit
        assertNull(ByteUtilities.decode("1G")); // 'G' invalid
    }

    @Test
    public void testDecodeNonAsciiCharacterReturnsNull() {
        // Unicode character beyond ASCII range (e.g., 'Ω')
        assertNull(ByteUtilities.decode("\u03A9\u03A9"));
    }

    @Test(expected = NullPointerException.class)
    public void testDecodeNullString() {
        ByteUtilities.decode(null);
    }

    /* ------------------------ isGzipped ------------------------ */

    @Test
    public void testIsGzippedTrue() {
        byte[] gzipped = {(byte) 0x1F, (byte) 0x8B, 0x00, 0x01};
        assertTrue(ByteUtilities.isGzipped(gzipped));
    }

    @Test
    public void testIsGzippedFalseDueToHeader() {
        byte[] notGzip = {(byte) 0x00, (byte) 0x00, 0x01, 0x02};
        assertFalse(ByteUtilities.isGzipped(notGzip));
    }

    @Test
    public void testIsGzippedFalseDueToShortArray() {
        assertFalse(ByteUtilities.isGzipped(new byte[0]));
        assertFalse(ByteUtilities.isGzipped(new byte[1]));
    }

    @Test
    public void testIsGzippedNullArray() {
        assertFalse(ByteUtilities.isGzipped(null));
    }

    /* -------------------------------------------------------------
     *  Additional combined scenario tests (optional but helpful)
     * ------------------------------------------------------------- */

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
}
